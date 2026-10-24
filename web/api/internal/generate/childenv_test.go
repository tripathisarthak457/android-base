package generate

import (
	"runtime"
	"slices"
	"strings"
	"testing"
)

func TestChildEnvPassesOnlyWhatPythonNeeds(t *testing.T) {
	t.Setenv("GENERATOR_API_ONLY", "kept in the server")
	t.Setenv("LOCALAPPDATA", `C:\Users\someone\AppData\Local`)
	env := childEnv(t.TempDir())
	if slices.ContainsFunc(env, func(entry string) bool { return strings.HasPrefix(entry, "GENERATOR_API_ONLY=") }) {
		t.Fatalf("the server's own environment reached the generator: %v", env)
	}
	// The Python install manager's `python` on Windows finds its interpreters under LOCALAPPDATA;
	// without it, it downloads and registers a fresh one in the working directory.
	want := runtime.GOOS == "windows"
	if got := slices.Contains(env, `LOCALAPPDATA=C:\Users\someone\AppData\Local`); got != want {
		t.Fatalf("LOCALAPPDATA passed = %v on %s, want %v: %v", got, runtime.GOOS, want, env)
	}
}
