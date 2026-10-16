# Visual QA round 1: WP04 (shell, bar, navigation)

Full table: `DEFECTS.md`. Code references are to `wp/WP04` at `d56ad13`, under `templates/android/`.
Each item states what you see, the cause, the fix, and how to prove it. Every fix must keep the
perf gates (`perf.sh` on a release build on the Redmi) and must not recompose per frame. Read
animated values only in layout, draw or `graphicsLayer`.

## QA-01 (S1). Opaque block around the bar (REQ 17): fixed on your branch, keep it fixed

On the integration branch, `core/navigation/.../AppShell.kt:222`
(`rootBottomInset = AppBottomBarDefaults.occupiedHeight()`) pads every root entry above the bar.
The window background then shows as a strip around the capsule. Your `511be84` replaces this with
content under the bar plus `chromeContentPadding()`. The r1 shots confirm it: no strip on any root
screen.

Acceptance: add a screenshot or semantics test where a root list's last item, scrolled to the
end, sits fully above the bar while its first items scroll under it. Also run a grep guard in
`verifyCodeRules` that fails any `padding(bottom = …occupiedHeight…)` on a root.

## QA-02 (S1). The bottom edge under the floating bar is cluttered (REQ 18)

You see cards scroll under both the capsule and the home indicator with no transition: outlines
cross the bar's border (light 01, 31, 34). In dark mode the bar surface `#171E2B` equals the card
colour, so the bar merges with the card behind it (dark 01 crop: two rounded outlines
intersecting).

The cause is in `AppShell.kt:177-187`: `ShellChrome` draws `chrome()` straight over `content()`.
`AppTabBar.kt:308-316` sets `surface = colors.surface`, the same role as cards.

The fix:

