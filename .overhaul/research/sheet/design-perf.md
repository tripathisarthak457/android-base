# Bottom sheet system: design spec (performance and robustness first)

Written 2026-10-12 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). This is one of the competing designs for
`research/SHEET-SPEC.md`. It starts from the 8.33 ms frame budget of the Redmi Note 10 Pro at 120 Hz and from the
ways the current sheets break. The customisation surface is built on top of that.

Nothing was changed in any repository or worktree. No Gradle or adb was run.

**Inputs:**

- `research/sheet/current-state.md` (cited as **CS §n**) and `research/sheet/research.md` (**R §n**);
- `research/CODE-STANDARDS.md`, `MOTION-PLAN.md` (M1, M6, M17, M22, M26), `decisions/CP1.md`;
- SMOOTHNESS S3, S8 and S10; RECOMPOSITION-PLAN R7 and §3; ADAPTIVE-PLAN AD-04, §3.5, §6 and §11.5;
- `predictive-back/SPEC.md` §2 and §5.4; MASTER-PLAN §2, A3.7, the A3.9 table and the ownership table.

The code was re-read at these heads: WP03 `718c1e7`, WP04 `489dc57`, WP05 (untracked glass files), WP07 `ec15ef4` and
WP08 `5cbcd9b`.

**Tags**, as in research.md:

