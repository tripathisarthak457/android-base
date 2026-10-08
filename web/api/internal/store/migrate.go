package store

import (
	"context"
	"embed"
	"fmt"
	"regexp"
	"slices"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
)

//go:embed migrations/*.sql
var migrationFiles embed.FS

// migrationLockKey is the pg_advisory_lock key every instance takes before migrating, so two cold
// starts against a fresh database do not race each other's CREATE TABLE.
const migrationLockKey = 72710001

var migrationName = regexp.MustCompile(`^(\d{4})_[a-z0-9_]+\.sql$`)

type migration struct {
	version string
	sql     string
}

// migrations returns the embedded files in version order.
func migrations() ([]migration, error) {
	entries, err := migrationFiles.ReadDir("migrations")
	if err != nil {
		return nil, fmt.Errorf("reading migrations: %w", err)
	}
	var all []migration
	for _, entry := range entries {
		match := migrationName.FindStringSubmatch(entry.Name())
		if match == nil {
			return nil, fmt.Errorf("migration %q is not named NNNN_name.sql", entry.Name())
		}
		body, err := migrationFiles.ReadFile("migrations/" + entry.Name())
		if err != nil {
			return nil, fmt.Errorf("reading %s: %w", entry.Name(), err)
		}
		all = append(all, migration{version: match[1], sql: string(body)})
	}
	slices.SortFunc(all, func(a, b migration) int { return strings.Compare(a.version, b.version) })
	for i := 1; i < len(all); i++ {
		if all[i].version == all[i-1].version {
			return nil, fmt.Errorf("two migrations share version %s", all[i].version)
		}
	}
	return all, nil
}

// migrationConn is the part of a connection the runner uses, so its statement order can be tested
// without a database.
type migrationConn interface {
	Exec(ctx context.Context, sql string, args ...any) (pgconn.CommandTag, error)
	QueryRow(ctx context.Context, sql string, args ...any) pgx.Row
	Begin(ctx context.Context) (pgx.Tx, error)
}

const (
	createMigrationsTable = `CREATE TABLE IF NOT EXISTS schema_migrations (
		version    text        PRIMARY KEY,
		applied_at timestamptz NOT NULL DEFAULT now()
	)`
	selectApplied   = `SELECT COALESCE(array_agg(version), '{}') FROM schema_migrations`
	insertMigration = `INSERT INTO schema_migrations (version) VALUES ($1)`
)

// migrate applies every migration not yet recorded, each in its own transaction, while holding the
// advisory lock. The connection is a dedicated one and is closed afterwards, so a lock left behind
// by a failure dies with it instead of returning to the pool.
func (s *Store) migrate(ctx context.Context) error {
	conn, err := pgx.ConnectConfig(ctx, s.pool.Config().ConnConfig.Copy())
	if err != nil {
		return fmt.Errorf("connecting to migrate: %w", err)
	}
	defer func() { _ = conn.Close(context.Background()) }()

	all, err := migrations()
	if err != nil {
		return err
	}
	applied, err := runMigrations(ctx, conn, all)
	for _, version := range applied {
		s.log.Info("migration applied", "version", version)
	}
	return err
}

func runMigrations(ctx context.Context, conn migrationConn, all []migration) (applied []string, err error) {
	if _, err := conn.Exec(ctx, `SELECT pg_advisory_lock($1)`, migrationLockKey); err != nil {
		return nil, fmt.Errorf("taking the migration lock: %w", err)
	}
	defer func() {
		if _, unlockErr := conn.Exec(context.Background(), `SELECT pg_advisory_unlock($1)`, migrationLockKey); unlockErr != nil && err == nil {
			err = fmt.Errorf("releasing the migration lock: %w", unlockErr)
		}
	}()

	if _, err := conn.Exec(ctx, createMigrationsTable); err != nil {
		return nil, fmt.Errorf("creating schema_migrations: %w", err)
	}
	var done []string
	if err := conn.QueryRow(ctx, selectApplied).Scan(&done); err != nil {
		return nil, fmt.Errorf("reading schema_migrations: %w", err)
	}

	for _, m := range all {
		if slices.Contains(done, m.version) {
			continue
		}
		if err := apply(ctx, conn, m); err != nil {
			return applied, err
		}
		applied = append(applied, m.version)
	}
	return applied, nil
}

func apply(ctx context.Context, conn migrationConn, m migration) error {
	tx, err := conn.Begin(ctx)
	if err != nil {
		return fmt.Errorf("starting migration %s: %w", m.version, err)
	}
	defer func() { _ = tx.Rollback(context.Background()) }()

	if _, err := tx.Exec(ctx, m.sql); err != nil {
		return fmt.Errorf("applying migration %s: %w", m.version, err)
	}
	if _, err := tx.Exec(ctx, insertMigration, m.version); err != nil {
		return fmt.Errorf("recording migration %s: %w", m.version, err)
	}
	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("committing migration %s: %w", m.version, err)
	}
	return nil
}
