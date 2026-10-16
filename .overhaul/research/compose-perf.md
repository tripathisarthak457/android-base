# Compose performance research: minimum jank at 120 Hz on the Redmi Note 10 Pro

Written 2026-10-09 for the android-base overhaul (`overhaul/multiplatform-v2`). Scope: Compose UI 1.12.1
(BOM 2026.09.00), Navigation 3 1.2.0, navigationevent 1.2.0, Kotlin 2.4.20, AGP 9.4.1, minSdk 26, targetSdk 37.
Reference phone: Redmi Note 10 Pro (M2101K6I, Snapdragon 732G, Adreno 618, 1080×2400, 120 Hz). Its ROM reports
SDK 37 and renders with HWUI **Skia OpenGL** (`Pipeline=Skia (OpenGL)` in every gfxinfo dump).

Evidence tags used throughout:

- **[S]** verified in source. androidx 1.12.1 comes from the `-sources.jar` files in `~/.gradle/caches`, Navigation
  3 from the 1.1.7 sources (1.2.0 ships no sources jar), and AOSP from `android.googlesource.com` main.
- **[D]** official documentation or an official blog post (URL in Sources).
- **[M]** measured: my analysis of the existing phone dumps in `perf/WP04/baseline` and `perf/WP04/phase2`.
- **[U]** unverified: a hypothesis, an estimate or an engineering judgement. Measure it before relying on it.

Items 19–22 and 24 of the user's requirements decide the bar: ~0 % janky frames, no dropped first frames, the
full iOS motion set, and no per-frame recomposition. This document ranks techniques by how much they move **this
project's** failing gates (`perf/WP04/phase2/summary.md`). It does not rank them in general.

---

## 0. TL;DR, ranked for this project

| # | Do this | Why, for this project | Gate rows it moves | Effort |
|---|---|---|---|---|
| 1 | **Ship the app's own baseline profile.** Also hand-write wildcard profiles for `:core:designsystem` and `:core:navigation`. First run the `-m speed` A/B to bound the gain | The phase-2 APK's merged ART profile has **0 rules for app code out of 5,169 lines** [M]. A push composes a 5-Text screen in 8–30 ms, every one of the 8 times [M]. That points to interpreted code that never gets hot enough to JIT | push composing frame, first visit, pop, p99 | S |
| 2 | **Stop moving the covered screen between scenes. Park it instead**: attached, measured, placed with its layer, `alpha = 0`, off-window. Apply the same to hidden kept tabs | Pop first frame: composition 7–19 ms plus layout/record 7–17 ms [M]. A movable-content move between scenes **detaches the subtree. That releases every graphics layer and runs `onDetach` on every Modifier.Node**. Unplacing also releases the layers [S]. So the "warm" pop re-attaches, re-measures and re-records the whole screen | pop first frame 15/37 → target ≤ 4/8 ms; gesture-pop first frame; rapid-back coalesced pop 23.6/34.6 | M–L |
| 3 | **Keep the composing frame from eating the animation.** Compose the arriving screen in the frame *before* the slide's clock starts. Seed the destination's state synchronously, with no skeleton swapped for content mid-slide. Defer below-the-fold content. Optional: precompose on press-down (`createPausedPrecomposition`) | In push, frame 1 composes (anim 8–30 ms) and is also the animation's first frame. The skeleton→content→related swaps land on frames 2–3 [M]. Compose has no View-style COMMIT start-time correction [S] | push/pop p99 57 ms, push hitches | M |
| 4 | **Stay inside the GPU budget during transitions.** Re-measure after `d56ad13`. Trace GPU frequency and the work per frame | Phase-2 pop issue→GPU-complete p50 is **14.6 ms vs 6.5** in the baseline, with `dequeueBuffer` waits of 2.9 ms at p50 (0 in the baseline) [M]. The APK was built at 08:13, before `d56ad13` (08:54), which removed a hidden full-screen draw on pops [M] | pop GPU, latency, p99 | S–M |
| 5 | **Pay first-use costs while idle.** Shader, glyph and texture warm-up after the first frame. Preload the downloadable Google Fonts before the splash ends. Create AGSL `RuntimeShader`s early | First tab visits show RenderThread "issue" times of 43–45 ms on 2 of 4 visits [M]. That is the first use of draw ops after `pm clear` wiped the HWUI shader cache [S]. `AppFonts` uses async `GoogleFont` with no `preload` [M] | first visit 22.7/27.6, first push | S–M |
| 6 | **Precompose tab roots** between frames (paused precomposition) after the first frame, or on press-down of a tab | First visit to a tab: 22.7 / 27.6 ms [M] | first-visit rows | M |
| 7 | **Use a lazy cache window** on primary lists. Pausable prefetch is already on by default | Scroll is already 0.15 % janky. This protects heavier rows | scroll rows | S |
| 8 | **Keep the rendering hygiene that is already in place** (deferred reads, Modifier.Node, slides with no alpha). Remove CompositionLocal and lifecycle flips from critical frames | `KeepUnderScene` and `KeptTabs` flip `LocalLifecycleOwner` and `LocalTabVisible` on the transition frame [M] | pop, tab switch | S |
| 9 | ModulateAlpha or no alpha. Never use an offscreen layer on large layers | Only small offenders remain (dialog and menu fades) | — | S |
| 10 | Keep the cheap text defaults. Use no `autoSize` in transitions. Animate colour with `ColorProducer` | Defaults resolve to `LineBreak.Simple` and `Hyphens.None` [S] | — | S |
| 11 | R8: full mode is already on. Generate the startup profile. Consider AGP 9.3+'s `optimization {}` block. Fix a stale keep rule | `proguard-rules.pro` keeps `com.base.app.core.navigation.AppNavKey`, but M1 moved it to `core.common.navigation` [M] | cold start (416 ms as installed vs a 400 ms gate) | S |
| 12 | Frame-rate voting (ARR) | The Redmi held 120 Hz: `FrameInterval` = 8.33 ms in all ~30k frames [M]. ARR phones need explicit votes for animations that only change graphicsLayer properties [S] | none on the Redmi | S |
| 13 | GC churn | Small per-frame allocations, for example in `CoverScrim` | — | S |
| 14 | Skia GL vs Vulkan | The device is Skia GL and an app cannot choose. Use it as an A/B diagnostic only | — | — |

**Prerequisite (§3): measure what the user sees.** gfxinfo's janky % counts deadline misses. It hides GPU
queueing and latency, and the "first frame UI ms" rows measure CPU, not dropped presents. Add FrameTimeline and
present-gap (hitch-ratio) metrics, a GPU-frequency trace and separate cold and warm first-use scenarios. Otherwise
fixes 2–5 cannot be told apart.

---

## 1. What the phone data already says (new analysis of the existing dumps)

