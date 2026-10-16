# Audit: `template/core/designsystem` + `template/core/ui`

Scope: every Kotlin file under `template/core/designsystem/**` (≈10,000 LOC main, 1 test) and
`template/core/ui/**`, compared file by file with the hand-improved copies in PeriFlow and VitFleet.
Compose BOM 2026.09.00 (foundation 1.12.x), Kotlin 2.4.20, minSdk 26. No Gradle was run. Framework
behaviour quoted below was checked against the foundation 1.12.1 sources jar in the Gradle cache.
The Kyant backdrop claims were checked against `io.github.kyant0:backdrop-android:2.0.1` sources.

Path abbreviations used throughout:

| Short | Path |
|---|---|
| `DS/` | `template/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` |
| `UI/` | `template/core/ui/src/main/kotlin/com/base/app/core/ui/` |
| `NAV/` | `template/core/navigation/src/main/kotlin/com/base/app/core/navigation/` |
| `PF/` | `periflow-android/core/designsystem/src/main/kotlin/com/jorwen/periflow/core/designsystem/` |
| `VF/` | `vitfleet-android/core/designsystem/src/main/kotlin/com/vitfleet/app/core/designsystem/` |

---

## 0. Executive summary

1. **A press overlay is applied to every touchable in the app, and that is the "ripple" the user hates.**
   `AppTheme` provides a custom `AppIndication` (a full-bounds grey rectangle at 9 % alpha) as
   `LocalIndication`. Every `appClickable` uses it, and so does every implicit `selectable` /
   `toggleable` overload. That covers the bottom bar, rail, tab row, segmented control, checkbox,
   radio and switch rows. Filled buttons get three press effects stacked: a scale spring, a
   pressed-colour crossfade and the grey overlay. Nothing in the system says which kind of feedback
   a component should use.
2. **The floating bottom bar is an opaque Material-style card, not a polished iOS bar.** It has no
   translucency or glass. Screen content never runs underneath it, because the nav host pads root
   screens by the bar's full height. Tab items flash the grey overlay. The pill, icon scale and
   colour animations all read state during composition, so the bar recomposes every frame of every
   tab switch, and that is the busiest moment the app has.
3. **There is no liquid glass in the template.** PeriFlow has a working Kyant-backdrop implementation
   (`GlassStage`, `Modifier.glass`, `GlassBar`) that contains real performance lessons: content glass
   is drawn rather than sampled, and the lens at rest samples only the tab layer. It also has fixable
   costs: per-frame coroutine launches, `composed {}`, no power-save fallback, and the tab row
   composed twice. Section 3 is a port-and-optimise plan.
4. **Motion is two systems that do not meet.** There are springs for press, sheet and navigation,
   and fixed tweens for nearly everything else. Tab indicators, switch thumbs, pager dots and the
   accordion chevron all reuse the *sheet* spring. `sheet()` hard-codes stiffness 460 for every motion
   style. `tracking()` is dead. The press "pop" does not show a dip on quick taps, which PeriFlow
   already fixed with a `Modifier.Node`.
5. **There are several real functional bugs.** Each is confirmed by reading the code, and most are
   also present unchanged in both apps:
   - `AppWheelPicker`'s settle collector captures `selectedIndex` and `onSelectedChange` at launch,
     so `AppTimePicker` resets the minutes when you change the hour.
   - A stepped `AppSlider` cannot be dragged slowly, and touching it to scroll sets its value.
   - `AppPullToRefresh` captures `isRefreshing` stale, so it can trigger a second refresh while one
     is running. VitFleet fixed this.
   - `AppCurrencyField`'s cursor drifts when a grouping comma appears: typing `1234567` gives
     `12,345.76`.
   - Back on `AppBottomSheet` removes the sheet with no exit animation.
   - `AppTooltip`'s pop-in never plays.
   - Tapping an open `AppSwipeToAction` row activates the row instead of closing it, which
     contradicts the code's own comment.
   - The `AppTopBar` title runs under a second action. VitFleet fixed this.
   - The dark-mode skeleton shimmer is invisible: the base and highlight are the same colour.
6. **Accessibility gaps:**
   - Unchecked controls, field outlines and the switch track are 1.1–1.7:1 against the surface
     (WCAG 1.4.11 asks for 3:1).
   - White text on the dark-theme danger colour is 3.08:1, in badges and the Destructive button.
   - Icon buttons are 36–44dp, and a chip's remove target is 15dp.
   - The text-field label and placeholder are not attached to the field's semantics.
   - `focusRing` is defined but never used, so the only focus cue is a 6 % overlay.
   - Every tap buzzes by default, which is not how iOS behaves.
7. **Weight and slop.** At least 2,681 LOC of components are used only by the catalog, yet ship in
   every generated app. Some APIs are dead: `bounceClick`, `AppResponsiveGrid`, `staggeredIn`,
   `tracking()`, `MessagingSurface`, `asStringOrEmpty`, `AppUserAvatar`, `focusRing`, and the
   `AppScaffold` `bottomBar` and `floatingAction` slots. There are cargo-cult settings
   (`includeFontPadding=false` with a deprecation suppression), and several comments are wrong:
   "Fifteen styles", "weights AppTypography actually asks for", "Clears the bottom bar",
   "Tapping an open row closes it".

---

## 1. Touch feedback, indication and press design

### 1.1 Inventory: where an indication or press effect is applied

| Site | Mechanism | What the user sees | Wanted (iOS-like) |
|---|---|---|---|
| `DS/theme/AppTheme.kt:66,87` | `LocalIndication provides AppIndication(resolved.contentPrimary)` | Global default for every clickable that does not pass `indication` | Default should be **no overlay** |
| `DS/foundation/AppIndication.kt:74-80` | `drawRect(color.copy(alpha))`, pressed 0.09, focus 0.06, hover 0.04, 90/160 ms tween | Grey or white rectangle over the full bounds | Keep only as an explicit "row highlight" |
| `DS/foundation/Modifiers.kt:125-128` `appClickable` | `clickable(indication = LocalIndication.current)` | Overlay on list rows, menu items, action-sheet rows, date cells, accordion, detail rows, stat tiles, section-header action, rating stars, select/phone/field-button fields | Rows: highlight. Everything else: none or dim |
| `DS/foundation/AppSurface.kt:84-100` `AppClickableSurface` | `pressScale` **and** `appClickable` (overlay) | Buttons, icon buttons, chips, FAB and clickable cards all scale and flash grey | One effect per component |
| `DS/component/button/AppButton.kt:100-104` | `animateColorAsState(containerPressed)` on top of the above | Primary: accent → accentPressed **plus** a 9 % black film **plus** a 0.96 scale with overshoot | Scale *or* darken, not three |
| `DS/component/navigation/AppBottomBar.kt:264-268` | `selectable(selected, role, onClick)` with no `indication` → foundation reads `LocalIndication` (verified in `foundation/selection/Selectable.kt:83`, which uses `composed`) | A **rectangular grey flash over the whole tab column**, and in the edge bars it spans the full bar height | None: the selection animation is the feedback |
| `DS/component/navigation/AppNavigationRail.kt:64-69` (reuses `BottomBarItem`) | same | same, on the rail | None |
| `DS/component/navigation/AppTabRow.kt:68-72` | `selectable` implicit | Grey rectangle per tab | None |
| `DS/component/selection/AppSegmentedControl.kt:87-92` | `selectable` implicit | Grey rectangle inside a segment, next to the sliding pill | None (or dim the label) |
| `DS/component/selection/AppSelection.kt:65-73, 149-157, 213-221` | `toggleable` / `selectable` implicit on `SelectionRow` | Whole checkbox, radio or switch row flashes grey (not clipped) | None |
| `DS/component/selection/AppChip.kt:85` | `clickableNoIndication` inside an indicated chip | none | ok |
| `DS/component/overlay/AppTooltip.kt:46-53` | `combinedClickable(indication = null, onClick = {})` | none (but see 6.8) | ok |

Root cause: there is no feedback taxonomy. "Indication" is a single global value, and press-scale
is a separate boolean flag (`scaleOnPress`) that only `AppClickableSurface` knows about.

### 1.2 Press-scale defects

* **Quick taps show a pop, not a press.** In `DS/foundation/Modifiers.kt:41-50` the Press event
  animates toward `scaleTo`, and Release *immediately* starts the overshoot to `pressOvershoot`.
  Inside a scrolling container, `clickable` delays the Press interaction until tap timeout. On a
  quick tap, Press and Release arrive together, so the control *grows* to 1.02–1.045 and never
  visibly dips. PeriFlow fixed exactly this
  (`PF/foundation/PressBounce.kt:92-100`): "A quick tap is over before the press has had time to
  show. Finish the dip first".
* **It is a `@Composable` modifier factory** (`Modifiers.kt:22-63`). Each instance costs a
  `remember { Animatable }`, a `LaunchedEffect` coroutine and a `graphicsLayer`. Every button, chip
  and day cell in a list pays this during composition. PeriFlow replaced it with
  `PressBounceNode : Modifier.Node, LayoutModifierNode` (`PF/foundation/PressBounce.kt:58-112`),
  which costs no composition at all.
