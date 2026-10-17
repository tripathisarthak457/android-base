# WP05-glass handoff: the full liquid glass effect set (REQUIREMENTS 37)

Branch `wp/WP05-glass`, worktree `$OV/wt/WP05-glass`, forked from `wp/WP05` at `5d515a0` with `wp/WP04` (`3c9c403`)
and `wp/WP03` (`285275d`) merged in (below). `OV=D:/projects/tools/.overhaul`,
`O=$OV/out/WP05-glass`, `P=$OV/perf/WP05-glass`, `L=$OV/bin/locked`,
`ds/` = `templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/`.

The user (Oct 15): restore the full liquid glass effect in the iOS-style components, the bottom bar first, with real
similarity to the Kyant playground's bottom bar (improved where possible). This note records the playground's exact
parameters, what each of our effects does against them, the numbers, the strips, and what is open.

## 1. The playground, read from source

Repository `github.com/Kyant0/AndroidLiquidGlass`, default branch `kmp` (read 2026-10-15 through the GitHub API; the
catalog sources are saved under `$OV/research/glass/kyant-catalog/`, the library's 2.0.1 sources under
`$OV/research/glass/kyant-src/`). The catalog module (`app/src/commonMain/kotlin/com/kyant/backdrop/catalog/`) builds
against the `backdrop` module of the same tree, whose `drawBackdrop` API matches 2.0.1 (`backdrop`, `shape`,
`effects`, `highlight`, `shadow`, `innerShadow`, `layerBlock`, `exportedBackdrop`, `onDrawBackdrop`, `onDrawSurface`,
`onDrawFront`; `Highlight`, `Shadow`, `InnerShadow` data classes; `lens(height, amount, depthEffect,
chromaticAberration)`, `blur`, `vibrancy`, `colorControls`). Every value below is in dp unless said otherwise.

### 1.1 `LiquidBottomTabs` (components/LiquidBottomTabs.kt, 288 lines)

Three stacked elements in a `BoxWithConstraints`, one `DampedDragAnimation` (utils/DampedDragAnimation.kt) driving
them all:

| Element | Backdrop | `effects` | `highlight` | `shadow` | `innerShadow` | `layerBlock` | `onDrawSurface` |
|---|---|---|---|---|---|---|---|
| Bar body (`Row`, 64 high, `Capsule()`, 4 padding) | the wallpaper | `vibrancy(); blur(8); lens(24, 24)` (static) | default `Highlight.Default` (0.5 wide, blur 0.25, white 0.5, `BlendMode.Plus`, 45°, falloff 1) | default `Shadow.Default` (radius 24, offset (0, 4), black 0.1) | none | scale `lerp(1, 1 + 16/width, pressProgress)` | `containerColor` = `FAFAFA` at 0.4 (light) / `121212` at 0.4 (dark) |
| Tabs copy (`Row`, 56 high, `alpha(0)`, exported as `tabsBackdrop`, tinted `ColorFilter.tint(accent)`, accent `0088FF` light / `0091FF` dark) | the wallpaper | `vibrancy(); blur(8); lens(24·p, 24·p)` (p = pressProgress, read inside `effects`) | `Highlight.Default.copy(alpha = p)` | default | none | none; its tab contents scale `lerp(1, 1.2, p)` through `LocalLiquidBottomTabScale` | `containerColor` |
| Lens (`Box`, 56 high, width 1/n of the bar, `Capsule()`) | `combined(wallpaper, tabsBackdrop)` | `lens(10·p, 14·p, chromaticAberration = true)` | `Highlight.Default.copy(alpha = p)` | `Shadow(alpha = p)` (radius 24, black 0.1) | `InnerShadow(radius = 8·p, alpha = p)` (offset (0, radius), black 0.15) | scale `dampedDrag.scaleX/Y` (1 at rest, 78/56 = 1.393 pressed), then `scaleX /= 1 − clamp(v·0.075, ±0.2)`, `scaleY *= 1 − clamp(v·0.025, ±0.2)` with v the value velocity in tabs/s | resting tint black 0.1 (light) / white 0.1 (dark) at alpha `1 − p`, plus black `0.03·p` |

