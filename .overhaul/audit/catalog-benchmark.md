# Audit: `template/catalog` and `template/benchmark`

Scope: `android-base/template/catalog/**`, `android-base/template/benchmark/**`, plus the generator,
build and docs code that shapes them, compared with the hand-improved copies in PeriFlow
(`periflow-android`) and VitFleet (`vitfleet-android`).

Method: read every file in both modules; inventoried every public function and token in
`:core:designsystem` and checked which ones the catalog references (script in this session);
read the androidx `baselineprofile` Gradle plugin 1.5.0 and `benchmark-macro` 1.5.0 sources from
the local Gradle cache to check task names and defaults; diffed both modules against PeriFlow and
VitFleet; inspected PeriFlow's committed baseline profile to see what its generator actually
recorded. Nothing was built. No files were changed in the three repositories.

Paths below are relative to `android-base/` unless they start with `periflow/` (=
`jorwen/periflow/periflow-android/`) or `vitfleet/` (= `tools/vitfleet/vitfleet-android/`).

---

## 1. Summary

**Benchmark.** As generated, the module cannot work at all. The app has four flavours and
`:benchmark` picks none of them, so Gradle cannot tell which app variant to test. PeriFlow found
this the hard way. Its commit 11e50cd says "The benchmark module never ran: with no flavour chosen
the app's four were ambiguous". VitFleet still ships the broken copy. Even with that fixed there
are three more problems:

- **The profile records only the onboarding screen.** In a default project (onboarding and auth
  are on by default), a fresh install lands there, and there is no test hook to skip past it.
- **Release builds would not ship the profile.** The app does not set `mergeIntoMain`, and the
  plugin's default for apps is per-variant.
- **There are no frame-timing tests.** Nothing measures tab switching, push/pop or scrolling,
  which are exactly what this overhaul wants to make smooth.

PeriFlow's hand-written walk shows how this goes wrong in practice. Its commit claims "every tab",
but the profile has no `TodayScreen`, no `AppShell` and no Kyant backdrop class. The walk failed
silently and only recorded onboarding.

**Catalog.** It is a well-built, readable dev app. Each section is a `CatalogGroup`, laid out in
masonry columns, with live theme and design-style switching. But it does not show "every
component, in both themes and all four styles", which its own subtitle claims:

- **Components missing:** 11 public components or helpers are never shown: `AppNavigationRail`,
  `AppDialog`, `AppDatePickerDialog`, `AppTimePickerDialog`, `AppSnackbarHost`, `AppPagerIndicator`,
  `AppSkeleton`/`Line`/`Circle`, `AppTwoPane`, `AppResponsiveGrid`, `AppClickableSurface`, and
  every press-feedback modifier.
- **Tokens missing:** 11 colour tokens, 3 shapes, `labelSmall`, every `AppSizes` value and every
  motion token are not shown.
- **States missing:** most disabled states are absent.
- **Feel section bug:** it nests an `AppTheme` that throws away the dark/light mode and design
  style the user picked.
- **Wrong default style:** a generated project's catalog opens in Utility/Standard, whatever style
  the project was generated with.
- **No accessibility toggles:** there is no RTL, font-scale or reduce-motion control. The
  Robolectric screenshot suite that used to cover light, dark and 2.0× font was deleted in
  7b1a97a. The generator still refers to its folder.

**Navigation transitions.** The catalog cannot show the push/pop and tab-switch transitions. They
are `internal` in `:core:navigation`, and the catalog depends only on the design system.

**Liquid glass.** If Kyant liquid glass becomes an optional feature, the catalog and benchmark are
where its FPS cost gets proven or disproven. Today neither project can do that: PeriFlow's
catalog shows no glass at all, and its only glass FPS figure is a hand measurement in a code
comment.

---

## 2. Catalog findings

### C1. Feel section's nested `AppTheme` discards the user's theme mode and design style (bug, high)

`catalog/src/main/kotlin/com/base/app/catalog/CatalogFeel.kt:79`

```kotlin
AppTheme(motionStyle = style, hapticsEnabled = haptics) {
```

