# Motion guide

> **Draft for WP12** (REQUIREMENTS 21, 24, 25, 28, 35). Written Oct 12, 2026. Values are tokens decided at checkpoint
> CP1 (`decisions/CP1.md`) or planned in `research/MOTION-PLAN.md`; their iOS sources are in
> `research/ios-motion-catalogue.md`. WP12 checks every value against `tokens/motion.json` and `AppTheme.springs` in
> the merged code. **[verify]** marks values that the sources record as planned or first picks.

## 1. Principles

These make Android motion feel like iOS without costing frames (`research/ios-motion-catalogue.md` §1.4;
`research/MOTION-PLAN.md` §1 rules, §6):

1. **Every animation is interruptible and keeps its velocity.** Retarget with `animateTo`; never `snapTo` an animation
   the user can see.
2. **Never a tween after a finger.** A gesture's release seeds a spring with the finger's velocity
   (`initialVelocity`). `SeekableTransitionState.animateTo(null)` loses velocity; `AnchoredDraggable`'s default spec is
   a tween. Units: px/s for a pixel value; px/s ÷ travel for a 0–1 fraction.
3. **Project before snapping.** `target = nearest(position + v / 1000 · r / (1 − r))`, r = 0.998, or 0.99 for detents
   and paging (WWDC18).
4. **Track the finger, then hand off.** During a drag, `snapTo` or the `interactive` spring; on release, the target
   spring with the velocity.
5. **No bounce on anything that moves a full screen or the bar.** `screen`, `sheet`, `thumb`, the tab reveal, bar
   minimize and the back-card commit are all critically damped. Bounce stays only where it is small and tactile: the
   release of a press dip, the indicator's leading edge, the icon's up-leg, `emphasis` (`decisions/CP1.md`
   "Rules that hold for every value").
6. **Every animation runs in layout or draw, with 0 recompositions in frames 2…N−1** (`research/MOTION-PLAN.md` §2;
   `SMOOTHNESS.md` §6).
7. **Springs come from `AppTheme.springs`**; never `tween(300)` and never a private spring copy (`handoff/WP03.md`
   "Rules"; MOTION-PLAN §1 rule 1).
8. **Reduce motion:** no parallax, no scale, no bounce. Slides become 150 ms `ModulateAlpha` fades; selection still
   moves (MOTION-PLAN §1 rule 5).

## 2. Spring tokens

**Conversion** (`research/ios-motion-catalogue.md` §1.1; `research/MOTION-PLAN.md` §1). `AppSpring(response, bounce)`
gives `stiffness = (2π / response)²` and `dampingRatio = 1 − bounce`; a negative bounce gives ζ > 1. SwiftUI uses
`1 / (1 + bounce)` for negative bounce; the tokens are written for the template's formula. Values are for
`AppMotionStyle.Standard`; springs marked ÷speed divide their response by the style's speed (Snappy 1.15, Calm 0.87,
`research/RECOMPOSITION-PLAN.md` §3.2).

