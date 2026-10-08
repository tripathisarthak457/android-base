//go:build unix

package httpapi

import (
	"encoding/json"
	"net/http"
	"strings"
	"testing"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/store"
	"github.com/base-android/generator-api/internal/telemetry"
)

func TestTargetsListTheCatalogueAndWhatThisDeploymentGenerates(t *testing.T) {
	ts := newTestServer(t, okScript, func(cfg *config.Config) { cfg.EnableTargets = []string{"android"} })
	w := ts.do(http.MethodGet, "/api/v1/targets", "", nil)
	var targets []targetInfo
	if err := json.Unmarshal(w.Body.Bytes(), &targets); err != nil {
		t.Fatal(err)
	}
	if len(targets) != 3 {
		t.Fatalf("got %+v", targets)
	}
	want := []targetInfo{
		{Key: "android", Title: "Android Native", Status: "available", Summary: "Compose.", Enabled: true},
		{Key: "multiplatform", Title: "Compose Multiplatform", Status: "beta",
			StatusNote: "Beta: the iOS app is not yet verified on a Mac", Summary: "Android and iOS.", Enabled: false},
		{Key: "ios", Title: "Swift", Status: "coming_soon", Summary: "SwiftUI.", Enabled: false},
	}
	for i := range want {
		if targets[i] != want[i] {
			t.Errorf("target %d = %+v, want %+v", i, targets[i], want[i])
		}
	}
}

func TestTheCatalogueIsRevalidatedWithItsETag(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	for _, path := range []string{"/api/v1/catalogue", "/api/options"} {
		w := ts.do(http.MethodGet, path, "", nil)
		if w.Code != http.StatusOK || w.Body.String() != testCatalogue || w.Header().Get("Cache-Control") != "no-cache" {
			t.Fatalf("%s: %d, cache %q", path, w.Code, w.Header().Get("Cache-Control"))
		}
		tag := w.Header().Get("ETag")
		w = ts.do(http.MethodGet, path, "", map[string]string{"If-None-Match": "W/" + tag})
		if w.Code != http.StatusNotModified || w.Body.Len() != 0 {
			t.Fatalf("%s with its ETag: %d, %d bytes", path, w.Code, w.Body.Len())
		}
	}
}

func TestHealthReportsTheDatabaseAndTargets(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	w := ts.do(http.MethodGet, "/api/v1/health", "", nil)
	var health map[string]any
	if err := json.Unmarshal(w.Body.Bytes(), &health); err != nil {
		t.Fatal(err)
	}
	if health["database"] != "connecting" || health["generator_commit"] != "99c4edf" {
		t.Fatalf("health %v", health)
	}
	if _, ok := health["telemetry"]; !ok {
		t.Fatal("the telemetry counters are missing")
	}
}

func TestEventsAreStoredWithTheVisit(t *testing.T) {
	ts := newTestServer(t, okScript, func(cfg *config.Config) { cfg.TrustedProxy = config.ProxyPolicy{Hops: 1, Vercel: true} })
	body := `{"session_id": "` + sessionID + `", "events": [
		{"name": "landed", "referrer": "https://www.reddit.com/r/androiddev", "utm_source": "reddit", "returning": true, "viewport": "sm"},
		{"name": "platform_selected", "target": "android"},
		{"name": "preset_chosen", "target": "android", "preset": "lean"}]}`
	w := ts.do(http.MethodPost, "/api/v1/events", body, map[string]string{
		"X-Client":                   "android-base-web/1a2b3c4",
		"User-Agent":                 "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1",
		"X-Forwarded-For":            "203.0.113.7",
		"X-Vercel-IP-Country":        "IN",
		"X-Vercel-IP-Country-Region": "MH",
		"X-Vercel-IP-City":           "Mumbai",
	})
	if w.Code != http.StatusNoContent {
		t.Fatalf("got %d %s", w.Code, w.Body.String())
	}
	batch := ts.sink.last(t)
	if len(batch.Events) != 3 || batch.Events[2].Value != "lean" || batch.Events[0].Client != telemetry.ClientWeb {
		t.Fatalf("events %+v", batch.Events)
	}
	if len(batch.Visits) != 1 {
		t.Fatalf("visits %+v", batch.Visits)
	}
	v := batch.Visits[0]
	if v.SessionID != sessionID || v.Landing.ReferrerOrigin != "https://www.reddit.com" || !v.Landing.Returning ||
		v.Geo != (telemetry.Geo{Country: "IN", Region: "MH", City: "Mumbai"}) ||
		v.Agent != (telemetry.Agent{Browser: "safari", OS: "ios", DeviceClass: "mobile"}) || v.VisitorHash == "" {
		t.Fatalf("visit %+v", v)
	}
}

