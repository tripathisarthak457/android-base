# NOUI-FINAL handoff (REQUIREMENTS 59 (n): everything left, then one deploy)

Started 2026-10-25 13:42 IST by the Opus planner of `scripts/noui-final.js`. **Plan:**
`D:/projects/tools/.overhaul/plans/2026-10-25/final.md` (lanes, file ownership, after-integration steps, how UI output
changes, risks). Read the orchestrator note at the top of `handoff/NOUI.md` before running Python, the API or `go test`.

| Lane | Worktree | Branch | Base |
|---|---|---|---|
| WEB | `wt/NOUI-FINAL-WEB` | `staging-wp/NOUI-FINAL-WEB` | `1f2fd6c85` |
| CORE | `wt/NOUI-NEXT-CORE` | `staging-wp/NOUI-NEXT-CORE` | `f5a773e74` (WIP `aebff213c`, unverified) |
| TEST | `wt/NOUI-NEXT-TEST` | `staging-wp/NOUI-NEXT-TEST` | `f5a773e74` (WIP `610593136`, unverified) |
| NAV | `wt/NOUI-FINAL-NAV` | `staging-wp/NOUI-FINAL-NAV` | `1f2fd6c85` |
| TOOLS | `wt/NOUI-FINAL-TOOLS` | `staging-wp/NOUI-FINAL-TOOLS` | `1f2fd6c85` |
| GUARD | `wt/NOUI-FINAL-GUARD` | `staging-wp/NOUI-FINAL-GUARD` | `1f2fd6c85` |

Integration: `wt/NOUI-FINAL` (`staging-wp/NOUI-FINAL`, from `staging-wp/NOUI` once the finish run's merge is in),
lanes merged in the order above. Logs under `logs/NOUI-FINAL/<LANE>/`, generated projects under
`out/NOUI-FINAL/<LANE>/`. Nothing in this batch pushes, touches `main`, deletes a branch or worktree, stashes or rewrites
history; `wt/NOUI` and `wt/NOUI-FIX` are off limits until the finish run's merge is in `staging-wp/NOUI`.

Each lane writes its own section below (`## <LANE>`: what it did, where reality differed and why, checks with logs,
what is open). Then come `## Integration`, `## After integration`, `## Fix round <n>`, `## Profiles` and
`## Gate fix <n>`.

## Cross-lane requests

(A lane that needs a change in a file it does not own writes it here: file, exact change, why. The integrator does each
and marks it done.)

- **Orchestrator, 14:25 Oct 25: the finish run's last re-review (finish:review#2, run wf_a2aae76a-192, saved in release/noui-finish-done.json) left two blocking findings on staging-wp/NOUI (merged at db2617930, which the integration merges). Do both, test-first, and mark them done:** **Integration (Oct 26 01:5x): 1 and 2 done in `776a3cd7f`; of 3, the flavours sentence in `0ecb71941`, the push sentence by TOOLS (`997b03265`), the No-UI code comments left to after-integration step 6 (plan 5.6; step 2 splits `SignIn.kt` and `Onboarding.kt` first).**
  1. `web/frontend/features/configurator/steps/IdentityStep.tsx`:131: Two Languages texts are shown in No-UI mode and are untrue there. (1) The site's Project step hint, shown to every visitor of the setup-only site: 'Each one you pick translates every screen and joins the language picker'. (2) The wizard's Languages question, generator/genkit/core/prompts.py:405-406: 'the language picker in Settings lists them'. My No-UI wizard run printed it (logs/NOUI/finish/review3-wizard-noui.log). No No-UI project has a language picker. With `language` on, SettingsRoute draws only PlaceholderScreen (out/NOUI/finish-review-2/mods/feature/settings/src/main/kotlin/com/example/reviewthree/feature/settings/Settings.kt), and the option's own feature_copy.language says 'for the picker your settings screen draws'. Without `language` (lean, none, and the wizard's default answers) there is no AppLocales, and no generateLocaleConfig either (fenced <opt:language> in app/build.gradle.kts), so nothing lists the languages anywhere. This is the same class as Finish fix 2's haptics finding (#3) and the README :catalog finding (#2). The fixer recorded it as open (handoff/NOUI.md:718-721). plans/2026-10-25/final.md:520-522 schedules only the wizard half, and only 'if the finish run's merge has not fixed it'. The site half is in no plan. Fix: Use the mechanism this branch added. Add `[field_copy].languages` to templates/android/options/ui_components.toml. `languages` is in COMMON_FIELDS and is not in [hides], so _field_copy accepts it. Suggested text: 'English is always included. Each one you pick gets every module's strings, the placeholder titles included, in a values-xx folder lint keeps complete; with the language feature, AppLocales.supported lists them for the picker your settings screen draws.' Then run `catalogue --write`. In the wizard, pass `described` to ask_languages and print described.get('languages') in place of the two dim lines. On the site, use `fieldCopy(target, state.uiComponents, 'languages') ?? <current text>` in IdentityStep. Add tests: a `{name: 'languages'}` entry in copy.test.ts's copyOf, so the existing /language picker/ in MISSING holds it, and a test_prompts case for each mode. With components, both texts keep their words.
  2. `templates/android/docs/ARCHITECTURE.md`:516: '`AppLocales.supported` lists the languages for the in-app picker.' is fenced only `<opt:language>`. So every No-UI project with `language` carries it: standard, the setup-only site's default preset, and everything (out/NOUI/finish-review-2/everything/docs/ARCHITECTURE.md:401, mods/docs/ARCHITECTURE.md:310). A No-UI project has no in-app picker. The settings route is a placeholder, and the option's own copy says the picker is the one your settings screen will draw. This is the same bar as the README ':catalog' finding: a generated No-UI doc naming something the project lacks. Fix: Fence the current line `<opt:language&ui_components>` (UI bytes unchanged). Add a `<opt:language&!ui_components>` variant that is true of the code, for example: '`AppLocales.supported` lists the languages for the picker your settings screen draws; `SettingsEvent.LanguagePicked` applies a choice through the settings route, and the phone's per-app language setting offers them on Android 13 and up (`generateLocaleConfig`).' Confirm UI bytes with the render check, as Finish fix 1 and 2 did. Optionally add an 'in-app picker' claim to GeneratedDocsTest.test_what_the_docs_say_asks_or_opens_is_what_the_code_does that holds only where the settings route draws something other than PlaceholderScreen.
  3. Also from that merge's open list, if no lane did them: UI projects without the catalog still carry the flavours sentence (UI_STALE_MODULES); code comments in No-UI output that still describe the shell (StartupViewModel.kt, SignIn.kt, Onboarding.kt, OnboardingNav.kt, Shortcuts.kt, AppBrowser.kt, the settings.gradle.kts header); a UI project with push but without onboarding or settings still says the permission is asked at the end of onboarding (docs, the rememberNotificationPermission KDoc, features/push.toml). Both modes may change now (59 (n)).
