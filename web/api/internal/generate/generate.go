// Package generate runs the Python generator and hands back a zip. The generator is not
// reimplemented here: the spec is passed through as the bytes the client sent, and Python is the
// authority on what is valid. The contract is in generator/genkit/core/headless.py.
package generate

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/base-android/generator-api/internal/telemetry"
)

// Options configure every target's generator.
type Options struct {
	PythonBin    string
	GeneratorDir string
	// WorkRoot holds one directory per running generation and is removed at shutdown.
	WorkRoot string
	// Timeout bounds one generation from spawn to exit; queueing does not count.
	Timeout time.Duration
	// Concurrency is per target, so a slow target cannot starve another.
	Concurrency int
	// CatalogueFile is the catalogue precomputed at image build. Empty asks the generator at boot.
	CatalogueFile string
}

// Generator runs one Python process per request for one target, bounded by its own semaphore.
type Generator struct {
	Target TargetEntry
	Rules  Rules

	pythonBin string
	scriptDir string
	// sourceDir is scriptDir with symlinks resolved, which is how Python names the files in a
	// traceback.
	sourceDir string
	workRoot  string
	timeout   time.Duration
	slots     chan struct{}
	// stopping is cancelled at shutdown, so children still running are killed rather than orphaned.
	stopping context.Context
}

// New makes the generator for one catalogue target. stopping ends every generation still running
// when it is cancelled.
func New(stopping context.Context, opts Options, target TargetEntry, rules Rules) (*Generator, error) {
	dir, err := filepath.Abs(opts.GeneratorDir)
	if err != nil {
		return nil, fmt.Errorf("resolving the generator directory: %w", err)
	}
	if _, err := os.Stat(filepath.Join(dir, "generate_headless.py")); err != nil {
		return nil, fmt.Errorf("generate_headless.py not found in %s: %w", dir, err)
	}
	source, err := filepath.EvalSymlinks(dir)
	if err != nil {
		return nil, fmt.Errorf("resolving the generator directory: %w", err)
	}
	return &Generator{
		Target:    target,
		Rules:     rules,
		pythonBin: opts.PythonBin,
		scriptDir: dir,
		sourceDir: source,
		workRoot:  opts.WorkRoot,
		timeout:   opts.Timeout,
		slots:     make(chan struct{}, max(opts.Concurrency, 1)),
		stopping:  stopping,
	}, nil
}

// Result is the generator's success report.
type Result struct {
	ProjectName      string   `json:"project_name"`
	ElapsedMs        int      `json:"elapsed_ms"`
	ZipBytes         int64    `json:"zip_bytes"`
	ResolvedFeatures []string `json:"resolved_features"`
	Notes            []string `json:"notes"`
	Warnings         []string `json:"warnings"`
	IgnoredFields    []string `json:"ignored_fields"`
	GeneratorCommit  string   `json:"generator_commit"`
	// Keys that were asked for and not produced: no JDK on this host, or keytool refused them.
	// Optional: generate_headless.py does not report it yet.
	KeystoresSkipped []string `json:"keystores_skipped"`
}

// Output is a finished project. Close removes it and everything else the generator wrote.
type Output struct {
	Result
	ZipPath   string
	QueueTime time.Duration
	RunTime   time.Duration
	dir       string
}

func (o *Output) Close() error { return os.RemoveAll(o.dir) }

// Failure says why a generation produced no zip. Message is safe to show only for Invalid; Detail
// (a traceback or stderr) is for the error list and never reaches the visitor.
type Failure struct {
	Outcome telemetry.Outcome
	Message string
	Field   string
	Detail  string
	// Signature tells apart the bugs behind internal failures that share a Message: the exception
	// and the frame it was raised in, or the last thing a crashed child wrote. Empty when Message
	// is all there is to go on.
	Signature string
	QueueTime time.Duration
	RunTime   time.Duration
}

func (f *Failure) Error() string {
	if f.Message == "" {
		return string(f.Outcome)
	}
	return string(f.Outcome) + ": " + f.Message
}

// headlessReply is the one JSON object generate_headless.py prints, success or failure.
type headlessReply struct {
	Result
	OK     bool   `json:"ok"`
	Kind   string `json:"kind"`
	Error  string `json:"error"`
	Field  string `json:"field"`
	Detail string `json:"detail"`
}

// Run generates a project from spec, which is passed to the generator unchanged. Every failure is
// a *Failure. On success the caller must Close the output once it has streamed the zip.
func (g *Generator) Run(ctx context.Context, spec []byte) (*Output, error) {
	queued := time.Now()
	// Queue before doing any work, so a burst waits rather than starting fifty interpreters. The
	// request's own context still applies, so a visitor who gives up frees their place.
	select {
	case g.slots <- struct{}{}:
	case <-ctx.Done():
		return nil, &Failure{Outcome: telemetry.AbandonedQueue, QueueTime: time.Since(queued)}
	}
	defer func() { <-g.slots }()
	queue := time.Since(queued)

	dir, err := os.MkdirTemp(g.workRoot, g.Target.Key+"-")
	if err != nil {
		return nil, &Failure{Outcome: telemetry.InternalError, Message: "could not create a working directory",
			Detail: err.Error(), QueueTime: queue}
	}
	output, failure := g.run(ctx, dir, spec)
	if failure != nil {
		_ = os.RemoveAll(dir)
		failure.QueueTime = queue
		return nil, failure
	}
	output.QueueTime = queue
	return output, nil
}

