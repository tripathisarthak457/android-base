package generate

import (
	"bytes"
	"path/filepath"
	"regexp"
	"strings"
)

// framePattern matches a traceback frame, `  File "/app/generator/genkit/core/pipeline.py", line 99, in
// build`. A SyntaxError's frame names no function.
var framePattern = regexp.MustCompile(`^\s*File "([^"]+)", line \d+(?:, in (.+))?$`)

// signature names the bug behind a traceback, so its repeats can be grouped while other bugs are
// not, although the generator reports every one of them with the same message. It is the final
// exception line and the innermost frame of the generator's own code (the innermost frame of all
// when none is the generator's), as module and function. Line numbers and the generator's location
// on disk are left out, so an edit elsewhere in the file or another deployment does not split a
// group. Output that holds no traceback is reduced to its last line.
func (g *Generator) signature(output string) string {
	var innermost, own, exception, last string
	for line := range strings.Lines(output) {
		line = strings.TrimRight(line, "\r\n")
		if strings.TrimSpace(line) == "" {
			continue
		}
		last = line
		switch frame := framePattern.FindStringSubmatch(line); {
		case strings.HasPrefix(line, "Traceback (most recent call last):"):
			// A chained exception prints its cause first; only the last traceback raised to the top.
			innermost, own, exception = "", "", ""
		case frame != nil:
			name, isOwn := g.module(frame[1])
			if frame[2] != "" {
				name += "." + frame[2]
			}
			innermost, exception = name, ""
			if isOwn {
				own = name
			}
		case innermost != "" && exception == "" && !startsIndented(line):
			exception = line
		}
	}
	if exception == "" {
		return last
	}
	if own != "" {
		return exception + "\n" + own
	}
	return exception + "\n" + innermost
}

// crashSignature identifies a child that ended without an answer by how it ended and what it last
// wrote to stderr: the uncaught traceback of a crash at import, or the last line before a kill.
func (g *Generator) crashSignature(runErr error, stderr *bytes.Buffer) string {
	signature := g.signature(stderr.String())
	switch {
	case runErr == nil:
		return signature
	case signature == "":
		return runErr.Error()
	}
	return runErr.Error() + "\n" + signature
}

// module turns a frame's file into a dotted module name relative to the generator, or into the bare
// file name for the standard library and anything else outside it. Slashes never reach the
// fingerprint, which treats them as the start of a path that varies between runs.
func (g *Generator) module(file string) (name string, own bool) {
	if filepath.IsAbs(file) {
		if rel, err := filepath.Rel(g.sourceDir, file); err == nil && filepath.IsLocal(rel) {
			return strings.ReplaceAll(filepath.ToSlash(strings.TrimSuffix(rel, ".py")), "/", "."), true
		}
	}
	return strings.TrimSuffix(filepath.Base(file), ".py"), false
}

func startsIndented(line string) bool {
	return line[0] == ' ' || line[0] == '\t'
}
