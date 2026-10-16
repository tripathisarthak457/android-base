# Bottom sheets: research for the generator's sheet component (2026-10-12)

Request: "add a highly customisable and modern designed bottom sheet component in the generator".
This file collects implementable facts only: constants, API semantics and code-level behaviour, each with
its source. It does not design the component. §5 maps the findings onto the existing WP03 sheet and lists the
gaps.

## How to read the citations

- `src/…` is a pinned local copy under `research/sheet/sources/`, so the line numbers stay valid:
  - **androidx** `androidx-main` @ `823e6aba` (2026-10-12): `ModalBottomSheet.kt`, `ModalBottomSheet.android.kt`,
    `BottomSheet.kt`, `SheetDefaults.kt`, `BottomSheetScaffold.kt`, `Scrim.kt`, `DraggableAnchors.kt` (the
    material3 internal one), `BackHandler.kt`, `MotionScheme.kt`, `MaterialTheme.kt`, the token files,
    `OverlayScene.kt`, `NavDisplay.kt`, `SceneState.kt` and `AnimatedOverlaySamples.kt`. Upstream:
    `https://github.com/androidx/androidx/blob/androidx-main/compose/material3/material3/src/...`.
  - **Compose foundation 1.12.1**, the stable version in BOM 2026.09, unpacked from
    `https://dl.google.com/android/maven2/androidx/compose/foundation/foundation-android/1.12.1/foundation-android-1.12.1-sources.jar`:
    `src/foundation-1.12.1/commonMain/androidx/compose/foundation/gestures/AnchoredDraggable.kt` and
    `.../foundation/Overscroll.kt`. Apart from explicit-API `public` modifiers and one `animateTo` line, it
    matches `androidx-main`.
  - **MDC-Android** `master` @ `60ff0943` (2026-09-14): `src/mdc/*`.
  - **Flutter** `master` @ `87d56361` (2026-10-12): `src/flutter/sheet.dart`. This is Flutter's reimplementation of
    the iOS 18 sheet. Its constants carry "eyeballed" or "measured on iPhone 16 Pro, iOS 18" comments.
  - **nav3-recipes** `main` (the bottom sheet recipe was last changed by `e7e64987`, 2026-10-09):
    `src/nav3-recipes-BottomSheetSceneStrategy.kt`.
- Project code is cited by its worktree path. The worktrees are read, never edited.
- Labels:
  - **[A]** Apple primary source.
  - **[G]** Google primary source (code, docs or spec).
  - **[M]** measured by a third party (the Flutter constants).
  - **[E]** measured earlier in this project.
  - **[D]** derived here by arithmetic or reasoning.
  - **[U]** unverified: measure it before relying on it.

---

## 0. The findings that change the design

1. **Google itself now draws the sheet in-window.** In 2026, androidx split out a `BottomSheet` composable that
   "renders directly in the composition hierarchy ... unlike ModalBottomSheet which launches a separate Dialog
   window" (`src/BottomSheet.kt:76-93`; commit `584c2f38`, bugs 455862212, 350779977, 444176963, 336349080).
   `Scrim` became a public component that draws a rect with a lambda alpha (`src/Scrim.kt:50-79`). For us an
   in-window sheet is required, not just preferred: the Kyant/`SceneBackdrop` glass cannot sample from another
   window ("A surface in another window (a dialog, a popup) has no scene behind it to sample",
   `wt/WP05/.../foundation/glass/SceneBackdrop.kt:105-113`). The iOS card stack also has to transform the
   presenter, which lives in the same window.
2. **iOS 26 geometry** [A]:
   - "partial height sheets are inset by default with a Liquid Glass background. At smaller heights, the bottom
     edges pull in, nesting in the curved edges of the display. When transitioning to a full height sheet, the
     glass background gradually transitions, becoming opaque and anchoring to the edge of the screen"
     (WWDC25 323).
   - Sheets have "an increased corner radius, and half sheets are inset from the edge of the display" (Adopting
     Liquid Glass).
   - Apple publishes no point values for the inset or the radius [U]. The corners follow the concentric rule
     (`ConcentricRectangle`).
3. **iOS 27 (WWDC26) added three APIs** [A]:
   - sheet placement: `presentationPlacement(.leading/.trailing/.center/.automatic)` and
     `UISheetPresentationController.preferredPlacement`;
   - `dismissalConfirmationDialog(_:shouldPresent:actions:)`;
   - a `.crossFade` sheet transition.

   Placement is how a sheet adapts to a large window: a Maps-style panel on the leading edge.
4. **The iOS 18 sheet motion is measured** [M]:
   - The sheet's top rests at 8 % of H.
   - The presenter scales to 0.9165 about its top centre and moves down 7 % of H. Its corners go to 12 pt, with a
     10 % overlay (black in light mode, `#C8C8C8` in dark).
   - Dismiss at ≥ 2 H/s, or when the sheet is past 52 % of its travel.
   - An upward pull stretches the top gap from 0.08 H to 0.072 H and snaps back in 180 ms easeOut.
   - A stacked sheet sits 2 % higher, and the sheet under it moves up 0.5 % and scales to 0.9165.

   Source: `src/flutter/sheet.dart:14-90, 1100-1121`.
5. **Material constants** [G]:
   - drag handle 32 × 4 dp with 22 dp vertical padding; the handle's touch area is the top 48 dp;
   - top corners: `CornerExtraLargeTop` (28 dp);
   - max width 640 dp. Above 640 dp of window width there is a 56 dp margin at the top and 56 dp at each side;
     the top margin is 72 dp in compact windows;
   - scrim 0.32;
   - predictive back scales the sheet by up to 48 dp in x and 24 dp in y, pivoting at the bottom, with the
     `(0.1, 0.1, 0, 1)` progress easing; the content is counter-scaled to keep its aspect ratio
     (`src/BottomSheet.kt:443-487`, `src/BackHandler.kt:47-51`).
6. **M3's own sheet code shows the perf rules**:
   - The offset is applied in placement (`src/DraggableAnchors.kt:161-193`).
   - Insets derived from the offset were moved out of composition "to reduce recomposition on drag" (commit
     `ff66ec4e`, `src/ModalBottomSheet.kt:254-275`).
   - Subcomposition was removed for performance (commit `f7fe1d92`).
   - The scrim is a drawn rect with a lambda alpha.

   But M3 still reads `predictiveBackProgress.value` in composition, which recomposes on every back frame
   (`src/BottomSheet.kt:168`). Do not copy that.
7. **The current WP03 `AppSheet` is solid on motion, thin on customisation and accessibility.** See §5:
   - Detents are a two-value enum. There are no custom or fit-content detents, no background interaction, no
     content-interaction mode, no style slots and no dismiss guard.
   - The grabber has no semantics, and the scrim tap has no semantics.
   - Predictive back has no easing and no content counter-scale.
   - The IME does not lift the medium detent or the footer.
   - The detent is not restored after process death.

---

## 1. iOS sheets (iOS 16–27, Liquid Glass)

### 1.1 Appearance in iOS 26

| Fact | Source |
|---|---|
| Partial-height sheets are inset by default, with a Liquid Glass background. At smaller heights the bottom edges pull in, nesting in the curved display edges. Moving to full height, the glass "gradually transitions, becoming opaque and anchoring to the edge of the screen". | [A] WWDC25 323 "Build a SwiftUI app with the new design", https://developer.apple.com/videos/play/wwdc2025/323/ |
| "If you've used the presentationBackground modifier to apply a custom background to your sheets, consider removing that and let the new material shine." | [A] same |
| "Sheets have an updated design in iOS 26. They adapt their appearance from smaller to larger heights. To take advantage of their new glass appearance, remove any custom backgrounds." | [A] WWDC25 284 "Build a UIKit app with the new design", https://developer.apple.com/videos/play/wwdc2025/284/ (13:31) |
| "Sheets feature an increased corner radius, and half sheets are inset from the edge of the display to allow content to peek through from beneath them. When a half sheet expands to full height, it transitions to a more opaque appearance." Check content near the rounder corners, and what peeks through the gap. | [A] Adopting Liquid Glass, https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass |
| A corner is concentric to its container when the two share a common centre. Sheets and popovers do this automatically. A corner far from the container corner resolves to radius 0, and `concentric(minimum:)` floors it. Example from the Notes format sheet: concentric bottom corners, fixed-radius top corners. | [A] `ConcentricRectangle`, https://developer.apple.com/documentation/swiftui/concentricrectangle |
| Form sheet (iPad, regular × regular): "the system adds a layer with a Liquid Glass effect over the background content and centers the view controller's content on top of this layer"; the content is smaller than a page sheet. In compact height it behaves like full screen. | [A] https://developer.apple.com/documentation/uikit/uimodalpresentationstyle/formsheet |
| Third-party claims of "34 pt corners, detents 25/50/100 %" come from agent skill registries, not Apple. | [U] reject |

What to implement from this [D]:
- The inset and the radius lerp with detent progress. With inset `i` and display corner radius `R`, the bottom
  corner radius is `max(R − i, min)`, by the concentric rule.
- The top corners keep a fixed radius.
- At the large detent, inset = 0 and the bottom corners are 0. The surface's opacity goes from glass to opaque
  over the same progress.
