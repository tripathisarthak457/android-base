# WP12-KNOW handoff (stage 36 of the release pipeline: the knowledge pass)

## Progress

Run 1 (impl, draft on the fast track), 2026-10-24 08:28 IST on.

- **Setup:** lane `wt/sw-WP12`, branch `staging-wp/WP12-KNOW`, cut from WP12-DOCS's ready commit `337f9bd4a`
  (the branch already pointed there; the worktree was clean). `git merge staging` at the start: already up to date
  (staging `4760b4071` is in the base).
- **Inputs read:** REQUIREMENTS 1-57; release-pipeline.md §1, §2.2, §3, §4.1; `handoff/WP12.md`, `design/wp/WP12.md`,
  `handoff/WP12-DOCS.md` (its routing notes for this stage: `STALE_NAMES`, AD-70), `docs-draft/` (ADAPTIVE,
  PENDING), `RELEASE-NOTES-android-draft.md`. `release/STATE.md` and `staging/REQUIREMENTS-GAPS.md` do not exist yet
  (INTAKE has not run), so no gap is routed here.
- **Plan for the draft** (docs only, no code; WP10-WEB's FAQ, `included.tsx` and telemetry page left alone; numbers
  that staging's fix still moves left for the final pass):
  1. `test_docs.STALE_NAMES` emptied: the template README's "Where the app starts" and the stale paragraphs of
     `docs/ARCHITECTURE.md` rewritten from the code (test first).
  2. AD-70: `docs/ADAPTIVE.md` in the template, `AGENTS.md`'s adaptive block, the checklist lines (test first).
  3. The repository's `README.md` and `AGENTS.md` checked against staging.
  4. The release notes draft brought from Oct 14 to staging.

## Measurements

None: a docs stage takes no phone measurement.

## Rules and gotchas the docs must state

(filled as the run goes)
