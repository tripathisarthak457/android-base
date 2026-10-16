# Shimmer and content clipping: code audit (REQUIREMENTS 34)

Written 2026-10-12 at 13:58 IST. This was a read-only audit. No repository, worktree or device setting was changed, and
this audit did not use the phone. The device frames cited below come from the evidence pass in `evidence/`.

**Branch heads read:**
- `wp/WP03` at `ec4dcb7`. The worktree has 25 modified files, all of them tests or catalog files, none in the
  skeleton or the screens.
- `wp/WP04` at `230f894`.
- The integration branch `overhaul/multiplatform-v2` at `8672003`.
- `wp/WP07` at `ec15ef4`, `wp/WP08` at `a95111a` and `wp/WP02` at `916b5f9`.

Paths are relative to `templates/android/` unless they start with another root. Line numbers are at those heads.

**The build the user watched.** `com.example.wp03.ptr.dev` was generated at 13:21 into `out/WP03/ptr` from the WP03
working tree. After the package rename, its `AppSkeleton.kt` and `SampleListScreen.kt` are byte-identical to
`wp/WP03`. Its `AppTopBar.kt` already has the large-title band, which was committed later as `ec4dcb7`.

**Severity** (the scale of `qa/r1/DEFECTS.md`):
- **S1:** visible on every launch, or it breaks a task.
- **S2:** a clear polish defect.
- **S3:** minor.
- **V:** needs a device check.

---

## 1. What the user saw, traced to code

### "Shimmer has issues"

1. **Every bone sweeps its own band.** On launch, the sample list shows six skeleton cards
   (`feature/sample/.../list/SampleListScreen.kt:137-148`), each with two bones. On the Redmi (393 dp wide), the
   bones are 0.6 and 0.9 of a 297 dp row.
   - Each bone sweeps a band that is 60 % of its own width across its own width, every 1.4 s.
   - The short bone's band is 107 dp wide and moves at 204 dp/s. The long bone's band is 160 dp wide and moves at
     305 dp/s.
   - Inside one card, the two highlights drift up to about 90 dp apart. They restart together at the left edge.
   - The result is twelve flickers out of step, not one light crossing the list.
   - The phone frames agree. In `evidence/analysis/sweep-ptr-cold-light.tsv` at t = 1.483 s, the two bones of one
     card peak at x = 276 px and x = 387 px. In the catalog (`sweep-catalog-light.tsv`), a 72 px circle gets a 20 px
     band at 114 px/s, and a 661 px block gets a 173 px band at 962 px/s.
2. **The highlight is unbalanced between themes.**
   - In light, the highlight is +4.3 ΔL* over the bone, which is barely visible.
   - In dark, it is +13.7 ΔL*, but the bones are only 4.9 ΔL* off the card. In dark, bright bars race over bones
     that can hardly be seen. The measured peak excess is about 31 luma in dark against about 12 in light
     (`sweep-catalog-*.tsv`).

### "Content gets cut"

1. **The skeleton list ignores the top bar.** It is padded with `PaddingValues(gutter)` only
   (`SampleListScreen.kt:141`), so it starts 16 dp below the window top: under the status bar, the large title and
   the search field.
   - Since `ec4dcb7`, the large title paints the screen colour over its own band (`AppTopBar.kt:151-153`), but not
     over the inline-bar area above it.
   - So the first skeleton card shows behind the status bar, is cut off at the top of the title band, and the
     second card is cut again at the band's lower edge.
   - Device frame: `evidence/frames/ptr-cold-light-skeleton-full.png`.
2. **The test project still runs the old shell.** `core/navigation/.../AppNavHost.kt:156-166` pads the root entry
   above a strip painted in the background colour, which cuts the last card
   (`perf/WP03/ptr/r8-after/light-rest.png`, `dark-rest.png`). This is REQUIREMENTS 17 and QA-01. WP04's shell
   removes the strip.

---

## 2. Defect index