I parsed every `---PROFILEDATA---` block in `perf/WP04/{baseline,phase2}/run*/` (Flags = 0 frames only). Stages:

- **ui**: `HandleInputStart→SyncQueued`
- **anim**: `AnimationStart→PerformTraversalsStart`. This is where the Recomposer runs, so it is composition.
- **record**: `DrawStart→SyncQueued`. Compose's measure/layout runs inside `dispatchDraw`, so this stage holds
  layout plus display-list recording.
- **issue**: `IssueDrawCommandsStart→SwapBuffers`
- **gpu**: `IssueDrawCommandsStart→GpuCompleted`. It includes waiting for the GPU queue.
- **dequeue**: `DequeueBufferDuration`

### 1.1 Stage medians and tails (ms)

| Scenario | total p50/p99 | ui p50/p99 | anim p99 | record p99 | issue p50 | gpu p50/p90 | dequeue p50/p90 |
|---|---|---|---|---|---|---|---|
| pop, baseline | 8.8 / 40.0 | 0.8 / 19.3 | 5.6 | 13.8 | 1.1 | 6.5 / 8.0 | 0.0 / 0.0 |
| **pop, phase 2** | **17.9 / 56.7** | 1.4 / 19.4 | **12.4** | 7.4 | **4.5** | **14.6 / 16.7** | **2.9 / 4.4** |
| push, baseline | 7.9 / 28.4 | 0.9 / 8.7 | 5.3 | 3.2 | 0.8 | 5.8 / 14.9 | 0.0 / 0.0 |
| **push, phase 2** | 11.0 / **61.8** | 1.4 / **20.0** | **11.1** | **9.3** | 1.6 | 8.3 / 16.4 | 0.0 / 4.3 |
| tabs, baseline | 9.4 / 40.6 | 1.3 / 16.8 | 8.2 | 6.6 | 1.0 | 7.1 / 15.1 | 0.0 / 4.2 |
| tabs, phase 2 | 9.4 / 26.8 | 1.1 / 6.5 | 3.2 | 3.3 | 1.1 | 6.6 / 8.6 | 0.0 / 0.0 |
| scroll root, phase 2 | 9.6 / 32.1 | 0.9 / 1.6 | 1.2 | 0.5 | 1.4 | 7.0 / 14.5 | 0.0 / 4.1 |

### 1.2 The first frames, one transition at a time (phase 2, run 2)

- **Pop, first frame.** anim 7.0–19.4 ms plus record 6.8–17.5 ms, then GPU 12–15 ms [M]. The list screen
  "kept warm" by `KeepUnderScene` is composed *and* laid out *and* re-recorded on the pop frame. §2.2 explains why.
- **Push, first 3 frames.** Frame 1: anim 8.1–30.2 ms and record 7.3–20.1 ms. Frames 2–3: GPU 14–29 ms, with
  issue = 16.2 ms on the very first push [M]. The detail screen is tiny: a scaffold, a top bar, three texts and
  three rows. It shows a skeleton first (`LoadState.Loading` until the `Appeared` event loads), then the item,
  then the related rows. That is three compositions inside the slide.
- **First tab visits.** Visits 001 and 004 have **issue = 43.1 and 45.2 ms, GPU 48–58 ms** [M]. RenderThread time
  of that size, with modest UI time, is first-use GPU work: shader programs, glyph-atlas uploads, texture uploads
  [U]. It is consistent with `perf.sh` running `pm clear` first, which deletes the HWUI shader disk cache (§2.5) [S].

### 1.3 Display cadence vs gfxinfo jank

`DisplayPresentTime` in framestats [M] shows that phase-2 pops presented **966 of 973 frames on consecutive
vsyncs**. The gfxinfo "Janky frames" row (2.3 %) and the 16 ms p50 frame time do not mean visible stutter. They mean
the GPU runs at or above the 8.33 ms budget. Frames queue: `dequeueBuffer` waits about 3 ms, and intended→present
latency is about 46 ms mid-pop vs about 38 ms in the baseline, roughly one extra frame [M][U]. That is one hiccup
away from a drop, and it adds touch-to-photon lag during back swipes.

A caveat on this column: some trailing rows of each dump carry stale `DisplayPresentTime` values (negative
latency), so treat these numbers as indicative. Confirm them with Perfetto FrameTimeline (§3).

### 1.4 Other facts from the dumps

- **The phone stayed at 120 Hz.** `FrameInterval` was 8.33 ms in every frame of both builds [M]. Refresh-rate
  switching is not a confounder on this ROM.
- **The phase-2 APK predates the pop-overdraw fix.** `app-dev-arm64-v8a-benchmarkRelease.apk` dates from 08:13.
  `d56ad13` ("a pop drew a hidden full screen per frame") was committed at 08:54. Re-measure before attributing
  the pop's GPU regression to anything else [M].
- **No app baseline profile.** `out/WP04/p2/app/build/intermediates/merged_art_profile/.../baseline-prof.txt` has
  5,169 lines and 0 matching `com/example/wp04` [M]. `gates.toml` says rows are "measured … with release builds
  compiled with their baseline profile". Today that means library profiles only.

### 1.5 Harness caveats that bias the numbers

- `pm clear` before each run wipes the shader cache, so cold first use is measured as if it were typical.
- `input swipe` produces the touch stream for the gesture scenarios. The 46 two-vsync present gaps in phase-2
  gesture pops may be injector cadence rather than app jank [U]. Check the input events in a trace.
- Tab taps hold the finger down for 250 ms before the lift (`PRESS_SETTLE_S`). That flatters any
  "precompose on press-down" technique. A real tap holds for 50–120 ms [U]. Keep a plain `input tap` variant
  for honesty.
- Battery was 29.9 °C for the baseline run and 36.0 °C for phase 2. The 37 °C gate is close.

---

## 2. Techniques, in rank order

### 2.1 Baseline profile for the app's own code (rank 1)

**What.** A Baseline Profile tells ART which classes and methods to AOT-compile at install (`speed-profile`).
Startup Profiles also drive R8's DEX layout, putting startup classes in `classes.dex` [D]. Rule syntax:
`HSPLcom/pkg/Class;->method(...)`. Wildcards are allowed (`HSPLandroidx/compose/ui/layout/**->**(**)**`), and
library modules can ship `src/main/baseline-prof.txt`, which is merged into the app profile [D].

**Why it ranks first here.** Every navigation frame runs app code: `:core:navigation`, `:core:designsystem`,
feature screens, MVI ViewModels and Hilt factories. That code is interpreted until the JIT finds it hot. Screen
composition is *latency-critical but rarely executed*: 8 pushes do not make a composable hot. That matches the
evidence. Push frame-1 composition stays at 8–30 ms on all 8 pushes instead of falling after the first [M][U].

