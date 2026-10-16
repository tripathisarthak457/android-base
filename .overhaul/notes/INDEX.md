# Orchestrator notes for every work package

Every implementation, fix or continuation agent reads this file and every plan below that names its package. These
add to the WP file and the master plan. Where they conflict, REQUIREMENTS.md decides; a requirement added later wins
over the plan.

| File | What | Packages it names |
|---|---|---|
| `../REQUIREMENTS.md` items 17–25 | Requirements the user added during the work: the opaque block under the bar, visual polish, iOS-level smoothness, before/after numbers, iOS motion, recomposition, adaptive UI, Gmail-style predictive back, docs for agents | all |
| `../qa/r1/DEFECTS.md`, `../qa/r1/for-<WP>.md` | Visual QA round 1: defects found on screenshots, each with its owner | WP03, WP04, WP06, WP02-M2/M3, WP01 |
| `../research/ANDROID-SKILLS-DIGEST.md` + `~/.claude/plugins/marketplaces/android-skills/` | Google's official Android skills: the rules that change what we build (navigation-event child dispatchers, Navigation 3 recipes, adaptive scenes and previews, edge-to-edge insets, screenshot testing, AGP 9 flags, R8 analyzer, security), with owners and the vendoring plan | WP02-M2, WP02-M3, WP03, WP04, WP06, WP07, WP12, WP13 |
| `../research/SMOOTHNESS-PLAN.md` | iOS-level smoothness and minimum jank: techniques marked applied, partial or missing | WP02, WP03, WP04, WP05, WP06 |
| `../research/MOTION-PLAN.md` | Every iOS animation and transition, with parameters, implementation and owner | WP03, WP04, WP05, WP02-M3 |
| `../research/RECOMPOSITION-PLAN.md` | Recomposition checklist, CI gate on the Compose compiler metrics, and the recomposition-count harness with budgets | WP03, WP04, WP02-M2 |
| `../research/ADAPTIVE-PLAN.md` | Adaptive UI: size classes, foldables, ChromeOS and desktop windowing, input, enforcement | WP03, WP04, WP02-M2/M3, WP07, WP08, WP12 |
| `../research/predictive-back/SPEC.md`, `gmail/` | Gmail-style predictive back: the shrinking card, constants from AOSP and Material, and recordings from the user's phone | WP04 (WP07 parity) |
| `../research/SHEET-SPEC.md` (+ `../research/sheet/`) | REQUIREMENTS 33: the customisable, modern bottom sheet system. Its "Work split" section lists what each package delivers. Final (revision 2, Oct 13 16:30; 4108 lines). §19 "Work split" is part of your scope, and §22 lists rejected proposals that reviewers must flag | WP03, WP04, WP05, WP07, WP08, QA2, WP12 |
| `../docs-draft/` (SMOOTHNESS.md, MOTION.md, AGENTS-SECTION.md, ADAPTIVE.md) | REQUIREMENTS 25: draft guides from every finding up to Oct 12, each rule and number cited. WP12 verifies them against the merged code and ships them; numbers marked interim come from WP13 | WP12 |
| `../decisions/CP1.md`, `CP2.md` | Motion feel (CP1) and glass feel (CP2), decided on the user's behalf from recordings | WP03, WP04, WP05, WP07, WP11 |

Rules:
- An item in your scope is part of your acceptance. Do it, or record why you reject it in your handoff under "Deviations".
- Items outside your scope: leave them. The orchestrator routes them.
- Some files may not exist yet, because their sweep is still running. Re-check this index when you resume after a wait.

For WP12 (docs): read every file above, every `../handoff/*.md` ("Rules and gotchas the docs must state" and
"Measurements"), and `../perf/**/summary.md`. Requirement 25 lists what the docs of every generated project must
carry.