- **[C]** a code fact, cited `file:line`;
- **[V]** verified this session in library sources or bytecode, with the path;
- **[A]** Apple; **[G]** Google;
- **[M]** measured by a third party (Flutter's iOS 18 constants);
- **[E]** measured earlier in this project;
- **[D]** derived here;
- **[U]** unverified: measure it before relying on it.

**Path prefixes:**

- `ds/` = `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`
- `nav4/` = `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/`
- `ds4/` = the same design-system path in WP04
- `glass/` = `wt/WP05/.../core/designsystem/.../foundation/glass/`
- `mat/` = `research/sheet/sources/`

Library sources read this session come from `~/.gradle/caches/modules-2/files-2.1/`: `ui-android-1.12.1-sources.jar`
(**ui**), `foundation-layout-android-1.12.1-sources.jar` (**fl**), `navigationevent-android-1.1.2-sources.jar` (**ne**),
and the 1.2.0 `navigationevent.aar` bytecode (**ne120**). The template pins navigationevent 1.2.0
(`wt/WP04/templates/android/gradle/libs.versions.toml:25`).

---

## 0. Decisions at a glance

| # | Question | Decision | Why |
|---|---|---|---|
| 1 | One host or two (CS §12.1) | One component and one state. A sheet draws in the nearest **sheet layer** (§9.1): the shell's tab layer (above the bar, below presentations), one layer per presented level, and a root layer that `AppTheme` installs. Destination sheets stay Navigation 3 `OverlayScene`s in WP04's presentation host. The overlay host (layer 4) keeps alerts, menus, the HUD and toasts | One rule fixes three bugs: an in-screen sheet drawn over a later presentation, N7 (a sheet outliving its tab) and N8 (screen readers reaching beneath) |
| 2 | Back pressed during a dismiss (N6) | **The host's `BackGate` counts it** (MASTER-PLAN:911). A sheet reports its motion to its host's gate through `HostMotion` and disables its own handler while dismissing. Further presses reach the host's `StackBackHandler`, which counts them, and they pop as one change once the sheet is hidden | Screens and sheets share one coalescing rule. No press is lost, and none is applied in the middle of an animation |
| 3 | Glass (CS §5) | `SheetStyle.Glass` samples the scene only in the `Live` and `LiveNoLens` tiers and only at partial detents. It ramps to opaque toward the large detent and stops sampling once attached. Below `LiveNoLens` it is drawn Frosted, and it is Solid under Reduce transparency. A `sheet_glass` GPU row gates it. This revises `design/wp/WP05.md:64` | REQUIREMENTS 33 is later than WP05's design, so it wins (`notes/INDEX.md`). iOS 26 does the same [A]. The gate decides per device |
| 4 | Default detents | `[SheetDetent.Large]` | The UIKit and SwiftUI default [A]; WP04 and WP07 already default to it. The card stack is the iOS sheet's signature |
| 5 | Detent model | A sealed `SheetDetent`: `Large`, `Medium`, `FitContent`, `Fraction`, `Height`, and `Custom(key, resolver → Dp?)`, where `null` means inactive | UIKit's model (R §1.3); fit-content as in PeriFlow and VitFleet (CS §11) |
| 6 | Large windows | `SheetPlacement.Automatic` (§7): a modal sheet on a Medium×Medium-or-larger window is a centred form sheet that scales in on `dialog`; a non-modal sheet from Expanded width is a side sheet at the start edge; elsewhere the sheet comes from the bottom. `Bottom`, `Center`, `Start` and `End` can be set explicitly | ADAPTIVE-PLAN:434, iOS 27 `presentationPlacement` [A], the Material side sheet [G] |
| 7 | Navigation inside a sheet (N5) | Pushing an ordinary key from a presented sheet closes the presented sheets, then pushes on the tab, queued on the gate. A sheet that needs its own stack hosts a nested `NavDisplay` (WP04 helper `SheetNavHost`, plus a recipe) | iOS's "close the sheet, then navigate" (HIG [A]); MASTER-PLAN A3.7 |
| 8 | Scroll container (CS §12.8) | The sheet adds none. The content scope provides `contentPadding`; `SheetScrollColumn` covers non-lazy content | A `verticalScroll` around a lazy list throws. Content must own its scroller |
| 9 | Predictive back | MDC's bottom container (48 dp across, 24 dp down), with progress eased by (0.1, 0.1, 0, 1) and the content counter-scaled. Progress is read only in layer blocks. Commit dismisses, so one gesture closes the sheet; `StepDown` is an option | SPEC §5.4; `mat/BottomSheet.kt:443-487`. M3 reads progress in composition (`mat/BottomSheet.kt:168`); not copied |
| 10 | Where the tokens live | A new `sheet` section in `tokens/motion.json`, emitted to Android, CMP and Swift | WP07 and WP08 need the same geometry (CS §2.3) |
| 11 | Gesture primitive | foundation's `AnchoredDraggableState`, with a custom `FlingBehavior` (projection, 2 H/s, the 0.52 position rule) and a `SheetOverPull` `OverscrollEffect` for the rubber band | CODE-STANDARDS M-3 (MUST). `updateAnchors` retargets a running animation (R §2.6), and `startDragImmediately` lets a touch catch a moving sheet |
| 12 | IME | The sheet reads `WindowInsetsRulers.Ime` in its own placement block. The sheet and footer ride the keyboard (L1), and the content padding changes only at the start and end of an IME animation. No `fitInside` on the content container | `fitInside` re-measures its child with fixed constraints on every IME frame ([V] fl `RulerAlignment.kt:36-50`) |
| 13 | Dismiss contract | `onDismissRequest` fires as soon as the user acts, while the sheet is already sliding out with the release velocity. The owner then removes it: `visible = false`, or a pop whose `onRemove()` awaits the slide | In-screen and destination sheets share one contract, and no velocity is lost waiting for the caller |
| 14 | Dim | What sits directly beneath a modal sheet draws its dim, inside its own layer, merged with the card-stack overlay: the shell presenter, or the lower sheet. The sheet's scrim node draws only when nothing beneath claims the dim | This saves one full-screen blended rect per frame during the card stack |
| 15 | Floating (the iOS 26 inset) | On by default for Frosted and Glass, off for Solid, and only with gesture navigation. A rounded clip with one radius, drawn only while the sheet is not attached | §6.2: cost and 3-button legibility |
| 16 | Shadow | None on modal sheets. Non-modal and floating sheets draw a shadow outside the shape only, cached per size | REQUIREMENTS 29: the shadow seen through a translucent body is the pull-to-refresh black-ring bug |
| 17 | Card-stack offset | The presenter moves down to `largeTop − peek`, with a 10 dp peek, which is the safe-drawing top. **Deviation** from MOTION-PLAN M6's "status bar + 10 dp" | That keeps iOS's strip of about 0.01 H above the sheet (0.07 H against 0.08 H [M]). M6's value would hide the presenter's top edge |
| 18 | Medium detent | 0.5 of the window height (was 55 %, `ds/component/overlay/AppSheet.kt:335`) | iOS medium is "approximately half" [A]; M3 uses 50 % [G] |
| 19 | Destination sheet state | Created by a `NavEntryDecorator` inside the saveable-state holder, keyed by `contentKey`. The strategy holds no state | Fixes N1 and restores the detent after process death |
| 20 | Action sheet on large windows | An in-window card anchored in the sheet layer, instead of `AppMenu`'s `Popup` window (`ds/component/overlay/AppMenu.kt:84`) | No window creation when it opens; glass can sample it |

---

## 1. Budgets

The frame budget is 8.33 ms at 120 Hz. Cost classes follow MOTION-PLAN §2:

- **L0**: layer properties only;
- **L1**: re-placement;
- **D1**: re-recording a node of card size;
- **D2**: one full-screen blended rect.

Recomposition budgets follow RECOMPOSITION-PLAN §3 (`RECOMPOSITION-PLAN.md:192-193`, `:220`).

| Phase | Allowed per frame | Recompositions | Notes |
|---|---|---|---|
| Drag (finger down) | L1 sheet + D2 dim + L0 presenter | **0 per move** | offset read in placement only |
| Settle / present / dismiss animation | same | **0 in frames 2…N−1**; ≤ 1 at the settle (if `currentDetent` is read) | spring seeded with velocity |
| Predictive back progress | L0 (surface scale, content counter-scale) | **0 per event** | `gates.toml` already has `recompose.back_gesture_progress.max = 0` for screens |
| IME show/hide | L1 (sheet lift, footer) + content re-measure **≤ 2 per IME transition** | ≤ 1 (start), ≤ 1 (end) for content that reads padding in composition; 0 for content that uses `contentPadding` | no per-frame re-measure of content |
| Card stack (presenter) | L0 + D2 (merged dim) + 3 opaque strips | **0** for the shell content | `RECOMPOSITION-PLAN.md:193` |
| Non-modal boundary crossing | D2 dim fade | ≤ 1 per crossing (semantics flag) | pointer pass-through needs none |
| Floating ↔ attached | D1 clip re-record of one node, L0 | 0 | only while not attached |
| Glass at partial detent | backdrop draw of the visible card | 0 | Live tier only, under its own gate |
| Opening (composing frame) | compose content + measure + place at hidden | 1 (the opening) | the clock starts the frame after (§10.3) |

**Forbidden**, from MOTION-PLAN §2 and A11.3, applied to sheets:

- `graphicsLayer { alpha }` on anything larger than a card;
- `Modifier.offset(dp)`, or `padding` driven by the offset;
- `BoxWithConstraints` or `SubcomposeLayout` in the sheet;
- a `Dialog` or `Popup` window;
- reading `offset`, `largeFraction`, `presentationFraction` or back progress in composition;
- per-frame `Brush` or `Shape` allocation;
- `fitInside` or `imePadding` on the content container;
- a tween after a finger.

---

## 2. Architecture

```
                        AppSheetState  (outside composition, @Stable)
   ┌──────────────────────────┼─────────────────────────────────────────────┐
   │ AnchoredDraggableState<SheetValue>.offset  (@FrequentlyChangingValue)   │
   │ SheetOverPull.amount       backProgress      imeLift (from rulers)       │
   └──────────────┬───────────────┬───────────────┬───────────────────────────┘
                  │ placement     │ layer block   │ draw
   SheetLayoutNode.place(..)   surface layer    SheetDimNode / presenter dim
   (sheet, header, footer)     (back scale,     (alpha = presentationFraction)
                               counter-scale)   SheetSurfaceNode (floating clip,
                                                 glass ramp)
                  │
   SheetStack (window-level, ordered)  ── largeFraction of the sheet above ──▶
        Modifier.sheetPresenter() on the shell  (layer: scale, translate, clip; draw: dim, strips)
        lower sheet's own layer               (covered scale and lift)
                  │
   currentDetent (derivedStateOf, settled) ── composition ──▶ grabber state text (≤ 1 per settle)
```

**Where each fast value is read** (the contract every reviewer checks):

| Value | Written by | Read in | Class |
|---|---|---|---|
| `offset` | drag, fling, `animateTo`, `updateAnchors` | `SheetLayoutNode` placement; `SheetDimNode` draw; `SheetStack` fractions (presenter layer block, lower sheet layer block) | L1, D2, L0 |
| `overPull.amount` | `SheetOverPull.applyToScroll/applyToFling` | placement (`top = offset − band(amount)`) | L1 |
| `backProgress` | `SheetBackHandler` | surface `placeWithLayer` block; content counter-scale layer block | L0 |
| `imeLift`, footer y | nothing writes them: read from `WindowInsetsRulers.Ime.current` | placement | L1 |
| IME target (content padding) | placement writes a `mutableIntStateOf` only when an IME animation starts or ends | `SheetContentScope.contentPadding.calculateBottomPadding()`, which a lazy list reads in measure | ≤ 2 re-measures per IME transition |
| `attach` (floating → attached) | derived from offset | `SheetSurfaceNode` draw (clip, glass ramp), layer `shape` only when it crosses 1 | D1 |
| `presentationFraction`, `largeFraction` | derived from offset | draw and layer blocks only (`@FrequentlyChangingValue`) | — |
| `currentDetent` | `derivedStateOf` over the settled value | composition (grabber `stateDescription`, caller UI) | ≤ 1 per settle |
| `isMoving` | the drag/animation mutex | `snapshotFlow` in an effect: `HostMotion.report`; frame-rate vote in the dim node | 0 |

---

## 3. Public API (Android)

All in `:core:designsystem`, package `com.base.app.core.designsystem.component.sheet` (layout in §11). KDoc below is the
intended text, short form; CODE-STANDARDS C-4/C-5 apply (no `@param modifier`, no restating names).

### 3.1 Detents

```kotlin
/**
 * A height a sheet can rest at. A sheet has one or more; people move between them by dragging,
 * flicking, tapping the grabber, or with the grabber's accessibility actions.
 *
 * List detents lowest first. They resolve against [SheetDetentScope] when the window, the fold or
 * the content's height changes, never during a drag. A detent that resolves to `null` is inactive in
 * that window, as iOS's medium detent is at Compact height; when every detent is inactive the sheet
 * uses [Large].
 */
@Immutable
sealed interface SheetDetent {
    /** Nearly the whole window: the top sits [AppSheetDefaults.LargeTopGap] below the safe-drawing top. */
    data object Large : SheetDetent

    /** Half the window's height. Inactive when the window's height class is Compact. */
    data object Medium : SheetDetent

    /**
     * The content's own height, header included, capped at [Large]. The sheet measures its content
     * with a height limit instead of a fixed height, so content that fills the height it is given
     * (a lazy list, `fillMaxHeight`) resolves to [Large].
     */
    data object FitContent : SheetDetent

    /** [fraction] of the window's height, measured from the bottom; in (0, 1]. */
    @JvmInline value class Fraction(val fraction: Float) : SheetDetent

    /** A fixed visible height, such as a peek of a search field and a row over a map. */
    data class Height(val height: Dp) : SheetDetent

    /**
     * A height computed from [SheetDetentScope], or `null` to make it inactive there. [key] names it
     * across process death and in comparisons with [AppSheetState.currentDetent].
     */
    class Custom(val key: String, val resolve: SheetDetentScope.() -> Dp?) : SheetDetent {
        override fun equals(other: Any?): Boolean = other is Custom && other.key == key
        override fun hashCode(): Int = key.hashCode()
    }
}

/** What a detent may depend on. Every value is settled; none changes while the sheet moves. */
@Stable
interface SheetDetentScope : Density {
    /** The visible height at [SheetDetent.Large]. */
    val maxHeight: Dp
    val windowHeight: Dp
    /** Header plus measured content; [Dp.Unspecified] unless a [SheetDetent.FitContent] is listed. */
    val fittingHeight: Dp
    val sizeClass: SizeClass
}
```

Resolution rules (`SheetAnchors.kt`, in measure):

| Detent | Visible height `h` | Inactive when |
|---|---|---|
| `Large` | `H − (safeTop + LargeTopGap)` | never |
| `Medium` | `0.5 · H`, clamped to `Large` | height class Compact (iOS [A]) |
| `FitContent` | `header + content`, clamped to `Large` | never |
| `Fraction(f)` | `f · H`, clamped | never |
| `Height(d)` | `d`, clamped | never |
| `Custom` | resolver result, clamped | resolver returns `null` |

Here `H` is the height of the sheet's container, the window for every host, and `safeTop` is
`WindowInsets.safeDrawing` top: the status bar, the caption bar or a cutout, whichever is largest. Today's code uses the
status bar only (`ds/component/overlay/AppSheet.kt:132`, `:232`); a freeform caption bar would cover the grabber.

The anchor offset is `H − h`. Two detents that resolve within 1 px of each other merge into the higher-listed one, and
an inactive detent drops out. In tabletop posture, the highest active detent below `Large` rests on the fold
(`hinge.bottom`), as today (`ds/component/overlay/AppSheet.kt:138-139`, `:235`; `PostureTest.kt:70-78`).

### 3.2 State

```kotlin
/**
 * Where a sheet is and where it may go: its detents, the detent it rests on, and its motion.
 *
 * It lives outside composition, so whoever presents the sheet can also move or close it, and so
 * the sheet's position is read only where it is used: in placement by the sheet, in draw by the dim,
 * in a layer by whatever recedes behind. A drag recomposes nothing.
 *
 * Make it with [rememberAppSheetState], which keeps the detent across process death and
 * configuration changes. A sheet restored that way appears at its detent without sliding in.
 *
 * **Deviation** from CODE-STANDARDS API-8 (state as an interface): this is a class, as material3's
 * `SheetState` and foundation's `AnchoredDraggableState` are, because the sheet's layout and
 * gesture nodes use internal members an interface would have to publish.
 */
@Stable
class AppSheetState @RememberInComposition constructor(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
) {
    /** The detents, lowest first, as given. */
    val detents: List<SheetDetent>

    /**
     * Changes the detents of an open sheet. It stays on its detent if that detent is still listed and
     * active; otherwise it moves to the nearest one, keeping its velocity.
     */
    fun updateDetents(detents: List<SheetDetent>)

    /** The detent the sheet rests on; null while hidden, between detents, or leaving. Changes at most once per settle. */
    val currentDetent: SheetDetent?

    /** True from the first frame of [show] until the sheet is hidden again. */
    val isVisible: Boolean

    /** 0 hidden to 1 at the lowest active detent. Drives the dim. Read it in draw or layout. */
    @get:FrequentlyChangingValue
    val presentationFraction: Float

    /**
     * 0 at or below the detent under [SheetDetent.Large], 1 at Large; 0 for a centred or side sheet.
     * Whatever is beneath recedes with it (the card stack). Read it in a layer block.
     */
    @get:FrequentlyChangingValue
    val largeFraction: Float

    /** Rises to the initial detent, or back to the last one. Reverses a dismissal still running. */
    suspend fun show()

    /** Moves to [detent], carrying the current velocity. Ignored while the sheet is leaving. */
    suspend fun animateTo(detent: SheetDetent)

    suspend fun snapTo(detent: SheetDetent)

    /** Slides away and returns once hidden. Idempotent: a second call waits for the first. */
    suspend fun dismiss()

    companion object {
        /** Saves the settled detent's key and whether the sheet was shown. */
        fun Saver(detents: List<SheetDetent>): Saver<AppSheetState, *>
    }
}

/** An [AppSheetState] kept across recomposition, configuration changes and process death. */
@Composable
fun rememberAppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState
```

**Internals that matter for performance** (not public):

- `draggable: AnchoredDraggableState<SheetValue>`, where `SheetValue` is `Hidden` or `At(detent)`. Hidden is in the
  anchor set while presenting, while dismissing, and whenever dismissal by drag is allowed. Otherwise it is left out, so
  a drag below the lowest detent goes to the over-pull band. That is foundation's own guidance: "the anchor set should
  not include disallowed anchors" (R §2.6).
- `overPull: SheetOverPull`, an `OverscrollEffect` that holds the travel past either bound.
- `backProgress: MutableFloatState`.
- `dismissing: Boolean`, readable only through `snapshotFlow`.
- `presentSpring: AppSpring`, which presets set to `dialog`.

The saved form is a `String` key: `large`, `medium`, `fit`, `fraction:<f>`, `height:<dp>` or `custom:<key>`, plus a
`shown` flag. That is all a `Bundle` needs.

### 3.3 Options

```kotlin
/** How the sheet's surface is drawn. Glass and Frosted step down with `LocalGlassQuality` (§6.6). */
enum class SheetStyle { Automatic, Solid, Frosted, Glass }

/** Whether the screen beneath stays usable while the sheet is up. */
@Immutable
sealed interface SheetModality {
    /** Dimmed beneath at every detent; a tap on the dim asks to close; beneath is hidden from screen readers. */
    data object Modal : SheetModality

    /**
     * Beneath stays touchable, visible to screen readers and undimmed while the sheet rests at or below
     * [upThrough]; above it the dim fades in over the travel to the next detent and the sheet turns modal
     * (iOS `presentationBackgroundInteraction(.enabled(upThrough:))`).
     */
    data class NonModal(val upThrough: SheetDetent) : SheetModality
}

/** Where the sheet sits in the window. RTL flips [Start] and [End]. */
enum class SheetPlacement { Automatic, Bottom, Center, Start, End }

/** What a vertical drag on scrolling content does first. */
enum class SheetContentInteraction {
    /** [Resizes] for modal sheets on touch; [Scrolls] for non-modal sheets and for mouse-only windows. */
    Automatic,
    /** The sheet grows to its top detent before the content scrolls (iOS default). */
    Resizes,
    /** The content scrolls first; the sheet moves only from its header, its grabber, or a pull at the content's top. */
    Scrolls,
}

/** Which of the user's ways out are allowed. Programmatic dismissal is always allowed. */
@Immutable
data class SheetDismissal(
    val enabled: Boolean = true,
    val byDrag: Boolean = true,
    val byScrimTap: Boolean = true,
    val byBack: Boolean = true,
    val back: SheetBackBehavior = SheetBackBehavior.Dismiss,
)

enum class SheetBackBehavior {
    /** One Back closes the sheet from any detent (iOS, Gmail). */
    Dismiss,
    /** Back moves down one detent, and closes only from the lowest (material3's behaviour). */
    StepDown,
}
```

### 3.4 The component

```kotlin
/**
 * A sheet over the screen, iOS-style in this design system's look. It rises to a detent and can
 * be dragged or flicked between detents or away. The screen behind recedes as a card at the large
 * detent. On a wide window it becomes a centred form sheet or a side sheet ([placement]).
 *
 * This overload draws in place. It is what the shell's sheet destinations use; a screen uses the
 * `visible` overload. The content is measured once for the window and only moved while the sheet
 * moves. Bring a scroller ([SheetScrollColumn], a lazy list) and pass it
 * [SheetContentScope.contentPadding], which keeps the last row above the footer, the navigation bar
 * and the keyboard.
 *
 * [onDismissRequest] runs once, as soon as the person dismisses the sheet (drag, flick, a tap
 * outside, Back, Escape, the close button). By then the sheet is already sliding away with the
 * finger's velocity; remove it when the call arrives.
 *
 * @param title announced as the sheet's pane title, and shown by the default [header].
 * @param onDismissAttempt called once per gesture or press when [dismissal] blocks the way out the
 *   person took: show a confirmation there, then call [AppSheetState.dismiss] if they confirm. While
 *   it is set, Back and a tap outside are attempts too.
 * @param floating whether the sheet floats inset from the edges at its partial detents and attaches
 *   to them as it reaches the large one (iOS 26). Ignored with 3-button navigation.
 * @param grabber null hides it; the top 48 dp then stop being a resize target, so give the header a
 *   way to change detents when there is more than one.
 */
@Composable
fun AppSheet(
    state: AppSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    style: SheetStyle = SheetStyle.Automatic,
    modality: SheetModality = SheetModality.Modal,
    dismissal: SheetDismissal = SheetDismissal(),
    onDismissAttempt: (() -> Unit)? = null,
    placement: SheetPlacement = SheetPlacement.Automatic,
    contentInteraction: SheetContentInteraction = SheetContentInteraction.Automatic,
    floating: Boolean = AppSheetDefaults.floating(style),
    colors: AppSheetColors = AppSheetDefaults.colors(style),
    cornerRadius: Dp = AppSheetDefaults.cornerRadius(),
    maxWidth: Dp = AppSheetDefaults.maxWidth(),
    grabber: (@Composable SheetGrabberScope.() -> Unit)? = AppSheetDefaults.grabber(),
    header: (@Composable SheetHeaderScope.() -> Unit)? = AppSheetDefaults.header(),
    footer: (@Composable () -> Unit)? = null,
    content: @Composable SheetContentScope.() -> Unit,
)

/**
 * A sheet opened from inside a screen, for a choice or a short task that is not a destination. Keep
 * it composed and drive [visible]: it draws in the sheet layer above whatever its screen is in (the
 * tab, or the sheet that opened it). It is hidden with its tab while another tab is in front and
 * comes back with it. It slides in when [visible] turns true, out when it turns false, and back
 * in, keeping its velocity, if [visible] turns true again while it is leaving.
 */
@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    title: String? = null,
    // …the same optional parameters, in the same order…
    content: @Composable SheetContentScope.() -> Unit,
)
```

The parameter order follows CODE-STANDARDS API-2: required parameters, `modifier`, then variant and behaviour, then
colours and shape, then slots, with `content` last. There are two overloads because the in-place one needs a state the
navigation layer owns, and the other hides its state lifecycle from screens. Dismissal flags are grouped in one
`@Immutable` value. A flat list of five booleans would make every call site noisy and every addition a breaking change
(API-14).

### 3.5 Scopes and slots

```kotlin
@Stable
interface SheetContentScope {
    /**
     * Padding for the content's scroller: the header's overlap at the top; at the bottom the footer,
     * the navigation bar and the keyboard, each counted once. Its values are read in layout, so a
     * keyboard or footer change re-measures the scroller without recomposing it.
     */
    val contentPadding: PaddingValues
    val state: AppSheetState
}

@Stable
interface SheetHeaderScope {
    val title: String?
    val state: AppSheetState
    /** Asks to close, through the same rules as a tap outside: [SheetDismissal] and `onDismissAttempt`. */
    fun requestDismiss()
}

@Stable
interface SheetGrabberScope {
    val state: AppSheetState
    /** Cycles to the next active detent (wrapping to the lowest), as tapping iOS's grabber does. */
    fun cycle()
}
```

- **`contentPadding` is a `PaddingValues` implementation backed by snapshot state.** `LazyColumn` and `Modifier.padding`
  read it in measure, so a change invalidates layout and never recomposes. That is the technique of material3's commit
  `ff66ec4e` (R §2.4). It does not change during a drag (§4.6).
- **`SheetScrollColumn(modifier, contentPadding = scope.contentPadding, content)`** is a `Column` with `verticalScroll`,
  the design system's overscroll and the padding applied. It is the default answer for non-lazy content (fixes D13).
- **Header** (`AppSheetDefaults.Header(leading, trailing, title = scope.title)`) is a three-slot row with a minimum
  height of 44 dp (`heightIn`, never a fixed height). It measures `leading` and `trailing` first and centres the title
  in what is left, reserving `max(leading, trailing)` on both sides, with one line and an ellipsis. That fixes D5
  (`ds/component/overlay/AppSheet.kt:306-325`). At font scale 2 the row grows instead of clipping.
- **`AppSheetDefaults.CloseButton()`**: inside the header scope it calls `requestDismiss()`. It is a Tinted Small
  `AppIconButton` with the `designsystem_close` string, as today (`AppSheet.kt:316-326`).
- **Footer**: the sheet pins it above the keyboard and the navigation bar (§4.6). Inside a sheet, `AppBottomActionBar`
  reads `LocalSheetSurface`. It paints no background of its own, because the sheet's surface is behind it, and it pads
  `navigationBars.exclude(ime)` instead of `navigationBars ∪ ime`. That fixes D2 (`AppBottomActionBar.kt:38-39`).
- **`LocalSheetSurface`** (from PeriFlow's `LocalSheetSurface`, CS §11): non-null inside a sheet's content. When it is
  set:
  - `AppScaffold` draws no opaque background and consumes no status-bar inset;
  - `AppTopBar` shows the close button at the sheet's root instead of Back, and pads no status bar;
  - `AppBottomActionBar` behaves as a footer.

  So a full screen presented as a sheet works unchanged, and N4(a) is fixed at its source.

### 3.6 Defaults

```kotlin
object AppSheetDefaults {
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)
    val LargeTopGap: Dp            // DesignTokens.Sheet.largeTopGap      = 10.dp
    val GrabberSize: DpSize        // 36 × 5 dp, 5 dp from the top          [M] sheet.dart:704-724
    val GrabberTouchHeight: Dp     // 48 dp                                 [G] Material: "top 48dp portion"
    val HeaderMinHeight: Dp        // 44 dp
    val HandleArea: Dp             // 16 dp: where content starts with no header ([M] 15 pt, on the 4 dp grid)
    val FloatingInset: Dp          // 8 dp                                  [U] iOS 26 unpublished; measure
    val SideMargin: Dp             // 16 dp                                 [G] MDC detached side sheet

    @Composable fun style(): SheetStyle      // Glass under the Glass design style, else Solid
    @Composable fun floating(style: SheetStyle): Boolean   // style is Frosted or Glass
    @Composable fun colors(
        style: SheetStyle = SheetStyle.Automatic,
        container: Color = …,   // light: colors.surface; dark: colors.surfaceElevated
        scrim: Color = AppTheme.colors.scrim,
        grabber: Color = AppTheme.colors.contentTertiary.copy(alpha = 0.45f),
        content: Color = AppTheme.colors.contentPrimary,
        shadow: Color = AppTheme.colors.shadow,
    ): AppSheetColors
    @Composable fun cornerRadius(): Dp       // AppTheme.shapes.radiusSheet (24 / 30 / 8 / 28 by style)
    @Composable fun maxWidth(): Dp           // AppTheme.layout.sheetMaxWidth (640)
    fun grabber(): @Composable SheetGrabberScope.() -> Unit
    fun header(): @Composable SheetHeaderScope.() -> Unit   // grabber area only when there is no title
    @Composable fun Grabber(…)
    @Composable fun SheetHeaderScope.Header(leading: …, trailing: …)
    @Composable fun SheetHeaderScope.CloseButton()
}

@Immutable
class AppSheetColors internal constructor(
    val container: Color, val scrim: Color, val grabber: Color, val content: Color, val shadow: Color,
)
```

Every value comes from `DesignTokens.Sheet` (§12), and none is private to the component file. Today they are
(`AppSheet.kt:335-345`).

### 3.7 Presets

- **`AppActionSheet`**, migrated onto the sheet infrastructure:
  - its own small surface: two `surfaceElevated` groups, as today (`AppActionSheet.kt:119-148`);
  - the `FitContent` detent capped at 0.6 H, with `presentSpring = dialog` (M17);
  - `SheetAction.selected` adds a checkmark and the `RadioButton` role (A2, QA-14);
  - it scrolls when long (A1), keeps its Back handler through the dismiss (A3) and sets `paneTitle` (A5);
  - from Medium width it is an anchored card in the sheet layer, never a `Popup` window (decision 20). It anchors to
    the bounds passed as `anchor: () -> Rect?`, falling back to the call site (A4).
- **`AppChoiceSheet<T>(visible, options, selected, onSelect, onDismissRequest, label, …)`**: VitFleet's choice sheet
  (CS §11), on
  `SheetDetent.Custom("choice") { min(fittingHeight, windowHeight * 0.6f) }`. Options are rows with the `RadioButton`
  role and a checkmark, plus an optional "none" row. The language picker in Settings uses it (QA-14).

### 3.8 Navigation API (`:core:navigation`, WP04)

```kotlin
sealed interface Presentation {
    /** A sheet over the screen, which stays composed beneath it and recedes at the large detent. */
    data class Sheet(val sheet: SheetPresentation = SheetPresentation()) : Presentation
    data object Cover : Presentation
}

/** How a destination presented as a sheet behaves; the same knobs as [AppSheet]. */
@Immutable
data class SheetPresentation(
    val detents: List<SheetDetent> = AppSheetDefaults.Detents,
    val initialDetent: SheetDetent = detents.first(),
    val style: SheetStyle = SheetStyle.Automatic,
    val modality: SheetModality = SheetModality.Modal,
    val dismissal: SheetDismissal = SheetDismissal(),
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Automatic,
    val grabber: Boolean = true,
)

/** The metadata that marks a presented entry; typed, as the Navigation 3 recipe does. */
internal object PresentationKey : NavMetadataKey<Presentation>
```

- `entry<K>(presentation = Presentation.Sheet(SheetPresentation(detents = listOf(Medium, Large))))` stays the only way a
  feature declares a sheet (`nav4/NavGraph.kt:50-57`).
- **Deleted:** WP04's `SheetDetent` enum (`nav4/Presentation.kt:17-24`); WP03's `SheetPresentation(Set)`, `SheetKey` and
  `rememberSheetSceneStrategy` (`wt/WP03/.../core/navigation/SheetScene.kt`); the string key `PRESENTATION_METADATA`
  (`nav4/presentation/PresentationHost.kt:205`). That fixes N3.
- **`AppShell(sheetStrategy)`** (`nav4/AppShell.kt:96`) is deprecated (API-14). The strategy becomes built in, so
  `Presentation.Sheet` keys stop rendering as covers (N2).
- **Dismissing from code:** `navigator.back()`, or `navigator.dismissPresented()`, which pops every presented entry as
  one change.
- **A feature never touches `AppSheetState` of a destination.** Its content reads
  `LocalSheetSurface.current?.state`, for example to expand on a button press.

---

## 4. Behaviour

### 4.1 Lifecycle

| Phase | Offset | Hidden anchor | Own Back handler | Reports moving to host gate | Touch |
|---|---|---|---|---|---|
| **Opening frame** (composed, laid out) | hidden | yes | enabled | yes (from composition apply) | — |
| **Presenting** | → initial detent on `presentSpring` | yes | enabled; Back reverses into a dismissal with the current velocity | yes | catches it (stops the animation) |
| **Resting** at a detent | detent | if `dismissal.enabled && byDrag` | enabled when top-most (§4.8) | no | drag |
| **Dragging** | follows the finger; past a bound → band | same | enabled | yes | — |
| **Settling** | → target, velocity-seeded | same | enabled | yes | catches it |
| **Dismissing** | → hidden on `presentSpring`, velocity-seeded | yes | **disabled** | yes | ignored (`show()` from code still reverses it) |
| **Gone** | hidden | — | removed | reports `false`, **then** the owner unmounts / `onRemove` returns | — |

- **Show:** `show()` waits for anchors through `snapshotFlow`, as today (`AppSheetState.kt:107-118`, `:132`). It then
  waits one `withFrameNanos {}` so the spring's clock starts on the frame after the opening frame (§10.3). Only then
  does it animate from hidden.
- **Restored state:** a restored sheet (`shown = true` in the saver), or a sheet under reduce motion, snaps to its
  detent and plays no slide.
- **Retargets keep velocity:** dismissing during the show, `animateTo` during a settle, an anchor change during an
  animation, and `show()` during a dismissal all start from the current offset with `draggable.lastVelocity`. Foundation
  already does this for `animateTo` and `updateAnchors` (R §2.6, `:1001-1029`). This fixes D1, D7 and D10.
- **No retarget while leaving:** `animateTo` is ignored while dismissing (D9). Only `show()` turns a dismissal around.
- **Catching a moving sheet:** a pointer-input node on the surface stops any running animation on the `Initial` pass of
  a finger down, whether the finger lands on the content, the header or the grabber. It runs
  `draggable.anchoredDrag(MutatePriority.UserInput) {}`. This is the iOS "catch it mid-flight", and it closes D8 for
  the nested-scroll path. Foundation's `startDragImmediately = isAnimationRunning` covers only the sheet's own
  draggable (R §2.6, `:479`).

### 4.2 Gestures

- **Drag surface:** the whole sheet uses `Modifier.anchoredDraggable(state.draggable, Vertical, enabled = !dismissing,
  overscrollEffect = state.overPull, flingBehavior = SheetFlingBehavior(state, …))`, plus the nested-scroll connection
  for scrolling content (§4.5).
- **Rubber band** (`SheetOverPull`): `shown = c · x · d / (d + c · x)` with c = `springs.rubberBand` = 0.55
  (`DesignTokens.kt:196` [C]).
  - **Above the top detent:** `d = 0.008 · H` when the top detent is `Large` ([M] the top gap stretches from 0.08 H to
    0.072 H, R §1.8), and `0.04 · H` for any other top detent [U: tune on the phone]. On the Redmi (H ≈ 872.7 dp),
    0.008 H is about 7 dp of travel at most.
  - **Below the lowest detent when dragging out is blocked:** `d = H`, the standard scroll band (MOTION-PLAN M10).
  - **Release:** `applyToFling` springs the amount back on `springs.sheet`, seeded with the leftover velocity times the
    band's slope at release, as pull to refresh does (M20). iOS uses a 180 ms easeOut [M]; the spring is about as long
    (90 % in 0.21 s [D]) and keeps velocity (MOTION-PLAN §1 rule 2).
- **Grabber:**
  - The tap target is the top 48 dp of the sheet, the full width. A tap cycles to the next active detent and wraps to
    the lowest.
  - Enter and Space do the same when the grabber is focused; it is focusable and shows the focus ring.
  - With a mouse, hovering shows a tooltip ("Resize sheet").
  - Dragging works from anywhere on the sheet anyway.
- **Haptic:** `AppHaptic.Threshold` plays only when a settle lands on a different detent from the one the gesture began
  on (kept from `AppSheetState.kt:194-195`). It is played by the fling behaviour, not from composition.

### 4.3 Choosing the detent on release

`SheetFlingBehavior.performFling(v)`. Velocity `v` is in px/s and positive downward. `offset` is the sheet's top.
`stops` are the active detent offsets, plus `hidden` when dragging out is allowed.

```
if |v| ≥ 2.0 · H per second:                                   // [M] iOS dismiss velocity (R §1.8)
    target = the first stop strictly beyond offset in the direction of v,
             or the last stop in that direction if none
else:
    projected = offset + v · 0.099 s                            // r = 0.99 per ms (MOTION-PLAN §1 rule 3)
    lowest = the lowest active detent
    if hidden ∈ stops and projected > lowest:
        target = hidden if (projected − lowest) / (hidden − lowest) ≥ 0.52 else lowest   // [M] 0.52
    else:
        target = the stop nearest projected
animate(offset → target, initialVelocity = v, springs.sheet.pixels)
```

- **Why not projection alone:** from the large detent, projection needs about 4.6 H/s to reach the dismiss midpoint,
  where iOS needs 2 H/s (R §1.9 [D]). The hybrid keeps projection for the in-between detents and adds iOS's two
  dismiss rules.
- **Fast flicks go one stop:** a fast flick from large with medium present lands on medium; iOS's behaviour there is
  [U] (R §6). A flick from large on a `[Large]` sheet dismisses.
- **Units:** on the Redmi, 2 H/s ≈ 4800 px/s, against WP03's 9000 px/s test flick (`SheetTest.kt:125-141` [C]).
- **Speed scaling:** the spring is divided by the motion style's speed, as every travelling spring is
  (`motion.json:15` `bySpeed: true`).

### 4.4 Dismissal and the confirm hook

- **Dismissal allowed:** drag out, a flick, a tap on the dim (modal only), Back, Escape and the close button each:
  1. start the slide to hidden with the current velocity;
  2. call `onDismissRequest()` once;
  3. mark the sheet as dismissing.
- **Dismissal blocked** (`dismissal.enabled = false`, or the flag for that particular way out is false):
  - **Drag:** the hidden anchor is absent, so a drag below the lowest detent rubber-bands with `d = H`. On release past
    the 0.52 rule, `onDismissAttempt` fires once and the sheet springs back.
  - **Scrim tap:** `onDismissAttempt` fires; nothing moves.
  - **Back:** predictive progress is damped to `0.25 · p`, so the sheet visibly resists. On commit,
    `onDismissAttempt` fires and the shrink springs back on `sheet`.
  - **Escape:** as Back without a gesture.
- **The dirty rule** (VitFleet `LoadFormScreen.kt:128`, CS §4.5): pass `dismissal = SheetDismissal(enabled = !dirty)`
  and an `onDismissAttempt` that opens an `AppActionSheet` "Discard changes?". On confirm it calls
  `scope.launch { state.dismiss() }` and then the owner's removal. An untouched form keeps every way out.
- **The `visible` overload:** a dismissal the user starts unmounts once the sheet is hidden **and** `visible` is false.
  If the caller never clears `visible`, the sheet stays composed but hidden: it is not drawn, not hit-tested and not in
  the stack, so it costs no frames. This makes D1 impossible: there is no state in which a mounted sheet is part-way
  and deaf.

### 4.5 Nested scroll

The connection sits on the surface, so every scroller inside reaches it. It writes the offset through
`draggable.dispatchRawDelta`, a plain state write.

| | Resizes (modal default) | Scrolls |
|---|---|---|
| `onPreScroll`, upward, `UserInput` | sheet takes it until its top detent; content scrolls after | content first |
| `onPostScroll`, downward, `UserInput`, content at top | sheet follows the finger (band below the lowest detent if blocked) | same |
| `onPostScroll` from a fling (`SideEffect`) | ignored: a list flung to its top bounces in its own overscroll and never collapses the sheet | same |
| `onPreFling` | sheet not resting → settle with the velocity through §4.3, **consume all of it** | same |
| `onPostFling` | nothing | nothing |

- **Mouse wheel:** with a mouse-only input (`AppTheme.input`), `Automatic` resolves to `Scrolls`. Since Compose 1.7
  the wheel also reports `UserInput`, so `Resizes` would grow the sheet on the first wheel notch [D].
- **Recomposition:** no handoff path recomposes. `dispatchRawDelta` writes `offset`, which only placement and draw
  read.

### 4.6 IME

The rule is that the sheet rides the keyboard and the content never re-measures per keyboard frame.

- **Lift** (L1): in the sheet's placement block, read `WindowInsetsRulers.Ime.current.bottom` ([V] ui
  `WindowInsetsRulers.kt:48`, `:97`) and turn it into `ime` px, the keyboard height above the window bottom. Then
  `visibleTop = max(largeTop, offset − ime)`. The sheet is placed at `visibleTop`, so a medium sheet rises with the
  keyboard frame by frame, synced to the system's 285 ms curve on API 30+ (M22), and stops at the large top.
  - Detent anchors do not include the keyboard, so dragging math, `currentDetent` and `largeFraction` (the card stack)
    are unchanged while it is up.
  - This fixes D4: the focused field moves up with the sheet.
- **Footer** (L1): placed at `min(visibleBottom, H − ime) − footerHeight`, and never above `visibleTop + header`.
- **Content padding:** `contentPadding.bottom = max(footer, navigationBarsExcludingIme, imeOverlap)`, where
  `imeOverlap` is the IME's **target** while it opens and its final value once it has closed. Use
  `WindowInsetsRulers.Ime.getAnimation(scope).target` and `.isAnimating`, read in placement ([V]
  `WindowInsetsRulers.kt:69`, `:231-250`); `maximum` is never provided for the IME on Android ([V] `:53`).
  - The padding therefore changes **once when the keyboard starts to open** (rows can scroll above it at once) and
    **once after it has closed**: at most 2 re-measures of the content per IME transition.
  - The padding is counted against the large-detent geometry. At a partial detent the bottom of the content is below
    the window anyway, which is iOS's `.resizes` behaviour: expand to reach the last row.
- **Consumption:** the content container calls `consumeWindowInsets(WindowInsets.ime)` and the safe-drawing top, so an
  `imePadding()` or `windowInsetsPadding(ime)` inside returns 0.
  - The edge-to-edge skill's preferred `fitInside(WindowInsetsRulers.Ime.current)` (SKILL.md:108-112) is right for a
    screen. Inside a sheet it would shrink the content again, because rulers are not consumed [U], and it re-measures
    its child on every frame ([V] fl `RulerAlignment.kt:36-50`).
  - Docs rule: content inside a sheet uses `contentPadding`, and never `fitInside(Ime)` or `imePadding()`.
  - A small bar inside the content, such as a comment composer of input-row size, may use `fitInside`; that is D1 cost.
- **API 26–29:** no IME animation runs and the inset jumps. The placement block sees a jump in `ime`. The sheet does
  not jump: `imeLift` is smoothed through an `Animatable` on `springs.screen` whenever
  `getAnimation(scope).isAnimating` is false and the value changed (the M22 fallback).
- **Back with the keyboard up:** the sheet's handler is disabled while the IME is visible. The flag is written from
  placement into a plain field and pushed to `isBackEnabled` from an effect, so it causes no recomposition. Back
  therefore hides the keyboard first, whatever the system's priority ordering between the IME window's callback and
  an app's `PRIORITY_OVERLAY` callback. That ordering is [U]; M3 had to lower its priority for this (commit `b8dd8af9`,
  R §2.4).
