# Performance

How an app built on this template stays light and smooth on a budget phone at 120 Hz: what to do in a
screen, what not to do, why, and the number each rule protects. The reference phone is a Redmi Note 10
Pro Max (M2101K6I): 1080 × 2400 at 120 Hz, so a frame has 8.33 ms. If a screen is smooth there, it is
smooth on most phones.

Read [the rules that matter most](#the-rules-that-matter-most) before your first screen. The sections
after it go area by area; [How to measure](#how-to-measure) says how to prove a change, and
[Before you merge UI work](#before-you-merge-ui-work) is the checklist.
<!-- <opt:perfhud> -->

The performance overlay has its own guide, [PERFORMANCE-OVERLAY.md](PERFORMANCE-OVERLAY.md).
<!-- </opt:perfhud> -->

Every rule below says what enforces it: a `verifyCodeRules` rule id (it fails the build; a justified
exception is `// allow(<rule-id>): <reason>` on the line above), a Compose lint id (an error in every
Compose module), a build check, a test, or review. Rules marked *review* have no automatic check: the
`jetpack-compose-performance` audit and the reviewer catch them.

<!-- <opt:performance> -->
Numbers come from release builds compiled with their baseline profile, measured on the reference phone
with `tools/perf/perf.sh`, unless a rule says otherwise. A limit quoted as "gate" is a row of
`tools/perf/gates.toml`.
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ Numbers come from release builds compiled with their baseline profile, measured on the reference phone -->
<!--~ with the template's perf harness, unless a rule says otherwise. A limit quoted as "gate" is one of that -->
<!--~ harness's limits. This project was generated without the harness (the `performance` feature), so treat -->
<!--~ the gates as targets and measure them as [How to measure](#how-to-measure) says. -->
<!-- </opt-else:performance> -->

---

## The rules that matter most

1. **Judge smoothness only on a release build, with its baseline profile, on a real phone.** A debug
   build is not a slow release build: on the reference phone a `devDebug` scroll read about 50 % janky
   frames where `devRelease` read a few percent.
   <!-- <opt:perfhud> -->
   Hide the performance overlay while you measure.
   <!-- </opt:perfhud> -->
   [More](#builds-that-mean-something)
2. **Read anything that moves in layout, draw or a layer, never in composition.** An animated, dragged
   or scrolled value read in composition recomposes a subtree on every frame, 120 times a second.
   [More](#read-moving-values-where-they-are-used)
3. **Never put alpha below 1 on a full-screen layer.** It renders the whole screen offscreen on every
   frame. Scrims are drawn rectangles. Screens have no exception. The shell has two, each bounded: the
   back gesture's card, drawn from a texture rendered once per gesture, and `HostCrossfade`, the 200 ms
   fade between whole hosts (onboarding, signed out, signed in) when the session changes, never during
   navigation. [More](#no-alpha-on-a-full-screen-layer)
4. **A screen's first composition is its final one.** Seed it from what is already in memory; load
   secondary sections after the slide has landed. Content swapped in mid-slide recomposes the arriving
   screen inside the motion. [More](#a-destinations-first-composition-is-final)
5. **Hide by parking, never by removing.** The shell keeps hidden tabs and covered screens composed,
   placed and invisible; keep `AppSheet` composed and drive `visible`. Removing a subtree throws away
   its layers, and showing it again rebuilds them on the frame that has to move.
   [More](#how-the-shell-moves-screens)
6. **Every lazy list: a stable `key`, a `contentType` when kinds mix, its padding inside the scroll, and
   `AppScrollDefaults.flingBehavior()`.** On a screen the padding is `chromeContentPadding()`; inside an
   `AppSheet` it is the sheet's `contentPadding`. `verifyCodeRules` checks all four in feature code.
   [More](#lists-and-very-large-data)
7. **Every motion comes from `AppTheme.springs` (its springs, and its fade specs for opacity); a motion
   that follows a finger ends on a spring seeded with the finger's speed.** [More](#motion-and-springs)
8. **An idle screen draws nothing.** Anything that loops pauses while its screen is hidden, and stops or
   slows under reduce motion (decoration stops; `AppActivityIndicator` slows, because its motion is the
   message). Gate: 5 s idle on every tab renders 0 frames. [More](#nothing-animates-while-idle)
9. **One opaque fill per screen.** The navigation entry paints the background; `AppScaffold` paints none
   on top of it. Removing a second full-screen fill cut the steady GPU time per frame of a push's slide
   from 8.0 to 4.9 ms. [More](#paint-the-screen-once)
10. **Keep the start-up path and the first frame free of work they do not need.** No I/O and no eager
    singletons in `Application.onCreate` or the first screen. Gate: cold start median 380 ms.
    [More](#start-up)

---

## What the frame budget looks like on a budget phone

- **8.33 ms per frame at 120 Hz.** Main-thread time (input, animation, composition, layout, recording)
  is graded against each frame's own refresh interval. A frame's whole pipeline (main thread, then the
  RenderThread, then the GPU) may run past one interval and still make its deadline.
- **The first frame after idle usually misses its deadline, whatever the app draws.** On the reference
  phone a frame that starts from idle gets a FrameTimeline deadline 13.67 ms after its vsync, and the GPU
  is still clocked down for it: 9-16 ms of GPU for a full-screen frame, against 4-6 ms a few frames
  later. A build with every screen's content removed missed that deadline on 7 of 7 tab switches from
  idle, and drawing less does not shorten that frame: removing one full-screen fill cut the GPU time of
  the steady frames after it, but moved the first frame's from 10.6 to 10.7 ms. So a transition's janky
  share is exactly one frame per transition: 1 of 87 frames, 1.15 %, on the tab-switch scenario.
- **What users see is the hitch, not the janky count.** A late first frame shows as the motion starting
  one vsync later, never as a stutter inside it. That is why the release limits judge transitions by the
  hitch time per second of motion and by the janky frames after each motion's first, and report the late
  first frames instead of gating them. See [What transitions protect](#what-transitions-protect).
- **The limits are the floor, not the goal.** The goal is about 0 % janky frames on tab switches,
  push/pop, back and scroll, and the lighter of two equally smooth designs. A row at its limit is not
  finished work.

---

## Recomposition and state reads

Recomposition is fine on the first frame of a transition or after a tap. It is a bug on every frame of
a scroll, an animation or a transition. Kotlin 2.4 turns strong skipping and lambda memoisation on, so
"is it skippable" no longer says much; three things still cost frames: new instances that are equal but
not identical, state read too high or too early, and composing a whole screen in the first frame of a
navigation.

### Read moving values where they are used

**Rule.** Animated, dragged and scrolled values are read only inside `Modifier.offset { }`,
`Modifier.layout { }`, `Modifier.graphicsLayer { }`, a draw block or a `Modifier.Node`. Hold an
animation's `State` with `val x = animate…(…)` and read `x.value` inside the lambda; never `by`.

**Why.** A value read in composition invalidates the composition that read it, so every animation frame
recomposes that subtree. Read in placement it re-places one node; read in a layer block it changes one
RenderNode property; neither composes anything.

**Do.**

```kotlin
@Composable
fun SavedBadge(saved: Boolean, modifier: Modifier = Modifier) {
    val springs = AppTheme.springs
    val lift = AppTheme.spacing.md
    // `emphasis` is the badge spring; its small overshoot is the pop.
    val shown = animateFloatAsState(if (saved) 1f else 0f, springs.emphasis.unit, label = "saved")
    AppIcon(
        AppIcons.Check,
        contentDescription = null,
        modifier = modifier
            // Read while placing and while drawing the layer: the animation composes nothing.
            .offset { IntOffset(0, ((1f - shown.value) * lift.toPx()).roundToInt()) }
            .graphicsLayer {
                alpha = shown.value.coerceIn(0f, 1f)
                // Alpha applied to each draw: no offscreen buffer for the fade.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
    )
}
```

**Don't.**

```kotlin
val y by animateDpAsState(if (saved) 0.dp else lift)   // animated-delegate: `by` reads it in composition
Box(Modifier.offset(y = y))                            // lint UseOfNonLambdaOffsetOverload
Box(Modifier.scale(progress))                          // recomposes on every frame of the animation
```

| Value | Where to read it |
|---|---|
| position, translation, parallax | `Modifier.offset { IntOffset(…) }`, `layout { … place(…) }`, `placeWithLayer` |
| scale, alpha, rotation, clip shape | `graphicsLayer { }` (the lambda form only) |
| a size that animates | `layout { }` or a `LayoutModifierNode`, never `Modifier.width(animated)` |
| colour of text | `BasicText(color = ColorProducer { … })` |
| background, scrim, progress fill | `drawBehind`, `drawWithContent`, a `DrawModifierNode` |
| a threshold crossing (show a title once scrolled) | `derivedStateOf` or `snapshotFlow { }.distinctUntilChanged()` |

**Protects.** Gates `recompose.back_gesture_progress.max = 0` (frames that recompose while the back card
follows the finger), `recompose.tab_switch.max = 2`, `recompose.pop.max = 2`; scroll janky at most
0.5 %.

**Enforced by.** `animated-delegate`; lint `FrequentlyChangingValue` and `UseOfNonLambdaOffsetOverload`;
the recomposition tests ([below](#prove-it-with-a-recomposition-test)).

### Take a provider for a value that changes often

**Rule.** A composable that receives a fast-changing value takes `() -> T`, not `T` and not `State<T>`,
and reads it where it is needed.

**Why.** A parameter that changes every frame recomposes the callee every frame. A provider defers the
read into the callee's layout or draw, where it costs nothing to the composition.

```kotlin
@Composable
fun UploadBar(progress: () -> Float, modifier: Modifier = Modifier) {
    val fill = AppTheme.colors.accent
    Box(
        modifier
            // Its own layer: a new progress value re-records this bar, not the layer of the screen around it.
            .graphicsLayer()
            .drawBehind {
                val width = size.width * progress().coerceIn(0f, 1f)
                val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(fill, topLeft = Offset(start, 0f), size = Size(width, size.height))
            },
    )
}
```

A draw block with no layer of its own records into the nearest layer above it, so without
`graphicsLayer()` every change of `progress` would re-record that whole layer's display list.

**Enforced by.** `state-param` (a `State<T>` parameter in features, `:core:ui` and the design system).

### Write custom modifiers as `Modifier.Node`

**Rule.** A custom modifier is a `ModifierNodeElement` with its `Modifier.Node`, never `Modifier.composed`
and never a `@Composable` modifier factory.

**Why.** `Modifier.composed` and composable factories allocate and compose per call site, on every
recomposition of the caller and for every row of a list. A node is created once and updated in place; its
reads in `measure`, `layout` and `draw` are observed without any composition.

**Also.** A node reads composition locals with `currentValueOf` in `measure`, `layout` or `draw`, never
in `onAttach`, where the value goes stale after a rotation or a display change. A node that implements a
node kind itself (`DrawModifierNode`, `SemanticsModifierNode`) is the only node of that kind Compose
visits for it: draw everything from that node.

**Enforced by.** `composed`; lint `UnnecessaryComposedModifier`, `ModifierFactoryReturnType`.

### UI state is read-only and holds no per-frame values

**Rule.** `UiState` holds read-only collections (`List`, `Set`, `Map`) and `val` properties, built by
the ViewModel. Scroll offsets, drag positions, animation progress and anything that changes faster than
about 10 times a second live in UI-layer state holders read in layout or draw. Nothing in composition
calls `.map { }` or `.filter { }` on state without `remember(keys)`; derive it in the ViewModel.

**Why.** `MviScreen` reruns the screen body on every new state, and children skip by parameter. A list
rebuilt in composition is a new instance on every recomposition, which defeats skipping in every row
that receives it. `config/compose-stability.conf` tells the compiler that `List`, `Set` and `Map` are
immutable here; a mutable collection in state would make that promise false and show stale UI.

**Enforced by.** `uistate-mutable` (`*State.kt` and `*Contract.kt` in features), `api-immutable` (models
in a data module's `api` package).

### Collect with the lifecycle

**Rule.** ViewModel state is collected with `collectAsStateWithLifecycle()`, never `collectAsState()`.
`MviScreen` already does it; a screen that collects anything else does the same.

**Why.** `MviViewModel.observe` runs its repository, database and session flows only while a screen
collects its state. The shell keeps hidden tabs and covered screens composed, and caps their lifecycle:
a hidden tab and every page deeper than the one directly under the top screen stay at CREATED, so their
lifecycle-aware collectors stop; the page directly under the top screen is STARTED once the screens stop
moving, so it is current when a pop returns to it. With `collectAsState()` every kept screen would keep
its flows running in the background and recompose out of sight.

**Enforced by.** `collect-as-state`; lint `StateFlowValueCalledInComposition`,
`LifecycleCurrentStateInComposition`.

### Keep parameters stable

**Rule.** `./gradlew check` runs `checkComposeStability` on `:core:designsystem`, `:core:ui` and
`:core:navigation`, the modules every screen calls. It fails on a parameter the compiler compares by
identity. Fix it with a stable type, an entry in `config/compose-stability.conf` for a type that really
is immutable, or a `() -> T` provider. An accepted exception goes in
`config/compose-stability-baseline.txt` as `fqName(parameter)  # reason`; an entry without a reason fails
the check.

**Why.** Under strong skipping an unstable parameter is compared with `===`: the composable reruns
whenever its caller passes a new instance, even an equal one, in every screen that uses it.

**Don't.** `kotlinx.collections.immutable` (`List`, `Set` and `Map` are already configured stable), and
`@Immutable` on a model that carries a large or nested list into a lazy layout.

**Enforced by.** `checkComposeStability`. `./gradlew <task> -PcomposeReports` writes the compiler's
reports and metrics for any other module.

### Write state before it is read, never after

**Rule.** State a parent computes for its children is written in composition before the children read
it. Never write it from a `SideEffect` and read it back in the same composition, and never write state
in composition that composition already read. Pass what composition knows down as parameters.

**Why.** A `SideEffect` runs after the composition that read the old value: the first frame is composed
with defaults and composed again on the next frame. In the shell this was one stale frame and a second
composition on the first frame and on every window-class change.

**Enforced by.** *review*.

### Static composition locals do not change on a transition frame

**Rule.** A `staticCompositionLocalOf` holds a value that never changes at run time, or a stable holder
whose fields are snapshot state (`LocalBottomChrome` and `LocalTopChrome` hold a `ChromeInset`,
`LocalTabVisible` a `TabVisibility`). Never give a static local, or a screen's lifecycle owner, a new
value on a frame that moves screens.

**Why.** A static local is not read-tracked: a new value recomposes the whole subtree under the
provider. When a popped screen lost its `LocalPaneContext` on the pop's first frame, that frame composed
the whole leaving screen, 416 composables. The shell now keeps the leaving screen's pane context until it
has slid off.

**Enforced by.** *review*; the shell's recomposition tests.

### Window size changes recompose only at a breakpoint

**Rule.** Lay out from `AppTheme.windowClass` and `AppTheme.containerClass`, which change only when a
breakpoint is crossed. Read the window's exact size from `LocalWindowMetrics` (`sizeDp`) inside a layout
or draw lambda. Never read `LocalWindowInfo.current.containerSize`, the configuration's screen size or
the display metrics in composition.

**Why.** The exact size changes on every frame of a resize, a fold or a desktop window drag. Read in
composition it recomposes everything under it on every step; the window size class does not move inside
a class.

**Protects.** A resize within one size class causes no recomposition of the classes' readers.

**Enforced by.** `adaptive-screen-metrics`, `adaptive-orientation`, `adaptive-device-type`; lint
`ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`.

### Callbacks, `derivedStateOf` and `snapshotFlow`

- Do not wrap callbacks in `remember`: the compiler already memoises lambdas and bound references such as
  `viewModel::onEvent`. What still breaks memoisation is capturing something rebuilt on every
  recomposition.
- Use `derivedStateOf` only when its input changes more often than its output
  (`listState.firstVisibleItemIndex > 0`), remember it, and key the `remember` on every parameter it
  captures, or it keeps the first value it saw. For a plain combination use `remember(a, b)`. Read in
  composition, it still costs a recompose pass in every frame its input changes, even while its value
  holds: no composable runs, so a recomposition test counts nothing, but the pass is in the frame. When
  the result only starts an effect or an animation, read the input in a `snapshotFlow` inside that
  effect instead (a large title bar that read its collapse in composition went through 622 empty
  recompose passes in one traced scroll).
- Drive side effects from state with `snapshotFlow { … }.distinctUntilChanged()` collected in a
  `LaunchedEffect`: haptics at a threshold, persisting a scroll position, analytics.
- `Column`, `Row` and `Box` are inline: state read in their content lambda invalidates the nearest
  non-inline composable around them. Move a frequently changing read into a small composable of its own,
  or better into a lambda.

**Enforced by.** lint `UnrememberedMutableState`, `UnrememberedAnimatable`, `AutoboxingStateCreation`,
`RememberReturnType`, `FlowOperatorInvokedInComposition`; *review* for the rest.

### Text costs

**Rule.** Leave line breaking and hyphenation unset: `AppTypography` sets neither, and Compose resolves
an unspecified value to `LineBreak.Simple` and `Hyphens.None`, the cheapest settings. Set
`LineBreak.Paragraph` or `Hyphens.Auto` only on long-form text that is never on a transitioning
screen. No `TextAutoSize` in rows or on screens that slide (it lays the text out several times). Bound
text in rows with `maxLines` and `overflow`. Build an `AnnotatedString` in the ViewModel or inside
`remember`. Animate a text colour through a `ColorProducer`, which redraws without a new layout.

**Enforced by.** *review*.

### Prove it with a recomposition test

**Rule.** A new screen or component that animates adds a recomposition test for its journey. The JVM
harness in `:core:testing:compose` counts every composable that executes, frame by frame:
`RecompositionCounter` is a test rule (chained outside the compose rule), `framesUntilIdle` steps the
paused clock one frame at a time, and `assertQuietBetweenEnds()` fails when any frame between the first
(the state change) and the last (the settle) executed something outside an allowlist.

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class SavedBadgeRecompositionTest {

    private val counter = RecompositionCounter()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(counter).around(compose)

    @Test
    fun `saving composes nothing while the badge moves`() {
        var saved by mutableStateOf(false)
        compose.setContent { AppTheme { SavedBadge(saved) } }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        counter.reset()

        saved = true
        val frames = compose.framesUntilIdle(counter)

        frames.assertQuietBetweenEnds()
    }
}
```

Budgets the shell's tests hold: a switch to a visited tab recomposes only the two bar cells whose
selection flips; a push composes the new screen once and nothing while it slides; a pop composes neither
the leaving screen nor the one it returns to; a press composes nothing.

<!-- <opt:performance> -->
**On the phone.** `perf.sh recompose` counts the frames that recomposed in each transition of a release
build from an atrace capture. Gates: tab switch at most 2 frames, pop at most 2, the back card's
progress frames 0, push reported (5 today). Layout Inspector's counts are for exploring, never for
judging.
<!-- </opt:performance> -->

---

## Lists and very large data

### Key every row; type rows that differ

**Rule.** Every `items(…)` call in a lazy layout passes a stable `key`. A scope with more than one kind
of item gives each kind a `contentType`.

**Why.** Without a key a row's state and the scroll position follow its index, not its item, and
`animateItem` cannot work. Compose reuses a row's composition only for an item of the same content type,
so a mixed list without types composes rows from scratch as they scroll in.

**Enforced by.** `lazy-key`, `lazy-content-type`.

### Pad scrolling content inside the scroll

**Rule.** A lazy list takes `contentPadding = chromeContentPadding(base)`; a `verticalScroll` chain goes
on to `.padding(chromeContentPadding(base))` after the scroll. Never pad the list's container or parent,
and never read `LocalBottomChrome.current.value` in composition to pad a list. Call
`ReportScrollEdge(state)` beside the screen's primary list. Inside an `AppSheet` the list takes the
sheet's own padding instead, `contentPadding = contentPadding` from `AppSheetScope` (see
[Sheets](#one-root-that-takes-contentpadding-no-keyboard-padding-no-extra-scroller)).

**Why.** Content runs under the floating bars and rests clear of them only when the padding is inside
the scroll. `chromeContentPadding()` reads the bars' heights when the list measures its padding (the
bottom never less than the navigation bar), so a bar settling or the keyboard rising lays the list out
again and recomposes nothing. `ReportScrollEdge` lets a docked bar show its surface, and the bottom edge
its fade, only while content runs under them.

```kotlin
@Composable
internal fun OrdersList(orders: List<OrderRow>, onOpen: (Long) -> Unit) {
    val listState = rememberLazyListState()
    ScrollToTopOnReselect(listState)   // a tab's root screen only
    ReportScrollEdge(listState)
    LazyColumn(
        state = listState,
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
        flingBehavior = AppScrollDefaults.flingBehavior(),
    ) {
        items(orders, key = { it.id }, contentType = { it.kind }) { order ->
            OrderRow(order, onOpen = onOpen, modifier = appAnimateItem())
        }
    }
}
```

**Enforced by.** `scroll-chrome` (vertical lazy lists and grids and `verticalScroll` in features; it
accepts `chromeContentPadding(…)` or a `contentPadding` passed through).

### Name the fling on every scrolling container

**Rule.** Every `LazyColumn`, `LazyRow`, lazy grid, `verticalScroll` and `horizontalScroll` passes
`flingBehavior = AppScrollDefaults.flingBehavior()`; a pager passes
`AppScrollDefaults.pagerFlingBehavior(state)`. A container the person cannot scroll
(`userScrollEnabled = false`) needs none.

**Why.** Compose has no composition local for the default fling, so a container that names none gets
Android's spline fling and decelerates differently from its neighbours. The template's fling keeps 0.998
of its speed per millisecond (UIScrollView's normal rate), ends at 30 px/s (at Compose's default rest
threshold a gentle release keeps a list moving invisibly for four seconds, and a tap in that time only
stops the fling), and a new flick in the direction of a fling a finger stopped less than 500 ms before,
at least half as fast, carries part of that fling's speed on, as on iOS. The edge needs nothing per
container: `AppTheme` installs the rubber band (`AppOverscroll`, c = 0.55) for every scrolling container
below it.

**Enforced by.** `scroll-fling` (features, `:core:ui`, `:app`).

### Animate rows with `appAnimateItem()` only

**Rule.** A row that moves, appears or leaves uses `appAnimateItem()`, an extension on the row's
`LazyItemScope`, on a keyed row: `modifier = appAnimateItem()`, or `appAnimateItem(Modifier.fillMaxWidth())`
to chain a modifier. It places rows on the theme's `layout` spring, fades them in over `fadeSlow`
(200 ms) and out over `fadeQuick` (120 ms), and does nothing under reduce motion.

**Why.** One path for row animation keeps placement in the layout phase. `animateContentSize()` on list
items re-measures the list every frame of the change.

**Enforced by.** *review*.

### No subcomposition in rows

**Rule.** No `SubcomposeAsyncImage`, `BoxWithConstraints` or other `SubcomposeLayout` inside a row. Size
from `AppTheme.containerClass`, from the constraints in a custom `Layout`, or from a fixed slot.

**Why.** Subcomposition composes during measure, after the frame's composition pass, and lazy prefetch
cannot hide it: prefetch runs only between frames. A subcomposing row costs a composition each time it
enters, and `BoxWithConstraints` costs a frame. If state must reach a `SubcomposeLayout`'s children,
give them a `() -> T` that reads state the event writes; `rememberUpdatedState` written while the parent
recomposes reaches them a frame late.

**Enforced by.** `subcompose-image` (features, `:core:ui`), `adaptive-box-constraints` (features).

### Loading states: the real row, redacted

**Rule.** A loading list is the real rows with placeholder data inside `AppRedacted(active = true)`,
with the loaded list's `contentPadding`, in a `LazyListState` of its own, and it is replaced by the
loaded list in one frame: no `AnimatedContent`, no cross-fade over full-screen content.

**Why.** The redacted row is exactly as tall as the row that replaces it at every font scale, so nothing
moves when data arrives. A cross-fade over two full-screen lists draws both, through alpha. A shared list
state compares the loaded rows with the placeholders and fades every row in as new. The shimmer is one
band per window (`ShimmerClock`: a 1200 ms sweep, then an 800 ms pause that draws no frames); it writes
no snapshot state, so it never recomposes, it redraws only the bones' own layers, and it stops while no
bone is on screen.

**Enforced by.** *review*. A bone sized by the text style's `lineHeight` instead of the text line made
every row jump by 4 dp when the data arrived.

### Scroll to the top cheaply

**Rule.** Go to the top with `LazyListState.animateScrollToTop()` or `ScrollState.animateScrollToTop()`.
A tab's root screen calls `ScrollToTopOnReselect(listState)`: Android delivers no status-bar tap, so
tapping the selected tab again is the way to the top.

**Why.** `animateScrollToItem(0)` alone animates through every row in between, up to 100 of them,
composing each. `animateScrollToTop()` jumps to two screens from the top first, so only those rows
compose.

### The first fling after an install or `pm clear`

Skia compiles a GPU program the first time it draws a new combination, on the RenderThread. On the
first fling after `pm clear`, the one first-use program was a `CircularRRectOp` (11-31 ms) for a card
clipped to its rounded shape once the list's edge cut it. `AppSurface` clips content only when a caller
asks (`clipContent = false` by default), and with the fill drawn outside the clip no program compiled in
6 of 6 runs. Keep it that way: clip a card's content only when the content overflows its shape.

Pull to refresh acts on `NestedScrollSource.UserInput` only, so the leftover of a fling reaching the top
never pulls the indicator out (it used to compile more programs on the first fling).

**Protects.** No hitch on the first fling after a cold start or `pm clear`.

### A cache window only behind a trace

Lazy prefetch is pausable and on by default. Add a `LazyLayoutCacheWindow` to a list only when a scroll
trace shows `compose:lazy:prefetch` running out of time, and check PSS after. No list in the template
uses one.

**Lists protect.** Gates `scroll_root.janky_pct` at most 0.5 % and `scroll_root.gpu_p90_ms` at most
7.0 ms, the same for `scroll_minimize`; no recomposition per scroll frame.

---

## Screen transitions, the back gesture and the back card

Features never move screens themselves: they register `entry<Key>(pane = …)` and navigate through
`AppNavigator`. The shell does the moving, and it is built so that a transition records and composes as
little as possible. Know what it does, so a screen does not undo it.

### How the shell moves screens

- **One spring for every full-screen move.** Push, pop, covers and the back card's commit ride the
  `screen` spring (0.344 s in the Standard style, and no bounce in any style). The covered page moves a
  third as far (parallax 0.333) under a drawn scrim (0.10 light, 0.24 dark) with an 8 dp edge shadow at
  0.04. No alpha and no scale on whole screens, except the back card (and `HostCrossfade` between hosts,
  below).
- **The slide starts after the frame that composes the new screen.** The spring is launched after the
  composition that changed the stack, so the heavy composing frame shows nothing moving and the first
  step of the slide is not skipped.
- **Only what is visible is drawn.** During a slide only the strip of the lower page the upper page has
  not reached is drawn (a scissor clip); under the back card the page beneath is drawn only around the
  card, in up to four whole-pixel strips.
- **Each screen is a layer of its own.** A slide frame records only the few operations around each
  screen instead of both screens' whole drawing: the main thread records 38 % less per frame, and the
  share of a push's frames that took 6 ms or more on the RenderThread fell from a fifth or a sixth to
  6 %.
- **Hidden tabs and covered screens are parked.** They stay measured and placed, their layer at alpha 0
  off the window and their semantics cleared, so their display lists survive and showing them changes
  two layer properties. A hidden tab's lifecycle is capped at CREATED, lowered the frame after it is
  hidden and raised two frames after it shows, so no collector stops or starts on the switch frame. In a
  stack, the page directly under the top screen is STARTED once the screens stop moving and every deeper
  page is CREATED. Pages deeper than three leave composition and come back from saved state.
- **Tab switches never cross-fade.** The incoming tab is nudged 12 dp toward the direction of travel and
  springs in on the `thumb` spring, moved in its layer block only; nothing is drawn over it and the
  outgoing tab is parked at once. A deep link, a navigation to a root and reduce motion switch with no
  motion. There is no veil: with a full-screen veil drawn over the incoming tab, the standard preset's
  tab switch measured a GPU p90 of 12 ms, against the 8 ms past which the design called for dropping it,
  and because the veil was read in draw, the tab's whole display list was recorded again on every frame
  of the reveal. The bar's indicator moves on two edges (lead 0.22 s, trail 0.32 s, stretch at most
  1.5 slots) and the selected icon dips to 0.84.
- **Unopened tabs are built while idle.** `TabWarmUp` precomposes up to five tab roots with
  `createPausedPrecomposition`, in slices that stop before the next frame is due, then draws each once
  under the opaque tab in front, so shaders, glyphs and textures are paid out of sight. Its lifecycle
  stays CREATED until it is shown, so a screen that loads when started does not load early.
- **Commands queue.** Programmatic navigation during a transition waits; a second push during a push is
  dropped; Back pressed while screens move is counted and applied as one pop at 99 % of the travel, never
  past the root. Whole hosts (onboarding, signed out, the signed-in shell) swap with `HostCrossfade`,
  and only the arriving host takes navigator commands; Back is swallowed while it runs.
- **`HostCrossfade` is the shell's one whole-screen cross-fade.** It fades the leaving host out and the
  arriving one in over 200 ms with `AnimatedContent`, both composed, over one drawn background. That
  renders each host offscreen on every frame of the fade, which is acceptable only because it runs when
  the session changes (onboarding finished, signed in, signed out), never during navigation. Nothing else
  in the app cross-fades whole screens.
- **Layer-only motion asks for 120 Hz while it moves.** A slide and the back card move layers, which cast
  no frame-rate vote of their own, so the stack votes `FrameRateCategory.High` only while they move.

### Rules for screens

#### A destination's first composition is final

**Rule.** Build a destination's initial state from what the caller or an in-memory cache already has,
synchronously, so its first frame shows the content: give the repository a `peek(id)` that returns the
item from memory with no I/O, and start the ViewModel from it. Show a skeleton only when nothing is in
memory. Load secondary sections (related rows, a second tab of detail) after the slide has landed:
`LocalNavigationSettled.current` is a `() -> Boolean`, read in an effect, true from 99 % of the travel.

**Why.** Every state change that lands during the slide recomposes the arriving screen inside the
motion. Measured: a push recomposed in 5 frames; in 2 of 8 pushes the loaded state arrived one frame
after the composing frame, and that frame took 15-18 ms on the main thread. Related rows loaded with the
item composed 185 composables 220 ms into every push, in the middle of the slide.

```kotlin
@HiltViewModel(assistedFactory = OrderViewModel.Factory::class)
internal class OrderViewModel @AssistedInject constructor(
    private val repository: OrderRepository,
    @Assisted private val orderId: Long,
) : MviViewModel<OrderState, OrderEvent, OrderEffect>(
    // The first frame already has the order when the list put it in memory.
    repository.peek(orderId)?.let { OrderState(order = it, loadState = LoadState.Success) } ?: OrderState(),
) { … }

@Composable
internal fun OrderRoute(orderId: Long, viewModel: OrderViewModel = …) {
    val settled = LocalNavigationSettled.current
    LaunchedEffect(Unit) {
        snapshotFlow { settled() }.first { it }
        viewModel.onEvent(OrderEvent.LoadRelated)
    }
    MviScreen(viewModel) { state, onEvent -> OrderScreen(state, onEvent) }
}
```

**Don't.** Start from an empty state and send a load event that swaps a skeleton for content during the
slide; read `LocalNavigationSettled.current()` in composition (it changes twice per transition).
<!-- <opt:sample> -->

The reference feature's detail screen does not follow this rule yet: `SampleDetailViewModel` starts from
an empty state, loads when the screen appears and loads the related rows in the same coroutine as the
item, although `SampleRepository.peek(id)` exists for exactly this. Copy its assisted injection, not its
loading.
<!-- </opt:sample> -->

**Protects.** `pushpop.push_first_frame_ms` (median 14.4, max 18.3 ms, non-regression) and
`recompose.push.max` (reported, 5 today).

**Enforced by.** *review*. The shell's push recomposition test pushes a probe screen that loads
nothing, so it checks the shell, not whether a feature seeds its first composition.

#### Paint the screen once

**Rule.** A screen's background is painted once per frame. Every navigation entry paints
`colors.background` and says so through `LocalPaintedBackground`; `AppScaffold` paints no background on
such a screen. Never add a full-screen background under a screen that already paints one, and keep every
surface opaque or over an opaque one: the window's own background turns transparent after the app's
first frame.

**Why.** HWUI does no occlusion culling: a fill that is covered is still drawn, on every frame the
screen draws. A full-window fill is 2.6 Mpx. Measured on the reference phone with the scaffold's second
fill removed, the steady GPU time per frame fell from 6.4 to 5.4 ms on a tab switch and from 8.0 to
4.9 ms during a push's slide: headroom under the GPU p90 limits and less battery on every frame. It does
not shorten the first frame of a transition (10.6 against 10.7 ms of GPU); that frame is late because
the GPU starts at its idle clock, not because of fill. The debug overdraw view once showed three fills
under every screen (the window, the entry, the scaffold).

**Protects.** `scroll_root.gpu_p90_ms` and `scroll_minimize.gpu_p90_ms` at most 7.0 ms.

**Check.** `adb shell setprop debug.hwui.overdraw show` on a release build, then set it back to `false`.

#### Do not touch transitions from a feature

**Rule.** A feature never sets `NavDisplay` transitions and never touches a back stack; it picks a
`NavTransitionStyle` when it registers an entry. Sheets and covers are presentations
(`Presentation.Sheet`, `Presentation.Cover`), never content shown inside a tab.

**Enforced by.** `nav-fade` (it matches the `NavDisplay` transition, pop and predictive-pop specs);
*review* for the back stack.

#### Handle Back through navigationevent

**Rule.** Handle Back with `NavigationBackHandler` (an overlay at overlay priority), never the
Activity's `OnBackPressedDispatcher`, `BackHandler` or `PredictiveBackHandler`. Keep a handler's enabled
state cheap to change: inside `:core:navigation`, a handler whose enabled state follows navigation state
is registered with `BackHandlerEffect(handler) { enabled }`, because toggling `isBackEnabled` as a
parameter recomposes the caller on every flip.

**Why.** One back system lets the shell coalesce rapid presses, run the back card, and leave the start
tab's root with no handler so the system's back-to-home animation plays. `PredictiveBackHandler` costs a
Flow and a coroutine resume per event.

**Enforced by.** `legacy-back`; `verifyManifestRules` (`enableOnBackInvokedCallback="true"`).

### The back gesture

**What it is.** Gmail's card: the screen shrinks to 0.90 about its centre, drifts toward the far edge
until an 8 dp gap remains, follows the finger vertically up to 24 dp, rounds its corners to 28 dp, over a
0.32 black scrim that fades in over 50 ms. On commit the card returns to full size while its content
fades over 100 ms and the stack pops with no slide; on cancel the system springs the gesture's progress
back to 0, so the card returns to full size, and the scrim fades out over 100 ms. A back
event with no swipe edge, the Back key, and Android below 14 pop with the slide. A back swipe during a
push shows one opaque card and lands on the list. At the start tab's root no handler is enabled, so the
system's back-to-home animation plays.

**How it stays cheap.**

- The card's progress is read only in layer and draw blocks: a gesture recomposes nothing (gate
  `recompose.back_gesture_progress.max = 0`).
- Each progress event is applied in the frame it arrives in. Writes made outside Compose's own frame are
  applied after the frame unless the handler sends the apply notification itself; before that, 161 of
  175 progress steps were drawn a frame late.
- **The card is drawn from one texture.** From the gesture's first frame until the card is gone, the
  screen the card shows (the one being dismissed) is drawn from its own innermost layer as a hardware layer
  (`CompositingStrategy.Offscreen`), rendered once per gesture while its content is still; every later
  frame moves, scales and fades that one texture instead of replaying the screen's drawing.

**The texture is an exception to "no whole screen offscreen", one of the shell's two** (the other is
`HostCrossfade`, [above](#how-the-shell-moves-screens)). That rule exists because a fade with alpha
below 1 renders the screen offscreen again on every frame. HWUI renders a persistent layer
again only when its own content changes, not when its transform or alpha does, so a texture rendered once
per gesture is the opposite case. It is limited to the back card (`BackCardState.textures`), released
when the card is gone, and guarded by tests that check the texture is the screen's innermost layer for
exactly the card's span and that the screen is not recorded again in between. Measured trade, when the
texture was adopted:

| | Before | With the texture |
|---|---|---|
| Mid-gesture hitches caused by the RenderThread, six runs | 38 | 2 |
| RenderThread per gesture frame, median | 2.24 ms | 1.50 ms |
| Hitch time per second of motion, back-gesture scenario | about 20 ms/s | 8-16 ms/s |
| First frame's GPU time (the card starts about one vsync later) | 10.8 ms | 14.9 ms |
| GPU memory while the card is up | 2.8 MB | 12.3 MB (10.1 MB purgeable) |

Text in the card is very slightly softer (it is drawn from a texture scaled 0.9-1.0).

The texture is rendered once per gesture only while the screen in the card holds still. Anything that
changes in that screen while the card is up (a spinner, a skeleton's sweep, a list still flinging)
renders the whole texture again for that frame; the decision that adopted the texture accepted that
cost. No signal tells a screen that it is in the card (`LocalNavigationSettled` turns false only while
the card commits), and the template's own skeleton shimmer and activity indicator keep animating in it.
The gesture's release rows are within their limits and its first three frames are not yet; see
[Where the template stands](#where-the-template-stands). Anything a screen redraws on its own while it
is in the card adds to every one of them.

**Don't.** Navigation 3's built-in predictive pop (it gets no touch position, recomposes per event, and
its default fades a whole screen offscreen every frame); reading a navigation event's transition state
in composition; `GraphicsLayer.toImageBitmap` or `PixelCopy` for the card; a second texture for the page
beneath.

### What transitions protect

| Scenario | Release limits | Non-regression rows |
|---|---|---|
| Tab switch (`tabs`, `tabs_tap`) | hitch at most 5.0 ms/s; janky after each switch's first at most 0.5 % | janky 1.26 % (lean 1.0); visited tab first frame median 7.6 ms (standard and lean 8.0), max 11.9 (standard 10.3, lean 16.0); plain taps median 4.5, max 11.7 |
| Push and pop with the Back key (`pushpop`) | hitch at most 5.0 ms/s; janky after each motion's first at most 0.5 % | composing frame median 14.4, max 18.3 ms; pop first frame median 4.1 (standard 2.7), max 5.9; janky 1.95 % (lean 1.73); p99 35 ms (standard 33, lean 29) |
| Back gesture (`pushpop_gesture`) | hitch at most 5.0 ms/s; janky after the first at most 1.0 % | janky 3.1 %; slowest of the first three frames median 2.7 ms |
| Rapid Back (`rapid_back`) | alive, lands on the expected screen; hitch at most 5.0 ms/s; janky after the first at most 1.0 % | janky 1.18 %; coalesced pop frame median 6.2, max 7.9 ms |
| Navigation stress | alive, stack invariants hold, janky at most 4.0 % | |

### Where the template stands

The Everything preset's release build, compiled with its profile, on the reference phone, as last
measured: the release gate's run (the median of three runs), and where a range is given, the spread of
single-run blocks over several sessions of the same build. The hitch is in ms per second of motion.

| Scenario | Measured | Against the limits |
|---|---|---|
| Tab switch | hitch 0 ms/s (plain taps 0.79), janky after the first 0 %; plain taps' first frame median 4.87-4.95 ms, max 11.8 ms | release rows within; plain taps' first frame over 4.5 and 11.7 |
| Push and pop with the Back key | hitch 0-1.02 ms/s, janky after the first 0 %; the frame that composes the screen max 20.99-21.94 ms; the pop's first frame max 5.87-6.29 ms | release rows within; the composing frame over 18.3, the pop's first frame over 5.9 |
| Back gesture | hitch 1.42-4.28 ms/s in 10 of 11 blocks (8.54 in one), janky after the first 0-0.41 %; slowest of the first three frames median 2.95-4.44 ms | release rows within; the first three frames over 2.7 |
| Rapid Back | hitch 1.57 ms/s, janky after the first 0.46 % | within |
| Scroll a root list; with the bar minimizing | janky 0.59-1.61 % (median about 0.74); 0.74-1.76 %; GPU p90 7 ms in both | janky over 0.5 % in both; GPU p90 at its 7.0 limit |
| Navigation stress | janky 5.21-6.82 % | over 4.0 % |
| Idle, recomposition, memory, cold start | 0 frames; tab, push, pop, back card 1, 5, 2, 0; app PSS 54.4 MB; cold start 362 ms compiled with the profile, 389 ms as installed | within |

The rows over their limits are open work in the template, not targets it meets: build nothing that makes
them worse, and measure a change to the shell, a list or the bar against them.
<!-- <opt:performance> -->

While a row here is over, `--gate solid` fails on every build, whatever the change; a non-regression
limit fails it like a release limit, so the table lists those it fails too. So being done (item 8
of [Before you merge UI work](#before-you-merge-ui-work), and "Smooth and light" and "Before you say it
is done" in `AGENTS.md`) excuses exactly the limits this table lists as over, at no worse than the values
it gives. A change that brings a row within its limits updates this table in the same change; when every
row reads "within", that exception goes from all three places.
<!-- </opt:performance> -->

---

## Sheets

`AppSheet` is the template's one sheet: it follows the finger 1:1, measures its content once per settle
and never during a drag, and moves only layers and placement while it moves. A screen keeps it that way
by using it as designed.

### Keep `AppSheet` composed and drive `visible`

**Rule.** Call `AppSheet` unconditionally and drive `visible` (or `item`); set `visible` to false in
`onDismissRequest`. A sheet called in a loop needs `key(…)`.

**Why.** A sheet removed by an `if`, a `when` or a lazy item vanishes without its exit, and its next
opening composes and measures it from nothing in the frame that has to start the motion.

```kotlin
var editing by rememberSaveable { mutableStateOf(false) }
AppSheet(visible = editing, onDismissRequest = { editing = false }) {
    SheetColumn {   // one root; it takes the sheet's contentPadding by default
        NoteField(…)
    }
}
```

**Enforced by.** `sheet-conditional`.

### One root that takes `contentPadding`; no keyboard padding; no extra scroller

**Rule.** A sheet's content is one root, a lazy list or `SheetColumn`, given
`AppSheetScope.contentPadding`. Never add `imePadding()`, `windowInsetsPadding(WindowInsets.ime)` or
`fitInside(WindowInsetsRulers.Ime)` inside a sheet, and never wrap its content in another scroll
container.

**Why.** `contentPadding` already keeps the last item above the footer, the navigation bar and the
keyboard, and it is read in measure. IME padding inside the sheet re-measures the content on every
keyboard frame. A second root stacks at the top start of the content box; a wrapping scroller breaks
lazy lists.

**Enforced by.** `sheet-root`, `sheet-ime-padding`.

### Let the sheet own its geometry

**Rule.** `AppSheet(modifier = …)` is for test tags, semantics and focus. The sheet owns its size,
position, padding, clip, shadow and layers. Inside the sheet system: no `Dialog` or `Popup` window, no
`Modifier.shadow`, RenderNode elevation or size-following `dropShadow`, and movement only through the
sheet's own drag and fling (never `dispatchRawDelta` or `settle`). Outside it, a `when` over
`SheetDetent`, `SheetModality`, `SheetDismissSource` or `SheetMaterial` has an `else` branch, and a
feature never opts into `ShellSeamApi`, the seams through which the shell and the navigation hosts feed
the sheets.

**Why.** Sheets draw in the window's overlay layer: a separate window would add a window to the first
frame and cut the card stack and glass sampling. A floating sheet changes size on every drag frame, and
Compose's shadow rebuilds a CPU-blurred bitmap for each new size and caches every one without a bound;
the sheet draws its shadow from one cached nine-slice instead, outside its shape, in light theme, and only
on non-modal, side, form and card surfaces and modal sheets whose scrim alpha is below 0.2. The sheet
types gain values as the sheet system grows, and a `when` without an `else` would stop compiling.

**Enforced by.** `sheet-modifier`, `sheet-window`, `sheet-shadow`, `sheet-raw-delta`,
`sheet-when-else`, `shell-seam`.

### A custom detent reads no state

**Rule.** A `SheetDetent.Custom` resolver runs during measure and reads no snapshot state: capture the
values it needs and remember the detent keyed by what it captured.

**Why.** A state read in measure re-resolves the detents whenever that state changes, mid-drag included.

**Enforced by.** *review*.

### How a sheet moves, and what it costs

- **Release rule.** A flick of at least 400 dp/s moves exactly one detent. A dismissal needs at least
  2 sheet heights per second, or a release at least 48 % of the way from the lowest detent to hidden;
  otherwise the sheet goes to the detent nearest its position plus 0.099 s of its velocity. Past the top
  detent it resists with the rubber band (c = 0.55) up to 0.008 of the window's height.
- **Card stack.** Under a modal sheet at the large detent, on a Compact-width window, the screen behind
  recedes to 0.9165 with a 10 dp peek. Its presenter is three nodes (a recede layer, a dim-and-clip draw
  node, a content layer), so a frame of sheet motion re-records only the dim node, and the presenter
  under an opaque sheet draws only the band still visible above it.
- **Dim.** One rectangle per presenter, drawn once: the exact composite of the overlay and every scrim
  that reaches it, so it never gets lighter as the sheet rises.
- **Budget.** 0 recompositions per drag move; at most 1 at a settle, in readers of the current detent;
  at most 2 content re-measures per keyboard transition.
- **Known cost.** On the reference phone, a sheet with a list inside spends 13.5-24.2 ms of main-thread
  work on its opening frame, 10-16 ms of it measuring and laying out the list's first rows; a light sheet
  spends 4-7 ms. Keep a sheet's first screen of content light.
<!-- <opt:coil> -->

---

## Images

### Draw remote images as painters

**Rule.** Show a remote image with `AppNetworkImage` (or Coil's `AsyncImage` with `placeholder` and
`error` painters), never `SubcomposeAsyncImage`. Inside an active `AppRedacted` it draws a skeleton and
loads nothing.

**Why.** `SubcomposeAsyncImage` composes per load state and swaps the subcomposed content when the load
finishes, mid-scroll; Coil's own documentation calls subcomposition slower than regular composition.
With painters a finished load changes only what is drawn: nothing recomposes, and a scrolling list stays
at one composition per row.

**Enforced by.** `subcompose-image`.

<!-- <opt:network> -->
### One image loader, on the app's client

**Rule.** Coil's singleton loader is set once, in `BaseAppApplication.onCreate`, on the app's own
`OkHttpClient` (`newBuilder().cache(null)`: Coil keeps images in a disk cache of its own). Never build a
second loader, and never let Coil build its default client.

**Why.** One client means one connection pool and one set of certificate pins for HTTP, sockets and
images; a default loader builds a client of its own with neither, and a second loader means a second
memory cache.
<!-- </opt:network> -->
<!-- <opt-else:network> -->
<!--~ ### One image loader -->
<!--~ -->
<!--~ **Rule.** Never build a second image loader. This project has no network module, so Coil's own -->
<!--~ singleton loader fetches images with an `OkHttpClient` it builds itself. Once the app has a client of -->
<!--~ its own, set the singleton loader on it (`SingletonImageLoader.setSafe` in the `Application`), so images -->
<!--~ share its connection pool and certificate pins. -->
<!--~ -->
<!--~ **Why.** A second loader means a second memory cache, and a second client a second connection pool. -->
<!-- </opt-else:network> -->

**Enforced by.** *review* (`AGENTS.md`: no second image loader).
<!-- </opt:coil> -->

---

## Motion and springs

### Springs from tokens, never `tween(300)`

**Rule.** Every spring and decay comes from `AppTheme.springs`; fades of colour and opacity use
`springs.fadeIn()` (200 ms) and `springs.fadeOut()` (120 ms). No raw colour and no raw `dp` other than
`0.dp` and `1.dp` in a feature.

**Why.** The springs follow the app's motion style (`AppMotionStyle`), are built once per process with
visibility thresholds (so the invisible tail of a settle stops drawing) and allocate nothing per
recomposition. Under reduce motion (`AppTheme.reduceMotion`) components shorten or drop their motion.

| Spring (Standard style) | Response | Bounce | Used for |
|---|---|---|---|
| `press` | 0.16 s | 0 | finger down |
| `release` | 0.32 s | 0.15 | finger up |
| `thumb` | 0.28 s | 0 | switches, segmented controls, the tab reveal, the bar minimizing |
| `indicatorLead` / `indicatorTrail` | 0.22 s / 0.32 s | 0.15 / 0 | the tab indicator's two edges |
| `screen` | 0.344 s | 0 | push, pop, covers, the back card's commit |
| `sheet` | 0.34 s | 0 | sheets and detents, toasts, search focus |
| `dialog` | 0.275 s | 0 | alerts, action sheets, menus |
| `layout` | 0.36 s | 0 | list placement, expand and collapse |
| `emphasis` | 0.5 s | 0.15 | pop-ins, badges, success marks |
| `interactive` | 0.15 s | 0.14 | a value trailing the finger |
| `bounceBack` | 0.444 s | -0.1 | content returning from past an edge |
| `fling` / `flingFast` | decay keeping 0.998 / 0.99 of the speed per ms | | scrolling / pagers and pickers |

Full-screen moves never bounce in any style. Calm and Snappy change the travel times; see
`DesignTokens.Motion`.

**Enforced by.** `raw-tween`, `raw-color`, `raw-dp`.

### A motion a finger started ends on a spring with the finger's speed

**Rule.** When a finger lets go, animate to the target with a spring from `AppTheme.springs` seeded with
the release velocity (`Animatable.animateTo(target, spec, initialVelocity)`); interrupting retargets with
`animateTo`, never `snapTo` on an animation the user can see. For a 0-to-1 fraction, divide the velocity
in px/s by the travel.

**Why.** A release that ends on a tween, or on a spring started from rest, shows a speed discontinuity
that `gfxinfo` does not count as jank but people see.

**Enforced by.** `raw-tween`; *review*.

### Say which press feedback a control uses

**Rule.** A tappable control uses `Modifier.pressable(onClick, feedback = PressFeedback.…)`: `Scale` for
filled buttons, chips and cards (0.96, 0.98 for wide controls, 0.90 for icon-sized ones), `Dim` for text
and icon buttons (0.45), `Highlight` for rows (on at once, fading over 220 ms after the lift), `None`
where the control's own change is the answer. No ripple, no grey overlay.

**Why.** One node does the click, the touch target and the feedback, and reads the press only where the
content is placed: pressing recomposes and re-measures nothing (`PressableTest` asserts zero
recompositions). A bare `clickable` under the app's `NoIndication` gives no feedback at all.

**Enforced by.** `bare-clickable`, `ripple`, `material`.

### No alpha on a full-screen layer

**Rule.** Never fade a whole screen or a full-screen layer with `alpha` below 1. Draw a scrim as a
rectangle; fade single-draw content and short entrances with `CompositingStrategy.ModulateAlpha`; use
`CompositingStrategy.Offscreen` only for small masks.

**Why.** With the default compositing, a layer with alpha below 1 and overlapping content is rendered
into an offscreen buffer and then composited, on every frame of the fade. Full-screen, that is a second
full-screen pass per frame. `ModulateAlpha` applies the alpha to each draw instead, with no buffer (an
offscreen fade would also clip an item's shadow square). A small layer that rests below 1, rather than
fading, is rendered offscreen in every frame the app draws: a 132 × 132 badge resting at 0.85 did so
through every measured motion and caused most of a traced journey's waits for a free buffer (106 for
332 ms, against 34 for 115 ms with `ModulateAlpha`). Give a resting alpha `ModulateAlpha` too: its
overlapping draws then show through each other a little, which at an alpha that high reads the same.

```kotlin
// A scrim is one rectangle; its alpha is read while drawing.
Box(Modifier.fillMaxSize().drawBehind { drawRect(scrim, alpha = shown.value) })

// An entrance fade of one card: per-draw alpha, no offscreen buffer.
AppCard(modifier = Modifier.graphicsLayer {
    alpha = shown.value
    compositingStrategy = CompositingStrategy.ModulateAlpha
}) { … }
```

**Don't.** `Modifier.fillMaxSize().alpha(a)`, or `graphicsLayer { alpha = a }` on a full-screen chain;
`AnimatedContent` or `Crossfade` between two full screens in a feature or a component.

**Exceptions.** None in screens or components. The shell has two, each bounded and each in
`:core:navigation`: the back card's texture, rendered once per gesture
([The back gesture](#the-back-gesture)), and `HostCrossfade`, the 200 ms fade between whole hosts when
the session changes ([How the shell moves screens](#how-the-shell-moves-screens)).

**Enforced by.** `layer-alpha-fill` (features and the design system; `:core:navigation` is outside it).

### Nothing animates while idle

**Rule.** No animation runs forever on its own. Something that loops (a spinner, a shimmer, a caret,
ambient decoration):

- pauses while its screen is hidden: read `LocalTabVisible.current.isVisible` in an effect or a draw
  block, never in composition;
- draws in a `graphicsLayer()` of its own, so each change redraws only itself; where the motion can
  step (a spinner, a row of dots), it steps on a timer (`delay(step)`) rather than asking for every
  vsync with `withFrameNanos` or `withFrameMillis`; where it must glide (the shimmer's band), it runs
  per frame only for the glide and draws nothing between (`ShimmerClock`: a 1200 ms sweep on a phone,
  then 800 ms with no frames);
- stops or slows under reduce motion (`AppTheme.reduceMotion`): decoration stops; an indicator whose
  motion is the message slows instead (`AppActivityIndicator` and the pull-to-refresh spinner step at
  half pace);
- if it is ambient decoration (a glow, an orb, a drifting gradient), moves only layer properties, builds
  its brushes in `drawWithCache`, votes `FrameRateCategory.Normal`, and winds down a few seconds after
  its screen shows.

`AppActivityIndicator` and the skeleton shimmer already stop when their screen is hidden. The
pull-to-refresh spinner, which turns only while a refresh runs, does not yet.

**Why.** A loop keeps the display at its full rate, the GPU busy and the battery draining on a screen
nobody touches, and it is redrawn under every frame of a sheet dragged over it. An app whose ambient orb
ignored this drew 606-632 frames in 5 idle seconds. A loop around `withFrameMillis` asks for a frame on
every vsync, 120 a second, for as long as the screen is shown; a timer step draws one frame per step.

```kotlin
@Composable
internal fun SyncDots(modifier: Modifier = Modifier) {
    var lit by remember { mutableIntStateOf(0) }
    val visibility = LocalTabVisible.current
    // Slower, not still, under reduce motion: these dots say that work is running.
    val step = if (AppTheme.reduceMotion) DotStepMillis * 2 else DotStepMillis
    LaunchedEffect(visibility, step) {
        // The holder is read in the effect: hiding the tab stops the loop and recomposes nothing.
        snapshotFlow { visibility.isVisible }.collectLatest { visible ->
            while (visible) {
                delay(step)                  // one frame per step, not one per vsync
                lit = (lit + 1) % DotCount
            }
        }
    }
    val dot = AppTheme.colors.contentSecondary
    // Its own layer: a step re-records these dots and nothing around them. `lit` is read while drawing.
    Canvas(modifier.size(AppTheme.spacing.xl, AppTheme.spacing.sm).graphicsLayer()) {
        val radius = size.height / 2f
        val gap = (size.width - 2 * radius) / (DotCount - 1)
        repeat(DotCount) { i ->
            drawCircle(dot, radius, Offset(radius + i * gap, radius), alpha = if (i == lit) 1f else 0.3f)
        }
    }
}

private const val DotCount = 3
private const val DotStepMillis = 250L
```

Decoration with no message also has a time bound and does not run under reduce motion:

```kotlin
// `visibility` as in SyncDots. `glow` is an Animatable(0f) read only inside the decoration's
// graphicsLayer { } block, on a modifier that also sets preferredFrameRate(FrameRateCategory.Normal).
val reduceMotion = AppTheme.reduceMotion
val springs = AppTheme.springs
LaunchedEffect(visibility, reduceMotion) {
    if (reduceMotion) return@LaunchedEffect               // decoration: still under reduce motion
    snapshotFlow { visibility.isVisible }.collectLatest { visible ->
        if (!visible) return@collectLatest                // hidden: cancelled, nothing draws
        withTimeoutOrNull(AmbientMillis) {                // a few seconds of motion
            while (true) {
                glow.animateTo(1f, springs.emphasis.unit)
                glow.animateTo(0f, springs.emphasis.unit)
            }
        }
        glow.animateTo(0f, springs.layout.unit)           // settle, then still until it shows again
    }
}
```

**Protects.** Gate `idle.frames = 0` (5 s on every tab).

**Enforced by.** the idle gate; *review*.

### Ask for 120 Hz only while layer-only motion moves

**Rule.** Motion that changes only `graphicsLayer` properties sets
`Modifier.preferredFrameRate(FrameRateCategory.High)` while it moves and back to `Default` when it stops.
Never pass a fixed rate, and never leave the vote on.

**Why.** On a phone with an adaptive refresh rate, content defaults to a lower rate unless something
votes. A node whose placement changes already votes High; a layer-only animation casts no vote and may
run at 60 Hz. A permanent vote passes down to every child layer and would hold 120 Hz for a spinner that
steps eight times a second.

**Ambient decoration votes `FrameRateCategory.Normal`, never High.** A glow or an orb does not follow a
finger, so it does not need the display's top rate; a High vote would hold 120 Hz for as long as it
moves.

**Enforced by.** *review*.

### Per-frame shapes and allocation

- A shape clipped or filled on every frame of a motion is a rounded rectangle, never a path of cubics:
  Skia clips a rounded rectangle analytically and masks a general path on every frame. On Android, add it
  to the platform path by coordinates (`asAndroidPath().addRoundRect(l, t, r, b, rx, ry, CW)`); Compose's
  `addRoundRect(RoundRect(…))` allocated a geometry object up to three times a frame.
- Nothing allocates in code that runs every frame: build brushes, paths and shapes in `drawWithCache`,
  reuse a rewound `Path`, keep one `Paint`. No iterator, `filter` or `maxBy` in per-frame functions, and
  no `() -> Float` lambda called per frame in a hot loop (it boxes). Allocation per frame shows as
  garbage collections that compete with the frame for the same big cores.
- A shadow never follows a size that animates, and nothing fades through a shadow.

**Enforced by.** *review*.

### Draw one-pixel separators as filled rects

**Rule.** A separator one physical pixel thick is `Modifier.hairline(edge, startInset)` on the element
it separates or, inside the design system's own draw blocks, `drawHairline(color, start, end, top)`:
both fill that row of pixels as a rect. Never draw it with `drawLine` and `strokeWidth = 1f`.

**Why.** The GPU renderer draws a stroked line at far greater cost than a fill of the same pixels, and a
separator is drawn again in every frame its screen moves or scrolls. On the reference phone the line
under a pushed screen's top bar alone kept a push's opening frames (the third to the seventh) at about
10 ms of GPU each, over the 8.33 ms a frame has; filled, they took about 7.8 ms.

**Protects.** `pushpop.hitch_ms_per_s` at most 5.0 ms/s: every separator drawn as a fill took push and
pop with the Back key from 8.1 ms/s of hitches to 0, and the back gesture's from 8.5-11.5 to 1.4-4.3.

**Enforced by.** `pixel-line` (features, `:core:ui`, the design system, `:core:navigation` and `:app`).
<!-- <opt:liquidglass> -->

---

## Liquid glass tiers

The glass foundation in `core/designsystem/…/foundation/glass` provides `GlassStage`,
`Modifier.glassSurface` and the tier policy (`GlassQuality`, `rememberGlassQuality()`,
`LocalGlassQuality`). Glass that samples the scene behind it is the most expensive thing the template can
draw, so its tier is a measured policy, not a device check.

**The shell does not draw glass chrome yet.** `ShellChrome` is a plain box. Nothing in the app calls
`GlassStage` or `rememberGlassQuality()`, and nothing provides `LocalGlassQuality`, so anything that reads
it gets its default, `Frosted`. The rules below are the foundation's API rules: they apply as soon as
something puts a `GlassStage` on screen.

**Who samples.** Only floating chrome in a `GlassStage`'s `chrome` slot samples the scene the stage
records from its `content`. Everything inside a screen (cards, chips, fields, sheets, top-bar buttons) is
`Modifier.glassSurface`, which samples nothing. A sampling surface inside the recorded content would
sample a recording of itself, which crashes the RenderThread; the scene fails a `check` instead, naming
the fix. In a tier that does not sample (`Frosted`, `Solid`) the stage records nothing.

| Tier | When | Floating chrome |
|---|---|---|
| `Live` | API 33 and later, a healthy device | blur, vibrancy, the lens |
| `LiveNoLens` | API 31-32, or a device warming up | blur and vibrancy |
| `Frosted` | below API 31, low RAM, battery saver, heat, a GPU that fell behind | drawn glass, samples nothing |
| `Solid` | the person asked to reduce transparency | the style's solid bar |

`rememberGlassQuality()` decides. The person's choices (battery saver, reduce transparency) apply both
ways at once. Measured signals only ever lower the tier for the rest of the session: the hottest thermal
status seen so far, and a GPU verdict (two consecutive windows of 120 frames in which more than 15 % of
frames took the GPU longer than one refresh period while the scene was sampled), kept per system build
and install until either changes.

**Rules.**

- Never put alpha below 1, `AnimatedVisibility` or a `saveLayer` with alpha around a sampling surface:
  it renders the whole chrome offscreen every frame. Hide chrome by translation, set alpha 0 only on the
  frame it is fully hidden, and keep it composed (hiding by removal rebuilds its backdrop layer and
  shaders in the first frame it comes back).
- Glass appears by refraction or tint, never by layer alpha over a backdrop.
- Never read state inside the backdrop's `effects` (Kyant rebuilds the effect chain on every change);
  remember the `drawBackdrop` chain once; pass `highlight = null` and `shadow = null`.
- `glassSurface` under a fading ancestor: fade the ancestor with `CompositingStrategy.ModulateAlpha`, or
  the offscreen layer cuts the outer shadow off square.
- Nothing inside the recorded scene animates forever: it re-blurs every sample on every frame.
- No `RenderEffect` blur over a full screen or behind a modal sheet: it renders offscreen on every frame
  the content or the sheet moves.

**Lessons from glass chrome built outside this template** (they apply when the shell's bar samples the
scene):

- Kyant records a sampling surface's sample again, and blurs it again, on every frame in which the
  surface's position on screen changes, through a layer on any ancestor too (a slide, a card-stack
  scale). Never track a glass surface's coordinates in state.
- A tap on a glass tab should move the lens and press nothing: a bar that "pressed" on tap re-rendered
  its blur, lens, highlight and shadow for about half a second.

**Measuring.** Measure glass both with the tier fixed (provide a fixed `LocalGlassQuality` in the test
or benchmark harness) and with the policy deciding: a run left to the policy can frost itself partway
through and pass by removing the glass.

**Enforced by.** the scene's run-time check (a sampling surface inside its own scene fails it);
*review* for the rest.
<!-- </opt:liquidglass> -->

---

## Keyboard

**Rule.** Read the keyboard in layout or placement (`Modifier.imePadding()`, `WindowInsetsRulers.Ime`),
never `WindowInsets.ime` in composition. A screen counts the keyboard once: while it hides the tab bar
the shell reports it as bottom chrome, which `chromeContentPadding()` already includes, so a list padded
that way needs no `imePadding()` of its own, and `AppScaffold` takes none. Inside a sheet, the sheet keeps
its content and footer above the keyboard.

**Why.** The keyboard animates every frame for about 285 ms each way. Read in composition, every frame
recomposes; padded twice, content jumps. A sheet's IME padding re-measures its content on every keyboard
frame.

**Known cost.** A full-screen destination with `AppBottomActionBar` measures the bar, the scaffold and
the list again on every keyboard frame (nothing recomposes).

**Enforced by.** `sheet-ime-padding`; *review*.

---

## Start-up, baseline profiles and APK size

### Start-up

#### Nothing on the start-up path the first frame does not need

**Rule.** Keep `Application.onCreate` and the first screen free of I/O and eager singletons. Start work
the first frame does not need after the first frame, in a message of its own.

**Measured on the reference phone.**

- The Play Store update check, started inside the cold start, woke the Play Store process for
  18.6-27.2 ms of CPU in the middle of it. In a build that started it after the first content frame, in
  a message of its own, the cold start median fell from 403 to 365.5 ms and `performResume` from 14.2 to
  5.3 ms.
  <!-- <opt:playstore> -->
  `PlayStorePrompts` works that way: both of Play's checks start after the first frame, in a main-thread
  message of their own, and only while the activity is resumed.
  <!-- </opt:playstore> -->
- In a build with Firebase, Firebase's eager components took 63 ms of `bindApplication` (Crashlytics
  41 ms of it).
- A launch hook that read the session token to see whether one existed decrypted it through the Keystore
  on the main thread: five binder calls, 8-13 ms of every start.

#### Decide signed in or out without unsealing tokens

**Rule.** The launch decides between the signed-in and the signed-out app from
`AuthTokenStore.isAuthenticated` (whether a session is stored), never by reading the tokens. Anything
that seals or unseals runs on the io dispatcher inside the store.

**Why.** Unsealing is binder calls into the Keystore, and the first frame waits for the decision.

#### Background work starts when there is work

<!-- <opt:offlinequeue> -->
- WorkManager never initialises itself: the manifest removes its start-up initializer and the
  Application supplies its configuration, so it starts the first time the offline queue asks for it.
<!-- </opt:offlinequeue> -->
- Two things this template leaves out to keep start-up light: a Glance home-screen widget (its runtime
  started WorkManager and a database at every launch to show one line of text) and Lottie (it pulled in
  appcompat, about 190 KB of code, for one decorative animation). Add either only with that cost in mind.
- One Preferences DataStore file per concern: inject `DataStoreFiles` (a `@Singleton`) and call
  `preferences("name")` once per file, from a `@Singleton` provider, because two instances of one file
  corrupt it. Every write rewrites the whole file, so on a shared file a search would wait behind a
  token refresh.
- No animated logo splash: the system splash covers launch, and an animation delays first content
  against the cold-start gate.

#### Fonts

**Rule.** Never add `<meta-data android:name="preloaded_fonts">` to a Compose app.

**Why.** The manifest entry makes the framework fetch each listed font synchronously on the main thread
before `Application.onCreate`, up to 500 ms per font, into a cache Compose's `GoogleFont` never reads.

**How the theme picks its fonts.** `FontPreload` is the mechanism for downloaded or bundled families:
`AppFontPreload.run(resolver)` fetches them within a 150 ms budget (`PreloadBudget`), and `AppTheme` draws
with them only if they arrived in time. The answer is fixed the first time the theme asks
(`AppFontPreload.fonts`), so text never reflows mid-process: a family that swaps in later would reflow
every line on screen, possibly mid-transition. The resolver reports its own timeout as an
`IllegalStateException`; `FontPreload` catches it and then checks `ensureActive()`, so a real cancellation
still propagates.

**The app's start-up never calls `AppFontPreload.run`**, so the app draws the platform families in every
process, whatever families `AppFontPreload` names.
<!-- <opt:catalog> -->
Only the catalog runs the preload today (`CatalogActivity`).
<!-- </opt:catalog> -->
To draw downloaded fonts in the app, call `AppFontPreload.run(createFontFamilyResolver(context))` while
the splash is up, before the first composition of `AppTheme`; a family that misses the budget then
applies from the next cold start, once it is cached on the device.

#### The theme never recreates the activity

`MainActivity` handles `uiMode` and sets the app's night mode from the theme setting
(`UiModeManager.setApplicationNightMode`), so the next cold start's splash is already in the app's theme
and a dark start never flashes light. `configChanges` covers size, layout, orientation, keyboard,
navigation and `uiMode`: a resize, a fold, a rotation or a theme change is laid out again in place, never
a rebuild of every tab. Never lock orientation or resizability.

**Enforced by.** `verifyManifestRules` (the size, layout, orientation, keyboard and navigation changes,
and no orientation or aspect-ratio lock; `uiMode` is in the manifest but not checked); lint
`LockedOrientationActivity`, `NonResizeableActivity`.

**Start-up protects.** Gates `coldstart.median_ms` at most 380 ms (speed-profile) and
`coldstart_installed.median_ms` at most 400 ms (the profile from the APK only).

### Baseline profiles

<!-- <opt:performance> -->
**Rule.** After a journey changes, regenerate the profile and split it into the modules:

```bash
./gradlew :app:generateBaselineProfile        # a connected phone using gesture navigation
python3 tools/perf/split_profile.py
grep -c '^H' core/navigation/src/main/baseline-prof.txt   # count the hot methods
```

- Record on a phone with gesture navigation: under button navigation an edge swipe is a plain drag and
  the back-card journeys cannot run.
- `speed-profile` compiles only methods marked hot (`H`), and how many the recording marks hot depends on
  the device: the same journeys marked 248 shell methods hot on an API 35 emulator and 9 on the reference
  phone's API 37. When a recording marks far fewer hot methods, merge it with the previous one (flags
  OR-ed per method) rather than ship the poorer one.
- Check the generated file is not empty before splitting: an install that fails (another machine's debug
  key, `INSTALL_FAILED_UPDATE_INCOMPATIBLE`) still reports `BUILD SUCCESSFUL` with no rules.
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ **Rule.** Keep the checked-in profiles (`src/main/baseline-prof.txt` in `app`, `core/navigation`, -->
<!--~ `core/designsystem` and `core/ui`) and ship them. They were recorded on the template's own journeys; this -->
<!--~ project has no benchmark module to record new ones. -->
<!--~ -->
<!-- </opt-else:performance> -->
- The hand-written wildcard lines at the top of `core/navigation` and `core/designsystem`
  (`HSPL<package>/**->**(**)**`) stay: they cover code no journey reaches. A module whose code must all
  be compiled says so the same way.
- A profile whose package was not renamed with the project matches no method: after renaming the
  package, check the profiles name it.

**Why.** Without a profile, the first use of every shell and screen method is interpreted or
JIT-compiled in the frame that needs it. `adb shell cmd package compile -f -m speed-profile
<applicationId>` compiles an installed profile now instead of at the next idle maintenance.

### First use of shaders

Skia compiles a GPU program the first time a combination is drawn (a scaled, round-clipped card, a
layer for the stretch), on the RenderThread, 10-70 ms each, and keeps it in HWUI's disk cache until
`pm clear` or a reinstall. The shell's idle warm-up draws unopened tabs once out of sight.
<!-- <opt:performance> -->
The perf harness's warm-up draws everything a gated scenario draws (one back swipe included) and
reports first uses as rows of their own (`first_push`, `first_gesture`, and their `_warm` variants).
<!-- </opt:performance> -->

### APK size and build weight

- Release builds run R8 with resource shrinking; `localeFilters` keeps only the languages the app ships;
  the APK carries no `LICENSE` files and no `.kotlin_builtins`.
- Per-ABI APKs are built only with `-PabiSplits=true`.
- Never add `-assumenosideeffects` for Compose's `traceEventStart`: the recomposition tools and
  `runtime-tracing` read those markers. `runtime-tracing` is a `devImplementation` dependency, so a
  Perfetto trace of a dev build names the composables in a slow frame.
- Flavours are only on the application module: a library with four flavours would build eight variants
  instead of two. Environment reaches libraries as configuration through Hilt; no library reads a
  `BuildConfig`.
- `./gradlew build` runs R8 for every release-like variant at once: on a 3 GB Gradle heap four R8 tasks
  ran out of memory (this project's `gradle.properties` asks for 4 GB). On a machine with less memory,
  build the variant you need, or run `build` with `--max-workers=2`. Set the heap in `gradle.properties`,
  never with `-Dorg.gradle.jvmargs` on the command line, which starts a second daemon.

---

## Memory

### What the PSS row measures

**Rule.** Judge memory by the app's own PSS: the total less Graphics. On the reference phone Graphics is
the window's buffers (EGL mtrack, 51-55 MB on every build of every app at 1080 × 2400 and 120 Hz), so a
70 MB total cannot be met by any app; the total is reported only. Compare PSS only between runs of the
same tools on the same day.

**Protects.** Gate `meminfo.pss_app_mb` at most 58.4 MB (everything preset) and 53.3 MB (standard),
non-regression.

### Never read PSS while animating

**Rule.** Never call `Debug.getMemoryInfo` (or run `dumpsys meminfo`) inside a measured journey or while
the app animates; sample PSS only in a pause.
<!-- <opt:performance> -->
`perf.sh meminfo` is a scenario of its own, after the journey.
<!-- </opt:performance> -->

**Why.** A PSS read walks every memory mapping of the process under the kernel's lock on its address
space; during a scroll it costs about one janky frame.

### Where native memory goes

After a journey, the native heap (about 19 MB sampled) is mostly text: Skia's glyph strike cache (a
variable font gives every weight its own strike), FreeType's glyph loaders, glyph atlas uploads, then the
GPU driver's compiled programs. Nothing grows per transition. Every weight and size in the type scale
costs strikes, so fewer of them is the lever; HWUI already purges these caches on `onTrimMemory`.

A back swipe keeps about 1 MB of native memory the Back key does not (about 4 MB on the standard preset,
levelling off after a few swipes): glyphs drawn at the card's scale and the programs for its clip and
scrim. While the card is up its texture takes about 10 MB of GPU memory, released when it goes.

---

## How to measure

### Builds that mean something

<!-- <opt:performance> -->
- Measure `devBenchmarkRelease` (with the benchmark module) or `devRelease`: R8, the baseline profile
  compiled with `speed-profile`, not debuggable.
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ - Measure `devRelease`: R8, the baseline profile compiled with `speed-profile`, not debuggable. -->
<!-- </opt-else:performance> -->
- Never quote a debug build: it runs without R8 and without a profile, with most of its code
  interpreted, so it drops several times more frames than the same code in a release.
- On a real phone, unlocked, with the screen on. The reference numbers are the Redmi's; an emulator's
  numbers are relative evidence, never a gate.
<!-- <opt:perfhud> -->
- With the overlay hidden. Your choice of Live stats is kept across launches and across reinstalls that
  keep the app's data, so launch a measurement with `--es perf.hud off` or `adb shell pm clear` the app
  first.
<!-- </opt:perfhud> -->
<!-- <opt:devtools> -->
- The measured builds are dev-flavour builds, so they carry the dev tooling: the DEV badge and its
  inspector. `verify<Variant>CarriesNoDevTooling` guards the shippable variants (staging, prod and
  playstore releases); prod and playstore releases hold none of that code.
<!-- </opt:devtools> -->
<!-- <opt:perfhud> -->

### The performance overlay

Dev and debug builds show frames per second and the janky share under the DEV badge, and a Perf tab with
frame times, a per-screen janky table, memory, start-up, StrictMode and an optimisation score. Use it
while working to see whether a screen still draws on time; take numbers that decide anything from a
measured release build with the overlay hidden. It is built not to change what it measures (no listener
while off, nothing per frame on the main thread but a clock check, redrawn only inside frames the app
draws, PSS only in a pause), and shown it may cost at most 0.3 percentage points of janky frames and
0.5 ms per frame (measured: 0.02-0.06 ms). Everything else is in
[PERFORMANCE-OVERLAY.md](PERFORMANCE-OVERLAY.md).
<!-- </opt:perfhud> -->
<!-- <opt:performance> -->

### The perf harness

`tools/perf/perf.sh` installs the APK, clears the app, installs and compiles its baseline profile as Play
does, launches with the dev launch options, visits every tab once, then runs the scenarios you name, three
runs by default. The median of the runs is what it reports and what `--gate` checks.

```bash
./gradlew :app:assembleDevBenchmarkRelease
tools/perf/perf.sh --package <applicationId> --variant devBenchmarkRelease --gate solid \
    tabs tabs_tap pushpop pushpop_gesture back_during_push back_card_held rapid_back \
    scroll_root scroll_minimize idle nav_stress restore meminfo recompose coldstart coldstart_installed
```

| Scenario | What it measures |
|---|---|
| `tabs`, `tabs_tap` | switches between visited tabs, timed from the finger's lift; `tabs_tap` with plain taps |
| `pushpop`, `pushpop_gesture` | open a row and go back with the Back key, or with the back gesture |
| `back_during_push`, `back_card_held` | a back swipe during a push, and one held over a pushed screen: one opaque card, no double exposure |
| `rapid_back` | Back pressed repeatedly during transitions |
| `scroll_root`, `scroll_minimize` | flings on a root list, with and without the bar minimizing |
| `idle` | 5 s on every tab, frames rendered |
| `nav_stress`, `restore` | random navigation for a minute; process death and restore |
| `meminfo` | PSS after the journey |
| `recompose` | frames that recompose per transition, from a trace; never inside the timing runs |
| `coldstart`, `coldstart_installed` | six cold starts, compiled with the profile, or with the APK's profile only |
| `tabs_large`, `pushpop_large`, `listdetail_select`, `scroll_large`, `sidebar_toggle` | the large-window layouts, on a window of Expanded width or more |

Options: `--runs N`, `--out DIR`, `--extra key=value` (a launch extra), `--compile speed` (every method
compiled ahead of time: what a perfect profile would give, to see what is still interpreted), `--baseline
summary.json` with `--gate smoke`. The harness refuses a locked phone and waits for the battery to cool
below 37 °C before a run. It launches with `--es perf.hud off` unless you pass `--extra perf.hud=on`.

`--gate solid` checks the run against the limits in `gates.toml`; it does not compare with an earlier
build. To compare a change with the build before it, measure both on the same day, then either pass the
earlier run's `summary.json` as `--baseline` with `--gate smoke` (it checks the rows the `smoke` column
gives a tolerance: tab-switch and push/pop janky and first frames, scroll janky and GPU p90, idle
frames), or read the two `summary.md` files side by side for the rest. Two runs already on disk are
compared without measuring again:
`python3 tools/perf/check_gates.py --gate smoke --baseline <earlier>/summary.json <later>/summary.json`.

`tools/perf/record.sh <applicationId> <journey> <out dir>` screen-records one journey at 720 × 1600 and
writes every frame and a strip of the moving frames: attach it to any change to motion.

### The gates

`tools/perf/gates.toml` has one column per preset (`solid`: the everything preset with solid chrome,
`standard`, `lean`), a `smoke` column (a tolerance against a reference run), and a `large` column for the
large-window scenarios: a tablet, or the phone with its display overridden for the run and restored
after.

- A limit is a number (the median must be at or under it), `true` (a check that must pass in every run)
  or `"ratchet"` (reported, not gated; when a row is first measured, its value plus 10 % goes there).
- A smoke tolerance is the reference value plus `max(add, reference × scale)`.
- Rows marked `[non-regression]` hold the worst acceptance run plus its noise margin. They may only go
  down, and every improvement is written back.
- `--gate <column>` fails on any failing row, including a row of that column the run did not measure.

| What | Limit (`solid`) |
|---|---|
| **Release limits: what users feel** | |
| Tab switch, push/pop, back gesture, Rapid Back: hitch time per second of motion | at most 5.0 ms/s |
| Janky frames other than each motion's first: tabs, push/pop / back gesture, Rapid Back | at most 0.5 % / 1.0 % |
| Motions whose first frame missed its deadline (`*.late_first_frames`) | reported |
| **Checks** | |
| Back swipe during a push; back card held: one opaque card, lands on the list | true |
| Rapid Back and navigation stress: alive, expected screen, stack invariants; process death restored | true |
| **Scroll, idle, start-up** | |
| Scroll a root list, and with the bar minimizing: janky / GPU p90 | at most 0.5 % / 7.0 ms |
| Idle 5 s on every tab | 0 frames |
| Cold start, speed-profile / as installed | at most 380 / 400 ms |
| Navigation stress, janky | at most 4.0 % |
| **Recomposition** | |
| Frames that recompose: tab switch / pop / back-card progress | at most 2 / 2 / 0; push reported |
| **Non-regression** | |
| Totals, first frames and p99 per transition | see [What transitions protect](#what-transitions-protect) |
| App PSS (total less graphics) | at most 58.4 MB (standard 53.3) |
| **Reported only** | first visit to a tab, first push, first back swipe (cold and warm), total PSS |
| **Large window** | tabs janky 1.0 % (first frame median 8.0, max 16.0 ms); list-detail select 1.0 % (12.0 / 20.0 ms); open and close a detail 2.0 % (pop 8.0 / 16.0 ms); scroll 0.5 % and GPU p90 7.0 ms; sidebar toggle 0.5 % |
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ -->
<!--~ ### Without the perf harness -->
<!--~ -->
<!--~ This project was generated without the template's perf harness (its scripts that measure every journey -->
<!--~ on a phone and check the limits, the benchmark module and the baseline profile generator: the -->
<!--~ `performance` feature). Measure a release build on a real phone: -->
<!--~ -->
<!--~ - `adb shell dumpsys gfxinfo <applicationId> reset`, run the journey, then -->
<!--~   `adb shell dumpsys gfxinfo <applicationId>` gives the janky share and the frame-time percentiles. -->
<!--~ - A Perfetto trace through the `android-profiler` skill shows which thread lost the time in a slow frame. -->
<!--~ - Compare a change with the build before it on the same day, the same phone and the same journey. -->
<!--~ -->
<!--~ The limits this guide quotes as gates are the template's own, measured on the reference phone. -->
<!-- </opt-else:performance> -->

### Reading a red row

Start from what users see, then find the frame and the thread.

1. **Hitch or first frame?** A transition's janky share that is about one frame per transition is the
   first frame from idle. Read the late first frames and the hitch ratio before anything else: if the
   hitch ratio and the janky frames after each motion's first are within their limits, the motion is
   smooth and the late first frame is the phone's idle clocks. Do not chase it by drawing less: on the
   reference phone it does not move.
2. **How the hitch ratio is computed.** From the display's own present times: for each gap between two
   presented frames up to 12 refresh intervals long, the gap counts as motion; a gap over 1.5 intervals is
   a hitch costing the time past one interval. Hitch milliseconds per second of motion is the ratio
   (Apple's scale: under 5 good, 5-10 a warning, 10 or more critical). `gfxinfo`'s janky share counts
   frames that missed HWUI's deadline, including ones the display never showed late.
3. **Janky after the first frame: something costs every frame of the motion.** In this order:
   recomposition on every frame (a recomposition test for the journey), alpha on a full-screen layer,
   allocation in draw code, a layer recorded again every frame, work started by the navigation itself on
   the main thread.
4. **GPU p90 high: too many pixels.** Turn on `debug.hwui.overdraw show` and
   `debug.hwui.show_layers_updates true`, look for a second full-screen fill, an offscreen layer larger
   than a card, a blur; set both back to `false` after. GPU time on a phone whose GPU clock follows load
   is work divided by frequency: compare like with like.
5. **A first use slow (the first push, the first back swipe):** a GPU program compiled on first draw. Find
   it in a Perfetto trace (`shader_compile` under an op) and either avoid the combination or make sure a
   warm-up draws it.
6. **Frames while idle:** something animates forever; find it with frame times while touching nothing.
7. **Memory high:** rerun beside the reference build on the same day; look at the native heap first.
8. **Cold start slow:** take only untraced runs as totals (a traced start overstates every app), then a
   trace for where the time goes.
9. **Not the app.** Device-side screenshots and videos go to `/data/local/tmp`, never `/sdcard`: on the
   reference phone the media scanner woke Google Photos, which ran up to 3.7 cores and GPU work inside the
   captures (33-37 ms GPU frames). Read the phone's night mode and rotation before a check that depends
   on them and give them back after. A scripted `input tap` has no gap between down and up, so nothing
   that starts on press-down can show in it.

A Perfetto trace from the `android-profiler` skill, on a release build, names the cause; the main-thread
`Choreographer#doFrame`, the RenderThread's `DrawFrame` and the GPU completion of the late frame say which
of the three lost the time.

---

## Before you merge UI work

1. The `jetpack-compose-performance` audit has run over every composable you touched, all seven
   categories, with the category's reference page read before each fix.
2. The changed journey composes nothing in its middle frames: `assertQuietBetweenEnds()` passes for it.
   <!-- <opt:performance> -->
   On the phone, `perf.sh recompose` is within its rows.
   <!-- </opt:performance> -->
3. No full-screen layer has alpha below 1, and no screen adds a second full-screen fill.
4. Every spring comes from `AppTheme.springs`, and a motion a finger started is released with the
   finger's velocity.
5. Every new lazy list has keys, content types where kinds mix, its padding inside the scroll
   (`chromeContentPadding()` on a screen, the sheet's `contentPadding` in an `AppSheet`) and the design
   system's fling.
6. A new destination's first frame shows its content when the data is in memory, and its secondary
   sections wait for `LocalNavigationSettled`.
7. Nothing new animates while idle or while its screen is hidden.
<!-- <opt:performance> -->
8. On a release build, `perf.sh --gate solid` fails on no limit but those
   [Where the template stands](#where-the-template-stands) lists as over, none of those is worse than it
   lists there, and against the previous build measured the same day,
   `--baseline <previous run>/summary.json --gate smoke` passes and no other row of the two `summary.md`
   files got worse.
9. Any new motion has a recording from `record.sh`.
<!-- </opt:performance> -->
<!-- <opt-else:performance> -->
<!--~ 8. Measured on a release build against the previous build, the same day on the same phone, no journey -->
<!--~    you touched got worse. -->
<!-- </opt-else:performance> -->
