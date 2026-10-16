# Sheet spec: adversarial critique 1 (performance and Compose correctness)

Written 2026-10-13 against `research/SHEET-SPEC.md` (2774 lines, read in full) and its inputs in `research/sheet/`
(`judge-1..3.md`, `design-perf.md`, `design-feel.md`, `research.md`, `current-state.md`, `critique-2.md` to avoid
duplicates). Lens: anything that recomposes per frame, reads animated state in composition, puts alpha on a
full-screen or backdrop layer, allocates in draw, opens a window on the open path, breaks nested scroll or the IME,
mishandles predictive back or `BackGate`, loses or leaks state across recreation, or conflicts with the presentation
host's layering, kept tabs or the glass tiers; and every constant checked against its source. No repository or
worktree was changed; no Gradle or adb was run.

**Tags.** [C] project code, `file:line`, at the heads read today: WP03 `87a3a06` plus an **uncommitted**
`component/sheet/` implementation of this spec (files dated 2026-10-13 14:26-14:47, cited `wip/…`), WP04 `666024c`,
WP05 `d549b2a`. [V] verified this session in the shipped sources jars under
`~/.gradle/caches/modules-2/files-2.1/` (`ui-android`, `foundation-android`, `runtime-android` 1.12.1;
`navigationevent-android` and `-compose-android` 1.1.2) or in `research/sheet/sources/` (`src/`). [D] derived here.
[U] unverified. Spec lines are `SHEET-SPEC.md:n`.

Prefixes as the spec's, plus: `ui112/` = `androidx/compose/ui/` in ui-android-1.12.1-sources, `fnd112/` =
`androidx/compose/foundation/` in foundation-android-1.12.1-sources, `rt112/` = `androidx/compose/runtime/` in
runtime-android-1.12.1-sources, `ne/` and `nec/` = navigationevent(-compose) 1.1.2 sources, `wip/` =
`wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/sheet/`.

Severity: **blocking** = the spec as written produces per-frame work or state loss in the core path; fix before WP03
commits. **major** = a real journey janks, misbehaves or loses data; or two implementers would build different things.
**minor** = cheaper to fix in the spec now than after generated apps depend on it.

## Summary

| # | Sev | Section | One line |
|---|---|---|---|
| C1-01 | blocking | §6.4, §4.10, B-54 | The presenter's dim draw re-records the whole receding shell every frame (and re-records the glass scene, re-blurring the bar) |
| C1-02 | blocking | §6.3, §7.1, B-45 | The sheet layer re-records every frame even when attached; children are not given layers |
| C1-03 | blocking | B-12, B-62, B-51, §11.1 | `HostedOverlay`'s identity key loses every `rememberSaveable` in sheet content on rotation and process death; its `modal` key rebuilds the sheet at each `upThrough` crossing |
| C1-04 | major | B-51, §21 | `sharePointerInputWithSiblings` cannot reach the app root (verified); the non-modal pass-through fails |
| C1-05 | major | §4.4, §13, B-17, B-37 | Mutex table wrong (foundation drags at `Default`); "leaving" derived from `targetValue` is late, so a flicked sheet can be caught or dismissed twice |
| C1-06 | major | §11.2 | `PresentedSheetFrame` is not counted in `live`: the host stops composing on the last pop, killing the exit and B-43 |
| C1-07 | major | B-28…B-30, §5.5 | Nested scroll through `dispatchRawDelta` skips the rubber band and the mutex; `settle(velocity)` is deprecated |
| C1-08 | major | B-15, D-15 | `NavGuard` silently drops pushes while an in-screen sheet reports motion (stale `lastChange`) |
| C1-09 | major | B-44, B-37 | "Holds either way" is unproven: with the IME up, Back can pop a guarded sheet without its guard, or the screen beneath |
| C1-10 | major | R-3, B-47, R-11 | FitContent re-resolves from the IME-constrained measure: a detent animation loop while the keyboard moves |
| C1-11 | major | B-52, B-45 | Non-modal `restBottom` uses `LocalBottomChrome`, which is the keyboard when it is up; a sheet also lifts for a keyboard it does not own |
| C1-12 | major | B-14, §11.1 | Parking by not placing drops layers, contradicting S4/KeptTabs; the tab-switch frame pays a rebuild |
| C1-13 | major | §6.6, D-17, §11.5 | Glass samples the tab scene even when its presenter is a cover or another sheet |
| C1-14 | major | §6.4, §10.1, §6.6 | The card stack's layer transform is not free in Compose: positioned callbacks re-sample the glass bar every frame |
| C1-15 | major | §4.8 slots, B-2 | `ProvideSheetHeader`/`Footer` content loses the screen's locals: activity-scoped ViewModels, wrong saveable registry |
| C1-16 | major | §3.2, §6.4, B-53 | Sibling sheets of one screen share a level: a persistent sheet stays undimmed above a modal sheet's dim |
| C1-17 | major | B-14, REQ 24/32 | An in-screen non-modal sheet does not follow its owner's back card or push: double exposure mid-gesture |
| C1-18 | major | §16.1, B-1, B-2 | The open path takes 3 frames and composes Large-height content; present gate stricter than push/pop; GPU gates unmeasured |
| C1-19 | major | §6.4, §10.1 | At Large the whole receded presenter is drawn under an opaque sheet; HWUI has no occlusion culling |
| C1-20 | minor | §4.10, §6.4 | `() -> Float` / `() -> Color` lambdas box per draw call; `HashMap<Int, Shape>` boxes above 127 px |
| C1-21 | minor | §4.10, §11.5 | `LocalSheetBackdrop` cannot be provided by `GlassStage`; a tier change would recompose both hosts mid-jank |
| C1-22 | minor | B-63, §7.7, §10.3 | B-63 fades the sheet layer, §7.7.6 says the surface never fades; the code rule would flag B-63 itself |
| C1-23 | minor | B-64, §21 | The always-on frame-rate vote propagates to every child layer; the check cannot see it |
| C1-24 | minor | §6.2, B-22, B-61, §6.1 | `dismissPosition` inverted from Flutter's; the 1 px haptic fires ~0.5 s after release; spring threshold unnamed |
| C1-25 | minor | B-21 | The named press-delay flag does not exist; the 100 ms target equals the platform tap timeout |
| C1-26 | minor | B-17 | A touch that stops a settle and lifts leaves the sheet between stops |
| C1-27 | minor | B-36, B-37 | The Compose `NavigationBackHandler` has no priority and enables through recomposition |
| C1-28 | minor | §11.2 | `SheetSessions` keyed by `contentKey.toString()` |
| C1-29 | minor | §7.9, §11.3 | Status-bar flip and black backing: mechanism unspecified; the template's owner re-runs `enableEdgeToEdge` |
| C1-30 | minor | §4.8 | `LocalSheetSurface` stability unspecified; an unstable holder recomposes all content per settle |
| C1-31 | minor | header, §19.1 | Pinned heads moved; the uncommitted WP03 code already embodies C1-01 and C1-20 and departs from §4.4 |

