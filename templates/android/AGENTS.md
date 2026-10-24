# Working on BaseApp

Instructions for AI coding agents (Claude Code, Codex, Cursor, Copilot, Gemini and the rest).
Read all of this before your first change. It is short on purpose, and every rule here is one
that a generated project has been broken by before.

If you are a person, the README is for you. This file is the same facts written as instructions.

---

## 1. What this project is

A multi-module Jetpack Compose app, generated from a template. MVI, Hilt, Ktor, Navigation 3, and
a design system of its own with no Material underneath. It builds, tests and lints clean as
generated. Keep it that way. A change that turns the build red is not finished.

```
app/          composition root: wiring only, no logic
catalog/      a second app showing every design-system component (if present)
core/         infrastructure. Never depends on data/ or feature/
data/         one business domain each: DTOs, mappers, repository. Never depends on another data/
feature/      screens only. Never depends on another feature/
build-logic/  convention plugins, and the guards that enforce the rules below
```

## 2. Build and verify

Gradle runs on Android Studio's bundled JDK (JBR) or any JDK 17+. Do not add a toolchain, a
`gradle-daemon-jvm.properties` or a JDK download. The code targets Java 17 bytecode.

```bash
./gradlew :app:assembleDevDebug        # it compiles, about a minute
./gradlew test                         # unit tests
./gradlew build                        # everything: all variants, tests, lint, the guards
./gradlew :app:installDevDebug         # run it
```

Before you say a task is done, run `./gradlew build`, or at least `assembleDevDebug` plus the
tests of the modules you touched, and read the output. If something fails, report the failure.
Do not report success on a build you did not run.

## 3. The rules the build enforces

These fail the build. Working around them is never the fix.

- **No Material.** Any `androidx.compose.material*` import fails `verifyCodeRules`. Build from
  `:core:designsystem`, and from `foundation` and `ui` when it lacks something.
- **Layering.** `feature → feature` and `data → data` fail `verifyModuleDependencies`. When two
  features need the same thing, it belongs in `core/` or in a `data/` module both depend on.
- **Copy lives in `strings.xml`.** A literal passed to any copy parameter (`text`, `title`,
  `confirmLabel`, `contentDescription` and the rest), either branch of an `if` written into one,
  or a `?: "…"` fallback turned into a `UiText` fails the build in features, `:app`,
  `:core:designsystem` and `:core:ui`. Use `stringResource(R.string.x)`, or `UiText.of(R.string.x)`
  (`UiText.plural` for a count) from a ViewModel.

## 4. How a feature is shaped

Copy the reference feature (`feature/sample` + `data/sample`) if it exists. Otherwise run the
scaffolder, which writes the same shape:

```bash
python3 path/to/generator/add_feature.py orders --project . --tab
```

By hand, a feature is:

1. A repository **interface** plus a `Default…` implementation, bound in a Hilt `@Module`.
2. A contract: an `@Immutable` state, a sealed event, and a sealed effect.
3. A ViewModel extending `MviViewModel`. State changes go through `setState`, one-off things
   (navigate, show a message) through `emit` or `showMessage`.
4. A **stateless** screen, `Screen(state, onEvent)`, and a route that connects it to the
   ViewModel through `MviScreen`. Previews call the screen, never the route.
5. A `@Serializable` key implementing `AppNavKey`, registered twice with `@Provides @IntoSet`:
   `navGraph { entry<Key> { … } }` and `navKeys { subclass(…) }`. Forgetting the second one
   works until the process is killed in the background, then comes back on the wrong screen.
6. Tests for the ViewModel with a fake repository and `MainDispatcherRule`, like
   `SampleListViewModelTest`.

Every screen that loads something draws all of its states: loading (a skeleton shaped like the
content), empty, error with retry, and offline. The `LoadState` type exists so none of them is
left out.

### State, effect or message?

| It is… | Use | Example |
|---|---|---|
| Something the screen renders, now or after rotation | `setState { copy(…) }` | the list, a field's error, a loading flag |
| Something that happens once and must not replay | `emit(…)` | navigate, open the share sheet, close the screen |
| A transient note the user may miss without harm | `showMessage(…)` | "Saved", "Could not refresh" |

Effects are collected only while the screen is started and on `Main.immediate`, so a navigation
emitted while the app is backgrounded waits for the user to come back rather than being lost.

### A screen that takes an argument

