package httpapi

import (
	"net/http"
	"net/http/httptest"
	"net/netip"
	"strings"
	"testing"
	"time"

	"github.com/base-android/generator-api/internal/config"
)

func TestLimiterRefusesPastTheHourlyAllowanceAndReportsTheFirstRefusal(t *testing.T) {
	l := newLimiter(2)
	for i := 1; i <= 2; i++ {
		if ok, _ := l.allow("1.2.3.4"); !ok {
			t.Fatalf("request %d should have been allowed", i)
		}
	}
	if ok, first := l.allow("1.2.3.4"); ok || !first {
		t.Fatalf("third request: ok %v first %v, want a first refusal", ok, first)
	}
	if ok, first := l.allow("1.2.3.4"); ok || first {
		t.Fatalf("fourth request: ok %v first %v, want a repeated refusal", ok, first)
	}
	if ok, _ := l.allow("5.6.7.8"); !ok {
		t.Fatal("a different client should not be affected by another's allowance")
	}
}

func TestLimiterForgetsWhenTheWindowRolls(t *testing.T) {
	l := newLimiter(1)
	l.allow("1.2.3.4")
	// Reaching in rather than waiting an hour; a clock for a four-field struct is more machinery
	// than the thing being tested.
	l.windowAt = time.Now().Add(-time.Hour - time.Second)
	if ok, _ := l.allow("1.2.3.4"); !ok {
		t.Fatal("the allowance should reset once the window has passed")
	}
}

func TestClientAddressUnderEachProxyPolicy(t *testing.T) {
	none := config.ProxyPolicy{}
	vercel := config.ProxyPolicy{Hops: 1, Vercel: true}
	twoHops := config.ProxyPolicy{Hops: 2}

	cases := []struct {
		name       string
		policy     config.ProxyPolicy
		forwarded  []string
		realIP     string
		remoteAddr string
		want       string
	}{
		{"no proxy ignores the header a client can forge", none, []string{"198.51.100.1"}, "", "203.0.113.7:5100", "203.0.113.7"},
		{"vercel overwrites the header with the client", vercel, []string{"203.0.113.7"}, "", "10.0.0.9:5100", "203.0.113.7"},
		{"vercel without the header falls back to x-real-ip", vercel, nil, "203.0.113.8", "10.0.0.9:5100", "203.0.113.8"},
		// Behind two appending proxies the client is second from the right; anything to its left
		// came from the client itself.
		{"two hops skips a forged entry", twoHops, []string{"6.6.6.6, 203.0.113.7, 10.0.0.2"}, "", "10.0.0.3:5100", "203.0.113.7"},
		{"two hops across header lines", twoHops, []string{"6.6.6.6", "203.0.113.7, 10.0.0.2"}, "", "10.0.0.3:5100", "203.0.113.7"},
		{"too few hops uses the connection", twoHops, []string{"203.0.113.7"}, "", "10.0.0.3:5100", "10.0.0.3"},
		{"garbage uses the connection", vercel, []string{"not-an-ip"}, "", "10.0.0.9:5100", "10.0.0.9"},
		{"IPv4-mapped IPv6 is IPv4", none, nil, "", "[::ffff:203.0.113.7]:5100", "203.0.113.7"},
		{"remote address without a port", none, nil, "", "203.0.113.7", "203.0.113.7"},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			r := httptest.NewRequest(http.MethodGet, "/api/v1/health", nil)
			r.RemoteAddr = c.remoteAddr
			for _, value := range c.forwarded {
				r.Header.Add("X-Forwarded-For", value)
			}
			if c.realIP != "" {
				r.Header.Set("X-Real-Ip", c.realIP)
			}
			if got := clientAddr(r, c.policy); got.String() != c.want {
				t.Fatalf("clientAddr() = %s, want %s", got, c.want)
			}
		})
	}
}

