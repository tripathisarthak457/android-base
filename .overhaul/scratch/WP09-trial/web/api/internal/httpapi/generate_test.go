//go:build unix

package httpapi

import (
	"encoding/json"
	"net/http"
	"slices"
	"strings"
	"testing"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/store"
	"github.com/base-android/generator-api/internal/telemetry"
)

const sessionID = "3f1c2a9e-8b7d-4c6e-9a1b-2d3e4f5a6b7c"

func decodeError(t *testing.T, body string) apiError {
	t.Helper()
	var e apiError
	if err := json.Unmarshal([]byte(body), &e); err != nil {
		t.Fatalf("error body %q: %v", body, err)
	}
	return e
}

func onlyGeneration(t *testing.T, b *store.Batch) store.Generation {
	t.Helper()
	if len(b.Generations) != 1 {
		t.Fatalf("%d generations recorded, want 1", len(b.Generations))
	}
	return b.Generations[0]
}

func TestGenerateStreamsTheZipAndRecordsTheSession(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	spec := `{"schema": 2, "platform": "android", "app_name": "Trail Notes", "package_name": "com.example.trail",
		"features": ["network", "liquidglass"], "android": {"min_sdk": 26,
		"keystores": [{"name": "release", "store_password": "hunter22"}]}}`

	w := ts.do(http.MethodPost, "/api/v1/android/generate", spec, map[string]string{
		"X-Session-Id": sessionID, "X-Config-Ms": "93000", "X-Client": "android-base-web/1a2b3c4",
	})

	if w.Code != http.StatusOK || w.Body.String() != spec {
		t.Fatalf("got %d %q", w.Code, w.Body.String())
	}
	for header, want := range map[string]string{
		"Content-Type":        "application/zip",
		"Content-Disposition": `attachment; filename="TrailNotes.zip"`,
		"X-Project-Name":      "TrailNotes",
		"X-Generation-Ms":     "412",
		"X-Keystores-Skipped": "release",
	} {
		if got := w.Header().Get(header); got != want {
			t.Errorf("%s = %q, want %q", header, got, want)
		}
	}

	batch := ts.sink.last(t)
	g := onlyGeneration(t, batch)
	if g.Outcome != telemetry.OK || g.SessionID != sessionID || *g.ConfigMs != 93000 || g.Client != telemetry.ClientWeb {
		t.Fatalf("session not recorded: %+v", g)
	}
	// Resolved features against the catalogue: standard plus glass.
	if g.Preset != "standard" || !slices.Equal(g.Added, []string{"liquidglass"}) || len(g.Removed) != 0 || !g.Glass {
		t.Fatalf("preset %q added %v removed %v glass %v", g.Preset, g.Added, g.Removed, g.Glass)
	}
	if g.MinSDK != 26 || g.Architecture != "compact" || g.DesignStyle != "Utility" || g.GeneratorCommit != "99c4edf" {
		t.Fatalf("spec fields and defaults not recorded: %+v", g)
	}
	if g.QueueMs == nil || g.PythonMs == nil || g.StreamMs == nil || g.ZipBytes != int64(len(spec)) {
		t.Fatalf("timings not recorded: %+v", g)
	}
	if strings.Contains(string(g.Spec), "hunter22") {
		t.Fatalf("a keystore password reached the recorded spec: %s", g.Spec)
	}
	var names []string
	for _, e := range batch.Events {
		names = append(names, e.Name)
	}
	if !slices.Equal(names, []string{"generated", "downloaded"}) {
		t.Fatalf("server events %v", names)
	}
	if len(batch.Requests) != 1 || batch.Requests[0].Route != "generate" || batch.Requests[0].Status != 200 {
		t.Fatalf("request row %+v", batch.Requests)
	}
}

