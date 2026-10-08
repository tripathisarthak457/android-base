package store

import (
	"cmp"
	"context"
	"encoding/json"
	"fmt"
	"slices"
	"strconv"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/base-android/generator-api/internal/telemetry"
)

// Filter is the admin portal's global window and filters (MASTER-PLAN A10.3). The window is
// half-open: From is the start of the first day, To the start of the day after the last.
type Filter struct {
	From        time.Time
	To          time.Time
	Target      string
	Preset      string
	Outcome     string
	Country     string
	DeviceClass string
	Source      string
}

// params numbers query arguments as they are added.
type params struct{ args []any }

func (p *params) add(value any) string {
	p.args = append(p.args, value)
	return "$" + strconv.Itoa(len(p.args))
}

// generated is the outcome set that produced a project.
const generated = `('ok', 'download_interrupted')`

// generations returns the FROM clause and conditions selecting generation rows `g` that match f.
// The source filter lives on the session's landed visit, so it joins one in.
func (f Filter) generations(p *params) (from, where string) {
	from = "generations g"
	conditions := []string{"g.created_at >= " + p.add(f.From), "g.created_at < " + p.add(f.To)}
	for _, c := range []struct{ column, value string }{
		{"g.target", f.Target}, {"g.preset", f.Preset}, {"g.outcome", f.Outcome},
		{"g.country", f.Country}, {"g.device_class", f.DeviceClass},
	} {
		if c.value != "" {
			conditions = append(conditions, c.column+" = "+p.add(c.value))
		}
	}
	if f.Source != "" {
		from += " JOIN visits v ON v.session_id = g.session_id"
		conditions = append(conditions, "v.source = "+p.add(f.Source))
	}
	return from, strings.Join(conditions, " AND ")
}

// sessions returns conditions selecting visits `v` in the window. Preset and outcome describe
// generations, not visits, and do not apply. withSession leaves out the rows of the legacy
// /api/track alias, which have no session.
func (f Filter) sessions(p *params, withSession bool) string {
	conditions := []string{"v.at >= " + p.add(f.From), "v.at < " + p.add(f.To)}
	if withSession {
		conditions = append(conditions, "v.session_id IS NOT NULL")
	}
	if f.Target != "" {
		conditions = append(conditions,
			"EXISTS (SELECT 1 FROM events t WHERE t.session_id = v.session_id AND t.target = "+p.add(f.Target)+")")
	}
	for _, c := range []struct{ column, value string }{
		{"v.country", f.Country}, {"v.device_class", f.DeviceClass}, {"v.source", f.Source},
	} {
		if c.value != "" {
			conditions = append(conditions, c.column+" = "+p.add(c.value))
		}
	}
	return strings.Join(conditions, " AND ")
}

func (f Filter) visitFiltered() bool { return f.Country != "" || f.DeviceClass != "" || f.Source != "" }

func (s *Store) query(ctx context.Context, sql string, args []any, scan func(pgx.Rows) error) error {
	if !s.Ready() {
		return ErrNotReady
	}
	rows, err := s.pool.Query(ctx, sql, args...)
	if err != nil {
		return err
	}
	defer rows.Close()
	for rows.Next() {
		if err := scan(rows); err != nil {
			return err
		}
	}
	return rows.Err()
}

func (s *Store) queryRow(ctx context.Context, sql string, args []any, into ...any) error {
	if !s.Ready() {
		return ErrNotReady
	}
	return s.pool.QueryRow(ctx, sql, args...).Scan(into...)
}

// ── Overview ─────────────────────────────────────────────────────────────────

type Overview struct {
	Generations    int64            `json:"generations"`
	Outcomes       map[string]int64 `json:"outcomes"`
	MedianMs       int              `json:"median_ms"`
	P95Ms          int              `json:"p95_ms"`
	Visitors       int64            `json:"visitors"`
	Sessions       int64            `json:"sessions"`
	ReturningShare float64          `json:"returning_share"`
	OpenErrors     int64            `json:"open_errors"`
	NewFeedback    int64            `json:"new_feedback"`
}

