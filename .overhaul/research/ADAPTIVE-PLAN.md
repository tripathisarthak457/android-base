# Adaptive UI plan

Requirement 23, with 18, 22, 24, 25, 26 and 31. Written 2026-10-09 from `research/adaptive.md`, `research/standards.md`,
`research/CODE-STANDARDS.md`, the WP03 and WP04 worktrees, the WP02 worktree (`CodeRules.kt`, uncommitted M2 work),
the integration branch at `709612d`, the Google `adaptive` skill (v1.0.13) and the primary sources in §17.
Nothing in a repository or worktree was changed.

How to read this file:
- **Status** of each item: **APPLIED** (in code, with `file:line`), **PARTIAL**, **MISSING**.
- **Evidence:** **[V]** checked today in source, bytecode or a primary page; **[S]** secondary source; **[U]** not
  verified (check before relying on it); **[D]** our decision, with the reason.
- Item ids `AD-nn` are what the handoff notes cite.
- Paths are relative to `templates/android/` in the tree named: **WP03** = `$OV/wt/WP03`, **WP04** = `$OV/wt/WP04`,
  **WP02** = `$OV/wt/WP02`, **int** = the integration branch.

---

## 0. Decisions that change the research (read these first)

1. **Requirement 31 wins over `adaptive.md` §1.3 and its rule A10.**
   - `androidx.compose.material3.adaptive:{adaptive, adaptive-layout, adaptive-navigation, adaptive-navigation3}` 1.3.0
     are allowed. Their POMs carry no `material3` [V, adaptive.md §1.3].
   - List-detail and supporting pane use Material's `ListDetailSceneStrategy` and `SupportingPaneSceneStrategy`, as
     the skill prescribes. They sit behind our `NavPane` API, so features never import Material.
   - They go in only after the spike in §5.2 passes. The spike exists because these strategies install their own
     `NavigationBackHandler` and pane motion inside the scene [V, androidx-main `ThreePaneScaffoldScene.kt`], and that
     has to coexist with `BackGate` (requirement 1) and the Gmail card (requirement 24).
   - If one acceptance check fails and configuration cannot fix it, WP04 keeps its own `ListDetailScene` for that
     pane kind and records the evidence under "Deviations".
   - Every public member of both strategies is `@ExperimentalMaterial3AdaptiveApi` [V, androidx-main
     `adaptive-navigation3/api/current.txt`; the official recipe opts in]. The opt-in is confined to one file,
     `core/navigation/.../scene/PaneStrategies.kt`.
2. **Window classes and posture come from our own gated source, not `currentWindowAdaptiveInfoV2()`.**
   - We use the same breakpoints Material uses (`window-core` `WindowSizeClass.BREAKPOINTS_V2`) and the same posture
     rule. Two reasons:
     - the design system must stay byte-identical with the CMP `commonMain` copy (A7.2);
     - the root must recompose only when a breakpoint is crossed (requirement 22, §3.3).
     - Calling `currentWindowAdaptiveInfoV2()` at the root would subscribe it to every size change [U: check its
       source; the reasoning holds either way for CMP-iOS, which animates `containerSize` every frame, adaptive.md
       §1.10].
   - Material types appear only at the scene boundary: our `SizeClass` and `WindowPosture` are converted to a
     `WindowAdaptiveInfo` to compute the `PaneScaffoldDirective`.
3. **`Grid` and `FlexBox` are experimental in Compose 1.12.**
   - `foundation-layout-android` 1.12.0 and 1.12.1 ship `ExperimentalGridApi` and `ExperimentalFlexBoxApi` as
     `kotlin.RequiresOptIn`. `GridKt` and `FlexBoxKt` carry them. Both annotation classes are gone in 1.13.0-alpha03 [V,
     bytecode in `~/.gradle/caches`].
   - `standards.md` §2 #12, §4.4 and `CODE-STANDARDS.md` L-3 say "stable in 1.12". **That is wrong.** STANDARDS-PLAN
     carries the correction.
   - Use `LazyVerticalGrid(GridCells.Adaptive(...))` until the BOM moves to Compose 1.13 stable.
4. **One rule task.** The adaptive text rules become ids inside `verifyCodeRules` (`adaptive-*`). They use the same
   `// allow(<id>): <reason>` marker, and there is no separate `verifyAdaptiveRules` with its own marker grammar.
   Manifest rules get a small task of their own, `verifyManifestRules`, over the merged manifest (§11.3).
5. **The screenshot tool is Roborazzi 1.76.0** [V, release 2026-09-29]. Reasons are in adaptive.md §6.3:
   - it reuses the Robolectric `NATIVE` graphics setup already in WP03;
   - it can resize live, inject input and open overlays;
   - it covers KMP `androidHostTest`;
   - CPST's standalone plugin is deprecated from AGP 9.5.0-alpha03.

   `CODE-STANDARDS.md` T-5 (CPST, a 9-size matrix) is superseded by §11.5.
6. **WP02's `material` rule must exempt `material3.adaptive`.**
   - `CodeRules.kt` in WP02 (uncommitted) matches `^\s*import\s+androidx\.compose\.material(?!\w*\.ripple\b)`, which also
     catches `androidx.compose.material3.adaptive.*` [V].
   - Change: exempt `androidx.compose.material3.adaptive.` except `.navigationsuite`.
   - A new rule, `adaptive-material-scope`, confines the allowed imports to the two adaptive source folders (§11.1).
7. **Density overrides on the user's phone need the user's go-ahead.**
   - `wm density` on the Redmi changes a system display setting on a personal device. The adaptive.md §8 plan to
     measure large layouts that way is therefore gated: ask once, and restore with a trap.
   - Until the user agrees, large-layout perf runs on the emulator and counts as relative evidence only (Part D 12).

---

## 1. Work list, ranked

| Id | Item | Status today | Owner |
|---|---|---|---|
| AD-01 | Size classes provided as classes, gated by `derivedStateOf`; a scoped `containerClass` per pane; `LocalWindowMetrics` for exact reads | **MISSING**. `AppWindowSize.equals` compares exact dp and is provided through `staticCompositionLocalOf` (WP03 and WP04 `theme/AppWindowSize.kt:28-57,94-109`, `theme/AppTheme.kt:91-107` WP03 / `:67-78` WP04), so every dp of a resize recomposes the whole app | WP03 |
| AD-13 | Shell reads of the exact window size in composition | **MISSING**. WP04 `core/navigation/StackHost.kt:101` and `PresentationHost.kt:86` read `LocalWindowInfo.current.containerSize` | WP04 |
| AD-14 | Multi-instance safety: `AppNavigator`, `PendingDeepLink` per Activity | **MISSING**. Both are `@Singleton` (WP04 `AppNavigator.kt:40`, `DeepLinks.kt:61`). The manifest comment on `singleTop` works around it only for the launcher | WP04 |
| AD-15 | Manifest and Activity: `configChanges`, the multi-instance property, `<layout>`; posture and input sources; keyboard shortcuts | **MISSING**. WP04 `app/src/main/AndroidManifest.xml:50-61` has no `configChanges` | WP04 (manifest lines, as with `launchMode`), WP02-M2 (manifest check) |
| AD-10 | Chrome: bar → rail → sidebar, the "More" rule, rail fixes, RTL | **PARTIAL**. Bar and rail are switched at Medium (WP04 `AppShell.kt:121-170`). The rail has a fixed 68 dp item height and no scroll (WP04 `AppTabBar.kt:481-568`, QA-08). `trailingAction`/`accessory` are null in the rail (`AppShell.kt:146`). There is no sidebar, no tab cap (QA-09), and no RTL mirroring (QA-07) | WP04 (+WP02-M2 `tabs.py`) |
| AD-11 | Panes: `NavPane` gains `Extra`, `Main` and `Supporting`; Material strategies after the spike; pane context | **PARTIAL**. WP04 `ListDetailScene.kt` handles List/Detail, keyed by the list, with `equals` and `sameStack` | WP04 |
| AD-12 | Back and predictive back in two panes, with BackGate | **MISSING** (the detail swaps instantly; there is no gesture feedback) | WP04 |
| AD-20…26 | Enforcement: rules, lint, manifest task, previews, Roborazzi, audit and resize tests, generator assertions | **MISSING**, except three lint ids, which are **APPLIED** (WP04/WP02 `AndroidCommon.kt:47-51`) | WP02-M2, WP03, WP04 |
| AD-05 | Input parity: right-click defect, hover, pointer icons, Esc, Enter, scroll indicator, zoom, multi-select | **MISSING**. Right-click fires `onClick` (WP03 `foundation/Pressable.kt:296`, `detectTapGestures` [V adaptive.md §7.1]). No hover, pointer icon or shortcut code anywhere [V, grep] | WP03 (+WP04 shell shortcuts) |
| AD-04 | Components that adapt themselves: form sheet, popover action sheet, readable width with full-width scrolling, compact-height rules | **PARTIAL**. Only `AppSheet`, `AppActionSheet` and `AppToast` read `sheetMaxWidth` (WP03 `AppSheet.kt:264`, `AppActionSheet.kt:94`, `AppToast.kt:145`) | WP03 |
| AD-30 | Screens: `@AppScreenPreviews`, pane registration, grids, saveable state | **PARTIAL**. Sample and search register panes (WP04 `SampleNavigation.kt:33`, `SearchNavModule.kt:26`). Feed, settings and profile cap their width. There are 9 `@Preview`s across all features | WP02-M3 |
| AD-07/08 | Foldables: posture and hinge | **MISSING** | WP03 (types, components), WP04 (source, scenes) |
| AD-50 | CMP parity | not started | WP07 |
| AD-60 | SwiftUI parity | not started (no Swift written; WP08 worktree has none) | WP08 |
| AD-70 | Docs and the skill-differences note | not started | WP12 |
| AD-40/45 | Glass and devtools behaviour on large windows | not started | WP05, WP06 |

