# Preview tree: merge notes

Branch `preview/catalog-2026-10-19` in the worktree `D:/projects/tools/.overhaul/wt/_preview`. Local only, never
pushed. It combines the newest code so the user's see-through back card (`bugs/BUG-2026-10-19-back-see-through.md`)
can be reproduced on it.

| Commit | What |
|---|---|
| `d9ef1e27` | merge `wp/WP04` at `4a15dda` (catalog-preview workflow, Oct 19 15:38) |
| `eb4f2632` | `out/WP04-perf/backcard-texture.patch` (T) and `out/WP04-perf/back-google/c1.patch` (C1) applied |
| `ebb1ca15` | merge `wp/WP06` at `08c0d0a` (catalog-preview workflow, 15:44) |
| `e0f443b8` | merge `wp/WP03` at `1dc5714` (this run) |
| `fe33d475` | the compile sweep after the WP03 merge |
| `aaab6fe4` | merge `wp/HUD` at `936fa2e` |
| `22992d3c` | the overlay module on WP03's API and WP04's profile; the preview's head for the capture's build (a) |

## Earlier merges (recorded from their commit messages)

This file did not exist when the catalog-preview workflow stopped at 15:44; its two merges say what they resolved:

- `wp/WP04`: only `web/frontend/lib/catalogue.json` conflicted (its `generator_commit` stamp); rewritten with
  `python -m genkit.core.catalogue --write`.
- `wp/WP06` (its handoff's plan, run 17: WP04 wins outside WP06's wires): `MainActivity` keeps WP04's
  `StartupViewModel`, shell config and deep-link holding, with WP06's `FragmentActivity`, jank recorder, DevTools,
  AppLock, ApiSignals and Play injections; `AppRoot` keeps WP04's entry hosts and system-bar icons inside WP06's
  `AppLockGate`, with the API status gate, Play prompts and `DevToolsOverlay`; the manifest, onboarding and root build
  as its message lists; datastore imports moved to `settings/session`; catalogue rewritten.

## wp/WP03 at 1dc5714 (merge `e0f443b8`)

Followed WP03's handoff ("When WP04 is merged", items 0-1) and WP06's merge notes (run 11). 26 files conflicted.

