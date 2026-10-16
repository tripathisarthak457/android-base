# Code standards for android-base templates

Status: proposal for adoption, 2026-10-09. Written for implementers and for the fresh-context reviewers of
MASTER-PLAN A12.8. Cite a rule by its id in review findings (`API-3 violated: modifier applied to a child`).

Precedence: `REQUIREMENTS.md` > `MASTER-PLAN.md` (Part D, A11.3) > this file > the upstream guides it is built
from. Where this file deliberately departs from a Google guide it says **Deviation** and gives the reason.
MUST rules block a merge. SHOULD rules need a reason in the review thread when broken.

Sources (all read for this file; see `standards.md` §6 for URLs): androidx `compose-api-guidelines.md` and
`compose-component-api-guidelines.md`, androidx `docs/api_guidelines/kotlin.md` and `docs/kdoc_guidelines.md`, the
Android Kotlin style guide, the architecture recommendations, UI events, domain layer and modularization pages,
coroutines best practices, the custom-modifiers guide, Now in Android, androidx sources (`PullToRefresh.kt`,
`Switch.kt`, `NavDisplay.kt`), and Google's Android skills v1.0.13 (navigation-3, navigation-event, adaptive,
styles, edge-to-edge, agp-9-upgrade, r8-analyzer, testing-setup, android-profiler, intent and permission security).

The forbidden performance patterns are in MASTER-PLAN A11.3 and are not repeated here; this file adds the code
quality bar around them.

---

## 1. Comments and KDoc (requirements 7 and 27: zero slop)

**C-1 (MUST) A comment states a reason the code cannot.** It belongs on a decision a reader would question: a
constraint, a workaround (and for what), a trade-off, the bug it prevents, an invariant. If deleting the comment
loses nothing a competent reader of that file needs, delete it.

**C-2 (MUST) These comment kinds are never written:**

| Kind | Example of what not to write |
|---|---|
| Narrating the code | `// Set loading to true` above `setState { copy(loading = true) }` |
| Narrating the change or its history | `// Added haptics`, `// Fixed back glitch`, `// Updated to Nav3`, `// used to be a fade`, `// New:` |
| Process and conversation | `// As requested`, `// Step 1`, `// per review`, ticket ids, agent names |
| Measurement anecdotes | `// Cut jank from 4.2 % to 1.1 % on the Redmi` (numbers go to commit messages and `docs/`) |
| Banners and dividers | `// ===== Helpers =====`, `// ── Neutrals · light ──────` |
| Shouting labels | `// IMPORTANT:`, `// NOTE:`, `// CRITICAL:`, `// PERF:` |
| Vague justification | `// for performance`, `// to avoid recomposition`, `// workaround` with no mechanism named |
| Commented-out code, `TODO`/`FIXME` | unless the plan asks for a TODO, with an owner and condition |
| KDoc that repeats the signature | `@param onClick the click handler`, `@param modifier the modifier` |

**C-3 (MUST) Voice.** Full sentences in the present tense, in the voice of the file around them. Name the
mechanism: what would go wrong, under which condition. Keep it to the lines it explains; put a longer rationale in
KDoc of the declaration or in `docs/`.

Slop and its rewrite:

```kotlin
// Don't
// IMPORTANT: use graphicsLayer for performance!
Modifier.graphicsLayer { translationY = offset.value }

// Do (only if the reason is not already obvious from the surrounding file)
// Read in the layer block, so a pull moves the content without recomposing the list.
Modifier.graphicsLayer { translationY = offset.value }
```

```kotlin
// Don't
// Handle the back press
// Added debounce to fix rapid back bug
if (now - lastBack < 300) return

// Do
// A second Back inside the pop's first frames joins that pop; BackGate turns the burst into one multi-pop.
if (gate.isAnimating) { gate.coalesce(); return }
```

Examples already in the template that meet the bar (keep this voice):

```kotlin
// A quick tap is over before the press has shown. Finish the dip first, briefly, so the tap is felt;
// a held press has already arrived and goes straight to the bounce.            (foundation/Pressable.kt)
// Counted, so a second finger lifting does not clear the first one's highlight.  (foundation/Pressable.kt)
```