---

## 2. The adaptive contract

What every generated app guarantees, with the mechanism that enforces each clause. "Enforced by" names a build
failure where one exists; "review" means the reviewer checklist only.

| # | Contract | Enforced by |
|---|---|---|
| AC-1 | Layout decisions read `AppTheme.containerClass` (components, screens) or `AppTheme.windowClass` (shell only). Nothing reads orientation, device type, `Configuration.screenWidthDp` or `LocalWindowInfo` outside `foundation/adaptive/` | `adaptive-orientation`, `adaptive-screen-metrics`, `adaptive-device-type`; lint `ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`, `LocalContextResourcesRead` |
| AC-2 | No orientation or resizability lock in code or in the merged manifest | lint `LockedOrientationActivity`, `SourceLockedOrientationActivity`, `NonResizeableActivity` (APPLIED); `verifyManifestRules` |
| AC-3 | A resize, fold, rotation or keyboard attach does not recreate the Activity, rebuild a tab or lose a stack | manifest `configChanges` + `verifyManifestRules`; `ResizeContinuityTest` A and G |
| AC-4 | A resize inside one class costs 0 recompositions of screen roots and at most 1 of the chrome | `ResizeContinuityTest` C (recomposition harness from `RECOMPOSITION-PLAN.md`) |
| AC-5 | Navigation chrome follows the window: bar, rail or sidebar (§4). Tabs and their state survive every flip | `ResizeContinuityTest` A; `perf.shell` log `chrome=` in `adaptive.sh` |
| AC-6 | Multi-pane navigation is declared (`entry(pane = …)`) and laid out by the shell. A feature never builds its own two-pane `Row` for navigation | review; `adaptive-material-scope` keeps feature code off Material scene APIs |
| AC-7 | A detail beside its list has no Back arrow and is not full screen; the list marks the open row | `PaneContextTest` |
| AC-8 | Text never stretches past the readable width at Large and wider; lists still scroll from anywhere in the window | `AdaptiveAuditTest` "stretched text" |
| AC-9 | No clipped text, no unreachable or overlapping controls, and targets of at least 48 dp at the six PR sizes and font scales 1 and 2 | `AdaptiveAuditTest` |
| AC-10 | Every interactive component has hover, focus, keyboard activation and (where it has a long-press menu) a right-click menu. Esc closes the top overlay; Enter submits a single-line form with a hardware keyboard | `InputTest` (E); review |
| AC-11 | State that must survive a pane move or a process death lives in `rememberSaveable` or the ViewModel | `ResizeContinuityTest` B; review |
| AC-12 | Every `XScreen` has an `@AppScreenPreviews` preview of its stateless UI | `adaptive-preview` |
| AC-13 | Two windows of the app are independent: navigation in one never moves the other | `MultiInstanceTest` (F) |
| AC-14 | The template keeps all of the above in every preset, lean to everything | generator tests (§11.8) |

---

## 3. Breakpoints and the adaptive layer (WP03; AD-01, AD-02, AD-03)

### 3.1 Classes [V, `window-core` 1.5.1 `WindowSizeClass.kt`]

| Width class | dp | Height class | dp |
|---|---|---|---|
| Compact | < 600 | Compact | < 480 |
| Medium | 600–839 | Medium | 480–899 |
| Expanded | 840–1199 | Expanded | ≥ 900 |
| Large | 1200–1599 | | |
| ExtraLarge | ≥ 1600 | | |

Lower bounds are `WIDTH_DP_MEDIUM_LOWER_BOUND` (600), `…_EXPANDED_…` (840), `…_LARGE_…` (1200), `…_EXTRA_LARGE_…`
(1600), `HEIGHT_DP_MEDIUM_LOWER_BOUND` (480) and `HEIGHT_DP_EXPANDED_LOWER_BOUND` (900). The computation stays as today:
`WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(widthDp, heightDp)` (WP03 `AppWindowSize.kt:32`). The classes
are decided on window width first; height adds phone-landscape rules. They are never decided by device type.

### 3.2 API (`core/designsystem/.../foundation/adaptive/`, identical in CMP `commonMain`)

```kotlin
enum class WindowWidth { Compact, Medium, Expanded, Large, ExtraLarge }   // unchanged
enum class WindowHeight { Compact, Medium, Expanded }                     // unchanged

/** A window's or a pane's size classes. Equal unless a breakpoint is crossed. */
@Immutable
data class SizeClass(val width: WindowWidth, val height: WindowHeight) {
    val hasRoomForTwoPanes: Boolean get() = width >= WindowWidth.Expanded
    val isShort: Boolean get() = height == WindowHeight.Compact
    companion object {
        fun of(widthDp: Float, heightDp: Float): SizeClass   // BREAKPOINTS_V2, as today
        val Phone: SizeClass                                  // 411×891, previews and bare tests
    }
}

/** Whether a foldable is half open, and where its separating hinge is. */
@Immutable
data class WindowPosture(val kind: Kind = Kind.Flat, val hinge: Rect? = null) {
    enum class Kind { Flat, Tabletop, Book }
}

/** What the person can type and point with right now. */
@Immutable
data class InputTraits(val finePointer: Boolean = false, val hardwareKeyboard: Boolean = false, val touchscreen: Boolean = true)

/** The exact size, read only inside lambdas (transition specs, layout); never in composition. */
@Stable
class WindowMetrics internal constructor(private val info: WindowInfo, private val density: Density) {
    val sizeDp: DpSize get() = with(density) { info.containerSize.toSize().toDpSize() }
}

AppTheme.windowClass: SizeClass        // the shell: chrome, pane count
AppTheme.containerClass: SizeClass     // components and screens: the pane they are in
AppTheme.layout: AppLayout             // tokens for containerClass
AppTheme.posture: WindowPosture
AppTheme.input: InputTraits
LocalWindowMetrics: WindowMetrics      // for StackHost/PresentationHost lambdas
@Composable fun ProvideContainerWidth(widthDp: Float, content: @Composable () -> Unit)
```

`AppTheme` gains `posture: WindowPosture = WindowPosture()` and `input: InputTraits = InputTraits()`. `:app` passes them
(§7, §9.1). Previews and the catalog keep the defaults.

Removed: `AppWindowSize`, `AppTheme.windowSize`, `rememberAppWindowSize()` and `AppLayout.forWindow(AppWindowSize)`
(replaced by `AppLayout.forClass(SizeClass)`). There are three call sites outside the design system:
- `AppShell.kt:118` (WP04) → `AppTheme.windowClass`;
- `AdaptiveLayout.kt:37` → `AppTheme.containerClass`;
- feature code has none [V, grep].

### 3.3 Gating (the requirement 22 part)

```kotlin
@Composable
internal fun rememberWindowClass(): SizeClass {
    val info = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowClass by remember(info, density) {
        derivedStateOf {
            val size = info.containerSize
            if (size.width == 0 || size.height == 0) SizeClass.Phone
            else with(density) { SizeClass.of(size.width.toDp().value, size.height.toDp().value) }
        }
    }
    return windowClass
}
```

- `AppTheme` reads the derived state, so it recomposes only when the class changes.
- Locals:
  - `LocalWindowClass` and `LocalContainerClass` are static: a change means a relayout anyway.
  - `LocalWindowMetrics` is static: its identity is stable and its reads are snapshot reads.
  - Posture and input traits are static.
- `ProvideContainerWidth(widthDp)` re-provides `LocalContainerClass` (height unchanged) and `LocalAppLayout` for one pane.
  - The width comes from tokens: a list pane is `listPaneWidth`.
  - A detail pane is `window − chrome − list`. Its class comes from a derived state too, so it changes only at
    breakpoints.
- Gate (AC-4, `ResizeContinuityTest` C): resize 900 → 1000 → 1100 dp gives 0 recompositions of screen roots and at
  most 1 of `AppShell`'s chrome.
- Expected effect: a fold or a desktop drag-resize stops recomposing the whole tree. On CMP-iOS a rotation drops from N
  full-tree recompositions to at most 1 [U, measure on the iOS CI job].

### 3.4 Tokens per class (`AppLayout.forClass`) [D; numbers from Material's pane directive and Apple's readable widths]

| Token | Compact | Medium | Expanded | Large | ExtraLarge |
|---|---|---|---|---|---|
| `gutter` | 16 | 24 | 24 | 32 | 32 |
| `listPaneWidth` | — | — | 360 | 412 | 412 |
| `supportingPaneWidth` (new) | — | — | 360 | 412 | 412 |
| `railWidth` | — | 88 | 88 | 88 | 88 |
| `sidebarWidth` (new) | — | — | 280 | 280 | 304 |
| `readableMaxWidth` | full | 720 | 720 | 720 | 720 |
| `formMaxWidth` | 480 | 480 | 480 | 480 | 480 |
| `contentMaxWidth` | full | full | 1040 | 1040 | 1200 |
| `sheetMaxWidth` | 640 | 640 | 640 | 640 | 640 |
| `formSheetWidth` (new) | — | 540 | 540 | 600 | 600 |
| `gridMinCellWidth` → two tokens: `gridMinTileWidth` / `gridMinCardWidth` (new) | 160 / 300 | same | same | same | same |

Today `AppLayout.forWindow` (WP03 `AppWindowSize.kt:84-91`) has `listPaneWidth = 320` at Medium, which no layout
uses (one pane at Medium); drop it. Pane counts follow Material's directive:
- 1 pane below Expanded;
- 2 panes at Expanded;
- 3 partitions at Large and wider when an `Extra` entry exists.