| File | HEAD (WP04 + T/C1 + WP06) | WP03 | Kept, and why |
|---|---|---|---|
| `generator/genkit/targets/android/variants.py` | removed the `firebase-off` variant (WP06 moved it into `opt-else` blocks) | removed `googlefonts-off` (Phase B: an `opt-else` block in `AppFonts.kt`) | both removed: `VARIANTS = {}`. Neither variant directory exists in the merged tree. |
| `generator/tests/test_android_target.py` | `database` import; the test renamed to `test_a_seam_without_its_vendor_binds_the_no_op`, asserting `{"googlefonts"}` | `kotlin` token import; asserts `{"firebase"}` | both imports; HEAD's test with `assertEqual({}, variants.VARIANTS)`, the truth after both removals. No assertion dropped. |
| `gradle/libs.versions.toml` | Navigation 3 `1.2.0` (WP04) | `1.1.7` | `1.2.0`: WP04's predictive-back and scene code is built against it, as the WP06 merge also kept. |
| `web/frontend/lib/catalogue.json` | stamp and rows | stamp and rows | regenerated with `--write` from the merged generator; `--check` says current. |
| `app/.../lock/LockActivity.kt` | deleted (WP06: `AppLockGate` replaced it) | typography rename only | deleted, per WP06's notes. |
| `catalog/.../CatalogComponents.kt` | WP04 swapped its bottom-bar demo to `AppTabBar` | deleted (split into `sections/`) | deleted, per WP03 item 1; WP04's `sections/CatalogNavigation.kt` shows every bar. |
| `designsystem/.../navigation/AppBottomBar.kt`, `AppNavigationRail.kt` | deleted (WP04's `AppTabBar`, `AppSidebar`, `TabRail`) | token renames | deleted, per WP03 item 1. The "Tab bar and rail" group of `catalog/sections/CatalogBars.kt` used both, so it is dropped (WP03 item 1). |
| `core/devtools/.../DevBadge.kt` | deleted (WP06: `badge/DevToolsOverlay.kt`) | `appClickable` and type renames | deleted; the renames are applied to `DevToolsOverlay.kt` in the sweep. |
| `core/navigation/.../AppNavHost.kt`, `ListDetailScene.kt`, `NavTransitions.kt` | deleted (WP04's shell, stack host and scene) | `AppMotion` -> `AppSprings`, `AppEmptyState` -> `AppStateView`, `windowSize` -> `windowClass` | deleted; their replacements get the same renames in the sweep. |
| `catalog/.../CatalogSection.kt` | added `Navigation` to the old enum | rewrote the enum (opt groups, `Loading`, `Sheets`, …) | WP03's enum plus `Navigation("Navigation", …) -> NavigationSection()` after `Bars` (WP03 item 1). |
| `catalog/src/main/res/values/strings.xml` | added `catalog_demo_list`, `catalog_demo_settings` | wrapped `app_name` in the generated `catalog-name` region | both: the region around `app_name`, WP04's two strings after it. |
| `designsystem/.../container/AppScaffold.kt` | `AppScaffoldDefaults.background()`: no fill where `LocalPaintedBackground` already painted the theme's background (WP04's one fill per screen, from `wp/WP04-perf` `ac56e69`); `imePadding`, a `floatingAction` slot | the rewrite: content under the bars (`SubcomposeLayout`), per-scaffold `LocalTopChrome`, sheet-aware fill, no `imePadding` (K14) | WP03's scaffold with WP04's rule built in: the fill it would paint is dropped when it equals `LocalPaintedBackground.current`, so `EntryFrame`/`PresentedFrame` paint each screen once (WP04's ask, WP03 item 10). No caller used `floatingAction` or `AppScaffoldDefaults`. Not done here: writing `ScreenChrome.topBar`/`.background` (rest of WP03 item 10). |
| `core/navigation/.../AppShell.kt` | WP04's rewrite | renames on the old shell | WP04's file; renames applied in the sweep. |
| `app/.../ui/AppRoot.kt` | WP04's entry hosts in WP06's gates | `hapticsEnabled = AppTheme.defaults.hapticsEnabled && settings.hapticsEnabled` (the spec's haptics live in `DesignTokens` now) | HEAD's structure with WP03's `AppTheme` call. |
| `core/devtools/.../panel/DevToolsPanel.kt` | WP06's moved and rewritten panel | WP03's sweep of the old file | WP06's file with WP06's rename list applied by hand: `AppListItem(supporting)` -> `AppRow(subtitle)`, `AppEmptyState` -> `AppStateView`, `selectedIndex` -> `selected`, `Ghost` -> `Plain`, `Secondary` -> `Tinted`, `fillWidth` -> `fillMaxWidth()`, `headingSmall`/`bodyMedium`/`labelSmall` -> `headline`/`callout`/`caption2`, `windowSize` -> `windowClass`, `WindowWidth` from `foundation.adaptive`, and the named fling on its three scrollers and its list (REQUIREMENTS 41). |
| `feature/settings/.../Licenses.kt` | `ReportScrollEdge`, `chromeContentPadding` on the list | `AppTopBar`, `AppStateView`, `AppRow`, the named fling | both; imports from `foundation.layout`. |
| `feature/settings/.../Settings.kt` | an inner `rememberScrollState`, `ReportScrollEdge`, `chromeContentPadding` | the scroll state hoisted for `AppLargeTitleTopBar(scrolled)`, the named fling, horizontal padding only | WP03's hoisted state (the inner one would have shadowed it and frozen the large title), WP04's `ReportScrollEdge` on it, and `chromeContentPadding(horizontal gutter)`: with WP03's scaffold the content runs under the bar, so it must pad itself (WP03 item 2, K4). |
| `feature/profile/.../ProfileScreen.kt`, `feature/sample/.../SampleDetailScreen.kt`, `feature/search/.../SearchResult.kt` | the same WP04 additions | the same WP03 changes | the same resolution as Settings. |
| `feature/sample/.../SampleListScreen.kt` | an inner `rememberLazyListState` with `ScrollToTopOnReselect` and `ReportScrollEdge` | the list state hoisted for the large title | WP03's hoisted state with both of WP04's calls on it (the inner state, auto-merged, was removed: it shadowed the hoisted one); WP03's redacted loading list. |
| `feature/search/.../SearchScreen.kt` | `Results` made its own list state for `ReportScrollEdge` | `Results(results, listState, …)` from a state keyed on the query, for the large title | WP03's signature with `ReportScrollEdge(listState)` inside; WP03's `ResultCard` and redacted `LoadingResults`; the unused `rememberLazyListState` import removed. |
| `feature/feed/.../FeedScreen.kt` | `chromeContentPadding` from `foundation.layout` | from `foundation` | `foundation.layout` (WP04 moved `Chrome.kt` there); WP03's redacted loading feed with its loading description. |

## The compile sweep after the WP03 merge

WP03 renamed or removed design-system API that WP04's and WP06's new files still used, and WP04 moved `Chrome.kt`
to `foundation/layout/` with a different `LocalTabVisible`. Nothing here changes a behaviour either branch tested;
every test file touched is a rename or an import, except the two `LocalTabVisible` providers noted.

- **`Chrome.kt` moved:** every import of `ChromeInset`, `LocalBottomChrome`, `LocalTopChrome`, `LocalTabVisible`,
  `chromeContentPadding` from `designsystem.foundation` now names `designsystem.foundation.layout` (22 files, WP03's
  design system, catalog, `core/ui` and tests).
- **`LocalTabVisible` is WP04's `TabVisibility` holder, not a Boolean** (WP03-skel M-1): `SkeletonNode.follow()`
  reads `.isVisible` inside `observeReads`; `AppActivityIndicator` steps inside `snapshotFlow { isVisible }` (no
  recomposition when a tab hides); the OTP caret reads `.isVisible` in composition (one small node, focused only);
  a sheet provides `TabVisibility { tab.isVisible && !controller.isCovered }`. Tests: `RecompositionJourneysTest`
  provides `remember { TabVisibility { visible } }` over its `visible` state and `SkeletonStillnessTest`
  `TabVisibility { false }`; their assertions are unchanged.
- **WP04's bar files on WP03's API** (`AppTabBar`, `AppSidebar`, `TabCells`): `rememberAppHaptics` /
  `AppHaptic.Selection` from `foundation.interaction`; `AppTheme.reduceMotion`; `caption2`/`body` type; the trailing
  action's `pressScale + clickable` -> `pressable(feedback = Scale)`; borders from `AppTheme.style.borderWidth`.
  WP03's styles have no `Chunky` or `Minimal` bar any more (`BarTreatment` is `Floating, Translucent, Docked` and
  the pill is `BarIndicator`), so `barLookFor` maps them: Floating and Translucent are the capsule (Translucent at
  88 % surface alpha, as WP03 documents it), a `Pill` indicator is the solid pill in `barIndicator` with `onAccent`
  content, `Subtle` the tint in `accentSubtle` with `accentContent`; dark capsules sit on `surfaceElevated`,
  hairlines are `separator`. `BarLook.rule` is now always false: WP04 can delete it and its drawing branch.
- **Navigation:** `rememberReduceMotion()` -> `AppTheme.reduceMotion`; `AppTheme.windowSize` -> `windowClass`;
  `WindowWidth`/`WindowHeight` from `foundation.adaptive` (also in `ShellChromePolicyTest`); `colors.divider` ->
  `separator`; `AppEmptyState` -> `AppStateView`; `MoreList`'s `AppListItem` -> `AppRow(showChevron = false)`,
  keeping WP04's own direction-aware chevron; `ScrolledUnder`'s tween on `springs.fadeQuick`/`enterEasing`.
  `ShellState.window` is WP03's `SizeClass` and a new `windowMetrics` holds the exact size, which the dev
  `ShellLog` prints as before (`window=411x891dp class=CompactxMedium`, the format `tools/adaptive` and
  `check_nav_log.py` read).
- **WP06's files** (its own sweep list): `DevToolsOverlay` (`pressable(Scale)`, `springs.sheet.unit`,
  `caption2`), `FlagsTab`, `AppLockGate` (`headline`, `callout`), `AppStatusGate` (`AppStateView`),
  `NotificationsRow` (`AppRow(subtitle)`). `PlayStorePrompts` had `AppSnackbarHost`, which WP03 removed: the
  update-ready message is now a toast through `LocalOverlayHost.toasts`, as `core/ui/MessageHost.kt` shows
  messages, with the restart action, the same 10 s, and shown once per process (saved); it no longer takes a
  `BoxScope` or `modifier`, and its one caller passed neither.
- **Elsewhere:** `CatalogNavigation` (`headline`, `style.borderWidth`, `colors.outline`), the feed's placeholder
  post gains WP06's `excerpt`.

## wp/HUD at 936fa2e (merge `aaab6fe4`, then `22992d3c`)

HUD branched from `wp/WP06` (already merged here) before WP04's navigation rewrite and WP03's design system, so
five files conflicted and its new module needed the same sweep.

| File | HEAD (WP04 + WP06 + WP03) | HUD | Kept, and why |
|---|---|---|---|
| `app/.../ui/AppRoot.kt` | WP04's `EntryHost` / `SingleStack` / `SignedIn` hosts in WP06's gates; `DevToolsOverlay(devTools)` | on the old root (one back stack or the old shell): `devTooling: DevTooling?` (an `Optional` binding, absent in prod), `devTooling?.Overlay()`, and `ReportCurrentScreen` beside each stack | WP04's structure with HUD's wiring: `devTooling` threaded (inside `opt:devtools`) to `SingleStack` and `SignedIn`, each reporting its top only while it is the host the app is on (`receivesCommands`: the identity crossfade keeps the departing host composed). The shell reports `presentedKeys.lastOrNull() ?: current.current`, the destination on top now that WP04's shell presents covers; HUD's old shell had none. `MainActivity` merged cleanly with HUD's `Optional<DevTooling>`. |
| `core/devtools/.../panel/DevToolsPanel.kt` | WP06's panel with WP03's sweep | keyed tabs with pages (`tabKeys`, `tabTitles`), `AppEnvironment`, `CurrentScreen` | HUD's panel, with `selected =` (WP03) and `WindowWidth` from `foundation.adaptive`; the `DevEnvironment` import, unused after HUD's change, dropped. |
| `generator/tests/consistency/hilt.py` | `_FIELD` accepts `@` in field types (WP04) | `_OPTIONAL` for `@BindsOptionalOf` | both. |
| `generator/tests/fixtures/v2/everything.json` | `baselineprofile` renamed `performance` (WP04) | adds `perfhud` | both, `perfhud` before `performance`. |
| `web/frontend/lib/catalogue.json` | | | regenerated with `--write`; `--check` current. |

After the merge (`22992d3c`): the overlay module (`core/perfhud`) on WP03's type scale (`titleSmall` -> `footnote`,
`bodySmall` -> `subheadline`, `bodyMedium` -> `callout`, `labelSmall` -> `caption2`, `caption` -> `caption1`,
`displaySmall` -> `title2`), colour roles (`divider` -> `separator`, `surfaceVariant` -> `fill`), `Tinted`,
`pressable(Scale)` for the pill and `DevToolsOverlayTest`'s probe, and `AppToggleRow(title)` where `AppSwitch` lost
its `label`. WP04 renamed the `baselineprofile` feature to `performance` and ships the checked-in baseline profiles
in every project, so the overlay's `<opt:baselineprofile>` blocks (its `profileinstaller` dependency and the
`ProfileVerifier` check) are unconditional now: every generated project failed on the unknown marker before.
`ProfileStatus.NotInProject` is left in place (reports and the score name it) but nothing produces it any more:
HUD's to remove. The fix text no longer tells the user to turn that feature on.

