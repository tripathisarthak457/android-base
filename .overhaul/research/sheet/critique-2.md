# Sheet spec: adversarial critique 2 (API, accessibility, adaptive, completeness)

Written 2026-10-13 against `research/SHEET-SPEC.md` (2774 lines, read in full) and its inputs in `research/sheet/`
(`design-*.md`, `judge-1..3.md`, `research.md`, `current-state.md`, the pinned `sources/`). This replaces the earlier
draft of this file: every claim carried over was checked again, and 25 findings are new. They are marked **new** in
the summary.

The lens covers:

- the androidx Compose component API guidelines, as `research/CODE-STANDARDS.md` §2 adopts them;
- TalkBack, keyboard and switch access;
- window size classes, foldables and ChromeOS;
- Multiplatform and SwiftUI parity;
- whether each acceptance criterion can be tested;
- anything an implementer could read two ways.

No repository or worktree was changed; no Gradle or adb was run.

**Tags.**

- [C] project code, `file:line`, at worktree heads WP03 `87a3a06`, WP04 `230f894`, WP06 `123b275`, WP07 `99239af`
  and WP08 `4a4bd4d`.
- [V] verified this session in a sources jar under `~/.gradle/caches/modules-2/files-2.1/` (Compose 1.12.1) or in
  `research/sheet/sources/`.
- [A] Apple documentation, as quoted in `research/sheet/research.md`.
- [D] derived here.
- [U] unverified: measure or read before relying on it.

`SPEC:n` is a line of `SHEET-SPEC.md`. `ds/`, `ds4/` and `nav4/` are the spec's path prefixes.

**Severity.**

- **blocking**: the spec is wrong or silent in a way that makes a correct build unlikely. Fix it before WP03 starts.
- **major**: a real app or a requirement breaks, or two implementers would build different things.
- **minor**: polish, guideline conformance, or a fix that is cheaper now than after generated apps depend on it.

## Summary

| # | Sev | Section | One line | |
|---|---|---|---|---|
| C2-01 | blocking | §9, B-42, D-22 | Focus never enters a windowless modal sheet opened by touch or code; Escape and TalkBack follow | |
| C2-02 | blocking | §7.2, §11.4, D-20 | Sheet metrics and the content's adaptive locals come from the caller's pane | |
| C2-03 | blocking | B-62, §11.1 | `rememberSaveable` in an in-screen sheet never restores (rotation, fold, process death) | new |
| C2-04 | major | B-14 | Parking reads `LocalTabVisible`, which WP04 already turns false on every push | new |
| C2-05 | major | B-14, B-60, REQ 24 | An in-screen sheet ignores its owner's predictive pop and vanishes when its call site leaves | new |
| C2-06 | major | §3.2, B-54, §6.4 | A modal sheet opened beside a non-modal one shares its level; the dim lands under the non-modal sheet | new |
| C2-07 | major | §3.3, B-38-B-40 | A Back gesture during a present or settle "reverses into the dismissal" before the person commits | new |
| C2-08 | major | B-63, §6.1 | "Remove animations" scales every Compose animation to 0: the 200 ms fade and the settle snap | new |
| C2-09 | major | R-6, §4.5, D-6, §6.3 | A full-height FitContent: which detent identity, is it Large, does it float, does the stack recede? | new |
| C2-10 | major | §4.4, §4.8, B-4 | `hide()`/`show()` on a `visible` sheet desync it; four close verbs with no rule | |
| C2-11 | major | B-33, B-37 | Back on a dismissible non-modal sheet is undefined | new |
| C2-12 | major | B-31, B-47, §9 | Rows below the visible bottom: keyboard focus lands off screen, TalkBack cannot reach them | new |
| C2-13 | major | §9 | The pane title is "Sheet" unless the header is `AppSheetHeader(title: String)` | |
| C2-14 | major | §4.9 | The anchored action card has no accessibility model | |
| C2-15 | major | §16, §19.6 | No TalkBack, Switch Access or keyboard run on the phone | |
| C2-16 | major | §8, R-10, B-10 | Detents under Center placement, and across fold and unfold, are undefined | |
| C2-17 | major | B-45-B-48, §8 | No keyboard rule for Center form sheets and Start/End panels | |
| C2-18 | major | §8 | The modal side sheet (REQUIREMENTS 33) is not specified | |
| C2-19 | major | §8 | Postures: tabletop contradicts itself; book ignores the half's class; explicit placements cross hinges | |
| C2-20 | major | B-51, B-13 | A pane-bounded non-modal sheet's dim, blocking and semantics scope | |
| C2-21 | major | B-27, B-20 | Resizes vs Scrolls decided per window; touchpad and stylus input not covered | |
| C2-22 | major | §6.4, §11.3 | Card stack: no per-sheet opt-out, SurfaceView content, recedes on desktop widths | |
| C2-23 | major | §7.2, §4.8 | No horizontal content inset: QA-17 reopens under the floating clip | |
| C2-24 | major | §4.8 | The content slot has no layout: several root children are undefined | new |
| C2-25 | major | D-31, §4.5, §4.6 | Sealed and enum public types: D-31's "not a breaking change" is false | new |
| C2-26 | major | §7.3, §4.7 | No full-bleed top content (artwork, maps); the container can only be a colour | new |
| C2-27 | major | §4.3 | The in-place `AppSheet(state, …)` overload is a same-name trap | |
| C2-28 | major | §4.5 | A `Custom` detent keeps its first resolver forever | |
| C2-29 | major | §4.6, B-51 | `upThrough` inactive or unlisted; non-modal Center undefined | |
| C2-30 | major | §7.3, §7.4 | The grabber's 48 dp target covers 17.5 dp of the header | |
| C2-31 | major | §4.3, §4.8 | A custom grabber loses click, actions, state, focus and hide rules; its colour has no source | |
| C2-32 | major | §4.2 | `modifier` on the surface: an undeclared Deviation with no size/offset contract | |
| C2-33 | major | §4.12 | No switch to stop dragging (signature pad, map, payment in progress) | |
| C2-34 | major | §4.4, §14 | No window-space visible frame for UI that rides the sheet | |
| C2-35 | major | §4.5, §7.4 | No header height for peeks; header + footer at font scale 2.0 in a short window leave no content | |
| C2-36 | major | §4.9 | `AppChoiceSheet`'s default `key = { it }` crashes for a non-Bundle type | |
| C2-37 | major | B-59, §4.8 | Steps inside a sheet: provider precedence, header motion, FitContent height undefined | |
| C2-38 | major | §13, §19 | The DevTools panel (a `Dialog` sheet) is missing from the migration | |
| C2-39 | major | §18, §17 | §18 is stale against WP08's head; no parity matrix; presets have no SwiftUI form | |
| C2-40 | major | §4.8, §18 | The dismiss guard is callback-shaped, iOS 27's is dialog-shaped; no shared convenience | |
| C2-41 | major | §15.5, API-13 | Previews and screenshot tests render no sheet | |
| C2-42 | major | §4.1, §15.2 | `SheetApiShapeTest` cannot compile the destination block, and the blocks break the line limit | |
| C2-43 | major | §15.2 | `SheetImeTest` cannot animate the IME in Robolectric | new |
| C2-44 | minor | §4.10 | Seams are public with nothing to keep features out | |
| C2-45 | minor | §4.7, §6.2 | Public surface wider than its uses; the Swift writer emits tokens nothing reads | |
| C2-46 | minor | §4.6, §4.11 | Public data classes; `AppSheetColors` has no `copy` | |
| C2-47 | minor | §4.7, §7.7 | Which `Shadow`?; the nine-slice ignores spread, brush and blend | |
| C2-48 | minor | §7.7 | A modal sheet over a faint scrim has no edge in light theme | |
| C2-49 | minor | §7.4, §9 | `dialog()` does nothing for TalkBack; title not a heading; dead close button; nodes unnamed | |
| C2-50 | minor | §9 | Custom actions instead of the built-ins; no click label; grabber contrast 1.9:1 | |
| C2-51 | minor | §4.8 | Content-side API asymmetries (providable local, scope-less provider, subtitle) | new |
| C2-52 | minor | B-52, §14 | A sheet resting on the tab bar vs the bar's minimise and accessory slot | new |
| C2-53 | minor | §4.6, B-27 | Automatic → Scrolls for non-modal sheets may not match Maps or iOS | new |
| C2-54 | minor | §4.1 | The Glass acceptance example never shows glass at rest | new |
| C2-55 | minor | B-6, B-62 | A restored detent against the caller's `visible`; a cold deep link to a sheet | |
| C2-56 | minor | §4.9 | Action-sheet rows read as a fixed 56 dp | |
| C2-57 | minor | §4.9 | `AppChoiceSheet`: supporting text, disabled rows, search, `onSelect(T?)`, who closes | |
| C2-58 | minor | §4.11 | `pane` + `presentation` undefined; a destination cannot change its grabber or colours | |
| C2-59 | minor | §4.3, §19.7 | No completion hook for the `visible` overload; no result-returning recipe | |
| C2-60 | minor | §17 | CMP iOS: VoiceOver mapping, the edge swipe, string defaults | |
| C2-61 | minor | §19.1, §19.4 | "Matches §4 exactly" has no tool; rules are not mapped to owners and tests | |
| C2-62 | minor | §16 | Feel checks need instruments nobody delivers; two gate rows are under-defined | |
| C2-63 | minor | §7.9 | Navigation-bar icon contrast over a sheet under 3-button navigation | new |
| C2-64 | minor | §21, §4.3, §8 | Internal inconsistencies an implementer will trip on | new |

---

## A. Accessibility and input focus

### C2-01 (blocking) Focus never enters a windowless modal sheet

**Problem.** D-22 drops the `Dialog` window that gives material3's sheet its focus isolation and its TalkBack entry.
§9 moves focus into the sheet only "opened with keyboard input (`InputMode.Keyboard`)" (`SPEC:1888-1890`). When the
sheet is opened by touch, mouse or code:

- A focused text field beneath keeps focus and its IME. Typed characters go to it. B-37's "IME target hidden" gate and
  B-45's lift then follow a keyboard that is not the sheet's.
- `focusProperties { onExit = { cancelFocusChange() } }` only traps focus that is already inside. Tab and arrow
  traversal start from the node beneath, and `hideFromAccessibility()` does not make nodes beneath unfocusable.
- B-42's premise, "a modal sheet traps focus (§9), so the key always reaches it" (`SPEC:1383-1384`), is therefore
  false. Key events go to the focused node's path, so the root's `onPreviewKeyEvent` never sees Escape. Escape then
  reaches the sheet only through the platform's Escape → Back fallback (`ds/component/overlay/OverlayBackHandler.kt:24-25`
  [C]) and is reported as `Back`, never `Escape`.