* `pressPop()` (`DS/theme/AppMotion.kt:137-138`) is a critically damped spring *to* an overshoot
  value and then a second spring back. That is a scripted two-step, not a spring overshoot. PeriFlow
  replaced it with a single under-damped `release()` spring (`PF/theme/AppMotion.kt`:
  `releaseDamping` / `releaseStiffness`). Its bounce is proportional to how far the press travelled,
  so small and wide controls bounce alike.
* PeriFlow also added `compactPressScale` (0.84–0.95) for icon-sized controls. A 0.96 scale on a 36dp
  glyph is invisible under a finger.

### 1.3 Haptics on every tap

`appClickable(haptic: HapticEffect? = HapticEffect.Tap)` (`DS/foundation/Modifiers.kt:118,132-135`)
buzzes `KEYBOARD_TAP` on every button, row, card and menu item (`AppHaptics.kt:61`). iOS does not
play haptics on ordinary button taps; it reserves them for toggles, selection changes, detents and
success or error. The comment "Every control gets a haptic by default" documents the choice, but it
contradicts the iOS-quality goal.

### 1.4 Recommended design

Introduce one explicit feedback enum and make the default "nothing":

```kotlin
enum class PressFeedback { None, Scale, Dim, Highlight }

// AppTheme: no global overlay. Highlight is opt-in, used by rows and menus only.
LocalIndication provides NoIndication            // object : IndicationNodeFactory { ... draws nothing }

fun Modifier.pressable(
    onClick: () -> Unit,
    feedback: PressFeedback,
    role: Role? = Role.Button,
    enabled: Boolean = true,
    haptic: HapticEffect? = null,                 // default silent, like iOS
    interactionSource: MutableInteractionSource? = null,
): Modifier = this
    .then(PressEffectElement(interactionSource, feedback, enabled))   // Modifier.Node, see below
    .clickable(interactionSource, indication = if (feedback == PressFeedback.Highlight) RowHighlight else null, ...)

private class PressEffectNode(...) : Modifier.Node(), LayoutModifierNode {
    private val progress = Animatable(0f)         // 0 = rest, 1 = pressed
    // collect interactions as PeriFlow's PressBounceNode does, including the "finish the dip" rule.
    override fun MeasureScope.measure(m: Measurable, c: Constraints): MeasureResult {
        val p = m.measure(c)
        return layout(p.width, p.height) {
            p.placeWithLayer(0, 0) {
                val v = progress.value                       // read only in the layer block
                when (feedback) {
                    Scale -> { scaleX = lerp(1f, scaleTo, v); scaleY = scaleX }
                    Dim   -> { alpha = lerp(1f, 0.45f, v); compositingStrategy = CompositingStrategy.ModulateAlpha }
                    else  -> Unit
                }
            }
        }
    }
}
```

Mapping:

| Component | Feedback |
|---|---|
| Filled and outlined buttons, FAB, chips, glass buttons | `Scale` (no colour crossfade, no overlay) |
| Tertiary, Ghost and icon buttons, top-bar actions, links | `Dim` |
| List rows, menu items, action-sheet rows, settings rows, date cells | `Highlight`: instant on, 200–250 ms fade out, colour from a `pressedOverlay` token |
| Bottom-bar and rail items, tabs, segments, selection rows | `None`. Pass `indication = null` explicitly to `selectable` / `toggleable`. |
| Cards | `Scale` at 0.98, or `Highlight`. Never a grey film over images. |

Haptics: `None` by default. `Select` on segment, tab or wheel change. `Toggle` on switches.
`Threshold` and `Confirm` for pulls, swipes and commits.

---

## 2. Bottom bar, rail and tab components

### 2.1 Floating bar design quality (`DS/component/navigation/AppBottomBar.kt`)

* **It is opaque, not floating.** It is drawn as `AppSurface(shape = pill, elevation = overlay)`
  (`:139-150`): a solid `surface` with an 8dp shadow in light mode and a border in dark mode.
* **No content runs under it.** `NAV/AppNavHost.kt:154-162` pads every root entry by
  `AppBottomBarDefaults.occupiedHeight()`, which is 64 + 2×12 + nav bar, about 88dp + insets. The
  capsule floats over an empty strip of background, not over the feed. iOS (and PeriFlow's glass
  path, `LocalBottomBarInset` in `PF/foundation/Glass.kt:63`) lets the content scroll beneath and
  pads only the *scrolling content*.
* **The selected pill is nearly invisible in light mode.** It is `accentSubtle` on `surface`:
  `#E8EFFD` on white is **1.15:1**. Selection is carried almost entirely by colour.
* **The filled-glyph cue is promised but never delivered.** The KDoc at `:47-51` promises an outline
  to filled switch through `selectedIcon`. The icon set has no filled variants except `StarFilled`
  (`DS/icon/AppIcons.kt:342`), and no shell tab passes one.
* **The pill wobbles in Bouncy.** Its spring is `motion.sheet()` (`:164`), which has damping 0.68 in
  Bouncy. Icons in the edge bars scale 1.06 on the press spring (`:255-259`), which has damping 0.42
  in Bouncy. That is the "pushy" bounce on every tab change.
* Labels are 11sp `labelSmall`. The capsule height is fixed at 64dp (`:147`) and edge bars at 60dp
  (`:118`). Both clip at large font scales (see 8.4).
* The bar's show and hide is in `NAV/AppShell.kt:230`: `slideInVertically(motion.sheet())` from the
  full bar height. PeriFlow changed this to a short tween rising a fraction of its height, because "a
  long spring up from the edge read as the screen arriving twice"
  (`periflow .../core/navigation/AppShell.kt:290-295`).

### 2.2 Bottom bar performance (every frame of a tab switch)

| Line | Read in composition | Effect |
|---|---|---|
| `AppBottomBar.kt:162-169` | `val x by animateDpAsState(...)` then `.offset(x = x)` | `SlidingPill` recomposes and re-measures every frame |
| `AppBottomBar.kt:178-189` | same for `SlidingRule` | same |
| `AppBottomBar.kt:229-252` | `tint`, `labelTint`, `pill` via `animateColorAsState` (250 ms) | The old and new items recompose `AppIcon` (new `ColorFilter.tint`) and `AppText` (new merged `TextStyle`) every frame |
| `AppBottomBar.kt:255-259,284` | `iconScale` → `Modifier.scale(iconScale)` | Recomposes each frame (edge bars) |
| `AppNavigationRail.kt:88-100, 105-116` | `offset(y = y)` | same as the pill |

All of this happens while `NavDisplay` is composing and animating the incoming screen. Fix: read
animated values only in `offset { }`, `graphicsLayer { }` and `drawBehind { }` lambdas, and pass
colours as `ColorProducer` (see 5.2):

```kotlin
val x = animateFloatAsState(index.toFloat(), motion.selection(), label = "barPill")   // keep the State
Box(Modifier.offset { IntOffset((x.value * itemWidthPx).roundToInt(), 0) } ...)
AppIcon(icon, null, tint = { tint.value })            // ColorProducer overload, draw-phase only
```

### 2.3 `AppTabRow` and `AppSegmentedControl`

* The indicator position is read inside `layout {}` (`AppTabRow.kt:96-108`,
  `AppSegmentedControl.kt:61-68`), which is correct and deferred.
* Fixed tab height 46dp (`AppTabRow.kt:53,117`) and segment height 40dp
  (`AppSegmentedControl.kt:51`) are below 48dp and do not grow with font scale.
* Tabs are equal width only, so long labels ellipsize. VitFleet measures labels and switches to
  content-weighted widths, with the indicator lerping its width between tabs
  (`VF/component/navigation/AppTabRow.kt`). Adopt this.
* The selected tab uses `accent` as text. VitFleet found that a dark navy accent sits under 2:1 on
  the dark surface and switched to `contentPrimary` in dark (`selectedTabContent`). The generator's
  `_legible()` (`generator/genkit/render.py:738-748`) only guarantees `onAccent` on accent; it never
  checks accent-as-text on `surface` or `background`.
* The segmented control has no thumb shadow and `enabled=false` does not dim it. iOS segmented
  controls dim the label on press rather than flashing.

### 2.4 `AppTopBar`

* **The title overlaps the actions.** The title column is padded only by
  `titleEnd = edge + slot` (`AppTopBar.kt:70,87`), which is one 44dp slot. With two or more
  actions, the title and subtitle run underneath the second action. VitFleet fixed this by laying
  the title out in the Row with `weight(1f)` (`VF/component/navigation/AppTopBar.kt`, its comment
  reads "The title takes what the icons leave").
* There is no scroll-linked behaviour: no large-title-to-inline collapse and no scroll-edge blur or
  fade. PeriFlow has `scrollEdgeEffect` (`PF/foundation/ScrollEdge.kt:42-59`), which is measured
  cheap, and `LocalTopBarInset`. `showDivider` animates, but no component computes it from scroll
  state.
* `AppLargeTitle` is static, and `displaySmall` is 24sp. iOS large titles are 34pt and collapse.

---

## 3. Liquid glass (Kyant backdrop): port and optimisation plan

### 3.1 What PeriFlow does well (adopt)

