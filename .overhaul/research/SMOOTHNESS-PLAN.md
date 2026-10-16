# Smoothness plan: techniques ranked against the measured weak spots

Written 2026-10-09 for REQUIREMENTS 19, 20, 21, 22, 24 and 28. Sources: `research/compose-perf.md` (cause analysis and
techniques), `research/ios-motion.md` (motion designs), `research/recomposition.md` (the harness), the phone data in
`perf/WP04/baseline/summary.md` and `perf/WP04/phase2/summary.md`, and the code as of today:

- WP04 tree `wt/WP04/templates/android` at `d56ad13`: shell, navigation, bar, perf harness. File paths below are
  relative to `core/navigation/src/main/kotlin/com/base/app/core/navigation/` unless they say otherwise.
  `AppTabBar.kt` means WP04's bar at `core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/navigation/AppTabBar.kt`.
- WP03 tree `wt/WP03/templates/android` at `69837f4`: the design system. Paths are relative to
  `core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`.
- The integration branch (`android-base/templates/android`, = `wt/WP02` at `709612d`): features, build-logic.

Status: **APPLIED** (in the code, with file:line), **PARTIAL** (some of it is there), **MISSING**. Evidence tags as in
the research: [S] source, [M] measured, [D] derived, [U] unverified, so measure it first. Companion plans:
`MOTION-PLAN.md` (every animation), `RECOMPOSITION-PLAN.md` (the harness and gates), and `predictive-back/SPEC.md`.

---

## 1. The weak spots (Redmi Note 10 Pro, 120 Hz, phase 2 vs the `solid` gates)

| Row | Baseline (old template) | Phase 2 (new shell) | Gate | Verdict |
|---|---|---|---|---|
| Pop, first frame median / max (ms) | 19.1 / 27.6 | **15.0 / 36.9** | 8 / 16 | fail |
| Push/pop (Back key) janky % / p99 (ms) | 4.03 / 34 | **2.34 / 57** | 2.0 / 25 | fail |
| Push/pop (gesture) janky % / p99 | 3.54 / 36 | **2.82** / 42 | 2.0 / — | fail |
| Push, composing frame median / max | 8.35 / 9.98 | **19.95 / 48.61** | ratchet (target 12 / 20) | regressed |
| Rapid Back, coalesced pop slowest frame median / max | — | **23.6 / 34.6** | 8 / 16 | fail |
| Tab switch janky % | 1.58 | **1.9** | 1.0 | fail |
| Tab switch to a visited tab, first frame median / max | 19.0 / 45.3 | 6.51 / **16.45** | 8 / 16 | max fails |
| First visit to a tab, first frame median / max | 19.4 / 25.5 | 22.7 / 27.6 | ratchet | — |
| Navigation stress janky % | — | **5.26** | 4.0 | fail |
| Cold start as installed (ms) / PSS (MB) | — / 109.8 | **416 / 93.8** | 400 / 70 | fail (secondary here) |
| Scroll root, scroll with minimize | 0.22 % | 0.15 % / 0.3 % | 0.5 % | pass |

Stage analysis of the dumps (compose-perf §1):

- **Pops are composition-, layout- and GPU-bound.** anim p99 12.4 ms; issue→GPU p50 **14.6 ms** against 6.5 in the
  baseline; `dequeueBuffer` p50 2.9 ms.
- **Push frame 1** composes for 8–30 ms and records for 7–20 ms. Frames 2–3 carry the skeleton → content → related
  swaps.
- **The phase-2 APK (08:13) predates `d56ad13` (08:54).** That commit removed a hidden full-screen draw on every pop
  frame, so the pop GPU numbers are stale.
- **The merged ART profile has 0 rules for app code.**

**What the ranks mean.** The rank is the expected effect on the rows above: how many failing rows a technique moves, and
by how much. Rows marked [U] are bounded by the experiments in S0 before anyone builds on them.

---

## 2. Ranked techniques (summary)