- **Drag with the keyboard up:** a downward drag of the header or grabber past 24 dp hides the keyboard
  (`LocalSoftwareKeyboardController.hide()` from the drag callback) [D]. That matches iOS's interactive keyboard
  dismissal in spirit.

### 4.7 Edge to edge and chrome insets

- **Top:** `largeTop = safeDrawing.top + 10 dp`. The content consumes `safeDrawing.only(Top)`, so an `AppTopBar` inside
  a sheet never pads the status bar (QA-19).
- **Bottom:** the surface is drawn to the window bottom, behind the navigation bar.
  - Content padding includes `navigationBars.exclude(ime)`, so with 3-button navigation the last row clears the
    buttons (D3).
  - The footer pads the same insets.
- **Sides:** the sheet's frame is centred within `safeDrawing.only(Horizontal)`, so it avoids a landscape cutout. Its
  width is `min(maxWidth, safe width)`.
- **3-button navigation:** detected by `WindowInsets.tappableElement.bottom > 0` [G: the tappable-element inset is
  non-zero only for tappable system bars]. With it, `floating` is off and the sheet is edge-attached, so the opaque
  button area never sits over the dimmed gap.
- **Tab bar:** a modal sheet covers it under the dim. A **non-modal** sheet at its non-modal detents rests on top of
  the bottom chrome: its "hidden" baseline is `H − LocalBottomChrome.value`, the settled value the shell writes
  (`ds4/foundation/layout/Chrome.kt:27-31`). So a Maps-style peek never covers the bar.
