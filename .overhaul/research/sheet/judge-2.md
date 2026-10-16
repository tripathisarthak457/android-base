# Judge 2: bottom sheet specs (androidx framework view: API, phases, jank, robustness)

Written 2026-10-13 for REQUIREMENTS 33. Read in full: `design-feel.md`, `design-api.md`, `design-perf.md`, plus the inputs
`current-state.md` and `research.md`, `REQUIREMENTS.md`, `decisions/CP1.md` and `CODE-STANDARDS.md` §2-3. No repository or
worktree was changed. No Gradle or adb was run.

Tags: **[C]** project code, `file:line`. **[V]** verified this session in the Compose 1.12.1, navigationevent 1.1.2 or
navigation3-runtime 1.1.7 sources jars from `~/.gradle/caches/modules-2/files-2.1/`. **[D]** derived. **[U]** unverified.

---

## 0. Verdict

**Winner: `design-feel.md`.** It is the only complete spec, and its mechanics are the most careful.

The other two files are **truncated**:

- **`design-api.md`** ends after §12 (line 1745). It cites §13 (tests, GPU budget), §14 (Compose Multiplatform), §15
  (SwiftUI) and §16 (agent docs) at lines 6, 1396, 1446 and 1713-1717, and none of them exists.
- **`design-perf.md`** ends after §8 (line 1115). Its central architectural decision, a "sheet layer" per presentation
  level (decision 1), points to §9.1. Integration (§9.2, §9.5), the opening clock (§10.3), the package layout (§11),
  tokens (§12), tests (§15) and gates (§16) are all referenced, and none of them exists.

REQUIREMENTS 33 asks for Compose Multiplatform and SwiftUI parity, and for a "Work split" section that each package
delivers. Only `design-feel.md` has them (§15-§17). `design-api.md` has the best API surface and `design-perf.md` the best
phase discipline, so both contribute many grafts (§3).

**One defect is in all three specs and must be fixed before anything is built** (§2.1). The sheet's drop shadow is
re-rasterised on the CPU for every new surface size and cached without a bound. A floating sheet changes size on every
drag frame.

## 1. Scores

Weights: performance 0.20, robustness 0.20, api_quality 0.15, feel 0.10, customisability 0.10, adaptive_a11y 0.10,
fit_with_architecture 0.10, parity 0.05. Missing sections lower robustness (no tests or gates), fit (no integration or
work split) and parity.

| Spec | feel | custom | api | perf | robust | a11y/adaptive | fit | parity | **total** |
|---|---|---|---|---|---|---|---|---|---|
| design-feel | 9 | 8 | 7 | 7 | 7.5 | 8.5 | 8 | 8.5 | **7.7** |
| design-api | 6.5 | 9 | 8 | 6.5 | 6.5 | 8 | 7.5 | 2 | **7.0** |
| design-perf | 6 | 7 | 6.5 | 8 | 6.5 | 8 | 5 | 2 | **6.6** |

**design-feel.** Strengths:

- A full state and interruption table (§2.2). D1-D16, A1-A5 and N1-N10 are each mapped to a fix.
- A release rule that responds to intent: a 400 dp/s flick moves one detent, and dismissal needs 2 H/s or 52 % (§4.3).
- A rubber-band asymptote scaled to the room left (§4.4). A floating sheet stretches its top edge, not its bottom.
- One uniform round-rect outline, which is clipped in hardware on every API level. [V] `OutlineResolver.android.kt:263`
  uses `setRoundRect` only when `isSimple`; any other shape takes the path branch at `:282-288`.
- Content measured once at the floating width, plus `sheetBleed`, so nothing is clipped (REQUIREMENTS 34).
- A merged dim, and black strips instead of a full-screen fill.
- The haptic plays on arrival.
- Card-stack and stacking geometry adapted to Android's short status bar.
- Thorough tests, gates and feel acceptance (§13-§14), plus parity (§15-§16) and a work split (§17).

Weaknesses:

- **It claims the surface and shadow never change size per frame (§5.4, §6.7). That is false** for a floating sheet
  moving between two partial detents, because its height is `bottomAnchor − offset`. Section 2.1 gives the consequence.
- `grabber` is an enum, not a slot (against API-5).
- A destination sheet has no way to supply a pinned header or footer: §9.2 renders `AppSheet(…){ content() }` only.
- A cover pushed over a destination sheet is assumed to keep the sheet drawn beneath it (§2.2 of this verdict).
- `SheetStyle.Automatic` turns live glass on by default.
- Zoom-from-source is in v1.
- In dark mode the dim lerps between colours.
- The claim that the mouse wheel never resizes the sheet has no mechanism behind it (§2.4 of this verdict).

