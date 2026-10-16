# Sheet system: the specification

Written 2026-10-13 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). This is the single authoritative spec for the sheet
system of every generated app: implementation agents build from it and reviewers check against it. It replaces the
three competing designs (`research/sheet/design-feel.md`, `design-api.md`, `design-perf.md`), which stay as background.

**How it was made.** `design-feel.md` is the base (all three judges picked it). Its public API is replaced by
`design-api.md` §2's shape. The grafts the judges asked for (`research/sheet/judge-1.md` §3, `judge-2.md` §2-§3,
`judge-3.md` §3) are applied, and their rejects are dropped (listed in §22 so reviewers can flag them). Where the
judges disagreed, the majority decides and the decision row says so. No repository or worktree was changed; no Gradle
or adb was run.

**Precedence.** `REQUIREMENTS.md` > this spec > `design/MASTER-PLAN.md` A3.7/A3.9, `research/MOTION-PLAN.md`,
`research/ADAPTIVE-PLAN.md`, `design/wp/WP05.md` and the handoffs, where they disagree. Every override is marked
**Overrides** in §1.

**Tags.** **[C]** project code, `file:line`. **[V]** verified in a library's shipped sources (by the design authors or
the judges, as cited). **[A]** Apple documentation. **[G]** Google documentation or androidx/MDC code. **[M]** measured
by a third party (Flutter's iOS 18 constants, `src/flutter/sheet.dart`). **[D]** derived here. **[U]** unverified:
measure before relying on it (§21 lists who measures each). **taste**: a design value QA2 confirms from recordings.

**Path prefixes.**

- `ds/` = `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` (head `8ea3985`)
- `ds4/` = the same path in `wt/WP04/` (WP04 owns `foundation/layout/Chrome.kt` there)
- `nav4/` = `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/` (head `230f894`)
- `glass/` = `wt/WP05/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/`
- `src/` = `research/sheet/sources/` (pinned androidx, foundation 1.12.1, MDC, Flutter and nav3-recipes copies)
- `perf/` = `wt/WP04/templates/android/tools/perf/`

**Reference phone.** Redmi Note 10 Pro, 1080 × 2400 px at 2.75 = 392.7 × 872.7 dp, 120 Hz (8.33 ms per frame). Worked
numbers use H = 872.7 dp and a 33 dp status bar [U: read it on the phone, §21]: `largeTop` = 43 dp, Medium's top =
436.4 dp, 2 H/s = 1745 dp/s.

**Definitions used throughout.**

- `y`: the sheet's top edge in px from the top of its bounds (the window for modal sheets). Larger is lower.
- `H`: the height of the sheet's bounds. `h`: the hidden offset (just below the bounds).
- `o_1 < … < o_n`: the offsets of the active detents, tallest first. `o_n` is the lowest detent.
- `largeTop` = `safeDrawing.top + sheet.detent.largeTopGap` (10 dp). For a stacked sheet see B-54.
- `restBottom`: the line a resting sheet's content ends at. `H − navigationBars.bottom` for a sheet anchored to the
  window; `H − bottomChrome` (settled) for a non-modal sheet resting above the bottom chrome (B-52).
- `pf` = `presentationFraction`, `e` = `expansionFraction` (both defined in §4.4).
- `i₀` = the style's floating inset (`AppSheetStyle.floatingInset`), `R` = its corner radius.
- Cost classes are MOTION-PLAN §2's (`research/MOTION-PLAN.md:66-75`): **L0** layer properties only, **L1**
  re-placement, **D1** re-recording one card-size node, **D2** one full-screen blended rect. They appear in cost columns
  and in §10.
- `D-n` are this spec's decisions (§1). `D1`-`D16` (sheet), `A1`-`A5` (action sheet) and `N1`-`N11` (navigation seams)
  are the defects of `research/sheet/current-state.md` §2.6, §3 and §4.4, cited as "fixes D7".

---

## 1. Decisions at a glance

| # | Decision | Rejected alternative | Why |
|---|---|---|---|
| D-1 | **One component, one state type, two hosts.** Destination sheets render in WP04's presentation host (layer 3). In-screen sheets of every modality render in WP03's overlay host (layer 4). Both hosts are outside the recorded glass scene. Both register in one window-level `SheetStack`, which drives the card stack, Back order and the dim | In-place hosting of non-modal sheets (design-api); per-level "sheet layers" (design-perf, never specified) | A non-modal sheet inside the recorded scene could never be Glass (the iOS 26 Maps card), would draw under the floating bar, and would leave the bar bright under its dim (judge-1 reject 6, judge-3 X7/X8) |
| D-2 | **Destination sheets are always modal.** Non-modal sheets are in-screen only | Non-modal destinations | Touch pass-through from the presentation host to a tab host is a cross-host path nobody needs; a persistent sheet belongs to its screen |
| D-3 | **Detents are a sealed type**: `Large`, `Medium`, `FitContent`, `Fraction`, `Height`, `Custom(id, resolver → Dp?)`. Default `[Large]` | Enum `Medium`/`Large` (`ds/component/overlay/AppSheetState.kt:20`); default `{Medium, Large}` | iOS's model and default ("By default, sheets support the large detent" [A]) |
| D-4 | **Release rule:** a flick ≥ 400 dp/s moves exactly one detent; dismissal needs ≥ 2 H/s or 52 % of the lowest-to-hidden travel; otherwise the detent nearest `y + 0.099 s · v` (B-22) | 2 H/s as the only flick threshold (design-api B-10, design-perf §4.3) | Under 2 H/s, a deliberate 1000 dp/s flick from Medium projects 99 dp and falls back: "the sheet ignored me". 2 H/s stays the dismiss rule. 400 dp/s is [U], tuned on the phone |
| D-5 | **Rubber band** `c = 0.55` (shared). Above the top detent the asymptote runs to Large's stretch line (0.008 H above `largeTop`); below the lowest detent while dismissal is blocked it runs to hidden | 0.008 H above any top detent (design-api B-8) | A Medium-only sheet would get 7 dp of stretch and feel welded (judge-1 reject 11) |
| D-6 | **Floating at partial detents, attached at Large** (iOS 26), with one uniform corner radius. On by default for every material; the inset is a per-design-style token (Editorial 0). Never floating under 3-button navigation, at Compact height, or for a sheet without a partial detent | Floating off for Solid (design-perf decision 15) | Solid is every shipped style's material: the default app would never show REQUIREMENTS 33's inset sheet |
| D-7 | **Content is measured once, at the sheet's full width.** The floating inset is a draw-time clip; the header slot sits inside a constant `i₀` side inset. No `sheetBleed` modifier | Content measured at the floating width plus `Modifier.sheetBleed()` (design-feel) | No adoption tax, no 8 dp strips at Large, nothing reflows either way (judge-3 X2; judge-1 open issue) |
| D-8 | **One dim, drawn once, by the presenter**, as one rect whose colour and alpha are the exact composite of the card overlay and the scrim (§6.4). It never lightens as the sheet rises. The scrim colour stays `colors.scrim` | A lerp from scrim to overlay that lightens toward Large (design-feel, design-api `× (1 − f)`); two rects in dark mode (design-perf) | Majority (judges 1, 2). One D2 rect in both themes. iOS's dim strength is [U]: QA2 compares, then the token's values change, never the mechanism (judge-3 X17) |
| D-9 | **Card stack**: the presenter scales to 0.9165 about its top centre and its top moves to `safeDrawing.top`; the sheet's top at Large is `safeDrawing.top + 10 dp`, so the presenter peeks 10 dp. **Overrides** MOTION-PLAN M6's "moves down by the status bar + 10 dp" | 7 % of H; status-bar-only tops | On Android's short status bars 7 % leaves a black band; `safeDrawing` covers a ChromeOS caption bar and a landscape cutout (judge-1 graft 5) |
| D-10 | **Stacked sheets**: the covered sheet lifts 0.005 H and scales to 0.9165; the covering sheet's Large top is the covered sheet's lifted top + 10 dp; at most two covered sheets are drawn | The new sheet 0.02 H higher (design-api B-36) | 0.02 H higher puts the grabber in the Redmi's status bar and punch-hole; a 10 dp peek reads as a stack (judge-2 graft 25) |
| D-11 | **Confirm hook in the content**: `SheetDismissGuard(enabled, onAttempt: (SheetDismissSource) -> Unit)`. `dismissible = false` is the static "never closable by the person" switch. `scrimTapDismisses` is the only per-path switch | Call-site `onDismissAttempt` plus per-path flags (design-perf) | A destination's screen never sees the `AppSheet` call, so only a content-side guard can protect a dirty form there |
| D-12 | **Back closes in one gesture**: MDC bottom container, eased progress, content counter-scale. Only a persistent non-modal sheet steps down, automatically. Back never reaches the screen beneath a **modal** sheet | Public `SheetBackBehavior.StepDown`; Back passing through a non-dismissible modal sheet (design-feel §4.5) | Gmail and iOS close in one gesture; two Backs to close was material3's bug source (`02b1dbca`) |
| D-13 | **Rapid Back during a dismissal**: a destination sheet pops first and slides out in `onRemove()`, and the presentation host's `BackGate` counts the extra presses; an in-screen sheet consumes and drops them | Pop after the slide (design-perf); extra presses popping the screen beneath an in-screen sheet (design-perf) | Navigation 3's `OverlayScene` contract animates after the pop; a reflexive double press must not cost the person their screen (WP03 `SheetTest.kt:65-79`) |
| D-14 | **Explicit Back enablement** (B-37), including "the IME target is hidden", so Back closes the keyboard first | Trusting registration order | `navigationevent` asks overlay handlers before default ones, LIFO across child dispatchers [V] (`NavigationEventProcessor.kt:272`, `:579`); the platform registers an overlay-priority input that can outrank the IME [V] (`OnBackInvokedInput.android.kt:38-48`) |
| D-15 | **`HostMotion`** for both hosts: a sheet's show and hide are reported to its host's gate, so `NavGuard` holds pushes while it rises or leaves | Reporting only destination sheets | In-screen sheets also move their host's content (judge-1 graft 3) |
| D-16 | **Haptic on arrival**: the `Threshold` haptic plays on the first frame the settle is within 1 px of a new detent | On settle start (`ds/component/overlay/AppSheetState.kt:194-195`) | The tick must coincide with the visual snap |
| D-17 | **Materials Solid / Frosted / Glass.** `Automatic` follows the design style's `sheetMaterial` token (Solid unless the Glass design style). Glass samples live only at partial detents on the Live tiers and is opaque by `e = 0.5`. **Overrides** `design/wp/WP05.md:64` ("no glass on sheets") | `Automatic` = Glass whenever glass ships (design-feel) | REQUIREMENTS 33 is later and wins; live half-screen sampling must not be the default on a GPU-bound phone (CP1 §1: push/pop GPU p90 12 ms against 8.33 ms) |
| D-18 | **Shadow**: none on modal bottom sheets. Non-modal, side, form and anchored-card surfaces draw a nine-slice made once from a small round-rect shadow, outside the shape only, light theme only | A `dropShadow` per surface size (all three designs) | [V] `ShadowRenderer.kt:70-79` rebuilds a CPU-blurred Alpha8 bitmap whenever the size changes, and `AndroidShadowContext.android.kt:53-84` caches one per size without a bound; a floating sheet changes size on every drag frame (judge-2 §2.1, blocker) |
| D-19 | **Form sheet** (Center placement): slides up, can be swiped away, no detents, height `min(0.85 H, 620 dp, fit)`, predictive back scales it about its centre to 0.90. **Overrides** `ADAPTIVE-PLAN.md:434` (scale-in on `dialog`, no drag). **taste**: confirmed or reverted at the sheet checkpoint from QA2's recording | Scale-in on `dialog` with no drag (design-api, design-perf, judge 3) | Majority (judges 1, 2): iPadOS form sheets slide up and swipe away; a scale-in is the alert's language |
| D-20 | **Placement**: modal sheets resolve from the **window** class, non-modal sheets from their pane's container class. Automatic: Medium width and up → Center for modal, Start for non-modal from Expanded width. A size-class change snaps to the new frame at the same detent. **Overrides** ADAPTIVE-PLAN's "every rule reads `AppTheme.containerClass`" for modal sheets | Container class for every sheet; a morph on resize (design-feel §4.13) | `HostedOverlay` re-provides the caller's locals, including the container class (`ds/component/overlay/AppOverlayHost.kt:92`, `ds/foundation/adaptive/WindowClass.kt:66` [C]): a modal sheet from a Compact pane on a tablet would come up as a phone sheet across 1280 dp. A resize is not navigation |
| D-21 | **Action sheet at Medium width and up**: an in-window card anchored to a `SheetAnchor` | `AppMenu`'s `Popup` window (`ds/component/overlay/AppMenu.kt:40-42` [C]) | One window, glass can sample it, and A4 (wrong anchor) is fixed |
| D-22 | **No `Dialog` or `Popup` window** anywhere in the sheet system | — | No glass sampling across windows (`glass/SceneBackdrop.kt:105-113` [C]), no card stack, an extra window in the first frame |
| D-23 | **Navigating from a presented sheet**: a sheet key stacks on it; a cover or an ordinary key first closes the presented sheets, then is submitted once the presentation gate settles. This settles "a cover over a sheet": it never happens | Cover as an overlay scene above the sheet | With `[base, sheet, cover]` the cover is a non-overlay scene, so the sheet's `OverlayScene` is removed and the sheet would vanish mid-push (judge-2 §2.2); HIG: close the sheet before navigating [A] |
| D-24 | **In-screen sheets park with their owner**: hidden instantly while the owner's tab is hidden; slid away on `sheet` while the owner is covered by a push or a presentation, and back when it is uncovered. Logical state is kept | Kept drawn over the next tab (N7); hidden with a pop-in | Fixes N7 and judge-3 G23 without a visible pop |
| D-25 | **The sheet adds no scroller.** `SheetColumn` is the column for forms. `contentPadding` is a snapshot-backed `PaddingValues` read in measure. No `fitInside(Ime)` or `imePadding()` inside a sheet | Wrapping content in `verticalScroll` (VitFleet); the edge-to-edge skill's `fitInside` | A wrapper scroller breaks lazy lists; [V] `RulerAlignment.kt:36-50` re-measures `fitInside`'s child in placement on every IME frame |
| D-26 | **Keyboard**: `SheetKeyboard.Lift` (default) rides the keyboard in placement; `Expand` goes to the top detent. While the IME target is visible, the content box is measured to the region actually visible (two re-measures per IME transition) | Content padding only (keeps `bringIntoView` aimed at an off-screen viewport) | Fixes D4 including focus scrolling at a partial detent |
| D-27 | **`AppSheetState` is a concrete `@Stable` class.** **Deviation** from CODE-STANDARDS API-8 (interface + factory) | Interface + factory (design-feel; judge 2 kept it) | Judge 3 (G2), design-api and design-perf: the layout writes anchors and reads motion specs through internal members an interface would have to publish, or reach by a cast; material3's `SheetState` and foundation's `AnchoredDraggableState` are classes |
| D-28 | **`AppSheetStyle`** bundles the look (material, colours, corner, inset, max width, shadow), built by `AppSheetDefaults.style()`. **Deviation** from CODE-STANDARDS API-7 (no grab-bag): it holds no structural variant, colours still resolve through `AppSheetColors`, and it maps 1:1 onto Compose's `Style` when that API is stable | Flat `cornerRadius`, `floatingInset`, `maxWidth`, `colors` parameters (judge 2) | Majority (judges 1, 3); keeps the §4.1 call sites short and the Styles migration mechanical |
| D-29 | **Tokens**: a new `sheet` section in `tokens/motion.json`; `sheetInset` and `sheetMaterial` per design style in `tokens/styles.json`. Reduce-motion fades use the existing `fades.longMs`/`fades.shortMs`. **Overrides** MOTION-PLAN §1 rule 5's 150 ms literal | Private `val`s (`ds/component/overlay/AppSheet.kt:335-345` [C]) | One source of truth for Android, Multiplatform and SwiftUI |
| D-30 | **Medium = 0.5 H** (top at the window's middle). **taste** against WP03's 0.55 | 0.55 (`AppSheet.kt:335` [C]) | iOS "approximately half" [A] |
| D-31 | **Zoom from a source** ships in phase 2 (§20) as `SheetTransition.Zoom`; `SheetTransition` is sealed so it is not a breaking change | Zoom in v1 (design-feel) | Not in REQUIREMENTS 33; it adds a second progress, a source registry and a glass morph |
| D-32 | **No navigation results store** in this work (`NavResultKey`, `popWithResult`) | design-api §2.9 | A navigation-wide feature; WP04's backlog |

---

## 2. Feel rules

Every later section implements these. Reviews cite them.

- **F1 The finger owns the sheet.** It tracks 1:1. A touch catches a sheet that is presenting or settling. A release
  seeds a spring with its velocity. Never a tween after a finger (`MOTION-PLAN.md:54-58`).
- **F2 Content never reflows while the sheet moves.** It is measured once; only the surface around it changes.
- **F3 Stops are chosen by intent; dismissal needs conviction.**
- **F4 Edges resist; nothing stops dead.** Past the top, and past the bottom when dismissal is blocked, the sheet
  rubber-bands with iOS's `c = 0.55`.
- **F5 Floating while partial, anchored when full.**
- **F6 One dim, drawn once**, in the presenter's layer, and it never lightens as the sheet rises.
- **F7 A haptic marks arrival**, never departure.
- **F8 No shadow follows a size that animates, and nothing ever fades through a shadow.**
- **F9 One gesture closes**, unless the content says it is precious (the guard).
- **F10 Every per-frame value is read in placement, a layer block or draw.** Zero recompositions per drag frame.

---

## 3. Anatomy, hosts and states

### 3.1 Layers

```
window root (AppTheme: provides LocalSheetStack and the overlay host)
├── shell, level 0                 Modifier.cardStackPresenter(stack, level = 0) on ShellChrome's box
│   └── GlassStage (glass only)    records the tab content only
├── presentation host, layer 3     destination sheets (SheetScene → PresentedSheetFrame) and covers (PresentedFrame);
│                                  a cover's frame applies cardStackPresenter(stack, level = its depth)
├── overlay host, layer 4          in-screen sheets of every modality, action and choice sheets, alerts, menus, HUD
└── toasts, layer 5

SheetLayout: one Layout per sheet, filling its bounds
├── scrim node        hit-testing and semantics; draws the dim only when no presenter claims it (§6.4)
└── sheet layer       placed at y (L1); graphicsLayer: back shrink and covered-sheet recede (L0)
    ├── surface node  SheetSurfaceNode: body, rim, nine-slice shadow, Glass backdrop through SheetBackdrop
    ├── grabber slot  over the top band; takes no layout room
    ├── header slot   pinned above the content, inside a constant i₀ side inset
    ├── content       clipped to the visible surface in draw; back counter-scale in its layer (L0); measured once
    └── footer slot   placed at the visible bottom or on the keyboard (L1)
```

The z-order is content (0) < tab bar (1) < presentation host (3) < overlay host (4) < toasts (5)
(`ds/component/overlay/AppOverlayHost.kt:20-33` [C]; `nav4/presentation/PresentationHost.kt:53-56` [C]).

### 3.2 Levels

- Level 0 is the shell. A presented entry at presentation depth `d` is level `d`. WP04 provides
  `LocalPresentationLevel` to each presented entry's content (§4.10).
- A sheet's level is its owner's level + 1. An in-screen sheet opened from a sheet's content takes the next level;
  every sheet provides its own level to its content.
- A sheet's **presenter** is whatever sits at the level below it: the shell, a cover, or another sheet.

### 3.3 States and interruption

```
Hidden ──show──▶ Presenting ──arrives──▶ Resting(detent) ◀──settle── Settling ◀──release── Dragging
                     │  ▲                     │  ▲                      │  ▲                  ▲
                     │  └──finger catches─────┼──┼──────────────────────┘  │                  │
                     │                        │  └──finger down───────────┼──────────────────┘
                     └──────── dismiss ───────┴──────────────▶ Leaving ──gone──▶ Hidden
Parked (in-screen only): any state above except Leaving ⇄ Parked, logical state kept (B-14)
```

| Running | Finger down on the sheet | Back | `animateTo` | `hide()`, `visible = false`, pop | `show()` | Window resize |
|---|---|---|---|---|---|---|
| Presenting | catches it, velocity kept (B-17) | reverses into the dismissal, velocity kept | retargets, velocity kept | reverses to hidden, velocity kept | no-op | anchors retarget in flight |
| Resting | drag | predictive dismiss (B-38) | animates | leaves | no-op | stays on its detent |
| Settling | catches it | predictive dismiss | retargets | leaves, velocity kept | no-op | retargets |
| Leaving, decided by the person | ignored | destination: counted by the host gate; in-screen: consumed (B-43) | ignored (D9) | joins | turns around with `lastVelocity` (D1) | hidden retargets |
| Leaving, programmatic | ignored | as above | ignored | joins | turns around | — |
| Parked | not hit-tested | not registered | ignored | leaves (no animation needed) | no-op | anchors move |

A person who flicked a sheet away has committed; catching it on the way out would undo a decision. A program that
hides and re-shows within the slide gets a smooth reversal instead of a stuck sheet.

---

## 4. Public API (Android, `:core:designsystem` and `:core:navigation`)

Package `com.base.app.core.designsystem.component.sheet` unless noted. Every public declaration gets KDoc (C-4). The
KDoc below is the intent the real KDoc must carry, trimmed of what the signature already says (C-5).

### 4.1 Acceptance examples

Each block is the whole call for a common sheet. **Review rule: a change that makes one of these longer is wrong.**
They are also compiled as tests (§15.2 `SheetApiShapeTest`).

```kotlin
// A quick in-screen sheet: full height, a title and a close button.
AppSheet(
    visible = showFilters,
    onDismissRequest = { showFilters = false },
    header = { AppSheetHeader(title = stringResource(R.string.orders_filters)) },
) {
    FilterList(contentPadding = contentPadding)
}

// Half height that can grow, with a list handing off to the sheet.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(detents = listOf(SheetDetent.Medium, SheetDetent.Large)),
) {
    LazyColumn(contentPadding = contentPadding) { items(places, key = { it.id }) { PlaceRow(it) } }
}

// A short form sized to its content, a sticky Save above the keyboard, a confirmation while dirty.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(detents = listOf(SheetDetent.FitContent)),
    header = { AppSheetHeader(title = stringResource(R.string.lists_rename)) },
    footer = { AppBottomActionBar { AppButton(text = save, onClick = onSave, modifier = Modifier.weight(1f)) } },
) {
    SheetDismissGuard(enabled = form.changed) { showDiscard = true }
    SheetColumn { RenameField(form.name, onNameChange) }
}

// A Maps-style persistent sheet: a peek, half and full; the map stays usable up to half.
AppSheet(
    visible = true,
    onDismissRequest = {},
    state = rememberAppSheetState(detents = listOf(SheetDetent.Height(96.dp), SheetDetent.Medium, SheetDetent.Large)),
    modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
    dismissible = false,
    header = { SearchHeader() },
) {
    PlaceList(contentPadding = contentPadding)
}

// A destination sheet, registered once; anyone navigating to the key gets a sheet over the tab bar.
entry<ShareKey>(presentation = Presentation.Sheet(SheetSpec(detents = listOf(SheetDetent.Medium, SheetDetent.Large)))) {
    ShareRoute(it)
}

// Glass, through the liquid glass feature's tiers.
AppSheet(visible = show, onDismissRequest = { show = false }, style = AppSheetDefaults.style(material = SheetMaterial.Glass)) {
    NowPlaying()
}

// A custom detent: a tenth of the room, never under 44 dp (Apple's CustomPresentationDetent sample).
val Sliver = SheetDetent.Custom("sliver") { maxOf(44.dp, maxHeight * 0.1f) }

// Choices.
AppActionSheet(visible = show, actions = actions, onDismissRequest = { show = false }, title = title)
AppChoiceSheet(visible = show, choices = languages, selected = current, onSelect = onLanguage,
               onDismissRequest = { show = false }, label = { it.displayName })
```

### 4.2 Surface map

| Layer | Types | Who uses it |
|---|---|---|
| Component | `AppSheet` (three overloads) | every sheet |
| State | `AppSheetState`, `rememberAppSheetState` | callers that need detents, selection or programmatic moves |
| Detents | `SheetDetent`, `SheetDetentContext` | callers with custom heights |
| Behaviour | `SheetModality`, `dismissible`, `AppSheetProperties` (`SheetPlacement`, `SheetContentInteraction`, `SheetTransition`, `SheetKeyboard`, `scrimTapDismisses`), `SheetDismissSource` | callers that change what the sheet does |
| Look | `AppSheetStyle`, `SheetMaterial`, `AppSheetColors`, `AppSheetDefaults` | callers that change how it looks; the theme |
| Slots and content | `AppSheetScope`, `AppSheetGrabber`, `AppSheetHeader`, `AppSheetCloseButton`, `SheetColumn`, `sheetContentPadding()`, `ProvideSheetHeader`, `ProvideSheetFooter`, `SheetBackHandler`, `SheetDismissGuard`, `LocalSheetSurface` | content, including a destination's screen |
| Presets | `AppActionSheet`, `SheetAction`, `SheetAnchor`, `AppChoiceSheet` | quick choices |
| Navigation (WP04) | `Presentation.Sheet(SheetSpec)`, `SheetSpec`, `AppSheetNavHost` | features |
| Seams (not for features) | `SheetStack`, `LocalSheetStack`, `Modifier.cardStackPresenter`, `HostMotion`, `LocalHostMotion`, `EntryVisibility`, `LocalEntryVisibility`, `PaneBounds`, `LocalPaneBounds`, `LocalPresentationLevel`, `SheetBackdrop`, `SheetFrame`, `LocalSheetBackdrop` | the shell and navigation (WP04), liquid glass (WP05) |

Rules the whole surface follows (CODE-STANDARDS §2):

- One `modifier`, applied first to the sheet **surface** (not the bounds-filling host box), so `testTag` and
  `semantics` land on the sheet (API-3).
- No `title: String`, `showCloseButton` or `showHandle` flags: those are slots with ready-made content (API-5).
- Every frequently changing value is a `@FrequentlyChangingValue` getter read in layout or draw (API-8, R7). No
  `State<T>` parameters.
- No visual value is read anywhere except from `AppSheetStyle`, and `AppSheetStyle` holds nothing that is not visual.

### 4.3 `AppSheet`

File `component/sheet/AppSheet.kt`.

```kotlin
/**
 * A sheet in the app's design language with iOS 26 behaviour. It rises to one of [state]'s detents,
 * follows the finger between them, floats inset with rounded corners at partial detents and attaches
 * to the window's edges at [SheetDetent.Large], where the screen behind recedes into a card. The
 * person closes it by dragging it down, tapping the dimmed screen, Back (the predictive shrink),
 * Escape, or a close button.
 *
 * Keep it composed and drive [visible]. Showing it again while it leaves turns it around at the
 * speed it had. [onDismissRequest] runs once, when the person has decided to close it, and the sheet
 * is already leaving: set [visible] to false there.
 *
 * The content is measured once per window size and only re-placed while the sheet moves. The sheet
 * adds no scroll container: bring a lazy list or [SheetColumn] and pass it
 * [AppSheetScope.contentPadding], which keeps the last item above the footer, the navigation bar
 * and the keyboard.
 *
 * A modal sheet draws over the whole window, the tab bar included. A non-modal sheet stays within
 * its pane and rests above the bottom chrome at its undimmed detents.
 *
 * @param state detents and position; one per call site.
 * @param dismissible false: the person cannot close the sheet. A drag down rubber-bands, Back on a
 *   modal sheet is consumed, and attempts go to a [SheetDismissGuard] in the content. To ask before
 *   closing a dirty form, use a guard instead. Code can always close it.
 * @param onDetentChange runs when the person settles the sheet on a different detent; not for moves
 *   made from code.
 * @param grabber drawn over the top edge; null for none. The default hides itself when the sheet has
 *   one active detent, is a form sheet, or is covered by another sheet.
 * @param header pinned above the content. Content can replace it with [ProvideSheetHeader].
 * @param footer pinned at the visible bottom, above the navigation bar and the keyboard. Content can
 *   replace it with [ProvideSheetFooter].
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
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
)

/**
 * A sheet for [item] while it is not null, like SwiftUI's `sheet(item:)`. The last item stays
 * composed during the exit. A different item while one is shown closes the sheet, then presents
 * the new one: one sheet at a time, as the HIG asks.
 */
@Composable
fun <T : Any> AppSheet(
    item: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.(T) -> Unit)? = null,
    footer: (@Composable AppSheetScope.(T) -> Unit)? = null,
    content: @Composable AppSheetScope.(T) -> Unit,
)

/**
 * The sheet without its own visibility, for a presenter that owns where it is drawn: the shell's
 * sheet scene or a custom host. It draws where it is composed and appears and leaves only through
 * [state]'s show() and hide(); keep it composed until hide() returns. App code uses the overloads
 * that take `visible` or `item`.
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
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
)
```

Parameter order (API-2): required, `modifier`, the state (as material3 places `sheetState`), behaviour, the
`XDefaults` value, the callback, then slots. The `visible` and `item` overloads host themselves through
`HostedOverlay` (B-12); the `state` overload draws in place.

### 4.4 `AppSheetState`

File `component/sheet/AppSheetState.kt`.

```kotlin
/**
 * Where a sheet rests and how it moves, kept outside composition so a presenter can show it, hide
 * it and read its position from layout and draw.
 *
 * It wraps foundation's AnchoredDraggableState: a drag, a fling, a programmatic change and a window
 * resize go through one mutex, so any of them interrupts another and the motion continues from the
 * current offset at the current velocity.
 *
 * [offset], [presentationFraction] and [expansionFraction] change on every frame of a drag: read
 * them in a layout, layer or draw block only. [currentDetent], [targetDetent], [isVisible] and
 * [isMoving] change at most once per settle and may be read in composition.
 */
@Stable
class AppSheetState @RememberInComposition constructor(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
) {
    /** The heights the sheet can rest at, in any order; at least one. */
    val detents: List<SheetDetent>

    /** The detent the sheet rests on or is settling onto; null while hidden or leaving. */
    val currentDetent: SheetDetent?

    /** Where the sheet is heading: the release target while settling, the nearest stop while dragged. */
    val targetDetent: SheetDetent?

    /** True from show() until the sheet has left after hide() or a dismissal. */
    val isVisible: Boolean

    /** True while the sheet is dragged or animates. */
    val isMoving: Boolean

    /** The sheet's top edge in px from the top of its bounds; NaN before its first layout. */
    @get:FrequentlyChangingValue
    val offset: Float

    /** 0 hidden, 1 at the lowest active detent and above. The dim follows it. */
    @get:FrequentlyChangingValue
    val presentationFraction: Float

    /**
     * 0 at the highest partial detent and below, 1 at [SheetDetent.Large]; 0 for a sheet without
     * Large, a form sheet and a side sheet. For a sheet whose only detent is Large it rises with the
     * present. The card stack, the floating-to-attached change and the glass opacity follow it.
     */
    @get:FrequentlyChangingValue
    val expansionFraction: Float

    /** Shows the sheet at [detent], or its initial detent. A sheet that is leaving turns around. */
    suspend fun show(detent: SheetDetent? = null)

    /** Slides the sheet away and returns once it has gone. A second call joins the first. */
    suspend fun hide()

    /** Moves to [detent], keeping the current velocity. Ignored while leaving or parked. */
    suspend fun animateTo(detent: SheetDetent)

    suspend fun snapTo(detent: SheetDetent)

    /** One detent up; false at the top. */
    suspend fun expand(): Boolean

    /** One detent down, never to hidden; false at the bottom. */
    suspend fun collapse(): Boolean

    /**
     * Replaces the detents. A sheet resting on one that stays active stays; otherwise it animates to
     * the nearest active one.
     */
    fun updateDetents(detents: List<SheetDetent>)

    companion object {
        /**
         * Saves whether the sheet was shown and the identity of the detent it rested on. The detents
         * come back from the caller, because a custom resolver cannot be saved.
         */
        fun Saver(detents: List<SheetDetent>): Saver<AppSheetState, String>
    }
}

/**
 * An [AppSheetState] kept across recomposition, configuration changes and process death. A sheet
 * restored this way appears at its detent without sliding in. A changed [detents] list is applied
 * with [AppSheetState.updateDetents].
 */
@Composable
fun rememberAppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState
```

Definitions (symbols as defined at the top; `y` is the anchored offset, before the park and keyboard translations):

- `presentationFraction` = `clamp((h − y) / (h − o_n), 0, 1)`.
- `expansionFraction`: with `y_P` = the offset of the highest active partial detent (or `h` when there is none) and
  `y_L` = Large's offset: `clamp((y_P − y) / (y_P − y_L), 0, 1)` when Large is active, else 0; 0 for Center and
  Start/End placement.

Notes for WP03 (not public):

- Inside: `AnchoredDraggableState<SheetStop>`, `SheetStop` = `Hidden` | `At(detent)`. `Hidden` is in the anchor set
  while presenting, while leaving, and whenever the person may dismiss by drag; otherwise it is absent and the drag
  below the lowest detent goes to the rubber band. That is foundation's replacement for the deprecated
  `confirmValueChange` (`src/foundation-1.12.1/.../AnchoredDraggable.kt:1716-1735` [G]).
- `currentDetent` and `targetDetent` are `derivedStateOf` over the foundation state's settled and target values
  (`AnchoredDraggable.kt:871` [G]).
- Mutex priorities (`MutatorMutex` interrupts a running mutation of equal or lower priority [U: re-read
  `MutatorMutex` in foundation 1.12.1]):
  - drag and the release's settle: `UserInput` (foundation's);
  - `show()` and `hide()`: `PreventUserInput`, so code closing its sheet wins over a finger, and `show()` during
    `hide()` interrupts it and starts from the current offset with `lastVelocity`;
  - the catch (B-17): `PreventUserInput`, so a finger stops a presenting sheet; it is skipped while the sheet leaves
    or is parked;
  - `animateTo`, `expand`, `collapse`: `UserInput`, so they retarget a settle with its velocity; they return without
    moving while a finger is down on the sheet, while it leaves and while it is parked.
- Springs come from `AppTheme.springs` through the attached layout node, never from fields written in composition
  (fixes D14). Before the first layout every request waits for the anchors (`AppSheetState.kt:107-118`, `:132` [C]).
- **Saver encoding:** one string, `"<shown>|<key>"`, where `<shown>` is `1` or `0` and `<key>` is `large`, `medium`,
  `fit`, `fraction:<f>`, `height:<dp>` or `custom:<id>`. A restored key that is no longer listed or active maps to the
  nearest active detent (B-62).
- `rememberAppSheetState` writes a changed `detents` list through `updateDetents` from a `SideEffect`, only when the
  list differs.
- An internal `presentSpring` (default `springs.sheet`) lets the presets arrive on `springs.dialog` (M17) without a
  public parameter.

### 4.5 Detents

File `component/sheet/SheetDetent.kt`.

```kotlin
/**
 * A height a sheet can rest at. A sheet lists one or more; the person moves between them by dragging,
 * flicking, tapping the grabber, or with the grabber's accessibility actions.
 *
 * Heights are measured within the safe area: above the navigation bar, or above the bottom chrome for
 * a non-modal sheet that rests on it. They resolve in the sheet's measure pass whenever the window,
 * the fold or the content's height changes, never during a drag. A detent may resolve to nothing: an
 * inactive detent is skipped, as iOS skips medium in compact height.
 */
@Immutable
sealed interface SheetDetent {
    /** Nearly the whole window; the screen behind recedes into a card. Always active. */
    data object Large : SheetDetent

    /** About half the window: the sheet's top at the middle. Inactive in Compact height. */
    data object Medium : SheetDetent

    /**
     * The sheet's own height: grabber band, header, content and footer as measured, capped at Large.
     * Content that fills the height it is given (a lazy list) resolves to Large. When the content's
     * height changes, a sheet resting here animates to the new height.
     */
    data object FitContent : SheetDetent

    /** [fraction] of the height Large shows, in (0, 1]. */
    data class Fraction(val fraction: Float) : SheetDetent {
        init { require(fraction > 0f && fraction <= 1f) { "A fraction is in (0, 1]: $fraction" } }
    }

    /** A fixed height: a peek that shows a search field, a mini player. */
    data class Height(val height: Dp) : SheetDetent

    /**
     * A height from [resolve], or null to leave the detent out in that context, as iOS's custom
     * resolvers do. Two Custom detents with the same [id] are the same detent, so the lambda may be
     * created in composition. [resolve] runs in measure and must read no snapshot state.
     */
    class Custom(val id: String, val resolve: SheetDetentContext.() -> Dp?) : SheetDetent {
        override fun equals(other: Any?): Boolean = other is Custom && other.id == id
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "Custom($id)"
    }
}

/** What a [SheetDetent.Custom] resolver can read. Every value is settled; none changes during a drag. */
interface SheetDetentContext : Density {
    /** Large's height: the most any detent shows. iOS's maximumDetentValue. */
    val maxHeight: Dp
    /** The height of the sheet's bounds: the window, or the pane of a non-modal sheet. */
    val containerHeight: Dp
    /** Grabber band, header, content and footer as measured. Always measured, whatever the detents. */
    val contentHeight: Dp
    /** The window's class for a modal sheet; the pane's for a non-modal one. */
    val sizeClass: SizeClass
    /** The bottom of a tabletop fold, from the top of the bounds, or null. */
    val foldBottom: Dp?
    /** Another built-in detent's height in this context, or null when it is inactive. Null for Custom. */
    fun heightOf(detent: SheetDetent): Dp?
}
```

Resolution rules (WP03, `layout/DetentResolution.kt`, pure Kotlin, unit-tested). Heights are "within the safe area",
measured up from `restBottom`; the offset is `restBottom − height` except for Large and Medium, which are absolute.

| # | Rule |
|---|---|
| R-1 | `Large`: offset `largeTop`; height `maxHeight = restBottom − largeTop`. For a stacked sheet see B-54. |
| R-2 | `Medium`: offset `0.5 · H` (`sheet.detent.mediumFraction`). **Inactive** in Compact height [A], and when it lies within 48 dp (`sheet.detent.mediumMinGap`) of Large. In tabletop posture its top rests at `foldBottom` (kept from `ds/component/overlay/AppSheet.kt:138-139`, `:235` [C]). |
| R-3 | `FitContent`: height = the column's measured height (grabber band or header, content measured with `maxHeight` as its limit, footer). |
| R-4 | `Fraction(f)`: height `f · maxHeight`. `Height(d)`: height `d`. `Custom`: the resolver's value. |
| R-5 | Every height is clamped to `[sheet.detent.minHeight (48 dp), maxHeight]`. |
| R-6 | Detents that resolve within 1 px of each other merge into the first in list order, so `[FitContent, Large]` with a full-height list is one stop. |
| R-7 | Stops are ordered by offset; list order matters only for R-6 ties and `initialDetent`. |
| R-8 | If every detent is inactive, the sheet uses `Large`. |
| R-9 | An inactive initial detent opens at the nearest active one by height. |
| R-10 | Center placement has one stop, its fitted height `min(0.85 · H, 620 dp, contentHeight)` (§8). Start/End keep their detents, resolved against the panel's height. |
| R-11 | When the stops change (rotation, unfold, resize, new detents, content height), the sheet keeps its detent by identity and an animation in flight is retargeted: `updateAnchors(new, newTarget = target)` (`AnchoredDraggable.kt:1001-1029` [G]). Anchors are written in measure only when they changed. |

### 4.6 Behaviour types

Files `component/sheet/SheetModality.kt`, `AppSheetProperties.kt`.

```kotlin
/** Whether what is behind the sheet stays usable. */
@Immutable
sealed interface SheetModality {
    /** Dims and blocks what is behind at every detent and hides it from screen readers; drawn over the tab bar. */
    data object Modal : SheetModality

    /**
     * What is behind stays usable, undimmed and visible to screen readers while the sheet rests at
     * [upThrough] or lower (every detent when null), as iOS's
     * presentationBackgroundInteraction(.enabled(upThrough:)). Above it the dim fades in over the
     * travel to the next detent up, and the sheet blocks like a modal one. With [aboveBottomChrome]
     * the sheet rests above the floating tab bar at its undimmed detents.
     */
    data class NonModal(
        val upThrough: SheetDetent? = null,
        val aboveBottomChrome: Boolean = true,
    ) : SheetModality
}

/** How the person tried to close a sheet. */
enum class SheetDismissSource { Drag, Scrim, Back, Escape, Request }

/** Behaviour a sheet seldom changes. */
@Immutable
data class AppSheetProperties(
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Automatic,
    val transition: SheetTransition = SheetTransition.Slide,
    val keyboard: SheetKeyboard = SheetKeyboard.Lift,
    /** False makes a tap on the dim do nothing; drag and Back still close the sheet. */
    val scrimTapDismisses: Boolean = true,
)

/** Where the sheet sits. Automatic follows the window (§8), as iOS 27's presentationPlacement(.automatic). */
enum class SheetPlacement { Automatic, Bottom, Center, Start, End }

/** What a vertical scroll inside the sheet does first. */
enum class SheetContentInteraction {
    /** Resizes for a modal sheet on touch; Scrolls for a non-modal sheet and for a window with only a mouse. */
    Automatic,
    /** Grows the sheet to its top detent, then scrolls: iOS's default. */
    Resizes,
    /** Scrolls at every detent; the sheet resizes from its grabber, header, footer and a pull at the content's top. */
    Scrolls,
}

/** How the sheet arrives and leaves. Reduce motion turns every transition into CrossFade. */
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

`SheetPlacement.Start`/`End` are iOS 27's `.leading`/`.trailing`, named for Compose's RTL-aware convention.

### 4.7 Style

Files `theme/AppDesignStyle.kt` (the enum, so the theme never imports a component), `component/sheet/AppSheetStyle.kt`,
`AppSheetDefaults.kt`.

```kotlin
/** What a sheet's surface is made of. The design style's `sheetMaterial` token is one of the last three. */
enum class SheetMaterial {
    /** The design style's choice; Solid unless the Glass design style. */
    Automatic,
    /** The opaque container. The cheapest, and what every material becomes under Reduce transparency. */
    Solid,
    /** Drawn translucency (container at 92-94 %, a rim, a sheen). Samples nothing: a few cached brush draws. */
    Frosted,
    /**
     * Live liquid glass at partial detents on the Live tiers, opaque by halfway to Large; Frosted on the
     * lower tiers and without the liquid glass feature.
     */
    Glass,
}

/** A sheet's colours. */
@Immutable
class AppSheetColors(
    /** The body. Frosted and Glass draw it translucent at partial detents (sheet.frosted, sheet.glass tokens). */
    val container: Color,
    val content: Color,
    val grabber: Color,
    val rim: Color,
    /** The dim at full strength, alpha included. */
    val scrim: Color,
)

/**
 * How a sheet looks. Build it with [AppSheetDefaults.style]. Each field maps to a property of
 * Compose's Style, so moving the sheet to Modifier.styleable is mechanical (§4.7 mapping).
 */
@Immutable
class AppSheetStyle internal constructor(
    val material: SheetMaterial,
    val colors: AppSheetColors,
    /** All four corners. */
    val cornerRadius: Dp,
    /** The gap to the side and bottom edges at partial detents; 0 keeps the sheet attached. */
    val floatingInset: Dp,
    val maxWidth: Dp,
    /** Drawn outside the shape, only where §7.7 allows; null for none. */
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
    // equals and hashCode over every field
}

object AppSheetDefaults {
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)
    const val MediumFraction: Float = 0.5f          // sheet.detent.mediumFraction
    val LargeTopGap: Dp = 10.dp                      // sheet.detent.largeTopGap
    val GrabberSize: DpSize = DpSize(36.dp, 5.dp)    // sheet.grabber.width/height
    val GrabberTop: Dp = 5.dp
    val ContentTop: Dp = 15.dp                       // content start when there is no header
    val HeaderTop: Dp = 14.dp
    val HeaderMinHeight: Dp = 44.dp
    const val FormHeightFraction: Float = 0.85f
    val FormMaxHeight: Dp = 620.dp
    val SideInset: Dp = 16.dp
    const val GrabberAlpha: Float = 0.45f            // sheet.grabber.alpha

    val Grabber: @Composable AppSheetScope.() -> Unit = { AppSheetGrabber() }

    @Composable
    fun colors(
        container: Color = AppTheme.colors.surfaceElevated,
        content: Color = AppTheme.colors.contentPrimary,
        grabber: Color = AppTheme.colors.contentTertiary.copy(alpha = GrabberAlpha),
        rim: Color = solidRim(),
        scrim: Color = AppTheme.colors.scrim,
    ): AppSheetColors

    /** For inset-grouped content (AppSection): the grouped background, as iOS's grouped sheets. */
    @Composable
    fun groupedColors(): AppSheetColors

    /** White at sheet.rim.solidDarkAlpha (0.08) in dark theme; transparent in light, where the shadow or dim separates. */
    @Composable
    fun solidRim(): Color

    /** Soft in light theme; null in dark, where the rim shows the edge. */
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

- `AppStyle` (`ds/theme/AppDesignStyle.kt:154` [C]) gains `sheetRadius: Dp` (from `radii.sheet`, which exists:
  `tokens/styles.json:5`, `:22`, `:39`, `:56` [C], 24 / 30 / 8 / 28 dp), `sheetInset: Dp` and
  `sheetMaterial: SheetMaterial` from new `tokens/styles.json` fields (§6.2). `SheetMaterial.Automatic` resolves from
  `AppTheme.style.sheetMaterial` inside the sheet.
- Every value above is generated from the `sheet` token section (§6.2); none is private to a component file (API-6).
- `SheetMaterial` sits in `theme/` beside `BarTreatment` (`ds/theme/AppDesignStyle.kt:102` [C]), which is the same kind
  of design-style choice, so the theme never imports a component package. The sheet's uniform radius replaces
  `shapes.sheet`'s top-only rounding (`ds/theme/AppMetrics.kt:47` [C]) for sheets.

**Mapping to the Styles API** (`androidx.compose.foundation.style`, experimental in foundation 1.12.1 [V]
`fnd112/.../style/ExperimentalFoundationStyleApi.kt:23`). Not adopted now. When it is stable the sheet takes
`style: Style` and applies `Modifier.styleable(...)`:

| `AppSheetStyle` | `StyleScope` | Stays in the node |
|---|---|---|
| `colors.container` | `background(color)` | Frosted and Glass bodies |
| `colors.content` | `contentColor(color)` | — |
| `cornerRadius` | `shape(RoundedCornerShape(r))` | the floating clip (it follows `e`) |
| `shadow` | `dropShadow(shadow)` | the nine-slice and its outside-only cut (§7.7) |
| `maxWidth` | `maxWidth(dp)` | — |
| `floatingInset` | `externalPadding(…)` at rest | the morph |
| `material`, `colors.scrim`, `colors.grabber`, `colors.rim` | none | other nodes |

### 4.8 Scope, slots and content-side API

Files `component/sheet/AppSheetScope.kt`, `chrome/`, `content/`.

```kotlin
/** What a sheet's slots and content can reach. */
@Stable
interface AppSheetScope {
    val sheetState: AppSheetState

    /**
     * Padding for the content's scroller: the footer, the navigation bar or the keyboard, and with
     * Scrolls the part of the content below the visible bottom at the settled detent, each counted
     * once. Backed by snapshot state and read in measure: a change re-measures the scroller and never
     * recomposes it. It changes only when the sheet settles, the footer's height changes, or the
     * keyboard's target changes; never during a drag.
     */
    val contentPadding: PaddingValues

    /** How far the content has scrolled under the header, 0 to 1 over sheet.header.scrollEdgeRamp. Read in draw. */
    @get:FrequentlyChangingValue
    val contentScrolled: Float

    /** True while another sheet is stacked on this one. */
    val isCovered: Boolean

    /** Closes the sheet as the scrim would: a SheetDismissGuard or dismissible = false turns it into an attempt. */
    fun requestDismiss()

    /** Closes the sheet without asking: a Done button after the work is saved. Calls onDismissRequest. */
    fun dismiss()
}

/** The grabber: tap, Enter or Space steps through the detents; screen readers get expand, collapse and close. */
@Composable
fun AppSheetGrabber(modifier: Modifier = Modifier)

/**
 * A sheet's title row: [title] centred between [leading] and [trailing] (Cancel, Done, close), never
 * under them: the wider side's width is reserved on both sides. At font scale 1.5 and above the title
 * takes up to two lines and the row grows. It names the sheet for TalkBack. Outside a sheet it draws
 * in place, without the scroll-edge fade.
 */
@Composable
fun AppSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = { AppSheetCloseButton() },
)

@Composable
fun AppSheetHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = { AppSheetCloseButton() },
)

/**
 * Asks the enclosing sheet to close (guarded). Absent outside a sheet, so a screen shown both as a
 * sheet and full screen can keep it.
 */
@Composable
fun AppSheetCloseButton(modifier: Modifier = Modifier)

/** The enclosing sheet's contentPadding; zero outside a sheet. */
@Composable
fun sheetContentPadding(): PaddingValues

/** A scrolling column with the sheet's contentPadding applied inside the scroll: a form, a settings group. */
@Composable
fun SheetColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = sheetContentPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
)

/** Puts [content] in the enclosing sheet's header slot, replacing `header`. Outside a sheet it draws in place. */
@Composable
fun ProvideSheetHeader(content: @Composable () -> Unit)

/** Puts [content] in the enclosing sheet's footer slot, pinned to its visible bottom. In place outside a sheet. */
@Composable
fun ProvideSheetFooter(content: @Composable () -> Unit)

/**
 * Takes Back (the key, a committed gesture, Escape) inside the enclosing sheet while [enabled],
 * instead of closing it: a step back in a flow the sheet holds. The sheet does not shrink for it.
 */
@Composable
fun SheetBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * Keeps the enclosing sheet up while [enabled]: a drag down rubber-bands and springs back; the dim,
 * Back, Escape and the close button call [onAttempt] instead, once per attempt. Enable it only while
 * there is something to lose, so an untouched form keeps the system's predictive back. Closing from
 * code is not guarded. Outside a sheet it does nothing.
 */
@Composable
fun SheetDismissGuard(enabled: Boolean = true, onAttempt: (SheetDismissSource) -> Unit)

/**
 * The sheet a composable is drawn in, or null outside one. AppScaffold lets the sheet's material show
 * instead of painting its own background; AppTopBar pads no status bar and shows a close button where
 * it would show Back at the root; AppBottomActionBar paints no background and pads no inset.
 */
val LocalSheetSurface: ProvidableCompositionLocal<SheetSurface?>

@Stable
class SheetSurface internal constructor(val material: SheetMaterial, val container: Color, val scope: AppSheetScope)
```

Slot rules:

- **Grabber.** Drawn over the top band; takes no layout room. The default (`AppSheetDefaults.Grabber`) is shown when
  the sheet has two or more active detents, and hidden for Center placement, while covered (iOS
  `prefersGrabberVisible` [A]) and when only one detent is active. When no grabber is drawn, the sheet root carries the
  grabber's accessibility actions (§9).
- **Header.** Measured once, placed above the content box, never scrolled, laid out inside a constant `i₀` side inset
  (D-7) so its actions never meet a rounded corner while the sheet floats.
- **Footer.** Measured separately and placed at the visible bottom (B-45); drawn over the content's bottom. The footer
  slot consumes `navigationBars ∪ ime`, so an `AppBottomActionBar` in it pads nothing twice (fixes D2,
  `AppBottomActionBar.kt:38-39` [C]); the sheet places the footer above the navigation area or on the keyboard.
- **Content.** `AppSheetScope` receiver, no layout scope. Measured at the sheet's full width.
- **Content-side providers** register through an internal `LocalSheetSlots` in a `DisposableEffect`; the innermost
  enabled one wins. They are how a destination's screen, composed by Navigation 3, customises its sheet.
  **Registration lands before the first visible frame** (B-2).

### 4.9 Presets

Files `component/sheet/action/`, `choice/`.

```kotlin
/**
 * "What would you like to do with this?": actions in a sheet that fits them, with Cancel apart. At
 * Medium width and up it is a card anchored to [anchor], without Cancel; with no anchor it is centred.
 */
@Composable
fun AppActionSheet(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String = stringResource(R.string.designsystem_cancel),
    anchor: SheetAnchor? = null,
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

/** The bounds an anchored card points at, recorded in placement without snapshot writes. */
@Stable
class SheetAnchor

@Composable
fun rememberSheetAnchor(): SheetAnchor

fun Modifier.sheetAnchor(anchor: SheetAnchor): Modifier

/**
 * One choice from a list, the current one checked: a language, a unit. Rows have the radio role; a
 * search field appears from AppChoiceSheetDefaults.SearchFrom choices; [noneLabel] adds a row that
 * selects nothing. Choosing calls onSelect, then closes the sheet.
 */
@Composable
fun <T : Any> AppChoiceSheet(
    visible: Boolean,
    choices: List<T>,
    selected: T?,
    onSelect: (T?) -> Unit,
    onDismissRequest: () -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    title: String? = null,
    noneLabel: String? = null,
    key: (T) -> Any = { it },
)

object AppChoiceSheetDefaults {
    const val SearchFrom: Int = 6           // sheet.choice.searchFrom (VitFleet ChoiceSheet)
    const val MaxFitFraction: Float = 0.6f  // sheet.choice.maxFitFraction
}
```

- **`AppActionSheet`, Compact width:** an in-screen `AppSheet` with one stop
  `Custom("action-fit") { minOf(contentHeight, maxHeight * 0.6f) }` (`sheet.actionSheet.maxFitFraction`), `Scrolls`,
  two `surfaceElevated` groups (actions, then Cancel), rows of 56 dp (`sheet.actionSheet.rowHeight`), arriving on
  `springs.dialog` (M17). It scrolls when taller than the cap (fixes A1), keeps consuming Back during its exit (A3),
  sets its pane title (A5). An action runs before the sheet asks to close (`AppActionSheet.kt:138-141` [C], kept).
- **`AppActionSheet`, Medium width and up (D-21):** a card in the overlay host, `sheet.actionSheet.cardWidth` (320 dp)
  wide, placed below the anchor's bounds (above when there is no room), aligned to its start edge, clamped 8 dp inside
  the window. It scales 0.94 → 1 from the anchor's nearest edge with a `ModulateAlpha` fade on `springs.dialog`
  (card-size layer). No dim; pointer input outside the card is consumed while it is up, and a tap there closes it; Esc
  and Back close it. Without an anchor it is a centred card over the dim. Shadow per §7.7. Never a `Popup` window.
- **`AppChoiceSheet`:** detents `[Custom("choice-fit") { minOf(contentHeight, maxHeight * 0.6f) }, Large]`, so a long
  list opens at 60 % and grows (VitFleet `ChoiceSheet.kt:34-114`, `current-state.md` §11). Multiple selection is a
  recipe built from `AppSheet` (WP12), not a flag.
- A persistent (Maps-style) sheet is not a separate component: it is `AppSheet` with `NonModal(upThrough)`,
  `dismissible = false`, and `Automatic` content interaction (§4.1).
- Call sites `remember` their action list (A5: a list rebuilt on every recomposition defeats skipping).

### 4.10 Seams (public, not for features)

```kotlin
// component/sheet/stack/SheetStack.kt (WP03 writes, WP04 applies)
/**
 * The sheets up in a window, for whatever recedes behind them. The outermost AppTheme installs one;
 * every sheet registers itself with its level (§3.2). Level 0 is the shell.
 */
@Stable
class SheetStack internal constructor() {
    /** How far the presenter at [level] has receded: 0 upright, 1 behind a sheet at Large. Read in a layer block. */
    @FrequentlyChangingValue
    fun recede(level: Int): Float

    /** The merged dim to draw over the presenter at [level], alpha included (§6.4). Read in draw. */
    @FrequentlyChangingValue
    fun dim(level: Int): Color

    /** True while the shell has receded past sheet.cardStack.statusBarFlip. Changes once per crossing. */
    val rootReceded: Boolean

    /** The front sheet's settled bottom chrome, for toasts. */
    val topBottomChrome: Dp
}

val LocalSheetStack: ProvidableCompositionLocal<SheetStack?> = staticCompositionLocalOf { null }

/**
 * Recedes this layout behind the sheet above [level], as iOS's card stack does, and draws that
 * sheet's dim over it. One layer (scale, translation, outline clip) and one rect, all read in layer
 * and draw blocks: the presenter never recomposes while the sheet moves.
 */
fun Modifier.cardStackPresenter(stack: SheetStack, level: Int = 0): Modifier

// foundation/layout/ (WP03 declares with no-op defaults; WP04 provides)
/** Where a component that moves its host's content says so, so the host's Back gate counts presses meanwhile. */
fun interface HostMotion { fun report(mover: Any, moving: Boolean) }
val LocalHostMotion: ProvidableCompositionLocal<HostMotion> = staticCompositionLocalOf { HostMotion { _, _ -> } }

/** Whether this navigation entry is uncovered: the top of its stack and nothing presented above its host. */
@Stable
class EntryVisibility(private val uncovered: () -> Boolean) { val isUncovered: Boolean get() = uncovered() }
val LocalEntryVisibility: ProvidableCompositionLocal<EntryVisibility> = staticCompositionLocalOf { EntryVisibility { true } }

/** The window-space bounds of the pane this content is in; null for the whole window. */
@Stable
class PaneBounds(private val bounds: () -> Rect?) { val rect: Rect? get() = bounds() }
val LocalPaneBounds: ProvidableCompositionLocal<PaneBounds> = staticCompositionLocalOf { PaneBounds { null } }

/** The presentation depth of this content: 0 in the shell, d in the d-th presented entry. */
val LocalPresentationLevel: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }

// component/sheet/surface/SheetBackdrop.kt (WP03 declares; WP05 implements)
/** The sheet's visible frame in its own coordinates. Getters are read in draw and allocate nothing. */
interface SheetFrame {
    @get:FrequentlyChangingValue val left: Float
    @get:FrequentlyChangingValue val top: Float
    @get:FrequentlyChangingValue val right: Float
    @get:FrequentlyChangingValue val bottom: Float
    val cornerRadius: Float
}

/**
 * Draws a Glass sheet's live body. The liquid glass feature provides it around the two hosts while
 * the tier samples; everywhere else it is absent and a Glass sheet draws Frosted.
 */
@Stable
interface SheetBackdrop {
    /**
     * The scene behind [frame], blurred, under [dim] (the presenter's dim, which the recorded scene does
     * not contain) and a tint whose opacity is [opaque] (0 glass, 1 opaque). Both are read in draw;
     * the sampling pass is skipped while [opaque] is 1.
     */
    fun Modifier.sheetBackdrop(frame: SheetFrame, opaque: () -> Float, dim: () -> Color): Modifier
}

val LocalSheetBackdrop: ProvidableCompositionLocal<SheetBackdrop?> = staticCompositionLocalOf { null }
```

`SheetStack` replaces `largeFraction` on `AppSheetState` and on `SheetSceneStrategy` (N10): the shell no longer casts a
strategy, and in-screen sheets reach the shell too.

### 4.11 Navigation API (WP04, `:core:navigation`)

```kotlin
// Presentation.kt, replacing nav4/Presentation.kt:8-24
sealed interface Presentation {
    /** A sheet over the current screen, which stays composed and visible beneath it. Always modal. */
    data class Sheet(val spec: SheetSpec = SheetSpec()) : Presentation

    data object Cover : Presentation
}

/** How a destination presented as a sheet rests and leaves. Its look beyond the material comes from the theme. */
@Immutable
data class SheetSpec(
    val detents: List<SheetDetent> = AppSheetDefaults.Detents,
    val initialDetent: SheetDetent = detents.first(),
    val dismissible: Boolean = true,
    val material: SheetMaterial = SheetMaterial.Automatic,
    val properties: AppSheetProperties = AppSheetProperties(),
)

internal object PresentationKey : NavMetadataKey<Presentation>

/**
 * Steps of a flow that stay inside a sheet: a nested stack with its own push and pop, clipped to the
 * sheet. Back pops this stack first and closes the sheet from its root. Each step can set the sheet's
 * header and footer with ProvideSheetHeader and ProvideSheetFooter. Outside a sheet it is an ordinary
 * single-pane host.
 */
@Composable
fun AppSheetNavHost(
    backStack: NavBackStack<AppNavKey>,
    modifier: Modifier = Modifier,
    entryProvider: (AppNavKey) -> NavEntry<AppNavKey>,
)
```

- `entry<K>(presentation = Presentation.Sheet(SheetSpec(...)))` stays the only way a feature declares a sheet
  destination (`nav4/NavGraph.kt:50-57` [C]).
- `SheetDetent` is the design system's. WP04's enum (`nav4/Presentation.kt:18` [C]), WP03's `SheetPresentation`
  (`wt/WP03/.../core/navigation/SheetScene.kt:24-28`) and WP07's copy are deleted (N3).
- The metadata travels under `PresentationKey` (`NavMetadataKey` exists in navigation3 1.1.7 [V] judge-3 §5, and in
  1.2.0). The string key `PRESENTATION_METADATA` and its `as?` read go (`nav4/presentation/PresentationHost.kt:199-205`
  [C]).
- A destination's header and footer come from its own screen (`ProvideSheetHeader`/`ProvideSheetFooter`), its confirm
  hook from `SheetDismissGuard`. No styling travels through metadata.
- A feature reaches its destination sheet's state through `LocalSheetSurface.current?.scope?.sheetState`, for example to
  expand it from a button.

### 4.12 Not in the API

| Left out | Why |
|---|---|
| A `Dialog`- or `Popup`-window sheet, for any preset | D-22 |
| `confirmValueChange` | Deprecated in foundation; the anchor set and `SheetDismissGuard` replace it |
| Per-path dismissal switches (drag, Back) and `SheetBackBehavior.StepDown` | `dismissible`, the guard and `scrimTapDismisses` cover the real uses; D-12 |
| `onDismissAttempt` on the component | D-11 |
| A `title: String` overload | `header = { AppSheetHeader(title) }` is one line |
| Wrapping content in a scroll container | D-25 |
| A per-detent background effect (iOS 26.1 `Detent.backgroundEffect`) | The `e` ramp covers the shipped behaviour |
| `Modifier.sheetBleed()` | D-7 |
| `SheetTransition.Zoom`, `Modifier.sheetSource` | Phase 2 (§20) |
| Concentric bottom corners | A non-uniform round rect becomes a path clipped on the canvas below API 33 [V] (`OutlineResolver.android.kt:263`, `:282-288`); minSdk is 26. Phase 2 with a recording |
| `NavResultKey`, `popWithResult`, `NavResultEffect` | D-32 |
| A public `@JvmInline value class` detent | Boxed every time it is used as the sealed type |
| Multiple selection in `AppChoiceSheet` | A different structure (Done button, no close on select): a recipe |

---

## 5. Behaviour

Rules are numbered so tests and reviews can cite them (`B-22 violated`). `v` is a velocity in px/s, positive
downward. "The person" means a touch, a key or an accessibility action; "code" means `AppSheetState` calls, `visible`,
`item` and pops.

### 5.1 Lifecycle

- **B-1 First frame.** A sheet is composed, measured and placed at the hidden offset `h` on the frame it is composed,
  and drawn in that pose (API-13). No `LaunchedEffect` sets up initial state.
- **B-2 Show.** `show()` waits for (a) the first layout with anchors and (b) any slot registration made by the content
  in its first composition (`ProvideSheetHeader`, `ProvideSheetFooter`) to have been composed and measured, so
  `FitContent` includes them. It then starts the spring on the next frame (the clock starts after composition,
  SMOOTHNESS S3): `h → initial detent` on `springs.sheet` (÷ speed). Presets use `springs.dialog` (M17). The first
  moving frame shows ≤ 4 % of the travel (CP1 §1 condition 1). Implementation: the slot registry carries a `pending`
  flag set by a registration and cleared by a `SideEffect` in the slot host; `show()` awaits `anchors != null &&
  !pending`.
- **B-3 Hide.** Idempotent: a second `hide()` joins the running one. `show()` during `hide()` turns the sheet around from
  its offset with `lastVelocity` (fixes D1, D7).
- **B-4 The `visible` overload.** One `AppSheetState` per call site (not one per opening, as WP03 did at
  `ds/component/overlay/AppSheet.kt:273-286` [C]). `LaunchedEffect(visible) { if (visible) state.show() else
  state.hide() }`. The sheet stays composed while `visible || state.isVisible`. After the person closes it, `visible`
  stays true until the caller clears it in `onDismissRequest`; meanwhile the sheet is hidden, not drawn, not hit-tested
  and not in the `SheetStack`, so it costs nothing and cannot be stuck half-way (D1 cannot happen).
- **B-5 The `item` overload.** A non-null `item` shows the sheet; null hides it, and the last item stays composed
  during the exit. A different non-null item while one is shown: `hide()`, then `show()` with the new item.
- **B-6 Restored.** A state restored with `shown = 1` snaps to its detent with no slide; if that detent is Large, the
  presenter is receded from the first frame.
- **B-7 A dismissal by the person** (B-33) is decided at the release, the tap, Back's commit or the key. Then, in the
  same frame: `onDismissRequest()` runs once, synchronously, and the exit starts, seeded with the release velocity (0
  for a tap or a key). The sheet leaves by itself; the owner only removes it.
- **B-8 A dismissal by code** (`hide()`, `visible = false`, `item = null`, a pop) calls no `onDismissRequest` and asks
  no guard.
- **B-9 `animateTo`, `expand`, `collapse`** retarget with the current velocity on `springs.sheet`. They are ignored while
  the sheet leaves (fixes D9) and while it is parked. `snapTo` is for restoration and tests.
- **B-10 Resize and content changes.** Rotation, unfold, freeform resize, a font-scale change or a content-height change
  re-resolve the stops in measure (R-11). The detent's identity is kept; an animation in flight is retargeted (fixes
  D10). A sheet resting on `FitContent` whose content height changed animates to the new height on `springs.sheet`. A
  placement change (Bottom ↔ Center ↔ Start/End) snaps to the new frame at the same detent, with no animation.
