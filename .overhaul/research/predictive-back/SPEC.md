# Predictive back like Gmail: specification (owner WP04; WP03 for sheets; WP07 parity)

Written 2026-10-09 for REQUIREMENTS 24, with 19, 21 and 22 as constraints. The inputs are
`research/ios-motion.md` §3 (the recordings and the AOSP/MDC reading), `research/compose-perf.md` §2.2–2.3 (why the
screen beneath must already be laid out) and `research/recomposition.md` §7.3 (where gesture progress may be read). The
plans that depend on this file are `research/SMOOTHNESS-PLAN.md` (S1) and `research/MOTION-PLAN.md` (M1).

Tags: **[R]** recorded on the Redmi (clips in `gmail/`). **[S]** read in source. **[S✓]** re-checked in source on
2026-10-09 for this spec (the MDC raw file, or `javap` on the exact AAR in `~/.gradle/caches`). **[D]** derived here.
**[U]** unverified; measure it before relying on it.

---

## 0. Decisions

1. **Gmail's in-app card**, not Navigation 3's seeked slide. During the gesture the top screen shrinks about its
   centre into a rounded card that follows the finger. It moves toward the far edge until an 8 dp gap remains, and it
   follows the finger's vertical drift by up to 24 dp. The screen beneath is static under a black 32 % scrim. On
   commit, the card returns to full size while it fades out in 100 ms, and then the stack pops with no transition. On
   cancel, the card follows the system's progress spring back to rest, and the scrim fades out over 100 ms.
