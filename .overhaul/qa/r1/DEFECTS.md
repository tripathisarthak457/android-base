# Visual QA, round 1: defects

Written 2026-10-09 12:20 IST. Read-only inspection: no repository or worktree was changed.

## What was inspected

| Source | What it is | Device |
|---|---|---|
| `qa/r1/shots/light/*.png` (44), `qa/r1/shots/dark/*.png` (25) | The WP04 phase-2 build `com.example.wp04.p2.dev` (`devBenchmarkRelease`): the new shell on the **old** design system. Taken 08:37 to 08:59. | Emulator `periflow_api35`, 1080x2400 at 420 dpi (2.625 px/dp). The inspector's User-Agent reads `sdk_gphone64_x86_64`. |
| `qa/ptr/frames/*`, `qa/ptr/peak_compare.png`, `qa/ptr/*_strip.png` | Pull to refresh on the same build, light and dark. | Redmi Note 10 Pro (9a7c74f4) |
| `perf/WP03/qa/*.png`, `perf/WP03/qa/interact/*.png` | The WP03 catalog on the **new** design system (`wp/WP03` at `69837f4`): 16 sections, light, plus 7 dark overlay frames. | Emulator |

I checked the code against these branch heads: `wp/WP04` `d56ad13` (08:54, clean), `wp/WP03` `69837f4` (08:03, clean), and the integration branch `overhaul/multiplatform-v2` `709612d`.

Measurements come from pixel scans of the PNGs. The colour values are sRGB, and the contrast figures use the WCAG 2 formula.

**Severity.** S1: visible on every launch, or it breaks a task. S2: a clear polish defect a user notices. S3: minor polish. V: needs a device check, because the evidence comes only from code or from a capture that may be an artefact.

**Branch status.** *Fixed on wp/WPxx* means fixed on that unmerged branch. *Planned* means the owner's handoff already lists the fix but it is not applied. *Open* means no branch addresses it.

## Defects

