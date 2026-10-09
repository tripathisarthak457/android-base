//go:build unix

package generate

import (
	"bytes"
	"errors"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func traceback(lines ...string) string { return strings.Join(lines, "\n") + "\n" }

func TestSignatureIsTheExceptionAndTheGeneratorsInnermostFrame(t *testing.T) {
	g := &Generator{sourceDir: "/app/generator"}
	cases := []struct {
		name   string
		output string
		want   string
	}{
		{
			name: "raised in the standard library",
			output: traceback(
				"Traceback (most recent call last):",
				`  File "/app/generator/genkit/core/headless.py", line 50, in run`,
				"    result = _generate(raw, target_key, output)",
				"             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^",
				`  File "/app/generator/genkit/targets/android/target.py", line 118, in finish`,
				"    text = (project / name).read_text()",
				`  File "/usr/lib/python3.12/pathlib.py", line 1027, in read_text`,
				"    with self.open(mode='r') as f:",
				"FileNotFoundError: [Errno 2] No such file or directory: '/tmp/android-4121/app/build.gradle.kts'",
			),
			want: "FileNotFoundError: [Errno 2] No such file or directory: '/tmp/android-4121/app/build.gradle.kts'\n" +
				"genkit.targets.android.target.finish",
		},
		{
			name: "chained: the exception that reached the top",
			output: traceback(
				"Traceback (most recent call last):",
				`  File "/app/generator/genkit/core/spec.py", line 6, in read`,
				`    {}["missing"]`,
				"KeyError: 'missing'",
				"",
				"The above exception was the direct cause of the following exception:",
				"",
				"Traceback (most recent call last):",
				`  File "/app/generator/genkit/core/pipeline.py", line 8, in build`,
				`    raise ValueError("no layout") from error`,
				"ValueError: no layout",
				"Note: added by add_note",
			),
			want: "ValueError: no layout\ngenkit.core.pipeline.build",
		},
		{
			name: "a syntax error names no function",
			output: traceback(
				"Traceback (most recent call last):",
				`  File "/app/generator/generate_headless.py", line 3, in <module>`,
				"    from genkit.core import headless",
				`  File "/app/generator/genkit/core/headless.py", line 24`,
				"    def (",
				"        ^",
				"SyntaxError: invalid syntax",
			),
			want: "SyntaxError: invalid syntax\ngenkit.core.headless",
		},
		{
			name: "no frame of the generator's own",
			output: traceback(
				"Traceback (most recent call last):",
				`  File "<frozen runpy>", line 198, in _run_module_as_main`,
				`  File "/usr/lib/python3.12/encodings/__init__.py", line 99, in search_function`,
				"LookupError: unknown encoding",
			),
			want: "LookupError: unknown encoding\n__init__.search_function",
		},
		{
			name:   "a directory that only starts like the generator's",
			output: traceback(`  File "/app/generator-old/genkit/core/spec.py", line 1, in read`, "KeyError: 'x'"),
			want:   "KeyError: 'x'\nspec.read",
		},
		{
			name:   "no traceback",
			output: "Rendering templates\nFatal Python error: Segmentation fault\n\n",
			want:   "Fatal Python error: Segmentation fault",
		},
		{name: "nothing", output: "", want: ""},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := g.signature(c.output); got != c.want {
				t.Fatalf("signature = %q, want %q", got, c.want)
			}
		})
	}
}

func TestACrashIsNamedByHowItEndedAndWhatItLastWrote(t *testing.T) {
	g := &Generator{sourceDir: "/app/generator"}
	killed := errors.New("signal: killed")
	if got := g.crashSignature(killed, bytes.NewBufferString("Writing the project\n")); got != "signal: killed\nWriting the project" {
		t.Fatalf("got %q", got)
	}
	if got := g.crashSignature(killed, &bytes.Buffer{}); got != "signal: killed" {
		t.Fatalf("got %q", got)
	}
	if got := g.crashSignature(nil, bytes.NewBufferString("garbage\n")); got != "garbage" {
		t.Fatalf("got %q", got)
	}
}

// Python reports the files it imported by their real path, so a generator reached through a
// symlink still has frames of its own.
func TestFramesAreMatchedThroughASymlinkedGeneratorDirectory(t *testing.T) {
	real := t.TempDir()
	if err := os.WriteFile(filepath.Join(real, "generate_headless.py"), nil, 0o600); err != nil {
		t.Fatal(err)
	}
	link := filepath.Join(t.TempDir(), "generator")
	if err := os.Symlink(real, link); err != nil {
		t.Fatal(err)
	}
	g, err := New(t.Context(), Options{PythonBin: "/bin/sh", GeneratorDir: link}, TargetEntry{Key: "android"}, Rules{})
	if err != nil {
		t.Fatal(err)
	}
	resolved, err := filepath.EvalSymlinks(real)
	if err != nil {
		t.Fatal(err)
	}
	output := traceback(`  File "`+resolved+`/genkit/core/pipeline.py", line 8, in build`, "KeyError: 'x'")
	if got := g.signature(output); got != "KeyError: 'x'\ngenkit.core.pipeline.build" {
		t.Fatalf("signature = %q", got)
	}
}