| # | Technique | Rows it moves | Status | Owner |
|---|---|---|---|---|
| S0 | Measure what the user sees: rebuild at HEAD, `-m speed` A/B, hitch ratio, FrameTimeline, GPU frequency, `dequeue`, warm vs cold, plain-tap variant | all: tells S1–S6 apart | PARTIAL | WP04 |
| S1 | **Persistent stack scene**: one scene per stack with a constant key; covered entries parked (attached, placed, alpha 0, off-window), never moved, never unplaced; no provider swaps | pop first frame, rapid-back coalesced pop, gesture start, push composing frame, push/pop p99, nav_stress | PARTIAL | WP04 |
| S2 | **Baseline profile for the app's own code**, plus wildcard profiles for the shell and design system | push composing frame, first visit, pop, cold start | MISSING | WP04 |
| S3 | **The frame that composes a screen is not the first frame of its animation**; a destination's first composition is final; secondary sections wait for the settle | push/pop p99, push hitches, nav_stress | MISSING | WP04 (clock), WP02-M3 (features), WP06 (cache) |
| S4 | **Kept tabs parked, not unplaced**; no lifecycle or local flips on the switch frame | tab-switch janky, visited first frame max | PARTIAL | WP04 |
| S5 | **GPU budget during transitions**: re-measure after `d56ad13`, no full-screen offscreen layers, cached gradients | pop GPU and latency, p99 | PARTIAL | WP04, WP03 |
| S6 | **First-use stalls paid while idle**: font preload, shader/glyph warm-up draw, AGSL created once | first visit, first push | PARTIAL | WP03, WP04, WP05 |
| S7 | **Precompose tab roots** in idle frames (paused precomposition) | first visit | MISSING | WP04 |
| S8 | **Overlays fade without a full-screen offscreen layer** (alert, action sheet, sheet scrims) | future sheet/alert journeys, GPU p90 | MISSING | WP03 |
| S9 | **No per-frame allocations** in transition draw code | p99 tails (GC on the UI thread) | MISSING | WP04 |
| S10 | **Frame-rate votes for layer-only motion** (ARR devices) | none on the Redmi; other phones | MISSING | WP04, WP03 |
| S11 | **Lazy lists**: `contentType`, cache window on primary lists, hoisted state for `AppPagingList` | protects scroll (passes) | PARTIAL | WP03, WP02-M3 |
| S12 | **R8 and the startup profile**: fix the stale keep rule, run the R8 analyzer | cold start as installed | PARTIAL | WP02-M2, WP04 |
| S13 | **Velocity hand-off**: every gesture ends on a spring seeded with the finger's velocity (MOTION-PLAN) | perceived smoothness, no visible jump on release | PARTIAL | WP04, WP03 |
| S14 | **Recomposition hygiene and its gates** (RECOMPOSITION-PLAN) | first frames, per-frame recompositions | PARTIAL | WP02-M2, WP03, WP04 |
| S15 | **Glass under its own budget** (one backdrop, lens only while touched, no alpha over a backdrop) | glass column | MISSING (not started) | WP05 |
| S16 | **Field and dev tooling that costs nothing per frame** (JankStats state tags, devtools badge in layout) | field attribution | MISSING | WP06 |

---

## 3. The techniques in detail

### S0. Measure what the user sees (prerequisite; WP04, `tools/perf/`)

| Item | Status | Change |
|---|---|---|
| Phase-2 numbers predate `d56ad13` | **MISSING** re-measure | `$O/p2-rebuild.sh` at HEAD, then `perf.sh … --runs 3 pushpop tabs tab1 rapid_back nav_stress pushpop_gesture`. Do this before attributing any pop GPU time |
| Code vs structure A/B | **MISSING** | the same scenarios after `adb shell cmd package compile -m speed -f <pkg>` vs `-m speed-profile -f`. A large drop under `speed` means S2 fixes most of it; a small drop means S1/S3 are the lever (compose-perf §2.1) |
| Only gfxinfo janky % | **PARTIAL**: `parse_framestats.py` exists; no present-gap metric | add `hitch_ms_per_s` from `DisplayPresentTime` (drop stale rows, gaps > 1.5 vsync; Apple: good < 5 ms/s) and per-transition **issue / GPU / dequeue** for the first three frames (compose-perf §3.1 items 1, 3) |
| FrameTimeline jank types, GPU frequency | **MISSING** | a `trace` scenario: Perfetto with `android.surfaceflinger.frametimeline`, `power/gpu_frequency`, `power/cpu_frequency`, `sched/sched_switch` (the profiler skill's `app_jank.pftxt` plus those events). It runs separately from the timing runs. The SQL is in compose-perf §3.1 |
| Cold vs warm first use | **MISSING**: `pm clear` runs before every run (`tools/perf/perf.py:238`), which wipes the HWUI shader cache | report `tab_first_visit` and the first push twice: after `pm clear` (cold) and after one warm-up visit (warm). Gate the warm rows; ratchet the cold ones |
| Honest taps | **PARTIAL**: taps are measured from the lift (`perf.py:50` `PRESS_SETTLE_S = 0.25`) | add `tabs_tap`, a plain `input tap` variant. The 250 ms hold flatters any press-down precompose (S7) |
| Gesture start | **MISSING** | a `gesture_first_frame` row: the slowest of the first three frames after the edge-swipe DOWN (SPEC §6) |
| Composition per transition | **MISSING** | `perf.sh recompose` (RECOMPOSITION-PLAN §4): frames containing `Compose:recompose` per transition, run outside the timing runs |

Proposed `solid` targets once S0 exists (compose-perf §4, [U] until calibrated): FrameTimeline app-deadline-missed ≤
0.5 % on push/pop/tabs/back and ≤ 0.3 % on scroll; hitch ratio < 5 ms/s on every journey; no present gap > 1 vsync in
the first 3 frames of a transition; warm pop and tab-switch first frame ≤ 4 ms; push composing frame ≤ 8 ms; GPU p90
≤ 7 ms with `dequeue` p90 = 0 during transitions. The existing UI-ms rows stay as ratchets.

### S1. Persistent stack scene with parked entries (WP04; the largest structural lever)

**Cause** [S], from compose-perf §2.2:

- `KeepUnderScene` is keyed by the top entry (`KeepUnderScene.kt:48` `key = top.contentKey`), so every push and every
  pop produces a scene with a new key.
- Navigation 3 then moves the covered entry's movable content into another scene. A move between parents is
  `LayoutNode.removeAt → detach()`: every `Modifier.Node.onDetach` runs and every coordinator calls `releaseLayer()`.
  The entry is then attached again, re-measured and re-recorded.
- While the entry sits under the top, `KeepUnderScene` neither measures nor places it (`KeepUnderScene.kt:84`),
  and unplacing also releases layers (`markSubtreeAsNotPlaced`).
- The same frame swaps `LocalLifecycleOwner` from the capped owner (`KeepUnderScene.kt:79`) to the entry's own
  owner (`top.Content()` at `:98`) and flips `LocalTabVisible` (`:80`). Every `collectAsStateWithLifecycle` reader
  recomposes.

That is the pop's first frame: anim 7–19 ms plus record 7–17 ms. **[D] It also explains the push regression** (8.4 /
10.0 → 20.0 / 48.6 ms): on a push the covered screen moves from the old scene's top slot into the new scene's under
slot, so frame 1 composes the new screen *and* re-attaches and re-lays out the old one. Confirm this with S0's
`speed` A/B and a composition trace.

