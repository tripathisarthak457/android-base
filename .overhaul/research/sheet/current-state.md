# Bottom sheet: current state, defects and integration seams

Written 2026-10-12 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-139`). Read-only survey: no worktree or repository was
changed, and no Gradle or adb was run.

Tags: **[C]** a code fact, cited `file:line`. **[V]** verified in the shipped bytecode of the named library (javap on the
AAR in `~/.gradle/caches`). **[D]** derived by reading the code, not run. **[U]** unverified. **[A]** Apple or Google
documentation (URL given, not fetched this session). **[E]** eyeballed or measured by a third party, as the research
files tag it.

Worktree heads read: WP03 `718c1e7` (Oct 10 16:31; sheet files last changed at `e68bbfa`; uncommitted edits in
other files only), WP04 `14dccc2` (Oct 12 13:26), WP05 `e783fd1` (glass files untracked), WP07 `ec15ef4`, WP08 `5cbcd9b`.
Integration branch `overhaul/multiplatform-v2` at `8672003`.

Path prefixes used below:

- `ds/` = `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`
- `nav3/` = `wt/WP03/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/`
- `nav4/` = `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/`
- `glass/` = `wt/WP05/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/`
- `mat/` = `research/sheet/sources/` (androidx Material 3 sources copied by a sibling agent; upstream
  https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/)

---

## 0. In one page

- **What ships today:** the integration branch still ships the old `AppBottomSheet`, a `Dialog` window with no
  detents. It fades its scrim with layer alpha, reads the drag offset in composition and dismisses on a tween
  (`wt/WP04/.../overlay/AppBottomSheet.kt:50-131`). WP03 deletes it.
- **The real component is WP03's, and it is unmerged:**
  - `AppSheet` + `AppSheetState` (`ds/component/overlay/AppSheet.kt`, `AppSheetState.kt`): two detents, chosen by
    iOS projection;
  - a rubber band past the top, a drag that only re-places (tested), and a scrim drawn as a rect;
  - idempotent dismiss and Back at overlay priority, the MDC bottom-container predictive shrink, and a pinned footer;
  - a form sheet on large windows and a fold detent in tabletop.

  The core mechanics are good and worth keeping.
- **Wiring:** the navigation side is unwired. WP03's `SheetScene` (`nav3/SheetScene.kt`) has no caller. WP04's
  `AppShell` gets no `sheetStrategy` (`wt/WP04/.../app/ui/AppRoot.kt:163`), so every `Presentation.Sheet` renders as a
  cover (`nav4/presentation/PresentationHost.kt:76`).
- **Defects that break behaviour:**
  1. `AppSheet(visible)` re-shown during its dismiss is left stuck half-way, deaf to drag, scrim and Back (§2.6 D1).
  2. `SheetSceneStrategy` wipes its state map on every scene calculation, because Navigation 3 calls it again for the
     overlaid entries ([V]). A sheet under another sheet re-animates on every stack change (§4.4 N1).
  3. WP04's `PresentedFrame` wraps sheet entries in an opaque full-screen background, a zeroed bottom-chrome inset and
     the wrong motion reporter (§4.4 N4).
  4. An in-screen sheet stays drawn over another tab after a deep link or a shortcut switches tabs (§4.4 N7).
- **Requirement 33 is mostly missing:**
  - fraction, fixed, fit-content and custom detents;
  - the card stack (QA-11);
  - non-modal sheets, a confirm-before-dismiss hook, styles (Frosted and Glass), header slots, nested navigation in a
    sheet;
  - TalkBack actions;
  - a side sheet, the iOS 26 inset sheet;
  - Multiplatform parity, and most of the SwiftUI options.
- **Glass:** WP05's design forbids sampling glass on sheets (`design/wp/WP05.md:64`), and REQUIREMENTS 33 asks for a
  Glass style through WP05's tiers. The spec has to settle that conflict (§5).

---

## 1. Inventory

| Piece | Where | State |
|---|---|---|
| Legacy `AppBottomSheet`, `AppActionSheet` | integration branch `8672003`; copies in WP01, WP02, WP04–WP10 | Shipping. Replaced by WP03 (`handoff/WP03.md`, "Removed": `AppBottomSheet -> AppSheet`) |
| `AppSheet`, `AppSheetState`, `SheetDetent` | `ds/component/overlay/AppSheet.kt` (345 lines), `AppSheetState.kt` (225) | WP03, unmerged |
| `AppActionSheet`, `SheetAction` | `ds/component/overlay/AppActionSheet.kt` (197) | WP03, unmerged |
| `OverlayBackHandler` | `ds/component/overlay/OverlayBackHandler.kt` (51) | WP03 |
| `AppOverlayHost`, `HostedOverlay`, `LocalOverlayHost` | `ds/component/overlay/AppOverlayHost.kt` (100) | WP03 (layers 4 and 5) |
| `SheetSceneStrategy`, `SheetScene`, `SheetPresentation` | `nav3/SheetScene.kt` (111) | WP03, **no caller anywhere** (grep) |
| Sheet tests | `SheetTest.kt` (7 tests), `RecompositionJourneysTest.kt:171-186`, `PostureTest.kt:70-78` | WP03 |
| Catalog demos | `wt/WP03/.../catalog/sections/CatalogOverlays.kt:61-97`, `CatalogAdaptive.kt:135-154` | WP03 |
| `Presentation` (`Sheet(detents)`, `Cover`), its own `SheetDetent` | `nav4/Presentation.kt:8-24` | WP04 |
| `PresentationHost`, `PresentedFrame`, `PresentationCard` | `nav4/presentation/` | WP04 |
| `BackGate`, `NavGuard`, `StackBackHandler`, `GateSettle` | `nav4/back/` | WP04 |
| `ShellState` presentation stack, `BottomChromeWriter`, `KeptTabs` | `nav4/ShellState.kt`, `nav4/shell/`, `nav4/shell/tabs/` | WP04 |
| `Chrome.kt` (insets, tab visibility) | WP03 `ds/foundation/Chrome.kt`; WP04 `.../foundation/layout/Chrome.kt` | Diverged (§4.7) |
| Glass foundation | `glass/` (12 files, 1194 lines) | WP05, untracked, in progress |
| `Presentation.kt` copy | `wt/WP07/templates/multiplatform/core/navigation/src/commonMain/.../Presentation.kt:8-24` | WP07, no sheet component |
| `View.appSheet(isPresented:/item:detents:)` | `wt/WP08/templates/ios/Packages/Core/Sources/DesignSystem/Components/Sheet/AppSheet.swift` | WP08 |
| PeriFlow `SheetScene`, `AppDetentSheet`, `AppBottomSheet`, `AppSheetCloseButton`, `LocalSheetSurface` | `~/Documents/projects/jorwen/periflow/periflow-android/core/{navigation,designsystem}` | Same on `main` and WP11, except a package move in the close button |
| VitFleet `AppBottomSheet`, `AppActionSheet`, `LocalOverlaysHidden`, `ChoiceSheet`, `HistorySheet` | `~/Documents/projects/tools/vitfleet/vitfleet-android/core/{designsystem,ui}` | Shipping in VitFleet 0.2.0 |
| Material reference | `mat/BottomSheet.kt`, `ModalBottomSheet*.kt`, `SheetDefaults.kt`, `AnchoredDraggable.kt`, … | Reference only (policy forbids the dependency, REQUIREMENTS 31) |

---

## 2. WP03 `AppSheet`

### 2.1 API as built

```kotlin
enum class SheetDetent { Medium, Large }                                         // AppSheetState.kt:20
@Stable class AppSheetState @RememberInComposition constructor(
    val detents: Set<SheetDetent> = setOf(Medium, Large),
    initialDetent: SheetDetent = Medium if present else Large,                    // :29-37
) {
    val currentDetent: SheetDetent?                                              // :62-67, null while hidden or leaving
    @get:FrequentlyChangingValue val largeFraction: Float                         // :74-82, 0 at medium, 1 at large; 0 for a form sheet
    var dismissing: Boolean (private set)                                         // :49
    suspend fun animateTo(detent: SheetDetent)                                    // :93-97
    suspend fun dismiss(onGone: () -> Unit = {})                                  // :100-105, idempotent
}
@Composable fun AppSheet(state, onDismiss, modifier, title: String? = null, showCloseButton = false,
    footer: (@Composable () -> Unit)? = null, content)                           // AppSheet.kt:84-96 (for SheetScene)
@Composable fun AppSheet(visible, onDismissRequest, modifier, detents = {Medium, Large}, title, showCloseButton,
    footer, content)                                                              // AppSheet.kt:262-292 (in-screen, overlay host)