---

## Blocking

### C1-01 (blocking) The presenter's dim re-records the whole receding subtree every frame

**Problem.** §4.10 and §6.4 put the merged dim "inside the layer, after the content" of `Modifier.cardStackPresenter`
(`SHEET-SPEC.md:1025-1030`, `:1660`, `:1665-1680`) and cost it as L0 + one D2 rect (`:1917`, `:1932`). In Compose, a
draw node invalidated by a snapshot read re-records the display list of its **enclosing layer**, and that re-record
executes `drawContent()` for every descendant down to the next layer boundary. The dim's alpha changes on every frame
of any sheet motion, so every frame re-records the whole shell subtree that has no layer of its own:

- On the glass presets: `GlassStage`'s content box has no layer, and `RecordSceneNode.draw()` calls
  `layer.record { drawContent() }` every time it draws (`glass/SceneBackdrop.kt:157-161` [C]). The whole tab scene is
  re-recorded per frame, and the bar's live sample (`drawLayer(layer)` of that recording) re-renders its blur per frame,
  on a phone CP1 measured as GPU-bound (`decisions/CP1.md:109-111`).
- On every preset: `BottomEdgeFade`'s gradient, the bar, the `Row`/`TabbedNavHost` nodes without layers.
- The same pattern recurs for stacked sheets: "the covered sheet draws the covering sheet's dim inside its own clipped
  layer" (B-54, `:1465`), so the covered sheet's whole surface and inline children are re-recorded per frame.

Evidence that the wording leads there: WP03's uncommitted presenter is exactly
`CardStackLayerElement then CardStackDimElement`, the dim node calling `drawContent(); drawRect(dim)` with no layer
between (`wip/stack/CardStackPresenter.kt:35-36`, `:110-128`).

**Fix.**

1. §4.10/§6.4 fix the node order: `graphicsLayer { recede }` → dim draw node → `graphicsLayer()` with no properties (a
   content layer) → content. A dim change then re-records two ops (`drawLayer` + `drawRect`).
2. Same for the covered sheet (B-54) and anywhere else a per-frame draw value wraps content.
3. §10.3 gains: "a draw node that reads a per-frame value wraps a layer, never inline content".
4. Tests:
   - §15.4: during 30 drag frames, `RecordSceneNode` records 0 times and no shell node's draw lambda runs (a counting
     draw modifier in the fake presenter).
   - §16.1: `sheet_cardstack.gpu_p90_ms` in the `glass_live` column.

### C1-02 (blocking) The sheet layer re-records every frame, attached included

**Problem.** §6.3 defines the surface in **window** coordinates:

- surface bottom `lerp(b₀, H + R, a)`, with `H + R` when attached;
- content clip `(i, y_v, W − i, bottom)` (`:1634-1641`);
- B-45 places the footer at `min(restBottom, surfaceBottom, H − K) − footerHeight` (`:1408-1410`), a window-fixed y
  while the sheet moves.

In the sheet layer's local space all three change with `y` on every frame, attached or not:

- the body height is `H + R − y`;
- the clip bottom is `H + R − y`;
- the footer offset is `restBottom − footerHeight − y`.

So "an attached sheet only moves (L1)" (`:1647-1648`) and the drag row "L1 sheet + D1 surface (floating or above chrome
only)" (`:1912`) are false. Examples: a `[Medium, Large]` sheet under 3-button navigation, any Compact-height sheet,
any drag at `e = 1`.

Worse, a child placed without its own layer is drawn inline. Moving it, or re-recording the parent, re-runs its draw:

- the grabber;
- the header (title text, scroll-edge gradient);
- the footer, whose placement changes every frame.

§3.1 and §4.4 give a layer only to the content (for the counter-scale).

**Fix.**

1. While `fl · (1 − a) = 0`, the body and clip are a **constant** local rect: top 0, bottom = the layer's measured
   height + R. Whatever lies below the window is clipped by the window. An attached move is then a RenderNode
   translation.
2. Rebuild the round rect only while floating (`0 < fl · (1 − a)`).
3. Place the grabber, header and footer with `placeWithLayer`. The footer's per-frame offset becomes a RenderNode
   position (L0). The content keeps its layer.
4. §10.1 says it outright: "attached drag: L1, 0 re-records".
5. A test counts sheet-layer draw executions:
   - 0 over 30 attached drag frames;
   - ≤ 30 over 30 floating frames, with no child re-recorded.

### C1-03 (blocking) `HostedOverlay` loses sheet content state, and rebuilds the sheet at `upThrough`

**Problem.** B-12 hosts every in-screen sheet through `HostedOverlay`. B-62 promises that content state survives
recreation and process death ("Content state: the content's own `rememberSaveable`", `:1508-1511`). Today's host breaks
both promises:

1. **Keys.** `HostedOverlay` wraps each entry in `key(entry)` (`ds/component/overlay/AppOverlayHost.kt:66` [C]).
   `OverlayEntry` is a plain class with identity `hashCode` (`:46`), created anew by each `DisposableEffect` run
   (`:90-98`).
   - Compose mixes `dataKey.hashCode()` into the compound key (`rt112/GapComposer.kt:3085-3097` [V]).
   - `rememberSaveable` keys by that hash.
   - So after rotation, a dark-mode switch or process death, every saveable inside a sheet gets a new key and restores
     nothing: `rememberLazyListState`, a `TextFieldState`, `SheetColumn`'s `rememberScrollState()` (`:857`).
   - A rename sheet loses the typed name on rotation.
2. **`modal` as an effect key.** `DisposableEffect(host, modal)` (`:90`) disposes the entry and adds a new one whenever
   `modal` changes.
   - §11.1 routes "beneath hidden from accessibility" through the hosted overlay's modality.
   - B-51 flips `blocking` at `upThrough`.
   - If `blocking` feeds `modal`, every crossing tears the sheet's composition down and composes it from scratch,
     mid-drag. Its scroll and fields are lost, and the composing frame is a hitch.
   - That is the opposite of B-51's "≤ 1 recomposition per crossing".