**Status: PARTIAL.**

- APPLIED: N−1 stays composed (`KeepUnderScene.kt:72-96`), and `d56ad13` stopped laying it out except during a push.
  `KeepUnderTest` proves that the composition survives a pop.
- MISSING: no detach, no unplace, no provider swap, and parked depth beyond N−1. Rapid Back coalesces to N−k, which
  is not composed at all today.

**Change.** Replace `KeepUnderSceneStrategy` for single-pane stacks with a `StackSceneStrategy` whose scene key is the
stack, so it is constant. All kept entries render as children of one `Box` that never changes:
`entries.forEach { key(it.contentKey) { … } }`. Reordering inside one parent is `LayoutNode.move`, not a detach. Keep
`ListDetailSceneStrategy` first in the list (`StackHost.kt:94-99`). A fold or unfold between list-detail and the stack
scene may still move entries; that is rare and acceptable.

```kotlin
/**
 * One scene per single-pane stack. Its key is the stack, so Navigation 3 never moves an entry between scenes, which
 * would detach it and drop its layers. Covered entries stay attached, measured and placed, and draw nothing until
 * they are needed again. The scene moves its entries itself: push, pop, rapid Back and the back card.
 */
internal class StackScene(
    private val stack: AppBackStack,
    private val motion: StackMotion,
    override val entries: List<NavEntry<AppNavKey>>,          // parked depth + top, plus a popped top still leaving
    override val previousEntries: List<NavEntry<AppNavKey>>,
) : Scene<AppNavKey> {
    override val key: Any = stack
    override val content: @Composable () -> Unit = {
        // The first tick lands on the frame after the one that composed the arriving screen (S3).
        LaunchedEffect(motion.changeId) { motion.run(stack.gate) }
        Box(Modifier.fillMaxSize().then(if (motion.isMoving) HighFrameRate else Modifier)) {
            entries.forEach { entry ->
                key(entry.contentKey) {
                    EntryLifecycle(motion, entry.contentKey) {      // one owner per entry; only its state changes
                        Box(Modifier.stackEntry(motion, entry.contentKey)) { entry.Content() }
                    }
                }
            }
        }
    }
    // equals/hashCode over stack identity, entries and previousEntries (as KeepUnderScene does today).
}

/** Pose of one entry: in place, sliding, the back card, or parked. Read in the layer and draw blocks only. */
internal fun Modifier.stackEntry(motion: StackMotion, key: Any): Modifier = this
    .graphicsLayer { motion.applyPose(key, this) }       // parked: alpha = 0, translationX = −2 × width
    .drawWithContent { drawContent(); motion.drawOver(key, this) }   // cover scrim / back scrim, as CoverScrim does

private val HighFrameRate = Modifier.preferredFrameRate(FrameRateCategory.High)   // S10
```

`StackMotion` is one `@Stable` holder per host:

- **Roles**: `top`, `beneath` (N−1), `leaving` (a popped top still sliding out) and `parked`. They are snapshot
  state, read only in `applyPose` and `drawOver`.
- **One `slide` float** with its velocity. It is driven by `animate(from, to, velocity, springs.screen.unit)`, so a
  retarget keeps its speed.
- **The `BackCardState`** from SPEC §4.4.
- **`prepare(change)`**, called where the stack changes (`AppBackStack.submit`/`gate.changed`, `StackHost.kt:62-69`),
  outside composition. It sets the pose the first composed frame must show:
  - push: the arriving top at `slide = 1` (off-window);
  - pop: `leaving = top`, `beneath` unparked at the parallax position;
  - `GesturePop`: no slide; the top is removed at once.
- **`run()`** animates `slide`, reports `moving` to the gate with the existing 99 % travel rule (`SceneMotion.kt:34-45`,
  `BackGate.kt:94`), and at the end parks the new N−1 or drops `leaving`.
- **The parallax and the cover scrim** move into `applyPose`/`drawOver` (today they are `KeepUnderScene.kt:85-93`
  and `CoverScrim.kt:57-80`).
- **Lifecycle**: the top RESUMED, N−1 **STARTED** (its flows stay current, so a pop restarts no collector), deeper
  entries CREATED. Each entry gets one `CappedLifecycle` (`KeptTabs.kt:114-132`) created once per content key, so the
  provided value never changes.
