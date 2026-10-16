# Bottom sheet system: design spec, API first

Written 2026-10-12 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). This is the "customisation and API first" design:
the smallest Compose API that still covers every sheet the user asked for, shaped by the androidx component API
guidelines (`research/CODE-STANDARDS.md` §2) and built on the mechanics WP03 already got right
(`research/sheet/current-state.md` §2.5). Android is primary; §14 and §15 give the Compose Multiplatform and SwiftUI
parity.

Nothing in a repository or worktree was changed to write it, and no Gradle or adb was run.

**Inputs.** `research/sheet/current-state.md` (what exists, defects D1–D16, A1–A5, N1–N11, seams),
`research/sheet/research.md` (iOS, Material, androidx and Navigation 3 facts), `research/CODE-STANDARDS.md`,
`research/MOTION-PLAN.md` M1/M6/M16/M17/M22/M26, `decisions/CP1.md`, `research/predictive-back/SPEC.md` §2 and §5.4,
`research/RECOMPOSITION-PLAN.md` §3, `research/ADAPTIVE-PLAN.md` §3.5 and §6, and the code cited below.

**Precedence.** `REQUIREMENTS.md` > this spec > `MASTER-PLAN.md` A3.7 and the earlier plans where they disagree. Each
place that overrides an earlier plan says **Overrides**.

**Tags** (as in the inputs). **[C]** code fact, cited `file:line`. **[V]** verified in shipped bytecode or sources.
**[A]** Apple documentation. **[G]** Google primary source. **[M]** measured by a third party (Flutter's iOS 18
constants). **[E]** measured earlier in this project. **[D]** derived here. **[U]** unverified: measure before
relying on it. A value marked **taste** is a design choice to confirm from QA2's recordings, like CP1's values.

**Path prefixes.** `ds/` = `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`.
`nav4/` = `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/`.
`nav3/` = `wt/WP03/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/`.
`glass/` = `wt/WP05/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/`.
`src/` = `research/sheet/sources/` (pinned androidx, foundation 1.12.1, MDC and Flutter copies). `ui112/` and
`fnd112/` = the Compose UI and foundation 1.12.1 sources jars in `~/.gradle/caches/modules-2/files-2.1/` (read for
this spec). `nav3rt117/` = `navigation3-runtime-android-1.1.7-sources.jar` from the same cache.

---

## 0. Decisions at a glance

