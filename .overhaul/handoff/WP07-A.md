# WP07-A handoff (Compose Multiplatform foundation, Android + iOS)

Branch `wp/WP07`, worktree `$OV/wt/WP07` (`OV=D:/projects/tools/.overhaul`,
`O=$OV/out/WP07`, `L=$OV/bin/locked`).

## Progress

Updated 2026-10-17 15:25 IST (run 4, the first on Windows). **Stopped waiting for WP04** (the shell port,
items 1–5 and 7 below) **and WP02-M2** (item 6). Next run: `git merge overhaul/multiplatform-v2`, then item 1.

Run 4 did only what does not need WP04:
- Merged the integration branch (WP10) into `wp/WP07` as `fdd01b3`. No conflicts: the only overlap
  candidates were web paths and `generator/tests/fixtures/validation_cases.json`, none of them touched here.
- Checks on Windows at `fdd01b3`, through `bin/memwait`: the generator suite ran 286 tests with exactly the
  six host failures of `logs/windows/generator-baseline.md` and nothing else (`$O/logs/gen-tests-win.log`;
  `test_target_multiplatform` and `test_sync` all pass; the `project.yml` YAML parse is skipped because
  PyYAML is not installed outside a venv, the string assertions on it still run). `genkit.tokens --check`:
  1 copy current (the multiplatform one; the Android copy does not exist on the integration branch yet).
  `genkit.core.catalogue --check`: current. `tools/sync_check.py`: 2 shared parts in sync.
- Ported `$O/accept-a.sh`, `build.sh`, `build1.sh` and `ios-klib.sh` to Windows (`D:/` paths, no
  `ANDROID_HOME` override because `locked` sets it and `KONAN_DATA_DIR`, Python through `memwait`).
- **No Gradle build in this run (decision).** The last full build, b5 (Linux, Oct 12, at `36c90cb`), was
  green: `:androidApp:assembleDevDebug check`, BUILD SUCCESSFUL (`$O/logs/b5.log`). Since then only
  `99239af` (the coroutine-test opt-in for every test compilation) and the web-only WP10 merge landed. The
  user put the Android release first on Oct 17 ("I care about full android release now", Compose
  Multiplatform and Swift after it), the Gradle lock and the 16 GB are shared with the Android packages,
  and the shell port needs a full build and the iOS klib compile anyway. So the first Windows build of
  the template, its first Windows iOS klib compile and the Kotlin/Native toolchain download (into
  `D:\.caches\konan`, which `locked` sets as `KONAN_DATA_DIR`) happen with the port.

Runs 1–3 (Linux, Oct 10–12): runs 1–2 wrote most of `templates/multiplatform/` and the generator target
without committing; run 3 fixed b2's two compile errors, finished the generator side and committed it all
(`bb59ea3`..`ec15ef4`); `36c90cb` and `99239af` followed on Oct 12.

| Step (WP07 phase A) | State |
|---|---|
| 1 Build (settings, root build, properties, catalogue) | committed; androidx Navigation 3 pinned strictly to 1.1.7 in androidApp |
| 2 build-logic (Kmp* plugins, ModuleGraph, ApiPurity, AppConfig/AppFlavor) | committed; green in b5 |
| 3 Modules except the shell port | committed; green in b5 (Linux); not yet built on Windows |
| 4 What cannot port to navigation3-ui 1.1.2 | waits for WP04 (see Remaining) |
| 5 DI (Koin modules, KoinModulesTest) | committed; `KoinModulesTest` green in b5 (`:shared:testAndroidHostTest`) |
| 6 Sync check (core/common, core/mvi) | done: `sync.toml`, `tools/sync_check.py`, `generator/tests/test_sync.py`; the shell pairs wait for WP04 |
| 7 Generator target `targets/multiplatform` | done: target, scaffold, settings, strings, translations, fixtures, catalogue |
| 8 Tests (`test_target_multiplatform.py`) | done (28 tests, green on Windows); the shell tests wait for WP04 |
| iOS klib compile | passed on Linux without the shell: every module's `compileKotlinIosSimulatorArm64`, `:shared` included, ran in b4 and was UP-TO-DATE in the green b5 (`check` depends on it). The standalone acceptance attempt (`$O/ios-klib.sh`) never ran (its log shows only the lock wait). Repeat on Windows after the port. |

## What remains, and what it waits for

Everything below needs the merged WP04 shell (`merged/WP04` on `overhaul/multiplatform-v2`), except the
two items marked otherwise.

