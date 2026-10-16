# WP03-skel: the shimmer and the WP03-owned clipping fixes (REQUIREMENTS 34)

Branch `wp/WP03-skel`, worktree `wt/WP03-skel`. Written 2026-10-14 by the fix agent of
`scripts/shimmer-fix-from-report.js`. Updated 17:30 after the first fresh-context review (five
blocking findings fixed, see "Review fixes") and again at 19:10 after the second one: its four
blocking findings are fixed (see "Second review fixes"), five of its non-blocking ones too, and
`wp/WP03` is merged in.

WP03: merge wp/WP03-skel into wp/WP03 (git merge wp/WP03-skel) at the start of your next run; the orchestrator verified it

`wp/WP03` had already fast-forwarded to this branch's `b8bdea6` and then gained `9823262` (link
button) and `285275d` (sheet tokens). This branch merged `wp/WP03` at `285275d` in `46ec781`, so
`git merge wp/WP03-skel` is a fast-forward unless WP03 commits first. The only conflict was
`web/frontend/lib/catalogue.json`'s `generator_commit` stamp, resolved by regenerating it
(`cd generator && python3 -m genkit.core.catalogue --write`, then `--check`). If WP03 commits
first and that file conflicts again, resolve it the same way. All checks below ran on the merged
tree.

## Source of truth

`qa/shimmer/REPORT.md` was never written to disk (the harness refused the design agent's write).
Its full text is the `summary` of the `design:report` agent of run `wf_b8570cc1-22c`
(`~/.claude/projects/-home-crazyhusir-Documents-projects-tools/e3f9060d-…/subagents/workflows/wf_b8570cc1-22c/agent-af58bada1c3907e35.jsonl`,
last `StructuredOutput`). The app restart at 18:33 cleared `/tmp`; a copy extracted again at
18:40 is in this session's scratchpad:
`/tmp/claude-1000/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/scratchpad/REPORT-from-transcript.md`.
Section numbers below are the report's.

## What changed

### The shimmer (§2)
- `foundation/draw/Skeleton.kt` (new): `Modifier.skeleton(shape)`, `Modifier.shimmerBounds()`,
  `LocalRedacted`, `ContentUnderBone`, and one `SkeletonNode` (`DrawModifierNode`,
  `CompositionLocalConsumerModifierNode`, `ObserverModifierNode`) in its own `graphicsLayer`. Each
  bone maps its origin into the window root (or the nearest `shimmerBounds`) and draws the one
  tilted band from a cached `SweepBrush` by translating the canvas: no mask, no `clipPath`, no
  `saveLayer`, no `setLocalMatrix`. `Bones.kt`: `OutlineBones` (any shape), `LineBones`,
  `TextBones` (one bone per laid-out line). `Sweep.kt`: `SweepGeometry`, `SweepBrush`, `drawSwept`.
- `foundation/draw/ShimmerClock.kt` (new): one clock per window, made by the outermost `AppTheme`.
  Sweep 1,200 ms eased `CubicBezier(0.3, 0, 0.7, 1)`, then 800 ms with no frames; longer on large
  windows (up to 2×) so the band keeps its phone speed. `progress` is a plain field; each sweep
  frame calls `invalidateDraw` on the joined bones, so nothing recomposes. One frame callback for
  all frames (no per-frame lambda). Bones join only while on screen (`LocalTabVisible`), with motion
  allowed and outside previews; the clock stops when the last bone leaves. `boneCount` says how
  many bones a sweep frame redraws.
- Tokens: `tokens/motion.json` `skeleton` (band 240 dp, tilt 20°, sweep, pause, easing, reference
  travel, max slowdown), emitted to `DesignTokens.Motion` and the Swift `Motion`; per-tone
  `skeleton`/`skeletonHighlight` in `tokens/palette.json` under the report's ΔL* rule (bone ≥ 7 L*
  from every surface, highlight +4.5..6 L*, light highlight ≥ 3 L* from every surface). BrandTint
  mixes the accent into the bone only; the resolver then lifts the band 5 L* from the tinted bone
  and moves the bone off the surfaces until both rules hold (`_skeleton` in
  `generator/genkit/tokens/__init__.py`). `web/frontend/lib/catalogue.json` regenerated.
