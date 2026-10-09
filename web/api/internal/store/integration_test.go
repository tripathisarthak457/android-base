package store

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"fmt"
	"io"
	"log/slog"
	"net/url"
	"os"
	"slices"
	"strings"
	"sync"
	"testing"
	"time"

	"github.com/jackc/pgx/v5"

	"github.com/base-android/generator-api/internal/telemetry"
)

var quiet = slog.New(slog.NewTextHandler(io.Discard, nil))

// testDatabase creates an empty database on the server named by TEST_DATABASE_URL and drops it
// afterwards, so each test starts from nothing.
func testDatabase(t *testing.T) string {
	t.Helper()
	admin := os.Getenv("TEST_DATABASE_URL")
	if admin == "" {
		t.Skip("TEST_DATABASE_URL is not set")
	}
	ctx := context.Background()
	conn, err := pgx.Connect(ctx, admin)
	if err != nil {
		t.Fatal(err)
	}
	defer func() { _ = conn.Close(ctx) }()

	suffix := make([]byte, 6)
	_, _ = rand.Read(suffix)
	name := "wp09_" + hex.EncodeToString(suffix)
	if _, err := conn.Exec(ctx, "CREATE DATABASE "+name); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		conn, err := pgx.Connect(context.Background(), admin)
		if err == nil {
			_, _ = conn.Exec(context.Background(), "DROP DATABASE "+name+" WITH (FORCE)")
			_ = conn.Close(context.Background())
		}
	})

	parsed, err := url.Parse(admin)
	if err != nil {
		t.Fatal(err)
	}
	parsed.Path = "/" + name
	return parsed.String()
}

func connected(t *testing.T, databaseURL string) *Store {
	t.Helper()
	s, err := New(databaseURL, "salt", quiet)
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(s.Close)
	ctx, cancel := context.WithTimeout(context.Background(), time.Minute)
	defer cancel()
	s.Connect(ctx)
	if !s.Ready() {
		t.Fatal("the store did not become ready")
	}
	return s
}

func appliedVersions(t *testing.T, s *Store) []string {
	t.Helper()
	var versions []string
	if err := s.pool.QueryRow(context.Background(),
		`SELECT array_agg(version ORDER BY version) FROM schema_migrations`).Scan(&versions); err != nil {
		t.Fatal(err)
	}
	return versions
}

func TestIntegrationMigratesOnceEvenWhenInstancesRace(t *testing.T) {
	databaseURL := testDatabase(t)

	var wg sync.WaitGroup
	stores := make([]*Store, 3)
	for i := range stores {
		s, err := New(databaseURL, "salt", quiet)
		if err != nil {
			t.Fatal(err)
		}
		t.Cleanup(s.Close)
		stores[i] = s
		wg.Add(1)
		go func() {
			defer wg.Done()
			if err := s.connectOnce(context.Background()); err != nil {
				t.Error(err)
			}
		}()
	}
	wg.Wait()

	want := []string{"0001", "0002", "0003", "0004"}
	if got := appliedVersions(t, stores[0]); !slices.Equal(got, want) {
		t.Fatalf("applied %v, want %v", got, want)
	}
	// A restart finds nothing to do.
	if err := stores[0].connectOnce(context.Background()); err != nil {
		t.Fatal(err)
	}
	var rows int
	if err := stores[0].pool.QueryRow(context.Background(), `SELECT count(*) FROM schema_migrations`).Scan(&rows); err != nil {
		t.Fatal(err)
	}
	if rows != 4 {
		t.Fatalf("%d rows in schema_migrations", rows)
	}
}

