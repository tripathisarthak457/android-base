# Performance overlay: recomposition score, "not optimised" flags and more signals

REQUIREMENTS 49 (the user, Oct 20 00:33), building on 40 (the overlay), 22 (recomposition) and 42 (release gates).
Design only; built on `staging-wp/HUD` after staging and the user's go (REQUIREMENTS 44). Revision 2, Oct 20, after
the design review (what changed: section 2.8). Code read from branch heads: `staging` at `f59721013` (HUD merged at
`c9e71640`, `perf.hud=off` default at `591d6d4e`, WP02 merged at `76cd66cdc` with `runtime-tracing` in the dev
flavour and the build-logic guard under `com/base/app/buildlogic/guard/`), `wp/HUD` at `936fa2e8`, `wp/WP04`
(`gates.toml`, identical on staging). Every API fact below was checked against the artifacts this project builds
with (section 2.0), not taken from the lookup.

---

## Part 1. In plain English

**What you will see.** The pill beside the DEV badge keeps its two numbers (frames per second and janky %). The
Perf tab gains three things:

1. **A recomposition score.** Compose apps stay smooth when moving things are *redrawn*, not *rebuilt*. Rebuilding
   part of the UI because a value it reads changed (a recomposition) is fine on the first frame of a transition or
   after a tap. It is a bug when it happens on every frame of a scroll, an animation or a transition: REQUIREMENTS 22
   forbids it, and it costs main-thread time in exactly the frames that must be on time. The overlay watches every
   motion and counts the frames that rebuilt something, per screen and per state:
   - **Scrolling or dragging** (a finger is down, and the fling after it),
   - **Animating** (motion without a finger: animations, settles, a back gesture that is cancelled),
   - **Transition** (the screen changed during the motion: push, pop, tab switch, a committed back gesture),
   - **Idle** (nothing moving and nobody touching).

   Building new content is kept apart from rebuilding: list rows scrolling into view and the first frame of a new
   screen are not held against the app. A second check catches the other classic offender: a layout that builds its
   children while measuring (`BoxWithConstraints`, a scaffold) being rebuilt from scratch on every frame of a size
   animation. Grades work like the other metrics: green when motions are quiet between their first and last frame,
   **red when something rebuilds on most frames of a motion**, amber when the app keeps rebuilding on a timer while
   idle (red if that also draws frames, which the release gate forbids). A **Capture** button records 10 seconds and
   lists the composables that rebuilt, by name and file:line, worst first. The names work in `devRelease` too,
   although it is minified.
2. **"Not optimised" flags.** One list at the top of the Perf tab: every metric that misses its target, why it
   matters, the limit it was held to (from `gates.toml`, the recomposition budgets or Android's own thresholds),
   and the fix in one or two sentences with a link to the doc.
3. **More signals, only cheap ones:** jank after each motion's first frame (measured the way the overlay can,
   comparable to the release gate's rows), whether late frames lost their time on the main thread or the GPU, how
   much the app process allocates per frame drawn, blocking garbage collections while drawing, the phone's thermal
   throttling (numbers taken on a hot phone are marked), whether the app hit an ANR since this build was installed,
   and whether the app reports "fully drawn" so start-up can be judged.

**What it costs.** Nothing while the overlay is hidden: no listener, no thread, no counter. While shown, it adds a
few counter increments to each composable that actually runs and one tiny main-thread message per frame that
composed anything; frames that compose nothing cost nothing extra. It allocates nothing per frame and never rebuilds
app content. Target: under 0.1 ms per frame on the Redmi, inside REQUIREMENTS 40's hard limits (+0.3 points of jank,
0.5 ms per frame). The 10-second Capture costs more and says so.

**Perfetto traces keep their names.** Dev builds include Compose's composition tracing (added by WP02), which
names each composable in a Perfetto trace. Compose has a single hook for this. While the overlay records it takes
the hook and passes every event on to Perfetto, so a trace taken at the same time still shows the names; when it
stops, it hands the hook back.

**What changes in how we measure.** The builds the perf harness measures (`devBenchmarkRelease`, and
`devNonMinifiedRelease`, which records the baseline profile) will no longer contain the DEV badge, the inspector or
the overlay. The baseline-profile plugin copies the release build's normal dependencies into both of those builds,
so the tooling is wired as a run-time-only dependency of `devRelease`, which the plugin does not copy. The build
fails if the tooling reaches a measured build anyway, and `perf.sh` refuses to gate one that has it. About the
Oct 19 regression (the combined app about twice as hitchy on the back gesture): the overlay was very likely **not
recording** in that run (section 2.4.1), so its frame listener is not the suspect; the measured build did carry the
dev tooling at rest, including a translucent DEV badge drawn over every frame. Removing the tooling from measured
builds settles that part either way, and the first A/B run measures how much it was.

**What you decide:** section 2.6 (what the pill shows; whether recomposition counts in the 0-100 score; the badge's
look if it turns out to cost frames). One item there is information only: measured builds lose the DEV badge, as
REQUIREMENTS 49 says.

---

## Part 2. Engineering detail

### 2.0 Facts this design relies on, and how each was checked

Artifacts from `D:/.caches/gradle`, read with `javap` (JBR): `runtime-android-1.12.1` (BOM `2026.09.00`),
`runtime-tracing-1.12.1` (its manifest and POM), `tracing-perfetto-1.0.1`, `startup-runtime-1.1.1` (consumer
rules), `ui-android` 1.12.1, `foundation-android` 1.12.1, `foundation-layout-android` 1.12.1, `metrics-performance`
1.0.0, `compose-compiler-gradle-plugin` 2.4.20, `benchmark-baseline-profile-gradle-plugin` 1.5.0,
`hilt-android-gradle-plugin` 2.60.1. Platform: `Sdk/sources/android-37.0` and
`platforms/android-37.0/data/api-versions.xml`.