- `R` is available on Android from API 31 through `WindowInsets.getRoundedCorner(...)`. MDC uses it at
  `src/mdc/BottomSheetBehavior.java:1639-1673`.

### 1.2 SwiftUI presentation API (the customisation surface Apple ships)

| API | Since | Semantics |
|---|---|---|
| `presentationDetents(_:)`, `presentationDetents(_:selection:)` | iOS 16 | "By default, sheets support the large detent." More than one detent makes the sheet resizable. `selection` must be one of the detents. |
| `PresentationDetent.medium` | iOS 16 | "approximately half the height of the screen, and is inactive in compact height". |
| `.large` | iOS 16 | Full height. |
| `.fraction(CGFloat)`, `.height(CGFloat)` | iOS 16 | Custom detents. |
| `.custom(CustomPresentationDetent.Type)` | iOS 16 | `static func height(in: Context) -> CGFloat?`. `Context.maxDetentValue` is "the height that the presentation appears in". Apple's sample: `max(44, context.maxDetentValue * 0.1)`. |
| `presentationBackgroundInteraction(.automatic / .disabled / .enabled / .enabled(upThrough:))` | iOS 16.4 | "At detents larger than the one you specify, SwiftUI disables interaction" with the view behind. Example: `.presentationDetents([.height(120), .medium, .large])` with `.enabled(upThrough: .height(120))`. |
| `presentationContentInteraction(.automatic / .resizes / .scrolls)` | iOS 16.4 | By default, a swipe up on a scroll view "grows to the next detent... A scroll view embedded in the presentation only scrolls after the presentation reaches its largest size". `.scrolls` scrolls first. "People can always resize your presentation using the drag indicator." |
| `presentationCornerRadius(CGFloat?)` | iOS 16.4 | `nil` means the system default. |
| `presentationBackground(_ ShapeStyle)`, `presentationBackground(alignment:content:)` | iOS 16.4 | Fills the whole presentation, and translucent styles show what is behind. Opaque on macOS. |
| `presentationDragIndicator(Visibility)` | iOS 16 | Show it "when it isn't apparent that a sheet can resize or when the sheet can't dismiss interactively". |
| `presentationCompactAdaptation(_:)` and `(horizontal:vertical:)` | iOS 16.4 | A sheet over a vertically compact view becomes a full-screen cover by default. `.none` opts out. |
| `presentationSizing(.automatic / .form / .page / .fitted)`, `.fitted(horizontal:vertical:)`, `.sticky(...)` | iOS 18 | `.automatic` resolves to `.form` on iOS. `.form` is "slightly less wide than `.page`". `.fitted` follows the content's ideal size. |
| `interactiveDismissDisabled(_:)` | iOS 15 | Blocks non-programmatic dismissal only. |
| `navigationTransition(.zoom(sourceID:in:))` + `matchedTransitionSource(id:in:)` | iOS 18 | Also works for sheets (§1.6). |
| **`presentationPlacement(.automatic / .center / .leading / .trailing)`** | **iOS 27** | "Use this modifier to place it on the leading or trailing edge... to maximize the visibility of the primary content behind a presented sheet." Sample: a `Map()` with a `[.medium, .large]` sheet at `.leading`. "Only sheet presentations respect this placement." |
| **`navigationTransition(.crossFade)`** | **iOS 27** | "Specify this transition in a sheet to have it appear by fading in over the content, as opposed to moving upwards". |
| **`dismissalConfirmationDialog(_:shouldPresent:actions:[message:])`** | **iOS 27** | On iOS it is shown only for a user-initiated dismissal. It has a default cancel action, which always prevents the dismissal, and a default close action; a `.destructive` button replaces the close action. |

Sources [A]:
- https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:)
- https://developer.apple.com/documentation/swiftui/presentationdetent
- https://developer.apple.com/documentation/swiftui/custompresentationdetent
- https://developer.apple.com/documentation/swiftui/view/presentationbackgroundinteraction(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationcontentinteraction(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationcornerradius(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationbackground(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationdragindicator(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationcompactadaptation(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationsizing(_:)
- https://developer.apple.com/documentation/swiftui/view/interactivedismissdisabled(_:)
- https://developer.apple.com/documentation/swiftui/view/presentationplacement(_:)
- https://developer.apple.com/documentation/swiftui/navigationtransition/crossfade
- https://developer.apple.com/documentation/swiftui/view/dismissalconfirmationdialog(_:shouldpresent:actions:)
- Topic index: https://developer.apple.com/documentation/swiftui/modal-presentations

### 1.3 UIKit `UISheetPresentationController`

| Property / API | Since | Semantics |
|---|---|---|
| `detents` | 15 | Defaults to `[.large()]`, must not be empty, ordered smallest to largest. |
| `selectedDetentIdentifier` | 15 | The most recent detent, set by the user or by code. `nil` means the smallest detent. |
| `Detent.custom(identifier:resolver:)` | 16 | The resolver returns a height "within the safe area of the sheet", plus `safeAreaInsets.bottom` when edge-attached. **Return `nil` to make the detent inactive** in that context. Do not set sheet properties inside the resolver. |
| `ResolutionContext.maximumDetentValue`, `.containerTraitCollection` | 16 | Inputs to the resolver. |
| `Detent.resolvedValue(in:)` | 16 | Builds a detent from the system ones (e.g. `medium` + 40). |
| `invalidateDetents()` | 16 | Call it when an external input to a custom detent changes. Call it inside `animateChanges` to animate the change. |
| `animateChanges(_:)` | 15 | Animates property changes. "Layout finishes for the sheet, all adjacent sheets in the sheet stack". WWDC21: "with a standard animation curve and animate other sheets in the stack as well, such as the root sheet scaling back up". |
| `largestUndimmedDetentIdentifier` | 15 | `nil` means a non-interactive dimming view at every detent. When set, dimming applies only above that detent, "and the undimmed area around the sheet responds to user interaction, allowing for a nonmodal experience". WWDC21: "dimming still fades in if I resize to the large detent". |
| `prefersScrollingExpandsWhenScrolledToEdge` | 15 | Default `true`: scrolling up first grows the sheet to the next detent, then scrolls. `false` is advised for a non-modal sheet. |
| `prefersEdgeAttachedInCompactHeight` | 15 | Default `false`, which gives a full-screen sheet in compact height. `true` attaches only the bottom edge. |
| `widthFollowsPreferredContentSizeWhenEdgeAttached` | 15 | Default `false`, so the width is the container's safe-area width. It has no effect in compact-width × regular-height. |
| `prefersGrabberVisible` | 15 | Default `false`. The system hides it "when the sheet is full screen in a compact-height size class or when another sheet presents on top of it". |
| `preferredCornerRadius` | 15 | Effective only for the front sheet of a stack. WWDC21: "the system will keep stacked corners looking consistent". |
| `prefersPageSizing` | 17 | Default `true` (page sheet, readable width). `false` gives form-sheet sizing from `preferredContentSize`. |
| `sourceView` | 15 | Floating sheets centre over it, within system margins. |
| `Detent.backgroundEffect: UIVisualEffect?` | **26.1** | Per-detent background effect. The docs have no abstract [U: per-detent glass/opaque control]. |
| `preferredPlacement: Placement` (`.automatic/.center/.leading/.trailing`) | **27** | The UIKit twin of `presentationPlacement`. The docs have no abstract. |
| `sheetPresentationControllerDidChangeSelectedDetentIdentifier(_:)` | 15 | Called after a **user** detent change only, "not after you change selectedDetentIdentifier programmatically". |

Sources [A]:
- https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller and each property's page (same
  base URL plus `/detents`, `/largestundimmeddetentidentifier`, `/prefersscrollingexpandswhenscrolledtoedge`,
  `/prefersedgeattachedincompactheight`, `/widthfollowspreferredcontentsizewhenedgeattached`,
  `/prefersgrabbervisible`, `/preferredcornerradius-3mb5`, `/preferspagesizing`, `/invalidatedetents()`,
  `/animatechanges(_:)`, `/preferredplacement`, `/detent/custom(identifier:resolver:)`, `/detent/backgroundeffect`)
- WWDC21 10063 "Customize and resize sheets in UIKit": https://developer.apple.com/videos/play/wwdc2021/10063/
  The session calls the dimming property `smallestUndimmedDetentIdentifier`; it shipped as
  `largestUndimmedDetentIdentifier`.

### 1.4 Dismiss guard and the dismiss-attempt pattern

- iOS 13+:
  - `isModalInPresentation = true` "will put the Sheet in a modal state where it cannot be dismissed, and you'll
    get the rubber-banding effect".
  - `presentationControllerDidAttemptToDismiss(_:)`: UIKit "will call this method when the user pulls down while
    in a modal state, at which point you can present the action sheet".
  - `presentationControllerShouldDismiss(_:)` vetoes a dismissal and must return quickly.
  - `DidDismiss` is "only called once if the user actually pulls the Sheet down and completes the transition".
  - "the presenting view controller's view is not removed from the view hierarchy", so `viewWillDisappear` is not
    called.

  Source: [A] WWDC19 224, https://developer.apple.com/videos/play/wwdc2019/224/ , and
  https://developer.apple.com/documentation/uikit/uiadaptivepresentationcontrollerdelegate/presentationcontrollerdidattempttodismiss(_:)