func TestIntegrationUpgradesADatabaseTheOldRunnerMigrated(t *testing.T) {
	databaseURL := testDatabase(t)
	ctx := context.Background()

	// What production holds today: 0001 and 0002 run by the old runner, with no version table.
	conn, err := pgx.Connect(ctx, databaseURL)
	if err != nil {
		t.Fatal(err)
	}
	for _, file := range []string{"migrations/0001_initial.sql", "migrations/0002_feedback.sql"} {
		body, err := migrationFiles.ReadFile(file)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := conn.Exec(ctx, string(body)); err != nil {
			t.Fatal(err)
		}
	}
	_, err = conn.Exec(ctx, `
		INSERT INTO generations (visitor_hash, app_name, package_name, features, succeeded, duration_ms, failure_reason, created_at)
		VALUES ('v', 'Ok', 'com.example.ok', '{network}', true, 400, NULL, now() - interval '2 days'),
		       ('v', 'Bad', 'com.example.bad', '{}', false, 50, 'The package name is not valid.', now() - interval '2 days'),
		       ('v', 'Slow', 'com.example.slow', '{}', false, 90000, 'the generator timed out after 1m30s', now() - interval '1 day'),
		       ('v', 'Bug', 'com.example.bug', '{}', false, 300, 'the generator failed: exit status 1; stderr: x', now());
		INSERT INTO visits (day, visitor_hash, step) VALUES (CURRENT_DATE, 'v', 'landed'), (CURRENT_DATE, 'v', 'configured');`)
	if err != nil {
		t.Fatal(err)
	}
	_ = conn.Close(ctx)

	s := connected(t, databaseURL)
	if got := appliedVersions(t, s); !slices.Equal(got, []string{"0001", "0002", "0003", "0004"}) {
		t.Fatalf("applied %v", got)
	}

	outcomes := map[string]string{}
	err = s.query(ctx, `SELECT app_name, outcome FROM generations`, nil, func(rows pgx.Rows) error {
		var app, outcome string
		if err := rows.Scan(&app, &outcome); err != nil {
			return err
		}
		outcomes[app] = outcome
		return nil
	})
	if err != nil {
		t.Fatal(err)
	}
	want := map[string]string{"Ok": "ok", "Bad": "invalid", "Slow": "timeout", "Bug": "internal_error"}
	for app, outcome := range want {
		if outcomes[app] != outcome {
			t.Errorf("%s backfilled as %q, want %q", app, outcomes[app], outcome)
		}
	}

	var visits, rollupRows int
	if err := s.pool.QueryRow(ctx, `SELECT (SELECT count(*) FROM visits), (SELECT count(*) FROM daily_rollup)`).Scan(&visits, &rollupRows); err != nil {
		t.Fatal(err)
	}
	if visits != 2 || rollupRows != 4 {
		t.Fatalf("visits %d, rollup rows %d", visits, rollupRows)
	}
}

const (
	sessionA = "11111111-1111-4111-8111-111111111111"
	sessionB = "22222222-2222-4222-8222-222222222222"
)

func intp(v int) *int { return &v }

