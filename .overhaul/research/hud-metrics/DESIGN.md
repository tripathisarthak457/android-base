# Performance overlay: recomposition score, "not optimised" flags and more signals

REQUIREMENTS 49 (the user, Oct 20 00:33), building on 40 (the overlay), 22 (recomposition) and 42 (release gates).
Design only; built on `staging-wp/HUD` after staging and the user's go (REQUIREMENTS 44). Written Oct 20 by the
overlay-metrics design workflow. Code read from branch heads: `staging` at `4818a926c` (HUD merged at `c9e71640`,
ported at `19aee799`, `perf.hud=off` default at `591d6d4e`), `wp/HUD` at `936fa2e8`, `wp/WP04` (`gates.toml`,
identical on staging). Every API fact below was checked against the artifacts this project builds with (section
2.0), not taken from the lookup.

---

## Part 1. In plain English

**What you will see.** The pill beside the DEV badge keeps its two numbers (frames per second and janky %). The
Perf tab gains three things:

1. **A recomposition score.** Compose apps stay smooth when moving things are *redrawn*, not *rebuilt*. Rebuilding
   part of the UI (a recomposition) is fine when something really changes, such as the first frame of a transition
   or a tap. It is a bug when it happens on every frame of a scroll, an animation or a transition: that is the
   pattern REQUIREMENTS 22 forbids, and it costs main-thread time in exactly the frames that must be on time. The
   overlay now watches every motion (a scroll, a fling, an animation, a screen transition) and counts how many of
   its frames rebuilt something, per screen and per state:
   - **Scrolling or dragging** (a finger is down, and the fling after it),
   - **Animating** (motion without a finger: animations, settles, a back gesture that is cancelled),
   - **Transition** (the screen changed during the motion: push, pop, tab switch, a committed back gesture),
   - **Idle** (nothing moving and nobody touching).

   It grades this like the other metrics: green when motions are quiet between their first and last frame, **red
   when something rebuilds on most frames of a motion**, amber when the app keeps rebuilding while idle (a ticking
   timer, a flow that keeps emitting). A **Capture** button records 10 seconds and lists the composables that
   rebuilt, by name and file:line, worst first, so you know what to fix. The names work in `devRelease` too,
   although it is minified.
2. **"Not optimised" flags.** One list at the top of the Perf tab: every metric that misses its target, why it
   matters, the limit it was held to (from `gates.toml`, the recomposition budgets or Android's own thresholds),
   and the fix in one or two sentences with a link to the doc.
3. **More signals, only cheap ones:** jank after each motion's first frame (the release gate's own metric), whether
   late frames lost their time on the main thread or the GPU, memory allocated per frame while drawing, the phone's
   thermal throttling (so numbers taken on a hot phone are marked), whether the last run ended in an ANR, and
   whether the app reports "fully drawn" so start-up can be judged.

**What it costs.** Nothing while the overlay is hidden: no listener, no thread, no tracer. While shown it works only
inside frames the app draws anyway, allocates nothing per frame and never rebuilds app content; the target is under
0.1 ms per frame on the Redmi, inside REQUIREMENTS 40's hard limits (+0.3 points of jank, 0.5 ms per frame). The
10-second Capture costs more and says so.

**What changes in how we measure.** The builds the perf harness measures (`devBenchmarkRelease`, and
`devNonMinifiedRelease` which records the baseline profile) will no longer contain the DEV badge, the inspector or the
overlay at all, enforced by the build and by `perf.sh` refusing to gate a build that has them. About the Oct 19
regression (the combined app about twice as hitchy on the back gesture): the overlay was very likely **not
recording** in that run (section 2.4.1), so its frame listener is not the suspect; what the measured build did carry
was the dev tooling at rest, including a new translucent DEV badge drawn over every frame. Removing dev tooling from
measured builds settles that part either way, and the first A/B run measures how much it was.

