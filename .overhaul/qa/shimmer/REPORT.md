# Shimmer and clipped content: root causes, design, fixes and acceptance (REQUIREMENTS 34)

Written 2026-10-13 as the design report of workflow run `wf_b8570cc1-22c`; the harness refused that write, so the
text lived only in the run's journal while the fix was built from it. This is the same report, written to disk on
2026-10-15 and brought up to date with what was built, reviewed and measured since. It is the single source of
truth for the fix. `evidence/EVIDENCE.md` (device frames, Oct 12) and `AUDIT.md` (code reading, Oct 12) are its
inputs; where it disagrees with them it says why. `after/` holds the Oct 15 phone session on the fixed builds.
`VERIFY.md` (the Verify step of the workflow) gives the verdict per defect against §5.

**The user's report** (Oct 12, 13:35, watching `com.example.wp03.ptr.dev` on the Redmi): "shimmer has issues and
content gets cut".

**How to use it:**
- §0 says what is done, where it is, and what is still open, so nobody rebuilds finished work.
- §1 is what was wrong, each defect confirmed or refuted.
- §2 is the shimmer as designed and built on `wp/WP03-skel`, precise enough to port (WP07-B) and to review against.
- §3 is every clipping case with its owner, the exact fix and its state.
- §4 is the Compose Multiplatform and SwiftUI parity.
- §5 is the acceptance: tests, recomposition, screenshots, the phone check, and what the reviewer holds the change to.
- §6 lists what is open and who takes it.

Item ids are stable: the handoff notes of WP03, WP04, WP02-M3, WP06, WP07-A and WP08 cite them (R1–R9 shimmer,
K1–K14 clipping, D9–D12 routed, M-1–M-3 merge). Paths are relative to `templates/android/` unless they start with
another root; code lines are on `wp/WP03` at `b595f9d` unless another branch is named.

**Heads read on Oct 15:**

| Branch | Head | Note |
|---|---|---|
| `wp/WP03-skel` | `46ec781` | The fix, two review rounds in; `wp/WP03` merged into it (`handoff/WP03-skel.md`) |
| `wp/WP03` | `b595f9d` (79 ahead of the integration branch) | Contains `wp/WP03-skel` whole; the dirty files are the uncommitted sheet work plus the K3, D10, D11 and D12 edits of run 12 |
| `wp/WP04` | `3ff25f8` | K3, K6, K11, the K13 seam, D9 and D12 coded in run 17, uncommitted while its build queue drains |
| `wp/WP08` | `d05a682` | SwiftUI parity built (`2bc4c4a`, `7400ba3`, `37558cc`), waiting for WP03's tokens |
| `wp/WP07` | `99239af` | Multiplatform parity not started (phase B) |
| `overhaul/multiplatform-v2` | `a402d87` | WP01, WP02-M1, WP09, WP10 merged; none of the above |

**Phone.** Absent from 13:22 to 13:48 on Oct 13 (polled), so the first draft re-measured D4 from the Oct 12
recordings (§1.4). Present on Oct 15: the `after/` session ran 16:07–16:13 on release builds of `wt/WP03-skel`
(`out/WP03-skel/{cp1/catalog,ptr}`), restored every setting it changed, and `adb devices` lists `9a7c74f4` at the time
of this write.

---

## 0. State on Oct 15, and what the relaunched workflow does with it

| Area | State | Where |
|---|---|---|
| Shimmer R1–R9 (§1.2) | **Fixed** on `wp/WP03-skel`, reviewed twice (9 blocking findings fixed), **verified on the phone** Oct 15 (§5.4) | `foundation/draw/{Skeleton,Bones,Sweep,ShimmerClock}.kt`, `component/feedback/{AppSkeleton,AppRedacted}.kt`, tokens |
| D4 "1.5× fast" | **Refuted** (§1.4) | `report/d4-period-recheck.{py,txt}` |
| K1, K2, K7, K9 (§3) | **Fixed** on `wp/WP03-skel`, verified Oct 15 (cold starts, the catalog fixture) | feature screens, `AppStateView`, `AppPagingList`, `AppTopBar`, `scaffold.py` |
| K10 | Coded (`AppScaffold` pads `safeDrawing` horizontally); **device check pending** (3-button navigation, landscape) | `component/container/AppScaffold.kt:71` |
| K3 | WP04 built the docked bar's scroll-edge look and the edge fade (run 17, uncommitted); WP03's half (Utility defaults to `Floating`) is in `wt/WP03`'s dirty tree, uncommitted | WP04 `AppTabBar`, `BottomEdgeFade`, `ScreenChrome`; WP03 `tokens/styles.json` |
| K6, K11, D9, D12 (bar) | WP04 built them in run 17 (uncommitted; `ChromePaddingTest`, `ChromeLayeringTest`, `TabBarTest`, `WindowManifestTest`) | WP04 `foundation/layout/Chrome.kt`, `shell/` |
| K13 | WP04 publishes `LocalWindowTopChrome`; WP06's consumer is prepared and waits for the WP04 merge | WP06 `badge/DevToolsOverlay.kt` |
| K5, K8 | **Open**: WP02-M3 has not started | `feature/search/.../SearchScreen.kt:105`, `:145` |
| K4, K14, M-1–M-3 | Bind whoever merges WP03 after WP04 (§2.13) | — |
| K12 | The sheet spec's (`research/SHEET-SPEC.md`); WP03's uncommitted sheet rework | — |
| D10, D11, D12 (title, glyph) | In `wt/WP03`'s dirty tree (run 12), uncommitted | WP03 `text/TextScale.kt`, catalog |
| WP08 parity (§4.2) | **Built**; its tokens re-emit once WP03 merges | `templates/ios/.../Components/Skeleton/` |
| WP07-B parity (§4.1) | **Not started** (Android ships first, REQUIREMENTS 36) | — |

