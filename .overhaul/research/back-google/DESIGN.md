# Back gesture: Google's way, and the smoothest build for the Redmi

Design for the user's request of Oct 19, 06:25:

> "That back gesture fix or implementation do what google says n check also for better idea how to do it smoothly"

It builds on the user's 06:16 decision to adopt `out/WP04-perf/backcard-texture.patch`.

**Inputs**
- The three research reports of this run: Google's guidance, Google's code, and the audit of `wp/WP04` at `6e14ace4`.
- `handoff/WP04-perf.md` §0, §2h and §3.
- New queries over the phone's existing captures (§3). They are saved in `research/back-google/sql/`.

**What was not done**
- Nothing in `wt/WP04` was edited.
- No Gradle, adb or device command ran.
- The only local runs were `trace_processor` over traces already on disk.

**Abbreviations**

| Short | Meaning |
|---|---|
| `OV` | `D:/projects/tools/.overhaul` |
| `O` | `$OV/out/WP04-perf` |
| `P` | `$OV/perf/WP04-perf` |
| `R` | `$OV/research/back-google` |
| `L` | `$OV/bin/locked` |
| T | the texture patch |
| C1, C2 | the two candidates of this design |

---

## 0. Decision

1. **Keep the card's look, and render it the way Google does: T is Google's technique.**
   - **The look:** the card is the user's REQ 24 card (Gmail's). That is Google's own "shared element / detached surface" pattern from the predictive-back design page, built with Material Components' numbers: 0.9 scale, an 8 dp edge gap, and the (0.1, 0.1, 0, 1) progress curve.
   - **What Google says about rendering:**
     - Google's performance docs say to draw an animating complex view from a hardware layer, and only for the animation's length.
     - Compose's docs say `CompositingStrategy.Offscreen` is "for performance when rendering complex sets of drawing instructions".
     - The system's own back animations move the window's existing buffer and never ask the app to redraw.
   - **What T does:** the same thing, inside the app. The screen is rendered once into a texture when the gesture starts. Every later frame moves, scales and fades that one texture.
   - **Material:** nothing Google publishes for this needs Material. The only Material pieces are components the template does not use, and their numbers are already copied (§1).
2. **Make T frame-exact: candidate C1.** Four changes, none of which changes a pixel of the look:
   - **a.** The texture moves one layer in, and the card's `active` flag becomes its own snapshot state.
     - The texture moves onto the screen's own layer, the inner `graphicsLayer` every stack entry already has.
     - The card's pose layer keeps the scale, the shift, the rounded clip and the fade.
     - Nothing the scene draws over the card can then render the texture again. Today it is rendered a second time, in full, on the commit's first frame (+2–4 ms of GPU). With C2's prewarm it would also be rendered again at the gesture's start.
     - The release and the cancel stop recomposing the stack.
   - **b.** Each progress event is applied in the frame it arrives in, by calling `Snapshot.sendApplyNotifications()` after the write.
     - Today Compose applies the write only after the frame ends.
     - So the card is always drawn one frame (8.3 ms) behind the platform's progress.
   - **c.** The commit is timed from the moment the finger lifts.
     - The first frame after the release then already shows the first step of the fade.
     - Today it shows the release pose a second time.
   - **d.** A cancel over a settled, full-width card ends at once.
     - Today about 12 frames of an invisible scrim fade still run.
     - Those frames hold the 10 MB texture and the High frame-rate vote.
   - **e.** Below API 36, the velocity uses the frame time. This makes no difference on the Redmi (SDK 37) and is covered by a unit test.
3. **Start the gesture from a texture that already exists: candidate C2 (C1 plus a prewarm).**
   - **When:** the texture is rendered as soon as a finger goes down inside the window's back-gesture inset. That is about 2 frames before the system hands the app the gesture with perf.py's fast injected swipe (§3.4), and more with a thumb that starts slower.
   - **Why:** this is the in-app version of the system's "the buffer is already there".
   - **What it fixes:** today the gesture's first frame pays the texture's +4 ms at the idle GPU clock. That frame is late, which stuffs the buffer queue. The card then trails the finger by one or two extra vsyncs for the first ~100 ms of the drag (§3.3).
4. **What to ship:**
   - Ship T + C1 if C1 passes its checks (§5.7).
   - Add C2 only if it wins the start metrics and costs nothing measurable on edge taps and edge scrolls.
   - Everything else considered is in §4, with the reason it was rejected.

**Earlier user decisions that stand, and are not re-argued here:**
- Gmail's look (REQ 24):
  - On commit, the card grows back to full size and fades.
  - The vertical follow is linear, up to 24 dp.
  - The corners are uniform, 0 to 28 dp.
  - The scrim is 0.32.
  - `EDGE_NONE` gets no card.
- Adopt the texture, with a written exception to A11.3 rule 1.
- Gate what users feel: `*.hitch_ms_per_s` and `*.janky_after_first_pct`.

Where Google's design-page text differs from Gmail and Material Components, §1 says so. Gmail is the user's choice.

---

## 1. What Google says, against the template