- **B-11 `updateDetents`.** A sheet resting on a detent that stays active stays. Otherwise the layout node launches
  `animateTo(nearest active detent by height)`.

### 5.2 Hosting and parking

- **B-12 Hosts.** `Presentation.Sheet` destinations render in the presentation host (§11.2). `AppSheet(visible)` and
  `AppSheet(item)` render through `HostedOverlay` in the overlay host (`ds/component/overlay/AppOverlayHost.kt:81-100`
  [C]), keeping the caller's composition locals, whatever their modality. Both register in `LocalSheetStack` with
  their level (§3.2).
- **B-13 Bounds and class.** A modal sheet's bounds are the window and its placement reads the **window** size class. A
  non-modal sheet's bounds are `LocalPaneBounds` (the window when null) and its placement reads the caller's container
  class.
- **B-14 Parking.** An in-screen sheet parks while its owner is not visible, and keeps its logical state (`isVisible`,
  `currentDetent`, the app's `visible`):
  - **owner's tab hidden** (`LocalTabVisible`, `ds4/foundation/layout/Chrome.kt:48-52` [C]): parks and returns
    instantly, with no animation (fixes N7);
  - **owner covered** (`LocalEntryVisibility.isUncovered` false: a push in its own stack, from the push's first frame, or
    a presentation above its host): slides to hidden on `springs.sheet` and slides back to its detent when uncovered
    (judge-3 G23);
  - while parked: not drawn, not hit-tested, semantics cleared, out of the `SheetStack` (the presenter un-recedes in step
    during the park animation, because the stack reads the parked fractions), Back not registered, no frame-rate vote;
  - the park is a separate `Animatable` (`parkProgress`, 0 to 1) added to the placement as
    `parkProgress · (h − y)`, so the anchored state and its detent are untouched (L1);
  - a sheet that is already leaving is not parked: it finishes its exit drawn.
- **B-15 Host motion.** Every sheet reports `LocalHostMotion.current.report(state, moving)` from an effect over
  `snapshotFlow`, where `moving` is true while a show, hide, park or unpark animation runs and more than 1 % of its
  travel remains (`navigation.backGateRelease` = 0.99, `nav4/back/BackGate.kt:108` [C]). Drags and settles are not
  reported. `report(state, false)` always runs before the owner removes the sheet. WP04 provides `HostMotion` per host
  (§11.3), so `NavGuard` holds a push while a sheet rises or leaves (`nav4/back/NavGuard.kt:15-24` [C]; fixes N4(c)).

### 5.3 Gestures

- **B-16 Drag.** Anywhere on the surface: grabber, header, footer and the content's background. The sheet layer
  carries `anchoredDraggable(state, Vertical, enabled = !leaving && !parked && placement allows it,
  overscrollEffect = SheetStretch, flingBehavior = SheetFlingBehavior)`; `startDragImmediately = isAnimationRunning`
  (`src/foundation-1.12.1/.../AnchoredDraggable.kt:479` [G]). The finger tracks 1:1 (F1).
- **B-17 Catch.** An `Initial`-pass pointer node on the sheet layer stops any running show or settle on a finger down
  anywhere on the sheet, content included, through `anchoredDrag(MutatePriority.PreventUserInput) {}` (§4.4; fixes D8;
  judge-1 graft 4). It is skipped while the sheet leaves or is parked, and it consumes nothing. If that down stopped motion and the pointer never crosses touch slop, the node
  consumes the matching up, so a tap that stops a moving sheet does not also activate the row under it.
- **B-18** A leaving sheet ignores touches (the person committed; code decided).
- **B-19 Grabber activation** (tap, Enter, Space, TalkBack double tap): the next taller active detent; from the
  tallest, the lowest. It never dismisses. The `Threshold` haptic plays on arrival (B-61).
- **B-20 Pointer.** A mouse or trackpad drags the sheet like a finger. The wheel is handled by B-27. The grabber shows a
  hover state and `PointerIcon.Hand` (ADAPTIVE L-5).
- **B-21 Press feedback** on rows inside a sheet appears within 100 ms of the down despite Compose's delayed presses in
  drag containers (`isDelayPressesUsingGestureConsumptionEnabled`, `research.md` §2.6) [U: tested in
  `SheetGestureTest`].

### 5.4 Choosing a stop on release

- **B-22 Release rule.** With release offset `y`, velocity `v`, projection `p = y + T · v` (`T` =
  `sheet.release.projectionSeconds` = 0.099 s, iOS's 0.99/ms deceleration), detent flick `F` =
  `sheet.release.detentFlick` (400 dp/s) and dismiss flick `D` = `sheet.release.dismissFlick · H` per second (2 H/s):
  1. `y < o_1` (over-pulled above the top detent) → `o_1`.
  2. `o_1 ≤ y ≤ o_n` (between the top and the lowest detent, inclusive):
     - `|v| ≥ F` and a detent lies strictly beyond `y` in the direction of `v` → the nearest such detent (one stop per
       flick, never hidden);
     - `|v| ≥ F` upward and no detent lies above `y` → `o_1`;
     - `v ≥ F` downward and no detent lies below `y` (the sheet is at its lowest detent) → rule 3's test;
     - otherwise → the detent nearest `p`, clamped to `[o_1, o_n]`.
  3. `y > o_n` (below the lowest detent), or sent here by rule 2:
     - `−v ≥ F` (upward) → `o_n`;
     - dismissal allowed → hidden if `v ≥ D` or `(p − o_n) / (h − o_n) > sheet.release.dismissPosition` (0.52); else
       `o_n`;
     - dismissal blocked → `o_n`; if the allowed rule would have dismissed, the innermost enabled guard's
       `onAttempt(Drag)` runs once for this gesture.

  Worked on the Redmi (Medium top 436 dp, Large top 43 dp, `h` 873 dp):

  | Release | Rule | Lands |
  |---|---|---|
  | At Medium, 300 dp/s up | 2, projection 436 − 30 = 406 | Medium |
  | At Medium, 600 dp/s up | 2, flick | Large |
  | 60 dp above Medium, 200 dp/s down | 2, projection 376 + 20 = 396 | Medium |
  | At Large, 1200 dp/s down, `[Medium, Large]` | 2, flick | Medium |
  | At Large, 3000 dp/s down, `[Medium, Large]` | 2, flick (one stop) | Medium |
  | At Large, 1800 dp/s down, `[Large]` | 2 → 3, ≥ 2 H/s | hidden |
  | At Medium, 1200 dp/s down | 2 → 3, (436 + 119 − 436) / 437 = 0.27 | Medium |
  | At Medium, 1800 dp/s down | 2 → 3, ≥ 2 H/s | hidden |
  | 250 dp below Medium, still | 3, 250 / 437 = 0.57 > 0.52 | hidden |

  This replaces WP03's projection-only rule (which needs about 4.6 H/s to dismiss from Large, `research.md` §1.9) and
  foundation's 125 dp/s default (`AnchoredDraggable.kt:1551-1566` [G]), which M3 found reads small scroll flings as
  dismissals (`5ab9549e`). Whether iOS skips Medium on a very hard flick from Large is [U]; one stop per flick is
  predictable and matches androidx's `computeTarget` (`AnchoredDraggable.kt:1483-1514` [G]).
- **B-23 Settle.** On `springs.sheet` (÷ speed), seeded with the release velocity.

### 5.5 Rubber band

The formula is iOS's, shared with overscroll and pull to refresh: `shown = c · x · d / (d + c · x)`, `c` =
`scroll.rubberBand` = 0.55 (`ds/theme/AppMotion.kt:182-186` [C]).

- **B-24 Above the top detent:** `d = o_1 − (largeTop − sheet.stretch.aboveLarge · H)`. At Large that is 0.008 H
  (7.0 dp on the Redmi: a 200 dp over-pull shows 6.6 dp) [M] `sheet.dart:27-37`. A Medium-only sheet gets the room up
  to Large's stretch line (≈ 400 dp: a 200 dp pull shows 86 dp).
- **B-25 Below the lowest detent while dismissal is blocked:** `d = h − o_n` (from Medium, 437 dp: a 300 dp pull shows
  120 dp).
- **B-26 Release from a stretch:** `springs.sheet` with the release velocity times the band's slope at release. A
  settle that overshoots the top detent passes through the same band, so the visible overshoot never exceeds 0.008 H
  above `largeTop`. The surface stretches with its top edge; its bottom edge stays put, so a floating sheet never shows
  a gap below itself.
- Implemented as a custom `OverscrollEffect` (`SheetStretch`) given to `anchoredDraggable(overscrollEffect = …)`,
  whose excess the layout reads in placement (`AnchoredDraggable.kt:410-477` [G]).

### 5.6 Nested scroll and reaching the last item

- **B-27 `Automatic` resolution.** `Resizes` for a modal sheet; `Scrolls` for a non-modal sheet, and for any sheet in a
  window with only a mouse or trackpad (`AppTheme.input.finePointer && !AppTheme.input.touchscreen`,
  `ds/foundation/adaptive/InputTraits.kt:11-15` [C]). A mouse wheel reports `NestedScrollSource.UserInput`
  ([V] `NestedScrollModifier.kt:294`), so `Resizes` would grow the sheet on the first notch.
- **B-28 `Resizes`** (iOS `prefersScrollingExpandsWhenScrolledToEdge = true` [A]): `onPreScroll` gives an upward
  `UserInput` delta to the sheet until it reaches its top detent (`dispatchRawDelta`, a state write that recomposes
  nothing); `onPostScroll` gives a downward `UserInput` leftover at the content's top to the sheet (down to hidden, or
  into the B-25 band); `onPreFling` settles a sheet that is not resting with the full velocity (B-22) and consumes it.
- **B-29 `Scrolls`**: upward deltas go to the content first and never grow the sheet; the downward leftover at the
  content's top still drags it. The grabber, header and footer still resize it ("People can always resize your
  presentation using the drag indicator" [A]).
- **B-30** Momentum never moves the sheet: deltas whose source is not `UserInput` are ignored, so a fling that reaches
  the top bounces in the list's own overscroll (`ds/component/overlay/AppSheet.kt:160-161` [C], kept). An
  accessibility scroll is not `UserInput`, so TalkBack scrolling never resizes the sheet; the grabber's Expand action
  does.
- **B-31 Reachability with `Scrolls`.** The content is measured at Large's height, so at a partial detent part of it is
  below the visible bottom. `contentPadding.bottom` adds `y_settled − largeTop` at each settle (≤ 1 re-measure per
  settle, never during a drag), so the last row can scroll into view at Medium.
- **B-32 `contentScrolled`** comes from the same connection: it rises with upward scroll the content consumes and
  returns to 0 when the content hands back leftover at its top. A programmatic jump (`scrollToItem`) can leave it stale
  until the next touch scroll [D]; acceptable for a fade.

### 5.7 Dismissal and the confirm hook

- **B-33 Ways out.**

| Path | No guard | Guard enabled | `dismissible = false`, modal | `dismissible = false`, non-modal (persistent) |
|---|---|---|---|---|
| Drag or flick down | B-22 rule 3 | rubber band (B-25); `onAttempt(Drag)` once if rule 3 would have closed it | rubber band; attempt to a guard if any | rubber band; attempt to a guard if any |
| Tap on the dim (`scrimTapDismisses`) | closes | `onAttempt(Scrim)` | attempt to a guard if any; else consumed | no dim at undimmed detents; above them as modal |
| Back gesture | predictive dismiss (B-38) | resisted shrink, commit springs back, `onAttempt(Back)` | resisted shrink, commit springs back, attempt to a guard if any | steps down one detent; at the lowest the handler is disabled and Back reaches the screen (B-37) |
| Back key | closes | `onAttempt(Back)` | consumed; attempt to a guard if any | as the gesture, without preview |
| Escape | closes | `onAttempt(Escape)` | as the Back key | as the Back key |
| `requestDismiss()` (close button) | closes | `onAttempt(Request)` | attempt to a guard if any | attempt to a guard if any |
| `scope.dismiss()` (Done after save) | closes | closes | closes | closes |
| Code (`hide()`, `visible = false`, pop) | closes | closes | closes | closes |

- **B-34 Guard semantics.** Dismissal is blocked when `dismissible = false` or an enabled guard is registered. Attempts
  go to the innermost enabled guard, once per gesture or press: a drag's on release, the dim's, Escape's and
  `requestDismiss`'s at once, the Back gesture's on commit. No haptic on an attempt: the confirmation the app shows is
  the feedback. A guard with `enabled = false` registers nothing, so a clean form keeps the system's predictive back
  (one handler that branches, navigation-event skill `SKILL.md:255-266` [G]; VitFleet `LoadFormScreen.kt:128`).
- **B-35 Callbacks.** `onDismissRequest` runs for every path in the first two columns that closes the sheet, including
  `scope.dismiss()`; never for code paths.

### 5.8 Back, predictive back and Escape

- **B-36 Registration.** One `NavigationEventHandler` per sheet at `PRIORITY_OVERLAY` on the dispatcher of the
  composition that calls `AppSheet` (replacing `ds/component/overlay/OverlayBackHandler.kt:27-51` [C]), one state per
  handler, branching in `onBackCompleted` (navigation-event skill). Verified ordering [V] (navigationevent 1.1.2;
  1.2.0 bytecode keeps the same deques): overlay handlers anywhere in the tree are asked before default ones, last
  registered first (`NavigationEventProcessor.kt:272`, `:579`); a handler is off while its dispatcher or an ancestor is
  off (`NavigationEventDispatcher.kt:169-170`, `NavigationEventHandler.kt:126`). So a sheet in a hidden kept tab is
  deaf, and a sheet's handler outranks the shell's.
- **B-37 Enablement.** Registration order is not trusted. The handler's `isBackEnabled` is true only when all hold:
  1. the sheet is the top-most visible sheet in the `SheetStack`, not covered and not parked;
  2. the IME **target** is hidden (B-44);
  3. a destination sheet is not leaving (its extra presses go to the host gate, B-43);
  4. a way out exists: always true for a modal sheet (Back on a blocked modal sheet is a consumed attempt, never a
     pass-through); for a persistent non-modal sheet, true only above its lowest detent.

  Each input is a snapshot value that changes rarely (the IME target is written from placement only when it changes);
  an effect collects them with `snapshotFlow` and sets `isBackEnabled`. None is read in composition, so nothing
  recomposes. A persistent sheet at its lowest detent leaves `isBackEnabled` false, so the system still plays
  back-to-home at a tab root.
- **B-38 Branches**, in order:
  1. a content `SheetBackHandler` is enabled → it handles Back; no shrink;
  2. a persistent sheet (non-modal, `dismissible = false`) above its lowest detent → the preview moves the sheet down by
     `sheet.back.persistentFollow · p` (24 dp), no shrink; the commit runs `collapse()` (one detent per Back [U: against
     Google Maps, §21]);
  3. dismissal is blocked (an enabled guard, or `dismissible = false` on a modal sheet) → resisted shrink
     (`p · sheet.back.guardedShare`, 0.2); the commit springs back on `springs.sheet` and fires the attempt;
  4. otherwise → predictive dismiss.
- **B-39 Predictive progress and pose.** `p = CubicBezierEasing(0.1, 0.1, 0, 1).transform(event.progress)`
  (`src/BackHandler.kt:47-51` [G]), held in a float state read only in layer blocks (material3 reads it in composition,
  `src/BottomSheet.kt:168`: not copied). MDC bottom container (`research/predictive-back/SPEC.md:79` [G]):
  - Bottom placement: sheet layer `scaleX = 1 − min(48 dp, w) · p / w`, `scaleY = 1 − min(24 dp, hv) · p / hv`, `hv` the
    visible height, `transformOrigin` at the visible bottom centre; the content layer counter-scales
    `scaleY' = scaleX / scaleY` about its top so text keeps its aspect ratio (fixes D6; `src/BottomSheet.kt:475-480`).
  - Center placement: uniform scale `1 − (1 − sheet.back.formScale) · p` (to 0.90) about the card's centre.
  - Start/End placement: as Bottom, about the panel's visible bottom centre.
  - The dim does not change during the gesture; it follows `pf` once the exit starts. The presenter does not move during
    the gesture.
- **B-40 Commit and cancel.** Commit: the exit slides the shrunken sheet to hidden on `springs.sheet` while the scale is
  held, then resets it (MDC, `research.md` §2.5). One gesture closes the sheet from any detent. Cancel: the system
  springs the progress back before `onBackCancelled` (`predictive-back/SPEC.md:77`); any residual returns to 0 on
  `springs.sheet`.
- **B-41** The Back key, 3-button navigation and API < 34 have no progress: no shrink, straight to the commit.
- **B-42 Escape.** `onPreviewKeyEvent` on the sheet root: `Key.Escape` key-down runs the Back key's path (fixes D16,
  `ADAPTIVE-PLAN.md:565`). A modal sheet traps focus (§9), so the key always reaches it.
