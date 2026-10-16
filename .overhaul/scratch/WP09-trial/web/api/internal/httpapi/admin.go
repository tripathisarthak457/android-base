package httpapi

import (
	"encoding/csv"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"regexp"
	"slices"
	"strconv"
	"strings"
	"time"
	"unicode"

	"github.com/base-android/generator-api/internal/generate"
	"github.com/base-android/generator-api/internal/store"
	"github.com/base-android/generator-api/internal/telemetry"
)

// maxWindowDays matches the retention of events and visits; a longer window would show a cliff.
const maxWindowDays = 400

var (
	targetParam  = regexp.MustCompile(`^[a-z][a-z0-9_]{0,31}$`)
	presetParam  = regexp.MustCompile(`^[a-z0-9_-]{1,64}$`)
	countryParam = regexp.MustCompile(`^[A-Z]{2}$`)
)

// badParam is a query parameter the admin cannot have meant.
type badParam struct{ field, message string }

func (e *badParam) Error() string { return e.message }

// window is the inclusive date range every admin response echoes back.
type window struct {
	From string `json:"from"`
	To   string `json:"to"`
}

func windowOf(f store.Filter) window {
	return window{From: f.From.Format(time.DateOnly), To: f.To.AddDate(0, 0, -1).Format(time.DateOnly)}
}

// parseFilter reads the global window and filters (MASTER-PLAN A10.3). from and to are inclusive UTC
// dates; the default is the last 30 days including today.
func parseFilter(r *http.Request, now time.Time) (store.Filter, error) {
	q := r.URL.Query()
	today := now.UTC().Truncate(24 * time.Hour)

	last := today
	if raw := q.Get("to"); raw != "" {
		day, err := time.Parse(time.DateOnly, raw)
		if err != nil {
			return store.Filter{}, &badParam{"to", "to must be a date like 2026-10-09."}
		}
		last = day
	}
	first := last.AddDate(0, 0, -29)
	if raw := q.Get("from"); raw != "" {
		day, err := time.Parse(time.DateOnly, raw)
		if err != nil {
			return store.Filter{}, &badParam{"from", "from must be a date like 2026-09-10."}
		}
		first = day
	}
	if first.After(last) {
		return store.Filter{}, &badParam{"from", "from is after to."}
	}
	if last.Sub(first) >= maxWindowDays*24*time.Hour {
		return store.Filter{}, &badParam{"from", fmt.Sprintf("The window can be at most %d days.", maxWindowDays)}
	}

	f := store.Filter{
		From:        first,
		To:          last.AddDate(0, 0, 1),
		Target:      q.Get("target"),
		Preset:      q.Get("preset"),
		Outcome:     q.Get("outcome"),
		Country:     strings.ToUpper(q.Get("country")),
		DeviceClass: q.Get("device_class"),
		Source:      strings.ToLower(strings.TrimSpace(q.Get("source"))),
	}
	switch {
	case f.Target != "" && !targetParam.MatchString(f.Target):
		return f, &badParam{"target", "Unknown target."}
	case f.Preset != "" && !presetParam.MatchString(f.Preset):
		return f, &badParam{"preset", "Unknown preset."}
	case f.Outcome != "" && !slices.Contains(telemetry.Outcomes, telemetry.Outcome(f.Outcome)):
		return f, &badParam{"outcome", "Unknown outcome."}
	case f.Country != "" && !countryParam.MatchString(f.Country):
		return f, &badParam{"country", "country is a two-letter code."}
	case f.DeviceClass != "" && !slices.Contains(telemetry.DeviceClasses, f.DeviceClass):
		return f, &badParam{"device_class", "Unknown device class."}
	case len(f.Source) > 100 || strings.IndexFunc(f.Source, unicode.IsControl) >= 0:
		return f, &badParam{"source", "That is not a source."}
	}
	return f, nil
}

// intParam reads an optional positive integer no larger than maximum.
func intParam(r *http.Request, name string, fallback, maximum int) (int, error) {
	raw := r.URL.Query().Get(name)
	if raw == "" {
		return fallback, nil
	}
	value, err := strconv.Atoi(raw)
	if err != nil || value < 1 || value > maximum {
		return 0, &badParam{name, fmt.Sprintf("%s must be a whole number from 1 to %d.", name, maximum)}
	}
	return value, nil
}

// admin wraps a read: the filter is parsed, the result is written, and a failure is answered with
// the right status. Only a bad token is a 401 (requireAdmin); a database that is down is a 503, so the
// portal keeps its last data and says so instead of logging out.
func (s *Server) admin(read func(r *http.Request, f store.Filter) (any, error)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		f, err := parseFilter(r, time.Now())
		if err == nil {
			var result any
			if result, err = read(r, f); err == nil {
				writeJSON(w, http.StatusOK, result)
				return
			}
		}
		s.adminFailed(w, r, err)
	}
}

