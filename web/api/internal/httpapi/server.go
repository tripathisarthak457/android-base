// Package httpapi is the service's HTTP surface. No router dependency.
package httpapi

import (
	"bytes"
	"cmp"
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"net/url"
	"slices"
	"strconv"
	"strings"
	"sync/atomic"
	"time"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/generate"
	"github.com/base-android/generator-api/internal/store"
	"github.com/base-android/generator-api/internal/telemetry"
)

// Sink takes a request's telemetry: the store's Writer in production.
type Sink interface {
	Submit(*store.Batch) <-chan struct{}
	Stats() store.WriterStats
}

type Server struct {
	cfg        config.Config
	catalogue  *generate.Catalogue
	generators map[string]*generate.Generator
	// store and sink are nil when DATABASE_URL is unset: nothing is recorded and the admin routes
	// are not registered.
	store      *store.Store
	sink       Sink
	log        *slog.Logger
	origins    map[string]bool
	adminToken string
	known      telemetry.Known
	startedAt  time.Time
	// warm is set by the first request to reach the generator, which is recorded as a cold start.
	// Requests refused before it never start Python, so they cannot be cold.
	warm atomic.Bool

	generateLimit *limiter
	feedbackLimit *limiter
	errorLimit    *limiter
	eventLimit    *limiter
	readLimit     *limiter
}

// New wires the server. generators holds the enabled targets the catalogue provides.
func New(
	cfg config.Config,
	catalogue *generate.Catalogue,
	generators map[string]*generate.Generator,
	st *store.Store,
	writer Sink,
	log *slog.Logger,
) *Server {
	origins := map[string]bool{}
	for _, origin := range cfg.AllowedOrigins {
		origins[origin] = true
	}
	known := telemetry.Known{}
	for _, target := range catalogue.Targets {
		presets := []string{}
		for _, preset := range target.Presets {
			presets = append(presets, preset.Key)
		}
		known[target.Key] = presets
	}
	return &Server{
		cfg:           cfg,
		catalogue:     catalogue,
		generators:    generators,
		store:         st,
		sink:          writer,
		log:           log,
		origins:       origins,
		adminToken:    cfg.AdminToken,
		known:         known,
		startedAt:     time.Now(),
		generateLimit: newLimiter(cfg.RateLimitPerHour),
		// Separate from generation, so a visitor who hit the generation limit can still report it.
		feedbackLimit: newLimiter(20),
		errorLimit:    newLimiter(60),
		// The site sends a batch every few seconds while someone configures.
		eventLimit: newLimiter(1200),
		readLimit:  newLimiter(1200),
	}
}

func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()

	handle := func(method, pattern, route string, handler http.HandlerFunc) {
		if method == http.MethodPost {
			handler = requireJSON(handler)
		}
		mux.HandleFunc(method+" "+pattern, s.cors(s.observe(route, handler)))
		mux.HandleFunc("OPTIONS "+pattern, s.cors(func(http.ResponseWriter, *http.Request) {}))
	}
	get := func(pattern, route string, handler http.HandlerFunc) { handle(http.MethodGet, pattern, route, handler) }
	post := func(pattern, route string, handler http.HandlerFunc) {
		handle(http.MethodPost, pattern, route, handler)
	}

	// Health is not limited: platform probes and uptime monitors poll it from one address, and
	// answering costs no more than refusing.
	get("/api/v1/health", "health", s.handleHealth)
	get("/api/v1/targets", "targets", s.limit(s.readLimit, s.handleTargets))
	get("/api/v1/catalogue", "catalogue", s.limit(s.readLimit, s.handleCatalogue))
	post("/api/v1/{target}/generate", "generate", func(w http.ResponseWriter, r *http.Request) {
		s.generate(w, r, r.PathValue("target"))
	})
	post("/api/v1/events", "events", s.limit(s.eventLimit, s.handleEvents))
	post("/api/v1/feedback", "feedback", s.limit(s.feedbackLimit, s.handleFeedback))
	post("/api/v1/client-error", "client_error", s.limit(s.errorLimit, s.handleClientError))

	// The routes before v1, kept for one release so a page or a script from the previous deploy
	// keeps working. They mean Android.
	get("/api/health", "legacy.health", s.handleHealth)
	get("/api/options", "legacy.options", s.limit(s.readLimit, s.handleCatalogue))
	post("/api/generate", "legacy.generate", func(w http.ResponseWriter, r *http.Request) { s.generate(w, r, "android") })
	post("/api/track", "legacy.track", s.limit(s.eventLimit, s.handleTrack))

	if s.store != nil {
		admin := func(name string, handler http.HandlerFunc) {
			get("/api/v1/admin/"+name, "admin."+name, s.requireAdmin(handler))
		}
		admin("overview", s.admin(s.readOverview))
		admin("funnel", s.admin(s.readFunnel))
		admin("series", s.admin(s.readSeries))
		admin("adoption", s.admin(s.readAdoption))
		admin("failures", s.admin(s.readFailures))
		admin("audience", s.admin(s.readAudience))
		admin("durations", s.admin(s.readDurations))
		admin("generations", s.admin(s.readGenerations))
		admin("generations.csv", s.handleGenerationsCSV)
		admin("errors", s.admin(s.readErrors))
		admin("feedback", s.admin(s.readFeedback))
		admin("health", s.admin(s.readAdminHealth))
		post("/api/v1/admin/errors/resolve", "admin.resolve", s.requireAdmin(s.handleResolveError))
		post("/api/v1/admin/feedback/update", "admin.feedback_update", s.requireAdmin(s.handleFeedbackUpdate))
	}
	return mux
}