func TestAGenerationWithoutASessionWritesNoFunnelEvents(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	w := ts.do(http.MethodPost, "/api/v1/android/generate", `{"app_name": "X"}`, map[string]string{"User-Agent": "curl/8.5.0"})
	if w.Code != http.StatusOK {
		t.Fatalf("got %d", w.Code)
	}
	batch := ts.sink.last(t)
	if g := onlyGeneration(t, batch); g.Client != telemetry.ClientCLI || g.SessionID != "" || g.ConfigMs != nil {
		t.Fatalf("got %+v", g)
	}
	if len(batch.Events) != 0 {
		t.Fatalf("events without a session: %+v", batch.Events)
	}
}

func TestGenerateMapsFailures(t *testing.T) {
	cases := []struct {
		name    string
		script  string
		body    string
		status  int
		code    string
		field   string
		outcome telemetry.Outcome
		// errorDetail is what the error list must keep; empty means nothing goes there.
		errorDetail string
	}{
		{
			name:   "precheck",
			script: okScript, body: `{"feature_modules": ["a","b","c","d","e","f","g","h","i","j","k","l","m"]}`,
			status: 400, code: "invalid_spec", field: "feature_modules", outcome: telemetry.Invalid,
		},
		{
			name:   "platform of another route",
			script: okScript, body: `{"schema": 2, "platform": "multiplatform"}`,
			status: 400, code: "invalid_spec", field: "platform", outcome: telemetry.Invalid,
		},
		{
			name:   "rejected by the generator",
			script: `echo '{"ok": false, "kind": "invalid", "error": "The package name is reserved.", "field": "package_name"}'; exit 2`,
			body:   `{}`, status: 400, code: "invalid_spec", field: "package_name", outcome: telemetry.Invalid,
		},
		{
			name:   "generator bug",
			script: `echo '{"ok": false, "kind": "internal", "error": "The generator failed.", "detail": "Traceback (most recent call last): KeyError"}'; exit 1`,
			body:   `{}`, status: 500, code: "generator_failed", outcome: telemetry.InternalError,
			errorDetail: "KeyError",
		},
		{
			name:   "crash",
			script: `echo 'Segmentation fault' >&2; exit 139`,
			body:   `{}`, status: 500, code: "generator_failed", outcome: telemetry.InternalError,
			errorDetail: "Segmentation fault",
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			ts := newTestServer(t, c.script, nil)
			w := ts.do(http.MethodPost, "/api/v1/android/generate", c.body, nil)
			if w.Code != c.status {
				t.Fatalf("status %d, want %d: %s", w.Code, c.status, w.Body.String())
			}
			e := decodeError(t, w.Body.String())
			if e.Code != c.code || e.Field != c.field {
				t.Fatalf("error %+v, want code %s field %s", e, c.code, c.field)
			}
			if c.status == 500 && strings.Contains(w.Body.String(), "Traceback") {
				t.Fatal("a traceback reached the visitor")
			}
			batch := ts.sink.last(t)
			if g := onlyGeneration(t, batch); g.Outcome != c.outcome || g.FailureField != c.field {
				t.Fatalf("recorded %s field %q, want %s %q", g.Outcome, g.FailureField, c.outcome, c.field)
			}
			if c.errorDetail == "" && len(batch.Errors) != 0 {
				t.Fatalf("a rejection went to the error list: %+v", batch.Errors)
			}
			if c.errorDetail != "" && (len(batch.Errors) != 1 || !strings.Contains(batch.Errors[0].Detail, c.errorDetail) ||
				batch.Errors[0].Kind != "generation") {
				t.Fatalf("error list entry %+v, want detail with %q", batch.Errors, c.errorDetail)
			}
		})
	}
}

func TestGenerateRefusesTargetsItDoesNotServe(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	cases := map[string]string{
		"/api/v1/ios/generate":     "target_unavailable",
		"/api/v1/windows/generate": "unknown_target",
	}
	for path, code := range cases {
		w := ts.do(http.MethodPost, path, `{}`, nil)
		if w.Code != http.StatusNotFound || decodeError(t, w.Body.String()).Code != code {
			t.Errorf("%s: %d %s", path, w.Code, w.Body.String())
		}
	}
}