- **Inside the sheet:**
  - `LocalBottomChrome` is the sheet's own holder, written with **settled** values only: the footer height, plus the
    navigation bar or the keyboard once its animation ends. Screens inside that use `chromeContentPadding()` (which
    reads in composition, `ds4/foundation/layout/Chrome.kt:92-103`) recompose at most twice per keyboard transition,
    never per frame (D2).
  - `LocalTabBarVisible = false`.
  - `LocalTopChrome` is the inner `AppScaffold`'s own.
- **Toasts over a sheet:** the shell's `BottomChromeWriter` takes `max(its value, SheetStack.topSheetBottomChrome)` and
  lets the keyboard win over a presented bar. That fixes N9 (`nav4/shell/BottomChromeWriter.kt:50-51`).

### 4.8 Back, predictive back, Escape and BackGate

**Registration.**

- **The handler:** one `NavigationEventHandler` per sheet at `PRIORITY_OVERLAY`, on the caller's dispatcher. It
  replaces `OverlayBackHandler` (`ds/component/overlay/OverlayBackHandler.kt:27-51`) and implements the navigation-event
  skill's "one state per handler, branch in `onBackCompleted`".
- **Verified mechanics** ([V] ne `NavigationEventDispatcher.kt:169-170`, `:194-195`; `NavigationEventHandler.kt:125-126`;
  `NavigationEventProcessor.kt:271-273`, `:574-579`; the 1.2.0 bytecode keeps the same two deques and the dispatcher
  check):
  - Child dispatchers share their root's processor.
  - Overlay handlers anywhere in the tree are asked before default handlers, last registered first.
  - A handler is disabled while its dispatcher or any ancestor is disabled.

  So an overlay handler in a kept tab outranks the shell's default-priority handlers, and it goes quiet when the tab is
  hidden. That answers CS §4.5's [U].
