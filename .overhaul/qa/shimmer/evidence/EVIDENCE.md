# Shimmer and clipped content: evidence (Oct 12, 2026)

**The user's report** (13:35, watching WP03's pull-to-refresh test app `com.example.wp03.ptr.dev`): "shimmer has
issues and content gets cut".

**Scope.** This file collects evidence only. No repository was modified. Every path below is relative to
`/home/crazyhusir/Documents/projects/tools/.overhaul/qa/shimmer/evidence/`.

## What the user saw

- **The run they watched.** WP03's `ptr_r8_session.sh` started at 13:33:26 and finished at about 13:36. An earlier run
  had finished at 13:30.
- **Where the skeleton came from.** Each run cold-starts the app 8 times: 3 launches in each of the two
  `ptr_measure.sh` calls, plus 2 in `ptr_record.sh`. Each cold start shows the Samples skeleton for about 1 s while the
  list loads from jsonplaceholder, which has no cache.
- **Why WP03's recordings miss it.** WP03's recordings start 4 s after launch, so they contain no skeleton. I recorded
  the cold starts myself.
- **What the user most likely saw:**
  - The sweep running out of step and too fast, chopping the lines (D2 to D4).
  - A skeleton card sitting under the status bar, cut off by the title band (D1).
  - The cards jumping when the content replaces the skeleton (D5).
  - During each pull, the last card sliced by the opaque strip behind the tab bar (D6).
- **Landscape in the 13:30 run.** Its `dark_slow` recording is in landscape: the phone was held sideways with
  auto-rotate on. See `frames/r8-1330-dark-landscape-f001.png`. The 13:35 run is in portrait.

## Setup

**Phone:**
- Redmi Note 10 Pro, serial `9a7c74f4`, 1080x2400, 440 dpi (2.75), 120 Hz.
- One adb lock hold, 13:39:51 to 13:48:58.

**Recordings:**
- `screenrecord --size 720x1600`.
- One recording pixel is 1.5 device px, which is 0.545 dp.
- Frames were extracted with `ffmpeg -fps_mode passthrough` (120 fps) after `bin/memwait`.

**Settings changed and restored:**
- Night mode, font scale 1.5, rotation lock (landscape and portrait).
- All restored, and verified identical: `device/initial-state.txt` and `device/final-state.txt`.
- No network setting was touched.

**Builds (all release, none rebuilt):**

| Label | APK | Notes |
|---|---|---|
| WP03 ptr (old shell) | `out/WP03/ptr/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk` (13:26) | The build the user watched. Generator 718c1e7, style Utility, features sample, network and catalog. `AppSkeleton.kt` is identical to `wt/WP03` (shared `ShimmerClock`, per-element band). |
| WP03 catalog | `out/WP03/cp1/catalog/build/outputs/apk/release/catalog-release.apk` (13:30) | Package `com.example.wp03.cp1.catalog`. Same `AppSkeleton.kt`. Opened with `--es section Feedback`. |
| WP04 final-everything (new shell) | `out/WP04/final-everything/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk` (Oct 10, `BUILT_FROM` 1d7a901, dirty) | Installed copy verified by md5 `587c7594…`. Launched with `--es perf.fakeSession true --es perf.theme light\|dark`. It carries the older `AppSkeleton` (one `rememberInfiniteTransition` per placeholder, highlight = `surface` in light). |

**WP03's own recordings, preserved before they were overwritten:**
- `wp03-r8-1330/`: mp4s, strips, gfxinfo and rest screenshots.
- `wp03-r8-1335/`: the same files from the run the user watched.

## Defect index

