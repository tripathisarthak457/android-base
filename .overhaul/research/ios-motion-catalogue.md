# iOS motion catalogue (iOS 17–26) and how to reproduce it in Compose without jank

Researched 2026-10-09 for REQUIREMENTS items 19–22. This file lists the iOS system animations and transitions with
the most exact parameters that could be sourced, and the cheapest Compose mechanism for each. It does not cover
frame pacing, GPU cost, or general recomposition hygiene; the other research files in this folder cover those
(`compose.md`, `platform.md`, `ios-parity.md`, `gpu-media.md`), and `research/predictive-back/` covers the back gesture.

## How to read the confidence tags

| Tag | Meaning |
|---|---|
| **[A]** | Apple documentation, HIG or WWDC session (official) |
| **[M]** | Measured by a third party from the real iOS object, e.g. Flutter reading `CASpringAnimation` properties in Xcode |
| **[E]** | Eyeballed by a third party side by side with iOS. Mostly the Flutter Cupertino fidelity work, with the iOS version noted in their source comments |
| **[D]** | Derived in this file from [A], [M] or [E] inputs. The working is shown in §1 |
| **[U]** | Unverified: an observation or estimate. It needs a recording on an iPhone before it becomes a token |

Source keys such as `S12` or `F2` refer to §7.

---

## 0. Findings that change what we build

1. **iOS push/pop is one critically damped spring with ω₀ ≈ 18.26 rad/s** (response ≈ 0.344 s, settles to 0.1 % in about
   0.51 s). In Compose that is `spring(dampingRatio = 1f, stiffness = 333f)` [D]. Flutter's eyeballed iOS page curve
   (`fastEaseInToSlowEaseOut`, 500 ms) [E, F1/F12] matches that spring with an RMS error of 0.0018 (§1.3). The constants
   are the same ones Flutter's engine uses for the iOS keyboard "curve 7" (mass 3, stiffness 1000, damping 500, which is
   overdamped, clamped to critical) [E, F13; clamping A, S7]. Two independent sources agree. Master plan A3.3 sets
   R = 0.24 s provisionally. That is about 1.4× faster than iOS: 74 % of the travel in the first 100 ms against 55 % on iOS.
   **Add 0.344 s to the checkpoint-1 candidates as the "iOS-exact" value.**
2. **Parallax is 1/3 of the width, not 30 %** [E, F1]. **The outgoing-screen dim is black at α ≈ 0.094** (`0x18000000`) [E, F1].
   The leading-edge shadow is very faint: a gradient from α 0.016 to 0 [E, F1]. The plan's light scrim of 0.10 is close
   enough. The 8 dp shadow is probably darker than iOS.
3. **Swipe-back commit rule:** commit if the release velocity is at least 1 screen width per second toward dismissal.
   Otherwise commit only if progress is above 0.5. The edge zone is 20 pt [E, F1]. iOS 26 also pops from a horizontal
   pan anywhere in the content (`interactiveContentPopGestureRecognizer`) [A, S15/S16].
4. **iOS 18+ never blocks navigation while a transition runs.** Apple says: "Don't avoid starting a new navigation push or
   pop based on whether a transition is currently running... the system handles the transitions." An interrupted push is
   completed and turned into a pop in the same run-loop turn [A, S12/S11]. This matters for the BackGate design
   (A3.4): iOS parity means retargeting in flight, not queueing (§5).
5. **Alerts, action sheets and dialogs use a critically damped spring with stiffness 522.35** (response 0.275 s, done at
   0.404 s). Flutter read this from `CASpringAnimation` in Xcode [M, F1]. An alert scales from 1.3 to 1.0 while it fades in,
   and dismisses with a fade only [E, F1]. The scrim is black at 0.20 (light) and 0.48 (dark) [E from Apple design
   resources, F1].
6. **Sheet card stack (iOS 18):** the presenter scales to 0.9165 about its top centre and shifts down 7 % of its height.
   Its corners go from about the device radius to 12 pt, and it gets a 10 % black overlay. The sheet's top sits at 8 % of
   screen height. A sheet dismisses at ≥ 2 screen heights per second or when dragged past 52 % [E, F2]. In iOS 26,
   partial-height sheets are inset glass with concentric corners. At the large detent they become opaque and
   edge-attached [A, S17; S26].
7. **Scroll deceleration:** `0.998` per ms (normal) and `0.99` per ms (fast) map exactly to Compose
   `exponentialDecay(frictionMultiplier = 0.4767f)` and `2.393f` [D, C1]. The rubber band is
   `y = (1 − 1/(x·0.55/d + 1))·d` [community, R1]. The default Android spline fling and Android 12+ stretch overscroll
   are not iOS-like.
8. **Tab switching on iOS is instant.** There is no transition unless the delegate supplies an animator [A, S23].
   Re-tapping the selected tab returns to its root [A, S22]. Six or more tabs show the first four plus "More" [A, S22].
   This settles the requirement-18 "7 tabs crammed" defect with Apple's own rule.
9. **Segmented thumb:** a critically damped spring with stiffness 503.55 (done at 412 ms). The pressed thumb scales to
   0.95 [M/E, F6]. **Switch:** 51 × 31 pt; the thumb stretches by 7 pt while pressed; a drag commits at 70 % and reverts
   at 20 % [E, F5]. In iOS 26 the switch, slider and segmented thumbs turn into a glass lens while held [A/E, S16, S37].
10. **Context menu:** a 0.5 s long press with 10 pt of allowed movement [A, S28]. The preview lifts to a scale of at most
    1.15 (at least 1.02 when space is tight). The backdrop gets blur σ 5 plus a 40 % tint. Dragging the preview down
    shrinks it to 0.8 and dismisses [E, F3].
11. **Keyboard:** `userInfo` reports a duration of 0.25 s and curve 7 [A forum, S35]. Curve 7 is the same ω₀ 18.26
    critical spring as push, so it is 94 % done at 0.25 s and 99.9 % done at 0.5 s [D]. On Android, frame-synced IME
    motion exists only on API 30+ (`WindowInsetsAnimationCompat`). On API 26–29 the inset jumps.
12. **Liquid Glass is built for performance as much as looks.** `GlassEffectContainer` exists so that several glass
    shapes share one sampling region and are "render[ed] together, improving rendering performance" [A, S19]. Shapes
    within `spacing` of each other merge, and `glassEffectID` morphs one shape into another. Materialising ramps the
    lensing, not the alpha [A, S18]. On Android with Kyant backdrop the equivalent is **one backdrop layer per bar or
    cluster**, with shapes unioned in one draw, never one blur pass per button.

---

## 1. Spring math: iOS to Compose

### 1.1 Conversions

| Quantity | Formula (mass = 1) | Source |
|---|---|---|
| stiffness from duration (response) | `k = (2π / duration)²` | [A] verified against Apple's example: `Spring(duration: 0.5, bounce: 0.3)` gives (1.0, 157.9, 17.6), S1 |
| damping coefficient | `c = 4π(1 − bounce) / duration` for bounce ≥ 0; `4π / (duration·(1 + bounce))` for bounce < 0 | [A] S1 example; negative branch [D] |
| Compose damping ratio | `ζ = 1 − bounce` (bounce ≥ 0) | [D] |
| bounce from (k, c) | `Spring(mass 1, stiffness 100, damping 10)` gives (duration 0.63, bounce 0.5), so ζ 0.5 means bounce 0.5 | [A] S1 |
| mass ≠ 1 | `k' = k/m`, `ζ = c / (2√(k·m))`. Compose has no mass, so divide k by m | [D] |
| over-damping | SwiftUI `Spring(mass:stiffness:damping:allowOverDamping: false)` treats ζ > 1 as **critically damped** | [A] S7 |
| `settlingDuration` | measured with target 1.0, v₀ 0 and ε 0.001 | [A] S1 |
| UIKit initial velocity | `initialSpringVelocity` is in "animation distances per second" (200 pt travel at 100 pt/s → 0.5) | [A] S8 doc |

