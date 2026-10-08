package telemetry

import (
	"net/http"
	"net/url"
	"regexp"
	"strings"
	"unicode"

	"github.com/base-android/generator-api/internal/clip"
)

// Geo is the coarse location Vercel's edge attaches to every request. The address it was derived
// from is never seen by this service.
type Geo struct {
	Country string
	Region  string
	City    string
}

var (
	countryCode = regexp.MustCompile(`^[A-Z]{2}$`)
	regionCode  = regexp.MustCompile(`^[A-Z0-9]{1,3}$`)
)

// ReadGeo decodes Vercel's `x-vercel-ip-*` headers. Only call it behind Vercel: anywhere else the
// headers come from the client and say whatever it likes.
func ReadGeo(h http.Header) Geo {
	var geo Geo
	if country := strings.ToUpper(h.Get("X-Vercel-IP-Country")); countryCode.MatchString(country) {
		geo.Country = country
	} else {
		// A region or city without a country cannot be told apart from another country's.
		return geo
	}
	if region := strings.ToUpper(h.Get("X-Vercel-IP-Country-Region")); regionCode.MatchString(region) {
		geo.Region = region
	}
	geo.City = city(h.Get("X-Vercel-IP-City"))
	return geo
}

// city decodes the header Vercel percent-encodes (RFC 3986, so "+" is a literal plus, not a space).
func city(raw string) string {
	decoded, err := url.PathUnescape(raw)
	if err != nil {
		return ""
	}
	decoded = strings.TrimSpace(clip.String(decoded, 64))
	if strings.IndexFunc(decoded, unicode.IsControl) >= 0 {
		return ""
	}
	return decoded
}
