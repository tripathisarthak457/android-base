package store

import (
	"context"
	"errors"
	"slices"
	"strings"
	"testing"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgconn"
)

// fakeConn records every statement the runner sends, in order.
type fakeConn struct {
	log     []string
	applied []string
	failOn  string
}

func (c *fakeConn) record(sql string, args ...any) {
	line := strings.Join(strings.Fields(sql), " ")
	if len(line) > 60 {
		line = line[:60]
	}
	for _, arg := range args {
		if s, ok := arg.(string); ok {
			line += " [" + s + "]"
		}
	}
	c.log = append(c.log, line)
}

func (c *fakeConn) Exec(_ context.Context, sql string, args ...any) (pgconn.CommandTag, error) {
	c.record(sql, args...)
	return pgconn.CommandTag{}, nil
}

func (c *fakeConn) QueryRow(_ context.Context, sql string, args ...any) pgx.Row {
	c.record(sql, args...)
	return appliedRow{c.applied}
}

func (c *fakeConn) Begin(context.Context) (pgx.Tx, error) {
	c.record("BEGIN")
	return &fakeTx{conn: c}, nil
}

type appliedRow struct{ versions []string }

func (r appliedRow) Scan(dest ...any) error {
	*dest[0].(*[]string) = slices.Clone(r.versions)
	return nil
}

type fakeTx struct {
	pgx.Tx
	conn *fakeConn
	done bool
}

func (t *fakeTx) Exec(_ context.Context, sql string, args ...any) (pgconn.CommandTag, error) {
	if t.conn.failOn != "" && strings.Contains(sql, t.conn.failOn) {
		t.conn.record("FAILED " + sql)
		return pgconn.CommandTag{}, errors.New("syntax error")
	}
	t.conn.record(sql, args...)
	return pgconn.CommandTag{}, nil
}

func (t *fakeTx) Commit(context.Context) error {
	t.done = true
	t.conn.record("COMMIT")
	return nil
}

func (t *fakeTx) Rollback(context.Context) error {
	if !t.done {
		t.conn.record("ROLLBACK")
	}
	return nil
}

func TestEmbeddedMigrationsAreOrderedAndNamed(t *testing.T) {
	all, err := migrations()
	if err != nil {
		t.Fatal(err)
	}
	var versions []string
	for _, m := range all {
		versions = append(versions, m.version)
		if strings.TrimSpace(m.sql) == "" {
			t.Errorf("migration %s is empty", m.version)
		}
	}
	if !slices.Equal(versions, []string{"0001", "0002", "0003", "0004"}) {
		t.Fatalf("versions %v", versions)
	}
}

var testMigrations = []migration{
	{version: "0001", sql: "CREATE TABLE a ()"},
	{version: "0002", sql: "CREATE TABLE b ()"},
	{version: "0003", sql: "ALTER TABLE a ADD COLUMN c int"},
}

func TestRunnerAppliesOnlyWhatIsMissingInOrderUnderTheLock(t *testing.T) {
	conn := &fakeConn{applied: []string{"0001"}}
	applied, err := runMigrations(context.Background(), conn, testMigrations)
	if err != nil {
		t.Fatal(err)
	}
	if !slices.Equal(applied, []string{"0002", "0003"}) {
		t.Fatalf("applied %v", applied)
	}
	want := []string{
		"SELECT pg_advisory_lock($1)",
		"CREATE TABLE IF NOT EXISTS schema_migrations",
		"SELECT COALESCE(array_agg(version), '{}') FROM schema_migrat",
		"BEGIN",
		"CREATE TABLE b ()",
		"INSERT INTO schema_migrations (version) VALUES ($1) [0002]",
		"COMMIT",
		"BEGIN",
		"ALTER TABLE a ADD COLUMN c int",
		"INSERT INTO schema_migrations (version) VALUES ($1) [0003]",
		"COMMIT",
		"SELECT pg_advisory_unlock($1)",
	}
	matches := len(conn.log) == len(want)
	for i := 0; matches && i < len(want); i++ {
		matches = strings.HasPrefix(conn.log[i], want[i])
	}
	if !matches {
		t.Fatalf("statements:\n%s\nwant:\n%s", strings.Join(conn.log, "\n"), strings.Join(want, "\n"))
	}
}

func TestAFailedMigrationIsRolledBackUnrecordedAndUnlocks(t *testing.T) {
	conn := &fakeConn{failOn: "ALTER TABLE"}
	applied, err := runMigrations(context.Background(), conn, testMigrations)
	if err == nil || !strings.Contains(err.Error(), "0003") {
		t.Fatalf("want an error naming 0003, got %v", err)
	}
	if !slices.Equal(applied, []string{"0001", "0002"}) {
		t.Fatalf("applied %v", applied)
	}
	tail := conn.log[len(conn.log)-3:]
	if !strings.HasPrefix(tail[0], "FAILED ALTER TABLE") || tail[1] != "ROLLBACK" || tail[2] != "SELECT pg_advisory_unlock($1)" {
		t.Fatalf("ended with %v", tail)
	}
	for _, line := range conn.log {
		if strings.Contains(line, "[0003]") {
			t.Fatal("a failed migration was recorded as applied")
		}
	}
}

func TestNothingToDoTakesAndReleasesTheLock(t *testing.T) {
	conn := &fakeConn{applied: []string{"0001", "0002", "0003"}}
	if _, err := runMigrations(context.Background(), conn, testMigrations); err != nil {
		t.Fatal(err)
	}
	if len(conn.log) != 4 || conn.log[3] != "SELECT pg_advisory_unlock($1)" {
		t.Fatalf("statements %v", conn.log)
	}
}