Also on the bar body and the tabs copy: `InteractiveHighlight` (utils/InteractiveHighlight.kt), a press glow: a flat
white `0.08·p` plus a radial white `0.15·p` (`smoothstep(r, r/2, dist)`, r = 1.5 × the bar's shorter side) at the
finger, both `BlendMode.Plus`, the finger position trailing on `spring(0.5, 300)`. The whole bar translates by
`panelOffset = 4 · sign(f) · EaseOut(|f|)`, f the drag's x offset over the bar's width, released on `spring(1, 300)`.

`DampedDragAnimation` springs: value `spring(1, 1000)` (ζ 1, k 1000: response 0.2 s), velocity `spring(0.5, 300)`,
pressProgress `spring(1, 1000)`, scaleX `spring(0.6, 250)`, scaleY `spring(0.7, 250)`. A press raises pressProgress and
the scales; a release waits until the value is within 2.5 % of its target, then lowers them. A **tap** goes through
`animateToValue`, which presses, animates the value to the tapped index and releases: so the playground's tap is the
same morph as a drag's release (swell to 1.39, refraction, dispersion, highlight, inner and outer shadow, the velocity
stretch), ending where the lens lands. The release projects nothing: the lens lands on `round(value)`.

What it costs, per frame of a tap or drag, on the playground's own code: the lens's and the tabs copy's `effects`
read `pressProgress`, so Kyant rebuilds both RenderEffect chains (`observeEffects`) every frame; the bar body's layer
scale changes, so its sample is inverse-scaled and re-recorded every frame; the tabs copy is a second full-bar
sampling layer; the three `highlight`/`shadow`/`innerShadow` lambdas allocate and re-record their layers (the outer
shadow at bar size + 96 dp with a 24 dp mask blur) every frame. That is PeriFlow's G1 (`audit/periflow-learnings.md`),
which this branch must not repeat while keeping every visible effect.

### 1.2 `LiquidButton` (122 lines)

`Capsule()`, 48 high, `vibrancy(); blur(2); lens(12, 24)` static, default highlight and shadow, press scale
`1 + 4/height`, the body follows the finger with `maxOffset · tanh(0.05 · offset / maxOffset)` (maxOffset = the
shorter side) and stretches by up to `4/height` along the drag, `InteractiveHighlight` glow; `tint` drawn as
`drawRect(tint, BlendMode.Hue)` then `tint` at 0.75; `surfaceColor` drawn plain (the catalog uses white 0.3).

### 1.3 `LiquidToggle` (203 lines)

Track 64 × 28, `Capsule()`, exported as `trackBackdrop`, `lerp(track, accent, value)` (track `787878` at 0.2 /
`787880` at 0.36, accent `34C759` / `30D158`). Thumb 40 × 24: backdrop `combined(wallpaper, track scaled
(lerp(2/3, 0.75, p), lerp(0, 0.75, p)))`; `blur(8·(1 − p)); lens(5·p, 10·p, chromaticAberration = true)`; highlight
`Highlight.Ambient` at width/1.5, blur/1.5, alpha p; shadow radius 4, black 0.05; inner shadow `4·p`, alpha p; scale
1.5 pressed with the velocity stretch (v/50); surface white at `1 − p`. Drag width 20; a release past 0.5 flips.

### 1.4 `LiquidSlider` (212 lines)

Track 6 high, `Capsule()`, exported; the active part accent. Thumb 40 × 24, backdrop `combined(wallpaper, track
scaled (lerp(2/3, 1, p), lerp(0, 1, p)))`, `blur(8·(1 − p)); lens(10·p, 14·p, chromaticAberration = true)`, the same
highlight, shadow, inner shadow and scale 1.5 as the toggle, velocity stretch v/10, surface white at `1 − p`.