// seed writes a small, known history through the same batch path the API uses.
func seed(t *testing.T, s *Store) {
	t.Helper()
	ctx := context.Background()
	landing := func(id, utm, referrer string, returning bool, city string) Visit {
		return Visit{
			SessionID: id, VisitorHash: "h" + id[:4],
			Landing: telemetry.Landing{UTMSource: utm, ReferrerOrigin: referrer, Returning: returning, Viewport: "lg"},
			Geo:     telemetry.Geo{Country: "IN", Region: "MH", City: city},
			Agent:   telemetry.Agent{Browser: "chrome", OS: "android", DeviceClass: telemetry.DeviceMobile},
			Client:  telemetry.ClientWeb,
		}
	}
	batch := &Batch{
		Visits: []Visit{
			landing(sessionA, "hn", "", false, "Mumbai"),
			landing(sessionB, "", "https://www.reddit.com", true, "Pune"),
		},
		Events: []Event{
			{SessionID: sessionA, Name: "landed"},
			{SessionID: sessionA, Target: "android", Name: "platform_selected"},
			{SessionID: sessionA, Target: "android", Name: "step_viewed", Step: "identity"},
			{SessionID: sessionA, Target: "android", Name: "generate_clicked"},
			{SessionID: sessionA, Target: "android", Name: "generated", Client: telemetry.ClientWeb, ConfigMs: intp(90000)},
			{SessionID: sessionA, Target: "android", Name: "downloaded"},
			{SessionID: sessionB, Name: "landed"},
			{SessionID: sessionB, Target: "multiplatform", Name: "platform_selected"},
		},
		Errors:   []ErrorRecord{{Kind: "generation", Message: "android: KeyError", Detail: "Traceback", Path: "/api/v1/android/generate"}},
		Requests: []Request{{Route: "generate", Method: "POST", Status: 200, Duration: 400 * time.Millisecond, Bytes: 1000}},
	}
	// Six generations with network+auth so the pair clears MinPairSamples, one failure among them.
	for i := 0; i < 6; i++ {
		g := Generation{
			Target: "android", Outcome: telemetry.OK, SessionID: sessionA, VisitorHash: "h", Client: telemetry.ClientWeb,
			ConfigMs: intp(45_000 + i*1000), AppName: fmt.Sprintf("App %d", i), PackageName: "com.example.app",
			Preset: "standard", Features: []string{"network", "auth", "settings"}, Added: []string{"auth"},
			Architecture: "compact", DesignStyle: "Utility", MotionStyle: "Standard", Languages: []string{"hi"},
			MinSDK: 26, TargetSDK: 37, Spec: []byte(`{"app_name": "App", "package_name": "com.example.app", "ios": {"bundle_id": "x"}}`),
			QueueMs: intp(5), PythonMs: intp(300 + i), StreamMs: intp(20), Duration: time.Duration(310+i) * time.Millisecond,
			ZipBytes: 500_000, GeneratorCommit: "99c4edf", Country: "IN", DeviceClass: telemetry.DeviceMobile,
			Notes: []string{`Moved min_sdk under "android".`}, IgnoredFields: []string{"theme"},
			Warnings: []string{"Keystore 'release' could not be created: keytool error"},
		}
		if i == 5 {
			// As the API records a generator bug: described from the spec and the catalogue, with no
			// zip and nothing the generator said about the spec.
			g.Outcome, g.FailureReason = telemetry.InternalError, "The generator failed."
			g.StreamMs, g.ZipBytes, g.Notes, g.IgnoredFields, g.Warnings = nil, 0, nil, nil, nil
		}
		batch.Generations = append(batch.Generations, g)
	}
	batch.Generations = append(batch.Generations,
		Generation{Target: "android", Outcome: telemetry.Invalid, VisitorHash: "h", Client: telemetry.ClientCLI,
			FailureReason: "The package name is not valid.", FailureField: "package_name", Duration: 40 * time.Millisecond},
		Generation{Target: "multiplatform", Outcome: telemetry.OK, SessionID: sessionB, VisitorHash: "h",
			Client: telemetry.ClientWeb, Preset: "standard", Features: []string{"network"}, PythonMs: intp(800),
			Duration: 900 * time.Millisecond, Country: "IN", DeviceClass: telemetry.DeviceMobile},
		Generation{Target: "android", Outcome: telemetry.RateLimited, VisitorHash: "h", Client: telemetry.ClientBot},
	)
	if err := s.write(ctx, batch); err != nil {
		t.Fatal(err)
	}
	// Three more sessions from Mumbai so it clears MinCitySessions, and Pune stays hidden.
	more := &Batch{}
	for i := 3; i <= 5; i++ {
		more.Visits = append(more.Visits, landing(fmt.Sprintf("%d%d%d%d%d%d%d%d-1111-4111-8111-111111111111", i, i, i, i, i, i, i, i), "", "", false, "Mumbai"))
	}
	more.Visits = append(more.Visits, landing("66666666-6666-4666-8666-666666666666", "", "", false, "Mumbai"))
	if err := s.write(ctx, more); err != nil {
		t.Fatal(err)
	}
}

func today() Filter {
	day := time.Now().UTC().Truncate(24 * time.Hour)
	return Filter{From: day.AddDate(0, 0, -29), To: day.AddDate(0, 0, 1)}
}