**design-api.** Strengths:

- The cleanest API surface: twelve parameters, and grabber, header and footer as slots.
- `ProvideSheetHeader`, `ProvideSheetFooter`, `SheetDismissGuard` and `SheetBackHandler`, so a destination's screen can
  customise its own sheet.
- `SheetTransition` is sealed, so Zoom can be added later without breaking callers.
- The glass seam `SheetBackdrop`, so the design system compiles with liquid glass off.
- The presentation host owns its sheet strategy, which removes N2 for good.
- Numbered B-rules.
- An `onRemove` that does not animate when a cover is pushed over the sheet. This is the only spec that noticed it.
- `SheetSessions` cleanup on `NavEntryDecorator.onPop` is right: [V] `NavEntryDecorator.kt:70-80`,
  `DecoratedNavEntries.kt:205-223`.

Weaknesses:

- A Medium → Large flick needs 2 H/s, about 1745 dp/s on the Redmi, so the sheet feels sluggish.
- The 0.008 H over-pull welds a Medium-only sheet in place.
- A stacked sheet sits 0.02 H higher, about 25 dp on the Redmi, which is under the 33 dp status bar.
- `AppSheetStyle` is a style grab-bag, against CODE-STANDARDS API-7.
- A full-screen black rect sits behind the receded shell.
- Different top and bottom radii force a path clip below API 33.
- `NavResultKey` is scope creep for this work.
- The tests and both parity sections are missing.

**design-perf.** Strengths:

- A budget per phase and a table of where each value is read (§1-§2).
- The rejection of `fitInside`, which is correct: [V] `foundation-layout RulerAlignment.kt:36-50` measures the child with
  fixed constraints inside the placement block, on every IME frame.
- The navigationevent ordering facts check out: [V] `NavigationEventProcessor.kt:272-273`, `:578-579`.
- The rule that the sheet's Back handler is off while the keyboard is up (§2.3).
- `sharePointerInputWithSiblings` for touches that pass through a non-modal sheet: [V] `PointerInputModifierNode.kt:73-79`.
- A mouse-only window resolves to `Scrolls`: [V] `NestedScrollModifier.kt:294`, `Wheel = UserInput`.
- `safeDrawing.top`, which covers a caption bar.
- Glass dimmed over its sample.
- A correct light-mode formula for the merged dim alpha.
- At most two covered sheets are drawn.

Weaknesses:

- The architecture section is missing.
- A destination sheet is popped only after its slide (§3.3 R7).
- A double Back on an in-screen sheet pops the screen beneath it.
- `onDismissAttempt` is a component parameter, so a destination's content cannot reach it.
- Floating is off for Solid, the default style.
- 2 H/s step flicks.
- `Fraction` is a value class that is always boxed.
- `fittingHeight` is `Unspecified` unless a FitContent detent is listed, which gives its own `AppChoiceSheet` a NaN
  detent.
- No parity sections.

## 2. Findings no spec gets right

### 2.1 The size-varying shadow (blocker, all three specs)

**How the shadow pipeline behaves** [V]:

- `ShadowRenderer.drawShadow` rebuilds the shadow whenever `generatedSize != size`
  (`ui-graphics …/shadow/ShadowRenderer.kt:70-79`).
- The rebuild rasterises an Alpha8 bitmap the size of the shape plus its outset, with a CPU `BlurMaskFilter`
  (`DropShadowPainter.kt:217-260`, `Blur.android.kt:23-24`).
- `AndroidShadowContext` keeps one renderer per (shape, size, layout direction, density, shadow) in an unbounded
  `MutableScatterMap` (`AndroidShadowContext.android.kt:53-84`).
- The public `DropShadowPainter(shape, shadow)` constructor uses the `Default` provider, which creates a new renderer on
  every `onDraw` (`DropShadowPainter.kt:76-78`, `:118-122`).
- WP05's `glassSurface`, which all three specs copy, rebuilds its `CacheDrawModifierNode` on every size change
  (`wt/WP05/…/glass/GlassSurface.kt:81`, `:97-121`).

**When a sheet surface changes size:**

- on every frame of a floating drag between two partial detents;
- during over-pull;
- across the attach band;
- on every frame of a non-modal Maps-style drag between Height(96) and Medium.

**The cost** [D]: the specs' "cached per size" shadow then blurs a bitmap of about 1000 × 1200 px on the CPU, on each of
those frames, and keeps one bitmap per size it ever drew.

