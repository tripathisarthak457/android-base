# AGENTS.md rules: smoothness and weight (draft for `templates/android/AGENTS.md`)

The block between the two rules below is the text to merge into the template's `AGENTS.md`, as a new
section after "5. The base components are a starting point". One line per rule; the part after the
dash is what enforces it (a `verifyCodeRules` rule id, a lint id, a build task, a test or a gate row);
*review* means nothing checks it automatically. Every rule is explained, with its numbers, in
`docs/PERFORMANCE.md`. The merge notes at the end are for whoever merges, not for the template.

---

## 6. Smooth and light

The app has to hold 120 Hz on a budget phone (8.33 ms a frame). These rules are why it does. Each one
is explained, with the number it protects, in [docs/PERFORMANCE.md](docs/PERFORMANCE.md); read that
before you change a screen's motion, a list or the start-up path.

**Measuring**

- Judge smoothness only on `devRelease` or `devBenchmarkRelease`, compiled with its profile, on a real phone, with the overlay hidden; never quote a debug build's numbers. — *review*
- A change to motion, a list or start-up is done when `tools/perf/perf.sh --gate solid` passes on a release build and, against the previous build measured the same day, `--baseline <previous run>/summary.json --gate smoke` passes and no other row got worse. — `perf.sh --gate` (`--gate solid` checks the limits only; it never compares builds)
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
- Compose lint is an error in every Compose module (`Compose.kt` lists the ids); never suppress one to pass the build. — lint
- A new screen or component that animates adds a recomposition test: its journey through `RecompositionCounter` and `framesUntilIdle`, then `assertQuietBetweenEnds()`. — the test

**Lists**