- When focus *is* inside, `onPreviewKeyEvent` at the root steals Escape from focused children: a search field that
  clears its query, a step inside `AppSheetNavHost`.
- MDC moves TalkBack into its sheet: it hides siblings and sends `TYPE_VIEW_FOCUSED` on the sheet
  (`sources/mdc/BottomSheetBehavior.java:2520`, `:2537` [V]). The spec focuses nothing in touch mode, so TalkBack stays
  on the trigger it has just hidden.
- Restore: the APIs exist, although §21 lists "the focus-restore API" as [U]. `Modifier.focusRestorer`
  (`ui/focus/FocusRestorer.kt:109` [V]) and `FocusRequester.saveFocusedChild()`/`restoreFocusedChild()`
  (`FocusRequester.kt:156`, `:174` [V]) are in ui 1.12.1. Clickables are not focusable in touch mode
  (`Focusability.kt:66-67` [V]), so restoring to the trigger works only in keyboard mode.

**Fix.** Add **B-42a Focus on present**. It applies to every modal present in every input mode, and to a non-modal
sheet when it starts blocking (B-51):

1. On the first moving frame, call `saveFocusedChild()` on a `FocusRequester` attached to the app-root focus group
   (the overlay host's content `Box`, `ds/component/overlay/AppOverlayHost.kt:57-62` [C]).
2. Move focus to the sheet surface: a `focusGroup()` + `focusable()` node that carries the pane title, never a text
   field, so no IME opens. This clears the field beneath, closes its IME and emits `TYPE_VIEW_FOCUSED`.
3. While a modal sheet is up, give the app-root group `focusProperties { onEnter = { cancelFocusChange() } }`, read
   from a plain field.
4. On close, call `restoreFocusedChild()` in keyboard mode. For the action sheet, return focus to the `SheetAnchor`.
   In touch mode, where TalkBack lands is [U]; C2-15 records it.
5. Handle Escape in `onKeyEvent` on the surface, not `onPreviewKeyEvent`, so focused children see it first. Report
   `SheetDismissSource.Escape` only from there.

Tests in `SheetA11yTest`:

- opened by touch, the surface is focused;
- a focused field beneath loses focus and its IME target hides;
- Tab cannot leave the sheet;
- Escape closes the sheet with source `Escape`;
- on close, focus is restored in keyboard mode.

Replace the §21 row with these APIs.

### C2-12 (major) Rows below the visible bottom are unreachable by keyboard and TalkBack

**Problem.** The content is measured at Large's height (§7.2, B-31). At a partial detent, the lower part of a
`LazyColumn` lies below the window, or under the footer, yet the list believes it is in its viewport.

- **Keyboard (ChromeOS).** Tab moves focus to a row placed below the window. Its `bringIntoView` asks the list, which
  sees the row inside its viewport and does not scroll. Focus is now off screen. B-47 fixes the viewport only while
  the IME is up.
- **TalkBack.** Compose drops a semantics node whose clipped bounds miss the root, or that a later important node
  covers (`ui/semantics/SemanticsOwner.kt:274-275`, `:318` [V]). Rows below the window and rows under the footer
  are not in the tree. TalkBack scrolling never resizes the sheet (B-30). Whether its page scroll reveals those rows,
  or skips them because the list's viewport is taller than what shows, is [U].

**Fix.**

- Add **B-47a**. The content box carries a `BringIntoViewModifierNode` (`ui/relocation/BringIntoViewModifierNode.kt:32`
  [V]). A request whose rect ends below the visible bottom (the footer's top, the keyboard or the window) first moves
  the sheet to the lowest active detent that reveals it, else the tallest. It then forwards the request to the parent.
  This holds for Resizes and Scrolls alike, never during a drag, and it is not a person's move, so no haptic and no
  `onDetentChange`.
- While touch exploration is on, `Automatic` content interaction resolves to `Scrolls`, so B-31's reach padding
  applies and TalkBack can page to every row.

Tests:

- `SheetA11yTest`: at Medium, Tab past the last visible row expands the sheet and the row ends visible;
- `SheetA11yTest`: with TalkBack's page-forward action, every row of a 60-row list is visited at Medium;
- QA2 confirms both on the phone (C2-15).

### C2-13 (major) The sheet has no name unless the header is `AppSheetHeader(title: String)`

**Problem.** `paneTitle` is "the header's title, else `designsystem_sheet_pane` ('Sheet')" (`SPEC:1879`). Only the
String overload of `AppSheetHeader` can supply it. These all announce "Sheet":

- the `@Composable title` overload (`SPEC:834-840`);
- custom headers, such as §4.1's `SearchHeader()` (`SPEC:218`);
- sheets with no header, such as §4.1's `NowPlaying` (`SPEC:229-231`).

A caller who adds `semantics { paneTitle = … }` through `modifier`, which lands on the same surface node (`SPEC:258`),
collides with the sheet's own value, and no precedence is stated.

**Fix.**

- Add `paneTitle: String? = null` to the three `AppSheet` overloads and to `SheetSpec` (API-12: accessibility is part
  of the API).
- Add a `title: String` parameter to `ProvideSheetHeader`.
- Precedence: explicit `paneTitle`, then `AppSheetHeader(String)`, then the string resource. The sheet's semantics
  are chained after `modifier`, and the KDoc says which wins.
- `SheetA11yTest` covers each source.

### C2-14 (major) The anchored action card has no accessibility model

**Problem.** At Medium width and up, the action sheet is a card with "no dim; pointer input outside the card is
consumed" (`SPEC:986-990`). Nothing hides what is beneath from accessibility, gives the card a `paneTitle`, moves
focus or offers dismiss. Consuming pointer input does not stop TalkBack: a double tap performs `ACTION_CLICK` directly
on a node beneath, so a TalkBack user can trigger controls under a modal card. The card has no Cancel row either, so
"dismiss" has no visible target.

**Fix.**

- The card is modal for semantics: what is beneath gets `hideFromAccessibility()`.
- The card gets `paneTitle = title ?: designsystem_actions_pane` and a built-in `dismiss(label)` action.
- Focus moves to the first action on present (C2-01) and returns to the `SheetAnchor` on close.
- Test in `AppActionSheetTest`.

### C2-15 (major) No accessibility verification on the phone

**Problem.** §16.2, §16.3 and §19.6 have no TalkBack, Switch Access, keyboard-only or large-text run. §21 has one
TalkBack row, although REQUIREMENTS 33 names TalkBack actions. Robolectric cannot show any of these:

- TalkBack's focus movement and pane announcements;
- explore-by-touch reaching what is beneath;
- TalkBack's own Back gesture, a key-style Back (B-41), interacting with B-44.

**Fix.** Add **§16.4 Accessibility acceptance**: QA2, on the Redmi with TalkBack on, for each catalog demo.

- focus lands in the sheet and the pane title is spoken;
- the swipe order is grabber, header, content, footer, then "Close sheet";
- the actions menu offers Expand, Collapse and Dismiss, and each works;
- nothing beneath is reachable by swipe or explore-by-touch, except the Maps demo below `upThrough`;
- every row of the long list is reachable at Medium (C2-12);
- after close, focus lands at or near the trigger [U];
- a guarded sheet's confirmation is announced;
- Switch Access reaches the grabber's actions;
- on an emulator with a hardware keyboard: Tab is trapped, Escape closes, focus is restored.

Add `SheetA11yInstrumentedTest` with `enableAccessibilityChecks()` for labels and targets.

### C2-49 (minor) `dialog()` is a no-op for TalkBack; the title is not a heading; other details

**Problem.**

- §9 lists `dialog()` as an accessibility measure (`SPEC:1879`). In ui-android 1.12.1, `IsDialog` is defined and set
  (`SemanticsProperties.kt:180`, `:1150` [V]) but read nowhere else in the module: TalkBack never sees it. It is a test
  marker, and its merge policy throws if a caller's `modifier` merges descendants on the same node (`:183-187` [V]).
- The title is not `heading()`. TalkBack users move by headings.
- The default `trailing = { AppSheetCloseButton() }` (`SPEC:831`) shows a dead close button on a `dismissible = false`
  sheet that has no guard.
- "Sheet root: `isTraversalGroup`, `traversalIndex = 0f`, `paneTitle`" (`SPEC:1879`) does not say which node. The
  scrim's `traversalIndex = 1f` only orders it when its parent is the traversal group, as material3's outer `Box` is
  (`sources/ModalBottomSheet.kt:141` [V]).
- The spec does not say when what is beneath is hidden and restored: at the show's start or arrival, and at the
  dismissal's decision or when the sheet is gone.

**Fix.**

- Keep `dialog()` for tests only and say so. The pane title (C2-13) and the focus move (C2-01) are what TalkBack uses.
- The title gets `heading()`.
- `AppSheetCloseButton` emits nothing while the sheet is statically non-dismissible and no guard is registered.
- Name the nodes:
  - `SheetLayout` is the traversal group;
  - the surface carries `paneTitle`, `traversalIndex = 0f`, and the grabber's actions when no grabber is drawn;
  - the scrim carries `traversalIndex = 1f`;
  - inside the surface, the order is grabber, header, content, footer.
- What is beneath is hidden from the first moving frame of a present, and restored on the frame a dismissal is
  decided (B-7), so TalkBack can land on the trigger while the sheet leaves.

### C2-50 (minor) Built-in actions, click label, grabber contrast, the accessibility dismiss source

**Problem.**

- §9 says "custom actions *Expand*, *Collapse*, *Close sheet*" (`SPEC:1880`) but cites the built-in
  `expand`/`collapse`/`dismiss`. material3 uses the built-ins (`sources/BottomSheet.kt:368-392` [V]). Custom actions
  lose `ACTION_EXPAND`, `ACTION_COLLAPSE` and `ACTION_DISMISS`, which Switch Access and Voice Access know.
- The grabber's click has no label. MDC names the next state (`sources/mdc/BottomSheetDragHandleView.java:82-87` [V]).
- `contentTertiary` at α 0.45 measures 1.9:1 on `surfaceElevated` in light theme and 2.1:1 in dark [D, from
  `ds/theme/tokens/DesignTokens.kt:348`, `:356`, `:391`, `:399`]. WCAG 1.4.11 asks 3:1 for a control, so ATF will flag
  it.
- `SheetDismissSource` (`SPEC:606`) has no value for TalkBack's dismiss action, or for the anchored card's tap
  outside. Which source each reports is undefined.

**Fix.**

- Use `expand(label)`, `collapse(label)` and `dismiss(label)`.
- Label the click with the state it moves to.
- Document the contrast exception (drag anywhere, plus the actions), keep the API 36 high-contrast path, and suppress
  only that ATF check, with the reason.
- The accessibility dismiss reports `Request`; the anchored card's outside tap reports `Scrim`. State both in B-33.

---

## B. Adaptive, foldables and ChromeOS

### C2-02 (blocking) Sheet metrics and the content's adaptive locals come from the caller's pane

**Problem.**

- `AppTheme.layout` and `AppTheme.containerClass` follow the caller's pane (`ds/theme/AppTheme.kt:110-111`,
  `ds/foundation/adaptive/WindowClass.kt:60-72` [C]).
- `HostedOverlay` re-provides every local of the caller (`ds/component/overlay/AppOverlayHost.kt:89-92` [C]).
- D-20 fixes only the placement choice.

What follows:

- **The sheet's own metrics.** §7.2 reads `layout.formSheetWidth`, `layout.gutter` and `layout.supportingPaneWidth`,
  and `AppSheetDefaults.style(maxWidth = AppTheme.layout.sheetMaxWidth)` is evaluated in the caller's composition
  (`SPEC:753`). A modal form sheet opened from a Compact list pane on a Large window is therefore 540 dp wide with
  16 dp gutters, not 600 dp with 32 dp (`ds/foundation/adaptive/AppLayout.kt:41-46` [C]).
- **The content's locals.** Content inside a sheet inherits the caller's container class, `AppLayout`, spacing and
  `LocalPaneContext` (role, `beside`; `ds4/foundation/layout/Chrome.kt:81-89` [C]). In a 360 dp Start panel or a
  540 dp form sheet on an Expanded window, it lays out as Expanded (`hasRoomForTwoPanes`, readable widths), and a
  screen reused in a sheet shows its "beside" UI. That breaks REQUIREMENTS 23.
- §11.4's table of locals has no rows for these.

**Fix.**

1. In §7.2, every sheet metric comes from `AppLayout.forClass(placementClass)`, resolved inside the sheet. The
   placement class is the window class for a modal sheet and the pane class for a non-modal one.
   `AppSheetStyle.maxWidth` defaults to `Dp.Unspecified`, meaning the placement's own width.
2. Add §11.4 rows:
   - inside the sheet, `ProvideContainerWidth(sheetWidth)` (`WindowClass.kt:60` [C]) supplies the container class,
     the layout and the gutter. `sheetWidth` comes from the window class and the placement, so it is known in
     composition and changes only at class crossings.
   - `LocalPaneContext` is reset to `PaneContext()`.
3. `SheetAdaptiveTest` asserts `AppTheme.containerClass` inside each placement:
   - a Start panel on 1280 × 800 is Compact;
   - a Center sheet on 1280 × 800 is Compact;
   - a non-modal Bottom sheet on 900 × 1000 is Medium.

### C2-16 (major) Detents under Center placement, and across fold and unfold

**Problem.** A Center form sheet has "one stop" (R-10, `SPEC:578`). The spec does not say:

- what `currentDetent` and `targetDetent` report;
- whether `onDetentChange` fires;
- what `animateTo(Medium)`, `expand()`, `collapse()` and `updateDetents` do;
- what "snaps to the new frame at the same detent" means when unfolding moves a sheet from Bottom to Center and
  folding moves it back (B-10, `SPEC:1187-1188`; §8, `SPEC:1860`).

An app that branches on `currentDetent == Large` behaves arbitrarily on tablets, and after a fold the detent is
undefined.

**Fix.**

- Under Center, `currentDetent` reports the tallest active listed detent, and `expand()` and `collapse()` return
  false.
- `animateTo(d)` records `d` as the detent to return to and does not move.
- `onDetentChange` never fires.
- On a Center → Bottom change the sheet lands on the recorded detent, else the tallest.
- Add `SheetAdaptiveTest` rows for unfold and fold with the sheet at Medium.

### C2-17 (major) No keyboard rule for Center form sheets and Start/End panels

**Problem.** B-45 to B-48 assume a bottom-anchored sheet ("a Medium sheet rises with the keyboard", `SPEC:1406-1407`).
Tablets have soft keyboards. Center's resting `y` is never defined (centred in the safe area? where does the slide-up
end?), so where the card goes when the IME rises, whether its content shrinks, and where its footer sits are all open.
A Start/End panel has no rule either. iPadOS lifts the form sheet.