### 1.5 `DialogContent` (139 lines)

`RoundedRectangle(48)`, `colorControls(brightness = 0.2 light / 0 dark, saturation = 1.5); blur(16 light / 8 dark);
lens(24, 48, depthEffect = true)`, `Highlight.Plain` (white 0.38, Plus), surface `FAFAFA` at 0.6 / `121212` at 0.4,
over a dim `29293A` at 0.23 / `121212` at 0.56. Buttons inside are plain capsules.

### 1.6 `GlassPlaygroundContent` (241 lines), `ControlCenterContent`

A 256 dp `RoundedRectangle(256/2 · cornerFrac)` with sliders for corner radius, blur (0–32), refraction height
(`frac · minDimension · 0.5`), refraction amount (`frac · minDimension`) and chromatic aberration, `depthEffect =
true`, `Highlight.Plain`, pan, zoom and rotation. Its sheet: `RoundedRectangle(32)`, `vibrancy(); blur(4); lens(16, 32)`,
`Highlight.Plain`, white 0.5, exported for the sliders. Control-center tiles: `vibrancy(); lens(24·p, 48·p,
depthEffect = true)`, `Highlight(HighlightStyle.Default(angle = gravity, falloff = 2))`.

### 1.7 Kyant internals that set the cost (2.0.1 source)

- `DrawBackdropNode.draw` re-records its sample layer on every draw of the node, and `observeReads { updateEffects() }`
  rebuilds the RenderEffect chain whenever a state read inside `effects` changes (`DrawBackdropModifier.kt`).
  `RenderEffect.createRuntimeShaderEffect` copies the shader's uniforms, so animated uniforms mean a new chain per frame.
- `lens()` needs API 33 (AGSL) and a rounded-rectangle shape; `blur()` pads the recording by its radius once another
  effect precedes it, and `lens()` takes `refractionHeight` back off that padding (`effects/Lens.kt:25`), so
  `vibrancy; blur(8); lens(24, 24)` records at the node's own size.
- `HighlightNode`, `ShadowNode` and `InnerShadowNode` each own a `GraphicsLayer` and re-record it on every draw, with a
  `BlurMaskFilter` (highlight, shadow) or a `BlurEffect` (inner shadow); the outer shadow's layer is the node plus
  four radii on each side.
- The dispersion shader samples the content seven times per pixel (`Shaders.kt`,
  `RoundedRectRefractionWithDispersionShaderString`); the plain refraction once.

## 2. What this branch builds (state: in progress)

See the sections below as they land. The foundation it builds on is `handoff/WP05.md` (`GlassQuality` tiers,
`SceneBackdrop` recording the scene once, `GlassSurface`, `LiquidLensState` and `liquidLens`).

### 2.0 Merges

`wp/WP04` merged clean but for `web/frontend/lib/catalogue.json` (regenerated). `wp/WP03` conflicted in 21 files,
resolved in `282dd3c` toward: WP04's shell, tab bar, stack and presentation host (the five files WP03 only swept
are gone, and so is WP03's pre-spec `SheetScene.kt`, which its own working tree deletes); WP03's components, catalog
sections and theme; `chromeContentPadding` and the chrome locals in `foundation.layout`, where WP04 moved them, with
WP03's 23 callers re-pointed; `navigation3` 1.2.0; WP04's `StackLayout` on WP03's `AppStateView`; WP03's `CatalogBars`
on `AppTabBar`; the WP04 shell on WP03's `SizeClass` (`AppTheme.windowClass`) in place of the `AppWindowSize` WP03
removed. WP03's committed tree has no sheet system yet (only the old `overlay/AppSheet.kt`), so the Glass sheet
style here is a foundation piece with a catalog demo, to be wired into the real `SheetSurfaceNode` when it exists.
The integration merge of WP03 after WP04 will meet the same conflicts; this resolution is a tested preview of it.


### 2.1 The bottom bar (`LiquidTabs`), effect by effect