- **TEST, 18:05 Oct 25 (L3, after CORE and TEST are merged):** `templates/android/data/auth/src/test/kotlin/com/base/app/data/auth/DefaultAuthRepositoryTest.kt` (CORE's L9 test, so TEST did not touch it) keeps a private `MemoryTokenStore`, a second copy of `:core:datastore`'s `FakeAuthTokenStore` with one extra seam (`refuse`: `save` throws "Keystore unavailable"). Every fake is to live once. Change: (a) in `templates/android/core/datastore/src/testFixtures/kotlin/com/base/app/core/datastore/session/FakeAuthTokenStore.kt` add `/** While set, [save] throws, as a Keystore that cannot be reached does. */ var refuseSaves = false` and make `save` start with `check(!refuseSaves) { "Keystore unavailable" }`; (b) in `DefaultAuthRepositoryTest.kt` delete `MemoryTokenStore`, construct `FakeAuthTokenStore()`, read `store.stored` where it reads `store.saved`, set `store.refuseSaves = true` where it sets `store.refuse = true`, import `com.base.app.core.datastore.session.FakeAuthTokenStore` and drop the now unused `AuthTokenStore`/`Flow`/`MutableStateFlow` imports; (c) add `testImplementation(testFixtures(project(":core:datastore")))` to `templates/android/data/auth/build.gradle.kts`. Its `replace` answering `false` is not needed by any of its tests (none calls `replace`). Proof: `:data:auth:testDebugUnitTest` green in a UI everything project, and in the modular one (the dependency stays on `:core:datastore`, which is not split). **TEST, 22:59: (a) and (c) are in TEST's files and done on `staging-wp/NOUI-NEXT-TEST` (`93fc35749`); the integrator does (b) only.** **Integration: (b) done in `2f572e71e`.**

- **TOOLS, 18:25 Oct 25 (harness advice in UI projects without `performance`; `test_android_truth.HARNESS_PENDING`
  lists these six files and fails while a listed file stops naming the harness, so take each off the list as it is
  fenced):** each line below names `tools/perf`, `tools/adaptive`, `perf.sh` or `gates.toml` in a UI project without
  `performance`, which owns that harness. Fence it `<opt:performance>` (`<opt:performance&ui_components>` where the
  text is already inside a `ui_components` fence) with the harness wording kept, and an `<opt-else:…>` without it:
  1. `app/src/dev/kotlin/com/base/app/perf/DevLaunchOptions.kt`, KDoc line "Launch extras for the benchmark journeys and
     tools/perf, read by dev builds only:" -> else "Launch extras for scripted runs, read by dev builds only:".
  2. `app/src/dev/kotlin/com/base/app/perf/ShellLog.kt`, KDoc "which tools/perf reads back: `nav_stress` checks the stack
     invariants on every line, and `restore` compares the state before and after process death" and "which
     tools/adaptive checks across resizes" -> else "which a scripted run can read back to check the stack invariants on
     every line, or compare the state before and after process death" and "which a resize check can read".
  3. `app/src/main/kotlin/com/base/app/ui/AppRoot.kt` (NAV's file), comment "Test tags become resource ids, which is how
     benchmarks and tools/perf find tabs and lists." -> else "Test tags become resource ids, which is how UI Automator
     and Macrobenchmark find tabs and lists."
  4. `catalog/src/main/kotlin/com/base/app/catalog/FrameTimes.kt`, KDoc "the numbers that count come from a release build
     and `perf.sh`." -> else "the numbers that count come from a release build on a phone (docs/PERFORMANCE.md,
     "Measuring")." (check that heading against the doc after the docs pass).
  5. `catalog/src/main/kotlin/com/base/app/catalog/sections/sheets/CatalogSheets.kt`, KDoc "its test tag is what
     `record.sh` and `perf.sh` tap." -> else "its test tag is what a UI Automator script taps."
  6. `core/perfhud/src/test/kotlin/com/base/app/core/perfhud/recording/PerformanceRecorderTest.kt`, KDoc "the launch
     extra `perf.sh` passes must win" (inside `<opt:ui_components>`) -> `<opt:performance&ui_components>` and an else
     with "the launch extra a scripted run passes must win".
  **Integration: all six fenced, `HARNESS_PENDING` gone, in `a74c97de5` (the test's pattern now stops at a word boundary: `perf.shell`, the dev build's log tag, matched `perf.sh`).**
- **TOOLS, 18:25 Oct 25 (F15.4):** the consistency matrix (`generator/tests/consistency`) needs a row for
  `add_component.py` like add_feature's: on a No-UI and a UI shape, run `add_component.py Tag --project <p>` after
  generation and build with the rest (`verifyCodeRules`, `:core:designsystem:testDebugUnitTest`). Not in TOOLS's files. **Integration: done in `64d9f56aa` and `585542469` (quick and full matrix runs `component-everything` and `noui-component-everything`; CI's everything and noui-everything rows add `Tag` before `check`).**
- **TOOLS, 23:05 Oct 25, for the integrator (when NAV and TOOLS are both merged): the UI app profile's rules for
  classes NAV moved.** `test_android_truth.ProfileTest.test_the_profiles_name_only_classes_the_project_has` (TOOLS,
  `8d99907e1`) reads `templates/android/app/src/main/baseline-prof.txt`, which NAV's item 7 above says goes stale: its
  owners `feature/<f>/<F>NavModule`, `feature/<f>/di/<F>NavModule` and their Hilt factories
  (`<F>NavModule_<F>NavGraphFactory`, `..._<F>LinksFactory`) are `feature/<f>/navigation/<F>Navigation` after NAV's L2,
  and any shell owner moved under `core/navigation/shell/`. The test lists the first ten stale lines it finds; rename
  each owner as NAV renamed the class (the same work NAV did for the No-UI app profile, `generator/variants/noui/app/
  src/main/baseline-prof.txt`), keep the identity form (`BaseApp...`, `com/base/app/...`), and drop a rule whose class
  no longer exists (the `navKeys` providers). The Profile phase then re-records it in the same form. Why: on NAV's
  branch nothing reads the UI app profile, and on TOOLS's branch the moved classes do not exist yet, so neither lane can
  make the change and stay green. **Integration: done in `541812d98` (identity form; the navKeys providers' class and member rules dropped).**
- **TOOLS, 23:05 Oct 25, for GUARD and the integrator (F14):** `:core:testing:compose` now depends on Roborazzi
  (`roborazzi`, `roborazzi-compose`, `roborazzi-compose-preview-scanner-support`), `composable-preview-scanner`,
  `roborazzi-accessibility-check` and ATF `4.1.1` (`accessibility-test-framework`, new catalog entries
  `roborazzi-accessibility-check` and `accessibility-test-framework`, version `accessibilityTestFramework`). If
  dependency-guard baselines a configuration that reaches `:core:testing:compose` (a test classpath), its baseline
  gains these; the release runtime classpaths do not change. **Integration: nothing to change: dependency-guard baselines only `:app`'s `prodReleaseRuntimeClasspath`.**

- **NAV, 18:37 Oct 25 (L1, L5, L2: files other lanes own that name moved code; branch `staging-wp/NOUI-FINAL-NAV`):**
  **Integration: 1 and 4 by GUARD (`619045b3f`, `5f70f202a`); 2 in `a1429a444`; 3's module list by GUARD (`ce3e383e4`), its baseline names in `a1429a444` and its docs lines in `a1429a444`/`676ae2b1d`; 5 and 6 in `3c01d5a77`; 7: the UI app profile follows in `541812d98`, the shell's and the runtime's own profiles wait for the Profile phase; 8 noted; 9 is after-integration step 6; 10 in `a1429a444`; 11 in `a74c97de5`.**
  1. **GUARD, `build-logic/convention/src/main/kotlin/com/base/app/buildlogic/guard/CodeRules.kt:134`:**
     `val inNavigation: Boolean = module == "core/navigation" || module == "core/navigation/shell"`. Why: the shell's
     files now sit in the module `core/navigation/shell`, so every rule scoped by `inNavigation` (AdaptiveRules'
     `uiModules`, the one-pixel-line rule at `ComposeRules.kt:195`, `legacy-back`, both `SheetRules` scopes,
     `layer-package`) stopped
     covering the code it covered before the split. Nothing fails without it; the shell is just unguarded by them.
  2. **GUARD, `config/code-rules-baseline.txt` lines 33-36,** with item 1, become:
     `core/navigation/shell/src/main/kotlin/com/base/app/core/navigation/shell/presentation/PresentationCard.kt layer-package 1`,
     `core/navigation/shell/src/main/kotlin/com/base/app/core/navigation/shell/presentation/PresentationHost.kt layer-package 1`,
     `core/navigation/shell/src/main/kotlin/com/base/app/core/navigation/shell/ShellShortcuts.kt legacy-back 1`,
     `core/navigation/shell/src/test/kotlin/com/base/app/core/navigation/shell/presentation/PresentationCardTest.kt layer-package 1`.
     If GUARD's regions (F10) put them in a navigation region, keep them there; the paths-exist test needs the new paths.
  3. **GUARD, `build-logic/convention/src/main/kotlin/com/base/app/buildlogic/android/ComposeStability.kt:26`:** add
     `":core:navigation:shell"` to `STABILITY_CHECKED_MODULES` (the shell's composables left a checked module), and in
     `config/compose-stability-baseline.txt` rename `com.base.app.core.navigation.back.BackHandlerEffect(handler)` to
     `com.base.app.core.navigation.shell.back.BackHandlerEffect(handler)` and
     `com.base.app.core.navigation.stack.rememberEntryLifecycle(key)` to
     `com.base.app.core.navigation.shell.stack.rememberEntryLifecycle(key)` (reasons unchanged). With it, name
     `:core:navigation:shell` beside `:core:navigation` in `docs/ARCHITECTURE.md`'s `checkComposeStability` row,
     `docs/PERFORMANCE.md`'s "Parameters are stable" item and `templates/android/README.md:75`; NAV left those three
     lines alone so they stay true of the code on its branch.
  4. **GUARD, a rule like `ShellSeamApi`'s (`SheetRules.kt:73`):** `NavigationHostApi` (`@OptIn`, `optIn.add`) in a
     file under `feature/` fails `verifyCodeRules`, and a `:feature:*` build file depending on `:core:navigation:shell`
     fails `verifyModuleDependencies` (or the same code rule over build files). Why: a feature navigates through
     `AppNavigator` and must not reach a host's internals or the shell; `test_android_navigation.py` checks the generated
     shapes, the rule keeps it true in a developer's own edits.
  5. **Integrator, `templates/android/core/common/src/main/kotlin/com/base/app/core/common/navigation/AppNavKey.kt:7`**
     (KDoc): "a key that is not registered in its feature's `navKeys` block" -> "a key no feature registers with
     `entry<K>`". `navKeys` no longer exists (L5).
  6. **Integrator, `templates/android/README.md` "Adding a feature" (lines 202-214, the finish run's hot file):** a
     feature needs two things now; drop step 3 (`navKeys { subclass(...) }`) and the paragraph "Step 3 is the one worth
     not forgetting...", and say that `entry<K>` records the key's serializer, so a key that can be opened can be saved
     (`NavKeyRegistrationTest` still checks every key).
  7. **TOOLS / the Profile phase, `templates/android/app/src/main/baseline-prof.txt`:** its rules for moved classes are
     stale until re-recorded: the shell's classes moved from `com/base/app/core/navigation/{AppShellKt, ShellState,
     ShellStateKt, ShellTab, ShellConfig, HostCrossfadeKt, NavigationChrome, AppNavigationHostKt}` and
     `.../navigation/{back/BackCard*, back/BackHandlerEffectKt, back/GateSettleKt, back/ShellRootBackKt,
     back/StackBack*, stack/*, presentation/*, transition/*}` to the same names under `com/base/app/core/navigation/shell/`;
     `DeepLinksKt.OpenDeepLinks` is `shell/OpenDeepLinksKt`; the features' `*NavModule` objects are
     `feature/<f>/navigation/<F>Navigation`. Run `tools/perf/split_profile.py` (it now knows `core/navigation/shell`).
     No test reads the UI app profile, so nothing fails meanwhile.
  8. **Integrator, for the record:** NAV renamed `generator/translations/android/*/core-navigation.json` to
     `core-navigation-shell.json` (`2f118aebe`, a rename only): the strings moved with the shell, and the generator
     matches a translation file to its module by name, so every spec with a second language failed to generate.
  9. **After-integration step 6:** `OnboardingNav.kt` is now `feature/onboarding/.../navigation/OnboardingNavigation.kt`,
     and `Shortcuts.kt` stays in the runtime; the shell's KDoc mentions in the runtime (`AppBackStack`, `BackGate`,
     `Presentation`, `NavigationSettled`, `Shortcuts`, `PendingDeepLink`) are in every No-UI project.
  10. **GUARD, with item 1 (NAV, 23:0x Oct 25):** the comment above `layer-alpha-fill` in
      `build-logic/convention/src/main/kotlin/com/base/app/buildlogic/guard/ComposeRules.kt:174` says the back card is
      "in :core:navigation"; it is in `:core:navigation:shell` now. Comment only.
  11. **Integrator (NAV, 23:0x Oct 25), TOOLS's item 3 above:** `app/src/main/kotlin/com/base/app/ui/AppRoot.kt` is
      NAV's file, but NAV left its "tools/perf" comment unfenced on purpose: TOOLS's
      `test_android_truth.HARNESS_PENDING` still lists it, and fencing it on NAV's branch would fail that test after
      the merge. Fence it and take it off the list in one step at integration.
- **NAV, 18:37 Oct 25, for the orchestrator (not a code change): D: is nearly full.** `df` showed 3.4 GB free at
  17:50, 1.4 GB at 18:35 and about 1.0 GB at 18:37, with several lanes building at once (`out/NOUI-FINAL/TEST` alone
  is 2.4 GB; NAV's is 12 MB). A full `check assembleDevDebug` of an everything project writes gigabytes of build
  output, so the next full builds of any lane can fail on a full disk, and a Gradle cache write can corrupt. Freeing
  space is the user's or the orchestrator's call (earlier lanes' `out/` folders, `D:\.caches\gradle`); NAV runs its
  proofs one shape at a time in one regenerated folder to keep its own share small. **Integration: D: had 82 GB free at 00:35 Oct 26 and 79 GB at 01:52.**

- **CORE, 20:39 Oct 25, for TOOLS (`AndroidScreenshotsConventionPlugin.kt`, the ATF tester) and the docs pass: a plain
  `testDebugUnitTest` renders no preview.** Roborazzi 1.76.0's `captureRoboImage` (compose and preview-scanner
  support alike) returns at once unless a task type is on (`RoborazziOptions.taskType.isEnabled()`: record, compare or
  verify; bytecode of `RoborazziPreviewScannerSupportKt.captureRoboImage`). Evidence: in a UI everything project,
  `:core:designsystem:testDebugUnitTest --tests '*AppDecimalFields*'` passes its five configurations in about 70 ms each
  (`logs/NOUI-FINAL/CORE/golden-decimal-probe-1.log`), while the same preview throws in record mode
  (`record-b-2.log`). So the convention's KDoc ("Without either, the unit tests still render every preview, so one
  that crashes at some size fails the build"), DESIGN-SYSTEM.md's and AGENTS.md's matching claims, and an ATF tester
  that audits inside `captureRoboImage` are true only under `recordRoborazziDebug`/`verifyRoborazziDebug`/
  `compareRoborazziDebug`. Fix: the tester renders the preview itself (`composeTestRule.setContent`, then captures
  only when a task type is on), or the convention turns compare on for the unit tests; then the render cost below is
  paid on every `check`. Cost measured here (corrected by CORE at 23:35): recording the design system's 79 component
  previews x 5 configurations (395 images) took 218 s with compilation (`record-b-3.log`); the 20 minutes first
  reported were one preview that never went idle, waiting out its timeout (`record-b-2.log`, fixed in `493cdf939`). **Integration: TOOLS's tester renders every preview in a plain `check` (`796d3e31d`); the merged UI build rendered and audited CORE's component previews. The docs pass (step 7) reconciles the sentences.**
- **CORE, 23:35 Oct 25, for GUARD (`config/code-rules-baseline.txt`, line 30 at `f5a773e74`), forced by S2:** a No-UI
  project's `DesignTokens.kt` is now the skeleton (well under 500 lines), so the unfenced line
  `core/designsystem/src/main/kotlin/com/base/app/core/designsystem/theme/tokens/DesignTokens.kt file-length 1`
  fails every No-UI `verifyCodeRules` ("accepts 1, the file now has 0"; `logs/NOUI-FINAL/CORE/narrow-noui-everything-1.log`).
  Change: fence that line `# <opt:ui_components>` ... `# </opt:ui_components>` (in whichever region F10 gives it); UI
  projects keep it (their tokens file is still about 880 lines). Until it lands, a No-UI build of CORE's branch fails
  only there; CORE proved the rest with the line removed in its generated projects (scratch, `narrow-noui-rules-2.log`). **Integration: done in `a1429a444`.**
- **CORE, 23:35 Oct 25, for GUARD (`SheetRules.kt`, `sheetModifiers`):** `sheet-modifier` takes the first
  `modifier =` anywhere inside an `AppSheet(...)` call's parentheses as the sheet's, so a footer such as
  `footer = { AppButton(..., modifier = Modifier.fillMaxWidth()) }` fails it although the sheet's own modifier is
  untouched (the two sheet previews did, `failfirst-money-1.log`). Change: read only the call's top-level arguments
  (skip lambdas and nested parentheses, as `topLevelArgument` already does for the value). CORE worked around it in
  `9a89a1e12` (the previews' footer button comes from a private function); nothing else needs to change. **Integration: done by GUARD in `239e51a7f`.**
- **CORE, 23:35 Oct 25, for TOOLS (the ATF tester) and step 4 (e), for information:** the component previews' "Right
  to left" configuration rendered left to right in Robolectric (the library's unit-test app declares no RTL
  support); `AppPreviewTheme` now takes the direction from the locale (`e559bc82b`), so the tester will audit real
  RTL layouts from the integration on. "Narrow" (`widthDp = 320`) is the same width as Robolectric's default screen,
  so in the screenshot tests it repeats "Light" at the same size; it still differs in Android Studio.

- **GUARD, 00:3x Oct 26 (L8, F13, and NAV's requests to GUARD; branch `staging-wp/NOUI-FINAL-GUARD`):**
  **Integration: 1 in `df73ab1bf` (for Android projects; the step is two lines); 2 in `a1429a444` and `676ae2b1d` (baseline paths, stability names, the runtime's design-system edge and its test unfenced, the shell named in the three docs); 3 noted; 4 is the docs pass (step 7); 5 in `a1429a444`.**
  1. **Integrator, `generator/create_project.py` `report()` (the CLI's "Next" steps):** after step 2 add
     `print("    3. Commit app/dependencies/ after the first ./gradlew check: it is the list of libraries the prod release ships, which every later check compares against.")`
     and renumber the firebase line to 4 (its test, if any, follows). Why: dependency-guard writes the list on the
     first `check` and passes; it guards nothing until committed (the project CI warns until it is). The README's
     next steps and AGENTS.md/CLAUDE.md say the same in the docs pass (CLAUDE.md is at 119 of its 120-line budget in
     a No-UI everything project, so GUARD added the check to `docs/ARCHITECTURE.md`'s guard table only).
  2. **Integrator, with NAV's merge (NAV's requests 2 and 3, which GUARD could not do on its branch: the paths and
     names do not exist there, and `test_android_guards.py` fails on a baseline entry the project lacks):** move the
     four `core/navigation/...` lines of `config/code-rules-baseline.txt` to NAV's `core/navigation/shell/...` paths
     (keep them in the `# Navigation shell` region, `<opt:ui_components>`), and rename the two
     `config/compose-stability-baseline.txt` entries to `...core.navigation.shell.back.BackHandlerEffect(handler)` and
     `...core.navigation.shell.stack.rememberEntryLifecycle(key)`. Done on GUARD's branch: `inNavigation` covers
     `core/navigation/shell` (`619045b3f`), `:core:navigation:shell` is in `STABILITY_CHECKED_MODULES`, the
     `navigation-host-api` rule and the graph's shell edges (`5f70f202a`). Also, once NAV's split takes the shell out
     of the runtime: drop the `<opt:!ui_components>` fence around `DESIGN_SYSTEM` in `ModuleGraph.kt`'s
     `NAVIGATION_HIDDEN`, around `ModuleGraphTest`'s "the navigation runtime never sees the design system" case,
     and the UI wording of `ModuleKind.NAVIGATION`'s advice, so the runtime never sees the design system in either
     mode; and name `:core:navigation:shell` beside `:core:navigation` in `docs/ARCHITECTURE.md`'s
     `checkComposeStability` row, `docs/PERFORMANCE.md`'s "Parameters are stable" item and `README.md`'s
     `checkComposeStability` line.
  3. **Integrator, for the record: two edits outside GUARD's files.** `templates/android/template.toml`'s marker
     `syntax` table gained `compose-stability-baseline.txt` (`8e5d60689`, the stability baseline's fences) and
     `.editorconfig` (`3eb817849`, the Compose Rules settings by feature and option); and
     `generator/variants/noui/core/ui/src/main/kotlin/com/base/app/core/ui/MessagePresenter.kt` gained one reasoned
     `// allow(feature-composition-local)` marker (`619045b3f`): the No-UI message seam is a composition local by
     design, and the rule now runs in both modes.
  4. **Docs pass (after-integration step 7):** AGENTS.md and CLAUDE.md's "Build and verify" should say that `check`
     runs the Compose Rules for ktlint through Spotless and dependency-guard, that the first build writes
     `app/dependencies/` to commit, and that `./gradlew dependencyGuardBaseline` rewrites the list after a deliberate
     change; README.md's next steps the same. Mind CLAUDE.md's 120-line budget (`test_docs.BUDGETS`).
  5. **Integrator, CORE's 23:35 request on `config/code-rules-baseline.txt` (the `DesignTokens.kt file-length 1` line,
     now in the `# Design system and catalog` region):** fence it `# <opt:ui_components>` ... `# </opt:ui_components>`
     when CORE's S2 is merged. Not done on GUARD's branch: without S2 a No-UI `DesignTokens.kt` is still 842 lines here,
     so the fence would fail every No-UI `verifyCodeRules` on this branch, as the unfenced line does on CORE's.
     CORE's second request (`sheet-modifier` reading a slot's `modifier =`) is done here: `239e51a7f`.

## WEB

Lane WEB, `wt/NOUI-FINAL-WEB`, branch `staging-wp/NOUI-FINAL-WEB` from `1f2fd6c85`. **Done at 17:55 Oct 25, head
`36f42853a`.** No Gradle, no phone. Logs in `logs/NOUI-FINAL/WEB/`.

Commits:
- `61dfa2508` (API, from the 13:56 run, checked again here as someone else's: kept unchanged). `store.Adoption` gains the
  dimension `ui_components` (`AdoptionDimensions` and the weekly `UNION ALL`, now the constant `weeklyAdoption`);
  `GenerationRow.UIComponents` (the Generations list, and a `ui_components` column in the CSV export, whose cells moved
  into `csvRow` so a test can line them up with `csvColumns`); `Filter.UIComponents`, validated in `parseFilter`
  against `store.UIComponentsValues` (`with`, `without`; anything else is the admin's 400 `bad_request` on the field)
  and bound as a parameter in `Filter.generations`, so every generation read (overview, series, adoption, failures,
  durations, clients, generations, CSV) honours it. The daily series reads the raw rows under this filter
  (`rollupCovers`), because the rollup keeps no spec. The integration test seeds one `"ui_components": false` spec and
  checks the dimension, the weekly value, the filter on overview, series and generations, and a spec-less row.
- `55e3dcf96` (portal): the Adoption panel's "UI components" breakdown and weekly-heatmap choice (`dimensions.ts`), the
  Generations table's "UI components" column beside the preset (`columns.ts`, `—` for a row with no spec), the filter
  bar's select with exactly the API's two values (`filters.ts` `UI_COMPONENTS`, sent by `query`), the `Generation`
  type's field, one sentence in `web/README.md`'s admin text.
- `36f42853a`: `next` 15.5.27 -> **15.5.28** (exact pin; the newest 15.5.x, npm's `backport` tag, published Oct 14;
  both advisories' ranges end at 15.5.27), lock file from `memwait npm install next@15.5.28 --save-exact`, the
  `overrides` block kept (next resolves the hoisted postcss 8.5.23). Not 16.x.

### Where reality differed from the plan, and why (also in `decisions/LOG.md`, Oct 25 13:59)

- The expression compares jsonb, `g.spec -> 'ui_components' = 'false'::jsonb`, not text (`->> ... = 'false'`): a
  JSON string `"false"` is refused by the API's option check and never made a project without components, so it must
  not count as `without`.
- A row with no spec (refused before its spec was read, such as a rate-limited request) is `''`, not `with`: it
  matches neither filter value and shows `—` in the table, as a row without a preset matches no preset filter. Adoption
  counts only generated projects, which always have a spec, so its two keys are `with` and `without`.
- The filter applies to every generation read, not only Adoption: one parameter for the whole portal, as the other
  filters work.

### Checks (all at head `36f42853a` unless named)

| Check | Result | Log |
|---|---|---|
| Go tests seen failing first (compile errors, then the httpapi filter tests failing with the field unwired) | red | `go-test-red.log` |
| Frontend admin tests seen failing first (missing modules, then the dimension and column tests) | red | `npm-test-red.log` |
| `gofmt -l .` empty, `go vet ./...`, `go build ./...` | pass | `go-checks-1.log` |
| `go test ./... -count=1` (all 7 packages ok; Smart App Control let the binaries run this time) | pass | `go-checks-1.log` |
| the new Go tests verbose: 3 store unit tests and 3 httpapi tests pass; the store integration tests skip without `TEST_DATABASE_URL` (CI runs them on Postgres) | pass / skip | `go-checks-1.log` (end) |
| `memwait npm ci` | pass | `npm-ci-audit-2.log` |
| `npm ls next` -> `next@15.5.28` | pass | `npm-ci-audit-2.log` |
| `npm audit --omit=dev` and the full `npm audit`: 0 vulnerabilities (before: next critical + high, `npm-audit-before.log`) | pass | `npm-ci-audit-2.log` |
| `npm run typecheck`, `npm test` (260 tests) | pass | `typecheck-test-3.log` |
| `memwait npx next build` on 15.5.28 | pass | `next-build-1.log` |
| Browser pane on `next start` (a second build with `NEXT_PUBLIC_API_BASE=http://127.0.0.1:8080`, the API built at head and run with the real Python, no database): landing, configurator (Features step with Lean 6 / Standard 18 / Everything 43 from `/api/options`, the No UI components box), `/admin` token screen, no token typed, no console errors; both servers stopped | pass | `browser-smoke.log`, `next-build-smoke.log`, `api-smoke.log` |

### Open

- The store integration test (`TestIntegrationAdminQueriesReadWhatWasRecorded`, with the new `ui_components`
  assertions) needs Postgres: CI on the pushed branch is its evidence.
- The `/admin` portal with data (the new select, column and breakdown against a real database) needs the user's token
  and a database: the release gate's `/admin` check.
- The cross-lane requests above (IdentityStep.tsx, prompts.py, ARCHITECTURE.md) are outside WEB's files and
  `IdentityStep.tsx` is a finish-run hot file; WEB did not touch them.
- `npm ci` warns that `@tailwindcss/oxide`'s postinstall is not covered by npm 11's `allowScripts`; it was so at the
  base too, and the build does not need it (the platform binary comes as an optional dependency).

## NAV

Lane NAV, `wt/NOUI-FINAL-NAV`, branch `staging-wp/NOUI-FINAL-NAV` from `1f2fd6c85`. **Done at 23:57 Oct 25 (third
run), head `3595e3d27`.** Logs in `logs/NOUI-FINAL/NAV/`, projects (sources only, build output deleted after each
result) in `out/NOUI-FINAL/NAV/`. The first two runs' commits and the second run's uncommitted host-API change were
checked as someone else's and kept; this run found and fixed the shell's resource names (lint), and ran every narrow
proof again at its own head. No full preset build and no `assembleProdRelease` here (the orchestrator, 21:0x): the
integration and the gate build the six shapes and R8.

Commits (all with `git mv` where files moved, so `git log --follow` reaches the old history):
- `b2948a6d8` L5: `entry<K>()` records `K::class` with `serializer<K>()` on the `NavGraphEntry`; `NavKeySerialization`
  is built from `Set<NavGraphEntry>`; every `navKeys { }` block and `@IntoSet SerializersModule` provider is gone
  (features, catalog, No-UI `StartNavigation.kt`, the scaffold's `Navigation.kt`); `NavKeyRegistrationTest` builds the
  serialization from the graphs it finds on the classpath. `NavKeyRestoreTest` (a key registered only with `entry`
  survives save and restore) was red first.
- `5c7fd1fda` L10: `TabStacks` in the runtime (per-tab `AppBackStack`s, the tab in front, `select` with
  reselect-to-root and reselection counters, `show`, its `Saver` and `rememberTabStacks`); `ShellState` composes it and
  keeps the presentation stack, More, chrome, window, sidebar and anchors. `TabStacksTest` was red first.
- `f5f645178` L1: `:core:navigation` is the runtime in both modes, with no design-system dependency;
  `:core:navigation:shell` (`core/navigation/shell/`, package `<pkg>.core.navigation.shell`, subpackages `.back`,
  `.presentation`, `.stack`, `.tabs`, `.transition`) holds the shell, its strings, tests and profile, with
  `api(project(":core:navigation"))` and the design system; only `:app` and `:catalog` depend on it. Host-only members
  are public behind `@RequiresOptIn(ERROR) @NavigationHostApi`, opted in module-wide by the runtime and the shell. No-UI
  owns `core/navigation/shell` plus its include and dependency lines (about thirty `owns` entries went); the NavDisplay
  `AppNavigationHost` and the single-stack `OpenDeepLinks` are runtime variants. The profile is split by class;
  `split_profile.py` knows the shell module.
- `2f118aebe` the translations follow the strings (`core-navigation.json` -> `core-navigation-shell.json`).
- `9f6151f34` L2: `AuthNav.kt`, `OnboardingNav.kt`, `SettingsNav.kt`, `di/{Feed,Profile,Search}NavModule.kt` ->
  `navigation/<Feature>Navigation.kt` with `internal object <Feature>Navigation`; the No-UI app profile follows.
- `2ec1fe21a` the shell's tests import their layer probe from the shell's test package.
- `e31dc2b6d` `generator/tests/test_android_navigation.py` (red at the base first).
- `265fedbc8` the No-UI "build your shell" list names `TabStacks`; two passages rewrapped.
- `6aa0a81a3` `AppBackStack.apply` and `applyWhileStarted` are host API too (the second run's uncommitted change).
- `4929ad29a` PERFORMANCE.md's shell line rewrapped to 120 columns.
- `3595e3d27` the shell's four strings are `shell_*`: the library convention sets each module's `resourcePrefix` to its
  namespace's last segment, so `navigation_*` failed `:core:navigation:shell:lintDebug` with four `ResourceName` errors
  in every UI project (`guard-1.log`); the second run's narrow builds ran no lint. The translations follow the keys.

### Where reality differed from the plan, and why (also in `decisions/LOG.md`, Oct 25 18:43 and 23:16)

- The UI `OpenDeepLinks` takes a `ShellState`, so it lives in the shell (`shell/OpenDeepLinks.kt`); the runtime keeps
  `DeepLink`, `DeepLinkRoute`, `LinkOrigin`, `PendingDeepLink`, and No-UI's single-stack `OpenDeepLinks` is a runtime
  variant beside its `AppNavigationHost`.
- `ScrollToTopOnReselect` stays in the runtime but cannot call the design system's `animateScrollToTop`, so it carries
  the same two-screens-then-animate scroll privately.
- The shell's tests keep their own copies of the test keys, `Screen` and `testGraphs`: shared test fixtures are TEST's
  convention, which lands at integration (open below).
- `modes.py`, `tabs.py`, `settings.py`, the scaffold's generator code, `add_feature.py` and `remove_feature.py` needed
  no change: the nested module is not a feature or data tier, `ShellTab`'s import is in the template's
  `AppDestinations.kt`, and the shared-key folders are the runtime's, which did not move. No R8 rule names a moved
  class (`app/proguard-rules.pro` and every consumer file checked), and `benchmark/` names none.
- `apply`/`applyWhileStarted` behind the host API, beyond the plan's list (same kind: they move a stack or take the
  host's commands).
- The translation files and their keys (not in NAV's list) changed because the strings moved and took the module's
  prefix; a rename and a key rename only.
- Proofs: narrow only (the orchestrator's 21:0x instruction), with the guards the second run had left out (code rules,
  module edges, api purity, stability, lint of both navigation modules).

### Checks (logs in `logs/NOUI-FINAL/NAV/`)

| Check | Result | Log |
|---|---|---|
| Seen failing first: `NavKeyRestoreTest` (no serializer registered), `TabStacksTest` (no `TabStacks`), `test_android_navigation.py` at the base (every shape), the shell's lint (4 `ResourceName` errors) | red | `l5-restore-red-2.log`, `tabstacks-red.log`, `test-navigation-red.log`, `guard-1.log` |
| UI everything, compact: `:core:navigation:testDebugUnitTest :core:navigation:shell:testDebugUnitTest :app:testDevDebugUnitTest :feature:sample:testDebugUnitTest :catalog:compileDebugKotlin` at `4929ad29a`; the shell, app and catalog again at `3595e3d27` | pass | `narrow-4.log`, `narrow-5-ui.log` |
| No-UI everything, compact: `:core:navigation:testDebugUnitTest :app:testDevDebugUnitTest` (the No-UI half the second run's `narrow-3.log` never ran) | pass | `narrow-4.log` |
| UI everything: `verifyCodeRules verifyModuleDependencies verifyApiPurity` (every module), `:core:navigation:checkComposeStability`, `:core:navigation:lintDebug :core:navigation:shell:lintDebug`: 0 errors (warnings: unknown lint ids from the shared lint config) | pass | `guard-2-ui.log`, `guard-2-ui-lint-*.txt` |
| No-UI everything: the same guards and `:core:navigation:lintDebug` | pass | `guard-1.log` (second half) |
| UI lean: `add_feature.py orders --tab`, then `:feature:orders:testDebugUnitTest :feature:orders:verifyCodeRules :feature:orders:verifyModuleDependencies :app:testDevDebugUnitTest` (NavKeyRegistrationTest reads the added graph); `remove_feature.py orders --yes` (no file names orders), then `:app:compileDevDebugKotlin` | pass | `addrm-ui-lean-1.log` |
| No-UI lean: the same without `--tab` | pass | `addrm-noui-lean-1.log` |
| Generator suite at `3595e3d27`: 615 tests OK, 9 skipped (the run at `4929ad29a` had one error, Windows refusing `os.rename` of a temp folder, WinError 5; `test_translations` alone then passed, `unittest-3*.log`) | pass | `unittest-4.log` |
| `tests.consistency --quick` at `4929ad29a`: 81 projects, 0 new issues, 188 known | pass | `consistency-quick-2.log` |
| Full: the six shapes' `check assembleDevDebug`, `assembleProdRelease` on both everything compact | not run here (orchestrator) | the integration's and the gate's |

### Open

- The full builds and R8 at integration and the gate. The only R8 risk left is kotlinx.serialization's own: `entry<K>`
  resolves `serializer<K>()` where a feature registers, which the library's bundled keep rules cover; process-death
  restore on a release build is the phone smoke's row. The Spotless carry-over applies as everywhere.
- Cross-lane requests NAV 1-11 above (GUARD: `inNavigation`, both baselines, stability module list, the no-opt-in
  rule, a comment; integrator: `AppNavKey.kt` KDoc, README "Adding a feature", the AppRoot.kt harness comment with
  TOOLS's list; the Profile phase: the app profile's moved classes).
- The shell's tests' copies of `TestKeys`/`TestScreens` become one shared fixture once TEST's testFixtures convention
  is merged.
- The runtime's checked-in profile still holds rules for classes that moved or went (`R`, `DeepLinksKt`); harmless
  (a rule for a missing class is skipped) until the Profile phase re-records it.

## CORE

Lane CORE, `wt/NOUI-NEXT-CORE`, branch `staging-wp/NOUI-NEXT-CORE` from `f5a773e74`. **Done at 00:32 Oct 26 (third
run), head `51f085cee`.** No phone, no full preset build (the orchestrator's 21:0x rule); narrow Gradle tasks in
generated projects under `out/NOUI-FINAL/CORE/`, logs in `logs/NOUI-FINAL/CORE/`. The earlier runs' commits (the WIP
`aebff213c`, then `d5937e8a6`..`d3044f71e`) were read again as someone else's: kept, and the faults the goldens and the
rules found in them are fixed in new commits on top.

### What the branch holds

- **S2 (No-UI token skeleton):** `kotlin.emit_skeleton` writes a No-UI project's `DesignTokens` with the generic slots
  only (palette roles, type scale, spacing, radii, sizes, elevation, springs, fling decays, fades, defaults), without
  `Sheet`, `Borders`, `Styles`, the motion styles, the tab bar's sizes and springs and the component colour roles;
  the UI theme files carry the S2 fences (`barIndicator`, `skeleton*`, `AppShapes.sheet`, `SurfaceTone`, the five
  component roles), `AppDesignStyle.kt` is option-owned, No-UI has its own `AppMotion`/`AppTypography`/`AppTheme` and
  a skeleton palette test. A UI everything project from the S2 commit is byte-identical to the base's in every theme
  file (`s2-identity-1.log`: only L9's files and the new preview annotation differ). DESIGN-SYSTEM.md's No-UI text says
  how a component adds its own tokens (its `XDefaults` first, the theme only when a second component needs the value).
- **L9:** `UiMessage.id` is a `kotlin.uuid.Uuid` (Android and multiplatform alike, the `sync.toml` difference gone);
  `DefaultAuthRepository` and `KtorNetworkClient` use `Uuid.random()`; `OfflineFirstSampleRepository` keeps its
  reader count and last list in one `MutableStateFlow` changed with `update`. One test per change (`UiMessageTest`,
  the owner and idempotency-key UUID tests, the 2,000-readers test, which fails against an unguarded count:
  `failfirst-l9-1.log`). What stays on the JVM and why is in `decisions/LOG.md` (Oct 25 23:14).
- **F8, UI half:** `preview/AppComponentPreviews.kt` lives in the template for both modes (the variant copy is gone);
  all 79 component files of a UI everything project carry a private `@AppComponentPreviews` preview with their real
  states (`test_android_designsystem.py` checks every component file of the UI everything and lean shapes, and that
  the skeleton holds no sheet, tab-bar or glass token). DESIGN-SYSTEM.md shows the one annotation in both modes.
- **F2 and F15.2, UI half:** every group moved, none left: dimensions into per-component `object XDefaults`
  (PascalCase vals), fades, arrivals, the caret blink, the press dip and the tab bar's fixed springs into `AppSprings`
  and `TabBarSprings` with the same durations, easings, damping and stiffness, the tab labels' `.sp` step onto the
  type scale (`AutoSizeStep`), and the components' whites and blacks onto five component roles (`thumb`, `sheen`,
  `shade`, `mediaBackdrop`, `onMedia`) with today's exact values in `tokens/palette.json`, emitted by `tokens --write`
  (the multiplatform and iOS copies too) and shown on the catalog's Foundations page. No `// allow(raw-…)` marker
  was needed (`raw-color` reads hex literals only); the template's markers stay at 5, within the budget of 10.
- **Fixes the goldens and the rules forced (this run):** `ce5bdccde` (the money field crashed for a locale without a
  region), `e559bc82b` (previews' "Right to left" was rendered left to right), `9a89a1e12` and `ead496b34` (the
  previews broke `sheet-modifier` and `file-length` in every UI project), `9db07352b` (the catalog showed none of the
  five roles, `test_catalog_coverage` failed), `51f085cee` (the UI guide names the five roles).

### Commits (`git log f5a773e74..51f085cee`)

- `aebff213c` WIP (power-cut stop point, Oct 25 10:27): the follow-up batch's lane work in progress
- `d5937e8a6` Keep message ids and the sample's reader count without JVM-only locks
- `0c78f69d1` Give the theme skeleton its own palette test, so UI projects keep theirs byte for byte
- `03869bc42` Preview every design-system component in both themes, at 2x, right to left and narrow
- `d083b697c` Call appAnimateItem as the item scope's modifier factory in its preview
- `0f536aa50` Build the design system's fades, arrivals and tab-bar springs in the theme
- `7dba3b185` Put the selection controls' metrics in their XDefaults
- `2842a5c67` Put the feedback components' metrics in their XDefaults
- `bd7682b36` Put the list rows' and the input fields' metrics in their XDefaults
- `5d01aaacb` Put the form fields' metrics and motion in their XDefaults and the theme
- `0ccb8a3e8` Put the data display and collection components' metrics in their XDefaults
- `7eb4965d2` Put the pickers' and pull to refresh's metrics in their XDefaults
- `200ae9383` Put the overlays' and the sheets' metrics in their XDefaults
- `c4eff0f76` Put the tab bar's, the rail's and the other bars' metrics in their XDefaults
- `f22dae024` Put the foundation primitives' metrics and motion in their XDefaults and the theme
- `c6e338530` Show one preview annotation in the design-system guide, in both modes
- `c9a0c7a6a` Share UiMessage with the multiplatform template again, now that its id needs no counter
- `493cdf939` Give each bar in the tab cells' preview its own measure, so the preview settles
- `f65074e12` Name the dialling-code width after the phone field it belongs to
- `1e0077a22` Draw the components' white and black from colour roles, with today's exact values
- `d3044f71e` Rewrite the website's catalogue for the five component colour roles
- `9db07352b` Show the five component colour roles on the catalog's Foundations page
- `ce5bdccde` Give the money field a currency when the locale names no region
- `9a89a1e12` Keep the sheet previews' footer buttons out of the AppSheet call
- `ead496b34` Split the tab bar's look and the refresh spinner into files of their own
- `e559bc82b` Lay a preview out right to left when its locale reads that way
- `51f085cee` Name the components' colour roles in a UI project's design-system guide

### Where reality differed from the plan, and why (also in `decisions/LOG.md`, Oct 25 23:14)

- L9: a `MutableStateFlow` updated by compare-and-set instead of a `Mutex` (the guarded section includes `peek`, which
  cannot suspend), and its test is the concurrent-readers one, not "two concurrent refreshes" (`refresh` was never
  inside the lock). `UiMessage.id` became a random `Uuid`, not a counter: the standard library's atomics are still
  experimental.
- F2: no `allow(raw-color)` markers; the literals that stay are not colours on screen and do not fire the rule.
- Outside CORE's files, forced by its changes: `templates/multiplatform/core/mvi/.../UiMessage.kt` and
  `templates/multiplatform/sync.toml` (`c9a0c7a6a`, `test_sync`), `web/frontend/lib/catalogue.json` (`d3044f71e`,
  `catalogue --write`). Two more are cross-lane requests to GUARD rather than edits: the No-UI baseline's
  `DesignTokens.kt file-length` line (S2 made the file short, so every No-UI `verifyCodeRules` fails on it until it is
  fenced) and `sheet-modifier` reading nested lambdas.
- The plan's "pixel-identical in RTL and 320 dp" needed the RTL fix first (`e559bc82b`); the goldens were re-recorded
  with it. "Narrow" stays the same 320 dp as Robolectric's default, so its 79 goldens equal the Light ones byte for
  byte: they prove nothing beyond Light.
- Time: recording the 395 goldens takes 3 to 5 minutes, not the 20 first reported (one preview waited out an idle
  timeout).

### Checks

UI everything is the compact everything preset (`com.example.finalcore.uieverything`); every Gradle run went through
`locked gradle`. Earlier runs' logs are named where they are still the evidence.

| Check | Result | Log |
|---|---|---|
| `test_tokens` on the base, before S2 (the skeleton emitter missing) | red | `failfirst-test_tokens-on-base.log` |
| `test_android_designsystem.py` before the previews (component files without one; the skeleton's tokens) | red | `failfirst-test_android_designsystem.log` |
| `test_tokens` before the five roles | red | `failfirst-component-roles.log` |
| `test_catalog_coverage` with the roles and no catalog swatches, then with them | red, then green | `unittest-suite-2.log`, `unittest-catalog-coverage-1.log` |
| `MoneyFieldTest` against the field before `ce5bdccde` ("de": IllegalArgumentException) | red | `failfirst-money-1.log` |
| `AppPreviewThemeTest` against the theme before `e559bc82b` ("ar" laid out left to right) | red | `failfirst-preview-rtl-1.log` |
| the concurrent-readers test against an unguarded reader count | red | `failfirst-l9-1.log` |
| `:data:sample`, `:core:mvi`, `:data:auth`, `:core:network` `testDebugUnitTest` with the code restored (results from the build cache of identical inputs) | pass | `l9-green-1.log` |
| UI everything from the S2 commit against the base, byte for byte: only L9's files and the new preview annotation differ, every theme file is identical | pass | `s2-identity-1.log` |
| (b) `recordRoborazziDebug` from (a) `d083b697c` plus the three preview-forced fixes as patches (`bin/golden-patches/`): 395 goldens (79 previews x 5), 310 s with compilation | pass | `record-b-4.log` |
| (d) `verifyRoborazziDebug` at `e559bc82b` against (b): 395 unchanged, 0 changed, 0 added | pass | `verify-d-1.log`, `verify-d-1-results-summary.json` |
| (d) byte for byte: `recordRoborazziDebug` at `e559bc82b`, every PNG compared with (b)'s: 395 of 395 identical | pass | `bytes-d-1.log` |
| the scratch compliance check (No-UI `ComponentRules` and theming rules over the UI design system, never committed) at `d3044f71e`: `file-length` x2, `sheet-modifier` x2, `designsystem-imports` x2 | red | `failfirst-money-1.log` |
| the same at `e559bc82b`: no `raw-color`, `raw-tween`, `raw-dp`, `raw-type`, `component-preview`, `file-length` or `sheet-modifier` finding; only `designsystem-imports` at `GlassVerdictStore.kt:4` and `DeviceGlassSignals.kt:6` (after-integration step 4 (a)) | as expected | `verify-d-1.log` |
| `:core:designsystem:testDebugUnitTest --rerun`: before the previews 148 s (`narrow1-s2-l9.log`), after 120 s, 831 tests, 0 failures (a plain unit-test run renders no preview; the render cost is in record and verify) | pass | `verify-d-1.log` |
| `:catalog:compileDebugKotlin` at `e559bc82b` | pass | `verify-d-1.log` |
| No-UI everything at `51f085cee`: `:core:designsystem:testDebugUnitTest` (23 tests, `AppPreviewThemeTest` included), `:app:compileDevDebugKotlin`, the L9 modules' tests; `verifyCodeRules` fails only on the stale baseline line (cross-lane request) | pass but that line | `narrow-noui-everything-1.log` |
| No-UI lean at `51f085cee`: the same tasks it has | pass but that line | `narrow-noui-lean-1.log` |
| `verifyCodeRules` in every module of both No-UI projects with that line removed (scratch) | pass | `narrow-noui-rules-2.log` |
| generator suite (`memwait <python> -I -m unittest discover -s tests -t .`) at `51f085cee` | 618 OK, 9 skipped | `unittest-suite-4.log` (`unittest-suite-3.log` at `ead496b34`: 618 OK, 9 skipped) |
| `test_docs`, `test_markers` after the guide's Colour row | pass | `unittest-docs-1.log` |
| `tokens --check` (3 copies current), `catalogue --check` (current) at `e559bc82b` | pass | `checks-tokens-catalogue-3.log` |
| full preset builds | not run (the orchestrator's rule: the integration and the gate build every preset in both modes) | |

### Open

- **Cross-lane, needed before a No-UI build is green:** GUARD fences the baseline's `DesignTokens.kt file-length`
  line `<opt:ui_components>` (request above). Without it every No-UI `verifyCodeRules`, and so `check`, fails on that
  one line after CORE merges.
- GUARD's `sheet-modifier` reads nested lambdas (request above; worked around).
- After-integration step 4 (a): `designsystem-imports` on `GlassVerdictStore.kt` and `DeviceGlassSignals.kt` in a UI
  project (the IO half GUARD listed); everything else the No-UI rules check passes on the UI design system, so step 4
  (b) can roll the theming rules and `component-preview` out to every project with no group left No-UI-only.
- The goldens are not committed anywhere (the plan keeps them out of the template); `ui-golden` keeps them under
  `out/NOUI-FINAL/CORE/ui-golden/core/designsystem/src/test/screenshots` for the integration's own verify, if wanted.
- `AppComponentPreviews`' KDoc, the convention's KDoc and DESIGN-SYSTEM.md say a plain build renders every preview;
  on this branch that is true only under record, compare or verify (CORE's 20:39 request). TOOLS's ATF tester changes
  how previews run; the docs pass (step 7) makes the sentences match whatever lands.
- "Narrow" repeats "Light" in the screenshot tests (both 320 dp); widening Robolectric's default or dropping the
  configuration is a choice for TOOLS or the docs pass, not made here.
- The design system's `baseline-prof.txt` names classes the moves renamed (`AppTabBarKt.barLook`, the new
  `…Defaults` objects): the Profile phase re-records it, as planned.
- `DefaultAuthRepositoryTest` keeps its own `MemoryTokenStore` until the integrator applies TEST's 18:05 request.
- L9's longer list (JVM calls left on purpose) is in `decisions/LOG.md`, Oct 25 23:14.

## TOOLS

Lane TOOLS, `wt/NOUI-FINAL-TOOLS`, branch `staging-wp/NOUI-FINAL-TOOLS` from `1f2fd6c85`. **Done at 23:58 Oct 25
(third run, from 22:43), head `638a3efa6`.** No phone. Logs in `logs/NOUI-FINAL/TOOLS/`, projects in
`out/NOUI-FINAL/TOOLS/` (build output deleted after each check, as approved). Per the orchestrator's 21:0x rule no
full preset `check` ran here: each item is proven by its tests and the narrowest Gradle tasks that cover it.

Commits (the first five were checked again in this run as someone else's and kept):
- `8d99907e1` (T4 prerequisite): the UI app profile names the application's classes in the identity form the renamer
  maps (`BaseAppApplication`, `Hilt_BaseApp...`, `DaggerBaseApp...`, `injectBaseApp...`), 175 rules. `ProfileTest`
  generates "Acme Ui" / `com.acme.ui` and asserts that every rule of the app, navigation, design-system and ui profiles
  names a class the project has, and that the app profile holds `AcmeUiApplication`, its Hilt and Dagger classes and
  `injectAcmeUiApplication`. The Profile phase must re-record in this form; the test keeps it so.
- `997b03265` (push in a UI project without onboarding or settings): `docs/ARCHITECTURE.md`'s push passage,
  `rememberNotificationPermission`'s KDoc and the manifest's comment name only the screens that ask (both, onboarding,
  the notifications row in settings), and with neither say what a No-UI project says (nothing asks yet, and how a
  screen of yours does); the manifest comment was wrong in No-UI projects too. `features/push.toml`'s description
  holds for every selection (No-UI keeps its own `feature_copy.push`); `catalogue.json` by `--write`.
  `PushPermissionTest` generates push alone, with each screen, with both and without UI components, and runs
  `GeneratedDocsTest.test_what_the_docs_say_asks_or_opens_is_what_the_code_does` on them (imported; `test_docs.py`
  untouched).
- `e5d1e5e7c` (harness advice): the overlay's cold-start and baseline-profile advice strings, the perfhud KDocs
  fenced in fix round 2 (FrameIntake, MemoryLog, PerformanceRecorder, OptimisationScore) and `LaunchHook.kt`'s seam
  comment (only its fence; the file is NAV's) name `tools/perf`, `perf.sh` and `gates.toml` only under
  `performance&ui_components`, and otherwise give the No-UI advice
  (Macrobenchmark's `StartupTimingMetric`, the median `TotalTime` of `adb shell am start -W`,
  `cmd package compile -f -m speed-profile`). `HarnessAdviceTest` is the UI twin of the No-UI harness test over lean,
  standard and everything without `performance`, compact and modular; six files outside TOOLS are on its
  `HARNESS_PENDING` list and in the cross-lane requests (AppRoot.kt is fenced at integration, NAV's item 11).
- `2cd20428d`, `febf31200` (F15.4): `generator/add_component.py <Name> --project <dir> [--group g] [--dry-run]` writes
  `App<Name>` and `App<Name>Test` into `:core:designsystem` from the project's own `component-template` and
  `component-test-template` blocks, with only the imports they use, in both modes. It refuses, before writing
  anything, a name that is not PascalCase (and says the name to use for `AppX`), a name the design system declares, a
  project without `:core:designsystem`, a bad `--group`, and a template naming something it cannot import.
  `test_add_component.py` (12 tests). The root README ("Adding a design-system component", the file list), root
  AGENTS.md and DESIGN-SYSTEM.md's Templates line name it; `febf31200` says the doc's templates are build-checked only
  where `DesignSystemDocTest` runs (No-UI). The template README is left to the docs pass; the matrix row is a
  cross-lane request.
- `796d3e31d` (F14): `AccessibilityPreviewTester` in `:core:testing:compose`, named by the screenshots convention
  (`testerQualifiedClassName`, with `useScanOptionParametersInTester`), runs ATF's `LATEST` preset through
  `roborazzi-accessibility-check` on every window a preview puts on screen, before any image is taken: ERROR results
  fail the test, warnings are printed, suppressions only through the tester's reviewed `SUPPRESSIONS` (matcher and
  reason; empty). It composes the preview itself in the test rule's activity when no Roborazzi task is on, so
  `./gradlew check` renders and audits every preview (CORE's 20:39 note: Roborazzi 1.76.0 rendered nothing in a plain
  run, which left the convention's "the unit tests still render every preview" untrue until now). Catalog:
  `roborazzi-accessibility-check` (version.ref roborazzi) and `accessibility-test-framework` 4.1.1
  (`accessibilityTestFramework`, the newest on Google Maven). The convention's KDoc, DESIGN-SYSTEM.md ("Previews are
  render tests") and ARCHITECTURE.md's Testing list say what fails and what prints. `PreviewTesterTest` checks that
  every generated shape's convention names a tester its `:core:testing:compose` declares.
- `638a3efa6` (F14): the audit is an internal function the tester calls, and `:core:testing:compose`'s own
  `AccessibilityAuditTest` (both modes, native graphics as the convention sets) holds it: a 20 dp control without a
  label must fail it, a labelled 48 dp one must pass. Without it, a change that stopped the audit failing would pass
  every build that has no finding.

### Where reality differed from the plan, and why (also in `decisions/LOG.md`, Oct 25 23:08)

- **Contrast prints; it does not fail.** ATF measures a Compose text's contrast from the drawn pixels (its heavyweight
  path) and reports that only as a WARNING; `ImageContrastCheck` and `DuplicateSpeakableTextCheck` are warnings too
  (bytecode of ATF 4.1.1). So under the plan's rule (ERROR fails, warnings print) the errors are touch targets, missing
  labels and two controls on one spot, while contrast and duplicate descriptions print, and the docs say exactly that.
  Exact contrast stays with `PaletteContrastTest` (every colour-role pair at WCAG AA, in both modes). Failing on
  warnings was rejected: the pixel heuristic misfires on small anti-aliased text (No-UI's `ThemeSlotsPreview`
  "footnote", drawn in `contentPrimary`, measured 2.83:1) and would also fail ATF's ClassName and TraversalOrder
  warnings. If the user wants contrast to fail anyway, it is a per-check escalation inside the tester.
- **Every `check` now pays for rendering every preview.** Measured here: 0.5-2 s per preview configuration after
  Robolectric's start (about 20 s, once per module). After CORE's merge the design system has about 79 component
  previews x 5 configurations; CORE measured about 20 minutes to record them on this laptop, and rendering with the
  audit costs about the same, so a UI project's `check` grows by up to that much. The gate's time budget should allow
  for it.
- Roborazzi's fallback activity is never used by a generated test (it always has its rule's), so the tester errors
  there instead of launching one, which also avoided applying the preview's setup twice.

### ATF findings in other lanes' code (after-integration step 4; none hidden, none suppressed)

From `:catalog`, `:core:designsystem` and every feature's `testDebugUnitTest` in a UI everything compact project on
this branch (`f14-ui-everything-previews-1.log`; summary `f14-ui-everything-findings-1.txt`; the result XMLs in
`f14-ui-everything-results/`). CORE's component previews are not on this branch: they are first audited at
integration, so the integration's UI builds will name more.
1. **The sheet's grabber is a touch target under 48 dp** (TouchTargetSizeCheck, ERROR): `:catalog`
   `SheetPreviewsKt_TwoDetentSheetPreview` (37 dp) and `SheetPreviewsKt_PersistentSheetPreview` in all seven of its
   configurations (35-43 dp). The view is `sheet_grabber`,
   `core/designsystem/.../component/sheet/chrome/SheetGrabberNode.kt` (CORE's design system): its draggable area
   needs the theme's `minTouchTarget` height.
2. **A text field with an empty value and no placeholder has no speakable label** (SpeakableTextPresentCheck,
   ERROR): `:feature:auth` `SignUpKt_SignUpPreview`, the "Name" `AppTextField` (bounds [16,16][304,87]); the email
   field, which has a placeholder, passes. The fix belongs in `AppTextField` (CORE's): its `label` should be the
   field's semantic label, so a labelled field passes whatever its value.
3. Printed warnings, for the record: `:feature:auth` text contrast on "Create account", "Send reset link", "Sign in"
   and "or" (pixel heuristic) and one DuplicateSpeakableText; No-UI's `ThemeSlotsPreview` "footnote" (the misfire
   above).

`:feature:sample` (10 preview tests), `:feature:onboarding`, `:feature:settings`, UI lean's `:feature:home` (14) and
the `AppTag` previews in both modes have no ATF result; `:feature:feed`, `:search` and `:profile` have no previews.

### Checks

| Check | Result | Log |
|---|---|---|
| Profile test on the old UI profile (base `1f2fd6c85`) | red, 5 failures | `profile-red-base.log` |
| Push claims test on the base | red, 14 failures | `push-red-base.log` |
| Harness test on the base | red, 7 failures | `harness-red-base.log` |
| `test_add_component` on the base (no module) | red | `add-component-red-base.log` |
| `PreviewTesterTest` on the base | red | `f14-tester-test-red-base.log` |
| Scratch preview in No-UI lean (a 20 dp clickable without a label, grey-on-grey text), plain `:core:designsystem:testDebugUnitTest` | red: AccessibilityViewCheckException, TouchTargetSize and SpeakableTextPresent ERRORs; the text a TextContrast WARNING | `f14-scratch-red-3.log` |
| The same under `recordRoborazziDebug`, with a text-only grey-on-grey scratch beside it | red on the clickable; the text-only scratch passes with a WARNING (the finding behind the contrast decision) | `f14-scratch-red-4.log` |
| No-UI lean, regenerated without the scratch, + `add_component Tag`: `:core:designsystem:verifyCodeRules :core:designsystem:testDebugUnitTest :core:testing:compose:verifyCodeRules :core:testing:compose:testDebugUnitTest` | pass: 7 preview tests (AppTag x5, ThemeSlots x2), AppTagTest 3 | `gen-noui-lean-5.log`, `add-component-noui-lean-1.log`, `f14-noui-lean-green-1.log` |
| UI lean + `add_component Tag`: `:feature:home:testDebugUnitTest :core:designsystem:verifyCodeRules :core:designsystem:testDebugUnitTest` | pass: home 14 preview tests, AppTag 4, AppTagTest 3 | `gen-ui-lean-2.log`, `add-component-ui-lean-1.log`, `f14-ui-lean-narrow-1.log` |
| UI everything compact, every preview-tested module's `testDebugUnitTest` | red only on the two findings above (catalog 8 of 21, auth 1 of 3) | `f14-ui-everything-previews-1.log` |
| UI standard (perfhud without performance): `:core:perfhud:testDebugUnitTest :core:perfhud:lintDebug :app:compileDevDebugKotlin` | pass | `harness-ui-standard-narrow-1.log` |
| `AccessibilityAuditTest` with the generated tester's failure level mutated to LogOnly (No-UI lean copy only) | red: "a control under 48 dp without a label fails" | `gen-noui-lean-6.log`, `f14-audit-test-red-1.log` |
| The same project as generated: `:core:testing:compose:verifyCodeRules :core:testing:compose:testDebugUnitTest :core:designsystem:testDebugUnitTest` | pass | `f14-audit-test-green-1.log` |
| `test_android_truth` + `test_docs` after the last wording change | pass, 25 | `docs-5.log` |
| Generator suite at `796d3e31d` | pass: 629, 9 skipped | `generator-suite-2.log` |
| Generator suite with `638a3efa6`'s change (then uncommitted) | pass: 629, 9 skipped | `generator-suite-3.log` |
| `catalogue --check` | current | `generator-suite-3.log` (end) |

### Open

- Not run here, by the orchestrator's rule: UI and No-UI everything `check assembleDevDebug`, UI lean `check`, and
  No-UI lean and UI lean `check` after `add_component Tag` (their narrow parts above passed). The integration and the
  gate build them; the Spotless carry-over applies as before.
- After-integration step 4: the two ATF findings above, and whatever CORE's component previews show at integration.
- Cross-lane requests: the six `HARNESS_PENDING` files; the matrix row for `add_component.py`; the UI app profile's
  rules for NAV's moved classes (`ProfileTest` fails once NAV and TOOLS are merged, until they are renamed);
  dependency-guard, if it baselines a test classpath that reaches `:core:testing:compose`.
- The time every UI `check` now spends rendering previews (above), for the gate's schedule.

## GUARD

Lane GUARD, `wt/NOUI-FINAL-GUARD`, branch `staging-wp/NOUI-FINAL-GUARD` from `1f2fd6c85`. **Done (00:3x Oct 26, third
run), head `239e51a7f`.** The first two runs' commits and uncommitted work were checked as someone else's: the three
commits and the F12, L8 and F13 work-in-progress were right and are kept; two of the second run's proofs proved nothing
and were redone (below). Everything of plan 3.6 is in; what waits for after-integration step 4 is listed under Open.
**Fourth run (06:50 Oct 26): no change.** The branch was still at `239e51a7f` with a clean tree, that head is merged in
`staging-wp/NOUI-FINAL` (`43a2462ae`), and the after-integration steps since (1, 4 (a)-(c), 5) already did this
section's Open items; a new commit here would never reach the integration, so none was made and no build ran (the
AFTER phase holds the Gradle lock). The cited logs were re-read: each shows the result this section states.

### What it did (commits on `1f2fd6c85`)

| Commit | Item | What |
|---|---|---|
| `619045b3f` | F15.1, F6, F7, F11; NAV 1, 4 | `ComponentRules.kt` and its cases test moved from `generator/variants/noui/` into the template. Live in both modes: `designsystem-imports` (core list), `stateless-params`, `raw-type` (features, `core:ui`), `modifier-unused`, `composition-read`, `feature-composition-local`, `empty-description`, `navigation-host-api` (NAV's request 4; inert until `@NavigationHostApi` exists); `lazy-key`, `lazy-content-type`, `collect-as-state`, `subcompose-image` over the design system too; `inNavigation` covers `core/navigation/shell` (NAV's request 1); `verifyCodeRules` of `:core:designsystem` also before `preBuild` (F15.3). No-UI only until step 4: `offscreen-layer`, the IO half of `designsystem-imports`, `feature-primitive`, `component-preview`, the theming rules over the design system. DESIGN-SYSTEM.md's rule index lists what each mode runs. |
| `8e5d60689` | F10, F1.4 | Each code-rule baseline line in the region of what it accepts; the stability baseline fenced the same way; `checkComposeStability` fails on an entry that matches nothing (it found `AppSwipeToAction(resetKey)`, gone; deleted). `generator/tests/test_android_guards.py`: every preset in both architectures plus a feature-modules-only project, with and without UI components: every baseline path exists, every stability entry is declared, every name an active rule's advice cites is declared (F1.4, sharing `test_android_noui.py`'s helper and library list; the No-UI test stays). |
| `5f70f202a` | F4 (3); NAV 4; L8 | `ModuleGraph.kt`: a `DESIGN_SYSTEM` kind (only `:core:testing:*`, and libraries from an allowlist with a reason per line: Compose, `androidx.window`, `navigationevent`, `lifecycle-runtime-compose`, `activity-compose` for the test activity, Kyant, the stdlib, the screenshot test stack with Roborazzi's accessibility check and ATF); no feature, data or domain module on `:core:navigation:shell`; the runtime never on the shell (and, without UI components, never on the design system); declared dependencies read through a provider, no `afterEvaluate`. |
| `98fb030fa` | F12 | All ten Compose lint ids as errors (each passes on UI everything); `UnrememberedState` and `ExitAwaitPointerEventScope`, which lint reported as unknown ids, removed. |
| `3b22d4592` | L8 | Kover and dependency analysis applied by the application, library and JVM conventions (`feedProjectReports`), not by the root's `subprojects { }`; `isolated.rootProject` instead of `rootProject` in `Compose.kt`, `ComposeStability.kt`, `CodeRules.kt`, `ManifestRules.kt`, `Signing.kt`; the root lists the modules for merged coverage by path, without `:benchmark`. Isolated Projects stays off by default. |
| `f08c8dbd9` | L8 | dependency-guard 0.5.0 (newest) on `:app`'s `prodReleaseRuntimeClasspath` from the flavours convention; `check` runs `dependencyGuard`; the project CI warns until `app/dependencies/` is committed; a row in `docs/ARCHITECTURE.md`'s guard table. |
| `3eb817849` | F13 | Spotless in every project with the Compose Rules for ktlint 0.6.8 (newest; built on ktlint 1.8.0); `ktlint_standard = disabled` without `formatting` (Gradle files only with it); `.editorconfig` turns off, each with its reason, the rules the build's own checks cover, those this design differs on, and those waiting for step 4 (sites under Open). |
| `087a8f804` | F4 (3) | The design system's Compose allowlist names Compose's groups instead of `androidx.compose*:*`, which let Material in. |
| `ce3e383e4` | F15.3; NAV 3 | `checkComposeStability` over every feature module (`:feature:x`, `:feature:x:impl`) after measuring (under 2 % of a `check`); `:core:navigation:shell` in `STABILITY_CHECKED_MODULES`. |
| `f5fe3b202` | F13 | Two rules the full run found once the others were off: `modifier-without-default-check` off in every project (No-UI's `AdaptiveLayout` too), `content-trailing-lambda` off with UI components. |
| `239e51a7f` | CORE's request | `sheet-modifier` judges the `AppSheet` call's own `modifier =`, not one a slot's lambda passes to its content. |

### Where reality differed from the plan, and why (also in `decisions/LOG.md`, Oct 26 00:3x)

- **Two proofs of the second run proved nothing:** its changed-dependency run added `androidx.window:window-core`, which the
  No-UI lean release already ships, so `dependencyGuard` passed; its stability measurement found every compile up to
  date. Both redone (`v6`, `f153-r*`).
- **F12:** besides the ten ids, two ids lint reported as `UnknownIssueId` left the list; they raised nothing and the code
  said every id had been checked against the jars.
- **F4 (3):** `androidx.compose*:*` let Material into the design system's allowlist; it names Compose's groups now.
- **L8:** `:benchmark` applies no convention plugin, so the root's merged coverage leaves it out by path (no unit tests,
  runs on a phone) and dependency analysis no longer covers it. dependency-analysis' root still depends on every
  project, including `:benchmark` and the folder projects `:core`, `:data`, `:feature` that have no build file and so
  no plugin now: `:dependencies` shows them FAILED, but its artifact resolver (`variant-artifacts`' `Resolver`) is
  lenient, and `buildHealth` passes and writes its report over every other module (`v10`). On UI everything
  `buildHealth` ran the daemon out of JVM Metaspace (1 GiB) after seven minutes (`v7`), a memory limit of this laptop,
  not a resolution error; heap and metaspace stay as `D:\.caches\gradle\gradle.properties` sets them.
  dependency-guard needs no first-run step (the plan's fallback): it writes a missing list and passes.
- **F13:** Spotless lists a step's lints in place of its format changes, so the first discovery hid the
  auto-correcting `modifier-without-default-check`; the full run found it in No-UI's `AdaptiveLayout`, and
  `content-trailing-lambda` in two UI sheet files (`f5fe3b202`). Compose Rules runs every rule not turned off, so `.editorconfig` lists only what is off, each with a reason;
  of the ten rules the plan named, `modifier-reused-check`, `content-emitter-returning-values-check`,
  `defaults-visibility`, `remember-missing-check` and `mutable-params-check` run in every project now;
  `modifier-missing-check` and `multiple-emitters-check` wait for step 4 in both modes (No-UI code has findings too:
  `AppRoot`, the developer panel, the overlay's detail sections), `modifier-not-used-at-root`, `param-order-check` and
  `lambda-param-in-effect` with UI components only. `ktlint_standard = disabled` is in `.editorconfig` (fenced
  `<opt:!formatting>`) rather than Spotless's override, so the IDE and the build read one file; that put `.editorconfig`
  in `template.toml`'s marker-syntax table (outside this lane's files, one key; cross-lane note).
- **F15.3:** stability on features adopted: the four alternating runs differ by less than equal runs do, and the largest
  pairing is under 2 % of a UI everything `check`; every feature, the scaffolded one included, passes as generated.
- **Docs:** CLAUDE.md is at 119 of its 120-line budget in a No-UI everything project, so the new checks are in
  `docs/ARCHITECTURE.md`'s guard table; AGENTS.md, CLAUDE.md and README.md are the docs pass's (cross-lane request).

### Checks

Logs under `logs/NOUI-FINAL/GUARD/`; projects under `out/NOUI-FINAL/GUARD/` (their build output deleted after each
check, as approved at 18:5x). The `v*` projects were generated from this worktree between `ce3e383e4` and `239e51a7f`;
`vui-ev` and `vno-ev` are everything compact plus a scaffolded `:feature:orders`.

- **Seen failing first:** `red-1-*-buildlogic-test.log` (graph tests, against the base graph), `red-2-*-buildlogic-test.log`
  (every new rule's cases), `red-guards-test.log` (`test_android_guards.py` against the unfenced baselines),
  `red-3-every-project-tools.log` (the Spotless, Compose Rules and dependency-guard tests, against the lane's base),
  `red-4-ui-modulegraph-material.log` (Material in the allowlist), `red-5-ui-stability-features.log` (features'
  stability; a compile error against the old code), `red-6-ui-sheet-modifier-slots.log` (a slot's `modifier =`).
- **Build-logic tests:** UI everything 98 of 98 (`v1-ui-ev-narrow.log`) and, at the head with the sheet case, 99 of 99
  (`v9-ui-ev-buildlogic-test.log`); No-UI everything 101 of 101 (`v1-noui-ev-narrow.log`).
- **`verifyCodeRules`, `verifyModuleDependencies`, `checkComposeStability`** (the core modules and all eight features):
  green in UI and No-UI everything (`v1-*-narrow.log`).
- **Compose Rules** (Spotless with a scratch `ktlint_standard = disabled`, so the formatting carry-over cannot hide a
  finding): UI everything green (`v7-ui-ev-compose-rules-and-buildHealth.log`, `:spotlessKotlinCheck` passed), No-UI
  everything green (`v8-noui-ev-compose-rules.log`). As generated, without `formatting`: `spotlessCheck` green on UI lean
  (`v3-ui-lean-spotlessCheck.log`) and No-UI lean (`v4-noui-lean-spotless-depguard-first.log`). A scratch composable
  taking a `MutableList` fails `spotlessCheck` with `compose:mutable-params-check`
  (`v6-noui-lean-changed-dep-and-compose-finding.log`).
- **dependency-guard** (No-UI lean): the first run without a list prints "Dependency Guard baseline created", passes and
  writes `app/dependencies/prodReleaseRuntimeClasspath.txt` (`v4`); the second run passes (`v5`); with
  `implementation(libs.junit)` added it fails with the diff (`+ junit:junit:4.13.2`, `+ org.hamcrest:hamcrest-core:1.3`)
  and the re-baseline command (`v6`); `check --dry-run` runs `:app:dependencyGuard` (`l8-5-depguard-check-wiring.log`).
- **Isolated Projects** (UI everything): `./gradlew help --configuration-cache -Dorg.gradle.unsafe.isolated-projects=true`
  succeeds and stores the entry with no problem, none from third-party plugins either (`v2-ui-ev-isolated-projects.log`);
  before L8 it failed on the root's `subprojects { apply }` (`ip-1-ui-before.log`).
- **Merged coverage without `:benchmark`:** the root's `koverExternalArtifacts` resolves every module but the benchmarks
  (`v1-ui-ev-narrow.log`, `:dependencies`). **`buildHealth`** passes on a No-UI lean project with dependency analysis
  and the benchmarks (`v10-noui-dagp-buildHealth.log`); on UI everything the daemon ran out of Metaspace (above).
- **F12 lint:** `:app:lintDevDebug` (checkDependencies) and `:catalog:lintDebug` on UI everything with the ten ids as
  errors: green (`f12-1-ui-ev-lint.log`, the second run's, kept: the ten ids are unchanged since).
- **F15.3 measurement:** `f153-r0-warm.log` to `f153-r4-with.log`.
- **Generator suite** at `239e51a7f`, clean tree: 617 tests OK, 9 skipped (`gen-suite-3.log`).
- **Not run here, on the orchestrator's word (21:0x):** the full `check assembleDevDebug` of UI and No-UI everything and
  lean; the integration and the gate build every preset in both modes. Lint with the new ids has run on UI everything
  only; No-UI code meets it first in those builds.

### Open

- **After-integration step 4 (a):** `offscreen-layer` with reasoned markers at `ScrollEdge.kt` (two),
  `PresentationHost.kt` and `StackLayout.kt`; the IO half of `designsystem-imports` with `GlassVerdictStore`'s marker;
  `feature-primitive` with `AuthFrame.kt:105`; then their `<opt:!ui_components>` fences in `ComponentRules.kt` and its
  cases test go. **(b)** the theming rules and `component-preview` over the design system (CORE's groups). **(d)**
  stability on features: nothing left, adopted here.
- **After-integration step 4 (c), the Compose Rules left off:**
  **Compose Rules left off until after-integration step 4 (c) fixes their sites** (from `f13-2-ui-ev-compose-discovery.log`,
  `f13-1-ui-ev-compose-discovery.log` and `f13-1-noui-ev-compose-discovery.log`, every rule on; paths under
  `core/designsystem/src/main/kotlin/<pkg>/core/designsystem/` unless named, lines as generated from `1f2fd6c85` plus this
  lane; NAV moves the shell files). Switch each on in `.editorconfig` once its sites are fixed or carry a reasoned
  `@Suppress("compose:<rule>")`:
  - Every project: `modifier-missing-check`: `app/.../ui/AppRoot.kt:51` (No-UI lean `:41`); with UI also
    `catalog/.../CatalogApp.kt:68` and the catalog sections `CatalogBars.kt:42`, `CatalogButtons.kt:22`,
    `CatalogCollections.kt:31`, `CatalogContainers.kt:33`, `CatalogData.kt:30`, `CatalogFeedback.kt:48`, `CatalogFeel.kt:53`,
    `CatalogFoundations.kt:47`, `CatalogLists.kt:44`, `CatalogLoading.kt:46`, `CatalogMotion.kt:57`,
    `CatalogNavigation.kt:59`, `CatalogOverlays.kt:30`, `CatalogText.kt:35`, and the test screen
    `core/designsystem/src/test/.../sheet/SheetTestScreen.kt:48`. `multiple-emitters-check`:
    `core/devtools/.../panel/DevToolsPanel.kt:102`, `core/perfhud/.../panel/DetailSections.kt:37`; with UI also
    `catalog/.../sections/sheets/CatalogSheets.kt:71`. `content-slot-reused`: `foundation/adaptive/AdaptiveLayout.kt:60`;
    with UI also `core/navigation/.../stack/EntryFrame.kt:27` and `core/designsystem/src/test/.../sheet/SheetScreenshotTest.kt:322`.
    `modifier-without-default-check` (auto-correcting: a private or internal part's `modifier` without `= Modifier`):
    `foundation/adaptive/AdaptiveLayout.kt:102` (`v1-noui-ev-narrow.log`); with UI also
    `component/formfields/AppStepIndicator.kt` (`ConnectorHalf`), `component/list/AppSection.kt` (`SeparatedColumn`),
    `component/navigation/AppTabBar.kt` (`BarSurface`), `component/navigation/TabCells.kt`, `component/navigation/TabRail.kt`,
    `core/ui/.../paging/AppPagingList.kt`, `feature/auth/.../AuthFrame.kt`.
  - With UI components only: `modifier-not-used-at-root`: `component/overlay/AppAlert.kt:89`,
    `component/sheet/action/ActionCard.kt:87`, `component/sheet/layout/SheetLayout.kt:87`. `modifier-naming`:
    `component/list/AppRow.kt:209`. `param-order-check`: `component/feedback/AppSkeleton.kt:77`. `content-trailing-lambda`:
    `component/sheet/AppSheet.kt:114`, `component/sheet/layout/SheetSession.kt:72` (`v1-ui-ev-narrow.log`). `parameter-naming` (past-tense event lambdas): `component/formfields/AppOtpField.kt:69`,
    `component/pickers/AppDatePicker.kt:62`, `component/sheet/AppSheet.kt:69`, `component/sheet/layout/SheetSession.kt:67`.
    `lambda-param-event-trailing`: `component/navigation/AppSidebar.kt:179`. `lambda-param-in-effect`:
    `component/overlay/AppToast.kt:182`, `core/navigation/.../presentation/PresentationHost.kt:176`,
    `core/navigation/.../shell/tabs/TabWarmUp.kt:68`. `state-param-check`: `core/navigation/.../shell/tabs/KeptTabs.kt:122`,
    `core/navigation/.../transition/SceneMotion.kt:35`.
- **Cross-lane requests** (in that section): the CLI's next step for `app/dependencies/`; NAV's baseline paths, stability
  names and the `NAVIGATION_HIDDEN` fence once NAV is merged; CORE's `DesignTokens.kt` baseline fence once CORE is
  merged; the docs pass's AGENTS.md, CLAUDE.md and README.md lines.
- **The first `check` of every generated project writes `app/dependencies/`** (gate note): the gate's logs should show
  "Dependency Guard baseline created" once per project.
- **`buildHealth` on UI everything** needs more Metaspace than this laptop's daemon has; the user's call whether that
  matters (it is advice, not a gate, and runs only when asked).
- **One slip, no effect:** at 22:55 a bare `python -` (the install manager's alias) ran once in this lane's shell, with the
  full environment; it waited on standard input and was stopped (this lane's own process). It created no file, folder or
  registration (`git status --ignored` and the process list checked).

## Integration

Integrator, `wt/NOUI-FINAL`, branch `staging-wp/NOUI-FINAL`, created at 00:35 Oct 26 from `staging-wp/NOUI` (`db2617930`,
which already contains `staging-wp/NOUI-FIX` `7779aab27`, so no separate FIX merge). **Done at 02:00, head `585542469`.**
Nothing pushed, `main` untouched, no branch or worktree deleted, no stash, no history rewritten; `wt/NOUI` and
`wt/NOUI-FIX` untouched. Logs in `logs/NOUI-FINAL/INTEG/`, projects in `out/NOUI-FINAL/INTEG/` (sources only: build,
`.gradle` and `.kotlin` deleted after each result was recorded, as approved). D: free: 82 GB at the start, 79 GB at
01:52. **`## TEST` is missing from this file** (TEST's returned result says it wrote one; a later lane's write
probably replaced the file): TEST's commits, checks and open items are in its returned result and in
`decisions/LOG.md` (Oct 25 18:15, 22:59); its open items are repeated under Open below.

### Merges (`git merge --no-ff`, in the plan's order)

| Merge | Lane head | Conflicts and how they were resolved |
|---|---|---|
| `231dd5801` WEB | `36f42853a` | none |
| `de7a8a97c` CORE | `51f085cee` | `web/frontend/lib/catalogue.json` (emitted): rewritten with `catalogue --write`, never hand-merged; `tokens --check` current |
| `39002236a` TEST | `1f20d595e` | `templates/multiplatform/sync.toml`: TEST's `MviViewModel.kt` allowance kept, CORE's deleted `UiMessage.kt` allowance left out, as TEST asked; `test_sync` green. `KtorNetworkClientTest.kt` and `features/forms.toml` merged clean (read: TEST's `testDispatchers()` beside CORE's Uuid change) |
| `72a3a3947` NAV | `3595e3d27` | `docs/ARCHITECTURE.md` module table: NAV's runtime and shell rows (by mode) with TEST's `:core:testing` row (`testDispatchers()`, `FakeSessionController`). NAV's 68 renames overlap no file TEST or CORE changed |
| `214e7dc0a` TOOLS | `638a3efa6` | `catalogue.json` again: `catalogue --write` (push's new description) |
| `43a2462ae` GUARD | `239e51a7f` | none (`libs.versions.toml` merged clean: GUARD's composeRules and dependencyGuard beside TOOLS's ATF lines) |

### What the merged tree needed (one commit each)

- `a1429a444` + `676ae2b1d`: the guards and docs follow NAV's split (NAV 1-3 and 10, GUARD 2 and 5, CORE 23:35):
  the four `config/code-rules-baseline.txt` lines and two stability entries at `core/navigation/shell`; the runtime
  never sees the design system in either mode (`NAVIGATION_HIDDEN`, its advice and `ModuleGraphTest` unfenced);
  `DesignTokens.kt file-length` fenced `<opt:ui_components>`; the `ComposeRules.kt` comment; `:core:navigation:shell`
  (and every feature module) named where `checkComposeStability` is described; "Only `:app` depends on the shell" where
  a project has no catalog. (`a1429a444`'s message names the ARCHITECTURE.md and README.md lines, but its staging left
  them out; `676ae2b1d` carries them and says so. No amend: history is not rewritten.)
- `2f572e71e`: `DefaultAuthRepositoryTest` uses the shared `FakeAuthTokenStore` (TEST 18:05 (b)).
- `541812d98`: the UI app profile names NAV's moved classes in the identity form; the navKeys providers' rules are gone
  (TOOLS 23:05). `ProfileTest` green.
- `a74c97de5`: the six `HARNESS_PENDING` files fenced on `performance` (AppRoot.kt included, NAV 11); the list is
  gone. The harness pattern stops at a word boundary: `perf.shell` (the dev log tag) matched `perf.sh`.
- `3c01d5a77`: README "Adding a feature" (two things; `entry<K>` records the serializer) and `AppNavKey`'s KDoc (NAV 5,
  6); `sync.toml` allows the multiplatform copy's navKeys wording until its own pass (59 (j)).
- `0ecb71941`: README's flavours sentence fenced on `catalog` (orchestrator item 3; `UI_STALE_MODULES` removed from
  `test_docs.py`), TEST's made-up `:api` module named "api module", and the destination rule in CLAUDE.md/AGENTS.md in
  three lines: TEST's and NAV's one line each had taken No-UI CLAUDE.md to 121 of 120.
- `776a3cd7f`: the languages texts (orchestrator items 1 and 2): `[field_copy].languages` in the option
  (`catalogue --write`), shown by the site's `languagesHint` and the wizard's `ask_languages` with the box ticked; the
  ARCHITECTURE.md AppLocales line is UI-only, with a No-UI line true of the code. Seen red first: the site's copy test
  (`failfirst-site-languages.log`, 2 of 8 failing on "language picker") and `test_prompts` (`failfirst-prompts-languages.log`).
- `df73ab1bf`: the CLI's next steps say to commit `app/dependencies/` (GUARD 1; `test_cli` red first).
- `64d9f56aa`, `585542469`: `add_component.py` in the consistency matrix (quick and full: `component-everything`,
  `noui-component-everything`) and in CI's everything rows (TOOLS F15.4). The matrix's No-UI ownership checks skip only
  files the run added itself (a No-UI developer builds components in that package; the guide says so). Tests red first.
- `3d0c33073`: the catalogue's contract test names the new described field.
- `170f895cf`: **found only by the merge**: GUARD's Compose Rules (`compose:defaults-visibility`, live in every
  project) failed UI projects on CORE's three internal `Defaults` objects of public components (`AppMenu`, `AppCheckbox`,
  `AppRadio`); they are public, as the other 46 are.
- `0260b8284`: an unused `AppResult` import TEST's fixture move left in `SearchViewModelTest` (the matrix found it).

Not done here, on the plan's schedule: the No-UI code comments that describe the shell (`StartupViewModel.kt`,
`SignIn.kt`, `Onboarding.kt`, `OnboardingNavigation.kt`, `Shortcuts.kt`, `AppBrowser.kt`, the `settings.gradle.kts`
header) are after-integration step 6, after step 2 splits the screen files; the AGENTS/CLAUDE/README lines for Compose
Rules and dependency-guard are the docs pass (step 7).

### Checks (logs in `logs/NOUI-FINAL/INTEG/`)

| Check | Result | Log |
|---|---|---|
| Generator suite right after the six merges (before any fix) | 32 failures: baselines and stability names (NAV's paths), ProfileTest, CLAUDE.md 121 lines, `:catalog`/`:api` in docs; all fixed above | `unittest-1.log` |
| Generator suite at the final head (`memwait <python> -I -m unittest discover -s tests -t .`) | 664 tests OK, 9 skipped | `unittest-3.log` |
| `catalogue --check`, `tokens --check`, catalogue JSON valid | current, 3 copies current, valid | `checks-catalogue-tokens-1.log` |
| `tests.consistency --quick` | first run: 19 new issues (the unused import in every search project; the No-UI component run's ownership findings), both fixed; then 83 projects, 0 new issues, 190 known, CONSISTENCY OK | `consistency-quick-1.log`, `consistency-quick-2.log` |
| No-UI everything compact + `add_component Tag` (`com.example.finalinteg.noui`, at `64d9f56aa`): `check assembleDevDebug assembleProdRelease koverVerify --continue` | 12 m 18 s; every task passes except `:spotlessKotlinCheck` and `:spotlessKotlinGradleCheck` (format diffs only: the carry-over, step 8); 598 unit tests, 0 failures; both APKs; koverVerify ran; "Dependency Guard baseline created" | `build-noui-everything-1.log` |
| UI everything compact + `add_component Tag` (`com.example.finalinteg.ui`, at `64d9f56aa`): `check assembleDevDebug assembleProdRelease --continue` | 15 m 22 s; fails only `:catalog`, `:core:designsystem`, `:feature:auth` `testDebugUnitTest` (ATF findings, below) and the two Spotless tasks (format diffs); 1555 unit tests, 68 failures, every one an `AccessibilityViewCheckException`; lint (32 tasks), every guard, stability, both APKs pass; "Dependency Guard baseline created" | `build-ui-everything-1.log`, `ui-everything-test-summary-1.txt`, `ui-everything-atf-details-1.txt` |
| The same at `170f895cf` (`ui-everything-2`) | the same 68 findings, nothing else; its `:spotlessKotlinCheck` passed only because the daemon's ktlint kept the scratch `.editorconfig` of the run before (see pitfall); as generated in a fresh path (`ui-everything-3`) it fails on format diffs only (365 files) | `build-ui-everything-2.log`, `ui-everything-test-summary-2.txt`, `spotless-ui-everything-3.log` |
| Compose Rules alone (scratch `ktlint_standard = disabled`, never committed), so the carry-over hides nothing | No-UI: pass. UI: 3 `defaults-visibility` errors, then pass after `170f895cf` | `compose-rules-noui-everything-1.log`, `compose-rules-ui-everything-1.log`, `-2.log` |
| Site: `npm ci`, `npm run typecheck`, `npm test` | pass, 263 tests | `npm-ci-1.log`, `site-typecheck-test-1.log` |
| API: `gofmt -l` (empty), `go vet`, `go build`, `go test ./... -count=1` | pass, 7 packages (Smart App Control let the binaries run) | `go-checks-1.log` |
| Private names in the integration's diff and messages (`db2617930..HEAD`) | none | (command in this run) |

**ATF findings in the merged UI build (after-integration step 4 (e); none suppressed):** TouchTargetSizeCheck on the
sheet grabber (`sheet_grabber`, 35-44 dp: catalog `TwoDetentSheetPreview`, `PersistentSheetPreview`, design system
`AppChoiceSheetPreview`), the sheet scrim (`sheet_scrim`, 10 dp: `AppSheetPreview`, `AppSheetCloseButtonPreview`,
`SheetColumnPreview`, `SheetLayoutPreview`), `AppButtonPreview` (two 12-22 dp wide targets), `AppSelectFieldPreview`
and `AppTextFieldPreview` (36 dp high fields), `AppTagFieldPreview` (35 dp wide), `AppDecimalFieldsPreview` (21 dp
high target, plus a field with no speakable label), `AppRowPreview` (47 dp and 24 dp rows), `AppSwipeActionsPreview`
(24 dp wide actions), `AppDatePickerPreview` (41x44 dp day cells); SpeakableTextPresentCheck on `SignUpPreview`'s empty
Name field. Each fails in every configuration it appears in (68 test cases); details per preview in
`ui-everything-atf-details-1.txt`.

**Pitfall for later proofs:** ktlint inside a Gradle daemon keeps a project's `.editorconfig` once read, so a scratch
edit of it (the Compose-Rules-only trick) leaks into later runs on the same path until the daemon ends. Run such a
scratch check on a copy at its own path, or regenerate before the real run.

### Open

- After-integration step 4 (e): the ATF findings above (the only intended visible UI changes, to name each fix).
- Step 4 (a)-(c) as GUARD and CORE listed (offscreen-layer markers, `designsystem-imports` IO half, `feature-primitive`,
  theming rules over the design system, the Compose Rules left off with their sites).
- Step 2 (the screen-file split), step 3 (the warm-up test), step 5 (`explicitApi`), step 6 (the No-UI shell comments
  above), step 7 (docs pass: Compose Rules, dependency-guard's first-run list, `add_component.py` in the template
  README, the preview-rendering sentences, "Narrow" equal to "Light"), step 8 (the Spotless reformat).
- From TEST's result: the Google button's `finally` has no test (Credential Manager needs an Activity); `AndroidCommon.kt`'s
  `onRobolectricTests` does not fire for a catalog `testImplementation(libs.robolectric)` (ten modules set the option
  themselves; fix proposed in `finalizeDsl`, needs both modes' full builds); the app profile's `Appeared` rules (Profile
  phase); the multiplatform template keeps `Appeared` and hand-set flags (59 (j)).
- From NAV: the shell's tests keep their own `TestKeys`/`TestScreens` copies; with TEST's testFixtures merged they can
  share one fixture. The runtime's and the shell's checked-in profiles name moved classes until the Profile phase.
- From GUARD: `buildHealth` on UI everything exhausts the daemon's Metaspace (the user's call); lint with the ten new
  ids has now run on both modes' everything builds (green).
- The store integration test needs Postgres (CI); `/admin` with real data is the gate's.
- `web/frontend/node_modules` was installed in this worktree by `npm ci` (git-ignored).

## After integration

After-integration run, `wt/NOUI-FINAL`, branch `staging-wp/NOUI-FINAL`, from the integration head `585542469`. Steps 1-7
ran from 02:0x to 05:37 Oct 26; that run was stopped at 06:44 inside step 8 (the reformat, uncommitted), and a second run
finished step 8 from the worktree as it stood and did step 9. **Done at 08:02 Oct 26, head `356e81d21`.** Nothing pushed,
`main` untouched, no branch or worktree deleted, no stash, no history rewritten; `wt/NOUI` and `wt/NOUI-FIX` untouched.
Logs in `logs/NOUI-FINAL/AFTER/`, projects in `out/NOUI-FINAL/AFTER/` (sources only: build, `.gradle` and `.kotlin`
deleted after each result was recorded, as approved). D: free: 74 GB at 06:54, 72.7 GB at the end.

### Commits (`git log 585542469..356e81d21`), one per step

| Step | Commit | What |
|---|---|---|
| 1 | `b777eaefe` | The cross-lane requests the integration had not already done: `navigation-host-api` also reads a feature's `build.gradle.kts` (the `optIn.add("…NavigationHostApi")` route), only for rules that declare `buildFiles = true`; `ProfileTest` checks the shell module's own profile. The baselines, stability names, matrix row, CLI step and LaunchHook fence were already in the integration's commits (`a1429a444`, `676ae2b1d`, `541812d98`, `a74c97de5`, `df73ab1bf`, `64d9f56aa`) |
| 2 | `3784613e1`, `797f34f5a` | L2: every mixed screen file moved whole first (so `git log --follow` reaches each part through a pure rename), then split into `<Screen>Contract.kt`, `ViewModel.kt`, `Route.kt`, `Screen.kt`, one package per screen; `ui_components.toml` owns the screen files; baselines, profiles, KDoc and test packages follow |
| 3 | `095068a67` | Shell test: a warmed tab whose ViewModel loads through `onFirstObserved` does not load until shown, loads once when shown, not again on return |
| 4 (a) | `2c966558a` | offscreen-layer, the IO half of designsystem-imports and feature-primitive in every project |
| 4 (b) | `d1150c2b1` | raw-color, raw-tween, raw-dp, raw-type and component-preview over every design system; `CodeRules.ALL` one declaration; `DesignSystemDocTest` in both modes |
| 4 (c) | `0111b148f` | every Compose Rule GUARD left off is on, its sites fixed or suppressed with a written reason |
| 4 (d) | none | GUARD adopted `checkComposeStability` on every feature module in its lane (`ce3e383e4`, merged); nothing was left |
| 4 (e) | `7b6a63621` | the ATF findings fixed in the components and one screen (named below) |
| 5 | `c2751e148`, `5ef85c345` | `explicitApi()` on `:core:designsystem` in both modes, the component template and add_component.py's output; the follow-up reads `public` glyph accessors and the font preload as the mode writes them |
| 6 | `c01c1330c` | the seven No-UI comments that described the shell, each an opt-else; UI renders byte-identical |
| 7 | `77e148b85` | the docs pass in both modes |
| 8 | `42f60102c`, `7ee33a7ac`, `356e81d21` | the Spotless carry-over (below); the later two fix what step 9's UI build found the reformat had broken: two offscreen markers ktlint had moved away from their line, and `TabCells.kt` past 500 lines (the indicator's state classes moved unchanged to `TabIndicator.kt`), then the compositing choice written as an `if`/`else` both format checks accept |
| 9 | `b19cb8ac6` | emitted files: `catalogue --write` moved only the generator commit the site's copy names; `tokens --write` changed nothing |

**The visible UI changes (step 4 (e), the only intended ones):** sign-up's and the password reset's form no longer
starts under the top bar (`AuthFrame` pads by the chrome); a chip's remove cross takes a 48 dp target around it
(overflowing the chip, no layout change); a tag in `AppTagField` is not a button of its own, and tag rows sit 16 dp
apart; a date-picker day's target is its whole cell (the pill drawn inset as before). Not visible: swipe actions' panel
buttons and the sheet scrim are hidden from accessibility services (custom actions and the sheet's dismiss action serve
screen readers). Two results stay as reviewed exceptions in the tester's `SUPPRESSIONS`, each with its reason: date-picker
days under 48 dp on windows narrower than 336 dp, and the sheet grabber's band.

### Step 8: the Spotless carry-over

The template's Kotlin and Gradle sources, the No-UI variant files and the `//~` fallback lines are in ktlint's Android
Studio style; the `<generated:formatting-check>` region is gone and the template's own `spotlessCheck` is enforced.
What the reformat could not do by itself, each with a test or a written reason:

- **Imports.** ktlint will not sort an import list that holds comments, and the markers are comments: every optional
  import sits at its sorted place, in a fence of its own where needed. `test_android_imports.TemplateTest` holds the
  template and the variants to that order; only the raw template suppresses `standard:import-ordering`
  (`<generated:template-import-order>`), so every project's check sorts its imports.
- **`//~` lines.** ktlint would rewrite `//~ x` as `// ~ x` and break the fallback, so only the raw template turns
  `comment-spacing` off (`<generated:template-comment-spacing>` and `-gradle-` regions); a project keeps the rule.
- **Fenced parameters.** A signature with a fenced parameter (`CoroutinesModule`, `FrameFeed`, `FrameFeeds`) is now a
  fenced signature with a `//~` fallback, so each project gets a signature ktlint keeps as written.
- **Markers above imports.** ktlint keeps comments out of an import list, so a `// allow(<rule>)` marker on the line
  above the list covers every import in it (`CodeRules.kt`; `CodeRulesTest` holds both that and that it covers nothing
  below the list).
- **Names ktlint refuses but the project means:** `@file:Suppress("ktlint:standard:filename")` with its reason on the
  `<Feature>Keys.kt` files and `GoogleSignIn.kt`, `class-naming` on a test's stand-in `R.string`;
  `ktlint_standard_property-naming = disabled` in every `.editorconfig` (Compose names constants in PascalCase);
  `standard:max-line-length` suppressed in Spotless (ktlint still wraps code at 100 columns; what it cannot wrap, a
  comment, a string or a long name, stays as written).
- **The generator writes what ktlint would:** a scaffolded tab one argument a line (`add_feature`/`remove_feature`
  read both forms), an entity's header on one line when it fits, the token copy's long calls wrapped, and no blank
  line left at a file's end where a fence closed it (`markers.collapse`; the golden trees lost that line in three
  `settings.gradle.kts`, regenerated with `GENKIT_UPDATE_GOLDEN=1` and reviewed).
- **The multiplatform copies.** `tools/sync_check.py` holds the shared `core/common` and `core/mvi` files line for
  line, and that template has no ktlint: its five drifted files took the Android layout (`RunSuspendCatching.kt`,
  `Validators.kt`, `MviViewModel.kt`, `UiMessage.kt`, `UiText.kt`) and the `UiText.kt` allowance names the new lines.
- **Hand layouts the formatter could not settle:** `HttpClientModule.kt` (three bodies), `AppButton.kt`'s preview calls
  (a lambda broken open mid-argument list, one argument a line now) and `Validators.minLength` (ktlint had broken its
  condition at `>=`), each then held by `spotlessCheck` in every shape.

**The comparison** (`logs/NOUI-FINAL/AFTER/step8-wscompare.py`, every preset in both modes generated from `HEAD` and
from the worktree with the same package, whitespace ignored; `step8-whitespace-compare-f8i.log`): no file only on one
side in any of the eight shapes; the files that differ (282 UI lean to 470 UI everything, 97 No-UI lean to 227 No-UI
everything) differ in whitespace and line breaks, in ktlint's punctuation (trailing commas, braces round a wrapped
branch: 126 to 350 files a shape), in an import's place, or in the same tokens in another order (an annotation or a
comment moved), except these, each one of the changes above: `CodeRules.kt` and `CodeRulesTest.kt` (the marker above an
import list), `<App>Application.kt` (a comment that sat inside the import list), `MainActivity.kt` (the applock note
joined to the KDoc it explains), `SignInViewModel.kt` (`showGoogleFailure()` named where the long branch was),
`SheetRules.kt` (parentheses that make an `&&` inside `||` explicit), the `ktlint` suppressions with their reasons
(`build.gradle.kts`, the `*Keys.kt` files, `GoogleSignIn.kt`, `SheetApiShapeTest.kt`), `docs/DESIGN-SYSTEM.md`'s
component template (named arguments and the layout `add_component.py` writes, which `DesignSystemDocTest` holds to the
code), `README.md`'s formatting sentence, and `generator-spec.json`'s commit. A fence check over every changed template
and variant file (`logs/NOUI-FINAL/AFTER/step8-fence_drift.py`) was read line by line: what crossed a marker is a
signature or declaration rewritten as a fenced whole with a `//~` fallback (`CoroutinesModule`, `FrameFeed`,
`OnboardingPage`, `SettingsEffect`), a comment that moved with its code, a line both branches now hold (`MemoryLog`), or
an import both branches held, now unfenced.

Observed and left: the raw template's `spotlessCheck` and a generated project's disagreed on the same code twice. The
raw check passed two lines in `HttpClientModule.kt` every generated project wrapped, and it asked for a blank line
between the branches of a `when` in `PresentationHost.kt` (a file with no markers) that the generated check did not. The
`editorConfigOverride` was ruled out only partly (the experiment moved `comment-spacing = disabled` into the
`.editorconfig` rather than dropping it; a rule ktlint orders after a disabled one may then run differently, one
possible cause). Both sites are now written so both checks pass (`HttpClientModule.kt` wrapped by hand; the compositing
choice an `if`/`else` with the marker on the offscreen branch), and AGENTS.md's Formatting paragraph says a Kotlin or
Gradle change is done when the template's check and a generated project's both pass.

### Where reality differed from the plan, and why (also in `decisions/LOG.md`)

- **Step 1:** most cross-lane requests were already done by the integration's own commits; what was left is the
  build-file route to `@NavigationHostApi` (`LOG.md` Oct 26 03:0x).
- **Step 2:** a screen named after its feature keeps the feature's root package; sign-in, sign-up, the password
  reset, the licences and a search result get packages of their own. The Google request's two declarations became
  `internal` in both modes, and the search result's inline content became `SearchResultScreen` with a preview (the only
  declaration differences, logged). Routes keep their fence around the placeholder rather than becoming variants, so
  their effect wiring stays one source (`LOG.md` Oct 26 02:4x).
- **Step 4 (a)-(c):** six reasoned offscreen markers, not four (glass and the lens blur match the rule too);
  AuthFrame's brand panel draws through `AppSurface` rather than taking a marker; `CodeRules.ALL` is one declaration
  with only `SheetRules.ALL` fenced, because the sheet exists only with components; every Compose Rule is on, with
  each site fixed or suppressed with its reason (`LOG.md` Oct 26 03:3x, 04:0x).
- **Step 4 (e):** 20 of the 68 ATF findings were Robolectric's 470 dp window cutting long previews off; the tester
  now gives a preview without a size a 1600 dp tall window. Two reviewed exceptions stay (`LOG.md` Oct 26 04:1x).
- **Step 5:** visibility kept exactly as it was (`public` written out on 886 UI and 60 more No-UI declarations, 190
  types stated); narrowing helpers to `internal` is a per-declaration review left for later (`LOG.md` Oct 26 04:4x).
- **Step 6:** the wizard's No-UI language question was already fixed by the integration (`776a3cd7f`).
- **Step 8:** what ktlint could not do in a template with markers, each with a test or a reason (the list above);
  the multiplatform copies took the Android layout; about 140 one-line conditions ktlint broke at their operator
  stay as ktlint left them (`LOG.md` Oct 26 07:20).
- **Step 9:** as the orchestrator asked (06:5x): no preset builds here, since the final gate builds every preset in both
  modes right after the reviews; one UI and one No-UI everything build with `check assembleDevDebug`, because step 8
  touched Kotlin and Gradle sources. The first UI build failed on two code rules the reformat had broken (the step-8
  proof had run only `spotlessCheck`), so UI everything was built again after the fix; the last commit, the layout of
  two expressions, was proven with narrow tasks rather than a third full build. `assembleProdRelease` is left to the
  gate, which builds it on both everything compact projects.

### Checks

Steps 1-7 were proven with the narrowest tasks that showed each change (one Gradle build at a time); step 9's two
everything builds are the first full `check` of all of them together since the integration's.

| Step | Check | Result | Log |
|---|---|---|---|
| 1 | Seen red first: a feature's build file opting in to `@NavigationHostApi` | `CodeRulesTest` 1 of 12 failing | `failfirst-step1-host-api-buildfile.log` |
| 1 | UI everything build-logic tests and the rules; generator suite | green; 664 OK, 9 skipped | `step1-ui-ev-buildlogic-rules.log`, `unittest-1-step1.log` |
| 2 | Top-level declarations of twelve preset shapes, before and after the split | the same sets but for the two internal Google-request declarations and the new `SearchResultScreen` and its preview (12 differences, each named) | `step2-declarations-1.log` |
| 2 | Seen red first: the search result's Back event; then No-UI and UI everything narrow builds | red as expected; No-UI green; UI's one failure was the sign-up preview's ATF finding (fixed in step 4 (e)) | `failfirst-step2-search-result-back.log`, `step2-noui-ev-narrow-1.log`, `step2-ui-ev-narrow-1.log` |
| 2 | Generator suite after the moves and after the split | 664 OK twice | `unittest-2a-moves.log`, `unittest-2b-split.log` |
| 3 | Seen red first against a hook that runs at construction; then the shell's tests | red, then green | `failfirst-step3-warmed-tab-load.log`, `step3-shell-green-1.log` |
| 4 (a)-(b) | Each family's rules and build-logic tests on UI and No-UI everything; generator subsets | green (after the fixes the first runs named) | `step4a-*`, `step4b-*`, `unittest-4a-subset.log`, `unittest-4b-subset.log` |
| 4 (c) | Compose Rules on with every rule on (discovery), fixed site by site | UI red through run 7, green at run 8; No-UI green | `step4c-discovery-*`, `step4c-ui-compose-rules-1..8.log`, `step4c-noui-compose-rules-1.log` |
| 4 (e) | ATF over every preview test, then the touched modules' tests and rules in both modes | 68 findings to 0 (two reviewed exceptions); UI and No-UI green | `step4e-atf-1..4.log`, `step4e-ui-tests-1.log`, `step4e-noui-tests-1.log` |
| 4 | Generator suite | 664 OK, 9 skipped | `unittest-4e.log` |
| 5 | `:core:designsystem` compiled with `explicitApi()` round by round in both modes; `add_component Pennant` on lean UI and No-UI with `DesignSystemDocTest` | green from UI round 4 and No-UI round 5 on (each round logged); the component compiles and its test passes in both | `step5-x*-compile.log`, `step5-xui6-component.log`, `step5-xnoui6-component.log`, `unittest-5-subset.log` |
| 6 | UI lean and everything rendered before and after | 0 differing files; No-UI everything render read against its code | (commands in that run; projects `s6*`) |
| 7 | `test_docs`, `test_android_truth`, `test_markers` | 66 OK | `unittest-7-docs.log` |
| 8 | Generator suite on the reformatted tree (the run before had 17 failures: the golden trees' end-of-file line and the multiplatform sync, both fixed above) | 668 OK, 9 skipped | `unittest-8-step8.log` (first run, its output cut to the tail), `unittest-8b-step8.log` |
| 8 | Raw template copy, `spotlessCheck` | green | `step8-raw6-check.log`, `step8-spotlesscheck-raw-f8i.log` |
| 8 | `spotlessCheck` with `formatting` on: UI and No-UI lean, standard, everything | first round (`f8h`): all six red on `HttpClientModule.kt` only; after its fix (`f8i`): all six green; `Validators.minLength`'s relayout then green in UI standard (`f8j`) | `step8-spotlesscheck-f8h-*.log`, `step8-spotlesscheck-f8i-*.log`, `step8-spotlesscheck-f8j-ui-standard.log`, `step8-f8i-summary.log` |
| 8 | Every preset in both modes before (`HEAD` 5ef85c345, exported) and after, whitespace ignored | as described above | `step8-whitespace-compare-f8i.log` (script `step8-wscompare.py`) |
| 8 | Experiments: the raw template with a 108- and a 127-column line, and with its `editorConfigOverride` moved into `.editorconfig` | the long lines are wrapped (the 100 columns apply); with the override moved, the two `HttpClientModule.kt` lines still pass | `step8-raw6-lineprobe.log`, `step8-rawexp1-no-override.log` |
| 9 | `catalogue --write`, `tokens --write`, then both `--check`s and the JSON | only the catalogue's commit stamp changed; current, 3 copies current, valid | `step9-emitted-and-checks.log` |
| 9 | `tests.consistency --quick` at `b19cb8ac6` | 83 projects, 0 new issues, 190 known, CONSISTENCY OK | `step9-consistency-quick.log` |
| 9 | No-UI everything compact (`com.example.finalafter.s9nouieverything`, at `b19cb8ac6`), `check assembleDevDebug --continue`, `formatting` on | green in 7 m 27 s, every task: unit tests, lint, every guard, stability, Spotless with the Compose Rules and ktlint, dependencyGuard ("baseline created"), the APK. `7ee33a7ac` and `356e81d21` touch only files a No-UI project does not have (and AGENTS.md), so this holds at the head | `step9-build-noui-everything.log` |
| 9 | UI everything compact at `b19cb8ac6`, the same command | red in 7 m: `:core:designsystem:verifyCodeRules` (`TabCells.kt` file-length) and `:core:navigation:shell:verifyCodeRules` (two offscreen markers off their line), which stop the design system's `preBuild`; fixed in `7ee33a7ac`, then those two tasks and `spotlessCheck` green on a fresh render | `step9-build-ui-everything.log`, `step9-narrow-ui-rules.log`, `step9-narrow-ui-rules-2.log` |
| 9 | UI everything compact at `7ee33a7ac` (`com.example.finalafter.s9buieverything`), `check assembleDevDebug --continue`, `formatting` on | green in 14 m 11 s, every task: unit tests with the ATF audit in every preview test, lint, every guard, stability, Spotless with the Compose Rules and ktlint, `DesignSystemDocTest` (through `:build-logic:convention:check`), dependencyGuard ("baseline created"), the APK | `step9-build-ui-everything-2.log` |
| 9 | `356e81d21` (the compositing choice as `if`/`else`; layout of two expressions): the raw template's `spotlessCheck`, and on a fresh UI everything render both modules' `verifyCodeRules`, `spotlessCheck` and the shell's compile | green; the raw check had asked for the `when`'s blank line the generated check refused (`step9-raw-template-spotlesscheck.log`). No third full UI build: the orchestrator allowed one each, and the gate builds every preset at the head | `step9-raw-template-spotlesscheck-2.log`, `step9-narrow-ui-rules-3.log` |
| 9 | Generator suite on the final tree | 668 OK, 9 skipped | `step9-unittest-head.log` |
| 9 | `tests.consistency --quick` on the final tree | 83 projects in 218 s, 0 new issues, 190 known, CONSISTENCY OK (started at `7ee33a7ac` with `356e81d21`'s two files already in the tree) | `step9-consistency-quick-head.log` |
| 9 | `catalogue --check`, `tokens --check`, `sync_check` at the head | catalogue current, 3 token copies current, 2 shared parts in sync | `step9-checks-head.log` |
| all | Private names in `585542469..HEAD`'s diff and messages | none | (command in this run) |

### Open

- **For the gate:** every preset in both modes with `check assembleDevDebug`, and `assembleProdRelease` on both
  everything compact projects (not run here, as the orchestrator asked); the phone checks in the plan's section 8
  (the No-UI app lock's focus fix among them).
- **Profiles (Profile phase):** step 2 renamed the screens' file classes (`SignInKt` to `SignInScreenKt` and the
  like); the checked-in startup profiles name the new classes in the identity form, and are re-recorded on the Redmi
  from release builds as the plan says.
- **Layout left as ktlint wrote it:** about 140 one-line conditions broken at their operator (`if (a >=` / `b` /
  `) {`), in components, guards and the catalog. Layout only; a hand pass can lay them out as a named value or one
  line where it fits, each then held by `spotlessCheck`.
- **The raw template's check and a generated project's can disagree** on a layout (above); the cause is not
  pinned down. A one-hour look: run the raw check with `comment-spacing` on over a copy without `//~` lines.
- **`explicitApi()`:** every former default-public declaration is written `public`; which helpers should be `internal`
  is a per-declaration review for later (step 5's decision).
- **Multiplatform template (59 (j)):** its five shared files now carry the Android layout by hand; it has no ktlint of
  its own, so its formatting pass comes with its own work after the Android release.
- **Carried from the integration's Open** (unchanged by this run): the Google button's `finally` has no test;
  `AndroidCommon.kt`'s `onRobolectricTests` for a catalog `testImplementation(libs.robolectric)`; the shell's tests'
  own `TestKeys`/`TestScreens` copies; `buildHealth` on UI everything exhausting Metaspace (the user's call); the store
  integration test needs Postgres (CI); `web/frontend/node_modules` installed in this worktree by the integration's
  `npm ci` (git-ignored).
- Not run in this run: the site's tests and `go test` (no site or API code changed after the integration's runs;
  `catalogue.json` changed only its commit stamp).

## Profiles

T4 (plan section 6), 08:03-08:53 Oct 26, in `wt/NOUI-FINAL` on `staging-wp/NOUI-FINAL` from `356e81d21`. Logs in
`logs/NOUI-FINAL/PROFILE/`, projects in `out/NOUI-FINAL/profile/`. **Head `a31d9dbc9`.**

| Commit | What |
|---|---|
| `3460572f6` | The UI profiles re-recorded: `templates/android/{app,core/navigation,core/navigation/shell,core/designsystem,core/ui}/src/main/baseline-prof.txt` |
| `a31d9dbc9` | The No-UI profiles re-recorded: `generator/variants/noui/{app,core/navigation,core/designsystem,core/ui}/src/main/baseline-prof.txt` |

**How.** Both everything compact projects generated at `356e81d21` as "Base App" / `com.base.app` (specs
`out/NOUI-FINAL/profile/spec-{ui,noui}-everything.json`, `--preset everything`), so every rule is already in the
template's identity form (`BaseAppApplication`, `Hilt_BaseApp…`, `DaggerBaseApp…`, `injectBaseApp…`, `com/base/app`):
no rename step, unlike f97's `sed`. Recorded on the Redmi (9a7c74f4, M2101K6I, API 37) with
`locked adb bash staging/scripts/t4-profile.sh <project> <modules>` (wake, `locked gradle ./gradlew
:app:generateBaselineProfile`, benchmark `nonMinifiedRelease` for `playstoreRelease`, split, HOME), then split with
`tools/perf/split_profile.py` (the hand-written wildcard rules kept) and copied over the template's files. The No-UI
project has no `tools/perf`, so the script stopped at the split (EXIT 2 in its log); the split ran with the worktree
template's `split_profile.py` over `core/navigation`, `core/designsystem` and `core/ui`, and the HOME key by hand
(appended to the same log). No device setting changed; no `com.base.app` package before or after either run.

| Profile | Rules recorded | app | core/navigation | core/navigation/shell | core/designsystem | core/ui |
|---|---|---|---|---|---|---|
| UI, before -> after (lines) | 42555 | 3491 -> 3513 | 198 -> 225 | 1151 -> 1159 | 2639 -> 2708 | 100 -> 96 |
| No-UI, before -> after (lines) | 25301 | 1703 -> 1670 | 142 -> 156 | absent | 308 -> 318 | 17 -> 17 |

Hot methods (`staging/scripts/t4_profile_stats.py old new`): UI shell 15 -> 15, design system 8 -> 12, app,
navigation and ui 0 -> 0; one method each in the shell and the design system was hot before and is not now. No-UI: 0
before and after in every module. The recording is not poorer than the one it replaces, so it ships alone
(PERFORMANCE.md's merge rule does not apply). The UI app profile now names the split screens' classes
(`FeedScreenKt`, `SampleListScreenKt`, `SettingsScreenKt`, …); the No-UI one `StartScreenKt`.

**Emitted files:** none changed. `catalogue --check`, `tokens --check`, `sync_check` current at `3460572f6`
(`checks-ui.log`); the goldens hold the mini template's profiles, not these.

| Check | Result | Log |
|---|---|---|
| UI `ProfileTest` (every rule names a class of an "Acme Ui" project; its application, Hilt and Dagger classes there) | OK | `ui-profiletest.log` |
| No-UI `test_the_profiles_name_only_classes_the_project_has` | OK | `noui-profiletest.log` |
| Generator suite at `a31d9dbc9` | 668 OK, 9 skipped (1231 s) | `unittest-head.log` |
| UI everything compact at `a31d9dbc9` ("Profile Check" / `com.example.profilecheck.ui`), `check assembleDevDebug assembleProdRelease --continue` | BUILD SUCCESSFUL in 16 m 39 s; the prod release carries `assets/dexopt/baseline.prof` (11894 B) and `.profm`; the merged ART profile has 12875 rules, 174 of them `ProfileCheckApplication`'s | `build-ui-everything.log` |
| No-UI everything compact at `a31d9dbc9` (`com.example.profilecheck.noui`), same command | BUILD SUCCESSFUL in 9 m 58 s; `baseline.prof` 8901 B; merged profile 7334 rules, 158 `ProfileCheckApplication` | `build-noui-everything.log` |
| Private names in the two commits | none | (command in this run) |

Build output (`build`, `.gradle`, `.kotlin`) of the four projects deleted after their results were recorded (the
user's 18:5x approval); specs, sources and logs kept. D: had 70.5 GB free before each full build.

### Open

- Cold start on the final head is the gate's (not measured here); the profiles were recorded at `356e81d21`, so a fix
  round that renames or moves classes on the cold-start path again leaves those rules unmatched until the next
  recording (`ProfileTest` and the No-UI profile test fail on a class that no longer exists, not on new code without
  rules).
- `staging/scripts/t4-profile.sh` assumes the project has `tools/perf`; a No-UI run needs the template's
  `split_profile.py` as above.

## Fix round 1

The review's three blocking findings, 08:53-09:13 Oct 26, in `wt/NOUI-FINAL` on `staging-wp/NOUI-FINAL` from
`a31d9dbc9`. One commit per cause; logs in `logs/NOUI-FINAL/FIX1/`, projects in `out/NOUI-FINAL/FIX1/`. **Head
`d24246684`.** Decisions with their reasons in `decisions/LOG.md` (Oct 26 09:05).

| Commit | Finding | What |
|---|---|---|
| `23e16ae22` | `NavigationHostApi.kt:7` names the shell in No-UI | The KDoc is fenced on `ui_components`: UI keeps its words, No-UI says "A host (this module's [AppNavigationHost], or one a project writes in a module of its own) opts in for its whole module". |
| `68d4a5e4b` | `ARCHITECTURE.md:704` "Fakes, never mocks" beside a mockk `DisplayManager` | Code made to match the doc: `FrameIntakeTest` runs under Robolectric (`AndroidJUnit4`, SDK 35 like perfhud's other Robolectric tests) and builds `RefreshRate` on the real system `DisplayManager`; `testImplementation(libs.mockk)` and the catalog's `mockk` version and library are gone. Nothing else in the template, variants or generator named mockk (the vendored testing-setup skill's mention is Google's text). The doc lines are unchanged and now true. |
| `d24246684` | `ComposeStability.stale` reads a partial report as complete | An entry is stale only when its function is in the report with none of its problems, or no `src/main` source of the module declares the function (`fun` regex as `test_android_guards.undeclared_stability_entries`, generic and extension forms included). A declared function the report leaves out keeps its entry. New `ComposeStability.functions(reports)`; the unused `packages()` is gone. Tests: `an entry stays while a source declares its function, though the report leaves the function out` (task level, seen failing first), `the functions a report describes are named whether or not they are flagged`, and the existing stale case now passes the report's functions and the sources, with a deleted composable (`Gone(value)`) added as stale. |

### Checks

| Check | Result | Log |
|---|---|---|
| Fail first: the new `ComposeStabilityTest` case against the old `ComposeStability.kt` (UI everything "Fix One") | 8 tests, 1 failed: the old check threw "accepts parameters this module no longer compares by identity … `com.acme.nav.BackEffect(handler)`" | `failfirst-stability-test-2.log` (`-1` is a compile error of my first draft of the test) |
| `./gradlew -p build-logic :convention:test` at the fix (UI everything) | BUILD SUCCESSFUL; 113 tests, 0 failed; `ComposeStabilityTest` 9 cases | `ui-ev-convention-test.log` |
| Edit-one-file-and-check on `:core:navigation:shell`, UI lean, OLD code (`cmp-before/ui-lean`, generated from `git archive a31d9dbc9`): full `checkComposeStability`, then `private val incrementalProbe = 0` appended to `ShellShortcuts.kt`, then the check again | full compile: report 39 functions, pass; incremental: report 1 function (`ShellShortcuts`), **FAILED** asking to delete `BackHandlerEffect(handler)` and `rememberEntryLifecycle(key)`: the finding reproduced on a UI project | `ic-before-1-full.log`, `ic-before-2-incremental.log`, `ic-before-report-{full,after-ic}.txt` |
| Same sequence, NEW code (`cmp-after/ui-lean`) | full: 39 functions, pass; incremental (`:core:navigation:shell:compileDebugKotlin` reran, report 1 function): **BUILD SUCCESSFUL** | `ic-after-1-full.log`, `ic-after-2-incremental.log`, `ic-after-report-{full,after-ic}.txt` |
| `checkComposeStability --continue` on every checked module, UI everything | BUILD SUCCESSFUL: 11 modules (core navigation, shell, designsystem, ui and the 7 features) | `ui-ev-checkComposeStability.log` |
| Same, No-UI everything ("Fix One" `com.example.fixone.nouiev`) | BUILD SUCCESSFUL: 10 modules | `noui-ev-checkComposeStability.log` |
| `:core:perfhud:testDebugUnitTest`, UI everything | BUILD SUCCESSFUL; 114 tests, 0 failed; `FrameIntakeTest` 3 of 3 under Robolectric | `ui-ev-perfhud-test.log` |
| `:core:perfhud:testDebugUnitTest :core:navigation:compileDebugKotlin`, No-UI everything | BUILD SUCCESSFUL; 114 tests, 0 failed; `FrameIntakeTest` 3 of 3 | `noui-ev-perfhud-test.log` |
| Render, HEAD (`a31d9dbc9`) vs the fixes, UI and No-UI lean and everything (`cmp-before/`, `cmp-after/`, `diff -rq` without `generator-spec.json`) | UI lean: only `ComposeStability.kt`, `ComposeStabilityTest.kt`, `libs.versions.toml` differ; UI everything: those plus perfhud's `build.gradle.kts` and `FrameIntakeTest.kt`. **`NavigationHostApi.kt` byte-identical in UI.** No-UI: the same files plus `NavigationHostApi.kt`, whose render was read (above wording, `[AppNavigationHost]` resolves to the variant in the same package; `core/navigation/build.gradle.kts` opts the module in) | `gen-cmp-*.log` |
| `catalogue --check`, `tokens --check`, `sync_check` at `d24246684` | current, current, in sync (no emitted file changed) | `checks-emitted.log` |
| Generator suite (`memwait python -I -m unittest discover -s tests -t .`), on the tree that became `d24246684` (every edit was on disk at its start; the commits only recorded them) | 668 OK, 9 skipped (604 s) | `unittest-1.log` |
| Private names in the three commits and the decision-log entry | none | (command in this run) |

D: had 70.5 GB free before the builds. Build output (`build`, `.gradle`, `.kotlin`) of the four projects built here
deleted after their results were recorded (the user's 18:5x approval); sources, specs and logs kept.

### Open

- The full preset builds of both modes are the final gate's; this round ran the tasks each fix touches.
- The profiles are untouched: no class on the cold-start path moved.