* **Content glass is drawn rather than sampled** (`PF/foundation/Glass.kt:138-146`): "a live
  backdrop under every card and chip cost a blur pass each per frame (measured: two in three frames
  missed on a mid-range phone)". This is confirmed by the library: every `drawBackdrop` node uses a
  layer with `compositingStrategy = CompositingStrategy.Offscreen`
  (`kyant backdrop DrawBackdropModifier.kt:257`), so each live-glass element is its own offscreen
  render target plus a RenderEffect.
* **There is one `GlassStage` per shell.** Only the scene is recorded (`layerBackdrop(sceneLayer)`,
  `Glass.kt:88`), and only chrome samples it.
* **The lens at rest samples only the small tinted-tabs layer.** It bends the screen only while
  pressed (`PF/component/navigation/GlassBottomBar.kt:253-256`).
* **Springs have a settle threshold of `SETTLED = 0.004`**, so invisible wobble stops re-rendering
  the glass (`PF/foundation/LiquidInteraction.kt:116-124`).
* **There is a frosted fallback** when `!isRenderEffectSupported()` (API < 31) or when no stage
  exists, for example inside a dialog window (`Glass.kt:149-154`).
* **Root screens pad only their scrolling content** through `LocalBottomBarInset`
  (`periflow .../core/navigation/AppNavHost.kt:161-166`).

### 3.2 PeriFlow costs to fix before porting

| Where | Problem | Fix |
|---|---|---|
| `PF/foundation/LiquidInteraction.kt:178-183` `updateValue` | `animationScope.launch { valueAnimation.animateTo(...) }` once **per pointer event** | One long-lived drag coroutine. Feed targets through `Animatable.animateTo` from a single `snapshotFlow`/`Channel`, or `snapTo` while dragging and spring only on release. |
| `LiquidInteraction.kt:198-202` `updateVelocity` | Called from the animation's per-frame block, and launches *another* coroutine every frame | Compute the velocity scalar inline. Animate the velocity only on release. |
| `LiquidInteraction.kt:249` and `GlassBottomBar.kt:155` | `scope.launch { positionAnimation.snapTo(...) }` and `scope.launch { overscroll.snapTo(...) }` on every move event | Use plain `mutableFloatStateOf` for values that track the finger, and `Animatable` only to settle |
| `PF/foundation/Glass.kt:137` | `Modifier.glass` uses `composed {}`, the pattern Google discourages | `@Composable fun Modifier.glass(...)` reading the locals once, or a `ModifierNodeElement` using `currentValueOf(LocalGlassLayers)` |
| `GlassBottomBar.kt:224-239` | The whole tab row (badge, icon, text) is composed a **second time** at `alpha(0f)` only to feed `tabsBackdrop` | Record the accent copy of the *same* row: draw the visible row into a `GraphicsLayer` once and replay it tinted (`ColorFilter.tint(accent)`) into `tabsBackdrop`, or draw the icons only (no text) |
| `GlassBottomBar.kt:137-158` | `remember(scope, tabsCount)` captures `tabWidth` and `isLtr` in the drag lambdas | Key on `tabWidth` and `isLtr`, or read them through `rememberUpdatedState` |
| `GlassBottomBar.kt:182,257` | `shape = { Capsule() }` allocates per call. `Capsule` overrides `equals`, so the outline cache still hits, but it is garbage per frame. | `private val BarShape = Capsule()` |
| whole implementation | No degradation under Battery Saver, low-RAM devices or thermal throttling | See the 3.3 policy |

### 3.3 Proposed generator design

* **A new optional feature `glass`** (marker `// <opt:glass>`) that adds
  `io.github.kyant0:backdrop:2.0.1` and `io.github.kyant0:shapes:1.2.1`. Both are stable and
  Apache-2.0; an entry goes in the Licenses screen. The feature adds a `GlassChrome` treatment that
  is **orthogonal to `design_style`**: any style can have glass chrome (tab bar, top-bar buttons,
  sheets), plus a `Glass` design style that also turns on drawn content glass.
* **Files** (all under `DS/foundation/glass/`): `GlassStage.kt`, `GlassPolicy.kt`, `Glass.kt` (with
  `drawnGlass` and chrome glass), `LiquidDrag.kt`. Navigation also gets `GlassTabBar.kt`, plus
  `LocalBottomBarInset` / `LocalTopBarInset` in the design system so lists can pad.
* **`GlassPolicy`** decides live, frosted or solid once, as state, and re-evaluates on change:

```kotlin
@Immutable enum class GlassMode { Live, Frosted, Solid }

@Composable fun rememberGlassMode(userPref: GlassPref): GlassMode {
    val context = LocalContext.current
    val power = remember { context.getSystemService(PowerManager::class.java) }
    val am = remember { context.getSystemService(ActivityManager::class.java) }
    var saver by remember { mutableStateOf(power.isPowerSaveMode) }
    var hot by remember { mutableStateOf(false) }
    DisposableEffect(power) {                       // ACTION_POWER_SAVE_MODE_CHANGED + thermal listener (API 29+)
        val listener = PowerManager.OnThermalStatusChangedListener { hot = it >= PowerManager.THERMAL_STATUS_SEVERE }
        power.addThermalStatusListener(context.mainExecutor, listener)
        /* register receiver for saver */ onDispose { power.removeThermalStatusListener(listener) }
    }
    return when {
        userPref == GlassPref.Off -> GlassMode.Solid
        Build.VERSION.SDK_INT < 31 || am.isLowRamDevice || saver || hot -> GlassMode.Frosted
        else -> GlassMode.Live
    }
}
```
* **Live mode:** blur 8–10dp, plus `lens` only on API 33+ (Kyant's `isRuntimeShaderSupported()` is
  `SDK_INT >= TIRAMISU`). There is exactly one sampling element per visible chrome piece. The lens
  samples the tab layer at rest and the scene only while `pressProgress > 0`. Every `Animatable`
  feeding glass gets a visibility threshold.
* **Content glass is never live**. Use `drawnGlass` as PeriFlow does. Use `drawWithCache` so the
  radial-gradient `Brush` and the rim `Brush` are not rebuilt per draw (PeriFlow allocates both in
  `drawnGlass`, `Glass.kt:185-201`), and cache the `outerShadow` `Path`/`Paint` (`Glass.kt:210-225`
  allocates a `Path` and a framework `Paint` every draw).
* **Root screens draw under chrome:** `LocalBottomBarInset` replaces the padding in
  `NAV/AppNavHost.kt:154-162` *for every bar treatment*. The floating solid bar benefits too.
* Add a **settings toggle "Reduce transparency"** and fold it into `GlassPref`, the equivalent of the
  iOS accessibility setting.
* **Acceptance gate:** a Macrobenchmark `FrameTimingMetric` on "scroll the feed under the bar" and
  "drag the lens across tabs". The template already has an optional `benchmark` module with a
  macrobenchmark (`generator/genkit/spec.py:299-305`, `template/benchmark/`), so the journeys can go
  there.

---

## 4. Motion system (`DS/theme/AppMotion.kt`, `DS/animation/AppTransitions.kt`)

### 4.1 Coherence

| Token | Definition | Used for | Problem |
|---|---|---|---|
| `press()` | spring(pressDamping, pressStiffness) | press scale, radio dot, slider thumb radius, `popIn`, nav icon scale | Bouncy damping 0.42 also makes radio dots and nav icons wobble |
| `pressPop()` | critically damped to the overshoot | release | Two-step script, see 1.2 |
| `sheet()` | `spring(sheetDamping, stiffness = 460f)` **hard-coded** (`AppMotion.kt:145-146`) | sheets, **tab and bar pills, rail pill, switch thumb, segmented pill, pager dots, accordion chevron, dialog and menu scale** | The semantic name is wrong for most uses, and Snappy and Calm move these at identical speed |
| `navigation()` | spring(navDamping, navStiffness) | nav, **LazyList `placementSpec`** (`AppTransitions.kt:175`), sparkline and bar-chart grow | Placement reuses the screen-travel spring |
| `tracking()` | spring(no bounce, 900) | **nothing** (no usages in the template) | Dead |
| durations `instant/quick/medium/slow` | tweens | colour changes, fades, FAB expand, snackbar, accordion, counter, progress, sheet *dismiss*, swipe commit | Fine for colour and opacity. Spatial moves use tweens in many places (sheet dismiss `AppBottomSheet.kt:72`, snackbar `AppSnackbar.kt:106-113`, FAB `AppFloatingActionButton.kt:70-73`, accordion `AppTransitions.kt:60`, date-picker month slide `AppDatePicker.kt:109-113`), so they cannot carry gesture velocity and do not retarget smoothly |
| `outgoingParallax`, `outgoingDim`, `behindSheetScale` | vals inside `AppMotion` | `NAV/NavTransitions.kt` | ok |

### 4.2 Recommended token shape (SwiftUI-style, cheap and coherent)

Express springs as SwiftUI does, as *(perceptual duration, bounce)*, and derive Compose's
parameters from them. With mass 1, `stiffness = (2π/duration)²` and `dampingRatio = 1 − bounce`.
Precompute the specs once per motion style instead of allocating a new `SpringSpec` per recomposition
as `press()` and `sheet()` do now:

