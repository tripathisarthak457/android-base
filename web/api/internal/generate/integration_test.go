package generate

import (
	"archive/zip"
	"context"
	"encoding/json"
	"errors"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/base-android/generator-api/internal/telemetry"
)

// The real generator, through the same code path as a request. Runs when GENERATOR_DIR points at
// the repository's generator/.
func TestIntegrationRealGenerator(t *testing.T) {
	dir := os.Getenv("GENERATOR_DIR")
	if dir == "" {
		t.Skip("GENERATOR_DIR is not set")
	}
	python := os.Getenv("PYTHON_BIN")
	if python == "" {
		python = "python3"
	}
	root := t.TempDir()
	opts := Options{PythonBin: python, GeneratorDir: dir, WorkRoot: root, Timeout: 3 * time.Minute, Concurrency: 1}
	ctx := context.Background()

	catalogue, err := LoadCatalogue(ctx, opts)
	if err != nil {
		t.Fatal(err)
	}

	for _, key := range []string{"android", "multiplatform"} {
		t.Run(key, func(t *testing.T) {
			target := catalogue.Target(key)
			if target == nil {
				t.Skipf("the catalogue has no %s target yet", key)
			}
			g, err := New(ctx, opts, *target, catalogue.Rules)
			if err != nil {
				t.Fatal(err)
			}

			spec := mustJSON(t, map[string]any{
				"schema": 2, "platform": key, "app_name": "Integration", "package_name": "com.example.integration",
				"features": target.SmallestPresetFeatures(),
			})
			if _, err := Precheck(spec, key, catalogue.Rules); err != nil {
				t.Fatalf("the precheck refused a spec the generator accepts: %v", err)
			}
			out, err := g.Run(ctx, spec)
			var failure *Failure
			if errors.As(err, &failure) {
				t.Fatalf("%v\n%s", failure, failure.Detail)
			}
			if err != nil {
				t.Fatal(err)
			}
			if out.ProjectName == "" || out.GeneratorCommit == "" || len(out.ResolvedFeatures) == 0 {
				t.Fatalf("result %+v", out.Result)
			}
			assertRunnableZip(t, out.ZipPath)
			if err := out.Close(); err != nil {
				t.Fatal(err)
			}

			bad := mustJSON(t, map[string]any{
				"schema": 2, "platform": key, "app_name": "Integration", "package_name": "Not A Package",
			})
			_, err = g.Run(ctx, bad)
			if !errors.As(err, &failure) || failure.Outcome != telemetry.Invalid || failure.Field != "package_name" {
				t.Fatalf("an invalid package name: %v (field %q)", err, fieldOf(failure))
			}
		})
	}

	t.Run("v1 spec", func(t *testing.T) {
		target := catalogue.Target("android")
		if target == nil {
			t.Skip("no android target")
		}
		g, err := New(ctx, opts, *target, catalogue.Rules)
		if err != nil {
			t.Fatal(err)
		}
		// What the previous site and scripts send to /api/generate: no schema, Android fields flat.
		out, err := g.Run(ctx, []byte(`{"app_name": "Legacy", "package_name": "com.example.legacy", "min_sdk": 26,
			"features": ["network", "settings"]}`))
		if err != nil {
			t.Fatal(err)
		}
		defer func() { _ = out.Close() }()
		if len(out.Notes) == 0 {
			t.Error("upgrading a v1 spec produced no note")
		}
	})

	entries, err := os.ReadDir(root)
	if err != nil {
		t.Fatal(err)
	}
	if len(entries) != 0 {
		t.Fatalf("generations left %v behind", entries)
	}
}

func mustJSON(t *testing.T, value any) []byte {
	t.Helper()
	raw, err := json.Marshal(value)
	if err != nil {
		t.Fatal(err)
	}
	return raw
}

func fieldOf(f *Failure) string {
	if f == nil {
		return ""
	}
	return f.Field
}

// assertRunnableZip checks the zip opens and its Gradle wrapper kept the executable bit, which the
// zip writer sets by hand and is the first thing a user runs.
func assertRunnableZip(t *testing.T, path string) {
	t.Helper()
	archive, err := zip.OpenReader(path)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = archive.Close() }()
	for _, file := range archive.File {
		if strings.HasSuffix(file.Name, "/gradlew") {
			if file.Mode()&0o111 == 0 {
				t.Fatalf("%s is not executable (%v)", file.Name, file.Mode())
			}
			return
		}
	}
	t.Fatalf("no gradlew among %d entries", len(archive.File))
}