androidx writes the same way, e.g. `NavDisplay.kt`: "AnimatedContent does not change the zIndex of content that is
already on the screen, regardless of what you pass as the targetZIndex."

**C-4 (MUST) KDoc is required on:** every public declaration of `:core:designsystem`, `:core:navigation`, `:core:mvi`,
`:core:common`, `:core:ui`, and every declaration in an `api/` package. Elsewhere KDoc follows C-1 (only when the
name and signature do not say it all).

**C-5 (MUST) KDoc shape** (Android Kotlin style guide, androidx `kdoc_guidelines.md`):

- First line: a summary fragment (noun or verb phrase, capitalised and punctuated like a sentence). It is what the
  IDE and an agent see in completion lists.
- Then, if needed: when to use it and when not (name the alternative), and behaviour the signature cannot show:
  threading, who owns state, units, what happens on recomposition or process death, accessibility semantics.
- Block tags in the order `@constructor @receiver @param @property @return @throws @see`, never empty.
- Links as `[AppSheet]`, never to the declaration itself; Markdown, never HTML; code in backticks or a fenced block.
- **Deviation** from androidx (which wants `@param` for every parameter): write `@param` only for a parameter whose
  meaning, unit, default or interaction with another parameter is not evident from its name and type. Never
  `@param modifier`. Reason: generated apps are read by agents and people who see the signature; one-line restatements
  are exactly the slop requirement 7 bans.

```kotlin
// Do
/**
 * An iOS-style switch: the thumb widens while pressed and can be dragged across.
 *
 * Use [AppToggleRow] for a labelled setting; it makes the whole row the toggle target. The switch
 * draws no press overlay: the thumb's motion is the feedback.
 *
 * @param onCheckedChange called with the new value; `null` makes the switch display-only, so it
 *   is not focusable and is announced without an action.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
)
```

**C-6 (MUST) Suppressions carry a reason** unless the reason is the line above (an `UNCHECKED_CAST` right after a
type check). `@Suppress("ComposableNaming") // A modifier-style factory: lower case reads as a modifier.`

**C-7 (MUST)** A `verifyCodeRules` allow marker is `// allow(<rule-id>): <reason a reviewer would accept>` (A11.3).

---

## 2. Compose component API shape (`:core:designsystem`, `:core:ui`, `:core:navigation`)

The androidx guidelines put "library development" at SHOULD and "app development" at MAY for most of these. Our
design system is a library to every feature and to every generated app, so it is held to the library bar: these are
MUST here.

**API-1 Naming.** A `Unit` composable is a PascalCase noun (`AppTabBar`, not `ShowTabBar`). A composable that returns
a value is camelCase (`rememberAppTransitions()`). A composable that remembers and returns a mutable object starts
with `remember`. A `CompositionLocal` is `LocalX` only when no more descriptive name fits, and never ends in `Local`.
Component parameters use the bare adjective for state (`enabled`, `checked`, `selected`, `expanded`), not `isEnabled`.

**API-2 Parameter order.** Required parameters, then `modifier`, then optional parameters, then the trailing content
slot. Among required parameters the state comes before its callback (`checked, onCheckedChange`); actions lead
(`onClick` first for buttons, as material3 does). Among optional parameters: `enabled`, variant and size, `colors`
and other `XDefaults` values, `contentPadding`, `interactionSource`, then other slots.

```kotlin
// Do (material3's Switch orders its parameters the same way, with thumbContent and colors in between)
fun AppSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier,
              enabled: Boolean = true, interactionSource: MutableInteractionSource? = null)
```

**API-3 The modifier parameter.** Exactly one `Modifier` parameter, named `modifier`, the first optional parameter,
default `Modifier`, applied once and first to the outermost layout of the component. Internal modifiers are chained
after it, never before it and never on a child.

```kotlin
// Don't
Box(Modifier.padding(16.dp)) { Icon(modifier = Modifier.size(24.dp).then(modifier)) }
// Do
Box(modifier.padding(16.dp)) { Icon(modifier = Modifier.size(24.dp)) }
```

**API-4 Behaviour that a modifier can add is not a parameter.** No `fillWidth`, `padding`, `width`, `clipToCircle`,
or `onClick` on a component that is not itself a control.