| Google says | Source | Template (T + C1) |
|---|---|---|
| Drive predictive back through the platform APIs: navigationevent's `NavigationEventHandler` (or `NavigationBackHandler`). With Navigation 3, prefer `NavDisplay`'s `predictivePopTransitionSpec`. | navigation-event skill; Nav3 docs | A `NavigationEventHandler` subclass (`StackBackEvents`), a documented API. Nav3's hook passes only the swipe edge, with no touch Y, so it cannot drive a card that follows the finger. In 1.2.0 it also recomposes on every event and fades a whole screen with `Auto` compositing (§4). Justified divergence. |
| `android:enableOnBackInvokedCallback="true"` on API 33–35 | navigation-event skill | Set in `app/src/main/AndroidManifest.xml`. |
| Never intercept Back at the root (it would lose the back-to-home animation). Use back callbacks for UI only. | predictive-back guide; codelab; blog May 2024 | The stack handler is disabled at the root while nothing moves. `ShellRootBack` acts only on a non-start tab's root. |
| Ease progress before use: (0.1, 0.1, 0, 1) | design page; SystemUI `BACK_GESTURE`; MDC | `BackCard.ProgressEasing`, the same curve. |
| Shared-element pattern: 90 % scale; x shift up to ((W/20) − 8) dp, leaving an 8 dp gap; y follow capped at ((H/20) − 8) dp with a decelerate curve; rounded corners | design page | Scale and x match exactly. Y follows Material Components (linear, 24 dp cap), as recorded on Gmail (REQ 24). |
| Do not smooth progress again: the platform already springs it (`BackProgressAnimator`, stiffness 1500, no bounce) | AOSP | No second animation on progress. Google's own navigationevent sample adds `animateFloatAsState`; the template does not. |
| Measure the release velocity from `BackEvent.frameTimeMillis` | `BackEvent` docs (API 36) | Done on API 36+. C1-e covers API 34–35, where navigationevent passes 0. |
| Read animated values only in layer, layout or draw lambdas. Animate only layer properties. | jetpack-compose-performance; hardware-acceleration docs | Yes. A gesture frame composes nothing and lays out nothing (audit §1). |
| `ModulateAlpha` only for content that does not overlap | graphics-modifiers doc | HEAD breaks this: it fades a whole screen with `ModulateAlpha`, so each draw fades on its own. T fixes it: what fades is one texture. C1 keeps that with `ModulateAlpha` on a pose layer that now draws only the texture (§2.2a). |
| A hardware layer for the length of an animation, then released | hardware-acceleration docs (Views); Compose `Offscreen` docs | T. C1-a makes it one render per gesture. C1-d releases it at once on a settled cancel. |
| Cancel: the surface quickly returns to edge-to-edge | design page | The platform springs progress to 0, then C1-d ends the card. |
| Commit: absorb the release velocity by moving further toward the maximum preview, then commit | design page (text); SystemUI `FLING_BOUNCE` | **Differs, by the user's decision.** Gmail and Material Components grow the card back to full size while it fades (REQ 24). The fade costs the same either way. |
| `EDGE_NONE` (3-button long-press on Android 16+): Google's sample scales the screen with no x shift | navigationevent guide | **Differs, by decision** (SPEC §0.5): no card, the pop slides. |
| `Modifier.preferredFrameRate` only if the display leaves 120 Hz | Compose API docs | The High vote is raised only while the card is up (`StackLayout`), as measured in this project. |
| Measure release builds with R8 and Baseline Profiles | performance docs | `devBenchmarkRelease`; the profiles include the gesture. |

**The A11.3 rule 1 exception (the user decided it on Oct 19).**
- The rule text is in `design/MASTER-PLAN.md`, around line 2025, and `handover/HANDOVER.md`, item 41. It is not in the template's own docs.
- Proposed wording:

  > "Exception: the back card. The screen under the card is drawn from its own hardware layer (`CompositingStrategy.Offscreen`) from the gesture's first frame until the card is gone, and released then (Google's 'hardware layer for the animation's duration'). It is rendered once per gesture while the screen's own content is still. `SlideRecordingTest` guards that the screen is not recorded again between the start and the pop; a trace shows one `drawLayer` per gesture."

- Rule 1's premise ("alpha < 1 renders the screen offscreen every frame") is true of HWUI's transient `saveLayerAlpha`, which is what `NavDisplay`'s fades get.
- It is not true of a persistent layer. HWUI re-renders a layer only when its display list or a child is damaged, never because the layer's own transform, alpha or outline changed (`RenderNode::prepareLayer`).

---

## 2. The design

### 2.1 Base: T, as decided

**Change.** In `BackCardState.applyCard`, `compositingStrategy = CompositingStrategy.Offscreen` for as long as the card is active, replacing `ModulateAlpha` during the fade.

**Why it is cheap.**
- HWUI renders the subject's whole display list once into a 1080 × 2400 texture.
- Each later frame composites one textured quad, with the card's scale, translation and alpha, under its rounded outline clip.
- Measured in `perf/WP04-perf/cardtex-nav`: the texture was not re-rendered once while the card followed the finger, in 3 of 3 gestures.
- Phase 17b measured:
  - RenderThread-caused mid-gesture hitches fell from 38 to 2.
  - The RenderThread median per gesture frame fell from 2.24 to 1.50 ms.
  - The hitch ratio fell from about 20 to 8–16 ms/s.

**What it costs.**
- The first frame: +4 ms of GPU at the idle clock (10.8 → 14.9 ms).
- About 10.4 MB of GPU memory while the card is up.
- Text drawn from a texture scaled to 0.9–1.0 with bilinear filtering. The system's own scaled window looks the same.

**C1 removes:**
- the second render, by moving the texture one layer in (§2.2a). It is still the texture the user adopted, kept for the gesture's length;
- the frame of lag;
- the repeated release pose;
- the cancel tail.

**C2 attacks the first-frame cost.**

### 2.2 C1: frame-exact (no visible change to the look)

Sketches against `wp/WP04` with T applied. Final names are the implementer's. Comments follow the template's rules: why, not what.

**a. The texture on the screen's own layer; `active` its own state.**

*Why T's texture gets rendered twice.*
- T puts `Offscreen` on the subject's pose layer: the outer `graphicsLayer { pose(key) }` of `StackLayout.sceneDraw`.
- That layer's display list is recorded by the `drawWithContent { drawOver(key) }` beneath it.
- `drawOver` reads `card.active` for every key, the subject included (`StackScene.kt:281`).
- So any change to what `drawOver` reads gives the subject's node a new display list. HWUI then renders the whole 10 MB texture again (`damageSelf`). That happens:
  - on the commit, when `active`, a getter on `phase`, sees `Tracking → Settling`. This is the second `drawLayer` per gesture in `sql/layers.sql`, at +2–4 ms on that frame;
  - on every frame of a gesture started mid-push, because `drawOver` reads `cover` first;
  - with C2, at the gesture's start, when `active` flips.

*Why reordering the reads is not enough.*
- The fix suggested in the Google-code research, `key in card.beneath && card.active`, still reads `beneath`, which `start()` writes. So it does not help C2.
- Any guard that skips the card's state for the subject also skips it for the page beneath once the pop makes that page the top. The scrim's last ~20 ms over the new top would vanish: a 5 % brightness step.

*The fix: move the texture one layer in.*
- `sceneDraw` already ends in a plain `.graphicsLayer()`: the screen's own layer, which keeps the slide from recording the screen again (`handoff/WP04-perf.md` §2b).
- Make that layer `Offscreen` while the entry is the card's subject (or C2's warm entry).
- The texture then holds exactly the screen's drawing. Re-recording the outer node only re-records a reference to that layer, and the texture stays valid.
- The pose layer keeps scale, translation and the rounded outline clip. For the fade it goes back to `ModulateAlpha`.
  - That is correct now: what the pose layer draws is one texture (plus, between panes, the 1-px divider), so nothing in it overlaps.
  - Google's caveat about `ModulateAlpha` applies to overlapping draws, which HEAD had.
  - It costs no offscreen pass.
