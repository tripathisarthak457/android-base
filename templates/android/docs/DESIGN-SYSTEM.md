# Design system

`:core:designsystem` is the only place that decides how the app looks: its theme (`AppTheme` and the tokens behind
it) and the components every screen is built from. It has no Material underneath: it is built on Compose's
`foundation` and `ui`, and an `androidx.compose.material` import fails the build (`material`).
<!-- <opt:ui_components> -->
It ships about eighty components in `component/`, the primitives they are built from in `foundation/` (press
feedback, scrolling, drawing, layout), four design styles and four motion styles. **They are a starting point to
reshape, not a kit to ship unchanged:** a finished app that looks exactly like the template was not designed.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
This project was generated without UI components, so the module is a **theme skeleton**: `AppTheme` with every token
slot filled (neutral values, your colours and your font), window size classes, preview plumbing, and no components.
This page is how to build your own on it so that they come out stateless, themed, adaptive and fast.
<!-- </opt:!ui_components> -->

Contents: [the theme and its tokens](#the-theme-and-its-tokens), [the component contract](#the-component-contract),
[interaction](#interaction), [the rule index](#the-rule-index), [templates](#templates),
[first things to build](#first-things-to-build), [previews are render tests](#previews-are-render-tests).

## The theme and its tokens

Everything visual reads the theme through one accessor, `AppTheme`, inside the one `AppTheme(…)` call in the app's
`AppRoot`. Screens and components never write a colour, a size, a duration or a font: they read a slot.

| Slot | Read it as | Holds |
|---|---|---|
<!-- <opt:ui_components> -->
| Colour | `AppTheme.colors` (`AppColors`) | roles, never hues: `background`, `surface`, `surfaceElevated`, `fill`, `separator`, `outline`, `contentPrimary`/`contentSecondary`/`contentTertiary`/`contentDisabled`, `accent` with `onAccent` and `accentSubtle`, `secondary`, `tertiary`, status colours (`success`, `warning`, `danger`, `info`), `scrim`, `focusRing`, and the ink components paint with, pure white or black in every theme and brand (`thumb`, `sheen`, `shade`, `mediaBackdrop`, `onMedia`) |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| Colour | `AppTheme.colors` (`AppColors`) | roles, never hues: `background`, `surface`, `surfaceElevated`, `fill`, `separator`, `outline`, `contentPrimary`/`contentSecondary`/`contentTertiary`/`contentDisabled`, `accent` with `onAccent` and `accentSubtle`, `secondary`, `tertiary`, status colours (`success`, `warning`, `danger`, `info`), `scrim`, `focusRing` |
<!-- </opt:!ui_components> -->
| Type | `AppTheme.typography` (`AppTypography`) | `largeTitle`, `title1`-`title3`, `headline`, `body`, `callout`, `subheadline`, `footnote`, `caption1`, `caption2`, `mono` |
| Spacing | `AppTheme.spacing` (`AppSpacing`) | `xxs` to `xxxl`, `section`, and `gutter`, the margin to the window's edge, which grows with the window |
<!-- <opt:ui_components> -->
| Shape | `AppTheme.shapes` (`AppShapes`) | `none`, `xs` to `xl`, `pill`, `sheet` |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| Shape | `AppTheme.shapes` (`AppShapes`) | `none`, `xs` to `xl`, `pill` |
<!-- </opt:!ui_components> -->
| Size | `AppTheme.sizes` (`AppSizes`) | `minTouchTarget` (48 dp), icon sizes, button and field heights, `rowHeight`, `topBarHeight` |
| Elevation | `AppTheme.elevation` (`AppElevation`) | `raised`, `card`, `overlay`, `modal` |
| Motion | `AppTheme.springs` (`AppSprings`), `AppTheme.reduceMotion` | springs as a response and a bounce (`press`, `release`, `thumb`, `screen`, `sheet`, `dialog`, `layout`, `emphasis`, `interactive`, `bounceBack`), decays for flings, and whether the person asked for less motion |
| Window | `AppTheme.windowClass`, `AppTheme.containerClass`, `AppTheme.layout`, `AppTheme.posture`, `AppTheme.input` | the window's and the pane's size classes, widths content stops at, a foldable's posture, the pointer and keyboard ([ARCHITECTURE.md](ARCHITECTURE.md#every-form-factor)) |
<!-- <opt:ui_components> -->
| Style | `AppTheme.style` (`AppStyle`) | the design style's decisions that must agree: corner radii, how cards, fields and bars are drawn, the surface tone and the type voice |
<!-- </opt:ui_components> -->

### Where the values live, and how to change one

<!-- <opt:ui_components> -->
Every value is in `DesignTokens.kt` under `theme/tokens/`, written when the project was generated with your colours,
font, design style and motion style: `DesignTokens.Palette` holds a light and a dark `AppColors` per surface tone,
`DesignTokens.Spacing`, `Radii`, `Sizes`, `Elevation` and `Type` the metrics and the type scale, `DesignTokens.Motion`
the springs per motion style, and `DesignTokens.Defaults` the style, motion and fonts the theme starts from. Edit the
values there; every slot class reads them, so the whole app follows.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
Every value is in `DesignTokens.kt` under `theme/tokens/`, written when the project was generated with your colours
and font, and nothing else: `DesignTokens.Palette` holds the light and the dark `AppColors`, `DesignTokens.Spacing`,
`Radii`, `Sizes`, `Elevation` and `Type` the metrics and the type scale, `DesignTokens.Motion` the springs, the fling
decays and the fades, and `DesignTokens.Defaults` haptics and the fonts. Edit the values there; every slot class
reads them, so the whole app follows.
<!-- </opt:!ui_components> -->

- **Change a colour:** edit it in both the light and the dark palette, then run `./gradlew :core:designsystem:test`:
  `PaletteContrastTest` checks every text colour against its background in both themes against WCAG AA.
- **Add a token:** add the value to `DesignTokens.kt`, a property to the slot's class (`AppSpacing`, `AppSizes`,
  `AppColors` and so on) that reads it, and, for a colour, the value in every palette. Read it through `AppTheme`. A
  value only one component uses belongs in that component's `XDefaults`, not in the theme.
- **Fonts:** `AppFonts` holds the families; a downloaded family goes through `AppFontPreload`, which waits at most
  150 ms and keeps its answer for the process, so text never reflows ([PERFORMANCE.md](PERFORMANCE.md#start-up-profiles-and-size)).
<!-- <opt:ui_components> -->
- **Restyle everything at once:** `AppTheme(designStyle = AppDesignStyle.Social)`, or edit `AppDesignStyle.style()`.
  Components take their shapes and treatments from the style, so one argument restyles every screen.
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->

**A component's own tokens.** The slots hold only what components share. A value one component needs (a switch's
track width, a bar's height, the pill behind a selected tab, a sheet's corner) is a named constant in that component's
`XDefaults`, built from the slots where it can be (`AppTheme.spacing.md`, `AppTheme.sizes.minTouchTarget`,
`AppTheme.springs.thumb`), as the [component template](#templates) does. Promote it to the theme only when a second
component needs the same value: add it to `DesignTokens.kt`, add a property that reads it to the slot's class
(`AppSizes`, `AppShapes`, `AppSprings`), and read it through `AppTheme`. A colour is a role, never a hue: a new role
is a value in both `DesignTokens.Palette.light` and `DesignTokens.Palette.dark` and a parameter of `AppColors`, and
`PaletteContrastTest` gets the pairing it must keep (text on it at 4.5:1, a control against every surface at 3:1). A
new spring is a `Spring(response, bounce)` in `DesignTokens.Motion` and a property of `AppSprings`, built once like
the others; a full-screen move keeps a bounce of 0.
<!-- </opt:!ui_components> -->

## The component contract

Every component in `:core:designsystem` follows these rules. Most are checked by the build; the rest by review and the
`jetpack-compose-performance` audit.

**State and data.**
- **Stateless by default:** take the value and a callback (`value` and `onValueChange`, `selected` and
  `onSelectedChange`); the caller owns the state. When a component has real policy of its own (a scroll position, a
  drag), hoist it into an `@Stable` class `XState` with a `rememberXState()` that the caller may pass in.
- **Never take `State<T>` or `MutableState<T>`** (`state-param`); take `() -> T` for a value read late (in layout or
  draw), so a change does not recompose the caller.
- **No ViewModel, repository, use case or `Flow` parameter,** and nothing from `data/`: a component sees Compose and
  the theme. Side effects leave only through callbacks.
- **Parameters are stable** (`checkComposeStability`): immutable types, `List` rather than a mutable collection.

**Shape of the API.**
- `modifier: Modifier = Modifier` is the first optional parameter (lint `ModifierParameter`) and is applied once, to
  the root.
- **Slots over data:** take `content: @Composable () -> Unit` (or a `RowScope` lambda) where a caller may want more
  than text, and plain types where it will not.
- **Defaults live in `XDefaults`:** an `object` holding the component's dimensions (`val Height = 32.dp`) and a
  `@Composable fun colors(…)` that builds an `@Immutable` `XColors` from `AppTheme.colors`. No hue, size or duration is
  written anywhere else (`raw-color`, `raw-dp`, `raw-tween`).
- **Accessibility:** a control states its `Role`, keeps a touch target of at least `AppTheme.sizes.minTouchTarget`
  (48 dp), and takes `contentDescription: String?` where it shows only an icon (an explicit `null` means decorative).
  Copy comes from resources (`hardcoded-copy`); previews may use sample text.

**Fast.**
- Custom modifiers are `Modifier.Node`, never `Modifier.composed` (`composed`).
- Animated and dragged values are read in `graphicsLayer { }`, `offset { }`, `layout { }` or a draw block
  (`animated-delegate`); motion comes from `AppTheme.springs` and stops under `AppTheme.reduceMotion`.
- Lazy content has keys and content types (`lazy-key`, `lazy-content-type`); no subcomposition in rows.
- No offscreen layer without a written reason; never alpha below 1 on a full-screen layer (`layer-alpha-fill`).
- A component that animates has a recomposition test ([PERFORMANCE.md](PERFORMANCE.md#prove-it-with-a-recomposition-test)).
<!-- <opt:ui_components> -->

The shipped components follow this contract. Edit a component in place when the design calls for it; do not wrap it
in a new one to avoid touching it, and do not fork a copy per screen. When nothing fits, build the new one in
`component/` from the primitives in `foundation/`. Keep what makes them solid when you change their look: 48 dp
targets, descriptions on icon-only controls, contrast in both themes, reduce motion, and loading states that do not
change a control's size. The catalog app (when present) shows every component in both themes.
<!-- </opt:ui_components> -->

## Interaction

No ripple and no grey overlay: `AppTheme` installs an indication that draws nothing, so a press looks like whatever
the control says it looks like, and a bare `clickable` shows nothing at all.
<!-- <opt:ui_components> -->
Controls use `Modifier.pressable(onClick, feedback = PressFeedback.…)`: `Scale` for filled buttons, chips and cards,
`Dim` for text and icon buttons, `Highlight` for rows, `None` where the control's own change answers. Features call
components, or `pressable` directly, never `clickable` (`bare-clickable`).
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
Build one press modifier as a `Modifier.Node` (the click, the touch target, and a scale or dim read in its draw or
layer, so a press composes nothing) and use it in every control; a feature never calls `clickable` itself
(`bare-clickable`), it calls a component.
<!-- </opt:!ui_components> -->
Haptics follow `AppTheme.defaults.hapticsEnabled` and the person's setting.

## The rule index

Every rule `verifyCodeRules` runs in this project, what it protects and how to fix a finding. A justified exception is
`// allow(<rule-id>): <reason>` on the line above; a marker with no reason, or one that silences nothing, fails too.
Each rule's message names the reference page in `.agents/skills/` where one applies.

| Rule | Protects | Fix |
|---|---|---|
| `material`, `ripple` | one design system, no Material underneath | use a component from `:core:designsystem` |
<!-- <opt:ui_components> -->
| `bare-clickable` | deliberate press feedback | `Modifier.pressable(onClick, feedback = PressFeedback.…)` |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| `bare-clickable` | deliberate press feedback | call a component, or the design system's press modifier |
<!-- </opt:!ui_components> -->
| `composed` | no allocation per call site | write a `Modifier.Node` |
| `animated-delegate` | no read per frame in composition | `val x = animate…()`, read `x.value` in a layout or draw lambda |
| `state-param` | reads where they are needed | take `() -> T` instead of `State<T>` |
| `collect-as-state` | flows stop with the screen | `collectAsStateWithLifecycle()` |
| `layer-alpha-fill` | no full-screen offscreen pass per frame | draw a scrim rect, or `CompositingStrategy.ModulateAlpha` |
<!-- <opt:ui_components> -->
| `pixel-line` | a cheap separator | `Modifier.hairline`, or `drawHairline` in the design system |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| `pixel-line` | a cheap separator | draw a filled rect one pixel tall |
<!-- </opt:!ui_components> -->
| `raw-color`, `raw-tween`, `raw-dp` | one theme, dark mode, the motion style | `AppTheme.colors`, `AppTheme.springs`, `AppTheme.spacing` or a size token |
| `lazy-key`, `lazy-content-type` | row state and reuse while scrolling | `key = { it.id }`, and `contentType` when kinds mix |
<!-- <opt:ui_components> -->
| `scroll-chrome` | content runs under the bars and rests clear of them | `contentPadding = chromeContentPadding(…)` inside the scroll |
| `scroll-fling` | one deceleration everywhere | `flingBehavior = AppScrollDefaults.flingBehavior()` |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| `scroll-chrome` | content runs under the bars and rests clear of them | the system bars' insets inside the scroll (`WindowInsets.safeDrawing.asPaddingValues()` as `contentPadding`), or the `contentPadding` your shell hands down |
| `scroll-fling` | one deceleration everywhere | name your design system's fling ([First things to build](#first-things-to-build)) |
<!-- </opt:!ui_components> -->
| `subcompose-image` | no composition inside measure in lists | `AsyncImage` with painters |
| `nav-fade` | screens never set transitions | register the entry; transitions belong to the navigation host |
| `legacy-back` | one back system | `NavigationBackHandler` |
| `vm-forwarding` | stateless screens | only a route takes the ViewModel |
| `experimental-api` | stable APIs only | the stable equivalent, or the decision in the marker |
| `hardcoded-copy` | translatable copy | a string resource |
| `layer-package`, `feature-public` | readable, splittable modules | name packages by concept; only `api` is public |
| `uistate-mutable`, `api-immutable` | the stability the compiler is promised | `val`s and read-only collections |
| `dispatchers-io`, `blocking`, `run-catching` | testable, cancellable coroutines | inject `AppDispatchers`; `runSuspendCatching` |
| `bang-bang` | no crash on a null | make it non-null, or `checkNotNull(x) { "why" }` |
| `slop-comment`, `commented-code`, `file-length` | code a reader can trust | say why; delete dead code; split by concept |
| `spaced-test-name` | device tests run everywhere | `camelCase_with_underscores` in device tests |
| `adaptive-orientation`, `adaptive-screen-metrics`, `adaptive-device-type` | every window works | `AppTheme.windowClass`, `AppTheme.containerClass` |
| `adaptive-fixed-size`, `adaptive-box-constraints` | narrow and short windows; no extra frame | `widthIn(max = AppTheme.layout.…)`; a custom `Layout` |
| `adaptive-mediaquery`, `adaptive-material-scope` | stable, Material-free adaptivity | `AppTheme.input`, `AppTheme.posture`; your own bar and rail |
| `adaptive-preview` | screens seen at every size | `@AppScreenPreviews` on a screen's preview |
<!-- <opt:ui_components> -->
| `sheet-conditional`, `sheet-root`, `sheet-ime-padding`, `sheet-modifier` | sheets that move only layers | keep `AppSheet` composed; one root taking `contentPadding` |
| `sheet-window`, `sheet-shadow`, `sheet-raw-delta`, `sheet-when-else`, `shell-seam` | the sheet system's geometry | no `Dialog` or shadows inside; an `else` branch; no `ShellSeamApi` in features |
<!-- </opt:ui_components> -->
<!-- <opt:!ui_components> -->
| `designsystem-imports` | a design system that cannot reach data | import only Compose and the theme |
| `stateless-params` | stateless components | take values and callbacks, never a ViewModel, repository or `Flow` |
| `raw-type` | one type scale | `AppTheme.typography.x`, `.copy()` for a one-off |
| `feature-primitive` | features arrange, the design system draws | draw it once in `:core:designsystem` |
| `component-preview` | every component rendered and tested | `@AppComponentPreviews` on the component's preview |
<!-- </opt:!ui_components> -->

A `@Composable` in a module without the Compose compiler fails as `compose-compiler`: apply the Compose convention.

## Templates

A component, in a package of its own under the design system's `component` package:

```kotlin component-template
@Immutable
class AppTagColors(
    val container: Color,
    val content: Color,
    val selectedContainer: Color,
    val selectedContent: Color,
)

object AppTagDefaults {
    val Height = 32.dp
    val HorizontalPadding = 12.dp

    @Composable
    fun colors(
        container: Color = AppTheme.colors.fill,
        content: Color = AppTheme.colors.contentPrimary,
        selectedContainer: Color = AppTheme.colors.accent,
        selectedContent: Color = AppTheme.colors.onAccent,
    ): AppTagColors = AppTagColors(container, content, selectedContainer, selectedContent)
}

/** A tag the person turns on and off. Stateless: the caller owns [selected] and changes it in [onSelectedChange]. */
@Composable
fun AppTag(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: AppTagColors = AppTagDefaults.colors(),
) {
    // The touch target is the theme's minimum; the drawn tag is smaller and centred in it.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minHeight = AppTheme.sizes.minTouchTarget)
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = null,
                indication = null,
                onValueChange = onSelectedChange,
            ),
    ) {
        BasicText(
            text = text,
            style = AppTheme.typography.subheadline,
            color = { if (selected) colors.selectedContent else colors.content },
            maxLines = 1,
            modifier = Modifier
                .height(AppTagDefaults.Height)
                .background(if (selected) colors.selectedContainer else colors.container, AppTheme.shapes.pill)
                .padding(horizontal = AppTagDefaults.HorizontalPadding)
                .wrapContentHeight(),
        )
    }
}

@AppComponentPreviews
@Composable
private fun AppTagPreview() {
    var selected by remember { mutableStateOf(true) }
    AppTag(text = "Nearby", selected = selected, onSelectedChange = { selected = it })
}
```

Its test, in the module's `src/test` beside it:

```kotlin component-test-template
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TEST_SDK])
class AppTagTest {

    private val counter = RecompositionCounter()
    private val compose = createComposeRule()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(counter).around(compose)

    @Test
    fun `a tap asks the caller to turn it on`() {
        var selected by mutableStateOf(false)
        compose.setThemed { AppTag(text = "Nearby", selected = selected, onSelectedChange = { selected = it }) }

        compose.onNodeWithText("Nearby").assertIsOff().performClick()

        compose.onNodeWithText("Nearby").assertIsOn()
    }

    @Test
    fun `its touch target is at least the theme's minimum`() {
        compose.setThemed { AppTag(text = "Nearby", selected = false, onSelectedChange = {}) }

        compose.onNodeWithText("Nearby").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `a change composes nothing after its first frame`() {
        var selected by mutableStateOf(false)
        compose.setThemed { AppTag(text = "Nearby", selected = selected, onSelectedChange = {}) }
        compose.mainClock.autoAdvance = false
        compose.waitForIdle()
        counter.reset()

        selected = true

        compose.framesUntilIdle(counter).assertQuietBetweenEnds()
    }
}
```
<!-- <opt:!ui_components> -->

What the build refuses in a component, each line marked with the rule that reports it. `DesignSystemDocTest` runs
the rules over both templates (no finding) and over this one (exactly these), so this page and the guard agree:

```kotlin component-dont
@Composable
fun AppBadge(
    count: State<Int>, // state-param
    updates: Flow<Int>, // stateless-params
    modifier: Modifier = Modifier,
) {
    val grown by animateFloatAsState(if (count.value > 0) 1f else 0.8f, tween(150)) // animated-delegate, raw-tween
    BasicText(
        text = "${count.value}",
        style = TextStyle(fontSize = 12.sp), // raw-type
        modifier = modifier
            .scale(grown)
            .padding(horizontal = 6.dp) // raw-dp
            .background(Color(0xFFD93025), AppTheme.shapes.pill), // raw-color
    )
}

@AppComponentPreviews
@Composable
private fun AppBadgePreview() {
    AppBadge(count = remember { mutableIntStateOf(3) }, updates = emptyFlow())
}
```
<!-- </opt:!ui_components> -->

## First things to build

<!-- <opt:!ui_components> -->
In this order, each under the contract above:

1. **A message presenter.** `MviScreen` hands every message a ViewModel shows to the `MessagePresenter` that
   `LocalMessagePresenter` provides; until you install one, messages go to the log. A toast or snackbar component
   that implements it and is provided above the navigation host makes `showMessage` reach people.
2. **A press modifier and a fling.** One `Modifier.Node` for presses ([Interaction](#interaction)), and one fling for
   every scrolling container (`scroll-fling`). The template's fling, which feels like iOS: a decay that keeps 0.998 of
   its speed per millisecond and stops at 30 px/s, built once as a remembered `FlingBehavior` over that decay, in a
   defaults object every scrolling container's caller passes as `flingBehavior`.
3. **Text, buttons, rows and fields** you need for the first screen, each with its `XDefaults` and preview.
4. **Screens** in place of each `PlaceholderScreen`: a stateless `XScreen(state, onEvent)` that the route already
   connects through `MviScreen`, drawing every `LoadState`, with `@AppScreenPreviews` on its preview
   (`adaptive-preview`) and the screenshots convention in its module
   ([Previews are render tests](#previews-are-render-tests)). Delete the placeholder when the last route stops
   calling it.
5. **A shell**, if the app has sections: tab stacks, a bar or rail switched by `AppTheme.windowClass`, and a
   `SceneStrategy` for list-detail and sheets ([ARCHITECTURE.md](ARCHITECTURE.md#adding-a-shell-tabs-panes-and-sheets)).
<!-- </opt:!ui_components> -->
<!-- <opt:ui_components> -->
Before a new component, look for one to reshape: buttons, fields, rows and lists, sheets, alerts, toasts and the
shell are all there. Make the app look like itself first in the tokens (palette, shapes, spacing, type), then in the
components; the press modifier (`Modifier.pressable`) and the fling (`AppScrollDefaults`) are already built and every
container uses them.
<!-- </opt:ui_components> -->

## Previews are render tests

The screenshots convention scans every `@Preview` in a module, private ones included, and renders each as a
Robolectric test at its configuration: adding a preview is enough to get a test that fails when the component crashes
at twice the font size, in dark theme or right to left. `@AppScreenPreviews` renders a screen on a phone, a phone held
sideways, a foldable, a tablet and a desktop window, at twice the font size and in dark theme; wrap a component's own
previews with `@PreviewWrapper(AppPreviewTheme::class)` so they draw in the theme.
<!-- <opt:!ui_components> -->
The convention runs in `:core:designsystem` from the start. Feature modules start without it, since a placeholder has
nothing to render, so a screen's previews are rendered only once its module applies it: with the first screen and its
`@AppScreenPreviews`, add `id("com.base.app.android.screenshots")` to the `plugins { }` of that feature's
`build.gradle.kts`, and once every feature has screens, apply it in `AndroidFeatureConventionPlugin` instead.
`preview/ThemePreview.kt` previews the token slots (the colour roles and the type scale) in light and dark, so the
skeleton is rendered from the first build. `@AppComponentPreviews` gives a component's preview every condition it
has to survive: light, dark, twice the font size, right to left and a 320 dp window, each inside `AppPreviewTheme`.
Put it on each component's preview, as the template above does; a component without one fails `component-preview`.
<!-- </opt:!ui_components> -->
<!-- <opt:ui_components> -->
Every component file in `:core:designsystem` ends with a private preview under `@AppComponentPreviews`, which renders
it in light and dark, at twice the font size, right to left and in a 320 dp window, each inside `AppPreviewTheme`;
give a component you add the same, in its own file.
<!-- </opt:ui_components> -->
Record and compare goldens with Roborazzi's Gradle tasks, and never re-record a golden to make a test pass.