**Fix.** Add **B-45a**:

- **Center** rests at `safeTop + (safeHeight − height) / 2`. With the keyboard up, it lifts in placement by
  `clamp(cardBottom − (H − K) + 16 dp, 0, cardTop − safeTop)`. Past that, the content box shrinks to the visible
  region (B-47) and the footer rides the keyboard.
- **Start/End**: the panel's bottom edge rides `H − K − side.inset`, and its content box shrinks.

Add `SheetImeTest` cases at 900 × 1000 (Center) and 1280 × 800 (Start), and a screenshot with an IME inset.

### C2-18 (major) The modal side sheet is not specified

**Problem.** REQUIREMENTS 33 asks for "a centered form sheet or side sheet on larger windows" (`REQUIREMENTS.md:132`).
Start/End is specified only as the non-modal Maps panel: vertical detents, "what is beside it stays usable", and
predictive back "as Bottom" (`SPEC:1375`, `:1855`, `:1866-1868`). An explicit modal `Start`/`End` has no rule for:

- the dim and blocking;
- the dismiss gesture: a downward drag on a full-height side panel is odd, and MDC dismisses side sheets toward their
  edge (`sources/mdc/SideSheetBehavior.java`);
- the predictive-back pose: MDC's side container values are already pinned in `research/predictive-back/SPEC.md:80`
  (shrink 24 dp, grow 12 dp, y 48 dp);
- RTL;
- whether detents apply.

Two consumers need it already: filter panels on tablets, and the DevTools panel (C2-38).

**Fix.** Add a "Modal Start/End" row to §8:

- a full-height panel with one stop, like Center, and no grabber;
- the dim and blocking, with what is beneath hidden from accessibility;
- it slides in from its edge on `springs.sheet`, and a horizontal drag toward the edge dismisses it (2 W/s flick, or
  52 % of the width);
- predictive back uses the side-container values, the swipe edge respected (REQUIREMENTS 32);
- RTL mirrored.

The non-modal Start panel keeps its vertical detents. Add tests, a `placement` demo case and 1280 × 800 screenshots.

### C2-19 (major) Postures: a contradiction, two gaps, and explicit placements across a hinge

**Problem.**

- **Tabletop.** The row says "never a card across the crease" and "card stack at Large" in one cell (`SPEC:1856`).
  Only Medium is parked on the fold (R-2), so Large, and any FitContent, Height or Fraction detent taller than the
  lower half, straddles the hinge.