func (s *Store) Overview(ctx context.Context, f Filter) (Overview, error) {
	o := Overview{Outcomes: map[string]int64{}}
	for _, outcome := range telemetry.Outcomes {
		o.Outcomes[string(outcome)] = 0
	}

	var p params
	from, where := f.generations(&p)
	err := s.query(ctx, `SELECT g.outcome, count(*) FROM `+from+` WHERE `+where+` GROUP BY 1`, p.args,
		func(rows pgx.Rows) error {
			var outcome string
			var n int64
			if err := rows.Scan(&outcome, &n); err != nil {
				return err
			}
			o.Outcomes[outcome] = n
			o.Generations += n
			return nil
		})
	if err != nil {
		return o, fmt.Errorf("counting outcomes: %w", err)
	}

	p = params{}
	from, where = f.generations(&p)
	if err := s.queryRow(ctx, `
		SELECT COALESCE(percentile_disc(0.5)  WITHIN GROUP (ORDER BY g.duration_ms), 0),
		       COALESCE(percentile_disc(0.95) WITHIN GROUP (ORDER BY g.duration_ms), 0)
		FROM `+from+` WHERE `+where+` AND g.outcome IN `+generated, p.args,
		&o.MedianMs, &o.P95Ms); err != nil {
		return o, fmt.Errorf("reading generation times: %w", err)
	}

	p = params{}
	if err := s.queryRow(ctx, `
		SELECT count(DISTINCT (v.day, v.visitor_hash)),
		       count(DISTINCT v.session_id),
		       COALESCE(avg(CASE WHEN v.returning_visitor THEN 1.0 ELSE 0.0 END)
		                FILTER (WHERE v.session_id IS NOT NULL), 0)
		FROM visits v WHERE `+f.sessions(&p, false), p.args,
		&o.Visitors, &o.Sessions, &o.ReturningShare); err != nil {
		return o, fmt.Errorf("counting visitors: %w", err)
	}

	if err := s.queryRow(ctx, `
		SELECT (SELECT count(*) FROM errors WHERE NOT resolved),
		       (SELECT count(*) FROM feedback WHERE status = 'new')`, nil,
		&o.OpenErrors, &o.NewFeedback); err != nil {
		return o, fmt.Errorf("counting open items: %w", err)
	}
	return o, nil
}

// ── Funnel ───────────────────────────────────────────────────────────────────

// Funnel holds the number of sessions that reached each step, per target. A step key is the event
// name, with `:<step>` for step_viewed.
type Funnel struct {
	Landed  int64
	Targets map[string]map[string]int64
}

func (s *Store) Funnel(ctx context.Context, f Filter) (Funnel, error) {
	funnel := Funnel{Targets: map[string]map[string]int64{}}

	var p params
	everyTarget := f
	everyTarget.Target = ""
	if err := s.queryRow(ctx, `SELECT count(DISTINCT v.session_id) FROM visits v WHERE `+everyTarget.sessions(&p, true),
		p.args, &funnel.Landed); err != nil {
		return funnel, fmt.Errorf("counting landings: %w", err)
	}

	p = params{}
	from := "events e"
	conditions := []string{
		"e.session_id IS NOT NULL", "e.target IS NOT NULL", "e.at >= " + p.add(f.From), "e.at < " + p.add(f.To),
	}
	if f.Target != "" {
		conditions = append(conditions, "e.target = "+p.add(f.Target))
	}
	if f.visitFiltered() {
		from += " JOIN visits v ON v.session_id = e.session_id"
		for _, c := range []struct{ column, value string }{
			{"v.country", f.Country}, {"v.device_class", f.DeviceClass}, {"v.source", f.Source},
		} {
			if c.value != "" {
				conditions = append(conditions, c.column+" = "+p.add(c.value))
			}
		}
	}
	err := s.query(ctx, `
		SELECT e.target, e.name || COALESCE(':' || e.step, ''), count(DISTINCT e.session_id)
		FROM `+from+` WHERE `+strings.Join(conditions, " AND ")+` GROUP BY 1, 2`, p.args,
		func(rows pgx.Rows) error {
			var target, key string
			var n int64
			if err := rows.Scan(&target, &key, &n); err != nil {
				return err
			}
			if funnel.Targets[target] == nil {
				funnel.Targets[target] = map[string]int64{}
			}
			funnel.Targets[target][key] = n
			return nil
		})
	if err != nil {
		return funnel, fmt.Errorf("reading the funnel: %w", err)
	}
	return funnel, nil
}

// ── Series ───────────────────────────────────────────────────────────────────

type SeriesDay struct {
	Day      string           `json:"day"`
	Outcomes map[string]int64 `json:"outcomes"`
	Visitors int64            `json:"visitors"`
	Sessions int64            `json:"sessions"`
}