| ID | Severity | Owner | Where | Summary |
|---|---|---|---|---|
| D1 | High | WP03 | Samples (and Feed, Search) loading state, light, dark, 1.5x and landscape | Skeleton is laid out under the top chrome. Card 1 shows through the status-bar band, and card 2 is hidden behind the large title. |
| D2 | High | WP03 | `AppSkeleton`, every placeholder, both themes | Each placeholder sweeps its own width. Bands are out of step, sized 11 to 94 dp, and move at 60 to 525 dp/s. |
| D3 | Medium | WP03 tokens (worse in the WP04 build) | Light theme | The highlight raises the placeholder almost to the card or page colour, so lines look chopped. In dark it is a bright bar. The two themes are inconsistent. |
| D4 | Medium (cause unconfirmed) | WP03 | Both skeleton implementations on the phone | The sweep cycle is 0.933 s against a declared 1.4 s, about 1.5x fast. A `delay()` spinner in the same video keeps real time. |
| D5 | High | WP03 (and WP04 build) | Skeleton to content | The skeleton is not shaped like the content. Cards change height and position on the frame the data arrives, which is worse at 1.5x. |
| D6 | High | Old shell (ptr app). WP04 does not fix it for Utility | Bottom bar, every screen | An opaque full-width strip slices the last card, at rest and during pull to refresh (REQ 17). |
| D7 | High | WP04 build (merge) | Every tab header in final-everything | The large title never collapses. The list is sliced at the header's bottom edge while scrolling. |
| D8 | Medium | WP03 and WP04 | Search: start state to loading | The crossfade double-exposes the empty state over the skeleton rows (alpha on full content layers). |
| D9 | Medium | WP04 build (app theme) | Cold start with the app's theme set to dark | White splash, a light-themed first frame, then a grey wash to dark. The tab bar is missing during the wash. |
| D10 | Low | WP03 catalog | Catalog with `--es theme dark` on a light phone | Status-bar icons stay dark on the dark bar and are illegible. |
| D11 | Low | WP03 | Landscape list pane | The search accessory sits flush against the inline bar's hairline, and titles clamp hard in the narrow pane. |
| D12 | Low | WP03 / WP04 | Font scale 1.5 | The large title does not scale. The search glyph stays small next to larger text. "Settings" in the WP04 bar ends about 9 dp from the screen edge. |

---

## D1: The skeleton is laid out under the top chrome (cut at the top)

**What is wrong.** In the loading state the skeleton list starts at y = 16 dp of the screen, not below the bar.
- Card 1 shows through the transparent status-bar and inline band, behind the clock and the battery.
- The opaque large-title band cuts card 1 off at its bottom edge.
- Card 2 is entirely behind the title and the search field. Only its bottom border peeks out under the field.
- When the data arrives the list re-lays out below the bar (see D5).
- In landscape the bar is inline and opaque, and the first card's top runs up under the search field.

**Where.** WP03 ptr app, Samples tab, Loading state. Seen in light, dark, font scale 1.5 and landscape.

**Evidence:**
- Light, top crop: `frames/ptr-cold-light-skeleton-top.png`. Full frame: `frames/ptr-cold-light-skeleton-full.png` (t = 1.75 s).
- Dark: `frames/ptr-cold-dark-skeleton-top.png` and `frames/ptr-cold-dark-sheet.png`.
- 1.5x: `frames/ptr-cold-font150-skeleton-to-content.png`, left half.
- Landscape: `frames/ptr-cold-landscape-skeleton-vs-content.png`, left is the skeleton, right is the content.
- Recordings: `rec/ptr-cold-light.mp4`, `rec/ptr-cold-dark.mp4`, `rec/ptr-cold-font150-light.mp4` and
  `rec/ptr-cold-landscape-light.mp4`.

**Measured.** White-run analysis of column x = 672 in recording px on `frames/ptr-cold-light/f_0162.png`, the last
skeleton frame (t = 2.261 s):

| What | y range (recording px) |
|---|---|
| Card 1, behind the status bar | 34–162 |
| Card 2 | Hidden under the title |
| Visible skeleton cards | 408–565, 596–753, 783–940, 971–1127 |

**Cause (code).**
- `wt/WP03/.../feature/sample/list/SampleListScreen.kt` `LoadingList()` pads with `PaddingValues(AppTheme.spacing.gutter)`.
  `Content()` uses `chromeContentPadding(...)`.
- `AppScaffold` runs content under the top bar by design.
- At rest, `AppLargeTitleTopBar` paints the screen colour only behind the large-title band, so the band above it is
  transparent.

**Same pattern elsewhere in WP03 (code reading; no WP03 build with these features to run):**
- `FeedScreen.LoadingFeed()`.
- `SearchScreen.LoadingResults()`.
- `SearchScreen.Results()`. This one cuts real search results at the top and the bottom, not only skeletons.

**Merge hazard.** WP04's branch already uses `chromeContentPadding` in all four places.

**Reproduce:**

```
adb install -r out/WP03/ptr/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk
adb shell pm clear com.example.wp03.ptr.dev
adb shell am start -n com.example.wp03.ptr.dev/com.example.wp03.ptr.MainActivity
```

Watch the first 1–2 s, or record and look at t = 1.1–2.2 s.

## D2: The sweep is not one band crossing the screen

