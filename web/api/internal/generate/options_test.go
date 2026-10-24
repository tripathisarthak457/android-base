package generate

import (
	"errors"
	"testing"
)

func optionTarget(t *testing.T, locked string) *TargetEntry {
	t.Helper()
	c, err := ParseCatalogue([]byte(optionCatalogue(locked)))
	if err != nil {
		t.Fatal(err)
	}
	return c.Target("android")
}

// checkBody prechecks body as an android spec and checks its options against target.
func checkBody(t *testing.T, target *TargetEntry, body string) error {
	t.Helper()
	spec, err := Precheck([]byte(body), "android", Rules{})
	if err != nil {
		t.Fatalf("precheck refused %s: %v", body, err)
	}
	return target.CheckOptions(spec)
}

func TestCheckOptionsRefusesAValueThatIsNotTrueOrFalse(t *testing.T) {
	for _, target := range []*TargetEntry{optionTarget(t, "null"), optionTarget(t, `{"value": false, "note": "`+lockNote+`"}`)} {
		for _, body := range []string{`{"ui_components": "no"}`, `{"ui_components": 0}`, `{"ui_components": "false"}`, `{"ui_components": [false]}`} {
			var refused *PrecheckError
			if err := checkBody(t, target, body); !errors.As(err, &refused) || refused.Field != "ui_components" {
				t.Fatalf("%s: got %v, want a refusal on ui_components", body, err)
			}
		}
	}
}

func TestCheckOptionsAcceptsEveryValueWhileUnlocked(t *testing.T) {
	target := optionTarget(t, "null")
	for _, body := range []string{`{}`, `{"ui_components": true}`, `{"ui_components": false}`, `{"ui_components": null}`} {
		if err := checkBody(t, target, body); err != nil {
			t.Fatalf("%s: %v", body, err)
		}
	}
}

func TestCheckOptionsHoldsALockedOptionToItsValue(t *testing.T) {
	target := optionTarget(t, `{"value": false, "note": "`+lockNote+`"}`)
	if err := checkBody(t, target, `{"ui_components": false}`); err != nil {
		t.Fatalf("the locked value was refused: %v", err)
	}
	// Absent and null both mean the default, which is the value the lock forbids.
	for _, body := range []string{`{}`, `{"ui_components": true}`, `{"ui_components": null}`, `{"schema": 2, "features": ["auth"]}`} {
		var locked *OptionLockedError
		err := checkBody(t, target, body)
		if !errors.As(err, &locked) {
			t.Fatalf("%s: got %v, want the lock", body, err)
		}
		if locked.Field != "ui_components" || locked.Note != lockNote || err.Error() != lockNote {
			t.Fatalf("%s: refusal %+v", body, locked)
		}
	}
}

func TestCheckOptionsLeavesATargetWithoutOptionsToItsGenerator(t *testing.T) {
	c, err := ParseCatalogue([]byte(optionCatalogue(`{"value": false, "note": "` + lockNote + `"}`)))
	if err != nil {
		t.Fatal(err)
	}
	spec, err := Precheck([]byte(`{"ui_components": "anything"}`), "multiplatform", Rules{})
	if err != nil {
		t.Fatal(err)
	}
	if err := c.Target("multiplatform").CheckOptions(spec); err != nil {
		t.Fatalf("the generator, not the API, refuses a field the target does not have: %v", err)
	}
}

func TestLockedValuesAreWhatASelftestMustAskFor(t *testing.T) {
	if got := optionTarget(t, "null").LockedValues(); len(got) != 0 {
		t.Fatalf("an unlocked target asks for %v", got)
	}
	got := optionTarget(t, `{"value": false, "note": "`+lockNote+`"}`).LockedValues()
	if value, ok := got["ui_components"]; len(got) != 1 || !ok || value {
		t.Fatalf("locked values %v", got)
	}
}
