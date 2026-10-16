# Sheet spec verdict, judge 3 (adoption, adaptive, accessibility, parity)

Written 2026-10-13 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). The viewpoint is a staff engineer who runs a
million-user app on this template. The criteria are customisability, adaptive behaviour, accessibility,
cross-platform parity, and what it costs teams to adopt and maintain the sheet.

Read in full: `design-feel.md` (1690 lines), `design-api.md` (1745), `design-perf.md` (1115), `current-state.md` and
`research.md`. No repository or worktree was changed, and no Gradle or adb was run. Library facts that decide a
graft or a reject were re-checked in the sources jars in `~/.gradle/caches/modules-2/files-2.1/`. Each check is
listed in §5.

## 1. The finding that decides the base

Only `design-feel.md` is a complete spec.

- **`design-api.md` stops at §12** (catalog). It points to sections it never writes: §13 for tests, screenshots and
  GPU budgets, §14 for Compose Multiplatform, §15 for SwiftUI and §16 for docs and agent rules. See its §5.7 ("§13.3"),
  §8, §9.6 ("§13.5"), §11 ("§15", "§16") and §0 ("§14 and §15 give the Compose Multiplatform and SwiftUI parity").
- **`design-perf.md` stops at §8** (accessibility). Its §9 to §16 are cited and never written:
  - §9.1, the "sheet layers", which are the core of its host architecture (decision 1);
  - §10.3, the clock;
  - §11, the file layout;
  - §12, the tokens;
  - §15, the tests;
  - §16, the gates.
- **REQUIREMENTS 33 requires a "Work split" section** that WP03, WP04, WP05, WP07-B, WP08, QA2 and WP12 each deliver
  (`REQUIREMENTS.md:136`). Only `design-feel.md` §17 has one. It is also the only spec with:
  - phone gates in `gates.toml` form (§14.1; I checked the rows it cites, `gates.toml:47-53`, `:139-142` and
    `:204-206`);
  - feel acceptance checks to read off recorded strips (§14.3);
  - Multiplatform parity (§15) and SwiftUI parity (§16);
  - a list of what to measure before relying on it, with an owner for each item (§18).

`design-api.md` has the best **public API**. A public API is the hardest thing to change once generated apps depend
on it. So the verdict is: **`design-feel.md` is the base, and its §3 public API is reshaped to `design-api.md` §2**
(graft G1). Behaviour, motion, visuals, tests, gates, parity and the work split stay Feel's, with the corrections
below.

## 2. Scores

The weighting is mine:

| Criterion | Weight |
|---|---|
| Feel | 10 % |
| Customisability | 15 % |
| API quality | 15 % |
| Performance | 10 % |
| Robustness | 15 % |
| Adaptive and accessibility | 15 % |
| Fit with the architecture | 10 % |
| Parity | 10 % |

Performance and feel get less weight here only because all three specs share the same core mechanics:

- the offset is read in placement, the scrim is a drawn rect, and the content is measured once;
- the gesture runs on foundation's `AnchoredDraggableState`;
- every sheet motion uses the `sheet` spring at 0.34 s with no bounce;
- the dismiss rules are iOS's 2 H/s and 0.52, and the over-pull past Large is 0.008 H;
- predictive back is the MDC bottom container with eased progress and a content counter-scale.

Among the specs, the scores differ more on the other criteria.

| Spec | Feel | Custom. | API | Perf | Robust. | Adapt./a11y | Arch. fit | Parity | **Total** |
|---|---|---|---|---|---|---|---|---|---|
| `design-feel.md` | 9 | 8 | 7 | 8 | 8 | 7.5 | 8 | 8 | **7.9** |
| `design-api.md` | 7 | 9 | 9 | 7.5 | 7 | 8.5 | 8.5 | 3 | **7.6** |
| `design-perf.md` | 6 | 6 | 6.5 | 9 | 7 | 8 | 5.5 | 2 | **6.4** |

### `design-feel.md`, strengths

- **The release rule is the best of the three** (§4.3). A flick of 400 dp/s or more moves one detent; dismissal needs
  2 H/s or 52 % of the travel.
  - Under the other two rules, a 1000 dp/s flick up from Medium on the Redmi does not reach Large. The projection is
    436 dp − 99 dp = 337 dp, which is still nearest Medium.
  - Only a flick of about 1745 dp/s (2 H/s) forces a move, so intent is ignored [D].
