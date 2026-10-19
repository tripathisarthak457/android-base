# Architecture

How this project is put together and why. The code's own KDoc covers the details of each class;
this document covers the decisions that span several of them.

Contents:

1. [Modules](#modules)
2. [MVI](#mvi)
3. [Navigation](#navigation)
4. [Data and the network](#data-and-the-network)
5. [Storage and the session](#storage-and-the-session)
6. [App-wide services](#app-wide-services)
7. [The design system](#the-design-system)
8. [Every form factor](#every-form-factor)
9. [Build](#build)
10. [Testing](#testing)

## Modules

```
        ┌──────────────┐   ┌──────────────┐
        │    :app      │   │  :catalog    │   composition roots: wiring, no logic
        └──────┬───────┘   └──────┬───────┘
               │                  │
        ┌──────▼───────┐          │
        │  :feature:*  │          │           screens and their ViewModels
        └──────┬───────┘          │
               │                  │
        ┌──────▼───────┐          │
        │   :data:*    │          │           one business domain each
        └──────┬───────┘          │
               │                  │
        ┌──────▼──────────────────▼───────┐
        │            :core:*              │   infrastructure
        └─────────────────────────────────┘
```

| Tier         | May depend on          |
|--------------|------------------------|
| `:app`       | anything               |
| `:feature:*` | `:core:*`, `:data:*`   |
| `:data:*`    | `:core:*`              |
| `:core:*`    | `:core:*`              |

The `verifyModuleDependencies` task checks this table on every build, so a wrong edge fails the
build and is not left for code review to catch. The rules that matter most are the two that forbid
siblings: no `:feature:` module depends on another, and no `:data:` module depends on another. A
`:feature:cart → :feature:catalog` edge looks harmless when it is added. Over time, edges like it
mean no feature can be built or tested on its own, and they tend to arrive as a one-line
convenience inside an unrelated change.

When two features need the same data, both depend on the `:data:` module that owns it. When two
features need to react to each other, one writes state that a `:data:` module exposes as a
singleton and the other observes it.

### Where a feature lives

```
data/orders/       DTO · mapper · API service · repository interface and implementation   no Compose
feature/orders/    contract · ViewModel · screen · navigation key                          no network
```

The split lets the data half compile in parallel with the feature half instead of before it. It
also makes cross-feature UI possible without a feature edge: a cart badge on the home screen reads
the cart repository, not the cart feature.

### What each core module holds

| Module | Holds |
|---|---|
| `:core:model` | Plain Kotlin types shared by data and UI, such as `Paged`. A JVM module, no Android. |
| `:core:common` | `MviViewModel` and the UI contracts, `AppResult`, `LoadState`, `UiText`, `NetworkMonitor`, `SessionController`, `AppLogger`. |
| `:core:coroutines` | Dispatcher qualifiers and the application `CoroutineScope`. |
| `:core:datastore` | The settings and session DataStores, the token store and the Keystore cipher. |
| `:core:designsystem` | Tokens, theme, components, icons and motion. No Material. |
| `:core:ui` | Composables that need more than the design system, such as network images, paging lists, forms and the in-app browser. |
| `:core:navigation` | `AppNavKey`, the navigator, the nav host and the tab shell. The only module that names Navigation 3. |
| `:core:testing` | `MainDispatcherRule`, `FakeNetworkMonitor` and the test libraries, as one dependency. |
<!-- <opt:network> -->
| `:core:network` | The HTTP client and everything around it. The only module that names Ktor. |
<!-- </opt:network> -->
<!-- <opt:analytics> -->
| `:core:analytics` | `AnalyticsTracker` and `CrashReporter`, with no-op or Firebase implementations. |
<!-- </opt:analytics> -->
<!-- <opt:flags> -->
| `:core:flags` | `Flag` declarations and the `FeatureFlags` source. |
<!-- </opt:flags> -->
<!-- <opt:push> -->
| `:core:notification` | Notification channels, the permission check and push-token registration. |
<!-- </opt:push> -->
<!-- <opt:media> -->
| `:core:media` | The photo and video picker, runtime permissions and image compression. |
<!-- </opt:media> -->
<!-- <opt:database> -->
| `:core:database` | The app's own Room database, separate from the network cache. |
<!-- </opt:database> -->
<!-- <opt:devtools> -->
| `:core:devtools` | The in-app network inspector and environment badge; dev and debug builds only (below). |
<!-- </opt:devtools> -->
<!-- <opt:perfhud> -->
| `:core:perfhud` | The live performance overlay beside the badge, its Perf tab, log, report and score; dev and debug builds only ([PERFORMANCE-OVERLAY.md](PERFORMANCE-OVERLAY.md)). |
<!-- </opt:perfhud> -->

## MVI

Each screen has three types and one ViewModel built on a shared base class.

```kotlin
@Immutable
data class OrdersState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<Order> = emptyList(),
) : UiState

sealed interface OrdersEvent : UiEvent { … }
sealed interface OrdersEffect : UiEffect { … }
```

- **State** is everything the screen renders, and it is complete. Rendering never needs a value
  that is not in it.
- **Events** are everything the user or the system can do to the screen.
- **Effects** happen once and are not state: navigate, dismiss the keyboard, open the dialler. An
  effect stored in state fires again after rotation.

The screen is a stateless function, `OrdersScreen(state, onEvent)`. A route composable connects it
to the ViewModel through `MviScreen`, which collects state with the lifecycle and hands effects to
a callback. Previews and UI tests call the screen and never need a ViewModel.

### Events are queued, not launched

`MviViewModel.onEvent` puts each event on a channel, and a single coroutine handles them in order.
The alternative, `viewModelScope.launch { handleEvent(event) }` for each call, starts a coroutine
per event. Two events that both read and then write the state can then interleave between the read
and the write, and one update is lost. That only happens under fast input, which makes it very
hard to reproduce.

The cost is that a slow handler delays the next event. That is usually what you want, because the
next event often depends on what this one writes. Work that should not hold the queue goes through
`launchWork { }`, and work where only the newest request matters, like search-as-you-type, goes
through `launchLatest(key)`, which cancels the previous job with the same key.

### Effects are delivered once

Effects go through a buffered `Channel`, not a `SharedFlow`, so an effect sent while nothing is
collecting waits instead of being dropped. `MviScreen` collects them on `Dispatchers.Main.immediate`
under `repeatOnLifecycle(STARTED)`. A navigation emitted while the app is in the background is held
until the user returns. Collecting on `Main.immediate` means an effect is handled in the frame it
arrives, so a lifecycle stop in between cannot lose it.

### Messages and errors

`showMessage(text, kind)` is the effect for transient notes such as "Saved" or "Could not refresh".
An exception that escapes an event handler goes to `onError`, which logs it and shows a generic
error message. A handler that expects a failure should turn it into state (usually
`LoadState.Error`) and not rely on `onError`.

### LoadState, not a boolean and a nullable

`isLoading: Boolean` plus `error: String?` allows four combinations, of which three make sense.
The fourth, loading and failed at the same time, shows up as a spinner drawn over an error message.
A sealed `LoadState` cannot express it.

The distinction that matters most in practice is `Loading` versus `Refreshing`. `Loading` has
nothing to show yet and draws a skeleton. `Refreshing` already has content on screen and must keep
it. Treating them as the same state is why pull-to-refresh often blanks the list it was refreshing.

### Surviving process death

Loaded data is fetched again after process death and is not saved. What the user typed is saved:
`persistState(savedStateHandle, save, restore)` in `init` writes chosen fields to the
`SavedStateHandle` whenever they change and restores them on the next start.

## Navigation

Feature modules never import a navigation library. They implement `AppNavKey` and contribute two
things through Hilt `@IntoSet`: the screens (`navGraph { }`) and the key serializers
(`navKeys { }`).

```kotlin
@Serializable
data class OrderDetailKey(val orderId: Long) : AppNavKey

@Provides @IntoSet
fun ordersNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
    entry<OrderDetailKey> { key -> OrderDetailRoute(key.orderId, navigator) }
}

@Provides @IntoSet
fun ordersNavKeys(): SerializersModule = navKeys {
    subclass(OrderDetailKey::class, OrderDetailKey.serializer())
}
```

Adding a screen touches no file outside its module. There is no central sealed `Route` class and
no `when` in `:app` to extend, so two people adding screens in the same week do not conflict.

`AppNavHost` is the only file that uses Navigation 3 directly. Replacing the library means changing
it and `NavTransitions`.

ViewModels calling `AppNavigator` do not touch the back stack themselves. The navigator sends a
`NavCommand` (`Navigate`, `Up`, `ResetTo`, `PopTo`) that the host applies. A ViewModel can then
navigate without holding a reference to anything in the composition, and tests can assert on the
commands.

### Keys carry ids, not models

A key is serialized into the saved-state bundle. A key that holds a whole object makes the bundle
larger and goes stale once the app is in the background. Values that look like content, such as a
title for the app bar, are only a placeholder to draw before the fetch returns. The screen still
fetches.

The id reaches the ViewModel through Hilt assisted injection, at construction. Every entry has its
own ViewModel store, so each open screen gets its own ViewModel, and Retry can use the injected id
even when the first load failed.

### Registering the serializer is easy to forget

Without `navKeys { }` the app works normally until the system kills it in the background. When the
user returns, the app opens at the start destination and not where they were.

### The entry point is derived, not navigated to

`AppDestinations` defines the tabs and the start destination; nothing else names one. What the app
opens on is a function of the settings and session that `MainActivity` has just read:

```
onboarding not finished  ->  the walkthrough, no tab bar
not signed in            ->  sign in, no tab bar
otherwise                ->  the tabs, starting at the first
```

Finishing onboarding is `settingsStore.setOnboardingCompleted(true)`, and signing in is a token
reaching the store. The UI follows from the same emission that updates the theme. The imperative
alternative, calling `navigate()` at startup, has to handle the flag changing for some other
reason, and each screen that could observe the change would handle it differently.

Sign-out works the same way in reverse: `SessionCoordinator` clears every `SessionScopedStore`
first and only then announces the sign-out, so the sign-in screen never appears while the previous
user's data is still on disk.

### Each tab keeps its own back stack

`ShellState` holds one stack per tab. With a single stack and tab markers in it, every position
would need its own answer to "what does Back do here", and some of those answers end up wrong.
With separate stacks the answer is simple: Back inside a tab pops that tab, and Back at a tab's
root is handled by the shell.

Each tab's entries are decorated separately, with saved state and a ViewModel store per entry, and
a tab stays decorated while another is showing. Navigation 3 treats an entry that leaves the list
it was given as popped and discards its state. If the display were handed a different tab's list
on every switch, the tab being left would lose its scroll position, query and loaded data. Keeping
every tab decorated means leaving a tab only hides it.

A tab switch crossfades instead of sliding. A slide between tabs looks like forward navigation, and
Back would then not undo it. The tab bar slides away over pushed screens instead of being removed
from the layout: tab roots reserve the bar's height and pushed screens are full height, so nothing
resizes during a transition.

Tapping the active tab again pops it to its root, as it does in most apps.

The bar is a bottom bar in a Compact-width window and a navigation rail beside the content from
Medium width up. The choice is made from the window size class, so a phone in split screen gets
the bar and a phone held sideways gets the rail.

### List and detail share the window

A destination registers as `NavPane.List`, `NavPane.Detail` or, by default, `NavPane.Single`:

```kotlin
entry<OrdersKey>(pane = NavPane.List) { OrdersRoute() }
entry<OrderKey>(pane = NavPane.Detail) { key -> OrderRoute(key.orderId) }
```

From Expanded width, `ListDetailSceneStrategy` draws a list and the detail on top of it side by
side, and a list on its own beside a "nothing selected" pane. Below that width the same entries
are separate screens. The back stack, the keys and the features are the same either way, so a
feature does not know which it is in and nothing has to be kept in sync when the window is resized
or a foldable is opened.

Details opened one after another from the same list stack up: the newest is shown, and Back steps
through the earlier ones in the detail pane before it closes the pane. A detail replaces the pane
without a crossfade, because a popped entry's ViewModel store has been cleared and drawing it
through a fade-out would create a new ViewModel for a screen that is gone.

<!-- <opt:deeplink> -->
### Deep links

`DeepLinkResolver.resolve(uri)` turns a link into an `AppNavKey`, or null. It handles both the
custom scheme and verified `https` links with one set of rules: for a custom-scheme link the first
path token is in the host, for an `https` link it is the first path segment. An unknown link
returns null and the app opens normally; it never crashes on a bad link. Links carry ids only,
like keys.
<!-- </opt:deeplink> -->

<!-- <opt:network> -->
## Data and the network

A repository calls `NetworkClient.execute(request)` and gets an `AppResult<NetworkResponse>`. It
never sees Ktor. `NetworkRequest` describes a call independently of the HTTP library: method,
path, query, headers, a JSON body or multipart parts, whether it needs auth, and optional cache and
queue behaviour.

### Failures are classified, not thrown

`KtorNetworkClient` catches failures and returns them as `AppResult.Failure`, keeping enough detail
for the UI to choose its message:

- `isOffline = true` when the device has no validated network. The screen shows its offline state.
- An I/O failure while online (wrong base URL, DNS, server down) is also a failure without a
  status code, but with a different message, because telling the user to check their connection
  would be wrong.
- An HTTP error keeps its `code`, the raw body, the server's message and any per-field validation
  errors.
- `CancellationException` is always rethrown, so cancelling a screen cancels its request.

`Failure.message` is only ever what the server said. The app's own wording lives in string
resources, so it can be translated: `failure.userMessage(fallback)` returns the server's message
when there is one, and otherwise a sentence for what went wrong — offline, unreachable, or queued
to send later — or the fallback.

### Retries

A request that got no answer, or a 408, 429 or 5xx, is sent again up to twice — but only for GET,
PUT and DELETE, which are safe to repeat. A POST sent twice can create two orders. The wait doubles
each time with random jitter, so clients that failed together do not all come back in the same
second, which is how a short outage becomes a long one. A `Retry-After` header is followed, up to
eight seconds. `RetryPolicy` holds the rules, and `NetworkConfig.maxRetries` changes the count.

### Sharing identical reads

Identical GETs made while one is already in flight wait for that one instead of sending their own.
The shared call runs in the application scope, so the caller that started it leaving its screen
does not cancel it for everyone else.

### Security

- The access token is sent only to the host in the configured base URL. A full URL to another host
  (a third-party API) never carries it, and neither does any request marked `requiresAuth = false`.
- Release builds allow HTTPS only and trust only the system's certificate authorities
  (`res/xml/network_security_config.xml`). Debug builds also trust user-installed certificates, so
  a proxy such as Charles or Proxyman can show the traffic, and allow plain HTTP to a backend on
  the development machine (`10.0.2.2` from the emulator).
- `NetworkConfig.certificatePins` pins public keys per host. Pin only with a backup key already
  issued: a rotated certificate that matches no pin locks every installed copy out until an update
  ships.
- Every request sends `Accept-Language`, so the server's own messages come back in the app's
  language, and a `User-Agent` naming the app and its version.

### Caching

Two caches, for two jobs.

The HTTP cache (OkHttp's, 50 MB in the cache directory) follows the server's `Cache-Control`,
`ETag` and `Last-Modified` headers. A resource the server marks cacheable is served without a call
while it is fresh, and revalidated with a conditional request when it is not, so an unchanged
resource costs a 304 and no body. It is cleared on sign-out. Nothing in the app has to ask for it:
it is as good as the headers the backend sends.

### Response shape

`ResponseUnwrapper` decides where the payload and the error message sit in a response.
`PassthroughUnwrapper`, the default, treats the whole body as the payload. `EnvelopeUnwrapper`
handles APIs that wrap everything in `{ "data": …, "error": … }`. Changing the binding in
`NetworkModule` changes every call at once. `NetworkJson` ignores unknown keys, so a backend adding
a field does not break older app versions.

### Auth and token refresh

Every request reads the session's tokens from `AuthTokenStore` as it is sent, through an `HttpSend`
interceptor; there is no token cache in the client to outlive a session. On a 401, `SessionTokens`
refreshes once, however many requests got the 401 at the same time, and each of them is sent again
with the new token. The refresher uses a plain client of its own, so a refresh cannot trigger
another refresh.

Only a refresh the server refuses with 401 ends the session. A refresh that got no answer, a 408,
a 429 or a 5xx leaves it in place: the refresh token is still good, and signing someone out because
they went through a tunnel would be wrong. A refresh that finishes after a sign-out, or after
someone else signed in, changes nothing, because the store swaps tokens only if they are still the
ones the refresh started from. When the session does end, `SessionEvents.expired` fires and
`SessionCoordinator` takes the same path as a manual sign-out.

Requests marked `requiresAuth = false` (sign in, register, refresh) are sent without a token, and a
full URL to another host never receives one.

### Caching

GET responses are cached by OkHttp's HTTP cache, as the server's `Cache-Control` headers say; there
is no second cache in the app. Data the app must show offline, or keep across launches whatever the
headers say, belongs in a repository backed by the database.

<!-- <opt:offlinequeue> -->
### Offline queue

A mutation can ask to be queued when it cannot be sent. It goes to `queue.db`, a database of its
own that survives sign-out, tagged with the account that made it, and is sent only with that
account's session. It carries an `Idempotency-Key` from the first attempt, so a backend that honours
the header applies it once even if the first attempt arrived before the connection dropped.

One WorkManager worker sends the queue, oldest first, when the device has a network. A refusal is
kept with the server's answer, so a screen can show what was refused and why; 401, 408, 426 and 429
are momentary and retried later, on a timer that grows. Entries too old or tried too often are
given up. WorkManager starts the first time something is queued, not at every launch: the manifest
removes its start-up initializer and the application supplies its configuration.
<!-- </opt:offlinequeue> -->

<!-- <opt:websocket> -->
### WebSocket

`AppWebSocket` reconnects on its own with exponential backoff: it starts at one second, is capped
at thirty, and adds up to 40% random jitter. The jitter keeps many clients from reconnecting in the
same second after a server restart.
<!-- </opt:websocket> -->

<!-- <opt:devtools> -->
### Network inspector

`:core:devtools` binds a `NetworkObserver` that hands each exchange to `DevToolsLog`, which the
in-app inspector reads; `:core:network` knows only the observer interface. Headers such as
`Authorization` and `Cookie`, and token and password fields in JSON bodies, are redacted when an
exchange is recorded, not when it is shown, so a token never sits in memory in the log; the bodies
of sign-in and refresh calls are not kept at all. Bodies are truncated and the log keeps a fixed
number of entries. A build without the inspector has no observer, so its clients record nothing.
<!-- </opt:devtools> -->
<!-- </opt:network> -->

## Storage and the session

`:core:datastore` keeps one Preferences file per concern rather than one shared file, because every
write rewrites the whole file: a recent search would otherwise wait behind a token refresh.
`DataStoreFiles.preferences(name)` makes one; call it once per file, from a `@Singleton` provider.
`@SettingsDataStore` holds what belongs to the device and outlives any account: theme, onboarding
progress, the analytics choice.

Tokens are encrypted before they reach disk. `KeystoreCipher` uses AES-GCM with a key held in the
Android Keystore and stores the IV at the start of each value. If decryption fails, for example
after a backup was restored to a different device and the key no longer matches, it returns null.
The app then treats the person as signed out, clears what the last session left, and does not
crash.

### Sign-out clears everything that belongs to the person

Any class holding per-user data implements `SessionScopedStore` and binds itself into the set with
`@IntoSet`: the database, the HTTP cache, in-flight shared requests, the push registration, the
WebSocket, flag overrides and the analytics user id among them. `SessionCoordinator` clears every
store in that set once per session end, however it ended, and two sign-outs at once clear each
store once. It also remembers whose data is on the device across launches, so a new person signing
in over a session that never signed out finds nothing of the last one; the same person signing back
in keeps their data. One store failing to clear is logged and does not stop the others.

The coordinator runs in the application scope, so leaving the screen that started a sign-out does
not cancel it halfway. Its `state` moves to a new `SignedIn` generation with every new owner, and
the navigation host resets its back stacks when the session ends.

## App-wide services

<!-- <opt:flags> -->
### Feature flags

A flag is declared once as a `Flag` subclass with its key and default value. Code reads it with
`featureFlags[Flag.X]`, or follows it with `featureFlags.observe(Flag.X)`, and never with a string
key. Reading is synchronous and always returns a value, and `refresh()`, called once at start-up,
fetches new values, so a slow or failed fetch never blocks a screen. Overrides set in the
inspector's Flags tab win over every source until sign-out.
<!-- <opt:remoteconfig> -->
The Remote Config implementation starts from the same defaults, so the app behaves the same before
the first fetch and when a fetch fails. It activates changes the console publishes while the app is
open, and a debuggable build fetches on every start.
<!-- </opt:remoteconfig> -->
<!-- </opt:flags> -->

<!-- <opt:push> -->
### Notifications

Notification channels are declared as the `NotificationChannelSpec` enum and created when the app
starts, off the main thread, before any notification arrives. People can see and mute each category
in system settings before the first message in it. The permission is asked for at the end of
onboarding and offered again in settings (`rememberNotificationPermission()`). The installation id
is handed to `PushTokenSink` only when it changes, and sign-out unregisters, so the next person on
the phone never receives the last one's notifications. A tap opens the link the push carries, or
the app as it was left; on the lock screen a notification shows only the app's name.
<!-- </opt:push> -->

### Connectivity

`NetworkMonitor.isOnline` is one shared registration with the system, whatever the number of
screens and requests asking, and counts networks that claim internet access
(`NET_CAPABILITY_INTERNET`). It is a hint for the UI and a trigger for the offline queue, never a
gate: every request is attempted, and a failure is classified from what actually happened.

## The design system

`:core:designsystem` depends on `androidx.compose.foundation` and `androidx.compose.ui`. The
project has no `material` or `material3` artifact, and importing one fails the build (see
`verifyCodeRules`).

The module therefore supplies what Material would otherwise provide: the indication every
clickable draws (`AppIndication`), the inherited content colour and text style, the text selection
colours, and every component and icon.

Screens read tokens through one accessor:

```kotlin
AppText(text = order.title, style = AppTheme.typography.titleLarge, color = AppTheme.colors.contentPrimary)
Spacer(Modifier.height(AppTheme.spacing.lg))
```

Only `Palette.kt` names colours by hue. Everything else uses roles such as `contentPrimary` and
`surfaceRaised`. A rebrand is a change to one file, and dark theme is a second `AppColors`
instance.

### Design styles

`AppDesignStyle` groups decisions that need to agree with each other into four named sets, read
through `AppTheme.style`:

- corner radii and the shapes of buttons and chips
- how cards and fields are drawn, and border weight
- the tab bar and label casing
- the surface tone (`SurfaceTone`: cool greys, brand-tinted, paper or cream)
- the type voice (`TypeVoice`)

Components take their defaults from the style instead of a constant, so
`AppTheme(designStyle = AppDesignStyle.Social)` restyles every screen, and any single component
can still be given its own `shape`.

The styles are starting points. A project is expected to move its tokens and components away from
the style it started with; what the style provides is a first version that is consistent.
`PaletteContrastTest` checks every text colour against its background in all four styles and both
themes against WCAG AA, so a change to a colour or surface tone that breaks contrast fails a test.

### Motion

`AppMotionStyle` (Standard, Bouncy, Calm, Snappy) sets the durations and easing read through
`AppTheme.motion`. Components animate `graphicsLayer` properties (alpha, offset, scale) rather
than size, so an animation never moves the elements around it. `rememberAppTransitions()` turns
transitions off when the system's remove-animations setting is on.

### Copy and languages

Every string a user reads is a resource, and the build fails on copy written into Kotlin in the
features, `:app`, `:core:designsystem` and `:core:ui`. A ViewModel produces `UiText.of(R.string.x)`
(or `UiText.plural` for a count), which is resolved when it is drawn, so a language change reaches
it without the ViewModel knowing. Components take their default copy from resources too, so an app
in Spanish has Spanish buttons in its date picker.

If the project was generated with extra languages, each module has a `values-xx/strings.xml` beside
`values/`, and `AppLocales.supported` lists them for the language picker. Lint fails a build that
ships a language with a string missing from it.

## Every form factor

The same app runs on a phone, a foldable, a tablet, a Chromebook, a desktop window and a car. None
of the screens assumes one: no activity is locked to an orientation, and lint fails the build if
one is (`LockedOrientationActivity`, `SourceLockedOrientationActivity`, `NonResizeableActivity`).

Layout is decided from the window, never from the device or its orientation. `AppTheme.windowSize`
holds Android's window size classes, computed by `androidx.window` from the window the app is
actually in — which changes when the window is resized, split or unfolded:

| Width | From | Typically |
|---|---|---|
| Compact | 0dp | a phone held upright, a narrow split-screen pane |
| Medium | 600dp | a phone held sideways, a small tablet, an unfolded foldable |
| Expanded | 840dp | a tablet held sideways, a desktop window |
| Large, ExtraLarge | 1200dp, 1600dp | a large tablet or a wide desktop window |

`AppTheme.layout` turns the class into measurements, so screens do not choose numbers:

- `gutter`, the margin to the window's edge, grows with the window. `AppTheme.spacing.gutter` is the
  same value, so every screen's margins adapt without asking.
- `formMaxWidth`, `readableMaxWidth` and `contentMaxWidth` are the widths content stops growing at.
  `AppScaffold(contentMaxWidth = …)` centres the content at that width while the bars and the
  background still span the window.
- `listPaneWidth` and `gridMinCellWidth` size list-detail panes and grid cells.

What adapts, and how:

- **Navigation:** a bottom bar below Medium width, a rail from Medium up. A rail widens to clear a
  camera cutout on its side instead of squeezing its labels.
- **List and detail:** side by side from Expanded width (see Navigation).
- **Two halves of one screen** (`AppTwoPane`): side by side from Expanded width, and on a short
  window of Medium width or more — a phone held sideways has no height to stack them in.
- **Grids** (`AppResponsiveGrid`): as many equal columns as fit at `gridMinCellWidth`.
- **Sheets, dialogs and snackbars** stop at `sheetMaxWidth` and centre.
- **Display cutouts:** `AppScaffold` pads both sides for them. A screen with a full-bleed panel turns
  that off (`clearDisplayCutout = false`) and pads the panel's content itself, so the colour reaches
  the edge of the glass.

To check a screen on other sizes without other devices, run the emulator and change its display:
`adb shell wm size 1600x2560` and `adb shell wm density 320` make a tablet, rotating makes it a
landscape tablet, and `adb shell wm size reset` undoes it.

## Build

`build-logic/` holds convention plugins, one per kind of module (`com.base.app.android.library`,
`com.base.app.android.feature`, `com.base.app.android.data` and so on). A module's own build file lists only its
dependencies and anything unusual about it. SDK levels, the Kotlin and Java targets, and the Compose
setup are decided once. `:core:model` is a plain JVM module and compiles without the Android
plugin.

`AppConfig.kt` holds the application id, version and backend URLs for each environment, and it is
the only place the build reads them.

### Build guards

These tasks run as part of `./gradlew build` and fail it:

| Task | Checks |
|---|---|
| `verifyModuleDependencies` | The module tier rules above. |
| `verifyCodeRules` | The forbidden patterns, each rule in `build-logic/…/guard/`: no Material or ripple, no raw tweens, colours or sizes in screens, keyed lazy rows, no `Dispatchers.IO` in a class, no orientation or screen-size branching, every `@Composable` module on the Compose compiler, no prose literal in a copy parameter, and the rest. A justified exception is `// allow(<rule>): <reason>` on the line above. |
<!-- <opt:devtools> -->
| `verify<Variant>CarriesNoDevTooling` | A release-like build outside the dev flavour (any build type but debug: stagingRelease, prodRelease, playstoreRelease and, with baseline profiles, their benchmark builds) depends on no dev tooling module, directly or through another module. Also runs before that build. |
<!-- </opt:devtools> -->
| `checkComposeStability` | No composable parameter in the design system, `:core:ui` or `:core:navigation` is compared by identity beyond `config/compose-stability-baseline.txt`, where each accepted entry carries its reason. |

### Flavours are only on the application modules

There are four environments (dev, staging, prod, playstore), each with debug and release, except
`playstoreDebug`, which does not exist. Library modules have no flavours. A library with four
flavours would build eight variants instead of two, which slows sync and means more for AGP to
invalidate on a variant switch. What varies by environment is configuration, and it reaches the
libraries through Hilt from `:app`. Switching from devDebug to stagingDebug therefore rebuilds no
library module.

Every build except a prod or playstore release adds its variant to the version name
(`1.0.0-stagingRelease`), so a screenshot or crash report shows which backend it was using.

<!-- <opt:devtools> -->
### Dev tooling is absent from shippable builds

The inspector (`:core:devtools`) and what plugs into it are `debugImplementation` and
`"devImplementation"` dependencies of `:app`, so devDebug, devRelease, stagingDebug and prodDebug
carry them and stagingRelease, prodRelease and playstoreRelease do not: no class, resource,
manifest entry or thread, rather than tooling switched off at run time. The root reaches it through
`DevTooling` in `:core:ui`, injected as an `Optional`; empty, the root composes nothing for it.
`:app` provides `AppEnvironment` (`:core:common`) from its `BuildConfig` for the inspector to show,
and only the inspector reads it.

<!-- </opt:devtools> -->
### No library has a BuildConfig

A library that reads `BuildConfig.DEBUG` behaves differently depending on which variant compiled
it, and the difference often only shows up in release builds. `:app` builds `NetworkConfig` from
its own `BuildConfig` and injects it, and it sets `AppLogger.debugEnabled` at startup.

### Release artifacts are named

`./gradlew :app:distProdRelease` copies every APK and the bundle into
`build/outputs/dist/<variant>/`. Each file is named with the variant, ABI, version and build time,
and a `checksums.sha256` file sits beside them. With four files all called `app-release.apk` in
four sibling directories, sending the wrong one to a tester is easy.

ABI splits are enabled only when the requested tasks assemble a release, so debug builds are not
slowed down by them.

### Signing falls back to the debug key

If `keystore.properties` is missing, or has no block for a flavour, the build signs with the debug
key and says so in the console. A fresh clone builds and runs without anyone handing out secrets.
The console message means nobody finds out at upload time that a release was debug-signed.

<!-- <opt:database> -->
### Room schemas

The Room convention plugin exports each database's schema to `schemas/` in its module. Commit those
files. They are what a migration test compares against, and without them a schema change cannot be
reviewed.
<!-- </opt:database> -->

## Testing

- Dispatchers are injected and classes never use `Dispatchers.IO` directly, so `runTest` controls
  the clock and tests do not wait in real time.
- Repositories are interfaces, so a ViewModel test injects a fake and never opens a socket.
- Screens are stateless functions of their state, so they can be previewed and tested without a
  ViewModel.
- `MainDispatcherRule` replaces `Dispatchers.Main`. Without it every ViewModel test hangs, because
  `viewModelScope` has no main looper off-device.
- `FakeNetworkMonitor` lets a test switch between online and offline.

<!-- <opt:sample> -->
`feature/sample` is the worked example of all of these.
<!-- </opt:sample> -->