**Do this.**

1. **Bound the gain first** (10 minutes on the phone):
   ```bash
   adb shell cmd package compile -m speed -f com.example.wp04.p2.dev          # everything AOT
   tools/perf/perf.sh ... pushpop tabs tab1                                   # vs the speed-profile run
   ```
   If the push composing frame and the pop first frame drop a lot under `speed`, code compilation is the
   bottleneck and the profile fixes most of it. If they do not, the cost is structural (§2.2, §2.3).
2. Generate and commit the profile: `./gradlew :app:generateBaselineProfile` (WP04 step 23). Then run
   `split_profile.py`. The existing `BaselineProfileGenerator` already covers startup (with
   `includeInStartupProfile = true`), tabs, push, related push, Back, edge-swipe Back and fling. Add sheets
   once a sheet destination exists, and add the predictive-back card.
3. **Template-level safety net, so every generated app is covered before anyone runs the generator.** Ship
   hand-written wildcard profiles in the shell modules:
   ```text
   # core/navigation/src/main/baseline-prof.txt
   HSPLcom/base/app/core/navigation/**->**(**)**
   Lcom/base/app/core/navigation/**;
   # core/designsystem/src/main/baseline-prof.txt
   HSPLcom/base/app/core/designsystem/**->**(**)**
   Lcom/base/app/core/designsystem/**;
   ```
   The generator rewrites the package. Size cost: the AOT code for two small modules [U: measure the APK and
   odex delta]. Wildcards are coarse; the generated profile stays the primary source.
4. `perf.sh` already installs the profile through `ProfileInstallReceiver`, then runs
   `cmd package compile -f -m speed-profile`, which is the Play path. Keep the `coldstart_installed` row; it is
   the honest one (416 ms vs a 400 ms gate).

**Expected.** A large drop in push frame-1 composition and in first visits [U; the A/B in step 1 gives the
bound]. Google's published results include Android Calendar: "median janky frame rate decreased by 42–60%" in
two views [D].

### 2.2 Keep the previous screen warm *without moving it* (rank 2)

**Root cause** [S], Compose UI 1.12.1:

