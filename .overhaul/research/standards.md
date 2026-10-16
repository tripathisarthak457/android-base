# Google/AOSP code-quality bar and project organisation: research and proposal

Date: 2026-10-09. Scope: requirements 6, 7, 15, 26 and 27. Inputs read: `REQUIREMENTS.md` (1–31),
`MASTER-PLAN.md` A2.2–A2.5, A4.7, A6, A9, A11 and Part D, `notes/INDEX.md`, `research/ANDROID-SKILLS-DIGEST.md`,
the WP02-M1 handoff, the template on `overhaul/multiplatform-v2` (`709612d`) and the in-flight WP02, WP03 and WP04
worktrees, every relevant Android skill (v1.0.13, commit `42dc227`), and the primary sources in §6.
No repository or worktree was modified.

Deliverables:

- `research/CODE-STANDARDS.md`: the rules reviewers and implementers apply, with ids (C-, API-, M-, K-, A-, CO-,
  N-, L-, T-, B-, F-) and do/don't code.
- This file: what to do first (§1), the decisions where Google's guides and our plan disagree (§2), the package and
  folder layout per module type (§3), how generated projects vendor the Android skills (§4), the androidx sources to
  copy from (§5), and sources (§6).

---

## 1. Ranked by importance for this project

Ranked by what blocks the requirements soonest and what reviewers will judge every package against. Each item names
its owner and the evidence in today's tree.

### Tier 0: adopt now; every review from here on uses them

1. **Comment and KDoc policy, enforced by the build** (CODE-STANDARDS §1; requirements 7, 27). Reviewers need one
   written bar for "slop". The template's comments are already mostly why-comments (a grep for narrating openers in
   the WP03/WP04 trees finds almost none), so the work is to keep it that way: add `slop-comment` and
   `commented-code` to `verifyCodeRules` (§1.1 below) and remove the remaining banners (`theme/Palette.kt` and
   `icon/AppIcons.kt` carry `// ── … ──` dividers; Part D 5 already bans them). Owner: WP02-M2 (rules), each package
   for its own files.
2. **Compose component API shape** (CODE-STANDARDS §2). The design system is a library to every feature and every
   generated app, so it is held to androidx's library bar. Concrete gaps in the WP03 tree: four `XDefaults` objects
   (`AppButtonDefaults`, `AppCardDefaults`, `AppBottomBarDefaults`, `AppNavigationRailDefaults`) for about a hundred
   public top-level functions, while A4.7 requires one per component; no component takes a `colors: XColors`
   parameter (`AppButtonDefaults.Colors` exists but callers cannot pass it), so changing one button's colour means
   forking it (API-6/7); `AppButton(fillWidth = …)` is a modifier passed as a parameter (API-4);
   `AppTheme(colors: AppColors? = null)` uses `null` to mean "the default" (API-6). Public KDoc is nearly complete
   (4 of 107 public top-level functions lack it). Owner: WP03 (design system), WP04 (tab bar, navigation chrome).
3. **Compose, Navigation 3 and Lifecycle lint checks as errors** (M-5). Free enforcement of half of §2: the ids are
   in the lint jars already on the classpath. Add `ResourceName` with `resourcePrefix` (F-5); the template's strings
   already follow `<module>_` prefixes except `:core:ui`'s `common_error_*` (give it `resourcePrefix = "common_"` or
   rename). Owner: WP02-M2 (convention plugins).
4. **Coroutine and error-handling correctness** (A-8, CO-1). On the integration branch, `runCatching` wraps suspend
   calls in `SessionCoordinator.signOut()` (`store.clear()`) and `ResponseCache.clear()` (`dao.clear()`), which
   swallows `CancellationException`; `NetworkClient.kt` hard-codes `Dispatchers.Default` for decoding instead of
   taking it from `AppDispatchers`. There are 36 `runCatching` calls in main sources; most wrap non-suspending code
   and are fine, which is why the proposed rule (§1.1) takes an allow marker. Owner: WP06 (network), WP02-M2
   (session, rule).
5. **Vendor Google's Android skills into every generated project** (§4; requirement 26). Decide the layout now
   (`.agents/skills/` canonical, `.claude/skills/` pointers) so WP02-M2 can emit it and WP12 can write `AGENTS.md`
   against it. Owner: WP01 or WP14 (generator copy step), WP02-M2/WP07 (template files), WP12 (docs).

### Tier 1: structure; land with the owning milestone

6. **Screen-first packages in every feature** (§3.1). Today only `feature/sample` has them. `feature/auth` and
   `feature/settings` put state, events, effects, ViewModel, route, screen and preview in one file per screen
   (`SignIn.kt` 315 lines, `Settings.kt` 463 lines); `feed`, `profile` and `search` are flat. Navigation registration
   has three names for one thing: `navigation/SampleNavigation.kt`, `di/FeedNavModule.kt`, `AuthNav.kt`. Unify on
   `navigation/<Feature>Navigation.kt`. Owner: WP02-M3 (features), the scaffolder in WP14 (`add_tab.py`, new-feature
   command).