- **Explicit enablement:** registration order is not trusted for stacking. The handler is enabled only when every one
  of these holds:
  - the sheet is the **top-most visible** entry of the `SheetStack` (§4.10);
  - its layer is not covered by a presentation (§9.1, invariant 8);
  - it is not dismissing;
  - the IME is hidden;
  - a way out exists: a dismissal, an attempt hook, or `StepDown` above the lowest detent.

  For a modal sheet the handler stays enabled even when dismissal is blocked, so Back never pops the page under a modal
  sheet.

**Predictive back** (SPEC §5.4; [G] `mat/BottomSheet.kt:443-487`, `mat/BackHandler.kt:47-51`):

| Quantity | Value |
|---|---|
| Eased progress `p` | `CubicBezierEasing(0.1f, 0.1f, 0f, 1f).transform(event.progress)`; `0.25 · p` when blocked |
| Surface scale | `scaleX = 1 − min(48 dp, W) · p / W`, `scaleY = 1 − min(24 dp, Hs) · p / Hs` (`Hs` = the surface layer's height) |
| Pivot | `TransformOrigin(0.5f, (visibleBottom − surfaceTop) / Hs)`: the window's bottom edge for bottom placements, the card's bottom for centre and side |
| Content | its own layer: `scaleY = scaleX / scaleY` about `(0.5f, 0f)`, so text keeps its aspect ratio (D6) |
| Where read | `backProgress` only in those two layer blocks: **0 recompositions per event** |
| Commit | `Dismiss`: the slide to hidden on `presentSpring` while the scale holds (MDC translates the scaled container, R §2.5), then `onDismissRequest()`. `StepDown` above the lowest detent: `animateTo(lower)` while the scale springs back on `sheet` |
| Cancel | the system springs progress back to 0 before `onBackCancelled` (SPEC §2, "Cancel motion"); any residual goes to 0 on `springs.sheet.unit` |
| Back key, 3-button bar, API < 34 | no progress: straight to the commit, no shrink |
| Dim | does not change during the gesture; follows `presentationFraction` once the slide starts |

The presenter (card stack) does not move during the gesture. It returns with `largeFraction` as the sheet slides, so
the commit reads as one motion.

**Escape:** `onPreviewKeyEvent` on the sheet's focus group: `Key.Escape` on key-down runs the same path as a completed
Back with no gesture. Since a modal sheet traps focus (§8), the key always reaches it. This is ADAPTIVE-PLAN:565's
explicit handler, and fixes D16.

**BackGate coalescing (decision 2):**

- **`HostMotion`** is a design-system interface in `foundation/layout`, which WP04 owns (MASTER-PLAN ownership row).
  WP04 provides one per host, forwarding to that host's `BackGate.report`:
  - each kept tab's stack host → that tab's gate;
  - the presentation host → `presented.gate`.

  ```kotlin
  /** Where a component that moves its host's content says so, so the host's Back gate counts presses meanwhile. */
  fun interface HostMotion { fun report(mover: Any, moving: Boolean) }
  val LocalHostMotion = staticCompositionLocalOf { HostMotion { _, _ -> } }
  ```
- **Reporting:** the sheet runs `snapshotFlow { state.isMoving }.collect { hostMotion.report(state, it) }` in an
  effect. `report(false)` always comes before the owner removes the sheet.
- **While dismissing**, the sheet's handler is off. A Back press therefore reaches the host's `StackBackHandler`, whose
  `enabled` is `canGoBack || gate.moving` (`nav4/back/StackBackHandler.kt:67`). It is counted (`BackGate.kt:51-57`)
  and applied as one pop once the gate settles (`GateSettle.kt:1-24`). Presses past the root are dropped
  (`BackGate.kt:18-19`).
- **Destination sheet:** the first Back commits the dismissal. `onDismissRequest` calls `presented.gate.onBack(pop)`.
  The sheet is moving, so that press is counted too. Once the sheet is hidden, `report(false)` lets `GateSettle` pop
  every counted press together. `onRemove()` then calls `state.dismiss()`, which returns at once, and the scene leaves
  composition. **Three quick Backs** over two presented sheets close both in one change. Over a single sheet, the
  extras are dropped at the presentation root and never reach the tab.
- **In-screen sheet:** the extra presses go to the tab's gate. They pop the tab's stack once the sheet is hidden:
  Back ×2 closes the sheet, then pops the screen, as a burst on a plain stack does.
- **`NavGuard`:** while a sheet presents or dismisses, its host's gate is moving. A push to that host waits
  (`nav4/back/NavGuard.kt:15-24`), so a row tapped in a sheet while it is still rising does not start a push under a
  moving sheet. N4(c) is fixed because the sheet frame no longer reads the underlying scene's transition (§9.2).

### 4.9 Background interaction (non-modal)

- **The boundary** is the offset of `upThrough`. While `offset ≥ boundary` (at or below it), the sheet is non-modal:
  - **Touch:** the `SheetDimNode` returns `sharePointerInputWithSiblings() = true` and consumes nothing, so a touch
    outside the sheet reaches the content beneath in the same frame. The node is not removed, so nothing recomposes
    [D: Compose hit testing passes to siblings only when the hit node shares].
  - **Dim:** none.
  - **Semantics:** beneath stays in the tree.
  - **Back:** for a non-dismissible sheet at its lowest detent, the handler is disabled and Back goes to the screen.
    Above that it steps down, as in Maps.
- **Above the boundary:**
  - the dim fades in linearly over the travel from `upThrough` to the next detent up, as iOS's dimming does (R §1.3,
    WWDC21);
  - the node consumes taps (an attempt or a dismissal) and stops sharing;
  - beneath is hidden from accessibility.
