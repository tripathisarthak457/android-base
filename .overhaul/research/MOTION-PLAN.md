# Motion plan: every iOS motion, its parameters, its implementation and its owner

Written 2026-10-09 for REQUIREMENTS 2, 21, 24, 28 and 29, under the constraints of 19 and 22 (every animation is
interruptible, keeps its velocity, runs in layout or draw with no per-frame recomposition, and passes the perf gates).
Sources: `research/ios-motion.md` (designs and the iOS/Android parameters), `research/ios-motion-catalogue.md`
(derivations, §1 spring maths, §5 deltas), `research/predictive-back/SPEC.md`, `research/SMOOTHNESS-PLAN.md`, the QA
notes `qa/r1/for-WP04.md` and `for-WP03.md`, and the code:

- WP03 tree `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` (shown as
  `ds/`);
- WP04 tree `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/` (shown as
  `nav/`), plus WP04's `AppTabBar` at `wt/WP04/…/designsystem/component/navigation/AppTabBar.kt` (`bar/AppTabBar.kt`).

Status: **APPLIED** / **PARTIAL** / **MISSING**, with file:line. Tags as in the research: [A] Apple, [S] source, [E]
eyeballed or measured by a third party, [R] recorded on the Redmi, [D] derived, [U] unverified. CP1 (`decisions/CP1.md`,
not written yet) decides the values marked **CP1**.

---

## 1. Springs: one table, consistent with `AppTheme.springs`

`AppSpring(response, bounce)` (`ds/theme/AppMotion.kt:41-55`) gives `stiffness = (2π / response)²` and
`dampingRatio = 1 − bounce`. With a negative bounce that is ζ > 1. Note that SwiftUI uses `1 / (1 + bounce)`; the token
values below are written for the template's formula. Values are for `AppMotionStyle.Standard`; `speed` divides the
response of the travelling springs (marked ÷speed).

| Token | Today (`AppMotion.kt`) | Target | Compose `spring(ζ, k)` | Used by | Source |
|---|---|---|---|---|---|
| `press` | 0.16 s, 0 (`:76`) | keep | 1, 1542 | press dip | [D] |
| `release` | 0.32 s, style bounce (`:79`) | keep | 0.85, 386 | press release | [A] bounce ≈ 0.15 not felt as bouncy |
| `selection` | 0.30 s, 0.6 × bounce (`:82`) | **retire into `thumb`** | — | — | catalogue §5: bounce belongs on release, not on indicators |
| **`thumb`** (new) | — | 0.28 s, 0 | 1, 504 | switch and segmented thumbs, tab reveal, bar minimize settle, page dots | [M/E] iOS segmented thumb k 503.55 |
| `screen` ÷speed | 0.24 s, 0 (`:85`); WP04 duplicates it as `ScreenMotion.DEFAULT_RESPONSE` (`nav/NavTransitions.kt:48`) | **CP1** among 0.24 / 0.30 / **0.344 "iOS-exact"** | 1, 685 / 439 / 333 | push, pop, covers, back-card commit, keyboard fallback | [E]+[D] ω₀ 18.26, catalogue §1.3 |
| `sheet` ÷speed | 0.34 s, 0 (`:88`) | keep (it matches the iOS page curve) | 1, 342 | sheet present and detents, toast, search focus | [D] |
| **`dialog`** ÷speed (new) | — | 0.275 s, 0 | 1, 522 | alert, action sheet, menu, popover | [M] Flutter iOS k 522.35 |
| `layout` ÷speed | 0.36 s, 0 (`:91`) | keep | 1, 305 | list placement, expand and collapse | — |
| `emphasis` | 0.50 s, style bounce (`:94`) | keep | 0.85, 158 | badges, success marks, HUD | [A] `.snappy`-like |
| **`interactive`** (new) | — | 0.15 s, 0.14 | 0.86, 1755 | a value that trails the finger (lens, reorder lift) | [A] `interactiveSpring` |
| **`bounceBack`** (new) | — | 0.444 s, −0.1 | 1.1, 200 | pull-to-refresh settle; fling caught at an edge | [S][E] Flutter iOS ballistic (m 0.5, k 100, ζ 1.1) |
| **`overscrollRelease`** (new) | — | explicit `spring(158.1f, 9.8e6f)` | 158.1, 9.8 × 10⁶ | a rubber band let go with no velocity: e^(−λt), λ 9.9/s (half-life 70 ms) | [S][E] Flutter's overdamped pair |
| **`indicatorLead`** / **`indicatorTrail`** (new) | `SelectionSpring` ζ 0.91, k 438.6 (`bar/AppTabBar.kt:576`) | 0.22 s, 0.15 (0 under Calm) / 0.32 s, 0 | 0.85, 816 / 1, 386 | the tab indicator's two edges | [D] ios-motion §4.3 |
| **`iconBounce`** (new, two legs) | — | down `spring(1f, 1500f)` to 0.84, back `spring(0.45f, 500f)` | — | tab icon on select; symbol bounce | [A] SF Symbols `bounce`; magnitude [U], **CP1** |
| **`fling`** / **`flingFast`** (new, decay) | Compose spline default | `exponentialDecay(0.4767f)` / `exponentialDecay(2.393f)` | — | every DS scroller, pager, wheel | [A][D] 0.998 / 0.99 per ms |
| `fadeQuick` / `fadeSlow` | 120 / 200 ms (`:97-98`) | keep, for colour and opacity only | — | fades | — |
| `navigation.parallax` | 0.30 (`:127-132`; WP04 `nav/NavTransitions.kt:51`) | **CP1**: 0.333 (iOS 1/3) | — | covered screen | [E] |
| `navigation.scrimAlphaLight/Dark` | 0.10 / 0.24 | keep (iOS light 0.094; dark [U]) | — | cover scrim | [E] |
| edge shadow alpha | 0.12 (`nav/CoverScrim.kt:83`) | **CP1**: about 0.04, far fainter (iOS gradient 0.016 → 0) | — | push edge | [E] |

