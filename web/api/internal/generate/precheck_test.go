package generate

import (
	"encoding/json"
	"errors"
	"fmt"
	"slices"
	"strings"
	"testing"
)

func list(n int, item func(int) string) string {
	items := make([]string, n)
	for i := range items {
		items[i] = item(i)
	}
	return "[" + strings.Join(items, ",") + "]"
}

func TestPrecheckRefusesWhatWouldBeExpensiveOrBroken(t *testing.T) {
	module := func(i int) string { return fmt.Sprintf(`"m%d"`, i) }
	feature := func(i int) string { return fmt.Sprintf(`"f%d"`, i) }
	keystore := func(i int) string { return fmt.Sprintf(`{"name": "k%d"}`, i) }

	cases := []struct {
		name  string
		body  string
		field string
	}{
		{"not JSON", `{"app_name": `, ""},
		{"not an object", `["android"]`, ""},
		{"trailing data", `{} {}`, ""},
		{"invalid UTF-8", "{\"app_name\": \"\xff\"}", ""},
		{"too many feature modules", `{"feature_modules": ` + list(13, module) + `}`, "feature_modules"},
		{"too many features", `{"features": ` + list(65, feature) + `}`, "features"},
		{"too many languages", `{"languages": ` + list(11, module) + `}`, "languages"},
		{"too many keystores (v1)", `{"keystores": ` + list(5, keystore) + `}`, "keystores"},
		{"too many keystores (v2)", `{"android": {"keystores": ` + list(5, keystore) + `}}`, "android.keystores"},
		{"identifier too long", `{"app_name": "` + strings.Repeat("a", 121) + `"}`, "app_name"},
		{"module name too long", `{"feature_modules": ["` + strings.Repeat("a", 121) + `"]}`, "feature_modules"},
		{"string too long", `{"android": {"api_base_urls": {"dev": "https://` + strings.Repeat("a", 250) + `"}}}`, "android.api_base_urls.dev"},
		// A trailing newline passes Python's `$` and would break the generated Kotlin.
		{"newline in a package", `{"package_name": "com.example.app\n"}`, "package_name"},
		{"NUL in a module", `{"feature_modules": ["orders\u0000"]}`, "feature_modules"},
		{"too deep", `{"a": {"b": {"c": {"d": {"e": {"f": {}}}}}}}`, "a.b.c.d.e.f"},
		{"too many fields", `{` + fieldList(65) + `}`, ""},
		{"platform of another target", `{"schema": 2, "platform": "ios"}`, "platform"},
		{"platform that is not a string", `{"platform": 7}`, "platform"},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			_, err := Precheck([]byte(c.body), "android", Rules{})
			var refused *PrecheckError
			if !errors.As(err, &refused) {
				t.Fatalf("want a *PrecheckError, got %v", err)
			}
			if refused.Field != c.field {
				t.Fatalf("field = %q, want %q (%s)", refused.Field, c.field, refused.Message)
			}
		})
	}
}

func fieldList(n int) string {
	fields := make([]string, n)
	for i := range fields {
		fields[i] = fmt.Sprintf(`"field%d": 1`, i)
	}
	return strings.Join(fields, ", ")
}

func TestPrecheckNamesTheSameFieldEveryTime(t *testing.T) {
	body := []byte(`{"languages": ` + list(11, func(i int) string { return fmt.Sprintf(`"l%d"`, i) }) +
		`, "app_name": "` + strings.Repeat("a", 121) + `", "feature_modules": ["a\u0007"]}`)
	for range 20 {
		var refused *PrecheckError
		if _, err := Precheck(body, "android", Rules{}); !errors.As(err, &refused) || refused.Field != "app_name" {
			t.Fatalf("got %v, want the first faulty field in order, app_name", err)
		}
	}
}

func TestPrecheckRefusesAnOversizedBodyAsTooLarge(t *testing.T) {
	body := []byte(`{"app_name": "` + strings.Repeat("a", 32<<10) + `"}`)
	if _, err := Precheck(body, "android", Rules{}); !errors.Is(err, ErrTooLarge) {
		t.Fatalf("want ErrTooLarge, got %v", err)
	}
}

