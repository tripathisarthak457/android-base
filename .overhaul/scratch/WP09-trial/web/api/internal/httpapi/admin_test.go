//go:build unix

package httpapi

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func TestParseFilter(t *testing.T) {
	now := time.Date(2026, 10, 9, 15, 4, 5, 0, time.UTC)
	day := func(s string) time.Time {
		d, _ := time.Parse(time.DateOnly, s)
		return d
	}
	cases := []struct {
		query    string
		from, to string
		field    string
	}{
		{"", "2026-09-10", "2026-10-10", ""},
		{"from=2026-10-09&to=2026-10-09", "2026-10-09", "2026-10-10", ""},
		{"to=2026-09-30", "2026-09-01", "2026-10-01", ""},
		{"from=2025-09-05&to=2026-10-08", "2025-09-05", "2026-10-09", ""},
		{"from=2025-09-04&to=2026-10-09", "", "", "from"},
		{"from=2026-10-10&to=2026-10-09", "", "", "from"},
		{"from=yesterday", "", "", "from"},
		{"target=Android", "", "", "target"},
		{"outcome=crashed", "", "", "outcome"},
		{"country=India", "", "", "country"},
		{"device_class=phone", "", "", "device_class"},
	}
	for _, c := range cases {
		t.Run(c.query, func(t *testing.T) {
			r := httptest.NewRequest(http.MethodGet, "/api/v1/admin/overview?"+c.query, nil)
			f, err := parseFilter(r, now)
			if c.field != "" {
				bad, ok := err.(*badParam)
				if !ok || bad.field != c.field {
					t.Fatalf("want a bad %s, got %v", c.field, err)
				}
				return
			}
			if err != nil {
				t.Fatal(err)
			}
			if !f.From.Equal(day(c.from)) || !f.To.Equal(day(c.to)) {
				t.Fatalf("window [%s, %s), want [%s, %s)", f.From.Format(time.DateOnly), f.To.Format(time.DateOnly), c.from, c.to)
			}
		})
	}

	r := httptest.NewRequest(http.MethodGet, "/api/v1/admin/overview?country=in&source=+News.YCombinator.com+&outcome=ok&device_class=mobile", nil)
	f, err := parseFilter(r, now)
	if err != nil {
		t.Fatal(err)
	}
	if f.Country != "IN" || f.Source != "news.ycombinator.com" || f.Outcome != "ok" || f.DeviceClass != "mobile" {
		t.Fatalf("filters %+v", f)
	}
	if got := windowOf(f); got != (window{From: "2026-09-10", To: "2026-10-09"}) {
		t.Fatalf("window %+v", got)
	}
}

func TestFormulaSafeCells(t *testing.T) {
	cells := formulaSafe([]string{"=HYPERLINK(\"x\")", "+1", "-1", "@SUM(A1)", "\tx", "plain", "", "a=b"})
	want := []string{"'=HYPERLINK(\"x\")", "'+1", "'-1", "'@SUM(A1)", "'\tx", "plain", "", "a=b"}
	for i := range want {
		if cells[i] != want[i] {
			t.Errorf("cell %d = %q, want %q", i, cells[i], want[i])
		}
	}
}

func TestAdminSeparatesAuthFromAnUnreachableDatabase(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	auth := map[string]string{"Authorization": "Bearer " + ts.adminToken}

	paths := []string{
		"overview", "funnel", "series", "adoption", "failures", "audience", "durations",
		"generations", "generations.csv", "errors", "feedback", "health",
	}
	for _, path := range paths {
		if w := ts.do(http.MethodGet, "/api/v1/admin/"+path, "", nil); w.Code != http.StatusUnauthorized {
			t.Errorf("%s without a token: %d", path, w.Code)
		}
		w := ts.do(http.MethodGet, "/api/v1/admin/"+path, "", auth)
		if path == "health" {
			// Route health needs the database; the process half answers regardless.
			if w.Code != http.StatusServiceUnavailable {
				t.Errorf("health: %d", w.Code)
			}
			continue
		}
		if w.Code != http.StatusServiceUnavailable || decodeError(t, w.Body.String()).Code != "unavailable" {
			t.Errorf("%s with the database down: %d %s", path, w.Code, w.Body.String())
		}
	}

	if w := ts.do(http.MethodGet, "/api/v1/admin/overview?outcome=crashed", "", auth); w.Code != http.StatusBadRequest {
		t.Errorf("a bad filter: %d", w.Code)
	}
	if w := ts.do(http.MethodPost, "/api/v1/admin/feedback/update", `{"id": 1, "status": "done"}`, auth); w.Code != http.StatusBadRequest {
		t.Errorf("an unknown status: %d", w.Code)
	}
	if w := ts.do(http.MethodPost, "/api/v1/admin/errors/resolve", `{"id": 1, "resolved": true}`, auth); w.Code != http.StatusServiceUnavailable {
		t.Errorf("resolving with the database down: %d", w.Code)
	}

	// The admin's own reads stay out of route health.
	for _, batch := range ts.sink.batches {
		for _, request := range batch.Requests {
			if strings.HasPrefix(request.Route, "admin.") {
				t.Fatalf("an admin request was recorded: %+v", request)
			}
		}
	}
}

func TestAdminRoutesAreAbsentWithoutADatabase(t *testing.T) {
	ts := newTestServer(t, okScript, nil)
	ts.store = nil
	handler := ts.Routes()
	r := httptest.NewRequest(http.MethodGet, "/api/v1/admin/overview", nil)
	w := httptest.NewRecorder()
	handler.ServeHTTP(w, r)
	if w.Code != http.StatusNotFound {
		t.Fatalf("got %d", w.Code)
	}
}