Rules for every package:

1. Every spring and decay comes from `AppTheme.springs`. Today WP04 has private copies: `ScreenMotion`
   (`nav/NavTransitions.kt:30-55`) and `SelectionSpring`/`SettleSpring` (`bar/AppTabBar.kt:576-577`). They move onto
   the tokens when WP03 merges (WP03 remaining work 5).
2. **Never a tween after a finger.** A gesture's release seeds a spring with the velocity:
   `Animatable.animateTo(…, initialVelocity = v)` or `animate(from, to, v, spec)`.
   - `SeekableTransitionState.animateTo(null)` loses velocity.
   - `AnchoredDraggableDefaults` uses a `tween()` unless it is given a spring.
   - Units: px/s for a pixel value; px/s ÷ travel for a 0–1 fraction (ios-motion §2.2).
3. **Project before snapping**: `target = nearest(position + v / 1000 · r / (1 − r))`, r = 0.998, or 0.99 for detents
   and paging.
4. Interrupting retargets. `animateTo` keeps the current velocity; never `snapTo` an animation the user can see.
5. Reduce motion: no parallax, no scale, no bounce. Slides become 150 ms `ModulateAlpha` fades; selection still moves.

---

## 2. Per-frame cost classes (the "cost rule" column below)

| Class | Allowed per frame | Example |
|---|---|---|
| **L0** | `graphicsLayer` properties only (translation, scale, rotation, outline clip, `ModulateAlpha`) | back card, slide, press |
| **L1** | re-placement only (`placeWithLayer`, `offset {}`) | overscroll, sheet offset, bar minimize |
| **D1** | re-recording a node no larger than a bar, a card or a control | tab indicator, switch track, spinner |
| **D2** | **one** full-screen blended rect, briefly | scrim, tab veil |
| forbidden | recomposition; re-measuring a full screen; an offscreen layer larger than a bar or card; `RenderEffect` over a full screen (glass excepted, under its budget); allocation in draw | — |

Each motion below also states its journey in `RECOMPOSITION-PLAN.md` §3. The rule is 0 executions in frames 2…N−1.

---

## 3. The motions

### M1. Predictive back like Gmail (REQUIREMENT 24) — WP04 (sheets WP03)

- **Status: MISSING.**
  - Navigation 3 seeks a linear slide (`nav/NavTransitions.kt:89-93`, wired at `nav/StackHost.kt:105,113`) and finishes
    a released swipe on its own `tween` (WP04 Deviation 3).
  - Sheets track the finger linearly with `PredictiveBackHandler` (`ds/component/overlay/AppSheet.kt:278-292`).
- **Parameters**: `predictive-back/SPEC.md` §2. Easing (0.1, 0.1, 0, 1); scale 0.90; 8 dp edge gap; 24 dp Y follow;
  corner 0 → 28 dp; scrim 0.32 in over 50 ms; commit = content fade over 100 ms + `screen` un-shrink seeded with the
  velocity; cancel = the system's spring + a 100 ms scrim fade. The Back key gets no card.
- **Implementation**: the shell owns the gesture: `NavDisplay(sceneState, inertNavState, …)` + `BackCardHandler`.
  The card is one `graphicsLayer` on the top entry; the beneath entry is parked and laid out ahead (SMOOTHNESS S1).
  Sheets use MDC's bottom container (48 dp x, 24 dp y).
- **Cost**: L0 + D2 (scrim). 0 recompositions per event. Gesture-start frame ≤ 8 ms.
- **Verify**: SPEC §7: unit tests with `DirectNavigationEventInput`; `perf.sh pushpop_gesture rapid_back nav_stress`;
  recordings compared with `gmail/key_frames.png`.

### M2. Push and pop with parallax and dim (Back key, up button) — WP04

- **Status: PARTIAL.**
  - APPLIED: incoming slides 100 %, outgoing slides by the parallax, slide only, no alpha (`nav/NavTransitions.kt:66-82`).
    The scrim and edge shadow are drawn over the visible strip only (`nav/CoverScrim.kt:57-80`). The bar travels with
    the screen it belongs to (`nav/BarAnchor.kt`).
  - MISSING: the iOS spring value (CP1); the start-after-compose clock (SMOOTHNESS S3); velocity on retarget, because
    the slide lives inside NavDisplay's `AnimatedContent`; a cached gradient (`CoverScrim.kt:68-76` allocates per frame).
- **Parameters**: `screen` (CP1, iOS-exact `1f, 333f`); parallax 0.333 (CP1); scrim 0.10 light / 0.24 dark; edge
  shadow about 0.04 (CP1). Reduce motion: a 150 ms fade.
- **Implementation**: in SMOOTHNESS S1's `StackScene`, one `slide` float animated by `animate(…, springs.screen.unit)`
  from `LaunchedEffect(changeId)`. The top layer has `translationX = slide · W`; N−1 has `−parallax · W · (1 − slide)`;
  the scrim is drawn by the top over N−1's visible strip. Optional iOS 26 card corners while moving: an outline clip with
  `WindowInsets.getRoundedCorner` radius, API 31+, off by default.