```

There is no `rememberAppSheetState()` and no `Saver`. Callers write `remember { AppSheetState() }` (`SheetTest.kt:38`,
`RecompositionJourneysTest.kt:172`).

### 2.2 Behaviour

- **Hosting:**
  - `AppSheet(state)` draws in place: inside `SheetScene`, it is drawn in the presentation host.
  - `AppSheet(visible)` draws through `HostedOverlay(modal = true)` (`AppSheet.kt:289`) in the window's overlay layer.
    That layer sits above the tab bar and above presented sheets (`AppOverlayHost.kt:20-33`). It keeps the caller's
    composition locals (`:88-96`) and clears the semantics of everything beneath (`:58`).
- **Show:** `LaunchedEffect(state) { state.show(animate = !reduceMotion) }` (`AppSheet.kt:147`).
  - `show` waits for the first anchors through `snapshotFlow` (`AppSheetState.kt:107-118`, `:132`).
  - It then slides from `hidden` to the initial detent on `springs.sheet.pixels` (`:205-214`).
- **Measure once, place on every frame:** one `Layout` with two slots, the sheet and the footer
  (`AppSheet.kt:179-252`).
  - The sheet is measured at `Constraints.fixed(width, height − large)` (`:238`, `:244`) and placed at
    `state.offset`, which is read only in the placement block (`:245-248`).
  - Anchors are computed in measure and written with `updateAnchors` (`:241`). Dragging never re-measures
    (`SheetTest.kt:94-107`) and recomposes nothing (`RecompositionJourneysTest.kt:171-186`).
- **Detents:**
  - Large: status bar + 10 dp (`AppSheet.kt:232`, `:343`). Medium: 55 % of the window (`:236`, `:335`).
  - In Compact height there is one height, the large one (`:142`, `:234`).
  - In tabletop posture, medium rests on the fold (`:138-139`, `:235`, `PostureTest.kt:70-78`).
- **Form sheet:**
  - Used when the container is Medium wide **and** Medium tall, and not in tabletop (`AppSheet.kt:140`).
  - It is a centred card, `min(formSheetWidth, width − 2·gutter)` × `min(0.85·H, 620 dp)` (`:225-229`), clipped
    with `shapes.xl` (`:192`). The close button is always shown (`:206`). It slides up from the bottom (`hidden =
    height`). `largeFraction` is 0 (`AppSheetState.kt:79`).
- **Drag:**
  - `draggable` on the sheet column (`AppSheet.kt:196-204`). Drag start calls `state.stop()` (a `MutatorMutex`
    mutate at `UserInput`, `AppSheetState.kt:134-136`).
  - `dragBy` (`:142-159`) clamps to `[top detent, hidden]`. Past the top detent it rubber-bands with iOS's
    `c·x·d / (d + c·x)`, where `c = springs.rubberBand` (0.55) and `d` = window height (`:161`).
- **Nested scroll** (`AppSheet.kt:153-169`):
  - Upward pre-scroll grows the sheet until its top detent before the content scrolls.
  - Downward post-scroll from user input at the content's top drags the sheet.
  - Pre-fling settles the sheet if it is not resting and consumes all the velocity.
- **Settle** (`AppSheetState.kt:179-197`):
  - It projects the release, `offset + v · 0.099 s` (iOS 0.99/ms deceleration, `:217-218`), and picks the nearest of
    the detents and `hidden`.
  - It animates there with the release velocity. The `Threshold` haptic plays only when the sheet lands on a
    different detent from the one the drag started at (`:194-195`; `handoff/WP03.md:400-401`).
- **Dismiss:** `dismiss()` sets `dismissing`, slides to `hidden`, then calls `onGone` once (`AppSheetState.kt:100-105`).
  Back while dismissing is consumed and ignored (`AppSheet.kt:110-113`; `SheetTest.kt:65-79`). Drag is disabled while
  dismissing (`AppSheet.kt:199`).
- **Back** (`AppSheet.kt:104-114`):
  - `OverlayBackHandler` registers a `NavigationEventHandler` at `PRIORITY_OVERLAY` on the caller's dispatcher
    (`OverlayBackHandler.kt:34-50`).
  - Progress drives `backProgress`. The sheet scales toward its bottom centre by up to 48 dp across and 24 dp down
    (`AppSheet.kt:184-191`, `:340-342`), which is MDC's bottom container (`research/predictive-back/SPEC.md:79`).
  - Cancel springs `backProgress` back on `springs.sheet.unit` (`AppSheetState.kt:200-203`). Commit runs `dismiss`.
- **Scrim:** `drawBehind { drawRect(colors.scrim, alpha = state.progress) }` (`AppSheet.kt:176`). It is a rect drawn
  in the draw phase, with no offscreen layer, which closes SMOOTHNESS S8 for sheets. A tap anywhere on it closes the
  sheet (`:177`).
- **Footer:**
  - It is measured with `maxWidth = width` and placed at `max(windowBottom − footerHeight, sheetTop)`
    (`AppSheet.kt:242`, `:249-250`); a form sheet uses its own bottom.
  - Its height is published to the content through a sheet-local `LocalBottomChrome` (`:143`, `:214`, `:243`).
- **Insets:**
  - The content box consumes the status bar inset (`AppSheet.kt:211`) and pads the bottom IME inset (`:212`).
  - Content runs edge to edge: `AppSection` insets itself, and anything else uses the gutter (KDoc `:81-82`, QA-17
    resolution `a8333b0`).
- **Header** (`AppSheet.kt:294-332`):
  - A grabber of 36 × 5 dp, 6 dp from the top, in `contentTertiary` at alpha 0.45, tagged `sheet_grabber`.
  - Below it, either a 44 dp row with the title centred and a Tinted Small close button at the end, or a 20 dp
    handle area.
- **Resize** (`AppSheetState.kt:120-130`): a sheet resting on a detent stays on that detent when the anchors change.
- **`AppSheet(visible)` lifecycle** (`AppSheet.kt:273-286`): a `session` counter makes a fresh `AppSheetState` for
  each opening. Hiding runs `dismiss()` and then unmounts.

### 2.3 Constants

| Constant | Value | Where | Reference |
|---|---|---|---|
| Medium detent | 55 % of H showing | `AppSheet.kt:335` | PeriFlow 62 % (`AppDetentSheet.kt:339`); iOS ≈ half [A S24] |
| Large detent | status bar + 10 dp | `AppSheet.kt:232`, `:343` | iOS 18: top at 8 % of H [E] (`research/ios-motion-catalogue.md:46-47`) |
| Form sheet | `formSheetWidth` 540 dp (600 dp from Large) × `min(0.85 H, 620 dp)` | `AppSheet.kt:226-227`, `:336-337`; `ds/foundation/adaptive/AppLayout.kt:45-66` | ADAPTIVE-PLAN:434 says `min(0.85·H, content)` |
| `sheetMaxWidth` | 640 dp | `AppLayout.kt:65` | ADAPTIVE-PLAN:231 |
| Predictive shrink | 48 dp across, 24 dp down, origin bottom centre | `AppSheet.kt:340-342` | MDC `m3_back_progress_bottom_container_max_scale_*` (SPEC:79) |
| Grabber | 36 × 5 dp, top 6 dp, `contentTertiary` α 0.45; handle area 20 dp; header 14 dp + 44 dp | `AppSheet.kt:300-306`, `:344-345` | Not tokens |
| Projection | 0.099 s of the velocity (r = 0.99/ms) | `AppSheetState.kt:217-218` | MOTION-PLAN §1 rule 3 |
| Rubber band `c` | 0.55 | `ds/theme/tokens/DesignTokens.kt:196`; `tokens/motion.json` `scroll.rubberBand` | iOS [A] |
| `springs.sheet` | 0.34 s, bounce 0 (Standard and Bouncy); Calm 0.3908; Snappy 0.2957 (÷speed) | `DesignTokens.kt:135`, `:152`, `:169`, `:186` | CP1 keep (`decisions/CP1.md:40`, `:137`): within 0.004 s of iOS's page curve |
| `springs.dialog` (action sheet) | 0.275 s, 0 | `DesignTokens.kt:136` | iOS k 522 (`ios-motion-catalogue.md:94`) |
| Scrim | light `0x660B0F1A`, dark `0x8F000000` | `DesignTokens.kt:249`, `:292` (and per style) | — |
| Corner radius | `shapes.sheet` top corners: 24 / 30 / 8 / 28 dp by style; form sheet `shapes.xl` | `DesignTokens.kt:617`, `:637`, `:657`, `:677`; `AppSheet.kt:192` | iOS 26 concentric [A S17] |
| Action sheet row | 56 dp | `AppActionSheet.kt:197` | — |

Only the springs and `rubberBand` are tokens. Every geometric sheet constant is a private `val` in `AppSheet.kt`, and
none of them is in `tokens/motion.json` (WP03 `tokens/motion.json`). WP07 and WP08 therefore cannot share them.

### 2.4 Tests

| Test | Covers |
|---|---|
| `SheetTest.kt:65-79` | A second and third Back during a dismiss are ignored; nothing beneath pops |
| `SheetTest.kt:81-92` | Two `dismiss()` calls run the callback once |
| `SheetTest.kt:94-107` | A drag never re-measures the content |
| `SheetTest.kt:109-123` | The rubber band gives less with each pull past large; the sheet never goes past hidden |
| `SheetTest.kt:125-141` | A slow release settles back; a flick of 9000 px/s from medium closes the sheet |
| `SheetTest.kt:143-151` | `largeFraction` is 0 at medium and 1 at large |
| `SheetTest.kt:153-162` | 1280×800 gives a centred form sheet with `largeFraction` 0 |
| `RecompositionJourneysTest.kt:171-186` | 30 drag moves on the grabber recompose nothing |
| `PostureTest.kt:70-78` | Tabletop: the sheet's top rests at the hinge's bottom |

Not covered:

- `AppSheet(visible)` toggling;
- predictive back progress and cancel;
- the IME, navigation-bar and footer insets;
- resizing while the sheet animates;
- accessibility;
- nested sheets, or a sheet inside a kept tab;
- `SheetScene` through a real `NavDisplay`, which WP03's own handoff asks for (`handoff/WP03.md:473-474`);
- `AppActionSheet`;
- screenshot tests of any of these.

WP04 has no sheet test at all: its tests register only `Presentation.Cover` (`.../test/.../TestScreens.kt:28`).

### 2.5 What is good (keep it)

1. The state lives outside composition. Anchors are written in measure and the offset is read in placement, so a drag
   costs a re-placement only (MOTION-PLAN class L1). Both properties are tested.
2. The content is measured once at the large height and only moved after that.
3. The detent is chosen by projection with r = 0.99 rather than a velocity threshold, and the settle carries the
   release velocity.
4. The rubber band uses the shared `c`, with the iOS formula.
5. Dismiss is idempotent through `dismissing` and a `MutatorMutex`. Back is consumed during a dismiss (tested).
6. The scrim is a drawn rect, with no full-screen layer alpha (S8).
7. Back goes through `navigationevent` at `PRIORITY_OVERLAY` on the caller's dispatcher, so an overlay in a hidden
   kept tab does not take Back (`OverlayBackHandler.kt:14-26`). It also replaces the legacy `BackHandler`.
8. The nested-scroll hand-off is iOS's `prefersScrollingExpandsWhenScrolledToEdge` [A S25].
9. A footer is pinned to the visible bottom without a composition local.
10. Adaptive behaviour: the form sheet, a single detent at Compact height, the fold detent, and keeping the detent
    across a resize.
11. The detent haptic plays only when the detent changes.
12. `largeFraction` is annotated `@FrequentlyChangingValue` for the card stack, and so is
    `SheetSceneStrategy.largeFraction` (RECOMPOSITION-PLAN R7).

### 2.6 Defects

Severity: **S1** broken or stuck, **S2** visibly wrong, **S3** API or polish.

| # | Sev | Defect | Where | Scenario | Conf. |
|---|---|---|---|---|---|
| D1 | S1 | `AppSheet(visible)` shown again during its dismiss gets stuck | `AppSheet.kt:276-286`; `AppSheetState.kt:100-105`; `AppSheet.kt:112`, `:199` | The caller sets `visible` false, then true within the slide (a flicker in UI state, a programmatic reopen). The new `LaunchedEffect` cancels `dismiss()` mid-animation, and `mounted` is still true, so nothing restarts. The sheet stays part-way down with `dismissing = true`: drag is off, Back is ignored, and a scrim tap calls `dismiss()`, which returns at once without calling `onGone`. Only another `visible = false` frees it | [D] high |
| D2 | S2 | The IME is counted twice when there is a footer, and content recomposes on every IME frame | `AppSheet.kt:212`, `:242-243`; `AppBottomActionBar.kt:39`; `Chrome.kt:46-55` | The content box pads the IME. The footer (`AppBottomActionBar`) pads navigation bar ∪ IME, and its whole height becomes the content's `LocalBottomChrome`. A list with `chromeContentPadding()` therefore ends one IME height above the footer. The footer height is written during measure on each frame of the IME animation, and `chromeContentPadding` reads it in composition | [D] |
| D3 | S2 | Content without a footer ignores the navigation bar | `AppSheet.kt:143`, `:243` (0 dp when there is no footer); `Chrome.kt:46-55` | At the large detent the sheet's bottom is the window's bottom. With 3-button navigation, the last row sits under the buttons, because nothing pads `navigationBars` | [D]; check with a 3-button screenshot |
| D4 | S2 | A text field at the medium detent can sit under the keyboard | `AppSheet.kt:238`, `:244`, `:247-248`, `:212` | At medium, the bottom (medium − large) px of the content is off-screen, so the IME padding acts on an edge nobody sees, and the sheet does not rise when a field takes focus. iOS raises a medium sheet when the keyboard shows [U] | [D] |
| D5 | S2 | A long title runs under the close button | `AppSheet.kt:306-325` | The title is `align(Center)` in a full-width box and the button sits at `CenterEnd` with no reserved width. The 44 dp row is fixed, so a 2× font may clip [U] | [D] |
| D6 | S2 | Predictive back squashes the content and uses raw progress | `AppSheet.kt:184-191`; `OverlayBackHandler.kt:40-41` | The non-uniform scale applies to text as well. Material counter-scales the content (`mat/BottomSheet.kt:475-479`) and eases the progress with `PredictiveBack.transform` (`mat/BottomSheet.kt:160`); SPEC §2 `ProgressEasing` (0.1, 0.1, 0, 1) | [C] vs [S] |
| D7 | S2 | A retarget loses velocity | `AppSheetState.kt:96`, `:103`, `:114`, `:205-214` | `animateTo`, `dismiss` and `show` start `animate(…)` with velocity 0. Dismissing during the show, or `animateTo` during a settle, restarts from rest: a visible kink against REQ 21 and MOTION-PLAN §1 rule 4. There is no `Animatable` that keeps the velocity | [C] |
| D8 | S3 | A content scroll does not stop a running settle | `AppSheet.kt:156-161` vs `:200` | The draggable calls `stop()`, but the nested-scroll path does not. A finger on a list while the sheet settles writes the offset while the animation also writes it | [D], runtime [U] |
| D9 | S3 | `animateTo` is allowed during a dismiss | `AppSheetState.kt:93-97` | It can pull a leaving sheet back up with `dismissing` still true, which leaves it stuck the way D1 does | [D] |
| D10 | S3 | A resize during an animation lands off its detent | `AppSheetState.kt:124-130` | The anchors move only when the sheet rests within 0.5 px. A running animation keeps its old pixel target through a rotation or an unfold | [D] |
| D11 | S2 | The form sheet does not match ADAPTIVE-PLAN | `AppSheet.kt:140`, `:225-229`, `:196-204` | The plan (`ADAPTIVE-PLAN.md:434`) asks for a scale-in on the alert spring, no detents or drag-to-dismiss, and a height of `min(0.85·H, content)`. The build slides up from the bottom, can be dragged, and has a fixed height | [C] |
| D12 | S2 | No TalkBack affordances | `AppSheet.kt:172-178`, `:194`, `:297-303` | The grabber has only a test tag: Material gives it expand, collapse and dismiss actions (`mat/BottomSheet.kt:360-362`). The scrim has no label or click semantics: Material's scrim says "Close sheet" (`mat/ModalBottomSheet.kt:153`). There is no `isTraversalGroup` (`mat/ModalBottomSheet.kt:141`). `paneTitle` is set only when there is a title | [C] |
| D13 | S3 | The sheet adds no scroll container, and the demo that claims a hand-off cannot scroll | `CatalogOverlays.kt:76-91`; `ds/component/list/AppSection.kt:46` (a plain `Column`) | The "Filters" demo's 16 rows are clipped, and the nested-scroll hand-off is never exercised in UI. Tall content without a scroller of its own gets cut off; VitFleet wraps content in `verticalScroll` for this reason (§11) | [C] |
| D14 | S3 | The composition writes the state's motion | `AppSheet.kt:144-145`; `AppSheetState.kt:42-43`, `:206-209` | `state.spring` and `state.rubberBand` are plain fields set during composition. Before the first composition, every animation is a snap | [C] |
| D15 | S3 | No `rememberAppSheetState`, no `Saver`; `AppSheet(visible)` freezes its `detents` | `AppSheet.kt:275` | After process death, a presented sheet comes back at its initial detent and animates in again, because `ShellState` saves only the keys (`nav4/ShellState.kt:304-314`). Changing `detents` while the sheet is open has no effect | [C] |
| D16 | S3 | Esc relies on the platform's fallback | `OverlayBackHandler.kt:24-25` | ADAPTIVE-PLAN:565 asks for an explicit `Key.Escape` handler (the `Generic.kcm` fallback is [U]) | [C] |

### 2.7 Against REQUIREMENTS 33

| Asked (`REQUIREMENTS.md:127-139`) | Today (WP03 unless noted) |
|---|---|
| Detents: fixed height, fraction, fit-content, custom | **Missing.** Enum `Medium`/`Large` only (`AppSheetState.kt:20`). PeriFlow and VitFleet have fit-content sheets (§11) |
| Chosen by velocity projection | **Done** (`AppSheetState.kt:184-188`) |
| Rubber band past the top | **Done** (`:150-157`, `:161`) |
| Card stack behind a modal sheet | **Missing** (QA-11, `qa/r1/DEFECTS.md:35`). The input exists (`largeFraction`); nothing reads it |
| Grabber | **Partial.** It is drawn only: no tap to cycle detents ([A] `ios-motion-catalogue.md:164`), no accessibility, no option to hide it |
| Haptics at detents | **Done** (snap only) |
| Inset sheet at partial detents that attaches to the edges when expanded (iOS 26) | **Missing** |
| Non-modal (background interaction up through a detent) | **Missing.** The scrim always takes taps (`AppSheet.kt:172-178`), and the overlay is always modal (`:289`) |
| Header, footer and sticky-action slots | **Partial.** A `title: String` and a close button; a footer slot. No header slot, no leading or trailing actions (Cancel / Done), no sticky top |
| Solid, Frosted and Glass styles | **Missing.** `background(colors.surface)` (`AppSheet.kt:193`) |
| Interactive dismiss with a confirm hook | **Missing.** No veto (Material: `confirmValueChange`, `mat/ModalBottomSheet.kt:114`, `:243`) and no way to turn off interactive dismissal |
| Nested sheets | **Partial and broken.** In-screen sheets stack without a transform on the lower sheet. Destination sheets: N1 (§4.4). iOS: the lower sheet scales to 0.9165 and moves up 0.5 % [E] (`ios-motion-catalogue.md:166`) |
| Navigation 3 sheet destinations through metadata | **Partial.** WP03 `SheetScene` exists but is unwired; there are two metadata shapes (§4.4 N2, N3) |
| Gmail-style predictive back | **Partial.** The MDC bottom container is right for sheets (SPEC §5.4), but D6 applies |
| IME-correct | **Defects** D2 and D4 |
| Adaptive: form sheet, side sheet, foldables, ChromeOS | Form sheet partial (D11). **Side sheet missing.** Tabletop done. A resize keeps the detent, but D10 applies |
| TalkBack actions | **Missing** (D12) |
| Zero recompositions per drag frame; offsets in placement, scrim in draw | **Done and tested** |
| Parity: Compose Multiplatform | **Missing** (§6) |
| Parity: SwiftUI `appSheet` over `presentationDetents` | **Partial** (§7) |

---

## 3. WP03 `AppActionSheet`

- **API:** `AppActionSheet(visible, actions: List<SheetAction>, onDismissRequest, modifier, title, message,
  cancelLabel)`. `SheetAction(label, onClick, icon, destructive, enabled)` is an `@Immutable` class
  (`AppActionSheet.kt:47-75`).
- **Compact width:**
  - It is a hosted overlay (`:97`) and slides up by its own height on `springs.dialog.unit`, which closes MOTION M17
    (`:100-101`, `:114`).
  - The scrim is a drawn rect (`:104-109`).
  - It has two `surfaceElevated` groups, the actions and Cancel apart (`:119-148`), with rows of 56 dp using
    `PressFeedback.Highlight` (`:177-197`).
  - A choice runs before the sheet asks to close (`:138-141`).
- **Medium width and up:** it becomes an `AppMenu` popover anchored to wherever the call is composed (`:76-89`).

Defects:

| # | Sev | Defect | Where |
|---|---|---|---|
| A1 | S2 | No scroll and no height cap. At Compact height, or with many actions, the column overflows the top of the window. The Settings language picker passes every supported locale plus System (`wt/WP03/.../feature/settings/Settings.kt:424-437`) | `AppActionSheet.kt:110-149` |
| A2 | S2 | No selected state or checkmark: the language picker shows no current choice (QA-14, `qa/r1/DEFECTS.md:38`) | `AppActionSheet.kt:48-55` |
| A3 | S3 | Back is enabled only while `targetState` is true (`:95`). A second Back during the exit slide pops the screen beneath, unlike `AppSheet`, which consumes it | `AppActionSheet.kt:95` |
| A4 | S3 | The popover anchors where it is composed. Settings composes it at the end of the screen, not beside the row that opens it, so on a tablet it anchors in the wrong place [D] | `Settings.kt:424`; `AppActionSheet.kt:63-65` |
| A5 | S3 | No `paneTitle` and no traversal group. The action list is rebuilt on every recomposition of the caller (`listOf(…) + map`), so the sheet is never skipped | `AppActionSheet.kt:110`; `Settings.kt:426-434` |

---

## 4. Navigation: `SheetScene` (WP03) and the presentation host (WP04)

### 4.1 `SheetScene` as written

- `SheetPresentation(detents: Set<SheetDetent> = {Medium, Large}, showCloseButton = false)` is stored under the typed
  key `SheetKey : NavMetadataKey<SheetPresentation>` (`nav3/SheetScene.kt:25-28`, `:69-75`). That matches the
  recipe's `BottomSheetKey` pattern
  (`~/.claude/plugins/marketplaces/android-skills/navigation/navigation-3/references/android/guide/navigation/navigation-3/recipes/bottomsheet.md:186-191`).
- `calculateScene` (`:55-67`):
  - When the top entry has no `SheetKey`, it runs `states.clear()` and declines.
  - With nothing below the sheet, it declines.
  - Otherwise it runs `retainAll(top)` and `getOrPut(top.contentKey) { AppSheetState(detents) }`, then returns an
    `OverlayScene` with `overlaidEntries = below`.
- The scene's content (`:93-101`):
  - `DisposableEffect` publishes the state as `strategy.shown`, which backs `largeFraction` for the card stack
    (`:51-53`).
  - It then calls `AppSheet(state, onDismiss = onBack, showCloseButton)`. There is no title, footer or style.
- `onRemove()` runs `state.dismiss()` (`:103-105`), so a pop from anywhere slides the sheet out first.

### 4.2 Navigation 3 facts this depends on (verified)

- **1.2.0 (`navigation3-ui-android-1.2.0` AAR):**
  - `rememberSceneState` runs `provideScene(entries)`. While the last scene is an `OverlayScene`, it runs
    `provideScene(overlay.overlaidEntries)` again, inside a `remember` keyed on the entries and strategies [V]
    (`SceneStateKt`, bytecode offsets 300-343). **The strategy is therefore called once per overlay level on every
    calculation.**
  - `NavDisplay` renders each overlay scene in a movable group keyed on the scene object [V] (`startMovableGroup` with
    the `OverlayScene`). The top overlay gets `rememberLifecycleOwner(RESUMED)`, the others `STARTED`.
  - `NavDisplay` also provides `LocalEntriesToExcludeFromCurrentScene`, `LocalCurrentScene` and
    **`LocalNavAnimatedContentScope` = the underlying scene's `AnimatedContentScope`** [V]. The library's own error
    text says "AnimatedContentScope in OverlayScenes are no-op".
- **`OverlayScene.onRemove(): suspend`** exists in 1.2.0 and in 1.1.7, which is what JetBrains 1.1.2 for WP07 is
  built from [V]. In 1.1.7 the scope-provision path was not checked in bytecode [U]; only the error message was seen.
- **The recipe** (`bottomsheet.md`): a stateless strategy (`:162-176`), a `data class` scene keyed by the content key,
  `rememberLifecycleOwner()` around the entry (`:141-147`, which NavDisplay 1.2.0 now does itself), and "add this
  strategy before any non-overlay scene strategies" (`:157`).

### 4.3 WP04's presentation host

- **Registration:**
  - `entry<K>(presentation = Presentation.Sheet(detents) | Presentation.Cover)` (`nav4/NavGraph.kt:50-57`).
  - `Presentation.Sheet(detents: List<SheetDetent> = listOf(Large))` uses WP04's own `SheetDetent` enum
    (`nav4/Presentation.kt:11-24`).
  - The metadata goes under the string key `"com.base.app.navigation.presentation"` and is read back with `as?`
    (`PresentationHost.kt:198-205`).
- **Routing** (`nav4/ShellState.kt:178`, `:195-210`):
  - `ShellState.submit` sends a key registered with a presentation to the flat `presented` stack. Every other command
    goes to `front`, which **is the presented stack while anything is presented** (`:116`).
  - A deep link (`open`) selects the owning tab first.
- **`PresentationHost`** (`PresentationHost.kt:66-155`):
  - It is composed only while something is presented or a `PresentedFrame` is alive (`:80`).
  - It runs a `NavDisplay` over `[PresentationBase] + presented.keys` (`:85-88`) with the injected `sheetStrategy`
    (`:121-122`). `onBack` goes to `gate.onBack(pop)` (`:122`).
  - Its navigation-event state is inert, so NavDisplay seeks no predictive pop of its own (`:123-125`).
  - Covers use the back card (`PresentationCard.kt:33-35`: only a single cover can become the card).
  - `StackBackHandler` and `GateSettle` (`:153-154`) give it rapid-Back coalescing.
- **`PresentedFrame`** (`PresentationHost.kt:161-196`) wraps **every** presented entry, sheets included:
  - `ReportMotion(LocalNavAnimatedContentScope.current.transition, …)` (`:171-172`);
  - `LocalTabBarVisible = false` and a fresh `LocalBottomChrome` (`:183-185`);
  - an opaque `fillMaxSize().background(colors.background)` (`:187-191`);
  - the cover card's `graphicsLayer` (`:190`).
- **`AppShell(sheetStrategy: SceneStrategy<AppNavKey>? = null)`** (`nav4/AppShell.kt:96`, `:200`). The app calls it
  without one (`wt/WP04/.../app/ui/AppRoot.kt:163`).

### 4.4 Defects and conflicts at the seam

| # | Sev | Finding | Where |
|---|---|---|---|
| N1 | S1 | **The state map does not survive a calculation.** With a sheet over the base, the second `calculateScene([base])` runs `states.clear()` (`SheetScene.kt:59`). With sheet B over sheet A, the call for `[base, A]` runs `retainAll({A})` and drops B, then creates a new state for A, because A's was cleared at the end of the previous calculation. On the next stack change, any sheet below the top gets a fresh `AppSheetState`. Its `LaunchedEffect(state)` (`AppSheet.kt:147`) runs `show()` again, so the sheet slides in from hidden again. The KDoc's promise (`SheetScene.kt:38-40`) does not hold. PeriFlow has the same pattern (`periflow/.../SheetScene.kt:33-40`) | [V] mechanism, [D] effect |
| N2 | S1 | **Unwired.** No call to `rememberSheetSceneStrategy` exists in any worktree. WP04's `AppRoot` passes none, so `Presentation.Sheet` keys are shown as covers (`PresentationHost.kt:76`; `handoff/WP04.md:304`). There is no template sheet destination either, so the `presentSheet()` journey, the restore scenario and the sheet recording have nothing to open (`handoff/WP04.md:473-475`) | [C] |
| N3 | S2 | **Two metadata shapes and three `SheetDetent` enums.** WP03: `SheetPresentation(Set)` under a typed key, default {Medium, Large}, enum in `designsystem` (`AppSheetState.kt:20`). WP04: `Presentation.Sheet(List)` under a string key, default [Large], enum in `core.navigation` (`Presentation.kt:11-24`). WP07 copies WP04's (`.../multiplatform/core/navigation/.../Presentation.kt:11-24`). WP08 defaults to [.medium, .large] (`AppSheet.swift:10`). WP03's planned fix: `handoff/WP03.md:469-474` | [C] |
| N4 | S2 | **`PresentedFrame` is a cover frame, applied to sheets.** (a) An opaque full-screen `colors.background` inside the sheet surface (`PresentationHost.kt:191`) defeats Frosted and Glass and paints over `surface`. (b) A fresh zero `LocalBottomChrome` (`:185`) shadows `AppSheet`'s footer inset. (c) `ReportMotion` reads the *underlying* scene's transition (§4.2), so `BackGate` never sees a sheet moving. A push or Back during a sheet's slide is neither held nor coalesced, and `NavGuard`'s push-during-push rule (`nav4/back/NavGuard.kt:15-24`) does not apply | [C] + [V] |
| N5 | S2 | **No navigation inside a sheet.** With a sheet up, `navigate(OrdinaryKey)` lands on the flat presented stack (`ShellState.kt:116`, `:178`). Its single-pane scene replaces the sheet's overlay: the sheet slides away (`onRemove`) while the new screen rises as a cover. MASTER-PLAN A3.7 promises "a nested `NavDisplay` inside its content (recipe in `docs/RECIPES.md`)" (`design/MASTER-PLAN.md:1025-1026`). No worktree has that recipe | [D] |
| N6 | S2 | **Rapid Back during a sheet dismiss: the plan and the build disagree.** MASTER-PLAN says "a sheet dismiss is idempotent and its host's gate counts the press" (`design/MASTER-PLAN.md:911`). WP03 consumes and drops the press instead (`handoff/WP03.md:397-398`, `SheetTest.kt:65-79`). The spec must pick one | [C] |
| N7 | S2 | **An in-screen sheet outlives its tab.** `HostedOverlay` is tied to composition, not to tab visibility (`AppOverlayHost.kt:81-100`). Kept tabs stay composed and only disable their dispatcher (`nav4/shell/tabs/KeptTabs.kt:34-47`, `:143-156`). A deep link (`ShellState.kt:203` changes `selectedIndex`) or Ctrl+N (`nav4/shell/ShellShortcuts.kt:22`) leaves the old tab's sheet, action sheet or alert drawn over the new tab, ignoring Back. VitFleet solved a related case with `LocalOverlaysHidden` (§11) | [D] |
| N8 | S2 | **Screen readers reach what is beneath a destination sheet or cover.** `HostedOverlay(modal)` clears the semantics beneath it (`AppOverlayHost.kt:58`), but nothing in WP04 clears the tab hosts beneath the presentation host. The only `clearAndSetSemantics` is for parked entries (`nav4/stack/StackLayout.kt:213`) | [D] |
| N9 | S3 | `BottomChromeWriter` puts a presented screen's bar ahead of the keyboard (`nav4/shell/BottomChromeWriter.kt:50-51`). A toast over a sheet with the keyboard up rests at 0 dp, behind the keys | [D] |
| N10 | S3 | **Card-stack seam.** `AppShell` takes `SceneStrategy<AppNavKey>?` (`AppShell.kt:96`), so it cannot read `largeFraction` without a cast. Sheets in the overlay host have no channel to the shell at all | [C] |
| N11 | S3 | **Merge seams in `Chrome.kt`.** The package moved (`foundation` → `foundation.layout`; `AppSheet.kt:59-60` imports the old one). `LocalTabVisible` is a `Boolean` in WP03 (`ds/foundation/Chrome.kt:42`) and a `TabVisibility` holder in WP04 (`.../foundation/layout/Chrome.kt:47-54`). WP04 adds `LocalTabBarVisible`, `LocalScreenBottomBar` and `LocalPaneContext` (`:62-89`) | [C] |

### 4.5 Back for sheets

- **Order on one dispatcher:**
  1. the sheet's `OverlayBackHandler` (`PRIORITY_OVERLAY`);
  2. the presentation host's `StackBackHandler`, registered after the tab hosts (`handoff/WP04.md:203-205`);
  3. the visible tab's stack;
  4. `ShellRootBack` (`nav4/back/ShellRootBack.kt:14-18`);
  5. the system, which plays back-to-home.

  An in-screen sheet registers on its tab's child dispatcher, so a hidden tab's sheet is deaf (by design; N7 is the
  flip side). Whether an overlay-priority handler on a child dispatcher outranks default-priority handlers on the
  parent is [U]; it was not checked in the `navigationevent` source.
- **Rules from the navigation-event skill**
  (`~/.claude/plugins/marketplaces/android-skills/navigation/navigation-event/SKILL.md`):
  - one `NavigationEventState` per handler (`:32`, `:240-266`);
  - a confirm-before-leaving flow is one handler that branches in `onBackCompleted` (`:255-266`);
  - prefer Navigation 3's own back handling where it is in use (`:39`).

  `OverlayBackHandler` is a raw `NavigationEventHandler`, which is justified for overlays.
- **Predictive back for a sheet:** use the MDC bottom container, not the Gmail card (SPEC §5.4,
  `research/predictive-back/SPEC.md:395-404`). The commit runs `dismiss`, and the cancel springs back on `sheet`.
  D6 lists what is missing.
- **Confirming before dismissal:** VitFleet enables its Back interception only while the form is dirty
  (`vitfleet/.../feature/trips/LoadFormScreen.kt:128`, `BackHandler(enabled = state.changed)`; commit `987a0c5`). An
  untouched form keeps the system's predictive back. The same rule fits a sheet's confirm hook: drag, scrim and Back
  all consult it only when the content says it is dirty.

### 4.6 IME and insets

- **Keyboard:**
  - The shell hides the bar under the keyboard in layout and follows `imeAnimationTarget` once settled
    (`handoff/WP04.md:206-215`; MOTION M22).
  - The edge-to-edge skill prefers `Modifier.fitInside(WindowInsetsRulers.Ime.current)` to `imePadding`, to avoid
    double padding and jank (`.../system/edge-to-edge/SKILL.md:108-112`). That is the fix for D2.
  - A sheet with fields needs "rise to the top detent on focus" (D4).
- **Bottom chrome inside a sheet:** chrome = max(footer, navigation bar), or the footer's height if the footer already
  pads them. The writer must run after layout settles, never per frame (D2). The tab bar is not under a sheet: it is
  covered by the scrim and `LocalTabBarVisible = false`.
- **Top chrome:** an `AppScaffold` inside a sheet provides its own `LocalTopChrome` (`ds/foundation/Chrome.kt:32-36`).
  The sheet consumes the status bar inset, so an `AppTopBar` inside it does not pad again (QA-19 note,
  `qa/r1/DEFECTS.md:43`).

### 4.7 Kept tabs

- Tabs stay composed and parked: alpha 0, off the window, semantics cleared, lifecycle capped at CREATED, dispatcher
  disabled (`KeptTabs.kt:34-47`, `:110-213`).
- A destination sheet lives in the presentation host, outside any tab, so it is safe.
- An in-screen sheet is drawn by its tab's composition, so it needs N7's fix:
  - either hide the overlay while `LocalTabVisible` is false, so it reappears when the tab returns;
  - or dismiss it on a tab switch.
- Ambient animation inside a sheet keys off `LocalTabVisible` like everything else.

### 4.8 Card stack (MOTION M6, QA-11): missing

- **Target** (`research/MOTION-PLAN.md:164-165`; `ios-motion-catalogue.md:45-47`, `:165`):
  - the presenter (content + bar) scales to **0.9165** about its top centre and moves down by status bar + 10 dp
    (≈ 7 % H);
  - its corners go to 12 dp, under a **10 %** overlay drawn as a rect (`#C8C8C8` in dark);
  - the medium detent gets the scrim only;
  - status bar icons turn light past 0.5, over a black window behind.