| Token | Response, bounce | Compose `spring(ζ, k)` | Used by | iOS source |
|---|---|---|---|---|
| `press` | 0.16 s, 0 | 1, 1542 | press dip | — |
| `release` | 0.32 s, style bounce (0.15 Standard) | 0.85, 386 | press release | bounce ≈ 0.15 is not felt as bouncy |
| `thumb` | 0.28 s, 0 | 1, 504 | switch and segmented thumbs, tab reveal, bar minimize, page dots (replaces `selection`) | iOS segmented thumb k 503.55 |
| `screen` ÷speed | **0.344 s, 0** | 1, 333 | push, pop, covers, back-card commit, keyboard fallback | iOS navigation and keyboard spring: CASpring m 3, k 1000, c 500 → ω₀ 18.26 |
| `sheet` ÷speed | 0.34 s, 0 | 1, 342 | sheet present and detents, toast, search focus | matches the iOS page curve |
| `dialog` ÷speed | 0.275 s, 0 | 1, 522 | alert, action sheet, menu, popover | iOS alert k 522.35 |
| `layout` ÷speed | 0.36 s, 0 | 1, 305 | list placement, expand and collapse | — |
| `emphasis` | 0.50 s, style bounce | 0.85, 158 | badges, success marks, HUD | SwiftUI `.snappy` |
| `interactive` | 0.15 s, 0.14 | 0.86, 1755 | a value that trails the finger (lens, reorder lift) | SwiftUI `interactiveSpring` |
| `bounceBack` | 0.444 s, −0.1 | 1.1, 200 | pull-to-refresh settle; a fling caught at an edge | Flutter iOS ballistic (m 0.5, k 100, ζ 1.1) |
| `overscrollRelease` | explicit | 158.1, 9.8 × 10⁶ | a rubber band let go with no velocity (half-life 70 ms) | Flutter's overdamped pair |
| `indicatorLead` | 0.22 s, style bounce (0 under Calm) | 0.85, 816 | the tab indicator's leading edge | — |
| `indicatorTrail` | 0.32 s, 0 | 1, 386 | the tab indicator's trailing edge | — |
| `iconBounce` | two legs | down `spring(1, 1500)` to 0.84, up `spring(0.45, 500)` | tab icon on select; symbol bounce | SF Symbols bounce |
| `fling` / `flingFast` | decay | `exponentialDecay(0.4767f)` / `exponentialDecay(2.393f)` | every scroller / paging and wheels | 0.998 / 0.99 per ms |
| `fadeQuick` / `fadeSlow` | 120 / 200 ms | tween | colour and opacity only | — |

Other motion tokens (`decisions/CP1.md` "Decisions at a glance"):

| Token | Value |
|---|---|
| `navigation.parallax` | 0.333 (iOS one third) |
| Cover scrim | 0.10 light / 0.24 dark |
| Edge shadow | alpha 0.04, width 8 dp |
| Press amounts | scale 0.96 / 0.98 large / 0.90 compact; dim 0.45; highlight 7 % light / 9 % dark, 220 ms fade |
| BackGate release (Back key, button) | 99 % of travel (363 ms at 0.344 s) |

**Why 0.344 s.** A critically damped spring with ω₀ 18.26 (sqrt(1000/3), the iOS keyboard spring) matches Flutter's
iOS page curve within 0.005 at every sample from 16 to 500 ms; a least-squares fit gives ω₀ 18.4 (response 0.341 s).
The earlier 0.24 s reached 74 % of travel at 100 ms against iOS's 55 %, which read as abrupt
(`research/ios-motion-catalogue.md` §1.3; `decisions/CP1.md` §1).

## 3. Cost classes

Every motion states its per-frame cost (`research/MOTION-PLAN.md` §2):

| Class | Allowed per frame | Example |
|---|---|---|
| L0 | `graphicsLayer` properties only (translation, scale, rotation, outline clip, `ModulateAlpha`) | back card, slide, press |
| L1 | re-placement only (`placeWithLayer`, `offset {}`) | overscroll, sheet offset, bar minimize |
| D1 | re-recording a node no larger than a bar, a card or a control | tab indicator, switch track, spinner |
| D2 | one full-screen blended rect, briefly | scrim, tab veil |
| Forbidden | recomposition; re-measuring a full screen; an offscreen layer larger than a bar or card; `RenderEffect` over a full screen (glass excepted, under its budget); allocation in draw | — |

## 4. The catalogue

Parameters from `research/MOTION-PLAN.md` §3 (M-numbers) unless noted. Owner packages in brackets. Status as of the
plan; WP12 confirms each is merged **[verify]**.