- **Cost**: L0 + D2 for the strip only. 0 recompositions in frames 2…N−1.
- **Verify**: `perf.sh pushpop` (janky ≤ 2 %, p99 ≤ 25 ms, hitch < 5 ms/s); `record.sh pushpop` strip against the
  catalogue §1.3 curve (54 % of travel at 100 ms for ω₀ 18.26).

### M3. Interactive edge-swipe back (iOS) — WP04 optional; WP07 iOS target

- **Status**: on Android, replaced by M1 (the system owns the edge). An optional full-content swipe (iOS 26) is
  **MISSING** and off by default.
- **Parameters**: iOS commits at ≥ 1 W/s or past 50 %; edge 20 pt [E]; then `screen` seeded with the velocity.
- **Implementation**: `draggable(Horizontal)` as a nested-scroll parent, driving the M1 card's state with synthetic
  progress, so horizontal scrollers win first. For the CMP iOS target (WP07) it is the primary back gesture, driving
  M2's `slide` directly.
- **Cost**: L0. **Verify**: recording; `pushpop_gesture` with the option on.

### M4. Rapid Back and interrupted transitions — WP04

- **Status: PARTIAL.**
  - APPLIED: BackGate counts presses during motion and pops them together at 99 % travel (`nav/BackGate.kt:11-23`,
    `:51-57`, `:82-88`, threshold `:94`). Measured: lands on the expected screen, 2.62 % janky.
  - MISSING: a warm target for a coalesced pop (23.6 / 34.6 ms; SMOOTHNESS S1 parked depth), and iOS-style
    retargeting.
- **Parameters**: iOS 18+ retargets in flight and never blocks [A].
- **Implementation**: keep coalescing. Once `StackScene` exists, test a `retarget` variant behind a flag: each Back
  applies at once, and `slide` animates toward the new target from its current value and velocity. Adopt it only if
  `rapid_back` and `nav_stress` stay green and the recordings show no snap.
- **Verify**: `RapidBackTest`; `perf.sh rapid_back nav_stress`; the frame strip at depth 4.

### M5. Zoom and hero transitions — WP04 (generator option), WP02-M3 (sample)

- **Status: MISSING.**
- **Parameters**: the source morphs into the destination on `screen`; interactive drag down to dismiss on iOS 18 [A].
- **Implementation**:
  - `SharedTransitionLayout` + `sharedBounds` with the default `scaleToBounds(ContentScale.FillWidth, Center)`. Never
    `RemeasureToBounds` on text.
  - Shapes through `clipInOverlayDuringTransition = OverlayClip(shape)`.
  - Navigation 3 takes the scope (`rememberSceneState(…, sharedTransitionScope, …)`, verified signature in SPEC §4.1).
  - `SharedTransitionLayout` makes everything under it pay a lookahead pass, so it is a **generator option**
    (`sharedTransitions`), off by default. The sample opens a row's detail with it when the option is on.
- **Cost**: lookahead on every frame under the layout. Measure the push composing frame with it on; the matched element
  renders in an overlay (L0).
- **Verify**: `perf.sh pushpop` with the option on vs off (≤ +1 ms on the composing frame); recording.

### M6. Modal sheets with detents and the card stack behind them — WP03 (sheet), WP04 (presenter)

- **Status: PARTIAL.**
  - APPLIED: detents with a velocity-carrying settle (`ds/component/overlay/AppSheet.kt:189-224`); a rubber band past
    the top detent (`:162-170`); a drag that re-places and never re-measures (`SheetTest`); `sheet` spring.
  - MISSING: the card stack (QA-11); the scrim fades with a full-screen layer alpha (`AppSheet.kt:312-317`,
    SMOOTHNESS S8); detent choice by projection (it uses a 1400 px/s threshold, `:195`, `:228`); `predictive back` (M1, sheet
    variant).
- **Parameters**:
  - Present and dismiss on `sheet`. Dismiss at ≥ 2 H/s or past 52 %.
  - Card stack at the large detent [E]: the presenter scales **0.9165** about its top centre and moves down by the
    status bar + 10 dp (iOS ≈ 7 % H); corners → 12 dp; a 10 % black overlay drawn as a rect. At medium: scrim only.
- **Implementation**:
  - WP03 exposes the sheet's presentation fraction as a `() -> Float`, annotated `@FrequentlyChangingValue`.
  - WP04 applies one `graphicsLayer` to the shell layer (content + bar) in `PresentationHost`/`ShellChrome`
    (`nav/AppShell.kt:178-187`): `scaleX/Y`, `transformOrigin = TransformOrigin(0.5f, 0f)`, `translationY`,
    `shape`/`clip`, all read from that fraction.
  - Scrims drawn as rects.
  - Projection with r = 0.99 picks the detent.
- **Cost**: L0 (presenter) + L1 (sheet offset) + D2 (scrim).
- **Verify**:
  - a sheet journey in `perf.sh`, once a template feature has a sheet destination (WP04 open issue: needs WP03's
    `SheetScene` + a sample sheet);
  - RECOMPOSITION-PLAN "Sheet drag": 0 per move;
  - the catalog "Feel" recording.

### M7. Context menu with a preview — WP03

- **Status: MISSING.** `AppMenu` is a popup that scales from 0.85 with layer alpha (`ds/component/overlay/AppMenu.kt:79-88`).
  There is no lifted preview and no backdrop.
- **Parameters**: 0.5 s long press, 10 pt slop; the preview lifts to ≤ 1.15 (≥ 1.02); backdrop blur σ 5 + a 40 % tint;
  a drag down shrinks it to 0.8; `LongPress` haptic; the menu appears on `dialog` [A][E].
