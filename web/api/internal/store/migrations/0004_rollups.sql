-- Daily counts and timings of generations, upserted whenever a generation is recorded, so the
-- admin's daily series reads a few rows per day instead of every generation in the window.
-- Empty strings rather than NULL in the key columns: NULLs never conflict, so they would upsert
-- into duplicate rows.

CREATE TABLE daily_rollup (
    day          date    NOT NULL,
    target       text    NOT NULL,
    preset       text    NOT NULL DEFAULT '',
    outcome      text    NOT NULL,
    country      text    NOT NULL DEFAULT '',
    device_class text    NOT NULL DEFAULT '',
    n            integer NOT NULL,
    p50_ms       integer,
    p95_ms       integer,

    PRIMARY KEY (day, target, preset, outcome, country, device_class)
);

INSERT INTO daily_rollup (day, target, preset, outcome, country, device_class, n, p50_ms, p95_ms)
SELECT (created_at AT TIME ZONE 'UTC')::date, target, COALESCE(preset, ''), outcome,
       COALESCE(country, ''), COALESCE(device_class, ''), count(*),
       percentile_disc(0.5)  WITHIN GROUP (ORDER BY duration_ms),
       percentile_disc(0.95) WITHIN GROUP (ORDER BY duration_ms)
FROM generations
GROUP BY 1, 2, 3, 4, 5, 6;
