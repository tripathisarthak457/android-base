-- The admin portal's schema.
--
-- Four tables, one per question the portal answers: what got generated, what broke, who came, and
-- how fast it was. They are separate rather than one events table with a JSON payload, because
-- every query the portal runs is "count these by day" and a table with the right columns answers
-- that with an index instead of a sequential scan through a JSONB blob.
--
-- Nothing here stores an IP address. `visitor_hash` is a salted digest whose salt lives in the
-- environment; rotating it forgets who visited without losing the counts.

CREATE TABLE IF NOT EXISTS generations (
    id              BIGSERIAL PRIMARY KEY,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    visitor_hash    TEXT         NOT NULL,

    app_name        TEXT         NOT NULL,
    package_name    TEXT         NOT NULL,
    preset          TEXT,
    features        TEXT[]       NOT NULL DEFAULT '{}',
    feature_modules TEXT[]       NOT NULL DEFAULT '{}',
    min_sdk         INTEGER,
    target_sdk      INTEGER,
    motion_style    TEXT,
    font_name       TEXT,
    accent_colour   TEXT,

    succeeded       BOOLEAN      NOT NULL,
    zip_bytes       BIGINT,
    duration_ms     INTEGER      NOT NULL,
    -- Set when succeeded is false. The generator's own message, not a stack trace: a stack trace
    -- goes in the errors table, where it can be grouped.
    failure_reason  TEXT,

    country         TEXT,
    referrer        TEXT
);

CREATE INDEX IF NOT EXISTS generations_created_at_idx ON generations (created_at DESC);
CREATE INDEX IF NOT EXISTS generations_succeeded_idx  ON generations (succeeded, created_at DESC);
-- GIN so "how many projects included Room" is an index lookup rather than an unnest per row.
CREATE INDEX IF NOT EXISTS generations_features_idx   ON generations USING GIN (features);

-- Errors are grouped by fingerprint so the portal shows "this broke 40 times" rather than forty
-- rows of the same traceback. The fingerprint is computed in the API, not here, because it has to
-- strip the parts that vary — paths, line numbers in temp files — and SQL is the wrong place for
-- that.
CREATE TABLE IF NOT EXISTS errors (
    id            BIGSERIAL PRIMARY KEY,
    fingerprint   TEXT        NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    occurrences   INTEGER     NOT NULL DEFAULT 1,

    kind          TEXT        NOT NULL,   -- 'generation' | 'api' | 'client'
    message       TEXT        NOT NULL,
    detail        TEXT,
    path          TEXT,
    resolved      BOOLEAN     NOT NULL DEFAULT FALSE,

    UNIQUE (fingerprint)
);

CREATE INDEX IF NOT EXISTS errors_last_seen_idx ON errors (resolved, last_seen_at DESC);

-- One row per visitor per day per step. The primary key does the deduplication, so a visitor who
-- reloads the page thirty times is one row and the funnel does not lie.
CREATE TABLE IF NOT EXISTS visits (
    day          DATE   NOT NULL,
    visitor_hash TEXT   NOT NULL,
    step         TEXT   NOT NULL,   -- 'landed' | 'configured' | 'generated' | 'downloaded'
    at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (day, visitor_hash, step)
);

CREATE INDEX IF NOT EXISTS visits_day_step_idx ON visits (day DESC, step);

-- Request-level timing, kept for every request rather than sampled: the volume is a handful a
-- minute at most, and a percentile computed from a sample is a percentile nobody trusts.
CREATE TABLE IF NOT EXISTS requests (
    id          BIGSERIAL PRIMARY KEY,
    at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    route       TEXT        NOT NULL,
    method      TEXT        NOT NULL,
    status      INTEGER     NOT NULL,
    duration_ms INTEGER     NOT NULL,
    bytes_out   BIGINT
);

CREATE INDEX IF NOT EXISTS requests_at_idx    ON requests (at DESC);
CREATE INDEX IF NOT EXISTS requests_route_idx ON requests (route, at DESC);