Nobody in the inputs caught this (`design-api.md:1226` and `design-perf.md:910` assume it works).

**Fix.**

1. §11.1: `HostedOverlay` keys an entry by the caller's `currentCompositeKeyHashCode`, which is stable across recreation
   for the same call site. It wraps the entry's content in a `SaveableStateHolder.SaveableStateProvider(thatKey)` owned
   by the host. A recreated entry for the same call site then finds its state.
2. `modal` becomes a `() -> Boolean` (or a snapshot field on the entry), read by the host where it applies semantics.
   It is never an effect or `key` input.
3. New `SheetRestoreTest` (`StateRestorationTester`):
   - a sheet at Medium with a typed field and a scrolled list: both restore;
   - a non-modal sheet dragged across `upThrough` keeps its field and scroll and creates no new composition groups.

---

## Major

### C1-04 (major) The non-modal pass-through cannot reach the app root

**Problem.** B-51 states as [V] that, with `sharePointerInputWithSiblings()` on the scrim, a touch beside the sheet
"reaches the app root, the overlay host's sibling" (`:1441-1444`). Hit testing does not work that way:

- Sharing is checked per child **LayoutNode at each level**, on that child's own modifier chain only
  (`ui112/node/InnerNodeCoordinator.kt:215-244`; `NodeCoordinator.kt:1478-1491`, `visitSelfAndLocalDescendants` [V]).
- The scrim sits several LayoutNodes below the root: scrim → `SheetLayout` → hosted content → overlay `Box`
  (`AppOverlayHost.kt:65`) → host `Box` (`:56`).
- At the root level, the overlay box child was hit (its descendant scrim was hit) and has no sharing node of its own. So
  `continueHitTest = false`, and the content box (`:57`) never sees the touch.

The Maps case fails. §21's fallback, removing the scrim's pointer node (`:2669`), works but swaps modifiers.

**Fix.**

- B-51: at `m = 0` the scrim child is **not placed**. `SheetLayout`'s placement block decides this from a plain field.
  An unplaced child is skipped by hit testing (`InnerNodeCoordinator.kt:216` [V]): no recomposition, no node swap.
- The overlay host's boxes carry no pointer modifiers (true today), so a miss falls through to the content sibling.
- Retag the claim as "verified: does not work". Replace the §21 row.
- `SheetModalityTest`:
  - a tap on a clickable in the app root beside a peek → clicked;
  - above `upThrough` → not clicked.

### C1-05 (major) The mutex table is wrong, and "leaving" is detected late

**Problem 1: priorities.** §4.4 says the drag and its release settle run at `UserInput` "(foundation's)" (`:481`).
Foundation runs both at `MutatePriority.Default`:

- `drag` calls `state.anchoredDrag { … }` and `fling` calls `state.anchoredDrag { performFling }`, both with the
  default priority (`src/foundation-1.12.1/.../AnchoredDraggable.kt:410-428`, `:457-477`, default at `:1155` [V]).
- `MutatorMutex` cancels a running mutation of equal or lower priority (`fnd112/MutatorMutex.kt:82` [V]). That resolves
  the spec's [U] at `:479`.

So with the spec's table, `animateTo`, `expand` and `collapse` at `UserInput` cancel a finger drag, and a drag started
during one is rejected. WP03's code already departs from the table: `wip/AppSheetState.kt:180-311` uses
`PreventUserInput` and `wip/gesture/SheetFlingBehavior.kt:64-104` uses `Default`. Two implementers, two tables.

**Problem 2: "leaving".** §13 defines leaving as `isVisible && targetDetent == null` (`:2198`). §4.4 derives
`targetDetent` from foundation's `targetValue` (`:477-478`). During a fling inside the untargeted `anchoredDrag`:

- `targetValue` is "the closest value to the current offset" (`AnchoredDraggable.kt:868-871` [V]);
- `isAnimationRunning` is false (`dragTarget != null`, `:935-936`).

A flick-dismiss from Medium therefore reads "not leaving" until it passes the midpoint, about 60-100 ms [D]. In that
window:

- B-17's catch is not skipped: a touch catches a sheet the person flicked away after `onDismissRequest` already ran.
  The sheet then fights `hide()` and the pop.
- B-37.3 keeps a destination sheet's handler enabled: a Back commits a second dismissal and calls `onDismissRequest`
  twice. `SheetDismissTest` asserts exactly once.
- `targetDetent` also flips at every midpoint of a drag, so the "≤ 1 recomposition per settle" in §10.2 (`:1936`) is
  false for its readers.

**Fix.**

1. Table:
   - drag and fling: `Default` (foundation's);
   - `animateTo`, `expand`, `collapse`: `Default`, plus an explicit "return while a finger is down";
   - `show()`, `hide()` and the catch: `PreventUserInput`.
2. An internal snapshot field `leaving`:
   - set synchronously at the dismissal decision (a release resolved to hidden, the scrim tap, Back's commit, a key,
     `requestDismiss`, `hide()`);
   - cleared only by `show()`;
   - read by B-17, B-18, B-37, B-43 and the nested-scroll connection.
3. `targetDetent` is the sheet's own field, written by the fling behaviour and `animateTo`. Its KDoc says it changes at
   midpoints during a drag. `isMoving` comes from the sheet's own flags.
4. Test: a 3000 dp/s flick from Medium with a touch 16 ms later is not caught. With a Back 16 ms later instead,
   `onDismissRequest` runs exactly once, and a destination pops exactly one entry.

### C1-06 (major) `PresentedSheetFrame` must keep the presentation host composed

**Problem.** `PresentationHost` returns before composing `NavDisplay` when
`presented.size == 0 && live == 0 && !host.card.active` (`nav4/presentation/PresentationHost.kt:80` [C]). `live`
counts composed `PresentedFrame`s (`:111`, `:173-176`). §11.2 replaces `PresentedFrame` for sheets and lists what
`PresentedSheetFrame` provides (`:2014-2019`), without `onLive`.

Popping the last destination sheet sets `presented.size` to 0, and the host returns early in the same recomposition.
`NavDisplay` and the `OverlayScene` then leave composition:

- `onRemove()`'s slide never runs, and its `hide()` is cancelled;
- `StackBackHandler` and `GateSettle` (`:153-154`) are gone.

So B-43's "further presses are counted by the presentation gate" has no handler. The second and third Back of a burst
reach the tab and pop the screen beneath, and `sheet_rapid_back.lands_on_presenter` fails.

**Fix.** §11.2: `PresentedSheetFrame` registers in `live` exactly as `PresentedFrame` does (`DisposableEffect`
`onLive(+1/−1)`). The host then stays composed until Navigation 3 disposes the entry after `onRemove()` returns.
`SheetSceneTest`: pop the only sheet; the host stays composed through the slide, and a Back during it is counted by
`presented.gate`.

### C1-07 (major) Nested scroll bypasses the rubber band and the mutex

**Problem.** B-28 moves the sheet from the content's nested scroll with `dispatchRawDelta` (`:1300`). That call has
three problems:

1. It clamps to the anchors (`newOffsetForDelta` → `coerceIn(min, max)`, `AnchoredDraggable.kt:1234-1250` [V]). The
   B-25 band below a blocked lowest detent can never be reached from a list. The B-24 band above the top is also lost
   unless the connection applies it. The leftover falls through to the list's own `AppOverscroll`, so the list
   stretches inside a sheet that does not move. That is a double rubber band in the wrong place, on the most common
   sheet (a list).
2. It bypasses the drag mutex (`:1245-1250`). A delta that arrives while `show`, `hide`, `animateTo` or a park runs is
   overwritten by the animation on the next frame: visible jitter.
   - B-17 stops `show` and settles, but it is skipped while the sheet leaves or parks.
   - Wheel and trackpad scrolls have no catch at all.
3. B-28's `onPreFling` "settles a sheet that is not resting with the full velocity (B-22)". The obvious call,
   `AnchoredDraggableState.settle(velocity)`, is deprecated and throws unless `usePreModifierChangeBehavior`
   (`:1062-1069`).

`anchoredDraggable(overscrollEffect = …)` (`:414-426`) only sees the draggable's own deltas, never the nested-scroll
ones, so `SheetStretch` gets nothing from a list.

**Fix.**

1. B-28 routes the connection's deltas through the sheet's own `dragBy(delta)`. That function applies `SheetStretch`
   past the stops and is shared with the draggable's overscroll path (WP03's `wip/gesture/SheetNestedScroll.kt:37`,
   `:46` already moves toward this).