- Navigation 3 renders every entry through `movableContentOf`, keyed by `contentKey`
  (`SceneSetupNavEntryDecorator`, still present in 1.2.0 according to the WP04 handoff's bytecode check). During a
  pop, the incoming scene claims entries first (`NavDisplay.kt`, `sceneToExcludedEntryMap`), so the covered entry
  moves from the old scene's "under" slot to the new scene's top.
- A move between parents is `LayoutNode.removeAt → onChildRemoved → child.detach()` (`LayoutNode.kt:341–385`).
  `detach()` runs `nodes.runDetachLifecycle()`, so every `Modifier.Node.onDetach` fires, and calls
  `onLayoutNodeDetach()` on every coordinator. That calls **`releaseLayer()` — "we release the layer as the node
  might be moved to another owner"** (`NodeCoordinator.kt:1311–1317`). The node is then attached again, relaid out
  and re-recorded.
- Leaving a node **unplaced** also releases its layers:
  `MeasurePassDelegate.markSubtreeAsNotPlaced() { … it.releaseLayer() }`, with the comment "nodes are not placed
  with a layer anymore, so the layers should be released" (`MeasurePassDelegate.kt:274–286`). So `KeptTabs` and
  `KeepUnderScene`, which hide content by not measuring or placing it, throw away every RenderNode display list
  of the hidden screen. Showing it again re-records all of them.
- The same transition frame changes `LocalLifecycleOwner` (a capped owner → the entry's own owner) and
  `LocalTabVisible`. That recomposes every reader, including every `collectAsStateWithLifecycle`, which re-reads
  the owner in composition.

**Fix: park, don't hide.** Keep the stack's entries as children of **one parent that never changes**. Keep the
covered entry (N−1) attached, measured, placed with its own layer, invisible and untouchable:

```kotlin
/**
 * A covered screen stays attached, measured and placed, so its layers and recorded display lists
 * survive, but it draws nothing and takes no touches. Showing it changes two layer properties.
 */
internal fun Modifier.parked(isParked: () -> Boolean): Modifier = graphicsLayer {
    val parked = isParked()             // read in the layer block: no recomposition, no relayout
    alpha = if (parked) 0f else 1f      // HWUI skips a RenderNode whose alpha is 0
    translationX = if (parked) -2f * size.width else 0f  // off the window, so no hit test either
}
```

- HWUI skips drawing a node with alpha ≤ 0 (`RenderNode::nothingToDraw()` includes
  `properties().getAlpha() <= 0`). `RenderNodeDrawable::forceDraw` returns early on it [S]. Compose's pointer
  hit-testing does not look at alpha [U: verify in Robolectric], hence the translation. Clear semantics while
  parked (`Modifier.clearAndSetSemantics {}` toggled at settle, not on the critical frame).
- Lifecycle: hold N−1 at **STARTED**, not CREATED. Its flows stay current, and the pop does not restart
  collectors on its first frame. Hold deeper entries at CREATED. Provide `LocalLifecycleOwner` the same way for
  an entry whether it is the top or covered (one owner per entry, with its max state changed). Never swap the
  provider.
- Read `LocalTabVisible`-style flags through a `State` read in effects or draw, not as a CompositionLocal value
  read in composition.

**Structural consequence: a persistent stack scene.** As long as each push or pop produces a scene with a new
key, Navigation 3 moves entries between scenes. The clean fix is one `Scene` per single-pane stack, with a
**constant key** (the stack's identity) that renders all entries in one `Box`:
`entries.forEach { key(it.contentKey) { … } }`. Compose's `key` reordering inside one parent issues
`LayoutNode.move`, not detach (`LayoutNode.move(from, to, count)`) [S]. NavDisplay's `AnimatedContent` then sees
the same content key and runs no transition. The scene owns push, pop and predictive-back motion:

- Slides use an `Animatable` read in `graphicsLayer {}` or `placeWithLayer`.
- Predictive back: a `NavigationBackHandler` registered after the display (the `GateEffects` pattern) reads
  gesture progress in the layer block.
- On release: a spring from the release velocity. This also resolves WP04 Deviation 3: NavDisplay finishes a
  released swipe with its own `tween`.
- The Gmail-style card (requirement 24) is `scaleX/scaleY/shape/clip` on the top entry's layer.

Keep `ListDetailScene` and overlay scenes (sheets) as they are. Cap the parked depth, for example N−1 to N−3
parked and deeper entries composed but unplaced, so a coalesced rapid-back pop to the root is also warm [U:
choose from PSS].

**Do the same for `KeptTabs`.** Hidden tabs stay placed and `parked {}` instead of `layout(…) {}` with nothing
placed.

**Costs.** Retained display lists and RenderNodes are native memory, typically KBs to a few MB per screen [U].
The phase-2 PSS is 93.8 MB vs a 70 MB gate, so measure `meminfo` at depth 4 with 5 tabs. A parked screen's
invalidated content (live data) is re-measured and re-recorded while parked. That is cheap, and it stops
mattering if N−1's flows are throttled.

**Expected.** Pop first frame (UI) ≈ the cost of changing two layer properties plus the arriving screen's first
draw, so a few ms [U]. Same for tab switches (phase 2: 6.5/16.5 ms), gesture-pop starts and the rapid-back
coalesced pop (23.6/34.6 ms).

**Alternative if the persistent scene is rejected** [U]: keep `KeepUnderScene`, but stop hiding by unplacing
(use `parked {}`) and stop changing CompositionLocals. The move-induced detach on push and pop remains, so a
large share of the cost remains.

### 2.3 The push: keep the composing frame from eating the motion (rank 3)

**Root cause.** Compose animations take their start time from the first frame callback
(`SuspendAnimation.kt: animate(...)`; `startTimeNanos` is internal) [S]. In NavDisplay the sequence is:

1. The back stack changes.
2. `LaunchedEffect(scene) { transitionState.animateTo(scene) }` runs.
3. The target state is set in the next frame, so the new screen is composed **in the same frame that starts the
   clock**.
4. If that frame takes 40 ms, the next frame jumps 40 ms of spring.

Views solved this in 2016 with Choreographer's **COMMIT** phase, which moves an animator's start time to after
the first, expensive traversal (AOSP commit `c42b28d`) [S][D]. Compose has no equivalent.

**Fixes, in order of value:**

1. **Compose and record first, then start the clock.** In the persistent stack scene (§2.2), add the arriving
   entry at its start offset (off-window) in the composition that applies the push. Then start the slide from a
   `LaunchedEffect(push)`. Its first frame callback lands on the frame after the composing one.
   Choreographer skips missed vsyncs, so `frameTimeNanos` is already "now" and nothing is skipped:
   ```kotlin
   // Frame N: the push applies; the arriving entry composes, measures and records at x = width.
   LaunchedEffect(push) {
       slide.snapTo(width)
       slide.animateTo(0f, motion.slideSpring)  // first tick is frame N+1: the clock starts after the work
   }
   ```
   Cost: one frame of latency, which is invisible next to iOS's own `viewDidLoad` frame. Gain: no motion is lost
   to the heavy frame.
2. **No skeleton→content swap inside the slide.** The list already has the row. Seed the detail's initial state
   synchronously, either from the key (pass the row's display fields) or from the repository's in-memory cache
   in the ViewModel's initial state rather than after an `Appeared` event. Show a skeleton only when there is
   truly nothing in memory. Hold secondary sections ("related") until the transition settles:
   `snapshotFlow { gate.moving }.first { !it }`, or precompose them (item 4). Document this as a feature-template
   rule, because every generated feature copies `feature/sample`.
3. **Prefetch data on press-down, not composition.** On `PressInteraction.Press` for a row, warm the repository
   (`repository.item(id)`). That is safe, cheap and cancellable.
4. **Optional, advanced: precompose on press-down.** Host the stack's entries in a `SubcomposeLayout` with a
   `SubcomposeLayoutState`. On press-down, call
   `state.createPausedPrecomposition(targetKey) { Destination(targetKey) }`. Drive
   `resume { System.nanoTime() >= deadline }` between frames, with the deadline computed as
   `AndroidPrefetchScheduler` does (`frameStart + frameInterval`, posted after the frame via `view.post` and
   `Choreographer.postFrameCallback`). When `isComplete`, `apply()`, then optionally `premeasure(0, constraints)`.
   On lift, `subcompose(targetKey)` finds the slot already composed [S: `SubcomposeLayout.kt:240–330`, public and
   not experimental in 1.12.1].

   Caveats [U]:
   - The precomposed entry creates its ViewModel through the entry decorators. A cancelled tap must clear that
     store.
   - The harness's 250 ms hold flatters this. A real tap gives about 6–12 idle frames at 120 Hz.
   - The Compose compiler must emit pausable code. The `PausableComposition` feature flag is enabled by default
     from Kotlin 2.2 [D], and the project uses 2.4.20.
5. **`DeferredTransitionState.defer(target)`** (animation-core 1.12, `@ExperimentalDeferredTransitionApi`) puts
   the pending target into `DeferredAnimatedContent` in `PreEnter` state, composed and laid out. `animateTo(target)`
   then hands off with velocity [S][D]. It suits a press-down → lift or gesture-start → commit flow if the stack
   keeps `AnimatedContent` instead of the persistent scene. Navigation 3 1.2.0 does not use it.

**Expected.** Push hitches go away, and push/pop p99 moves from 57 ms toward the ≤ 25 ms gate [U].
`opening_frame` (UI ms of the composing frame) will still report the composition. Gate on hitches and present gaps
(§3) for transitions, and keep the UI-ms rows as ratchets.

### 2.4 GPU budget during transitions (rank 4)

**Facts.** Phase-2 pops are GPU-bound: issue→GPU-complete p50 14.6 ms, RenderThread issue 4.5 ms (1.1 in the
baseline), `dequeueBuffer` 2.9 ms, present latency about +1 frame [M]. Scroll and tab switches are fine. The APK
predates `d56ad13` [M].

**Do this.**

1. Rebuild `p2` at `d56ad13`+ and re-measure `pushpop` before touching anything else.
2. Separate work from clock. Adreno DVFS starts a transition from idle at a low clock. Add
   `ftrace_events: "power/gpu_frequency"` (plus `power/cpu_frequency`, `sched/sched_switch`) to the
   `app_jank.pftxt` config from the profiler skill and read the GPU frequency during the pop [U: event name per
   kernel]. GPU time per frame on a DVFS GPU is work ÷ frequency. A "6 ms GPU p90" scroll can be a low-clock
   idle state, not a cost.
3. Visual checks on the phone:
   - `adb shell setprop debug.hwui.overdraw show` (overdraw)
   - `setprop debug.hwui.show_layers_updates true` (offscreen layers redrawn per frame)
   - `setprop debug.hwui.profile visual_bars`
   - In Perfetto, look for `alpha caused saveLayer WxH` slices: HWUI traces them when alpha forces an offscreen
     buffer (`RenderNodeDrawable.cpp`) [S].
4. Budget rules for 1080×2400 at 120 Hz on Adreno 618 [U: sensible targets]:
   - one opaque fill per screen (`EntryFrame` already paints `colors.background`)
   - no `saveLayer` on anything larger than a bar; `BarAnchor.followCovered` uses a bar-sized `saveLayer` during
     cover transitions, which is acceptable
   - scrims only over the visible strip (`coverScrim` already does this)
   - shadows cached as bitmaps; Compose's `dropShadow` caches an Alpha8 blurred bitmap per size
     (`DropShadowPainter.kt`) [S], but resizing every frame (bar minimize) regenerates it [U]
   - for the Gmail card, no `shadowElevation` on a full-screen layer that scales every frame; draw an edge
     gradient like `coverScrim` [U]
5. Glass (WP05): `RenderEffect` blur and AGSL shaders render offscreen every frame. Keep the blurred region small,
   downsample, and stop rendering it when the backdrop is static.

### 2.5 First-use stalls: shaders, glyphs, textures, fonts, AGSL (rank 5)

**Facts** [S]:

- HWUI's Skia shader disk cache lives in the app's **device-protected cache dir**:
  `ActivityThread → HardwareRenderer.setupDiskCache(deviceCacheDir)`, files `com.android.skia.shaders_cache` and
  `com.android.opengl.shaders_cache` (`HardwareRenderer.java:178–179, 1191`). The `ShaderCache` is size-capped
  (4 MiB total) and validated against an identity hash that clears it on mismatch (`ShaderCache.cpp:40, 53–75`).
  So: `pm clear` wipes it, cache trimming can wipe it, and it persists otherwise [U: whether an app update changes
  the identity].
- `new RuntimeShader(sksl)` compiles the SkSL **on the calling thread at construction**
  (`RuntimeShader_createShaderBuilder → SkRuntimeEffect::MakeForShader`, `jni/Shader.cpp:250`). The GPU program
  is compiled on RenderThread at first draw.

**Do this.**

1. **Fonts.** `AppFonts` uses downloadable `GoogleFont` (async, `bestEffort`) with no preload [M]. Text first
   renders in the fallback face, then re-lays out the whole screen when the font arrives, possibly mid-transition.
   A weight first used on a pushed screen (`displaySmall`) loads during the push. Preload before the splash
   releases, with a timeout:
   ```kotlin
   // StartupViewModel, before `startup` emits: fills the process-wide typeface caches.
   val resolver = createFontFamilyResolver(context)
   withTimeoutOrNull(FONT_PRELOAD_MS) {
       resolver.preload(fonts.sans)
       resolver.preload(fonts.mono)
   }
   ```
   `FontFamily.Resolver.preload` is a public suspend function (`FontFamily.kt:71`), and the resolver uses the
   global `GlobalTypefaceRequestCache` / `GlobalAsyncTypefaceCache` (`FontFamilyResolver.kt:27–33`) [S]. Better
   still for a template whose purpose is smoothness: offer bundled font resources, with no IPC and no async swap,
   and keep downloadable fonts as an option.
2. **Idle warm-up pass after the first frame.** Once TTFD is reported:
   - precompose the tab roots (§2.6)
   - draw each warmed screen **once, for one frame, under the opaque current screen**, so Skia compiles the exact
     programs and uploads glyph runs and textures; then park it (alpha 0), which skips drawing
   - optionally render key components (bar, sheet, glass, a gradient, rounded clip) into a
     `rememberGraphicsLayer()` and call `toImageBitmap()` [U: whether this shares the window's GrContext and
     program cache]

   All of this is [U] until measured: count `issue`-stage spikes on the first visit with and without the pass.
   Do not keep anything visible at alpha < 1, which forces offscreen.
3. **AGSL and glass.** Construct each `RuntimeShader` once (remember it in a holder created off the main thread at
   startup), never per recomposition. Draw the glass once during the warm-up pass.
4. **Harness.** Keep a "cold first use" scenario (after `pm clear`) and a "warm" one (after one warm-up visit), and
   gate them separately. Today the first push of each run and `tab_first_visit` carry shader compiles.

### 2.6 Precompose tab roots (rank 6)

`KeptTabs` composes a tab on its first visit (22.7/27.6 ms first frame [M]). Use the same machinery as §2.3
item 4:

- Make the tab container a `SubcomposeLayout`.
- After TTFD, precompose unopened tab roots with `createPausedPrecomposition`, resuming only while
  `nanoTime < frameDeadline`.
- Apply, then place them parked.

Cost: ViewModels and initial loads for every tab start early. Gate this behind a size rule (≤ 5 tabs) or do it on
press-down of the tab item only [U: choose by PSS and network policy]. Expected: a first visit costs the same as a
warm switch.

### 2.7 Lazy lists: cache window and pausable prefetch (rank 7)

- Pausable composition in lazy prefetch is **on by default**:
  `ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled = true` in 1.12.1 [S], announced in Compose 1.10
  [D]. Pager cache windows too (`isCacheWindowForPagerEnabled = true`) [S].
- The default list strategy prefetches the next item, with 2 nested items
  (`LazyListPrefetchStrategy(nestedPrefetchItemCount = 2)`) [S]. A cache window prepares more ahead and keeps
  items behind:
  ```kotlin
  @OptIn(ExperimentalFoundationApi::class)
  val listState = rememberLazyListState(
      cacheWindow = LazyLayoutCacheWindow(aheadFraction = 0.5f, behindFraction = 0.5f), // or ahead = 300.dp
  )
  ```
  `LazyLayoutCacheWindow(ahead: Dp, behind: Dp)` and `LazyLayoutCacheWindow(aheadFraction, behindFraction)` are
  `@ExperimentalFoundationApi` [S].
- Apply it in `AppPagingList` (which today creates its own state with no hook; WP04 open issue) and in primary
  lists. Keep `key =` (present) and add `contentType =` where rows are heterogeneous, since prefetch time
  estimates are per content type.
- Prefetch only runs between frames (`AndroidPrefetchScheduler`: `nextFrameTimeNs = max(frameStart, drawTime) +
  frameIntervalNs`, idle mode after 2 idle frames) [S]. It therefore cannot hide work done on the frame itself:
  `onGloballyPositioned`, measurement inside composition, `SubcomposeLayout` inside rows.

### 2.8 Recomposition hygiene: deferred reads and Modifier.Node (rank 8)

This is mostly done, and `RECOMPOSITION-PLAN.md` gates it. The rules that matter for frames [D]:

- Read animated values in the phase that uses them: `Modifier.offset { }`, `layout { }`, `graphicsLayer { }` or
  `drawBehind { }` lambdas, or a `Modifier.Node`'s `measure`/`draw`. Never `Modifier.offset(x.dp)` with
  `x = anim.value` in composition.
- Prefer `Modifier.Node` (`ModifierNodeElement` + `DrawModifierNode`/`LayoutModifierNode`) over
  `Modifier.composed`. The template has no `composed {}` [M]. `CoverScrimNode` and `FollowCoveredNode` are the
  model.
- New for this project: **no CompositionLocal or lifecycle-owner changes on a transition frame** (§2.2). Every
  `collectAsStateWithLifecycle` reads `LocalLifecycleOwner` in composition, so changing the provided owner
  recomposes every screen-level collector.
- `SideEffect` with keys (1.12) is cheaper than `LaunchedEffect` for one-shot, non-suspending effects: "up to 90%
  faster than LaunchedEffect" [D].
- Strong skipping has been the default since Kotlin 2.0.20. Keep the stability config for cross-module models.

### 2.9 Compositing strategy and alpha (rank 9)

- `CompositingStrategy.Auto` + alpha < 1 renders offscreen first. `ModulateAlpha` "modulates alpha for each of the
  drawing instructions … avoids usage of an offscreen buffer". `Offscreen` always uses a buffer [S: KDoc]. HWUI does
  the same: a RenderNode with alpha < 1 that is not a layer and `hasOverlappingRendering` gets
  `saveLayerAlpha(bounds)`. Without overlap it just multiplies alpha (`RenderNodeDrawable::setViewProperties`) [S].
- Rules:
  - never alpha on a full-screen layer (already a project rule)
  - `graphicsLayer { alpha = a; compositingStrategy = CompositingStrategy.ModulateAlpha }` for single-draw content
    (icons, text, indicators)
  - `Auto` is acceptable for a short fade of a card-sized dialog or menu (`AppDialog`, `AppDropdownMenu` use
    `Modifier.alpha`)
  - `Offscreen` only for real masking (`BlendMode.DstIn` edge fades). Keep edge fades to the strip under the bar,
    not the whole list
- `Modifier.alpha(0f)` inside content (e.g. `AppButton` while loading, `AppOtpField`) is fine: alpha 0 draws
  nothing.

### 2.10 Text layout cost (rank 10)

- Compose resolves unspecified paragraph styles to **`LineBreak.Simple` and `Hyphens.None`**
  (`ParagraphStyle.kt:470–471`) [S]. These are the cheapest platform settings (`BREAK_STRATEGY_SIMPLE`,
  `HYPHENATION_FREQUENCY_NONE`). Do not set `LineBreak.Paragraph` or `Hyphens.Auto` in `AppTypography`. Use them
  only on long-form body text that is not shown during transitions.
- Do not use `TextAutoSize` in rows or on transitioning screens: it lays out several times.
- Animate text colour without relayout by passing a `ColorProducer` to `BasicText(color = { … })`.
  `AppText` should accept one for the iOS large-title collapse and tab label colours [U: check AppText's
  signature].
- Bound text with `maxLines` and `overflow` in rows. Avoid building `AnnotatedString` in composition without
  `remember`.
- First-time shaping is slower. Minikin's word cache is process-wide [U]. The warm-up pass (§2.5) and font
  preload cover most of it.

### 2.11 R8, startup profile and DEX layout (rank 11)

- AGP 9 already provides full mode (the default since AGP 8.0), optimized resource shrinking (default in 9.0),
  Kotlin null-check optimisation, and class repackaging (default since 9.1) [D]. The template sets
  `isMinifyEnabled = true` and `isShrinkResources = true` with `proguard-android-optimize.txt` [M].
- Optional on AGP 9.4.1: the new `optimization { enable = true }` DSL (9.3+), with keep rules in
  `src/<variant>/keepRules/*.keep` [D].
- Compose's runtime AAR ships `-assumenosideeffects` rules that strip `sourceInformation*` calls in optimized
  builds (`runtime.aar!/proguard.txt`) [S]. This is why debug builds are about 10× jankier and must never be
  measured.
- DEX layout: `baselineProfile { dexLayoutOptimization = true }` has been the default since AGP 8.3 when a
  startup profile exists. Startup code should fit in `classes.dex`. Startup profiles are "typically 15–30% faster"
  for startup than baseline profiles alone [D]. The generator already collects the startup journey with
  `includeInStartupProfile = true` [M].
- **Stale rule to fix** (WP02/WP04): `app/proguard-rules.pro` keeps `** implements
  com.base.app.core.navigation.AppNavKey`, but M1 moved `AppNavKey` to `com.base.app.core.common.navigation`. The
  rule now matches nothing. Check whether polymorphic restore still needs it (the `restore` scenario passed) and
  update or delete it [M].
- Run the installed `r8-analyzer` skill over the template's rules once. It ships Google's R8 configuration
  guidance.

### 2.12 Frame rate: preferredFrameRate, FrameRateCategory, ARR (rank 12)

- **The Redmi** holds 120 Hz during interactions (every frame had an 8.33 ms interval) [M]. Nothing is needed
  there.
- **On ARR phones** (Android 15 QPR1+, `Display.hasArrSupport()`), content defaults to "Normal" (~60 Hz) unless
  boosted: touch boost, flings, window transitions, "animations: movement or size changes" [D]. In Compose
  1.12.1 (`isArrEnabled = SDK_INT >= 35`) [S]:
  - A node whose **placement position changes** votes `High` (`NodeCoordinator.kt:451`). So do layer
    `move`/`resize` calls (`GraphicsLayerOwnerLayer.move/resize`). This covers `AnimatedContent` slides, which
    use `placeWithLayer`.
  - An animation that **only changes graphicsLayer properties** (translationX/Y, alpha, scale) does **not** vote.
    The layer's `frameRate` defaults to `0f`, and `voteFrameRate(0f)` is a no-op.
  - Therefore: put `Modifier.preferredFrameRate(FrameRateCategory.High)` on containers whose motion is purely
    layer-driven (a persistent-stack slide in `graphicsLayer`, sheet drag in `translationY`, pull to refresh,
    swipe actions, the predictive-back card) while they move. A vote lasts only "as long as the Composable is
    drawn" [S: KDoc], so an idle screen stops voting.
- Do not set fixed rates (`preferredFrameRate(120f)`). Categories let the platform pick.

### 2.13 GC churn (rank 13)

- ART's concurrent copying GC has one short pause that does not depend on heap size, and has been generational
  since Android 10 [D]. GC rarely causes a dropped frame directly. High allocation rates cost CPU on the same big
  cores and can force `WaitForGcToComplete` on the UI thread [U].
- Per-frame allocations seen: `CoverScrimNode.draw` builds `listOf(Color, Color)` (boxing) and a new
  `Brush.horizontalGradient`, and therefore a native `LinearGradient`, on every frame of every push and pop [M].
  Cache the gradient per size and vary only `alpha` in `drawRect(brush, alpha = …)`.
- Check with Perfetto: `HeapTaskDaemon` slices overlapping transition frames, or an allocation recording during
  `pushpop`.

### 2.14 Skia GL vs Vulkan in HWUI (rank 14, diagnostic only)

- Selection is per device. `Properties::peekRenderPipelineType()` reads `debug.hwui.renderer`, defaulting to
  `skiavk` when `use_vulkan()` is set and `skiagl` otherwise [S]. The Redmi ROM uses Skia GL [M]. Apps cannot opt
  in.
- For an A/B only: `adb shell setprop debug.hwui.renderer skiavk && adb shell am force-stop <pkg>`. Vulkan may
  change first-use costs (pipeline creation) and steady-state GPU time on Adreno 6xx [U]. Gate on what users run
  (Skia GL here).

---

## 3. Measuring it right

### 3.1 Metrics to add to `tools/perf` (in this order)

1. **Hitch ratio from presents.**
   - Within each interaction, sort `DisplayPresentTime`, drop stale rows (present < intended), and keep intervals
     ≤ 12 vsyncs.
   - `hitch_ms = Σ max(0, interval − 1 vsync)` for intervals > 1.5 vsync, and
     `ratio = hitch_ms / animation_seconds`.
   - Apple's thresholds: **good < 5 ms/s, warning 5–10, critical ≥ 10** [D].
   - My first pass over phase 2 [M][U]: pop 7.2, push 11.1, tabs 1.3, rapid back 26.5 and gesture pop 50 ms/s.
     The gesture number is probably polluted by injector cadence (§1.5).
2. **FrameTimeline jank types**, through Perfetto (`android.surfaceflinger.frametimeline`) [D]:
   - `actual_frame_timeline_slice.jank_type` (`App Deadline Missed`, `Buffer Stuffing`,
     `SurfaceFlinger CPU/GPU Deadline Missed`, `Prediction Error`, `Display HAL`)
   - `present_type` (on time, late, early, dropped)

   This is authoritative where the framestats present column is not. Use the profiler skill's `app_jank.pftxt`
   plus `power/gpu_frequency`:
   ```sql
   SELECT a.jank_type, COUNT(*) AS frames
   FROM actual_frame_timeline_slice a JOIN process p USING (upid)
   WHERE p.name = 'com.example.wp04.p2.dev'
   GROUP BY a.jank_type ORDER BY frames DESC;
   ```
3. **Stage breakdown per first frame.** `parse_framestats.phases()` exists. Also report **issue**, **GPU** and
   **dequeueBuffer**. `dequeue > 0` is the GPU-backpressure flag.
4. **Macrobenchmark `FrameTimingMetric`** in the existing `PushPopBenchmark`, `TabSwitchBenchmark` and others.
   - `frameOverrunMs` (API 31+): positive means a missed deadline.
   - `frameDurationCpuMs`.
   - Read P95 and P99 [D].
   - Add `TraceSectionMetric` around `trace("nav:push") {}` to time the composing frame.
5. **JankStats in the field** (`androidx.metrics:metrics-performance:1.0.0`) [D]:
   - `PerformanceMetricsState.getHolderForHierarchy(view).state?.putState("nav", "push:<key>")` while the gate
     is moving. Then field jank is attributed to transitions.
   - Ship it in the template behind `<opt:analytics>`.
6. **Cold vs warm first use** as separate rows (§2.5). Add a plain `input tap` tab-switch variant without the
   250 ms hold (§1.5).

### 3.2 Experiments that settle causes quickly

| Question | Experiment |
|---|---|
| Is push or pop cost compiled code or structure? | `cmd package compile -m speed -f` vs `-m speed-profile -f`, rows `pushpop tabs tab1` |
| Is the pop GPU regression real after `d56ad13`? | rebuild `p2` at HEAD, `pushpop` ×3 |
| Is it GPU clock ramp or work? | Perfetto with `power/gpu_frequency` during one pop |
| Do first visits stall on shaders? | the same run without `pm clear` (warm cache) vs with it |
| Where did the main thread run? | Perfetto `sched_switch` + `cpu_frequency`: a little core at low frequency on the first frame |

---

## 4. What top apps reach, and realistic targets

- **Google's Compose hero benchmark** (Pokedex, Pixel 3a, 60 Hz, locked clocks, R8, fully compiled): scroll jank
  **0.21 %** (1 of 485 frames), equal to Views since Compose 1.9. TTID with Compose 1.11 was 2.5 % slower than
  Views. 1.12 claims TTID "comparable to Views" [D].
- **Android Calendar** (baseline profiles): about 50 % fewer slow or frozen frames, with median janky-frame rate
  down 42–60 % in two views [D]. **Reddit** (a Compose upgrade, R8, baseline profiles): excessive slow frames down
  25 %, frozen frames down 10 % [D].
- **Android vitals**: slow frames are 16–700 ms (8 ms window at 120 fps). Frozen frames are over 700 ms [D].
  These are coarse floors, not iOS-level targets.
- **iOS**: hitch ratio < 5 ms/s is "good" [D]. Public per-transition jank rates for Gmail, Photos and similar apps
  are not published [U].

**Suggested targets for the `solid` column** on the Redmi at 120 Hz, warm, release, with the profile [U: proposals
for WP04 and WP12]:

- FrameTimeline app-deadline-missed ≤ 0.5 % on push/pop/tabs/back, and ≤ 0.3 % on scroll
- hitch ratio < 5 ms/s on every journey
- zero present gaps > 1 vsync in the first 3 frames of a transition
- warm pop or tab-switch first-frame UI ≤ 4 ms
- push composing frame ≤ 8 ms with the profile; its hitches are hidden by §2.3 item 1
- GPU p90 ≤ 7 ms with `dequeueBuffer` p90 = 0 during transitions

Keep the existing UI-ms rows as ratchets. Make hitch and FrameTimeline the gates for "smooth".

---

## 5. Rules the docs must state (input for WP12)

1. Release builds compiled with the baseline profile only. Regenerate the profile when journeys change.
   Debug numbers are meaningless.
2. Hide a covered screen or tab by **parking** it: placed, layered, alpha 0, off-window. Not by skipping its
   measure or placement, which drops every layer, and not by moving it to another parent, which detaches it.
3. Never change a CompositionLocal or the lifecycle owner of a whole screen on a transition frame.
4. The frame that composes a new screen must not be the first frame of its animation. Start the clock after it.
5. A destination's first composition is final. Seed it from what the caller already has, and never swap a
   skeleton for content mid-transition. Defer secondary sections until the transition settles.
6. No alpha on full-screen layers. Use `ModulateAlpha` for single-draw content. Use `Offscreen` only for masks,
   kept small.
7. Read animation state in layout or draw lambdas or in `Modifier.Node`. No `Modifier.composed`.
8. Preload fonts before the splash ends, or bundle them. Create AGSL shaders once.
9. Lists: stable `key`, `contentType`, and a cache window on primary lists.
10. Layer-only motion on ARR devices: `preferredFrameRate(FrameRateCategory.High)` while moving.
11. Measure with hitch ratio and FrameTimeline as well as gfxinfo janky %. Report GPU and `dequeueBuffer`.

---

## 6. Open questions and unverified points

- Whether Navigation 3 1.2.0 still moves entries between scenes exactly as 1.1.7 does. The handoff's bytecode
  check found `SceneSetupNavEntryDecorator`; the persistent-scene design does not depend on it either way.
- Pointer hit-testing on alpha-0 layers. I believe alpha is ignored, hence the off-window translation. Verify in
  Robolectric. Same for the accessibility tree.
- Whether HWUI's `prepareTree` still uploads textures for alpha-0 nodes. If not, the first unpark frame uploads
  images. That is acceptable, but measure it.
- The memory cost of parked display lists at depth 4 × 5 tabs against the 70 MB PSS gate.
- Whether `GraphicsLayer.toImageBitmap()` rendering warms the window's shader programs (shared GrContext).
- How much of the phase-2 pop GPU time is the pre-`d56ad13` draw, how much is DVFS ramp and how much is new
  content.
- Injector cadence of `input swipe` in the gesture scenarios.

---

## Sources

Official docs and blogs:

- Compose December '25 (1.10): pausable composition in lazy prefetch on by default —
  https://android-developers.googleblog.com/2025/12/whats-new-in-jetpack-compose-december.html
- Compose August '26 (1.12): TTID, `DeferredTransition`, `SideEffect` keys —
  https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html
- Compose release notes — https://developer.android.com/jetpack/androidx/releases/compose
- `SubcomposeLayoutState` / `PausedPrecomposition` —
  https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/SubcomposeLayoutState and
  https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/SubcomposeLayoutState.PausedPrecomposition
- Lazy layout package (`LazyLayoutCacheWindow`) —
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/layout/package-summary
- Compose compiler `PausableComposition` flag —
  https://kotlinlang.org/api/kotlin-gradle-plugin/compose-compiler-gradle-plugin/org.jetbrains.kotlin.compose.compiler.gradle/-compose-feature-flag/-companion/-pausable-composition.html
- Hero benchmarks — https://developer.android.com/develop/ui/compose/performance/herobenchmark
- Compose performance best practices (deferred reads) —
  https://developer.android.com/develop/ui/compose/performance/bestpractices
- Compose phases — https://developer.android.com/develop/ui/compose/phases
- Custom modifiers (`Modifier.Node`) — https://developer.android.com/develop/ui/compose/custom-modifiers
- `CompositingStrategy` — https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/CompositingStrategy
- Baseline profiles — https://developer.android.com/topic/performance/baselineprofiles/overview and
  https://developer.android.com/topic/performance/baselineprofiles/manually-create-measure
- Startup profiles and DEX layout — https://developer.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
- R8 app optimization —
  https://developer.android.com/topic/performance/app-optimization/enable-app-optimization (also in the installed
  `android-skills` `r8-analyzer` references)
- Android Calendar case study —
  https://developer.android.com/topic/performance/baselineprofiles/case-study-android-calendar
- Reddit case study —
  https://android-developers.googleblog.com/2024/12/reddit-improved-app-startup-speed-using-baseline-profiles-r8.html
- Adaptive refresh rate — https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate
- Macrobenchmark metrics — https://developer.android.com/topic/performance/benchmarking/macrobenchmark-metrics
- JankStats — https://developer.android.com/topic/performance/jankstats
- Slow rendering (vitals) — https://developer.android.com/topic/performance/vitals/render
- Perfetto FrameTimeline — https://perfetto.dev/docs/data-sources/frametimeline
- ART GC — https://source.android.com/docs/core/runtime/gc-debug
- Apple, "Eliminate animation hitches with XCTest" (WWDC20 10077) —
  https://developer.apple.com/videos/play/wwdc2020/10077/

AOSP (`android.googlesource.com/platform/frameworks/base/+/refs/heads/main/…`):

- `libs/hwui/pipeline/skia/RenderNodeDrawable.cpp`: `forceDraw`, `setViewProperties`, saveLayer for alpha
- `libs/hwui/RenderNode.h`: `nothingToDraw()`
- `libs/hwui/pipeline/skia/ShaderCache.cpp`
- `graphics/java/android/graphics/HardwareRenderer.java`: `setupDiskCache`
- `core/java/android/app/ActivityThread.java`: shader cache in the device-protected cache dir
- `graphics/java/android/graphics/RuntimeShader.java` and `libs/hwui/jni/Shader.cpp`: SkSL compiled at
  construction
- `libs/hwui/Properties.cpp`: renderer selection
- Commit `c42b28d`, the Choreographer COMMIT phase that corrects an animator's start time:
  https://android.googlesource.com/platform/frameworks/base/+/c42b28d

androidx sources read locally (`~/.gradle/caches/modules-2/files-2.1/…-sources.jar`). Compose 1.12.1:

- `ui/layout/SubcomposeLayout.kt` (precompose, `createPausedPrecomposition`)
- `ui/node/LayoutNode.kt` (`removeAt`, `detach`, `move`)
- `ui/node/NodeCoordinator.kt` (`onLayoutNodeDetach` → `releaseLayer`; votes High on position change)
- `ui/node/MeasurePassDelegate.kt` (`markSubtreeAsNotPlaced` → `releaseLayer`)
- `ui/FrameRate.kt`, `ui/platform/AndroidComposeView.android.kt` (`isArrEnabled`),
  `ui/platform/GraphicsLayerOwnerLayer.android.kt`
- `foundation/ComposeFoundationFlags.kt`, `foundation/lazy/LazyListState.kt`,
  `foundation/lazy/LazyListPrefetchStrategy.kt`, `foundation/lazy/layout/LazyLayoutCacheWindow.kt`,
  `foundation/lazy/layout/PrefetchScheduler.android.kt`
- `animation-core/Transition.kt` (`DeferredTransitionState`), `animation-core/SuspendAnimation.kt`
- `animation/AnimatedContent.kt` (`DeferredAnimatedContent`), `animation/EnterExitTransition.kt`
  (`placeWithLayer`, clip layer)
- `ui-text/ParagraphStyle.kt` (text defaults), `ui-text/font/FontFamily.kt` (`preload`),
  `ui-graphics/shadow/DropShadowPainter.kt`
- `runtime.aar!/proguard.txt`

Navigation 3 1.1.7: `scene/SceneSetupNavEntryDecorator.kt`, `ui/NavDisplay.kt`.

Installed Google skills: `profilers/android-profiler` (`app_jank.pftxt`, `hints_graphics.md`) and
`performance/r8-analyzer`.

Project data: `perf/WP04/{baseline,phase2}/run*/*.txt` (framestats), `handoff/WP04.md`, `wt/WP04` at `d56ad13`,
and `out/WP04/p2` (APK and merged ART profile).
