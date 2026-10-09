package store

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"regexp"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/base-android/generator-api/internal/clip"
	"github.com/base-android/generator-api/internal/telemetry"
)

// Batch is everything one request records. The writer sends it in one round trip, so a request
// costs the database one exchange however many rows it adds.
type Batch struct {
	Generations []Generation
	Events      []Event
	Visits      []Visit
	Errors      []ErrorRecord
	Requests    []Request
}

func (b *Batch) Empty() bool {
	return len(b.Generations)+len(b.Events)+len(b.Visits)+len(b.Errors)+len(b.Requests) == 0
}

// Generation is one generate request, whatever its outcome.
type Generation struct {
	Target      string
	Outcome     telemetry.Outcome
	SessionID   string
	VisitorHash string
	Client      telemetry.Client
	ConfigMs    *int
	ColdStart   bool

	AppName        string
	PackageName    string
	Preset         string
	Features       []string
	Added          []string
	Removed        []string
	FeatureModules []string
	Architecture   string
	DesignStyle    string
	MotionStyle    string
	Languages      []string
	MinSDK         int
	TargetSDK      int
	Glass          bool
	FontName       string
	AccentColour   string
	Spec           []byte

	Notes         []string
	IgnoredFields []string
	Warnings      []string

	// Nil when the request never reached that stage.
	QueueMs  *int
	PythonMs *int
	StreamMs *int
	// Duration is from the start of the request to the generator's exit, without the download.
	Duration time.Duration
	ZipBytes int64

	FailureReason   string
	FailureField    string
	GeneratorCommit string
	Country         string
	DeviceClass     string
}

// Event is a funnel event. Client events arrive through /events; the generate route writes the
// server ones.
type Event struct {
	SessionID string
	Target    string
	Name      string
	Step      string
	Value     string
	Client    telemetry.Client
	ConfigMs  *int
}

// Visit is a session's `landed` row: how the visitor arrived and from where, coarsely.
type Visit struct {
	// SessionID is empty only for the legacy /api/track alias.
	SessionID   string
	VisitorHash string
	Landing     telemetry.Landing
	Geo         telemetry.Geo
	Agent       telemetry.Agent
	Client      telemetry.Client
}

// ErrorRecord is something the software noticed about itself. Fingerprint groups repeats; when it is
// empty the kind and message are used.
type ErrorRecord struct {
	Kind        string
	Message     string
	Detail      string
	Path        string
	Fingerprint string
}

type Request struct {
	Route    string
	Method   string
	Status   int
	Duration time.Duration
	Bytes    int64
}

// Milliseconds is a nullable duration column.
func Milliseconds(d time.Duration) *int {
	ms := int(d.Milliseconds())
	return &ms
}

const insertGeneration = `
	INSERT INTO generations (
		target, outcome, session_id, visitor_hash, client, config_ms, cold_start,
		app_name, package_name, preset, features, added, removed, feature_modules,
		architecture, design_style, motion_style, languages, min_sdk, target_sdk, glass,
		font_name, accent_colour, spec, queue_ms, python_ms, stream_ms, duration_ms, zip_bytes,
		succeeded, failure_reason, failure_field, generator_commit, country, device_class,
		notes, ignored_fields, warnings
	) VALUES (
		$1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17, $18, $19, $20,
		$21, $22, $23, $24, $25, $26, $27, $28, $29, $30, $31, $32, $33, $34, $35, $36, $37, $38
	)`

// upsertRollup recomputes the day's row for the generation just inserted, so its percentiles are
// exact rather than an approximation updated one sample at a time.
const upsertRollup = `
	INSERT INTO daily_rollup (day, target, preset, outcome, country, device_class, n, p50_ms, p95_ms)
	SELECT CURRENT_DATE, $1, $2, $3, $4, $5, count(*),
	       percentile_disc(0.5)  WITHIN GROUP (ORDER BY duration_ms),
	       percentile_disc(0.95) WITHIN GROUP (ORDER BY duration_ms)
	FROM generations
	WHERE created_at >= CURRENT_DATE AND created_at < CURRENT_DATE + 1
	  AND target = $1 AND COALESCE(preset, '') = $2 AND outcome = $3
	  AND COALESCE(country, '') = $4 AND COALESCE(device_class, '') = $5
	ON CONFLICT (day, target, preset, outcome, country, device_class)
	DO UPDATE SET n = EXCLUDED.n, p50_ms = EXCLUDED.p50_ms, p95_ms = EXCLUDED.p95_ms`

const insertEvent = `
	INSERT INTO events (session_id, target, name, step, value, client, config_ms)
	VALUES ($1, $2, $3, $4, $5, $6, $7)`

const insertVisit = `
	INSERT INTO visits (
		day, visitor_hash, step, session_id, returning_visitor, referrer_origin,
		utm_source, utm_medium, utm_campaign, viewport, country, region, city,
		browser, os, device_class, client
	) VALUES (
		CURRENT_DATE, $1, 'landed', $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15
	)
	ON CONFLICT (session_id) WHERE session_id IS NOT NULL DO NOTHING`