- Every lazy `items(…)` has a stable `key`; a scope that mixes kinds of item gives each a `contentType`. — `lazy-key`, `lazy-content-type`
- Pad scrolling content inside the scroll: `contentPadding = chromeContentPadding(…)`, or `.verticalScroll(…).padding(chromeContentPadding(…))`; inside an `AppSheet`, the sheet's `contentPadding`; never pad the parent, never read `LocalBottomChrome` in composition. — `scroll-chrome` (vertical lists, grids and `verticalScroll` in features)
- Every lazy list, grid, `verticalScroll` and `horizontalScroll` passes `flingBehavior = AppScrollDefaults.flingBehavior()`, a pager `AppScrollDefaults.pagerFlingBehavior(state)`. — `scroll-fling`
- Call `ReportScrollEdge(state)` beside a screen's primary list, and `ScrollToTopOnReselect(state)` on a tab's root screen. — *review*
- No `SubcomposeAsyncImage`, `BoxWithConstraints` or other subcomposition in a feature's rows. — `subcompose-image`, `adaptive-box-constraints`
- Animate rows only with `appAnimateItem()` (an extension on the row's `LazyItemScope`: `modifier = appAnimateItem()`; pass a modifier to chain) on keyed rows; never `animateContentSize()` on a list item. — *review*
- A loading list is the real rows inside `AppRedacted`, with its own `LazyListState` and the loaded list's padding, swapped in one frame: no `AnimatedContent`, no cross-fade. — *review*

**Screens and navigation**

- A feature never sets `NavDisplay` transitions or touches a back stack: it registers `entry<Key>(…)` and navigates through `AppNavigator`. — `nav-fade` (the `NavDisplay` transition specs); *review* (the back stack)
- A destination's first composition is final: seed its state from `peek(id)` when the data is in memory, and load secondary sections after `LocalNavigationSettled.current()` turns true, read in an effect. — *review*
- Never add a full-screen background under a screen: the navigation entry paints it and `AppScaffold` paints none on top. — *review*
- Never put alpha below 1 on a full-screen layer, and never cross-fade two full screens: scrims are drawn rectangles, short fades use `CompositingStrategy.ModulateAlpha`. No exception in features or components; the shell in `:core:navigation` has two, the back card's texture and `HostCrossfade` between hosts. — `layer-alpha-fill` (layer alpha on a full-screen chain); *review* (cross-fades)
- Handle Back with `NavigationBackHandler` (overlays at overlay priority), never `BackHandler`, `PredictiveBackHandler` or the Activity's dispatcher. — `legacy-back`, `verifyManifestRules`

**Sheets**

- Keep `AppSheet` composed and drive `visible`; never put it in an `if`, a `when` or a lazy item. — `sheet-conditional`
- A sheet's content is one root, a lazy list or `SheetColumn`, taking `contentPadding`; no `imePadding()` or IME insets inside a sheet. — `sheet-root`, `sheet-ime-padding`
- `AppSheet(modifier = …)` carries test tags, semantics and focus only. — `sheet-modifier`
- In the sheet system: no `Dialog` or `Popup` window, no shadow modifier or elevation, no `dispatchRawDelta` or `settle`; a `when` over a sheet type has an `else`; features never opt into `ShellSeamApi`. — `sheet-window`, `sheet-shadow`, `sheet-raw-delta`, `sheet-when-else`, `shell-seam`
- A `SheetDetent.Custom` resolver reads no state. — *review*

**Images**

- Show remote images with `AppNetworkImage` or `AsyncImage` with painters; the app has one image loader, set in `BaseAppApplication` on the app's `OkHttpClient`. — `subcompose-image`, *review*

**Motion**

- Every spring and decay comes from `AppTheme.springs`, fades from `springs.fadeIn()`/`fadeOut()`; no raw colour, and no raw `dp` but `0.dp` and `1.dp`. — `raw-tween`, `raw-color`, `raw-dp`
- A motion a finger released ends on a spring seeded with the release velocity; interrupt with `animateTo`, never `snapTo` what the user can see. — *review*
- Every tappable control says its feedback: `Modifier.pressable(onClick, feedback = PressFeedback.…)`; no ripple, no bare `clickable`. — `bare-clickable`, `ripple`
- Nothing animates while idle: a loop pauses while `LocalTabVisible.current.isVisible` is false (read in an effect or a draw block), steps on a timer in a `graphicsLayer()` of its own, and stops or slows under reduce motion (decoration stops; `AppActivityIndicator` slows); ambient decoration winds down after a few seconds and votes `FrameRateCategory.Normal`. — gate `idle.frames = 0`
- Motion that changes only layer properties sets `preferredFrameRate(FrameRateCategory.High)` while it moves and drops it after; never a fixed rate. — *review*
- A shape clipped or filled every frame of a motion is a rounded rectangle, never a path of cubics; nothing allocates in a draw, layer or placement block. — *review*

**Glass**

- With the `liquidglass` foundation: only chrome in a `GlassStage`'s `chrome` slot samples the scene; glass inside a screen is `Modifier.glassSurface`. (The shell draws no glass chrome yet.) — the scene's `check`, at run time
- Never alpha below 1, `AnimatedVisibility` or `saveLayer` around a sampling surface; never read state inside backdrop effects; remember the backdrop chain once. — *review*

**Keyboard**

- Read the keyboard in layout or placement (`imePadding()`, `WindowInsetsRulers.Ime`), never `WindowInsets.ime` in composition; `chromeContentPadding()` already counts it. — *review*

**Start-up and build**

- No I/O and no eager singletons in `Application.onCreate` or the first screen; start what the first frame does not need after it. — gates `coldstart` 380 ms, `coldstart_installed` 400 ms
- Decide signed in or out from `AuthTokenStore.isAuthenticated`, never by reading the tokens. — *review*
- Never add `preloaded_fonts` meta-data to the manifest. — *review*
- After a journey changes, regenerate the baseline profile (`./gradlew :app:generateBaselineProfile` on a phone with gesture navigation, then `tools/perf/split_profile.py`) and check the file is not empty. — *review*
- Never add `-assumenosideeffects` for Compose's `traceEventStart`. — *review*
- Dev tooling reaches `:app` only as `debugImplementation` and `"devImplementation"`. — `verify<Variant>CarriesNoDevTooling`
- One Preferences DataStore file per concern: inject `DataStoreFiles` and call `preferences("name")` once per file from a `@Singleton` provider (two instances of one file corrupt it). — *review*
- Build the variant you need, or run `./gradlew build --max-workers=2`: four R8 tasks at once run out of a 3 GB heap. — *review*

---

## Merge notes (not part of the template text)

1. **Section numbers.** The block is written as section 6; the current sections 6-10 become 7-11.
   Renumbering breaks these numbered references, all inside `AGENTS.md` (staging `b21f0a981`):
   - "Before you say it is done", item 3 (`AGENTS.md:298`): "(§8)" becomes "(§9)", the Google's Android
     skills section.
   - The new block's own text names no section number.
   Nothing outside `AGENTS.md` cites it by number: the `.claude/skills/*/SKILL.md` stubs and
   `.agents/skills/README.md` name the section ("Google's Android skills"), and
   `generator/tests/test_vendored_skills.py` matches its heading with an optional number
   (`^## (?:\d+\. )?Google's Android skills`). Re-run that grep after merging.
2. **"Before you say it is done" gains two lines**, after item 3:
   - "A change to motion, a list or start-up has `perf.sh --gate solid` passing on a release build, and
     `--baseline <previous run>/summary.json --gate smoke` passing against the previous build measured the
     same day."
   - "Any new motion has a recording from `tools/perf/record.sh`."
3. **Stale lines in the current `AGENTS.md`** that contradict this block or the code:
   - §4 Recipes, "A new tab": "switching crossfades" (`AGENTS.md:138-139`). Tab switches never
     cross-fade: the incoming tab is nudged 12 dp and the outgoing one is parked at once
     (`core/navigation/…/shell/tabs/TabReveal.kt:28-34`).
   - §4 "Every screen works on every window": `AppTheme.windowSize` (`:180`) and `AppResponsiveGrid`
     (`:187`) do not exist on staging; the code and the adaptive rules use `AppTheme.windowClass` and
     `AppTheme.containerClass`.
   - §5, last bullet: "`AppTheme.motion`" (`:218`) does not exist; it is `AppTheme.springs`. The rest of
     the bullet (animate `graphicsLayer` properties rather than size) stays.
   - §4 "How a feature is shaped" and its table (`:69-70`, `:87-88`, `:140`) still name `updateState`,
     `emitEffect` and `persistState`, none of which exists on staging; `MviViewModel` has `setState`,
     `emit`, `launchLatest`, `launchExclusive` and `observe`. Not a smoothness rule, but an agent following
     it writes code that does not compile.
   - `docs/ARCHITECTURE.md` repeats the first three (`:255`, `:557`, `:584`, `:535`) and says ABI splits
     are enabled "only when the requested tasks assemble a release" (`:655-656`), while
     `build-logic/…/config/Flavors.kt:72-78` enables them only with `-PabiSplits=true`.
4. **Skills table, `jetpack-compose-performance` row (`:282`)** already carries the project's
   departures; keep it, and point "read the category's `references/*.md`" at `docs/PERFORMANCE.md` as the
   project's own reading of the same rules.
5. **Rules held back on purpose** (they wait for the owner's approval, see `PENDING.md`): holding
   decorative motion and new data until a slide lands, a push that waits for its content, keyboard as
   one layout then a slide, list paging distances, the image loader's limits, the overlay's
   recomposition score and the `devReleaseRuntimeOnly` wiring.
6. **Code that should change in the same change as these docs, or the docs say less** (each was read on
   staging `b21f0a981`; the docs describe the code as it is, so none of these is a false claim, but each
   is a gap an agent copying the template would inherit):
   - **The reference feature does the "Don't" of "A destination's first composition is final".**
     `feature/sample/…/detail/SampleDetailViewModel.kt` starts from `SampleDetailState()`, loads on
     `Appeared`, and loads the related rows in the same coroutine as the item, although
     `SampleRepository.peek(id)` exists for exactly this. Agents copy the sample: fix it (seed from
     `peek(itemId)`, send a `LoadRelated` event once `LocalNavigationSettled` turns true) before or with
     this doc.
   - **Play Store check on the start-up path.** `core/playstore/…/PlayStorePrompts.kt:45` calls
     `updates.check()` from a `SideEffect` of the first composition, and `AppUpdates.check()` launches on
     `dispatchers.main` (`Main.immediate`, `app/…/di/CoroutinesModule.kt:32`). The 365 ms cold start in
     `PERFORMANCE.md` was measured with the check after the first content frame (WP04's
     `followPlayStore` in `MainActivity`, `9ed16d25`: an ancestor of staging, but its code did not survive
     the merge with `:core:playstore`'s `PlayStorePrompts`; `followPlayStore` is no identifier on staging).
     Either restore that ordering in `PlayStorePrompts` or re-measure `coldstart` and replace the bullet's
     numbers.
   - **Downloaded fonts never apply in the app.** Only `catalog/…/CatalogActivity.kt:48` calls
     `AppFontPreload.run`. `PERFORMANCE.md` says so. If the startup gate is wired to call it (inside the
     splash's keep-condition, before `AppTheme` first composes), replace "Today only the catalog runs the
     preload" with the app's behaviour, and fix `AppFonts.kt`'s KDoc, which already says "the startup
     gate runs it".
   - **Glass chrome is not wired.** `PERFORMANCE.md` says the shell draws none. When `wp/WP05-glass`
     merges, move the "Lessons from glass chrome built outside this template" bullets into the rules, and
     fix `GlassQuality`'s KDoc, which names "a tier switcher (the catalog, a benchmark)" that does not
     exist on staging.
   - **The pull-to-refresh spinner ignores `LocalTabVisible`** (`AppPullToRefresh.kt:383`): it steps every
     125 ms while refreshing, even in a hidden tab. `PERFORMANCE.md` says so; remove the sentence when it
     is fixed (Telegram P1 in `PENDING.md` does it).
   - **Comments that name one exception or a veil.** `ComposeRules.kt`'s comment above `layer-alpha-fill`
     and `ARCHITECTURE.md`'s `verifyCodeRules` row (`:612`) name the back card as the only whole-screen offscreen
     exception, and `BackCardState.textures`' KDoc (`BackCard.kt:228`) calls it "The one exception";
     `HostCrossfade` is the second (outside the rule's modules, like the card). The same KDoc says "the
     screen under it" for the screen the card shows; `PERFORMANCE.md` says "the screen the card shows".
     `TabReveal.kt:33-34` justifies "no veil" by the GPU's idle clock, which the one-fill measurement
     contradicts (first-frame GPU 10.6 → 10.7 ms with a fill removed); the measured reasons are the
     veil's GPU p90 (12 ms against 8) and the display list re-recorded per reveal frame. `AppMotion.kt:100`
     and `:108` still describe and expose `revealVeilAlpha` (`DesignTokens.kt:223`), which nothing draws.
   - **`uiMode` is not enforced.** `ManifestRules.CONFIG_CHANGES` lacks `uiMode`, so the theme rule rests
     on the manifest alone; add it to the list if it should be checked.
