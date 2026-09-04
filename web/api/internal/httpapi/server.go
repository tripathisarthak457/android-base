// Package httpapi is the service's HTTP surface: five public endpoints and five admin ones.
//
// No router dependency. Go 1.22's `http.ServeMux` matches on method and path pattern, which is
// everything this needs — and a service whose whole API fits on one screen does not benefit from
// a framework that has to be learned by whoever inherits it.
package httpapi

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/base-android/generator-api/internal/config"
	"github.com/base-android/generator-api/internal/generate"
	"github.com/base-android/generator-api/internal/store"
)

type Server struct {
	cfg        config.Config
	generator  *generate.Generator
	store      *store.Store
	log        *slog.Logger
	limiter    *limiter
	origins    []string
	adminToken string
	catalogue  json.RawMessage
	startedAt  time.Time

	// background outlives any one request, for the recording that must not be cancelled when a
	// client disconnects mid-download.
	background context.Context
}

func New(
	ctx context.Context,
	cfg config.Config,
	generator *generate.Generator,
	st *store.Store,
	log *slog.Logger,
) (*Server, error) {
	catalogue, err := generator.Catalogue(ctx)
	if err != nil {
		return nil, fmt.Errorf("loading the catalogue at boot: %w", err)
	}

	return &Server{
		cfg:        cfg,
		generator:  generator,
		store:      st,
		log:        log,
		limiter:    newLimiter(cfg.RateLimitPerHour),
		origins:    cfg.AllowedOrigins,
		adminToken: cfg.AdminToken,
		catalogue:  catalogue,
		startedAt:  time.Now(),
		background: context.Background(),
	}, nil
}

func (s *Server) Routes() http.Handler {
	mux := http.NewServeMux()

	get := func(pattern, route string, handler http.HandlerFunc) {
		mux.HandleFunc("GET "+pattern, s.cors(s.observe(route, handler)))
		mux.HandleFunc("OPTIONS "+pattern, s.cors(func(http.ResponseWriter, *http.Request) {}))
	}
	post := func(pattern, route string, handler http.HandlerFunc) {
		mux.HandleFunc("POST "+pattern, s.cors(s.observe(route, handler)))
		mux.HandleFunc("OPTIONS "+pattern, s.cors(func(http.ResponseWriter, *http.Request) {}))
	}

	get("/api/health", "health", s.handleHealth)
	get("/api/options", "options", s.handleOptions)
	post("/api/generate", "generate", s.rateLimit(s.handleGenerate))
	post("/api/track", "track", s.handleTrack)

	if s.store != nil {
		get("/admin/overview", "admin.overview", s.requireAdmin(s.handleOverview))
		get("/admin/daily", "admin.daily", s.requireAdmin(s.handleDaily))
		get("/admin/features", "admin.features", s.requireAdmin(s.handleFeatures))
		get("/admin/errors", "admin.errors", s.requireAdmin(s.handleErrors))
		get("/admin/generations", "admin.generations", s.requireAdmin(s.handleRecent))
		get("/admin/health", "admin.health", s.requireAdmin(s.handleRouteHealth))
		post("/admin/errors/resolve", "admin.resolve", s.requireAdmin(s.handleResolveError))
	}

	return mux
}

// ── Public ───────────────────────────────────────────────────────────────────

func (s *Server) handleHealth(w http.ResponseWriter, _ *http.Request) {
	writeJSON(w, http.StatusOK, map[string]any{
		"ok":          true,
		"uptime":      time.Since(s.startedAt).Round(time.Second).String(),
		"persistent":  s.store != nil,
		"keystores":   false,
		"generatedAt": time.Now().UTC().Format(time.RFC3339),
	})
}

// handleOptions serves the generator's own catalogue, cached from boot.
//
// The site's form is rendered from this rather than from a list checked into the frontend, so a
// feature added to the Python cannot become a checkbox that the site does not offer.
func (s *Server) handleOptions(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "public, max-age=300")
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write(s.catalogue)
}

type generateRequest struct {
	generate.Request
	// Preset is recorded but not sent to the generator, which only knows about features.
	Preset string `json:"preset"`
}