**What is wrong.**
- Every placeholder runs its own sweep across its own width, and the band is 60% of that width.
- The skeletons share one `progress`, so all placeholders light up at their left edges at the same moment. At any
  instant, though, the bands sit at different x positions.
- Circles get a narrow, slow band. Wide blocks get a wide, fast one.
- Inside one sample-list card, the 0.6 and 0.9 lines are visibly out of step.
- Each cycle has a gap with no highlight anywhere, about 0.07 s on the block and up to about 0.3 s on the circles.

**Where.** WP03 `AppSkeleton` (catalog Feedback → Skeletons, and the Samples loading state), light and dark.

**Evidence:**
- Dark: `frames/cat-skel-dark-f040.png`. Every band sits at about 85% of its own element, so the bands form a
  staircase.
- Light: `frames/cat-skel-light-f040.png`.
- One sample card over 5 instants: `frames/ptr-cold-light-sweep-strip.png`.
- Recordings: `rec/cat-skel-light.mp4` and `rec/cat-skel-dark.mp4`.

**Measured** (`analysis/sweep-catalog-light.tsv`, `analysis/sweep-catalog-dark.tsv`; speed is a least-squares fit per
visible run):

| Placeholder | Width | Band FWHM | Speed |
|---|---|---|---|
| Circle 40 dp | 39 dp | 11 dp | 62 dp/s |
| Line in the list item | 165 dp | 45 dp | 235 dp/s |
| Line 0.9 | 248 dp | 66 dp | 363 dp/s |
| Line 0.7 | 252 dp | 67 dp | 386 dp/s |
| 80 dp block | 360 dp | 94 dp | 525 dp/s |

**Peak x at the same instant** (light, t = 0.759 s, recording px):

| Circle 1 | Line 1 | Line 2 | Line 0.7 | Circle 2 | Block |
|---|---|---|---|---|---|
| 94 | 295 | 367 | 243 | 62 | 339 |

That is a 305 px (166 dp) spread across a 6-element group.

**Cause.** In `AppSkeleton.kt`, `band = size.width * BandFraction` and `x = -band + (size.width + band) * clock.progress`.
Both are in each placeholder's own coordinates.

**Comparison.** WP08's SwiftUI skeleton sweeps one band over the whole skeleton (commit 8e1485d).

**Reproduce:**

```
adb shell am start -n com.example.wp03.cp1.catalog/.CatalogActivity --es section Feedback [--es theme dark]
```

Scroll to Skeletons and record.

## D3: In light the band erases the placeholder; in dark it is a bright bar

**What is wrong.**
- **Light.** The highlight brings the placeholder up to the colour of the card or page under it. As the band passes,
  the line seems to break apart or be cut away.
- **Dark.** The same band is a strong bright bar.
- **WP04 build.** Its older skeleton uses `highlight = surface` in light, so the right ends of the lines melt
  completely into the white card.

**Where.** WP03 ptr app (cards), WP03 catalog (page), WP04 final-everything (cards).

**Evidence:**
- WP03 sample card: `frames/ptr-cold-light-sweep-strip.png`.
- Catalog: `shots/cat-skel-light.png` against `shots/cat-skel-dark.png`.
- WP04: `frames/evr-cold-light-skeleton-crop.png`.

**Measured** (luma, 0–255):

| Surface | Skeleton | Band peak | Surrounding colour | Contrast to surrounding |
|---|---|---|---|---|
| WP03 sample card, light | 236.6 | 250.4 | Card 255 | 18.4 → 4.6 (75% removed) |
| Catalog page, light | 235.5 | about 248 | Page 247.4 | Band is brighter than the page (the placeholder vanishes) |
| Catalog page, dark | 31.9 | about 62 | Page 11.6 | 20 → 50 (2.5x) |

No dark fringe at the band edges. The gradient is interpolated premultiplied, and the minimum excess in light is
−1.3 luma. The only dark-theme dips are at a rounded line end in 6 of 317 frames, which is compression.

## D4: The sweep runs about 1.5x faster than declared (cause unconfirmed)

**What is wrong.** `ShimmerClock` declares `PeriodMillis = 1_400`. The pattern repeats every **0.933 s**. This holds
for all six catalog placeholders in light and dark, and on the Samples skeleton.
- The dark 360 dp block's leading edge moves linearly at 1120 recording px/s (611 dp/s), against 756 px/s nominal.
- WP04's per-placeholder `rememberInfiniteTransition` (1400 ms tween) also measured about 1.5–1.7x fast in
  `rec/evr-cold-light.mp4` (`analysis/sweep-wp04-everything-light.tsv`).

