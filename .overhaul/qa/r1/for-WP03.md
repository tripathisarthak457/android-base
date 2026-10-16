# Visual QA round 1: WP03 (design system, catalog, overlays, pull to refresh)

Full table: `DEFECTS.md`. Code references are to `wp/WP03` at `69837f4`, under
`templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` unless
stated.

Evidence comes from two places: the r1 shots (old design system in the WP04 build) and your own
catalog frames in `perf/WP03/qa`. An item marked *fixed on wp/WP03* still needs one shot after the
sweep to close it.

## Must fix (S1/S2)

### QA-12 (S1). Toasts draw behind the tab bar

You see the green "Saved" toast drawn behind the floating capsule, with only its ears showing
(light 18a, 18b). In dark mode it is not visible at all.

The cause is that `core/ui/.../MviScreen.kt` hosts `MessageHost` inside each screen's Box, below
the shell chrome. The old `AppSnackbarHost` pads only `navigationBarsPadding()`.

The fix is your planned step 1 ("Remaining work"). `MessageHost` must call
`LocalOverlayHost.current?.toasts?.show(ToastMessage(…))` with lifecycle-aware collection.
Delete the in-screen host and `MessagingSurface`. Your `AppToast` already rests above
`max(LocalBottomChrome, navigation bar) + 12 dp` (`8eae2c0`).

To prove it, add a test: with the shell's bar visible, a `showMessage` toast's bounds end above
the bar's top. Then take the profile shot again.

### QA-20 and QA-21 (S1/S2). Pull to refresh (REQ 29)

The old version is replaced by your `component/refresh/AppPullToRefresh.kt`: content follows the
finger, spokes are drawn, the threshold haptic plays, there is no disc and no shadow, and the
offset is read in draw. Close the remaining gaps before you record before and after on the
Redmi:

- **(a) Carry velocity.** `settleTo` (`:74-77`) and the resting animation (`:105`) call
  `animate(offset, target, animationSpec = spring)` with no velocity. Keep `available.y` from
  `onPreFling` and pass `initialVelocity`, scaled by `RESISTANCE`, so the snap-back keeps the
  finger's momentum.
- **(b) Release upward flings.** `onPreFling` returns `Velocity(0f, available.y)` whenever
  `offset > 0` (`:116`), so an upward flick to cancel never reaches the list. For
  `available.y < 0`, call `settleTo(0f)` and return `Velocity.Zero`.
- **(c) Keep one drawing.** At release the drawn spokes are replaced by a separate
  `AppActivityIndicator` (`:144-148`), which pops visibly. Give `AppActivityIndicator` a
  `revealed: () -> Float` input, so the same eight spokes appear while pulling and then start
  stepping. Keep the stepping at 8 per turn, as now.
- **(d) Clear the top chrome (V, from code).** Under your `AppScaffold` the content starts at
  y = 0 under the top bar. The indicator (`:136-139`, `Alignment.TopCenter`) would therefore draw
  under the large-title bar. Add `LocalTopChrome.current.value.toPx()` to its `translationY`
  inside `graphicsLayer`, which reads it in draw.
- **(e) Trigger timing (unverified).** `UIRefreshControl` appears to start refreshing when the
  pull crosses the threshold during the drag, not on release. Check this against an iPhone
  recording or Apple sample. If it holds, trigger in `onPostScroll` on the crossing, where the
  haptic already plays.
- **Failure feedback.** When `onRefresh` fails, keep the spinner at least 400 ms, then settle and
  let the screen show a toast (see WP02-M3 QA-22).
- **Recording.** Record the same script as `qa/ptr/record-ptr.sh` on the merged build, in light
  and dark. Confirm zero recompositions per frame with the recomposition-count harness.

### QA-23 (S2). Dark segmented control: the thumb is darker than its track

`AppSegmentedControl.kt:84` and `:101` draw the thumb in `surfaceElevated` (dark `Ink700
#1D232E`) on `fill` (dark `Ink600 #262D3A`). The old design system shows the same inversion at
1.16:1 (dark 20, 21). iOS draws a lighter thumb on a darker translucent track in dark mode.

The fix is a dedicated role, `segmentThumb`: light `White`, dark `Ink500 #38414F` or lighter.
The label `contentPrimary` on it stays at about 10:1. Extend `PaletteContrastTest` to require
thumb/track ≥ 1.4:1 in both themes and every style.

