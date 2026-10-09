//go:build unix

package httpapi

import (
	"context"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/generate"
	"github.com/base-android/generator-api/internal/store"
)

const testCatalogue = `{
  "version": 2, "generator_commit": "99c4edf",
  "targets": [{
    "key": "android", "title": "Android Native", "status": "available", "summary": "Compose.",
    "features": [{"key": "network", "title": "Network"}, {"key": "auth", "title": "Sign-in", "implies": ["network"]},
                 {"key": "settings", "title": "Settings"}, {"key": "liquidglass", "title": "Glass"}],
    "presets": [{"key": "lean", "features": ["settings"]},
                {"key": "standard", "features": ["network", "auth", "settings"]}],
    "defaults": {"preset": "standard", "architecture": "compact", "design_style": "Utility", "motion_style": "Standard"}
  }, {
    "key": "multiplatform", "title": "Compose Multiplatform", "status": "beta",
    "status_note": "Beta: the iOS app is not yet verified on a Mac", "summary": "Android and iOS.",
    "features": [{"key": "network", "title": "Network"}], "presets": [{"key": "standard", "features": ["network"]}],
    "defaults": {"preset": "standard"}
  }, {
    "key": "ios", "title": "Swift", "status": "coming_soon", "summary": "SwiftUI.",
    "features": [], "presets": [], "defaults": {}
  }],
  "rules": {"spec_bytes_max": 32768, "feature_modules_max": 12, "languages_max": 10, "keystores_max": 4}
}`

// okScript is a generator that writes the spec it was given as the zip and reports success.
const okScript = `cat > "$3"
echo '{"ok": true, "target": "'"$2"'", "project_name": "TrailNotes", "elapsed_ms": 412, "zip_bytes": 1,
  "resolved_features": ["network", "auth", "settings", "liquidglass"], "notes": [],
  "warnings": ["The release key password was not used."], "ignored_fields": ["theme"],
  "generator_commit": "99c4edf", "keystores_skipped": ["release"]}'
`

type fakeSink struct {
	mu      sync.Mutex
	batches []*store.Batch
}

func (f *fakeSink) Submit(b *store.Batch) <-chan struct{} {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.batches = append(f.batches, b)
	done := make(chan struct{})
	close(done)
	return done
}

func (f *fakeSink) Stats() store.WriterStats { return store.WriterStats{} }

// last is the batch of the most recent request.
func (f *fakeSink) last(t *testing.T) *store.Batch {
	t.Helper()
	f.mu.Lock()
	defer f.mu.Unlock()
	if len(f.batches) == 0 {
		t.Fatal("nothing was recorded")
	}
	return f.batches[len(f.batches)-1]
}

var quiet = slog.New(slog.NewTextHandler(io.Discard, nil))

type testServer struct {
	*Server
	sink    *fakeSink
	handler http.Handler
}

// newTestServer runs script as every enabled target's generator. The store is real but never
// connects, so reads answer "not ready" and writes go to the fake sink.
func newTestServer(t *testing.T, script string, configure func(*config.Config)) *testServer {
	t.Helper()
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "generate_headless.py"), []byte(script), 0o600); err != nil {
		t.Fatal(err)
	}
	catalogue, err := generate.ParseCatalogue([]byte(testCatalogue))
	if err != nil {
		t.Fatal(err)
	}
	cfg := config.Config{
		AdminToken:       "0123456789abcdef0123456789abcdef",
		AllowedOrigins:   []string{"http://localhost:3000"},
		RateLimitPerHour: 30,
		EnableTargets:    []string{"android", "multiplatform"},
	}
	if configure != nil {
		configure(&cfg)
	}
	opts := generate.Options{PythonBin: "/bin/sh", GeneratorDir: dir, WorkRoot: t.TempDir(), Timeout: 5 * time.Second, Concurrency: 2}
	generators := map[string]*generate.Generator{}
	for _, key := range cfg.EnableTargets {
		g, err := generate.New(context.Background(), opts, *catalogue.Target(key), catalogue.Rules)
		if err != nil {
			t.Fatal(err)
		}
		generators[key] = g
	}
	st, err := store.New("postgres://wp09@127.0.0.1:1/none", "salt", quiet)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(st.Close)
	sink := &fakeSink{}
	s := New(cfg, catalogue, generators, st, sink, quiet)
	return &testServer{Server: s, sink: sink, handler: s.Routes()}
}

func (ts *testServer) do(method, path, body string, headers map[string]string) *httptest.ResponseRecorder {
	r := httptest.NewRequest(method, path, strings.NewReader(body))
	r.RemoteAddr = "203.0.113.7:51000"
	if method == http.MethodPost {
		r.Header.Set("Content-Type", "application/json")
	}
	for name, value := range headers {
		if value == "" {
			r.Header.Del(name)
		} else {
			r.Header.Set(name, value)
		}
	}
	w := httptest.NewRecorder()
	ts.handler.ServeHTTP(w, r)
	return w
}