- **Parked entries** clear their semantics and refuse focus through a small `SemanticsModifierNode` that calls
  `invalidateSemantics()` when the role changes. That costs no recomposition. Off-window translation keeps them out
  of hit testing [U: verify in Robolectric].
- **Parked depth** `PARKED_DEPTH = 3` (N−1…N−3). Deeper entries are not in the scene (state saved by the saveable
  decorator) [U: choose from PSS at depth 4 × 5 tabs].

**Spike first (half a day, Robolectric, `core/navigation/src/test`)**. Prove three properties before rewriting
`StackHost`:

1. A probe `Modifier.Node` in the covered entry counts `onDetach` = 0 across push, pop, a coalesced pop and the back
   card.
2. A popped entry stays composed until its slide ends. Its ViewModel is cleared exactly once afterwards, because the
   decorators' `onPop` runs on disposal [U for 1.2.0; check `rememberDecoratedNavEntries` in the AAR].
3. `NavDisplay` runs no `AnimatedContent` transition between two `StackScene`s with the same key, and the content
   shows the new scene's entries. AnimatedContent swaps the state of a same-key child [U].

If (2) or (3) fails inside `NavDisplay`, use the fallback: `StackHost` renders single-pane stacks itself
(`rememberDecoratedNavEntries` plus the same `Box`) and uses `NavDisplay` only for the list-detail scene.
`EntryFrame`'s `LocalNavAnimatedContentScope` reads (`StackHost.kt:183`) and `ReportMotion` switch to `StackMotion`
in both designs.

**Expected** [U]: pop first frame ≈ two layer-property changes plus the arriving screen's draw (a few ms). The
rapid-back coalesced pop is the same when the target is within the parked depth. The push composing frame comes back
to the baseline's 8–10 ms, and S2 lowers it further. The gesture start costs nothing (SPEC §4.3). PSS rises by the
retained display lists, so measure it.

**Verify**:

- `perf.sh pushpop pushpop_gesture rapid_back nav_stress meminfo`: pop first frame ≤ 8 / 16 ms, coalesced pop ≤
  8 / 16 ms, push/pop p99 ≤ 25 ms, PSS delta reported.
- RECOMPOSITION-PLAN "Pop": the covered entry's screen executes 0 times.
- `KeepUnderTest`, `RapidBackTest`, `SceneChangeTest` and `RestoreTest` adapted and green.

### S2. Baseline profile for app code (WP04 generates; template safety net)

**Status: MISSING.**

- `out/WP04/p2/app/build/intermediates/merged_art_profile/…/baseline-prof.txt` has 5,169 lines and 0 app rules [M].
- No `src/main/baseline-prof.txt` exists in any template module (`find templates/android -name 'baseline-prof*.txt'`
  is empty).
- APPLIED: `BaselineProfileGenerator` and the journeys exist (`benchmark/src/main/kotlin/com/base/app/benchmark/`),
  with `includeInStartupProfile = true`, and `app/build.gradle.kts` sets `baselineProfile { mergeIntoMain = true;
  saveInSrc = true }`.

**Change**:

1. Run S0's `speed` A/B first.
2. `./gradlew :app:generateBaselineProfile` on the phone, then split the result per module with
   `tools/perf/split_profile.py` into `src/main/baseline-prof.txt` of `:core:navigation`, `:core:designsystem`,
   `:core:ui` and `:app` (MASTER-PLAN A5.1 `performance` row).
3. **Template safety net**, so a generated app is covered before anyone regenerates: hand-written wildcard rules at
   the top of `core/navigation/src/main/baseline-prof.txt` and `core/designsystem/src/main/baseline-prof.txt`:
   ```text
   HSPLcom/base/app/core/navigation/**->**(**)**
   Lcom/base/app/core/navigation/**;
   ```
   Do the same for `designsystem`. The generator's rename rewrites the package. Measure the APK and odex delta.
4. Add the profile journeys this plan creates: the back card (slow `input motionevent` swipe), the tab reveal, a
   sheet once a sheet destination exists, and pull to refresh.

**Expected**: a large drop in the push composing frame and first visits if the A/B confirms it [U]. Calendar's case
study reports median janky frames down 42–60 % [D].

**Verify**: `perf.sh pushpop tab1 tabs coldstart_installed`. Check that the merged profile contains
`com/base/app/core/navigation` rules.

### S3. Keep the composing frame out of the animation (WP04 clock; WP02-M3 features; WP06 cache)

**Cause** [S]: Compose takes an animation's start time from its first frame callback. In `NavDisplay` the target is
set in the frame that composes the new screen, so a 40 ms composing frame skips 40 ms of spring. Views fixed this with
the Choreographer COMMIT phase; Compose has no equivalent. Then the sample detail screen swaps content inside the
slide:

- it starts `Idle` (`feature/sample/.../detail/SampleDetailViewModel.kt:19`);
- it loads on `Appeared` (`SampleDetailRoute.kt:18`, `LaunchedEffect`);
- it shows `Loading`, then `Success` (`SampleDetailViewModel.kt:36-40`);
- then the related rows arrive (`:48`).