The spacer is **0 dp** (the recipe's override for b/418201867) [V, recipe in the skill]. A 1 dp separator is drawn
instead.

### 3.5 Compact height (a phone held sideways, a short split window)
- The rail replaces the bar. Rail labels are hidden when the height class is Compact.
- Large titles render inline (`AppLargeTitleTopBar` collapses to its inline form).
- A sheet opens at full height, capped at `sheetMaxWidth` and centred.
- `AppTwoPane` puts its halves side by side (`shouldUseTwoPanes`, WP03 `AdaptiveLayout.kt:35-40`, already does this).
  It reads `containerClass`.

---

## 4. Navigation chrome: bar → rail → sidebar (WP04; AD-10)

### 4.1 Policy (`ShellChromePolicy`, one function, unit-tested)

| Window (`windowClass`) | Chrome | Items shown | Search / trailing action | Accessory |
|---|---|---|---|---|
| height Compact (any width) | rail, labels hidden | ≤ 7, else 6 + More; `verticalScroll` fallback | rail header | rail footer |
| width Compact | floating bar (existing) | ≤ 5, else 4 + More (requirement 18, QA-09) | trailing capsule (existing) | above the bar (existing) |
| width Medium or Expanded | rail, 88 dp; at Expanded a sidebar toggle sits in the rail header (collapsed by default) | ≤ 7, else 6 + More | rail header | rail footer |
| width Large or ExtraLarge | sidebar, 280/304 dp, collapsible to the rail | every tab, grouped by `ShellTab.section`; rows `heightIn(min = 48.dp)` | a search field at the top of the sidebar (iPadOS) | bottom of the sidebar |

```kotlin
enum class ShellChrome { Bar, Rail, Sidebar }
internal fun chromeFor(window: SizeClass, sidebarCollapsed: Boolean): ShellChrome = when {
    window.height == WindowHeight.Compact -> ShellChrome.Rail
    window.width == WindowWidth.Compact -> ShellChrome.Bar
    window.width >= WindowWidth.Large && !sidebarCollapsed -> ShellChrome.Sidebar
    else -> ShellChrome.Rail
}
```

- **Keep the one `TabbedNavHost` call site** (WP04 `AppShell.kt:121-170`, APPLIED). Only the chrome changes, so tabs,
  stacks and ViewModels survive every flip.
- **"More".**
  - The shell owns a `MoreKey` root, listing the overflow tabs as `AppRow`s with their icons and badges.
  - Choosing one selects that tab. The bar highlights "More" while an overflow tab is in front.
  - Back at an overflow tab's root returns to the More list (`ShellState.canBackAtRoot`).
  - `ShellTab` gains `section: Int? = null` (a string resource, for sidebar groups).
  - Generator side (WP02-M2, QA-09, QA-10):
    - `tabs.py` stops making every scaffolded module a bar item when more than 5 tabs result. The extras go to
      overflow.
    - Each scaffolded tab gets a distinct icon pair (`icon`, `selectedIcon`) from a fixed list of `AppIcons` glyphs
      instead of `AppIcons.Grid` (`tabs.py:16`).
- **Rail fixes (QA-08):**
  - Items are `heightIn(min = 56.dp)` in a `Column(Modifier.verticalScroll(…))`, so 6 or more tabs and font scale 2
    no longer clip.
  - The selection indicator is still drawn in the draw phase from the scroll-adjusted item offsets.
  - Pass `trailingAction` and `accessory` to the rail header and footer (`AppShell.kt:146` passes `null` today), so
    search and a mini-player stay on wide windows.
- **RTL (QA-07):** `placeRelative`/`placeRelativeWithLayer` in `AppTabBar.kt:412-417`, and the pill and rail
  indicator mirrored in `:381`.
- **Sidebar collapse and expand.**
  - Lay out once and animate transforms. The content takes its final width in the first frame (one relayout).
  - The sidebar's clip, the sidebar's `translationX` and the content's `translationX` animate from the old offset to 0
    on `springs.layout`, inside `graphicsLayer`.
  - There is no per-frame relayout and no per-frame recomposition. Gate: `sidebar_toggle` (§12.6).
- **A class flip is not animated** (Android and iPadOS both relayout at once).
- **Collapsed state** is a saveable in `ShellState`, persisted when the app has a settings store.
- **Keyboard:**
  - Ctrl+Shift+S toggles the sidebar (⌃⌘S on iOS) [D].
  - The bar, rail and sidebar are each a `focusGroup()` with `focusRestorer()`.
- **New file:** `core/designsystem/.../component/navigation/AppSidebar.kt` (WP04, like `AppTabBar.kt`):
  - It draws the selected-row highlight on `springs.selection` in the draw phase.
  - It shares `TabItem` with the bar.
- **Insets:**
  - The rail and sidebar keep `safeDrawing.only(Start)` (today's rail does this).
  - With 3-button navigation held sideways, content must clear the **end**-side navigation bar [U: check on the
    Redmi in landscape and on the AVD].
- **Predictive back** (SPEC.md): the card shrinks inside the content area. The rail and sidebar stay still; the host is
  already `clipToBounds()` beside a rail (`AppShell.kt:153-157`).
- **Dev log:** `ShellLog` (`app/src/dev/.../perf/`) adds `window=<w>x<h>dp class=<W>x<H> chrome=<bar|rail|sidebar>`
  to each line, which the device script asserts (§12).

---

## 5. Panes and scenes (WP04; AD-11, AD-12)

### 5.1 Feature API (unchanged shape, more kinds)

```kotlin
enum class NavPane { Single, List, Detail, Extra, Main, Supporting }
navGraph {
    entry<OrderListKey>(pane = NavPane.List) { OrderListRoute() }
    entry<OrderDetailKey>(pane = NavPane.Detail) { key -> OrderDetailRoute(key.orderId) }
    entry<OrderHistoryKey>(pane = NavPane.Extra) { … }            // third column from Large
    entry<MapKey>(pane = NavPane.Main) { … }
    entry<PlaceInfoKey>(pane = NavPane.Supporting) { … }          // beside Main from Expanded
}
```

- The registry maps `NavPane` to the Material metadata **inside `:core:navigation`**:
  - `listPane(sceneKey, detailPlaceholder = { NothingSelected() })`, `detailPane(sceneKey)`, `extraPane(sceneKey)`;
  - `mainPane(sceneKey)`, `supportingPane(sceneKey)`;
  - plus `paneAnimation(EnterTransition.None, ExitTransition.None)` [U: present in 1.3.0? It is on androidx-main;
    otherwise the default pane motion is accepted only if the spike's no-alpha check passes].
- `sceneKey` = the feature's `NavGraphEntry`, so a list and a detail from different features never merge into one
  scene.
- Rename WP04's internal `ListDetailSceneStrategy` (`ListDetailScene.kt:58`) to `PaneSceneStrategy`. It keeps
  existing as the fallback, and the name no longer shadows Material's class.

### 5.2 Strategy choice, the spike, and the fallback

```kotlin
// core/navigation/.../scene/PaneStrategies.kt: the one file that opts in
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberPaneStrategies(window: SizeClass, posture: WindowPosture, layout: AppLayout): List<SceneStrategy<AppNavKey>> {
    val directive = remember(window, posture, layout) {
        calculatePaneScaffoldDirective(window.toAdaptiveInfo(posture), HingePolicy.AvoidSeparating)
            .copy(horizontalPartitionSpacerSize = 0.dp, defaultPanePreferredWidth = layout.listPaneWidth)
    }
    val listDetail = rememberListDetailSceneStrategy<AppNavKey>(directive = directive)   // single pane: returns null
    val supporting = rememberSupportingPaneSceneStrategy<AppNavKey>(directive = directive)
    return remember(listDetail, supporting) { listOf(listDetail, supporting) }
}
// window.toAdaptiveInfo(): WindowAdaptiveInfo(WindowSizeClass(minWidthDp = lowerBound(width), minHeightDp = lowerBound(height)),
//                                             Posture(isTabletop = posture.kind == Tabletop, hingeList = …))
```

- Order in `StackHost`: `[sheet (WP03), panes…, the single-pane stack scene]`.
  - The single-pane scene is today's `KeepUnderScene`, or the `StackSceneStrategy` that `SMOOTHNESS-PLAN.md` proposes
    in its place. Either way the pane strategies come first, as `ListDetailSceneStrategy` does today
    (`StackHost.kt:94-99`).
  - `shouldHandleSinglePaneLayout = false` (the default) keeps Compact windows on our scenes. That means our push/pop,
    the kept-under entries and the Gmail card [V, `rememberListDetailSceneStrategy` signature].
- **Spike acceptance** (WP04; all must pass on Robolectric plus one emulator run, before the swap is merged):
  1. **Rapid Back.** At 1280×800 with list + detail 3, three Backs during a detail swap land on the expected entry
     and never go past the list (`RapidBackTest` variant).
  2. **Gesture.** A predictive-back gesture on the detail seeks Material's pane motion and cancels cleanly.
     - A second gesture during pane motion is consumed or coalesced.
     - BackGate's consuming handler still wins while a NavDisplay transition runs.
  3. **Fold, unfold, resize.** List + detail at 411 ↔ 673 ↔ 1280: no NavDisplay transition (`sameStack`, the existing
     `SceneChangeTest`). Tab, stack and the selected detail are unchanged.
  4. **Detail swap.** Selecting row 2 after row 1 shows no alpha below 1 on any pane-sized layer (layer inspection or
     the `GraphicsLayer` hook in the test).
     - No recomposition per frame beyond the new detail's first composition (the recomposition harness).
     - No bounds animation that relayouts per frame, unless `paneAnimation(None)` disables it.
  5. **Perf.** `listdetail_select` (§12.6) meets its gate.
  6. **Compact.** At Compact the Gmail card, the single-pane stack scene and BackGate behave exactly as before
     (existing tests green).
  7. **Size.** The R8 release APK grows by at most 150 KB [D].
- **Fallback.** A failed check that configuration cannot fix keeps `PaneSceneStrategy` for that pane kind. Extend the
  current `ListDetailScene` with Extra and Supporting, the hinge split and pane context. Record the measurement in
  "Deviations".
- **Gmail parity note.** On a tablet, Gmail is itself a Material list-detail app. Material's pane predictive back (a
  scaffold seek with `CubicBezierEasing(0.1, 0.1, 0, 1)`, progress ratio 0.15 for two panes [V, androidx-main
  `ThreePaneScaffoldScene.kt`]) *is* the Google-app behaviour on large windows. The SPEC.md card applies to
  single-pane pops.

### 5.3 Pane context (both paths)
- An entry decorator in `:core:navigation` computes `LocalPaneContext(role, twoPane, detailDepth, selectedKey)`:
  - `twoPane` = `directive.maxHorizontalPartitions >= 2` and an adjacent List/Main entry exists in the stack;
  - it wraps the entry in `ProvideContainerWidth(paneWidth)` (§3.3).
- `AppTopBar` (WP03) hides the Back arrow when `role in {Detail, Supporting} && twoPane && detailDepth == 1`.
- The list screen reads `selectedKey` to mark the open row `selected` (iPad Mail, Settings).
- A `FullScreenKey` detail does not hide the chrome while `twoPane` (a skill rule).
- `PaneContextTest`: the arrow is hidden or shown at 411 and 1280, and the selected row is marked.

### 5.4 Back in two panes
- Back pops the detail stack first, then closes the detail and shows the placeholder (Material
  `BackNavigationBehavior.PopUntilScaffoldValueChange`, or the same rule in the fallback), then defers to the shell.
- BackGate counts presses during a NavDisplay transition as today. Pane-internal motion (Material) is interruptible
  through its own seek, so it does not need BackGate.

### 5.5 State continuity
- Moving between a pane scene and the single-pane scene recreates the entry's composition. What survives:
  `rememberSaveable` (saveable decorator) and ViewModels (ViewModel-store decorator). What is lost: plain `remember`.
- Rule for screens (AC-11): scroll state, text, expansion and selection are saveable (`rememberLazyListState` is).
- `ResizeContinuityTest` B (§11.6).
- Nav3 1.2's `SharedEntryInSceneNavEntryDecorator` may keep the composition across scenes [U: spike only if B shows a
  visible cost].

### 5.6 Canonical layouts
- **List-detail:** §5.1–5.4. Sample and search register it already. Settings (sections → detail) and licenses →
  detail are WP02-M3 candidates.
- **Supporting pane:** `NavPane.Main` + `Supporting`. Below Expanded the supporting entry is pushed (Material
  single-pane behaviour). Showing it as a sheet below Expanded (iOS `.inspector`) is a later option, not v1 [D].
- **Feed grid:** `LazyVerticalGrid(GridCells.Adaptive(AppTheme.layout.gridMinCardWidth))`.
  - Headers use `span = { GridItemSpan(maxLineSpan) }`, `key` and `contentType` are given, and `contentPadding` comes
    from the gutter and `chromeContentPadding()`.
  - In a pane the grid gets fewer columns by itself.
  - `Grid`/`FlexBox`: only after the BOM reaches Compose 1.13 stable (§0.3).

---

## 6. Components that adapt themselves (WP03; AD-04)

Every rule reads `AppTheme.containerClass` (so it is right inside a pane), never the window.

| Component | Rule |
|---|---|
| `AppSheet` | Compact width: bottom sheet with detents (today's). Width ≥ Medium **and** height ≥ Medium: **form sheet**, centred, `min(formSheetWidth, width − 2·gutter)` × `min(0.85·height, content)`, all corners rounded, scale-in on the alert spring, detents and drag-to-dismiss off, close button on (iPad). Compact height: full height, `sheetMaxWidth`, centred |
| `AppActionSheet` | Compact: bottom (today's). Regular: popover anchored to the source bounds, `AppMenu` style |
| `AppMenu` / context menu | Width already bounded. On a right-click it anchors at the pointer, with no lift-and-preview (desktop convention). Arrow keys, Enter, and Esc to close |
| `AppAlert` | Fixed 300 dp; never full width (APPLIED) |
| `AppToast` | Centred over the content area, not the window, when a rail or sidebar is shown: the shell provides the content insets |
| `AppSection`, `AppRow`, settings rows | Items apply `centeredMaxWidth(readableMaxWidth)` from Expanded. The lazy list stays full window width, so the wheel scrolls anywhere and the scroll indicator sits at the window edge. `AppScaffold(contentMaxWidth)` (`AppScaffold.kt:46-52`) narrows the whole column today; for lists, move to item-level width |
| `AppTopBar`, `AppLargeTitleTopBar` | Inline title at Compact height. No Back arrow at detail depth 1 in two panes (§5.3). With a sidebar, the title starts after it |
| `AppBottomActionBar` | Spans its pane, not the window |
| `AppImageViewer` | Ctrl+wheel and trackpad pinch zoom; ←/→ between images; tabletop: image above the fold, controls below |
| `AppPullToRefresh` | With a fine pointer and no touchscreen, the shell's Ctrl+R/F5 calls the same `onRefresh`. Pulling stays the touch gesture |
| `AppSwipeActions` | The same actions are reachable from the row's context menu (right-click, long-press) and from keyboard focus. A swipe-only action fails Tier 2 for mouse users |
| `AppReorderableList` | With a fine pointer, dragging the handle starts at once (no long press). Ctrl/Shift multi-select where selection exists |
| `AppCarousel`, `AppPageControl` | ←/→ keys. With a fine pointer, previous/next buttons appear on hover |
| `AppTextField` | `onSubmit`: Enter submits when `input.hardwareKeyboard`; Shift+Enter adds a newline. Handwriting already works through `BasicTextField` [V adaptive.md §7.2] |
| `AppSearchField` used as a button | `Modifier.handwritingDetector { … }` focuses the real field |
| `AppDatePicker` | Two months side by side at Expanded and wider (optional) |
| `AppTabRow`, `AppSegmentedControl` | Width from content; cap at `contentMaxWidth` and centre |
| `AppDataTable` | Shift+wheel scrolls horizontally; column widths grow at Expanded |
| `AppTwoPane` | Book posture: split at the hinge. Tabletop: top and bottom of the fold |

Catalog (WP03): an **Adaptive** section with a resizable frame. A drag handle sets the frame width and calls
`ProvideContainerWidth`, so every component can be seen crossing breakpoints. A posture toggle injects `WindowPosture`;
an input toggle injects `InputTraits`.

---

## 7. Foldables (AD-07, AD-08)

- **Source:** one per Activity, `:app` `MainActivity` (WP04). `androidx.window:window` 1.5.x is already on the runtime
  classpath through Compose UI [V adaptive.md §11]; declare `androidx.window:window:1.5.1` in `:app` to compile against
  it.

  ```kotlin
  @Composable
  private fun rememberWindowPosture(activity: ComponentActivity): WindowPosture {
      val flow = remember(activity) {
          WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
              .map { it.toPosture() }.distinctUntilChanged()
      }
      return flow.collectAsStateWithLifecycle(WindowPosture()).value
  }
  // toPosture(): the first FoldingFeature with state == HALF_OPENED → Tabletop (HORIZONTAL) or Book (VERTICAL);
  // hinge = bounds when isSeparating (window coordinates); FLAT, non-separating features are ignored.
  ```

  The rule matches Compose's `resolvePosture` and Material's `calculatePosture` [V adaptive.md §11].
- **Tabletop** (Tier 1 `Foldables_Postures`):
  - `AppImageViewer` and media screens put the image above the fold and the controls below it.
  - `AppSheet`'s medium detent rests on the fold line.
  - A list-detail may stack top and bottom (Material allows two vertical partitions in tabletop [V]).
- **Book:** list-detail splits at the hinge (Material `HingePolicy.AvoidSeparating`), and `AppTwoPane` uses the hinge
  as its divider. The hinge rect is in window pixels. The scene captures its window offset with `onPlaced` into a
  state read in measure: one relayout per fold change.
- **Continuity:** a fold or unfold changes the class. The scene changes without a transition (`sameStack`), and
  `configChanges` prevents recreation [U: whether the Pixel Fold's two screens differ in `densityDpi`; a density change
  still recreates, which `ResizeContinuityTest` G and the `restore` scenario cover].
- **Tests:** inject `WindowPosture` in Robolectric (§11.6 D). On the emulator, the Pixel Fold AVD with `adb emu fold`,
  `unfold` and `posture 2` (§12.3).

---

## 8. Desktop windowing, multi-window, multi-instance (AD-14, AD-15)

1. **No recreation on size, keyboard or navigation changes** (manifest, §10). The rule for `AndroidView` and
   `AndroidFragment` hosts: handle `onConfigurationChanged` themselves [V, runtime-changes page].
2. **Per-window navigation.**
   - `AppNavigator` and `PendingDeepLink` become `@ActivityRetainedScoped`, and `NavigationModule` installs in
     `ActivityRetainedComponent`. ViewModels still inject them, because `ViewModelComponent` is its child, and they
     survive configuration changes [V, dagger.dev/hilt/components].
   - This matches the Navigation 3 Hilt recipe and MASTER-PLAN A3.1 ("scoped per Activity").
   - The `singleTop` manifest comment stops being the only defence and should say so.
3. **Multi-instance system UI** (Android 15+):
   `<application><property android:name="android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI" android:value="true"/></application>`
   [V, desktop-windowing page: it goes on `<application>`].
   - "Open in new window" opens a new instance with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK |
     FLAG_ACTIVITY_LAUNCH_ADJACENT`.
   - A recipe and a catalog demo are enough; no feature needs it by default.
4. **Default desktop window:**
   `<layout android:defaultWidth="1024dp" android:defaultHeight="720dp" android:minWidth="320dp" android:minHeight="400dp"/>`
   in `MainActivity` [U: how desktop windowing and Googlebook honour `<layout>`; the desktop page documents
   `ActivityOptions.setLaunchBounds()` instead].
5. **Caption bar:** leave the system header as it is. `safeDrawing` includes `captionBar` through `systemBars` [U: check
   on a desktop image]. Drawing under a transparent header (`APPEARANCE_TRANSPARENT_CAPTION_BAR_BACKGROUND`,
   `getBoundingRects`) is out of scope for v1.
6. **Precise pointer:** `WindowInfoTracker.windowEngagementInfo(...)` with `PRECISE_POINTER` is in window
   1.6.0-alpha05 only [V adaptive.md §1.8]. Until it is stable, `InputTraits.finePointer` comes from the device scan
   (§9.1).
7. **Per-window UI state** in other packages: the overlay host and its `LocalBottomChrome` holder are per `AppTheme`
   root, so per window (APPLIED by design, WP03). The devtools badge and panel and the glass stage must not keep UI
   state in singletons (§16, WP05, WP06).

---

## 9. Input (WP03 components, WP04 shell; AD-05)

### 9.1 Input traits source (`MainActivity`, one listener per window)

```kotlin
@Composable
private fun rememberInputTraits(activity: ComponentActivity): InputTraits {
    val input = remember(activity) { activity.getSystemService(InputManager::class.java) }
    var traits by remember { mutableStateOf(input.traits()) }
    DisposableEffect(input) {
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { traits = input.traits() }
            override fun onInputDeviceRemoved(deviceId: Int) { traits = input.traits() }
            override fun onInputDeviceChanged(deviceId: Int) { traits = input.traits() }
        }
        input.registerInputDeviceListener(listener, null)
        onDispose { input.unregisterInputDeviceListener(listener) }
    }
    return traits
}
// traits(): finePointer = any device with SOURCE_MOUSE, SOURCE_TOUCHPAD or SOURCE_STYLUS and a valid X motion range
// (Compose's own resolvePointerPrecision rule [V]); hardwareKeyboard = KEYBOARD_TYPE_ALPHABETIC && !isVirtual;
// touchscreen = any SOURCE_TOUCHSCREEN.
```

This is one registration per window, not one per popup as with `mediaQuery` (adaptive.md §1.4). Do not set
`ComposeUiFlags.isMediaQueryIntegrationEnabled` (rule `adaptive-mediaquery`).

### 9.2 Defects and items

| Item (tier id) | Change | Where |
|---|---|---|
| **Right-click fires `onClick`** (defect) | In `PressableNode`, a press where every change is `PointerType.Mouse` and `!buttons.isPrimaryPressed` goes to `onSecondaryClick(offset)` and never to `onClick`. This mirrors foundation's internal `RightClickGestures.kt:43-49` rule `isSecondaryPressed && changes.fastAll { it.changedToDown() }` [V] | WP03 `foundation/Pressable.kt:293-296` |
| Context menus (`Context_Menus`) | `pressable(onSecondaryClick = …)`. `AppRow` and `AppCard` open the same `AppMenu` as long-press, anchored at the pointer | WP03 |
| Hover (`Hover_States`, `Hover_Parity`) | `PressableNode` delegates a hover node and draws the `RowHighlight` colour at about 50 % while hovered, in draw only, only for mouse and stylus hover | WP03 |
| Pointer icons (`Custom_Cursors`) | `pointerHoverIcon(PointerIcon.Hand)` from `pressable` for links and buttons | WP03 |
| Focus (`Drawable_Focus`, `Keyboard_Navigation`) | Focus ring and Enter/Space/DPad already exist (`Pressable.kt:485-516`, APPLIED). Add `focusRestorer()` on lazy lists, bar, rail and sidebar; `focusGroup()` on the chrome; in two panes, focus order is list then detail | WP03, WP04 |
| Shortcuts (`Keyboard_Shortcuts`) | `AppShortcuts`, a per-window registry provided by `AppShell`. Defaults: Ctrl+1…9 select a tab, Ctrl+F search, Ctrl+N new (when a screen registers it), Ctrl+[ and Alt+← back, Ctrl+Shift+S sidebar, Ctrl+R refresh (when registered). `MainActivity.onProvideKeyboardShortcuts` lists them in the Shortcuts Helper (Meta+/) [V, keyboard-shortcuts-helper page]. A screen registers through `ShortcutEffect(Shortcut(Key.N, ctrl = true, label = R.string.x)) { … }` | WP04 (registry, shell defaults), WP02-M3 (screen shortcuts) |
| Esc (`Keyboard_Exit`) | An explicit `Key.Escape` handler on sheets, menus and the image viewer, besides the platform Back fallback [U: `Generic.kcm` fallback] | WP03 |
| Enter (`Keyboard_Send`) | `AppTextField(onSubmit)` (§6) | WP03; WP02-M3 wires sign-in and search |
| Scroll indicator (`Scrollbar_Display`) | `Modifier.appScrollIndicator(state)`: a thin capsule that fades in while scrolling, drawn by a `DrawModifierNode` reading `scrollIndicatorState` in draw only [V the API exists in foundation 1.12.1]. Wider and draggable with a fine pointer | WP03 |
| Zoom (`Content_Zoom`) | `AppImageViewer` Ctrl+wheel and pinch | WP03 |
| Multi-select (`Input_Combinations`) | Shift-range and Ctrl-toggle in `AppSelection` and `AppReorderableList` when `finePointer` | WP03 |
| Drag and drop (`Drag_Drop_Support`) | A recipe in `docs/ADAPTIVE.md` and a catalog demo: `dragAndDropSource`/`dragAndDropTarget`, `DRAG_FLAG_GLOBAL_SAME_APPLICATION` | WP03, WP12 |
| Targets | `minimumTouchTarget` stays at 48 dp (`Pressable.kt:510`). `InputTraits` (fine pointer, no touch) may tighten padding but never targets | WP03 |

---

## 10. Manifest and Activity (WP04 lines, WP02-M2 check; AD-15)

```xml
<application …>
    <property android:name="android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI" android:value="true" />
    <activity
        android:name=".MainActivity"
        android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboard|keyboardHidden|navigation"
        android:exported="true"
        android:launchMode="singleTop"
        android:theme="@style/Theme.BaseApp.Starting"
        android:windowSoftInputMode="adjustResize">
        <layout android:defaultWidth="1024dp" android:defaultHeight="720dp"
                android:minWidth="320dp" android:minHeight="400dp" />
        …
    </activity>
```

- The first four `configChanges` values follow Google's recommendation [V]. The keyboard and navigation values cover
  Tier 3 `Keyboard_Input` (attaching a keyboard must not relaunch) [U: confirm with `dumpsys activity` on the emulator].
- `density`, `uiMode`, `locale` and `fontScale` still recreate, because resources must reload. The process-death
  `restore` scenario stays mandatory.
- Never: `screenOrientation`, `resizeableActivity="false"`, `minAspectRatio`, `maxAspectRatio`,
  `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`.
  - Android 16 ignores the first four on sw600+ displays.
  - Android 17 (target 37) removes the opt-out [V, behavior-changes-16 and the Android 17 blog post].
- `MainActivity` (WP04):
  - passes `posture = rememberWindowPosture(this)` and `input = rememberInputTraits(this)` to `AppTheme`;
  - overrides `onProvideKeyboardShortcuts`;
  - keeps `enableEdgeToEdge()` with the in-app theme's bar style.

---

## 11. Enforcement: how the template forces adaptivity

### 11.1 `verifyCodeRules` ids (WP02-M2, in `CodeRules.ALL`, each with a fixture test and the allow marker)

| Id | Fails on (simplified regex over code lines, comments and strings stripped) | Scope | Fix it names |
|---|---|---|---|
| `adaptive-orientation` | `\bconfiguration\.orientation\b`, `ORIENTATION_(LANDSCAPE\|PORTRAIT)`, `\b(set\|get)RequestedOrientation\b`, `\brequestedOrientation\b` | all main sources | `AppTheme.windowClass` / `containerClass` |
| `adaptive-screen-metrics` | `screen(Width\|Height)Dp`, `smallestScreenWidthDp`, `displayMetrics\.(width\|height)Pixels`, `\bdefaultDisplay\b`, `getRealMetrics`, `(current\|maximum)WindowMetrics`, `\bLocalWindowInfo\b` | main sources except `core/designsystem/**/foundation/adaptive/**` | `containerClass`, constraints, `LocalWindowMetrics` inside a lambda |
| `adaptive-device-type` | `\bisTablet\b`, `\bisPhone\b`, `R\.bool\.is_tablet`, `Build\.(MODEL\|DEVICE\|PRODUCT)` used in UI code | `feature/**`, `core/ui`, `core/designsystem`, `core/navigation` | classes |
| `adaptive-fixed-size` | `\.(width\|requiredWidth\|size\|requiredSize)\(\s*([3-9]\d\d\|\d{4,})(\.\d+)?\.dp` and `\.(height\|requiredHeight)\(\s*(4[89]\d\|[5-9]\d\d\|\d{4,})(\.\d+)?\.dp` | `core/ui`, `core/designsystem/**/component/**`, `catalog` (features are already covered by `raw-dp`) | `widthIn(max = AppTheme.layout.*)`, weights, `fillMax*` |
| `adaptive-box-constraints` | `\bBoxWithConstraints\b` | `feature/**` | `containerClass` or a custom `Layout` (SubcomposeLayout costs a frame) |
| `adaptive-mediaquery` | `isMediaQueryIntegrationEnabled`, `\bmediaQuery\s*\{`, `LocalUiMediaScope` | everywhere | `AppTheme.input`, `AppTheme.posture` |
| `adaptive-material-scope` | an import of `androidx.compose.material3.adaptive.navigationsuite` or `NavigationSuiteScaffold` anywhere; an import of `androidx.compose.material3.adaptive.` outside `core/navigation/**/scene/**` and `core/designsystem/**/foundation/adaptive/**` | everywhere | `NavPane`, `AppShell` |
| `adaptive-preview` (module-level) | a top-level `fun <Name>Screen(` in `feature/**/src/main` with no function annotated `@AppScreenPreviews` in the same file | `feature/**` | add the preview (§11.4) |

Also change `material` (WP02 `CodeRules.kt`, the `material` rule) to `^\s*import\s+androidx\.compose\.material(?!\w*\.ripple\b)(?!3\.adaptive\.)`
so the allowed adaptive artifacts pass. When the optional `materialbridge` module arrives (requirement 31), it gets an
exemption by module path.

### 11.2 Lint (WP02-M2, `AndroidCommon.kt`)
- **APPLIED:** `LockedOrientationActivity`, `SourceLockedOrientationActivity` and `NonResizeableActivity` as errors
  (`AndroidCommon.kt:47-51`).
- **Add as errors:** `ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`, `LocalContextResourcesRead` and
  `UnusedBoxWithConstraintsScope` [V, ids in the ui 1.12.1 `lint.jar`].
- Confirm every id with `./gradlew :app:lint --list` (or a lint report), because an unknown id in `lint.error` is
  ignored silently [V, CODE-STANDARDS M-5].

### 11.3 `verifyManifestRules` (WP02-M2; `:app`, per variant, over `SingleArtifact.MERGED_MANIFEST`)
- **Fails when:**
  - `MainActivity` lacks any of the seven `configChanges` flags;
  - the `<application>` multi-instance property is missing;
  - any activity has `screenOrientation`, `resizeableActivity="false"`, `minAspectRatio` or `maxAspectRatio`;
  - `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` appears;
  - `enableOnBackInvokedCallback` is not `true`;
  - any component lacks `android:exported` (B-4).
- A library activity that breaks a rule is allowed by name in `config/manifest-allow.txt`, one line with a reason.
- The task is cacheable with a marker output, wired into `check` like `ModuleGraph`.

### 11.4 Multi-size previews (WP03, `core/designsystem/.../preview/`)

```kotlin
@Preview(name = "Phone", device = Devices.PHONE)
@Preview(name = "Phone landscape", device = "spec:width=891dp,height=411dp")
@Preview(name = "Foldable", device = Devices.FOLDABLE)
@Preview(name = "Tablet", device = Devices.TABLET)
@Preview(name = "Desktop", device = Devices.DESKTOP)
@Preview(name = "Phone, font 2x", device = Devices.PHONE, fontScale = 2f)
@Preview(name = "Phone, dark", device = Devices.PHONE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@PreviewWrapper(AppPreviewTheme::class)        // stable in ui-tooling-preview 1.12.1 [V adaptive.md §1.11]
annotation class AppScreenPreviews
```

- `AppPreviewTheme` is a `PreviewWrapperProvider` that wraps the preview in `AppTheme(fonts = PlatformFonts)`.
  ComposablePreviewScanner 0.9.0+ applies `@PreviewWrapper` itself [V, Roborazzi release notes].
- `ui-tooling-preview` is an `implementation` dependency of `:core:designsystem`. Previews stay in `src/main` next to
  their screen, and R8 removes them from release.

### 11.5 Screenshot matrix (WP02-M2 plugin; WP03 and WP02-M3 content)
- Convention plugin `<pkg>.android.screenshots` (Roborazzi 1.76.0), applied to `:core:designsystem`, `:catalog` and
  `:feature:*`:
  - `generateComposePreviewRobolectricTests { enable = true; packages = listOf("<pkg>") }`;
  - `robolectricConfig` sdk 35 (`TEST_SDK`);
  - `systemProperty("robolectric.graphicsMode", "NATIVE")`;
  - the test dependencies (`roborazzi`, `roborazzi-compose`, `roborazzi-compose-preview-scanner-support`,
    ComposablePreviewScanner) at the versions the 1.76.0 release pins [U: exact coordinates].
- **PR sizes** (each width class and each height class at least once):

  | Id | dp | Classes |
  |---|---|---|
  | phone | 411×891 | C×M |
  | phone-land | 891×411 | E×C |
  | fold | 673×841 | M×M |
  | tablet-8 | 1024×640 | E×M |
  | tablet-10 | 1280×800 | L×M |
  | desktop | 1920×1080 | XL×E |

- **Nightly:** 320×568, 800×1280, font 2 at phone and tablet-10, dark at phone, RTL at tablet-10.
- **Goldens.**
  - The template ships no goldens for generated apps. The developer records once with `recordRoborazziDevDebug`.
  - CI runs `verifyRoborazzi…` only when goldens exist, and otherwise `compareRoborazzi…` with a warning.
  - An agent never re-records goldens to make a test pass.
- This supersedes CODE-STANDARDS T-5's tool and sizes.

### 11.6 Golden-free tests that always run
- **`AdaptiveAuditTest`** (`:app` `src/test`, which sees every feature):
  - It finds every `@AppScreenPreviews` function with ComposablePreviewScanner [U: `includePrivatePreviews()`] and
    renders it at the six PR sizes and at font scales 1 and 2.
  - It asserts:
    - no visual overflow on text nodes without `maxLines`;
    - every clickable inside the window and not under `LocalBottomChrome`;
    - targets of at least 48 dp;
    - no overlapping clickables;
    - no text node wider than `readableMaxWidth + gutter` at Large and wider.
  - It also runs `enableAccessibilityChecks()` [U under Robolectric].
- **`ResizeContinuityTest`** (`:core:navigation`, Robolectric, `DeviceConfigurationOverride.WindowSize(sizeState)`,
  which re-reads its size live [V adaptive.md §1.11]):
  - **A. Tabs survive class flips.**
    - Three fake tabs, each holding a `remember { Any() }` identity, a saveable counter and a ViewModel.
    - Push on tab 2, then walk 411 → 891 → 1280 → 1920 → 673 → 411.
    - Assert: no identity changed, stacks and tab unchanged, the chrome tag is `tab_bar`/`rail`/`sidebar` as expected,
      `BackGate.moving` never true.
  - **B. List-detail flip.**
    - At 1280, scroll the list to item 30 and open a detail; resize to 411.
    - The detail is on top. Back shows the list at item 30.
    - Resize back: two panes, and the row is marked selected.
  - **C.** A resize from 900 to 1000 to 1100 dp causes 0 screen-root recompositions and at most 1 chrome
    recomposition.
  - **D. Posture.** Injected `Tabletop` and `Book` put the viewer's split, the sheet's detent and the list-detail
    split on the hinge.
  - **E. Input.**
    - Tab order at each size; Esc closes the top sheet; Ctrl+F focuses search; Ctrl+2 selects tab 2.
    - `performMouseInput { rightClick() }` on a row opens its menu and does not call `onClick`.
    - Hover shows the highlight.
  - **F. Multi-instance.** Two `ActivityScenario`s in one process: a command from A's ViewModel changes only A.
  - **G. No recreation.** `RuntimeEnvironment.setQualifiers("+land")`, then `w1280dp-h800dp`, then `+keyshidden`:
    `onCreate` count == 1.
- **`PaneContextTest`, `TwoPaneBackTest`, `ShellChromePolicyTest`** (pure function), and **`SizeClassTest`**
  (599/600, 839/840, 1199/1200, 1599/1600, 479/480, 899/900).

### 11.7 Docs that require it (WP12): §15.

### 11.8 Generator assertions (WP02-M2, `generator/tests`)
- For lean, standard and everything: the generated project has the seven `configChanges` flags and the property; the
  `adaptive-*` ids and the material exemption are present in `CodeRules.kt`; the lint ids are present; the
  screenshot plugin is applied to `:core:designsystem` and every `:feature:*`; `AdaptiveAuditTest` exists.
- `scaffold.py` emits a list/detail pair with `pane = NavPane.List/Detail`, an `@AppScreenPreviews` per screen and
  saveable list state, so a scaffolded module passes `adaptive-preview` on day one.

---

## 12. Verification on this machine (no Mac, 15 GB RAM, locks)

`L=$OV/bin/locked`. Gradle through `$L gradle`, adb through `$L adb`, emulator boots through `$L emu`.

### 12.1 What runs without a device
All of §11.1–11.6 runs under Robolectric on the JVM: `./gradlew check` plus `testDebugUnitTest` in
`:core:designsystem`, `:core:navigation` and `:app` (`testDevDebugUnitTest`). This is where the adaptive correctness
gates live.

### 12.2 AVDs (system image already installed: `system-images;android-35;google_apis;x86_64`; no download)

```bash
A=~/Android/Sdk/cmdline-tools/latest/bin/avdmanager
IMG="system-images;android-35;google_apis;x86_64"
$A create avd -n adaptive_fold_api35      -k "$IMG" -d pixel_fold      # fold/unfold/posture
$A create avd -n adaptive_resizable_api35 -k "$IMG" -d resizable       # resize-display 0..3
$A create avd -n adaptive_tablet_api35    -k "$IMG" -d pixel_tablet    # freeform, multi-instance
# then in each ~/.android/avd/<name>.avd/config.ini: hw.ramSize=2048, hw.keyboard=yes
$L emu ~/Android/Sdk/emulator/emulator -avd adaptive_fold_api35 -no-snapshot -no-boot-anim -gpu host -memory 2048 &
```

- Device ids `pixel_fold`, `resizable` and `pixel_tablet` are in `avdmanager list device` on this machine [V].
- One emulator at a time: about 2–3 GB with the host process; Gradle takes 4 + 2 GB [U: measure].
- **Desktop AVDs** (`desktop_medium`, `desktop_large`) need an `android-desktop` system image that is not installed.
  Downloading it is not granted (ORCH), so the tablet AVD's freeform mode stands in.
- The existing `periflow_api35` (Pixel 8, 1080×2400 at 420 dpi, so 411×914 dp) is shared with other packages. Use it
  only with the overrides in §12.4, and reset them.

### 12.3 Emulator controls [V adaptive.md §1.11 unless marked]

| Action | Command |
|---|---|
| fold / unfold | `adb emu fold`, `adb emu unfold` |
| posture | `adb emu posture 1` closed, `2` half-open (tabletop/book), `3` open |
| resizable display | `adb emu resize-display 0` phone, `1` unfolded, `2` tablet [U: `3` desktop] |
| rotate | `adb emu rotate`, or `adb shell settings put system accelerometer_rotation 0; adb shell settings put system user_rotation 1` |
| device state (alternative) | `adb shell cmd device_state print-states`, `adb shell cmd device_state state <id>` [U] |
| freeform window (tablet AVD) | `adb shell settings put global enable_freeform_support 1; adb shell settings put global force_resizable_activities 1; adb reboot`, then `adb shell am start --windowingMode 5 -n <appId>/<pkg>.MainActivity` and `adb shell am task resize <taskId> 0 0 1200 800` [U] |
| second instance | `adb shell am start -n <appId>/<pkg>.MainActivity -f 0x18001000` (NEW_TASK, MULTIPLE_TASK, LAUNCH_ADJACENT); two tasks in `dumpsys activity activities` |
| keys | `adb shell input keyevent KEYCODE_TAB`, `adb shell input keycombination KEYCODE_CTRL_LEFT KEYCODE_F` [U: Android 13+] |
| mouse | `adb shell input mouse tap X Y` (primary only; right-click and hover are covered by Robolectric E) |
| font scale | `adb shell settings put system font_scale 2.0` (reset `1.0`) |
| RTL | `adb shell cmd locale set-app-locales <appId> --locales ar` [U] |
| recreation count | `adb logcat -s perf.shell` (one `generation=` line per `onCreate`), or `dumpsys activity <appId>` |

### 12.4 Window sizes by `wm` overrides (any AVD; same density = a size-only change, which must not recreate)

| Target | Commands | dp | Classes |
|---|---|---|---|
| phone-land | rotate | 914×411 | E×C |
| fold | `wm size 1346x1682; wm density 320` | 673×841 | M×M |
| tablet portrait | `wm size 1600x2560; wm density 320` | 800×1280 | M×E |
| tablet-8 | `wm size 2048x1280; wm density 320` | 1024×640 | E×M |
| tablet-10 | `wm size 2560x1600; wm density 320` | 1280×800 | L×M |
| desktop | `wm size 1920x1080; wm density 160` | 1920×1080 | XL×E |
| smallest | `wm size 640x1136; wm density 320` | 320×568 | C×M |
| reset | `wm size reset; wm density reset` | | |

With density 320 fixed, `1346x1682 → 2048x1280 → 2560x1600 → 1346x1682` is a live resize series. Expect no
`onCreate`, the stacks unchanged and the chrome following the class [U: whether `wm size` with width > height on a
portrait-natural display reports the window as given; check the `perf.shell` `window=` field].

### 12.5 `tools/adaptive/adaptive.sh` (WP04; run with `$L adb`)
- Arguments: `--serial`, `--package`, `--out DIR`, scenarios `sizes|fold|posture|multiinstance|freeform|keyboard`.
- Each step:
  - applies one row of §12.3 or §12.4;
  - waits for idle;
  - dumps the UI (`uiautomator dump`);
  - reads the last `perf.shell` line;
  - asserts the chrome type, the selected tab and stacks, the recreation count and that nothing crashed;
  - saves a screenshot.
- A `trap` restores `wm size/density`, rotation and font scale on every exit.
- Output: `$OV/perf/WP04/adaptive/<date>/summary.md`.

### 12.6 Perf scenarios (WP04 `perf.sh` and `gates.toml`)

| Scenario | Gate (solid) |
|---|---|
| `tabs_large` (rail or sidebar at L×M) | janky ≤ 1.0 %; first frame of a visited tab ≤ 8 / ≤ 16 ms (the phone row) |
| `listdetail_select` (20 rows, the detail swaps in place) | janky ≤ 1.0 %; detail-swap first frame ≤ 12 / ≤ 20 ms |
| `pushpop_large`, `scroll_large` | the phone rows |
| `sidebar_toggle` (10 toggles) | janky ≤ 0.5 %, 0 recompositions per frame |
| `resize` (emulator, functional) | no crash, stacks and tab unchanged, no recreation |
| PSS after the large journey | ≤ phone PSS + 15 MB [D] |

- **On the Redmi** (reference GPU, 120 Hz), only with the user's explicit OK (§0.7).
  - `adb shell wm density 260` with the phone held sideways (`user_rotation 1`) gives about 1477×664 dp (L×M);
    `wm density 240` held sideways gives 1600×720 (XL×M). Computed for 1080×2400 px [U until run].
  - Restore by trap with `wm density reset` and `user_rotation 0`.
  - Record the phone's default density first (`adb shell wm density`).
- **Without the OK:** the same scenarios on `adaptive_tablet_api35`, as relative evidence only, never gated.
- The final report (requirement 20) adds large-layout rows for old and new once the phone runs exist.

---

## 13. Compose Multiplatform parity (WP07; AD-50)

1. `foundation/adaptive/` (`SizeClass`, `AppLayout`, gating, `ProvideContainerWidth`) goes to `commonMain` unchanged.
   `window-core` 1.5.1 has iOS artifacts [V]; add the pair to `sync.toml` as identical.
   - The gating of §3.3 is **mandatory** on iOS, because `containerSize` is lerped every frame of a rotation or Stage
     Manager resize [V, JetBrains `WindowContext.ios.kt`].
2. **Traits as `expect`/`actual`:**
   - `WindowPosture`: Android uses `WindowInfoTracker`; iOS is always `Flat`.
   - `InputTraits`: Android uses `InputManager`; iOS uses `GCKeyboard.coalesced != nil` for a hardware keyboard, and
     `finePointer` is true on iPad [U].
3. **Panes.** Check whether JetBrains publishes `org.jetbrains.compose.material3.adaptive:adaptive-navigation3` for
   iOS at a version built against JetBrains `navigation3-ui` 1.1.2 [S: the artifact family exists; U: version
   pairing].
   - If it exists and the §5.2 spike passes on `androidApp`, use it on both platforms.
   - Otherwise port `PaneSceneStrategy` to `commonMain`, using only `Scene`/`SceneStrategy` (1.1.2 has no
     `SceneDecoratorStrategy` [U]). `sync.toml` lists `scene/PaneStrategies.kt` as an allowed difference.
4. **Chrome policy per platform** (`ShellChromePolicy` takes a `platform`):
   - Android: as §4.1.
   - iPhone: the bottom bar stays held sideways (iOS convention).
   - iPad: Medium → rail, Expanded → rail with sidebar toggle, Large and wider → sidebar.
5. **iPad `Info.plist`** (XcodeGen `info.properties`), asserted by `test_target_multiplatform.py`:
   - all four `UISupportedInterfaceOrientations~ipad`;
   - no `UIRequiresFullScreen` (deprecated in iPadOS 26, ignored in 27 [V TN3192]);
   - `UILaunchScreen` (required from iOS 27);
   - `UIApplicationSupportsIndirectInputEvents = YES` (trackpad and mouse as pointer events);
   - `UIApplicationSupportsMultipleScenes = false` for v1 [D].
6. **Input:** the §9.2 right-click fix lives in shared code. Check `firstDownRefersToPrimaryMouseButtonOnly` on iOS
   [U].
7. **Tests:** Roborazzi `androidHostTest` renders the `commonMain` previews under
   `com.android.kotlin.multiplatform.library` [V, Roborazzi #781]. `ResizeContinuityTest` A/B/C run in
   `androidHostTest`. iOS gets the static checks and the macOS CI build.

---

## 14. SwiftUI parity (WP08; AD-60)

1. **Shell:**
   - `TabView` with `.tabViewStyle(.sidebarAdaptable)` and `TabSection`/`.tabViewCustomization` on iOS 18+;
   - `TabView` with `.tabItem` on iOS 17 (code sketch: adaptive.md §10);
   - more than 5 tabs on iPhone: the system "More" (iOS's own rule).
2. **List-detail:** `NavigationSplitView(columnVisibility:preferredCompactColumn:)` (iOS 17) with
   `ContentUnavailableView` as the placeholder, and a `NavigationStack(path:)` in the detail column.
3. **Supporting pane:** `.inspector(isPresented:)` with `inspectorColumnWidth(min: 280, ideal: 320, max: 412)`. It is a
   sheet in compact width (iOS 17).
4. **Width classes from geometry**, not `horizontalSizeClass` alone (iPhone apps stay `.compact` in a resizable window
   [S, fatbobman]):
   - `.onGeometryChange(for: WindowWidth.self) { WindowWidth(points: $0.size.width) } action: { width = $0 }` at the
     root;
   - `@Environment(\.appWindowWidth)`;
   - the same 600/840/1200/1600 thresholds, in points.
5. **Input:**
   - `.keyboardShortcut` on toolbar buttons and `.commands { CommandMenu(...) }`;
   - `.onKeyPress` (17);
   - `.hoverEffect(.highlight)` (17) on custom rows;
   - `.contextMenu(menuItems:preview:)`;
   - `.draggable` and `.dropDestination(for:)`.
6. **Resizing:**
   - all iPad orientations, no `UIRequiresFullScreen`, `UILaunchScreen`;
   - `.windowResizability(.contentMinSize)` with a 320×400 minimum;
   - `onInteractiveResizeChange` (iOS 26, behind `#available`) defers expensive relayout until the drag ends;
   - never `UIScreen.main`, never `userInterfaceIdiom` in `Features/`.
7. **Static checks** (`test_target_ios.py`, A12.4):
   - the plist keys;
   - bans on `UIScreen.main`, on `userInterfaceIdiom` in `Features/`, on `.frame(width: N)` with N ≥ 320 in
     `Features/`, and on `UIRequiresFullScreen`;
   - each feature view has a `#Preview` with an iPad layout trait [U: the exact trait spelling].
8. **Snapshot tests** on macOS CI: `swift-snapshot-testing` at iPhone and iPad layouts [U: version and configuration
   names].

---

## 15. Docs (WP12; AD-70)

1. **`AGENTS.md` block** (Android and CMP; it replaces "Every screen works on every window", int `AGENTS.md:169-184`,
   which still names `AppTheme.windowSize`, `AppResponsiveGrid` and `wm size 1600x2560` with no density reset):

   ```markdown
   ### Adaptive UI (enforced by verifyCodeRules, lint, verifyManifestRules, AdaptiveAuditTest)
   - Decide layout from `AppTheme.containerClass` (screens, components) or `AppTheme.windowClass` (shell only). Never
     from orientation, device type, `Configuration.screenWidthDp` or `LocalWindowInfo`.
   - Lists with a detail: `entry<ListKey>(pane = NavPane.List)` and `entry<DetailKey>(pane = NavPane.Detail)`; a side
     panel: `NavPane.Main` + `NavPane.Supporting`. The shell decides when panes sit side by side. Never build a two-pane
     `Row` for navigation and never call Material's scene strategies from a feature.
   - No screen-sized fixed dimensions. Use `widthIn(max = AppTheme.layout.*)`, weights, and
     `LazyVerticalGrid(GridCells.Adaptive(AppTheme.layout.gridMinCardWidth))` for cards. `Grid`/`FlexBox` are
     experimental in Compose 1.12: not without an explicit decision.
   - Every `XScreen` has an `@AppScreenPreviews` preview of its stateless UI.
   - State that must survive fold, resize or a pane move: `rememberSaveable` or the ViewModel.
   - Pointer and keyboard: interactive things use `Modifier.pressable` (hover, focus, right-click menu included); screen
     shortcuts go through `ShortcutEffect`; Esc closes, Enter submits.
   - Never re-record screenshot goldens to make a test pass; show the diff.
   ```

2. **`docs/ADAPTIVE.md`:**
   - the class, token, chrome and pane tables (§3–5), the component table (§6), the input checklist (§9) and the
     tier mapping;
   - the test commands (§11.5–11.6) and the device recipes (§12.3–12.5, including the AVD creation commands);
   - the root causes found: the static exact-size local, the singleton navigator, rail overflow, the right-click bug;
   - the measured numbers from §12.6 once they exist.
3. **Pre-merge checklist** lines:
   - `./gradlew check` green (rules, lint, manifest);
   - `AdaptiveAuditTest` and `ResizeContinuityTest` green;
   - screens checked at phone, phone-land, fold, tablet-10 and desktop (the Roborazzi compare report);
   - Tab order, Esc and shortcuts; hover and right-click.
4. **Skill differences** for the vendored `adaptive` skill (the AGENTS.md skills table, see STANDARDS-PLAN §4):
   - no `NavigationSuiteScaffold` (it needs material3);
   - panes through `NavPane` (the shell uses Material's scene strategies);
   - `LazyVerticalGrid` rather than `Grid` on Compose 1.12;
   - `AppTheme` traits rather than `MediaQuery`;
   - Roborazzi rather than `com.android.compose.screenshot`;
   - `AppLargeTitleTopBar` rather than M3 scroll behaviours.
5. **SwiftUI `AGENTS.md`:** §14 rules 4–7 as do/don't.

---

## 16. Owners (what each handoff note lists)

| Package | Items |
|---|---|
| WP03 | AD-01 adaptive layer v2 (§3); AD-02 posture and input types plus `AppTheme` parameters; AD-03 tokens (§3.4); AD-04 components (§6); AD-05 input in `pressable` and components (§9.2); AD-06 `@AppScreenPreviews` + `AppPreviewTheme` (§11.4); AD-07 component screenshot and audit content for the catalog; AD-08 `AppTwoPane` hinge and tabletop; AD-09 catalog Adaptive section; `AppTopBar` reads `LocalPaneContext` (§5.3) |
| WP04 | AD-10 chrome policy, sidebar, More, rail fixes, RTL (§4); AD-11 `NavPane` kinds, Material strategies spike and fallback, pane context (§5); AD-12 two-pane back; AD-13 the `LocalWindowInfo` reads in `StackHost`/`PresentationHost`; AD-14 activity-retained navigator and deep links; AD-15 manifest lines, posture and input sources, `AppShortcuts`, `onProvideKeyboardShortcuts` (§8–10); `ResizeContinuityTest` A/B/D/E(shell)/F/G, `PaneContextTest`, `TwoPaneBackTest`, `ShellChromePolicyTest`; `adaptive.sh`; §12.6 scenarios and gates; `ShellLog` fields; the `material3-adaptive*` 1.3.0 catalogue lines (navigation family, B.3) |
| WP02-M2 | AD-20 `material` rule exemption + `adaptive-*` ids (§11.1); AD-21 lint ids (§11.2); AD-22 `verifyManifestRules` (§11.3); AD-23 `<pkg>.android.screenshots` plugin and CI wiring (generated `.github/**`) (§11.5); AD-24 `tabs.py` cap, More and distinct icons (QA-09, QA-10); AD-25 `scaffold.py` emits adaptive-ready modules; AD-26 generator assertions (§11.8) |
| WP02-M3 | AD-30 screens: stateless `XScreen` + `@AppScreenPreviews`; panes (settings → list-detail, licenses detail); feed and search results as adaptive grids; saveable state; `onSubmit` on auth and search; screen shortcuts (Ctrl+F search, Ctrl+N where a screen creates); right-click menus where long-press menus exist |
| WP05 | AD-40 glass on rail and sidebar: the stage records only the content area. During a live resize (size changing within 150 ms) glass shows its frosted tier, so the backdrop layer is not reallocated every frame [D; U cost]. No singleton stage state (multi-instance). `tabs_large` with glass on |
| WP06 | AD-45 `DevBadge`: position from the safe-drawing and caption insets, clamped to the window on resize, draggable with a mouse (QA-44 is related). `DevToolsPanel`: a side panel from Expanded instead of a full-width sheet; per-window UI state. The app-lock overlay (`:core:applock`, A2.2) covers each window. Push and notification `PendingIntent`s keep opening the existing task |
| WP07 | AD-50 (§13) |
| WP08 | AD-60 (§14) |
| WP12 | AD-70 (§15) |

---

## 17. Sources

Read for this plan (in addition to those listed in `adaptive.md` §14):
- Compose foundation-layout bytecode, `~/.gradle/caches/modules-2/files-2.1/androidx.compose.foundation/foundation-layout-android/{1.12.0,1.12.1,1.13.0-alpha03}`:
  `ExperimentalGridApi`, `ExperimentalFlexBoxApi` (`RequiresOptIn`), `GridKt`, `FlexBoxKt`.
- Material 3 adaptive releases: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
  (1.3.0 on 2026-08-12, 1.4.0-alpha03 on 2026-10-07).
- androidx-main, `compose/material3/adaptive/adaptive-navigation3`: `ListDetailSceneStrategy.kt`,
  `ThreePaneScaffoldScene.kt`, `api/current.txt`
  (https://github.com/androidx/androidx/tree/androidx-main/compose/material3/adaptive/adaptive-navigation3).
- androidx-main `compose/material3/adaptive/adaptive/.../WindowAdaptiveInfo.kt` (`currentWindowAdaptiveInfoV2`).
- Desktop windowing: https://developer.android.com/develop/ui/compose/layouts/adaptive/support-desktop-windowing
  (multi-instance property on `<application>`, Android 15).
- Roborazzi releases: https://github.com/takahirom/roborazzi/releases (1.72.0–1.76.0; ComposablePreviewScanner 0.9.0+
  applies `@PreviewWrapper`).
- Google `adaptive` skill v1.0.13 (`~/.claude/plugins/marketplaces/android-skills/jetpack-compose/adaptive/SKILL.md`)
  and its Material list-detail recipe reference.
- JetBrains material3 adaptive in CMP [S]: https://proandroiddev.com/building-a-fully-adaptive-navigation-system-in-compose-multiplatform-kmp-e1a8921ca792 ,
  https://kotlinlang.org/docs/multiplatform/whats-new-compose-170.html
- Local: `avdmanager list device` (device ids), `~/Android/Sdk/system-images/android-35/google_apis/x86_64`,
  `~/.android/avd/periflow_api35.avd/config.ini`.
- Code: WP02 `CodeRules.kt` (`material` rule regex), WP03/WP04 `AppWindowSize.kt`, `AppTheme.kt`, WP04 `AppShell.kt`,
  `ListDetailScene.kt`, `StackHost.kt:101`, `PresentationHost.kt:86`, `AppNavigator.kt:40`, `DeepLinks.kt:61`,
  `AppTabBar.kt`, `AndroidManifest.xml`; QA `qa/r1/DEFECTS.md` (QA-07, QA-08, QA-09, QA-10, QA-44).