func (s *Server) handleGenerate(w http.ResponseWriter, r *http.Request) {
	started := time.Now()

	// 64KB: the biggest legitimate body is a few hundred bytes. Anything larger is either a
	// mistake or an attempt, and reading it costs memory either way.
	body, err := io.ReadAll(http.MaxBytesReader(w, r.Body, 64<<10))
	if err != nil {
		writeError(w, http.StatusRequestEntityTooLarge, "That request was too large.")
		return
	}

	var request generateRequest
	if err := json.Unmarshal(body, &request); err != nil {
		writeError(w, http.StatusBadRequest, "That request was not valid JSON.")
		return
	}

	visitor := ""
	if s.store != nil {
		visitor = s.store.VisitorHash(clientIP(r))
	}

	result, cleanup, err := s.generator.Run(r.Context(), request.Request)
	elapsed := time.Since(started)

	if err != nil {
		var invalid *generate.InvalidRequestError
		if errors.As(err, &invalid) {
			// A rejection by the generator's own rules is the caller's mistake. Recorded as a
			// failed generation so the funnel is honest, but deliberately not as an *error* —
			// the error list is for bugs, and filling it with typed-in package names would make
			// it useless within a day.
			s.record(request, visitor, r, false, 0, elapsed, invalid.Message)
			writeError(w, http.StatusBadRequest, invalid.Message)
			return
		}

		s.log.Error("generation failed", "error", err, "package", request.PackageName)
		s.record(request, visitor, r, false, 0, elapsed, err.Error())
		if s.store != nil {
			go s.store.RecordError(s.background, "generation", err.Error(), "", "/api/generate")
		}
		writeError(w, http.StatusInternalServerError,
			"The generator failed. This has been logged; the CLI in the repository does the same "+
				"job and is unaffected.")
		return
	}
	defer cleanup()

	file, err := os.Open(result.ZipPath)
	if err != nil {
		s.log.Error("opening the generated zip", "error", err)
		s.record(request, visitor, r, false, 0, elapsed, err.Error())
		writeError(w, http.StatusInternalServerError, "The project was generated but could not be read.")
		return
	}
	defer func() { _ = file.Close() }()

	filename := result.ProjectName + ".zip"
	w.Header().Set("Content-Type", "application/zip")
	w.Header().Set("Content-Disposition", `attachment; filename="`+filename+`"`)
	w.Header().Set("Content-Length", strconv.FormatInt(result.ZipBytes, 10))
	// So the browser can show the summary without a second request.
	w.Header().Set("X-Project-Name", result.ProjectName)
	w.Header().Set("X-Generation-Ms", strconv.Itoa(result.ElapsedMillis))
	w.Header().Set("Access-Control-Expose-Headers", "X-Project-Name, X-Generation-Ms, Content-Disposition")
	w.WriteHeader(http.StatusOK)

	if _, err := io.Copy(w, file); err != nil {
		// The client went away mid-download. Not an error worth alerting on, but worth counting.
		s.log.Warn("download interrupted", "error", err, "project", result.ProjectName)
	}

	s.record(request, visitor, r, true, result.ZipBytes, elapsed, "")
	if s.store != nil {
		go s.store.RecordVisit(s.background, visitor, "generated")
	}
}

// handleTrack records one funnel step. Deliberately tiny and deliberately not a general event
// endpoint: a public write path that accepts arbitrary payloads is a public write path somebody
// fills with junk.
func (s *Server) handleTrack(w http.ResponseWriter, r *http.Request) {
	if s.store == nil {
		w.WriteHeader(http.StatusNoContent)
		return
	}

	var payload struct {
		Step string `json:"step"`
	}
	if err := json.NewDecoder(io.LimitReader(r.Body, 1<<10)).Decode(&payload); err != nil {
		writeError(w, http.StatusBadRequest, "That request was not valid JSON.")
		return
	}

	switch payload.Step {
	case "landed", "configured", "downloaded":
		s.store.RecordVisit(r.Context(), s.store.VisitorHash(clientIP(r)), payload.Step)
		w.WriteHeader(http.StatusNoContent)
	default:
		// "generated" is recorded by the generate handler, from what actually happened, and is
		// not accepted here — a funnel whose last step can be claimed by the client is a funnel
		// that always converts.
		writeError(w, http.StatusBadRequest, "Unknown step.")
	}
}