### 1.2 Reference springs

| Name (where iOS uses it) | iOS definition | Compose `spring(...)` | Response / settle | Tag |
|---|---|---|---|---|
| **Navigation push/pop, keyboard curve 7, sheet present** | CASpring m 3, k 1000, c 500 → clamped critical, ω₀ 18.26 | `dampingRatio = 1f, stiffness = 333f` | 0.344 s / 0.51 s | [E]+[D] F13, F1 |
| **Alert, action sheet, modal popup** | m 1, k 522.35, c 45.71 (ζ = 1), duration 0.404 | `1f, 522f` | 0.275 s / 0.404 s | [M] F1 |
| **Segmented-control thumb** | m 1, k 503.551, c 44.88 (ζ = 1), 412 ms | `1f, 504f` | 0.280 s / 0.412 s | [M/E] F6 |
| SwiftUI `.smooth` (iOS 17 default family) | duration 0.5, bounce 0 | `1f, 158f` | 0.5 s | [A] S3 |
| SwiftUI `.snappy` | duration 0.5, base bounce 0.15 | `0.85f, 158f` | 0.5 s | [A] S3 |
| SwiftUI `.bouncy` | duration 0.5, base bounce 0.3 | `0.70f, 158f` | 0.5 s | [A] S3 |
| SwiftUI `Animation.default` (iOS 17+) | response 0.55, dampingFraction 1.0 (easeInOut before iOS 17) | `1f, 130f` | 0.55 s | [A] S4 |
| SwiftUI legacy `spring()` | response 0.5, dampingFraction 0.825 | `0.825f, 158f` | | [A] S6 |
| SwiftUI `interactiveSpring` (finger tracking) | response 0.15, dampingFraction 0.86, blend 0.25 | `0.86f, 1755f` | 0.15 s | [A] S5 |
| UIKit `UIView.animate(springDuration:)` default | duration 0.5, bounce 0 | `1f, 158f` | | [A] S8 |
| WWDC18 gesture dismiss with momentum | damping 0.8 ("a little bit of bounce and squish") | `0.8f, k from response` | | [A] S10 |
| Apple's general guidance | "start with 100 % damping"; bounce ≈ 0.15 is not felt as bouncy, 0.3 is; avoid > 0.4 | | | [A] S9/S10 |

### 1.3 Why the push spring is ω₀ 18.26 (the working)

`x(t) = 1 − e^(−ω₀t)(1 + ω₀t)` against Flutter's `fastEaseInToSlowEaseOut` over 500 ms:

| t (ms) | 16 | 33 | 50 | 75 | 100 | 150 | 200 | 250 | 300 | 400 | 500 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| Flutter iOS page curve | .040 | .128 | .238 | .403 | .546 | .760 | .880 | .942 | .973 | .994 | 1.00 |
| critical, ω₀ 18.26 (R 0.344) | .035 | .123 | .232 | .398 | .545 | .758 | .879 | .942 | .973 | .994 | .999 |
| critical, R 0.24 (plan) | .067 | .214 | .376 | .584 | .736 | .903 | .967 | .989 | .997 | 1.00 | 1.00 |
| critical, R 0.30 | .045 | .153 | .282 | .466 | .619 | .821 | .921 | .967 | .986 | .998 | 1.00 |

The least-squares fit gives ω₀ = 18.4 (R 0.341 s), RMS 0.0018. sqrt(1000/3) = 18.257 is the keyboard spring. The reference
script is `scratchpad/iosmotion/fit.py` (not in the repo).

### 1.4 Rules that make iOS motion feel like iOS (all [A] unless noted)

