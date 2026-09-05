// Package generate runs the Python generator and hands back a zip.
//
// The generator is not reimplemented here. It is the same code the CLI runs, invoked as a
// subprocess, so there is exactly one definition of what a generated project is. A Go port would
// be faster and would be wrong within a month — the two would drift, and the drift would show up
// as a project from the website that does not match the one from the terminal.
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
	"time"
)

// Request is the subset of the generator's spec a browser is allowed to set. The field names are
// the Python ones, because this struct is serialised straight to the subprocess's stdin.
type Request struct {
	AppName        string            `json:"app_name"`
	PackageName    string            `json:"package_name"`
	MinSDK         int               `json:"min_sdk,omitempty"`
	TargetSDK      int               `json:"target_sdk,omitempty"`
	CompileSDK     int               `json:"compile_sdk,omitempty"`
	VersionName    string            `json:"version_name,omitempty"`
	VersionCode    int               `json:"version_code,omitempty"`
	Features       []string          `json:"features"`
	FeatureModules []string          `json:"feature_modules"`
	APIBaseURLs    map[string]string `json:"api_base_urls,omitempty"`
	WebSocketURLs  map[string]string `json:"web_socket_urls,omitempty"`
	DeeplinkScheme string            `json:"deeplink_scheme,omitempty"`
	DeeplinkHost   string            `json:"deeplink_host,omitempty"`
	FontName       string            `json:"font_name,omitempty"`
	MonoFontName   string            `json:"mono_font_name,omitempty"`
	AccentColour   string            `json:"accent_colour,omitempty"`
	MotionStyle    string            `json:"motion_style,omitempty"`
	HapticsEnabled *bool             `json:"haptics_enabled,omitempty"`
	Keystores      []Keystore        `json:"keystores,omitempty"`
}

// Keystore asks the generator to create one signing key.
//
// These fields carry passwords. They go to the subprocess's stdin and nowhere else: never to the
// analytics store, never to a log line, and never back in the response — the only place they end
// up is inside the zip the visitor downloads, in `keystore.properties`, which is what makes the
// generated project buildable. Anything added here that widens that path is a credential leak.
//
// There is deliberately no field for an existing .jks path. The generator would read that file
// off this server's disk and put it in the zip.
type Keystore struct {
	Name          string `json:"name"`
	Alias         string `json:"alias"`
	StorePassword string `json:"store_password"`
	KeyPassword   string `json:"key_password"`
	CommonName    string `json:"common_name,omitempty"`
	Organisation  string `json:"organisation,omitempty"`
	Country       string `json:"country,omitempty"`
	ValidityDays  int    `json:"validity_days,omitempty"`
}

// Result is what the generator reports on success.
type Result struct {
	ProjectName    string   `json:"projectName"`
	PackageName    string   `json:"packageName"`
	Features       []string `json:"features"`
	FeatureModules []string `json:"featureModules"`
	// Which keys keytool actually produced. A name in Skipped means the project is complete but
	// that variant falls back to debug signing, which the visitor has to be told.
	KeystoresGenerated []string `json:"keystoresGenerated"`
	KeystoresSkipped   []string `json:"keystoresSkipped"`
	ZipPath            string   `json:"zipPath"`
	ZipBytes           int64    `json:"zipBytes"`
	ElapsedMillis      int      `json:"elapsedMillis"`
	Warnings           []string `json:"warnings"`
	IgnoredFields      []string `json:"ignoredFields"`
}

// InvalidRequestError is a rejection by the generator's own validation — a bad package name, an
// unknown feature. It is the caller's fault, so it becomes a 400 rather than a 500. Separating it
// from a genuine failure is what keeps the error dashboard meaningful: everything left in there
// is a bug in the generator rather than a typo by a visitor.
type InvalidRequestError struct{ Message string }

func (e *InvalidRequestError) Error() string { return e.Message }

// Generator runs one Python process per request, bounded by a semaphore.
type Generator struct {
	pythonBin  string
	scriptDir  string
	timeout    time.Duration
	concurrent chan struct{}
}

