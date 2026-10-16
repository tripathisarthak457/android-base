# Smoothness guide

> **Draft for WP12** (REQUIREMENTS 25). Written Oct 12, 2026 from the overhaul's audits, research plans, handoffs and
> perf data. Every rule and number cites its source; WP12 checks each claim against the merged code before shipping.
> Paths starting with `audit/`, `research/`, `handoff/`, `perf/`, `qa/`, `design/` or `decisions/` are under
> `~/Documents/projects/tools/.overhaul/`. Code paths are as of the heads the sources name; the merged code may have
> moved them. Marks: **[verify]** means the source describes a plan or work in progress, not merged code.

## 1. The bar

**Why it matters.** The generator exists to produce apps that feel light. The audits found that generated apps were
heavy and stuttered on exactly the moments people notice: switching tabs, pushing and popping screens, pressing Back
quickly (`PROGRESS.md` "Audit headlines").

**The target** (REQUIREMENTS "Non-negotiable outcome", 19, 21):

- The reference device is a mid-range phone: Redmi Note 10 Pro (`M2101K6I`, serial `9a7c74f4`), running at 120 Hz,
  so each frame has **8.33 ms** (`design/MASTER-PLAN.md` §2). If it is smooth there, it is smooth on most phones.
- iOS-level smoothness: about **0 % janky frames** on tab switches, push/pop, back and scroll, and **no dropped first
  frames** (REQUIREMENTS 19). The machine gates in §6 are the floor, not the goal.
- Smoothness never comes at the expense of animation. The full iOS motion set ships (see `MOTION.md`), and every
  animation is interruptible, keeps its velocity, and runs in the layout or draw phase with no per-frame
  recomposition (REQUIREMENTS 21).
- Is iOS level reachable on Android? Perceptually yes. Push/pop realistically lands at 0–1 % janky at 120 Hz, using
  warm previous screens, baseline profiles and an animation clock started after the first frame. Compose animations
  run on the main thread, unlike Core Animation, so main-thread discipline is mandatory (`STATUS.md` §4, closing
  paragraph).

---

## 2. Root causes found, with the fix and the reason

Each entry: what people saw, the cause, the fix, and why the fix is the right one. Sources at the end of each entry.

### 2.1 Tab switches rebuilt the whole tab

- **Seen.** Tab switches felt "glitchy / pushy" (REQUIREMENTS 1). Each switch rebuilt the incoming tab: first frame
  21–52 ms, 1–5 dropped frames; the two tabs cross-faded with both visible at once (`PROGRESS.md` "Audit headlines";
  `audit/template-navigation.md` finding 1: `AppNavHost.kt:99-106` disposes and rebuilds the tab and cross-fades two
  full-screen layers).
- **Cause.** The tab was disposed on leave and composed again on return. The cross-fade put alpha on two full-screen
  layers at once (see 2.3).
- **Fix.**
  1. **Kept tabs.** Every visited tab stays composed for the life of the shell.
  2. **Parked, not unplaced.** A hidden tab stays measured and placed, its layer at alpha 0 off the window, its
     semantics cleared. Skipping its measure or placement drops every display list it had, so the next switch
     re-records the whole screen (`research/SMOOTHNESS-PLAN.md` S4; the first fix skipped placement and still cost
     6.5 / 16.45 ms on the visited first frame).
  3. **No flips on the switch frame.** The tab's lifecycle is raised after its first drawn frame; the leaving tab is
     lowered at once. `LocalTabVisible` became a static local holding a snapshot holder, so the switch recomposes
     nothing (S4 change 2–3; RECOMPOSITION-PLAN R3).
  4. A hidden tab is capped at CREATED and its back dispatcher is off (`handoff/WP04.md` "Rules", "Hidden tabs and
     covered pages are parked").
  5. **No cross-fade.** The incoming tab is nudged 12 dp and unveiled from a 0.6 veil on the `thumb` spring; the
     outgoing tab is parked at once. Deep links, navigation to a root and reduce motion switch with no motion
     (`handoff/WP04.md` "Rules"; `decisions/CP1.md`).
- **Why.** A switch to a kept, parked tab only replays display lists that already exist. The veil is drawn in content,
  so no full-screen layer goes offscreen.
- **Measured.** Visited-tab first frame, median / max: 19.0 / 45.3 ms (old) → 6.5 / 16.5 ms (phase 2), before parking
  landed (`STATUS.md` §4; `perf/WP04/baseline/summary.md`, `perf/WP04/phase2/summary.md`). Phase 3 numbers: pending
  (§3).

### 2.2 The bar moved out of step with the screens

- **Seen.** The bottom bar showed and hid on its own vertical spring, out of step with the screen transitions. It also
  reserved an empty strip painted in the background colour, so root screens were cut off above it: the "opaque white
  block" of REQUIREMENTS 17 (`PROGRESS.md` "Audit headlines"; `audit/template-navigation.md` finding 2,
  `AppShell.kt:175-176, 227-233`).
- **Cause.** Bar visibility followed stack depth through `AnimatedVisibility`, an animation separate from the screen
  slide. The root entry was padded above the bar instead of scrolling under it.
- **Fix.**
  - The bar stays visible at every depth by default, as on iOS. No `AnimatedVisibility`, no bar motion during push
    or pop (`audit/template-navigation.md` §7.3).
  - A destination that must hide the bar implements `FullScreenKey` (or the app sets
    `ShellConfig(hideBarOnPush = true)`). The bar then follows the covered root screen in the draw phase through
    `BarAnchor` (`handoff/WP04.md` "Rules"; `shell/BarAnchor`, `followCovered`) **[verify the merged name]**.
  - The bar floats over content at every depth. Every scrolling container pads with `chromeContentPadding(base)`, and a
    bottom edge fade sits between content and the bar. Never pad a root above the bar (`handoff/WP04.md` "Rules").
  - Bar minimize (`BarMinimize.OnScrollDown`) folds after 10 dp of scroll in one direction on `thumb`, opens on a scroll
    up, at the top or on a tap, and is never scrubbed by the scroll (`handoff/WP04.md` "Rules").
- **Why.** One moving thing per transition. A bar driven by the same geometry as the screen cannot drift from it, and
  a draw-phase translation costs no recomposition.

### 2.3 Alpha on full-screen layers

- **Seen.** Push and pop faded whole screens (`audit/template-navigation.md` finding 9, `NavTransitions.kt:30-33,
  43-46`). Alerts, action sheets and sheets faded a full-screen box with layer alpha (`research/SMOOTHNESS-PLAN.md` S8:
  `AppAlert.kt:79-82`, `AppActionSheet.kt:86-90`, `AppSheet.kt:312-317`). PeriFlow faded its glass bar (G3).
