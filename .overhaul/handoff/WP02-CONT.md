# WP02-CONT handoff (release pipeline stage 24, fast track)

Lane `WP02`: worktree `D:/projects/tools/.overhaul/wt/sw-WP02`, branch `staging-wp/WP02-CONT`, cut from `staging` at
`d429cbfbf` (Oct 23 2026, 23:3x IST). Logs and scratch projects of this stage: `D:/projects/tools/.overhaul/out/WP02-CONT/`.

## Progress

Run 5 (started 2026-10-24 16:37 IST): branch clean at `32b86b70`; staging still `4760b407` (`git merge staging`:
already up to date). Run 4's backgrounds finished: `v8.sh` step 6 green on lean, standard, all and everything (each
check read the whole report: designsystem 1264/1576 lines, navigation 231, ui 27-60), `none` exited 66 behind the
lock; `accept.cont.sh` passed every step (generator suite, consistency `--quick`, catalogue and tokens, seven fixtures
generated, lean/standard/standard-modular `check assembleDevDebug`, everything's with only formatting excepted and in
fact clean, none/modules-* builds and tests, the cross-feature split, both guards firing) except `template-build`,
which also exited 66 behind the lock, at the same second (09:05:18) as v8's `none`.

- [ ] `v9.sh` (16:5x-, `logs/v9.log`): presets regenerated at this head; the stability order on all five presets;
      devRelease sizes of lean, standard, everything at this head; the template's `build --continue`.
- [ ] `tools/size_budgets.toml` from v9's sizes; Measurements.

Run 4 (started 2026-10-24 08:28 IST): run 3 committed step 4 (`09613c5a`, 08:14) and stopped with `v6.sh` still running
(it finished its size step in this run) and 23.8 uncommitted. Merged staging `4760b407` (web only) as `89fc915d`.