| # | Motion | Parameters | Cost |
|---|---|---|---|
| M1 | Predictive back (Gmail) | §5 below | L0 + D2 |
| M2 | Push and pop | `screen` (0.344 s, no bounce); incoming slides 100 %, covered page by parallax 0.333 under a drawn scrim (0.10 / 0.24) and a 0.04 edge shadow; no alpha, no scale; the clock starts on the frame after the composing frame. Reduce motion: 150 ms fade | L0 + D2 (visible strip) |
| M3 | Full-content swipe back (iOS 26 option, off by default; the primary back on the CMP iOS target) | commit at ≥ 1 W/s or past 50 %; then `screen` with the velocity | L0 |
| M4 | Rapid Back, interrupted transitions | BackGate counts presses while screens move and applies them as one pop at 99 % of travel, never past the root | — |
| M5 | Zoom and hero | `SharedTransitionLayout` + `sharedBounds` on `screen`; a generator option (`sharedTransitions`), off by default, because everything under the layout pays a lookahead pass | lookahead; overlay L0 |
| M6 | Modal sheet with detents and card stack | present and dismiss on `sheet`; dismiss at ≥ 2 H/s or past 52 %; detent chosen by projection (r 0.99); rubber band past the top. Card stack at the large detent: presenter scales 0.9165 about its top centre, moves down by the status bar + 10 dp, corners → 12 dp, a 10 % black rect. Full spec: `research/SHEET-SPEC.md` (REQUIREMENTS 33) | L1 + L0 + D2 |
| M7 | Context menu with preview | 0.5 s long press, 10 pt slop; preview lifts to ≤ 1.15 (≥ 1.02); backdrop blur σ 5 + 40 % tint; a drag down shrinks it to 0.8; menu on `dialog`; `LongPress` haptic | L0 |
| M8 | Large-title collapse | bar 44 + 52 pt; title 34 pt bold → 17 pt semibold; inline fade 150 ms; background over 10 pt; over-pull magnify ≤ 1.1 anchored bottom-start | L0 / D1 |
| M9 | Tab bar minimize | triggered by direction after 10 dp, settled on `thumb` with the fling velocity and projection; 8–10 dp hysteresis; opens on scroll up, at the top or on a tap; never scrubbed by the scroll (`decisions/CP1.md` §5) | L1 |
| M10 | Rubber-band overscroll | `y = c·x·d / (d + c·x)`, c 0.55, d = the viewport; release on `overscrollRelease`, or `bounceBack` with velocity; a fling into the edge carries `min(leftover, 5000 px/s)` | L1 |
| M11 | Scroll deceleration | `fling` 0.998/ms; paging and wheels `flingFast` 0.99/ms; momentum accumulation `min(0.000816 · |v|^1.967, 40000) px/s` when a fling starts within a running one | — |
| M12 | Press states | `press` down, `release` up with velocity; scale 0.96 / 0.98 large / 0.90 compact; dim 0.45; row highlight 220 ms fade; no ripple | L0 |
| M13 | Switch | 51 × 31 pt; the thumb widens 7 pt while pressed; drag commits at 0.7 and reverts at 0.2; `thumb` with velocity; `ToggleOn`/`ToggleOff` haptics | D1 |
| M14 | Segmented control | thumb as two edges, both on `thumb`; pressed thumb 0.95; a selection haptic per crossing | D1 |
| M15 | Picker wheel | diameter ratio 1.07, perspective 0.003, squeeze 1.45, off-centre opacity 0.447, row 32 pt; snap on `thumb`, decay `flingFast` | D1 |
| M16 | Alert scale-in | scale 1.3 → 1.0 + fade in on `dialog`; fade out only; scrim 0.20 light / 0.48 dark, drawn as a rect | L0 + D2 |
| M17 | Action sheet | slide up on `dialog`; scrim as M16 | L0 + D2 |
| M18 | List insert, delete, reorder | placement on `layout`; insert fade 200 ms; delete fade 120 ms; reorder lift about 1.03 + shadow + `DragStart` haptic **[verify]** | L1 |
| M19 | Swipe actions | full swipe at about 60 % **[verify]**; `thumb` with velocity; rubber band (c 0.55) past full reveal | L1 |
| M20 | Pull to refresh | trigger at 100 dp of visible gap during the drag (haptic tick); rest at 60 dp; spinner 8 ticks, radius 14 dp, revealed `ceil(8 · gap / trigger)` at α 147/255, 125 ms steps (250 ms under reduce motion); rubber band c 0.55; settle on `bounceBack` with the velocity; no surface, no shadow; one `Modifier.Node` (`SMOOTHNESS.md` §2.12) | L1 + D1 |
| M21 | Search bar focus | one progress on `sheet`: title hides, field moves up, Cancel slides in | L0 / L1 |
| M22 | Keyboard-synced motion | API 30+ follows the system's 285 ms `PathInterpolator(0.2, 0, 0, 1)` frame by frame (`WindowInsetsAnimationCompat`); API 26–29 animates bottom chrome on `screen`. Never read `WindowInsets.ime` in composition | L1 |
| M23 | Toast and HUD | toast in from the bottom edge above the chrome on `sheet`; HUD on `emphasis`; `Confirm`/`Reject` haptics | L0 |
| M24 | Tab switch and bar selection | §6 below | L0 + D2 (~200 ms) + D1 |
| M25 | Symbol effects | bounce = M24's two legs; replace = outgoing scales down and fades, incoming scales up, both on `emphasis`; stops while the tab is hidden | L0 |
| M26 | Glass morphing (glass on) | glass appears by ramping the lens or blur radius, never by layer alpha; morph = rect and corner lerp on `sheet`; §7 below | glass budget |
| M27 | Scroll edge under floating bars | a gradient in `colors.background`: 0 → 0.78 at 55 % → 0.94; height = bottom chrome + 24 dp; hides with the bar and under the keyboard; one cached-brush rect, no offscreen, no blur | D2 strip |

