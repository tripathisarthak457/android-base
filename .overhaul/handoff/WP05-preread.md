# WP05 pre-read (package not started)

## Progress

Not started. Notes for the agent that starts WP05; copy them into `handoff/WP05.md` when it is created.

## Orchestrator note: motion-perf

Written 2026-10-09. Read `research/SMOOTHNESS-PLAN.md` (S6, S15), `research/MOTION-PLAN.md` (§1 tokens, M26) and
`research/predictive-back/SPEC.md` §6. MASTER-PLAN A4.9 rules 1–7 stand. All MISSING (package not started).

1. **Glass appears by ramping the lens or blur radius, never by layer alpha over a backdrop.** Morph = a rect and
   corner lerp from the source shape on `springs.sheet`.
2. **The lens** follows the finger on `springs.interactive` while dragged, and is released on `springs.thumb` with
   the velocity. Use `preferredFrameRate(FrameRateCategory.High)` while it moves.
3. **AGSL**: construct every `RuntimeShader` once, off the main thread at startup (SkSL compiles on the constructing
   thread), and draw the glass once in WP04's idle warm-up pass.
4. **Tab switch**: no glass animation. WP04's reveal veil is drawn in content, so the bar's backdrop only re-records.
5. **Predictive back**: the Gmail card changes the content every frame, so the bar's backdrop re-blurs every frame.
   Gate `pushpop_gesture` on `everything-glass` (≤ 3 % janky, GPU p90 ≤ 8 ms).
6. **Recomposition**: glass state read in draw only. A glass bar adds 0 executions to the RECOMPOSITION-PLAN tab-switch
   and scroll journeys.

## Orchestrator note: adaptive-standards

Written 2026-10-09. Read `research/ADAPTIVE-PLAN.md` (§4, §8.7, §16) and `research/STANDARDS-PLAN.md` (§2.4, §3.1).
All MISSING (package not started).

1. **AD-40: glass on wide windows.**
   - With a rail or sidebar, the glass stage records only the content area: the chrome column is not part of the
     backdrop.
   - During a live resize (the container size changed within the last 150 ms: a desktop drag, a fold), show the
     frosted tier, so the backdrop layer is not reallocated every frame [D; U: measure the cost first].
   - No glass stage state in singletons (two windows).
   - Gate: `tabs_large` (ADAPTIVE-PLAN §12.6) with glass on.
2. **The `kyant` rule** in `verifyCodeRules` (A11.3; STANDARDS-PLAN §3.1): `import com.kyant` fails in `feature/**`.
3. **Structure:** `foundation/glass/**` follows STANDARDS-PLAN §2.4 (one public type per file, under 500 lines, no
   layer packages, comments under `slop-comment`). No `experimental-api` opt-ins.