- HIG: "If people have unsaved changes in the sheet when they begin swiping to dismiss it, use an action sheet to
  let them confirm" ([A] https://developer.apple.com/design/human-interface-guidelines/sheets).
- SwiftUI before iOS 27: `interactiveDismissDisabled(hasChanges)`, plus a UIKit delegate bridge for the attempt.
  iOS 27 adds `dismissalConfirmationDialog` (§1.2).
- To implement [D]:
  - `dismissible: Boolean`, and `onDismissAttempt: (() -> Unit)?`.
  - While dismissal is blocked, a pull down rubber-bands (c 0.55 over the window height) and springs back.
  - The attempt fires once per gesture, on release or when the gesture passes the dismiss threshold [U: which of
    the two iOS uses].
  - Back (Android) and a scrim tap count as attempts too.

### 1.5 Stacking

- HIG: "Display only one sheet at a time from the main interface... If something people do within a sheet results
  in another sheet appearing, close the first sheet before displaying the new one" [A].
- The system still stacks sheets that are presented from a sheet:
  - the covered sheet moves up 0.5 % of H and scales to 0.9165 (`src/flutter/sheet.dart:57-62`, `:84-90`);
  - the new sheet ends 2 % of H higher than a first sheet would (`:48-55`) [M];
  - corners stay consistent across the stack (WWDC21) [A].
- The grabber hides on a covered sheet (`prefersGrabberVisible` doc) [A].

### 1.6 Zoom from a source view

- iOS 26 sheets morph out of the button that presents them [A] WWDC25 323 (7:10):
  ```swift
  Button { isPresented = true } label: { Image(systemName: "map") }
      .matchedTransitionSource(id: "transition-id", in: namespace)
  // …
  .sheet(isPresented: $isPresented) {
      SheetContent().navigationTransition(.zoom(sourceID: "transition-id", in: namespace))
  }
  ```
- UIKit: `viewController.preferredTransition = .zoom { _ in folderBarButtonItem }` [A] WWDC25 284 (14:07).
- WWDC24 10145 [A]:
  - "these same APIs work with the fullScreenCover and sheet presentation APIs";
  - the transition is "continuously interactive... from the beginning or during the transition";
  - an interrupted push "completes immediately... and then in the same turn of the run loop, the pop transition
    starts";
  - when a gesture ends, the final spring takes over "the velocity from the interactiveSprings".
- Action sheets in iOS 26 are anchored to their source on iPhone too, and have no cancel button when inline
  (WWDC25 284, 14:46) [A].
- To implement [D]: a `source` slot, the bounds of a keyed element. Presentation lerps the sheet rect and corner
  from the source rect on the `sheet` spring. The glass surface draws the morph (L1 + D1); content fades in after
  about 40 % [U, as in MOTION-PLAN M17].

### 1.7 Apple Maps-style persistent sheet

- Recipe [A]:
  - a small detent such as `.height(120)` or a custom resolver, plus `.medium` and `.large`;
  - `presentationBackgroundInteraction(.enabled(upThrough: .medium))`, or UIKit
    `largestUndimmedDetentIdentifier = .medium`;
  - `interactiveDismissDisabled()`;
  - `prefersScrollingExpandsWhenScrolledToEdge = false` for content that should scroll rather than grow;
  - on iOS 27 large windows, `presentationPlacement(.leading)`.

  Sources: §1.2 and §1.3, and the `presentationPlacement` sample, which is literally a `Map`.
- HIG: "In iOS and iPadOS, a sheet can be either modal or nonmodal... Notes on iPhone and iPad uses a nonmodal
  sheet to let people format various text selections" [A].
- iOS 26 Maps' floating glass card at the peek detent is an observation only [U].

### 1.8 Measured motion (iOS 18 unless noted)

| Quantity | Value | Source |
|---|---|---|
| Large-detent top gap | **0.08 H** | [M] `sheet.dart:27-30` |
| Upward over-pull at the top detent | top gap stretches to **0.072 H**, a range of 0.008 H. On release it animates back in **180 ms easeOut** | [M] `sheet.dart:32-37, 488-492, 1063-1090` |
| Present / dismiss | `transitionDuration` 500 ms, `fastEaseInToSlowEaseOut`. The project's matching spring is critically damped, **ω₀ 18.26 (k ≈ 333), response 0.344 s** | [M] `sheet.dart:815, 515-519`; [E] `research/ios-motion-catalogue.md:93` |
| Project tokens | `sheet` = response 0.34 s, bounce 0. `dialog` = 0.275 s (k 522). Rubber band c = **0.55** | [E] `wt/WP03/tokens/motion.json:15-25` |
| Dismiss by velocity | **≥ 2.0 screen heights/s** downward | [M] `sheet.dart:76-78, 1110-1113` |
| Dismiss by position | once **≥ 0.48** of the drag range has been dragged, judged at the release position (the controller is 1 when shown, falls as the sheet is dragged, and the sheet stays while it is above 0.52). Corrected 2026-10-13 (SHEET-SPEC revision 2, C1-24): this row first read "past 0.52" | [M] `sheet.dart:1071`, `:1110-1119` |
| Released mid-drag (settle) | **300 ms easeOut** | [M] `sheet.dart:80-82, 1102-1140` |
| Presenter (card stack) | scale `1 − 0.0835 = 0.9165` about its top centre; translate **0.07 H** down; corners lerp from `0.9 × topSafeInset` (only if that is > 20 pt) to **12 pt**; overlay **0 → 0.10** alpha, black in light mode and `#C8C8C8` in dark | [M] `sheet.dart:14-25, 64-74, 84-90, 327-345` |
| Status bar over the card stack | light content (`statusBarIconBrightness: light`) | [M] `sheet.dart:362-367` |
| Stacked sheet | covered sheet `−0.005 H`, ×0.9165; new sheet ends at `−0.02 H` | [M] `sheet.dart:48-62` |
| Grabber | **36 × 5 pt**, 5 pt from the top, fully rounded, colour `tertiaryLabel`; content starts 15 pt below the top | [M] `sheet.dart:704-724` ("derived from Apple's Figma files and ... iOS 18.2") |
| Corner radius at the front, iOS 13–15 | 10 pt; later larger | [U] (community; already noted in `ios-motion-catalogue.md:165`) |
| iOS 26: inset at small detents, corner radius, glass-to-opaque curve | not published | [U] measure from a screen recording |

### 1.9 Choosing a detent on release: projection versus thresholds

- WWDC18 803 "Designing Fluid Interfaces" [A] (https://developer.apple.com/videos/play/wwdc2018/803/):
  - "Don't just use position, use all of the information that's at your disposal".
  - The PiP example projects the release velocity with `UIScrollView.DecelerationRate` and snaps to the corner
    nearest the projected point.
  - The slide code, widely reproduced but not in the transcript [U wording]:
    `project(v, rate) = (v / 1000) * rate / (1 − rate)`. That gives 0.499 s·v at `.normal` (0.998) and
    0.099 s·v at `.fast` (0.99).
- WP03 projects with 0.099 s (`wt/WP03/.../overlay/AppSheetState.kt:184-188, 216-219`). [D] It cannot reproduce
  the iOS dismiss rule:
  - From the large detent (top at 0.08 H, hidden at 1.0 H), the midpoint is 0.54 H.
  - A release with no prior travel needs v ≥ 0.46 H / 0.099 s ≈ **4.6 H/s** to project past it.
  - iOS dismisses at **2 H/s** (§1.8).
- Recommended rule [D], to verify against a recording [U]:
  1. If |v| ≥ 2 H/s, go one stop in the direction of the fling. Hidden counts as a stop when dismissal is allowed.
     This mirrors foundation's velocity path (`computeTarget`, `AnchoredDraggable.kt:1483-1514`, with a 2 H/s
     threshold instead of 125 dp/s).
  2. Otherwise go to the stop nearest `offset + v · 0.099 s`. Between the lowest detent and hidden, use the 0.52
     position rule.
  - iOS behaviour for a fast flick from large past medium (skip medium, or stop at it) is not documented [U].

### 1.10 Large windows, compact height

- iPad: prefer page or form sheets ([A] HIG).
  - `.page` is "roughly the size of a page"; `.form` is smaller. Both are centred over dimming (or Liquid Glass
    for form sheets in iOS 26).
  - In compact height both behave as full screen.
  - Source: [A] the `formSheet` and `pageSheet` docs, and `presentationSizing`.
- `medium` is inactive in compact height [A]. Detents then reduce to the large detent.
- iOS 27: `presentationPlacement(.leading/.trailing)` for side-placed sheets [A].

---

## 2. Android

### 2.1 Material 3 spec constants

