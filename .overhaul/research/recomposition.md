# Recomposition: state of the art, and what android-base should do

Research for requirement 22 ("optimise recomposition everywhere ... compiler metrics clean for the design system and
the shell ... recomposition-count tests on the critical journeys") and the recomposition clauses of requirements 19,
21, 24, 25, 28 and 29. Written 2026-10-09. Nothing in any repository or worktree was changed.

Read with: `research/ios-motion-catalogue.md` §3–4 (motion contract and the "must not recompose per frame" journey
table), `research/CODE-STANDARDS.md` §3 (M-1…M-5: modifiers, stability, the lint list), `research/predictive-back/SPEC.md`.

**Evidence tags.**
- **[V-bin]**: verified in the exact artifacts this project resolves, read from the Gradle cache: the Kotlin Compose
  compiler plugin 2.4.20, Compose runtime/ui/foundation 1.12.1 (BOM 2026.09.00), benchmark-macro 1.5.0, and
  navigationevent 1.2.0. Checked by reading bytecode with `javap`, the sources jars, R8 rules and lint jars.
- **[V-doc]**: verified on an official page fetched today. URLs are in §15.
- **[V-repo]**: read in the worktrees `wt/WP03`, `wt/WP04`.
- **[U]**: not verified. This covers estimates and memory. Check these before relying on them.

---

## 0. Summary

In Kotlin 2.4 the compiler already does most of what the old advice tried to do by hand. Strong skipping, lambda and
function-reference memoisation, intrinsic remember and group optimisation are all on by default [V-bin]. So
**"skippable %" no longer means anything as a metric**: every restartable composable is skippable.

Three things still cost frames:
1. **New instances that are equal but not identical.** These include lists rebuilt in composition, models re-emitted
   by the data layer, and static CompositionLocals holding a value that changes often.
2. **State read too high or too early.** One example is an animated value read in composition, which recomposes a
   subtree on every frame at 120 Hz.
3. **Cold composition in the first frame of a navigation.** Recomposition rules do not fix this, but the same tracing
   tools find it.

The project's design system already follows the hard rules: draw- and layout-phase reads, `ColorProducer`,
`Modifier.Node` press feedback, and holders inside static locals. The work left is mostly about **verification and
enforcement**:
- a tracer-based recomposition-count harness with per-journey budgets and a "zero per frame" invariant;
- device-side composition tracing to attribute the failing first-frame gates;
- a corrected and extended compiler-report gate;
- a few concrete fixes found in this audit.

## 1. Ranked actions for this project

| # | Action | Why it matters here | Owner | Effort |
|---|---|---|---|---|
| 1 | **Recomposition-count harness** (JVM/Robolectric, `Composer.setTracer`), with scripted journeys for tab switch, push/pop, predictive-back scrub, rapid back, sheet drag, press, switch/segmented drag, scroll + bar minimize, pull to refresh, idle and window resize. Budgets as in §3. The core rule: **no counted composable runs in frames 2…N-1 of any animation**. | Requirement 22 asks for this verbatim. It is the only thing that keeps the design system's draw-phase discipline from regressing once agents start editing generated apps. | WP02-M2 (test infra), WP03 (DS journeys), WP04 (shell journeys) | M |
| 2 | **Attribute first-frame composition on the Redmi** with composition tracing (`runtime-tracing` in the dev flavour plus Macrobenchmark `TraceSectionMetric` in `Mode.Count`). Add a cheap `perf.py` metric: *frames containing `Compose:recompose` per transition*. | The open phase-2 gate failures are first-frame-bound: pop first frame median **15.0 ms** (gate 8), push compose frame median **19.95 ms**, visited-tab max **16.45 ms** (gate 16), rapid-back coalesced pop **23.6 ms** (gate 8). Scroll already passes (0.15 % janky). Recomposition hygiene alone will not close these. Knowing *what composes* in those frames will. | WP04 (+ perf tooling) | M |
| 3 | **Compiler-report gate v2**: fix `ComposeStability.kt` and extend it to the shell (`:core:navigation`). Make it always on. Key entries by `fqName(param)`. Correct its wrong strong-skipping explanation. | Requirement 22 asks for "metrics clean for the design system and the shell". Today `:core:navigation` is not checked, and the message tells agents something false (§4.2). | WP02-M2 | S |
| 4 | **Stability config v2**: read-only collection interfaces, the data-layer `api` models, `AppNavKey`, `UiText`/`LoadState`/`MviViewModel`. Delete the dead `kotlinx.collections.immutable.*` line. Add a code rule that keeps the promised types immutable. | `SampleRow(item: SampleItem)` takes a type from a module without the Compose compiler. Each refresh re-emits equal-but-new items, so every visible row recomposes. 27 public DS parameters are `List`/`Set`/`Any` (§5.4). | WP02-M2, WP03 | S |
| 5 | **Fix `LocalAppWindowSize`**: it is a *static* local whose value carries raw dp, so every resize step recomposes the whole app with skipping disabled (§6.2). | Requirement 23 (desktop windowing, free resize, split-screen drag) produces a size change every frame there. This is invisible on the Redmi and certain on ChromeOS and tablets. | WP03 (theme), with WP04 adaptive | S |
| 6 | **Make the design system's own fast values lint-protected**: put `@get:FrequentlyChangingValue` on state-holder getters (bar collapse, sheet offset, refresh progress, title collapse, swipe offset) and `@RememberInComposition` on state-holder constructors. Raise the Compose lint ids to errors (list in CODE-STANDARDS M-5). | This turns "read it in layout/draw" into a build error for *feature* code written by agents. It costs a one-line annotation per property [V-bin]. | WP03, WP04, WP02-M2 | S |
| 7 | **Read sites for every animation**: `() -> T` providers instead of values on public APIs; `ColorProducer`; `Modifier.Node`. Read predictive-back progress in `graphicsLayer` (the official sample reads it in composition, §7.3). Finish the PTR rewrite (requirement 29). Make sure the deletion of `AppBottomBar`/`AppNavigationRail` lands, because both read animated values in composition. | This delivers the "every animation runs in layout/draw" clause of requirement 21. | WP03, WP04 | M |
| 8 | **`verifyCodeRules` additions** (§4.4): `state-param`, `animated-delegate`, `uistate-mutable`, `vm-forwarding`, `api-immutable`, `lazy-content-type`. | The Android template's own tokenizer is fast and already wired into `check`. | WP02-M2 | S |
| 9 | **UiState rules**: no per-frame values (scroll, drag, progress above ~10 Hz) in `UiState`. Derive values in the reducer, not with `.map {}` in composition. Hidden tabs stay at CREATED, so their collectors pause. | `MviScreen` recomposes the whole screen on each state emission. That is correct, but only if emissions are rare. | WP02-M2 docs, WP12 | S |
| 10 | **Lazy lists**: `key` (rule exists), `contentType` (new rule), `animateItem` keyed. Measure `LazyLayoutCacheWindow` on the feed before shipping it (it is experimental in 1.12.1 [V-bin]). | Scroll already passes. This protects it in generated feature code. | WP03, WP02-M2 | S |
| 11 | **Docs**: the short rule list in §13 goes into `AGENTS.md`, the full version into `docs/MOTION-AND-PERFORMANCE.md`, plus the budgets and how to run the harness. | Requirement 25. | WP12 | S |
| 12 | **Watch list, not now**: the Styles API (experimental), the link-buffer composer flag, paused precomposition for the push frame, and `DeferredAnimatedContent`. | See §12. | — | — |

---

## 2. Ground truth: toolchain defaults and what the compiler does now

### 2.1 Compiler defaults in Kotlin 2.4.20 [V-bin]

The `FeatureFlag` enum in `kotlin-compose-compiler-plugin-embeddable-2.4.20.jar` has all four flags with
`default = true`:

| Flag | Default | Effect |
|---|---|---|
| `StrongSkipping` | on (since Kotlin 2.0.20 [V-doc]) | Every restartable composable is skippable. Unstable params are compared with `===`, stable ones with `equals`. Lambdas inside composables are remembered, keyed on their captures. |
| `IntrinsicRemember` | on | `remember` is inlined, with cheaper key checks. |
| `OptimizeNonSkippingGroups` | on | Removes groups around non-skipping calls, so there is less slot-table work. |
| `PausableComposition` | on | Code generation lets a composition pause mid-way. Lazy prefetch uses it by default since Compose 1.10 [V-doc]. |

Gradle extension options available in 2.4.20 [V-bin]: `featureFlags`, `includeSourceInformation`,
`includeTraceMarkers`, `includeComposeMappingFile`, `metricsDestination`, `reportsDestination`,
`stabilityConfigurationFiles`, and `targetKotlinPlatforms`. The singular `stabilityConfigurationFile` and the
`enable*` booleans are deprecated. The template already uses `stabilityConfigurationFiles` [V-repo].

**Do not change any of these flags.** Keep `includeTraceMarkers` on: the harness (§3) and on-device composition
tracing (§8) both depend on the `traceEventStart` calls it emits. Compose's consumer R8 rules strip only
`sourceInformation*`, so the trace markers survive release builds [V-bin: `proguard.txt` in runtime-1.12.1]. The docs
suggest an `-assumenosideeffects` rule for `isTraceInProgress`/`traceEventStart` to save APK size in production
[V-doc]. Do **not** add it to this template: it would also strip `benchmarkRelease`, which inherits release's
ProGuard files. The runtime cost when no tracer is installed is one static-field read per executed composable. The
size cost has not been measured [U]; check it with `apkanalyzer` if it matters.

### 2.2 The rules that decide whether something reruns

1. **Skipping.** A restartable composable reruns when its scope is invalidated (a state it read changed) or when its
   caller reruns and passes a parameter that compares unequal. For stable types the comparison is `equals`. For
   unstable types it is `===` [V-doc].
2. **Lambdas** inside a composable are wrapped in `remember(captures…)`. Unstable captures are compared by instance
   [V-doc]. **Bound function references** (`viewModel::onEvent`) are memoised as well: `ComposerLambdaMemoization`
   has `rememberFunctionReference` [V-bin]. So `remember { … }` around callbacks, and the pre-2024 "use method
   references" advice, are obsolete. `@DontMemoize` opts a lambda out [V-doc].
3. **`remember(keys)` compares keys with `equals`**, whatever their stability. `remember(list)` therefore does hit when
   the list is rebuilt equal.
4. **Known-stable types** built into the compiler [V-bin, `KnownStableConstructs`]: the kotlinx immutable collections
   (`ImmutableList`/`PersistentList`/…) and Guava immutable collections. `dagger.Lazy` is also known-stable. The
   compiler honours errorprone `@Immutable`. Calls to `listOf`/`setOf`/`mapOf`/`emptyList`/`persistentListOf`/… with
   stable arguments are stable expressions, so `options = listOf("Day", "Week")` written inline at a call site
   should not defeat skipping. [U] That inline call sites are treated as static has not been verified; confirm it
   once in a report or with the harness.
5. **What the reports cannot see.** Even with every composable skippable, a subtree reruns when it receives a new
   instance on every caller recomposition (`items.filter {}` in composition), or when an equal-but-new instance
   arrives from a type the compiler treats as unstable (a data-layer model, a `List`). It also reruns when it reads a
   value that changes per frame, or sits under a static CompositionLocal that changed (§6). Each of these leaves
   `skippable` at 100 %. That is why §3 counts executions instead of trusting the report.
6. **Inline composables are not scopes.** `Column`/`Row`/`Box(content)` are inline, so a state read in their content
   lambda invalidates the nearest *non-inline* enclosing function. "Read state in the lowest scope" means pushing the
   read into a small non-inline composable, or better, out of composition entirely (§7).

---

## 3. The recomposition-count harness (requirement 22) — rank 1

### 3.1 Mechanism: count `traceEventStart` from a test tracer [V-bin]

With trace markers on, the compiler emits `if (isTraceInProgress()) traceEventStart(key, $changed, $changed1, info)`
at the start of every **executed** (not skipped) non-inline composable function and composable lambda. `info` is
`"<fq name> (<File>.kt:<line>)"`. This was checked in WP04's compiled `AppTabBarKt.class`, for example
`com.base.app.core.designsystem.component.navigation.AppTabBar (AppTabBar.kt:192)`. The runtime exposes
`Composer.setTracer(CompositionTracer?)`, which is `@InternalComposeTracingApi` (opt-in, stable signature since
1.2) [V-bin, runtime-1.12.1 sources]. A test that installs its own tracer therefore counts executions per composable
without touching production code. The third-party library *Dejavu* uses the same mechanism to assert recomposition
counts [V-doc, github.com/himattm/dejavu]. Do **not** depend on Dejavu; ~60 lines of our own code avoid a library
pinned to Compose versions.

```kotlin
// :core:testing (compose-enabled source set) — RecompositionCounter.kt
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

    /** Executions of [name], given as a simple or fully qualified function name. */
    fun count(name: String): Int =
        executions.entries.sumOf { (fq, n) -> if (fq == name || fq.endsWith(".$name")) n else 0 }
}

/** Steps frames until nothing is pending; returns what executed in each frame. */
fun ComposeTestRule.framesUntilIdle(counter: RecompositionCounter, maxFrames: Int = 600): List<Map<String, Int>> =
    buildList {
        var frames = 0
        do {
            val before = counter.snapshot()
            mainClock.advanceTimeByFrame()
            add(counter.snapshot().deltaFrom(before))
        } while (hasPendingWork() && ++frames < maxFrames)     // hasPendingWork(): new in ui-test 1.12 [V-bin]
    }
```

Things to get right:
- **Rule order.** Install the tracer before `setContent` and `reset()` after setup, so the setup's composition is not
  counted. `Composer.setTracer` is process-global. That is fine for Robolectric, where tests in one JVM run
  sequentially, but the tracer must always be cleared, which the `finally` does.
- **It counts executions, not "recompositions".** A destination composed for the first time is counted once, so
  budgets say "≤ 1" for things that are allowed to compose. The **per-frame invariant** below sidesteps the
  distinction.
- Inline composables have no marker, and lambdas show up with `<anonymous>` in their names [U: exact lambda naming].
  Budget named functions such as `TabCell`, `SampleRow`, `AppSheet` and screen roots, not lambdas.
- Robolectric with `mainClock.autoAdvance = false` gives deterministic 16 ms frames. Use
  `runWithoutImplicitWait {}` (1.12) when one frame queries many nodes [V-doc, V-bin].
- Predictive back on the JVM: attach a `DirectNavigationEventInput` (navigationevent 1.2.0: `backStarted`,
  `backProgressed`, `backCancelled`, `backCompleted` [V-bin]) to the test's `NavigationEventDispatcher` and send it
  30 progress events.
- **Cause diagnostics (optional).** `Composition.setObserver(CompositionObserver)` and
  `Recomposer.observe(CompositionRegistrationObserver)` give `onScopeInvalidated(scope, value)` and
  `onReadInScope(scope, value)`. They are `@ExperimentalComposeRuntimeApi` in 1.12.1 [V-bin]. They can report
  *which state* invalidated a scope when a budget fails. Treat this as debugging help, not a gate.

### 3.2 Three invariants that need no tuning

1. **Zero per frame.** For every animated journey, the per-frame deltas for frames 2…N-1 must be empty, apart from an
   explicit allowlist of composables whose job is to change at a frame boundary. Frame 1 holds the state flip; frame N
   holds the settle or removal. This is the testable form of "no per-frame recomposition during animations"
   (requirement 21).
2. **Duration invariance.** Run the same journey under `AppMotionStyle.Snappy` (speed 1.15) and `Calm` (speed 0.87).
   Frame counts differ, so the execution counts must be **identical**. If they are not, something recomposes per
   frame. This catches cases that a single run's allowlist could mask.
3. **Hidden tabs are silent.** In every journey, composables under a kept-but-hidden tab execute 0 times, including
   while a repository emits. This checks the `TabLifecycle` CREATED cap.

### 3.3 Journeys and budgets (executions after setup; "screen" means everything under `feature.*` plus the entry root)

These extend the table in `ios-motion-catalogue.md` §4. Where a number says "calibrate", the first green run records
it, the budget ratchets (first value, never raised without a reviewed commit), and the per-frame invariant still
applies.

| Journey (script) | Budget |
|---|---|
| **Tab switch to a visited tab** (tap `tab_k`, run frames to idle) | Both tabs' screens: **0**, apart from readers of `LocalTabVisible` (`AppSkeleton`, `AppActivityIndicator`): ≤ 1 each. `TabCell`: **≤ 2** (the two whose `selected` flips; also `showLabel` under `TabLabels.Selected`). `AppTabBar`, `TabRow`, `KeptTabs`: ≤ 1. `TabLifecycle`, `TabBackDispatcher`: ≤ 2. Frames 2…N: **0** (the pill and colours move in draw). |
| **First visit to a tab** | Not a recomposition. Report the number of composables executed in frame 1 as "first-frame composition size" and ratchet it. It is the JVM proxy for the 22.7 ms first-visit frame. |
| **Push** (tap a row) | Source screen: **0**. Destination: ≤ 1 per composable (its initial composition). `AppTabBar`: **0** (persistent; `BarAnchor` works in draw). NavDisplay internals: calibrate. Frames 2…N-1: **0**. |
| **Pop** (Back key) | The covered entry, kept composed by `KeepUnderScene`: **0** executions of its screen content. This is the property behind the pop first-frame gate. Leaving screen: 0. Frames 2…N-1: **0**. |
| **Predictive back scrub** (`backStarted`, 30 × `backProgressed`, then cancel; repeat with complete) | **0** executions per progress event in both screens and the bar. Start and end: calibrate. Must hold for the Gmail-style card (requirement 24). |
| **Rapid back ×3 within one pop** | The same totals as one pop. Intermediate entries: **0**. |
| **Sheet drag across detents** (30 moves, release) | Sheet content and the presenter: **0** per move. ≤ 1 at a detent settle, and only if `currentDetent` is read. |
| **Press feedback** (down, hold 400 ms, up on `AppButton`, `AppIconButton`, `AppRow`, `AppCard`, `AppChip`) | **0**. This generalises the existing `PressableTest` assertion [V-repo]. |
| **Switch / segmented / picker drag** | The parent: **0**. The control: ≤ 1 per committed value change. |
| **Scroll** (fling three screens of a root list with a large title and `BarMinimize.OnScrollDown`) | Rows already visible: **0**. Rows entering: ≤ 1 each. `AppLargeTitleTopBar`: **0** per frame. `AppTabBar`: ≤ 2 (the minimized flag flips). Screen root: **0**. |
| **Pull to refresh** (drag 200 px, cross the threshold, release, refresh completes) | List rows and screen root: **0** during the drag. ≤ 2 for the `refreshing` flag. Spinner: calibrate. Requirement 29 says "zero recomposition per frame". |
| **Idle 5 s on each tab** | **0** anywhere (the JVM twin of the device "idle frames = 0" gate). |
| **Window resize within one size class** (20 width steps of 1–200 dp, same `WindowWidth`) | **0** outside composables that explicitly read raw width. Fails today (§6.2). |
| **Theme/accent change** | Not budgeted. A deliberate whole-tree recomposition. Document it. |

Placement: DS journeys go in `core/designsystem/src/test` (WP03). Shell journeys go in `core/navigation/src/test`
(WP04), next to `KeptTabsTest`/`RapidBackTest`. A small end-to-end `ShellRecompositionTest` in `app/src/test` runs
the generated sample features. Failure output lists the top 10 executed composables per offending frame.

Generated projects get the harness and one example test per feature template (`<Feature>RecompositionTest`: list
scroll and row tap). The `new feature` checklist in AGENTS.md names it.

---

## 4. Compiler reports as a CI gate — rank 3

### 4.1 What exists [V-repo]

`build-logic/.../ComposeStability.kt` comes from the pre-overhaul template and is unchanged in WP03 and WP04. For
`:core:designsystem` and `:core:ui` it does the following:
- it always sets `reportsDestination`;
- it parses `*-composables.txt`;
- it fails on functions that are "restartable but not skippable" or that have an `unstable` parameter, unless they
  are in `config/compose-stability-baseline.txt`, which currently lists `AppSwipeToAction` and `MviScreen`;
- `check` depends on it.

The last report on disk was from WP04 at 08:40 and is stale. Its `designsystem` had 124 functions, 109 restartable,
0 non-skippable, and 1 unstable parameter (`AppSwipeToAction.resetKey: Any?`). `ui` had
`MviScreen.viewModel: MviViewModel` unstable.

### 4.2 Problems

1. **The explanation is wrong under strong skipping.** The KDoc says an unstable parameter means the function
   compares it "with `equals` on every recomposition … a comparison that never says equal". The error message says it
   "makes the whole composable unskippable". Both are false since Kotlin 2.0.20: unstable means **compared by
   `===`** [V-doc]. The real risk is a new instance on every call, or an equal-but-new instance from the data layer.
   Agents will act on whatever the message says, so it has to be accurate (CODE-STANDARDS bar).
2. **The shell is not checked.** Requirement 22 says "design system **and the shell**". Add `:core:navigation`, which
   holds `AppShell`, `KeptTabs`, `TabbedNavHost` and the scenes.
3. **Baseline entries are simple names.** `AppSwipeToAction` in the baseline hides *any new* unstable parameter on the
   same function, and two private `Content()`s collide. Key entries as `fqName(param)`.
4. **The function regex misses generic functions** (`fun <T> com.x.AppWheelPicker(`).
5. **Optional vs required.** The plan marks `composemetrics` as an optional feature ("reports only with
   `-PcomposeReports`"). Requirement 22 makes the check mandatory. Keep the check on in **every preset**. Let
   `-PcomposeReports` only turn on reports and metrics for the *other* modules.

### 4.3 Gate v2 (sketch)

```kotlin
private val STABILITY_CHECKED_MODULES = setOf(":core:designsystem", ":core:ui", ":core:navigation")

// "restartable skippable scheme(...) fun <T> com.x.AppWheelPicker(" → "com.x.AppWheelPicker"
private val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s+)?([\w.]+)\s*\(""")
// "      unstable items: List<T>" → "items"
private val UNSTABLE = Regex("""^\s+unstable\s+(\w+):""")

private fun offenders(reports: Set<File>): List<String> = reports.filter(File::isFile).flatMap { report ->
    var function = ""
    report.readLines().mapNotNull { line ->
        FUNCTION.find(line)?.let { match ->
            function = match.groupValues[1]
            return@mapNotNull if (line.startsWith("restartable ") && " skippable " !in line) "$function: not skippable" else null
        }
        UNSTABLE.find(line)?.let { "$function(${it.groupValues[1]})" }
    }
}.distinct().sorted()
```

Message text (accurate): *"Under strong skipping, `<fn>(<param>)` is compared by identity, so it reruns whenever the
caller passes a new instance, even an equal one. Pass a stable type (a read-only collection, an `@Immutable` class, a
type listed in `config/compose-stability.conf`), make it a `() -> T` provider, or accept it in the baseline in a
commit that says why."*

Additionally:
- Run `checkComposeStability` in CI's `check`. It already depends on `compileDebugKotlin`. Debug is fine for library
  modules: live literals, which once made debug reports differ, no longer exist in the 2.x plugin [V-bin: no option].
- Commit `<module>-module.json` metrics only as an artefact. With strong skipping, `skippableComposables` vs
  `restartableComposables` is always equal, so gating on it is noise.
- **Do not adopt** skydoves' *Compose Stability Analyzer* (a Kotlin compiler plugin with `stabilityDump`/`stabilityCheck`,
  0.15.0 for Kotlin 2.4.20 [V-doc]) or similar plugins. They duplicate this gate and couple the template to compiler
  versions, which the master plan rejected for DI plugins for the same reason (MASTER-PLAN, Koin/Metro note).

### 4.4 Static rules and lint (pairs with the report gate)

The Compose lint ids are already listed in CODE-STANDARDS M-5 [V-bin, same jars]. Two additions:

- **Annotate the design system's own fast values** so the lint protects the DS API surface too
  (`androidx.compose.runtime.annotation`, in `runtime-annotation` 1.12.1, targets `FUNCTION`/`PROPERTY_GETTER` and,
  for `@RememberInComposition`, also `CONSTRUCTOR` [V-bin]):

  ```kotlin
  @Stable
  class AppTabBarState internal constructor(/* … */) {
      /** 0 expanded … 1 minimized. Read it in layout or draw; reading it in composition recomposes every frame. */
      @get:FrequentlyChangingValue
      val collapse: Float get() = collapseState.floatValue
  }

  @Stable
  class AppSheetState @RememberInComposition constructor(/* … */) { /* … */ }
  ```

  Candidates: `AppTabBarState.collapse`, sheet offset/progress, pull-to-refresh distance/progress, large-title
  collapse fraction, swipe-action offset, carousel page offset, and reorderable drag offset. The lint id
  `FrequentlyChangingValue` warns by default [V-bin]; M-5 raises it to an error. Foundation itself annotates
  `LazyListState.firstVisibleItemIndex`/`firstVisibleItemScrollOffset` [V-bin], so feature code that reads scroll
  position in composition already fails the build.

- **`verifyCodeRules` additions** (tokenizer rules, A11.3):

| Rule id | Fails on | Scope | Why |
|---|---|---|---|
| `state-param` | a `State<`/`MutableState<` parameter type in a `@Composable` signature | `core/designsystem`, `core/ui`, `feature/**` | Compose API guidelines: use `() -> T` for deferred reads [V-doc] |
| `animated-delegate` | `by animate\w*AsState(`, `by animateFloat(`, `by animateValue(` and the `by` forms on `Transition` | `core/designsystem`, `feature/**` | The `by` form invites a read in composition. `val x = animate…AsState(...)` plus `x.value` inside a lambda makes the read site explicit. The legacy `AppBottomBar.SlidingPill` shows the bug (§7.4) |
| `uistate-mutable` | `MutableList`, `ArrayList`, `HashMap`, `MutableMap`, `MutableSet`, `mutableListOf(` or `var` in a `*State`/`*Contract.kt` file | `feature/**` | Required by the stability config's promise on read-only collections (§5.3) |
| `vm-forwarding` | a parameter of type `*ViewModel` on a composable whose name does not end in `Route` | `feature/**` | Keeps ViewModels at the route; screens take state and callbacks (mrmans0n compose-rules `ViewModelForwarding` [V-doc]) |
| `api-immutable` | `var`, mutable collection types or arrays in `data/*/…/api/**` declarations | `data/**` | Required by the stability config's promise on `api` models (§5.3) |
| `lazy-content-type` | a lazy scope with two or more distinct `item(`/`items(` calls that lack `contentType =` | `feature/**`, `core/ui` | Composition reuse across item kinds (§9) |

---

## 5. Stability inference, annotations, config, collections — rank 4

### 5.1 Annotations

- `@Immutable`: no property changes after construction. `@Stable`: mutable, but changes are observable (snapshot
  state) and `equals` is consistent [V-doc, compose-api-guidelines].
- Hoisted state holders are `@Stable`, should be interfaces or final classes with a same-named factory, and
  must keep the contract [V-doc]. The WP04 handoff already marks `ShellState`, `AppBackStack`, `BackGate`,
  `BarAnchor`, `AppTabBarState` and `TabItem`/`ShellConfig` (`@Immutable`) [V-repo]. WP03 marks `OverlayHostState`
  and `AppSheetState`.
- The annotations live in **`androidx.compose.runtime:runtime-annotation`**, which is multiplatform (JVM, Android,
  iOS…) and has no runtime dependency [V-bin: BOM 2026.09 entries, `Stable.kt` sources]. A pure-Kotlin module
  (`:core:common`, `:core:mvi`, `data:*`) can annotate types without applying the Compose compiler. This is the
  Compose Multiplatform-friendly option. Option 2 in the docs ("enable the Compose compiler on data modules") is
  heavier [V-doc]. [U] Whether a consumer module honours `@Immutable` on a class from a module compiled without the
  Compose compiler has not been verified here. Check it once in the `-classes.txt` and `-composables.txt` reports. If
  it is not honoured, use the config file (§5.2).
- Do **not** spray `@Stable`. "Don't attempt to make every composable skippable" [V-doc]. Under strong skipping the
  annotation only buys `equals` instead of `===`, which matters only where equal-but-new instances arrive.

### 5.2 Config file syntax [V-doc + V-bin]

One fully qualified name per line, with `//` comments. Wildcards are parsed by `FqNameMatcher` into a regex
[V-bin]:
- `*` becomes `\w+` (one segment) and may appear **mid-pattern**, so `com.x.data.*.api.*` works;
- `**` becomes `[\w.]+`.

Partial-name globs such as `*Model` are not supported [V-doc]. Generic masks: `com.example.Generic<*,_>`, where `*`
means that type argument counts towards stability and `_` means it is ignored [V-doc].

### 5.3 Proposed `config/compose-stability.conf`

```text
// Types the Compose compiler cannot see inside but which are immutable here. Each line is a promise that
// verifyCodeRules keeps (uistate-mutable, api-immutable); breaking it shows stale UI, not a crash.

// Read-only collection interfaces: compared with equals, so a list rebuilt equal does not recompose its reader.
// Strong skipping already skips an in-place-mutated list (same instance), so this adds no new hazard.
kotlin.collections.List<*>
kotlin.collections.Set<*>
kotlin.collections.Map<*,*>

java.time.*
kotlin.time.*
kotlin.ranges.*
java.util.UUID
kotlinx.serialization.json.JsonElement

// Data-layer models: plain data classes in modules without the Compose compiler (data/*/api).
com.base.app.data.*.api.*

// Core value types used in UiState and as composable parameters.
com.base.app.core.common.LoadState
com.base.app.core.common.result.AppError
com.base.app.core.common.navigation.AppNavKey
com.base.app.core.mvi.UiText
com.base.app.core.mvi.UiMessage
// A screen's ViewModel never changes identity.
com.base.app.core.mvi.MviViewModel
```

Notes:
- **Delete `kotlinx.collections.immutable.*`.** The compiler already knows those types [V-bin], and the template does
  not depend on the library [V-repo: `libs.versions.toml`]. The line misleads agents into thinking it is used.
- The generator renames `com.base.app` in every non-binary file, so this `.conf` is rewritten [V-repo:
  `genkit/render.py` `_BINARY_SUFFIXES`].
- **Collections policy.** CODE-STANDARDS M-4 allows "read-only collections … (or `ImmutableList`)". Recommendation:
  **read-only `List` with the config entries above, no kotlinx-immutable dependency.** It is lighter, ergonomic for
  DS callers, and gives the same skipping as `ImmutableList` [U: confirm `List<*>` masks in the classes report on the
  first build]. If the user prefers compile-time guarantees, `ImmutableList` is the alternative. It is a dependency
  plus `toImmutableList()` friction at every boundary.
- The cost of `equals` on a new-but-equal list is O(n). It is paid only when the instance differs (`AbstractList.equals`
  short-circuits on identity), and it is far cheaper than recomposing the subtree.

### 5.4 Current DS parameters that rely on identity (WP03 tree) [V-repo]

There are 27 public parameters, for example `AppSegmentedControl(options: List<String>)`,
`AppTabRow(tabs: List<String>)`, `AppActionSheet(actions: List<SheetAction>)`,
`AppSwipeActions(leading/trailing: List<SwipeAction>, resetKey: Any?)`, `AppSheet(detents: Set<SheetDetent>)`,
`AppDataTable(columns, rows)` and `AppWheelPicker(items: List<T>)`. With §5.3 most become `equals`-compared. That
leaves `resetKey: Any?`: make it a typed key or keep it in the baseline with a reason. `AppBottomBar`/`AppNavigationRail`
`items: List<BottomNavItem>` disappear when WP04's deletion lands.

---

## 6. Static vs dynamic CompositionLocals — rank 5 (one real bug)

### 6.1 Rule [V-doc + V-bin]

- `compositionLocalOf`: reads are tracked, and a change recomposes only the readers.
- `staticCompositionLocalOf`: reads are not tracked. A change (unequal under the local's policy) marks the provider's
  content as invalid. In `GapComposer`, `providersInvalid` turns `skipping` off for **the whole subtree**, so
  everything below reruns [V-bin: `GapComposer.kt` `skipping` getter, `startProvider`].
- Use static only for values that are "highly unlikely to change" [V-doc]. A third pattern combines both and fits
  values that change often but are read in few places: a **static local holding a stable holder whose fields are
  snapshot state**. Readers subscribe to the field and the provider never changes. The template already does this
  well with `ChromeInset` in `LocalBottomChrome`/`LocalTopChrome` and with `LocalReduceMotion: State<Boolean>`.

### 6.2 Audit of the template's locals (WP03 tree) [V-repo]

| Local | Kind | Changes when | Verdict |
|---|---|---|---|
| `LocalAppColors`, `LocalAppTypography`, `LocalAppShapes`, `LocalAppStyle`, `LocalAppSpacing` | static | theme, style or font change | OK. A deliberate whole-tree recompose. Never *animate* a theme change through these locals (a per-frame whole-app recomposition). Switch instantly, as iOS does. |
| `LocalAppSprings` | static | motion style | OK. Cached per style (`SpringsByStyle`), so the same instance is provided on each AppTheme recomposition. |
| **`LocalAppWindowSize`** | **static** | **every window size change**, because `AppWindowSize.equals` compares `widthDp`/`heightDp` and `rememberAppWindowSize` reads `LocalWindowInfo.containerSize` | **Bug.** Every freeform-resize step, split-screen divider drag or desktop-window drag recomposes the whole app with skipping off. Fix: provide **only the class** (`WindowWidth` × `WindowHeight`, equality on the enums) through the static local. Expose raw dp through a holder (a `State<IntSize>` inside a static local) read where needed, ideally in `layout {}`. Covered by the "window resize" journey (§3.3). |
| `LocalAppLayout` | static | size-class change | OK once the above is fixed. It is a data class keyed on the class. |
| `LocalReduceMotion` | static of `State<Boolean>` | never (the holder is stable) | Good pattern. |
| `LocalBottomChrome`, `LocalTopChrome` | static of `ChromeInset` | never | Good pattern. The shell writes settled heights only (MASTER-PLAN A3.5). |
| `LocalTabVisible` | dynamic | tab switch | OK. Two tabs' readers recompose once per switch. |
| `LocalContentColor`, `LocalTextStyle` | dynamic | per component | OK, the same as foundation/material. |
| `LocalHapticsEnabled`, `LocalOverlayHost`, `LocalShimmer` | static | never at runtime | OK. |

`AppTheme.colors` and the other accessors are `@Composable @ReadOnlyComposable` [V-repo]. Good.

---

## 7. Reading state in the lowest scope, deferring reads, no recomposition during animation — rank 7

### 7.1 Where each kind of value is read

| Value | Read in | API |
|---|---|---|
| Position, translation, parallax, bar follow | placement | `Modifier.offset { IntOffset(…) }`, `layout { … place(x, y) }`, `placeWithLayer { translationX = … }` |
| Scale, alpha, rotation, clip shape | draw layer | `graphicsLayer { … }` (lambda form only) |
| Width/height that animates (bar minimize, expanding pill) | measure | `layout { }` or a `LayoutModifierNode`; never `Modifier.width(animated)` |
| Colour of text and icons | draw | `BasicText(color = ColorProducer)`; `drawBehind { painter.draw(…, ColorFilter.tint(tint())) }` (as `TabCell` does) |
| Background, scrim, progress fill | draw | `drawBehind`/`drawWithContent`; `DrawModifierNode` |
| Anything a component receives from outside that changes often | wherever it is used | a `() -> T` parameter (Compose component guidelines prefer it over `State<T>` [V-doc]; e.g. `AppLinearProgress(progress: () -> Float)`) |
| Threshold crossings (haptic tick, "show scroll-to-top") | composition, but only on change | `derivedStateOf { offset > threshold }`, or `snapshotFlow { … }.distinctUntilChanged()` in a `LaunchedEffect` |
| Settled values others lay out against (bar height → content padding) | composition, settled only | write after the gesture ends (A3.5). Never per frame. |

In a `Modifier.Node`, reads inside `measure`/`draw` are observed automatically and invalidate only that phase. For
reads elsewhere use `ObserverModifierNode.observeReads`. Opt out of auto-invalidation when one input affects one
phase (CODE-STANDARDS M-1).

Already right in the code [V-repo]:
- `AppTabBar`: pill and tint in `drawBehind`, collapse in `layout`, label `ColorProducer`.
- `CoverScrimNode`: draw only.
- `FollowCoveredElement` (`BarAnchor`): draw.
- `Pressable`: `placeWithLayer`, with the test asserting zero recompositions.
- `AppSwitch`, `AppTabRow`, `AppStepIndicator`, `AppTextField` edge, `AppDisclosure`, `AppProgress`: animated State
  read in draw or a layer.
- `ReportMotion`: `snapshotFlow` in an effect, not a composition read.

### 7.2 `derivedStateOf` and `snapshotFlow` rules

- `derivedStateOf` only when the input changes **more often than the output**. The canonical case is
  `firstVisibleItemIndex > 0` [V-doc]. For a plain combination of parameters, use `remember(a, b) { … }`.
- Remember it, and key it on any captured *parameter*: `remember(threshold) { derivedStateOf { offset() > threshold } }`.
  Otherwise it keeps the first value it captured.
- `snapshotFlow` is for side effects driven by state: haptics at thresholds, analytics, persisting scroll position,
  and BackGate motion reporting. Pair it with `distinctUntilChanged()` (snapshotFlow already only emits on change,
  but a mapped value may not). Collect it in `LaunchedEffect`.
- Never write state in composition (a backwards write). Write it in callbacks or effects [V-doc].

### 7.3 Predictive back: a trap in the official sample

The navigation-event guide's `HandlingBackWithTransitionState` reads `navigationState.transitionState` in
composition and takes `progress` from it [V-doc, android-skills `navigation-event/references/…/handle-back.md`]. That
recomposes the reading scope on every gesture event (≈ 120/s). For the Gmail-style shrinking card (requirement 24),
pass the `NavigationEventState` (or a `() -> Float`) down and read `transitionState`/`latestEvent.progress` **inside
`graphicsLayer { }`** (scale, corner shape, translation) and `drawBehind` (scrim). The predictive-back scrub journey
(§3.3) enforces this.

Compose 1.12 adds `DeferredTransitionState` / `DeferredAnimatedContent` with a mutable content transform for
gesture-driven phases and a velocity hand-off [V-doc, August '26 release post]. [U] It is unverified whether its
transform block is evaluated outside composition. Evaluate it for WP04's predictive back only if the harness shows
zero per-frame executions.

### 7.4 Violations found [V-repo, WP03 tree]

- `AppBottomBar.SlidingPill`: `val x by animateDpAsState(…)`, then `Modifier.offset(x = x)`, so it recomposes every
  frame of the indicator slide. The same file uses `Modifier.scale(iconScale)` and `tint` from `animateColorAsState`
  in composition. `AppNavigationRail.VerticalPill` uses `Modifier.offset(y = y)`. WP04 deletes both (handoff
  "Removed: AppBottomBar, AppNavigationRail"). **Check that the deletion reaches the integration branch.** If either
  survives, lint `UseOfNonLambdaOffsetOverload` [V-bin] fails it.
- Pull to refresh: the offset is read in composition (requirement 29 text). WP03 is fixing it. The PTR journey (§3.3)
  is its test.
- `AppTabBar` `TabCell` allocates `ColorFilter.tint(...)` per frame per tab. That is a minor allocation with no
  recomposition. Leave it unless allocation tracking flags it.

### 7.5 UiState and MVI

`MviScreen` reads `state` and calls `content(state, viewModel::onEvent)` [V-repo]. Every emission reruns the screen
body, and children skip by parameter. Rules:
- **No per-frame values in `UiState`.** Scroll offsets, drag positions, animation progress and download progress
  faster than ~10 Hz live in UI-layer state holders read in layout/draw.
- **Derive in the reducer** (`visibleItems` in `SampleListState` already does) [V-repo]. Never derive with
  `.filter {}`/`.map {}` in composition without `remember(keys)`.
- `onEvent` lambdas need no `remember` (§2.2).
- `collectAsStateWithLifecycle` plus `TabLifecycle` capped at CREATED means hidden tabs do not collect, so they do not
  recompose. Invariant 3 (§3.2) checks this.

---

## 8. Composition tracing in Perfetto (device) — rank 2

### 8.1 Coarse metric, no new dependency

The Compose runtime always emits `android.os.Trace` sections [V-bin, runtime/ui 1.12.1 sources]:
- `Recomposer:animation` (frame-clock callbacks);
- `Recomposer:recompose` (the recomposer's frame work; it appears on every animated frame, even an empty one);
- `Compose:recompose` (`GapComposer.doCompose`, which only runs when a composition actually composes or recomposes);
- `Compose:SideEffect:effect`, `Compose:onRemembered`;
- `compose:lazy:prefetch:compose`;
- `AndroidOwner:measureAndLayout`, `AndroidOwner:draw`.

**Metric: frames with `Compose:recompose` per transition.** Use a Perfetto capture with `atrace_apps = <package>`
plus FrameTimeline. Count main-thread `Choreographer#doFrame*` slices that contain a `Compose:recompose` slice inside
the journey window. Budget **≤ 2 per push, pop or tab switch** (the commit frame and the settle frame), ≤ 1 per sheet
detent, and report-only for scroll (new rows compose). This needs no `runtime-tracing`, so it can run on the
benchmarkRelease build. Run it as its own `perf.py` scenario (`recompose`), **never inside the timing runs**: the
capture itself perturbs timing. Today `perf.py` uses only `dumpsys gfxinfo framestats` [V-repo].

```sql
-- sketch for trace_processor: frames in [start, end] that recomposed something
SELECT COUNT(*) AS frames,
       SUM(EXISTS (SELECT 1 FROM slice r
                   WHERE r.track_id = f.track_id AND r.name = 'Compose:recompose'
                     AND r.ts >= f.ts AND r.ts < f.ts + f.dur)) AS recomposing_frames
FROM slice f JOIN thread_track tt ON f.track_id = tt.id JOIN thread t USING (utid) JOIN process p USING (upid)
WHERE p.name = '{pkg}' AND t.is_main_thread AND f.name GLOB 'Choreographer#doFrame*'
  AND f.ts BETWEEN {start} AND {end};
```

[U] App atrace sections should be captured for a profileable, non-debuggable app on the Redmi's user build (this is
how Macrobenchmark reads them). Confirm with one capture.

### 8.2 Fine attribution: which composables run in the slow first frames

Setup [V-doc]:
- `runtime-tracing` (1.12.1, in the BOM [V-bin]) in the app;
- `tracing-perfetto` + `tracing-perfetto-binary` in the **macrobenchmark** module;
- the instrumentation argument `androidx.benchmark.fullTracing.enable=true`. benchmark-common 1.5.0 also accepts
  `androidx.benchmark.perfettoSdkTracing.enable` [V-bin].

Minimum API 30 (the Redmi qualifies). Never ship `tracing-perfetto-binary` to production [V-doc].

For this template:
- Add `runtime-tracing` as **`devImplementation`**. The dev flavour is the only one with `benchmarkRelease`, and
  prod/playstore never get it. `benchmarkRelease` is created by the baseline-profile plugin, so a
  `benchmarkReleaseImplementation` configuration may not exist when the build script runs [U].
- Add a `CompositionCountBenchmark` beside the existing journeys (`TabSwitchBenchmark`, `PushPopBenchmark`,
  `PredictiveBackBenchmark`, `RapidBackBenchmark`, `ScrollBenchmark` [V-repo]) with
  `TraceSectionMetric("%.AppTabBar (%", TraceSectionMetric.Mode.Count)` and similar metrics, plus `Mode.Sum` for time.
  `Mode.Count` exists in 1.5.0 [V-bin]. Keep it out of the timing gates.
- Use it first to answer the phase-2 failures: **what composes in the pop first frame (15.0 ms median) and the push
  compose frame (19.95 ms)?** Query by self time:

```sql
SELECT s.name, COUNT(*) AS n, SUM(s.dur) / 1e6 AS ms
FROM slice s JOIN thread_track tt ON s.track_id = tt.id JOIN thread t USING (utid)
WHERE t.is_main_thread AND s.name GLOB 'com.base.app.*' AND s.ts BETWEEN {frame_start} AND {frame_end}
GROUP BY s.name ORDER BY ms DESC LIMIT 30;
```

- For deeper investigation, set `ComposeToolingFlags.isVerboseTracingEnabled = true` (1.12, `@ComposeToolingApi`) in
  the dev `Application.onCreate`. It adds trace blocks for effects (`Compose:DisposableEffect:effect`, …), composition
  coroutines and measure/layout [V-bin, KDoc]. It costs performance, so enable it only in the investigation build.
- Interactive: Android Studio Layout Inspector shows recomposition and skip counts per node (API 29+) [V-doc].
  Combined with the JVM harness this covers the manual loop.

### 8.3 Dev builds (optional, WP06)

The devtools panel could install the same `CompositionTracer` in the dev flavour and show the "top recomposers in the
last 5 s". Google Play built a debug recomposition highlighter for the same purpose (§11). This is lower priority
than §3, and it must not use `Modifier.composed` (A11.3).

---

## 9. Lazy layouts

- **`key`** on every `items`, which the existing `lazy-key` rule enforces. Keys enable `animateItem` and keep scroll
  position and state across reorders [V-doc].
- **`contentType`** whenever a list mixes item kinds. Compose reuses compositions only between items of the same
  type. Play Store and Compose built "LazyList item type caching" for this [V-doc, Play blog]. Rule
  `lazy-content-type` (§4.4).
- **Pausable prefetch** is on by default since Compose 1.10. The runtime can pause prefetch composition and resume it
  in a later frame [V-doc]. Nothing to configure.
- **`LazyLayoutCacheWindow(ahead, behind)`** (dp or fraction) with `rememberLazyListState(cacheWindow = …)`. It is
  `@ExperimentalFoundationApi` in foundation 1.12.1 [V-bin]; the template already opts in module-wide [V-repo]. It
  composes more content ahead. Try it on the feed only if a scroll trace shows `compose:lazy:prefetch:*` running out
  of time. It costs memory, and the PSS gate (93.8 MB vs 70) is already failing, so measure both.
- Rows take stable parameters (an `@Immutable` row model or a configured `api` type) and plain callbacks.
  `SampleRow(item: SampleItem, onClick)` becomes equals-compared once §5.3 lands.

---

## 10. Lambdas and callbacks: what is now unnecessary

- `remember { { onEvent(X) } }` and `remember(vm) { vm::onEvent }` are redundant under strong skipping, and bound
  references are memoised too (§2.2). Remove them in code review as noise (CODE-STANDARDS anti-slop).
- What still breaks memoisation: capturing something rebuilt on every recomposition. For example,
  `val visible = items.filter {…}` followed by `{ open(visible) }` gives a new key, a new lambda and a rerun child.
  Fix it upstream (reducer or `remember`).
- Slot lambdas (`content: @Composable () -> Unit`) are already memoised (composable singletons and remembered
  composable lambdas).

---

## 11. What large apps and Google's own samples do

| Who | What | Evidence |
|---|---|---|
| Google Play Store | Built a debug recomposition highlighter. Fixed unnecessary recompositions in reused scrolling components, which reduced jank by **10–15 %**. Worked with the Compose team on LazyList item-type caching. Baseline profiles cut search-results first render by 40 %. (2022, before strong skipping.) | [V-doc] android-developers.googleblog.com/2022/03/play-time-with-jetpack-compose.html |
| Now in Android (Google's reference app) | Opt-in compiler reports/metrics through `enableComposeCompilerMetrics`/`enableComposeCompilerReports` Gradle properties, the same as this template. A stability config that declares its whole pure-Kotlin model package stable instead of running the Compose compiler there, plus `java.time.ZoneId`/`ZoneOffset`. Strong skipping cost NiA **+4 kB** APK. | [V-doc] NiA `AndroidCompose.kt`, `compose_compiler_config.conf`; strong-skipping page |
| AndroidX itself | Macrobenchmarks with `TraceSectionMetric` over composition tracing for counts and durations; `fullTracing` instrumentation arguments. | [V-bin] benchmark-macro 1.5.0 API; [V-doc] tracing page |
| GetYourGuide | Compiler reports to find unstable params. A date picker went from recomposing ~50 cells per tap to only the changed cells (2024). | [V-doc] getyourguide.careers/posts/mastering-jetpack-compose-performance-part-2 |
| Twitter/X → community (mrmans0n compose-rules) | Static rules: `UnstableCollections`, `MutableParams`, `MutableStateParam`, `ViewModelForwarding`, `CompositionLocalAllowlist`, `ModifierComposed`, `MutableStateAutoboxing`, `LambdaParameterInRestartableEffect`. | [V-doc] mrmans0n.github.io/compose-rules/rules |
| Recomposition assertions in tests | Dejavu: tracer-based per-testTag counts with causality from dirty bits (0.5.0, Compose 1.11–1.12). The proandroiddev "catching excessive recompositions with tests" article uses `SideEffect` counters. | [V-doc] github.com/himattm/dejavu |

No public source found describes a large app gating *recomposition counts per journey* in CI. The harness in §3 is
ahead of what is documented [U: private practice may differ].

---

## 12. Watch list (not for this overhaul)

- **Styles API** (`androidx.compose.foundation.style`, `Modifier.styleable`): it applies state-based styling (pressed,
  hovered) in layout/draw, skipping composition. It is still `@ExperimentalFoundationStyleApi` and "expect breaking
  changes" (August '26 post) [V-doc, android-skills `styles`]. The DS's `Pressable` node already gets the same effect.
  Revisit when it is stable.
- **Link-buffer composer.** `ComposeRuntimeFlags.isLinkBufferComposerEnabled` (off; Compose's R8 rules force it off
  [V-bin]). This is a new slot-table implementation in 1.12. No action.
- **Paused precomposition for the push frame.** `SubcomposeLayoutState.createPausedPrecomposition(slotId, content)`
  with `resume(shouldPause)`/`apply()` is public in ui 1.12.1 [V-bin]. In theory it could spread a destination's
  initial composition over idle frames, but NavDisplay does not expose it. This is only relevant if §8.2 shows the
  push frame dominated by the destination's own composition. [U] feasibility.
- **`retain {}`** (1.10) and keyed `SideEffect(key) {}` (1.12, "up to 90 % faster than LaunchedEffect" for one-shot
  keyed effects) [V-doc, V-bin]. Use keyed `SideEffect` for analytics/logging effects in templates.

---

## 13. Rules for the generated `AGENTS.md` (short, enforceable) and the long guide

Short version for AGENTS.md (requirement 25):

1. Animated, dragged or scrolled values are read only in `offset {}`, `layout {}`, `graphicsLayer {}`, `drawBehind {}`
   or a `Modifier.Node`. Never in a composable body (the lints `FrequentlyChangingValue` and
   `UseOfNonLambdaOffsetOverload` fail the build).
2. Components that take a fast-changing input take `() -> T`, not `T` or `State<T>`.
3. `UiState` holds read-only collections and is built by the reducer. No `.map {}` or `.filter {}` in composition
   without `remember(keys)`. No per-frame values in `UiState`.
4. Do not wrap callbacks in `remember`; the compiler already does it.
5. New CompositionLocals: static only for values that never change at runtime. For changing values, use a static
   local holding a `@Stable` holder with snapshot fields.
6. Every lazy `items` has a `key`; mixed lists also have a `contentType`.
7. A new screen or component adds a recomposition test: one journey and a per-frame-zero assertion
   (`framesUntilIdle`).
8. `./gradlew check` runs `checkComposeStability`. Fix an unstable parameter (stable type, config entry or provider
   lambda). Never baseline it silently.
9. Judge recomposition on the JVM harness and with `perf.sh recompose` on a release build. Layout Inspector counts are
   for exploration only.

The long version for `docs/MOTION-AND-PERFORMANCE.md` should include:
- §2.2 (semantics);
- the §7.1 table;
- the §3.3 budgets;
- the §8 commands and SQL;
- the root causes and fixes found in this audit: the window-size static local, the old bar's composition-phase pill,
  PTR offset in composition, the predictive-back sample trap, and data-layer models without stability.

---

## 14. Compose Multiplatform and SwiftUI templates

- **CMP**: the same Kotlin Compose compiler, so §2, §4 and §5 apply unchanged. Put `stabilityConfigurationFiles` in
  the shared module's convention. `runtime-annotation` publishes iOS artefacts (`runtime-annotation-iosarm64` and
  others in BOM 2026.09 [V-bin]). The tracer harness is common code: `Composer.setTracer` is in `commonMain`
  [V-bin]. It runs in desktop/JVM tests through `runComposeUiTest`; [U] on the iOS simulator. On iOS there is no
  Perfetto, so device attribution stays an Android-only gate. Run §8 on the Android target of the CMP app.
- **SwiftUI** [U, from memory; verify against WWDC sessions before writing docs]:
  - `@Observable` (iOS 17) tracks reads per property, so views that read only what they display are invalidated only
    when that changes. This is the analogue of snapshot reads.
  - Pass the narrowest value into leaf views, not the whole model.
  - Use `onScrollGeometryChange(for:of:action:)` (iOS 18) with a transform to a `Bool` as the analogue of
    `derivedStateOf`.
  - Use `let _ = Self._printChanges()` in debug and Instruments' SwiftUI template (view body counts; Xcode 26 adds
    cause-and-effect views) for counting.
  - Avoid `AnyView` and wide `GeometryReader` dependencies.

---

## 15. Sources

Official (fetched 2026-10-09):
- Strong skipping: https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
- Fix stability (config syntax, `kotlin.collections.*` opt-in, multi-module options): https://developer.android.com/develop/ui/compose/performance/stability/fix
- Diagnose stability (report formats, Layout Inspector): https://developer.android.com/develop/ui/compose/performance/stability/diagnose
- Best practices (remember, keys, derivedStateOf, deferred reads, backwards writes): https://developer.android.com/develop/ui/compose/performance/bestpractices
- CompositionLocal: https://developer.android.com/develop/ui/compose/compositionlocal
- Composition tracing: https://developer.android.com/develop/ui/compose/tooling/tracing
- Compose compiler Gradle options: https://kotlinlang.org/docs/compose-compiler-options.html and https://kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin/org.jetbrains.kotlin.compose.compiler.gradle/-compose-feature-flag/-companion/
- Compose December '25 release (pausable prefetch default, cache window, retain): https://developer.android.com/blog/posts/whats-new-in-the-jetpack-compose-december-release
- Compose August '26 release (1.12: keyed SideEffect, DeferredTransition, `hasPendingWork`, `runWithoutImplicitWait`, Styles still experimental): https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- Compose component API guidelines (`State<T>` params discouraged, `() -> T` preferred): https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md
- Compose API guidelines (stable types, hoisted state): https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md
- Play Store case study: https://android-developers.googleblog.com/2022/03/play-time-with-jetpack-compose.html
- Now in Android: https://github.com/android/nowinandroid (`build-logic/.../AndroidCompose.kt`, `compose_compiler_config.conf`)
- Google Android skills (local): `~/.claude/plugins/marketplaces/android-skills/` — `navigation/navigation-event` (handle-back sample), `jetpack-compose/theming/styles`, `jetpack-compose/adaptive/.../tooling/debug.md` (Layout Inspector counts), `profilers/android-profiler`

Community (secondary):
- Dejavu: https://github.com/himattm/dejavu
- Compose Stability Analyzer: https://github.com/skydoves/compose-stability-analyzer
- compose-rules: https://mrmans0n.github.io/compose-rules/rules/
- GetYourGuide: https://www.getyourguide.careers/posts/mastering-jetpack-compose-performance-part-2

Artifacts read locally [V-bin] (`~/.gradle/caches/modules-2/files-2.1/`):
- `kotlin-compose-compiler-plugin-embeddable-2.4.20`: `FeatureFlag`, `KnownStableConstructs`, `FqNameMatcher`,
  `ComposerLambdaMemoization`;
- `compose-compiler-gradle-plugin-2.4.20`: the extension;
- `runtime-android-1.12.1`: `Composer`, `CompositionTracer`, `tooling/CompositionObserver`, `ComposeToolingFlags`,
  `GapComposer`, `Recomposer`, `proguard.txt`, lint ids;
- `runtime-annotation-android-1.12.1`: `FrequentlyChangingValue`, `RememberInComposition`, `Stable`;
- `foundation-android-1.12.1`: `LazyLayoutCacheWindow`, `LazyListState` annotations, the
  `UseOfNonLambdaOffsetOverload` lint;
- `ui-android-1.12.1`: `SubcomposeLayoutState.createPausedPrecomposition`, trace names;
- `ui-test-junit4-android-1.12.1`: `hasPendingWork`, `runWithoutImplicitWait`;
- `benchmark-macro-1.5.0`: `TraceSectionMetric.Mode.Count`;
- `benchmark-common-1.5.0`: `fullTracing.enable`, `perfettoSdkTracing.enable`;
- `navigationevent-android-1.2.0`: `DirectNavigationEventInput`;
- compose-bom 2026.09.00 (`runtime-tracing` 1.12.1).

Project files read [V-repo]:
- `wt/WP04/templates/android/build-logic/convention/src/main/kotlin/com/base/app/buildlogic/{Compose,ComposeStability,AndroidCommon}.kt`
- `wt/WP04/templates/android/config/compose-stability{.conf,-baseline.txt}`
- `wt/WP04/templates/android/core/designsystem/build/compose_reports/*`
- `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/{KeptTabs,SceneMotion,CoverScrim,BarAnchor,AppShell}.kt`
- `wt/WP04/templates/android/core/designsystem/.../navigation/AppTabBar.kt`
- `wt/WP03/templates/android/core/designsystem/src/main/kotlin/.../{theme/AppTheme,theme/AppWindowSize,theme/AppMotion}.kt`
- `wt/WP03/.../component/navigation/{AppBottomBar,AppNavigationRail}.kt`
- `wt/WP03/.../foundation/PressableTest.kt`
- `wt/WP04/templates/android/feature/sample/**`
- `wt/WP04/templates/android/core/ui/src/main/kotlin/com/base/app/core/ui/MviScreen.kt`
- `wt/WP04/templates/android/tools/perf/{perf.py,gates.toml}`
- `perf/WP04/{baseline,phase2}/summary.md`