func TestGenerateRequiresJSON(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	for _, contentType := range []string{"text/plain", "application/x-www-form-urlencoded", ""} {
		w := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, map[string]string{"Content-Type": contentType})
		if w.Code != http.StatusUnsupportedMediaType {
			t.Errorf("Content-Type %q: got %d", contentType, w.Code)
		}
	}
	w := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, map[string]string{"Content-Type": "application/json; charset=utf-8"})
	if w.Code != http.StatusOK {
		t.Fatalf("JSON with a charset: got %d", w.Code)
	}
}

func TestOversizedSpecIsA413(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	w := ts.do(http.MethodPost, "/api/v1/android/generate", `{"app_name": "`+strings.Repeat("a", 40<<10)+`"}`, nil)
	if w.Code != http.StatusRequestEntityTooLarge {
		t.Fatalf("got %d", w.Code)
	}
	if g := onlyGeneration(t, ts.sink.last(t)); g.Outcome != telemetry.Invalid {
		t.Fatalf("recorded %s", g.Outcome)
	}
}

func TestRateLimitedRequestsAreRecordedOncePerWindow(t *testing.T) {
	ts := newTestServer(t, okScript, func(cfg *config.Config) { cfg.RateLimitPerHour = 1 })

	if w := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, nil); w.Code != http.StatusOK {
		t.Fatalf("first request: %d", w.Code)
	}
	w := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, nil)
	if w.Code != http.StatusTooManyRequests || w.Header().Get("Retry-After") == "" {
		t.Fatalf("second request: %d", w.Code)
	}
	if g := onlyGeneration(t, ts.sink.last(t)); g.Outcome != telemetry.RateLimited {
		t.Fatalf("recorded %s", g.Outcome)
	}

	ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, nil)
	if n := len(ts.sink.last(t).Generations); n != 0 {
		t.Fatalf("a repeated refusal was recorded again (%d rows)", n)
	}
}

func TestTheRateLimitCoversAWholeIPv6Slash64(t *testing.T) {
	ts := newTestServer(t, okScript, func(cfg *config.Config) {
		cfg.RateLimitPerHour = 1
		cfg.TrustedProxy = config.ProxyPolicy{Hops: 1, Vercel: true}
	})
	first := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, map[string]string{"X-Forwarded-For": "2001:db8:1:2::1"})
	second := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, map[string]string{"X-Forwarded-For": "2001:db8:1:2:ffff::9"})
	other := ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, map[string]string{"X-Forwarded-For": "2001:db8:1:3::1"})
	if first.Code != 200 || second.Code != 429 || other.Code != 200 {
		t.Fatalf("got %d %d %d, want 200 429 200", first.Code, second.Code, other.Code)
	}
}

func TestTheLegacyRouteGeneratesAndroid(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	w := ts.do(http.MethodPost, "/api/generate", `{"app_name": "X", "package_name": "com.example.x", "preset": "lean"}`, nil)
	if w.Code != http.StatusOK {
		t.Fatalf("got %d %s", w.Code, w.Body.String())
	}
	batch := ts.sink.last(t)
	if g := onlyGeneration(t, batch); g.Target != "android" {
		t.Fatalf("target %q", g.Target)
	}
	if batch.Requests[0].Route != "legacy.generate" {
		t.Fatalf("route %q", batch.Requests[0].Route)
	}
}

func TestOnlyTheFirstGenerationIsAColdStart(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, nil)
	first := onlyGeneration(t, ts.sink.last(t)).ColdStart
	ts.do(http.MethodPost, "/api/v1/android/generate", `{}`, nil)
	second := onlyGeneration(t, ts.sink.last(t)).ColdStart
	if !first || second {
		t.Fatalf("cold start: first %v, second %v", first, second)
	}
}