| Item | Value | Source |
|---|---|---|
| Variants | Modal (with scrim) and standard (no scrim). "Besides this, both variants ... have the same specs." | [G] m3.material.io/components/bottom-sheets/specs (read 2026-10-12) |
| Width | "Bottom sheets span the full window width up to 640dp" | [G] spec |
| Window wider than 640 dp | "bottom sheets adjust to have a top margin of 56dp and side margins of 56dp" | [G] spec |
| Top margin (compact) | **72 dp** | [G] spec |
| Drag handle | centred; **padding top and bottom 22 dp** | [G] spec; `src/SheetDefaults.kt:808-809` |
| Drag handle size | **32 × 4 dp**, colour `OnSurfaceVariant`, shape `extraLarge` | [G] `src/SheetBottomTokens.kt:30-37`, `src/SheetDefaults.kt:590-609` |
| Container | `SurfaceContainerLow`. Shape `CornerExtraLargeTop` (28 dp top corners); the hidden shape is `CornerNone`. Elevation Level1 (1 dp) | [G] `src/SheetBottomTokens.kt:24-46`; MDC docs `src/mdc/BottomSheet.md:46-52` |
| Scrim | `Scrim` colour at **0.32** | [G] `src/ScrimTokens.kt:21-26` |
| Peek height (standard sheet) | 56 dp | [G] `src/SheetDefaults.kt:553` |
| Max width constant | `SheetMaxWidth = 640.dp`; `Dp.Unspecified` means full width | [G] `src/SheetDefaults.kt:556`; `src/mdc/dimens.xml:23` |
| Initial position of a modal sheet | "capped at 50% of the screen height" | [G] m3 guidelines |
| Touch target | "The top 48dp portion of the bottom sheet is interactive when user-initiated resizing is available and the drag handle is present." MDC: `BottomSheetDragHandleView` minimum 48 × 48 dp | [G] m3 accessibility; `src/mdc/BottomSheet.md:529-531` |
| Side sheet, for expanded windows | docked width **256 dp**; detached margin **16 dp**; shape `CornerLarge` (16 dp); modal colour `SurfaceContainerLow` | [G] MDC `sidesheet/res/values/dimens.xml`, `theme/res/values/tokens.xml:24-33` |

Spec gap [G]: M3 Compose does not implement the 72 dp compact top margin. The expanded anchor is
`fullHeight − sheetHeight` (`src/BottomSheet.kt:305-307`), and the content consumes the safe-drawing top inset
instead (`src/SheetDefaults.kt:573-575`).

### 2.2 Material behaviour guidance ([G] m3.material.io bottom sheets guidelines)

- Modal sheets are dismissed by:
  - tapping an item;
  - tapping the scrim;
  - swiping down;
  - a close affordance in the sheet's app bar. "Display a close affordance in a full-screen modal bottom sheet."
- "Modal bottom sheets are used in mobile apps only." On medium and expanded widths they have a default max width
  that "can be overridden". On expanded windows "a bottom sheet can be swapped for a side sheet".
- "Selecting the drag handle should toggle through preset heights or close the sheet, while selecting the scrim
  should always close the bottom sheet." "If the bottom sheet has multiple preset heights but can't use a drag
  handle, Material requires the inclusion of a single-pointer alternative".
- Predictive back: "Bottom sheet detaches from the left and right edges of the screen to signal it will close;
  previous screen is revealed in a preview".
- Standard sheet at full height: "contain a collapse icon in an app bar to return to their initial position".

### 2.3 Accessibility (Material, MDC and Compose)

**Material spec** [G]:
- Users should be able to "resize bottom sheets without having to rely on touch gestures".
- Keyboard: Tab lands on the drag handle, and Space or Enter "toggles between available heights".
- "Label only the drag handle. The accessibility role for the drag handle is 'button'."

**Compose M3** [G], `src/BottomSheet.kt`:
- The sheet has `paneTitle = "Bottom Sheet"` and `traversalIndex = 0f` (`:334-337`).
- The drag handle is `clickable`:
  - Expanded → dismiss;
  - PartiallyExpanded → expand;
  - otherwise → show (`:365-371`).
- The handle merges semantics with custom actions (`:372-397`):
  - `dismiss("Dismiss bottom sheet")`;
  - `expand("Expand bottom sheet")` when partially expanded;
  - `collapse("Collapse bottom sheet")` when expanded and a partial detent exists.
- The handle has a tooltip (`DragHandleWithTooltip`, `src/SheetDefaults.kt:612-628`).
- Strings: `compose/material3/material3/src/androidMain/res/values/strings.xml` (`m3c_bottom_sheet_*`).
- The scrim is `onClick` plus `contentDescription = "Close sheet"`, merged, with `traversalIndex = 1f`
  (`src/Scrim.kt:58-71`, `src/ModalBottomSheet.kt:152-157`).
- The dialog root is `isTraversalGroup` (`src/ModalBottomSheet.kt:141`) and has `dialog()` semantics
  (`src/ModalBottomSheet.android.kt:254`).

**MDC Views** [G]:
- When expanded, siblings are set to `IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` and the sheet sends
  `TYPE_VIEW_FOCUSED` (`src/mdc/BottomSheetBehavior.java:2489-2534`).
- Actions per state: expand, half-expand, collapse, dismiss (`:2574-2630`).
- The handle prefixes its description with the state ("Collapsed. / Expanded. / Half expanded.")
  (`src/mdc/BottomSheetDragHandleView.java:176-206`).
- The handle's click action label names the next state (`:274-299`).
- Tap cycles states; double tap hides (`src/mdc/BottomSheet.md:519-527`).

**Compose UI 1.12.1 primitives** [G], in `ui-android-1.12.1-sources.jar`:
- `SemanticsProperties.kt`: `paneTitle` (`:972`), `dialog()` (`:1149`), `expand/collapse/dismiss(label, action)`
  (`:1632-1660`), `customActions` (`:1325`), `liveRegion`.
- `hideFromAccessibility()` (`:1063-1075`): "if the node is currently occluded by a dark semitransparent pane
  above it ... this property can be set to make the screen reader linear navigation skip over this type of node".
  This is the in-window replacement for a Dialog's window isolation.
- Keyboard focus trapping: `FocusProperties.onExit { cancelFocusChange() }` (`focus/FocusProperties.kt:188,
  232-256`).