## 5. Predictive back like Gmail (REQUIREMENTS 24)

**What it looks like.** During the back gesture the current screen shrinks into a rounded card that follows the
finger, the previous screen shows behind it under a scrim, the card flies out on commit and springs back on cancel.
Back-to-home is the system's own animation. Recordings from the user's phone: `research/predictive-back/gmail/`
(`key_frames.png`). Ours: `perf/WP04/predictive-back/` (slow commit, slow cancel, fast flick, right edge, y-follow,
back-to-home, dark, RTL).

**Constants** (`research/predictive-back/SPEC.md` §2; [S✓] verified against MDC and AOSP sources, [R] measured on
the Gmail recordings):

| Token (`BackCard.*`) | Value | Source |
|---|---|---|
| `ProgressEasing` | `CubicBezierEasing(0.1f, 0.1f, 0f, 1f)` on `event.progress` | AOSP `BackGestureInterpolator` |
| Progress input | the system's `Δx / maxDistance`, linear up to 412 dp (on the Redmi, Δx / W) | AOSP `BackTouchTracker` |
| `MIN_SCALE` | 0.90, about the centre | MDC `MIN_SCALE = 0.9f` [S✓]; Gmail 0.911–0.914 at 44.8 % [R] |
| `EdgeGap` | 8 dp from the far edge at full progress | MDC `min_edge_gap` [S✓] |
| Horizontal shift | `towards · max(0, (W − 0.9·W)/2 − 8 dp) · p` (11.6 dp at p = 1 on the Redmi) | MDC [S✓] |
| `MaxFollowY` | 24 dp, linear in `abs(Δy) / H`, capped by the room left | MDC `max_translation_y` [S✓] |
| `Corner` | `lerp(0, 28 dp, p)`, uniform on all corners (keeps the clip a hardware outline) | [R] |
| `SCRIM_ALPHA` | 0.32 black over the screen beneath, under the card | [R]; dark mode [U] |
| `SCRIM_IN_MILLIS` | 50, linear, from the first progress event | [R] |
| `COMMIT_FADE_MILLIS` | 100, content alpha 1 → 0 with `CompositingStrategy.ModulateAlpha` | [R] |
| `SCRIM_OUT_MILLIS` | 120 on commit, 100 on cancel | [R]; MDC cancel 100 ms |
| Commit un-shrink | `AppTheme.springs.screen`, seeded with the release velocity | [D] |
| Release velocity | `VelocityTracker1D` over (frame time, progress), clamped to 1–4 progress/s | AOSP `ProgressVelocityTracker` |
| Cancel | none of our own: the system springs progress back to 0 (k 1500, ζ 1) before `onBackCancelled` | AOSP `BackProgressAnimator` |
| Sheet variant | bottom container: scaleX by up to 48 dp, scaleY by up to 24 dp, origin bottom centre | MDC [S✓] |
| Back to home | the system's; the template does nothing | Launcher3 |

