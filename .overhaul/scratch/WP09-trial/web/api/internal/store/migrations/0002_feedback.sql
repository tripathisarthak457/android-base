-- Bug reports and feedback submitted from the site.
--
-- Separate from `errors`, which is what the software noticed about itself. This is what a person
-- noticed, and the two need different columns and different triage: an error has a fingerprint
-- and a count, a report has a description and a reply address.
--
-- The context columns are the point. A bug report that says "the zip is broken" is unactionable;
-- the same report with the exact feature set, the generated package name and the browser it was
-- submitted from is usually enough to reproduce it without asking a single follow-up question.

CREATE TABLE IF NOT EXISTS feedback (
    id           BIGSERIAL PRIMARY KEY,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    visitor_hash TEXT        NOT NULL,

    kind         TEXT        NOT NULL,   -- 'bug' | 'idea' | 'praise' | 'question'
    severity     TEXT,                   -- 'blocks' | 'annoying' | 'cosmetic'; bugs only
    area         TEXT,                   -- 'website' | 'generated-project' | 'cli' | 'docs'
    title        TEXT        NOT NULL,
    body         TEXT        NOT NULL,

    -- What they were doing. Filled in by the site from state it already has, so the reporter is
    -- not asked to retype what the page already knows.
    steps        TEXT,
    expected     TEXT,
    actual       TEXT,

    app_name     TEXT,
    package_name TEXT,
    features     TEXT[]      NOT NULL DEFAULT '{}',
    preset       TEXT,
    min_sdk      INTEGER,
    motion_style TEXT,
    font_name    TEXT,
    accent_colour TEXT,

    -- Optional, and the only thing here a person types about themselves.
    contact      TEXT,

    user_agent   TEXT,
    page_url     TEXT,
    app_version  TEXT,

    status       TEXT        NOT NULL DEFAULT 'new',   -- 'new' | 'triaged' | 'fixed' | 'wontfix'
    notes        TEXT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS feedback_status_idx  ON feedback (status, created_at DESC);
CREATE INDEX IF NOT EXISTS feedback_kind_idx    ON feedback (kind, created_at DESC);
CREATE INDEX IF NOT EXISTS feedback_created_idx ON feedback (created_at DESC);