| # | Fact | Checked by | Consequence |
|---|---|---|---|
| F1 | `Composer.setTracer(CompositionTracer)` exists; `CompositionTracer` has `isTraceInProgress()`, `traceEventStart(key, dirty1, dirty2, info)`, `traceEventEnd()`; annotated `@InternalComposeTracingApi`. `ComposerKt.compositionTracer` is a private, non-volatile static with no getter; `isTraceInProgress()` is a null check when none is set. | `javap` of `Composer$Companion`, `CompositionTracer`, `ComposerKt` | Zero cost when not installed; one global slot; another tracer cannot be detected, only overwritten. |
| F2 | Compiled composables call `traceEventStart(key, $dirty, -1, "fq.Name (File.kt:line)")` only inside the branch that executes (after `shouldExecute`), never for a skip. | `javap -c` of `BoxKt.Box` (foundation-layout); the traced lambdas of `LayoutNodeSubcompositionsState` and `GapComposer` | Counts executions, not skips; names come from a string literal. |
| F3 | **The force bit (bit 0 of `dirty1`) is set by a scope restart and by every composition root.** Roots: `Expect_jvmKt.invokeComposable` calls the content with `changed = 1` (`iconst_1`), for every `setContent`, `setContentWithReuse` and `setPausableContent` (`GapComposer.doCompose`). `ComposableLambdaImpl.invoke(c, changed)` ORs in `differentBits(0)` (`0b100`) when `c.changed(this)` is true and `sameBits(0)` (`0b010`) otherwise; a root is new (inserting), reused (`GapComposer.nextSlot()` returns `Empty` while `reusing`, so `changed` is true) or a new lambda instance, so it reads `0b101`. Restarts: `RecomposeScopeImpl.compose` passes 1; a lambda's restart (`ComposableLambdaImpl$invoke$1`) re-invokes with it and, the instance being the same, reads `0b011`; a function's restart passes `updateChangedFlags($changed or 1)`, which turns every Different field into Same (`lo = d and 0x12492492`, `hi = d and 0x24924924`, result `(d and 0xC9249249) or lo or (hi shr 1) or ((lo shl 1) and hi)`). The 2-argument `traceEventStart` overload passes `dirty1 = -1`. | `javap -c` of `Expect_jvmKt`, `ComposableLambdaImpl.invoke`, `ComposableLambdaImpl$invoke$1`, `ComposableLambdaKt.bitsForSlot`, `RecomposeScopeImpl.compose`, `RecomposeScopeImplKt.updateChangedFlags`, `GapComposer.nextSlot`/`changed`, `ComposerKt.traceEventStart(I,String)` | **Restart** = force bit set and no 3-bit parameter field of `dirty1` (or of `dirty2` when it is not −1) reads Different (`0b10` in its low two bits). **Root** = force bit set and some field reads Different. Neither = called by its parent. |
| F3a | **SubcomposeLayout root paths.** `LayoutNodeSubcompositionsState.subcompose` passes the slot's own content to the composition when `Owner.outOfFrameExecutor` is non-null, and `AndroidComposeView.getOutOfFrameExecutor()` returns the view itself while it is attached; only a detached owner gets a fresh `composableLambdaInstance(1524156494, …)` wrapper, whose traced body (`…subcompose.<anonymous>… (SubcomposeLayout.kt:706)`) passes 0 to the content. Movable content is wrapped in a fresh `composableLambdaInstance(-59194059, …)` (`GapComposer.kt:2265`) that passes 0 to the content. A slot is set again when its content instance changed, its composition has invalidations, or `forceRecompose` is set; `SubcomposeLayout` calls `forceRecomposeChildren()` from a `SideEffect` whenever it did not skip. | `javap -c` of `LayoutNodeSubcompositionsState.subcompose` (both overloads), `subcompose$lambda$5$0$0`, `AndroidComposeView.getOutOfFrameExecutor`, `Owner.getOutOfFrameExecutor` (default null), `GapComposer.invokeMovableContentLambda`, `SubcomposeLayoutKt.SubcomposeLayout` | Every root path reads `0b101` (the root itself) and passes no force bit below it. A slot set again with the **same** content instance reads `0b011` and counts as a restart of that slot root: the composition really re-runs from there (its own state changed during measure, or its `SubcomposeLayout` was recomposed). A slot given a new lambda on every measure (AppScaffold's content slot, `BoxWithConstraints`) reads as a root each time it is measured. |
| F3b | **Lazy rows.** A lazy row's root (attached path) is `androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactory.CachedItemContent.createContentLambda.<anonymous> (LazyLayoutItemContentFactory.kt:85)`, a `composableLambdaInstance(818252804, true, …)` cached per key, whose body reads `itemProvider()`. `LazyListScrollPosition` builds `LazyLayoutNearestRangeState(…, 30, 100)`; `update` sets a new range only when the first visible item crosses a multiple of 30 (`30 * (first / 30)`), and the item provider is derived from that range. | `javap -c` of `LazyLayoutItemContentFactory$CachedItemContent`, `LazyListScrollPosition.<init>`, `LazyLayoutNearestRangeState.update`, `…$Companion.calculateNearestItemsRange` | Rows entering a list are roots with that name prefix; about once per 30 items scrolled the visible rows' roots restart (the library's list window update). Both are classified by the prefix and never scored. |
| F4 | The runtime's consumer R8 rules strip only `sourceInformation*` (and assume values for `ComposeStackTraceMode.isMinified` and `ComposeRuntimeFlags.isLinkBufferComposerEnabled = false`, so minified builds use `GapComposer`); nothing strips `traceEventStart`/`isTraceInProgress`. The template has no `-assumenosideeffects` at all. The compiler plugin defaults: `includeTraceMarkers = true`, `includeSourceInformation = true`. | `runtime.aar!proguard.txt`; `git grep` on staging; `javap -c` of `ComposeCompilerGradlePluginExtension` | In a minified `devRelease`, trace strings (names with file:line) survive; slot-table source information does not. |
| F5 | `Recomposer.runningRecomposers: StateFlow<Set<RecomposerInfo>>` and `RecomposerInfo.changeCount` carry no opt-in; `changeCount` is a plain `long` incremented once in the frame callback, only when changes were applied. | `javap` of `Recomposer$Companion`, `RecomposerInfo`, `Recomposer$runRecomposeAndApplyChanges$2` | Used only to detect that another tool replaced the tracer (2.1.6). |
| F6 | `Compose:recompose` is traced in `GapComposer.doCompose`, which both a Recomposer pass and every `setContent`/subcomposition run. `perf.py`'s `recomposing_frames` marks a `Choreographer#doFrame` slice as recomposing when any `Compose:recompose` section opens inside it. | `javap -c` of `GapComposer.doCompose`; `perf.py` `recomposing_frames` (lines 1127-1172) on staging | `perf.sh recompose` counts frames with any composition inside the frame, roots included, and also a pass that executed nothing; the overlay's "frames that composed" is the comparable number (2.1.4), at or below it. |
| F7 | `CompositionObserver`, `CompositionRegistrationObserver`, `RecomposerInfo.observe` are `@ExperimentalComposeRuntimeApi`; no `RecompositionTracer` class in 1.12.1. Opt-in levels: `InternalComposeTracingApi` is `RequiresOptIn(ERROR)` "This is internal API that may change frequently and without warning."; `ExperimentalComposeRuntimeApi` is `RequiresOptIn(ERROR)` "experimental API that may change frequently and without warning". | `javap -v` of both annotation classes and the `tooling` package | `setTracer` is the more internal of the two APIs; 2.1.6 says why it is still the right hook and how a Compose bump is caught. |
| F8 | Compose `Dialog` composes with `rememberCompositionContext()` (shares the activity's Recomposer); the inspector panel is a `Dialog`. | byte search of `ui-android` classes; `DevToolsPanel.kt` on staging | Counting pauses while the panel is open. |
| F9 | **Measure-time composition runs inside the draw, after every app-visible pre-draw or on-draw callback.** `AndroidComposeView.scheduleMeasureAndLayout` calls `invalidate()`, and `requestLayout()` only when the change can resize the root; `AndroidComposeView.dispatchDraw` runs `Owner.measureAndLayout` before drawing. `ViewRootImpl` (SDK 37) dispatches pre-draw at line 4837, then `performDraw` (5940), whose `draw()` dispatches on-draw (6234) before the hierarchy draws. All of it runs inside the one `Choreographer#doFrame` message, and inside the frame's FrameMetrics UI window (`UNKNOWN_DELAY` through `SYNC`, F10). | `javap -c` of `AndroidComposeView.dispatchDraw` and `scheduleMeasureAndLayout`; SDK 37 `ViewRootImpl.java` | A frame boundary taken at pre-draw or on-draw splits a frame's composition in two. The overlay instead attributes composition by when it ran: one batch per main-thread message, stamped at its first event (2.1.1). |
| F10 | JankStats 1.0.0 API 31 frame data: UI = sum of FrameMetrics 0-5; `frameDurationCpuNanos = TOTAL − GPU_DURATION + SWAP_BUFFERS`; `frameOverrunNanos = TOTAL − DEADLINE`. | `javap -c` of `JankStatsApi31Impl.getFrameData` | Main thread, render thread and (GPU − swap) are separable per frame from data the overlay already receives. |
| F11 | `RenderNode.setHasOverlappingRendering`: "Non-overlapping rendering can use a fast path for alpha that avoids rendering to an offscreen buffer." Compose's `CompositingStrategy.Auto` sets overlapping rendering true without a compositing layer. | SDK 37 `RenderNode.java:1042`; `javap -c` of `GraphicsLayerV29.applyCompositingStrategy` | The DEV badge (`graphicsLayer { alpha = 0.85f }` at rest, Auto) is drawn through an offscreen buffer; whether HWUI re-renders it every frame is native code: the A/B trace answers it. |
| F12 | Thermal: `getCurrentThermalStatus`/`addThermalStatusListener` API 29; `getThermalHeadroom(int)` API 30, NaN when unsupported or called too often, 1.0 = `SEVERE`; `getThermalHeadroomThresholds` API 35. | SDK 37 `PowerManager.java:3120-3334`, api-versions | Push listener plus a headroom read every 10 s. |
| F13 | `Debug.getRuntimeStat("art.gc.bytes-allocated")`, `"art.gc.blocking-gc-count"` (API 23): process-wide totals, approximate. | SDK 37 `Debug.java:1862-1952` | Process allocation per frame drawn at 1 Hz, coarse but cheap; blocking GCs are the ones that pause the main thread. |
| F14 | `Looper.setMessageLogging` calls `StrictMode.noteSlowCall("setMessageLogging")`; the overlay's StrictMode watch enables `detectCustomSlowCalls()`. | SDK 37 `Looper.java:509-525`; `StrictModeWatch.kt` | Rejected (2.3.2). |
| F15 | `ApplicationStartInfo` API 35 (`START_TIMESTAMP_FULLY_DRAWN = 5`); `getHistoricalProcessExitReasons`, `ApplicationExitInfo.REASON_ANR`/`REASON_LOW_MEMORY` and `getTimestamp()` API 30; `PackageInfo.lastUpdateTime` API 9. | api-versions | Exit reason readable once per recording start, and comparable with the install time. |
| F16 | `PointerInputModifierNode.sharePointerInputWithSiblings()` exists in ui 1.12.1. | `javap` | An observer node sees touches without taking them (pinned by a test). |
| F17 | `perf.py` runs `pm clear` before every run, and at `e404e391` (the Oct 19 bug run) passed no `perf.hud` extra; `PerformanceRecorder` starts with `stored ?: environment.isDebugBuild`. | `git show e404e391:…/perf.py`, `staging:…/PerformanceRecorder.kt` | In the bug run the overlay was off (2.4.1). |
| F18 | The control tree's badge (`wp/WP04` `DevBadge.kt`) has no `graphicsLayer` alpha and no `systemGestureExclusion`; staging's `EdgeBadge` has both. | `git show wp/WP04:…/DevBadge.kt`; staging `DevToolsOverlay.kt` | Both arms had a badge; only the new one is translucent. |
| F19 | **runtime-tracing is in every dev build and owns the tracer slot from process start.** Staging's `app/build.gradle.kts:142` has `devImplementation(libs.androidx.compose.runtime.tracing)` (WP02 merge `76cd66cdc`). Its manifest merges `androidx.compose.runtime.tracing.ComposeTracingInitializer` into `InitializationProvider`; `create()` calls `Composer.setTracer` with a tracer whose `isTraceInProgress()` is `PerfettoSdkTrace.isEnabled()`, `traceEventStart` is `PerfettoSdkTrace.beginSection(info)` and `traceEventEnd` is `endSection()`. `PerfettoSdkTrace` (tracing-perfetto 1.0.1, a runtime dependency of runtime-tracing) is public, with no `RestrictTo`. startup-runtime's consumer rules keep every `Initializer`'s name and no-argument constructor. | `git show staging:…/app/build.gradle.kts`; `javap -c -v` of `ComposeTracingInitializer`, `ComposeTracingInitializer$create$1`, `PerfettoSdkTrace`; the AAR manifests and POM; `startup-runtime-1.1.1/proguard.txt` | The overlay forwards to Perfetto while it records and restores runtime-tracing's tracer when it stops (2.1.6). |
| F20 | **The baseline-profile plugin copies release dependencies into the measured build types.** `BaselineProfileAppTargetAgpPlugin.onApplicationVariants` calls `Dependencies.copy(buildType, "<mapped>")` and `copy(variant.name, camelCase(flavorName, "<mapped>"))` for the `nonMinified` and `benchmark` mappings; `Dependencies` copies `implementation`, `api`, `kapt`, `annotationProcessor`, `compile` and `compileOnly` (`DependencySet.addAll`), not `runtimeOnly`. | `javap -c` of `BaselineProfileAppTargetAgpPlugin` and `utils.Dependencies` (plugin 1.5.0) | `"devReleaseImplementation"` would put the tooling back into `devBenchmarkRelease` and `devNonMinifiedRelease`; `"devReleaseRuntimeOnly"` does not. |
| F21 | Hilt's aggregating task (plugin 2.60.1) takes the variant's runtime and compile configurations as input; the template does not turn the aggregating task off; `:app` imports nothing from `core.devtools` or `core.perfhud` (`DevTooling` is in `:core:ui`). | `javap -c` of `HiltGradlePlugin.configureAggregatingTask$lambda$0`; `git grep` on staging | Run-time-only wiring keeps the tooling's Hilt bindings in `devRelease` (inferred from the plugin; proven in build step 1 by the badge on a `devRelease` install). |
| F22 | Trace strings carry 0-based lines: `androidx.compose.foundation.layout.Box (Box.kt:232)` while `BoxKt.Box(Modifier, Composer, int)`'s line table starts at 233. | `javap -c -l` of `BoxKt` | Capture shows line + 1, pinned by a test (2.5). |

### 2.1 The recomposition score

#### 2.1.1 Signals

| Signal | API | Thread, when | Cost | Gives |
|---|---|---|---|---|
| **Composition tracer** | `Composer.setTracer` (F1-F3b), installed while recording | Main thread, inside each executed composable | Per execution: the compiler's two `isTraceInProgress()` calls (now true), a thread check, a bit test and one or two `int` increments; a root or restart also probes a 64-entry identity table to classify its name. Estimate 10-25 ns per execution; nothing when nothing composes. | Per batch: executions, app restarts, list-window restarts, roots outside lists, list-row roots. Names during a capture. |
| **Batch close** | At a batch's first event: one `System.nanoTime()`, and one asynchronous message sent to the front of the main queue (`Message.obtain(handler, close)`, `isAsynchronous = true`, `sendMessageAtFrontOfQueue`) | Runs as soon as the current main-thread message ends: after the whole `doFrame`, draw included (F9), before the next frame's message and past any sync barrier | One clock read and one pooled message per main-thread message that composed; zero in messages that compose nothing | Closes the batch and publishes `(stamp, counters)` to a 128-entry ring. |
| **Recomposer change count** | `RecomposerInfo.changeCount` (F5), recomposers copied to an array when the set changes | Read when a batch closes | A field read per running recomposer | Detects a replaced tracer (2.1.6). |
| **Motion segmentation** | The frame log the overlay already keeps (start, UI time, interval, deadline) | JankStats' thread, per frame, under `FrameLog`'s existing lock | O(1) integer work | Motions, their first/middle/last frames, state, screen. |
| **Screen** | `ScreenTimeline` (run 6, unchanged) | as today | as today | The screen a motion belongs to. |
| **Touch** | A `Modifier.Node` observer (`PointerInputModifierNode`, `sharePointerInputWithSiblings = true`, never consumes) composed full-window by the overlay while recording | Main thread, per pointer event | A volatile store per event | "A finger was down during this motion." |
| **Inspector open** | `PanelVisibility` (new, `:core:devtools`) | — | — | Batches and frames while the panel (a `Dialog` sharing the Recomposer, F8) is open are excluded. Today they are counted for the screen beneath. |

The classification in `traceEventStart` (F3; the masks are the ones `updateChangedFlags` uses):

```kotlin
private const val LowBits = 0x12492492   // the low bit of every 3-bit parameter field
private const val HighBits = 0x24924924  // the high bit of every field
// A field reads Different (0b10) when its high bit is set and its low bit is not.
private fun differentFields(dirty: Int): Int = dirty and HighBits and ((dirty and LowBits) shl 1).inv()

val kind = when {
    dirty1 == -1 || dirty1 and 1 == 0 -> Execution   // called by its parent
    differentFields(dirty1) or (if (dirty2 == -1) 0 else differentFields(dirty2)) == 0 -> Restart
    else -> Root                                       // setContent, subcompose, reuse, movable content
}
```

A `Restart` or `Root` whose `info` starts with `androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactory`
is a **list row** (F3b): a list-row root is a row entering, being reused or prefetched; a list-row restart is the
library's list window update. The prefix test runs once per distinct `info` reference (string literals are
interned, `String.hashCode` is cached); later events probe the table. Everything else is an **app restart** or an
**app root**.

**Attribution by time, not by a frame callback.** Compose composes in two places within a frame: the Recomposer's
pass in the animation callback, and measure-time subcomposition inside the window's draw (F9), after every pre-draw
and on-draw callback the app can register. Both run inside one `Choreographer#doFrame` message. So a batch is
"everything composed from the first traced event until the end of the main-thread message it ran in", stamped with
that first event's time. `FrameIntake` (JankStats' thread) credits a batch to the frame whose UI window
`[frameStartNanos, frameStartNanos + frameDurationUiNanos]` contains the stamp, the window `ScreenTimeline` already
uses; several batches in one window merge. A batch outside every window ran **between drawn frames**: lazy prefetch
in idle time, a Recomposer pass that changed nothing visible (no frame drawn), measure done while handling input. It
is counted at its time (Idle accounting, the export's `betweenFrames`), never in a frame. A frame is finalised when
the next frame arrives or its motion ends, so a batch published after JankStats delivered its frame (the close runs
microseconds after `doFrame`, FrameMetrics milliseconds later, but the order is not guaranteed) is still credited.
If the ring overflows before `FrameIntake` reads it, the lost batches are counted, as FrameMetrics drops are.

#### 2.1.2 Motions and states

- **Motion:** consecutive frames whose starts are at most 12 refresh intervals apart (the same separation
  `parse_framestats.hitch_time` uses, `_MAX_HITCH_INTERVALS = 12`), and at least 4 frames long. Shorter bursts
  (1-3 frames: a tap highlight, a text change, a clock tick) are **updates**.
- **Middle frames:** frames 2 to N−1 of a motion. RECOMPOSITION-PLAN §3.2 invariant 1: "Frame 1 holds the state
  flip; frame N holds the settle." The gates allow the same ends (`recompose.tab_switch.max = 2`,
  `recompose.pop.max = 2`).