2. The deltas run inside one `anchoredDrag(Default)` session, opened on the gesture's first delta and closed in
   `onPreFling`.
3. The connection consumes nothing while `leaving`, while `parkProgress > 0`, or while a `PreventUserInput` mutation
   holds the mutex.
4. `onPreFling` runs `SheetFlingBehavior` (B-22) inside that session, never `settle(velocity)`.
5. Tests:
   - a list dragged down at its top on a non-dismissible sheet: the sheet bands, the list does not stretch;
   - a scroll during the exit: offset unchanged.

### C1-08 (major) `NavGuard` drops pushes while an in-screen sheet reports motion

**Problem.** B-15 and D-15 make every in-screen sheet report its show, hide, park and unpark to its tab's `BackGate`
(§11.3, `:2040-2041`).

- `NavGuard` **drops** a `Navigate` while the gate is moving and `lastChange == Push`
  (`nav4/back/NavGuard.kt:121-130` [C]).
- `lastChange` is the host's last stack change. It is never cleared by motion reports (`BackGate.kt:47`, `:59-64`,
  `:67-74`).

So on any screen reached by a push (`lastChange` stays `Push`), a navigation submitted while its sheet presents or
leaves is silently lost (`AppShell`'s loop stops on `Drop`). Examples:

- a row tapped within 360 ms of opening;
- an `onDismissRequest` that navigates while `hide()` is reported;
- a ViewModel effect that navigates during an unpark.

B-15 wants the push held. The code drops it.

**Fix.**

- Report sheet motion so that `guard()` returns `Wait`, never `Drop`. For example
  `BackGate.report(reporter, moving, overlay = true)`, with `guard()` treating overlay-only motion as `Wait`. Or clear
  `lastChange` when a transition settles.
- `SheetGateTest`: with `lastChange = Push` and a sheet presenting, a submitted push is applied after the sheet's 99 %,
  not dropped.

### C1-09 (major) Back with the keyboard up can bypass the guard or pop the screen beneath

**Problem.** B-44 disables the sheet's handler while the IME target is visible. It claims the rule "holds either way",
whatever the IME callback's priority (`:1395-1400`, `:2663`). It does not:

- navigationevent keeps one platform callback per priority, registered while a handler of that priority is enabled
  (`ne/OnBackInvokedInput.android.kt:76-91` [V]).
- With the sheet's overlay handler off, the app's `PRIORITY_DEFAULT` callback stays registered while the stack
  handlers are enabled. The platform then picks between it and the IME's callback by priority, then by recency [U].
- The default input re-registers whenever its enabled set goes from empty to non-empty. That happens, for example,
  when `StackBackHandler`'s `canGoBack || gate.moving` toggles on a sheet's own motion report (B-15). Re-registered
  after the IME, the app's callback wins.

Then Back goes to the stack handler beneath:

- **destination sheet:** the presentation host's `StackBackHandler` calls `gate.onBack(pop)`. The sheet is popped
  **without its `SheetDismissGuard`**, so a dirty form is lost;
- **in-screen modal sheet:** the tab's `StackBackHandler` pops the screen beneath.

B-37.4 forbids both.

**Fix.** Make "Back never reaches what is beneath a modal sheet" structural, not dependent on the order of the IME's
callback:

1. While a modal sheet is up, the default-priority stack handlers beneath it (the tab's, and the presentation host's
   for lower levels) are disabled. The flag is derived from `SheetStack`: ≤ 1 recomposition per present.
2. Keep the sheet's handler enabled while the IME target is visible, with a branch 0: commit hides the keyboard (no
   shrink, no attempt). If QA2 proves on the phone that the IME always takes Back first, (1) alone suffices; record
   which.

`SheetBackTest`: with the IME visible and the default input re-registered after the IME's callback, the guard is still
asked and nothing beneath pops.

### C1-10 (major) FitContent re-resolves from the IME-constrained measure

**Problem.** Three rules combine into a loop:

- R-3 resolves FitContent from "the column's measured height" (`:571`).
- B-47 re-measures the content box to the region visible at the IME target (`:1414-1419`).
- R-11 writes new anchors whenever the stops change, and B-10 animates a FitContent sheet "to the new height".

Nothing says detents ignore the IME-constrained measure. Content taller than the visible region at the target triggers
it. Redmi example [D]: a 720 dp FitContent form, a 300 dp keyboard, ≈ 472 dp visible.

1. The IME measure shrinks the column, so FitContent shrinks and the anchors move.
2. The sheet animates on `springs.sheet` while the keyboard rises, and `lift = clamp(K − bottomInset, 0, y − largeTop)`
   changes with that `y`.
