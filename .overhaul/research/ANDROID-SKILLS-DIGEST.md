# Google's Android skills: the rules that apply to this project

Source: `~/.claude/plugins/marketplaces/android-skills/` (github.com/android/skills, Apache-2.0, **v1.0.15**, commit
`5bda39f154fb859a08d45e2392f3568e049f83fb`, Oct 14 2026; the templates still vendor v1.0.13 `42dc2270e9…`). Read the
full SKILL.md and its `references/` before implementing anything in that area. This digest lists only the rules that
change what we build, who owns each, and whether the branches already follow them.

**v1.0.13 → v1.0.15 in one paragraph.** Five new skills: `jetpack-compose/jetpack-compose-performance` (an audit
skill with seven rule categories and nine reference files), `system/glance-canonical-layouts` (Google's widget
layouts as copyable sources), `wear/wear-widgets`, `cars/android-for-cars-parked-apps` and
`device-ai/litert/litert-runtime`. Ten updated: `android-profiler` was restructured (orchestrators, a session-based
`trace_processor`, evidence tags, the hints moved), `navigation-3` changed two recipes, `adaptive` and `testing-setup`
changed references, and `navigation-event`, `camerax`, `engage-sdk-integration`, `play-billing-library-version-upgrade`,
`wear-compose-m3` and the XR glimmer skill changed only metadata or reference text. The repository now ships
`.claude-plugin/plugin.json` listing all 30 skills, and the README documents `/plugin install android-skills
--marketplace android/skills`.

**Status words.** APPLIED, PARTIAL, MISSING, with `file:line` relative to `templates/android/` on the named branch;
[U] marks something nobody has verified yet. Owners: WP02 (build, rules, scaffold, vendoring), WP03 (design system),
WP04 (navigation, shell, perf), WP04-perf (the smoothness deep work on `wp/WP04-perf`), WP05 (glass), WP06 (data and
security), WP07 (Compose Multiplatform), WP12 (docs), WP13 (verification).

## jetpack-compose-performance (WP04-perf first; WP03, WP04, WP05 for their code; WP02 for the rules and the scaffold; WP07 parity; WP12 docs; WP13 gates)

### What it is, and how agents must use it

- It is an **audit workflow**, not a tutorial: scan every `@Composable`, modifier chain and lazy-item model against all
  seven categories, and never stop at the first finding. **Before flagging or editing anything a category matches,
  read that category's `references/*.md`**; the summary bullets are not enough to write a fix. This binds our review
  loops: a reviewer who cites the skill cites the reference file, and the `AGENTS.md` row says the same.
- **Foundations it demands before micro-optimising:** release builds only for profiling and benchmarks (we do:
  `tools/perf/perf.sh`, `devBenchmarkRelease`), R8 on (we do, full mode), and **baseline profiles for the critical
  journeys**. On `wp/WP04-perf` the wildcard profiles exist (`app/src/main/baseline-prof.txt`,
  `core/designsystem/src/main/baseline-prof.txt`, `core/navigation/src/main/baseline-prof.txt`); on `wp/WP02` they do
  not yet (they arrive with WP04's merge; the `baselineprofile` feature owns only `benchmark`, so the wildcard files ship
  with every project). The generated profile stays behind the `baselineprofile` feature, which defaults to off: WP02
  and WP04 decide whether the skill's "foundational prerequisite" makes it default on.
- Versions that change which rule applies: Compose BOM 2026.09.00 = Compose 1.12.1 (`libs.versions.toml:23`), so
  **`derivedStateOf`, not `computedStateOf`** (1.13+); **keyed `SideEffect(keys)` is available** (1.12+);
  `Modifier.onVisibilityChanged` is available (1.9+); `mutableStateSetOf` is available (1.8+).

### What it says about the first frame after idle

The measured problem (`handoff/WP04-perf.md` §1): the first frame after the app has been idle runs with the main
thread on a big core at its 652 MHz floor (or a little core at 1248 MHz) and the GPU at its idle clock, so the same
work that takes 1.8 ms UI + 2.7 ms RenderThread + 5.5 ms GPU in steady state takes 6–11 + 3–10 + 9–15 ms and misses
FrameTimeline's 13.67 ms deadline. No app API moves the clocks (ADPF is already on). The skill never mentions DVFS,
but its central claim is the right frame for that problem: **"leaving the house is the bulk of the cost"**. Once a
composition is scheduled, skipping one more composable saves little; the wins are not scheduling composition at all
(phase deferral), making whatever does run cheap, and judging by benchmarks rather than recomposition counts. On the
idle frame every microsecond of composition, layout, record and draw costs three to eighteen times its steady-state
price, so the skill's rules are each multiplied there. What the skill says about each measured cause:

