package telemetry

import (
	"net/http"
	"testing"
)

func TestReadGeo(t *testing.T) {
	cases := []struct {
		name                  string
		country, region, city string
		want                  Geo
	}{
		{"plain", "IN", "MH", "Mumbai", Geo{"IN", "MH", "Mumbai"}},
		// Vercel percent-encodes the city (RFC 3986), so a plus is a plus.
		{"encoded", "BR", "SP", "S%C3%A3o%20Paulo", Geo{"BR", "SP", "São Paulo"}},
		{"plus is literal", "US", "CA", "A+B", Geo{"US", "CA", "A+B"}},
		{"lowercase country", "de", "be", "Berlin", Geo{"DE", "BE", "Berlin"}},
		{"no country, no location", "", "MH", "Mumbai", Geo{}},
		{"not a country code", "IND", "MH", "Mumbai", Geo{}},
		{"bad region", "IN", "Maharashtra", "Mumbai", Geo{"IN", "", "Mumbai"}},
		{"bad encoding", "IN", "MH", "%zz", Geo{"IN", "MH", ""}},
		{"control character", "IN", "MH", "Mum%0Abai", Geo{"IN", "MH", ""}},
		{"long city", "IN", "MH", "%E0%A4%AE" + repeat("%E0%A4%AE", 40), Geo{"IN", "MH", repeat("म", 21)}},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			h := http.Header{}
			h.Set("X-Vercel-IP-Country", c.country)
			h.Set("X-Vercel-IP-Country-Region", c.region)
			h.Set("X-Vercel-IP-City", c.city)
			if got := ReadGeo(h); got != c.want {
				t.Fatalf("got %+v, want %+v", got, c.want)
			}
		})
	}
}

func repeat(s string, n int) string {
	out := ""
	for range n {
		out += s
	}
	return out
}