- **B-43 Rapid Back during a dismissal** (decides N6):
  - **Destination sheet.** The commit calls `onDismissRequest` (B-7), which is the host's `gate.onBack(pop)`
    (`nav4/presentation/PresentationHost.kt:122` [C]): the pop happens while the gate is idle. `onRemove()` then runs the
    slide, which the sheet reports as host motion (B-15), and the sheet's handler is off while it leaves (B-37). Further
    presses reach the presentation host's `StackBackHandler` (enabled while `canGoBack || gate.moving`,
    `nav4/back/StackBackHandler.kt:67` [C]), are counted (`BackGate.kt:51-57`) and applied as one pop at 99 % of the
    exit (`BackGate.kt:108`); they pop further presented entries and are dropped at the presentation root, so they never
    reach the tab beneath. MASTER-PLAN:911 holds.
  - **In-screen sheet.** No stack owns it. Its handler stays enabled while it leaves and consumes and drops Back
    (WP03's tested behaviour, `SheetTest.kt:65-79` [C]). A burst on a sheet never goes past the sheet's presenter.
- **B-44 Back with the keyboard.** While the IME target is visible
  (`WindowInsetsRulers.Ime.getAnimation(scope).isVisible`/`target`, read in placement), the handler is disabled, so the
  IME takes Back and closes. From the frame the target becomes hidden, the handler is enabled again: a second Back
  during the keyboard's closing animation closes the sheet, never the screen beneath a modal sheet (judge-2 §2.3). The
  ordering of the IME window's callback against `PRIORITY_OVERLAY` is [U]; this rule holds either way. QA2 confirms on
  the phone.

### 5.9 Keyboard and insets

- **B-45 `Lift` (default).** In the placement block (API 30+), with `K` the IME inset read from
  `WindowInsetsRulers.Ime` ([V] `WindowInsetsRulers.kt:48`, `:97`) and `bottomInset = H − restBottom` (the navigation
  bar, or the bottom chrome for a sheet resting on it): `lift = clamp(K − bottomInset, 0, y − largeTop)`; the sheet is placed at `y − lift`, so a Medium sheet rises with the
  keyboard frame by frame on the system's curve, keeps its height while it fits, and stops at `largeTop`. The footer's
  top is `max(min(restBottom, surfaceBottom, H − K) − footerHeight, y_v + headerBottom)`, with `surfaceBottom` from
  §6.3 and `y_v` the placed top: above the navigation area or the floating gap, on the keyboard when it is up, and
  never above the header's bottom (it rides the header out when the sheet leaves, as `AppSheet.kt:249-250` [C] does). Detent anchors exclude the
  keyboard, so drag math, `currentDetent` and `e` are unchanged. L1, no recomposition.
- **B-46 `Expand`.** When the IME target becomes visible the sheet animates to its tallest active detent on
  `springs.sheet`; when it hides, back to the detent it left, unless the person moved it meanwhile.
- **B-47 Content at the IME target.** While the IME target is visible, the content box is measured to the region
  actually visible at the target: from the lifted header's bottom to the keyboard's top
  (`getAnimation(scope).target`, [V] `WindowInsetsRulers.kt:69`, `:231-250`), so `bringIntoView` aims at the real
  viewport (fixes D4). While the target is hidden, it is measured at Large's content height. That is one re-measure when
  the keyboard starts to open and one after it closes (≤ 2 per IME transition), plus ≤ 1 per settle while it is up.
  The target is written from placement into a snapshot field only when it changes.
- **B-48 API 26-29.** No IME animation: the inset jumps. `lift` is animated toward the new value on `springs.screen`
  (MOTION-PLAN M22) by an `Animatable` that runs whenever `getAnimation(scope).isAnimating` is false and the value
  changed.
- **B-49** A drag of the sheet itself (not a content scroll) that moves 24 dp (`sheet.keyboard.dragHidesAfter`) while
  the IME is up hides the keyboard (`LocalSoftwareKeyboardController.hide()` from the drag callback) [D].
- **B-50 Insets are counted once.** The content box consumes `safeDrawing.only(Top)`, `navigationBars` and `ime`, so an
  `imePadding()`, `navigationBarsPadding()` or `statusBarsPadding()` inside returns 0 and an `AppTopBar` inside pads no
  status bar (QA-19). While the keyboard is down, `contentPadding.bottom` = `footerHeight + max(bottomInset,
  floatingGap) + reach`, where `floatingGap` is `i₀` for a floating sheet (a constant at measure time) and `reach` is
  B-31's; while it is up, `footerHeight` (the box already ends at the keyboard, B-47). The footer itself is never padded
  by an inset: it is placed above `restBottom` or on the keyboard. Content without a footer
  therefore clears the navigation bar (fixes D3), and the IME is never counted in both the footer and the content
  (fixes D2). The sheet's frame is centred within `safeDrawing.only(Horizontal)` and is at most the safe width wide.
- **Forbidden inside sheet content** (docs and a code rule, §19 WP03): `Modifier.fitInside(WindowInsetsRulers.Ime…)`,
  `imePadding()`, `windowInsetsPadding(WindowInsets.ime)`. `fitInside` re-measures its child with fixed constraints in
  the placement block on every IME frame ([V] foundation-layout 1.12.1 `RulerAlignment.kt:36-50`).

### 5.10 Background interaction (non-modal)

- **B-51 The boundary** is `upThrough`'s offset `y_U` (no boundary when `upThrough` is null: always undimmed). With
  `y_N` the offset of the next detent above it, the modal ramp is `m = clamp((y_U − y) / (y_U − y_N), 0, 1)`.
  - `m = 0` (at or below `upThrough`): no dim; the scrim node's `sharePointerInputWithSiblings()` returns true and it
    consumes nothing, so a touch around the sheet reaches the app root, the overlay host's sibling, in the same frame
    ([V] `PointerInputModifierNode.kt:73-79`). The flag is a plain field, not snapshot state: no recomposition, no node
    swap. What is beneath stays visible to screen readers.
  - `m > 0`: the dim fades in with `m` (drawn per frame by the presenter, §6.4) and the scrim blocks: taps are attempts or
    dismissals as for a modal sheet. iOS: "dimming still fades in if I resize to the large detent" (WWDC21 10063 [A]).
  - `blocking = settled or target detent above upThrough` is a `derivedStateOf`; it flips the beneath
    `hideFromAccessibility()` with ≤ 1 recomposition per crossing.
- **B-52 Above the bottom chrome.** With `aboveBottomChrome`, `restBottom = H − bottomChrome` (the settled
  `LocalBottomChrome` the shell writes, `ds4/foundation/layout/Chrome.kt:31` [C]), so a peek rests on top of the floating
  tab bar, which stays usable. The surface's bottom edge moves from `restBottom` to the window bottom across `e`
  (§6.3), covering the bar at Large.
- **B-53** A non-modal sheet never drives the card stack: `recede` reads only modal sheets.

### 5.11 Stacking

- **B-54 Geometry.** A sheet presented while another is up stacks on it (an in-screen sheet opened from a sheet's
  content, or a destination over a destination). The covered sheet, in its own layer (L0), scales by
  `1 − (1 − sheet.stack.coveredScale) · e_top` (to 0.9165) about its top centre and moves up by
  `sheet.stack.coveredLift · H · e_top` (0.005 H, 4.4 dp on the Redmi) [M] `sheet.dart:57-62`, `:84-90`, where `e_top`
  is the covering sheet's `expansionFraction`: as with the shell, a covering sheet at a partial detent (an action sheet,
  a Medium sheet) only dims the covered one, and the covered sheet recedes as the covering one reaches Large. The
  covering sheet's `largeTop` is the covered sheet's lifted top plus `sheet.stack.peek` (10 dp), so each level shows a
  10 dp strip and no sheet reaches the status bar. The covered sheet draws the covering sheet's dim inside its own
  clipped layer (it is the presenter at that level). Top corners keep `R` (WWDC21: "the system will keep stacked
  corners looking consistent" [A]).
- **B-55** Only the top sheet takes drags, Back, Escape and focus. A covered sheet's grabber hides, its drag is
  disabled, and it is hidden from accessibility while a modal sheet is above it.
- **B-56** At most `sheet.stack.maxCoveredDrawn` (2) sheets behind the front one are drawn; deeper ones get layer alpha
  0, which the renderer skips without an offscreen pass. Their compositions stay.
- **B-57** When the top sheet leaves, the covered sheet un-recedes by the same fraction, so the two motions stay locked
  together. The HIG asks for one sheet at a time [A]; `AppSheet(item)` follows it, stacking stays possible.

### 5.12 Navigating from a sheet

- **B-58 From a presented sheet** (`ShellState.submit`, `nav4/ShellState.kt:116`, `:178` [C]), while the top presented
  entry is a sheet:
  - a key registered as `Presentation.Sheet` is pushed on the presented stack and stacks over it (B-54);
  - a `Presentation.Cover` key or an ordinary key: the presented sheets above the topmost non-sheet presented entry are
    popped as one stack change (they slide out in `onRemove()`), and the key is submitted as if they had not been there
    once the presentation gate settles (`NavGuard` holds it). A cover is presented; an ordinary key goes to the front
    stack (the tab, or the presented stack when a cover remains). So a cover never sits above a sheet (D-23), and
    `onRemove()`'s `isPresented` check (§11.2) stays as a guard only.
- **B-59 Inside a sheet.** `AppSheetNavHost` keys never reach `ShellState`. Its push and pop run on `springs.screen`
  with parallax `navigation.parallax` (0.333), clipped to the sheet, with `SheetBackHandler(enabled = backStack.size >
  1) { backStack.removeLastOrNull() }`. A `FitContent` sheet animates its height between steps through the re-resolving
  detent (B-10). The inner Back is a key-style pop in v1 (no predictive tracking inside the sheet). Fixes N5 and
  MASTER-PLAN A3.7.
- **B-60 From an in-screen sheet.** If its content navigates its own screen away, the owner is covered and the sheet
  parks (B-14). The docs tell features to close an in-screen sheet in the same event that navigates.

### 5.13 Haptics

- **B-61** `AppHaptic.Threshold` (`GESTURE_THRESHOLD_ACTIVATE` on API 34+, `CONTEXT_CLICK` below;
  `ds/foundation/interaction/AppHaptics.kt:59-60` [C]) plays on the first frame a settle is within 1 px of a detent
  other than the one the gesture started on, played from the settle coroutine, never from composition; and on arrival
  after a grabber tap, Enter or Space. At most one per settle: a retarget before arrival plays it once, at the final
  arrival. None on show, dismissal, attempts, code moves or accessibility actions.

### 5.14 Process death and configuration

- **B-62**

| Case | Behaviour |
|---|---|
| Destination sheet, process death | `ShellState` restores the presented keys (`nav4/ShellState.kt` saver); `SheetSessions` restores each sheet's saved string (§11.2): `shown = 1` → snap to the detent, no slide; the presenter is receded from the first frame when the detent is Large |
| In-screen sheet, process death | `visible` is the caller's (`rememberSaveable` or ViewModel state); `rememberAppSheetState` restores the detent the same way |
| Activity recreated | as process death |
| Resize without recreation (freeform, fold, unfold, split) | B-10 |
| Restored detent no longer listed or active (it was Medium, now Compact height) | the nearest active detent by height |
| Content state | the content's own `rememberSaveable` |

### 5.15 Reduce motion and transitions

- **B-63**

| Motion | `Slide` (default) | Reduce motion, or `SheetTransition.CrossFade` |
|---|---|---|
| Present | slide on `springs.sheet` | fade of the sheet layer with `CompositingStrategy.ModulateAlpha` at its detent, in over `fades.longMs` (200 ms) with `fades.enterEasing`; the dim fades with it |
| Dismiss | slide | fade out over `fades.shortMs` (120 ms) with `fades.exitEasing` |
| Card stack | scale, translation, corners, overlay | overlay and dim only, no scale or translation |
| Drag, settle, rubber band | unchanged | unchanged (direct manipulation, as iOS keeps it) |
| Back gesture | shrink | unchanged shrink (it follows the finger); the commit fades |
| Floating to attached | follows `e` | unchanged |
| Form sheet | slide | fade |
| Glass | sampling | a Glass sheet draws Frosted while it fades: never a layer alpha around a sampling surface (`handoff/WP05.md:230`) |

  **Overrides** MOTION-PLAN §1 rule 5's 150 ms, which is not a token (`tokens/motion.json:31` holds the fades).

### 5.16 Frame rate

- **B-64** The sheet layer carries `Modifier.preferredFrameRate(FrameRateCategory.High)` ([V] `ui112/.../FrameRate.kt:71`;
  SMOOTHNESS S10). It must vote only while the sheet draws [U: a Perfetto trace shows that an idle sheet does not hold
  120 Hz; gate `sheet_idle.frames = 0`].

---

## 6. Motion and tokens

### 6.1 Springs

All from `AppTheme.springs`: `AppSpring(response, bounce)`, `k = (2π / response)²`, `ζ = 1 − bounce`
(`ds/theme/AppMotion.kt:61-77` [C]); travelling springs are divided by the motion style's speed (Calm 0.87, Snappy 1.15,
`tokens/motion.json:3-6`).

