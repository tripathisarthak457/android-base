// Package store is everything the admin portal reads and everything the API writes.
//
// A thin layer over pgx with hand-written SQL. No ORM: every query here is either an insert or a
// grouped count, and both are clearer as SQL than as a query builder — the two aggregate queries
// in particular are the whole portal, and hiding them behind method chains would make them harder
// to reason about rather than easier.
//
// Writes are best-effort. A generation that succeeded must not be reported as failed because the
// database was briefly unreachable, so `Record*` logs and returns rather than propagating.
package store

import (
	"context"
	"crypto/sha256"
	"embed"
	"encoding/hex"
	"errors"
	"fmt"
	"log/slog"
	"regexp"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

//go:embed migrations/*.sql
var migrations embed.FS

type Store struct {
	pool *pgxpool.Pool
	salt string
	log  *slog.Logger
}

func Open(ctx context.Context, databaseURL, ipSalt string, log *slog.Logger) (*Store, error) {
	pool, err := pgxpool.New(ctx, databaseURL)
	if err != nil {
		return nil, fmt.Errorf("connecting to Postgres: %w", err)
	}
	if err := pool.Ping(ctx); err != nil {
		pool.Close()
		return nil, fmt.Errorf("pinging Postgres: %w", err)
	}
	s := &Store{pool: pool, salt: ipSalt, log: log}
	if err := s.migrate(ctx); err != nil {
		pool.Close()
		return nil, err
	}
	return s, nil
}

func (s *Store) Close() { s.pool.Close() }

// migrate runs every embedded .sql file in name order. The files are written so that running
// them twice is harmless, which is what lets the service migrate itself at boot instead of
// needing a separate step in the deploy that somebody eventually forgets.
func (s *Store) migrate(ctx context.Context) error {
	entries, err := migrations.ReadDir("migrations")
	if err != nil {
		return fmt.Errorf("reading migrations: %w", err)
	}
	for _, entry := range entries {
		if entry.IsDir() || !strings.HasSuffix(entry.Name(), ".sql") {
			continue
		}
		body, err := migrations.ReadFile("migrations/" + entry.Name())
		if err != nil {
			return fmt.Errorf("reading %s: %w", entry.Name(), err)
		}
		if _, err := s.pool.Exec(ctx, string(body)); err != nil {
			return fmt.Errorf("applying %s: %w", entry.Name(), err)
		}
		s.log.Info("migration applied", "file", entry.Name())
	}
	return nil
}

// VisitorHash turns a client address into a stable, salted, non-reversible id.
//
// Truncated to sixteen bytes: enough that two visitors will not collide, short enough that the
// column is cheap, and — with the salt — not something a leaked database could be walked back to
// an address by trying every IPv4.
func (s *Store) VisitorHash(remoteAddr string) string {
	host := remoteAddr
	if index := strings.LastIndex(host, ":"); index > 0 && !strings.Contains(host, "]") {
		host = host[:index]
	}
	sum := sha256.Sum256([]byte(s.salt + "|" + host))
	return hex.EncodeToString(sum[:16])
}

// ── Writes ───────────────────────────────────────────────────────────────────

type Generation struct {
	VisitorHash    string
	AppName        string
	PackageName    string
	Preset         string
	Features       []string
	FeatureModules []string
	MinSDK         int
	TargetSDK      int
	MotionStyle    string
	FontName       string
	AccentColour   string
	Succeeded      bool
	ZipBytes       int64
	Duration       time.Duration
	FailureReason  string
	Country        string
	Referrer       string
}

func (s *Store) RecordGeneration(ctx context.Context, g Generation) {
	const query = `
		INSERT INTO generations (
			visitor_hash, app_name, package_name, preset, features, feature_modules,
			min_sdk, target_sdk, motion_style, font_name, accent_colour,
			succeeded, zip_bytes, duration_ms, failure_reason, country, referrer
		) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17)`

	_, err := s.pool.Exec(ctx, query,
		g.VisitorHash, g.AppName, g.PackageName, nullable(g.Preset), g.Features, g.FeatureModules,
		nullableInt(g.MinSDK), nullableInt(g.TargetSDK), nullable(g.MotionStyle),
		nullable(g.FontName), nullable(g.AccentColour),
		g.Succeeded, nullableInt64(g.ZipBytes), g.Duration.Milliseconds(),
		nullable(g.FailureReason), nullable(g.Country), nullable(g.Referrer),
	)
	if err != nil {
		s.log.Error("recording a generation", "error", err)
	}
}

// digitsAndPaths is what makes two occurrences of the same bug group together: temp directory
// names, line numbers and hex addresses differ between runs and say nothing about which bug it is.
var digitsAndPaths = regexp.MustCompile(`(0x[0-9a-f]+|[/\\][^\s'"]+|\d+)`)

func Fingerprint(kind, message string) string {
	normalised := digitsAndPaths.ReplaceAllString(message, "*")
	sum := sha256.Sum256([]byte(kind + "|" + normalised))
	return hex.EncodeToString(sum[:12])
}

func (s *Store) RecordError(ctx context.Context, kind, message, detail, path string) {
	const query = `
		INSERT INTO errors (fingerprint, kind, message, detail, path)
		VALUES ($1,$2,$3,$4,$5)
		ON CONFLICT (fingerprint) DO UPDATE SET
			last_seen_at = now(),
			occurrences  = errors.occurrences + 1,
			-- A bug that reappears after being ticked off is not resolved.
			resolved     = FALSE,
			detail       = COALESCE(EXCLUDED.detail, errors.detail)`

	if _, err := s.pool.Exec(ctx, query,
		Fingerprint(kind, message), kind, truncate(message, 1000), nullable(truncate(detail, 8000)),
		nullable(path),
	); err != nil {
		s.log.Error("recording an error", "error", err)
	}
}

// RecordVisit marks one funnel step for one visitor for today. Re-recording the same step is a
// no-op, which is what makes the funnel counts unique visitors rather than page views.
func (s *Store) RecordVisit(ctx context.Context, visitorHash, step string) {
	const query = `
		INSERT INTO visits (day, visitor_hash, step) VALUES (CURRENT_DATE, $1, $2)
		ON CONFLICT DO NOTHING`

	if _, err := s.pool.Exec(ctx, query, visitorHash, step); err != nil {
		s.log.Error("recording a visit", "error", err)
	}
}

func (s *Store) RecordRequest(ctx context.Context, route, method string, status int, duration time.Duration, bytesOut int64) {
	const query = `
		INSERT INTO requests (route, method, status, duration_ms, bytes_out)
		VALUES ($1,$2,$3,$4,$5)`

	if _, err := s.pool.Exec(ctx, query, route, method, status, duration.Milliseconds(), bytesOut); err != nil {
		s.log.Error("recording a request", "error", err)
	}
}

// ── Reads ────────────────────────────────────────────────────────────────────

type Overview struct {
	GenerationsTotal    int64            `json:"generationsTotal"`
	GenerationsToday    int64            `json:"generationsToday"`
	Generations30Days   int64            `json:"generations30Days"`
	SuccessRate         float64          `json:"successRate"`
	UniqueVisitors30    int64            `json:"uniqueVisitors30Days"`
	UniqueVisitorsToday int64            `json:"uniqueVisitorsToday"`
	MedianDurationMs    int              `json:"medianDurationMs"`
	P95DurationMs       int              `json:"p95DurationMs"`
	MedianZipBytes      int64            `json:"medianZipBytes"`
	OpenErrors          int64            `json:"openErrors"`
	Funnel              map[string]int64 `json:"funnel"`
}

func (s *Store) Overview(ctx context.Context) (Overview, error) {
	var o Overview
	o.Funnel = map[string]int64{}

	const headline = `
		SELECT
			count(*),
			count(*) FILTER (WHERE created_at >= CURRENT_DATE),
			count(*) FILTER (WHERE created_at >= now() - interval '30 days'),
			COALESCE(avg(CASE WHEN succeeded THEN 1.0 ELSE 0.0 END), 0),
			COALESCE(percentile_disc(0.5) WITHIN GROUP (ORDER BY duration_ms), 0),
			COALESCE(percentile_disc(0.95) WITHIN GROUP (ORDER BY duration_ms), 0),
			COALESCE(percentile_disc(0.5) WITHIN GROUP (ORDER BY zip_bytes), 0)
		FROM generations`

	if err := s.pool.QueryRow(ctx, headline).Scan(
		&o.GenerationsTotal, &o.GenerationsToday, &o.Generations30Days,
		&o.SuccessRate, &o.MedianDurationMs, &o.P95DurationMs, &o.MedianZipBytes,
	); err != nil {
		return o, fmt.Errorf("reading the overview: %w", err)
	}

	const visitors = `
		SELECT
			count(DISTINCT visitor_hash) FILTER (WHERE day >= CURRENT_DATE - 29),
			count(DISTINCT visitor_hash) FILTER (WHERE day = CURRENT_DATE)
		FROM visits`
	if err := s.pool.QueryRow(ctx, visitors).Scan(&o.UniqueVisitors30, &o.UniqueVisitorsToday); err != nil {
		return o, fmt.Errorf("reading visitor counts: %w", err)
	}

	if err := s.pool.QueryRow(ctx,
		`SELECT count(*) FROM errors WHERE NOT resolved`,
	).Scan(&o.OpenErrors); err != nil {
		return o, fmt.Errorf("reading the error count: %w", err)
	}

	rows, err := s.pool.Query(ctx, `
		SELECT step, count(DISTINCT visitor_hash)
		FROM visits WHERE day >= CURRENT_DATE - 29
		GROUP BY step`)
	if err != nil {
		return o, fmt.Errorf("reading the funnel: %w", err)
	}
	defer rows.Close()
	for rows.Next() {
		var step string
		var count int64
		if err := rows.Scan(&step, &count); err != nil {
			return o, err
		}
		o.Funnel[step] = count
	}
	return o, rows.Err()
}

type DayPoint struct {
	Day      string `json:"day"`
	Total    int64  `json:"total"`
	Failed   int64  `json:"failed"`
	Visitors int64  `json:"visitors"`
}

func (s *Store) Daily(ctx context.Context, days int) ([]DayPoint, error) {
	// generate_series so a day with nothing on it is a zero rather than a gap — a sparse chart
	// with the gaps closed up misrepresents a quiet week as a busy one.
	const query = `
		WITH span AS (
			SELECT generate_series(CURRENT_DATE - ($1::int - 1), CURRENT_DATE, '1 day')::date AS day
		)
		SELECT
			span.day::text,
			COALESCE(g.total, 0),
			COALESCE(g.failed, 0),
			COALESCE(v.visitors, 0)
		FROM span
		LEFT JOIN (
			SELECT created_at::date AS day, count(*) AS total,
			       count(*) FILTER (WHERE NOT succeeded) AS failed
			FROM generations GROUP BY 1
		) g ON g.day = span.day
		LEFT JOIN (
			SELECT day, count(DISTINCT visitor_hash) AS visitors FROM visits GROUP BY 1
		) v ON v.day = span.day
		ORDER BY span.day`

	rows, err := s.pool.Query(ctx, query, days)
	if err != nil {
		return nil, fmt.Errorf("reading the daily series: %w", err)
	}
	defer rows.Close()

	var points []DayPoint
	for rows.Next() {
		var p DayPoint
		if err := rows.Scan(&p.Day, &p.Total, &p.Failed, &p.Visitors); err != nil {
			return nil, err
		}
		points = append(points, p)
	}
	return points, rows.Err()
}

type FeatureCount struct {
	Feature string  `json:"feature"`
	Count   int64   `json:"count"`
	Share   float64 `json:"share"`
}

func (s *Store) FeaturePopularity(ctx context.Context) ([]FeatureCount, error) {
	const query = `
		WITH total AS (SELECT NULLIF(count(*), 0)::float AS n FROM generations WHERE succeeded)
		SELECT feature, count(*), count(*) / (SELECT n FROM total)
		FROM generations, unnest(features) AS feature
		WHERE succeeded
		GROUP BY feature
		ORDER BY 2 DESC`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("reading feature popularity: %w", err)
	}
	defer rows.Close()

	var counts []FeatureCount
	for rows.Next() {
		var c FeatureCount
		if err := rows.Scan(&c.Feature, &c.Count, &c.Share); err != nil {
			return nil, err
		}
		counts = append(counts, c)
	}
	return counts, rows.Err()
}