### QA-25 (S2). Multi-line field: the placeholder is centred and the text starts at the top

You see this in the old design system (profile "About you") and in your catalog Inputs ("Note").
`component/input/AppTextField.kt:176-186` sets `verticalAlignment = Alignment.CenterVertically`
and the Box to `contentAlignment = Alignment.CenterStart`, with placeholder `maxLines = 1`.

The fix: pass `multiline = lineLimits !is SingleLine` into `fieldDecorator`. When it is true, use
`Alignment.Top` and `TopStart`, and let the placeholder wrap. Icons and prefix stay top-aligned
with the first line.

### QA-17 (S2). Sheet content gets no inset

`interact/sheet_large.png` shows the body text at x = 0. The catalog sample
(`catalog/.../CatalogOverlays.kt:92`) passes a bare `AppText`, and `AppSheet` adds no gutter.
`AppSection` pads itself, so a default gutter would double it. Fix the sample instead: pad it
with `AppTheme.spacing.gutter`. State in `AppSheet`'s KDoc that content is edge to edge, or add a
`contentPadding` parameter that defaults to zero.

### QA-18 (S2). The context menu has no preview (REQ 21)

Long-press → `AppMenu` (`interact/menu.png`) is a plain popup: no lifted preview, no backdrop, no
separators.

Add an `AppContextMenu(preview = { … })` that:

- lifts the anchor into the overlay layer at scale 1.03 with a soft shadow on `springs.emphasis`;
- dims the window with `scrim` at about 0.2. Use no blur: blur is GPU-bound on the Adreno 618 and
  would need the reduce-transparency path;
- places the menu below the preview, or above it when there is no room;
- draws hairline separators between items and 8 dp gaps between groups.

It must be interruptible. Dismiss runs the lift in reverse.

### QA-14 (S2). Single-choice rows need a checkmark

The language picker has nothing that marks the current value (light 22b). Add
`AppRow(…, selected: Boolean)` with a trailing accent checkmark for single choice, as iOS
Settings does, and use it inside `AppSheet` or `AppSection`. WP02-M3 then rebuilds the picker.

### QA-06 (S3). Filled glyphs for tabs

WP04 will pass `selectedIcon`. Feed's `ListView` has no filled form, and none of these do:
`Cart`, `CreditCard`, `Package`, `Calendar`, `Mail`, `MapPin`, `Globe`, `Users`, `Tag`. The
generator maps scaffolded tabs to those glyphs (for-WP02-M2). Add filled forms, or publish the
list of glyphs that have one so the generator only picks from it.

### QA-35 (S1, shared). A search accessory for the large title

Root screens put the large title and the search field in a static header, and the list is cut by
a hard edge (light 02, 08b, 13, 21). `AppLargeTitleTopBar` has no slot for a search field.

Add `accessory: (@Composable () -> Unit)? = null`, laid out under the large title. It is part of
the expanded height written to `LocalTopChrome`, and it translates away with `scrolled()`, in
`graphicsLayer`. Pair it with `scrollEdgeFade`.

**Perf note for `foundation/ScrollEdge.kt`.** Your fade records the content and masks a strip
with `BlendMode.DstIn` in an offscreen layer every frame. On a solid background, a gradient rect
in `colors.background` drawn over the strip gives the same pixels with no offscreen pass. Keep
`DstIn` only where the background is not solid (glass). WP04 uses the gradient form for the
bottom edge (QA-02); a shared `Modifier.edgeProtection(top, bottom)` here would serve both.

## Fixed on wp/WP03: verify after the sweep

| ID | Old design system defect | Why it should be gone |
|---|---|---|
| QA-15 | The sign-out alert icon is clipped at the top-left of its circle (light 23b, dark 23) | `AppAlert.kt:109` draws a centred 28 dp icon. Map `AppAlertDialog(icon=…)` to `AppAlert(icon=…)` in the sweep. |
| QA-16 | Dark destructive button: white on `#F0666D`, 3.08:1 | `danger.on = readableOn(…, Ink900)`; alert actions are text. |
| QA-24 | Dark switch thumb is navy on blue | `AppSwitch.kt:166` always uses `Color.White`. |
| QA-13 | The sheet scrim leaves a lighter status-bar band (light 22b) | `AppSheet` renders in the overlay host and consumes `statusBars` (`AppSheet.kt:343`). Take a dark and a light shot of the language sheet. |
| QA-20 | The old pull to refresh: disc, black ring, composition reads | Replaced, apart from the gaps above. |

