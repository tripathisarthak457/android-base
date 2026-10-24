# Performance

How an app built on this project stays light and smooth on a budget phone at 120 Hz: what to do, what not to do, why,
and the number each rule protects. The reference phone is a Redmi Note 10 Pro class budget phone (1080 × 2400,
120 Hz), so a frame has 8.33 ms. If a screen is smooth there, it is smooth on most phones.

Read [the rules that matter most](#the-rules-that-matter-most) before your first screen. The sections after it go area
by area; [Measuring](#measuring) says how to prove a change, and [Before you merge UI work](#before-you-merge-ui-work)
is the checklist.
<!-- <opt:perfhud> -->
The live performance overlay has its own page, [PERFORMANCE-OVERLAY.md](PERFORMANCE-OVERLAY.md).
<!-- </opt:perfhud> -->

Every rule says what enforces it: a `verifyCodeRules` rule id (it fails the build; a justified exception is
`// allow(<rule-id>): <reason>` on the line above), a Compose lint id (an error in every Compose module), a build check,
a test, or *review*, which means nothing checks it automatically and the `jetpack-compose-performance` audit and the
reviewer must. Numbers come from release builds compiled with their baseline profile on the reference phone; each says
how it was measured, so you can measure it again.
<!-- <opt:!ui_components> -->

This project has no UI components yet. The principles below are the ones the template's own components, shell and
lists were measured against; where a rule names a design-system API, that API is one you build (the
[component contract](DESIGN-SYSTEM.md#the-component-contract) says how), and the numbers are the template's.
<!-- </opt:!ui_components> -->

---

## The frame budget

- **8.33 ms per frame at 120 Hz.** Main-thread time (input, animation, composition, layout, recording the display
  list) is graded against each frame's own refresh interval. The RenderThread then turns the display list into GPU
  commands, and the GPU draws; a frame's whole pipeline may run past one interval and still make its deadline.
- **Janky** is what Android's FrameTimeline counts: a frame presented after its deadline. At 120 Hz that is any frame
  past 8.33 ms of work. `dumpsys gfxinfo` and Perfetto both report it.
- **The first frame after idle usually misses, whatever the app draws.** The GPU is still clocked down: 9-16 ms of GPU
  for a full-screen frame from idle against 4-6 ms a few frames later, and a build with every screen's content
  removed still missed that deadline on 7 of 7 tab switches from idle. It shows as the motion starting one vsync
  late, never as a stutter inside it, so the release limits judge transitions by the hitch time per second of motion
  and by the janky frames after each motion's first, and only report the late first frames.
- **Limits are ceilings, not goals.** The goal is about 0 % janky frames in tab switches, push and pop, Back and
  scroll, and the lighter of two equally smooth designs.

---

## The rules that matter most

The part after the dash is what enforces each rule.

1. Judge smoothness only on a release build with its baseline profile, on a real phone, never a debug build. —
   *review* ([Measuring](#measuring))
2. Read anything that moves (animated, dragged, scrolled) in `offset { }`, `layout { }`, `graphicsLayer { }`, a draw
   block or a `Modifier.Node`, never in composition; hold an animation as `val x = animate…()` and read `x.value`
   there. — `animated-delegate`, lint `FrequentlyChangingValue`, `UseOfNonLambdaOffsetOverload`
3. A composable that receives a value changing every frame takes `() -> T`, never `State<T>`. — `state-param`
4. Never put alpha below 1 on a full-screen layer, and never cross-fade two full screens. — `layer-alpha-fill`
5. A destination's first composition is final: seed it from memory, load secondary sections after the transition. —
   *review*
6. Every lazy list has a stable `key`, a `contentType` when kinds mix, its padding inside the scroll and a named fling.
   — `lazy-key`, `lazy-content-type`, `scroll-chrome`, `scroll-fling`
7. Every spring and fade comes from `AppTheme.springs`; a motion a finger released ends on a spring seeded with the
   finger's speed. — `raw-tween`; *review*
8. Nothing animates while idle or while its screen is hidden, and reduce motion stops or slows it. — *review*
<!-- <opt:performance&ui_components> -->
   and the gate `idle.frames = 0`
<!-- </opt:performance&ui_components> -->
9. Nothing on the start-up path the first frame does not need. — *review*
<!-- <opt:performance&ui_components> -->
   and the gates `coldstart.median_ms` (380 ms) and `coldstart_installed.median_ms` (400 ms)
<!-- </opt:performance&ui_components> -->
10. Custom modifiers are `Modifier.Node`; state is collected with `collectAsStateWithLifecycle()`; no parameter of a
    shared module is compared by identity. — `composed`, `collect-as-state`, `checkComposeStability`

---

## State reads per frame

Recomposition is fine on the first frame of a transition or after a tap. It is a bug on every frame of a scroll, an
animation or a transition. With strong skipping and lambda memoisation on (Kotlin 2.x), three things still cost
frames: new instances that are equal but not identical, state read too high or too early, and composing a whole
screen in the first frame of a navigation.

### Read moving values where they are used

**Rule.** Animated, dragged and scrolled values are read only inside `Modifier.offset { }`, `Modifier.layout { }`,
`Modifier.graphicsLayer { }`, a draw block or a `Modifier.Node`.

**Why.** A value read in composition invalidates the composition that read it, so every animation frame recomposes
that subtree, 120 times a second. Read in placement it re-places one node; read in a layer block it changes one
RenderNode property; neither composes anything.

```kotlin
@Composable
fun SavedMark(saved: Boolean, modifier: Modifier = Modifier) {
    val lift = AppTheme.spacing.md
    val color = AppTheme.colors.accent
    // `emphasis` is the theme's pop-in spring; its small overshoot is the pop.
    val shown = animateFloatAsState(if (saved) 1f else 0f, AppTheme.springs.emphasis.unit, label = "saved")
    Box(
        modifier
            .size(AppTheme.spacing.lg)
            // Read while placing and while drawing the layer: the animation composes nothing.
            .offset { IntOffset(0, ((1f - shown.value) * lift.toPx()).roundToInt()) }
            .graphicsLayer {
                alpha = shown.value.coerceIn(0f, 1f)
                // Alpha applied to each draw: no offscreen buffer for the fade.
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .drawBehind { drawCircle(color) },
    )
}
```

| Value | Where to read it |
|---|---|
| position, translation, parallax | `Modifier.offset { IntOffset(…) }`, `layout { … place(…) }` |
| scale, alpha, rotation, clip | `graphicsLayer { }`, the lambda form only |
| a size that animates | `layout { }` or a `LayoutModifierNode`, never `Modifier.width(animated)` |
| colour of text | `BasicText(color = ColorProducer { … })` |
| background, scrim, progress fill | `drawBehind`, `drawWithContent`, a `DrawModifierNode` |
| a threshold crossing (a title once scrolled) | `derivedStateOf`, or `snapshotFlow { }.distinctUntilChanged()` in an effect |

**Enforced by.** `animated-delegate`; lint `FrequentlyChangingValue`, `UseOfNonLambdaOffsetOverload`; recomposition
tests ([below](#prove-it-with-a-recomposition-test)).

### Providers, stable parameters, read-only state

- **A fast value is a provider.** A composable that receives a value changing often takes `() -> T` and reads it in its
  layout or draw. A draw block with no layer of its own records into the nearest layer above it, so give a progress
  bar `Modifier.graphicsLayer()` before its `drawBehind`, or every change re-records the screen's layer.
  — `state-param`
- **Parameters are stable.** Under strong skipping an unstable parameter is compared with `===`, so the composable
  reruns whenever its caller passes a new, equal instance. `checkComposeStability` reads the compiler's reports for
  `:core:designsystem`, `:core:ui` and `:core:navigation` and fails on such a parameter; fix it with a stable type, an
  entry in `config/compose-stability.conf` for a type that really is immutable, or a provider. An accepted exception
  goes in `config/compose-stability-baseline.txt` with a `# reason`. `List`, `Set` and `Map` are configured stable,
  so never `ImmutableList`, and never `@Immutable` on a model that carries a large list into a lazy layout.
  `./gradlew <task> -PcomposeReports` writes the reports for any module. — `checkComposeStability`
- **UI state is read-only and holds no per-frame values.** `UiState` holds `val`s and read-only collections built by
  the ViewModel; scroll offsets, drag positions and animation progress live in UI state holders read in layout or
  draw. Nothing in composition calls `.map { }` or `.filter { }` on state without `remember(keys)`: a list rebuilt
  in composition is a new instance every time and defeats skipping in every row that receives it. —
  `uistate-mutable`, `api-immutable`
- **Collect with the lifecycle.** `MviViewModel.observe` runs its flows only while a screen collects its state, so
  `collectAsStateWithLifecycle()` stops them for a screen that is stopped or hidden; `collectAsState()` would keep
  every kept screen's flows running and recomposing out of sight. — `collect-as-state`, lint
  `StateFlowValueCalledInComposition`, `LifecycleCurrentStateInComposition`
- **Custom modifiers are `Modifier.Node`.** `Modifier.composed` and `@Composable` modifier factories allocate and
  compose per call site, per recomposition and per list row; a node is created once and updated in place. A node reads
  composition locals with `currentValueOf` in measure, layout or draw, never in `onAttach`, where the value goes stale
  after a rotation. — `composed`, lint `UnnecessaryComposedModifier`, `ModifierFactoryReturnType`

### Write state before it is read, and keep static locals still

State a parent computes for its children is written in composition before the children read it, never from a
`SideEffect` read back in the same composition: a `SideEffect` runs after the composition that read the old value, so
the first frame is composed with defaults and composed again on the next. A `staticCompositionLocalOf` holds a value
that never changes at run time, or a stable holder whose fields are snapshot state; a new value recomposes the whole
subtree under the provider. Measured in the template's navigation: a static local that changed on a pop's first frame
recomposed the whole leaving screen, 416 composables, in that frame. — *review*

### Window size changes recompose only at a breakpoint

Lay out from `AppTheme.windowClass` and `AppTheme.containerClass`, which change only when a breakpoint is crossed, and
read the window's exact size from `LocalWindowMetrics` only inside a layout or draw lambda. The exact size changes on
every frame of a resize, a fold or a desktop window drag; read in composition, it recomposes everything under it on
every step. — `adaptive-screen-metrics`, `adaptive-orientation`, `adaptive-device-type`; lint
`ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`

### Callbacks, `derivedStateOf`, `snapshotFlow` and text

- Do not wrap callbacks in `remember`: the compiler memoises lambdas and bound references such as
  `viewModel::onEvent`. What breaks memoisation is capturing something rebuilt on every recomposition.
- Use `derivedStateOf` only when its input changes more often than its output, remembered and keyed on every
  parameter it captures. Read in composition it still costs a recompose pass in every frame its input changes: when
  the result only starts an effect, read the input in a `snapshotFlow` inside that effect instead (a title bar that
  read its collapse in composition went through 622 empty recompose passes in one traced scroll).
- `Column`, `Row` and `Box` are inline: state read in their content invalidates the nearest non-inline composable
  around them. Move a frequently changing read into a small composable of its own, or into a lambda.
- Leave line breaking and hyphenation unset (Compose then uses the cheapest, `LineBreak.Simple` and `Hyphens.None`);
  no `TextAutoSize` in rows or on screens that slide (it lays text out several times); bound text in rows with
  `maxLines`; build an `AnnotatedString` in the ViewModel or in `remember`.

**Enforced by.** lint `UnrememberedMutableState`, `UnrememberedAnimatable`, `AutoboxingStateCreation`,
`RememberReturnType`, `FlowOperatorInvokedInComposition`; *review* for the rest.

### Prove it with a recomposition test

A screen or component that animates gets a recomposition test for its journey. The JVM harness in
`:core:testing:compose` counts every composable that executes, frame by frame: `RecompositionCounter` is a test rule
chained outside the compose rule, `framesUntilIdle` steps the paused clock one frame at a time, and
`assertQuietBetweenEnds()` fails when any frame between the first (the change) and the last (the settle) executed a
composable.

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class SavedMarkRecompositionTest {

    private val counter = RecompositionCounter()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(counter).around(compose)

    @Test
    fun savingComposesNothingWhileTheMarkMoves() {
        var saved by mutableStateOf(false)
        compose.setContent { AppTheme { SavedMark(saved) } }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        counter.reset()

        saved = true
        compose.framesUntilIdle(counter).assertQuietBetweenEnds()
    }
}
```
<!-- <opt:ui_components> -->

Budgets the shell's own tests hold: a switch to a visited tab recomposes only the two bar cells whose selection flips; a
push composes the new screen once and nothing while it slides; a pop composes neither screen; a press composes nothing.
<!-- </opt:ui_components> -->
<!-- <opt:performance&ui_components> -->
On the phone, `perf.sh recompose` counts the frames that recomposed per transition from a trace of a release build.
Gates: `recompose.tab_switch.max` 2, `recompose.pop.max` 2, `recompose.back_gesture_progress.max` 0.
<!-- </opt:performance&ui_components> -->

---

## Layers and alpha

### No alpha on a full-screen layer

**Rule.** Never fade a whole screen or a full-screen layer with alpha below 1, and never cross-fade two full screens
(`AnimatedContent`, `Crossfade`). Draw a scrim as a rectangle (`drawRect(scrim, alpha = …)` read in draw); fade a
single card or a short entrance with `CompositingStrategy.ModulateAlpha`; use `CompositingStrategy.Offscreen` only for
a small mask, with the reason in a comment.

**Why.** A layer with alpha below 1 and overlapping content is rendered into an offscreen buffer and then composited,
on every frame of the fade: full-screen, a second full-screen pass per frame. `ModulateAlpha` applies the alpha to each
draw instead, with no buffer. A small layer *resting* below 1 is offscreen in every frame the app draws: a 132 × 132
badge resting at 0.85 caused most of a traced journey's waits for a free buffer (106 waits, 332 ms, against 34 and
115 ms with `ModulateAlpha`).

**Enforced by.** `layer-alpha-fill` (features and the design system).
<!-- <opt:ui_components> -->
The shell has two bounded exceptions in `:core:navigation`, outside the rule: the back card's texture, rendered once
per gesture ([Navigation and back](#navigation-and-back)), and `HostCrossfade`, a 200 ms fade between whole hosts
that runs only when the session changes, never during navigation.
<!-- </opt:ui_components> -->

### Paint the screen once

A screen's background is painted once per frame. HWUI does no occlusion culling: a covered fill is still drawn, every
frame, and a full-window fill is 2.6 Mpx. Measured with a second full-screen fill removed: the steady GPU time per frame
fell from 6.4 to 5.4 ms in a tab switch and from 8.0 to 4.9 ms in a push's slide.
<!-- <opt:ui_components> -->
Every navigation entry paints `colors.background` and says so through `LocalPaintedBackground`, and `AppScaffold`
paints nothing on top of it: never add a full-screen background under a screen.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
Paint the background once, where the screen is hosted (the start screen and the placeholders paint
`AppTheme.colors.background`), and never again in a scaffold or a screen on top of it.
<!-- </opt:!ui_components> -->
Check with `adb shell setprop debug.hwui.overdraw show` on a release build, and set it back to `false` after. —
*review*

### Blur, shadows and separators

- No `RenderEffect` blur or `Modifier.blur` over a full screen or behind a modal surface: it renders offscreen on
  every frame the content or the surface moves.
- A shadow never follows a size that animates, and nothing fades through a shadow: Compose rebuilds a blurred bitmap
  for each new size and caches every one.
- A shape clipped or filled on every frame of a motion is a rounded rectangle, never a path of cubics: Skia clips a
  rounded rectangle analytically and masks a general path every frame.
- Nothing allocates in code that runs every frame: build brushes, paths and shapes in `drawWithCache`, reuse a rewound
  `Path`, no iterators or `() -> Float` lambdas called per frame in a hot loop (they box). Allocation per frame shows as
  garbage collections competing with the frame for the big cores.
- **A separator one pixel thick is a filled rect, never a one-pixel `drawLine`.** The GPU renderer draws a stroked line
  at far greater cost than a fill of the same pixels, and a separator is drawn again in every frame its screen moves.
  Measured: one stroked line under a pushed screen's top bar kept a push's third to seventh frames at about 10 ms of
  GPU each; filled, about 7.8 ms, and every separator drawn as a fill took push and pop with the Back key from
  8.1 ms/s of hitches to 0. — `pixel-line`
<!-- <opt:ui_components> -->
  Use `Modifier.hairline(…)` on the element it separates, or `drawHairline` inside the design system.
<!-- </opt:ui_components> -->
<!-- <opt:liquidglass> -->

### Glass

Glass that samples the scene behind it is the most expensive thing the design system can draw, so its tier is a
measured policy (`GlassQuality`, decided by `rememberGlassQuality()`), not a device check: `Live` from API 33 on a
healthy device, `LiveNoLens` on API 31-32 or a warming device, `Frosted` (samples nothing) below API 31, on low RAM,
battery saver, heat or a GPU that fell behind, and `Solid` when the person asks to reduce transparency. Measured
signals only ever lower the tier for the rest of the session. Only floating chrome in a `GlassStage`'s `chrome` slot
samples; anything inside a screen is `Modifier.glassSurface`, which samples nothing (a sampling surface inside its own
scene fails a run-time check). Never put alpha below 1, `AnimatedVisibility` or a `saveLayer` around a sampling
surface; never read state inside backdrop effects; nothing in the recorded scene animates forever, because it
re-blurs every sample every frame. Measure glass with the tier fixed as well as with the policy deciding, or a run can
frost itself and pass by removing the glass.
<!-- </opt:liquidglass> -->

---

## Lists

### Key every row; type rows that differ

Every `items(…)` passes a stable `key`, and a scope with more than one kind of item gives each a `contentType`. Without
a key a row's state and the scroll position follow its index, not its item, and item animations cannot work; Compose
reuses a row's composition only for an item of the same content type, so an untyped mixed list composes rows from
scratch as they scroll in. — `lazy-key`, `lazy-content-type`

### Pad scrolling content inside the scroll

Padding that keeps content clear of the bars goes inside the scroll (`contentPadding` on a lazy list, `.padding(…)`
after `.verticalScroll(…)`), never on the list's container, so content runs under the bars and rests clear of them.
<!-- <opt:ui_components> -->
On a screen that padding is `chromeContentPadding(base)`, which reads the bars' heights when the list measures (so a
bar settling or the keyboard rising lays the list out again and composes nothing); inside an `AppSheet` it is the
sheet's own `contentPadding`. Never read `LocalBottomChrome.current.value` in composition to pad a list. Call
`ReportScrollEdge(state)` beside a screen's primary list, so a docked bar shows its surface only while content runs
under it, and `ScrollToTopOnReselect(state)` on a tab's root (Android delivers no status-bar tap).

```kotlin
LazyColumn(
    state = listState,
    contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
    flingBehavior = AppScrollDefaults.flingBehavior(),
) {
    items(orders, key = { it.id }, contentType = { it.kind }) { order ->
        OrderRow(order, onOpen = onOpen, modifier = appAnimateItem())
    }
}
```
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
A screen passes the `contentPadding` it is given straight through (`contentPadding = contentPadding`), which the
rule accepts; read inset sizes in measure, never in composition, so the keyboard rising lays the list out again and
composes nothing.
<!-- </opt:!ui_components> -->
— `scroll-chrome` (vertical lazy lists, grids and `verticalScroll` in features)

### Name the fling on every scrolling container

Compose has no composition local for the default fling, so a container that names none gets Android's spline fling
and decelerates differently from its neighbours, which no preview shows. Every lazy list, grid, pager,
`verticalScroll` and `horizontalScroll` the person can scroll passes a `flingBehavior` from the design system.
<!-- <opt:ui_components> -->
`AppScrollDefaults.flingBehavior()` (pagers: `AppScrollDefaults.pagerFlingBehavior(state)`) keeps 0.998 of its speed
per millisecond, as an iOS scroll view does, ends at 30 px/s (at Compose's default rest threshold a gentle release
keeps a list moving invisibly for four seconds, and a tap in that time only stops the fling), and carries part of a
recent fling's speed into a new flick. `AppTheme` installs the rubber-band edge (`AppOverscroll`) for every container.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
Build that fling once in `:core:designsystem` ([First things to build](DESIGN-SYSTEM.md#first-things-to-build) has the
curve the template used: keep 0.998 of the speed per millisecond, stop at 30 px/s).
<!-- </opt:!ui_components> -->
— `scroll-fling` (features, `:core:ui`, the app)

### No subcomposition in rows

No `SubcomposeAsyncImage`, `BoxWithConstraints` or other `SubcomposeLayout` inside a row; size from
`AppTheme.containerClass`, from a custom `Layout`'s constraints, or from a fixed slot. Subcomposition composes during
measure, after the frame's composition pass, and lazy prefetch cannot hide it: prefetch runs only between frames, so a
subcomposing row costs a composition inside the frame each time it enters, and `BoxWithConstraints` costs a frame. —
`subcompose-image`, `adaptive-box-constraints`

### Row animation, loading rows, the first fling

- Rows that move, appear or leave animate their placement in the layout phase on keyed rows
  <!-- <opt:ui_components> -->
  with `appAnimateItem()` (an extension on the row's `LazyItemScope`), which does nothing under reduce motion;
  <!-- </opt:ui_components> -->
  <!-- <opt:!ui_components> -->
  with `Modifier.animateItem()` and the theme's `layout` spring, skipped under reduce motion;
  <!-- </opt:!ui_components> -->
  never `animateContentSize()` on a list item, which re-measures the list every frame of the change. — *review*
- A loading list is the real rows with placeholder data, redacted, in a `LazyListState` of its own and with the loaded
  list's padding, swapped for the loaded list in one frame: no `AnimatedContent`, no cross-fade. A redacted row is as
  tall as the row that replaces it at every font scale, so nothing jumps when data arrives (a bone sized by the text
  style's `lineHeight` instead of the line made every row jump 4 dp). A shimmer is one band per window that writes no
  snapshot state and stops while no bone is on screen. — *review*
- The first fling after an install or `pm clear` compiles GPU programs on the RenderThread (10-70 ms each). A card
  clipped to its rounded shape once the list's edge cut it compiled one (11-31 ms); clip a card's content only when it
  overflows its shape. — *review*
- Lazy prefetch is pausable and on by default. Add a `LazyLayoutCacheWindow` only when a scroll trace shows
  `compose:lazy:prefetch` running out of time, and check memory after.

**Lists protect.**
<!-- <opt:performance&ui_components> -->
Gates `scroll_root.janky_pct` and `scroll_minimize.janky_pct` at most 1.0 %, `scroll_root.gpu_p90_ms` and
`scroll_minimize.gpu_p90_ms` at most 7.0 ms; no recomposition per scroll frame.
<!-- </opt:performance&ui_components> -->
<!-- <opt:!performance|!ui_components> -->
The template held a root list's scroll to at most 1.0 % janky frames and a GPU p90 of 7.0 ms on the reference phone,
with no recomposition per scroll frame; measure yours with `dumpsys gfxinfo` ([Measuring](#measuring)).
<!-- </opt:!performance|!ui_components> -->

---

## Motion

### Springs from tokens, never `tween(300)`

**Rule.** Every spring and decay comes from `AppTheme.springs`; fades of colour and opacity use `springs.fadeIn()`
and `springs.fadeOut()`. No raw `tween`, colour, or `dp` other than `0.dp` and `1.dp` in a feature.

**Why.** The springs follow the app's motion style, are built once per process with visibility thresholds (so the
invisible tail of a settle stops drawing) and allocate nothing per recomposition. A spring is a response time and a
bounce, the way iOS describes one, so a motion keeps its feel at any distance; `AppTheme.reduceMotion` shortens or
drops motion for people who ask.

| Spring (Standard style) | Response | Bounce | Used for |
|---|---|---|---|
| `press` | 0.16 s | 0 | finger down |
| `release` | 0.32 s | 0.15 | finger up |
| `thumb` | 0.28 s | 0 | switches, segmented controls, a tab's reveal |
| `screen` | 0.344 s | 0 | push, pop, covers |
| `sheet` | 0.34 s | 0 | sheets and detents, toasts |
| `dialog` | 0.275 s | 0 | alerts, menus |
| `layout` | 0.36 s | 0 | list placement, expand and collapse |
| `emphasis` | 0.5 s | 0.15 | pop-ins, badges, success marks |
| `interactive` | 0.15 s | 0.14 | a value trailing the finger |
| `bounceBack` | 0.444 s | -0.1 | content returning from past an edge |

Full-screen moves never bounce in any style. The values live in `DesignTokens.kt` ([DESIGN-SYSTEM.md](DESIGN-SYSTEM.md#the-theme-and-its-tokens)).
— `raw-tween`, `raw-color`, `raw-dp`

### A motion a finger started ends on a spring with the finger's speed

When a finger lets go, animate to the target with a spring from `AppTheme.springs` seeded with the release velocity
(`Animatable.animateTo(target, spec, initialVelocity)`; for a 0-to-1 fraction, divide px/s by the travel). Interrupt
with `animateTo`, never `snapTo` on anything visible. A release that ends on a tween, or on a spring from rest, shows a
speed jump that `gfxinfo` does not count as jank but people see. — *review*

### Press feedback is deliberate

No ripple and no grey overlay: the theme installs no indication, so a bare `clickable` shows nothing at all.
<!-- <opt:ui_components> -->
A tappable control says its feedback with `Modifier.pressable(onClick, feedback = PressFeedback.…)`: `Scale` for
filled buttons, chips and cards, `Dim` for text and icon buttons, `Highlight` for rows, `None` where the control's own
change answers. One node does the click, the touch target and the feedback, and reads the press only where the
content is placed, so pressing recomposes nothing (`PressableTest` asserts zero recompositions).
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
Build one press modifier in the design system (a `Modifier.Node` that handles the click, the touch target and a scale
or dim read in its draw or layer), and have every control use it, so a press composes nothing.
<!-- </opt:!ui_components> -->
— `bare-clickable`, `ripple`, `material`

### Nothing animates while idle

Nothing runs forever on its own. Something that loops (a spinner, a shimmer, ambient decoration):

- pauses while its screen is hidden, read in an effect or a draw block, never in composition;
- draws in a `graphicsLayer()` of its own, and steps on a timer (`delay(step)`) where it can step, rather than asking
  for every vsync with `withFrameNanos`, which draws 120 frames a second for as long as the screen shows;
- stops or slows under `AppTheme.reduceMotion`: decoration stops; an indicator whose motion is the message slows;
- if it is decoration, moves only layer properties, votes `FrameRateCategory.Normal` and winds down after a few
  seconds.

**Why.** A loop keeps the display at its full rate, the GPU busy and the battery draining on a screen nobody touches.
An app whose ambient orb ignored this drew 606-632 frames in 5 idle seconds.
<!-- <opt:ui_components> -->
`AppActivityIndicator` and the skeleton shimmer (`ShimmerClock`: a 1200 ms sweep, then 800 ms that draws no frames)
already stop when `LocalTabVisible` says their screen is hidden.
<!-- </opt:ui_components> -->
<!-- <opt:performance&ui_components> -->
Gate: `idle.frames` = 0 (5 s on every tab).
<!-- </opt:performance&ui_components> -->
— *review*

### Ask for 120 Hz only while layer-only motion moves

On a phone with an adaptive refresh rate, content runs at a lower rate unless something votes. A node whose placement
changes votes High already; a motion that changes only `graphicsLayer` properties casts no vote and may run at 60 Hz.
Set `Modifier.preferredFrameRate(FrameRateCategory.High)` while it moves and back after; never a fixed rate, never a
permanent vote, which would hold 120 Hz for a spinner that steps eight times a second. — *review*

---

## Navigation and back

### A destination's first composition is final

**Rule.** Build a destination's first state from what is already in memory, synchronously, so its first frame shows
its content: give the repository a `peek(id)` that returns the item from memory with no I/O and start the ViewModel
from it. Show a skeleton only when nothing is in memory. Load secondary sections (related rows, a second tab of
detail) after the transition has landed.

**Why.** Every state change that lands during the transition recomposes the arriving screen inside the motion.
Measured: in 2 of 8 pushes the loaded state arrived one frame after the composing frame, and that frame took 15-18 ms
on the main thread; related rows loaded with the item composed 185 composables 220 ms into every push, mid-slide.
<!-- <opt:ui_components> -->
`LocalNavigationSettled.current` is a `() -> Boolean` that turns true at 99 % of the travel: wait for it in an effect
(`snapshotFlow { settled() }.first { it }`), never read it in composition. Gate (non-regression):
`pushpop.push_first_frame_ms` median 14.4 ms, max 18.3 ms.
<!-- </opt:ui_components> -->
<!-- <opt:sample> -->
The reference detail screen does not follow this yet: `SampleDetailViewModel` starts empty and loads the related rows
with the item, although `SampleRepository.peek(id)` exists for exactly this. Copy its assisted injection, not its
loading.
<!-- </opt:sample> -->
— *review*

### Features never move screens

A feature never sets `NavDisplay` transitions and never touches a back stack: it registers `entry<Key>(…)` and
navigates through `AppNavigator`. Sheets and covers are presentations, never content shown inside a screen. —
`nav-fade`; *review*

### Back goes through navigationevent

Handle Back with `NavigationBackHandler`, never `BackHandler`, `PredictiveBackHandler` or the Activity's dispatcher:
one back system lets the host coalesce rapid presses and leave the start destination with no handler, so the system's
back-to-home animation plays. `PredictiveBackHandler` also costs a Flow and a coroutine resume per event. —
`legacy-back`, `verifyManifestRules`
<!-- <opt:ui_components> -->

### How the shell moves screens

Know what it does, so a screen does not undo it:

- **One spring for every full-screen move** (`screen`, no bounce in any style). The covered page moves a third as far
  under a drawn scrim; no alpha and no scale on whole screens. The slide starts after the frame that composes the new
  screen, so that heavy frame shows nothing moving.
- **Each screen is a layer of its own, and only what is visible is drawn.** A slide frame records only the operations
  around each screen: the main thread records 38 % less per frame, and the share of a push's frames over 6 ms on the
  RenderThread fell from about a fifth to 6 %.
- **Hidden tabs and covered screens are parked**, never removed: measured, placed, their layer at alpha 0 off the
  window, so their display lists survive and showing them changes two layer properties. Removing a subtree throws its
  layers away and rebuilds them on the frame that has to move. A hidden tab's lifecycle is CREATED, so its collectors
  stop. Keep `AppSheet` composed and drive `visible` for the same reason (`sheet-conditional`).
- **Tab switches never cross-fade.** The incoming tab springs in from 12 dp in its layer block; the outgoing one is
  parked at once. A full-screen veil over the incoming tab measured a GPU p90 of 12 ms and was removed.
- **Unopened tabs are built while idle** (`TabWarmUp`, paused precomposition in slices that stop before the next frame
  is due), so shaders and glyphs are paid out of sight.
- **Commands queue.** Navigation during a transition waits; Back pressed while screens move is applied as one pop.

### The back gesture

The back card follows the finger: the screen shrinks to 0.90, drifts toward the far edge, rounds its corners, over a
scrim; on commit the stack pops with no slide. Its progress is read only in layer and draw blocks (gate
`recompose.back_gesture_progress.max` = 0), each progress event is applied in the frame it arrives in, and the screen
in the card is drawn from one texture rendered once per gesture (`BackCardState.textures`): mid-gesture hitches from
the RenderThread fell from 38 to 2 in six runs, at the cost of about 10 MB of GPU memory while the card is up.
Anything that changes inside that screen while the card is up renders the texture again, so a screen should hold still
under the card. Do not use Navigation 3's built-in predictive pop: it gets no touch position, recomposes per event and
fades a whole screen offscreen every frame.

### Sheets

`AppSheet` follows the finger 1:1, measures its content once per settle and never during a drag, and moves only layers
while it moves (0 recompositions per drag move). Keep it composed and drive `visible`; its content is one root (a lazy
list or `SheetColumn`) taking `contentPadding`, with no `imePadding()` inside (`sheet-root`, `sheet-ime-padding`);
`AppSheet(modifier = …)` carries test tags and semantics only (`sheet-modifier`); no `Dialog` or `Popup` window and no
shadow modifier inside the sheet system (`sheet-window`, `sheet-shadow`). Measured: a sheet with a list spends
13.5-24.2 ms of main-thread work on its opening frame, 10-16 ms of it laying out the first rows; keep a sheet's first
screen of content light.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->

### When you build a shell

The template's shell was measured into these rules; keep them when you build yours on `NavDisplay`:

- **Never use Navigation 3's default transitions**, which fade whole screens (and scale one during a predictive
  back): each fade renders both screens offscreen on every frame. The host passes none until you give it yours, as
  `transitionSpec`, `popTransitionSpec` and `predictivePopTransitionSpec` in `:core:navigation`.
- **Slide whole screens on the `screen` spring; never fade or scale them.** Start the slide after the frame that
  composes the new screen, so that heavy frame shows nothing moving.
- **Give each screen a layer of its own**, so a slide frame records only the operations around each screen (measured:
  38 % less main-thread recording per frame).
- **Hide by parking, never by removing.** A hidden tab or a covered screen stays composed and placed with its layer
  at alpha 0; removing it throws its layers away and rebuilds them on the frame that has to move. Cap a hidden tab's
  lifecycle at CREATED, so its collectors stop.
- **Tab switches never cross-fade** (a full-screen veil measured a GPU p90 of 12 ms).
- **A back gesture's progress is read only in layer and draw blocks**, and applied in the frame it arrives in.
<!-- </opt:!ui_components> -->

<!-- <opt:performance&ui_components> -->
**Navigation protects.** For tab switches, push and pop, the back gesture and Rapid Back: hitch time at most
5.0 ms per second of motion (`*.hitch_ms_per_s`) and janky frames after each motion's first at most 1.0 %
(`*.janky_after_first_pct`); late first frames reported (`*.late_first_frames`); navigation stress at most 6.0 % janky
(`nav_stress.janky_pct`).
<!-- </opt:performance&ui_components> -->
<!-- <opt:!performance|!ui_components> -->
**Navigation protects.** The template held tab switches, push and pop, the back gesture and Rapid Back to at most
5.0 ms of hitches per second of motion and 1.0 % janky frames after each motion's first, on the reference phone.
<!-- </opt:!performance|!ui_components> -->
<!-- <opt:coil> -->

---

## Images

Draw remote images as painters, with Coil's `AsyncImage` and its `placeholder` and `error` painters, never
`SubcomposeAsyncImage`: it composes per load state and swaps content when the load finishes, mid-scroll, while a
painter's finished load only changes what is drawn. — `subcompose-image`
<!-- <opt:ui_components> -->
`AppNetworkImage` does this, and draws a skeleton inside an active redaction.
<!-- </opt:ui_components> -->

The app has one image loader. Never build a second: it means a second memory cache, and a second client a second
connection pool and set of certificate pins.
<!-- <opt:network> -->
Coil's singleton loader is set once in the `Application`, on the app's own `OkHttpClient`. — *review*
<!-- </opt:network> -->
<!-- <opt:!network> -->
Without a network module Coil builds its own client; once the app has one, set the singleton loader on it. — *review*
<!-- </opt:!network> -->
<!-- </opt:coil> -->

---

## Start-up, profiles and size

### Nothing on the start-up path the first frame does not need

**Rule.** Keep `Application.onCreate` and the first screen free of I/O and eager singletons; start what the first
frame does not need after it, in a message of its own.

**Measured on the reference phone.** The Play Store update check, started inside the cold start, woke the Play Store
process for 18.6-27.2 ms of CPU mid-launch; started after the first frame, the cold start median fell from 403 to
365.5 ms. Firebase's eager components took 63 ms of `bindApplication` in a build with Firebase. A launch hook that read
the session token to see whether one existed decrypted it through the Keystore on the main thread: five binder calls,
8-13 ms of every start. So the launch decides signed in or out from `AuthTokenStore.isAuthenticated`, never by reading
the tokens.

- WorkManager, a database or a Glance widget started at launch costs start-up for everyone: start them when there is
  work. No animated splash: the system splash covers launch.
- Never add `preloaded_fonts` meta-data to the manifest: the framework then fetches each font synchronously on the
  main thread before `Application.onCreate`, up to 500 ms per font, into a cache Compose never reads. Downloaded
  families go through `AppFontPreload`, which waits at most 150 ms (`PreloadBudget`) and fixes its answer for the
  process, so text never reflows mid-transition.
- The theme never recreates the activity: the manifest handles `uiMode` and the size and layout changes, so a theme
  change, a resize or a fold is laid out again in place. — `verifyManifestRules`; lint `LockedOrientationActivity`,
  `NonResizeableActivity`
<!-- <opt:performance&ui_components> -->

**Start-up protects.** Gates `coldstart.median_ms` at most 380 ms (compiled with the profile) and
`coldstart_installed.median_ms` at most 400 ms (the profile from the APK only).
<!-- </opt:performance&ui_components> -->
<!-- <opt:!performance|!ui_components> -->

**Start-up protects.** The template's cold start measured a median under 380 ms compiled with its profile, and under
400 ms as installed, on the reference phone.
<!-- </opt:!performance|!ui_components> -->

### Baseline profiles

Without a profile, the first use of every method is interpreted or JIT-compiled in the frame that needs it. The app
and the libraries ship `src/main/baseline-prof.txt`, compiled at install by `profileinstaller`, and
`adb shell cmd package compile -f -m speed-profile <applicationId>` compiles an installed profile now instead of at the
next idle maintenance.
<!-- <opt:performance> -->

After a journey changes, record the profile again on a phone with gesture navigation and check the file is not empty
(an install that fails still reports `BUILD SUCCESSFUL` with no rules):

<!-- <opt:ui_components> -->
```bash
./gradlew :app:generateBaselineProfile
python3 tools/perf/split_profile.py      # moves each module's rules into its own profile
```

`speed-profile` compiles only methods marked hot, and how many a recording marks hot depends on the device (the same
journeys marked 248 shell methods hot on an API 35 emulator and 9 on the reference phone): when a recording marks far
fewer, merge it with the previous one rather than ship the poorer one.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
```bash
./gradlew :app:generateBaselineProfile
```

The generator records start-up only (`StartupBenchmark` measures it), because the app has one blank screen. Add a
journey to `BaselineProfileGenerator` for each screen you build, and record again.
<!-- </opt:!ui_components> -->
<!-- </opt:performance> -->
<!-- <opt:!performance> -->
This project has no benchmark module, so ship the checked-in profiles; add the `performance` feature's module (or
Macrobenchmark's baseline profile plugin) to record new ones once the screens change.
<!-- </opt:!performance> -->

### R8 and APK size

- Release builds run R8 in full mode with resource shrinking, and `app/proguard-rules.pro` holds only rules that name
  their reflection; the `r8-analyzer` skill reviews keep rules. Never add `-assumenosideeffects` for Compose's
  `traceEventStart`: the recomposition tools read those markers.
- `localeFilters` keeps only the languages the app ships, and the APK carries no `LICENSE` files.
- Flavours are only on the app module, and no library has a `BuildConfig` ([ARCHITECTURE.md](ARCHITECTURE.md#build)).
- `./gradlew build` runs R8 for every release-like variant at once; on a machine short of memory, build the variant
  you need. Set the heap in `gradle.properties`, never with `-Dorg.gradle.jvmargs`, which starts a second daemon.

---

## Memory

- **Judge the app's own PSS: the total less Graphics.** Graphics is the window's buffers (51-55 MB for every app at
  1080 × 2400 and 120 Hz on the reference phone), so the total cannot be compared across devices.
<!-- <opt:performance&ui_components> -->
  Gate: `meminfo.pss_app_mb` at most 58.4 MB on the everything preset (53.3 on standard), non-regression.
<!-- </opt:performance&ui_components> -->
- **Never read PSS while animating.** `Debug.getMemoryInfo` and `dumpsys meminfo` walk every mapping under the
  kernel's lock on the address space; during a scroll that cost about one janky frame. Sample only in a pause.
- **Native memory is mostly text:** Skia's glyph caches (a variable font gives every weight its own strike) and the
  GPU driver's compiled programs. Fewer weights and sizes in the type scale is the lever; HWUI purges these caches on
  `onTrimMemory`.

---

## Measuring

### Builds that mean something

- Measure `devRelease` (or `devBenchmarkRelease` with the benchmark module): R8, the baseline profile compiled with
  `speed-profile`, not debuggable. Never quote a debug build: it runs without R8 or a profile, mostly interpreted, and
  on the reference phone a `devDebug` scroll read about 50 % janky frames where `devRelease` read a few percent.
- On a real phone, unlocked, screen on. An emulator's numbers are relative evidence, never a gate.
<!-- <opt:perfhud> -->
- With the performance overlay hidden: launch with `--es perf.hud off`, or clear the app's data first.
<!-- </opt:perfhud> -->

### The noise rule

One build measured several times in one session spreads: 0.15-0.69 % janky on the template's root-list scroll,
3.5-5.2 % on random navigation. A limit inside that spread passes or fails a build by chance. So:

- **Ceilings, not targets.** Janky frames are held to visible ceilings (1.0 % for scrolls and motions, 6.0 % for
  random navigation); under a ceiling, the goal is still about 0 %.
- **Judge a change against noise.** Measure the build before and after in the same session, on the same phone, with
  the arms rotated (A, B, A, B) and the same number of runs. A row is worse only if the change's median exceeds the
  other's by more than the larger of the two arms' spreads (max less min over the runs).
- **Medians over several runs**, never a single run, and never two runs from different days.
<!-- <opt:performance&ui_components> -->

### The perf harness

`tools/perf/perf.sh` installs the APK, clears the app, installs and compiles its profile as Play does, visits every tab
once, then runs the scenarios you name, three runs each by default; `--gate` checks the median against
`tools/perf/gates.toml`.

```bash
./gradlew :app:assembleDevBenchmarkRelease
tools/perf/perf.sh --package <applicationId> --variant devBenchmarkRelease --gate solid \
    tabs tabs_tap pushpop pushpop_gesture rapid_back scroll_root scroll_minimize idle nav_stress restore \
    meminfo recompose coldstart coldstart_installed
```

`--gate solid` checks limits only; to compare two builds, pass the earlier run's `summary.json` as `--baseline` with
`--gate smoke`, or compare runs on disk with `python3 tools/perf/check_gates.py`. Rows marked non-regression in
`gates.toml` may only go down. `tools/perf/record.sh` screen-records a journey at 720 × 1600 with every frame
extracted: attach it to any change to motion.
<!-- </opt:performance&ui_components> -->
<!-- <opt:!performance|!ui_components> -->

### Measuring without a harness

```bash
adb shell dumpsys gfxinfo <applicationId> reset
# run the journey on the phone
adb shell dumpsys gfxinfo <applicationId>          # janky frames and frame-time percentiles
```

For start-up, Macrobenchmark's `StartupTimingMetric` (or `am start -W` over several cold starts, the median of their
`TotalTime`). For a slow frame, a Perfetto trace through the `android-profiler` skill shows which thread lost the
time.
<!-- </opt:!performance|!ui_components> -->

### Reading a slow frame

1. **One frame per transition is the first frame from idle**: if the hitch time and the janky frames after the first
   are low, the motion is smooth. Do not chase it by drawing less; on the reference phone it does not move.
2. **Janky after the first frame: something costs every frame.** In this order: recomposition every frame (write a
   recomposition test for the journey), alpha on a full-screen layer, allocation in draw code, a layer recorded again
   every frame, work the navigation itself starts on the main thread.
3. **GPU time high: too many pixels.** `debug.hwui.overdraw show` and `debug.hwui.show_layers_updates true` show a
   second full-screen fill, an offscreen layer larger than a card, a blur; set both back to `false` after.
4. **A first use slow** (the first push, the first back swipe): a GPU program compiled on first draw; find
   `shader_compile` in a Perfetto trace, then avoid the combination or draw it once while idle.
5. **Frames while idle:** something animates forever; watch frame times while touching nothing.
6. **Not the app:** device screenshots and recordings go to `/data/local/tmp`, never `/sdcard`, where the media scanner
   woke a gallery app that ran up to 3.7 cores inside the captures. A scripted `input tap` has no gap between down and
   up, so nothing that starts on press-down shows in it.

A Perfetto trace of a release build names the cause: the main thread's `Choreographer#doFrame`, the RenderThread's
`DrawFrame` and the GPU completion of the late frame say which of the three lost the time.

---

## Before you merge UI work

1. The `jetpack-compose-performance` audit ran over every composable you touched, with the category's reference page
   read before each fix.
2. The changed journey composes nothing in its middle frames: `assertQuietBetweenEnds()` passes for it.
3. No full-screen layer has alpha below 1, and no screen adds a second full-screen fill.
4. Every spring comes from `AppTheme.springs`, and a motion a finger started is released with its velocity.
5. Every new lazy list has keys, content types where kinds mix, its padding inside the scroll and a named fling.
6. A new destination's first frame shows its content when the data is in memory.
7. Nothing new animates while idle or while its screen is hidden.
<!-- <opt:performance&ui_components> -->
8. On a release build, `perf.sh --gate solid` passes, and against the previous build measured in the same session
   (arms rotated) no row is worse than the larger of the two arms' spreads.
9. Any new motion has a recording from `record.sh`.
<!-- </opt:performance&ui_components> -->
<!-- <opt:!performance|!ui_components> -->
8. Measured on a release build against the previous build in the same session (arms rotated), no journey you touched
   is worse than the run-to-run spread.
<!-- </opt:!performance|!ui_components> -->