**Rule for the merged spec:**

1. **Modal sheets cast no shadow** (perf decision 16). The dim separates them.
2. **Non-modal, side and form sheets** draw a nine-slice made from one cached shadow of a small round rect: the corners
   stay fixed and the edges stretch. Alternatively, draw a shadow whose size never changes.
3. **Skip the draw completely when the shadow's alpha is 0.** `onDraw` rasterises even at alpha 0.
4. **The sheet body is its own `SheetSurfaceNode`, not `glassSurface`.** It caches outlines and brushes by radius and
   width, not by height.

**Correction to feel §5.4 and §5.11:** the surface is re-recorded on every drag frame for a floating sheet, not only in
the attach band. That costs D1 (a round rect and a rim), which is cheap. The shadow is what must not follow it.

### 2.2 A cover over a destination sheet (open item for WP04) [D]

- Navigation 3 calls the strategy again only while the last scene is an `OverlayScene` (`current-state.md` §4.2 [V]).
- With `[base, sheet, cover]`, the cover's scene is an ordinary scene, so the sheet's `OverlayScene` is removed. Its
  `onRemove()` runs, and the sheet is not drawn under the cover.
- design-api avoids animating the sheet out (`isPresented`), but the sheet still vanishes when the cover starts its push.
- design-feel §9.2 assumes the sheet stays drawn.

The merged spec must choose one of two options, and test it in `SheetSceneTest`:

1. present a cover over a sheet as an overlay scene, so the sheet stays beneath it;
2. close the sheet first, as the HIG does.

### 2.3 Back with the keyboard up

- [V] navigationevent registers an `OnBackInvokedOverlayInput` at the platform's `PRIORITY_OVERLAY` whenever an enabled
  overlay handler exists (`OnBackInvokedInput.android.kt:38-48`).
- material3 had to lower its priority so the keyboard closes first (androidx `b8dd8af9`, `research.md` §2.4).
- So a sheet's overlay-priority handler can take Back before the IME does. feel and api leave this as [U]. perf's rule (the
  handler is off while the keyboard shows) is needed.

**Amend perf's rule:** read the IME **target**. While the keyboard animates closed, keep a handler that consumes Back.
Otherwise a Back press in that window can fall through to the screen beneath a modal sheet. Whether `ComponentActivity`
installs the overlay input in the template's activity version is [U].

### 2.4 The mouse wheel

[V] `NestedScrollSource.Wheel` is `UserInput` (`ui …/nestedscroll/NestedScrollModifier.kt:294`). In `Resizes` mode, the
first wheel notch would therefore grow the sheet. feel B and api B-13 assert "the wheel never resizes" with nothing that
enforces it. Take perf's rule instead: with mouse-only input, `Automatic` resolves to `Scrolls`.

### 2.5 Strips around the card stack

The black strips (feel and perf) must also cover:

- the presenter's rounded-corner cut-outs;
- the bottom band and side gaps that show while the sheet floats part-way through the expansion.

Otherwise the window background, which is not black, shows through. An alternative is to draw the root background black
only while the shell is receded.

## 3. Grafts into design-feel

### 3.1 From design-api

1. **Grabber as a slot.** `grabber: (@Composable SheetScope.() -> Unit)? = AppSheetDefaults.Grabber` replaces the
   `SheetGrabber` enum (API-5). The automatic hide rules move into the default grabber (api §2.1, §2.7).
2. **Content-side providers** (api §2.7): `ProvideSheetHeader`, `ProvideSheetFooter` and `SheetBackHandler`, through an
   internal sheet-scope local. They close feel's gap: a destination screen gets a pinned footer at partial detents and an
   inner Back.
3. **The `visible` overload takes `state`.** It gets `state: AppSheetState = rememberAppSheetState()`, with one state per
   call site and `LaunchedEffect(visible) { show() / hide() }` under the mutex (api §2.2, B-2). That fixes D1 by
   construction. Keep feel's `AppSheet(item)` as well.
4. **`onRemove()` skips the exit** while the entry is still presented, keyed on `isPresented(contentKey)` (api §2.9). Then
   settle §2.2 above.
5. **The presentation host builds its own strategy.** Remove `AppShell(sheetStrategy)` (api §2.9, §9.2).
6. **The `SheetBackdrop` and `LocalSheetBackdrop` seam** (api §2.11). The design system declares it and WP05 implements it,
   with a `dim: () -> Float` lambda so the sampled scene is dimmed like the presenter (perf §6.6 makes the same point).
   Feel §5.8 omits the dim.