## Polish (S3)

- **QA-26: removable chip.** The × sits 3 dp after the label with ~20 dp empty after it
  (catalog Selection "Removable×", Fields tags). The likely cause, unverified, is that
  `pressable`'s touch-target sizing widens the remove `Box` beyond `RemoveSpace = 30.dp`
  (`selection/AppChip.kt:50-92`). Put the visual × inside the Row (label, 6 dp gap, 14 dp ×,
  10 dp end padding). Put the hit area in an overlay at `CenterEnd` whose touch expansion does
  not feed measurement.
- **QA-27: disabled buttons.** `disabledAlpha` (`foundation/Modifiers.kt:23`, 0.38 over the whole
  button) gives white on pale accent, 1.85:1 (light 16, 35 to 38). Draw disabled colours
  instead: filled → `fill` container with `contentDisabled` label; tinted and plain →
  `contentDisabled`. This drops the alpha layer too. WCAG 1.4.3 exempts inactive controls, so
  this is polish.
- **QA-28: avatar.** `AppAvatar.kt:102-107` gives "Author 10" → "A1" and "You" → "YO". Use only
  words that start with a letter, and one letter for a single word. `:49` uses fixed `footnote`
  text: size the initials at about 0.4 × diameter (88 dp profile avatar). In dark mode the subtle
  tints (`:112-114`) barely separate from `surface`: use mid-tone tints.
- **QA-29: link buttons.** "Show more", "Create account" / "Forgot password" and "Clear" sit
  inset from the text column (light 07, 35, 14). Check that `AppLinkButton` has zero horizontal
  content padding and that its 48 dp target extends outside its layout bounds without shifting
  the text.
- **QA-30: section header.** "See all" sits about 16 dp above the "Recent" baseline (catalog
  Lists). `list/AppSection.kt:79-91` is a Row aligned `Bottom` with a 48 dp-target link. Use
  `alignByBaseline()` on both. Give `AppSectionHeader` the row-text inset by default (the
  lazy-list demo's header sits flush left).
- **QA-31: helper text.** `input/AppTextField.kt:194` pads it `horizontal = 2.dp`, so it starts
  2 dp right of the label. Remove the padding.
- **QA-19: embedded top bars.** `AppTopBar` and `AppLargeTitleTopBar` always apply
  `windowInsetsPadding(WindowInsets.statusBars)` (`navigation/AppTopBar.kt:81`, `:126`). An
  embedded bar shows a ~24 dp empty band (catalog "Messages", "Checkout"). Add
  `windowInsets: WindowInsets = WindowInsets.statusBars`, as androidx top bars do, and pass
  `WindowInsets(0)` in demos and panes.
- **QA-34: slider thumb.** A white thumb on a near-white page has only a faint shadow. Use a
  two-layer shadow: a 0.5 dp ring at 4 % plus a 2 dp blur at about 15 %.
- **QA-32: catalog.** Seven things to fix in the catalog:
  - the inset-grouped demo's `backgroundGrouped` band has square corners and no bottom padding,
    so the footer touches its edge;
  - icon-grid and swatch labels clip with no ellipsis ("MoreHorizont", "BookmarkFille",
    "backgroundGroupe"): ellipsize, or let them wrap to two lines;
  - `AppTimeline` draws no connector from the current node to the pending one ("Out for
    delivery" → "Delivered"): draw it dashed or in `separator`;
  - `AppDataTable`'s last row is cut mid-row with no edge fade;
  - `AppCarousel` page dots overlap the card's bottom border;
  - `AppImageViewer`'s close button is a white circle with a near-white ×: use a dark material
    capsule with a white glyph;
  - the perf HUD overlaps long titles: move it under the bar, or into the bottom-leading corner.
- **QA-33 (V): black first frame.** The first capture of Selection, Pickers and Motion is pure
  black (RGB 1,0,2). `Theme.Catalog` sets `windowBackground`, so this is probably capture timing
  in `cp1_record.py` or the QA script. Confirm with `screenrecord` on a cold start in light mode.
  If it is real, it is a black flash.
- **QA-11 (shared with WP04): card stack.** Expose the sheet's presentation fraction from
  `SheetScene` so WP04 can scale the shell layer at the large detent.