| Measured cause (WP04-perf §1) | The skill's rule | On `wp/WP04-perf` |
|---|---|---|
| A pop composed the whole leaving screen because a static `LocalPaneContext` changed | Lazy layouts §6: resolve `CompositionLocal`s once in the parent and pass values down; never let a local change invalidate a subtree on a hot frame (our R3 is the wider rule) | APPLIED: `stack/StackLayout.kt` `rememberPaneContexts` keeps a leaving entry's context (`6436b75`) |
| Every tab switch recomposed every cell: `onTap` captured `selected` | State §1 and lazy §2: pass **lambda providers** (`isSelected: () -> Boolean`) to leaves; strong skipping memoises lambdas by their captures, so a capture that changes per switch defeats it | APPLIED: `component/navigation/AppTabBar.kt` reads the selection through state (`1ec7462`) |
| The pill's two clips and fill were a path of cubics | Graphics §1 and §3: cache `Path`/`Brush` in `drawWithCache`, `rewind()` in hot loops; a shape the renderer recognises is clipped analytically | APPLIED: `TabCells.kt:396-415` `addCapsule` builds a rounded rect; `PillPaths` are three paths rewound per frame (`b7e8074`) |
| `AppScaffold` painted the background over `EntryFrame`'s on every frame | Layouts §3: no decorative wrappers or redundant draws | APPLIED: `LocalPaintedBackground`, `AppScaffoldDefaults.background()` (`ac56e69`) |
| The related rows composed 185 composables 220 ms into every push | Effects §2 and lazy §7: defer secondary content until it is visible or settled (`Modifier.onVisibilityChanged`); ours is `LocalNavigationSettled` | APPLIED in `feature/sample/.../detail/*` (uncommitted until round 2's tests) |
| The GPU starts each transition at its idle clock | Not covered. The graphics category only lowers the work per frame (fewer draw ops, no offscreen layers, cached shaders); the profiler skill's new `hints_cpu` names the frequency diagnosis (see android-profiler below) | n/a |

What the skill adds for the next round on the same frame, all of them work that runs on the composing frame of a
push, a pop or a switch:

1. **`MviScreen` subscribes with `collectAsStateWithLifecycle()`** (`core/ui/MviScreen.kt:40`). The skill's own
   benchmark note: it is **3–4× slower to subscribe and dispatch** than `collectAsState()`, and it is the right choice
   only for flows that must stop when the screen is not visible (database streams, sensors, polling). A ViewModel's
   in-memory `StateFlow` already scoped with `WhileSubscribed` is the case for `collectAsState()`, and in Compose
   Multiplatform common code it is the only option. It also reads `LocalLifecycleOwner` in composition, which is why
   provider swaps on a pop recomposed every screen-level collector (compose-perf §2.2). **Decision for WP02 and WP04:**
   `MviScreen` collects the ViewModel state with `collectAsState()`; the `collect-as-state` code rule (which fails on
   `collectAsState()` in `core/ui` today, `CodeRulesTest.kt:18`) gets an allowance for ViewModel `StateFlow`s and keeps
   forbidding it for repository, database and sensor flows. WP07 needs the same call in common code anyway.
2. **Every scaffolded screen runs `LaunchedEffect(Unit) { viewModel.onEvent(X.Appeared) }`** (`SampleListRoute.kt:16`,
   `SampleDetailRoute.kt:21`, `SearchScreen.kt:56`, `ProfileScreen.kt:44`, `Licenses.kt:118`, `SearchResult.kt:95`).
   `onEvent` does not suspend, so the skill's rule is `SideEffect(Unit)` (keyed, Compose 1.12): no coroutine
   allocation, no cancellation on exit. The ordering changes (`SideEffect` runs after composition and before layout;
   `LaunchedEffect` after draw), which does not matter for an event whose result lands on a later frame. The template
   uses keyed `SideEffect` nowhere yet (0 hits). **WP02-M3 changes the scaffold; measure with the recomposition harness
   before calling it a win.** Same for `AppDropdownMenu.kt:46` and `AppDialog.kt:58` (`LaunchedEffect(Unit) { visible
   = true }`): the enter animation only needs the second composition to see `visible`, which a `SideEffect` write gives.
3. **Overlay animations are read in composition.** `AppDialog.kt:81-82` (`.scale(scale).alpha(alpha)`),
   `AppDropdownMenu.kt:64` (`.alpha(alpha)`), `AppTransitions.kt:244` (`busyOverlay`'s `.alpha(alpha)`) and
   `AppOtpField.kt:175` (the caret blink, `.alpha(caretAlpha)`) each recompose their subtree on every animation tick.
   Animation §1: `graphicsLayer { alpha = …; scaleX = …; scaleY = … }` with `CompositingStrategy.ModulateAlpha` for
   single-draw content (compose-perf §2.9 already allows `Auto` for a card-sized dialog fade; the read still belongs
   in the layer block). MISSING, WP03.
4. **`BoxWithConstraints` on the composing frame.** `feature/auth/AuthFrame.kt:63`,
   `core/designsystem/foundation/AdaptiveLayout.kt:89`; on `wp/WP03` also `AppSegmentedControl.kt:95`,
   `AppTabRow.kt:65`, `AppDataTable.kt:109`, `AppStateView.kt:73` and `AppBottomBar.kt:138` (WP04 replaces that file).
   Layouts §4: subcomposition breaks the single-pass pipeline; use `Modifier.layout`, a `LayoutModifierNode`, flow
   layouts, or the window size class when the branch is on width. `AdaptiveLayout` and `AppStateView` are screen-level
   frames, so they sit on every push. `KeptTabs.kt:99`'s `SubcomposeLayout` is deliberate (tab hosting and
   precomposition) and stays. WP02's `adaptive-box-constraints` code rule exists; its scope is [U]. PARTIAL, WP03 (the
   design system) and WP02-M3 (`AuthFrame`).
5. **Per-frame allocations in draw.** The sparkline (`AppDataDisplay.kt:308-345`) allocates two `Path`s and a
   `Brush.verticalGradient` on every frame of its draw-in animation inside `Canvas`; the checkbox tick
   (`AppSelection.kt:102-107`) allocates two `Path`s and a `PathMeasure` per frame of the tick animation. Graphics §1
   and §3: `drawWithCache` keyed on size and values, `rewind()`, the brush cached per size. `BarAnchor.kt:123` (a path as
   a node member) and `PillPaths` are the model. MISSING, WP03.
