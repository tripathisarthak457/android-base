package generate

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"strings"
	"unicode"
	"unicode/utf8"
)

// The precheck's own bounds. The generator enforces the real rules; these only make sure a request
// that would cost thirty seconds of CPU never reaches it (audit web-api H1).
const (
	defaultSpecBytes      = 32 << 10
	defaultFeatures       = 64
	defaultFeatureModules = 12
	defaultLanguages      = 10
	defaultKeystores      = 4
	maxDepth              = 6
	maxIdentifierBytes    = 120
	maxStringBytes        = 256
	// maxEntries bounds every other array and object, so a field added to the spec later is
	// covered without an edit here.
	maxEntries = 64
)

// identifierKeys are the fields that name things. Everything else (URLs, passwords, certificate
// subjects) gets the longer string bound.
var identifierKeys = map[string]bool{
	"schema": true, "platform": true, "architecture": true, "app_name": true, "display_name": true,
	"package_name": true, "features": true, "feature_modules": true, "design_style": true,
	"motion_style": true, "accent_colour": true, "secondary_colour": true, "tertiary_colour": true,
	"font_name": true, "mono_font_name": true, "languages": true, "version_name": true,
	"deeplink_scheme": true, "deeplink_host": true, "name": true, "alias": true, "country": true,
	"bundle_id": true, "deployment_target": true, "team_id": true,
}

// PrecheckError is a spec refused before it reached the generator. Message is written for the
// person who filled in the form.
type PrecheckError struct {
	Message string
	Field   string
}

func (e *PrecheckError) Error() string { return e.Message }

// ErrTooLarge is returned for a body over the spec size limit, which the route answers with 413.
var ErrTooLarge = errors.New("the spec is too large")

// Spec is what the API reads from a request for its own records. The generator still receives the
// exact bytes the client sent.
type Spec struct {
	AppName        string
	PackageName    string
	Architecture   string
	DesignStyle    string
	MotionStyle    string
	FontName       string
	AccentColour   string
	Features       []string
	FeatureModules []string
	Languages      []string
	MinSDK         int
	TargetSDK      int
	// Sanitised is the spec as it is stored: keystore entries are reduced to their names, so no
	// password or certificate subject reaches the database.
	Sanitised json.RawMessage
}

// SpecBytesLimit is the largest body the generate routes read.
func (r Rules) SpecBytesLimit() int { return positive(r.SpecBytesMax, defaultSpecBytes) }

// Precheck bounds a spec's size and shape before it takes a generator slot (MASTER-PLAN A10.1), and
// reads the fields the API records.
func Precheck(body []byte, target string, rules Rules) (Spec, error) {
	if len(body) > rules.SpecBytesLimit() {
		return Spec{}, ErrTooLarge
	}
	if !utf8.Valid(body) {
		return Spec{}, &PrecheckError{Message: "The spec is not valid UTF-8."}
	}

	decoder := json.NewDecoder(bytes.NewReader(body))
	decoder.UseNumber()
	var root any
	if err := decoder.Decode(&root); err != nil {
		return Spec{}, &PrecheckError{Message: "The spec is not valid JSON."}
	}
	if _, err := decoder.Token(); err != io.EOF {
		return Spec{}, &PrecheckError{Message: "The spec has data after the JSON object."}
	}
	object, ok := root.(map[string]any)
	if !ok {
		return Spec{}, &PrecheckError{Message: "The spec must be a JSON object."}
	}

	limits := map[string]int{
		"features":        positive(rules.FeaturesMax, defaultFeatures),
		"feature_modules": positive(rules.FeatureModulesMax, defaultFeatureModules),
		"languages":       positive(rules.LanguagesMax, defaultLanguages),
		"keystores":       positive(rules.KeystoresMax, defaultKeystores),
	}
	if err := check(object, "", "", 1, limits); err != nil {
		return Spec{}, err
	}

	if platform, present := object["platform"]; present && platform != target {
		return Spec{}, &PrecheckError{
			Message: fmt.Sprintf("This spec is for %v, but it was sent to the %s generator.", platform, target),
			Field:   "platform",
		}
	}

	return summarise(object)
}