2. The requirement text says the card "flies out on commit". Gmail does not do that [R]: it un-shrinks and fades.
   The default is **Gmail** (`BackCardCommit.Fade`). `BackCardCommit.FlyOut` (the system's cross-activity choreography)
   is a token option. `ReturnToSource` (the card shrinks back into the row it came from) is P2 and needs the optional
   shared-element feature.
3. **The shell owns the gesture.** `NavDisplay` is given a `NavigationEventState` that no handler is ever bound to, so
   it never enters its own predictive phase (the linear seek that ends on a `tween`, WP04 Deviation 3). One
   `NavigationBackHandler` per stack host drives the card.
4. **Back-to-home is the system's.** At the root of the start tab, with nothing presented, no handler is enabled, so
   the launcher plays its own animation, as requirement 24 asks.
5. **The Back key, 3-button Back, API < 34, reduce motion and `EDGE_NONE` progress get no card.** They get the pop
   slide (MOTION-PLAN M2). Reduce motion pops with a 150 ms `ModulateAlpha` fade.
6. **Zero recomposition per gesture event.** Progress, Δy, the commit and the scrim are read only in `graphicsLayer`,
   `drawWithContent` or `layout` blocks. The handler composable recomposes at most when `isBackEnabled` flips.

---

## 1. What Gmail does on the reference phone [R]

Gmail 2026.09.21, conversation → inbox, single activity. Clips are in `gmail/`: `slow_commit.mp4`, `slow_cancel.mp4`,
`fast_flick.mp4`, `y_follow.mp4`, `to_home_slow.mp4`, `to_home_commit.mp4`, and the strip `key_frames.png`.
`diag_cancel.mp4` is invalid: no back gesture started. The full measurements are in `ios-motion.md` §3.1. In short:

| Observation | Value |
|---|---|
| Card scale at a 44.8 % swipe | 0.911–0.914. The MDC formula predicts 0.9135 |
| Far-edge gap | 7–8 frame px, about **8 dp** |
| Vertical follow | +7.3 dp for Δy = +30 % H, and −6.9 dp for −25 % H. That is linear, up to 24 dp |
| Corners at full shrink | about 72 / 60 device px (top / bottom), so the collapsed size is 28 dp |
| Screen beneath | static: no scale and no shift |
| Scrim | black **0.32**, fully on about 50 ms after the gesture starts, then constant |
| Commit | the card returns to full size while fading over about 100 ms. The scrim reaches 0 in about 117 ms. The whole visible commit takes 170–270 ms, including Gmail's row collapse |
| Cancel | the system decides once the finger reverses about 36 dp. The card springs back in about 100 ms |
| Back to home (inbox root) | Pixel Launcher's window shrink: about 0.79 at a 40 % swipe |
| Bottom navigation | stays where it is. The card is the content area above it |

Not recorded: dark mode. Recording it would need the user's theme switched, so the dark scrim stays [U] at 0.32.

---

## 2. Constants, with sources

| Token (`BackCard.*`) | Value | Source |
|---|---|---|
| `ProgressEasing` | `CubicBezierEasing(0.1f, 0.1f, 0f, 1f)` applied to `event.progress` | [S] AOSP `BackGestureInterpolator`; MDC `MaterialBackAnimationHelper` (ios-motion P4, M2); consistent with [R] |
| Progress input | the system's `progress = Δx / maxDistance`, linear up to 412 dp. The Redmi is 392.7 dp wide, so progress is Δx / W | [S] AOSP `BackTouchTracker`, `dimens.xml` 412 dp, factor 0.2 (P6, P7) |
| `MIN_SCALE` | **0.90**, about the centre | [S✓] MDC `MaterialMainContainerBackHelper.MIN_SCALE = 0.9f`; AOSP `CrossActivityBackAnimation` MAX_SCALE 0.9; [R] 0.911–0.914 at 44.8 % |
| `EdgeGap` | **8 dp**: at full progress the far edge sits 8 dp from the screen edge | [S✓] MDC `m3_back_progress_main_container_min_edge_gap = 8dp`; AOSP `cross_task_back_vertical_margin` 8 dp; [R] |
| Horizontal shift | `towards · max(0, (W − 0.9·W)/2 − 8 dp) · p`. On the Redmi that is 11.6 dp at p = 1 | [S✓] MDC `translationX = lerp(0, availableHorizontalSpace, progress) · (leftSwipeEdge ? 1 : −1)` |
| `MaxFollowY` | **24 dp**, linear in `abs(Δy) / H`, and never more than the room `(H − scale·H)/2 − 8 dp` | [S✓] MDC `m3_back_progress_main_container_max_translation_y = 24dp`; [R] |
| `Corner` | `lerp(0, 28 dp, p)`, **uniform** on all four corners | [R] collapsed 28 dp (M3 extra-large shape). Gmail starts its top corners from the display radius; uniform corners keep the clip a hardware outline (ios-motion §2.3). The difference at p = 1 is about 3 dp [D] |
| `SCRIM_ALPHA` | **0.32** black, over the screen beneath and under the card | [R] (luminance 239.7 → 162–164). Dark mode [U] |
| `SCRIM_IN_MILLIS` | **50**, linear, from the first progress event | [R] about 3 frames |
| `COMMIT_FADE_MILLIS` | **100**, linear, content alpha 1 → 0 with `CompositingStrategy.ModulateAlpha` | [R] about 100 ms |
| `SCRIM_OUT_MILLIS` | **120** on commit, **100** on cancel, linear | [R] 117 ms; MDC cancel `motionDurationShort2` 100 ms [S] |
| Commit un-shrink | `AppTheme.springs.screen.unit`, seeded with the release velocity | [D]. The user asked for springs; the card is invisible before the spring matters |
| Release velocity | `VelocityTracker1D(isDataDifferential = false)` over `(frameTimeMillis, progress)`, clamped to **1–4 progress/s** | [D] from AOSP `ProgressVelocityTracker` and its velocity floor below 10 % progress |
| Cancel motion | none of our own: the system springs progress back to 0 (stiffness 1500, ζ 1) before `onBackCancelled` | [S] AOSP `BackProgressAnimator` (P5); [R] about 100 ms |
| `FlyOut` option | scale → 1, moved by the edge shift + **96 dp** toward the swipe direction, alpha 0 by **90 ms**, on `screen` with velocity | [S] AOSP `DefaultCrossActivityBackAnimation` (450 ms EMPHASIZED there) |
| Sheet variant | the bottom container: scaleX by up to **48 dp**, scaleY by up to **24 dp**, origin bottom centre | [S✓] MDC `m3_back_progress_bottom_container_max_scale_{x,y}_distance` |
| Side-sheet variant (rail or drawer, if one is ever added) | shrink 24 dp, grow 12 dp, y 48 dp | [S✓] MDC `m3_back_progress_side_container_*` |
| Back to home | the system's. The template does nothing | [S] Launcher3 `MIN_WINDOW_SCALE` 0.85; [R] Pixel about 0.79 at 40 % |

Verified on 2026-10-09: the MDC constants above from
`https://raw.githubusercontent.com/material-components/material-components-android/master/lib/java/com/google/android/material/motion/MaterialMainContainerBackHelper.java`
and `.../motion/res/values/dimens.xml`.

---

## 3. Curves and poses

`p = ProgressEasing(event.progress)` during the gesture. `u ∈ [0, 1]` is the commit's unwind, so the drawn pose
is `q = p · (1 − u)`. With `W`, `H` the entry's size:

```
scale        = 1 − (1 − MIN_SCALE) · q
translationX = towards · max(0, W · (1 − MIN_SCALE) / 2 − EdgeGap) · q          towards = +1 for EDGE_LEFT, −1 for EDGE_RIGHT
translationY = sign(Δy) · min(MaxFollowY, max(0, H · (1 − scale) / 2 − EdgeGap)) · min(1, |Δy| / H) · (1 − u)
corner       = Corner · q                                                       (cached RoundedCornerShape per whole px)
contentAlpha = 1 → 0 over COMMIT_FADE_MILLIS on commit                          (ModulateAlpha)
scrim        = 0 → SCRIM_ALPHA over SCRIM_IN_MILLIS at gesture start; → 0 over SCRIM_OUT_MILLIS on commit or cancel
```

| Phase | What moves | Timing |
|---|---|---|
| Gesture start (first `InProgress`) | the screen beneath is shown (it is already laid out, §4.3); the scrim starts; the velocity tracker resets | the frame of the first event |
| Gesture | the card pose follows `p` and Δy, one frame per event (`snapshotFlow` conflates to the frame) | — |
| Commit (`onBackCompleted` while the card is active) | `u` springs 0 → 1 on `screen`, seeded with the velocity; `contentAlpha` reaches 0 at 100 ms; **pop at 100 ms** with no transition; the scrim finishes at 120 ms, drawn over the new top | 100 ms to the pop; 120 ms visible |
| Cancel (`onBackCancelled`) | progress is already back at 0 (the system's spring); the scrim fades in 100 ms; then the beneath screen is parked again | 100 ms |
| Back key, 3-button, API < 34, `EDGE_NONE` | no card: `gate.onBack(pop)`, the slide pop | `screen` spring |
| Reduce motion | no card: on completion the top fades out over 150 ms (`ModulateAlpha`) and then pops | 150 ms |
| RTL | the direction comes from `swipeEdge` only. Layout direction does not change it | — |

`EDGE_NONE` matters because on newer Android a 3-button Back can arrive as progress animated to 1 with stiffness 200
[S] (`predictiveBackSwipeEdgeNoneApi`). The card must ignore it, so a button press never shrinks the screen.

---

## 4. Navigation 3 1.2.0 design

### 4.1 Verified APIs (`javap` on `navigation3-ui-android-1.2.0.aar`, `navigationevent{,-compose}-android-1.2.0.aar`) [S✓]

```kotlin
// androidx.navigation3.scene
fun <T> rememberSceneState(entries: List<NavEntry<T>>, sceneStrategies: List<SceneStrategy<T>>,
    sceneDecoratorStrategies: List<SceneDecoratorStrategy<T>>, sharedTransitionScope: SharedTransitionScope?,
    onBack: () -> Unit): SceneState<T>
fun <T> rememberNavigationEventState(sceneState: SceneState<T>): NavigationEventState<SceneInfo<T>>
fun <T> NavigationBackHandler(sceneState: SceneState<T>, navigationEventState: NavigationEventState<SceneInfo<T>>,
    onBackCancelled: () -> Unit = …, onBackCompleted: () -> Unit = …)          // what the entries overload uses
// androidx.navigation3.ui
fun <T> NavDisplay(sceneState: SceneState<T>, navigationEventState: NavigationEventState<SceneInfo<T>>,
    modifier, contentAlignment, sizeTransform, transitionSpec, popTransitionSpec, predictivePopTransitionSpec)
// androidx.navigationevent.compose
fun NavigationBackHandler(state: NavigationEventState<*>, isBackEnabled: Boolean, onBackCancelled: () -> Unit,
    onBackCompleted: () -> Unit)
fun <T : NavigationEventInfo> rememberNavigationEventState(currentInfo: T, backInfo: List<T> = …, forwardInfo: List<T> = …)
class NavigationEventState<T> { val transitionState: NavigationEventTransitionState /* snapshot state */ }
class NavigationEventTransitionState.InProgress(val latestEvent: NavigationEvent, val direction: Int)
class NavigationEvent(val swipeEdge: Int, val progress: Float, val touchX: Float, val touchY: Float, val frameTimeMillis: Long)
// EDGE_LEFT, EDGE_RIGHT, EDGE_NONE
class DirectNavigationEventInput { backStarted(e); backProgressed(e); backCancelled(); backCompleted() }   // tests
```

In the bytecode, the entries overload of `NavDisplay` calls `rememberSceneState`, then
`rememberNavigationEventState(sceneState)` and `scene.NavigationBackHandler(sceneState, navState, …)`, and then the
`SceneState` overload. **The `SceneState` overload registers no handler.** So the shell can pass it a state that
nothing drives.

### 4.2 Wiring in `StackHost` (today `StackHost.kt:107-116`)

```kotlin
val sceneState = rememberSceneState(entries, strategies, emptyList(), null, onBack = { gate.onBack(pop) })
// Bound to no handler, so its transitionState stays Idle: NavDisplay never seeks a predictive pop.
val inert = rememberNavigationEventState(sceneState)
NavDisplay(
    sceneState = sceneState,
    navigationEventState = inert,
    modifier = modifier,
    transitionSpec = …,                       // unchanged for scenes NavDisplay still animates (list-detail, presentation)
    popTransitionSpec = …,
    predictivePopTransitionSpec = { NavTransitions.none },
)
BackCardHandler(card, stack, gate, springs = AppTheme.springs, reduceMotion = AppTheme.reduceMotion)
GateEffects(gate, pop)                        // unchanged: the consuming handler while gate.moving
```

`entries` keep their decorators (`rememberDecoratedNavEntries`, `StackHost.kt:87-91`). With this overload the
`onBack` passed to `rememberSceneState` serves scenes that call back themselves, such as dialogs. Back from the system
reaches only `BackCardHandler`, `GateEffects`, `ShellRootBack` and `PresentationHost`.

### 4.3 The screen beneath must already be laid out

This is the whole difference between a smooth gesture start and a dropped frame. Measuring the screen beneath on the
first progress event costs what a pop's first frame costs today: 15.0 ms median and 36.9 ms max on the Redmi
(`perf/WP04/phase2/summary.md`), which is a missed frame at 120 Hz.

- **Target (B), with SMOOTHNESS-PLAN S1's persistent `StackScene`.** Entries N−1… stay attached, measured and placed
  in one parent, parked (alpha 0, off-window). Gesture start unparks N−1 at x = 0 under the scrim: two layer
  properties. Commit pops with no transition: the top leaves and N−1 is already the screen on display, in the same
  `LayoutNode`. The frame after the pop is pixel-identical and costs a few ms [U, measure].
- **Interim (A), if S1 lands later.** `KeepUnderScene` keeps N−1 composed but neither measured nor placed
  (`KeepUnderScene.kt:84`), and that drops its layers (compose-perf §2.2). Extend `arriving` (`:77-79`) to
  `… || card.active` and place it at x = 0 instead of the parallax position. The first gesture frame then measures
  N−1: expect one heavy frame at p ≈ 0, where the visual change is tiny. Commit pops with
  `StackChange.GesturePop`, which `NavTransitions.pop` maps to `none` (read `gate.lastChange` in the spec lambda).
  Navigation 3 moves N−1 into the next scene through its movable content: the image is identical, but the frame pays
  a detach and a re-layout. (A) is acceptable only as a stepping stone. Gate (B).

### 4.4 State, modifiers and handler (sketch; names follow `core/navigation`)

```kotlin
/** Gmail's in-app predictive back: Material Components' main-container helper, constants in SPEC §2. */
internal object BackCard {
    const val MIN_SCALE = 0.9f
    val EdgeGap = 8.dp
    val MaxFollowY = 24.dp
    val Corner = 28.dp
    const val SCRIM_ALPHA = 0.32f
    const val SCRIM_IN_MILLIS = 50
    const val COMMIT_FADE_MILLIS = 100
    const val SCRIM_OUT_MILLIS = 120
    const val CANCEL_MILLIS = 100
    const val MIN_COMMIT_VELOCITY = 1f
    const val MAX_COMMIT_VELOCITY = 4f
    val ProgressEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
}

@Stable
internal class BackCardState {
    /** Eased gesture progress. Read in graphicsLayer and draw only. */
    var progress by mutableFloatStateOf(0f)
        private set
    var deltaY by mutableFloatStateOf(0f)
        private set
    var fromRightEdge = false
        private set
    /** True from the first progress event until the card is gone. Read in layer and layout blocks only. */
    var active by mutableStateOf(false)
        private set
    val unwind = Animatable(0f)
    val contentAlpha = Animatable(1f)
    val scrim = Animatable(0f)

    private var startY = Float.NaN
    private val tracker = VelocityTracker1D(isDataDifferential = false)
    private var scrimJob: Job? = null

    fun track(event: NavigationEvent, scope: CoroutineScope) {
        if (!active) {
            active = true
            fromRightEdge = event.swipeEdge == NavigationEvent.EDGE_RIGHT
            startY = event.touchY
            tracker.resetTracking()
            scrimJob = scope.launch { scrim.animateTo(BackCard.SCRIM_ALPHA, tween(BackCard.SCRIM_IN_MILLIS, easing = LinearEasing)) }
        }
        progress = BackCard.ProgressEasing.transform(event.progress)
        deltaY = event.touchY - startY
        tracker.addDataPoint(event.frameTimeMillis, event.progress)
    }

    /** Runs the commit until the card is invisible; the caller pops then. The scrim keeps fading over the new top. */
    suspend fun commit(spring: SpringSpec<Float>, scope: CoroutineScope) = coroutineScope {
        val v = tracker.calculateVelocity().coerceIn(BackCard.MIN_COMMIT_VELOCITY, BackCard.MAX_COMMIT_VELOCITY)
        val unwinding = launch { unwind.animateTo(1f, spring, initialVelocity = v) }
        scrimJob?.cancel()
        scrimJob = scope.launch { scrim.animateTo(0f, tween(BackCard.SCRIM_OUT_MILLIS, easing = LinearEasing)) }
        contentAlpha.animateTo(0f, tween(BackCard.COMMIT_FADE_MILLIS, easing = LinearEasing))
        unwinding.cancel()
    }

    suspend fun cancel() {
        scrimJob?.cancel()
        scrim.animateTo(0f, tween(BackCard.CANCEL_MILLIS, easing = LinearEasing))
        reset()
    }

    suspend fun finish() {
        scrimJob?.join()
        reset()
    }

    private suspend fun reset() {
        active = false
        progress = 0f
        deltaY = 0f
        unwind.snapTo(0f)
        contentAlpha.snapTo(1f)
        scrim.snapTo(0f)
    }
}

/** On the top entry's layer. Every value is read here, so a gesture recomposes nothing. */
internal fun Modifier.backCard(card: BackCardState): Modifier = graphicsLayer {
    if (!card.active) return@graphicsLayer                  // the scope is reset to identity before each run
    val q = card.progress * (1f - card.unwind.value)
    val scale = 1f - (1f - BackCard.MIN_SCALE) * q
    val towards = if (card.fromRightEdge) -1f else 1f
    val shiftX = max(0f, size.width * (1f - BackCard.MIN_SCALE) / 2f - BackCard.EdgeGap.toPx())
    val roomY = max(0f, size.height * (1f - scale) / 2f - BackCard.EdgeGap.toPx())
    val followY = min(BackCard.MaxFollowY.toPx(), roomY) * min(1f, abs(card.deltaY) / size.height)
    scaleX = scale
    scaleY = scale
    translationX = towards * shiftX * q
    translationY = sign(card.deltaY) * followY * (1f - card.unwind.value)
    alpha = card.contentAlpha.value
    compositingStrategy = CompositingStrategy.ModulateAlpha  // 100 ms full-screen fade with no offscreen buffer
    shape = CardShapes.of((BackCard.Corner.toPx() * q).roundToInt())
    clip = q > 0f
}

/** One shape per whole-pixel radius (at most about 80 on a 2.75 density), so a gesture allocates none per frame. */
private object CardShapes {
    private val cache = MutableIntObjectMap<Shape>()
    fun of(px: Int): Shape = cache.getOrPut(px) { RoundedCornerShape(px.toFloat()) }
}

/** On the entry beneath: drawn over it and under the card. */
internal fun Modifier.backScrim(card: BackCardState): Modifier = drawWithContent {
    drawContent()
    val a = card.scrim.value
    if (a > 0f) drawRect(Color.Black, alpha = a)
}

@Composable
internal fun BackCardHandler(card: BackCardState, stack: AppBackStack, gate: BackGate, springs: AppSprings, reduceMotion: Boolean) {
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    val scope = rememberCoroutineScope()
    val pop = remember(stack) { { count: Int -> stack.pop(count) } }
    LaunchedEffect(state, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        snapshotFlow { state.transitionState }
            .filterIsInstance<NavigationEventTransitionState.InProgress>()
            .filter { it.latestEvent.swipeEdge != NavigationEvent.EDGE_NONE }   // a button press never shrinks the screen
            .collect { card.track(it.latestEvent, scope) }
    }
    NavigationBackHandler(
        state = state,
        isBackEnabled = stack.canGoBack && !gate.moving,       // GateEffects owns Back while anything moves
        onBackCancelled = { scope.launch { card.cancel() } },
        onBackCompleted = {
            if (!card.active) {
                gate.onBack(pop)                                // Back key, 3-button, API < 34, reduce motion
            } else {
                scope.launch {
                    gate.hold {                                 // presses meanwhile are counted, then applied
                        card.commit(springs.screen.unit, scope)
                        if (pop(1) > 0) gate.changed(StackChange.GesturePop)
                    }
                    card.finish()
                }
            }
        },
    )
}
```

The new `BackGate` API is `suspend fun hold(block: suspend () -> Unit)`: report `moving` for the duration through the
existing reporter set (`BackGate.kt:27`, `report` at `:67`), then release. The new enum value is `StackChange.GesturePop`.
`ReportMotion` stays as it is for the slide pops.

### 4.5 Where each modifier goes

| Element | (B) `StackScene` | (A) interim `KeepUnderScene` |
|---|---|---|
| Top entry | `entryLayer(…)` composes `backCard(card)` into the same `graphicsLayer` block while `card.active` | `Box(Modifier.backCard(card)) { top.Content() }` at `KeepUnderScene.kt:98` |
| Entry beneath | unparked at x = 0 while `card.active`; `backScrim(card)` | `arriving \|\| card.active`; placed at 0; `backScrim(card)` |
| Frame rate | `Modifier.preferredFrameRate(FrameRateCategory.High)` on the stack box while a gesture or slide runs. On ARR devices a layer-only animation casts no vote (compose-perf §2.12) | same |
| `EntryFrame` cover scrim (`StackHost.kt:194`) | unchanged for the slide; draws nothing while the card is active (the card has its own scrim) | same |

---

## 5. How it composes with BackGate, the tab bar and presentations

### 5.1 Handlers per state (one dispatcher per kept tab, `KeptTabs.kt:75-92`)

| State of the visible tab's stack | `BackCardHandler` | `GateEffects` (consuming) | `ShellRootBack` | Result of a back gesture |
|---|---|---|---|---|
| Idle, depth ≥ 2 | **enabled** | off | — | Gmail card; commit pops one |
| A push, pop or commit is running (`gate.moving`) | off | **enabled** | — | counted; applied as one coalesced pop once settled (rapid Back, `BackGate.kt:11-23`, `settle` at `:82`) |
| Root of a non-start tab | off (`canGoBack` false) | off | **enabled** | no card; completion selects the start tab, with the tab reveal (MOTION-PLAN M24) |
| Root of the start tab, nothing presented | off | off | off | **system back-to-home** (requires `enableOnBackInvokedCallback`, already in the manifest) |
| Something presented | the tab's handlers keep their state, but `PresentationHost` registers later and wins while its stack is non-empty | — | — | cover: the same card on the presentation host's own `BackCardState`; sheet: §5.4 |

Only one of `BackCardHandler` and `GateEffects` is ever enabled, because their `isBackEnabled` conditions are each
other's negation. Registration order therefore does not matter between them. Each binds its own
`NavigationEventState`: the navigation-event skill forbids binding two handlers to one state (`IllegalArgumentException`).

### 5.2 Rapid Back and `nav_stress`

- A Back during a commit is counted (`gate.hold`) and applied after the pop as a slide pop. Its target is warm because
  S1 keeps N−2 parked. The phase-2 coalesced pop was 23.6 / 34.6 ms against an 8 / 16 ms gate.
- A second gesture during a commit goes to `GateEffects`. It is consumed and counted, never seeked.
- A tab switch during a commit is instant (`ShellState.select`). The leaving tab's commit finishes off screen, the
  existing rule for transitions in a leaving tab. The tab's dispatcher is disabled when the tab is hidden, so it
  receives no new gesture.
- iOS-parity retargeting (apply each Back immediately) is a later experiment on the persistent scene. It is not part of
  this spec.

### 5.3 The tab bar

- **Default (the bar shows at every depth):** the bar is chrome above the tab host, so it stays where it is, drawn over
  the card. This is what Gmail's bottom navigation does [R]. Nothing changes in `ShellBar`.
- **`FullScreenKey` top, or `hideBarOnPush`, over a screen that shows the bar:** the bar belongs to the screen beneath,
  so during the gesture it must appear with that screen, under the card. `BarAnchor` (`BarAnchor.kt:38-53`) gains a
  second follower besides the slide fraction: `cover: () -> CoverShape?`, the card's window rect and corner, written
  by the card. `FollowCoveredNode.draw` (`BarAnchor.kt:101-121`) then draws the bar at rest, clips out the card's
  rounded rect (a reused `Path`, `clipPath(path, ClipOp.Difference)`, bar-sized) and darkens it by the card's scrim
  through the existing bar-sized `saveLayer` with `SrcAtop`. On commit the cover goes away with the card, and
  `ShellBar`'s `covered` (`AppShell.kt:209-211`) becomes false on the pop frame. The bar is then already in place:
  no slide in, as in Gmail.
- The bar's `Parked` modifier (`AppShell.kt:258-263`) unplaces the bar while it is covered, which drops its layers.
  Change it to the same park used for screens (placed, alpha 0, semantics cleared) so the gesture's first frame does
  not re-record the bar.

### 5.4 Presentations

- **Covers** (`PresentationHost`): the same `BackCardHandler` with the host's own `BackCardState`. The scrim is drawn
  over the shell layer: the content plus the bar.
- **Sheets** (WP03, `AppSheet.kt:278-292` today tracks the finger linearly downward with `PredictiveBackHandler`):
  migrate to `NavigationBackHandler` (navigation-event skill checklist) and use the MDC bottom container. Set
  `transformOrigin = TransformOrigin(0.5f, 1f)`, `scaleX = 1 − 48 dp · p / W` and `scaleY = 1 − 24 dp · p / H`, read in
  the sheet surface's `graphicsLayer`. Commit runs the existing dismiss (`AppSheetState.dismiss`, a spring seeded
  with 0). Cancel springs back on `sheet`. iOS sheets track the finger down instead: keep the drag-down interactive
  dismiss for touch, and use the bottom-container shrink for the system gesture.

---

## 6. Per-frame cost and perf rules

| Rule | Why |
|---|---|
| The card is one `graphicsLayer` on the top entry: scale, translation, outline clip. No `shadowElevation`, no `RenderEffect`, no alpha during the gesture | layer properties only, with no re-record (ios-motion §2.3). A scaled full-screen shadow is regenerated every frame [U] |
| `ModulateAlpha` only for the 100 ms commit fade | `Auto` with alpha < 1 renders the whole screen offscreen every frame (compose-perf §2.9) |
| The beneath screen is already laid out (§4.3 B) | a measure on the first event costs 15–37 ms on the Redmi |
| The scrim is one blended full-screen rect, about 1 ms of GPU fill [U] | if `pushpop_gesture` GPU p90 exceeds 8 ms, draw it as four bands around the card |
| Corner shapes cached per px; no allocation per event | GC churn on the UI thread (compose-perf §2.13) |
| `preferredFrameRate(FrameRateCategory.High)` while active | ARR phones do not boost for layer-only motion |
| The baseline profile journey includes the card | `BaselineProfileGenerator`'s edge-swipe step must use a slow `input motionevent` swipe (as in `record-gmail-r3.sh`), so the card code is in the profile |

Budget on the Redmi (release, profile installed): `pushpop_gesture` janky ≤ 2 %, p99 ≤ 25 ms, GPU p90 ≤ 8 ms with
`dequeueBuffer` p90 = 0, **gesture-start frame ≤ 8 ms** (a new `perf.py` row: the slowest of the first three frames
after the DOWN that starts the edge swipe), commit-to-pop frame ≤ 8 ms, and 0 recompositions per progress event.

---

## 7. Verification

**Unit tests** (Robolectric, `core/navigation/src/test`, `DirectNavigationEventInput` attached to the test's dispatcher):

1. A started gesture shows the beneath entry and draws the card. The top layer's `scaleX` ≈ 0.9135 at progress 0.448
   (`ProgressEasing(0.448) = 0.865`), and the translation leaves an 8 dp far gap at progress 1.
2. Commit pops exactly once; the pop starts no transition; the beneath entry is the same `LayoutNode` before and after
   (S1's probe node counts `onDetach` = 0).
3. Cancel never pops and ends with the beneath entry parked.
4. A Back key during a commit lands one screen further once the commit ends (BackGate).
5. At the start tab's root no handler is enabled (`dispatcher.hasEnabledHandlers()` false, or the equivalent query).
6. `EDGE_NONE` progress never activates the card; its completion pops with the slide.
7. RTL: `EDGE_RIGHT` moves the card left.
8. FullScreenKey: during the gesture the bar is drawn, clipped by the card.

**Recomposition** (RECOMPOSITION-PLAN journey "Predictive back scrub"): `backStarted`, 30 × `backProgressed`, then
cancel; repeat with complete. Screen composables execute 0 times per event, and `BackCardHandler` at most twice.

**Device perf:** `$L adb tools/perf/perf.sh --variant devBenchmarkRelease --runs 3 --gate solid pushpop_gesture
rapid_back nav_stress pushpop` against §6's budget and `perf/WP04/phase2/summary.md`.

**Recording** (requirement 24 acceptance): adapt `slow_back`, `fast_flick` and the cancel variant from
`research/predictive-back/record-gmail-r3.sh` to the template sample (list → detail, `input motionevent` from x = 2)
with `screenrecord --size 720x1600`. Write `perf/WP04/predictive-back/{slow_commit,slow_cancel,fast_flick,y_follow,to_home}.mp4`
plus a key-frame strip, and check it against these numbers. They were measured from the Gmail clips, which stay
off GitHub because they show real emails; these numbers and §1 are the reference (`gmail/key_frames.png` exists only
in the user's Drive backup and is not needed):

- scale 0.91 ± 0.005 at a 45 % swipe
- far-edge gap 8 dp (≈ 15 px at 720 wide)
- vertical follow ≤ 24 dp
- corners about 25 dp at full shrink
- scrim on within 50 ms, at luminance ≈ 0.68 × the beneath's
- visible commit ≤ 120 ms
- cancel about 100 ms

Repeat in dark theme and in RTL (pseudo-locale `ar-XB`). At the start tab's root, `to_home` must show the launcher's
own animation, the same as Gmail's `to_home_slow.mp4`.

---

## 8. Upstream path and open points

- Navigation 3 1.3.0-alpha01 moves to `DeferredAnimatedContent`, which hands off from the predictive spec to
  `popTransitionSpec` on release; compose-animation 1.13 adds the catch-up spring (ios-motion §3.8). When both are
  stable, re-evaluate whether the card can be a `ContentTransform` plus an entry-level `graphicsLayer`. Until then the
  shell owns the gesture.
- [U] Gmail's dark-mode scrim. [U] The GPU cost of the full-screen scrim on Adreno 618. [U] Whether HWUI's
  `prepareTree` skips texture uploads for alpha-0 parked nodes, which would show up on the first unpark frame.
- [U] The Pixel Launcher's back-to-home minimum scale. It does not matter for the template, because the system plays it.
- Compose Multiplatform (WP07): the Android target reuses this file unchanged (navigationevent is multiplatform). The
  iOS target has no system back gesture. It needs an interactive edge-swipe pop instead (MOTION-PLAN M3, iOS: commit
  at ≥ 1 W/s or past 50 %, on the `screen` spring with the velocity).
