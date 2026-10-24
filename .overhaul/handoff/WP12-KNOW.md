# WP12-KNOW handoff (stage 36 of the release pipeline: the knowledge pass)

## Progress

Run 2 (impl, draft on the fast track), 2026-10-24 17:26 IST on.

- **Reconstructed:** run 1 stopped before committing anything (its last write 09:04). The worktree held its draft
  uncommitted: `test_docs.py` (`STALE_NAMES` removed, a new test that every `docs/X.md` a page names ships), the
  template `AGENTS.md` (the adaptive block, the tokens line, checklist item 2), `README.md` ("Where the app starts",
  the scaffolder lines), `docs/ARCHITECTURE.md` (module table, navigation, deep links, design system, form factors,
  build tasks, tests), `docs/PERFORMANCE.md` (the shell's own springs), and the new `docs/ADAPTIVE.md`. Staging is
  still `4760b4071`, already in the branch: `git merge staging` has nothing to bring.
- **Review of run 1's draft against the code** (claim by claim, done): kept, except these, corrected:
  - `StartupViewModel` "reads the settings and the session once": it follows both for the activity
    (`combine(settings, isAuthenticated)`, `stateIn(Eagerly)`), so the host swaps as they change (README,
    `ARCHITECTURE.md`).
  - `ADAPTIVE.md` chrome table: the rail in a short window does not hide its labels (`TabRail` keeps them
    unless `TabLabels.Never`; its items shrink toward 56 dp, then it scrolls). The lint list lacked
    `LocalContextResourcesRead`; `verifyManifestRules` also refuses the restricted-resizability property.
    The `ShortcutEffect` sample used `SearchEvent.Focus`, which does not exist: now the docs' made-up
    `OrdersEvent.Refresh`.
  - `PERFORMANCE.md`: `ShellConfig(screenResponse = …)` is there to compare candidates on a phone.
  - `:core:media` takes images and photos only (no video, no `CaptureVideo`): `ARCHITECTURE.md` and the
    `camerax` row of `AGENTS.md`; `CaptureVideo` left `LIBRARY_NAMES`.
- **Found and fixed besides:** `ARCHITECTURE.md`'s sample read `AppTheme.typography.titleLarge` (the scale is
  iOS's twelve roles: `headline` now), invisible to the name check because code blocks are skipped; new
  test `test_every_theme_value_the_docs_read_exists` reads every `AppTheme.x.y` on every page, code
  blocks included (red on the base, `logs/WP12-KNOW/r2-theme-read-red-on-base.log`). The template
  README said `:catalog` carries flavours (only `:app` does), that the catalog depends on the design
  system alone (also `:core:navigation`), and that `distProdRelease` writes per-ABI APKs (only with
  `-PabiSplits=true`).
- **New in this run:** `ARCHITECTURE.md` "Inside a module" (the package trees and the screen-package
  names of STANDARDS-PLAN §1-2.2, as the sample and the scaffolder have them), "A screen another feature
  opens" (the shared-key folder the module graph's message names), "Compact or modular" (the
  `architecture` field, `modes.py`, the modular dependency table from `ModuleGraph.kt`, `:domain:`).
  `GEMINI.md` (`@AGENTS.md`) in the template and the repository, with a test. The repository's
  `AGENTS.md` (feature TOMLs instead of `spec.py`'s `Feature(...)`, the marker grammar with `&` and
  `opt-else`, the catalogue `--write`, real verification commands, the other templates as coming soon,
  a pointer to `ADAPTIVE.md`) and `README.md` (rewritten: presets and feature tables from the catalogue,
  with a test that they stay equal; the guards; the wizard; release artifacts; layout; Python 3.11).
  The code-rules failure message now names `docs/ADAPTIVE.md` too (build-logic string; Gradle test run).

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