| Use | Token | Standard | Calm / Snappy | Source |
|---|---|---|---|---|
| Show, hide, detent settle, programmatic move, stretch release, park, `Expand`, covered-sheet recede, card stack (it rides the sheet; no spring of its own) | `springs.sheet` | 0.34 s, bounce 0 (k 341.5, ζ 1) | 0.3908 s / 0.2957 s | CP1 keep (`decisions/CP1.md:40`, `:137`) |
| Action and choice sheet arrival, anchored card | `springs.dialog` | 0.275 s, bounce 0 (k 522) | ÷ 0.87 / ÷ 1.15 | CP1 (`:41`), M17 |
| Keyboard lift on API 26-29; push and pop inside `AppSheetNavHost` | `springs.screen` | 0.344 s, bounce 0 (k 333) | ÷ speed | M22; CP1 (`:32`) |
| Reduce-motion and cross-fade | `fades.longMs` / `fades.shortMs` | 200 / 120 ms, enter/exit easings | — | `tokens/motion.json:31` [C] |

No sheet motion bounces (CP1: "no bounce on anything that moves a full screen"). Every animation is seeded with the
velocity it inherits (release, retarget): `anchoredDrag` retargets with `lastVelocity` (androidx does the same,
`src/SheetDefaults.kt:316-339` [G]), which fixes D7. The only fixed-duration animations are the fades. `springs.sheet`
on the Redmi: 55 % of the travel at 100 ms, 88 % at 200 ms, 99 % at 359 ms [D].

### 6.2 The `sheet` token section

New top-level section in `tokens/motion.json` (beside `springs`, `scroll`, `fades`). Lengths in dp, speeds in dp/s,
`release.dismissFlick` in window heights per second, `stretch.aboveLarge` and `stack.coveredLift` as fractions of H.
The token writer (`generator/genkit/tokens/kotlin.py`, `swift.py`, and the Multiplatform writer) emits it as
`DesignTokens.Sheet` for Android, Compose Multiplatform and SwiftUI.

```json
"sheet": {
  "detent":      { "mediumFraction": 0.5, "largeTopGap": 10, "mediumMinGap": 48, "minHeight": 48 },
  "release":     { "projectionSeconds": 0.099, "detentFlick": 400, "dismissFlick": 2.0, "dismissPosition": 0.52 },
  "stretch":     { "aboveLarge": 0.008 },
  "grabber":     { "width": 36, "height": 5, "top": 5, "touchTarget": 48, "alpha": 0.45, "hoverAlpha": 0.7, "contentTop": 15 },
  "header":      { "top": 14, "minHeight": 44, "scrollEdgeHeight": 24, "scrollEdgeRamp": 8 },
  "keyboard":    { "dragHidesAfter": 24 },
  "form":        { "heightFraction": 0.85, "maxHeight": 620 },
  "side":        { "inset": 16 },
  "cardStack":   { "scale": 0.9165, "corner": 12, "deviceCornerFactor": 0.9, "deviceCornerMin": 20,
                   "overlayAlpha": 0.10, "overlayColor": { "light": "#000000", "dark": "#C8C8C8" }, "statusBarFlip": 0.5 },
  "stack":       { "coveredScale": 0.9165, "coveredLift": 0.005, "peek": 10, "maxCoveredDrawn": 2 },
  "back":        { "shrinkX": 48, "shrinkY": 24, "easing": [0.1, 0.1, 0.0, 1.0], "guardedShare": 0.2,
                   "persistentFollow": 24, "formScale": 0.90 },
  "frosted":     { "bodyAlpha": { "light": 0.92, "dark": 0.94 } },
  "glass":       { "opaqueAt": 0.5, "blur": 16, "bodyAlpha": { "light": 0.55, "dark": 0.50 } },
  "rim":         { "solidDarkAlpha": 0.08 },
  "shadow":      { "blur": 20, "y": 6, "alpha": 0.10 },
  "actionSheet": { "rowHeight": 56, "maxFitFraction": 0.6, "cardWidth": 320 },
  "choice":      { "maxFitFraction": 0.6, "searchFrom": 6 }
}
```