1. Draw a bottom edge protection in `ShellChrome`, between content and chrome. Use a gradient in
   the background colour, not a blur and not an offscreen mask:
   ```kotlin
   Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
       .layout { m, c ->            // height = bottom chrome + 24.dp, read in layout
           val h = (bottomChrome.value + EdgeTail).roundToPx()
           val p = m.measure(Constraints.fixed(c.maxWidth, h)); layout(p.width, h) { p.place(0, 0) } }
       .drawBehind {
           drawRect(Brush.verticalGradient(0f to bg.copy(alpha = 0f), 0.55f to bg.copy(alpha = 0.78f), 1f to bg.copy(alpha = 0.94f)))
       })
   ```
   This costs one rect per frame: no offscreen layer, no recomposition. It is the same pattern as
   the system-bar protection in the edge-to-edge skill
   (`~/.claude/plugins/marketplaces/android-skills/system/edge-to-edge/SKILL.md`,
   `SystemBarProtectionSnippets`). iOS 26 does the same job with its scroll edge effect
   (`ScrollEdgeEffectStyle` soft and hard; see https://developer.apple.com/documentation/swiftui/scrolledgeeffectstyle).
   Hide it with the bar: same `followCovered` progress, alpha in `graphicsLayer`, and 0 while the
   keyboard covers the bar.
2. Give the bar its own elevated surface in dark mode: `surfaceElevated` (`Ink700` in WP03's
   palette) rather than `surface`. Add a 0.5 dp top hairline at white 6–8 %, so it reads as
   floating over content of the same colour. Keep `dropShadow` for light mode only; in dark mode
   it is invisible and still costs a blur.
3. If WP03 wants one shared modifier for both edges, ask for
   `Modifier.edgeProtection(top = …, bottom = …)` in `foundation/ScrollEdge.kt`. The colour
   gradient matches the existing `DstIn` strip on any solid background, without its offscreen
   layer.

To verify: light and dark shots of 01, 07, 31, then dark 01 cropped at the bar. Scroll janky %
and GPU p90 must not move against `perf/WP04/phase2/summary.md` (scroll_root GPU p90 6 ms).

## QA-03 (S2). The selected pill nearly fills the bar (REQ 30)

Measured: the pill is 137 px (52 dp) tall in a 165 px (~64 dp) bar, 83 % of it, with a saturated
accent fill and white content.

The cause is `AppTabBar.kt:376-389` (pill height = `size.height - inset*2`) together with
`:564` (`ItemInset = 6.dp`), used for both axes.

The fix:

- Split the insets: vertical `PillInsetV = 10.dp` makes the pill 44 dp in a 64 dp bar, and
  horizontal `PillInsetH = 4.dp`.
- Cap the width at `min(slot - 8.dp, 72.dp)` and centre it in the slot.
- Default to WP03's `barIndicator` role. For the Utility style, use the subtle treatment
  (`BarIndicator.Subtle` in WP03's `AppDesignStyle`): accent-subtle fill with accent glyph and
  label. iOS tints the selected tab rather than filling it with the accent.
- Keep the filled pill as a style option.
- Label and pill must share the same horizontal inset, so a label can never overflow its pill
  (the REQ 18 item).

To verify, take bar shots at 5 tabs, at 4 tabs and at font scale 1.3.

## QA-04 (S2). Labels wash out while the pill slides

You see this in `light/42-tab-switch-transition-sheet.png`. In row 2, frame 2, the pill
half-covers "Search": "Se" is near-white on the white bar and "arch" is grey on blue. In row 1's
last frame, "Profile" and "Settings" are washed out.

The cause is in `AppTabBar.kt:434-436`. Each tab gets a single colour,
`lerp(content, selectedContent, 1 - |indicator - index|)`, so a half-covered tab is half-tinted
everywhere.

The fix is a masked two-layer render. Both layers are composed once, and per frame only the clip
changes, in draw:

- the base `TabRow` draws every glyph and label in `look.content`, with no per-tab lerp;
- an overlay copy draws them in `look.selectedContent`, clipped to the pill:
  ```kotlin
  Modifier.drawWithContent {
      pill.rewind(); pill.addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(h / 2)))
      clipPath(pill) { this@drawWithContent.drawContent() }
  }
  ```
  Reuse one `Path`. Use `clearAndSetSemantics {}` on the overlay so TalkBack reads each tab once.

Check the edge quality on the Redmi. If HWUI's `clipPath` edge looks jagged, draw the overlay
into a bar-sized layer with a pill mask (`BlendMode.DstIn`). That is one small offscreen layer,
only while the indicator moves.

## QA-05 (S2). The tab switch is an instant swap (REQ 28)

The selection moves as a pill translating on `SelectionSpring` (`:206`). There is no content
transition, no stretch and no icon bounce.

The design to implement and record:

- **Content.** Fade the incoming kept tab through a veil: one full-screen rect in
  `colors.background`, alpha 1 to 0 over about 150 ms on `springs.fadeQuick`, drawn over the
  incoming tab only. The outgoing tab is removed at once. There is no double exposure and no
  offscreen alpha on a full screen. Optionally add `translationY` 8 dp to 0 on the incoming
  layer.
- **Indicator.** Give the pill's leading and trailing edges two `Animatable`s. The edge facing
  the target runs on a stiffer spring, for example response 0.22 s against 0.34 s, so the pill
  stretches towards the target and contracts on arrival. Both are read in `drawBehind`.
- **Icon bounce.** On select, the new icon scales 1 → 1.12 → 1 on `springs.emphasis` in
  `graphicsLayer`. Under reduce motion, use none of these.
- **Gate.** The tabs scenario stays at about 0 % janky with no dropped first frame on the Redmi.
  Record light and dark with `screenrecord`.

## QA-06 (S3). The selected tab keeps the outline glyph

`app/.../ui/AppDestinations.kt:36-50` passes no `selectedIcon`. After WP03 merges, pass
`AppIcons.HomeFilled`, `SearchFilled`, `UserFilled` and `SettingsFilled`. WP03 must add a filled
list glyph for Feed (asked in `for-WP03.md`).

## QA-07 (V). The bar does not mirror in RTL (found in code)

`AppTabBar.kt:412-417` calls `placeable.place(...)` and `placeWithLayer(index * slot, 0)`, and
`:381` computes the pill's `x` from the left. Neither mirrors.

The fix: use `placeRelative` and `placeRelativeWithLayer`. In `drawBehind`, compute
`x = if (layoutDirection == Rtl) size.width - (indicator + 1) * slot else indicator * slot`.
Mirror the minimize target too. Add an RTL screenshot test (`ar-XB`).

## QA-08 (V). The rail clips in short windows

The rail lays out fixed 68 dp items in a non-scrolling `Column` (`:480-526`). Five tabs need
372 dp plus insets, more than a landscape phone or a split window offers. The catalog rail
already cuts off its 4th item (`perf/WP03/qa/Bars-light-7.png`; that is WP03's rail, which you
delete, but yours has the same structure).

The fix: compress the item height to `max(56.dp, available / count)`, then scroll if it is still
too tall. Test on a 360 dp-tall window.

## QA-09 (S2). Seven tabs in one bar, "Settin…" truncated (REQ 18)

Give the shell an overflow rule. On a compact width with more than 5 tabs, show the first 4 plus
"More" (`AppIcons.MoreHorizontal`). "More" is a kept tab whose root lists the remaining
destinations and pushes them inside its own stack, as iOS's More tab does. The HIG's
recommendation is 3–5 tabs (https://developer.apple.com/design/human-interface-guidelines/tab-bars;
read from a search summary, not the page). Compose's navigation bar guidance also says 3–5
destinations (https://developer.android.com/develop/ui/compose/components/navigation-bar).
The rail shows them all (with QA-08). WP02-M2 emits the tabs in priority order; the string
"More" needs translations.

## QA-11 (S2). No card stack behind modal sheets (REQ 21)

At the large detent, `PresentationHost` should transform the shell layer: content plus bar,
scale to about 0.93, `translationY` = status bar + 10 dp, rounded 12 dp corners
(`graphicsLayer { shape; clip = true }`), with the scrim on top. Drive all of it from the sheet's
presentation fraction, which `SheetScene` exposes; read it only in `graphicsLayer`. At the medium
detent, scrim only, as iOS does. Coordinate with WP03 on that fraction. Gate it with a sheet
journey.

## Shared with other owners (FYI)

- QA-12: toasts behind the bar. WP03's sweep moves `MessageHost` to the overlay host. Do not add
  bottom padding in screens to dodge it.
- QA-35: root headers are clipped hard by the old `AppScaffold` (your worktree's copy at
  `component/container/AppScaffold.kt:63-65`). WP02-M3 moves the screens to WP03's scaffold;
  keep `LocalTopChrome` and `LocalBottomChrome` identical through your merge.