| ID | Sev | Area | Owner | Status |
|---|---|---|---|---|
| S-1 | S1 | Each bone sweeps its own band, sized and timed by its own width | WP03 (`wp/WP03-skel`) | Open on every branch |
| S-2 | S2 | No tokens for band, angle, period, pause and easing; the highlight differs by 3× between themes | WP03 + tokens | Open |
| S-3 | S1 (dark) | Bones are invisible on raised cards in dark, and faint on plain ones | WP03 + tokens | Open; the test the handoff names does not exist |
| S-4 | S3 | Two draws and a `clipPath` per bone per frame, with no layer of its own | WP03 | Open |
| S-5 | S2 | No pause between sweeps: frames are drawn for as long as any bone is on screen | WP03 | Open |
| S-6 | S2 | Visibility: a Boolean read in composition on WP03; ignored entirely on WP04 and the integration branch; the merge breaks the API | WP03 (port) | Open |
| S-7 | S3 | No provider means silently static; previews and screenshots catch a random band | WP03 | Open |
| S-8 | S3 | The sweep is not mirrored in right-to-left layouts | WP03 | Open |
| S-9 | S3 | The first frame after a re-attach shows a stale band position | WP03 | Open |
| S-10 | S3 | Android never announces "Loading"; the Multiplatform and SwiftUI skeletons do | WP03 / WP02-M3 | Open |
| S-11 | V | Gradient stops at `Color.Transparent` (safe on Android, a risk for Multiplatform) | WP03 / WP07 | Removed by S-4's fix |
| S-12 | S2 | The old skeleton on WP04 and the integration branch: one `InfiniteTransition` and one new `Brush` per bone per frame | (superseded) | Fixed by WP03's version; see M-3 |
| K-1 | S2 | Skeleton shapes do not match the content, so it jumps on arrival (much worse at font 1.5) | WP03 (API), WP02-M3 (screens), WP02-M2 (scaffold) | Open |
| K-2 | S3 | `AppNetworkImage`: a rounded skeleton inside the caller's clip, with a band per image | WP03 | Open |
| K-3 | S2 | Skeleton to content: Search cross-fades the whole screen through alpha | WP02-M3 | Open |
| C-1 | S1 | Loading, empty and start states and lists without chrome padding | WP02-M3, WP03, WP02-M2 | Fixed on WP04 for most screens; open on WP03, integration and the scaffolder |
| C-2 | S2 | Large title: the band hides misplaced content instead of placing it, and `scrolledFromTop()` snaps the title | WP03 | Open |
| C-3 | S2 | `chromeContentPadding` never adds the navigation bar, so the last row sits under it wherever no chrome is written | WP04 (contract), WP03 (function) | Open |
| C-4 | S2, V | The scaffold clears only the display cutout at the sides; with 3-button navigation in landscape, rows end under the bar | WP03 | Open |
| C-5 | S1 | The old shell's opaque strip under the bar | WP04 | Fixed on WP04 |
| C-6 | S3 | The bottom edge fade is taller than the list's last padding, and fades to `background` on grouped screens | WP04 | Open |
| C-7 | S2 | Sheets: the bottom of the content is off screen at the medium detent, and there is no navigation-bar inset | WP03 (sheet spec) | Open; D4 in `research/sheet/current-state.md` |
| C-8 | S3 | The DEV badge's top bound ignores the large title | WP06 | Open |
| C-9 | S2, V | The keyboard is counted twice on WP04: the scaffold's `imePadding()` and the keyboard reported as bottom chrome | WP04 / merge | Goes away if WP03's `AppScaffold` wins |
| M-1…M-3 | build | Merge hazards between WP04's chrome contract and WP03's design system | merge of WP03 | See §5 |

---

## 3. Shimmer defects

### S-1 (S1): every bone sweeps its own band

**Where:**
- `wp/WP03`, `core/designsystem/.../component/feedback/AppSkeleton.kt`:
  - `:102` `val band = size.width * BandFraction`.
  - `:104-110` a gradient spanning `0..band` in the bone's own coordinates.
  - `:114` `val x = -band + (size.width + band) * clock.progress`.
  - `:158` `BandFraction = 0.6f`.
- `wp/WP04` and the integration branch, same file:
  - `:43-52` a `rememberInfiniteTransition` per bone.
  - `:62` `start = -width + progress * (width * 3f)`, again per bone.

**Root cause:**
- `ShimmerClock` (`:45-74`) shares only a phase from 0 to 1. Every bone turns that phase into a position, a band
  width and a speed from its own `size`, so bones of different widths can never line up.
