# NOUI handoff (REQUIREMENTS 59)

## Orchestrator note (Oct 25, 00:42), read before running the API or the generator

Never run the API or the generator with `PYTHON_BIN=python` (or any bare `python`) under a minimal environment on this machine: the Python install manager's alias then installs and registers a new Python in the working directory (incident 1 in ## WEB). Use the real interpreter `C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe`, and make sure `75887a654` (the child environment keeps LOCALAPPDATA) is in the tree you run. Do not touch `wt/NOUI-WEB/generator/Python` or its log: the user's registry points at it until they approve the repair. `go test` binaries are blocked by Smart App Control (incident 2): record that row as 'not runnable here, CI is the evidence', do not try to bypass it.

Lanes: GEN, WEB, DOCS (wt/NOUI-*), integration in wt/NOUI (staging-wp/NOUI). Plan: plans/2026-10-24/noui.md.

## GEN

GEN runs as one implementer in `wt/NOUI-GEN` (branch `staging-wp/NOUI-GEN`); the GEN-kt work is done in the same
worktree, so there is no `wt/NOUI-GEN-KT` branch to merge.

- **First commit (engine and option metadata): `83c2dd10f`** (23:08 IST). WEB and DOCS: `git merge 83c2dd10f`.
  It adds `templates/android/options/ui_components.toml` (metadata, no `owns` or `variant` yet), the option in the
  manifest, the spec field `ui_components` (absent = true, recorded only when false, non-bool = SpecError on the
  field), the dropped-feature notes, `"options"` in the dry-run plan, and the catalogue's `options` per target
  (contract 1.4, `locked: null`), with `web/frontend/lib/catalogue.json` rewritten by `--write`.
- Later commits: `b7af6520b` (CLI flag, wizard, `--list-features`, scaffold/tabs/add_feature No-UI), `0993ebaf3`
  (`tools/compare_generations.py`), `da27da32d` (the template: owns, fences, variants under `generator/variants/noui/`,
  No-UI static checks, `test_android_noui.py`), `388b054f7` (CI rows `noui-none`, `noui-everything`; catalogue and
  tokens `--check` in the generator job).
- **State at 01:15: P0 and P1 done, except `DesignSystemDocTest` (it reads DOCS's `docs/DESIGN-SYSTEM.md`, which is
  not on this branch; see "For the integrator" below). Head `d3c5b818c`.** Commits after `388b054f7`: `2d79e15b1`
  (No-UI shapes in the consistency matrix), `6e6524c76` (`add_feature --domain` reads the architecture from the
  record), `1a32c8801` (the code-rule baseline's screen entries and the theme's words fenced), `da02f9bff`
  (`SharedDestinationsTest` without a feature module), `15b6c72a9` (DOCS's two findings: `RealTemplatesTest` renders
  with `manifest.marker_keys` on and off, `compare_generations.py` writes through `write_text`), `a3b26df3c` (P1:
  `ComponentRules`, `ComponentRuleCasesTest`, `@AppComponentPreviews`), `01827c81c` (P1: the F1.4 advice-name test),
  `d3c5b818c` (P1: the No-UI startup profile recorded on the Redmi, split per module).