- **State**, in order of precedence:
  1. **Transition:** `ScreenTimeline` has an entry inside the motion, or within 100 ms before its first frame.
  2. **Scrolling or dragging:** the touch observer saw a finger down at any frame of the motion (the fling after
     the lift stays in the same motion).
  3. **Animating:** any other motion. The system back gesture lands here until it commits (gesture navigation does
     not deliver the edge swipe to the app as touches); a committed one changes the screen and is a Transition.
  4. **Idle:** updates and between-frames batches with no finger down.
- **Screen:** the screen at the motion's last frame (a transition counts for the screen it opens).

#### 2.1.3 What is counted (per screen × state, at most 33 screens × 4 states, primitive arrays)

Motions, frames, seconds of motion; app restarts, frames with an app restart, middle frames with an app restart;
middle frames with **app-root composition and no list row entering** (the per-frame composition rule below);
frames that composed anything (comparable to `perf.sh recompose`, F6); frames with list rows entering; list window
updates; per-frame motions; janky frames after each motion's first; motions whose first frame missed its deadline;
skipped vsyncs (2.3); between-frames batches (with and without app restarts, and whether a frame followed).

Shown per row: **app restarts per second** and **per frame**, **share of frames that restarted something**,
**share of middle frames that restarted something**, and the number of per-frame motions. Executions, roots, list
rows and between-frames composition are in the export, not the table.

#### 2.1.4 Verdicts against REQUIREMENTS 22

| Finding | Rule | Source | Colour |
|---|---|---|---|
| **Per-frame recomposition** | A motion with ≥ 4 middle frames, ≥ 50 % of which had an **app restart**; any state | REQ 22 "no per-frame recomposition during animations"; REQ 41 "no recomposition per scroll frame"; RECOMPOSITION-PLAN invariant 1. A field rule: the gates' `recompose.*` rows stay `perf.sh`'s | **Red**, with screen, state and the count of such motions |
| **Per-frame composition** | A motion with ≥ 4 middle frames, ≥ 50 % of which composed an **app root** (a `SubcomposeLayout` slot, `BoxWithConstraints`, a scaffold body) in a frame where no list row entered | Same sources; the JVM journeys' `assertQuietBetweenEnds` counts any execution | **Red** |
| **Recomposition mid-motion** | Middle frames with an app restart, below the per-frame rule | `RestartsMidMotion` limits below | Amber past grade A |
| **Periodic recomposition while idle** | App restarts in Idle that recur at a steady period: at least 3 occurrences, at least 1 s apart, consecutive intervals within 25 % of each other | `gates.toml` `idle.frames = 0` ("Idle 5 s on every tab, frames rendered"); RECOMPOSITION-PLAN's idle journey ("0 anywhere") | **Red** when those restarts draw frames (the release gate fails on them); **amber** when they draw nothing (CPU and battery, no frame) |
| **Composition during transitions** | Per Transition motion: frames that composed anything; the most in one transition and the median | Comparable to `recompose.tab_switch.max = 2`, `recompose.pop.max = 2` and the `recompose.push.max` ratchet, which `perf.sh` counts the same way (F6) | Report-only; the gates stay `perf.sh`'s |

