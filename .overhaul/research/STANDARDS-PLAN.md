# Standards plan: package structure, enforced rules, vendored Android skills

Requirements 6, 7, 15, 25, 26 and 27, with 31. Written 2026-10-09 from `research/standards.md`,
`research/CODE-STANDARDS.md` and `research/adaptive.md`. Checked against:
- the integration branch at `709612d` (**int**);
- the in-flight worktrees: **WP02** (`CodeRules.kt`, uncommitted M2 work), **WP03** (design system, catalog) and
  **WP04** (navigation, shell, app);
- the Android skills checkout (v1.0.13, commit `42dc2270e96032bd860bb94511e440aa00a43125`);
- the generator engine (`generator/genkit/core/rename.py`, `manifest.py`).

Nothing in a repository or worktree was changed.

- Status of each item: **APPLIED**, **PARTIAL**, **MISSING**.
- Evidence: **[V]** checked today, **[U]** unverified, **[D]** our decision with its reason.
- Item ids `ST-nn` are what the handoff notes cite. Rule ids (C-, API-, M-, K-, A-, CO-, N-, L-, T-, B-, F-) are
  `CODE-STANDARDS.md`'s.

---

## 0. Corrections to the research (apply before using it)

| # | Research says | Fact | Decision |
|---|---|---|---|
| 1 | `standards.md` §2 #12, §4.4 and `CODE-STANDARDS.md` L-3: `Grid`/`FlexBox` are stable in Compose 1.12 | `foundation-layout-android` 1.12.0 and 1.12.1 carry `ExperimentalGridApi`/`ExperimentalFlexBoxApi` (`RequiresOptIn`) on `GridKt`/`FlexBoxKt`; the annotation classes are gone only in 1.13.0-alpha03 [V, bytecode] | Experimental on our BOM. Use `LazyVerticalGrid(GridCells.Adaptive(...))`. Rule `experimental-api` (§3.1) |
| 2 | `CODE-STANDARDS.md` T-5: `com.android.compose.screenshot`, nine sizes 400/610/900 × 400/500/1000 | The standalone plugin is deprecated from AGP 9.5.0-alpha03. Roborazzi 1.76.0 runs on the existing Robolectric `NATIVE` setup and covers KMP `androidHostTest` [V adaptive.md §6.3] | Roborazzi with the six-size matrix of ADAPTIVE-PLAN §11.5. Screenshot tests are generated from previews in `src/test`; there is no `screenshotTest` source set (the trees in `standards.md` §3.1 and §3.4 change accordingly) |
| 3 | `CODE-STANDARDS.md` L-2: features use `ListDetailSceneStrategy` | Requirement 31 allows it, but it installs its own back handler and pane motion [V androidx-main `ThreePaneScaffoldScene.kt`] | Features declare `entry(pane = NavPane.X)`. Only `:core:navigation` touches Material's strategies, after the spike (ADAPTIVE-PLAN §5.2) |
| 4 | `material` rule (WP02 `CodeRules.kt`) "Compose Material is not a dependency" | Its regex `import\s+androidx\.compose\.material(?!\w*\.ripple\b)` also rejects `androidx.compose.material3.adaptive.*`, which requirement 31 allows [V] | Exempt `material3.adaptive.` and confine it with `adaptive-material-scope` |
| 5 | `standards.md` §4.3: copy skills from `third_party/` with a new pipeline step | The renamer would corrupt them anyway. `testing-setup/references/android/training/dependency-injection/hilt-testing.md:373` has `BaseApplication`, and the `BaseApp` token with `prefix` boundary checks only the character before (`rename.py:13,63-64`), so it becomes `<Pascal>lication` [V] | Vendor in the template trees, marked `verbatim` (§4.3). This needs no new pipeline step and uses the existing `owns` and `sync.toml` |
| 6 | `standards.md` §3.7: move `ui/AppDestinations.kt` to `navigation/` | `generator/add_feature.py:184`, `remove_feature.py:109` and `targets/android/tabs.py` address `ui/AppDestinations.kt`; v1 apps have it there [V] | Keep `ui/AppDestinations.kt` and `ui/AppRoot.kt`. Move only startup and logging (§2.7) |
| 7 | `standards.md` §1.1 `styles-api` rule | The Style API is one of four experimental APIs we do not adopt | One rule, `experimental-api`: Style, Grid, FlexBox, MediaQuery |
| 8 | `standards.md` §1.1 `legacy-back` is "proposed" | WP03 already uses `BackHandler` in `AppActionSheet.kt:82`, `AppLoadingOverlay.kt:50`, `AppAlert.kt:76` and `PredictiveBackHandler` in `AppSheet.kt:278` [V, grep] | WP03 migrates these to `NavigationBackHandler` at overlay priority before the rule lands (§3.1) |

---

## 1. Principles (summary; the full argument is `standards.md` §3)

- Modules split by feature and data domain. **Packages inside a module split by screen or concept, never by layer**
  (no `ui/`, `viewmodel/`, `presentation/`, `util/`).
- The public surface of a feature or data module is its `api/` package (pure Kotlin, `verifyApiPurity`, APPLIED);
  everything else is `internal`.
- **`:core:navigation` and `:core:designsystem`: the root package and the component packages are the API.** In
  navigation, sub-packages hold `internal` code only, so a feature imports from one package.
- **Depth is earned.**
  - A screen package always exists.
  - `component/` appears when a composable is shared by two or more screens, and `model/` when a UI model or
    formatter is.
  - A sub-flow package appears for a multi-step journey, and `di/` only when the feature binds more than navigation.
  - The thresholds table is `standards.md` §3.
- Packages are identical in compact and modular mode (A2.1). `modes.py` moves `api/` only, and none of the moves
  below touch it.
- **Screen package names [D]:**
  - `list/` for a list of the feature's items;
  - `detail/` for one item;
  - `overview/` for a feature's single entry screen that is not a list (profile, settings root);
  - `query/` and `result/` for search;
  - a flow is named for the flow (`signin/`, `signup/`, `passwordreset/`, `intro/`).
  - Class names keep their screen name (`SettingsScreen` in `…settings.overview`).
- **File shape per screen:** `XContract.kt` (State, Event, Effect), `XViewModel.kt`, `XRoute.kt` (stateful),
  `XScreen.kt` (stateless, `@AppScreenPreviews` at the bottom). Screen-private pieces stay in `XScreen.kt` until they
  earn a file.

---

## 2. Target trees and the moves from today's trees

Every move is a **pure move commit**:
- `git mv`, the `package` line and imports only, with no behaviour change;
- one commit per module, titled for why ("Group the design system's foundation by what each part does");
- `./gradlew :<module>:compileDebugKotlin testDebugUnitTest` green;
- feature-TOML `owns` paths updated in the same commit (B.3: "any package updates `owns` paths for files it moves");
- generated goldens regenerated where a generator test compares trees.

Do the moves right after the owner's next integration merge and before its next feature step, so later diffs
read against the new layout. Kotlin `internal` spans packages within a module, so the moves need no visibility
changes.

### 2.1 Feature module (owner WP02-M3; scaffold output owner WP02-M2) — ST-01, ST-02