```kotlin
@Immutable
class AppSprings(style: AppMotionStyle) {
    val press     = spec(0.16f, 0.00f)   // finger down: fast, no bounce
    val release   = spec(0.32f, style.bounce)            // Bouncy .35, Standard .15, Calm 0, Snappy .1
    val selection = spec(0.30f, style.bounce * 0.6f)     // pills, indicators, thumbs, dots
    val spatial   = spec(0.42f, 0f)                      // sheets, navigation, list placement
    val emphasis  = spec(0.50f, style.bounce)            // pop-ins, badges, success
    private fun spec(d: Float, b: Float) = spring<Float>(1f - b, (2 * PI / d).pow(2).toFloat(), visibilityThreshold = 0.001f)
}
```
Keep tweens for colour and opacity only. Each style changes `bounce` and a duration multiplier, not
twelve unrelated numbers.

### 4.3 Reduce motion

* `rememberReduceMotion()` (`DS/theme/ReduceMotion.kt:10-21`) reads
  `Settings.Global.TRANSITION_ANIMATION_SCALE` once per `Context` and never observes it. Toggling
  "Remove animations" has no effect until the activity is recreated. Use a `ContentObserver` on
  `Settings.Global.getUriFor(ANIMATOR_DURATION_SCALE)` exposed as state, one instance provided from
  `AppTheme` as a `CompositionLocal`. As written, every caller re-queries `Settings`.
* Compose's own animations already honour `ANIMATOR_DURATION_SCALE` through
  `MotionDurationScale`, so the explicit checks matter mostly for content swaps. They are missing in
  `AppSkeleton` (shimmer keeps running), `AppCircularProgress`, `AppOtpField`'s caret, the bottom-bar
  pill and `AppAnimatedCounter` (that one is covered).

### 4.4 Smaller motion findings

* `STAGGER_OFFSET_PX = 24` is **pixels** (`AppTransitions.kt:136`), while `APPEAR_OFFSET` is 12.dp.
  Stagger travel is about 8dp on xxxhdpi and 24dp on mdpi.
* `staggeredIn()` (`AppTransitions.kt:123`) has no callers. `AppStaggeredColumn` reimplements it.
* `AppAnimatedCounter` builds one `AnimatedContent` plus a `SizeTransform` per digit
  (`AppAnimatedCounter.kt:60-79`), which is heavy inside list rows.
* `AppWheelPicker` swaps `headingSmall` and `bodyMedium` at the centre (`AppWheelPicker.kt:151-160`).
  The text re-lays out and jumps in size, fighting the smooth `graphicsLayer` scale. iOS keeps one
  font and transforms only.

---

## 5. Compose performance anti-patterns

### 5.1 State read in composition that belongs in layout or draw

| File:line | Read | Fix |
|---|---|---|
| `AppBottomBar.kt:169,185,284` | `offset(x = x)`, `Modifier.scale(iconScale)` | `offset { }`, `graphicsLayer { }` |
| `AppNavigationRail.kt:95,111` | `offset(y = y)` | `offset { }` |
| `AppBottomSheet.kt:81-85` | `offsetY.value` computed into `progress` **in composition** and then captured by `graphicsLayer { alpha = progress }` | The whole sheet `Box` content recomposes every frame of open, drag and dismiss. Compute it inside the lambda. |
| `AppDialog.kt:60-82`, `AppDropdownMenu.kt:48-64` | `Modifier.scale(scale).alpha(alpha)` with animated values | `graphicsLayer { scaleX = ...; alpha = ... }` |
| `AppPullToRefresh.kt:98-99` | `offset.value` read in composition for `progress` and `if (offset.value > 0f)` | `derivedStateOf { offset.value > 0f }`, and progress inside lambdas |
| `AppSwipeToAction.kt:161` | `if (offset.value != 0f)` in composition: the whole row recomposes during the drag | `derivedStateOf` |
| `AppOtpField.kt:70-79,175` | An **infinite** caret transition created always. `.alpha(caretAlpha)` in composition recomposes the box at 60–120 Hz while the field is focused, and the transition keeps the frame clock running for the screen's lifetime even when unfocused. | Create the transition only inside the `isNext` branch, and use `graphicsLayer { alpha = caretAlpha }` |
| `AppButton.kt:93-118` | `container` (colour tween) and `sink` read in composition and passed to `AppClickableSurface` / `offsetShadow` | The button recomposes on every press frame. Draw the container in `drawBehind { drawOutline(..., color = container.value) }`. |
| `AppChip.kt:40-63`, `AppTextField.kt:76-91`, `AppSelection.kt:46-55,137-141,196-205` (track only), `AppStepIndicator.kt:94-118`, `AppFieldButton` (`AppSpecialFields.kt:419-423`) | Colours from `animateColorAsState` passed as plain `Color` params | Acceptable individually. Fix once with `ColorProducer` overloads (5.2). |
| `AppListPieces.kt:95` | `Modifier.rotate(rotation)` | `graphicsLayer { rotationZ = ... }` |
| `AppListPieces.kt:228` | `.width(width)` animated per dot | `layout {}` or a draw-based indicator |
| `AppTransitions.kt:237-244` | `busyOverlay` uses `Modifier.alpha(animated)` in composition | `graphicsLayer { alpha = ... }` |

Already done correctly, for reference: `AppTabRow`, `AppSegmentedControl` (layout lambda), `AppSwitch`
(Canvas draw), `AppWheelPicker` (`graphicsLayer`), `AppAppear`, `AppStaggeredColumn` and
`AppBottomSheet`'s sheet translation.

### 5.2 Text and icon cannot defer colour

`AppText` (`DS/component/text/AppText.kt:21-71`) has only a `Color` parameter. It also merges a new
`TextStyle` every recomposition (`resolve()`, `:73-91`). `BasicText` accepts `color: ColorProducer`,
which updates in the draw phase. Add `AppText(text, color: ColorProducer, …)`. Also add
`autoSize: TextAutoSize?`, which both PeriFlow and VitFleet added independently
(`PF/component/text/AppText.kt`, `VF/component/text/AppText.kt`).

`AppIcon` (`AppIcon.kt:17-34`) creates `ColorFilter.tint(tint)` per recomposition. Its
`clearAndSetSemantics {}` when `contentDescription == null` is redundant, because `Image` adds no
semantics in that case. Provide a `tint: () -> Color` variant drawn with
`Modifier.paint`/`drawBehind`.

### 5.3 Modifier factories and allocation

* There are nine `@Composable` modifier factories: `pressScale`, `bounceClick`, `appClickable`,
  `disabledAlpha`, `busyOverlay`, `appAnimateItem`, `fieldContainer`, `cardShadow` and
  `touchOnFocusLost` (`UI/form/FormState.kt:154`). `disabledAlpha` (`Modifiers.kt:140-142`) reads
  nothing composable at all. `appClickable` eagerly allocates
  `remember { MutableInteractionSource() }` (`Modifiers.kt:119`), although foundation 1.7+ creates
  one lazily when `null` is passed (as `clickableNoIndication` already does, `:99-100`).
* `AppClickableSurface` stacks five or six layers per button (`AppSurface.kt:84-100`): `alpha`
  (when disabled), the `pressScale` `graphicsLayer`, `shadow`, `clip`, `background(shape)` after
  `clip` (the shape is applied twice), `border`, `clickable` and the indication node, plus two
  coroutines per instance (the press `LaunchedEffect` and the indication collector). A feed row with
  four chips and buttons runs eight coroutines and builds four layers while scrolling. Moving to the
  §1.4 node removes one coroutine and the composition cost per control.
* `AppSurface` always applies `.clip(shape)` (`AppSurface.kt:47`), which creates a layer even for
  cards whose content cannot overflow. Make clipping opt-in (`clipContent: Boolean = false`).
* `offsetShadow` (`DS/foundation/StyleModifiers.kt:15-19`) calls `shape.createOutline` on every draw.
  Use `drawWithCache`.
* `AppSkeleton` (`AppSkeleton.kt:43-71`): each placeholder has **its own** `InfiniteTransition`.
  Because `progress` is read inside the `drawWithCache` *cache* block, the cache is invalidated every
  frame and a new `Brush.linearGradient` (and `listOf`) is built per skeleton per frame. A list of
  ten `AppSkeletonListItem`s runs 30 transitions and allocates 30 brushes per frame. Fix: build the
  brush once in the cache, read `progress` only in `onDrawBehind`
  (`translate(left = …) { drawRect(brush) }`), and share one timeline through
  `LocalShimmer`/`staticCompositionLocalOf` so all placeholders sweep in sync.
* `AppCheckbox` allocates two `Path`s and a `PathMeasure` on every draw while checked
  (`AppSelection.kt:101-111`), not only while animating. Cache them in `drawWithCache`.
* `AppAvatar.initials()` compiles `Regex("\\s+")` on every recomposition (`AppAvatar.kt:53`). Hoist
  it to a top-level `val`.
* `AppIcons` is an `object` with 67 eager `val`s plus an `all` list (`AppIcons.kt:515-536`). The
  first access builds every vector, on the main thread, during the first frame. Use lazy backing
  fields, as material-icons does. The `all` list is catalog-only and should move to the catalog.