`ds/component/navigation/LiquidTabs.kt` is `AppTabBar`'s `BarTreatment.Glass` branch (fenced) for the bar and the
rail. The design style `Glass` (`tokens/styles.json`, `requires = ["liquidglass"]`, Social's shapes and surfaces with
`bar = Glass`, `indicator = Subtle`, `sheetMaterial = Glass`) selects it; the token emitter now fences a style that
needs a feature (`genkit/tokens/kotlin.py`), and `AppDesignStyle.Glass` / `BarTreatment.Glass` are fenced entries.
The shell's `ShellChrome` becomes a `GlassStage` under that treatment, `ProvideGlassTier` (fenced,
`core/navigation/shell/GlassTier.kt`) decides the tier (`ShellConfig.glassQuality` forces one; the dev extra
`glass.quality=<Tier>` sets it) and `ShellLog` logs `perf.glass: tier=<Tier> stage=<true|false>`, which `perf.py`
checks before it keeps a forced tier's numbers.

| Effect | Playground (`LiquidBottomTabs`) | Here | Why it is cheap |
|---|---|---|---|
| Bar body: blur, vibrancy | `vibrancy(); blur(8)` | `vibrancy(); blur(8)` (`GlassDefaults.ChromeBlur`, was 10) | one sampling layer, recorded once per frame the scene changes, by reference to the scene recorded once (`SceneBackdrop`) |
| Bar body: refraction at the rim | `lens(24, 24)` | `lens(24, 24)` on the Live tier (`ChromeEffects`), none at LiveNoLens | static chain; one AGSL pass over the bar's area, with the blur |
| Bar body: rim light | `Highlight.Default` (0.5 dp, white 0.5, Plus, 45°) | `rimLight(ChromeRim)` (`RimLight.kt`): the same maths, recorded once per size | Kyant's node re-strokes with a new blur mask per draw; ours composites a layer |
| Bar body: shadow | `Shadow.Default` (24 dp, black 0.1) re-recorded per draw | `glassEdges` shadow: Compose's cached drop shadow, 16 dp, light theme only | rasterised once per size, outside the sampled layer |
| Bar body: tint | `FAFAFA`/`121212` at 0.4 | `surface` at 0.40 / 0.35 | in `onDrawSurface`, inside the recording |
| Bar body: press swell | layer scale `1 + 16/width · p` | the same, on a layer outside the Kyant node (`BarSwell`) | the sample is not inverse-scaled, so nothing re-records: the blurred scene stretches with the glass |
| Bar body: give with the drag | `panelOffset` 4 dp · EaseOut | `BarGive` 4 dp · easeOut(pull / tabs), `LiquidLensState.pull` | a layer translation on the whole group; `pull` follows the finger and springs back on the settle spring, in the one frame loop |
| Press glow | `InteractiveHighlight`: flat white 0.08·p plus a radial 0.15·p (r = 1.5 × short side), Plus, shader per frame | `pressGlow` (`PressGlow.kt`): the same two, the pool recorded once and translated | layer alpha and position per frame, no shader, no allocation |
| Tabs copy through the lens | a second full-bar sampling row at alpha 0, exported, tinted, scaled 1.2 on press | WP04's `TabLayer.Selected` cells recorded unclipped into a `SceneBackdrop` (`recordScene`) and combined with the scene for the lens | no second composition or sampling pass; the lens magnifies the copy instead of scaling it |
| Lens: refraction with dispersion | `lens(10·p, 14·p, chromaticAberration = true)` | `lens(12, 20, chromaticAberration = true)` static (`LensEffects`); the resting tint fades off it with the intensity and the sample magnifies `1 + max(0.2·pressure, 0.12·travel)` | one fixed chain; the lens's draw reads the intensity, the bar never does |
| Lens: highlight | `Highlight.Default.copy(alpha = p)`, allocated and re-recorded per frame | `rimLight(LensRim) { intensity }` | layer alpha per frame |
| Lens: inner shadow | `InnerShadow(8·p, alpha = p)`, a blurred layer per frame | a vertical gradient band inside the top edge, black 0.15 over 16 dp, at the intensity (`InnerBand`) | one brush per size, drawn inside the shape's clip in `onDrawFront` |
| Lens: cast shadow | `Shadow(alpha = p)` (24 dp, black 0.1), re-recorded per frame | `glassShadow(LensShadow) { intensity }`: Compose's cached painter at 24 dp, black 0.10, light theme only | rasterised once, drawn with the frame's alpha, outside the body |
| Lens: swell | scale 78/56 = 1.393 on `spring(0.6, 250)` / `spring(0.7, 250)` | `PressSwell` 0.39 on the press spring; the lens is a sibling of the capsule so it reaches past its edge (`LensHeight` 50 dp in the 64 dp bar) | `layerBlock`, as before |
| Lens: velocity stretch | `scaleX /= 1 − clamp(v·0.075, ±0.2)`, `scaleY *= 1 − clamp(v·0.025, ±0.2)` | along `1 + 0.2·travel`, across `1 − 0.08·travel`, travel read off the settle spring's energy | holds through a turnaround (handoff/WP05.md "Travel is the spring's energy") |
| Lens: resting tint | black/white 0.1 at `1 − p`, plus black 0.03·p | the indicator's colour over the chrome's tint at `1 − intensity`, plus black 0.03·intensity (`ShadeAlpha`) | `onDrawSurface` |
| Tap morph | `animateToValue` presses, travels on `spring(1, 1000)`, releases | the travelling lens of REQUIREMENTS 35: the selection moves the lens on the `thumb` spring and the motion lifts it (`travel`), no press | nothing but the lens element reads it |
| Drag | drag on the lens only, relative; release lands on `round(value)` | a hold (150 ms) or a move past the touch slop anywhere on the row presses the lens where the finger is; it trails on the `interactive` spring; release projects the velocity (`release()`), ticks on each tab crossed | one `pointerInput` on the initial pass, writing plain values; no coroutine per event |
| Warm-up | — | the lens shows itself under its full tint for its first two frames (`WarmFrames`), so its AGSL pipelines are compiled on launch, not on the first tap | the two frames look like the indicator |