7. **Sub-packages in `:core:navigation` and `:core:designsystem`** (§3.4, §3.5). WP04's navigation module has 22 flat
   files; WP03's `foundation/` has 11 files of four different kinds, and the optional component groups live in
   packages whose names do not match their feature keys (`data/` holds `datadisplay`, `fields/` holds `formfields`).
   Matching names let the generator remove a group as one directory. Owner: WP04, WP03.
8. **Naming consistency** (A-4, A-5, K-4). Repository surfaces mix `val profile: Flow<…>`, `fun posts(): Flow<…>` and
   `suspend fun items(…)`; constants mix `UPPER_SNAKE` and PascalCase in one module (`Palette.kt`, `AppFonts.kt`).
   Owner: WP02-M3 (data), WP03.
9. **One formatter and 100 columns** (K-1). `.editorconfig` says 120 and nothing enforces it: in the WP03 tree 329
   lines exceed 120 and 955 exceed 100. Make the template pass ktlint `android_studio` style (the `formatting`
   feature's Spotless setup) with one mechanical commit after the last UI wave, and run the check on the template in
   CI even when the feature is off, so turning it on in a generated app produces no diff. Owner: WP13.
10. **`AppNavigator` scope** (§2, item 11). WP04's `AppNavigator` is `@Singleton`; A3.1 says it is scoped per
    Activity, and the Navigation 3 Hilt recipe installs the navigator and entry installers in
    `ActivityRetainedComponent`. With desktop windowing or "open in new window" (requirement 23), two Activity
    instances would share one navigator. Owner: WP04.

### Tier 2: worth doing, after the waves settle

11. `explicitApi()` in `:core:designsystem`, `:core:navigation`, `:core:mvi`, `:core:common` (K-5). androidx's newer
    libraries (material3, navigation3) write `public` explicitly; it makes every widening of a module's surface a
    visible diff. Mechanical, but noisy in diffs, so after WP03/WP04 merge.
12. The `messages` channel in `MviViewModel` (A-3): keep with a written argument, or move to Google's
    "message in state + `onMessageShown(id)`" pattern. Owner: WP02-M2.
13. Fakes in `testFixtures` of the module that owns the interface (T-2, §3.2). AGP 9 gives test fixtures full IDE
    support; `ModuleGraph` already checks the `testFixtures*` configurations.
14. Domain naming when a project goes modular (A-7): `VerbNounUseCase` with `operator fun invoke`, created only when
    logic spans two repositories or two ViewModels.

### 1.1 New build-enforced rules (proposed additions to `CodeRules.ALL`, WP02-M2)

WP02's in-progress `CodeRules.kt` has `ripple`, `material`, `composed`, `nav-fade`, `collect-as-state`,
`dispatchers-io`, `raw-tween`, `raw-color`, `raw-dp`, `subcompose-image`, `bare-clickable`, `lazy-key`, `blocking`
and `hardcoded-copy`. Proposed additions, each with a fixture test and the existing `// allow(id): reason` escape:

| Id | Fails on | Scope |
|---|---|---|
| `slop-comment` | a `//` comment starting with `Added`, `Updated`, `Fixed`, `New:`, `Step <n>`, `As requested`, `IMPORTANT`, `NOTE:`, `CRITICAL`, `PERF:`, `HACK`, `FIXME`, `TODO`; a comment that is only box-drawing, `=`, `-` or `*` runs | all Kotlin main and test sources |
| `commented-code` | a `//` line whose text parses as a statement start (`val `, `var `, `fun `, `import `, `if (`, `return `) or ends in `)`, `}` or `;` | all Kotlin sources |
| `run-catching` | `runCatching {` | main sources of `core/**`, `data/**`, `app/**` (use a helper that rethrows `CancellationException`) |
| `bang-bang` | `!!` | main sources |
| `styles-api` | `androidx.compose.foundation.style` imports, `ExperimentalFoundationStyleApi` | everywhere (§4.4) |
| `legacy-back` | `BackHandler(`, `PredictiveBackHandler(`, `OnBackPressedCallback` | `feature/**`, `core/ui`, `core/designsystem` |
| `layer-package` | a package segment `ui`, `viewmodel`, `presentation`, `screens`, `util`, `utils`, `helper`, `helpers`, `misc`, `base` | `feature/**`, `data/**`, `core/designsystem`, `core/navigation` |
| `file-length` | a Kotlin file over 500 lines | main sources (icon sets take an allow marker) |
| `spaced-test-name` | a backticked function name containing a space | `src/androidTest`, `benchmark/**`, multiplatform `commonTest` (K-8) |

Lint (convention plugin): the M-5 ids plus `ResourceName` as errors, `abortOnError = true`, `checkDependencies = true`
in `:app`; no `lint-baseline.xml` in the template.

### 1.2 Reviewer checklist (paste into the A12.8 review prompt)

1. Every comment passes C-1/C-2; no KDoc restates a signature (C-5).
2. Public composables: order, single `modifier` applied first to the root, no modifier-shaped parameters, slots for
   styleable content, `XDefaults`/`XColors`, no `State<T>`/`MutableState<T>` parameters (API-2 to API-8).
3. Animated or scroll-driven values read only in layout or draw (M-2); custom modifiers are `Modifier.Node` (M-1).
4. No new public declaration where `internal` or `private` works (K-5); `api/` packages pure (F-4).
5. Packages named for screens or concepts, not layers (F-2, F-3); files under 500 lines (K-2).
6. Coroutines: injected dispatchers, main-safe suspend functions, no swallowed cancellation (CO-1 to CO-6, A-8).
7. Tests: fakes not mocks, behaviour per test, no sleeps (T-1 to T-6).
8. The relevant Android skill was followed, or the conflict table in §4.4 says why not.
9. `./gradlew check` (lint, `verifyCodeRules`, module guards) green.

---

## 2. Where Google's guidance and our plan disagree, and the decision

| # | Google says | Template or plan does | Decision |
|---|---|---|---|
| 1 | ViewModel events always become UI state; channels "don't guarantee delivery" (UI events page) | `MviViewModel.effects` and `messages` are buffered channels | Keep `effects` for navigation and system actions only: `MviScreen` collects under `repeatOnLifecycle(STARTED)` on `Main.immediate`, which closes the receive-then-drop window. Document it in `docs/ARCHITECTURE.md`. Revisit `messages` (Tier 2). |
| 2 | androidx KDoc: document every parameter | Part D: comments only for why | KDoc mandatory on public API of core and `api/` packages; `@param` only when the name and type do not say it (C-5). |
| 3 | Android style: constants `UPPER_SNAKE` | Compose guidelines and androidx Compose sources: PascalCase | PascalCase in Compose modules, `UPPER_SNAKE` elsewhere, never mixed in a file (K-4). |
| 4 | Android style: no spaces in function names | 34 JVM test files use backtick sentences | Allowed in JVM tests only; D8 rejects spaces below DEX 040 (minSdk 26), so never in `androidTest`, `benchmark` or `commonTest` (K-8). |
| 5 | Android style: 100 columns | `.editorconfig` 120, not enforced | 100, one mechanical reformat (Tier 1, item 9). |
| 6 | Architecture (optional): `getXStream()` | Mixed | Noun for streams (property without arguments, function with), verb for one-shots (A-4). |
| 7 | Domain layer: `VerbNounUseCase`, `operator fun invoke` | "No UseCase ceremony" (A2.3) | No pass-through use cases; when one is justified, Google's naming (A-7). |
| 8 | Now in Android: impl packages end in `.impl`, keys in `api.navigation` | Identical packages in compact and modular | Keep ours: the compact-to-modular split must leave sources byte-identical (A2.1, A2.4). |
| 9 | adaptive skill: `NavigationSuiteScaffold`, M3 top app bar scroll behaviours | Requirement 31: no material3 components | `AppTabBar(orientation)` and `AppLargeTitleTopBar` fill those roles; `material3.adaptive` scene strategies are allowed. |
| 10 | styles skill: migrate components to `Style` | Stable APIs only | Do not adopt (§4.4). |
| 11 | Navigation 3 Hilt recipe: navigator and entry installers in `ActivityRetainedComponent` | `AppNavigator` `@Singleton`, graphs in `SingletonComponent` | Move to activity-retained scope, as A3.1 already says (Tier 1, item 10). |
| 12 | adaptive skill: `Grid`/`FlexBox` experimental | — | Stale for our versions: both were promoted to stable in Compose foundation 1.12.0 (release notes). `MediaQuery` is still experimental (its posture types change in 1.13). |

---

## 3. Package and folder structure for million-user apps

Principles (all from the modularization guide, Now in Android and our A2):

- **Modules split by feature and data domain; packages inside a module split by screen or concept.** Layer packages
  (`ui/`, `viewmodel/`, `presentation/`) inside a module that is already one feature scatter one change across
  folders. NiA does the same at module level; its feature impl modules are flat (screen, ViewModel, UI state and a
  `navigation/` package), which is fine for its size. We go one level deeper because generated apps grow features
  with many screens.
- **The public surface is one package per module** (`api/`), and everything else is `internal` (modularization guide:
  "expose as little as possible").
- **Depth is earned.** The scaffolder always creates the screen package (a one-screen feature costs one folder and keeps
  every feature the same shape for agents to copy). `component/`, `model/`, flow sub-packages and `di/` appear only
  when the thresholds below are met.
- **Packages are identical in compact and modular mode** (decision 8 above).

Thresholds that keep small apps small:

| Signal | Then |
|---|---|
| A composable is used by two or more screens of one feature | it moves to the feature's `component/` |
| A UI model or formatter is used by two or more screens | it moves to `model/` |
| A screen package passes about 8 files, or a file 400 lines | split the screen by concept (`list/filter/`), or the file |
| A user journey has steps that share state (checkout, onboarding) | one flow package with a sub-package per step |
| A feature passes about 12 screens, or two teams own parts of it | split it into two feature modules, not deeper packages |
| More than about 10 features, more than one team, or `:app` compile time hurts | switch to `modular` (`make_modular.py`) |
| Logic spans two data domains or is reused by two ViewModels | a `:domain:<area>` module (modular only) |

### 3.1 Feature module (compact; `modular` moves `api/` to `:feature:orders:api`, the rest to `:impl`)

```
feature/orders/
├── build.gradle.kts                    plugins + dependencies only (B-2)
└── src/
    ├── main/
    │   ├── kotlin/<pkg>/feature/orders/
    │   │   ├── api/                    PUBLIC, pure Kotlin
    │   │   │   ├── OrdersKeys.kt       @Serializable OrderListKey, OrderDetailKey(orderId: String) : AppNavKey
    │   │   │   └── OrdersResults.kt    result contracts other features read
    │   │   ├── navigation/
    │   │   │   └── OrdersNavigation.kt @Module: navGraph { entry<…> }, navKeys { }, DeepLinkRoute, all @IntoSet
    │   │   ├── list/                   one package per screen
    │   │   │   ├── OrderListRoute.kt       stateful: hiltViewModel(), effects, AppNavigator
    │   │   │   ├── OrderListScreen.kt      stateless: state, onEvent, modifier; private @Preview at the bottom
    │   │   │   ├── OrderListViewModel.kt
    │   │   │   ├── OrderListContract.kt    OrderListState, OrderListEvent, OrderListEffect
    │   │   │   └── OrderRow.kt             screen-private piece, only once it earns its own file
    │   │   ├── detail/                 same four files
    │   │   ├── checkout/               a flow: shared step state + one sub-package per step
    │   │   │   ├── CheckoutFlowState.kt
    │   │   │   ├── address/  payment/  review/
    │   │   ├── component/              composables shared by two or more screens (internal)
    │   │   ├── model/                  UI models and formatters shared by two or more screens
    │   │   └── di/                     only when the feature binds something besides navigation
    │   └── res/values/strings.xml      orders_* (F-5); values-<lang>/ beside it
    ├── test/kotlin/<pkg>/feature/orders/list/OrderListViewModelTest.kt
    └── screenshotTest/kotlin/<pkg>/feature/orders/list/OrderListScreenshots.kt   (T-5)
```

Modular mode, packages unchanged:

```
feature/orders/api/   <pkg>.jvm.library      src/main/kotlin/<pkg>/feature/orders/api/…
feature/orders/impl/  <pkg>.android.feature  src/main/kotlin/<pkg>/feature/orders/{navigation,list,detail,…}
```

A key another feature opens lives in `core/navigation/src/main/kotlin/<pkg>/feature/<owner>/api/` in compact mode and
moves to `:feature:<owner>:api` in modular mode (A2.4, unchanged).

### 3.2 Data module

```
data/orders/src/
├── main/kotlin/<pkg>/data/orders/
│   ├── api/                                PUBLIC, pure Kotlin
│   │   ├── OrdersRepository.kt             interface: streams as nouns, one-shots as verbs (A-4)
│   │   └── model/  Order.kt  OrderId.kt (value class)  OrderStatus.kt
│   ├── OfflineFirstOrdersRepository.kt     internal; DefaultOrdersRepository when `database` is off (A-5)
│   ├── remote/
│   │   ├── OrdersApi.kt                    internal; calls go through NetworkClient
│   │   ├── dto/  OrderDto.kt  OrderPageDto.kt
│   │   └── OrderDtoMappers.kt              OrderDto.toDomain(), OrderDto.toEntity()
│   ├── local/
│   │   ├── OrderEntityMappers.kt           entities and DAOs live in :core:database (package …core.database.orders)
│   │   └── OrdersPreferences.kt            only for DataStore-backed state of this domain
│   ├── sync/                               (offlinequeue) queued writes of this domain and their replay
│   └── di/OrdersDataModule.kt              @Binds internal implementation to the api interface
├── testFixtures/kotlin/<pkg>/data/orders/api/FakeOrdersRepository.kt      (modular: in :data:orders:api)
└── test/kotlin/<pkg>/data/orders/
    ├── OfflineFirstOrdersRepositoryTest.kt
    └── remote/OrderDtoMappersTest.kt
```

`:domain:<area>` (modular only): `<pkg>/domain/pricing/GetOrderSummaryUseCase.kt`, pure Kotlin, one class per file,
tests beside it.

### 3.3 Core data infrastructure

```
core/database/…/core/database/   AppDatabase.kt, migration/ (Migration1To2.kt …), converter/,
                                 <domain>/ (orders/OrderEntity.kt, orders/OrderDao.kt), queue/ (offlinequeue), di/
core/network/…/core/network/     client/ (NetworkClient, KtorNetworkClient, NetworkConfig, RetryPolicy, InFlightRequests),
                                 response/ (ResponseUnwrapper, ResponseCache), auth/, monitor/ (ConnectivityNetworkMonitor),
                                 recording/ (NetworkObserver seam for devtools), websocket/, model/, di/
core/datastore/…/core/datastore/ settings/ (AppSettingsStore), session/ (AuthTokenStore), crypto/, di/
core/common/…/core/common/       unchanged: result/, coroutines/, log/, navigation/, network/, session/, time/, validation/
```

`core/network` has ten files at its root today beside four sub-packages; the split above groups them by the question a
reader comes with.

### 3.4 `:core:designsystem`

```
core/designsystem/src/main/kotlin/<pkg>/core/designsystem/
├── theme/                       AppTheme (the composable and the accessor object), AppColors, AppTypography,
│   │                            AppMetrics (spacing, radii, sizes), AppMotion (AppSprings), AppDesignStyle,
│   │                            StyleTones, AppFonts, ReduceMotion
│   └── tokens/DesignTokens.kt   GENERATED, internal (A9.2; never edited)
├── foundation/                  behaviour and drawing with no visual opinion of its own
│   ├── interaction/             Pressable.kt (pressable, PressFeedback), NoIndication, RowHighlight,
│   │                            MinimumTouchTarget.kt, AppHaptics.kt
│   ├── scroll/                  AppOverscroll.kt, ScrollEdgeFade.kt, nested-scroll pieces shared by bars and sheets
│   ├── layout/                  Chrome.kt (chrome insets), Bleed.kt, Hairline.kt, GroupItemShape.kt
│   ├── adaptive/                AppWindowSize.kt, AdaptiveLayout.kt, posture
│   └── content/                 ContentColor.kt, CurrentLocale.kt, AppSurface.kt
├── animation/                   AppTransitions.kt, Emphasis.kt, stagger
├── icon/                        AppIcons.kt (the accessor object) + glyph/<Family>.kt (path builders, split by family)
└── component/                   one package per family; one public component per file (K-2)
    ├── text/ button/ input/ selection/ list/ container/ navigation/ overlay/ feedback/ refresh/   (core)
    └── pickers/ formfields/ datadisplay/ collections/    (optional groups: package name = feature key)
```

Each component file: the public composable(s), `XDefaults`, `XColors`, `XState` + `rememberXState`, internal
pieces, private pieces, private constants last. Component previews and screenshot tests live in
`src/screenshotTest/kotlin/<pkg>/core/designsystem/component/<family>/`, so main sources stay lean. The catalog app
mirrors the families (`catalog/…/catalog/<family>/`), which also splits today's 658-line `CatalogComponents.kt`.

Moves from the WP03 tree: `component/data` → `component/datadisplay`, `component/fields` → `component/formfields`,
`foundation/*` into the five sub-packages, `AppWindowSize` from `theme/` to `foundation/adaptive/`, `AppIcons.kt`
(709 lines) split by glyph family. The generator's `features/{pickers,formfields,datadisplay,collections}.toml`
then remove one directory each.

### 3.5 `:core:navigation`

```
core/navigation/src/main/kotlin/<pkg>/core/navigation/
├── AppNavigator.kt              PUBLIC: the only API a feature calls
├── NavGraph.kt                  PUBLIC: navGraph { entry<K>(transition, pane) }, navKeys { }, NavPane, NavTransitionStyle
├── DeepLinkRoute.kt             PUBLIC: the per-feature contribution
├── shell/                       AppShell.kt (the one host call site), ShellState.kt, ShellChrome.kt, BarAnchor.kt
│   └── tabs/                    KeptTabs.kt, TabLifecycle.kt, per-tab NavigationEventDispatcherOwner
├── stack/                       AppBackStack.kt, NavKeySerialization.kt, StackHost.kt, TabbedNavHost.kt
├── back/                        BackGate.kt, NavGuard.kt, predictive-back card motion
├── transition/                  NavTransitions.kt, SceneMotion.kt, CoverScrim.kt, KeepUnderScene.kt
├── scene/                       ListDetailScene.kt, SheetScene.kt, supporting pane
├── presentation/                PresentationHost.kt, Presentation.kt (layer 3 of A3.9)
├── deeplink/                    DeepLinks.kt (PendingDeepLink: hold once, take once)
├── registry/                    NavRegistry.kt, DestinationCatalog, the registration check
└── di/NavigationModule.kt
core/navigation/src/main/kotlin/<pkg>/feature/<owner>/api/   shared keys (A2.4)
```

Everything outside the three public files and `shell/AppShell` is `internal`.

### 3.6 `:core:ui`

```
core/ui/…/core/ui/   screen/ (MviScreen, MessageHost), error/ (AppError → text), text/ (UiText), image/ (AppNetworkImage),
                     paging/, form/, browser/, locale/
```

### 3.7 `:app`

After A2.2's moves (push, lock and Play Store prompts to their core modules, the widget deleted), `:app` holds only
composition:

```
app/src/main/kotlin/<pkg>/
├── BaseAppApplication.kt  MainActivity.kt
├── ui/AppRoot.kt                 theme + shell
├── navigation/AppDestinations.kt tabs and start keys (today in ui/)
├── startup/                      StartupViewModel (splash hold, font preload)
├── session/                      SessionCoordinator, SessionModule
├── log/AndroidLogger.kt          (today a private class inside BaseAppApplication.kt)
└── di/                           AppModule, CoroutinesModule, ClockModule
```

`deeplink/DeepLinkResolver.kt` leaves `:app` once routes are multibound per feature (A2.4).

### 3.8 Build logic, benchmark, tests

- `build-logic/convention/…/buildlogic/`: one file per convention plugin (`AndroidFeatureConventionPlugin.kt`, as NiA
  names them) plus `guard/` (ModuleGraph, CodeRules, ApiPurity) and `config/` (AppConfig, Flavors, Signing).
- `benchmark/…/benchmark/<journey>/` (tabs, pushpop, rapidback, scroll, startup) with shared selectors in
  `benchmark/…/benchmark/Selectors.kt` (A3.8).
- Every module mirrors main packages in `test/`, `screenshotTest/`, `testFixtures/` and (rarely) `androidTest/`.

---

## 4. Google's Android skills in generated projects (requirement 26)

### 4.1 What each template vendors

| Skill (upstream path) | Android | Multiplatform | When agents use it | Notes |
|---|---|---|---|---|
| `navigation-3` | yes | yes | new destinations, scenes, deep links, results, multiple back stacks | the JetBrains Navigation 3 fork keeps the same API shape |
| `navigation-event` | yes | yes | any Back handling outside `NavDisplay`: sheets, menus, search, kept tabs | |
| `adaptive` | yes | yes | any screen or layout work; previews per form factor | conflicts in §4.4 |
| `edge-to-edge` | yes | yes (androidApp) | insets, IME, system bars, content under the floating bar | |
| `testing-setup` | yes | yes | adding tests or test infrastructure | screenshot plugin choice in T-5 |
| `android-profiler` | yes | yes | jank, startup or memory investigations; traces for perf escalations | release builds only |
| `r8-analyzer` | yes | yes (androidApp) | keep rules and app size | its Path A calls `scripts/` that upstream does not ship (§4.4) |
| `android-intent-security` | yes | yes | manifest components, deep links, notification `PendingIntent`s | |
| `android-permissions-security` | yes | yes | runtime permissions, providers, receivers | |
| `agp-9-upgrade` | yes | no | AGP bumps and moving a v1 app | the skill excludes KMP |
| `restore-credentials` | when `auth` | when `auth` | Credential Manager restore keys | |
| `verified-email` | when `auth` | when `auth` | OTP-less verified email sign-up | |
| `camerax` | only if a feature adopts CameraX | — | | `media` today uses the system capture intent |
| `styles` | **no** | **no** | | experimental API (§4.4) |
| `android-cli` | **no** | **no** | | its first step pipes a downloaded install script into a shell; people install the CLI themselves |
| `play-policy-insights` | no (opt-in later) | no | | 1.4 MB with Python scrapers of the Play Store site |
| others (wear, tv, xr, cast, engage, billing, appfunctions, ML Kit, XML migration) | no | no | | no template feature uses them |

The core set is about 1.5 MB uncompressed (navigation-3's recipes are 404 KB of it), mostly Markdown that compresses
well in the download zip. The SwiftUI template vendors none.

### 4.2 Layout in a generated project

Agents look in different places: Codex scans `.agents/skills/`; Gemini CLI reads `.gemini/skills/` or the
`.agents/skills/` alias; Android Studio (Quail 1 and later) reads `.agents/skills/` or `.android-studio/skills/`;
Claude Code reads `.claude/skills/`. The r8-analyzer skill itself hard-codes `.agents/skills/r8-analyzer/`. So:

```
.agents/skills/
├── README.md                          provenance: upstream URL, version 1.0.13, commit 42dc227, "unmodified copies",
│                                      how to update; points at AGENTS.md for project overrides
├── navigation-3/                      verbatim upstream folder (SKILL.md + references/)
│   └── LICENSE.txt                    Apache-2.0 text, beside the SKILL.md whose front matter says
│                                      "license: Complete terms in LICENSE.txt"
├── navigation-event/ … (one folder per vendored skill, each with LICENSE.txt)
.claude/skills/
├── navigation-3/SKILL.md              pointer (below), one per vendored skill
└── …
```

Pointer file (generated; `name` and `description` copied from the upstream front matter so Claude Code triggers on
the same cues):

```markdown
---
name: navigation-3
description: <upstream description, verbatim>
---

Google's `navigation-3` skill is vendored at `.agents/skills/navigation-3/` (Android skills 1.0.13, Apache-2.0).
Read `.agents/skills/navigation-3/SKILL.md` now and resolve every relative path in it from that folder.
Where it disagrees with `AGENTS.md`, `AGENTS.md` wins.
```

Why pointers rather than a second copy or symlinks: one copy to update; symlinks do not survive the web download zip
on Windows. (Unverified: whether Claude Code also scans `.agents/skills/`. If it starts to, the pointers become
redundant and can be dropped; duplicate names are harmless.)

Licence: Apache-2.0 §4 is met by shipping the licence text with the files, keeping them unmodified (so no change
notices are needed), and stating the source; upstream has no `NOTICE` file to carry. The pointer files are ours. The
skills are development files and never reach the APK, so they do not belong in the in-app licences screen.

### 4.3 Generator side

```
third_party/android-skills/            verbatim upstream subset + LICENSE.txt + README.md (version, commit)
third_party/android-skills/skills.toml one entry per skill: upstream path, targets, optional feature expression
```

```toml
[skill.restore-credentials]
path = "identity/restore-credentials"
targets = ["android", "multiplatform"]
when = "auth"                      # same expression grammar as the marker engine
```

A pipeline step copies the selected folders into `.agents/skills/<name>/`, adds `LICENSE.txt`, writes the pointers and
`README.md`, and renders the AGENTS.md skills table from the same list, so the docs can never name a skill that was not
shipped (a consistency-matrix check). `tools/update_android_skills.py <commit>` refreshes `third_party/` from a
checkout and prints the diff for review. The generator repository's own `.agents/skills/` and `.claude/skills/` hold
pointers into `third_party/android-skills/`, and its `AGENTS.md` lists them for agents editing the templates.

Inside a generated project the refresh is `android skills add <name> --project=.` per skill (Android CLI). (Unverified:
which agent folders the CLI writes to; the project README should tell users to keep `.agents/skills/` as the one copy.)

### 4.4 What the skills say that the project overrides

| Skill | Says | Project rule |
|---|---|---|
| adaptive | wrap the app in `NavigationSuiteScaffold`; M3 top-app-bar scroll behaviours | requirement 31: `AppTabBar(orientation)` and `AppLargeTitleTopBar`; `material3.adaptive` scene strategies are allowed |
| adaptive | `Grid`/`FlexBox` are experimental | stable since Compose foundation 1.12.0; usable |
| adaptive | use `MediaQuery` for device queries | experimental in 1.12; window size class and posture through our `AppWindowSize` until we opt in |
| edge-to-edge | `Scaffold` `innerPadding` | our chrome insets (`chromeContentPadding()`); the `contentPadding`-not-parent-padding rule applies unchanged |
| navigation-3 | navigator in `ActivityRetainedComponent` | adopt (Tier 1, item 10) |
| navigation-3 | features call `NavDisplay`, set transition metadata | features register through `navGraph {}` and pick a `NavTransitionStyle` (N-2) |
| navigation-event | migrate `BackHandler` to `NavigationBackHandler` | adopt; `KeptTabs` provides per-tab child dispatcher owners |
| styles | move component styling to `Style` and `Modifier.styleable` | do not: `ExperimentalFoundationStyleApi`, and 1.12.0-alpha03 broke its source and binary compatibility. Keep component APIs shaped (`XDefaults`, `XColors`, no long styling parameter lists) so a later migration is mechanical. Rule `styles-api`. |
| testing-setup | install Jacoco | the template's `coverage` feature decides the tool |
| testing-setup | screenshot tests via AGP test suites | needs AGP 9.5.0-alpha03+; on 9.4.1 use the standalone plugin |
| r8-analyzer | on AGP ≥ 9.3 run `analyzeReleaseR8Config`, then `python3 .agents/skills/r8-analyzer/scripts/…` | upstream ships no `scripts/` folder (checked against GitHub); run the task, then follow Path C (manual review against `REDUNDANT-RULES.md` and `KEEP-RULES-IMPACT-HIERARCHY.md`) |
| agp-9-upgrade | "never write or run Python scripts" | applies to that skill's work only; the generator's own Python tools are unaffected |

Corrections for `research/ANDROID-SKILLS-DIGEST.md`: `Grid`/`FlexBox` are stable in 1.12; the r8-analyzer scripts do
not exist upstream; the canonical vendored location is `.agents/skills/` with `.claude/skills/` pointers, not
`.claude/skills/` alone.

### 4.5 What `AGENTS.md` and `CLAUDE.md` say

`CLAUDE.md` stays `@AGENTS.md` (A11.2); `GEMINI.md` is `@AGENTS.md` too (Gemini CLI supports `@` imports);
`.github/copilot-instructions.md` and `.cursor/rules/project.mdc` point at `AGENTS.md`. `AGENTS.md` gets one section,
rendered from `skills.toml` (about 25 lines, inside the 400-line budget):

```markdown
## Google's Android skills

`.agents/skills/` holds Google's official Android skills (Apache-2.0, unmodified; `.agents/skills/README.md` says
which version). Before work in one of these areas, read that skill's `SKILL.md` and follow it. Where it disagrees with
this file, this file wins; the known differences are listed under each row.

| Skill | Read it before you… | This project differs |
|---|---|---|
| `navigation-3` | add a destination, scene, deep link or result | register in `navigation/<Feature>Navigation.kt` with `navGraph {}`; never call `NavDisplay` or set transition metadata in a feature |
| `navigation-event` | handle Back anywhere outside a screen pop | one `NavigationBackHandler` per state; overlays use overlay priority; no `BackHandler` |
| `adaptive` | build or change any screen | no `NavigationSuiteScaffold` or Material components; `AppTabBar` and our scenes |
| `edge-to-edge` | touch insets, the keyboard or system bars | pad scrolling content with `chromeContentPadding()` as `contentPadding` |
| `testing-setup` | add tests | fakes in `testFixtures`; screenshot plugin as in `docs/TESTING.md` |
| `android-profiler` | investigate jank, startup or memory | release builds only, through `tools/perf/perf.sh` |
| `r8-analyzer` | change keep rules | its scripts are missing upstream: use its manual path |
| `android-intent-security`, `android-permissions-security` | touch the manifest, deep links, notifications or permissions | |
| `agp-9-upgrade` | bump AGP or Gradle | the project is already on AGP 9 |

Not vendored on purpose: `styles` (experimental API; the design system stays on stable foundation APIs).
```

---

## 5. androidx and AOSP implementations to read before writing the equivalent

| We build | Read | Copy | Do not copy |
|---|---|---|---|
| `AppPullToRefresh` (requirement 29) | `compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/pulltorefresh/PullToRefresh.kt` | `PullToRefreshState` interface + same-name factory + `rememberPullToRefreshState`; `PullToRefreshElement`/`PullToRefreshModifierNode` (`DelegatingNode`, `NestedScrollConnection`); `DragMultiplier = 0.5f`, 80 dp positional threshold, release in `onPreFling`; indicator moved in `placeWithLayer`'s layer block; `shadowElevation` set only while pulled or refreshing, which is material's own fix for the black ring | the disc, and content that does not move (we want iOS behaviour) |
| `AppSwitch`, segmented thumb | `compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Switch.kt` | `ThumbElement`/`ThumbNode : LayoutModifierNode` with `Animatable`s inside the node, driven by `interactionSource`, applied in placement; `@Immutable SwitchColors` with `thumbColor(enabled, checked)` | |
| Push, pop and predictive back | `navigation3/navigation3-ui/src/commonMain/kotlin/androidx/navigation3/ui/NavDisplay.kt` | progress read through a `State<Float>` over `NavigationEventState.transitionState`; `predictivePopTransitionSpec` receives the swipe edge; z-order handling comments | on `androidx-main`, metadata keys are `NavDisplay.TransitionKey` objects and there is a `sceneDecoratorStrategies` parameter: check what 1.2.0 ships before using either |
| Sheets with detents, swipe actions | foundation `gestures/AnchoredDraggable.kt`; material3 `ModalBottomSheet`/`SheetState` (unverified path) | anchors, velocity and positional thresholds, `animateTo` that keeps velocity | |
| Rubber-band overscroll | foundation `OverscrollEffect` and `LocalOverscrollFactory` | a factory provided once by the theme | |
| Press visuals | `IndicationNodeFactory` and material's `RippleNode` (unverified path) | one node per indication, created from the interaction source | the ripple itself |
| Back outside screens | `navigationevent-compose`'s `NavigationBackHandler` (unverified path) | one state per handler, priorities | |

How androidx writes comments, for calibration (`NavDisplay.kt`): "AnimatedContent does not change the zIndex of
content that is already on the screen, regardless of what you pass as the targetZIndex." One sentence, the constraint,
no narration.

---

## 6. Sources

Primary, read for this work:

- androidx Compose API guidelines: https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md
- androidx Compose component API guidelines: https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-component-api-guidelines.md
- androidx Kotlin guidelines: https://github.com/androidx/androidx/blob/androidx-main/docs/api_guidelines/kotlin.md
- androidx KDoc guidelines: https://github.com/androidx/androidx/blob/androidx-main/docs/kdoc_guidelines.md
- Android Kotlin style guide: https://developer.android.com/kotlin/style-guide
- Architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- UI events: https://developer.android.com/topic/architecture/ui-layer/events
- Domain layer: https://developer.android.com/topic/architecture/domain-layer
- Modularization patterns: https://developer.android.com/topic/modularization/patterns
- Coroutines best practices: https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- Custom modifiers: https://developer.android.com/develop/ui/compose/custom-modifiers
- Compose lint: https://developer.android.com/develop/ui/compose/tooling/lint
- Compose foundation releases (Grid, FlexBox stable; Style API experimental): https://developer.android.com/jetpack/androidx/releases/compose-foundation
- Compose UI releases (MediaQuery changes in 1.13): https://developer.android.com/jetpack/androidx/releases/compose-ui
- Now in Android modularization: https://github.com/android/nowinandroid/blob/main/docs/ModularizationLearningJourney.md
  and its `feature/foryou/{api,impl}` and `core/designsystem` trees
- androidx sources: `PullToRefresh.kt`, `Switch.kt` (material3), `NavDisplay.kt` (navigation3-ui), on `androidx-main`
- Android skills: https://github.com/android/skills (local copy v1.0.13), https://developer.android.com/tools/agents/android-skills
- Android Studio skill folders: https://developer.android.com/studio/gemini/skills
- Codex skill folders: https://developers.openai.com/codex/skills
- Gemini CLI skill folders: https://geminicli.com/docs/cli/skills/
- D8 and spaces in names: https://discuss.kotlinlang.org/t/android-issue-with-backticked-method-names/66
- Local evidence: lint ids from the Compose/Navigation 3/Lifecycle lint jars in `~/.gradle/caches/9.7.1/transforms`;
  `resourcePrefix` on `CommonExtension` in `gradle-api-9.4.1.jar`.

Marked unverified above: whether Claude Code scans `.agents/skills/`; which folders `android skills add --project`
writes; the material3 sheet, ripple and `navigationevent-compose` file paths; the exact ids of the coroutine and
flow-in-composition lint checks; whether `NavDisplay.TransitionKey` and `sceneDecoratorStrategies` are in Navigation 3
1.2.0 or only on `androidx-main`.