```
feature/orders/
├── build.gradle.kts                          plugins, namespace, dependencies only (B-2)
└── src/
    ├── main/kotlin/<pkg>/feature/orders/
    │   ├── api/OrdersKeys.kt                 PUBLIC, pure Kotlin: @Serializable keys : AppNavKey; results contracts
    │   ├── navigation/OrdersNavigation.kt    internal @Module: navGraph { entry<…>(pane = …) }, DeepLinkRoute (@IntoSet)
    │   ├── list/  OrderListContract.kt  OrderListViewModel.kt  OrderListRoute.kt  OrderListScreen.kt
    │   ├── detail/  (same four)
    │   ├── checkout/  CheckoutFlowState.kt  address/  payment/  review/      (a flow, only when one exists)
    │   ├── component/                        composables used by ≥ 2 screens
    │   ├── model/                            UI models and formatters used by ≥ 2 screens
    │   └── di/                               only when binding more than navigation
    ├── main/res/values/strings.xml           orders_* (F-5); values-<lang>/ beside it
    └── test/kotlin/<pkg>/feature/orders/list/OrderListViewModelTest.kt   (fakes from testFixtures; mirrors main)
```

Moves on **int** (`feature/**`, WP02-M3; do them with the WP03 call-site sweep, which rewrites by content and not
by path):

| Module | Today | Target |
|---|---|---|
| auth | `AuthNav.kt` | `navigation/AuthNavigation.kt` |
| | `SignIn.kt` (315 lines: state, events, effects, ViewModel, route, screen, preview) | `signin/{SignInContract,SignInViewModel,SignInRoute,SignInScreen}.kt` |
| | `SignUp.kt` (305) | `signup/{SignUpContract,SignUpViewModel,SignUpRoute,SignUpScreen}.kt` |
| | `PasswordReset.kt` (236) | `passwordreset/{…Contract,…ViewModel,…Route,…Screen}.kt` |
| | `AuthFrame.kt` (used by 3 screens) | `component/AuthFrame.kt` |
| | `GoogleSignIn.kt` (`GoogleSignInButton`) | `component/GoogleSignInButton.kt`; update `features/googlesignin.toml` `owns` |
| | `AuthMessages.kt` (error → text, used by 3 screens) | `model/AuthMessages.kt` |
| | `test/.../auth/SignInViewModelTest.kt` | `test/.../auth/signin/SignInViewModelTest.kt` |
| feed | `di/FeedNavModule.kt` | `navigation/FeedNavigation.kt` |
| | `FeedScreen.kt` (route + screen + `FeedCard`), `FeedViewModel.kt` (state + VM) | `list/{FeedContract,FeedViewModel,FeedRoute,FeedScreen}.kt` |
| onboarding | `OnboardingNav.kt` | `navigation/OnboardingNavigation.kt` |
| | `Onboarding.kt` (260) | `intro/{OnboardingContract,OnboardingViewModel,OnboardingRoute,OnboardingScreen,OnboardingPages}.kt` |
| profile | `di/ProfileNavModule.kt` | `navigation/ProfileNavigation.kt` |
| | `ProfileScreen.kt`, `ProfileViewModel.kt` | `overview/{ProfileContract,ProfileViewModel,ProfileRoute,ProfileScreen}.kt`; test into `overview/` |
| sample | `list/`, `detail/`, `component/SampleRow.kt` (used by both), `navigation/SampleNavigation.kt` | **APPLIED** (the reference shape) |
| search | `di/SearchNavModule.kt` | `navigation/SearchNavigation.kt` |
| | `SearchScreen.kt`, `SearchViewModel.kt` | `query/{SearchContract,SearchViewModel,SearchRoute,SearchScreen}.kt` |
| | `SearchResult.kt` | `result/{SearchResultContract,SearchResultViewModel,SearchResultRoute,SearchResultScreen}.kt` |
| settings | `SettingsNav.kt` | `navigation/SettingsNavigation.kt` |
| | `Settings.kt` (463) | `overview/{SettingsContract,SettingsViewModel,SettingsRoute,SettingsScreen}.kt` |
| | `Licenses.kt` | `licenses/{LicensesContract,LicensesViewModel,LicensesRoute,LicensesScreen}.kt`; update `features/licenses.toml` `owns` to the `licenses/` directory |

Three names for navigation registration (`navigation/SampleNavigation.kt`, `di/FeedNavModule.kt`, `AuthNav.kt`)
become one: `navigation/<Feature>Navigation.kt` holding `internal object <Feature>NavModule`.

**Scaffold (WP02-M2, `generator/genkit/targets/android/scaffold.py`).** Today it writes:
- `<Name>NavKeys.kt`, `<Name>Contract.kt`, `<Name>ViewModel.kt`, `<Name>Screen.kt` and `<Name>Route.kt` at the
  feature root (`scaffold.py:130-349`);
- `di/<Name>NavModule.kt`;
- APIs older than M1 (WP04 handoff, Open issues).

It must emit:
- `api/<Name>Keys.kt` (`<Name>ListKey`, `<Name>DetailKey(id: String)`);
- `navigation/<Name>Navigation.kt` with `pane = NavPane.List/Detail`;
- `list/` and `detail/` with the four files each and `@AppScreenPreviews` previews;
- `test/.../list/<Name>ListViewModelTest.kt`;
- the M1 APIs (`core.mvi`, `core.common.result`).

`add_feature.py` uses the same shape.

### 2.2 Data module (owner WP06) — ST-03

```
data/orders/src/
├── main/kotlin/<pkg>/data/orders/
│   ├── api/OrdersRepository.kt  api/model/{Order,OrderId,OrderStatus}.kt          PUBLIC, pure Kotlin
│   ├── OfflineFirstOrdersRepository.kt      internal (named for what it is, A-5)
│   ├── remote/OrdersApi.kt  remote/dto/OrderDto.kt  remote/OrderDtoMappers.kt
│   ├── local/OrderEntityMappers.kt          entities and DAOs stay in :core:database/…/orders/
│   ├── sync/                                (offlinequeue) queued writes and replay
│   └── di/OrdersDataModule.kt
├── testFixtures/kotlin/<pkg>/data/orders/api/FakeOrdersRepository.kt
└── test/kotlin/<pkg>/data/orders/{OfflineFirstOrdersRepositoryTest,remote/OrderDtoMappersTest}.kt
```

Moves on **int**:
- Each `api/<Model>.kt` goes to `api/model/` (`FeedPost`, `Profile`, `SampleItem`, `SearchResult`).
- Each `remote/<X>Dto.kt` goes to `remote/dto/`.
- DTO mapping moves out of the repository into `remote/<X>DtoMappers.kt` (`toDomain()`).
- `Default<X>Repository` is renamed only where a truer name exists (A-5). Example: `OfflineFirstSampleRepository`
  once `database` backs it.
- Fakes move to `testFixtures` (T-2): `FakeNetworkMonitor` stays in `:core:testing`.

### 2.3 Core infrastructure modules (owner WP06) — ST-04