The key carries an id, never a model. The id reaches the ViewModel through Hilt assisted injection,
at construction — not through a `LaunchedEffect` that sends a "load" event after the fact. Every
destination has its own ViewModel store, so this ViewModel belongs to exactly one open screen:

```kotlin
@Serializable
data class OrderKey(val orderId: Long) : AppNavKey

@HiltViewModel(assistedFactory = OrderViewModel.Factory::class)
class OrderViewModel @AssistedInject constructor(
    private val repository: OrderRepository,
    @Assisted private val orderId: Long,
) : MviViewModel<OrderState, OrderEvent, OrderEffect>(OrderState()) {

    @AssistedFactory
    interface Factory {
        fun create(orderId: Long): OrderViewModel
    }

    init {
        onEvent(OrderEvent.Retry)
    }
    …
}

@Composable
fun OrderRoute(
    orderId: Long,
    viewModel: OrderViewModel = hiltViewModel<OrderViewModel, OrderViewModel.Factory>(
        creationCallback = { factory -> factory.create(orderId) },
    ),
) { … }
```

Retry uses the injected id, so it works even when the first load failed and there is no item on
screen to read an id from.
<!-- <opt:sample> -->
`SampleDetailViewModel` is the working example.
<!-- </opt:sample> -->

### Recipes

- **A new tab:** add a `ShellTab` to `AppDestinations.tabs`. Each tab keeps its own back stack,
  scroll position and ViewModels; a switch nudges the incoming tab in and cross-fades nothing.
- **Keep typed input across process death:** inject `SavedStateHandle` into the ViewModel and hold
  each field the user types as
  `savedStateHandle.saveable(KEY, saver = TextFieldState.Saver) { TextFieldState() }` — not
  loaded data, which is fetched again, and never a password.
- **Search as you type:** `launchLatest(key, debounce)` cancels the previous request, so a
  slow answer for an old query can never overwrite a newer one.
<!-- <opt:search> -->
  `SearchViewModel` shows it.
<!-- </opt:search> -->
- **Data that belongs to the signed-in user:** a table in `:core:database`, or a preferences file
  of its own from `DataStoreFiles.preferences("name")`, with a `SessionScopedStore` bound
  `@IntoSet` that empties it, so sign-out clears it. Settings that outlive a session go in
  `@SettingsDataStore`.
<!-- <opt:paging> -->
- **A paged list:** return `Flow<PagingData<T>>` from the repository, `cachedIn(viewModelScope)` in
  the ViewModel, and render it with `AppPagingList`, which already draws loading, error, empty
  and the load-more footer. `feature/feed` is the example.
<!-- </opt:paging> -->
<!-- <opt:network> -->
<!-- <opt:database> -->
- **Show saved data at once, then the latest:** make the database the source of truth. The
  repository exposes the table as a `Flow`, and `refresh()` writes the server's answer into it.
  GETs are otherwise cached by the HTTP cache, as the server's headers say.
<!-- <opt:sample> -->
  `OfflineFirstSampleRepository` is the example.
<!-- </opt:sample> -->
<!-- </opt:database> -->
- **Show a failure:** `error.toUiText(UiText.of(R.string.x))` (`AppError.toUiText` in `:core:ui`): the
  server's own `message` when it sent one, the app's sentence for offline or unreachable, otherwise
  your fallback. The app's wording comes from resources so it can be translated.
<!-- </opt:network> -->
- **A new string:** the feature's own `res/values/strings.xml`. Copy typed into Kotlin fails the
  build. If the project ships other languages (`values-es/` and the like beside `values/`), add
  the translation to each one too: lint fails a build with a string missing from a language.
- **A new dependency:** a version and alias in `gradle/libs.versions.toml`, then `libs.x` in the
  module. Take the latest stable release, not an alpha.

### Every screen works on every window

Android runs on phones, foldables, tablets, desktop windows and cars. A screen built for a phone
held upright is not finished.