| # | Question (`current-state.md` §12 and new ones) | Decision | Why |
|---|---|---|---|
| 1 | One host or two | **One component, one state type, three placements.** A destination sheet renders in WP04's presentation host (layer 3). An in-screen **modal** sheet renders in WP03's overlay host (layer 4). An in-screen **non-modal** sheet renders in place, inside its screen. All three register with one window-level `SheetStage`, so the card stack, Back, chrome, kept tabs and screen-reader isolation behave the same. | A non-modal sheet belongs to its screen: it pushes, pops and parks with it, and the tab bar stays on top. A modal sheet must cover the bar and transform the shell. |
| 2 | Rapid Back during a dismiss (N6) | **Destination sheets: counted by the presentation host's `BackGate`.** The sheet pops its entry first and slides out in `onRemove()`; presses during the slide coalesce and never go past the presentation root. **In-screen sheets: consumed and dropped** (WP03's tested behaviour). | Matches MASTER-PLAN:911 where a gate exists, and keeps "one gesture never pops the screen beneath" everywhere. |
| 3 | Glass | **`SheetMaterial.Glass` samples live only at partial detents, in the two hosts, and turns opaque at `Large`.** Below the `Live` tiers it draws Frosted; under Reduce transparency, Solid. In-place sheets never sample. **Overrides** `design/wp/WP05.md:64` ("no glass on sheets"), because REQUIREMENTS 33 came later. | iOS 26 behaviour [A]; the opaque large detent caps the cost; in-place sheets sit inside the recorded scene and would sample themselves (Kyant #100, `glass/SceneBackdrop.kt:114-126`). |
| 4 | Default detents | **`listOf(SheetDetent.Large)`**, everywhere (`rememberAppSheetState()`, `SheetSpec`). Quick choices use `AppActionSheet`/`AppChoiceSheet`, which fit their content. | iOS's default (`UISheetPresentationController.detents` defaults to `[.large()]` [A]); WP04 and WP07 already use it. |
| 5 | Detent model | **A sealed interface:** `Medium`, `Large`, `FitContent`, `Fraction`, `Height`, `Custom(id, resolver)`. Fit-content is measured in the same single measure pass, capped at `Large`. | Covers every iOS detent kind; one measure per window size stays true (§3.4). |
| 6 | Large windows | `SheetPlacement.Automatic`: a modal sheet becomes a **centred form sheet** that scales in on `dialog`, sized to its content (≤ min(0.85 H, 620 dp)), without detents or drag-to-dismiss. A non-modal sheet at Expanded width and up moves to the **leading edge** (iOS 27 `presentationPlacement(.leading)`), keeping its detents. Compact height: one full-height detent, 640 dp wide at most. | ADAPTIVE-PLAN:434 and §3.5; iOS 27 placement [A]; fixes D11. |
| 7 | Navigation inside a sheet (N5) | **A nested stack in the sheet** (`AppSheetNavHost`); the presentation stack stays flat. | MASTER-PLAN A3.7. |
| 8 | Scroll container | **The sheet adds none.** Content brings a `LazyColumn` or a `Column(Modifier.verticalScroll(…))`; the sheet pads it for its footer, the navigation bar and the keyboard itself. | A wrapper `verticalScroll` around a `LazyColumn` crashes on infinite height; D13 is a demo bug, fixed in the catalog. |
| 9 | Predictive back | **MDC bottom container** (48 dp × 24 dp, origin bottom centre) with the `(0.1, 0.1, 0, 1)` progress easing and a content counter-scale, read only in the layer. Commit dismisses; cancel springs back on `sheet`. | SPEC §5.4; Gmail's own sheets are Material's [D]; fixes D6. |
| 10 | Token home | **A new `sheet` section in `tokens/motion.json`** plus `sheetInset` and `sheetMaterial` per design style in `tokens/styles.json`; the generator writes them for Android, Multiplatform and SwiftUI. | Only the springs and `rubberBand` are tokens today (`current-state.md` §2.3). |
| 11 | Gesture engine | **Foundation's `AnchoredDraggableState`** (1.12.1), with a custom fling behaviour (projection, 2 H/s, 52 %) and an overscroll effect for the rubber band. | CODE-STANDARDS M-3. It also fixes D7 (retargets keep `lastVelocity`, `fnd112/…/AnchoredDraggable.kt:1370-1380`, `:990`), D8 (touch catches a settle, `src/foundation-1.12.1/…/AnchoredDraggable.kt:479`) and D10 (`updateAnchors` retargets in flight, `:1001-1029`). |
| 12 | When `onDismissRequest` runs | **Once, when the dismissal is decided** (release, scrim tap, Back's commit, close button), before the exit animation starts in the same frame. The sheet leaves by itself, carrying the release velocity. | A request after the slide would land while `BackGate` sees the sheet moving and be counted instead of popped (§3.6). |
| 13 | Medium height | **0.5 × the container height** (WP03 used 0.55). | "approximately half the height of the screen" [A]. **taste**: compare with WP03's 0.55 in QA2's recording. |
| 14 | Card stack geometry | **The presenter's top goes to the status bar's bottom edge**, so the 10 dp gap above a `Large` sheet shows a strip of it. **Overrides** MOTION-PLAN M6's "moves down by the status bar + 10 dp", which would hide the strip under the sheet's top edge. | iOS: sheet top 0.08 H, presenter top 0.07 H [M] (`src/flutter/sheet.dart:30`, `:64-68`); on iPhone 16 Pro 0.07 H ≈ the 62 pt safe top [D]. |
| 15 | Confirm before dismiss | **`dismissible: Boolean`** at the call site, plus **`SheetDismissGuard(enabled, onAttempt)`** inside the content (it works for destinations too). One Back handler branches; an untouched form keeps the system's predictive back. | navigation-event skill (one handler that branches); VitFleet's dirty-only interception (`current-state.md` §4.5). |
| 16 | Results | In-screen sheets: callbacks. Destination sheets: a typed, saveable `NavResultKey<R>` through `AppNavigator.popWithResult` and `NavResultEffect`. | Nav3 results recipes (event and state) lose results on process death; keys are `@Serializable` here (N-1), so results can be saved. |

---

## 1. What the API makes easy

Each block is the whole call for a common sheet. They are the acceptance examples for the API: if a change makes one
of them longer, the change is wrong.

```kotlin
// A quick in-screen sheet: full height, a title and a close button.
AppSheet(
    visible = showFilters,
    onDismissRequest = { showFilters = false },
    header = { AppSheetHeader(title = stringResource(R.string.orders_filters), trailing = { AppSheetCloseButton() }) },
) {
    FilterList()
}

// Half height that can grow, with a list handing off to the sheet.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(detents = listOf(SheetDetent.Medium, SheetDetent.Large)),
) {
    LazyColumn { items(places, key = { it.id }) { PlaceRow(it) } }
}

// A short form sized to its content, a sticky Save above the keyboard, and a confirmation while it is dirty.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(detents = listOf(SheetDetent.FitContent)),
    header = { AppSheetHeader(title = stringResource(R.string.lists_rename)) },
    footer = { AppBottomActionBar { AppButton(text = save, onClick = onSave, modifier = Modifier.weight(1f)) } },
) {
    SheetDismissGuard(enabled = state.changed) { showDiscard = true }
    RenameField(state.name, onNameChange)
}

// A Maps-style persistent sheet: a peek, half and full; the map stays usable up to half.
AppSheet(
    visible = true,
    onDismissRequest = {},
    state = rememberAppSheetState(
        detents = listOf(SheetDetent.Height(96.dp), SheetDetent.Medium, SheetDetent.Large),
    ),
    modality = SheetModality.ModalAbove(SheetDetent.Medium),
    dismissible = false,
    properties = AppSheetProperties(contentInteraction = SheetContentInteraction.Scrolls),
    header = { SearchHeader() },
) {
    PlaceList()
}

// A destination sheet, registered once; anyone navigating to the key gets a sheet over the tab bar.
navGraph {
    entry<ShareKey>(presentation = Presentation.Sheet(SheetSpec(detents = listOf(SheetDetent.Medium, SheetDetent.Large)))) { key ->
        ShareRoute(key)
    }
}

// Glass, through the liquid glass feature's tiers.
AppSheet(visible = show, onDismissRequest = { show = false }, style = AppSheetDefaults.style(material = SheetMaterial.Glass)) {
    NowPlaying()
}

// A custom detent: a tenth of the room, never under 44 dp (Apple's sample, iOS `CustomPresentationDetent`).
val Sliver = SheetDetent.Custom("sliver") { maxOf(44.dp.toPx(), maxHeight * 0.1f) }

// Choices.
AppActionSheet(visible = show, actions = actions, onDismissRequest = { show = false }, title = title)
AppChoiceSheet(
    visible = show,
    choices = languages,
    selected = current,
    onSelect = onLanguage,
    onDismissRequest = { show = false },
    label = { it.displayName },
)
```

---

## 2. Public API (Android, `:core:designsystem` and `:core:navigation`)

### 2.1 Shape of the API

| Layer | Types | Who uses it |
|---|---|---|
| Component | `AppSheet` (two overloads) | every sheet |
| State | `AppSheetState`, `rememberAppSheetState` | callers that need detents, selection or programmatic moves |
| Detents | `SheetDetent`, `SheetDetentContext` | callers with custom heights |
| Behaviour | `SheetModality`, `dismissible`, `SheetDismissGuard`, `AppSheetProperties` (`SheetPlacement`, `SheetContentInteraction`, `SheetTransition`, `SheetKeyboard`, `scrimTapDismisses`) | callers that change what the sheet does |
| Look | `AppSheetStyle`, `SheetMaterial`, `AppSheetColors`, `AppSheetDefaults` | callers that change how it looks; the theme |
| Slots and scope | `AppSheetScope`, `AppSheetGrabber`, `AppSheetHeader`, `AppSheetCloseButton`, `ProvideSheetHeader`, `ProvideSheetFooter`, `SheetBackHandler`, `LocalSheetSurface` | content, including a destination's screen |
| Presets | `AppActionSheet`, `SheetAction`, `AppChoiceSheet` | quick choices |
| Navigation (WP04) | `Presentation.Sheet(SheetSpec)`, `AppSheetNavHost`, `NavResultKey`, `popWithResult`, `NavResultEffect` | features |
| Seams (not for features) | `SheetStage`, `LocalSheetStage`, `Modifier.recedeBehindSheets`, `SheetBackdrop`, `LocalSheetBackdrop` | the shell (WP04), liquid glass (WP05) |

Rules the whole surface follows (CODE-STANDARDS §2):

- One `Modifier` parameter, applied first to the sheet **surface** (not to the window-filling host box), so
  `Modifier.testTag` and `semantics` land on the sheet (API-3).
- Styling lives in one `AppSheetStyle` value built by `AppSheetDefaults.style()`; behaviour lives in a handful of
  plain parameters and one `AppSheetProperties` value for the rarely changed ones (API-6, API-7). No `title: String`,
  `showCloseButton` or `showHandle` flags: those are slots with ready-made content (API-5).
- Every frequently changing value is a `@FrequentlyChangingValue` getter read in layout or draw (API-8, R7). No
  `State<T>` parameters.
- `AppSheetState` is a concrete `@Stable` class, as material3's `SheetState` is. **Deviation** from API-8's
  "interface plus factory": the anchors-in-measure contract between the state and the layout is internal, and a
  second implementation would need it.

### 2.2 `AppSheet`

File `ds/component/sheet/AppSheet.kt`.

```kotlin
/**
 * A sheet: a surface that rises from the bottom edge, rests at one of its [state]'s detents, follows
 * the finger between them, and goes away when it is dragged down, when the dimmed screen behind it
 * is tapped, or on Back.
 *
 * Use it for a task that belongs to the screen it opens from: a filter, a short form, a picker. A
 * sheet that is a destination of its own is registered with
 * `entry<K>(presentation = Presentation.Sheet(…))` and presented by the shell. For a list of choices
 * use [AppActionSheet] or [AppChoiceSheet].
 *
 * Keep it composed and drive [visible]: it animates in and out by itself, and showing it again
 * while it leaves turns it around from where it is, at the speed it had. When the person dismisses
 * it, [onDismissRequest] runs once, at the moment the dismissal is decided (the release, the tap,
 * Back's commit), and the sheet is already leaving; set [visible] to false there.
 *
 * A [SheetModality.Modal] sheet draws in the window's overlay layer, over the tab bar. It dims and
 * blocks what is behind it, hides it from screen readers, and makes the shell recede when it
 * reaches [SheetDetent.Large], as iOS's card stack does. Any other modality draws in place, inside
 * the screen that composes it, above that screen's bottom chrome.
 *
 * The content is measured once per window size and only moved after that: a drag, a settle and
 * the keyboard re-place it, and never recompose or re-measure it. The sheet adds no scroll
 * container; tall content brings its own, and scrolling hands off to the sheet at either end (see
 * [SheetContentInteraction]). The sheet pads its content for the footer, the navigation bar and the
 * keyboard, so a list inside needs no inset padding of its own.
 *
 * @param dismissible false makes every dismissal an attempt: a drag rubber-bands below the lowest
 *   detent and springs back, and the scrim, Back and the close button go to the content's
 *   [SheetDismissGuard], if any. Programmatic hiding and [visible] are not affected.
 * @param grabber drawn over the top edge, outside the header; `null` for none. The default hides
 *   itself when the sheet cannot be resized or dismissed by dragging.
 * @param header pinned above the content; it never scrolls, and it draws a hairline once the content
 *   has scrolled under it. Usually an [AppSheetHeader]. Content can replace it with
 *   [ProvideSheetHeader].
 * @param footer pinned to the sheet's visible bottom at every detent, above the navigation bar and
 *   the keyboard. Usually an `AppBottomActionBar`. Content can replace it with [ProvideSheetFooter].
 */
@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
)

/**
 * The sheet without its own visibility, for a presenter that owns where it is drawn: the shell's
 * sheet scene, or a custom host. It draws where it is composed, and appears and leaves only through
 * [state]'s `show()` and `hide()`; keep it composed until `hide()` returns. App code uses the
 * overload that takes `visible`.
 */
@Composable
fun AppSheet(
    state: AppSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
)
```

Parameter order follows API-2: required, `modifier`, the state (as material3's `ModalBottomSheet` places
`sheetState`), behaviour, the `XDefaults` value, then slots. Twelve parameters against material3's thirteen, and only
one of them is visual.

How the `visible` overload works (it fixes D1, D9 and D15):

- One `AppSheetState` per call site, not one per opening (WP03 made a fresh state per session,
  `ds/component/overlay/AppSheet.kt:273-286`). `LaunchedEffect(visible) { if (visible) state.show() else state.hide() }`.
- The sheet stays composed while `visible || state.isVisible`. A `show()` during a `hide()` takes the same mutex and
  animates back from the current offset with `lastVelocity`; nothing is left half-way with `dismissing` stuck true.
- Placement by modality: `Modal` → `HostedOverlay(modal = true)` (`AppOverlayHost.kt:81-100` [C]); any other modality
  → in place.
- A restored state (process death) shows at its saved detent with no slide (§3.10).

### 2.3 `AppSheetState`

File `ds/component/sheet/AppSheetState.kt`.

```kotlin
/**
 * Where a sheet rests and how it moves, kept outside composition so a presenter can show it, hide
 * it, and read its position from layout and draw.
 *
 * It wraps a foundation `AnchoredDraggableState`: a drag, a fling, a programmatic change and a
 * window resize all go through one mutex, so any of them interrupts another and the motion goes on
 * from the current offset at the current velocity.
 *
 * [detents] may change while the sheet is up, as `UISheetPresentationController.detents` can. The
 * sheet re-resolves them in its next layout and, if the detent it rests on is gone, moves to the
 * nearest one that is left.
 */
@Stable
class AppSheetState @RememberInComposition constructor(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
) {
    /** The heights the sheet can rest at, in any order; at least one. */
    var detents: List<SheetDetent>

    /** The detent the sheet rests on, or null while it is hidden or leaving. Changes once per settle. */
    val currentDetent: SheetDetent?

    /** Where the sheet is going: the detent it will rest on, or null while it leaves or is hidden. */
    val targetDetent: SheetDetent?

    /** True from `show()` until the sheet has left after `hide()` or a dismissal. */
    val isVisible: Boolean

    /** True while the sheet moves: a drag, a settle, showing or leaving. */
    val isMoving: Boolean

    /** The sheet's top edge, in pixels from the top of the area it is shown in; NaN before its first layout. */
    @get:FrequentlyChangingValue
    val offset: Float

    /** 0 while hidden, 1 at the lowest active detent and above. The scrim follows it. */
    @get:FrequentlyChangingValue
    val presentedFraction: Float

    /**
     * 0 at the highest detent below [SheetDetent.Large] (or hidden, when Large is the only detent),
     * 1 at Large; 0 for a form or side sheet. The shell recedes by it, a floating sheet attaches to
     * the edges by it, and glass turns opaque by it.
     */
    @get:FrequentlyChangingValue
    val largeFraction: Float

    /** Shows the sheet at [detent], or at its initial detent; a sheet that is leaving turns around. */
    suspend fun show(detent: SheetDetent? = null)

    /** Slides the sheet away. Idempotent: a second call joins the running exit. */
    suspend fun hide()

    /** Moves to [detent], keeping the current velocity. Ignored while the sheet is leaving or hidden. */
    suspend fun animateTo(detent: SheetDetent)

    suspend fun snapTo(detent: SheetDetent)

    companion object {
        /**
         * Saves whether the sheet is up and the index of the detent it rests on; [detents] come back
         * from the caller, because a custom detent's resolver cannot be saved.
         */
        fun Saver(detents: List<SheetDetent>): Saver<AppSheetState, *>
    }
}

/** An [AppSheetState] that survives configuration changes and process death. */
@Composable
fun rememberAppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState
```

Notes for the implementer (WP03):

- Inside, `AnchoredDraggableState<SheetStop>`, where `SheetStop` is `Hidden` or `Rest(index)`. Detents resolve to
  `DraggableAnchors` in measure (§3.4). A non-dismissible sheet has **no `Hidden` anchor** while it rests; that is
  foundation's replacement for the deprecated `confirmValueChange` (`src/foundation-1.12.1/…/AnchoredDraggable.kt:1716-1735`).
  `hide()` adds the anchor for its own run.
- `currentDetent` and `targetDetent` are `derivedStateOf` over the foundation state's `currentValue` and
  `targetValue`, which recompose only when they change (`AnchoredDraggable.kt:871`).
- `rememberAppSheetState` writes a changed `detents` list into the state from a `SideEffect`, only when it differs.
- The motion specs come from `AppTheme.springs` through the layout, not through fields written in composition
  (D14): the layout node holds them and the state's suspend functions ask the attached node for them; before the
  first layout every request waits for the anchors, as WP03's `laidOut()` does (`AppSheetState.kt:132`).
- `isMoving` is the foundation state's `isAnimationRunning` or an active drag (`AnchoredDraggable.kt:935`).

### 2.4 Detents

File `ds/component/sheet/SheetDetent.kt`.

```kotlin
/**
 * A height a sheet can rest at: from the bottom of the area it is shown in up to its top edge.
 * Resolved in layout from the container's height, its size class, the content and the system bars,
 * whenever one of them changes.
 */
@Immutable
sealed interface SheetDetent {

    /** About half the window, as iOS's medium. Off in Compact height, where only Large has room. */
    data object Medium : SheetDetent

    /**
     * The full height, its top [AppSheetDefaults.LargeTopGap] below the status bar. The only detent
     * at which the shell recedes behind a modal sheet.
     */
    data object Large : SheetDetent

    /** The sheet's own measured height (grabber, header, content and footer), capped at Large. */
    data object FitContent : SheetDetent

    /** [fraction] of the container's height, capped at Large. */
    data class Fraction(val fraction: Float) : SheetDetent {
        init { require(fraction > 0f && fraction <= 1f) { "A fraction is in (0, 1]: $fraction" } }
    }

    /** A fixed height, capped at Large: a peek that shows a header. */
    data class Height(val height: Dp) : SheetDetent

    /**
     * A height from [resolve], in pixels, called whenever the context changes; null switches the
     * detent off in that context, as iOS's custom resolvers do. Two Custom detents with the same [id]
     * are the same detent, so the lambda may be created in composition.
     */
    class Custom(val id: String, val resolve: SheetDetentContext.() -> Float?) : SheetDetent {
        override fun equals(other: Any?): Boolean = other is Custom && other.id == id
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "Custom($id)"
    }
}

/** What a [SheetDetent.Custom] resolver can read. Pixels throughout. */
interface SheetDetentContext : Density {
    /** Large's height: the most any detent shows. iOS's `maximumDetentValue`. */
    val maxHeight: Float
    val containerHeight: Float
    /** The sheet's measured height: what FitContent resolves to. */
    val contentHeight: Float
    val sizeClass: SizeClass
    /** The bottom of a tabletop fold, from the container's top, or null. */
    val foldBottom: Float?
    /** Another detent's height in this context, or null when it is off: `heightOf(Medium)!! + 40.dp.toPx()`. */
    fun heightOf(detent: SheetDetent): Float?
}
```

Resolution rules (WP03, `layout/SheetAnchors.kt`; unit-tested in `SheetDetentResolutionTest`):

| # | Rule |
|---|---|
| R-1 | `Large` = `containerHeight − (statusBarTop + LargeTopGap)`; for a sheet stacked on another, minus `stack.stackedLift · H` (§4). It is `maxHeight`. |
| R-2 | `Medium` = `mediumFraction · containerHeight`, capped at `maxHeight`; **off** when the height class is Compact [A]. In tabletop posture it rests on the fold: its top at `foldBottom` (WP03 behaviour, `AppSheet.kt:235`). |
| R-3 | `FitContent` = the sheet column's measured height (grabber band, header, content and footer) plus the bottom system inset, capped at `maxHeight`. |
| R-4 | `Fraction(f)` = `f · containerHeight`; `Height(h)` = `h` in pixels; both capped at `maxHeight`. |
| R-5 | `Custom` = the resolver's value, capped at `maxHeight`; `null` switches it off. |
| R-6 | Detents that resolve within 1 px of each other merge into the first in list order, so `[FitContent, Large]` with a full-height list is one stop. |
| R-7 | Stops are ordered by height; list order matters only for R-6 ties and for `initialDetent`. |
| R-8 | If every detent is off, the sheet uses `Large` (iOS keeps at least one). |
| R-9 | A centred form sheet has one stop, its fitted height (§6); a leading or trailing sheet keeps its detents. |
| R-10 | When the stops change (rotation, unfold, resize, new `detents`, content height), a sheet keeps its detent by identity, and an animation in flight is retargeted to the new position (`updateAnchors(new, newTarget = targetValue)`). |

### 2.5 Behaviour types

File `ds/component/sheet/SheetModality.kt` and `AppSheetProperties.kt`.

```kotlin
/** Whether what is behind the sheet stays usable. */
@Immutable
sealed interface SheetModality {

    /** Dims and blocks what is behind at every detent; drawn over the tab bar. The shell recedes at Large. */
    data object Modal : SheetModality

    /** Never dims; what is behind stays usable. Drawn in place, above the screen's bottom chrome. */
    data object NonModal : SheetModality

    /**
     * Usable behind while the sheet rests at [detent] or lower, as iOS's
     * `presentationBackgroundInteraction(.enabled(upThrough:))`. Above it the scrim fades in over the
     * travel to the next detent up, and blocks. Drawn in place.
     */
    data class ModalAbove(val detent: SheetDetent) : SheetModality
}

/** How the person tried to dismiss a sheet. */
enum class SheetDismissSource { Drag, Scrim, Back, Escape, Request }

/** Behaviour a sheet seldom changes. */
@Immutable
data class AppSheetProperties(
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Resizes,
    val transition: SheetTransition = SheetTransition.Slide,
    val keyboard: SheetKeyboard = SheetKeyboard.Lift,
    /** False leaves a scrim tap doing nothing; drag and Back still dismiss. */
    val scrimTapDismisses: Boolean = true,
)

/** Where the sheet sits. Automatic follows the window (§6), as iOS 27's `presentationPlacement(.automatic)`. */
enum class SheetPlacement { Automatic, Bottom, Center, Leading, Trailing }

/** What an upward scroll inside the sheet does first. */
enum class SheetContentInteraction {
    /** Grows the sheet to its next detent, then scrolls: iOS's default. */
    Resizes,

    /** Scrolls at every detent; the sheet resizes from its grabber, header and footer. */
    Scrolls,
}

/** How the sheet arrives and leaves. Reduce motion turns every transition into [CrossFade]. */
@Immutable
sealed interface SheetTransition {
    data object Slide : SheetTransition
    data object CrossFade : SheetTransition
}

/** What the sheet does when the keyboard rises. */
enum class SheetKeyboard {
    /** Rises with the keyboard, keeping its height, up to Large. */
    Lift,

    /** Goes to its tallest detent while the keyboard is up, and back when it hides. */
    Expand,
}
```

`SheetTransition` is sealed rather than an enum so a `Zoom(source)` transition (iOS 26's sheet morphing out of its
button, MOTION-PLAN M26) can be added later without breaking callers (API-14). It is not in v1 (§2.12).

Content-side behaviour (`AppSheetScope.kt`):

```kotlin
/**
 * Makes the enclosing sheet's dismissals attempts while [enabled]: a drag past the dismiss point
 * springs back, and the scrim, Back, Escape and the close button call [onAttempt] instead, once per
 * attempt. Enable it only while there is something to lose, so an untouched form keeps the system's
 * predictive back. Outside a sheet it does nothing; a screen guards Back with the navigator's guard.
 */
@Composable
fun SheetDismissGuard(enabled: Boolean = true, onAttempt: (SheetDismissSource) -> Unit)
```

The guard and the `dismissible` parameter combine: dismissal is blocked if either says so; attempts go to the
innermost enabled guard.

### 2.6 Style

Files `ds/component/sheet/AppSheetStyle.kt` and `AppSheetDefaults.kt`.

```kotlin
/** What the sheet's body is made of. */
enum class SheetMaterial {
    /** The design style's choice (`tokens/styles.json` `sheetMaterial`); Solid unless the style says otherwise. */
    Automatic,

    /** Opaque [AppSheetColors.container]. The cheapest, and what every material becomes under Reduce transparency. */
    Solid,

    /** Drawn translucency (the container at 92–94 %, a rim and a sheen). Samples nothing: a few cached brush draws. */
    Frosted,

    /**
     * Live liquid glass at partial detents, turning opaque at Large, where the device's glass tier
     * samples and the sheet sits outside the recorded scene; Frosted elsewhere. Needs the liquid glass
     * feature for the live tier.
     */
    Glass,
}

/** The sheet's colours. */
@Immutable
class AppSheetColors(
    val container: Color,
    val content: Color,
    val scrim: Color,
    val grabber: Color,
    val separator: Color,
)

/**
 * How a sheet looks: material, colours, corner, floating inset, width and shadow. Build it with
 * [AppSheetDefaults.style]. Each field maps to a property of Compose's `Style`, so moving the sheet
 * to `Modifier.styleable` is mechanical (see the mapping in the design spec, §2.6).
 */
@Immutable
class AppSheetStyle internal constructor(
    val material: SheetMaterial,
    val colors: AppSheetColors,
    /** The top corners; the bottom corners follow the display's when the sheet floats. */
    val cornerRadius: Dp,
    /** The gap to the side and bottom edges at partial detents; 0 keeps the sheet attached. */
    val floatingInset: Dp,
    val maxWidth: Dp,
    /** Drawn outside the shape only; null for none. */
    val shadow: Shadow?,
) {
    fun copy(
        material: SheetMaterial = this.material,
        colors: AppSheetColors = this.colors,
        cornerRadius: Dp = this.cornerRadius,
        floatingInset: Dp = this.floatingInset,
        maxWidth: Dp = this.maxWidth,
        shadow: Shadow? = this.shadow,
    ): AppSheetStyle
}

object AppSheetDefaults {
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)

    /** The gap between the status bar and a Large sheet's top edge. */
    val LargeTopGap: Dp = 10.dp
    const val MediumFraction: Float = 0.5f
    val GrabberSize: DpSize = DpSize(36.dp, 5.dp)
    val GrabberTop: Dp = 5.dp
    val HeaderMinHeight: Dp = 44.dp
    val FormMaxHeight: Dp = 620.dp
    const val FormMaxHeightFraction: Float = 0.85f
    val SideInset: Dp = 16.dp

    val Grabber: @Composable AppSheetScope.() -> Unit = { AppSheetGrabber() }

    @Composable
    fun colors(
        container: Color = AppTheme.colors.surface,
        content: Color = AppTheme.colors.contentPrimary,
        scrim: Color = AppTheme.colors.scrim,
        grabber: Color = AppTheme.colors.contentTertiary.copy(alpha = GrabberAlpha),
        separator: Color = AppTheme.colors.separator,
    ): AppSheetColors

    /** For inset-grouped content (`AppSection`s), as iOS's grouped sheets: the grouped background. */
    @Composable
    fun groupedColors(): AppSheetColors

    /** The shadow of a floating or non-modal sheet: soft in light theme, none in dark, where a rim shows the edge. */
    @Composable
    fun shadow(): Shadow?

    @Composable
    fun style(
        material: SheetMaterial = SheetMaterial.Automatic,
        colors: AppSheetColors = colors(),
        cornerRadius: Dp = AppTheme.style.sheetRadius,
        floatingInset: Dp = AppTheme.style.sheetInset,
        maxWidth: Dp = AppTheme.layout.sheetMaxWidth,
        shadow: Shadow? = shadow(),
    ): AppSheetStyle
}
```

`AppStyle` (`ds/theme/AppDesignStyle.kt:154-168` [C]) gains `sheetRadius: Dp` (today only a `Shape` exists,
`ds/theme/AppMetrics.kt:47`), `sheetInset: Dp` and `sheetMaterial: SheetMaterial`, from `tokens/styles.json`.

**Mapping to the Styles API** (`androidx.compose.foundation.style`, present in foundation 1.12.1 behind
`@ExperimentalFoundationStyleApi`, `fnd112/commonMain/androidx/compose/foundation/style/ExperimentalFoundationStyleApi.kt:23`
[V]; the Android styles skill says it is experimental and needs an opt-in). It is not adopted now. When it is, the
sheet takes `style: Style = Style` and applies `Modifier.styleable(styleState, AppTheme.styles.sheet, style)`
(`StyleModifier.kt:113` [V]); each field moves as follows:

| `AppSheetStyle` | `StyleScope` (`fnd112/…/style/StyleScope.kt`) | Notes |
|---|---|---|
| `colors.container` | `background(color)` `:1363` | the Solid body; Frosted and Glass stay in the surface node |
| `colors.content` | `contentColor(color)` `:1607` | inherited by children, as `LocalContentColor` is today |
| `cornerRadius` | `shape(RoundedCornerShape(top = r))` `:1439` | the floating bottom corners stay in the node: they follow `largeFraction` |
| `shadow` | `dropShadow(shadow)` `:1465` | the outer-only cut stays in the node (§5.6) |
| `maxWidth` | `maxWidth(dp)` `:958` | |
| `floatingInset` | `externalPadding(…)` `:349-403` | at rest only; the morph stays in the node |
| `material`, `colors.scrim`, `colors.grabber`, `colors.separator` | none | behaviour, or other nodes (scrim, grabber, header) with styles of their own |

The rule that keeps the migration mechanical: **no visual value is read anywhere except from `AppSheetStyle`**, and
`AppSheetStyle` holds nothing that is not visual.

### 2.7 Scope, slots and content-side API

File `ds/component/sheet/AppSheetScope.kt`, plus `header/`.

```kotlin
/** What the sheet's slots and content can reach. */
@Stable
interface AppSheetScope {
    val sheetState: AppSheetState

    /** Dismisses as the person would: through [SheetDismissGuard] while dismissal is blocked. */
    fun requestDismiss()

    /**
     * How far the content has scrolled under the header, 0 to 1 over the first
     * `sheet.header.scrollEdge` (8 dp). Read it in draw.
     */
    @get:FrequentlyChangingValue
    val contentScrolled: Float

    /** True while another sheet is stacked on this one. Changes once per stack change. */
    val isCovered: Boolean
}

/** The grabber: drag, tap or press Enter to step through the detents; screen readers get the same as actions. */
@Composable
fun AppSheetScope.AppSheetGrabber(modifier: Modifier = Modifier)

/**
 * A sheet's title row: [title] centred between [leading] and [trailing] (Cancel, Done, a close
 * button), never under them. It grows with the font size and draws a hairline once the content has
 * scrolled under it.
 */
@Composable
fun AppSheetScope.AppSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
)

@Composable
fun AppSheetScope.AppSheetHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
)

/** A round close button that dismisses the sheet through [AppSheetScope.requestDismiss]. */
@Composable
fun AppSheetScope.AppSheetCloseButton(modifier: Modifier = Modifier)

/**
 * Puts [content] in the enclosing sheet's header slot, replacing the `header` parameter. Outside a
 * sheet it draws in place, so a screen can be shown full screen and as a sheet unchanged.
 */
@Composable
fun ProvideSheetHeader(content: @Composable () -> Unit)

/** Puts [content] in the enclosing sheet's footer slot, pinned to its visible bottom; in place outside a sheet. */
@Composable
fun ProvideSheetFooter(content: @Composable () -> Unit)

/**
 * Takes Back (the key, a committed gesture, Escape) inside the enclosing sheet while [enabled],
 * instead of closing it: a step back in a stack the sheet holds. The sheet does not shrink for it.
 */
@Composable
fun SheetBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * The sheet a composable is drawn in, or null outside one. `AppScaffold` lets the sheet's material
 * show instead of painting its own background; `AppTopBar` drops the status-bar inset and shows a
 * close button where it would show Back at the root; `AppBottomActionBar` takes the material.
 */
val LocalSheetSurface: ProvidableCompositionLocal<SheetSurface?>

@Immutable
class SheetSurface internal constructor(val material: SheetMaterial, val container: Color, val scope: AppSheetScope)
```

Slot rules:

- **Grabber.** Drawn over the top band; it never takes layout room from the header. Hidden automatically when the
  sheet has one stop and cannot be dragged away, when it is covered by another sheet (iOS hides it then,
  `prefersGrabberVisible` [A]), and for a centred form sheet.
- **Header.** Sticky: measured once, placed above the content box, never scrolled. `AppSheetHeader`'s title is
  measured between equal reserved widths (the wider of the two slots), so it stays centred and ellipsizes before it
  reaches a button (fixes D5). The row is at least 44 dp and grows with the font (no fixed height).
- **Footer.** Measured separately and placed at the sheet's visible bottom (§3.4), over the content. The footer's
  slot consumes `navigationBars ∪ ime` at the bottom, so an `AppBottomActionBar` inside it pads nothing twice
  (`ds/component/container/AppBottomActionBar.kt:39` pads that union today; fixes D2). The sheet draws its material
  under the navigation-bar strip below the footer.
- **Content.** `AppSheetScope` receiver, no layout scope: content brings its own `Column` or `LazyColumn`. It is
  padded by the sheet (§3.4); inside a sheet `LocalBottomChrome` is a zero holder, so `chromeContentPadding()` adds
  nothing there and a screen reused inside a sheet does not pad for a tab bar that is not under it.
- **Content-side providers** (`ProvideSheetHeader`, `ProvideSheetFooter`, `SheetDismissGuard`, `SheetBackHandler`)
  reach the sheet through an internal `LocalAppSheetScope`. They are how a destination's screen, which is composed
  by the navigation layer and never sees the `AppSheet` call, customises its sheet. Each one registers in a
  `DisposableEffect`; the innermost enabled one wins.

### 2.8 Presets

`ds/component/sheet/action/AppActionSheet.kt`:

```kotlin
/**
 * "What would you like to do with this?": actions in a sheet that fits them, with Cancel apart. On a
 * window of Medium width or more it becomes a popover anchored where it is composed, so compose it
 * inside the element it belongs to.
 */
@Composable
fun AppActionSheet(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String? = stringResource(R.string.designsystem_cancel),
)

@Immutable
class SheetAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    /** Draws a checkmark and announces the row as selected (QA-14). */
    val selected: Boolean = false,
)
```

It is an `AppSheet` with `[FitContent, Large]`, the grouped look (two `surfaceElevated` groups), rows of 56 dp, the
`dialog` spring for arrival (M17), and the content scrolled when it is taller than `Large` (fixes A1). Back during
its exit is consumed like any sheet's (A3). `selected` fixes A2. A1–A5 are in `current-state.md` §3.

`ds/component/sheet/choice/AppChoiceSheet.kt`:

```kotlin
/**
 * One choice from a list, with the current one checked: a language, a unit, a category. Rows have
 * the radio role; a search field appears from [AppChoiceSheetDefaults.SearchFrom] choices; [noneLabel]
 * adds a row that selects nothing. Choosing closes the sheet.
 */
@Composable
fun <T> AppChoiceSheet(
    visible: Boolean,
    choices: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    onDismissRequest: () -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    title: String? = null,
    noneLabel: String? = null,
    key: (T) -> Any = { it as Any },
)

object AppChoiceSheetDefaults {
    const val SearchFrom: Int = 8
    /** The fitted height's cap before the list scrolls, as a fraction of the window. */
    const val MaxFitFraction: Float = 0.6f
}
```

Detents: a `Custom("choice-fit")` that resolves to `min(contentHeight, 0.6 · containerHeight)`, plus `Large`, so a
long list opens at 60 % and grows (VitFleet's `ChoiceSheet`, `current-state.md` §11). The settings language picker
moves from `AppActionSheet` to `AppChoiceSheet` (WP02-M3).

A persistent (Maps-style) sheet is not a separate component: it is `AppSheet` with `ModalAbove`, `dismissible = false`
and `Scrolls` (§1). The catalog shows it.

### 2.9 Navigation 3 (WP04, `:core:navigation`)

**Registration and metadata** (`nav4/Presentation.kt`, replacing `:8-24`):

```kotlin
sealed interface Presentation {
    /** A sheet over the current screen, which stays composed and visible beneath it. Always modal. */
    data class Sheet(val spec: SheetSpec = SheetSpec()) : Presentation

    data object Cover : Presentation
}

/** How a destination presented as a sheet rests and leaves. Its look comes from the theme's sheet style. */
@Immutable
data class SheetSpec(
    val detents: List<SheetDetent> = AppSheetDefaults.Detents,
    val initialDetent: SheetDetent = detents.first(),
    val dismissible: Boolean = true,
    val material: SheetMaterial = SheetMaterial.Automatic,
    val properties: AppSheetProperties = AppSheetProperties(),
)
```

- `SheetDetent` is the design system's. WP04's own `enum class SheetDetent` (`nav4/Presentation.kt:18-24`), WP03's
  `SheetPresentation` (`nav3/SheetScene.kt:24-28`) and WP07's copy go (N3).
- The metadata is written under a typed key, `internal object PresentationKey : NavMetadataKey<Presentation>`, with
  `metadata { put(PresentationKey, presentation) }` (the recipe's pattern, `src/nav3-recipes-BottomSheetSceneStrategy.kt:85-90`;
  `NavMetadataKey` exists in 1.1.7 too, `nav3rt117/…/NavMetadataKey.kt` [V]). The string key and the `as?` read
  (`nav4/presentation/PresentationHost.kt:198-205`) go.
- A destination's look beyond the material comes from the theme (`AppSheetDefaults.style()`); its header and footer
  come from its own screen through `ProvideSheetHeader`/`ProvideSheetFooter`, and its confirm hook through
  `SheetDismissGuard`. No styling travels through metadata.

**Scene strategy** (`nav4/presentation/sheet/`, all `internal`):

```kotlin
internal class SheetSceneStrategy(
    private val sessions: SheetSessions,
    private val isPresented: (contentKey: Any) -> Boolean,
) : SceneStrategy<AppNavKey> {
    override fun SceneStrategyScope<AppNavKey>.calculateScene(
        entries: List<NavEntry<AppNavKey>>,
    ): Scene<AppNavKey>? {
        val top = entries.lastOrNull() ?: return null
        val sheet = top.metadata[PresentationKey] as? Presentation.Sheet ?: return null
        val below = entries.dropLast(1)
        if (below.isEmpty()) return null
        return SheetScene(top, below, sheet.spec, sessions, isPresented, onBack)
    }
}
```

- `calculateScene` **mutates nothing.** Navigation 3 calls it once per overlay level on every calculation
  (`current-state.md` §4.2 [V]), which is why WP03's `states.clear()`/`retainAll` lost the state of every sheet but
  the top one (N1).
- **`SheetSessions`** holds one `AppSheetState` per `contentKey`. The scene's content gets or creates it in
  composition; it is removed by a `NavEntryDecorator`'s `onPop`, which runs only when the last entry with that key
  has left the back stack **and** composition (`nav3rt117/…/NavEntryDecorator.kt:70-88`,
  `DecoratedNavEntries.kt:205-223` [V]), so after `onRemove()` has finished. The decorator joins the host's list next
  to the saveable and ViewModel decorators (`nav4/presentation/PresentationHost.kt:92-94`). Its saver keeps
  `contentKey.toString()` → (detent index), so a sheet comes back at its detent after process death, with no slide.
- **`SheetScene`** (a class with `equals`/`hashCode` over the entry, the keys beneath and the spec; lint
  `SceneEqualsHashCode`; `key = entry.contentKey`):
  - `content`: `PresentedSheetFrame(state, gate) { AppSheet(state = state, onDismissRequest = onBack, modality = Modal, dismissible = spec.dismissible, properties = spec.properties, style = AppSheetDefaults.style(material = spec.material)) { entry.Content() } }`, and
    `LaunchedEffect(state) { state.show() }` (a no-op for a sheet already up).
  - `onRemove()`: if the entry is still presented (`isPresented(contentKey)`: a cover was pushed over the sheet),
    nothing animates; otherwise `sessions.peek(contentKey)?.hide()`. The recipe's `lateinit` state
    (`src/nav3-recipes-BottomSheetSceneStrategy.kt:31`) is not copied: `peek` is null-safe when the scene never
    composed.
  - Navigation 3 keeps a popped overlay composed until `onRemove()` returns (`src/NavDisplay.kt:874-881` [V]) and renders
    the first instance it saw for a key (`:686-698`), so the session map, not the scene instance, is the source of
    truth.
- **`PresentedSheetFrame`** replaces `PresentedFrame` (`nav4/presentation/PresentationHost.kt:161-196`) for sheet entries
  (N4):
  - no opaque `background(colors.background)`, no card `graphicsLayer`;
  - `LocalTabBarVisible provides false`; the sheet provides its own zero `LocalBottomChrome`;
  - it reports the **sheet's** motion to the host's gate: `gate.report(frame, moving)` with
    `moving = state.isMoving && remaining travel > 1 %`, collected with `snapshotFlow` in an effect (never read in
    composition). `LocalNavAnimatedContentScope` is the scene beneath in an overlay (`current-state.md` §4.2 [V]) and
    is not used.
- `AppShell(sheetStrategy = …)` (`nav4/AppShell.kt:96`) stops being a parameter: the presentation host creates its
  strategy itself (it needs the sessions and the presented keys), so N2 cannot recur. A sheet key never falls back to
  a cover.

**Navigation inside a sheet** (`nav4/presentation/sheet/AppSheetNavHost.kt`, public):

```kotlin
/**
 * Steps of a flow that stay inside a sheet: a nested stack with its own push and pop, sized to the
 * sheet. Back pops this stack first and closes the sheet from its root. Each step can set the
 * sheet's header and footer with ProvideSheetHeader and ProvideSheetFooter. Outside a sheet it is
 * an ordinary single-pane host.
 */
@Composable
fun AppSheetNavHost(
    backStack: NavBackStack<AppNavKey>,
    modifier: Modifier = Modifier,
    entryProvider: (AppNavKey) -> NavEntry<AppNavKey>,
)
```

It is a `NavDisplay` with the template's push/pop (on `screen`, parallax 0.333), clipped to the sheet, with
`SheetBackHandler(enabled = backStack.size > 1) { backStack.removeLastOrNull() }`. Its keys are ordinary `AppNavKey`s
with entries the feature provides; they never reach `ShellState`. Navigating to a *registered* key from inside a
sheet still goes to the presentation stack, as today (`nav4/ShellState.kt:116`, `:178`).

**Results** (`nav4/result/`):

```kotlin
/** A result a presented destination hands back. Declared beside the key that produces it. */
class NavResultKey<R : Any>(val name: String, internal val serializer: KSerializer<R>)

/** Pops the presented destination on top and delivers [value] to whoever waits on [key]. */
fun <R : Any> AppNavigator.popWithResult(key: NavResultKey<R>, value: R)

/**
 * Calls [onResult] once for each result delivered to [key], while this composable's lifecycle is at
 * least RESUMED, so a screen in a hidden tab does not take it.
 */
@Composable
fun <R : Any> NavResultEffect(key: NavResultKey<R>, onResult: (R) -> Unit)
```

Results are kept as JSON in `ShellState`'s saved state until delivered, so a result sent just before process death
still arrives. In-screen sheets need none of this: their content calls the screen's callbacks.

### 2.10 The stage and the card stack seam

`ds/component/sheet/stack/SheetStage.kt` and `RecedeBehindSheets.kt` (WP03 writes them; WP04 applies them):

```kotlin
/**
 * The sheets up in a window, bottom to top, for what reacts to them as a group: the shell receding
 * behind the first modal sheet, a covered sheet receding behind the one on it, the status bar's
 * icons, and screen readers. The outermost AppTheme provides one; every AppSheet registers itself.
 */
@Stable
class SheetStage internal constructor() {
    /** How far the shell recedes, 0 to 1: the bottom-most modal sheet's `largeFraction`. */
    @get:FrequentlyChangingValue
    val recede: Float

    /** True while a modal sheet is up or arriving. Changes once per present and dismiss. */
    val hasModalSheet: Boolean
}

val LocalSheetStage: ProvidableCompositionLocal<SheetStage?>

/**
 * Recedes the content behind sheets, as iOS's card stack does: it scales about its top centre, moves
 * down to the status bar's edge, rounds its corners and darkens under a drawn overlay, over a black
 * window. Everything is read in this node's layer and draw, so the content never recomposes.
 */
fun Modifier.recedeBehindSheets(stage: SheetStage, enabled: Boolean = true): Modifier
```

- Registration is per sheet, in a `DisposableEffect`, with its modality and placement. Only a modal `Bottom` sheet
  with a `Large` detent drives `recede`; a form, side or non-modal sheet never does.
- Covered sheets read the stage for the sheet above them (`isCovered`, and the transform in §4.5).
- `ShellConfig(sheetCardStack: Boolean = true)` (WP04) turns the card stack off app-wide; `recedeBehindSheets` is then
  not applied and the scrim does the dimming alone.

### 2.11 The glass seam

`ds/component/sheet/draw/SheetBackdrop.kt` (WP03 declares it; WP05 implements it):

```kotlin
/**
 * Draws a sheet's live glass. The liquid glass feature provides it around the hosts that sit outside
 * the recorded scene; everywhere else it is absent and a Glass sheet draws Frosted.
 */
@Stable
interface SheetBackdrop {
    /**
     * The live body for [shape]: the scene behind, blurred, under [dim] of the scrim and a tint whose
     * opacity rises with [opaque] (0 glass, 1 opaque). Both are read in draw.
     */
    fun Modifier.sheetBackdrop(shape: Shape, opaque: () -> Float, dim: () -> Float): Modifier
}

val LocalSheetBackdrop: ProvidableCompositionLocal<SheetBackdrop?> = staticCompositionLocalOf { null }
```

The design system compiles without the glass package (the `liquidglass` feature removes it), so the sheet only knows
this interface.

### 2.12 Not in the API

| Left out | Why |
|---|---|
| A `Dialog`-window sheet | Glass cannot sample another window (`glass/SceneBackdrop.kt:105-113`); the card stack needs the presenter in the same window; M3's dialog sheet has a history of inset bugs (`research.md` §2.4, §3). |
| `confirmValueChange` | Deprecated in foundation; the anchor set and `SheetDismissGuard` replace it. |
| Separate switches for drag, Back and scrim | Only `scrimTapDismisses` has a real use (a sheet over a canvas); the rest is `dismissible` or the guard. |
| A per-detent background effect (iOS 26.1 `Detent.backgroundEffect`) | The `largeFraction` opacity ramp covers the shipped behaviour; no use found. |
| `Zoom` transition | v1.1, behind `SheetTransition` (§2.5). It needs a source-bounds registry and a glass morph (M26). |
| A `title: String` overload | `header = { AppSheetHeader(title) }` is one line, and a second overload doubles the surface. |
| Wrapping content in a scroll container | §0 decision 8. |

---

## 3. Behaviour

Rules are numbered so tests and reviews can cite them (`B-12 violated`). `H` is the container's height, `v` a
velocity in px/s (positive downward), and offsets are the sheet's top edge from the container's top.

### 3.1 Lifecycle

- **B-1 Show.** `show()` waits for the first layout (anchors are written in measure), then animates hidden → target on
  `springs.sheet` (0.34 s, ζ 1). The first composed frame places the sheet at `hidden`, below the window, so nothing
  flashes. The spring's clock starts after that frame (SMOOTHNESS S3, "clock after composition"): the first moving
  frame shows ≤ 4 % of the travel, CP1's acceptance for `screen` applied to sheets (`decisions/CP1.md:118-121`).
- **B-2 Hide.** Idempotent: a second `hide()` joins the running one. A `show()` during a hide takes the mutex and turns
  the sheet around from its offset with `lastVelocity` (fixes D1 and D7).
- **B-3 A user dismissal** (drag past the dismiss point, scrim, Back's commit, Escape, `requestDismiss`) calls
  `onDismissRequest` synchronously, then starts the exit in the same frame, seeded with the release velocity (0 for a
  tap or a key).
- **B-4 Programmatic moves.** `animateTo` uses `sheet` seeded with `lastVelocity`; it is ignored while the sheet
  leaves (fixes D9). `snapTo` is for restoration and tests.
- **B-5 Resize.** Rotation, unfold, freeform resize and a content-height change re-resolve the stops in measure; the
  sheet keeps its detent by identity and an animation in flight is retargeted (R-10; fixes D10).
- **B-6 Detents changed while up.** As B-5; if the current detent is gone, the sheet animates to the nearest stop by
  height.

### 3.2 Gestures

- **B-7 Where a drag starts.** Anywhere on the surface: grabber, header, footer and the content's background. The
  surface carries `anchoredDraggable(state, Vertical, enabled = !leaving && placement != Center,
  overscrollEffect = band, flingBehavior = SheetFlingBehavior)`. A touch during a settle catches the sheet
  (`startDragImmediately = isAnimationRunning`, `AnchoredDraggable.kt:479`). An `Initial`-pass pointer observer on the
  surface stops a running settle on any finger down, so a touch on the content (a nested-scroll path) also catches it
  (fixes D8).
- **B-8 Past the top stop.** Rubber band `y = c·x·d / (d + c·x)` with `c = springs.rubberBand` (0.55,
  `ds/theme/AppMotion.kt:186`) and **`d = sheet.gesture.overPullRange · H` = 0.008 H**: the top gap stretches by at
  most about 7 dp, as iOS's 0.08 H → 0.072 H [M] (`src/flutter/sheet.dart:30`, `:37`). **Overrides** WP03's
  `d = H` (`AppSheetState.kt:161`), which lets the sheet travel about half the pull. The surface is drawn to the
  container's bottom plus that range, so no gap opens below it.
- **B-9 Below the lowest stop while dismissal is blocked.** The same band with `d = sheet.gesture.blockedPullRange · H`
  = 0.25 H (**taste**: a 100 dp pull moves it 44 dp, 300 dp moves it 94 dp [D]).
- **B-10 Release.** With the stops `S` (active detents, plus `hidden` when dismissal is allowed), `T` =
  `sheet.gesture.projectionSeconds` (0.099 s: iOS's 0.99/ms deceleration, MOTION-PLAN §1 rule 3) and
  `V` = `sheet.gesture.flickVelocity · H` (2 H/s [M], `src/flutter/sheet.dart:78`):
  1. Above the lowest detent: `p = y + v·T`; target = the stop nearest `p`. If `|v| ≥ V` and that stop is not past
     `y` in the direction of `v`, the target is the first stop past `y` in that direction.
  2. Below the lowest detent (between it and `hidden`): dismiss if `v ≥ V`, or if
     `(y + v·T − lowest) ≥ sheet.gesture.dismissFraction · (hidden − lowest)` with `dismissFraction` = 0.52 [M]
     (`src/flutter/sheet.dart:1110-1119`); return to the lowest detent if `v ≤ −V` or otherwise.
  3. If the target is `hidden` and dismissal is blocked: the sheet returns to the lowest detent and the guard's
     `onAttempt(Drag)` runs once for the gesture.
  - Worked numbers [D]: from Medium (0.5 H) a 2 H/s flick dismisses (rule 2), as iOS does; a slow release dismisses
    past 0.76 H. From Large (≈ 0.05 H) a 2 H/s flick stops at Medium (rule 1); skipping Medium to dismiss needs
    `p ≥ 0.75 H`, about 7 H/s. Whether iOS skips Medium on a hard flick is [U] (`research.md` §1.9).
  - This replaces WP03's projection-only rule, which needed about 4.6 H/s to dismiss from Large
    (`research.md` §1.9), and foundation's default 125 dp/s threshold (`AnchoredDraggable.kt:1551-1566`), which
    M3 found reads small scroll flings as dismissals (`5ab9549e`, `research.md` §2.4).
- **B-11 Settle.** On `sheet`, seeded with the release velocity. The `Threshold` haptic plays when a user settle lands
  on a detent other than the one the drag started from (WP03's rule, `AppSheetState.kt:194-195`); never on show,
  hide or programmatic moves.
- **B-12 Grabber activation** (tap, Enter, Space, TalkBack double-tap): the next taller stop; from the tallest, the
  lowest (iOS tap-to-cycle [A]; Material "toggle through preset heights" [G]). With one stop, a dismissible sheet
  closes, otherwise nothing.
- **B-13 Pointer.** A mouse or trackpad drags the sheet like a finger; the wheel scrolls the content and never
  resizes the sheet. The grabber shows a hover state (§5.3).

### 3.3 Nested scroll

- **B-14 `Resizes`** (default): `onPreScroll` gives an upward user delta to the sheet until it reaches its top stop
  (`dispatchRawDelta`, a state write that recomposes nothing); `onPostScroll` gives a downward leftover from user
  input at the content's top to the sheet (down to `hidden`, or into the B-9 band); `onPreFling` settles a sheet that
  is not at rest with the full velocity and consumes it; `onPostFling` settles with what is left. This is iOS's
  `prefersScrollingExpandsWhenScrolledToEdge = true` and M3's connection (`src/SheetDefaults.kt:630-689`).
- **B-15 `Scrolls`:** the content scrolls at every detent; `onPreScroll` gives the sheet nothing, and only the
  downward leftover at the content's top drags it. The grabber, header and footer still resize it ("People can
  always resize your presentation using the drag indicator" [A]).
- **B-16 Header hairline.** `contentScrolled` comes from the same connection: it rises with upward scroll the content
  consumes and returns to 0 when the content hands back leftover at its top. A programmatic jump (`scrollToItem`)
  does not pass through nested scroll and can leave it stale until the next touch scroll [D]; acceptable for a
  hairline.
- **B-17** Horizontal scrollers inside are untouched (the sheet drags vertically only).

### 3.4 Layout: measure once, place every frame

One `Layout` (WP03's structure, `ds/component/overlay/AppSheet.kt:179-252`, kept and extended) with three measured
children: the sheet column (grabber band, header, content box), the footer and the scrim.

1. **Placement policy** first (§6): `Bottom`, `Center` or `Leading`/`Trailing`, from `properties.placement`, the size
   class and the posture. Modal sheets read the **window** class (they draw over the whole window); in-place sheets
   read the **container** class (they live in a pane).
2. **Width.** Bottom: `min(containerWidth, style.maxWidth)`, centred. Center: `min(layout.formSheetWidth,
   containerWidth − 2·gutter)`. Leading/Trailing: `layout.supportingPaneWidth`. The floating inset is **not** a width
   change: content is measured at the full width and the inset is drawn (§5.2), so the morph never re-measures.
3. **Column.** Measured once with `maxHeight` = Large's height (R-1), `minHeight = 0`. The content box is
   `weight(1f, fill = false)`, so a `LazyColumn` fills it and a short form wraps (material3 measures the same way,
   `src/BottomSheet.kt:299-307`). Its height is FitContent's (R-3).
4. **Stops** from the detents (§2.4), written with `updateAnchors` in the measure pass only when they changed.
5. **Content padding**, applied by the content box, from **settled** values only (never per frame):
   bottom = footer height + max(navigation bar, keyboard overlap) + (with `Scrolls`, the part of the sheet below the
   window at the settled detent). Written when the footer is measured at a new height, when the keyboard animation
   starts or ends, and when a detent settles: a re-measure of the content at those moments, never during a drag. The
   content box consumes the status bar and cutout insets at the top and pads `safeDrawing` horizontally when the
   sheet spans the window.
6. **Placement block** (every frame, L1): `y = offset − keyboardLift` (§3.8); the column at `(x, y)`; the footer at
   `max(min(y + columnHeight, visibleBottom) − footerHeight, y + headerBottom)`, where `visibleBottom` = container
   bottom − max(navigation bar, keyboard). The offset is read here and nowhere in composition, inside
   `withMotionFrameOfReferencePlacement` (`src/DraggableAnchors.kt:161-193`).
7. **Drawing** (D1/D2): the scrim rect; the surface node (material, shape, inset, shadow, rim); the predictive-back
   and covered-sheet transforms in the surface's layer.

Fit-content and "measure once" coexist because one measure of the column gives both the content's layout and its
height; taller detents only extend the drawn surface below the content.

### 3.5 Dismissal and the confirm hook

- **B-18** Sources: drag (B-10), scrim tap (when `scrimTapDismisses`), Back's commit, Escape, `requestDismiss` (the
  close button). Each follows B-3.
- **B-19 Blocked** (`dismissible = false` or an enabled `SheetDismissGuard`): drag rubber-bands (B-9) and fires the
  attempt on release; a scrim tap, Escape and `requestDismiss` fire the attempt at once; Back shows a resisted shrink
  (`predictiveBack.blockedFactor` = 0.25 of the progress, **taste**) and fires the attempt on commit, then springs
  back. Each attempt fires once per gesture. An attempt with no enabled guard does nothing else.
- **B-20 Persistent sheets** (`dismissible = false` and not `Modal`): Back above the lowest detent steps down to the
  lowest detent; at the lowest, the sheet's Back handler is disabled and Back reaches the screen. This is decided in
  the handler's `isBackEnabled`, so the system still plays back-to-home from a tab root.
- **B-21** The guard is meant to be enabled only while the content is dirty (VitFleet `LoadFormScreen.kt:128`): an
  untouched form keeps the plain, predictive dismissal.

### 3.6 Back, predictive back and Escape

- **B-22 Order.** The keyboard closes first (the IME takes Back before the app [U]: test it, `research.md` §2.10).
  Then the topmost sheet's single handler, registered with `OverlayBackHandler` at `PRIORITY_OVERLAY` on the
  dispatcher of the composition that calls `AppSheet` (`ds/component/overlay/OverlayBackHandler.kt:27-50`), so a sheet
  in a hidden kept tab never takes Back. Inside it, one branch per case (the navigation-event skill's rule):
  1. a content `SheetBackHandler` is enabled → it handles Back, no shrink;
  2. dismissal is blocked → B-19;
  3. a persistent sheet above its lowest detent → B-20;
  4. otherwise → dismiss.
- **B-23 Predictive progress.** `p = ProgressEasing(event.progress)`, `CubicBezierEasing(0.1f, 0.1f, 0f, 1f)` (SPEC §2;
  `src/BackHandler.kt:47-51`). The surface's layer: `transformOrigin = (0.5, 1)` at the container's bottom edge,
  `scaleX = 1 − 48 dp · p / width`, `scaleY = 1 − 24 dp · p / height`. The content's layer counter-scales
  `scaleY' = scaleX / scaleY` about `(0.5, 0)`, so text keeps its proportions (material3,
  `src/BottomSheet.kt:475-480`; fixes D6). The progress is a float state read only in those two layer blocks: zero
  recompositions per event (material3 reads it in composition, `src/BottomSheet.kt:168`; not copied).
- **B-24 Commit** keeps the shrunk pose and slides the sheet to `hidden` on `sheet` (MDC translates by the scaled
  height, `src/mdc/MaterialBottomContainerBackHelper.java:119-138`). **Cancel** springs `p` back to 0 on
  `springs.sheet.unit`; the system has already animated the raw progress most of the way (SPEC §2 "Cancel motion").
  The Back **key** has no shrink and goes straight to commit.
- **B-25 Rapid Back** (§0 decision 2):
  - Destination sheet: the commit calls `onDismissRequest` first (B-3), so the pop happens while the gate is idle;
    `onRemove()` then runs the slide, which `PresentedSheetFrame` reports as motion; presses meanwhile are counted
    and applied at 99 % of the travel (`nav4/back/BackGate.kt:51-57`, `:96-101`, `SETTLED_ENOUGH` `:108`). They pop
    further presented entries and are dropped at the presentation root, so they never reach the tab beneath.
  - In-screen sheet: while leaving, the handler stays registered and consumes Back (WP03's tested behaviour,
    `SheetTest.kt:65-79`).
- **B-26 Escape** has an explicit handler on the sheet root (`onPreviewKeyEvent`, `Key.Escape` key-down), the same
  path as the Back key (ADAPTIVE-PLAN:565; fixes D16).

### 3.7 Background interaction

- **B-27 `NonModal`:** no scrim and no pointer node over the screen; touches around the sheet reach the screen.
  Screen readers reach both; the sheet is a traversal group with its pane title (§7).
- **B-28 `ModalAbove(d)`:** at or below `d`, as `NonModal`. Above, the scrim's alpha ramps from 0 at `d` to full at
  the next stop up, drawn per frame; touch blocking and screen-reader isolation switch on a derived
  `blocking = settled or target detent above d`, one recomposition per crossing (the scrim's pointer node is
  present only while blocking, because a pointer node that does not consume still stops hit-testing of the
  siblings beneath).
- **B-29** Only a `Modal` sheet registers as modal with `SheetStage` (card stack, semantics clearing under the
  presentation host). A `ModalAbove` sheet dims only its own screen.

### 3.8 Keyboard

- **B-30 `Lift`** (default): `lift = clamp(imeCurrent − navigationBar, 0, offsetAtRest − largeTop)`, read from
  `WindowInsetsRulers.Ime` in the placement block (`ui112/…/layout/WindowInsetsRulers.kt:97`), so the sheet rises
  with the system's own curve frame by frame: L1, no recomposition, no re-measure. When the lift is clamped at
  Large's top, the rest of the keyboard overlaps the content; that overlap goes into the content's bottom padding
  from the animation's **target** (`getAnimation(scope).target`, `:69`, `:240` [V]) once at the animation's start
  and once at its end.
- **B-31 `Expand`:** when the keyboard's target becomes visible, the sheet animates to its tallest stop on `sheet`, and
  back to the detent it left when the keyboard hides, unless the person moved it meanwhile.
- **B-32 API 26–29** has no inset animation, so the inset jumps: the lift then animates on `sheet` toward the new
  value (MOTION-PLAN M22).
- **B-33** A drag that starts on the sheet's grabber, header or footer hides the keyboard; a content scroll does not.
- **B-34** The keyboard is counted once: the footer's slot consumes it (§2.7), the content's padding uses only the
  overlap left after the lift (fixes D2, D3, D4). Focusing a field in a Medium sheet therefore never leaves it under
  the keyboard.

### 3.9 Stacking

- **B-35** A sheet shown while another is up stacks on it: an in-screen sheet opened from a sheet's content, or a
  destination sheet over a destination sheet. HIG advises one sheet at a time [A]; the stack still has to look right.
- **B-36** The new sheet's Large top is the covered sheet's minus `stack.stackedLift · H` (0.02 H [M],
  `src/flutter/sheet.dart:48-55`). It draws its own scrim over the covered sheet.
- **B-37** The covered sheet scales to `stack.coveredScale` (0.9165) about its top centre and moves up
  `stack.coveredLift · H` (0.005 H) [M] (`sheet.dart:57-62`, `:84-90`), by the covering sheet's `presentedFraction`,
  in its layer (L0). Its grabber hides (`isCovered`). Only the bottom-most modal sheet makes the shell recede; the
  shell does not recede further.
- **B-38** Back, the scrim and drags act on the top sheet only. When it leaves, the covered sheet un-recedes by the
  same fraction, so the two motions stay locked together.

### 3.10 Tabs, restoration, configuration

- **B-39 In-place sheets** live in their screen: they park, push and pop with it, and their Back handler is on the
  tab's dispatcher, which a hidden kept tab disables (`nav4/shell/tabs/KeptTabs.kt:34-47`).
- **B-40 Overlay-host sheets follow their tab** (fixes N7): `HostedOverlay` reads `LocalTabVisible` (WP04's
  `TabVisibility` holder) and, while the owning tab is hidden, parks the entry: not placed, not drawn, semantics
  cleared, pointer ignored, composition kept. It returns at its detent without animating. The same applies to
  `AppAlert`, `AppActionSheet` and every other hosted overlay (VitFleet's `LocalOverlaysHidden` idea,
  `current-state.md` §11).
- **B-41 Destination sheets** live in the presentation host, outside the tabs; a deep link that changes tab leaves them
  up (`nav4/ShellState.kt:195-210` behaviour unchanged).
- **B-42 Process death.** `rememberAppSheetState` and `SheetSessions` save visibility and the detent index; a restored
  sheet shows at its detent with no slide (fixes D15). Content state is the content's own `rememberSaveable`.
- **B-43** A configuration change keeps the detent by identity (B-5). Font scale changes re-measure the column once.

### 3.11 Reduce motion

- **B-44** Show and hide become a 150 ms fade of the surface with `CompositingStrategy.ModulateAlpha` plus the scrim's
  rect fading with it (MOTION-PLAN §1 rule 5). `SheetTransition.CrossFade` is the same fade for everyone.
- **B-45** Detent changes still move, on `sheet` (ζ 1, no bounce). The card stack keeps only its overlay (no scale,
  no translation). Predictive back has no shrink; the commit fades.

### 3.12 Frame rate

- **B-46** The surface carries `Modifier.preferredFrameRate(FrameRateCategory.High)` (`ui112/…/FrameRate.kt:71` [V];
  SMOOTHNESS S10). It votes only on API 35+ devices with adaptive refresh, and only while the node draws [U]; QA2
  checks that a resting sheet costs no frames (`idle` gate).

---

## 4. Motion

Springs are written as response and bounce, as `AppSpring` defines them (`ds/theme/AppMotion.kt:61-77`:
`stiffness = (2π / response)²`, `dampingRatio = 1 − bounce`). Values are for `AppMotionStyle.Standard`; springs marked
÷speed divide their response by the style's speed (Calm 0.87, Snappy 1.15, `tokens/motion.json:3-6`).

### 4.1 Springs used by sheets

| Motion | Token | Response / bounce | Compose `spring(ζ, k)` | Source |
|---|---|---|---|---|
| Show, hide, detent settle, programmatic move, keyboard fallback, covered-sheet recede, card stack (it follows the sheet, it has no spring of its own) | `springs.sheet` ÷speed | 0.34 s / 0 | 1, 341.5 | CP1 keep (`decisions/CP1.md:137`); iOS page curve within 0.004 s [D] |
| Form sheet scale-in, action sheet arrival | `springs.dialog` ÷speed | 0.275 s / 0 | 1, 522 | CP1 (`:138`); iOS k 522.35 [M] |
| Predictive-back cancel | `springs.sheet.unit` | 0.34 s / 0 | 1, 341.5 | SPEC §5.4 |
| Push and pop inside `AppSheetNavHost` | `springs.screen` ÷speed | 0.344 s / 0 | 1, 333 | CP1 (`:32`) |
| Glass lens (none on sheets in v1) | — | — | — | M26 |

No sheet motion bounces: a full-height surface with overshoot reads as unstable (CP1 "no bounce on anything that
moves a full screen"). Every one of them is seeded with the current velocity and interruptible (MOTION-PLAN §1 rules
2–4). There is no tween after a finger; the only fixed-duration animations are the reduce-motion fades.

### 4.2 The `sheet` token section (new, `tokens/motion.json`)

```json
"sheet": {
  "detent":   { "mediumFraction": 0.5, "largeTopGap": 10 },
  "gesture":  { "projectionSeconds": 0.099, "flickVelocity": 2.0, "dismissFraction": 0.52,
                "overPullRange": 0.008, "blockedPullRange": 0.25 },
  "grabber":  { "width": 36, "height": 5, "top": 5, "alpha": 0.45, "hoverAlpha": 0.7, "touchTarget": 48 },
  "header":   { "top": 14, "minHeight": 44, "scrollEdge": 8 },
  "floating": { "minBottomRadius": 16 },
  "form":     { "maxHeightFraction": 0.85, "maxHeight": 620, "enterScale": 0.94, "enterRise": 24 },
  "side":     { "inset": 16 },
  "stack":    { "coveredScale": 0.9165, "coveredLift": 0.005, "stackedLift": 0.02 },
  "cardStack": {
    "scale": 0.9165, "cornerRadius": 12, "deviceCornerMin": 20,
    "overlayAlpha": 0.10, "overlayColor": { "light": "#000000", "dark": "#C8C8C8" },
    "statusBarFlip": 0.5
  },
  "predictiveBack": { "shrinkX": 48, "shrinkY": 24, "easing": [0.1, 0.1, 0.0, 1.0], "blockedFactor": 0.25 },
  "glass":    { "tintAlpha": { "light": 0.55, "dark": 0.62 }, "attachHysteresis": 0.001 },
  "shadow":   { "radius": 24, "alphaNonModal": 0.10, "alphaFloating": 0.06 },
  "fade":     { "reduceMotionMs": 150 }
}
```

`tokens/styles.json` gains, per design style: `"sheetInset"` (Utility 8, Social 8, Editorial 0, Playful 10; **taste**)
and `"sheetMaterial"` (`"Solid"` for all four; the Glass design style, which needs `liquidglass`, sets `"Glass"`).
`radii.sheet` already exists (24 / 30 / 8 / 28, `tokens/styles.json:5`, `:22`, `:39`, `:56`).

| Token | Value | Meaning | Source |
|---|---|---|---|
| `detent.mediumFraction` | 0.5 | Medium's height over the container's | [A] "approximately half"; WP03 had 0.55 (`AppSheet.kt:335`); **taste** |
| `detent.largeTopGap` | 10 dp | below the status bar | WP03 (`AppSheet.kt:343`); iOS 0.08 H sits ≈ 8 pt under a 62 pt safe top [M][D] |
| `gesture.projectionSeconds` | 0.099 s | `v · r / (1 − r) / 1000` at r = 0.99/ms | WWDC18 803 [A]; WP03 `AppSheetState.kt:216-219` |
| `gesture.flickVelocity` | 2.0 H/s | a flick moves at least one stop; dismisses from the lowest | [M] `sheet.dart:78` |
| `gesture.dismissFraction` | 0.52 | of the lowest-detent-to-hidden travel | [M] `sheet.dart:1119` |
| `gesture.overPullRange` | 0.008 H | rubber band asymptote past the top stop | [M] `sheet.dart:30`, `:37` (0.08 → 0.072) |
| `gesture.blockedPullRange` | 0.25 H | asymptote below the lowest stop when blocked | **taste** [D] |
| `grabber.*` | 36 × 5 dp, 5 dp from the top, α 0.45 of `contentTertiary`, hover α 0.7, 48 dp target | | [M] `sheet.dart:704-724`; WP03 α (`AppSheet.kt:302`); 48 dp: M3 accessibility [G] |
| `header.top`, `minHeight`, `scrollEdge` | 14 dp, 44 dp, 8 dp | | WP03 (`AppSheet.kt:306`); iOS bar height |
| `floating.minBottomRadius` | 16 dp | bottom corners when the display radius is unknown or small | [D] |
| `form.*` | ≤ min(0.85 H, 620 dp); enters from 0.94 and 24 dp lower | | ADAPTIVE-PLAN:434; iPad 540 × 620 [A]; PeriFlow `AppDetentSheet.kt:333-336` |
| `side.inset` | 16 dp | gap of a leading or trailing sheet | M3 detached side sheet 16 dp [G] |
| `stack.*` | 0.9165, −0.005 H, −0.02 H | | [M] `sheet.dart:48-62`, `:88` |
| `cardStack.scale` | 0.9165 about the top centre | | [M] `sheet.dart:88` |
| `cardStack.cornerRadius` | 12 dp at full recede, from the display's radius (used when ≥ `deviceCornerMin` 20 dp, else 0) | | [M] `sheet.dart:18-25`, `:327-336` |
| `cardStack.overlay*` | α 0.10, black in light, `#C8C8C8` in dark | | [M] `sheet.dart:73`, `:344` |
| `cardStack.statusBarFlip` | 0.5 | light status-bar icons past half recede | [M] `sheet.dart:366`; the 0.5 threshold is [D] |
| `predictiveBack.*` | 48 / 24 dp, `(0.1, 0.1, 0, 1)` | bottom container | [G] MDC `motion_dimens.xml:23-24`; SPEC §2 |
| `predictiveBack.blockedFactor` | 0.25 | resisted shrink when blocked | **taste** |
| `glass.tintAlpha` | 0.55 light / 0.62 dark at partial detents | lerps to 1 at Large | **[U]: CP2 decides from recordings** |
| `shadow.*` | radius 24 dp; α 0.10 non-modal, 0.06 floating modal; light theme only | | **taste**; WP05's chrome shadow is radius 16, α 0.10 (`glass/GlassDefaults.kt:84`) |

### 4.3 Show, hide and detents

| Moment | Motion | Cost |
|---|---|---|
| Show | `hidden → target` on `sheet`, clock after the first frame (B-1). The scrim follows `presentedFraction`; the card stack follows `largeFraction` | L1 + D2 |
| User settle | to the B-10 target on `sheet` with the release velocity | L1 + D2 |
| Hide | to `hidden` on `sheet` with the release velocity (0 for a tap, a key or a programmatic hide) | L1 + D2 |
| Form sheet in | scale 0.94 → 1 and 24 dp → 0 on `dialog` in the surface's layer, with the scrim on the same fraction; out: the reverse, also on `dialog` | L0 + D2 |
| Over-pull release | back to the top stop on `sheet` with the velocity (iOS uses a 180 ms easeOut [M]; a spring keeps the velocity) | L1 |

### 4.4 Card stack (the shell behind a modal sheet)

With `f = stage.recede`, `s = 1 − (1 − 0.9165) · f`:

| Property | Value | Phase |
|---|---|---|
| `scaleX`, `scaleY` | `s`, `transformOrigin = (0.5, 0)` | layer |
| `translationY` | `f · statusBarTop` (the presenter's top meets the status bar's bottom, §0 decision 14) | layer |
| corners | `lerp(R0, 12 dp, f)`, uniform, `R0` = the display's top corner radius (`WindowInsets.getRoundedCorner`, API 31+) when ≥ 20 dp, else 0; shapes cached per whole pixel (SPEC §3) | layer outline (round rect: hardware clip) |
| overlay | `drawRect(overlayColor, alpha = 0.10 · f)` after the content | draw, inside the layer |
| window | a black rect behind the layer while `f > 0` | draw |
| status bar | light icons while `f > 0.5`, from a derived flag | one recomposition per crossing |
| scrim | the sheet's scrim alpha is multiplied by `1 − f` when the card stack is on, so the presenter is not darkened twice | draw |

Under reduce motion: overlay only. The shell's content recomposes zero times (RECOMPOSITION-PLAN "Card stack").

### 4.5 Stacked sheets

The covered sheet: `scale = 1 − (1 − 0.9165) · g`, `transformOrigin = (0.5, 0)`, `translationY = −0.005 H · g`, with
`g` the covering sheet's `presentedFraction`; in the covered surface's layer. The covering sheet's Large top is
0.02 H higher (R-1).

### 4.6 Floating inset and glass transitions

With `a = state.largeFraction` (0 at the highest partial detent, 1 at Large):

| Property | At `a = 0` | At `a = 1` | Phase |
|---|---|---|---|
| side and bottom gap | `style.floatingInset` | 0 | draw (the surface's rect and clip) |
| bottom corners | `max(R − inset, 16 dp)` with `R` the display's bottom corner radius (API 31+), else `cornerRadius` | 0 | draw |
| top corners | `cornerRadius` | `cornerRadius` | — |
| Frosted body alpha | 0.92 light / 0.94 dark (`glass/GlassDefaults.kt:60-67`) | 1 | draw |
| Glass tint alpha | `glass.tintAlpha` | 1 | draw (Kyant `onDrawSurface`) |
| Glass backdrop | sampling | not drawn once settled at Large (`a ≥ 1 − attachHysteresis` and not moving); back on at the next drag | one recomposition per switch |
| shadow alpha (floating modal) | `alphaFloating` | 0 | draw (shadow block) |

A sheet whose only stops are Large never floats. Glass appears by sliding in with the sheet, never by an alpha ramp
on the backdrop (M26 rule).

---

## 5. Visuals

### 5.1 Surface and shape

- **Attached** (at Large, or with `floatingInset = 0`): full width up to `maxWidth`, top corners `cornerRadius`
  (24 / 30 / 8 / 28 dp by design style), square at the window's edges.
- **Floating** (partial detents, `floatingInset > 0`): inset by `floatingInset` at the sides and the bottom; bottom
  corners concentric with the display's (`radius = max(R − inset, 16 dp)`, Apple's concentric rule
  `ConcentricRectangle` [A]; `R` from `WindowInsets.getRoundedCorner`, API 31+, as MDC does,
  `src/mdc/BottomSheetBehavior.java:1639-1673`). The gap and the bottom corners shrink to 0 as `largeFraction` goes to 1
  (§4.6). Wider than `maxWidth` (a tablet in portrait), the sheet is already clear of the sides; only the bottom gap
  applies.
- **Form sheet:** all four corners `shapes.xl` (28 dp), centred.
- **Leading/Trailing:** all four corners `cornerRadius`, `side.inset` (16 dp) from the edge, the bottom and the status
  bar.
- **Clip.** Content is clipped to the surface's shape (WP03 clips today, `ds/component/overlay/AppSheet.kt:192`). The
  outline is cached per whole-pixel radius. With equal radii it is a round rect, which the hardware clips without an
  offscreen layer; with different top and bottom radii it is a path, which before API 33 cannot be an outline clip and
  falls back to a canvas clip path [U: check for `saveLayer` and anti-aliasing on API 26 and 29 emulators,
  `research.md` §6]. If that costs a layer, the floating shape uses one uniform radius (the larger of the two) below
  API 33.

### 5.2 Materials, light and dark

| Material | Light | Dark | Cost |
|---|---|---|---|
| Solid | `colors.container` = `surface` | `surface` | one rect |
| Frosted | `surface` at 0.92 + rim (white 0.85, 0.75 dp) + sheen | `surface` at 0.94 + rim (white 0.20) | cached brushes (`glass/GlassSurface.kt:26-44`); the sheet draws its own copy, so Frosted exists without the glass feature |
| Glass, live | scene blurred (`GlassDefaults.ChromeBlur`, 10 dp) and saturated, then the scrim's `dim`, then the tint at `glass.tintAlpha` | same, dark tint | one backdrop pass per frame while anything behind or the sheet moves; budgeted (§13.5) |
| Glass, below Live | Frosted | Frosted | as Frosted |
| any, Reduce transparency (`GlassQuality.Solid`) | Solid | Solid | — |

Grouped content (`AppSection`s) uses `AppSheetDefaults.groupedColors()`: `backgroundGrouped` behind
`surfaceGrouped` rows, as iOS's grouped sheets.

### 5.3 Grabber

36 × 5 dp, fully rounded, centred, 5 dp from the top edge, `contentTertiary` at α 0.45; α 0.7 while a mouse hovers
it; a 48 × 48 dp touch target centred on it; the focus ring (`colors.focusRing`, 2 dp) around a 48 × 24 dp rounded
rect when focused by keyboard. It is drawn over the header's top band and takes no layout room. Hidden as §2.7 says.
Test tag `sheet_grabber` (A3.8 contract, `design/MASTER-PLAN.md:1032`).

### 5.4 Header and footer

- Header: starts 14 dp below the top edge, at least 44 dp tall, gutter padding at the start and `spacing.xs` at the
  end (WP03's, `AppSheet.kt:306`); title in `typography.headline`, `contentPrimary`, one line, ellipsized; subtitle
  in `footnote`, `contentSecondary`. A hairline in `colors.separator` along its bottom, alpha = `contentScrolled`.
- `AppSheetCloseButton`: a 30 dp circle in `fill`, a 13 dp cross in `contentSecondary`, inside a 48 dp target
  (PeriFlow's animated cross is not copied: decorative motion is out of scope, `current-state.md` §11).
- Footer: whatever the slot draws; an `AppBottomActionBar` keeps its top hairline and takes the material's body
  through `LocalSheetSurface` (Solid: `container`; Frosted and Glass: the frosted body, so text stays legible).

### 5.5 Scrim

One rect over the whole window, status bar included (QA-13), in `colors.scrim` (light `0x660B0F1A`, dark
`0x8F000000`, `ds/theme/tokens/DesignTokens.kt:249`, `:292` [C]), at `alpha × dim`, drawn in a `drawBehind` (D2). No
layer alpha on any full-screen node (SMOOTHNESS S8). Its alpha against iOS's dimming is [U]: QA2 compares a medium
sheet side by side with an iOS recording; the token can change, the mechanism does not.

### 5.6 Shadow and elevation

- No `Modifier.shadow(elevation)` and no RenderNode elevation on a sheet: a fading or translucent body over an
  elevation shadow is exactly pull to refresh's black ring (REQUIREMENTS 29).
- The shadow is `dropShadow` drawn by the surface node with the inside of the shape cut away
  (`ClipOp.Difference` on the cached outline), as `glassSurface` does (`glass/GlassSurface.kt:33-35`). A translucent
  material, a fade (reduce motion uses `ModulateAlpha`, which applies alpha per draw) or a glass ramp can then never
  show it through the body.
- Its alpha is set in the `DropShadowScope` block (`ui112/…/draw/Shadow.kt:154` [V]), so it follows `largeFraction`
  or the fade without recomposing.
- Who gets one: a non-modal sheet (light: α 0.10; nothing separates it from the screen otherwise), a floating modal
  sheet at partial detents (light: α 0.06 × (1 − `largeFraction`)). An attached modal sheet has none; the scrim and
  the card stack separate it, as on iOS.
- Dark theme: no shadow; a 0.5 dp rim in `colors.separator` along the top edge (and around a floating sheet).

### 5.7 RTL and large text

Leading/Trailing placement and the header's slots follow the layout direction; the grabber and the predictive-back
origin are centred. At font scale 1.5 and 2.0 the header grows, FitContent re-measures once, and nothing is clipped
(REQUIREMENTS 34): screenshot tests at 1.5 (§13.3).

---

## 6. Adaptive behaviour

`SheetPlacement.Automatic` resolves from the size class (modal sheets: the window's; in-place sheets: the
container's) and the posture. Width classes: Compact < 600, Medium 600–839, Expanded 840–1199, Large 1200–1599,
ExtraLarge ≥ 1600 dp; heights: Compact < 480, Medium 480–899, Expanded ≥ 900 dp (`ds/foundation/adaptive/SizeClass.kt:7-50`).

| Window | Modal sheet | Non-modal or `ModalAbove` sheet |
|---|---|---|
| Compact width (phones) | Bottom, full width, detents, floating at partial detents | Bottom, in place, above the bar |
| Medium width, height ≥ Medium (small tablets, unfolded inner screens) | **Center** form sheet: `min(formSheetWidth 540, W − 2·gutter)` wide, fitted height ≤ min(0.85 H, 620 dp), all corners rounded, close button in the header, no detents, no drag-to-dismiss, scale-in on `dialog` (fixes D11) | Bottom, `min(sheetMaxWidth 640, W − 2·gutter)`, centred |
| Expanded width and up, height ≥ Medium | Center form sheet (600 dp wide from Large) | **Leading** sheet: `supportingPaneWidth` (360 / 412 dp), 16 dp from the leading, bottom and top edges, detents kept as heights; the content beside it stays usable (iOS 27 `presentationPlacement(.leading)`, Maps [A]) |
| Compact height, any width (phones sideways, short splits) | Bottom, **Large only** (Medium is off, R-2), at most 640 dp wide, centred (ADAPTIVE §3.5) | Bottom, the detents that stay on, at most 640 dp |
| Tabletop posture | Bottom (never a card across the crease); Medium rests on the fold (R-2) | same |
| Book posture (vertical hinge) | Center form sheet moves into the larger half, centred there, never across the hinge (Material `AvoidSeparating`) | Leading sheet in the leading half |
| Freeform window, ChromeOS, desktop | re-resolves on every size-class change; within one class, a resize moves the anchors (R-10) without recomposing the content (RECOMPOSITION "Window resize") | same |

- A form sheet's predictive back scales it about its centre to 0.90 (MDC's main container minimum, SPEC §2) with the
  same easing; the commit fades and scales it out on `dialog`.
- `SheetPlacement.Bottom`/`Center`/`Leading`/`Trailing` force a placement. A forced `Center` on a phone is a centred
  card with a fitted height.
- Input: Escape closes (B-26), Tab moves focus inside a modal sheet only (§7), the grabber has hover and focus states,
  a mouse drag moves the sheet (B-13).
- The overlay host's sheet measures against the window; a sheet opened from the detail pane of a list-detail layout
  still centres in the window, as an iPad form sheet does.

---

## 7. Accessibility

| Element | Semantics | Source |
|---|---|---|
| Sheet root | `paneTitle` always: the header's title, or "Sheet" (`designsystem_sheet_pane`); `isTraversalGroup = true`, `traversalIndex = 0f`; a `Modal` sheet also gets `dialog()` | material3 (`src/BottomSheet.kt:334-337`, `src/ModalBottomSheet.kt:141`); `ui112/…/semantics/SemanticsProperties.kt:972`, `:1022`, `:1149` [V]; fixes D12 |
| What is beneath a modal sheet | hidden from screen readers: the overlay host's `clearAndSetSemantics {}` (`AppOverlayHost.kt:58` [C]); the tab hosts under the presentation host get the same while `stage.hasModalSheet` or a cover is up (WP04; fixes N8) | MDC hides siblings (`src/mdc/BottomSheetBehavior.java:2489-2534`) |
| Grabber | `Role.Button`; one label, the action ("Expand", "Collapse", "Close"); `stateDescription` "Expanded", "Half expanded" or "Collapsed" from the settled detent; custom actions `expand`, `collapse` and `dismiss` when each applies; focusable; Enter and Space act as a tap | M3 a11y [G]; `src/BottomSheet.kt:365-397`; MDC `BottomSheetDragHandleView.java:176-206`; `SemanticsProperties.kt:930`, `:1632-1660` [V] |
| Scrim | when it dismisses: `onClick(label = "Close sheet")`, `contentDescription` "Close sheet", `traversalIndex = 1f`, so it is the last stop; nothing when it does not | `src/Scrim.kt:58-71`, `src/ModalBottomSheet.kt:152-157` |
| Close button | `contentDescription` "Close" (`designsystem_close`), 48 dp | WP03 string |
| Detent changes | announced through the grabber's `stateDescription`, never `announceForAccessibility` (deprecated in API 36) | `research.md` §2.3 |
| Keyboard focus | a modal sheet traps Tab: `focusProperties { onExit = { cancelFocusChange() } }` on its root (`ui112/…/focus/FocusProperties.kt:188`); opened from the keyboard, focus moves to the sheet's first focusable; on dismiss it returns to the element that had it | ADAPTIVE-PLAN `Keyboard_Navigation` |
| Escape | closes, like Back (B-26) | ADAPTIVE-PLAN:565 |
| Dismiss attempt | the guard's confirmation (an `AppAlert`) is a modal overlay with its own pane title | — |
| Reduce motion | §3.11 | — |
| Font scale | header and FitContent grow; nothing clipped at 1.5 and 2.0 | REQUIREMENTS 34 |

All strings live in `:core:designsystem` with its `designsystem_` prefix (F-5). TalkBack scrolling inside a Medium
sheet does not grow it (an accessibility scroll is not `UserInput`); the grabber's Expand action does.

---

## 8. Performance contract

Frame budget at 120 Hz: 8.33 ms. Cost classes are MOTION-PLAN §2's.

| Moment | What changes per frame | Where it is read | Class | Recompositions |
|---|---|---|---|---|
| Drag, settle, show, hide | `offset` | the sheet layout's placement block | L1 | 0 per frame; content never re-measured |
| — | scrim alpha | `drawBehind` of one rect | D2 | 0 |
| — | floating gap, bottom corners, shadow alpha, Frosted alpha | the surface node's draw (cached outline per whole px) | D1 | 0 |
| — | card stack (shell) | `recedeBehindSheets`' layer and draw | L0 + D2 | shell content 0 |
| — | covered sheet's recede | its surface's layer | L0 | 0 |
| — | glass tint | Kyant `onDrawSurface` | backdrop pass | 0; 1 when the backdrop switches off or on at Large |
| Predictive back | `p` | the surface's and the content's layer blocks | L0 | 0 per event |
| Keyboard | `lift` | placement, from `WindowInsetsRulers.Ime` | L1 | 0 per frame; ≤ 1 at the animation's start and end (padding) |
| Content scroll | header hairline | the header's draw | D1 | 0 per scroll frame |
| Detent settles | `currentDetent`, grabber `stateDescription`, padding with `Scrolls` | composition | — | ≤ 1 per settle |
| `ModalAbove` crossing | `blocking` (pointer node, semantics) | composition | — | 1 per crossing |
| Show | the sheet's first composition | — | — | frame 1 only; frames 2…N−1: 0 |

Forbidden in sheet code (each is a review finding): reading `offset`, `presentedFraction`, `largeFraction`,
`contentScrolled` or back progress in composition (lint `FrequentlyChangingValue` is an error, M-5); `Modifier.offset(dp)`
or padding from the offset; `graphicsLayer { alpha }` on a window-sized node (code rule `layer-alpha-fill`, R9);
`BoxWithConstraints` or `SubcomposeLayout` in the sheet (material3 removed its own, `f7fe1d92`); allocating in a
draw or placement block (shapes and paths are cached or rewound); a `Dialog` window; a tween after a finger.

---

## 9. Integration

### 9.1 Layers

Bottom to top, in one window (`nav4/presentation/PresentationHost.kt:53-64`, `ds/component/overlay/AppOverlayHost.kt:20-33` [C]):

| Layer | What | Sheets in it |
|---|---|---|
| 1 | tab content (recorded by the `GlassStage` when glass samples) | in-place non-modal and `ModalAbove` sheets, inside their screen |
| 2 | the bar, the rail or the sidebar (`ShellChrome`'s chrome slot) | — |
| — | `recedeBehindSheets` wraps layers 1 and 2 | — |
| 3 | the presentation host: covers and **destination sheets** | `SheetScene` |
| 4 | the overlay host: alerts, HUDs, **in-screen modal sheets**, action and choice sheets | `HostedOverlay(modal = true)` |
| 5 | toasts | — |

An in-screen modal sheet opened while a destination sheet is up draws above it (layer 4 over 3) and registers after
it, so the stage's order and the drawing order agree. The reverse (a destination presented from an in-screen sheet)
would draw the new sheet under the old one: the navigator's `submit` hides in-screen sheets first in that case (WP04),
as HIG's "close the first sheet before displaying the new one" [A].

### 9.2 The presentation host (WP04)

- The host builds its own `SheetSceneStrategy` (with `SheetSessions` and `isPresented`) and adds the sessions'
  decorator to `rememberDecoratedNavEntries` (`PresentationHost.kt:92-95`). The `sheetStrategy` parameter of
  `AppShell` and `PresentationHost` goes (`AppShell.kt:96`, `PresentationHost.kt:70`).
- `PresentationCard` asks "is this key a cover?" from the typed metadata (`PresentationHost.kt:74-78` today asks
  through the string key).
- Sheet entries get `PresentedSheetFrame` (§2.9); covers keep `PresentedFrame`.
- `StackBackHandler` and `GateSettle` (`PresentationHost.kt:153-154`) are unchanged: the sheet's own handler runs
  ahead of them at overlay priority; Back reaches the host's handler only while a sheet is leaving (B-25), where the
  gate counts it.
- While `stage.hasModalSheet` is true, the tab hosts get `clearAndSetSemantics {}` (N8), derived, one recomposition
  per present and dismiss.

### 9.3 The overlay host (WP03)

- `HostedOverlay` parks its entry while the owning tab is hidden (B-40).
- The host already clears what is beneath while a modal overlay is up (`AppOverlayHost.kt:58`).
- `AppTheme` (outermost) provides `LocalSheetStage` next to `LocalOverlayHost`.

### 9.4 Chrome and insets

| Local | Inside a sheet | Why |
|---|---|---|
| `LocalBottomChrome` | a zero holder | the sheet pads its content itself (§3.4); a reused screen must not pad for the tab bar |
| `LocalTabBarVisible` | false | no bar under a sheet |
| `LocalTopChrome` | the sheet's header height | an `AppScaffold` inside draws its top bar below the header, not under the status bar (QA-19) |
| `LocalSheetSurface` | the sheet | §2.7 |
| status bar inset | consumed by the content box | the sheet's top is below it |

`BottomChromeWriter` gives the keyboard precedence over a presented sheet's footer (N9), so a toast over a sheet with
the keyboard up rests above the keys. WP03's and WP04's `Chrome.kt` are reconciled at their merge (N11): the
`foundation.layout` package and WP04's `TabVisibility` holder.

### 9.5 The card stack (WP04 applies, WP03 provides)

- `AppShell` applies `Modifier.recedeBehindSheets(stage, enabled = config.sheetCardStack)` to `ShellChrome`'s box,
  which holds the content and the bar (`nav4/shell/ShellChrome.kt:12-22`, `nav4/AppShell.kt:131`).
- `SheetStatusBarEffect(stage)` flips the status bar's icons to light past `statusBarFlip` and back.
- With liquid glass, the `GlassStage` sits inside the receded box, so the bar's sampling sees an ancestor scale.
  Whether the sampled region stays aligned is [U] (`current-state.md` §4.8); WP05 checks it on the phone. The sheet's
  own glass is opaque at Large, where the shell recedes, so it never samples a receded scene [D].

### 9.6 Glass (WP05)

- WP05 provides `LocalSheetBackdrop` around the presentation host and the overlay layer, outside the recorded
  subtree, only when the tier samples (`GlassQuality.samplesScene`, `glass/GlassQuality.kt:41-43`). Its
  `sheetBackdrop` is Kyant's `drawBackdrop` over the stage's `SceneBackdrop`, as `rememberGlassChrome` builds it
  (`glass/GlassChrome.kt:24-48`): effects built once and reading no state, the tint and the scrim's `dim` drawn in
  `onDrawSurface`.
- `LocalGlassScene` is internal to the chrome slot today (`glass/GlassStage.kt:51-58`); WP05 exposes the scene to
  the two hosts through this provider rather than widening that local.
- The GPU watcher (`glass/GlassQuality.kt:96-108`) watches frames while a live glass sheet draws, and demotes the tier
  if the GPU falls behind.
- Never a layer alpha around a sampling surface (`handoff/WP05.md:230`): the sheet's reduce-motion fade is applied
  to the content and the Frosted body, not to the backdrop (a Glass sheet under reduce motion draws Frosted).

### 9.7 Benchmarks and test tags

Contract tags (A3.8): `sheet_grabber` (exists), `sheet_surface` (the sheet's root), `sheet_scrim`, and the sample's
trigger `sheet_trigger` (WP02-M3). The overlay layer already exposes test tags as resource ids
(`AppOverlayHost.kt:63-66`); the presentation host does the same for its sheets.

---

## 10. Packages and files

Concept packages, no `internal`, `util` or `common` packages (F-2, F-3); one public component per file with its
defaults and private pieces (K-2); every file well under 400 lines.

```
core/designsystem/src/main/kotlin/<pkg>/core/designsystem/component/sheet/       (WP03)
├── AppSheet.kt                 both AppSheet overloads; the visible-driven hosting
├── AppSheetState.kt            AppSheetState, rememberAppSheetState, Saver
├── AppSheetStyle.kt            AppSheetStyle, AppSheetColors, SheetMaterial
├── AppSheetDefaults.kt         AppSheetDefaults
├── AppSheetProperties.kt       AppSheetProperties, SheetPlacement, SheetContentInteraction, SheetTransition, SheetKeyboard
├── AppSheetScope.kt            AppSheetScope, SheetDismissGuard, SheetBackHandler, ProvideSheetHeader/Footer, LocalSheetSurface
├── SheetDetent.kt              SheetDetent, SheetDetentContext
├── SheetModality.kt            SheetModality, SheetDismissSource
├── header/
│   ├── AppSheetHeader.kt
│   ├── AppSheetGrabber.kt      grabber drawing, tap-to-cycle, semantics
│   └── AppSheetCloseButton.kt
├── layout/
│   ├── SheetLayout.kt          the one Layout: measure once, place every frame, footer, padding
│   ├── SheetAnchors.kt         detents → stops (R-1…R-10)
│   ├── SheetPlacementPolicy.kt Automatic → Bottom / Center / Leading / Trailing (§6)
│   └── SheetKeyboardLift.kt    lift from the IME rulers; API 26–29 fallback
├── gesture/
│   ├── SheetFlingBehavior.kt   B-10
│   ├── SheetOverscroll.kt      B-8, B-9 rubber bands (an OverscrollEffect)
│   └── SheetNestedScroll.kt    B-14…B-16
├── draw/
│   ├── SheetSurface.kt         a Modifier.Node: material, shape, floating morph, outer-only shadow, rim
│   ├── SheetScrim.kt           rect, tap, semantics, ModalAbove ramp
│   └── SheetBackdrop.kt        the glass seam
├── back/
│   └── SheetBack.kt            the single handler, eased progress, bottom container, Escape
├── stack/
│   ├── SheetStage.kt
│   └── RecedeBehindSheets.kt   the card-stack node
├── action/
│   ├── AppActionSheet.kt
│   └── SheetAction.kt
└── choice/
    └── AppChoiceSheet.kt

core/designsystem/src/main/kotlin/<pkg>/core/designsystem/component/overlay/    (WP03, kept)
├── AppOverlayHost.kt           HostedOverlay parks with its tab (B-40)
└── OverlayBackHandler.kt

core/navigation/src/main/kotlin/<pkg>/core/navigation/                           (WP04)
├── Presentation.kt             Presentation, SheetSpec, PresentationKey
├── presentation/
│   ├── PresentationHost.kt     builds the sheet strategy; semantics clearing
│   ├── PresentationCard.kt
│   ├── PresentedFrame.kt       covers only (moved out of PresentationHost.kt)
│   └── sheet/
│       ├── SheetSceneStrategy.kt
│       ├── SheetScene.kt
│       ├── SheetSessions.kt    + its NavEntryDecorator and Saver
│       ├── PresentedSheetFrame.kt
│       └── AppSheetNavHost.kt
├── result/
│   ├── NavResultKey.kt
│   └── NavResults.kt           popWithResult, NavResultEffect, the saved store
└── shell/
    └── CardStack.kt            applies recedeBehindSheets; SheetStatusBarEffect

core/designsystem/.../foundation/glass/GlassSheetBackdrop.kt                     (WP05, liquidglass only)

catalog/src/main/kotlin/<pkg>/catalog/sections/sheets/                           (WP03)
├── CatalogSheets.kt            the page and its index
├── SheetDetentDemos.kt
├── SheetStyleDemos.kt
├── SheetBehaviourDemos.kt
└── SheetPresetDemos.kt
```

Deleted: `ds/component/overlay/AppSheet.kt`, `AppSheetState.kt`, `AppActionSheet.kt` (moved and rebuilt),
`nav3/SheetScene.kt` (WP03's, never wired), the legacy `component/overlay/AppBottomSheet.kt` on the integration branch.

Tests mirror these packages (F-6): `core/designsystem/src/test/.../component/sheet/{layout,gesture,draw,back,stack}/`
and `core/navigation/src/test/.../presentation/sheet/`.

---

## 11. Migration

Only the legacy `AppBottomSheet` and `AppActionSheet` have shipped (integration branch `8672003`,
`current-state.md` §1). WP03's `AppSheet` and WP04's `Presentation.Sheet` are unmerged, so they change without
deprecation; the two shipped ones go through one release of `@Deprecated(level = WARNING)` (API-14), because
generated apps take template changes through `upgrade_project.py`.

| From | To |
|---|---|
| `if (show) AppBottomSheet(onDismissRequest, title, showHandle, dismissOnClickOutside) { … }` (`nav4/../overlay/AppBottomSheet.kt:42-49`, a `Dialog`) | `AppSheet(visible = show, onDismissRequest, state = rememberAppSheetState(listOf(SheetDetent.FitContent)), properties = AppSheetProperties(scrimTapDismisses = dismissOnClickOutside), grabber = if (showHandle) AppSheetDefaults.Grabber else null, header = title?.let { t -> { AppSheetHeader(t) } }) { Column { … } }`. The deprecated `AppBottomSheet` becomes exactly that delegation, so old call sites keep working for one release. |
| legacy `AppActionSheet(actions, onDismissRequest, title, message, cancelLabel)` (no `visible`) | `AppActionSheet(visible = true, actions, onDismissRequest, …)`; the deprecated overload delegates. `SheetAction.isDestructive` → `destructive`. |
| WP03 `AppSheet(state, onDismiss, title, showCloseButton, footer)` | `AppSheet(state, onDismissRequest = …, header = { AppSheetHeader(title, trailing = { AppSheetCloseButton() }) }, footer = …)` |
| WP03 `AppSheet(visible, onDismissRequest, detents: Set, title, showCloseButton, footer)` | `AppSheet(visible, onDismissRequest, state = rememberAppSheetState(detents.toList()), header = …, footer = …)` |
| WP03 `AppSheetState(detents: Set)`; `remember { AppSheetState() }` | `rememberAppSheetState(detents: List)`; `currentDetent`, `largeFraction` keep their names; `dismiss(onGone)` → `hide()` plus the caller's own follow-up |
| WP03 `enum SheetDetent { Medium, Large }`, WP04 `enum SheetDetent`, WP07's copy | the sealed `SheetDetent` (`Medium` and `Large` keep their names, so most call sites compile unchanged) |
| WP03 `SheetSceneStrategy`, `SheetPresentation`, `rememberSheetSceneStrategy` | gone; the presentation host owns its strategy |
| WP04 `Presentation.Sheet(detents: List<SheetDetent>)`, string metadata key | `Presentation.Sheet(SheetSpec(detents = …))`, typed `PresentationKey` |
| WP04 `AppShell(sheetStrategy = …)` | parameter removed |
| Catalog demos (`catalog/sections/CatalogOverlays.kt:61-97`, `CatalogAdaptive.kt:135-154`, `CatalogFeel.kt:119`) | the `sheets/` page (§12); the "Filters" demo gets a `LazyColumn` (D13) |
| Settings language picker (`feature/settings/Settings.kt:424-437`) | `AppChoiceSheet` composed inside the language row (A2, A4) |
| Tests `SheetTest`, `RecompositionJourneysTest.kt:171-186`, `PostureTest.kt:70-78` | ported to the new API; every assertion kept (§13) |
| WP08 `appSheet(isPresented:detents: Set<PresentationDetent>)` | the §15 signature; WP08 is hidden on the portal, so no deprecation |

Docs (WP12): `docs/COMPONENTS.md` sheet section, `docs/RECIPES.md` (nested navigation in a sheet, results, persistent
sheet, confirm on dirty), and the agent rules in `AGENTS.md` (§16).

---

## 12. Catalog demos (WP03, `catalog/sections/sheets/`)

Each demo has a trigger with a test tag, so QA2's recordings and `perf.sh` can open it.

| # | Demo | Shows | Covers |
|---|---|---|---|
| 1 | Basic | `[Large]`, header with a close button, a long `LazyColumn` | card stack, rubber band at the top, hand-off |
| 2 | Medium and Large | `[Medium, Large]`, a list; "Resizes" vs "Scrolls" switch | B-14, B-15 |
| 3 | Fit content | a rename form with a sticky Save; keyboard `Lift` vs `Expand` switch | FitContent, footer, B-30, B-31 |
| 4 | Detent lab | `Height(96 dp)`, `Fraction(0.4)`, `Custom("sliver")`, `Large`; buttons that call `animateTo`; a live readout of `currentDetent` and `targetDetent` | §2.4, B-4, B-6 (a button edits the detent list while it is up) |
| 5 | Confirm on dirty | a form with `SheetDismissGuard` and a discard `AppAlert` | B-19, B-21 |
| 6 | Maps-style | a fake map grid behind a `ModalAbove(Medium)`, non-dismissible sheet with a search header | B-20, B-28, Leading placement on a tablet |
| 7 | Now playing | a `NonModal` mini sheet over a scrolling list | B-27, shadow |
| 8 | Styles | Solid / Frosted / Glass, floating inset on and off, corner radius, max width, grouped colours; light and dark side by side | §5 |
| 9 | Stacked sheets | a sheet that opens a second sheet | §4.5, B-35…B-38 |
| 10 | Steps in a sheet | `AppSheetNavHost` with three steps and a result | §2.9 |
| 11 | Action sheet | destructive, disabled and selected actions; a popover on a wide window | §2.8, A1–A5 |
| 12 | Choice sheet | 30 choices with search and a "None" row | `AppChoiceSheet` |
| 13 | Placement | Automatic, Bottom, Center, Leading, Trailing forced | §6 |
| 14 | Long title, large text | a long title between Cancel and Done at font scale 2.0 | D5, REQUIREMENTS 34 |
| 15 | Cross-fade | `SheetTransition.CrossFade`, and the reduce-motion note | B-44 |
| 16 | Predictive back | instructions and a blocked variant | B-23…B-25 |

The "Feel" page keeps one sheet (`CatalogFeel.kt:119`) for CP-style recordings. The sample feature's destination
sheet (WP02-M3) is the registered-destination demo; the catalog links to it.