func TestGeoHeadersAreIgnoredWhenNotBehindVercel(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	ts.do(http.MethodPost, "/api/v1/events", `{"session_id": "`+sessionID+`", "events": [{"name": "landed"}]}`,
		map[string]string{"X-Vercel-IP-Country": "IN"})
	if v := ts.sink.last(t).Visits[0]; v.Geo != (telemetry.Geo{}) {
		t.Fatalf("a client-supplied country was trusted: %+v", v.Geo)
	}
}

func TestInvalidEventsAreRefusedWhole(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	w := ts.do(http.MethodPost, "/api/v1/events",
		`{"session_id": "`+sessionID+`", "events": [{"name": "landed"}, {"name": "generated", "target": "android"}]}`, nil)
	if w.Code != http.StatusBadRequest {
		t.Fatalf("got %d", w.Code)
	}
	if batch := ts.sink.last(t); len(batch.Events)+len(batch.Visits) != 0 {
		t.Fatalf("part of a refused batch was stored: %+v", batch)
	}
}

func TestTheLegacyTrackRouteMapsOntoEvents(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	for step, want := range map[string]string{"landed": "landed", "configured": "generate_clicked", "downloaded": "download_started"} {
		w := ts.do(http.MethodPost, "/api/track", `{"step": "`+step+`"}`, nil)
		if w.Code != http.StatusNoContent {
			t.Fatalf("%s: %d", step, w.Code)
		}
		batch := ts.sink.last(t)
		if len(batch.Events) != 1 || batch.Events[0].Name != want || batch.Events[0].SessionID != "" {
			t.Fatalf("%s: events %+v", step, batch.Events)
		}
		if (step == "landed") != (len(batch.Visits) == 1) {
			t.Fatalf("%s: visits %+v", step, batch.Visits)
		}
	}
	if w := ts.do(http.MethodPost, "/api/track", `{"step": "generated"}`, nil); w.Code != http.StatusBadRequest {
		t.Fatalf("a client claimed a generation: %d", w.Code)
	}
}

func TestClientErrorsAreFingerprintedByMessageAndFrame(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	report := func(message, stack, url string) store.ErrorRecord {
		t.Helper()
		body, _ := json.Marshal(clientErrorReport{Message: message, Stack: stack, URL: url, Release: "1a2b3c4"})
		if w := ts.do(http.MethodPost, "/api/v1/client-error", string(body), nil); w.Code != http.StatusNoContent {
			t.Fatalf("got %d %s", w.Code, w.Body.String())
		}
		return ts.sink.last(t).Errors[0]
	}
	a := report("x is undefined", "TypeError: x is undefined\n    at LookStep (https://site/_next/static/chunks/12.js:4:2311)", "https://site/configure?draft=abc#look")
	b := report("x is undefined", "TypeError: x is undefined\n    at LookStep (https://site/_next/static/chunks/99.js:7:18)", "https://site/")
	c := report("x is undefined", "TypeError: x is undefined\n    at ReviewStep (https://site/_next/static/chunks/12.js:4:2311)", "https://site/")

	if a.Kind != "client" || a.Path != "/configure" || !strings.HasPrefix(a.Detail, "release 1a2b3c4") {
		t.Fatalf("record %+v", a)
	}
	if a.Fingerprint != b.Fingerprint {
		t.Fatal("the same error in a different build split into two groups")
	}
	if a.Fingerprint == c.Fingerprint {
		t.Fatal("errors thrown from different components were grouped together")
	}
}

func TestFeedbackIsRefusedWhileTheDatabaseIsUnreachable(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	w := ts.do(http.MethodPost, "/api/v1/feedback", `{"kind": "bug", "title": "Broken zip", "body": "The zip would not open at all."}`, nil)
	if w.Code != http.StatusServiceUnavailable {
		t.Fatalf("got %d", w.Code)
	}
}