func (s *Server) adminFailed(w http.ResponseWriter, r *http.Request, err error) {
	var bad *badParam
	switch {
	case errors.As(err, &bad):
		writeError(w, http.StatusBadRequest, "bad_request", bad.message, bad.field)
	case errors.Is(err, store.ErrNotReady):
		writeError(w, http.StatusServiceUnavailable, "unavailable", "The database is not reachable right now.", "")
	case errors.Is(err, store.ErrNotFound):
		writeError(w, http.StatusNotFound, "not_found", "Nothing with that id.", "")
	default:
		s.log.Error("admin query failed", "path", r.URL.Path, "error", err)
		recordings(r).Errors = append(recordings(r).Errors, store.ErrorRecord{
			Kind: "api", Message: err.Error(), Path: r.URL.Path,
		})
		writeError(w, http.StatusInternalServerError, "internal", "Could not read the data.", "")
	}
}

// scopedTargets are the catalogue targets a filter covers: the one it names, or all of them.
func (s *Server) scopedTargets(f store.Filter) []generate.TargetEntry {
	if f.Target == "" {
		return s.catalogue.Targets
	}
	if entry := s.catalogue.Target(f.Target); entry != nil {
		return []generate.TargetEntry{*entry}
	}
	return nil
}

func featureKeys(targets []generate.TargetEntry) []string {
	keys := []string{}
	for _, target := range targets {
		for _, feature := range target.Features {
			if !slices.Contains(keys, feature.Key) {
				keys = append(keys, feature.Key)
			}
		}
	}
	return keys
}

func (s *Server) handleOverview(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		overview, err := s.store.Overview(r.Context(), f)
		return struct {
			window
			store.Overview
		}{windowOf(f), overview}, err
	})(w, r)
}

// funnelSteps is the order of a target's funnel, from choosing the platform to the finished
// download. Other events are reported beside it.
var funnelSteps = []string{
	telemetry.EventPlatformSelected,
	"step_viewed:identity", "step_viewed:features", "step_viewed:look", "step_viewed:motion",
	"step_viewed:build", "step_viewed:review",
	telemetry.EventGenerateClicked, telemetry.EventGenerated, telemetry.EventDownloaded,
}

type funnelStep struct {
	Key      string `json:"key"`
	Sessions int64  `json:"sessions"`
}

type targetFunnel struct {
	Target string           `json:"target"`
	Steps  []funnelStep     `json:"steps"`
	Also   map[string]int64 `json:"also"`
}

func (s *Server) handleFunnel(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		counts, err := s.store.Funnel(r.Context(), f)
		if err != nil {
			return nil, err
		}
		targets := []targetFunnel{}
		for _, target := range s.scopedTargets(f) {
			got := counts.Targets[target.Key]
			funnel := targetFunnel{Target: target.Key, Steps: []funnelStep{}, Also: map[string]int64{}}
			for _, key := range funnelSteps {
				funnel.Steps = append(funnel.Steps, funnelStep{Key: key, Sessions: got[key]})
			}
			for key, n := range got {
				if !slices.Contains(funnelSteps, key) {
					funnel.Also[key] = n
				}
			}
			targets = append(targets, funnel)
		}
		return struct {
			window
			Landed  int64          `json:"landed"`
			Targets []targetFunnel `json:"targets"`
		}{windowOf(f), counts.Landed, targets}, nil
	})(w, r)
}

func (s *Server) handleSeries(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		days, err := s.store.Series(r.Context(), f)
		return struct {
			window
			Days []store.SeriesDay `json:"days"`
		}{windowOf(f), days}, err
	})(w, r)
}

type featureShare struct {
	store.FeatureAdoption
	Title string  `json:"title"`
	Share float64 `json:"share"`
}

type targetAdoption struct {
	Target     string                   `json:"target"`
	Total      int64                    `json:"total"`
	Features   []featureShare           `json:"features"`
	Dimensions map[string][]store.Count `json:"dimensions"`
	Weekly     []store.Week             `json:"weekly"`
}

