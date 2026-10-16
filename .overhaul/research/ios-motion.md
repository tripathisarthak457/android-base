# iOS motion for android-base: ranked catalogue, Compose implementation, and three full designs

Researched 2026-10-09 for REQUIREMENTS 19, 21, 22, 24, 28 and 29. This file extends
`ios-motion-catalogue.md` (same folder), which still holds the long-form iOS parameter tables and their
derivations; everything below is ranked by importance for this project and says how to build each item in
Compose 1.12 / Navigation 3 1.2.0 / navigationevent 1.2.0 without per-frame recomposition.

Three items get full designs with code: **predictive back like Gmail (§3)**, **tab switch and bar selection
motion (§4)**, and **pull to refresh (§5)**. §3 rests on new recordings of Gmail on the reference phone.

Confidence tags (as in the catalogue, plus two):

| Tag | Meaning |
|---|---|
| **[R]** | Recorded on the Redmi Note 10 Pro (9a7c74f4, 1080 × 2400, density 2.75, 120 Hz, Android 17 build CP2A.260605.016, Pixel Launcher, display corner radius 90 px) in this research and measured frame by frame |
| **[S]** | Read in source: AOSP, androidx `androidx-main`, Material Components Android, the Compose 1.12.1 sources jar in `~/.gradle`, or Flutter |
| **[A]** | Apple documentation, HIG or WWDC |
| **[M] / [E]** | Measured from / eyeballed against real iOS by a third party (mostly Flutter's Cupertino fidelity work) |
| **[D]** | Derived here from tagged inputs; the working is shown |
| **[U]** | Unverified; needs a recording (iPhone, or the Redmi) before it becomes a token |

---

## 0. What is new compared with `ios-motion-catalogue.md`

1. **Gmail's in-app predictive back is Material Components' `MaterialMainContainerBackHelper`, exactly** [R][S].
   Measured on the phone: card scale 0.911–0.914 at a 44.8 % swipe; the helper predicts 0.9135. 8 dp gap on the
   far edge, 24 dp maximum vertical follow (linear in Δy / height), corners lerp to 28 dp, progress eased with
   `PathInterpolator(0.1, 0.1, 0, 1)`. The screen beneath does **not** move or scale. It sits under a **black
   32 % scrim** that comes on within about 50 ms of the gesture starting. On commit the card returns to full size
   while fading out (about 100 ms), and the conversation collapses into its inbox row. The whole commit takes
   170–270 ms. Gmail does not fly the card out (§3.1).
2. **The system's own cross-activity animation** (what users see between activities) uses the same 0.9 scale and
   8 dp margin, plus a 96 dp entering offset, a 450 ms EMPHASIZED post-commit, and a closing fade that is gone at 20 %
   of the commit [S] (§3.2). **Back-to-home** is the launcher's. On this phone the window reaches about 0.79 scale
   at a 40 % swipe [R] (AOSP Launcher3 `main` says 0.85; Pixel Launcher differs).
3. **Navigation 3 1.2.0 cannot do the Gmail card.** Its predictive phase seeks a `ContentTransform`, which has no
   touch Y, corner radius, scrim or velocity. It also finishes a released gesture with
   `tween(remainingDuration)` (WP04 Deviation 3). The design in §3.5 owns the gesture through the `NavDisplay`
   overload that takes a `SceneState` and a `NavigationEventState`. Nav3 1.3.0-alpha01 moved to
   `DeferredAnimatedContent`, which hands off to `popTransitionSpec` on release [S]; that is the upstream fix, but
   it is still alpha.
4. **`SeekableTransitionState.animateTo(spec = null)` traverses the rest of the transition linearly by duration**
   [S]. It never takes the finger's velocity. Release velocity reaches a screen transition only if an `Animatable`
   drives it (§2.2).
5. **`AnchoredDraggableDefaults.SnapAnimationSpec` is `tween()`** [S]. A tween drops the release velocity, so every
   sheet, swipe row and drawer must pass a spring to `AnchoredDraggableDefaults.flingBehavior(...)`.
6. **The iOS rubber band has a per-event form**: `dy = dx · 0.55 · (1 − y/d)²`. Flutter's iOS physics uses the same
   form with 0.52 [S]. Integrated, it is the closed form `y = 0.55·x·d / (d + 0.55·x)` [D]. Both pull to refresh and
   overscroll can track a single float with it.
7. **Pull to refresh**: all three existing versions (template `main`, VitFleet, WP03) still use linear 0.55
   resistance with a hard clamp at 140 dp, and none hands the release velocity to the settle. The `main` version
   also captures `isRefreshing` stale and has the elevation ring bug. §5 has the iOS design: a `Modifier.Node`,
   zero recomposition, and a spinner that redraws 8 times a second.
8. **Tab switch**: iOS switches instantly [A]. The design in §4 adds a 12 dp nudge plus a background-coloured
   unveil, applied to the incoming tab only (no double exposure, no offscreen buffer). The bar's indicator is
   driven by **two edge springs** (leading 0.22 s ζ 0.85, trailing 0.32 s ζ 1), clamped to 1.5 slots, with an
   icon bounce.
9. **Compose facts checked in source for this file**: in 1.12.1, `sharedBounds` defaults to
   `scaleToBounds(ContentScale.FillWidth, Center)`. A summary of the developer.android.com shared-elements page
   called `RemeasureToBounds` the default; the source wins. `veilOut`/`unveilIn` exist in 1.12.1 behind
   `@ExperimentalAnimationApi` and are stable in 1.13.0-alpha01. `DeferredTransitionState` is experimental in 1.12
   and stable in 1.13.0-alpha03. `NavigationEventState.transitionState` is snapshot state, so it is safe to read
   in draw. Compose has no `CompositionLocal` for the default fling.
10. **The Android IME inset animation** in sync mode lasts 285 ms with `PathInterpolator(0.2, 0, 0, 1)` [S]. iOS uses
    curve 7 at 0.25 s (catalogue §2.6).

Recording housekeeping. The first Gmail run (`record-gmail.sh`) produced 0-byte files: `screenrecord` at the native
1080 × 2400 fails on this phone, while `--size 720x1600` works (the `qa/ptr` script already used it). The
re-recording is `research/predictive-back/record-gmail-r3.sh`, logged in `record-gmail-r2.log`. It opens only a
conversation that is already read, so nothing in the mailbox changes. Gmail did show (and consumed) its one-time
"Snooze" tip on that conversation; the tip sits in the top half of `slow_cancel`. Clip index: §3.1.

---

## 1. Ranking for this project

The order follows four things: what the user named (requirements 24, 28, 29), which perf gates fail on the Redmi
(`perf/WP04/phase2/summary.md`), how often users see the motion, and how risky it is to get wrong.

| # | Motion | Why it ranks here | Owner | § |
|---|---|---|---|---|
| 1 | **Predictive back, Gmail style** (card, scrim, commit, cancel, back-to-home) | Req 24, named by the user. Every Android 14+ gesture shows it. Today Nav3 seeks a slide and finishes on a tween. The screen beneath is not laid out ahead: pop first frame median 15.0 ms, max 36.9 ms (gate 8/16) | WP04 | §3 |
| 2 | **Push/pop, Back key, rapid Back** | Failing gates: push/pop janky 2.34 % (gate 2), p99 57 ms (gate 25), push composing frame 48.6 ms max, nav_stress 5.26 %. The spring and parallax tokens are not yet the iOS values | WP04 | §6.1 |
| 3 | **Tab switch and bar selection** | Req 28, plus the req 18/30 bar defects (pill crowds the bar, label overflows it). Tab switch janky 1.9 % (gate 1), visited-tab first frame max 16.45 ms (gate 16) | WP04, WP03 tokens | §4 |
| 4 | **Pull to refresh** | Req 29: black ring, per-frame recomposition, content does not move | WP03 | §5 |
| 5 | **Scroll physics** (rubber band, iOS decay, fling into the edge) | Felt on every screen, and pull to refresh builds on it. WP03 has the rubber band opt-in only, and the decay is not wired | WP03 | §6.3 |
| 6 | **Press feedback** | Req 2 | WP03 | §6.4 |
| 7 | **Large title collapse, bar minimize, scroll edge fade** | Req 17/18/21. scroll_minimize already passes at 0.3 % | WP03, WP04 | §6.2 |
| 8 | **Sheets with detents and the card stack** | Req 21. No template feature has a sheet destination yet (WP04 open issue) | WP03, WP04 | §6.5 |
| 9 | **Alert and action-sheet scale-in** | Req 21, small surfaces, cheap | WP03 | §6.5 |
| 10 | **List insert/delete/move, swipe actions** | Req 21 | WP03 | §6.6 |
| 11 | **Switch, segmented control, picker, slider, stepper** | Req 21 | WP03 | §6.4 |
| 12 | **Keyboard-synced motion, search focus** | Req 21. API 30+ only for frame sync | WP03, WP04 | §6.7 |
| 13 | **Context menu with preview** | Req 21. The blur/snapshot is a GPU risk on Adreno 618 | WP03 | §6.5 |
| 14 | **Zoom/hero** | Req 21. `SharedTransitionLayout` adds lookahead cost to everything under it: opt-in | WP04 | §6.1 |
| 15 | **Toast/HUD** | Req 21 | WP03 | §6.8 |
| 16 | **Symbol effects** (bounce, replace) | Cosmetic, and reused by the tab icon bounce | WP03 | §6.8 |
| 17 | **Liquid Glass morph** | Only when glass is on (WP05). The cost rules are in the catalogue §2.8 | WP05 | §6.9 |

---

## 2. Toolkit shared by every item

### 2.1 iOS springs in Compose

```kotlin
/** SwiftUI's Spring(duration:bounce:) with mass 1: stiffness = (2π / response)², and ζ = 1 − bounce (bounce ≥ 0). */
fun iosSpring(response: Float, bounce: Float = 0f, visibilityThreshold: Float = 0.001f): SpringSpec<Float> {
    val omega = 2f * PI.toFloat() / response
    val dampingRatio = if (bounce >= 0f) 1f - bounce else 1f / (1f + bounce)
    return spring(dampingRatio, omega * omega, visibilityThreshold)
}
```

`AppSpring` in WP03's `theme/AppMotion.kt` already does this for bounce ≥ 0. The values below are the iOS references
this file uses. `AppSprings` names are WP03's.

| Use | iOS | Compose `spring(ζ, k)` | Source | AppSprings today → proposed |
|---|---|---|---|---|
| Push, pop, cover, sheet present, keyboard curve 7 | critical, ω₀ 18.26 (response 0.344 s) | `1f, 333f` | [E]+[D] catalogue §1.3 | `screen` 0.24 s → **0.344 s** "iOS-exact" (CP1 decides) |
| Alert, action sheet | k 522.35, ζ 1 (done at 0.404 s) | `1f, 522f` | [M] | missing → **`dialog`** |
| Segmented thumb; tab reveal (§4) | k 503.55, ζ 1 (0.412 s) | `1f, 504f` | [M/E] | `selection` 0.30 s, 0.6·bounce → **`thumb` 0.28 s, bounce 0** |
| Finger tracking (interactive) | response 0.15, ζ 0.86 | `0.86f, 1755f` | [A] | missing → `interactive` |
| Overscroll release / pull-to-refresh settle | Flutter iOS ballistic: m 0.5, k 100, ζ 1.1 | `1.1f, 200f` | [S][E] | layout spring today → **`bounceBack`** |
| Overscroll release with no velocity | exponential, half-life 70 ms (λ 9.9 /s) | overdamped roots −λ and −10⁵λ (§6.3) | [S][E] | — |
| System predictive-back progress (cancel) | `SpringForce` stiffness 1500, ζ 1 (response 0.162 s) | `1f, 1500f` | [S] AOSP | follow the event (§3) |
| Gesture "momentum" release (WWDC18) | ζ 0.8 | `0.8f, k` | [A] | `release` uses style bounce |

### 2.2 Velocity hand-off: which API keeps the finger's speed

| Mechanism | Keeps velocity on retarget? | Takes release velocity? | Use it for |
|---|---|---|---|
| `Animatable.animateTo(target, spec, initialVelocity = v)` | yes. The default `initialVelocity` is the current velocity | yes | every gesture-driven motion: back card, pull to refresh, sheet, swipe row, bar minimize |
| `animate*AsState`, `updateTransition` children | yes (springs only) | no | state-driven motion (selection, pressed) |
| `AnchoredDraggableState` + `AnchoredDraggableDefaults.flingBehavior(state, threshold, animationSpec)` | yes when `animationSpec` is a spring | yes | detents, swipe actions, switch thumb drag. The default spec is `tween()`, which drops it [S] |
| `SeekableTransitionState.seekTo` / `animateTo(null)` | — | **no**: "the transition is linearly traversed based on the duration" [S] | scrubbing only |
| `DeferredTransitionState` + `DeferredAnimatedContent` (Compose 1.13, Nav3 1.3-alpha) | catch-up spring after the deferred phase [S] | partly (catch-up) | future replacement for §3.5's own handler |
| Nav3 1.2.0 predictive pop | — | **no**, `tween(remaining)` (WP04 Deviation 3) | not for the Gmail card |

The release recipe (WWDC18 [A], catalogue §1.4):

```kotlin
val v = velocityTracker.calculateVelocity().y                 // px/s
val projected = position + v / 1000f * 0.998f / (1f - 0.998f)  // where an iOS scroll view would coast to
val target = anchors.closestTo(projected)
anim.animateTo(target, spring, initialVelocity = v)            // never a tween after a finger
```

Units. UIKit's `initialSpringVelocity` is distance/second ÷ travel distance. Compose's `initialVelocity` is in the
animated value's own unit per second. For an `Animatable` over a 0..1 fraction, divide px/s by the travel in px.

### 2.3 What each visual operation costs (phase discipline)

L = placement (`offset {}`, `placeWithLayer {}`), D = draw (`graphicsLayer {}` lambda, `drawWithContent`,
`DrawModifierNode`), C = composition (forbidden for any per-frame value).

| Operation | Phase | Cost on the Redmi (Adreno 618, 2.6 Mpx) | Rule |
|---|---|---|---|
| translation, scale, rotation in `graphicsLayer` | D (RenderNode properties) | no re-record | the default for anything that moves |
| `offset {}` / `placeWithLayer` reading animated state | L (re-place only) | small | when siblings must follow |
| outline clip with a uniform `RoundedCornerShape` (`graphicsLayer { shape; clip = true }`) | D | hardware outline clip | card corners |
| non-uniform corners or a `Path` clip on a moving full screen | D | path clip; on API < 33 not an outline clip [S Android Outline] | avoid on full screens |
| `alpha < 1` on a full-screen layer with overlapping content | offscreen buffer every frame | one full-screen render target plus composite [U ms] | forbidden on full screens (req 21) |
| `graphicsLayer { alpha; compositingStrategy = CompositingStrategy.ModulateAlpha }` | D | per-draw-op alpha, no buffer | short (≤ 150 ms) fades of a full screen, e.g. the back-card commit |
| one full-screen blended rect (scrim, veil) | D | one blended fill of 2.6 Mpx [U ≈ 1 ms] | keep it short-lived, or draw only the uncovered area |
| `RenderEffect` blur | GPU | grows with radius × area | snapshot once at 1/4 scale (catalogue §2.2) |
| scaling text-heavy screens repeatedly | D | glyph re-raster at each scale [U] | translate, don't scale, on tab switches |
| reading an animated value in composition | C | recomposes the subtree every frame | forbidden |

---

## 3. Design A: predictive back like Gmail (rank 1)

### 3.1 What Gmail does: recorded on the phone [R]

Gmail 2026.09.21, conversation → inbox, in-app (one activity, `ConversationListActivityGmail`). Frames are 360 × 800
(scale 1/3); 1 frame px = 3 device px = 1.09 dp. Clips live in `research/predictive-back/gmail/`:

| Clip | Gesture | Frames of interest |
|---|---|---|
| `slow_commit.mp4` | edge swipe to 45 % of the width over about 2.4 s, hold 1 s, release | 60 rest, 86–89 scrim on, 220–284 full shrink, 288–300 commit |
| `slow_cancel.mp4` | the same, then the finger returns | 312–320 the system cancels, card springs back. Gmail's one-time "Snooze" tip covers the top half |
| `fast_flick.mp4` | 120 ms swipe to 60 % | 77 shrink, 86–96 commit |
| `y_follow.mp4` | swipe 18 %, then drift +30 % H, then −25 % H, then cancel | 216 (down), 384 (up) |
| `to_home_slow.mp4`, `to_home_commit.mp4` | the same swipe at the inbox root | launcher back-to-home |
| `diag_cancel.mp4` | **invalid**: the first move was too vertical and the system never started a back gesture | — |
| `key_frames.png` | slow_commit 60/150/284/290/292/294/298 and y_follow 228/396 side by side | — |

| Quantity | Measured | What MDC's `MaterialMainContainerBackHelper` gives | Match |
|---|---|---|---|
| Card scale at a 44.8 % swipe | 0.911–0.914 (left 25, right 352–353 of 360; top 31, bottom 702 of the 734 px content area) | `lerp(1, 0.9, I(p))`, with I = cubic(0.1, 0.1, 0, 1), I(0.448) = 0.865 → **0.9135** | yes |
| Horizontal position | far-edge gap 7–8 px = **≈ 8 dp**; near edge 25 px | `translationX = lerp(0, (W − 0.9W)/2 − 8dp, I(p))` toward the swipe direction | yes |
| Vertical centring | centred (no shift without a vertical finger move) | scale about the centre | yes |
| Vertical follow | +6.7 px (7.3 dp) for Δy = +30 % H; −6.3 px for −25 % H | `24dp · abs(Δy) / height` → 7.9 dp / 6.5 dp | yes |
| Corner radius at I = 0.865 | top 24 px (≈ 72 device px); bottom 20 px (≈ 60 device px) | lerp(expanded → 28 dp, I): top expanded = display radius 90 px (the container touches the top corners), bottom expanded = 0 → 78 / 67 px in view space × 0.911 = 71 / 61 px | yes; collapsed size is **28 dp** (M3 extra-large) |
| What the card is | the content area above Gmail's bottom navigation bar. The bottom bar stays put | the container view | — |
| Screen beneath | **static**: no scale, no shift (row text stays at x = 73 px, as at rest) | not part of the helper | — |
| Scrim on the screen beneath | **black α 0.32** (L 239.7 → 162–164, cards 253 → 172), reached in about 3 frames (≈ 50 ms) at gesture start, then constant whatever the progress | Gmail's own, the M3 scrim opacity (32 %) | — |
| Commit | about 5 frames after release, the toolbar fades; the card returns to full width (left 25 → 0 in ≈ 100 ms) while fading; scrim 0.32 → 0 in ≈ 117 ms; then the conversation collapses into its inbox row, which is shown raised and highlighted. Total ≈ **270 ms** slow, **≈ 170 ms** after a fast flick | `finishBackProgress(duration)` resets scale and translation; hide durations 150–300 ms (`motionDurationShort3`..`Medium2`) | — |
| Cancel | the system cancels once the finger reverses about 36 dp (left edge 25 → 23 px, then the snap); the card springs back to full size in ≈ 100 ms (frames 312–320); the scrim goes with it | cancel animator 100 ms (`motionDurationShort2`); system progress spring stiffness 1500 ζ 1 | yes |
| Back to home (inbox root) | the whole window shrinks to ≈ **0.79** at a 40 % swipe, 8 dp from the far edge, over the wallpaper, then flies into the Gmail icon | Pixel Launcher (closed source). AOSP Launcher3 `main`: `MIN_WINDOW_SCALE` 0.85, the same interpolator and Y follow, scrim 0.2 light / 0.8 dark | Pixel's minimum scale ≈ 0.75 [D] |

Not recorded: dark mode. Recording it means switching the phone's theme, which is the user's setting, so it was
left alone. The dark scrim is **[U]**. Until it is recorded, keep 0.32, which is M3's scrim in both themes.

The requirement text says the card "flies out on commit". Gmail does not do that in-app: it un-shrinks and fades
(§3.4 offers both).

### 3.2 The system's constants (AOSP `main`) [S]

What Android itself does between activities, and the shape of the progress events every app receives:

| Constant | Value | Where |
|---|---|---|
| Progress definition | `deltaX / maxDistance`, linear up to `navigation_edge_action_progress_threshold` = **412 dp**, then non-linear with factor **0.2**. The Redmi is 392.7 dp wide, so progress = dx / screen width, linear | `BackTouchTracker.getProgress`, `core/res/values/dimens.xml`, `EdgeBackGestureHandler` |
| Progress smoothing (what apps receive) | `SpringForce` stiffness **MEDIUM (1500)**, damping ratio 1. A button press (`EDGE_NONE`) animates progress to 1 with stiffness LOW (200) when `predictiveBackSwipeEdgeNoneApi` is on | `BackProgressAnimator` |
| Cancel | progress springs back to 0 (stiffness 1500), then `onBackCancelled`. When the finger turns back, the system sets `triggerBack = false` and restarts progress from the latest touch | `BackProgressAnimator.onBackCancelled`, `BackTouchTracker.setTriggerBack` |
| Progress easing | `BACK_GESTURE` = `PathInterpolator(0.1, 0.1, 0, 1)` ("decelerate with a slight acceleration phase") | `BackGestureInterpolator` |
| Closing window | `MAX_SCALE` **0.9**, scaled about its centre, then offset so its far edge keeps **8 dp** (`cross_task_back_vertical_margin`) from the screen edge on a left-edge swipe | `CrossActivityBackAnimation`, `DefaultCrossActivityBackAnimation` |
| Y follow | `deltaY = max(0, (H − h)/2 − 8dp) · Decelerate(min(H/2, abs(Δy))/(H/2)) · sign` | `getYOffset` (Launcher3 uses the same) |
| Entering window | starts **96 dp** behind (`cross_activity_back_entering_start_offset`), scaled with the closing one, follows Y | `preparePreCommitEnteringRectMovement` |
| Corner radius | `ScreenDecorationsUtils.getWindowCornerRadius` (the display radius) on both | `applyTransform` |
| Scrim | black colour layer under the closing window, **0.2 light / 0.8 dark**, fading out over the commit | `ensureScrimLayer`, `onPostCommitProgress` |
| Commit | **450 ms**, `EMPHASIZED` (fast-out-extra-slow-in). Closing: to full size, moved by (its left + 96 dp), alpha `max(1 − 5t, 0)` (gone at 90 ms). Entering: to full size at x 0 | `DefaultCrossActivityBackAnimation.onGestureCommitted/onPostCommitProgress` |
| Fling bounce on commit | extra scale on a spring, stiffness LOW (200), damping ratio LOW_BOUNCY (0.75), seeded with `velocity · 100 · (1 − 0.9) · 2` (edge gestures), capped at 1000, at least 120 below 10 % progress | `postCommitFlingSpring` |

Material's guidance (developer.android.com predictive back design page [D1]): the surface scales to 90 % with
"8 dp gap from the screen edge"; x-shift `(W/20 − 8) dp`; y-shift `(H/20 − 8) dp` limited by the 8 dp margin; feed
progress through (0.1, 0.1, 0, 1) "to match the interpolator used for the SystemUI animations". For full-screen
surfaces there is a **fade-through at 35 %**. Gmail does not use that fade-through: it keeps the card opaque during
the gesture [R].

Google's own NavigationEvent sample (android skills `navigation-event/handle-back.md`) uses
`scale = 1 − 0.1·progress` and `maxShift = W/20 − 8 dp`, the same family of numbers.

### 3.3 Spec for the template

| Token | Value | Source |
|---|---|---|
| `progressEasing` | `CubicBezierEasing(0.1f, 0.1f, 0f, 1f)` on `event.progress` | [S] MDC, SystemUI; [R] |
| `minScale` | 0.90, about the centre | [S][R] |
| `edgeGap` | 8 dp: full progress puts the far edge 8 dp from the screen | [S][R] |
| `maxTranslationY` | `min(24 dp, (h − scale·h)/2 − 8 dp)`, linear in abs(Δy) / h | [S][R] |
| `cardCorner` | `lerp(0, 28 dp, eased progress)`, uniform on all four corners | [S][R]. Uniform is a cost choice [D]: Gmail lerps the top corners from the display radius, but non-uniform corners need a path clip (§2.3). The difference at full progress is about 3 dp |
| `scrim` | black 0.32 over the screen beneath: in over 50 ms at gesture start, out over the commit or cancel | [R]; dark [U] |
| Screen beneath | static, placed at 0, laid out before the gesture starts (§3.5 step 4) | [R] |
| Tab bar | stays where it is, above the card (like Gmail's bottom navigation). A destination that hides the bar lets it follow the eased progress, then the commit | [R] |
| Cancel | follow the event (the system springs progress to 0), then clean up over 100 ms on `onBackCancelled` | [S][R] |
| Commit, default ("Gmail") | from the frozen gesture values: scale → 1, translation → 0, corners → 0 on `screen` spring seeded with the gesture's velocity; content alpha 1 → 0 over **100 ms** (`ModulateAlpha`); scrim → 0 over the same 100 ms | [R] Gmail 100–120 ms |
| Commit, option `FlyOut` | the system's choreography: scale → 1, moved by (gap shift + **96 dp**) toward the swipe direction, alpha gone at **90 ms**, over ≈ 300 ms on the `screen` spring seeded with velocity | [S] AOSP (450 ms EMPHASIZED) |
| Commit, option `ReturnToSource` | when the destination carries a source key (list → detail), the card returns into the row with `sharedBounds` (Gmail's container return) | [R]. P2, needs `SharedTransitionLayout` (§6.1) |
| Back key / 3-button Back | no card: the iOS pop slide (`screen` spring, parallax 1/3, scrim) | catalogue §2.1 |
| At the root of the start tab, nothing presented | **no enabled handler**, so the system plays back-to-home | [S][R] |
| Reduce motion | no card: commit pops with a 150 ms fade (`ModulateAlpha`) | [A] HIG reduce motion |

### 3.4 Velocity

The progress events carry `frameTimeMillis` [S `NavigationEvent`]. Feed them to
`VelocityTracker1D(isDataDifferential = false)` as (time, progress) pairs; AOSP's `ProgressVelocityTracker` does the
same. On a phone narrower than 412 dp, progress per second is the finger's speed in screen widths per second. Seed
the commit spring (a 0..1 fraction) with it, clamped to 1..4 per second. AOSP raises its commit velocity to a floor
(`DEFAULT_FLING_VELOCITY`) when the release comes below 10 % progress; the lower clamp does the same job [D], so a
slow release still reads as intentional.

### 3.5 Architecture with Navigation 3 1.2.0 and the existing shell

1. **One owner per stack host.** `StackHost` switches to the `NavDisplay(sceneState, navigationEventState, …)`
   overload [S X1]. NavDisplay gets a `NavigationEventState` that is never bound to a handler. Its `transitionState`
   stays `Idle`, so NavDisplay never enters its own predictive phase, and its `predictivePopTransitionSpec` stops
   mattering. The shell binds its own `NavigationBackHandler` (navigationevent-compose 1.2.0) to a second state,
   and that handler drives the card. Nothing else registers for that stack, so registration order and priority are
   not a concern. The scene state comes from `rememberSceneState(entries, sceneStrategies, …, onBack)`; on `main`
   it also takes `sceneDecoratorStrategies` and `sharedTransitionScope`. Check the parameter list against the
   1.2.0 artifact (only the AAR is in the Gradle cache, not its sources).
2. **The handler is enabled only when the gesture can pop**: `stack.canGoBack && !gate.moving`. The existing
   `GateEffects` consuming handler is enabled exactly when `gate.moving`, so the two never compete. At the start
   tab's root nothing is enabled, and the launcher animation plays (requirement 24, "back-to-home matches the
   system"). Non-start tab roots keep their "back to the start tab" handler; that is the Android convention, and
   iOS has no Back there.
3. **Events are mirrored into draw-only state.** A `snapshotFlow { state.transitionState }` collector writes eased
   progress, Δy and edge into `mutableFloatStateOf`s, and feeds the velocity tracker. Only `graphicsLayer` and draw
   blocks read them. `active` flips twice per gesture and is read in the layout of the entry beneath (one re-place
   at start and at end).
4. **The screen beneath must be laid out before the gesture starts.** `KeepUnderScene` keeps the entry beneath
   composed, but it measures it only during a push (`d56ad13`). Measuring it on the first progress event repeats
   the pop's first-frame cost: 15 ms median on the Redmi, a dropped frame at 120 Hz. Two options; measure both
   with `perf.sh pushpop_gesture`:
   - **(a) measure ahead**: keep the entry beneath measured and placed with a zero-alpha layer while idle. HWUI
     skips drawing a zero-alpha RenderNode [U, verify in a Perfetto trace]. Its lifecycle stays CREATED, so it
     rarely relayouts. This also speeds up the Back-key pop (gate: pop first frame ≤ 8 ms).
   - (b) measure on `onBackStarted`, accepting one heavy frame at progress ≈ 0, where the visual change is tiny.
5. **Commit** = run the commit animation, then `stack.pop()` **with no transition** (a `StackChange.GesturePop`
   that the pop spec maps to `NavTransitions.none`). Nav3 moves the entry beneath into the next scene through its
   movable content (WP04 Deviation 4), so the frame after the pop is pixel-identical. Report `moving = true` to
   `BackGate` for the commit's duration, so a Back press in that window is counted and applied after it (rapid
   Back).
6. **Cancel**: progress is already back at 0 when `onBackCancelled` arrives (§3.2). Fade the scrim out over 100 ms,
   set `active = false`, and stop placing the entry beneath.
7. **Presented layers** (`PresentationHost`) use their own variant. A cover uses the card. A sheet uses MDC's bottom
   container: scale x by up to 48 dp, y by up to 24 dp, anchored at the bottom
   (`m3_back_progress_bottom_container_max_scale_*` [S]). Interactive swipe-down dismiss stays as in §6.5.
8. **RTL / right-edge swipes**: the direction comes from `swipeEdge` (`EDGE_RIGHT` shifts the card left). Layout
   direction does not change it.

### 3.6 Code sketch (core/navigation; check names against WP04's tree)

```kotlin
/** Gmail's in-app predictive back (Material Components' main-container helper), tokens from §3.3. */
internal object BackCardSpec {
    const val MIN_SCALE = 0.9f
    val EdgeGap = 8.dp
    val MaxTranslationY = 24.dp
    val Corner = 28.dp
    const val SCRIM_ALPHA = 0.32f
    const val SCRIM_IN_MILLIS = 50
    const val COMMIT_FADE_MILLIS = 100
    val Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
}

@Stable
internal class BackCardState {
    var progress by mutableFloatStateOf(0f)          // eased, read in draw only
        private set
    var deltaY by mutableFloatStateOf(0f)
        private set
    var edge = NavigationEvent.EDGE_LEFT
        private set
    var active by mutableStateOf(false)               // read in the layout of the entry beneath
        private set
    val commit = Animatable(0f)                        // 0 = gesture pose, 1 = full screen
    val contentAlpha = Animatable(1f)
    val scrim = Animatable(0f)

    private var startY = Float.NaN
    private val tracker = VelocityTracker1D(isDataDifferential = false)

    fun track(event: NavigationEvent, scope: CoroutineScope) {
        if (!active) {
            active = true
            edge = event.swipeEdge
            startY = event.touchY
            tracker.resetTracking()
            scope.launch { scrim.animateTo(BackCardSpec.SCRIM_ALPHA, tween(BackCardSpec.SCRIM_IN_MILLIS)) }
        }
        progress = BackCardSpec.Easing.transform(event.progress)
        deltaY = event.touchY - startY
        tracker.addDataPoint(event.frameTimeMillis, event.progress)
    }

    suspend fun commit(spring: SpringSpec<Float>) = coroutineScope {
        // Progress per second; on screens under 412 dp that is the finger's speed in screen widths per second.
        // A short flick still commits briskly: AOSP floors its commit velocity below 10 % progress too.
        val v = tracker.calculateVelocity().coerceIn(MIN_COMMIT_VELOCITY, MAX_COMMIT_VELOCITY)
        launch { contentAlpha.animateTo(0f, tween(BackCardSpec.COMMIT_FADE_MILLIS, easing = LinearEasing)) }
        launch { scrim.animateTo(0f, tween(BackCardSpec.COMMIT_FADE_MILLIS, easing = LinearEasing)) }
        commit.animateTo(1f, spring, initialVelocity = v)
    }

    suspend fun cancel() {
        scrim.animateTo(0f, tween(100))
        reset()
    }

    suspend fun reset() {
        active = false
        progress = 0f
        deltaY = 0f
        commit.snapTo(0f)
        contentAlpha.snapTo(1f)
        scrim.snapTo(0f)
    }
}

/** On the top entry: the card. Every value is read here, in the layer block, so a gesture never recomposes. */
internal fun Modifier.backCard(card: BackCardState): Modifier = graphicsLayer {
    val c = card.commit.value
    if (!card.active && c == 0f) return@graphicsLayer
    val p = card.progress * (1f - c)                       // the commit unwinds the gesture pose
    val gap = BackCardSpec.EdgeGap.toPx()
    val scale = 1f - (1f - BackCardSpec.MIN_SCALE) * p
    val towards = if (card.edge == NavigationEvent.EDGE_RIGHT) -1f else 1f
    val maxShift = (size.width * (1f - BackCardSpec.MIN_SCALE) / 2f - gap).coerceAtLeast(0f)
    val roomY = (size.height * (1f - scale) / 2f - gap).coerceAtLeast(0f)
    val followY = min(roomY, BackCardSpec.MaxTranslationY.toPx()) * (abs(card.deltaY) / size.height).coerceAtMost(1f)
    scaleX = scale
    scaleY = scale
    translationX = towards * maxShift * p
    translationY = sign(card.deltaY) * followY * (1f - c)
    alpha = card.contentAlpha.value
    // A short full-screen fade without an offscreen buffer.
    compositingStrategy = CompositingStrategy.ModulateAlpha
    shape = CardShapes.forRadius((BackCardSpec.Corner.toPx() * p).roundToInt())
    clip = p > 0f
}

/** One RoundedCornerShape per whole-pixel radius, so a gesture allocates no shapes per frame. */
private object CardShapes {
    private val cache = mutableIntObjectMapOf<Shape>()      // androidx.collection
    fun forRadius(px: Int): Shape = cache.getOrPut(px) { RoundedCornerShape(px.toFloat()) }
}

private const val MIN_COMMIT_VELOCITY = 1f
private const val MAX_COMMIT_VELOCITY = 4f

/** On the entry beneath (inside KeepUnderScene): the scrim, drawn over it, under the card. */
internal fun Modifier.backScrim(card: BackCardState): Modifier = drawWithContent {
    drawContent()
    val a = card.scrim.value
    if (a > 0f) drawRect(Color.Black, alpha = a)
}

@Composable
internal fun BackCardHandler(card: BackCardState, stack: AppBackStack, gate: BackGate, motion: ScreenMotion) {
    val state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    val scope = rememberCoroutineScope()
    LaunchedEffect(state) {
        snapshotFlow { state.transitionState }
            .filterIsInstance<NavigationEventTransitionState.InProgress>()
            .collect { card.track(it.latestEvent, scope) }
    }
    NavigationBackHandler(
        state = state,
        isBackEnabled = stack.canGoBack && !gate.moving,
        onBackCancelled = { scope.launch { card.cancel() } },
        onBackCompleted = {
            if (card.active) {
                scope.launch {
                    gate.whileMoving {                     // presses meanwhile are counted, then applied
                        card.commit(motion.fraction)
                        stack.apply(NavCommand.Pop(style = StackChange.GesturePop))
                    }
                    card.reset()
                }
            } else {
                stack.apply(NavCommand.Pop())              // Back key, 3-button Back: the iOS slide
            }
        },
    )
}
```

Notes on the sketch:

- `snapshotFlow` conflates events to one per frame, which is what the draw reads anyway.
- The scrim is a full-screen blended rect for the length of the gesture: about 1 ms of GPU fill [U]. If GPU p90 goes
  over budget, draw it as four bands around the card's bounds, using the same arithmetic as `backCard`. A
  `clipOutRect` would make the clip non-rectangular and cost more.
- The card stays opaque during the gesture: no alpha and no RenderEffect. Its cost is one extra full screen drawn
  (the screen beneath), the same as any pop.
- `whileMoving` stands for "report `moving` to the gate while this block runs". WP04's `ReportMotion` already uses
  that reporter mechanism.
- A commit that starts while the user immediately swipes again: the gate is moving, so the second gesture goes to
  `GateEffects` and is counted, not seeked. iOS would retarget instead (catalogue §0.4). The counting is the
  glitch fix measured on device (rapid_back lands on the expected screen, 2.62 % janky).

### 3.7 Acceptance for WP04

- **Recordings**: `record.sh gesture_pop` with `screenrecord --size 720x1600` on the template sample (list → detail).
  Compare frame by frame with `gmail/key_frames.png`: scale 0.91 ± 0.005 at a 45 % swipe, far-edge gap 8 dp,
  scrim 0.32, corners about 25 dp at full shrink, commit ≤ 300 ms.
- **Perf** (`perf.sh pushpop_gesture rapid_back nav_stress`, release build): janky ≤ 2 %, p99 ≤ 25 ms, no frame
  over 16 ms at gesture start (that is what step 4 above is for).
- **Recomposition test**: during a scripted swipe (Compose test `NavigationEventDispatcher` input, or a
  `TestNavigationEventInput`), the top and beneath entries' content recomposes 0 times. The handler composable
  recomposes at most twice: `active` on and off.
- **Unit tests**: commit pops exactly once; cancel never pops; a Back key during a commit lands one screen further
  after the commit (BackGate); at the start tab's root no handler is enabled.

### 3.8 Upstream path

Nav3 1.3.0-alpha01 (2026-09-23) uses `DeferredAnimatedContent`, which hands off from the predictive spec to
`popTransitionSpec` when the gesture ends; compose-animation 1.13 adds the catch-up spring [S X1, X3]. When both
are stable, the card could become a `ContentTransform` plus an entry-level `graphicsLayer` for touch Y and corners.
Until then, owning the gesture is the only way to get the Gmail card with springs.

---

## 4. Design B: tab switch and bar selection motion (rank 3)

### 4.1 Reference and constraints

- iOS: switching tabs is **instant**. There is no transition unless the delegate supplies an animator [A S23].
  Only the bar moves. In iOS 26 the selection capsule slides with a liquid stretch, and a long press turns it into a
  draggable lens [E S37].
- Requirement 28 asks for a tasteful content transition between kept tabs with **no rebuild, no double exposure,
  and no offscreen alpha on full screens**, plus bar motion: an indicator that stretches toward its target and an
  icon that bounces. Gates: tab-switch janky ≤ 1 %, visited-tab first frame ≤ 8 ms median and ≤ 16 ms max.
- `KeptTabs` today: hidden tabs stay composed but are not measured or placed; their lifecycle is capped at
  CREATED; their back dispatchers are disabled. The selected tab is measured and placed on the switch frame.
  Measured: first frame 6.51 ms median, 16.45 ms max.

### 4.2 Content: "nudge and unveil", on the incoming tab only

| Choice | Why not / why |
|---|---|
| Crossfade | double exposure, and both tabs drawn with alpha; it was removed for that reason (WP04) |
| Horizontal slide of both tabs (pager style) | draws two full screens per frame, and the outgoing tab would have to be placed again; it reads as navigation, not a switch |
| Scale | re-rasterises text at every scale step (§2.3), on the screen users switch to most often |
| **Nudge + unveil (chosen)** | the outgoing tab disappears at once, as on iOS. The incoming tab is placed 12 dp toward the travel direction and springs to 0; a veil in the background colour starts at α 0.6 and opens with the same spring. Only one screen is drawn, plus one blended rect for about 200 ms |

Spec [D]: `shift = 12.dp × sign(new − old)`, `veilStart = 0.6`, spring critical with response 0.28 s (k 504, the
iOS segmented-thumb spring). It is 66 % of the way at 100 ms and 94 % at 200 ms. Reduce motion: none (instant, as on
iOS). If the GPU p90 gate fails because of the veil, keep only the nudge, which costs nothing.

```kotlin
/** Which kept tab is arriving, and how far. Written in SideEffect, so the first drawn frame is already offset. */
@Stable
internal class TabReveal {
    var index by mutableIntStateOf(-1)
        private set
    var direction = 0f
        private set
    var progress by mutableFloatStateOf(1f)          // read in the layer block and in draw only

    fun begin(to: Int, from: Int) {
        index = to
        direction = sign((to - from).toFloat())
        progress = 0f
    }
}

// In KeptTabs:
val reveal = remember(generation) { TabReveal() }
val previous = remember(generation) { IntArray(1) { selected } }   // plain holder, written only after apply
SideEffect {
    // Runs when the switch is applied, before this frame's layout and draw: the first frame is already offset.
    if (previous[0] != selected) {
        reveal.begin(selected, previous[0])
        previous[0] = selected
    }
}
LaunchedEffect(reveal, selected) {
    if (reveal.index == selected) animate(0f, 1f, animationSpec = TabRevealSpring) { v, _ -> reveal.progress = v }
}
// …each kept tab's Box:
Modifier.tabReveal(reveal, index, veil = { colors.background }, shift = 12.dp)

private val TabRevealSpring = spring(dampingRatio = 1f, stiffness = 504f, visibilityThreshold = 0.001f)

private class TabRevealNode(var reveal: TabReveal, var index: Int, var veil: ColorProducer, var shift: Dp) :
    Modifier.Node(), LayoutModifierNode, DrawModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                if (reveal.index == index) translationX = reveal.direction * shift.toPx() * (1f - reveal.progress)
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (reveal.index == index) {
            val a = VEIL_START * (1f - reveal.progress)
            if (a > 0.004f) drawRect(veil(), alpha = a)
        }
    }
}
private const val VEIL_START = 0.6f
```

RTL: the direction follows visual order, so multiply by −1 in RTL. Programmatic tab changes from deep links get no
reveal: pass `animate = false` from `ShellState.select`.

### 4.3 The bar: two edge springs, clamp, icon bounce

What is there now: `animateFloatAsState(selected, SelectionSpring)` (ζ 0.91, k 438.6) drives one pill position, read
in `drawBehind`. That is good: zero recomposition. Replace it with two edges in slot units:

| Part | Spec | Source |
|---|---|---|
| Leading edge (in the direction of travel) | ζ 0.85, response 0.22 s (k 816) | [D] |
| Trailing edge | ζ 1, response 0.32 s (k 386) | [D] |
| Peak stretch | 1.27 slots for a 1-slot move, 1.54 for 2, at ≈ 90 ms; trailing edge settles at 379 ms; leading overshoot ≤ 0.02 slot [D, simulated] | — |
| Clamp | never wider than 1.5 slots: the trailing edge is drawn at `max(trail, lead − 0.5)` | [D] |
| Squash | pill height × `(1 − 0.06 · (stretch − 1))`, so it reads as a gel [U taste] | iOS 26 lens [E] |
| Icon bounce on select | scale to 0.84 on `spring(1f, 1500f)`, then to 1 on `spring(0.45f, 500f)`; the peak is about 1.04 | SF Symbols `bounce` [A S33], magnitude [U] |
| Haptic | `SegmentTick` (API 34+) on selection; already `HapticEffect.Select` | [A] HIG selection haptics |
| Pill geometry (req 18/30) | height `BarHeight − 2 × 10 dp` = 44 dp in the 64 dp capsule (today 52 dp, which crowds it). Width per slot `clamp(labelWidth + 20 dp, 52 dp, slot − 4 dp)`, so the label never overflows the pill; labels ellipsize at `slot − 8 dp` | req 18/30 |
| Long-press lens (optional) | after 0.3 s, the pill scales 1.12 and follows the finger x; a selection haptic at each slot crossed; release selects | [E] iOS 26 |

```kotlin
@Stable
internal class IndicatorEdges(initial: Int) {
    val start = Animatable(initial.toFloat())
    val end = Animatable(initial.toFloat())

    /** Which edge leads; read in draw to clamp the trailing one. */
    var forward = true
        private set

    suspend fun moveTo(target: Int) = coroutineScope {
        val t = target.toFloat()
        forward = t > end.value
        launch { (if (forward) end else start).animateTo(t, Lead) }
        launch { (if (forward) start else end).animateTo(t, Trail) }
    }
}
private val Lead = spring(dampingRatio = 0.85f, stiffness = 816f, visibilityThreshold = 0.001f)
private val Trail = spring(dampingRatio = 1f, stiffness = 386f, visibilityThreshold = 0.001f)

// In AppTabBar: val edges = remember { IndicatorEdges(selected) }; LaunchedEffect(selected) { edges.moveTo(selected) }
// Per-slot pill bounds are written by the Layout's measure into a FloatArray holder (not state) and read here.
Modifier.drawBehind {
    var s = edges.start.value
    var e = edges.end.value
    if (e - s > MAX_STRETCH) { if (edges.forward) s = e - MAX_STRETCH else e = s + MAX_STRETCH }
    val left = pills.leftAt(s)                       // lerp between neighbouring slots' pill lefts
    val right = pills.rightAt(e)
    val stretch = (right - left) / pills.widthAt((s + e) / 2f)
    val h = PillHeight.toPx() * (1f - 0.06f * (stretch - 1f).coerceAtLeast(0f))
    drawRoundRect(look.indicator, Offset(left, (size.height - h) / 2f), Size(right - left, h), CornerRadius(h / 2f))
}
private const val MAX_STRETCH = 0.5f
```

The tint lambda keeps `(1 − |center − index|)`, with `center = (start + end) / 2`. The icon bounce is an
`Animatable` per cell, started from `LaunchedEffect(selected)`. The cell recomposes once on that flip anyway, and
the scale is read in the icon's `graphicsLayer`. Minimize (`collapse`) keeps working, because it already
interpolates the drawn rect.

### 4.4 Acceptance

`perf.sh tabs tab1` (release): janky ≤ 1 %, visited first frame ≤ 8 / 16 ms, GPU p90 ≤ 8 ms. In the recomposition
test, a switch recomposes `KeptTabs` once and the two cells whose `selected` flipped once each. Tab roots: 0.
`record.sh tab_switch` light and dark, at 1× and 2× font scale.

---

## 5. Design C: pull to refresh, iOS level (rank 4)

### 5.1 What is wrong today

| Version | Defects |
|---|---|
| Template `main` (`core/designsystem/.../refresh/AppPullToRefresh.kt`) | (1) `offset.value` is read in composition (`progress`, `if (offset.value > 0f)`), so the indicator subtree recomposes every frame of a pull. (2) `connection` is remembered on `thresholdPx, maxPullPx` but reads `isRefreshing` directly: the value is captured at first composition, so a pull during a refresh passes the guard and can call `onRefresh` again. (3) No `NestedScrollSource.UserInput` check, so a fling reaching the top pulls the indicator. (4) An elevated `AppSurface` faded with layer alpha: the outline shadow shows through the translucent disc as the black ring in `qa/ptr/peak_compare.png`, worst in dark mode. (5) The content does not move. (6) `scope.launch { snapTo }` per scroll event: a coroutine per event, and the visual lags the consumed delta by a frame. (7) Linear 0.55 resistance and a hard clamp at 140 dp, so the content stops dead under the finger. (8) The settle ignores the release velocity. (9) `onPreFling` returns zero, so the list also flings while the indicator settles |
| VitFleet | fixes (2), (3), (5), (9). Keeps (1), (4), (7), (8) |
| WP03 (`wt/WP03`) | fixes (1) mostly, (2), (3), (4) (no surface), (5), (9); adds spokes and a threshold haptic. Still has (7) and (8). New: while a pulled refresh runs, `onPreScroll` returns zero, so the user cannot push the gap away: the list scrolls inside a box shifted 56 dp. On iOS the gap scrolls away with the content. The stepping spinner (`AppActivityIndicator`) is right, at 85 ms per step against iOS's 125 ms [E] |

Material 3's `PullToRefresh` [S X9], for technique: a `DelegatingNode` that is its own `NestedScrollConnection`,
`shouldAutoInvalidate = false`, `source == UserInput` guards, ignores scroll while its state animates, consumes the
downward fling when it was pulled, and draws the indicator in `placeWithLayer` with `clipRect(top = 0)`. It never
alpha-fades an elevated surface. Its tension curve beyond the threshold is `t − t²/4` over 0..2 thresholds. iOS
uses the rubber band instead.

### 5.2 iOS behaviour and numbers

| Behaviour | Value | Source |
|---|---|---|
| The content follows the finger with rubber-band resistance | `y = 0.55·x·d / (d + 0.55·x)`, per event `dy = dx · 0.55 (1 − y/d)²`, d = viewport height | [community R1], [S] Flutter 0.52 (1 − f)² |
| Trigger | when the visible gap reaches **100 pt**, **during the drag** (not on release): haptic, spinner starts, `valueChanged` fires | [S][E] Flutter `refreshTriggerPullDistance` 100, `HapticFeedback.mediumImpact`; iOS trigger distance itself [U] |
| Finger travel to trigger | x = d·y / (0.55 (d − y)): on a 700 dp viewport, 212 dp (about a quarter of the screen) | [D] |
| Resting gap while refreshing | **60 pt** | [S][E] Flutter `refreshIndicatorExtent` |
| Spinner | 8 ticks, radius 14 pt, centre 16 + 14 pt below the gap's top, tick from r/3 to r, width r/5, rounded | [S][E] Flutter (from iOS 13.2) |
| Reveal while pulling | ticks shown = ceil(8 × gap/trigger), at α 147/255; whole-spinner opacity eases in over the first 35 % of the trigger (easeInOut) | [S][E] Flutter, "derived using Xcode … iOS 13.5" |
| Spinning | one turn per second in 8 steps; tick alphas (newest to oldest) 147, 122, 97, 72, 47, 47, 47, 47 /255 | [S][E] |
| Colours | `#3C3C44` light, `#EBEBF5` dark | [S][E] Flutter, iOS 13.2 |
| Release past the trigger | content springs to the 60 pt rest, carrying the finger's velocity | [E] |
| Scrolling while refreshing | the 60 pt gap is part of the content inset: pushing up scrolls it away 1:1, letting go inside it leaves it where it is, a fling up carries on through it, and pulling down at the top brings it back, with resistance past 60 | [A] `contentInset` semantics; [U] for the exact feel |
| End of refresh | content springs back to 0 while the spinner shrinks with the gap | [S][E] Flutter `done` state |
| Fling into the top | bounces (overscroll); never triggers a refresh | [E] |

### 5.3 Spec for the template

| Token | Value |
|---|---|
| `trigger` | 100 dp of visible gap |
| `rest` | 60 dp |
| `spinnerRadius`, `spinnerTop` | 14 dp, 16 dp below `indicatorTop` (the top chrome's height, so the spinner sits under a pinned bar) |
| `resistance` | 0.55, d = the PTR box's height |
| `settle` | `spring(dampingRatio = 1.1f, stiffness = 200f)` (Flutter's iOS ballistic spring), seeded with the release velocity × the band's slope |
| `spinStep` | 125 ms (8 redraws a second); reduce motion: 250 ms |
| `minVisible` | 400 ms from arming to retracting, so a cached refresh is still seen to happen [D] |
| haptic | `GestureThresholdActivate` (Compose `HapticFeedbackType`, API 34 constant with fallback) at the trigger, once per pull |
| colour | `contentSecondary` (light and dark) |
| surface, shadow | **none** |

### 5.4 States

```
Idle ──pull──▶ Pulling ──gap ≥ trigger──▶ Armed(refreshing, haptic, onRefresh, spinner spins)
  ▲               │ release < trigger            │ release
  │               ▼                              ▼
  └──settle 0── Settling ◀──refresh ends── Resting(gap = rest, spinner spins; drag moves the gap 1:1)
```

`isRefreshing` from the caller is the source of truth for "refreshing". "Armed" covers the frames between the
trigger and the caller's state arriving. An external refresh (not pulled) shows nothing unless
`showExternalRefresh` is set, as WP03 already does.

### 5.5 Code sketch: one `Modifier.Node`, no recomposition per frame

```kotlin
fun Modifier.appPullToRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    tickColor: ColorProducer,
    indicatorTop: Dp = 0.dp,
    enabled: Boolean = true,
): Modifier = this then PullToRefreshElement(refreshing, onRefresh, tickColor, indicatorTop, enabled)

private class PullToRefreshNode(
    var refreshing: Boolean,
    var onRefresh: () -> Unit,
    var tickColor: ColorProducer,
    var indicatorTop: Dp,
    var enabled: Boolean,
) : DelegatingNode(), NestedScrollConnection, LayoutModifierNode, DrawModifierNode,
    CompositionLocalConsumerModifierNode {

    override val shouldAutoInvalidate = false

    private var gap by mutableFloatStateOf(0f)   // read in the layer block and in draw only
    private var raw = 0f                         // finger travel the gap stands for
    private var linear = 0f                      // a running refresh moves its gap 1:1 up to here
    private var dragging = false
    private var armed = false
    private var armedAt = 0L
    private var tick = 0
    private var settle: Job? = null
    private var spin: Job? = null
    private var viewport = 1f
    private var trigger = 0f
    private var rest = 0f

    init { delegate(nestedScrollModifierNode(this, null)) }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (available.y >= 0f || gap <= 0f) return Offset.Zero
        if (source == NestedScrollSource.SideEffect) {
            // A fling up through a running refresh's gap closes the gap first, as a content inset scrolls away.
            settle?.cancel()
            val give = min(gap, -available.y)
            gap -= give
            return Offset(0f, -give)
        }
        if (source != NestedScrollSource.UserInput) return Offset.Zero
        beginDrag()
        val give = min(raw, -available.y)        // pushing up closes the gap before the list scrolls
        raw -= give
        gap = visible(raw)
        return Offset(0f, -give)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (!enabled || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        beginDrag()
        raw += available.y
        gap = visible(raw)
        if (!armed && !refreshing && gap >= trigger) arm()
        return Offset(0f, available.y)
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!dragging) return Velocity.Zero
        dragging = false
        if (gap <= 0f) return Velocity.Zero
        val holding = refreshing || armed
        // Let go inside a running refresh's gap: it stays where the finger left it, as an inset does, and the
        // list's own fling (onPreScroll, SideEffect) closes it.
        if (holding && gap <= rest) return Velocity.Zero
        settleTo(if (holding) rest else 0f, velocity = available.y * slope())
        return available                          // this release belongs to the pull, not to a list fling
    }

    private fun beginDrag() {
        if (dragging) return
        dragging = true
        settle?.cancel()
        linear = if (refreshing || armed) rest else 0f
        raw = travelFor(gap)
    }

    private fun visible(r: Float) = if (r <= linear) r else linear + band(r - linear, viewport - linear)
    private fun travelFor(v: Float) = if (v <= linear) v else linear + bandInverse(v - linear, viewport - linear)
    private fun slope() = if (raw <= linear) 1f else RESISTANCE * (1f - (gap - linear) / (viewport - linear)).pow(2)

    private fun arm() {
        armed = true
        armedAt = SystemClock.uptimeMillis()
        currentValueOf(LocalHapticFeedback).performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        startSpin()
        onRefresh()
    }

    fun update(refreshing: Boolean) {             // from PullToRefreshElement.update
        if (refreshing == this.refreshing) return
        this.refreshing = refreshing
        if (refreshing) return startSpin()
        coroutineScope.launch {
            val shown = SystemClock.uptimeMillis() - armedAt
            if (armed && shown < MIN_VISIBLE_MILLIS) delay(MIN_VISIBLE_MILLIS - shown)
            armed = false
            if (!dragging) settleTo(0f, 0f)
        }
    }

    private fun settleTo(target: Float, velocity: Float) {
        settle?.cancel()
        settle = coroutineScope.launch {
            animate(gap, target, velocity, SettleSpring) { v, _ -> gap = v }
            if (target == 0f && !refreshing) { spin?.cancel(); tick = 0 }
        }
    }

    private fun startSpin() {
        if (spin?.isActive == true) return
        spin = coroutineScope.launch {
            while (true) {
                delay(SPIN_STEP_MILLIS)            // iOS steps, so 8 redraws a second rather than 120
                tick = (tick + 1) % TICKS
                invalidateDraw()
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        viewport = placeable.height.toFloat().coerceAtLeast(1f)
        trigger = Trigger.toPx()
        rest = Rest.toPx()
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) { translationY = gap }
        }
    }

    override fun ContentDrawScope.draw() {
        val g = gap
        if (g > 0.5f) {
            val top = indicatorTop.toPx()
            val r = SpinnerRadius.toPx()
            val spinning = refreshing || armed
            // Only the opened gap shows the spinner, so a transparent list header never reveals it early.
            clipRect(top = top, bottom = top + g) {
                val pulled = (g / trigger).coerceIn(0f, 1f)
                val fade = if (spinning) 1f else FastOutSlowInEasing.transform((pulled / 0.35f).coerceAtMost(1f))
                val shrink = if (spinning || dragging) 1f else (g / rest).coerceIn(0f, 1f)
                drawSpokes(
                    center = Offset(size.width / 2f, top + SpinnerTop.toPx() + r),
                    radius = r * shrink,
                    count = if (spinning) TICKS else ceil(TICKS * pulled).toInt(),
                    head = tick,
                    spinning = spinning,
                    color = tickColor(),
                    alpha = fade,
                )
            }
        }
        drawContent()
    }
}

private fun DrawScope.drawSpokes(center: Offset, radius: Float, count: Int, head: Int, spinning: Boolean, color: Color, alpha: Float) {
    val width = radius / 5f
    for (i in 0 until count) {
        val a = if (spinning) SpinAlpha[(i - head).mod(TICKS)] else PULL_ALPHA
        rotate(i * 360f / TICKS, center) {
            drawLine(color, Offset(center.x, center.y - radius / 3f), Offset(center.x, center.y - radius + width / 2f),
                strokeWidth = width, cap = StrokeCap.Round, alpha = a * alpha)
        }
    }
}

private fun band(x: Float, d: Float) = (1f - 1f / (x * RESISTANCE / d + 1f)) * d
private fun bandInverse(y: Float, d: Float) = d * y / (RESISTANCE * (d - y))

private const val RESISTANCE = 0.55f
private const val TICKS = 8
private const val PULL_ALPHA = 147f / 255f
private val SpinAlpha = floatArrayOf(47f, 47f, 47f, 47f, 72f, 97f, 122f, 147f).map { it / 255f }.toFloatArray()
private const val SPIN_STEP_MILLIS = 125L
private const val MIN_VISIBLE_MILLIS = 400L
private val Trigger = 100.dp
private val Rest = 60.dp
private val SpinnerRadius = 14.dp
private val SpinnerTop = 16.dp
private val SettleSpring = spring(dampingRatio = 1.1f, stiffness = 200f, visibilityThreshold = 0.5f)
```

`AppPullToRefresh(isRefreshing, onRefresh, modifier, content)` stays the public API: a `Box` with this modifier
and `contentSecondary` as the `ColorProducer`. It also gets a `semantics` custom action "Refresh", and a state
description while refreshing (VoiceOver exposes the iOS control the same way). Only a change of `isRefreshing`
recomposes it.

### 5.6 How it fits with the rest

- **App-wide rubber band** (`AppOverscroll`): Compose's scrollable calls `overscroll.applyToScroll(delta) {
  pre-scroll → self → post-scroll }`. The pull consumes the leftover in post-scroll, so the overscroll effect sees
  zero and nothing stretches twice. Flings (`SideEffect`) pass through to the overscroll bounce.
- **Large title**: the title is the first list item, so it moves down with the content; the spinner appears above
  it, as on iOS. Optional magnify on over-pull: catalogue §2.1.
- **Floating tab bar, pinned top bar**: `indicatorTop` is the top chrome height. The PTR box wraps the list, not the
  screen, so the bar does not move.
- **Tests**: Robolectric. A slow pull past the trigger calls `onRefresh` once, before release, with one haptic. A
  second pull during the refresh does not call it. Pushing up during a refresh closes the gap before the list
  scrolls; letting go inside the gap leaves it; a fling up carries through it. A fling into the top never
  refreshes. A release past the trigger settles with the release velocity (assert that the first settle frame
  moves further than a zero-velocity settle would). Recomposition count of the content during a full pull and
  settle: 0. Screenshot tests: light, dark, 2× font. Record before and after with `qa/ptr/record-ptr.sh`
  (req 29), with `--size 720x1600` as that script already does.

---

## 6. The rest of the catalogue: iOS parameters, Compose implementation, cost

Each row: iOS values (tags as in the catalogue), then the cheapest correct Compose mechanism with its phase.
Values with no tag come from the catalogue's tables, where the working is shown.

### 6.1 Navigation (rank 2, 14)

| Item | iOS | Compose |
|---|---|---|
| Push | incoming slides 100 %; outgoing −1/3 W and black α 0.094; edge shadow α 0.016 → 0; one critical spring ω₀ 18.26 [E][D] | Nav3 `transitionSpec`: `slideIntoContainer(Start, screen.offset)` togetherWith `slideOutOfContainer(Start, screen.offset) { it / 3 }` (L). Scrim and edge shadow in `coverScrim` (D, already draws only the visible strip, since the rect sits outside the entry). Parallax token 0.30 → **0.333** |
| Pop (Back key, up button) | mirror of push [E] | `popTransitionSpec` mirrored. The first frame needs the screen beneath measured (§3.5 step 4): today 15 ms median |
| Rapid / interrupted | iOS 18+ retargets in flight and never blocks [A S11/S12] | BackGate counts and coalesces (measured: lands on the expected screen, 2.62 % janky). Retargeting is the iOS-parity follow-up |
| Interactive swipe back | edge 20 pt; commit at ≥ 1 W/s or > 50 % [E] | replaced on Android by §3 (the system owns the edge) |
| iOS 26 full-content swipe back | pan anywhere pops; yields to horizontal scrollers [A S15] | optional `contentSwipeBack`: `draggable(Horizontal)` as a nested-scroll parent driving the §3 card with synthetic progress. Off by default: it fights the system gesture |
| Zoom / hero (iOS 18) | source morphs into destination; continuously interactive; drag down to dismiss [A] | `SharedTransitionLayout` + `sharedBounds` (default `scaleToBounds(FillWidth, Center)` in 1.12.1 [S], avoids per-frame remeasure; `RemeasureToBounds` "incurs frame-by-frame remeasurement" [S]). **Cost**: `SharedTransitionLayout` is a `LookaheadScope`, so everything under it gets a lookahead pass, and matched elements render in an overlay. Make it a generator option, measure push first frame with it on, and never wrap the whole app by default. Nav3 takes `sharedTransitionScope` since 1.1.0 [S D3]. Shapes do not animate between `sharedElement`s: use `sharedBounds` + `clipInOverlayDuringTransition = OverlayClip(shape)` [D5] |
| Card corners while moving (iOS 26) | moving screen drawn with display-radius corners [A forum] | `graphicsLayer { shape = RoundedCornerShape(r); clip = true }` on the moving entry (D). Radius from `WindowInsets.getRoundedCorner` (API 31+; 90 px on the Redmi) |

### 6.2 Bars and scroll-linked chrome (rank 7)

| Item | iOS | Compose |
|---|---|---|
| Large title collapse | bar 44 + 52 pt; title 34 pt bold → 17 pt semibold; inline title fades in 150 ms after the large title passes under the bar; bar background fades in over 10 pt; over-pull magnifies ≤ 1.1, anchored bottom-start [E][A] | Large title as list item 0. Inline title alpha and bar background alpha are derived from `firstVisibleItemScrollOffset` inside `graphicsLayer`/`drawBehind` (D). The 150 ms fade is an `Animatable` started on a threshold crossing; the crossing is a `derivedStateOf` boolean read in an effect, not in composition |
| Tab bar minimize (iOS 26) | `onScrollDown`: minimizes while content scrolls down, expands on scroll up, tab tap, or scroll to top [A S20] | done in WP04 (`AppTabBarState.collapse`, read in L/D; scroll_minimize 0.3 % janky). Hysteresis 8–10 dp [U]; settle on `thumb`-class spring with velocity |
| Scroll edge effect (iOS 26) | soft blur and fade under floating bars; one per view [A S29/S30] | a gradient scrim drawn in D (WP03 `ScrollEdge.kt`), no blur on Adreno 618. Req 18's "cluttered bottom edge" is this |

### 6.3 Scrolling physics (rank 5)

| Item | iOS | Compose |
|---|---|---|
| Deceleration | `0.998`/ms normal, `0.99`/ms fast [A][E] | `exponentialDecay(frictionMultiplier = 0.4767f)` (fast `2.393f`) [D catalogue §2.3] in a custom `FlingBehavior`. No `CompositionLocal` exists for the default fling [S X8], so the design-system list, grid, column and pager wrappers must pass it |
| Momentum accumulation | carried = sign · min(0.000816 · \|v\|^1.967, 40000) px/s [S][E] | keep the last fling's live velocity in the `FlingBehavior` and add the carried term when a new fling starts within it |
| Rubber band while dragging | §0.6 formula, c 0.55 [R1][S] | WP03 `AppOverscroll` (`RubberBandOverscroll`, `placeWithLayer`, L): correct. **Make it the default** (iOS lists always bounce [A]) |
| Release while stretched | exponential, half-life 70 ms (λ = ln 2/0.07 = 9.9 /s) [S][E] | WP03 uses the `layout` spring with no velocity. Use the release velocity and Flutter's overdamped pair: `spring(dampingRatio = (1e5f + 1f) / (2f * sqrt(1e5f)), stiffness = 1e5f * λ²)` = ζ ≈ 158.1, k ≈ 9.8 × 10⁶. That gives the e^(−λt) decay with v₀ honoured |
| Fling into the edge | velocity carried across the edge (cap 5000 px/s), caught by m 0.5, k 100, ζ 1.1 [S][E] | in `applyToFling`, hand the leftover to `spring(1.1f, 200f)` with `initialVelocity = min(left, 5000)`. WP03 scales the leftover by 0.12 instead |
| Paging | `isPagingEnabled`, fast deceleration [A] | `PagerDefaults.flingBehavior(snapAnimationSpec = spring(1f, 333f))`; target page from the r 0.99 projection |
| Touch delay | 150 ms in scroll views [A] | Compose's `TapIndicationDelay` (100 ms) inside scrollables: keep it |

### 6.4 Press feedback and controls (rank 6, 11)

| Item | iOS | Compose |
|---|---|---|
| Plain button | opacity 0.4, 120 ms down / 180 ms up [E] | WP03 `Dim` 0.45 via `ModulateAlpha` (D) |
| Filled button, card | darken/lighten, no scale ≤ iOS 18 [U]; iOS 26 glass scales **up** [A][U ≈ 1.05] | WP03 `Scale` 0.96 / 0.98 is a design choice; press = `press` spring, release = `release` spring with velocity (D) |
| Row highlight | grey after the 150 ms delay; deselects in step with the pop [A][U] | highlight alpha driven by §3/§6.1 progress (D) |
| Switch | 51 × 31 pt; thumb widens 7 pt while pressed; drag commits at 0.7, reverts at 0.2; haptic [E][A] | one `Modifier.Node` drawing the track and thumb (D); thumb x and width from two `Animatable`s; drag via `anchoredDraggable` with a **spring** fling spec; `ToggleOn`/`ToggleOff` haptics |
| Segmented control | thumb k 503.55 critical; pressed thumb 0.95; drag across segments [M/E] | thumb as two edge springs (the §4.3 pattern, `1f, 504f` both) (D); selection haptic per crossing |
| Picker wheel | cylinder: diameter ratio 1.07, perspective 0.003, squeeze 1.45, off-centre opacity 0.447, row 32 pt [E] | `LazyColumn` + snap fling with iOS decay; per-row `rotationX`/scale/alpha in `graphicsLayer` from the item offset (D); `SegmentFrequentTick` at most once per frame |
| Slider (iOS 26) | glass knob grows; momentum [E] | knob scale on press (D); after release an iOS decay on the value, clamped |
| Stepper | autorepeat ~0.5 s then accelerating [A][U] | coroutine repeat with shrinking `delay`; `SegmentTick` per step |

### 6.5 Presentations (rank 8, 9, 13)

| Item | iOS | Compose |
|---|---|---|
| Sheet present / dismiss | page curve (≈ ω₀ 18.26); dismiss at ≥ 2 H/s or past 52 %; over-pull stretches the top gap 8 % → 7.2 % H [E] | `AnchoredDraggableState` (Hidden/Medium/Large), offset in L; `AnchoredDraggableDefaults.flingBehavior(state, { it * 0.5f }, animationSpec = spring(1f, 333f))`, because the default spec is a tween [S]; rubber band past the top detent |
| Detents | medium ≈ half, large; scroll expands to the next detent first; `largestUndimmedDetent` [A] | nested-scroll connection: pre-scroll to the sheet until the top detent; post-fling settles. Measure content once at the large detent, offset only |
| Card stack | presenter scales **0.9165** about its top centre, moves down 7 % H, corners → 12 pt, 10 % overlay [E] | one `graphicsLayer` on the presenter root (scale, `transformOrigin = TransformOrigin(0.5f, 0f)`, translation, shape, clip) reading sheet progress (D). Overlay as a drawn rect, not alpha |
| Predictive back on a sheet | — | MDC bottom container: scale x by ≤ 48 dp, y by ≤ 24 dp, from the bottom [S]; the system progress drives it (§3.2) |
| Alert | 1.3 → 1.0 + fade in on k 522; fade-only out; scrim 0.20 light / 0.48 dark [M][E] | `graphicsLayer { scaleX; scaleY; alpha }` on the small card (D, cheap); `dialog` spring `1f, 522f`; scrim rect |
| Action sheet ≤ iOS 18; menus iOS 26 | slide up on k 522; iOS 26 morphs from the source button [M][A] | offset in L; iOS 26 look: scale from the anchor + pill → rounded-rect lerp in D, content fades in after ~40 % [U] |
| Context menu | 0.5 s long press, 10 pt slop; lift to ≤ 1.15 (≥ 1.02); backdrop blur σ 5 + 40 % tint; drag down shrinks to 0.8 [A][E] | `detectTapGestures(onLongPress)`; snapshot the row with `rememberGraphicsLayer().record { }` and animate that layer in an overlay (no list recomposition); backdrop = blurred 1/4-scale snapshot (`RenderEffect` API 31+) or a tint below 31; `LongPress` haptic |

### 6.6 Lists (rank 10)

| Item | iOS | Compose |
|---|---|---|
| Insert / delete / move | batch updates; row fades and slides, others move [A]; timing ≈ 0.3 s [U] | `Modifier.animateItem(fadeInSpec, placementSpec, fadeOutSpec)` with the `layout` spring; stable keys. The fade is a per-item layer alpha (small, acceptable) |
| Reorder | lift ≈ 1.03 + shadow + haptic [U] | drag offset in `graphicsLayer { translationY }` (D); neighbours via `animateItem` placement; `DragStart`/`SegmentTick` |
| Swipe actions | one row open; full swipe performs the first action (default true) [A]; full-swipe point ≈ 60 % [U] | `AnchoredDraggableState` (Closed/Open/Full), offset in L, action widths revealed by clip in D, **spring** fling spec; haptic `GestureThresholdActivate` when crossing Full; rubber band past the last anchor |

### 6.7 Text input (rank 12)

| Item | iOS | Compose |
|---|---|---|
| Keyboard show/hide | 0.25 s, curve 7 (≈ ω₀ 18.26) [A forum][D] | API 30+: `imePadding()` / `windowInsetsPadding(WindowInsets.ime)` read in layout is frame-synced via `WindowInsetsAnimationCompat` (needs edge-to-edge + `adjustResize`). The system curve is 285 ms `PathInterpolator(0.2, 0, 0, 1)` [S P10]. API 26–29: the inset jumps; animate the bottom chrome on the `screen` spring instead. Never read `WindowInsets.ime` in composition |
| Interactive dismiss | drag the scroll view down drags the keyboard [A] | `Modifier.imeNestedScroll()` (experimental), API 30+ only |
| Search focus (≤ iOS 18) | title hides; field moves up; Cancel (≈ 67 pt) slides in; 300 ms easeInOut [A][E] | one progress `Animatable` (`sheet` spring) read in L for field width/offset and Cancel; dim drawn in D; request focus one frame after the animation starts so the IME does not land on the first frame [U] |
| Search (iOS 26) | bottom search pill morphs into a field above the keyboard [A] | pill → field shape lerp (D), vertical offset from the IME inset (L) |

### 6.8 Feedback (rank 15, 16)

| Item | iOS | Compose |
|---|---|---|
| Toast / HUD | no public API; system confirmations drop in from the top on springs [U] | one overlay host above the shell; offset in L, scale 0.9 → 1 and alpha on the small surface in D; `snappy`-class spring (0.4 s, bounce 0.15); swipe up to dismiss with velocity; `Confirm`/`Reject` haptics |
| Symbol bounce / replace | `bounce` (up/down), `replace` (`downUp`, Magic Replace), iOS 26 `drawOn` [A S33] | bounce = the §4.3 two-leg scale on a bouncy spring (D); replace = outgoing scale-down + fade, incoming scale-up, both composed once, values read in `graphicsLayer`; `AnimatedImageVector` only for real path morphs; indefinite effects stop when `LocalTabVisible` is false |
| Numeric text | digits roll [A] | one `drawText` with a per-digit y offset and a cached `TextMeasurer` (D), not per-digit `AnimatedContent` |

### 6.9 Liquid Glass (rank 17, glass on only)

Unchanged from catalogue §2.8. The rule that matters for FPS: **one backdrop layer per bar or cluster**. Shapes are
unioned in one draw, and glass materialises by ramping the lens or blur radius, never by layer alpha. Morphing
(`glassEffectID`) is a rect and corner lerp from the source key's shape in D on the `sheet` spring.

---

## 7. Deltas to act on, by package

| Package | Change | Section |
|---|---|---|
| WP04 | Own the back gesture: `NavDisplay(sceneState, idleNavState, …)` + `BackCardHandler`; Gmail card tokens; commit pops without a transition; `whileMoving` on the gate during the commit | §3.5–3.6 |
| WP04 | Lay out the screen beneath ahead (zero-alpha placement) or on `onBackStarted`; measure both with `pushpop_gesture` | §3.5 (4) |
| WP04 | `KeptTabs` reveal (nudge + unveil); `AppTabBar` two-edge indicator with clamp; icon bounce; pill height 44 dp; label-aware pill width | §4 |
| WP04 / CP1 | `screen` 0.344 s "iOS-exact" candidate; parallax 0.333; edge shadow far fainter | §6.1, catalogue §5 |
| WP03 | Pull to refresh as a `Modifier.Node`: rubber band, trigger during drag, 60 dp rest, velocity settle, gap scrolls away while refreshing, 125 ms spinner steps | §5 |
| WP03 | `AppOverscroll` on by default; release with velocity and the 70 ms half-life pair; fling-into-edge on `spring(1.1f, 200f)`, capped 5000 px/s; iOS `FlingBehavior` passed by every DS scroller | §6.3 |
| WP03 | Springs: add `dialog` (k 522), `thumb` (k 504, no bounce), `interactive`, `bounceBack`; pass a spring to every `anchoredDraggable` fling behaviour | §2.1, §6.4–6.6 |
| WP12 | Document: "never a tween after a finger", "`SeekableTransitionState` loses velocity", "`AnchoredDraggable` default spec is a tween", the cost table §2.3, and the Gmail constants | §2 |

---

## 8. Still unverified

1. Gmail's dark-mode scrim (recording it needs the user's theme switched).
2. iOS `UIRefreshControl` trigger distance and its haptic (Flutter's 100 pt / medium impact are eyeballed).
3. Whether HWUI skips a zero-alpha RenderNode cheaply enough for the measure-ahead in §3.5 (Perfetto).
4. GPU cost of the full-screen scrim and veil on the Redmi (measure GPU p90 with and without).
5. iOS tab bar lens stretch magnitude, symbol bounce magnitude, iOS 26 menu pop timing.
6. Pixel Launcher's back-to-home minimum scale (≈ 0.75 from one clip; AOSP Launcher3 says 0.85).
7. Everything still open in catalogue §6.

---

## 9. Sources

Recorded and local:
- L1 Gmail clips, frames and key-frame strip: `research/predictive-back/gmail/` (this research; script `record-gmail-r3.sh`, log `record-gmail-r2.log`)
- L2 `perf/WP04/baseline/summary.md`, `perf/WP04/phase2/summary.md`; `qa/ptr/` (pull-to-refresh before)
- L3 Code read: `android-base/templates/android/core/designsystem/.../refresh/AppPullToRefresh.kt` (`main`), `~/Documents/projects/tools/vitfleet/vitfleet-android/core/designsystem/.../refresh/AppPullToRefresh.kt`, `wt/WP03/.../refresh/AppPullToRefresh.kt`, `wt/WP03/.../foundation/AppOverscroll.kt`, `wt/WP03/.../theme/AppMotion.kt`, `wt/WP03/.../feedback/AppProgress.kt`, `wt/WP04/.../core/navigation/*` (`NavTransitions`, `KeptTabs`, `KeepUnderScene`, `StackHost`, `CoverScrim`, `SceneMotion`), `wt/WP04/.../navigation/AppTabBar.kt`
- L4 Android skills (installed): `~/.claude/plugins/marketplaces/android-skills/navigation/navigation-event/SKILL.md` and `references/.../navigation-event/handle-back.md` (scale `1 − 0.1·progress`, shift `W/20 − 8 dp`); `navigation/navigation-3/SKILL.md`

AOSP (`refs/heads/main`), read in source:
- P1 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/WindowManager/Shell/src/com/android/wm/shell/back/CrossActivityBackAnimation.kt
- P2 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/WindowManager/Shell/src/com/android/wm/shell/back/DefaultCrossActivityBackAnimation.kt
- P3 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/WindowManager/Shell/res/values/dimen.xml (`cross_task_back_vertical_margin` 8 dp, `cross_activity_back_entering_start_offset` 96 dp)
- P4 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/WindowManager/Shell/shared/src/com/android/wm/shell/shared/animation/Interpolators.java and https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/animation/BackGestureInterpolator.java
- P5 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/window/BackProgressAnimator.java
- P6 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/window/BackTouchTracker.java
- P7 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/res/res/values/dimens.xml (412 dp, 0.2)
- P8 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/packages/SystemUI/src/com/android/systemui/navigationbar/gestural/EdgeBackGestureHandler.java
- P9 https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/main/quickstep/src/com/android/quickstep/LauncherBackAnimationController.java
- P10 https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/view/InsetsController.java (IME 285 ms, `PathInterpolator(0.2, 0, 0, 1)`)

Material Components Android, read in source:
- M1 https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/motion/MaterialMainContainerBackHelper.java
- M2 https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/motion/MaterialBackAnimationHelper.java
- M3 https://github.com/material-components/material-components-android/blob/master/lib/java/com/google/android/material/motion/res/values/dimens.xml

Android documentation:
- D1 https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
- D2 https://developer.android.com/guide/navigation/navigation-event (mirrored in L4)
- D3 https://developer.android.com/jetpack/androidx/releases/navigation3 (1.2.0 on 2026-09-23; 1.3.0-alpha01 `DeferredAnimatedContent` handoff; 1.2.0-beta01 rapid-back crash fix and seeking during forward transitions)
- D4 https://developer.android.com/jetpack/androidx/releases/compose-animation
- D5 https://developer.android.com/develop/ui/compose/animation/shared-elements

androidx (`androidx-main`, plus the Compose 1.12.1 sources jar in `~/.gradle/caches`):
- X1 https://github.com/androidx/androidx/blob/androidx-main/navigation3/navigation3-ui/src/commonMain/kotlin/androidx/navigation3/ui/NavDisplay.kt
- X2 https://github.com/androidx/androidx/blob/androidx-main/navigation3/navigation3-ui/src/commonMain/kotlin/androidx/navigation3/ui/PredictivePopGesture.kt
- X3 https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/Transition.kt (`DeferredTransitionState`; `SeekableTransitionState.animateTo` linear when `animationSpec` is null)
- X4 https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/src/commonMain/kotlin/androidx/compose/animation/EnterExitTransition.kt (`veilOut`, `unveilIn`)
- X5 https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/src/commonMain/kotlin/androidx/compose/animation/SharedTransitionScope.kt
- X6 https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Overscroll.kt
- X7 https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/AnchoredDraggable.kt (`SnapAnimationSpec = tween()`, min fling 125 dp/s)
- X8 https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/Scrollable.kt
- X9 https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/pulltorefresh/PullToRefresh.kt
- X10 https://github.com/androidx/androidx/blob/androidx-main/navigationevent/navigationevent/src/commonMain/kotlin/androidx/navigationevent/NavigationEventDispatcher.kt, `NavigationEvent.kt`, and `navigationevent-compose/.../NavigationEventState.kt`, `NavigationEventHandler.kt`

Flutter (iOS fidelity constants, with their "eyeballed"/"Xcode" comments):
- F8 https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/cupertino/refresh.dart
- F8b https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/cupertino/activity_indicator.dart
- F11 https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/widgets/scroll_physics.dart and `scroll_simulation.dart`

Apple and the remaining third-party sources (S1–S37, R1, F1–F13, C1–C6): see `ios-motion-catalogue.md` §7.