func (g *Generator) run(ctx context.Context, dir string, spec []byte) (*Output, *Failure) {
	zipPath := filepath.Join(dir, "project.zip")

	runCtx, cancel := context.WithTimeout(ctx, g.timeout)
	defer cancel()
	stopRun := context.AfterFunc(g.stopping, cancel)
	defer stopRun()

	cmd := exec.CommandContext(runCtx, g.pythonBin, "generate_headless.py", "--target", g.Target.Key, zipPath)
	cmd.Dir = g.scriptDir
	cmd.Stdin = bytes.NewReader(spec)
	cmd.Env = childEnv(dir)
	ownProcessGroup(cmd)
	// A child that outlives the kill still holds the output pipes open; stop waiting on them.
	cmd.WaitDelay = 5 * time.Second

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &capped{limit: 1 << 20, buf: &stdout}
	cmd.Stderr = &capped{limit: 64 << 10, buf: &stderr}

	started := time.Now()
	runErr := cmd.Run()
	ran := time.Since(started)

	if runErr != nil {
		switch {
		case ctx.Err() != nil:
			return nil, &Failure{Outcome: telemetry.AbandonedRunning, RunTime: ran}
		case errors.Is(runCtx.Err(), context.DeadlineExceeded):
			return nil, &Failure{Outcome: telemetry.Timeout, Message: fmt.Sprintf("the generator ran past %s", g.timeout),
				Detail: stderr.String(), RunTime: ran}
		case runCtx.Err() != nil:
			// The server is shutting down: neither the visitor's spec nor the generator is at fault.
			return nil, &Failure{Outcome: telemetry.AbandonedRunning, Message: "the server stopped during generation", RunTime: ran}
		}
	}

	var reply headlessReply
	if err := json.Unmarshal(stdout.Bytes(), &reply); err != nil {
		return nil, &Failure{Outcome: telemetry.InternalError, Message: "the generator's reply was not JSON",
			Detail: describeExit(runErr, &stdout, &stderr), Signature: g.crashSignature(runErr, &stderr), RunTime: ran}
	}

	if !reply.OK {
		if reply.Kind == "invalid" {
			return nil, &Failure{Outcome: telemetry.Invalid, Message: reply.Error, Field: reply.Field, RunTime: ran}
		}
		detail, signature := reply.Detail, g.signature(reply.Detail)
		if detail == "" {
			detail, signature = describeExit(runErr, &stdout, &stderr), g.crashSignature(runErr, &stderr)
		}
		message := reply.Error
		if message == "" {
			message = "the generator failed"
		}
		return nil, &Failure{Outcome: telemetry.InternalError, Message: message, Detail: detail, Signature: signature,
			RunTime: ran}
	}

	info, err := os.Stat(zipPath)
	if runErr != nil || err != nil || info.Size() == 0 {
		return nil, &Failure{Outcome: telemetry.InternalError, Message: "the generator reported success without a zip",
			Detail: describeExit(runErr, &stdout, &stderr), RunTime: ran}
	}
	reply.ZipBytes = info.Size()
	return &Output{Result: reply.Result, ZipPath: zipPath, RunTime: ran, dir: dir}, nil
}

func describeExit(runErr error, stdout, stderr *bytes.Buffer) string {
	status := "exit 0"
	if runErr != nil {
		status = runErr.Error()
	}
	return fmt.Sprintf("%s\nstdout:\n%s\nstderr:\n%s", status, stdout.String(), stderr.String())
}

// childEnv is the whole environment the generator gets. Nothing is inherited: the server's own
// environment holds the database URL and the admin token. HOME and TMPDIR point into the work
// directory, so everything Python and keytool write (staging trees, keystores and their passwords)
// is under the one directory that is removed, whatever signal ends the process.
func childEnv(dir string) []string {
	env := []string{
		"PATH=" + os.Getenv("PATH"),
		"HOME=" + dir,
		"TMPDIR=" + dir,
		"LANG=C.UTF-8",
		"PYTHONUNBUFFERED=1",
		"PYTHONIOENCODING=utf-8",
		"PYTHONDONTWRITEBYTECODE=1",
	}
	if javaHome := os.Getenv("JAVA_HOME"); javaHome != "" {
		env = append(env, "JAVA_HOME="+javaHome)
	}
	// Python on Windows cannot initialise without SYSTEMROOT. The Python install manager's
	// `python` finds its interpreters under LOCALAPPDATA; without it, it downloads a fresh one into
	// the working directory and registers that one for the whole user account.
	if runtime.GOOS == "windows" {
		env = append(env, "SYSTEMROOT="+os.Getenv("SYSTEMROOT"))
		if localAppData := os.Getenv("LOCALAPPDATA"); localAppData != "" {
			env = append(env, "LOCALAPPDATA="+localAppData)
		}
	}
	return env
}

// capped keeps the first limit bytes and discards the rest, so a runaway child cannot grow the
// server's memory.
type capped struct {
	limit int
	buf   *bytes.Buffer
}

func (c *capped) Write(p []byte) (int, error) {
	if room := c.limit - c.buf.Len(); room > 0 {
		c.buf.Write(p[:min(room, len(p))])
	}
	return len(p), nil
}

// ProjectFileName is the download's file name, reduced to characters that are safe in a
// Content-Disposition header whatever the generator reported.
func ProjectFileName(projectName string) string {
	name := strings.Map(func(r rune) rune {
		switch {
		case r >= 'a' && r <= 'z', r >= 'A' && r <= 'Z', r >= '0' && r <= '9', r == '-', r == '_':
			return r
		}
		return -1
	}, projectName)
	if name == "" {
		name = "project"
	}
	return name + ".zip"
}