- **Cause.** `graphicsLayer { alpha }` with the default `CompositingStrategy.Auto` and alpha < 1 renders the layer into
  an offscreen buffer every frame. On a full screen that is a full-screen render target per frame
  (`research/SMOOTHNESS-PLAN.md` S8; `research/compose-perf.md` §2.9). PeriFlow measured it: "fading an opaque glass
  screen rendered it all offscreen" (`audit/periflow-learnings.md` §3.3, commit `a8aedb0`).
- **Fix.**
  - Full-screen moves are slides only. The covered page moves a third as far (parallax 0.333) under a **drawn** scrim
    (0.10 light, 0.24 dark) and a 0.04 edge shadow. No alpha or scale on whole screens, except the back card's 100 ms
    `ModulateAlpha` commit fade (`handoff/WP04.md` "Rules"; `decisions/CP1.md`).
  - Scrims are `drawBehind { drawRect(colors.scrim, alpha = shown.value) }` with no layer alpha. A card fades on its own
    card-sized layer (S8).
  - Entrance fades of single cards or rows use `CompositingStrategy.ModulateAlpha`: no offscreen buffer, and outer
    shadows are not clipped to the bounds (the "square shadow corners", `audit/periflow-learnings.md` G10).
  - Screens draw one opaque background each (`EntryFrame` paints `colors.background`); never a second full-screen
    background (S5).
- **Why.** A drawn rect costs one blended fill. An offscreen layer costs a buffer, a full redraw into it and a
  composite, every frame.

### 2.4 Rapid Back

- **Seen.** Pressing Back several times while a pop was running glitched, and could leave the app
  (REQUIREMENTS 1). On the old build every burst ended outside the app: the presses after the first were applied at
  once, so the second landed on the root while the first pop still ran and the third left the app; 18 of 18 bursts
  ended in the launcher (`handoff/WP04.md` "Measurements", emulator, pre-rewrite tree). On the phone the old build
  never landed on the expected screen (`perf/WP04/baseline/summary.md`, "Rapid Back: lands on the expected screen: no").
- **Cause.** A back press during a running push or pop re-targeted that animation, and nothing guarded against it
  (`PROGRESS.md` "Audit headlines").
- **Fix.** `BackGate`: programmatic navigation during a transition waits; a second push during a push is dropped;
  Back pressed while the screens move is counted and applied as one pop at 99 % of travel, never past the root
  (`handoff/WP04.md` "Rules"; `decisions/CP1.md`: "BackGate releases at 99 %"). Regression test `RapidBackTest`;
  device scenario `perf.sh rapid_back`.
- **Why.** Coalescing presses into one pop means one transition, one composition, and a predictable final screen.
- **Measured.** Lands correctly: no → yes. Janky: 5.32 % → 2.62 % (`STATUS.md` §4).

### 2.5 Back swipe during a push

- **Seen.** Starting a back swipe while the push animation was still running showed both screens half-transparent,
  shifted the wrong way, with a seam. Reproduced 3/3 on the old build (REQUIREMENTS 32;
  `qa/user/back-glitch-1309.png`, `qa/user/midback/bdp_compare.png`). The first WP04 build was clean but held the
  gesture until the push settled.
- **Cause.** The old host scrubbed a running spring with the gesture and had no guard for a gesture during a push
  (`audit/template-navigation.md` findings 3 and 4, `AppNavHost.kt:109-131`).
- **Fix.** The Gmail-style predictive back runs from any moment of a push, starting from the screen's current position;
  a swipe during a pop is counted by `BackGate`. Every entry stays opaque; left- and right-edge swipes move the card
  toward the correct side (`handoff/WP04.md` "Rules"; REQUIREMENTS 32). Device scenario `perf.sh back_during_push`
  (tap, then the swipe 60–120 ms later; asserts no double exposure mid-gesture and that the release lands on the
  list). Recordings: `perf/WP04/back-during-push/{left,right,rtl}`.
- **Why.** Holding the gesture feels unresponsive; scrubbing a spring double-exposes. Handing the push's position to
  the back card keeps one owner of the geometry.

### 2.6 Cold first frames

- **Seen.** Pop first frame 15.0 / 36.9 ms median / max and push/pop p99 57 ms in phase 2, against gates of
  8 / 16 ms and 25 ms; first visit to a tab 22.7 / 27.6 ms (`research/SMOOTHNESS-PLAN.md` §1).
- **Causes and fixes.**

| Cause | Fix | Source |
|---|---|---|
| The merged ART profile had **0 rules for app code**, so shell and design-system code ran interpreted on first use | Baseline profile generated from the journeys (`:app:generateBaselineProfile`, then `tools/perf/split_profile.py`), plus hand-written wildcard rules for the shell and design system as a safety net. Regenerate when a journey changes | SMOOTHNESS-PLAN S2; `handoff/WP04.md` "Measuring" |
| Compose takes an animation's start time from its first frame callback, so a 40 ms composing frame skips 40 ms of spring | The slide's clock starts on the frame **after** the one that composes the new screen; the heavy frame shows nothing moving | S3.1; `handoff/WP04.md` "Rules" |
| The sample detail screen composed three times inside the slide (Idle → Loading → Success → related rows) | **A screen's first composition is final.** Seed it from what the caller or a cache already has (`peek(id)`), and load secondary sections after the slide via `LocalNavigationSettled` (`() -> Boolean`, read in an effect, never in composition) | S3.2–3.3; `handoff/WP04.md` "Rules" |
| Covered screens were moved between scenes and re-provided on pop | Persistent stack scene: covered entries parked (attached, placed, alpha 0), never moved or unplaced; no provider swaps; pages deeper than three leave composition and return from saved state | S1; `handoff/WP04.md` "Rules" |
| A pop drew a hidden full screen every frame | Removed in `d56ad13` (`KeepUnderScene.kt:76-84`) | S5 |
| Downloadable fonts swapped mid-transition | Startup gate runs `AppFontPreload.run(resolver)`; `AppTheme` draws with the answer, decided once per process. Never add `preloaded_fonts` meta-data: it fetches fonts on the main thread before `Application.onCreate` | S6; `handoff/WP03.md` "Rules" |
| Shader and glyph first use (`issue` 43–45 ms on 2 of 4 first visits after `pm clear`) | Warm-up pass after first display; AGSL `RuntimeShader`s created once, off the main thread | S6 **[verify: warm-up pass status]** |
| Tabs composed on first visit | Precompose tab roots in idle frames (`createPausedPrecomposition`) | S7 **[verify: was MISSING at plan time]** |

- **Why.** Every one of these moves work out of the frame where motion starts. The user sees a transition begin on
  time; the cost is paid while nothing moves.

### 2.6a The first frame after idle (WP04-perf, Oct 15)

- **Seen.** On every build measured, each tab switch, push, pop, back gesture and coalesced pop had exactly one janky
  frame: its first. Tab switch 1.15 % janky = 1 frame in 87, hitch ratio 0 ms/s; push/pop p99 31 ms is the first
  frames of a push plus the two or three queued behind them (`handoff/WP04.md` "Phase 3 on the phone").