func TestPrecheckTakesItsLimitsFromTheCatalogue(t *testing.T) {
	body := []byte(`{"feature_modules": ["a", "b", "c"]}`)
	if _, err := Precheck(body, "android", Rules{FeatureModulesMax: 2}); err == nil {
		t.Fatal("three modules passed a limit of two")
	}
	if _, err := Precheck(body, "android", Rules{FeatureModulesMax: 3}); err != nil {
		t.Fatalf("three modules failed a limit of three: %v", err)
	}
	features := []byte(`{"features": ["network", "auth"]}`)
	if _, err := Precheck(features, "android", Rules{FeaturesMax: 1}); err == nil {
		t.Fatal("two features passed a limit of one")
	}
}

func TestPrecheckAcceptsRealSpecs(t *testing.T) {
	cases := map[string]string{
		"v1 flat": `{"app_name": "Trail Notes", "package_name": "com.example.trailnotes", "min_sdk": 26,
			"features": ["network", "auth"], "feature_modules": ["trips"], "languages": ["es", "hi"],
			"keystores": [{"name": "release", "alias": "upload", "store_password": "p@ss word", "key_password": "x"}]}`,
		"v2 nested": `{"schema": 2, "platform": "android", "app_name": "Trail Notes", "display_name": "Café Trails",
			"package_name": "com.example.trailnotes", "architecture": "modular", "features": ["network"],
			"android": {"min_sdk": 26, "target_sdk": 37, "api_base_urls": {"dev": "https://dev.example.com/api/"},
			"keystores": [{"name": "release", "store_password": "secret", "common_name": "Jane Doe"}]}}`,
		"minimal": `{"app_name": "X", "package_name": "com.example.x"}`,
	}
	for name, body := range cases {
		t.Run(name, func(t *testing.T) {
			if _, err := Precheck([]byte(body), "android", Rules{}); err != nil {
				t.Fatal(err)
			}
		})
	}
}

func TestPrecheckReadsTheRecordedFieldsAndDropsSecrets(t *testing.T) {
	body := `{"schema": 2, "platform": "android", "app_name": "Trail Notes", "package_name": "com.example.trail",
		"architecture": "modular", "design_style": "Glass", "motion_style": "Calm",
		"features": ["network", "liquidglass"], "feature_modules": ["trips", "maps"], "languages": ["hi"],
		"android": {"min_sdk": 26, "target_sdk": 37,
			"keystores": [{"name": "release", "alias": "upload", "store_password": "hunter22", "common_name": "Jane Doe"}]}}`
	spec, err := Precheck([]byte(body), "android", Rules{})
	if err != nil {
		t.Fatal(err)
	}
	if spec.AppName != "Trail Notes" || spec.Architecture != "modular" || spec.DesignStyle != "Glass" ||
		spec.MinSDK != 26 || spec.TargetSDK != 37 || len(spec.FeatureModules) != 2 ||
		!slices.Equal(spec.Languages, []string{"hi"}) || !slices.Contains(spec.Features, "liquidglass") {
		t.Fatalf("fields not read: %+v", spec)
	}
	for _, secret := range []string{"hunter22", "Jane Doe", "upload"} {
		if strings.Contains(string(spec.Sanitised), secret) {
			t.Fatalf("the sanitised spec still holds %q: %s", secret, spec.Sanitised)
		}
	}
	var stored map[string]any
	if err := json.Unmarshal(spec.Sanitised, &stored); err != nil {
		t.Fatal(err)
	}
	keystores := stored["android"].(map[string]any)["keystores"].([]any)
	if len(keystores) != 1 || keystores[0].(map[string]any)["name"] != "release" {
		t.Fatalf("keystore names not kept: %v", keystores)
	}
}

func TestPrecheckReadsV1FlatAndroidFields(t *testing.T) {
	spec, err := Precheck([]byte(`{"min_sdk": 24, "target_sdk": 36, "keystores": [{"name": "debug", "key_password": "k"}]}`),
		"android", Rules{})
	if err != nil {
		t.Fatal(err)
	}
	if spec.MinSDK != 24 || spec.TargetSDK != 36 {
		t.Fatalf("got min %d target %d", spec.MinSDK, spec.TargetSDK)
	}
	if strings.Contains(string(spec.Sanitised), `"k"`) || !strings.Contains(string(spec.Sanitised), `"debug"`) {
		t.Fatalf("sanitised: %s", spec.Sanitised)
	}
}
