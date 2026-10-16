# Adaptive UI rules

> **Draft for WP12** (REQUIREMENTS 23, 25). Rules only, from `research/ADAPTIVE-PLAN.md` (section numbers below) and
> `handoff/WP04.md` "Rules". The plan records most items as planned work; WP12 checks every rule id, token and API
> name against the merged code before shipping **[verify all]**. Test commands, device recipes and measured numbers
> (ADAPTIVE-PLAN §11.5–12.6) are not drafted here.

## 1. Why

Every generated app runs on phones, foldables, tablets, Chromebooks and desktop windows, and must stay adaptive after
agents add features. So the template forces it: build-time code rules, manifest checks, multi-size previews and
screenshot tests (REQUIREMENTS 23). Adaptivity must also cost nothing: a resize inside one size class recomposes no
screen root (REQUIREMENTS 22; ADAPTIVE-PLAN AC-4).

## 2. The contract (ADAPTIVE-PLAN §2)

| # | Rule | Enforced by |
|---|---|---|
| AC-1 | Layout decisions read `AppTheme.containerClass` (screens, components) or `AppTheme.windowClass` (shell only). Nothing reads orientation, device type, `Configuration.screenWidthDp` or `LocalWindowInfo` outside `foundation/adaptive/` | code rules `adaptive-orientation`, `adaptive-screen-metrics`, `adaptive-device-type`; lint |
| AC-2 | No orientation or resizability lock, in code or in the merged manifest | lint `LockedOrientationActivity`, `SourceLockedOrientationActivity`, `NonResizeableActivity`; `verifyManifestRules` |
| AC-3 | A resize, fold, rotation or keyboard attach does not recreate the Activity, rebuild a tab or lose a stack | manifest `configChanges`; `ResizeContinuityTest` |
| AC-4 | A resize within one class costs 0 recompositions of screen roots and at most 1 of the chrome | `ResizeContinuityTest` (recomposition harness) |
| AC-5 | Navigation chrome follows the window: bar, rail or sidebar; tabs and their state survive every flip | `ResizeContinuityTest` |
| AC-6 | Multi-pane navigation is declared (`entry(pane = …)`) and laid out by the shell; a feature never builds a two-pane `Row` for navigation | review; `adaptive-material-scope` |
| AC-7 | A detail beside its list has no Back arrow and is not full screen; the list marks the open row | `PaneContextTest` |
| AC-8 | Text never stretches past the readable width at Large and wider | `AdaptiveAuditTest` |
| AC-9 | No clipped text, no unreachable or overlapping controls, targets ≥ 48 dp, at the six sizes and font scales 1 and 2 | `AdaptiveAuditTest` |
| AC-10 | Every interactive component has hover, focus, keyboard activation and, where it has a long-press menu, a right-click menu. Esc closes the top overlay; Enter submits a single-line form | `InputTest`; review |
| AC-11 | State that must survive a pane move or process death lives in `rememberSaveable` or the ViewModel | `ResizeContinuityTest`; review |
| AC-12 | Every `XScreen` has an `@AppScreenPreviews` preview of its stateless UI | `adaptive-preview` |
| AC-13 | Two windows of the app are independent: navigation in one never moves the other | `MultiInstanceTest` |
| AC-14 | All of the above hold in every preset, lean to everything | generator tests |

## 3. Size classes (ADAPTIVE-PLAN §3.1)

Material's breakpoints (`WindowSizeClass.BREAKPOINTS_V2`), decided on window width first, never by device type.

| Width class | dp | Height class | dp |
|---|---|---|---|
| Compact | < 600 | Compact | < 480 |
| Medium | 600–839 | Medium | 480–899 |
| Expanded | 840–1199 | Expanded | ≥ 900 |
| Large | 1200–1599 | | |
| ExtraLarge | ≥ 1600 | | |

**Why our own source, not `currentWindowAdaptiveInfoV2()`:** the root must recompose only when a breakpoint is
crossed, and the design system must stay identical with the Compose Multiplatform copy (ADAPTIVE-PLAN §0 decision 2).

## 4. Layout tokens per class (ADAPTIVE-PLAN §3.4, dp)