- Covers get the same structure: one more plain layer between `applyCard()` and `.background()` in `PresentedFrame`.

```kotlin
// StackLayout.sceneDraw
Modifier
    .graphicsLayer { with(scene) { pose(key) } }
    .drawWithContent { with(scene) { drawOver(key, isLight) }; if (divided) drawPaneDivider(divider) }
    // The screen's own layer. Under the back card it is a texture, rendered once and moved, scaled and
    // faded by the card's pose above it; what the scene draws over the card can change on any frame
    // without rendering the screen again.
    .graphicsLayer { compositingStrategy = if (scene.card.textures(key)) CompositingStrategy.Offscreen else CompositingStrategy.Auto }

// BackCardState
fun textures(key: Any): Boolean = active && key == subject   // C2 adds: || key == warm

// BackCardState.applyCard(): no Offscreen; the fade as HEAD had it, with the reason it now holds:
//   if (alpha < 1f) { this.alpha = alpha; compositingStrategy = CompositingStrategy.ModulateAlpha }
//   // Fades the one texture this layer draws, with no offscreen pass of its own.

// PresentedFrame
Modifier.fillMaxSize()
    .graphicsLayer { if (card.active && card.subject == key) with(card) { applyCard() } }
    .graphicsLayer { compositingStrategy = if (card.textures(key)) CompositingStrategy.Offscreen else CompositingStrategy.Auto }
    .background(background)
```

*`active` as its own state.*
- `StackLayout` reads `scene.card.active` in composition, for the frame-rate vote.
- As a getter on `phase`, it recomposes the layout on the commit and the cancel, on the release frame.

```kotlin
/** Read only by the Back handler between events, so no reader runs again when it changes. */
var phase = Phase.Idle
    private set

/**
 * True from the first progress event until the card is gone, commit and cancel included. Written only
 * as the card comes and goes, so the commit and the cancel compose nothing.
 */
var active by mutableStateOf(false)
    private set
// start(): phase = Phase.Tracking; active = true.   reset(): phase = Phase.Idle; active = false.
```

- One HWUI behaviour must be confirmed: that the pose node, with `ModulateAlpha` (no overlapping rendering), multiplies its alpha into the child layer's `drawImageRect`. This is the View-system case of a faded parent over a child with a hardware layer.
- The check: `BackCardDrawingTest` with alpha < 1, and the recording's fade frames (§5.6).

**b. Progress is applied in the frame it arrives in.**
- **How progress is delivered today.**
  - The platform calls `onBackProgressed` from its progress spring, in the animation phase of `Choreographer#doFrame`.
  - A snapshot write made there is applied by Compose's `GlobalSnapshotManager` only after the frame, through `Handler.post` and the next frame callback.
  - Only then do the layer properties update and the view invalidate.
- **The result.** Each step reaches the screen one vsync late. The trace signature is a doFrame that ticks the spring but has no traversal, right after one that draws without a tick (§3.1).
- **Why the fix works.** View property animators, and Shell's per-vsync transactions, show a value in the frame that computed it. Calling `sendApplyNotifications()` in the callback runs the layer blocks and invalidates the view during the animation phase. Choreographer then runs the traversal it posts in the same frame.

```kotlin
private fun follow(event: NavigationEvent) {
    if (!cardEnabled) return
    val card = host.card
    when (card.phase) {
        BackCardState.Phase.Tracking -> card.track(event)
        BackCardState.Phase.Idle ->
            if (event.swipeEdge != NavigationEvent.EDGE_NONE && host.canStartCard()) host.startCard(event, scope) else return
        BackCardState.Phase.Settling -> return
    }
    // The platform calls this from its progress spring, in the frame's animation phase. Applied now,
    // the writes are drawn by this frame's traversal; left to Compose they are applied after the
    // frame, and the card is drawn a frame behind the finger.
    Snapshot.sendApplyNotifications()
}
```

**c. The commit is timed from the release.**
- **Today:**
  - `onBackCompleted` launches `commitCard` with a dispatch.
  - The Animatables then start on their first frame at play time 0, so that frame draws the release pose again.
  - With T, that frame also re-renders the texture.
- **With C1:**
  - Start the commit undispatched.
  - Run its three animations on a frame clock whose first frame is the release instant. Later frames come from the composition's clock.
  - The first real frame is then already `vsync − release` into the commit. Every Animatable and every reader stays as it is.

```kotlin
override fun onBackCompleted() {
    if (host.card.phase == BackCardState.Phase.Tracking) {
        val released = System.nanoTime()
        // Undispatched: the commit's animations are running before the next frame starts.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { host.commitCard(pop, released, scope) }
    } else {
        gate.onBack(pop)
    }
}

// in BackCardState.commit(spring, releasedNanos, scope):
//   val unwinding = launch(startedAt(releasedNanos)) { unwind.animateTo(1f, spring, initialVelocity = velocity) }
//   scrimJob = scope.launch(startedAt(releasedNanos)) { scrim.animateTo(0f, tween(SCRIM_OUT_MILLIS, easing = LinearEasing)) }
//   withContext(startedAt(releasedNanos)) { contentAlpha.animateTo(0f, tween(COMMIT_FADE_MILLIS, easing = LinearEasing)) }

/**
 * A frame clock whose first frame is [startNanos], a moment already past, and every later one
 * [frames]'s: an animation run on it is as far along on its first drawn frame as the time since then.
 */
private class StartedAt(private val startNanos: Long, private val frames: MonotonicFrameClock) : MonotonicFrameClock {
    private var first = true
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R =
        if (first) { first = false; onFrame(startNanos) } else frames.withFrameNanos(onFrame)
}
```

- Choreographer's frame times and `System.nanoTime()` share `CLOCK_MONOTONIC`.
- An alternative that also works: `AnimationState.animateTo(sequentialAnimation = true)` starting from `lastFrameTimeNanos`. It would change the type behind `unwind`, `contentAlpha` and `scrim` and every reader, so the clock is the smaller change.

