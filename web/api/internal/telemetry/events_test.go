package telemetry

import (
	"encoding/json"
	"errors"
	"os"
	"testing"
)

type eventsFixture struct {
	Catalogue map[string][]string `json:"catalogue"`
	Accepted  []struct {
		Name   string          `json:"name"`
		Body   json.RawMessage `json:"body"`
		Stored struct {
			Events []struct {
				Name   string  `json:"name"`
				Target *string `json:"target"`
				Step   *string `json:"step"`
				Value  *string `json:"value"`
			} `json:"events"`
			Visit *struct {
				ReferrerOrigin *string `json:"referrer_origin"`
				UTMSource      *string `json:"utm_source"`
				UTMMedium      *string `json:"utm_medium"`
				UTMCampaign    *string `json:"utm_campaign"`
				Returning      bool    `json:"returning"`
				Viewport       *string `json:"viewport"`
			} `json:"visit"`
		} `json:"stored"`
	} `json:"accepted"`
	Refused []struct {
		Name string          `json:"name"`
		Body json.RawMessage `json:"body"`
	} `json:"refused"`
}

func text(p *string) string {
	if p == nil {
		return ""
	}
	return *p
}

func TestEventsContract(t *testing.T) {
	raw, err := os.ReadFile("../../testdata/contract/events.json")
	if err != nil {
		t.Fatal(err)
	}
	var fixture eventsFixture
	if err := json.Unmarshal(raw, &fixture); err != nil {
		t.Fatal(err)
	}
	known := Known(fixture.Catalogue)

	for _, c := range fixture.Accepted {
		t.Run("accepts "+c.Name, func(t *testing.T) {
			batch, err := DecodeEvents(c.Body)
			if err != nil {
				t.Fatal(err)
			}
			got, err := ValidateEvents(batch, known, "android-base.vercel.app")
			if err != nil {
				t.Fatal(err)
			}
			if len(got.Events) != len(c.Stored.Events) {
				t.Fatalf("%d events stored, want %d", len(got.Events), len(c.Stored.Events))
			}
			for i, want := range c.Stored.Events {
				e := got.Events[i]
				if e.Name != want.Name || e.Target != text(want.Target) || e.Step != text(want.Step) || e.Value != text(want.Value) {
					t.Errorf("event %d = %+v, want %+v", i, e, want)
				}
			}
			want := c.Stored.Visit
			switch {
			case want == nil && got.Landing != nil:
				t.Fatalf("a visit was stored: %+v", got.Landing)
			case want != nil && got.Landing == nil:
				t.Fatal("no visit was stored")
			case want != nil:
				l := got.Landing
				if l.ReferrerOrigin != text(want.ReferrerOrigin) || l.UTMSource != text(want.UTMSource) ||
					l.UTMMedium != text(want.UTMMedium) || l.UTMCampaign != text(want.UTMCampaign) ||
					l.Returning != want.Returning || l.Viewport != text(want.Viewport) {
					t.Fatalf("visit = %+v, want %+v", l, want)
				}
			}
		})
	}

	for _, c := range fixture.Refused {
		t.Run("refuses "+c.Name, func(t *testing.T) {
			batch, err := DecodeEvents(c.Body)
			if err == nil {
				_, err = ValidateEvents(batch, known, "android-base.vercel.app")
			}
			if !errors.Is(err, ErrInvalidEvents) {
				t.Fatalf("want ErrInvalidEvents, got %v", err)
			}
		})
	}
}

func TestReferrerOrigin(t *testing.T) {
	cases := []struct{ raw, want string }{
		{"https://www.reddit.com/r/androiddev/comments/abc", "https://www.reddit.com"},
		{"HTTPS://News.YCombinator.com/item?id=1", "https://news.ycombinator.com"},
		{"http://localhost:3000/", "http://localhost:3000"},
		// Moving between the site's own pages is not a referral.
		{"https://android-base.vercel.app/admin", ""},
		{"android-app://com.google.android.gm/", ""},
		{"javascript:alert(1)", ""},
		{"not a url", ""},
		{"", ""},
	}
	for _, c := range cases {
		if got := ReferrerOrigin(c.raw, "android-base.vercel.app"); got != c.want {
			t.Errorf("ReferrerOrigin(%q) = %q, want %q", c.raw, got, c.want)
		}
	}
}

func TestUTMValuesAreClippedOnARuneBoundary(t *testing.T) {
	long := ""
	for len(long) < 100 {
		long += "é"
	}
	got, err := ValidateEvents(EventBatch{
		SessionID: "3f1c2a9e-8b7d-4c6e-9a1b-2d3e4f5a6b7c",
		Events:    []EventIn{{Name: EventLanded, UTMCampaign: long}},
	}, Known{}, "")
	if err != nil {
		t.Fatal(err)
	}
	if n := len(got.Landing.UTMCampaign); n > 64 || n < 63 {
		t.Fatalf("campaign clipped to %d bytes", n)
	}
}