| Token | Compact | Medium | Expanded | Large | ExtraLarge |
|---|---|---|---|---|---|
| `gutter` | 16 | 24 | 24 | 32 | 32 |
| `listPaneWidth` / `supportingPaneWidth` | — | — | 360 | 412 | 412 |
| `railWidth` | — | 88 | 88 | 88 | 88 |
| `sidebarWidth` | — | — | 280 | 280 | 304 |
| `readableMaxWidth` | full | 720 | 720 | 720 | 720 |
| `formMaxWidth` | 480 | 480 | 480 | 480 | 480 |
| `contentMaxWidth` | full | full | 1040 | 1040 | 1200 |
| `sheetMaxWidth` | 640 | 640 | 640 | 640 | 640 |
| `formSheetWidth` | — | 540 | 540 | 600 | 600 |
| `gridMinTileWidth` / `gridMinCardWidth` | 160 / 300 | same | same | same | same |

Panes: 1 below Expanded, 2 at Expanded, 3 partitions at Large and wider when an `Extra` entry exists. The pane spacer is
0 dp with a drawn 1 dp separator.

**Compact height** (a phone held sideways, a short split window; §3.5): the rail replaces the bar with labels hidden;
large titles render inline; a sheet opens at full height, capped at `sheetMaxWidth` and centred.

## 5. Navigation chrome: bar → rail → sidebar (ADAPTIVE-PLAN §4.1; `handoff/WP04.md` "Rules")

| Window | Chrome | Items |
|---|---|---|
| height Compact | rail, labels hidden | ≤ 7, else 6 + More |
| width Compact | floating bar | ≤ 5, else 4 + More |
| width Medium, Expanded | rail, 88 dp (sidebar toggle in the header at Expanded) | ≤ 7, else 6 + More |
| width Large, ExtraLarge | sidebar 280 / 304 dp, collapsible to the rail | every tab, grouped by section |

- One `TabbedNavHost` call site: only the chrome changes, so tabs, stacks and ViewModels survive every flip.
- A class flip is not animated (Android and iPadOS both relayout at once). A fold or resize never slides and composes
  nothing within a class.
- Sidebar collapse lays out once and animates transforms on `springs.layout`; no per-frame relayout.
- Each scaffolded tab gets a distinct icon pair; extra tabs go to More (REQUIREMENTS 18).

## 6. Panes (ADAPTIVE-PLAN §5; `handoff/WP04.md` "Rules")

```kotlin
// DO: declare panes; the shell decides when they sit side by side
entry<ListKey>(pane = NavPane.List) { … }
entry<DetailKey>(pane = NavPane.Detail) { … }
// a side panel: NavPane.Main + NavPane.Supporting

// DON'T: a hand-built two-pane Row for navigation, or Material scene strategies called from a feature
Row { ListScreen(…); DetailScreen(…) }
```

- A screen reads `LocalPaneContext` (role, `beside`, `selectedKey`) and hides its back arrow when shown beside its list.
- Material's `ListDetailSceneStrategy` / `SupportingPaneSceneStrategy` are allowed only inside
  `core/navigation/.../scene/`, behind `NavPane`; the experimental opt-in is confined to one file (§0 decision 1).
- Only `androidx.compose.material3.adaptive:*` is allowed; never `material3` components or
  `material3-adaptive-navigation-suite` (REQUIREMENTS 31).

## 7. Sizing

- No screen-sized fixed dimensions: `widthIn(max = AppTheme.layout.*)`, weights, `fillMax*` (code rule
  `adaptive-fixed-size`).
- Card grids: `LazyVerticalGrid(GridCells.Adaptive(AppTheme.layout.gridMinCardWidth))`. `Grid` and `FlexBox` are
  experimental in Compose 1.12; not without an explicit decision (§0 decision 3).
- No `BoxWithConstraints` in features: read `containerClass`, or write a custom `Layout` (SubcomposeLayout costs a
  frame; rule `adaptive-box-constraints`).
- No MediaQuery APIs: use `AppTheme.input` and `AppTheme.posture` (rule `adaptive-mediaquery`).

## 8. Foldables (ADAPTIVE-PLAN §7)

- **Tabletop** (half-opened, horizontal fold): media and image screens put the image above the fold and controls below;
  a sheet's medium detent rests on the fold line.