- **Implementation**:
  - `detectTapGestures(onLongPress)`.
  - Snapshot the row with `rememberGraphicsLayer().record { }` and animate *that layer* in the overlay host (L0), so
    the list does not recompose.
  - Backdrop: one blurred 1/4-scale snapshot taken once (`RenderEffect` on API 31+), or a tint below 31. Never a live
    blur.
  - Dismiss with the source scale seeded with velocity.
- **Cost**: one-time snapshot + L0. GPU budget: the blur is rendered once, not per frame.
- **Verify**: catalog recording; RECOMPOSITION-PLAN "Context menu": the source list executes 0 times.

### M8. Large-title collapse — WP03

- **Status: PARTIAL.**
  - APPLIED: the title moves in the layer (`ds/component/navigation/AppTopBar.kt:158`); the inline title alpha is read
    in the layer (`:136-139`).
  - MISSING:
    - The inline title is **scrubbed** by scroll; iOS fades it over 150 ms once the large title passes under the bar.
    - The bar background switches on at a threshold with no fade (`:121-124`); iOS fades it over 10 pt of scroll.
    - No over-pull magnify.
- **Parameters**: bar 44 + 52 pt; title 34 pt bold → 17 pt semibold; inline fade 150 ms; background over 10 pt;
  over-pull magnify ≤ 1.1 anchored bottom-start [E][A].
- **Implementation**:
  - `remember { derivedStateOf { scrolled() >= area } }` read in a `LaunchedEffect` starts an `Animatable` (150 ms
    tween: opacity only), read in the layer.
  - The background alpha is `((scrolled − area) / 10.dp).coerceIn(0, 1)` in `drawBehind`.
  - Magnify: optional, from the overscroll offset (M10) once it is exposed.
- **Cost**: D1. **Verify**: RECOMPOSITION-PLAN "Scroll": `AppLargeTitleTopBar` executes 0 times per frame; recording.

### M9. Tab bar minimize on scroll — WP04

- **Status: PARTIAL.**
  - APPLIED: collapse read in layout and draw (`bar/AppTabBar.kt:106-151`); `scroll_minimize` 0.3 % janky.
  - MISSING:
    - The settle ignores the fling velocity: `onPostFling` calls `settleTo` with nothing (`bar/AppTabBar.kt:133-136`).
    - The settle spring is private (`:577`).
    - No hysteresis.
- **Parameters**: `thumb` with velocity; hysteresis 8–10 dp before the direction flips [U]; expand on a scroll up, on a
  tab tap, or at the top [A].
- **Implementation**: `settleTo(target, velocity = available.y / CollapseDistancePx)`, projected (r 0.99) to choose
  the target. Annotate `AppTabBarState.collapse` with `@get:FrequentlyChangingValue`.
- **Cost**: L1 + D1. **Verify**: `perf.sh scroll_minimize` (≤ 0.5 %, GPU p90 ≤ 7 ms); recording.

### M10. Rubber-band overscroll — WP03

- **Status: PARTIAL.**
  - APPLIED: the iOS band formula in placement (`ds/foundation/AppOverscroll.kt:91-99`, `:102-120`).
  - MISSING:
    - It is opt-in (`:28-46`).
    - A release while pulled springs back from velocity 0 on `layout` (`:77-83`).
    - A fling reaching the edge passes 0.12 × its leftover (`:85-88`, `:125`).
- **Parameters**: c 0.55, d = the viewport [S][E]. Release with velocity on `overscrollRelease`, or on `bounceBack`
  when the release carries velocity. A fling into the edge carries `min(leftover, 5000 px/s)` and is caught on
  `bounceBack`.
- **Implementation**:
  - `AppTheme` provides `LocalOverscrollFactory provides AppOverscroll` by default; iOS lists always bounce [A].
  - Keep Android's stretch as a style option.
  - `applyToFling` gets the velocity: `settle.animateTo(Offset.Zero, spring, initialVelocity = releaseVelocity)`.
- **Cost**: L1. **Verify**: RECOMPOSITION-PLAN "Overscroll bounce" 0; catalog recording of a fling into the top.

### M11. Scroll deceleration that matches iOS — WP03

- **Status: MISSING.** No DS scroller passes a `FlingBehavior`, and Compose has no local for the default fling [S].
- **Parameters**: `fling` = `exponentialDecay(0.4767f)` (0.998/ms); paging and wheels `flingFast` (2.393f, 0.99/ms);
  momentum accumulation `min(0.000816 · |v|^1.967, 40000) px/s` when a fling starts within a running one [S][E].
- **Implementation**: `AppScroll.flingBehavior()` (a `FlingBehavior` over `springs.fling`, keeping the last fling's
  live velocity for accumulation). It is passed by every DS wrapper (lists, grids, `AppPager`, `AppCarousel`,
  `AppWheelPicker`, `AppDataTable`) and documented for feature `LazyColumn`s. Pagers use
  `PagerDefaults.flingBehavior(state, decayAnimationSpec = springs.flingFast, snapAnimationSpec = springs.screen.unit)`, which projects with the fast (0.99) rate.
- **Cost**: none per frame beyond the scroll itself. **Verify**: `perf.sh scroll_root` stays ≤ 0.5 %; a fling of
  2000 px/s travels about v/λ = 2000/2.0 ≈ 1000 px [D]. Record against the catalogue.

### M12. Spring press states (REQUIREMENT 2) — WP03

