package generate

import (
	"slices"
	"testing"
)

const sampleCatalogue = `{
  "version": 2, "generator_commit": "99c4edf",
  "targets": [{
    "key": "android", "title": "Android Native", "status": "available", "summary": "Compose.",
    "features": [{"key": "network", "title": "Network"}, {"key": "auth", "title": "Sign-in"},
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
  "rules": {"spec_bytes_max": 32768, "feature_modules_max": 12, "languages_max": 10, "keystores_max": 4}
}`

func TestParseCatalogueReadsTargetsAndRules(t *testing.T) {
	c, err := ParseCatalogue([]byte(sampleCatalogue + "\n"))
	if err != nil {
		t.Fatal(err)
	}
	if c.GeneratorCommit != "99c4edf" || len(c.Targets) != 2 || c.Rules.FeatureModulesMax != 12 {
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