| ID | Sev | Area | What is wrong | Evidence | Root cause (code) | In-progress branch | Owner |
|---|---|---|---|---|---|---|---|
| QA-01 | S1 | Shell, insets | An opaque strip of window background sits around the floating bar and cuts off root screens (REQ 17). | The integration build. The r1 shots (WP04) no longer show it. | Integration `core/navigation/.../AppShell.kt:222` passes `rootBottomInset = AppBottomBarDefaults.occupiedHeight()`, so every root entry is padded to stop above the bar. | **Fixed on wp/WP04** `511be84`: content runs under the bar and each list pads itself with `chromeContentPadding()`. Pending merge. | WP04 |
| QA-02 | S1 | Shell, bar | Under the floating bar the bottom edge looks cluttered. Cards scroll under both the bar and the home indicator with no edge treatment, so the card outline and the bar outline cross. In dark mode the bar and the cards share one colour, `#171E2B`, and the bar seems to melt into the card behind it (REQ 18). | light 01, 07, 31, 34, 42; dark 01, 07 | WP04 `AppShell.kt:177-187`: `ShellChrome` draws the bar straight over the content. `AppTabBar.kt:308-316`: the bar surface is `colors.surface`, the same role as cards. | Open. WP03's `foundation/ScrollEdge.kt` fades the top edge only. | WP04 (WP03 if the modifier is shared) |
| QA-03 | S2 | Bar | The selected tab's filled pill is 52 dp tall in a 64 dp bar (137 of 165 px, 83 %). It crowds the bar, and a saturated accent fill with white label is heavy (REQ 30). | light 01 to 34; dark all | WP04 `AppTabBar.kt:376-389`: the pill is `size.height - 2*ItemInset`. `:564`: `ItemInset = 6.dp` is used for both axes. | Open | WP04 |
| QA-04 | S2 | Bar, motion | While the pill slides between tabs, the labels it passes wash out. The half of a label outside the pill turns near-white on a white bar. | `light/42-tab-switch-transition-sheet.png`, row 2 frame 2 ("Se\|arch"); row 1 last frame | WP04 `AppTabBar.kt:434-436`: each tab gets one tint, `lerp(content, selectedContent, 1-\|indicator-index\|)`, for its whole label. | Open | WP04 |
| QA-05 | S2 | Tab switch | Content swaps instantly. The indicator neither stretches nor morphs, and the icon does not bounce (REQ 28). | 42 sheet | WP04 `AppTabBar.kt:206` (`SelectionSpring` translate only) and `KeptTabs`, which has no content transition. | Open (REQ 28) | WP04 |
| QA-06 | S3 | Bar, icons | The selected tab keeps its outline glyph. Feed has no filled form at all. | All bar shots | WP04 `app/.../ui/AppDestinations.kt:36-50` passes no `selectedIcon`. WP03 `AppIcons` has `HomeFilled`, `SearchFilled`, `UserFilled` and `SettingsFilled`, but no filled list glyph. | Open | WP04, WP03 |
| QA-07 | V | Bar, RTL | The tab order and the pill do not mirror in right-to-left layouts. | Code only; there is no RTL shot. | WP04 `AppTabBar.kt:412-417` uses `place`/`placeWithLayer` rather than the relative calls. `:381` draws the pill from the left. | Open | WP04 |
| QA-08 | V | Rail | In short windows the rail clips its last items: a phone in landscape, a split window. | The catalog rail cuts off its 4th item (`perf/WP03/qa/Bars-light-7.png`). | WP04 `AppTabBar.kt:480-526`: fixed 68 dp items in a non-scrolling `Column`. 5 tabs need 372 dp plus insets. | Open | WP04 |
| QA-09 | S2 | Generator, bar | Seven tabs are crammed into one bar and "Settin…" is truncated (REQ 18). | REQ 18 (user's phone) | `generator/genkit/targets/android/tabs.py:25-29`: every scaffolded module becomes a tab, with no cap and no "More". | Open | WP02-M2 (generator rule), WP04 ("More" tab in the shell), WP01 (spec field) |
| QA-10 | S2 | Generator | Every scaffolded module gets the same tab icon (Orders and Wallet) (REQ 18). | REQ 18 | `tabs.py:16` hard-codes `icon = AppIcons.Grid` with no `selectedIcon`. | Open | WP02-M2, WP01 |
| QA-11 | S2 | Sheets, motion | A modal sheet dims the screen behind it but does not shrink it into a card stack (REQ 21). | `perf/WP03/qa/interact/sheet_large.png`, `sheet_medium.png` | Nothing scales the presenting layer. | Open | WP04 (presentation host), WP03 (`SheetScene`) |
| QA-12 | S1 | Toast | The "Saved" toast draws behind the floating bar: only green ears show on either side. In dark mode it is hidden entirely. | light 18a, 18b; dark 18 | WP04 `core/ui/.../MviScreen.kt:55-57` puts `MessageHost` inside the screen's own Box, which sits below the shell chrome. `AppSnackbar.kt:99-119` pads only the navigation bar and ignores `LocalBottomChrome`. | **Planned**: WP03 "Remaining work" step 1 routes `MessageHost` to `LocalOverlayHost.toasts`. `AppToast` already rests above `LocalBottomChrome` (`8eae2c0`). | WP03 |
| QA-13 | S2 | Sheet scrim | With the language sheet open, the status bar band stays lighter than the rest of the scrim (RGB 95,97,100 against 57,60,67). The scrim stops short of the status bar. | light 22b (top 250 px) | The old `AppBottomSheet` window is not edge-to-edge. | Likely fixed on wp/WP03: `AppSheet` renders in the overlay host (`AppSheet.kt:343`). Verify with a shot after the sweep. | WP03 |
| QA-14 | S2 | Sheet, picker | The language sheet has no checkmark on the current choice, no grabber and no row separators. Cancel is a full-width tinted pill. | light 22b; dark 22 | Settings feature on the old sheet. WP03 `AppRow` has no checkmark accessory. | Partly fixed: `AppSheet` has a grabber. The checkmark row is missing. | WP03 (row), WP02-M3 (picker) |
| QA-15 | S2 | Alert | The sign-out icon is clipped and pinned to the top-left of its circle instead of centred. | light 23b (zoomed); dark 23 | WP04 `AppDialog.kt:113-129`: the inner `Box(contentAlignment = Center)` does not fill the `AppSurface`. | **Fixed on wp/WP03**: `AppAlert.kt:109` draws a centred 28 dp icon. Lands with the sweep. | WP03 |
| QA-16 | S2 | Dark contrast | The destructive "Sign out" button in dark mode is white on `#F0666D`: 3.08:1. | dark 23 | Old design-system destructive colours. | **Fixed on wp/WP03**: `danger.on = readableOn(DangerDark, Ink900)`, and alert actions are text. | WP03 |
| QA-17 | S2 | Sheet | Sheet content has no horizontal inset. The body text of the large sheet starts at x=0. | `interact/sheet_large.png` | WP03 `AppSheet` passes content through without the gutter, and `catalog/.../CatalogOverlays.kt:92` passes a bare `AppText`. | Open | WP03 |
| QA-18 | S2 | Context menu | A long-press menu has no lifted preview, no backdrop and no item separators (REQ 21). | `interact/menu.png` | WP03 `AppMenu` is a plain `Popup`. | Open | WP03 |
| QA-19 | S3 | Top bar | An `AppTopBar` that is not at the window top still pads the status bar, which leaves a ~24 dp empty band above it. | Catalog Bars and Containers demos ("Messages", "Checkout") | WP03 `AppTopBar.kt:81`, `:126`: `windowInsetsPadding(statusBars)`. | Open (sheets consume the inset; panes and demos do not) | WP03 |
| QA-20 | S1 | Pull to refresh | The old version, as recorded: a 38 dp disc sits over the text, the content does not move, and in dark mode a black elevation ring appears. The offset is read in composition, so it recomposes every frame (REQ 29). | `qa/ptr/peak_compare.png`; `dark_slow` f_0100 to f_0110 | WP04 `refresh/AppPullToRefresh.kt:98-112`: progress is read in the composable, `AppSurface(elevation = overlay)` is drawn under an alpha layer, and `:57`/`:69` launch a coroutine per scroll event. | **Replaced on wp/WP03**: content follows the finger, spokes are drawn, the threshold haptic plays, there is no disc, and reads happen in draw. Pending merge. | WP03 |
| QA-21 | S2 / V | Pull to refresh (new) | The new pull to refresh still falls short of iOS in five ways: (a) the springs back do not carry release velocity. (b) An upward fling is swallowed. (c) The drawn spokes are swapped for a separate spinner on release. (d) Under the new `AppScaffold` the spinner would sit under the top bar. (e) The refresh starts on release, not when the threshold is crossed (unverified against iOS). | Code; catalog Lists | WP03 `AppPullToRefresh.kt:74-77`, `:105` (`animate` without `initialVelocity`); `:116` (returns all velocity); `:144-148`; `:136-139` (no `LocalTopChrome`). | Open | WP03 |
| QA-22 | S2 | Offline | A pull while offline settles back silently. The cached list shows no offline cue. | light 31, 34 | Sample feature: refresh failure is not surfaced. | Open | WP02-M3 |
| QA-23 | S2 | Dark, segmented | In dark mode the selected segment's thumb is darker than its track. Old design system: 1.16:1. The new design system repeats the inversion. | dark 20, 21 | WP03 `AppSegmentedControl.kt:84`, `:101`: the thumb is `surfaceElevated` (`Ink700 #1D232E`) on `fill` (`Ink600 #262D3A`), from `AppColors.kt:163-165`. | **Open** (not fixed in WP03) | WP03 |
| QA-24 | S2 | Dark, switch | In dark mode the switch thumb is navy on a blue track. | dark 20 to 22 | Old `AppSwitch`. | **Fixed on wp/WP03**: the thumb is always `Color.White` (`AppSwitch.kt:166`). | WP03 |
| QA-25 | S2 | Text field | In a multi-line field the placeholder sits vertically centred, but typed text starts at the top, so the text jumps on the first keystroke. | light 16, 17; dark 16; catalog Inputs "Note" | WP03 `AppTextField.kt:176-186`: the Row is `CenterVertically`, the Box is `CenterStart`, and the placeholder is `maxLines = 1`. | **Open** (also in the new design system) | WP03 |
| QA-26 | S3 | Chip | On a removable chip the × sits 3 dp from the label and leaves ~20 dp empty after it. | Catalog Selection ("Removable×"), Fields tags | WP03 `AppChip.kt:50-92`. Likely cause, unverified: `pressable`'s touch-target sizing widens the remove box beyond `RemoveSpace = 30.dp`. | Open | WP03 |
| QA-27 | S3 | Button | A disabled filled button is white on pale accent, 1.85:1. It reads as broken rather than inactive. | light 16, 35 to 38; catalog Buttons | Old design system. WP03 `Modifiers.kt:23` uses `alpha(0.38f)` over the whole button. | Open (WCAG exempts inactive controls; this is polish) | WP03 |
| QA-28 | S3 | Avatar | Initials come out wrong: "Author 10" gives "A1" and "You" gives "YO". On the 88 dp profile avatar the initials are tiny. Dark tints barely separate from the cards. | light 07, 16; dark 08b, 16 | WP03 `AppAvatar.kt:49` uses fixed `footnote` text. `:102-107` takes digits as initials and `take(2)` of a single word. `:112-114` picks subtle tints. | Open | WP03 |
| QA-29 | S3 | Link buttons | Link buttons sit inset from the text column: "Show more" by ~15 dp in Feed, "Create account" and "Forgot password" in sign-in, "Clear" in Search. | light 07, 35, 14 | The old text button's content padding. WP03 `AppLinkButton` needs a check. | Verify on wp/WP03 | WP03 |
| QA-30 | S3 | Section header | "See all" sits ~16 dp above the "Recent" baseline. The header in the lazy list is not inset to the row text. | Catalog Lists | WP03 `AppSection.kt:79-91`: a Row aligned `Bottom` with a 48 dp-target link. | Open | WP03 |
| QA-31 | S3 | Field | Helper text is indented 2 dp from the field label. | light 37 | WP03 `AppTextField.kt:194`: `padding(horizontal = 2.dp)`. | Open | WP03 |
| QA-32 | S3 | Catalog | Seven catalog-only defects. The grouped-section demo shows a square band with no bottom padding. Swatch and icon labels clip with no ellipsis ("MoreHorizont", "BookmarkFille", "backgroundGroupe"). The timeline has no connector into the pending node. The table cuts off its last row mid-row. Carousel dots overlap the card border. The image viewer's close × is near-invisible white on white. The perf HUD crowds long titles. | `perf/WP03/qa` Lists, Text, Foundations, Data, Collections | The catalog demos, plus `AppTimeline` and `AppImageViewer`. | Open | WP03 |
| QA-33 | V | Cold start | Entering the Selection, Pickers and Motion pages captured a pure black frame (RGB 1,0,2). | `Selection-light-0`, `Pickers-light-0`, `Motion-light-0` | The catalog theme sets `windowBackground`, so this is probably capture timing. Recheck with `screenrecord`. | Verify | WP03 |
| QA-34 | S3 | Slider | A white slider thumb on a near-white page has only a faint shadow. | Catalog Selection | WP03 `AppSlider` thumb shadow. | Open | WP03 |
| QA-35 | S1 | Root screens | The large title and search field are a static header above the list. Scrolled content is cut by a hard horizontal edge: no collapse, no edge effect. | light 02, 08a, 08b, 13, 21; dark 02, 08b, 13 | WP04 old `AppScaffold.kt:63-65` stacks `topBar()` above `content` in a `Column`, and the features put title and search in `topBar`. | Open. The parts exist on wp/WP03 (`AppLargeTitleTopBar`, `scrollEdgeFade`), but there is no search-accessory slot. | WP02-M3 (screens), WP03 (search slot) |
| QA-36 | S2 | Detail | The inline bar title repeats the H1 below it. The bar is solid white at rest. | light 05, 06; dark 06 | `SampleDetailScreen.kt:40-41` uses `AppBackTopBar(title = item.title)` with no scroll link. | Open. WP03 `AppTopBar(scrolled = …)` supports a bar that is clear at rest. | WP02-M3 |
| QA-37 | S2 | Detail push | A pushed detail first shows the "Detail" placeholder and skeleton, then swaps to the content mid-slide. | `light/41-push-pop-transition-sheet.png` frames 2 to 5; dark 05 | `SampleDetailScreen.kt:41` falls back to `R.string.sample_detail_title` until the item loads. | Open | WP02-M3 |
| QA-38 | S2 | Settings | Each setting sits in its own card under heavy bold section headers. Haptic feedback uses a bell (notification) glyph. The version string is monospace. The rhythm is uneven: 34 px under the title, 48 px between sections. | light 20, 21; dark 20, 21 | `feature/settings/.../Settings.kt:242-414` uses `AppCard` per row, `AppIcons.Bell` at `:271` and `AppMonoText` at `:413`. | Open | WP02-M3 |
| QA-39 | S3 | Copy | The offline state repeats "You are offline" in its title and its message. The action is "Retry" on Feed but "Try again" on Search. | light 32, 33; dark 32 | `core/ui` `common_error_offline`, `feed_retry`, `search_retry`. | Open | WP02-M3 |
| QA-40 | S2 | Sign-in | "Continue with Google" has no Google "G" mark, which Google's branding guidelines require. Disabled Sign in gives no reason. An invalid email is not flagged on blur. | light 35, 36 | Auth feature | Open | WP02-M3 |
| QA-41 | S3 | Profile, keyboard | While editing, the "Save changes" button is half-hidden at the keyboard's top edge. | light 17; dark 17 | Profile form: nothing keeps the primary action in view above the IME. | Open | WP02-M3 |
| QA-42 | S3 | Feed | Excerpts keep the API's hard line breaks, so two-line excerpts come out ragged. | light 07, 08a, 08b | `data/feed/.../FeedPostDto.kt:18-19`: `body = body.trim()` (the sample mapper does replace `\n`). | Open | WP06 |
| QA-43 | S3 | Search | Search results do not highlight the matched term. | light 12, 13 | Search feature | Open | WP02-M3 |
| QA-44 | S1 | DEV badge | The DEV badge overlaps the large title on the Redmi (REQ 30). On pushed screens it sits on the back button and can take its taps. A dragged badge can be dropped over titles. | `qa/ptr/frames/*`; light 05, 06, 30, 37, 38 | WP04 `core/devtools/.../DevBadge.kt:62-63`, `:104`: a fixed `48.dp` from the top that ignores the status bar and top chrome. `:91`: `minTouchTarget = 0.dp`. No edge snapping. | Open | WP06 |
| QA-45 | S2 | DEV badge, dark | In dark mode the badge is white on amber `#E9A23B`: 2.17:1. | dark all | `DevBadge.kt:88`, `:98`: `warning.content` background with `Color.White` text. | Open | WP06 |
| QA-46 | S2 | Inspector | Opaque grey scrim bands sit under the status bar and the navigation bar. The badge peeks out at the top-left under the panel. | light 25 to 29; dark 25, 28, 29 | `DevToolsPanel.kt:58-66`: a platform `Dialog` without `decorFitsSystemWindows = false`. | Open | WP06 |
| QA-47 | S2 | Inspector data | The request body of a GET shows "e71". Every size shows "0 kB". Failed shows "0  (0%)" with a double space. | light 26, 28 | `core/network/.../NetworkRecording.kt:31-34`: `content::class.simpleName` is the R8-obfuscated name of Ktor's `EmptyContent`. `:36`, `:47-48`: size comes from Content-Length only, which chunked or gzip responses lack. | Open | WP06 |

## Not defects

These were seen in the captures but are not app defects:

- **light 19:** a scrim with no sheet on top. "Change photo" launches the system photo picker (`rememberMediaPicker`), and the frame caught the system UI opening.
- **light 24:** Chrome's first-run screen inside the privacy-policy Custom Tab. That is device state.
- **Green "120" digits over the DEV badge** in the pull-to-refresh recordings. That is the Developer options refresh-rate overlay, not the app.

## Coverage gaps for round 2

These were not captured. Please capture them on the merged build with the new design system.

- Font scale 1.3 and 2.0
- RTL, with `ar` or pseudo-locale `ar-XB`
- Tablet and foldable, both postures; landscape phone, to check the rail (QA-08)
- 7-tab and scaffolded-module specs (QA-09, QA-10)
- Glass style
- Auth screens in dark mode
- Every catalog section in dark mode
- Tab switch and pull to refresh on the Redmi at 120 Hz with `screenrecord`

## Sources

- iOS scroll edge effect, the soft and hard boundary between pinned bars and scrolling content: <https://developer.apple.com/documentation/swiftui/scrolledgeeffectstyle>
- Apple HIG, Tab bars. The 3 to 5 tab guidance and the "More" tab on iPhone were read from a search summary, not the page itself, which did not render: <https://developer.apple.com/design/human-interface-guidelines/tab-bars>
- Compose navigation bar guidance (3 to 5 destinations): <https://developer.android.com/develop/ui/compose/components/navigation-bar>
- Google Sign-In branding ("The button must always include the standard color for the Google 'G'"): <https://developers.google.com/identity/branding-guidelines>
- Full-screen dialogs edge-to-edge (`decorFitsSystemWindows = false`) and gradient protection behind system bars: `~/.claude/plugins/marketplaces/android-skills/system/edge-to-edge/SKILL.md` (§ Dialogs, `SystemBarProtectionSnippets`)
- WCAG 2.1 SC 1.4.3, which exempts inactive components: <https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html>

Per-owner action lists: `for-WP03.md`, `for-WP04.md`, `for-WP06.md`, `for-WP02-M3.md`, `for-WP02-M2.md`, `for-WP01.md`.
