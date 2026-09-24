//go:build !unix

package generate

import "os/exec"

// ownProcessGroup has no process groups to use here; cancelling kills the interpreter only.
func ownProcessGroup(*exec.Cmd) {}