6. **Lazy lists.** Keys are present on every data list (`SampleListScreen.kt:131`, `SearchScreen.kt:202`,
   `Licenses.kt:157`, `DevToolsPanel.kt:154`, `AppPagingList.kt:70`); **`contentType` is absent everywhere** (the only
   hit is a network model); `shell/MoreList.kt:35` keys by index (`key = { from + it }`), which the skill forbids even
   for a static list (use the tab's key); skeleton rows (`items(SKELETON_ROWS)`) have neither key nor `contentType`,
   so they are never reused. WP02's `lazy-key` and `lazy-content-type` rules already encode the skill; the lists on
   `wp/WP04-perf` and `wp/WP03` do not pass the second yet. PARTIAL, WP03, WP04, WP02-M3.
7. **The load-more footer fires from composition** (`AppListPieces.kt:140`, `LaunchedEffect(Unit) { onLoadMore() }`).
   Effects §2 and lazy §8: lazy prefetch composes an item before it is visible, so this loads the next page about one
   viewport early, from a coroutine that never suspends. If the early load is wanted, say so in the KDoc and use
   `SideEffect`; if not, `Modifier.onVisibilityChanged(minFractionVisible = …)`. Same rule for any impression or
   screen-view analytics in rows once the `analytics` feature logs them: never `LaunchedEffect` in an item. WP03 decides;
   WP02-M3 and WP06 for analytics.
8. **`derivedStateOf` is used correctly for 1.12**: `KeptTabs.kt:124` (a boolean from a fast-changing selection) and
   `AppWheelPicker.kt:54` (a computation over `visibleItemsInfo`), both unkeyed. When the BOM reaches 1.13, the first
   becomes `computedStateOf`. No `produceState` + `snapshotFlow` derivations, no `derivedStateOf` around `.size`.
   APPLIED.
9. **Stability and collections**: see "Where it disagrees with our plans" below. `@Immutable` UI states hold `List`s
   (`SampleListContract.kt:17-18`, `SampleDetailContract.kt:14`, `Onboarding.kt:64`, `FeedViewModel.kt:19`), no
   `kotlinx.collections.immutable` anywhere (the only hit is a comment in `ComposeStability.kt:165`), and
   `compose-stability.conf` lists `kotlin.collections.List<*>`, `Set<*>` and `Map<*,*>` as stable.
10. **`SnapshotStateList` back stacks** (`AppBackStack.kt:23`, `ShellState.kt`, `KeptTabs.kt`): Navigation 3's
    `NavBackStack` is one, so the skill's `mutableStateOf(list, neverEqualPolicy())` alternative does not apply to the
    stack itself. State §3B still applies to readers: any composition that queries a stack repeatedly (`contains`,
    `indexOf`, iteration) snapshots it once with `toList()`. [U] whether any reader does; WP04 audits.
11. **`onGloballyPositioned`**: none on `wp/WP04-perf`; on `wp/WP05-glass`
    `foundation/glass/SceneBackdrop.kt:163` implements `GlobalPositionAwareModifierNode` to feed the backdrop's
    position to the glass scene. Layouts §1: it runs on every layout pass of the whole tree; query
    `requireLayoutCoordinates()` on demand in the draw or pointer handler (the node already does so at `:153`), or
    `onLayoutRectChanged` for bounds. WP05 justifies or replaces it before the glass merge.
12. **Already clean on every branch:** no `Modifier.composed`, no `Pair`/`Triple` in hot paths, no `Handler.postDelayed`
    or `TimerTask`, no `Image(painterResource)` for photos (Coil `AsyncImage`), no `collectAsState` in items, no
    receivers registered in items, `rememberUpdatedState` used for long-lived callbacks (26 sites), `RuntimeShader`
    created once in a node (`RimLight.kt:114`, `wp/WP05-glass`; [U] that size uniforms are set in the cache block and
    per-frame uniforms in draw). The one empty `onDispose {}` is `catalog/CatalogApp.kt:144` on `wp/WP03`.

### Rules by category (the ones that change what we build)

1. **State, snapshots, stability** (`references/state-and-stability.md`, plus `backwards-write.md` and
   `strongskipping.md`):
   - Read fast-changing state in `offset {}`, `layout {}`, `graphicsLayer {}` or draw lambdas, never in composition;
     a component that takes a fast value takes `() -> T` (our R1 and R2).
   - **Backwards writes** are a read followed by a write of the same state in the same pass, or a composition read
     written from layout or draw (`onSizeChanged`, `onPlaced`, `onGloballyPositioned`, `drawBehind`): they cost an extra
     frame and make the first frame wrong. A write before any read (`Write -> Read`) is not one, but a
     `remember { mutableStateOf() }` overwritten at the top of composition is **redundant state wrapping**: use a `val`
     or read in the target phase.
   - `remember` keys: pass every parameter the block reads; never pass snapshot state values as keys around
     `derivedStateOf`; `remember` costs `keys + 1` slots and an equality check per recomposition, so only for heavy
     allocations or identity.
   - `computedStateOf` (1.13+) for cheap conditions on fast inputs; `derivedStateOf` for heavy calculations read often;
     a plain `get()` for trivial inversions; never `produceState` + `snapshotFlow` for a synchronous derivation.
   - Whole-list updates: a `MutableList` in `mutableStateOf(list, neverEqualPolicy())` with a load-bearing assignment,
     not `mutableStateListOf()`; `toList()`/`toSet()` once for repeated queries; extract repeated `state.value` reads
     into locals; no state reads inside custom setters.
   - **Skipping is not free**: each parameter of a skippable composable is stored and compared. Under strong skipping
     (default since Kotlin 2.0.20; we are on 2.4.21) unstable parameters compare by `===`, stable ones by `equals`.
     **Never add `@Stable`/`@Immutable` to a model that holds a collection, never replace `List` with `ImmutableList`,
     never add `ImmutableList` to `stability_config.txt`**; remove existing annotations only from large or nested models
     that feed a lazy layout; add them only to small flat classes from non-Compose modules whose instances a data source
     recreates. Do not unwrap an `ImmutableList` out of a data class into a child composable (the `equals` runs twice).
   - Make composables cheaper instead of forcing skipping, and **benchmark**; recomposition counts are not a metric.
2. **Layouts** (`references/layouts.md`): no `onGloballyPositioned` (use `onVisibilityChanged`, `onPlaced`,
   `onLayoutRectChanged`, `onSizeChanged`, or `requireLayoutCoordinates()` on demand inside a `Modifier.Node`;
   `detectTapGestures` gives the tap offset); anchor floating content with a custom `Layout`, never by writing
   coordinates into state; `Modifier.offset { }` for dynamic offsets; no wrapper `Box` or decorative `Canvas` (use
   `drawBehind`/`drawWithCache` on the node); no `BoxWithConstraints`/`SubcomposeLayout` in hot paths or lazy items;
   intrinsics only where siblings must match.
3. **Lazy layouts** (`references/lazy-layouts.md`): applies to `LazyColumn`/`Row`/`Grid`/`Pager` only, never to a
   `Column` loop. Always a primitive, Bundle-saveable, unique `key` (never `hashCode` or an index) **and** a
   `contentType`; `itemsIndexed` instead of `indexOf`; selection in a `Set` inside `MutableState` or `mutableStateSetOf`,
   read through a lambda provider in the leaf; `CompositionLocal`s resolved in the parent and passed down;
   `sharedBounds`/`sharedElement` only once visible; `.size` read directly; `Row`/`Column` for 1–10 bounded items;
   shared `Flow`s collected once above the list; no `LaunchedEffect(Unit)` analytics in items.
4. **Animation** (`references/animation.md`): alpha, scale, rotation and colour in `graphicsLayer { }`; offsets in
   `offset { }`; sizes through `Modifier.layout { }` or `animateBounds` in a `LookaheadScope`; lambda providers to
   children; a persistent `Animatable` as a `Modifier.Node` member started in `onAttach()` (never `composed` +
   `remember` + `LaunchedEffect`).
5. **Effects, lifecycle, threading** (`references/effects-and-threading.md`): no side effects in a body;
   `SideEffect(keys)` for synchronous work (never a `LaunchedEffect` that does not suspend, never
   `DisposableEffect` with an empty `onDispose`); heavy allocations and computations in `remember`; related
   `LaunchedEffect(Unit)`s merged into one with nested `launch`; effects run `DisposableEffect` → `SideEffect` →
   layout and draw → `LaunchedEffect` after the frame, and cancelling a `LaunchedEffect` costs a coroutine teardown,
   so do not cycle its keys during animations; blocking I/O through `produceState` + `withContext(Dispatchers.IO)`;
   `collectAsStateWithLifecycle()` for flows that do background work, `collectAsState()` for in-memory `StateFlow`s and
   on non-Android targets; receivers and listeners hoisted to the screen; optional effects guarded by `if (callback !=
   null)`; `rememberUpdatedState` in loops; `delay()` instead of `Handler.postDelayed`.
6. **Graphics** (`references/graphics-performance.md`): `drawWithCache` for `Path`, `Paint`, `Brush` and `Shader`
   allocations; `RuntimeShader` and `ShaderBrush` remembered, size uniforms in the cache block, per-frame uniforms in
   `onDrawBehind`; `Path.rewind()` over `reset()` in hot loops; `androidx.collection.MutableLongSet`/`MutableIntSet`,
   `packInts`/`packFloats`, `fastForEach` and value classes instead of `Pair`, boxed sets and iterators;
   `AsyncImage` for photos and large bitmaps, `painterResource` only for vectors and icons (synchronous main-thread
   decode at full resolution otherwise). This makes the `coil` feature load-bearing for any row with a raster image:
   the scaffold must not put bitmaps in rows when `coil` is off (WP02-M3).
7. **Custom modifiers** (`references/android/develop/ui/compose/custom-modifiers.md`): `ModifierNodeElement` +
   `Modifier.Node` with capability interfaces (`DrawModifierNode`, `LayoutModifierNode`, `SemanticsModifierNode`,
   `CompositionLocalConsumerModifierNode` with `currentValueOf`), `DelegatingNode` to compose nodes, opt-outs of
   auto-invalidation with explicit `invalidateDraw()`/`invalidateMeasurement()`; `requireLayoutCoordinates()` on demand
   rather than `GlobalPositionAwareModifierNode`. The template already has 8 (`wp/WP04-perf`) to 41 (`wp/WP03`)
   `ModifierNodeElement`s and no `composed {}`: APPLIED.

### Where it disagrees with our plans, and the decisions

- **`compose-stability.conf` marks `kotlin.collections.List<*>`, `Set<*>` and `Map<*,*>` stable** (RECOMPOSITION-PLAN
  R5, applied on `wp/WP02`). The skill says the opposite for collections, because `equals` on a stable collection is
  O(N) per recomposition. The two are reconcilable: `AbstractList.equals` returns on identity first, so a state copy
  that keeps the list instance (what our reducers' `copy()` does) costs O(1), and a list that really changed must
  recompose its reader anyway, so the O(N) walk is paid only when it buys a recomposition. **Decision:** keep the three
  lines, add that reasoning to the conf's comment (WP02), and adopt the skill's prohibitions verbatim: no
  `kotlinx.collections.immutable`, no `@Immutable` on a model whose job is to carry a large or nested collection into a
  lazy layout, and never recommend either in a review. `api-immutable` and `uistate-mutable` keep the promise. Measure
  with the recomposition harness, not with counts.
- **`collect-as-state`** (WP02's rule) forbids what the skill recommends for ViewModel state; see item 1 above.
- **`LaunchedEffect(Unit) { Appeared }`** in the scaffold; see item 2 above.
- **`layer-alpha-fill`**, **`lazy-key`**, **`lazy-content-type`**, **`state-param`**, **`animated-delegate`**,
  **`subcompose-image`** and **`adaptive-box-constraints`** on `wp/WP02` already encode skill rules; keep them and
  cite the skill's reference file in each rule's message (WP02).
- Our rules the skill does not have and which stay: no CompositionLocal or lifecycle-owner change on a transition
  frame (R3), parked rather than unplaced hidden screens, the composing frame is not the first frame of the animation,
  a destination's first composition is final, `ModulateAlpha`/no alpha on full-screen layers, frame-rate votes for
  layer-only motion (compose-perf §5).

### What it does not cover

DVFS and touch boost, ADPF hint sessions, `createPausedPrecomposition`, `movableContentOf` moves between Navigation 3
scenes, shader and glyph warm-up, font preloading, `preferredFrameRate`, GC churn. Those stay in
`research/compose-perf.md` and `SMOOTHNESS-PLAN.md`; the skill is the checklist for the code that runs on the frame,
and `android-profiler` is the skill for finding out why the frame was slow.

## navigation-event (WP04; WP07 for parity)

v1.0.15 changed only a typo in `references/.../navigation-event/index.md` and the date. The rules stand:

- `ComponentActivity` already is the `NavigationEventDispatcherOwner`. Never override `navigationEventDispatcher` or
  wrap it: that recursion crashes at launch.
- **Kept tabs:** each tab gets `rememberNavigationEventDispatcherOwner(enabled = isSelected)` and provides it through
  `LocalNavigationEventDispatcherOwner`. Disabling a parent cascades to its children, so a hidden tab can never take
  Back. Use this instead of a hand-rolled per-tab dispatcher.
- **One `NavigationBackHandler` per `NavigationEventState`.** Binding two throws `IllegalArgumentException`, so branch
  inside `onBackCompleted`. Migrate every legacy `BackHandler`.
- **Dialogs and `ComponentDialog`-backed sheets** get their own dispatcher owner automatically. Do not re-provide it.
- **Ordering:** handlers run by priority (`PRIORITY_OVERLAY` before `PRIORITY_DEFAULT`), then LIFO within a priority.
  Overlays such as sheets and menus use the overlay priority.
- **With Navigation 3**, use its built-in predictive back (`predictivePopTransitionSpec` and the 1.2.0 mid-flight
  support) rather than low-level handlers. Use handlers only for non-NavDisplay surfaces.
- **The Material reference motion** for a predictive-back card (the Gmail look): scale `1 - progress * 0.1` (down to
  0.9), and shift `width / 20 - 8` dp toward the swipe edge (`EDGE_LEFT`/`EDGE_RIGHT` from the event). Read progress
  and swipe edge from `NavigationEventTransitionState.InProgress.latestEvent`. Apply them in `graphicsLayer`, not
  `offset(x =)`/`scale()` in composition as the sample does.
- **Manifest:** `compileSdk >= 36`. `android:enableOnBackInvokedCallback` must be `true` on API 33–35 and must never be
  `false`.
- **On the branches:** `NavigationBackHandler` throughout the shell (`app/ui/AppRoot.kt:133`,
  `back/StackBackHandler.kt:38`); the catalog app still uses `BackHandler` (`catalog/CatalogApp.kt:77`, `wp/WP03`),
  which WP02's `legacy-back` rule will fail. PARTIAL, WP03.

## navigation-3 (WP04)

- **Recipes to match:**
  - multiple back stacks (one stack per tab, survives configuration changes and process death);
  - `common-ui` (bar with per-tab history);
  - animations (global override plus a per-destination override in metadata) and conditional transitions;
  - conditional navigation (auth and onboarding flows switch as a state, not a reset);
  - deep links, including the synthetic back stack;
  - results as events or as state;
  - modular navigation with Hilt;
  - the lifecycle owner;
  - the bottom-sheet scene, dialog scene, and list-detail and two-pane scenes.
- **Recipe sources** are in `navigation/navigation-3/references/android/guide/navigation/navigation-3/recipes/`.
- **v1.0.15, multiple back stacks:** every stack's `rememberDecoratedNavEntries` now takes both
  `rememberSaveableStateHolderNavEntryDecorator` and `rememberViewModelStoreNavEntryDecorator`. APPLIED:
  `stack/StackHost.kt:105-106` and `presentation/PresentationHost.kt:94-95` on `wp/WP04-perf`.
- **v1.0.15, bottom sheet:** `BottomSheetScene` keeps its `sheetState` and overrides `suspend fun onRemove() {
  sheetState.hide() }` so a pop runs the sheet's hide animation before the entry leaves, and it no longer wraps the
  content in `CompositionLocalProvider(LocalLifecycleOwner provides rememberLifecycleOwner())`: the decorators own the
  lifecycle. [U] whether `Scene.onRemove` exists in navigation3 1.2.0 (the recipes track the newest release). If it
  does, our sheet scene (SHEET-SPEC) awaits its own dismissal there instead of running a parallel pop animation; if
  not, keep ours and note the API for the next bump. WP04 (the scene), WP03 (`AppBottomSheet`'s animation).

## adaptive (WP03 components, WP04 shell and scenes, WP02-M2 enforcement, WP02-M3 screens)

- **Multi-pane:** list-detail and supporting pane use Navigation 3 `SceneStrategy`, never
  `ListDetailPaneScaffold`/`SupportingPaneScaffold`. **Verified:** `androidx.compose.material3.adaptive:adaptive`,
  `adaptive-layout`, `adaptive-navigation` and `adaptive-navigation3` (stable 1.3.0) do not depend on material3. Their
  Android POMs list only foundation, ui, window, activity, navigationevent and navigation3. Use
  `rememberListDetailSceneStrategy` / `rememberSupportingPaneSceneStrategy` and `currentWindowAdaptiveInfo()` directly
  (REQUIREMENTS 31). `NavigationSuiteScaffold` is the exception: it lives in `material3-adaptive-navigation-suite`,
  which needs material3, so our own bar and rail play that role.
- **Detail screens:** no back arrow when shown in a two-pane layout. A detail that is full screen on a phone is not
  full screen on a large screen.
- **Navigation area:** a bottom bar on compact width, an edge rail on larger widths. Hide it while scrolling down and
  show it on scroll up, and hide it for distracting full-screen content. Our `AppTabBar`/rail are the equivalent of
  `NavigationSuiteScaffold`, and visibility is state driven by the shell, never `AnimatedVisibility` around the bar.
- **Lists:** a `LazyColumn` of cards becomes `LazyVerticalGrid(GridCells.Adaptive(minWidth))`, and staggered grids use
  `Adaptive`. **v1.0.15 removed the "experimental" notes from the `Grid` and `FlexBox` pages and pins the samples to
  Compose 1.13.0-beta01**: both are heading to stable in 1.13. Keep `LazyVerticalGrid`/`FlowRow` until the BOM carries
  1.13 stable, then re-evaluate (WP03; the `AGENTS.md` row says "experimental in Compose 1.12").
- **Device queries:** the Compose `MediaQuery` API reads window size, pointer precision, keyboard type and so on. Prefer
  it over hand-rolled checks where available.
- **Previews:** every screen gets a multipreview annotation covering Phone, Foldable, Tablet and Desktop
  (`@FormFactorPreviews`).
- **v1.0.15, screenshots:** the skill now links the AGP test-suites screenshot doc
  (`references/android/studio/preview/compose-screenshot-testing-with-testsuites.md`: AGP 9.5.0-alpha03+, engine
  `0.0.1-alpha16`, `android.experimental.enableScreenshotTest` and `testSuiteSupport`, `testOptions {
  screenshotTests.create(…) }`) instead of the removed `tooling/debug.md`. We are on AGP 9.4.1 with Roborazzi (WP02-M2
  deviation 2); nothing changes until 9.5 is stable.
- **On the branches:** `BoxWithConstraints` sites listed under jetpack-compose-performance item 4; WP02's
  `adaptive-*` rules (`adaptive-box-constraints`, `-mediaquery`, `-fixed-size`, `-orientation`, `-screen-metrics`,
  `-device-type`, `-material-scope`, `-preview`) enforce the rest.

## testing-setup (WP02-M2 owns the infrastructure; every package writes its tests)

- **Screenshot tests:** use the Compose Preview Screenshot Testing tool. With AGP ≥ 9.5.0-alpha03, use AGP test suites
  (`testOptions { screenshotTests.create(...) }`). We are on AGP 9.4.1 stable, so it is the standalone plugin
  `com.android.compose.screenshot`; record that it is deprecated and move to test suites when 9.5 is stable. Never
  combine the two in one module. (We use Roborazzi; the deviation is recorded in `handoff/WP02-M2.md`.)
- **Screen screenshots** at nine sizes, widths 400/610/900 dp × heights 400/500/1000 dp, plus 400×500 with every
  theme and font scale 1.5. **Component screenshots** with every theme and font scale.
- **Defaults:** JUnit4, Compose testing APIs, Robolectric for UI tests in `test/`. Use fakes first, created with an
  interface plus a `Default` implementation; MockK only when it is clearly necessary. Room is tested with an in-memory
  database in instrumented tests. Do not unit-test composables, Activities or DI modules; test ViewModels,
  repositories and DAOs.
- **v1.0.15, failure artifacts** (`references/.../testing/common-patterns.md`): `ComposeUiTestConfig(failurePolicy =
  TestFailurePolicy(screenshotCaptureMode = Enabled, uiHierarchyCaptureMode = Enabled, failureHandlers = …))`, or for a
  whole module `testInstrumentationRunnerArguments["androidx.compose.ui.test.failure.isScreenshotCaptureEnabled"] =
  "true"` and `…isUiHierarchyCaptureEnabled`; a test's explicit mode beats the runner argument. Needs
  `ui-test` 1.13.0-alpha01+, so MISSING by version: WP02-M2 wires the runner arguments and WP13 uploads the artifacts
  from CI when the BOM reaches 1.13.
- **v1.0.15, previews from the CLI:** `android studio render-compose-preview` (the android-cli skill) renders a
  preview without Studio; useful for the catalog's visual checks.

## edge-to-edge (WP04 shell, WP03 components, WP02-M3 screens)

Unchanged in v1.0.15.

- **Lists:** insets go into `contentPadding`, never `Modifier.padding()` on the list's parent. Parent padding clips the
  content and stops it scrolling behind the bars. That is exactly the user's "white opaque block around the bottom bar".
- **IME:** prefer `Modifier.fitInside(WindowInsetsRulers.Ime.current)` to `imePadding()`. It causes less jank and
  avoids double padding. Set `android:windowSoftInputMode="adjustResize"`. Never apply IME insets twice (for example
  `contentWindowInsets = safeDrawing` and then `imePadding()`). **On the branches:** `AppScaffold.kt:52` still uses
  `imePadding()` and no branch uses `fitInside` (0 hits). MISSING, WP03.
- **Nested content:** deeply nested content uses `WindowInsetsRulers` with `fitInside`, not stacked paddings.
- **Navigation bar:** with a bottom bar, set `window.isNavigationBarContrastEnforced = false` (SDK 29+) so the system
  adds no translucent scrim behind it.
- **System bar icons** must follow the app's theme, not the system's. With `ComponentActivity.enableEdgeToEdge` pass a
  `SystemBarStyle` derived from the in-app theme. Add status-bar protection (a short gradient scrim) where content
  scrolls under the status bar.
- **Full-screen dialogs:** `usePlatformDefaultWidth = false` + `decorFitsSystemWindows = false`.

## agp-9-upgrade (WP02-M2)

Unchanged in v1.0.15.

- `android.disallowKotlinSourceSets=false` must never be in `gradle.properties`. Remove the migration flags
  `android.builtInKotlin`, `android.newDsl`, `android.uniquePackageNames` and `android.enableAppCompileTimeRClass`
  once migrated. APPLIED on `wp/WP02` (`4a91033`: no migration flag is left).
- KSP ≥ 2.3.6 and Hilt ≥ 2.59.2. Kotlin is built in, so never apply `kotlin-android`. Migrate kapt to KSP.
- Verify with `./gradlew help`, then `./gradlew build --dry-run`, then a real build. Never run `clean` to "fix"
  anything.

## r8-analyzer (WP02-M2 for the template's rules; WP13 in verification)

Unchanged in v1.0.15.

- AGP ≥ 9.3, so use the standalone task `./gradlew :app:analyzeReleaseR8Config` (one task per variant:
  `analyzeProdReleaseR8Config`). It writes `app/build/reports/r8/r8-config-analyzer-release.pb`; convert and score it
  with the skill's scripts.
- Remove rules that libraries already bundle (the skill's `REDUNDANT-RULES.md`). Refine package-wide keeps. Full mode
  stays on. Validate rule changes with a Macrobenchmark journey. APPLIED on `wp/WP02` (`f4b4349`, `6b68cfd`).

## theming/styles (WP03: evaluate, do not adopt blindly)

Unchanged in v1.0.15.

- The Compose Styles API (`androidx.compose.foundation.style.Style`, `Modifier.styleable`, `MutableStyleState`,
  per-state blocks such as `pressed {}`/`disabled {}` with animations) fits a custom design system like ours. It is
  **experimental** (`@ExperimentalFoundationStyleApi`, compileSdk 37+).
- Our rule is stable APIs only, so do not build the design system on it. Record the evaluation in the WP03 handoff,
  and keep component APIs shaped so a later migration is mechanical: a `style`-like defaults object, no long lists of
  styling parameters.

## Security skills (WP06, WP04 for the manifest)

Unchanged in v1.0.15.

- **android-intent-security:** every exported component justified, intent redirection prevented (never launch an
  `Intent` taken from extras without validation), `PendingIntent` immutable and explicit. Audit `MainActivity`'s
  deep-link handling and notification taps.
- **android-permissions-security:** runtime permissions requested in context (`POST_NOTIFICATIONS`, camera, media),
  custom permissions `signature`-level, providers not exported (the `FileProvider` scoped to `uploads/`).

## android-profiler (WP04-perf now; WP04, WP05, WP11, WP13)

Restructured in v1.0.15. The `SKILL.md` is now a router (intent → `recording/recording_orchestrator.md` or
`analysis/analysis_orchestrator.md`), its description triggers on any jank, startup, ANR, memory or battery question
"even if the user does not explicitly mention profiling", and the rules below are new or sharpened.

- **`$SKILL_ROOT`** (`references/env_setup.md`) is the absolute path of the skill folder; every path in the skill is
  relative to it. In a generated project that is `.agents/skills/android-profiler/`.
- **The bundled `trace_processor` wrapper is pinned to the skill's Perfetto release** (`references/perfetto/setup.md`):
  never download, symlink or `find` another `trace_processor`; a team-mandated one wins only in environments that
  mandate it. APPLIED: `handoff/WP04-perf.md` uses it (Perfetto v57).
- **Sessions** (`references/perfetto/sql.md`): load a trace once with `trace_processor server unix --name <session>
  --daemonize TRACE`, run every query with `trace_processor query --remote <session> "SQL"`, and `server kill` it when
  done. Created tables and `INCLUDE PERFETTO MODULE`s persist across calls; loading flags belong on the server call;
  idle sessions are reaped after 30 minutes. [U] whether `$O/measure.sh` and the `q*.sql`/`d*.sql` queries already
  share a session; if not, WP04-perf adopts it (the per-frame join is run many times per trace).
- **Analysis protocol** (`analysis/workflows/perfetto-trace-analysis/`): triage first (`references/triage.md`: the
  `stats` table for dropped data, then `--run-metrics android_startup,android_cpu,android_mem,android_lmk,
  android_binder,android_surfaceflinger,android_gpu`, the symptom window, and **expand upstream** when the first
  blocking event started before the window, following the waker chain into other processes), then one investigation
  per candidate (`references/per_candidate_analysis.md`: a baseline from an A/B trace or **a healthy instance of the
  same operation in the same trace**, thread-state buckets, hints discovery, exhaustive traversal). **Every claim is
  tagged `[SQL]`, `[INFERRED]` or `[GAP]`**, partial suspects are reported with their evidence, and the report
  classifies the root cause (hardware, code, scheduling policy, system exhaustion, external dependency) with a
  dependency chain of thread names, UTID/UPIDs, states and timestamps. Our first-frame table (`handoff/WP04-perf.md`
  §1) already uses the "steady frames of the same animation" baseline; adopt the tags and the candidate block in the
  handoff's §6 and in WP13's before/after report.
- **Hints moved**: `references/hints/subsystems/hints_*.md` are now
  `analysis/workflows/perfetto-trace-analysis/references/hints/hints_{cpu,graphics,io,ipc,memory,power}.md`;
  `references/guiding_principles.md` and `hints/domain_hints_template.md` are gone (folded into
  `per_candidate_analysis.md`). `research/compose-perf.md` cites the old path in its Sources; WP12 fixes it when it
  copies. Config names are unchanged (`app_jank.pftxt`, `cpu_profile.pftxt`, `sched_cpu.pftxt`, `memory_counters.pftxt`,
  `java_heap_dump.pftxt`, `native_heap.pftxt`, `long_background.pftxt`).
- **`hints_cpu.md` now names our root cause**: check the `cpu_frequency` counter for the cores the main process ran
  on, and treat missing transitions or static low frequencies during a burst as a governor bottleneck or stall; "if a
  slice's wall duration increases but its Running share is unchanged, the frequency dropped"; check `cpu_id` for key
  threads landing on slower cores; quantify Runnable latency at p95/p99 with `preceding_sched_slice_for_thread`; when a
  woken thread is delayed, check the CPU's IRQ track; query ftrace for the governor thread. That is the §1 analysis
  (main thread at 652 MHz on a big core, RenderThread on a little core for one mid-fling frame). What the hints add
  for round 2 (WP04-perf): the Runnable p99 per first frame for the UI thread and RenderThread, the IRQ track at the
  touch-down, and the governor's ftrace lines around the boost, so the handoff can say `[SQL]` whether the boost
  shape or the placement is the larger term.
- **`hints_graphics.md`**: `actual_frame_timeline_slice` against `expected_frame_timeline`, long `texture_upload`
  slices on the main thread (first visits), `eglSwapBuffersWithDamageKHR` duration as the damage-area signal,
  `gpu_mem_total` and `android_graphics_allocs` for memory. GPU frequency is unreadable on this phone (`kgsl` denied
  to shell), so GPU evidence stops at frame timeline and damage area; the handoff already says so.
- Use its Perfetto workflows (trace recording, trace_processor SQL) for the worst-case traces the perf loops must
  attach.

## glance-canonical-layouts (new; WP02-M3 for the widget feature's code, WP02 vendoring, WP12 docs)

Google's canonical widget layouts (action list, check list, image grid, image-text list, long text, text with image,
full-bleed image with snap scrolling, standard, search and expressive toolbars) as copyable Kotlin and resources under
`references/`. Vendored only with the `widget` feature.

- **Dependencies:** `glance-appwidget`, `glance-material3`, `glance-appwidget-preview` and `glance-preview`, all
  ≥ 1.2.0. Snap scrolling needs Glance 1.3.0-alpha02, compileSdk 37 and AGP 9.1: alpha, so not for us.
- **The provider overrides `val sizeMode: SizeMode = SizeMode.Exact`.** MISSING: `app/.../widget/AppWidget.kt:41`
  keeps the default `SizeMode.Single`.
- **The receiver is registered in `<application>` after `<activity>`**, with `android:resource="@xml/…"` matching the
  info file. APPLIED: `AndroidManifest.xml:118-128` inside `<opt:widget>`.
- **The info XML keeps the given `minWidth`, `minHeight` and `minResizeHeight`, through dimension resources.**
  PARTIAL: `res/xml/app_widget_info.xml` uses literal `180dp`/`80dp`.
- **Colours:** `GlanceModifier.background(Color)` for solid backgrounds (a `ColorProvider` background renders
  transparent); elsewhere the type is `androidx.glance.unit.ColorProvider` and the creator
  `androidx.glance.color.ColorProvider(day, night)`.
- **Copy a layout's whole file set at once**, fix package names, delete the sample's unused classes.
- **Our difference for `AGENTS.md`:** the template's widget uses `GlanceTheme` (dynamic colours) from `glance` core
  without `glance-material3`, which would put material3 on the app's classpath. When a project copies a canonical
  layout, rewrite its `ui/theme/Theme.kt` onto `GlanceTheme(colors = ColorProviders(…))` built from the app's tokens
  rather than adding `glance-material3`. WP02-M3 fixes `sizeMode` and the dimension resources in the feature now.

## camerax (metadata only in v1.0.15; vendored with the `media` feature)

The skill is about in-app capture: CameraX `Preview`/`ImageCapture`/`VideoCapture`, `CameraXViewfinder`, Camera1 and
Camera2 migration, the immutable-builder trap (`prepareRecording(...).withAudioEnabled()` returns a new instance that
must be kept), ML Kit and Media3 effects. The `media` feature launches the system camera
(`core/media/MediaPickerController.kt:86-97`, `TakePicture`/`CaptureVideo`) and never depends on CameraX. The
`AGENTS.md` row says: read it only when adding in-app capture; the picker stays the default.

## Skills with no feature behind them

- **android-cli** (new to the vendored set, always): `android run`/`install` (delta installs), `android layout` and
  `android screen capture|resolve` for device inspection (read `references/interact.md` first, and
  `references/journeys.md` before a journey), `android emulator`, `android docs search|fetch` (the Android knowledge
  base: use it before guessing an API), `android studio render-compose-preview|version-lookup`, `android skills
  add|update|find` and `android sdk`. **Its install line pipes a downloaded script into a shell: an agent never runs
  it; the person installs the CLI from developer.android.com.** The perf harness keeps using `adb` directly.
- **play-policy-insights** (not vendored): a pre-submission audit (`scripts/orchestrator.py`, sub-agents, a
  `.scratch/` directory, a fail-fast mandate) across permissions hygiene, account and identity, and data safety. WP13
  runs it once from the installed plugin on the `everything` preset before the Play-facing docs are final; `AGENTS.md`
  tells projects to `android skills add play-policy-insights` before a submission.
- **play-billing-library-version-upgrade** (not vendored; updated release notes to PBL 9.1.0: `BillingChoiceInfo`,
  external payments return `USER_CANCELED` with `BILLING_ACTIVITY_PAUSED`): no billing feature exists; the `playstore`
  feature is Play Core in-app update and review. Vendor it with a `billing` feature if one is added.
- **engage-sdk-integration** (not vendored, 1 MB; v1.0.15 adds per-vertical pages): no feature.
- **litert-runtime** (new, not vendored): `com.google.ai.edge.litert:litert` and `litert-gpu` 2.3.0, `CompiledModel`
  on one single-thread dispatcher, buffers created once and closed before the model, warm-up in `init`, GPU → CPU
  fallback. Only with an on-device-AI feature; same for `appfunctions` and `ml-kit-genai-prompt-api`.
- **wear-widgets** (new), **wear-compose-m3** (now 1.8.0-alpha01, one-handed gesture APIs no longer `[rc]`),
  **android-for-cars-parked-apps** (new: `CAR_LAUNCHER`, `android.hardware.type.automotive`, compatibility mode,
  `LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS|NEVER` through `values-car-v30`, `OnControllableInsetsChangedListener`,
  `CarUxRestrictionsManager` pausing playback), **leanback-to-compose-tv-migration**, and the XR glimmer skill
  (`ProjectedActivityCompat.requestPermissions` replaces `ProjectedPermissionsResultContract`): form factors the
  templates do not target. The cars skill is the one to read first if a generated video app is ever taken to
  Automotive OS; the adaptive rules above do not cover its insets and cutouts.
- **media3-cast-integration**, **migrate-xml-views-to-jetpack-compose**: no cast feature, no XML.
- **styles**: experimental; see theming/styles.

## Vendored into generated projects (WP02 files and tool, WP12 docs, WP13 checks)

A feature-aware map. "Always" means every generated project; a feature name means the skill ships exactly when that
feature is on (`features/<key>.toml` `owns` the skill folder and its `.claude/skills/` pointer, as `auth.toml` does
today). Sizes are upstream `du` at v1.0.15.

### Android template (`templates/android`)

| Skill | Ships | Why, and the `AGENTS.md` difference |
|---|---|---|
| `navigation-3` | always | destinations, scenes, deep links, results; features register through `navGraph { entry<K>(pane = …) }` |
| `navigation-event` | always | Back outside `NavDisplay`; `NavigationBackHandler` only |
| `adaptive` | always | every screen; the shell's bar, rail and sidebar; `LazyVerticalGrid` until Compose 1.13 stable |
| `edge-to-edge` | always | insets, IME, system bars; `chromeContentPadding()` as `contentPadding` |
| `testing-setup` | always | tests and infrastructure; Roborazzi at the ADAPTIVE sizes |
| `android-profiler` | always | jank, startup, memory; release builds through `tools/perf/perf.sh`; its wrapper downloads Google's binary on first run |
| `r8-analyzer` | always | keep rules and size; `analyzeProdReleaseR8Config`, then the manual path |
| `android-intent-security`, `android-permissions-security` | always | manifest, deep links, notifications, permissions; links land in `LinkActivity` |
| `agp-9-upgrade` | always | a reference for AGP and Gradle bumps; the project is already on AGP 9 |
| **`jetpack-compose-performance`** (200 KB) | **always** (new) | before changing any composable, modifier or list: read the category's reference first; `derivedStateOf` on 1.12; `collectAsState()` for ViewModel state; keys and `contentType` on every lazy list; no `BoxWithConstraints` in features |
| **`android-cli`** (48 KB) | **always** (new) | `android docs search`, `run`, `layout`, `screen`, `render-compose-preview`, `skills update --all`; never run its piped installer |
| `restore-credentials`, `verified-email` | `auth` (and so `googlesignin`, which implies it) | flows in `:feature:auth`, credentials in `:data:auth`, Credential Manager 1.6.0 |
| **`glance-canonical-layouts`** (620 KB) | **`widget`** (new) | copy a layout's file set; `SizeMode.Exact`; `GlanceTheme` from tokens, not `glance-material3` |
| **`camerax`** (76 KB) | **`media`** (new) | only for in-app capture; the feature's picker uses the system camera |
| `play-policy-insights` | not vendored | WP13 runs it from the plugin before a Play submission; projects add it with `android skills add` |
| `play-billing-library-version-upgrade` | not vendored | no billing feature (`playstore` is update and review); add with a `billing` feature |
| `engage-sdk-integration`, `litert-runtime`, `appfunctions`, `ml-kit-genai-prompt-api`, `media3-cast-integration`, `wear-widgets`, `wear-compose-m3`, `android-for-cars-parked-apps`, `leanback-to-compose-tv-migration`, `display-glasses-with-jetpack-compose-glimmer`, `migrate-xml-views-to-jetpack-compose` | not vendored | no feature or form factor uses them |
| `styles` | not vendored | experimental API |

Features that bring no skill of their own and rely on the always-on set: `coil`, `network`, `room`, `database`,
`paging`, `websocket`, `workmanager`, `applock`, `profile`, `forms`, `language`, `licenses`, `onboarding`, `sample`,
`search`, `settings`, `browser`, `deeplink` (the security skills), `playstore`, `push` (the security skills for
`POST_NOTIFICATIONS` and notification taps), `analytics`, `devtools`, `firebase`, `flags`, `catalog`, `googlefonts`,
`baselineprofile` (the performance skill's prerequisite; the wildcard profiles ship regardless), `ci`, `coverage`,
`depsanalysis`, `formatting`, `leakcanary`.

### Compose Multiplatform template (`templates/multiplatform`, `wp/WP07`; features today: `coil`, `database`, `language`, `network`, `onboarding`, `sample`, `settings`)

| Skill | Ships | Difference |
|---|---|---|
| `navigation-3`, `navigation-event`, `adaptive`, `testing-setup` | always | JetBrains Navigation 3 1.1.2 (no 1.2 mid-flight predictive back); the Android host tests run the shell tests |
| `edge-to-edge`, `r8-analyzer`, `android-intent-security`, `android-permissions-security`, `android-profiler`, `android-cli` | always (`androidApp`) | they apply to the Android app module and the Android host |
| **`jetpack-compose-performance`** | **always** | common code uses `collectAsState()` (no Android lifecycle), `derivedStateOf` (CMP 1.12.1), keyed `SideEffect` and `onVisibilityChanged` [U: verify both exist in the JetBrains 1.12.1 artifacts] |
| `agp-9-upgrade` | never | the skill excludes Kotlin Multiplatform |
| `restore-credentials`, `verified-email` | `auth`, when phase C adds it | `androidApp` only |
| `glance-canonical-layouts`, `camerax` | `widget`, `media`, when those features exist | `androidApp` only |

### What WP02 changes to get there

- `tools/update_android_skills.py`: four `Skill` entries (`jetpack-compose-performance` and `android-cli` always;
  `glance-canonical-layouts` with `feature="widget"`; `camerax` with `feature="media"`), re-run against a v1.0.15
  checkout (`--tag v1.0.15`), which rewrites `README.md` (tag and commit), `MANIFEST.sha256`, the pointers and the
  repository's own `.agents/skills/` and `.claude/skills/` pointers. The "no template feature uses them" comment and
  the `android-cli` exclusion comment go.
- `features/widget.toml` and `features/media.toml` `owns` gain `.agents/skills/<name>` and `.claude/skills/<name>`,
  exactly as `auth.toml` does.
- `generator/tests/test_vendored_skills.py`: the exclusion list drops `android-cli` and `camerax` and keeps `styles`
  and `play-policy-insights`; `test_identity_skills_belong_to_auth` becomes "every feature skill is owned by its
  feature"; `GeneratedProjectTest` adds presets with `widget` and `media` off and asserts the two skills follow them.
- `templates/android/AGENTS.md` §8: rows for the four skills (the `widget` and `media` rows inside `<opt:widget>` and
  `<opt:media>` markers), the perf row saying "read the category's `references/*.md` before you change what it
  flags", the `android-cli` row with the installer warning, and the closing paragraph naming `play-policy-insights`
  as install-on-demand. WP12 owns the wording from there; `CLAUDE.md`, `GEMINI.md`, `.cursor/rules` and
  `.github/copilot-instructions.md` keep pointing at the same table.
- Sizes: +248 KB on every project (`jetpack-compose-performance` 200 + `android-cli` 48), +620 KB with `widget`,
  +76 KB with `media`, on top of the 1,324 KB always-on set and 268 KB for `auth`. [U] whether any of the four ships a
  `build/` folder that the `.gitignore`/`.vercelignore`/`.dockerignore` exceptions must name.

### How they ship

- They go under `.agents/skills/<name>/`, unmodified, with `LICENSE.txt` beside each `SKILL.md`, a `README.md` with the
  tag and commit, and `MANIFEST.sha256`; Claude Code reads the pointers in `.claude/skills/<name>/SKILL.md`.
- `AGENTS.md` lists when to use each and how the project differs, and `CLAUDE.md`, `GEMINI.md`, Codex, Cursor and
  Copilot are pointed at the same files.
- `android skills update --all` (the Android CLI) refreshes them; afterwards put `LICENSE.txt` back and regenerate
  `MANIFEST.sha256` as the README says.