- **Status: APPLIED.**
  - `Modifier.pressable` with `Scale` / `Dim` / `Highlight` / `None` (`ds/foundation/Pressable.kt:340-366`).
  - The scale is read in `placeWithLayer` (`:429-438`).
  - The dim uses `ModulateAlpha` (`:414-416`).
  - The tap timeout delays the press (`:298-302`).
  - `PressableTest` asserts zero recompositions.
- **Parameters**: `press` down, `release` up with velocity; scale 0.96 / 0.98 large / 0.90 compact; dim 0.45;
  highlight 220 ms fade.
- **Follow-up**: a press-down hook for SMOOTHNESS S3.4 prefetch (`onPressStart: (() -> Unit)?`), which WP02-M3 uses.
- **Cost**: L0. **Verify**: `PressableTest`; RECOMPOSITION-PLAN "Press feedback".

### M13. Switch — WP03

- **Status: PARTIAL.**
  - APPLIED: the thumb is drawn from an `Animatable` with a press widen (`ds/component/selection/AppSwitch.kt:136-137`)
    and haptics (`:66`).
  - MISSING:
    - The release drops the velocity: `snapTo(released)`, then `animateTo` from 0 (`:94-103`).
    - The commit threshold is 0.5, against iOS's 0.7 / 0.2 hysteresis.
    - The spring is `selection`, which has bounce.
- **Parameters**: 51 × 31 pt; the thumb widens 7 pt while pressed; drag commits at 0.7 and reverts at 0.2; `thumb`;
  `ToggleOn`/`ToggleOff` [E][A].
- **Implementation**: keep one node drawing the track and thumb (D1). On release, `animateTo(target, springs.thumb.unit,
  initialVelocity = v / travel)`.