func TestClientKeyGroupsAnIPv6Slash64(t *testing.T) {
	cases := map[string]string{
		"203.0.113.7":              "203.0.113.7",
		"2001:db8:1:2::1":          "2001:db8:1:2::/64",
		"2001:db8:1:2:ffff:ffff::": "2001:db8:1:2::/64",
		"2001:db8:1:3::1":          "2001:db8:1:3::/64",
	}
	for addr, want := range cases {
		if got := clientKey(netip.MustParseAddr(addr)); got != want {
			t.Errorf("clientKey(%s) = %s, want %s", addr, got, want)
		}
	}
	if got := clientKey(netip.Addr{}); got != "unknown" {
		t.Errorf("an invalid address keyed as %q", got)
	}
}

func TestRequireAdminRejectsEverythingButTheToken(t *testing.T) {
	s := &Server{adminToken: "0123456789abcdef0123456789abcdef"}
	guarded := s.requireAdmin(func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(http.StatusOK) })

	cases := []struct {
		name   string
		header string
		want   int
	}{
		{"correct token", "Bearer 0123456789abcdef0123456789abcdef", http.StatusOK},
		{"no header", "", http.StatusUnauthorized},
		{"wrong token", "Bearer 0123456789abcdef0123456789abcdee", http.StatusUnauthorized},
		{"missing scheme", "0123456789abcdef0123456789abcdef", http.StatusUnauthorized},
		// A prefix must not pass: a hand-rolled HasPrefix check would let it through.
		{"prefix of the token", "Bearer 0123456789abcdef", http.StatusUnauthorized},
		{"empty bearer", "Bearer ", http.StatusUnauthorized},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			r := httptest.NewRequest(http.MethodGet, "/api/v1/admin/overview", nil)
			if c.header != "" {
				r.Header.Set("Authorization", c.header)
			}
			w := httptest.NewRecorder()
			guarded(w, r)
			if w.Code != c.want {
				t.Fatalf("status = %d, want %d", w.Code, c.want)
			}
		})
	}
}

func TestCORSAllowsTheSessionHeadersForTheSiteOnly(t *testing.T) {
	s := &Server{origins: map[string]bool{"http://localhost:3000": true}}
	handler := s.cors(func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(http.StatusOK) })

	preflight := httptest.NewRequest(http.MethodOptions, "/api/v1/android/generate", nil)
	preflight.Header.Set("Origin", "http://localhost:3000")
	w := httptest.NewRecorder()
	handler(w, preflight)
	if w.Code != http.StatusNoContent || w.Header().Get("Access-Control-Allow-Origin") != "http://localhost:3000" {
		t.Fatalf("preflight: %d %v", w.Code, w.Header())
	}
	allowed := w.Header().Get("Access-Control-Allow-Headers")
	for _, header := range []string{"Content-Type", "X-Session-Id", "X-Config-Ms", "X-Client"} {
		if !containsToken(allowed, header) {
			t.Errorf("%s is not allowed: %q", header, allowed)
		}
	}

	stranger := httptest.NewRequest(http.MethodOptions, "/api/v1/android/generate", nil)
	stranger.Header.Set("Origin", "https://evil.example")
	w = httptest.NewRecorder()
	handler(w, stranger)
	if w.Header().Get("Access-Control-Allow-Origin") != "" {
		t.Fatal("an unknown origin was allowed")
	}
	if w.Header().Get("Vary") != "Origin" {
		t.Fatal("a refusal that depends on Origin does not say so to caches")
	}
}

func containsToken(list, token string) bool {
	for _, item := range strings.Split(list, ",") {
		if strings.TrimSpace(item) == token {
			return true
		}
	}
	return false
}

func TestETagMatching(t *testing.T) {
	tag := `"abc"`
	for header, want := range map[string]bool{
		`"abc"`:          true,
		`W/"abc"`:        true,
		`"x", "abc"`:     true,
		`*`:              true,
		`"abcd"`:         false,
		``:               false,
		`"x" , W/"abc" `: true,
	} {
		if got := etagMatches(header, tag); got != want {
			t.Errorf("etagMatches(%q) = %v, want %v", header, got, want)
		}
	}
}
