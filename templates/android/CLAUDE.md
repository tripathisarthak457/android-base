# Working on BaseApp

<!-- <opt:ui_components> -->
BaseApp is a multi-module Jetpack Compose app generated from a template: MVI on `MviViewModel`, Hilt, Ktor, Room,
Navigation 3, and a design system of its own in `:core:designsystem` with no Material underneath. It builds, tests and
lints clean as generated, and the build enforces most rules below. A change that turns the build red is not finished.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
BaseApp is a multi-module Jetpack Compose app generated from a template without UI components: MVI, Hilt, Navigation 3
and every build guard, with a theme skeleton in `:core:designsystem` (no Material) for your own components. Each screen
is a placeholder route over its ViewModel. It builds, tests and lints clean; a change that turns it red is not finished.
<!-- </opt:!ui_components> -->
`CLAUDE.md` and `AGENTS.md` hold the same text; the three docs at the end hold the reasons, the numbers and the recipes.

## Rules

**Modules.** `app/` wires and owns no logic, `core/` is infrastructure, `data/` holds one business domain each, `feature/`
holds screens. A `feature` depending on another `feature`, a `data` module on another `data` module, or `core` on either
fails `verifyModuleDependencies`: shared code goes to `core/`, or to a `data/` module both depend on. Only a feature's
`api` package is public (`feature-public`), and the feature's navigation keys live there.

**Screens.**
<!-- <opt:ui_components> -->
- A screen is a route that takes the ViewModel and connects it through `MviScreen` to a stateless
  `Screen(state, onEvent)`; only the route takes a ViewModel (`vm-forwarding`), and previews call the screen.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
- A route takes the ViewModel and connects it through `MviScreen`; today it draws `PlaceholderScreen`. Build each screen
  as a stateless `Screen(state, onEvent)` the route calls; only the route takes a ViewModel (`vm-forwarding`).
<!-- </opt:!ui_components> -->
- The ViewModel extends `MviViewModel`: `setState { copy(…) }` for what the screen renders, `emit(effect)` for one-off
  things (navigate, share), `showMessage(UiText)` for a transient note. `handleEvent` never suspends: slow work goes
  through `launchLatest(key, debounce)` (the newest request wins) or `launchExclusive(key, busy)` (a repeat is ignored,
  and `busy` holds a flag up exactly while it runs), and repository flows through `observe(flow) { … }`, which runs
  them only while the screen watches. The first load: `onFirstObserved { load() }` in `init`, never a `LaunchedEffect`.
- State is read-only: `val`s and `List`, `Set`, `Map` (`uistate-mutable`). Loading is a sealed `LoadState`, never a
  boolean beside a nullable, and a screen that loads draws loading, empty, error with retry, and offline.
- A destination is a `@Serializable` key implementing `AppNavKey`, registered twice with `@Provides @IntoSet`:
  `navGraph { entry<Key> { … } }` and `navKeys { subclass(…) }`. Without the second the app works until the process is
  killed in the background; `NavKeyRegistrationTest` catches it. Navigate through `AppNavigator`, never a back stack.

**Look, copy and windows.**
- No Material (`material`, `ripple`). Every colour, type style, spacing, shape and motion comes from `AppTheme`
  (`colors`, `typography`, `spacing`, `shapes`, `springs`): no raw colour, `tween`, or `dp` other than `0.dp` and `1.dp`
  in a feature (`raw-color`, `raw-tween`, `raw-dp`).
<!-- <opt:!ui_components> -->
- Components live in `:core:designsystem`, built to the contract in `docs/DESIGN-SYSTEM.md` (stateless, values from
  `XDefaults` and the theme, a preview each); a feature arranges components and draws nothing of its own.
<!-- </opt:!ui_components> -->
- Copy lives in `strings.xml` (`hardcoded-copy`): `stringResource(R.string.x)` in UI, `UiText.of(R.string.x)` in a
  ViewModel. A new string is translated into every `values-*` folder the project has, or lint fails the build.
- Every screen works on every window: lay out from `AppTheme.windowClass` and `AppTheme.containerClass`, never from
  orientation, screen size or device type (`adaptive-orientation`, `adaptive-screen-metrics`,
  `adaptive-device-type`); never lock orientation; a screen carries `@AppScreenPreviews` (`adaptive-preview`).

**Smooth at 120 Hz on a budget phone, where a frame has 8.33 ms.** The eight rules that matter most:
1. Judge smoothness only on a release build with its baseline profile, on a real phone; a debug build drops several
   times more frames and its numbers mean nothing.