7. **`SheetTransition` as a sealed interface** (api §2.5), so Zoom arrives later without an API-14 break.
8. **In-place hosting for `NonModal` and `ModalAbove` sheets** (api §0 #1, B-27 to B-29, B-39). They park and pop with
   their screen, stay inside their pane in list-detail, and rest above the bar. Only `Modal` sheets go to the overlay
   host.
9. **`SheetDismissSource`** is passed to the guard's `onAttempt`. `AppSheetProperties(scrimTapDismisses)` is kept for
   sheets over a canvas.
10. **`SheetKeyboard.Lift` (default) and `Expand`** (B-30, B-31).
11. **`SheetDetentContext.heightOf(detent)` and `foldBottom`** for custom resolvers. Rules R-6 (merge within 1 px) and R-8
    (fall back to Large) (api §2.4).
12. **Form-sheet predictive back** scales about the centre to 0.90 with the same easing (api §6).
13. **The api §1 call blocks become the acceptance tests for API shape:** a change that makes one of them longer is wrong.
    Number the behaviour rules the same way as api's B-rules.
14. **Per-style tokens in `tokens/styles.json`** (api §4.2): `sheetInset` (Utility 8, Social 8, Editorial 0, Playful 10)
    and `sheetMaterial`. `SheetStyle.Automatic` resolves from `sheetMaterial`.
15. **A TalkBack scroll is not `UserInput`, so it never resizes the sheet.** The grabber's Expand action does (api §7).

### 3.2 From design-perf

16. **The phase budget table (§1) and the read-site contract table (§2)** become feel §5.11, the reviewer checklist. Add
    perf's forbidden list, including `fitInside(Ime)` and `imePadding()` on sheet content (verified above).
17. **Explicit Back-handler enablement** (§4.8): the sheet is top-most in `SheetStack`, not covered, not dismissing, the
    IME target is hidden (with the §2.3 amendment), and a way out exists. Cite the verified processor facts. They resolve
    `current-state.md` §4.5's [U].
18. **The `HostMotion` fun interface** (§4.8). The design system declares it and WP04 provides one per host, so in-screen
    sheets also report motion to their tab's gate, and `NavGuard` holds a push under a rising sheet.
19. **`largeTop = safeDrawing.top + 10 dp`**, and centring within the horizontal safe-drawing insets (§3.1, §4.7).
20. **A mouse-only window resolves to `Scrolls`** (§4.5; §2.4 above).
21. **Non-modal pass-through:** `sharePointerInputWithSiblings()` returns a flag that is not snapshot state, so a boundary
    crossing recomposes nothing (§4.9). Add a test that a tap crosses from the overlay host to the app root [U].
22. **An `Initial`-pass pointer node stops any settle on a finger down** (§4.1). Make it the rule, not feel §4.2's
    "add it if a test fails". It closes D8.
23. **Modal sheets cast no shadow** (decision 16), together with §2.1.
24. **`contentPadding` is `PaddingValues` backed by snapshot state and read in measure**, with at most 2 re-measures per
    IME transition (§3.5, §4.6).
25. **Stacking geometry:** the covering sheet's top is the covered sheet's lifted top plus a 10 dp peek (§4.10). Feel's
    shared top leaves only a 4.4 dp strip, which does not read as a stack. At most two covered sheets are drawn.
26. **Saver encoding:** `large`, `medium`, `fit`, `fraction:<f>`, `height:<dp>`, `custom:<key>`, plus `shown` (§3.2).
27. **`AppActionSheet` at Medium width and up** becomes an in-window card anchored by `anchor: () -> Rect?`, never
    `AppMenu`'s `Popup` window (decision 20, §3.7). That fixes A4 and keeps glass and a single window.
28. **An ordinary key navigated from a presented sheet** closes the presented sheets, then pushes on the tab, sequenced
    by the gate (decision 7). This closes N5 outside `AppSheetNavHost`.
29. **Dim composition:** use one rect with merged alpha `1 − (1 − a·pf)(1 − 0.10·lf)` in light mode, and two rects in
    dark mode (§6.4). That replaces feel's colour lerp toward `#C8C8C8`, which passes through a grey veil mid-expansion.
30. **Reduce-motion fades** use the existing `fades.longMs` and `fades.shortMs` tokens (§5), not a new 150 ms literal.

### 3.3 Decisions to keep from design-feel against the others

- The 400 dp/s detent flick [U, tune on the phone].
- The rubber-band asymptote set by the room left.
- Content measured at the floating width, plus `sheetBleed`.
- `hideFromAccessibility()` beneath a modal sheet, instead of `clearAndSetSemantics` ([V] `SemanticsProperties.kt:1075`).
- The haptic on arrival, played from the settle coroutine.
- `SheetStack` levels for presenters.
- `AppSheetState` as an interface plus a factory (API-8).
- The `onDetentChange` callback for user moves.
- Form sheet: feel slides it up and lets it be swiped away (iPadOS). api and perf scale it in (ADAPTIVE-PLAN:434). Keep
  feel's as the default, mark it **taste**, and decide from QA2's recording.

## 4. Rejects (do not build)

| # | Proposal | Where | Why |
|---|---|---|---|
| R1 | Drop shadows rasterised per size on a sheet surface that resizes, including reusing `glassSurface`'s shadow path for the sheet | all three (feel §6.7, api §5.6, perf §6.5) | §2.1: a CPU blur of a sheet-sized bitmap per drag frame, and an unbounded cache |
| R2 | `SheetStyle.Automatic` = Glass whenever glass ships and the tier allows it | feel §3.3 | Live half-screen sampling by default on a phone already GPU-bound (CP1 §1: push/pop GPU p90 12 ms against 8.33). Automatic follows `sheetMaterial` |
| R3 | Zoom from a source (`SheetSource`, `Modifier.sheetSource`) in v1 | feel §3.5, §5.9 | Not in REQ 33. It adds a second per-frame progress, a bounds registry and a glass morph. Ship it after the core passes its gates, as `SheetTransition.Zoom` |
| R4 | `stack.stackedLift = 0.02 H` | api B-36, §4.2 | Puts the covering sheet at about 25 dp on the Redmi, under the 33 dp status bar and the punch-hole |
| R5 | `overPullRange = 0.008 H` above any top stop | api B-8 | Welds a Medium-only sheet (about 7 dp of stretch) |
| R6 | 2 H/s as the detent-to-detent flick threshold | api B-10, perf §4.3 | Medium → Large then needs about 1745 dp/s, a sluggish feel. Keep 2 H/s for dismissal only |
| R7 | Pop the destination only after the slide (press counted by the gate) | perf §4.8 | Breaks Navigation 3's `onRemove` contract. The dismissed entry stays in the back stack and in saved state for about 0.5 s, and the screen beneath stays STARTED. Pop first and animate in `onRemove` |
| R8 | A double Back on an in-screen sheet closes it, then pops the screen | perf §4.8 | A reflexive double press loses the page. Keep WP03's tested consume-and-drop (`SheetTest.kt:65-79`) |
| R9 | `onDismissAttempt` as a component parameter, and the five-flag `SheetDismissal` | perf §3.3-3.4 | A destination's content cannot reach it. Use `dismissible` + `SheetDismissGuard` + `scrimTapDismisses` |
| R10 | Floating off for Solid by default | perf decision 15 | The default style would never get REQ 33's floating inset. Make it a per-style token |
| R11 | `AppSheetStyle` as a grab-bag class | api §2.6 | CODE-STANDARDS API-7. Use flat `cornerRadius` and `floatingInset`, plus `colors(style)`. Revisit when Compose Styles is stable |
| R12 | `fitInside(WindowInsetsRulers.Ime)` or `imePadding()` on sheet content | the edge-to-edge skill's screen advice | [V] it re-measures the child on every IME frame |
| R13 | `fittingHeight = Unspecified` unless FitContent is listed | perf §3.1 | Its own `AppChoiceSheet` detent becomes NaN. Always expose the measured height |
| R14 | `NavResultKey`, `popWithResult`, `NavResultEffect` in the sheet work | api §2.9 | A general navigation subsystem. It belongs in WP04's backlog, not REQ 33 |
| R15 | A full-screen black rect behind the receded shell | api §4.4 | An extra full-screen fill on a GPU-bound phone. Draw strips (§2.5) |
| R16 | `@JvmInline value class Fraction : SheetDetent` | perf §3.1 | Always boxed through the interface. Use a data class |

## 5. For the merged spec

- **Start from `design-feel.md`.** Apply §3 and the fixes in §2. Delete R1-R16.
- **Ask WP04 to decide §2.2,** and test it.
- **Put §2.1 in the docs (WP12):** "never let a drop shadow follow a size that animates".
- **Ask the authors of design-api and design-perf whether their files were cut off.** The missing sections (api §13-§16;
  perf §9-§16) may hold more grafts. In particular, check perf's per-level "sheet layer" against feel's two-host model
  before closing N7 and N8.