const upsertError = `
	INSERT INTO errors (fingerprint, kind, message, detail, path)
	VALUES ($1, $2, $3, $4, $5)
	ON CONFLICT (fingerprint) DO UPDATE SET
		last_seen_at = now(),
		occurrences  = errors.occurrences + 1,
		-- A bug that reappears after being ticked off is not resolved.
		resolved     = FALSE,
		detail       = COALESCE(EXCLUDED.detail, errors.detail)`

const insertRequest = `
	INSERT INTO requests (route, method, status, duration_ms, bytes_out)
	VALUES ($1, $2, $3, $4, $5)`

// queue turns a batch into pgx statements, clipping every free-text value to its column budget.
func (b *Batch) queue(into *pgx.Batch) {
	for _, g := range b.Generations {
		succeeded := g.Outcome == telemetry.OK || g.Outcome == telemetry.DownloadInterrupted
		into.Queue(insertGeneration,
			g.Target, string(g.Outcome), nullable(g.SessionID), g.VisitorHash, nullable(string(g.Client)),
			g.ConfigMs, g.ColdStart,
			nullable(clip.String(g.AppName, 120)), nullable(clip.String(g.PackageName, 120)),
			nullable(g.Preset), list(g.Features), list(g.Added), list(g.Removed), list(g.FeatureModules),
			nullable(g.Architecture), nullable(g.DesignStyle), nullable(g.MotionStyle), list(g.Languages),
			nullableInt(g.MinSDK), nullableInt(g.TargetSDK), g.Glass,
			nullable(clip.String(g.FontName, 120)), nullable(clip.String(g.AccentColour, 32)), nullableJSON(g.Spec),
			g.QueueMs, g.PythonMs, g.StreamMs, g.Duration.Milliseconds(), nullableInt64(g.ZipBytes),
			succeeded, nullable(clip.String(g.FailureReason, 1000)), nullable(g.FailureField),
			nullable(g.GeneratorCommit), nullable(g.Country), nullable(g.DeviceClass),
			clipped(g.Notes, 300), clipped(g.IgnoredFields, 120), clipped(g.Warnings, 300),
		)
		into.Queue(upsertRollup, g.Target, g.Preset, string(g.Outcome), g.Country, g.DeviceClass)
	}
	for _, e := range b.Events {
		into.Queue(insertEvent,
			nullable(e.SessionID), nullable(e.Target), e.Name, nullable(e.Step), nullable(e.Value),
			nullable(string(e.Client)), e.ConfigMs,
		)
	}
	for _, v := range b.Visits {
		into.Queue(insertVisit,
			v.VisitorHash, nullable(v.SessionID), v.Landing.Returning, nullable(v.Landing.ReferrerOrigin),
			nullable(v.Landing.UTMSource), nullable(v.Landing.UTMMedium), nullable(v.Landing.UTMCampaign),
			nullable(v.Landing.Viewport), nullable(v.Geo.Country), nullable(v.Geo.Region), nullable(v.Geo.City),
			nullable(v.Agent.Browser), nullable(v.Agent.OS), nullable(v.Agent.DeviceClass), nullable(string(v.Client)),
		)
	}
	for _, e := range b.Errors {
		fingerprint := e.Fingerprint
		if fingerprint == "" {
			fingerprint = Fingerprint(e.Kind, e.Message)
		}
		into.Queue(upsertError,
			fingerprint, e.Kind, clip.String(e.Message, 1000), nullable(clip.String(e.Detail, 16000)),
			nullable(clip.String(e.Path, 300)),
		)
	}
	for _, r := range b.Requests {
		into.Queue(insertRequest, r.Route, r.Method, r.Status, r.Duration.Milliseconds(), r.Bytes)
	}
}

func (s *Store) write(ctx context.Context, b *Batch) error {
	var batch pgx.Batch
	b.queue(&batch)
	return s.pool.SendBatch(ctx, &batch).Close()
}

// digitsAndPaths is what makes two occurrences of the same bug group together: temp directory
// names, line numbers and hex addresses differ between runs and say nothing about which bug it is.
var digitsAndPaths = regexp.MustCompile(`(0x[0-9a-f]+|[/\\][^\s'"]+|\d+)`)

// Fingerprint groups errors that differ only in paths, numbers and addresses.
func Fingerprint(kind string, parts ...string) string {
	normalised := digitsAndPaths.ReplaceAllString(strings.Join(parts, "\n"), "*")
	sum := sha256.Sum256([]byte(kind + "|" + normalised))
	return hex.EncodeToString(sum[:12])
}

// list keeps a nil slice out of a NOT NULL text[] column: pgx sends a nil slice as NULL.
func list(values []string) []string {
	if values == nil {
		return []string{}
	}
	return values
}

// clipped is a text[] of the generator's free text, each entry clipped to limit bytes: the
// generator bounds how many entries there are, not how long each is.
func clipped(values []string, limit int) []string {
	out := make([]string, len(values))
	for i, value := range values {
		out[i] = clip.String(value, limit)
	}
	return out
}

func nullable(s string) any {
	if s == "" {
		return nil
	}
	return s
}

func nullableInt(v int) any {
	if v == 0 {
		return nil
	}
	return v
}

func nullableInt64(v int64) any {
	if v == 0 {
		return nil
	}
	return v
}

func nullableJSON(raw []byte) any {
	if len(raw) == 0 {
		return nil
	}
	return json.RawMessage(raw)
}