3. The reverse animation runs when the keyboard falls.

That is two extra detent animations and anchor writes per IME transition, a visible wobble, and more re-measures than
"≤ 2 per transition".

**Fix.** R-3 and B-47: detents resolve only from the IME-free measure (content measured against Large's height, cached
as `contentHeight`). The measure at the IME target sizes the content box for placement and never updates
`contentHeight` or the anchors. `SheetImeTest`: a long FitContent form, keyboard up and down: 0 anchor writes, 0 detent
animations, ≤ 2 content re-measures.

### C1-11 (major) A non-modal sheet treats the keyboard as bottom chrome

**Problem.** B-52 sets `restBottom = H − bottomChrome` from the shell's settled `LocalBottomChrome` (`:1449-1452`).
That holder is "the tab bar, its accessory, **or the keyboard when it hides the bar**"
(`ds4/foundation/layout/Chrome.kt:27-31` [C]); `bottomChrome()` puts the keyboard first
(`nav4/shell/BottomChromeWriter.kt:50-55`).

When any keyboard opens, `restBottom` jumps to the keyboard's target. The keyboard may come from a field in the
persistent sheet's header or from a field in the screen beneath.

- R-11 re-resolves every detent, and the sheet springs on `springs.sheet` instead of riding the IME curve (B-45's
  guarantee).
- `lift` computes `K − bottomInset` with `bottomInset` equal to the keyboard target, so it stays near 0.
- `m`, `e` and `currentDetent` can all change mid-transition.

B-45 also lifts a non-modal sheet for a keyboard opened by a field *beneath* it, covering the field being typed into.

**Fix.**

- B-52 reads the bar's own settled top: a holder the shell writes from `barState.settledHeight`, which excludes the
  keyboard. Never `LocalBottomChrome`.
- B-45 lifts only while focus is inside the sheet: `onFocusChanged` on the sheet root writes a plain field that the
  placement block reads.
- For a keyboard owned by content beneath, the sheet stays put. Decide against iOS Maps, §21.
- Test:
  - a peek above the bar, focus a field beneath: the sheet does not move;
  - focus the sheet's header field: it rises frame by frame with the IME, with 0 anchor writes.

### C1-12 (major) Parking by not placing contradicts the project's own kept-tab finding

**Problem.** §11.1 and B-14 park an overlay by making it "not placed, drawn, hit-tested or announced" (`:1981-1982`).
The project has already measured why that is the wrong way to hide something that comes back:

- Unplacing drops the layers and display lists, so the return frame re-records everything (SMOOTHNESS S4,
  `research/SMOOTHNESS-PLAN.md:296-329`, visited first frame max 16.45 ms).
- WP04 fixed it the other way: "still measured and placed … drawn at alpha 0 off the window … Skipping its layout
  instead would drop every layer it had" (`nav4/shell/tabs/KeptTabs.kt:38-42` [C]).

A peek sheet on tab A would be rebuilt on the switch back to A, the frame REQUIREMENTS 28 wants cheap. B-14 also
leaves open where `LocalTabVisible` is read. Read in composition, it recomposes the sheet on the switch frame (S4 item
3 forbids local flips there).

**Fix.** Park in-screen overlays the KeptTabs way:

- still measured and placed;
- the sheet layer at alpha 0 (HWUI skips it without an offscreen pass) and off-window;
- semantics cleared through a node, pointer input ignored by a plain-field check, no frame-rate vote;
- `TabVisibility` read in the layer block and placement, plus a `snapshotFlow` for semantics and Back, never in
  composition.

`SheetParkingTest`: switching away from and back to a tab with a peek sheet recomposes nothing in the sheet and
re-records nothing on the return frame. Add a `perf.sh tabs` variant with a peek sheet.

### C1-13 (major) Glass samples the wrong scene above level 1

**Problem.** D-17, §6.6 and §11.5 let any Glass sheet sample the `GlassStage` scene through `LocalSheetBackdrop`,
provided around both hosts. The scene holds **only the tab content** (`glass/GlassStage.kt:47-50` [C]). The bar is in
the chrome slot (`:51-53`), and covers and other sheets are in the presentation and overlay hosts. So:

- A Glass sheet over a cover, stacked on another sheet (B-54), or opened in-screen from a destination sheet's content
  shows a blur of tab content that is hidden behind an opaque cover or sheet. These are wrong pixels, not a
  misalignment.
- Even at level 1 over the shell, the sample has no bar. A floating Glass sheet over the bar shows content without the
  bar inside its body, while the real bar shows through the 8 dp gap below it.

**Fix.**

- §6.6: sample live only when the sheet's presenter is level 0 (the shell) and no cover is up. Every other Glass sheet
  draws Frosted, decided in draw from `SheetStack`.
- For the bar, either `GlassSheetBackdrop` also draws the chrome's recording after the scene, or the gap is documented
  and checked in CP2's recording.
- Screenshot or `SheetStackTest`: a Glass sheet over a cover draws Frosted.

### C1-14 (major) The card stack's transform is not free in Compose

**Problem.** The spec classes the presenter's recede as L0 (§6.4, §10.1 `:1917`). In Compose, changing a layer's
scale or translation does more than update a RenderNode:

- `updateLayerParameters` calls `owner.onLayoutChange(layoutNode)`, which does accessibility and content-capture
  bookkeeping for the subtree (`ui112/node/NodeCoordinator.kt:612-680`;
  `ui112/platform/AndroidComposeView.android.kt:2187-2190` [V]).
- When the subtree has position observers, it requests their positioned callbacks (`:670-675`).
- A shape change with `clip = true` invalidates semantics (`:640-646`).

The shell subtree holds `RecordSceneNode`. Its `onGloballyPositioned` writes `sceneOrigin = positionInRoot()` into
snapshot state (`glass/SceneBackdrop.kt:52`, `:58-61`, `:163` [C]), and every sample's draw reads it "to make samples
redraw". So on the glass presets every recede frame invalidates the bar's sample and the Glass sheet's, and re-runs the
live blur.

The corner lerp R₀ → 12 dp also swaps the outline ~49 times on the Redmi (81 px → 33 px [D]) with clip on: 49 semantics
invalidations of the shell root.

**Fix.**

1. WP05: `SceneBackdrop` invalidates samples on the scene-to-surface offset (`scene.localPositionOf(surface)`), not on
   `positionInRoot`. A transform shared by the stage and the bar then triggers nothing.