- Decide layout from `AppTheme.windowClass` (the window's size class) and `AppTheme.containerClass`
  (the pane's), never from orientation, screen size or device type. Never lock orientation: lint
  fails the build.
- Give content a width to stop at: `AppScaffold(contentMaxWidth = AppTheme.layout.readableMaxWidth)`
  for text and lists, `formMaxWidth` for a form. Full-width lines on a tablet are unreadable.
- A list with a detail registers both halves: `entry<ListKey>(pane = NavPane.List)` and
  `entry<DetailKey>(pane = NavPane.Detail)`. They share the window when it is wide; do not build
  your own two-pane layout for it.
- Two halves of one screen use `AppTwoPane`; a set of cards uses
  `LazyVerticalGrid(GridCells.Adaptive(AppTheme.layout.gridMinCardWidth))`.
- Check the screen at Compact, Medium and Expanded width, and in landscape. On the emulator,
  `adb shell wm size 1600x2560` plus `adb shell wm density 320` is a tablet; `wm size reset` and
  `wm density reset` undo it.

## 5. The base components are a starting point

`:core:designsystem` ships about eighty components, four design styles (`AppDesignStyle`) and
four motion styles (`AppMotionStyle`). **They are there for you to change.** Treat them as a
reference, not as a fixed kit you have to use exactly as delivered.

A design style is a bundle of tokens read through `AppTheme.style`: corner radii, button and chip
shapes, how cards, fields and the tab bar are drawn, border weight, label casing, the surface tone
(`SurfaceTone` — cool greys, brand-tinted, paper, cream) and the type voice (`TypeVoice`). The
four enums are named starting points; editing `AppDesignStyle.style()` or adding a fifth is the
intended way to give the app its own look everywhere at once.

- Make the app look like itself. Adjust tokens first (`Palette.kt`, `AppDesignStyle`, `AppShapes`,
  `AppSpacing`, `AppTypography`), then edit the components. A finished app that looks exactly
  like the template's catalog was not designed.
- Edit a component in place when the design calls for it. Do not wrap it in a new component just
  to avoid touching it, and do not fork a copy per screen.
- When nothing fits, build something new in `:core:designsystem` from `foundation` and `ui`,
  reading the theme through `AppTheme.colors`, `AppTheme.spacing` and the rest. Never hard-code a
  hex colour or a `dp` in a feature.
- Keep what makes them solid when you change how they look: 48dp touch targets, content
  descriptions on icon-only controls, contrast in both themes, reduce-motion handling, and the
  loading states that do not change a button's size.
- Changed a colour or a surface tone? Run `./gradlew :core:designsystem:test`.
  `PaletteContrastTest` checks every text colour against its background in every style and both
  themes against WCAG AA.
- Animate with the theme's tokens (`AppTheme.springs`, `rememberAppTransitions()`), never a raw
  `tween(300)`. Animate `graphicsLayer` properties (alpha, offset, scale) rather than size, so
  nothing around the animated element moves.

## 6. Smooth and light

The target is 120 Hz on a budget phone: 8.33 ms a frame. Each rule below is one way an app on this
template has lost frames or memory before, and each is explained, with the number it protects, in
[docs/PERFORMANCE.md](docs/PERFORMANCE.md). Read that before you change a screen's motion, a list or
the start-up path. The part after the dash is what enforces the rule: a `verifyCodeRules` rule id, a
lint id, a build task, a test or a measured limit; *review* means nothing checks it automatically.

**Measuring**

<!-- <opt:performance> -->
- Judge smoothness only on `devRelease` or `devBenchmarkRelease`, compiled with its profile, on a real phone; never quote a debug build's numbers. — *review*
- A change to motion, a list or start-up is done when, on a release build, `tools/perf/perf.sh --gate solid` fails on no limit but those [Where the template stands](docs/PERFORMANCE.md#where-the-template-stands) lists as over, none of those is worse than it lists there, and against the previous build measured the same day `--baseline <previous run>/summary.json --gate smoke` passes and no other row got worse. — `perf.sh --gate` and its table (`--gate solid` checks the limits only; it never compares builds)
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ - Judge smoothness only on `devRelease`, compiled with its profile, on a real phone; never quote a debug build's numbers. — *review* -->
<!--~ - A change to motion, a list or start-up is done when, measured on a release build against the previous build the same day on the same phone, no journey it touches got worse. — *review* -->
<!-- </opt-else:performance> -->
<!-- <opt:perfhud> -->
- Measure with the performance overlay hidden: a number taken with it shown is not the app's. — *review*
<!-- </opt:perfhud> -->
- Never call `Debug.getMemoryInfo` or `dumpsys meminfo` while the app animates or inside a measured journey. — *review*

**Composition**

- Read animated, dragged or scrolled values only in `offset { }`, `layout { }`, `graphicsLayer { }`, a draw block or a `Modifier.Node`; hold an animation as `val x = animate…()` and read `x.value` there, never `by`. — `animated-delegate`, lint `FrequentlyChangingValue`, `UseOfNonLambdaOffsetOverload`
- A composable that receives a value changing every frame takes `() -> T`, never `State<T>`. — `state-param`
- Write custom modifiers as `Modifier.Node`; never `Modifier.composed` or a `@Composable` modifier factory. — `composed`
- Collect state with `collectAsStateWithLifecycle()`, never `collectAsState()`. — `collect-as-state`
- `UiState` holds read-only collections and `val`s built by the ViewModel, and no per-frame values; never `.map`/`.filter` state in composition without `remember(keys)`. — `uistate-mutable`, `api-immutable`
- No parameter of `:core:designsystem`, `:core:ui` or `:core:navigation` compared by identity; a baseline entry needs a `# reason`; never `ImmutableList`, never `@Immutable` on a model carrying a large list into a lazy layout. — `checkComposeStability`
- Never write state from a `SideEffect` that the same composition reads, and never give a static composition local or a screen's lifecycle owner a new value on a transition frame. — *review*
- Lay out from `AppTheme.windowClass` and `AppTheme.containerClass`; read the exact window size only from `LocalWindowMetrics` inside layout or draw. — `adaptive-screen-metrics`, `adaptive-orientation`, `adaptive-device-type`
- Compose lint is an error in every Compose module (`Compose.kt` in `build-logic` lists the ids); never suppress one to pass the build. — lint
- A new screen or component that animates adds a recomposition test: its journey through `RecompositionCounter` and `framesUntilIdle`, then `assertQuietBetweenEnds()`. — the test

**Lists**

- Every lazy `items(…)` has a stable `key`; a scope that mixes kinds of item gives each a `contentType`. — `lazy-key`, `lazy-content-type`
- Pad scrolling content inside the scroll: `contentPadding = chromeContentPadding(…)`, or `.verticalScroll(…).padding(chromeContentPadding(…))`; inside an `AppSheet`, the sheet's `contentPadding`; never pad the parent, never read `LocalBottomChrome.current.value` in composition. — `scroll-chrome` (vertical lists, grids and `verticalScroll` in features)
- Every lazy list, grid, `verticalScroll` and `horizontalScroll` passes `flingBehavior = AppScrollDefaults.flingBehavior()`, a pager `AppScrollDefaults.pagerFlingBehavior(state)`. — `scroll-fling`
- Call `ReportScrollEdge(state)` beside a screen's primary list, and `ScrollToTopOnReselect(state)` on a tab's root screen. — *review*
- No `SubcomposeAsyncImage`, `BoxWithConstraints` or other subcomposition in a feature's rows. — `subcompose-image`, `adaptive-box-constraints`
- Animate rows only with `appAnimateItem()` (an extension on the row's `LazyItemScope`: `modifier = appAnimateItem()`; pass a modifier to chain) on keyed rows; never `animateContentSize()` on a list item. — *review*
- A loading list is the real rows inside `AppRedacted`, with its own `LazyListState` and the loaded list's padding, swapped in one frame: no `AnimatedContent`, no cross-fade. — *review*

**Screens and navigation**

- A feature never sets `NavDisplay` transitions or touches a back stack: it registers `entry<Key>(…)` and navigates through `AppNavigator`. — `nav-fade` (the `NavDisplay` transition specs); *review* (the back stack)
- A destination's first composition is final: seed its state from a repository's in-memory `peek(id)` when the data is there, and load secondary sections after `LocalNavigationSettled.current()` turns true, read in an effect. — *review*
- Never add a full-screen background under a screen: the navigation entry paints it and `AppScaffold` paints none on top. — *review*
- Never put alpha below 1 on a full-screen layer, and never cross-fade two full screens: scrims are drawn rectangles, short fades use `CompositingStrategy.ModulateAlpha`. No exception in features or components; the shell in `:core:navigation` has two, the back card's texture and `HostCrossfade` between hosts. — `layer-alpha-fill` (layer alpha on a full-screen chain); *review* (cross-fades)
- Handle Back with `NavigationBackHandler` (overlays at overlay priority), never `BackHandler`, `PredictiveBackHandler` or the Activity's dispatcher. — `legacy-back`, `verifyManifestRules`

**Sheets**

- Keep `AppSheet` composed and drive `visible`; never put it in an `if`, a `when` or a lazy item. — `sheet-conditional`
- A sheet's content is one root, a lazy list or `SheetColumn`, taking `contentPadding`; no `imePadding()` or IME insets inside a sheet. — `sheet-root`, `sheet-ime-padding`
- `AppSheet(modifier = …)` carries test tags, semantics and focus only. — `sheet-modifier`
- Inside the sheet system: no `Dialog` or `Popup` window, no shadow modifier or elevation, no `dispatchRawDelta` or `settle`. Outside it, a `when` over a sheet type has an `else`, and features never opt into `ShellSeamApi`. — `sheet-window`, `sheet-shadow`, `sheet-raw-delta`, `sheet-when-else`, `shell-seam`
- A `SheetDetent.Custom` resolver reads no state. — *review*
<!-- <opt:coil> -->

**Images**

- Show remote images with `AppNetworkImage`, or `AsyncImage` with painters; never `SubcomposeAsyncImage`. — `subcompose-image`
<!-- <opt:network> -->
- The app has one image loader, set in `BaseAppApplication` on the app's `OkHttpClient`; never build a second. — *review*
<!-- </opt:network> -->
<!-- <opt-else:network> -->
<!--~ - The app has one image loader, Coil's own singleton; never build a second. — *review* -->
<!-- </opt-else:network> -->
<!-- </opt:coil> -->

**Motion**

- Every spring and decay comes from `AppTheme.springs`, fades from `springs.fadeIn()`/`fadeOut()`; no raw colour, and no raw `dp` but `0.dp` and `1.dp`. — `raw-tween`, `raw-color`, `raw-dp`
- A motion a finger released ends on a spring seeded with the release velocity; interrupt with `animateTo`, never `snapTo` what the user can see. — *review*
- Every tappable control says its feedback: `Modifier.pressable(onClick, feedback = PressFeedback.…)`; no ripple, no bare `clickable`. — `bare-clickable`, `ripple`
<!-- <opt:performance> -->
- Nothing animates while idle: a loop pauses while `LocalTabVisible.current.isVisible` is false (read in an effect or a draw block), steps on a timer in a `graphicsLayer()` of its own, and stops or slows under reduce motion (decoration stops; `AppActivityIndicator` slows); ambient decoration winds down after a few seconds and votes `FrameRateCategory.Normal`. — gate `idle.frames = 0`
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ - Nothing animates while idle: a loop pauses while `LocalTabVisible.current.isVisible` is false (read in an effect or a draw block), steps on a timer in a `graphicsLayer()` of its own, and stops or slows under reduce motion (decoration stops; `AppActivityIndicator` slows); ambient decoration winds down after a few seconds and votes `FrameRateCategory.Normal`. — *review* -->
<!-- </opt-else:performance> -->
- Motion that changes only layer properties sets `preferredFrameRate(FrameRateCategory.High)` while it moves and drops it after; never a fixed rate. — *review*
- A shape clipped or filled every frame of a motion is a rounded rectangle, never a path of cubics; nothing allocates in a draw, layer or placement block. — *review*
<!-- <opt:liquidglass> -->

**Glass**

- With the glass foundation: only chrome in a `GlassStage`'s `chrome` slot samples the scene; glass inside a screen is `Modifier.glassSurface`. (The shell draws no glass chrome yet.) — the scene's run-time check
- Never alpha below 1, `AnimatedVisibility` or `saveLayer` around a sampling surface; never read state inside backdrop effects; remember the backdrop chain once. — *review*
<!-- </opt:liquidglass> -->

**Keyboard**

- Read the keyboard in layout or placement (`imePadding()`, `WindowInsetsRulers.Ime`), never `WindowInsets.ime` in composition; `chromeContentPadding()` already counts it. — *review*

**Start-up and build**

<!-- <opt:performance> -->
- No I/O and no eager singletons in `Application.onCreate` or the first screen; start what the first frame does not need after it. — gates `coldstart` 380 ms, `coldstart_installed` 400 ms
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ - No I/O and no eager singletons in `Application.onCreate` or the first screen; start what the first frame does not need after it. — *review* -->
<!-- </opt-else:performance> -->
- Decide signed in or out from `AuthTokenStore.isAuthenticated`, never by reading the tokens. — *review*
- Never add `preloaded_fonts` meta-data to the manifest. — *review*
<!-- <opt:performance> -->
- After a journey changes, regenerate the baseline profile (`./gradlew :app:generateBaselineProfile` on a phone with gesture navigation, then `tools/perf/split_profile.py`) and check the file is not empty. — *review*
<!-- </opt:performance> -->
- Never add `-assumenosideeffects` for Compose's `traceEventStart`. — *review*
<!-- <opt:devtools> -->
- Dev tooling reaches `:app` only as `debugImplementation` and `"devImplementation"`. — `verify<Variant>CarriesNoDevTooling`
<!-- </opt:devtools> -->
- One Preferences DataStore file per concern: inject `DataStoreFiles` and call `preferences("name")` once per file from a `@Singleton` provider (two instances of one file corrupt it). — *review*
- `./gradlew build` runs R8 for every release-like variant at once; on a machine short of memory, build the variant you need or pass `--max-workers=2`, and set the heap in `gradle.properties`, never with `-Dorg.gradle.jvmargs`. — *review*

## 7. Code quality: write like the person who owns this code

This is what separates a good agent change from a typical one. Follow it exactly.

**Comments**

- Comment *why*, never *what*. If a comment repeats the code in English, delete it.
- Most lines need no comment. A comment belongs on the decision a reader would otherwise
  question: a workaround, a non-obvious constraint, a trade-off, a bug it prevents.
- Write in the same voice as the comments already here: full sentences, no filler.
- Never write comments about the change itself: no "Added X", "Updated to…", "New:", "Fixed",
  "As requested", "Step 1:", and no ticket or conversation references. That history belongs in
  the commit message.
- No commented-out code, no banner comments, no `// TODO` unless the user asked for one.
- KDoc on public API only when the name and signature do not already say it all.

**Changes**

- Match the surrounding code: naming, file layout, argument order, how state and errors are
  handled. Read two neighbouring files before writing a new one.
- Keep the diff to what was asked. Do not reformat, rename or "tidy" code you were not asked to
  touch, and do not reorder imports in files you did not otherwise change.
- No placeholders presented as finished work: no stubbed repository, fake delay, `TODO()` body
  or hard-coded list standing in for a real call unless the user asked for a mock. If something
  cannot be done, say so.
- No new library without a reason you can state. Every version goes in
  `gradle/libs.versions.toml`, never inline.
- Do not delete or weaken a test to make the build pass. Fix the code, or explain why the test
  is wrong.
- Small, named functions over long ones. No `!!`, no `runBlocking`, no `GlobalScope`, no
  `Dispatchers.IO` inside a class (inject the dispatcher).

## 8. Where things live

| You want to… | Go to |
|---|---|
| Change a backend URL or a version number | `build-logic/…/AppConfig.kt`, the only place the build reads them |
| Add or reorder a tab | `app/…/ui/AppDestinations.kt` |
| Change the look app-wide | `core/designsystem/…/theme/` and the `AppTheme(…)` call in `app/…/ui/AppRoot.kt` |
| Add an icon | `core/designsystem/…/icon/AppIcons.kt` |
| Call an API | a `data/` module, through `NetworkClient` |
| Store something | `core/database` for the app's data, `core/datastore` for preferences: a file per concern, cleared on sign-out through `SessionScopedStore` when it belongs to the person |

## 9. Google's Android skills

`.agents/skills/` holds Google's official Android skills (Apache-2.0, unmodified; `.agents/skills/README.md` says
which version). Before work in one of these areas, read that skill's `SKILL.md` and follow it. Where it disagrees
with this file, this file wins; the known differences are in the last column.

| Skill | Read it before you… | This project differs |
|---|---|---|
| `navigation-3` | add a destination, scene, deep link or result | register in `navigation/<Feature>Navigation.kt` with `navGraph { entry<K>(pane = …) }`; never call `NavDisplay`, a scene strategy or transition metadata from a feature |
| `navigation-event` | handle Back anywhere outside a screen pop | `NavigationBackHandler` only, one state per handler, overlays at overlay priority; never `BackHandler` (the build fails) |
| `adaptive` | build or change any screen | no `NavigationSuiteScaffold` or Material components: the shell's bar, rail and sidebar; panes through `NavPane`; `LazyVerticalGrid`, not `Grid`/`FlexBox` (experimental in Compose 1.12); `AppTheme.containerClass`, `posture` and `input`, not `MediaQuery`; Roborazzi, not `com.android.compose.screenshot`; `AppLargeTitleTopBar`, not M3 scroll behaviours |
| `edge-to-edge` | touch insets, the keyboard or system bars | pad scrolling content with `chromeContentPadding()` as `contentPadding`, never on the list's parent |
| `testing-setup` | add tests or test infrastructure | fakes in `testFixtures`; screenshots with Roborazzi at the sizes in `docs/ADAPTIVE.md`; never re-record goldens to make a test pass |
<!-- <opt:performance> -->
| `android-profiler` | investigate jank, startup or memory | release builds only, through `tools/perf/perf.sh`; its `trace_processor` script downloads Google's binary on first run |
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ | `android-profiler` | investigate jank, startup or memory | release builds only (`devRelease`); its `trace_processor` script downloads Google's binary on first run | -->
<!-- </opt-else:performance> -->
| `r8-analyzer` | change keep rules or chase app size | upstream ships no `scripts/`: run `./gradlew :app:analyzeProdReleaseR8Config` (one task per variant; the report is `app/build/reports/r8/`), then follow its manual path |
| `android-intent-security`, `android-permissions-security` | touch the manifest, deep links, notifications or permissions | links land in `LinkActivity` and become typed keys in a `DeepLinkRoute` |
| `agp-9-upgrade` | bump AGP or Gradle | the project is already on AGP 9 with built-in Kotlin |
| `jetpack-compose-performance` | change a composable, a modifier chain or a lazy list, and again before you call that change done: run its audit over every composable you touched | read the category's `references/*.md` before you change what it flags, and `docs/PERFORMANCE.md`, this project's own reading of the same rules; Compose is 1.12, so `derivedStateOf`, not `computedStateOf`; ViewModel state stays on `collectAsStateWithLifecycle()`, because `MviViewModel.observe` runs its flows only while a screen collects; `List`, `Set` and `Map` are in `config/compose-stability.conf`, so never `ImmutableList`, and never `@Immutable` on a model that carries a large list into a lazy layout |
| `android-cli` | look up an API (`android docs`), run the app, inspect a screen (`android layout`, `android screen`) | never run its install line, which pipes a downloaded script into a shell: if `android` is not on the PATH, ask the person to install it; measure performance on release builds, never through it |
<!-- <opt:auth> -->
| `restore-credentials`, `verified-email` | change sign-in or sign-up | flows live in `:feature:auth`, credentials in `:data:auth` |
<!-- </opt:auth> -->
<!-- <opt:media> -->
| `camerax` | add capture inside the app (a viewfinder, `ImageCapture`, `VideoCapture`) | `:core:media` hands photos and video to the system camera (`TakePicture`, `CaptureVideo`) and needs no CameraX; add it only for in-app capture |
<!-- </opt:media> -->

Not vendored on purpose: `styles` (experimental; the design system stays on stable foundation APIs). Before a Play
submission, add Google's `play-policy-insights` audit with `android skills` and run it.

## 10. Before you say it is done

1. `./gradlew build` is green, or you have said exactly what fails and why.
2. Every new screen has loading, empty, error and offline states, and works in dark theme.
3. A change to Compose UI has been through the `jetpack-compose-performance` audit (§9), and its findings are
   fixed.
4. Every new string is in `strings.xml`; every new colour, size and duration is a token.
5. New logic has a test beside the existing ones; no test was deleted or weakened.
6. The diff contains only what the task needed, with comments that explain decisions, not the
   change.
<!-- <opt:performance> -->
7. A change to motion, a list or start-up, on a release build: `tools/perf/perf.sh --gate solid` fails on
   no limit but those [Where the template stands](docs/PERFORMANCE.md#where-the-template-stands) lists as
   over, none of those is worse than it lists there, and
   `--baseline <previous run>/summary.json --gate smoke` passes against the previous build measured the
   same day (§6).
8. Any new motion has a recording from `tools/perf/record.sh`.
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ 7. A change to motion, a list or start-up was measured on a release build against the previous build, -->
<!--~    the same day on the same phone, and no journey it touches got worse (§6). -->
<!-- </opt-else:performance> -->

## 11. Never

- Commit `keystore.properties`, `keys/`, `local.properties` or a real `google-services.json`.
- Print tokens, passwords or personal data to the log.
- Add Material, a second DI framework, a second navigation library or a second image loader.
- Edit files under `build/`, or generated sources.
- Leave the build red, or say it is green when you did not run it.
