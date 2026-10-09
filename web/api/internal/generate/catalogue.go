package generate

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"os/exec"
	"time"
)

// Catalogue is what `genkit.core.catalogue --json` prints: the generator's own description of
// every target. The raw bytes are served to the website unchanged; the parsed fields are the few
// the API needs itself.
type Catalogue struct {
	Raw []byte `json:"-"`
	// ETag is a strong validator, fixed for the life of the process.
	ETag string `json:"-"`

	Version         int           `json:"version"`
	GeneratorCommit string        `json:"generator_commit"`
	Targets         []TargetEntry `json:"targets"`
	Rules           Rules         `json:"rules"`
}

// TargetEntry is one target of the catalogue.
type TargetEntry struct {
	Key        string        `json:"key"`
	Title      string        `json:"title"`
	Status     string        `json:"status"`
	StatusNote string        `json:"status_note,omitempty"`
	Summary    string        `json:"summary"`
	Features   []FeatureInfo `json:"features"`
	Presets    []PresetInfo  `json:"presets"`
	Defaults   Defaults      `json:"defaults"`
}

type FeatureInfo struct {
	Key   string `json:"key"`
	Title string `json:"title"`
	// ImpliedOnly features are pulled in by others and cannot be asked for directly.
	ImpliedOnly bool `json:"implied_only"`
}

// PresetInfo carries the preset's resolved feature list.
type PresetInfo struct {
	Key      string   `json:"key"`
	Features []string `json:"features"`
}

type Defaults struct {
	Preset       string `json:"preset"`
	Architecture string `json:"architecture"`
	DesignStyle  string `json:"design_style"`
	MotionStyle  string `json:"motion_style"`
}

// Rules are the limits the generator enforces. The precheck uses the same numbers, so it can never
// refuse a spec the generator would accept.
type Rules struct {
	SpecBytesMax      int `json:"spec_bytes_max"`
	FeaturesMax       int `json:"features_max"`
	FeatureModulesMax int `json:"feature_modules_max"`
	LanguagesMax      int `json:"languages_max"`
	KeystoresMax      int `json:"keystores_max"`
}

// Target returns the entry for key, or nil.
func (c *Catalogue) Target(key string) *TargetEntry {
	for i := range c.Targets {
		if c.Targets[i].Key == key {
			return &c.Targets[i]
		}
	}
	return nil
}

// LoadCatalogue reads the catalogue from file when one was precomputed at image build, and asks the
// generator otherwise.
func LoadCatalogue(ctx context.Context, opts Options) (*Catalogue, error) {
	var raw []byte
	var err error
	if opts.CatalogueFile != "" {
		raw, err = os.ReadFile(opts.CatalogueFile)
		if err != nil {
			return nil, fmt.Errorf("reading the catalogue: %w", err)
		}
	} else {
		raw, err = runCatalogue(ctx, opts)
		if err != nil {
			return nil, err
		}
	}
	return ParseCatalogue(raw)
}

// ParseCatalogue checks the parts of the catalogue the API relies on.
func ParseCatalogue(raw []byte) (*Catalogue, error) {
	raw = bytes.TrimSpace(raw)
	var c Catalogue
	if err := json.Unmarshal(raw, &c); err != nil {
		return nil, fmt.Errorf("the catalogue is not valid JSON: %w", err)
	}
	if c.Version != 2 {
		return nil, fmt.Errorf("the catalogue is version %d; this API reads version 2", c.Version)
	}
	if len(c.Targets) == 0 {
		return nil, errors.New("the catalogue lists no targets")
	}
	c.Raw = raw
	c.ETag = fmt.Sprintf(`"%x"`, sha256.Sum256(raw))
	return &c, nil
}

func runCatalogue(ctx context.Context, opts Options) ([]byte, error) {
	home, err := os.MkdirTemp(opts.WorkRoot, "catalogue-")
	if err != nil {
		return nil, fmt.Errorf("creating a working directory: %w", err)
	}
	defer func() { _ = os.RemoveAll(home) }()

	runCtx, cancel := context.WithTimeout(ctx, 60*time.Second)
	defer cancel()

	cmd := exec.CommandContext(runCtx, opts.PythonBin, "-m", "genkit.core.catalogue", "--json")
	cmd.Dir = opts.GeneratorDir
	cmd.Env = childEnv(home)
	var stdout, stderr bytes.Buffer
	cmd.Stdout = &stdout
	cmd.Stderr = &capped{limit: 16 << 10, buf: &stderr}

	if err := cmd.Run(); err != nil {
		return nil, fmt.Errorf("reading the catalogue: %w; stderr: %s", err, stderr.String())
	}
	return stdout.Bytes(), nil
}

// NearestPreset names the preset whose features differ least from resolved, and the differences:
// what was added on top of it and what was taken out. Ties go to the catalogue's default preset, then
// to catalogue order. The spec carries no preset of its own, so this is how the admin tells a
// feature someone picked from one that came with their starting point.
func (t *TargetEntry) NearestPreset(resolved []string) (preset string, added, removed []string) {
	have := make(map[string]bool, len(resolved))
	for _, key := range resolved {
		have[key] = true
	}
	best := -1
	for _, candidate := range t.Presets {
		inPreset := make(map[string]bool, len(candidate.Features))
		var plus, minus []string
		for _, key := range candidate.Features {
			inPreset[key] = true
			if !have[key] {
				minus = append(minus, key)
			}
		}
		for _, key := range resolved {
			if !inPreset[key] {
				plus = append(plus, key)
			}
		}
		distance := len(plus) + len(minus)
		if best < 0 || distance < best || distance == best && candidate.Key == t.Defaults.Preset {
			best, preset, added, removed = distance, candidate.Key, plus, minus
		}
	}
	return preset, added, removed
}