2. While `recede(0) > 0`, the bar (dimmed under a modal sheet) may draw its last sample or Frosted.
3. The presenter's corner becomes a draw-time clip in C1-01's dim node, or is quantised to ≤ 8 shape steps.
4. §16.1: a trace check that neither the scene RenderNode nor the bar's backdrop is re-recorded during a Medium → Large
   drag, `glass_live` column.

### C1-15 (major) Slot providers lose the screen's composition locals

**Problem.** Content-side providers "register through an internal `LocalSheetSlots` in a `DisposableEffect`", and the
slot host composes them (`:911-913`, B-2). The provided lambda is therefore composed in the slot host, **outside** the
content's CompositionLocal scope. For a destination sheet, the content is `entry.Content()` inside Navigation 3's
decorators (saveable state, `ViewModelStoreOwner`, lifecycle, `PresentationHost.kt:92-94`), while the slot host sits
around it in `AppSheet`. A header or footer then sees the host's locals, not the entry's:

- **ViewModels.** A header or footer that calls `hiltViewModel()` or `viewModel()` resolves the presentation host's
  owner, the activity. The ViewModel outlives the sheet and is reused by the next opening: state leaks across
  sessions.
- **Saved state.** A `rememberSaveable` in a provided footer saves into the host's registry and can restore into a
  later opening of the same destination.
- **Lifecycle.** `LocalLifecycleOwner` is the activity's, so `collectAsStateWithLifecycle` in a header keeps
  collecting while the entry is stopped.
- **Other locals.** Locals the screen provides above `ProvideSheetHeader` (a nested `AppTheme`, `LocalPaneContext`) are
  lost.

**Fix.**

- §4.8: the provider captures `currentCompositionLocalContext` at its call site, and the slot host composes
  `CompositionLocalProvider(captured) { content() }` inside a `SaveableStateProvider` keyed by the provider's compound
  key. This is the pattern `HostedOverlay` already uses (`AppOverlayHost.kt:89-92`).
- Test: a destination footer calling `viewModel()` gets the same instance as its screen, cleared when the sheet pops.

### C1-16 (major) Sibling sheets of one screen share a level

**Problem.** §3.2 gives an in-screen sheet "its owner's level + 1" (`:139-143`), and the presenter draws the dim for
"the sheet directly above it" (`:1652-1653`). Take the Maps pattern: a persistent `NonModal` sheet plus a modal detail
sheet opened from the screen (a tapped pin). Both are siblings at level 1.

- The shell draws the modal sheet's dim inside its own layer, so **under** the persistent sheet (layer 4, registered
  earlier).
- The persistent sheet stays bright above the dim, with a visible seam.
- Which sheet the shell recedes behind is undefined.

Separately, `DevToolsOverlay` sits in `AppRoot`'s box above the shell and the presentation host
(`app/.../ui/AppRoot.kt:86-89`). It is neither dimmed nor receded.

**Fix.**

