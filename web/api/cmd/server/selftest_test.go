package main

import (
	"encoding/json"
	"strings"
	"testing"

	"github.com/base-android/generator-api/internal/generate"
)

const selftestCatalogue = `{
  "version": 2, "generator_commit": "99c4edf",
  "targets": [{
    "key": "android", "title": "Android Native", "status": "available", "summary": "Compose.",
    "features": [{"key": "network", "title": "Network"}, {"key": "settings", "title": "Settings"}],
    "presets": [{"key": "lean", "features": ["settings"]}, {"key": "standard", "features": ["network", "settings"]}],
    "defaults": {"preset": "standard"},
    "options": [{"key": "ui_components", "default": true, "locked": LOCKED}]
  }],
  "rules": {"spec_bytes_max": 32768}
}`

func selftestTarget(t *testing.T, locked string) (*generate.TargetEntry, generate.Rules) {
	t.Helper()
	c, err := generate.ParseCatalogue([]byte(strings.Replace(selftestCatalogue, "LOCKED", locked, 1)))
	if err != nil {
		t.Fatal(err)
	}
	return c.Target("android"), c.Rules
}

func TestTheSelftestSpecIsOneTheRouteAccepts(t *testing.T) {
	for _, locked := range []string{"null", `{"value": false, "note": "UI components come with the Android release."}`} {
		target, rules := selftestTarget(t, locked)
		body, err := selftestSpec(*target)
		if err != nil {
			t.Fatal(err)
		}
		spec, err := generate.Precheck(body, target.Key, rules)
		if err == nil {
			err = target.CheckOptions(spec)
		}
		if err != nil {
			t.Fatalf("locked %s: the route would refuse the selftest's spec %s: %v", locked, body, err)
		}
	}
}

func TestTheSelftestSendsTheLockedValueAndNothingElseNew(t *testing.T) {
	target, _ := selftestTarget(t, `{"value": false, "note": "UI components come with the Android release."}`)
	body, err := selftestSpec(*target)
	if err != nil {
		t.Fatal(err)
	}
	var fields map[string]any
	if err := json.Unmarshal(body, &fields); err != nil {
		t.Fatal(err)
	}
	if value, sent := fields["ui_components"]; !sent || value != false {
		t.Fatalf("the selftest asked for %v, not the locked value", fields)
	}

	unlocked, _ := selftestTarget(t, "null")
	body, err = selftestSpec(*unlocked)
	if err != nil {
		t.Fatal(err)
	}
	if strings.Contains(string(body), "ui_components") {
		t.Fatalf("an unlocked selftest names the option: %s", body)
	}
}