// Series returns one point per day of the window, zeros included: a chart with the quiet days left
// out shows a quiet week as a busy one. Generations come from the daily rollup unless the source
// filter needs each generation's session.
func (s *Store) Series(ctx context.Context, f Filter) ([]SeriesDay, error) {
	days := map[string]*SeriesDay{}
	var series []SeriesDay
	for day := f.From; day.Before(f.To); day = day.AddDate(0, 0, 1) {
		series = append(series, SeriesDay{Day: day.Format(time.DateOnly), Outcomes: map[string]int64{}})
	}
	for i := range series {
		for _, outcome := range telemetry.Outcomes {
			series[i].Outcomes[string(outcome)] = 0
		}
		days[series[i].Day] = &series[i]
	}

	var p params
	var sql string
	if f.Source == "" {
		conditions := []string{"r.day >= " + p.add(f.From) + "::date", "r.day < " + p.add(f.To) + "::date"}
		for _, c := range []struct{ column, value string }{
			{"r.target", f.Target}, {"r.preset", f.Preset}, {"r.outcome", f.Outcome},
			{"r.country", f.Country}, {"r.device_class", f.DeviceClass},
		} {
			if c.value != "" {
				conditions = append(conditions, c.column+" = "+p.add(c.value))
			}
		}
		sql = `SELECT r.day::text, r.outcome, sum(r.n)::bigint FROM daily_rollup r
		       WHERE ` + strings.Join(conditions, " AND ") + ` GROUP BY 1, 2`
	} else {
		from, where := f.generations(&p)
		sql = `SELECT g.created_at::date::text, g.outcome, count(*) FROM ` + from + ` WHERE ` + where + ` GROUP BY 1, 2`
	}
	err := s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var day, outcome string
		var n int64
		if err := rows.Scan(&day, &outcome, &n); err != nil {
			return err
		}
		if point := days[day]; point != nil {
			point.Outcomes[outcome] += n
		}
		return nil
	})
	if err != nil {
		return nil, fmt.Errorf("reading the daily series: %w", err)
	}

	p = params{}
	err = s.query(ctx, `
		SELECT v.at::date::text, count(DISTINCT v.visitor_hash), count(DISTINCT v.session_id)
		FROM visits v WHERE `+f.sessions(&p, false)+` GROUP BY 1`, p.args,
		func(rows pgx.Rows) error {
			var day string
			var visitors, sessions int64
			if err := rows.Scan(&day, &visitors, &sessions); err != nil {
				return err
			}
			if point := days[day]; point != nil {
				point.Visitors, point.Sessions = visitors, sessions
			}
			return nil
		})
	if err != nil {
		return nil, fmt.Errorf("reading daily visitors: %w", err)
	}
	return series, nil
}

// ── Adoption ─────────────────────────────────────────────────────────────────

type FeatureAdoption struct {
	Key string `json:"key"`
	// N projects had the feature; Picked of them added it on top of their preset.
	N      int64 `json:"n"`
	Picked int64 `json:"picked"`
}

type Count struct {
	Key string `json:"key"`
	N   int64  `json:"n"`
}

type Week struct {
	Week  string `json:"week"`
	Total int64  `json:"total"`
	// Values holds, per dimension (features, presets, architectures, design_styles,
	// motion_styles, languages), the number of projects with each value that week.
	Values map[string]map[string]int64 `json:"values"`
}

type Adoption struct {
	Total      int64              `json:"total"`
	Features   []FeatureAdoption  `json:"features"`
	Dimensions map[string][]Count `json:"dimensions"`
	Weekly     []Week             `json:"weekly"`
}

// AdoptionDimensions are the scalar and list columns adoption breaks down, besides features.
var AdoptionDimensions = []string{"presets", "architectures", "design_styles", "motion_styles", "languages"}

// Adoption counts what successful projects contained. featureKeys is the target's catalogue, so a
// feature nobody picked shows as a zero instead of being missing.
func (s *Store) Adoption(ctx context.Context, f Filter, featureKeys []string) (Adoption, error) {
	adoption := Adoption{Dimensions: map[string][]Count{}}

	base := func(p *params) string {
		from, where := f.generations(p)
		return `WITH g AS (SELECT g.* FROM ` + from + ` WHERE ` + where + ` AND g.outcome IN ` + generated + `)`
	}

	var p params
	sql := base(&p) + `
		SELECT k.key,
		       count(g.id) FILTER (WHERE k.key = ANY (g.features)),
		       count(g.id) FILTER (WHERE k.key = ANY (g.added))
		FROM unnest(` + p.add(featureKeys) + `::text[]) WITH ORDINALITY AS k(key, position)
		LEFT JOIN g ON true
		GROUP BY k.key, k.position ORDER BY k.position`
	err := s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var row FeatureAdoption
		if err := rows.Scan(&row.Key, &row.N, &row.Picked); err != nil {
			return err
		}
		adoption.Features = append(adoption.Features, row)
		return nil
	})
	if err != nil {
		return adoption, fmt.Errorf("reading feature adoption: %w", err)
	}

	p = params{}
	sql = base(&p) + `, d AS (
			SELECT date_trunc('week', created_at)::date AS week, 'total' AS dim, '' AS key FROM g
			UNION ALL SELECT date_trunc('week', created_at)::date, 'presets', COALESCE(preset, '') FROM g
			UNION ALL SELECT date_trunc('week', created_at)::date, 'architectures', COALESCE(architecture, '') FROM g
			UNION ALL SELECT date_trunc('week', created_at)::date, 'design_styles', COALESCE(design_style, '') FROM g
			UNION ALL SELECT date_trunc('week', created_at)::date, 'motion_styles', COALESCE(motion_style, '') FROM g
			UNION ALL SELECT date_trunc('week', g.created_at)::date, 'languages', l FROM g, unnest(g.languages) AS l
			UNION ALL SELECT date_trunc('week', g.created_at)::date, 'features', x FROM g, unnest(g.features) AS x
		)
		SELECT week::text, dim, key, count(*) FROM d GROUP BY 1, 2, 3 ORDER BY 1`
	weeks := map[string]*Week{}
	var order []string
	totals := map[string]map[string]int64{}
	err = s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var week, dim, key string
		var n int64
		if err := rows.Scan(&week, &dim, &key, &n); err != nil {
			return err
		}
		w := weeks[week]
		if w == nil {
			w = &Week{Week: week, Values: map[string]map[string]int64{}}
			weeks[week] = w
			order = append(order, week)
		}
		if dim == "total" {
			w.Total = n
			adoption.Total += n
			return nil
		}
		if w.Values[dim] == nil {
			w.Values[dim] = map[string]int64{}
		}
		w.Values[dim][key] = n
		if totals[dim] == nil {
			totals[dim] = map[string]int64{}
		}
		totals[dim][key] += n
		return nil
	})
	if err != nil {
		return adoption, fmt.Errorf("reading weekly adoption: %w", err)
	}
	adoption.Weekly = []Week{}
	for _, week := range order {
		adoption.Weekly = append(adoption.Weekly, *weeks[week])
	}

	for _, dim := range AdoptionDimensions {
		counts := []Count{}
		for key, n := range totals[dim] {
			counts = append(counts, Count{Key: key, N: n})
		}
		sortCounts(counts)
		adoption.Dimensions[dim] = counts
	}
	return adoption, nil
}