`AppTheme`'s other parameters default to `mode = ThemeMode.System` and `designStyle =
AppDesignStyle.Utility` (`core/designsystem/.../theme/AppTheme.kt:35,41`). With colours null it
re-resolves the palette from those defaults (`AppTheme.kt:48-50`). So with **Dark + Social**
picked in the index on a light-mode phone, the "Try it" and "The vocabulary" groups render in
**light Utility** inside a dark Social page. That is the exact mismatch the index comment at
`CatalogApp.kt:155-156` says the catalog exists to reveal.

Root cause: the design system has no way to override only the motion. `LocalAppMotion` is
`internal` (`AppMotion.kt:166`), so the only route for a caller is a whole second `AppTheme`.

Fix (design system, a few lines):

```kotlin
// core/designsystem/theme/AppMotion.kt
@Composable
fun ProvideAppMotion(
    style: AppMotionStyle,
    hapticsEnabled: Boolean = LocalAppHapticsEnabled.current,
    content: @Composable () -> Unit,
) {
    val motion = remember(style) { style.motion() }
    CompositionLocalProvider(
        LocalAppMotion provides motion,
        LocalAppHapticsEnabled provides hapticsEnabled,
        content = content,
    )
}
```

Then the catalog uses `ProvideAppMotion(style, haptics) { … }` in place of the nested
`AppTheme`.

### C2. A generated project's catalog ignores the project's design and motion style (bug, high)

- `catalog/.../CatalogApp.kt:51`: `var designStyle by rememberSaveable { mutableStateOf(AppDesignStyle.Utility) }`
- `CatalogApp.kt:53`: `AppTheme(mode = themeMode, designStyle = designStyle)`. No `motionStyle`,
  so it is always `Standard`.
- `CatalogFeel.kt:32`: `mutableStateOf(AppMotionStyle.Standard)`
- `generator/genkit/render.py:328-365` (`apply_feel`) rewrites only `app/.../ui/AppRoot.kt`.

A project generated with `"design_style": "Social", "motion_style": "Calm"` therefore opens its
catalog in Utility/Standard. PeriFlow fixed this by hand: the diff against the template shows
`CatalogApp.kt:51 → AppDesignStyle.Bloom` and `CatalogFeel.kt:32 → AppMotionStyle.Fluid`.

Fix: the generator writes the style once, into the design system, and both roots read it:

```kotlin
// core/designsystem/theme/AppBrand.kt   (generator rewrites these two lines)
object AppBrand {
    val designStyle = AppDesignStyle.Utility
    val motionStyle = AppMotionStyle.Standard
}
```

Then use `AppBrand.designStyle` as the default for `AppTheme(designStyle=…)`, for `AppRoot` and
for `CatalogApp`. `apply_feel` then edits one constant instead of string-replacing an
`AppTheme(` call.

### C3. The catalog does not show every component; subtitle and website over-promise (bug/docs, medium)

`CatalogApp.kt:136` says "Every component, in both themes and all four styles".
`web/frontend/components/hero.tsx:56` says "catalog app that shows every component". AGENTS.md
("The website must not over-promise") forbids that.

Public design-system API that the catalog never references (checked by script against all catalog
sources):

| Missing | Defined at |
|---|---|
| `AppNavigationRail` (the wide-window shell nav) | `component/navigation/AppNavigationRail.kt:36` |
| `AppDialog` (custom-content dialog) | `component/overlay/AppDialog.kt:42` |
| `AppDatePickerDialog`, `AppTimePickerDialog` | `component/datetime/AppDatePicker.kt:269`, `AppTimePicker.kt:112` |
| `AppSnackbarHost` (the animated, real snackbar) | `component/feedback/AppSnackbar.kt:99` |
| `AppPagerIndicator` | `component/list/AppListPieces.kt:207` |
| `AppSkeleton`, `AppSkeletonLine`, `AppSkeletonCircle` | `component/feedback/AppSkeleton.kt:32,76,90` |
| `AppTwoPane`, `AppResponsiveGrid`, `Modifier.centeredMaxWidth` | `foundation/AdaptiveLayout.kt:50,82,29` |
| `AppClickableSurface` | `foundation/AppSurface.kt:61` |
| `Modifier.pressScale`, `bounceClick`, `clickableNoIndication`, `appClickable`, `disabledAlpha` | `foundation/Modifiers.kt:23,67,93,113,141` |
| `Modifier.offsetShadow` | `foundation/StyleModifiers.kt:13` |

The missing press-feedback modifiers matter for the "no unnecessary ripple" goal. There is no page
where a designer can compare the overlay indication (`AppIndication`), `pressScale` and
`bounceClick` side by side.

**States never shown**, from a signature-by-signature check:

- **Disabled:** missing for `AppIconButton`, `AppChip`, `AppRadioButton`, `AppSlider`,
  `AppRangeSlider`, `AppSegmentedControl`, `AppListItem`, `AppCard(onClick)`. Only Primary is shown
  disabled (`CatalogComponents.kt:126`); Secondary, Ghost and Destructive disabled are never
  rendered.
- **Icon buttons:** `AppIconButton` sizes Small/Large and variants Primary/Tertiary.
- **Text field options:** `readOnly`, `leadingIcon`, `trailing`, `showCounter` and
  `singleLine = false` on `AppTextField` (`AppTextField.kt:52-67`).
- **Bars and sheets:** `AppTopBar(showDivider = false)` and `AppBottomSheet(showHandle = false)`.

`CatalogComponents.kt:213` `AppCheckbox(false, {}, label = "Unchecked")` is wired to a no-op, so
tapping it shows press feedback and nothing else. It looks broken.

### C4. Foundations page: tokens missing, a false caption and a misleading summary (bug/docs, medium)

- `CatalogSection.kt:16` summary is "Colour, type, spacing, elevation, **motion**", but
  `FoundationsSection` (`CatalogFoundations.kt:31-141`) has no motion group. Durations, easings and
  the press, navigation and sheet springs (`AppMotion.kt:93-164`) are shown nowhere.
- **Colour tokens missing:** 11 of 33 `AppColors` fields are never shown. These are
  `surfaceInverse`, `contentDisabled`, `contentInverse`, `onAccent`, `onSecondary`, `onTertiary`,
  `borderStrong`, `divider`, `skeleton`, `scrim` and `focusRing` (`AppColors.kt:15-51`).
- **Shape tokens missing:** `none`, `xl` and `sheet` (`AppMetrics.kt:38,43,47`). The "Shapes" group
  (`CatalogFoundations.kt:132-140`) stops at `pill`.
- **Spacing tokens missing:** `xxs`, `xxxl` and `section` (`AppMetrics.kt:13,20,26`).
- **Type tokens missing:** `labelSmall` (`AppTypography.kt:35`), and `TypeRow`
  (`CatalogFoundations.kt:194-207`) shows only the font size. Weight, line height and tracking are
  what `TypeVoice` changes between design styles, so the style switch is invisible on this page.
- **Size tokens missing:** every `AppSizes` field (touch target, icon, button and field heights,
  bar heights, border widths).
- **Status border:** the caption at `CatalogFoundations.kt:64` says "Each meaning carries a
  content, a subtle fill and a border — always together", but `StatusRow` (`:167-191`) never draws
  `status.border`.
- **Typeface caption:** `CatalogFoundations.kt:75` "Downloaded, not bundled." is false whenever
  `googlefonts` is off. The variant
  `generator/variants/googlefonts-off/.../AppFonts.kt` sets `AppFontNames.Sans = "sans-serif"`, so
  the generated catalog prints "Sans: sans-serif" under a claim that it was downloaded. Wrap the
  sentence in `// <opt:googlefonts>` with a `// <opt:!googlefonts>` alternative.

### C5. `AppLoadingOverlay` is laid out as a masonry tile, not over the screen (bug, medium)

`CatalogComponents.kt:535` emits `AppLoadingOverlay(visible = overlay, label = "Uploading…")` as a
direct child of `OverlaysSection`. That content is placed inside `MasonryColumns`
(`CatalogApp.kt:108-117`).

`AppLoadingOverlay` is an in-layout `AnimatedVisibility { Box(Modifier.fillMaxSize()…) }`
(`component/overlay/AppActionSheet.kt:134-155`), not a `Dialog`. `MasonryColumns` measures each
child with `Constraints(minWidth = columnWidth, maxWidth = columnWidth)` and an unbounded height
(`CatalogLayout.kt:29`), so `fillMaxSize` collapses to the content height. The overlay becomes a
column-wide scrim tile about 110dp tall, placed in whichever column is shortest, usually off-screen
at the bottom of the page.

Tapping "Block the screen" (`CatalogComponents.kt:532`) appears to do nothing, and the demo
contradicts its own caption (`:530`). `AppSnackbarHost` (`BoxScope`) cannot be demonstrated at all
for the same reason: section content has no full-screen overlay slot.

Fix: give `SectionPage` a `Box` with an overlay layer, and let a section contribute to it:

```kotlin
@Composable
private fun SectionPage(section: CatalogSection, onBack: (() -> Unit)?) {
    val overlays = remember { CatalogOverlays() }        // a mutableStateListOf of composables
    Box(Modifier.fillMaxSize()) {
        AppScaffold(...) { CompositionLocalProvider(LocalCatalogOverlays provides overlays) { MasonryColumns(...) { section.Content() } } }
        overlays.forEach { it() }                         // AppLoadingOverlay, AppSnackbarHost
    }
}
```

### C6. Window insets: last content hidden under the navigation bar; demo bottom bar double-padded (bug, low-medium)

- `CatalogActivity.kt:13` calls `enableEdgeToEdge()`. `SectionPage` scrolls with only
  `.padding(AppTheme.spacing.gutter)` (`CatalogApp.kt:111-114`), which is 16–32dp, and
  `AppScaffold` adds no navigation-bar inset (`AppScaffold.kt:45-71`). With 3-button navigation
  (48dp) the bottom of every section page stays under the bar even when fully scrolled.
- The index uses a fixed `contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl)` (32dp)
  instead of the inset (`CatalogApp.kt:153`).
- `ContainersSection` consumes the status-bar inset for the top-bar demos
  (`CatalogComponents.kt:281-285`) but not the navigation-bar inset for the bottom-bar demo
  (`:367-379`). `AppBottomBar` applies `navigationBarsPadding()` (`AppBottomBar.kt:117` (EdgeBar) and
  `:141` (FloatingBar)), so the inline demo bar carries a phantom gap the height of the system bar.

Fix: `Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))`
after `verticalScroll` in `SectionPage`, `WindowInsets.navigationBars.asPaddingValues()` in the
index, and `consumeWindowInsets(WindowInsets.navigationBars)` around the bottom-bar demo.

### C7. Index ↔ page crossfade shows the window background mid-transition (bug, low-medium)

`CatalogApp.kt:84-92` crossfades two opaque full-screen scaffolds,
`fadeIn(tween(medium)) togetherWith fadeOut(tween(quick))`, with nothing behind them. While both
are partly transparent, the activity window shows through. The window colour is fixed in XML
(`res/values/colors.xml:3` `#FFF7F8FA`, `values-night/colors.xml:3` `#FF0B0E14`), so:

- **Opposite theme forced:** Light picked on a dark-mode device (or the reverse) gives a visible
  dark (or light) flash on every navigation.
- **Other design styles:** Social (brand tint), Editorial (`#F7F4EE`, `StyleTones.kt:60`) and
  Playful backgrounds differ from `#F7F8FA`, so the page tints toward grey mid-fade.

The app's own tab switch (`core/navigation/.../NavTransitions.kt:105-113`, an `instant`-delayed
fade-in against a 135ms fade-out) has the same "two partly transparent screens" structure. That is
one plausible source of the "glitchy" tab switch reported in the overhaul brief, and worth checking
with the navigation audit.

