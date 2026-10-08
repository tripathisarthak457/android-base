//go:build unix

package generate

import (
	"bytes"
	"context"
	"errors"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
	"testing"
	"time"

	"github.com/base-android/generator-api/internal/telemetry"
)

// fake writes script as generate_headless.py and runs it with sh, which sees the same arguments the
// real generator gets: $1 --target, $2 the target, $3 the zip path.
func fake(t *testing.T, script string, timeout time.Duration, stopping context.Context) (*Generator, string) {
	t.Helper()
	dir := t.TempDir()
	if err := os.WriteFile(filepath.Join(dir, "generate_headless.py"), []byte(script), 0o600); err != nil {
		t.Fatal(err)
	}
	root := filepath.Join(dir, "work")
	if err := os.Mkdir(root, 0o700); err != nil {
		t.Fatal(err)
	}
	if stopping == nil {
		stopping = context.Background()
	}
	g, err := New(stopping, Options{PythonBin: "/bin/sh", GeneratorDir: dir, WorkRoot: root, Timeout: timeout, Concurrency: 1},
		TargetEntry{Key: "android"}, Rules{})
	if err != nil {
		t.Fatal(err)
	}
	return g, root
}

func failure(t *testing.T, err error) *Failure {
	t.Helper()
	var f *Failure
	if !errors.As(err, &f) {
		t.Fatalf("want a *Failure, got %v", err)
	}
	return f
}

func assertEmpty(t *testing.T, dir string) {
	t.Helper()
	entries, err := os.ReadDir(dir)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 0 {
		t.Fatalf("%s should be empty, holds %v", dir, entries)
	}
}

func TestSuccessPassesTheSpecThroughAndReportsTheZip(t *testing.T) {
	g, root := fake(t, `
test "$1" = --target && test "$2" = android || exit 9
cat > "$3"
echo '{"ok": true, "target": "android", "project_name": "TrailNotes", "elapsed_ms": 412, "zip_bytes": 1,
  "resolved_features": ["network", "auth"], "notes": [], "warnings": ["w"], "ignored_fields": [],
  "generator_commit": "99c4edf"}'
`, 5*time.Second, nil)

	spec := []byte(`{"schema": 2, "app_name": "Trail Notes",  "features": ["auth"]}`)
	out, err := g.Run(context.Background(), spec)
	if err != nil {
		t.Fatal(err)
	}
	written, err := os.ReadFile(out.ZipPath)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(written, spec) {
		t.Fatalf("the generator received %q, want the request body unchanged", written)
	}
	if out.ProjectName != "TrailNotes" || out.GeneratorCommit != "99c4edf" || len(out.ResolvedFeatures) != 2 {
		t.Fatalf("result not read: %+v", out.Result)
	}
	if out.ZipBytes != int64(len(spec)) {
		t.Fatalf("zip_bytes = %d, want the size on disk, %d", out.ZipBytes, len(spec))
	}

	if err := out.Close(); err != nil {
		t.Fatal(err)
	}
	assertEmpty(t, root)
}

func TestFailureKindsMapToOutcomes(t *testing.T) {
	cases := []struct {
		name    string
		script  string
		outcome telemetry.Outcome
		message string
		field   string
		detail  string
	}{
		{
			name:    "invalid input",
			script:  `echo '{"ok": false, "kind": "invalid", "error": "The package name is not valid.", "field": "package_name"}'; exit 2`,
			outcome: telemetry.Invalid, message: "The package name is not valid.", field: "package_name",
		},
		{
			name:    "internal failure keeps the traceback",
			script:  `echo '{"ok": false, "kind": "internal", "error": "The generator failed.", "detail": "Traceback: KeyError"}'; exit 1`,
			outcome: telemetry.InternalError, message: "The generator failed.", detail: "Traceback: KeyError",
		},
		{
			name:    "a crash without a reply",
			script:  `echo 'Fatal Python error' >&2; exit 1`,
			outcome: telemetry.InternalError, detail: "Fatal Python error",
		},
		{
			name:    "success without a zip",
			script:  `echo '{"ok": true, "project_name": "X"}'`,
			outcome: telemetry.InternalError, detail: "exit 0",
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			g, root := fake(t, c.script, 5*time.Second, nil)
			_, err := g.Run(context.Background(), []byte(`{}`))
			f := failure(t, err)
			if f.Outcome != c.outcome {
				t.Fatalf("outcome = %s, want %s", f.Outcome, c.outcome)
			}
			if c.message != "" && f.Message != c.message {
				t.Fatalf("message = %q, want %q", f.Message, c.message)
			}
			if f.Field != c.field {
				t.Fatalf("field = %q, want %q", f.Field, c.field)
			}
			if !strings.Contains(f.Detail, c.detail) {
				t.Fatalf("detail %q does not contain %q", f.Detail, c.detail)
			}
			assertEmpty(t, root)
		})
	}
}