**Detent announcements**:
- `View.announceForAccessibility` is deprecated in API 36 ([G]
  https://developer.android.com/sdk/api_diff/36/changes/android.view.View).
- Announce through state instead: a `stateDescription` on the handle ("Expanded" / "Half expanded"), as MDC does,
  plus `paneTitle` for appearance.

**iOS** [A]: "a grabber also works with VoiceOver so people can resize the sheet without seeing the screen" (HIG).
Tapping it cycles detents.

### 2.4 androidx material3 implementation (androidx-main, 2026-10-12)

**Window choice**
- `ModalBottomSheet` wraps a `ModalBottomSheetDialog`, a fork of Compose `Dialog` and a `ComponentDialog` with
  `EdgeToEdgeFloatingDialogWindowTheme` (`src/ModalBottomSheet.android.kt:226-321`). Its window:
  - transparent background, `setDecorFitsSystemWindows(false)` (`:333-336`);
  - `MATCH_PARENT × MATCH_PARENT`, `SOFT_INPUT_ADJUST_NOTHING` on API 30+ and `ADJUST_RESIZE` below
    (`:411-422`);
  - status and navigation bar icon appearance from the content colour (`:423-430`);
  - an 8 dp elevation allocation, because of a pre-S accessibility bug, b/232788477 (`:325-327`);
  - `cancel()` is a no-op (`:446-449`);
  - a saveable-id tag so `rememberSaveable` inside survives (`:339-341`).
- History: popup → dialog in April 2024, "for system bars styling, setDecorFitsSystemWindows ... and unconsumed
  window insets" (commit `33c32f65`, bugs 274872542, 300280229, 290893168). Then an **in-tree `BottomSheet`**
  in February 2026 (§0.1).

**Structure** (`src/ModalBottomSheet.kt:96-188`):
- The dialog contains a `Box` holding the `Scrim` and the `BottomSheet`, top-centre aligned.
- The scrim alpha comes from `animateFloatAsState`. Its target is 1 when `targetValue != Hidden`, on
  `DefaultEffects` (`:143-151`). It **does not follow the finger**, unlike iOS dimming.
- The sheet consumes top insets equal to its offset through a `WindowInsets` object read lazily
  (`SheetWindowInsets`, `:254-275`). Commit `ff66ec4e`: "This reduces recomposition on drag by instead
  invalidating layout and draw phases".
- It shows on launch (`:175-187`).

**`BottomSheet`** (`src/BottomSheet.kt`):
- Motion (`:135-142`): show uses `defaultSpatialSpec`, hide uses **`fastEffectsSpec`**, and the drag settle uses
  `defaultSpatialSpec`. All come from `MaterialTheme.motionScheme`, which defaults to `standard()`
  (`src/MaterialTheme.kt:181`):

  | Spec | Standard (damping, stiffness) | Expressive (damping, stiffness) | Standard response (2π/√k) |
  |---|---|---|---|
  | Default spatial | 0.9, 700 | 0.8, 380 | 0.237 s |
  | Fast effects | 1.0, 3800 | 1.0, 3800 | 0.102 s |

  Sources: `src/StandardMotionTokens.kt:20-31`, `src/ExpressiveMotionTokens.kt:22-33`.
- Fling (`:211-260`):
  - foundation `AnchoredDraggableDefaults.flingBehavior`, with a positional threshold of **56 dp**
    (`src/SheetDefaults.kt:577`);
  - velocity clamped to the system maximum;
  - near Hidden, velocity is damped **quadratically** inside a **125 dp** zone (`BoundaryDampeningZone`,
    `src/SheetDefaults.kt:587`), floored at the 125 dp/s velocity threshold so a valid fling survives. Commit
    `e4862f87`: "smoother 'soft landing' ... preventing the jarring abrupt stop near the screen edge".
- Anchors (`:293-327`), computed in measure through `draggableAnchors`:
  - `Hidden at fullHeight`;
  - `PartiallyExpanded at fullHeight − min(H/2, sheetH/2)` under the deterministic flag, legacy `H/2`
    (`:416-441`);
  - `Expanded at max(0, fullHeight − sheetHeight)`;
  - on a size change the target is preserved: an Expanded sheet stays Expanded (commit `0a8673ff`, b/324934884).
- Offset (`src/DraggableAnchors.kt:132-193`):
  - anchors are recomputed in every main measure pass;
  - **the offset is applied in placement** (`placeable.place(x, offset)`) inside
    `withMotionFrameOfReferencePlacement`, which marks it as scroll-like motion so placement animations don't
    chase it.
- Gestures: `anchoredDraggable(enabled = gesturesEnabled && currentValue != Hidden)` (`:328-333`).
- Bounce gap: `verticalScaleUp` and `verticalScaleDown` stretch the surface and counter-scale the content when a
  bouncy spring pushes the offset above the top anchor (`src/SheetDefaults.kt:765-802`). The default sheet tween
  (300 ms `FastOutSlowIn`, `:805-806`) came from commit `8de6e21c`: "This resolves an issue with a visible
  overshoot".
- Predictive back (`:144-166`, `:443-487`):
  - `PredictiveBackHandler` snaps an `Animatable` to `PredictiveBack.transform(progress)`, which is
    `CubicBezierEasing(0.1, 0.1, 0, 1)` (`src/BackHandler.kt:47-51`).
  - Surface scale: `scaleX = 1 − lerp(0, min(48dp, w), p)/w` and `scaleY = 1 − lerp(0, min(24dp, h), p)/h`,
    with `transformOrigin = (0.5, (offset + height)/height)`, the window's bottom edge.
  - Content counter-scale: `scaleY = scaleX/scaleY` about `(0.5, 0)`, which keeps the content's aspect ratio.
  - Commit: from Expanded with a partial detent, go to `partialExpand()`; otherwise `hide()` then
    `onDismissRequest` (`:146-155`). This produced the "back pressed twice" bug for short sheets, fixed in
    `02b1dbca` (b/521534697).
  - Cancel: `animateTo(0f)` on the default spring.
  - **Perf flaw:** `predictiveBackProgress.value` is passed as a `Float` in composition (`:168`), so every back
    event recomposes `BottomSheetImpl`.
- `SheetState` (`src/SheetDefaults.kt`):
  - `currentValue` is the **settled** value (`:198-202`);
  - `expand/partialExpand/show/hide` check `confirmValueChange` (`:258-304`);
  - `animateTo` retargets through `anchoredDrag(targetValue)` and seeds the animation with `lastVelocity`
    (`:316-339`);
  - the `Saver` saves `currentValue` (`:397-432`);
  - `rememberBottomSheetState(initialValue, enabledValues, confirmValueChange)` uses `rememberSaveable`
    (`:703-762`);
  - state restoration was fixed in August 2026 to keep Expanded (commit `f22fad2c`, b/533440736).
- Nested scroll (`src/SheetDefaults.kt:630-689`):
  - pre-scroll: an upward `UserInput` delta goes to `dispatchRawDelta` (the sheet grows first);
  - post-scroll: the leftover in either direction goes to the sheet;
  - pre-fling: upward, if the sheet is below its top anchor, the sheet flings and consumes all of it;
  - post-fling: the rest flings the sheet.

  This is the `prefersScrollingExpandsWhenScrolledToEdge = true` behaviour. There is no "scrolls first" mode.
- Insets:
  - `modalWindowInsets = safeDrawing.only(Bottom + Top)` (`src/SheetDefaults.kt:572-575`), applied to the content
    with `windowInsetsPadding` (`src/BottomSheet.kt:350-353`);
  - the hard-coded `imePadding()` was removed in April 2026, so the IME goes only through `contentWindowInsets`
    (commit `024ff1d8`, b/289824811);
  - the back callback priority was lowered "to allow IME keyboard to dismiss first" (commit `b8dd8af9`,
    b/331977823).

**Pitfall history** [G]:

| Commit | Problem it fixed |
|---|---|
| `aaf898a6` (2023) | Huge padding on API 29–33 with a small TextField |
| `01cd3669` | Swiping down did not call `onDismissRequest` |
| `f7fe1d92` (2024) | `BoxWithConstraints` subcomposition: slow, and crashed with `LookaheadScope` |
| `76b721e3` | Scrim alpha outside 0..1 |
| `0a1194b2` | Recreating the sheet on a configuration change was reverted |
| `5ab9549e` (2026) | Hard-coded physics "interpreted minor scroll velocities as aggressive sheet-dismissal swipes" |
| `4ccdc39b` | "Orphan anchors" ambiguous-target errors (flag `isAnchoredDraggableComponentsAnchorRecoveryEnabled`) |
| `943e6af1` | Three reverts of a `MaterialAnchoredDraggableState` refactor (b/498165876, 471021165, 477969920) |

### 2.5 MDC-Android (Views) constants that Compose does not expose

| Constant | Value | Source |
|---|---|---|
| Back progress easing | `PathInterpolator(0.1, 0.1, 0, 1)` | `src/mdc/MaterialBackAnimationHelper.java:46` |
| Back hide duration | `lerp(300, 150, progress)` ms (`motionDurationMedium2` to `Short3`); cancel **100 ms** (`Short2`); `FastOutSlowIn` | `:41-67`; `MaterialBottomContainerBackHelper.java:108-163` |
| Back commit (not persistent) | translate Y by the scaled height, then reset | `MaterialBottomContainerBackHelper.java:119-138` |
| Bottom container back scale | x 48 dp, y 24 dp, pivot Y = view height; children `scaleY = scaleX/scaleY` with pivot at `-child.top` | `src/mdc/motion_dimens.xml:23-24`; `MaterialBottomContainerBackHelper.java:73-106` |
| Side container back scale | shrink 24 dp, grow 12 dp, y 48 dp | `motion_dimens.xml:19-21` |
| Hide decision | `newTop = top + yvel · 0.1` (`HIDE_FRICTION`), hide if `abs(newTop − collapsed)/peek > 0.5` | `src/mdc/BottomSheetBehavior.java:228-230, 1784-1798` |
| Significant velocity | 500 px/s default | `:226` |
| Corner removal at full screen | expanded corners animate to the **device's rounded-corner radius** (API 31+, `insets.getRoundedCorner`) when the sheet touches the top, otherwise to 0; 500 ms | `:232, 1610-1673, 1706-1718` |
| Elevations | modal 16 dp (legacy design), M3 Level1 | `src/mdc/dimens.xml:19-26` |

### 2.6 Compose foundation 1.12.1 `AnchoredDraggable` (what to build on)

All references are in `src/foundation-1.12.1/commonMain/androidx/compose/foundation/gestures/AnchoredDraggable.kt`.

| Fact | Line |
|---|---|
| `Modifier.anchoredDraggable(state, reverseDirection, orientation, enabled, interactionSource, overscrollEffect, flingBehavior)`. "Dragging is constrained between the minimum and maximum anchors." | `:79-125` |
| Drag: `dragTo(state.newOffsetForDelta(delta))`. With an `overscrollEffect`, the delta first goes through `applyToScroll`, and the excess past the bounds goes to the effect (this is how to get a rubber band). | `:410-430` |
| Release: fling through the resolved `FlingBehavior`; with an effect, `applyToFling` gets the leftover velocity at a bound | `:433-477` |
| `startDragImmediately` = `isAnimationRunning`, so a touch catches a settling sheet mid-flight | `:479` |
| `newOffsetForDelta` clamps to `[minPosition, maxPosition]`: no rubber band by default | `:1234-1238` |
| `DraggableAnchors { Value at px }` builder | `:549-675` |
| `updateAnchors(newAnchors, newTarget)`: "If your anchors depend on the size of the layout, updateAnchors should be called in the layout (placement) phase". When an animation holds the lock it retargets instead of snapping. | `:1001-1029` |
| `offset` (a `mutableFloatState`); `requireOffset`; `progress(from, to)`; `progress` and `lastVelocity`. The frequently changing ones are annotated `@FrequentlyChangingValue`, so lint flags reads in composition. | `:916-990` |
| `targetValue` is `derivedStateOf { dragTarget ?: calculateTargetValue(offset) }`. Reading it in composition recomposes only when the target changes. | `:871` |
| `computeTarget`: at rest, the closest anchor; if `abs(v) ≥ velocityThreshold`, the next anchor in the direction of v; else the positional threshold decides | `:1483-1514` |
| Defaults: `SnapAnimationSpec = tween()`, `PositionalThreshold = distance/2`, `DecayAnimationSpec = exponentialDecay()` | `:1518-1529` |
| `AnchoredDraggableDefaults.flingBehavior(state, positionalThreshold, animationSpec)`: a `snapFlingBehavior` with **no decay** (`calculateApproachOffset = 0`) and a **125 dp/s** velocity threshold | `:1551-1566, 1714, 1744-1790` |
| `confirmValueChange` is checked in the snap target (`:1781`) but **deprecated**: "the anchor set should not include disallowed anchors". Thresholds and specs moved from the state to the fling behaviour ("ConfigurationMovedToModifier"). | `:1716-1735` |

Constraints this puts on the design [D]:
- For an iOS-like 2 H/s velocity rule, projection and a velocity-seeded spring settle, write a custom
  `FlingBehavior`, or drive `anchoredDrag { dragTo(...) }` directly. The default fling's threshold is 125 dp/s
  with no projection.
- The spring must be seeded with the release velocity: `animate(prev, target, velocity, spring)`, as M3 does at
  `src/SheetDefaults.kt:326-329`.
- To dismiss or veto, change the anchor set rather than use `confirmValueChange`. A non-dismissible sheet has no
  Hidden anchor plus an overscroll (rubber-band) effect at the bottom bound.

`OverscrollEffect` (`src/foundation-1.12.1/commonMain/androidx/compose/foundation/Overscroll.kt`):
- `applyToScroll` (`:86`), `applyToFling` (`:117`), `isInProgress` (`:124`);
- `val node: DelegatableNode` renders the effect (`:152`); `Modifier.overscroll(effect)` (`:276`);
- `withoutEventHandling()` (`:197`).

A custom effect can implement the 0.55 rubber band in layout or draw, with no recomposition.

Compose foundation release notes [G] (https://developer.android.com/jetpack/androidx/releases/compose-foundation):
- Stable 1.12.1 shipped 2026-09-09; 1.13.0-beta01 shipped 2026-10-07.
- 1.11.0-alpha05: `isDelayPressesUsingGestureConsumptionEnabled`. "containers with drag gesture will delay press.
  This includes a behavior change for containers based off `Modifier.draggable`." A sheet is such a container,
  so check that press feedback inside the sheet is not delayed [U].

### 2.7 Navigation 3: a sheet as a destination

- `OverlayScene<T> : Scene<T>` (`src/OverlayScene.kt:35-57`):
  - `overlaidEntries` "must always be a non-empty list";
  - **`suspend fun onRemove()`** "is invoked after this OverlayScene is popped from the backStack, but before it
    leaves composition. Animations for exiting overlays should be implemented within onRemove".
- `NavDisplay` keeps popped overlay scenes composed until `onRemove()` returns (`src/NavDisplay.kt:683-700,
  855-881`).
  - Overlays render **after the `AnimatedContent`, in the same composition** (`:855-873`).
  - Only the top overlay gets `RESUMED`; the others get `STARTED` (`:857-862`).
  - The underlying scene stays `RESUMED` only while no overlay is up (`:818`).
- Overlay scenes are not decorated by `SceneDecoratorStrategy` and are "animated separately from non-overlays"
  (`src/SceneState.kt:212-225`). NavDisplay's predictive pop does not animate them, so **the sheet must own its
  back-gesture animation** [D from the code].
- Official sample (`src/AnimatedOverlaySamples.kt:88-131`) and recipe (`src/nav3-recipes-BottomSheetSceneStrategy.kt`,
  updated 2026-10-09 to use `onRemove()`):
  - a `data class` scene keyed by `lastEntry.contentKey`;
  - `previousEntries = overlaidEntries = entries.dropLast(1)`;
  - metadata marks the entry (`bottomSheet()`);
  - `onRemove() { sheetState.hide() }`;
  - "This strategy should always be added before any non-overlay scene strategies".
  - Pitfall: `lateinit var sheetState` is assigned in composition, so `onRemove` before the first composition
    would throw [D].
- Project hook: WP04's `PresentationHost` takes a `sheetStrategy: SceneStrategy<AppNavKey>?`. It draws sheets
  above the tab bar, and "without one, a sheet key is presented as a cover"
  (`wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/presentation/PresentationHost.kt:54-77, 121`).

### 2.8 Google apps

There is no primary documentation for the sheet constants in Maps, Gmail or Photos.
- M3 names the pattern: "Standard bottom sheets can contain supplementary content that continues below the
  screen, such as location information over a map" [G guidelines].
- Everything else about these apps (Maps' peek, half and full stops, the map staying interactive under a
  collapsed sheet, the full-height sheet becoming an opaque page with a top bar, how predictive back collapses
  it) is **[U]**. Record it on the Redmi with `screenrecord` and step through the frames before copying any
  constant.

### 2.9 Large screens and foldables

- Material: the sheet is capped at 640 dp, with 56 dp margins above a 640 dp window. On expanded windows, swap
  to a side sheet (256 dp, detached margin 16 dp) (§2.1) [G].
- iOS: form or page sheet on iPad; `presentationPlacement(.leading)` on iOS 27 [A].
- Foldables [G] (https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware):
  - tabletop = `HALF_OPENED` + `HORIZONTAL`; book = `HALF_OPENED` + `VERTICAL`;
  - "Don't place UI controls too close to a fold or hinge when `isSeparating` is true";
  - use `occlusionType` to decide what may sit inside the fold's `bounds`.
  - WP03 already rests the medium detent on a tabletop fold and avoids the form card across the crease
    (`wt/WP03/.../overlay/AppSheet.kt:135-140, 233-237`).
- Project tokens: `sheetMaxWidth = 640.dp`; `formSheetWidth` is 540 dp at Medium and Expanded and 600 dp at
  Large and above (`wt/WP03/.../foundation/adaptive/AppLayout.kt:31-65`). The AD-04 rules are in
  `research/ADAPTIVE-PLAN.md:434`.

### 2.10 IME

- Edge-to-edge skill [G], `~/.claude/plugins/marketplaces/android-skills/system/edge-to-edge/SKILL.md:108-114`
  (last updated 2026-08-24):
  - use `adjustResize`;
  - "PREFERRED: Add `Modifier.fitInside(WindowInsetsRulers.Ime.current)` to the content container ... because it
    reduces jank and extra padding caused by forgetting to consume insets upstream";
  - otherwise `imePadding()` before `verticalScroll`;
  - never pad twice when `contentWindowInsets` already contains the IME.
- `fitInside` (foundation-layout 1.12.1 `RulerAlignment.kt:36-66`) reads the rulers inside the **placement
  block**. An IME animation frame therefore re-places and re-measures the child, but does not recompose.
- `WindowInsetsRulers.Ime` exposes `current`, plus `getAnimation(scope)` with `source`, `target`, `isVisible`
  and `isAnimating` (ui 1.12.1 `layout/WindowInsetsRulers.kt:40-69, 97, 226-255`). That is enough to animate a sheet
  in sync with the keyboard.
- M3: on API 30+ the window is `SOFT_INPUT_ADJUST_NOTHING`, so Compose insets handle the IME
  (`src/ModalBottomSheet.android.kt:416-422`). The IME is now driven only by `contentWindowInsets`
  (commit `024ff1d8`).
- Back with the IME open must close the IME first (commit `b8dd8af9`) [G]. Test it, because WP03's sheet handler
  registers at `PRIORITY_OVERLAY` (`wt/WP03/.../overlay/OverlayBackHandler.kt:47-49`) [U].
- iOS behaviour when a text field in a medium-detent sheet takes focus (grow to large, or lift by the keyboard
  height) is not documented [U].

### 2.11 Process death and state restoration

- M3: `rememberSaveable` with `SheetState.Saver`, which saves `currentValue` (`src/SheetDefaults.kt:397-432,
  739-762`). The dialog's own saveable registry is keyed by a saveable UUID
  (`src/ModalBottomSheet.android.kt:240, 339-341`).
- Nav3: a sheet that is a destination is restored with the back stack (`rememberNavBackStack` is saveable).
  `rememberSaveableStateHolderNavEntryDecorator` keeps the content's saveable state, and WP04 already installs it
  (`PresentationHost.kt:92-93`).
- Restore the **detent** as well. Saving the settled detent and restoring it without animation is what M3's
  August 2026 fix does (`f22fad2c`).

---

## 3. Performance: a sheet drag at 0 recompositions per frame

The frame budget at 120 Hz is 8.33 ms. The project's cost classes (L0/L1/D1/D2) are in
`research/MOTION-PLAN.md:66-75`.

| Concern | Rule | Evidence |
|---|---|---|
| Sheet offset | Read only in a placement block (`Layout` placement, `Modifier.offset { }`, or a `LayoutModifierNode`). Wrap it in `withMotionFrameOfReferencePlacement`. Never `Modifier.offset(dp)`, never `padding(top = offset)`. | [G] `src/DraggableAnchors.kt:161-193`; developer.android.com/develop/ui/compose/performance/bestpractices ("use the lambda versions of the modifiers") |
| Anchors | Compute them in measure from the constraints. Write them with an equality check so an unchanged frame does not invalidate. Avoid `BoxWithConstraints` and `SubcomposeLayout`. | [G] `src/DraggableAnchors.kt:132-158`; commit `f7fe1d92` |
| Content measure | Measure the content once, at the large-detent height, and only re-place it on drag. Never re-measure a full-screen subtree per frame. M3 measures wrap-content once, then offsets it. | [E] WP03 `AppSheet.kt:77-80, 244-248`; [G] `src/BottomSheet.kt:299-307` |
| Insets that depend on the offset | A `WindowInsets` implementation that reads the offset lazily, so insets resolve in layout. | [G] commit `ff66ec4e`, `src/ModalBottomSheet.kt:254-275` |
| Scrim | `drawRect(color, alpha = progress())` in a draw lambda (D2). **No** `graphicsLayer { alpha }` on a full-screen box, which needs an offscreen buffer. | [G] `src/Scrim.kt:75-77`; [E] SMOOTHNESS-PLAN S8 |
| Card stack (presenter) | One `graphicsLayer { scaleX; scaleY; transformOrigin = (0.5, 0); translationY; shape; clip }` on the presenter, reading the sheet fraction in the layer block (L0). Draw the 10 % overlay as a rect, not as alpha. | [E] `ios-motion-catalogue.md:165`, MOTION-PLAN M6 |
| Predictive back | Keep progress in a `Float` state read **only** in `graphicsLayer {}`. Do **not** pass `progress.value` to a composable (M3's flaw, `src/BottomSheet.kt:168`). | [G] code |
| Nested scroll | `onPreScroll`/`onPostScroll` call `dispatchRawDelta`, a plain state write that recomposes nothing. Fling hand-off happens in `onPreFling`/`onPostFling` with velocity. | [G] `src/SheetDefaults.kt:630-689` |
| Fast values | Annotate offset, progress and fraction getters with `@FrequentlyChangingValue`, as foundation does, so lint catches reads in composition. | [G] `AnchoredDraggable.kt:925, 945, 973`; [E] RECOMPOSITION-PLAN R7 |
| Derived discrete state | `currentDetent`, "scrim visible" and the a11y state come from `derivedStateOf`, so they recompose only when they change (≤ 1 per detent settle). | [G] `src/ModalBottomSheet.kt:143-145`; [E] RECOMPOSITION-PLAN:192 |
| Bouncy springs | If a spring may overshoot the top anchor, stretch the surface (`verticalScaleUp`) and counter-scale the content, rather than letting a gap show. | [G] `src/SheetDefaults.kt:765-802`; commit `8de6e21c` |
| Allocation | No lambdas or objects allocated per frame in modifiers; pass the state object instead. | [G] commit `20e62d73` ("Removed the lambdas and passing in the state to save a few allocations") |
| Glass | Do not use a live backdrop on a full-height sheet. WP05 uses non-sampling `glassSurface` for sheets ("a live backdrop under each of them would cost a blur pass per frame"). Stop drawing glass at the opaque large detent. | [E] `wt/WP05/.../foundation/glass/GlassSurface.kt:26-38` |
| Interruptibility | A touch during a settle must catch the sheet (`startDragImmediately = isAnimationRunning`). Retargeting must keep velocity (`lastVelocity`). | [G] `AnchoredDraggable.kt:479, 990`; `src/SheetDefaults.kt:316-339` |
| Frame rate | `Modifier.preferredFrameRate(FrameRateCategory.High)` exists (ui 1.12.1 `FrameRate.kt:52, 71`). It only matters on API 35+ ARR devices; whether the Redmi's ROM honours it is [U]. | [G] |

**ModalBottomSheet pitfalls (why we don't wrap it)**:

1. A separate window (`ComponentDialog`):
   - a second `ViewRootImpl` and surface;
   - its own composition (`setContent(composition)`) created inside `remember` on the first frame
     (`src/ModalBottomSheet.android.kt:241-266`);
   - the first-frame cost includes window add, a new layout pass and the dialog surface allocation (the size of
     this cost is [U]; measure the first-frame time with `perf.sh`);
   - no access to the presenter for the card stack, and no glass sampling (§0.1);
   - `DisposableEffect` calls `dialog.dismiss()` on dispose, so the window can vanish before an exit animation
     unless the caller waits for `hide()` (the documented `hide().invokeOnCompletion` pattern,
     https://developer.android.com/develop/ui/compose/components/bottom-sheets).
2. Insets bugs over the years: content under the navigation bar, landscape insets, the status-bar scrim, and huge
   padding on API 29–33 with a TextField (§2.4 table).
3. The scrim does not follow the drag (`src/ModalBottomSheet.kt:143-151`).
4. The default motion is a 300 ms tween, chosen to dodge overshoot (`src/SheetDefaults.kt:805-806`), not a
   velocity-preserving spring.
5. Back from Expanded goes to PartiallyExpanded first (`src/BottomSheet.kt:146-155`), the opposite of the Gmail
   and iOS expectation that one gesture closes the sheet.

---

## 4. What a "highly customisable" sheet must expose (derived from §1–§3)

This maps each platform concept to a parameter or behaviour. It is a checklist for whoever designs the API (WP03),
not a design.

| Concept | iOS name | M3 name | Needed behaviour |
|---|---|---|---|
| Detents | `.medium/.large/.fraction/.height/.custom(resolver)` with `maxDetentValue`; `nil` means inactive | `enabledValues` (Hidden, Partial, Expanded) | Any number of detents. Each is a fraction, a fixed height, the content's own height (fit), or a resolver from a context (window height, insets, size class) that can return "inactive". Inactive in compact height by default, as iOS `medium` is. |
| Selected detent | `selection` binding; the delegate reports user changes only | `currentValue` / `targetValue` | Observable, settable with animation, saveable. A callback on **user** changes. |
| Background interaction | `.enabled(upThrough:)`, `largestUndimmedDetentIdentifier` | standard vs modal sheet | Per detent: modal (scrim, blocks touches, hides the content beneath from a11y) or non-modal (no scrim, passes touches through). Dimming fades in across the boundary. |
| Content interaction | `.resizes` (default) / `.scrolls`; `prefersScrollingExpandsWhenScrolledToEdge` | resizes only | Both modes in the nested-scroll connection. |
| Dismissal | `interactiveDismissDisabled`, `isModalInPresentation`, did-attempt, `dismissalConfirmationDialog` | `confirmValueChange` (deprecated in foundation), `shouldDismissOnBackPress/ClickOutside` | `dismissible`, `onDismissAttempt`, with a rubber band when blocked. Separate switches for scrim tap, Back and swipe. |
| Grabber | `presentationDragIndicator`, `prefersGrabberVisible`; tap cycles detents | drag handle: 32 × 4, 22 dp padding, 48 dp target, tooltip, a11y actions | Visibility; a slot for a custom handle; tap and Enter cycle detents; semantics with expand, collapse and dismiss actions plus a state description. |
| Corners | `presentationCornerRadius`, concentric bottom corners in iOS 26 | `shape` (28 dp top) | Top radius token; bottom corners concentric when floating; a morph to edge-attached. |
| Background | `presentationBackground` (any style or view); iOS 26 glass by default; `Detent.backgroundEffect` | `containerColor`, `tonalElevation` | Surface, glass (non-sampling) or a custom painter; opacity lerp toward the large detent. |
| Floating vs attached | iOS 26 inset at partial detents; `prefersEdgeAttachedInCompactHeight` | always attached | Inset at partial detents, attached at large; the inset as a token. |
| Width and placement | `widthFollowsPreferredContentSize…`, `presentationSizing(.form/.page/.fitted)`, `presentationPlacement` (iOS 27) | `sheetMaxWidth` (640) | Max width; form-sheet mode on regular windows (WP03 has it); leading or trailing placement on wide windows; fitted height. |
| Compact height | full screen by default; `presentationCompactAdaptation` | — | A rule: full height, or a 640-wide card. |
| Stacking | automatic; corners consistent | — | A sheet presented from a sheet: the lower sheet recedes 0.9165 and moves −0.5 % H; the new one sits 2 % H higher. |
| Presenter recede | automatic card stack at large | none | Expose the large fraction (WP03 has `largeFraction`) for WP04's presenter transform. |
| Source morph | `navigationTransition(.zoom)`, `preferredTransition = .zoom` | none | An optional source-bounds slot. |
| Transition style | default slide, `.crossFade` (iOS 27) | — | Slide, or cross-fade (also the reduced-motion choice). |
| Footer | toolbars in the sheet | — | A pinned footer above the IME and gesture insets (WP03 has `footer`). |
| Back | n/a (swipe down) | predictive back: 48/24 dp, easing (0.1, 0.1, 0, 1), content counter-scale | Shrink with the easing; commit dismisses (or, configurably, steps down one detent); cancel springs back. |
| Navigation | `.sheet(item:)` | Nav3 `OverlayScene` + `onRemove()` | A `SheetSceneStrategy` for WP04's `PresentationHost.sheetStrategy`, an in-window `OverlayScene` whose exit animation runs in `onRemove()`. |
| SwiftUI template (WP08, iOS 17+) | — | — | Native `.sheet` with the modifiers in §1.2, behind `#available` gates: 16.4 (interaction, corner, background, compact adaptation), 18 (sizing, zoom), 26 (automatic glass), 27 (placement, dismissal dialog, cross-fade). |

---

## 5. Audit of the current WP03 `AppSheet` against these findings

Files: `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/overlay/`
`AppSheet.kt` and `AppSheetState.kt`. Code reading only; nothing was run.

**Already right**
- In-window: hosted through `HostedOverlay` (`AppSheet.kt:289-291`), and the content beneath gets
  `clearAndSetSemantics {}` while a modal overlay is up (`AppOverlayHost.kt:58`).
- The offset is read in the placement block of a `Layout` (`AppSheet.kt:245-251`). The content is measured once at
  the large height (`:238, 244`).
- The scrim is a drawn rect following the offset (`:176`, `AppSheetState.kt:85-91`).
- Rubber band c = 0.55 past the top detent (`AppSheetState.kt:138-161`).
- Velocity-seeded settle on the `sheet` spring (`:205-214`), and `MutatorMutex` interruption (`:52, 134-136`).
- `largeFraction` is `@FrequentlyChangingValue` for the card stack (`:69-82`). The constructor is
  `@RememberInComposition` (`:30`).
- Anchors are preserved across resizes (`:120-130`).
- Form sheet on regular windows (`AppSheet.kt:135-140, 225-229`), and the tabletop fold detent (`:233-237`).
- Nested scroll grows the sheet first and hands back at the top (`:153-169`).

**Gaps** (each tied to a finding above)
1. **Customisation surface.** Detents are `enum SheetDetent { Medium, Large }` (`AppSheetState.kt:19-20`), and the
   medium detent is a fixed 0.55 fraction (`AppSheet.kt:334-335`). The sheet has no:
   - fraction, height, fit-content or resolver detents, and no detent that is inactive in compact height;
   - background-interaction mode;
   - scroll-first mode;
   - corner, background, glass or handle slots;
   - `dismissible` or attempt callback;
   - placement;
   - selection callback.

   (§1.2, §1.3, §4)
2. **Grabber a11y and interaction.** The grabber is a plain `Box` with a test tag (`AppSheet.kt:297-303`): no
   click (tap to cycle), no focus, no 48 dp target, no expand, collapse or dismiss actions, no state description.
   (§2.3)
3. **Scrim a11y.** `detectTapGestures { close() }` with no semantics (`AppSheet.kt:177`). M3 gives the scrim
   `onClick` plus "Close sheet" (`src/Scrim.kt:58-71`).
4. **Pane title.** It is set only when `title != null` (`AppSheet.kt:194`). M3 always sets one
   (`src/BottomSheet.kt:335`).
5. **Predictive back.**
   - The raw progress is used, without the `(0.1, 0.1, 0, 1)` easing (`AppSheet.kt:110` versus
     `src/BackHandler.kt:47-51`).
   - The whole column, content included, is scaled with no `scaleX/scaleY` counter-scale, so text squashes
     vertically (`AppSheet.kt:184-191` versus `src/BottomSheet.kt:475-480`).
   - The cancel spring is the `sheet` unit spring (`AppSheetState.kt:200-203`); MDC uses 100 ms
     `FastOutSlowIn` [G].
6. **Detent choice.** The 0.099 s projection alone needs about 4.6 H/s to dismiss from large; iOS needs 2 H/s
   (§1.9).
7. **IME.**
   - The content pads the IME from the sheet's own bottom (`AppSheet.kt:211-212`). At the medium detent that
     bottom is below the window, so the focused field is not lifted.
   - The footer is pinned to the window bottom (`:249-250`) and sits under the keyboard.
   - Fix direction: anchors from `height − ime` (`WindowInsetsRulers.Ime` in placement), or grow to the large
     detent when a field takes focus [U: iOS behaviour] (§2.10).
8. **Restoration.** `AppSheetState` is not saveable (`AppSheetState.kt:29-37`), so the detent is lost on process
   death (§2.11).
9. **iOS 26 look.** It has a fixed shape (`AppSheet.kt:192`) and is always edge-attached. There is no inset at
   partial detents, no concentric bottom corners from `getRoundedCorner`, and no glass-to-opaque lerp (§1.1,
   §2.5).
10. **Navigation.** No `OverlayScene` strategy exists yet for `PresentationHost.sheetStrategy` (§2.7, MOTION-PLAN
    M6 "needs WP03's SheetScene").
11. **IME and Back order.** Check that Back with the keyboard open hides the keyboard first (§2.10) [U].

---

## 6. Measure before trusting

| Item | How |
|---|---|
| iOS 26 inset (pt) at medium and at small custom detents; corner radii; the glass-to-opaque curve; whether the card stack is unchanged from iOS 18 | An iOS 26 screen recording (no Mac or iPhone is available here: ask the user, or use public review captures), stepped frame by frame |
| iOS fast flick from large with a medium detent: skips medium or stops at it | Same |
| iOS keyboard focus in a medium sheet: auto-grow or lift | Same |
| Google Maps, Gmail and Photos sheet stops, and their back behaviour | `adb shell screenrecord` on the Redmi, through `bin/locked adb` |
| Cost of a `Dialog`-window sheet's first frame versus the in-window sheet | `perf.sh` first-frame metrics on a release build |
| Rounded-rect `clip` on the full-screen presenter layer: no offscreen buffer | Perfetto: look for `saveLayer` or offscreen slices during the card-stack frames |
| `isDelayPressesUsingGestureConsumptionEnabled`: press feedback inside a draggable sheet | Compose UI test plus a recording |

---

## Sources

**Apple** [A]
- HIG Sheets: https://developer.apple.com/design/human-interface-guidelines/sheets (change log: 2026-03-24 button
  placement)
- Adopting Liquid Glass: https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass
- `ConcentricRectangle`: https://developer.apple.com/documentation/swiftui/concentricrectangle
- SwiftUI modal presentations index: https://developer.apple.com/documentation/swiftui/modal-presentations (each
  API page is linked in §1.2)
- UIKit `UISheetPresentationController`: https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller
  (property pages in §1.3)
- `UIModalPresentationStyle.formSheet` / `.pageSheet`:
  https://developer.apple.com/documentation/uikit/uimodalpresentationstyle/formsheet
- `UIViewController.Transition.zoom(options:sourceViewProvider:)`:
  https://developer.apple.com/documentation/uikit/uiviewcontroller/transition/zoom(options:sourceviewprovider:)
- WWDC sessions:
  - WWDC19 224 (sheet dismissal): https://developer.apple.com/videos/play/wwdc2019/224/
  - WWDC18 803 (projection): https://developer.apple.com/videos/play/wwdc2018/803/
  - WWDC21 10063 (UIKit sheets): https://developer.apple.com/videos/play/wwdc2021/10063/
  - WWDC24 10145 (zoom, velocity): https://developer.apple.com/videos/play/wwdc2024/10145/
  - WWDC25 323 (SwiftUI new design): https://developer.apple.com/videos/play/wwdc2025/323/
  - WWDC25 284 (UIKit new design): https://developer.apple.com/videos/play/wwdc2025/284/
  - WWDC26 269 "What's new in SwiftUI": https://developer.apple.com/videos/play/wwdc2026/269/ (no sheet details
    in the transcript; the iOS 27 sheet APIs come from the API docs)
- The DocC JSON behind each page was read at `https://developer.apple.com/tutorials/data/<path>.json`.

**Google** [G]
- Material 3 bottom sheets:
  - specs: https://m3.material.io/components/bottom-sheets/specs
  - guidelines: https://m3.material.io/components/bottom-sheets/guidelines
  - accessibility: https://m3.material.io/components/bottom-sheets/accessibility
- Gestures (predictive back components): https://m3.material.io/foundations/interaction/gestures
- androidx sources, pinned in `src/`. Commits are cited by short SHA at
  `https://github.com/androidx/androidx/commit/<sha>`.
- Compose foundation 1.12.1 sources jar (URL at the top); foundation-layout and ui 1.12.1 sources jars from the
  same Maven path.
- Compose foundation release notes: https://developer.android.com/jetpack/androidx/releases/compose-foundation
- Compose performance best practices: https://developer.android.com/develop/ui/compose/performance/bestpractices
- Compose bottom sheets guide: https://developer.android.com/develop/ui/compose/components/bottom-sheets
- Fold-aware apps: https://developer.android.com/develop/ui/compose/layouts/adaptive/foldables/make-your-app-fold-aware
- API 36 `View` changes (`announceForAccessibility` deprecated):
  https://developer.android.com/sdk/api_diff/36/changes/android.view.View
- MDC-Android `master` (pinned in `src/mdc/`):
  https://github.com/material-components/material-components-android
- Nav3 recipes: https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/bottomsheet
- Android skills (installed): `navigation/navigation-3/.../recipes/bottomsheet.md`, `system/edge-to-edge/SKILL.md`

**Third party** [M]/[U]
- Flutter Cupertino sheet (iOS 18 measurements):
  https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/cupertino/sheet.dart
- Nil Coalescing, Liquid Glass sheets: https://nilcoalescing.com/blog/PresentingLiquidGlassSheetsInSwiftUI
  (consistent with Apple's statements; no numbers)
- TrueSheet liquid glass guide: https://sheet.lodev09.com/guides/liquid-glass. A custom `backgroundColor` or blur
  disables the glass. `UIDesignRequiresCompatibility` is ignored from iOS 27 [U].