Fix in the catalog: `Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
AnimatedContent(...) }`. Better still, use the real push/pop transition (see C9).

### C8. `MasonryColumns` ignores RTL (bug, low)

`CatalogLayout.kt:38`: `placeable.place(x, y)`. `place` does not mirror for
`LayoutDirection.Rtl`; `placeRelative` does. In an RTL locale the first group stays on the left.
Change it to `placeRelative`. That only matters once the catalog can show RTL at all (C10).

### C9. The catalog cannot demonstrate the navigation transitions the overhaul must fix (architecture, high)

The push, pop, modal and tab-switch specs live in
`core/navigation/src/main/kotlin/com/base/app/core/navigation/NavTransitions.kt:18`
(`internal object NavTransitions`). The catalog depends on `:core:designsystem` alone
(`catalog/build.gradle.kts:30`).

So the motion people complain about (the "pushy" bottom-bar and tab transitions, push/pop) cannot
be shown, tuned per motion style, or regression-checked in the catalog. The catalog's own
navigation is a plain crossfade (C7), and its "Animation" page (`CatalogMotion.kt`) only covers
in-place transitions. The bottom bar is shown as a static strip (`CatalogComponents.kt:367-379`)
with no content switching under it.

The specs depend only on Compose animation and `AppMotion` (`NavTransitions.kt:3-15`), so they
belong in the design system.