- Levels follow the window's stack order: a modal sheet registers at `topLevel(stack) + 1` whoever owns it. The
  topmost existing sheet then presents it and draws its dim (B-54's path).
- Alternatively, a modal sheet opened beside a persistent one parks it (B-14's "covered", extended).
- State the DevTools overlay's z-order explicitly.
- Test: a peek sheet plus a modal sheet from the screen: the peek is dimmed and hidden from accessibility.

### C1-17 (major) In-screen non-modal sheets ignore their owner's back card and push

**Problem.** In-screen sheets draw in the overlay host, outside their owner's stack entry, whose pose is a
`graphicsLayer` (`nav4/stack/StackLayout.kt:128`, `:149` [C]).

- A persistent sheet at its lowest detent leaves Back to the stack (B-37.4). WP04's Gmail-style back card then shrinks
  the owner screen into a card that follows the finger.
- The owner's sheet meanwhile stays full size and unscaled on top, across the card's edge. That is a double exposure,
  which REQUIREMENTS 24 and 32 ("never double-exposed") forbid.
- On commit, the owner leaves composition and its sheet vanishes in one frame.
- The same happens when a screen arrives by push with a visible persistent sheet (the sheet does not slide with it),
  and during the tab reveal nudge.

**Fix.**

- B-14 adds "owner moving" as a park trigger. Either the entry exposes its pose (a `LocalEntryPose` read in the sheet's
  layer block) and the sheet applies the same transform, or the sheet parks from the gesture's first frame, unparks on
  cancel, and on commit finishes its exit drawn.
- Add `back_during_sheet` to `perf.sh` with a mid-gesture screenshot.

### C1-18 (major) The open path, and gates that do not match the project's own numbers

**Problem.** An in-screen sheet with a content-side provider takes three frames before the first moving frame:

| Frame | What happens |
|---|---|
| N | The caller composes; `HostedOverlay` registers in a `DisposableEffect` (`AppOverlayHost.kt:90-98`) |
| N+1 | The overlay host composes the sheet and its content, measured at Large's height, so a lazy list in a Medium sheet composes about twice what is visible |
| N+2 | The slot host composes the provider (B-2) |
| N+3 | First moving frame |

The composing frame will usually exceed 8.33 ms and counts as janky. The gates do not match:

- `sheet_present.janky_pct` is 1.0 over 10 open/close cycles (`:2333`). Push/pop has the same composing frame and is
  gated at 2.0, with a hitch metric to separate composition from motion (`perf/gates.toml:114-131`).
- The GPU rows (`sheet_drag` 7.0, `sheet_cardstack` 8.0, `sheet_scroll` 7.0) rest on no measurement. A sheet at Large is
  a near-full-screen sheet over a full receded shell plus a blended full-screen dim. CP1 measured two screens at GPU
  p90 12 ms (`decisions/CP1.md:109-111`).
- No row covers "tap a row in a sheet that closes it and pushes" (B-60): the exit, the push and the un-recede run
  together.

**Fix.**

1. §16.1 rows:
   - `sheet_present.hitch_ms_per_s` and `.tap_to_motion_ms`, as ratchets;
   - `janky_pct` over motion frames only, or 2.0 as push/pop has;
   - the GPU rows as "ratchet" until QA2's first run;
   - a new `sheet_push_from` row, janky 2.0.
2. State the +1 frame of `HostedOverlay` in B-1/B-2.
3. Offer an opt-in keep-composed mode (`AppSheet(…, properties = AppSheetProperties(keepContent = true))`). After the
   first open, the content stays composed and parked KeptTabs-style, so later opens compose nothing.
4. Optionally, precompose on press-down in the **host** (not the sheet) with
   `SubcomposeLayoutState.createPausedPrecomposition`, as SMOOTHNESS S3.5 allows.

### C1-19 (major) At Large the receded presenter is drawn in full under an opaque sheet

**Problem.** HWUI has no occlusion culling. With the sheet at Large and opaque (Solid, or Frosted/Glass past
`opaqueAt`), every frame of these journeys redraws the whole receded shell and any covered sheets beneath:

- a scroll inside the sheet;
- a drag near Large;
- the IME moving.

That is about two screens plus the D2 dim, for a 10 dp strip of visible presenter. It is the largest GPU lever for
`sheet_scroll` and `sheet_cardstack` on the phone.

**Fix.**

- In C1-01's dim node: when `e = 1` and the sheet's body is opaque, clip the presenter's content layer to the band above
  the sheet's top, in presenter coordinates, plus R. The clip edge lies under the opaque sheet, so nothing visible
  changes.
- Do the same for B-56's covered sheets.
- Measure `sheet_scroll.gpu_p90_ms` with and without it.

---

## Minor

### C1-20 (minor) Boxing in draw

- `sheetBackdrop(frame, opaque: () -> Float, dim: () -> Color)` (`:1071`) allocates on every call. A Kotlin function
  type returns a boxed `Float`, and `Float.valueOf` has no cache; the `Color` value class is boxed the same way. Both
  are called in draw every frame. WP03's code already has this signature (`wip/surface/SheetBackdrop.kt:38`).
  - **Fix:** put `opaque: Float` and `dim: Color` on `SheetFrame` as getters. A non-generic interface returns
    `float`/`long`, so nothing boxes.
- "Shape objects cached per whole px" (`:1659`) is a `HashMap<Int, Shape>` in WP03's code
  (`wip/stack/CardStackPresenter.kt:59`, `:76`). It boxes the key, and `Integer.valueOf` allocates above 127 px (3×
  phones with large display corners).
  - **Fix:** an array indexed by px, or `MutableIntObjectMap`.
- `SheetStack.recede`/`dim` run per frame, per presenter.
  - **Fix:** specify index loops over a fixed-capacity array (no iterator, `filter` or `maxBy`), and have `PaneBounds`
    return a cached mutable `Rect`.

### C1-21 (minor) `LocalSheetBackdrop` cannot be provided the way §11.5 says

**Problem.** The overlay layer lives in `AppOverlayHost`, an ancestor of the shell (`ds/theme/AppTheme.kt:145`). The
scene is created inside `GlassStage`, a descendant (`glass/GlassStage.kt:43-45`), and a descendant cannot provide a
local around its ancestor. If both hosts are wrapped instead, swapping a static local on a tier demotion
(`glass/GlassQuality.kt:96-108`) recomposes both hosts and all sheet content, right when the GPU watcher has fired.

**Fix.** `AppTheme` installs one stable `SheetBackdropHolder`. `GlassStage` writes `holder.scene` on attach and on
tier change. The surface node reads it in draw and switches Glass ↔ Frosted there. Test: a tier change recomposes
nothing in sheet content.

### C1-22 (minor) B-63 and §7.7 disagree about what fades

**Problem.** B-63 fades "the sheet layer with `CompositingStrategy.ModulateAlpha`" (`:1519-1520`). §7.7.6 says "The
surface never fades. Only the content layer fades" (`:1817`). §10.3's code rule `layer-alpha-fill` (`:1947`) would also
flag B-63, since the sheet layer at Large is screen-sized.

**Fix.**

- Keep B-63: `ModulateAlpha` needs no offscreen buffer, and the shadow is outside-only.
- Delete §7.7.6's second sentence.
- The code rule allows `alpha` with `ModulateAlpha` on the sheet layer by name.
- Glass draws Frosted while alpha < 1 (already stated).

### C1-23 (minor) The frame-rate vote

**Problem.** B-64 votes High on the sheet layer at all times (`:1532-1534`). Three facts from the 1.12.1 sources [V]:

- `preferredFrameRate` propagates to every child layer whose own rate is 0 (`ui112/FrameRate.kt:115-145`).
- Each such layer votes whenever its display list updates (`GraphicsLayerOwnerLayer.android.kt:274-277`). This path is
  live from API 35 (`AndroidComposeView.android.kt:364-365`), and the Redmi runs Android 17
  (`research/ios-motion.md:15`).
- Placement changes already vote High (`NodeCoordinator.kt:450-451`).

So a spinner, a shimmer or a caret in a resting sheet holds High. The drag and settle need no explicit vote, while
layer-only motion lacks one: the back shrink and the recedes. `sheet_idle.frames` counts frames, not votes, so it
cannot see any of this.

**Fix.** Toggle `preferredFrameRate(High ↔ 0f)` from a `snapshotFlow` on the sheet layer and on `cardStackPresenter`,
only while back progress or a recede moves (SMOOTHNESS S10). Verify with the `dumpsys SurfaceFlinger` or Perfetto
frame-rate track on an API 35+ device.

### C1-24 (minor) Constants checked against their sources

| Constant | Finding | Fix |
|---|---|---|
| `release.dismissPosition` 0.52 (`:1251`, `:1611`) | Inverted. Flutter's controller is 1 when shown and decreases as the sheet is dragged (`value -= delta`, `src/flutter/sheet.dart:1071`). The sheet stays while `value > 0.52` (`:1119`). It is dismissed once ≥ **48 %** of the travel is dragged, judged at the current position, not projected. Inherited from `research.md:278` | `0.48`, compared on `(y − o_n)/(h − o_n)`; fix the worked table and `research.md:278` |
| B-61 haptic "within 1 px" | `springs.sheet` with ζ = 1 and ω = 18.48 rad/s leaves `(1 + ωt)e^{−ωt}` of the travel. 1 px of a 1081 px Medium → Large travel is reached at **t ≈ 0.50 s**, with or without a 600 dp/s seed [D]. The sheet is 88 % there at 0.2 s and 99 % at 0.36 s, so the tick lands 150-250 ms after the motion visibly ends. D-16 fails | Fire at remaining ≤ max(1 px, 2 % of the travel) |
| Settle spring threshold | Not named. `AppSpring.unit` uses 0.001 (`tokens/motion.json:30`), so a px settle would run ≈ 0.75 s of sub-pixel tail, holding frames, `isMoving` and host motion | §6.1 names `springs.sheet.pixels` (0.5 px, `ds/theme/AppMotion.kt:72`) |
| `cardStack.statusBarFlip` 0.5, tagged [M] | Flutter switches to light icons for the whole secondary transition (`AnnotatedRegion`, `sheet.dart:361-368`), not at 0.5 | Retag [D]/taste |
| Spring math, rubber band, merged dim, 0.9165, 0.005 H, 0.10, `#C8C8C8`, 2 H/s, 0.008 H, grabber, MDC 48/24 dp and counter-scale | Re-derived; all correct | none |

### C1-25 (minor) B-21's press delay

**Problem.** `isDelayPressesUsingGestureConsumptionEnabled` does not exist in foundation 1.12.1 (no match in its
sources). `clickable` delays a press by `TapIndicationDelay`, which is the platform tap timeout of 100 ms
(`fnd112/Clickable.android.kt:40`), whenever an ancestor gesture is `Waiting` (`fnd112/Clickable.kt:2155-2156`,
`:2179-2190`). The sheet's `anchoredDraggable` is such an ancestor for every row. "Within 100 ms" is therefore
unattainable, and CP1 already accepts the tap-timeout press (`decisions/CP1.md:137`).

**Fix.** B-21: "no delay beyond the platform tap timeout, as in a `LazyColumn` row". The test asserts press ≤ tap
timeout + 1 frame. Drop the [U].

### C1-26 (minor) B-17 leaves a stopped sheet stranded

**Problem.** B-17 does not say what happens when a down stops a settle and the finger lifts without a vertical drag,
or starts a horizontal scroll instead. Nothing re-settles, so the sheet rests between stops. WP03 already settles "where
it was heading" (`wip/gesture/SheetCatch.kt:22-27`, `:92-104`), but the spec is what WP07-B ports.

**Fix.** On the matching up, or on cancel, with no vertical drag: settle at v = 0 to the stopped animation's target,
with no haptic. Add a test.

### C1-27 (minor) Name the class-based Back handler

**Problem.** B-36's "one state per handler" points at the Compose `NavigationBackHandler`. That composable takes no
priority: it calls `dispatcher.addHandler(sourceHandler)` at the default priority (`nec/NavigationEventHandler.kt:115`,
`ne/NavigationEventDispatcher.kt:292-295` [V]). It also applies `isBackEnabled` in a `SideEffect` (`:97-103`), so
enabling it needs a recomposition, which B-37's composition-free enablement rules out.

**Fix.** Name the pattern of `ds/component/overlay/OverlayBackHandler.kt:38-50`:
`addHandler(handler, PRIORITY_OVERLAY)`, with `handler.isBackEnabled` written from the `snapshotFlow` collector.

### C1-28 (minor) `SheetSessions`' saved key

**Problem.** The saver keys sessions by `contentKey.toString()` (`:2007-2008`). A key without a stable `toString`
silently loses its detent across process death, and keys popped before death linger in the saved map.

**Fix.** Create each session's state with `rememberSaveable` inside a decorator that runs within the saveable-state
decorator's scope (`design-perf.md:909`), or key the map by `contentKey` itself, which must already be saveable.

### C1-29 (minor) Status-bar flip and black backing need a mechanism

**Problem.** §7.9 and §11.3 leave the mechanism open. The template's only owner of the system bars re-runs
`activity.enableEdgeToEdge(...)` from a `LaunchedEffect` keyed on `dark` (`app/.../ui/AppRoot.kt:171-180`). Feeding it
`rootReceded` would re-run `setDecorFitsSystemWindows`, re-set the bar colours and re-dispatch insets in the middle of a
drag. Replacing the window's background drawable can also request a layout.

**Fix.**

- The flip writes only `WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars`, from a `snapshotFlow`.
- The backing mutates the existing `ColorDrawable`'s colour, never `setBackgroundDrawable`.
- A trace check: no View layout during a Medium → Large drag.

### C1-30 (minor) `LocalSheetSurface` stability

**Problem.** `AppScaffold`, `AppTopBar` and `AppBottomActionBar` read `LocalSheetSurface` in composition
(`:888-896`). A `SheetSurface` rebuilt on a sheet recomposition (a settle), or one carrying the *effective* material
(the tier, a fade, `e` past `opaqueAt`), recomposes all content through a static local.

**Fix.** One remembered `SheetSurface` per sheet, holding the resolved declared material. Anything animated is read
through `scope` in draw. Test: a settle and a tier change recompose no content.

### C1-31 (minor) Provenance

**Problem.** The spec pins WP03 `8ea3985` and WP04 `230f894`. The heads are now `87a3a06`, with this spec half
implemented and uncommitted, and `666024c`. That code already embodies C1-01 and C1-20 and departs from §4.4's
priority table (C1-05).

**Fix.** WP03 applies these fixes to the uncommitted files before committing. Re-pin the [C] citations at the sheet
checkpoint.

---

## Checked and found sound

Verified this session; no change needed:

| Claim | Evidence |
|---|---|
| `springs.sheet` k = 341.5, ζ = 1; 55/88/99 % at 100/200/359 ms; first moving frame 1.07 % of travel (≤ 4 %); `dialog` k 522; `screen` k 333.6; Calm/Snappy 0.3908/0.2957 s | Recomputed [D] |
| Rubber band 6.6, 86 and 120 dp; release table rows; merged dim 0.40/0.46 light, 0.56/0.604 dark | Recomputed [D] |
| Flutter constants 0.9165, 0.005, 0.10, `#C8C8C8`, 0.9 / 20, 2.0 H/s, 0.08 − 0.072; `topCenter` scale origin; grabber 36 × 5 at 5 dp, content 15 dp | `src/flutter/sheet.dart:14-90`, `:327-375`, `:704-724` |
| MDC 48/24 dp, pivot at the bottom, content counter-scale `scaleX / scaleY` about its top | `src/mdc/MaterialBottomContainerBackHelper.java:82-103`; `motion_dimens.xml` |
| `@FrequentlyChangingValue` may annotate functions | `runtime-annotation/FrequentlyChangingValue.kt:38` |
| `WindowInsetsRulers.getAnimation` is placement-only, so writing the IME target from placement is the only route | `ui112/layout/WindowInsetsRulers.kt:69`, `:223-250` |
| Layer alpha 0 is skipped without an offscreen pass (B-56); KeptTabs relies on the same | `KeptTabs.kt:38-42` |
| Both hosts lie outside the recorded scene; a surface inside it fails loudly | `glass/GlassStage.kt:47-53`; `SceneBackdrop.kt` `canBeSampledBy` |
| `adjustResize` is set, so B-45's IME rulers apply | `app/src/main/AndroidManifest.xml:68` |
| No `Dialog` or `Popup` on any sheet path (D-22); the merged dim's `Color` return is unboxed; the nine-slice and no-shadow-on-modal choice (D-18) | Spec |