func TestIntegrationAdminQueriesReadWhatWasRecorded(t *testing.T) {
	s := connected(t, testDatabase(t))
	seed(t, s)
	ctx := context.Background()
	f := today()
	keys := []string{"network", "auth", "settings", "liquidglass"}

	overview, err := s.Overview(ctx, f)
	if err != nil {
		t.Fatal(err)
	}
	if overview.Generations != 9 || overview.Outcomes["ok"] != 6 || overview.Outcomes["invalid"] != 1 ||
		overview.Outcomes["internal_error"] != 1 || overview.Outcomes["rate_limited"] != 1 {
		t.Fatalf("overview %+v", overview)
	}
	if overview.Sessions != 6 || overview.OpenErrors != 1 || overview.ReturningShare <= 0 || overview.MedianMs == 0 {
		t.Fatalf("overview %+v", overview)
	}

	funnel, err := s.Funnel(ctx, f)
	if err != nil {
		t.Fatal(err)
	}
	if funnel.Landed != 6 || funnel.Targets["android"]["step_viewed:identity"] != 1 ||
		funnel.Targets["android"]["downloaded"] != 1 || funnel.Targets["multiplatform"]["platform_selected"] != 1 {
		t.Fatalf("funnel %+v", funnel)
	}
	android := f
	android.Target = "android"
	if funnel, err = s.Funnel(ctx, android); err != nil || funnel.Targets["multiplatform"] != nil {
		t.Fatalf("target filter: %+v %v", funnel, err)
	}

	series, err := s.Series(ctx, f)
	if err != nil {
		t.Fatal(err)
	}
	last := series[len(series)-1]
	if len(series) != 30 || last.Outcomes["ok"] != 6 || last.Outcomes["internal_error"] != 1 || last.Sessions != 6 {
		t.Fatalf("%d days, today %+v", len(series), last)
	}
	// The source filter reads the raw rows and must agree with the rollup for the same rows.
	hn := f
	hn.Source = "hn"
	bySource, err := s.Series(ctx, hn)
	if err != nil {
		t.Fatal(err)
	}
	if got := bySource[len(bySource)-1].Outcomes; got["ok"] != 5 || got["internal_error"] != 1 {
		t.Fatalf("source series %+v", got)
	}

	adoption, err := s.Adoption(ctx, android, keys)
	if err != nil {
		t.Fatal(err)
	}
	if adoption.Total != 5 || len(adoption.Features) != 4 || adoption.Features[1].Key != "auth" ||
		adoption.Features[1].N != 5 || adoption.Features[1].Picked != 5 || adoption.Features[3].N != 0 {
		t.Fatalf("adoption %+v", adoption)
	}
	if len(adoption.Weekly) == 0 || adoption.Dimensions["languages"][0] != (Count{Key: "hi", N: 5}) {
		t.Fatalf("adoption dimensions %+v weekly %+v", adoption.Dimensions, adoption.Weekly)
	}

	failures, err := s.Failures(ctx, android, keys)
	if err != nil {
		t.Fatal(err)
	}
	if len(failures.Rejections) != 1 || failures.Rejections[0].Field != "package_name" {
		t.Fatalf("rejections %+v", failures.Rejections)
	}
	if failures.ByFeature[0].On != (Rate{N: 6, Failed: 1}) || failures.ByFeature[3].On != (Rate{}) {
		t.Fatalf("by feature %+v", failures.ByFeature)
	}
	if len(failures.Pairs) != 3 || failures.Pairs[0].N != 6 || failures.Pairs[0].Failed != 1 {
		t.Fatalf("pairs %+v", failures.Pairs)
	}
	if !slices.Contains(failures.ByPreset, GroupFailure{Key: "standard", Rate: Rate{N: 6, Failed: 1}}) ||
		!slices.Contains(failures.ByCommit, GroupFailure{Key: "99c4edf", Rate: Rate{N: 6, Failed: 1}}) ||
		len(failures.ByMinSDK) == 0 {
		t.Fatalf("groups %+v", failures)
	}
	standard := android
	standard.Preset = "standard"
	if overview, err := s.Overview(ctx, standard); err != nil || overview.Outcomes["internal_error"] != 1 {
		t.Fatalf("the preset filter hides failures: %+v %v", overview, err)
	}

	audience, err := s.Audience(ctx, f)
	if err != nil {
		t.Fatal(err)
	}
	cities := audience.Buckets["cities"]
	if audience.Sessions != 6 || len(cities) != 1 || cities[0].Key != "Mumbai" || cities[0].Sessions != 5 {
		t.Fatalf("audience %d sessions, cities %+v", audience.Sessions, cities)
	}
	if len(audience.Campaigns) != 1 || audience.Campaigns[0].Source != "hn" || audience.Campaigns[0].Generated != 1 {
		t.Fatalf("campaigns %+v", audience.Campaigns)
	}
	sources := map[string]Bucket{}
	for _, b := range audience.Buckets["sources"] {
		sources[b.Key] = b
	}
	if sources["reddit.com"].Sessions != 1 || sources["hn"].Sessions != 1 || sources["direct"].Sessions != 4 {
		t.Fatalf("sources %+v", audience.Buckets["sources"])
	}

	durations, err := s.Durations(ctx, f)
	if err != nil {
		t.Fatal(err)
	}
	if durations.Config.N != 6 || durations.ConfigBuckets[1].N != 6 || durations.ByFeatures[0].N != 6 || durations.Download.N != 5 {
		t.Fatalf("durations %+v", durations)
	}

	page, err := s.Generations(ctx, f, 0, 4)
	if err != nil {
		t.Fatal(err)
	}
	next, err := s.Generations(ctx, f, page[3].ID, 100)
	if err != nil {
		t.Fatal(err)
	}
	if len(page) != 4 || len(next) != 5 || next[0].ID >= page[3].ID {
		t.Fatalf("pages of %d and %d", len(page), len(next))
	}
	if g := next[len(next)-1]; g.AppName != "App 0" || !slices.Equal(g.IgnoredFields, []string{"theme"}) ||
		len(g.Notes) != 1 || len(g.Warnings) != 1 {
		t.Fatalf("what the generator said was not read back: %+v", g)
	}

	errs, err := s.Errors(ctx, f, false, 10)
	if err != nil || len(errs) != 1 || errs[0].Detail != "Traceback" {
		t.Fatalf("errors %+v %v", errs, err)
	}
	if err := s.ResolveError(ctx, errs[0].ID, true); err != nil {
		t.Fatal(err)
	}
	if err := s.ResolveError(ctx, 999999, true); err != ErrNotFound {
		t.Fatalf("resolving a missing error: %v", err)
	}

	health, err := s.RouteHealth(ctx, f)
	if err != nil || len(health) != 1 || health[0].MedianMs != 400 {
		t.Fatalf("health %+v %v", health, err)
	}

	// Every filter at once must still be valid SQL.
	all := Filter{From: f.From, To: f.To, Target: "android", Preset: "standard", Outcome: "ok", Country: "IN",
		DeviceClass: "mobile", Source: "hn"}
	if _, err := s.Overview(ctx, all); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Funnel(ctx, all); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Adoption(ctx, all, keys); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Failures(ctx, all, keys); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Audience(ctx, all); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Durations(ctx, all); err != nil {
		t.Fatal(err)
	}
	if rows, err := s.Generations(ctx, all, 0, 50); err != nil || len(rows) != 5 {
		t.Fatalf("filtered generations %d %v", len(rows), err)
	}
	all.Source = ""
	if _, err := s.Series(ctx, all); err != nil {
		t.Fatal(err)
	}
}