That is three compositions inside the slide.

**Status: MISSING** in all three places.

**Change**:

1. **Clock after composition (WP04).** In S1's design, `prepare(push)` places the arriving entry at `slide = 1`
   before it composes. `run()` starts in `LaunchedEffect(changeId)`, so its first tick is frame N+1 and the heavy
   frame N shows nothing moving. Cost: one frame of latency, invisible next to iOS's own `viewDidLoad` frame.
2. **A destination's first composition is final (WP02-M3, every feature template).**
   - `SampleDetailKey(itemId)` (`feature/sample/.../api/SampleKeys.kt:14`) seeds the ViewModel's initial state from
     the repository's in-memory cache, synchronously in the constructor:
     `MviViewModel(initialState = repository.peek(itemId)?.let { SampleDetailState(item = it, loadState = Success) } ?: SampleDetailState())`.
   - It refreshes only if the cached value is stale.
   - A skeleton shows only when nothing is in memory.
   - "Related" rows load after the transition settles: the route waits on
     `snapshotFlow { gate.moving }.first { !it }`, exposed to features as a tiny `LocalNavigationSettled: () -> Boolean`
     provider from `:core:navigation`.
   - Write this into the feature template rules (WP12 documents it).
3. **Synchronous cache read (WP06).** Repositories with an in-memory cache expose `fun peek(id): T?` (non-suspending,
   main-safe, no I/O) next to the suspending `item(id)`.
4. **Prefetch on press-down (WP02-M3, optional).** A row's `PressInteraction.Press` warms `repository.item(id)`. It is
   cancellable and cheap.
5. **Optional, later (WP04):** precompose the destination on press-down with
   `SubcomposeLayoutState.createPausedPrecomposition` (public in ui 1.12.1, `SubcomposeLayout.kt:254`). This needs S1's
   single parent. Do it only if S0 shows the push composing frame is still over 8 ms after S2 and S3.1–3.

**Expected** [U]: push hitches disappear from the motion, and push/pop p99 moves from 57 ms toward ≤ 25 ms. Gate
transitions on hitches and present gaps (S0); keep "push composing frame" as a ratchet.

**Verify**: `perf.sh pushpop` hitch ratio < 5 ms/s. In a `record.sh push` strip, the first moved frame is at the
spring's t = 1 frame. RECOMPOSITION-PLAN "Push": the destination executes ≤ 1 time per composable, with no recomposition
in frames 2…N−1.

### S4. Kept tabs: park, don't unplace; no flips on the switch frame (WP04)

**Cause** [S][M]:

- A hidden tab is neither measured nor placed (`KeptTabs.kt:61-65`), so every display list it had is dropped.
- A switch to a visited tab therefore re-measures and re-records the whole screen: visited first frame median
  6.5 ms, max 16.45 ms. The janky frames are those first frames at 120 Hz.
- The same frame flips `LocalTabVisible` (`KeptTabs.kt:67`).
- It also raises the tab's lifecycle from CREATED to RESUMED (`KeptTabs.kt:57`, `:106-109`). Every
  `collectAsStateWithLifecycle` in the tab restarts collection on that frame.

**Status: PARTIAL.**

- APPLIED: tabs are kept (no rebuild); the owner instance is stable (`rememberTabLifecycle`, `KeptTabs.kt:95-111`);
  the dispatcher is disabled per tab (`:76-88`).
- MISSING: parking, and deferring the lifecycle and visibility flips.

**Change**:

1. Hidden tabs stay measured and placed with `Modifier.parked { index != selected }` (alpha 0, off-window,
   semantics cleared through a node; compose-perf §2.2).
2. Raise a tab's lifecycle **after** its first frame is drawn (`withFrameNanos` twice in a `LaunchedEffect`), and
   lower the leaving tab's at once. Collectors then restart one frame later, out of the switch frame.
3. `LocalTabVisible` becomes a static local holding a stable `TabVisibility` holder with a snapshot field.
   `AppSkeleton` and `AppActivityIndicator` read it in their draw or effect, so the switch recomposes nothing
   (RECOMPOSITION-PLAN R3).
4. The tab reveal (MOTION-PLAN M24) is drawn on the incoming tab only, in the layout and draw phases.

**Expected** [U]: visited first frame ≈ 1–3 ms, tab-switch janky toward 0 %. PSS rises by the parked tabs' display
lists (5 tabs × one screen).

**Verify**: `perf.sh tabs tabs_tap tab1 meminfo idle` (idle must stay 0 frames: a parked tab must not draw).
RECOMPOSITION-PLAN "Tab switch".

### S5. GPU budget during transitions (WP04 for the shell, WP03 for components)

