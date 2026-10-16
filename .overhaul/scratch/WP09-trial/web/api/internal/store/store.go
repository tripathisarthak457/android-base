// Package store is everything the admin portal reads and everything the API records. A thin layer
// over pgx with hand-written SQL.
package store

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"log/slog"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

// ErrNotReady is returned by reads while the database is still being reached or migrated.
var ErrNotReady = errors.New("the database is not reachable yet")

// ErrNotFound is returned by updates whose id matches no row.
var ErrNotFound = errors.New("no row with that id")

type Store struct {
	pool  *pgxpool.Pool
	salt  string
	log   *slog.Logger
	ready chan struct{}
}

// New prepares a pool without connecting. The generator must keep working while the analytics
// database is down or slow to wake, so connecting and migrating happen in Connect, off the boot path.
func New(databaseURL, ipSalt string, log *slog.Logger) (*Store, error) {
	cfg, err := pgxpool.ParseConfig(databaseURL)
	if err != nil {
		return nil, fmt.Errorf("parsing DATABASE_URL: %w", err)
	}
	// Every day boundary in the admin queries and the rollup is a UTC day, whatever the server's
	// default time zone is.
	cfg.ConnConfig.RuntimeParams["timezone"] = "UTC"
	pool, err := pgxpool.NewWithConfig(context.Background(), cfg)
	if err != nil {
		return nil, fmt.Errorf("configuring the database pool: %w", err)
	}
	return &Store{pool: pool, salt: ipSalt, log: log, ready: make(chan struct{})}, nil
}

// Connect pings and migrates, retrying with backoff until it succeeds or ctx ends. Writes queue in
// the meantime and reads answer ErrNotReady.
func (s *Store) Connect(ctx context.Context) {
	delay := time.Second
	for {
		err := s.connectOnce(ctx)
		if err == nil {
			close(s.ready)
			s.log.Info("database ready")
			return
		}
		s.log.Error("database unavailable", "error", err, "retry_in", delay.String())
		select {
		case <-ctx.Done():
			return
		case <-time.After(delay):
		}
		delay = min(delay*2, time.Minute)
	}
}

func (s *Store) connectOnce(ctx context.Context) error {
	ctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
	defer cancel()
	if err := s.pool.Ping(ctx); err != nil {
		return fmt.Errorf("pinging Postgres: %w", err)
	}
	return s.migrate(ctx)
}

// Ready reports whether Connect has finished.
func (s *Store) Ready() bool {
	select {
	case <-s.ready:
		return true
	default:
		return false
	}
}

func (s *Store) Close() { s.pool.Close() }

// VisitorHash turns a client key (an IPv4 address or an IPv6 /64) into a salted, non-reversible id
// that changes every day. A daily salt counts unique visitors per day without linking a visitor's
// days together; whether someone is returning comes from the browser's saved draft instead.
func (s *Store) VisitorHash(clientKey string, day time.Time) string {
	sum := sha256.Sum256([]byte(s.salt + "|" + day.UTC().Format(time.DateOnly) + "|" + clientKey))
	return hex.EncodeToString(sum[:16])
}