- Acceptance at `d3c5b818c` (logs in `logs/NOUI/GEN/`):
  1. `memwait python -I -m unittest discover -s tests -t .`: 553 tests OK, 9 skipped (`unittest-final-3.log`); the
     base is 477 OK, 9 skipped.
  2. `catalogue --check` and `tokens --check`: current.
  3. `consistency --quick`: 81 projects, 0 new issues, 188 known (`consistency-quick-3.log`).
  4. `compare_generations.py --before out/NOUI/base --after wt/NOUI-GEN`: 19 specs identical outside the exceptions
     (`compare-final-2.log`).
  5. The `create_project.py --preset everything --no-ui-components ...` command, wizard answered by Enter:
     `out/NOUI/gen-everything` (regenerated from the committed head: only the record's commit stamp differs).
  6. `check assembleDevDebug assembleProdRelease koverVerify --continue` there (`build-everything-5.log`): every task
     passes except `:spotlessKotlinCheck` (180 Kotlin files) and `:spotlessKotlinGradleCheck`
     (`benchmark/build.gradle.kts`), the WP02 formatting carry-over (`staging/CARRY-OVER.md`): the UI everything
     project at the base fails the same two tasks (`spotless-check-ui-base-2.log`: 359 Kotlin files, the same
     Gradle file). Not fixed here: the one reformat changes every UI project's bytes, which this lane must not. Both
     APKs built; the prod release carries `assets/dexopt/baseline.prof`. No-UI lean (`build-lean-4.log`) and none
     (`build-none-3.log`), `check assembleDevDebug assembleProdRelease`: BUILD SUCCESSFUL.

### Where reality differed from the plan, and why

- `--list-features --json` stays the list of features it always printed: scripts read it as a list (README, AGENTS.md,
  `test_cli`), and an object would break them. The options are in the catalogue (`catalogue --json`) and in the
  dry-run plan (`"options"`); the human `--list-features` prints the option, its default, what it leaves out and
  any lock note.
- The wizard asks the option right after the preset question, as a yes/no on its checkbox label ("No UI components?",
  default no), so "choose individually" never offers a disabled feature; the look questions skip the hidden fields.
- `core/ui/form` is not owned: `ValidationError.toUiText()` is the auth ViewModels' validation copy, not UI.
- `PostureTest` is kept with its sheet case fenced (it imports `AppSheet`); `AppLockGateTest` keeps its cover tests
  and fences the three that drive overlays or the Unlock button. `SheetRules.kt` and `SheetRuleCasesTest.kt` are owned
  (dead code once `CodeRules.ALL` drops them); `CodeRulesTest`'s sheet fixtures are fenced.
- The two sample routes are variant files rather than fences: their only No-UI difference is the content call plus
  two imports, which a fence could only carry with an empty `opt` block before the `opt-else`.
- The tooling kit (`core/devtools/.../kit/ToolingKit.kt`) has the 17 names the tools import, not 12:
  `AppText`, `AppMonoText`, `AppButton`, `ButtonVariant`, `ButtonSize`, `AppCard`, `AppDivider`, `AppSwitch`,
  `AppToggleRow`, `AppRow`, `AppStatusPill`, `AppStateView`, `AppSegmentedControl`, `AppScrollDefaults`,
  `Modifier.pressable`, `PressFeedback`, `LocalBottomChrome`. Press feedback is a draw-time scale, never a ripple.
- The app lock's No-UI cover is `PlaceholderScreen(R.string.applock_title)` that asks for the prompt again on a tap
  (with the Unlock label as its click label), because without the Unlock button a dismissed prompt would leave the
  person locked out; the system's failure message is its state description. The app-status placeholders have no
  actions until components exist (update and retry are what the user's screen calls).
- `core:applock` and `core:playstore` depend on `:core:ui` instead of `:core:designsystem` without UI components
  (PlaceholderScreen, LocalMessagePresenter; `core:ui` exposes the theme). `core:ui`'s No-UI test dependency on
  `:core:testing:compose` is a live `opt:!ui_components` line: in the raw template it only adds an unused test
  dependency to a module with no tests, and no generated UI project contains it.
- No-UI code never sits live in the raw template: every No-UI line is a `//~` fallback or a variant file, so the
  template's own `./gradlew build` is the UI project it always was.
- The consistency checks count a No-UI project's unused strings and plurals as intended (D14: each screen's copy stays,
  translated, for the screen built over its placeholder; release builds shrink what stays unused). Other resource
  kinds are still checked.
- `test_manifest.OrphanedTestTest` now runs for options too (a kept test whose main file the option owns fails).
- The acceptance command `create_project.py --preset everything --no-ui-components --set ... --out ... --no-zip` has no
  `--spec`, so it runs the wizard; it was run with every question answered by Enter (`yes '' | python create_project.py
  ...`), which keeps the defaults.
- For the integrator (not changed: it would change UI output): the template's `app/src/main/baseline-prof.txt` names
  its application and Hilt component classes `EverythingApplication`/`DaggerEverythingApplication_...`, the names of
  the project it was recorded from, so in a generated project those entries match nothing. The No-UI variant profile
  was the template's filtered to the classes a No-UI everything project has (3337 of 3520 entries), with the same
  quirk; since `d3c5b818c` it is recorded (below), still under the name Everything.
- The lean and none presets have no feature module without UI components (no home tab is scaffolded), so
  `SharedDestinationsTest`'s sanity check found no keys and failed `:app:testDevDebugUnitTest`; it now waits, as a
  skipped test, for the first feature module (`assumeTrue(File("../feature").isDirectory)`), and holds as before once
  one exists. It uses the inline `// <opt:!ui_components>CODE` form, since an `opt-else` needs an `opt` block before it.
- `stateless-params` leaves a feature's ViewModel parameter to the template's `vm-forwarding`, which already reports
  it (`CodeRulesTest` caught the double report); in the design system it reports any ViewModel parameter.
- `ComponentRules`' two pattern rules carry their regexes inline: an `object`'s `private val` declared after `ALL` is
  still null while `ALL` is built.
- The F1.4 test checks every `AppTheme` member, annotation and code-shaped name in the advice of the rules in each No-UI
  shape's `CodeRules.ALL` against the project's main sources, with a reviewed list of library names
  (`ADVICE_LIBRARY_NAMES`); `docs/...` paths are left to DOCS's `test_docs.py`.
- The No-UI startup profile (P9) was recorded from `out/NOUI/profile-everything-2` (No-UI everything generated as
  "Everything" / `com.base.app`, the names the test checks it under) with `locked adb` around `locked gradle
  ./gradlew :app:generateBaselineProfile` (`profile-record.log`: 25496 rules, 01:01), then split with the template's
  `tools/perf/split_profile.py` as today: app 1727 lines, core:navigation 143, core:designsystem 444, core:ui 17. The
  three module profiles were owned at P0 (absent in No-UI); they are variant files now, so `owns` lost those paths.
  The phone has no `com.base.app` package afterwards (the plugin uninstalls what it installed) and no setting was
  changed; it was woken before and sent HOME after. The app profile keeps the `EverythingApplication` quirk below.

### For the integrator (GEN)

- **P1 rule rows in DESIGN-SYSTEM.md.** `ComponentRules` landed, so DOCS's block (the five rows `designsystem-imports`,
  `stateless-params`, `raw-type`, `feature-primitive`, `component-preview` in `<!-- <opt:!ui_components> -->`, quoted
  under ## DOCS) goes into the rule index, and "Previews are render tests" (No-UI block) names `@AppComponentPreviews`.
  The ids match `ComponentRules.ALL` exactly; `test_docs.py` fails until the rows are in.
- **`DesignSystemDocTest` (P1, not done).** After DOCS is merged: a variant test
  `generator/variants/noui/build-logic/convention/src/test/kotlin/com/base/app/buildlogic/guard/DesignSystemDocTest.kt`
  that reads `../../docs/DESIGN-SYSTEM.md` (the build-logic test runs in `build-logic/convention`), takes the
  ```` ```kotlin component-template ```` block and asserts `CodeRules.check(<a core/designsystem/.../component/ path>,
  block)` is empty. The plan's second half (a "don't" block with exact rule ids) needs DOCS to add that block; there is
  none at `a76edea35`. On GEN's branch alone the doc is absent, so the test would fail every No-UI build there.
- **DOCS's NavDisplay recommendation is not taken.** The No-UI host still uses Navigation 3's default transitions
  (fades); the variant host and DOCS's sentence that the defaults fade must change together, so it is left as one
  decision for integration.

## DOCS

DOCS runs in `wt/NOUI-DOCS` (branch `staging-wp/NOUI-DOCS`). **State at 00:17: P0 and P1 done, head `a76edea35`.**
Acceptance: `python -m unittest tests.test_docs tests.test_vendored_skills -v` green on that head
(`logs/NOUI/DOCS/accept-2.log`, 39 tests, the one skip is the multiplatform-skills check that skips while that
template carries none); the full suite's only failures are GEN's (below). GEN was last merged at `388b054f7`; GEN's
later commits (from `2d79e15b1`) are not in DOCS: the integrator merges both lanes, and D1 runs test_docs on the
result. Merged `83c2dd10f` (GEN's plumbing) at 23:15 as
`7a5e0df0c` (its message is git's default, without the why and the trailer: a slip, left as is because history is
never rewritten).

Done (commits on `staging-wp/NOUI-DOCS`):

- `7af23196f` skills: android/skills **v1.0.17** (`f88c691a8a94`, checked with `git ls-remote` at 22:4x; v1.0.17 is the
  newest tag). 30 skills in every project (the official 33 minus `styles`,
  `migrate-xml-views-to-jetpack-compose`, `leanback-to-compose-tv-migration`); `auth.toml`/`media.toml` own no
  skill path now. LICENSE.txt read: Apache-2.0, no NOTICE file at v1.0.17. The clone went to `%TEMP%skills` and
  was sent to the Recycle Bin. Copies come from git objects (`ls-tree` + `cat-file`), not the working tree.
  New `templates/android/tools/android_skills.py` (P1 done): the project-local refresh,
  `python3 tools/android_skills.py --tag <tag>`; the repository's `tools/update_android_skills.py` now imports
  its copying, README, manifest and pointer code, so both write the same files.
- `cdab379b3` docs: `docs/ARCHITECTURE.md` (T1 fixes, No-UI blocks), `docs/PERFORMANCE.md` (new, condensed from
  WP12-KNOW's guide via `git show`, never merged), `docs/DESIGN-SYSTEM.md` (new), README.md.
- `10f24c9df` `generator/tests/test_docs.py`.
- CLAUDE.md (rules, done-checklist, skills table, refresh command, three `@docs/…` imports) and AGENTS.md
  (the same text, imports as links) in `7af23196f` and the No-UI blocks in the commit after `10f24c9df`.

Where reality differed from the plan, and why:

- **The multiplatform template gets no skills.** `templates/multiplatform/template.toml` has no `verbatim` rule, so
  the generator would rename and marker-parse upstream's text (`BaseApplication` in the testing skill, for one).
  The update tool now skips a template that does not copy `.agents/skills/**` verbatim and says so; a test holds
  "skills only where they are copied verbatim". Ship them there together with that rule when the multiplatform
  template is worked on.
- **PERFORMANCE.md uses the gate values at `8fdcba741`**, not WP12-KNOW's: REQUIREMENTS 58 moved the janky
  ceilings to 1.0 % (scroll and motion) and 6.0 % (navigation stress) after WP12-KNOW branched. WP12's "Where the
  template stands" table is not carried over: it goes stale with every measurement; the guide quotes limits and the
  measured numbers that explain each rule.
- **The doc test checks project-declared names per project, library names only at template level.** A No-UI project
  has no `LazyColumn` call, yet its PERFORMANCE.md must name Compose APIs; so per project the test fails on a name the
  template or a variant *declares* that the project lacks (its own API), and against the raw template every cited
  name must exist in the template, a variant, or a short reviewed allowlist.
- **README.md "The look"** is UI-only (the No-UI `AppTheme` takes no design or motion style); No-UI gets its own
  paragraph. The `readme-styles` and `readme-theme-call` regions sit inside the UI block.

For GEN (the names the No-UI docs cite; the doc test fails until each exists in the No-UI project, so either match
them or tell DOCS):

- `StartKey`, `StartScreen` (the blank start screen), `AppDestinations.start` names it.
- `PlaceholderScreen` (core:ui), `MessagePresenter` and `LocalMessagePresenter` (core:ui).
- `NavDisplayHostTest` (core:navigation tests), `AppNavigationHost` still the host's name, `OpenDeepLinks`,
  `PendingDeepLink`, `NavPane`, `Presentation` kept.
- `preview/ThemePreview.kt` (file name), `@AppComponentPreviews` (P1).
- `CodeRules.kt` failure message: "AGENTS.md and docs/PERFORMANCE.md say why" in both modes (the doc test's
  `test_every_doc_the_code_points_to_is_in_the_project` fails on today's `docs/MOTION-AND-PERFORMANCE.md` and
  `docs/ADAPTIVE.md` in every project).
- The rule index in DESIGN-SYSTEM.md must list exactly `CodeRules.ALL`'s rules per project (the test compares). No-UI:
  everything but `SheetRules`, plus the P1 `ComponentRules`.
- `generator/tests/test_markers.py` (`RealTemplatesTest`, GEN's file) renders every template file with
  `known = frozenset(manifest.features)`, so any `<opt:ui_components>` marker is "unknown feature" there: 12 errors
  on DOCS's branch today (CLAUDE.md, AGENTS.md, README.md, docs/*.md, both ways), and every Kotlin fence will hit it
  too. It needs `manifest.marker_keys`, and the option on and off. Run finished 23:53 on DOCS's branch
  (`logs/NOUI/DOCS/suite-1.log`): 539 tests, the only failures are those 12 and test_docs.py's checks waiting for
  GEN (names, CodeRules pointer, P1 rules).
- After merging GEN's head `388b054f7` (00:00) the DOCS branch runs 569 tests (`logs/NOUI/DOCS/suite-2.log`, 00:15):
  test_docs and test_vendored_skills green; the failures are GEN's only: `test_markers.RealTemplatesTest` 96 errors
  (the `known = frozenset(manifest.features)` above, now hit by every fenced Kotlin file too) and
  `test_host.test_text_is_written_only_through_write_text` (`tools/compare_generations.py:98` writes text without
  `genkit.core.copy.write_text`).

For the integrator:

- **P1 rows (changed at 00:05, after merging GEN's `da27da32d`).** The P1 rule rows are *not* in the docs now,
  because GEN's P1 has not landed and the doc test requires the rule index to equal `CodeRules.ALL` exactly. If
  `ComponentRules` lands, whoever lands it adds this block to the rule index in
  `templates/android/docs/DESIGN-SYSTEM.md`, right after the UI-only sheet rows (the doc test fails until they do):

  ```
  <!-- <opt:!ui_components> -->
  | `designsystem-imports` | a design system that cannot reach data | import only Compose and the theme |
  | `stateless-params` | stateless components | take values and callbacks, never a ViewModel, repository or `Flow` |
  | `raw-type` | one type scale | `AppTheme.typography.x`, `.copy()` for a one-off |
  | `feature-primitive` | features arrange, the design system draws | draw it once in `:core:designsystem` |
  | `component-preview` | every component rendered and tested | a `@Preview` on the component's file |
  <!-- </opt:!ui_components> -->
  ```

  and, if `AppComponentPreviews` lands, names it in "Previews are render tests" (No-UI block).
- **For GEN, a recommendation:** the No-UI `AppNavigationHost` passes no transitions, so `NavDisplay` uses Navigation 3's
  Android defaults, which fade whole screens (`fadeIn`/`fadeOut`, `scaleOut` on predictive pop; read from
  navigation3-ui 1.2.0's bytecode). That is what the template's own rule forbids (no cross-fade of full screens). The
  docs now say the defaults fade and must be replaced when a shell is built; a slide (or no motion) in the variant
  would make the project follow its own rule from the start. Not changed by DOCS (GEN's file).
- **Root `AGENTS.md`** (no lane owns it) still says a skill that ships with a feature is owned by that feature's
  TOML; every project now ships the whole set. Suggested replacement for that sentence: "Every project ships the
  whole set except the skills `LEFT_OUT` names in `templates/android/tools/android_skills.py`; a skill added to the
  set needs its row in that template's CLAUDE.md and AGENTS.md table." and the refresh command is
  `python3 tools/update_android_skills.py <checkout> --tag <tag>`.
## WEB

WEB runs in `wt/NOUI-WEB` (branch `staging-wp/NOUI-WEB`, from `8fdcba741`). Merged GEN's `83c2dd10f` at 23:3x as
`5536e7b6d` (git's default message, no why and no trailer: the same slip DOCS logged, left as is because history is
never rewritten). Head at 00:04 IST: `314b0fd15`. Nothing pushed.

Commits (all P0 and P1 of the lane):

- `77b0480c7` state: `Option`/`OptionLock`/`Target.options` types, `features/catalogue/options.ts` (one accessor
  per question: the option, the effective value, disabled features, hidden fields, setup-only, the Android-only note),
  `uiComponents` and `skipped` in the state, presets/toggles/drafts subtract the disabled features, `ui_components:
  false` sent only when ticked, the browser validator takes option keys as booleans. Tests: `options.test.ts`,
  `uiComponents.test.ts`, two in `request.test.ts`.
- `b5265c554` Go API: `options` parsed from the catalogue (a missing boolean default, a lock without a boolean value
  or a note, a key twice: boot fails), `TargetEntry.CheckOptions` (non-bool 400 `invalid_spec` on `ui_components`;
  lock mismatch 403 `{code: option_locked, field: ui_components, error: <note>}`), checked after the precheck and
  before `gen.Run` (no slot); selftest sends `LockedValues()` and runs the route's checks on its own spec first.
- `efcad37de` steps: the box first on Features (option's label/headline/description; locked = ticked, fixed,
  the note), disabled cards greyed with their reason (and "Coming with the Android release" when locked), preset
  counts without disabled features, Look/Motion show the `hides` reason where the style choice was (colours, font,
  haptics stay), Review "UI components" row, hidden rows dropped, "Skipped, and why"; Platform cards for multiplatform
  and iOS carry the Android-only note, which also stands in for the box on those targets.
- `bcf072658` the contract stand-in in `testing.ts` removed once the real catalogue had options; the locked catalogue
  stays a fixture (`lockedCatalogue()`), since only the release branch carries a lock.
- `e1727bf8d` integration test (runs only with `GENERATOR_DIR`): the ticked box on the largest preset passes the API
  checks, generates, drops `catalog` and notes it.
- `71dd17c23` `web/README.md`: the option, the lock (where, how it reaches site and API, the 403, fail-closed boot,
  selftest, how the Android release removes it), `ENABLE_TARGETS` and `TRUSTED_PROXY` rows; `Dockerfile.api` ->
  `Dockerfile`.
- `fe8e633b2` P1: theme-slots preview (colour swatches, type scale, blank start screen; light and dark) beside Look
  and Motion and in Review when ticked; landing badges in locked mode (design-styles showcase; the four "included"
  items that need components).
- `75887a654` the API's child environment passes `LOCALAPPDATA` on Windows (see incident 1).
- `314b0fd15` locked mode: the hero shows the lock's note under its promise (beyond the plan; the hero says "pick a
  look", which a setup-only site cannot honour).

Where reality differed from the plan, and why:

- **Design and motion style reset to the target's defaults when the box is ticked**, and stay there when it is
  unticked; the request always carries the effective (default) styles. A remembered style would need every reader
  (request, preview, Review, glass note, features' "needed by the X look") to pick the effective value; resetting
  keeps `state` the truth. Unticking shows the style choices again.
- **`skipped` lives in the state** (features the selection or the chosen preset had, in the option's order), because
  after subtraction the selection cannot say what it lost; it is recomputed on tick, preset, platform switch and draft
  restore, and persisted with the draft (draft version unchanged; older drafts read as UI on).
- **Disabled cards stay focusable** (`aria-disabled` plus `aria-describedby` on the reason) instead of the native
  `disabled`, so a keyboard or screen-reader visitor meets the reason. `ChoiceCard` gained an `unavailable` state for
  it; per its existing rule the text is not faded (dashed edge, no fill).
- **A locked refusal is recorded as outcome `invalid`** with the note as reason and `ui_components` as field, and the
  spec described (features, preset): a new outcome would need a migration and admin changes for no gain tonight.
- **`null` counts as absent** in the API check, as the generator strips nulls; so a locked deployment refuses
  `"ui_components": null` with the 403 too.
- **Lock route tests run on every platform**: the platform-neutral helpers (`testCatalogue`, `fakeSink`, `newServer`,
  `decodeError`, ...) moved from the unix-only test files into `httpapi/helpers_test.go`; `httpapi/options_test.go`
  uses a generator whose interpreter does not exist, so only a refusal before the slot can pass. The "accepted" cases
  (locked + false, unlocked: absent/true/false) need `/bin/sh` and are in the unix-only `generate_test.go` (CI).
- The site's `stepOf("ui_components")` is the Features step, so the API's 403 links there.

Checks (logs under `logs/NOUI/WEB/`):

| Check | Result | Log |
|---|---|---|
| `memwait npm ci` | ok | `accept-npm-ci.log` |
| `npm run typecheck && npm test` | ok, 246 tests, 0 fail | `accept-typecheck-test.log` |
| `memwait npx next build` | ok | `accept-next-build.log` |
| `gofmt -l .` empty, `go vet ./...` (also `GOOS=linux go vet ./...` for the unix-only tests), `go build ./...` | ok | `accept-api.log` |
| `go test ./...` at the head | **blocked**: Smart App Control refuses every freshly linked test binary since ~23:55 (incident 2); clip, config, store, telemetry ok | `accept-api.log` |
| `go test ./...` at `b5265c554` (lock, options, selftest, route tests) | ok, every package | `api-1.log` |
| integration with the real generator (`GENERATOR_DIR`, real interpreter path) | android, resolution, **no UI components**, v1 ok; multiplatform fails on Windows MAX_PATH in the temp dir (pre-existing, not this lane) | `api-integration.log` |
| site in the browser (`next start`, Browser pane) | box, greyed cards with reasons, `aria-disabled` + reason ids, Standard 17 still the preset, Look/Motion notes, theme slots light/dark, Review rows and "Skipped, and why", Platform notes; draft cleared afterwards, server stopped | `next-start.log` |

Not run or not verified here: the unix-only route tests (CI runs them); `TestChildEnvPassesOnlyWhatPythonNeeds`
(written, blocked by incident 2; its Windows behaviour was checked by hand: with `LOCALAPPDATA` the alias resolves
`%LOCALAPPDATA%\Python\pythoncore-3.15-64\python.exe` and writes nothing); the locked site in a browser (the checked-in
catalogue is never hand-edited; the locked behaviour is covered by `lockedCatalogue()` tests).

**Incident 1 (system change, needs the user): a stray Python registered for the user account.** At 23:27 the
integration run was started with `PYTHON_BIN=python`. The API runs the generator with a minimal environment (no
`LOCALAPPDATA`); the Python install manager's `python` alias then downloaded Python 3.15.1 from python.org
(signature and hash verified by the manager) into its working directory and registered it:
- files: `wt/NOUI-WEB/generator/Python/` (181 MB, untracked) and `wt/NOUI-WEB/generator/python_install_20261024232739_10016.log`;
- registry, checked from a Terminal tab (real, not the app's virtualised view):
  `HKCU\Software\Python\PythonCore\3.15` (`InstallPath` and its values) and the Apps-list entry
  `HKCU\...\Uninstall\pymanager-pythoncore-3.15-64` now point at that folder instead of
  `%LOCALAPPDATA%\Python\pythoncore-3.15-64`;
- Start menu: `%APPDATA%\Microsoft\Windows\Start Menu\Programs\Python\Python 3.15\*.lnk` point at it;
- unchanged: the real install (3.15.0 in `%LOCALAPPDATA%\Python`, runs), its `bin` aliases, user and system PATH.
Cause fixed in `75887a654` (the child environment keeps `LOCALAPPDATA` on Windows). Repair, for the user to approve
(not done: registry and Start menu are the user's): in a Terminal tab, `py install --refresh` (re-registers the
installs the manager owns, which rewrites the PEP 514 key, the Apps entry and the shortcuts to the real install),
check with `reg query "HKCU\Software\Python\PythonCore\3.15\InstallPath"`, then send `generator\Python` and the log
file to the Recycle Bin. Undo: none needed; the stray copy is not used by `python`/`py`.
**Gate D16 on this machine**: run the API smoke only with `75887a654` in, or with `PYTHON_BIN` set to the real
interpreter (`C:\Users\crazyhusir\AppData\Local\Python\pythoncore-3.15-64\python.exe`); with the alias and an older
API it repeats incident 1.

**Incident 2 (environment): Smart App Control is On and blocks new unsigned executables.** Code Integrity event 3077
for every `go-build...\*.test.exe` and a hello-world from 23:55; earlier the same evening fresh test binaries ran.
Not changed (a security setting is the user's). Gate D13 on Windows needs the user to allow it, or rely on CI's API
job for `go test`.

For the integrator:

- Lane order GEN, DOCS, WEB: WEB's only generator-side file is `lib/catalogue.json` from the merge of `83c2dd10f`;
  take GEN's (re-run `catalogue --write` if stale). WEB adds no hand edit to it.
- The user's relayed request also asked for a manual Vercel deploy when done. Not done from this lane: lane commits
  are not integrated or gated, and plan section 7 puts the deploy after the gate and the user's go; the Vercel plugin
  also needs `/mcp` authorisation.

## Pause (Oct 25, 01:21), usage 94 % of the window that resets 02:40

Stopped at a clean point: the three lanes had returned (GEN done d3c5b818c, WEB partial 314b0fd15, DOCS done a76edea35; their results are saved with the plan in plans/2026-10-24/noui-resume-args.json under 'built'), and the integrator had just started: staging-wp/NOUI at 39f71cca7, clean tree. NOUI-WEB's two untracked entries are the stray Python folder and its log from incident 1 (leave them). The relaunch at 02:43 skips the architecture reviews, the plan and the lanes, and starts at the integration, which continues from 39f71cca7.

## Integration

Integrator in `wt/NOUI` (`staging-wp/NOUI`). **State at 03:18: done; head `4b169b8d2`.** Nothing pushed; `main`,
branches, worktrees and history untouched; no stash.

Merges (made by the first integration run before the 01:21 pause, checked by this one): `cb9560dfa` GEN (`d3c5b818c`),
`0a51c82a5` DOCS (`a76edea35`), `7c1129a1c` WEB (`314b0fd15`), each `--no-ff` with a why-message and the trailer, in
the plan's order. None had a conflict (no hand-resolved hunk in `git diff-tree --cc`), so nothing was resolved by
taking both sides. Emitted files: `39f71cca7` ran `catalogue --write` after the merges; content was already current
and only `generator_commit` moved (to `7c1129a1c645`). Nothing this run changed is emitted, so `--check` stays
current at the head and the stamp was not moved again (`--check` ignores it).

Cross-lane items finished (commits on `staging-wp/NOUI`):

- `77fb9371e` The five ComponentRules rows (`designsystem-imports`, `stateless-params`, `raw-type`,
  `feature-primitive`, `component-preview`) in DESIGN-SYSTEM.md's No-UI rule index; `component-preview`'s fix names
  `@AppComponentPreviews` (the rule's own advice) rather than DOCS's draft "a `@Preview`". The No-UI component template
  carries `@AppComponentPreviews` (UI keeps its four `@Preview`s), and "Previews are render tests" names it.
  `test_docs.active_rule_ids` read `CodeRules.ALL` from one line; GEN's No-UI sum wraps over three, so every No-UI
  case errored (`logs/NOUI/INTEG/test-docs-1.log`). It now follows lines ending in `=` or `+`.
- `9a804b1cf` `DesignSystemDocTest` (P1, F3), a No-UI variant test in build-logic: the component template and the
  test template break no rule, and a new No-UI "what the build refuses" block (`kotlin component-dont`, an `AppBadge`
  whose lines carry `// state-param`, `// stateless-params`, `// animated-delegate, raw-tween`, `// raw-type`,
  `// raw-dp`, `// raw-color`) yields exactly those findings at exactly those lines. That block is the "don't" block
  the plan's 4.2 asked for and DOCS had not written. The test reads the project's page from outside build-logic, so
  the No-UI `build-logic/convention/build.gradle.kts` declares the page as an input of `tasks.test` (a
  `<opt:!ui_components>` block: live but harmless in the raw template like GEN's `core:ui` line, absent from every UI
  output; D5 below proves it).
- `dbadd6ab2` + `4b169b8d2` **Decision (logged in `decisions/LOG.md`):** the No-UI `AppNavigationHost` passes no
  motion (`EnterTransition.None togetherWith ExitTransition.None`) for push, pop and predictive pop instead of
  Navigation 3's defaults (fade whole screens, scale one under predictive back), which broke the template's own rule
  in every No-UI project. Options were no motion, a slide, or the defaults with the docs' warning; no motion because
  (c) takes the shell's visuals out and No-UI `:core:navigation` has no design system (no springs or reduce-motion to
  slide with); the docs already say to add a slide with the shell. ARCHITECTURE.md and PERFORMANCE.md changed with
  it. New `NavDisplayHostTest` case: six paused frames (0.1 s) after a push only the new screen is composed. The first
  form (one 0.1 s advance) failed in the build because the paused clock never let the looper hand the change to
  Compose (`build-everything-1.log`); fixed in `4b169b8d2`. Checked both ways in the generated project: green with
  the host (`navhost-test-2.log`), red with the defaults put back (`navhost-test-3-defaults.log`, the start screen
  still drawn), then restored.
- `9c25389d9` Root `AGENTS.md`: DOCS's replacement for the feature-owned-skill sentence, `--tag` on the refresh
  command, and the Compose Multiplatform template described as carrying no skills (it never did; its
  `template.toml` has no verbatim rule).

Checks (logs under `logs/NOUI/INTEG/`):

| Check | Result | Log |
|---|---|---|
| Docs, skills and markers tests after the edits | 80 tests OK, 1 skip | `test-docs-2.log` |
| D1 generator suite, `memwait python -I -m unittest discover -s tests -t .` | 576 OK, 9 skipped, at `9c25389d9` and again at `4b169b8d2` | `unittest-1.log`, `unittest-2.log` |
| D2 `catalogue --check`, `tokens --check` | current, at both heads | `catalogue-tokens-check.log`, `-2.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI` | 19 specs identical outside 1.6's globs; the `CodeRules.kt` hunk is the message only | `compare-1.log` |
| No-UI everything (`--preset everything --no-ui-components`, wizard by Enter, `out/NOUI/integ-everything`), `locked gradle ./gradlew check assembleDevDebug --continue` at `4b169b8d2` | every task green except `:spotlessKotlinCheck` (178 files) and `:spotlessKotlinGradleCheck` (`benchmark/build.gradle.kts`): the WP02 carry-over, exactly GEN's result. `DesignSystemDocTest` 3/3 (in `build-everything-1.log`'s run; cache hit at head), `NavDisplayHostTest` green, 31 `verifyCodeRules`, lint, Kover, `assembleDevDebug` | `build-everything-2.log` (first run at `9c25389d9`: `build-everything-1.log`) |
| The four files this run added or changed, in the generated project, through Spotless's IDE hook (nothing written) | all `IS CLEAN` | `spotless-ide-hook.log` |
| D12 website: `memwait npm ci`, `npm run typecheck`, `npm test`, `memwait npx next build` | ok; 246 tests, 0 fail | `npm-ci.log`, `web-typecheck-test.log`, `next-build.log` |

Not run here: D13 `go test` (Smart App Control, WEB's incident 2), D3/D4 consistency, D7-D11, D15-D17 and the phone
rows: they are the gate's.

Open, for the orchestrator and the gate:

- **`next` 15.5.27 has a critical advisory** (`npm audit`: GHSA-2464-fhr4-w9xw, remote code execution in the Image
  Optimization API with SVG; GHSA-p3m5-59mp-jv5r, a Server Actions DoS); fixed in 15.5.28, while `package.json` pins
  exactly `15.5.27`. Present since before this work (`package.json` unchanged since `8fdcba741`; WEB's `npm ci` log shows it
  too). The site uses neither `next/image` nor server actions, but `/_next/image` is served by default. For D15 and the
  deploy: a one-line bump of `next` (and the lock file) is WEB's to make and gate; not done here.
- Spotless carry-over (both modes): the planned one-time `spotlessApply` after the last UI change.
- `EverythingApplication`/`DaggerEverythingApplication_*` names in the app profiles (GEN's note): unchanged.
- WEB's incident 1 (the stray Python registration) and incident 2 (Smart App Control) still need the user.
- Flags for the morning report (plan 1.6, risk 9): README.md and the CodeRules failure message change in UI projects.
- The Vercel deploy waits for the gate and the user's go; the Vercel plugin still needs `/mcp` authorisation.
- Housekeeping: `%TEMP%\nav3ui` (navigation3-ui's classes, extracted to read `NavDisplay`'s signature) was sent to the
  Recycle Bin; one scratch formatter output in `%TEMP%` was deleted with `rm`. Handoff backup before this section:
  `C:/Users/CRAZYH~1/AppData/Local/Temp/claude/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/scratchpad/NOUI.md.before-integration`.

## Fix round 1

Fixer in `wt/NOUI` (`staging-wp/NOUI`), from `4b169b8d2`, for the 13 blocking findings of the generator, template
and web/API reviews (two of them the same private-name finding). **Head `b7eeb6c9a`.** One commit per cause, each
with a test where the cause is code. Nothing pushed; `main`, other branches, worktrees and history untouched; no
stash; `wt/STAGING` not touched; the phone not used (no finding needed it). Logs under `logs/NOUI/FIX1/`.

| # | Finding | Commit | What changed, and the proof |
|---|---|---|---|
| 1 | Private names in `test_docs.py` (`WORKSHOP_WORDS`), reported twice | `e00ea8a3d` | The three names are gone from the repository's tree (`git grep -i` over HEAD finds none); the test keeps the workshop words. Private names are the workspace pre-push scan's job (see Open). `test_docs` green (`test-docs-1.log`). |
| 2 | `tools/android_skills.py` refresh deleted a project's own skills | `268ca81ed`, `36a924385` (drops the constant the old manifest read) | The tool's `MANIFEST.sha256` is the record of what it vendored: only those skills are replaced or removed, the manifest hashes only them, and a skill that would land on a folder or pointer it did not write is refused before anything changes (`SkillsError`). New `ToolTest` cases: a user's `.claude/skills/my-own-skill` and `.agents/skills/my-team-skill` survive a refresh and an upstream drop, byte for byte, and are not called dropped; `--add` over a user folder exits 1 and changes nothing. Both new cases fail on the old tool and pass on the new one (run with the old file put back and then restored). The template's README, pointers and manifest are unchanged (the template-copy tests compare them). |
| 3 | No-UI app profile named `EverythingApplication` and its Hilt/Dagger classes | `39215bd49` | The variant profile names them in the identity form (`BaseAppApplication`, `Hilt_BaseAppApplication`, `DaggerBaseAppApplication_…`, `injectBaseAppApplication`; 222 occurrences), and `template.toml` gains three identity tokens (`Hilt_BaseApp`, `DaggerBaseApp`, `injectBaseApp` → the `{pascal}` forms, prefix boundary), data rather than engine special cases; no UI file contains them (git grep), and D5 below is identical. `test_rename` covers the three forms and their boundaries; the profile test now generates as "Acme NoUi" / `com.acme.noui` and asserts the profile names `AcmeNoUiApplication`, `Hilt_AcmeNoUiApplication` and `DaggerAcmeNoUiApplication_HiltComponents_SingletonC`. With the old profile put back it fails (DaggerEverything… lines unmatched, the three classes absent). The UI template's own profile has the same quirk; not changed (UI output), see Open. |
| 4 | `DevToolsOverlayTest` press imports inside `<opt:flags>` | `6dbd13dd6` | The UI/No-UI import pair sits above `// <opt:flags>`; the generator sorts imports, so UI bytes equal the base again. `compare_generations.py`'s default set gains `only-<key>` and `allbut-<key>` for every selectable feature (105 specs). D5 (`compare-1.log` before the fix commit was made, `compare-2.log` at the head): identical outside 1.6's globs, `only-devtools`, `only-perfhud`, `allbut-flags` included. Gradle: a UI project with `features: ["devtools"]`, `locked gradle ./gradlew :core:devtools:testDebugUnitTest`: BUILD SUCCESSFUL, `DevToolsOverlayTest` 3/3 (`build-ui-devtools-only-test.log`; the review's log of the same spec at the old head fails with `Unresolved reference 'pressable'`). No-UI lean + devtools, no flags: the same test 3/3 (row 5's build). |
| 5 | add then remove a feature left No-UI lean/none red | `51031084e` | `remove_feature.py` deletes a tier (`feature/`, `data/`) it leaves empty; `SharedDestinationsTest`'s No-UI guard needs a module folder (`File("../feature").listFiles(File::isDirectory).orEmpty().isNotEmpty()`), not the tier; the test helper `snapshot` records folders too, so `test_remove_undoes_add` compares the folder set (it fails with the old `remove_feature.py`, passes now). Gradle: No-UI lean + devtools generated, `add_feature orders`, `remove_feature orders --yes` (both tiers gone afterwards), then an empty `feature/` made by hand to recreate the review's state, `locked gradle ./gradlew check assembleDevDebug --continue`: BUILD SUCCESSFUL in 3m 51s, `SharedDestinationsTest` 2 tests, the scan check skipped (`build-noui-lean-removed.log`). |
| 6 | The consistency matrix read the skills' 45 Kotlin samples | `f169cf3a1`, `b7eeb6c9a` | First `Project` skipped the manifest's `verbatim` paths; the quick run then still failed every project with 88 `RES` issues from the glance skill's sample widget XML (`consistency-quick-1.log`: 81 projects, 7128 new issues, all under `.agents/skills`), because the resource, manifest, settings, translation, empty-file and leftovers checks each walked the tree themselves. The root fix is one walker, `project_files(root, pattern)` in `project.py`, which every check and `Project` use. `VendoredFilesTest`: vendored Kotlin and Gradle are not read, and `_resources`, `_translations` and `_hollow` report nothing under `.agents/skills` (fails on `f169cf3a1`'s walkers, passes now). D3 `consistency-quick-2.log`: 81 projects, 0 new issues, 188 known. |
| 7 | Setup-only site still promised slides and predictive back | `888951c47` | The landing items moved to `features/landing/included.ts` (`includedItems(catalogue)`), so they are testable with `node --test`; on a setup-only catalogue "Smooth by rule" drops "screens slide rather than fade" and "Navigation nobody has to coordinate on" says deep links open on the back stack, which survives process death, instead of predictive back. The unlocked copy is the same text as before. `included.test.ts`: in locked mode no unbadged item promises slides, predictive back, a tab bar or a floating bar; unlocked keeps both promises; both list the same items. |
| 8 | FAQ and landing said AGENTS.md with CLAUDE.md pointing at it | `52c7720e2` | Both now say CLAUDE.md and AGENTS.md hold the same rules, with the three detailed guides CLAUDE.md imports and Google's Android skills pinned to a release (`.agents/skills`). |
| 9 | No-UI docs promised render tests features do not run | `dea19fad0` | Option 2 of the finding, keeping T4 (a placeholder feature would pay Robolectric resource merging and a preview scan for nothing). No-UI only: ARCHITECTURE.md Testing (its bullet split per mode), DESIGN-SYSTEM.md "Previews are render tests" and "First things to build" step 4, and the `testing-setup` row in CLAUDE.md and AGENTS.md say the convention runs in `:core:designsystem`, and a feature adds `id("<package>.android.screenshots")` to its own `plugins { }` with its first screen and `@AppScreenPreviews` (per module, so features still on placeholders are not scanned), moving it into `AndroidFeatureConventionPlugin` once every feature has screens. UI text unchanged. Rendered in `out/NOUI/fix1-noui-lean/docs/ARCHITECTURE.md` with the project's own plugin id. |
| 10 | No-UI scroll-chrome accepted only padding the project never has | `d47dbb02e` | Option 2. In No-UI (`opt-else` fences, UI lines unchanged) the rule also accepts insets inside the scroll: `contentPadding = WindowInsets.<x>[.only(…)…].asPaddingValues()` on a list, and after `.verticalScroll(…)` `.windowInsetsPadding(…)`, `.safeDrawingPadding()`, `.safeContentPadding()`, `.systemBarsPadding()` or `.padding(WindowInsets.<x>.asPaddingValues())`; the pass-through `contentPadding` stays for a shell the user builds. The advice names the edge-to-edge forms; PERFORMANCE.md (No-UI block, with an example) and DESIGN-SYSTEM.md's rule row say the same. New No-UI variant test `ScrollChromeCasesTest` (six forms pass; insets on the list's modifier, insets before the scroll and a fixed `PaddingValues` are flagged at lines 1, 8, 10); the shared `CodeRuleCasesTest` pads with insets in No-UI. The F1.4 advice test's library list gains `WindowInsets`, `asPaddingValues`, `safeDrawing`, `windowInsetsPadding` (Compose foundation names). Gradle (row 5's No-UI build): `:build-logic:convention:test` ran, `ScrollChromeCasesTest` 2/2, `CodeRuleCasesTest` 22/22, `DesignSystemDocTest` passed. |
| 11 | PERFORMANCE-OVERLAY.md pointed at `tools/perf` everywhere | `7a703f571` | Every `perf.sh` / `gates.toml` / `devBenchmarkRelease` line is in `<opt:performance&ui_components>`; the other branch gives `dumpsys gfxinfo`, `am start -W` and Macrobenchmark's `StartupTimingMetric`, with the thresholds as plain numbers (0.5 % janky, 380 ms cold start, 58.4 MB PSS). `test_docs` checks the page (`FEATURE_DOCS`) like the other docs in every case that has the overlay: with the old page it fails on `tools/perf/perf.sh` and `tools/perf/gates.toml` in noui-standard, noui-everything and noui-everything-modular; green now. `run-as` joined the tool words the rule-id check skips (an adb command the page names). |
| 12 | D7: UI generation +16 % | none | Not fixable in code without dropping files from (g); the user decides (see Open). Not re-measured; the review's numbers stand and D7 is not reported as passing. |

Checks at the head (logs in `logs/NOUI/FIX1/`):

| Check | Result | Log |
|---|---|---|
| D1 generator suite, `memwait python -I -m unittest discover -s tests -t .` | 581 tests OK, 9 skipped, at `b7eeb6c9a` (580 OK at `7a703f571`) | `unittest-2.log`, `unittest-1.log` |
| D5 `compare_generations.py --before out/NOUI/base --after wt/NOUI` (105 specs) | 105 specs identical outside 1.6's globs; the reviewed `CodeRules.kt` hunk is the message only. Run at `7a703f571`: the two later commits touch only `tools/android_skills.py` (an exception path) and the generator's tests | `compare-2.log` |
| D2 `catalogue --check`, `tokens --check` | both current | `catalogue-tokens-check.log` |
| D3 `memwait python -m tests.consistency --quick` | 81 projects in 268 s, 0 new issues, 188 known (`-1.log` is the run between the two commits of row 6) | `consistency-quick-2.log` |
| D4 `memwait python -m tests.consistency` | 223 projects in 497 s, 0 new issues, 374 known | `consistency-full-2.log` |
| No-UI lean + devtools, add/remove, empty `feature/`, `locked gradle ./gradlew check assembleDevDebug --continue` | BUILD SUCCESSFUL | `build-noui-lean-removed.log` |
| UI `features: ["devtools"]`, `locked gradle ./gradlew :core:devtools:testDebugUnitTest` | BUILD SUCCESSFUL, 3/3 | `build-ui-devtools-only-test.log` |
| D12 `npm run typecheck`, `npm test`, `memwait npx next build` | ok; 249 tests, 0 fail | `web-typecheck-test.log`, `web-next-build.log` |

Not run: D7 (row 12), D8-D11 beyond the two builds above, D13 (no Go change), the phone rows.

Process note: a full matrix run on `f169cf3a1`'s walkers (started about 04:54, before row 6's second commit) was
stopped as a task, but its Python process and four workers kept running; stopping those PIDs was refused by the
permission classifier, so it ran to its end at 05:05 (`consistency-full-1.log`: 223 projects, 19624 new issues, which
is 223 x 88, the skill-XML `RES` class alone). It is evidence that nothing but that class was left; D4 is
`consistency-full-2.log`.

Open, for the orchestrator and the user:

- **The private names are already public.** `10f24c9df` (the commit that added them) is on `origin/staging-wp/NOUI-DOCS`
  and `origin/staging-wp/NOUI` (`git branch -r --contains 10f24c9df`). The fix removes them from the tree; only a
  history rewrite and a force push would take them out of those branches, which the rules forbid without the user.
  The user's call: leave them in that history, or rewrite and force-push those two branches (and keep them out of
  any release branch, which a squash would do). Also: the orchestrator's pre-push scan should carry the private names
  (case-insensitive) beside PUSHED.md §5's token patterns, so a later slip is caught before a push.
- **D7, generation time (REQUIREMENTS 59 g against m).** The review measured UI generation at 2.14 s against 1.84 s
  at the base (+16 %, about 0.3 s per project), almost all of it the 459 vendored skill files against 169 (about
  0.9 ms each on this machine; `shutil.copyfile` is no faster). Options, recommended first:
  (a) accept the cost of the full skills set in the gate report: (g) as the user asked, 0.3 s per project; catch: (m)'s
  "as fast as before" is then not literally true, and D7's +10 % bar has to be waived by the user;
  (b) ship fewer files, for example without glance-canonical-layouts' 45 reference `.kt` files: fewer files to copy;
  catch: the vendored copies stop being upstream's unmodified set (the manifest, the README's "unmodified" and the
  verbatim policy change), the skill points at files it does not have, and it may still not reach +10 %;
  (c) leave a skill out entirely (`LEFT_OUT`): catch: (g) asks for the latest official set.
- The UI template's own `app/src/main/baseline-prof.txt` still names `EverythingApplication` and its Hilt/Dagger classes,
  so in every UI project not named "Everything" those rules match nothing. With the identity tokens of row 3 in, the fix is
  the same rewrite of that file to the identity form; it changes UI output (D5), so it waits for the UI release work.
- Housekeeping: this round left generated projects in `out/NOUI/fix1-noui-lean`, `out/NOUI/fix1-ui-devtools` and
  `out/NOUI/fix1-read-std` (specs `out/NOUI/fix1-noui-lean.json`, `fix1-ui-devtools.json`), kept as evidence; nothing was
  written outside the workspace, the worktree and the scratchpad, and no setting or system state was changed.