- The KDoc (`:39-44`: "sweeps in step") and the catalog caption (`catalog/.../CatalogFeedback.kt:50`: "One shared
  sweep") describe a shared band that the code does not draw.

**Fix:**
- Put the band in root (window) coordinates. One clock gives the band's x; each bone reads `positionInRoot()` in its
  draw and paints the band at `bandX - origin.x`.
- The band width is a dp token, not a fraction of the bone.
- The sketch in §6.3 is a `Modifier.Node`, the way Wear Compose Material 3, compose-shimmer and Facebook Shimmer all
  do it (§6.1).

### S-2 (S2): no tokens, and a highlight that differs by theme

**Where:**
- `AppSkeleton.kt:72` (`PeriodMillis = 1_400L`, linear, no pause) and `:158`. The band is horizontal.
- Colours: `core/designsystem/.../theme/tokens/DesignTokens.kt:253-254`, `:296-297`, `:342-343`, `:385-386`,
  `:431-432`, `:474-475`, `:520-521`, `:563-564`, emitted from `tokens/palette.json:80-81` (light: `@fill` and
  `@background`) and `:104-105` (dark: `ink.700` and `ink.500`).

**Measured (CIELAB L\*):**

| Palette | Theme | Bone vs card surface ΔL\* | Highlight vs bone ΔL\* |
|---|---|---|---|
| Cool (Utility) | light | 6.7 | +4.3 |
| Cool (Utility) | dark | 4.9 | **+13.7** |
| BrandTint (Social) | light | 10.3 | +7.8 |
| BrandTint (Social) | dark | 6.2 (**1.4** on a raised card, S-3) | +9.9 |
| Paper (Editorial) | light | 8.6 | +5.5 |
| Paper (Editorial) | dark | 8.4 | +9.6 |
| Cream (Playful) | light | 7.5 | +5.0 |
| Cream (Playful) | dark | 8.8 | +9.3 |

**Root cause:** the highlight is mapped to an existing role (`background`, `ink.500`) rather than defined as a
perceptual step over the bone. No token exists for the band's geometry or timing.

**Fix:**
- Add skeleton tokens to `tokens/motion.json` and `palette.json`, emitted to `DesignTokens.Skeleton`, with the values
  in §6.2: band width, tilt, sweep, pause, easing, `skeleton` and `skeletonHighlight`.
- Derive both colours as ΔL\* steps: a bone at least 7 ΔL\* from every surface it sits on, and a highlight 5 to 7
  ΔL\* above the bone, in both themes.
- Example for Cool dark: bone ≈ `#232934`, highlight ≈ `#303641`.

### S-3 (S1 in dark with raised cards): invisible bones

**Where:**
- `palette.json:104` sets the dark `skeleton` to `ink.700`, which is the dark `surfaceElevated`.
- `component/container/AppCard.kt:79-81` puts raised cards on `surfaceElevated`.

**Effect:**
- For the Social style (raised cards on BrandTint), dark bones are 1.4 ΔL\* from their card: invisible. Only the
  bright band (S-2) shows, sweeping over empty cards.
- For Utility, a dark bone on a plain card is 4.9 ΔL\* (faint).
- The WP03 handoff (`handoff/WP03.md:376`) lists "Dark skeleton invisible → `PaletteContrastTest` + `skeletonHighlight`
  role", but `PaletteContrastTest` has no skeleton assertion.

**Fix:** use the token rule in S-2, and add `PaletteContrastTest` cases: bone against `surface`, `surfaceGrouped` and
`surfaceElevated` at least 7 ΔL\*, and highlight against bone between 4 and 8 ΔL\*, for every palette and theme.

### S-4 (S3): two draws and a clip per bone per frame

**Where:** `AppSkeleton.kt:111-118`. It draws the outline in the bone colour, then
`clipPath(clip) { translate { drawRect(band) } }`. The bone is a `Spacer` with no layer, so each frame re-records the
display list of the nearest layer (the card's).

**Fix:**
- Draw once: `drawOutline(outline, brush)` with a CLAMP gradient that goes bone, highlight, bone. Outside the band
  the clamp paints the plain bone colour, so no clip and no second draw are needed.
- Move the gradient without allocating:
  - On Android, cache one `LinearGradientShader` and call `setLocalMatrix` each frame. This is what Facebook's
    `ShimmerDrawable` and compose-shimmer's `ShimmerEffect` do.
  - In common code, `translate(dx)` the canvas and draw the outline at `-dx`.
- Give the node its own layer (`placeWithLayer` in a `LayoutModifierNode`), so a frame re-records one draw per bone.
  `AppActivityIndicator` got the same treatment in `4a3f9e4`.

### S-5 (S2): no pause, so frames never stop

**Where:** `AppSkeleton.kt:55-58`. A `withFrameMillis` loop writes `progress` on every frame for as long as one bone is
attached. At 120 Hz, a loading screen redraws continuously.

**Fix:** a sweep followed by a pause (§6.2). During the pause the clock writes "resting" once and calls `delay()`, so
the window draws no frames. Compose-shimmer pauses 1.5 s between 0.8 s sweeps, and Wear Material 3 pauses 2 s.

### S-6 (S2): visibility

**Where:**
- On `wp/WP03`, `AppSkeleton.kt:89` reads `LocalTabVisible.current`, a Boolean `compositionLocalOf`
  (`foundation/Chrome.kt:42`), in composition.
  - Nothing on WP03 provides it: the old shell has no provider. A skeleton in a hidden tab keeps the clock running.
- On `wp/WP04` and the integration branch, the old `AppSkeleton.kt` ignores visibility and reduce motion altogether.
  - Its per-bone `InfiniteTransition`s tick inside parked tabs and screens, which are drawn at alpha 0 off the window
    (`StackScene.kt:243`, `TabReveal.kt:143`).
  - The window therefore never goes idle.
- WP04 replaced the local with a `TabVisibility` holder (`foundation/layout/Chrome.kt:47-54`). Its `isVisible` is
  meant to be read in an effect or a draw block (SMOOTHNESS-PLAN, item 3).

**Fix:** port to the holder. The node reads `currentValueOf(LocalTabVisible).isVisible` and reduce motion inside
`observeReads { }` (an `ObserverModifierNode`) and attaches to or detaches from the clock. Nothing recomposes on a tab
switch. Apply the same port to `AppProgress.kt:53` and `AppOtpField.kt:150`.

### S-7 (S3): no provider, previews and screenshots

**Where:** `AppSkeleton.kt:76` defaults to `null`, which makes `:89` static without saying so. Under
`LocalInspectionMode` and in screenshot tests, the band's position depends on the clock, so screenshots are not
reproducible.

**Fix:** keep `null` as static, but say so in the KDoc. Draw static under `LocalInspectionMode` (API-13).

### S-8 (S3): right-to-left layouts

**Where:** `AppSkeleton.kt:104-114` sweeps left to right whatever the layout direction.

**Fix:** sweep in reading direction, as Facebook Shimmer's `direction` does.

### S-9 (S3): stale first frame

**Where:** `ShimmerClock.progress` keeps its last value while the clock is stopped. The first frame after a re-attach
draws the band at that old position, and the next frame jumps to the phase computed from the frame time.

**Fix:** compute the phase from the frame time on attach, or draw no band until the first tick.

### S-10 (S3): accessibility

**Where:** every bone clears its own semantics (`AppSkeleton.kt:98`), and no Android call site announces "Loading".
Multiplatform's `AppSkeletonRows` (`wp/WP07 .../AppSkeleton.kt:43`) and SwiftUI's `AppSkeleton`
(`AppSkeleton.swift:32-33`) both do.

**Fix:** the skeleton container (the `AppRedacted` of K-1, or the loading list) sets one `contentDescription` from
`designsystem_loading`.

### S-11 (V): transparent gradient stops

**Where:** `AppSkeleton.kt:105` and `:107` use `Color.Transparent`.

**Android:** no defect. HWUI builds every linear gradient with `kInterpolateColorsInPremul_Flag`
(`frameworks/base/libs/hwui/jni/Shader.cpp`, `sGradientShaderFlags`), so a transparent black stop leaves no dark
fringe.

**Multiplatform:** Skia's default is unpremultiplied interpolation, so a port must check skiko's flag. The single-draw
gradient of S-4 has no transparent stops, which settles this either way.

### S-12 (S2): the old skeleton on WP04 and the integration branch

**Where:** `AppSkeleton.kt:58-69` reads `progress` inside the cache block of `drawWithCache`. The cache is rebuilt
every frame, which allocates a `listOf(...)`, a `Brush` and a shader per bone per frame. There is also one
`InfiniteTransition` (one frame coroutine) per bone. The highlight is `surface` in light and `surfaceVariant` in dark
(`:37-41`).

**Status:** WP03's file replaces it on merge (M-3). It is listed so that nobody resolves the merge toward this
version.

---

## 4. Skeleton shapes and content clipping

### K-1 (S2): skeleton shapes do not match the content

**Root cause:** each skeleton is a hand-made approximation of a row, sized in dp. `AppSkeletonListItem` also bakes in
its own padding (`AppSkeleton.kt:146`, against API-3 and API-4), and every call site wraps it in an `AppCard`, which
pads again.

| Call site (WP03) | Skeleton | Loaded content | Height (font 1.0) | At font 1.5 |
|---|---|---|---|---|
| `SampleListScreen.kt:145` | `AppCard { AppSkeletonListItem(false) }`: 90 dp; bones inset 32 dp | `SampleRow` (`component/SampleRow.kt:20-35`): 98 to 120 dp; text inset 16 dp | +8 to +30 dp per row | the row grows to 129–162 dp; the skeleton stays 90 |
| `feature/feed/.../FeedScreen.kt:157` | circle and two lines: 96 dp | `FeedCard` (`:103-146`): avatar row, title, two-line body, button: about 196 dp | about +100 dp per row | worse |
| `feature/search/.../SearchScreen.kt:230` | 90 dp | result card (`:198-217`): 96 to 116 dp | +6 to +26 dp | worse |
| `feature/search/.../SearchResult.kt:119-123` | three 14 dp lines | a headline and a body paragraph | different structure | — |
| `feature/sample/.../detail/SampleDetailScreen.kt:60-65` | five 14 dp lines | `#id`, a `title2` title, the body, related cards | different structure | — |
| `generator/genkit/targets/android/scaffold.py:288` (WP03), and WP02's `snippets/scaffold/feature/ListScreen.kt:99` | two-line bones | a single headline row | the skeleton is taller than the content | — |

- `AppSkeletonLine(height = 14.dp)` (`AppSkeleton.kt:125-131`) is in dp, so it never follows the font scale.

**Fix:**
- Build placeholders the way SwiftUI's `.redacted(reason: .placeholder)` does: the real row composable, given
  placeholder data, inside `AppRedacted { }`. Apple's docs say the content is masked as placeholders while
  "maintaining their original size and shape".
- In placeholder mode:
  - `AppText` draws one bone per line from its `TextLayoutResult` (the line's box less its leading).
  - `AppIcon`, `AppAvatar`, `AppNetworkImage` and `AppButton` draw their own shape.
  - Every bone uses S-1's node.
- The geometry is then identical to the loaded row at any font scale, and nothing jumps. WP08 already works this way
  (`SampleListView.swift:84-96`).
- Keep `AppSkeleton` for free-form blocks. Remove `AppSkeletonListItem`'s internal padding, or deprecate it.

### K-2 (S3): `AppNetworkImage`

**Where:** `core/ui/.../AppNetworkImage.kt:36` draws `AppSkeleton(Modifier.fillMaxSize())` with the default `sm`
rounding inside whatever the caller clips to, and gives each image its own band (S-1).

**Fix:** use `RectangleShape`, since the caller clips, and S-1's shared band.

### K-3 (S2): skeleton to content

**Where:** `SearchScreen.kt:101-104` cross-fades the whole content area with `AnimatedContent` and
`fadeIn`/`fadeOut`. That is a `graphicsLayer` alpha on a full-screen layer, which is drawn offscreen (forbidden by
the requirements). `SampleListScreen` swaps instantly.

**Fix:** once K-1 makes the shapes match, swap in place: no full-screen alpha, and per-row `animateItem` or nothing.
Owner: WP02-M3.

### C-1 (S1): states and lists without chrome padding

**Root cause:** `chromeContentPadding()` is opt-in for each call, and nothing fails when a scrolling container inside
an `AppScaffold` leaves it out. Each place below starts under the large title or the top bar, ends under the bar, or
both.

| WP03 location | Problem | On WP04 |
|---|---|---|
| `SampleListScreen.kt:139-147` (loading list) | `PaddingValues(gutter)` only; also scrollable (`userScrollEnabled` defaults to true) and not tied to the title (C-2) | fixed (`:151`) |
| `SampleDetailScreen.kt:52-56` | `padding(gutter)` only, so `#id` and the title sit behind the solid `AppTopBar` | fixed (`:57`) |
| `SearchScreen.kt:150-154` (start content) | no chrome, and **not scrollable**: at font 1.5 or in landscape, the recent chips are cut at the bottom | **still open** (`:157-159`) |
| `SearchScreen.kt:191-195` and `:223-231` | results and loading lists use `PaddingValues(gutter)` | fixed (`:199`, `:231`) |
| `SearchResult.kt:111-116` | `padding(gutter)` only | fixed (`:116`) |
| `FeedScreen.kt:69-75` and `:150-153` | `AppPagingList` falls back to its default `PaddingValues(gutter)`; the loading list is the same | fixed (`:71`, `:149`) |
| `core/ui/.../paging/AppPagingList.kt:50` | the default `contentPadding` ignores chrome; `:59-62` ignore `modifier` in the loading, failed and empty states (API-3) | same |
| `component/feedback/AppStateView.kt:65-71` | centred in the whole window, under the title and the bar; at font 1.5 or in landscape its top or its action can be covered | same |
| `scaffold.py:283-296` (WP03), and WP02's `ListScreen.kt:68` and `:96` and `DetailScreen.kt:54` | every scaffolded module is generated with this defect | same |

The integration branch has the same feature code as WP03's column, on top of the old shell.

**Fix:**
1. Keep WP04's fixes when WP03 merges. The merge order is WP04 then WP03. WP03 rewrote these same files for the large
   title, so a conflict resolved toward WP03 drops them (M-2).
2. Make `AppPagingList`'s default `chromeContentPadding(PaddingValues(gutter))`, and pass the modifier through in every
   state.
3. Give `AppStateView` `.padding(chromeContentPadding(...))` inside its scroll.
4. Make `StartContent` scroll, with chrome padding.
5. Update the scaffolder snippets.
6. Add a `verifyCodeRules` rule, `scroll-chrome`: a `LazyColumn`, `LazyVerticalGrid` or `verticalScroll` in a feature's
   `AppScaffold` takes `chromeContentPadding`.

### C-2 (S2): the large title

**Where:**
- `ec4dcb7` paints the screen colour over the title's band (`component/navigation/AppTopBar.kt:146-154`). That hides
  content placed under the title rather than placing it below. Combined with C-1, it is what cuts the skeleton cards
  on the phone.
- The band leaves the inline-bar area clear, so the misplaced card still shows behind the status bar
  (`evidence/frames/ptr-cold-light-skeleton-full.png`).
- `scrolledFromTop()` (`AppTopBar.kt:55-56`) returns `Int.MAX_VALUE` as soon as item 1 is the first visible item.
  `firstVisibleItemIndex` does not count items inside the content padding, so a first row shorter than the collapse
  distance (about 121 dp with a search field) snaps the title the rest of the way.
- Loading lists and other states are not wired to `scrolled`:
  - `SampleListScreen.kt:58` reads the loaded list's state while the loading list scrolls on its own.
  - `SearchScreen.kt:85` returns 0 unless results are showing.

**Fix:**
- Compute `scrolled` exactly. While item 0 is in `layoutInfo.visibleItemsInfo` (which includes the padding region),
  `scrolled` is `-item0.offset`; it becomes `MAX` only once item 0 has left that region. This is still read in draw.
- Share one `LazyListState` between the loading and loaded lists, or make loading lists non-scrollable.
- Keep the band as a guard for rubber-band overscroll, not as the fix.

### C-3 (S2): no navigation-bar inset where no chrome is written

**Where:** `chromeContentPadding()` (`foundation/Chrome.kt:46-56` on WP03, `foundation/layout/Chrome.kt:92-103` on WP04)
adds only the holder's value. The holder is 0 in these cases:
- Wide windows with a rail or sidebar: `shell/BottomChromeWriter.kt:38` passes `bar = if (wide) 0.dp`.
- Screens that hide the bar: `stack/EntryFrame.kt:42` and `:50` (`NoChrome`).
- Presented covers: `presentation/PresentationHost.kt:185`.
- The single-stack host: `AppNavigationHost.kt` writes nothing.
- Sheets without a footer: `AppSheet.kt:243`.

In each case, a list's last row ends under the 3-button navigation bar or the gesture handle and cannot scroll clear.
`evidence/shots/ptr-landscape-light-scrolled-more.png` shows the handle over a card in the rail layout. The toast
already does this correctly (`AppToast.kt:259`: `maxOf(chrome, navigationBars)`).

**Fix:**
- The bottom padding is `base + max(chrome, navigationBars.bottom)`. The top is the same with the status bar, for a
  scaffold that has no top bar.
- Return a `PaddingValues` whose `calculate*Padding` reads the holder and the insets lazily, as `InsetsPaddingValues`
  does. A settled chrome change then re-measures the list rather than recomposing the screen.
- Owner: WP04 (the contract), with WP03 (the function).

### C-4 (S2, V): side insets

**Where:** `AppScaffold` clears only the display cutout at the sides (`component/container/AppScaffold.kt:61` on WP03,
`:57` on WP04). With 3-button navigation on a phone held sideways, the navigation bar is at the end edge, so the ends
of rows sit under it. `AppShell` consumes only the start side, for the rail.

**Fix:** pad `WindowInsets.safeDrawing.only(Horizontal)`. Check it on the device with 3-button navigation.

### C-5 (S1, fixed on WP04): the old shell's opaque strip

**Where:** WP03's `core/navigation/.../AppNavHost.kt:156-166` (integration `:163`) puts the root screen in a
`Box(background).padding(bottom = inset)`. This is QA-01. WP04 deletes it.

**Note:** regenerate the WP03 phone project after WP04 merges before judging any bottom clipping.

### C-6 (S3): the bottom edge fade

**Where:** `shell/BottomEdgeFade.kt:38` and `:58`. The fade reaches 24 dp above the bar, but lists end 16 dp above it
(gutter), so at the end of a list the last card's bottom 8 dp is veiled by up to about 9 %. The fade also uses
`colors.background` (`:44`), while a screen may paint `backgroundGrouped` (`CatalogApp.kt:152`), which after the merge
shows a lighter band under the bar.

**Fix:** keep the tail at or below the lists' base padding (or report it as bottom chrome), and fade to the front
screen's `LocalScaffoldBackground`.

### C-7 (S2): sheets

**Where:** `AppSheet.kt:238` measures the sheet at its large height at every detent. At the medium detent, the bottom of
the content is off screen, so a list cannot reach its last rows. This is D4 in `research/sheet/current-state.md`,
which raises it for the keyboard. Edge-attached sheet content gets no navigation-bar inset (`:211-214`; see C-3).

**Fix:** handled by the sheet spec. Size the content viewport to the visible detent, as UIKit sheets resize the
presented view.

### C-8 (S3): the DEV badge

**Where:** `wp/WP06` `core/devtools/.../badge/DevToolsOverlay.kt:101` bounds the badge at status bar + standard top bar
height. Dragged upwards, it can rest on the large title or the search field. Docked, it covers the ends of rows at
85 % alpha (`:174`). It appears in dev builds only.

**Fix:** bound the badge by `LocalTopChrome`.

### C-9 (S2, V): the keyboard counted twice on WP04

**Where:** WP04's `AppScaffold.kt:50` applies `imePadding()` to the whole scaffold, while `BottomChromeWriter.kt:35`
and `:52` also report the keyboard as bottom chrome. With the keyboard up, a list therefore gets the keyboard's height
as padding inside a scaffold that has already shrunk by that height.

**Fix:** WP03's `AppScaffold`, which has no `imePadding`, must win the merge.

### PTR resting offset: no defect

The spinner sits at top chrome + 16 dp (`AppPullToRefresh.kt:148`), the rest gap is 60 dp (`:490`), and the gap moves
the content in placement (`:106`). Every chrome value is read in measure, so nothing recomposes. Frames in
`r8-after/*-rest.png` show the spinner under the search field and the first card below it.

---

## 5. Merge hazards (they bear on both issues)

### M-1: the chrome contract moved

WP04 moved `foundation/Chrome.kt` to `foundation/layout/Chrome.kt` and turned `LocalTabVisible` into the
`TabVisibility` holder. Fourteen WP03 main-source files import the old names:
- `AppScaffold`, `AppProgress`, `AppSkeleton`, `AppOtpField`, `AppTopBar`, `AppSheet`, `AppToast`,
  `AppPullToRefresh`, `ScrollIndicator` and `AppTheme`;
- `CatalogApp`, `CatalogBars` and `CatalogContainers`;
- `SampleListScreen`.

The build breaks loudly, so this is not silent. The port should do more than fix imports: read `.isVisible` in an
effect or a draw block, not in composition (S-6).

### M-2: WP04's chrome fixes collide with WP03's title rewrite

WP04's chrome-padding fixes (C-1) and WP03's large-title rewrite touch the same lines of `SampleListScreen`,
`SearchScreen`, `FeedScreen`, `SampleDetailScreen` and `SearchResult`. Keep both.

### M-3: WP03's skeleton wins, but needs porting

WP04 never touched `AppSkeleton.kt`, so WP03's version wins without a conflict. That is correct, but it still needs
M-1's port.

---

## 6. Best practice

### 6.1 What the references do

**Wear Compose Material 3, `Modifier.placeholderShimmer` (androidx, Google):**
- A "45 degree gradient from Top|Left of the screen to Bottom|Right", coordinated through the animation frame clock
  "so that every component will shimmer as the gradient progresses across the screen".
- Implementation in `Placeholder.kt`:
  - A `Modifier.Node` (`DrawModifierNode` + `GlobalPositionAwareModifierNode` + `CompositionLocalConsumerModifierNode`).
  - The gradient is offset by the bone's `positionInRoot()` from `onGloballyPositioned`. It is about 2.8× the
    screen's larger side, with stops at 0.1, 0.65 and 0.9.
  - An 800 ms sweep (`DurationExtraLong2`) and a 2 × `ExtraLong4` gap, eased with `CubicBezier(0.3, 0, 0.7, 1)`.
  - Skipped entirely with reduce motion.
  - It allocates a `Brush` per frame, which we can avoid.

**compose-shimmer (valentinilk):**
- `ShimmerBounds.View` is the default and gives each view its own speed, which is exactly S-1. For several views, the
  library uses `Window` or `Custom` bounds.
- Defaults: 800 ms sweep, 1,500 ms delay, linear, 15°, 400 dp wide, white at alpha 0.25 → 1 → 0.25.
- A `Modifier.Node` that reads `unclippedBoundsInWindow()` in `onGloballyPositioned`, and reuses one `ShaderBrush` with
  a new matrix each frame.
- It masks with `BlendMode.DstIn` inside a `saveLayer`. **Do not copy that:** our bones have no content to mask, so one
  gradient fill is enough.

**Facebook Shimmer (Android):**
- One `ShimmerFrameLayout` wraps the whole placeholder layout, so one band crosses every child.
- Defaults: 1,000 ms, no repeat delay, 20° tilt, base alpha 0.3, highlight alpha 1, band as wide as the container.
- `ShimmerDrawable` rotates and translates one `LinearGradient` with `setLocalMatrix` each frame, then draws a single
  rectangle.

**Apple:**
- `redacted(reason: .placeholder)` masks text and images "maintaining their original size and shape", and does not
  animate.
- The HIG's Loading page asks for placeholder content that is replaced as the content loads. It does not ask for a
  shimmer.

### 6.2 Values for the template (tokens, per style and theme)

| Token | Value | Why |
|---|---|---|
| Coordinate space | root (window); one band crosses every bone on screen | Wear Material 3, compose-shimmer `Window`, Facebook's single container |
| `BandWidth` | 240 dp, soft-edged (bone → highlight at 0.5 → bone) | about 0.6 of a phone's width; the references use 1× the container (Facebook) to 400 dp (compose-shimmer) |
| `Tilt` | 20° | Facebook's default; a light "wave" down the list instead of a vertical bar |
| `Sweep` | 1,000 ms, `CubicBezier(0.3, 0, 0.7, 1)` | Wear Material 3's easing; references use 800–1,000 ms |
| `Pause` | 800 ms with **no frames** (the clock `delay()`s) | compose-shimmer 1.5 s, Wear 2 s; cuts the frames a loading screen draws by about 45 % |
| `skeleton` | at least 7 ΔL\* from `surface`, `surfaceGrouped` and `surfaceElevated` | S-3 |
| `skeletonHighlight` | bone + 5 to 7 ΔL\*, in light and in dark | S-2: today it is +4.3 and +13.7 |
| Reduce motion | static bones | Wear Material 3, Apple |
| Hidden | the clock stops when no bone is visible (hidden tab, covered screen, activity stopped) | already the intent of the WP03 KDoc |
| Direction | follows the layout direction | S-8 |

### 6.3 Shape of the Compose implementation (`wp/WP03-skel`)

This sketch is plain Compose code. It also compiles in `commonMain` if the Android shader-matrix step is replaced by
the canvas translate.

```kotlin
fun Modifier.skeleton(shape: Shape): Modifier = this then SkeletonElement(shape)

private class SkeletonNode(var shape: Shape) : Modifier.Node(), LayoutModifierNode, DrawModifierNode,
    GlobalPositionAwareModifierNode, CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private var coordinates: LayoutCoordinates? = null
    override fun MeasureScope.measure(m: Measurable, c: Constraints) =
        m.measure(c).let { p -> layout(p.width, p.height) { p.placeWithLayer(0, 0) } } // one op re-recorded per frame
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) { this.coordinates = coordinates }
    override fun onAttach() = onObservedReadsChanged()
    override fun onObservedReadsChanged() = observeReads {
        val on = currentValueOf(LocalTabVisible).isVisible && !currentValueOf(LocalReduceMotion).value
        clock.setUser(this, on)                                        // attach/detach, no recomposition
    }
    override fun ContentDrawScope.draw() {
        val band = clock.bandX                                         // NaN while resting: plain bone
        val origin = coordinates?.positionInRoot() ?: Offset.Zero
        // gradient bone→highlight→bone, CLAMP, tilted; shifted so its centre sits at band - origin.x (+ y·tan(tilt))
        drawOutline(outline, brushAt(band - origin.x - origin.y * TanTilt))
    }
}
```

- `ShimmerClock` stays one per window, held by the outermost `AppTheme`.
- Its loop: if there are users, write `bandX` on each frame for `Sweep`, then set it to NaN once and `delay(Pause)`.
- `bandX` runs from `-BandWidth` to `rootWidth + BandWidth + rootHeight·tan(Tilt)`, and is read only in draw.

### 6.4 Tests that would have caught these

- **Coherence (pixels):** Robolectric with native graphics and the test's main clock. Stack an 80 dp bone and a 300 dp
  bone at the same x. At five clock times, the brightest column falls at the same x in both (±1 px, after removing the
  tilt).
- **Recomposition:** a six-row skeleton list running for 3 s executes 0 composables after frame 1. Add it to
  `RecompositionJourneysTest`.
- **Idle:** with the holder false, with reduce motion, or during the pause, the clock writes nothing (count snapshot
  writes).
- **Palette:** the ΔL\* bounds of S-2 and S-3.
- **Layout:** for every loading state, the first bone starts at or below `LocalTopChrome`, and the last row can scroll
  above the bar and the navigation bar. At font 1.5, the placeholder row's height equals the loaded row's (this holds by
  construction with K-1's redaction).

---

## 7. Parity

### Compose Multiplatform (`wp/WP07`, `templates/multiplatform`)

**`AppSkeleton.kt:20-58`:**
- Still grey bars with no shimmer. The KDoc gives the reason: content that arrives in a few hundred milliseconds does
  not need an animation that redraws every frame.
- There are only `AppSkeletonLine` and `AppSkeletonRows`, an API different from Android's.
- `LineHeight = 14.dp` is fixed in dp.

**Shape mismatch:**
- The rows are bare lines with an `lg` inset, while the content is cards with a `gutter` inset
  (`feature/sample/.../SampleRow.kt`). That is K-1 again.
- The skeleton sits in a `Column` while the loaded content is a `LazyColumn` (`SampleListScreen.kt:56-97`). The header
  is rebuilt and moves when the data arrives.

**Clipping:**
- The loaded list's `contentPadding` is the status bar only (`:63`).
- The interim shell stacks the tab bar under the content in a `Column` (`core/navigation/.../AppShell.kt:35-45`), which
  is REQUIREMENTS 17's strip in Multiplatform.

**Fix (WP07-B):**
- Port §6.3 as is. `positionInRoot`, the frame clock and `drawOutline` are all common code.
- Port the redaction API, and WP04's chrome contract with C-3's fix.
- Add the `skeletonHighlight` token.

### SwiftUI (`wp/WP08`, `templates/ios`)

**What `8e1485d` changed** ("Sweep the skeleton's highlight over it instead of masking to it"):
- The highlight used to be masked to a second, redacted copy of the content. It is now an overlay clipped only to the
  skeleton's frame (`AppSkeleton.swift:26-30`, `:38-54`).
- The reason given is sound: a `List`, which UIKit draws, is not a dependable source for a SwiftUI mask.
- The replacement is wrong, in four ways:
  1. The band now lights the page and the gaps between rows, not just the bones.
  2. Its colour is `theme.colors.surface.opacity(0.7)` (`:42`). In dark, the surface (`#141922`) is darker than the
     bones (`#1D232E`), so the "highlight" darkens the bones while it lightens the page (`#0B0E14`): the band is
     inverted and inconsistent.
  3. The band is 0.6 of the container's width (`:40`), with a linear 1.4 s sweep and no pause (`:49`).
  4. It fades to `.clear`.

**What is right:**
- One band per screen.
- The real row filled with placeholder data and `.redacted(reason: .placeholder)`, so the shapes match the content
  (`SampleListView.swift:84-96`, `SampleDetailView.swift:52-58`).
- Off with Reduce Motion, stopped in `onDisappear`, and one "Loading" label.

**Fix (WP08):**
- **Preferred (Apple's way):** static redacted placeholders, optionally with one synchronised pulse of the bones'
  foreground colour. That is one animation, bones only, with no mask.
- **If parity requires a band:** apply it per text bone with `.visualEffect { content, proxy in
  content.colorEffect(ShaderLibrary.shimmer(.float2(proxy.frame(in: .global).origin), .float(phase))) }` when
  `redactionReasons` contains `.placeholder`. This uses global coordinates, touches bones only and needs no mask. Both
  APIs exist on iOS 17, the template's minimum.
- Add a `skeletonHighlight` role to the Swift palette, and use §6.2's values.

---

## 8. Routing

| Owner | Items |
|---|---|
| WP03 (`wp/WP03-skel`) | S-1 to S-11, K-1 (the API), K-2, C-2, AppPagingList/AppStateView in C-1, C-4, the M-1 port, the §6.4 tests |
| WP03 and the tokens (`tokens/palette.json`, `motion.json`, `genkit.tokens`) | S-2, S-3 |
| WP04 | C-3 (the contract and the "no chrome" holders), C-6, C-9 |
| WP02-M3 | the feature screens of C-1, K-1 (screens move to redaction), K-3 |
| WP02-M2 | the scaffolder snippets (C-1, K-1) |
| WP06 | C-8 |
| WP07-B | the Multiplatform parity in §7 |
| WP08 | the SwiftUI parity in §7 |
| Merge of WP03 | M-1 to M-3: keep WP04's chrome padding in the feature screens, and port WP03's skeleton to `TabVisibility` |

---

## Sources

- Wear Compose Material 3 `placeholderShimmer` reference:
  https://composables.com/docs/androidx.wear.compose/compose-material3/1.5.0-beta05/modifiers/placeholderShimmer
- Its source:
  https://raw.githubusercontent.com/androidx/androidx/androidx-main/wear/compose/compose-material3/src/main/java/androidx/wear/compose/material3/Placeholder.kt
- compose-shimmer README: https://github.com/valentinilk/compose-shimmer
- compose-shimmer theme defaults:
  https://raw.githubusercontent.com/valentinilk/compose-shimmer/master/shimmer/src/commonMain/kotlin/com/valentinilk/shimmer/ShimmerTheme.kt
- compose-shimmer node:
  https://raw.githubusercontent.com/valentinilk/compose-shimmer/master/shimmer/src/commonMain/kotlin/com/valentinilk/shimmer/ShimmerModifier.kt
- compose-shimmer effect:
  https://raw.githubusercontent.com/valentinilk/compose-shimmer/master/shimmer/src/commonMain/kotlin/com/valentinilk/shimmer/ShimmerEffect.kt
- Facebook Shimmer defaults:
  https://raw.githubusercontent.com/facebookarchive/shimmer-android/main/shimmer/src/main/java/com/facebook/shimmer/Shimmer.java
- Facebook Shimmer drawable:
  https://raw.githubusercontent.com/facebookarchive/shimmer-android/main/shimmer/src/main/java/com/facebook/shimmer/ShimmerDrawable.java
- SwiftUI `redacted(reason:)`: https://developer.apple.com/documentation/swiftui/view/redacted(reason:)
- SwiftUI `RedactionReasons.placeholder`: https://developer.apple.com/documentation/swiftui/redactionreasons/placeholder
- Apple HIG, Loading: https://developer.apple.com/design/human-interface-guidelines/loading
- SwiftUI `visualEffect(_:)`: https://developer.apple.com/documentation/swiftui/view/visualeffect(_:)
- AOSP HWUI gradient flags:
  https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/libs/hwui/jni/Shader.cpp
- Skia linear gradient example: https://skia.googlesource.com/skia/+/main/docs/examples/GradientShader_MakeLinear.cpp
- `LazyListState` (`firstVisibleItemIndex` excludes the content padding region):
  https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState
- The edge-to-edge skill, rule at line 341 (insets go to `contentPadding`, never to the parent):
  `~/.claude/plugins/marketplaces/android-skills/system/edge-to-edge/SKILL.md`
