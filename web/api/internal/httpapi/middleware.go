package httpapi

import (
	"context"
	"crypto/subtle"
	"io"
	"mime"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/base-android/generator-api/internal/store"
)

// recorder captures what a handler did so the middleware can log and record it. `http.ResponseWriter`
// tells you nothing about the status once the handler has returned.
type recorder struct {
	http.ResponseWriter
	status int
	bytes  int64
}

func (r *recorder) WriteHeader(status int) {
	if r.status == 0 {
		r.status = status
	}
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

// ReadFrom lets io.Copy reach the connection's own ReadFrom, so a zip goes out with sendfile instead
// of through a 32 KB buffer.
func (r *recorder) ReadFrom(src io.Reader) (int64, error) {
	if r.status == 0 {
		r.status = http.StatusOK
	}
	n, err := io.Copy(r.ResponseWriter, src)
	r.bytes += n
	return n, err
}

func (r *recorder) Flush() {
	if flusher, ok := r.ResponseWriter.(http.Flusher); ok {
		flusher.Flush()
	}
}

func (r *recorder) Unwrap() http.ResponseWriter { return r.ResponseWriter }

type batchKey struct{}

// recordings returns the batch the current request writes its telemetry into.
func recordings(r *http.Request) *store.Batch {
	if batch, ok := r.Context().Value(batchKey{}).(*store.Batch); ok {
		return batch
	}
	return &store.Batch{}
}

// telemetryWait bounds how long a finished request stays alive for its own telemetry write.
const telemetryWait = 2 * time.Second

// observe logs every request and records what its handler gathered, in one batch.
//
// The client never waits for the database: every response carries a Content-Length, so flushing it
// hands the whole response over before the write starts. The handler then stays alive until the
// batch is written (bounded), because the hosting platform freezes the process once the handler
// returns, and a write left in a goroutine at that point was never scheduled and silently lost.
func (s *Server) observe(route string, next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		rec := &recorder{ResponseWriter: w}
		batch := &store.Batch{}

		next(rec, r.WithContext(context.WithValue(r.Context(), batchKey{}, batch)))

		duration := time.Since(started)
		if rec.status == 0 {
			rec.status = http.StatusOK
		}
		s.log.Info("request",
			"route", route, "method", r.Method, "status", rec.status,
			"ms", duration.Milliseconds(), "bytes", rec.bytes,
		)
		if s.sink == nil {
			return
		}
		if recordsTimings(route) {
			batch.Requests = append(batch.Requests, store.Request{
				Route: route, Method: r.Method, Status: rec.status, Duration: duration, Bytes: rec.bytes,
			})
		}
		if batch.Empty() {
			return
		}
		rec.Flush()
		written := s.sink.Submit(batch)
		// Until the database is connected the batch can only queue, and waiting would hold every
		// request, and the edge's connection it came on, for the whole bound.
		if !s.store.Ready() {
			return
		}
		timer := time.NewTimer(telemetryWait)
		defer timer.Stop()
		select {
		case <-written:
		case <-timer.C:
		}
	}
}

// recordsTimings reports whether a route's requests count in the route health. The admin's own
// reads would fill the health it is reading, and probes polling health every few seconds would
// fill the table with rows that say nothing about visitors.
func recordsTimings(route string) bool {
	return !strings.HasPrefix(route, "admin.") && route != "health" && route != "legacy.health"
}

// cors answers the browser. An explicit allow-list rather than `*`: the generate endpoint is a POST
// that returns a file, and there is no reason for any origin but the site to call it.
func (s *Server) cors(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if origin := r.Header.Get("Origin"); origin != "" && s.origins[origin] {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Add("Vary", "Origin")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers",
				"Content-Type, Authorization, If-None-Match, X-Session-Id, X-Config-Ms, X-Client")
			w.Header().Set("Access-Control-Expose-Headers",
				"Content-Disposition, ETag, Retry-After, X-Project-Name, X-Generation-Ms, X-Keystores-Skipped, X-Next-Before")
			w.Header().Set("Access-Control-Max-Age", "86400")
		}
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next(w, r)
	}
}

// requireJSON refuses a POST that is not declared as JSON. A cross-site page can send a form or
// text/plain POST without asking; a JSON one needs a preflight, which the CORS allow-list then
// refuses.
func requireJSON(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		mediaType, _, err := mime.ParseMediaType(r.Header.Get("Content-Type"))
		if err != nil || mediaType != "application/json" {
			writeError(w, http.StatusUnsupportedMediaType, "unsupported_media_type", "Send the request as application/json.", "")
			return
		}
		next(w, r)
	}
}

// requireAdmin guards the portal with a bearer token compared in constant time. Only a missing or
// wrong token is a 401, so the portal can tell "log in again" from "the database is down".
//
// A shared token rather than accounts: there is one administrator, and a login system for one
// person is a login system whose password reset flow nobody will ever test.
func (s *Server) requireAdmin(next http.HandlerFunc) http.HandlerFunc {
	expected := []byte("Bearer " + s.adminToken)

	return func(w http.ResponseWriter, r *http.Request) {
		provided := []byte(r.Header.Get("Authorization"))
		// Config refuses a blank token; this keeps a blank one from ever matching "Bearer ".
		if s.adminToken == "" || subtle.ConstantTimeCompare(provided, expected) != 1 {
			writeError(w, http.StatusUnauthorized, "unauthorized", "Not authorised.", "")
			return
		}
		next(w, r)
	}
}

// limiter is a fixed-window counter per client key, which is the right amount of machinery for
// public endpoints that cost a CPU-second at most. It counts in one process: across several
// instances the real ceiling is the limit times the instance count.
type limiter struct {
	mu       sync.Mutex
	perHour  int
	counts   map[string]int
	windowAt time.Time
}

func newLimiter(perHour int) *limiter {
	return &limiter{perHour: perHour, counts: map[string]int{}, windowAt: time.Now()}
}

// allow counts one request. first is true for the first refusal of a key in a window, so a refused
// client is recorded once rather than once per retry.
func (l *limiter) allow(key string) (ok, first bool) {
	l.mu.Lock()
	defer l.mu.Unlock()

	if time.Since(l.windowAt) >= time.Hour {
		l.counts = map[string]int{}
		l.windowAt = time.Now()
	}
	count := l.counts[key]
	if count < l.perHour {
		l.counts[key] = count + 1
		return true, false
	}
	if count == l.perHour {
		l.counts[key] = count + 1
		return false, true
	}
	return false, false
}

// limit refuses a client over its hourly allowance for one route family.
func (s *Server) limit(l *limiter, next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if ok, _ := l.allow(s.clientKey(r)); !ok {
			w.Header().Set("Retry-After", "3600")
			writeError(w, http.StatusTooManyRequests, "rate_limited", "Too many requests. Try again in an hour.", "")
			return
		}
		next(w, r)
	}
}