**d. A settled cancel ends at once.**
- When `onBackCancelled` arrives, the platform has already sprung the progress to 0, so the card is opaque and full size.
- Over a settled stack, with the subject as wide as the host, the strips around the card are empty and the scrim is invisible. The 100 ms scrim fade there draws nothing anyone can see.

```kotlin
suspend fun cancel(midPush: Boolean, fullWidth: Boolean) = coroutineScope {
    phase = Phase.Settling
    scrimJob?.cancel()
    // The system has sprung the card back to full size: over a settled stack it hides everything its
    // scrim would dim, so nothing is left to animate.
    if (!midPush && fullWidth) {
        reset()
        return@coroutineScope
    }
    // ... unchanged: reveal and scrim back over CANCEL_MILLIS, then reset()
}
// StackScene.cancelCard(): card.cancel(midPush = moving, fullWidth = (widths[card.subject] ?: width) >= width)
// PresentationCard.cancelCard(): card.cancel(midPush = false, fullWidth = true)
```

**e. Velocity below API 36.**
- In `track()`: `val frameMillis = event.frameTimeMillis.takeIf { it > 0L } ?: AnimationUtils.currentAnimationTimeMillis()`. This gives the frame the platform's spring ticked in.
- Then `tracker.addDataPoint(frameMillis, event.progress)`.

**Tests C1 adds (in the patch):**
- `BackCardTest`:
  - `active` is written exactly twice per gesture: at start and at reset. Count it with an apply observer.
  - A settled cancel resets without waiting for the clock.
  - With `frameTimeMillis = 0`, a moving swipe gives a non-zero velocity.
  - After `onBackCompleted` and one frame, `contentAlpha < 1`.
- `SlideRecordingTest`: the subject's screen, the content inside its own layer, is not recorded again between the gesture's first frame and the pop, commit included; that layer is `Offscreen` for exactly that span.
- A test that after `follow(event)` returns, with no frame advanced, the global snapshot has no pending writes. Count it with `Snapshot.registerApplyObserver`.

If an existing test pins the old one-frame hold or the 100 ms cancel tail, update it to assert the new intended behaviour, and say so in the commit. Never loosen it.

### 2.3 C2: prewarm on touch-down (C1 plus this)

**The timing it uses.**
- With gesture navigation, the app window receives the `ACTION_DOWN` of an edge swipe. The system's back handler takes the pointer later (the app receives `CANCEL`), and `onBackStarted` follows.
- With perf.py's swipe, that gap is 12–17 ms down to cancel, and 13–19 ms down to the gesture's first frame (§3.4). A thumb that starts slower gives several more frames.
- No frame is drawn in that gap today.

**What C2 does.** A `Modifier.Node` on each stack host:
- watches the `Initial` pass, never consumes;
- on a touch-down inside the window's `systemGestures` left or right inset, when the API level is 34 or higher and `canStartCard()` holds, marks the top entry *warm*;
- that entry's screen layer becomes `Offscreen` at once (`textures(key)` is true), and the next frame renders the texture while nothing visible moves.

**When the texture is dropped.**
- A vertical move beyond the touch slop drops it at once (the touch is a scroll).
- An up or a cancel with no card within 120 ms drops it.
- A cap: 1 s after the touch-down.
- When the gesture does start, `start()` clears `warm` in the same write as `active = true`.
  - `textures(key)` stays true across that write, so the screen's layer stays `Offscreen`.
  - The texture lives one layer below the node that `drawOver` records (C1-a), so the start's own changes do not render it again.
  - This only works on top of C1-a. With T's texture on the pose layer, the start's write of `active` would render the prewarmed texture a second time.

```kotlin
// BackCardState
/** The entry drawn into its texture ahead of a gesture: a finger is down at the window's back edge. */
var warm: Any? by mutableStateOf(null)
    private set
fun warm(subject: Any) { if (!active) warm = subject }
fun cool() { warm = null }
fun textures(key: Any): Boolean = (active && key == subject) || key == warm
// start(): warm = null alongside active = true

// back/BackEdgeWarm.kt: Modifier.backEdgeWarm(host) = a PointerInputModifierNode +
// CompositionLocalConsumerModifierNode. One coroutine per edge touch-down and up, never one per event
// (A11.3). The window x comes from requireLayoutCoordinates().localToWindow(position); the inset from
// ViewCompat.getRootWindowInsets(currentValueOf(LocalView)).getInsets(WindowInsetsCompat.Type.systemGestures());
// the slop from currentValueOf(LocalViewConfiguration).touchSlop. Applied in StackHost only while the
// card is enabled (not under reduce motion).
```

- Keep the method names `fun warm(subject: Any) {` and `fun cool() {`. `instrument.py` anchors its counter on them.
- Covers (`PresentationCard`) get the same node only if C2 is adopted.

**Tests C2 adds:**
- In a Robolectric Compose test, with the inset seam set:
  - a down inside the edge warms;
  - a vertical drag past the slop cools at once;
  - an up cools after the grace period;
  - a gesture that starts keeps the layer and clears `warm`.
- A down outside the edge, or at the root, never warms.

### 2.4 What each frame does

| Frame | HEAD | T | T + C1 | T + C1 + C2 |
|---|---|---|---|---|
| Edge touch-down | nothing drawn | nothing | nothing | one frame: the texture rendered while nothing moves (+4 ms GPU, invisible) |
| Gesture start (progress 0) | the beneath is unparked under the opaque card | + texture render (+4 ms GPU at the idle clock) | the same; any progress step that arrives in this frame is drawn in it (b) | the same, with no texture render: the texture already exists |
| Tracking | the subject's display list replayed under a changing scale (RT 2.2 ms median; 7–12 ms on a little core) | one textured quad (RT 1.5 ms) | the same, each step drawn one vsync sooner | the same |
| Commit, first frame | the release pose again | the release pose again + a full texture re-render | the fade's first step, no re-render, no recomposition | the same |
| Fade, about 12 frames | beneath + scrim + `ModulateAlpha` card, each draw faded alone (GPU 7.4–8.9 ms) | beneath + scrim + one faded quad (group fade) | the same | the same |
| Settled cancel tail | about 12 frames of invisible scrim, High vote | the same + the texture held | none | none |

Not changed by any candidate:
- The fade's three full-screen fills. They cause the 6–7 GPU-late fade frames in 36 gestures. §6 has the next lever if they become the largest remaining item.
- The beneath's four strip walks.