## Checks run on the combined tree (`22992d3c`)

- Every module's main and unit-test Kotlin compiles (`compileDebugKotlin compileDebugUnitTestKotlin` at the root,
  `:app:compileDevDebugKotlin :app:compileDevDebugUnitTestKotlin :app:compileDevReleaseKotlin`), logs
  `bugs/2026-10-19-back-licenses/logs/compile-*.log`.
- Generator: `tests.test_android_target` and `tests.test_manifest`, 68 tests, OK (`logs/gen-test-hud-2.log`);
  `catalogue --check` current.
- Not run: the modules' unit tests and the full generator suite (the preview's purpose is the phone reproduction;
  each owner's acceptance runs them at the real merge).

## Not done here (owners' follow-ups; none affects the back card)

- WP03's "When WP04 is merged" items 2-10 beyond what the conflicts forced: SheetScene on `Presentation.Sheet`,
  `AppBottomActionBar`'s assertion and height into `ScreenChrome.bottomBar`, the font preload, `AppTheme`'s
  `posture`/`input` from `:app` (AD-15), the catalog's padding (item 7), same-pass chrome (item 8), the fling sweep
  over WP04's screens (item 9), and `AppScaffold` writing `ScreenChrome.topBar`/`.background` (item 10).
- WP06 §19.8 (the DevTools panel onto `AppSheet`) and K13's real top bound, which needs item 10.

## The capture of the see-through back card (Oct 19, 18:40)

Build (a) of `bugs/2026-10-19-back-licenses/CAPTURE-PROCEDURE.md` is this branch at `22992d3c`, generated from the
everything fixture as `com.example.bug.combined` (devRelease). Its first scene, run by the procedure's writer, shows
the defect on this tree: a fast back swipe from the right edge on the licences page commits as a cross-fade of the
two pages, both half transparent (`check/shots-verify-1833/combined-licences-flick-right-now.png`, the recording in
`check/record-verify-1834/`). Held gestures stay one opaque card.