// ── Failures ─────────────────────────────────────────────────────────────────

type Rejection struct {
	Field  string `json:"field"`
	Reason string `json:"reason"`
	N      int64  `json:"n"`
}

type Rate struct {
	N      int64 `json:"n"`
	Failed int64 `json:"failed"`
}

type FeatureFailure struct {
	Key string `json:"key"`
	On  Rate   `json:"on"`
	Off Rate   `json:"off"`
}

type PairFailure struct {
	A string `json:"a"`
	B string `json:"b"`
	Rate
}

type GroupFailure struct {
	Key string `json:"key"`
	Rate
}

type Failures struct {
	Rejections []Rejection      `json:"rejections"`
	ByFeature  []FeatureFailure `json:"by_feature"`
	Pairs      []PairFailure    `json:"pairs"`
	ByMinSDK   []GroupFailure   `json:"by_min_sdk"`
	ByPreset   []GroupFailure   `json:"by_preset"`
	ByCommit   []GroupFailure   `json:"by_commit"`
}

// MinPairSamples keeps a pair of features out of the failing-pairs list until it has been generated
// often enough for its rate to mean something.
const MinPairSamples = 5

// Failures separates the generator's own failures (internal errors and timeouts) from rejections of
// the input, and breaks the former down by what was in the project. The outcome filter does not
// apply: the point is to compare outcomes.
func (s *Store) Failures(ctx context.Context, f Filter, featureKeys []string) (Failures, error) {
	failures := Failures{
		Rejections: []Rejection{}, ByFeature: []FeatureFailure{}, Pairs: []PairFailure{},
		ByMinSDK: []GroupFailure{}, ByPreset: []GroupFailure{}, ByCommit: []GroupFailure{},
	}
	f.Outcome = ""

	var p params
	from, where := f.generations(&p)
	err := s.query(ctx, `
		SELECT COALESCE(g.failure_field, ''), COALESCE(g.failure_reason, ''), count(*)
		FROM `+from+` WHERE `+where+` AND g.outcome = 'invalid'
		GROUP BY 1, 2 ORDER BY 3 DESC LIMIT 20`, p.args,
		func(rows pgx.Rows) error {
			var r Rejection
			if err := rows.Scan(&r.Field, &r.Reason, &r.N); err != nil {
				return err
			}
			failures.Rejections = append(failures.Rejections, r)
			return nil
		})
	if err != nil {
		return failures, fmt.Errorf("reading rejections: %w", err)
	}

	base := func(p *params) string {
		from, where := f.generations(p)
		return `WITH g AS (
			SELECT g.features, g.min_sdk, g.preset, g.generator_commit,
			       g.outcome IN ('internal_error', 'timeout') AS failed
			FROM ` + from + ` WHERE ` + where + `
			  AND g.outcome IN ('ok', 'download_interrupted', 'internal_error', 'timeout'))`
	}

	p = params{}
	sql := base(&p) + `
		SELECT k.key,
		       count(g.failed) FILTER (WHERE k.key = ANY (g.features)),
		       count(g.failed) FILTER (WHERE k.key = ANY (g.features) AND g.failed),
		       count(g.failed) FILTER (WHERE NOT k.key = ANY (g.features)),
		       count(g.failed) FILTER (WHERE NOT k.key = ANY (g.features) AND g.failed)
		FROM unnest(` + p.add(featureKeys) + `::text[]) WITH ORDINALITY AS k(key, position)
		LEFT JOIN g ON true
		GROUP BY k.key, k.position ORDER BY k.position`
	err = s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var r FeatureFailure
		if err := rows.Scan(&r.Key, &r.On.N, &r.On.Failed, &r.Off.N, &r.Off.Failed); err != nil {
			return err
		}
		failures.ByFeature = append(failures.ByFeature, r)
		return nil
	})
	if err != nil {
		return failures, fmt.Errorf("reading failures by feature: %w", err)
	}

	p = params{}
	sql = base(&p) + `
		SELECT a, b, count(*), count(*) FILTER (WHERE g.failed)
		FROM g, unnest(g.features) AS a, unnest(g.features) AS b
		WHERE a < b
		GROUP BY a, b
		HAVING count(*) >= ` + p.add(MinPairSamples) + ` AND count(*) FILTER (WHERE g.failed) > 0
		ORDER BY count(*) FILTER (WHERE g.failed)::float / count(*) DESC, count(*) DESC
		LIMIT 20`
	err = s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var r PairFailure
		if err := rows.Scan(&r.A, &r.B, &r.N, &r.Failed); err != nil {
			return err
		}
		failures.Pairs = append(failures.Pairs, r)
		return nil
	})
	if err != nil {
		return failures, fmt.Errorf("reading failing pairs: %w", err)
	}

	p = params{}
	sql = base(&p) + `
		SELECT 'min_sdk', COALESCE(min_sdk::text, ''), count(*), count(*) FILTER (WHERE failed) FROM g GROUP BY 2
		UNION ALL
		SELECT 'preset', COALESCE(preset, ''), count(*), count(*) FILTER (WHERE failed) FROM g GROUP BY 2
		UNION ALL
		SELECT 'commit', COALESCE(generator_commit, ''), count(*), count(*) FILTER (WHERE failed) FROM g GROUP BY 2
		ORDER BY 1, 3 DESC`
	err = s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var dim string
		var r GroupFailure
		if err := rows.Scan(&dim, &r.Key, &r.N, &r.Failed); err != nil {
			return err
		}
		switch dim {
		case "min_sdk":
			failures.ByMinSDK = append(failures.ByMinSDK, r)
		case "preset":
			failures.ByPreset = append(failures.ByPreset, r)
		case "commit":
			failures.ByCommit = append(failures.ByCommit, r)
		}
		return nil
	})
	if err != nil {
		return failures, fmt.Errorf("reading failures by group: %w", err)
	}
	return failures, nil
}