- **Implementation:**
  - one `graphicsLayer` on the shell layer in `AppShell`/`ShellChrome` (`nav4/AppShell.kt:131-199`), reading the
    fraction in the layer block (`handoff/WP04.md:562-564`);
  - zero recompositions of the shell content (`RECOMPOSITION-PLAN.md:193`).
- **Inputs today:** `AppSheetState.largeFraction` and `SheetSceneStrategy.largeFraction` (destination sheets only).
- **Needed:**
  - a typed host seam (N10);
  - an equivalent for overlay-host sheets;
  - the status bar appearance flip (app/edge-to-edge);
  - stacked sheets: the lower *sheet* also scales [E].
- **Glass:** scaling the shell also scales a `GlassStage` and its bar. Whether the sampling stays aligned under an
  ancestor scale is [U]. `SceneBackdrop` undoes only the sampling surface's own layer scale
  (`glass/SceneBackdrop.kt:89-102`).

---

## 5. Glass (WP05) and a sheet

- **Tiers** (`glass/GlassQuality.kt:24-48`, `GlassPolicy.kt:23-35`):
  - **Live** (API 33+, healthy device);
  - **LiveNoLens** (API 31-32, or heating);
  - **Frosted** (API < 31, low RAM, battery saver, a severe thermal state, or a GPU that falls behind);
  - **Solid** (Reduce transparency).

  Read through `LocalGlassQuality`. The level only steps down within a session.
