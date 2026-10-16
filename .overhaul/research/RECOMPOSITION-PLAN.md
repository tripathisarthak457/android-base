# Recomposition plan: checklist, compiler-metrics gate, recomposition-count harness and budgets

Written 2026-10-09 for REQUIREMENT 22 ("optimise recomposition everywhere … compiler metrics clean for the design
system and the shell … recomposition-count tests on the critical journeys"), and for the "no per-frame recomposition"
clauses of 21, 24, 28 and 29. Sources: `research/recomposition.md` (mechanisms, verified against the exact artifacts),
`research/ios-motion-catalogue.md` §4, `research/MOTION-PLAN.md` (the journeys each motion owes),
`research/SMOOTHNESS-PLAN.md`, and the code:

- the integration branch, which equals `wt/WP02` at `709612d` (build-logic, features, `config/`);
- `wt/WP03` (design system, shown as `ds/` = `core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`);
- `wt/WP04` (shell, shown as `nav/` = `core/navigation/src/main/kotlin/com/base/app/core/navigation/`; WP04's tab
  bar is shown as `bar/AppTabBar.kt` = `core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/navigation/AppTabBar.kt`).

Status: **APPLIED** / **PARTIAL** / **MISSING**, with file:line. [V] verified in the artifacts or the repo, [U]
unverified.

---

## 0. In one paragraph

Kotlin 2.4.20 turns on strong skipping, lambda and function-reference memoisation, intrinsic remember and pausable
composition by default [V]. Do not touch those flags, and keep `includeTraceMarkers` on. "Skippable %" no longer
means anything, so the gates that matter are three:

1. **Count executions per journey with a test tracer**, and fail on any execution in frames 2…N−1 of an animation.
2. **Gate the compiler report on unstable parameters** in the design system, `core/ui` *and* the shell, with an
   accurate message.
3. **Count frames that recompose on the device**, per transition.

The design system already reads animated values in layout and draw. What is missing is the enforcement, and a handful
of real bugs: the static window-size local, the provider swaps on a pop, and data-layer models without stability.

---

## 1. Checklist

| # | Rule | Status (file:line) | Change | Owner |
|---|---|---|---|---|
| R1 | Animated, dragged or scrolled values are read only in `offset {}`, `layout {}`, `graphicsLayer {}`, draw blocks or a `Modifier.Node` | **APPLIED** in: `bar/AppTabBar.kt:376-388` (pill), `:342-358` and `:408-419` (collapse), `nav/CoverScrim.kt:57-80`, `nav/BarAnchor.kt:101-121`, `ds/foundation/Pressable.kt:429-438`, `ds/component/navigation/AppTopBar.kt:136-158`, `ds/foundation/AppOverscroll.kt:109-119`. **PARTIAL**: WP03's pull to refresh flips `derivedStateOf` booleans in composition (`ds/component/refresh/AppPullToRefresh.kt:71`, `:133`); the integration branch still has the old `AppBottomBar`/`AppNavigationRail` with composition-phase offsets until WP04 merges | PTR → one node (MOTION-PLAN M20). Confirm the deletion reaches the integration branch; lint `UseOfNonLambdaOffsetOverload` catches leftovers | WP03, WP04 |
| R2 | A component taking a fast-changing input takes `() -> T`, not `T` or `State<T>` | **PARTIAL**: `AppTopBar(scrolled: () -> Int)` and `AppPageControl(selected: () -> Int)` are right; `AppLinearProgress(progress: Float)` (WP03 handoff signature) is not; `nav/CoverScrim.kt:26` takes `State<Float>` (internal) | `AppLinearProgress(progress: () -> Float)`; new state holders expose getters (R7), not `State` | WP03 |
| R3 | No CompositionLocal value or lifecycle owner changes on a transition frame | **MISSING**: `nav/KeepUnderScene.kt:79-80` provides a capped owner and `LocalTabVisible` to the entry beneath, and `:98` renders the same entry without them after the pop, so every `collectAsStateWithLifecycle` reader in that screen recomposes on the pop frame. `nav/KeptTabs.kt:67` flips `LocalTabVisible` on the switch frame | SMOOTHNESS S1: one stable `CappedLifecycle` per entry. S4: `LocalTabVisible` becomes a static local holding a `TabVisibility` holder with a snapshot field, read in draw or effects | WP04 |
| R4 | Static locals only hold values that never change at runtime, or stable holders | **MISSING** fix: `LocalAppWindowSize` is static (`ds/theme/AppWindowSize.kt:94`), and its value's `equals` compares raw `widthDp`/`heightDp` (`:28-55`), so every resize step (freeform window, split-screen drag) recomposes the whole app with skipping off. Provided at `ds/theme/AppTheme.kt:106` | provide only the size **class** through the static local (equality on `WindowWidth` × `WindowHeight`); expose raw dp through a `State<IntSize>` holder in a static local, read in `layout {}`. Journey "Window resize" | WP03 (with ADAPTIVE-PLAN) |
| R5 | Types the compiler cannot see into are configured stable, and the promise is enforced | **PARTIAL**: `config/compose-stability.conf` has `java.time.*`, `java.util.UUID`, `kotlin.time.*`, `kotlinx.serialization.json.JsonElement`, `kotlin.ranges.*` (the dead `kotlinx.collections.immutable.*` line is already gone on the integration branch) | add `kotlin.collections.List<*>`, `Set<*>`, `Map<*,*>`; `com.base.app.data.*.api.*`; `com.base.app.core.common.LoadState`, `…core.common.result.AppError`, `…core.common.navigation.AppNavKey`, `…core.mvi.UiText`, `…core.mvi.UiMessage`, `…core.mvi.MviViewModel` (recomposition §5.3). The promise is kept by the rules `uistate-mutable` and `api-immutable` (R9) | WP02-M2 |
| R6 | Compiler-report gate covers the design system, `core/ui` and the shell, with an accurate message | **PARTIAL**: `build-logic/convention/src/main/kotlin/com/base/app/buildlogic/ComposeStability.kt:66` checks only `:core:designsystem`, `:core:ui`; the KDoc (`:81`) and message (`:158`) say an unstable parameter makes the composable unskippable, which is false since Kotlin 2.0.20; baseline entries are simple names; the regex (`:107`) misses generic functions | gate v2 (§2) | WP02-M2 |
| R7 | The design system's own fast values are lint-protected | **MISSING**: no `@FrequentlyChangingValue` anywhere | annotate the getters `AppTabBarState.collapse` (`bar/AppTabBar.kt:114`), the sheet's offset and presentation fraction (`ds/component/overlay/AppSheet.kt:120`), the PTR gap, the large-title fraction, the swipe offset, the carousel page offset and the reorder drag offset; `@RememberInComposition` on the state-holder constructors (`AppSheetState`, `AppTabBarState`) | WP03, WP04 |
| R8 | Compose lint ids are errors | **MISSING**: `AndroidCommon.kt:43-47` raises only the three orientation ids | wire CODE-STANDARDS M-5's list (`FrequentlyChangingValue`, `UseOfNonLambdaOffsetOverload`, `UnrememberedAnimatable`, `AutoboxingStateCreation`, `SceneEqualsHashCode`, …) as errors in Compose modules; check each id with `./gradlew lint --list` (an unknown id is silently ignored) | WP02-M2 |
| R9 | Code rules for what lint cannot see | **PARTIAL**: `CodeRules.kt` has `composed`, `nav-fade`, `collect-as-state`, `raw-tween`, `lazy-key`, `bare-clickable`, … (`:135-216`) | add `state-param`, `animated-delegate`, `uistate-mutable`, `vm-forwarding`, `api-immutable`, `lazy-content-type` (recomposition §4.4), and `layer-alpha-fill`: `graphicsLayer { … alpha` on a modifier chain that also has `fillMaxSize()` in `core/designsystem` and `feature/**`, with the advice "draw a rect, or use ModulateAlpha" (SMOOTHNESS S8) | WP02-M2 |
| R10 | `UiState` holds no per-frame values; derivations happen in the reducer; a destination's first state is final | **PARTIAL**: `SampleListState.visibleItems` is derived in the reducer; `SampleDetailViewModel` starts `Idle` and swaps three times during the push (`feature/sample/.../detail/SampleDetailViewModel.kt:19-48`) | seed from `peek` (SMOOTHNESS S3.2) | WP02-M3 |
| R11 | Lazy `key`, plus `contentType` on mixed lists | **PARTIAL**: keys APPLIED (`SampleListScreen.kt:131`, `SearchScreen.kt:202`, `Licenses.kt:157`); `contentType` MISSING | rule `lazy-content-type`; add it in features | WP02-M3, WP02-M2 |
| R12 | Hidden tabs are silent | **APPLIED** by design: CREATED cap (`nav/KeptTabs.kt:57`, `:113-132`). Unverified by any test | invariant 3 of the harness (§3.2) | WP04 |
| R13 | Callbacks are not wrapped in `remember`; bound references are memoised [V] | **APPLIED** in the sample (`viewModel::onEvent`) | review rule only (anti-slop) | WP12 docs |
| R14 | Identity-compared parameters on DS APIs | **PARTIAL**: 27 public `List`/`Set`/`Any` parameters (recomposition §5.4); R5's config makes the read-only collections `equals`-compared; `AppSwipeActions(resetKey: Any?)` stays identity-compared and is in the baseline (`config/compose-stability-baseline.txt`) | give `resetKey` a type, or keep it in the baseline with a reason | WP03 |
| R15 | `MviScreen(viewModel: MviViewModel)` unstable | **PARTIAL**: baseline entry `MviScreen` | config entry `com.base.app.core.mvi.MviViewModel` (a screen's ViewModel never changes identity), then delete the baseline line | WP02-M2 |
| R16 | Predictive-back progress is read in the layer, never in composition | **MISSING** (new code). The official sample reads `transitionState` in composition (android-skills `navigation-event/…/handle-back.md`) | SPEC §4.4: `snapshotFlow` → draw-only state | WP04 |
| R17 | Theme changes are not animated through static locals | **APPLIED**: the theme switches instantly | document: never animate a theme through `LocalAppColors` | WP12 |
| R18 | No per-frame allocation in draw (not recomposition, but the same discipline) | **MISSING**: `nav/CoverScrim.kt:68-76`, `bar/AppTabBar.kt:457` | SMOOTHNESS S9 | WP04 |

---

## 2. CI gate on the Compose compiler metrics (gate v2; WP02-M2)

### 2.1 What changes in `build-logic/…/ComposeStability.kt`

1. **Modules**:
   `STABILITY_CHECKED_MODULES = setOf(":core:designsystem", ":core:ui", ":core:navigation")`. In the modular mode,
   add every module under `core/designsystem*` and `core/navigation*` that applies the Compose convention.
2. **Always on, in every preset.** Today reports are written only with `-PcomposeReports` (`Compose.kt:16-26`).
   Set `reportsDestination` unconditionally for the checked modules. `-PcomposeReports` keeps its meaning for
   everything else, plus `metricsDestination`.
3. **Parse fully-qualified names and generics:**
   ```kotlin
   // "restartable skippable scheme(...) fun <T> com.x.AppWheelPicker(" → "com.x.AppWheelPicker"
   private val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s+)?([\w.]+)\s*\(""")
   // "      unstable items: List<T>" → "items"
   private val UNSTABLE = Regex("""^\s+unstable\s+(\w+):""")
   ```
   Report `fqName(param)` per unstable parameter, plus `fqName: not skippable` for any restartable function that is
   not skippable. Under strong skipping that should never happen; if it does, a flag was changed.
4. **Baseline keyed by `fqName(param)`.** One line per accepted parameter, followed by `# reason`. A new unstable
   parameter on a baselined function fails. Today's simple names (`AppSwipeToAction`, `MviScreen`) hide that. Migrate
   the two lines, or delete them (R14, R15).
5. **The message tells the truth:**
   > Under strong skipping, `<fn>(<param>)` is compared by identity, so it reruns whenever the caller passes a new
   > instance, even an equal one. Pass a stable type (a read-only collection, an `@Immutable` class, a type listed in
   > `config/compose-stability.conf`), make it a `() -> T` provider, or accept it in
   > `config/compose-stability-baseline.txt` with a `# reason` in a commit that says why.
6. **Wiring**: `check` already depends on `checkComposeStability` (`ComposeStability.kt:62`), and it compiles
   `compileDebugKotlin`. Debug is fine for reports: live literals no longer exist in the 2.x plugin [V]. CI's
   `./gradlew check` therefore runs it in the template and in every generated preset (WP09's matrix).
7. **Metrics are artefacts, not gates.** `*-module.json` goes to the CI artefacts. `skippableComposables` always equals
   `restartableComposables` under strong skipping, so gating on it is noise.
8. **Do not adopt** third-party stability plugins (Compose Stability Analyzer and the like). They duplicate this and
   pin the template to compiler versions.

### 2.2 Acceptance

- `./gradlew :core:designsystem:checkComposeStability :core:ui:checkComposeStability :core:navigation:checkComposeStability`
  passes with a baseline of at most `AppSwipeActions(resetKey)` (with its reason).
- A negative test in `generator/tests` or a Gradle TestKit test: add `fun Foo(items: MutableList<String>)` to the
  design system. The check must fail and name `…Foo(items)`.

---

## 3. The recomposition-count harness (WP02-M2 builds it; WP03, WP04 and WP02-M3 write journeys)

### 3.1 Where it lives

`:core:testing` is plain (`core/testing/build.gradle.kts`: junit, turbine, coroutines-test; no Compose). Keep it
plain, so JVM-only test classpaths stay free of Compose. Add a Compose-enabled sibling, for example
`:core:testing:compose` (the name follows STANDARDS-PLAN), with:

- `api(libs.androidx.compose.ui.test.junit4)`, `api(libs.robolectric)`;
- `RecompositionCounter` (below), `framesUntilIdle`, a `setThemed {}` helper (moved from WP03's tests);
- `DirectNavigationEventInput` helpers for back journeys (`navigationevent` 1.2.0: `backStarted`, `backProgressed`,
  `backCancelled`, `backCompleted` [V `javap`]).

```kotlin
/**
 * Counts executed composables while a test runs, from the trace markers the Compose compiler emits at the start of
 * every function and lambda that runs rather than skips. Install before setContent; reset() after setup.
 */
@OptIn(InternalComposeTracingApi::class)
class RecompositionCounter : TestRule {
    private val executions = ConcurrentHashMap<String, Int>()

    private val tracer = object : CompositionTracer {
        override fun isTraceInProgress() = true
        override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
            executions.merge(info.substringBefore(" ("), 1, Int::plus)
        }
        override fun traceEventEnd() = Unit
    }

    override fun apply(base: Statement, description: Description) = object : Statement() {
        override fun evaluate() {
            Composer.setTracer(tracer)
            try { base.evaluate() } finally { Composer.setTracer(null) }
        }
    }

    fun reset() = executions.clear()
    fun snapshot(): Map<String, Int> = HashMap(executions)
    fun count(name: String): Int = executions.entries.sumOf { (fq, n) -> if (fq == name || fq.endsWith(".$name")) n else 0 }
}

/** Steps frames until nothing is pending; returns what executed in each frame. */
fun ComposeContentTestRule.framesUntilIdle(counter: RecompositionCounter, maxFrames: Int = 600): List<Map<String, Int>> =
    buildList {
        var frames = 0
        do {
            val before = counter.snapshot()
            mainClock.advanceTimeByFrame()
            add(counter.snapshot().deltaFrom(before))
        } while (hasPendingWork() && ++frames < maxFrames)
    }

/** Fails with the top executed composables of every frame in 2…N−1 that ran anything outside [allowed]. */
fun List<Map<String, Int>>.assertQuietBetweenEnds(allowed: Set<String> = emptySet())
```

- `Composer.setTracer` and `CompositionTracer` are `@InternalComposeTracingApi`, with a stable signature since 1.2
  [V]. They are in `commonMain`, so the same code runs in the Compose Multiplatform template's desktop tests.
- `hasPendingWork()` is new in ui-test 1.12 [V].
- Robolectric `TEST_SDK = 35`; `mainClock.autoAdvance = false`.
- Keep `includeTraceMarkers` on (the default). Never add `-assumenosideeffects` for `traceEventStart`
  (SMOOTHNESS S12).

### 3.2 Three invariants that need no tuning

1. **Quiet between the ends.** For every animated journey, frames 2…N−1 execute nothing outside a named allowlist.
   Frame 1 holds the state flip; frame N holds the settle or the removal.
2. **Duration invariance.** Run each journey under `AppMotionStyle.Snappy` (speed 1.15) and `Calm` (speed 0.87).
   The frame counts differ, so the execution counts must be **identical**. A difference means something recomposes
   per frame.
3. **Hidden tabs and parked entries are silent.** Composables under a kept-but-hidden tab, or a parked stack entry,
   execute 0 times in every journey, including while a repository emits (R12; SMOOTHNESS S1, S4).

### 3.3 Journeys and budgets

"Screen" means everything under `feature.*` plus the entry root. Budgets are executions after setup. **Calibrate**
means the first green run records the number in the test as a constant, with a comment. It ratchets: it is never
raised without a reviewed commit, and the invariants still apply.

| Journey | Script | Budget | Test home, owner |
|---|---|---|---|
| Tab switch to a visited tab, with the reveal (MOTION-PLAN M24) | tap `tab_k`, frames to idle | both tabs' screens **0**; `TabCell` ≤ 2 (the two whose `selected` flips, plus the label under `TabLabels.Selected`); `AppTabBar`, `TabRow`, `KeptTabs` ≤ 1; frames 2…N **0** (reveal, pill, icon bounce in layout and draw) | `core/navigation/src/test/TabSwitchRecompositionTest`, WP04 |
| First visit to a tab | tap an unopened tab | report "first-frame composition size" (composables in frame 1) and ratchet it; with SMOOTHNESS S7 it becomes 0 at the tap | WP04 |
| Push | tap a row | source screen **0**; destination ≤ 1 per composable, and its state does not change during the slide (SMOOTHNESS S3.2); `AppTabBar` **0**; frames 2…N−1 **0** | WP04 (+ WP02-M3's feature test) |
| Pop (Back key) | `backCompleted` without progress | the screen beneath **0** (parked, not re-provided: R3); the leaving screen 0; frames 2…N−1 **0** | WP04 |
| Predictive back scrub (SPEC) | `backStarted`, 30 × `backProgressed`, cancel; repeat with complete | **0** per progress event in both screens and the bar; `BackCardHandler` ≤ 2; commit: the screen beneath 0 | WP04 |
| Rapid Back ×3 within one pop | three `backCompleted` during a pop | the same totals as one pop; intermediate entries **0** | WP04 |
| Sheet drag across detents | 30 moves, release | sheet content and presenter **0** per move; ≤ 1 at a detent settle, and only if `currentDetent` is read | `core/designsystem/src/test`, WP03 |
| Card stack (MOTION-PLAN M6) | open a sheet to large | the shell's content **0** (the transform is in the layer) | WP04 + WP03 |
| Press feedback | down, hold 400 ms, up on `AppButton`, `AppIconButton`, `AppRow`, `AppCard`, `AppChip` | **0** (generalises `PressableTest`) | WP03 |
| Switch, segmented, picker drag | drag across | parent **0**; the control ≤ 1 per committed value | WP03 |
| Scroll with large title and bar minimize | fling three screens | rows already visible **0**; rows entering ≤ 1 each; `AppLargeTitleTopBar` **0** per frame (≤ 1 for the inline-title threshold flip, MOTION-PLAN M8); `AppTabBar` ≤ 2; screen root **0** | WP03 (bar part WP04) |
| Overscroll bounce | fling into the top | list items **0** | WP03 |
| Pull to refresh (MOTION-PLAN M20) | drag 200 px, cross the trigger, hold, release, refresh completes | rows and screen root **0** during the drag; ≤ 2 for `refreshing`; the spinner **0** (it redraws through `invalidateDraw`) | WP03 |
| Context menu open (M7) | long press a row | the source list **0** | WP03 |
| Idle 5 s on each tab | no input | **0** anywhere | WP04 |
| Window resize within one size class | 20 width steps of 1–200 dp, same `WindowWidth` | **0** outside composables that read raw width; fails today (R4) | WP03 |
| Theme or accent change | — | not budgeted: a deliberate whole-tree recomposition | — |
| Per feature (generated) | list scroll + row tap | `<Feature>RecompositionTest`, one per feature template: rows already visible 0 during a scroll, and the destination composes once | WP02-M3 (template), WP02-M2 (scaffold emits it) |

Failure output lists, for each offending frame, the frame index and the top 10 executed composables with counts.
When a budget fails, `Composition.setObserver`/`Recomposer.observe` (`@ExperimentalComposeRuntimeApi` in 1.12.1 [V])
can print which state invalidated the scope. That is a debugging aid, not a gate.

### 3.4 How to run

`./gradlew testDebugUnitTest` runs them all (they are ordinary Robolectric tests, in `check`). Quick subsets:
`:core:navigation:testDebugUnitTest --tests '*Recomposition*'` and the same for `:core:designsystem`.

---

## 4. Device side (WP04 tools/perf; WP02-M2 build wiring)

| Item | Status | Change |
|---|---|---|
| Frames that recompose per transition (`perf.sh recompose`) | **MISSING** (`perf.py` reads only `gfxinfo framestats`) | a Perfetto capture with `atrace_apps = <pkg>` + FrameTimeline. Count main-thread `Choreographer#doFrame*` slices that contain a `Compose:recompose` slice inside each transition window (SQL in recomposition §8.1). Budget **≤ 2 per push, pop, tab switch and back-card commit** (the commit frame and the settle frame), **0 per back-gesture progress frame**, ≤ 1 per sheet detent; report-only for scroll. It runs separately from the timing runs, and it works on `benchmarkRelease` because the runtime always emits these sections |
| Which composables run in a slow first frame | **MISSING** | `runtime-tracing` as **`devImplementation`** (the dev flavour is the only one with `benchmarkRelease`); `tracing-perfetto` + `tracing-perfetto-binary` in `:benchmark`; instrumentation argument `androidx.benchmark.fullTracing.enable=true`; a `CompositionCountBenchmark` with `TraceSectionMetric("%.AppTabBar (%", Mode.Count)` and `Mode.Sum` for time. Kept out of the timing gates. Use it first on the pop first frame and the push composing frame (SMOOTHNESS S1/S3) |
| Investigation builds | — | `ComposeToolingFlags.isVerboseTracingEnabled = true` in the dev `Application.onCreate`, only in an investigation build |
| Dev panel "top recomposers" (optional) | **MISSING** | WP06: install the same `CompositionTracer` in the dev flavour and show the last 5 s; no `Modifier.composed` |

---

## 5. Order of work and owners

| Order | Work | Owner |
|---|---|---|
| 1 | `:core:testing:compose` with `RecompositionCounter`, `framesUntilIdle`, `assertQuietBetweenEnds`, back-input helpers | WP02-M2 |
| 2 | Gate v2 (§2), stability config v2 (R5, R15), lint errors (R8), code rules (R9) | WP02-M2 |
| 3 | DS journeys (press, switch, segmented, picker, sheet drag, scroll and title, overscroll, PTR, context menu, window resize) and the fixes they expose (R1, R2, R4, R7, R14) | WP03 |
| 4 | Shell journeys (tab switch, first visit, push, pop, back scrub, rapid Back, card stack, idle) and R3, R7 (`collapse`), R12, R16, R18 | WP04 |
| 5 | `perf.sh recompose`, `CompositionCountBenchmark`, `runtime-tracing` dev dependency | WP04 (perf), WP02-M2 (build) |
| 6 | Feature tests and the scaffold's per-feature test; R10, R11 | WP02-M3, WP02-M2 |
| 7 | The same harness in the Compose Multiplatform template (`commonTest`, `runComposeUiTest`) | WP07 |

Until WP02-M2 lands step 1, WP03 and WP04 may write the counter locally in their test source sets (≈ 60 lines) and
move to the shared module at the next merge.

---

## 6. Rules the docs must state (WP12; short version for `AGENTS.md`)

1. Animated, dragged or scrolled values are read only in `offset {}`, `layout {}`, `graphicsLayer {}`, draw blocks or a
   `Modifier.Node`. The lints `FrequentlyChangingValue` and `UseOfNonLambdaOffsetOverload` fail the build.
2. A component that takes a fast-changing input takes `() -> T`.
3. `UiState` holds read-only collections built by the reducer; no `.map {}`/`.filter {}` in composition without
   `remember(keys)`; no per-frame values in `UiState`; a destination's first state is final.
4. Do not wrap callbacks in `remember`; the compiler already memoises them.
5. Static CompositionLocals only for values that never change at runtime. For changing values, use a static local
   holding a `@Stable` holder with snapshot fields. Never change a local or a lifecycle owner on a transition frame.
6. Every lazy `items` has a `key`; mixed lists also have a `contentType`.
7. A new screen or component adds a recomposition test: one journey and `assertQuietBetweenEnds()`.
8. `./gradlew check` runs `checkComposeStability`. Fix an unstable parameter with a stable type, a config entry or a
   provider; never baseline it silently.
9. Judge recomposition with the JVM harness and `perf.sh recompose` on a release build. Layout Inspector counts are for
   exploration only.
10. Root causes found here (requirement 25):
    - the static window-size local;
    - the old bar's composition-phase pill;
    - pull-to-refresh offset read in composition;
    - the predictive-back sample's composition read;
    - provider swaps when a screen moves between scenes;
    - data-layer models without stability.