**Not held against the app:** list rows entering, being reused or prefetched, and the library's list window
updates (F3b); app roots in a frame where a list row entered (a row containing a `BoxWithConstraints` composes it as
a root of its own when it enters); first and last frames of a motion. The back-gesture gate
(`recompose.back_gesture_progress.max = 0`) cannot be reproduced in the overlay without a hook into the back handler
(the overlay cannot tell a back drag from another animation); its per-frame rule is the 50 % field rule above, and
every mid-gesture restart still counts in `RestartsMidMotion`.

**Score metrics** (added to `OptimisationScore.Metric`; graded only with enough data, as `MIN_FRAMES` does today):

| Metric | Value | A / B / C / D (F above D) | Weight | Graded after |
|---|---|---|---|---|
| `RestartsMidMotion` | % of middle frames, all motions, with an app restart | ≤ 1 / 3 / 10 / 25 % | 10 | 300 middle frames |
| `RestartsWhileIdle` | **Field heuristic, not a gate:** bursts of app restarts per minute in Idle (data arriving, a flow re-emitting an equal state). Periodic ones are the finding above | ≤ 2 / 6 / 12 / 30 | 5 | 60 s in Idle |
| `JankyAfterFirst` (2.3) | % janky among motion frames other than each first, by the overlay's own janky flag (FrameMetrics overrun on API 31+) over its own motions | ≤ 0.5 / 1.0 / 2 / 4 % | 10 | 300 such frames |

`JankyAfterFirst` is **comparable to** the release rows (`*.janky_after_first_pct` 0.5, back gesture 1.0), not the
gate's number: the gate uses `parse_framestats.missed_deadline` (`max(FrameCompleted, GpuCompleted) > FrameDeadline`
from FrameTimeline) per scenario dump, with `perf.sh`'s scripted motions.

The two `Restarts*` metrics also form the **Recomposition score** (0-100 and a letter, weights 10:5) on its own card.
Their limits are a starting point and **must be calibrated in build step 9** before they ship: the template's own
journeys (catalog, the everything and standard presets' tabs, push/pop, back gesture, long flings past 30 items,
programmatic scroll to top) must grade A, and two scratch offenders must be caught and named: a scroll offset read
in composition on one screen (per-frame recomposition, red) and a `BoxWithConstraints` under an animated size
(per-frame composition, red, with no restart counted). A template journey that does not grade A is a template bug
routed to its owner, not a reason to loosen a limit without a written reason. Known in advance: AppScaffold passes a
fresh lambda to `subcompose(ScaffoldSlot.Content) { … }` on every measure (`AppScaffold.kt:88` on staging), so any
remeasure of a scaffold composes its body root again; if the bar minimize or a sheet changes bottom chrome on every
frame, the overlay will report per-frame composition, correctly, and it goes to WP03 as a template finding.

#### 2.1.5 Names: the 10-second Capture

A button in the Recomposition section. It arms a capture that starts when the panel closes (the panel's own
composition would otherwise be counted) and stops after 10 s or 1200 drawn frames, whichever is first; the pill shows
a small "REC" marker while it runs, read in the draw phase (no composition). While it runs, `traceEventStart` also
looks the composable up in an open-addressing table keyed by the compiler's group `key` and checked by the `info`
reference (a collision probes on): 2048 slots of `IntArray`s (executions, restarts, roots, batches ran in, batches
restarted in, peak per batch, last batch index) and the `info` references. Allocated once when the capture starts
(about 80 KB); a full table counts newcomers as "other". No allocation per call.

Result, sorted on the default dispatcher after the capture: the top composables by **batches they restarted in**,
then executions, with `Name · File.kt:line` split from `info` and shown **with line + 1** (F22), executions,
restarts, roots, "ran in K of the N frames drawn" (K counted per main-thread message that composed), peak per batch,
and a "list row" or "library" tag. Red when a composable restarted in ≥ 50 % of the frames drawn during the capture
and N ≥ 30. The overlay's own packages (`…core.perfhud`, `…core.devtools`, string constants the generator's package
rewrite covers) are filtered out; androidx and other libraries are grouped under "library" with a toggle.

**What it can see in a minified dev build:** every composable function and composable lambda compiled with trace
markers, by its source name and line (F2, F4): the template, its features and androidx; and for each, whether it
re-ran because its own state changed (restart), was called by its parent, or started a composition (root, F3).

**What it cannot see, or sees imprecisely:**
- **Which state** invalidated a scope. That needs `CompositionObserver.onScopeInvalidated` (2.1.7).
- **Inline composables** (`Column`, `Row`) and non-restartable or read-only ones: no trace of their own; they count
  as part of their caller.
- Code compiled without trace markers, or a build that strips them with its own R8 rule (detected, 2.1.6).
- A slot set again with the same content instance (F3a) is a restart of the **slot root**, not of the scope inside
  that changed: the count is right, the name is one level up.
- A parameter that compares unequal to itself (a `NaN` float) makes a restart read as a root.
- Content handed to a composition that is not a composable lambda (a hand-written `Function2`) reads as a restart
  each time it is set; the template's `subcompose` call sites (AppScaffold, KeptTabs) all pass composable lambdas.
- In a detached view, `subcompose` wraps the slot (F3a), so list rows there are not recognised as list rows; the
  overlay only records attached windows.
- Composition in another window drawn in the same `doFrame` (a dialog, a popup) is credited to the activity
  window's frame; composition in other processes is not seen.

**Cost:** the hash probe on every executed composable while it runs, estimated ≤ 0.3 ms on a frame that composes a
whole screen (about 2000 executions), nothing on frames that compose nothing. Declared in the panel, flagged in the
export ("recorded during a capture"), measured in A/B arm 4.

#### 2.1.6 Coexistence with other tracers (decision taken in this revision)

Compose has one tracer slot and it cannot be read (F1). Users today: runtime-tracing in every dev build, installed
at process start by App Startup (F19); `RecompositionCounter` in JVM tests; the overlay. Layout Inspector's counts use
their own bytecode hooks and its state-read view uses `setObserver`; neither touches the slot.

**Decision: forward and restore.** `CompositionWatch` installs its own tracer only while recording, and that tracer
forwards every event (`isTraceInProgress`, `traceEventStart`, `traceEventEnd`) to a **delegate**:
- **Dev builds (runtime-tracing present):** the delegate is `PerfettoForward`, the same three calls runtime-tracing's
  tracer makes (F19: `isEnabled`, `beginSection(info)`, `endSection`), so a Perfetto trace taken while the overlay
  records keeps every composable name. On stop, `RuntimeTracing.restore(context)` calls
  `ComposeTracingInitializer().create(context)`, which installs runtime-tracing's own tracer again: after recording
  the slot holds exactly what App Startup put there.
- **Debug builds of other flavours (no runtime-tracing):** no delegate; on stop the slot is set to null, as before.
- **Detection:** `Class.forName("androidx.compose.runtime.tracing.ComposeTracingInitializer", false, loader)` at
  recording start; startup's consumer rules keep that name in minified builds (F19). `:core:perfhud` compiles against
  runtime-tracing and tracing-perfetto with `compileOnly`, and only the small `RuntimeTracing` object touches them,
  loaded after detection succeeds, so a debug build without them never loads it.
- **Why not the alternatives:** installing without forwarding drops Perfetto's names exactly when a developer who
  sees a red finding is likely to take a trace; taking runtime-tracing out of `devRelease` undoes WP02's choice, and
  `devRelease` is where the overlay sends people for real numbers. Costs of the decision: `PerfettoForward` copies
  runtime-tracing's behaviour, so a version-pinned test (2.5) fails when runtime-tracing changes version, and whoever
  bumps it re-reads `ComposeTracingInitializer`.

**Why `setTracer` although it is the more internal API (F7).** It is the only hook that sees each composable that
executes, with its name and file:line, in a minified build, at the cost of a counter increment. `CompositionObserver`
reports scopes without names once `sourceInformation` is stripped (F4) and calls back on every state read. The shape
of `traceEventStart(key, dirty1, dirty2, info)` is compiled into every composable of every published library (the
runtime still carries the older two-argument overload for code built by old compilers), so it is effectively frozen;
`setTracer` itself and the meaning of the `dirty` bits are not. `CompositionWatchTest` therefore pins the runtime
version it was checked against and every root and restart path (2.5): a Compose bump that changes
`invokeComposable`'s force bit, the `ComposableLambdaImpl` bits or the lazy row's name fails the build's tests.

**Tracer replaced by someone else.** If another tool calls `setTracer` while the overlay records, the overlay cannot
see it directly. It notices when the change count moves (F5) while its tracer saw nothing for 60 frames that applied
changes: the Recomposition section then says "tracer replaced by another tool", recomposition metrics go ungraded,
the export records `recomposition.tracer = "lost"`, and the overlay does not fight for the slot.