- **Everything is interruptible and velocity-preserving.** Springs retarget and keep their velocity (S6: "preserving
  velocity from one animation to the next"). In Compose, `Animatable.animateTo` and `animate*AsState` keep velocity on
  retarget automatically. When a gesture ends, pass the `VelocityTracker` velocity as `initialVelocity`. Never finish a
  gesture with a `tween`.
- **Track the finger with an interactive spring, then hand off.** During a drag use `snapTo` (or a 0.15 s ζ 0.86
  spring), and on release use the target spring seeded with the finger's velocity (S11).
- **Project before you snap** (WWDC18): `projection = v/1000 · r/(1 − r)` with r = 0.998 (or 0.99 for snappier). Pick the
  nearest snap point to the projected position, not to the release position [A, S10 sample code].
- **Hysteresis is 10 pt** before a swipe begins (S10). Android touch slop of 8 dp is equivalent.
- **Reduce Motion** turns elastic glass and parallax effects off and keeps fades (S18). Map it to the plan's `ReduceMotion`.

---

## 2. Catalogue

Phase column: **L** = layout or placement (`Modifier.offset { }`, `layout { }`, `placeWithLayer`), **D** = draw
(`graphicsLayer { }` lambda, `drawWithContent`, `drawBehind`), **C** = composition. No animated value may be read in C.

### 2.1 Navigation

| Item | iOS behaviour and parameters | Tag / source | Compose implementation (spec, phase) |
|---|---|---|---|
| Push | Incoming screen slides 100 % from the trailing edge. Outgoing screen slides −1/3 W (parallax) and darkens to black α 0.094. A 1 px-scale gradient shadow sits on the incoming leading edge (α 0.016 → 0). One critically damped spring, ω₀ 18.26 (§1.3), duration about 500 ms. iOS 18+ runs it on interruptible property animators. | [E] F1; [D] §1.3; S36 | Nav3 `transitionSpec`: `slideInHorizontally(screen) { it } togetherWith slideOutHorizontally(screen) { -it / 3 }` with `screen = spring(1f, 333f, IntOffset.VisibilityThreshold)` (L). Draw the scrim and edge shadow in a draw modifier that reads transition progress (D). No alpha or scale on screens. |
| Pop (button or Back key) | Exact mirror of push, same spring | [E] F1 | `popTransitionSpec` mirrored (L/D) |
| iOS 26 card corners during push/pop | The moving screen is drawn as a rounded "card" (radius ≈ display corner radius) while it moves and goes square at the end | [A-forum] S36 (developer reports; no API) | Optional token. `graphicsLayer { shape = RoundedCornerShape(r); clip = true }` on the moving entry (D, an outline clip, cheap). Take r from `Display.getRoundedCorner()` on API 31+, else 0. Off on API < 31. |
| Interactive edge swipe back | Edge zone ≈ 20 pt. The screen tracks the finger linearly (both screens, parallax 1/3). On release: if \|v\| ≥ 1.0 W/s, go in the fling direction; else commit if progress > 0.5. The dropped drag finishes on the page curve (350 ms in Flutter, which is the same curve). The nav bar title and back label cross-fade with progress. | [E] F1 | Android reserves both edges for system back, so map this to **predictive back** (Nav3 `predictivePopTransitionSpec`, linear track while seeking). On release, finish with the `screen` spring seeded with the gesture velocity where Nav3 allows it (plan A3.3 WP04 note). The commit rule is the same as above. Details are in `research/predictive-back/`. |
| iOS 26 full-content swipe back | A leading-to-trailing horizontal pan **anywhere** in the content pops. It yields to competing gestures (swipe actions, horizontal scrollers) "automatically". It does not start at the far trailing side. | [A] S15, S16; forum S36 | Optional `contentSwipeBack` on screens without horizontal scrollers: `Modifier.draggable(Horizontal)` on the entry host that drives the same seekable pop. It must be a **nested-scroll parent**, so horizontal lists and swipe rows consume first. Use 10 pt hysteresis. On 3-button-nav phones this is the only swipe-back. |
| Rapid or interrupted navigation | iOS 18+: a pop during a push completes the push and immediately starts the pop in the same run-loop turn. Callbacks always run the full cycle. Apple's guidance is to let the new push or pop start. | [A] S12, S11 | Retarget the running transition (`SeekableTransitionState` or AnimatedContent retargeting keeps velocity). Don't queue the presses one after another. See §5 for the BackGate delta. |
| Nav bar during push | The title cross-fades. The previous title flies into the back-button label position. Large titles slide with the content. Curves (eyeballed, iOS 17.5): top bar `Cubic(0, .45, .45, .98)`, bottom bar `Cubic(.05, .9, .9, .95)`. | [E] F9 | Cross-fade the titles with alpha read in `graphicsLayer` (D). Translating the back label is optional. iOS 26 back buttons are glass chevrons [U]. |
| Large title collapse | The bar is 44 pt plus a 52 pt large-title extension (96 total). Large title is 34 pt bold, inline title 17 pt semibold. The large title scrolls with the content. The inline title fades in (150 ms) once the large title has gone under the bar (10 pt threshold). The bar background fades in over the first 10 pt of scroll-under (iOS 17.4). On overscroll the large title magnifies, anchored bottom-start: `scale = 1 + 0.03 · extra/height`, at most 1.1. | [E] F9; type sizes [A] HIG | Large title is a plain list item, so it moves with the scroll for free. Inline title alpha and bar background alpha are derived from `firstVisibleItemScrollOffset` and read only in `graphicsLayer { }` / `drawBehind` (D). Overscroll magnify uses `graphicsLayer { scaleX = scaleY = s; transformOrigin = TransformOrigin(0f, 1f) }` (D). The 150 ms title fade is the one time-based fade. |
| Scroll edge effect (iOS 26) | Content under floating bars gets a "subtle blur and fade". `.soft` is the default and `.hard` is a more opaque separation for dense UIs. Use only behind floating elements, one effect per view. | [A] S29, S30, S16 | The plan's `scrollEdgeFade`: a gradient mask drawn in D, no blur. A real progressive blur is a GPU cost question (`gpu-media.md`); on Adreno 618 use a gradient scrim for "soft" and a solid tint for "hard". |
| Tab switch | **Instant**: no slide, fade or scale. Each tab keeps its navigation state. Only the bar's selection indicator moves. | [A] S22, S23, S21 | Matches plan A3.2. All tab roots stay composed and are shown or hidden by placement. Only the indicator animates, with its offset in L and colour via `ColorProducer`. |
| Re-tap the selected tab | Returns the tab to its root ("User taps always display the root view of the tab ... even if the tab was already selected"). A second tap at the root scrolls to top (Apple apps) [U]. | [A] S22; [U] | Pop to root with the normal pop spring, then `animateScrollToItem(0)` on a second tap. |
| Tab count | Six or more tabs: the bar shows the first four plus "More". The HIG recommends five or fewer. | [A] S22, S21 | Generator rule: ≤ 5 items, else 4 plus More (fixes the REQUIREMENTS 18 defect). |
| iOS 26 tab bar minimize | `onScrollDown`: the bar minimizes while content scrolls down and expands on scroll back, a tab tap, or scrolling to the top. `onScrollUp` is for bottom-anchored content. The bottom accessory moves inline when minimized. In practice the minimized bar is a small glass pill showing only the selected tab [U]. | [A] S20, S21, S16; [U] | A scroll-direction detector in a `NestedScrollConnection` (no state write per frame; flip a `Boolean` only when the direction changes after about 8–10 dp of hysteresis [U]). The bar's width and offset animate on the `selection` spring in L. Contents fade in D. |
| iOS 26 tab bar selection lens | The indicator slides with a "liquid stretch" on tap. On long-press it becomes a magnifying glass "selection bubble" with lensing and chromatic aberration that can be dragged across tabs. | [E] S37, community | Stretch at no extra cost: animate the indicator's **leading and trailing edges with two springs** (a stiffer leading edge in the direction of travel) and draw one rounded rect in D. Long-press drag maps the finger x to the nearest tab with a selection haptic per tab. |

### 2.2 Presentations

| Item | iOS behaviour and parameters | Tag / source | Compose implementation |
|---|---|---|---|
| Sheet present / dismiss | Slides from the bottom on the page curve (500 ms ≈ ω₀ 18.26 spring). Swipe down to dismiss: ≥ 2.0 screen heights/s, or dragged past 52 %. A dropped drag finishes in 300 ms easeOut. Pulling upward past the top stretches the top gap from 8 % to 7.2 % of H, then snaps back (180 ms easeOut). | [E] F2 (iOS 18 simulator) | `AnchoredDraggableState` (Hidden / Medium / Large). Offset read in `Modifier.offset { }` (L). Fling uses a positional threshold of 0.5 and a velocity threshold of 2 H/s, and settles on the `screen` spring with the release velocity. Upward over-pull uses the rubber band from §2.3. |
| Detents | `large` = full height. `medium` ≈ half the screen and inactive in compact height. Custom detents resolve from context. Scrolling in the sheet first grows it to the next detent (`prefersScrollingExpandsWhenScrolledToEdge` = true by default). The grabber is optional and tapping it cycles detents. Dimming applies at every detent unless `largestUndimmedDetentIdentifier` is set, in which case the area under the sheet stays interactive. | [A] S24, S25 | Nested-scroll connection: pre-scroll goes to the sheet until it reaches the top detent, post-fling settles the detent. Measure the content once at the large detent and only offset it (plan A3.7). |
| Card stack behind a sheet | The presenter scales to **0.9165** (`1 − 0.0835`, measured on iPhone 16 Pro, iOS 18) about its **top centre** and moves down **7 % of its height**. Its corners go from ~0.9 × the top safe-area inset (≈ display radius) to **12 pt**. A **10 %** overlay goes on top (black in light mode, `#C8C8C8` in dark). The window behind is black, so the status bar switches to light content. Corner radius was 10 pt on iOS 13–15. | [E] F2; iOS 13 radius [community] | One `graphicsLayer { scaleX; scaleY; transformOrigin = TransformOrigin(0.5f, 0f); translationY; shape; clip = true }` on the presenter root, reading sheet progress (D). Draw the overlay as a rect, **not** `alpha` (alpha on a large layer forces an offscreen pass). Status-bar icons switch with progress > 0.5. |
| Stacked sheets | The previous sheet scales to 0.9165 and moves up 0.5 %. The new sheet stops 2 % higher than a first sheet would. | [E] F2 | Same transform on the lower sheet (D) |
| iOS 26 sheets | Partial-height sheets are **inset** from the edges with a Liquid Glass background. At smaller heights the bottom corners pull in, concentric with the display corners. Dragging to the large detent turns the glass opaque and attaches the sheet to the sides and bottom. Sheets can morph out of the source button (`navigationTransition(.zoom)` / `preferredTransition = .zoom` with a bar-button source). | [A] S17, S16; S26 | The inset and corner radius lerp with detent progress (L for the inset, D for the shape). Glass is opt-in (Kyant): one backdrop for the sheet surface, and an opaque surface at the large detent so the blur stops entirely. |
| Full-screen cover | `coverVertical`: slides up the full height and back down on dismiss. The presenter is not scaled or dimmed. No interactive dismissal by default. | [A] UIKit `UIModalTransitionStyle.coverVertical` (default) | `modal` spec from plan A3.3, on the `screen` spring (L) |
| Zoom / hero (iOS 18) | The source view (thumbnail or cell) morphs into the destination, which zooms from the source rect. It is "continuously interactive", so it can be grabbed and dragged during the transition and reversed. Options: `dimmingColor`, `dimmingVisualEffect`, `alignmentRectProvider`, `interactiveDismissShouldBegin`. The source is looked up by a stable id, not a captured view. Dismissal by dragging down [A in principle]; edge swipe and pinch [U]. | [A] S11, S12, S13, S14 | `SharedTransitionLayout` + `Modifier.sharedBounds()` with Nav3's `LocalNavAnimatedContentScope`. Keep the **default `ScaleToBounds`**: `RemeasureToBounds` "incurs frame-by-frame remeasurement" [A androidx, C5]. `boundsTransform` = the `screen` spring. Interactive dismiss is a vertical drag that scales the destination by `1 − 0.3·dy/H` [U] in D and on release seeks or retargets the pop. Use plain `RoundedCornerShape` clip shapes. |
| Context menu | Long press 0.5 s with 10 pt of allowed movement. The preview lifts out of the content with a shadow (α 0.25, blur 10) and scales up to **1.15**, or to the largest scale ≥ **1.02** that fits the safe area. Preview corner radius 12. The backdrop blurs (σ 5) and dims (`0x6604040F`, α 0.4). The menu is 250 pt wide. Dragging the preview down shrinks it toward 0.8 and hides the menu below 0.9 scale. Lift timing eyeballed on iOS 16: about 800 ms from finger-down to fully open, including the hold. Haptic on open [U]. HIG: make the preview clip path match the content so its corners don't change during the animation. | [A] S28, S27; [E] F3 | `detectTapGestures(onLongPress)` with a press-progress `Animatable` for the slight lift during the hold (D). On trigger, snapshot the row into a `GraphicsLayer` (`rememberGraphicsLayer` + `record`) and animate that layer in an overlay, so the list never recomposes. For the backdrop, use a **captured-and-blurred snapshot** (`RenderEffect` blur on API 31+, on a 1/4-scale snapshot) or a 40 % tint on API < 31. No live blur. Haptic `LongPress`. |
| Alert | Appears scaling from **1.3 → 1.0** and fading in, on the [M] spring k 522 (0.404 s). Disappears with a **fade only**. Scrim 0.20 light / 0.48 dark. 270 pt wide (310 for accessibility sizes), corner radius 14, backdrop blur σ 30 in Flutter's model. | [M] F1 spring; [E] F1/F4 | `graphicsLayer { scaleX = s; scaleY = s; alpha = a }` on a small surface (D). Alpha on a small card is cheap. Scrim drawn as a rect. Spring `1f, 522f`. |
| Action sheet (≤ iOS 18) | Slides up from the bottom on the same k 522 spring (popup duration 335 ms). 8 pt edge padding, a separate cancel group, scrim as for alerts. | [M/E] F1, F4 | Offset in L, spring `1f, 522f` |
| Action sheet, popover, menu (iOS 26) | Anchored to the source on iPhone too. When opened from a glass button, the button **morphs** into the overlay: "the bubble simply pops open to reveal the content... right where you just tapped". Glass gets thicker and more refractive at larger sizes. | [A] S16, S18 | Scale from the anchor plus a shape lerp (pill → rounded rect) in D. Content fades in after about 40 % of the travel [U]. Use `sheet`-class critical springs, or 0.1–0.15 bounce for "pop" [U]. |

### 2.3 Scrolling

| Item | iOS behaviour and parameters | Tag / source | Compose implementation |
|---|---|---|---|
| Deceleration | `velocity *= rate` every millisecond, with **normal 0.998** (default) and **fast 0.99**. Equivalent per-second decay `e^(−2.002·t)` (normal) and `e^(−10.05·t)` (fast). Distance to rest = `v · r/(1 − r)/1000`, i.e. 0.499·v (normal) and 0.099·v (fast), with v in px/s. | [A] names S34-doc; values [E] F11 ("Taken from UIScrollView.decelerationRate (.normal = 0.998)"), WWDC18 sample code | `FlingBehavior` over `exponentialDecay(frictionMultiplier = 0.4767f)` (fast: `2.393f`), since Compose friction = −4.2 × multiplier per second [C1]. There is **no CompositionLocal for the default fling**, so the DS list and scroll wrappers must pass it. Set `absVelocityThreshold` so the tail stops at about 10–20 px/s [U]. |
| Momentum accumulation | Repeated flings stack speed: carried = `sign · min(0.000816·\|v\|^1.967, 40000)` px/s, added when a new fling starts during a running one | [E] F11 (fitted against iOS with Flutter's scroll_overlay tool) | Inside the custom `FlingBehavior`, keep the last fling's live velocity and add the carried term when a new fling begins within the running decay |
| Min fling / drag start | Flutter's iOS physics: min fling 100 px/s, drag-start threshold 3.5 px. WWDC18 hysteresis 10 pt. | [E] F11; [A] S10 | Defaults are fine. Ignore flings under ~100 dp/s [U]. |
| Rubber band while dragging | `y = (1 − 1/(x·c/d + 1))·d`, with c = 0.55 and d = viewport size along the axis. Equivalent form: `x·d·c/(d + c·x)`. Approaches d asymptotically (x = d → y = 0.355 d). The inverse, for finger reversal, is `x = d·y/(c·(d − y))`. | community (Grant Paul's formula, quoted in R1); constant widely reused | Custom `OverscrollEffect` installed app-wide via **`LocalOverscrollFactory`** [C2]. `applyToScroll` keeps the raw overscroll x in a plain float. The visual offset is read in the effect's node in `placeWithLayer` (L). No recomposition. |
| Release from overscroll (no velocity) | Exponential return with a **half-life of 70 ms** (λ = ln2/0.07 = 9.9 /s): 95 % back in about 0.3 s | [E] F11 ("approximation of iOS native rubber band decay rate") | `Animatable.animateTo(0f, spring(dampingRatio ≈ 1f, stiffness ≈ 98f))` is close (ω = λ). Exact match: an overdamped spring with roots −λ and −10⁵λ, as Flutter does. |
| Fling into the edge (bounce) | The fling crosses the edge carrying its velocity (Flutter caps the transfer at 5000 px/s) and is caught by a spring: m 0.5, k 100, ζ 1.1 (fast mode: m 0.3, k 75, ζ 1.3) | [E] F11 | `applyToFling`: hand the remaining velocity to `spring(dampingRatio = 1.1f, stiffness = 200f)` (k/m). Cap the velocity. |
| Bounce when content is short | iOS lists bounce even when the content is shorter than the viewport (`alwaysBounceVertical`) | [A] S34 (`bounces`); [U] for the list default | The overscroll effect must apply even when `canScroll == false` |
| Paging | `isPagingEnabled` stops on multiples of the bounds. SwiftUI uses `.scrollTargetBehavior(.paging)` and `.viewAligned`. Uses fast deceleration. | [A] S34 docs | `HorizontalPager` with `PagerDefaults.flingBehavior(snapAnimationSpec = spring(1f, 333f))`. Target page from the projection with r = 0.99, clamped to ±1 page. |
| Touch delay in scroll views | `delaysContentTouches = true`: highlight is delayed about **150 ms** until the scroll intent is known. A quick tap still flashes the highlight on touch-up. | [A] S34; 150 ms from WWDC14 235 notes | Compose `clickable` already delays the press by `TapIndicationDelay` = tap timeout (100 ms) inside scrollables [C6]. Keep it, and render the highlight on release for quick taps (plan `RowHighlight`). |
| Scroll to top | Status-bar tap scrolls to top (animated). There is no Android equivalent, so the tab re-tap is the substitute. | [A] UIKit `scrollsToTop` | `animateScrollToItem(0)`. For long lists, jump to about 2 screens from the top first, then animate. |

### 2.4 Controls and press feedback

| Item | iOS behaviour and parameters | Tag / source | Compose implementation |
|---|---|---|---|
| Plain/system button press | On press, opacity goes to **0.4** (fade-out 120 ms, `easeInOutCubicEmphasized`). On release it fades back to 1 (180 ms, easeOutCubic). A quick tap finishes the dip before the release. | [E] F10 ("eyeballed") | Plan `Dim` (alpha 0.45 via `ModulateAlpha`) matches. Read the alpha only in D. |
| Filled / tinted button press | Background darkens or lightens while highlighted; no scale on iOS ≤ 18 [U] | [U] | Plan `Scale` 0.96 is a design choice, not an iOS default. Keep it subtle. |
| iOS 26 glass button press | Interactive glass reacts "by scaling, bouncing, and shimmering". It "illuminates from within", and the glow spreads to nearby glass. The press scales **up**, not down [U magnitude ≈ 1.05–1.1]. | [A] S17, S18; [U] | Glass-only `Scale(up = 1.06)` [U] on `release`-class spring, and a radial highlight drawn at the touch point in D (one gradient, cheap). No extra backdrop pass. |
| Row highlight | A grey highlight (`systemGray4`-like) after the 150 ms scroll delay. On return from a pushed screen the row deselects **in step with the pop**, and the selection is restored if the swipe-back is cancelled (UIKit `clearsSelectionOnViewWillAppear` + transition coordinator) | [A] clears-selection doc; animation-sync detail [U] | Highlight alpha is driven by pop progress (D) while the screen is uncovered |
| Switch (≤ iOS 18) | 51 × 31 pt track; thumb radius ≈ 14 pt. While pressed the thumb **widens by 7 pt** toward the centre (300 ms reaction). Position animation 200 ms ease. A drag commits at **0.7** of the track and reverses back at **0.2**. Thumb shadows `0x26000000` y 3 blur 8 and `0x0F000000` y 3 blur 1. Plays a haptic on toggle. | [E] F5; haptic [A] S32 | One `Modifier.Node` drawing the track and thumb in D. Thumb x and width come from two `Animatable`s (`selection` spring). Haptic `ToggleOn`/`ToggleOff` (API 34+, `CONTEXT_CLICK` below). |
| Switch (iOS 26) | Pill design. The thumb becomes a Liquid Glass lens with a highlight/shadow that is not clipped by ancestors and can be dragged beyond the control | [E/forum] S37; search result | Lens is opt-in with glass on. Otherwise a slightly enlarged thumb on press. Draw outside the bounds without clipping (no `clip`) in D. |
| Segmented control | The thumb slides on the **k 503.55 critical spring (412 ms)**. A pressed selected thumb scales to **0.95**; a momentary press peaks at **1.05**. Pressed unselected segment content dims to opacity **0.2**. Font 13 pt, w500, w600 when selected. Height ≥ 28, corner 9, thumb corner 7. The thumb can be dragged across segments. | [M/E] F6 | Thumb rect from two edge springs (`1f, 504f`) in L or D. Label weight change is static per selection (C, only on change). Selection haptic per segment crossed. |
| Stepper | `autorepeat = true` by default: holding repeats the change. Timing (initial delay ≈ 0.5 s, then accelerating repeats) [U] | [A] stepper doc; [U] | Coroutine repeat with `delay` acceleration. Haptic `SegmentTick` per step. |
| Picker wheel | A cylinder: diameter ratio **1.07**, perspective **0.003**, squeeze **1.45**. Rows off-centre have opacity **0.447**. Tapping a row scrolls to it (300 ms easeInOut). Row extent 32 pt. Rounded selection band. Decelerates and snaps per row. A selection haptic on every row crossed. | [E] F7; haptics [A] S32 | `LazyColumn` with snap fling (iOS decay + `SnapLayoutInfoProvider`). Per-row rotationX, scale and alpha computed in `graphicsLayer { }` from the item offset (D, no recomposition). Haptic `SegmentFrequentTick`, throttled to one per frame. |
| Slider (iOS 26) | The knob turns into a glass bubble that grows under the finger. Supports momentum like scrolling. | [E] S37 | Knob scale spring on press (D). After release, apply an iOS decay to the value, clamped. |

### 2.5 Lists

| Item | iOS behaviour and parameters | Tag / source | Compose implementation |
|---|---|---|---|
| Insert | The row fades and slides into place while the rows below move down. Batch updates animate together. Timing ≈ 0.3 s [U]. | [A] S-batch (`performBatchUpdates`, diffable `apply(animatingDifferences:)`); [U] timing | `Modifier.animateItem(fadeInSpec, placementSpec, fadeOutSpec)` (Compose 1.7+, default `StiffnessMediumLow` = 400) [C3]. Use the plan's `layout` spring (0.36 s). |
| Delete | The row collapses to zero height while its content slides out, and the rows below close the gap | [U] | `animateItem(fadeOutSpec = …)` for removal plus placement. For swipe-delete, run the swipe-out first, then remove (the height collapse is placement). |
| Reorder | Long press lifts the row (shadow plus slight scale [U] ≈ 1.03) with a haptic. Other rows shift on springs while dragging. Drop settles on a spring. | [U] | Drag offset in `graphicsLayer { translationY }` (D). Neighbours use `animateItem` placement. Haptic `DragStart`/`SegmentTick`. |
| Swipe actions | The row tracks the finger. Actions are revealed underneath. Only one row is open at a time, and a tap elsewhere closes it. **A full swipe performs the first action** (default true). Past the full-swipe point the first action expands across the row [U: about 60 % of the row width] with a haptic [U]. If full swipe is off, overdrag rubber-bands. | [A] S-swipe docs (default `allowsFullSwipe`/`performsFirstActionWithFullSwipe` = true); [U] threshold | `AnchoredDraggableState` (Closed / Open / Full). Offset in L. Action widths are revealed with clip in D. Crossing Full plays haptic `GestureThresholdActivate` (API 34+) and springs the first action's width. Rubber band from §2.3 past the last anchor. |
| Pull to refresh | Tick spokes are revealed in proportion to the pull. Past the trigger it spins, and the content stays inset by the indicator height until the refresh ends, then springs back. Flutter values: **trigger 100 pt, indicator extent 60 pt**, spinner radius 14. Opacity ramps over the first 35 % of the pull. iOS's own trigger distance [U]. | [E] F8; [U] iOS exact | A nested-scroll connection on top of the rubber-band overscroll. Spinner rotation and tick reveal in D. Haptic `Threshold` at the trigger (plan). |

### 2.6 Text input, search, keyboard

| Item | iOS behaviour and parameters | Tag / source | Compose implementation |
|---|---|---|---|
| Keyboard show/hide | `keyboardWillShow` userInfo: **duration 0.25, curve 7**. Later notifications while typing have duration 0. Curve 7 is the ω₀ 18.26 critical spring (94 % at 0.25 s). `UIKeyboardLayoutGuide` tracks it in layout. | [A-forum] S35; [E] F13; [D] | API 30+: `Modifier.imePadding()` / `WindowInsets.ime` read in layout (L) is frame-synced through `WindowInsetsAnimationCompat`, and the Android IME curve is the system's (can't match iOS). API 26–29: no animation callbacks. Accept the jump, or animate the inset change with the `screen` spring. Never read `WindowInsets.ime` in composition. |
| Interactive keyboard dismiss | Dragging the scroll view down drags the keyboard with the finger (`keyboardDismissPadding` widens the touch area) | [A] UIKeyboardLayoutGuide doc | API 30+: `Modifier.imeNestedScroll()` (experimental) or `WindowInsetsControllerCompat.controlWindowInsetsAnimation`. Off below 30. |
| Search focus (iOS ≤ 18, in the nav bar) | Tapping the field hides the large title and bar (`hidesNavigationBarDuringPresentation`). The field moves to the top and shrinks as **Cancel** (≈ 67 pt) slides in from the trailing edge, and the content dims. **300 ms easeInOut**. The field is 36 pt high. | [A] S-search docs; [E] F9 | Field width and offset from one progress `Animatable` (`sheet` spring) read in L. Cancel slides in L. Dim drawn in D. Focus is requested after the first frame of the animation, so the IME opening doesn't add a cold-start frame [U]. |
| Search (iOS 26) | Search sits at the bottom (toolbar or `Tab(role: .search)`). Tapping "animates into a search field above the keyboard". `searchToolbarBehavior(.minimize)` collapses it into a button. | [A] S17, S31 | Bottom search pill: on tap, morph the shape (pill → full-width field) with its offset synced to the IME inset (L) |

### 2.7 Feedback: toasts, haptics, symbols, numbers

| Item | iOS behaviour and parameters | Tag / source | Compose implementation |
|---|---|---|---|
| Toast / HUD | **iOS has no public toast or HUD.** System confirmations (AirPods card, "Copied", Dynamic Island activities) drop or expand from the top on springs. Suggested: from the top or Dynamic-Island area, scale 0.9 → 1 plus translate, `snappy`-class spring (0.4 s, bounce 0.15). Swipe up to dismiss with velocity. Auto-hide after 2–3 s. Notification haptic. | [U] design guidance | Overlay host above the shell. Offset in L, scale and alpha in D on a small surface. `Confirm`/`Reject` haptics. One host, no per-toast composition at the root. |
| Haptics | Switches, sliders and pickers play haptics automatically. Three families: notification, impact, selection ("selection haptics provide feedback while the values of a UI element are changing"). | [A] S32 | Already mapped in plan A4.2. Add selection haptics on picker rows, segmented thumb crossings, tab-lens drags and the swipe-action full threshold. |
| SF Symbols effects | Discrete: `bounce` (up/down, by layer or whole), `wiggle`, `rotate`. Indefinite: `pulse`, `variableColor`, `breathe`, `scale`. Transitions: `appear`/`disappear`, `replace` (`downUp`, `offUp`, `upUp`, plus **Magic Replace** for shared layers). iOS 26 adds `drawOn`/`drawOff`. | [A] S33 | Animated icon primitives in the DS: bounce = scale 1 → 1.2 → 1 on a bouncy spring [U magnitude]; replace = outgoing scale-down and fade plus incoming scale-up (`downUp`). Both icons are composed once and alpha/scale are read in `graphicsLayer` (D). `AnimatedImageVector` only for real path morphs. Indefinite effects stop when the tab is hidden (`LocalTabVisible`). |
| Numeric text | `contentTransition(.numericText())`: digits roll vertically and change individually | [A] SwiftUI doc | Per-digit `AnimatedContent` is too heavy for counters that tick often. Draw the digits in one `drawText` call with a per-digit y-offset (D), using a `TextMeasurer` cache. |

### 2.8 Liquid Glass (when glass is on)

| Item | iOS behaviour and parameters | Tag / source | Compose / Kyant implementation |
|---|---|---|---|
| Material | Lensing ("bends, shapes, and concentrates light in real time"). Regular is adaptive (tint, light/dark flip, and a shadow that strengthens over text). Clear is permanently transparent. Larger glass is thicker, with deeper shadows and stronger refraction. | [A] S18 | Two presets. Shadow and tint are static per surface size class, never recomputed per frame. |
| Materialize / dematerialize | "Instead of fading, Liquid Glass objects materialize in and out by gradually modulating the light bending and lensing." UIKit: set `effect = UIGlassEffect()` / `nil` inside `UIView.animate`. | [A] S18, S16 | Animate the backdrop's refraction height or blur radius from 0 → target. Don't use layer alpha, which would force an offscreen pass over the backdrop. |
| Interactive press | "Responds to interaction by instantly flexing and energizing with light". Gel-like flex that "moves in tandem with your interaction". The glow starts under the finger and spreads to nearby glass. `.interactive()` / `isInteractive = true`. Reduce Motion "disables any elastic properties". | [A] S18, S17, S16 | Press scale-up plus a slight stretch toward the drag direction (scaleX/scaleY from drag delta, D). Glow is a radial gradient at the touch point. Off under ReduceMotion. |
| Container and merge | `GlassEffectContainer(spacing:)`: shapes within `spacing` "start to blend into one another", and higher spacing blends sooner. Glass can't sample glass, so the container shares one sampling region and renders them together "improving rendering performance". UIKit: `UIGlassContainerEffect.spacing` (example 20). | [A] S19, S17, S16 | **One backdrop layer per container.** Union the child shapes (metaball-style smooth union within `spacing`) into one path or SDF per frame and draw the backdrop once through it. This is the main FPS fix for PeriFlow-style glass. |
| Morph (`glassEffectID`) | With the same `@Namespace`, glass shapes morph into each other during transitions ("fluid morphing"; badges "re-absorbed gracefully"). `glassEffectTransition`: `.matchedGeometry` (default within a container), `.materialize`, `.identity`. `glassEffectUnion` merges distant shapes into one. | [A] S19, S17 | Keyed shape registry inside the container. On add/remove, lerp rect and corner radius from the source key's shape on the `sheet` spring (or 0.15 bounce), drawn in D. |
| Concentric corners | `ConcentricRectangle` / `.containerConcentric` / `cornerConfiguration = .containerRelative()`: the radius follows the distance to the container corner | [A] S17, S16 | `radius = max(0, containerRadius − inset)`, computed in layout. Use the device radius from `Display.getRoundedCorner()` (API 31+). |
| Glass sheet → opaque | A partial sheet is glass. As it expands to full height "the glass background gradually transitions, becoming opaque". | [A] S17 | Lerp the surface opacity over the backdrop and stop drawing the backdrop at 1.0 |

---

## 3. Implementation contract for every animation above

These rules come from the iOS behaviour plus Compose mechanics. They are not general perf advice.

1. **One spring per gesture family, precomputed.** Hold the §1.2 springs as `@Immutable` `SpringSpec`s
   (plan `AppSprings`). Per family: `screen` = ω₀ 18.26 if the user picks iOS-exact, `dialog` = k 522, `thumb` = k 504,
   `interactive` = k 1755 ζ 0.86.
2. **Drive from `Animatable`/`AnchoredDraggableState` held in a remembered state holder.** Read the value only in
   `offset { }`, `layout { }`, `graphicsLayer { }`, `drawWithContent { }`, or inside a `Modifier.Node`
   (`LayoutModifierNode`/`DrawModifierNode`), so 120 Hz animation costs no recomposition. Pass colours as `ColorProducer`.
3. **Gesture end:** take `VelocityTracker.calculateVelocity()`. Project with r = 0.998 (0.99 for paging and detents) to
   pick the target, then `animateTo(target, spec, initialVelocity = v)`. Thresholds are in screen fractions per second
   (pop 1 W/s, sheet 2 H/s).
4. **Interrupt by retargeting.** A new target during an animation calls `animateTo` again (velocity is kept). Navigation
   retargets the running transition and does not wait for it to finish.
5. **Never alpha a full screen.** Dim with a drawn rect. Alpha is fine on small cards (alerts, toasts). Corners use an
   outline clip (`graphicsLayer { shape; clip }`), never `Modifier.clip` with a custom `Path` on a moving full screen.
6. **Shared elements:** `sharedBounds` with the default `ScaleToBounds`. Never `RemeasureToBounds` on text or complex
   trees [C5].
7. **Lists:** `animateItem` for insert/delete/move; the item key is stable. Per-row scroll-linked effects (picker
   cylinder, parallax headers) are read from `LazyListState` inside `graphicsLayer`.
8. **Rubber band and decay app-wide:** `LocalOverscrollFactory` provides the iOS overscroll. The DS `List`, `Column` and
   `Pager` wrappers pass the iOS `FlingBehavior`, since Compose has no default-fling local.

---

## 4. Recomposition checks specific to motion (feeds requirement 22)

Count these in the recomposition-count tests on the critical journeys. Each should show **0 recompositions of the
animated subtree per frame**; only state flips (selected index, open/closed) recompose, once.

| Journey | What must not recompose per frame |
|---|---|
| Push, pop, swipe-back scrub | Both entries' content. Only the transition's internal nodes update. |
| Tab switch | Every tab root, and the bar items except the two whose selected flag flips |
| Scroll to collapse a large title and minimize the tab bar | Top bar, title and tab bar: alpha, offset and width read in L/D. The minimize `Boolean` flips once. |
| Sheet drag across detents | Sheet content and presenter. Offset is in L, presenter transform in D. |
| Switch / segmented / picker drag | The control's parent. The `Modifier.Node` invalidates draw only. |
| Overscroll bounce | The list items. The overscroll node offsets in placement. |
| Context menu open | The source list, because the preview is a recorded `GraphicsLayer`. |

---

## 5. Deltas against MASTER-PLAN (A3.3, A3.4, A4.3) for the orchestrator

| Plan item | Plan value | iOS reference | Suggested action |
|---|---|---|---|
| `screen` response (A3.3/A4.3) | 0.24 s provisional; candidates 0.24 / 0.30 / 0.36 | **0.344 s** critical (stiffness 333) [D] | Replace 0.36 with **0.344** and label it "iOS-exact" at checkpoint 1. The user can still pick faster. |
| `navigation.parallax` | 0.30 | **1/3** [E] | 0.333 |
| Scrim light / dark | 0.10 / 0.24 | light **0.094** [E]; dark not sourced [U] | Keep. Light is fine; record dark against an iPhone if one becomes available. |
| Edge shadow 8 dp | | gradient α **0.016 → 0** [E] | Make it fainter; it should barely be visible |
| `sheet` response | 0.34 s | presentation on the push curve, ≈ **0.344 s** critical [D from F2] | Matches |
| Dialog / action sheet | `sheet` spring | k **522** (0.275 s, done at 0.404 s) [M] | Add a `dialog` token: response 0.275, bounce 0 |
| Alert entrance | | scale **1.3 → 1.0** + fade in; fade-out only on dismiss [E] | Use as specified |
| `selection` (thumbs, pills) | 0.30 s, 0.6 × style bounce | segmented thumb k **504**, critical (0.28 s) [M/E] | For Standard, bounce 0 for thumbs matches iOS. Bounce belongs on press release, not on indicators. |
| BackGate "coalesce" (A3.4) | Back presses are counted during the transition and applied after it | iOS 18+: never block; the new pop starts at once and retargets in flight [A] | Keep coalescing as the glitch fix if retargeting proves unstable on device. The iOS-parity target is: apply each Back immediately and **retarget** the running `AnimatedContent`/seekable transition to the new top entry, carrying velocity. Compose the destination ahead of time (keep the previous entry composed) so retargeting never hits a cold composition. |
| Tab overflow | (requirement 18) | ≤ 5 tabs; 6+ → **4 + More** [A] | Make it a generator rule |
| Decay | Compose default spline | **`exponentialDecay(0.4767f)`** [D] | Add `AppScroll.fling` and pass it in the DS wrappers |
| Overscroll | `AppOverscroll` opt-in | rubber band c 0.55, release half-life 70 ms [E] | Use these constants. Default it on, since iOS lists always bounce. |

---

## 6. Open items: what still needs an iPhone (no Mac or iPhone is available here, so all remain [U])

Measure each by screen-recording an iPhone at 120 Hz (ProMotion), stepping frames and plotting position against time.
Fit a critical or underdamped spring as in §1.3. Flutter's `flutter/platform_tests` scroll_overlay is the method for
scroll physics.

1. Dark-mode navigation scrim alpha; iOS 26 card corner radius timing during push.
2. Full-swipe threshold for swipe actions, and whether a haptic fires there.
3. UIRefreshControl trigger distance and haptic.
4. Tab bar minimize hysteresis and its spring; tab lens stretch magnitude.
5. Glass press scale-up magnitude; iOS 26 menu pop timing and bounce.
6. Zoom-transition dismiss gestures (pinch? edge?) and its spring.
7. List insert/delete timing (UIKit batch updates) and reorder lift scale.
8. Stepper autorepeat timing.
9. iOS 26 alert entrance (did 1.3 → 1.0 change?).

---

## 7. Sources

Apple (official):
- S1 SwiftUI `Spring` (conversion example 157.9 / 17.6): https://developer.apple.com/documentation/swiftui/spring
- S2 `Spring.init(duration:bounce:)` (defaults 0.5 / 0): https://developer.apple.com/documentation/swiftui/spring/init(duration:bounce:)
- S3 `Animation.snappy/bouncy/smooth(duration:extraBounce:)` (base bounce 0.15 / 0.3 / 0, duration 0.5): https://developer.apple.com/documentation/swiftui/animation/snappy(duration:extrabounce:) , https://developer.apple.com/documentation/swiftui/animation/bouncy(duration:extrabounce:) , https://developer.apple.com/documentation/swiftui/animation/smooth(duration:extrabounce:)
- S4 `Animation.default` (response 0.55, dampingFraction 1.0): https://developer.apple.com/documentation/swiftui/animation/default
- S5 `interactiveSpring(response: 0.15, dampingFraction: 0.86, blendDuration: 0.25)`: https://developer.apple.com/documentation/swiftui/animation/interactivespring(response:dampingfraction:blendduration:)
- S6 `spring(response: 0.5, dampingFraction: 0.825)` and velocity preservation: https://developer.apple.com/documentation/swiftui/animation/spring(response:dampingfraction:blendduration:)
- S7 `Spring.init(mass:stiffness:damping:allowOverDamping:)` (over-damping treated as critical): https://developer.apple.com/documentation/swiftui/spring/init(mass:stiffness:damping:allowoverdamping:)
- S8 `UIView.animate(springDuration: 0.5, bounce: 0.0, …)`: https://developer.apple.com/documentation/uikit/uiview/animate(springduration:bounce:initialspringvelocity:delay:options:animations:completion:) and the velocity unit: https://developer.apple.com/documentation/uikit/uiview/animate(withduration:delay:usingspringwithdamping:initialspringvelocity:options:animations:completion:)
- S9 WWDC23 Animate with springs: https://developer.apple.com/videos/play/wwdc2023/10158/ (notes: https://wwdcnotes.com/documentation/wwdc23-10158-animate-with-springs/)
- S10 WWDC18 Designing Fluid Interfaces (100 % / 80 % damping, 10 pt hysteresis, projection): https://developer.apple.com/videos/play/wwdc2018/803/
- S11 WWDC24 Enhance your UI animations and transitions (zoom, push-to-pop conversion, velocity merging): https://developer.apple.com/videos/play/wwdc2024/10145/
- S12 Enhancing your app with fluid transitions: https://developer.apple.com/documentation/uikit/enhancing-your-app-with-fluid-transitions
- S13 `UIViewController.Transition.ZoomOptions`: https://developer.apple.com/documentation/uikit/uiviewcontroller/transition/zoomoptions
- S14 `navigationTransition(_:)`, `zoom(sourceID:in:)`, `matchedTransitionSource(id:in:)`: https://developer.apple.com/documentation/swiftui/view/navigationtransition(_:)
- S15 `interactiveContentPopGestureRecognizer`: https://developer.apple.com/documentation/uikit/uinavigationcontroller/interactivecontentpopgesturerecognizer
- S16 WWDC25 Build a UIKit app with the new design: https://developer.apple.com/videos/play/wwdc2025/284/
- S17 WWDC25 Build a SwiftUI app with the new design: https://developer.apple.com/videos/play/wwdc2025/323/
- S18 WWDC25 Meet Liquid Glass: https://developer.apple.com/videos/play/wwdc2025/219/
- S19 `GlassEffectContainer`, `glassEffectID(_:in:)`, `GlassEffectTransition`: https://developer.apple.com/documentation/swiftui/glasseffectcontainer , https://developer.apple.com/documentation/swiftui/view/glasseffectid(_:in:) , https://developer.apple.com/documentation/swiftui/glasseffecttransition
- S20 `tabBarMinimizeBehavior(_:)` and `UITabBarController.MinimizeBehavior`: https://developer.apple.com/documentation/swiftui/view/tabbarminimizebehavior(_:) , https://developer.apple.com/documentation/uikit/uitabbarcontroller/minimizebehavior
- S21 HIG Tab bars: https://developer.apple.com/design/human-interface-guidelines/tab-bars
- S22 `UITabBarController` (root on re-tap, 4 + More): https://developer.apple.com/documentation/uikit/uitabbarcontroller
- S23 `tabBarController(_:animationControllerForTransitionFrom:to:)`: https://developer.apple.com/documentation/uikit/uitabbarcontrollerdelegate/tabbarcontroller(_:animationcontrollerfortransitionfrom:to:)
- S24 HIG Sheets: https://developer.apple.com/design/human-interface-guidelines/sheets
- S25 `UISheetPresentationController.Detent`, `largestUndimmedDetentIdentifier`, `prefersScrollingExpandsWhenScrolledToEdge`: https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller/detent , https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller/largestundimmeddetentidentifier , https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller/prefersscrollingexpandswhenscrolledtoedge
- S27 HIG Context menus: https://developer.apple.com/design/human-interface-guidelines/context-menus
- S28 `UILongPressGestureRecognizer.minimumPressDuration` (0.5 s), `allowableMovement` (10 pt): https://developer.apple.com/documentation/uikit/uilongpressgesturerecognizer/minimumpressduration
- S29 HIG Scroll views (scroll edge effects): https://developer.apple.com/design/human-interface-guidelines/scroll-views
- S30 `scrollEdgeEffectStyle(_:for:)`: https://developer.apple.com/documentation/swiftui/view/scrolledgeeffectstyle(_:for:)
- S31 HIG Search fields: https://developer.apple.com/design/human-interface-guidelines/search-fields
- S32 HIG Playing haptics: https://developer.apple.com/design/human-interface-guidelines/playing-haptics
- S33 Symbols `SymbolEffect`, `ReplaceSymbolEffect`, `BounceSymbolEffect`: https://developer.apple.com/documentation/symbols/symboleffect
- S34 UIScrollView `decelerationRate`, `delaysContentTouches`, `isPagingEnabled`, `bounces`: https://developer.apple.com/documentation/uikit/uiscrollview/decelerationrate-swift.property , https://developer.apple.com/documentation/uikit/uiscrollview/delayscontenttouches ; 150 ms: https://wwdcnotes.com/documentation/wwdc14-235-advanced-scrollviews-and-touch-handling-techniques/
- S35 Keyboard notification duration 0.25 / curve 7: https://developer.apple.com/forums/thread/714339
- S36 iOS 26 rounded-corner push transition: https://developer.apple.com/forums/thread/792662 ; iOS 18 fluid parallax transitions (`_UIParallaxOverlayView`, property animators): https://developer.apple.com/forums/thread/796786
- Swipe actions default full swipe: https://developer.apple.com/documentation/uikit/uiswipeactionsconfiguration/performsfirstactionwithfullswipe , https://developer.apple.com/documentation/swiftui/view/swipeactions(edge:allowsfullswipe:content:)
- Batch updates: https://developer.apple.com/documentation/uikit/uicollectionview/performbatchupdates(_:completion:)

Third party:
- S26 Nil Coalescing, Presenting Liquid Glass sheets in SwiftUI on iOS 26: https://nilcoalescing.com/blog/PresentingLiquidGlassSheetsInSwiftUI
- S37 MacStories iOS 26 review (tab bar bubble, slider and switch glass): https://www.macstories.net/stories/ios-and-ipados-26-the-macstories-review/3
- R1 Rubber band formula c = 0.55: https://www.holko.pl/2014/07/06/inertia-bouncing-rubber-banding-uikit-dynamics/ , https://oleb.net/blog/2014/05/scrollview-inertia-bouncing-rubberbanding/
- F1–F12 Flutter Cupertino sources (values carry "eyeballed"/"measured" comments with the iOS version), https://github.com/flutter/flutter/tree/master/packages/flutter/lib/src :
  F1 `cupertino/route.dart`, F2 `cupertino/sheet.dart`, F3 `cupertino/context_menu.dart`, F4 `cupertino/dialog.dart`,
  F5 `cupertino/switch.dart`, F6 `cupertino/sliding_segmented_control.dart`, F7 `cupertino/picker.dart`,
  F8 `cupertino/refresh.dart`, F9 `cupertino/nav_bar.dart`, F10 `cupertino/button.dart`,
  F11 `widgets/scroll_physics.dart` + `widgets/scroll_simulation.dart`, F12 `animation/curves.dart`
- F13 Flutter engine iOS keyboard spring (mass 3, stiffness 1000, damping 500): https://github.com/flutter/flutter/blob/master/engine/src/flutter/third_party/spring_animation/SpringAnimationTest.mm

AndroidX (behaviour of the Compose side):
- C1 `FloatExponentialDecaySpec` (`ExponentialDecayFriction = -4.2f`): https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation-core/src/commonMain/kotlin/androidx/compose/animation/core/FloatDecayAnimationSpec.kt
- C2 `OverscrollEffect`, `LocalOverscrollFactory`: https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/Overscroll.kt
- C3 `Modifier.animateItem` defaults: https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/lazy/LazyItemScope.kt
- C4 Navigation 3 `NavDisplay` (`transitionSpec`, `popTransitionSpec`, `predictivePopTransitionSpec`): https://github.com/androidx/androidx/blob/androidx-main/navigation3/navigation3-ui/src/commonMain/kotlin/androidx/navigation3/ui/NavDisplay.kt
- C5 `SharedTransitionScope.ResizeMode` (default `ScaleToBounds`; `RemeasureToBounds` per-frame remeasure caveat): https://github.com/androidx/androidx/blob/androidx-main/compose/animation/animation/src/commonMain/kotlin/androidx/compose/animation/SharedTransitionScope.kt
- C6 `clickable` press delay `TapIndicationDelay` = tap timeout inside scrollables: https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation/src/androidMain/kotlin/androidx/compose/foundation/Clickable.android.kt