Fix:
1. Move them to `core/designsystem/animation/AppScreenTransitions.kt` as a public API. Keep
   `NavDisplay` wiring in `:core:navigation`.
2. Add a catalog section "Navigation" with these parts:
   - a mini shell: `AppBottomBar` or `AppNavigationRail` over a scrolling list per tab, switched
     with the real tab transition;
   - a list → detail push/pop using the real specs;
   - a `PredictiveBackHandler` that scrubs the pop transition;
   - all of it under the motion-style selector.

   This becomes the place to tune springs by hand before touching the app, and a deterministic
   target for frame-timing tests (B9).

### C10. No RTL, font-scale or reduce-motion toggles; the screenshot suite that covered them was deleted (dx/measurement, medium)

The index offers theme mode and design style (`CatalogApp.kt:157-174`), nothing else. Motion style
is only in the Feel page, and only for two groups (C1).

The deleted test `template/catalog/src/test/kotlin/com/base/app/catalog/CatalogScreenshotTest.kt`
(removed in 7b1a97a, "optimise project and add new features") rendered every section in light,
dark and light at **font scale 2.0**. Commit 8a13df1 records why: "Text that clips … does it there
and nowhere else". Nothing replaced it. Leftovers:

- `generator/genkit/render.py:52`: `_TEMPLATE_ONLY = {"catalog/src/test/screenshots"}` is now dead.
- `catalog/build.gradle.kts:17-18,33` keep the blank lines where the screenshot blocks were.
- `CatalogComponents.kt:593` explains the fixed date with a reason ("keeps the page identical every
  day") that only existed for the screenshot goldens. See commit f02aff5.

Reduce motion cannot be previewed: `rememberReduceMotion()` reads `Settings.Global` directly with
no composition-local override (`core/designsystem/.../theme/ReduceMotion.kt:10-20`).

Fix, at zero dependency cost: a "Preview environment" row in the index, applied around
`AppTheme`:

```kotlin
CompositionLocalProvider(
    LocalLayoutDirection provides if (env.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    LocalDensity provides Density(LocalDensity.current.density, env.fontScale),   // 1.0 / 1.3 / 2.0
    LocalReduceMotion provides env.reduceMotion,                                   // new, designsystem
) { AppTheme(mode, designStyle = env.designStyle, motionStyle = env.motionStyle) { … } }
```

`rememberReduceMotion()` would then read `LocalReduceMotion.current ?: <system setting>`. If the
screenshot suite comes back, bring it back as an opt-in feature, not a default. Robolectric and
Roborazzi are heavy for a "lighter generated apps" goal.

### C11. Duplication with the design system (ai-slop/drift, low-medium)

- **Motion-style descriptions:** `CatalogFeel.kt:132-138` (`AppMotionStyle.describe()`) restates
  the KDoc of each `AppMotionStyle` entry (`AppMotion.kt:22-41`). PeriFlow had to add its `Fluid`
  description in both places.
- **Stagger step:** `CatalogMotion.kt:229` `STAGGER_STEP_MILLIS = 28` copies
  `AppTransitions.STAGGER_MILLIS = 28` (`AppTransitions.kt:134`, `internal`). The "Delayed by N ms"
  labels (`CatalogMotion.kt:143`) will lie the day the token changes. The "capped at eight" caption
  (`:135`) is never demonstrated, because `STAGGER_ROWS = 6`.
- **Group header:** `CatalogGroup`'s header (`CatalogSection.kt:67-80`, `headingSmall` title plus a
  tertiary `caption`) duplicates `AppSectionHeader` (`component/data/AppDataDisplay.kt:189-228`).
- **Shape swatch:** `ShapeSwatch(name, shape: @Composable () -> Shape)` (`CatalogFoundations.kt:247`)
  takes a composable lambda only to read `AppTheme.shapes.x`, which the caller could pass directly.
- **Token lists:** every token row in `CatalogFoundations.kt` is listed by hand, which is how 11
  colours and 3 shapes went missing (C4).

Fix: make the description a property of the enum, `AppMotionStyle.summary` (one source, used by the
catalog and the website). Expose the stagger step publicly or remove the label. Add the drift
check in C13 so a hand list cannot fall behind.

### C12. Build file leftovers (bloat, low)

- `catalog/build.gradle.kts:14-15` sets `versionCode = 1` / `versionName = "1.0.0"`, which override
  `AppConfig.VERSION_CODE/NAME`, already applied by the convention plugin
  (`build-logic/.../AndroidApplicationConventionPlugin.kt:22-23`). A later bump in AppConfig will
  silently not reach the catalog.
- `catalog/build.gradle.kts:32` `implementation(libs.androidx.lifecycle.runtime.compose)` is
  unused: no catalog source imports `androidx.lifecycle`.
- `generator/genkit/readme.py:57` says "Only `:app` and `:catalog` carry flavours". `:catalog`
  applies no flavour plugin (`catalog/build.gradle.kts:5-8`). Commit 148d1d0 deleted the comment
  "No flavours: there is no environment for a catalog to point at", and the README still says the
  opposite.
- `CatalogComponents.kt` holds eight sections in 657 lines while every other section has its own
  file. Split it by section (`CatalogButtons.kt`, `CatalogInputs.kt`, …) so the catalog scales with
  the design system.

### C13. Nothing keeps the catalog in step with the design system (process, medium)

