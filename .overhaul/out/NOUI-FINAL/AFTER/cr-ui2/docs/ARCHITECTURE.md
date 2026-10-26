# Architecture

How this project is put together and why. The code's own KDoc covers each class; this page covers the decisions
that span several of them. [PERFORMANCE.md](PERFORMANCE.md) covers what keeps it smooth, and
[DESIGN-SYSTEM.md](DESIGN-SYSTEM.md) the theme and the components.

Contents:

1. [Modules](#modules)
2. [MVI](#mvi)
3. [Navigation](#navigation)
4. [Data and the network](#data-and-the-network)
5. [Storage and the session](#storage-and-the-session)
6. [App-wide services](#app-wide-services)
7. [Copy and languages](#copy-and-languages)
8. [Every form factor](#every-form-factor)
9. [Build](#build)
10. [Testing](#testing)

## Modules

```
        ┌──────────────┐
        │     app      │   composition root: wiring, no logic
        └──────┬───────┘
        ┌──────▼───────┐
        │  feature/*   │   screens and their ViewModels
        └──────┬───────┘
        ┌──────▼───────┐
        │    data/*    │   one business domain each
        └──────┬───────┘
        ┌──────▼───────┐
        │    core/*    │   infrastructure
        └──────────────┘
```

| Tier | May depend on |
|---|---|
| app | anything |
| a feature | core modules and data modules, never another feature |
| a data module | core modules, never another data module |
| a core module | other core modules |

`verifyModuleDependencies` checks this on every build, before anything compiles, so a wrong edge fails the build
instead of waiting for review. The rules that matter are the two that forbid siblings. A feature-to-feature edge
looks harmless the day it is added; a year later it is why nothing can be built or tested on its own, and it always
arrives as a one-line convenience inside an unrelated change. When two features need the same data, both depend on
the data module that owns it; when one must react to the other, a data module exposes the state and both observe it.

### Compact and modular

The project is generated in one of two shapes, and the same rules hold in both:

- **Compact** (the default): one module per feature and per data domain. A feature's public surface is its `api`
  package (its navigation keys); everything else is `internal` (`feature-public`). Google's modularisation guidance
  warns against splitting a small app further than this.
- **Modular**: each feature and data module is split into an `api` module (pure Kotlin) and an `impl` module, and
  business rules shared by features can live in `domain` modules. An `impl` sees only other modules' `api`.

`verifyApiPurity` keeps every compact project splittable, so moving to modular is one command of the generator
(`python3 -m genkit.targets.android.modes <project>`) when the team grows. `:core:common`, every `api` module and every
`domain` module are pure Kotlin: applying an Android or Compose plugin to one fails the same check.

### Where a feature lives

```
data/orders/       API service, DTOs, mapper, repository interface and implementation      no Compose
feature/orders/    api keys, contract, ViewModel, route, screen, navigation registration  no network
```

The data half compiles in parallel with the feature half, and cross-feature UI needs no feature edge: a cart badge
on the home screen reads the cart repository, not the cart feature. In a data module the public surface is its
`api` package; DTOs, mappers and the implementation are `internal`.

### What each core module holds

| Module | Holds |
|---|---|
| `:core:common` | Pure Kotlin: `AppResult`, `AppError`, `LoadState`, `AppDispatchers`, `ApplicationScope`, `NetworkMonitor`, `SessionController`, `SessionScopedStore`, `AppClock`, the logger (`Log`) and `AppNavKey`. |
| `:core:mvi` | `MviViewModel`, the UI contracts (`UiState`, `UiEvent`, `UiEffect`), `UiMessage` and `UiText`. |
| `:core:datastore` | The settings and session stores, the token store and the Keystore cipher. |
| `:core:designsystem` | The theme (`AppTheme` and its tokens), window size classes and previews; [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md). No Material. |
| `:core:ui` | What screens share beyond the theme: `MviScreen`, message delivery, `UiText` resolution, `AppError.toUiText`, locales. |
| `:core:navigation` | The navigation runtime: the destination registry, `AppNavigator`, the back stacks and `TabStacks`, deep links and process-death restore. No design system. |
| `:core:navigation:shell` | The shell that draws it: the tab bar, rail and sidebar, the stack host and its slides, the back card, sheets and covers. Only `:app` and `:catalog` depend on it. |
| `:core:testing`, `:core:testing:compose` | `MainDispatcherRule`, `testDispatchers()`, `FakeNetworkMonitor`, `FakeSessionController`, the Compose test helpers and `RecompositionCounter`. |
| `:core:network` | The HTTP client and everything around it. The only module that names Ktor. |
| `:core:analytics` | `AnalyticsTracker` and `CrashReporter`, with no-op or Firebase implementations. |
| `:core:flags` | `Flag` declarations and the `FeatureFlags` source. |
| `:core:notification` | Notification channels, the permission check and push-token registration. |
| `:core:media` | The photo and video picker, the system camera and image compression. |
| `:core:database` | The app's own Room database, separate from the network cache. |
| `:core:applock` | The app lock and the system biometric prompt. |
| `:core:playstore` | Play's in-app update and review prompts. |
| `:core:devtools` | The in-app network inspector and environment badge; dev and debug builds only. |
| `:core:perfhud` | The live performance overlay and its Perf tab; dev and debug builds only ([PERFORMANCE-OVERLAY.md](PERFORMANCE-OVERLAY.md)). |

## MVI

Each screen has three types and one ViewModel built on `MviViewModel`.

```kotlin
@Immutable
data class OrdersState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<Order> = emptyList(),
) : UiState

sealed interface OrdersEvent : UiEvent { … }
sealed interface OrdersEffect : UiEffect { … }
```

- **State** is everything the screen renders, and it is complete: rendering never needs a value outside it.
- **Events** are everything the person or the system can do to the screen.
- **Effects** happen once and are not state: navigate, close the keyboard, open the dialler. An effect stored in
  state fires again after a rotation.

The screen is a stateless function, `OrdersScreen(state, onEvent)`. A route connects it to the ViewModel through
`MviScreen`, which collects the state with the lifecycle and hands effects to a callback. Previews and UI tests call
the screen and never need a ViewModel.

### Events never suspend

`onEvent` calls `handleEvent` at once, on the main thread, and `handleEvent` cannot suspend. A read of `current`
followed by `setState { copy(…) }` is therefore never interleaved with another event. The alternative,
`viewModelScope.launch { handleEvent(event) }` per call, lets two events that read and then write the state interleave
between the read and the write, and one update is lost: only under fast input, so it is very hard to reproduce. And
since no handler waits, nothing an event starts can make Back or a keystroke wait.

Slow work goes through the keyed launchers. `launchLatest(key, debounce)` cancels the previous run under the same key,
for work where only the newest request matters (search as you type, reload, paging); `debounce` is a `Duration`.
`launchExclusive(key)` ignores a new request while one runs (submit, pay, save); with
`busy = { copy(submitting = it) }` it raises the screen's flag as the run starts and lowers it however the run ends,
an unexpected throw and a cancellation included, so a button it disables always comes back. Repository flows go
through `observe(flow) { value -> copy(…) }`, which collects them only while the screen observes the state, with a
five-second grace period so a rotation does not restart them.

### The first load

A screen loads when it is first started: the ViewModel calls `onFirstObserved { load() }` in `init`, and the block runs
once, the first time the route collects the state (`MviScreen` collects with `collectAsStateWithLifecycle`, so only
while the screen is started). A second collector, a rotation or a return from the background does not load again;
a refresh or a retry is an event. Not at construction, which would fetch for every ViewModel built, a test's or a
restored back stack entry's included, and not from a `LaunchedEffect` in the route, which runs in composition, so
also for a screen composed before anyone sees it.
The shell's tab warm-up is such a case: it composes unseen tabs with their lifecycle held at CREATED, so a screen that
loads when started does not load early.
The reference screens load this way.
A screen whose data may already be in memory starts its state from it instead, so its first frame shows content
([PERFORMANCE.md](PERFORMANCE.md#navigation-and-back)).

### Effects are delivered once, on purpose not as state

Google's guidance models one-off events as state the UI clears. This project sends them through a buffered `Channel`
instead, a deliberate deviation: an effect sent while nothing collects waits instead of being dropped or replayed,
and `MviScreen` collects under `repeatOnLifecycle(STARTED)` on `Dispatchers.Main.immediate`, so an effect is handled
in the frame it arrives and a lifecycle stop in between cannot lose it. A navigation emitted while the app is in the
background is held until the person returns.

### Messages and errors

`showMessage(text)` sends a transient note such as "Saved"; its `kind` is `MessageKind.Info` unless you pass another
(`MessageKind.Error` for "Could not refresh"). Notes have their own channel, `messages`, beside the effects.
`MviScreen` shows them as toasts in the window's toast layer, above the tab bar and any sheet.

An exception that escapes work started with `launchLatest`, `launchExclusive` or `observe` is logged and passed to
`onUnexpected`, which does nothing unless the ViewModel overrides it. A handler that expects a failure turns it into
state (usually `LoadState.Error`) and does not rely on `onUnexpected`.

### LoadState, not a boolean and a nullable

`isLoading` plus a nullable error allows four combinations, three of which make sense; the fourth, loading and failed
at once, shows as a spinner over an error. A sealed `LoadState` (`Idle`, `Loading`, `Refreshing`, `Success`, `Empty`,
`Error`) cannot express it. `Loading` has nothing to show and draws a skeleton; `Refreshing` already has content and
keeps it. Treating them alike is why pull to refresh so often blanks the list it refreshes.

### Surviving process death

Loaded data is fetched again after process death and is not saved. What the person typed is saved: the ViewModel
holds each typed field as `savedStateHandle.saveable(KEY, saver = TextFieldState.Saver) { TextFieldState() }`, under
`@OptIn(SavedStateHandleSaveableApi::class)`. A password is never saved: the system writes that bundle to disk.

## Navigation

Feature modules never import a navigation library. A destination is a `@Serializable` key implementing `AppNavKey`
in the feature's `api` package, and the feature contributes what each key shows through Hilt `@IntoSet`
(`navGraph { }`). That one registration also records the key's serializer, which the saved back stack is written with.

```kotlin
@Serializable
data class OrderKey(val orderId: Long) : AppNavKey

@Provides @IntoSet
fun ordersNavGraph(): NavGraphEntry = navGraph {
    entry<OrderKey> { key -> OrderRoute(key.orderId) }
}
```

Adding a screen touches no file outside its module: no central route class and no `when` in the app to extend, so two
people adding screens in one week do not conflict. A key registered twice fails when the registry is built.

ViewModels navigate through `AppNavigator` and never touch a back stack. It sends a `NavCommand` (`Navigate`, `Up`,
`ResetTo`, `PopTo`) that the host applies while the activity is started, so a ViewModel navigates without holding
anything from the composition and a test asserts on the commands.

### The runtime and what draws it

`:core:navigation` is the runtime a feature sees: keys, `navGraph { }`, `AppNavigator`, deep links, the back stacks,
`TabStacks` and their restore after process death, as the `navigation-3` skill's modular recipe keeps its navigator
in a common module. It draws nothing and does not depend on `:core:designsystem`.
`:core:navigation:shell` draws it: the tab bar, rail and sidebar, the stack host, the back card, sheets and covers.
Only `:app` and `:catalog` depend on the shell, so a feature cannot reach a shell API by accident.
What only a host uses (a stack's entries and its back gate, the rules a command is applied by, the content a key
renders) is marked `@NavigationHostApi`, an opt-in a host makes for its whole module and a feature never makes.

### Keys carry ids, not models

A key is written into the saved-state bundle. A key holding a whole object makes the bundle larger and goes stale in
the background. The id reaches the ViewModel through Hilt assisted injection, at construction; every entry has its own
ViewModel store, so each open screen has its own ViewModel and Retry works from the injected id even when the first
load failed.

### A key is registered in one step

A key whose serializer is missing works until the system kills the app in the background; then the app reopens at the
start destination instead of where the person was. So there is no second list to forget: `entry<K>` records K's
serializer, `NavKeySerialization` is built from the graphs, and a key that can be opened can be saved.
`NavKeyRegistrationTest` still fails on a key in the app that no graph registers.

### The entry point is derived, not navigated to

`AppDestinations` names the tabs and the start destination; nothing else names one. `StartupViewModel` turns the
settings and the session into an `EntryIdentity`, and `AppRoot` shows one host per identity:

```
EntryIdentity.Onboarding  ->  the walkthrough, one stack, no tab bar
EntryIdentity.SignedOut   ->  sign in, one stack, no tab bar
EntryIdentity.SignedIn    ->  the tabs, starting at the first
```

Finishing onboarding is a write to the settings store and signing in is a token reaching the token store; the UI
follows from the same emission. Calling `navigate()` at start-up instead would have to handle the flag changing for
other reasons, each screen differently. `HostCrossfade` swaps whole hosts when the identity changes, and only the
arriving host takes navigator commands. Sign-out runs the other way: `SessionCoordinator` clears every
`SessionScopedStore` first and only then announces it, so sign-in never appears over the last person's data.

### Each tab keeps its own back stack

`TabStacks` holds one stack per tab and the tab in front, and `ShellState` composes it with the presentation stack
and the chrome. Back inside a tab pops that tab; Back at a tab's root is the shell's. Each tab's entries stay
decorated (saved state and a ViewModel store per entry) while another tab shows, because Navigation 3
treats an entry that leaves the list it was given as popped and discards its state: leaving a tab only hides it.
A tab switch does not slide, which would read as forward navigation that Back cannot undo; the incoming tab springs
in from 12 dp and the tab being left is parked at once ([PERFORMANCE.md](PERFORMANCE.md#navigation-and-back)).
Tapping the active tab again pops it to its root. The bar is a bottom bar in a Compact window and a rail from Medium
up, chosen from the window size class, so a phone in split screen gets the bar.

### List and detail share the window

A destination registers as `NavPane.List`, `NavPane.Detail` or, by default, `NavPane.Single`:

```kotlin
entry<OrdersKey>(pane = NavPane.List) { OrdersRoute() }
entry<OrderKey>(pane = NavPane.Detail) { key -> OrderRoute(key.orderId) }
```

From Expanded width the list and its detail sit side by side, and a list alone sits beside a "nothing selected"
pane; below that they are separate screens. The back stack, the keys and the features are the same either way, so
nothing has to be kept in sync when the window is resized or a foldable opens. Details opened one after another stack
up in the detail pane, and Back steps through them before it closes the pane.

### Deep links

Links arrive at `LinkActivity`, which has no history and no UI, and become typed keys: each feature contributes a
`DeepLinkRoute` that maps a parsed `DeepLink` (path segments and query parameters) to an `AppNavKey`, or null. Custom
scheme and verified `https` links follow one set of rules. An unknown link opens the app as it was and never crashes
it, and links carry ids only, like keys. `PendingDeepLink` holds the key until the host can open it.

## Data and the network

A repository calls `NetworkClient.execute(request)` and gets an `AppResult<NetworkResponse>`; it never sees Ktor.
`NetworkRequest` describes a call independently of the HTTP library: method, path, query, headers, a JSON body or
multipart parts, whether it needs auth, and optional cache and queue behaviour.

### Failures are classified, not thrown

`KtorNetworkClient` returns failures as `AppResult.Failure` with an `AppError`:

- `AppError.Offline` when no answer came and no network that claims internet access is up: the offline state.
- `AppError.Timeout` when no answer came while online (wrong base URL, DNS, server down): a different message,
  because "check your connection" would be wrong.
- `AppError.Http` when the server refused: its `status`, the backend's own `code`, the server's `message`, any
  per-field `fieldErrors`, and the wait a `Retry-After` header asked for.
- `CancellationException` is always rethrown, so leaving a screen cancels its request.

`AppError.Http.message` is only ever what the server said. The app's own wording lives in resources:
`AppError.toUiText(fallback)` in `:core:ui` returns the server's message when there is one, a sentence for offline or
unreachable, and otherwise the fallback.

### Retries and shared reads

A request with no answer, or a 408, 429 or 5xx, is sent again up to twice, but only for GET, PUT and DELETE, which are
safe to repeat: a POST sent twice can create two orders. The wait doubles with random jitter, so clients that failed
together do not return in the same second, and a `Retry-After` is followed up to eight seconds (`RetryPolicy`,
`NetworkConfig.maxRetries`). Identical GETs made while one is in flight wait for it; the shared call runs in the
application scope, so the caller that started it leaving does not cancel it for the rest.

### Caching

GET responses go through OkHttp's HTTP cache (50 MB), as the server's `Cache-Control`, `ETag` and `Last-Modified`
headers say: fresh responses are served without a call and stale ones revalidated, so an unchanged resource costs a
304. It is cleared on sign-out. There is no second cache: data the app must show offline, whatever the headers say,
belongs in a repository backed by the database.

### Security

- The access token goes only to the configured base URL's host, never to a full URL elsewhere or a request marked
  `requiresAuth = false`.
- Release builds allow HTTPS only and trust only the system's certificate authorities; debug builds also trust user
  certificates (for a proxy) and allow plain HTTP to the development machine.
- `NetworkConfig.certificatePins` pins public keys per host; pin only with a backup key already issued.
- Every request sends `Accept-Language`, so the server's messages come back in the app's language.

### Response shape, auth and refresh

`ResponseUnwrapper` decides where the payload and the error sit: `PassthroughUnwrapper` (the default) takes the whole
body, `EnvelopeUnwrapper` handles `{ "data": …, "error": … }`. `NetworkJson` ignores unknown keys, so a backend adding
a field breaks no older app.

Every request reads the tokens from `AuthTokenStore` as it is sent; there is no token cache to outlive a session. On a
401, `SessionTokens` refreshes once, however many requests got the 401, and each is sent again. Only a refresh the
server refuses with 401 ends the session; no answer, a 408, a 429 or a 5xx leave it in place, because signing someone
out in a tunnel would be wrong. A refresh that finishes after a sign-out changes nothing.

### Offline queue

A mutation can ask to be queued when it cannot be sent. It goes to `queue.db`, tagged with the account that made it,
and carries an `Idempotency-Key` from its first attempt. One WorkManager worker sends the queue oldest first when a
network is up; refusals are kept with the server's answer; WorkManager starts the first time something is queued, not
at every launch.

### WebSocket

`AppWebSocket` reconnects with exponential backoff from one second to thirty, plus up to 40 % jitter, so many clients
do not reconnect in the same second after a server restart.

### Network inspector

`:core:devtools` binds a `NetworkObserver` that hands each exchange to the in-app inspector; `:core:network` knows only
the interface. `Authorization`, cookies, and token and password fields are redacted when an exchange is recorded, not
when it is shown, and the bodies of sign-in and refresh calls are not kept. A build without the inspector records
nothing.

## Storage and the session

`:core:datastore` keeps one Preferences file per concern, because every write rewrites the whole file: a recent search
would otherwise wait behind a token refresh. `DataStoreFiles.preferences(name)` makes one; call it once per file, from a
`@Singleton` provider, because two instances of one file corrupt it. `@SettingsDataStore` holds what belongs to the
device and outlives any account: theme, onboarding progress, the analytics choice.

Tokens are encrypted before they reach disk: `KeystoreCipher` uses AES-GCM under a key in the Android Keystore. If
decryption fails (a backup restored to another device), it returns null and the person is treated as signed out.
Whether a session exists is read from `AuthTokenStore.isAuthenticated`, without unsealing the tokens.

### Sign-out clears everything that belongs to the person

A class holding per-user data implements `SessionScopedStore` and binds itself `@IntoSet`: the database, the HTTP
cache, in-flight shared requests, the push registration, the WebSocket, flag overrides and the analytics id among
them. `SessionCoordinator` clears every store once per session end, however it ended, in the application scope, and
remembers whose data is on the device, so a new person signing in over a session that never signed out finds nothing
of the last one. One store failing is logged and does not stop the others. Its `state` moves to a new `SignedIn`
generation with every new owner, and the navigation host starts fresh back stacks.

## App-wide services

**Feature flags.** A flag is declared once as a `Flag` with its key and default. Code reads `featureFlags[Flag.X]` or
follows `featureFlags.observe(Flag.X)`, never a string key. Reading is synchronous and always returns a value, and
`refresh()` runs once at start-up, so a slow fetch never blocks a screen.
Remote Config starts from the same defaults, so the app behaves the same before the first fetch and when it fails.

**Notifications.** Channels are the `NotificationChannelSpec` enum, created at start-up off the main thread, so people
can mute a category before its first message. The permission is asked at the end of onboarding and offered again in
settings. The push token is handed to `PushTokenSink` only when it changes, and sign-out unregisters it.

**Connectivity.** `NetworkMonitor.isOnline` is one shared registration with the system and counts networks that claim
internet access. It is a hint for the UI and a trigger for queued work, never a gate: every request is attempted and
its failure classified from what happened.

**Logging.** `Log` in `:core:common` writes through the `Log.logger` the application installs at start-up
(`AndroidLogger`, verbose only in debug builds); a library never reads `BuildConfig` to decide what to log.

## Copy and languages

Every string a person reads is a resource, and `hardcoded-copy` fails the build on copy written into Kotlin in the
features, the app, `:core:designsystem` and `:core:ui`. A ViewModel produces `UiText.of(R.string.x)` (`UiText.plural`
for a count), resolved when it is drawn, so a language change reaches it without the ViewModel knowing. If the project
was generated with more languages, each module has a `values-xx/strings.xml` beside `values/`, and lint fails a build
with a string missing from one.
`AppLocales.supported` lists the languages for the in-app picker.

## Every form factor

The same app runs on a phone, a foldable, a tablet, a Chromebook, a desktop window and a car. No activity is locked
to an orientation or a size, and lint fails the build if one is (`LockedOrientationActivity`,
`SourceLockedOrientationActivity`, `NonResizeableActivity`). The manifest handles size, layout, orientation and
keyboard changes itself, so a resize, fold or rotation lays the screen out again instead of recreating the activity.

Layout is decided from the window, never the device or its orientation. `AppTheme.windowClass` holds the window size
classes, and `AppTheme.containerClass` those of the pane a screen is in, computed from the window the app is actually
in, which changes when it is resized, split or unfolded:

| Width | From | Typically |
|---|---|---|
| Compact | 0 dp | a phone held upright, a narrow split-screen pane |
| Medium | 600 dp | a phone held sideways, a small tablet, an unfolded foldable |
| Expanded | 840 dp | a tablet held sideways, a desktop window |
| Large, ExtraLarge | 1200 dp, 1600 dp | a large tablet or a wide desktop window |

`AppTheme.layout` turns the class into measurements: `gutter` (the margin to the window's edge, also
`AppTheme.spacing.gutter`), `formMaxWidth`, `readableMaxWidth` and `contentMaxWidth` (where content stops growing),
`listPaneWidth`, and `gridMinCardWidth` and `gridMinTileWidth` for grid cells. `AppTheme.posture` says whether a
foldable is half open, and `AppTheme.input` whether there is a mouse, a keyboard or touch.

What adapts, and how: a bottom bar below Medium width and a rail from Medium up (a rail widens to clear a camera
cutout); list and detail side by side from Expanded width; `AppTwoPane` puts two halves of a screen side by side from
Expanded width, and on a short window of Medium width or more; grids (`LazyVerticalGrid` with `GridCells.Adaptive`)
fit as many columns as `gridMinCardWidth` allows; sheets and dialogs stop at `sheetMaxWidth`; `AppScaffold` centres
content at `contentMaxWidth` and pads both sides for display cutouts.

To check a screen without other devices, change the emulator's display: `adb shell wm size 1600x2560` with
`adb shell wm density 320` is a tablet, and `adb shell wm size reset` with `adb shell wm density reset` undoes it.

## Build

`build-logic/` holds one convention plugin per kind of module (`com.example.finalafter.crui2.android.library`,
`com.example.finalafter.crui2.android.feature` and so on), so a module's build file lists only its dependencies. SDK levels, the
Kotlin and Java targets and the Compose setup are decided once. `AppConfig.kt` in `build-logic` holds the application
id, version and backend URLs per environment, and it is the only place the build reads them. Every version is in
`gradle/libs.versions.toml`, stable releases only.

### Build guards

These run as part of `./gradlew build` and fail it:

| Task | Checks |
|---|---|
| `verifyModuleDependencies` | The tier rules above, and the pure-Kotlin modules. |
| `verifyApiPurity` | A compact project's `api` packages stay splittable into modules of their own. |
| `verifyCodeRules` | The forbidden patterns, one rule each in `build-logic/convention/src/main/kotlin/com/example/finalafter/crui2/buildlogic/guard/`; [DESIGN-SYSTEM.md](DESIGN-SYSTEM.md#the-rule-index) lists them with their fixes. A justified exception is `// allow(<rule-id>): <reason>` on the line above; a marker with no reason, or that silences nothing, fails too. |
| `verifyManifestRules` | No orientation or size lock, the configuration changes handled in place, Back through `OnBackInvokedCallback`. |
| `checkComposeStability` | No composable parameter in `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:navigation:shell` or a feature module is compared by identity, beyond `config/compose-stability-baseline.txt`, where each entry carries its reason. |
| `verify<Variant>CarriesNoDevTooling` | A shippable build (staging, prod and playstore releases) depends on no dev tooling module, directly or through another. |
| `dependencyGuard` | The prod release's libraries, transitive ones included, match the list the first build writes under app/dependencies, which is committed. A deliberate change fails it until `./gradlew dependencyGuardBaseline` rewrites the list, reviewed and committed with the change. |
| `spotlessCheck` | ktlint's Android Studio style over every Kotlin and Gradle file (`./gradlew spotlessApply` fixes it), and the Compose Rules for ktlint; `.editorconfig` names each rule turned off and why. |

Compose lint runs as errors in every Compose module; the ids are listed in `build-logic`'s `Compose.kt`.

### Flavours are only on the application module

Four environments (dev, staging, prod, playstore), each debug and release, except `playstoreDebug`. Library modules
have no flavours: a library with four would build eight variants instead of two, and switching from devDebug to
stagingDebug would rebuild every library. What varies by environment is configuration, injected through Hilt from the
app. Every build but a prod or playstore release adds its variant to the version name (`1.0.0-stagingRelease`), so a
screenshot or a crash report says which backend it used.

### No library has a BuildConfig

A library reading `BuildConfig.DEBUG` behaves differently depending on which variant compiled it, and the difference
often shows only in release builds. The app installs the logger (`Log.logger = AndroidLogger(…)`) at start-up
and builds `NetworkConfig` from its own `BuildConfig` and injects it
instead.

### Dev tooling is absent from shippable builds

The inspector and what plugs into it are `debugImplementation` and `"devImplementation"` dependencies of the app, so
devDebug, devRelease, stagingDebug and prodDebug carry them and stagingRelease, prodRelease and playstoreRelease have
no class, resource or manifest entry of theirs. The root reaches the tooling through `DevTooling` in `:core:ui`,
injected as an `Optional`; empty, it composes nothing.

### Release artifacts and signing

`./gradlew :app:distProdRelease` copies the APKs and the bundle into `build/outputs/dist/<variant>/`, each named with
the variant, ABI, version and build time, with a `checksums.sha256` beside them. Per-ABI APKs are built only with
`-PabiSplits=true`: Play already delivers one ABI per device from the bundle. Without `keystore.properties`, or without
a block for a flavour, the build signs with the debug key and says so on every run, so a fresh clone builds without
secrets and nobody finds out at upload time that a release was debug-signed. Release builds run R8 in full mode with
resource shrinking; [PERFORMANCE.md](PERFORMANCE.md#start-up-profiles-and-size) covers keep rules and profiles.

### Room schemas

The Room convention exports each database's schema to `schemas/` in its module. Commit those files: a migration test
compares against them, and without them a schema change cannot be reviewed.

## Testing

- Dispatchers are injected and no class uses `Dispatchers.IO` directly, so `runTest` controls the clock and no test
  waits in real time. `MainDispatcherRule` replaces `Dispatchers.Main`; without it a ViewModel test hangs. Inside
  `runTest`, `testDispatchers()` gives the class under test its `AppDispatchers` on the same clock.
- Repositories are interfaces, so a ViewModel test injects a fake and never opens a socket; `FakeNetworkMonitor`
  switches between online and offline. Each fake lives once. A fake other modules' tests use sits in the
  `src/testFixtures` of the module that owns its interface (a data module's beside the interface in its `api` package;
  `FakeAuthTokenStore` and `FakePreferencesDataStore` in `:core:datastore`), and a test reaches it with
  `testImplementation(testFixtures(project(":data:x")))`; the modular layout moves a domain's fixtures to its api
  module with the interface. A double scripted for one module's tests, such as a network client that answers that
  module's endpoints, stays in those tests. Fakes, never mocks.
- Unit tests run against the real Android stubs, which throw rather than return defaults: code that needs Android runs
  under Robolectric, and everything else sits behind a seam such as `Log`'s `AppLogger`.
- Compose tests run on the JVM with Robolectric. Every `@Preview` becomes a screenshot test through Roborazzi, which
  also runs Google's Accessibility Test Framework over what it drew
  ([DESIGN-SYSTEM.md](DESIGN-SYSTEM.md#previews-are-render-tests)), and `RecompositionCounter` with `framesUntilIdle`
  proves a journey composes nothing while it moves ([PERFORMANCE.md](PERFORMANCE.md#state-reads-per-frame)).
- `NavKeyRegistrationTest` fails on a destination with no serializer.

`feature/sample` and `data/sample` are the worked example of all of these.