- **The rubber band is tuned for each top detent** (§4.4).
- **One dim, merged into the card overlay** (§5.5). On iOS the 10 % overlay *is* the dim at Large, and Feel models
  that.
- **Card-stack presenters per level** (§3.7). A sheet over a cover makes the cover recede, not the shell.
- **The interruption matrix** (§2.2).
- **A uniform round-rect outline** (§6.1).
- **A cut-out shadow** (§6.7).
- **Thorough tests** (§13).

### `design-feel.md`, weaknesses

- **`Modifier.sheetBleed()` is an adoption tax** (rejected as X2).
- **Seventeen flat parameters** on `AppSheet`.
- **`SheetDetent.Custom` is missing `equals`** in its code, although the KDoc says equality uses `id`.
- **Placement reads `AppTheme.containerClass`** (§7). That is wrong inside the overlay host (G5).
- **No dim over a glass sample** (G8).
- **A mouse wheel would resize the sheet**, and nothing says how that is avoided (G15).
- **The caption bar is ignored** (G14).
- **`NavMetadataKey` is marked [U] for 1.1.x.** It exists (§5).
- **Destination sheets cannot set a header or footer, or take an inner Back, from their own screen** (G3).

### `design-api.md`, strengths

- **API quality.**
  - Acceptance examples (§1).
  - `AppSheetProperties` and `AppSheetStyle` instead of a flat parameter list.
  - A slot for the grabber.
  - Content-side providers.
  - Rules B-1 to B-46, numbered so tests and reviews can cite them.
  - A "Not in the API" table (§2.12).
- **Architecture fit.**
  - The `SheetBackdrop` seam (§2.11): the design system compiles without the glass package.
  - The host builds its own strategy, so `AppShell(sheetStrategy)` goes and N2 cannot recur.
  - `SheetSessions` is removed by a decorator's `onPop`.
- **The window and container class split** (§3.4.1).

### `design-api.md`, weaknesses

