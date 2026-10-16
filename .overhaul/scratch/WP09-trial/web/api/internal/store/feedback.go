package store

import (
	"context"
	"fmt"
	"time"

	"github.com/base-android/generator-api/internal/clip"
)

// Feedback is a bug report or a suggestion from a person, as opposed to `errors`, which is what the
// software noticed about itself. Its JSON names predate the v1 API and are kept, so the report form
// did not have to change with it.
type Feedback struct {
	ID           int64     `json:"id"`
	CreatedAt    time.Time `json:"createdAt"`
	Target       string    `json:"target"`
	Kind         string    `json:"kind"`
	Severity     string    `json:"severity"`
	Area         string    `json:"area"`
	Title        string    `json:"title"`
	Body         string    `json:"body"`
	Steps        string    `json:"steps"`
	Expected     string    `json:"expected"`
	Actual       string    `json:"actual"`
	AppName      string    `json:"appName"`
	PackageName  string    `json:"packageName"`
	Features     []string  `json:"features"`
	Preset       string    `json:"preset"`
	MinSDK       int       `json:"minSdk"`
	MotionStyle  string    `json:"motionStyle"`
	FontName     string    `json:"fontName"`
	AccentColour string    `json:"accentColour"`
	Contact      string    `json:"contact"`
	UserAgent    string    `json:"userAgent"`
	PageURL      string    `json:"pageUrl"`
	AppVersion   string    `json:"appVersion"`
	Status       string    `json:"status"`
	Notes        string    `json:"notes"`
	VisitorHash  string    `json:"-"`
}

// FeedbackStatuses is the closed set the portal filters by.
var FeedbackStatuses = []string{"new", "triaged", "fixed", "wontfix"}

// SaveFeedback returns an error rather than swallowing it, unlike telemetry: somebody took the
// trouble to write this, and they are told when it could not be kept.
func (s *Store) SaveFeedback(ctx context.Context, f Feedback) (int64, error) {
	if !s.Ready() {
		return 0, ErrNotReady
	}
	const query = `
		INSERT INTO feedback (
			visitor_hash, target, kind, severity, area, title, body, steps, expected, actual,
			app_name, package_name, features, preset, min_sdk, motion_style, font_name,
			accent_colour, contact, user_agent, page_url, app_version
		) VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18,$19,$20,$21,$22)
		RETURNING id`

	var id int64
	err := s.pool.QueryRow(ctx, query,
		f.VisitorHash, nullable(f.Target), f.Kind, nullable(f.Severity), nullable(f.Area),
		clip.String(f.Title, 200), clip.String(f.Body, 8000),
		nullable(clip.String(f.Steps, 4000)), nullable(clip.String(f.Expected, 2000)),
		nullable(clip.String(f.Actual, 2000)),
		nullable(clip.String(f.AppName, 120)), nullable(clip.String(f.PackageName, 120)), list(f.Features),
		nullable(clip.String(f.Preset, 64)), nullableInt(f.MinSDK), nullable(clip.String(f.MotionStyle, 64)),
		nullable(clip.String(f.FontName, 120)), nullable(clip.String(f.AccentColour, 32)),
		nullable(clip.String(f.Contact, 200)), nullable(clip.String(f.UserAgent, 500)),
		nullable(clip.String(f.PageURL, 500)), nullable(clip.String(f.AppVersion, 64)),
	).Scan(&id)
	if err != nil {
		return 0, fmt.Errorf("saving feedback: %w", err)
	}
	return id, nil
}

// FeedbackList returns reports in the window, blocking bugs first. An empty status means all.
func (s *Store) FeedbackList(ctx context.Context, f Filter, status string, limit int) ([]Feedback, error) {
	if !s.Ready() {
		return nil, ErrNotReady
	}
	const query = `
		SELECT id, created_at, COALESCE(target,''), kind, COALESCE(severity,''), COALESCE(area,''),
		       title, body, COALESCE(steps,''), COALESCE(expected,''), COALESCE(actual,''),
		       COALESCE(app_name,''), COALESCE(package_name,''), features, COALESCE(preset,''),
		       COALESCE(min_sdk,0), COALESCE(motion_style,''), COALESCE(font_name,''),
		       COALESCE(accent_colour,''), COALESCE(contact,''), COALESCE(user_agent,''),
		       COALESCE(page_url,''), COALESCE(app_version,''), status, COALESCE(notes,'')
		FROM feedback
		WHERE created_at >= $1 AND created_at < $2
		  AND ($3 = '' OR status = $3)
		  AND ($4 = '' OR target = $4)
		ORDER BY
			-- Blocking bugs first however old, then everything else newest first. Sorting purely
			-- by date buries the one report that matters under a week of suggestions.
			CASE WHEN status = 'new' AND severity = 'blocks' THEN 0 ELSE 1 END,
			created_at DESC
		LIMIT $5`

	rows, err := s.pool.Query(ctx, query, f.From, f.To, status, f.Target, limit)
	if err != nil {
		return nil, fmt.Errorf("reading feedback: %w", err)
	}
	defer rows.Close()

	all := []Feedback{}
	for rows.Next() {
		var r Feedback
		if err := rows.Scan(
			&r.ID, &r.CreatedAt, &r.Target, &r.Kind, &r.Severity, &r.Area, &r.Title, &r.Body,
			&r.Steps, &r.Expected, &r.Actual, &r.AppName, &r.PackageName, &r.Features, &r.Preset,
			&r.MinSDK, &r.MotionStyle, &r.FontName, &r.AccentColour, &r.Contact, &r.UserAgent,
			&r.PageURL, &r.AppVersion, &r.Status, &r.Notes,
		); err != nil {
			return nil, err
		}
		r.CreatedAt = r.CreatedAt.UTC()
		all = append(all, r)
	}
	return all, rows.Err()
}

type FeedbackCounts struct {
	New       int64 `json:"new"`
	Blocking  int64 `json:"blocking"`
	Total     int64 `json:"total"`
	Last7Days int64 `json:"last7Days"`
}

func (s *Store) FeedbackCounts(ctx context.Context) (FeedbackCounts, error) {
	var c FeedbackCounts
	if !s.Ready() {
		return c, ErrNotReady
	}
	const query = `
		SELECT
			count(*) FILTER (WHERE status = 'new'),
			count(*) FILTER (WHERE status = 'new' AND severity = 'blocks'),
			count(*),
			count(*) FILTER (WHERE created_at >= now() - interval '7 days')
		FROM feedback`

	if err := s.pool.QueryRow(ctx, query).Scan(&c.New, &c.Blocking, &c.Total, &c.Last7Days); err != nil {
		return c, fmt.Errorf("counting feedback: %w", err)
	}
	return c, nil
}

// UpdateFeedback sets the status when status is not empty, and the notes when notes is not nil; an
// empty string clears them.
func (s *Store) UpdateFeedback(ctx context.Context, id int64, status string, notes *string) error {
	if !s.Ready() {
		return ErrNotReady
	}
	const query = `
		UPDATE feedback
		SET status     = COALESCE(NULLIF($2, ''), status),
		    notes      = CASE WHEN $3 THEN NULLIF($4, '') ELSE notes END,
		    updated_at = now()
		WHERE id = $1`

	note := ""
	if notes != nil {
		note = clip.String(*notes, 4000)
	}
	tag, err := s.pool.Exec(ctx, query, id, status, notes != nil, note)
	if err != nil {
		return fmt.Errorf("updating feedback: %w", err)
	}
	if tag.RowsAffected() == 0 {
		return ErrNotFound
	}
	return nil
}