func check(value any, path, key string, depth int, limits map[string]int) error {
	if depth > maxDepth {
		return &PrecheckError{Message: "The spec is nested too deeply.", Field: path}
	}
	switch v := value.(type) {
	case map[string]any:
		if len(v) > maxEntries {
			return &PrecheckError{Message: fmt.Sprintf("%s has more than %d fields.", describe(path), maxEntries), Field: path}
		}
		for name, child := range v {
			childPath := join(path, name)
			if len(name) > maxIdentifierBytes || strings.IndexFunc(name, unicode.IsControl) >= 0 {
				return &PrecheckError{Message: "The spec has a field name that is too long or not printable.", Field: path}
			}
			if err := check(child, childPath, name, depth+1, limits); err != nil {
				return err
			}
		}
	case []any:
		limit, named := limits[key]
		if !named {
			limit = maxEntries
		}
		if len(v) > limit {
			return &PrecheckError{Message: fmt.Sprintf("%s can have at most %d entries.", describe(path), limit), Field: path}
		}
		for _, child := range v {
			// Elements keep the array's key, so feature names get the identifier bound.
			if err := check(child, path, key, depth+1, limits); err != nil {
				return err
			}
		}
	case string:
		limit := maxStringBytes
		if identifierKeys[key] {
			limit = maxIdentifierBytes
		}
		if len(v) > limit {
			return &PrecheckError{Message: fmt.Sprintf("%s is longer than %d bytes.", describe(path), limit), Field: path}
		}
		if strings.IndexFunc(v, unicode.IsControl) >= 0 {
			return &PrecheckError{Message: fmt.Sprintf("%s contains a control character.", describe(path)), Field: path}
		}
	}
	return nil
}

func summarise(object map[string]any) (Spec, error) {
	android, _ := object["android"].(map[string]any)
	spec := Spec{
		AppName:        text(object["app_name"]),
		PackageName:    text(object["package_name"]),
		Architecture:   text(object["architecture"]),
		DesignStyle:    text(object["design_style"]),
		MotionStyle:    text(object["motion_style"]),
		FontName:       text(object["font_name"]),
		AccentColour:   text(object["accent_colour"]),
		Features:       texts(object["features"]),
		FeatureModules: texts(object["feature_modules"]),
		Languages:      texts(object["languages"]),
		// A v1 spec, and the v2 shorthand, keep the Android fields at the top level.
		MinSDK:    integer(android["min_sdk"], object["min_sdk"]),
		TargetSDK: integer(android["target_sdk"], object["target_sdk"]),
	}

	sanitised := withKeystoreNames(object)
	if android != nil {
		sanitised["android"] = withKeystoreNames(android)
	}

	encoded, err := json.Marshal(sanitised)
	if err != nil {
		return Spec{}, fmt.Errorf("encoding the sanitised spec: %w", err)
	}
	spec.Sanitised = encoded
	return spec, nil
}

// withKeystoreNames copies an object with its keystore entries reduced to the names of the keys that
// were asked for, which is all the admin needs to know about them.
func withKeystoreNames(object map[string]any) map[string]any {
	copied := make(map[string]any, len(object))
	for name, value := range object {
		copied[name] = value
	}
	entries, ok := object["keystores"].([]any)
	if !ok {
		return copied
	}
	names := make([]any, 0, len(entries))
	for _, entry := range entries {
		fields, _ := entry.(map[string]any)
		names = append(names, map[string]any{"name": text(fields["name"])})
	}
	copied["keystores"] = names
	return copied
}

func text(value any) string {
	s, _ := value.(string)
	return s
}

func texts(value any) []string {
	items, _ := value.([]any)
	out := make([]string, 0, len(items))
	for _, item := range items {
		if s, ok := item.(string); ok {
			out = append(out, s)
		}
	}
	return out
}

func integer(values ...any) int {
	for _, value := range values {
		if number, ok := value.(json.Number); ok {
			if n, err := number.Int64(); err == nil && n > 0 && n < 1000 {
				return int(n)
			}
		}
	}
	return 0
}

func join(path, name string) string {
	if path == "" {
		return name
	}
	return path + "." + name
}

func describe(path string) string {
	if path == "" {
		return "The spec"
	}
	return path
}

func positive(value, fallback int) int {
	if value > 0 {
		return value
	}
	return fallback
}