---

## 3. New evidence behind C1 and C2 (existing traces, no new phone run)

### 3.1 Progress is drawn one frame late

`research/back-google/sql/gestureframes.out`, `cardtex-nav`:
- In gesture 1, the doFrame at 5724.0 ms traverses without an `animation` tick: it draws the previous tick's value, applied late.
- The one at 5732.4 ms ticks the spring and has no traversal.
- The same pair appears at 16365.8, 16491.2 and 26790.7 ms.

The mechanism, from the bytecode:
- Writes made outside Compose's own frame are applied by `GlobalSnapshotManager` after the doFrame.
- The Recomposer applies its own animations' writes inside its frame.
- The back card is the only per-frame motion fed from a platform callback, so it is the only one drawn a frame late.

### 3.2 The texture is rendered twice per gesture

`layers.sql` on `cardtex-nav`:
- The texture is rendered on the gesture's first frame and again on the commit's first frame, 29 frames later.
- It is never rendered in between.

`commitframes.sql`:
- The commit frames take 10.7, 12.3 and 11.8 ms against 7.9–10.5 ms for their neighbours.
- `commitmain.sql` shows a `Compose:recompose` in exactly those frames: `StackLayout` reading `active`, which reads `phase`.
- In the same frames, the subject's outer node gets a new display list, because `drawOver` reads `card.active` for every key. Its `Offscreen` layer is then rendered in full again. §2.2a explains why the fix is to move the texture one layer in, rather than to reorder the reads.

### 3.3 A late start frame stuffs the buffer queue (new)

`research/back-google/sql/presentlat.sql` measures, for each gesture's frames, the vsync-to-present latency: FrameTimeline's app frame matched to the SurfaceFlinger display frame that presented it.

| Capture | Frames 1–10 (ms) | Frames 11–30 (ms) | Pattern |
|---|---|---|---|
| `cardtex-nav` (T), 3 gestures | 37.7, 37.7, 42.7 | 36.8, 30.6, 29.3 | 46 → 38 → 29, falling one vsync at each skipped frame |
| `layer-nav` (HEAD-like), 4 gestures | 37.7 ×4 | 37.7, 32.2, 31.0, 36.8 | the same, and it climbs back to 38–46 after a mid-gesture RenderThread spike |

- **The floor.** On this phone the floor is 29.3 ms.
- **The stuffing.** Every transition from idle starts 1–2 buffers deep ("Buffer Stuffing" in FrameTimeline), because its first frame is late. That puts the card 8–17 ms further behind the finger than the floor.
- **How it drains.** Only a vsync in which the app draws nothing drains a buffer.
- **What follows:**
  - In these few captures, T keeps the drag at the floor once it gets there, while HEAD re-stuffs after little-core RenderThread frames. The samples are too few to call it a result.
  - Most of perf.py's "frame on time (input cadence)" hitches (24 of T's 42) are such drains. They are vsyncs with nothing new to draw (the injected moves paused, or progress saturated near the far edge), not stutters.
  - C1-b does not change those counts: the gap moves by one frame, it does not disappear. It shortens the touch-to-screen path by one vsync on every frame.
- **The lever.** The one way to start the drag at the floor is a first frame that is on time, which is what C2 aims at.

### 3.4 The touch-down comes before the gesture

`down → cancel → first gesture frame` for perf.py's `input swipe 3 … 260`:
- `cardtex-nav`: 12.9 / 13.4, 12.4 / 18.1 and 12.3 / 16.3 ms.
- `layer-nav`: 11.7–17.3 / 17.2–18.9 ms.
- The touch-down draws no frame today.
- So even this fast injected swipe leaves one vsync for a prewarm, and a thumb leaves more. `bgsteps.sh`'s slow-start swipe measures that case.

### 3.5 Checked and dismissed

- `Compose:onPositionedCallbacks` runs in every gesture frame. It takes 0.02–0.07 ms: layer moves notify position listeners. Not worth chasing.

---

## 4. Considered and rejected