Evidence of drift across the three projects:

- **Template:** the 11 components and the tokens listed in C3/C4.
- **PeriFlow:** its catalog is the template's, with only two default-style edits. It shows none of
  its own additions: `glass`, `GlassStage`, `AuraBackground`, `ScreenEdgeBlur`, `AppGlassIcon`,
  `AppDetentSheet`, `AppMonthCalendar`, `AppCycleRing`, `AppVoiceOrb`, `AppTypingDots`,
  `AppContentSwap`, `AppNotice`, `scrollEdgeEffect`, among 40+ unreferenced names (script output).
  The liquid glass, the most FPS-sensitive code it has, has never been on the catalog.
- **VitFleet:** the counter-example. It added `CatalogFleet.kt`, `CatalogReports.kt` and new
  DateTime and Fields groups as it grew.

Neither the generated `AGENTS.md` §5 (`template/AGENTS.md:186-212`) nor any test asks for a catalog
entry when a component is added.

Fix (cheap, generator-side, runs in the existing `unittest` suite):

```python
# generator/tests/test_catalog_coverage.py
PUBLIC = re.compile(r"^\s*fun (?:<\w+> )?(?:Modifier\.|BoxScope\.)?(App[A-Z]\w*|[a-z]\w+)\(", re.M)
ALLOW = {"appTypography", "rememberAppFonts", "rememberCurrentLocale", ...}   # non-visual API
def test_every_design_system_component_is_in_the_catalog(self):
    names = {m for f in DS.rglob("*.kt") for m in PUBLIC.findall(f.read_text()) if "private" not in ...}
    catalog = "".join(p.read_text() for p in CATALOG.rglob("*.kt"))
    missing = sorted(n for n in names - ALLOW if not re.search(rf"\b{n}\b", catalog))
    self.assertEqual(missing, [], "add a CatalogGroup for these")
```

Add the same check for the `AppColors`, `AppShapes`, `AppSpacing` and `AppTypography` fields, plus
one line in the generated AGENTS.md §5: "A new component gets a `CatalogGroup` in its section."

### C14. Smaller correctness and UX notes (low)

- **Feel page card:** `CatalogFeel.kt:95-101` puts a clickable `AppCard` under "Press and hold,
  then release. The pop on release is the part that matters." The clickable card is built with
  `scaleOnPress = false` (`AppCard.kt:61`), so it has no pop to show.
- **Shared field value:** `CatalogComponents.kt:162-174` binds "Full name" and "With an error" to
  the same `text`, so typing in one edits both.
- **Index scroll lost:** `SectionIndex`'s `LazyColumn` (`CatalogApp.kt:151`) leaves composition on
  every push, because `AnimatedContent` disposes it. Its saveable scroll position is dropped and
  the index returns to the top.
- **Inline FQCN:** `CatalogComponents.kt:648` uses a fully-qualified
  `androidx.compose.ui.text.style.TextAlign.Center` inline instead of an import.

---

## 3. Benchmark and baseline-profile findings

### B1. `:benchmark` cannot resolve the flavoured app it targets (bug, critical)

`benchmark/build.gradle.kts:10-26` sets `targetProjectPath = ":app"` with no flavours and no
`missingDimensionStrategy`. The app has the `environment` dimension with
dev/staging/prod/playstore (`build-logic/.../Flavors.kt:9-23`).

The `baselineprofile` producer plugin copies only build types into the test module. Its own
comment says flavours must also match: "we need to replicate the same build types and flavors"
(plugin 1.5.0 `producer/BaselineProfileProducerPlugin.kt:165-170`). Only `createExtendedBuildTypes`
follows it.

Evidence that it fails in practice:

- PeriFlow commit 11e50cd: "The benchmark module never ran: with no flavour chosen the app's four
  were ambiguous."
- PeriFlow's fix is at `periflow/benchmark/build.gradle.kts:18-20`.
- VitFleet still has the template's version unchanged (`vitfleet/benchmark/build.gradle.kts`, which
  differs from the template only by a blank line), and has no generated profile.

Feature `baselineprofile` is in the `everything` preset and in `tests/spec_full.json`. CI builds
`check assembleDevDebug assembleProdRelease` (`.github/workflows/ci.yml:138`). That list probably
never compiles `:benchmark` against the app, which would be how this got through (not verified;
the PeriFlow commit is the proof that it fails).

Fix:

```kotlin
// benchmark/build.gradle.kts
defaultConfig {
    // Only the dev flavour is measured: it installs beside a real copy of the app, and without a
    // choice the four flavours are ambiguous.
    missingDimensionStrategy("environment", "dev")
}
```

Add a compile-only CI step that needs no device: `./gradlew :benchmark:assembleBenchmarkRelease`
in the `all` matrix entry.

### B2. Shipping variants would not get the profile: no `mergeIntoMain` (bug, high)

