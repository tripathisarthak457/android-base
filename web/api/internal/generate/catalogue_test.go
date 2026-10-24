package generate

import (
	"slices"
	"strings"
	"testing"
)

const sampleCatalogue = `{
  "version": 2, "generator_commit": "99c4edf",
  "targets": [{
    "key": "android", "title": "Android Native", "status": "available", "summary": "Compose.",
    "features": [{"key": "network", "title": "Network"}, {"key": "auth", "title": "Sign-in", "implies": ["network"]},
                 {"key": "settings", "title": "Settings"}, {"key": "liquidglass", "title": "Glass"}],
    "presets": [{"key": "lean", "features": ["settings"]},
                {"key": "standard", "features": ["network", "auth", "settings"]},
                {"key": "everything", "features": ["network", "auth", "settings", "liquidglass"]}],
    "defaults": {"preset": "standard", "architecture": "compact", "design_style": "Utility", "motion_style": "Standard"}
  }, {
    "key": "multiplatform", "title": "Compose Multiplatform", "status": "beta",
    "status_note": "Beta: the iOS app is not yet verified on a Mac", "summary": "Android and iOS.",
    "features": [], "presets": [], "defaults": {}
  }],
  "rules": {"spec_bytes_max": 32768, "features_max": 64, "feature_modules_max": 12, "languages_max": 10, "keystores_max": 4}
}`

func TestParseCatalogueReadsTargetsAndRules(t *testing.T) {
	c, err := ParseCatalogue([]byte(sampleCatalogue + "\n"))
	if err != nil {
		t.Fatal(err)
	}
	if c.GeneratorCommit != "99c4edf" || len(c.Targets) != 2 || c.Rules.FeatureModulesMax != 12 || c.Rules.FeaturesMax != 64 {
		t.Fatalf("parsed %+v", c)
	}
	if mp := c.Target("multiplatform"); mp == nil || mp.Status != "beta" || mp.StatusNote == "" {
		t.Fatalf("multiplatform entry: %+v", mp)
	}
	if c.Target("ios") != nil {
		t.Fatal("found a target the catalogue does not list")
	}
	if c.ETag == "" || c.ETag[0] != '"' || string(c.Raw) != sampleCatalogue {
		t.Fatalf("raw bytes or ETag wrong: %q", c.ETag)
	}
}

func TestParseCatalogueRefusesAnotherVersion(t *testing.T) {
	if _, err := ParseCatalogue([]byte(`{"version": 1, "targets": [{"key": "android"}]}`)); err == nil {
		t.Fatal("a version 1 catalogue was accepted")
	}
	if _, err := ParseCatalogue([]byte(`{"version": 2, "targets": []}`)); err == nil {
		t.Fatal("a catalogue without targets was accepted")
	}
}

func TestNearestPreset(t *testing.T) {
	c, err := ParseCatalogue([]byte(sampleCatalogue))
	if err != nil {
		t.Fatal(err)
	}
	android := c.Target("android")

	cases := []struct {
		name           string
		resolved       []string
		preset         string
		added, removed []string
	}{
		{"exactly a preset", []string{"network", "auth", "settings"}, "standard", nil, nil},
		{"one picked on top", []string{"network", "auth", "settings", "liquidglass"}, "everything", nil, nil},
		{"one taken out", []string{"network", "settings"}, "standard", nil, []string{"auth"}},
		{"nothing", nil, "lean", nil, []string{"settings"}},
		// Equally far from lean and standard: the default wins the tie.
		{"tie", []string{"settings", "network"}, "standard", nil, []string{"auth"}},
		{"glass on lean", []string{"settings", "liquidglass"}, "lean", []string{"liquidglass"}, nil},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			preset, added, removed := android.NearestPreset(c.resolved)
			if preset != c.preset || !slices.Equal(added, c.added) || !slices.Equal(removed, c.removed) {
				t.Fatalf("got %s +%v -%v, want %s +%v -%v", preset, added, removed, c.preset, c.added, c.removed)
			}
		})
	}
}