**For the Fix step of `scripts/shimmer-fix-from-report.js`** (which implements "§2, the WP03 items of §3 and the
tests of §5" from this file): all of that exists on `wp/WP03-skel` and in `wp/WP03`. Do not rebuild it. What is left
for that branch is in §6 under "WP03-skel / WP03"; everything else in §6 is another package's. **For the Verify
step:** `after/` already holds the catalog and cold-start measurements of §5.4 steps 1–6 on `46ec781`; `VERIFY.md`
is written from them plus the steps §5.4 lists as remaining.

---

## 1. Root causes

### 1.1 What the user most likely saw

The run they watched (13:33–13:36, `ptr_r8_session.sh`) cold-started the app 8 times, each start showing the
Samples skeleton for about 1 s, and pulled to refresh several times. Five defects were on screen in that run, each
many times:

| What the user said | Defect | Seen in the watched run |
|---|---|---|
| "shimmer has issues" | **R1**: twelve bands out of step, one per bone | Every cold start (`evidence/frames/ptr-cold-light-sweep-strip.png`) |
| "shimmer has issues" | **R2**: in light, the band wipes the bone into the card | Every cold start |
| "content gets cut" | **K1**: the skeleton is laid out under the status bar and the large title; card 1 is cut by the title band, card 2 is hidden | Every cold start (`evidence/frames/ptr-cold-light-skeleton-full.png`) |
| "content gets cut" | **K3**: the opaque strip behind the tab bar slices the last card | Every pull, and at rest (`evidence/frames/r8-1335-light_pull-bottom-cut.png`, `perf/WP03/ptr/r8-after/light-rest.png`) |
| "content gets cut" (likely) | **K2**: cards jump and resize when the data replaces the skeleton | Every cold start (`evidence/frames/ptr-cold-light-skeleton-to-content.png`) |

### 1.2 Shimmer defects

| ID | Defect | Root cause | Finding | Evidence | State |
|---|---|---|---|---|---|
| R1 (D2, S-1) | Each bone sweeps its own band: 11 to 94 dp wide, 62 to 525 dp/s, peaks up to 166 dp apart at one instant | Old `AppSkeleton.kt:102` `band = size.width * BandFraction` and `:114` `x = -band + (size.width + band) * clock.progress`, both in the bone's own coordinates; `ShimmerClock` shared only a 0..1 phase, so bones of different widths could never line up. The KDoc and the catalog caption ("One shared sweep") described a band the code did not draw. | **Confirmed** on device and in code | `evidence/frames/cat-skel-dark-f040.png` (the staircase), `evidence/analysis/sweep-catalog-*.tsv` | Fixed (§2.4); phone: one band, §5.4 step 3 |
| R2 (D3, S-2) | In light the highlight lifts the bone almost to the card (75 % of its contrast removed, so lines look chopped); in dark it is a bright bar (+13.7 ΔL\* against +4.3) | `tokens/palette.json` mapped `skeletonHighlight` to `@background` (light) and `ink.500` (dark): an existing role, not a step over the bone | **Confirmed** (luma and CIELAB) | EVIDENCE D3, AUDIT S-2 | Fixed (§2.6); phone: +5.6 L\* light, +5.4 dark |
| R3 (S-3) | Dark bones invisible on raised cards (1.4 ΔL\* in Social), faint on plain ones (4.9) | Dark `skeleton` was `ink.700`, the dark `surfaceElevated`; `PaletteContrastTest` had no skeleton case | **Confirmed** by computing ΔL\* | `palette.json`, `AppCard.kt` | Fixed (§2.6, three test cases) |
| R4 (S-5) | The sweep never pauses: a loading screen draws every frame at 120 Hz | A `withFrameMillis` loop wrote `progress` on every frame while any bone was attached | Confirmed in code | — | Fixed: 1.2 s sweep, 0.8 s with no frames; phone: 728 frames in 10 s against 1,210 before |
| R5 (S-4, S-12) | Per-frame cost: two draws and a `clipPath` per bone with no layer of its own (WP03); a `Brush`, a list and an `InfiniteTransition` per bone per frame (WP04, integration branch) | Old `AppSkeleton.kt:111-118`; WP04's `:43-69` | Confirmed in code | — | Fixed (§2.8); WP04's copy is replaced at merge (M-3) |
| R6 (S-6) | Visibility: nothing on WP03 provides `LocalTabVisible`, so hidden tabs keep the clock running; WP04's skeleton ignores visibility and reduce motion | Boolean local read in composition; WP04 made it a `TabVisibility` holder | Confirmed in code | — | Fixed on WP03's local (observed in the node); the port to WP04's holder is M-1 (§2.13) |
| R7 (S-7, S-9) | Without a provider the bones are silently static; after a re-attach the first frame shows a stale band | `LocalShimmer` defaulted to null; `progress` kept its last value while stopped | Confirmed in code | — | Fixed: `NaN` between sweeps, the sweep starts at the first frame after a bone joins |
| R8 (S-8) | The sweep runs left to right in right-to-left layouts | Old `:104-114` | Confirmed in code | — | Fixed: mirrored (phone: `after/rec/cat-loading-light-rtl.mp4`) |
| R9 (S-10) | Android never announces "Loading" | Every bone cleared its semantics; no call site set the label | Confirmed in code | — | Fixed: one `contentDescription` per loading region (`AppRedacted`, the loading lists) |
| ~~D4~~ | "The sweep runs about 1.5× faster than declared" | — | **Refuted**, §1.4 | `report/d4-period-recheck.{py,txt}` | — |

### 1.3 Content-clipping defects

| ID | Defect | Root cause | Finding | Evidence | Owner (fix in §3) |
|---|---|---|---|---|---|
| K1 (D1, C-1, C-2) | The loading list ignored the top chrome: it started 16 dp below the window top, under the status bar, the large title and the search field | `SampleListScreen.kt` `LoadingList` padded with `PaddingValues(gutter)` only; the large-title band hid what ran under it rather than placing it below, and left the inline-bar area clear. The same in `FeedScreen`, `SearchScreen` (`LoadingResults`, `Results`), `SearchResult`, `SampleDetailScreen` | **Confirmed** in light, dark, 1.5× and landscape | `evidence/frames/ptr-cold-*-skeleton-*.png` | WP03: fixed, verified |
| K2 (D5, K-1) | The skeleton did not match the content: 86 dp with two 14/12 dp bars inset 48 dp, against cards of 77–110 dp (98–144 at 1.5×) with text inset 32 dp; cards shifted +19, +22, +47 dp on arrival | Hand-made dp approximations: `AppSkeletonListItem` padded inside `AppCard`'s padding, `AppSkeletonLine` was sized in dp and ignored the font scale | **Confirmed** | `evidence/frames/ptr-cold-light-skeleton-to-content.png`, `-font150-` | WP03: fixed, verified (§5.4 step 5) |
| K3 (D6, C-5) | An opaque full-width strip behind the bar sliced the last card, at rest and during every pull | Old shell: `AppNavHost` padded the root entry above a background-coloured strip. WP04 fixed the padding, but its `Docked` treatment was still a full-width opaque block with a hard top edge, and Utility (the default, both test apps) is `Docked` | **Confirmed** (old shell; WP04 Utility) | `evidence/frames/r8-1335-*-pull-bottom-cut.png`, `shots/evr-light-loaded.png` | WP04 (look) + WP03 (default): both built, uncommitted |
| K4 (D7) | WP04's build: the large title never collapses, and the list is sliced under the header | WP04 still used the static `AppLargeTitle`; WP03 moved to `AppLargeTitleTopBar` | Confirmed (WP04 build) | `shots/evr-light-scrolled-small.png` | Merge of WP03 (M-2) |
| K5 (D8, K-3) | Search cross-fades the empty state over the skeleton rows: a double exposure, alpha on full-screen layers | `SearchScreen.kt:105` `AnimatedContent(fadeIn togetherWith fadeOut)` | Confirmed (WP04 build; the same code is on WP03) | `evidence/frames/evr-search-crossfade-double-exposure.png` | WP02-M3: open |
| K6 (C-3) | Where no chrome is written (rail layout, no-bar screens, covers, the single-stack host, footerless sheets) the last row ends under the navigation bar or gesture handle | `chromeContentPadding()` added only the chrome holder, never the navigation-bar inset | Confirmed (landscape rail shot) | `shots/ptr-landscape-light-scrolled-more.png` | WP04: built |
| K7 (C-1) | `AppStateView` centred in the whole window, so at 1.5× or in landscape its top or action could sit under the chrome; `AppPagingList`'s default padding ignored chrome and its states dropped `modifier` | `AppStateView`, `core/ui/.../AppPagingList.kt` | Code only | — | WP03: fixed (`StateViewTest`, `after/shots/ptr-offline-font150.png`) |
| K8 (C-1) | Search's start content has no chrome padding and does not scroll: the recent chips are cut at 1.5× or in landscape | `SearchScreen.kt:145` `StartContent` | Code only; device unconfirmed | — | WP02-M3: open |
| K9 (C-2) | The large title snapped closed when item 1 became the first visible item; the loading list scrolled on its own state, not the title's | `scrolledFromTop()` returned `MAX` at `firstVisibleItemIndex > 0` | Code only | — | WP03: fixed (`LargeTitleTopBarTest` "a first row shorter than the title is followed under the bar, not skipped") |
| K10 (C-4) | With 3-button navigation in landscape, row ends sit under the navigation bar | `AppScaffold` cleared only the display cutout at the sides | **Unconfirmed**: needs the device in 3-button mode | — | WP03: coded; device check pending |
| K11 (C-6) | At the end of a list the bottom edge fade veiled the last card's lower 8 dp, and faded to `background` on grouped screens | WP04 `shell/BottomEdgeFade.kt` | Code only | — | WP04: built |
| K12 (C-7) | At a sheet's medium detent the bottom of its content is off screen | Old `AppSheet.kt:238`, `:211-214` | Code only | `research/sheet/current-state.md` D4 | Sheet spec (WP03 component) |
| K13 (C-8) | The DEV badge's top bound ignores the large title, so it can cover the title (REQ 30) | WP06 `DevToolsOverlay.kt` clamps at status bar + standard bar height | Confirmed (WP04 build) | `shots/evr-light-loaded.png` | WP06: prepared, after the WP04 merge |
| K14 (C-9) | On WP04 the keyboard is counted twice (`imePadding()` plus the keyboard as bottom chrome) | WP04 `AppScaffold.kt:50` | **Unconfirmed** | — | Merge of WP03 (its scaffold wins) |

### 1.4 D4 is refuted: the sweep ran at the declared 1.4 s

EVIDENCE reported a 0.933 s cycle against the declared 1,400 ms and left the cause open. Both catalog recordings
were re-measured on their per-frame timestamps (`ffprobe pts_time`, 476 frames over 3.96 s); script and output are
`report/d4-period-recheck.py` and `report/d4-period-recheck.txt`.

| Recording | Block peak x (recording px) at t | Cycle |
|---|---|---|
| `evidence/rec/cat-skel-light.mp4` | 472 at 0.000 s, 472 at 1.401 s, 483 at 2.801 s | **1.40 s** |
| `evidence/rec/cat-skel-dark.mp4` | 424 at 0.093 s, 430 at 1.494 s, 426 at 2.893 s | **1.40 s** |

- The `delay(125)` spinner in the same light video steps every 0.125 s (median of 25 intervals), so video time is
  real time.
- The peak moved at about 736 recording px/s against 755 nominal.
- The old code was right about its period; EVIDENCE's `period_s` column was an analysis error. No frame-clock
  anomaly exists on the Redmi, and motion timings measured there stand.
- The new clock's period is pinned by `ShimmerClockTest` and measured on the phone at 2.024–2.026 s (§5.4 step 3).

### 1.5 Other defects found on the way (routed; not part of this fix)

| ID | Defect | Owner | State |
|---|---|---|---|
| D9 | With the app theme set to dark, the cold start showed a white splash, a light first frame and a grey wash; the tab bar was missing during the wash | WP04 | Built (run 17): `MainActivity` handles `uiMode` in place; the one light splash on the launch that first stores the theme is the system's, before any app code runs |
| D10 | Catalog with `--es theme dark` on a light phone: status-bar icons stay dark | WP03 (catalog) | In `wt/WP03`'s dirty tree (run 12) |
| D11 | Landscape: the search accessory touches the inline bar's hairline | WP03 | In the dirty tree; `LargeTitleTopBarTest` "in a window held sideways the accessory keeps its gap below the inline bar's hairline" |
| D12 | At 1.5×, the large title does not scale and the search glyph stays small | WP03 | In the dirty tree (`text/TextScale.kt`); `LargeTitleTopBarTest` "at a larger font scale the large title grows with the text, up to its cap" |
| D12 | At 1.5×, "Settings" ends 9 dp from the screen edge and the pill crowds "Home" | WP04 | Built (run 17): 8 dp docked inset, labels shrink by `TextAutoSize` to 0.85× |

### 1.6 Found on the fixed build: five slow frames in 10 s, part of them the wake from the pause

On the Oct 15 build, both themes count exactly 5 janky frames in 10 s on the catalog's Loading fixture
(`after/gfx/cat-loading-{light,dark}-10s.txt`: "Number Slow issue draw commands: 5", "Frame deadline missed: 5"),
and there are 5 sweep starts in 10 s (period 2.02 s). The framestats block keeps only the last 120 frames (1.8 s,
one pause included). In it, the first two frames after the 817 ms gap are the slowest or next-slowest: 18.1 and
18.6 ms in light, 17.9 and 18.2 ms in dark. The render thread and the GPU wake from 0.8 s of idle with their clocks
down, and the first frames are late; the band at those frames is still off screen (progress under 2 % of a 908 dp
travel, under 2 dp in), so nothing visible stutters. One more slow frame per window lies inside a sweep (16.6 ms in
light, 19.4 ms in dark), which the window cannot attribute. What a viewer could see is the interval between presented
frames inside a sweep: mean 8.35 ms and at most 15.8 ms (light) and 11.3 ms (dark), under two vsyncs at 120 Hz
(`after/analysis/sweep-timing.json`), so the band never visibly hitches. §5.4 step 1 says how this is judged and what
`VERIFY.md` still has to attribute.

---

## 2. The shimmer design, as built on `wp/WP03-skel`

### 2.1 What it must do, and how

| Requirement | Mechanism |
|---|---|
| One band crosses every placeholder in one coordinate space (REQ 34) | Each bone node maps its origin into the sweep area (the window root, or the nearest `Modifier.shimmerBounds()`) and draws the shared band at that offset (§2.4) |
| Tokens for band width, angle, period, easing, pause and highlight per theme (REQ 34) | `tokens/motion.json` `skeleton` and the `skeleton`/`skeletonHighlight` roles of `tokens/palette.json`, emitted to `DesignTokens.Motion` and the palettes (§2.6, §2.7) |
| Zero recomposition per frame (REQ 21, 22) | The clock writes a plain field and calls `invalidateDraw()` on the bones that joined it; nothing is snapshot state, nothing is read in composition (§2.8) |
| No per-frame allocation | One cached `SweepBrush` per colours, band width and direction; one frame callback for all frames; the draw translates the canvas and draws round rects (§2.8) |
| Idle costs nothing | A pause with no frames; no bone joined means no clock; hidden, covered, reduced-motion and preview bones never join (§2.9) |
| Skeletons shaped like the content (REQ 34) | `AppRedacted { RealRow(placeholder) }`: the real layout, with text drawn as one bone per laid-out line (§2.10) |
| No jump, no clip, no alpha over full-screen layers | The loading list is laid out as the list that replaces it (same padding, same rows) and swapped in one frame (§2.11) |
| Adaptive (REQ 23), right to left | The band keeps its phone speed on large windows (the sweep lengthens, up to 2×); the direction follows the layout direction |

### 2.2 The model

- **A bone** is anything drawn as a placeholder: a free-form `AppSkeleton`, a line of redacted text, a redacted
  icon, avatar, image, chip, badge, pill or filled button. Every bone is drawn by one node class, `SkeletonNode`,
  with one of three `Bones` geometries (§2.8).
- **The band** is one tilted, soft-edged stripe: a gradient from bone colour to highlight and back, clamped to the
  bone colour outside the band. Every bone paints its own outline with that gradient, offset by its own position, so
  a bone outside the band draws its plain colour at no extra cost. There is no mask, no `saveLayer`, no `clipPath`.
- **The sweep area** is the coordinate space the band crosses: the window root by default. `Modifier.shimmerBounds()`
  makes the nearest such ancestor the area instead, for a sheet or a pane whose placeholders should sweep on their
  own.
- **The clock** is one `ShimmerClock` per window, created by the outermost `AppTheme`. It publishes the eased progress
  of the sweep in flight, or `NaN` between sweeps and while no bone has joined.

### 2.3 Public API (`:core:designsystem`)

Public names stayed. `AppRedacted`, `LocalRedacted`, `Modifier.skeleton` and `Modifier.shimmerBounds` are new;
`AppSkeletonLine(style)` replaces the fixed height; two declarations are deprecated at `WARNING` (API-14).

```kotlin
// component/feedback/AppSkeleton.kt
@Composable fun AppSkeleton(modifier: Modifier = Modifier, shape: Shape = AppSkeletonDefaults.shape)
@Composable fun AppSkeletonLine(modifier: Modifier = Modifier, widthFraction: Float = 1f, style: TextStyle = AppTheme.typography.body)
@Deprecated("A fixed height ignores the font scale. Pass the text style the line stands for.", ReplaceWith("AppSkeletonLine(modifier, widthFraction)"))
@Composable fun AppSkeletonLine(modifier: Modifier = Modifier, widthFraction: Float = 1f, height: Dp)
@Composable fun AppSkeletonCircle(modifier: Modifier = Modifier, size: Dp = AppSkeletonDefaults.CircleSize)
@Deprecated("Its shape matches no real row, so content jumps when it arrives. Redact the real row instead.", ReplaceWith("AppRedacted(active = true) { /* the row, given placeholder data */ }"))
@Composable fun AppSkeletonListItem(modifier: Modifier = Modifier, showLeading: Boolean = true)
object AppSkeletonDefaults { val shape: Shape /* AppTheme.shapes.sm */; val CircleSize: Dp = 40.dp }

// component/feedback/AppRedacted.kt
@Composable fun AppRedacted(active: Boolean, modifier: Modifier = Modifier, contentDescription: String? = null, content: @Composable () -> Unit)

// foundation/draw/Skeleton.kt
fun Modifier.skeleton(shape: Shape): Modifier
fun Modifier.shimmerBounds(): Modifier
val LocalRedacted: CompositionLocal<Boolean>      // read-only view of the internal provider
```

- `AppSkeletonLine(style)` measures one line of `style` once (`rememberTextMeasurer(cacheSize = 0)`, keyed on the
  style) and draws the bone a redacted `AppText` would draw for that line, so a free-form line and a redacted line
  are the same shape at every font scale.
- `AppSkeletonListItem` keeps its layout while deprecated (fixed heights included) and now draws through the node, so
  even old call sites sweep coherently. Nothing in the template sets warnings as errors, so the deprecation breaks no
  build. The catalog keeps one under "Skeleton list item: kept for screens written against it".
- `AppRedacted` always emits the same `Box` (`propagateMinConstraints = true`), so toggling `active` keeps the
  content's composition and state. While active it carries `clearAndSetSemantics { contentDescription }`: pass the
  label once per loading region, not per row. The KDoc states, once, the rules every loading list follows (§2.11) and
  which controls a redaction stops (§2.10).
- `Modifier.skeleton` and `Modifier.shimmerBounds` follow M-1 of CODE-STANDARDS: a public factory, a private
  `ModifierNodeElement` whose `update()` mutates the node, a private node. Nothing is `composed`. The internal
  `Modifier.lineSkeleton(top, bottom)` and `Modifier.textSkeleton(bones)` are the same shape for text.
- `LocalShimmer` became the internal `LocalShimmerClock` (API-1).

### 2.4 Band geometry (`foundation/draw/Sweep.kt`)

Notation: θ is the tilt and W the band width in px; c = cos θ, s = sin θ; (Aw, Ah) is the sweep area's size in px;
(ox, oy) is the bone's origin in the area; `progress` is in 0..1, already eased by the clock.

- **Phase.** A point (X, Y) of the area is at phase u = X·c + Y·s. The band occupies u ∈ [b, b + W].
- **Travel.** D = Aw·c + Ah·s + W, and b = −W + progress·D: the band starts wholly before the area's start edge and
  ends wholly past its far corner (`SweepGeometry.start`).
- **Gradient.** One `LinearGradientShader` per look, in bone-local space, from (0, 0) to (W·c, W·s), stops
  0 → `skeleton`, 0.5 → `skeletonHighlight`, 1 → `skeleton`, `TileMode.Clamp` (`SweepBrush`, a `ShaderBrush` whose
  `createShader` returns the cached shader).
- **Per frame, per bone,** left to right: shift = b / c − ox − oy·tan θ; `translate(left = shift) { bones.draw(brush,
  dx = −shift) }` (`SweepGeometry.shift`, `drawSwept`). The shader then samples the bone at phase u − b, so every bone
  in the area draws the same band in the same frame.
- **Right to left.** The gradient runs to (−W·c, W·s) and shift = (Aw − ox) + oy·tan θ − b / c: the band enters at
  the right edge, leaning the other way (`SweepGeometry.gradientEnd`).
- **Origin.** `self.positionInRoot()` for the window area; `bounds.localPositionOf(self, Offset.Zero)` inside a
  `shimmerBounds` area. Both include layer transforms, so during predictive back a scaled screen's bones follow the
  window band, narrower in proportion. Accepted.
- **Scrolling during a sweep.** Each sweep frame invalidates every joined bone and the draw reads the current origin,
  so a bone that moves is drawn at its new place in the same frame. During the pause no band is drawn, so no stale
  origin can show.
- **Coherence** is what `SweepGeometryTest` (the maths) and `SkeletonCoherenceTest` (pixels) check (§5.1).

### 2.5 Timing (`foundation/draw/ShimmerClock.kt`)

- **Sweep.** `skeletonSweepMs` 1,200 on a phone, eased `CubicBezierEasing(0.3, 0, 0.7, 1)`, then `skeletonPauseMs`
  800 with no frames (`delay()`, no `withFrameNanos`). Period 2.0 s on a phone; measured 2.024–2.026 s (§5.4).
- **Phone speed.** On the Redmi (393 × 873 dp) D ≈ 908 dp, so the band crosses a row in about 0.5 s (about 750 dp/s
  on average), in line with Facebook Shimmer and Wear Compose Material 3 (AUDIT §6.1).
- **Large windows.** `sweepMillis(size) = skeletonSweepMs × clamp(D / skeletonReferenceTravel, 1, skeletonMaxSlowdown)`,
  with `referenceTravel` 900 dp and `maxSlowdown` 2: a 1280 × 800 dp window sweeps in about 2.3 s so the band keeps
  its phone speed. Computed once per sweep from the theme's `WindowMetrics.sizeDp` through a lambda, never in
  composition.
- **Areas.** A `shimmerBounds` area uses the same progress over its own D, so it never runs faster than the window.
- **Start.** A sweep starts at the first frame after the first bone joins (`sweepStart = withFrameNanos { it }`);
  until then progress is `NaN` and the bones draw plain (R7). Because the band enters from off screen, a load shorter
  than about 0.4 s shows still bones only, and the band never flashes mid-screen.
- **Motion style.** The motion style's `speed` does not scale the shimmer: it is ambient, not a response to the user.

### 2.6 Colours (per theme, as tokens)

**The rule** (CIELAB L\*; `genkit/tokens/__init__.py` `BONE_STEP`, `SKELETON_LIFT`, `HIGHLIGHT_STEP`;
`PaletteContrastTest` `BoneStep`, `MinLift`..`MaxLift`, `HighlightStep`):
- **Bone.** At least 7 ΔL\* from every surface it can sit on (`background`, `backgroundGrouped`, `surface`,
  `surfaceGrouped`, `surfaceElevated`).
- **Highlight lift.** The highlight is the bone lifted by 5 L\* (accepted 4.5 to 6), in light and in dark, so the
  band reads the same in both themes.
- **Light only.** The highlight stays at least 3 ΔL\* from every surface, so the band never wipes the bone into its
  card (R2).

**Values in `tokens/palette.json` and `DesignTokens.kt` (the two default styles; Paper and Cream follow the same
rule):**

| Tone | Theme | `skeleton` | `skeletonHighlight` | Lift |
|---|---|---|---|---|
| Cool (Utility) | light | `#D7DADF` | `#E5E8ED` | +4.9 |
| Cool | dark | `#2D333E` | `#393F4B` | +5.4 |
| BrandTint (Social), template accent | light | `#C9D2E1` | `#D7E0EF` | +5.0 |
| BrandTint | dark | `#313A4B` | `#3C4557` | +5.0 |

**BrandTint and generated accents.** A tone mixes the accent into `skeleton` only (`tones.BrandTint.mix.*.skeleton`
0.08); mixing `skeletonHighlight` is a token error. The resolver (`_skeleton` in `genkit/tokens/__init__.py`) lifts
the band 5 L\* from the tinted bone in the bone's Oklab hue, then moves the bone off every surface until the three
rules hold. Reason: tinting both roles by the same amount narrowed the lift to 4.28–4.39 L\* for 33 of the 200 golden
accents, which would have failed every generated project's `PaletteContrastTest`
(`test_a_skeleton_shows_on_every_surface_for_every_accent` runs the rules over 200 accents × 4 tones × 2 themes; a
wider sweep of 288 accents gave 2,304 palettes with no failure, lifts 4.83–5.49).

**Before, for comparison:** Cool light lift +4.3 with the bone 2.6 L\* off `backgroundGrouped`; Cool dark +13.7
with the bone 0.0 off `surfaceElevated`.

**Measured on the phone** (`after/analysis/colours-cat-loading-*.json`, screencaps): light bone `#D7DADF` on cards
`#FFFFFF` and page `#F7F8FA`, band peak `#E7EAEF`, +5.64 L\* (the gradient interpolates encoded sRGB, a little above
the token's +4.9); dark bone `#2D333E`, peak `#393F4B`, +5.43 L\*.

### 2.7 Tokens

`tokens/motion.json`:

```json
"skeleton": {
  "bandWidth": 240, "tiltDegrees": 20, "sweepMs": 1200, "pauseMs": 800,
  "easing": [0.3, 0, 0.7, 1], "referenceTravel": 900, "maxSlowdown": 2
}
```

Validated by the token loader (positive sizes, tilt in [0, 60), pause ≥ 0, slowdown ≥ 1, an easing whose x control
points stay in 0..1), emitted by `genkit/tokens/kotlin.py` into `DesignTokens.Motion` as `skeletonBandWidth: Dp`,
`skeletonTiltDegrees`, `skeletonSweepMs`, `skeletonPauseMs`, `skeletonEasing: CubicBezierEasing`,
`skeletonReferenceTravel: Dp`, `skeletonMaxSlowdown`; by `swift.py` into the Swift `Motion` (`skeletonSweep` and
`skeletonPause` in seconds, `skeletonEasing` a `UnitCurve.bezier`) and `skeletonHighlight` into the Swift roles.
`python3 -m genkit.tokens --check` and `genkit.core.catalogue --check` are current on `46ec781`; `test_tokens` covers
the block. (The Swift emitter is WP08's file; the edit was allowed by this report and WP08 re-emits at its merge.)

### 2.8 Implementation

**Files on `wp/WP03-skel` (`:core:designsystem`):**

| File | Contents |
|---|---|
| `foundation/draw/Skeleton.kt` (238 lines) | `Modifier.skeleton`, `Modifier.shimmerBounds`, `LocalRedacted`, the internal `lineSkeleton`/`textSkeleton`, `ContentUnderBone`, the three elements, `SkeletonNode`, `ShimmerBoundsNode` |
| `foundation/draw/Bones.kt` | `Bones`, `OutlineBones` (any shape, cached outline), `LineBones`, `TextBones` (one bone per laid-out line) |
| `foundation/draw/Sweep.kt` | `TiltCos`/`TiltSin`, `SweepGeometry`, `SweepBrush`, `drawSwept` |
| `foundation/draw/ShimmerClock.kt` | `ShimmerClock`, `ShimmerClock.Bone`, `LocalShimmerClock` |
| `component/feedback/AppSkeleton.kt`, `AppRedacted.kt` | The public components (§2.3) |
| `theme/AppTheme.kt` | Creates the clock in the outermost theme; keeps one `derivedStateOf` for reduce motion for the theme's life, so a node observing it outside composition sees an override change too |

**`SkeletonNode`** (`DrawModifierNode`, `CompositionLocalConsumerModifierNode`, `ObserverModifierNode`,
`ShimmerClock.Bone`). What the review holds it to:

- **Its own layer.** The element is chained after `graphicsLayer()` (`Modifier.layered`), so a sweep frame re-records
  one bone's draw, not the card or row around it. (The first draft placed the child with `placeWithLayer` from a
  `LayoutModifierNode`; a plain `graphicsLayer()` ahead of the draw node does the same with less code.)
- **Joining the clock.** `onAttach` finds the nearest `ShimmerBoundsNode` (a `TraversableNode`) and calls `follow()`;
  `follow()` runs inside `observeReads { }` and reads `LocalTabVisible`, `LocalReduceMotion` and
  `LocalInspectionMode`; when all allow motion it takes `LocalShimmerClock` and `add`s itself, otherwise it `remove`s
  itself. A tab switch or a motion-setting change therefore re-runs `follow()` through `onObservedReadsChanged` and
  recomposes nothing. `onDetach` leaves the clock.
- **Draw.** `clock?.progress ?: NaN`; if `NaN`, the bones draw with a cached `SolidColor(skeleton)`. Otherwise the
  origin is mapped into the area (§2.4) and `drawSwept` draws the bones through the cached `SweepBrush`. The line
  radius for text bones is the style's `xs` corner, at most half the line's height.
- **Auto-invalidation stays on.** The node draws with colours and shapes read from static locals (`LocalAppColors`,
  `LocalAppShapes`), and a static local's change reaches a node only through auto-invalidation: with
  `shouldAutoInvalidate = false` an in-app theme switch left bones in the old colour (review finding; calibration
  `logs/v3-theme-test-before-revert.log`, pinned by `SkeletonStillnessTest` "with motion reduced a bone takes a new
  theme's colour on the next frame").
- **No per-frame allocation.** The outline, the shader and the brush are cached per colours, band width and
  direction; `translate` is inline; `Offset`, `Size` and `CornerRadius` are value classes; `positionInRoot` and
  `localPositionOf` allocate nothing; a generic path is moved in place through one reused `Path`. The gradient is
  never moved with `Shader.setLocalMatrix`: on Android each new matrix discards the native shader and re-registers a
  cleaner, which is a Java allocation per frame, and the translate-and-offset draw is common code (§4.1).
- **Cut-off lines.** A text bone whose line lies past the box's height is skipped, and one that crosses it is cut at
  the edge: a bone's layer does not clip, so a line that `maxLines` or a fixed height cut off would draw over
  whatever lies below.

**`ShimmerClock`:**

```kotlin
internal class ShimmerClock(private val scope: CoroutineScope, private val window: () -> DpSize) {
    var progress = Float.NaN; private set           // a plain field: nothing can recompose on it
    val boneCount: Int get() = bones.size
    private val sweepFrame: (Long) -> Boolean = { now -> /* ease, write progress, invalidate each bone */ }  // one callback for every frame
    fun add(bone: Bone)      // starts the sweep job on the first bone
    fun remove(bone: Bone)   // cancels it on the last; progress = NaN
    private suspend fun sweep() { while (true) { sweepStart = withFrameNanos { it }; while (withFrameNanos(sweepFrame)) {}; delay(Motion.skeletonPauseMs.toLong()) } }
    fun interface Bone { fun onSweepFrame() }
    companion object { fun sweepMillis(size: DpSize): Float }
}
```

- The outermost `AppTheme` creates it with `rememberCoroutineScope()` (the composition's frame clock) and a lambda
  over its `WindowMetrics`; nested themes reuse it.
- One `withFrameNanos` per frame per window is the clock's only frame cost: the frame awaiter every Compose animation
  pays, paid once rather than per bone. One `sweepFrame` lambda serves every frame; a lambda that captured the
  sweep's start and length would be an object per frame.
- `boneCount` says how many bones a sweep frame redraws; `RedactedBoneCountTest` uses it (§2.10).

### 2.9 Behaviour in every condition

| Condition | Behaviour | Frames | Checked by |
|---|---|---|---|
| Sweep in flight, bones on screen | One band crosses the area | One per vsync for 1.2 s | phone: 728 frames / 10 s |
| Pause | Plain bones; one draw as the band leaves | **0** for 0.8 s | `ShimmerClockTest`; phone: 0.80–0.82 s gaps |
| Reduce motion (system, or `AppTheme(reduceMotion = true)`, also flipped mid-sweep) | Plain bones; the node leaves the clock at once | **0** | `SkeletonStillnessTest`; phone: 0 frames / 5 s |
| Hidden tab, covered screen | Plain bones; the node leaves, nothing recomposes | **0** from that tab | `RecompositionJourneysTest`; phone: 0 frames / 5 s covered, 5 CPU ticks |
| Data arrived, or the rows scrolled out of a lazy list | The last bone leaves and the job is cancelled | **0** | `ShimmerClockTest`; phone: 0 frames / 5 s loaded |
| Activity stopped | The Recomposer's frame clock pauses, so `withFrameNanos` waits | 0 | — |
| Outside `AppTheme` (no `LocalShimmerClock`) | Plain bones; the KDoc says so | 0 | `SkeletonStillnessTest` |
| `LocalInspectionMode` (previews, screenshot tests) | Plain bones, so previews are deterministic | 0 | `SkeletonStillnessTest` |
| Robolectric tests | The clock runs on the test's frame clock; `mainClock.advanceTimeByFrame()` puts the band at an exact progress, with no test hook | — | every pixel test |
| Right to left | The band enters at the right edge, mirrored | — | `SweepGeometryTest`; phone: `after/strip-rtl-light.png` |
| Theme or dark-mode change | The brushes are rebuilt (auto-invalidation) | — | `SkeletonStillnessTest` |
| Density or font-scale change | The band's px width changes with the brush; text bones follow the new layout | — | `RedactedGeometryTest` at 1.5× |
| A dialog or popup window | The same clock (locals propagate); the band crosses that window's root | — | — |
| A list scrolled during the sweep | Bones are drawn at their current position each frame | — | — |

### 2.10 Skeletons shaped like the content (K2)

`AppRedacted` provides the internal `LocalRedactedState = true`; `LocalRedacted` is its public read-only view.
Inside it:

| Component | Drawn as |
|---|---|
| `AppText` (all three overloads; `AppExpandableText` through it) | `BasicText` lays out as usual; `Modifier.textSkeleton(bones)` draws one bone per line of the last `TextLayoutResult` (`TextBones.update` from `onTextLayout`, chained with the caller's). The size is exactly the loaded text's. |
| `AppIcon` | A bone over its box, `shapes.xs` |
| `AppAvatar` | A circle bone; its image slot stays redacted so no network image loads under the bone |
| `AppNetworkImage` (`:core:ui`) | A `RectangleShape` bone, nothing loaded; the caller's clip shapes it (K-2) |
| `AppButton` (not `Plain`), `AppIconButton` (with a container), `AppChip`, `AppBadge`, `AppStatusPill` | A bone in their own shape; a `Plain` button has no container to stand for it, so its label and icons redact themselves |
| `AppCard` and other containers | Unchanged, as SwiftUI leaves backgrounds |
| Controls built on `Modifier.pressable` (buttons, cards, chips, `AppRow`, `AppChoiceRow`, `AppSelectField`) | `PressableNode` observes `LocalRedacted`: no tap, key, hover or focus; a held press is let go of and springs back; focus is lost on redaction (`RedactedInputTest`) |
| `AppSwitch`, `AppCheckbox`, `AppRadio`, `AppToggleRow`, `AppSegmentedControl`, `AppSlider`, `AppRangeSlider`, `AppStepper`, text fields | **Not redacted.** The KDoc names them and says to keep them out of placeholder content |
| Custom components | Read `LocalRedacted.current`, add `Modifier.skeleton(shape)`, take no input |

**`ContentUnderBone`.** A component that draws one bone over itself wraps its content in it, which turns the
redaction off inside while the bone is drawn: the bone never draws what is under it, so bones there would be redrawn
on every sweep frame and never seen. The provider stays in place when the component is not redacted, so toggling
keeps the content's composition. `RedactedBoneCountTest`: one each of the six self-boning components is 6 bones to the
sweep, shown and redacted again (16 without it); a plain button with an icon plus a plain icon button is 3.

**Text bones** (`TextBones`, per line i): left `getLineLeft(i)`, right `getLineRight(i)` (an ellipsized line ends at
its ellipsis), top `baseline(i) − 0.72·em`, bottom `baseline(i) + 0.10·em`, corner `min(height / 2, radius xs)`. em is
the style's font size in px when it is in sp (so it follows the font scale), else 0.8 of the first line's box. An
empty line draws nothing. `RedactedGeometryTest` holds the bones inside their line boxes for every role of the type
scale.

**Placeholder data.** Each loading row is the real row composable given a private placeholder value in the feature
(`SampleRowPlaceholder`, `FeedPlaceholder`, …). Its strings give the line count typical content has at phone width;
they are never drawn or read out, so they are not string resources (the KDoc of `AppRedacted` states this once; the
screens point there). A row whose secondary text is clamped keeps its structure; only a title that varies in line
count can differ from its placeholder by a line. That residual is the only movement on arrival, and §5.4 step 5
measures it.

### 2.11 From skeleton to content (no jump, no clip, no alpha)

The pattern every loading list follows (`SampleListScreen.LoadingList`; `FeedScreen`, `SearchScreen.LoadingResults`,
`SearchResult`, `SampleDetailScreen` and `scaffold.py`'s generated list are the same):

```kotlin
@Composable
private fun LoadingList() {
    val loading = stringResource(DesignSystemR.string.designsystem_loading)
    LazyColumn(
        modifier = Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = loading },
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),   // the loaded list's exact expression (WP04's)
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
        userScrollEnabled = false,
    ) {
        items(PlaceholderRows) { AppRedacted(active = true) { SampleRow(item = SampleRowPlaceholder, onClick = {}) } }
    }
}
```

- **Same padding, same rows.** The first row's top is identical in both lists, and nothing sits under the status bar
  or the title (K1). Verified: the first card's top is at the same recording row before and after the swap in light,
  dark, 1.5× and landscape (§5.4 step 5).
- **A list state of its own.** The first draft shared the loaded list's `LazyListState`. Review found the hazard: a
  list state's item animations follow the rows it laid out last, so whenever both lists are on screen together (a
  cross-fade, K5) the loaded rows are compared with the placeholders and every row fades in as new. On an instant
  swap foundation 1.12.1 resets the animator when the placeholder list detaches (`LazyLayoutItemAnimatorModifier`),
  so the calibration with a shared state showed whole rows on the first frame (`logs/v2-swap-calibration.log`); the
  rule is still a state of its own, stated once in `AppRedacted`'s KDoc and pinned by `LoadingSwapTest`. **This
  corrects the first draft and the WP07-A note that still says "one state for both lists".**
- **The title.** `AppLargeTitleTopBar` reads the loaded list's state, which reports 0 while nothing is laid out, so
  the title stays open through the load and collapses with the content (K9 made `scrolledFromTop()` follow item 0
  through the top padding instead of snapping at index 1).
- **The swap** is an instant `when` branch in one frame, with no `AnimatedContent` and no fade. No alpha touches a
  full-screen layer (REQ 21, A11.3). Verified: no recorded frame shows both states (before: one frame did).
- **Row count.** `PlaceholderRows` is 12; a lazy list composes only the rows that show.
- **Search (K5).** `SearchScreen` still cross-fades its content kinds (WP02-M3 replaces that with the same instant
  switch, §3); its `LoadingResults` already follows the pattern, so the swap is one frame once the fade goes.
- **The catalog fixture** (`--es section Loading`, `CatalogLoading.kt`) lifts the redaction in place on one list
  and one data set, so the phone can watch the reveal for movement (`after/strip-reveal-in-place-light.png`: no card
  moves). Its KDoc says an app must not build a loading screen that way.

### 2.12 Cost per window

| | Before (WP03) | Now |
|---|---|---|
| Frames while a skeleton is on screen | Every vsync (1,210 in 10 s on the phone) | 60 % of vsyncs (728 in 10 s) |
| Work per frame, per bone | 2 draws + a `clipPath`; re-records the card's whole display list | 1 draw re-recorded in the bone's own layer |
| Recompositions per frame | 0 (draw-phase read) | 0 (no snapshot write at all; `RecompositionJourneysTest`) |
| JVM allocations in draw | 0 on WP03; a brush, list and shader per bone on WP04 | 0 (`SkeletonDrawAllocationTest`) |
| Hidden tab, reduce motion, stopped, no bones | Clock ran (no provider on WP03) | 0 frames (phone: 0 in 5 s, three conditions) |

### 2.13 Merge and API evolution

- **Branch.** `wp/WP03-skel` was cut from `wp/WP03`, merged it back at `285275d` (`46ec781`), and is in `wp/WP03`
  whole. WP03 merges after WP04.
- **M-1 (gate).** `SkeletonNode.follow()` reads WP03's Boolean `LocalTabVisible` (`foundation/Chrome.kt:42`). On
  WP04 the local is the `TabVisibility` holder (`foundation/layout/Chrome.kt`): the read becomes
  `currentValueOf(LocalTabVisible).isVisible`, still inside `observeReads`. The same port applies to `AppProgress`
  and `AppOtpField`. Without it a skeleton in a kept tab keeps the clock running; do not ship the merge without it.
- **M-2.** At every skeleton call site keep WP03's `AppLargeTitleTopBar` together with WP04's `chromeContentPadding`
  (the expressions on `wp/WP03-skel` are WP04's exact ones).
- **M-3.** This `AppSkeleton.kt` replaces WP04's per-bone `InfiniteTransition` copy; never resolve toward it.
- **Chrome at the merge (from WP04's handoff, Deviation 27).** WP03's `AppScaffold` runs content under the top bar, so
  `chromeContentPadding`'s top becomes `base + max(top chrome, statusBars.top)`; WP03's scaffold writes
  `LocalScreenChrome.current.topBar` and `.background` (for `LocalWindowTopChrome` and the edge fade) and has no
  `imePadding()` (K14).
- **Public API.** Additive except the two `WARNING` deprecations. `AppSkeletonLine()` with no `height` changes from a
  14 dp line to one line of `body` at the current font scale, with no warning: WP12 says so in the upgrade notes.
- **Catalog.** The Feedback caption now describes what the code does; "Skeletons" shows a redacted `PersonCard` with
  a toggle; the Loading page is the measurement fixture.

### 2.14 Scope of `wp/WP03-skel` (every path it changed)

- `core/designsystem`: `foundation/draw/{Skeleton,Bones,Sweep,ShimmerClock}.kt` (new);
  `component/feedback/{AppSkeleton,AppRedacted,AppAvatar,AppBadge,AppStateView}.kt`; `component/text/{AppText,AppIcon}.kt`;
  `component/button/AppButton.kt`; `component/selection/AppChip.kt`; `foundation/interaction/PressableNode.kt`;
  `theme/AppTheme.kt`; `component/navigation/AppTopBar.kt` (K9); `component/container/AppScaffold.kt` (K10);
  `theme/tokens/DesignTokens.kt` (generated); `res/values/strings.xml`; the tests of §5.1.
- `core/ui`: `AppNetworkImage.kt` (K-2), `paging/AppPagingList.kt` (K7).
- `catalog`: `sections/CatalogFeedback.kt`, `sections/CatalogLoading.kt` (new), `CatalogSection.kt`, `CatalogApp.kt`.
- Feature call sites: `SampleListScreen`, `SampleDetailScreen`, `FeedScreen`, `SearchScreen` (`LoadingResults` and,
  past the first draft's "`LoadingResults` only", `Results` took the chrome padding and its row became `ResultCard`,
  so the first loaded row lands where its placeholder did), `SearchResult`.
- Tokens: `tokens/motion.json`, `tokens/palette.json`, `generator/genkit/tokens/{__init__,kotlin,swift}.py`,
  `generator/tests`; `web/frontend/lib/catalogue.json` regenerated (flag at the WP10 merge; done).
- `generator/genkit/targets/android/scaffold.py`: the generated list's loading state.
- Translations of `designsystem_loading` in `generator/translations/android/*/core-designsystem.json`.
- **Not in scope:** `foundation/Chrome.kt` (K6 is WP04's) and the search cross-fade (K5, WP02-M3).

---

## 3. Content clipping: cases, owners, exact fixes, state

**Ground rule** (edge-to-edge skill; CODE-STANDARDS L-4). Insets and chrome reach scrolling content as
`contentPadding` (or as padding inside the scroll), never as padding on a parent. Bars are drawn over content that
runs under them. WP02's `verifyCodeRules` rule `scroll-chrome` (`13eb1f1`) now fails a feature's lazy list or
`verticalScroll` that the chrome does not pad.

| ID | Owner | Exact fix | Acceptance | State |
|---|---|---|---|---|
| K1 | **WP03** | Every loading list takes `chromeContentPadding(PaddingValues(gutter))` and a list state of its own (§2.11): `SampleListScreen.LoadingList`, `FeedScreen`'s loading list, `SearchScreen.LoadingResults`, `SearchResult` and `SampleDetailScreen` (`padding(chromeContentPadding(...))` inside their scroll). The large-title band stays as a guard for rubber-band overscroll only. | §5.3 assertions 1–2; phone: no bone pixel under the status bar or above the title's bottom edge in any cold-start frame, light, dark, 1.5×, landscape | **Done, verified** (`after/analysis/ptr-cold-*.json`: 0 changed px above the first card in every skeleton frame; before 625) |
| K2 | **WP03** | `AppRedacted` around the real row with placeholder data at every skeleton call site, `scaffold.py` included; `AppSkeletonListItem` deprecated | `RedactedGeometryTest`; phone: the first card's top identical across the swap (±1 recording px); every other card moves only by the title lines the cards above it gained over their placeholders | **Done, verified** (§5.4 step 5) |
| K2 (scaffolder snippets) | **WP02-M2** | `snippets/scaffold/feature/ListScreen.kt`, `DetailScreen.kt`: the §2.11 pattern (redacted row, own state, chrome padding), as `scaffold.py` now writes it | A generated module's loading list passes §5.3 assertion 1 | Open; the `scroll-chrome` rule is in (`13eb1f1`). A scaffolded module does not compile today for an unrelated reason (`scaffold.py` imports pre-M1 `core.common.mvi` names), also WP02-M2's |
| K3 (old shell) | **WP04** (`511be84`) | Content runs under the bar and only lists are padded | — | Done; the WP03 phone project must be regenerated on the merged shell before bottom clipping is judged |
| K3 (`Docked` look) | **WP04** | (a) `Docked` (and `Minimal`, `Chunky`) take iOS's scroll-edge appearance: `BarSurface` draws its surface and rule in proportion to `scrolledUnder()`, read in draw; (b) the trigger is the front screen's primary list, reported through `ReportScrollEdge(state)` into the per-screen `ScreenChrome`, `contentBelow = canScrollForward`, eased over `motion.quick`; (c) `BottomEdgeFade` fades into the bar's surface over its tail above a docked bar; (d) WP03's half: Utility defaults to `Floating` | Phone, Utility, light and dark, at rest and during a pull: no glyph cut by a hard horizontal edge above the bar; the end of the list rests a gutter above the bar | WP04: **built** (run 17, uncommitted; `ChromeLayeringTest`, `TabBarTest`). WP03: `tokens/styles.json` `"bar": "Floating"`, `AppDesignStyle.Utility`'s KDoc and the README edited in `wt/WP03`, **uncommitted** (the classifier refused the edit on `wp/WP03-skel`; WP03's run 12 made it). Still to do with that commit: regenerate `DesignTokens.kt`, the website catalogue, and `web/frontend/lib/app-theme.ts` lines 252 and 306 ("docked") |
| K4 | **Merge of WP03** | Keep `AppLargeTitleTopBar` with `chromeContentPadding` in the five screens (M-2) | The merged build's titles collapse and lists slide under the collapsed bar | At the merge |
| K5 | **WP02-M3** | `SearchScreen.kt:105`: replace `AnimatedContent(fadeIn togetherWith fadeOut)` with a plain `when (kind)`; if motion is wanted, per-row `appAnimateItem()` inside the results list, never alpha on the whole content. Then let the large title's `scrolled` read `resultsList` for `Loading` too | Phone, typing "qui": no frame shows the empty state and the rows together; the first result card's top is the same before and after the swap (±1 recording px) | **Open** (`handoff/WP02-M3.md`) |
| K6 | **WP04** | `chromeContentPadding()` returns a `PaddingValues` read in measure (`ChromePadding`): bottom `base + max(bottom chrome, navigationBars.bottom)`; the top floor `statusBars.top` comes with WP03's scaffold at the merge (§2.13) | Phone, landscape rail and a no-bar screen: the last row scrolls fully above the gesture handle and the 3-button bar; `ChromePaddingTest`: a chrome change re-measures without recomposing | **Built** (run 17) |
| K7 | **WP03** | `AppStateView(contentPadding = chromeContentPadding(PaddingValues(xl)))` inside its own scroll (a state inside a box, card, pane or sheet passes `PaddingValues(xl)`; the parameter sits before the trailing `illustration` slot, API-2); `AppPagingList`'s default `contentPadding` is `chromeContentPadding(PaddingValues(gutter))` and `modifier` reaches its loading, failed and empty states | §5.3 assertion 3; `StateViewTest` | **Done**; `after/shots/ptr-offline-font150.png` (offline state at 1.5×, clear of both chromes). WP06's `DevToolsPanel` empty state should pass `PaddingValues(xl)` (§6) |
| K8 | **WP02-M3** | `SearchScreen.StartContent`: a `verticalScroll` column with `padding(chromeContentPadding(PaddingValues(horizontal = gutter, vertical = md)))` inside the scroll | At 1.5× and in landscape the last recent chip scrolls fully into view | **Open** |
| K9 | **WP03** | `scrolledFromTop()`: while item 0 is in `visibleItemsInfo` (the padding region included) return `-item0.offset`; `MAX` only once it has left | `LargeTitleTopBarTest` "a first row shorter than the title is followed under the bar, not skipped" | **Done** |
| K10 | **WP03** | `AppScaffold` pads `WindowInsets.safeDrawing.only(Horizontal)` instead of the cutout only (`AppScaffold.kt:71`). WP04 notes it also pads a pane's inner edge in a two-pane landscape with 3-button navigation, and that `clearDisplayCutout` no longer names what it does: handle both at the merge | Phone with 3-button navigation, landscape: no row end under the navigation bar | Coded; **device check pending** (§6) |
| K11 | **WP04** | `BottomEdgeFade`: tail 16 dp (the list gutter), gated by `scrolledUnder` so a list resting on its end is never veiled, fading into the front screen's `background` under a floating bar and into the bar's surface above a docked one | At the end of a list the last card's border is not veiled, plain and grouped, light and dark | **Built** (run 17) |
| K12 | **Sheet spec** (WP03 component) | Size the sheet's content viewport to the visible detent; edge-attached content takes the navigation-bar inset | `research/SHEET-SPEC.md`'s acceptance | WP03's sheet rework (uncommitted, `handoff/WP03-RESUME.md`) |
| K13 | **WP06** (seam: WP04) | `DevToolsOverlay.bounds()` takes `top = max(status bar + topBarHeight, LocalWindowTopChrome.current.value) + sm`, read in `offset { }` only; WP04's shell writes `LocalWindowTopChrome` from the settled front screen's `ScreenChrome.topBar`, which WP03's scaffold writes at its merge | `badge_qa_check.py --top-chrome` on Samples and a pushed screen, phone and emulator: the badge never intersects the title, the search field or Back | WP04 seam built; WP06 prepared (`out/WP06/badge_qa_check.py --top-chrome`), applied after the WP04 merge |
| K14 | **Merge of WP03** | Take WP03's `AppScaffold` (no `imePadding()`), so the keyboard is counted once, as bottom chrome | With the keyboard up the last row sits directly above it (QA2) | At the merge |
| — | **WP02-M2** | `verifyCodeRules` `scroll-chrome` | Fails the old `LoadingList`, passes the new one | **Done** (`13eb1f1`) |

---

## 4. Parity

### 4.1 Compose Multiplatform (WP07-B, `templates/multiplatform`)

**State:** not started; Android ships first (REQUIREMENTS 36). `handoff/WP07-A.md` carries the note; this section
corrects one line of it (the list state).

**Defects (code at `wp/WP07` `99239af`; no Multiplatform app has run on the phone):**
- `core/designsystem/.../component/feedback/AppSkeleton.kt`: still grey bars with no shimmer, an API
  (`AppSkeletonLine(widthFraction)`, `AppSkeletonRows`) unlike Android's, `LineHeight` fixed at 14 dp.
- `feature/sample/.../list/SampleListScreen.kt`: the skeleton is a `Column` and the loaded list a `LazyColumn`, so the
  header is rebuilt and moves when the data arrives; the loaded list's `contentPadding` is the status bar only.
- `core/navigation/.../AppShell.kt`: the interim `Column` stacks the tab bar under the content, which is REQUIREMENTS
  17's strip (the one that cut the last card on Android).

**Fix:**
1. Port §2 as it is into `commonMain`: `foundation/draw/{Skeleton,Bones,Sweep,ShimmerClock}.kt`, `AppRedacted`,
   `LocalRedacted`, the four public components, and the redaction hooks in `AppText`, `AppIcon`, `AppAvatar`,
   `AppButton`, `AppChip`, `AppBadge` and `PressableNode`. Everything is common API: `positionInRoot`,
   `localPositionOf`, `withFrameNanos`, `LinearGradientShader` with `ShaderBrush`, `graphicsLayer()`, and the
   translate-and-offset draw. No Android `Matrix`, no `setLocalMatrix`. The gradient has no transparent stops, so
   Skia's unpremultiplied interpolation (AUDIT S-11) cannot fringe. On iOS the frame clock is `CADisplayLink`-driven,
   right for 120 Hz ProMotion.
2. `AppSkeletonRows` and the 14 dp line go with no deprecation cycle: no generated Multiplatform app exists yet. Add
   the file pairs to `sync.toml` so drift from Android is caught.
3. Tokens: emit `DesignTokens.Motion.skeleton*` and the `skeletonHighlight` role from the same `tokens/*.json`.
4. Sample list: the §2.11 pattern, **with a list state of its own for the loading list** (not "one state for both",
   as the WP07-A note says: §2.11 gives the reason), the real `SampleRow` with a placeholder value inside
   `AppRedacted`, `contentPadding` from WP04's chrome contract with K6, and a one-frame swap.
5. The shell port (WP07-B's own item 1) removes the `Column`: content runs under the bar and only lists are padded.
6. Bones hold still, with no frames, with reduce motion, in a hidden tab (`TabVisibility`), in previews and without
   `AppTheme`.

**Acceptance:** `commonTest` on a fake `MonotonicFrameClock`: `SweepGeometryTest` and `ShimmerClockTest` as on
Android (§5.1); Android host tests: `SkeletonCoherenceTest` and the recomposition journey;
`:shared:compileKotlinIosSimulatorArm64` passes; on the phone a release build of the generated Multiplatform sample
meets §5.4's numbers (iOS stays unverified, beta).

### 4.2 SwiftUI (WP08, `templates/ios`)

**State: built** on `wp/WP08` (`2bc4c4a` "Sweep one shimmer band across every SwiftUI placeholder in the window",
`7400ba3`, `9babc57`, `cff6d77`, `37558cc`), `accept.sh` `ACCEPT OK` (39 iOS tests). It waits for WP03's merge to
re-emit `DesignTokens.swift` (`Motion.skeleton*`, `Roles.skeletonHighlight`); device verification stays pending
without a Mac (REQ 12).

**What was wrong** (`8e1485d`, AUDIT §7): the band was an overlay that lit the page and the gaps, coloured
`surface.opacity(0.7)` (which darkened bones in dark), 0.6 of the container wide, linear, with no pause, fading to
`.clear`; and `SampleListSkeleton` wrapped a UIKit-drawn `List` in it.

**What stays:** the real row with placeholder data under `.redacted(reason: .placeholder)`, one "Loading" label,
off with Reduce Motion, stopped on disappear.

**As built** (`Packages/Core/Sources/DesignSystem/Components/Skeleton/`):
- `appRedacted(_:)`, a modifier on each row's or card's content (never on a `List` or `ScrollView`; a static check
  enforces it), plus `appLoadingRegion()` for the one VoiceOver label.
- `SkeletonClock` (`@Observable`, one per window through `SkeletonClockProvider` in `AppThemeProvider`): `sweep`
  changes twice per period, never per frame; a sweep is `Motion.skeletonSweep` eased by `Motion.skeletonEasing`,
  lengthened on large windows by `clamp(travel / skeletonReferenceTravel, 1, skeletonMaxSlowdown)`, then a
  `Motion.skeletonPause` sleep; the loop runs only while a placeholder attends and stops when the last leaves
  (`SkeletonClockTests`). A cancelled loop never clears a newer loop's sweep (`37558cc`).
- `SkeletonSweep`/`SkeletonBand`: Android's formula (§2.4) in global coordinates, mirrored for right to left
  (`SkeletonSweepTests`: two origins sample the same phase; the band is off the area at 0 and 1; right to left
  mirrors; a larger window sweeps longer up to the limit).
- Each row wraps only the effect in `TimelineView(.animation(paused: sweep == nil))`; the row's view is built outside
  the timeline closure, so per frame only the shader's arguments re-evaluate.
- **Deviation 20 (accepted).** The band is a Metal **layer effect** (`Resources/AppSkeleton.metal`, `appSkeleton`)
  that draws the token colours, not the colour effect the first draft sketched. SwiftUI draws a placeholder in the
  text's own foreground style at an opacity of its own, and a row's `.foregroundStyle(contentPrimary)` wins over an
  outer one, so a colour effect over the placeholder colour would have been faint or inverted: the dark defect again.
  The layer effect recovers each pixel's coverage from the strongest alpha one point around it and draws
  `skeleton` → `skeletonHighlight` by the band's triangular profile at that coverage. It costs four extra samples per
  placeholder pixel only.
- Previews: light and dark, right to left, the largest text size. Reduce Motion cannot be set in a preview.

---

## 5. Acceptance

### 5.1 Unit and Robolectric tests (`:core:designsystem`, `src/test`; 129/129 pass on the merged tree, `logs/x8-tests-lint-compile-merged.log`)

| Test | Kind | Asserts |
|---|---|---|
| `SweepGeometryTest` | JVM, pure maths | **Two bones at different places sample the same band** (the required test: two placeholders at different x map the band's centre to the same root X, ±0.5 px); a bone further down sees the band further back by the tilt; right to left is mirrored and enters at the right edge; the band starts wholly before the area and ends wholly past it |
| `SkeletonCoherenceTest` | Robolectric `NATIVE`, `captureToImage()` | A 411 dp reference block is the colour of each phase; a 160 × 16 dp line and a 40 dp circle elsewhere match it pixel for pixel (tolerance 3 levels) at four to six instants of one sweep, and the band crossed both. Second case: two bones inside a 200 × 100 dp `shimmerBounds` area match the band laid out from the area's corner over the area's travel, and a bone outside matches the window's |
| `ShimmerClockTest` | JVM, `runTest`, a counting fake `MonotonicFrameClock` | A bone that joins sees no band until the sweep has started; progress eases 0 → 1 over `sweepMillis` (first tenth of the time under 5 % of the way); **no frame is asked for and no bone redrawn during the 800 ms pause**; removing the last bone stops the clock; a 1280 × 800 dp window sweeps `travel / 900` times longer, a 4000 × 3000 one exactly 2× |
| `SkeletonStillnessTest` | Robolectric `NATIVE` | With motion reduced, in a hidden tab, in a preview and outside the theme: the clock never runs and every bone pixel is the plain colour across 120 frames; reducing motion mid-sweep stops the clock at once and allowing it again resumes; with motion reduced a theme change repaints the bone on the next frame |
| `RedactedGeometryTest` | Robolectric `NATIVE` | A redacted row takes exactly the room of the loaded row at font scale 1.0 and 1.5, light and dark, with no glyph pixels; a placeholder line is as tall as the line of text it stands for; each line's bone lies inside the line for every role of the type scale |
| `RedactedInputTest` | Robolectric | A redacted card and button take no tap, and take one again once shown; a redacted button takes no focus and no Enter, and loses focus it had; redacting a held card lets go of the press and brings it back to full size |
| `RedactedBoneCountTest` | Robolectric | Six self-boning components are 6 bones to the sweep, shown and redacted again (16 without `ContentUnderBone`); a plain button with an icon plus a plain icon button is 3 |
| `LoadingSwapTest` | Robolectric `NATIVE` | A redacted list swapped for keyed `appAnimateItem` rows with motion on: the first frame after the swap equals the settled frame pixel for pixel (it pins the behaviour on 1.12.1; it passes with a shared state too, see §6) |
| `SkeletonDrawAllocationTest` | JVM | A shaped bone and the lines of a paragraph draw the band without allocating more than plain bones (1,000-frame warm-up, least of five rounds, `ThreadMXBean`) |
| `PaletteContrastTest` (3 cases) | JVM | For every style × theme: bone ≥ 7 ΔL\* from every surface; highlight lift in [4.5, 6]; in light, highlight ≥ 3 ΔL\* from every surface |
| `StateViewTest` | Robolectric | A state view in a 280 dp box under an `AppTopBar` keeps its title and action inside the box, centred within 1 dp |
| `LargeTitleTopBarTest` (K9 case) | Robolectric | A first row shorter than the title is followed under the bar, not skipped |
| generator `test_tokens`, `test_a_skeleton_shows_on_every_surface_for_every_accent` | Python | The skeleton block's validation; the three colour rules over 200 golden accents × 4 tones × 2 themes |

### 5.2 Recomposition: zero per frame

`RecompositionJourneysTest` "a skeleton list sweeping recomposes nothing, and hiding it recomposes only the
provider": a screen of redacted rows (`AppCard`, `AppText`, `AppAvatar`) plus a lone `AppSkeleton` sweeps under the
Snappy and the Calm motion styles with 0 composables executed; hiding and showing the list through `LocalTabVisible`
stops and restarts the band, and `AppRedacted`, `AppText`, `AppCard`, `AppAvatar`, the row and `AppSkeleton` run 0
times. Pass on `46ec781`.

### 5.3 Screenshot tests

`:core:designsystem` has no Roborazzi (WP02-M2's plugin); `SkeletonScreenshotTest` uses Robolectric `NATIVE`
`captureToImage()` at 411 × 891 dp, light and dark, font scale 1.0 and 1.5, under an `AppLargeTitleTopBar` with 80 dp
of fake bottom chrome, frozen mid-sweep by `mainClock`, PNGs in `core/designsystem/build/outputs/skeleton/`. It
asserts:
1. the first placeholder row starts at or below the top chrome plus the gutter;
2. the list's after-content padding is at least the bottom chrome plus the gutter;
3. the rows' bounds are unchanged when the content replaces the placeholders;
4. (second case) `AppStateView`'s title and action lie between both chromes at 1.5×.

`@AppScreenPreviews` of the loading states exist on the screens (`SampleListLoadingPreview` and the like) for
WP02-M2's preview-scanner goldens after the merge. Note (§6): the test toggles redaction in place on one list,
while the screens swap two lists; `LoadingSwapTest` covers the swap.

### 5.4 Phone check (QA2 / the Verify step; written to `VERIFY.md`)

**Builds and setup, as run on Oct 15** (`after/scripts/session.py`, one `bin/locked adb` hold 16:07–16:13, log
`after/session.log`): release catalog and sample app from `wt/WP03-skel` at `46ec781`
(`out/WP03-skel/cp1/catalog/.../catalog-release.apk`, `out/WP03-skel/ptr/.../app-dev-arm64-v8a-release.apk`),
the Oct 12 catalog for the old numbers; `screenrecord --size 720x1600`; every changed setting read first and
restored, the final state identical to the initial one (`after/device/`).

| Step | Pass criterion | Measured Oct 15 | Verdict |
|---|---|---|---|
| 1. Sweep frames: `--es section Loading`, light and dark, `dumpsys gfxinfo` over 10 s | GPU p90 ≤ 7 ms; frames in 10 s ≤ 750 (sweep 1.2 of every 2.0 s at 120 Hz); janky ≤ 0.5 % (the animation gate of MASTER-PLAN §2); the longest interval between presented frames inside a sweep ≤ 2 vsyncs (what a viewer could see) | Light: 728 frames, GPU p50/p90/p99 6/7/13 ms, frame p50/p90/p99 9/10/17 ms, **5 janky (0.69 %)**; dark: 725 frames, 6/7/13 ms, 5 janky (0.69 %). Of the five, the last-120-frame window shows two at each sweep start after the 817 ms pause (18.1/18.6 ms light, 17.9/18.2 dark; the band still off screen) and one inside a sweep (16.6 ms light, 19.4 dark). Longest interval inside a sweep 15.8 ms light, 11.3 ms dark | GPU, frame count and the visible interval **pass**. Janky is 0.19 points over the gate: **recorded as a deviation pending attribution**. `VERIFY.md` captures the full frame record of one period (framestats dumped right after a sweep, or a Perfetto trace with the GPU frequency track): if the slow frames are the wake from the pause and a GPU-clock dip, accept them as invisible and record it; if they are draw cost where the band crosses the densest rows, reduce the cost (fewer bones per row, or a wider `lineRadius` cache) and re-measure |
| 2. Idle: loaded, reduce motion, covered | 0 frames in 5 s | Loaded 0; reduce motion 0; covered 0 (5 process CPU ticks in 5 s) | **Pass** |
| 3. One band: screencaps across a sweep, light and dark | The band's phase u = x·cos 20° + y·sin 20° is one value across all bone rows: median deviation of the fitted centre ≤ 3 device px, p90 ≤ 12 px (screencaps; video compression adds ±10); the cycle 2.0 s ± 1 frame; no band for 0.8 s between sweeps | Light: median |Δu| 1.48/1.46 px, p90 5.6/6.6 px over 46/41 rows; dark 2.27/1.89/1.84 px, p90 10.7/5.2/10.7 (`after/analysis/coherence-*.json`). Periods 2.024–2.026 s, sweeps 1.207–1.223 s, pauses 0.80–0.82 s, the same timing in right to left (`sweep-timing.json`). Strips: `after/strip-skeleton-list-{light,dark}.png`, `strip-catalog-skeletons-{light,dark}.png` | **Pass** |
| 4. Highlight | Light: at the band's peak the bone stays ≥ 3 ΔL\* below its card; dark: the bone is visible on a raised card, lift 4.5–6 | Light peak `#E7EAEF` on `#FFFFFF`/`#F7F8FA`: +5.64 over the bone, 5.7 L\* below the page; dark peak `#393F4B`, +5.43 | **Pass** |
| 5. Cold start of the sample app after `pm clear`, light, dark, 1.5×, landscape | No bone pixel under the status bar or above the title's bottom edge; the first card's top identical before and after the data (±1 recording px); every other card moves only by the title lines the cards above it gained; no frame with two states | 0 changed px above the first card in every skeleton frame (before 625); first card top 393 → 393 light, dark and landscape, 409 → 409 at 1.5× (before 404 → 396); the cards below shift +37/+40/+81/+81 px light (one headline line is 37 recording px, about 20 dp; cards 1 and 3 have two-line titles), +40/+41/+81/+81 dark, +56/+56/+115 at 1.5× (one line is 56 px there), +38 landscape; no frame shows both states (before: one). Skeleton on screen 0.75 s light. Strips: `after/strip-cold-start-{light,dark,font150-light,landscape-light}.png` | **Pass**; the residual is the data's line count, which the placeholder cannot know |
| 6. Before and after | Side-by-side strips in `after/` | `strip-skeleton-list-*.png`, `strip-catalog-skeletons-*.png`, `strip-cold-start-*.png`, `strip-reveal-in-place-light.png`, `strip-rtl-light.png` | Done |
| 7. The routed bottom strip (K3) | Judged only on a build with WP04's shell | `after/rec/ptr-light-pull.mp4` and `shots/ptr-light-loaded.png` still show the old shell's strip (the ptr app is generated from WP03) | Re-record on the merged shell |

**Also measured.** The catalog's Feedback page ("Skeletons" group, which also runs the activity indicator at 8 steps
a second and the other demos): 763 frames and 21 janky (2.75 %) in 10 s on the new build against 1,210 frames and 0
janky on the old, p50 6 against 7 ms. The old page never idled; the new one most likely wakes from idle about 30
times in 10 s (the indicator's steps during the pauses), and the counted frames are those wake-ups, invisible at 8
steps a second. The last 120 frames of its framestats hold no frame over 8.8 ms. `VERIFY.md` attributes it with step 1.

**Remaining for `VERIFY.md`:** the attribution of step 1; K10 (3-button navigation, landscape), K14 (keyboard up)
and K8 (search chips at 1.5×), which need the device and, for K14 and K8, the merged or the WP02-M3 build; K3's look
on the merged shell; the right-to-left coherence fit (the recording and its timing exist; the screencap fit was not
run).

### 5.5 What the reviewer checks

- **The draw path:** nothing in composition reads the clock; no snapshot state is written per frame; no
  `Modifier.composed`; no `setLocalMatrix`; no `saveLayer` or `clipPath` per bone; the bone has its own layer;
  auto-invalidation kept, with the reason in the KDoc.
- **Behaviour:** reduce motion (also flipped at runtime), hidden tab, preview and no provider each hold still with 0
  frames; the pause asks for no frame.
- **Geometry:** redacted equals loaded at 1.0 and 1.5; every loading list uses the loaded list's padding expression
  and a state of its own; the swap is a `when`, no `AnimatedContent`.
- **KDoc and comments:** KDoc on every public declaration (C-4, C-5); comments give reasons, not narration (C-1,
  C-2); the placeholder-data and own-state rules are stated once, in `AppRedacted`.
- **API:** deprecations at `WARNING` with `ReplaceWith` (API-14); parameters ordered (API-2); `modifier` first and
  applied once (API-3).
- **Tokens:** changed only through `tokens/*.json` and the emitters; `python3 -m genkit.tokens --check` and
  `genkit.core.catalogue --check` clean; the three colour rules hold for every generated accent.
- **Scope:** only the paths of §2.14.

---

## 6. Open items and who takes them

**WP03-skel / WP03 (the shimmer branch):**
- K10's device check (3-button navigation, landscape), and WP04's two notes on `safeDrawing.only(Horizontal)` (a
  pane's inner edge; `clearDisplayCutout`'s name) at the merge.
- Commit the K3 half (Utility `Floating`), D10, D11 and D12 from `wt/WP03`'s dirty tree, with `DesignTokens.kt`, the
  website catalogue and `web/frontend/lib/app-theme.ts` regenerated or edited.
- Test strength: `SkeletonDrawAllocationTest` measures the bones and `drawSwept`, not `SkeletonNode.draw`
  (`currentValueOf`, `positionInRoot`, `localPositionOf`, the brush cache); `LoadingSwapTest` passes with a shared
  state too (it pins behaviour, not the rule); `SkeletonScreenshotTest` toggles redaction in place while the screens
  swap two lists; the `AppPagingList` state change and `AppNetworkImage`'s redacted branch have no tests.
- F-5: the feature modules borrow `designsystem_loading` for their loading lists, and the loading region has no live
  region. A design-system helper that owns the announcement would fix both.
- `9823262` ("Keep a link button on its text column…") also deleted `component/overlay/SheetTest.kt` (7 cases),
  which its message does not say; restore it unless the sheet rework replaces it.
- The Cool tone's skeleton roles are hex values between ramp steps (accepted; they come from the ΔL\* rule).

**Merge of WP03 after WP04:** M-1 (`TabVisibility` port, a gate), M-2 (K4), M-3, K14 (WP03's scaffold), the chrome top
floor and `ScreenChrome` writes (§2.13); then regenerate the WP03 phone project and re-record K3.

**WP04:** commit run 17 (K3, K6, K11, the K13 seam, D9, D12); the WP03-side seams listed in its handoff.

**WP02-M3:** K5, K8; `AppPagingList` taking its `LazyListState` so the feed can `ReportScrollEdge`.

**WP02-M2:** the scaffolder snippets (K2); `scaffold.py`'s pre-M1 imports, which keep a scaffolded module from
compiling.

**WP06:** K13 after the WP04 merge; `DevToolsPanel.kt`'s empty `AppStateView` passes `contentPadding =
PaddingValues(AppTheme.spacing.xl)` (it is in a full-screen dialog that clears the insets itself).

**WP07-B:** §4.1, with the list-state correction.

**WP08:** re-emit `DesignTokens.swift` at its merge; device verification when a Mac exists.

**WP12:** the upgrade note on `AppSkeletonLine()`'s height; the rules the docs must state (from
`handoff/WP03-skel.md`): one band per window, mapped into one coordinate space, never sized or timed by the element; a
sweep then a pause with no frames, nothing sweeping while hidden, reduced or idle; loading states redact the real row
with placeholder data in the loaded list's padding on a list state of their own and swap in one frame, never a
cross-fade over full-screen layers; only text, icons, avatars, images, chips, badges, pills and filled buttons become
bones and only `pressable` controls stop taking input; a component that draws its own bone wraps its content in
`ContentUnderBone`; a tone mixes the accent into `skeleton` only; a node that draws from a static local keeps
auto-invalidation; a state view that is not the whole screen passes its own `contentPadding`; scrolling content takes
chrome as `contentPadding`, never parent padding.

**Values.** The band, tilt, sweep and pause (§2.5) were chosen from the references and the phone's geometry and
measured as specified on Oct 15. QA2 may tune them within ±20 % from the recordings; any change goes into the tokens
with its reason.
