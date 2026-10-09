package store

import (
	"context"
	"fmt"
	"time"
)

// Retention is how long each kind of row is kept.
type Retention struct {
	// Requests is per-request route timing, only useful while it is recent.
	Requests time.Duration
	// Events covers the funnel events and the visits they belong to.
	Events time.Duration
	// Names is how long a generation keeps what identifies its project (names, addresses, deep
	// link, Apple team) and the generator's warnings. The rest of the row stays, so adoption and
	// failure history survive.
	Names time.Duration
}

// RunRetention prunes once the database is ready and then every hour until ctx ends. Several
// instances may run it at once; every statement is idempotent.
func (s *Store) RunRetention(ctx context.Context, r Retention) {
	select {
	case <-s.ready:
	case <-ctx.Done():
		return
	}
	ticker := time.NewTicker(time.Hour)
	defer ticker.Stop()
	for {
		if err := s.prune(ctx, r); err != nil {
			s.log.Error("retention", "error", err)
		}
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
		}
	}
}

func (s *Store) prune(ctx context.Context, r Retention) error {
	ctx, cancel := context.WithTimeout(ctx, time.Minute)
	defer cancel()

	statements := []struct {
		sql string
		age time.Duration
	}{
		{`DELETE FROM requests WHERE at < now() - $1::interval`, r.Requests},
		{`DELETE FROM events WHERE at < now() - $1::interval`, r.Events},
		{`DELETE FROM visits WHERE at < now() - $1::interval`, r.Events},
		// The spec names the project too, as do its server addresses, deep link and Apple team, both
		// where schema 2 keeps them and flat as schema 1 sent them. A rejection message often quotes
		// the value that was rejected, and a warning can quote a keystore's name or keytool's output.
		// A refused spec can hold anything under "android" or "ios", so those are only stripped when
		// they are objects: a path operator through an array would fail the whole statement.
		{`UPDATE generations
		  SET app_name = NULL,
		      package_name = NULL,
		      spec = (spec - '{app_name,display_name,package_name,api_base_urls,web_socket_urls,deeplink_scheme,deeplink_host}'::text[])
		          || CASE WHEN jsonb_typeof(spec -> 'android') = 'object' THEN jsonb_build_object('android',
		                 (spec -> 'android') - '{api_base_urls,web_socket_urls,deeplink_scheme,deeplink_host}'::text[])
		                 ELSE '{}' END
		          || CASE WHEN jsonb_typeof(spec -> 'ios') = 'object' THEN jsonb_build_object('ios',
		                 (spec -> 'ios') - '{bundle_id,team_id}'::text[])
		                 ELSE '{}' END,
		      failure_reason = CASE WHEN outcome = 'invalid' THEN NULL ELSE failure_reason END,
		      warnings = '{}'
		  WHERE created_at < now() - $1::interval
		    AND (app_name IS NOT NULL OR package_name IS NOT NULL
		         OR spec ?| '{app_name,display_name,package_name,api_base_urls,web_socket_urls,deeplink_scheme,deeplink_host}'
		         OR (jsonb_typeof(spec -> 'android') = 'object'
		             AND spec -> 'android' ?| '{api_base_urls,web_socket_urls,deeplink_scheme,deeplink_host}')
		         OR (jsonb_typeof(spec -> 'ios') = 'object' AND spec -> 'ios' ?| '{bundle_id,team_id}')
		         OR (outcome = 'invalid' AND failure_reason IS NOT NULL)
		         OR warnings <> '{}')`,
			r.Names},
	}
	for _, statement := range statements {
		tag, err := s.pool.Exec(ctx, statement.sql, statement.age)
		if err != nil {
			return fmt.Errorf("pruning: %w", err)
		}
		if tag.RowsAffected() > 0 {
			s.log.Info("retention", "statement", tag.String())
		}
	}
	return nil
}