**Behaviour rules** (`handoff/WP04.md` "Rules"; SPEC §5):

- The commit returns the card to full size while fading for 100 ms and pops with no slide; cancel follows the system's
  spring back in about 100 ms.
- It runs from any moment of a push, starting from the screen's current position (REQUIREMENTS 32). A swipe during a
  pop is counted by BackGate. Left- and right-edge swipes move the card toward the correct side.
- Progress is read only in layer and draw blocks: 0 recompositions per progress event (`SMOOTHNESS.md` §6).
- `EDGE_NONE`, the Back key and API < 34 pop with the M2 slide.
- At the start tab's root no back handler is enabled, so the system's back-to-home runs.
- Each kept tab has its own `NavigationEventDispatcher`, enabled in the switching composition.
- Gates: the push/pop gesture rows of `SMOOTHNESS.md` §6; `perf.sh back_during_push` asserts no double exposure.

## 6. Tab switch effect (REQUIREMENTS 28)

Decided at CP1 (`decisions/CP1.md` §4); built in WP04 (`handoff/WP04.md` "Rules").

**Content: "nudge and unveil", incoming tab only.**

- `translationX` 12 dp × the direction of travel → 0, and a veil in `colors.background` at α 0.6 → 0, both on `thumb`
  (66 % done at 100 ms, 94 % at 200 ms).
- The outgoing tab is parked at once: no crossfade, no double exposure, no offscreen alpha.
- RTL flips the direction. A programmatic or deep-link switch, a navigation to a root and reduce motion get no
  reveal.
- Why not a veil from α 1: a fully opaque start puts a blank frame on screen, which reads as a flash.
- Fallback: if the veil pushes tab-switch GPU p90 above 8 ms or janky above 1 %, keep only the nudge.

**Indicator: two edges.** Leading edge on `indicatorLead` (0.22 s), trailing edge on `indicatorTrail` (0.32 s), in slot
units; drawn width clamped to 1.5 slots; height squashed by `1 − 0.06 · (stretch − 1)`. Both edges start on the same
frame as the reveal, so the bar and the content move together.

**Pill and labels.** The pill is 44 dp tall in the 64 dp capsule (10 dp vertical inset); width
`clamp(label + 20 dp, 52 dp, min(slot − 8 dp, 72 dp))`; a label never passes the pill (ellipsized, text capped at
1.3×). Labels are drawn in two layers, the selected copy clipped to the pill (`clearAndSetSemantics` on the copy):
no per-tab colour lerp, no `ColorFilter` allocation. Filled glyphs when selected. Labels `Always` by default.

**Icon bounce.** Down to 0.84 on `spring(1, 1500)`, back on `spring(0.45, 500)`, peaking at about 1.03; mostly over in
300 ms, settled by 500 ms. Under Calm the up-leg is ζ 1. Why down-then-up: it is SF Symbols' bounce, it reads as a
press on a bar whose items have no press feedback, and it never grows the glyph past the pill.