- **Verify**: `SelectionTest` (a drag release continues at the finger's speed); catalog recording.

### M14. Segmented control — WP03

- **Status: PARTIAL.**
  - APPLIED: one thumb position read in draw, a drag across segments, a selection haptic
    (`ds/component/selection/AppSegmentedControl.kt:70-120`).
  - MISSING: two edges; the iOS spring; the pressed-thumb scale.
- **Parameters**: the thumb as two edges on `thumb` (`1f, 504f` both); pressed thumb 0.95; a selection haptic per
  crossing [M/E].
- **Implementation**: reuse the M24 `IndicatorEdges` helper with both edges on `thumb`, drawn in `drawBehind` (D1).
- **Verify**: `SegmentedControlTest`; RECOMPOSITION-PLAN "Switch / segmented / picker".

### M15. Picker wheel — WP03

- **Status: PARTIAL.**
  - APPLIED: snap fling (`ds/component/pickers/AppWheelPicker.kt:57`); a per-row cylinder in `graphicsLayer`
    (`:117-123`); a selection haptic on centring (`:85`).
  - MISSING: iOS decay (`flingFast`); the iOS cylinder constants.
- **Parameters**: diameter ratio 1.07, perspective 0.003, squeeze 1.45, off-centre opacity 0.447, row 32 pt [E].
- **Implementation**: `rememberSnapFlingBehavior(listState, decayAnimationSpec = springs.flingFast,
  snapAnimationSpec = springs.thumb.unit)`. Haptic at most once per frame.
- **Verify**: `WheelPickerTest`; catalog recording.

### M16. Alert scale-in — WP03

- **Status: PARTIAL.**
  - APPLIED: arrives from larger and leaves by fading (`ds/component/overlay/AppAlert.kt:89-97`).
  - MISSING:
    - The entrance is 1.12 → 1.0 (`:155`); iOS is 1.3 → 1.0.
    - The spring is `sheet`; iOS uses k 522.
    - The whole full-screen box, card included, fades with layer alpha (`:79-82`), which is offscreen every frame
      (SMOOTHNESS S8).
- **Parameters**: scale 1.3 → 1.0 + fade in on `dialog`; fade out only; scrim 0.20 light / 0.48 dark [M][E].
- **Implementation**: the scrim is `drawBehind { drawRect(scrim, alpha = shown) }`; the card's own layer animates
  `scaleX/Y` and alpha (card-sized; `ModulateAlpha` if its content allows).
- **Cost**: L0 (card) + D2 (scrim). **Verify**: Perfetto shows no full-screen `saveLayer`; `OverlayHostTest`; recording.

### M17. Action sheet (and the iOS 26 menu look) — WP03

- **Status: PARTIAL.**
  - APPLIED: slides up from its height (`ds/component/overlay/AppActionSheet.kt:92-96`).
  - MISSING: the spring is `sheet` (`:81`) rather than `dialog`; the scrim uses layer alpha (`:86-90`).
- **Parameters**: slide up on `dialog`; scrim as M16. iOS 26 popover look: scale from the anchor plus a pill →
  rounded-rect lerp, content fading in after about 40 % [U].
- **Implementation**: as M16. **Cost**: L0 + D2. **Verify**: recording; `OverlayHostTest`.

### M18. List insert, delete and reorder — WP03 (DS), WP02-M3 (features)

- **Status: PARTIAL.**
  - APPLIED: `appAnimateItem` (`ds/animation/AppTransitions.kt:143-152`: fade + `layout` placement); reorder lift
    with `animateItem` (`ds/component/collections/AppReorderableList.kt:91-105`).
  - MISSING: no feature list uses `appAnimateItem` (`SampleListScreen.kt:131`, `SearchScreen.kt:202`).
- **Parameters**: placement on `layout`; insert fade over 200 ms; delete fade over 120 ms; reorder lift about 1.03 +
  shadow + a `DragStart` haptic [U].
- **Implementation**: features call `Modifier.appAnimateItem()` on keyed rows. The reorder drag offset goes in the
  layer, and neighbours move by placement.
- **Cost**: per-item layer alpha (small, acceptable). **Verify**: recording of a filter change in the sample; scroll
  gates unchanged.

### M19. Swipe actions — WP03

- **Status: APPLIED, with gaps.**
  - APPLIED: offset in the layer; velocity carried into the settle (`ds/component/list/AppSwipeActions.kt:120-124`,
    `:169-183`); a full-swipe haptic (`:139`); accessibility actions.
  - Gaps: no projection; no rubber band past the last anchor (a hard clamp at `:137`); `sheet` spring.
- **Parameters**: full swipe at about 60 % [U]; `thumb` for open and close; a rubber band (c 0.55) past full reveal.
- **Implementation**: in place; add projection and the band. Give `resetKey: Any?` a type (RECOMPOSITION-PLAN).
- **Verify**: `SwipeActionsTest` (a release keeps its velocity); recording.

### M20. Pull to refresh (REQUIREMENT 29) — WP03

- **Status: PARTIAL.** WP03's rewrite (`ds/component/refresh/AppPullToRefresh.kt`) fixed the black ring (no surface),
  the stale `isRefreshing`, the `UserInput` guard and the moving content, and added spokes and a threshold haptic.
  Still missing against iOS:
  1. Linear 0.55 resistance with a hard clamp at 140 dp (`:84-93`, `:173`): the content stops dead under the finger.
  2. The refresh triggers on release, not when the gap reaches the threshold during the drag (`:98-117`).
  3. The settle ignores the release velocity (`:76`, `:105`).
  4. While refreshing, `onPreScroll` returns zero (`:82`), so the gap cannot be scrolled away as a content inset can.
  5. Threshold 80 dp / rest 56 dp, against iOS's 100 / 60.
  6. The indicator is a composable subtree with `derivedStateOf` flips (`:71`, `:133-150`) rather than one node.
- **Parameters** (ios-motion §5.3): trigger 100 dp of visible gap, during the drag; rest 60 dp; spinner 8 ticks, radius
  14 dp, 16 dp below `indicatorTop`; ticks revealed `ceil(8 · gap / trigger)` at α 147/255, with opacity eased in over
  the first 35 %; spin 125 ms steps (250 ms under reduce motion); colour `contentSecondary`; rubber band
  `y = 0.55·x·d / (d + 0.55·x)`; settle `bounceBack` seeded with the velocity × the band's slope; `minVisible` 400 ms;
  haptic `Threshold` once per pull; **no surface, no shadow**.
- **Implementation**: one `Modifier.Node` (`DelegatingNode` + `NestedScrollConnection` + `LayoutModifierNode` +
  `DrawModifierNode`), exactly as ios-motion §5.5's sketch:
  - The gap is snapshot state read in `placeWithLayer` and draw.
  - The spinner redraws 8 times a second through `invalidateDraw()`.
  - A running refresh's gap scrolls away 1:1 and a fling carries through it.
  - The public `AppPullToRefresh(isRefreshing, onRefresh, modifier, content)` stays, with a semantics "Refresh" action.
- **Cost**: L1 + D1. 0 recompositions per frame; ≤ 2 for the `refreshing` flag.
- **Verify**:
  - Robolectric (ios-motion §5.6 list): one `onRefresh` before release; none during a refresh; a gap pushed up before
    the list scrolls; a fling into the top never refreshes; the first settle frame moves further than a zero-velocity
    settle.
  - RECOMPOSITION-PLAN "Pull to refresh".
  - Screenshot tests light/dark/2× font.
  - Before/after recordings with `qa/ptr/record-ptr.sh` (`--size 720x1600`) into `perf/WP03/ptr/`.

### M21. Search bar focus expansion — WP03

- **Status: PARTIAL.** The cancel button uses `expandHorizontally` (`ds/component/input/AppSearchField.kt:113-114`), a
  size animation that re-measures the row every frame, plus a tween fade (`:68`). There is no title hide or field rise.
- **Parameters**: ≤ iOS 18: the title hides, the field moves up, Cancel (≈ 67 pt) slides in, 300 ms easeInOut [A][E] →
  `sheet`. iOS 26: the bottom pill morphs into a field above the keyboard [A].
- **Implementation**:
  - One progress `Animatable` on `sheet`.
  - Measure the field once at its final width; the Cancel slot's offset and the field's clip read the progress in
    layout and draw (L1).
  - Request focus one frame after the animation starts, so the IME's first frame is not the animation's [U].
- **Verify**: catalog recording; a recomposition journey with ≤ 2 executions (the focus flip).

### M22. Keyboard-synced motion — WP04 (bar), WP03 (fields, sheets), WP02-M3 (screens)

- **Status: PARTIAL.**
  - APPLIED: the bar reads the IME inset in layout (`nav/AppShell.kt:245-255`); the bottom chrome follows
    `imeAnimationTarget` once settled (`:275-299`).
  - MISSING: a fallback on API 26–29, where the inset jumps; a rule for screens.
- **Parameters**: API 30+ follows the system's 285 ms `PathInterpolator(0.2, 0, 0, 1)` frame by frame
  (`WindowInsetsAnimationCompat`) [S]. On API 26–29, animate the bottom chrome on `screen` instead.
- **Implementation**: screens use `imePadding()` or `windowInsetsPadding(WindowInsets.ime)` read in layout. Never read
  `WindowInsets.ime` in composition. `Modifier.imeNestedScroll()` (experimental) for interactive dismiss on API 30+.
- **Verify**: recording on the phone (API 37) and on an API 29 emulator.

### M23. Toast and HUD — WP03

- **Status: PARTIAL.**
  - APPLIED: slide + fade on `sheet`; the HUD scales from 0.9 on `emphasis` (`ds/component/overlay/AppToast.kt:222-234`);
    a swipe to dismiss with velocity (`:264-283`); haptics by tone (`:189-190`).
  - MISSING: `AnimatedVisibility` fades the whole toast with an `Auto` layer (card-sized, acceptable); dismiss by
    projection.
- **Parameters**: in from the bottom edge above the chrome on `sheet`; the HUD on `emphasis` (`snappy`-class);
  `Confirm`/`Reject` haptics.
- **Verify**: `OverlayHostTest`; recording.

### M24. Tab switch and bar selection motion (REQUIREMENT 28) — WP04 (WP03 tokens)

- **Status: MISSING.**
  - The switch is an instant swap (`nav/KeptTabs.kt:52-71`).
  - One indicator position on `SelectionSpring` (`bar/AppTabBar.kt:206`).
  - The pill is 52 dp tall in a 64 dp bar (`bar/AppTabBar.kt:376-388`, `ItemInset = 6.dp` at `:564`; QA-03).
  - The per-tab tint lerp washes labels out mid-slide (`:434-436`; QA-04).
  - No icon bounce; outline glyphs when selected (QA-06).
- **Parameters**:
  - **Content, "nudge and unveil" (ios-motion §4.2)**, incoming tab only:
    - `translationX` = 12 dp × direction → 0;
    - a veil in `colors.background` at α 0.6 → 0;
    - both on `thumb` (k 504: 66 % done at 100 ms, 94 % at 200 ms);
    - the outgoing tab disappears at once (iOS switches instantly; no crossfade, no double exposure, no offscreen);
    - RTL flips the direction;
    - a programmatic or deep-link switch gets no reveal;
    - reduce motion: none.
    - If the veil costs GPU p90, keep only the nudge.
  - **Indicator, two edges**: lead on `indicatorLead`, trail on `indicatorTrail`. Clamp the drawn width to 1.5 slots.
    Squash the height by `1 − 0.06 · (stretch − 1)`.
  - **Icon bounce** on select: `iconBounce`, down to 0.84, then back with a peak of about 1.04. QA-05's 1 → 1.12 → 1
    is the alternative; record both, CP1 picks.
  - **Pill geometry** (QA-03): vertical inset 10 dp, so the pill is 44 dp in the 64 dp capsule; width
    `clamp(label + 20 dp, 52 dp, min(slot − 8 dp, 72 dp))`; labels ellipsize at that width − 20 dp, so a label never
    overflows its pill.
  - **Labels** (QA-04): the base row is drawn in `content`; a copy in `selectedContent` is clipped to the pill path
    (one reused `Path`, `clearAndSetSemantics {}` on the copy). No per-tab colour lerp and no `ColorFilter`
    allocation.
  - **Filled glyphs** when selected (`AppIcons.*Filled`, WP03).
  - Haptic `Selection` (existing).
- **Implementation**:
  - The `TabReveal` holder + `TabRevealNode` (a `LayoutModifierNode` + `DrawModifierNode`), sketched in ios-motion
    §4.2. It is written in `SideEffect`, so the first drawn frame is already offset.
  - Combine it with SMOOTHNESS S4: the incoming tab is parked → unparked, with no re-measure.
  - `IndicatorEdges` (two `Animatable`s) read in `drawBehind`, with per-slot pill bounds in a non-state `FloatArray`
    written by the layout (ios-motion §4.3).
  - The icon scale is an `Animatable` per cell read in its `graphicsLayer`.
- **Cost**: L0 (nudge) + D2 for about 200 ms (veil) + D1 (bar). Visited first frame ≤ 8 / 16 ms.
- **Verify**:
  - `perf.sh tabs tabs_tap tab1`: janky ≤ 1 %, GPU p90 ≤ 8 ms.
  - RECOMPOSITION-PLAN "Tab switch": `KeptTabs` ≤ 1, the two `TabCell`s ≤ 1 each, tab roots 0, frames 2…N 0.
  - `record.sh tab_switch` in light and dark, at 1× and 2× font, 4 and 5 tabs, RTL, into `perf/WP04/tab-switch/`
    (requirement 28 "record it").

### M25. Symbol effects (bounce, replace) — WP03

- **Status: MISSING** (no DS helper).
- **Parameters**: bounce = M24's two legs; replace = the outgoing glyph scales down and fades, the incoming one scales
  up, both on `emphasis` [A].
- **Implementation**: `Modifier.symbolBounce(trigger)` and `AppSymbolReplace(icon)`, values read in `graphicsLayer`,
  both glyphs composed once. `AnimatedImageVector` only for real path morphs. An indefinite effect stops while
  `LocalTabVisible` is false.
- **Verify**: catalog page; recomposition 0 per frame.

### M26. Glass morphing (glass on only) — WP05

- **Status: MISSING** (WP05 not started).
- **Parameters**: one backdrop layer per bar or cluster; glass appears by ramping the lens or blur radius, never by
  layer alpha; morph (`glassEffectID` analogue) = a rect and corner lerp from the source's shape on `sheet` (catalogue
  §2.8, MASTER-PLAN A4.9).