| Item | Status | Change |
|---|---|---|
| Hidden full-screen draw on pops | **APPLIED** `d56ad13` (`KeepUnderScene.kt:76-84`), not yet measured on the phone | S0 re-measure |
| GPU frequency vs work | **MISSING** | S0 `trace` scenario: GPU time on a DVFS GPU is work ÷ frequency |
| One opaque fill per screen | **APPLIED**: `EntryFrame` paints `colors.background` (`StackHost.kt:192-195`) | keep. Screens must not add a second full-screen background |
| Bar-sized `saveLayer` while covered | **APPLIED** acceptable: `BarAnchor.kt:114` | keep it bar-sized |
| Overdraw and offscreen checks | **MISSING** as a routine | `setprop debug.hwui.overdraw show`, `debug.hwui.show_layers_updates true`, Perfetto `alpha caused saveLayer` slices, on every transition the plans add |
| Back-card scrim | new | one blended rect; four bands if GPU p90 exceeds 8 ms (SPEC §6) |
| Bottom edge protection (QA-02) | **MISSING** | a gradient rect in `ShellChrome` between content and chrome (`AppShell.kt:178-187`), not a `DstIn` mask and not a blur (MOTION-PLAN M27) |
| Full-screen offscreen fades in overlays | **MISSING** | S8 |
| `dropShadow` on a resizing bar | **PARTIAL**: cached per size, but minimize resizes it every frame [U] | measure `scroll_minimize` GPU p99 (7 ms today). If it rises, draw the shadow at the expanded size and scale the layer |

### S6. First-use stalls paid while idle (WP03 fonts, WP04 warm-up, WP05 shaders)

| Item | Status | Change |
|---|---|---|
| Downloadable fonts swap mid-transition | **PARTIAL**: WP03 built `FontPreload.run(resolver, budget = 150 ms)` (`theme/AppFonts.kt:57-75`) and `AppTheme` draws with its answer; `StartupViewModel` does not call it (WP03 remaining work 4) | wire it in `app/.../ui/StartupViewModel` inside `<opt:googlefonts>`, before `startup` emits (WP03) |
| Shader, glyph and texture first use (`issue` 43–45 ms on 2 of 4 first visits after `pm clear`) | **MISSING** | after TTFD, a warm-up pass (WP04): precompose the tab roots (S7), draw each warmed screen once under the opaque current screen for one frame, then park it. Count `issue` spikes with and without the pass [U] |
| AGSL `RuntimeShader` compiles SkSL at construction on the calling thread | **MISSING** (glass not started) | WP05: construct once, off the main thread at startup, held in a holder; draw the glass once during the warm-up |
| Measured cold vs warm | **MISSING** | S0 |

### S7. Precompose tab roots (WP04)

**Status: MISSING.** Tabs compose on their first visit (`KeptTabs.kt:49-50`): 22.7 / 27.6 ms on the first frame.

**Change**: make the tab container a `SubcomposeLayout` with a `SubcomposeLayoutState`. After TTFD, call
`createPausedPrecomposition(index) { content(index) }` for unopened tabs. Resume it only while
`System.nanoTime() < frameDeadline`, posted after each frame the way `AndroidPrefetchScheduler` does. Apply it, then
place the tab parked (S4). Limit this to ≤ 5 tabs, or do it on the tab item's press-down only [U: choose by PSS and by
the network calls of a tab's initial load]. A precomposed tab's ViewModel starts early, so its initial load must be
lifecycle-gated (it is: CREATED cap).

**Expected**: a first visit costs what a warm switch costs. **Verify**: `perf.sh tab1 tabs_tap meminfo`.

### S8. Overlays fade without a full-screen offscreen layer (WP03)

**Cause** [S]: `graphicsLayer { alpha }` with the default `CompositingStrategy.Auto` and alpha < 1 renders the layer
into an offscreen buffer every frame (compose-perf §2.9).

**Status: MISSING** in three places:

- `component/overlay/AppAlert.kt:79-82`: the full-screen box *containing the alert card* fades with layer alpha, so the
  whole screen plus the card goes offscreen for the 0.34 s fade.
- `component/overlay/AppActionSheet.kt:86-90`: the full-screen scrim box.
- `component/overlay/AppSheet.kt:312-317`: the full-screen scrim box.

**Change**: draw scrims as `drawBehind { drawRect(colors.scrim, alpha = shown.value) }` with no layer alpha. Fade the
alert card on its own card-sized layer (`Auto` is acceptable for a card; `ModulateAlpha` if it has a single
background). See MOTION-PLAN M16/M17 for the springs.

**Verify**: Perfetto shows no `alpha caused saveLayer` slice larger than a card while an alert opens. The catalog
"Feel" page recording shows no visual change.

### S9. No per-frame allocations in transition draw code (WP04)

| Where | Status | Change |
|---|---|---|
| `CoverScrim.kt:68-76` builds `Color.copy`, `listOf(…)` (boxing) and a new `Brush.horizontalGradient` (a native `LinearGradient`) every frame of every push and pop | **MISSING** | cache the gradient per size in the node (`onRemeasured`/size check); vary only `drawRect(brush, alpha = …)` |
| `AppTabBar.kt:457` (WP04) allocates `ColorFilter.tint(tint())` per frame per tab while the pill moves | **MISSING** (minor) | it goes away with the masked two-layer label render (MOTION-PLAN M24, QA-04): no per-tab tint |
| Back-card shapes | new | the per-px cache (SPEC §4.4) |

### S10. Frame-rate votes for layer-only motion (WP04, WP03)

**Status: MISSING** (`preferredFrameRate` is not used anywhere).