**Why the recording is trustworthy.** In the same catalog video, `AppActivityIndicator` (`delay(125)` per step) steps
every **0.125 s**, so video time is real time.

**What to check.** In a dev build on the Redmi, log `withFrameMillis` deltas against `SystemClock.uptimeMillis` before
changing the constant. It may be a frame-clock effect of this phone rather than code.

**Evidence.** `analysis/sweep-catalog-*.tsv` (period_s column) and the frame-by-frame trace described above.

## D5: The skeleton does not match the content; cards jump on arrival

**What is wrong:**
- The skeleton card is 2 thin lines (14 and 12 dp), 86 dp tall.
- The real card is a headline title of 1–2 lines plus a 2-line body: 77–110 dp at 1.0x and 98–144 dp at 1.5x.
- The skeleton's line heights are fixed dp, so they ignore font scale.
- The lines are inset 16 dp more than the text, at 48 dp against 32 dp. `AppSkeletonListItem` adds its own `lg`/`md`
  padding inside `AppCard`'s `lg` padding.
- The swap is a one-frame cut in which every card below the first moves and resizes.

**Where.** WP03 ptr app, Samples, Loading → Success, light (dark and 1.5x are the same). The WP04 build also has it.

**Evidence:**
- `frames/ptr-cold-light-skeleton-to-content.png`: frames 162 and 163, 8 ms apart, with red and blue guide lines at
  the skeleton card edges.
- `frames/ptr-cold-font150-skeleton-to-content.png`.
- WP04: `frames/evr-cold-light-skeleton-to-content.png`.

**Measured** (recording px, x = 672):

| Build | Skeleton cards | Content cards | Shift |
|---|---|---|---|
| WP03, 1.0x | 408–565, 596–753, 783–940, 971–1127 | 397–576, 630–791, 823–1022, 1058–1200 | +19, +22, +47 dp |
| WP03, 1.5x | Same 157 px cards | 413–676, 711–921, 952–1216 | — |
| WP04 | 61 dp cards: 360–468, 504–616, 647–759 | 360–563, 594–755, 786–988 | 2nd card +49 dp |

**Cause.**
- `LoadingList()` renders `AppCard { AppSkeletonListItem(showLeading = false) }` instead of a placeholder built from
  `SampleRow`'s layout.
- `AppSkeletonListItem` hard-codes its padding (`AppSkeleton.kt`).
- The same applies in Feed and Search.

## D6: The opaque bottom strip cuts the content (REQ 17); old shell against WP04

**Old shell (WP03 ptr app):**
- What is wrong:
  - The tab bar is an opaque full-width strip with a hairline (`BarTreatment.Docked`, the Utility default).
  - The root entry is padded by the bar's height (`AppNavHost.rememberEntryProvider`: `Box(Modifier.background(...).padding(bottom = inset))`),
    so content stops at the strip.
  - At rest the 6th card's top edge peeks out above the strip.
  - During a pull the content moves down and the strip slices the last card's title and body.
  - At 1.5x the 4th card's body is sliced.
- Evidence:
  - Pull in light: `frames/r8-1335-light_pull-bottom-cut.png` ("nesciunt quas odio" sliced). Taken from the run the
    user watched.
  - Pull in dark: `frames/r8-1335-dark_pull-bottom-cut.png`.
  - Sheet of the pull: `frames/r8-1335-light_slow-sheet.png`.
  - Rest: `wp03-r8-1335/r8-after-light-rest.png` and `wp03-r8-1330/light-rest.png`.
  - Dark scrolled: `shots/ptr-dark-scrolled-bottom-cut.png`.
  - 1.5x: `shots/ptr-font150-light-loaded.png`.
- Reproduce: open the ptr app and pull down from 40% of the screen height by 800 px over 1.4 s
  (`input swipe 540 960 540 1760 1400`), or scroll.

**WP04 final-everything (new shell):**
- What it fixes: the mechanics. Content now runs under the bar, and only the list is padded. At the end of the list
  the last card ends a gutter above the bar (`shots/evr-light-scrolled-end.png`), and glyph tops are drawn right up to
  the bar edge (`shots/evr-light-loaded.png`).
- What it does not fix: the look. For the Utility style the bar is still an opaque, full-width surface block with a
  hairline, so content is cut off visually exactly as before. There is no edge fade.
- Light and 1.5x evidence: `shots/evr-light-loaded.png` and `shots/evr-font150-light-scrolled.png`.
- Dark evidence: `shots/evr-dark-feed-loaded.png`.