- **Implementation**: the lens on `interactive` while dragged, released on `thumb` with velocity;
  `preferredFrameRate(High)` while the lens moves; `RuntimeShader` created once off the main thread (SMOOTHNESS S6, S15).
- **Verify**: the glass column of MASTER-PLAN §2 on `everything-glass`; `pushpop_gesture` with the M1 card (the bar
  re-blurs every frame there).

### M27. Scroll edge effect under floating bars — WP04 (bottom), WP03 (top, modifier)

- **Status: PARTIAL.**
  - APPLIED: the top fade (`ds/foundation/ScrollEdge.kt:25-47`), with the content recorded once per frame and only a
    strip drawn offscreen.
  - MISSING: the bottom protection under the floating bar (QA-02, requirement 18 "edge fade"); `ShellChrome` draws the
    chrome straight over the content (`nav/AppShell.kt:178-187`).
- **Parameters**: a gradient in `colors.background`: 0 → 0.78 at 55 % → 0.94; height = bottom chrome + 24 dp; it hides
  with the bar (the `followCovered` progress) and under the keyboard.
- **Implementation**:
  - One `drawBehind` rect with a cached `Brush` per height (D2 strip, no offscreen); the height is read in `layout`.
  - WP03 may provide `Modifier.edgeProtection(top, bottom)` in `ScrollEdge.kt` for both edges.
  - Keep the top `DstIn` strip only where a clear bar sits over arbitrary content.