* `AppNetworkImage` uses `SubcomposeAsyncImage` (`UI/AppNetworkImage.kt:34-41`). Coil documents it
  as slower and unsuitable for lazy lists. Each loading image also starts its own skeleton infinite
  transition. Use `AsyncImage` with a `placeholder`/`error` painter, or `rememberAsyncImagePainter`
  plus a state switch.
* `AppPagingList` creates `rememberLazyListState()` inside the `else` branch
  (`UI/paging/AppPagingList.kt:66`). The state cannot be hoisted, so scroll-to-top on tab reselect
  is impossible, and the state is lost when the list passes through the empty or error branch.

---

## 6. Correctness bugs (confirmed by reading code and framework source)

### 6.1 `AppWheelPicker`: the stale collector resets other wheels (high)
`DS/component/datetime/AppWheelPicker.kt:75-81`:
```kotlin
LaunchedEffect(listState, items, placed) {            // selectedIndex, onSelectedChange not keys
    snapshotFlow { listState.isScrollInProgress to centeredIndex }
        .collect { (scrolling, index) -> if (!scrolling && index != selectedIndex) onSelectedChange(index) }
}
```
`selectedIndex` and `onSelectedChange` are captured when the effect launches.
1. After the first change, the wheel cannot report a return to the original index (`index != staleIndex` is false).
2. `AppTimePicker` (`AppTimePicker.kt:257-291`) passes lambdas that close over the *current*
   `minuteIndex` and `periodIndex`. The hour wheel keeps the first-composition lambda, so after a
   minute change, changing the hour emits `emit(hour, minutes[initialMinuteIndex], initialPeriod)`
   and the minutes **snap back**. The same goes for AM/PM.

The `centeredIndex` `derivedStateOf` at `:53-64` also captures `selectedIndex` and `items` inside an
unkeyed `remember`. The code is identical in PeriFlow and VitFleet.
Fix: `val current by rememberUpdatedState(selectedIndex)` and
`val onChange by rememberUpdatedState(onSelectedChange)`, read inside the collector, and key the
`derivedStateOf` on `items`.

### 6.2 `AppSlider`: stepped drag is stuck and touch-to-scroll sets the value (high)
`DS/component/selection/AppSlider.kt:127-145`:
* `detectTapGestures(onPress = { emit(it.x) })`. Foundation's `processTapGesture` consumes the down
  and calls `onPress` **immediately on down** (`gestures/TapGestureDetector.kt:132-136`). Starting a
  vertical scroll on a settings screen with the thumb under the finger changes the slider's value.
* The drag computes `emitFraction(fraction + delta / travel)` (`:138`), where `fraction` is the
  last *composed* value. With `steps > 0`, `fraction` is snapped, and any per-event delta smaller
  than half a step rounds back to the same value, so a slow drag never moves. For example, with
  steps = 4 on a 300dp track, one move event has to exceed about 30dp. Without steps, the second of
  two events arriving between recompositions is dropped.
* `AppRangeSlider` already does this correctly by mapping absolute positions in `awaitEachGesture`.
  `AppSlider` is identical in both apps.

Fix: mirror `AppRangeSlider`. Track an absolute `fractionAt(change.position.x)`, snap on output,
and emit on down only after touch slop, or on tap-up.

### 6.3 `AppPullToRefresh`: stale `isRefreshing` and the per-event `snapTo` launch (medium)
`DS/component/refresh/AppPullToRefresh.kt:50-86`: the `NestedScrollConnection` is
`remember(thresholdPx, maxPullPx)`, but it reads the `isRefreshing` *parameter* at `:66` and `:76`.
It is forever the first value (`false`), so pulling again during a refresh calls `onRefresh()` a
second time. Each scroll event launches `scope.launch { offset.snapTo(...) }` (`:57,68-70`), and the
consumed amount is computed from a value the previous launch has not applied yet.
`offset.animateTo(restingPx)` inside `onPreFling` suspends the fling for the whole animation (`:79`),
although the comment at `:83-84` says the fling should carry. Only the indicator moves, never the
content, which is Material style rather than iOS. **VitFleet fixed all of this**
(`VF/component/refresh/AppPullToRefresh.kt`): `rememberUpdatedState(isRefreshing)`,
`mutableFloatStateOf` while dragging with `Animatable` only to settle, the content translated by the
pull, and a minimum visible acknowledgement. Port it.

### 6.4 `AppCurrencyField`: the cursor drifts when a comma appears (medium)
`DS/component/input/AppSpecialFields.kt:278-305` with `formatMinorUnits` (`:314-321`). Value-based
`BasicTextField(String)` keeps the old selection index on an external value change
(`foundation/text/BasicTextField.kt:819-831`: `textFieldValueState.copy(text = value)`). When
`123.45` becomes `1,234.56` the text grows by two characters but the caret advances by one. A
simulation of typing `1234567` with the caret at the end:
```
typed 6: shown '1,234.56'   cursor 7/8
typed 7: shown '12,345.76'  cursor 8/9      <- expected 12,345.67
```
The placeholder also never shows, because the value is never empty (`"0.00"`). Fix: the
`TextFieldState` API with an `OutputTransformation` for grouping (digits stored raw), or the
`TextFieldValue` overload with `TextRange(text.length)`.

### 6.5 `AppBottomSheet`: back and double-dismiss (medium)
`DS/component/overlay/AppBottomSheet.kt:50-56`: `Dialog(onDismissRequest = onDismissRequest)`, so
Back calls the caller directly and the sheet **vanishes without the exit animation**. Drag and scrim
taps do animate (`:68-76`). `dismiss()` can also run twice (scrim double tap) and call
`onDismissRequest` twice. The dismiss is a tween that ignores fling velocity, and the settle-back
spring does not receive `initialVelocity` (`:121-127`). There is no nested scroll, so a scrollable
sheet cannot be dragged down from its top, and there are no detents.
VitFleet added nested scroll, IME-correct insets (`decorFitsSystemWindows = false`) and a close
button. PeriFlow has `AppDetentSheet` with a hoisted `DetentSheetState` that can hide with animation
before removal (`PF/component/overlay/AppDetentSheet.kt`). Merge both: route Back through
`dismiss()`, guard against re-entry, use a velocity-aware spring, and add medium/large detents.

### 6.6 `AppDialog` and `AppDropdownMenu`: no exit, centre-origin scale (medium)
`AppDialog.kt:49-94` and `AppDropdownMenu.kt:39`: the content is removed the instant the caller
flips state, with no exit transition. The menu scales from its centre (`:63`). iOS menus grow from
the anchor (set `transformOrigin` from the anchor side). The dialog relies on the platform dim with
no custom scrim, so its colour is inconsistent with `AppBottomSheet`'s scrim token.

### 6.7 `AppSwipeToAction`: the comment is false, and RTL is wrong (medium / low)
`DS/component/list/AppSwipeToAction.kt:158-168` says "Tapping an open row closes it rather than
activating whatever is under the finger". The close handler is a `clickableNoIndication` on the
*parent* Box. Pointer events reach children first (Main pass), and a clickable child (an
`AppListItem` with `onClick`) consumes the up, so the row's own action fires. Gate the content with
`Modifier.pointerInput(PointerEventPass.Initial)` consuming taps while open. Also:
* The actions are placed with `Arrangement.End` (`:92`), which mirrors in RTL, while `translationX`
  (`:123`) and the clamp `coerceIn(-revealPx, 0f)` (`:130`) do not. In RTL the content slides away
  from the actions.
* There are no `customActions` semantics, so TalkBack users cannot reach the hidden actions.
* `SwipeAction.contentColor = Color.White` is the default (`:51`).
* Each drag event launches `scope.launch { offset.snapTo(...) }` (`:126-132`).

### 6.8 `AppTooltip`: the enter animation never runs, and an empty action is exposed (low)
`AppTooltip.kt:65-69`: `AnimatedVisibility(visible = true, enter = popIn, exit = popOut)` is
composed only while `visible`. An `AnimatedVisibility` that starts visible does not run `enter`, and
`exit` never runs because the `Popup` is removed. Use `MutableTransitionState(false)`. The wrapper's
`combinedClickable(onClick = {})` (`:46-53`) adds a click action that does nothing to the wrapped
element's semantics, so TalkBack announces "double tap to activate" for nothing.

### 6.9 `AppTopBar`: the title runs under a second action (medium)
See 2.4. `AppTopBar.kt:65-70,83-108`. Fixed in VitFleet.

### 6.10 Dark-mode skeleton shimmer is invisible (medium)
`AppSkeleton.kt:36-41` uses `highlight = if (isLight) surface else surfaceVariant`. In
`DarkColors` both `skeleton` and `surfaceVariant` are `Ink700` (`AppColors.kt:115,151`), so the
gradient is `Ink700 → Ink700 → Ink700`. It is invisible, and it still allocates a brush every frame
(5.3). The Paper and Cream dark tones are within 1–2 % of each other too (`StyleTones.kt:55,
71-81, 95-105`). Use a dedicated `skeletonHighlight` token.