func New(pythonBin, generatorDir string, timeout time.Duration, maxConcurrent int) (*Generator, error) {
	absolute, err := filepath.Abs(generatorDir)
	if err != nil {
		return nil, fmt.Errorf("resolving the generator directory: %w", err)
	}
	script := filepath.Join(absolute, "generate_headless.py")
	if _, err := os.Stat(script); err != nil {
		return nil, fmt.Errorf("generate_headless.py not found at %s: %w", script, err)
	}
	return &Generator{
		pythonBin:  pythonBin,
		scriptDir:  absolute,
		timeout:    timeout,
		concurrent: make(chan struct{}, maxConcurrent),
	}, nil
}

// Run generates a project and returns the path to a zip and a cleanup function. The caller must
// call cleanup once it has finished streaming the file, whether or not it succeeded.
func (g *Generator) Run(ctx context.Context, request Request) (Result, func(), error) {
	noop := func() {}

	// Queue before doing any work, so a burst waits rather than starting fifty interpreters. The
	// request's own context still applies, so a visitor who gives up frees their slot.
	select {
	case g.concurrent <- struct{}{}:
		defer func() { <-g.concurrent }()
	case <-ctx.Done():
		return Result{}, noop, ctx.Err()
	}

	body, err := json.Marshal(request)
	if err != nil {
		return Result{}, noop, fmt.Errorf("encoding the request: %w", err)
	}

	workDir, err := os.MkdirTemp("", "androidgen-out-")
	if err != nil {
		return Result{}, noop, fmt.Errorf("creating a working directory: %w", err)
	}
	cleanup := func() { _ = os.RemoveAll(workDir) }

	output := filepath.Join(workDir, "project.zip")

	runCtx, cancel := context.WithTimeout(ctx, g.timeout)
	defer cancel()

	cmd := exec.CommandContext(runCtx, g.pythonBin, "generate_headless.py", output)
	cmd.Dir = g.scriptDir
	cmd.Stdin = bytes.NewReader(body)
	// Unbuffered, so a crash's traceback is not lost in a pipe that never flushes.
	cmd.Env = append(os.Environ(), "PYTHONUNBUFFERED=1", "PYTHONIOENCODING=utf-8")

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	runErr := cmd.Run()

	// The generator reports both success and rejection as JSON on stdout, so parse before
	// looking at the exit code: a non-zero exit with a parseable body is a validation failure,
	// and a non-zero exit without one is a crash.
	var failure struct {
		Error  string `json:"error"`
		Detail string `json:"detail"`
	}
	if json.Unmarshal(stdout.Bytes(), &failure) == nil && failure.Error != "" {
		cleanup()
		return Result{}, noop, &InvalidRequestError{Message: failure.Error}
	}

	if runErr != nil {
		cleanup()
		if errors.Is(runCtx.Err(), context.DeadlineExceeded) {
			return Result{}, noop, fmt.Errorf("the generator timed out after %s", g.timeout)
		}
		return Result{}, noop, fmt.Errorf(
			"the generator failed: %w; stderr: %s", runErr, truncate(stderr.String(), 2000),
		)
	}

	var result Result
	if err := json.Unmarshal(stdout.Bytes(), &result); err != nil {
		cleanup()
		return Result{}, noop, fmt.Errorf(
			"could not read the generator's output: %w; got: %s",
			err, truncate(stdout.String(), 500),
		)
	}
	if result.ZipPath == "" {
		cleanup()
		return Result{}, noop, errors.New("the generator reported success but produced no zip")
	}

	return result, cleanup, nil
}

// Catalogue asks the generator for its own option list. Called once at boot and cached, so the
// site's form and the generator's rules can never disagree.
func (g *Generator) Catalogue(ctx context.Context) (json.RawMessage, error) {
	runCtx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()

	cmd := exec.CommandContext(runCtx, g.pythonBin, "-m", "genkit.catalogue")
	cmd.Dir = g.scriptDir
	cmd.Env = append(os.Environ(), "PYTHONUNBUFFERED=1", "PYTHONIOENCODING=utf-8")

	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &stderr

	if err := cmd.Run(); err != nil {
		return nil, fmt.Errorf("reading the catalogue: %w; stderr: %s", err, truncate(stderr.String(), 1000))
	}
	if !json.Valid(stdout.Bytes()) {
		return nil, errors.New("the catalogue was not valid JSON")
	}
	return json.RawMessage(bytes.TrimSpace(stdout.Bytes())), nil
}

func truncate(s string, limit int) string {
	if len(s) <= limit {
		return s
	}
	return s[:limit] + "… (truncated)"
}