```kotlin
// Don't (present today in AppButton)
AppButton(text = "Pay", onClick = pay, fillWidth = true)
// Do
AppButton(text = "Pay", onClick = pay, modifier = Modifier.fillMaxWidth())
```

**API-5 Slots over data.** Content that a caller may want to style, replace, animate or render as glass is a slot:
`content: @Composable RowScope.() -> Unit`, optional slots `icon: (@Composable () -> Unit)? = null`. A convenience
overload that takes `text: String` is fine when it delegates to the slot overload. A slot keeps its lifecycle when the
component's layout changes: wrap with `remember(content) { movableContentOf(content) }` if it moves between parents.
No DSL-based slot APIs outside lazy layouts.

**API-6 One `XDefaults` object per component** (MASTER-PLAN A4.7 already requires it). Every default expression is
inline or reads `XDefaults`; no private or internal call inside a public default expression. Dimension and value
constants are PascalCase `val`s (`AppButtonDefaults.MinHeight`). Colours come from
`@Composable fun colors(...): XColors` where `XColors` is an `@Immutable` class with internal per-state resolvers
(`internal fun container(enabled: Boolean): Color`), the way material3's `SwitchColors.thumbColor(enabled, checked)`
works. Never `null` as "use the default".

**API-7 Variants.** Prefer separate components to a style grab-bag class. A variant enum (`ButtonVariant`) is
acceptable when variants differ only in which tokens they read and share layout and behaviour; it must still resolve
through an overridable `colors: AppButtonColors = AppButtonDefaults.colors(variant)` so a caller never forks the
component to change a colour. Structural differences (a toggle button, a button with a menu) are separate components:
one component solves one problem.

**API-8 State.** Stateless and controlled by default (`value` + `onValueChange`). When a component has real policy
(scroll position, sheet detents, refresh offset), hoist it into `@Stable XState`, declared as an interface with a
factory function of the same name and `rememberXState()`, as `PullToRefreshState` and `rememberPullToRefreshState()`
do. Never accept `MutableState<T>` or `State<T>`: take the value, or `() -> T` when the read must be deferred to layout
or draw. App code SHOULD use a concrete class until a second implementation exists.

```kotlin
// Don't
fun AppBadge(offset: State<Dp>)
// Do (read happens where the badge is placed, not where it is composed)
fun AppBadge(offset: () -> Dp)
```

**API-9 Emit or return, never both.** A composable that draws returns `Unit`.

**API-10 Explicit over implicit.** A `CompositionLocal` carries only tree-wide ambient data (theme, `LocalTabVisible`,
`LocalOverlayHost`), is read in default expressions, and uses `staticCompositionLocalOf` for values that almost never
change (theme objects). Never pass one component's configuration through a local.

**API-11 Events.** Callback parameters are `on` + verb (`onClick`, `onCheckedChange`, `onSelect`,
`onDismissRequest`). MVI events are past-tense facts named for what happened (`ItemClicked`, `QueryChanged`,
`Appeared`), as the template does.

**API-12 Accessibility is part of the API.** Icon-only controls require `contentDescription: String?` (an explicit
`null` means decorative). Interactive components set a `Role`, merge descendants, meet 48 dp through
`minimumTouchTarget()`, and expose swipe or drag gestures as custom accessibility actions. A new component copies the
semantics of the closest foundation component.

**API-13 Previews and first frame.** A component draws its initial state in the first frame and in preview: no
`LaunchedEffect` to set up initial state; branch on `LocalInspectionMode` only for things a preview cannot do.

**API-14 Evolving a public component.** Generated apps take template changes through `upgrade_project.py`, so a
public signature change breaks user code. Add parameters with defaults, last (before the trailing lambda). Rename or
remove through one release of `@Deprecated(level = WARNING, replaceWith = ...)`.

**API-15 Component or modifier.** If it applies to any single element and adds behaviour, it is a modifier. If it adds
or removes structure (an overlay, a placeholder), it is a component.

---

## 3. Modifiers, state and recomposition

