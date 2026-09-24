//go:build unix

package generate

import (
	"context"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"syscall"
	"testing"
	"time"
)

func TestTimeoutKillsTheGeneratorsChildren(t *testing.T) {
	dir := t.TempDir()
	pidFile := filepath.Join(dir, "child.pid")
	// Run by sh rather than Python: a child that outlives its parent is the whole case.
	script := "sleep 60 &\necho $! > " + pidFile + "\nwait\n"
	if err := os.WriteFile(filepath.Join(dir, "generate_headless.py"), []byte(script), 0o600); err != nil {
		t.Fatal(err)
	}
	generator, err := New("/bin/sh", dir, 300*time.Millisecond, 1)
	if err != nil {
		t.Fatal(err)
	}

	if _, _, err := generator.Run(context.Background(), Request{}); err == nil ||
		!strings.Contains(err.Error(), "timed out") {
		t.Fatalf("want a timeout, got %v", err)
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
}
