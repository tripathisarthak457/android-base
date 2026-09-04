package httpapi

import (
	"crypto/subtle"
	"log/slog"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"
)

// recorder captures what a handler did so the middleware can log and record it. `http.ResponseWriter`
// tells you nothing about the status once the handler has returned.
type recorder struct {
	http.ResponseWriter
	status int
	bytes  int64
}

func (r *recorder) WriteHeader(status int) {
	r.status = status
	r.ResponseWriter.WriteHeader(status)
}

func (r *recorder) Write(b []byte) (int, error) {
	if r.status == 0 {
		r.status = http.StatusOK
	}
	n, err := r.ResponseWriter.Write(b)
	r.bytes += int64(n)
	return n, err
}

// Flush is forwarded so a streamed download is not buffered to completion by the middleware.
func (r *recorder) Flush() {
	if flusher, ok := r.ResponseWriter.(http.Flusher); ok {
		flusher.Flush()
	}
}

func (s *Server) observe(route string, next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		rec := &recorder{ResponseWriter: w}

		next(rec, r)

		duration := time.Since(started)
		if rec.status == 0 {
			rec.status = http.StatusOK
		}

		s.log.Info("request",
			"route", route, "method", r.Method, "status", rec.status,
			"ms", duration.Milliseconds(), "bytes", rec.bytes,
		)
		if s.store != nil {
			// Detached context: the request's is already cancelled by the time a download
			// finishes streaming, and losing the timing of exactly the slow requests would be
			// the opposite of useful.
			go s.store.RecordRequest(s.background, route, r.Method, rec.status, duration, rec.bytes)
		}
	}
}

// cors answers the browser. An explicit allow-list rather than `*`: the generate endpoint is a
// POST that returns a file, and there is no reason for any origin but the site to call it.
func (s *Server) cors(next http.HandlerFunc) http.HandlerFunc {
	allowed := make(map[string]bool, len(s.origins))
	for _, origin := range s.origins {
		allowed[origin] = true
	}

	return func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")
		if origin != "" && allowed[origin] {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Set("Vary", "Origin")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
			w.Header().Set("Access-Control-Max-Age", "86400")
		}
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next(w, r)
	}
}

// requireAdmin guards the portal with a bearer token compared in constant time.
//
// A shared token rather than accounts: there is one administrator, and a login system for one
// person is a login system whose password reset flow nobody will ever test.
func (s *Server) requireAdmin(next http.HandlerFunc) http.HandlerFunc {
	expected := []byte("Bearer " + s.adminToken)

	return func(w http.ResponseWriter, r *http.Request) {
		provided := []byte(r.Header.Get("Authorization"))
		if subtle.ConstantTimeCompare(provided, expected) != 1 {
			writeError(w, http.StatusUnauthorized, "Not authorised.")
			return
		}
		next(w, r)
	}
}

// limiter is a fixed-window counter per client, which is the right amount of machinery for a
// public endpoint that costs a CPU-second. A sliding window would be more accurate at the
// boundary and would need a dependency; the failure mode here is that somebody gets 2n requests
// across a window edge, which does not matter.
type limiter struct {
	mu       sync.Mutex
	perHour  int
	counts   map[string]int
	windowAt time.Time
}

func newLimiter(perHour int) *limiter {
	return &limiter{perHour: perHour, counts: map[string]int{}, windowAt: time.Now()}
}

func (l *limiter) allow(key string) bool {
	l.mu.Lock()
	defer l.mu.Unlock()

	if time.Since(l.windowAt) >= time.Hour {
		l.counts = map[string]int{}
		l.windowAt = time.Now()
	}
	if l.counts[key] >= l.perHour {
		return false
	}
	l.counts[key]++
	return true
}

func (s *Server) rateLimit(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if !s.limiter.allow(clientIP(r)) {
			w.Header().Set("Retry-After", "3600")
			writeError(w, http.StatusTooManyRequests,
				"That is a lot of projects. Try again in an hour, or run the generator locally — "+
					"it is the same code and there is no limit on it.")
			return
		}
		next(w, r)
	}
}

// clientIP trusts X-Forwarded-For only because this service is designed to sit behind Caddy on
// the same host. Exposed directly to the internet it would be spoofable, which is worth knowing
// before anyone moves it.
func clientIP(r *http.Request) string {
	if forwarded := r.Header.Get("X-Forwarded-For"); forwarded != "" {
		if comma := strings.IndexByte(forwarded, ','); comma > 0 {
			return strings.TrimSpace(forwarded[:comma])
		}
		return strings.TrimSpace(forwarded)
	}
	host, _, err := net.SplitHostPort(r.RemoteAddr)
	if err != nil {
		return r.RemoteAddr
	}
	return host
}

func logger() *slog.Logger {
	return slog.New(slog.NewJSONHandler(logWriter{}, &slog.HandlerOptions{Level: slog.LevelInfo}))
}