### 6.11 `AppTimeline` connector has a fixed height (low)
`AppListPieces.kt:285-291,342`: `CONNECTOR_HEIGHT = 44.dp` regardless of the row's content. A
two-line description leaves a gap, and a one-line title overshoots. Draw the connector in
`drawBehind` of the row, or use `IntrinsicSize.Min`. The white check on the dark success colour
(`:279`) is 1.89:1.

### 6.12 `AppScaffold` slots that nobody uses and that misplace the FAB (low)
`AppScaffold.kt:38-41,68-81`: `bottomBar` and `floatingAction` have **no callers** in the template.
The FAB box is aligned to the bottom of the *outer* Box with 16 + 8dp padding. The comment says it
"Clears the bottom bar when there is one", but a 60dp bar inside the same Column would be
overlapped. No navigation-bar inset is applied either, so on three-button navigation the FAB sits on
the nav buttons. Either delete the slots, or use `WindowInsets.safeDrawing` plus the measured bar
height. PeriFlow rewrote this scaffold with `SubcomposeLayout` and `LocalTopBarInset`
(`PF/component/container/AppScaffold.kt`).

### 6.13 `MessageHost` is not lifecycle-aware (medium)
`UI/MessageHost.kt:26-28` collects a `Channel.receiveAsFlow()` (`core/common/.../MviViewModel.kt:46-47`)
in a plain `LaunchedEffect`. Messages sent while the app is in the background are consumed and time
out unseen. `MviScreen` already uses `repeatOnLifecycle` for effects (`UI/MviScreen.kt:46-53`), so
do the same here. Also: a new message replaces the visible one without a transition, and there is no
queue and no swipe-to-dismiss.

### 6.14 Snackbar double-insets on root tabs (plausible; confirm on device)
`AppSnackbarHost` adds `navigationBarsPadding()` (`AppSnackbar.kt:116-120`), but root entries are
already padded by `occupiedHeight()`, which *includes* the navigation bar
(`AppBottomBar.kt:81-86`, `NAV/AppNavHost.kt:162`). That padding does not consume the insets, so
the snackbar should float about a nav-bar height too high on tab roots.

---

## 7. Theme and token architecture

**Strengths:**
- Semantic colour roles with status triads (`AppColors.kt:10-66`).
- Window-aware layout tokens (`AppWindowSize.kt`).
- Style and motion choices as data (`AppDesignStyle.style()`, `AppMotionStyle.motion()`).
- A palette contrast test.
- The generator derives accent ramps and legible `on*` colours (`render.py:655-830`).
- Static locals for rarely changing tokens.

**Problems:**
* **There is no state-layer or interaction token set.** The pressed overlay colour is
  `contentPrimary` (`AppTheme.kt:66`). `accentPressed`, `secondaryPressed` and `tertiaryPressed`
  exist, but the generic pressed, highlight and hover fills do not.
* **`focusRing` is never used** (`AppColors.kt:51`; zero usages outside the file). The only focus
  indication is the 6 % overlay (`AppIndication.kt:84`), which is unusable for keyboard, D-pad and
  switch access.
* **`AppSizes` is documented as "Minimum interactive sizes"** (`AppMetrics.kt:60`) but holds icon
  sizes, bar heights and border widths. `borderWidth` is duplicated between `AppStyle` and
  `AppSizes`, and `AppTheme.kt:54-56` copies one into the other.
* **There is no elevation colour model for dark mode** (iOS uses elevated backgrounds), only
  surface and surfaceVariant. `CardTreatment.Raised` claims "a lighter surface in dark"
  (`AppDesignStyle.kt:105`), but it simply gets `surface`.