- **Verify**: QA-02's shots; `scroll_root` and `scroll_minimize` GPU p90 unchanged (6 ms).

### M28. Controls with smaller motions — WP03

- **Status: PARTIAL.**
  - Slider: knob scale on press [E] (exists through `pressable`); momentum after release (MISSING), an iOS decay on
    the value, clamped.
  - Stepper: autorepeat after 0.5 s, then accelerating [A][U]: a coroutine repeat with a shrinking `delay`, and a
    `Selection` tick per step.
  - Page dots: `thumb`.
  - Numeric text (`AppAnimatedCounter`): digits roll in one `drawText` with a per-digit y offset and a cached
    `TextMeasurer` (D1), never `AnimatedContent` per digit.
- **Verify**: catalog recordings; recomposition 0 per frame.

---

## 4. Owner index

| Owner | Motions |
|---|---|
| WP04 | M1 (shell, covers), M2, M3 (option), M4, M5 (option), M6 (presenter transform), M9, M22 (bar), M24, M27 (bottom) |
| WP03 | §1 tokens; M1 (sheet variant), M6 (sheet), M7, M8, M10, M11, M12, M13, M14, M15, M16, M17, M18 (DS), M19, M20, M21, M22 (fields), M23, M25, M27 (modifier), M28 |
| WP02-M3 | M5 (sample usage), M18 (features), M22 (screens), the M12 press-down prefetch |
| WP05 | M26 |
| WP07 | every motion in common code with the same tokens; M3 as the iOS target's back gesture; M1 on the Android target |
| WP08 | the native SwiftUI equivalents (§5) |
| WP12 | §6 |

## 5. SwiftUI template (WP08): use the native motion, mapped to the same tokens

Apple's components already carry the motion, so the template's job is to use them and not fight them. Map the shared
tokens to `Animation.spring(response:dampingFraction:)`: `screen` → `.spring(response: 0.344, dampingFraction: 1)`,
`dialog`, `thumb`, `emphasis` and so on; `tokens/motion.json` → `DesignTokens`.

| Motion | SwiftUI |
|---|---|
| Push/pop, the interactive back swipe | `NavigationStack` (never a custom transition) |
| Zoom | iOS 18 `navigationTransition(.zoom(sourceID:in:))` + `matchedTransitionSource`, behind `#available` |
| Sheets | `presentationDetents([.medium, .large])` (the card stack is automatic) |
| Context menu | `contextMenu(menuItems:preview:)` |
| Pull to refresh | `.refreshable` |
| Swipe actions | `.swipeActions` |
| List changes | `withAnimation` around the data change |
| Tab bar minimize | iOS 26 `tabBarMinimizeBehavior(.onScrollDown)`, behind `#available` |
| Glass | `glassEffect`, behind `#available` |
| Haptics | `sensoryFeedback` |
| Symbol effects | `symbolEffect(.bounce)` |
| Scroll-linked effects | `scrollTransition` / `onScrollGeometryChange` |
| Keyboard | system safe area |

[A] for the APIs. The exact availability of the iOS 26 modifiers is [U] until checked against the SDK.

Rules:

- No `AnyView`.
- `@Observable` stores, read narrowly in leaf views.
- `let _ = Self._printChanges()` and the Instruments SwiftUI template to count body evaluations.
- Every custom animation uses a token spring and is interruptible: SwiftUI springs retarget and keep velocity by
  default.

## 6. Rules the docs must state (WP12)

1. The spring table of §1 with its sources, the cost classes of §2, and "every animation runs in layout or draw, with
   0 recompositions in frames 2…N−1".
2. Never a tween after a finger. `SeekableTransitionState.animateTo(null)` loses velocity. `AnchoredDraggable`'s
   default spec is a tween. Project, then snap.
3. No alpha on full-screen layers: scrims and veils are drawn rects; `ModulateAlpha` for short full-screen fades.
4. Predictive back: the Gmail constants (SPEC §2), the Back key gets the slide, and the start tab's root lets the
   system play back-to-home.
5. Tab switches: the incoming tab only, a nudge and a veil, no crossfade. The bar indicator has two edges.
6. Pull to refresh: the iOS numbers, no surface, no shadow, one node.
7. Reduce motion: what each motion becomes.
8. How to record a motion: `record.sh` / `screenrecord --size 720x1600`, and what to compare it with.