**WP04 HEAD 230f894 (code).**
- `AppTabBar.BarSurface` still draws every non-floating treatment (Docked, Minimal, Chunky) as
  `.background(look.surface)` across the full width, and `AppStyle()` (Utility) defaults to `BarTreatment.Docked`.
- Only the Social style (`Floating`) floats.
- So REQ 17 holds only for Social. Utility (the default, and both test apps), Editorial and Playful keep the opaque
  block.

## D7: WP04 build's large title never collapses; the list is sliced under it

**What is wrong.** In final-everything the tab headers are static. Samples has `AppLargeTitle` plus `AppSearchField`
in `topBar`, and Feed, Search, Profile and Settings have `AppLargeTitle`. Scrolling leaves the roughly 200 dp header in
place, and cards are cut at a hard edge under the search field. The DEV badge also overlaps the title (REQ 30, known).

**Evidence:**
- `shots/evr-light-scrolled-small.png`: the "qui est esse" card is cut under the field.
- `shots/evr-light-scrolled-end.png`.
- `shots/evr-font150-light-scrolled.png`.

**Cause and merge risk.** WP04 HEAD still uses `AppLargeTitle` in all five screens, while WP03 moved them to the
collapsing `AppLargeTitleTopBar`. The merge must keep WP03's header together with WP04's `chromeContentPadding` (see
D1).

## D8: The search crossfade double-exposes the empty state over the skeleton

**Evidence.** `frames/evr-search-crossfade-double-exposure.png`, from `rec/evr-search-typing-light.mp4` (WP04 build,
typing "qui"). "Find anything" and its icon are drawn on top of the skeleton rows.

**Cause.** WP03's `SearchScreen` uses the same `AnimatedContent(fadeIn togetherWith fadeOut)` over full-size content.

## D9: Dark cold start passes through a light frame and a grey wash (WP04 build)

**What is wrong:**
- The app's theme is set to dark through `perf.theme`, while the phone is light.
- The cold start shows a white splash, then the skeleton screen in the light theme.
- It then fades through grey to dark. Background luma at (600, 700) goes 247 → 23 between 1.81 s and 2.03 s.
- The tab bar is absent for most of the wash.

**Evidence.** `frames/evr-cold-dark-theme-flash.png` and `rec/evr-cold-dark-feed.mp4`.

**Unrelated to skeletons.** In the same recording the Feed tab's skeleton is replaced by "Could not reach the server"
after about 0.3 s.

## D10–D12: Smaller findings

- **D10, catalog dark.** `--es theme dark` on a light phone leaves the status-bar icons dark on the dark bar:
  `frames/dark-statusbar-legibility.png`, bottom half. The WP04 app, top half, is correct once settled.
- **D11, landscape list pane.** The search field touches the inline bar's hairline with no top spacing
  (`shots/ptr-landscape-light-loaded.png`). Titles clamp at 2 lines ("reprehend…") in the narrow pane
  (`frames/r8-1330-dark-landscape-f001.png`).
- **D12, font scale 1.5:**
  - The large title "Samples" is the same size at 1.0x and 1.5x.
  - The search glyph stays small while the placeholder text grows (`shots/ptr-font150-light-loaded.png`).
  - In the WP04 bar, "Settings" ends about 9 dp from the screen edge and the selected pill crowds "Home"
    (`shots/evr-font150-light-scrolled.png`).

## Checked and fine

- **Reduce motion.** The skeletons hold still: `shots/cat-skel-reduce-motion-a.png`.
- **Edges and corners.** The band is clipped to each placeholder's outline, with no bleed past the rounded corners or
  circles.
- **Draw-phase reads.** The sweep reads `progress` only in `onDrawBehind`. I did not measure recomposition counts.
- **Sides.** Nothing is cut at the sides in portrait.

## Files

| Folder | Contents |
|---|---|
| `rec/` | My recordings: `ptr-cold-{light,light-2,dark,font150-light,landscape-light}`, `cat-skel-{light,dark}`, `evr-cold-light`, `evr-cold-dark-feed`, `evr-cold-font150-light`, `evr-search-typing-light` |
| `frames/` | Cited crops and composites, plus the kept source frames per recording, each with its `pts*.txt` |
| `shots/` | Full-resolution screencaps |
| `analysis/` | Per-placeholder sweep measurements |
| `device/` | Settings before and after |
| `wp03-r8-1330/`, `wp03-r8-1335/` | WP03's own session recordings |
