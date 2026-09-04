package httpapi

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

// The three pieces of this package that decide whether a request is served, refused, or
// attributed to the wrong person. Everything else here is plumbing that the handlers exercise;
// these three are the ones where being subtly wrong is silent.

func TestLimiterRefusesPastTheHourlyAllowance(t *testing.T) {
	l := newLimiter(3)

	for i := 1; i <= 3; i++ {
		if !l.allow("1.2.3.4") {
			t.Fatalf("request %d should have been allowed", i)
		}
	}
	if l.allow("1.2.3.4") {
		t.Fatal("the fourth request should have been refused")
	}
	if !l.allow("5.6.7.8") {
		t.Fatal("a different client should not be affected by another's allowance")
	}
}

func TestLimiterForgetsWhenTheWindowRolls(t *testing.T) {
	l := newLimiter(1)
	l.allow("1.2.3.4")

	// Reaching in rather than waiting an hour. The alternative is injecting a clock into a
	// four-field struct, which is more machinery than the thing being tested.
	l.windowAt = time.Now().Add(-time.Hour - time.Second)

	if !l.allow("1.2.3.4") {
		t.Fatal("the allowance should reset once the window has passed")
	}
}

func TestClientIPPrefersTheFirstForwardedHop(t *testing.T) {
	cases := []struct {
		name       string
		forwarded  string
		remoteAddr string
		want       string
	}{
		{"no header", "", "10.0.0.9:54321", "10.0.0.9"},
		{"single hop", "203.0.113.7", "10.0.0.9:54321", "203.0.113.7"},
		// The client is the leftmost entry; the rest are proxies. Taking the last would rate
		// limit the CDN as one visitor and let everyone through together.
		{"proxy chain", "203.0.113.7, 70.41.3.18, 150.172.238.178", "10.0.0.9:54321", "203.0.113.7"},
		{"padded", "  203.0.113.7  ,10.0.0.1", "10.0.0.9:54321", "203.0.113.7"},
		{"remote addr without a port", "", "10.0.0.9", "10.0.0.9"},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			r := httptest.NewRequest(http.MethodGet, "/api/health", nil)
			r.RemoteAddr = c.remoteAddr
			if c.forwarded != "" {
				r.Header.Set("X-Forwarded-For", c.forwarded)
			}
			if got := clientIP(r); got != c.want {
				t.Fatalf("clientIP() = %q, want %q", got, c.want)
			}
		})
	}
}

func TestRequireAdminRejectsEverythingButTheToken(t *testing.T) {
	s := &Server{adminToken: "0123456789abcdef0123456789abcdef"}
	guarded := s.requireAdmin(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
	})

	cases := []struct {
		name   string
		header string
		want   int
	}{
		{"correct token", "Bearer 0123456789abcdef0123456789abcdef", http.StatusOK},
		{"no header", "", http.StatusUnauthorized},
		{"wrong token", "Bearer 0123456789abcdef0123456789abcdee", http.StatusUnauthorized},
		{"missing scheme", "0123456789abcdef0123456789abcdef", http.StatusUnauthorized},
		// A prefix must not pass. subtle.ConstantTimeCompare returns 0 on a length mismatch,
		// but a hand-rolled HasPrefix check would let this through.
		{"prefix of the token", "Bearer 0123456789abcdef", http.StatusUnauthorized},
		{"empty bearer", "Bearer ", http.StatusUnauthorized},
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			r := httptest.NewRequest(http.MethodGet, "/api/admin/overview", nil)
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