- **What the frame is made of** (Perfetto, `handoff/WP04-perf.md` §1). The Redmi starts every transition with the
  CPU and the GPU at their idle DVFS levels. The touch-down boost raises the little cluster from 576 to 1248 MHz and
  leaves the big cluster at its 652 MHz floor; the main thread lands on either. The same work that takes 1.8 ms
  (UI) + 2.7 ms (RenderThread) + 5.5 ms (GPU) in the steady frames of the animation takes 6–11 (big core) or 32
  (little core) + 3–10 + 9–15 ms on the first frame, against FrameTimeline's 13.67 ms deadline. SurfaceFlinger tags
  the frame `Non Animating` (it does not count it as jank); HWUI's `FrameDeadline` check does, which is what
  gfxinfo and the gates read. No app API raises the clocks (HWUI's ADPF hint session is active on this ROM and the
  clocks do not move).
- **Causes found and fixed on `wp/WP04-perf`.**

| Cause | Fix | Commit |
|---|---|---|
| A popped entry left every page, so the layout gave it the default `PaneContext`; `LocalPaneContext` is a static local, so the new value composed the whole leaving screen (416 composables) in the pop's first frame | A leaving entry keeps the pane context it had until it has slid off (`rememberPaneContexts`) | `6436b75` |
| `AppTabBar`'s tap handler captured `selected`, so every switch made a new lambda and all ten `TabCell`s recomposed to take it | The handler reads the selection through `rememberUpdatedState`; only the two cells whose selection flips recompose | `1ec7462` |
| The pill was a path of cubics; Skia masks a general path on every frame the two cell layers are clipped to it | The capsule is added with `addRoundRect`, which Skia recognises and clips and fills analytically | `b7e8074` |
| `AppScaffold` painted `colors.background` over `EntryFrame`'s on every frame of every screen: 2.6 Mpx, about a third of a frame's GPU time | `EntryFrame` and the presented frame provide `LocalPaintedBackground`; `AppScaffoldDefaults.background()` is `Unspecified` when the host has painted that colour already | `ac56e69` |
| The hidden tab's lifecycle dropped to CREATED inside the switch frame, stopping every collector it had while the frame composed the switch | Lowered the frame after, as raising already waits two frames | round 2 |
| The sample detail's related rows composed 185 composables 220 ms into every push | They wait for `LocalNavigationSettled` (the documented rule, `SampleDetailEvent.LoadRelated`) | round 2 |

- **Measured.** `handoff/WP04-perf.md` §3 (HEAD against the fixed build and the floor build, same session).
- **Why.** The first frame cannot be made short by the clock, only by the work: every composable, layout pass,
  draw op and filled pixel in it costs two to three times what it costs a frame later.

### 2.7 Glass costs (PeriFlow G1–G17)

PeriFlow's liquid glass (Kyant backdrop) dropped frames: 6.8 % janky while scrolling, and each tab tap re-rendered
about 8 offscreen passes for half a second (`PROGRESS.md` "Audit headlines"). What one Kyant `drawBackdrop` costs:
an `Offscreen` layer per element, re-rendered whenever anything under it is damaged; a new `RenderEffect` chain
whenever a state read inside `effects {}` changes; a re-record on any re-placement (coordinates held in
`neverEqualPolicy` state); shadow and highlight re-recorded on every draw; a new `ShapeProvider` per call, so every
recomposition of the caller updates the node; the scene recorded twice; and `chromaticAberration` sampling 7 times per
pixel (`audit/periflow-learnings.md` §3.2).

| # | Cost found | Fix | Why |
|---|---|---|---|
| G1 | A tab tap pressed the whole bar: ~0.5 s of re-render with `layerBlock`, inner shadow and dispersion | No bar-body `layerBlock`; no inner shadow or dispersion on the tap path. **But see 2.8:** the lens itself must still distort on a tap | The cost was the bar body, not the lens |
| G2 | The bar was torn down and rebuilt on every return to a root (`AnimatedVisibility`), constructing shaders and layers in a pop's first frame | Keep the bar composed for the life of the shell; hide it by translation; alpha 0 only when fully hidden; clear semantics while hidden; process-wide `RuntimeShader` cache | Construction is the expensive part; do it once |
| G3 | Fading a live backdrop | Translate only. If a fade is wanted, fade the tint and content, never an ancestor of `drawBackdrop` | Alpha above a backdrop renders the whole subtree offscreen every frame |
| G4 | Three full-window background fills before content | Drop the stage aura on phones; drop the window background after the first frame | HWUI does no occlusion culling |
| G5 | Aura repainted a CPU bitmap every frame while colours animated | Crossfade two cached pictures | Painting a bitmap on the main thread each frame |
| G6 | Every bar recomposition rebuilt the effect chain | `remember` the items and the backdrop modifier (keyed by scene, tier, colours) | Kyant allocates per call, compared by identity |
| G7 | Ambient animation kept the frame loop at display rate (606–632 frames in 5 idle seconds) | Layer properties only, brushes in `drawWithCache`, `FrameRateCategory.Normal`, wind down after a few seconds, pause when the tab is hidden or reduce motion is on | Idle must render 0 frames |
| G8 | Hidden kept tabs still ticked infinite transitions | Shared animated components stop when `LocalTabVisible` is false | Each tick requests a frame |
| G9 | Drawn glass allocated a `Path`, `Paint` and blurred shadow per draw | `Modifier.Node` with `drawWithCache`; outer-only shadow baked once per (size, shape, density) into an `Alpha8` mask | It is on every card, chip and field |
| G10 | Alpha fades clipped outer shadows ("square shadow corners") | `CompositingStrategy.ModulateAlpha` for entrance fades | No offscreen buffer, no clipping |
| G11 | API 30: glass top bar over content with no fade | Offscreen `GraphicsLayer` masked with a `DstIn` gradient, all API levels | `RenderEffect` blend needs API 31 |
| G12 | "Frosted" still called `drawBackdrop` | The frosted tier draws a tinted bar, no backdrop | Below API 31 the scene is still replayed under the tint |
| G13 | Per-event coroutines and stale captures in the drag lens | One target state + one `collectLatest`; read sizes and layout direction through state | See 2.11 |
| G14 | Scene recorded twice | A backdrop that records the scene once | Halves the scene cost |
| G15 | A second composed tab row only for the lens tint | Record the row once into a `GraphicsLayer`; draw a tinted copy clipped to the lens | A second row costs composition, measure and a colour-filter layer |
| G16 | Dead glass code | Deleted | Bloat |
| G17 | No quality tiers | `GlassQuality` policy (`Live`, `LiveNoLens`, `Frosted`, `Solid`) from API level, low RAM, battery saver, thermal status, a measured GPU verdict and Reduce transparency; signals only lower the tier for the session | Glass must degrade, not stutter |