2. Read anything that moves (animated, dragged, scrolled) in `offset { }`, `layout { }`, `graphicsLayer { }` or a draw
   block, never in composition (`animated-delegate`); pass a fast-changing value as `() -> T` (`state-param`).
3. Never put alpha below 1 on a full-screen layer (`layer-alpha-fill`): it renders the screen offscreen every frame.
4. A destination's first composition is its final one: seed it from memory and load secondary sections after the
   transition has landed.
5. Every lazy list has a stable `key`, a `contentType` when kinds mix (`lazy-key`, `lazy-content-type`), its padding
   inside the scroll (`scroll-chrome`) and a named fling (`scroll-fling`).
6. Motion comes from `AppTheme.springs`; a motion a finger released ends on a spring that starts at the finger's speed.
7. Nothing animates while idle or hidden, and every motion respects `AppTheme.reduceMotion`.
8. Nothing on the start-up path that the first frame does not need: no I/O and no eager singletons in
   `Application.onCreate` or the first screen. Also: `collectAsStateWithLifecycle()` (`collect-as-state`), and custom
   modifiers as `Modifier.Node`, never `Modifier.composed` (`composed`).

**Kotlin and tests.**
- Inject `AppDispatchers`; never `Dispatchers.IO` in a class, `runBlocking`, `GlobalScope` or `!!` (`dispatchers-io`,
  `blocking`, `bang-bang`). Around suspending code use `runSuspendCatching`, not `runCatching` (`run-catching`).
- Comments say why, never what or what changed (`slop-comment`, `commented-code`); a file stays under 500 lines
  (`file-length`). An exception to any rule is `// allow(<rule-id>): <reason>` on the line above, with a real reason.
- Never weaken, skip or delete a test to make the build pass. Stable library releases only, each version in
  `gradle/libs.versions.toml`. Never commit `keystore.properties`, `keys/` or `local.properties`, or log tokens.

## Build and verify

```bash
./gradlew :app:assembleDevDebug        # it compiles
./gradlew test                         # unit tests
./gradlew build                        # every variant, the tests, lint and every guard
```

Gradle runs on Android Studio's JDK or any JDK 17+ (never add a toolchain); memory goes in `gradle.properties`.

## Before you say it is done

1. `./gradlew build` is green, or you have said exactly what fails and why. Never report a build you did not run.
2. New logic has tests beside the existing ones, and no test was deleted or weakened.
3. A Compose change went through the `jetpack-compose-performance` audit, reading the category's page before each fix.
4. Every new string is in resources and translated; every new colour, size and duration is a token.
5. A new screen draws all of its states, in both themes and at every window size.
6. The diff holds only what the task needed, with comments that explain decisions, not the change.

## Google's Android skills

`.agents/skills/` holds Google's official Android skills, unmodified (Apache-2.0; its `README.md` names the release).
Before work in an area, read that skill's `SKILL.md`; where it disagrees with this file, this file wins (last column).