- **Book** (half-opened, vertical fold): list-detail splits at the hinge; `AppTwoPane` uses the hinge as its divider.
- **Continuity:** a fold or unfold changes the class without a transition and without recreating the Activity.
- Posture comes from one `WindowInfoTracker` source per Activity; flat, non-separating features are ignored.

## 9. Windows, multi-window, multi-instance (ADAPTIVE-PLAN §8; `handoff/WP04.md` "Rules")

- The Activity is never recreated by a resize, fold, keyboard or orientation change (`configChanges`). `AndroidView`
  and fragment hosts handle `onConfigurationChanged` themselves.
- Navigation is per window: `AppNavigator`, `PendingDeepLink`, `NavRegistry` and `NavKeySerialization` are
  `@ActivityRetainedScoped`. No UI state in singletons.
- Leave the system caption bar alone; `safeDrawing` covers it.

## 10. Input (ADAPTIVE-PLAN §9.2)

- Interactive things use `Modifier.pressable`: hover highlight (draw only, mouse and stylus), focus ring, Enter/Space,
  right-click (`onSecondaryClick`) opening the same menu as long-press, hand pointer on links and buttons.
- A right click never fires `onClick` (a defect found and fixed in `PressableNode`).
- Shortcuts: the shell answers Ctrl+1…9, Ctrl+[ and Alt+←, Ctrl+Shift+S; a screen adds its own with
  `ShortcutEffect(Shortcut(Key.F, label, ctrl = true)) { … }`, active only while resumed.
- Esc closes sheets, menus and the image viewer; Enter submits a single-line form (`AppTextField(onSubmit)`).
- Focus: `focusGroup()` on the chrome, `focusRestorer()` on lists, bar, rail and sidebar; in two panes, list then
  detail.
- Touch targets stay ≥ 48 dp even with a fine pointer.

## 11. Enforcement (ADAPTIVE-PLAN §11)

- `verifyCodeRules` ids: `adaptive-orientation`, `adaptive-screen-metrics`, `adaptive-device-type`,
  `adaptive-fixed-size`, `adaptive-box-constraints`, `adaptive-mediaquery`, `adaptive-material-scope`,
  `adaptive-preview`. Exceptions take `// allow(<id>): <reason>`.
- Lint errors: `LockedOrientationActivity`, `SourceLockedOrientationActivity`, `NonResizeableActivity`,
  `ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`, `LocalContextResourcesRead`,
  `UnusedBoxWithConstraintsScope`.
- `verifyManifestRules` (merged manifest) fails on missing `configChanges` flags, a missing multi-instance property,
  `screenOrientation`, `resizeableActivity="false"`, aspect-ratio limits, or `enableOnBackInvokedCallback` not `true`.
- Screenshots: Roborazzi matrix; never re-record goldens to make a test pass; show the diff.

## 12. Root causes found (ADAPTIVE-PLAN §15 item 2)

| Cause | Fix |
|---|---|
| A static window-size local holding raw dp recomposed the whole app, with skipping off, on every resize step | Classes from a gated source; the root recomposes only when a breakpoint is crossed |
| A singleton navigator: two windows shared one back stack | Per-window navigation (`@ActivityRetainedScoped`) |
| Rail overflow: 6+ tabs or font scale 2 clipped the rail | Items `heightIn(min = 56.dp)` in a scrolling column; More beyond 7 |
| Right-click fired `onClick` | Mouse secondary presses route to `onSecondaryClick` only |

## 13. Pre-merge checklist lines (ADAPTIVE-PLAN §15 item 3)

- `./gradlew check` green (rules, lint, manifest).
- `AdaptiveAuditTest` and `ResizeContinuityTest` green.
- Screens checked at phone, phone landscape, fold, 10-inch tablet and desktop (the Roborazzi compare report).
- Tab order, Esc and shortcuts; hover and right-click.

## 14. Differences from the vendored `adaptive` skill (ADAPTIVE-PLAN §15 item 4)

No `NavigationSuiteScaffold` (it needs material3); panes through `NavPane`; `LazyVerticalGrid` rather than `Grid` on
Compose 1.12; `AppTheme` traits rather than `MediaQuery`; Roborazzi rather than `com.android.compose.screenshot`;
`AppLargeTitleTopBar` rather than M3 scroll behaviours.