Sources: `audit/periflow-learnings.md` §3.5 (G1–G17) and §3.6; `handoff/WP11.md` "Rules" 1–13 (as built in PeriFlow
on `overhaul/smooth-glass`). Generator side: WP05 owns `foundation/glass/` **[verify against WP05's merged code]**.

PeriFlow's own earlier fixes are the strongest evidence for what glass cannot afford: live backdrops inside screens,
masked multi-band blur, gradients replayed inside backdrops, and fades over glass screens. Content glass drawn instead
of sampled took scrolling from 67 % janky to 12.5 %; painting the aura once at quarter resolution took it to 6 %
(`audit/periflow-learnings.md` §3.3, commits `4386048`, `f0eed93`).

### 2.8 The lens tap lesson: never remove a visible effect to save cost

- **Seen.** After the G1 fix, tapping a glass tab no longer refracted or distorted anything; the user wanted it back
  (REQUIREMENTS 35, Oct 12). The phone was on the `Live` tier, so no downgrade was involved.
- **Cause.** G1 limited swell, refraction and sheen to a held (120 ms) or dragged lens. G1's real cost was the whole
  bar re-rendering for about 0.5 s with inner shadow and dispersion, not the lens.
- **Rule.** When an effect is expensive, find the part that costs and make it cheap. Do not delete the part people see.
  Removing a visible effect breaks REQUIREMENTS 21 ("smoothness must not come at the expense of animation").
- **Fix** (REQUIREMENTS 35): on a tap the lens travels to the new tab as a liquid droplet: it lifts and swells
  slightly, refracts and magnifies what is under it, stretches along the direction of travel and settles with the
  selection spring, within about 0.3–0.4 s.
  - Only the lens samples, within its own bounds, from the scene recorded once. The bar body is never re-rendered.
  - No dispersion or inner shadow on the tap path; the pressure pulse is driven in draw.
  - The same path serves taps and programmatic selection (back stack, deep link).
  - `LiveNoLens` and lower tiers keep a lens-free tap. A drag keeps the full press behaviour.
  - Gate: with `glass.quality=Live`, tab-tap janky % and GPU p90 within the `glass_live` gate.
- **Status.** PeriFlow committed `58c17e0`; the generator's lens was in progress with 6 files uncommitted at 14:50 on
  Oct 12 (`PROGRESS.md`) **[verify]**. Strips: `perf/WP11/lens-tap/`.

### 2.9 Recomposition during animations

- **Seen / cause.** Values that change every frame were read in composition, so the subtree recomposed every frame.
  Root causes found (`research/RECOMPOSITION-PLAN.md` §6 item 10):
  - the static window-size local held raw dp, recomposing the whole app with skipping off on every resize step
    (`theme/AppWindowSize.kt:28-55`, SMOOTHNESS-PLAN S14);
  - the old bar's pill position read in composition;
  - the pull-to-refresh offset read in composition (REQUIREMENTS 29);
  - the predictive-back sample's composition read;
  - provider swaps when a screen moved between scenes;
  - data-layer models without stability.
- **Fix.** Animated, dragged or scrolled values are read only in `offset {}`, `layout {}`, `graphicsLayer {}`, draw
  blocks or a `Modifier.Node`. Components take fast-changing inputs as `() -> T`. Changing values reach the tree
  through a static local holding a `@Stable` holder with snapshot fields. Lints `FrequentlyChangingValue` and
  `UseOfNonLambdaOffsetOverload` fail the build (RECOMPOSITION-PLAN §6 rules 1–5).
- **Verified by** the JVM harness: "quiet between the ends" (frames 2…N−1 execute nothing), duration invariance
  (same counts under Snappy and Calm motion), hidden tabs and parked entries silent (RECOMPOSITION-PLAN §3.2), and
  on the device by `perf.sh recompose` (§4).
- **Why.** Recomposition is main-thread work in the frame. A draw-phase read invalidates only the draw.

### 2.10 Ripple and press overlays

- **Seen.** A grey 9 % overlay was drawn on every touchable; filled buttons stacked three press effects; tab items
  flashed a grey rectangle over the whole tab column (`PROGRESS.md` "Audit headlines"; `audit/template-designsystem.md`
  §1.1).
- **Cause.** `AppTheme` provided a full-bounds `AppIndication` as the global `LocalIndication`, and every
  `appClickable`, `selectable` and `toggleable` without an explicit indication used it. There was no feedback
  taxonomy (`audit/template-designsystem.md` §1, `theme/AppTheme.kt:66,87`, `foundation/AppIndication.kt:74-80`).
- **Fix.** `LocalIndication` provides `NoIndication`. Every control states its feedback:
  `Modifier.pressable(onClick, feedback = …)`. Rows use a row highlight; bars, tabs, segments and selection rows use
  none (the selection motion is the feedback); foundation's `selectable`/`toggleable` take `indication = null`
  explicitly. `accept.sh` fails on a bare `.clickable(` in `feature` or `core/ui` (`handoff/WP03.md` "Rules" and
  "Remaining work"; `audit/template-designsystem.md` §1).
- **Why.** iOS press feedback is subtle and spring-based (REQUIREMENTS 2). A full-bounds overlay is a flash, not
  feedback, and three stacked effects are three draws per frame of the press.

### 2.11 Stale gesture captures (the devtools drag)

- **Seen.** The devtools API-log badge could not be dragged. On the Redmi a 503×1038 px swipe moved it 7×14 px
  (`PROGRESS.md` "Audit headlines"; REQUIREMENTS 8).
- **Cause.** `detectDragGestures` in `DraggableBadge` (`core/devtools/.../DevBadge.kt:50-102`) captured copies of the
  offset when the gesture coroutine started. `pointerInput` outlives recompositions, so each per-event delta was added
  to the stale base and the badge snapped back (`audit/template-infra.md` §1). PeriFlow's drag lens had the same class
  of bug: `tabWidth` and `isLtr` captured at gesture start went stale after rotation, resize or RTL changes
  (`audit/periflow-learnings.md` G13).
- **Fix.** Accumulate drags in state that is read at placement (`offset { }`); read sizes and layout direction
  through state (`rememberUpdatedState`); one target state with a single collector, not one coroutine per event
  (`handoff/WP06.md` "Rules"; `handoff/WP11.md` "Rules" 14).
- **Why.** A gesture runs for many frames across many compositions; anything it captured at the start is a snapshot
  of the past.

### 2.12 Pull to refresh

- **Seen.** A tiny 38 dp disc over the content's text; the content did not move; the disc's elevation shadow showed
  through the fading surface as a black ring, worst in dark mode; the offset was read in composition, so it
  recomposed every frame (REQUIREMENTS 29; recorded in `qa/ptr/`).
- **Fix** (`handoff/WP03.md` "Orchestrator note: motion-perf" item 3; `research/MOTION-PLAN.md` M20): one
  `Modifier.Node`. The content follows the finger with rubber-band resistance; the spinner is revealed progressively;
  a haptic tick at the 100 dp trigger during the drag; on release the content rests at 60 dp while refreshing, then
  springs back on `bounceBack` seeded with the finger's velocity; no surface disc, no shadow; the spinner in its own
  layer.
- **Why.** The shadow was a shadow under a translucent surface; removing the surface removes the bug. A node that
  reads the offset in layout and draw recomposes only when a refresh starts and ends.
- **Measured** (§3): 1.10 % / 1.08 % janky (light / dark) → 0.54 % / 0.53 %, GPU p90 9 / 7 ms → 6 / 6 ms; 36
  `Compose:recompose` slices for a whole set, only at refresh start and end (`handoff/WP03.md` "Measurements").

### 2.13 The per-element shimmer

- **Seen.** "Shimmer has issues" (REQUIREMENTS 34). Every skeleton bone swept its own band: a band 60 % of its own
  width, across its own width, every 1.4 s. Bones of different widths moved at different speeds, so one list showed
  twelve flickers out of step instead of one light crossing it. The highlight was +4.3 ΔL* in light (barely visible)
  and +13.7 ΔL* in dark over bones only 4.9 ΔL* off the card (`qa/shimmer/AUDIT.md` §1, S-1, S-2; phone frames in
  `qa/shimmer/evidence/`).
- **Cause.** `ShimmerClock` shared only a phase from 0 to 1; each bone turned it into a position, width and speed from
  its own size (`AppSkeleton.kt:102-114`, `BandFraction = 0.6f`; `qa/shimmer/AUDIT.md` S-1).
- **Fix.** One band in root (window) coordinates: one clock gives the band's x; each bone reads `positionInRoot()` in
  its draw and paints the band at `bandX - origin.x`. Band width, angle, period and highlight are tokens per theme.
  A `Modifier.Node`, zero recomposition per frame; it pauses when not visible. Skeletons are shaped like the content
  they stand for, so nothing jumps when it arrives (REQUIREMENTS 34; `qa/shimmer/AUDIT.md` S-1, S-2, S-5, K-1). Built on
  `wp/WP03-skel` **[verify merged]**.
- **Why.** iOS and every mature shimmer library (Facebook Shimmer, compose-shimmer, Wear Compose Material 3) draw one
  band in a shared space (`qa/shimmer/AUDIT.md` §6.1).

### 2.14 Content cut by padding instead of `contentPadding`

- **Seen.** Skeleton cards showed behind the status bar and were cut by the large title; the last card was cut by the
  old shell's opaque strip; at font scale 1.5 the search screen's recent chips were cut at the bottom
  (`qa/shimmer/AUDIT.md` §1, C-1, C-5; REQUIREMENTS 17, 34).
- **Cause.** `chromeContentPadding()` was opt-in per call, and nothing failed when a scrolling container inside an
  `AppScaffold` left it out. Lists used `PaddingValues(gutter)` or `Modifier.padding`, so they started under the top
  chrome and ended under the bar (`qa/shimmer/AUDIT.md` C-1 table).
- **Fix.** Insets and chrome go in the scrolling container's `contentPadding`, never in a parent's padding: content
  scrolls under floating chrome and only the scroll content is inset. `AppPagingList` defaults to
  `chromeContentPadding(PaddingValues(gutter))`; `AppStateView` pads inside its scroll; non-scrolling start content
  scrolls. Proposed code rule `scroll-chrome`: a `LazyColumn`, `LazyVerticalGrid` or `verticalScroll` in a feature's
  `AppScaffold` takes `chromeContentPadding` (`qa/shimmer/AUDIT.md` C-1 fix 1–6) **[verify the rule exists]**.
- **Why.** Parent padding clips: the content can never reach the area under the chrome. `contentPadding` insets the
  first and last items but lets the list scroll edge to edge.

---

## 3. Measured before/after (interim; final numbers come from WP13)

> **Interim.** These are the numbers measured so far. The final before/after table (old template vs new for the
> everything, standard and lean presets, and PeriFlow before vs after, REQUIREMENTS 20) comes from WP13. Replace this
> section with WP13's table; keep the conditions line.

**Conditions.** Redmi Note 10 Pro (`9a7c74f4`), 120 Hz, release builds (`devBenchmarkRelease` or `devRelease`),
speed-profile compiled, 3 runs, median of runs; "janky" is the FrameTimeline count from `dumpsys gfxinfo`, not the
legacy 16 ms count (`design/MASTER-PLAN.md` §2). Emulator runs are relative evidence only and never quoted as results
(`handoff/WP04.md` "Measurements").

### 3.1 Shell: old template vs the new shell at phase 2 (everything preset)

Source: `STATUS.md` §4; raw `perf/WP04/baseline/summary.md` (old) and `perf/WP04/phase2/summary.md` (new, phase 2).
Phase 2 predates parking (S4), the persistent stack scene (S1), the baseline profile (S2) and the clock after
composition (S3.1); phase 3 is pending (`perf/WP04/phase3b-r10-partial/` holds a partial run).

| Metric | Old template | New, phase 2 | Gate (solid) | Target |
|---|---|---|---|---|
| Rapid Back lands on the expected screen | no | **yes** | yes | yes |
| Rapid Back, janky | 5.32 % | 2.62 % | ≤ 4 % | ≤ 1 % |
| Tab switch to a visited tab, first frame median / max | 19.0 / 45.3 ms | **6.5 / 16.5 ms** | ≤ 8 / ≤ 16 ms | ≤ 8 / 8.3 ms |
| Tab switch, janky | 1.58 % | 1.9 % (worse) | ≤ 1.0 % | ~0 % |
| Push/pop with the Back key, janky / p99 | 4.03 % / 34 ms | 2.34 % / 57 ms (p99 worse: cold pop) | ≤ 2.0 % / ≤ 25 ms | 0–0.5 % / ≤ 16 ms |
| Pop, first frame median / max | 19.1 / 27.6 ms | 15.0 / 36.9 ms | ≤ 8 / ≤ 16 ms | — |
| Push/pop with the back gesture, janky | 3.54 % | 2.82 % | ≤ 2.0 % | ~0 % |
| Scroll a root list, janky / GPU p90 | 0.22 % / 6 ms | 0.15 % / 6 ms | ≤ 0.5 % / ≤ 7 ms | — |
| Scroll with the bar minimizing, janky | — | 0.3 % | ≤ 0.5 % | — |
| Idle 5 s on every tab, frames | 0 | 0 | 0 | 0 |
| Cold start, speed-profile median | 336 ms | 355 ms (no baseline profile yet) | ≤ 380 ms | below 336 ms |
| Cold start as installed | — | 416 ms | ≤ 400 ms | — |
| PSS after the journey | 109.8 MB | 93.8 MB | ≤ 70 MB | ≤ 70 MB |

### 3.2 Old template, standard and lean presets (baselines)

Source: `perf/WP04/baseline-standard/summary.md`, `perf/WP04/baseline-lean/summary.md` (`devRelease`; lean has one
tab, so tab rows are skipped). New-build numbers for these presets: pending (WP04 phase 3, then WP13).

| Metric | Standard (old) | Lean (old) |
|---|---|---|
| Push/pop with the Back key, janky / p99 | 4.73 % / 30 ms | 4.73 % / 32 ms |
| Pop, first frame median / max | 11.96 / 20.18 ms | 10.66 / 16.08 ms |
| Tab switch, janky | 1.46 % | — |
| Tab switch to a visited tab, first frame median / max | 20.52 / 49.93 ms | — |
| Cold start, speed-profile median | 271 ms | 306 ms |

### 3.3 Pull to refresh (WP03)

Source: `handoff/WP03.md` "Measurements" table; raw `perf/WP03/ptr/measure-before/`, `r8-head-2/`, `r8-after-2/`.
Target: ≤ 1 % janky and GPU p90 ≤ 7 ms while pulling.

| Build | Theme | Frames | Janky | p50 / p90 / p99 | GPU p90 |
|---|---|---|---|---|---|
| Before: the old control (phase-2 build, Oct 10) | light | 545 | 1.10 % | 9 / 15 / 21 ms | 9 ms |
| | dark | 557 | 1.08 % | 9 / 11 / 21 ms | 7 ms |
| After: rebuilt control, spinner in its own layer (`4a3f9e4`) | light | 741 | **0.54 %** | 7 / 8 / 13 ms | **6 ms** |
| | dark | 749 | **0.53 %** | 8 / 8 / 13 ms | **6 ms** |

The remaining 4 janky frames per set are the first frame of each pull: a full redraw after idle. The first rebuild
(Oct 9, `perf/WP03/ptr/after/`, recorded while `screenrecord` ran) measured 16.81 % janky in light; it is superseded.
`STATUS.md` §4's "16.8 % (light) / 8.2 % (dark)" row is that first rebuild.

### 3.4 PeriFlow before (WP11)

Source: `perf/WP11/before/summary.md` (gates in `perf/WP11/gates.toml`, column `periflow`). After numbers: pending
(`perf/WP11/after-early*/` hold runs without a summary; `perf/WP11/after/` comes from WP11's acceptance).

| Metric | PeriFlow before | Gate |
|---|---|---|
| Tab switch, janky | 1.84 % | ≤ 2.5 % |
| Push/pop with the Back key, janky | 4.53 % | ≤ 3 % |
| Push/pop with the back gesture, janky | 3.62 % | ≤ 3 % |
| Rapid Back, janky | 2.68 % | ≤ 4 % |
| Scroll Today, janky | 0.14 % | ≤ 2 % |
| Idle 5 s on every tab, frames (most on one tab) | 632 | 0 |
| Cold start, speed-profile median | 495 ms | ≤ 400 ms |
| PSS after the journey | 224.7 MB | ≤ 90 MB |

---

## 4. Forbidden patterns, with do and don't

Each pattern names the root cause in §2 it comes from. API names follow the sources; WP12 checks them against the
merged code (`chromeContentPadding`, `pressable`, `AppTheme.springs`, `LocalNavigationSettled`).

### 4.1 Animated values read in composition (§2.9)

```kotlin
// DON'T: recomposes the subtree on every frame of the animation
val offset by animateDpAsState(target)
Box(Modifier.offset(y = offset))

// DO: read the value in the layout phase; nothing recomposes
val offset = animateDpAsState(target)
Box(Modifier.offset { IntOffset(0, offset.value.roundToPx()) })
```

Components that take a fast-changing value take a lambda: `fun AppHeader(collapse: () -> Float)`, not
`collapse: Float` (`research/RECOMPOSITION-PLAN.md` §6 rule 2).

### 4.2 Alpha on a full-screen layer (§2.3)

```kotlin
// DON'T: renders the whole screen offscreen every frame of the fade
Box(Modifier.fillMaxSize().graphicsLayer { alpha = shown.value }.background(scrim))

// DO: a drawn rect, one blended fill
Box(Modifier.fillMaxSize().drawBehind { drawRect(scrim, alpha = shown.value) })

// DO, for a card or row entrance fade: no offscreen buffer, shadows not clipped
Modifier.graphicsLayer {
    alpha = appear.value
    compositingStrategy = CompositingStrategy.ModulateAlpha
}
```

Never fade a screen on push or pop, and never put alpha < 1 on an ancestor of a glass backdrop
(`research/SMOOTHNESS-PLAN.md` S8, §6 rule 6; `audit/periflow-learnings.md` G3, G10).

### 4.3 Hiding a tab, a covered screen or glass chrome by removing it (§2.1, §2.2, G2)

```kotlin
// DON'T: disposes the bar; the next show builds layers and shaders in a pop's first frame
AnimatedVisibility(visible = onRoot) { AppTabBar(…) }

// DON'T: skipping placement drops every display list the hidden tab had
if (index == selected) placeable.place(0, 0)

// DO: keep it composed and placed; park it (layer alpha 0, off the window, semantics cleared)
Modifier.parked { index != selected }
```

`Modifier.parked` is the shell's helper (SMOOTHNESS-PLAN S4) **[verify name]**. Features never do this themselves:
the shell keeps tabs and stacks.

### 4.4 Swapping content inside a transition (§2.6)

```kotlin
// DON'T: starts Idle, then Loading, then Success, then related rows: four compositions inside the slide
class DetailViewModel(…) : MviViewModel<DetailState>(initialState = DetailState())
LaunchedEffect(Unit) { viewModel.onEvent(Appeared) }

// DO: the first composition is final. Seed from what is already in memory
class DetailViewModel(repository: ItemRepository, key: DetailKey) : MviViewModel<DetailState>(
    initialState = repository.peek(key.id)?.let { DetailState(item = it, loadState = Success) } ?: DetailState(),
)

// DO: load secondary sections after the slide settles; read the signal in an effect, never in composition
val settled = LocalNavigationSettled.current
LaunchedEffect(key) {
    snapshotFlow { settled() }.first { it }
    viewModel.onEvent(LoadRelated)
}
```

Source: `research/SMOOTHNESS-PLAN.md` S3.2; `handoff/WP04.md` "Rules".

### 4.5 Changing a CompositionLocal or lifecycle on a transition frame (§2.1, §2.9)

```kotlin
// DON'T: every reader recomposes on the switch frame
CompositionLocalProvider(LocalTabVisible provides (index == selected)) { … }

// DO: a static local holding a stable holder; readers read the snapshot field in draw or in an effect
@Stable class TabVisibility { var visible by mutableStateOf(false) }
val LocalTabVisibility = staticCompositionLocalOf { TabVisibility() }
```

Static locals only for values that never change at runtime (`research/RECOMPOSITION-PLAN.md` §6 rule 5;
SMOOTHNESS-PLAN §6 rule 3).

### 4.6 Ripple and implicit indication (§2.10)

```kotlin
// DON'T: no feedback at all under NoIndication, or a grey flash where an indication is provided
Modifier.clickable { open(item) }
Modifier.selectable(selected, onClick = onSelect)

// DO: every control states its feedback
Modifier.pressable(onClick = { open(item) }, feedback = PressFeedback.Highlight)     // a row
Modifier.selectable(selected, indication = null, interactionSource = null, onClick = onSelect)  // a tab or segment
```

Source: `handoff/WP03.md` "Rules"; `audit/template-designsystem.md` §1 (taxonomy). `accept.sh` fails on a bare
`.clickable(` in `feature` or `core/ui` **[verify]**.

### 4.7 Gesture state captured at gesture start (§2.11)

```kotlin
// DON'T: x and y are copies captured when the pointer coroutine started; pointerInput outlives recompositions
var offset by remember { mutableStateOf(Offset.Zero) }
val (x, y) = offset
Modifier.pointerInput(Unit) { detectDragGestures { _, drag -> offset = Offset(x + drag.x, y + drag.y) } }

// DO: accumulate into state, read it at placement
val offset = remember { mutableStateOf(Offset.Zero) }
Modifier
    .offset { offset.value.round() }
    .pointerInput(Unit) { detectDragGestures { _, drag -> offset.value += drag } }
```

Sizes and layout direction used inside a gesture are read through `rememberUpdatedState` (`handoff/WP11.md` "Rules"
14; `audit/periflow-learnings.md` G13).

### 4.8 Padding that clips scrolling content (§2.14)

```kotlin
// DON'T: starts under the large title, ends under the floating bar, clipped by the parent
LazyColumn(Modifier.padding(gutter)) { … }
LazyColumn(contentPadding = PaddingValues(gutter)) { … }

// DO: chrome and insets go in contentPadding; the list scrolls edge to edge under the chrome
LazyColumn(contentPadding = chromeContentPadding(PaddingValues(gutter))) { … }
```

Never pad a root screen above the bar; never paint a strip behind the bar (REQUIREMENTS 17; `handoff/WP04.md`
"Rules"; `qa/shimmer/AUDIT.md` C-1).

### 4.9 Allocations in per-frame draw code (SMOOTHNESS-PLAN S9; G9)

```kotlin
// DON'T: a new native gradient every frame of every push and pop
Modifier.drawBehind { drawRect(Brush.horizontalGradient(listOf(edge.copy(alpha = a), Color.Transparent))) }

// DO: build once per size; vary only alpha
Modifier.drawWithCache {
    val brush = Brush.horizontalGradient(listOf(edge, Color.Transparent))
    onDrawBehind { drawRect(brush, alpha = progress()) }
}
```

### 4.10 A gesture that ends on a tween (SMOOTHNESS-PLAN S13)

```kotlin
// DON'T: a speed discontinuity at the release; gfxinfo does not count it, people see it
offset.animateTo(target, tween(300))

// DO: settle on a spring seeded with the finger's velocity
offset.animateTo(target, AppTheme.springs.bounceBack.pixels(), initialVelocity = velocity)
```

Springs come from `AppTheme.springs`; never `tween(300)` (`handoff/WP03.md` "Rules") **[verify accessor names]**.

### 4.11 Per-element shimmer (§2.13)

```kotlin
// DON'T: each bone computes its own band from its own width; bones drift out of step
val x = -band + (size.width + band) * clock.progress   // band = size.width * 0.6f

// DO: one band in window coordinates; each bone offsets by its own position
val origin = coordinates.positionInRoot()
drawRect(bandBrush, topLeft = Offset(bandX - origin.x, 0f), size = Size(bandWidthPx, size.height))
```

Use the design system's skeleton; do not write a shimmer in a feature (`qa/shimmer/AUDIT.md` S-1, §6.3).

### 4.12 Also forbidden (one line each)

| Don't | Why | Source |
|---|---|---|
| A second full-screen background under a screen | `EntryFrame` already paints one; HWUI does no occlusion culling | S5; G4 |
| Infinite animations that keep running when hidden or idle | Idle must render 0 frames | G7, G8; gate "Idle 5 s" |
| Animating the colours of a painted background | Paints a bitmap on the main thread every frame | `handoff/WP11.md` "Rules" 12 |
| `<meta-data android:name="preloaded_fonts">` | Fetches fonts on the main thread before `Application.onCreate` | `handoff/WP03.md` "Rules" |
| Lazy `items` without `key`; mixed lists without `contentType` | Reuse and prefetch break | S11; RECOMPOSITION-PLAN §6 rule 6 |
| `-assumenosideeffects` for Compose trace calls | The recomposition harness needs the markers | S12 |
| Locking orientation or resizability | Adaptive requirement; the activity is never recreated by a resize | `handoff/WP04.md` "Rules" |
| Removing a visible effect to save cost | Breaks REQUIREMENTS 21; make it cheap instead | REQUIREMENTS 35 |
| A static CompositionLocal whose value changes on a transition frame (a pane context, a lifecycle owner, a holder swapped for another) | Every reader in the subtree recomposes in the frame that starts the motion | §2.6a |
| A handler lambda that captures the selection, index or page it acts on | A new lambda per change, so every cell or row that takes it recomposes | §2.6a |
| A clip or fill path built from lines and arcs where a rect or rounded rect would do | Skia clips rects and rounded rects analytically and masks everything else | §2.6a |
| Painting a screen's background where the host already has (`LocalPaintedBackground`) | One more full-window fill per frame; HWUI does no occlusion culling | §2.6a |

---

## 5. How to measure

**Release builds only, never debug.** Debug builds were measured 10× jankier than release builds, so a debug number
says nothing about what people get (`PROGRESS.md` "Audit headlines"; `audit/device-baseline.md` F7). Measure
`devBenchmarkRelease` when the project has the benchmark module, otherwise `devRelease`, compiled with its baseline
profile, on a real phone (`handoff/WP04.md` "Measuring").

**Tools** (shipped with the `performance` feature under `tools/perf/`; `handoff/WP04.md` "perf script usage"):

```bash
# Gate run: 3 runs, the median is gated
tools/perf/perf.sh --package <appId>.dev --variant devBenchmarkRelease --runs 3 --out perf/run1 \
    --gate solid tabs tabs_tap pushpop pushpop_gesture back_during_push rapid_back scroll_root idle meminfo coldstart

# Frame strips of one journey (on the Redmi always 720x1600: the encoder fails at full resolution)
tools/perf/record.sh <appId>.dev pushpop out/strips

# After a journey changes: regenerate the baseline profile, then split it into the modules
./gradlew :app:generateBaselineProfile
python3 tools/perf/split_profile.py
```

- **Scenarios:** `tabs`, `tabs_tap`, `tab1`, `pushpop`, `pushpop_gesture`, `back_during_push`, `rapid_back`,
  `scroll_root`, `scroll_minimize`, `idle`, `nav_stress`, `restore`, `meminfo`, `recompose`, `trace`, `coldstart`,
  `coldstart_installed`, and the `large` ones for big windows.
- **Gate columns:** `solid`, `standard`, `lean`, `smoke`, `large`.
- `perf.sh` refuses a locked phone and measures taps from the finger's lift. `--compile speed` compiles every method
  ahead of time, for an A/B against the profile.
- **Recomposition:** the JVM harness (`./gradlew testDebugUnitTest`, tests named `*Recomposition*`) and
  `perf.sh recompose` on the device. Layout Inspector counts are for exploration only (RECOMPOSITION-PLAN §3.4, §6
  rule 9).
- **What to report:** FrameTimeline janky %, frame-time p50 / p90 / p99, GPU p90, first-frame UI time, cold start, PSS.
  Where a number is close to its gate, also report the hitch ratio, FrameTimeline jank and GPU / `dequeue` time
  (SMOOTHNESS-PLAN §6 rule 1).
- **Conditions:** install, `pm clear`, a warm-up pass that visits every tab once (first visits are their own row), then
  the scenario; three runs; battery under 37 °C (`design/MASTER-PLAN.md` §2).
- **Glass:** measure with the tier pinned (`--extra glass.quality=Live`) and also unpinned. An unpinned run can lower
  its own tier partway through and pass by removing glass (`handoff/WP11.md` "Rules" 10).
- **Overdraw and offscreen checks** on every new transition: `setprop debug.hwui.overdraw show`,
  `debug.hwui.show_layers_updates true`, and Perfetto `alpha caused saveLayer` slices (SMOOTHNESS-PLAN S5).
- **Emulator numbers are not results.** The emulator's GPU columns read 4950 ms and its janky % is 36–98 % on every
  build; use it for correctness and motion shape only (`handoff/WP04.md` "Measurements").

---

## 6. Budgets and gates

Gates are machine-checked: `tools/perf/gates.toml` holds every row, and `perf.sh --gate <column>` exits non-zero and
prints each failing row (`design/MASTER-PLAN.md` §2). Presets: E = everything, S = standard, L = lean.

| Scenario | Gate, solid chrome | Gate, glass `Live` | Presets |
|---|---|---|---|
| Tab switch, janky (21 switches) | ≤ 1.0 % | ≤ 2.5 % | E S L |
| Tab switch to a visited tab, first frame median / max | ≤ 8 / ≤ 16 ms | ≤ 10 / ≤ 20 ms | E S L |
| First visit to a tab, first frame | ratchet | ratchet | E |
| Push, first frame median / max | ratchet, target ≤ 12 / ≤ 20 ms | ratchet | E |
| Pop, first frame median / max | ≤ 8 / ≤ 16 ms | ≤ 10 / ≤ 20 ms | E S |
| Push/pop with the Back key, janky / p99 | ≤ 2.0 % / ≤ 25 ms | ≤ 3.0 % / ≤ 30 ms | E S L |
| Push/pop with the back gesture, janky | ≤ 2.0 % | ≤ 3.0 % | E |
| Rapid Back | no crash, expected final screen, no blank or double-exposed frame, janky ≤ 4 %, coalesced pop first frame ≤ 8 / ≤ 16 ms | same, first frame ≤ 10 / ≤ 20 ms | E |
| Navigation stress (60 s of seeded random taps and Back bursts) | no crash, stack invariants hold, janky ≤ 4 % | same | E |
| Restore after process death | tab, stacks and presented sheet restored; Back pops correctly | same | E |
| Scroll a root list, janky / GPU p90 | ≤ 0.5 % / ≤ 7 ms | ≤ 2.0 % / ≤ 8 ms | E S |
| Scroll with the bar minimizing | ≤ 0.5 % / ≤ 7 ms | ≤ 2.0 % / ≤ 8 ms | E |
| Idle 5 s on any tab | 0 frames | 0 frames | E S L |
| Cold start, speed-profile median | ≤ 380 ms | ≤ 400 ms | E S L |
| Cold start as installed | ≤ 400 ms | ≤ 420 ms | E S L |
| PSS after the journey | ≤ 70 MB | ≤ 80 MB and ≤ solid + 10 MB | E S |
| `Frosted` tier against `Solid` | — | janky ≤ solid + 0.3 pp, GPU p90 ≤ solid + 1 ms, PSS ≤ solid + 5 MB | E |

- **Ratchet rows:** WP04's final median + 10 % is written into `gates.toml`, so no later change can regress them.
  WP12: copy the final values from `tools/perf/gates.toml` in the merged template **[verify]**.
- **The gates are the floor.** The goal is ~0 % janky on tab switches, push/pop, back and scroll (REQUIREMENTS 19).
- **Glass additions:** `pushpop_gesture` on `everything-glass` at ≤ 3 % janky and GPU p90 ≤ 8 ms (SMOOTHNESS-PLAN S15);
  tab-tap janky % and GPU p90 within `glass_live` with the travelling lens on (REQUIREMENTS 35).
- **Recomposition budgets** (device, `perf.sh recompose`): ≤ 2 recomposing frames per push, pop, tab switch and
  back-card commit; 0 per back-gesture progress frame; ≤ 1 per sheet detent; scroll is report-only
  (RECOMPOSITION-PLAN §4).
- **Recomposition budgets** (JVM harness; RECOMPOSITION-PLAN §3.3):

| Journey | Budget |
|---|---|
| Tab switch to a visited tab | both tabs' screens 0; `TabCell` ≤ 2; bar, row and `KeptTabs` ≤ 1; frames 2…N 0 |
| Push | source 0; destination ≤ 1 per composable, state unchanged during the slide; bar 0; frames 2…N−1 0 |
| Pop | the screen beneath 0; the leaving screen 0; frames 2…N−1 0 |
| Predictive back scrub | 0 per progress event in both screens and the bar; `BackCardHandler` ≤ 2 |
| Rapid Back ×3 within one pop | same totals as one pop; intermediate entries 0 |
| Sheet drag across detents | content and presenter 0 per move; ≤ 1 at a detent settle |
| Press feedback (button, icon button, row, card, chip) | 0 |
| Switch, segmented, picker drag | parent 0; control ≤ 1 per committed value |
| Scroll with large title and bar minimize | visible rows 0; entering rows ≤ 1 each; large title 0 per frame; bar ≤ 2 |
| Pull to refresh | rows and screen root 0 during the drag; ≤ 2 for `refreshing`; spinner 0 |
| Idle 5 s on each tab | 0 anywhere |
| Window resize within one size class | 0 outside composables that read raw width |
| Per generated feature | visible rows 0 during a scroll; the destination composes once |

  Invariants that apply to every animated journey: quiet between the ends, duration invariance under Snappy and Calm
  motion, hidden tabs and parked entries silent (RECOMPOSITION-PLAN §3.2).
- **Size budgets:** `devRelease` arm64 APK and dex size per preset, measured after WP02-M2 and WP06 merge + 5 %, in
  `tools/size_budgets.toml`; `tools/matrix.py` fails on a breach (`design/MASTER-PLAN.md` §2) **[verify]**.