// ── Audience ─────────────────────────────────────────────────────────────────

type Bucket struct {
	Key       string `json:"key"`
	Country   string `json:"country,omitempty"`
	Sessions  int64  `json:"sessions"`
	Generated int64  `json:"generated"`
}

type Campaign struct {
	Source    string `json:"source"`
	Medium    string `json:"medium"`
	Campaign  string `json:"campaign"`
	Sessions  int64  `json:"sessions"`
	Generated int64  `json:"generated"`
}

type Audience struct {
	Sessions int64 `json:"sessions"`
	// Buckets holds countries, regions, cities, browsers, os, device_classes, viewports, sources and
	// returning, each with the sessions that went on to generate a project.
	Buckets   map[string][]Bucket `json:"buckets"`
	Campaigns []Campaign          `json:"campaigns"`
	// Clients counts generations, not sessions: scripts and crawlers have no session.
	Clients []Count `json:"clients"`
}

// AudienceDimensions are the keys of Audience.Buckets.
var AudienceDimensions = []string{
	"countries", "regions", "cities", "browsers", "os", "device_classes", "viewports", "sources", "returning",
}

// MinCitySessions keeps a city out of the audience until enough sessions share it that none of them
// stands out.
const MinCitySessions = 5

func (s *Store) Audience(ctx context.Context, f Filter) (Audience, error) {
	audience := Audience{Buckets: map[string][]Bucket{}, Campaigns: []Campaign{}, Clients: []Count{}}
	for _, dim := range AudienceDimensions {
		audience.Buckets[dim] = []Bucket{}
	}

	sessions := func(p *params) string {
		return `WITH s AS (
			SELECT v.*, EXISTS (
				SELECT 1 FROM generations g WHERE g.session_id = v.session_id AND g.outcome IN ` + generated + `
			) AS generated
			FROM visits v WHERE ` + f.sessions(p, true) + `)`
	}

	var p params
	if err := s.queryRow(ctx, sessions(&p)+` SELECT count(*) FROM s`, p.args, &audience.Sessions); err != nil {
		return audience, fmt.Errorf("counting sessions: %w", err)
	}

	p = params{}
	sql := sessions(&p) + `
		SELECT 'countries', COALESCE(country, ''), '', count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		UNION ALL SELECT 'regions', region, COALESCE(country, ''), count(*), count(*) FILTER (WHERE generated)
			FROM s WHERE region IS NOT NULL GROUP BY 2, 3
		UNION ALL SELECT 'cities', city, COALESCE(country, ''), count(*), count(*) FILTER (WHERE generated)
			FROM s WHERE city IS NOT NULL GROUP BY 2, 3 HAVING count(*) >= ` + p.add(MinCitySessions) + `
		UNION ALL SELECT 'browsers', COALESCE(browser, ''), '', count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		UNION ALL SELECT 'os', COALESCE(os, ''), '', count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		UNION ALL SELECT 'device_classes', COALESCE(device_class, ''), '', count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		UNION ALL SELECT 'viewports', COALESCE(viewport, ''), '', count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		UNION ALL SELECT 'sources', source, '', count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		UNION ALL SELECT 'returning', CASE WHEN returning_visitor THEN 'returning' ELSE 'new' END, '',
			count(*), count(*) FILTER (WHERE generated) FROM s GROUP BY 2
		ORDER BY 1, 4 DESC`
	err := s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var dim string
		var b Bucket
		if err := rows.Scan(&dim, &b.Key, &b.Country, &b.Sessions, &b.Generated); err != nil {
			return err
		}
		audience.Buckets[dim] = append(audience.Buckets[dim], b)
		return nil
	})
	if err != nil {
		return audience, fmt.Errorf("reading the audience: %w", err)
	}

	p = params{}
	sql = sessions(&p) + `
		SELECT COALESCE(utm_source, ''), COALESCE(utm_medium, ''), COALESCE(utm_campaign, ''),
		       count(*), count(*) FILTER (WHERE generated)
		FROM s WHERE utm_source IS NOT NULL OR utm_medium IS NOT NULL OR utm_campaign IS NOT NULL
		GROUP BY 1, 2, 3 ORDER BY 4 DESC LIMIT 50`
	err = s.query(ctx, sql, p.args, func(rows pgx.Rows) error {
		var c Campaign
		if err := rows.Scan(&c.Source, &c.Medium, &c.Campaign, &c.Sessions, &c.Generated); err != nil {
			return err
		}
		audience.Campaigns = append(audience.Campaigns, c)
		return nil
	})
	if err != nil {
		return audience, fmt.Errorf("reading campaigns: %w", err)
	}

	p = params{}
	from, where := f.generations(&p)
	err = s.query(ctx, `SELECT COALESCE(g.client, ''), count(*) FROM `+from+` WHERE `+where+` GROUP BY 1 ORDER BY 2 DESC`,
		p.args, func(rows pgx.Rows) error {
			var c Count
			if err := rows.Scan(&c.Key, &c.N); err != nil {
				return err
			}
			audience.Clients = append(audience.Clients, c)
			return nil
		})
	if err != nil {
		return audience, fmt.Errorf("counting clients: %w", err)
	}
	return audience, nil
}