`tokens/styles.json` gains two fields per design style (judge-1 graft 10, judge-3 G9):

| Style | `sheetInset` (dp) | `sheetMaterial` |
|---|---|---|
| Utility | 8 | `Solid` |
| Social | 8 | `Solid` |
| Editorial | 0 (square sheets read wrong floating inside rounded display corners) | `Solid` |
| Playful | 10 | `Solid` |
| Glass (WP05, `requires = ["liquidglass"]`) | 8 | `Glass` |

`radii.sheet` already exists (24 / 30 / 8 / 28 dp, `tokens/styles.json:5`, `:22`, `:39`, `:56` [C]). The inset values are
**taste**.

| Token | Value | Meaning | Source |
|---|---|---|---|
| `detent.mediumFraction` | 0.5 | Medium's top at half the bounds | [A] "approximately half"; WP03 0.55 (`AppSheet.kt:335` [C]); **taste** |
| `detent.largeTopGap` | 10 dp | Large's top below `safeDrawing.top` | WP03 (`AppSheet.kt:343` [C]); iOS ≈ 0.01 H [M] |
| `detent.mediumMinGap` | 48 dp | Medium is inactive within this of Large | [D] |
| `detent.minHeight` | 48 dp | lower clamp for any detent | [D] |
| `release.projectionSeconds` | 0.099 s | `v · r / (1 − r) / 1000` at r = 0.99/ms | WWDC18 803 [A]; `AppSheetState.kt:216-219` [C] |
| `release.detentFlick` | 400 dp/s | a flick moves one detent | [D] **[U]: QA2 tunes it (§21)** |
| `release.dismissFlick` | 2.0 H/s | dismiss velocity from the lowest detent | [M] `sheet.dart:76-78` |
| `release.dismissPosition` | 0.52 | of the lowest-to-hidden travel | [M] `sheet.dart:1114-1119` |
| `stretch.aboveLarge` | 0.008 H | rubber-band asymptote past Large | [M] `sheet.dart:27-37` (0.08 → 0.072 H) |
| `grabber.*` | 36 × 5 dp, 5 dp top, 48 dp target, α 0.45, hover α 0.7, content 15 dp | | [M] `sheet.dart:704-724`; WP03 α (`AppSheet.kt:302` [C]); 48 dp [G] |
| `header.*` | 14 dp top, 44 dp min, 24 dp fade, full after 8 dp of scroll | | WP03 (`AppSheet.kt:306` [C]); iOS bar height |
| `keyboard.dragHidesAfter` | 24 dp | B-49 | [D] |
| `form.*` | ≤ min(0.85 H, 620 dp) | Center placement height cap | WP03 (`AppSheet.kt:227`, `:337` [C]); ADAPTIVE-PLAN:434 |
| `side.inset` | 16 dp | gap of a Start/End panel | M3 detached side sheet [G] |
| `cardStack.*` | 0.9165 scale; corners `0.9 ×` display corner (if ≥ 20 dp, else 0) → 12 dp; overlay α 0.10, black / `#C8C8C8`; flip at 0.5 | | [M] `sheet.dart:14-25`, `:64-74`, `:84-90`, `:327-367` |
| `stack.*` | 0.9165, 0.005 H, 10 dp peek, 2 drawn | B-54, B-56 | [M] `sheet.dart:57-62`; peek [D] |
| `back.*` | 48 / 24 dp; `(0.1, 0.1, 0, 1)`; guarded 0.2; persistent 24 dp; form 0.90 | | [G] MDC `motion_dimens.xml:23-24`, SPEC §2; guarded and persistent [D]; form: MDC main container minimum [G] |
| `frosted.bodyAlpha` | 0.92 / 0.94 | Frosted body at partial detents | `glass/GlassDefaults.kt:57-67` [C] |
| `glass.*` | opaque by e = 0.5; blur 16 dp; tint 0.55 / 0.50 | | [A] WWDC25 323; values **[U]: CP2 decides** |
| `rim.solidDarkAlpha` | 0.08 | hairline in dark theme | [D] |
| `shadow.*` | blur 20 dp, y 6 dp, black α 0.10, light only | §7.7 | [D] between WP05's chrome (16/4/0.10) and content (10/3/0.08) shadows, `glass/GlassDefaults.kt:84-85` [C] |
| `actionSheet.*`, `choice.*` | 56 dp rows; 0.6 cap; 320 dp card; search from 6 | | WP03 (`AppActionSheet.kt:197` [C]); VitFleet `ChoiceSheet.kt` |

### 6.3 Floating ↔ attached

Let `a = e`, `fl` = 1 when the sheet floats (Bottom placement, `i₀ > 0`, a partial detent active, gesture navigation
(`WindowInsets.tappableElement.bottom == 0` [G]), height not Compact), else 0. Everything below is read in draw or
placement.

| Quantity | Value |
|---|---|
| Side inset | `i₀ · fl · (1 − a)` |
| Resting surface bottom `b₀` | window-anchored: `H − i₀` floating, `H + R` attached; above the chrome: `H − bottomChrome − i₀ · fl` |
| Surface bottom | `lerp(b₀, H + R, a)` (at `a = 1` the bottom corners are R below the window: square-bottomed and edge-attached) |
| Corners | uniform `R` on all four |
| Content clip | the same round rect: `(i, y_v, W − i, bottom)`, radius `R`, with `y_v` the placed top |
| Header | inside a constant side inset `i₀ · fl` (D-7) |
| Footer | B-45 (it rides the visible bottom) |
| Body alpha (Frosted, Glass) | `lerp(bodyAlpha, 1, min(a / glass.opaqueAt, 1))` |
| Shadow (where allowed, §7.7) | non-modal Bottom: `alpha · (1 − a)`; Start/End, Center, cards: constant |

The change is continuous and the outline stays a **uniform** round rect throughout. **Correction to design-feel §5.4**
(judge-2 §2.1): a floating sheet's surface is re-recorded (D1: one round rect and a rim) on every frame of a drag
between partial detents, because its height is `bottom − y_v`. That is cheap; the shadow never follows it (§7.7). An
attached sheet only moves (L1).

### 6.4 Card stack and the merged dim

The presenter at level `L` reads `e = stack.recede(L)`: the `expansionFraction` of the sheet directly above it when that
sheet is modal, Bottom-placed, has Large active, and the height class is not Compact; otherwise 0.

| Quantity | Value | Phase |
|---|---|---|
| Scale | `1 − (1 − cardStack.scale) · e` about `(0.5, 0)` | layer |
| Translation Y | `safeDrawing.top · e`: the presenter's top meets the safe area, 10 dp above the sheet's top | layer |
| Corners | `lerp(R₀, cardStack.corner, e)`, uniform; `R₀ = deviceCornerFactor ·` the display's top corner radius (`WindowInsets.getRoundedCorner`, API 31+) when that is ≥ `deviceCornerMin`, else 0; `Shape` objects cached per whole px; `clip = e > 0` | layer outline (round rect: hardware clip) |
| Merged dim | below | draw, inside the layer, after the content |
| Backing | §7.9 | — |
| Status bar | light icons while `stack.rootReceded` (light theme) | ≤ 1 recomposition per crossing |
| Reduce motion | no scale, translation or corners; the dim stays | — |

**The merged dim** (D-8; judge-1 graft 7, judge-2 graft 29). The presenter draws one rect that is the exact composite of
its card overlay (part of the presenter, drawn first) and the sheet's scrim (drawn over it):

```
u  = pf (modal) or m (non-modal ramp, B-51)          l  = e (0 when the card stack does not apply)
a1 = cardStack.overlayAlpha · l                      a2 = scrim.alpha · u
A  = 1 − (1 − a1) · (1 − a2)                         // the combined alpha
K  = (O · a1 · (1 − a2) + S · a2) / A                // per channel; O = overlay RGB, S = scrim RGB
drawRect(color = K, alpha = A)                       // skipped when A == 0
```

`A` never decreases as the sheet rises (F6). One D2 rect in both themes; a `Color` is a value class, so nothing is
allocated. Worked with `colors.scrim`: light at Medium `A = 0.40`, at Large `1 − 0.9 · 0.6 = 0.46`; dark at Medium
0.56, at Large `1 − 0.9 · 0.44 = 0.604`. With no presenter at that level (a preview, a catalog page without the shell),
the sheet's scrim node draws `S` at `a2` itself. The dim covers the whole presenter, status bar and tab bar included
(QA-13). Its strength against iOS 26 is [U] (§21).

### 6.5 Stacked sheets

B-54 to B-57. The covered sheet's transform is in its surface's layer (L0), read from the stack.

### 6.6 Glass

| Tier (`LocalGlassQuality`, `glass/GlassQuality.kt:24-48` [C]) | `SheetMaterial.Glass` at partial detents | Across the expansion | At Large |
|---|---|---|---|
| Live, LiveNoLens (`samplesScene`, `:42-43`) | live backdrop through `LocalSheetBackdrop`: blur `glass.blur`, the presenter's dim, tint at `glass.bodyAlpha` | tint `lerp(bodyAlpha, 1, min(e / 0.5, 1))`; the sampling pass is skipped in draw once the tint is 1 | opaque `container`, no sampling |
| Frosted | Frosted | body alpha → 1 by `e = 0.5` | opaque |
| Solid (Reduce transparency) | Solid | — | — |

- Without the liquid glass feature (lean presets), `LocalSheetBackdrop` is null and Glass draws Frosted; the backdrop
  file is not generated (WP01 marker).
- Never a layer alpha around a sampling surface; Kyant `effects` read no state; the tint and dim animate in
  `onDrawSurface` (`handoff/WP05.md:230`; `glass/GlassChrome.kt:47-54` [C]).
- The sampled scene excludes the presenter's dim, so the backdrop draws `dim()` over the sample (judge-3 G8).
- The card stack scales the presenter while the sheet samples it between `e = 0` and `0.5`. WP05 verifies alignment on
  the phone; if sampling cannot follow the presenter's transform within 1 px, `glass.opaqueAt` becomes the first frame
  the presenter recedes (the body is opaque whenever `e > 0`).

---

## 7. Visuals

### 7.1 Shape and clip

