# Bottom sheet system: design spec, feel first

Written 2026-10-12 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). This is one of the competing designs for
`research/SHEET-SPEC.md`. Its angle: **iOS 26 fidelity and Apple-designer polish, in our own design language and
tokens**. Android is primary. Compose Multiplatform and SwiftUI parity are in §15 and §16.

Inputs read in full: `research/sheet/current-state.md`, `research/sheet/research.md`, `research/CODE-STANDARDS.md`,
`research/MOTION-PLAN.md` (§1, §2, M1, M6, M17, M22), `decisions/CP1.md`. Code was read in the worktrees and in the
pinned sources; nothing was changed and no Gradle or adb was run.

Tags: **[C]** code fact, `file:line`. **[V]** verified in a library's shipped sources this session. **[A]** Apple
documentation. **[G]** Google documentation or androidx/MDC code. **[M]** measured by a third party (Flutter's iOS 18
constants). **[E]** measured earlier in this project. **[D]** derived here. **[U]** unverified: measure before relying
on it.

Path prefixes:

- `ds/` = `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`
- `nav4/` = `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/`
- `glass/` = `wt/WP05/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/`
- `src/` = `research/sheet/sources/` (pinned androidx, MDC and Flutter copies; see `research.md` "How to read")
- `ui112/` = Compose UI 1.12.1 sources jar (`~/.gradle/caches/.../ui-android-1.12.1-sources.jar`), read this session

Reference phone: Redmi Note 10 Pro, 1080 × 2400 px at 2.75 (392.7 × 872.7 dp, `predictive-back/SPEC.md:65`), 120 Hz
(8.33 ms per frame). Worked numbers below use H = 872.7 dp and a status bar of 33 dp [U: read it on the phone].

---

## 0. Decisions at a glance

