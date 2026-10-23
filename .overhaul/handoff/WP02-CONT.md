# WP02-CONT handoff (release pipeline stage 24, fast track)

Lane `WP02`: worktree `D:/projects/tools/.overhaul/wt/sw-WP02`, branch `staging-wp/WP02-CONT`, cut from `staging` at
`d429cbfbf` (Oct 23 2026, 23:3x IST). Logs and scratch projects of this stage: `D:/projects/tools/.overhaul/out/WP02-CONT/`.

## Progress

Run 1 (started 2026-10-23 23:29 IST).

- [x] Worktree and branch created from staging `d429cbfbf` (no earlier run; `release/STATE.md` and
      `staging/REQUIREMENTS-GAPS.md` do not exist yet, so no gap rows are routed here).
- [~] Step 2a: `AndroidLogger` moved to `app/.../log/AndroidLogger.kt` (`internal`, unchanged), `AndroidLoggerTest`,
      the baseline profile's five lines follow the class; ARCHITECTURE.md's stale `AppLogger.debugEnabled` line
      corrected. Uncommitted until batch `v1.sh` (red on staging, green here) passes.
- [~] Step 3: (a) `LoadingList` a `Column` (`wrapContentHeight(unbounded)` + `clipToBounds`, one TalkBack node
      "Loading"), (b) `contentType = { ROW }`, (c) keyed `SideEffect(Unit)` for `Appeared` in both routes; generator
      tests red on staging, green here. `AuthFrame`: the form drew under SignUp's and PasswordReset's top bar (content
      sits at y=0 under `AppTopBar`, nothing padded for it); now `.padding(chromeContentPadding())` inside the scroll,
      the inner navigation-bar padding dropped (the chrome padding's floor is the navigation bar), min height on the
      scroll so a centred form does not scroll; `AuthFrameTest`; baseline entry deleted. `r10-fit` still to run.
- [ ] Step 4: database scaffolding in WP06's reference shape.
- [x] Step 5: `sharedTransitions`: `git grep SharedTransitionLayout templates/android` finds only the vendored
      navigation-3 skill's recipes, no template code, so there is nothing for an option to switch (Deviation 10 stands).
- [ ] Step 6: `accept.win.sh`, sizes.
- [~] H-8: measured (`out/WP02-CONT/h8/`): an incremental compile of `:core:designsystem` after touching one file
      wrote a 62-line report against the full 1264, so the check passed on whatever the last compile touched.
      Fix: the check reads a copy of `compileDebugKotlin`'s report taken as that compile ends
      (`build/compose_stability/`), and a build that reads it compiles the module in full (an input marks it), so
      `d429cbfb`'s ordering is no longer needed. Proof in `v1.sh` (H8Planted).
- [~] F-8: `TestTimeLimit.kt` (15 min per test task, Android and JVM conventions), `TestTimeLimitTest`, ARCHITECTURE.md.
- [ ] REQUIREMENTS 23.8: `AdaptiveAuditTest` guard and its generator assertion.
- [ ] G-7: `tools/size_budgets.toml`.
- [ ] The stability check's order fix (`d429cbfb`) re-checked on every preset.

## Measurements

## Rules and gotchas the docs must state

## Open issues