- **Book.** The row picks "the larger [half]; the end half when equal" from the window class (`SPEC:1857`). The half
  may itself be Compact (about 420 dp on a Fold's inner screen), where a centred card fits worse than a bottom sheet
  inside that half. Nor does it say that "end" follows the layout direction.
- **Flat separating hinge.** `WindowPosture(Kind.Flat, hinge != null)` is possible (`WindowPosture.kt:12` [C]) and is
  not covered.
- **Explicit placements.** "Explicit `Bottom` / `Center`: honoured in every window" (`SPEC:1858`). On a book posture,
  a 640 dp Bottom sheet or a Center card then sits across the hinge.

**Fix.** Key the rules on the separating hinge, not on the posture name.

- **Horizontal hinge:** every partial detent's top is clamped to `foldBottom` or lower. Large stays allowed, as the
  person's choice, with the dim but no card stack.
- **Vertical hinge (book, or a flat dual screen):** the sheet goes in one half. That is the half holding the presenting
  pane or anchor (`LocalPaneBounds`), else the larger half, else the end half in layout direction. Placement then runs
  again with that half's own class.
- Explicit placements are honoured within the chosen half.

Update `SheetPlacementPolicyTest` and the posture screenshots.

### C2-20 (major) A pane-bounded non-modal sheet: the scope of its dim, blocking and semantics

**Problem.** A non-modal sheet's bounds are its pane (B-13, `SPEC:1197-1199`). Above `upThrough` it "blocks like a
modal one", and the dim is "drawn per frame by the presenter" (`SPEC:1445`). An in-screen sheet's presenter is level 0,
the whole shell, with the rail, the other pane and the bar. The spec does not say whether blocking, the dim,
`hideFromAccessibility` and Back cover the pane or the window. A Maps-style sheet in the list pane that expands would
dim and block the detail pane.

**Fix.** A pane-bounded non-modal sheet dims and blocks only its pane.

- Its scrim node fills `LocalPaneBounds` and draws the merged dim itself, because `SheetStack.dim(level)` covers a whole
  presenter.
- Hiding semantics applies to that pane's subtree, through a per-pane flag that WP04 provides.

State this in B-51 and B-53. Test it in `SheetModalityTest` with a list-detail scene.

### C2-21 (major) Resizes vs Scrolls is decided per window; touchpad and stylus are not covered

**Problem.**

- `Automatic` chooses Scrolls only when `finePointer && !touchscreen` (`SPEC:1296-1297`). Touchscreen Chromebooks and
  tablets with a keyboard-cover trackpad report `touchscreen = true` (`ds/foundation/adaptive/InputTraits.kt:11-15`
  [C]). A wheel notch or a two-finger scroll there grows the sheet, which is exactly what B-27 exists to prevent.
- Android delivers touchpad two-finger scrolls as synthetic finger events with a two-finger-swipe classification [U].
  A touchpad scroll in a sheet may therefore drag the sheet itself (B-20: "a mouse or trackpad drags the sheet like a
  finger").
- Stylus input (REQUIREMENTS 23) is not mentioned: a stylus drag, or handwriting that starts on a text field inside a
  draggable sheet.

**Fix.**

- Decide per gesture. The B-17 `Initial`-pass node records the last `PointerType`, and a `PointerEventType.Scroll`
  event or a two-finger-swipe classification counts as Mouse. The Resizes connection gives upward deltas to the sheet
  only after a Touch or Stylus down.
- Only a primary-button press-and-drag or a single finger moves the sheet with a pointer device.
- Stylus drags the sheet like a finger, except that stylus handwriting on a text field wins over the sheet's drag.

Tests: with `touchscreen = true`, a wheel notch never resizes; a two-finger touchpad scroll scrolls the list; stylus
handwriting in a field does not move the sheet.

### C2-22 (major) Card stack: no per-sheet opt-out, SurfaceView content, desktop widths

**Problem.**

- The card stack can be turned off only app-wide (`sheetCardStack`, `SPEC:2036`). Some screens should never recede,
  such as a video call.
- A Large sheet over a camera preview, Media3's `PlayerSurface` (a SurfaceView by default) or a SurfaceView map: the
  presenter's layer scale and round-rect clip probably do not apply to the SurfaceView's own surface [U]. The preview
  would stay full size with square corners while the rest recedes.
- An explicit `Bottom` at Expanded width recedes too (`SPEC:1652-1653`). On a 2560 dp desktop window, the whole shell
  shrinks to 91.65 %, which iPadOS never does.

**Fix.**

- Add `AppSheetProperties.recedesPresenter: Boolean = true`, also reachable through `SheetSpec`.
- Recede only at Compact window width; elsewhere draw the dim only.
- Add a RECIPES note on SurfaceView and on `PlayerSurface(surfaceType = TextureView)` under a receding presenter, and a
  §21 row for QA2 to record it.

### C2-23 (major) No horizontal content inset contract: QA-17 reopens

**Problem.** QA-17 reports that sheet body text starts at x = 0 (`qa/r1/DEFECTS.md:41` [C], still Open). Content is
measured at the full width and clipped by `i₀` each side while floating (D-7, `SPEC:1736-1740`). `contentPadding`
documents only the bottom and the insets (`SPEC:793-799`). Text without a gutter of its own therefore loses 8 dp at
partial detents, which is REQUIREMENTS 34's "cut by clip shapes". D-7's "nothing is clipped" holds only for content
whose gutter is at least `i₀`. QA-17 is missing from §13's list of defects closed.

**Fix.**

- `contentPadding` carries start and end padding equal to the sheet's gutter (C2-02). Full-bleed rows lay themselves
  out edge to edge.
- `SheetColumn` applies the gutter.
- Add a §15.5 case: a bare `AppText` at Medium, floating.
- List QA-17 under the defects closed.

---

## C. Lifecycle, hosting and state

### C2-03 (blocking) `rememberSaveable` inside an in-screen sheet never restores

**Problem.** B-62 promises "Content state: the content's own `rememberSaveable`" (`SPEC:1511`). The overlay host keys
each overlay by its entry object:

- `state.overlays.forEach { entry -> key(entry) { entry.content() } }` (`ds/component/overlay/AppOverlayHost.kt:66`
  [C]);
- `OverlayEntry` is a plain class (`:46`), created anew in each `DisposableEffect` (`:91`).

The compound key takes `dataKey.hashCode()` for a non-enum key (runtime 1.12.1 `GapComposer.kt:3095-3096` [V]), and
`rememberSaveable` keys its value by that compound hash (runtime-saveable 1.12.1 `RememberSaveable.kt:81-87` [V]). An
identity hash differs after every recreation, so a value saved in a sheet is never found again.

The activity declares no `configChanges` (`app/src/main/AndroidManifest.xml:44-48` [C]), so this hits on every
rotation, fold and unfold, and freeform resize across a configuration, as well as process death. A half-typed rename
form in a sheet is lost when the phone rotates, which breaks REQUIREMENTS 23's "fold/unfold continuity". material3
avoids this with a saveable-id tag on its dialog window (`research.md:423`); D-22 removed that window.

**Fix.**

- In §11.1, every `HostedOverlay` gets a stable id saved at the call site (`rememberSaveable { newId() }`).
- The overlay host composes each entry under `key(id)` inside a `SaveableStateHolder.SaveableStateProvider(id)`
  (`SaveableStateHolder.kt:72-78` [V]), whose holder is saved in the caller's registry.
- When the call site leaves for good, `removeState(id)` runs.
- B-62's row then holds. Add to `SheetPresentTest`: a field's text survives activity recreation, with a
  `StateRestorationTester`. Add a §16.1 row: rotate with the rename sheet open; the text and the detent are kept.

### C2-04 (major) Parking reads `LocalTabVisible`, which WP04 turns false on every push

**Problem.** B-14 parks "instantly, with no animation" when "the owner's tab is hidden (`LocalTabVisible`,
`ds4/foundation/layout/Chrome.kt:48-52`)", and with a slide when the owner is covered (`LocalEntryVisibility`)
(`SPEC:1202-1206`). But WP04 provides `LocalTabVisible` per stack entry as
`TabVisibility { tab.isVisible && key in scene.top }` (`nav4/stack/StackLayout.kt:123`, `:136` [C]), and its KDoc
defines it as "its tab is in front and nothing covers it in its stack" (`ds4/foundation/layout/Chrome.kt:40`, `:48-52`
[C]). A push
over the owner therefore turns `LocalTabVisible` false too: an implementer following B-14 parks instantly on every
push, the pop-out B-14 exists to avoid. The tab-level holder (`KeptTabs.kt:120`) is shadowed by the per-entry one, so
nothing a screen reads means "tab hidden" alone. `LocalEntryVisibility` (§4.10) also duplicates the per-entry holder.

**Fix.**

- WP04 exposes two distinct values to every entry: whether its tab is selected, and whether the entry is uncovered.
  Either fold both into `EntryVisibility(tabSelected, uncovered)` and keep `LocalTabVisible` as their conjunction for
  clocks, or rename the tab-level holder `LocalTabSelected`.
- B-14 then reads "tab not selected → instant park; selected but covered → animated park".
- `SheetParkingTest` asserts that a push parks with a slide, never instantly.

### C2-05 (major) An in-screen sheet ignores its owner's predictive pop and vanishes when its call site leaves

**Problem.**

- **Predictive pop of the owner.** Take a persistent non-modal sheet at its lowest detent: its Back handler is
  disabled (B-37). Back then pops the owner, and REQUIREMENTS 24's Gmail card shrinks the owner and follows the finger.
  The sheet lives in the overlay host (layer 4) above the shell. It is parked only once the entry stops being the
  target top (§11.3, `SPEC:2042-2044`), which happens at commit. During the whole gesture the sheet stays full size on
  top of the shrinking card. A dismissible non-modal sheet has the same problem once C2-11 lets Back through.
- **Call site removal.** `HostedOverlay` removes its entry in `onDispose` (`AppOverlayHost.kt:98` [C]). The spec does
  not say what a visible sheet does when its call site leaves composition. Examples: `if (loaded) AppSheet(...)`, a
  sheet declared in a `LazyColumn` item that scrolls away, an owner removed without a pop animation. The sheet vanishes
  in one frame. If it was receded, the presenter jumps upright. `HostMotion` and Back are cleaned up by nothing the
  spec names.

**Fix.**

- **B-14a.** While the owner entry is the outgoing entry of a predictive pop, the sheet's park progress follows the
  gesture's eased progress: it slides down with the card. Cancel unparks it with the system's spring, and commit
  finishes the park. WP04 exposes the outgoing entry's back progress as a seam (`EntryVisibility.backProgress`, read in
  placement).
- **B-4a.** A sheet whose call site leaves composition while visible is removed in that frame. It unregisters from the
  `SheetStack`, and the presenter un-recedes in the same frame. It reports `HostMotion(false)` and restores focus. Add
  a code rule against `AppSheet` inside `if` blocks or lazy items, and a docs line: keep the call composed and drive
  `visible`.
- Tests: `SheetParkingTest` with `DirectNavigationEventInput` on the owner's stack: the sheet's placed `y` follows the
  progress. Disposing the call site removes the sheet from the stack.

### C2-06 (major) A modal sheet opened beside a non-modal one from the same owner

**Problem.** "A sheet's level is its owner's level + 1" (§3.2, `SPEC:141`). A Maps screen with a persistent sheet
(level 1) that opens a modal in-screen sheet, or an `AppActionSheet` for a tapped pin, creates a second sheet at
level 1. B-54 covers only a sheet opened from a sheet's content. Then:

- The merged dim is drawn by the presenter, the shell at level 0, inside its own layer (§6.4, `SPEC:1665-1679`). That
  is beneath the overlay host, where the persistent sheet lives. The persistent sheet stays bright and undimmed between
  the dim and the new modal sheet.
- `recede(0)` reads "the sheet directly above it" (`SPEC:1652`), and B-37's "top-most visible sheet" is ambiguous with
  two sheets at one level.
- Destination sheets do not hit this, because the owner is covered and the in-screen sheet parks (B-14). In-screen
  modal sheets and presets do.

iOS presents from the top-most presented controller, so a new sheet stacks over the persistent one.

**Fix.**

- Add **B-54a**: a sheet opened at level `L` while a visible non-modal sheet sits at level `L` takes level `L + 1`,
  and the non-modal sheet becomes its presenter (covered-sheet transform, B-54).
- A modal sheet's dim is drawn by every presenter between it and the nearest lower modal sheet, or the shell: here,
  both the shell and the persistent sheet, each in its own layer. Each draw is one rect, so the cost stays within §10.
- Test in `SheetStackTest`: a persistent sheet at its peek, plus an action sheet from the screen. The persistent sheet
  is dimmed, it takes no input, and Back closes the action sheet first.

### C2-07 (major) A Back gesture during a present or a settle

**Problem.** The state table says that Back on a Presenting sheet "reverses into the dismissal, velocity kept"
(`SPEC:158`). That is right for the Back key. For a predictive gesture, a reversal at `onBackStarted` would slide the
sheet away before the person commits, and a cancel could not bring it back. B-38 to B-40 assume a resting sheet.
REQUIREMENTS 32 asks for a Back swipe at any moment, mid-push included, from the current position.

**Fix.**

- At `onBackStarted` during Presenting or Settling, stop the motion at the current offset (B-17's catch,
  `PreventUserInput`) and run the shrink from there.
- On cancel, settle by B-22 with `v = 0` from that offset.
- On commit, exit from the shrunken pose with `lastVelocity` (D7).
- The key path keeps the table's reversal. Split the table's Back column into "Back key" and "Back gesture".
- Test in `SheetBackTest`: start the gesture 60 ms after `show()`, cancel it, and the sheet lands on its detent; commit
  it, and the sheet leaves without jumping.

### C2-08 (major) "Remove animations" scales every Compose animation to zero

**Problem.**

- The design system's reduce motion is `ANIMATOR_DURATION_SCALE == 0` (`ds/theme/ReduceMotion.kt:41-42` [C]).
- Compose's `MotionDurationScale` follows the same setting (ui-android 1.12.1 `WindowRecomposer.android.kt:97`,
  `:427-446` [V]), and every suspend animation scales its duration by it.