- **The modal flag** is `derivedStateOf { offset < boundary }`. It flips the semantics node's
  `hideFromAccessibility()` through `invalidateSemantics()` in a node, or by one recomposition of the layer per
  crossing (budget ≤ 1).
- **No card stack while non-modal.** The presenter recedes only from `largeFraction`, which is 0 at non-modal detents.

### 4.10 Stacked sheets

- **`SheetStack`** is window-wide and provided by `AppTheme`. It keeps the visible sheets in z-order: by layer level
  (§9.1), then by registration. From that it derives:
  - `presenterRecede` = `largeFraction` of the lowest **modal** sheet directly above the presenter;
  - for each sheet, `recede` = `largeFraction` of the sheet directly above it;
  - `isTopMost`;
  - `presenterDrawsDim`;
  - `topSheetBottomChrome`.

  The fast values are `@FrequentlyChangingValue` getters, computed on read with no allocation.
- **Covered sheet transform** [M] (`sheet.dart:48-62`, R §1.5), in its own surface layer block (L0):
  `scale = 1 − (1 − 0.9165) · r` about its top centre, and `translationY = −0.005 · H · r`.
- **Covering sheet:** its `largeTop` is the covered sheet's lifted top plus the 10 dp peek [D]. iOS's "2 % higher"
  (`sheet.dart:48-55`) assumes a 0.08 H top gap, which would put an Android sheet under the status bar; the peek keeps
  each level's strip visible.
- **Dim:** the covered sheet draws the covering sheet's dim inside its own clipped layer (decision 14).
- **Grabber:** hidden on a covered sheet [A] (`prefersGrabberVisible` doc). Its alpha is read in draw.
- **Depth:** at most **two** sheets behind the front one are drawn. Deeper ones get layer alpha 0, which HWUI skips
  without an offscreen pass, so overdraw stays bounded [D]. Their compositions stay.
- **Back and semantics:** only the top-most sheet takes Back, Escape and focus. Covered sheets are hidden from
  accessibility while a modal sheet is above.
- **HIG:** "display only one sheet at a time" [A]. The docs (WP12) say to prefer replacing a sheet
  (`navigate(key, popUpTo = current)`), which the gate sequences: the old sheet slides out and the push waits
  (`NavGuard`).

### 4.11 Kept tabs

An in-screen sheet opened from a tab screen lives in the **tab sheet layer** (§9.1). Its entry carries the owner tab's
`TabVisibility` (`ds4/foundation/layout/Chrome.kt:47-54`). While the owner is hidden:

- **Drawing:** the layer skips placing it. That is a placement-time read of `owner.isVisible`, so the tab switch
  re-places and recomposes nothing. It stays composed, with its state.
- **Back:** its handler is disabled through the tab's dispatcher (`nav4/shell/tabs/KeptTabs.kt:151` sets
  `isEnabled = active`; handler check [V] above).
- **Lifecycle:** it is the caller's, capped at CREATED (`KeptTabs.kt:188`), so flows inside pause.
- **Stack:** `SheetStack` excludes it. The presenter returns to scale 1 at once; a programmatic tab switch is instant
  anyway.