| Module | Target sub-packages | Moves on **int** |
|---|---|---|
| `:core:network` | `client/` (NetworkClient, KtorNetworkClient, NetworkConfig, RetryPolicy, InFlightRequests, RequestAttributes), `response/` (ResponseUnwrapper, ResponseCache), `monitor/` (ConnectivityNetworkMonitor), `recording/` (NetworkObserver seam, from `NetworkRecording.kt`), `auth/`, `websocket/`, `model/`, `di/` | ten root files into `client/`, `response/`, `monitor/`, `recording/` |
| `:core:datastore` | `settings/AppSettingsStore.kt`, `session/AuthTokenStore.kt`, `crypto/`, `di/` | two root files |
| `:core:database` | `AppDatabase.kt`, `migration/`, `converter/`, `<domain>/` (entity + DAO per file), `queue/` (offlinequeue), `di/` | `Note.kt` into `note/{NoteEntity,NoteDao}.kt` |
| `:core:devtools` | `badge/`, `panel/`, `log/` (DevToolsLog, NetworkExchange), `DevEnvironment.kt` | five root files |
| `:core:media` | `picker/`, `compress/` (ImageCompressor, CompressionSettings, VideoTranscoder), `permission/` | six root files |
| `:core:analytics` | `AnalyticsTracker.kt` (seam) + `firebase/` (FirebaseAnalyticsTracker, CrashlyticsReporter), `di/` | so `features/firebase.toml` owns one directory |
| `:core:flags` | `FeatureFlags.kt`, `Flag.kt`, `remoteconfig/`, `di/` | one file |
| `:core:notification`, `:core:applock`, `:core:playstore` | `push/`, `channel/`, `permission/` as they grow (A2.2 moves from `app/{push,lock,playstore}`) | with the A2.2 moves |

`NetworkClient.kt`'s hard-coded `Dispatchers.Default` (CO-1) and the `runCatching` around suspend calls in
`ResponseCache.clear()` (A-8) are fixed in the same package (rules `dispatchers-io`, `run-catching`).

### 2.4 `:core:designsystem` (owner WP03; WP04 owns the navigation files and `Chrome.kt`) — ST-05

```
core/designsystem/src/main/kotlin/<pkg>/core/designsystem/
├── theme/          AppTheme, AppColors, AppTypography, AppMetrics, AppMotion (AppSprings), AppDesignStyle, StyleTones,
│   │               AppFonts, ReduceMotion, Palette
│   └── tokens/DesignTokens.kt          GENERATED, internal (A9.2)
├── foundation/
│   ├── interaction/  Pressable.kt  PressFeedback.kt  RowHighlight.kt  MinimumTouchTarget.kt  AppHaptics.kt
│   ├── scroll/       AppOverscroll.kt  ScrollEdge.kt  ScrollIndicator.kt (ADAPTIVE-PLAN §9.2)
│   ├── layout/       Chrome.kt (WP04)  Bleed.kt
│   ├── draw/         Hairline.kt  GroupItemShape.kt  StyleShadow.kt  DisabledAlpha.kt
│   ├── adaptive/     SizeClass.kt  AppLayout.kt  WindowPosture.kt  InputTraits.kt  WindowMetrics.kt  AdaptiveLayout.kt
│   └── content/      ContentColor.kt  CurrentLocale.kt  AppSurface.kt
├── animation/      AppTransitions.kt  Emphasis.kt
├── icon/           AppIcons.kt (the accessor object)  glyph/<Family>.kt (path builders, internal)
├── preview/        AppScreenPreviews.kt  AppPreviewTheme.kt (ADAPTIVE-PLAN §11.4)
└── component/      one public component per file (K-2), XDefaults/XColors/XState in the same file
    ├── text/ button/ input/ selection/ list/ container/ navigation/ overlay/ feedback/ refresh/   (core)
    └── pickers/ formfields/ datadisplay/ collections/    (optional; package name = feature key)
```

Moves from the **WP03** tree:

