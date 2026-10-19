# HUD metrics: API lookups (recomposition + cheap runtime signals)

Date of lookup: 2026-10-20. Facts only, no recommendations. Read-only research; nothing run (no gradle, no adb), no
worktree touched. Code was read from branch heads with `git -C D:/projects/tools/android-base show/grep wp/HUD`.

## How each fact was verified (tags used below)

- [WEB] official page fetched with WebFetch (developer.android.com, kotlinlang.org, source.android.com).
- [SRC] AndroidX source read from `androidx-main` (android.googlesource.com or raw.githubusercontent.com). `androidx-main`
  is ahead of the stable 1.12.1; where the difference matters it is stated.
- [SDK] the platform sources and `api-versions.xml` of the local SDK, `platforms/android-37.0/data/api-versions.xml` and
  `sources/android-37.0/`. The developer.android.com reference pages for platform classes were fetched but their body was
  cut off by the fetcher (it only returned the navigation sidebar), so API levels and javadoc text for platform classes come
  from the SDK, which is what those pages are generated from. Reference URLs are still given so they can be opened.
- [MVN] Google Maven metadata / POM.
- [INFER] my inference from the cited code; marked every time.

## Project snapshot (so versions can be matched)

From `wp/HUD:templates/android` (read only): Compose BOM 2026.09.00, which maps to `androidx.compose.runtime:runtime` 1.12.1
and `ui` 1.12.1 [WEB bom-mapping]; AGP 9.4.1; Kotlin 2.4.20; minSdk 26; compileSdk and targetSdk 37;
`androidx.metrics:metrics-performance` 1.0.0 (latest on Maven: 1.0.0, lastUpdated 2025-10-08 [MVN]).
Latest Compose runtime on Maven: 1.13.0-beta01 (2026-10-07); latest stable 1.12.1 (2026-09-09) [MVN, WEB release notes].
`androidx.compose.runtime:runtime-tracing` on Maven: latest 1.13.0-beta01, latest stable 1.12.1 [MVN].
Already in the perfhud module on that branch: JankStats (FrameFeed, FrameIntake), `Debug.getRuntimeStat` and
`Debug.getMemoryInfo` (MemorySampler), `ApplicationStartInfo` (StartupTimes), `Display` refresh rate (RefreshRate.kt),
StrictMode with `detectCustomSlowCalls()` (StrictModeWatch.kt), and the build already sets `reportsDestination`
(Compose.kt, ComposeStability.kt, flags `enableComposeCompilerMetrics/Reports` in gradle.properties).

---------------------------------------------------------------------------------------------------------------------

# PART 1. Counting recompositions at runtime in Compose

## 1.1 CompositionObserver family (androidx.compose.runtime.tooling)

Package `androidx.compose.runtime.tooling`, artifact `androidx.compose.runtime:runtime`. [SRC]
https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/tooling/CompositionObserver.kt
Reference: https://developer.android.com/reference/kotlin/androidx/compose/runtime/tooling/CompositionObserver

Public surface (all `@ExperimentalComposeRuntimeApi`):

- `interface CompositionObserver` with callbacks `onBeginComposition(ObservableComposition)`, `onScopeEnter(RecomposeScope)`,
  `onReadInScope(RecomposeScope, Any)`, `onScopeExit(RecomposeScope)`, `onEndComposition(ObservableComposition)`,
  `onScopeInvalidated(RecomposeScope, Any?)`, `onScopeDisposed(RecomposeScope)`.
- `interface ObservableComposition { fun setObserver(CompositionObserver): CompositionObserverHandle }`.
- `fun Composition.setObserver(CompositionObserver): CompositionObserverHandle?` (null if that composition cannot be observed).
- `interface CompositionObserverHandle { fun dispose() }`.
- `interface CompositionRegistrationObserver` with `onCompositionRegistered(ObservableComposition)` and
  `onCompositionUnregistered(ObservableComposition)`; `fun Recomposer.observe(CompositionRegistrationObserver): CompositionObserverHandle`.

Semantics stated in the KDoc [SRC]:
- Lifecycle: begin, then scope enter, reads, scope exit (nested), then end; `onScopeDisposed` happens in the apply phase and
  "can be called from any thread".
- `onScopeInvalidated(scope, value)`: `value` is the invalidating object (usually a snapshot state instance) or null for a
  direct `RecomposeScope.invalidate()`. For state changes it is not called at the write, but right before recomposition or
  during composition; it is always called before the matching `onScopeEnter`. Invalidation does not guarantee composition
  (scope may have left the tree, or its movable content moved).
- `onReadInScope`: reads can be recorded without the scope's function re-running (e.g. derived state invalidating with the
  same value). Instances are only tracked between enter and exit.
- `setObserver` on a composition replaces that composition's previous observer (one observer per composition). The observer
  covers the composition and all its sub-compositions transitively; calling `setObserver` on a sub-composition overrides the
  parent's observer for that sub-composition and its children (the parent then stops receiving events for it).
- `onCompositionRegistered` is called on the thread the composition is composed on (and, during the initial replay, on the
  thread that registered the observer); implementations must be thread safe.