When the tab returns, the sheet is placed again at its detent, with no animation. This is VitFleet's
`LocalOverlaysHidden` behaviour (CS §11) and fixes N7. A destination sheet belongs to no tab.

### 4.12 Process death and configuration changes

| Case | What happens |
|---|---|
| Destination sheet, process death | WP04's saver restores `presentedKeys` (`nav4/ShellState.kt` `shellSaver`). `SheetEntryDecorator` runs inside the `rememberSaveableStateHolderNavEntryDecorator` scope (`nav4/presentation/PresentationHost.kt:92-94`), keyed by `contentKey`, so `rememberAppSheetState` restores the detent. `shown = true` → snap, no slide; the presenter is receded from the first frame |
| In-screen sheet, process death | `visible` is the caller's: ViewModel state with `persistState`, or `rememberSaveable`. The sheet's state is `rememberSaveable` in the caller's composition, so it restores the same way |
| Activity recreated (rotation, theme, locale) | Same as process death, through saved state |
| Resize with no recreation (freeform, fold, unfold, split) | Anchors are recomputed in placement. A resting sheet moves to the same detent's new offset. A running animation is retargeted by `updateAnchors` and keeps its velocity (D10). A drag in progress keeps its finger-relative offset, clamped. A placement change (bottom ↔ centre ↔ side) snaps to the new frame at the same detent: a resize is not navigation (MASTER-PLAN A3.9 row "Fold, unfold or freeform resize") |
| Detents changed by the caller while open | `updateDetents` (D15) |
| Restored detent no longer active (it was medium, now Compact height) | The nearest active detent |

---

## 5. Motion

Every value is a token (§12). Springs are `AppSpring(response, bounce)` (`ds/theme/AppMotion.kt`), and the travelling
ones are divided by the motion style's speed.

| Motion | Token | Value | Source |
|---|---|---|---|
| Present, detent settle, dismiss | `springs.sheet` | **0.34 s, bounce 0** (k ≈ 342, ζ 1); Calm 0.3908 s, Snappy 0.2957 s | CP1 (`decisions/CP1.md:40`, `:137`); `DesignTokens.kt:135`, `:169`, `:186` [C] |
| Action sheet, choice sheet, form-sheet scale-in | `springs.dialog` | **0.275 s, bounce 0** (k ≈ 522) | CP1 `:41`; M17 |
| Detent projection | `sheet.projectionSeconds` | **0.099 s** (r 0.99 per ms) | MOTION-PLAN §1 rule 3 |
| Dismiss and step velocity | `sheet.flingHeightsPerSecond` | **2.0 H/s** | [M] `sheet.dart:76-78` |
| Dismiss position | `sheet.dismissPosition` | **0.52** of lowest → hidden | [M] `sheet.dart:1114-1119` |
| Rubber band | `scroll.rubberBand`; `sheet.overPull.atLarge` / `.atPartial` | **c 0.55**; `d` = **0.008 H** / **0.04 H** [U]; below the lowest detent `d = H` | [A] c; [M] 0.008 |
| Band release | `springs.sheet` | velocity × band slope | MOTION-PLAN M20 pattern |
| Card stack: presenter | `sheet.cardStack.scale`, `.corner`, `.overlayAlpha`, `.overlayDark`, `.peek` | **0.9165** about the top centre; corner **device radius × 0.9 (if > 20 dp, else 0) → 12 dp**; overlay **0.10** black in light, **#C8C8C8** in dark; translation to `largeTop − peek` with **peek 10 dp** | [M] `sheet.dart:14-25`, `:64-74`, `:84-90`; peek [D] |
| Card stack: covered sheet | `sheet.stack.coveredScale`, `.coveredLift` | **0.9165**, **−0.005 H** | [M] `sheet.dart:57-62` |
| Status bar flip | `sheet.cardStack.statusBarFlip` | light icons while `presenterRecede > 0.5` (light theme only) | [M] `sheet.dart:362-367` |
| Floating → attached | `sheet.floating.inset`, `.minRadius`, `.maxRadius` | inset **8 dp → 0** and radius **concentric → `cornerRadius`**, both linear in `attach` (= `largeFraction`) | [A] WWDC25 323; numbers [U] |
| Glass → opaque | `sheet.glass` | body alpha from `GlassColors.body` to 1 over `attach`; no backdrop sampling at `attach ≥ 0.999` | [A] WWDC25 323 |
| Predictive back | `sheet.back.shrinkX`, `.shrinkY`, `.easing`, `.blockedLimit` | **48 dp**, **24 dp**, **(0.1, 0.1, 0, 1)**, **0.25** | [G] SPEC §2; blocked [D] |
| Form sheet in / out | `sheet.form.enterScale`, `.enterRise` on `dialog` | in: scale **0.94 → 1**, rise **24 dp**, `ModulateAlpha` 0 → 1; out: the reverse | PeriFlow `AppDetentSheet.kt:333-336` [C] (CS §11) |
| IME lift on API 26–29 | `springs.screen` | 0.344 s, bounce 0 | MOTION-PLAN M22 |
| Haptic | `AppHaptic.Threshold` | on landing on another detent | `ds/foundation/interaction/AppHaptics.kt:26-27` [C] |

**Reduce motion** (MOTION-PLAN §1 rule 5):

- **Present and dismiss** become a fade of the surface with `CompositingStrategy.ModulateAlpha`: in over
  `fades.longMs` (200 ms) with the enter easing, out over `fades.shortMs` (120 ms) with the exit easing.
  **Deviation:** rule 5's 150 ms is not a token, and the two fade tokens already exist (`motion.json:31`).
- **Card stack:** no scale; the overlay only.
- **Unchanged:** drags still follow the finger and settle on `sheet`, because that motion is the person's own. The
  rubber band stays.
- **Form sheet:** fades with no scale.

**No bounce anywhere a sheet moves** (CP1 "no bounce on anything that moves a full screen"). The only bounce is the
release of a press inside the content.

---

## 6. Visuals

### 6.1 Shape

- **Attached** (Solid by default, and any sheet at `attach = 1`):
  - the surface layer has `shape = RoundedCornerShape(cornerRadius)` with **one radius** and `clip = true`;
  - the layer is `cornerRadius` taller than the visible sheet, so its bottom corners sit below the window edge;
  - the outline is then a round rect with a single radius, which HWUI clips in hardware on every API from 26 [G:
    `android.graphics.Outline.setRoundRect` takes one radius; a path outline clips only from API 33];
  - the shape object is constant, so the outline is never rebuilt during motion.
  - Today's `.clip(shapes.sheet)` (`AppSheet.kt:192`) is top-only rounded, which makes a path outline below API 33 [D].
- **Centre and side:** all four corners at `cornerRadius` (`shapes.xl` for the form sheet, as today `:192`), constant.

### 6.2 Floating (iOS 26 inset)

- **When:** with `floating = true`, gesture navigation, a bottom placement, and `attach < 1`.
- **How:** `SheetSurfaceNode` clips in **draw** to one rounded rect: `(inset, visibleTop, W − inset, H − bottomGap)`
  with radius `r`.
  - It uses one reused `Path` (`rewind()` + `addRoundRect`), so nothing is allocated per frame.
  - The path is a single rrect, which Skia clips analytically without a layer [U: confirm in Perfetto that no
    `saveLayer` appears during a drag].
  - Cost: re-recording that one node per frame (D1). The content's layer is only re-placed.
- **Values:**

  | Name | Value |
  |---|---|
  | `inset` | `lerp(8 dp, 0, attach)` |
  | `bottomGap` | the same as `inset` |
  | `r` | `lerp(rFloat, cornerRadius, attach)`, with `rFloat = clamp(Rdisplay − inset, 16 dp, 36 dp)` |
  | `Rdisplay` | `rootWindowInsets.getRoundedCorner(POSITION_BOTTOM_LEFT).radius` (API 31+, read once on attach [G], R §1.1, MDC `BottomSheetBehavior.java:1639-1673`); before API 31, `rFloat = cornerRadius` |

- **One radius is a deviation:** iOS gives a floating sheet fixed top corners and concentric bottom corners [A]. A
  single radius keeps the clip on Skia's rrect fast path. With `Rdisplay − inset` within a few dp of the style's sheet
  radius on most phones [U], the difference is small.
- **At `attach = 1`** the node stops clipping, and the static outline of §6.1 takes over. A resting attached sheet
  costs nothing.
- **Fallback if the rrect clip shows offscreen work** on the phone: drop the bottom gap and clip only the sides and
  corners through a constant-size layer outline (`Outline.setRoundRect` with an inset rect). That outline can be cached
  per whole pixel of inset, at most about 22 entries at density 2.75 [D].

### 6.3 Grabber and header

- **Grabber:** 36 × 5 dp, fully rounded, 5 dp from the top, in `contentTertiary` at α 0.45 [M size; E colour]. It is
  drawn by a `drawBehind` of the 48 dp touch band, so it is one node with no child layout.
- **Content start:** with no header, the content begins at `HandleArea` (16 dp). With a header, the header row
  (minimum 44 dp) starts at 14 dp, as today (`AppSheet.kt:306`).
- **Header background:** transparent, sitting on the surface. Over scrolling content it gets the existing top fade
  (`ds/foundation/scroll/ScrollEdge.kt`), drawn as a strip, never as a layer alpha.

### 6.4 Dim (scrim)

- **Colour:** `colors.scrim`, light `0x660B0F1A`, dark `0x8F000000` (`DesignTokens.kt:249`, `:292` [C]). iOS's sheet
  dimming value is unpublished [U]. The token stays.