1. **Port the shell** into `templates/multiplatform/core/navigation` (step 3, A3.2–A3.4a, A3.9):
   KeptTabs v2, StackScene/StackPlan, transitions with the draw-phase scrim, BackGate per host, NavGuard,
   the presentation host, one tab host, chrome policy per platform (ADAPTIVE §13.4: iPhone keeps the bar
   sideways; iPad rail, then sidebar), the iOS edge-swipe back (MOTION M3) and the Gmail card on
   Android (M1). Delete `core/navigation/.../interim/`, replace `AppShell.kt`'s body and
   `rememberShellState`'s signature, and update `shared/.../App.kt`. Features need no change: they
   use only `navGraph`, `entry`, `navKeys`, `AppNavigator` and the keys, which already match WP04's API.
2. **List what cannot port to JetBrains navigation3-ui 1.1.2** (step 4). Found so far by reading
   WP04's imports against the androidx 1.1.7 sources that 1.1.2 is built from:
   - `androidx.navigation3.scene.rememberNavigationEventState(SceneState)` (WP04 `PresentationHost`)
     is 1.2.0 only; 1.1.7's `NavDisplay` builds the same from `navigationevent.compose
     .rememberNavigationEventState(currentInfo, backInfo)`, which the port can do directly.
   - `rememberSceneState(entries, strategies, decorators, sharedTransitionScope, onBack)`,
     `rememberDecoratedNavEntries`, `SceneDecoratorStrategy` and `LocalNavAnimatedContentScope` exist in
     1.1.7.
   - The 1.2.0 mid-flight predictive back during a push is absent, so BackGate holds gestures during
     pushes on this template (A7.2).
   - `LocalOnBackPressedDispatcherOwner` (activity) is Android-only: `androidMain` or replaced by
     `navigationevent`.
   - Risk: JetBrains navigation3-ui 1.1.2 brings `org.jetbrains.androidx.navigationevent:
     navigationevent-compose` 1.1.0, whose iOS variants are JetBrains' own artefacts. Adding androidx
     `navigationevent-compose` 1.2.0 (WP04's catalogue) may put two copies of the same classes on the
     iOS klib path. Check with `:shared:compileKotlinIosSimulatorArm64` before relying on any 1.2.0
     navigationevent API in common code.
3. **Port the shell tests** to `core/navigation/src/commonTest` (camelCase, K-8) and run them as Android
   host tests: `KeptTabsTest`, `BackGateTest`, `RapidBackTest`, `NavGuardTest`, `ChromeLayeringTest`
   (and `ResizeContinuityTest` A/B/C, ADAPTIVE §13.7). They need `compose-ui-test` and Robolectric back
   in the catalogue.
4. **Extend `sync.toml` to the shell** (step 6 covers core/common, core/mvi and the shell): one pair
   per `core/navigation` sub-package, with the 1.1.2 differences above as allowances.
5. **Platform sources of posture and input** (ADAPTIVE §13.2): Android `WindowInfoTracker` and
   `InputManager` from `MainActivity`; iOS `GCKeyboard` in `MainViewController`; passed to
   `AppTheme(posture, input)`. They belong with the shell's window plumbing (WP04 AD-15 on Android).
6. **Vendored skills** (STANDARDS §4; waits for WP02-M2, not WP04): run WP02-M2's
   `tools/update_android_skills.py` once it has merged (it already writes `templates/multiplatform/`
   when the template exists, without `agp-9-upgrade`), add the `verbatim` key it introduces to
   `template.toml`, and add the `.agents/skills` pair to `sync.toml`.
7. **Acceptance** (`$O/accept-a.sh`, written, ported to Windows in run 4; WP07 phase A). It runs the full
   template build and both generated fixtures; it can only end `ACCEPT OK` after items 1–4, and only after
   package WIN has merged (until then the generator suite fails its six Windows host tests under `set -e`).

## What changed (by path)

Commits on `wp/WP07` since `8672003` (the merge of WP09): `bb59ea3` build and conventions, `4713a43`
core/common, core/mvi, core/testing and the sync check, `2cc1a02` design system and the commonMain
token emitter, `5a64981` core/ui and the navigation API with the stand-in host, `96db98c` network,
datastore and database, `0e24a63` sample, settings and onboarding, `52ff904` shared, androidApp and
iosApp, `a50b264` the generator target, `62f2064` constant names and unused aliases, `3e82a7d` system
bar icons follow the app theme, `65f5f58` the wizard's multiplatform questions, `282a90a` the font
note, `ec15ef4` Kotlin/Native outside the Gradle daemon.

```
templates/multiplatform/
  settings.gradle.kts build.gradle.kts gradle.properties gradle/ (wrapper 9.7.1, libs.versions.toml)
  template.toml presets.toml features/{network,coil,database,sample,settings,onboarding,language}.toml
  sync.toml config/compose-stability.conf .editorconfig .gitignore .gitattributes
  build-logic/convention/   Kmp{Pure,Library,Compose,Feature,Data,Room,Umbrella}ConventionPlugin,
                            AndroidApplicationConventionPlugin; com/base/app/buildlogic/{kmp,guard,config}/
  core/common   PURE (kotlin multiplatform: jvm + iosArm64 + iosSimulatorArm64; Android consumes the jvm jar)
  core/mvi      MviViewModel, UiContracts, UiMessage, UiText (Compose resources)
  core/designsystem  theme/ (AppTheme, tokens/DesignTokens.kt emitted), foundation/{adaptive,content,interaction}/,
                component/{text,button,list,container,input,feedback,navigation}/, icon/ (+ glyph/)
  core/ui       MviScreen, MessageHost, AppErrorText, UiTextExt, AppNetworkImage (coil)
  core/navigation  public API as WP04's (AppNavigator, NavCommand, AppBackStack, NavGraph, NavKeySerialization,
                Presentation, NavTransitionStyle, ShellState, AppShell, AppNavigationHost) + interim/StackHost.kt
  core/network  NetworkClient on Ktor (OkHttp / Darwin), ResponseUnwrapper, NetworkMonitor actuals
  core/datastore  PreferencesFile expect/actual, AppSettingsStore
  core/database Room 3 + BundledSQLiteDriver, DatabaseBuilder expect/actual, SampleEntity/SampleDao
  core/testing  runViewModelTest, FakeNetworkMonitor
  data/sample   api/ (SampleRepository, model/SampleItem), OfflineFirst… (database) or Network… (no database)
  feature/{sample,settings,onboarding}
  shared        App.kt, AppEnvironment, di/{AppModules,Koin}.kt, navigation/AppDestinations.kt,
                iosMain/{MainViewController.kt, log/ConsoleLogger.kt}; androidHostTest KoinModulesTest
  androidApp    BaseAppApplication (startKoin), MainActivity, flavours dev/prod, launcher icon, locales_config
  iosApp        project.yml, BaseApp/{BaseAppApp.swift, ComposeView.swift, Info.plist, Assets.xcassets},
                Configuration/{Dev,Prod}.xcconfig