**M-1 (MUST) Custom modifiers.** In order of preference: chain existing modifiers; write a `Modifier.Node`; never
`Modifier.composed` and never a `@Composable` modifier factory in a component or feature (it is never skipped and
resolves locals at the call site). A node is three parts: a public factory `fun Modifier.x(...) = this then XElement(...)`,
a `private data class XElement : ModifierNodeElement<XNode>` whose `update()` mutates the existing node, and a
`private class XNode : Modifier.Node()`. Opt out of auto-invalidation and call `invalidateDraw()` or
`invalidateMeasurement()` yourself when one input affects only one phase. material3's `Switch` (`ThumbElement`,
`ThumbNode`) and `PullToRefresh` (`PullToRefreshElement`, `PullToRefreshModifierNode`) are the reference.

**M-2 (MUST) Frequently changing values are read in layout or draw** (`offset { }`, `graphicsLayer { }`,
`drawBehind { }`, `placeWithLayer { }`, or inside a node's `measure`/`draw`, where reads are observed for you).
material3's pull-to-refresh moves its indicator from `placeWithLayer(layerBlock = { translationY = ... })`.

**M-3 (MUST) Use the foundation primitive before writing your own.** `AnchoredDraggableState` for anything that
snaps to anchors (sheet detents, swipe actions, switch drag); `NestedScrollConnection` for pull and collapse;
`OverscrollEffect` through `LocalOverscrollFactory` for rubber banding; `Modifier.animateItem()` for list insert,
delete and reorder; `TextFieldState` with `BasicTextField`/`BasicSecureTextField`; `IndicationNodeFactory` for press
visuals. Hand-rolled drag state with a coroutine per pointer event is forbidden (A11.3 #5).

**M-4 (MUST) Stability.** UI state classes are `@Immutable` data classes holding read-only collections that are never
mutated after construction (or `ImmutableList`); `config/compose-stability.conf` lists the external types we treat as
stable. Primitive state uses `mutableIntStateOf`/`mutableFloatStateOf`/`mutableLongStateOf`
(lint `AutoboxingStateCreation`). Hoisted state holders are `@Stable` and honour the contract (observable public
properties, consistent `equals`).

**M-5 (MUST) Lint is part of the bar.** The Compose, Navigation 3 and Lifecycle artifacts ship their own lint checks.
The convention plugin raises these ids to errors in every Compose module (each id was found as a string constant in
the lint jars of the artifacts in the local Gradle cache):
`ComposableNaming`, `ComposableLambdaParameterNaming`, `CompositionLocalNaming`, `ModifierParameter`,
`ModifierFactoryReturnType`, `UnnecessaryComposedModifier`, `SuspiciousModifierThen`, `UnrememberedMutableState`,
`UnrememberedAnimatable`, `UnrememberedState`, `RememberReturnType`, `RememberInComposition`,
`AutoboxingStateCreation`, `AutoboxingStateValueProperty`, `MutableCollectionMutableState`, `FrequentlyChangingValue`,
`OpaqueUnitKey`, `ProduceStateDoesNotAssignValue`, `ConfigurationScreenWidthHeight`, `LocalContextConfigurationRead`,
`LocalContextResourcesRead`, `NonObservableLocale`, `MultipleAwaitPointerEventScopes`, `ExitAwaitPointerEventScope`,
`UnusedBoxWithConstraintsScope`, `UseOfNonLambdaOffsetOverload`, `SceneEqualsHashCode`.
The same jars also contain detectors for coroutine creation, flow operators and `StateFlow.value` reads in
composition, modifier factory declarations, `ModifierNodeElement.inspectableProperties`, and
`Lifecycle.currentState` reads in composition; take their exact ids from `./gradlew lint --list` (or a lint report)
when wiring the list, because an unknown id in `lint { error += ... }` is ignored without a warning.

---

## 4. Kotlin style (Android Kotlin style guide, kotlinlang conventions)

**K-1 Formatting.** 4-space indent, 100-column limit, K&R braces, trailing commas on multi-line lists, no wildcard
imports, imports in one ASCII-sorted list. One formatter decides: ktlint `android_studio` code style through the
`formatting` feature's Spotless setup (A5.1), and the template itself must already pass it so enabling the feature
produces no diff. **Deviation today:** `.editorconfig` says 120 columns and nothing enforces it (329 lines exceed 120,
955 exceed 100 in the WP03 tree). Fix with one mechanical reformat commit after the last UI wave merges, not inside
feature diffs.

**K-2 Files.** A file holding one top-level class is named after it. A component file holds one public component and
its `XDefaults`, `XColors`, `XState` and private pieces, in that order, with private constants last (material3
layout). A file with several top-level declarations gets a PascalCase name for what they are (`NavTransitions.kt`).
No `Utils.kt`, `Helpers.kt`, `Extensions.kt`, `Constants.kt`, `Common.kt`. A file over about 400 lines is split by
concept, and the proposed `file-length` rule fails at 500 (in the WP03/WP04 trees today: `AppIcons.kt` 709 lines, `Pressable.kt` 519, `AppTabBar.kt` 577,
`CatalogComponents.kt` 658).

**K-3 Member order** (kotlinlang): properties and `init`, secondary constructors, functions, companion object last.
Overloads sit together. Public before internal before private within a file.

**K-4 Names.**
- Packages are lowercase words run together, no underscores: `feature.orderhistory`.
- Acronyms are words: `HttpClient`, `userId`, `UiState`, `OrderUrl`, never `URL`/`ID` inside an identifier.
- Constants: in Compose modules (`:core:designsystem`, `:core:navigation`, `:core:ui`, `feature/**` UI files), deeply
  immutable values are PascalCase (`private const val DragMultiplier = 0.5f`, `private val FadeTail = 32.dp`), as the
  Compose API guidelines and androidx Compose sources do. In non-Compose modules, `const val` and deeply immutable
  top-level values are `UPPER_SNAKE_CASE` (Android Kotlin style guide). Never mix the two in one file (today
  `theme/Palette.kt` has `AA_TEXT` next to PascalCase colour values, `AppFonts.kt` has `const val Sans`).
- Enum entries are PascalCase everywhere (already the template's style).
- Backing properties are `_name`. Boolean properties read as predicates (`isOnline`, `hasContent`, `canSubmit`);
  component parameters do not (API-1).

**K-5 Visibility.** Everything is `private` or `internal` unless it is a module's surface: `api/` packages, design
system components and their `XDefaults`/`XState`, the navigation entry points features call. In modular mode the
`api` module is the only cross-module surface. SHOULD (tier 2): `explicitApi()` in `:core:designsystem`,
`:core:navigation`, `:core:mvi` and `:core:common`, as androidx libraries (material3, navigation3) now do.

**K-6 Types and nullability.** IDs are `@JvmInline value class`es (`OrderId`). Closed hierarchies are `sealed
interface`s with `data object` singletons. No `!!` in main sources: restructure, or `checkNotNull(x) { "why it
cannot be null here" }`. `require` for arguments, `check` for state, `error()` for unreachable branches. `when` over a
sealed type has no `else`. `lateinit` only for framework or DI injection.

**K-7 Expression style.** Expression bodies for single expressions; explicit return types on public API. One level
of scope functions at most; prefer `if (x != null)` to nested `?.let { }`. No extension functions on Android
framework types in shared modules unless they are the natural API (androidx `kotlin.md`).

**K-8 Test names.** In JVM tests (`src/test`) sentence names in backticks are allowed and are the template's style.
In `src/androidTest`, `benchmark/` and multiplatform `commonTest`, names are camelCase with optional underscores
(`pop_emptyStack_returnsFalse`): D8 rejects spaces in method names below DEX 040 (API 30), and our minSdk is 26.
**Deviation:** the Android style guide forbids spaces in function names everywhere; kotlinlang allows them in tests
and the template already uses them in 34 JVM test files.

---

## 5. Architecture and MVI

**A-1 (MUST) Unidirectional data flow.** A screen's ViewModel exposes one `state: StateFlow<S>` and takes
`onEvent(E)`. A screen is a `XRoute` (stateful: `hiltViewModel()`, effects, navigation) and a `XScreen` (stateless:
`state`, `onEvent`, `modifier`, private previews). ViewModels live at screen level only; reusable UI uses plain
state holders (API-8). No `Context`, `Resources`, `Activity` or `AndroidViewModel` in a ViewModel; strings travel as
`UiText`.

**A-2 (MUST) Collect with lifecycle.** `collectAsStateWithLifecycle()` only; lifecycle work in `LifecycleStartEffect`
and `LifecycleResumeEffect`, never in overridden Activity callbacks.

**A-3 ViewModel-to-UI events.** Google strongly recommends that every ViewModel event becomes UI state.
**Deviation (accepted, document in `docs/ARCHITECTURE.md`):** `MviViewModel` keeps a buffered `Channel` for effects
whose target is not UI state (navigation, opening a URL or share sheet). It is safe because `MviScreen` collects it
under `repeatOnLifecycle(STARTED)` on `Dispatchers.Main.immediate`, so an effect is never received and then dropped.
Rules that keep it safe: effects are only for navigation and system actions; anything the user must see (an error, a
"saved" confirmation) is state or a `UiMessage`; never add another channel. The `messages` channel is the open
question (tier 2): Google's pattern is a message in state plus `onMessageShown(id)`; keep the channel only if the
reviewer accepts the same Main.immediate argument.

**A-4 (MUST) Data layer.** A data module's public surface is its repository interface and models in `api/`; data
sources, DTOs and mappers are `internal`. Repositories expose `Flow` for data that changes and `suspend` functions for
one-shot work, are main-safe, and return `AppResult` from fallible one-shots. DTOs never appear in `api/`, a
ViewModel or a key. Naming, so a module's surface reads the same everywhere (**Deviation** from Google's optional
`getXStream()`): a stream without arguments is a property named for the data (`val profile: Flow<Profile>`); a stream
with arguments is a function named for the data (`fun orders(status: OrderStatus): Flow<List<Order>>`); one-shot
work is a verb (`refresh()`, `save(...)`, `fetchOrder(id)`).

**A-5 Implementations and fakes.** Name implementations for what they are: `OfflineFirstOrdersRepository` (with
`database`), `DefaultOrdersRepository` only when no better name exists, `FakeOrdersRepository` in tests. An interface
with one production implementation is allowed for repositories and named seams (they have a fake); nowhere else
(Part D 6).

**A-6 Models per layer.** `OrderDto` (`@Serializable`, `remote/dto/`), `OrderEntity` (`:core:database`), `Order`
(`api/model/`), and a UI model only when the screen needs a different shape. Mappers are extension functions in a
`XMappers.kt` beside the source type: `OrderDto.toDomain()`, `OrderDto.toEntity()`, `OrderEntity.toDomain()`.

**A-7 Domain layer (modular mode only, optional).** Create a domain class only when logic combines two or more
repositories or is reused by two or more ViewModels. Then follow Google's naming, which agents already know:
`GetOrderSummaryUseCase` (`verb + noun + UseCase`), `operator fun invoke`, stateless, main-safe, plain constructor
injection. This keeps MASTER-PLAN's "no ceremony" intent (no pass-through use cases) while matching the guide.

**A-8 Errors.** Expected failures are values (`AppResult.Failure(AppError)`, `LoadState.Error`), classified once at the
boundary that sees the exception (`:core:network`'s `ResponseUnwrapper`, the Room and DataStore wrappers). Never
catch `CancellationException`; never wrap a suspend call in `runCatching { }` (it swallows cancellation: today
`SessionCoordinator.signOut()` does this around `store.clear()` and `ResponseCache.clear()` around `dao.clear()`). `catch (e: Exception)` only at a classifying
boundary; `Throwable` only in `MviViewModel`'s guard. Log once, where handled, without personal data.

---

## 6. Coroutines

**CO-1 (MUST)** Dispatchers are injected (`AppDispatchers`); `withContext(dispatchers.io)` lives in data sources and
repositories, never in ViewModels or composables. No `Dispatchers.Default`/`IO` literal outside `app/**/di` (today
`core/network/NetworkClient.kt` decodes on a hard-coded `Dispatchers.Default`).
**CO-2 (MUST)** Every `suspend` function is main-safe.
**CO-3 (MUST)** ViewModels start coroutines and never expose `suspend` functions; `handleEvent` never suspends and
starts work only through `launchLatest`, `launchExclusive` and `observe` (A6.1).
**CO-4 (MUST)** Work that must outlive the screen runs in the injected `ApplicationScope`, joined when the caller
needs the result. No `GlobalScope`, no `runBlocking` outside tests.
**CO-5** Derived hot flows use `stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)`; nothing collects
from `init`.
**CO-6** Loops over blocking work call `ensureActive()`. Mutual exclusion in suspend code is a `Mutex`, never
`synchronized` around a suspension point. `callbackFlow` always ends with `awaitClose { unregister() }`.
**CO-7** Tests use `runTest`, `MainDispatcherRule` and a `StandardTestDispatcher(testScheduler)` injected through
`AppDispatchers`; assert `StateFlow.value` where possible, Turbine for sequences.

---

## 7. Navigation and Back (navigation-3 and navigation-event skills)

**N-1** Keys are `@Serializable data object`/`data class` `XKey : AppNavKey` in the owner's `api/` package (or the
shared-key folder of A2.4), carrying only primitives, same-package value classes or `:core:common` types; pass IDs and
load in the destination, never objects.
**N-2** A feature registers its entries in `navigation/<Feature>Navigation.kt` through `navGraph { entry<K> { } }`
(`@IntoSet`); it never calls `NavDisplay`, never touches a back stack, and never sets transition metadata
(`verifyCodeRules` `nav-fade`).
**N-3** Navigation goes through `AppNavigator`, which already gates on STARTED and drops duplicate pushes during
motion, so features do not add `dropUnlessResumed`.
**N-4** A custom `Scene` implements `equals`/`hashCode` (lint `SceneEqualsHashCode`) and its `SceneStrategy` is
remembered once.
**N-5** Back outside `NavDisplay` (sheets, menus, search) uses `NavigationBackHandler` with exactly one
`NavigationEventState` per handler, branching inside `onBackCompleted`; overlays register at overlay priority. No new
`BackHandler` or `OnBackPressedCallback`. Never override `ComponentActivity.navigationEventDispatcher`; per-tab scoping
uses `rememberNavigationEventDispatcherOwner(enabled = isSelected)`; dialogs get their own owner automatically.
**N-6** Predictive-back motion reads progress and swipe edge from the transition state inside `graphicsLayer`,
never through composition. The manifest keeps `android:enableOnBackInvokedCallback="true"`.

---

## 8. Adaptive layout and insets (adaptive and edge-to-edge skills)

**L-1 (MUST)** No orientation or resizability locks. No `LocalConfiguration.screenWidthDp` (lint
`ConfigurationScreenWidthHeight`) and no `isTablet` flags: layouts read the window size class and posture.
**L-2** Multi-pane is a Navigation 3 scene (`ListDetailSceneStrategy`, `SupportingPaneSceneStrategy` from
`material3.adaptive:adaptive-navigation3`, allowed by requirement 31), never `ListDetailPaneScaffold`. A detail shown
beside its list has no back arrow and is not full screen.
**L-3** The navigation area is our `AppTabBar(orientation)`; agents do not introduce `NavigationSuiteScaffold`
(it needs material3, requirement 31). Lists of cards become `LazyVerticalGrid(GridCells.Adaptive(minWidth))`;
`Grid` and `FlexBox` are stable in Compose 1.12 and may be used (the adaptive skill still calls them experimental).
`MediaQuery` is still experimental in 1.12 (its posture types change in 1.13): do not use it without an opt-in decision.
**L-4 (MUST)** Every `Activity` calls `enableEdgeToEdge()`; `windowSoftInputMode="adjustResize"`. Insets reach
scrolling content as `contentPadding`, never as padding on the list's parent (that is the opaque block of
requirement 17). IME: `Modifier.fitInside(WindowInsetsRulers.Ime.current)` before `imePadding()`; insets are consumed
once. With a floating bar, `window.isNavigationBarContrastEnforced = false`; system bar icons follow the app theme.
**L-5** Hover, focus, keyboard shortcuts and right-click context menus are part of an interactive component's
contract on large screens (ADAPTIVE-PLAN).

---

## 9. Testing (testing-setup skill)

**T-1 (MUST)** Test ViewModels (events to state and effects), repositories (fake data sources), mappers, and
navigation registration. Do not unit-test DI modules, Activities or composable internals.
**T-2 (MUST)** Fakes before mocks. A fake is named `FakeX` and lives next to its interface's owner: in the module's
`testFixtures` source set (modular mode: the `:api` module's), or in `:core:testing` for core seams. MockK only for a
final third-party type that cannot be faked.
**T-3** Tests mirror main packages and are named `XTest`. One behaviour per test; given, when and then are separated
by blank lines, not comments.
**T-4** Compose UI tests match semantics first; a `testTag` only when more than three matchers would be needed. Verify
state restoration.
**T-5** Screenshot tests: each screen at widths 400/610/900 dp by heights 400/500/1000 dp, plus 400×500 in each theme
and at font scale 1.5; each component in each theme and font scale. On AGP 9.4.1 that is the standalone
`com.android.compose.screenshot` plugin (deprecated; AGP test suites need 9.5.0-alpha03+); never both in one module.
**T-6** No `Thread.sleep`, no real network, no wall-clock time; animation tests drive `mainClock`. Never weaken a test
to get green (Part D 15).

---

## 10. Build, R8 and security

**B-1 (MUST)** AGP 9 rules: built-in Kotlin (no `org.jetbrains.kotlin.android`), KSP ≥ 2.3.6, Hilt ≥ 2.59.2, never
`android.disallowKotlinSourceSets=false`, none of the migration flags (`android.builtInKotlin`, `android.newDsl`,
`android.uniquePackageNames`, `android.enableAppCompileTimeRClass`), and never `clean` as a fix.
**B-2 (MUST)** Module build files contain plugins, namespace and dependencies only; everything else is a convention
plugin. `implementation` over `api` (the one intended `api` edge is `:core:ui` re-exporting the design system).
Versions only in `libs.versions.toml`; project accessors (`projects.core.common`).
**B-3 (MUST)** R8 full mode with `proguard-android-optimize.txt`, minify and resource shrinking on release. Every keep
rule is as narrow as the reflection it serves, with a comment naming that reflection; no package-wide `** { *; }`, no
`!` inversions, no rules for libraries that ship consumer rules, no `-dontoptimize`/`-dontobfuscate`. Check rule
changes with a Macrobenchmark journey and with the r8-analyzer skill.
**B-4 (MUST)** Every manifest component states `android:exported`; only the launcher Activity (and its deep-link
filters) is exported. `PendingIntent`s are `FLAG_IMMUTABLE` with an explicit component. An `Intent` taken from extras is
never launched without `IntentSanitizer`. Dynamic receivers are `RECEIVER_NOT_EXPORTED`. `FileProvider` paths are
scoped, never `/`. Deep-link input becomes a typed key or `null` in `DeepLinkRoute`, never a raw string handed onward.
**B-5** Runtime permissions are requested in context: check, rationale, launch the contract, send a permanent denial to
app settings, and re-check at the point of use. Photo Picker instead of storage permissions.
**B-6** Smoothness is judged only on release builds with `perf.sh` (Part D 12); traces for escalations come from the
android-profiler skill's Perfetto workflows.

---

## 11. Files and packages (summary; the full layout is `standards.md` §3)

**F-1 (MUST)** Package equals directory; a module's root package is its namespace (`<pkg>.feature.orders`).
**F-2 (MUST)** Inside a feature, packages are named for a screen, flow or concept (`list/`, `detail/`, `checkout/`),
never for a layer (`ui/`, `viewmodel/`, `presentation/`, `screens/`).
**F-3 (MUST)** No `util`, `utils`, `helper`, `common`, `misc`, `base` or `internal` packages inside a module; a helper
lives in the package of the concept it serves, `private` when it can be.
**F-4 (MUST)** `api/` packages stay pure (`verifyApiPurity`).
**F-5** Resource names carry the module's prefix (`orders_title`, `orders_empty_body`): the library convention
plugin sets `resourcePrefix` (present on `CommonExtension` in AGP 9.4.1) and raises lint `ResourceName` to an error.
Same-named resources in two libraries silently override each other in the app's merge. A string lives in the module
that shows it.
**F-6** Tests mirror the main package of the code they test; fakes live in `testFixtures`.