| Skill | Read it before you… | This project differs |
|---|---|---|
<!-- <opt:ui_components> -->
| `navigation-3` | add a destination, a scene, a deep link or a result | register with `navGraph { entry<Key>(…) }` and navigate through `AppNavigator`; a feature never builds a `NavDisplay` or sets its transitions (`nav-fade`) |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| `navigation-3` | add a destination, a scene, a deep link or a result | register with `navGraph { entry<Key>(…) }` and navigate through `AppNavigator`; the one `NavDisplay` is in `:core:navigation`, and list-detail and sheets go there as a `SceneStrategy`, never in a feature (`nav-fade`) |
<!-- </opt:!ui_components> -->
| `navigation-event` | handle Back anywhere but a screen pop | `NavigationBackHandler` only, never `BackHandler` or the Activity's dispatcher (`legacy-back`) |
| `adaptive` | build or change any screen | no `NavigationSuiteScaffold` or other Material (`adaptive-material-scope`); `LazyVerticalGrid`, not `Grid` or `FlexBox`, and `AppTheme.containerClass`, `posture` and `input`, not `MediaQuery`, all experimental in Compose 1.12 (`experimental-api`, `adaptive-mediaquery`); screenshots with Roborazzi |
| `edge-to-edge` | touch insets, the keyboard or the system bars | pad scrolling content inside the scroll, never on the list's parent (`scroll-chrome`) |
| `jetpack-compose-performance` | change a composable, a modifier chain or a lazy list, and again before you call it done: run its audit | read the category's `references/*.md` before a fix; `derivedStateOf`, not `computedStateOf` (Compose 1.12); state stays on `collectAsStateWithLifecycle()`; `List`, `Set` and `Map` are stable through `config/compose-stability.conf`, so never `ImmutableList` |
| `android-profiler`, `anr-audit-and-fix` | chase jank, an ANR, slow start-up or memory | release builds only (docs/PERFORMANCE.md, "Measuring"); the profiler's `trace_processor` script downloads Google's binary on its first run |
<!-- <opt:ui_components> -->
| `testing-setup` | add tests or test infrastructure | JVM tests with fakes and `MainDispatcherRule`, Robolectric for Compose; screenshots with Roborazzi, rendered from previews; never re-record a golden to make a test pass |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| `testing-setup` | add tests or test infrastructure | JVM tests with fakes and `MainDispatcherRule`, Robolectric for Compose; screenshots with Roborazzi, rendered from previews in `:core:designsystem` and in each feature that applies the screenshots convention with its first screen (docs/ARCHITECTURE.md, "Testing"); never re-record a golden to make a test pass |
<!-- </opt:!ui_components> -->
| `r8-analyzer` | change keep rules or chase app size | upstream ships no `scripts/`: run `./gradlew :app:analyzeProdReleaseR8Config` (one task per variant), then follow its manual path |
| `android-intent-security`, `android-permissions-security` | touch the manifest, deep links, notifications or permissions | external links become typed keys through a `DeepLinkRoute`; no exported component without a reason |
| `agp-9-upgrade` | bump AGP or Gradle | the project is already on AGP 9 with built-in Kotlin |
| `android-cli` | look up an API (`android docs`), run the app or inspect a screen | never run its install line, which pipes a downloaded script into a shell: ask the person to install `android` |
<!-- <opt:auth> -->
| `restore-credentials`, `verified-email` | change sign-in or sign-up | the flows live in `:feature:auth`, the credentials in `:data:auth` |
<!-- </opt:auth> -->
<!-- <opt:!auth> -->
| `restore-credentials`, `verified-email` | add sign-in or sign-up | credentials belong in a `data/` module behind an interface, the flow in a feature |
<!-- </opt:!auth> -->
<!-- <opt:media> -->
| `camerax` | add capture inside the app | `:core:media` hands photos and video to the system camera and needs no CameraX; add it only for in-app capture |
<!-- </opt:media> -->
<!-- <opt:!media> -->
| `camerax` | add capture inside the app | for a single photo the system camera needs no CameraX; add it only for in-app capture |
<!-- </opt:!media> -->
| `media3-cast-integration` | add Cast | the Cast button is a design-system component, with no Material (`material`) |
| `litert-lm`, `litert-runtime`, `ml-kit-genai-prompt-api`, `appfunctions` | run a model on the device, or offer an action to the system and its agents | a skill's `AndroidViewModel` becomes an `MviViewModel` and its screen a route; inject `AppDispatchers`; an app function calls a `data/` repository, never a ViewModel |
| `glance-canonical-layouts`, `wear-compose-m3`, `wear-widgets` | build a home-screen widget, or a Wear OS app or widget | each goes in a module of its own with that platform's components (Glance, Wear Compose), never imported by the phone app's modules; data still comes from `data/` repositories |
| `android-for-cars-parked-apps`, `display-glasses-with-jetpack-compose-glimmer`, `projected-hardware` | run the app parked in a car, or build for Android XR glasses | the app is already resizable and free of orientation locks; a glasses UI is a surface of its own, kept out of `:core:designsystem` |
| `engage-sdk-integration`, `play-billing-library-version-upgrade`, `play-policy-insights` | publish to Play's Engage surfaces, add or upgrade Play Billing, or submit to Play (run its audit first) | Play calls live in a `data/` module behind an interface, so screens and tests never see the SDK |

Left out on purpose: `styles` (it needs an alpha Compose and experimental opt-ins), and the XML-views and Leanback
migrations (this project has neither). To move every skill to a newer release, run
`python3 tools/android_skills.py --tag <tag>` and review the diff; it clones that release and never runs what it
downloads.

## The detailed docs

- @docs/ARCHITECTURE.md: modules, MVI, navigation, data, storage, app services, guards, variants, tests, form factors.
- @docs/PERFORMANCE.md: the frame budget, every smoothness rule with its reason and number, and how to measure.
- @docs/DESIGN-SYSTEM.md: the theme's token slots, the component contract, the rule index and templates.
<!-- <opt:perfhud> -->
- [docs/PERFORMANCE-OVERLAY.md](docs/PERFORMANCE-OVERLAY.md): the live performance overlay in dev builds.
<!-- </opt:perfhud> -->