So with "Remove animations" on, B-63's fade "in over `fades.longMs` (200 ms)" and its 120 ms fade-out take no time.
The table's "Drag, settle, rubber band: unchanged" is false too: the settle spring finishes in one frame. Tests that
set `AppTheme(reduceMotion = true)` (`ds/theme/AppTheme.kt:88`, `:103-104` [C]) leave the duration scale at 1, so they
pass while the phone snaps.

**Fix.** Decide in B-63 and test the decision.

- **Recommended:** honour the setting. Under "Remove animations", present and dismiss are instant, and the dim and
  card stack jump to their end values. Drags still follow the finger. A release snaps to its detent.
- `SheetTransition.CrossFade` (not reduce motion) keeps its 200/120 ms fades under the normal scale.
- If the team wants a fade even with the setting on, run the fade in `withContext(MotionDurationScale(1f))` and say
  so, because it overrides the person's setting.
- Tests set the real setting through `Settings.Global` in Robolectric, not only the theme flag.

### C2-09 (major) A full-height FitContent: identity, Large-ness, floating and the card stack

**Problem.**

- R-6 merges stops "into the first in list order, so `[FitContent, Large]` with a full-height list is one stop"
  (`SPEC:574`). The merged stop's identity is then `FitContent`.
- `expansionFraction` is 0 "when Large is not active" (`SPEC:466-469`), and D-6 floats "a sheet with a partial
  detent".

So `[FitContent, Large]` and `[Large, FitContent]` differ visually for the same content: one floats inset with no card
stack, the other attaches and recedes. `[FitContent]` alone with a tall list sits at Large's height, but is it partial
(floating, no stack) or Large? FitContent's KDoc says it "resolves to Large" (`SPEC:522`) without saying whether that
means the height or the identity. The §13 migration maps every legacy `AppBottomSheet` to `[FitContent]`, so this is
the common case.

**Fix.** Add **R-6a**:

- A stop whose offset is within 1 px of Large's is treated as Large for `expansionFraction`, floating, the card stack
  and the glass ramp, whatever detent it came from.
- When Large is listed, merges resolve to `Large`'s identity. Otherwise the first in list order is kept, but it is
  Large-equivalent.

Add `DetentResolutionTest` cases for both list orders and `[FitContent]` with a 60-row list, and screenshots.

### C2-10 (major) `hide()`/`show()` on a `visible` sheet desync it

**Problem.** There are four ways to close a sheet:

- `state.hide()`, with no callback;
- `scope.dismiss()`, which calls `onDismissRequest` without asking a guard;
- `scope.requestDismiss()`, guarded;
- `visible = false`.

§4.11 points features at `LocalSheetSurface…sheetState`. A caller of the `visible` overload who calls `state.hide()`
is left with `visible == true` and a hidden sheet. `LaunchedEffect(visible)` (B-4, `SPEC:1169`) never fires again, so a
later `visible = true` shows nothing: the sheet is stuck. The mirror case is `show()` while `visible == false`.

**Fix.**

- For the `visible` and `item` overloads, the caller's value is the truth:
  - a `hide()` that completes while `visible` is still true calls `onDismissRequest` once;
  - `show()` while `visible` is false returns at once.
- Add a "which to call" table to §4.8:
  - Done → `dismiss()`;
  - Cancel or close → `requestDismiss()`;
  - code → `visible = false`.
- Test in `SheetDismissTest`.

### C2-11 (major) Back on a dismissible non-modal sheet is undefined

**Problem.** B-37's fourth condition covers "a modal sheet" and "a persistent non-modal sheet" (`SPEC:1353-1355`).
B-33's table has columns for modal sheets and for a non-dismissible non-modal one (`SPEC:1321`). Neither covers the
catalog's own `nowplaying` demo, a `NonModal()` sheet with `dismissible = true` (`SPEC:2231`). It is undefined whether:

- its handler is enabled at an undimmed detent;
- Back at a tab root closes it or plays back-to-home;
- a tap beside it is a dismissal.

**Fix.**

- Add a B-33 column "non-modal, dismissible" and a B-37 clause:
  - Back is enabled and closes it (predictive dismiss) at every detent;
  - at an undimmed detent, a tap beside it passes through and never dismisses;
  - above `upThrough`, it behaves as a modal sheet.
- If Back should reach the screen beneath instead, say so explicitly.
- Test in `SheetGuardTest`.

---

## D. API shape and customisation

### C2-24 (major) The content slot has no layout: several root children are undefined

**Problem.** "Content. `AppSheetScope` receiver, no layout scope" (`SPEC:910`). The spec's own §4.1 rename example
emits two root children, `SheetDismissGuard(...)` and `SheetColumn { … }` (`SPEC:207-208`), and any feature will emit
a banner plus a list. Whether they overlap (a `Box`), stack (a `Column`), or throw, is not said. material3 gives its
content a `ColumnScope` (`sources/ModalBottomSheet.kt:110` [V]). Two implementers will build different things, and
the migration claims a legacy `ColumnScope` lambda goes "unchanged" into `SheetColumn` (`SPEC:2191`).

**Fix.** State it in §4.3 and §4.8:

- Content is laid out in a `Box` measured with Large's height as its max, `propagateMinConstraints = false`, children
  stacked at the top start, as `Scaffold`'s body is.
- A content that emits nothing visible (providers, guards) is fine.
- The KDoc says to bring one root, a `LazyColumn` or `SheetColumn`.
- Add a lint-style code rule: more than one layout emitter at the root of an `AppSheet` content lambda.

### C2-25 (major) Sealed and enum public types will grow; D-31's "not a breaking change" is false

**Problem.** D-31 says "`SheetTransition` is sealed so it is not a breaking change" to add `Zoom` (`SPEC:87`; also
`judge-2.md:74`, `:218`). Adding a subtype to a sealed hierarchy, or a value to an enum, breaks every exhaustive `when`
in callers. Since Kotlin 1.7, a non-exhaustive `when` statement over a sealed or enum subject is an error. The spec
plans additions already:

- `SheetTransition.Zoom` (§20);
- a header-height detent (C2-35);
- `SheetDismissSource` values for side sheets and the anchored card (C2-18, C2-50);
- possibly `SheetPlacement` and `SheetKeyboard` values.

Generated apps take changes through `upgrade_project.py` (API-14).

**Fix.**

- For types callers only pass in (`SheetTransition`, `SheetPlacement`, `SheetKeyboard`, `SheetContentInteraction`),
  use a `@JvmInline value class` with companion constants, as Compose does for `Role` and `TextAlign`. Alternatively,
  document that `when` over them needs `else`, and add a code rule that enforces it in generated code.
- For `SheetDismissSource` and `SheetDetent`, which callers branch on, keep the sealed or enum type, and state in KDoc
  and §4.12 that new values will be added and `when` must carry `else`.
- Correct D-31's rationale.

### C2-26 (major) No full-bleed top content; the container is only a colour

**Problem.**

- "With no header, content starts at `ContentTop` (15 dp)" (`SPEC:716`, `:1754`), so artwork or a map can never reach
  the sheet's top edge or its rounded corners. Apple Music's full player, App Store and Maps place cards do exactly
  that, with the grabber drawn over the image. §4.1's own `NowPlaying` example is that case.
- It is unclear whether the 15 dp still applies when `grabber = null`.
- `AppSheetColors.container: Color` (`SPEC:675`) rules out a gradient or brand brush on the surface. The content box
  cannot paint it either, because the header and grabber band lie outside it and the floating clip shapes the surface.

**Fix.**

- Add `AppSheetProperties.contentUnderGrabber: Boolean = false`. When true, the content box starts at the surface top,
  and the grabber draws over it with a contrast-aware pill (white α 0.7 over media).
- State that `ContentTop` applies only when a grabber is drawn and there is no header.
- Let `AppSheetStyle` take an optional `containerBrush: Brush?`, drawn by the surface node, cached by width and radius
  as §7.6 caches its brushes.
- Add a catalog `media` demo and screenshots.

### C2-27 (major) The in-place overload is a same-name trap

**Problem.** `AppSheet(state, onDismissRequest, …)` (`SPEC:340-360`) draws in place, under the tab bar, and never
appears without `state.show()`. The app-facing overloads also take `state`. So an agent's natural
`AppSheet(state = sheetState, onDismissRequest = …) { }` resolves to the presenter variant. Same-named composables
should differ only in convenience [U: the exact wording in androidx `compose-component-api-guidelines.md`].

**Fix.**

- Rename it `AppSheetLayout` and move it to `component/sheet/layout/`.
- Gate it with C2-44's opt-in.
- Add a `SheetApiShapeTest` fixture showing that `AppSheet(state = s, onDismissRequest = {}) {}` cannot compile
  without `visible` or `item`.

### C2-28 (major) A `Custom` detent keeps its first resolver forever

**Problem.** Custom detents are equal by `id` (`SPEC:540-544`), and `rememberAppSheetState` calls `updateDetents`
"only when the list differs" (`SPEC:493-494`). A resolver that captures changing values, such as
`Custom("peek") { headerHeight + extra }` or one that reads a ViewModel flag, therefore never replaces the first
lambda, although the KDoc invites creating it in composition.

**Fix.**

- On every `SideEffect`, the state stores the latest resolver for each id, as a plain field write.
- The next measure re-resolves, and the anchors are written only if the values changed (R-11).
- Test: changing a captured value moves a resting sheet to the new height.

### C2-29 (major) `upThrough` inactive or unlisted; non-modal Center undefined

**Problem.**

- Medium is inactive in Compact height (R-2). §4.1's Maps example uses `upThrough = Medium` (`SPEC:216`), so on a phone
  held sideways B-51's boundary `y_U` is undefined.
- Nothing requires `upThrough`, `initialDetent` or `SheetSpec.initialDetent` to be among the detents.
- An explicit `Center` non-modal sheet is "honoured" (`SPEC:1858`) but has no undimmed detent rule.

**Fix.**

- Add `require(upThrough == null || upThrough in detents)`, and `require(initialDetent in detents)` in `AppSheetState`
  and `SheetSpec`.
- An inactive `upThrough` maps to the highest active detent whose height is at most its nominal height, else to the
  lowest active detent.
- A non-modal Center sheet is undimmed only when `upThrough` is null.
- Add a `SheetModalityTest` case at 900 × 400.

### C2-30 (major) The grabber's target covers 17.5 dp of the header

