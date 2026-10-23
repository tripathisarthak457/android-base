# WP12-DOCS handoff (stage 4 of the release pipeline: the docs base)

## Progress

Run 1 (impl), started 2026-10-23 23:30 IST.

- Lane worktree `D:/projects/tools/.overhaul/wt/sw-WP12` created on new branch `staging-wp/WP12-DOCS` from staging
  `d429cbfbf` (no earlier run: no handoff, branch or worktree existed). `release/STATE.md` and
  `staging/REQUIREMENTS-GAPS.md` do not exist yet (INTAKE has not run), so no gap is routed here.
- Verified the drafts against staging `d429cbfbf` (names, rule ids, lint ids, gate rows, scenarios, springs, shell
  constants). Staging changed since the drafts' base `b21f0a981`: `PlayStorePrompts` now asks Play after the first
  frame (`63c74acd`), so the start-up text is updated; the phone verification (TEST-REPORT) contradicts two current-state
  claims (root list "0.15 % janky", gesture "6.9 ms/s"), replaced by a "Where the template stands" table.
- Found: the drafts cite `tools/perf`, the benchmark build, the overlay, dev tooling, glass, Coil and the catalog
  unfenced, but those are features (`performance` is off in every preset but Everything). The docs are fenced per feature.
- Test written first: `generator/tests/test_docs.py` (fails on staging: no guide; and a broken `#data-and-the-network`
  link in ARCHITECTURE.md's contents for projects without `network`).
- Next: write `templates/android/docs/PERFORMANCE.md`, AGENTS.md §6, the stale-line fixes, the generator AGENTS pointer.

## Measurements

None of its own: a docs stage takes no phone measurement.

## Rules and gotchas the docs must state

(filled as the run goes)