| # | Decision | Was / alternative | Why |
|---|---|---|---|
| 1 | **One component, one state type, two hosts.** Destination sheets render in WP04's presentation host; in-screen sheets in WP03's overlay host. Both register in one window-level `SheetStack`, which drives the card stack | Two unrelated paths (current-state §9.1) | The card stack, dimming, Back, kept tabs and semantics then behave the same in both |
| 2 | **Detents are a sealed type**: `Large`, `Medium`, `FitContent`, `Fraction`, `Height`, `Custom(id, resolver)`. A resolver may return `null` (inactive) | Enum `Medium`/`Large` (`ds/component/overlay/AppSheetState.kt:20`) | iOS's model (`research.md` §1.2-1.3) |
| 3 | **Default detents `[Large]`** | `{Medium, Large}` (WP03, WP08) or `[Large]` (WP04, WP07) | Apple: "By default, sheets support the large detent" [A]. A partial detent changes how scrolling behaves, which surprises a full form. The presets (`AppChoiceSheet`, `AppActionSheet`) default to `FitContent`, so they float |
| 4 | **Floating at partial detents, attached at Large.** Inset 8 dp, uniform corners; the inset closes and the bottom corners slide off-screen as the sheet expands | Always attached | iOS 26 (WWDC25 323 [A]). Done with a uniform round-rect outline, so the clip stays a hardware outline on every API (§6.1) |
| 5 | **Content never reflows while the sheet moves.** Content is measured once at the floating width; at Large the surface grows around it. Full-bleed children use `Modifier.sheetBleed()` | Re-measure per frame (forbidden) or clip the gutters | Re-measuring a list per frame is the cost class MOTION-PLAN forbids (`MOTION-PLAN.md:74`) |
| 6 | **Release rule:** a flick ≥ 400 dp/s moves one detent; dismissal needs ≥ 2 H/s or 52 % of the way to hidden; otherwise the stop nearest `offset + 0.099 s · v` | Projection alone (needs ≈ 4.6 H/s to dismiss from Large, `research.md` §1.9) | Detent changes respond to intent; dismissal needs conviction, as on iOS 18 [M] |
| 7 | **Rubber band asymptote = 0.8 % of H above Large**, c = 0.55 | c = 0.55 over the window height (`AppSheetState.kt:161`) | iOS barely lets a large sheet move up: top gap 8 % → 7.2 % of H [M] (`src/flutter/sheet.dart:27-37`) |
| 8 | **One dim, drawn once**, by the presenter, inside its own clip. It is the scrim at partial detents and turns into the 10 % card overlay at Large | Scrim rect on the sheet + overlay on the presenter (two full-screen blends) | One D2 rect per frame (`MOTION-PLAN.md:73`) |
| 9 | **Card stack geometry anchored to the status bar**: the presenter moves down by the status-bar height and scales to 0.9165; the sheet's top is status bar + 10 dp | 7 % of H | On iPhone 0.07 H *is* the status-bar height; on Android's shorter bars 7 % would leave a 30 dp black band (§5.5) |
| 10 | **Stacked sheets: same top, the covered sheet lifts 0.5 % H and scales to 0.9165**, so it peeks 4-5 dp above the new one | iOS 18: the new sheet sits 2 % H higher [M] | 2 % higher puts the grabber into the status bar and the punch-hole camera of the Redmi |
| 11 | **Confirm hook = `SheetDismissGuard(enabled, onAttempt)`** placed in the content. `dismissible = false` is a separate, static "never closable by the person" switch | No veto (`current-state.md` §2.7) | The dirty flag lives next to the form (VitFleet's `BackHandler(enabled = changed)` pattern); a persistent Maps sheet is a different thing |
| 12 | **Back is the MDC bottom container with eased progress and content counter-scale** | Raw progress, squashed text (D6) | `predictive-back/SPEC.md:79`, `:395-404`. Gmail's own sheets are M3 sheets, so this is "Gmail-style" for a sheet |
| 13 | **Rapid Back during a dismiss (N6):** a destination sheet's host gate counts it; an in-screen sheet consumes and drops it | Plan and build disagreed | Each sheet answers to the host that owns it (§4.8) |
| 14 | **Haptic on arrival** at a new detent (`AppHaptic.Threshold`), never on departure, dismissal or a11y actions | On settle start (`AppSheetState.kt:194-195`) | The tick must coincide with the visual snap |
| 15 | **Glass = live sampling at partial detents only**, opaque by halfway to Large, backdrop pass skipped once opaque. Frosted = drawn glass. Solid = opaque | WP05: "no glass on sheets" (`design/wp/WP05.md:64`) | iOS 26 behaviour [A], and the sampling stops where it would cost the most. REQUIREMENTS 33 is later and wins |
| 16 | **Zoom from a source** is a second progress (`morph`) composed with the offset, not a different hidden pose | — | A finger can grab the sheet mid-morph with no jump and no lost velocity (§5.9) |
| 17 | **Form sheet slides up and can be swiped away**; no detents; height `min(0.85 H, fit)` | ADAPTIVE-PLAN: scale-in on the alert spring, drag off (`ADAPTIVE-PLAN.md:434`) | That is what iPadOS form sheets do. A scale-in is the alert's language. **Deviation from ADAPTIVE-PLAN, recorded here** |
| 18 | **Leading/Trailing placement** (iOS 27 `presentationPlacement`) at Expanded width and up: a floating panel with vertical detents | Material side sheet | Maps-style panels on tablets and ChromeOS |
| 19 | **The sheet adds no scroller.** Content brings one; `SheetColumn` is the scrolling column for forms | VitFleet wraps every sheet in `verticalScroll` | A nested scroller inside a lazy list breaks; one obvious way is better |
| 20 | **Header sits above the content, never over it**; its scroll-edge fade draws over the content's top only after the content has scrolled | iOS 26 lets content run under the toolbar | REQUIREMENTS 34: nothing may be clipped by chrome. A caller who forgets padding would lose content under an overlaid header |
| 21 | **Nested navigation inside a sheet: `AppSheetNavHost`** (a nested stack), not routing through the presented stack | Pushes inside a sheet replace it with a cover (N5) | MASTER-PLAN A3.7 promised it |
| 22 | **Token home: a new `sheet` section in `tokens/motion.json`**, written by the generator to Android, Multiplatform and SwiftUI | Private `val`s in `AppSheet.kt:334-345` | One source of truth for three templates |
| 23 | **Scrim/dim strength for sheets: black 0.20 light, 0.48 dark** [U] | `colors.scrim` 0x66 / 0x8F (`ds/theme/tokens/DesignTokens.kt:249`, `:292`) | The iOS dim values the project already uses for alerts (`ios-motion-catalogue.md:43-44`); confirm on an iOS 26 recording |

---

## 1. Feel rules

These are the rules every later section implements. A reviewer can cite them.

- **F1 The finger owns the sheet.** It tracks 1:1. A touch catches a sheet that is presenting or settling. A release
  seeds a spring with its velocity. There is never a tween after a finger (`MOTION-PLAN.md:54-58`).
- **F2 Content never reflows while the sheet moves.** It is measured once; only the glass around it changes.
- **F3 Stops are chosen by intent; dismissal needs conviction.** A small flick changes detent. Closing needs a fast
  flick or most of the travel.
- **F4 Edges resist; nothing stops dead.** Past the top, and past the bottom when dismissal is blocked, the sheet
  rubber-bands with iOS's `c = 0.55`.
- **F5 Floating while partial, anchored when full.**
- **F6 One dim, drawn once**, in the presenter's layer.
- **F7 A haptic marks arrival.**
- **F8 Nothing with a shadow ever fades through it.** Shadows are cut out of the shape's inside, and never resized per
  frame.
- **F9 One gesture closes**, unless the content says it is precious (the guard).
- **F10 Every per-frame value is read in placement, a layer block or draw.** Zero recompositions per drag frame.

---

## 2. Anatomy, layers and states

### 2.1 Layers

```
window root (AppTheme)
├── shell layer (tabs + floating bar)        Modifier.cardStackPresenter(stack, level = 0)
│                                             scale, translationY, outline clip (L0) + one dim rect (D2)
├── presentation host (layer 3)               destination sheets and covers (WP04)
│   └── cover entry                           cardStackPresenter(level = n) when a sheet is over it
└── overlay host (layer 4: in-screen sheets, alerts, HUD; layer 5: toasts)   (WP03)

SheetLayout: one Layout filling the window
├── scrim node        hit-testing and semantics; draws the dim only when no presenter is attached
└── sheet layer       placed at the offset (L1); graphicsLayer: back shrink, zoom scale (L0);
    │                 cardStackPresenter(level) when another sheet covers it
    ├── surface node  body, rim, cut-out shadow (draw; re-recorded only in the attach band, §5.4)
    ├── header        grabber band + optional AppSheetHeader (pinned)
    ├── content layer clip outline + back counter-scale (L0); measured once
    └── footer        placed at the visible bottom, above the IME (L1)
```

The z-order rule: content (0) < tab bar (1) < presentation host (3) < overlay host (4) < toasts (5)
(`ds/component/overlay/AppOverlayHost.kt:20-33` [C]; `nav4/presentation/PresentationHost.kt:53-56` [C]). An in-screen
sheet is hidden while anything is presented above its host (§4.12), so a destination sheet never draws under it.

### 2.2 States and interruption

```
Hidden ──show──▶ Presenting ──arrives──▶ Resting(detent) ◀──settle── Settling ◀──release── Dragging
                     │  ▲                     │  ▲                      │  ▲                  ▲
                     │  └──finger catches─────┼──┼──────────────────────┘  │                  │
                     │                        │  └──finger down───────────┼──────────────────┘
                     └──────── dismiss ───────┴──────────────▶ Dismissing ──gone──▶ Hidden (onRemove returns)
```

| Running | Finger down on the sheet | Back gesture | `animateTo` | `visible = false` / pop | Window resize |
|---|---|---|---|---|---|
| Presenting | catches it; velocity kept | shrink, then dismiss | retargets, velocity kept | reverses to hidden, velocity kept | anchors retarget in flight |
| Resting | drag | shrink | animates | dismisses | stays on its detent |
| Settling | catches it | shrink | retargets | dismisses, velocity kept | retargets |
| Dismissing, committed by the person | ignored | consumed (in-screen) or counted by the host gate (destination) | ignored (fixes D9) | no-op | hidden retargets |
| Dismissing, programmatic (`visible = false`) | ignored | consumed | ignored | — | — |
| ... then `visible = true` again | — | — | — | retargets back up to the initial detent with its velocity (fixes D1) | — |

A person who flicked a sheet away has committed; catching it on the way out would undo a decision, so it is ignored.
A program that hides and re-shows within the slide gets a smooth reversal instead of a stuck sheet.

---

## 3. Public API (Android, `:core:designsystem`)

Package `com.base.app.core.designsystem.component.sheet`. File layout in §10. Every public declaration gets KDoc
(C-4); the KDoc below is the intent the real KDoc must carry, trimmed of anything the signature already says (C-5).

### 3.1 Detents

```kotlin
/**
 * A height a sheet can rest at. A sheet lists one or more; the person moves between them by dragging,
 * flicking, tapping the grabber, or with TalkBack actions.
 *
 * Each detent resolves against the window the sheet is shown in, again whenever that window changes size,
 * and may resolve to nothing: an inactive detent is skipped, as iOS skips `medium` in compact height.
 * Detents are ordered by resolved height; two that resolve to the same height count once.
 */
@Immutable
sealed interface SheetDetent {
    /** Nearly the whole window; what is behind recedes into a card. Always active. */
    data object Large : SheetDetent

    /** About half the window ([AppSheetDefaults.MediumFraction]). Inactive in Compact height. */
    data object Medium : SheetDetent

    /**
     * The sheet's own height: header, content and footer as measured, capped at [Large]. Content that
     * scrolls measures as tall as [Large]. When the content's height changes, the detent re-resolves and
     * a sheet resting on it animates to the new height.
     */
    data object FitContent : SheetDetent

    /** [fraction] of the height [Large] shows, in (0, 1]. */
    data class Fraction(val fraction: Float) : SheetDetent

    /** Shows [height] of the sheet above the system navigation area: a peek, a mini player. */
    data class Height(val height: Dp) : SheetDetent

    /**
     * A height in pixels from [resolve], or `null` to leave the detent out in that window. [id] names it
     * for equality and state restoration. [resolve] runs in measure: it must read no snapshot state.
     */
    class Custom(val id: String, val resolve: SheetDetentScope.() -> Float?) : SheetDetent
}

/** What a [SheetDetent.Custom] resolver sees. Heights are pixels of sheet showing above the navigation area. */
interface SheetDetentScope : Density {
    /** What [SheetDetent.Large] shows. iOS calls it `maximumDetentValue`. */
    val maxHeight: Float
    /** Header, content and footer as measured. */
    val contentHeight: Float
    val windowSize: SizeClass
}
```

Resolution (in the sheet's measure pass, never per frame):

| Detent | Visible height (above the bottom anchor) | Inactive when |
|---|---|---|
| `Large` | `H − largeTop`, `largeTop = statusBars.top + 10 dp` | never |
| `Medium` | `0.5 · H` (sheet top at the window's middle) | Compact height; or within 48 dp of `Large` |
| `FitContent` | header + content (loose measure, max = Large's content height) + footer + bottom safe inset | never (caps at Large) |
| `Fraction(f)` | `f · maxHeight` | `f` outside (0, 1] (a `require` in the constructor) |
| `Height(h)` | `h` + bottom safe inset | taller than `Large` (clamped to it) |
| `Custom` | the resolver's value, clamped to `[48 dp, maxHeight]` | resolver returns `null` |

If the initial detent is inactive, the sheet opens at the nearest active one. In tabletop posture `Medium` rests on the
fold (`ds/component/overlay/AppSheet.kt:138-139`, `:235` [C]; kept).

### 3.2 State

```kotlin
/**
 * Where a sheet is and where it may go, kept outside composition so whoever presents it can move it or
 * take it away animated: a programmatic detent change, a pop, a parent hiding it.
 *
 * [presentationFraction] and [expansionFraction] change on every frame of a drag: read them in a layout,
 * layer or draw block only (lint `FrequentlyChangingValue`). [currentDetent], [targetDetent] and
 * [isVisible] change at most once per settle and may be read in composition.
 *
 * Built on foundation's `AnchoredDraggableState` (M-3). A retarget keeps the current velocity.
 */
@Stable
interface AppSheetState {
    val detents: List<SheetDetent>

    /** The detent the sheet rests on or is settling onto; null while hidden or leaving. */
    val currentDetent: SheetDetent?

    /** Where the sheet is heading: the release target while settling, the nearest stop while dragged. */
    val targetDetent: SheetDetent?

    val isVisible: Boolean
    val isDismissing: Boolean

    /** 0 hidden, 1 at the lowest detent and above. Drives the dim. */
    @get:FrequentlyChangingValue
    val presentationFraction: Float

    /**
     * 0 at the highest partial detent and below, 1 at [SheetDetent.Large]. Drives the card stack, the
     * floating-to-attached change and the glass opacity. Always 0 for a sheet without Large.
     */
    @get:FrequentlyChangingValue
    val expansionFraction: Float

    suspend fun animateTo(detent: SheetDetent)
    suspend fun snapTo(detent: SheetDetent)

    /** One detent up; false at the top. */
    suspend fun expand(): Boolean

    /** One detent down, never to hidden; false at the bottom. */
    suspend fun collapse(): Boolean

    /**
     * Closes the sheet and returns once it has gone. Idempotent: a call during a dismissal joins it. Not
     * asked of a [SheetDismissGuard]: a program closing its own sheet has already decided.
     */
    suspend fun dismiss()

    /** Replaces the detents. A sheet resting on one that remains stays; otherwise it animates to the nearest. */
    fun updateDetents(detents: List<SheetDetent>)
}

@RememberInComposition
fun AppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState

/**
 * Saved across process death: the restored sheet comes back on the detent it rested on, without
 * animating in again. A changed [detents] list is applied with [AppSheetState.updateDetents].
 */
@Composable
fun rememberAppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState
```

The `Saver` stores the settled detent's identity (`Large`/`Medium`/`FitContent` by name, `Fraction` and `Height` by
value, `Custom` by `id`) and a `restored` flag that turns the next `show` into a snap (as androidx did in `f22fad2c`,
`research.md` §2.11 [G]). The interface plus factory follows API-8 (`CODE-STANDARDS.md:187-191`).

### 3.3 Options

```kotlin
/** What the sheet's surface is made of. */
enum class SheetStyle {
    /** Glass when the app ships glass and the tier allows it, otherwise Solid. */
    Automatic,
    /** The opaque elevated surface. */
    Solid,
    /** Drawn glass: translucent, lit, never sampling what is behind. Cheap in every tier. */
    Frosted,
    /** Live glass at partial detents, through `LocalGlassQuality`; opaque at Large. Frosted below the Live tiers. */
    Glass,
}

@Immutable
sealed interface SheetModality {
    /** Dims what is behind and takes every touch outside the sheet; screen readers see only the sheet. */
    data object Modal : SheetModality

    /**
     * Leaves the screen behind usable while the sheet rests at or below [upThrough], as Maps does. Above it
     * the sheet dims and blocks like a modal one, the dim fading in across the detent above [upThrough].
     * With [aboveBottomChrome], the sheet rests above the floating tab bar at undimmed detents.
     */
    data class NonModal(
        val upThrough: SheetDetent,
        val aboveBottomChrome: Boolean = true,
    ) : SheetModality
}

/** What a vertical scroll inside the sheet does first. */
enum class SheetContentInteraction {
    /** Grows the sheet to its top detent, then scrolls (iOS's default). */
    Resizes,
    /** Scrolls first; the grabber and the header still resize. For a non-modal sheet. */
    Scrolls,
}

/** Where the sheet appears. Leading and Trailing apply at Expanded width and up, Bottom below. */
enum class SheetPlacement { Automatic, Bottom, Center, Leading, Trailing }

/** Automatic: shown when the sheet has two or more active detents; hidden in Center placement and when covered. */
enum class SheetGrabber { Automatic, Visible, Hidden }

/** How the sheet arrives when no source is given. Reduce motion always uses CrossFade. */
enum class SheetTransition { Slide, CrossFade }
```

For navigation metadata, the same choices travel as one object:

```kotlin
/** A sheet's presentation as data, for a destination registered with `Presentation.Sheet(options)`. */
@Immutable
class SheetOptions(
    val detents: List<SheetDetent> = AppSheetDefaults.Detents,
    val initialDetent: SheetDetent = detents.first(),
    val style: SheetStyle = SheetStyle.Automatic,
    val modality: SheetModality = SheetModality.Modal,
    val dismissible: Boolean = true,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Resizes,
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val grabber: SheetGrabber = SheetGrabber.Automatic,
    val transition: SheetTransition = SheetTransition.Slide,
)
```

### 3.4 The component

```kotlin
/**
 * A sheet, as iOS 26 draws one, in the app's design language. It rises to its detent; floats inset with
 * rounded corners while partial and anchors to the window's edges at [SheetDetent.Large], where the screen
 * behind recedes into a card. It can be dragged between detents, flicked away, closed with Back (the
 * bottom-container shrink), the scrim, Esc or a close button, and morphs out of [source] when one is given.
 *
 * [onDismissRequest] runs once, at the moment the person closes the sheet; the sheet then animates out on
 * its own. Remove it (set `visible` false, pop the entry) in that callback.
 *
 * The content is measured once and only moved after that: dragging re-places it and never re-measures or
 * recomposes it. Lists pass [SheetContentScope.contentPadding] to `contentPadding` so their last item stays
 * reachable above the footer, the navigation bar and the keyboard. A form uses [SheetColumn].
 *
 * The sheet adds no scroller. Edge-to-edge children use `Modifier.sheetBleed()`.
 *
 * @param dismissible false keeps the sheet up whatever the person does: no swipe away, no scrim tap, and
 *   Back passes to the screen beneath once the sheet rests on its lowest detent. For a persistent sheet.
 *   To ask before closing a dirty form, put a [SheetDismissGuard] in the content instead.
 * @param floatingInset the gap around a sheet resting at a partial detent; 0 keeps it attached.
 * @param onDetentChange runs when the person settles the sheet on a different detent; not for programmatic moves.
 */
@Composable
fun AppSheet(
    state: AppSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    style: SheetStyle = SheetStyle.Automatic,
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    contentInteraction: SheetContentInteraction = SheetContentInteraction.Resizes,
    placement: SheetPlacement = SheetPlacement.Automatic,
    grabber: SheetGrabber = SheetGrabber.Automatic,
    transition: SheetTransition = SheetTransition.Slide,
    source: SheetSource? = null,
    cornerRadius: Dp = AppSheetDefaults.cornerRadius(),
    floatingInset: Dp = AppSheetDefaults.floatingInset(),
    colors: AppSheetColors = AppSheetDefaults.colors(style),
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    header: (@Composable SheetHeaderScope.() -> Unit)? = null,
    footer: (@Composable SheetContentScope.() -> Unit)? = null,
    content: @Composable SheetContentScope.() -> Unit,
)

/**
 * A sheet shown from inside a screen, for a choice that is not a destination of its own. Keep it composed
 * and drive [visible]; it draws in the window's overlay layer, over the tab bar. Hiding it plays the exit;
 * showing it again during the exit reverses it smoothly.
 */
@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
    // style … colors, onDetentChange: as above
    header: (@Composable SheetHeaderScope.() -> Unit)? = null,
    footer: (@Composable SheetContentScope.() -> Unit)? = null,
    content: @Composable SheetContentScope.() -> Unit,
)

/**
 * A sheet for [item] while it is not null, like SwiftUI's `sheet(item:)`. The last item stays composed
 * during the exit. A different item while one is shown closes the sheet, then presents the new one: one
 * sheet at a time, as the HIG asks.
 */
@Composable
fun <T : Any> AppSheet(
    item: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    // detents … as the `visible` overload
    content: @Composable SheetContentScope.(T) -> Unit,
)
```

Parameter order follows API-2: required, `modifier`, behaviour, variants, geometry, colours, callbacks, then slots.

### 3.5 Scopes, guard, header, helpers

```kotlin
@Stable
interface SheetContentScope {
    /**
     * Padding that keeps the last item reachable above the footer, the navigation bar, the keyboard and
     * the part of the sheet below the visible bottom at the detent it rests on. It changes only when the
     * sheet settles, the footer's settled height changes or the keyboard's target changes, so a list
     * re-measures at most once per detent and never per drag frame.
     */
    val contentPadding: PaddingValues

    /** Asks to close, as the scrim or Back would; a [SheetDismissGuard] may keep the sheet up. */
    fun requestDismiss()

    /** Closes without asking: a Done button after the work is saved. */
    fun dismiss()
}

@Stable
interface SheetHeaderScope : SheetContentScope {
    /** True once the content has scrolled away from its top. Read in draw: the header's edge fade. */
    val isContentScrolled: () -> Boolean
}

/**
 * Keeps the enclosing sheet up while [enabled]. A swipe down rubber-bands and springs back, and a scrim
 * tap, Back and Esc do not close it; [onAttempt] runs once per attempt so the screen can ask, typically
 * with an `AppActionSheet` offering Discard and Keep Editing. While [enabled] is false the sheet behaves
 * as if the guard were absent, so a clean form keeps the system's predictive back.
 *
 * Closing from code ([SheetContentScope.dismiss], [AppSheetState.dismiss], a pop) is not guarded.
 */
@Composable
fun SheetDismissGuard(enabled: Boolean, onAttempt: () -> Unit)

/**
 * The bar at the top of a sheet: a centred [title] with [leading] and [trailing] actions (Cancel, Done,
 * close). The title never runs under an action: the wider action's width is reserved on both sides. At
 * large font sizes the title takes two lines and the bar grows. It also names the sheet for TalkBack.
 */
@Composable
fun SheetHeaderScope.AppSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = { AppSheetCloseButton() },
)

/** A circular close button that asks the sheet to close (guarded). Glass in the Glass style. */
@Composable
fun SheetContentScope.AppSheetCloseButton(modifier: Modifier = Modifier)

/** A scrolling column with [SheetContentScope.contentPadding] applied: a form, a settings group. */
@Composable
fun SheetContentScope.SheetColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
)

/**
 * Widens an edge-to-edge child (a hero image, a full-width row highlight) by the sheet's floating inset on
 * each side, so it meets the window's edges at Large and the sheet's edges when it floats. Measured once.
 * A no-op outside a sheet.
 */
fun Modifier.sheetBleed(): Modifier

/** Where a sheet morphs from: a button, a card. Hold it with [rememberSheetSource]. */
@Stable
class SheetSource internal constructor()

@Composable
fun rememberSheetSource(): SheetSource

/**
 * Marks this element as [source]'s bounds and shape. Its bounds are read in placement, never in
 * composition, and the element hides in its own layer while the sheet morphs out of it or back into it.
 */
fun Modifier.sheetSource(source: SheetSource, shape: Shape = RectangleShape): Modifier
```

### 3.6 Defaults and colours

```kotlin
object AppSheetDefaults {
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)
    const val MediumFraction: Float = 0.5f
    val LargeTopGap: Dp = 10.dp
    val GrabberSize: DpSize = DpSize(36.dp, 5.dp)
    val GrabberTop: Dp = 5.dp
    val ContentTop: Dp = 15.dp

    /** The style's sheet radius: 24 / 30 / 8 / 28 dp for Utility / Social / Editorial / Playful. */
    @Composable @ReadOnlyComposable fun cornerRadius(): Dp

    /** 8 dp; 0 for the Editorial style, whose square sheets read wrong floating inside rounded display corners. */
    @Composable @ReadOnlyComposable fun floatingInset(): Dp

    @Composable fun colors(style: SheetStyle = SheetStyle.Automatic): AppSheetColors
}

/** The sheet's colours. Resolved per theme and style; override any of them. */
@Immutable
class AppSheetColors(
    /** The opaque surface: Solid everywhere, and every style at Large. */
    val surface: Color,
    /** The translucent body at partial detents (Frosted, Glass); equal to [surface] for Solid. */
    val floatingBody: Color,
    val rim: Color,
    val grabber: Color,
    /** The dim at full strength, alpha included. */
    val dim: Color,
    /** The card-stack overlay at full strength, alpha included. */
    val stackOverlay: Color,
    /** Null in dark theme, where a shadow would not show and would still cost its blur. */
    val shadow: Shadow?,
)
```

Values per theme are in §6.9. API-6 holds: no default expression calls anything private.

### 3.7 Card stack seam

```kotlin
/**
 * The sheets up in this window, for whatever recedes behind them. `AppTheme` installs one at the window
 * root; both the overlay host and the presentation host register their sheets in it.
 *
 * Level 0 is the window's root content (the shell). Each presented cover or sheet takes the next level; a
 * sheet's presenter is the level directly beneath it.
 */
@Stable
class SheetStack internal constructor() {
    /** How far the presenter at [level] has receded, 0 upright to 1 behind a sheet at Large. Read in a layer or draw block. */
    @FrequentlyChangingValue fun recede(level: Int): Float

    /** The dim to draw over the presenter at [level], alpha included. Read in draw. */
    @FrequentlyChangingValue fun dim(level: Int): Color

    /** True while the root has receded past halfway: the status bar's icons turn light. Changes rarely. */
    val rootReceded: Boolean
}

val LocalSheetStack: ProvidableCompositionLocal<SheetStack?>

/**
 * Recedes this layout behind the sheets presented over [level], as iOS's card stack does, and draws their
 * dim over it. One layer (scale, translation, outline clip) and one rect, all read in layer and draw
 * blocks: a presenter under a moving sheet never recomposes.
 */
fun Modifier.cardStackPresenter(stack: SheetStack, level: Int = 0): Modifier
```

This replaces `largeFraction` on `AppSheetState` (`AppSheetState.kt:74-82` [C]) and on `SheetSceneStrategy`
(N10): the shell no longer casts a `SceneStrategy` to read it, and in-screen sheets reach the shell too.

### 3.8 Presets

```kotlin
/**
 * A short list of actions. On a phone it is a floating `FitContent` sheet of two groups (actions, then
 * Cancel), arriving on the `dialog` spring. With a [source] (iOS 26), or at Medium width and up, it is a
 * menu anchored to the source instead, with no Cancel row. Scrolls past 60 % of the window.
 */
@Composable
fun AppActionSheet(visible, actions: List<SheetAction>, onDismissRequest, modifier, title, message,
                   cancelLabel, source: SheetSource? = null)

/**
 * Pick one value (or several with [multiple]): rows with a radio or checkbox role and a checkmark, an
 * optional "None" row, a search field from six options, `FitContent` capped at 60 % of the window.
 */
@Composable
fun <T> AppChoiceSheet(visible, options: List<T>, selected: Set<T>, onSelectedChange, onDismissRequest,
                       label: (T) -> String, modifier, title, multiple = false, noneLabel: String? = null)
```

`SheetAction` gains `selected: Boolean = false` (A2). Both presets are thin: they call `AppSheet`.

### 3.9 Use

```kotlin
// A filter sheet from a screen: two detents, a list, a pinned Apply.
var filtersShown by rememberSaveable { mutableStateOf(false) }
AppSheet(
    visible = filtersShown,
    onDismissRequest = { filtersShown = false },
    detents = listOf(SheetDetent.Medium, SheetDetent.Large),
    header = { AppSheetHeader(title = stringResource(R.string.orders_filters), leading = null) },
    footer = { AppButton(text = stringResource(R.string.orders_apply), onClick = { onEvent(ApplyClicked); dismiss() }) },
) {
    LazyColumn(contentPadding = contentPadding) { items(filters, key = { it.id }) { FilterRow(it) } }
}

// A compose sheet that asks before discarding.
AppSheet(visible = composing, onDismissRequest = { onEvent(ComposeClosed) }) {
    SheetDismissGuard(enabled = state.isDirty, onAttempt = { onEvent(DiscardAsked) })
    SheetColumn { /* fields */ }
}

// A destination: registered once, opened by navigating to its key.
navGraph {
    entry<FiltersKey>(presentation = Presentation.Sheet(SheetOptions(detents = listOf(Medium, Large)))) { FiltersRoute() }
}
```

Don't:

```kotlin
// A tween after a finger, a scrim faded with layer alpha, an offset read in composition.
Box(Modifier.graphicsLayer { alpha = scrim }.background(Color.Black))      // offscreen full-screen layer
Modifier.offset(y = state.offsetDp)                                       // recomposes every drag frame
LazyColumn { … }                                                          // no contentPadding: the last row hides under the footer
```

---

## 4. Behaviour

### 4.1 Present

- **Slide:** from below the window to the initial detent on `springs.sheet`, the clock started after the first
  composed frame is drawn (SMOOTHNESS S3, CP1 §1 condition 1: the first visible frame shows ≤ 4 % of the travel).
- **Zoom:** with a `source`, the surface morphs out of the source's bounds and corners (§5.9).
- **Cross-fade:** `SheetTransition.CrossFade` or reduce motion: the sheet layer fades in over 150 ms with
  `CompositingStrategy.ModulateAlpha` at its detent; nothing moves (`MOTION-PLAN.md:62`).
- First frame: the sheet is composed hidden and drawn in its initial pose on the frame it is composed (API-13); no
  `LaunchedEffect` sets up initial state. The show animation starts from a `LaunchedEffect` that waits for the first
  frame with anchors, as today (`AppSheetState.kt:107-118`, `:132` [C]).
- A restored sheet (process death) snaps to its saved detent without the slide.

### 4.2 Gestures

- **Drag anywhere on the sheet**: header, footer, and content that does not scroll. One `anchoredDraggable` on the
  sheet layer; `startDragImmediately = isAnimationRunning` lets a touch catch a presenting or settling sheet
  (`src/foundation-1.12.1/.../AnchoredDraggable.kt:479` [G]).
- **Nested scroll** for scrollables (§4.6).
- **Grabber tap** cycles up through the active detents and wraps to the lowest; it never dismisses (Material: "toggle
  through preset heights or close", `research.md` §2.2 [G]; iOS cycles, `ios-motion-catalogue.md:164` [A]).
- **Press delay.** Compose 1.11 delays presses inside drag containers (`isDelayPressesUsingGestureConsumptionEnabled`,
  `research.md` §2.6 [G]). Press feedback on rows inside a sheet must still appear within the tap timeout [U]: a
  Compose test asserts the row's pressed state within 100 ms of DOWN.
- **Child wins the slop race?** When a scrollable child is touched during a settle, the parent's
  `startDragImmediately` must win, so the settle stops and the sheet follows the finger (fixes D8). If a Robolectric
  test shows the child crossing slop first, add an `Initial`-pass pointer handler on the sheet layer that stops the
  animation on DOWN [U].

### 4.3 Choosing a detent on release

Stops = active detents ordered by height, plus hidden when dismissal is allowed. `v` in px/s, positive downward. H =
window height in px.

1. **Over-pulled above the top detent** → the top detent.
2. **Between two detents:**
   - `|v| ≥ 400 dp/s` → the stop beyond the release point in the direction of `v` (one stop per flick);
   - otherwise the stop nearest `offset + 0.099 s · v` (iOS's 0.99/ms projection, `AppSheetState.kt:216-219` [C]).
3. **Between the lowest detent and hidden** (dismissal allowed):
   - `v ≥ 2.0 H/s` downward → hidden;
   - otherwise hidden only if `(offset + 0.099 s · v − lowest) / (hidden − lowest) > 0.52`; else the lowest detent.
   - `v ≤ −400 dp/s` (upward) → the lowest detent.
4. **Dismissal blocked** (guard enabled or `dismissible = false`): rule 3 picks the lowest detent; with a guard, if
   rule 3 would have dismissed, `onAttempt` runs once for this gesture.

The settle animates with the release velocity on `springs.sheet`. This replaces both the 1400 px/s threshold of the
old build and the projection-only rule, and keeps MOTION-PLAN M6's "dismiss at ≥ 2 H/s or past 52 %"
(`MOTION-PLAN.md:163`) [M].

Worked on the Redmi (H 872.7 dp; Medium top 436 dp; Large top 43 dp):

| Release | Rule | Lands |
|---|---|---|
| At Medium, 300 dp/s up | 2, projection: 436 − 30 = 406 | Medium |
| At Medium, 600 dp/s up | 2, flick | Large |
| 60 dp above Medium, 200 dp/s down | 2, projection: 376 + 20 = 396, nearest Medium | Medium |
| At Large, 1200 dp/s down | 2, flick (one stop) | Medium |
| At Medium, 1200 dp/s down (< 1745 dp/s = 2 H/s) | 3, position: (436 + 119 − 436)/437 = 0.27 | Medium |
| At Medium, 1800 dp/s down | 3, ≥ 2 H/s | hidden |
| 250 dp below Medium, 0 dp/s | 3, position 250/437 = 0.57 > 0.52 | hidden |

Whether iOS lets one very fast flick from Large skip Medium and dismiss is not documented [U]. One stop per flick is
predictable and matches androidx's `computeTarget` (`AnchoredDraggable.kt:1483-1514` [G]). Record an iOS reference
and revisit.

### 4.4 Rubber band

The formula is iOS's, shared with overscroll and pull to refresh: `shown = c · x · d / (d + c · x)`, `c = 0.55`
(`ds/theme/AppMotion.kt:182-186` [C]).

| Where | `d` (the asymptote) | On the Redmi |
|---|---|---|
| Above the top detent | `topDetentTop − (largeTop − 0.008 H)` | at Large: 7.0 dp, so a 200 dp over-pull shows 6.6 dp. Medium-only sheet: 400 dp, so 200 dp shows 86 dp |
| Below the lowest detent, dismissal blocked | `hidden − lowestTop` | from Medium: 437 dp, so 300 dp shows 120 dp |

Release from a stretch: `springs.sheet` with the release velocity. A settle that overshoots the top detent with a hard
flick passes through the same band, so the visible overshoot is capped at 0.008 H and no gap opens under the status
bar. The surface stretches with the top edge rather than lifting off its bottom (§5.4), so a floating sheet never
shows a gap below itself while stretched.

Implemented as a custom `OverscrollEffect` given to `anchoredDraggable(overscrollEffect = …)`, whose excess the sheet
reads in placement (`src/foundation-1.12.1/.../AnchoredDraggable.kt:410-477` [G]; M-3).

### 4.5 Dismissal and the confirm hook

| Path | No guard | Guard enabled | `dismissible = false` |
|---|---|---|---|
| Swipe / flick down | §4.3 rule 3 | rubber band at the lowest detent; `onAttempt` once if rule 3 would have closed it | rubber band; nothing else |
| Scrim tap | closes | `onAttempt` | no scrim tap target |
| Back key / button | closes | `onAttempt` | above the lowest detent: collapses one detent; at it: not handled, Back goes to the screen |
| Back gesture | bottom-container shrink, commit closes | shrink at 20 % strength, commit springs back and runs `onAttempt` | as the Back key, with a 24 dp downward follow as the preview |
| Esc | closes | `onAttempt` | as the Back key |
| Close button (`requestDismiss`) | closes | `onAttempt` | closes (the app put the button there) |
| `dismiss()`, `visible = false`, pop | closes | closes | closes |

- `onDismissRequest` runs once, at the commit, for every person-initiated path. Programmatic paths do not call it.
- The attempt fires on release, not mid-drag [U: iOS's exact moment, WWDC19 224]. No haptic on an attempt: the
  confirmation the app shows is the feedback.
- A guard with `enabled = false` registers nothing (one `NavigationEventHandler` that branches inside
  `onBackCompleted`, navigation-event skill `SKILL.md:255-266` [G]), so a clean form keeps the system's predictive
  back.

### 4.6 Nested scroll and reaching the last item

`SheetContentInteraction.Resizes` (default; iOS `prefersScrollingExpandsWhenScrolledToEdge = true` [A]):

- `onPreScroll`, upward, `UserInput`, sheet below its top detent → the sheet takes the delta.
- `onPostScroll`, downward leftover, `UserInput`, content at its top → the sheet takes it.
- `onPreFling`: if the sheet is not resting, settle it with the velocity (§4.3) and consume all of it.
- Momentum never moves the sheet: a fling that reaches the top bounces in the list's own overscroll
  (`AppSheet.kt:160-161` keeps `source == UserInput` [C]; kept).

`Scrolls`: upward deltas go to the content first and never grow the sheet; downward at the top still drags it.

**Reachability.** The content is measured at Large's height, so at a partial detent part of it is below the visible
bottom. `contentPadding.bottom = (largeBottom − settledVisibleBottom) + footer + bottom safe inset`, recomputed when
the sheet *settles*, never during a drag. A list scrolled at Medium can therefore reach its last row (iOS resizes the
view per detent; this gets the same result with at most one re-measure per settle).

### 4.7 Keyboard

- **The sheet rides the keyboard.** The sheet's bottom anchor is `max(navigation area, IME)`. Detent offsets are
  measured from that anchor, so a Medium sheet moves up with the keyboard and keeps its height while it fits; when it
  no longer fits, its top stops at `largeTop` and the visible height shrinks. The footer sits on the keyboard.
  [U: whether iOS lifts a medium sheet or grows it to large.]
- **API 30+:** the animated IME inset is read in the placement block, so the sheet follows the system's 285 ms curve
  frame by frame with no recomposition (`MOTION-PLAN.md:403`; `research.md` §2.10).
- **API 26-29:** the inset jumps; the anchor animates to it on `springs.screen` (M22).
- **Measure at the target, place at the animated value.** Content constraints use the IME *target* height (one
  re-measure when the keyboard starts and one when it ends), so `bringIntoView` already knows the final viewport on
  the first frame. Placement follows the animated value.
- **Insets are consumed once.** The sheet consumes `navigationBars` and `ime` for its children, so an
  `AppBottomActionBar` in the footer pads nothing twice (fixes D2), and content without a footer still clears the
  navigation bar through `contentPadding` (fixes D3). A field at Medium is never under the keyboard (fixes D4).
- Back with the keyboard up closes the keyboard first [U: the IME's window takes Back before `PRIORITY_OVERLAY`; a
  test on the phone].

### 4.8 Back, predictive back and Esc

- **Handler:** `OverlayBackHandler` at `PRIORITY_OVERLAY` on the caller's dispatcher, as today
  (`ds/component/overlay/OverlayBackHandler.kt:27-51` [C]), so a sheet in a hidden kept tab is deaf.
- **Progress:** `p = CubicBezierEasing(0.1, 0.1, 0, 1)(event.progress)` (`src/BackHandler.kt:47-51` [G]), held in a
  float state read only in the sheet layer's `graphicsLayer` (androidx reads it in composition, `src/BottomSheet.kt:168`;
  do not copy that).
- **Pose** (MDC bottom container, `predictive-back/SPEC.md:79` [G]):
  - sheet layer `scaleX = 1 − 48 dp · p / w`, `scaleY = 1 − 24 dp · p / h`, where `h` is the *visible* height;
  - `transformOrigin` at the visible bottom centre;
  - content layer `scaleY = scaleX / scaleY` about its top, so text keeps its aspect ratio (fixes D6;
    `src/BottomSheet.kt:475-480` [G]).
- **Commit:** the exit slides the shrunken sheet down on `springs.sheet`; the scale is held until it has gone, then
  reset (MDC, `research.md` §2.5). One gesture closes the sheet from any detent; it does not step down a detent
  (androidx's behaviour, `research.md` §3 pitfall 5).
- **Cancel:** the system springs progress back to 0 before `onBackCancelled` (`predictive-back/SPEC.md:77`); if it is
  not at 0, finish on `springs.sheet`.
- **Guard:** the shrink is scaled by 0.2 (it reads as resistance), and the commit springs back and runs `onAttempt`.
- **Back key:** no shrink; same commit.
- **Esc:** an explicit `onPreviewKeyEvent(Key.Escape)` on the sheet, not the key-map fallback (fixes D16,
  `ADAPTIVE-PLAN.md:565`).
- **Rapid Back during a dismiss (N6), decided:**
  - *Destination sheets:* `SheetEntryFrame` reports the sheet as moving to the presentation host's `BackGate` while it
    presents or dismisses (`nav4/back/BackGate.kt:67-74` [C]). Presses meanwhile are counted and applied as one pop at
    99 % of the exit, as for screens (`BackGate.kt:108` [C]). MASTER-PLAN:911 holds.
  - *In-screen sheets:* no stack owns them. Back during the exit is consumed and dropped (today's tested behaviour,
    `SheetTest.kt:65-79`), so a burst never pops the screen beneath by accident.

### 4.9 Stacking

- A sheet presented from a sheet takes the next level. The covered sheet runs `cardStackPresenter(level)` on its own
  layer: it lifts 0.5 % of H and scales to 0.9165 about its top centre, with the 10 % overlay, as the new sheet expands
  (§5.6). The new sheet uses the same `largeTop`.
- The covered sheet's grabber hides (iOS `prefersGrabberVisible` doc [A]); its Back handler is shadowed by the newer
  registration; its drag is disabled while covered.
- Corners stay consistent: both use the same radius token.
- The HIG asks for one sheet at a time. `AppSheet(item)` follows it; stacking stays possible for flows that need it.

### 4.10 Background interaction (non-modal)

- At detents at or below `upThrough`: no dim is drawn, the scrim node has no pointer input (touches reach the screen
  beneath through Compose hit-testing), the sheet is not a `dialog()`, and nothing beneath is hidden from TalkBack.
- Between `upThrough` and the next detent above it, the dim fades in with the sheet's position. Above, the sheet is
  modal. iOS: "dimming still fades in if I resize to the large detent" (WWDC21 10063 [A]).
- `isModalNow` is a `derivedStateOf`, so crossing the boundary recomposes the scrim node once.
- `aboveBottomChrome`: at undimmed detents the bottom anchor is `LocalBottomChrome` + the floating inset, so the sheet
  rests above the floating tab bar, which stays usable; the anchor moves to the window's bottom across the expansion
  (§5.4), covering the bar at Large.

### 4.11 Haptics

| Moment | Haptic |
|---|---|
| A settle arrives on a detent other than the one the gesture started on: the first frame the spring is within 1 px of it | `AppHaptic.Threshold` (`GESTURE_THRESHOLD_ACTIVATE` on API 34+, `CONTEXT_CLICK` below; `ds/foundation/interaction/AppHaptics.kt:59-60` [C]) |
| Grabber tap, Enter or Space arrives on the next detent | `Threshold` |
| Dismissal, guard attempt, programmatic moves, TalkBack actions | none |

At most one haptic per settle; a retarget before arrival plays it once, at the final arrival.

### 4.12 Kept tabs and presentations above

- An in-screen sheet follows `LocalOverlaysVisible = LocalTabVisible && nothing is presented above its host`. While it
  is false the sheet is kept but not drawn: no hit-testing, semantics cleared, not in the `SheetStack`. It comes back
  in place with no animation (fixes N7; VitFleet's `LocalOverlaysHidden`, `current-state.md` §11).
- A destination sheet lives outside any tab and is unaffected.

### 4.13 Resize, fold and restore

- Anchors are recomputed in measure and handed to `updateAnchors(newAnchors, newTarget)`, which retargets a running
  animation instead of leaving it aimed at the old pixels (fixes D10; `AnchoredDraggable.kt:1001-1029` [G]).
- The sheet keeps its detent's *identity* across a resize. If the placement changes (an unfold takes it from Bottom to
  Center), the surface morphs from its old rect to its new one on `springs.sheet` (the zoom machinery of §5.9 with the
  old rect as source).
- Process death: §3.2. Destination sheets come back with the back stack; their state is held in the entry (§9.2).

### 4.14 Reduce motion

| Motion | Reduced |
|---|---|
| Present / dismiss | 150 ms `ModulateAlpha` fade of the sheet layer, at the detent |
| Zoom from source | the same fade |
| Card stack | no scale, no translation; the dim and the overlay only |
| Drag, settle, rubber band | unchanged (direct manipulation, as iOS keeps it) |
| Back gesture | unchanged shrink (it follows the finger) |
| Floating to attached | unchanged (it follows the sheet) |

---

## 5. Motion

### 5.1 Springs

All from `AppTheme.springs` (`AppSpring(response, bounce)`: `k = (2π / response)²`, `ζ = 1 − bounce`,
`ds/theme/AppMotion.kt:61-77` [C]).

| Use | Token | Standard / Bouncy | Calm | Snappy | Notes |
|---|---|---|---|---|---|
| Present, settle, detent change, stretch release, morph, dismiss | `sheet` | 0.34 s, 0 → k 341.5, ζ 1 | 0.3908 s → k 258.5 | 0.2957 s → k 451.6 | CP1 kept (`decisions/CP1.md:40`, `:137`); no bounce on a full-height move (`CP1.md:53-56`) |
| Action sheet arrival | `dialog` | 0.275 s, 0 → k 522 | ÷ 0.87 | ÷ 1.15 | `CP1.md:41`, M17 |
| Keyboard anchor on API 26-29 | `screen` | 0.344 s, 0 → k 333 | | | M22 |
| Reduce-motion fade | — | 150 ms linear, `ModulateAlpha` | | | `MOTION-PLAN.md:62` |

`sheet` on the Redmi: 55 % of the travel at 100 ms, 88 % at 200 ms, 99 % at 359 ms, within 1 px of a 436 dp present
at 511 ms [D].

Every animation is seeded with the velocity it inherits (release, retarget). `AnchoredDraggableState.anchoredDrag`
retargets with `lastVelocity` (androidx does the same, `src/SheetDefaults.kt:316-339` [G]), which fixes D7.

### 5.2 Release constants

| Token | Value | Source |
|---|---|---|
| `sheet.release.projectionSeconds` | 0.099 s | iOS 0.99/ms (`AppSheetState.kt:216-219` [C]; WWDC18 803 [A]) |
| `sheet.release.detentFlick` | 400 dp/s | [D] above deliberate-drag release speeds, below a flick; tune on the phone [U] |
| `sheet.release.dismissFlick` | 2.0 window heights/s | [M] `src/flutter/sheet.dart:76-78` |
| `sheet.release.dismissPosition` | 0.52 of lowest → hidden | [M] `sheet.dart:1114-1119` |

### 5.3 Rubber band

| Token | Value | Source |
|---|---|---|
| `scroll.rubberBand` (shared) | c = 0.55 | `tokens/motion.json:25` [C] |
| `sheet.detents.stretch` | 0.008 of H above Large | [M] `sheet.dart:27-37` (8 % → 7.2 %) |

### 5.4 Floating ↔ attached

Let `a = expansionFraction` (0 at the highest partial detent, 1 at Large) and `i₀ = floatingInset` (8 dp).

| Quantity | Value | Read in |
|---|---|---|
| Side inset | `i₀ · (1 − a)` | surface draw, content-layer outline |
| Bottom edge of the surface | `bottomAnchor − i₀ · (1 − a) + R · a` (R = corner radius) | surface draw, outline |
| Corners | uniform `R` on all four | outline (constant) |
| Floating bottom anchor (3-button navigation) | `windowBottom − tappableElement.bottom` | placement |
| Shadow alpha | `shadow.alpha · (1 − a)`; size constant | `dropShadow` block (§6.7) |
| Glass/frosted body → opaque | §5.8 | surface draw |

At `a = 1` the surface's bottom edge is `R` below the window, so its bottom corners are off-screen and the sheet looks
square-bottomed and edge-attached; at `a = 0` it floats `i₀` above its anchor with all four corners visible. The
change is continuous and the outline stays a *uniform* round rect throughout (§6.1). The surface node is re-recorded
only while `0 < a < 1` or while stretched (D1, a bar-to-sheet-sized rect); at rest and in ordinary drags between
partial detents it only moves (L1).

The concentric refinement (bottom radius `max(displayCorner − i₀, R)` from `WindowInsets.getRoundedCorner`, API 31+ [G]
https://developer.android.com/reference/android/view/WindowInsets#getRoundedCorner(int)) makes the outline
non-uniform, which Compose clips as a path, in hardware only from API 33 (`ui112/.../platform/OutlineResolver.android.kt:263-290`
[V]; `Outline.canClip()` [G] https://developer.android.com/reference/android/graphics/Outline#canClip()). It is
**off** in v1: with the style radii (24-30 dp) and typical display corners the difference is a few dp [U: read the
Redmi's corner radius]. Revisit with a recording.

### 5.5 Card stack

Driven by `e` = the expansion fraction of the topmost modal sheet over this presenter. No spring of its own: it rides
the sheet.

| Quantity | Value | Source |
|---|---|---|
| Scale | `1 − 0.0835 · e` → 0.9165, about the top centre | [M] `sheet.dart:84-90` |
| Translation Y | `statusBars.top · e` (33 dp on the Redmi [U]) | [D] iOS's 0.07 H equals the iPhone's status bar; see §0 #9 |
| Sheet top at Large | `statusBars.top + 10 dp` → the presenter peeks 10 dp above the sheet | `AppSheet.kt:232`, `:343` [C]; iOS ≈ 0.01 H [M] |
| Corners (uniform) | `lerp(0.9 · displayTopCorner if ≥ 20 dp else 0, 12 dp, e)`, cached per whole px | [M] `sheet.dart:14-25`, `:327-345` |
| Overlay | `0.10 · e`: black (light), `#C8C8C8` (dark) | [M] `sheet.dart:64-74`, `:345` |
| Backdrop | black, drawn only where the receded presenter uncovers the window: the top band and the two side slivers | [M] window behind is black; [D] not a full-screen fill |
| Status bar icons | light while `rootReceded` (`e > 0.5` at level 0) | [M] `sheet.dart:362-367` |
| Applies when | placement Bottom, a Large detent active, height not Compact, reduce motion off (dim still applies) | [D] |

**The dim, merged.** The presenter draws one rect inside its clip: colour `lerp(dimColour, overlayColour, e)`, alpha
`lerp(dimAlpha · presentation, 0.10, e)`. At partial detents that is the scrim; at Large it is the card overlay; never
two full-screen blends (F6). With no presenter attached at that level (a bare preview, a catalog demo without the
shell), the sheet's scrim node draws `dimColour · presentation` itself.

### 5.6 Stacked sheets

| Quantity | Value | Source |
|---|---|---|
| Covered sheet scale | 0.9165 about its top centre, with `e` of the sheet above | [M] `sheet.dart:57-62`, `:84-90` |
| Covered sheet lift | `0.005 · H · e` (4.4 dp on the Redmi) | [M] |
| New sheet's top | the same `largeTop` (not 2 % higher) | [D] §0 #10 |
| Covered sheet overlay | 10 %, as the card stack | [M] |
| Covered sheet corners | top corners keep `R` | WWDC21 10063 [A] |

### 5.7 Predictive back

| Token | Value | Source |
|---|---|---|
| `sheet.back.easing` | `(0.1, 0.1, 0, 1)` | `src/BackHandler.kt:47-51` [G] |
| `sheet.back.shrinkX` | 48 dp at p = 1 | MDC `m3_back_progress_bottom_container_max_scale_x_distance` [G] |
| `sheet.back.shrinkY` | 24 dp at p = 1 | MDC [G] |
| `sheet.back.guardedShare` | 0.2 of the shrink while a guard blocks | [D] |
| Commit | exit on `sheet`, scale held | MDC [G] |
| Cancel | the system's spring; `sheet` fallback | `predictive-back/SPEC.md:77` |

### 5.8 Glass transitions

| Tier (`LocalGlassQuality`, `glass/GlassQuality.kt:24-38` [C]) | `SheetStyle.Glass` at partial detents | Across the expansion | At Large |
|---|---|---|---|
| Live, LiveNoLens | live backdrop (blur `sheet.glass.blur`, vibrancy) under a body tint | body alpha `lerp(glassAlpha, 1, min(e / 0.5, 1))`, tint animated in draw | opaque `surface`; the backdrop pass is **skipped in draw** from `e ≥ 0.5` |
| Frosted | `glassSurface(shape, frosted colours)`: body α 0.92 light / 0.94 dark (`glass/GlassDefaults.kt:57-67` [C]) | body alpha → 1 by `e = 0.5` | opaque |
| Solid | `surface` | — | — |

- Never a layer alpha around a sampling surface; Kyant `effects` read no state; the tint animates in `onDrawSurface`
  (`handoff/WP05.md:230`; `glass/GlassChrome.kt:47-54` [C]).
- `glassAlpha` and `sheet.glass.blur` are CP2 decisions [U]. Proposal: body α 0.55 light / 0.50 dark; blur 16 dp.
  WP05's chrome blur is 10 dp (`glass/GlassDefaults.kt:28` [C]); a half-screen sheet over busy content needs more for
  its text to read.
- The card stack scales the presenter while the sheet samples it. Opaque by `e = 0.5` keeps the misalignment to a
  scale of ≥ 0.958 under a ≥ 50 % tint [U: verify alignment of `SceneBackdrop` under an ancestor scale,
  `current-state.md` §4.8].
- SheetStyle.Glass needs the glass feature. Without it (lean presets), `Glass` resolves to `Frosted` and the glass
  surface file is not generated (WP01 marker).

### 5.9 Zoom from a source

A second progress, `morph` (0 at the source, 1 at the sheet), animated on `springs.sheet`, composed with the offset:

| Quantity | Value |
|---|---|
| Surface rect | `lerp(sourceRect, sheetRect(offset), morph)` |
| Corners | `lerp(sourceCorner, R, morph)` (uniform) |
| Content layer | uniform scale `rectWidth / sheetWidth` about the top centre; alpha 0 below `morph = 0.35`, linear to 1 at 0.85, `ModulateAlpha` [U: iOS fades content in after about 40 %] |
| Dim | `· morph` |
| Source element | alpha 0 in its own layer while `morph < 1`, back at the end of the return |

- Present: `morph` 0 → 1 while the offset sits at the initial detent.
- A finger grabbing the sheet mid-morph takes the offset; `morph` keeps animating to 1. The displayed rect is a lerp of
  two continuous values, so nothing jumps.
- Dismiss by drag: the sheet slides down (morph stays 1). Dismiss by Back, scrim, close button or code while the source
  is on screen: `morph` 1 → 0 back into the source. Source gone or off-window: slide.
- Glass sources (a glass FAB): the surface keeps the glass body throughout; MOTION-PLAN M26's glass morph on `sheet`.
- iOS: "continuously interactive", velocity carried (WWDC24 10145 [A]). Drag-down on a zoomed iOS sheet may shrink
  back into the source rather than slide [U].

### 5.10 Tokens: the `sheet` section of `tokens/motion.json`

```json
"sheet": {
  "detents":  { "mediumFraction": 0.5, "largeTopGap": 10, "stretch": 0.008, "minHeight": 48 },
  "release":  { "projectionSeconds": 0.099, "detentFlick": 400, "dismissFlick": 2.0, "dismissPosition": 0.52 },
  "floating": { "inset": 8, "insetEditorial": 0, "sideInset": 16 },
  "grabber":  { "width": 36, "height": 5, "top": 5, "touch": 48, "alpha": 0.45, "contentTop": 15 },
  "dim":      { "light": 0.20, "dark": 0.48 },
  "cardStack":{ "scale": 0.9165, "corner": 12, "overlayAlpha": 0.10, "overlayDark": "#C8C8C8",
                "deviceCornerFactor": 0.9, "deviceCornerMin": 20, "statusBarFlip": 0.5 },
  "stacked":  { "scale": 0.9165, "lift": 0.005 },
  "back":     { "shrinkX": 48, "shrinkY": 24, "easing": [0.1, 0.1, 0.0, 1.0], "guardedShare": 0.2 },
  "glass":    { "opaqueAt": 0.5, "blur": 16, "bodyAlpha": { "light": 0.55, "dark": 0.50 } },
  "form":     { "heightFraction": 0.85 },
  "zoom":     { "contentFadeFrom": 0.35, "contentFadeTo": 0.85 },
  "shadow":   { "radius": 20, "y": 6, "alpha": 0.10 },
  "reduceMotionFadeMs": 150
}
```

Lengths in dp, speeds in dp/s except `dismissFlick` (window heights per second). The springs stay in `springs`
(`sheet`, `dialog`). WP01's `genkit/tokens` writes them into Android `DesignTokens.kt`, WP07's `DesignTokens.kt` and
WP08's tokens (`wt/WP03/generator/genkit/tokens/{kotlin,swift}.py`).

### 5.11 Cost per frame

| Value | Changes | Read in | Class |
|---|---|---|---|
| `offset` (+ stretch excess) | every drag frame | `SheetLayout` placement | L1 |
| `presentationFraction`, `expansionFraction` | every drag frame | presenter layer + dim draw; surface draw in the attach band; content-layer outline | L0 + D2 (+ D1 in band) |
| `backProgress` | every gesture frame | sheet-layer `graphicsLayer` | L0 |
| IME inset | every IME frame | placement | L1 |
| `morph` | present / return | sheet layer, surface draw | L0 + D1 |
| `currentDetent` | per settle | grabber `stateDescription` (`derivedStateOf`) | 1 recomposition |
| `isModalNow` | per boundary crossing | scrim node | 1 recomposition |
| `contentPadding` | per settle | list measure | 1 re-measure, 0 recompositions |

Forbidden here, as everywhere: `graphicsLayer { alpha }` on a full-screen box, `Modifier.shadow(elevation)` on the
sheet, `Modifier.offset(dp)`, `BoxWithConstraints`, `SubcomposeLayout`, allocation in draw
(`MOTION-PLAN.md:66-75`, `research.md` §3).

`Modifier.preferredFrameRate(FrameRateCategory.High)` (`ui112/.../FrameRate.kt:71` [V]) sits on the sheet layer
(SMOOTHNESS S10). It votes only while the node draws [U: confirm with a trace that an idle sheet does not hold 120 Hz].

---

## 6. Visuals

### 6.1 Shape

- **Uniform round rect, radius `R` = the style's sheet radius** (24 / 30 / 8 / 28 dp, `ds/theme/tokens/DesignTokens.kt:617`,
  `:637`, `:657`, `:677` [C]). One radius on all four corners keeps the outline a simple `RoundRect`, which Android
  clips in the RenderNode on every API (`OutlineResolver.android.kt:263-275` [V]). A non-uniform one becomes a path
  that is clipped on the canvas below API 33 (`:280-290` [V]).
- **Attached = bottom corners off-screen**, not a different shape (§5.4).
- **Circular corners, not continuous.** iOS draws continuous corners; a continuous corner is a path and costs more to
  clip (`glass/GlassDefaults.kt:17-22` [C]). The difference at 24-30 dp is about a pixel of curvature [D].
- **Form sheet:** the same radius, all four corners visible. **Side panel:** the same.

### 6.2 Width and content inset

| Placement | Sheet width | Content measured at |
|---|---|---|
| Bottom, Compact width | window width (≤ `sheetMaxWidth` 640 dp, `ds/foundation/adaptive/AppLayout.kt:65` [C]) | sheet width − 2 · `floatingInset` |
| Bottom, Compact height | `min(W, 640 dp)`, centred | sheet width (always attached) |
| Center (form) | `min(formSheetWidth, W − 2 · gutter)` (540 / 600 dp) | sheet width |
| Leading / Trailing | `supportingPaneWidth` (360 / 412 dp) | sheet width |

At Large, gutter content sits `gutter + i₀` from the window edge (24 dp on a phone). `sheetBleed()` covers the
children that must meet the edges: `AppTopBar`'s background when a screen is presented as a sheet, full-width row
highlights, hero images. The DS components that draw edge to edge apply it themselves.

### 6.3 Grabber

| Property | Value | Source |
|---|---|---|
| Size | 36 × 5 dp, fully rounded | [M] `sheet.dart:704-724` |
| Position | centred, 5 dp from the sheet's top | [M] |
| Content below it | starts 15 dp from the top | [M] |
| Colour | `contentTertiary` at α 0.45 (≈ iOS `tertiaryLabel`: #B2B6BD on white vs ≈ #B3B3B6) | `AppSheet.kt:302` [C] |
| High contrast (API 36 high-contrast text, WP05's signal) | `contentSecondary`, opaque | [D] WCAG 1.4.11 non-text contrast |
| Touch target | 48 × 48 dp centred on the pill; dragging works on the whole sheet | Material 48 dp [G] |
| Hover (mouse) | `PointerIcon.Hand` | ADAPTIVE L-5 |
| Shown | §3.3 `SheetGrabber.Automatic` | iOS [A] |

### 6.4 Header

- Pinned above the content, never over it (§0 #20).
- `AppSheetHeader`: min height 44 dp below the grabber band; title `typography.headline`, centred, one line (two at
  font scale ≥ 1.5, then the bar grows); subtitle `footnote` in `contentSecondary`.
- A three-slot `Layout` reserves `max(leadingWidth, trailingWidth)` on both sides of the title (fixes D5).
- Actions: `AppSheetCloseButton` (an `xmark` in a 36 dp circle, 48 dp target; tinted `fill` in Solid/Frosted, glass in
  Glass), text actions as `AppButton` text variants, Done in `semibold`.
- RTL mirrors the slots.
- Scroll-edge effect: when `isContentScrolled`, the header fades in a 24 dp gradient of the surface colour over the
  content's top (`ds/foundation/scroll/ScrollEdge.kt`, M27; alpha on `fadeQuick`, read in draw). No hairline.

### 6.5 Footer

- Placed at `max(visibleBottom − footerHeight, sheetTop + headerHeight)`, so on a short detent it never rides over the
  header (`AppSheet.kt:249-250` [C] places it the same way).
- Draws on the sheet's own surface; a 24 dp scroll-edge gradient above it while content is under it.
- Padding below it: the bottom safe inset when the sheet is attached and the keyboard is down; 0 on the keyboard.
- Its settled height becomes `LocalBottomChrome` inside the sheet (written after layout settles, never per frame).

### 6.6 Surface styles

| Style | Partial detents | Large | Rim | Cost |
|---|---|---|---|---|
| Solid | `surface` (opaque) | same | dark: white α 0.08, 1 px; light: none | one rect |
| Frosted | `glassSurface`, frosted colours | opaque | `GlassDefaults` rim | a few cached brushes (`glass/GlassSurface.kt:26-38` [C]) |
| Glass | live backdrop + tint (Live tiers), else Frosted | opaque, no backdrop | `GlassDefaults` rim | one blur pass over the sheet's area at partial detents only |

### 6.7 Elevation without alpha-over-shadow artifacts

The pull-to-refresh black ring (REQUIREMENTS 29) came from a shadow showing through a fading surface. The sheet rules:

1. **The surface never fades.** It moves, scales and morphs. Only the *content* layer fades (zoom, cross-fade).
2. **The shadow is cut out of the shape's inside**, in every style, as WP05's `glassSurface` does
   (`glass/GlassSurface.kt:33-35` [C]): `clipPath(outline, ClipOp.Difference) { draw the DropShadowPainter }`
   (`DropShadowPainter` is public, `ui-graphics 1.12.1 shadow/DropShadowPainter.kt:42-66` [V]). A reduce-motion
   `ModulateAlpha` fade then modulates the body and the shadow together and nothing shows through.
3. **No `Modifier.shadow(elevation)` / `shadowElevation`**: an elevation shadow is regenerated as the layer scales and
   shows through a translucent body (`predictive-back/SPEC.md:412`).
4. **The shadow's size never changes per frame.** The cached blur is per size (`DropShadowPainter.kt:142` [V]).
   It is sized for the floating card and *fades* across the attach band through the block form
   `dropShadow(shape) { alpha = … }`, which animates "without recomposition" (`ui112/.../draw/Shadow.kt:141-155` [V]).
5. **No shadow in dark theme**; the rim separates instead.

Shadow: radius 20 dp, y 6 dp, black α 0.10, light theme only [D: between WP05's chrome (16/4/0.10) and content
(10/3/0.08) shadows, `glass/GlassDefaults.kt:84-85` [C]]. Form sheets and side panels use the same.

### 6.8 Scrim

- Drawn as one rect inside the presenter (§5.5); the sheet's scrim node only hit-tests and carries semantics.
- Covers the whole window including the status bar and the tab bar (QA-13).
- Dim strength: §6.9 [U].

### 6.9 Light and dark

| Element | Light (Cool palette) | Dark (Cool palette) |
|---|---|---|
| Surface | `surfaceElevated` #FFFFFF | `surfaceElevated` #1D232E (`DesignTokens.kt:237`, `:280` [C]) |
| Frosted body | `surfaceElevated` α 0.92 | α 0.94 |
| Glass body (partial) | α 0.55 [U, CP2] | α 0.50 [U, CP2] |
| Rim | none (Solid); white α 0.85 (glass) | white α 0.08 (Solid); α 0.20 (glass) |
| Shadow | black α 0.10, r 20, y 6 | none |
| Grabber | `contentTertiary` α 0.45 | `contentTertiary` α 0.45 |
| Dim | black α 0.20 | black α 0.48 |
| Card overlay | black α 0.10 | #C8C8C8 α 0.10 |
| Behind the receded presenter | black | black |
| Status bar icons over the card stack | light past `e = 0.5` | light |

Every palette and style reads the same roles, so Warm, Paper and Cream follow without extra code.

---

## 7. Adaptive behaviour

`SheetPlacement.Automatic` resolves from `AppTheme.containerClass` and `AppTheme.posture` (`AppSheet.kt:133-142` [C];
`ds/foundation/adaptive/SizeClass.kt:8-29` [C]):

| Window | Placement | Detents | Look | Card stack |
|---|---|---|---|---|
| Compact width, Medium/Expanded height (phone portrait) | Bottom | as given | floating while partial, attached at Large | at Large |
| Compact height (phone landscape, short split screen) | Bottom, ≤ 640 dp, centred | `Medium` inactive; others if they fit; Large top = safe top + 8 dp | attached | none; dim only |
| Medium width × Medium+ height (unfolded foldable, tablet portrait, a resized window) | Center (form sheet) | ignored; one pose, height `min(0.85 H, fit)` | floating card, four corners; slides up; swipe down closes (§0 #17); close button shown, grabber hidden | none |
| Expanded+ width | Center; Leading / Trailing on request | panel: as given, heights from the window | floating panel, inset 16 dp from its edge and the bottom | none |
| Tabletop | Bottom, never a card across the crease | `Medium` rests on the fold | as phone | at Large |
| Book, separating hinge | Center inside one half (the source's half, else the trailing one); Bottom sheets one half wide | as given | | none |
| Any size-class change while shown | re-resolved | detent identity kept | morphs old rect → new on `sheet` (§4.13) | follows |

Input on large screens: Tab reaches the grabber, Space/Enter cycles detents (Material accessibility [G]), Esc
requests dismissal, focus is trapped in a modal sheet and returns to where it was on close (§8). A mouse drags the
sheet by any non-scrolling part; the wheel scrolls content and never resizes the sheet. No orientation or
resizability locks (L-1).

---

## 8. Accessibility

| Node | Semantics |
|---|---|
| Sheet root | `isTraversalGroup = true`; `paneTitle` = the header's title, else "Sheet" (`designsystem_sheet_pane`), always set (fixes D12); `dialog()` while modal (`ui112/.../SemanticsProperties.kt:1149` [V]); `traversalIndex = 0f` |
| Grabber | `Role.Button`, label "Sheet grabber"; `stateDescription` "Expanded" (Large), "Collapsed" (lowest of several), "Partially expanded" (others); click cycles; custom actions *Expand*, *Collapse*, *Close sheet* (only those that apply) (`src/BottomSheet.kt:365-397` [G]; MDC state prefix, `research.md` §2.3) |
| Hidden grabber (form sheet, `Hidden`) | the actions move to the sheet root |
| Scrim (modal, dismissible) | `onClick(label = "Close sheet")`, `contentDescription` "Close sheet", `traversalIndex = 1f` (`src/Scrim.kt:58-71` [G]) |
| Content beneath a modal sheet | `hideFromAccessibility()` at both hosts (`ui112/.../SemanticsProperties.kt:1075` [V]; replaces `clearAndSetSemantics` at `AppOverlayHost.kt:58` and fixes N8 in the presentation host) |
| Detent changes | announced through the grabber's `stateDescription` and the pane title; no `announceForAccessibility` (deprecated in API 36, `research.md` §2.3 [G]) |

- **Keyboard focus:** on present, focus moves into the sheet (first focusable after the header); a modal sheet traps it
  with `focusProperties { onExit = { cancelFocusChange() } }` (`research.md` §2.3 [G]); on close it returns to the
  element focused before [U: the restore API to use].
- **Touch targets:** 48 dp for the grabber, close button and header actions (`minimumTouchTarget()`).
- **Font scale 1.5 and 2.0:** the header grows (no fixed 44 dp row), the title wraps to two lines, `FitContent`
  re-resolves.
- **TalkBack and haptics:** no haptic for accessibility actions.
- **Reduce motion:** §4.14. **Reduce transparency:** Glass and Frosted fall to Solid through `GlassQuality.Solid`.

---

## 9. Integration

### 9.1 Hosts

- **In-screen** (`AppSheet(visible)`, `AppSheet(item)`): `HostedOverlay(modal)` in the overlay host
  (`AppOverlayHost.kt:81-100` [C]), keeping the caller's composition locals; follows `LocalOverlaysVisible` (§4.12).
- **Destination** (`Presentation.Sheet`): the presentation host's `NavDisplay` through `SheetSceneStrategy` (§9.2).
- **Both** register with `LocalSheetStack` at their level and use the same `SheetLayout`.

### 9.2 Navigation 3

- **One metadata shape.** `Presentation.Sheet(val options: SheetOptions = SheetOptions())` replaces
  `Presentation.Sheet(detents: List<SheetDetent>)` and its private enum (`nav4/Presentation.kt:8-24` [C]). The
  metadata travels under one typed `NavMetadataKey<Presentation>` instead of the string key and `as?`
  (`PresentationHost.kt:198-205` [C]). The same change in WP07's copy. Fixes N3.
- **Stateless strategy.** `SheetSceneStrategy` keeps no per-sheet map: `calculateScene` returns
  `SheetScene(key = top.contentKey, entry = top, previousEntries = overlaidEntries = entries.dropLast(1), options)`, a
  data class keyed by the content key, so being called once per overlay level on every calculation
  (`current-state.md` §4.2 [V]) yields an equal scene and the same movable group. Fixes N1. It is first in the host's
  strategies (`bottomsheet.md:157`).
- **State in the entry.** A `rememberSheetEntryDecorator()` wraps each sheet entry's content: it creates
  `rememberAppSheetState(options.detents, options.initialDetent)` inside the entry, so the entry's saveable-state
  decorator restores it (`PresentationHost.kt:92-94` [C]); it publishes the state in a small registry keyed by
  content key (`DisposableEffect`), and renders `AppSheet(state, onDismissRequest = onBack, …options) { content() }`.
- **`onRemove()`** looks the state up and awaits `state.dismiss()`; with no state yet (removed before its first
  frame), it returns at once. Popping from anywhere therefore slides the sheet away first
  (`src/OverlayScene.kt:35-57` [G]).
- **`SheetEntryFrame`** replaces `PresentedFrame` for sheets (N4): no opaque background, no cover card, the sheet's
  own `LocalBottomChrome`, `LocalTabBarVisible = false`, and the sheet's own motion reported to the presentation
  host's `BackGate` (moving while presenting or dismissing, from a `snapshotFlow` of a derived boolean, never per
  frame). `PresentedFrame` stays for covers. `LocalNavAnimatedContentScope` is never used for the sheet's motion: in an
  overlay scene it is the underlying scene's (`current-state.md` §4.2 [V]).
- **Wiring.** `AppRoot` passes `rememberSheetSceneStrategy()` to `AppShell(sheetStrategy = …)` (`nav4/AppShell.kt:96`
  [C]); the template ships a sample sheet destination (WP02-M3) so `perf.sh` and the restore scenario have one to
  open (N2).
- **Gate.** While a sheet moves, `NavGuard` holds pushes and `BackGate` counts presses (§4.8).
- **A cover over a sheet, a sheet over a cover:** each presented entry's frame applies `cardStackPresenter(level)`.

### 9.3 Navigation inside a sheet

`AppSheetNavHost(startKey, entryProvider)` in `core/navigation/presentation/sheet/`: a nested stack with WP04's
`StackHost` push and pop (slide with parallax, clipped to the sheet), its own `BackGate`, and a Back handler at a
higher priority than the sheet's while its depth is above one. `FitContent` sheets animate their height between steps
through the re-resolving detent. A push from inside a sheet to a key registered as a presentation still goes to the
shell's presented stack. Fixes N5 and MASTER-PLAN A3.7.

### 9.4 Chrome locals inside a sheet

| Local | Value inside the sheet |
|---|---|
| `LocalBottomChrome` | footer + bottom safe inset, settled values only |
| `LocalTopChrome` | the header's settled height |
| `LocalTabBarVisible` | false |
| `LocalInSheet` (new, DS-internal) | true: `AppTopBar` shows a close button instead of Back; `AppScaffold` paints no background of its own (PeriFlow's `LocalSheetSurface`, `current-state.md` §11) |
| Status bar inset | consumed by the sheet, so an `AppTopBar` inside does not pad it again (QA-19) |

`BottomChromeWriter` gives the keyboard precedence over a presented bar (N9, WP04). The `Chrome.kt` merge (package
`foundation.layout`, `TabVisibility` holder) is reconciled at the WP03/WP04 merge (N11).

### 9.5 Glass (what WP05 provides)

1. A sampling surface usable outside `GlassStage`'s chrome slot: the scene exposed to the overlay host and the
   presentation host, which are both outside the recorded subtree (`glass/GlassStage.kt:17-58` [C]).
2. A backdrop pass that is skipped in draw when its opacity reaches 1, with no recomposition.
3. `GlassDefaults.sheetColors()` and the sheet blur, decided in CP2.
4. A GPU budget row for a sampling sheet, and `watchGpuBudget` covering it (`glass/GlassQuality.kt:96-108` [C]).
5. `design/wp/WP05.md:64` revised: sheets may sample at partial detents.
6. A check that sampling stays aligned while the presenter recedes (§5.8).

### 9.6 Card stack in the shell (WP04)

`ShellChrome`'s root layer (content + bar) gets `Modifier.cardStackPresenter(stack, level = 0)`
(`nav4/AppShell.kt:131-199` [C]); the edge-to-edge owner flips the status bar while `stack.rootReceded`; the glass stage
lies inside the receding layer. No recomposition of the shell's content (RECOMPOSITION-PLAN `:193`).

---

## 10. Package and file layout

Each file stays under 400 lines (K-2); names say what they hold; no `util`/`internal`/`common` packages (F-3).

```
core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/sheet/
├── AppSheet.kt               the three AppSheet overloads and their KDoc
├── AppSheetDefaults.kt       AppSheetDefaults, AppSheetColors
├── AppSheetState.kt          AppSheetState, its factory, rememberAppSheetState, the Saver
├── SheetDetent.kt            SheetDetent, SheetDetentScope
├── SheetOptions.kt           SheetOptions and the option types (style, modality, interaction, placement, grabber, transition)
├── SheetScopes.kt            SheetContentScope, SheetHeaderScope
├── SheetDismissGuard.kt      SheetDismissGuard and the sheet's guard registry
├── chrome/
│   ├── AppSheetHeader.kt     the three-slot header
│   ├── AppSheetCloseButton.kt
│   └── SheetGrabber.kt       pill, semantics, tap to cycle
├── content/
│   ├── SheetColumn.kt
│   └── SheetBleed.kt         Modifier.sheetBleed (a LayoutModifierNode)
├── gesture/
│   ├── SheetFlingBehavior.kt the release rule of §4.3 as a FlingBehavior, haptic on arrival
│   ├── SheetStretch.kt       the rubber-band OverscrollEffect
│   ├── SheetNestedScroll.kt  Resizes / Scrolls connections
│   └── SheetBack.kt          predictive back, Esc, the guard's branch
├── layout/
│   ├── SheetAnchors.kt       resolved anchors (pure Kotlin)
│   ├── DetentResolution.kt   detents → pixels for a window (pure Kotlin)
│   ├── SheetGeometry.kt      inset, attach, outline, stretch (pure Kotlin)
│   ├── SheetPlacementPolicy.kt Automatic → Bottom / Center / Leading / Trailing
│   └── SheetLayout.kt        the one measure-once Layout
├── surface/
│   ├── SheetSurface.kt       Solid / Frosted surface node, cut-out shadow
│   ├── GlassSheetSurface.kt  Glass (generated only with the glass feature)
│   └── SheetScrim.kt         hit-testing, semantics, fallback dim
├── cardstack/
│   ├── SheetStack.kt         window registry, levels, recede and dim
│   └── CardStackPresenter.kt Modifier.cardStackPresenter (one layer node + one draw)
├── source/
│   └── SheetSource.kt        rememberSheetSource, Modifier.sheetSource, morph geometry
└── preset/
    ├── AppActionSheet.kt     moved from component/overlay
    └── AppChoiceSheet.kt

core/designsystem/src/test/.../component/sheet/   mirrors the packages above (§13)

core/navigation/src/main/kotlin/com/base/app/core/navigation/
├── Presentation.kt                         Presentation.Sheet(options), the typed metadata key
└── presentation/
    ├── PresentationHost.kt                 (existing) strategies, gates
    ├── CoverFrame.kt                       PresentedFrame, renamed: covers only
    └── sheet/
        ├── SheetSceneStrategy.kt           rememberSheetSceneStrategy, SheetScene (data class, onRemove)
        ├── SheetEntryDecorator.kt          per-entry state, registry
        ├── SheetEntryFrame.kt              chrome locals, gate reporting
        └── AppSheetNavHost.kt              a stack inside a sheet
```

`component/overlay/` keeps `AppAlert`, `AppMenu`, `AppContextMenu`, `AppToast`, `AppOverlayHost`,
`OverlayBackHandler`. WP03's `core/navigation/SheetScene.kt` is deleted (one owner: WP04).

---

## 11. Migration

| From | To | Notes |
|---|---|---|
| Legacy `AppBottomSheet` (a `Dialog`, integration branch `8672003`, copies in WP01-WP10) | `AppSheet(visible, onDismissRequest, detents = listOf(SheetDetent.FitContent))` | Shipped on `main`, so one release of `@Deprecated(level = WARNING, replaceWith = …)` delegating to `AppSheet` (API-14). `showHandle` → `grabber`; `dismissOnClickOutside = false` → `SheetDismissGuard` or `dismissible = false` |
| Legacy `AppActionSheet` | `preset/AppActionSheet` | same name, new package; deprecated alias for one release |
| WP03 `AppSheet(state, onDismiss, …, title, showCloseButton, footer)` | `AppSheet(state, onDismissRequest, …, header = { AppSheetHeader(title) }, footer)` | Unmerged: no deprecation |
| WP03 `enum SheetDetent { Medium, Large }` | `sealed interface SheetDetent` | `SheetDetent.Medium` / `.Large` references compile unchanged; `Set<SheetDetent>` → `List<SheetDetent>` |
| `AppSheetState.largeFraction` | `expansionFraction`; the shell reads `SheetStack.recede(0)` | |
| `AppSheetState(detents)` + `remember { }` | `rememberAppSheetState(detents)` | saveable (fixes D15) |
| `AppSheetState.dismissing` | `isDismissing` | K-4 predicate naming |
| WP04 `Presentation.Sheet(detents: List<core.navigation.SheetDetent>)` | `Presentation.Sheet(SheetOptions(detents = …))` | enum deleted; same in WP07 |
| WP04 string metadata key | typed `NavMetadataKey<Presentation>` | |
| WP03 `nav3/SheetScene.kt` (`SheetPresentation`, `SheetKey`) | WP04 `presentation/sheet/` | deleted |
| `PresentedFrame` for sheets | `SheetEntryFrame` | covers keep their frame |
| `HostedOverlay` modal `clearAndSetSemantics` | `hideFromAccessibility()` | |
| Settings language picker (`AppActionSheet` with every locale) | `AppChoiceSheet` with a checkmark and search | fixes A1, A2, QA-14 |
| WP08 `appSheet(isPresented:detents:)` | `appSheet(isPresented:options:)` (§16) | the old signature stays as a convenience |

Defects closed by this design: D1 (§2.2), D2-D4 (§4.7), D5 (§6.4), D6 (§4.8), D7 (§5.1), D8 (§4.2), D9 (§2.2),
D10 (§4.13), D11 (§0 #17, §7), D12 (§8), D13 (§0 #19, `SheetColumn`), D14 (springs read from `AppTheme` by the
state's factory, not written in composition), D15 (§3.2), D16 (§4.8); A1-A5 (§3.8, §11); N1-N10 (§9).

---

## 12. Catalog demos

The catalog's Sheets page (WP03, `CatalogOverlays.kt` replaced by `catalog/sections/CatalogSheets.kt`) shows each
variant over a colourful, scrollable backdrop so glass and dimming read. Each demo has a fixed test tag for
`record.sh`.

| # | Demo | Shows |
|---|---|---|
| 1 | Filters | Medium + Large, a 40-row list (the scroll hand-off), a pinned Apply footer |
| 2 | Compose message | Large only, card stack, `SheetDismissGuard` with a Discard / Keep Editing action sheet |
| 3 | Summary | `FitContent`, floating, short content |
| 4 | Map panel | `Height(120.dp)` + Medium + Large, `NonModal(upThrough = Medium)`, `dismissible = false`, Back collapses; the backdrop stays scrollable |
| 5 | Detent lab | `Fraction(0.25/0.5/0.75)` + Large, toggles for `Resizes`/`Scrolls`, grabber, floating inset; the current detent printed by a draw-phase label |
| 6 | Styles | Solid / Frosted / Glass side by side, with a glass-tier switcher (Live, Frosted, Solid) |
| 7 | Headers | title only; Cancel + Done; close only; subtitle; a long title at font 2× |
| 8 | Checkout | footer with Pay, a text field: the sheet rides the keyboard |
| 9 | Zoom | a glass FAB and a card that morph into a sheet and back |
| 10 | Stack | sheet → sheet → sheet, the deck behind |
| 11 | Choice | `AppChoiceSheet` single and multiple, with search |
| 12 | Actions | `AppActionSheet` at the bottom, and anchored to a source |
| 13 | State | loading skeleton → content (FitContent animates) → error with retry |
| 14 | Destination | opens a registered sheet key; deep link `app://catalog/sheet` |
| 15 | Steps | `AppSheetNavHost`: a three-step flow inside one sheet |
| 16 | Adaptive | Automatic placement; resize the window or fold: Bottom ↔ Center morph; Leading panel at Expanded |
| 17 | Reduce motion | the cross-fade present, the card stack without scale |
| 18 | Accessibility | grabber actions and state description, scrim label, focus trap with a keyboard |

---

## 13. Tests

### 13.1 Unit (JVM, pure Kotlin, `core/designsystem/src/test/.../component/sheet/layout|gesture`)

- `DetentResolutionTest`: each detent type; `Medium` inactive in Compact height and within 48 dp of Large; `Custom`
  returning null; duplicates collapse; inactive initial detent → nearest; tabletop fold.
- `SheetReleaseTest`: every row of the §4.3 table; flick one stop; 2 H/s and 52 % rules; guard blocks and reports an
  attempt once; non-dismissible.
- `SheetStretchTest`: monotonic, below the asymptote, 0.008 H at Large, bottom band when blocked.
- `SheetGeometryTest`: inset and bottom edge at a = 0, 0.5, 1; the outline is always a simple round rect; content
  width per placement.
- `CardStackMathTest`: scale, translation, corner and dim at e = 0, 0.5, 1; merged dim colour; stacked lift.

### 13.2 Compose UI (Robolectric, `core/designsystem/src/test`, clock driven, T-6)

| Test | Asserts |
|---|---|
| `SheetPresentTest` | first frame draws the hidden pose; present lands on the initial detent; restored state snaps without animation; `visible` false → true during the exit reverses (D1) |
| `SheetGestureTest` | drag follows 1:1; a touch during a settle catches the sheet (D8); a retarget keeps velocity: per-frame offset deltas never jump (D7); `animateTo` during a dismiss is ignored (D9); a resize mid-settle lands on the detent (D10); haptic once, on arrival |
| `SheetNestedScrollTest` | Resizes grows first, then scrolls; Scrolls scrolls first; momentum never moves the sheet; the last row is reachable at Medium |
| `SheetDismissTest` | flick, scrim, Back key, Esc, close button each call `onDismissRequest` once; programmatic paths do not |
| `SheetGuardTest` | each person path with the guard: the sheet stays, `onAttempt` once per attempt; guard disabled: no handler registered; `dismissible = false`: Back passes through at the lowest detent and collapses above it |
| `SheetBackTest` (`DirectNavigationEventInput`) | eased progress → layer `scaleX/Y` values; content counter-scale; cancel returns to 1; commit dismisses from Medium and from Large; in-screen Back burst during exit is dropped |
| `SheetImeTest` (insets injected on the compose view) | footer above the IME; Medium lifted; content padding counts the IME once (D2, D3, D4) |
| `SheetModalityTest` | non-modal: a click beneath reaches the screen at Peek, is blocked above `upThrough`; dim draws only above |
| `SheetA11yTest` | pane title, traversal group, `dialog()`, grabber role, state and actions, scrim label and order, beneath hidden, focus trapped and restored |
| `SheetAdaptiveTest` | 1280 × 800 → Center; 900 × 400 → Bottom 640 wide, one detent; tabletop; book; Leading at Expanded; Bottom → Center morph keeps the detent |
| `SheetStackTest` | covered sheet scale and lift from the top sheet's expansion; grabber hidden; only the top sheet takes Back; `SheetStack.recede` per level |
| `SheetSourceTest` | morph start rect = source bounds; drag mid-morph does not jump; return into a visible source; slide when it is gone |
| `KeptTabSheetTest` | a tab switch hides an in-screen sheet and its scrim; returning restores it without animation (N7) |
| `AppChoiceSheetTest`, `AppActionSheetTest` | selection role and checkmark; scroll past 60 %; Back during exit consumed (A3); anchored with a source |

### 13.3 Navigation (WP04, `core/navigation/src/test`, a real `NavDisplay`)

`SheetSceneTest`: a lower sheet keeps its state and never re-presents across stack changes (N1); a pop slides out
before the entry leaves; `onRemove` before the first frame returns; metadata through the typed key; a cover over a
sheet and a sheet over a cover; restore after process death (detent kept). `SheetGateTest`: Back burst during a
destination sheet's dismiss is counted and coalesced; a push during the present is held. `AppSheetNavHostTest`: inner
Back pops the inner stack first.

### 13.4 Recomposition budgets (`RecompositionJourneysTest`, RECOMPOSITION-PLAN §3)

| Journey | Budget |
|---|---|
| 30 drag moves across detents | 0 in the sheet content, header, footer, surface and presenter |
| A settle on a new detent | ≤ 1, only in readers of `currentDetent` (the grabber) |
| Medium → Large with the card stack | 0 in the shell's content |
| 30 back-gesture progress events | 0 |
| IME animation | 0 in content |
| Zoom present | 0 in frames 2…N−1 |
| Crossing a non-modal boundary | ≤ 1 (scrim node) |

### 13.5 Screenshots (T-5, `com.android.compose.screenshot`)

Light and dark, font scale 1.0 and 1.5, LTR and RTL for each of:

| Size (dp) | Cases |
|---|---|
| 400 × 1000 | Medium floating (Solid, Frosted); Large attached with the card stack (a fake presenter at e = 1 and 0.5); FitContent; header variants; footer; stacked sheets; guard attempt with an action sheet over it |
| 400 × 500 | Medium + Large (Medium active at 500 dp) |
| 400 × 400 | Compact height: one detent |
| 610 × 1000, 900 × 1000 | Center form sheet |
| 900 × 400 | Compact height, 640 dp centred |
| 1280 × 800 | form sheet; Leading panel |
| tabletop and book (injected posture) | fold detent; Center in one half |

Glass Live cannot render in a screenshot test; it is verified on the phone (§14).

---

## 14. Phone gates and recordings

### 14.1 `perf.sh` sheet journeys (WP04 adds them to `tools/perf/perf.py` and `gates.toml`)

The sample destination (WP02-M3) exposes tags `sheet_open`, `sheet_grabber`, `sheet_list`, `sheet_field`,
`sheet_footer` (`sheet_grabber` is already in the resource-id contract, `handoff/WP04.md:248`). Release builds with
their baseline profile, as every row (`gates.toml:1-15` [C]).

| Row | solid | standard | lean | Notes |
|---|---|---|---|---|
| `sheet_present.janky_pct` | 1.0 | 1.0 | 1.0 | as `tabs` (`gates.toml:47-53` [C]); smoke add 0.5 |
| `sheet_present.compose_frame_ms.median` / `.max` | 8.0 / 16.0 | 8.0 / 16.0 | 8.0 / 16.0 | ratchet until S2 lands; then these |
| `sheet_drag.janky_pct` | 1.0 | 1.0 | 1.0 | grabber drags Medium ↔ Large, slow and fast, 10 each |
| `sheet_drag.gpu_p90_ms` | 7.0 | 7.0 | 7.0 | glass column: 8.0 |
| `sheet_drag.p99_ms` | 16.0 | 16.0 | 16.0 | a drag is L1 only |
| `sheet_dismiss.janky_pct`, `.dismissed` | 1.0, true | 1.0, true | 1.0, true | flick at 3 H/s from Medium |
| `sheet_back.gesture_first_frame_ms.median` | 8.0 | 8.0 | 8.0 | as `pushpop_gesture` (`gates.toml:139-142` [C]) |
| `sheet_back.janky_pct` | 2.0 | 2.0 | 2.0 | commit and cancel |
| `sheet_ime.janky_pct`, `.footer_above_ime` | 2.0, true | 2.0, true | 2.0, true | bounds from UI Automator |
| `sheet_scroll.janky_pct`, `.gpu_p90_ms` | 0.5, 7.0 | 0.5, 7.0 | 0.5, 7.0 | list at Large, as `scroll_root` |
| `sheet_cardstack.gpu_p90_ms` | 8.0 | 8.0 | 8.0 | Medium → Large with the shell receding |
| `sheet_cardstack.no_offscreen` | true | | | trace: no `saveLayer` larger than a bar in the expansion frames (`research.md` §6); the `trace` scenario runs on solid only |
| `sheet_rapid_back.alive`, `.lands_on_presenter` | true | true | true | three Back keys within 150 ms of opening |
| `sheet_restore.detent_restored` | true | | | `am kill` at Large, relaunch; beside `restore.restored` (`gates.toml:204-207` [C]) |
| `recompose.sheet_drag.max` | 0 | | | frames that recompose while dragged |
| `recompose.sheet_settle.max` | 1 | | | |
| `recompose.sheet_back_progress.max` | 0 | | | |
| `meminfo.sheet_cycle_pss_growth_mb` | ratchet | | | 20 open/close cycles |
| Glass column (`everything-glass`): `sheet_drag_glass.janky_pct` / `.gpu_p90_ms` / `.no_demotion` | 1.0 / 8.0 / true | | | WP05 owns the column |

### 14.2 Recordings (QA2, `record.sh`, light and dark, into `perf/sheet/`)

Present to Medium; Medium → Large by drag and by flick; flick dismiss from Medium; one-stop flick from Large; rubber
band at Large and on a Medium-only sheet; non-dismissible pull; guard attempt; predictive back commit and cancel from
Medium and Large; Back key; IME rise and fall; card stack; three stacked sheets; zoom present and return; glass tiers;
font 2×; RTL; landscape (compact height); tablet form sheet and Leading panel (emulator, relative evidence only);
tabletop (emulator).

### 14.3 Feel acceptance from the strips

| Check | Pass |
|---|---|
| First moving frame of a present | ≤ 4 % of the travel (CP1 §1 condition 1) |
| A drag | the sheet's top under the finger within 1 frame of lag, on every frame |
| A release | no frame in which the sheet moves against its release velocity, except a settle that reverses past a stop |
| Over-pull at Large | the top never goes above `largeTop − 0.008 H` |
| Expansion | content text does not move sideways on any frame |
| Card stack | the presenter peeks 10 dp above the sheet at Large; black band light status icons |
| Shadows | no dark band inside any surface, in light or dark |
| Haptic | logcat marker within ±1 frame of the arrival frame |
| Predictive back | text never squashes; the sheet never moves up during the gesture |

---

## 15. Compose Multiplatform parity (WP07-B)

- **Same API and package** in `core/designsystem/src/commonMain/.../component/sheet/` (WP07 has no overlay component
  today, `current-state.md` §6). Pure geometry and release files are shared verbatim; the tests in §13.1 run in
  `commonTest` (camelCase names, K-8).
- **Tokens:** WP07 expresses springs as stiffness and damping (`wt/WP07/.../theme/tokens/DesignTokens.kt:101`,
  `:119-146` [C]); its generated tokens move to `Spring(response, bounce)` with the same `sheet` section so values
  cannot drift.
- **`expect`/`actual`:**
  - haptics: Android `View.performHapticFeedback`; iOS `UISelectionFeedbackGenerator` / `UIImpactFeedbackGenerator(.light)`
    for `Threshold`;
  - display corner radius: Android `getRoundedCorner`; iOS has no public API, so the style radius is used;
  - status-bar appearance over the card stack: Android `WindowInsetsControllerCompat`; iOS the hosting controller's
    `preferredStatusBarStyle` [U: the CMP hook];
  - frame rate: Android `preferredFrameRate`; iOS needs `CADisableMinimumFrameDurationOnPhone = YES` in `Info.plist`
    for 120 Hz on iPhone ([A, not fetched]
    https://developer.apple.com/documentation/quartzcore/optimizing-promotion-refresh-rates-for-iphone-13-pro-and-ipad-pro).
- **Back:** Android target as §4.8 through `navigationevent` (WP07-A rebuilt the 1.2.0 pieces it needs,
  `handoff/WP07-A.md:39-42`). iOS target: no system Back; drag, scrim, close button and Esc (hardware keyboard).
- **IME on iOS:** the sheet rides `WindowInsets.ime` [U: CMP iOS inset animation fidelity].
- **Glass:** Kyant backdrop is Android-only; on iOS `Glass` resolves to `Frosted` (drawn). A `UIVisualEffectView`
  interop is a later option [D].
- **Navigation:** JetBrains `navigation3-ui` 1.1.2 = androidx 1.1.7 (`libs.versions.toml:15-20` [C]); `OverlayScene.onRemove`
  exists there [V]. The typed `NavMetadataKey` is [U] in 1.1.x: keep a string key behind the same `Presentation`
  accessor until the library catches up.
- **Card stack:** the same `cardStackPresenter` in the common shell.
- **Gates:** Android target on the Redmi as §14; iOS target relative only (no device).

## 16. SwiftUI parity (WP08, iOS 17+)

The native sheet already carries the motion and the card stack; the template maps the same options onto it and does
not fight it (`MOTION-PLAN.md:526-556`).

```swift
public struct AppSheetOptions {
    public var detents: [AppSheetDetent] = [.large]            // .large, .medium, .fitContent, .fraction, .height, .custom
    public var style: AppSheetStyle = .automatic              // .solid, .frosted, .glass
    public var modality: AppSheetModality = .modal            // .nonModal(upThrough:)
    public var dismissible = true
    public var contentInteraction: AppSheetContentInteraction = .resizes
    public var placement: AppSheetPlacement = .automatic
    public var grabber: Visibility = .automatic
}
extension View {
    public func appSheet<Content: View>(isPresented: Binding<Bool>, options: AppSheetOptions = .init(),
                                        selection: Binding<AppSheetDetent>? = nil,
                                        @ViewBuilder content: @escaping () -> Content) -> some View
    public func appSheet<Item: Identifiable, Content: View>(item: Binding<Item?>, options: AppSheetOptions = .init(),
                                        @ViewBuilder content: @escaping (Item) -> Content) -> some View
    /// The guard: keeps the sheet up while `enabled` and calls `onAttempt` on a swipe down.
    public func appSheetDismissGuard(enabled: Bool, onAttempt: @escaping () -> Void) -> some View
}
```

| Option | iOS 17 | Newer, behind `#available` |
|---|---|---|
| Detents | `presentationDetents(_:selection:)`; `.fraction`, `.height`, `CustomPresentationDetent`; `fitContent` = `.height` of the measured content (`onGeometryChange` [U: availability on 17]) | — |
| Modality | `presentationBackgroundInteraction(.enabled(upThrough:))` | — |
| Content interaction | `presentationContentInteraction(.resizes / .scrolls)` | — |
| Dismissible | `interactiveDismissDisabled(!dismissible)` | — |
| Guard | `interactiveDismissDisabled(enabled)` + a UIKit bridge setting the presentation controller's delegate for `presentationControllerDidAttemptToDismiss` | iOS 27 `dismissalConfirmationDialog` [A] |
| Style | Solid: `presentationBackground(surfaceElevated)`; Frosted: `.regularMaterial`; Glass: `.ultraThinMaterial` | iOS 26: no `presentationBackground` for Glass, the system glass ("remove any custom backgrounds", WWDC25 284 [A]) |
| Corner | `presentationCornerRadius(theme.shapes.radiusSheet)` | iOS 26: system concentric corners (`AppSheet.swift:37-41` [C], kept) |
| Grabber | `presentationDragIndicator` | — |
| Placement | iPad form via `.presentationSizing(.form)` | 18; `presentationPlacement(.leading/.trailing)` on 27 |
| Zoom | — | 18: `navigationTransition(.zoom(sourceID:in:))` + `matchedTransitionSource` |
| Cross-fade | — | 27: `navigationTransition(.crossFade)` |
| Header | `NavigationStack` + `.toolbar` (Cancel / Done; iOS 26 makes them glass) | — |
| Footer | `.safeAreaInset(edge: .bottom)` | iOS 26 `safeAreaBar` [U] |
| Haptics | none: iOS plays none at detents, and the template follows the platform | — |
| Springs | `withAnimation(theme.springs.sheet)` for programmatic detent changes: `.spring(response: 0.34, dampingFraction: 1)` | — |

iOS 27 symbols need the iOS 27 SDK to compile even behind `#available`; gate them with a `#if compiler(>=…)` check for
the Swift version that SDK ships [U]. `SampleDetailView`'s raw `.presentationDetents` moves to `appSheet`
(`current-state.md` §7). The template stays hidden on the portal (REQUIREMENTS 12).

---

## 17. Work split

| Package | Delivers |
|---|---|
| **WP03** | Everything in `component/sheet/` (§10): API, state on `AnchoredDraggableState`, detents, release rule, stretch, geometry, surfaces (Solid/Frosted), scrim, grabber, header, footer, `SheetColumn`, `sheetBleed`, guard, back, IME, placement policy, `SheetStack` and `cardStackPresenter`, `SheetSource`, presets; the `sheet` token section in `tokens/motion.json`; `LocalOverlaysVisible` and `hideFromAccessibility` in the overlay host; `LocalInSheet` in `AppTopBar`/`AppScaffold`; catalog page (§12); tests §13.1, §13.2, §13.4 (DS journeys), §13.5; deletes `nav3/SheetScene.kt` and the legacy sheet |
| **WP04** | `Presentation.Sheet(options)` and the typed key; `presentation/sheet/` (strategy, decorator, entry frame, `AppSheetNavHost`); wiring in `AppRoot`; `cardStackPresenter(level 0)` on the shell and per presented entry; status-bar flip; `hideFromAccessibility` under presentations; `BottomChromeWriter` keyboard precedence; BackGate/NavGuard reporting; `perf.py` journeys and `gates.toml` rows (§14.1); baseline-profile steps that open, drag and dismiss a sheet; tests §13.3 and the card-stack recomposition journey |
| **WP05** | `GlassSheetSurface` (Live sampling at partial detents, opaque by `e = 0.5`, backdrop pass skipped once opaque); scene access for both hosts; `sheetColors()`, sheet blur (with CP2); GPU budget row and demotion; alignment check under the card stack; WP05.md:64 revised |
| **WP07-B** | §15: the common sheet over shared tokens, `expect`/`actual` pieces, the common presentation host's sheet strategy on 1.1.x, the iOS target without Back, drawn glass |
| **WP08** | §16: `appSheet` options, guard bridge, availability gates, token names |
| **QA2** | §14.2 recordings, §14.3 feel acceptance, the [U] list of §18 measured on the Redmi and from an iOS 26 reference, visual defect sweep (clipping at font 1.5, dark mode, RTL, tablet) |
| **WP12** | Docs in every template and the generator: the feel rules F1-F10, the API with do/don't, the guard pattern, `contentPadding` and `SheetColumn`, the cost table §5.11, the release rule, the recipes (destination sheet, nested navigation in a sheet, Maps-style persistent sheet, zoom from a button), the gates of §14 and how to run them |
| Supporting: **WP01** | the token writer for the `sheet` section in three languages; the glass feature marker on `GlassSheetSurface.kt` |
| Supporting: **WP02-M3** | the sample sheet destination with its test tags; the language picker as `AppChoiceSheet` |

Order: WP03 (API, state, tokens) → WP04 (navigation, shell) and WP05 (glass) in parallel → WP02-M3 sample → QA2 on the
phone → WP07-B, WP08 → WP12.

---

## 18. Measure before trusting

| Item | How | Who |
|---|---|---|
| iOS 26 floating inset, corner radii, the glass-to-opaque curve, the dim strength | iOS 26 screen recording, stepped frame by frame (no device here: the user, or public captures) | QA2 |
| iOS: one very fast flick from Large with Medium present: skip to dismiss or stop | same | QA2 |
| iOS: medium sheet and the keyboard (lift or grow) | same | QA2 |
| iOS: drag-down on a zoomed sheet (shrink into the source or slide) | same | QA2 |
| 400 dp/s detent flick threshold | phone: 20 deliberate drags and 20 flicks, count wrong outcomes | QA2 |
| Redmi status-bar height and display corner radius | `adb shell dumpsys window` / a debug overlay | QA2 |
| Child scrollable vs the sheet's `startDragImmediately` | Robolectric test (§4.2) | WP03 |
| Press delay inside a draggable sheet | Compose test (§4.2) | WP03 |
| Back with the keyboard up closes the keyboard first | phone | QA2 |
| `preferredFrameRate` vote stops when the sheet is idle | Perfetto | WP04 |
| Round-rect clip on the full-screen presenter needs no offscreen buffer | Perfetto `saveLayer` slices | WP04 |
| Glass sampling alignment under the receding presenter | phone recording | WP05 |
| Focus restore API after close | prototype | WP03 |
| CMP iOS inset animation, `NavMetadataKey` in 1.1.x, status-bar hook | prototype | WP07-B |
| SwiftUI `onGeometryChange` on iOS 17, `safeAreaBar`, compiler gate for iOS 27 symbols | Xcode (no Mac here) | WP08 |

---

## 19. Sources

Project:

- `REQUIREMENTS.md:125-136`; `research/sheet/current-state.md` (§2-§12); `research/sheet/research.md` (§0-§6);
  `research/CODE-STANDARDS.md` (API-2, API-6, API-8, API-13, API-14, M-1, M-3, K-2, K-4, K-8, F-3, T-5, T-6, N-5,
  N-6, L-1, L-4); `research/MOTION-PLAN.md:49-75`, `:82-97`, `:154-179`, `:321-328`, `:397-408`, `:526-556`;
  `decisions/CP1.md:32-56`, `:129-138`; `research/predictive-back/SPEC.md:60-87`, `:395-425`;
  `research/ios-motion-catalogue.md:40-48`, `:160-170`; `research/RECOMPOSITION-PLAN.md:45`, `:192-193`, `:220`;
  `research/ADAPTIVE-PLAN.md:231-232`, `:434-435`, `:482`, `:565`; `research/SMOOTHNESS-PLAN.md:63-65`, `:393-403`.
- Code read this session: `ds/component/overlay/AppSheet.kt`, `AppSheetState.kt`, `OverlayBackHandler.kt`,
  `AppOverlayHost.kt`; `ds/theme/AppMotion.kt`; `ds/theme/tokens/DesignTokens.kt`; `ds/foundation/interaction/AppHaptics.kt`;
  `ds/foundation/adaptive/AppLayout.kt`, `SizeClass.kt`; `ds/component/selection/AppSegmentedControl.kt`;
  `wt/WP03/tokens/motion.json`; `nav4/Presentation.kt`, `presentation/PresentationHost.kt`, `AppShell.kt`,
  `back/BackGate.kt`; `wt/WP04/templates/android/tools/perf/gates.toml`, `perf.py`; `glass/GlassQuality.kt`,
  `GlassSurface.kt`, `GlassDefaults.kt`; WP07 `DesignTokens.kt`, `libs.versions.toml`; WP08 `AppSheet.swift`;
  WP03 `core/navigation/SheetScene.kt`.
- Library sources read this session: Compose UI 1.12.1 (`draw/Shadow.kt`, `platform/OutlineResolver.android.kt`,
  `semantics/SemanticsProperties.kt`, `FrameRate.kt`); Compose UI graphics 1.12.1 (`shadow/DropShadowPainter.kt`);
  Flutter `sheet.dart` (pinned, `src/flutter/`).

External:

- Apple [A]: WWDC25 323 https://developer.apple.com/videos/play/wwdc2025/323/ ; WWDC25 284
  https://developer.apple.com/videos/play/wwdc2025/284/ ; Adopting Liquid Glass
  https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass ; WWDC24 10145
  https://developer.apple.com/videos/play/wwdc2024/10145/ ; WWDC21 10063 https://developer.apple.com/videos/play/wwdc2021/10063/ ;
  WWDC19 224 https://developer.apple.com/videos/play/wwdc2019/224/ ; WWDC18 803 https://developer.apple.com/videos/play/wwdc2018/803/ ;
  HIG Sheets https://developer.apple.com/design/human-interface-guidelines/sheets ; the SwiftUI presentation modifiers
  listed in `research.md` §1.2; ProMotion on iPhone
  https://developer.apple.com/documentation/quartzcore/optimizing-promotion-refresh-rates-for-iphone-13-pro-and-ipad-pro
  (not fetched this session).
- Google [G]: Material 3 bottom sheets https://m3.material.io/components/bottom-sheets/specs and `/guidelines`,
  `/accessibility`; `WindowInsets.getRoundedCorner`
  https://developer.android.com/reference/android/view/WindowInsets#getRoundedCorner(int) ; `Outline.canClip`
  https://developer.android.com/reference/android/graphics/Outline#canClip() (not fetched this session); androidx and MDC
  sources pinned in `research/sheet/sources/`.
- Third party [M]: Flutter Cupertino sheet
  https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/cupertino/sheet.dart