func TestTimeoutKillsTheGeneratorAndItsChildren(t *testing.T) {
	dir := t.TempDir()
	pidFile := filepath.Join(dir, "child.pid")
	// A child that outlives its parent is the whole case: keytool runs under the interpreter.
	g, root := fake(t, "sleep 60 &\necho $! > "+pidFile+"\nwait\n", 300*time.Millisecond, nil)

	_, err := g.Run(context.Background(), []byte(`{}`))
	if f := failure(t, err); f.Outcome != telemetry.Timeout {
		t.Fatalf("outcome = %s, want timeout", f.Outcome)
	}

	raw, err := os.ReadFile(pidFile)
	if err != nil {
		t.Fatal(err)
	}
	pid, err := strconv.Atoi(strings.TrimSpace(string(raw)))
	if err != nil {
		t.Fatal(err)
	}
	deadline := time.Now().Add(2 * time.Second)
	for syscall.Kill(pid, 0) == nil {
		if time.Now().After(deadline) {
			_ = syscall.Kill(pid, syscall.SIGKILL)
			t.Fatalf("child %d survived the timeout", pid)
		}
		time.Sleep(20 * time.Millisecond)
	}
	assertEmpty(t, root)
}

func TestAKilledGeneratorLeavesNothingOutsideItsWorkDirectory(t *testing.T) {
	outside := t.TempDir()
	t.Setenv("TMPDIR", outside)
	t.Setenv("DATABASE_URL", "postgres://secret")
	seen := filepath.Join(t.TempDir(), "env")

	// Writes where Python's tempfile and keytool would (TMPDIR and HOME), records the environment
	// it was given, then hangs until it is killed.
	g, root := fake(t, `
env > `+seen+`
mkdir -p "$TMPDIR/staging/keys" && echo secret > "$TMPDIR/staging/keys/keystore.properties"
echo key > "$HOME/.keystore"
sleep 60
`, 500*time.Millisecond, nil)

	_, err := g.Run(context.Background(), []byte(`{}`))
	if f := failure(t, err); f.Outcome != telemetry.Timeout {
		t.Fatalf("outcome = %s, want timeout", f.Outcome)
	}

	assertEmpty(t, root)
	assertEmpty(t, outside)

	env, err := os.ReadFile(seen)
	if err != nil {
		t.Fatal(err)
	}
	if strings.Contains(string(env), "DATABASE_URL") {
		t.Fatal("the server's environment leaked into the generator")
	}
	for _, want := range []string{"LANG=C.UTF-8", "PYTHONDONTWRITEBYTECODE=1", "PYTHONIOENCODING=utf-8", "TMPDIR=" + root} {
		if !strings.Contains(string(env), want) {
			t.Fatalf("the generator's environment lacks %s:\n%s", want, env)
		}
	}
}

func TestAVisitorWhoLeavesIsAbandonedNotFailed(t *testing.T) {
	g, root := fake(t, "sleep 60\n", time.Minute, nil)

	running, cancelRunning := context.WithCancel(context.Background())
	result := make(chan error, 1)
	go func() {
		_, err := g.Run(running, []byte(`{}`))
		result <- err
	}()

	// Wait until the first request holds the only slot, then queue one that gives up.
	for len(g.slots) == 0 {
		time.Sleep(10 * time.Millisecond)
	}
	queued, cancelQueued := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancelQueued()
	_, err := g.Run(queued, []byte(`{}`))
	if f := failure(t, err); f.Outcome != telemetry.AbandonedQueue {
		t.Fatalf("queued request: outcome = %s, want abandoned_queue", f.Outcome)
	}

	cancelRunning()
	if f := failure(t, <-result); f.Outcome != telemetry.AbandonedRunning {
		t.Fatalf("running request: outcome = %s, want abandoned_running", f.Outcome)
	}
	assertEmpty(t, root)
}

func TestShutdownStopsRunningGenerations(t *testing.T) {
	stopping, stop := context.WithCancel(context.Background())
	g, root := fake(t, "sleep 60\n", time.Minute, stopping)

	go func() {
		for len(g.slots) == 0 {
			time.Sleep(10 * time.Millisecond)
		}
		stop()
	}()
	_, err := g.Run(context.Background(), []byte(`{}`))
	if f := failure(t, err); f.Outcome != telemetry.InternalError {
		t.Fatalf("outcome = %s, want internal_error", f.Outcome)
	}
	assertEmpty(t, root)
}

func TestProjectFileNameIsHeaderSafe(t *testing.T) {
	cases := map[string]string{
		"TrailNotes":           "TrailNotes.zip",
		`Evil"; filename=x.sh`: "Evilfilenamexsh.zip",
		"":                     "project.zip",
		"Café":                 "Caf.zip",
	}
	for in, want := range cases {
		if got := ProjectFileName(in); got != want {
			t.Errorf("ProjectFileName(%q) = %q, want %q", in, got, want)
		}
	}
}
