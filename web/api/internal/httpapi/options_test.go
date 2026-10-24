package httpapi

import (
	"net/http"
	"path/filepath"
	"strings"
	"testing"

	"github.com/base-android/generator-api/internal/telemetry"
)

const setupOnlyNote = "UI components come with the Android release. Until then this site generates setup-only projects: " +
	"the architecture, the theme skeleton and every build guard, without components."

// optionsCatalogue is testCatalogue with the android target declaring ui_components, locked as given.
func optionsCatalogue(locked string) string {
	return strings.Replace(testCatalogue, `"defaults": {"preset": "standard", "architecture"`,
		`"options": [{"key": "ui_components", "title": "UI components", "default": true,
		  "disables": [{"feature": "liquidglass", "reason": "Glass is drawn by components."}], "hides": [],
		  "locked": `+locked+`}],
		"defaults": {"preset": "standard", "architecture"`, 1)
}

// newUnrunnableServer serves catalogueJSON with a generator whose interpreter does not exist: a
// request that reached it would fail with a 500, so any other answer was given before a slot.
func newUnrunnableServer(t *testing.T, catalogueJSON string) *testServer {
	t.Helper()
	return newServer(t, catalogueJSON, filepath.Join(t.TempDir(), "no-python-here"), "", nil)
}

func TestALockedOptionRefusesEveryOtherValueBeforeTheGenerator(t *testing.T) {
	ts := newUnrunnableServer(t, optionsCatalogue(`{"value": false, "note": "`+setupOnlyNote+`"}`))
	for _, body := range []string{
		`{"schema": 2, "platform": "android", "app_name": "Trail Notes", "package_name": "com.example.trail", "features": ["auth"]}`,
		`{"schema": 2, "platform": "android", "app_name": "Trail Notes", "package_name": "com.example.trail", "ui_components": true}`,
		`{"schema": 2, "platform": "android", "app_name": "Trail Notes", "ui_components": null}`,
		`{"app_name": "Trail Notes"}`,
	} {
		for _, route := range []string{"/api/v1/android/generate", "/api/generate"} {
			w := ts.do(http.MethodPost, route, body, nil)
			if w.Code != http.StatusForbidden {
				t.Fatalf("%s %s: got %d %s, want 403", route, body, w.Code, w.Body.String())
			}
			e := decodeError(t, w.Body.String())
			if e.Code != "option_locked" || e.Field != "ui_components" || e.Error != setupOnlyNote {
				t.Fatalf("%s %s: body %+v", route, body, e)
			}
			g := onlyGeneration(t, ts.sink.last(t))
			if g.Outcome != telemetry.Invalid || g.FailureReason != setupOnlyNote || g.FailureField != "ui_components" || g.AppName != "Trail Notes" {
				t.Fatalf("%s %s: recorded %+v", route, body, g)
			}
		}
	}
	if ts.warm.Load() {
		t.Fatal("a refused request reached the generator")
	}
}

func TestAnOptionThatIsNotTrueOrFalseIsAnInvalidSpec(t *testing.T) {
	for _, locked := range []string{"null", `{"value": false, "note": "` + setupOnlyNote + `"}`} {
		ts := newUnrunnableServer(t, optionsCatalogue(locked))
		w := ts.do(http.MethodPost, "/api/v1/android/generate", `{"schema": 2, "ui_components": "no"}`, nil)
		if e := decodeError(t, w.Body.String()); w.Code != http.StatusBadRequest || e.Code != "invalid_spec" || e.Field != "ui_components" {
			t.Fatalf("locked %s: got %d %+v", locked, w.Code, e)
		}
		if ts.warm.Load() {
			t.Fatal("an invalid request reached the generator")
		}
	}
}