- **One uniform radius `R`** (`AppSheetStyle.cornerRadius`, the style's `radii.sheet`) on all four corners, so the
  outline is a simple round rect, which the renderer clips without an offscreen buffer on every API [V]
  (`OutlineResolver.android.kt:263` uses `setRoundRect` only when the round rect is simple; any other shape takes the
  path branch at `:282-288`, a canvas clip below API 33).
- **Attached = the bottom corners below the window** (§6.3), not a different shape.
- **Circular corners**, not continuous ones: a continuous corner is a path (`glass/GlassDefaults.kt:17-22` [C]); at
  24-30 dp the difference is about a pixel of curvature [D].
- **Clip mechanism.** The content is clipped to the current surface rect in the sheet layer's draw, before
  `drawContent()`: a reused `Path` rebuilt with the platform's float-argument `addRoundRect(l, t, r, b, rx, ry, dir)`
  (Android `android.graphics.Path`), so nothing is allocated per frame. A single round rect is clipped analytically
  [U: Perfetto shows no `saveLayer` and no stencil pass during a drag, §21]. **Fallback** if it does: a constant-size
  layer outline (round rect, hardware clip) cached per whole px of side inset (at most 23 entries for 8 dp at 2.75), its
  bottom R below the window: the floating bottom gap is dropped and the sides keep floating. The layer-outline approach
  is not the first choice because its rect depends on `y`, so it would need a new outline every drag frame (judge-3
  G19's order, adjusted for that reason).
- **Center and Start/End:** the same radius, all four corners visible, constant shape.

### 7.2 Width and what the content is measured at

| Placement | Sheet width | Content measured at |
|---|---|---|
| Bottom, Compact width | the safe width, ≤ `layout.sheetMaxWidth` (640 dp, `ds/foundation/adaptive/AppLayout.kt:65` [C]), centred | the sheet width (the floating inset is a clip) |
| Bottom, Medium width and up (non-modal, or forced) | `min(640 dp, W − 2 · gutter)`, centred | the sheet width |
| Bottom, Compact height | `min(safe width, 640 dp)`, centred; attached | the sheet width |
| Center (form sheet) | `min(layout.formSheetWidth, W − 2 · gutter)` (540 / 600 dp, `AppLayout.kt:45-66` [C]) | the sheet width |
| Start / End | `layout.supportingPaneWidth` (360 / 412 dp) | the sheet width |

The content column is measured once with `maxHeight` = Large's height and `minHeight = 0`; the content box is
`weight(1f, fill = false)`, so a lazy list fills it and a short form wraps (material3 measures the same way,
`src/BottomSheet.kt:299-307` [G]). Its height is `FitContent`'s. Gutter content keeps the app's gutter from the
sheet's edge at Large and sits `i₀` closer to the visible edge while floating; nothing is clipped (REQUIREMENTS 34),
because the header, the only slot near the top corners, is inset (D-7).

### 7.3 Grabber

| Property | Value |
|---|---|
| Size and place | 36 × 5 dp, fully rounded, centred, 5 dp from the sheet's top [M] `sheet.dart:704-724` |
| Colour | `colors.grabber`: `contentTertiary` at α 0.45 (≈ iOS `tertiaryLabel`; `AppSheet.kt:302` [C]); with high-contrast text (API 36): `contentSecondary`, opaque |
| Touch target | 48 × 48 dp centred on the pill (`minimumTouchTarget()`); dragging works on the whole sheet |
| Hover (mouse) | α 0.7 and `PointerIcon.Hand` |
| Keyboard focus | focusable; a 2 dp ring in `colors.focusRing` around a 48 × 24 dp rounded rect |
| Shown | §4.8 slot rules |
| Test tag | `sheet_grabber` (resource-id contract, `handoff/WP04.md:248`) |

With no header, content starts at `ContentTop` (15 dp).

### 7.4 Header

- Pinned above the content, never over it: a caller who forgets padding never loses content under it (REQUIREMENTS 34).
- `AppSheetHeader`: starts 14 dp below the sheet's top; min height 44 dp (`heightIn`, never fixed); title in
  `typography.headline`, `contentPrimary`, centred, one line (up to two at font scale ≥ 1.5, then the row grows),
  ellipsized; subtitle in `footnote`, `contentSecondary`.
- A three-slot `Layout` measures `leading` and `trailing` first and reserves `max(leadingWidth, trailingWidth)` on both
  sides of the title (fixes D5, `AppSheet.kt:306-325` [C]). RTL mirrors the slots.
- `AppSheetCloseButton`: `AppIconButton` in its Tinted Small size with the close glyph and `designsystem_close`, 48 dp
  target, as today (`AppSheet.kt:316-326` [C]); in the Frosted and Glass materials its circle is drawn frosted. Text
  actions are `AppButton` text variants; Done is semibold.
- **Scroll-edge effect:** while `contentScrolled > 0`, the header draws a 24 dp gradient of the container colour over
  the content's top, alpha = `contentScrolled` (full after 8 dp of scroll), read in draw (M27,
  `ds/foundation/scroll/ScrollEdge.kt`). No hairline.
- Background: transparent, on the sheet's surface.

### 7.5 Footer

- Placed per B-45, so on a short detent it never rides over the header.
- Draws on the sheet's surface; `AppBottomActionBar` inside a sheet paints no background and keeps its top hairline.
- Never padded by an inset: the sheet places it above the navigation area, or on the keyboard.

### 7.6 Materials, light and dark

| Element | Light (Cool palette) | Dark (Cool palette) |
|---|---|---|
| Container | `surfaceElevated` #FFFFFF | `surfaceElevated` #1D232E (`ds/theme/tokens/DesignTokens.kt:237`, `:280` [C]) |
| Frosted body (partial) | container α 0.92 + rim + sheen | container α 0.94 + rim + sheen |
| Glass tint (partial) | α 0.55 [U, CP2] | α 0.50 [U, CP2] |
| Rim | none (Solid); white α 0.85 (Frosted, Glass) | white α 0.08 hairline (Solid); white α 0.20 (Frosted, Glass) |
| Shadow | black α 0.10, blur 20, y 6, where §7.7 allows | none |
| Grabber | `contentTertiary` α 0.45 | `contentTertiary` α 0.45 |
| Dim | `colors.scrim` `0x660B0F1A` | `colors.scrim` `0x8F000000` (`DesignTokens.kt:249`, `:292` [C]) |
| Card overlay | black α 0.10 | `#C8C8C8` α 0.10 |
| Backing behind a receded presenter | black | black |
| Status bar icons over the card stack | light past `e = 0.5` | light |

Every palette and design style reads the same roles, so Warm, Paper and Cream follow without code. `groupedColors()`
uses `backgroundGrouped` behind `surfaceGrouped` rows, as iOS's grouped sheets.

The surface is its own `SheetSurfaceNode` (not `glassSurface`): it draws the body as one `drawRoundRect` of the
current rect, the rim as a stroke, and Frosted's brushes; brushes and strokes are cached by width and radius, never by
height (the rim gradient spans a fixed distance from the top). WP05's `glassSurface` rebuilds its draw cache on every
size change (`glass/GlassSurface.kt:81`, `:97-121` [C]) and is not used for sheets.

### 7.7 Shadow

The pull-to-refresh black ring (REQUIREMENTS 29) came from a shadow seen through a fading surface; the sheet's drag
would add a CPU blur per frame (D-18). The rules:

1. **Who.** Non-modal Bottom sheets, Start/End panels, Center form sheets and anchored action-sheet cards, in light
   theme. **Never** a modal Bottom sheet (the dim separates it, as on iOS), never in dark theme (the rim does).
2. **How: a nine-slice.** Once per key (corner radius px, blur px, offset px, colour, density), draw a shadow of a
   round rect of side `2R + 2 px` with `DropShadowPainter` into an `ImageBitmap` padded by the blur and offset, then
   clear the rect's interior (`BlendMode.Clear`), so only the outside remains and a translucent body can never show it.
   Each frame, draw it as nine `drawImage(image, srcOffset, srcSize, dstOffset, dstSize)` slices around the current
   surface rect: four fixed corners, four edges stretched along their axis, no centre. No allocation per frame.
3. **Skip** every slice when the shadow's alpha is 0 (the painter rasterises even at alpha 0).
4. **Bounded.** The node holds at most one bitmap per key it has used (two at most: one per theme).
5. **No `Modifier.shadow(elevation)`, no RenderNode elevation, no `dropShadow` sized to the surface** on any sheet
   node or ancestor.
6. **The surface never fades.** Only the content layer fades (cross-fade, reduce motion), with `ModulateAlpha`.

### 7.8 Scrim node

- Fills the sheet's bounds; hit-tests (dismissal, attempts, non-modal pass-through B-51) and carries semantics (§9).
- Draws only when no presenter claims its level (§6.4).
- No `graphicsLayer { alpha }` on it or on any window-sized node (SMOOTHNESS S8).

### 7.9 Backing and status bar

- **Backing.** Behind a receded presenter the window shows black: the top band, the side slivers, the bottom band, the
  presenter's rounded-corner cut-outs, and whatever shows through a part-floating sheet's gaps (judge-2 §2.5). It is
  the fill that already lies beneath the presenter, recoloured black while `e > 0`: the window's background for the
  shell (WP04 sets it from a derived flag, one change per crossing), the cover frame's own full-screen fill for a cover.
  **Never an added full-screen draw** (judge-2 R15).
- **Status bar.** Light icons while `stack.rootReceded` in light theme (`sheet.cardStack.statusBarFlip` = 0.5), set by
  WP04's edge-to-edge owner.

### 7.10 RTL and large text

Start/End placement and the header's slots follow the layout direction; the grabber and the predictive-back origin are
centred. At font scale 1.5 and 2.0 the header grows, `FitContent` re-resolves once, and nothing is clipped
(REQUIREMENTS 34; screenshot tests §15.5).

---

## 8. Adaptive behaviour

`SheetPlacement.Automatic` resolves in measure, with no `BoxWithConstraints`, from the size class (modal: the window's;
non-modal: the container's, B-13) and `AppTheme.posture` (`ds/foundation/adaptive/SizeClass.kt` [C]). Width classes:
Compact < 600, Medium 600-839, Expanded 840-1199, Large 1200-1599, ExtraLarge ≥ 1600 dp; heights: Compact < 480,
Medium 480-899, Expanded ≥ 900 dp. `SizeClass` is equality-stable, so a resize within a class changes nothing.

| Window | Modal sheet | Non-modal sheet |
|---|---|---|
| Compact width, height not Compact (phone portrait) | **Bottom**: detents as given; floating while partial; attached at Large; card stack at Large | Bottom, above the bottom chrome at undimmed detents |
| Compact height, any width (phone sideways, short split) | **Bottom**, ≤ 640 dp, centred in the safe width; Medium inactive (R-2); other detents if they fit; attached, never floating; dim only, no card stack (ADAPTIVE §3.5) | same, its peek kept if it resolves |
| Medium width, height Medium and up (small tablet, unfolded inner screen, resized window) | **Center** form sheet: one stop `min(0.85 H, 620 dp, fit)` (R-10), all corners `R`, slides up on `springs.sheet`, swipe down closes (D-19), grabber hidden, dim, no card stack (fixes D11) | Bottom, `min(640 dp, W − 2 · gutter)`, centred |
| Expanded width and up, height Medium and up | Center form sheet (600 dp wide from Large width) | **Start** panel (iOS 27 `presentationPlacement(.leading)`, the Maps panel [A]): `supportingPaneWidth`, 16 dp (`side.inset`) from the start edge, the top (below the safe area) and the bottom of its bounds; detents as heights within the panel; what is beside it stays usable |
| Tabletop posture | Bottom, never a card across the crease; Medium rests on the fold (R-2); card stack at Large | same |
| Book posture (vertical separating hinge) | Center form sheet inside one half (the larger; the end half when equal), never across the hinge (Material `AvoidSeparating`) | Start panel inside the start half |
| Explicit `Bottom` / `Center` | honoured in every window | honoured |
| Explicit `Start` / `End` | below Expanded width: Bottom (no room) | same |
| Size-class or posture change while shown | re-resolved; snaps to the new frame at the same detent (B-10) | same |
| Freeform, ChromeOS, desktop windowing | the caption bar is part of `safeDrawing.top`; a resize within a class moves anchors without recomposing the content | same |

- **Form sheet mechanics.** The same state and layout node. One stop plus hidden. It enters by sliding up from below
  the window (the dim follows `pf`), the person can drag it down (B-22 with one stop), and predictive back scales it
  about its centre (B-39). `e = 0`: nothing recedes. No grabber; give it a header with a close button (docs).
- **Start/End mechanics.** A floating card at all detents (no attach morph), all corners `R`, shadow per §7.7. Its
  bottom edge is fixed `side.inset` above its bounds' bottom (plus the navigation bar); its top moves with the detent.
  Large's top is the bounds' safe top + `side.inset`. No card stack.
- **Input on large screens.** Tab reaches the grabber; Enter or Space cycles detents (Material [G]); Escape requests
  dismissal (B-42); a modal sheet traps focus and returns it on close (§9); a mouse drags the sheet by any non-scrolling
  part; the wheel scrolls (B-27); right-click does nothing on the sheet. No orientation or resizability locks (L-1).

---

## 9. Accessibility

| Node | Semantics |
|---|---|
| Sheet root | `isTraversalGroup = true`, `traversalIndex = 0f`; `paneTitle` always set: the header's title, else `designsystem_sheet_pane` ("Sheet") (fixes D12); `dialog()` while modal or above `upThrough` ([V] `SemanticsProperties.kt:972`, `:1022`, `:1149`) |
| Grabber | `Role.Button`; `contentDescription` `designsystem_sheet_grabber` ("Resize sheet"); `stateDescription` from the settled detent: "Expanded" at the tallest, "Collapsed" at the lowest when there are three or more, else "Partially expanded" (`derivedStateOf`, ≤ 1 recomposition per settle); click = B-19; custom actions *Expand*, *Collapse*, *Close sheet*, each only when it applies ([V] `:1632-1660`; `src/BottomSheet.kt:365-397` [G]; MDC state prefix) |
| No grabber drawn | the actions and state move to the sheet root |
| Scrim (modal, `scrimTapDismisses`) | `onClick(label = "Close sheet")`, `contentDescription` `designsystem_sheet_close_scrim`, `traversalIndex = 1f`, so it is the last stop (`src/Scrim.kt:58-71` [G]); nothing when it does not dismiss |
| Content beneath a modal sheet | `hideFromAccessibility()` at both hosts ([V] `SemanticsProperties.kt:1075`), replacing `clearAndSetSemantics` at `AppOverlayHost.kt:58` [C] and fixing N8 under the presentation host [U: TalkBack's linear navigation skips the hidden node's descendants; if not, keep `clearAndSetSemantics`, §21] |
| Covered sheet | hidden from accessibility while a modal sheet is above it |
| Close button | `designsystem_close`, 48 dp |
| Detent changes | announced through the grabber's `stateDescription` and the pane title; never `announceForAccessibility` (deprecated in API 36 [G]) |

- **Focus.** Opened with keyboard input (`InputMode.Keyboard`), focus moves to the sheet's first focusable after the
  header. A modal sheet traps Tab: `focusProperties { onExit = { cancelFocusChange() } }` on its root ([V]
  `FocusProperties.kt:188`). On close, focus returns to the element focused before [U: the restore API, §21].
- **TalkBack scrolling** never resizes the sheet (B-30); the grabber's Expand action does.
- **Touch targets** 48 dp for the grabber, the close button and header actions.
- **Haptics:** none for accessibility actions (B-61).
- **Reduce motion:** B-63. **Reduce transparency:** Glass and Frosted draw Solid through `GlassQuality.Solid`.
- **Strings** in `core/designsystem/src/main/res/values/strings.xml`, prefix `designsystem_` (F-5), with translations in
  every `generator/translations/android/` file: `designsystem_sheet_pane`, `designsystem_sheet_grabber`,
  `designsystem_sheet_expand`, `designsystem_sheet_collapse`, `designsystem_sheet_dismiss`,
  `designsystem_sheet_close_scrim`, `designsystem_sheet_state_expanded`, `designsystem_sheet_state_partial`,
  `designsystem_sheet_state_collapsed`. `designsystem_close` and `designsystem_cancel` exist
  (`strings.xml:4-5` [C]).

---

## 10. Performance contract

The reviewer's checklist. Frame budget at 120 Hz: 8.33 ms.

### 10.1 Budget per phase

| Phase | Allowed per frame | Recompositions |
|---|---|---|
| Drag (finger down) | L1 sheet + D1 surface (floating or above chrome only) + D2 dim + L0 presenter | **0** per move |
| Show, settle, hide | the same | **0** in frames 2…N−1; frame 1 composes; ≤ 1 at a settle, in readers of `currentDetent` |
| Show with a content-side provider | as Show, starting one frame later (B-2) | the slot host once, before the first moving frame |
| Predictive back progress | L0 (surface scale, content counter-scale) | **0** per event |
| IME show or hide | L1 (lift, footer) + content re-measure | **0** in content; ≤ 2 content re-measures per IME transition |
| Card stack (presenter) | L0 + D2 (merged dim); the backing is the existing fill | **0** in the shell's content |
| Floating ↔ attached | D1 (surface and clip) + L0 | 0 |
| Non-modal boundary crossing | D2 dim fade | ≤ 1 per crossing (beneath semantics) |
| Park, unpark | L1 | 0 per frame |
| Glass at a partial detent | one backdrop pass over the visible card (Live tiers only) | 0; ≤ 1 when the pass switches off or on |

### 10.2 Where each fast value is read

| Value | Written by | Read in | Class |
|---|---|---|---|
| `offset` (anchored) | drag, fling, `animateTo`, `updateAnchors` | `SheetLayout` placement (inside `withMotionFrameOfReferencePlacement`, `src/DraggableAnchors.kt:161-193` [G]); `SheetStack` fractions | L1 |
| stretch amount | `SheetStretch.applyToScroll`/`applyToFling` | placement | L1 |
| `parkProgress` | the park `Animatable` | placement; `SheetStack` | L1 |
| IME current inset, `lift`, footer y | nobody: read from `WindowInsetsRulers.Ime.current` | placement | L1 |
| IME target | placement writes a snapshot field only when an IME animation starts or ends | content box measure; `contentPadding` | ≤ 2 re-measures per transition |
| `presentationFraction`, `expansionFraction`, `recede(level)`, `dim(level)` | derived from `offset` | presenter layer and draw; covered sheet layer; surface draw; scrim draw; backdrop draw | L0, D1, D2 |
| back progress | the Back handler | the sheet layer's and the content layer's layer blocks | L0 |
| `contentScrolled` | the nested-scroll connection | the header's draw | D1 |
| `contentPadding` values | settles, footer measure, IME target | the scroller's measure | re-measure, 0 recompositions |
| `currentDetent`, `targetDetent` | `derivedStateOf` | composition (grabber `stateDescription`, caller UI) | ≤ 1 per settle |
| `isMoving`, show/hide progress | the mutex and animations | `snapshotFlow` in an effect: `HostMotion.report`; frame-rate vote | 0 |
| `blocking` (non-modal) | `derivedStateOf` | beneath semantics; scrim `sharePointerInputWithSiblings` field | ≤ 1 per crossing |

### 10.3 Forbidden in sheet code

Each is a review finding:

- reading `offset`, `presentationFraction`, `expansionFraction`, `recede`, `dim`, `contentScrolled` or back progress in
  composition (lint `FrequentlyChangingValue` is an error, M-5);
- `Modifier.offset(dp)`, or padding driven by the offset;
- `graphicsLayer { alpha }` on any node larger than a card (code rule `layer-alpha-fill`, R9);
- `BoxWithConstraints` or `SubcomposeLayout` anywhere in the sheet (material3 removed its own, `f7fe1d92`);
- allocating in a draw, layer or placement block (paths are rewound, shapes and brushes cached);
- a `Dialog` or `Popup` window;
- a tween after a finger;
- `Modifier.shadow(elevation)`, RenderNode elevation, or a `dropShadow` whose size follows the sheet (§7.7);
- `fitInside(WindowInsetsRulers.Ime)`, `imePadding()` or `windowInsetsPadding(WindowInsets.ime)` in sheet content;
- a `LaunchedEffect` that sets up first-frame state (API-13).

### 10.4 Recomposition budgets

Asserted by `RecompositionJourneysTest` (RECOMPOSITION-PLAN §3) and on the phone (§16.1).

| Journey | Budget |
|---|---|
| 30 drag moves across detents | 0 in content, header, footer, surface and presenter |
| A settle on a new detent | ≤ 1, only in readers of `currentDetent` |
| Medium → Large with the card stack | 0 in the shell's content |
| 30 back-gesture progress events | 0 |
| IME rise and fall | 0 in content |
| Show | frames 2…N−1: 0 (the slot host once before the first moving frame when a provider registered) |
| Non-modal boundary crossing | ≤ 1 |
| Park and unpark (push over the owner, pop) | 0 per frame in the sheet |
| Status-bar flip | ≤ 1 per crossing, in the edge-to-edge owner only |

---

## 11. Integration seams

### 11.1 The overlay host (WP03)

- `AppTheme` (outermost) provides `LocalSheetStack` beside `LocalOverlayHost`.
- `HostedOverlay` (`ds/component/overlay/AppOverlayHost.kt:81-100` [C]) keeps re-providing the caller's locals
  (`:92`), and gains: parking (B-14) for sheets and for every other hosted overlay (alerts, action sheets): an overlay
  whose owner's tab is hidden is not placed, drawn, hit-tested or announced, and returns in place; the bounds of a
  non-modal sheet (B-13); `hideFromAccessibility()` on what is beneath a modal overlay instead of
  `clearAndSetSemantics {}` (`:58`).
- In-screen sheets register in the `SheetStack` with level `LocalPresentationLevel.current + 1` (or their parent sheet's
  level + 1).

### 11.2 The presentation host and Navigation 3 (WP04)

- **The host builds its own strategy.** `PresentationHost` creates `SheetSceneStrategy(sessions, isPresented)` and puts
  it first in its strategies (`bottomsheet.md:157`: before any non-overlay strategy). The `sheetStrategy` parameters of
  `AppShell` and `PresentationHost` are deleted (`nav4/AppShell.kt:96`, `:200`; `PresentationHost.kt:70`, `:121` [C];
  unmerged, so no deprecation). A sheet key never falls back to a cover, so N2 cannot recur.
- **Stateless strategy.** `calculateScene(entries)`: the top entry's `metadata[PresentationKey] as? Presentation.Sheet`,
  else `null`; `null` when nothing is beneath; otherwise
  `SheetScene(entry = top, overlaidEntries = previousEntries = entries.dropLast(1), spec, sessions, isPresented, onBack)`.
  It mutates nothing: Navigation 3 calls it once per overlay level on every calculation ([V] `current-state.md` §4.2),
  which is why WP03's `states.clear()`/`retainAll` lost every lower sheet's state (N1).
- **`SheetScene`** is an `OverlayScene` with `key = entry.contentKey` and `equals`/`hashCode` over the entry's content
  key, the keys beneath and the spec (lint `SceneEqualsHashCode`). Its content:
  `PresentedSheetFrame { AppSheet(state = sessions[contentKey], onDismissRequest = onBack, modality = Modal,
  dismissible = spec.dismissible, properties = spec.properties, style = AppSheetDefaults.style(material =
  spec.material)) { entry.Content() } }` plus `LaunchedEffect(state) { state.show() }` (a no-op for a sheet already up).
- **`SheetSessions`** holds one `AppSheetState` per `contentKey`, created in composition with `spec.detents` and
  `spec.initialDetent`. A `NavEntryDecorator` removes a session in `onPop`, which runs only when the last entry with that
  key has left the back stack and composition, so after `onRemove()` has finished ([V]
  `navigation3-runtime 1.1.7 NavEntryDecorator.kt:70-88`, `DecoratedNavEntries.kt:205-223`). The decorator joins the
  host's list after the saveable-state and ViewModel decorators (`PresentationHost.kt:92-94` [C]). Its saver maps
  `contentKey.toString()` to the state's saved string (§4.4).
- **`onRemove()`**: when the entry is still presented (`isPresented(contentKey)`), return at once; otherwise
  `sessions.peek(contentKey)?.hide()`. `peek` is null-safe for a scene removed before its first composition (the
  recipe's `lateinit`, `src/nav3-recipes-BottomSheetSceneStrategy.kt:31`, is not copied). Navigation 3 keeps a popped
  overlay composed until `onRemove()` returns and renders the first scene instance it saw for a key ([V]
  `src/NavDisplay.kt:686-698`, `:874-881`), so the session map, not the scene object, is the source of truth.
- **`PresentedSheetFrame`** replaces `PresentedFrame` for sheet entries (N4): no opaque `background(colors.background)`
  and no card `graphicsLayer` (`PresentationHost.kt:162-196` [C]); `LocalTabBarVisible provides false`;
  `LocalHostMotion` = the presentation gate; `LocalPresentationLevel` = the entry's depth; `LocalEntryVisibility` true
  while nothing is presented above it. It never reads `LocalNavAnimatedContentScope`, which in an overlay scene is the
  underlying scene's ([V] `current-state.md` §4.2). `ReportMotion` is not used for sheets: the sheet reports itself
  (B-15).
- **Covers** keep `PresentedFrame`. It applies `cardStackPresenter(stack, level = depth)` inside its existing
  full-screen fill, which turns black while that level recedes (§7.9).
- `PresentationCard` asks "is this key a cover?" through `PresentationKey` (today through the string key,
  `PresentationHost.kt:74-78` [C]).
- `StackBackHandler` and `GateSettle` stay (`PresentationHost.kt:153-154` [C]); the sheet's handler runs ahead of them
  at overlay priority, and Back reaches them only while a destination sheet leaves (B-43).
- **Semantics (N8):** while a modal presentation (sheet or cover) is up, the tab hosts beneath get
  `hideFromAccessibility()`, from a derived flag: ≤ 1 recomposition per present and dismiss.
- **Routing:** `ShellState.submit` implements B-58. A deep link that changes tab leaves presented sheets up
  (`nav4/ShellState.kt:195-210` behaviour unchanged).

### 11.3 The shell and the stacks (WP04)

- `ShellChrome`'s box (tab content and bar) gets `Modifier.cardStackPresenter(stack, level = 0)`
  (`nav4/AppShell.kt:131-199`, `nav4/shell/ShellChrome.kt` [C]). The `GlassStage` lies inside the receding box. The shell
  content recomposes 0 times (RECOMPOSITION-PLAN `:193`).
- An `AppShell` option `sheetCardStack: Boolean = true` turns the card stack off app-wide (the dim stays).
- The window's background is black while `stack.recede(0) > 0`, from a derived flag (§7.9); nothing opaque lies between
  the window background and the receding box.
- The edge-to-edge owner flips the status bar while `stack.rootReceded` (§7.9).
- **`LocalHostMotion`** per host: each kept tab's stack host forwards to its `BackGate.report`
  (`nav4/back/BackGate.kt:67` [C]); the presentation host to `presented.gate`; `AppSheetNavHost` to its own gate.
- **`LocalEntryVisibility`** per stack entry (`StackHost`, `KeptTabs`): `isUncovered` is true while the entry is the
  target top of its stack and nothing is presented above its host. It turns false on the first frame of a push over the
  entry and true on the first frame of the pop that reveals it.
- **`LocalPaneBounds`**: the shell provides the content area's window bounds (excluding a rail or sidebar); each pane of
  the list-detail and supporting-pane scenes provides its own. The holder is written from `onPlaced` and read in the
  sheet's measure.
- **`LocalPresentationLevel`**: 0 in the shell; the depth in each presented entry.
- `BottomChromeWriter` gives the keyboard precedence over a presented bar (N9, `nav4/shell/BottomChromeWriter.kt:50-51`
  [C]) and takes `max(its value, stack.topBottomChrome)` so a toast over a sheet rests above its footer.
- `Chrome.kt` merge (N11): WP03's `ds/foundation/Chrome.kt` and WP04's `ds4/foundation/layout/Chrome.kt` reconcile at the
  WP03/WP04 merge on WP04's package and `TabVisibility` holder; the new seam files (§12) live beside it.

### 11.4 Chrome locals inside a sheet

| Local | Value inside the sheet | Why |
|---|---|---|
| `LocalBottomChrome` | a holder carrying the sheet's settled `contentPadding.bottom` | a screen reused in a sheet that pads with `chromeContentPadding()` (`ds4/foundation/layout/Chrome.kt:92-103` [C]) gets the same bottom as `contentPadding`; use one, never both |
| `LocalTopChrome` | a zero holder; an `AppScaffold` inside provides its own | the header is above the content, not over it |
| `LocalTabBarVisible` | false | no bar under a sheet |
| `LocalSheetSurface` | the sheet (§4.8) | `AppScaffold`, `AppTopBar`, `AppBottomActionBar` adapt |
| `LocalPresentationLevel` | the sheet's level | sheets opened from its content stack on it |
| insets | `safeDrawing.only(Top)`, `navigationBars`, `ime` consumed | B-50 |

### 11.5 Glass (WP05)

1. `GlassSheetBackdrop` implements `SheetBackdrop` with Kyant's `drawBackdrop` over the stage's `SceneBackdrop`, as
   `rememberGlassChrome` does (`glass/GlassChrome.kt:24-48` [C]): effects built once and reading no state; the tint and
   `dim()` drawn in `onDrawSurface`; the pass skipped while `opaque() == 1` or the sheet is parked; no allocation per
   frame (if Kyant needs a `Shape`, cache shapes per whole px).
2. It provides `LocalSheetBackdrop` around the presentation host and the overlay layer, both outside the recorded
   subtree, only while `GlassQuality.samplesScene`. It exposes the stage's scene to those two hosts without widening
   `LocalGlassScene` (`glass/GlassStage.kt:51-58` [C]).
3. The GPU watcher (`glass/GlassQuality.kt:96-108` [C]) watches frames while a live Glass sheet draws, and demotes the
   tier if the GPU falls behind.
4. CP2 decides `sheet.glass.blur` and `sheet.glass.bodyAlpha` from recordings.
5. It verifies sampling alignment under the receding presenter (§6.6).
6. `design/wp/WP05.md:64` and the handoff's "sheets never sample" (`handoff/WP05.md:125`) are revised to this spec.

### 11.6 Test tags

`modifier` lands on the surface, so `sheet_surface` is the sheet's root tag. Contract tags (A3.8): `sheet_surface`,
`sheet_grabber` (exists), `sheet_scrim`. The sample destination (WP02-M3) exposes `sheet_open`, `sheet_list`,
`sheet_field`, `sheet_footer`. Catalog triggers are `catalog_sheet_<demo>` (§14). The overlay host already exposes
tags as resource ids (`AppOverlayHost.kt:63-66` [C]); the presentation host does the same.

---

## 12. Packages and files

Concept packages, no `util`, `internal` or `common` packages (F-2, F-3); one public component per file with its
defaults (K-2); every file under 400 lines. Tests mirror the packages (F-6).

```
core/designsystem/src/main/kotlin/com/base/app/core/designsystem/                      (WP03)
├── theme/AppDesignStyle.kt              + SheetMaterial; AppStyle.sheetRadius, sheetInset, sheetMaterial
├── foundation/layout/
│   ├── HostMotion.kt                    HostMotion, LocalHostMotion
│   ├── EntryVisibility.kt               EntryVisibility, LocalEntryVisibility
│   ├── PaneBounds.kt                    PaneBounds, LocalPaneBounds
│   └── PresentationLevel.kt             LocalPresentationLevel
└── component/sheet/
    ├── AppSheet.kt                      the three AppSheet overloads
    ├── AppSheetState.kt                 AppSheetState, rememberAppSheetState, the Saver
    ├── AppSheetStyle.kt                 AppSheetStyle, AppSheetColors
    ├── AppSheetDefaults.kt              AppSheetDefaults
    ├── AppSheetProperties.kt            AppSheetProperties, SheetPlacement, SheetContentInteraction, SheetTransition, SheetKeyboard
    ├── SheetModality.kt                 SheetModality, SheetDismissSource
    ├── SheetDetent.kt                   SheetDetent, SheetDetentContext
    ├── AppSheetScope.kt                 AppSheetScope, LocalSheetSurface, SheetSurface
    ├── chrome/
    │   ├── AppSheetGrabber.kt           pill, hover, focus ring, semantics, tap to cycle
    │   ├── AppSheetHeader.kt            the three-slot header and its scroll-edge fade
    │   └── AppSheetCloseButton.kt
    ├── content/
    │   ├── SheetColumn.kt               SheetColumn, sheetContentPadding
    │   ├── SheetSlots.kt                ProvideSheetHeader, ProvideSheetFooter, the slot registry
    │   ├── SheetDismissGuard.kt
    │   └── SheetBackHandler.kt
    ├── layout/
    │   ├── SheetLayout.kt               the one measure-once Layout: placement, footer, padding, park
    │   ├── DetentResolution.kt          R-1…R-11 (pure Kotlin)
    │   ├── SheetGeometry.kt             floating, attach, clip rect, stretch band (pure Kotlin)
    │   ├── SheetPlacementPolicy.kt      Automatic → Bottom / Center / Start / End (pure Kotlin)
    │   └── SheetImeLift.kt              lift and target from the IME rulers; API 26-29 fallback
    ├── gesture/
    │   ├── SheetReleaseRule.kt          B-22 (pure Kotlin)
    │   ├── SheetFlingBehavior.kt        applies B-22, settles, plays the arrival haptic
    │   ├── SheetStretch.kt              the rubber-band OverscrollEffect
    │   ├── SheetNestedScroll.kt         Resizes / Scrolls connections, contentScrolled
    │   └── SheetCatch.kt                the Initial-pass catch node (B-17)
    ├── back/
    │   └── SheetBack.kt                 the handler, enablement, branches, eased progress, Escape
    ├── surface/
    │   ├── SheetSurfaceNode.kt          body, rim, Frosted brushes, content clip
    │   ├── SheetShadow.kt               the nine-slice (pure geometry + one cached bitmap)
    │   ├── SheetScrim.kt                hit-testing, semantics, pass-through, fallback dim
    │   └── SheetBackdrop.kt             SheetBackdrop, SheetFrame, LocalSheetBackdrop
    ├── stack/
    │   ├── SheetStack.kt                registry, levels, recede, merged dim (math in CardStackMath)
    │   ├── CardStackMath.kt             §6.4 formulas (pure Kotlin)
    │   └── CardStackPresenter.kt        Modifier.cardStackPresenter (one layer node + one draw)
    ├── action/
    │   ├── AppActionSheet.kt            sheet and anchored card
    │   ├── SheetAction.kt
    │   └── SheetAnchor.kt               SheetAnchor, rememberSheetAnchor, Modifier.sheetAnchor
    └── choice/
        └── AppChoiceSheet.kt            AppChoiceSheet, AppChoiceSheetDefaults

core/designsystem/.../component/overlay/          kept: AppAlert, AppMenu, AppContextMenu, AppToast, AppOverlayHost (+ parking)
core/designsystem/.../foundation/glass/GlassSheetBackdrop.kt                        (WP05, liquidglass only)

core/navigation/src/main/kotlin/com/base/app/core/navigation/                       (WP04)
├── Presentation.kt                      Presentation, SheetSpec, PresentationKey
└── presentation/
    ├── PresentationHost.kt              builds the sheet strategy; semantics beneath; routing hooks
    ├── PresentationCard.kt
    ├── PresentedFrame.kt                covers only (moved out of PresentationHost.kt); cardStackPresenter(level)
    └── sheet/
        ├── SheetSceneStrategy.kt
        ├── SheetScene.kt
        ├── SheetSessions.kt             + its NavEntryDecorator and saver
        ├── PresentedSheetFrame.kt
        └── AppSheetNavHost.kt
core/navigation/.../shell/CardStack.kt   applies cardStackPresenter(0); window backing; status-bar flip

catalog/src/main/kotlin/com/base/app/catalog/sections/sheets/                       (WP03)
├── CatalogSheets.kt                     the page and its index
├── SheetDetentDemos.kt
├── SheetBehaviourDemos.kt
├── SheetStyleDemos.kt
└── SheetPresetDemos.kt
```

Deleted: `ds/component/overlay/AppSheet.kt`, `AppSheetState.kt`, `AppActionSheet.kt` (moved and rebuilt),
`ds/component/overlay/OverlayBackHandler.kt`'s use by sheets (it stays for other overlays), WP03's
`core/navigation/SheetScene.kt` (never wired), the legacy `component/overlay/AppBottomSheet.kt` after its deprecation
release (§13), `catalog/sections/CatalogOverlays.kt`'s sheet demos.

---

## 13. Migration

Only the legacy `AppBottomSheet` and `AppActionSheet` have shipped (integration branch `8672003`, copies in WP01-WP10,
`current-state.md` §1). WP03's `AppSheet` and WP04's `Presentation.Sheet` are unmerged and change without deprecation.
The two shipped ones go through one release of `@Deprecated(level = WARNING, replaceWith = …)` (API-14), because
generated apps take template changes through `upgrade_project.py`.

| From | To | Notes |
|---|---|---|
| `if (show) AppBottomSheet(onDismissRequest, title, showHandle, dismissOnClickOutside) { … }` (a `Dialog`, `ds4/component/overlay/AppBottomSheet.kt:42-50` [C]; its content is a `ColumnScope` lambda, so `SheetColumn` takes it unchanged) | `AppSheet(visible = show, onDismissRequest, state = rememberAppSheetState(listOf(SheetDetent.FitContent)), properties = AppSheetProperties(scrimTapDismisses = dismissOnClickOutside), grabber = if (showHandle) AppSheetDefaults.Grabber else null, header = title?.let { t -> { AppSheetHeader(t) } }) { SheetColumn { … } }` | The deprecated `AppBottomSheet` is exactly this delegation for one release |
| Legacy `AppActionSheet(actions, onDismissRequest, title, message, cancelLabel)` (no `visible`) | `AppActionSheet(visible = true, actions, onDismissRequest, …)` | Deprecated overload delegates; `SheetAction.isDestructive` → `destructive` |
| WP03 `AppSheet(state, onDismiss, …, title, showCloseButton, footer)` | `AppSheet(state, onDismissRequest, header = { AppSheetHeader(title) }, footer = …)` | `showCloseButton = false` → `AppSheetHeader(title, trailing = null)` |
| WP03 `AppSheet(visible, onDismissRequest, detents: Set, title, showCloseButton, footer)` | `AppSheet(visible, onDismissRequest, state = rememberAppSheetState(detents.toList()), header = …, footer = …)` | |
| WP03 `enum SheetDetent { Medium, Large }`, WP04's enum, WP07's copy | sealed `SheetDetent` | `SheetDetent.Medium`/`.Large` references compile unchanged; `Set` → `List` |
| WP03 `AppSheetState(detents: Set)`, `remember { AppSheetState() }` | `rememberAppSheetState(detents: List)` | saveable (fixes D15) |
| `AppSheetState.largeFraction` | `expansionFraction`; the shell reads `SheetStack.recede(0)` | |
| `AppSheetState.dismissing`, `dismiss(onGone)` | `hide()` plus the caller's follow-up; leaving is `isVisible && targetDetent == null` | |
| WP03 `SheetSceneStrategy`, `SheetPresentation`, `SheetKey`, `rememberSheetSceneStrategy` | gone; the presentation host owns its strategy | |
| WP04 `Presentation.Sheet(detents)`, string key `PRESENTATION_METADATA` | `Presentation.Sheet(SheetSpec(detents = …))`, `PresentationKey` | same in WP07 |
| WP04 `AppShell(sheetStrategy = …)`, `PresentationHost(sheetStrategy)` | parameters removed | |
| `PresentedFrame` for sheets | `PresentedSheetFrame` | covers keep `PresentedFrame` |
| `HostedOverlay` modal `clearAndSetSemantics {}` | `hideFromAccessibility()` (§9) | |
| `OverlayBackHandler` for sheets | `SheetBack` (B-36, B-37) | `OverlayBackHandler` stays for alerts and menus |
| Catalog demos (`catalog/sections/CatalogOverlays.kt:61-97`, `CatalogAdaptive.kt:135-154`, `CatalogFeel.kt:119`) | `catalog/sections/sheets/` (§14); the Feel page keeps one sheet for CP recordings | the Filters demo gets a `LazyColumn` (D13) |
| Settings language picker (`feature/settings/Settings.kt:424-437`, an `AppActionSheet` of every locale) | `AppChoiceSheet` with a checkmark, search and its list remembered | fixes A1, A2, A5, QA-14 (WP02-M3) |
| Tests `SheetTest.kt`, `RecompositionJourneysTest.kt:171-186`, `PostureTest.kt:70-78` | ported to the new API; every assertion kept | |
| WP08 `appSheet(isPresented:detents:)`, `appSheet(item:detents:)` | `appSheet(isPresented:options:selection:content:)` (§18) | the template is hidden on the portal; no deprecation |
| WP07 `Presentation.kt` copy, `Spring(stiffness, damping)` sheet token | the common `SheetSpec`, `PresentationKey`; `Spring(response, bounce)` from the shared `sheet` section | §17 |

Defects closed: D1 (B-3, B-4), D2-D4 (B-45 to B-50), D5 (§7.4), D6 (B-39), D7 (§6.1), D8 (B-17), D9 (B-9), D10 (B-10,
R-11), D11 (D-19, §8), D12 (§9), D13 (D-25, `SheetColumn`, the catalog's lazy list), D14 (§4.4 springs through the
layout), D15 (§4.4 Saver), D16 (B-42); A1-A5 (§4.9, §13); N1-N5 and N10 (§11.2, B-58, B-59, §4.10), N6 (B-43), N7
(B-14), N8 (§9, §11.2), N9 and N11 (§11.3).

---

## 14. Catalog demos

The catalog's Sheets page (`catalog/sections/sheets/`) shows each demo over a colourful, scrollable backdrop so glass
and dimming read. Each demo's trigger has the test tag `catalog_sheet_<id>`, so `record.sh` and `perf.sh` can open it.

| id | Demo | Shows | Covers |
|---|---|---|---|
| `basic` | Basic | `[Large]`, `AppSheetHeader` with close, a 60-row `LazyColumn` | card stack, rubber band at the top, scroll hand-off |
| `filters` | Filters | `[Medium, Large]`, a 40-row list, a pinned Apply footer; a Resizes/Scrolls switch | B-28, B-29, B-31 |
| `rename` | Rename | `FitContent`, a field and a sticky Save; a Lift/Expand switch | B-45 to B-47 |
| `compose` | Compose message | `[Large]`, `SheetDismissGuard` with a Discard / Keep Editing `AppActionSheet` | B-33, B-34 |
| `lab` | Detent lab | `Height(96 dp)`, `Fraction(0.25 / 0.5 / 0.75)`, `Custom("sliver")`, `Large`; buttons calling `animateTo`, `expand`, `collapse`, `updateDetents`; a draw-phase readout of `currentDetent` and `targetDetent` | §4.5, B-9, B-11 |
| `maps` | Maps-style | a fake map grid behind `NonModal(upThrough = Medium)`, `[Height(120 dp), Medium, Large]`, `dismissible = false`, a search header; a FAB riding on the sheet's top through `state.offset`; Start panel on a wide window | B-51, B-52, B-38 step-down, §8 |
| `nowplaying` | Now playing | `NonModal()` mini sheet over a scrolling list | pass-through, nine-slice shadow |
| `styles` | Styles | Solid / Frosted / Glass side by side; a glass-tier switch (Live, Frosted, Solid); inset on/off; corner radius; grouped colours | §6.6, §7.6 |
| `headers` | Headers | title only; Cancel + Done; close only; subtitle; a long title at font scale 2 | §7.4, D5 |
| `stack` | Stack | sheet → sheet → sheet | B-54 to B-57 |
| `choice` | Choice | `AppChoiceSheet` with 30 choices, search and None; a multi-select recipe built on `AppSheet` | §4.9 |
| `actions` | Actions | `AppActionSheet` with destructive, disabled and selected rows; anchored card on a wide window | §4.9, A1-A5 |
| `state` | Loading | skeleton → content (`FitContent` animates) → error with retry | B-10 |
| `destination` | Destination | opens the registered sample sheet; deep link `app://catalog/sheet`; its screen sets header and footer with the providers | §11.2, §4.8 |
| `steps` | Steps | `AppSheetNavHost` with three steps | B-59 |
| `placement` | Placement | Automatic, Bottom, Center, Start, End forced; resize the window | §8 |
| `motion` | Reduce motion | `SheetTransition.CrossFade` and the reduce-motion note | B-63 |
| `a11y` | Accessibility | grabber actions and state, scrim label, focus trap with a keyboard | §9 |
| `back` | Predictive back | instructions; a guarded variant; a persistent step-down | B-38 to B-41 |

---

## 15. Tests

### 15.1 Unit (JVM, pure Kotlin; `core/designsystem/src/test/.../component/sheet/{layout,gesture,stack,surface}/`)

- `DetentResolutionTest`: every detent type; R-1 to R-11; Medium inactive in Compact height and within 48 dp of Large;
  Custom returning null; R-6 merge; R-8 fallback; R-9 nearest; tabletop fold; non-modal `restBottom` above the chrome;
  stacked `largeTop` (B-54).
- `SheetReleaseRuleTest`: every row of the B-22 table, plus boundary cases (exactly `F`, exactly `D`, exactly 0.52,
  `y == o_n`, single-detent sheet), guard and `dismissible = false` reporting one attempt.
- `SheetStretchTest`: monotonic, below the asymptote, 0.008 H at Large, the Medium-only asymptote, the blocked band.
- `SheetGeometryTest`: side inset, resting bottom and bottom edge at `e` = 0, 0.5, 1 for each case of §6.3; the clip is
  always a uniform round rect; the header inset is constant.
- `CardStackMathTest`: scale, translation and corner at `e` = 0, 0.5, 1; the merged dim equals the two-rect composite to
  1/255 per channel for a grid of `u`, `l` in both themes; `A` is monotonic in `u` and in `l`; `A = 0` draws nothing.
- `SheetSaverTest`: every key round-trips; a missing or inactive key maps to the nearest active detent.
- `SheetShadowTest`: nine-slice source and destination rects for a range of surface sizes; corners never scale.
- `SheetPlacementPolicyTest`: every row of §8, with window class versus container class (B-13).

### 15.2 Compose UI (Robolectric, clock driven, T-6; `core/designsystem/src/test/.../component/sheet/`)

| Test | Asserts |
|---|---|
| `SheetApiShapeTest` | the §4.1 blocks compile as written (a source set of call sites) |
| `SheetPresentTest` | frame 1 draws the hidden pose; the first moving frame ≤ 4 % of the travel; a provider's header and footer are composed and `FitContent` includes them before the first moving frame (B-2); a restored state snaps without a slide; `visible` false → true during the exit reverses with velocity (D1); after a person's dismissal with `visible` left true the sheet is not drawn, hit-tested or stacked (B-4) |
| `SheetGestureTest` | drag follows 1:1; a touch on the content during a settle catches the sheet (D8), and a tap that stops it does not click the row (B-17); a retarget keeps velocity, per-frame deltas never jump (D7); `animateTo` during a hide is ignored (D9); a resize mid-settle lands on the detent (D10); the haptic plays once, on the arrival frame; a row's pressed state appears within 100 ms of the down (B-21) |
| `SheetNestedScrollTest` | Resizes grows then scrolls; Scrolls scrolls first; momentum never moves the sheet; the last row is reachable at Medium with Scrolls (B-31); with mouse-only input a wheel notch never resizes the sheet (B-27); an accessibility scroll never resizes it (B-30) |
| `SheetDismissTest` | drag, flick, scrim, Back key, Escape, close button and `scope.dismiss()` each call `onDismissRequest` exactly once; code paths call it never |
| `SheetGuardTest` | every path of the B-33 table with a guard: the sheet stays and `onAttempt` gets the right source once per attempt; a disabled guard registers no handler; `dismissible = false` on a modal sheet: Back is consumed, never passed through; on a persistent non-modal sheet: Back steps down, and at the lowest detent the handler is disabled |
| `SheetBackTest` (`DirectNavigationEventInput`) | eased progress → layer `scaleX/Y`; content counter-scale; Center scales to 0.90 about its centre; cancel returns to 1; commit closes from Medium and from Large in one gesture; in-screen Back burst during the exit is dropped and nothing beneath pops; the handler is disabled while the IME target is visible and enabled from the frame it hides (B-44); only the top sheet takes Back |
| `SheetImeTest` (insets injected) | Lift: the footer rides the keyboard, a Medium sheet rises with it and stops at `largeTop`; the content box is re-measured to the visible region at the target, at most twice per transition; a focused field at Medium ends above the keyboard (D4); content without a footer clears the navigation bar (D3); the IME is counted once (D2); Expand goes to the top detent and back; a drag past 24 dp hides the keyboard |
| `SheetModalityTest` | non-modal: a tap beside the sheet reaches a sibling of the overlay host at the peek [U: the cross-sibling path, §21]; blocked above `upThrough`; the dim draws only above; ≤ 1 recomposition per crossing |
| `SheetA11yTest` | pane title, traversal group, `dialog()`, grabber role, state and actions (moved to the root when no grabber), scrim label and order, beneath hidden at both hosts, focus trapped and restored |
| `SheetAdaptiveTest` | 1280 × 800 modal → Center even when opened from a Compact pane (B-13, D-20); 900 × 400 → Bottom 640 wide, one detent; tabletop; book; non-modal → Start at Expanded width inside `LocalPaneBounds`; a placement change snaps at the same detent |
| `SheetStackTest` | the covered sheet's scale and lift follow the top sheet; the covering `largeTop` is the lifted top + 10 dp; the covered grabber hides; only the top takes input; at most two covered sheets drawn |
| `SheetParkingTest` | tab hidden → parked instantly, restored without animation (N7); a push over the owner → animated park from the push's first frame, unpark on pop; a presentation above the host parks it; a leaving sheet finishes its exit drawn; parked sheets are out of the stack and take no Back |
| `SheetShadowTest` (UI) | no shadow on a modal Bottom sheet; a non-modal sheet dragged 30 frames creates no new shadow bitmap; no draw at alpha 0; none in dark theme |
| `AppActionSheetTest`, `AppChoiceSheetTest` | selection role and checkmark; scroll past the 0.6 cap; Back during the exit consumed (A3); the anchored card at Medium width creates no window (the root view count is unchanged) and places against the anchor |

### 15.3 Navigation (WP04, `core/navigation/src/test/`, a real `NavDisplay`)

- `SheetSceneTest`: a lower sheet keeps its state and never re-presents across stack changes (N1); a pop slides out
  before the entry leaves; `onRemove` before the first composition returns; metadata through `PresentationKey`; a sheet
  over a cover recedes the cover; a cover submitted over a sheet closes the sheet first, then rises (B-58); restore after
  process death keeps the detent with no slide.
- `SheetGateTest`: a Back burst during a destination sheet's dismissal is counted, coalesced, and dropped at the
  presentation root; a push submitted while a sheet presents is held by `NavGuard`; an in-screen sheet presenting holds
  its tab's push (B-15).
- `SheetNavigationTest`: an ordinary key submitted from a presented sheet closes the sheets, then pushes on the tab
  (B-58).
- `AppSheetNavHostTest`: inner Back pops the inner stack first; from the root it closes the sheet.
- `PresentationSemanticsTest`: the tab hosts are hidden from accessibility under a modal presentation (N8).
- `CardStackShellTest`: the shell content recomposes 0 times from Medium to Large; the status bar flips once per
  crossing; the window background is black only while receded.

### 15.4 Recomposition journeys

`RecompositionJourneysTest` gains one journey per row of §10.4, with those budgets.

### 15.5 Screenshots (T-5, `com.android.compose.screenshot`)

Light and dark, font scale 1.0 and 1.5 (and 2.0 for headers), LTR and RTL:

| Size (dp) | Cases |
|---|---|
| 400 × 1000 | Medium floating (Solid, Frosted); Large attached with the card stack (a fake presenter at `e` = 0.5 and 1); FitContent; header variants; footer; stacked sheets; a guard attempt with an action sheet over it; non-modal peek above a fake bar |
| 400 × 1000, 3-button insets | Medium attached (no float), last row clear of the buttons |
| 400 × 500 | Medium and Large (Medium active at 500 dp) |
| 400 × 400 | Compact height: one detent |
| 900 × 400 | Compact height, 640 dp centred |
| 610 × 1000, 900 × 1000 | Center form sheet |
| 1280 × 800 | form sheet; Start panel |
| tabletop and book (injected posture) | fold detent; Center in one half |

Live glass cannot render in a screenshot test; it is verified on the phone (§16).

---

## 16. Phone gates, recordings and feel acceptance

### 16.1 `perf.sh` rows (WP04 adds the scenarios to `perf/perf.py` and the rows to `perf/gates.toml`)

Release builds with their baseline profile, as every row (`perf/gates.toml:1-15` [C]). Scenarios open the sample
destination by `sheet_open` (and the catalog's `catalog_sheet_maps` for the non-modal rows, `solid` only).

| Metric | solid | standard | lean | Other | Scenario |
|---|---|---|---|---|---|
| `sheet_present.janky_pct` | 1.0 | 1.0 | 1.0 | smoke `{ add = 0.5, scale = 0.2 }` | open and close 10 times |
| `sheet_present.compose_frame_ms.median` / `.max` | ratchet, then 8.0 / 16.0 once SMOOTHNESS S2 lands | same | same | | the frame that composes the sheet |
| `sheet_drag.janky_pct` | 1.0 | 1.0 | 1.0 | | grabber drags Medium ↔ Large, slow and fast, 10 each |
| `sheet_drag.gpu_p90_ms` | 7.0 | 7.0 | 7.0 | | |
| `sheet_drag.p99_ms` | 16.0 | 16.0 | 16.0 | | a drag is L1 + D1 + D2 |
| `sheet_dismiss.janky_pct`, `.dismissed` | 1.0, true | 1.0, true | 1.0, true | | flick at 3 H/s from Medium |
| `sheet_back.gesture_first_frame_ms.median` | 8.0 | 8.0 | 8.0 | | as `pushpop_gesture` |
| `sheet_back.janky_pct` | 2.0 | 2.0 | 2.0 | | commit and cancel, through `UiDevice` swipes |
| `sheet_ime.janky_pct`, `.footer_above_ime` | 2.0, true | 2.0, true | 2.0, true | | focus `sheet_field`; bounds from UI Automator |
| `sheet_scroll.janky_pct`, `.gpu_p90_ms` | 0.5, 7.0 | 0.5, 7.0 | 0.5, 7.0 | | a list at Large, as `scroll_root` |
| `sheet_cardstack.gpu_p90_ms` | 8.0 | 8.0 | 8.0 | | Medium → Large with the shell receding |
| `sheet_cardstack.no_offscreen` | true | | | | trace: no `saveLayer` larger than a bar and no stencil clip in the expansion and drag frames |
| `sheet_nonmodal.janky_pct`, `.tap_passes_through` | 1.0, true | | | | drag peek ↔ Medium; tap the map beside the sheet |
| `sheet_rapid_back.alive`, `.lands_on_presenter` | true, true | true, true | true, true | | three Back keys within 150 ms of opening |
| `sheet_restore.detent_restored` | true | | | | `am kill` at Large, relaunch; beside `restore.restored` |
| `sheet_idle.frames` | 0 | 0 | 0 | | 5 s with a sheet resting at Medium |
| `recompose.sheet_drag.max` | 0 | | | | frames that recompose while dragged |
| `recompose.sheet_settle.max` | 1 | | | | |
| `recompose.sheet_back_progress.max` | 0 | | | | |
| `recompose.sheet_ime.max` | 0 | | | | content frames that recompose during the IME animation |
| `meminfo.sheet_cycle_pss_growth_mb` | ratchet | | | | 20 open/close cycles (also catches a growing shadow cache) |
| `sheet_large.janky_pct` | | | | `large` 1.0 | form sheet present and close; Start panel drag (joins `LARGE_SCENARIOS`, `perf/perf.py:50`) |
| `sheet_drag_glass.janky_pct`, `.gpu_p90_ms`, `.no_demotion` | | | | `glass_live` 1.0, 8.0, true | the drag scenario with `SheetMaterial.Glass` (WP05 owns the column) |

### 16.2 Recordings (QA2, `record.sh`, light and dark, into `perf/sheet/`)

Present to Medium; Medium → Large by drag and by flick; a one-detent flick from Large; flick dismiss from Medium;
rubber band at Large and on a Medium-only sheet; a non-dismissible pull; a guard attempt; predictive back commit and
cancel from Medium and Large; the Back key; the IME rising and falling at Medium; the card stack; three stacked
sheets; the Maps-style sheet (peek, pass-through tap, step-down Back); parking on a push and a tab switch; the glass
tiers; font scale 2; RTL; landscape (Compact height); a tablet form sheet and Start panel (emulator: relative
evidence only); tabletop (emulator). Also a side-by-side against an iOS 26 reference for §21's items.

### 16.3 Feel acceptance from the strips

| Check | Pass |
|---|---|
| First moving frame of a present | ≤ 4 % of the travel (`cp1_edge.py`, CP1 §1 condition 1) |
| A drag | the sheet's top under the finger within one frame of lag, on every frame |
| A release | no frame in which the sheet moves against its release velocity, except a settle that reverses past a stop |
| Over-pull at Large | the top never goes above `largeTop − 0.008 H` |
| Expansion | content text never moves sideways; the floating inset closes smoothly; no corner pops |
| Dim | never lighter on a later frame while the sheet rises, light and dark |
| Card stack | the presenter peeks 10 dp above the sheet at Large; black backing, no window colour at the corners or in the floating gaps; light status icons |
| Shadows | no dark band inside any surface, light or dark; a non-modal sheet's shadow corners keep their shape while it resizes |
| Haptic | the logcat marker within ±1 frame of the arrival frame |
| Predictive back | text never squashes; the sheet never moves up during the gesture |
| Parking | no pop-in: the sheet slides with the push and back with the pop |
| IME | the focused field above the keyboard on the last frame of the rise |

---

## 17. Compose Multiplatform parity (WP07-B)

- **Same API, same package**, in `core/designsystem/src/commonMain/kotlin/com/base/app/core/designsystem/component/sheet/`.
  WP07 has no overlay component, overlay host or presentation host today (`current-state.md` §6); WP07-B ports WP03's
  overlay host and sheet, and WP04's presentation-host sheet path, to common code.
- **Shared verbatim:** the pure files (`DetentResolution`, `SheetGeometry`, `SheetPlacementPolicy`, `SheetReleaseRule`,
  `CardStackMath`, the Saver encoding); the §15.1 tests run in `commonTest` (camelCase names, K-8).
- **Tokens:** WP07 expresses springs as stiffness and damping (`wt/WP07/.../theme/tokens/DesignTokens.kt:100-119`
  [C]). Its generated tokens move to `Spring(response, bounce)` from the same `springs` and `sheet` sections, so values
  cannot drift.
- **`expect`/`actual`:**

| Piece | Android target | iOS target |
|---|---|---|
| `Threshold` haptic | `View.performHapticFeedback`, as Android | `UISelectionFeedbackGenerator.selectionChanged()` [U: against `UIImpactFeedbackGenerator(.light)`] |
| Display corner radius for the card stack's `R₀` | `WindowInsets.getRoundedCorner` (API 31+) | no public API: `0.9 · safeDrawing.top` when that is > 20 dp ([M] Flutter's heuristic, `sheet.dart:14-25`) |
| Status bar over the card stack | `WindowInsetsControllerCompat` | the hosting view controller's `preferredStatusBarStyle` [U: the CMP hook] |
| Frame rate | `preferredFrameRate` | `CADisableMinimumFrameDurationOnPhone = YES` in `Info.plist` for 120 Hz [A] |
| Clip path | float-argument `android.graphics.Path.addRoundRect` (no allocation) | Compose `Path.addRoundRect(RoundRect)`: one small allocation per drag frame, accepted on iOS only [U: no GC hitch] |
| Shadow bitmap | `DropShadowPainter` into an `ImageBitmap` | the same where the CMP version ships it [U]; otherwise a Skia blur mask filter, rasterised once |
| Back | `navigationevent` (WP07-A rebuilt the 1.2.0 pieces it needs, `handoff/WP07-A.md:39-42`) | no system Back: drag, the dim, the close button, Escape on a hardware keyboard |
| IME | `WindowInsetsRulers.Ime` | `WindowInsets.ime` [U: animation fidelity]; the same placement-only lift |
| Glass | Kyant through `SheetBackdrop` | no Kyant: Glass draws Frosted; `LocalSheetBackdrop` is null [D] |

- **Navigation:** JetBrains `navigation3-ui` 1.1.2 is androidx 1.1.7 (`wt/WP07/templates/multiplatform/gradle/libs.versions.toml:15-20`
  [C]). `OverlayScene.onRemove` exists there [V], `NavMetadataKey` exists [V] (judge-3 §5), and so does
  `NavEntryDecorator.onPop` [V] (`NavEntryDecorator.kt:70-88`): use the typed `PresentationKey` in common code and drop
  any string-key fallback.
- **Card stack:** the same `cardStackPresenter` in the common shell.
- **Gates:** the Android target on the Redmi with §16.1's `solid` rows for present, drag, back and IME; the iOS target
  is relative evidence only (no device).

---

## 18. SwiftUI parity (WP08, iOS 17+)

The native sheet already carries the motion, the card stack and the glass; the template maps the same options onto it
and does not fight it (`MOTION-PLAN.md:526-556`).

```swift
public enum AppSheetDetent: Hashable {
    case large, medium, fitContent
    case fraction(CGFloat)
    case height(CGFloat)
    case custom(PresentationDetent)          // .custom(MyDetent.self): Apple's CustomPresentationDetent
}

public enum AppSheetMaterial { case automatic, solid, frosted, glass }
public enum AppSheetModality: Equatable { case modal; case nonModal(upThrough: AppSheetDetent? = nil) }
public enum AppSheetContentInteraction { case automatic, resizes, scrolls }
public enum AppSheetPlacement { case automatic, bottom, center, leading, trailing }

public struct AppSheetOptions: Equatable {
    public var detents: [AppSheetDetent] = [.large]
    public var material: AppSheetMaterial = .automatic
    public var modality: AppSheetModality = .modal
    public var dismissible = true
    public var contentInteraction: AppSheetContentInteraction = .automatic
    public var placement: AppSheetPlacement = .automatic
    public var grabber: Visibility = .automatic
    public init(/* every property, with these defaults */)
}

extension View {
    public func appSheet<Content: View>(isPresented: Binding<Bool>, options: AppSheetOptions = .init(),
                                        selection: Binding<AppSheetDetent>? = nil, onDismiss: (() -> Void)? = nil,
                                        @ViewBuilder content: @escaping () -> Content) -> some View
    public func appSheet<Item: Identifiable, Content: View>(item: Binding<Item?>, options: AppSheetOptions = .init(),
                                        selection: Binding<AppSheetDetent>? = nil, onDismiss: (() -> Void)? = nil,
                                        @ViewBuilder content: @escaping (Item) -> Content) -> some View
    /// Keeps the enclosing sheet up while `enabled`; a swipe down calls `onAttempt`.
    public func appSheetDismissGuard(enabled: Bool, onAttempt: @escaping () -> Void) -> some View
}
```

| Option | iOS 17 | Newer, behind `#available` |
|---|---|---|
| Detents | `presentationDetents(_:selection:)`; `.fraction`, `.height`, `.custom`; `fitContent` = `.height` of the measured content (`onGeometryChange` [U: on 17], else a `GeometryReader` preference) | — |
| Modality | `presentationBackgroundInteraction(.enabled)` / `.enabled(upThrough:)` (16.4) | — |
| Content interaction | `presentationContentInteraction(.resizes / .scrolls)`; `automatic` → `.scrolls` for `nonModal` | — |
| Dismissible | `interactiveDismissDisabled(!dismissible)` | — |
| Guard | `interactiveDismissDisabled(enabled)` + a UIKit bridge that sets the presentation controller's delegate for `presentationControllerDidAttemptToDismiss` [A] | iOS 27 `dismissalConfirmationDialog` [A] |
| Material | Solid: `presentationBackground(theme surfaceElevated)`; Frosted: `.regularMaterial`; Glass: `.ultraThinMaterial` | iOS 26 Glass: no `presentationBackground`, the system glass ("remove any custom backgrounds", WWDC25 284 [A]) |
| Corners | `presentationCornerRadius(theme radius)` | iOS 26: system concentric corners (`AppSheet.swift:37-41` [C], kept) |
| Grabber | `presentationDragIndicator`; `automatic` = visible with two or more detents | — |
| Placement | default | 18: `.center` → `presentationSizing(.form)`; 27: `.leading`/`.trailing` → `presentationPlacement` |
| Cross-fade | — | 27: `navigationTransition(.crossFade)` |
| Header | `NavigationStack` + `.toolbar` (Cancel / Done; iOS 26 makes them glass) | — |
| Footer | `.safeAreaInset(edge: .bottom)` | iOS 26 `safeAreaBar` [U] |
| Haptics | none: iOS plays none at detents; the template follows the platform | — |
| Springs | programmatic detent changes in `withAnimation(theme.springs.sheet)` = `.spring(response: 0.34, dampingFraction: 1)` | — |

- The default detents change from `[.medium, .large]` (`AppSheet.swift:10` [C]) to `[.large]`.
- iOS 27 symbols need the iOS 27 SDK to compile even behind `#available`: gate them with a `#if compiler(>=…)` check for
  the Swift version that SDK ships [U].
- `SampleDetailView.swift:33-38`'s raw `.presentationDetents` moves to `appSheet`. The template stays hidden on the
  portal (REQUIREMENTS 12).

---

## 19. Work split

Order: **WP03** (API, state, tokens, seams) → **WP04** and **WP05** in parallel → **WP02-M3** sample → **QA2** on the
phone → **WP07-B** and **WP08** (they may start once WP03's API is merged and finish after QA2's decisions) →
**WP12**. Each package writes its numbers and its "rules the docs must state" in its handoff (REQUIREMENTS 25).

### 19.1 WP03: the component, its state and tokens, the catalog

**Delivers**

1. Everything under `component/sheet/` (§4.2-§4.10, §12), `SheetMaterial` and the `AppStyle` fields, and the four
   `foundation/layout/` seam files with no-op defaults.
2. The `sheet` section of `tokens/motion.json`, `sheetInset` and `sheetMaterial` in `tokens/styles.json` (§6.2), and the
   Kotlin token writer (`generator/genkit/tokens/kotlin.py`).
3. The overlay host changes (§11.1): parking for every hosted overlay, `hideFromAccessibility`, pane bounds.
4. `LocalSheetSurface` adoption in `AppScaffold`, `AppTopBar` and `AppBottomActionBar` (§4.8, §11.4).
5. The presets (§4.9) and the deprecated `AppBottomSheet` and legacy `AppActionSheet` delegations (§13).
6. The strings of §9 with translations in `generator/translations/android/`.
7. The catalog's Sheets page (§14).
8. Tests §15.1, §15.2, the design-system journeys of §15.4, and §15.5.
9. Code rules, in the project's code-rule mechanism: `fitInside(WindowInsetsRulers.Ime…)`, `imePadding()` and
   `windowInsetsPadding(WindowInsets.ime)` inside a lambda passed to `AppSheet`, `ProvideSheetHeader` or
   `ProvideSheetFooter`; `Modifier.shadow`, `shadowElevation` and `dropShadow` under `component/sheet/`.
10. The deletions of §12.

**Acceptance**

- The public API matches §4 exactly: names, parameter order, defaults, annotations; KDoc per C-4/C-5. The §4.1 blocks
  compile unchanged (`SheetApiShapeTest`).
- Every B-rule and R-rule WP03 implements is cited by at least one test (rule id in the test name or KDoc).
- §15.1 and §15.2 green; the §10.4 budgets green; the Compose compiler metrics show the sheet's public types stable and
  its composables skippable (RECOMPOSITION-PLAN gate).
- §15.5 screenshots recorded and reviewed: nothing clipped at font scale 1.5, in dark theme or in RTL.
- The code rules catch each forbidden pattern in a fixture.
- Files under 400 lines; no `util`/`internal`/`common` packages; comments only where they explain why (REQUIREMENTS 7).

### 19.2 WP04: presenter, Navigation 3 scene, predictive back and BackGate, card stack

**Delivers**

1. `Presentation.Sheet(SheetSpec)`, `PresentationKey`, and the deletion of WP04's enum and string key (§4.11).
2. `presentation/sheet/` (`SheetSceneStrategy`, `SheetScene`, `SheetSessions` with its decorator and saver,
   `PresentedSheetFrame`, `AppSheetNavHost`); the host building its own strategy; the `sheetStrategy` parameters removed
   (§11.2).
3. Covers' `PresentedFrame` with `cardStackPresenter(level)` and the black backing; `hideFromAccessibility` beneath
   modal presentations (N8).
4. The shell (§11.3): `cardStackPresenter(stack, 0)`, the window backing, the status-bar flip, the `sheetCardStack`
   option, `LocalHostMotion` per host, `LocalEntryVisibility` per entry, `LocalPaneBounds` per pane,
   `LocalPresentationLevel`; `BottomChromeWriter` (N9); the `Chrome.kt` reconciliation (N11).
5. `ShellState.submit` per B-58.
6. Back: the destination-sheet path of B-43 through the presentation gate; the handler ordering of B-36 proven in tests.
7. Perf: the §16.1 scenarios and rows (all but the `glass_live` column), `sheet_large` in `LARGE_SCENARIOS`, the
   `no_offscreen` trace check, and baseline-profile steps that open, drag and dismiss a sheet.
8. Tests §15.3, `CardStackShellTest` and the card-stack journey of §15.4.

**Acceptance**

- §15.3 green. A sheet key never renders as a cover; `AppRoot` passes no strategy.
- On the Redmi, release builds: every §16.1 row of the `solid`, `standard`, `lean` and `large` columns passes; numbers in
  the handoff and `perf/sheet/`.
- `restore` and `rapid_back` scenarios pass with a sheet presented.
- N1-N11 closed as §13 maps them.
- Depends on WP03's merged sheet API, and on WP02-M3's sample destination for the phone rows (a test-only destination
  may stand in until then).

### 19.3 WP05: the Glass style

**Delivers**

1. `GlassSheetBackdrop` implementing `SheetBackdrop`, fenced by the liquidglass marker, and the providers around both
   hosts (§11.5 items 1-3).
2. CP2's decisions on `sheet.glass.blur` and `sheet.glass.bodyAlpha`, from recordings.
3. The alignment check under the receding presenter, and `glass.opaqueAt` changed per §6.6 if it fails.
4. The Glass design style's `sheetInset` and `sheetMaterial`; `sheet.frosted.bodyAlpha` kept equal to
   `GlassDefaults.frostedColors()`.
5. The `glass_live` rows of §16.1.
6. `design/wp/WP05.md:64` and the handoff's sheet rule revised (§11.5 item 6).

**Acceptance**

- With `glass.quality = Live` on the Redmi: `sheet_drag_glass.janky_pct ≤ 1.0`, `.gpu_p90_ms ≤ 8.0`, no demotion.
- Recordings show the dim over the sample equal to the dim beside it (no undimmed patch), the ramp to opaque toward
  Large, and alignment within 1 px under the receding presenter (or the recorded `opaqueAt` change).
- The design system and every sheet test build and pass with the liquidglass feature removed.
- 0 recompositions per drag frame with Glass.

### 19.4 WP07-B: Compose Multiplatform parity

**Delivers** §17: the common sheet, overlay host and presentation-host sheet path; the `expect`/`actual` pieces; the
shared tokens; the typed metadata key.

**Acceptance**

- The common public API equals Android's, diffed signature by signature; differences only where §17 names them.
- The §15.1 tests green in `commonTest`; an Android-target subset (`SheetPresentTest`, `SheetGestureTest`,
  `SheetBackTest`, `SheetSceneTest`) green.
- The Android target passes §16.1's `sheet_present`, `sheet_drag`, `sheet_back` and `sheet_ime` rows (`solid`) on the
  Redmi; the iOS target compiles (through `bin/memwait`).

### 19.5 WP08: SwiftUI parity

**Delivers** §18 and the Swift token writer (`generator/genkit/tokens/swift.py`) for the `sheet` section.

**Acceptance**

- The API and every mapping of §18, behind the stated `#available` and compiler gates.
- The template's forbidden-pattern checks pass; `SampleDetailView` uses `appSheet`; the template stays hidden.
- Without a Mac: reviewed by reading, type-checked where possible, and the §21 WP08 items listed in the handoff.

### 19.6 QA2: phone verification and recordings

**Delivers**

1. The §16.2 recordings and the §16.3 checks per strip.
2. A full §16.1 run on the merged build (release, every column that applies to the Redmi), in `perf/sheet/summary.md`.
3. The §21 items assigned to QA2, and the taste calls (D-19 form sheet, D-30 Medium, the dim strength, the 400 dp/s
   flick, `sheetInset`) with a recommendation for the sheet checkpoint (`decisions/`).
4. A visual defect sweep: clipping at font scale 1.5 and 2.0, dark theme, RTL, 3-button navigation, tablet emulator.

**Acceptance**

- Every §16.3 check passes, or a defect is filed to its owner with the frame strip.
- Every §21 QA2 item is replaced by a measured value or a recorded decision.

### 19.7 WP12: documentation

**Delivers**, in every template (Android, Multiplatform, SwiftUI) and the generator repo:

1. `docs/COMPONENTS.md` sheet section: the API with do/don't code, the §4.1 examples.
2. `docs/RECIPES.md`: a destination sheet with `ProvideSheetHeader`/`ProvideSheetFooter`; a Maps-style persistent sheet;
   confirm on dirty; steps inside a sheet; navigating from a sheet (B-58, B-60); action and choice sheets; multi-select.
3. The smoothness and motion guides: F1-F10, the release rule, the §10 tables, the §10.3 forbidden list with these
   called out: "never let a drop shadow follow a size that animates", "no `fitInside(Ime)` or `imePadding()` in a
   sheet", "`contentPadding` or `chromeContentPadding()`, never both", "close an in-screen sheet in the event that
   navigates away".
4. The §16 gates and how to run them (`perf.sh` on a release build), with QA2's numbers.
5. `AGENTS.md`: the short enforceable rules and pre-merge checklist lines; the CMP and SwiftUI equivalents.

**Acceptance:** every rule of §2 and §10.3 appears with a do/don't snippet; numbers come from QA2's summary; every
link resolves.

### 19.8 Supporting packages

- **WP02-M3:** the sample sheet destination (`Presentation.Sheet(SheetSpec(detents = listOf(Medium, Large)))` with a
  list, a field and a footer, tagged per §11.6); the Settings language picker as `AppChoiceSheet` with its choices
  remembered; feature call sites migrated (§13).
- **WP02-M2:** hosts the code-rule mechanism WP03's rules plug into, if it lives there.

---

## 20. Phase 2 (after the core passes §16 on the Redmi)

- **`SheetTransition.Zoom(source)`**, as design-feel §5.9 designs it: a second progress `morph` (0 at the source, 1 at
  the sheet) on `springs.sheet`, composed with the offset; the surface rect is `lerp(sourceRect, sheetRect(y), morph)`
  and the corners `lerp(sourceCorner, R, morph)`; the content fades in over `morph` 0.35 to 0.85 with `ModulateAlpha`
  [U]; the dim follows `morph`; the source hides in its own layer while the sheet morphs. A finger grabbing the sheet
  mid-morph takes the offset while `morph` finishes, so nothing jumps. Dismissal by drag slides; by Back, the dim, the
  close button or code it returns into a visible source. A glass source keeps the glass body (M26). API:
  `rememberSheetSource()`, `Modifier.sheetSource(source, shape)`. Own tests and gate rows.
- Concentric bottom corners while floating, decided from a recording (a path clip below API 33).
- iOS 26-style action sheets anchored to their source on phones (with Zoom).
- Predictive back for the steps inside `AppSheetNavHost`.

---

## 21. Measure before trusting

| Item | How | Who | If different |
|---|---|---|---|
| The 400 dp/s detent flick | phone: 20 deliberate drags and 20 flicks; count wrong outcomes | QA2 | tune `release.detentFlick` |
| Dim strength and its curve from Medium to Large, light and dark | an iOS 26 recording beside ours, stepped frame by frame (the user, or public captures) | QA2 | change the scrim value sheets use (a `sheet.dim` token feeding `AppSheetDefaults.colors(scrim)`), never the §6.4 mechanism |
| iOS 26 floating inset, corner radius, glass-to-opaque curve | same | QA2, WP05 | `sheetInset`, `glass.opaqueAt` |
| iOS: a very fast flick from Large with Medium present | same | QA2 | B-22 rule 2 |
| iOS: a Medium sheet and the keyboard (lift or grow) | same | QA2 | the `SheetKeyboard` default |
| Redmi status-bar height, gesture-navigation inset, display corner radius | `adb shell dumpsys window`, a debug overlay | QA2 | worked numbers only |
| The draw-phase round-rect clip: no `saveLayer`, no stencil pass during a drag; the presenter's round-rect clip needs no offscreen buffer | Perfetto | WP03, WP04 | §7.1 fallback |
| `preferredFrameRate` stops voting when the sheet is idle | Perfetto; `sheet_idle.frames` | WP04 | remove the vote at rest |
| Back with the keyboard up closes the keyboard first; a second Back closes the sheet | phone | QA2 | B-44 |
| Whether the template's activity installs the overlay back input | test and phone | WP04 | none: B-37 holds either way |
| Glass sampling alignment under the receding presenter | phone recording | WP05 | §6.6 |
| Slot registration lands before the first moving frame | `SheetPresentTest` | WP03 | B-2 |
| Press feedback inside a draggable sheet within 100 ms | Compose test, recording | WP03 | B-21 |
| A tap that stops a settling sheet does not click the row | Compose test | WP03 | B-17 |
| `hideFromAccessibility()` hides descendants from TalkBack's linear navigation | test and TalkBack on the phone | WP03, QA2 | keep `clearAndSetSemantics` |
| A tap crosses from the overlay host to the app root through `sharePointerInputWithSiblings` | Compose test, phone | WP03 | remove the scrim's pointer node at undimmed detents (≤ 1 recomposition per crossing) |
| The focus-restore API after close | prototype | WP03 | §9 |
| The nine-slice looks the same as a `dropShadow` | screenshot side by side | WP03 | tune the slice key |
| Form sheet: slide (D-19) or scale-in (ADAPTIVE-PLAN:434) | both recorded on a tablet emulator | QA2 → the sheet checkpoint | flip D-19 |
| Medium at 0.5 or 0.55 | recording | QA2 | `detent.mediumFraction` |
| A persistent sheet: one detent per Back, or straight to the lowest (Google Maps) | `screenrecord` of Google Maps on the Redmi | QA2 | B-38 branch 3 |
| CMP: iOS inset animation, the status-bar hook, `DropShadowPainter`, the clip allocation | prototype | WP07-B | §17 |
| SwiftUI: `onGeometryChange` on iOS 17, `safeAreaBar`, the compiler gate for iOS 27 symbols | Xcode (no Mac here) | WP08 | §18 |

---

## 22. Rejected (do not build)

Reviewers flag any of these.

| Proposal | From | Why not |
|---|---|---|
| 2 H/s as the detent-to-detent flick threshold | design-api B-10, design-perf §4.3 | A Medium → Large flick then needs ≈ 1745 dp/s; 2 H/s is the dismiss rule only (D-4) |
| A stacked sheet 0.02 H higher | design-api B-36, R-1 | Puts the grabber in the Redmi's status bar and punch-hole (D-10) |
| Floating off for Solid by default | design-perf decision 15 | The default sheet would lose the iOS 26 inset (D-6) |
| Call-site `onDismissAttempt` and per-path `SheetDismissal` flags | design-perf §3.3-3.4 | Unreachable from a destination's screen; `byBack = false` on a modal sheet gives a dead Back (D-11) |
| Public `SheetBackBehavior.StepDown` | design-perf §3.3 | One gesture closes (D-12) |
| Non-modal sheets drawn in place inside the screen | design-api decisions 1, 3, B-27-B-29 | Never Glass, under the bar, the bar left bright under the dim (D-1) |
| Per-level "sheet layers" | design-perf decision 1 | A cross-package rewrite that was never specified (D-1) |
| Extra Back presses on an in-screen sheet popping the screen beneath | design-perf §4.8 | A reflexive double press must not cost the screen (D-13) |
| Popping a destination only after its slide | design-perf §4.8 | Breaks the `OverlayScene` contract; keeps the entry in the stack ≈ 0.5 s (D-13) |
| A dim that lightens toward Large | design-feel §5.5, design-api §4.4 | D-8, unless an iOS 26 recording shows iOS does it (§21) |
| Form sheet as a scale-in on `dialog` with no drag | design-api, design-perf, ADAPTIVE-PLAN:434 | D-19 (taste; revisited at the checkpoint) |
| A placement morph on resize or fold | design-feel §4.13 | A resize is not navigation; the rect would chase a live resize (B-10) |
| 0.008 H over-pull above a top detent that is not Large | design-api B-8 | D-5 |
| A navigation results store | design-api §2.9 | D-32 |
| A header hairline | design-api §5.4 | iOS 26 uses a scroll-edge fade (§7.4) |
| Any `Dialog` or `Popup` window, including the action sheet's popover | all | D-22 |
| `@JvmInline value class` detents in the sealed type | design-perf §3.1 | Always boxed through the interface |
| A `dropShadow` sized to the sheet, or reusing `glassSurface`'s shadow path | all three designs | D-18 |
| `SheetStyle.Automatic` = Glass whenever glass ships | design-feel §3.3 | D-17 |
| Zoom from a source in v1 | design-feel §3.5, §5.9 | D-31 |
| `Modifier.sheetBleed()` and measuring content at the floating width | design-feel §0 #5 | D-7 |
| Concentric bottom corners in v1 | design-api §4.6, §5.1 | Path clip below API 33; minSdk 26 (§4.12) |
| Padding the sheet's content box instead of `contentPadding` | design-api §2.2, §3.4 | REQUIREMENTS 17 and 34: insets go in `contentPadding` (D-25) |
| `fittingHeight = Unspecified` unless FitContent is listed | design-perf §3.1 | `AppChoiceSheet`'s custom detent would resolve to NaN; `contentHeight` is always measured |
| A full-screen black rect behind the receded shell | design-api §4.4 | An extra full-screen fill; the existing fill is recoloured (§7.9) |
| A sheet-only dim token at 0.20 / 0.48 now | design-feel §0 #23 | Keep `colors.scrim` until QA2 compares (§21) |
| A cover presented as an overlay scene above a sheet | judge-2 §2.2 option 1 | B-58 closes the sheet first (D-23) |

---

## 23. Sources

**Project.** `REQUIREMENTS.md:125-136`; `research/sheet/current-state.md` (§2-§12), `research.md` (§0-§6),
`design-feel.md`, `design-api.md`, `design-perf.md`, `judge-1.md`, `judge-2.md`, `judge-3.md`;
`research/CODE-STANDARDS.md` §2-§3 (API-1 to API-15, M-1, M-5, K-2, K-4, K-8, F-2, F-3, F-5, F-6, T-5, T-6, L-1, L-5);
`research/MOTION-PLAN.md` (§1, §2, M1, M6, M17, M22, M26, M27); `decisions/CP1.md:28-60`, `:112-140`;
`research/predictive-back/SPEC.md:60-87`, `:395-425`; `research/RECOMPOSITION-PLAN.md` R7, §3;
`research/ADAPTIVE-PLAN.md:430-437`, `:565`; `research/SMOOTHNESS-PLAN.md` S2, S3, S8, S10; `notes/INDEX.md`;
`handoff/WP05.md:105-242`.

**Code read for this spec** (worktree heads WP03 `8ea3985`, WP04 `230f894`, WP05 `d549b2a`, WP07 `99239af`, WP08
`a95111a`): `ds/component/overlay/AppSheet.kt:21`, `:132`, `:211`, `:227`, `:232`, `:236`, `:335-343`;
`ds/component/overlay/AppOverlayHost.kt:55-94`; `ds/component/overlay/AppMenu.kt:40-42`;
`ds/foundation/adaptive/WindowClass.kt:16`, `:66`; `ds/foundation/adaptive/InputTraits.kt:11-15`;
`ds/foundation/adaptive/AppLayout.kt:15-66`; `ds/theme/AppTheme.kt:107-110`, `:217-235`;
`ds/theme/tokens/DesignTokens.kt:249`, `:292`; `ds/foundation/interaction/AppHaptics.kt:27`, `:59`;
`wt/WP03/tokens/motion.json` (whole), `tokens/styles.json:2-56`; `wt/WP03/generator/genkit/tokens/`;
`ds4/foundation/layout/Chrome.kt:31-89`; `nav4/presentation/PresentationHost.kt:49-205`; `nav4/AppShell.kt:84-200`;
`nav4/Presentation.kt:11-18`; `nav4/back/BackGate.kt:17-109`; `nav4/back/StackBackHandler.kt:67-70`;
`glass/GlassQuality.kt:22-47`, `glass/GlassSurface.kt:75-125`; `perf/gates.toml` (whole), `perf/perf.py:50`.

**Library facts relied on** (verified by the design authors or judges, as cited): Compose UI 1.12.1
(`OutlineResolver.android.kt:263-288`, `SemanticsProperties.kt:972`, `:1022`, `:1075`, `:1149`, `:1632-1660`,
`FrameRate.kt:71`, `Placeable.kt:501`, `WindowInsetsRulers.kt:48-97`, `:231-250`, `NestedScrollModifier.kt:294`,
`PointerInputModifierNode.kt:73-79`, `FocusProperties.kt:188`, `draw/Shadow.kt:154`); ui-graphics 1.12.1
(`ShadowRenderer.kt:70-79`, `DropShadowPainter.kt:76-78`, `:118-122`, `:217-260`, `AndroidShadowContext.android.kt:53-84`);
foundation-layout 1.12.1 (`RulerAlignment.kt:36-50`); foundation 1.12.1 (`AnchoredDraggable.kt:410-479`, `:871`,
`:1001-1029`, `:1483-1514`, `:1551-1566`, `:1716-1735`); navigationevent 1.1.2 (`NavigationEventProcessor.kt:272`,
`:579`, `NavigationEventDispatcher.kt:169-170`, `NavigationEventHandler.kt:126`, `OnBackInvokedInput.android.kt:38-48`);
navigation3-runtime 1.1.7 (`NavMetadataKey.kt`, `NavEntryDecorator.kt:70-88`, `DecoratedNavEntries.kt:205-223`);
navigation3-ui 1.2.0 (`NavDisplay.kt:686-698`, `:874-881`).

**Apple [A].** HIG Sheets https://developer.apple.com/design/human-interface-guidelines/sheets ; WWDC25 323
https://developer.apple.com/videos/play/wwdc2025/323/ ; WWDC25 284 https://developer.apple.com/videos/play/wwdc2025/284/ ;
Adopting Liquid Glass https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass ; WWDC21 10063
https://developer.apple.com/videos/play/wwdc2021/10063/ ; WWDC19 224 https://developer.apple.com/videos/play/wwdc2019/224/ ;
WWDC18 803 https://developer.apple.com/videos/play/wwdc2018/803/ ; WWDC24 10145
https://developer.apple.com/videos/play/wwdc2024/10145/ ; `UISheetPresentationController`
https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller ; `presentationDetents`
https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:) ; `presentationBackgroundInteraction`
https://developer.apple.com/documentation/swiftui/view/presentationbackgroundinteraction(_:) ; `presentationPlacement`
https://developer.apple.com/documentation/swiftui/view/presentationplacement(_:) ; `dismissalConfirmationDialog`
https://developer.apple.com/documentation/swiftui/view/dismissalconfirmationdialog(_:shouldpresent:actions:) ;
`presentationControllerDidAttemptToDismiss`
https://developer.apple.com/documentation/uikit/uiadaptivepresentationcontrollerdelegate/presentationcontrollerdidattempttodismiss(_:) ;
ProMotion on iPhone
https://developer.apple.com/documentation/quartzcore/optimizing-promotion-refresh-rates-for-iphone-13-pro-and-ipad-pro
(not fetched for this spec).

**Google [G].** Material 3 bottom sheets https://m3.material.io/components/bottom-sheets/specs , `/guidelines`,
`/accessibility`; `WindowInsets.getRoundedCorner`
https://developer.android.com/reference/android/view/WindowInsets#getRoundedCorner(int) ; Compose performance
https://developer.android.com/develop/ui/compose/performance/bestpractices ; nav3 recipes
https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/bottomsheet ; androidx and
MDC sources pinned in `research/sheet/sources/`; androidx commits cited by short SHA at
`https://github.com/androidx/androidx/commit/<sha>`.

**Third party [M].** Flutter's Cupertino sheet (iOS 18 measurements)
https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/cupertino/sheet.dart