**What you decide:** section 2.6 (what the pill shows; whether recomposition counts in the 0-100 score; measured
builds without the badge; the badge's look if it turns out to cost frames).

---

## Part 2. Engineering detail

### 2.0 Facts this design relies on, and how each was checked

Artifacts: `runtime-android-1.12.1` (BOM `2026.09.00`), `ui-android`/`ui-graphics-android` 1.12.1,
`foundation-layout-android` 1.12.1, `metrics-performance` 1.0.0, `compose-compiler-gradle-plugin` 2.4.20, all from
`D:/.caches/gradle`, read with `javap` (JBR). Platform: `Sdk/sources/android-37.0` and
`platforms/android-37.0/data/api-versions.xml`.

| # | Fact | Checked by | Consequence |
|---|---|---|---|
| F1 | `Composer.setTracer(CompositionTracer)` exists; `CompositionTracer` has `isTraceInProgress()`, `traceEventStart(key, dirty1, dirty2, info)`, `traceEventEnd()`; annotated `@InternalComposeTracingApi`. `ComposerKt.compositionTracer` is a private, non-volatile static with no getter; `isTraceInProgress()` is a null check when none is set. | `javap` of `Composer$Companion`, `CompositionTracer`, `ComposerKt` | Zero cost when not installed; one global slot; another tracer cannot be detected, only overwritten. |
| F2 | Compiled composables call `traceEventStart(key, $dirty, -1, "fq.Name (File.kt:line)")` only inside the branch that executes (after `shouldExecute`), never for a skip. | `javap -c` of `BoxKt.Box` (foundation-layout 1.12.1); 21 such strings in that one artifact | Counts executions, not skips; names come from a string literal. |
| F3 | A scope that restarts calls its function with the force bit: `updateChangedFlags($changed or 1)` for functions, `updateChangedFlags(changed) or 1` for composable lambdas; `updateChangedFlags` keeps bit 0 (mask `0xC9249249`). The 2-argument `traceEventStart` overload passes `dirty1 = -1`. | `javap -c` of `BoxKt.Box$lambda$1`, `ComposableLambdaImpl.invoke$lambda$0`, `RecomposeScopeImplKt.updateChangedFlags`, `ComposerKt.traceEventStart(I,String)` | `dirty1 != -1 && (dirty1 and 1) != 0` marks an execution as **a restart of its own invalidated scope** (a state it reads changed), as opposed to being called by its parent (new content or changed arguments). |
| F4 | The runtime's consumer R8 rules strip only `sourceInformation*` (and assume values for two flags); nothing strips `traceEventStart`/`isTraceInProgress`. The template has no `-assumenosideeffects` at all. The compiler plugin defaults: `includeTraceMarkers = true`, `includeSourceInformation = true`. | `runtime.aar!proguard.txt`; `git grep` on staging; `javap -c` of `ComposeCompilerGradlePluginExtension` | In a minified `devRelease`, trace strings (names with file:line) survive; slot-table source information does not. |
| F5 | `Recomposer.runningRecomposers: StateFlow<Set<RecomposerInfo>>` and `RecomposerInfo.changeCount` carry no opt-in; `changeCount` is a plain `long` incremented once in the frame callback, only when `toApply` is non-empty; `performRecompose` returns the composition only when `recompose()` returned true (it produced changes). | `javap` of `Recomposer$Companion`, `RecomposerInfo` annotations, `Recomposer$runRecomposeAndApplyChanges$2` (bytecode 967-994), `Recomposer.performRecompose` | A stable, almost free per-frame signal, but it misses re-executions that change nothing and composition done during measure. Used as fallback and cross-check only. |
| F6 | `SubcomposeLayout` composes its slots during measure through `ReusableComposition.setContent`/`setContentWithReuse`/`setPausableContent`, outside the Recomposer's frame pass. `Compose:recompose` (what `perf.sh recompose` counts) is traced in `doCompose`, which both `recompose` and `composeContent` call. | `javap -c` of `LayoutNodeSubcompositionsState.subcompose`, `GapComposer.doCompose` | Lazy rows entering a list are composition, not recomposition; `perf.sh recompose` counts both, the overlay separates them (F3). |
| F7 | `CompositionObserver`, `CompositionRegistrationObserver`, `RecomposerInfo.observe` are `@ExperimentalComposeRuntimeApi`; `IdentifiableRecomposeScope`, `CompositionData.find`, `CompositionGroup.sourceInfo` exist. No `RecompositionTracer` class in 1.12.1. | `javap` of the `tooling` package | Observer kept for a later, debug-only "why" view (2.1.7). |
| F8 | Compose `Dialog` composes with `rememberCompositionContext()` and `DialogLayout.setParentCompositionContext` (shares the activity's Recomposer); the inspector panel is a `Dialog`. | byte search of `ui-android` classes; `DevToolsPanel.kt` on staging | The open panel's own recompositions hit the same Recomposer and tracer: counting pauses while the panel is open. |
| F9 | `Choreographer.postFrameCallback` posts to `CALLBACK_ANIMATION` (where Compose's frame clock recomposes); `performTraversals` measures, lays out, then `dispatchOnPreDraw`, then draws. | SDK 37 `Choreographer.java:753`, `ViewRootImpl.java:4556-4837` | A pre-draw listener closes a frame after all of that frame's composition (Recomposer pass and measure-time subcomposition). |
| F10 | JankStats 1.0.0 API 31 frame data: UI = sum of FrameMetrics 0-5; `frameDurationCpuNanos = TOTAL − GPU_DURATION + SWAP_BUFFERS`; `frameOverrunNanos = TOTAL − DEADLINE`. | `javap -c` of `JankStatsApi31Impl.getFrameData` | Main thread, render thread and (GPU − swap) are separable per frame from data the overlay already receives; no second listener. |
| F11 | `RenderNode.setHasOverlappingRendering`: "Non-overlapping rendering can use a fast path for alpha that avoids rendering to an offscreen buffer." Compose's `CompositingStrategy.Auto` sets overlapping rendering true without a compositing layer. | SDK 37 `RenderNode.java:1042`; `javap -c` of `GraphicsLayerV29.applyCompositingStrategy` | The DEV badge (`graphicsLayer { alpha = 0.85f }` at rest, Auto) is drawn through an offscreen buffer. Whether HWUI re-renders it every frame or caches it is native code, not checked: the A/B trace answers it. |
| F12 | Thermal: `getCurrentThermalStatus`/`addThermalStatusListener` API 29 (listener on the main thread unless an executor is given); `getThermalHeadroom(int)` API 30, "no benefit calling more than about once per second", NaN if unsupported; `getThermalHeadroomThresholds` API 35. | SDK 37 `PowerManager.java:3120-3334`, api-versions | Push listener plus a headroom read every 10 s (ADPF guide) is enough. |
| F13 | `Debug.getRuntimeStat("art.gc.bytes-allocated")` (API 23): total bytes allocated, approximate ("individual allocations may not be immediately reflected"). | SDK 37 `Debug.java:1862-1952` | Allocation per frame at 1 Hz, coarse but cheap. |
| F14 | `Looper.setMessageLogging` calls `StrictMode.noteSlowCall("setMessageLogging")`; the overlay's StrictMode watch enables `detectCustomSlowCalls()`. | SDK 37 `Looper.java:509-525`; `StrictModeWatch.kt` on staging | Rejected (2.3.2). |
| F15 | `ApplicationStartInfo` API 35; `getHistoricalProcessExitReasons` and `ApplicationExitInfo.REASON_ANR`/`REASON_LOW_MEMORY` API 30. | api-versions | Exit reason readable once per recording start. |
| F16 | `PointerInputModifierNode.sharePointerInputWithSiblings()` exists in ui 1.12.1. | `javap` | Lets an observer node see touches without taking them (behaviour pinned by a test, 2.5). |
| F17 | `perf.py` runs `pm clear` before every run, and at `e404e391` (the Oct 19 bug run) passed no `perf.hud` extra; `PerformanceRecorder` starts with `stored ?: environment.isDebugBuild`. | `git show e404e391:…/perf.py`, `staging:…/PerformanceRecorder.kt` | In the bug run the overlay was off: no frame listener of its own (2.4.1). |
| F18 | The control tree's badge (`wp/WP04` `DevBadge.kt`, visible in dev builds since `visible = isDebugBuild || !isShippable`) has no `graphicsLayer` alpha and no `systemGestureExclusion`; staging's `EdgeBadge` has both. | `git show wp/WP04:…/DevBadge.kt`, `DevEnvironment.kt`; staging `DevToolsOverlay.kt` | Corrects the bug file's "T + C1 had neither": both arms had a badge; only the new one is translucent. |

### 2.1 The recomposition score

#### 2.1.1 Signals

| Signal | API | Thread, when | Cost | Gives |
|---|---|---|---|---|
| **Composition tracer** (primary) | `Composer.setTracer` (F1-F3), installed while recording | Main thread, inside each executed composable | Per executed composable: the compiler's `isTraceInProgress()` call (now true) plus our `traceEventStart`/`traceEventEnd` (two monomorphic interface calls, a thread check, two int increments). Estimate 10-20 ns; nothing when nothing composes. | Per frame: **executions** (anything composed or recomposed) and **restarts** (scopes whose own state changed, F3). Names during a capture. |
| **Frame close** | `ViewTreeObserver.OnPreDrawListener` on the resumed activity's window, while recording (F9) | Main thread, once per drawn frame | One callback, one `nanoTime` only when the frame composed, three array stores, a volatile index write | Frame boundary for the tracer's counters; publishes `(time, executions, restarts)` to a 64-entry ring. |
| **Recomposer change count** (fallback, cross-check) | `RecomposerInfo.changeCount` (F5) from `Recomposer.runningRecomposers`, copied to an array when the set changes | Read in the frame close | A field read per running recomposer (usually one) | "This frame applied recomposition changes", when the tracer cannot be installed (2.1.6). |
| **Motion segmentation** | The frame log the overlay already keeps (start, UI time, interval, deadline) | JankStats' thread, per frame, under `FrameLog`'s existing lock | O(1) integer work | Motions, their first/middle/last frames, state, screen. |
| **Screen** | `ScreenTimeline` (run 6, unchanged) | as today | as today | The screen a motion belongs to. |
| **Touch** | A `Modifier.Node` observer (`PointerInputModifierNode`, `sharePointerInputWithSiblings = true`, never consumes) composed full-window by the overlay while recording | Main thread, per pointer event | A volatile store per event; no per-frame work | "A finger was down during this motion." |
| **Inspector open** | `PanelVisibility` (new, `:core:devtools`) | — | — | Counting pauses while the panel (a `Dialog` sharing the Recomposer, F8) is open; frames drawn then are excluded from the tables and the score. Today they are counted for the screen beneath. |

`FrameIntake` (JankStats' thread) matches each frame with the ring entry whose time lies in
`[frameStartNanos, frameStartNanos + frameDurationUiNanos]`, the window `ScreenTimeline` already uses. Entries are
consumed once, in order; entries older than the frame being matched are dropped. Frames without an entry composed
nothing. Composition that ran in a frame the activity window did not draw is counted with its next drawn frame
(documented). If JankStats' thread falls more than 64 frames behind, entries are overwritten and counted as lost
(FrameMetrics reports can be dropped the same way).

#### 2.1.2 Motions and states

- **Motion:** consecutive frames whose starts are at most 12 refresh intervals apart (the same separation
  `parse_framestats.hitch_time` uses, `_MAX_HITCH_INTERVALS = 12`), and at least 4 frames long. Shorter bursts
  (1-3 frames: a tap highlight, a text change, a clock tick) are **updates**.
- **Middle frames:** frames 2 to N−1 of a motion. RECOMPOSITION-PLAN §3.2 invariant 1: "Frame 1 holds the state
  flip; frame N holds the settle." The gates allow the same ends (`recompose.tab_switch.max = 2`,
  `recompose.pop.max = 2`, `recompose.back_gesture_progress.max = 0`).
- **State**, in order of precedence:
  1. **Transition:** `ScreenTimeline` has an entry inside the motion, or within 100 ms before its first frame.
  2. **Scrolling or dragging:** the touch observer saw a finger down at any frame of the motion (the fling after
     the lift stays in the same motion).
  3. **Animating:** any other motion. The system back gesture lands here until it commits (gesture navigation does
     not deliver the edge swipe to the app as touches); a committed one changes the screen and is a Transition.
  4. **Idle:** updates with no finger down.
- **Screen:** the screen at the motion's last frame (a transition counts for the screen it opens), the rule frames
  already follow.

#### 2.1.3 What is counted (per screen × state, at most 33 screens × 4 states, primitive arrays)

Motions, frames, seconds of motion, executions, restarts, frames with a restart, frames with any execution, middle
frames, middle frames with a restart, middle frames with any execution, **per-frame motions** (below), janky frames
after each motion's first, motions whose first frame missed its deadline, skipped vsyncs (2.3).

Shown per row: **restarts per second** and **per frame** (scope restarts, F3, which is what "recompositions" means
to a developer), **share of frames that restarted something**, **share of middle frames that restarted
something**, and the number of per-frame motions. "Executions per frame" (all composition, including rows entering
a list) is in the export, not the table.

#### 2.1.4 Verdicts against REQUIREMENTS 22

| Finding | Rule | Source | Colour |
|---|---|---|---|
| **Per-frame recomposition** | A motion with ≥ 4 middle frames, ≥ 50 % of which **restarted** a scope; in any state | REQ 22 "no per-frame recomposition during animations"; RECOMPOSITION-PLAN invariant 1; `recompose.back_gesture_progress.max = 0` | **Red**, listed with screen, state and the count of such motions |
| **Per-frame composition during a transition or animation** | A Transition or Animating motion with ≥ 50 % of middle frames **executing** anything (a `BoxWithConstraints` or `SubcomposeLayout` re-composed every frame of a size animation; F6) | Same invariant (the JVM journeys count any execution); `recompose.*` rows | **Red** |
| **Recomposition mid-motion** | Middle frames with a restart, below the per-frame rule | `RestartsMidMotion` limits below | Amber past grade A |
| **Steady recomposition while idle** | Restart frames in Idle updates, per minute of recording outside motion (a few data arrivals a minute are normal; something recurring is not) | RECOMPOSITION-PLAN journey "Idle 5 s on each tab: 0 anywhere"; `idle.frames = 0` | Amber past grade A (more than 2 a minute), red past D (more than one a second) |
| **Composition mid-transition** | Middle frames of Transition motions that executed anything (a list filling in during a push) | `recompose.push.max` is a ratchet, tab and pop allow 2 frames in total | Report-only, compared with the gates' rows |

Scrolling legitimately composes rows as they enter (executions without restarts). That is why the score uses
restarts, and the "executions" rule applies to Transition and Animating only.

**Score metrics** (added to `OptimisationScore.Metric`; graded only with enough data, as `MIN_FRAMES` does today):

| Metric | Value | A / B / C / D (F above D) | Weight | Graded after |
|---|---|---|---|---|
| `RestartsMidMotion` | % of middle frames, all motions, that restarted a scope | ≤ 1 / 3 / 10 / 25 % | 10 | 300 middle frames |
| `RestartsWhileIdle` | restart frames per minute outside motion with no finger down | ≤ 2 / 6 / 12 / 60 | 5 | 60 s outside motion |
| `JankyAfterFirst` (2.3) | % janky among motion frames other than each first | ≤ 0.5 / 1.0 / 2 / 4 % | 10 | 300 such frames |

The two `Restarts*` metrics also form the **Recomposition score** (0-100 and a letter, weights 10:5) shown on its
own card. The limits of both `Restarts*` metrics are a starting point and **must be calibrated in build step 9** before
they ship: the template's own journeys (catalog, the everything and standard presets' tabs, push/pop, back gesture,
scroll) must grade A, and a scratch build with one deliberate offender (a scroll offset read in composition on one
screen) must produce the red per-frame finding and name the composable in a capture. A template journey that does
not grade A is a template bug routed to its owner, not a reason to loosen the limit without a written reason.

#### 2.1.5 Names: the 10-second Capture

A button in the Recomposition section. It arms a capture that starts when the panel closes (the panel's own
composition would otherwise be counted) and stops after 10 s or 1200 drawn frames, whichever is first; the pill shows
a small "REC" marker while it runs (draw-only). While it runs, `traceEventStart` also looks the composable up in an
open-addressing table keyed by the compiler's group `key` and checked by the `info` reference (string literals, so
identity is stable; a collision probes on): 2048 slots of `IntArray`s (executions, restarts, frames ran in, frames
restarted in, peak per frame, last frame index) and the `info` references. Allocated once when the capture starts
(about 70 KB); a full table counts newcomers as "other". No allocation per call.

Result, sorted on the default dispatcher after the capture: the top composables by **frames they restarted in**,
then executions, with `Name · File.kt:line` split from `info`, executions, restarts, "ran in K of N frames", peak per
frame. Red when a composable restarted in ≥ 50 % of the frames drawn during the capture and N ≥ 30. The overlay's
own packages (`…core.perfhud`, `…core.devtools`, string constants the generator's package rewrite covers) are
filtered out; androidx and other libraries are grouped under "library" with a toggle.

**What it can see in a minified dev build:** every composable function and composable lambda compiled with trace
markers, by its source name and line (F2, F4): the template, its features and androidx. The restart/parent split
(F3) tells "its own state changed" from "its parent called it".

**What it cannot see:**
- **Which state** invalidated a scope. That needs `CompositionObserver.onScopeInvalidated` (2.1.7).
- **Inline composables** (`Column`, `Row`) and non-restartable or read-only ones: they have no restart scope of
  their own and count as part of their caller.
- Code compiled without trace markers, or a build that strips them with its own R8 rule. Detected: the change count
  moves (F5) while the tracer sees nothing for 60 frames; the section then says so and falls back (2.1.6).
- Composition in another process, and frames of other windows (dialogs are composed by the same Recomposer, F8,
  but drawn in their own window).

**Cost:** the hash probe on every executed composable while it runs, estimated ≤ 0.3 ms on a frame that composes a
whole screen (about 2000 executions), nothing on frames that compose nothing. Declared in the panel, flagged in the
export ("recorded during a capture"), measured in A/B arm 4.

#### 2.1.6 Fallback and coexistence

- The tracer is a process-wide slot that cannot be read (F1). The project does not use `runtime-tracing` today
  (`git grep` on staging); RECOMPOSITION-PLAN §4 proposes it as a dev dependency later. If its classes are on the
  classpath (one `Class.forName` at recording start), the overlay does not install its tracer, uses the change count
  (F5) for the per-frame flag, disables Capture and says why. When recording stops the overlay sets the tracer to
  null, so a session that recorded leaves Perfetto composition tracing off until restart (documented).
- Change-count mode cannot see re-executions that change nothing (a threshold derived in composition: the parent
  re-runs every frame, its children skip) or composition during measure (F5, F6), so its numbers can only be lower.
  A JVM test pins the two modes to agree on the simple case (a state written every frame and shown).
- JVM tests that compose the app through Hilt in a debug variant would start recording and install the tracer; they
  launch with recording off. `RecompositionCounter` (the JVM journeys) is in `:core:designsystem` and
  `:core:navigation`, which do not have the overlay.
- Layout Inspector's recomposition counts use their own bytecode hooks and keep working; its state-read view uses
  `setObserver`, which this design does not use.

#### 2.1.7 Considered for recomposition, and not used now

| Option | Why not (now) |
|---|---|
| `CompositionObserver` via `RecomposerInfo.observe` | Experimental (F7). No names in a minified build: `sourceInformation` is stripped (F4), and mapping a scope to `CompositionGroup.sourceInfo` needs each composition's `CompositionData`, which the observer does not hand over. One observer per composition, the slot Studio's state-read view uses. **Later, debug builds only:** a "why" column (the state class and, for a `State` holding a `Float`/`Offset`/`Dp`, "an animated value read in composition"), reading values under `Snapshot.withoutReadObservation`. |
| Change count as the primary signal | Free and stable, but blind to no-change re-executions and measure-time composition (F5, F6): it would grade a classic offender green. Kept as fallback and cross-check. |
| `RecompositionTracer` / `RecompositionTracingReceiver` | Not in 1.12.1 (F7; 1.13 alphas only); its KDoc warns of significant overhead. |
| `runtime-tracing` in the app | Names go to a system trace only, no in-app read-back, and it takes the same global tracer slot. `perf.sh recompose` and `CompositionCountBenchmark` already cover the trace side. |
| Layout Inspector counts | Not readable from the app. |

### 2.2 "Not optimised" flags

One list at the top of the Perf tab, also in the JSON (`findings`) and Markdown export and one `PerfHud` log line per
new finding. Each entry: severity, metric, value, the limit and where it comes from, the hint, the doc anchor in
`docs/PERFORMANCE-OVERLAY.md`. Red before amber, then by points lost. Context flags never cost points but mark the
recording.

| Metric | Amber / red | Limit from | Hint the overlay shows (shortened) |
|---|---|---|---|
| Janky frames (existing) | past A (0.5 %) / past C (2 %) | `gates.toml` scroll rows | "Open Frame times: red bars missed their deadline. The cause split below says whether the main thread or the GPU lost the time." |
| Janky after each motion's first (new) | past 0.5 % / past 2 % | REQ 42 release rows (`*.janky_after_first_pct` 0.5, back gesture 1.0) | "A motion's first frame may miss on a cold GPU; the ones after must not. Look at what runs on every frame of the motion: recomposition, allocation, a layer re-rendered each frame." |
| Main-thread p90/p99 (existing) | past A / past C | `gates.toml` UI-thread frame times | as today |
| Slow / frozen frames, long main-thread work (existing) | as today | Android vitals | as today |
| Per-frame recomposition (new) | red | 2.1.4 | "On {screen}, something rebuilds on every frame while it {scrolls/animates}. Read the moving value where it is used: in `offset {}`, `layout {}`, `graphicsLayer {}` or a draw block, or pass `() -> T` (R1, R2); for a threshold use `derivedStateOf`. Run Capture to see which composable." |
| Per-frame composition in a transition or animation (new) | red | 2.1.4 | "Content is composed on every frame of the motion: a `BoxWithConstraints` or `SubcomposeLayout` under an animated size, or a screen whose state changes while it slides in. Keep the entering screen's first state final (R10); size with `layout {}`." |
| `RestartsMidMotion` (new) | past A / past C | 2.1.4 | as per-frame recomposition, softer |
| `RestartsWhileIdle` (new) | past A (more than 2 a minute) / past D (more than 60) | RECOMPOSITION-PLAN idle journey; `idle.frames = 0` | "Something rebuilds about every {n} s with nobody touching the app: a ticker, a timer, or a flow emitting new but equal state. Collect with `collectAsStateWithLifecycle`, keep `UiState` equality honest, or draw a ticking value instead of composing it." |
| Jank cause (new, context) | majority main thread, or majority after it | F10 | Main thread: "composition, layout or work in the frame; see Recomposition and StrictMode." After it: "GPU or render thread: overdraw, blur, offscreen layers (a translucent full-screen layer, A11.3), large bitmaps; try hardware bitmaps and the glass tier below." |
| App memory, blocking GC (existing) | as today | `gates.toml` `meminfo.pss_app_mb`; existing limits | as today |
| Allocation while drawing (new) | past the calibrated limit | Template baseline measured in step 9 (unscored until then) | "The app allocates about {n} KB per frame while drawing. Nothing per frame in draw, layout or composition (R18): remember `Path`s and `Paint`s, use `mutableFloatStateOf`, no string formatting per frame." |
| GC while drawing (new) | any collection in a second with ≥ 30 frames | follows from the above | same hint |
| Cold / warm start (existing) | as today | `gates.toml` cold start 380 ms | as today |
| Full display not reported (new, context) | amber when the latest cold start has no `FULLY_DRAWN` (Android 15+) | Android vitals / Macrobenchmark TTFD | "Start-up can be judged only to the first frame. Add `ReportDrawnWhen { … }` on the start destination when its content has loaded." (The template does not call it today: `git grep` on staging; routed to the template's owner, 2.7.) |
| Previous exit (new) | red for `REASON_ANR`; low-memory kill reported only | F15 | "The last run ended in an ANR: the main thread was blocked for seconds. StrictMode and the long-work count show where." |
| StrictMode, R8, baseline profile, not debuggable (existing) | as today | as today | as today |
| Thermal (new, context) | status `MODERATE` and up (or, on API 35+, headroom past the device's own `MODERATE` threshold from `getThermalHeadroomThresholds`) / status `SEVERE` and up, or headroom ≥ 1.0, which the javadoc defines as `SEVERE` | F12 | "The phone is throttling: numbers recorded now understate the app. Let it cool before judging." The score card says "recorded while throttled". |
| Overlay context (new) | — | — | Marks numbers taken during a capture or with the panel open, as the debug-build notice does today. |

### 2.3 Other signals worth adding

#### 2.3.1 Added (cheap enough)

| Signal | How it is read | How often, which thread | Verdict |
|---|---|---|---|
| **Janky after each motion's first** and **late first frames** | From each frame's deadline flag (already recorded) and the motion segmentation | Per frame, JankStats' thread, O(1) | Scored (2.1.4); late first frames reported, as `gates.toml` does |
| **Where late frames lost their time** | Per janky frame: main thread = UI time over the interval; else render thread = `cpu − ui`, GPU (minus swap) = `total − cpu` (F10). API 31+ | Per janky frame, JankStats' thread, a few subtractions | Context: majority cause, with its hint |
| **Skipped vsyncs during motion** (app side) | Sum of `start gap − interval` for gaps over 1.5 intervals inside motions, per second of motion | When a summary is taken, from the ring | Report-only. Not the release gate's hitch ratio, which uses the display's present times that an app cannot read; `perf.sh` stays the gate |
| **Allocation while drawing** | `art.gc.bytes-allocated` (F13) with the 1 Hz sampler, divided by frames drawn in that second; GC count delta in seconds with ≥ 30 frames | 1 Hz, sampler (off the main thread); one more counter read | Flag after calibration (2.2) |
| **Thermal status and headroom** | `addThermalStatusListener(executor, …)` while recording (API 29+); `getThermalHeadroom(0)` every 10 s (API 30+); NaN reads "not reported" (some devices always report NONE: the ADPF guide's warning) | Push on change; 10 s on the sampler | Context (2.2) |
| **Previous exit reason** | `getHistoricalProcessExitReasons(pkg, 0, 1)` (API 30+) | Once per recording start, I/O dispatcher | ANR red; low-memory kill reported |
| **Full display reported** | `START_TIMESTAMP_FULLY_DRAWN` from the existing `ApplicationStartInfo` read | With the existing start-up read | Context flag |
| **Inspector frames excluded** | `PanelVisibility` | — | Accuracy fix: frames drawn while the panel is open no longer count for the screen beneath |

#### 2.3.2 Considered and rejected

| Signal | Why not |
|---|---|
| `Looper.setMessageLogging` message timings | Builds a string per message ("performance penalty", javadoc) and is itself a StrictMode slow call the overlay's own watch counts (F14). A frame delayed by a blocked main thread already shows its delay in the UI time (UNKNOWN_DELAY is part of it, F10). |
| A second `OnFrameMetricsAvailableListener` (exact `GPU_DURATION`, `UNKNOWN_DELAY`) | A second per-frame callback for numbers F10 already approximates; the overlay's rule is one listener per window. |
| A main-thread watchdog (post every 500 ms, time the delay) | Wakes an idle main thread twice a second; frames already include the delay; StrictMode and the ANR exit reason cover the rest. |
| `ActivityManager.getProcessMemoryInfo`, more PSS | Rate-limited, debugging-only per its javadoc, and a PSS walk costs a frame (`perf/HUD/pss-probe/`). PSS stays at most every 15 s in a pause. |
| Thermal headroom listener, headroom thresholds | API 36 and 35, behind flags; the 10 s read is enough. |
| Frame-rate requests and ARR (`getRequestedFrameRate`, `hasArrSupport`) | Read-back of what the app asked, not what it got; the overlay already grades each frame against its own interval and shows the rate drawn at. |
| Compose compiler stability reports in the overlay | Build-time data; `checkComposeStability` already fails the build for the design system and `:core:ui`. Showing feature modules' counts would need every module's reports wired into a dev-only asset. Later, if wanted. |

### 2.4 The cost budget

#### 2.4.1 What the Oct 19 regression says about the overlay

- `bugs/BUG-2026-10-19-back-see-through.md` (Result) measured the combined tree's back gesture at 12.7 ms/s of
  hitch against 6.9 for T + C1, and 1.2-1.5 % janky after the first frame against 0.3-0.4 %, A/B controlled. Its
  first suspect, "the HUD's frame listener and its DEV badge in the measured `devBenchmarkRelease`", was not
  verified, and `staging/STAGING-PLAN.md` §0 ranks a stale baseline profile first.
- **The overlay was almost certainly not recording in that run** (F17): `perf.py` at `e404e391` cleared the app's
  data before each run and passed no `perf.hud` extra, and a release-like build with no stored choice starts with
  recording off. So the overlay's JankStats feed was not on the window. In the everything preset the analytics
  recorder's JankStats is always on, in prod as well; it is not the overlay's.
- **What the measured build did carry** is the dev tooling at rest: the recorder built at launch (measured within
  noise for cold start in run 4), the inspector's network observer, the screen reports, and WP06's `EdgeBadge`
  composed over every screen, translucent at rest (alpha 0.85 with Auto compositing, so drawn through an offscreen
  buffer, F11) and excluding its edge from system gestures. The control tree also had a badge, but an opaque one
  without gesture exclusion (F18), so "T + C1 had neither" in the bug file is half right.
- This design does not claim a cause. It removes all dev tooling from measured builds (below), which makes the
  question moot for the gates, and A/B arm 1 measures how much of the regression it was. Staging's fix loop owns the
  regression.

#### 2.4.2 The budget

| Rule | Limit | Enforced by |
|---|---|---|
| **Absent from every shipped build** (unchanged) | No class, resource, manifest entry, thread | `verify<Variant>CarriesNoDevTooling`, `apk_inspect.py`, the prod thread check (run 2) |
| **Absent from every build the perf harness measures** (new) | `devBenchmarkRelease` and `devNonMinifiedRelease` carry no `:core:devtools` or `:core:perfhud` | Wiring: `debugImplementation` + `"devReleaseImplementation"` in place of `"devImplementation"` (declare the configuration if AGP 9.4 does not create flavour-and-build-type configurations itself). Guard: register `verify<Variant>CarriesNoDevTooling` for every non-debug variant except `devRelease` (today it skips the whole dev flavour). Harness: `perf.py` refuses `--gate` when the APK's dex holds the dev tooling markers (`devtools_badge`, `perf.hud`: string constants found only in those modules), unless `--allow-dev-tooling`, which never gates. The baseline profile, recorded on `devNonMinifiedRelease`, then holds no tooling classes either. |
| **Zero work while hidden** | No pre-draw listener, pointer observer, frame listener, tracer, thermal listener, timer or thread of the overlay's; idle draws 0 frames | Unit tests (listener lists by reflection as `RedrawDuringFramesTest` does; `isTraceInProgress()` false after stop; thermal listener removed); phone: thread list and idle frames hidden vs a build without tooling (arm 2) |
| **Sampling, not per-frame work, where possible** | Memory, GC, allocation, thermal, exit reason: sampled (1 Hz, 10 s, once). Per frame only what must be: the frame close (main) and the intake (JankStats' thread) | Code review against this table |
| **No allocation per frame** | 0 bytes per frame in `FrameIntake.onFrame`, the frame close, the tracer callbacks (outside a capture and, after warm-up, inside it), the motion tracker | JVM tests with `com.sun.management.ThreadMXBean.getThreadAllocatedBytes` over 10,000 calls after warm-up (JBR is HotSpot) |
| **No recomposition of app content** | App composables execute the same with recording on and off; the overlay's composables execute 0 times per frame while the app animates (the pill redraws in draw only) | JVM test with `RecompositionCounter` and `assertQuietBetweenEnds` (the journeys' own tools) |
| **No frame of its own** (unchanged) | Redraws only inside app frames | `RedrawDuringFramesTest`; idle 0 frames shown (phone) |
| **Shown: total overhead** | REQ 40 hard limits: +0.3 points of jank and under 0.5 ms main-thread time per frame; **target ≤ 0.1 ms** (run 6 measured +0.020 to +0.055 ms before these additions) | A/B arm 3 on the Redmi |
| **Capture: declared** | Ends by itself in 10 s; ≤ 0.3 ms extra on a frame that composes a screen (estimate, to be measured); numbers flagged | A/B arm 4; `CompositionCaptureTest` |
| **Dev badge at rest** | No offscreen buffer per frame unless the A/B shows it costs nothing | Arm 1 and 2 traces; if it costs, `CompositingStrategy.ModulateAlpha` or opaque at rest (question 4) |

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
| 3. Overlay shown | `devRelease` `perf.hud=on` vs `off` | as arm 2, plus `recompose` from `perf.sh --allow-dev-tooling --extra perf.hud=on` | REQ 40 limits; target ≤ 0.1 ms; idle 0 frames shown; the overlay's per-row numbers agree with `perf.sh recompose` within the expected difference (perf.sh also counts rows entering) |
| 4. Capture | shown + capture vs shown | 10 s captures during the scroll and push/pop steps | Reported; the capture stops by itself; no tracer installed afterwards (unit test) |
| Calibration (step 9) | the template's journeys, and a scratch offender | `devRelease` shown | 2.1.4: template A, offender red and named |

### 2.5 Where it goes, tests, and order

Owner of every item: **`staging-wp/HUD`**. Reviewers: WP04 for `tools/perf`, the devtools owner (WP06) for the
`:core:devtools` API, WP12 for the docs. Paths under `templates/android/`; `P` =
`core/perfhud/src/main/kotlin/com/base/app/core/perfhud`.

| Piece | Files |
|---|---|
| Measured builds without tooling | `app/build.gradle.kts` (wiring); build-logic `DevToolingGuard.kt` (guard every non-debug variant but `devRelease`), `DevToolingGuardTest.kt`; `tools/perf/perf.py` (`--allow-dev-tooling`, the marker check), `test_perf.py`; generator `tests/test_android_target.py` (the two wiring lines); `docs/ARCHITECTURE.md` (guard row); `docs/PERFORMANCE-OVERLAY.md` |
| Inspector hooks | `core/devtools/.../page/DevToolsPage.kt` (`windowContent: (@Composable () -> Unit)?`, composed at the overlay's root, outside the keyboard early return); `.../panel/PanelVisibility.kt` (new `@Singleton`: `open: StateFlow<Boolean>`, `requestClose()`); `.../badge/DevToolsOverlay.kt` (publishes the panel state, composes `windowContent`); `features/devtools.toml` owns both |
| Composition tracking | `P/recomposition/CompositionWatch.kt` (the tracer, main-thread counters, capture hook, install and uninstall), `FrameCompositions.kt` (frame close, ring, change-count fallback), `Recomposers.kt` (running recomposers to an array), `CompositionCapture.kt` (table, 10 s stop, result), `RecompositionSummary.kt` |
| Motions | `P/frame/MotionTracker.kt` (segmentation, state, per screen × state arrays, janky after first, late first frames, skipped vsyncs, cause split); `FrameIntake.kt` (ring match, touch, cause); `FrameLog.kt` (holds the tracker, `summary()`); `ScreenTimeline.kt` (whether a screen was entered within a span, allocation-free); `FrameReadings.kt` |
| Touch | `P/frame/TouchState.kt`; `P/overlay/TouchObserver.kt` (`Modifier.Node`, shares with siblings, never consumes) |
| Extra signals | `P/device/ThermalWatch.kt`; `P/memory/MemorySampler.kt`, `MemoryLog.kt` (bytes allocated, GC while drawing); `P/startup/StartupTimes.kt` (fully drawn present); `P/startup/ExitReason.kt` |
| Score, findings | `P/score/OptimisationScore.kt` (three metrics, recomposition sub-score), `P/score/Findings.kt` (rules, severity, hint resources, doc anchors) |
| UI | `P/panel/RecompositionSection.kt`, `FindingsSection.kt`, `PerformanceTab.kt`; `P/overlay/PerformancePill.kt` (only as the user chooses, 2.6); `res/values/donottranslate.xml` |
| Export, log | `P/report/PerformanceReport.kt` (schema 3: `motions`, `idle`, `capture`, `findings`, `thermal`, `allocation`, `jankCauses`), `MarkdownReport.kt`, `P/log/PerfLogcat.kt` (`restartMid=`, `perFrameMotions=`, `idleRestarts=`, `jankyAfterFirst=`) |
| Recorder | `P/recording/PerformanceRecorder.kt` (start and stop the new pieces with recording; pause on `PanelVisibility`) |
| Docs (REQ 50, same change) | `docs/PERFORMANCE-OVERLAY.md` (Recomposition, the findings and their limits, costs, what a minified build can name, the tracer's single slot); `AGENTS.md` short rule: "a red Recomposition finding is fixed before UI work is done; run Capture to find it" |
| Generator | `features/perfhud.toml` (owns the new files), `web/frontend/lib/catalogue.json` (regenerated) |

Tests (JVM and Robolectric unless marked):

- `CompositionWatchTest`: a state written every frame and shown gives one restart per frame; a parent passing a new
  argument executes the child without a restart; `dirty1 = -1` is not a restart; calls off the main thread are
  ignored; after stop `isTraceInProgress()` is false; with `runtime-tracing` simulated present it does not install.
- `FrameCompositionsTest`: pre-draws (dispatched by hand, as `RedrawDuringFramesTest` does) close frames; an entry is
  matched once and in order; a frame that composed nothing writes nothing; change-count mode agrees with the tracer
  on the simple case.
- `MotionTrackerTest`: the 12-interval split, the 4-frame minimum, first and last frames excluded, the state
  precedence, the per-frame rules, janky after first, late first frames, skipped vsyncs, idle updates.
- `CompositionCaptureTest`: names and file:line, restarts vs executions, "ran in K of N frames", a full table counts
  "other", the 10 s and 1200-frame stops, the overlay's own packages filtered.
- `PerFrameAllocationTest`: zero bytes per call for the intake, frame close, tracer (and capture after warm-up),
  tracker.
- `OverlayRecomposesNothingTest`: an animated screen's composables execute the same with recording on and off; the
  overlay's composables 0 times between the ends.
- `TouchObserverTest`: a click and a scroll on content beneath still work while recording; down and up are seen;
  nothing is composed while not recording.
- `ThermalWatchTest` (Robolectric's `ShadowPowerManager` if it drives thermal status, otherwise a small
  `PowerManager` seam), `PanelVisibilityTest`, `OptimisationScoreTest` (the new metrics, ungraded without enough
  data), `FindingsTest` (each rule at its limit, each hint resource present), `ReportFormatsTest` (schema 3).
- build-logic `DevToolingGuardTest`: `devBenchmarkRelease` and `devNonMinifiedRelease` guarded, `devRelease` and
  debug not. `test_perf.py`: `--gate` refused for an APK whose dex holds a marker, accepted for one without, and
  `--allow-dev-tooling` never gates. Generator: the wiring lines.
- Mutation check, as runs 4-6 did: put back per-frame allocation, per-frame recording on the pill, the tracer left
  installed after stop, and the old wiring; each must fail its test.
- Phone: section 2.4.3; `apk_inspect.py` on `devBenchmarkRelease` (zero tooling classes, resources, strings); the
  jetpack-compose-performance audit over every new composable and node.

**Order** (each step ends green and reviewed before the next):

0. Staging done, the user's go, `staging-wp/HUD` cut from staging (REQUIREMENTS 44).
1. **Measured builds without tooling** (wiring, guard, `perf.py` refusal, tests, docs) and **A/B arm 1**. First,
   because every later measurement depends on it and it feeds staging's regression hunt; if staging's fix loop has
   already changed the wiring, take theirs and keep only what is missing.
2. Inspector hooks (`PanelVisibility`, `windowContent`) and the exclusion of inspector frames.
3. Composition tracking, motions, janky after first, cause split, skipped vsyncs; the allocation tests.
4. Score metrics, findings, Recomposition and Findings sections, log and export (schema 3), docs.
5. Capture.
6. Touch observer (until then "Scrolling or dragging" folds into "Animating").
7. Thermal, allocation and GC while drawing, full-display flag, exit reason.
8. The pill, as the user chose.
9. Phone: calibration, A/B arms 2-4, demo capture (`perf/HUD-metrics/SHOW.md`), Opus verifier, Opus review, fix loop.

Phase B (Compose Multiplatform desktop and web) can reuse `CompositionWatch` and the motion logic (common Kotlin);
the frame close and the touch observer are Android-only.

### 2.6 Questions only the user can answer

1. **What the pill shows.** (a, recommended) Keep FPS and janky %, and add one coloured dot for the worst open
   finding (no text, no wider pill); tapping opens the findings. Pros: no new line, red per-frame recomposition is
   visible at a glance. Cons: a dot needs the tab to explain it. (b) A third line, the mid-motion restart share
   ("R 0.4%"). Pros: a number at a glance. Cons: a taller pill over the app; one more number to read. (c) No change;
   findings only in the tab. Pros: nothing new on screen. Cons: problems stay unseen until someone opens the tab.
2. **Recomposition in the 0-100 score.** (a, recommended) Yes: `RestartsMidMotion` (10) and `RestartsWhileIdle` (5)
   join the total, and the Recomposition score also shows on its own card. Pros: REQ 22 is a release requirement
   and the score should say so. Catch: every existing score moves a little; graded only with enough motions.
   (b) A separate Recomposition score only. Pros: the existing score is unchanged. Cons: an app can read A while
   rebuilding on every frame.
3. **Measured builds carry no dev tooling.** (a, recommended) `devBenchmarkRelease` and `devNonMinifiedRelease`
   lose the DEV badge, the inspector and the overlay; `perf.sh` refuses to gate a build that has them. Pros: the
   gates measure what ships; ends the "is it the badge" question; cleaner baseline profiles. Catches: perf
   recordings show no DEV badge; QA wanting the badge uses `devRelease`; REQUIREMENTS 40's "every dev-flavour
   build" yields to 49's "absent from every build the perf harness measures". (b) Keep them and prove they cost
   nothing. Cons: a proof to repeat after every change to the badge or the inspector.
4. **Only if A/B arm 1 or 2 shows the badge costs frames:** make the badge opaque at rest (a visible change to
   WP06's look; recommended), or draw its alpha per operation (`ModulateAlpha`: the same look within a few percent,
   the label slightly tinted by its own background).

### 2.7 Notes for others

- **Staging's fix loop:** F17 and F18 correct the bug file's suspect list: the overlay was off in the bug run, and
  the control tree had a badge too, but an opaque one. Arm 1 is the discriminator for the dev tooling; it does not
  replace the baseline-profile check ranked first in `STAGING-PLAN.md`.
- **Template gap, for the shell and features' owners:** no destination calls `ReportDrawn`/`ReportDrawnWhen`, so
  time to full display is never measured (REQUIREMENTS 20 asks for start-up numbers). The overlay will flag it.
- **WP04 (`tools/perf`):** the marker check and `--allow-dev-tooling` in `perf.py`; `perf.sh recompose` counts
  frames with any `Compose:recompose` section, which includes rows entering a list, so its numbers are at or above
  the overlay's restart counts by design.
- **WP12 (docs):** the recomposition section, the findings and their sources, the measured-build rule and the
  tracer's single slot belong in the generated project's performance guide and `AGENTS.md` (REQUIREMENTS 50).
