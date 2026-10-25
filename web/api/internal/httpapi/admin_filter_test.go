package httpapi

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/base-android/generator-api/internal/store"
)

func TestTheUIComponentsFilterIsWithOrWithout(t *testing.T) {
	now := time.Date(2026, 10, 25, 9, 0, 0, 0, time.UTC)
	for _, value := range []string{"with", "without", ""} {
		r := httptest.NewRequest(http.MethodGet, "/api/v1/admin/adoption?ui_components="+value, nil)
		f, err := parseFilter(r, now)
		if err != nil || f.UIComponents != value {
			t.Fatalf("%q: filter %+v, error %v", value, f, err)
		}
	}
	for _, value := range []string{"true", "false", "WITHOUT", "none", "with%27%20OR%201%3D1"} {
		r := httptest.NewRequest(http.MethodGet, "/api/v1/admin/adoption?ui_components="+value, nil)
		if _, err := parseFilter(r, now); err == nil {
			t.Fatalf("%q was taken", value)
		} else if bad, ok := err.(*badParam); !ok || bad.field != "ui_components" {
			t.Fatalf("%q: %v", value, err)
		}
	}
}

func TestTheExportPutsUIComponentsUnderItsColumn(t *testing.T) {
	row := csvRow(store.GenerationRow{ID: 7, UIComponents: "without", AppName: "Trail Notes", PackageName: "com.example.trail"})
	if len(row) != len(csvColumns) {
		t.Fatalf("%d cells for %d columns", len(row), len(csvColumns))
	}
	cell := map[string]string{}
	for i, column := range csvColumns {
		cell[column] = row[i]
	}
	if cell["ui_components"] != "without" || cell["id"] != "7" || cell["package_name"] != "com.example.trail" {
		t.Fatalf("cells %v", cell)
	}
}

func TestAnUnknownUIComponentsFilterIsABadRequestBeforeTheDatabase(t *testing.T) {
	ts := newUnrunnableServer(t, testCatalogue)
	auth := map[string]string{"Authorization": "Bearer " + ts.adminToken}
	for _, path := range []string{"overview", "adoption", "generations", "generations.csv"} {
		w := ts.do(http.MethodGet, "/api/v1/admin/"+path+"?ui_components=maybe", "", auth)
		e := decodeError(t, w.Body.String())
		if w.Code != http.StatusBadRequest || e.Code != "bad_request" || e.Field != "ui_components" || e.Error == "" {
			t.Fatalf("%s: got %d %+v", path, w.Code, e)
		}
	}
	if w := ts.do(http.MethodGet, "/api/v1/admin/adoption?ui_components=without", "", auth); w.Code != http.StatusServiceUnavailable {
		t.Fatalf("a known value with the database down: %d %s", w.Code, w.Body.String())
	}
}