func TestResolveAddsWhatEachFeatureImplies(t *testing.T) {
	c, err := ParseCatalogue([]byte(sampleCatalogue))
	if err != nil {
		t.Fatal(err)
	}
	android := c.Target("android")

	cases := []struct {
		name      string
		requested []string
		want      []string
	}{
		{"an implied feature", []string{"auth", "settings"}, []string{"auth", "network", "settings"}},
		{"asked for as well", []string{"network", "auth"}, []string{"auth", "network"}},
		{"a key the catalogue does not list", []string{"retired", "liquidglass"}, []string{"liquidglass"}},
		{"nothing", nil, nil},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := android.Resolve(c.requested); !slices.Equal(got, c.want) {
				t.Fatalf("Resolve(%v) = %v, want %v", c.requested, got, c.want)
			}
		})
	}
}

func TestSmallestPresetFeaturesLeavesOutWhatCannotBeAskedFor(t *testing.T) {
	target := TargetEntry{
		Features: []FeatureInfo{{Key: "network"}, {Key: "firebase", ImpliedOnly: true}, {Key: "push"}},
		Presets: []PresetInfo{
			{Key: "everything", Features: []string{"network", "firebase", "push"}},
			{Key: "lean", Features: []string{"network", "firebase"}},
		},
	}
	if got := target.SmallestPresetFeatures(); !slices.Equal(got, []string{"network"}) {
		t.Fatalf("got %v", got)
	}
	// Marshalled into a spec, where null is not a list.
	if got := (&TargetEntry{}).SmallestPresetFeatures(); got == nil {
		t.Fatal("a target without presets gave nil")
	}
}

// optionCatalogue is sampleCatalogue with the android target declaring one option, whose `locked`
// is the JSON given.
func optionCatalogue(locked string) string {
	return strings.Replace(sampleCatalogue, `"defaults": {"preset": "standard"`,
		`"options": [{"key": "ui_components", "title": "UI components", "default": true,
		  "disables": [{"feature": "liquidglass", "reason": "Glass is drawn by components."}],
		  "hides": [{"field": "design_style", "reason": "Your design system decides."}],
		  "locked": `+locked+`}],
		"defaults": {"preset": "standard"`, 1)
}

const lockNote = "UI components come with the Android release."

func TestParseCatalogueReadsOptionsAndTheirLock(t *testing.T) {
	c, err := ParseCatalogue([]byte(optionCatalogue("null")))
	if err != nil {
		t.Fatal(err)
	}
	options := c.Target("android").Options
	if len(options) != 1 || options[0].Key != "ui_components" || !*options[0].Default || options[0].Locked != nil {
		t.Fatalf("options %+v", options)
	}
	if got := c.Target("multiplatform").Options; len(got) != 0 {
		t.Fatalf("a target without options has %+v", got)
	}

	c, err = ParseCatalogue([]byte(optionCatalogue(`{"value": false, "note": "` + lockNote + `"}`)))
	if err != nil {
		t.Fatal(err)
	}
	lock := c.Target("android").Options[0].Locked
	if lock == nil || *lock.Value || lock.Note != lockNote {
		t.Fatalf("lock %+v", lock)
	}
}

func TestParseCatalogueRefusesAnOptionItCannotEnforce(t *testing.T) {
	cases := map[string]string{
		"a lock that is not an object":    optionCatalogue(`true`),
		"a lock with a string value":      optionCatalogue(`{"value": "false", "note": "` + lockNote + `"}`),
		"a lock without a value":          optionCatalogue(`{"note": "` + lockNote + `"}`),
		"a lock without a note":           optionCatalogue(`{"value": false}`),
		"a lock with a blank note":        optionCatalogue(`{"value": false, "note": "  "}`),
		"an option without a default":     strings.Replace(optionCatalogue("null"), `"default": true,`, "", 1),
		"an option with a string default": strings.Replace(optionCatalogue("null"), `"default": true`, `"default": "true"`, 1),
		"an option without a key":         strings.Replace(optionCatalogue("null"), `"key": "ui_components",`, "", 1),
		"an option declared twice": strings.Replace(optionCatalogue("null"), `"options": [`,
			`"options": [{"key": "ui_components", "default": true, "locked": null}, `, 1),
	}
	for name, raw := range cases {
		t.Run(name, func(t *testing.T) {
			if _, err := ParseCatalogue([]byte(raw)); err == nil {
				t.Fatal("the catalogue was accepted, so the API would boot without enforcing it")
			}
		})
	}
}