| Today | Target |
|---|---|
| `foundation/Pressable.kt` (519) | `foundation/interaction/{Pressable,PressFeedback,RowHighlight,MinimumTouchTarget}.kt` (`PressFeedback` + `NoIndication` in `PressFeedback.kt`) |
| `foundation/AppHaptics.kt` | `foundation/interaction/` |
| `foundation/{AppOverscroll,ScrollEdge}.kt` | `foundation/scroll/` |
| `foundation/Chrome.kt` | `foundation/layout/` (WP04's file; WP04 moves it) |
| `foundation/Modifiers.kt` (`disabledAlpha`, `bleed`, `hairline`, `Shape.groupItem`) | `foundation/draw/{DisabledAlpha,Hairline,GroupItemShape}.kt`, `foundation/layout/Bleed.kt` |
| `foundation/StyleModifiers.kt` | `foundation/draw/StyleShadow.kt` |
| `foundation/AdaptiveLayout.kt`, `theme/AppWindowSize.kt` | `foundation/adaptive/` (split per ADAPTIVE-PLAN §3.2) |
| `foundation/{ContentColor,CurrentLocale,AppSurface}.kt` | `foundation/content/` |
| `component/data/` | `component/datadisplay/`; split `AppDataDisplay.kt` (358: `AppStatTile`, `AppRating`, `AppSparkline`, `AppBarChart`, `AppKeyValueGrid`) one per file |
| `component/fields/` | `component/formfields/` (`AppDecimalFields.kt` stays one file: `AppDecimalField`, `AppMoneyField` and `DecimalInput` are one family) |
| `component/selection/AppSelection.kt` | `AppCheckbox.kt`, `AppRadio.kt` |
| `component/feedback/AppStateViews.kt` | `AppStateView.kt` (+ `StateKind`), `AppBanner.kt` |
| `component/overlay/AppSheet.kt` (463) | `AppSheet.kt` + `AppSheetState.kt` |
| `icon/AppIcons.kt` (709) | `AppIcons.kt` + `icon/glyph/{Navigation,Actions,Content,Status,Filled}.kt` |
| `component/navigation/{AppBottomBar,AppNavigationRail}.kt` | deleted at the WP04 merge (WP03 handoff) |

Then the generator's `features/{pickers,formfields,datadisplay,collections}.toml` (WP03 phase B, step 5a) each own one
directory plus their catalog section file.

### 2.5 `:core:navigation` (owner WP04; WP03 owns `SheetScene.kt`) — ST-06

```
core/navigation/src/main/kotlin/<pkg>/core/navigation/
├── AppNavigator.kt  NavGraph.kt (navGraph, NavGraphBuilder, NavGraphEntry, NavPane)  NavTransitionStyle.kt
├── Presentation.kt  DeepLinks.kt (DeepLink, DeepLinkRoute, PendingDeepLink, OpenDeepLinks)  ScrollToTopOnReselect.kt
├── AppShell.kt (AppShell, ShellConfig, ShellTab, AppNavigationHost)  ShellState.kt     ← the whole public API
├── shell/          ShellChromePolicy.kt  ShellBar.kt  BottomChromeWriter.kt  BarAnchor.kt  MoreTab.kt  Shortcuts.kt
│   └── tabs/       KeptTabs.kt  TabbedNavHost.kt
├── stack/          AppBackStack.kt  NavKeySerialization.kt  StackHost.kt  EntryFrame.kt
├── back/           BackGate.kt  NavGuard.kt  PredictiveBackCard.kt (predictive-back/SPEC.md)
├── transition/     NavTransitions.kt  SceneMotion.kt  CoverScrim.kt
├── scene/          StackScene.kt (SMOOTHNESS-PLAN; today KeepUnderScene.kt)  PaneStrategies.kt  PaneScene.kt (fallback)
│                   PaneContext.kt  SheetScene.kt (WP03)
├── presentation/   PresentationHost.kt
├── registry/       NavRegistry.kt
└── di/NavigationModule.kt                     (ActivityRetainedComponent, ADAPTIVE-PLAN §8)
core/navigation/src/main/kotlin/<pkg>/feature/<owner>/api/     shared keys (A2.4, unchanged)
```

Moves from the **WP04** tree (22 flat files):

| Today | Target |
|---|---|
| `NavRegistry.kt` | `NavGraph.kt` (public builder) + `registry/NavRegistry.kt` (internal) |
| `ListDetailScene.kt` (`NavPane` + scene) | `NavPane` → `NavGraph.kt`; the rest → `scene/PaneScene.kt` (renamed strategy, ADAPTIVE-PLAN §5.1) |
| `AppNavKey.kt` (only `NavTransitionStyle`) | `NavTransitionStyle.kt` |
| `TabbedNavHost.kt` (`ScrollToTopOnReselect` is public) | `ScrollToTopOnReselect.kt` + `shell/tabs/TabbedNavHost.kt` |
| `AppShell.kt` (311) | `AppShell.kt` (public) + `shell/{ShellBar,BottomChromeWriter}.kt` |
| `KeptTabs.kt` | `shell/tabs/` |
| `BarAnchor.kt` | `shell/` |
| `AppBackStack.kt`, `NavKeySerialization.kt`, `StackHost.kt` | `stack/` |
| `BackGate.kt`, `NavGuard.kt` | `back/` |
| `NavTransitions.kt`, `SceneMotion.kt`, `CoverScrim.kt` | `transition/` |
| `KeepUnderScene.kt` | `scene/` |
| `PresentationHost.kt` | `presentation/` |
| `AppNavigator.kt`, `ShellState.kt`, `DeepLinks.kt`, `Presentation.kt` | stay at the root (public) |

`AppBackStack` and `rememberAppBackStack` are used by `:app` for the onboarding and signed-out hosts. Either re-export
them through `AppNavigationHost` (preferred, so `stack/` stays internal) or keep `AppBackStack.kt` at the root [D:
WP04 picks one and states it].

### 2.6 `:core:ui`, `:core:mvi`, `:core:common`, `:core:testing` (owner WP02) — ST-07

| Module | Target | Moves on **int** |
|---|---|---|
| `:core:ui` | `screen/` (MviScreen, MessageHost), `text/` (AppErrorText, UiTextExt), `image/` (AppNetworkImage), `paging/`, `form/`, `browser/`, `locale/` | four root files. `AppLottie.kt` is deleted with the `lottie` feature (A4.7). `AppPagingList` takes a list state and modifier (WP04 open issue: the feed list cannot carry `primary_list`) |
| `:core:mvi` | flat (four files) | none: depth is earned |
| `:core:common` | `result/`, `coroutines/`, `log/`, `navigation/`, `network/`, `session/`, `time/`, `validation/` | **APPLIED**. Add `result/RunSuspendCatching.kt`: `suspend inline fun <T> runSuspendCatching(block: suspend () -> T): Result<T>`, which rethrows `CancellationException` (the fix that rule `run-catching` names) |
| `:core:testing` | stays plain JVM (no Compose): `MainDispatcherRule.kt`, `FakeNetworkMonitor.kt` | none |
| `:core:testing:compose` (new, `core/testing/compose/`, the Compose-enabled sibling `RECOMPOSITION-PLAN.md` §3.1 asks this file to name) | `recomposition/` (`RecompositionCounter`, `framesUntilIdle`), `theme/` (`setThemed {}`, moved from WP03's tests), `navigation/` (back-input helpers), `adaptive/` (`AdaptiveAudit` assertions, `WindowSizes` = the six PR sizes of ADAPTIVE-PLAN §11.5) | new module; check that `ModuleGraph` classifies the nested path as core |

### 2.7 `:app` (owners per B.3: WP04 `MainActivity`, `ui/**`, `deeplink/**`; WP02 `BaseAppApplication.kt`, `di/**`; WP06 `session/`, `push/`, `lock/`, `playstore/`, `widget/`) — ST-08

```
app/src/main/kotlin/<pkg>/
├── BaseAppApplication.kt  MainActivity.kt
├── ui/AppRoot.kt  ui/AppDestinations.kt        (kept: generator tools address these paths, §0 #6)
├── startup/StartupViewModel.kt  startup/LaunchHook.kt     (from ui/)
├── deeplink/LinkActivity.kt
├── session/SessionCoordinator.kt  session/SessionModule.kt
├── log/AndroidLogger.kt                        (today a private class inside BaseAppApplication.kt)
└── di/AppModule.kt  CoroutinesModule.kt  ClockModule.kt  FeatureBindingsModule.kt
```

`lock/`, `playstore/` and `push/` leave for their core modules, and `widget/` is deleted (A2.2, WP06).
`deeplink/DeepLinkResolver.kt` is gone on WP04 (APPLIED there).

### 2.8 `:catalog` (owner WP03) — APPLIED

The WP03 tree already has `catalog/sections/Catalog<Family>.kt`, one per family. `CatalogComponents.kt` (658 lines) is
deleted. This meets `standards.md`'s intent, so no deeper tree is needed. WP03 adds `sections/CatalogAdaptive.kt`
(ADAPTIVE-PLAN §6).

### 2.9 `build-logic` (owner WP02-M2) — ST-09

```
build-logic/convention/src/main/kotlin/
├── AndroidApplicationConventionPlugin.kt …      plugin classes in the default package (NiA does the same)
└── <pkg>/buildlogic/
    ├── guard/      ModuleGraph.kt  ApiPurity.kt  CodeRules.kt  KotlinSource.kt  LazyKeys.kt  HardcodedCopy.kt  ManifestRules.kt
    ├── android/    AndroidCommon.kt  Compose.kt  ComposeStability.kt  ReleaseArtifacts.kt  Screenshots.kt
    ├── config/     AppConfig.kt  Flavors.kt  Signing.kt
    ├── licenses/   BundleLicensesTask.kt
    └── ProjectExtensions.kt
```

Today these are 12 flat files in `com/base/app/buildlogic/` (WP02). Plugins and tests reference them, so do the move
in the M2 step that touches them anyway.

### 2.10 `benchmark/`, `tools/perf/` (owner WP04)

`benchmark/…/benchmark/<journey>/` (`tabs/`, `pushpop/`, `rapidback/`, `scroll/`, `startup/`, `adaptive/`), plus
`Journeys.kt`/`Selectors.kt` at the root (A3.8). The perf tools stay flat (Python scripts and `gates.toml`).

### 2.11 Compose Multiplatform (owner WP07)

- Same module names (A7.1). Inside each module:
  - `src/commonMain/kotlin/<pkg>/…` mirrors the Android packages above, byte-identical where `sync.toml` pairs them;
  - `expect` declarations live in the package that uses them;
  - actuals are `X.android.kt` and `X.ios.kt` in the same package of `androidMain`/`iosMain`;
  - tests are `commonTest` (camelCase names, K-8) and `androidHostTest`.
- `sync.toml` gains the `foundation/<sub>/` paths after §2.4 lands, so a later move breaks the sync check loudly
  instead of silently.

### 2.12 SwiftUI (owner WP08)

```
Packages/Core/Sources/DesignSystem/{Theme,Tokens,Foundation/{Interaction,Scroll,Adaptive},Components/<Family>}/
Packages/Core/Sources/Navigation/{Shell,Router,DeepLinks}/
Packages/Features/Sources/<X>Feature/{List,Detail,Components}/   <X>ListView.swift  <X>ListStore.swift  …
Packages/Features/Sources/<X>Data/{API,Remote/DTO,Local}/
```

Folder = concept, one public type per file, stores beside their views, the same "earned depth" thresholds.
`test_target_ios.py` asserts the folder rules for the template's features.

---

## 3. Rules and how each is enforced

### 3.1 `verifyCodeRules` (owner WP02-M2; `guard/CodeRules.kt`; each rule has a fixture that fires and one the allow marker silences) — ST-10

The marker is `// allow(<id>): <reason>` on the line above; a marker without a reason fails (C-7). The comment rules
need `KotlinSource` to expose comment text with line numbers, besides the code lines it already strips.

| Id | Fails on (sketch) | Scope | Fix it names | Source rule | State |
|---|---|---|---|---|---|
| `ripple` | `import androidx.compose.material*.ripple` | all | `Modifier.pressable(feedback = …)` | A11.3 #4 | PARTIAL (WP02, uncommitted) |
| `material` | `import androidx.compose.material…`, **except** `material3.adaptive.` | all | design-system component | req 31 | PARTIAL: exempt `material3.adaptive` (§0 #4) |
| `composed` | `Modifier.composed` / `composed {` | all | `Modifier.Node` | M-1 | PARTIAL |
| `nav-fade` | `NavDisplay.transitionSpec`/`popTransitionSpec`/`predictivePopTransitionSpec` | `feature/**` | `NavTransitionStyle` | N-2 | PARTIAL |
| `collect-as-state` | `.collectAsState(` | `feature/**`, `core/ui` | `collectAsStateWithLifecycle()` | A-2 | PARTIAL |
| `dispatchers-io` | `Dispatchers.IO\|Default` | all but `app/**/di/**`, tests | `AppDispatchers` | CO-1 | PARTIAL |
| `raw-tween` | `tween(`/`spring(` | `feature/**`, `core/ui` | `AppTheme.springs` | A11.3 #11 | PARTIAL |
| `raw-color` | `Color(0x` | `feature/**`, `core/ui` | `AppTheme.colors` | A11.3 #11 | PARTIAL |
| `raw-dp` | `N.dp` other than 0 and 1 | `feature/**` | spacing and size tokens | A11.3 #11 | PARTIAL |
| `subcompose-image` | `SubcomposeAsyncImage` | `feature/**`, `core/ui` | `AsyncImage` | A11.3 #6 | PARTIAL |
| `bare-clickable` | `.clickable(`, `.combinedClickable(` | `feature/**`, `core/ui` | `pressable` | A11.3 #4 | PARTIAL |
| `lazy-key` | `items(`/`itemsIndexed(` without `key =` | `feature/**`, `core/ui` | stable keys | A11.3 #13 | PARTIAL |
| `blocking` | `runBlocking`, `GlobalScope` | main | ViewModel or `ApplicationScope` | CO-4 | PARTIAL |
| `hardcoded-copy` | string literals shown to people | `feature/**`, `app`, `core/designsystem`, `core/ui` main | `strings.xml` | F-5 | PARTIAL |
| `compose-compiler` (pseudo) | `@Composable` in a module without the compiler plugin | all | the compose convention | — | PARTIAL |
| `kyant` | `import com.kyant` | `feature/**` | the glass seam | A4.9 | MISSING (WP05 adds) |
| `slop-comment` | a comment starting with `Added`, `Updated`, `Fixed`, `Changed`, `Removed`, `New:`, `Step <n>`, `As requested`, `Per review`, `IMPORTANT`, `NOTE:`, `CRITICAL`, `PERF:`, `HACK`, `FIXME`, `TODO`, `XXX`; a comment that is only `─ = - * #` runs (≥ 4) or contains `── ` banners | all Kotlin, main and test | C-1/C-2 voice; a TODO needs an allow marker naming the owner and condition | C-2 | MISSING |
| `commented-code` | a `//` comment (not KDoc) whose text starts with `val\|var\|fun\|import\|return\|if (\|for (\|while (\|when\|class\|object\|override\|private\|internal ` or ends in `{`, `}` or `;` | all Kotlin | delete it (git keeps history) | C-2 | MISSING |
| `run-catching` | `runCatching {` | main of `core/**`, `data/**`, `app/**` | `runSuspendCatching` (rethrows cancellation, §2.6) or a classifying `catch` | A-8 | MISSING (33 hits on int; most wrap non-suspending code and take a marker) |
| `bang-bang` | `!!` in code | main | restructure, or `checkNotNull(x) { "why" }` | K-6 | MISSING |
| `experimental-api` | `import androidx.compose.foundation.style.`, `ExperimentalFoundationStyleApi`, `ExperimentalGridApi`, `ExperimentalFlexBoxApi`, `ExperimentalMediaQueryApi` | all | stable equivalents; an opt-in needs a decision, recorded in the marker | §0 #1, #7 | MISSING (replaces `styles-api`) |
| `legacy-back` | `BackHandler(`, `PredictiveBackHandler(`, `OnBackPressedCallback`, `onBackPressedDispatcher` | `feature/**`, `core/ui`, `core/designsystem`, `core/navigation` | `NavigationBackHandler` (overlay priority for overlays) | N-5 | MISSING; WP03 has 4 sites (§0 #8) |
| `layer-package` | a package segment `ui`, `viewmodel`, `presentation`, `screens`, `util`, `utils`, `helper`, `helpers`, `misc`, `base`, `common` | `feature/**`, `data/**`, `core/designsystem`, `core/navigation` | name the screen or concept | F-2, F-3 | MISSING |
| `feature-public` | a top-level declaration without `internal` or `private` in `feature/**/src/main` outside `api/` | `feature/**` | `internal` (Hilt modules and `@HiltViewModel` classes can be internal; the template's already are) | K-5 | MISSING |
| `file-length` | a main Kotlin file over 500 lines | main | split by concept (K-2) | K-2 | MISSING; offenders: WP03 `AppIcons.kt` 709 and `Pressable.kt` 519, WP04 `AppTabBar.kt` 577 (split, or a marker on the icon accessor) |
| `spaced-test-name` | a backticked function name with a space | `src/androidTest`, `benchmark/**`, `commonTest` | camelCase with underscores | K-8 | MISSING |
| `adaptive-orientation`, `adaptive-screen-metrics`, `adaptive-device-type`, `adaptive-fixed-size`, `adaptive-box-constraints`, `adaptive-mediaquery`, `adaptive-material-scope`, `adaptive-preview` | see ADAPTIVE-PLAN §11.1 | see there | see there | L-1…L-3 | MISSING |

False positives (risk R27): before landing, the template with every feature on must pass with **at most ten** allow
markers in total. A rule that needs more is narrowed, not marked around.

### 3.2 Lint (owner WP02-M2; `android/AndroidCommon.kt`) — ST-11

- **Errors in every Compose module:**
  - the M-5 list: `ComposableNaming`, `ComposableLambdaParameterNaming`, `CompositionLocalNaming`,
    `ModifierParameter`, `ModifierFactoryReturnType`, `UnnecessaryComposedModifier`, `SuspiciousModifierThen`,
    `UnrememberedMutableState`, `UnrememberedAnimatable`, `UnrememberedState`, `RememberReturnType`,
    `RememberInComposition`, `AutoboxingStateCreation`, `AutoboxingStateValueProperty`,
    `MutableCollectionMutableState`, `FrequentlyChangingValue`, `OpaqueUnitKey`, `ProduceStateDoesNotAssignValue`,
    `NonObservableLocale`, `MultipleAwaitPointerEventScopes`, `ExitAwaitPointerEventScope`,
    `UnusedBoxWithConstraintsScope`, `UseOfNonLambdaOffsetOverload`, `SceneEqualsHashCode`;
  - the adaptive set: `ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`,
    `LocalContextResourcesRead`;
  - the three orientation and resizability ids, which are **APPLIED** (`AndroidCommon.kt:47-51`).
- `ResourceName` as an error, with `resourcePrefix = "<module>_"` set by the library convention (F-5).
  - `:core:ui`'s `common_error_*` strings get `resourcePrefix = "common_"` or a rename.
- `:app`: `abortOnError = true`, `checkDependencies = true`. No `lint-baseline.xml` in the template.
- Verify every id against `./gradlew :app:lint --list` (an unknown id is ignored silently).
  - Take the coroutine, flow-in-composition and `Lifecycle.currentState` detector ids from the same listing and add
    them.

### 3.3 Other build guards — ST-12

| Guard | Owner | State |
|---|---|---|
| `ModuleGraph`, `verifyApiPurity` | WP02 | **APPLIED** (WP02-M1, merged) |
| `verifyManifestRules` (merged manifest: configChanges, multi-instance property, no locks, `exported` everywhere, `enableOnBackInvokedCallback`) | WP02-M2 | MISSING (ADAPTIVE-PLAN §11.3) |
| Compose stability and compiler metrics gate (no unstable parameters in public design-system and shell APIs) | WP02-M2 (task), WP03/WP04 (clean code) | per `RECOMPOSITION-PLAN.md` |
| Formatting: ktlint `android_studio` through the `formatting` feature's Spotless setup, 100 columns, `.editorconfig` 120 → 100; the template itself passes it even when the feature is off | WP02-M2 step 9; one mechanical reformat commit after the last UI wave (orchestrator schedules) | MISSING (WP03 tree: 955 lines over 100) |
| `explicitApi()` in `:core:designsystem`, `:core:navigation`, `:core:mvi`, `:core:common` (K-5) | WP02-M2 | tier 2, after WP03/WP04 merge |
| Screenshot convention plugin and audit tests | WP02-M2 (plugin), WP03/WP04/WP02-M3 (content) | MISSING (ADAPTIVE-PLAN §11.5–11.6) |
| Vendored-skills consistency test (§4.6) | WP02-M2 | MISSING |

### 3.4 Review only (the fresh-context reviewer of A12.8 cites the ids)

These rules have no mechanical check:
- C-1 and C-3 (is the comment a *why*?), C-4 and C-5 (KDoc presence and shape on the public API of core modules and
  `api/`);
- API-2 to API-15:
  - parameter order; `modifier` applied first to the root; no modifier-shaped parameters (`fillWidth`);
  - slots; one `XDefaults`/`XColors` per component, with no `null` meaning "default";
  - no `State<T>` parameters; emit or return; accessibility semantics;
- M-2 (animated reads in layout or draw beyond what `FrequentlyChangingValue` catches) and M-3 (foundation primitive
  first);
- K-3, K-4 (no mixed constant casing in one file), K-6 (`when` over a sealed type without `else`) and K-7;
- A-1 to A-7, CO-2 to CO-6, N-1 to N-6, L-5, T-1 to T-6, B-2 to B-6, F-1 and F-6;
- whether the relevant Android skill was followed, or the AGENTS.md differences table says why not.

Concrete component-API work found by `standards.md` §1 (owner WP03, WP04 for the tab bar):
- four `XDefaults` objects for about a hundred public functions;
- no `colors:` parameter anywhere;
- `AppButton(fillWidth = …)`;
- `AppTheme(colors: AppColors? = null)`;
- `Palette.kt` and `AppFonts.kt` mixing constant casing.

### 3.5 Reviewer checklist (paste into the A12.8 prompt)
`standards.md` §1.2 items 1–9, plus:
- 10. ADAPTIVE-PLAN AC-1…AC-13 hold for any screen or component touched.
- 11. No file moved and edited in the same commit.

---

## 4. Vendored Android skills (requirement 26) — ST-20…ST-24

### 4.1 Which skills each template ships (upstream v1.0.13, commit `42dc227`)

| Skill (upstream path) | Android | Multiplatform | SwiftUI | Why or when |
|---|---|---|---|---|
| `navigation-3` (`navigation/navigation-3`) | yes | yes | — | destinations, scenes, deep links, results |
| `navigation-event` (`navigation/navigation-event`) | yes | yes | — | Back outside `NavDisplay` |
| `adaptive` (`jetpack-compose/adaptive`) | yes | yes | — | every screen and layout |
| `edge-to-edge` (`system/edge-to-edge`) | yes | yes (androidApp, common UI) | — | insets, IME, system bars |
| `testing-setup` (`testing/testing-setup`) | yes | yes | — | tests and their infrastructure |
| `android-profiler` (`profilers/android-profiler`) | yes | yes | — | jank, startup, memory. Its `references/perfetto/bin/trace_processor` is Google's launcher script, which downloads the trace processor binary when run [V, file header] |
| `r8-analyzer` (`performance/r8-analyzer`) | yes | yes (androidApp) | — | keep rules and size; upstream has no `scripts/`, so use its manual path |
| `android-intent-security`, `android-permissions-security` (`security/…`) | yes | yes | — | manifest, deep links, `PendingIntent`s, permissions |
| `agp-9-upgrade` (`build-system/agp/agp-9-upgrade`) | yes | **no** (the skill excludes KMP) | — | AGP and Gradle bumps |
| `restore-credentials`, `verified-email` (`identity/…`) | when `auth` | when `auth` (phase C) | — | Credential Manager flows |
| `styles` | **no** | **no** | — | experimental API (§0 #7) |
| `android-cli` | **no** | **no** | — | its install step pipes a downloaded script into a shell |
| `camerax`, `play-policy-insights`, wear, tv, xr, cast, engage, billing, appfunctions, ML Kit, XML migration | no | no | — | no template feature uses them |

Measured sizes: the always-on Android set is 1,324 KB, and the two identity skills add 268 KB, mostly Markdown [V,
`du`]. The SwiftUI template vendors none: there is no equivalent official Apple set. Its `AGENTS.md` names Apple's
documentation instead.

### 4.2 Layout in a generated project

```
.agents/skills/
├── README.md              provenance: https://github.com/android/skills, tag v1.0.13, commit 42dc2270e9…, the list
│                          shipped, "files below each skill folder are unmodified upstream copies; LICENSE.txt added",
│                          how to refresh (`android skills add <name> --project=.`, then keep this folder the only copy),
│                          and "where a skill disagrees with AGENTS.md, AGENTS.md wins"
├── MANIFEST.sha256        one line per vendored file (lets the generator test prove the copies are unmodified)
├── LICENSE.txt            Apache-2.0, from upstream's root
└── <skill>/               verbatim upstream folder (SKILL.md + references/ …) + LICENSE.txt (SKILL.md's front matter
                           says "license: Complete terms in LICENSE.txt"; upstream keeps it only at its root)
.claude/skills/<skill>/SKILL.md     pointer (below), one per vendored skill
```

- Agents look in these places [V standards.md §4.2]:
  - Codex reads `.agents/skills/`;
  - Gemini CLI reads `.gemini/skills/` or the `.agents/skills/` alias;
  - Android Studio reads `.agents/skills/` or `.android-studio/skills/`;
  - Claude Code reads `.claude/skills/`;
  - the r8-analyzer text hard-codes `.agents/skills/r8-analyzer/`.
- Pointers rather than symlinks or a second copy: one copy to update, and symlinks do not survive the website's zip
  on Windows. [U: whether Claude Code also reads `.agents/skills/`; if it starts to, the pointers become redundant.]
- The pointer is ours, and the tool writes it. It does not repeat the project's differences, so `AGENTS.md`'s table
  (WP12) stays their only source:

```markdown
---
name: adaptive
description: <upstream description, verbatim>
---

Google's `adaptive` skill is vendored at `.agents/skills/adaptive/` (Android skills v1.0.13, Apache-2.0).
First read the `adaptive` row of the "Google's Android skills" table in `AGENTS.md`: it lists where this project
differs, and `AGENTS.md` wins wherever the two disagree. Then read `.agents/skills/adaptive/SKILL.md` and resolve every
relative path in it from that folder.
```

### 4.3 Generator mechanism (owners: WP02-M2 executes; the engine files are WP01's, which has merged, so the orchestrator confirms the executor)

1. **Vendored copies live in the templates:** `templates/android/.agents/skills/…` and
   `templates/multiplatform/.agents/skills/…`.
   - The template is what you get: agents editing the template see the same skills.
   - Preset filtering reuses `owns`.
   - Android/CMP parity reuses `sync.toml` and `tools/sync_check.py`.
2. **New `[files] verbatim = [".agents/skills/**"]` in `template.toml`** (`genkit/core/manifest.py` accepts the key;
   the pipeline skips renaming and marker parsing for matching paths; `test_manifest.py`/`test_rename.py` cover it).
   Without it the renamer corrupts `hilt-testing.md:373` (§0 #5). Upstream Markdown must never be marker-parsed
   either.
3. **`features/auth.toml`** `owns` gains `.agents/skills/restore-credentials`, `.agents/skills/verified-email` and the
   two `.claude/skills/…` pointers.
4. **`tools/update_android_skills.py <checkout> [--tag vX.Y.Z]`:**
   - copies the §4.1 set into both templates, minus `agp-9-upgrade` for multiplatform;
   - adds `LICENSE.txt`, rewrites `README.md`, `MANIFEST.sha256` and the pointers;
   - prints a per-file diff summary for review.

   It is never run by the generator at generation time.
5. **The generator repository's own** `.agents/skills/<skill>/SKILL.md` and `.claude/skills/<skill>/SKILL.md` are
   pointers into `templates/android/.agents/skills/<skill>/`, so there is no third copy. Claude Code users of this
   machine also have the `android-skills` plugin.

### 4.4 `AGENTS.md` wording (owner WP12; the single source of the per-skill differences)

**Android template** (`templates/android/AGENTS.md`, about 30 lines inside the 400-line budget; auth rows inside
`<!-- <opt:auth> -->` markers):

```markdown
## Google's Android skills

`.agents/skills/` holds Google's official Android skills (Apache-2.0, unmodified; `.agents/skills/README.md` says
which version). Before work in one of these areas, read that skill's `SKILL.md` and follow it. Where it disagrees
with this file, this file wins; the known differences are in the last column.

| Skill | Read it before you… | This project differs |
|---|---|---|
| `navigation-3` | add a destination, scene, deep link or result | register in `navigation/<Feature>Navigation.kt` with `navGraph { entry<K>(pane = …) }`; never call `NavDisplay`, a scene strategy or transition metadata from a feature |
| `navigation-event` | handle Back anywhere outside a screen pop | `NavigationBackHandler` only, one state per handler, overlays at overlay priority; never `BackHandler` (the build fails) |
| `adaptive` | build or change any screen | no `NavigationSuiteScaffold` or Material components: the shell's bar, rail and sidebar; panes through `NavPane`; `LazyVerticalGrid`, not `Grid`/`FlexBox` (experimental in Compose 1.12); `AppTheme.containerClass`, `posture` and `input`, not `MediaQuery`; Roborazzi, not `com.android.compose.screenshot`; `AppLargeTitleTopBar`, not M3 scroll behaviours |
| `edge-to-edge` | touch insets, the keyboard or system bars | pad scrolling content with `chromeContentPadding()` as `contentPadding`, never on the list's parent |
| `testing-setup` | add tests or test infrastructure | fakes in `testFixtures`; screenshots with Roborazzi at the sizes in `docs/ADAPTIVE.md`; never re-record goldens to make a test pass |
| `android-profiler` | investigate jank, startup or memory | release builds only, through `tools/perf/perf.sh`; its `trace_processor` script downloads Google's binary on first run |
| `r8-analyzer` | change keep rules or chase app size | upstream ships no `scripts/`: run `./gradlew :app:analyzeReleaseR8Config`, then follow its manual path |
| `android-intent-security`, `android-permissions-security` | touch the manifest, deep links, notifications or permissions | links land in `LinkActivity` and become typed keys in a `DeepLinkRoute` |
| `agp-9-upgrade` | bump AGP or Gradle | the project is already on AGP 9 with built-in Kotlin |
| `restore-credentials`, `verified-email` | change sign-in or sign-up | flows live in `:feature:auth`, credentials in `:data:auth` |

Not vendored on purpose: `styles` (experimental; the design system stays on stable foundation APIs) and
`android-cli` (install it yourself if you want it).
```

**Multiplatform template:**
- the same section without the `agp-9-upgrade` row;
- `navigation-3`'s row adds "this template uses JetBrains Navigation 3 1.1.2, which lacks 1.2's mid-flight predictive
  back";
- `edge-to-edge`, `r8-analyzer` and the security rows say "(androidApp)".

**SwiftUI template:** no skills section. One line: "No Google skills apply here; for platform behaviour read Apple's
documentation for SwiftUI, `NavigationSplitView` and size classes."

**Other agents' entry files (A11.2, every template):**
- `CLAUDE.md` = `@AGENTS.md` (APPLIED on int).
- `GEMINI.md` = `@AGENTS.md`.
- `.github/copilot-instructions.md` and `.cursor/rules/project.mdc`: "Read `AGENTS.md` first and follow it. Google's
  Android skills are in `.agents/skills/`; read the one that matches your task before you start (the table in
  `AGENTS.md` says when)."

**Generator repository `AGENTS.md`** (new section):

```markdown
## Google's Android skills

The Android and Compose Multiplatform templates ship Google's Android skills in `templates/*/.agents/skills/`
(unmodified, Apache-2.0, `README.md` there gives the version). When you change those templates, read the skill for the
area first (`.agents/skills/<name>/SKILL.md` and `.claude/skills/<name>/SKILL.md` here point at the Android copy) and
the differences table in `templates/android/AGENTS.md`. Never edit a vendored file: refresh all of them with
`python3 tools/update_android_skills.py <checkout>` and review the diff. A skill added to a template needs its row in
that template's AGENTS.md table; `generator/tests/test_vendored_skills.py` fails otherwise.
```

### 4.5 What `AGENTS.md` must not say
- Never say "Grid and FlexBox are stable" (§0 #1).
- Never point an agent at a skill that the preset did not ship. The rows are inside the same feature markers as the
  files.

### 4.6 Checks (owner WP02-M2; `generator/tests/test_vendored_skills.py`, part of A12.1)
- Every vendored folder has `SKILL.md` and `LICENSE.txt`, and its files match `MANIFEST.sha256`, so they are unmodified.
- Every vendored skill has a `.claude/skills/<name>/SKILL.md` pointer whose `name`/`description` equal upstream's
  front matter.
- For lean, standard and everything (auth on and off): the AGENTS.md table rows equal the set of vendored folders, in
  both directions.
- No excluded skill (`styles`, `android-cli`, …) is present.
- Multiplatform has no `agp-9-upgrade`, and the shared skills are byte-identical with Android (also `sync_check`).
- Generated output keeps `BaseApplication` intact in `hilt-testing.md` (the `verbatim` regression).

---

## 5. Owners

| Package | Items |
|---|---|
| WP02-M2 | ST-02 `scaffold.py`/`add_feature.py` emit the §2.1 shape with M1 APIs; ST-09 build-logic packages; ST-10 the §3.1 rules (fix `material`, add the new ids, fixtures, ≤ 10 markers); ST-11 lint ids and `resourcePrefix`; ST-12 `verifyManifestRules`, screenshot plugin, formatting step 9; ST-20…24 vendored skills (template copies, `verbatim` engine key, `auth.toml` owns, `update_android_skills.py`, repo pointers, `test_vendored_skills.py`) |
| WP02-M3 | ST-01 the §2.1 feature moves on every feature; ST-07 the `:core:ui` moves and `runSuspendCatching` (WP02 owns `core/{common,ui}`); `feature-public` clean; screens pass `slop-comment`, `layer-package`, `adaptive-*` |
| WP03 | ST-05 the §2.4 moves and splits; migrate the four `BackHandler`/`PredictiveBackHandler` sites to `NavigationBackHandler` (`legacy-back`); the §3.4 component-API work (`XDefaults`, `XColors` with a `colors` parameter, no `fillWidth`, no `null` defaults, constant casing); `preview/` package; `sections/CatalogAdaptive.kt` |
| WP04 | ST-06 the §2.5 moves (public root package, internal sub-packages, `AppShell.kt` split, `AppTabBar.kt` under 500 lines); `Chrome.kt` into `foundation/layout/`; ST-08 `app/startup/` and `app/log/` (with WP02 for `BaseAppApplication.kt`); `benchmark/<journey>/` |
| WP06 | ST-03 the §2.2 data moves (`api/model/`, `remote/dto/`, mappers, fakes in `testFixtures`, names per A-5); ST-04 the §2.3 core moves; `runCatching` and `Dispatchers.Default` fixes in `:core:network` |
| WP05 | `kyant` rule; glass files under `foundation/glass/` follow §2.4 conventions |
| WP07 | §2.11 package mirroring and `sync.toml` paths; vendored skills in `templates/multiplatform/.agents/skills/` (copied by WP02-M2's tool) and the multiplatform AGENTS.md variant's data |
| WP08 | §2.12 folder rules with `test_target_ios.py` checks; no vendored skills |
| WP12 | §4.4 wording in every `AGENTS.md`, `GEMINI.md`, copilot and cursor pointers; `docs/ARCHITECTURE.md` gains the §2 trees and the screen-package naming rule; the code-standards summary (CODE-STANDARDS with the §0 corrections) in each generated project's docs |

---

## 6. Sources

- `research/standards.md`, `research/CODE-STANDARDS.md`, `research/adaptive.md`, `research/ANDROID-SKILLS-DIGEST.md`,
  MASTER-PLAN A2.2–A2.5, A11.2, A11.3 and B.3.
- Compose foundation-layout 1.12.0/1.12.1/1.13.0-alpha03 bytecode (Gradle cache): `ExperimentalGridApi`,
  `ExperimentalFlexBoxApi`, `GridKt`, `FlexBoxKt`.
- androidx-main `compose/material3/adaptive/adaptive-navigation3` (`ThreePaneScaffoldScene.kt`, `api/current.txt`):
  https://github.com/androidx/androidx/tree/androidx-main/compose/material3/adaptive/adaptive-navigation3
- Android skills checkout `~/.claude/plugins/marketplaces/android-skills` (tag v1.0.13, commit `42dc2270e9…`, sizes
  by `du`, `hilt-testing.md:373`, `profilers/android-profiler/references/perfetto/bin/trace_processor`).
- Generator: `generator/genkit/core/rename.py:13,63-64` (prefix boundary), `manifest.py` (`[files]` keys),
  `targets/android/scaffold.py:130-392`, `tabs.py`, `add_feature.py:184`, `remove_feature.py:109`.
- Code: WP02 `CodeRules.kt` (`material` rule); WP03 `AppActionSheet.kt:82`, `AppLoadingOverlay.kt:50`,
  `AppAlert.kt:76`, `AppSheet.kt:278`, `component/data/AppDataDisplay.kt`, `foundation/Pressable.kt`,
  `foundation/Modifiers.kt`, `icon/AppIcons.kt`; WP04 `core/navigation/*` (22 files); int `feature/**`, `data/**`,
  `core/**`, `app/**` (file lists and line counts).
- Agent skill folders: https://developers.openai.com/codex/skills , https://geminicli.com/docs/cli/skills/ ,
  https://developer.android.com/studio/gemini/skills , https://developer.android.com/tools/agents/android-skills