| Idea | Why not |
|---|---|
| Nav3's `predictivePopTransitionSpec` or its default | The hook gets only the swipe edge (no touch Y, corner or scrim). 1.2.0 recomposes and launches a seek coroutine per event. The default `fadeIn(spring 1600)` + `scaleOut(0.7)` fades a whole screen with `Auto` compositing, which is a fresh offscreen pass every frame: exactly rule 1's case. |
| `NavigationBackHandler` reading `transitionState` for the card | A composition read per event. It also has no per-event start callback, which the card needs for the velocity, the start Y and the `EDGE_NONE` filter. |
| `PredictiveBackHandler` (activity-compose) | A Flow and a coroutine resume per event, and cancel arrives as an exception. The template writes in the callback directly. |
| Material3's predictive back (`PredictiveBack.transform`, `SearchBar`, the sheets) | Material is forbidden, and Google's guidance never requires it. `SearchBar` also re-measures every frame. The numbers are already copied, without the library. |
| Google's navigationevent sample, literally | It reads progress in composition, adds `animateFloatAsState` (a second smoothing, adding lag), and uses `Modifier.offset`, which flips in RTL. |
| The design page's "full screen surfaces" fade-through (exit 100 → 90 %, enter 110 → 100 %, crossover at 35 %) | Not the user's look (REQ 24). It also puts two whole screens under alpha for most of the gesture, which needs two textures to be cheap. |
| Commit "toward the maximum preview" (Google's design text) | The user chose Gmail's grow-back. Same cost per frame with T. |
| A texture only while the finger moves, plus a cheap fade without it | The texture is the cheap fade. Without it the fade replays the screen with `ModulateAlpha` (wrong, per Google) or `Auto` (offscreen every frame). |
| The platform-style captured buffer: `GraphicsLayer.toImageBitmap`, `PixelCopy` | `toImageBitmap` renders synchronously, with the UI thread waiting. `PixelCopy` is an asynchronous readback of 5–20 ms of the whole window, bar included. The HWUI layer *is* the in-process captured buffer. |
| `SurfaceControl` leashes, as Shell uses | The setters are hidden API. |
| Recording a `GraphicsLayer` once and drawing it | The same RenderNode replay unless it is also `Offscreen`. No gain over T. |
| A second texture for the page beneath | +10 MB and another full render on the start frame, while the RenderThread median is already 1.5 ms. |
| `Offscreen` from the second gesture frame | It moves the texture's cost onto the first *visible* frame. T's choice of the invisible start frame is the better one; C2 moves it earlier still. |
| A texture for the top screen whenever it is idle | It re-renders on every scroll frame: a full extra pass per frame. |
| An opaque commit that slides the card off the far edge | It keeps the strips valid (GPU ~5 ms), but Google's design page says the card must not look dismissed in the swipe direction, and it is not Gmail's look. |
| Skipping a frame on purpose to drain a stuffed queue | There is no public signal of the queue depth. The cause is the late start frame (C2). |
| CPU or GPU boosts (ADPF), pinning the RenderThread | Ruled out in §2h: the clocks did not move, and core numbering differs per SoC. |
| A y follow with a decelerate curve, or corners that start at the display's radius | Google's text and the system do this. The user's spec copies Gmail and Material Components: linear 24 dp, uniform 0 → 28 dp. Neither change makes any difference per frame. |

---

## 5. Experiment plan

Four builds of one `wp/WP04` commit (B), measured on the Redmi `9a7c74f4`. Besides HEAD and T, there are exactly the two candidates C1 and C2, as a ladder in which each step adds one change set.

| Build | What |
|---|---|
| `bg-head` | B without T: reverse-applied if WP04 has committed it by then |
| `bg-tex` | B with T |
| `bg-c1` | `bg-tex` + `patches/c1.patch` |
| `bg-c2` | `bg-c1` + `patches/c2.patch` |

### 5.1 What to build

1. **Patches (an Opus developer writes them; they do not exist yet):**
   - `$R/patches/c1.patch` against B + T, per §2.2, tests included.
   - `$R/patches/c2.patch` against B + T + C1, per §2.3, tests included.
   - Write them in a scratch extraction under `$O/s/`: a `git archive` of B, T applied with `git apply`. Never in a worktree.
   - Produce them with `git diff --no-index` between two extracted copies, rewriting the prefixes to `a/` and `b/`, so `git apply` takes them from a tree root.
   - Do not create a git repository anywhere under `.overhaul`.
   - Keep `instrument.py`'s anchors intact (§5.2).
2. **Build:** `$R/bin/build-ladder.win.sh <B>`, logged to `$O/logs/bg-build.log`. It:
   - archives B four times under `$O/s/bg-<name>-<B>`;
   - applies the patches in order (it detects whether B already contains T);
   - runs `instrument.py`;
   - runs `:core:navigation:testDebugUnitTest` under `locked gradle`;
   - generates the everything preset into `$O/bg-<name>`;
   - builds `:app:assembleDevBenchmarkRelease` under `locked gradle`.

   It stops if an output folder already exists. It deletes nothing.

### 5.2 Instrumentation (identical in all four builds)

`$R/bin/instrument.py <tree>` is already written, and was tested on copies of HEAD's files with T applied after it. It adds `back/BackTrace.kt` and these app trace counters:

| Counter | Value | Written in |
|---|---|---|
| `BackCard.write` | raw progress × 1000 | `track()` |
| `BackCard.apply`, `BackCard.alpha` | pose and alpha × 1000 | each run of `applyCard()` |
| `BackCard.phase` | 1 started, 2 completed, 3 cancelled, 0 reset | `onBackStarted`, `onBackCompleted`, `onBackCancelled`, `reset()` |
| `BackCard.warm` | 1 / 0 | C2's `warm()` and `cool()` |

- **Cost:** `Trace.setCounter`, guarded by API 29. When tracing is off, it is a flag check.
- **Profiles:** it also appends wildcard baseline-profile rules for `back/**`, `StackScene*` and `StackHost*`. Every build then compiles the touched classes ahead of time alike, and the candidates' new methods are not interpreted.
- **Anchors it needs:**
  - `fun track(event: NavigationEvent) {`
  - `fun GraphicsLayerScope.applyCard() {`
  - `private suspend fun reset() {`
  - `override fun onBackStarted(event: NavigationEvent) = follow(event)`
  - `override fun onBackCompleted() {`
  - `override fun onBackCancelled() {`
  - C2 only: `fun warm(subject: Any) {` and `fun cool() {`
- It exits non-zero if a required anchor is missing.
- The instrumentation is never committed. WP04 takes `c1.patch` (and `c2.patch`) without it, and re-records the baseline profile.

### 5.3 Unit tests

- All of `:core:navigation` in every patched tree (130 tests at phase 17b, more since), including `BackCardDrawingTest` and `SlideRecordingTest`, plus §2.2's and §2.3's new tests.
- A red test stops the ladder.

### 5.4 Phone session

One adb hold: `$R/bin/bg-phone.win.sh`, logged to `$O/logs/bg-phone.log`. It changes no device setting. Device files go to `/data/local/tmp` and `/data/misc/perfetto-traces`.

| Step | What | Order | Runs | Output |
|---|---|---|---|---|
| 1 | `perf.sh` `pushpop_gesture` + `meminfo` (`measure.win.sh`, `NOTRACE=1`) | head, tex, c1, c2 | 3 runs each = 18 gestures | `$P/bg-<n>-bg1` |
| 2 | the same | c2, c1, tex, head | 3 runs each | `$P/bg-<n>-bg2` |
| 3 | `perf.sh` `back_during_push` (mid-push card: one opaque card, never double-exposed) | head, tex, c1, c2 | 3 runs each | `$P/bg-<n>-bgp` |
| 4 | `back-trace.win.sh t1`: one Perfetto capture per build of `bgsteps.sh` | head, tex, c1, c2 | 1 | `$P/bg-<n>-t1` |
| 5 | `back-trace.win.sh t2` | c2, c1, tex, head | 1 | `$P/bg-<n>-t2` |
| 6 | per build: `heldmem.win.sh`, `rec-gesture.win.sh`, `rec-cancel.win.sh` | head, tex, c1, c2 | 1 | `$P/bg-<n>-held`, `-rec`, `-cancel` |

**Steps 1–2.** These give 36 commits per build, the same depth as phase 17b, in both orders, so drift and order fall on every build.

**What `bgsteps.sh` does in each capture (90 s):**
- 4 perf.py-style fast swipes, each with a push before it;
- 2 slow-start commits: a touch-down, then 9 moves that are each a separate `input` call, so the system takes the pointer several frames after the touch-down;
- 2 slow cancels: out to 40 %, a pause, back to the edge;
- 3 edge touches on the detail: a down held 250 ms at x = 3, ended with `CANCEL`, so nothing under it is clicked;
- 2 vertical edge swipes that scroll the detail.

Each step's `/proc/uptime` goes to `steps.txt`. That is the trace's `CLOCK_BOOTTIME`, so `layerts.sql`'s `uptime_s` lines up with it directly.

Two captures per build give 8 fast commits, 4 slow commits, 4 cancels, 6 edge touches and 4 edge scrolls.

**`heldmem.win.sh` takes:**
- a full-resolution screenshot of the card held at 45 % (`heldshot.win.sh`'s swipe);
- `dumpsys gfxinfo` and `dumpsys meminfo` before the gesture, held, 1.5 s after the commit and 35 s after it;
- the same around a 0.5 s still touch at the edge (C2's prewarm).

**Time:** about 75 minutes of adb hold. Run it with the phone idle, the screen unlocked, and Google Photos not busy.

### 5.5 Reading the results

| Question | Tool (from `$O` unless noted) | Expected |
|---|---|---|
| Gate rows and ratchets: janky / after the first / p99 / hitch ms/s / late first frames / PSS | `$P/bg-<n>-bg{1,2}/summary.md` (perf.sh) | C1 ≈ T within T's spread (17b: hitch 8.3–16.5 ms/s; after the first 0.40–0.53 %) |
| Late frames, RenderThread ≥ 6 ms, GPU over a vsync, display hitches | `python3 abstats.py 'gesture_pop_\d+\.txt' $P/bg-*-bg1 $P/bg-*-bg2` | RenderThread ≥ 6 ms ≈ 11–15 for tex/c1/c2 (85–90 for head) |
| Where each hitch is, and its cause | `python3 hitchpos.py $P/bg-<n>-bg{1,2}/run* 'gesture_pop_\d+\.txt'` | c1: fewer hitches at the release; input-cadence drains unchanged |
| First frame, gesture frames and tail, by stage | `python3 gesturephases.py $P/bg-*-bg1 $P/bg-*-bg2`; `latestage.py <run dir> 'gesture_pop_\d+\.txt' all` | First-frame GPU: head ≈ 10.8, tex/c1 ≈ 14.9, c2 lower (**note:** in c2, dump frame 0 can be the prewarm frame, so read the trace for c2's start) |
| Janky after the first, for the dumps | `python3 afterfirst.py $P/bg-*-bg1 $P/bg-*-bg2` | as above |
| Texture renders per gesture, per edge touch, during edge scrolls | `./tp.sh $R/sql/layerts.sql <trace>`, bucketed by `steps.txt` | head 0, tex 2, c1 1, c2 1 per gesture; c2 1 per edge touch, 0 while a scroll moves |
| Progress drawn in its own frame; touch-to-present; start; release; cancel tail | `./tp.sh $R/sql/backlat.sql <trace>` | `late_draws` ≈ all writes in head/tex, 0 in c1/c2; `write_to_trav_med` ≥ 8.3 ms in head/tex, < 8.3 in c1/c2; `write_to_present_med` c1 ≈ tex − 8.3; `release_to_fade_ms` c1 ≈ tex − 8.3; `frames_after_settle` on cancels about 12 in tex, ≤ 2 in c1 |
| The commit's first frames | `./tp.sh $R/sql/releaseframes.sql <trace>` | tex: `layer_renders` 1 on frame 1, actual +2–4 ms; c1: 0, no slower than its neighbours |
| Buffer stuffing at the start of the drag | `./tp.sh $R/sql/presentlat.sql <trace>` (each gesture starts at the app's `CANCEL`) | c2 slow-start: `avg_1_10` toward 29–38 ms, against 38–46 in c1 |
| RenderThread per frame (big or little core) | `./tp.sh sql/rtframes.sql <trace>` then `python3 bursts.py` | as in §2h |
| T only: the old windows | `$R/sql/gestureframes.sql`, `commitframes.sql`, `commitmain.sql` | They pair `drawLayer` slices, so they work on tex only. Use `backlat.sql` and `releaseframes.sql` for the others. |

- `backlat.sql` and `releaseframes.sql` were syntax-checked on `cardtex-nav`, which has no counters, so they returned no rows. Check the counter track names on the first capture (the query to use is in `backlat.sql`'s header).
- `presentlat.sql` and `layerts.sql` already give the numbers in §3.

### 5.6 Visual checks

| Check | Files | Method | Pass |
|---|---|---|---|
| **Text sharpness, mid-gesture** | `$P/bg-<n>-held/held.png` | Take two text crops (a title line and a body line inside the card) and one icon crop, choosing the rectangles once from `bg-head`'s shot. Compare with ffmpeg (both views below). | tex, c1 and c2 identical to each other (PSNR ≥ 50 dB on the card); against head, SSIM ≥ 0.97 on the text crops (bilinear softness only, no layout shift) |
| **Corners** | the same shots | the four card corners, zoomed 6× | Smooth anti-aliased arcs; no dark or transparent fringe on the straight edges; the strips meet the card with no seam. `BackCardDrawingTest` covers the pixels in Robolectric. |
| **Shadows** | the same shots | look for any elevation shadow on the detail's content, and around the card | Expected the same in head and tex. HWUI draws a child's shadow into the layer with the light mapped into it (from memory, not re-read), so a shift or a change in softness is a finding. No shadow around the card: none is designed. |
| **Commit fade** | `$P/bg-<n>-rec/gesture.mp4` | extract every composed frame: `ffmpeg -v error -i gesture.mp4 -fps_mode passthrough all/f%04d.png` | head: overlapping draws fade separately (`ModulateAlpha`); tex/c1/c2: one uniform group fade; c1/c2: the first frame after the finger lifts is already fading (cross-check `backlat.sql`'s `release_to_fade_ms`) |
| **Cancel end** | `$P/bg-<n>-cancel/frames/` | inspect the last frames | No flash and no step when c1 resets at once; the screen identical to the frame before |
| **First frame's latency** | traces | `backlat.sql` `start_to_shrink_ms`; `presentlat.sql` frames 1–10; the start frame's `DrawFrames` and actual length | c1 ≤ tex − 8 ms; c2 ≤ c1 − 8 ms on slow starts, not worse on fast swipes |
| **GPU memory** | `$P/bg-<n>-held/gfx-*.txt`, `mem-*.txt`; the perf.sh `meminfo` rows | the "Total GPU memory usage" line, its purgeable part, and meminfo's Graphics and GL mtrack | held − before ≈ +10.4 MB non-purgeable in tex/c1/c2 (0 in head); after1 back to before's non-purgeable level; after35 not above before + 1 MB, or purgeable; c2 edgehold +10.4 MB and edgeafter back; the perf.sh PSS row within the head runs' spread |

**The text-crop comparison** (the rectangle `W:H:X:Y` is chosen once):
- SSIM: `ffmpeg -v error -i a.png -i b.png -lavfi "[0]crop=W:H:X:Y[a];[1]crop=W:H:X:Y[b];[a][b]ssim" -f null -`
- PSNR: the same with `psnr`.
- To view: `ffmpeg -v error -i held.png -vf "crop=W:H:X:Y,scale=iw*6:ih*6:flags=neighbor" crop6x.png`

### 5.7 Decision rules

**Adopt C1** on top of T when all of these hold:
1. One `drawLayer` per back gesture, in all 16 traced gestures.
2. `late_draws` = 0, and `write_to_present_med` at least 6 ms below tex's.
3. The first frame after release fades (`release_to_fade_ms` at least 6 ms below tex's), and `releaseframes.sql` frame 1 has no layer render.
4. Cancels end within 2 frames.
5. perf.sh's `pushpop_gesture` rows are no worse than tex beyond the spread of its two blocks, and `back_during_push` passes.
6. The held shots are identical to tex, and the recordings show no seam or flash.
7. All tests are green.

If one sub-change fails, its own signature names it (1 → a, 2 → b, 3 → c, 4 → d). Drop or fix that one alone; no new build is needed to find it.

**Adopt C2** on top of C1 when all of these hold:
1. On slow starts, the first gesture frame's GPU is at least 3 ms below c1's median, and `start_to_shrink_ms` is at least 8 ms below.
2. `presentlat.sql` `avg_1_10` is at least 5 ms below c1's.
3. On fast swipes it is not worse than c1 by more than 2 ms on either.
4. Exactly one texture render per edge touch, none while an edge scroll moves, and the layer released within 200 ms of the touch's end (`gfx-edgeafter`).
5. perf.sh's `hitch_ms_per_s` is not worse than c1's beyond the spread. A prewarm frame adds one present before the gesture, so read `hitchpos.py`'s first-three bucket.
6. Otherwise ship T + C1, and record C2's numbers.

### 5.8 Risks of the experiment itself

- **Script availability.**
  - `input motionevent CANCEL` needs Android 12 or later (the phone is on SDK 37).
  - `power/gpu_frequency` may be missing from this kernel; Perfetto only warns.
  - If `bgsteps.sh` overruns 90 s, raise `duration_ms`. The `steps.txt` stamps show where it ended.
- **perf.py's own reading of C2.**
  - perf.py's first-frame rows read dump frame 0. In c2 that can be the prewarm frame, which shows identical pixels.
  - Judge c2's start on the trace (`backlat`, `presentlat`), not on `late_first_frames` or `gesture_first_frame_ms`.
- **New code under the baseline profile.** The candidates' new methods are covered by the wildcard rules in every build. A shipped C1 or C2 needs WP04's profile re-recorded.
- **T already committed in a reworded form.** If WP04 commits T with wording that differs from the patch, `git apply -R` stops the ladder.
  - Build `bg-head` from the parent of WP04's texture commit instead: find the commit with `git log -1 --format=%H -S"CompositingStrategy.Offscreen" -- <BackCard.kt>`, then run `HEAD_BASE=<that commit>^ build-ladder.win.sh <B>`.
  - Write `c1.patch` against the committed T.

---

## 6. Not in this round

- **Fold the scrim into the card's fade.** This is the lever for the 6–7 GPU-late fade frames, if they become the largest late bucket after C1 and C2.
  - Draw the card's texture with a color matrix that scales RGB by a / a′, at layer alpha a′ = 1 − (1 − a)(1 − s).
  - The page beneath then needs no scrim under the card.
  - That is about 0.8 of a full-screen fill less per fade frame.
  - Needs: `GraphicsLayerScope.colorFilter` (check it with `javap` on ui 1.12.1), the strips' scrim clipped out of the card's rounded rect in its corner bands, and a pixel test.
- **Cover cards.** The shell is drawn whole under the cover card, with a full-screen scrim. No perf scenario covers it.
- **Bar-hiding destinations.** A bar-sized `saveLayer` every gesture frame. Not in the sample.
- **`HostCrossfade`.** The one whole-host fade, 200 ms at most once a session, is rule 1's case, a transient offscreen pass every frame. The same Google technique (`Offscreen` for the fade's length) would apply. It is outside the back gesture.

---

## 7. Files

| Path | What |
|---|---|
| `research/back-google/DESIGN.md` | this design |
| `research/back-google/bin/build-ladder.win.sh` | the four builds (§5.1) |
| `research/back-google/bin/instrument.py` | the trace counters and profile rules (§5.2); tested on copies |
| `research/back-google/bin/bg-phone.win.sh` | the phone session (§5.4) |
| `research/back-google/bin/back-trace.win.sh` | install, plus one Perfetto capture of `bgsteps.sh` |
| `research/back-google/bin/bgsteps.sh` | the device-side steps, with uptime stamps |
| `research/back-google/bin/heldmem.win.sh` | held screenshot and GPU / process memory |
| `research/back-google/bin/rec-cancel.win.sh` | a recording of a slow cancel |
| `research/back-google/sql/presentlat.sql` | vsync-to-present per gesture frame (§3.3) |
| `research/back-google/sql/layerts.sql` | texture renders with uptime stamps |
| `research/back-google/sql/backlat.sql` | latency, start, release and cancel from the counters |
| `research/back-google/sql/releaseframes.sql` | the commit's and the cancel's first frames |
| `research/back-google/sql/{layers,commitframes,commitmain,gestureframes,names}.sql` | the Google-code research's queries (T's two renders, the frame pattern of §3.1) |
| `research/back-google/patches/c1.patch`, `c2.patch` | **to be written** (§5.1) |

- None of the scripts has run on the phone yet.
- The shell scripts pass `bash -n`.
- `instrument.py` was run on copies of HEAD's `BackCard.kt`, `StackBackHandler.kt` and `baseline-prof.txt`, and T still applies after it.