type ErrorGroup struct {
	ID          int64     `json:"id"`
	Fingerprint string    `json:"fingerprint"`
	Kind        string    `json:"kind"`
	Message     string    `json:"message"`
	Detail      string    `json:"detail"`
	Path        string    `json:"path"`
	Occurrences int       `json:"occurrences"`
	FirstSeen   time.Time `json:"firstSeen"`
	LastSeen    time.Time `json:"lastSeen"`
	Resolved    bool      `json:"resolved"`
}

func (s *Store) Errors(ctx context.Context, includeResolved bool, limit int) ([]ErrorGroup, error) {
	const query = `
		SELECT id, fingerprint, kind, message, COALESCE(detail, ''), COALESCE(path, ''),
		       occurrences, first_seen_at, last_seen_at, resolved
		FROM errors
		WHERE ($1 OR NOT resolved)
		ORDER BY resolved, last_seen_at DESC
		LIMIT $2`

	rows, err := s.pool.Query(ctx, query, includeResolved, limit)
	if err != nil {
		return nil, fmt.Errorf("reading errors: %w", err)
	}
	defer rows.Close()

	var groups []ErrorGroup
	for rows.Next() {
		var g ErrorGroup
		if err := rows.Scan(&g.ID, &g.Fingerprint, &g.Kind, &g.Message, &g.Detail, &g.Path,
			&g.Occurrences, &g.FirstSeen, &g.LastSeen, &g.Resolved); err != nil {
			return nil, err
		}
		groups = append(groups, g)
	}
	return groups, rows.Err()
}