**Problem.** The pill is 36 × 5 dp, 5 dp from the top, so its centre is at 7.5 dp. Its 48 × 48 target (`SPEC:1746-1748`)
spans −16.5 to 31.5 dp. The header starts at 14 dp (`SPEC:1759`). The grabber "takes no layout room" and is drawn over
the header, so it swallows the top 17.5 dp of the header's centre: §4.1's search field, a segmented control. It also
reaches 16.5 dp above the sheet, into the dim or, for a non-modal sheet, the map.

**Fix.**

- The touch target spans the pill's centre ± 24 dp horizontally, but vertically only the band above the header's top
  (0 to 14 dp). The 48 × 48 rect stays the semantic and focus bounds.
- A drag starting in that band drags the sheet, and a tap there cycles detents.
- Test: a tap at (centre, 20 dp) reaches the header.

### C2-31 (major) A custom grabber loses its behaviour; the grabber's colour has no source

**Problem.**

- "The default hides itself …" (`SPEC:295-296`, `:901-904`) implies that a custom `grabber` lambda is always shown and
  owns nothing. Who then provides the click (B-19), the hover, the focus, the `stateDescription`, the actions, the
  48 dp target and the hiding rules? A team that only restyles the pill loses every TalkBack action.
- `AppSheetGrabber(modifier)` takes no colour (`SPEC:815-817`), and `SheetSurface` carries no grabber colour
  (`SPEC:896`). How it reads `style.colors.grabber` is undefined.

**Fix.**

- The sheet owns a grabber node for hit-testing, focus, click, hover, semantics and visibility. The slot only draws
  inside that node. `null` removes the drawing and moves the semantics to the surface.
- `AppSheetGrabber` reads its colour from the enclosing sheet's style through the internal slot registry, and draws
  `contentTertiary` outside a sheet.
- Test: a custom grabber keeps its actions.

### C2-32 (major) The `modifier` contract