func (s *Server) record(
	request generateRequest,
	visitor string,
	r *http.Request,
	succeeded bool,
	zipBytes int64,
	elapsed time.Duration,
	failure string,
) {
	if s.store == nil {
		return
	}
	go s.store.RecordGeneration(s.background, store.Generation{
		VisitorHash:    visitor,
		AppName:        request.AppName,
		PackageName:    request.PackageName,
		Preset:         request.Preset,
		Features:       request.Features,
		FeatureModules: request.FeatureModules,
		MinSDK:         request.MinSDK,
		TargetSDK:      request.TargetSDK,
		MotionStyle:    request.MotionStyle,
		FontName:       request.FontName,
		AccentColour:   request.AccentColour,
		Succeeded:      succeeded,
		ZipBytes:       zipBytes,
		Duration:       elapsed,
		FailureReason:  failure,
		Referrer:       trimReferrer(r.Header.Get("Referer")),
	})
}

// ── Admin ────────────────────────────────────────────────────────────────────

func (s *Server) handleOverview(w http.ResponseWriter, r *http.Request) {
	overview, err := s.store.Overview(r.Context())
	if err != nil {
		s.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, overview)
}

func (s *Server) handleDaily(w http.ResponseWriter, r *http.Request) {
	days := intParam(r, "days", 30, 1, 365)
	points, err := s.store.Daily(r.Context(), days)
	if err != nil {
		s.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, points)
}

func (s *Server) handleFeatures(w http.ResponseWriter, r *http.Request) {
	counts, err := s.store.FeaturePopularity(r.Context())
	if err != nil {
		s.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, counts)
}

func (s *Server) handleErrors(w http.ResponseWriter, r *http.Request) {
	includeResolved := r.URL.Query().Get("resolved") == "true"
	groups, err := s.store.Errors(r.Context(), includeResolved, intParam(r, "limit", 100, 1, 500))
	if err != nil {
		s.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, groups)
}

func (s *Server) handleRecent(w http.ResponseWriter, r *http.Request) {
	recent, err := s.store.RecentGenerations(r.Context(), intParam(r, "limit", 50, 1, 500))
	if err != nil {
		s.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, recent)
}

func (s *Server) handleRouteHealth(w http.ResponseWriter, r *http.Request) {
	health, err := s.store.Health(r.Context())
	if err != nil {
		s.fail(w, err)
		return
	}
	writeJSON(w, http.StatusOK, health)
}

func (s *Server) handleResolveError(w http.ResponseWriter, r *http.Request) {
	var payload struct {
		ID       int64 `json:"id"`
		Resolved bool  `json:"resolved"`
	}
	if err := json.NewDecoder(io.LimitReader(r.Body, 1<<10)).Decode(&payload); err != nil {
		writeError(w, http.StatusBadRequest, "That request was not valid JSON.")
		return
	}
	if err := s.store.ResolveError(r.Context(), payload.ID, payload.Resolved); err != nil {
		writeError(w, http.StatusNotFound, err.Error())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (s *Server) fail(w http.ResponseWriter, err error) {
	s.log.Error("admin query failed", "error", err)
	go s.store.RecordError(s.background, "api", err.Error(), "", "/admin")
	writeError(w, http.StatusInternalServerError, "Could not read the data.")
}

// ── Helpers ──────────────────────────────────────────────────────────────────

func writeJSON(w http.ResponseWriter, status int, payload any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(payload)
}

func writeError(w http.ResponseWriter, status int, message string) {
	writeJSON(w, status, map[string]string{"error": message})
}

func intParam(r *http.Request, name string, fallback, minimum, maximum int) int {
	value, err := strconv.Atoi(r.URL.Query().Get(name))
	if err != nil || value < minimum || value > maximum {
		return fallback
	}
	return value
}

// trimReferrer keeps the origin and drops the path. Knowing that traffic came from a Reddit
// thread is useful; recording which thread is more than this needs to know.
func trimReferrer(raw string) string {
	if raw == "" {
		return ""
	}
	if index := strings.Index(raw, "://"); index >= 0 {
		rest := raw[index+3:]
		if slash := strings.IndexByte(rest, '/'); slash > 0 {
			return raw[:index+3] + rest[:slash]
		}
	}
	return raw
}

type logWriter struct{}

func (logWriter) Write(p []byte) (int, error) { return os.Stdout.Write(p) }