The Redmi held 120 Hz throughout [M], so this costs nothing there. On ARR phones (Android 15 QPR1+), Compose 1.12.1
votes `High` only for placement changes. Pure `graphicsLayer` motion casts no vote (`NodeCoordinator`, layer
`frameRate` 0).

**Change**: `Modifier.preferredFrameRate(FrameRateCategory.High)` (`ui/FrameRate.kt:71`, verified in the 1.12.1
sources) on the container while a layer-only animation runs. The candidates are S1's stack box, the back card, the
sheet drag, pull to refresh, swipe actions and the overscroll release. A vote lasts only while the node draws, but
toggle it at start and end anyway, so a spinner does not hold 120 Hz on an idle screen. Never pass a fixed rate.

### S11. Lazy lists (WP03 wrappers, WP02-M3 features)

| Item | Status | Change |
|---|---|---|
| `key` on items | **APPLIED**: `SampleListScreen.kt:131`, `SearchScreen.kt:202`, `Licenses.kt:157` | keep (`lazy-key` rule) |
| `contentType` on mixed lists | **MISSING** everywhere (`grep contentType feature core/ui` is empty) | add it wherever skeleton, header and rows mix; new rule `lazy-content-type` (RECOMPOSITION-PLAN) |
| Cache window | **MISSING** | `rememberLazyListState(cacheWindow = LazyLayoutCacheWindow(aheadFraction = 0.5f, behindFraction = 0.5f))` on primary lists, behind a measurement: only if a scroll trace shows prefetch running out of time, and with PSS checked |
| `AppPagingList` creates its own state (`core/ui/.../paging/AppPagingList.kt:66`) | **MISSING** hoist | take `state: LazyListState = rememberLazyListState()` and a `modifier` for the list, so the feed can carry `primary_list` and `ScrollToTopOnReselect` (WP04 open issue). Owner WP02-M3 (`core/ui`) |
| Pausable prefetch | **APPLIED** by default in Compose 1.12.1 (`ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled`) | nothing to do |

### S12. R8, keep rules and the startup profile (WP02-M2, WP04)

| Item | Status | Change |
|---|---|---|
| Stale keep rule | **MISSING**: `app/proguard-rules.pro:13,16` keep `** implements com.base.app.core.navigation.AppNavKey`; `AppNavKey` moved to `core.common.navigation` in M1, so the rule matches nothing | point it at `com.base.app.core.common.navigation.AppNavKey`, or delete it after checking that the `restore` scenario passes without it (it passed in phase 2, so delete is likely) |
| R8 full mode, resource shrinking | **APPLIED** (AGP 9 defaults, `isMinifyEnabled`, `isShrinkResources`) | run the installed `r8-analyzer` skill once over the template's rules |
| Startup profile and DEX layout | **PARTIAL**: collection exists (`includeInStartupProfile = true`), never generated | S2 |
| Trace markers | **APPLIED**: Compose's consumer rules strip only `sourceInformation*` | never add `-assumenosideeffects` for `traceEventStart`; RECOMPOSITION-PLAN needs the markers |

### S13. Velocity hand-off (perceived smoothness)

A release that ends on a tween, or on a spring started from zero velocity, shows a visible speed discontinuity.
gfxinfo does not count that as jank, but users see it. MOTION-PLAN tracks every instance. The ones in today's code:

- **WP04**: `NavDisplay` finishes a released swipe on its own `tween` (WP04 Deviation 3; replaced by SPEC); the bar
  minimize settle ignores the fling velocity (`AppTabBar.kt:133-136`, `onPostFling` → `settleTo` with no velocity).
- **WP03**: the overscroll release starts from 0 (`foundation/AppOverscroll.kt:77-83`); the fling into the edge is
  scaled by 0.12 (`:85-88`, `:125`); the pull-to-refresh settle starts from 0 (`component/refresh/AppPullToRefresh.kt:98-117`).
- **APPLIED** (velocity carried): `AppSheet` settle (`component/overlay/AppSheet.kt:189-224`), `AppSwipeActions`
  (`component/list/AppSwipeActions.kt:118-124`, `:169-183`).

### S14. Recomposition hygiene (RECOMPOSITION-PLAN)

Recomposition is not today's dominant cost: scroll passes, and the failing rows are first frames. But no per-frame
recomposition during animations is part of the bar, and agents will regress it without gates. The items that touch
frames directly are:

- the provider swaps (S1, S4);
- the static `LocalAppWindowSize` holding raw dp (`theme/AppWindowSize.kt:28-55`, `:94`), which recomposes the whole
  app with skipping off on every resize step;
- composition-phase reads in the old bar and the old pull to refresh, which are deleted or rewritten.

### S15. Glass under its own budget (WP05, not started)

MASTER-PLAN A4.9 rules 1–7 stand. The motion-specific additions:

- Glass materialises by ramping the lens or blur radius, **never by layer alpha** over a backdrop.
- A tab switch does not animate glass. The reveal veil is drawn in content, under the bar, so the bar's backdrop
  re-records once per frame for about 200 ms and runs no extra pass.
- During the back card the content changes every frame, so the bar's backdrop is re-blurred every frame. Gate
  `pushpop_gesture` on `everything-glass` at ≤ 3 % janky and GPU p90 ≤ 8 ms.