func TestIntegrationRejectionsAreGroupedByCauseNotByValue(t *testing.T) {
	s := connected(t, testDatabase(t))
	ctx := context.Background()
	rejected := func(field, reason string) Generation {
		return Generation{Target: "android", Outcome: telemetry.Invalid, VisitorHash: "h", FailureField: field, FailureReason: reason}
	}
	batch := &Batch{Generations: []Generation{
		rejected("colour", "Unknown field 'colour'."),
		rejected("theme", "Unknown field 'theme'."),
		rejected("design_style", "Unknown design style 'Neon'. Choose one of: Utility, Social."),
		rejected("design_style", "Unknown design style 'Retro'. Choose one of: Utility, Social."),
		rejected("package_name", "A package name is required, like com.example.myapp."),
		rejected("package_name", "A package name is required, like com.example.myapp."),
	}}
	if err := s.write(ctx, batch); err != nil {
		t.Fatal(err)
	}

	failures, err := s.Failures(ctx, today(), []string{})
	if err != nil {
		t.Fatal(err)
	}
	want := []Rejection{
		{Field: "package_name", Reason: "A package name is required, like com.example.myapp.", N: 2},
		{Field: "design_style", Reason: "Unknown design style '…'. Choose one of: Utility, Social.", N: 2},
		{Field: "", Reason: "Unknown field '…'.", N: 2},
	}
	if !slices.Equal(failures.Rejections, want) {
		t.Fatalf("rejections %+v", failures.Rejections)
	}
}