**Acceptance.** `perf.sh tabs tabs_tap tab1`: janky ≤ 1 %, visited first frame ≤ 8 / ≤ 16 ms, GPU p90 ≤ 8 ms;
RECOMPOSITION "Tab switch" budgets. Recordings in `perf/WP04/tab-switch/` (light, dark, four tabs, RTL).

## 7. The travelling lens (glass, REQUIREMENTS 35)

**What it looks like** (iOS 26 tab bar): on a tap, the lens travels to the new tab as a liquid droplet. It lifts and
swells slightly, refracts and magnifies what is under it while it moves, stretches along the direction of travel, and
settles with the selection spring, within about 0.3–0.4 s (REQUIREMENTS 35).

**How it is built** (generator `foundation/glass/LiquidLens.kt`; `handoff/WP05.md` "Lens on tap") **[verify merged]**:

- Every selection change (a tap, the back stack, a deep link, a drag's release) moves the lens on its settle spring
  (`thumb`). The lens's own velocity is the only input: `travel = smoothstep(min(1, |velocity| / 6 slots/s))`.
  No new animation, no new coroutine; travel is 0 on the frame the lens settles. `intensity = max(pressure, travel)`.
- Per frame, read only in the lens's own layer block and draw:
  - layer scale: swell `1 + max(0.3 · pressure, 0.12 · travel)`, stretched along the bar by `1 + 0.2 · travel` and
    narrowed across by `1 − 0.08 · travel`, never below 1 on either axis;
  - the backdrop sample magnified `1 + 0.12 · intensity` about the lens centre (a canvas transform, no extra pass);
  - the resting selection colour at alpha `1 − intensity`, so the refraction shows through as it fades.
- The effect chain is static and built once: vibrancy, blur 2 dp, lens 12/20 dp. No dispersion, no inner shadow, no
  highlight, no shadow on the tap path.
- While the lens is not sampling, its layer alpha is 0 (HWUI skips it). On `thumb` at 120 Hz a tap to the next tab is
  full glass from 24 to 88 ms and samples until about 264 ms; four tabs away, until about 336 ms.
- The bar body never reads `travel`, `intensity` or `scale`: no bar re-render, no bar-body `layerBlock` (G1).
- `LiveNoLens` and lower tiers keep a lens-free tap. A drag keeps the full press behaviour (`interactive` while
  dragged, released on `thumb` with velocity).
- The values (6 slots/s, 0.12, 0.2 / 0.08, 0.12) are first picks from the iOS 26 description, to be tuned on the phone
  with the CP2 strips **[verify final values]**.
- The lens's shader first reaches the GPU on the first tap, so the idle warm-up must render it once with sampling on.
- Gate: `glass.quality=Live`, tab-tap janky % and GPU p90 within `glass_live`, measured against `LiveNoLens` on the
  same journey (the delta is the lens).

## 8. How to record a motion

- `tools/perf/record.sh <appId>.dev <journey> <out>`; journeys include `tab_switch`, `push`, `pop`, `gesture_pop`,
  `rapid_back`, `back_slow_commit`, `back_slow_cancel`, `back_fast_flick`, `back_y_follow`, `back_to_home`,
  `back_during_push` (`handoff/WP04.md` "perf script usage").
- On the Redmi always `screenrecord --size 720x1600`; full resolution fails on its encoder (`STATUS.md` §3).
- RTL recordings set the app's own locale (`cmd locale set-app-locales <pkg> --locales ar-XB`), never the phone's.
- Frame strips list only frames whose whole-screen difference passes a threshold, so a moving pill alone may not
  appear; crop the bar from `frames/` to judge bar motion (`decisions/CP1.md` §4).
- Compare push/pop with the critically damped curve for ω₀ 18.26 (54 % of travel at 100 ms); compare the back card
  with `research/predictive-back/gmail/key_frames.png`.
- Override a value: every value here is a token in `tokens/motion.json`; `decisions/CP1.md` "How to override" lists
  the recording to watch for each.
