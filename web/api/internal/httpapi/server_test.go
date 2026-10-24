//go:build unix

package httpapi

import (
	"testing"

	"github.com/base-android/generator-api/internal/config"
)

// okScript is a generator that writes the spec it was given as the zip and reports success.
const okScript = `cat > "$3"
echo '{"ok": true, "target": "'"$2"'", "project_name": "TrailNotes", "elapsed_ms": 412, "zip_bytes": 1,
  "resolved_features": ["network", "auth", "settings", "liquidglass"], "notes": [],
  "warnings": ["The release key password was not used."], "ignored_fields": ["theme"],
  "generator_commit": "99c4edf", "keystores_skipped": ["release"]}'
`

// newTestServer runs script with /bin/sh as every enabled target's generator.
func newTestServer(t *testing.T, script string, configure func(*config.Config)) *testServer {
	t.Helper()
	return newServer(t, testCatalogue, "/bin/sh", script, configure)
}