func TestIntegrationTheWriterRecordsInTheBackground(t *testing.T) {
	s := connected(t, testDatabase(t))
	w := NewWriter(s, 4, quiet)
	done := w.Submit(&Batch{Requests: []Request{{Route: "health", Method: "GET", Status: 200}}})
	select {
	case <-done:
	case <-time.After(10 * time.Second):
		t.Fatal("the batch was not written")
	}
	w.Close(context.Background())
	if stats := w.Stats(); stats.Written != 1 || stats.Failed != 0 {
		t.Fatalf("stats %+v", stats)
	}
	// After Close nothing is accepted, and nothing blocks.
	<-w.Submit(&Batch{})
	if w.Stats().Dropped != 1 {
		t.Fatal("a batch after Close was not counted as dropped")
	}
}

func TestIntegrationRetentionForgetsNamesAndOldRows(t *testing.T) {
	s := connected(t, testDatabase(t))
	seed(t, s)
	ctx := context.Background()
	// A spec the generator refused can hold anything where an object belongs.
	refused := &Batch{Generations: []Generation{{Target: "android", Outcome: telemetry.Invalid, VisitorHash: "h",
		AppName: "Trails", Spec: []byte(`{"app_name": "Trails", "ios": [1], "android": {"deeplink_host": "trails.example", "min_sdk": 26},
			"api_base_urls": {"prod": "https://api.trails.example"}, "features": ["network"]}`)}}}
	if err := s.write(ctx, refused); err != nil {
		t.Fatal(err)
	}
	_, err := s.pool.Exec(ctx, `
		UPDATE generations SET created_at = now() - interval '100 days' WHERE app_name = 'App 0';
		UPDATE generations SET created_at = now() - interval '100 days' WHERE outcome = 'invalid';
		UPDATE requests SET at = now() - interval '31 days';
		UPDATE events SET at = now() - interval '401 days' WHERE name = 'landed';
		UPDATE visits SET at = now() - interval '401 days' WHERE session_id = '`+sessionB+`';`)
	if err != nil {
		t.Fatal(err)
	}
	if err := s.prune(ctx, Retention{Requests: 30 * 24 * time.Hour, Events: 400 * 24 * time.Hour, Names: 90 * 24 * time.Hour}); err != nil {
		t.Fatal(err)
	}

	var appName, packageName, reason *string
	var spec []byte
	if err := s.pool.QueryRow(ctx, `SELECT app_name, package_name, spec FROM generations WHERE app_name IS NULL AND spec IS NOT NULL
		AND outcome = 'ok'`).
		Scan(&appName, &packageName, &spec); err != nil {
		t.Fatal(err)
	}
	if appName != nil || packageName != nil || string(spec) != `{"ios": {}}` {
		t.Fatalf("names kept: %v %v %s", appName, packageName, spec)
	}
	var notes, ignored, warnings []string
	if err := s.pool.QueryRow(ctx, `SELECT notes, ignored_fields, warnings FROM generations WHERE app_name IS NULL AND
		spec IS NOT NULL AND outcome = 'ok'`).Scan(&notes, &ignored, &warnings); err != nil {
		t.Fatal(err)
	}
	if len(notes) != 1 || len(ignored) != 1 || len(warnings) != 0 {
		t.Fatalf("notes %v, ignored fields %v, warnings %v", notes, ignored, warnings)
	}
	if err := s.pool.QueryRow(ctx, `SELECT spec FROM generations WHERE spec ? 'features' AND outcome = 'invalid'`).
		Scan(&spec); err != nil {
		t.Fatal(err)
	}
	if string(spec) != `{"ios": [1], "android": {"min_sdk": 26}, "features": ["network"]}` {
		t.Fatalf("a refused spec kept what names its project: %s", spec)
	}
	if err := s.pool.QueryRow(ctx, `SELECT failure_reason FROM generations WHERE outcome = 'invalid'`).Scan(&reason); err != nil {
		t.Fatal(err)
	}
	if reason != nil {
		t.Fatalf("an old rejection kept the input it quoted: %q", *reason)
	}

	var requests, landed, recent, visits, oldVisits int
	if err := s.pool.QueryRow(ctx, `SELECT (SELECT count(*) FROM requests), (SELECT count(*) FROM events WHERE name = 'landed'),
		(SELECT count(*) FROM generations WHERE app_name IS NOT NULL), (SELECT count(*) FROM visits),
		(SELECT count(*) FROM visits WHERE session_id = $1)`, sessionB).
		Scan(&requests, &landed, &recent, &visits, &oldVisits); err != nil {
		t.Fatal(err)
	}
	if requests != 0 || landed != 0 || recent != 5 {
		t.Fatalf("requests %d, old events %d, recent named generations %d", requests, landed, recent)
	}
	if visits != 5 || oldVisits != 0 {
		t.Fatalf("%d visits left, %d of them old", visits, oldVisits)
	}
}