Tiers: Live has all of it. LiveNoLens (API 31–32, or thermal moderate) keeps the blur, the vibrancy, the rim light
(a plain line below API 33) and WP04's two-edge drawn indicator, no refraction and no lens. Frosted draws the capsule
with `glassSurface` and samples nothing. Solid is the style's bar. Reduce motion keeps the lens plain for taps and
drags (handoff/WP05.md). The rail (beside the content, outside the stage) keeps its opaque body and, at Live, a lens
that refracts the tinted tabs over it.

Semantic merge decisions this needed (for the integration merge of WP03 after WP04): WP04's bar reads WP03's
`BarTreatment` (Floating, Translucent, Docked) and `BarIndicator` (Pill: the solid `barIndicator` pill; Subtle: the
accent-tinted pill, where WP03 meant no pill at all), WP03's colour roles (`surfaceElevated`, `separator`,
`barIndicator`) and `AppStyle.borderWidth`; the tab labels use `caption2`; `LocalTabVisible` is WP04's holder
(`.isVisible`) in WP03's callers; `AppTheme.reduceMotion` replaces WP04's `rememberReduceMotion()`.

WP05: merge wp/WP05-glass into wp/WP05 at the start of your next run; the orchestrator verified it

## Orchestrator note (Oct 15, 20:50): Android skills v1.0.15 (REQUIREMENTS 38)

Google published new and updated skills, now installed under `~/.claude/plugins/marketplaces/android-skills/` (v1.0.15). Read the new **jetpack-compose/jetpack-compose-performance** SKILL.md and its references before your next change, along with the updated navigation-3, navigation-event, adaptive and android-profiler skills where they touch your area. The digest `research/ANDROID-SKILLS-DIGEST.md` is being updated with the rules that change what we build.