- **Drawn as a rect**, `drawRect(scrim, alpha = presentationFraction)` (D2), never as layer alpha (SMOOTHNESS S8).
- **With a presenter that claims it** (`SheetStack.presenterDrawsDim`), the dim is drawn inside the presenter's
  clipped layer. In light mode it merges with the card-stack overlay into one rect of combined alpha:
  `1 − (1 − a_scrim · pf)(1 − 0.10 · lf)`, where `pf` is `presentationFraction` and `lf` is `largeFraction`. Dark mode
  needs two rects (`#C8C8C8` against black) [D]. Outside the scaled presenter the window shows black, so it needs no
  dim.
- **Accessibility and touch:** the dim node is the sheet's click target "Close sheet" (§8) and its pointer blocker
  (§4.9), whether it draws or not.

### 6.5 Elevation without alpha-over-shadow artefacts

- **Modal sheets cast no shadow.** The dim separates them, as on iOS. M3's Level 1 is 1 dp
  (`mat/SheetBottomTokens.kt` [G]).
- **Non-modal sheets, side sheets and floating sheets** draw a drop shadow **outside the shape only**: a cached
  `Shadow` painter with the inside cut away. This is WP05's `glassSurface` technique (`glass/GlassSurface.kt:26-38`
  [C]): "a see-through body would otherwise show it as a dark band".
  - The shadow is built once per surface size and never per frame.
  - **Default tokens [U, tune on the phone]:** radius 16 dp, y 0; `colors.shadow` at α 0.10 in light and 0.32 in dark.
  - It fades out as `attach → 1`; alpha is passed to the painter in draw.
- **Never on any sheet ancestor:** layer alpha, or a `Surface`-style elevation with an opaque fill behind a translucent
  body. An offscreen layer "is clipped to its bounds and cuts the shadow off square" (`GlassSurface.kt:36-38` [C]). The
  reduce-motion fade is `ModulateAlpha` on the sheet layer, which needs no buffer, and with the inside of the shadow cut
  away nothing dark shows through. That is the pull-to-refresh defect of REQUIREMENTS 29, not repeated.

### 6.6 Styles, light and dark

| Style | Body | Rim | Tier mapping (`LocalGlassQuality`, `glass/GlassQuality.kt:24-48`) |
|---|---|---|---|
| Solid | `container`: `surface` (light), `surfaceElevated` (dark) | none | every tier |
| Frosted | `glassSurface(shape, GlassDefaults.frostedColors())` (`glass/GlassDefaults.kt:57-67`) | the glass rim | Live, LiveNoLens, Frosted; Solid under Reduce transparency |
| Glass | Live: the sampled scene behind the visible card (blur and vibrancy from WP05), the body tint over it, rim; ramps to opaque with `attach` | the glass rim | Live and LiveNoLens sample; Frosted draws Frosted; Solid draws Solid |
| Automatic | Glass under the `Glass` design style, else Solid | — | — |

- **Dark mode:** `surfaceElevated` keeps the sheet apart from dimmed black content, as iOS's elevated dark background
  does. Nothing else changes: the scrim token is per theme, and the card-stack overlay switches to `#C8C8C8`.
- **Glass sampling rules** (CS §5; WP05 builds them, §9.5):
  - sample only from outside the recorded subtree;
  - draw the dim over the sample with the same alpha as the dim over the presenter, because the recorded scene
    excludes it;
  - apply the presenter's card-stack transform to the sample;
  - never use layer alpha around a sampling surface (`handoff/WP05.md:230`);
  - read no state in Kyant's `effects`.

---

## 7. Adaptive behaviour

`SheetPlacement.Automatic` resolves from `AppTheme.containerClass`, `AppTheme.posture` and `AppTheme.input`
(`ds/theme/AppTheme.kt:110`, `:223-238` [C]). It is evaluated in measure from the container constraints, with no
`BoxWithConstraints`. `SizeClass` is equality-stable, so a resize within a class changes nothing
(`ds/foundation/adaptive/SizeClass.kt:20-29`).

| Window | Modal sheet | Non-modal sheet |
|---|---|---|
| Width Compact, height not Compact | **Bottom**: full width; floating at partial detents when allowed | Bottom, above the bottom chrome at non-modal detents |
| Height Compact (any width), e.g. a phone held sideways | **Bottom, one height**: only `Large`, `Height` and `Custom` detents that resolve; width `min(sheetMaxWidth, safe width)`, centred; never floating (ADAPTIVE-PLAN §3.5) | same, with its peek kept if it resolves |
| Width Medium and up, height Medium and up, flat | **Center (form sheet)**: `min(formSheetWidth, W − 2·gutter)` × `min(0.85 H, 620 dp, fit)`, all corners `shapes.xl`, scale-in on `dialog`, **no detents, no drag-to-dismiss**, close button always shown, dim; Escape, Back and a tap outside close it (ADAPTIVE-PLAN:434) | width Medium: Bottom, 640 wide, centred. **Width Expanded and up: Start (side sheet)**: a floating card `supportingPaneWidth` wide (360 / 412 dp, `ADAPTIVE-PLAN.md:225-231`), 16 dp from the start, top and bottom edges, keeping its vertical detents; beneath stays usable at non-modal detents (iOS 27 `.leading` [A]) |
| Tabletop | Bottom: the highest active detent below `Large` rests on the fold's bottom edge; never a centred card across the crease (today `AppSheet.kt:135-140`) | same |
| Book (vertical hinge) | Center **within the end half**, at `min(formSheetWidth, half − 2·gutter)` [D] | Start, within the start half: width `min(sideWidth, hinge.left − 2·16 dp)` |
| Explicit `Center` / `Start` / `End` / `Bottom` | honoured in every window; `Center` at Compact width becomes Bottom (a centred card on a phone reads as an alert) | — |

- **Form sheet mechanics:**
  - The same state and layout node; the detents collapse to one anchor at the centre, plus hidden.
  - The present motion is the scale-in (§5), done in the surface layer (L0), not by sliding the offset.
  - `largeFraction = 0`, so nothing recedes (iOS's form sheet dims instead [A]).
  - A downward drag is disabled. This keeps ADAPTIVE-PLAN:434 and fixes D11.
- **Fold and unfold continuity:** the placement switches at the next measure. The sheet snaps to the new frame at the
  same detent, with no animation (§4.12).
- **ChromeOS and desktop windowing:**
  - free resizing goes through the same path;
  - the caption bar is part of `safeDrawing` top (§3.1);
  - mouse: hover tooltip on the grabber, wheel → `Scrolls`, right-click does nothing on the sheet;
  - keyboard: Escape closes; Tab moves within the sheet (focus trap, §8); Enter or Space on the grabber cycles
    detents;
  - nothing locks orientation or resizability (CODE-STANDARDS L-1).
- **Large-window rows in `gates.toml`:** a `sheet_large` scenario joins `LARGE_SCENARIOS` (`tools/perf/perf.py:50`) (§16).

---

## 8. Accessibility

| Element | Semantics | Source / fixes |
|---|---|---|
| Sheet container | `isTraversalGroup = true`; `traversalIndex = 0f`; `paneTitle = title ?: "Sheet"` (string `designsystem_sheet_pane`), **always set**; modal sheets add `dialog()` ([V] ui `SemanticsProperties.kt:972`, `:1022`, `:1149`) | M3 `mat/BottomSheet.kt:334-337`, `mat/ModalBottomSheet.kt:141`; D12 |
| Grabber (48 dp band) | `Role.Button`, `onClick(label = "Resize sheet")` = cycle; `stateDescription` "Expanded" / "Partially expanded" from `currentDetent` (≤ 1 recomposition per settle); actions `expand("Expand")` when above-detent exists, `collapse("Collapse")` when below-detent exists, `dismiss("Dismiss")` when dismissible ([V] `:930`, `:1632`, `:1642`, `:1652`); focusable with the focus ring; tooltip on hover | M3 `mat/BottomSheet.kt:360-397`; MDC handle state prefix (`mat/mdc/BottomSheetDragHandleView.java:176-206`); D12 |
| Dim (modal, dismissible or with an attempt hook) | merged `contentDescription = "Close sheet"`, `onClick`, `traversalIndex = 1f` | M3 `mat/Scrim.kt:58-71`; D12 |
| Content beneath a modal sheet | `hideFromAccessibility()` on the level beneath ([V] `:1075`), applied by `Modifier.sheetPresenter()` and by the covered sheet; focus cannot leave the sheet: `focusProperties { onExit = { cancelFocusChange() } }` on the sheet's focus group | R §2.3; N8 |
| Opening | TalkBack announces the pane title. With keyboard input, `InputMode.Keyboard`, focus moves into the sheet. No `announceForAccessibility` (deprecated in API 36 [G]) | R §2.3 |
| Detent changes | announced through the grabber's state description | — |
| Close | the header's close button (`designsystem_close`); Escape; Back | — |
| Font scale 2 | header row `heightIn(min = 44.dp)`; title one line with ellipsis between reserved action widths; `FitContent` grows; screenshot-tested (§15) | D5 |
| Reduce transparency | Glass and Frosted draw Solid (WP05 Solid tier) | WP05 |
| Reduce motion | §5 | — |
| Touch targets | grabber band 48 dp; close button 48 dp through `minimumTouchTarget()` | CODE-STANDARDS API-12 |

New strings go in `core/designsystem/src/main/res/values/strings.xml` with translations in every
`generator/translations/` file (AGENTS.md):

- `designsystem_sheet_pane`
- `designsystem_sheet_resize`
- `designsystem_sheet_expand`
- `designsystem_sheet_collapse`
- `designsystem_sheet_dismiss`
- `designsystem_sheet_close_scrim`
- `designsystem_sheet_state_expanded`
- `designsystem_sheet_state_partial`