func TestIntegrationFeedbackNotesCanBeCleared(t *testing.T) {
	s := connected(t, testDatabase(t))
	ctx := context.Background()
	// Longer than the body's byte budget, in a script where every character is three bytes: a byte
	// cut would leave invalid UTF-8 and Postgres would refuse the row, as it would a NUL.
	id, err := s.SaveFeedback(ctx, Feedback{Kind: "bug", Title: "Broken\x00", Body: strings.Repeat("नमस्ते ", 600),
		Features: []string{"network", "auth\x00"}, VisitorHash: "h", Target: "android"})
	if err != nil {
		t.Fatal(err)
	}
	note := "looking"
	if err := s.UpdateFeedback(ctx, id, "triaged", &note); err != nil {
		t.Fatal(err)
	}
	if err := s.UpdateFeedback(ctx, id, "", nil); err != nil {
		t.Fatal(err)
	}
	items, err := s.FeedbackList(ctx, today(), "triaged", 10)
	if err != nil || len(items) != 1 || items[0].Notes != "looking" || items[0].Target != "android" {
		t.Fatalf("after a status change: %+v %v", items, err)
	}
	empty := ""
	if err := s.UpdateFeedback(ctx, id, "", &empty); err != nil {
		t.Fatal(err)
	}
	if items, _ = s.FeedbackList(ctx, today(), "", 10); items[0].Notes != "" || items[0].Status != "triaged" {
		t.Fatalf("notes not cleared: %+v", items[0])
	}
	if err := s.UpdateFeedback(ctx, 999999, "fixed", nil); err != ErrNotFound {
		t.Fatalf("updating a missing report: %v", err)
	}
}
