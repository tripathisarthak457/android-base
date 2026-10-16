//go:build unix

package generate

import (
	"os/exec"
	"syscall"
)

// ownProcessGroup makes cancelling cmd kill everything it started. The generator runs keytool as
// a child, and killing only the interpreter would leave keytool holding a slot's worth of memory
// after the request has already been answered with a timeout.
func ownProcessGroup(cmd *exec.Cmd) {
	cmd.SysProcAttr = &syscall.SysProcAttr{Setpgid: true}
	cmd.Cancel = func() error { return syscall.Kill(-cmd.Process.Pid, syscall.SIGKILL) }
}