**JVM tests.** `CompositionWatch` takes the delegate as a constructor parameter (production passes `PerfettoForward`
or null) and the restore action as another, so a test can make the overlay forward to `RecompositionCounter`'s
tracer and restore it afterwards (2.4.2, 2.5). Tests that compose the app through Hilt in a debug variant launch with
recording off.

#### 2.1.7 Considered for recomposition, and not used now

| Option | Why not (now) |
|---|---|
| `CompositionObserver` via `RecomposerInfo.observe` | Experimental (F7). No names in a minified build: `sourceInformation` is stripped (F4). One observer per composition, the slot Studio's state-read view uses. **Later, debug builds only:** a "why" column (the state class and, for a `State` holding a `Float`/`Offset`/`Dp`, "an animated value read in composition"). |
| A frame boundary at pre-draw, on-draw or frame commit | Pre-draw and on-draw run before Compose's draw-time composition (F9); the frame-commit callback comes after the RenderThread and can arrive after the next frame has started. |
| Excluding known root keys (1524156494, -59194059) | On an attached view the root is the app's own slot content (F3a), whose key is not known; keys also change with every library release. The bit test needs neither. |
| Change count as the primary signal | Blind to re-executions that change nothing and to measure-time composition; it would grade a classic offender green. Kept as the replaced-tracer detector. |
| `RecompositionTracer` / `RecompositionTracingReceiver` | Not in 1.12.1 (1.13 alphas only); its KDoc warns of significant overhead. |
| Layout Inspector counts | Not readable from the app. |

### 2.2 "Not optimised" flags

One list at the top of the Perf tab, also in the JSON (`findings`) and Markdown export and one `PerfHud` log line per
new finding. Each entry: severity, metric, value, the limit and where it comes from, the hint, the doc anchor in
`docs/PERFORMANCE-OVERLAY.md`. Red before amber, then by points lost. Context flags never cost points but mark the
recording.