`app/build.gradle.kts:105-108` declares `baselineProfile(project(":benchmark"))` and nothing else.
The consumer default is "true for library modules and **false for application modules**" (plugin
`consumer/BaselineProfileConsumerExtension.kt:233-243`). Each variant then gets its own
`src/<variant>/generated/baselineProfiles`. A profile generated against dev (B1's fix) lands in
`src/devRelease/…`, so `prodRelease` and `playstoreRelease`, the two that ship, carry none.

PeriFlow fixed it at `periflow/app/build.gradle.kts:131-137`:

```kotlin
// <opt:baselineprofile>
baselineProfile {
    mergeIntoMain = true          // one profile, generated on dev, shipped by every flavour
    dexLayoutOptimization = true  // use startup-prof.txt to lay out the primary dex (see B6)
}
// </opt:baselineprofile>
```

### B3. The profile journey stops at onboarding in default projects (bug, high)

`BaselineProfileGenerator.kt:21-30` does `pressHome(); startActivityAndWait(); device.waitForIdle()`.
On a fresh install `MainActivity`'s entry point returns `OnboardingKey`, or `signIn`, before any
tab (`app/src/main/kotlin/com/base/app/MainActivity.kt:239-246`). `onboarding` and `auth` both
default to `True` (`generator/genkit/spec.py:256,277`). So the default project's profile covers splash
plus onboarding:

- It never covers the shell, `AppBottomBar`, `NavDisplay`, a list or a detail screen, which is
  what every returning user's cold start and first minutes run.
- `StartupBenchmark` (`StartupBenchmark.kt:28-37`) likewise measures cold start to the onboarding
  screen, not to the home tab.

There is no test hook either:

- no `testTagsAsResourceId` or `testTag` anywhere in the template (grep: zero hits), so
  UiAutomator can only match on localized text, and the template ships five languages;
- no `ReportDrawn`/`reportFullyDrawn`, so `StartupTimingMetric` cannot report
  `timeToFullDisplayMs`.

Fix:
1. **A benchmark-only start gate.** In `MainActivity`, honour an intent extra only on
   non-shippable flavours:

   ```kotlin
   // <opt:baselineprofile>
   val skipGates = !BuildConfig.IS_SHIPPABLE && intent.getBooleanExtra("benchmark.skipGates", false)
   // </opt:baselineprofile>
   ```

   The extra marks onboarding complete and installs a fake signed-in session through the existing
   stores. `BuildConfig.IS_SHIPPABLE` already exists (`Flavors.kt:20`).
2. **Stable selectors.** Add `Modifier.semantics { testTagsAsResourceId = true }` at the root in
   `AppRoot`, and `Modifier.testTag("tab_$index")` on each `BottomNavItem`/rail item
   (`core/navigation/.../AppShell.kt:184`), plus `"list"` on primary `LazyColumn`s. UiAutomator then
   uses `By.res("tab_0")`, independent of language and copy.
3. **TTFD.** `ReportDrawnWhen { state.isLoaded }` on the start tab's screen.

### B4. PeriFlow's hand-written journey failed silently; its "every tab" profile has no tab (bug, high: the pattern must not be ported as-is)

`periflow/benchmark/src/main/kotlin/com/jorwen/periflow/benchmark/BaselineProfileGenerator.kt`:

- `:37` `tap(tab) ?: return@forEach` skips a tab it cannot find, without failing.
- `:45-55` `repeat(MAX_ONBOARDING_STEPS) { … }` gives up after 14 steps without failing.
- `:58` finds tabs by English text (`By.text`), as its own KDoc admits (`:20-22`).

The committed profile `periflow/app/src/main/generated/baselineProfiles/baseline-prof.txt` (36,575
lines) contains:

- for `feature/today` only `TodayKey`, `TodayNavModule*` and `TodayViewModel_HiltModules*`. These
  are registered at startup by Hilt. There is no `TodayScreenKt` and no `TodayViewModel`.
- the same for Calendar, Insights, Learn and Chat: only `*Key`/`*NavModule`.
- zero `AppShell` classes and **zero `com/kyant` classes**. `GlassStage`, which calls Kyant's
  `rememberLayerBackdrop()`, was never composed. The only glass entries are `GlassKt.drawnGlass`,
  the cheap content-glass path.

So the walk never got past onboarding, while commit 11e50cd says "visits and scrolls each tab". The
liquid-glass draw path, the hot path for FPS, is the part the profile is meant to pre-compile, and
it is not in it.

Fix for the template version: every step asserts.

```kotlin
private fun MacrobenchmarkScope.tapTab(index: Int) {
    val tab = device.wait(Until.findObject(By.res("tab_$index")), TIMEOUT)
        ?: error("Tab $index not found — the journey would record nothing past this point")
    tab.click()
    check(device.wait(Until.hasObject(By.res("screen_tab_$index")), TIMEOUT)) { "Tab $index did not open" }
}
```

Then add a post-generation check: `grep -q 'AppShellKt' src/main/generated/baselineProfiles/baseline-prof.txt`
(and, with liquid glass on, `com/kyant/backdrop`). This can be a Gradle task in the app that runs
after `generateBaselineProfile`, or a line in the README run-book.

### B5. The generator's comment describes code that does not exist (ai-slop, medium)

`BaselineProfileGenerator.kt:28-29`:

```kotlin
// Scrolling is included because the first list frames are part of perceived startup.
device.waitForIdle()
```

Nothing scrolls. Either add the scroll (B7's journey) or delete the comment.

### B6. One `collect` with `includeInStartupProfile = true` mixes startup and journey (performance, medium)

`BaselineProfileGenerator.kt:23` sets `includeInStartupProfile = true` on the only `collect`. That
is fine while it only launches, but PeriFlow kept it after adding the walk (`periflow/...:31`).
Its `baseline-prof.txt` and `startup-prof.txt` are both exactly 36,575 lines, so the whole
onboarding walk went into the startup profile.

The startup profile drives the dex layout optimisation, which puts startup classes in the primary
dex. Filling it with everything defeats the purpose.

Fix: two tests.

```kotlin
@Test fun startup() = rule.collect(PACKAGE, includeInStartupProfile = true) { pressHome(); startToShell() }
@Test fun journeys() = rule.collect(PACKAGE, includeInStartupProfile = false) {
    startToShell(); visitEveryTab(); openAndCloseFirstDetail(); flingFirstList()
}
```

### B7. No frame-timing benchmarks at all (measurement, high)

The module has `StartupBenchmark` (cold only, `StartupBenchmark.kt:32`) and the generator. There
is no `FrameTimingMetric` anywhere in the template, PeriFlow or VitFleet, and no runtime tracing
(`androidx.tracing`, `runtime-tracing` and JankStats each grep to zero hits in all three projects).

The overhaul's goals are smooth spring transitions, a non-glitchy tab bar and push/pop, and liquid
glass at high FPS. Each of those is a frame-time claim nobody can check today. PeriFlow's only
glass FPS evidence is a hand measurement in a comment: `periflow/.../foundation/Glass.kt:141-144`,
"measured: two in three frames missed on a mid-range phone".

Missing tests, in priority order. Each uses `FrameTimingMetric()` and reads
`frameDurationCpuMs` P50/P90/P99 and `frameOverrunMs`:

| Test | setupBlock | measureBlock | Guards |
|---|---|---|---|
| `TabSwitchBenchmark` | start to shell (B3 gate), WARM | tap `tab_0…tab_n` twice round | the "pushy" tab switch: `AnimatedContent` over whole `NavDisplay`s (`AppNavHost.kt:99-105`) |
| `PushPopBenchmark` | start to shell | tap first row → wait for detail → `device.pressBack()` → wait for list, ×5 | `NavTransitions.push/pop` springs (`NavTransitions.kt:22-51`) |
| `PredictiveBackBenchmark` | open a detail | `device.swipe(1, h/2, w*0.6, h/2, 30)` edge swipe | `predictivePopTransitionSpec` (`AppNavHost.kt:126-129`) |
| `ScrollBenchmark` | open feed or sample list | `list.fling(DOWN)` ×3, `fling(UP)` | list item cost, `appAnimateItem` |
| `GlassBenchmark` (with liquid glass) | `@Parameterized glass: Boolean` via intent extra | scroll content under the glass bar, then a tab switch | Kyant `drawBackdrop` per-frame blur/lens cost on and off |
| `StartupBenchmark` | add `StartupMode.WARM`, `HOT` | — | resume path; TTFD once `ReportDrawnWhen` exists |

Sketch:

```kotlin
@RunWith(Parameterized::class)
class TabSwitchBenchmark(private val compilation: CompilationMode) {
    @get:Rule val rule = MacrobenchmarkRule()

    @Test fun switchTabs() = rule.measureRepeated(
        packageName = TARGET_APP_ID,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilation,
        startupMode = StartupMode.WARM,
        iterations = 8,
        setupBlock = { startToShell() },
    ) {
        repeat(2) { tabCount().forEach { tapTab(it) } }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun modes() = listOf(CompilationMode.None(), CompilationMode.Partial())
    }
}
```

Composition tracing (`androidx.compose.runtime:runtime-tracing`) plus `TraceSectionMetric("%GlassStage%",
Mode.Sum)` would attribute frame time to the glass composables instead of only reporting that a
frame was slow.

Frame timing on emulators or Gradle Managed Devices is not representative, so these run on a
physical mid-range phone. CI only compiles them (B1).

### B8. Wrong commands in the KDoc (docs, medium)

- `BaselineProfileGenerator.kt:12` says `./gradlew :benchmark:generateBaselineProfile`.
  `generateBaselineProfile` is registered only by the **consumer** plugin
  (`consumer/task/GenerateBaselineProfileTask.kt:31-49`), which is applied to `:app`. The producer
  registers no generate task. The correct command is `./gradlew :app:generateBaselineProfile`,
  which is what PeriFlow corrected it to.
- `StartupBenchmark.kt:14` says `./gradlew :benchmark:connectedBenchmarkAndroidTest`. AGP names the
  task per variant (`connectedBenchmarkReleaseAndroidTest`, with the producer's `benchmarkRelease`
  build type, `producer/BaselineProfileProducerPlugin.kt:99`). The plugin registers no aggregate,
  and Gradle's camel-case abbreviation does not skip the `Release` word.
- The generated README has no section on running either (`generator/genkit/readme.py`: no
  benchmark text).

### B9. Hard-coded target id and SDK numbers; the module bypasses the convention plugins (dx, low)

- `BaselineProfileGenerator.kt:38` and `StartupBenchmark.kt:40` hard-code `"com.base.app.dev"`. It
  works only because the generator renames the package and the dev suffix stays `.dev`
  (`AppConfig.kt:46`). The robust form, used by Android Studio's own template, passes the tested
  APK's id:

  ```kotlin
  androidComponents.onVariants { v ->
      val loader = v.artifacts.getBuiltArtifactsLoader()
      v.instrumentationRunnerArguments.put("targetAppId", v.testedApks.map { loader.load(it)?.applicationId!! })
  }
  // in tests:
  val TARGET_APP_ID = InstrumentationRegistry.getArguments().getString("targetAppId")!!
  ```

- `benchmark/build.gradle.kts:12,15,16,21-22,30` hard-code `compileSdk = 37`, `minSdk = 28`,
  `targetSdk = 37` and JVM 17, while every other module reads `AppConfig`
  (`build-logic/.../AppConfig.kt:12-25`), the one file the generator rewrites
  (`render.py:284-308`). Add a small `com.base.app.android.test` convention plugin that applies
  `configureAndroidCommon`, or at least read `AppConfig`.

### B10. Unused dependencies (bloat, low)

`benchmark/build.gradle.kts:39-40`: `androidx.test.ext.junit` and `androidx.test.espresso.core`
are never imported by either source file. Espresso is used nowhere else in the template. Its
catalogue entry (`gradle/libs.versions.toml:58,170`) exists only for this line. Keep `uiautomator`,
which the journeys need, and `benchmark-macro-junit4`.

### B11. The feature description over-promises (docs, medium)

`generator/genkit/spec.py:298-306`: "Typically a 20-30% cut in cold start for no code change". The
README table repeats the claim (`README.md:305`), and the website renders the catalogue. As
shipped, the module cannot build its test APK (B1), shipping flavours would not carry the result
(B2), and the profile would cover onboarding only (B3). It also defaults to `False`, so most
generated apps get no perf tooling at all.

Once B1–B3 are fixed: describe what it does ("records startup and the main journeys; measures
startup, tab switch, push/pop and scroll"), and consider making it default-on. It is a test-only
module with no runtime cost beyond `profileinstaller`.

---

## 4. Liquid glass (Kyant backdrop): what the catalog and benchmark need

PeriFlow uses `io.github.kyant0:backdrop:2.0.1` and `io.github.kyant0:shapes:1.2.1`
(`periflow/gradle/libs.versions.toml:43-44,78-79`). The design is sound for FPS:

- **Content glass is drawn, not sampled** (`Glass.kt:141-146`).
- **Only floating chrome samples a backdrop.** It records one shared `LayerBackdrop` in `GlassStage`
  (`Glass.kt:76-96`) and falls back to frosted tint without RenderEffect (`:149-155`).

If this becomes an optional template feature (`// <opt:liquidglass>`), the catalog and benchmark
should include the following.

**Catalog: a "Glass" section, behind `// <opt:liquidglass>`**

1. A `GlassStage` around the section page. Without it every chrome glass element takes the
   fallback (`LocalGlassLayers.current == null` → frosted). PeriFlow's catalog has no `GlassStage`
   anywhere, so its "Bloom" style bar (`BarTreatment.Glass`, `AppDesignStyle.kt:83`) has never
   rendered as real glass in the catalog.
2. The demos:
   - chrome glass (`AppBottomBar` with `BarTreatment.Glass`) floating over a scrolling list of
     high-contrast rows;
   - content glass (drawn) on cards and chips;
   - a forced-fallback toggle, showing what API < 31 users and dialogs get;
   - `GlassStrength.Subtle/Regular/Strong` side by side;
   - `ScreenEdgeBlur`.
3. A live frame-time readout. `androidx.metrics:metrics-performance` (JankStats) on the catalog
   activity only, showing jank % for the current page. Glass cost then shows up while designing,
   not after release. Catalog-only, so generated apps stay light.
4. A deep link into a section, `adb shell am start -n …/.CatalogActivity --es section Glass`, so
   benchmarks (below) and designers can open it directly.

**Benchmark**

- `GlassBenchmark` with glass on and off (B7), on one API 31–32 device (blur only) and one API 33+
  device (`lens` uses RuntimeShader).
- The profile journey must pass through a `GlassStage` screen, verified by the presence of
  `com/kyant/backdrop` in `baseline-prof.txt` (B4).
- Optional: a catalog-targeted test module (`:catalog-benchmark`, `targetProjectPath = ":catalog"`),
  measuring `FrameTimingMetric` on the Glass and Navigation sections. The catalog has no gates, no
  network and deterministic content, which makes it the cleanest jank lab for components.
  `com.android.test` allows one target, hence a second module. Make it opt-in with
  `baselineprofile`.

---

## 5. Worth back-porting from PeriFlow / VitFleet

- `periflow/benchmark/build.gradle.kts:18-20`: `missingDimensionStrategy("environment", "dev")`,
  with its comment.
- `periflow/app/build.gradle.kts:131-137`: `baselineProfile { mergeIntoMain = true }`, with its
  comment.
- `periflow/benchmark/.../BaselineProfileGenerator.kt:44-69`: the shape of the journey (onboarding
  helper, per-tab visit, fling down and up with a gesture margin). Port it with asserting steps,
  `By.res` selectors and a separate startup test (B4, B6). Do not port the silent `?: return`.
- `periflow/.../foundation/Glass.kt`: the `GlassDepth.Content` (drawn) vs `Chrome` (sampled)
  split, as the template's optional liquid-glass foundation, plus a catalog page that uses it.
- `vitfleet/catalog/.../CatalogFleet.kt` and `CatalogReports.kt`: one catalog section per domain
  as an app grows. Mention it in the generated AGENTS.md. Its `CatalogToday = LocalDate.of(...)`
  pattern passes a fixed "today" into date components and keeps the page deterministic.

---

## 6. Proposed shape after the overhaul

```
catalog/
  CatalogActivity.kt          reads ?section= extra
  CatalogApp.kt               index + environment bar (theme, style, motion, RTL, font scale, reduce motion)
  CatalogEnvironment.kt       CompositionLocal overrides around AppTheme
  CatalogSection.kt           enum + CatalogGroup (built on AppSectionHeader)
  CatalogLayout.kt            MasonryColumns (placeRelative) + overlay slot
  sections/Catalog{Foundations,Buttons,Inputs,Fields,Selection,Containers,Lists,Data,
                   Feedback,Overlays,Navigation,Motion,Feel,DateTime,Icons,Glass*}.kt
benchmark/
  build.gradle.kts            missingDimensionStrategy, targetAppId, test convention plugin
  Journeys.kt                 startToShell(), tapTab(), openFirstDetail(), flingList(): asserting
  BaselineProfileGenerator.kt startup() [startup profile] + journeys()
  StartupBenchmark.kt         COLD/WARM/HOT × None/Partial
  TabSwitchBenchmark.kt  PushPopBenchmark.kt  PredictiveBackBenchmark.kt  ScrollBenchmark.kt
  GlassBenchmark.kt*          (* = // <opt:liquidglass>)
generator/tests/test_catalog_coverage.py
```