- Create `RuntimeShader`s once, off the main thread, and draw them once in the warm-up (S6).
- `preferredFrameRate(High)` while the lens drags.

### S16. Field and dev tooling (WP06)

- **JankStats in the field**, behind `<opt:analytics>`:
  - `androidx.metrics:metrics-performance` with `PerformanceMetricsState.getHolderForHierarchy(view)`.
  - Tag `nav = push:<key>|pop|tab|backcard` while `gate.moving`; the shell exposes a `ShellObserver` seam
    (`app/ui/LaunchHook.kt`) for this.
  - Report janky frames per tag, anonymously.
- **Devtools overlays cost nothing per frame.** The DEV badge's drag position is read in `offset { }`. The panel does
  not recompose the app beneath. An optional "top recomposers" panel installs the RECOMPOSITION-PLAN tracer in the
  dev flavour only (recomposition §8.3).

---

## 4. Order of work for WP04 (re-measure after each step)

| Step | Work | Measure |
|---|---|---|
| 1 | S0: rebuild at HEAD; `speed` A/B; hitch and stage metrics; `tabs_tap`; warm/cold rows; `gesture_first_frame` | the new baseline for everything below, saved as `perf/WP04/phase2b/` |
| 2 | S2: generate the profile; add the wildcard safety net | `pushpop tab1 tabs coldstart_installed` |
| 3 | S4: park kept tabs; deferred lifecycle; `TabVisibility` holder | `tabs tabs_tap idle meminfo` |
| 4 | S1 spike (3 properties), then `StackScene` with S3.1 (clock after composition), parked depth 3, S9 (`CoverScrim` cache), S10 votes | `pushpop pushpop_gesture rapid_back nav_stress meminfo` |
| 5 | SPEC: the back card on `StackScene` | `pushpop_gesture gesture_first_frame`; recordings |
| 6 | MOTION-PLAN M24: tab reveal, two-edge indicator, icon bounce, pill geometry, masked labels | `tabs tabs_tap`; recordings |
| 7 | S6 warm-up pass, S7 precompose tab roots | `tab1` cold and warm |
| 8 | Phase 3: write the final medians + 10 % into the ratchet rows (MASTER-PLAN §2) | full `--gate solid` |

WP02-M3's S3.2 (feature seeding) and WP06's `peek` can land in parallel. Until they do, WP04 measures push with the
sample as it is and records the skeleton swap as a known cost.

---

## 5. Owner index (each owner's handoff note lists its items)

| Owner | Items |
|---|---|
| WP04 | S0, S1, S2, S3.1, S3.5, S4, S5 (shell rows), S6 warm-up, S7, S9, S10 (stack, card), S13 (shell), S15 coordination |
| WP03 | S5 (components), S6 font wiring, S8, S10 (sheet, PTR, swipe), S11 wrappers, S13 (overscroll, PTR) |
| WP02-M2 | S12 keep rule and R8 analyzer, S14 gates (RECOMPOSITION-PLAN) |
| WP02-M3 | S3.2 seeding and deferred sections, S3.4 press-down prefetch, S11 (`contentType`, `AppPagingList` state) |
| WP05 | S6 AGSL, S15 |
| WP06 | S3.3 `peek`, S16 |
| WP07 | the same rules in Compose Multiplatform (S1, S3, S4, S8–S11 are common code) |
| WP12 | §6 |

---

## 6. Rules the docs must state (WP12, requirement 25)

1. Measure release builds compiled with the baseline profile, never debug. Regenerate the profile when journeys
   change. Report hitch ratio, FrameTimeline jank and GPU/`dequeue`, not only gfxinfo janky %.
2. Hide a covered screen or a hidden tab by **parking** it (placed, layered, alpha 0, off-window). Never by skipping
   its measure or placement, which drops every layer. Never by moving it to another parent, which detaches it.
3. Never change a CompositionLocal value or the lifecycle owner of a whole screen on a transition frame.
4. The frame that composes a new screen is not the first frame of its animation: start the clock after it.
5. A destination's first composition is final. Seed it from what the caller or the cache already has, never swap a
   skeleton for content mid-transition, and load secondary sections after the transition settles.
6. No alpha on full-screen layers. Scrims are drawn rects. `ModulateAlpha` for single-draw content and short
   full-screen fades. `Offscreen` only for masks, and only small ones.
7. No allocation in draw code that runs every frame: cache brushes, paths and shapes.
8. Preload or bundle fonts. Create AGSL shaders once, off the main thread.
9. Lists: stable `key`, `contentType` on mixed lists, and a cache window only where a trace shows prefetch starving.
10. Layer-only motion votes `FrameRateCategory.High` while it moves.
11. Every gesture ends on a spring seeded with the finger's velocity. Never a tween after a finger.
12. Root causes found here, each with its fix (for the "root causes" section of requirement 25):
    - tab rebuilds → kept tabs;
    - unplaced hidden tabs → parking;
    - moved covered screens → the persistent stack scene;
    - provider swaps on pop → stable per-entry owners;
    - cold first frames → profile, warm-up, precompose;
    - a pop that drew a hidden full screen → `d56ad13`;
    - alpha on full-screen layers → drawn scrims;
    - a released swipe finished on a tween → the shell-owned back card.