func (s *Server) databaseState() string {
	switch {
	case s.store == nil:
		return "off"
	case s.store.Ready():
		return "ready"
	default:
		return "connecting"
	}
}

func (s *Server) enabledTargets() []string {
	var keys []string
	for _, target := range s.catalogue.Targets {
		if s.generators[target.Key] != nil {
			keys = append(keys, target.Key)
		}
	}
	return keys
}

func (s *Server) handleHealth(w http.ResponseWriter, _ *http.Request) {
	body := map[string]any{
		"ok":               true,
		"uptime_s":         int(time.Since(s.startedAt).Seconds()),
		"database":         s.databaseState(),
		"targets":          s.enabledTargets(),
		"generator_commit": s.catalogue.GeneratorCommit,
	}
	if s.sink != nil {
		body["telemetry"] = s.sink.Stats()
	}
	writeJSON(w, http.StatusOK, body)
}

type targetInfo struct {
	Key        string `json:"key"`
	Title      string `json:"title"`
	Status     string `json:"status"`
	StatusNote string `json:"status_note,omitempty"`
	Summary    string `json:"summary"`
	// Enabled says whether this deployment generates the target. A `coming_soon` target can be
	// enabled on a test deployment while the site still shows it as coming soon.
	Enabled bool `json:"enabled"`
}

func (s *Server) handleTargets(w http.ResponseWriter, _ *http.Request) {
	targets := []targetInfo{}
	for _, t := range s.catalogue.Targets {
		if t.Status == "hidden" {
			continue
		}
		targets = append(targets, targetInfo{
			Key: t.Key, Title: t.Title, Status: t.Status, StatusNote: t.StatusNote, Summary: t.Summary,
			Enabled: s.generators[t.Key] != nil,
		})
	}
	writeJSON(w, http.StatusOK, targets)
}

// handleCatalogue serves the generator's own catalogue, read once at boot.
//
// Revalidated on every load rather than trusted for minutes: a browser holding the previous deploy's
// catalogue would render a form the page's code no longer matches. An unchanged catalogue costs a
// 304 with no body.
func (s *Server) handleCatalogue(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-cache")
	w.Header().Set("ETag", s.catalogue.ETag)
	if etagMatches(r.Header.Get("If-None-Match"), s.catalogue.ETag) {
		w.WriteHeader(http.StatusNotModified)
		return
	}
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Content-Length", strconv.Itoa(len(s.catalogue.Raw)))
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(s.catalogue.Raw)
}

// etagMatches applies If-None-Match's weak comparison: a list, `*`, and W/ prefixes all count.
func etagMatches(header, etag string) bool {
	for _, candidate := range strings.Split(header, ",") {
		candidate = strings.TrimPrefix(strings.TrimSpace(candidate), "W/")
		if candidate == "*" || candidate == etag {
			return true
		}
	}
	return false
}

// A closed set, checked here rather than trusted: these values are rendered into the admin portal
// and grouped in SQL.
var (
	feedbackKinds      = []string{"bug", "idea", "praise", "question"}
	feedbackSeverities = []string{"blocks", "annoying", "cosmetic", ""}
	feedbackAreas      = []string{"website", "generated-project", "cli", "docs", ""}
)

// handleFeedback takes a bug report or a suggestion.
func (s *Server) handleFeedback(w http.ResponseWriter, r *http.Request) {
	if s.store == nil || !s.store.Ready() {
		// Honest rather than a silent 204: somebody took the trouble to write this.
		writeError(w, http.StatusServiceUnavailable, "unavailable",
			"Reports cannot be recorded right now. Please open an issue on GitHub instead.", "")
		return
	}

	body, ok := readBody(w, r, 64<<10)
	if !ok {
		return
	}
	var payload store.Feedback
	if err := json.Unmarshal(body, &payload); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", "That request was not valid JSON.", "")
		return
	}
	payload.Title = strings.TrimSpace(payload.Title)
	payload.Body = strings.TrimSpace(payload.Body)

	switch {
	case !slices.Contains(feedbackKinds, payload.Kind):
		writeError(w, http.StatusBadRequest, "invalid_feedback", "Pick what kind of report this is.", "kind")
		return
	case !slices.Contains(feedbackSeverities, payload.Severity):
		writeError(w, http.StatusBadRequest, "invalid_feedback", "That is not a severity.", "severity")
		return
	case !slices.Contains(feedbackAreas, payload.Area):
		writeError(w, http.StatusBadRequest, "invalid_feedback", "That is not an area.", "area")
		return
	case payload.Target != "" && s.known[payload.Target] == nil:
		writeError(w, http.StatusBadRequest, "invalid_feedback", "That is not a platform.", "target")
		return
	case len([]rune(payload.Title)) < 4:
		writeError(w, http.StatusBadRequest, "invalid_feedback", "The summary needs to be a few words longer.", "title")
		return
	case len([]rune(payload.Body)) < 10:
		writeError(w, http.StatusBadRequest, "invalid_feedback", "Please say a little more about what happened.", "body")
		return
	}

	payload.VisitorHash = s.store.VisitorHash(s.clientKey(r), time.Now())
	// The browser family is what a report needs to be reproduced; the raw user agent is not stored
	// anywhere.
	agent := telemetry.ClassifyAgent(r.UserAgent(), r.Header.Get("Sec-CH-UA-Mobile"))
	payload.UserAgent = agent.Browser + " · " + agent.OS + " · " + agent.DeviceClass

	id, err := s.store.SaveFeedback(r.Context(), payload)
	if err != nil {
		s.log.Error("saving feedback", "error", err)
		recordings(r).Errors = append(recordings(r).Errors, store.ErrorRecord{
			Kind: "api", Message: err.Error(), Path: r.URL.Path,
		})
		writeError(w, http.StatusInternalServerError, "internal",
			"That could not be saved. Please open an issue on GitHub instead.", "")
		return
	}
	writeJSON(w, http.StatusCreated, map[string]any{"id": id, "ok": true})
}