// ── Durations ────────────────────────────────────────────────────────────────

type Timing struct {
	N     int64 `json:"n"`
	P50Ms int   `json:"p50_ms"`
	P95Ms int   `json:"p95_ms"`
}

type DurationBucket struct {
	// UpToMs is the bucket's exclusive upper bound.
	UpToMs int   `json:"up_to_ms"`
	N      int64 `json:"n"`
}

type FeatureCountTiming struct {
	Bucket string `json:"bucket"`
	Timing
}

type Durations struct {
	// Config is time in the configurator, from the identity step to the Generate click.
	Config        Timing               `json:"config"`
	ConfigBuckets []DurationBucket     `json:"config_buckets"`
	ByFeatures    []FeatureCountTiming `json:"generation_by_feature_count"`
	Queue         Timing               `json:"queue"`
	Download      Timing               `json:"download"`
}

// configBounds are the upper bounds of the configurator-time buckets: 30 s, 1, 2, 5, 10 and 30
// minutes, and the two-hour cap.
var configBounds = []int{30_000, 60_000, 120_000, 300_000, 600_000, 1_800_000, telemetry.MaxConfigMs + 1}

var featureCountBuckets = []string{"0-9", "10-19", "20-29", "30+"}

func (s *Store) Durations(ctx context.Context, f Filter) (Durations, error) {
	d := Durations{ByFeatures: []FeatureCountTiming{}}
	timing := func(column, condition string, into *Timing) error {
		var p params
		from, where := f.generations(&p)
		return s.queryRow(ctx, `
			SELECT count(`+column+`),
			       COALESCE(percentile_disc(0.5)  WITHIN GROUP (ORDER BY `+column+`), 0),
			       COALESCE(percentile_disc(0.95) WITHIN GROUP (ORDER BY `+column+`), 0)
			FROM `+from+` WHERE `+where+` AND `+column+` IS NOT NULL AND `+condition, p.args,
			&into.N, &into.P50Ms, &into.P95Ms)
	}
	if err := timing("g.config_ms", "true", &d.Config); err != nil {
		return d, fmt.Errorf("reading configurator times: %w", err)
	}
	if err := timing("g.queue_ms", "true", &d.Queue); err != nil {
		return d, fmt.Errorf("reading queue times: %w", err)
	}
	if err := timing("g.stream_ms", "g.outcome = 'ok'", &d.Download); err != nil {
		return d, fmt.Errorf("reading download times: %w", err)
	}

	counts := make([]int64, len(configBounds))
	var p params
	from, where := f.generations(&p)
	err := s.query(ctx, `
		SELECT width_bucket(g.config_ms, `+p.add(configBounds[:len(configBounds)-1])+`::int[]), count(*)
		FROM `+from+` WHERE `+where+` AND g.config_ms IS NOT NULL GROUP BY 1`, p.args,
		func(rows pgx.Rows) error {
			var bucket int
			var n int64
			if err := rows.Scan(&bucket, &n); err != nil {
				return err
			}
			if bucket >= 0 && bucket < len(counts) {
				counts[bucket] = n
			}
			return nil
		})
	if err != nil {
		return d, fmt.Errorf("reading configurator buckets: %w", err)
	}
	for i, bound := range configBounds {
		d.ConfigBuckets = append(d.ConfigBuckets, DurationBucket{UpToMs: bound, N: counts[i]})
	}

	p = params{}
	from, where = f.generations(&p)
	byBucket := map[int]Timing{}
	err = s.query(ctx, `
		SELECT LEAST(cardinality(g.features) / 10, 3), count(*),
		       percentile_disc(0.5)  WITHIN GROUP (ORDER BY g.python_ms),
		       percentile_disc(0.95) WITHIN GROUP (ORDER BY g.python_ms)
		FROM `+from+` WHERE `+where+` AND g.python_ms IS NOT NULL AND g.outcome IN `+generated+`
		GROUP BY 1`, p.args,
		func(rows pgx.Rows) error {
			var bucket int
			var t Timing
			if err := rows.Scan(&bucket, &t.N, &t.P50Ms, &t.P95Ms); err != nil {
				return err
			}
			byBucket[bucket] = t
			return nil
		})
	if err != nil {
		return d, fmt.Errorf("reading generation times by feature count: %w", err)
	}
	for i, label := range featureCountBuckets {
		d.ByFeatures = append(d.ByFeatures, FeatureCountTiming{Bucket: label, Timing: byBucket[i]})
	}
	return d, nil
}