**Problem.** `modifier` is "applied first to the sheet surface" (`SPEC:258-259`). That departs from API-3 ("the
outermost layout") without the **Deviation** marker CODE-STANDARDS requires (`CODE-STANDARDS.md:7`). Nothing says how
these interact with a surface whose size and position the sheet owns:

- `height` and `fillMaxHeight`;
- `padding`: inside or outside the floating clip?
- `offset`, `clip` and `graphicsLayer`.

**Fix.**

- Mark it as a Deviation. material3 applies `modifier` to its sheet too (`sources/ModalBottomSheet.kt:159` [V]).
- State that `modifier` is for test tags, semantics and focus, and that the sheet's own size and placement win.
- Add a code rule that flags size, offset, padding and layer modifiers passed to `AppSheet(modifier = …)`.

### C2-33 (major) No switch to stop dragging

**Problem.** material3 has `sheetGesturesEnabled` (`sources/ModalBottomSheet.kt:101` [V]). §4.12 drops per-path
switches, and `dismissible = false` still lets the person drag between detents. Apps need a sheet that holds its
detent while it shows a signature pad, a pannable chart or map, a drawing canvas, or a payment in progress. B-16 makes
the whole surface a drag area.

**Fix.**

- Add `AppSheetProperties.dragEnabled: Boolean = true`. When it is false:
  - there is no `anchoredDraggable` and no nested-scroll resizing;
  - Back, the scrim and the close button still follow `dismissible`;
  - the grabber's accessibility actions stay.
- Add `Modifier.sheetDragExclusion()` for a region inside the content.
- Add a catalog toggle and a test.

### C2-34 (major) No window-space visible frame for UI that rides the sheet

**Problem.** The `maps` demo rides a FAB "through `state.offset`" (`SPEC:2230`). But `offset` is in the sheet's bounds
(its pane), and is measured "before the park and keyboard translations" (`SPEC:464`) and before the stretch. A FAB in
the screen's coordinates therefore misses the rubber band, the lift, parking and the floating gap. A map needs bottom
padding equal to the visible sheet height, and nothing public gives that; `SheetFrame` is a WP05 seam.

**Fix.**

- Add `@FrequentlyChangingValue val visibleTop: Float` and `val visibleHeight: Float` to `AppSheetState`, in window px
  after stretch, lift and park. Alternatively, add `Modifier.sheetAvoidance(state)`, which keeps a node above the sheet
  in placement.
- Rewrite the `maps` demo with it, and add a RECIPES entry.
- Budget: 0 recompositions.

### C2-35 (major) No header height for peeks; large text in short windows

**Problem.**

- A Maps peek must show the grabber band and the header. `SheetDetentContext` exposes `contentHeight` (the whole
  column) but no header height, so the spec itself uses `Height(96.dp)` and `Height(120.dp)` (`SPEC:215`, `:2230`). At
  font scale 1.5 and 2.0 the header grows (§7.4), and the fixed peek clips it, which REQUIREMENTS 34 forbids. WP08
  already warns about exactly this (`AppSheetDetent.swift:35-36` [C]).
- At font scale 2.0 in Compact height (a 300-400 dp split window), the header and an `AppBottomActionBar` footer can
  take most of the visible height. B-45 then places the footer at the header's bottom (`SPEC:1408-1410`), and no
  content row is visible.

**Fix.**

- Add `val headerHeight: Dp` to `SheetDetentContext` (grabber band plus header, as measured). Document
  `Custom("peek") { headerHeight }`, or add a ready-made detent for it (see C2-25 on adding to the sealed type).
- Switch the §4.1 example and the catalog to it.
- When header plus footer exceed half the visible height, the footer stops being sticky and scrolls as the content's
  last item.
- Screenshots: a peek at font scale 2.0, and 900 × 400 at 2.0 with a header and a footer.

### C2-36 (major) `AppChoiceSheet`'s default key can crash

**Problem.** `key: (T) -> Any = { it }` (`SPEC:972`) becomes a lazy-list key. Lazy items run under a
`SaveableStateHolder` that does `require(canBeSaved(key))`: "Type of the key … is not supported. On Android you can
only use types which can be stored inside the Bundle" (runtime-saveable 1.12.1 `SaveableStateHolder.kt:74-77` [V]). A
plain data class `T` crashes.

**Fix.**

- Make it `key: ((T) -> Any)? = null`, with position keys by default.
- The KDoc says a key must be saveable in a Bundle.
- Test with a non-Parcelable `T`, including a restore.

### C2-37 (major) Steps inside a sheet

**Problem.** During an `AppSheetNavHost` push or pop, both steps are composed, and both register
`ProvideSheetHeader`, `ProvideSheetFooter`, `SheetDismissGuard` and `SheetBackHandler`. They are siblings, so "the
innermost enabled one wins" (`SPEC:911-913`) cannot decide between them. Also undefined:

- the header snaps at registration while the content slides, where iOS slides the title with the push;
- `FitContent` may take the outgoing step's height, the incoming one's, or the larger;
- an outgoing step's guard may block Back on the incoming step.

The inner Back is key-style in v1 (B-59), so a back swipe inside a multi-step sheet shows no preview. That defers
REQUIREMENTS 24 inside sheets and needs the user's sign-off.

**Fix.**

- The registration of the uncovered entry wins (`LocalEntryVisibility`, C2-04).
- The header and footer layers cross-fade with the step transition (`ModulateAlpha`, progress read in draw).
- `FitContent` resolves to the target step's height when the transition starts.
- Covered steps' guards and Back handlers are ignored, and the pane title follows the uncovered step.
- Add `AppSheetNavHostTest` cases.
- List the predictive-back deferral in the sheet checkpoint's decisions for the user.

### C2-38 (major) The DevTools panel is missing from the migration

**Problem.** REQUIREMENTS 33 makes this "the one sheet system of every generated app", and D-22 forbids a `Dialog`
anywhere in it. `DevToolsPanel` is a `Dialog`: full screen on a phone, a side panel from Expanded
(`wt/WP06/templates/android/core/devtools/.../panel/DevToolsPanel.kt:62-83` [C]). REQUIREMENTS 8 names that panel
too. §13 and §19 give it neither a row nor an owner.

**Fix.**

- Add a §13 row: the DevTools panel becomes an `AppSheet` with `[Medium, Large]`, and the modal End placement from
  Expanded (C2-18).
- The owner is WP06, after WP03 merges. List it in §19.8.

---

## E. Parity

### C2-39 (major) §18 is stale against WP08's head; no parity matrix; no SwiftUI presets

**Problem.** §18 was written against WP08 `a95111a`. WP08 is now at `4a4bd4d` ("Give appSheet the options of the
app's one sheet system", plus two more commits), with a different `AppSheetOptions`:

- `detents: Set<AppSheetDetent>`, where `AppSheetDetent` is a struct;
- `style: AppSheetStyle` instead of `material`;
- SwiftUI's own `PresentationContentInteraction`;
- no `placement`;
- `nonModal(upThrough:)` with a required detent;
- `appSheetPresentation(_:selection:)` as an extra entry point.

(`AppSheetOptions.swift:5-41`, `AppSheetDetent.swift:5-58`, `AppSheetModality.swift:4-20`, `AppSheet.swift:16-50`,
`AppSheetStyle.swift:2-14` [C].) §19.5's acceptance, "every mapping of §18", conflicts with shipped code. Behaviour
also diverges:

- **Initial detent:** Android opens at `detents.first()`; iOS opens at the smallest detent unless `selection` says
  otherwise.
- **Automatic material:** iOS's `.automatic` is the system's Liquid Glass on iOS 26 (`AppSheetStyle.swift:3-5`), while
  Android's is Solid.
- **Android-only options:** `scrimTapDismisses`, `SheetKeyboard`, `SheetTransition`, `aboveBottomChrome`, the header
  and footer slots, `SheetDismissSource`, `onDetentChange` (person-only, while SwiftUI's `selection` sees every move)
  and the haptics have no stated iOS outcome.
- **Non-modal sheets:** on iOS a non-modal sheet covers the tab bar, stays up over a push beneath it and over a tab
  switch, and blocks any other `.sheet` from the screen beneath. Android parks it (B-14) and stacks (C2-06).
- **Presets:** `AppActionSheet` and `AppChoiceSheet` have no SwiftUI form. On iOS the natural mappings are
  `confirmationDialog` and a `Picker` in a sheet.

**Fix.**

- Rebase §18 on WP08's head. Keep `appSheetPresentation`. Keep `Set`, with an explicit `initialDetent` option. Adopt
  WP08's names where they are better.
- Add a parity matrix (Android | CMP iOS | SwiftUI) for every public option and preset, each marked "same",
  "platform default" or "n/a, because …".
- Add a SwiftUI `AppSheetHeader` (title, leading, trailing: Cancel, Done, close), `appSheetFooter`, and preset
  mappings, so every §4.1 example has a one-to-one Swift form.

### C2-40 (major) The dismiss guard's shape on iOS

**Problem.**

- iOS 27's `dismissalConfirmationDialog` shows the system's own dialog, with default cancel and close actions
  (`research.md:143` [A]). The callback-shaped `appSheetDismissGuard(enabled:onAttempt:)` (`SPEC:2455-2456`) cannot
  map onto it.
- Before iOS 27, the bridge sets the presentation controller's delegate (`SPEC:2466`). SwiftUI already uses that
  delegate to reset the binding, so replacing it may break `isPresented` [U].
- Every Android app hand-builds the same "Discard / Keep editing" action sheet.

**Fix.** Add a convenience on both platforms, and keep the raw guard for custom UI:

- **Android:** `SheetDismissConfirmation(enabled, title, discardLabel, onDiscard)`, built from `SheetDismissGuard` and
  a stacked `AppActionSheet`;
- **SwiftUI:** `appSheetDismissConfirmation(isEnabled:title:discardLabel:onDiscard:)`. On iOS 27 it uses
  `dismissalConfirmationDialog`. Earlier, it uses `interactiveDismissDisabled` plus a delegate proxy that forwards
  every call to SwiftUI's original delegate [U, WP08 handoff].

### C2-60 (minor) CMP's iOS target

**Problem.**

- How `paneTitle`, `hideFromAccessibility` and the actions reach VoiceOver is [U], and §21 does not list it.
- CMP may route an iOS edge swipe into navigation-event Back [U]. A sheet would then shrink on an edge swipe, which no
  iOS sheet does.
- The default `stringResource(R.string…)` (`SPEC:932`) is Android-only.

**Fix.**

- Add §21 rows for WP07-B: the VoiceOver mapping, read in CMP's iOS accessibility sources, and the back-gesture
  configuration.
- On iOS the sheet registers no Back handler.
- CMP defaults use `Res.string`.

---

## F. Testability and acceptance

### C2-41 (major) Previews and screenshot tests render no sheet

**Problem.** Frame 1 draws the hidden pose (B-1), and motion starts from `LaunchedEffect(visible)` (B-4). A layoutlib
preview and a `com.android.compose.screenshot` test do not run that animation, so they show an empty screen. API-13
requires the initial state in preview, and REQUIREMENTS 23 requires multi-size previews. §15.5 also asks for "a fake
presenter at e = 0.5 and 1" without an API to pose one.

**Fix.**

- Under `LocalInspectionMode`, a visible sheet is placed at its initial detent with no animation; that is API-13's
  allowed branch.
- Add a test-only pose: `rememberAppSheetState(…, initiallyShown = true)`, or a `@VisibleForTesting` snap to a
  fraction.
- Ship a multi-size `@Preview` set for the catalog's sheet demos.
- Acceptance: every §15.5 image differs from its no-sheet baseline.

### C2-42 (major) `SheetApiShapeTest` cannot compile the destination block, and the blocks break the line limit

**Problem.**

- §4.1 includes `entry<ShareKey>(presentation = Presentation.Sheet(…))` (`SPEC:224`), which is `:core:navigation` API.
  `SheetApiShapeTest` lives in `core/designsystem/src/test` (`SPEC:2265`, `:2269`), and the design system cannot depend
  on `:core:navigation`.
- The blocks call app symbols with no stubs defined: `FilterList`, `PlaceRow`, `RenameField`,
  `R.string.orders_filters`, `ShareRoute`, `SearchHeader`, `NowPlaying`.
- Lines 205, 215, 224, 229, 233 and 237 are 101 to 127 columns, past K-1's 100 and even the template's current 120
  (`.editorconfig:17` [C]). ktlint will reflow them, so "compile as written" and "a change that makes one longer is
  wrong" (`SPEC:177`) conflict.

**Fix.**

- Split the test:
  - `SheetApiShapeTest` in the design system, with stub composables in the test source set;
  - `SheetSpecShapeTest` in `:core:navigation`, for the destination block.
- List the stubs.
- Rewrap the §4.1 blocks at 100 columns, and measure "longer" in arguments, not characters.

### C2-43 (major) `SheetImeTest` cannot animate the IME in Robolectric

**Problem.** `SheetImeTest (insets injected)` asserts frame-by-frame lift, "at most twice per transition" re-measures,
and B-48's fallback (`SPEC:2276`). The only inset injection in ui-test 1.12.1 is
`DeviceConfigurationOverride.WindowInsets`. It dispatches a static `WindowInsetsCompat`
(`DeviceConfigurationOverride.android.kt:406-424` [V]), and ui-test has no `WindowInsetsAnimation` dispatch at all
[V]. `WindowInsetsRulers.Ime.getAnimation(scope)`'s `isAnimating` and `target`, which B-44, B-45 and B-47 depend on,
can therefore never be driven in a JVM test.

**Fix.**

- Read the IME through an internal `SheetImeSource` (current, target, animating), backed by the rulers in production
  and by a test double in `SheetImeTest`.
- Move one end-to-end IME case to an instrumented emulator test (`connectedCheck`, run by WP04's perf job), and keep
  §16.1's `sheet_ime` row as the phone gate.
- State the same seam for B-44's Back-with-keyboard test.

### C2-61 (minor) "Matches exactly" needs a tool; rules need owners

**Problem.**

- "The public API matches §4 exactly" and "diffed signature by signature" (`SPEC:2512-2513`, `:2578`) have no tool, so
  reviewers would compare by eye.
- "Every B-rule and R-rule WP03 implements is cited by at least one test" (`SPEC:2514`) cannot be checked, because no
  table says which rules WP03 implements and which WP04 does. B-43, for one, is split.

**Fix.**

- Add the Kotlin binary-compatibility-validator to `:core:designsystem` and `:core:navigation`, with klib dumps for
  the multiplatform module, and commit the `api/*.api` files. Acceptance becomes "the `.api` diff equals §4". For
  WP07-B, diff the common dump against Android's, limited to the sheet package.
- Add a §15.6 table: rule, owner package, test name.

### C2-62 (minor) Feel checks need instruments; two gate rows are under-defined

**Problem.**

- "The sheet's top under the finger within one frame" (`SPEC:2371`) needs the finger in the strip.
- "The logcat marker within ±1 frame of the arrival" (`SPEC:2378`) needs a haptic marker in the app. §19 assigns
  neither.
- `sheet_idle.frames = 0` (`SPEC:2348`) fails for a reason that is not the sheet if its content blinks a caret or runs
  a shimmer. The scenario's content is not specified.
- `sheet_cardstack.no_offscreen` asks for "no stencil clip" in a trace (`SPEC:2344`). HWUI does not trace Skia's clip
  method unless Skia atrace is enabled (`debug.hwui.skia_atrace_enabled`) [U].

**Fix.**

- QA2 records drags with `adb shell settings put system pointer_location 1`.
- WP03 emits a trace section for the arrival haptic in perf builds.
- The idle scenario uses a static content (no field, no shimmer).
- `no_offscreen` names its trace setup and the slice names it greps, or drops the stencil half.

---

## G. Minor API and completeness items

### C2-44 (minor) The seams are public with nothing to keep features out

**Problem.** These are public in `:core:designsystem` and "not for features" (`SPEC:254`, `:998-1075`):

- `SheetStack`, `cardStackPresenter`;
- `HostMotion`, `EntryVisibility`, `PaneBounds`, `LocalPresentationLevel`;
- `SheetBackdrop`, `SheetFrame`, `LocalSheetBackdrop`.

Every feature's completion list, and every agent, sees them.

**Fix.**

- Annotate them `@RequiresOptIn(level = ERROR) annotation class ShellSeamApi`.
- `:core:navigation` and the glass module opt in, and a code rule forbids the opt-in under `feature/**`.
- The renamed in-place overload (C2-27) takes the same annotation.

### C2-45 (minor) Public surface wider than its uses; dead tokens on iOS

**Problem.**

- These `AppSheetDefaults` values are the default of no parameter (`SPEC:710-723`): `GrabberTop`, `ContentTop`,
  `HeaderTop`, `HeaderMinHeight`, `FormHeightFraction`, `FormMaxHeight`, `SideInset`, `GrabberAlpha`, `MediumFraction`,
  `LargeTopGap`, `GrabberSize`. Once public, they are API that `upgrade_project.py` must carry (API-14).
- The Swift token writer emits the whole `sheet` section (`SPEC:1562-1563`, `:2586`). The SwiftUI template uses the
  system sheet (§18) and reads only `springs.sheet` and the radius, so the rest is dead code in every iOS project
  (REQUIREMENTS 7).

**Fix.**

- Keep public only the parameter defaults: `Detents`, `Grabber`, `colors()`, `groupedColors()`, `style()`, `shadow()`
  and `solidRim()`. The rest live in the generated `DesignTokens.Sheet`.
- The Swift writer emits only the tokens the SwiftUI template reads.

### C2-46 (minor) Public data classes; `AppSheetColors` has no `copy`

**Problem.** `AppSheetProperties`, `SheetSpec` and `SheetModality.NonModal` are data classes (`SPEC:599`, `:610`,
`:1093`). Adding a property, as API-14 prescribes, breaks `copy` and `componentN` in callers [U: the exact androidx
`kotlin.md` rule]. `AppSheetColors` (`SPEC:673-681`) has no `copy`, unlike API-6's `XColors` pattern.

**Fix.**

- Make `AppSheetProperties` and `SheetSpec` plain classes with `equals`, `hashCode`, `toString` and `copy`, as
  `AppSheetStyle` already is.
- Add `AppSheetColors.copy(...)`.

### C2-47 (minor) Which `Shadow`?

**Problem.** `shadow: Shadow?` (`SPEC:697`, `:745`) could be either of two classes in ui-graphics 1.12.1:

- the text shadow `androidx.compose.ui.graphics.Shadow`;
- `androidx.compose.ui.graphics.shadow.Shadow`, which has spread, brush, blend mode and alpha.

Both files exist [V]. The nine-slice key (`SPEC:1808-1809`) ignores spread, brush and blend mode.

**Fix.**

- Name the import.
- The nine-slice supports radius, spread, offset, colour and alpha. A brush or a non-`SrcOver` blend mode fails a
  `require`.
- Cover both in `SheetShadowTest`.

### C2-48 (minor) A modal sheet over a faint scrim has no edge

**Problem.** A modal Bottom sheet never draws a shadow because "the dim separates it" (`SPEC:1806-1807`). But the
scrim colour is public. A caller who picks a transparent or faint scrim (a sheet over a camera or a map) gets, in
light theme, a white sheet with no rim (Solid's light rim is transparent, `SPEC:739-741`) and no shadow: no visible
edge. An explicit `style.shadow` is silently ignored.

**Fix.**

- On a modal Bottom sheet, draw the nine-slice when `scrim.alpha < 0.2`. The nine-slice does not depend on size, so
  §7.7 still holds.
- Document when `style.shadow` applies.

### C2-51 (minor) Content-side API asymmetries

**Problem.**

- `LocalSheetSurface` is a public `ProvidableCompositionLocal` (`SPEC:893`), so any feature can provide a fake sheet.
- `ProvideSheetHeader(content: @Composable () -> Unit)` (`SPEC:864-866`) lacks the `AppSheetScope` receiver that the
  `header` slot has, so a destination's header cannot read `contentScrolled` the same way.
- `AppSheetHeader`'s slot overload (`SPEC:834-840`) has no `subtitle`, so the String overload is not a pure
  convenience over it (API-5).

**Fix.**

- Expose `val LocalSheetSurface: CompositionLocal<SheetSurface?>`, with an internal provider.
- Give `ProvideSheetHeader` and `ProvideSheetFooter` the `AppSheetScope` receiver.
- Add `subtitle: (@Composable () -> Unit)?` to the slot overload, and make the String overload delegate to it.

### C2-52 (minor) A sheet resting on the tab bar, against the bar's minimise and its accessory slot

**Problem.**

- B-52 rests a non-modal sheet on "the settled `LocalBottomChrome`" (`SPEC:1449-1452`). WP04's tab bar minimises on
  scroll (REQUIREMENTS 21; `nav4/shell/ShellBar.kt`, `AppTabBarState.kt` [C]). The resting sheet then jumps once the
  bar settles, instead of riding it.
- `LocalBottomChrome` already includes the bar's accessory slot (`ds4/foundation/layout/Chrome.kt:28-31` [C]),
  iOS 26's place for a mini player. The `nowplaying` demo (`SPEC:2231`) builds the same thing as a non-modal sheet.
  Agents get two patterns with no rule between them.

**Fix.**

- A sheet resting on the bar reads the bar's live top in placement (L1, no recomposition), not the settled value.
- WP12's docs state the rule: a mini player is the bar's accessory; a Maps-style sheet is a non-modal `AppSheet`.
  Change the demo to match.

### C2-53 (minor) Automatic → Scrolls for non-modal sheets may not match Maps

**Problem.** `Automatic` resolves to Scrolls for every non-modal sheet (`SPEC:624`, `:1295`). §4.1 presents the
persistent sheet as "Maps-style". Whether iOS's `.automatic` content interaction scrolls first under
`presentationBackgroundInteraction` is [U]. Apple Maps appears to expand its sheet when the list is scrolled at
medium [U]. WP08 sets `.scrolls` explicitly in its own non-modal preview (`AppSheet.swift:203` [C]).

**Fix.**

- Add a §21 row: QA2 records Apple Maps (or a public capture) scrolling the list at medium.
- Until then, state the reason for Scrolls in B-27, so it can be flipped without an API change.

### C2-54 (minor) The Glass acceptance example never shows glass at rest

**Problem.** §4.1's Glass example has the default detents `[Large]` (`SPEC:228-231`). Glass samples "only at partial
detents … opaque by `e = 0.5`" (D-17), and for a Large-only sheet `e` rises with the present (`SPEC:413-416`). The
sheet is therefore glass for half its entrance and opaque at rest, so the example the review rule protects
demonstrates nothing.

**Fix.** Give the example `rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large))`, or a `Height`
detent. State in `SheetMaterial.Glass`'s KDoc that a Large-only sheet is opaque at rest.

### C2-55 (minor) A restored detent against the caller's `visible`; a cold deep link to a sheet

**Problem.**

- An in-screen sheet's state restores `shown = 1` (B-62, `SPEC:1507`), while the caller's `visible` may come back
  false. B-4 then calls `hide()`, and the sheet slides out at launch. The opposite mismatch is possible too.
- A cold start through a deep link to a sheet destination is neither a restore (snap, B-6) nor a tap (slide). The spec
  does not say which it is.

**Fix.**

- For the `visible` and `item` overloads, whether the sheet shows comes only from the caller. The restored detent is
  used. If `visible` is true on the first composition, the sheet snaps to that detent; if false, it is absent.
- A cold deep link slides the sheet in after the first frame of the presenter (CP1's first-frame rule), and a test
  says so.

### C2-56 (minor) Action-sheet rows read as a fixed height

**Problem.** "Rows of 56 dp" (`SPEC:983`). At font scale 2.0, a wrapped label would be clipped.

**Fix.** State that 56 dp is a minimum (`heightIn(min = 56.dp)`). Add font-scale 2.0 screenshots of the sheet and the
anchored card.

### C2-57 (minor) `AppChoiceSheet` details

**Problem.**

- `label: (T) -> String` is all a row gets. The shipped consumer, the language picker (`SPEC:2206`), needs native and
  localised names. Units or plans need a description or a disabled row.
- Search matching (case, diacritics, locale) is unspecified.
- `onSelect: (T?) -> Unit` is nullable even when `noneLabel` is null, so callers handle a null that never comes.
- "Choosing calls onSelect, then closes the sheet" (`SPEC:959`) does not say whether it calls `onDismissRequest`.

**Fix.**

- Add `supportingText: ((T) -> String?)? = null` and `enabled: (T) -> Boolean = { true }`.
- Search matches the label and the supporting text, ignoring case and diacritics (NFD, marks stripped).
- Split the API: `onSelect: (T) -> Unit` plus `onSelectNone: (() -> Unit)? = null`, where a non-null value adds the
  None row with `noneLabel`.
- Choosing calls `onSelect` and then `onDismissRequest`, as `AppActionSheet` does.

### C2-58 (minor) Destinations: `pane` + `presentation`; grabber and colours

**Problem.**

- `entry(pane = …, presentation = …)` accepts both (`nav4/NavGraph.kt:50-55` [C]). The combination is undefined.
- "Detail as a sheet on a phone, a pane on a tablet" (SwiftUI's `.inspector`) has no Android path, though WP08 ships
  `appSheetPresentation` for it (`AppSheet.swift:42-50` [C]).
- "No styling travels through metadata" (`SPEC:1124-1125`), so a destination sheet cannot use `groupedColors()`, hide
  its grabber or supply a custom one.

**Fix.**

- Add `require(presentation == null || pane == NavPane.Single)` at registration now, and put the inspector-as-sheet
  pattern in §20.
- Add `ProvideSheetStyle(style, grabber)`, a content-side provider like `ProvideSheetHeader` that registers before the
  first frame (B-2).

### C2-59 (minor) No completion hook; no result recipe

**Problem.**

- With the `visible` overload, a caller cannot learn when the exit has finished. Sequential sheets need that, and
  SwiftUI has `onDismiss` (`AppSheet.swift:15` [C]).
- D-32 drops a results store, yet the commonest destination sheet is a picker that returns a value, and §19.7's recipes
  leave it out.

**Fix.**

- Add `onHidden: (() -> Unit)? = null`, last before the slots (API-14), or document a `snapshotFlow` recipe.
- Add a RECIPES entry for a destination sheet that returns a result.

### C2-63 (minor) Navigation-bar icon contrast over a sheet

**Problem.** §7.9 flips only the status bar. Under 3-button navigation, a sheet attached at the bottom is drawn behind
the buttons. Their icon appearance follows the window's `isAppearanceLightNavigationBars`, not the sheet's container
(a dark media sheet in light theme leaves dark icons on a dark sheet). Gesture navigation samples its handle colour
[U].

**Fix.** While a sheet covers the bottom edge (attached, or Large) under 3-button navigation, WP04's edge-to-edge owner
sets the navigation-bar appearance from the sheet container's luminance, at most one change per crossing. Add a
screenshot with 3-button insets and a dark container in light theme.

### C2-64 (minor) Internal inconsistencies

**Problem.**

- §21's persistent-sheet row sends a different result to "B-38 branch 3" (`SPEC:2674`). The step-down is branch 2;
  branch 3 is the guarded shrink (`SPEC:1362-1366`).
- `AppSheet`'s KDoc says "the content is measured once per window size and only re-placed while the sheet moves"
  (`SPEC:281-282`). B-31 and B-47 re-measure on settles and on IME transitions.
- §8 lists "Compact height, any width" and "Medium width, height Medium and up" as rows without saying which wins for
  a landscape phone 800 × 360 dp (Medium width, Compact height).
- The spec cites WP03 at `8ea3985`. WP03 is now at `87a3a06`, a catalog-only Back change, so the citations still hold.

**Fix.**

- Point §21 at branch 2.
- Rewrite the KDoc as "never re-measured during a drag".
- State that §8 rows apply top to bottom and the first match wins, so Compact height takes precedence.
- Refresh the heads in §23.

---

## Checked and found sound

- Parameter order (API-2), `on`-verb callbacks (API-11), slot lambdas with a scope receiver (API-5), one `XDefaults`
  per component (API-6), and no `State<T>` parameters (API-8) hold for `AppSheet`, `AppActionSheet` and
  `AppChoiceSheet`.
- D-27 (a concrete state class) is a marked Deviation with a reason, as CODE-STANDARDS asks.
- `hideFromAccessibility()`, `paneTitle`, `isTraversalGroup` and `traversalIndex` exist in ui 1.12.1. Compose's
  pruning of covered nodes (`SemanticsOwner.kt:318` [V]) also hides what is beneath a modal scrim that carries
  semantics, which backs up §9's `hideFromAccessibility`.
- `DirectNavigationEventInput` exists for `SheetBackTest` (navigationevent 1.1.2 sources [V]).
- Window size, posture, input traits and reduce motion are all injectable in tests: through `LocalWindowInfo`, which
  `DeviceConfigurationOverride.WindowSize` sets, and through `AppTheme(posture, input, reduceMotion)`
  (`ds/foundation/adaptive/WindowClass.kt:29-42`, `ds/theme/AppTheme.kt:88-90` [C]).
- The size classes and breakpoints of §8 match `ds/foundation/adaptive/SizeClass.kt:35-50` [C].
- The presentation host always has a `PresentationBase` beneath (`nav4/presentation/PresentationHost.kt` [C]), so a
  deep link straight to a sheet key still finds a scene beneath it.
- The CMP template is on Compose Multiplatform 1.12.1, with runtime-annotation artefacts for iOS in the cache, so
  `@FrequentlyChangingValue` and `@RememberInComposition` are available to common code.