func (s *Server) handleAdoption(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		targets := []targetAdoption{}
		for _, target := range s.scopedTargets(f) {
			scoped := f
			scoped.Target = target.Key
			adoption, err := s.store.Adoption(r.Context(), scoped, featureKeys([]generate.TargetEntry{target}))
			if err != nil {
				return nil, err
			}
			titles := map[string]string{}
			for _, feature := range target.Features {
				titles[feature.Key] = feature.Title
			}
			features := []featureShare{}
			for _, feature := range adoption.Features {
				share := 0.0
				if adoption.Total > 0 {
					share = float64(feature.N) / float64(adoption.Total)
				}
				features = append(features, featureShare{FeatureAdoption: feature, Title: titles[feature.Key], Share: share})
			}
			targets = append(targets, targetAdoption{
				Target: target.Key, Total: adoption.Total, Features: features,
				Dimensions: adoption.Dimensions, Weekly: adoption.Weekly,
			})
		}
		return struct {
			window
			Targets []targetAdoption `json:"targets"`
		}{windowOf(f), targets}, nil
	})(w, r)
}

func (s *Server) handleFailures(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		failures, err := s.store.Failures(r.Context(), f, featureKeys(s.scopedTargets(f)))
		return struct {
			window
			store.Failures
		}{windowOf(f), failures}, err
	})(w, r)
}

func (s *Server) handleAudience(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		audience, err := s.store.Audience(r.Context(), f)
		return struct {
			window
			store.Audience
		}{windowOf(f), audience}, err
	})(w, r)
}

func (s *Server) handleDurations(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		durations, err := s.store.Durations(r.Context(), f)
		return struct {
			window
			store.Durations
		}{windowOf(f), durations}, err
	})(w, r)
}

// page reads `before` and `limit` and fetches one row more than asked, to know whether there is a
// next page without counting.
func (s *Server) page(r *http.Request, f store.Filter, fallback, maximum int) ([]store.GenerationRow, int64, error) {
	limit, err := intParam(r, "limit", fallback, maximum)
	if err != nil {
		return nil, 0, err
	}
	var before int64
	if raw := r.URL.Query().Get("before"); raw != "" {
		if before, err = strconv.ParseInt(raw, 10, 64); err != nil || before < 1 {
			return nil, 0, &badParam{"before", "before is the id the previous page ended at."}
		}
	}
	rows, err := s.store.Generations(r.Context(), f, before, limit+1)
	if err != nil {
		return nil, 0, err
	}
	var next int64
	if len(rows) > limit {
		rows = rows[:limit]
		next = rows[limit-1].ID
	}
	return rows, next, nil
}

func (s *Server) handleGenerations(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		rows, next, err := s.page(r, f, 50, 200)
		if err != nil {
			return nil, err
		}
		var nextBefore *int64
		if next > 0 {
			nextBefore = &next
		}
		return struct {
			window
			Items      []store.GenerationRow `json:"items"`
			NextBefore *int64                `json:"next_before"`
		}{windowOf(f), rows, nextBefore}, nil
	})(w, r)
}

// csvColumns are the export's columns, in order.
var csvColumns = []string{
	"id", "at", "target", "outcome", "preset", "added", "removed", "features", "feature_count",
	"architecture", "design_style", "motion_style", "languages", "feature_modules", "min_sdk", "target_sdk",
	"glass", "duration_ms", "queue_ms", "python_ms", "stream_ms", "config_ms", "zip_bytes", "cold_start",
	"client", "country", "device_class", "failure_field", "failure_reason", "generator_commit",
	"app_name", "package_name",
}