- `AppRedacted(active, modifier, contentDescription, content)` (new): the real row drawn as bones.
  `AppText` draws one bone per line; `AppIcon`, `AppAvatar`, `AppButton`/`AppIconButton` (not
  `Plain`), `AppChip`, `AppBadge`/`AppStatusPill` and `AppNetworkImage` draw bones in their own
  shape, and what their own bone covers draws none (`ContentUnderBone`). Controls built on
  `Modifier.pressable` take no tap, key, hover or focus while redacted. Controls built any other
  way (switch, checkbox, radio, toggle row, segmented control, sliders, stepper, text fields) are
  not redacted; the KDoc names them and says to keep them out of placeholder content. Toggling
  `active` keeps composition. The KDoc also states, once, the placeholder-data rule and why a
  loading list has a list state of its own.
- `AppSkeleton` keeps its name; `AppSkeletonLine(style)` is as tall as a line of that style at the
  font scale; `AppSkeletonLine(height)` and `AppSkeletonListItem` are deprecated at `WARNING`.
  `LocalShimmer` became the internal `LocalShimmerClock`. `AppTheme`'s reduce-motion state is one
  `derivedStateOf` for the theme's life, so nodes observing it see an override change too.

### Clipping, the WP03 items of §3
- K1/K2: every loading list is the real row redacted, in a list with the loaded list's
  `chromeContentPadding(PaddingValues(gutter))` and a non-scrolling `LazyListState` of its own:
  `SampleListScreen`, `FeedScreen`, `SearchScreen.LoadingResults`, `SearchResult`,
  `SampleDetailScreen`, and `generator/genkit/targets/android/scaffold.py`. The swap is an instant
  `when` branch. The large title reads the loaded list's state, which reports 0 while nothing is
  laid out, so it stays open through the load. Search's `Results` also takes the chrome padding
  (WP04's exact expression) and its row became `ResultCard`, so the first row lands where the
  placeholder's did: past §2.14's "`LoadingResults` only", but needed for K1/K2.
- K7: `AppStateView` takes `contentPadding` (before the `illustration` slot), by default
  `chromeContentPadding(PaddingValues(xl))` inside its scroll, so a full-screen state clears both
  chromes; one inside a box, card, pane or sheet passes its own (the catalog's demos pass
  `PaddingValues(xl)`). `AppPagingList`'s default `contentPadding` is chrome-aware and `modifier`
  reaches its loading, failed and empty states.
- K9: `scrolledFromTop()` follows item 0 through the top padding instead of snapping at index 1.
- K10: `AppScaffold` clears `safeDrawing` horizontally (three-button bar in landscape), not only
  the cutout. Device check pending.
- Catalog: `--es section Loading` opens a measurement fixture (12 redacted rows under a large
  title; Load/Reload lifts the redaction in place on the same list), for the phone measurements of
  §5.4. Its KDoc says an app must not build a loading screen that way. The Feedback page shows a
  redacted card with a toggle.

### Not done
- **K3, WP03's half: Utility's default bar `Docked` → `Floating` in `tokens/styles.json`.** The
  auto-mode permission classifier refused the edit (and the regeneration of `DesignTokens.kt` and
  the website catalogue) as a change to a shared resource. It needs the user's go-ahead. If it is
  made: also `AppDesignStyle.Utility`'s KDoc, the README style table (line 239) and the website's
  preview (`web/frontend/lib/app-theme.ts` lines 252 and 306, WP10) say "docked" today.
- §5.4 phone check: not this step (the workflow's Verify agent / QA2).

## Second review fixes (Oct 14, evening)

| Finding | Outcome | Commit |
|---|---|---|
| 1. BrandTint mixed 8 % of the accent into both skeleton roles; for 33 of the 200 golden accents the light lift fell to 4.28–4.39 L*, so every generated project's `PaletteContrastTest` (4.5..6) failed | **Fixed.** `_resolve_roles` mixes the bone only, then `_skeleton` lifts the band 5 L* in the bone's Oklab hue (bisected to the CIELAB step, rounded to 8 bit) and moves the bone away from the surfaces until it is ≥ 7 L* off each (`surfaceGrouped` included) and, in light, the band ≥ 3 L* off each, as `_legible_on` does for text. Mixing `skeletonHighlight` in a tone is now a token error. `test_a_skeleton_shows_on_every_surface_for_every_accent` runs the three rules over all 200 golden accents × 4 tones × 2 themes (it fails on the 33 with the old resolver). A wider sweep, 288 accents (36 hues × 4 lightnesses × 2 saturations), gave 2,304 palettes, 0 failures, lifts 4.83–5.49 L*. The template accent's light BrandTint band moves one step (`D6DFEE` → `D7E0EF`). | `0fe34b8` |
| 2. `AppRedacted`'s KDoc said every design-system control is built on `pressable` | **Fixed by the KDoc route.** It names what draws bones, gives the `pressable` controls that take no input as examples, names the controls that stay live (switch, checkbox, radio, toggle row, segmented control, slider, range slider, stepper, text fields) and says to keep them out of placeholders; a custom control reads `LocalRedacted`. Making those controls redact themselves is not done. | `69b4654`, `689e23b` |
| 3. `AppStateView.contentPadding` came after the trailing `illustration` slot (API-2, API-14) | **Fixed:** moved before `illustration`. No caller passes these by position. | `7542ce0` |
| 4. Nothing tested a runtime change of `AppTheme(reduceMotion = …)` | **Fixed:** `SkeletonStillnessTest` "reducing motion mid-sweep stills the bone, and allowing it again resumes the sweep": override off, wait for a sweep, turn it on; the clock stops at once and stays stopped for 120 frames, the bone is its plain colour and unchanged between two captures; turning it off starts a new sweep. Run against the old `AppTheme` (a new `State` per override), it fails on its first check, "the sweep went on" (`x1-reduce-motion-test-on-old-theme.log`). | `9d2a011` |

Non-blocking items also done:
- `Skeleton.kt` (463 lines, K-2/K-3) split into `Skeleton.kt` (238), `Bones.kt` (148) and
  `Sweep.kt` (98), each public, internal, private; `LocalRedacted` is a getter so it can stand
  first. `SkeletonNode`'s KDoc says why it keeps auto-invalidation; the cut-off-line comment gives
  its reason (`357814b`).
- The placeholder-data and own-list-state rules are stated once, in `AppRedacted`; the five to
  seven copies in the screens, `AppPagingList` and `scaffold.py` point there.
  `SampleDetailScreen`'s placeholder says why its id is 10: the screen shows `#id`, and 90 of the
  100 sample posts have a two-digit id (`64c74cd`).
- `CatalogLoading`'s KDoc calls the page a measurement fixture (`91986ba`).
- Components that draw their own bone no longer let their content join the clock: `AppButton`,
  `AppIconButton`, `AppChip`, `AppBadge`, `AppStatusPill` and `AppAvatar`'s initials wrap their
  content in `ContentUnderBone`, an always-present provider that turns redaction off inside while
  the component draws its bone. `AppAvatar`'s image slot stays redacted, so a network image does
  not load under the bone. `RedactedBoneCountTest`: one of each of the six is 6 bones, before and
  after a reveal (16 without the change, `x4-bone-count-test-calibration.log`); a plain button with
  an icon plus a plain icon button is 3 (`e2cec54`). Phone measurement of the saving: QA2, §5.4.

## Review fixes (Oct 14, afternoon)

| Finding | Outcome | Commit |
|---|---|---|
| 1. Loading lists shared the loaded list's `LazyListState`, so (the review said) every loaded row would fade in on the swap | **Changed, and the stated mechanism corrected.** On foundation 1.12.1 an instant swap does not fade: the placeholder list's `lazyLayoutItemAnimator` node calls `animator.reset()` in `onDetach` (`LazyLayoutItemAnimatorModifier.kt:54`) before the loaded list measures. A calibration run with one shared state showed the rows whole on the first frame (`v2-swap-calibration.log`, `swap diffs=[14408, 0, 0, 0, 0]`, the 14408 being the frame before the swap). The hazard is real whenever both lists are on screen together, as in a cross-fade: then nothing detaches first, and the loaded rows are compared with the placeholders. Each loading list now has its own state; `AppRedacted`'s KDoc says that, not the wrong mechanism. `LoadingSwapTest` swaps a redacted list for keyed `appAnimateItem` rows with motion on and requires the first frame after the swap to equal the settled frame pixel for pixel. It passes either way on 1.12.1, so it pins the behaviour, not the change. | `9456950` |
| 2. `shouldAutoInvalidate = false` left bones stale after an in-app theme or style switch | **Fixed: `b4008be` reverted.** The `SkeletonStillnessTest` case captures once in light before switching to dark; with the flag it failed (the bone stayed `#D7DADF`, `v3-theme-test-before-revert.log`). Without the flag it passes. | `9bfb214` |
| 3. `AppStateView` always padded by the scaffold's chrome, which cut the catalog's boxed demos | **Fixed:** `contentPadding` parameter, catalog demos pass `PaddingValues(xl)`, `StateViewTest` (280 dp box under an `AppTopBar`: title and action inside, centred within 1 dp). | `d765dc7` |
| 4. `PressableNode`'s redaction had no test | **Fixed:** `RedactedInputTest`: no tap on a redacted `AppCard(onClick)` and `AppButton` until shown; no focus and no Enter while redacted, and focus is lost on redaction; a held card is let go of and springs back to full size, and the lift does not click. | `caad6ca` |
| 5. `Modifier.shimmerBounds()` had no caller and no test | **Kept (report §2.2/§2.3 specify it) and tested:** a `SkeletonCoherenceTest` case checks every pixel of two bones in a 200 × 100 dp area against the band laid out from the area's corner over the area's own travel, and a bone outside the area against the window's band. | `e90a8ee` |

Also from that round: a text bone the box cuts off stops at the edge, `TiltCos`/`TiltSin` sit
beside `SweepGeometry`, `@Stable` dropped from `ShimmerClock`, `TextBones.top(line)`/`bottom(line)`
(`d26669c`); both easings' x control points held to 0..1 through one `_easing` check, and
`test_tokens` covers the skeleton block (`b8bdea6`).

## Open, and who takes it

- **M-1 gate (whoever merges WP03 after WP04).** `SkeletonNode.follow()` handles `LocalTabVisible`
  and is tested, but WP03's shell has no production provider, so a skeleton in a kept tab keeps
  the clock running until the port to WP04's `TabVisibility` (`.isVisible`, read inside
  `observeReads`). Do not ship the WP03-after-WP04 merge without that port.
- **WP06:** `core/devtools/.../DevToolsPanel.kt:143` puts its empty `AppStateView` in a full-screen
  dialog with its own header and `safeDrawingPadding`; with the new default padding it also clears
  the window's tab-bar height, so it sits too high (it scrolls; nothing is cut). Pass
  `contentPadding = PaddingValues(AppTheme.spacing.xl)` there. `RequestList` and `NothingSelected`
  (detail pane, reaches the tab bar) are right with the default.
- **WP04 (at the merge):** `AppScaffold.kt:62` `safeDrawing.only(Horizontal)` also pads a pane's
  inner edge (a two-pane landscape phone with 3-button navigation gets a gap before the divider),
  and `clearDisplayCutout` no longer names what it does. K10's device check is still pending.
- **WP02-M3:** `SearchScreen.kt:105` `AnimatedContent(fadeIn togetherWith fadeOut)` (K5) still
  cross-fades the redacted list over full-screen layers; Search's one-frame swap depends on it.
- **WP08 (at its merge):** `generator/genkit/tokens/swift.py:110-135` (WP08's file; the report
  allowed the edit) now emits the skeleton colours and `Motion.skeleton*`. WP08's checked-in
  `DesignTokens.swift` is stale until `python3 -m genkit.tokens --write` runs on the merged tree;
  the new Swift `UnitCurve.bezier` line is compiled nowhere on this branch.
- **WP12 (release notes):** `AppSkeletonLine()` and `AppSkeletonLine(widthFraction = …)` change
  from a 14 dp line to one line of `body` at the current font scale, with no warning. Say so in
  the upgrade notes; `AppSkeletonLine(height = 14.dp)` (deprecated) keeps the old look.
- **Test strength, open:** `SkeletonDrawAllocationTest` measures the bones and `drawSwept`, not
  `SkeletonNode.draw` (`currentValueOf`, `positionInRoot`, `localPositionOf`, `lineRadius`, brush
  cache). `LoadingSwapTest` passes with a shared state too (see above). `SkeletonScreenshotTest`
  toggles redaction in place on one list with real data, while the screens swap two lists. The
  `AppPagingList` state change and `AppNetworkImage`'s redacted branch have no tests.
- **F-5:** the feature modules borrow `designsystem_loading` for their loading lists' semantics,
  and the loading region has no live region. A design-system helper that owns the announcement
  would fix both.
- **WP03, to confirm:** `9823262` ("Keep a link button on its text column…") also deletes
  `component/overlay/SheetTest.kt` (7 cases), which its message does not mention. If that was not
  meant as part of the sheet rework, restore it.
- The Cool tone's skeleton roles are hex values that fall between ramp steps.
  `web/frontend/lib/catalogue.json` and `genkit/tokens/__init__.py` are outside report §2.14: flag
  them at the WP10 merge.
- Pre-existing, not from this branch: a scaffolded module does not compile. `scaffold.py`'s data
  and feature templates import `core.common.AppResult` and `core.common.mvi.{LoadState,
  MviViewModel, UiState, UiEvent, UiEffect}`, which the template has moved. Owner: the scaffolder
  (WP02-M2).

## Tests and checks (logs in `qa/shimmer/logs/`)

After the second review's fixes, on the tree with `wp/WP03` merged in (`46ec781`):

| Check | Result | Log |
|---|---|---|
| `:core:designsystem:testDebugUnitTest` | 129/129 pass (wp/WP03 removed `SheetTest`, 7 cases, and added one `PressableTest` case) | `x8-tests-lint-compile-merged.log` |
| `verifyComposeUsage` (all modules in the graph) | pass | same |
| `lintDebug`: designsystem, ui, sample, feed, search, catalog | 0 errors; no issue in a changed file | same |
| `:app:compileDevDebugKotlin`, `:catalog:compileDebugKotlin`, `:catalog:compileReleaseKotlin` | pass | same |
| Generator `python3 -m unittest discover -s tests -t .` | 263 pass, 1 skipped | `x7-generator-tests-merged.log` |
| `genkit.tokens --check`, `genkit.core.catalogue --check` | current | — |

The same before the merge (HEAD `e2cec54`): 135/135 unit tests, `verifyComposeUsage` pass, lint 0
errors and no issue in a changed file, the three compiles pass (`x5-tests-lint-compile.log`);
generator 261 pass, 1 skipped, both `--check`s current (`x6-generator-tests.log`).

Calibrations: the reduce-motion case fails on the old `AppTheme` (`x1-…`); `RedactedBoneCountTest`
counts 16 bones with `ContentUnderBone` made a no-op (`x4-…`).

After the first review's fixes (HEAD `b8bdea6`): 132/132 unit tests, `verifyComposeUsage`, lint and
the three compiles pass (`v4-tests-lint-compile.log`); generator 259 pass (`v5-generator-tests.log`).
Before them: 125/125 (`u2-stability-tests-lint.log`); `checkComposeStability` passes `:core:ui` and
fails `:core:designsystem` on `AppMoneyField` only (WP03's, untouched here); the generated lean
project with `orders` compiled the sample feature and the scaffolded loading list (`u4-…`, `u6-…`).

Tests on this branch (all in `:core:designsystem` `src/test`): `SweepGeometryTest`,
`SkeletonCoherenceTest` (Robolectric NATIVE: a line and a circle match a wide reference bone pixel
for pixel at the same phase; the shimmer-area case), `ShimmerClockTest` (first frame plain, eased
0→1, no frame asked for during the pause, stop on last removal, large-window slowdown),
`SkeletonStillnessTest` (reduce motion, reduce motion turned on mid-sweep, hidden tab, preview, no
theme, theme change with motion reduced), `RedactedGeometryTest` (redacted = loaded bounds at font
1.0 and 1.5, light and dark; no glyph pixels; bones inside line boxes for every type role),
`RedactedInputTest`, `RedactedBoneCountTest`, `LoadingSwapTest`, `StateViewTest`,
`SkeletonDrawAllocationTest` (sweep frames allocate no more than plain ones; 1,000-frame warm-up,
least of five rounds, `a469f57`), `SkeletonScreenshotTest` (rows below the title, above 80 dp of
bottom chrome, unmoved by the swap; state view clear of both chromes at 1.5×; PNGs in
`core/designsystem/build/outputs/skeleton/`), `PaletteContrastTest` (three skeleton cases),
`LargeTitleTopBarTest` (K9), `RecompositionJourneysTest` (a sweeping skeleton list recomposes
nothing under two motion styles; hiding and showing it recomposes no row).

## Commits (`git log --no-merges b8bdea6..wp/WP03-skel` for this round)

| Commit | Subject |
|---|---|
| `b292ea8` | Give the skeleton's shimmer its own tokens and colour steps |
| `0a1f203` | Sweep one band across every skeleton in the window |
| `70a8184` | Redact the real row instead of drawing a lookalike |
| `6a87aa2` | Keep state views and lists clear of the chrome |
| `7a29902` | Lay loading lists out as the lists that replace them |
| `b7811e0` | Add a loading screen to the catalog |
| `b236f9a` | Merge wp/WP03 into wp/WP03-skel (`87a3a06`) |
| `4f73ff2` | Give the shimmer's sweep one frame callback for all its frames |
| `a469f57` | Judge a sweep frame's allocations on compiled drawing code |
| `3ba3920` | Wrap the skeleton's lines that ran past the 120-column limit |
| `b4008be` | Invalidate a skeleton's draw only through its own update (reverted by `9bfb214`) |
| `9456950` | Give each loading list a list state of its own |
| `9bfb214` | Let a theme change repaint a skeleton bone |
| `d765dc7` | Let a state view inside a box take a padding of its own |
| `caad6ca` | Test that a redacted control takes no input |
| `e90a8ee` | Test that a shimmer area sweeps its own band |
| `d26669c` | Clip a cut-off line's bone, and keep the sweep's geometry together |
| `2640f72` | Say which controls a redaction stops, and drop a narrating comment |
| `b8bdea6` | Check that an easing's time stays between start and end, and test the shimmer's tokens |
| `0fe34b8` | Lift a tinted bone's band from the tinted bone |
| `69b4654` | Name the controls a redaction stops, and the ones it does not |
| `7542ce0` | Keep a state view's illustration slot last |
| `9d2a011` | Test that reducing motion mid-sweep stills a skeleton |
| `689e23b` | Give the redacted pressable controls as examples, not as the full list |
| `357814b` | Split the skeleton's bones and sweep out of Skeleton.kt |
| `64c74cd` | State the loading-list rules once, in AppRedacted |
| `91986ba` | Call the catalog's loading screen a measurement fixture |
| `e2cec54` | Keep what a component's own bone covers off the shimmer |
| `46ec781` | Merge wp/WP03 into wp/WP03-skel (`285275d`) |

## For whoever merges WP03 after WP04 (report §2.13)
- M-1: `SkeletonNode.follow()` reads `currentValueOf(LocalTabVisible)` as WP03's Boolean; on WP04's
  `TabVisibility` holder it becomes `.isVisible` (still inside `observeReads`). Gate the merge on it.
- M-2: keep WP03's `AppLargeTitleTopBar` and WP04's `chromeContentPadding` together at every
  skeleton call site; the expressions here are WP04's exact ones.
- M-3: this `AppSkeleton.kt` replaces WP04's per-bone `InfiniteTransition` version; never resolve
  toward it.

## Rules the docs must state (for WP12)
- One band per window: bones map into one coordinate space; never size or time a sweep by the
  element. Sweep, then a pause with no frames; nothing sweeps while hidden, reduced or idle.
- Loading states redact the real row with placeholder data (`AppRedacted`), in the loaded list's
  padding, on a list state of their own, and swap in one frame: no crossfade over full-screen layers.
  Two lists never share one `LazyListState`: while both are on screen its item animations compare
  the loaded rows with the placeholders and fade every row in.
- Inside a redaction only text, icons, avatars, images, chips, badges, pills and filled buttons
  become bones, and only `pressable` controls stop taking input. Switches, sliders, steppers,
  segmented controls and text fields stay live: keep them out of placeholder content.
- A component that draws one bone over itself wraps its content in `ContentUnderBone`, so what the
  bone covers does not sweep too.
- A tone mixes the accent into `skeleton` only; the generator derives `skeletonHighlight` from it.
- A component that draws from a static local in a `Modifier.Node` keeps auto-invalidation on: a
  static local's change reaches the node only through it.
- A state view that is not the whole screen passes its own `contentPadding`.
- Scrolling content takes chrome as `contentPadding` (`chromeContentPadding`), never parent padding.