- [x] `v6.sh` read: generator suite 484 OK; c-local built and tested; the planted screen failed on every kind; but
      the audit failed **real** screens (58 of 60 generated cases, 12 of the template's) and step 5 never ran a check
      (it asked for `:core:ui:compileDebugUnitTestKotlin`; no preset's `:core:ui` has tests).
- [x] Audit false positive 1, "the text is cut off" on short lines ("Order 1"): for plain text the semantics action
      (`ParagraphLayoutCache.slowCreateTextLayoutResultOrNull`) lays the text out again with the min constraints
      dropped, so `MultiParagraph.width` is the widest width offered (bytecode: `width = Constraints.maxWidth`) while
      `size` is the real box: `didOverflowWidth` is true for any line shorter than its space. Now: a line wider than
      the box, or the lines taller than it.
- [x] Audit false positive 2, the sample's skeleton rows "outside the window" and "under the tab bar": they sit under
      `clearAndSetSemantics` (read as one "Loading"), which also drops the lazy list's scroll range on that node, so
      the rows lost their scrolling exemption. Controls under a clearing node are no longer held to a control's rules
      (text inside still is).
- [x] `app/build.gradle.kts`: the WIP's `isIncludeAndroidResources = true` dropped; the convention's
      `onRobolectricTests` already sets it once `:core:testing:compose` is a test dependency.
- [x] `v8.sh` (08:38-, `logs/v8.log`): the audit on the regenerated project PASS (60 cases: the reference's skeleton
      and two scaffolded modules, `order` and a 30-letter name); planted FAIL on all 12 cases with every kind named
      (text cut, long lines from Large up, under the bar at Phone, the two 48 dp touch areas, the overlap, outside the
      window) and no real screen failing; the template's screens PASS (12 cases; app tests 35/35); `v7.sh` (via v8):
      the two "rows written after ..." cases FAIL under the mutated reducer and pass restored, the other five pass both.
- [x] Committed 23.8: `ce4f6c1c` (the audit, its wiring, the generator test) and `32b86b70` (AGENTS.md and
      ARCHITECTURE.md).
- [ ] v8 step 6, the stability order on five presets (queued behind other workflows' Gradle work at 09:04).
- [ ] `accept.cont.sh` started 09:0x in the background (`logs/accept.log`, `logs/accept/`).
- [ ] `tools/size_budgets.toml` once v6's everything size lands; Measurements.

Run 3 (started 2026-10-24 06:21 IST): run 2 stopped after `v3.sh` (02:45) with step 4 and 23.8 uncommitted. `v3`:
c-netdb and c-net green (app assembled, every module's tests and code rules at name lengths 3-30), generator suite 484
OK (9 skipped); c-local never built (the batch asked for `:data:*:testDebugUnitTest`, which a data module without
`src/test` does not have: the convention disables its unit tests). Staging moved to `7230b666` (5 commits: hairline
rects, the badge's per-draw fade, the large title's collapse from an effect, a new code rule, re-recorded profiles).

- [x] Merged staging (`2e447a6f`): one conflict, `app/src/main/baseline-prof.txt` (staging's re-recorded profile, its
      five `AndroidLogger` lines renamed to `com/base/app/log/`). The WIP stayed uncommitted across the merge.
- [x] The audit gives each case its own `LocalBottomChrome` holder instead of writing the shared default from a
      `SideEffect` (the shell's default holder is process-wide, so a case could inherit another's bar).
- [ ] `v6.sh`: generator suite; c-local (no-source shape) built and tested; the audit on a generated project (pass),
      then on a planted screen (fail naming each kind); the audit on the template's own screens (everything on);
      the stability order on five presets; devRelease sizes for `tools/size_budgets.toml`.
- [ ] Commit step 4 and 23.8; `size_budgets.toml`; `accept.cont.sh`; Measurements.

Run 2 (started 2026-10-24 01:14 IST): staging still at `d429cbfb` (nothing to merge). Run 1's work was uncommitted;
its batch `v1.sh` showed the logger and AuthFrame tests red on staging and green here, but its H-8 probe was void
(`MutableList<String>` counts as stable through `compose-stability.conf`'s `kotlin.collections.List<*>`, so neither
side could fail) and its step (a) never ran (`rc=66` behind the lock). Batch `v2.sh` redoes both.

- [x] `v2.sh` (01:23-01:33, `out/WP02-CONT/logs/v2.log`): build-logic tests green; H-8 with `class H8Box(var count: Int)`:
      staging's check PASSES after an incremental compile (62-line report, no `H8Planted`) and FAILS after a full one
      (control), the branch's check FAILS on `H8Planted(box)` after the same incremental sequence and passes once the
      probe is gone; generator suite 478 OK (9 skipped).
- [x] Committed run 1's work in five commits: `91367d3d` logger move (2a), `13121eac` scaffold audit (3 a-c),
      `8643c3d7` AuthFrame chrome padding, `9a3f0cca` H-8, `71893c48` test time limit (F-8).
- [~] Step 4 (uncommitted): data module in the reference's shape (`api/model/`, `remote/{Api,DtoMappers}`,
      `remote/dto/`, `Network<P>Repository`, `OfflineFirst<P>Repository` + `local/<P>EntityMappers.kt` + the DAO from
      `AppDatabase` with `database`, `Default<P>Repository` with no source without a network); the flow API
      (`items`, `refresh()`, `peek`, `item`); the list ViewModel observes `items` and moves Empty to Success when rows
      arrive (followups F-5, a regression test in the scaffolded `ListViewModelTest`); the database snippets in
      `SampleDao`'s shape (`position`, `ORDER BY position`, `deleteAllExcept`); tables only for modules that fetch
      (decision below); `database` on in `modules-*`. Generator tests for scaffold and target: 58 OK. ktlint harness
      `fit.sh` (every name length 1-30 × four data shapes, `fit/` a bare Spotless project with the template's
      `.editorconfig`): `logs/fit-f3.out` 0 files changed; the harness proven live by a planted joined header and
      peek that `spotlessApply` rewrote. The DAO queries quote the table name (a module named `order`).
- [~] REQUIREMENTS 23.8 (uncommitted): `app/src/test/.../adaptive/AdaptiveAuditTest.kt` (ComposablePreviewScanner
      over the app's package, every `@AppScreenPreviews` function at the six `WindowSize`s × font 1 and 2, a
      simulated tab bar at compact width; text cut off, outside the window, under the bar, 48 dp touch areas that run
      into another control, overlapping controls, lines past `readableMaxWidth` + gutter from Large up);
      `test_android_adaptive.py` asserts it ships in lean, standard and everything with something to audit (red on
      staging: the file is absent). Docs: ARCHITECTURE.md "Every form factor"; AGENTS.md and ARCHITECTURE.md
      describe the scaffolded data shape. Proof batches: `v3.sh` (scaffold compiled and tested at six name lengths in
      three data shapes), `v4.sh` (the audit green on a generated project with the sample and two modules, then red
      on a planted screen naming each kind of breakage); `v5.sh` (stability order on five presets, sizes);
      `accept.cont.sh` (the port of `accept.win.sh`).

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

## Decisions (taken here, with the reason)

- **Tables only for modules that fetch.** `database.py` used to give every scaffolded module a table, but a module
  without a network has a repository with no source, so its table was read by nothing and would only have been a
  schema to migrate. Now a module gets a table when it fetches and the database is on (`scaffold.stores`); a project
  with the database and no such module gets the starter `note` table, as before. A no-network module keeps
  `Default<P>Repository`, whose KDoc says where a source goes.
- **A module the spec names `home` fetches like any other.** The old test was `name != "home"`, which also cut the
  network from a user's own `home` module; only the synthesized module (not in `feature_modules`) has no source.
- **The scaffolded entity is `SampleEntity`'s shape** (`id`, `title`, `position`; `ORDER BY position`;
  `deleteAllExcept` then upsert) instead of WP06's `(id, title, body)`: the scaffold has no body, and without a
  position the list read back in whatever order SQLite chose.
- **The touch-target check is the one Compose applies.** Compose answers a touch up to 48 dp around a smaller
  control, so `touchBoundsInRoot` is 48 dp for almost everything (and is internal); the audit fails when that 48 dp
  area of a smaller control runs into another control, which is when the expansion stops working.

## Measurements

## Rules and gotchas the docs must state

- **`checkComposeStability` reads `build/compose_stability/`, not `compose_reports/`.** The compiler reports only the
  files it compiles, so after an incremental compile the report holds the changed files alone; `compileDebugKotlin`
  copies its report aside as it ends, and a build that runs the check (or `recordComposeStability`) compiles
  `:core:designsystem`, `:core:ui` and `:core:navigation` in full. The cost: `./gradlew check` and `build` recompile
  those three modules without incremental compilation (the build cache still serves an unchanged module), and
  switching between a build with the check and one without re-runs their compile once.
- **Unit tests fail after 15 minutes per module** (`TestTimeLimit.kt`). A stall shows as a failed task naming the
  module, not as a hung build.
- **The scaffold's data module is the reference's shape**: `api/` (interface, `model/`), `remote/` (`<P>Api`,
  `dto/`, mappers), `local/` (entity mappers, with the database); the implementation is named after its source
  (`Network…`, `OfflineFirst…`, `Default…` with no source). The list ViewModel observes `items`, so rows written
  after an empty answer or a failed first load replace that state (followups F-5, fixed in the scaffold; the
  reference's own `SampleListViewModel` still has it, WP02-M3).
- **The scaffold's list, as the compose-performance audit left it** (skill categories: lazy layouts, effects):
  a loading state of a few fixed placeholders is a `Column` (`wrapContentHeight(unbounded = true)` +
  `clipToBounds`, one TalkBack node), not a `LazyColumn` with scrolling off: the push's first frame does not build a
  lazy layout's machinery for six rows that never scroll. Every `items()` call carries a `contentType`, so a header
  or footer added later never reuses a row's composition. A one-off event on entering composition is
  `SideEffect(Unit) { … }` (keyed, Compose 1.12), not `LaunchedEffect(Unit)`: sending an event suspends nothing,
  so it needs no coroutine. Anti-pattern: `LazyColumn(userScrollEnabled = false)` for a skeleton.
- **A screen inside the shell pads its scrolling content with `chromeContentPadding()`, inside the scroll** (AuthFrame
  drew its form under SignUp's and PasswordReset's top bar until this stage): the padding's floor is the navigation
  bar, so no separate `navigationBarsPadding()` beside it; give the scroll a minimum height when the content is
  centred, so a form that fits does not scroll.
- **`add_feature.py` writes no table.** It reads only whether the project has `:core:network`, so in a project with
  the database it adds a `Network<P>Repository` kept in memory; an offline-first module needs its table in
  `:core:database` (entity, DAO, the three `AppDatabase` lists) and `OfflineFirst<P>Repository` by hand (WP14 owns
  the tool).
- **`AdaptiveAuditTest`**: put `@AppScreenPreviews` on a screen's stateless preview and the audit renders it at six
  windows and font scales 1 and 2 with every unit-test run; it simulates the floating tab bar (96 dp) only below
  600 dp wide, where the shell shows one. A failure lists each node by its text, content description or test tag.
  Fix the layout, never the audit.
- **Reading a text's layout from the semantics tree: never `hasVisualOverflow`.** For plain text (`BasicText` with a
  `String` and no `onTextLayout`) `GetTextLayoutResult` lays the text out again at the widest width it was offered
  (`slowCreateTextLayoutResultOrNull` drops the min constraints; `MultiParagraph.width` is `Constraints.maxWidth`)
  and pairs it with the real box, so `didOverflowWidth` is true for every line shorter than its space ("Order 1" in
  a card). A text is cut when a line is wider than `size.width` or the lines are taller than `size.height`; that is
  what `AdaptiveAuditTest` checks.
- **`clearAndSetSemantics` on a lazy list's own modifier wipes that list's semantics too** (the clearing modifier
  resets the node's collapsed config, dropping the scroll axis range the list adds inside it). A test that walks the
  unmerged tree still sees the cleared children (`useUnmergedTree = true` includes replaced semantics). The audit
  treats a clearing node as standing for its subtree: controls inside are not held to a control's rules, text is.
- **ktlint fit of the scaffold** is checked outside a project by `out/WP02-CONT/fit.sh`: Spotless alone resolves okio
  3.6.0, which the offline cache lacks (forced to 3.9.1), and without a `.gitattributes` it writes CRLF on Windows
  (`lineEndings = UNIX`). ktlint keeps more than one layout stable (a joined `peek` came back wrapped inside its
  lambda), so the check is "unchanged by `spotlessApply`", not "the only layout".
- **Git Bash heredocs**: a `\n` inside a Python heredoc reached the file as a real newline once in this run; edit
  source with backslashes through the editor, not a heredoc.

## Open issues

- **No accessibility-framework pass in the audit.** ADAPTIVE-PLAN §11.6 also asks for `enableAccessibilityChecks()`;
  it needs `ui-test-junit4-accessibility` and is unverified under Robolectric. The audit's own target, overlap and
  window checks cover the layout half; the ATF pass (contrast, labels) belongs with the TalkBack proof (coverage
  D-4), which has no owner yet.
- **The everything-glass size row** waits for WP05-BAR's glass build (no `Glass` design style on staging).
- **WP02-M3:** the scaffolded detail could seed its first frame from `peek(id)` as the reference will; the
  reference's `SampleListViewModel` Empty trap (F-5) is still open there.
- **For WP12:** `templates/android/AGENTS.md` still names `updateState`/`emitEffect` (the API is `setState`/`emit`)
  and points at a `docs/ADAPTIVE.md` that does not exist; this stage changed only the data-shape line.