- **Parity is missing** (§1 above).
- **The release rule ignores intent between detents** (Feel strengths, first item).
- **Concentric bottom corners need a path outline** on API 26-32 (X3).
- **Two full-screen blends during the card stack**: the scrim times (1 − f), plus the overlay (§4.4).
- **In-place non-modal sheets cannot be glass** (X8).
- **The card-stack stage drives only the shell.** A sheet over a cover leaves the cover upright (§2.10: "only the
  bottom-most modal sheet").
- **The sheet pads its content box** (X9).

### `design-perf.md`

It has the most rigorous cost contract (§1, §2) and verified research:

- the ordering of `navigationevent` handlers;
- `fitInside` re-measures its child on every frame.

Against it:

- **The host architecture (§9), tests, tokens, gates, file layout and parity are all missing.**
- **Floating is off by default** (X1).
- **The confirm hook cannot work for a destination sheet** (X4).
- **The dim stays at full strength at Large** (X5).
- **The pop happens after the slide** (X6).
- **A non-modal sheet in `Scrolls` mode cannot reach its last row at a partial detent.** §4.6 leaves that part of the
  content below the window, and `Automatic` makes every non-modal sheet `Scrolls` (§3.3). The Maps-style sheet is
  therefore broken. Feel §4.6 adds `largeBottom − settledVisibleBottom` to the padding at each settle, which fixes
  it.

## 3. Grafts into the winner

### From `design-api.md`

- **G1 Public API shape** (API §1, §2.1-§2.7).
  - Replace Feel §3.3-§3.4 with `AppSheet(visible, onDismissRequest, modifier, state = rememberAppSheetState(),
    modality, dismissible, properties: AppSheetProperties, style: AppSheetStyle, grabber slot, header, footer,
    content)`, plus the `state` overload.
  - Keep Feel's `AppSheet(item)` overload: it is SwiftUI's `sheet(item:)` and follows the HIG's one sheet at a time.
  - Placement, content interaction, transition, keyboard and `scrimTapDismisses` go into `AppSheetProperties`.
  - Material, colours, corner, inset, max width and shadow go into `AppSheetStyle`, built by
    `AppSheetDefaults.style()`.
  - Rename Feel's `SheetStyle` to `SheetMaterial`.
  - The grabber becomes a nullable slot (`AppSheetDefaults.Grabber`) instead of `SheetGrabber`.
  - API §1's acceptance examples become a review rule: a change that lengthens one of them is wrong.
- **G2 One state per call site** (B-2).
  - `AppSheetState` is a concrete `@Stable` class with `show()`, `hide()` and `isMoving`, held once per call site
    rather than one per opening. A `show()` during a `hide()` turns the sheet around with `lastVelocity`. That
    fixes D1 by construction.
  - Keep Feel's Saver, which saves the detent's identity: a name, a value or a `Custom` id. API's saves an index,
    which breaks when the caller reorders its detents.
- **G3 Content-side providers** (API §2.7).
  - `ProvideSheetHeader`, `ProvideSheetFooter` and `SheetBackHandler`, plus `LocalSheetSurface` carrying the
    scope.
  - `SheetDismissGuard(enabled, onAttempt: (SheetDismissSource) -> Unit)`.
  - A destination screen is composed by Navigation 3 and never sees the `AppSheet` call. Without these, a destination
    sheet cannot pin a footer above the IME, set its header, or step back through its own steps.
  - Risk: the provider must register before the first visible frame, or the header pops in a frame late. The sheet
    is off-screen on its composing frame (B-1), so this is a test, not a redesign.
- **G4 `SheetKeyboard { Lift, Expand }`** in the properties (B-30, B-31). `Lift` is the default and is Feel §4.7's
  behaviour.
- **G5 Placement by class** (API §3.4.1 and §6).
  - Modal sheets resolve placement from the **window** class, in-place sheets from the container class.
  - Feel §7 reads `AppTheme.containerClass`. `HostedOverlay` re-provides the caller's composition locals
    (`wt/WP03/.../component/overlay/AppOverlayHost.kt:88-94`), and those include `LocalContainerClass`
    (`foundation/adaptive/WindowClass.kt:66`). WP03's sheet already decides "form" from it (`AppSheet.kt:134`, `:140`).
  - Result: a modal sheet opened from a Compact list pane on a tablet comes up as a phone bottom sheet across a
    1280 dp window. The same latent bug is in today's code.
- **G6 Non-modal sheets go to the Leading edge automatically** at Expanded width and up (API §6, the same as
  `design-perf.md` §7). This is iOS 27's `presentationPlacement(.leading)`, the Maps panel. Feel offers it only on
  request.
- **G7 Navigation 3 seam** (API §2.9, §9.2).
  - `SheetSessions`, removed by a `NavEntryDecorator`'s `onPop` after `onRemove()`.
  - `onRemove()` checks `isPresented(contentKey)`, because a cover pushed over the sheet must not slide it away.
  - The host builds the strategy, and `AppShell(sheetStrategy)` is deleted (unmerged, so no deprecation).
  - The typed `PresentationKey : NavMetadataKey<Presentation>` is used **in Multiplatform too**. It exists in
    1.1.7 (§5), so drop Feel §15's string-key fallback.
  - `onDismissRequest` runs before the exit (B-3). The pop then happens while the gate is idle, and `onRemove()`
    runs the slide that the gate counts presses during (B-25).
- **G8 `SheetBackdrop` seam** with `opaque` and `dim` lambdas (API §2.11, §9.6).
  - The design system compiles without the glass package.
  - Feel's merged dim is drawn by the presenter, outside the recorded scene. A live glass sheet would otherwise show
    an **undimmed** patch of the screen behind it, beside the dimmed area around it. `design-perf.md` §6.6 makes the
    same point.
- **G9 Per-style tokens.** `sheetInset` and `sheetMaterial` per design style in `tokens/styles.json` (API §4.2).
  Add the API §2.6 rule that no visual value is read anywhere except from `AppSheetStyle`, which makes the later
  move to `Modifier.styleable` mechanical.
- **G10 Detent context and resolution rules.**
  - `SheetDetentContext.heightOf(detent)` (iOS `resolvedValue(in:)`) and `foldBottom`.
  - R-6: detents within 1 px of each other merge. R-8: when every detent is off, use Large.
  - `Custom` with `equals` and `hashCode` over `id` (API §2.4).
- **G11 Citeable rules and exclusions.** Number Feel's behaviour rules B-n, as API §3 does, and adopt API §2.12's
  "Not in the API" table. Add B-20: a persistent sheet decides Back through `isBackEnabled`, so back-to-home still
  plays at a tab root.
- **G12 Accessibility and input details** (API §6, §7).
  - TalkBack's scroll does not grow a Medium sheet; the grabber's Expand action does.
  - The grabber has a hover state (α 0.7) and a focus ring.
  - A form sheet's predictive back scales it about its centre, to 0.90.

### From `design-perf.md`

- **G13 Back handler enablement** (§4.8).
  - The sheet's handler is enabled only when the sheet is the top-most visible sheet, is not covered, is not
    dismissing, and the IME is hidden. The IME flag is written from placement into a field and pushed to
    `isBackEnabled` from an effect.
  - The IME rule closes Feel's [U] ("Back with the keyboard up closes it first"), whatever order the system gives
    the IME window and `PRIORITY_OVERLAY`.
  - Perf's verified `navigationevent` ordering replaces Feel's "shadowed by newer registration". I re-checked it (§5).
- **G14 `largeTop = safeDrawing.top + 10 dp`** (§3.1, §4.7), not `statusBars.top`. A freeform caption bar on ChromeOS
  or desktop windowing would otherwise cover the grabber. A landscape cutout is handled the same way.
- **G15 Mouse wheel** (§3.3, §4.5). A mouse wheel reports `UserInput`, so `Resizes` would grow the sheet on the first
  notch [D]. Use `SheetContentInteraction.Automatic`, which resolves to `Scrolls` for mouse-only input, or an
  equivalent check. Feel §7 asserts that the wheel never resizes the sheet, but gives no mechanism.
- **G16 Value-read contract** (§2, "Where each fast value is read") and the per-phase budget (§1). Add both to Feel
  §5.11 as the reviewer's checklist.
- **G17 IME rules for content** (§4.6).
  - Forbid `fitInside(WindowInsetsRulers.Ime)` and `imePadding()` inside sheet content. `fitInside` measures its
    child in the placement block with `Constraints.fixed`, so it re-measures on every IME frame. I verified this in
    `RulerAlignment.kt` (foundation-layout 1.12.1, the `fitInside` body).
  - Implement `contentPadding` as `PaddingValues` backed by snapshot state and read in measure (androidx
    `ff66ec4e`). It changes at most twice per IME transition and recomposes nothing.
  - WP12 puts this in the docs, and a lint rule enforces it.
- **G18 Stacking cap** (§4.10). Draw at most two sheets behind the front one.
- **G19 Floating clip fallback** (§6.2). The fallback is a draw-phase single-rrect clip on a rewound `Path`, then a
  constant-size outline cached per whole pixel of inset. Use it only if Perfetto shows a `saveLayer` with Feel's
  outline approach.
- **G20 Action sheet on wide windows** (decision 20, §3.7). At Medium width and up, the action sheet is an in-window
  card anchored to `anchor: () -> Rect?`, not `AppMenu`'s `Popup` window. No window is created when it opens, glass can
  sample it, and A4 (the wrong anchor) is fixed.
- **G21 Navigating out of a presented sheet** (decision 7). An ordinary key navigated to from inside a presented sheet
  closes the presented sheets, then pushes on the tab, sequenced by the gate. Without this rule, N5's general case
  is undefined in both other specs.
- **G22 Housekeeping.**
  - Reduce-motion fades use the existing `fades.longMs` and `fades.shortMs` tokens, not a new 150 ms literal (§5).
  - New strings go into `generator/translations/` (§8).
  - The process-death table includes "restored detent now inactive → nearest" (§4.12).

### A correction to the winner itself

- **G23 In-screen sheets must hide when their screen is covered in its own stack.**
  - Feel's `LocalOverlaysVisible` (§4.12) is the tab's visibility AND "nothing presented above". It does not cover a
    push inside the same tab.
  - A Maps-style non-modal sheet in the overlay host would therefore stay drawn over the pushed screen. API's
    in-place hosting avoids this (B-39).
  - Add the owning entry's visibility, for example its lifecycle at least STARTED, combined with the existing
    presented-above check.

## 4. Rejects (do not build)

- **X1 Floating off by default for Solid** (`design-perf.md` decision 15, §6.2). Solid is every shipped design style's
  material, so the default app would never show the floating inset sheet that REQUIREMENTS 33 names
  (`REQUIREMENTS.md:127`). Keep floating on, with an inset per style (0 for Editorial).
- **X2 `Modifier.sheetBleed()`, and measuring content at the floating width** (`design-feel.md` §0 #5, §3.5, §6.2).
  - Every edge-to-edge child and every design-system component would have to apply it.
  - A screen presented as a sheet would show 8 dp of extra gutter at Large.
  - Instead, measure at full width and clip the inset in draw (API §3.4.2, Perf §6.2). The content still never
    reflows.
- **X3 Concentric bottom corners that differ from the top corners, in v1** (`design-api.md` §4.6, §5.1). A RoundRect
  with unequal radii becomes a path outline, and the clip runs on the canvas below API 33 (§5). Use Feel's uniform
  round rect, with its bottom corners pushed off-screen at Large (§6.1, as Perf §6.1 also does). minSdk is 26.
- **X4 A call-site `onDismissAttempt` and static `SheetDismissal(byDrag, byScrimTap, byBack)` flags as the confirm
  mechanism** (`design-perf.md` §3.3-§3.4, §4.4).
  - A destination sheet's options are static metadata, so a dirty form there cannot guard itself.
  - The five switches are noise: only the scrim-tap switch has a real use (API §2.12).
  - Use `dismissible` plus `SheetDismissGuard`.
- **X5 `design-perf.md`'s dim formula** `1 − (1 − a·pf)(1 − 0.10·lf)` (§6.4). It keeps the full scrim on the receded
  presenter at Large, darker than iOS. Use Feel's lerp from the scrim to the card overlay (§5.5).
- **X6 Pop after the slide for destination sheets** (`design-perf.md` §4.8). It inverts Navigation 3's `OverlayScene`
  contract, under which `onRemove()` animates after the pop, and it keeps the entry in the stack for about 0.5 s. Use
  API B-3 and B-25.
- **X7 `design-perf.md`'s "sheet layers"** (decision 1). Moving in-screen sheets out of WP03's overlay host into new
  per-level layers is a rewrite that crosses packages, and §9, which would define it, was never written. Feel's
  `LocalOverlaysVisible`, with G23, fixes the same bugs inside the existing hosts.
- **X8 In-place (layer 1) hosting for non-modal and `ModalAbove` sheets** (`design-api.md` decision 1, B-27 to B-29).
  - They sit inside the recorded glass scene, so they can never be Glass. That rules out the iOS 26 floating glass
    Maps card.
  - They draw under the floating tab bar.
  - A `ModalAbove` sheet at Large dims its screen but leaves the bar bright and tappable.
- **X9 Padding the sheet's content box** (`design-api.md` §2.2 KDoc, §3.4.5). REQUIREMENTS 34 says insets go in
  `contentPadding`, never parent padding, and REQUIREMENTS 17 says only scroll content is padded. Use Feel and Perf's
  `SheetContentScope.contentPadding` with `SheetColumn`, plus a lint or docs rule.
- **X10 Zoom-from-source in v1** (`design-feel.md` §3.5 `SheetSource`, §5.9) **and the morph on a size-class
  change** (§4.13).
  - It needs a source registry, a morph composed with the offset, M26's glass morph, and its own tests and gates.
    REQUIREMENTS 33 does not list it.
  - Ship it in v1.1 behind API's sealed `SheetTransition`, with Feel §5.9 as its design.
  - In v1, a placement change snaps to the new frame at the same detent (Perf §4.12).
- **X11 Feel's form-sheet deviation** (§0 #17: slide up, swipe to dismiss). Keep ADAPTIVE-PLAN:434 (scale-in on
  `dialog`, no drag), which both other specs follow. Changing an approved plan needs a checkpoint decision.
- **X12 `SheetBackBehavior.StepDown` as a public option** (`design-perf.md` §3.3). research.md §3 lists it as an M3
  pitfall: one Back should close the sheet. Persistent sheets already step down (B-20).
- **X13 Stacked sheets "0.02 H higher"** (`design-api.md` B-36). On the Redmi it puts the grabber in the status bar
  and over the punch-hole camera. Keep Feel's same top with the covered sheet lifted, or Perf's peek.
- **X14 Extra Back presses during an in-screen sheet's exit pop the screen beneath** (`design-perf.md` §4.8). Keep
  consume-and-drop (WP03's tested behaviour, Feel and API): one gesture never pops what is beneath the sheet.
- **X15 `@JvmInline value class Fraction`** in a public sealed hierarchy (`design-perf.md` §3.1). It is boxed every
  time it is used as `SheetDetent`. Use a `data class`.
- **X16 Nav results in the sheet deliverable** (`design-api.md` §2.9 "Results", `NavResultKey`). It is a
  navigation-wide feature. Split it into its own WP04 item; it is not part of the sheet.
- **X17 A sheet-only dim token** (`design-feel.md` §0 #23, 0.20 and 0.48 [U]). Keep `colors.scrim` until QA2 compares
  it with an iOS recording. Then change the token's values, not the mechanism.

## 5. Checks run for this verdict

| Claim | Where it was used | Result |
|---|---|---|
| `NavMetadataKey` exists in Navigation 3 1.1.7, the base of JetBrains 1.1.2 | Feel §15 marks it [U]; API §2.9 says it exists | `navigation3-runtime-android-1.1.7-sources.jar` `NavMetadataKey.kt` declares `public interface NavMetadataKey<T : Any>`. **API is right** (G7) |
| Overlay-priority handlers are scanned before default ones across child dispatchers, last in first out; a handler is off when an ancestor dispatcher is off | Perf §4.8 | navigationevent-android 1.1.2: `NavigationEventProcessor.kt:272` (`overlayHandlers.addFirst`), `:579` (overlay handlers checked before default ones); `NavigationEventDispatcher.kt:169-170` (ancestor check), `:194-195` (shared processor); `NavigationEventHandler.kt:126`. **Correct** (G13) |
| `fitInside` re-measures its child on every ruler change | Perf decision 12, §4.6 | foundation-layout 1.12.1 `RulerAlignment.kt:48`: `measurable.measure(Constraints.fixed(...))` inside the `layout(...) { }` placement block. **Correct** (G17) |
| A RoundRect with unequal radii becomes a path that cannot be an outline clip before API 33 | Feel §6.1, Perf §6.1; API §5.1 has a fallback | ui 1.12.1 `OutlineResolver.android.kt:263` (`isSimple` → `setRoundRect`), otherwise a path with `usePathForClip = !cachedOutline.canClip()` (`:288`). **Correct** (X3) |
| `@FrequentlyChangingValue` on a function (Feel `SheetStack.recede(level)`) | Feel §3.7 | runtime-annotation 1.12.1 `FrequentlyChangingValue.kt:38`: `@Target(FUNCTION, PROPERTY_GETTER)`. **Valid** |
| `dropShadow` block form, `hideFromAccessibility`, `preferredFrameRate(FrameRateCategory)`, `withMotionFrameOfReferencePlacement` | all three | ui 1.12.1: `draw/Shadow.kt:154`, `semantics/SemanticsProperties.kt:1075`, `FrameRate.kt:71`, `layout/Placeable.kt:501`. **All exist** |
| The overlay host re-provides the caller's locals, including the container class | G5 | `wt/WP03/.../overlay/AppOverlayHost.kt:88-94`; `foundation/adaptive/WindowClass.kt:66`; `AppSheet.kt:134`, `:140` |
| `gates.toml` and `perf.py` rows cited by Feel and Perf | Feel §14.1, Perf §7 | `wt/WP04/templates/android/tools/perf/gates.toml:47-53`, `:139-142`, `:204-206`; `perf.py:50` (`LARGE_SCENARIOS`). **Match** |

Still unverified, and carried into the merged spec's measurement list (Feel §18):

- the 400 dp/s detent-flick threshold;
- the iOS 26 floating inset and the curve from glass to opaque;
- whether a Glass sheet's sampling stays aligned while the presenter recedes;
- whether `preferredFrameRate` stops voting while the sheet is idle;
- whether the `ProvideSheetHeader` registration lands before the first visible frame (G3).