// ── Generations ──────────────────────────────────────────────────────────────

type GenerationRow struct {
	ID              int64           `json:"id"`
	At              time.Time       `json:"at"`
	Target          string          `json:"target"`
	Outcome         string          `json:"outcome"`
	AppName         string          `json:"app_name"`
	PackageName     string          `json:"package_name"`
	Preset          string          `json:"preset"`
	Features        []string        `json:"features"`
	Added           []string        `json:"added"`
	Removed         []string        `json:"removed"`
	FeatureModules  []string        `json:"feature_modules"`
	Architecture    string          `json:"architecture"`
	DesignStyle     string          `json:"design_style"`
	MotionStyle     string          `json:"motion_style"`
	Languages       []string        `json:"languages"`
	MinSDK          *int            `json:"min_sdk"`
	TargetSDK       *int            `json:"target_sdk"`
	Glass           *bool           `json:"glass"`
	DurationMs      int             `json:"duration_ms"`
	QueueMs         *int            `json:"queue_ms"`
	PythonMs        *int            `json:"python_ms"`
	StreamMs        *int            `json:"stream_ms"`
	ConfigMs        *int            `json:"config_ms"`
	ZipBytes        *int64          `json:"zip_bytes"`
	ColdStart       *bool           `json:"cold_start"`
	Client          string          `json:"client"`
	Country         string          `json:"country"`
	DeviceClass     string          `json:"device_class"`
	FailureReason   string          `json:"failure_reason"`
	FailureField    string          `json:"failure_field"`
	GeneratorCommit string          `json:"generator_commit"`
	Spec            json.RawMessage `json:"spec"`
}

// Generations returns rows newest first. before is the id the previous page ended at, 0 for the
// first page; paging by id stays correct while new rows arrive, which an offset does not.
func (s *Store) Generations(ctx context.Context, f Filter, before int64, limit int) ([]GenerationRow, error) {
	var p params
	from, where := f.generations(&p)
	if before > 0 {
		where += " AND g.id < " + p.add(before)
	}
	sql := `
		SELECT g.id, g.created_at, g.target, g.outcome, COALESCE(g.app_name, ''), COALESCE(g.package_name, ''),
		       COALESCE(g.preset, ''), g.features, g.added, g.removed, g.feature_modules,
		       COALESCE(g.architecture, ''), COALESCE(g.design_style, ''), COALESCE(g.motion_style, ''),
		       g.languages, g.min_sdk, g.target_sdk, g.glass, g.duration_ms, g.queue_ms, g.python_ms,
		       g.stream_ms, g.config_ms, g.zip_bytes, g.cold_start, COALESCE(g.client, ''),
		       COALESCE(g.country, ''), COALESCE(g.device_class, ''), COALESCE(g.failure_reason, ''),
		       COALESCE(g.failure_field, ''), COALESCE(g.generator_commit, ''), COALESCE(g.spec, 'null'::jsonb)
		FROM ` + from + ` WHERE ` + where + `
		ORDER BY g.id DESC LIMIT ` + p.add(limit)

	rows := []GenerationRow{}
	err := s.query(ctx, sql, p.args, func(r pgx.Rows) error {
		var g GenerationRow
		var spec []byte
		if err := r.Scan(&g.ID, &g.At, &g.Target, &g.Outcome, &g.AppName, &g.PackageName, &g.Preset,
			&g.Features, &g.Added, &g.Removed, &g.FeatureModules, &g.Architecture, &g.DesignStyle,
			&g.MotionStyle, &g.Languages, &g.MinSDK, &g.TargetSDK, &g.Glass, &g.DurationMs, &g.QueueMs,
			&g.PythonMs, &g.StreamMs, &g.ConfigMs, &g.ZipBytes, &g.ColdStart, &g.Client, &g.Country,
			&g.DeviceClass, &g.FailureReason, &g.FailureField, &g.GeneratorCommit, &spec); err != nil {
			return err
		}
		g.Spec = spec
		rows = append(rows, g)
		return nil
	})
	if err != nil {
		return nil, fmt.Errorf("reading generations: %w", err)
	}
	return rows, nil
}