Version history (all from the androidx release notes and the file's commit history):
- Original experimental observer: commit "Address API feedback" 2023-07-07; the reference page says `CompositionObserverHandle`
  was added in 1.6.0 (search snippet of the reference page; body not fetchable) [WEB partial].
- 1.8.0-alpha06 (2024-11-13): "tooling APIs to observe the creation of new compositions within a Recomposer"
  (`CompositionRegistrationObserver`, commit 2024-10-29) [WEB release notes, GitHub history].
- 1.9.0-alpha04 (2025-06-04) and again 1.9.0-beta01 (2025-06-18): "Updated experimental composition observers API to allow
  for more comprehensive observation of recomposition causes" (commit "Update CompositionObserver APIs", 2025-05-20). This is
  the shape listed above (scope-level callbacks). [WEB release notes, GitHub history]
- 1.10.0-alpha03 (2025-09-10): `IdentifiableRecomposeScope` added for tooling (see 1.4).
- 1.13.0-beta01 (2026-10-07): "The CompositionObserver is now resolved once per composition pass"; setting or replacing it
  with `setObserver` mid-pass takes effect on the next pass; movable-content insertion now also reports
  `onBeginComposition`/`onEndComposition` (commit "Pin CompositionObserver per composition pass", 2026-09-25). Not in 1.12.1.
  [WEB release notes, GitHub history]
- BOM 2026.09.00 = runtime 1.12.1 (identical to 1.12.0 per its notes), so the 1.9.0-alpha04 shape applies to the project.

Opt-in: `@ExperimentalComposeRuntimeApi` is `@RequiresOptIn(level = ERROR, "This in experimental API that may change
frequently and without warning.")`, targets class, function, property, field, getter, retention BINARY [SRC
Annotations; file ExperimentalComposeRuntimeApi.kt on androidx-main]. Using it needs `@OptIn(ExperimentalComposeRuntimeApi::class)`
(or the `-opt-in` compiler arg).

Release-like build: nothing in the source gates on debuggable. `CompositionImpl.setObserver` stores the observer in a holder
under a lock; the call sites are `observer()?.onReadInScope`, `observer()?.onScopeInvalidated`, `observer()?.onScopeDisposed`
(null-safe, no early return other than the `?.`) [SRC Composition.kt]. These are plain calls (no reflection), so no keep rules
are implied by the source. `onBeginComposition`/`onEndComposition` are called from the composer implementations (not shown
in Composition.kt).

Cost: no official number. By construction the callbacks run synchronously on the composing thread for every scope enter,
exit and recorded state read while an observer is set [INFER from KDoc]. The only official overhead statement is for the
tracer built on top of it (see 1.5).

Skip detection: the public interface has no "skipped" flag. Google's own `RecompositionTracer` reads the internal
`(scope as RecomposeScopeImpl).skipped` inside `onScopeExit` and does not clean up reads for skipped scopes, so `onScopeExit`
is also delivered for skipped scopes [SRC RecompositionTracer.jvmAndAndroid.kt]. A public observer therefore cannot tell
"entered and re-executed" from "entered and skipped" by the documented API [INFER].

## 1.2 Wiring used by Google's own code (how an app gets at the compositions)

Both `RecompositionTracer` (androidx-main) and Studio's Layout Inspector state-read handler do this [SRC]:
1. collect `Recomposer.runningRecomposers` (a `StateFlow<Set<RecomposerInfo>>`),
2. for each new `RecomposerInfo` call `info.observe(registrationObserver)` and keep the handle (dispose it when the recomposer
   leaves the set),
3. in `onCompositionRegistered(composition)` call `composition.setObserver(compositionObserver)`, keep the handle,
4. in `onCompositionUnregistered` dispose it.
Sources:
https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/runtime/runtime/src/jvmAndAndroidMain/kotlin/androidx/compose/runtime/tooling/RecompositionTracer.jvmAndAndroid.kt
https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/ui/ui-inspection/src/main/java/androidx/compose/ui/inspection/recompositions/StateReadHandler.kt
(Studio's handler also calls `onReadInScope` with a captured `Exception()` per read and `onScopeInvalidated`, and, when
parameter tracking is on, `Composer.setTracer`.)

## 1.3 Recomposer.runningRecomposers and RecomposerInfo

[SRC] https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/Recomposer.kt
[WEB] https://composables.com/docs/androidx.compose.runtime/runtime/interfaces/RecomposerInfo

- `Recomposer.runningRecomposers: StateFlow<Set<RecomposerInfo>>`, companion property, no opt-in annotation. KDoc: observable
  set of the currently running recomposers, emitted sets are immutable. Process-wide. A recomposer is added in
  `recompositionRunner` (when `runRecomposeAndApplyChanges` starts, after the apply observer is registered) and removed in its
  `finally`. Added in 1.0.0-alpha12 (2021-02-10): "global StateFlow of read-only RecomposerInfo ... Prefer this API to
  Recomposer.current()" [WEB release notes]. Not experimental.
- `RecomposerInfo` members: `state: Flow<Recomposer.State>`, `hasPendingWork: Boolean`, `changeCount: Long`,
  `@ComposeToolingApi errorState: StateFlow<RecomposerErrorInformation?>` (added 1.11.0-alpha04, 2026-01-28, hot reload only),
  `@ExperimentalComposeRuntimeApi fun observe(CompositionRegistrationObserver): CompositionObserverHandle? = null`.
  `@ComposeToolingApi` is `@RequiresOptIn("This API is for tooling only and is likely to change in the future.")` [WEB composables.com].
- `Recomposer.State` values: ShutDown, ShuttingDown, Inactive, InactivePendingWork, Idle, PendingWork.
- What `changeCount` means: KDoc "running count of the number of times the recomposer awoke and applied changes to one or
  more composers; unaffected if the composer awoke and recomposed but composition produced no changes to apply". In the source
  it is incremented once inside the `parentFrameClock.withFrameNanos` callback, after the recompose loop, when `toApply` is
  not empty (`changeCount++`). So it advances at most once per frame callback and counts frames in which composition applied
  changes, not composable invocations and not scope recompositions. It is a plain non-volatile Long read without a lock;
  `hasPendingWork` reads several collections under `synchronized(stateLock)`. [SRC]
- Release-like build: no debuggable gate in the source.

## 1.4 RecomposeScope and IdentifiableRecomposeScope

- `RecomposeScope` has one public member, `invalidate()` (thread safe). [WEB composables.com]
- `IdentifiableRecomposeScope { val identity: Any? }` in `androidx.compose.runtime.tooling`, "a value that identifies a scope
  independently of movement caused by recompositions", equal to the identity of the `CompositionGroup` that holds the scope;
  `@ComposeToolingApi`. Added 1.10.0-alpha03 (2025-09-10) [WEB release notes, composables.com].
  https://composables.com/jetpack-compose/androidx.compose.runtime/runtime/interfaces/IdentifiableRecomposeScope/api
- Google's tracer keys its per-scope metadata by the `RecomposeScope` instance itself (`MutableScatterMap<RecomposeScope,
  ScopeData>`), not by identity or source info. [SRC]
- No public API on `RecomposeScope` gives a composable name or source location. Names come from the slot table / source
  information (`CompositionData`, ui-tooling-data), which need source information compiled in (`includeSourceInformation`
  option exists in the Compose compiler Gradle extension; its default is not stated on the reference page) [WEB kotlinlang API].

## 1.5 Composition tracing (androidx.compose.runtime:runtime-tracing)

[WEB] https://developer.android.com/develop/ui/compose/tooling/tracing

- Dependency `androidx.compose.runtime:runtime-tracing`, page shows 1.12.1 and BOM-managed. Maven POM 1.12.1: depends on
  `androidx.startup:startup-runtime:1.1.1` (runtime), `androidx.tracing:tracing-perfetto:1.0.1` (runtime),
  `androidx.annotation:annotation:1.8.1`, `kotlin-stdlib:2.1.20`. [MVN]
  https://dl.google.com/android/maven2/androidx/compose/runtime/runtime-tracing/1.12.1/runtime-tracing-1.12.1.pom
- Page states minimums: Android Studio Flamingo, Compose UI 1.3.0, Compose Compiler 1.3.0, device or emulator API 30+.
  No minimum AGP is stated.
- Capture paths on the page: Studio Profiler (CPU timeline, System Trace), `adb shell perfetto` with a `track_event` data
  source plus `adb shell am broadcast -a androidx.tracing.perfetto.action.ENABLE_TRACING`, and Macrobenchmark with
  instrumentation argument `androidx.benchmark.fullTracing.enable=true`. `tracing-perfetto` + `tracing-perfetto-binary` 1.0.0
  are needed for the terminal and Macrobenchmark paths; "do not ship tracing-perfetto-binary in production" (large).
- Release-like builds: the page says to make the app profileable and non-debuggable for accurate profiling; tracing strings
  are embedded in all apps from Compose Compiler 1.3.0 and are unobfuscated; they can be stripped with an R8
  `-assumenosideeffects` rule for `androidx.compose.runtime.ComposerKt` (`isTraceInProgress`, `traceEventStart`,
  `traceEventEnd`), with a small APK-size cost if kept. The page does not state outright whether it works in a fully
  release-configured build; it only covers profileable non-debuggable builds and the stripping rule.
- Runtime mechanics [SRC androidx-main]: `ComposeTracingInitializer` (App Startup) calls `Composer.setTracer(tracer)`.
  `ComposeTracer` emits through `androidx.tracing` `Tracer.global`, category `"androidx.compose"`, and only when that category
  is enabled (`isTraceInProgress() = Tracer.global.isCategoryEnabled(...)`); when it is off, no sections are created. The
  output goes to a trace session; there is no in-process read-back API in the files read [INFER].
  https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/runtime/runtime-tracing/src/main/java/androidx/compose/runtime/tracing/ComposeTracer.kt
- `Composer.setTracer`/`CompositionTracer` (`traceEventStart(key, dirty1, dirty2, info)`, `traceEventEnd()`,
  `isTraceInProgress()`) are `@InternalComposeTracingApi`; the tracer is one private global var and `setTracer` overwrites it,
  so a second tracer replaces runtime-tracing's. The compiler calls these around composable bodies that execute. [SRC Composer.kt]
  Release notes: tracing API introduced 1.2.0-alpha03 (2022-02-09); `Composer.setTracer` restricted to
  `OptIn(InternalComposeTracingApi)` in 1.2.0-alpha08; runtime tracing in 1.3.0 (2022-10-24) [WEB release notes].
  Compiler option `includeTraceMarkers` ("Include composition trace markers in the generated code") exists [WEB kotlinlang].
- Newer: 1.11.0-alpha03 trace annotation for movable content insertion; 1.12.0-beta01 verbose tracing for
  `DisposableEffect`/`SideEffect`; 1.13.0-beta01 "Support off-main thread usage of `ComposeTracer`" [WEB release notes].
- Recomposition counting from a trace, officially: Macrobenchmark `TraceSectionMetric("%EntryRow%", Mode.Sum)` reports both
  summed duration and occurrence count; "only shows results when running with composition tracing" [WEB]
  https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics (TraceSectionMetric is marked experimental).

## 1.6 RecompositionTracer (newer, not in the project's 1.12.1)

[SRC] RecompositionTracer.jvmAndAndroid.kt (URL in 1.2); reference https://developer.android.com/reference/kotlin/androidx/compose/runtime/tooling/RecompositionTracer
- Added in runtime 1.13.0-alpha01 (2026-08-12): "Added `RecompositionTracer` to record recomposition invalidation flow"
  [WEB release notes]. Still in the 1.13 beta line only; BOM 2026.09.00 has 1.12.1.
- `@InternalComposeTracingApi public class RecompositionTracer(traceCollector: TraceCollector)` with
  `installTracing(coroutineContext): CancellationHandle`; `TraceCollector` has `beginSection(name, flowIds)`, `endSection()`,
  `instantEvent(name, stackTrace, id, flowIds)`, `isEnabled()`. Output is Perfetto flow events linking state reads/writes to
  scope recomposition ("Recompose group", "State read of ...", "State write of ...", "Direct invalidation").
- KDoc warning, quoted: recomposition tracing "captures a stack trace and instance string on all state reads and writes,
  which results [in] a significant overhead. Avoid using it in production."
- It hooks `Recomposer.runningRecomposers` + `CompositionObserver` as in 1.2, and also `Snapshot.observeSnapshots` and
  `Snapshot.registerGlobalWriteObserver` for writes.
- `RecompositionTracingReceiver` (runtime-tracing): actions `androidx.compose.tracing.action.START_RECOMPOSE_TRACING` /
  `STOP_RECOMPOSE_TRACING`, described as meant for tooling such as macrobenchmarks, "higher overhead", enabled state persisted
  until reinstall. [SRC RecompositionTracingReceiver.kt]

## 1.7 What Layout Inspector's recomposition counts rely on, and whether an app can read them

- Docs: counts of compositions and skips per node need API 29+ and Compose 1.2.0+; do not strip
  `META-INF/androidx.compose.*.version`; "Show Recomposition Counts" in the View options; "Reset" zeroes the counts [WEB]
  https://developer.android.com/develop/ui/compose/tooling/debug and
  https://developer.android.com/develop/ui/compose/performance/stability/diagnose
  The pages do not say whether the app must be debuggable.
- Mechanism [SRC `RecompositionHandler.kt`, androidx-main]: Studio injects an inspector that uses `ArtTooling` to install
  bytecode hooks on the composer implementations (`ComposerImpl`, `GapComposer`, `LinkComposer`): an exit hook on
  `startRestartGroup(I)Composer` increments a per-anchor recomposition count (keyed by `Composer.recomposeScopeIdentity`),
  and an entry hook on `skipToGroupEnd()V` increments the skip count (`RecompositionData.incrementSkip` subtracts 1 from
  the count and adds 1 to skips). The code states that production hooks come from Studio and are reinstalled each time the
  inspector is created; hooks are never removed.
  https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/compose/ui/ui-inspection/src/main/java/androidx/compose/ui/inspection/recompositions/RecompositionHandler.kt
- Therefore the counts are produced inside Studio's inspector agent; there is no public API in the app that returns them.
  What an app can do in-process with public/experimental API is the observer path in 1.1 and 1.2 (invalidations, scope
  enter/exit, reads), which is also what Studio uses for its "state reads" feature.

## 1.8 Compose compiler stability and skippability reports

- Enable [WEB diagnose page]: `composeCompiler { reportsDestination = ...; metricsDestination = ... }` per module. Outputs:
  `<module>-classes.txt`, `<module>-composables.txt`, `<module>-composables.csv`; metrics adds module totals.
  Page text: "Make sure to always run this on a release build to ensure accurate results." Build-time compiler output only,
  nothing at runtime.
  https://developer.android.com/develop/ui/compose/performance/stability/diagnose
- Extension properties [WEB kotlinlang API]: `reportsDestination` ("Save Compose build reports to this folder"),
  `metricsDestination` ("Save Compose build metrics to this folder"), `stabilityConfigurationFiles`, `includeSourceInformation`,
  `includeTraceMarkers`, `featureFlags`, `generateFunctionKeyMetaClasses`, `includeComposeMappingFile`. Defaults are not
  stated on that page. https://kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin/org.jetbrains.kotlin.compose.compiler.gradle/-compose-compiler-gradle-plugin-extension/
- File formats [WEB JetBrains design doc]: `-module.json` has integer counts: skippableComposables, restartableComposables,
  readonlyComposables, totalComposables, restartGroups, totalGroups, staticArguments, certainArguments, knownStableArguments,
  knownUnstableArguments, unknownStableArguments, totalArguments, markedStableClasses, inferredStableClasses,
  inferredUnstableClasses, inferredUncertainClasses, effectivelyStableClasses, totalClasses, memoizedLambdas,
  singletonLambdas, singletonComposableLambdas, composableLambdas, totalLambdas. `-composables.txt` is pseudo-Kotlin
  signatures marking each composable restartable/skippable/readonly and each parameter stable/unstable; `-classes.txt` marks
  classes stable/unstable/runtime. https://github.com/JetBrains/kotlin/blob/master/plugins/compose/design/compiler-metrics.md
- Strong skipping: on by default since Kotlin 2.0.20; all restartable composables become skippable; unstable parameters are
  compared by instance (`===`), stable ones by `equals`; lambdas with unstable captures are remembered; opt out per
  composable with `@NonSkippableComposable` [WEB]. https://developer.android.com/develop/ui/compose/performance/stability/strongskipping
  The diagnose page's example ("a composable with an unstable List parameter is never skipped") reads as written for the
  pre-strong-skipping behaviour; the strong-skipping page and the performance agent skill say unstable parameters are
  compared by instance equality, so the two pages disagree for that case when strong skipping is on [INFER which applies].
- Project already generates these (Compose.kt/ComposeStability.kt on wp/HUD).

## 1.9 Official guidance on measuring recomposition in production builds

None found. What the official pages say instead:
- Compose performance tooling page: Layout Inspector for counts, composition tracing for system traces; "Recomposition in
  itself is not bad; however, unexpected recomposition can be an issue." No production or release guidance. [WEB]
  https://developer.android.com/develop/ui/compose/performance/tooling
- Compose performance page: run in release mode, enable R8; nothing about measuring recomposition. [WEB]
  https://developer.android.com/develop/ui/compose/performance
- Android's Compose performance agent skill: "Never profile or benchmark debuggable builds"; "Do not overindex on recomposition
  counts" (skipping one composable saves little once composition is scheduled; verify with benchmarks). [WEB]
  https://developer.android.com/agents/skills/jetpack-compose/jetpack-compose-performance/skill
- Third-party, for reference only: `himattm/dejavu` (test-only library) counts recompositions with `Composer.setTracer`
  (CompositionTracer) and says nothing about release or minified builds. https://github.com/himattm/dejavu

## 1.10 Derived facts that bear on recomposition cost (chain of cited facts)

- The Recomposer's frame work runs inside `parentFrameClock.withFrameNanos` [SRC Recomposer.kt]; Compose's
  `AndroidUiFrameClock` posts that through `AndroidUiDispatcher`/`Choreographer.postFrameCallback` [SRC AndroidUiFrameClock.android.kt];
  `Choreographer.postFrameCallbackDelayed` posts as `CALLBACK_ANIMATION` [SDK Choreographer.java line 748-753];
  `FrameMetrics.ANIMATION_DURATION` is "time issuing animation callbacks" [SDK]. So recompose-and-apply time lands in
  `ANIMATION_DURATION` [INFER from that chain]; measure/layout of Compose nodes lands in `LAYOUT_MEASURE_DURATION` and draw
  recording in `DRAW_DURATION`.
- JankStats sums UNKNOWN_DELAY+INPUT+ANIMATION+LAYOUT_MEASURE+DRAW+SYNC into `frameDurationUiNanos` (see 2.2), so a
  JankStats-only listener cannot separate composition from layout or draw; a `Window.OnFrameMetricsAvailableListener` can.

---------------------------------------------------------------------------------------------------------------------

# PART 2. Other cheap runtime signals

API levels are from [SDK] `api-versions.xml` unless stated. minSdk of the project is 26.

## 2.1 FrameMetrics (per-frame stage timings)

Reference: https://developer.android.com/reference/android/view/FrameMetrics , https://developer.android.com/reference/android/view/Window.OnFrameMetricsAvailableListener
Registration: `Window.addOnFrameMetricsAvailableListener(listener, Handler)` / `remove...` API 24; "Must be in hardware
rendering mode"; throws `IllegalStateException` without an attached decor view; with a non-accelerated view it only logs a
warning [SDK Window.java, View.java].

| Constant | Meaning (javadoc) | API |
|---|---|---|
| UNKNOWN_DELAY_DURATION | ns waiting for the UI thread to become responsive and process the frame; "should be 0 most of the time" | 24 |
| INPUT_HANDLING_DURATION | ns issuing input handling callbacks | 24 |
| ANIMATION_DURATION | ns issuing animation callbacks | 24 |
| LAYOUT_MEASURE_DURATION | ns measuring and laying out invalidated views | 24 |
| DRAW_DURATION | ns computing display lists | 24 |
| SYNC_DURATION | ns syncing display lists to the render thread | 24 |
| COMMAND_ISSUE_DURATION | ns issuing draw commands to the GPU | 24 |
| SWAP_BUFFERS_DURATION | ns issuing the frame buffer to the display subsystem | 24 |
| TOTAL_DURATION | frame begin to end; may not equal the sum because stages overlap (internally INTENDED_VSYNC to FRAME_COMPLETED) | 24 |
| FIRST_DRAW_FRAME | 1 if first draw in a new window layout (usually exempt from jank) | 24 |
| INTENDED_VSYNC_TIMESTAMP | intended start; differs from VSYNC_TIMESTAMP when the UI thread was busy | 26 |
| VSYNC_TIMESTAMP | actual vsync time used by Choreographer callbacks and animations | 26 |
| GPU_DURATION | total ns on the GPU (internally COMMAND_SUBMISSION_COMPLETED to GPU_COMPLETED) | 31 |
| DEADLINE | total ns the system allotted; TOTAL_DURATION < DEADLINE means the deadline was met | 31 |
| FRAME_TIMELINE_VSYNC_ID | id correlating the frame with compositor timeline data (flagged API, `FLAG_JANK_API`) | 36 |

- CPU vs GPU: UI-thread stages are UNKNOWN_DELAY through DRAW plus SYNC; render-thread stages are COMMAND_ISSUE and SWAP_BUFFERS;
  GPU time is GPU_DURATION (API 31+ only). JankStats on API 31+ defines CPU time as TOTAL - GPU + SWAP_BUFFERS [SRC JankStatsApi31Impl.kt].
- Cost and threading [SDK javadoc]: the callback runs on the `Handler` you pass; "Reports can be dropped if this callback
  takes too long"; the `FrameMetrics` object is reused, so copy it with `FrameMetrics(FrameMetrics)` (copies 25 longs) and
  defer work; `dropCountSinceLastInvocation` reports dropped reports. Several listeners on one window are allowed (each gets
  its own `FrameMetricsObserver`, own `FrameMetrics` object and Handler) [SDK View.java].
- Release-like build: works in any build; no debuggable gate.
- Aggregated alternative: `androidx.core.app.FrameMetricsAggregator` (API 24+, otherwise a no-op returning null): per Activity
  histograms in whole ms for TOTAL, INPUT, LAYOUT_MEASURE, DRAW, SYNC, COMMAND, SWAP, DELAY, ANIMATION; uses a shared
  HandlerThread named "FrameMetricsAggregator" [SRC core/core/.../FrameMetricsAggregator.java].

## 2.2 JankStats (androidx.metrics:metrics-performance 1.0.0)

https://developer.android.com/topic/performance/jankstats ; source files under
https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/metrics/metrics-performance/src/main/java/androidx/metrics/performance/
- `JankStats.createAndTrack(window, OnFrameListener)` (UI thread; decor view required); `isTrackingEnabled` (default true;
  page says set false in onPause); `jankHeuristicMultiplier` default 2.0 times the refresh rate.
- `FrameData`: `frameStartNanos`, `frameDurationUiNanos` (sum of UNKNOWN_DELAY, INPUT, ANIMATION, LAYOUT_MEASURE, DRAW, SYNC),
  `isJank`, `states`. The doc page lists `FrameDataApi24.frameDurationCpuNanos` and `FrameDataApi31.frameOverrunNanos`
  (TOTAL - DEADLINE; positive means past the deadline) as API 31+ fields; the `FrameDataApi31` constructor also carries
  `frameDurationTotalNanos` [SRC FrameDataApi31.kt]. `isJank` is `uiDuration > expectedDuration`; on API 31+ the expected
  duration is the platform's DEADLINE metric (`getExpectedFrameDuration` returns `metrics.getMetric(DEADLINE)`), before that it
  is derived from the refresh rate; the Api24 listener multiplies the expected duration by `jankHeuristicMultiplier`, and I
  inferred, without reading the superclass, that the multiplier applies on API 31+ too [SRC Api24/Api31Impl, INFER].
- States: `PerformanceMetricsState.getHolderForHierarchy(view)`, `putState(key, value)`, `removeState`, `putSingleFrameState`;
  the holder's `state` is non-null only while JankStats tracks that window. Keys must be unique per instance.
- Threading: API 24+ the listener runs on a shared HandlerThread "FrameMetricsAggregator" (static handler field, shared by
  windows); API 23 and lower on the main thread. The `FrameData` object is reused; copy it. One JankStats per window; the
  delegator is cached on the decor view tag and shared by further JankStats instances.
- Official wording on cost: return quickly from the callback; no overhead number. The page also still describes saving and
  uploading as outside the scope of "the alpha JankStats API release" although the coordinate is 1.0.0.
- Release-like build: works; below API 24 it falls back to OnPreDraw estimates (not relevant at minSdk 26).

## 2.3 PowerManager thermal status and headroom

https://developer.android.com/reference/android/os/PowerManager ; guide https://developer.android.com/games/optimize/adpf/thermal
| API | Gives | API level |
|---|---|---|
| `getCurrentThermalStatus()` | int `THERMAL_STATUS_NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN` | 29 |
| `addThermalStatusListener([Executor,] OnThermalStatusChangedListener)` / remove | push on status change; default callbacks are enqueued on the main thread, or the Executor you pass | 29 |
| `getThermalHeadroom(forecastSeconds 0..60)` | float; 1.0 = throttled at SEVERE; may exceed 1.0; NaN if unsupported; forecast needs several samples (a few seconds) first | 30 |
| `getThermalHeadroomThresholds()` | map status to headroom threshold; keys only for statuses the OEM defines; from Android 16 (BAKLAVA) the map can change between calls; throws `UnsupportedOperationException` if not enabled | 35 (flagged `FLAG_ALLOW_THERMAL_HEADROOM_THRESHOLDS`) |
| `addThermalHeadroomListener([Executor,] OnThermalHeadroomChangedListener)` | headroom, forecastHeadroom, forecastSeconds, thresholds on significant change; does not notify on forecast change | 36 in api-versions (flagged `FLAG_ALLOW_THERMAL_THRESHOLDS_CALLBACK`) |

- Cost [SDK source]: each read is a binder call to the thermal service (`mThermalService`). Rate limits: javadoc says no
  benefit calling `getThermalHeadroom` more than about once per second and much more often "may result in ... NaN"; the ADPF guide
  says not more than once every 10 seconds, warns that calling from several threads can return NaN, and that some devices
  return `THERMAL_STATUS_NONE` regardless of real throttling, so validate with headroom. If the first headroom reading is NaN
  the device does not support it.
- Release-like build: works; no permission is declared in the javadoc.

## 2.4 Debug runtime stats (GC)

https://developer.android.com/reference/android/os/Debug#getRuntimeStat(java.lang.String) ; API 23 for `getRuntimeStat(String)` and `getRuntimeStats()`.
Keys, all API 23 and "approximate" [SDK javadoc]: `art.gc.gc-count`, `art.gc.gc-time` (ms, total), `art.gc.bytes-allocated`,
`art.gc.bytes-freed`, `art.gc.blocking-gc-count`, `art.gc.blocking-gc-time` (ms), `art.gc.gc-count-rate-histogram` and
`art.gc.blocking-gc-count-rate-histogram` (strings like "0:34503,1:45350,..."; a sample every 10 s since process start).
Values are strings, null if unknown; "may be added or removed in a future API level".
Cost: no published number; the project's own MemorySampler treats each as a counter read. Release-like: works.
Also `Debug.threadCpuTimeNanos()` (CPU time of the calling thread; -1 if unsupported; only meaningful as a difference) and
`Process.getElapsedCpuTime()` (CPU time of the process since start; no `since` in api-versions, so API 1) exist.

## 2.5 Memory: Debug.getMemoryInfo, ActivityManager, trim callbacks

- `Debug.getMemoryInfo(MemoryInfo)` and `MemoryInfo.getMemoryStat(String)` / `getMemoryStats()` (API 23): keys
  `summary.java-heap`, `summary.native-heap`, `summary.code`, `summary.stack`, `summary.graphics`, `summary.private-other`,
  `summary.system`, `summary.total-pss`, `summary.total-swap`, all kB. Javadoc caveat: it reads low-level data directly and
  "may not be able to retrieve information about some protected allocations, such as graphics"; use
  `ActivityManager.getProcessMemoryInfo(int[])` (API 5) to see all, which the javadoc calls "only intended for debugging" and,
  since Android 10, "significantly limited" in sample rate (a faster call returns the previous data) and own uid only.
  https://developer.android.com/reference/android/os/Debug.MemoryInfo#getMemoryStats()
- Cost of PSS: project measurement in `templates/android/docs/PERFORMANCE-OVERLAY.md` (wp/HUD): reading PSS walks every
  memory mapping under the kernel's address-space lock and on the Redmi Note 10 Pro Max a read during a scroll costs about one
  janky frame; the overlay reads it at most every 15 s and only after a quarter second without a frame. Not an official figure.
  The official memory guide only says PSS is slower to compute than RSS. https://developer.android.com/topic/performance/memory-management
- `ActivityManager.getMemoryClass()` (API 5; MB, per-app heap guidance), `getLargeMemoryClass()` (API 11; heap if
  `largeHeap`), `isLowRamDevice()` (API 19; roughly 1 GB RAM or less), `getMemoryInfo(MemoryInfo)` (availMem, totalMem API 16,
  threshold, lowMemory, advertisedMem API 34, `freeMem` API 37 flagged). Javadoc: "polling is not recommended"; in SDK 37 the
  call is served through a rate-limiting cache (at most 100 service calls per second). `getMyMemoryState(RunningAppProcessInfo)`
  (API 16) fills pid, uid, `lastTrimLevel`, importance, lru, importanceReasonCode; a binder call.
  https://developer.android.com/reference/android/app/ActivityManager
- `ComponentCallbacks2.onTrimMemory`: only `TRIM_MEMORY_UI_HIDDEN` (20) and `TRIM_MEMORY_BACKGROUND` (40) are current. The
  levels `RUNNING_MODERATE/LOW/CRITICAL`, `MODERATE`, `COMPLETE` are deprecated at 35 with the note "Apps are not notified of
  this level since API level 34". Compare with `>=`, never equality. https://developer.android.com/reference/android/content/ComponentCallbacks2
- Release-like build: all work.

## 2.6 Looper message timing on the main thread

https://developer.android.com/reference/android/os/Looper#setMessageLogging(android.util.Printer) (API 1)
- `Looper.setMessageLogging(Printer)`: the Printer receives a line before and after every dispatched message. Javadoc:
  "Message logging introduces a performance penalty, and is disabled by default." Source [SDK Looper.java 255-260, 343]: the
  start line is built by string concatenation (`">>>>> Dispatching to " + msg.target + " " + msg.callback + ": " + msg.what`)
  for every message while a printer is set, and an end line `"<<<<< Finished to ..."` after it. Setting a non-null printer
  calls `StrictMode.noteSlowCall("setMessageLogging")` on the calling thread, which the project's StrictModeWatch
  (`detectCustomSlowCalls()`) would count as a violation if called from a thread under that policy.
- Not public: `Looper.setObserver` and `setSlowLogThresholdMs` are `@hide` [SDK]. `MessageQueue.isIdle()` API 23,
  `Looper.getQueue()` API 23, `Handler.getMessageName` API 14.
- Cheaper main-thread stall signals already in the platform: `FrameMetrics.UNKNOWN_DELAY_DURATION` (2.1); `Choreographer.postFrameCallback`
  (API 16) gives the frame time of each callback, `postVsyncCallback` + `FrameData.getFrameTimelines()/getPreferredFrameTimeline()`
  (vsyncId, expectedPresentationTimeNanos, deadlineNanos) from API 33. `ApplicationExitInfo.REASON_ANR` (API 30) reports ANRs
  after the fact via `getHistoricalProcessExitReasons`.
- Release-like build: works in any build.

## 2.7 Display refresh rate and frame-rate override

https://developer.android.com/reference/android/view/Display#getRefreshRate()
- `Display.getRefreshRate()` (API 1), `getMode()` / `getSupportedModes()` (API 23), `Display.Mode.getRefreshRate()`,
  `getAlternativeRefreshRates()` (31), `getSupportedRefreshRates()` (api-versions says 21; flagged `FLAG_ENABLE_GET_SUPPORTED_REFRESH_RATES`;
  javadoc: Android 16 (BAKLAVA) and above returns the display's supported render rates, Android 15 and below only the rates of the
  default modes, so use `getSupportedModes()` for more). [SDK]
- What `getRefreshRate()` returns [SDK `DisplayInfo.java`, API 37 source]: "the refresh rate the application would
  experience": the per-app `refreshRateOverride` if above 0, else `renderFrameRate` (a divisor of the active mode's rate, the
  rate SurfaceFlinger consumes frames and "observable by applications via the cadence of Choreographer callbacks") if above 0,
  else the active mode's rate. The `renderFrameRate` branch is in the API 37 source; I did not check older releases. No
  developer.android.com page describing "frame rate override" was found; this is AOSP source text.
- Change notifications: `DisplayManager.DisplayListener.onDisplayChanged` (API 17). In the API 37 source `getRefreshRate()`
  lazily registers for refresh-rate changes (`registerForRefreshRateChanges`) the first time it is called; the newer
  `registerDisplayListener(Executor, eventFlags, listener)` (API 36) has `EVENT_TYPE_DISPLAY_REFRESH_RATE` (flagged) and says
  `EVENT_TYPE_DISPLAY_CHANGED` does not fire for refresh-rate changes "as they can change very often".
- Requests and system-side behaviour: `Surface.setFrameRate` (API 30/31); `View.setRequestedFrameRate` + categories,
  `Window.setFrameRatePowerSavingsBalanced`, `Window.setFrameRateBoostOnTouchEnabled` (API 35); `Display.hasArrSupport()`
  (API 36; page says ARR needs devices implementing specific HAL APIs and Android 15 QPR1+); `Display.getSuggestedFrameRate`
  (36), `getFrameRateVelocityMapping` (37). The games page says the system may override a requested rate for battery or
  temperature, Android 15 defaults games to 60 Hz, and recommends measuring with "a frame counter or performance overlay"; it
  names no API for reading the active rate. https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate
  and https://developer.android.com/games/optimize/display-refresh-rate-change
- Cost: `getRefreshRate()` takes the Display lock and calls `updateDisplayInfoLocked()`; I did not trace whether that can reach
  the system server when the cached `DisplayInfo` is stale. Release-like: works.

## 2.8 ApplicationStartInfo (API 35)

https://developer.android.com/reference/android/app/ApplicationStartInfo
- `ActivityManager.addApplicationStartInfoCompletionListener(Executor, Consumer)`: called when the record of this startup is
  complete, i.e. first frame drawn; does not wait for `reportFullyDrawn`; if the start is already complete it is invoked
  immediately; once, then removed; javadoc says it is asynchronous and should be made from a background thread.
  `getHistoricalProcessStartReasons(maxNum)`: ring buffer, most recent first, can include in-progress starts. `addStartInfoTimestamp(int, long)`
  for developer timestamps in the range 21..30. All API 35.
- Fields: `getStartType()` COLD/WARM/HOT (set for FIRST_FRAME_DRAWN), `getReason()`, `getStartupState()`
  (STARTED/ERROR/FIRST_FRAME_DRAWN), `getLaunchMode()`, `getStartComponent()` (API 36, flagged), `getStartupTimestamps()` map of
  monotonic ns: LAUNCH(0), FORK(1), APPLICATION_ONCREATE(2), BIND_APPLICATION(3), FIRST_FRAME(4), FULLY_DRAWN(5, only if the
  app calls `reportFullyDrawn`), INITIAL_RENDERTHREAD_FRAME(6), SURFACEFLINGER_COMPOSITION_COMPLETE(7). Guaranteed
  timestamps: LAUNCH at STARTED; APPLICATION_ONCREATE, BIND_APPLICATION, FIRST_FRAME at FIRST_FRAME_DRAWN. For service starts
  LAUNCH "may be incorrect in BAKLAVA and below". [SDK]
- Cost: one system call per read; the system fills the record. Nothing on API 34 and lower (minSdk is 26).
- Release-like build: works.

## 2.9 Bitmap and texture memory

- Observable in process: `Debug.MemoryInfo` `summary.graphics` (kB; see 2.5 for the "protected allocations" caveat) and
  `summary.native-heap`; `Debug.getNativeHeapAllocatedSize()`; `Runtime` heap numbers; per bitmap
  `Bitmap.getAllocationByteCount()` (API 19) and `getByteCount()` (API 12). `Bitmap.Config.HARDWARE` (API 26) bitmaps are
  stored only in graphics memory, are immutable and their pixel access throws `IllegalStateException`. [SDK]
- I found no public in-process API that returns per-process GPU or texture memory. `dumpsys meminfo` / `dumpsys gfxinfo` are
  adb-side. A search shows AOSP changed how the Graphics and GL categories are accounted in Android 12 (DMA-BUF and GPU private
  memory) and that devices upgraded to S need not update their memtrack HAL, so coverage varies by device
  (https://android.googlesource.com/platform/frameworks/base/+/9785b9a84bf4 and the memtrack HAL docs); secondary evidence.
- The Memory Profiler doc page fetched did not describe the Graphics category.

## 2.10 Other platform signals seen while checking (levels from api-versions)

`PerformanceHintManager.createHintSession` (31), `ProfilingManager.requestProfiling` (35), `ApplicationExitInfo` (30),
`TrafficStats.getUidRxBytes/TxBytes` (8), `Process.getStartElapsedRealtime` (24), `StrictMode.noteSlowCall` (11),
`ViewTreeObserver.addOnDrawListener` (16). Not researched beyond existence and level.

---------------------------------------------------------------------------------------------------------------------

# Not found or unverified

1. Official guidance on measuring recomposition in production (non-debuggable, minified) builds: none on the Compose
   performance, tooling, debug, stability or agent-skill pages.
2. Whether Layout Inspector's recomposition counts require a debuggable app: not stated on the official pages.
3. A public API that returns Layout Inspector's recomposition or skip counts, or a public "skipped" indicator on
   `CompositionObserver`: none found (the skip flag is internal `RecomposeScopeImpl.skipped`).
4. A published overhead figure for `CompositionObserver`, `Looper.setMessageLogging`, `Debug.getRuntimeStat`: only qualitative text.
5. A developer.android.com page for per-app "frame rate override": only AOSP `DisplayInfo` source comments.
6. Public in-process per-process GPU or texture memory API: none found.
7. `RecompositionTracer` in a stable Compose release: only in 1.13.0-alpha01 and later; BOM 2026.09.00 ships 1.12.1.
8. The developer.android.com reference page bodies for the platform classes could not be read through the fetcher; their
   facts come from the SDK 37 sources.
