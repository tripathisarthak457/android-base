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
	// Names is how long a generation keeps the app and package name it was made with. The rest of
	// the row stays, so adoption and failure history survive.
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
		// The spec carries the same names, and a rejection message often quotes the value that was
		// rejected, so both go with them.
		{`UPDATE generations
		  SET app_name = NULL,
		      package_name = NULL,
		      spec = spec - 'app_name' - 'display_name' - 'package_name' #- '{ios,bundle_id}',
		      failure_reason = CASE WHEN outcome = 'invalid' THEN NULL ELSE failure_reason END
		  WHERE created_at < now() - $1::interval
		    AND (app_name IS NOT NULL OR package_name IS NOT NULL OR spec ? 'app_name' OR spec ? 'package_name'
		         OR (outcome = 'invalid' AND failure_reason IS NOT NULL))`,
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