// ── Errors and route health ──────────────────────────────────────────────────

type ErrorGroup struct {
	ID          int64     `json:"id"`
	Fingerprint string    `json:"fingerprint"`
	Kind        string    `json:"kind"`
	Message     string    `json:"message"`
	Detail      string    `json:"detail"`
	Path        string    `json:"path"`
	Occurrences int       `json:"occurrences"`
	FirstSeen   time.Time `json:"first_seen"`
	LastSeen    time.Time `json:"last_seen"`
	Resolved    bool      `json:"resolved"`
}

// Errors returns error groups last seen in the window, open ones first.
func (s *Store) Errors(ctx context.Context, f Filter, includeResolved bool, limit int) ([]ErrorGroup, error) {
	groups := []ErrorGroup{}
	err := s.query(ctx, `
		SELECT id, fingerprint, kind, message, COALESCE(detail, ''), COALESCE(path, ''),
		       occurrences, first_seen_at, last_seen_at, resolved
		FROM errors
		WHERE last_seen_at >= $1 AND last_seen_at < $2 AND ($3 OR NOT resolved)
		ORDER BY resolved, last_seen_at DESC
		LIMIT $4`, []any{f.From, f.To, includeResolved, limit},
		func(rows pgx.Rows) error {
			var g ErrorGroup
			if err := rows.Scan(&g.ID, &g.Fingerprint, &g.Kind, &g.Message, &g.Detail, &g.Path,
				&g.Occurrences, &g.FirstSeen, &g.LastSeen, &g.Resolved); err != nil {
				return err
			}
			groups = append(groups, g)
			return nil
		})
	if err != nil {
		return nil, fmt.Errorf("reading errors: %w", err)
	}
	return groups, nil
}

func (s *Store) ResolveError(ctx context.Context, id int64, resolved bool) error {
	if !s.Ready() {
		return ErrNotReady
	}
	tag, err := s.pool.Exec(ctx, `UPDATE errors SET resolved = $2 WHERE id = $1`, id, resolved)
	if err != nil {
		return fmt.Errorf("updating the error: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}

type RouteHealth struct {
	Route     string  `json:"route"`
	Requests  int64   `json:"requests"`
	ErrorRate float64 `json:"error_rate"`
	MedianMs  int     `json:"median_ms"`
	P95Ms     int     `json:"p95_ms"`
}

func (s *Store) RouteHealth(ctx context.Context, f Filter) ([]RouteHealth, error) {
	health := []RouteHealth{}
	err := s.query(ctx, `
		SELECT route,
		       count(*),
		       COALESCE(avg(CASE WHEN status >= 500 THEN 1.0 ELSE 0.0 END), 0),
		       COALESCE(percentile_disc(0.5)  WITHIN GROUP (ORDER BY duration_ms), 0),
		       COALESCE(percentile_disc(0.95) WITHIN GROUP (ORDER BY duration_ms), 0)
		FROM requests
		WHERE at >= $1 AND at < $2
		GROUP BY route ORDER BY 2 DESC`, []any{f.From, f.To},
		func(rows pgx.Rows) error {
			var h RouteHealth
			if err := rows.Scan(&h.Route, &h.Requests, &h.ErrorRate, &h.MedianMs, &h.P95Ms); err != nil {
				return err
			}
			health = append(health, h)
			return nil
		})
	if err != nil {
		return nil, fmt.Errorf("reading route health: %w", err)
	}
	return health, nil
}

// sortCounts orders by count, largest first, then by key so equal counts keep a stable order.
func sortCounts(counts []Count) {
	slices.SortFunc(counts, func(a, b Count) int {
		if c := cmp.Compare(b.N, a.N); c != 0 {
			return c
		}
		return strings.Compare(a.Key, b.Key)
	})
}