// handleGenerationsCSV exports the generations under the current filter, a page at a time. The next
// page's `before` is in the X-Next-Before header.
func (s *Server) handleGenerationsCSV(w http.ResponseWriter, r *http.Request) {
	f, err := parseFilter(r, time.Now())
	if err != nil {
		s.adminFailed(w, r, err)
		return
	}
	rows, next, err := s.page(r, f, 1000, 10000)
	if err != nil {
		s.adminFailed(w, r, err)
		return
	}

	var out strings.Builder
	writer := csv.NewWriter(&out)
	_ = writer.Write(csvColumns)
	for _, g := range rows {
		_ = writer.Write(formulaSafe([]string{
			strconv.FormatInt(g.ID, 10), g.At.UTC().Format(time.RFC3339), g.Target, g.Outcome, g.Preset,
			strings.Join(g.Added, " "), strings.Join(g.Removed, " "), strings.Join(g.Features, " "),
			strconv.Itoa(len(g.Features)), g.Architecture, g.DesignStyle, g.MotionStyle,
			strings.Join(g.Languages, " "), strings.Join(g.FeatureModules, " "), optional(g.MinSDK),
			optional(g.TargetSDK), optional(g.Glass), strconv.Itoa(g.DurationMs), optional(g.QueueMs),
			optional(g.PythonMs), optional(g.StreamMs), optional(g.ConfigMs), optional(g.ZipBytes),
			optional(g.ColdStart), g.Client, g.Country, g.DeviceClass, g.FailureField, g.FailureReason,
			g.GeneratorCommit, g.AppName, g.PackageName,
		}))
	}
	writer.Flush()

	if next > 0 {
		w.Header().Set("X-Next-Before", strconv.FormatInt(next, 10))
	}
	w.Header().Set("Content-Type", "text/csv; charset=utf-8")
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="generations-%s-%s.csv"`,
		f.From.Format("20060102"), f.To.AddDate(0, 0, -1).Format("20060102")))
	w.Header().Set("Content-Length", strconv.Itoa(out.Len()))
	w.WriteHeader(http.StatusOK)
	_, _ = w.Write([]byte(out.String()))
}

// formulaSafe stops a spreadsheet from running a cell as a formula: app names and failure messages
// are typed by strangers, and "=HYPERLINK(...)" in one of them is an attack on whoever opens the
// export.
func formulaSafe(cells []string) []string {
	for i, cell := range cells {
		if cell != "" && strings.ContainsRune("=+-@\t\r", rune(cell[0])) {
			cells[i] = "'" + cell
		}
	}
	return cells
}

func optional[T int | int64 | bool](value *T) string {
	if value == nil {
		return ""
	}
	return fmt.Sprint(*value)
}

func (s *Server) handleErrors(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		limit, err := intParam(r, "limit", 100, 500)
		if err != nil {
			return nil, err
		}
		groups, err := s.store.Errors(r.Context(), f, r.URL.Query().Get("resolved") == "true", limit)
		return struct {
			window
			Items []store.ErrorGroup `json:"items"`
		}{windowOf(f), groups}, err
	})(w, r)
}

func (s *Server) handleFeedbackList(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		status := r.URL.Query().Get("status")
		if status == "all" {
			status = ""
		}
		if status != "" && !slices.Contains(store.FeedbackStatuses, status) {
			return nil, &badParam{"status", "Unknown status."}
		}
		limit, err := intParam(r, "limit", 100, 500)
		if err != nil {
			return nil, err
		}
		items, err := s.store.FeedbackList(r.Context(), f, status, limit)
		if err != nil {
			return nil, err
		}
		counts, err := s.store.FeedbackCounts(r.Context())
		return struct {
			window
			Items  []store.Feedback     `json:"items"`
			Counts store.FeedbackCounts `json:"counts"`
		}{windowOf(f), items, counts}, err
	})(w, r)
}

func (s *Server) handleAdminHealth(w http.ResponseWriter, r *http.Request) {
	s.admin(func(r *http.Request, f store.Filter) (any, error) {
		routes, err := s.store.RouteHealth(r.Context(), f)
		return struct {
			window
			Routes    []store.RouteHealth `json:"routes"`
			Database  string              `json:"database"`
			Telemetry store.WriterStats   `json:"telemetry"`
			UptimeS   int                 `json:"uptime_s"`
			Targets   []string            `json:"targets"`
		}{windowOf(f), routes, s.databaseState(), s.sink.Stats(), int(time.Since(s.startedAt).Seconds()), s.enabledTargets()}, err
	})(w, r)
}

func (s *Server) handleResolveError(w http.ResponseWriter, r *http.Request) {
	var payload struct {
		ID       int64 `json:"id"`
		Resolved bool  `json:"resolved"`
	}
	if !decodeSmall(w, r, &payload) {
		return
	}
	if err := s.store.ResolveError(r.Context(), payload.ID, payload.Resolved); err != nil {
		s.adminFailed(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// handleFeedbackUpdate sets a report's status, its notes, or both. Sending `"notes": ""` clears
// them; leaving `notes` out keeps them.
func (s *Server) handleFeedbackUpdate(w http.ResponseWriter, r *http.Request) {
	var payload struct {
		ID     int64   `json:"id"`
		Status string  `json:"status"`
		Notes  *string `json:"notes"`
	}
	if !decodeSmall(w, r, &payload) {
		return
	}
	if payload.Status != "" && !slices.Contains(store.FeedbackStatuses, payload.Status) {
		writeError(w, http.StatusBadRequest, "bad_request", "Unknown status.", "status")
		return
	}
	if err := s.store.UpdateFeedback(r.Context(), payload.ID, payload.Status, payload.Notes); err != nil {
		s.adminFailed(w, r, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func decodeSmall(w http.ResponseWriter, r *http.Request, into any) bool {
	body, ok := readBody(w, r, 8<<10)
	if !ok {
		return false
	}
	if err := json.Unmarshal(body, into); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", "That request was not valid JSON.", "")
		return false
	}
	return true
}