func (s *Store) ResolveError(ctx context.Context, id int64, resolved bool) error {
	tag, err := s.pool.Exec(ctx, `UPDATE errors SET resolved = $2 WHERE id = $1`, id, resolved)
	if err != nil {
		return fmt.Errorf("updating the error: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return errors.New("no error with that id")
	}
	return nil
}

type RecentGeneration struct {
	At          time.Time `json:"at"`
	AppName     string    `json:"appName"`
	PackageName string    `json:"packageName"`
	Features    []string  `json:"features"`
	Succeeded   bool      `json:"succeeded"`
	DurationMs  int       `json:"durationMs"`
	ZipBytes    int64     `json:"zipBytes"`
	Failure     string    `json:"failure"`
}

func (s *Store) RecentGenerations(ctx context.Context, limit int) ([]RecentGeneration, error) {
	const query = `
		SELECT created_at, app_name, package_name, features, succeeded,
		       duration_ms, COALESCE(zip_bytes, 0), COALESCE(failure_reason, '')
		FROM generations ORDER BY created_at DESC LIMIT $1`

	rows, err := s.pool.Query(ctx, query, limit)
	if err != nil {
		return nil, fmt.Errorf("reading recent generations: %w", err)
	}
	defer rows.Close()

	var recent []RecentGeneration
	for rows.Next() {
		var r RecentGeneration
		if err := rows.Scan(&r.At, &r.AppName, &r.PackageName, &r.Features, &r.Succeeded,
			&r.DurationMs, &r.ZipBytes, &r.Failure); err != nil {
			return nil, err
		}
		recent = append(recent, r)
	}
	return recent, rows.Err()
}

type RouteHealth struct {
	Route     string  `json:"route"`
	Requests  int64   `json:"requests"`
	ErrorRate float64 `json:"errorRate"`
	MedianMs  int     `json:"medianMs"`
	P95Ms     int     `json:"p95Ms"`
}

func (s *Store) Health(ctx context.Context) ([]RouteHealth, error) {
	const query = `
		SELECT route,
		       count(*),
		       COALESCE(avg(CASE WHEN status >= 500 THEN 1.0 ELSE 0.0 END), 0),
		       COALESCE(percentile_disc(0.5)  WITHIN GROUP (ORDER BY duration_ms), 0),
		       COALESCE(percentile_disc(0.95) WITHIN GROUP (ORDER BY duration_ms), 0)
		FROM requests
		WHERE at >= now() - interval '7 days'
		GROUP BY route ORDER BY 2 DESC`

	rows, err := s.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("reading route health: %w", err)
	}
	defer rows.Close()

	var health []RouteHealth
	for rows.Next() {
		var h RouteHealth
		if err := rows.Scan(&h.Route, &h.Requests, &h.ErrorRate, &h.MedianMs, &h.P95Ms); err != nil {
			return nil, err
		}
		health = append(health, h)
	}
	return health, rows.Err()
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

func truncate(s string, limit int) string {
	if len(s) <= limit {
		return s
	}
	return s[:limit] + "…"
}
