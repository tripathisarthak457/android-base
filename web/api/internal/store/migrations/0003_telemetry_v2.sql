-- Telemetry v2: how each generation ended and where its time went, the browser
-- session it came from, and a funnel of anonymous events. Still no address, raw user agent or email.

ALTER TABLE generations
    -- Nulled after 90 days by the retention job, and absent for a request refused before its body
    -- was read.
    ALTER COLUMN app_name     DROP NOT NULL,
    ALTER COLUMN package_name DROP NOT NULL,

    ADD COLUMN target           text    NOT NULL DEFAULT 'android',
    ADD COLUMN outcome          text,
    ADD COLUMN session_id       uuid,
    ADD COLUMN config_ms        integer,
    ADD COLUMN client           text,
    ADD COLUMN device_class     text,
    ADD COLUMN cold_start       boolean,
    ADD COLUMN queue_ms         integer,
    ADD COLUMN python_ms        integer,
    ADD COLUMN stream_ms        integer,
    ADD COLUMN generator_commit text,
    ADD COLUMN failure_field    text,
    -- `preset` from here on is the preset nearest to the resolved features, and these are the
    -- differences from it, so "picked on purpose" can be told from "came with the preset".
    ADD COLUMN added            text[]  NOT NULL DEFAULT '{}',
    ADD COLUMN removed          text[]  NOT NULL DEFAULT '{}',
    ADD COLUMN architecture     text,
    ADD COLUMN design_style     text,
    ADD COLUMN languages        text[]  NOT NULL DEFAULT '{}',
    ADD COLUMN glass            boolean,
    -- The spec as sent, with keystore entries reduced to their names.
    ADD COLUMN spec             jsonb;

-- Rows from before this migration only recorded a boolean. The API's own failure messages tell its
-- failures apart from rejections by the generator's validation.
UPDATE generations SET outcome = CASE
    WHEN succeeded THEN 'ok'
    WHEN failure_reason LIKE 'the generator timed out%' THEN 'timeout'
    WHEN failure_reason LIKE 'the generator failed%'
      OR failure_reason LIKE 'could not read the generator%'
      OR failure_reason LIKE 'the generator reported success%' THEN 'internal_error'
    ELSE 'invalid'
END;

ALTER TABLE generations ALTER COLUMN outcome SET NOT NULL;

CREATE INDEX generations_target_created_idx ON generations (target, created_at DESC);
CREATE INDEX generations_session_idx ON generations (session_id) WHERE session_id IS NOT NULL;

-- Visits become one row per browser session, written by its `landed` event. The old key (one row
-- per visitor, day and step) would drop a second session from the same network on the same day.
ALTER TABLE visits DROP CONSTRAINT visits_pkey;

ALTER TABLE visits
    ADD COLUMN id                bigserial PRIMARY KEY,
    ADD COLUMN session_id        uuid,
    -- True when the configurator found a saved draft: the only "returning" signal, and it carries
    -- no identifier or date.
    ADD COLUMN returning_visitor boolean,
    ADD COLUMN referrer_origin   text,
    ADD COLUMN utm_source        text,
    ADD COLUMN utm_medium        text,
    ADD COLUMN utm_campaign      text,
    ADD COLUMN viewport          text,
    ADD COLUMN country           text,
    ADD COLUMN region            text,
    ADD COLUMN city              text,
    ADD COLUMN browser           text,
    ADD COLUMN os                text,
    ADD COLUMN device_class      text,
    ADD COLUMN client            text,
    -- What the admin's "source" filter means: the campaign when one was tagged, else the referring
    -- site, else direct.
    ADD COLUMN source text GENERATED ALWAYS AS (
        COALESCE(
            NULLIF(lower(utm_source), ''),
            substring(referrer_origin FROM '^[a-z]+://(?:www\.)?([^/:]+)'),
            'direct'
        )
    ) STORED;

CREATE UNIQUE INDEX visits_session_idx ON visits (session_id) WHERE session_id IS NOT NULL;
CREATE INDEX visits_at_idx ON visits (at DESC);

-- The funnel. Client events come from the site; `generated`, `downloaded` and
-- `download_interrupted` are written by the generate route.
CREATE TABLE events (
    id         bigserial   PRIMARY KEY,
    at         timestamptz NOT NULL DEFAULT now(),
    session_id uuid,
    target     text,
    name       text        NOT NULL,
    step       text,
    value      text,
    client     text,
    config_ms  integer
);

CREATE INDEX events_at_idx ON events (at DESC);
CREATE INDEX events_session_idx ON events (session_id) WHERE session_id IS NOT NULL;

ALTER TABLE feedback ADD COLUMN target text;