- **Who may sample:** only floating chrome in `GlassStage`'s chrome slot samples the recorded scene
  (`glass/GlassStage.kt:17-55`). A sampling surface inside the recorded content fails loudly, because it would sample
  itself (Kyant #100, `glass/SceneBackdrop.kt:105-126`). `LocalGlassScene` is null outside the chrome slot
  (`GlassStage.kt:57-58`).
- **What WP05 says about sheets:**
  - Sheets are drawn glass, `Modifier.glassSurface(shape, colors, shadow)` with no sampling, in every tier but Solid
    (`GlassQuality.kt:20-22`, `glass/GlassSurface.kt:26-44`; `handoff/WP05.md:222-226`).
  - **The WP05 design says "do not add glass to sheets, dialogs or popups (drawn glass only there)"**
    (`design/wp/WP05.md:64`).
  - REQUIREMENTS 33 (later, so it wins per `notes/INDEX.md`) asks for "Solid, Frosted and Glass styles, with Glass
    through WP05's tiers".
- **Options for the spec:**
  1. **Frosted** = `glassSurface` with `GlassDefaults.frostedColors()` (`glass/GlassDefaults.kt:57-67`). It costs a
     few brush draws cached per size, so it is cheap in every tier.
  2. **Glass** = Live sampling **only at partial detents**. It turns opaque as the sheet reaches the large detent, then
     stops drawing the backdrop, as iOS 26 does ([A S17]; `ios-motion-catalogue.md:167`, `:243`). Glass falls back to
     Frosted below Live, and to the style's solid surface under Solid.
- **Rules a sampling sheet must follow:**
  - It must sit outside the recorded subtree. The overlay host (a sibling of the app root) and the presentation host
    (above the tab hosts) are both outside it if the stage wraps only the tab content [D]. WP05's shell seam is not
    built yet.
  - It needs the scene exposed to it, because `LocalGlassScene` today reaches only the chrome slot.
  - It needs a budget. A half-screen blur is far larger than the bar's, so it needs its own GPU gate, and the
    `watchGpuBudget` demotion (`GlassQuality.kt:96-108`) covers it.
  - **Never** use layer alpha around a sampling surface (`handoff/WP05.md:230`). The scrim and the content fade stay
    drawn rects or `ModulateAlpha`, and the predictive scale goes in `layerBlock` / `graphicsLayer`.
  - Kyant's `effects` must read no state (`glass/GlassChrome.kt:47-54`). Animate the tint through `onDrawSurface`.
  - A sheet in a separate window (`Dialog`) can never sample (#91). That is one more reason to keep sheets in-window.

---

## 6. Compose Multiplatform (WP07)

- **Exists:**
  - `core/navigation` `Presentation.kt`, a copy of WP04's, with its own `SheetDetent`
    (`.../multiplatform/core/navigation/src/commonMain/.../Presentation.kt:8-24`);
  - `NavGraph.entry(presentation = …)` (`NavGraph.kt:43-64`);
  - the token `sheet = Spring(341.51f, 1.0f)`, expressed as stiffness and damping (`DesignTokens.kt:100-119`). It
    feels the same as 0.34 s / 0, but the representation differs from Android's `Spring(response, bounce)`
    (`ds/theme/tokens/DesignTokens.kt:106`);
  - `layout.sheetMaxWidth` and `formSheetWidth` (`.../foundation/adaptive/AppLayout.kt:30-66`);
  - `shapes.radiusSheet` (`DesignTokens.kt:608-687`).
- **Missing:**
  - any overlay component: `component/` has button, container, feedback, input, list, navigation and text only;
  - an overlay host;
  - a presentation host. The shell is interim (`interim/StackHost.kt:30`) until WP07-B ports WP04's
    (`handoff/WP07-A.md:31-37`).
- **Library constraints:**
  - JetBrains `navigation3-ui` 1.1.2 is androidx 1.1.7 (`.../multiplatform/gradle/libs.versions.toml:15-20`).
  - `OverlayScene.onRemove` exists there [V].
  - 1.2.0's `rememberNavigationEventState(SceneState)` does not, so it is rebuilt from `navigationevent`
    (`handoff/WP07-A.md:39-42`). Mid-flight predictive back is absent.
- **Platform notes for a common sheet:**
  - iOS has no system Back, so the sheet closes by drag, scrim or its close button.
  - Haptics need `expect`/`actual`.
  - Kyant backdrop is Android-only, so the Glass style on iOS is drawn (Frosted) unless a `UIVisualEffectView` interop
    is added [D].
  - IME insets on the iOS target: CMP support for `WindowInsets.ime` is [U].

---

## 7. SwiftUI (WP08)

- **`appSheet`** (`AppSheet.swift:3-49`):
  - It wraps `.sheet(isPresented:)` and `.sheet(item:)`, with `.presentationDetents(detents)` (default
    [.medium, .large]) and `.presentationDragIndicator(.visible)`.
  - Before iOS 26 it sets `.presentationCornerRadius(theme.shapes.radiusSheet)`; iOS 26 uses the system's concentric
    corners (`:37-41`).
  - The card stack comes from the system. The token is `springs.sheet` (`.../Theme/AppSprings.swift:26`).
- **Other sheet code:** `SampleDetailView.swift:33-38` uses `.inspector`, which is a sheet on a phone, with raw
  `.presentationDetents([.medium, .large])`. That is legitimate system behaviour, but it bypasses `appSheet`. The
  SwiftUI navigation layer has no presentation router.
- **Not exposed** (all available on iOS 17, the template's minimum; [A], URLs not fetched):
  - a detent selection binding: `presentationDetents(_:selection:)`
    (https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:selection:));
  - custom detents: `.height`, `.fraction`, `CustomPresentationDetent`;
  - background interaction: `presentationBackgroundInteraction(.enabled(upThrough:))`
    (https://developer.apple.com/documentation/swiftui/view/presentationbackgroundinteraction(_:));
  - `presentationContentInteraction(.scrolls)`;
  - `presentationBackground(_:)`;
  - `interactiveDismissDisabled(_:)` (https://developer.apple.com/documentation/swiftui/view/interactivedismissdisabled(_:));
  - `presentationCompactAdaptation`;
  - iOS 18 `presentationSizing(.form/.page/.fitted)`, behind `#available`.
- **Confirm before dismissal:** SwiftUI has no attempt-to-dismiss callback. Use `interactiveDismissDisabled(isDirty)`
  plus a Cancel action with `confirmationDialog`, or bridge UIKit's `presentationControllerDidAttemptToDismiss`
  (https://developer.apple.com/documentation/uikit/uiadaptivepresentationcontrollerdelegate/presentationcontrollerdidattempttodismiss(_:)).

---

## 8. Plans, decisions and QA items that bind the sheet

| Source | Item | State |
|---|---|---|
| MOTION-PLAN M1 (`:82-97`) | Sheet variant of predictive back: the MDC bottom container | Built, with D6 |
| MOTION-PLAN M6 (`:154-179`) | Detents by projection, rubber band, present on `sheet`, card stack, scrim as a rect. Dismiss at ≥ 2 H/s or past 52 % (iOS [E]) | Done except the card stack. Projection replaces the 2 H/s / 52 % rule; the spec should confirm that equivalence |
| MOTION-PLAN M16 / M17 (`:307-329`) | Alert and action sheet on `dialog`, scrims as rects | Action sheet done |
| MOTION-PLAN M22 (`:397-408`) | Keyboard-synced motion for fields and sheets: read the IME in layout only | D2 and D4 |
| MOTION-PLAN M26 (`:474-484`) | Glass morph on `sheet` | For a glass sheet that morphs out of a button (iOS 26) |
| MOTION-PLAN §1 rules (`:49-62`) | Never a tween after a finger; project then snap; retargets keep velocity | D7 |
| SMOOTHNESS S8 (`:63`, `:366-378`) | No full-screen offscreen fade | Done for sheets |
| SMOOTHNESS S10 (`handoff/WP03.md:569`) | `preferredFrameRate(High)` while a sheet is dragged | Missing |
| RECOMPOSITION R7 (`:45`), journeys (`:192-193`), budget (`:220`) | 0 recompositions per drag move; ≤ 1 per detent; shell content 0 under the card stack | Drag done; card stack has no test yet |
| ADAPTIVE AD-04 (`:85`, `:434-435`), §3.5 (`:247`), tabletop (`:482`), Esc (`:565`) | Form sheet by class; popover action sheet; Compact height full height; fold detent; explicit Esc | D11, D16 |
| SPEC §2 (`:79`), §5.4 (`:395-404`) | Bottom-container constants; commit = dismiss; cancel on `sheet` | D6 (easing, counter-scale) |
| CP1 (`decisions/CP1.md:40`, `:137-138`) | `sheet` 0.34 s / 0 kept; `dialog` 0.275 s added | Done |
| QA-11 (`qa/r1/DEFECTS.md:35`) | No card stack | Open (WP04 + WP03) |
| QA-13 (`:37`) | Scrim stops short of the status bar (old `Dialog` sheet) | Likely fixed by the overlay host; verify by screenshot |
| QA-14 (`:38`) | Language sheet: no checkmark, no grabber, no separators, full-width tinted Cancel | Grabber done; checkmark missing (A2) |
| QA-17 (`:41`) | Sheet content at x = 0 | Resolved as "edge to edge, like a screen" (`a8333b0`) |
| QA-19 (`:43`) | `AppTopBar` pads the status bar where it is not at the window top | Sheets consume the inset |
| MASTER-PLAN A3.7 (`design/MASTER-PLAN.md:1018-1026`), table row (`:911`) | Measure once, footer, rubber band, nested `NavDisplay` recipe, gate counts Back during dismiss | Recipe missing (N5); N6 conflict |

---

## 9. Seams a new sheet component must use

1. **Two hosts, one component.**
   - *Destination* sheets: the Navigation 3 `OverlayScene` in WP04's presentation host (layer 3; above the bar, below
     alerts and toasts).
   - *In-screen* sheets: `HostedOverlay` in WP03's overlay host (layer 4) (`AppOverlayHost.kt:20-33`).
   - Both must share one state type and one surface. The card stack, the bottom-chrome rules, kept-tab behaviour (N7)
     and modal semantics (N8) must behave the same in both, or the spec must say why not.
2. **Metadata.**
   - One typed `NavMetadataKey`, as in the recipe, carried through `entry<K>(presentation = Presentation.Sheet(…))`
     (`nav4/NavGraph.kt:50-57`) and `registry.presentationOf`.
   - One `SheetDetent` type, living in `designsystem` and re-exported, shared by WP04 and WP07. Delete the duplicates
     (N3).
3. **Scene strategy state.** Do not keep per-sheet state in a map that `calculateScene` clears: it is called once per
   overlay level on every calculation ([V], N1). Key the state by `contentKey` and remove it only in `onRemove` or when
   the entry leaves the back stack. Alternatively, hold it in the entry's composition through a `NavEntryDecorator`,
   which also gives it a `Saver`.
4. **`OverlayScene` contract.**
   - `onRemove()` animates out.
   - The lifecycle is RESUMED or STARTED, supplied by NavDisplay 1.2.0.
   - `LocalNavAnimatedContentScope` is the underlying scene's scope; never use it for the sheet's own motion.
   - Put the sheet strategy before the single-pane fallback (`bottomsheet.md:157`).
5. **A presented-sheet frame.**
   - It replaces `PresentedFrame` for sheet entries: no opaque background, and the sheet's own bottom chrome.
   - It reports the *sheet's* motion to `presented.gate` through `BackGate.report(…)` (`nav4/back/BackGate.kt:67-74`),
     so `NavGuard` and coalescing work during the show and the dismiss (N4).
6. **Back.**
   - Keep `OverlayBackHandler` at `PRIORITY_OVERLAY` on the caller's dispatcher.
   - Ease the progress, and counter-scale the content (D6).
   - The confirm hook is one handler that branches, and it is enabled only when the content is dirty
     (navigation-event skill; VitFleet).
   - Decide N6.
7. **Gesture state.**
   - Velocity-preserving retargets: one `Animatable`, or a velocity carried through `animate` (D7).
   - Every input path must stop a running settle, nested scroll included (D8).
   - No `animateTo` while dismissing (D9).
   - Re-target an in-flight animation when the anchors change (D10).
8. **IME.**
   - Read the IME in layout only (`fitInside(WindowInsetsRulers.Ime)`).
   - Rise to the top detent on focus.
   - Never count the IME in both the content padding and the footer (D2).
   - Fall back on API 26-29 per M22.
9. **Chrome locals.**
   - Inside a sheet: `LocalBottomChrome` = footer ∪ navigation bar, settled values only (D3); `LocalTabBarVisible =
     false`.
   - A screen inside a sheet gets its own `LocalTopChrome`.
   - `BottomChromeWriter` gives the keyboard precedence over a presented bar (N9).
   - Reconcile `Chrome.kt` at the WP03/WP04 merge (N11).
10. **Kept tabs.** In-screen overlays follow `LocalTabVisible` (N7).
11. **Card stack.**
    - A typed fraction provider from both hosts (N10).
    - One `graphicsLayer` on the shell layer.
    - An overlay rect, not alpha.
    - The status bar flip.
    - The lower sheet's transform when sheets are stacked.
12. **Glass.**
    - `LocalGlassQuality` chooses Live, Frosted or Solid.
    - A Live sheet needs WP05 to expose the scene outside the chrome slot, a GPU gate, and opacity at the large detent
      (§5).
    - Drawn glass is `Modifier.glassSurface`.
13. **Tokens.**
    - `springs.sheet` / `dialog`, `rubberBand`, `shapes.sheet` / `xl`, `layout.sheetMaxWidth` / `formSheetWidth`,
      `colors.scrim` / `surface`.
    - Add the sheet geometry to `tokens/motion.json`, so WP01's generator writes it into Android `DesignTokens.kt`,
      WP07's `DesignTokens.kt` and WP08's tokens: detent fractions, top gap, grabber, predictive constants, form
      sizes, card-stack constants, iOS 26 inset sizes.
14. **Adaptive.**
    - Read `AppTheme.containerClass`, `posture` and `windowSize` (`AppSheet.kt:133-142`).
    - Form sheet (D11), side sheet (new), Compact height, fold.
15. **Accessibility.**
    - Semantics of the grabber and the scrim, and a traversal group (D12).
    - `paneTitle` always set.
    - Modal clearing at both hosts (N8).
    - Esc (D16).
16. **Perf and benchmarks.**
    - A template sheet destination with a tagged trigger, for `presentSheet()`, `restore` and the sheet recording
      (`handoff/WP04.md:473-475`).
    - The `sheet_grabber` test tag is in the resource-id contract (`handoff/WP04.md:245`).
    - Frame-rate votes while dragging (S10).

---

## 10. Package ownership

From REQUIREMENTS 33 (`REQUIREMENTS.md:139`), mapped to the files that exist:

| Package | Owns for the sheet |
|---|---|
| **WP03** | `AppSheet`, `AppSheetState`, detents, styles, slots and tokens (`ds/component/overlay/`, `tokens/motion.json`); `AppActionSheet`; `OverlayBackHandler`; `AppOverlayHost`/`HostedOverlay` (layers 4-5); catalog demos; its `SheetScene.kt` in `core/navigation` (WP03's design gives it that file and one line in `AppRoot`, `design/wp/WP03.md:56`, `:66`, `:72`); DS tests and recomposition journeys |
| **WP04** | `Presentation`/`NavGraph` metadata, `ShellState` routing, `PresentationHost` and a sheet frame, `BackGate`/`NavGuard`/`StackBackHandler`, `BottomChromeWriter`, `KeptTabs` behaviour, the card-stack transform in `AppShell`/`ShellChrome`, semantics clearing under presentations, perf journeys (`tools/perf`), baseline-profile steps |
| **WP05** | The Glass style: tier mapping, scene access outside the chrome slot, GPU budget, opacity at large; `glassSurface` for Frosted. Its design's "no glass on sheets" rule must be revised (§5) |
| **WP07-B** | A common-code sheet over the shared tokens and a common presentation host on navigation3 1.1.x; the iOS target without a back gesture; a drawn Glass style |
| **WP08** | `appSheet` options over the native presentation API (§7), with the same token names |
| **WP02-M3** | Feature call sites: a sample sheet destination (WP04 needs one), the language picker as a choice sheet (QA-14) |
| **QA2** | Phone verification and recordings: medium/large drag, flick, rubber band, predictive back (commit and cancel), IME, card stack, glass tiers |
| **WP12** | Docs: rules, recipes (nested `NavDisplay` in a sheet, N5), do/don't |

---

## 11. Ideas from PeriFlow and VitFleet worth keeping

| Idea | Source | Why it matters | Do not copy |
|---|---|---|---|
| **`LocalSheetSurface`**: content knows it is in a sheet. `AppTopBar` shows the close button instead of Back, and `AppScaffold` inherits the sheet's surface instead of painting its own | `periflow/.../component/container/AppScaffold.kt:46`, `:62`; `.../navigation/AppTopBar.kt:139`; provided at `AppDetentSheet.kt:265`, `:317` | Lets a full screen be presented as a sheet unchanged, and solves N4(a) at the source | — |
| **Dialog-style form sheet with scale-in** (0.94 → 1 plus a 24 dp rise) | `periflow/.../overlay/AppDetentSheet.kt:273-322`, `:333-336` | Matches ADAPTIVE-PLAN:434 better than WP03's slide-up (D11) | Its layer-alpha scrim (`:292`), its own non-token spring (`:285`) |
| **Fit-content sheet**: wrap the content's height, scroll under a fixed header when taller than the window | VitFleet `overlay/AppBottomSheet.kt:230-241`; PeriFlow `AppBottomSheet.kt:198-204` | The missing fit-content detent, with "Save stays reachable" built in | `Dialog` windows (no glass, wrong layering), the offset read in composition (VitFleet `:149-158`), tween dismiss (`:96-99`), threshold dismiss (`:113-114`) |
| **IME-correct sheet**: edge to edge; the surface pads `safeDrawing.bottom`, so the sheet stops at the keyboard instead of the window panning | VitFleet `AppBottomSheet.kt:76-79`, `:186-188` | The behaviour D4 asks for | — |
| **Content drag returns the sheet to rest before scrolling** | VitFleet `AppBottomSheet.kt:121-136` | The same hand-off for a fit-content sheet | `snapTo` inside `launch` on every delta (`:104-109`) |
| **Choice sheet**: single select with a radio role, checkmark, optional "none" row, search from 6 items, height capped at 60 % | VitFleet `core/ui/fleet/ChoiceSheet.kt:34-114` | Closes QA-14; a preset the language picker and pickers need | — |
| **State sheet**: loading skeleton, empty, error with retry, capped at 70 % | VitFleet `core/ui/history/HistorySheet.kt:46-79`, `:116` | A common content pattern for a "details" sheet | — |
| **`LocalOverlaysHidden`**: overlays are not shown while something covers the whole app | VitFleet `overlay/OverlayCover.kt:5-11`; `app/ui/AppRoot.kt:77` | The shape of the N7 fix (hide while not visible, restore after) | — |
| **Confirm only when dirty** | VitFleet `feature/trips/LoadFormScreen.kt:128`; commit `987a0c5` | The confirm hook's rule: keeps the system's predictive back for clean content | — |
| **Animated close button**: a glass circle with a self-drawing cross and a quarter turn on press | `periflow/.../overlay/AppSheetCloseButton.kt:33-100` | A sheet-wide close affordance; reads deferred values in layer and draw | Its private springs (`:93-100`). Keep it optional and on tokens; decorative motion is a taste call |
| **Optional grabber and outside dismissal** (`showHandle`, `dismissOnClickOutside`) | PeriFlow `AppBottomSheet.kt:53-55`; VitFleet `:64-66` | Two of the knobs REQUIREMENTS 33's "customisable" implies | — |
| **Sheet destinations in real use** (`NavTransitionStyle.Sheet` keys) | PeriFlow `feature/log/LogNav.kt:24-25` | Proves the destination path. The template has no sheet destination yet (N2) | PeriFlow's `SheetScene` has the N1 map pattern and a legacy `BackHandler` (`SheetScene.kt:30-42`, `:60-65`) |

---

## 12. Decisions the spec has to make

1. **Hosts:** one shared component and state for destination sheets (presentation host) and in-screen sheets (overlay
   host), or a single host. Either way, the card stack, Back, chrome, kept tabs and semantics must behave the same.
2. **Back burst during a dismiss:** drop it (WP03, tested) or count it in the host's gate (MASTER-PLAN:911) (N6).
3. **Glass:** Live sampling for sheets at partial detents only, opaque at large (overrides WP05's design), or drawn
   glass only, with Frosted standing in as "Glass".
4. **Default detents:** [Large] (iOS, WP04, WP07) or {Medium, Large} (WP03, WP08).
5. **Detent model:** an enum or a sealed type (`Fraction`, `Height`, `FitContent`, `Custom(resolver)`, plus
   `Medium`/`Large` as presets); how fit-content coexists with "measure once at the large height".
6. **Large-window presentation:** a form sheet that scales in (ADAPTIVE) or slides in, and when a side sheet replaces
   it.
7. **Navigation inside a sheet:** a nested `NavDisplay` (MASTER-PLAN) or a stack in the presentation host (N5).
8. **Scroll container:** whether the sheet provides one (VitFleet) or requires the content to bring its own (WP03
   today).
9. **Predictive back for a sheet:** MDC bottom container (SPEC §5.4) with eased progress and content counter-scale.
   Keep that recommendation.
10. **Token home:** sheet geometry in `tokens/motion.json`, or a new `sheet` section that the generator writes to all
    three targets.

---

## 13. Sources

- Code read in full:
  - WP03: `AppSheet.kt`, `AppSheetState.kt`, `AppActionSheet.kt`, `OverlayBackHandler.kt`, `AppOverlayHost.kt`,
    `SheetTest.kt`, `SheetScene.kt`, `Chrome.kt`;
  - WP04: `Presentation.kt`, `PresentationHost.kt`, `PresentationCard.kt`, `AppShell.kt`, `NavGraph.kt`,
    `BackGate.kt`, `NavGuard.kt`, `GateSettle.kt`, `ShellRootBack.kt`, `StackBackHandler.kt`, `KeptTabs.kt`,
    `BottomChromeWriter.kt`, `layout/Chrome.kt`, legacy `AppBottomSheet.kt`;
  - WP05: `GlassPolicy.kt`, `GlassQuality.kt`, `GlassSurface.kt`, `GlassStage.kt`, `GlassChrome.kt`, `GlassColors.kt`,
    `GlassDefaults.kt`, `SceneBackdrop.kt`;
  - WP07: `Presentation.kt`;
  - WP08: `AppSheet.swift`;
  - PeriFlow: `SheetScene.kt`, `AppDetentSheet.kt`, `AppBottomSheet.kt`, `AppSheetCloseButton.kt`;
  - VitFleet: `AppBottomSheet.kt`, `AppActionSheet.kt`, `OverlayCover.kt`, `ChoiceSheet.kt`, `HistorySheet.kt`.
- Code read in part:
  - WP03 tokens and `AppMotion.kt`, `RecompositionJourneysTest.kt`, `PostureTest.kt`, `CatalogOverlays.kt`,
    `Settings.kt`, `AppBottomActionBar.kt`, `AppSection.kt`;
  - WP04 `ShellState.kt`.
- Plans:
  - `research/MOTION-PLAN.md` (§1, M1, M6, M16, M17, M22, M26, §5, §6), `SMOOTHNESS-PLAN.md`,
    `RECOMPOSITION-PLAN.md`, `ADAPTIVE-PLAN.md`, `ios-motion-catalogue.md`, `predictive-back/SPEC.md`;
  - `qa/r1/DEFECTS.md`, `design/wp/WP03.md`, `WP04.md`, `WP05.md`, `design/MASTER-PLAN.md` (A3.7, A3.9 table);
  - `handoff/WP03.md`, `WP04.md`, `WP05.md`, `WP07-A.md`, `WP08.md`, `decisions/CP1.md`.
- Android skills: `navigation/navigation-3/references/.../recipes/bottomsheet.md`, `navigation/navigation-event/SKILL.md`,
  `system/edge-to-edge/SKILL.md` (under `~/.claude/plugins/marketplaces/android-skills/`). Recipe upstream:
  https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/bottomsheet
- Bytecode: `androidx.navigation3:navigation3-ui-android:1.2.0` and `1.1.7` AARs (`OverlayScene`, `SceneStateKt`,
  `NavDisplayKt__NavDisplayKt`, `LocalNavAnimatedContentScopeKt`), extracted to the session scratchpad.
- Material 3 reference sources: `research/sheet/sources/` (`BottomSheet.kt`, `ModalBottomSheet.kt`, `Scrim.kt`,
  `SheetDefaults.kt`).