* **`TypeVoice.Bold` and `Rounded` ask for `FontWeight.Black`** (`StyleTones.kt:112-113,134-136`),
  but `AppFonts` downloads only up to ExtraBold (`AppFonts.kt:37-44`, whose comment reads "The
  weights AppTypography actually asks for"). Font matching falls back to 800, so `Bold` voice's
  display styles are **identical** to `Neutral`'s, which are already ExtraBold (`AppTypography.kt:73-74`).
* **The `AppTypography` KDoc says "Fifteen styles"** (`AppTypography.kt:13`). There are 17 fields.
* **`@Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)`**
  (`AppTypography.kt:47-48`) is cargo cult. `false` has been the default since Compose 1.5.
* **The Typography `display*` sizes (32/28/24) are small for "Apple-level".** iOS uses Large Title
  34, Title1 28, Title2 22, Title3 20, Headline 17, Body 17, Callout 16, Subhead 15, Footnote 13,
  Caption 12/11. The template's body is 14 (`bodyMedium`) and the default text style is `bodyMedium`
  (`AppTheme.kt:85`). Consider aligning with the iOS Dynamic Type scale, which also simplifies the
  planned SwiftUI generator. A shared token JSON could feed Compose, CMP and SwiftUI alike.
* **Dynamic colour (Material You) is absent by design.** That is fine for an iOS-flavoured system.
  Offer it as an opt-in `dynamic_accent` that maps `system_accent1_*` onto the accent ramp on API 31+.
* **`AppDivider` is a 1dp `Box` per divider** (`AppDivider.kt:17-32`), three physical pixels on
  xxhdpi. iOS separators are 1 physical pixel. Use `Dp.Hairline` or `1 / density`, drawn with
  `drawBehind` on the row rather than as an extra layout node.

---

## 8. Accessibility

### 8.1 Contrast (computed with WCAG relative luminance)

| Pair | Ratio | Where | Requirement |
|---|---|---|---|
| White on `DangerDark #F0666D` | **3.08** | `AppBadge.kt:37` (badge text 11sp), `AppButton.kt:233-238` (Destructive, 15sp) | 4.5 (text) |
| White on `SuccessDark #3DD4A0` | **1.89** | `AppListPieces.kt:279` (timeline check) | 3.0 (non-text) |
| `borderStrong #CBD1DB` on white | **1.53** | checkbox and radio ring (`AppSelection.kt:52,138`), switch track border | 3.0 |
| `Ink500` on `Ink800` (dark) | **1.71** | same | 3.0 |
| `border #E3E6EC` on white | **1.25** | text-field outline (`AppTextField.kt:82`) | 3.0 |
| `surfaceVariant` on white | **1.13** | switch off track, slider track | 3.0 |
| `accentSubtle` on surface | **1.15** | floating-bar selected pill | (selection cue) |

`PaletteContrastTest.kt:29-50` checks only text-on-fill pairs from the palette. It does not check
hard-coded `Color.White` usages, accent-as-text on surface or background, or non-text 3:1 pairs. Add
those checks, and have the generator run the same checks on the user's accent. PeriFlow already
switched Destructive to red-on-red-tint (`PF/component/button/AppButton.kt`: "white text disappears
into" the dark coral).

### 8.2 Touch targets
* `AppClickableSurface` passes `minTouchTarget = 0.dp` (`AppSurface.kt:99`). As a result:
  - `AppIconButton` Medium is 44dp and Small is 36dp (`AppButton.kt:183`, `AppMetrics.kt:67-68`).
    Small is used for the password reveal, search clear, stepper, date-picker month arrows and tag add.
  - Chips are 34dp (`AppChip.kt:58`).
  - Segments are 40dp.
  - Tabs are 46dp.
* A chip's **remove "×" is a 15dp `clickableNoIndication` nested inside the chip's own clickable**
  (`AppChip.kt:80-87`). Compose's hit-test expansion does not apply, because the parent is a direct
  hit, so taps near the × select the chip instead.
* A checkbox, radio or switch without a `label` is a 22dp or 46×28dp target (`AppSelection.kt:252-275`).

Provide a foundation-only `Modifier.minimumTouchTarget()` (a layout modifier that reserves 48dp and
centres the content) and use it in `AppIconButton`, chips and selection controls.

### 8.3 Semantics
* **The `AppTextField` label and placeholder are siblings of `BasicTextField`**
  (`AppTextField.kt:97-126`), so they are not in its semantics. TalkBack announces "Edit box". Put
  label, placeholder, leading and trailing into `decorationBox`. That merges them and also makes the
  whole container focus the field; today the horizontal padding is dead.
* **Settings toggles have no label and a small target.** `AppListItem(trailing = { AppSwitch(...) })`
  (`feature/settings/.../Settings.kt:270-275,309-314,332-337`) produces an unlabelled
  "Switch, off" plus a separate title node, and only the 46×28 switch toggles. Add an
  `AppToggleRow` or `AppListItem(toggle = ...)` that puts `Modifier.toggleable(role = Switch)` on
  the whole row.
* `AppOtpField` boxes are unlabelled clickables (`AppOtpField.kt:160`), and there is no SMS autofill
  `contentType`.
* `AppDatePicker` day cells read as bare numbers, without the date or selected state
  (`AppDatePicker.kt:231-264`).
* `AppAccordion` has no expanded or collapsed state description (`AppListPieces.kt:65-97`).
* `AppAvatar` sets `contentDescription = name` and also leaves the initials as a child node
  (`AppAvatar.kt:30-45`).
* `AppSwipeToAction` hides its actions from TalkBack (6.7).

### 8.4 Font scaling
Text-bearing containers with fixed heights do not grow with font scale:
- top bar 56dp with title and subtitle (`AppTopBar.kt:81`)
- floating bar 64dp and edge bar 60dp (`AppBottomBar.kt:118,147`)
- rail item 68dp (`AppNavigationRail.kt:119`)
- tabs 46dp and segments 40dp
- wheel items 40dp (`AppWheelPicker.kt:42`)
- OTP boxes 48dp

At a 2.0× scale, the bar's 11sp/14sp label needs about 28sp of line height beside a 24dp icon,
which is more than the 52dp inner height of the floating bar. Use `heightIn(min = …)` and let the
bar grow, or cap only the label's scale.

### 8.5 RTL
* No `ImageVector` sets `autoMirror` (`AppIcons.kt:543-550`), so `ArrowLeft` and `ChevronLeft` point
  the wrong way in RTL (back buttons, the month header).
* `AppSwipeToAction` is wrong in RTL (6.7).
* The `AppDatePicker` month slide is not mirrored.

---

## 9. Dark mode

* Lifted surfaces swap their shadow for a border in dark (`AppSurface.kt:36-49`). That is
  reasonable, but it causes the floating bar's border in dark mode.
* There are hard-coded `Color.White` foregrounds in `AppBadge.kt:37`, `AppButton.kt:236`,
  `AppListPieces.kt:279` and `AppSwipeToAction.kt:51`. These fail contrast in dark (8.1).
* The skeleton is invisible (6.10).
* `onAccent` in dark is regex-patched by the generator (`render.py:772-800`), but the comment-tolerant
  regex depends on the comment layout in `AppColors.kt:127-130`, which is fragile. Prefer computing
  it in Kotlin from luminance at theme build time.

---

## 10. Component API design

* Several components take **different param names for the same concept**:
  - `AppTabRow(tabs, selectedIndex, onTabSelected)` and `AppSegmentedControl(options, selectedIndex, onSelect)`
  - `AppBottomBar(..., onItemSelected)` and `AppWheelPicker(..., onSelectedChange)`
  - `AppChip(selected, onClick)` and `AppRadioButton(selected, onClick)`
  - `AppCheckbox(onCheckedChange: ((Boolean)->Unit)?)` (null means read-only) and `AppSlider(enabled)`

  Pick one convention per pattern: `selected` + `onSelect(Int)`, and `checked` + `onCheckedChange`.
* **Slot APIs are inconsistent.** `AppButton` takes `leadingIcon: ImageVector?`, but `AppTextField`
  takes `leadingIcon: ImageVector?` and `trailing: @Composable`. `AppListItem` takes
  `leading`/`trailing` composables. `AppTopBar.navigationIcon` is an `ImageVector`. Prefer a slot
  for anything that might animate or be glass.
* **There are no `*Defaults` objects** except `AppCardDefaults`, `AppBottomBarDefaults` and
  `AppNavigationRailDefaults`. Colours are computed in private `@Composable` functions
  (`ButtonVariant.style()`, `AppButton.kt:207-247`), so callers cannot reuse them, for example for a
  glass variant. PeriFlow had to fork `AppButton` to add `glass` and `glow`.
* **`AppTextField` is value-based** (`String`). The modern `TextFieldState` API gives
  `InputTransformation.maxLength()` (today an over-long paste is silently rejected,
  `AppTextField.kt:129-131`) and `OutputTransformation` for phone and currency formatting (fixing
  6.4).
* **Missing API hooks:**
  - `AppBottomBar` has no reselect callback (`onReselect` for scroll-to-top or pop-to-root).
  - `AppPagingList` has no `state`.
  - Dialogs and menus have no `visible`/`onDismissed` pair for exit animations.
  - `AppSnackbar` has no swipe-to-dismiss.

---

## 11. AI slop, dead code and weight

**Dead APIs (no callers anywhere in the template):**
- `Modifier.bounceClick` (`Modifiers.kt:67`)
- `AppResponsiveGrid` (`AdaptiveLayout.kt:82`)
- `AppTransitions.staggeredIn` (`AppTransitions.kt:123`)
- `AppMotion.tracking()` (`AppMotion.kt:149`)
- `focusRing` (`AppColors.kt:51`)
- `MessagingSurface` (`UI/MessageHost.kt:54`)
- `UiText?.asStringOrEmpty` (`UI/UiTextExt.kt:15`)
- `AppUserAvatar` (`UI/AppNetworkImage.kt:46`)
- the `AppScaffold` `bottomBar` and `floatingAction` slots

**Catalog-only weight.** These ship in every generated app but are referenced only by the catalog:
- `AppTooltip`, `AppRangeSlider`, `AppSlider`
- all of datetime: `AppDatePicker`, `AppTimePicker`, `AppWheelPicker`
- `AppAnimatedCounter`, `AppStepIndicator`, `AppOtpField`
- all of `AppSpecialFields`: stepper, select, phone, currency, tag and field-button
- `AppFloatingActionButton`, `AppSwipeToAction`, `AppTabRow`, `AppExpandableText`, `AppDropdownMenu`
- `AppAccordion`, `AppLoadMoreFooter`, `AppTimeline`, `AppDetailRow`, `AppStatTile`, `AppRating`,
  `AppSparkline`, `AppBarChart`, `AppKeyValueGrid`
- `AppCheckbox`, `AppRadioButton`, `AppNumberField`, `AppBottomSheet` (direct), `AppLoadingOverlay`
- `AppBadgedBox` (catalog plus the bar), `AppDotBadge`

Whole catalog-only files add up to **2,681 LOC of about 10,030**. R8 strips them from release APKs,
so this is about source weight, build time and the "starting point to reshape" promise. Split
`core/designsystem` into `foundation` and core components (always), plus generator feature groups:
`components-forms`, `components-pickers`, `components-data` and `components-feedback`. Each is
fenced with `// <opt:x>` or owned as whole files in `spec.py`. The catalog requires all of them.

**Inaccurate or narrating comments:**
- "Fifteen styles" (`AppTypography.kt:13`).
- "The weights AppTypography actually asks for" (`AppFonts.kt:37`).
- "Clears the bottom bar when there is one" (`AppScaffold.kt:76-77`).
- "Tapping an open row closes it…" (`AppSwipeToAction.kt:158-159`).
- "Read from `WeekFields.of(Locale.getDefault())`" (`AppDatePicker.kt:48-51`; the code uses `rememberCurrentLocale()`).
- "Minimum interactive sizes" (`AppMetrics.kt:60`).
- The `selectedIcon` promise (`AppBottomBar.kt:47-51`).
- Meta-narration about Material: "Pull-to-refresh ships in Material 3." (`AppPullToRefresh.kt:31`),
  "one of several things Material supplied that has to be rebuilt here" (`AppDialog.kt:37-40`),
  "Not a floating label that animates into the border." (`AppTextField.kt:46`).
- Tautologies: "/** The top bar. */" (`AppTopBar.kt:35`), "/** The defaults. */" (`AppFonts.kt:25`),
  "/** A stable index for this string. */" (`AppAvatar.kt:61`).
- Flowery: "in the language a hand understands" (`AppHaptics.kt:14`); "The personality of an
  interface is carried almost entirely by two numbers" (`AppMotion.kt:13-15`).

Overall comment quality is above average (most comments explain *why*). The slop is concentrated
in the items above.

---

## 12. Per-component verdict (vs iOS quality)

| Component | Verdict |
|---|---|
| `AppTheme` / tokens | Solid architecture. It lacks state-layer, elevation and spring tokens and globally injects the grey overlay. |
| `AppSurface` / `AppClickableSurface` | Good primitive, but it stacks scale + overlay + layers. Needs a feedback enum and opt-in clip. |
| `AppIndication` | Fine as a *row highlight*. Wrong as a global default. |
| `pressScale` | Quick taps pop instead of dip, and it is a composable factory. Port `PressBounceNode`. |
| `appClickable` | Buzzes by default and sets no minimum target on surfaces. |
| `AppHaptics` | Well-mapped API tiers. The `Toggle` on/off distinction is missing (`TOGGLE_OFF`). Default usage is too frequent. |
| `AppButton` | Decent metrics. Triple press feedback. White-on-coral Destructive in dark fails contrast. |
| `AppIconButton` | 36–44dp, overlay on press. Should dim; needs a 48dp target. |
| `AppFloatingActionButton` | Tween expand. Fine, but catalog-only. |
| `AppCard` | Clean treatments. The clickable card gets a grey film over content. |
| `AppDivider` | 1dp, too heavy. Use a hairline. |
| `AppListItem` | Reasonable. Needs grouped/inset (iOS) variants, toggle and value rows, and gutter consistency (it uses `spacing.lg` rather than `gutter`). |
| `AppScaffold` | Basic. Dead slots, misplaced FAB, no content-under-chrome support. |
| `AppTopBar` / `AppBackTopBar` | Title overlaps actions. No collapse, no scroll-edge. Below iOS. |
| `AppLargeTitle` | Static and small (24sp). |
| `AppBottomBar` (Floating) | Opaque card, invisible pill, grey flash, per-frame recomposition. **Not iOS-grade.** |
| `AppBottomBar` (Docked/Minimal/Chunky) | Serviceable. Icon bounce reads "pushy" in Bouncy. |
| `AppNavigationRail` | Same issues as the bar. |
| `AppTabRow` | Good indicator mechanics. Equal widths only, 46dp, overlay flash. |
| `AppSegmentedControl` | Good pill mechanics. No thumb shadow, overlay flash, 40dp. |
| `AppChip` | Nice colour transitions. A 15dp remove target and 34dp height. |
| `AppCheckbox` | Nice drawn tick. Path allocation, 1.5:1 ring, 22dp target without a label. |
| `AppRadioButton` | Fine. Same contrast and target issues. |
| `AppSwitch` | iOS-like proportions, deferred draw. No thumb shadow, 1.13:1 off track, unlabelled in rows. |
| `AppSlider` | **Broken stepped drag; touch-to-scroll sets the value.** |
| `AppRangeSlider` | Correct gesture model and a11y. Good. |
| `AppTextField` (and variants) | Clean visuals. Label and placeholder not in semantics. Value-based API. |
| `AppOtpField` | Looks right. Always-running caret recomposes. No SMS autofill. |
| `AppSpecialFields` | Currency caret bug. Stepper Small targets. Catalog-only. |
| `AppBottomSheet` | Animated open and drag. Back has no animation, no detents, no nested scroll. VitFleet and PeriFlow both improved it. |
| `AppActionSheet` | iOS-like layout (grouped plus a separate cancel). Inherits the sheet issues. |
| `AppDialog` / `AppAlertDialog` | Enter only, composition-read animation. |
| `AppDropdownMenu` | Enter only, centre-origin scale, no edge-aware positioning. |
| `AppTooltip` | The pop animation never runs, and an empty click is exposed to a11y. |
| `AppLoadingOverlay` | Fine. |
| `AppSnackbar` / Host | Tone-aware. Tweens, no swipe, no queue, not lifecycle-safe (host). |
| `AppBanner`, `AppEmptyState`, `AppErrorState` | Good, clear. |
| `AppSkeleton` | Invisible in dark. Allocates per frame. One transition per instance. |
| `AppCircularProgress` / `AppLinearProgress` | Fine (draw-phase reads). The spinner is Material-like, not the iOS spokes. |
| `AppBadgedBox` / `AppDotBadge` / `AppStatusPill` | Fine. Badge text contrast in dark fails. |
| `AppAvatar` | Fine. Regex per recomposition, duplicate semantics. |
| `AppStepIndicator` | Good, including a11y. |
| `AppTone` | Fine. |
| `AppAccordion` | Fine. Missing expand state semantics. |
| `AppPager` / `AppPagerIndicator` | Fine. |
| `AppTimeline` | The fixed connector height misaligns rows. |
| `AppSwipeToAction` | Open-row tap bug, RTL bug, no a11y actions. |
| `AppPullToRefresh` | Stale `isRefreshing`, indicator-only (Material). VitFleet's version is better. |
| `AppDatePicker` | Good locale handling. Day semantics weak, no swipe between months. |
| `AppWheelPicker` / `AppTimePicker` | Beautiful 3D wheel. **The stale-capture bug breaks the time picker.** |
| `AppAnimatedCounter` | Nice. Heavy per digit. |
| `AppExpandableText` | Fine. |
| `AppText` / `AppMonoText` | Needs `ColorProducer` and `autoSize`. |
| `AppIcon` / `AppIcons` | Uniform stroke set. No filled variants, no `autoMirror`, eager init. |
| `AppTransitions` / `AppAppear` / `AppStaggeredColumn` | Good deferred reads. Mixed px and dp, one dead member. |
| `busyOverlay` | Composition-read alpha. Content stays focusable. |
| `AdaptiveLayout` | Good (`centeredMaxWidth`, `AppTwoPane`). The grid is dead. |
| UI `MviScreen` | Good (lifecycle-aware effects). |
| UI `MessageHost` | Not lifecycle-aware. |
| UI `AppNetworkImage` | `SubcomposeAsyncImage` in lists. |
| UI `AppLottie` | Good (reduce-motion aware). |
| UI `AppBrowser` | Good (scheme allow-list). |
| UI `AppLocales` | Fine. |
| UI `AppPagingList` | Good state coverage. State not hoistable, placeholders render nothing. |
| UI `FormState` | Works. Compose `mutableStateOf` inside a ViewModel is a second state model next to MVI `UiState`, so decide on one. |

---

## 13. Missing components a serious app needs

1. **Inset-grouped list sections** (iOS "insetGrouped" table): a rounded group container, inset
   hairline separators, header and footer text. Row variants: navigation (chevron), value, toggle,
   destructive. This is the most-used iOS pattern, and VitFleet started on it (`VF/foundation/GroupShapes.kt`).
2. A **collapsing large-title top bar**, scroll-linked, with a scroll-edge fade or blur (PeriFlow's
   `scrollEdgeEffect`).
3. A **glass tab bar and rail** (section 3), plus a **floating glass toolbar** and glass top-bar buttons.
4. A **detent sheet** (medium/large, nested scroll, hoisted state); see PeriFlow's `AppDetentSheet`.
5. A **context menu / long-press preview** and a **pull-down menu button** anchored with edge-aware
   positioning.
6. A **search bar with cancel**, focus animation and scope chips.
7. A **toast or HUD** for non-blocking confirmations, and a **progress HUD**.
8. **Undo snackbar** with swipe-to-dismiss and queueing.
9. **Leading and trailing swipe actions** with full-swipe and a11y custom actions; **drag-to-reorder** lists.
10. **Inline compact date and time pills** that expand to a picker; a picker-in-sheet.
11. **Zoomable image viewer** and **carousel** (snap, peek); an **avatar group**.
12. **Sticky bottom action bar** (CTA plus safe area plus keyboard aware).
13. **iOS-style activity indicator** (spokes) as a style option; **page control** with
    interactive scrubbing.
14. **Rubber-band overscroll** (`LocalOverscrollFactory`) as an opt-in for the iOS feel.
15. **Inline link / text button** with dim feedback; **toggle row** with whole-row semantics.
16. **`minimumTouchTarget` modifier**, `PressFeedback`, `NoIndication`, and **shimmer provider**
    (foundation-level pieces).

---

## 14. Port-back list (from the apps)

| From | File | What |
|---|---|---|
| PeriFlow | `PF/foundation/PressBounce.kt` | `Modifier.Node` press bounce, finish the dip on quick taps, read only in the layer block |
| PeriFlow | `PF/theme/AppMotion.kt` | `release()` under-damped spring, `compactPressScale`, `stagger` token, `Fluid` style |
| PeriFlow | `PF/foundation/Glass.kt`, `LiquidInteraction.kt`, `component/navigation/GlassBottomBar.kt`, `GlassRail.kt` | Liquid glass, after the 3.2 fixes |
| PeriFlow | `PF/foundation/ScrollEdge.kt` | Cheap scroll-edge fade, `LocalTopBarInset`, `PaddingValues.plusTop` |
| PeriFlow | `PF/foundation/ProgressiveBlur.kt` (`ScreenEdgeBlur`) | Status- and nav-bar edge blur over glass |
| PeriFlow | `PF/component/container/AppScaffold.kt` | Content under bars through `SubcomposeLayout`, `LocalSheetSurface` |
| PeriFlow | `PF/component/overlay/AppDetentSheet.kt`, `AppSheetCloseButton.kt` | Detents, hoisted state, animated hide on Back |
| PeriFlow | `PF/component/button/AppButton.kt` | Destructive as red-on-tint; `ButtonIcon` pop swap; side-padding minimums |
| PeriFlow | `PF/foundation/AppSurface.kt` | Press scale applied outside the caller's modifiers |
| PeriFlow | `periflow …/core/navigation/AppShell.kt` (bar enter/exit) | Short tween partial rise for bar show and hide |
| VitFleet | `VF/component/refresh/AppPullToRefresh.kt` | Stale-state fix, content follows the pull, minimum acknowledgement |
| VitFleet | `VF/component/overlay/AppBottomSheet.kt` | Nested scroll, IME insets (`decorFitsSystemWindows=false`), close button, scrollable body |
| VitFleet | `VF/component/navigation/AppTopBar.kt` | Title laid out in the Row (no overlap), `widthIn(min = slot)` actions |
| VitFleet | `VF/component/navigation/AppTabRow.kt` | Content-weighted tab widths, width-lerping indicator, `selectedTabContent` dark fix |
| Both | `component/text/AppText.kt` | `autoSize: TextAutoSize?` (VitFleet also added `AppFiguresText`) |

---

## 15. Suggested order of work

1. Feedback overhaul (section 1) and the bottom-bar deferred reads (2.2). This is the fastest
   visible win against "unnecessary ripple" and "pushy bar".
2. Functional bug fixes in section 6 (wheel, slider, refresh, sheet, top bar, currency, skeleton).
3. Spring tokens (section 4), and replace `sheet()` misuse with `selection`/`spatial`.
4. Content-under-chrome insets (`LocalBottomBarInset` / `LocalTopBarInset`), then the optional
   `glass` feature (section 3) with `GlassPolicy` and Macrobenchmark gates.
5. A11y contrast and target fixes, plus the extended palette test.
6. Component tiering (section 11), dead-code removal and comment cleanup.