| Metric | Amber / red | Limit from | Hint the overlay shows (shortened) |
|---|---|---|---|
| Janky frames (existing) | past A (0.5 %) / past C (2 %) | `gates.toml` scroll rows | "Open Frame times: red bars missed their deadline. The cause split below says whether the main thread or the GPU lost the time." |
| Janky after each motion's first (new) | past 0.5 % / past 2 % | Comparable to REQ 42's release rows (`*.janky_after_first_pct` 0.5, back gesture 1.0); the gate's own number is `perf.sh`'s | "A motion's first frame may miss on a cold GPU; the ones after must not. Look at what runs on every frame of the motion: recomposition, allocation, a layer re-rendered each frame." |
| Main-thread p90/p99 (existing) | past A / past C | `gates.toml` UI-thread frame times | as today |
| Slow / frozen frames, long main-thread work (existing) | as today | Android vitals | as today |
| Per-frame recomposition (new) | red | 2.1.4 | "On {screen}, something rebuilds on every frame while it {scrolls/animates}. Read the moving value where it is used: in `offset {}`, `layout {}`, `graphicsLayer {}` or a draw block, or pass `() -> T` (R1, R2); for a threshold use `derivedStateOf`. Run Capture to see which composable." |
| Per-frame composition (new) | red | 2.1.4 | "Content is composed from scratch on every frame of the motion: a `BoxWithConstraints` or `SubcomposeLayout` re-measured under an animated size, or a scaffold whose bars change size each frame. Size with `layout {}` or a fixed slot; keep the entering screen's first state final (R10). Capture names the root." |
| `RestartsMidMotion` (new) | past A / past C | 2.1.4 | as per-frame recomposition, softer |
| Periodic recomposition while idle (new) | amber if it draws nothing / red if it draws frames | `gates.toml` `idle.frames = 0`; RECOMPOSITION-PLAN idle journey | "Something rebuilds every {n} s with nobody touching the app{, and draws a frame each time}: a ticker, a timer, or a flow emitting new but equal state. Collect with `collectAsStateWithLifecycle`, keep `UiState` equality honest, or draw a ticking value instead of composing it." |
| `RestartsWhileIdle` (new, heuristic) | past A / past D | Field heuristic, calibrated in step 9; not a gate | "The app rebuilt {n} times a minute with nobody touching it. Some is data arriving; equal states emitted again are not. Check `distinctUntilChanged` and `UiState` equality." |
| Jank cause (new, context) | majority main thread, or majority after it | F10 | Main thread: "composition, layout or work in the frame; see Recomposition and StrictMode." After it: "GPU or render thread: overdraw, blur, offscreen layers (a translucent full-screen layer, A11.3), large bitmaps; try hardware bitmaps and the glass tier below." |
| App memory, blocking GC (existing) | as today | `gates.toml` `meminfo.pss_app_mb`; existing limits | as today |
| Process allocation per frame drawn (new) | past the calibrated limit | Template baseline measured in step 9 (unscored until then) | "The app process allocates about {n} KB per frame drawn (every thread, the overlay's own 1 Hz sampling included). If it rises while a screen animates, look for allocation in draw, layout or composition (R18): remember `Path`s and `Paint`s, use `mutableFloatStateOf`, no string formatting per frame." |
| Blocking GC while drawing (new) | amber: a blocking collection in a second with ≥ 30 frames drawn | F13 (`art.gc.blocking-gc-count`; concurrent collections, which do not pause the main thread, are reported only) | same hint |
| Cold / warm start (existing) | as today | `gates.toml` cold start 380 ms | as today |
| Full display not reported (new, context) | amber when the latest cold start has no `FULLY_DRAWN` (Android 15+) | Android vitals / Macrobenchmark TTFD | "Start-up can be judged only to the first frame. Add `ReportDrawnWhen { … }` on the start destination when its content has loaded." (The template does not call it today; routed to its owner, 2.7.) |
| Previous exit (new) | red for `REASON_ANR` with a timestamp after this APK's install or last update (`PackageInfo.lastUpdateTime`); an older ANR and a low-memory kill are reported with their time only | F15 | "The app hit an ANR on {date, time} with this build: the main thread was blocked for seconds. StrictMode and the long-work count show where." |
| StrictMode, R8, baseline profile, not debuggable (existing) | as today | as today | as today |
| Thermal (new, context) | status `MODERATE` and up (or, on API 35+, headroom past the device's own `MODERATE` threshold) / status `SEVERE` and up, or headroom ≥ 1.0 (`SEVERE` by definition) | F12 | "The phone is throttling: numbers recorded now understate the app. Let it cool before judging." The score card says "recorded while throttled". |
| Overlay context (new) | — | — | Marks numbers taken during a capture, with the panel open, or with the tracer lost. |

### 2.3 Other signals worth adding

#### 2.3.1 Added (cheap enough)

| Signal | How it is read | How often, which thread | Verdict |
|---|---|---|---|
| **Janky after each motion's first** and **late first frames** | The overlay's janky flag per frame (already recorded) and the motion segmentation | Per frame, JankStats' thread, O(1) | Scored (2.1.4), comparable to the release rows; late first frames reported, as `gates.toml` does |
| **Where late frames lost their time** | Per janky frame: main thread = UI time over the interval; else render thread = `cpu − ui`, GPU (minus swap) = `total − cpu` (F10). API 31+ | Per janky frame, JankStats' thread, a few subtractions | Context: majority cause, with its hint |
| **Skipped vsyncs during motion** (app side) | Sum of `start gap − interval` for gaps over 1.5 intervals inside motions, per second of motion | When a summary is taken, from the ring | Report-only; not the gate's hitch ratio (display present times an app cannot read) |
| **Process allocation per frame drawn** | `art.gc.bytes-allocated` (F13) with the 1 Hz sampler, divided by frames drawn in that second; labelled process-wide | 1 Hz, sampler (off the main thread); one more counter read | Flag after calibration (2.2) |
| **Blocking GC while drawing** | `art.gc.blocking-gc-count` delta (already read by `MemorySampler`) in seconds with ≥ 30 frames; `art.gc.gc-count` reported only | 1 Hz, sampler | Amber (2.2) |
| **Thermal status and headroom** | `addThermalStatusListener(executor, …)` while recording (API 29+); `getThermalHeadroom(0)` every 10 s (API 30+); NaN reads "not reported" (some devices always report NONE) | Push on change; 10 s on the sampler | Context (2.2) |
| **Previous exit reason** | `getHistoricalProcessExitReasons(pkg, 0, 1)` and its `getTimestamp()` (API 30+), against `PackageInfo.lastUpdateTime` | Once per recording start, I/O dispatcher | ANR with this build red; older ANR and low-memory kill reported with their time |
| **Full display reported** | `START_TIMESTAMP_FULLY_DRAWN` from the existing `ApplicationStartInfo` read | With the existing start-up read | Context flag |
| **Inspector frames excluded** | `PanelVisibility` | — | Accuracy fix: frames and batches while the panel is open no longer count for the screen beneath |

#### 2.3.2 Considered and rejected

| Signal | Why not |
|---|---|
| `Looper.setMessageLogging` message timings | Builds a string per message ("performance penalty", javadoc) and is itself a StrictMode slow call the overlay's own watch counts (F14). A frame delayed by a blocked main thread already shows its delay in the UI time. |
| A second `OnFrameMetricsAvailableListener` (exact `GPU_DURATION`, `UNKNOWN_DELAY`) | A second per-frame callback for numbers F10 already approximates. |
| A main-thread watchdog (post every 500 ms, time the delay) | Wakes an idle main thread twice a second; frames already include the delay; StrictMode and the exit reason cover the rest. |
| `ActivityManager.getProcessMemoryInfo`, more PSS | Rate-limited, debugging-only per its javadoc, and a PSS walk costs a frame (`perf/HUD/pss-probe/`). PSS stays at most every 15 s in a pause. |
| Thermal headroom listener, headroom thresholds | API 36 and 35, behind flags; the 10 s read is enough. |
| Frame-rate requests and ARR (`getRequestedFrameRate`, `hasArrSupport`) | Read-back of what the app asked, not what it got; the overlay already grades each frame against its own interval. |
| Compose compiler stability reports in the overlay | Build-time data; `checkComposeStability` already fails the build for the design system and `:core:ui`. Later, if wanted. |
| A back-gesture hook for `recompose.back_gesture_progress` | Needs an API from `:core:navigation` to the overlay for one number `perf.sh` already gates. Later, if the field rule proves too coarse. |

### 2.4 The cost budget

#### 2.4.1 What the Oct 19 regression says about the overlay

- `bugs/BUG-2026-10-19-back-see-through.md` (Result) measured the combined tree's back gesture at 12.7 ms/s of
  hitch against 6.9 for T + C1, and 1.2-1.5 % janky after the first frame against 0.3-0.4 %, A/B controlled. Its
  first suspect, "the HUD's frame listener and its DEV badge in the measured `devBenchmarkRelease`", was not
  verified, and `staging/STAGING-PLAN.md` §0 ranks a stale baseline profile first.
- **The overlay was almost certainly not recording in that run** (F17): `perf.py` at `e404e391` cleared the app's
  data before each run and passed no `perf.hud` extra, and a release-like build with no stored choice starts with
  recording off. In the everything preset the analytics recorder's JankStats is always on, in prod as well; it is not
  the overlay's.
- **What the measured build did carry** is the dev tooling at rest: the recorder built at launch (within noise for
  cold start in run 4), the inspector's network observer, the screen reports, and WP06's `EdgeBadge` composed over
  every screen, translucent at rest (alpha 0.85 with Auto compositing, so drawn through an offscreen buffer, F11)
  and excluding its edge from system gestures. The control tree also had a badge, but an opaque one without gesture
  exclusion (F18).
- This design does not claim a cause. It removes all dev tooling from measured builds, which makes the question moot
  for the gates, and A/B arm 1 measures how much of the regression it was. Staging's fix loop owns the regression.

#### 2.4.2 The budget

| Rule | Limit | Enforced by |
|---|---|---|
| **Absent from every shipped build** (unchanged) | No class, resource, manifest entry, thread | `verify<Variant>CarriesNoDevTooling`, `apk_inspect.py`, the prod thread check (run 2) |
| **Absent from every build the perf harness measures** (new) | `devBenchmarkRelease` and `devNonMinifiedRelease` carry no `:core:devtools` or `:core:perfhud` | **Wiring:** `debugImplementation(project(...))` and `"devReleaseRuntimeOnly"(project(...))` for both modules, replacing `"devImplementation"`. Not `"devReleaseImplementation"`: the baseline-profile plugin copies `devRelease`'s `implementation`, `api`, `compileOnly`, `kapt`, `annotationProcessor` and `compile` dependencies into both measured variants, but not `runtimeOnly` (F20). Run-time-only works because `:app` compiles against nothing in those modules, and Hilt still aggregates their bindings from the runtime configuration (F21). Declare the configuration explicitly if AGP 9.4 does not create flavour-and-build-type configurations itself. **Guard:** `verify<Variant>CarriesNoDevTooling` is registered for every variant except the debug build type and `devRelease` (today it skips the whole dev flavour), so `devBenchmarkRelease` and `devNonMinifiedRelease` are guarded; it resolves the variant's runtime classpath, after the plugin's copy, which is what the APK gets. **Harness:** `perf.py` refuses `--gate` when the APK's dex holds the tooling markers (the badge's test tag `devtools_badge` and the launch extra `perf.hud`, string constants that only those modules hold and use at run time), unless `--allow-dev-tooling`, which never gates; a test keeps the two constants and `perf.py`'s copy in step (2.5). The build guard is the primary enforcement. **Profile:** the baseline profile is re-recorded on the new `devNonMinifiedRelease` (it then holds no tooling classes). |
| **Zero work while hidden** | No tracer of the overlay's (the slot holds runtime-tracing's tracer or null, as App Startup left it), no pending batch-close message, no pointer observer, frame listener, thermal listener, timer or thread of the overlay's; idle draws 0 frames | Unit tests (listener lists by reflection as `RedrawDuringFramesTest` does; the tracer slot read by reflection in a JVM test; thermal listener removed); phone: thread list and idle frames hidden vs a build without tooling (arm 2) |
| **Sampling, not per-frame work, where possible** | Memory, GC, allocation, thermal, exit reason: sampled (1 Hz, 10 s, once). Per frame only what must be: the intake (JankStats' thread). Per main-thread message that composed: one clock read and one pooled message | Code review against this table |
| **No allocation per frame** | 0 bytes per frame in `FrameIntake.onFrame`, the tracer callbacks and batch close (outside a capture and, after warm-up, inside it), the motion tracker | JVM tests with `com.sun.management.ThreadMXBean.getThreadAllocatedBytes` over 10,000 calls after warm-up (JBR is HotSpot); the batch close is tested through its `Poster` seam (production posts a pooled `Message`, which allocates only if the pool is empty) |
| **No recomposition of app content** | App composables execute the same with recording on and off; the overlay's composables execute 0 times while the app animates (the pill, its REC marker and the finding dot read their values in the draw phase), so the overlay never appears in its own counts | `OverlayRecomposesNothingTest`: `CompositionWatch` forwards to `RecompositionCounter`'s tracer through its delegate, so both see the same events while recording; the counter fails if it counted nothing (2.5) |
| **No frame of its own** (unchanged) | Redraws only inside app frames | `RedrawDuringFramesTest`; idle 0 frames shown (phone) |
| **Shown: total overhead** | REQ 40 hard limits: +0.3 points of jank and under 0.5 ms main-thread time per frame; **target ≤ 0.1 ms** (run 6 measured +0.020 to +0.055 ms before these additions) | A/B arm 3 on the Redmi |
| **Capture: declared** | Ends by itself in 10 s; ≤ 0.3 ms extra on a frame that composes a screen (estimate, to be measured); numbers flagged | A/B arm 4; `CompositionCaptureTest` |
| **Dev badge at rest** | No offscreen buffer per frame unless the A/B shows it costs nothing | Arm 1 and 2 traces; if it costs, `CompositingStrategy.ModulateAlpha` or opaque at rest (question 3) |

#### 2.4.3 Proving it on the Redmi

All builds from one commit of `staging-wp/HUD`, the HUD runs' phone specs (`out/HUD/specs/hud-everything.json`:
analytics on, shared JankStats; `hud-standard.json`: the overlay's own JankStats), installed fresh and compiled
`speed-profile`. Runs alternate A-B-A-B in one session, so phone state cancels; the thermal status
(`dumpsys thermalservice`, read-only) is logged at each block start and a block starting above NONE is repeated.
A Sonnet operator captures under `bin/locked adb`; raw files to `perf/HUD-metrics/ab-<arm>/`; an Opus verifier
re-derives every row from the raw `gfxinfo framestats` with `parse_framestats.py`; a separate Opus reviewer checks
the claims.

| Arm | A vs B | How | Pass |
|---|---|---|---|
| 1. Dev tooling at rest in the measured build | `devBenchmarkRelease` (new wiring, no tooling) vs the same with the old `"devImplementation"` lines (scratch build, never committed) | `perf.sh --runs 3`, 4 blocks, `tabs pushpop pushpop_gesture scroll_root idle recompose` and `coldstart`; one Perfetto trace per arm (`gfx view sched`, FrameTimeline, GPU render stages if exposed) | No limit: it measures the Oct 19 suspect. Report hitch ms/s, janky after first, the gesture's first three frames, and whether the badge's node draws through an offscreen pass each frame |
| 2. Overlay hidden costs nothing | `devRelease`, `perf.hud=off` vs `devRelease` built without tooling (scratch) | `out/HUD/overhead.py`, 6 alternating rounds (idle 5 s, 12 flings, 5 tabs, 4 push/pop) | Δ janky within ±0.3 points, UI-thread mean Δ ≤ 0.05 ms, idle 0 frames both, the same thread names (`ps -T`), no `PerfHud` line |
| 3. Overlay shown | `devRelease` `perf.hud=on` vs `off` | as arm 2, plus `perf.sh --allow-dev-tooling --extra perf.hud=on recompose` with the overlay's export taken over the same run, plus one 10 s Perfetto trace while recording | REQ 40 limits; target ≤ 0.1 ms; idle 0 frames shown. **The tracer was installed:** the export's `recomposition.tracer` is `"installed"` (not `"lost"`) and the `PerfHud` start line says so; the Perfetto trace still holds composable-name sections in category `androidx.compose` (forwarding works). **Attribution:** the overlay's frames that composed, summed over the scenario's transitions, are at or below `perf.sh`'s recomposing frames and differ only by doFrames that drew nothing or composed nothing (F6); any other difference is a bug |
| 4. Capture | shown + capture vs shown | 10 s captures during the scroll and push/pop steps | Reported; the capture stops by itself; no tracer of the overlay's installed afterwards (unit test) |
| Calibration (step 9) | the template's journeys, and two scratch offenders | `devRelease` shown | 2.1.4: template A; the scroll-offset offender red as per-frame recomposition and named; the `BoxWithConstraints` offender red as per-frame composition, with no restart |

### 2.5 Where it goes, tests, and order

Owner of every item: **`staging-wp/HUD`**. Reviewers: WP04 for `tools/perf`, the devtools owner (WP06) for the
`:core:devtools` API, WP12 for the docs, WP02 for the build wiring and `runtime-tracing` coexistence, the owners of
`:core:testing`, `:core:designsystem` and `:core:navigation` for the `RecompositionCounter` change. Paths under
`templates/android/`; `P` = `core/perfhud/src/main/kotlin/com/base/app/core/perfhud`.

| Piece | Files |
|---|---|
| Measured builds without tooling | `app/build.gradle.kts` (`debugImplementation` + `"devReleaseRuntimeOnly"` for both modules); `build-logic/convention/src/main/kotlin/com/base/app/buildlogic/guard/DevToolingGuard.kt` (the variant filter as a pure function: guard every variant but debug and `devRelease`), `…/src/test/kotlin/…/guard/DevToolingGuardTest.kt`; `tools/perf/perf.py` (`--allow-dev-tooling`, the marker check), `test_perf.py`; generator `tests/test_android_target.py` (the wiring lines); `docs/ARCHITECTURE.md` (guard row); `docs/PERFORMANCE-OVERLAY.md`; the re-recorded `app/src/main/baseline-prof.txt` |
| Inspector hooks | `core/devtools/.../page/DevToolsPage.kt` (`windowContent: (@Composable () -> Unit)?`, composed at the overlay's root, outside the keyboard early return); `.../panel/PanelVisibility.kt` (new `@Singleton`: `open: StateFlow<Boolean>`, `requestClose()`); `.../badge/DevToolsOverlay.kt` (publishes the panel state, composes `windowContent`); `features/devtools.toml` owns both |
| Composition tracking | `P/recomposition/CompositionWatch.kt` (the tracer, the kind and list-row classification, main-thread counters, delegate, install and restore, capture hook), `BatchClose.kt` (first-event stamp, the front-of-queue asynchronous message behind a `Poster` seam, the 128-entry ring), `RuntimeTracing.kt` (detection, `PerfettoForward`, `restore`), `Recomposers.kt` (running recomposers to an array; the replaced-tracer check), `CompositionCapture.kt` (table, 10 s stop, result, line + 1), `RecompositionSummary.kt`; `core/perfhud/build.gradle.kts` (`compileOnly` and `testImplementation` of runtime-tracing; `compileOnly` of tracing-perfetto); `gradle/libs.versions.toml` (an `androidx-tracing-perfetto` entry at 1.0.1, the version runtime-tracing 1.12.1 depends on) |
| Test support | `core/testing/compose/.../recomposition/RecompositionCounter.kt` (exposes `tracer`; fails a test that counted no execution at all); the private copies in `:core:designsystem` and `:core:navigation` tests get the same guard or move to the shared one, their owners' call |
| Motions | `P/frame/MotionTracker.kt` (segmentation, state, per screen × state arrays, the four rules of 2.1.4, periodic idle detection, janky after first, late first frames, skipped vsyncs, cause split); `FrameIntake.kt` (batch matching by stamp, frame finalised on the next frame, touch, cause); `FrameLog.kt` (holds the tracker, `summary()`); `ScreenTimeline.kt` (whether a screen was entered within a span, allocation-free); `FrameReadings.kt` |
| Touch | `P/frame/TouchState.kt`; `P/overlay/TouchObserver.kt` (`Modifier.Node`, shares with siblings, never consumes) |
| Extra signals | `P/device/ThermalWatch.kt`; `P/memory/MemorySampler.kt`, `MemoryLog.kt` (process allocation per frame drawn, blocking GC while drawing); `P/startup/StartupTimes.kt` (fully drawn present); `P/startup/ExitReason.kt` (timestamp, install time) |
| Score, findings | `P/score/OptimisationScore.kt` (three metrics, recomposition sub-score), `P/score/Findings.kt` (rules, severity, hint resources, doc anchors) |
| UI | `P/panel/RecompositionSection.kt`, `FindingsSection.kt`, `PerformanceTab.kt`; `P/overlay/PerformancePill.kt` (only as the user chooses, 2.6; draw-phase reads only); `res/values/donottranslate.xml` |
| Export, log | `P/report/PerformanceReport.kt` (schema 3: `motions`, `idle`, `betweenFrames`, `capture`, `findings`, `thermal`, `allocation`, `jankCauses`, `recomposition.tracer`), `MarkdownReport.kt`, `P/log/PerfLogcat.kt` (`restartMid=`, `perFrameMotions=`, `perFrameComposition=`, `idleRestarts=`, `jankyAfterFirst=`, `tracer=`) |
| Recorder | `P/recording/PerformanceRecorder.kt` (start and stop the new pieces with recording; pause on `PanelVisibility`) |
| Docs (REQ 50, same change) | `docs/PERFORMANCE-OVERLAY.md` (Recomposition: restart vs root vs list row, the findings and their limits, costs, what a minified build can name, the tracer's single slot and the forwarding); `AGENTS.md` short rule: "a red Recomposition finding is fixed before UI work is done; run Capture to find it" |
| Generator | `features/perfhud.toml` (owns the new files), `web/frontend/lib/catalogue.json` (regenerated) |

Tests (JVM and Robolectric unless marked):

- `CompositionWatchTest` (version-pinned: asserts the Compose runtime and runtime-tracing versions it was checked
  against, read from their `META-INF/*.version` resources, so a bump fails with "re-check F3, F3a, F3b and F19"):
  - A root never counts as a restart: the first `setContent` root; a re-`setContent` with a new lambda; a new
    subcomposed slot on an attached view (raw path); a reused slot (`setContentWithReuse`); a detached view's
    wrapper (key 1524156494); movable content (key -59194059).
  - Restarts: a state written every frame and read in composition gives one app restart per frame (function and
    lambda scopes, lambdas with 0 and 1 parameters); a slot set again with the same content after
    `forceRecomposeChildren` reads as one restart of the slot root.
  - Executions: a parent passing a new argument executes the child with no restart; `dirty1 = -1` is an execution.
  - **A LazyColumn flung so that a row enters on every frame, across more than 30 items:** 0 app restarts; every
    entering row is a list-row root; list-window restarts ≤ ⌈items / 30⌉.
  - **`BoxWithConstraints` under an animated size:** an app root every frame and 0 app restarts, so only the
    per-frame composition rule trips.
  - **A scroll offset read in composition:** an app restart every frame.
  - Calls off the main thread are not counted (and still forwarded).
  - **Coexistence:** without runtime-tracing, the slot is null after stop. With runtime-tracing on the test
    classpath: while recording the slot (read by reflection on `ComposerKt`'s private field, test-only, unminified)
    holds the overlay's tracer and every event reaches the delegate; after stop it holds the tracer
    `ComposeTracingInitializer` creates. Never skipped.
- `BatchCloseTest` (Robolectric, paused looper): composition inside one main-looper message is one batch stamped at
  its first event; **a `SubcomposeLayout` remeasured through `invalidate()` and drawn in the same message as a
  Recomposer pass is in that batch and is credited to the frame whose window covers the stamp**, and the next
  message's composition is a separate batch; a message that composes nothing posts nothing; the close message is
  asynchronous and at the front of the queue.
- `FrameIntakeTest` (extended): batches matched by stamp, several merged into one frame, batches outside every window
  counted as between frames, a batch published after its frame was delivered still credited, ring overflow counted.
- `MotionTrackerTest`: the 12-interval split, the 4-frame minimum, first and last frames excluded, the state
  precedence, the four rules of 2.1.4 (app roots in a frame with a list row not counted; periodic idle red only when
  it draws), janky after first, late first frames, skipped vsyncs, idle updates.
- `CompositionCaptureTest`: names and **line + 1, checked against the line of a probe composable found by reading
  the test's own source file**; restarts vs roots vs executions; "ran in K of N"; a full table counts "other"; the
  10 s and 1200-frame stops; the overlay's own packages filtered.
- `PerFrameAllocationTest`: zero bytes per call for the intake, the tracer and batch close (through the `Poster`
  seam; capture after warm-up), the tracker.
- `OverlayRecomposesNothingTest`: with `RecompositionCounter` applied, an animated screen and the pill composed, and
  `CompositionWatch` recording with the counter's tracer as its delegate (and restoring it on stop): the app's
  per-name execution counts equal a run with recording off; the overlay's composables execute 0 times between the
  ends; the counter's total equals the watch's own execution count (both saw the same events), and is above zero.
- `RecompositionCounterTest`: a test whose composition was never seen (tracer replaced, nothing composed) fails.
- `TouchObserverTest`, `ThermalWatchTest` (Robolectric's `ShadowPowerManager` if it drives thermal status, otherwise
  a small `PowerManager` seam), `ExitReasonTest` (an ANR before the install time is not red), `PanelVisibilityTest`,
  `OptimisationScoreTest` (the new metrics, ungraded without enough data or with the tracer lost), `FindingsTest`
  (each rule at its limit, each hint resource present), `ReportFormatsTest` (schema 3).
- build-logic `DevToolingGuardTest`: the variant filter guards `devBenchmarkRelease`, `devNonMinifiedRelease` and
  every release-like variant of the other flavours, and not `devRelease` or any debug variant (a pure function, as
  the existing test stays a graph walk without AGP). The real plugin is exercised where AGP runs: CI's `check` on
  the generated presets runs the guard on both measured variants with the baseline-profile plugin applied, and the
  acceptance script lists the guard tasks and runs `:app:verifyDevBenchmarkReleaseCarriesNoDevTooling`.
- `test_perf.py`: `--gate` refused for an APK whose dex holds a marker, accepted for one without, `--allow-dev-tooling`
  never gates; **the marker pin:** reads the template's `DevToolsOverlay.kt` and `PerformanceRecorder.kt` and
  asserts the `devtools_badge` and `perf.hud` literals `perf.py` looks for are still declared there. Generator: the
  wiring lines.
- Mutation check, as runs 4-6 did: put back per-frame allocation, per-frame recording on the pill, the tracer left
  installed after stop, the old `"devImplementation"` wiring, **`"devReleaseImplementation"` wiring (must fail the
  guard on both measured variants, F20)**, the force-bit-only restart test (must fail the LazyColumn and
  `BoxWithConstraints` cases), and a pre-draw frame close (must fail `BatchCloseTest`); each must fail its test.
- Phone: section 2.4.3; `apk_inspect.py` on `devBenchmarkRelease` (zero tooling classes, resources, strings) and on
  `devRelease` (tooling present, the DEV badge shows: F21); the jetpack-compose-performance audit over every new
  composable and node.

**Order** (each step ends green and reviewed before the next):

0. Staging done, the user's go, `staging-wp/HUD` cut from staging (REQUIREMENTS 44).
1. **Measured builds without tooling** (run-time-only wiring, guard, `perf.py` refusal and marker pin, tests, docs,
   re-recorded baseline profile) and **A/B arm 1**. First, because every later measurement depends on it and it
   feeds staging's regression hunt; if staging's fix loop has already changed the wiring, take theirs and keep only
   what is missing.
2. Inspector hooks (`PanelVisibility`, `windowContent`) and the exclusion of inspector frames.
3. Composition tracking (classification, batch close, coexistence with runtime-tracing, the `RecompositionCounter`
   change), motions, janky after first, cause split, skipped vsyncs; the allocation and attribution tests.
4. Score metrics, findings, Recomposition and Findings sections, log and export (schema 3), docs.
5. Capture.
6. Touch observer (until then "Scrolling or dragging" folds into "Animating").
7. Thermal, process allocation and blocking GC while drawing, full-display flag, exit reason.
8. The pill, as the user chose.
9. Phone: calibration, A/B arms 2-4, demo capture (`perf/HUD-metrics/SHOW.md`), Opus verifier, Opus review, fix loop.

Phase B (Compose Multiplatform desktop and web) can reuse `CompositionWatch`'s classification and the motion logic
(common Kotlin); the batch close (a Looper message), the touch observer and runtime-tracing coexistence are
Android-only, and desktop closes a batch at the end of its frame callback instead.

### 2.6 Questions only the user can answer

1. **What the pill shows.** (a, recommended) Keep FPS and janky %, and add one coloured dot for the worst open
   finding (no text, no wider pill; its colour read in the draw phase); tapping opens the findings. Pros: no new
   line, red per-frame recomposition is visible at a glance. Cons: a dot needs the tab to explain it. (b) A third
   line, the mid-motion restart share ("R 0.4%"). Pros: a number at a glance. Cons: a taller pill over the app; one
   more number to read. (c) No change; findings only in the tab. Pros: nothing new on screen. Cons: problems stay
   unseen until someone opens the tab.
2. **Recomposition in the 0-100 score.** (a, recommended) Yes: `RestartsMidMotion` (10) and `RestartsWhileIdle` (5)
   join the total, and the Recomposition score also shows on its own card. Pros: REQ 22 is a release requirement
   and the score should say so. Catch: every existing score moves a little; graded only with enough motions.
   (b) A separate Recomposition score only. Pros: the existing score is unchanged. Cons: an app can read A while
   rebuilding on every frame.
3. **Only if A/B arm 1 or 2 shows the badge costs frames:** make the badge opaque at rest (a visible change to
   WP06's look; recommended), or draw its alpha per operation (`ModulateAlpha`: the same look within a few percent,
   the label slightly tinted by its own background).

**For your information, no answer needed unless you object:** REQUIREMENTS 49, as recorded from your Oct 20 request,
says the tooling is absent from every build the perf harness measures. So `devBenchmarkRelease` and
`devNonMinifiedRelease` lose the DEV badge, the inspector and the overlay; perf recordings show no badge; QA wanting
the badge in a release-like build uses `devRelease`; REQUIREMENTS 40's "dev-flavour builds" for the badge now means
`devDebug` and `devRelease`.

### 2.7 Notes for others

- **Staging's fix loop:** F17 and F18 correct the bug file's suspect list: the overlay was off in the bug run, and
  the control tree had a badge too, but an opaque one. Arm 1 is the discriminator for the dev tooling; it does not
  replace the baseline-profile check ranked first in `STAGING-PLAN.md`.
- **WP02 (build, runtime-tracing):** the tooling's wiring becomes `"devReleaseRuntimeOnly"` because the
  baseline-profile plugin copies `devRelease`'s other dependency kinds into the measured builds (F20); any future
  dev-only dependency that must stay out of measured builds needs the same. runtime-tracing stays as WP02 wired it;
  the overlay takes Compose's tracer slot only while recording, forwards to Perfetto, and puts runtime-tracing's
  tracer back (2.1.6). Any other tool that calls `Composer.setTracer` must do likewise.
- **WP03 (design system):** AppScaffold's content slot gets a new lambda on every measure (`AppScaffold.kt:88`), so
  any per-frame remeasure of a scaffold is per-frame composition; expect the overlay to say so if the bar minimize
  or a sheet changes bottom chrome on every frame.
- **Template gap, for the shell and features' owners:** no destination calls `ReportDrawn`/`ReportDrawnWhen`, so
  time to full display is never measured (REQUIREMENTS 20 asks for start-up numbers). The overlay will flag it.
- **WP04 (`tools/perf`):** the marker check and `--allow-dev-tooling` in `perf.py`, and the marker pin in
  `test_perf.py`; `perf.sh recompose` counts frames whose `doFrame` opened any `Compose:recompose` section, roots
  and passes that executed nothing included, so it reads at or above the overlay's frames that composed (F6).
- **Owners of `:core:testing`, `:core:designsystem`, `:core:navigation`:** `RecompositionCounter` gains a public
  `tracer` and fails a test that counted nothing; the two private copies should follow or use the shared one.
- **WP12 (docs):** the recomposition section (restart, root, list row), the findings and their sources, the
  measured-build rule and the tracer's single slot belong in the generated project's performance guide and
  `AGENTS.md` (REQUIREMENTS 50).

### 2.8 What changed in this revision (answers to the design review)

| Review item | Change |
|---|---|
| Force bit is not specific to a restart (blocking) | F3 corrected; F3a and F3b added from the bytecode. Restart = force bit and no Different field; root = force bit with one; list rows classified by name and never scored. Verified that on an attached view `subcompose` passes the app's own content (not the 1524156494 wrapper), so the rule is checked on that path too, with reuse, re-set with the same lambda, movable content and the detached wrapper. Tests for all of them and the three cases the review asked for. |
| Pre-draw fires before measure-time composition (blocking) | F9 corrected. The frame close is gone: a batch ends with the main-thread message it ran in (a front-of-queue asynchronous message), is stamped at its first event and credited by time to the frame window; between-frames composition is its own category. `BatchCloseTest` covers the `SubcomposeLayout`-remeasured-in-draw case. |
| runtime-tracing is on staging (blocking) | F19 added; 2.1.6 rewritten: forward to Perfetto while recording, restore with `ComposeTracingInitializer().create(context)` on stop, decision and rejected options recorded; the test handles runtime-tracing instead of skipping; arm 3 asserts the tracer was installed and Perfetto names survived. |
| `"devReleaseImplementation"` is copied into measured builds (blocking) | F20 and F21 added; wiring is `"devReleaseRuntimeOnly"`; guard on every variant but debug and `devRelease`; guard filter test, CI `check` and the acceptance run cover the real plugin; the `"devReleaseImplementation"` mutation must fail. |
| `OverlayRecomposesNothingTest` cannot work with one tracer slot (blocking) | `CompositionWatch` forwards to a delegate; the test passes `RecompositionCounter`'s tracer and compares both counts; `RecompositionCounter` fails when it counted nothing. |
| Non-blocking | Line + 1 with a pinned test (F22); why `setTracer` despite being more internal, with version-pinned tests (F7, 2.1.6); question 3 turned into information; `JankyAfterFirst` presented as comparable, not the gate; idle split into a periodic finding tied to `idle.frames = 0` and a labelled heuristic, and the back-gesture citation removed from the 50 % rule; blocking GC instead of all GCs; "process allocation per frame drawn"; ANR red only after this build's install, with its time; marker pin test; draw-phase reads for the REC marker and the dot; AppScaffold heads-up in calibration and notes. |