// clientErrorReport is an exception the website caught (window.onerror, unhandledrejection, the
// error boundary).
type clientErrorReport struct {
	Message string `json:"message"`
	Stack   string `json:"stack"`
	// URL is the page it happened on; only the path is kept.
	URL     string `json:"url"`
	Release string `json:"release"`
}

func (s *Server) handleClientError(w http.ResponseWriter, r *http.Request) {
	body, ok := readBody(w, r, 16<<10)
	if !ok {
		return
	}
	var report clientErrorReport
	decoder := json.NewDecoder(bytes.NewReader(body))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&report); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", "That request was not valid JSON.", "")
		return
	}
	report.Message = strings.TrimSpace(report.Message)
	if report.Message == "" {
		writeError(w, http.StatusBadRequest, "invalid_report", "An error report needs a message.", "message")
		return
	}

	// The first frame tells two errors with the same message apart: "undefined is not a function"
	// says nothing until you know where.
	firstFrame := ""
	for _, line := range strings.Split(report.Stack, "\n") {
		if line = strings.TrimSpace(line); strings.HasPrefix(line, "at ") || strings.Contains(line, "@") {
			firstFrame = line
			break
		}
	}
	detail := report.Stack
	if report.Release != "" {
		detail = "release " + report.Release + "\n" + detail
	}
	recordings(r).Errors = append(recordings(r).Errors, store.ErrorRecord{
		Kind:        "client",
		Message:     report.Message,
		Detail:      detail,
		Path:        pagePath(report.URL),
		Fingerprint: store.Fingerprint("client", report.Message, firstFrame),
	})
	w.WriteHeader(http.StatusNoContent)
}

// pagePath keeps the path of a page URL and drops its query and fragment, which can carry anything.
// A value that is not a page's URL (a `javascript:` URL, a bare word) is not kept at all.
func pagePath(raw string) string {
	page, err := url.Parse(raw)
	if err != nil {
		return ""
	}
	switch {
	case page.Scheme == "http" || page.Scheme == "https":
		if page.Host == "" {
			return ""
		}
	case page.Scheme != "" || page.Host != "" || !strings.HasPrefix(page.Path, "/"):
		return ""
	}
	return cmp.Or(page.EscapedPath(), "/")
}

// readBody reads at most limit bytes, answering 413 or 400 itself when it cannot.
func readBody(w http.ResponseWriter, r *http.Request, limit int64) ([]byte, bool) {
	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, limit))
	if err == nil {
		return body, true
	}
	var tooLarge *http.MaxBytesError
	if errors.As(err, &tooLarge) {
		writeError(w, http.StatusRequestEntityTooLarge, "too_large", "That request was too large.", "")
	} else {
		writeError(w, http.StatusBadRequest, "unreadable", "The request could not be read.", "")
	}
	return nil, false
}

// writeJSON always sets Content-Length: a response whose length is known is complete for the client
// as soon as it is flushed, which observe relies on.
func writeJSON(w http.ResponseWriter, status int, payload any) {
	body, err := json.Marshal(payload)
	if err != nil {
		status = http.StatusInternalServerError
		body = []byte(`{"error":"The response could not be encoded.","code":"internal"}`)
	}
	body = append(body, '\n')
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Content-Length", strconv.Itoa(len(body)))
	w.WriteHeader(status)
	_, _ = w.Write(body)
}

// apiError is every error body: a sentence for people, a code for programs, and the spec field it
// is about when there is one.
type apiError struct {
	Error string `json:"error"`
	Code  string `json:"code"`
	Field string `json:"field,omitempty"`
}

func writeError(w http.ResponseWriter, status int, code, message, field string) {
	writeJSON(w, status, apiError{Error: message, Code: code, Field: field})
}