generator/genkit/targets/multiplatform/{__init__,target,settings,strings,modules}.py
generator/genkit/tokens/kotlin_common.py (+ its entry in tokens/__main__.py)
generator/translations/multiplatform/{de,es,fr,hi,pt-BR}/  (_language, _scaffold, one file per module)
generator/tests/{test_target_multiplatform,test_sync}.py, fixtures/v2/mp-{lean,standard,everything}.json
tools/sync_check.py
web/frontend/lib/catalogue.json (re-emitted: the multiplatform target, beta)
Touch edits outside owned paths: generator/create_project.py (--target defaults to the spec's platform),
generator/genkit/core/prompts.py (the multiplatform wizard questions), generator/tests/test_spec.py (each
v2 fixture is read by its own platform's target).
```

### Module graph and Koin layout

`ModuleGraph` (compact rules of A2.4): `:core:*` → `:core:*`; `:feature:x` → `:core:*`, `:data:*`;
`:data:x` → `:core:*`; `:core:common` is pure (no Android, Compose or Koin plugin). `:shared` and
`:androidApp` are composition roots and unchecked. `verifyApiPurity` also forbids `org.jetbrains.compose.`,
`org.koin.` and `platform.` imports in `api/` packages.

| Koin module | Declared in | Binds |
|---|---|---|
| `appModule(environment)` (private) | `shared/di/AppModules.kt` | `AppEnvironment`, `AppDispatchers`, `ApplicationScope`, `AppNavigator`, `NavRegistry(getAll<NavGraphEntry>())`, `NavKeySerialization(getAll<SerializersModule>())`, `NetworkConfig`, `SettingsAppInfo` |
| `dataStoreModule` | core/datastore | `AppSettingsStore` |
| `networkModule` | core/network | `NetworkMonitor`, `ResponseUnwrapper`, `NetworkClient` (its `HttpClient` is built inside) |
| `databaseModule` | core/database | `AppDatabase`, `SampleDao` |
| `sampleDataModule` | data/sample | `SampleApi`, `SampleRepository` |
| `sampleModule`, `settingsModule`, `onboardingModule` | feature/*/navigation | ViewModels; the feature's `NavGraphEntry` and `SerializersModule` under `named<RootKey>()` |
| `<name>DataModule`, `<name>Module` | scaffolded modules | the same shape |

`initKoin(environment, platform)` starts it: from `Application.onCreate` (with `androidContext`) and on
the first `MainViewController` call on iOS. `KoinModulesTest` (`:shared:testAndroidHostTest`) runs
`verify()` over the whole list, with the types the graph is handed rather than asked for listed as
extra types.

### What differs from the Android template, and why

- **DI is Koin** (A7.2), so features register a Koin module instead of Hilt `@IntoSet` providers; the
  navigation registration (`navGraph { entry<K> { } }`) and the keys are the same.
- **Keys are serialised polymorphically** (`navKeys { subclass(K::class, K.serializer()) }` per feature),
  because Kotlin/Native has no reflection. A key missing from its feature's `navKeys` is logged and the
  stack is not saved.
- **`UiText` holds Compose `StringResource`s**, resolved by `core/ui`'s `UiText.asString()`; there is no
  `resolve(Context)`.
- **Strings live in `composeResources/values/strings.xml`** per module, with `Res` generated as
  `<module package>.resources.Res`, internal to the module.
- **Environments**: two (dev, prod), as Android flavours and iOS build configurations; values reach
  shared code as `AppEnvironment`. No staging, no playstore flavour, no keystores, no deep links in v1.
- **Fonts**: the platform's own face (Roboto, SF); a chosen font gets a generation note (A7.2).
- **Navigation host**: the stand-in under `core/navigation/.../interim/` until the shell port; see
  "What remains".

### Sync allowances (`templates/multiplatform/sync.toml`)

| Pair | Allowed difference | Reason |
|---|---|---|
| core/common | none | the module is pure Kotlin on both sides |
| core/mvi `UiMessage.kt` | the id counter: `AtomicLong()`/`incrementAndGet()` ↔ `AtomicLong(0)`/`incrementAndFetch()` with `@OptIn(ExperimentalAtomicApi::class)` | no `java.util.concurrent` on Kotlin/Native |
| core/mvi `UiText.kt` | `@StringRes Int`/`@PluralsRes Int` ↔ `StringResource`/`PluralStringResource`; Android's `resolve(Context)` | Compose resources, resolved in core/ui |

`tools/sync_check.py` drops import lines, aligns the rest with `difflib.SequenceMatcher`, cancels a line
that is on both sides of a changed stretch (the matcher's alignment, not a difference), and fails on any
other line, a one-sided file, or an allowance that no longer matches. Tests in `test_sync.py`.

## Deviations, with evidence

1. **`kotlin.native.disableCompilerDaemon=true`** in `gradle.properties` (not in the plan). In b3 the
   in-process Kotlin/Native compiler exhausted the shared Gradle daemon's 1 GB metaspace
   (`$O/logs/b3.log` lines 125–132, then `OutOfMemoryError: Metaspace` in five iOS compiles and every
   later packaging task). Part D 14 forbids raising the heap, and the daemon is the one every other
   template build on the machine shares, so the compiler runs in its own process instead. KGP reads the
   property in `internal/properties/NativeProperties.kt` (`forceDisableRunningInProcess`).
2. **The androidx Navigation 3 pin is a strict constraint in androidApp**, not only a catalogue
   version: on Android, JetBrains navigation3-ui 1.1.2 resolves to androidx `navigation3-ui` and
   `navigation3-runtime` 1.1.7 (its Gradle module metadata), so androidApp holds both at 1.1.7 with
   `strictly`, and a library that pulls a newer androidx version fails the build.
3. **coil requires network**: `coil-network-ktor3` needs a Ktor engine, which only core/network brings.
4. **Touch edits to engine files** (`create_project.py`, `core/prompts.py`, `tests/test_spec.py`), each
   the smallest change the multiplatform target needed: the acceptance command generates from a spec
   without `--target`; the wizard asked multiplatform projects for a staging URL and keystores that the
   target then refused (WP01's handoff left this hook to WP07); the v2 fixture test read every fixture
   with the Android target.
5. **README regions dropped from the target**: the template has no README yet (WP12 writes it), and
   the engine refuses regions the template does not declare. WP12 adds `readme-*` regions with the
   README.
6. **System bar icons follow the app theme on Android** (`MainActivity` re-applies edge-to-edge from
   `App(onDarkThemeChange)`); iOS keeps the device's status bar style until the shell port (overriding
   the interface style there would also freeze `isSystemInDarkTheme()`).

## Rules and gotchas the docs must state (for WP12)

- The multiplatform template is **beta**: the iOS half is compiled to klibs on Linux but never linked
  or run here; the macOS CI job (below) is what proves it.
- Kotlin/Native compiles in its own process (`kotlin.native.disableCompilerDaemon=true`); do not move it
  back into the Gradle daemon on a machine that shares one daemon between projects.
- iOS: `CADisableMinimumFrameDurationOnPhone` must stay `true` in `Info.plist`, or Compose renders at
  60 Hz on ProMotion iPhones. The iPad keys (all four orientations, no `UIRequiresFullScreen`,
  `UILaunchScreen`, `UIApplicationSupportsIndirectInputEvents`) keep the app in Split View and Stage
  Manager with pointer input.
- A new feature module registers one Koin module (ViewModels, `navGraph`, `navKeys` under
  `named<RootKey>()`), adds it to `appModules()`, and lists every key in `navKeys`; `KoinModulesTest`
  catches a missing binding, nothing catches a missing `navKeys` entry except a lost stack (logged).
- Strings: one `composeResources/values/strings.xml` per module, names prefixed with the module, the
  `Res` class internal to it. Translations go to `values-<language>[-r<REGION>]`.
- The window size class is read once in `AppTheme` and changes only at a breakpoint; on iOS the window
  size animates every frame of a rotation or Stage Manager resize, so never read
  `LocalWindowInfo.containerSize` in composition.
- `AppNetworkImage` draws a painter (`AsyncImage`), never `SubcomposeAsyncImage`.
- The HttpClient is private to `NetworkClient`; Coil uses its own Ktor client.

## iOS: commands the CI macOS job must run (WP13)

```bash
brew install xcodegen
cd <project>                                   # the template or a generated project
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
cd iosApp && xcodegen generate
xcodebuild -project <App>.xcodeproj -scheme <App> -configuration Dev \
  -destination 'generic/platform=iOS Simulator' -sdk iphonesimulator CODE_SIGNING_ALLOWED=NO build
```

`<App>` is the Pascal name (`BaseApp` in the template). The scheme's pre-build script runs
`./gradlew :shared:embedAndSignAppleFrameworkForXcode`, which reads `KOTLIN_FRAMEWORK_BUILD_TYPE` from the
xcconfig (debug for Dev, release for Prod).

## What the generated AGENTS.md must say about iOS

Unverified locally: the iOS app is built only by CI on macOS; no Mac was used to write it. 120 Hz needs
the `Info.plist` key above. Frosted glass is the iOS default once phase B adds glass (risk R5).

## Open issues

- The stand-in host rebuilds a tab's screens on every tab switch (one `SaveableStateProvider` keyed by
  the tab) and has no back gate; acceptable only until the shell port replaces it.
- `AppTabBar` here is a basic docked bar with no tab cap: more than five tabs would crowd it. The shell
  port brings WP04's bar, rail, sidebar and "More".
- Right-click: Android's `clickable` reacts to a secondary mouse button
  (`firstDownRefersToPrimaryMouseButtonOnly() = false` in foundation 1.12), so `pressable` does too.
  ADAPTIVE §13.6 puts the fix in shared code; it arrives with WP03's `Pressable` in phase B.
- iOS status bar style follows the device, not the app theme (deviation 6).

## Orchestrator note (Oct 12, 13:25): bottom sheet system (REQUIREMENTS 33)

The user asked for a highly customisable, modern bottom sheet. Its spec is `research/SHEET-SPEC.md` (being written now by a design workflow; re-check `notes/INDEX.md`). Once it exists, deliver your package's subsection of its "Work split" as part of your scope; reviewers check it.

## Orchestrator note (Oct 12): shimmer and clipping (the user's report)

Written 2026-10-14, for phase B (WP07-B). The user, Oct 12 at 13:35, watching the phone: "shimmer has issues and
content gets cut" (REQUIREMENTS 34). Android's fix is on `wp/WP03-skel` (merged through WP03). Phase B brings the
Multiplatform template to parity. Reviewers check each item against its acceptance line. Android ships first
(REQUIREMENTS 36), so this does not block the Android release.

Source: the design report's §2 and §4.1, run `wf_b8570cc1-22c`. `qa/shimmer/REPORT.md` was never written to disk.
Its full text is the `design:report` result in
`~/.claude/projects/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/subagents/workflows/wf_b8570cc1-22c/journal.jsonl`
(see `handoff/WP03-skel.md`). Also read `qa/shimmer/AUDIT.md` §7.

**Defects.** These are from code, at `wp/WP07` `99239af`. No Multiplatform app has run on the phone yet.
- `templates/multiplatform/core/designsystem/src/commonMain/.../component/feedback/AppSkeleton.kt:20-58`:
  - The skeleton is still grey bars, with an API (`AppSkeletonLine`, `AppSkeletonRows`) that differs from Android's.
  - `LineHeight` is fixed at 14 dp, so it ignores the font scale.
  - The rows are bare lines with an `lg` inset, while the content is cards with a `gutter` inset.
- `feature/sample/.../list/SampleListScreen.kt:56-97`:
  - The skeleton is a `Column`, and the loaded list is a `LazyColumn`, so the header is rebuilt and moves when
    the data arrives.
  - The loaded list's `contentPadding` is the status bar only (`:63`).
- `core/navigation/.../AppShell.kt:35-45`: the interim `Column` stacks the tab bar under the content. This is
  REQUIREMENTS 17's opaque strip, the one that cut the last card on Android (evidence:
  `qa/shimmer/evidence/frames/r8-1335-light_pull-bottom-cut.png`).

**Fix:**
1. Port the Android design as it is into `commonMain`:
   - `foundation/draw/Skeleton.kt`: `Modifier.skeleton`, `Modifier.shimmerBounds`, `SkeletonNode`,
     `ShimmerBoundsNode` and `SweepGeometry`.
   - `foundation/draw/ShimmerClock.kt`.
   - `AppRedacted` and `LocalRedacted`.
   - `AppSkeleton`, `AppSkeletonLine(style)` and `AppSkeletonCircle`, plus the redaction hooks in `AppText`,
     `AppIcon`, `AppAvatar`, `AppButton`, `AppChip`, `AppBadge` and `Pressable`.
   - Everything is common API: `localPositionOf`, `withFrameNanos`, `LinearGradientShader` with `ShaderBrush`, and
     the translate-and-offset draw. Use no Android `Matrix` and no `setLocalMatrix`.
   - `AppSkeletonRows` and the 14 dp line go, with no deprecation cycle, since no generated Multiplatform app exists
     yet.
   - Add the pairs to `sync.toml` so drift from Android is caught.
2. Tokens: emit `DesignTokens.Motion.skeleton*` and the `skeletonHighlight` role from the same `tokens/*.json`.
3. Sample list:
   - Use one `LazyListState` for the loading list and the loaded list.
   - The loading rows are the real `SampleRow`, given a placeholder value, inside `AppRedacted`.
   - `contentPadding` comes from WP04's chrome contract, with WP04's K6 navigation-bar fix, not from the status
     bar alone.
   - The swap happens in one frame, with no cross-fade.
4. The shell port (your item 1) removes the `Column`: content runs under the bar, and only lists are padded.
5. Bones hold still, with no frames requested:
   - with reduce motion;
   - in a hidden tab (`TabVisibility`);
   - in previews;
   - without `AppTheme`.

   On iOS the frame clock is driven by `CADisplayLink`, which is right for 120 Hz ProMotion.

**Acceptance:**
- `commonTest`, on a fake `MonotonicFrameClock`:
  - `SweepGeometryTest`: two placeholders at different x sample the same band phase (±0.5 px); the tilt holds;
    right-to-left mirrors; the band starts and ends off the area.
  - `ShimmerClockTest`: the first frame is plain; progress eases from 0 to 1; no frame is requested during the
    800 ms pause; the clock stops when the last bone leaves; large windows slow the sweep.
- Android host tests:
  - `SkeletonCoherenceTest`: pixel coherence.
  - The recomposition journey: a sweeping skeleton list recomposes nothing.
- `:shared:compileKotlinIosSimulatorArm64` passes.
- On the phone, a release build of the generated Multiplatform sample meets Android's numbers from the report's
  §5.4:
  - janky ≤ 0.5 % and GPU p90 ≤ 7 ms over 10 s of a skeleton list, with ≤ 750 frames at 120 Hz;
  - 0 frames while idle;
  - one straight band;
  - no bottom strip.
- iOS stays unverified (beta).
