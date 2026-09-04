# Architecture

The decisions this project has already made, and why. Read it once; the rest is in the KDoc on
the files themselves.

## The module graph

```
        ┌──────────────┐   ┌──────────────┐
        │    :app      │   │  :catalog    │   composition roots — wire, own no logic
        └──────┬───────┘   └──────┬───────┘
               │                  │
        ┌──────▼───────┐          │
        │  :feature:*  │          │           presentation only
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

Four rules, all enforced by the `verifyModuleDependencies` task rather than by review:

| Tier         | May depend on          |
|--------------|------------------------|
| `:app`       | anything               |
| `:feature:*` | `:core:*`, `:data:*`   |
| `:data:*`    | `:core:*`              |
| `:core:*`    | `:core:*`              |

The two "never a sibling" rules are the load-bearing ones. A single `:feature:cart →
:feature:catalog` edge is harmless the day it is added and, eighteen months later, is the reason
nothing can be built or tested in isolation. It always arrives as a one-line convenience in a pull
request about something else, which is why it is a build failure.

Two features that need the same data both depend on the `:data:` module that owns it. Two features
that need to react to each other do so through state a `:data:` module exposes as a singleton.

## Where a feature lives

```
data/orders/       DTO · mapper · API service · repository        — no Compose
feature/orders/    contract · ViewModel · screen · nav key        — no network
```

Splitting them is what lets the data half compile in parallel with the feature half rather than
behind it, and what makes "the cart badge on the home screen" possible without an edge between
two features.

## MVI

Three types per screen, and one base class.

```kotlin
@Immutable
data class OrdersState(
    val loadState: LoadState = LoadState.Idle,
    val items: List<Order> = emptyList(),
) : UiState

sealed interface OrdersEvent : UiEvent { … }
sealed interface OrdersEffect : UiEffect { … }
```

- **State** is everything the screen renders, and it is complete. Rendering must never need a
  value that is not in it.
- **Events** are everything the user or the system can do to the screen.
- **Effects** are what happens *once* and is not state: navigate, dismiss the keyboard, open the
  dialler. Putting one of those in state is how a navigation fires again on rotation.

### Events are queued, not launched

`MviViewModel` puts each event on a channel that a single coroutine drains in order. The obvious
alternative — `viewModelScope.launch { handleEvent(event) }` per call — starts a coroutine per
event, and two events that both read-modify-write the state can interleave between the read and
the write. That is a lost update; it only appears under fast input, and it is close to impossible
to reproduce deliberately.

The cost is that a slow handler delays the next event. That is the right default, because the
events behind it almost always depend on what this one is about to write. Anything genuinely
long-running opts out explicitly with `launchWork { }`.

### LoadState, not a boolean and a nullable

`isLoading: Boolean` plus `error: String?` has four representable combinations and three
meaningful ones. "Loading *and* errored" shows up in practice as a spinner on top of an error
message. A sealed `LoadState` cannot express it.

The distinction that earns its keep daily is `Loading` versus `Refreshing`: the first has nothing
to show and gets a skeleton, the second already has content on screen and must not replace it with
one. Collapsing them is why pull-to-refresh so often blanks the list it was asked to refresh.

## Navigation

Feature modules never import a navigation library. They implement `AppNavKey`, register a screen
with `navGraph { }` and their keys with `navKeys { }`, and both go into a Hilt `@IntoSet`.

```kotlin
@Serializable
data class OrderDetailKey(val orderId: Int) : AppNavKey

@Provides @IntoSet
fun ordersNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
    entry<OrderDetailKey> { key -> OrderDetailRoute(key.orderId, navigator) }
}

@Provides @IntoSet
fun ordersNavKeys(): SerializersModule = navKeys {
    subclass(OrderDetailKey::class, OrderDetailKey.serializer())
}
```

Adding a screen touches no file outside its own module. There is no central sealed `Route` class
to extend and no `when` in `:app` to add a branch to — which is also why two people adding screens
in the same week do not conflict on the same two files.

`AppNavHost` is the only file that names Navigation 3. Replacing the library is a change to it and
to `NavTransitions`.

### Keys carry ids, not models

A key is serialised into the saved-state bundle. One holding a whole object both bloats the bundle
and goes stale the moment the app is backgrounded. Parameters that look like content — a name for
the app bar — are a seed so the screen has something to render before its fetch resolves, and the
screen still fetches.

### Registering the serializer is the step people forget

Skip `navKeys { }` and the app works perfectly until it is killed in the background, then comes
back at the start destination instead of where the user was.

### The entry point is derived, not navigated to

`AppDestinations` holds the tabs and the start destination; nothing else names one. What the app
opens on is a pure function of the settings and session `MainActivity` has just read:

```
onboarding not finished  ->  the walkthrough, no tab bar
not signed in            ->  sign in, no tab bar
otherwise                ->  the tabs, starting at the first
```

So finishing onboarding is `settingsStore.setOnboardingCompleted(true)` and signing in is a token
reaching the store — the composition follows from the same emission that repaints the theme. The
imperative alternative, a `navigate()` at startup, has to answer "what if the flag changes for
some other reason", and the answer is different on each of the three screens that could ask.

The same argument runs the other way at sign-out: `SessionCoordinator` clears every
`SessionScopedStore` *before* announcing it, so the sign-in screen never appears over data that is
still on disk.

### Each tab keeps its own back stack

`ShellState` holds one stack per tab rather than one stack with tab markers in it. Someone three
screens deep in Orders who checks Profile and comes back expects to be where they left off, and
every app they use behaves that way. A single list with boundaries in it has to answer "what does
Back do here" for every position in the list, and gets it subtly wrong somewhere; separate lists
make the answer structural — Back inside a tab pops that tab, Back at a tab root is the shell's
decision.

Re-tapping the active tab pops it to its root. That is a gesture people use constantly and
almost never discover by being told about; a tab that ignores its own re-tap feels broken to
anyone who has the habit.

## The design system

`:core:designsystem` depends on `androidx.compose.foundation` and `androidx.compose.ui`. There is
no `material` or `material3` artifact anywhere in the project, and an import of one fails the
build — see `verifyComposeUsage`.

That means this module also supplies what Material would otherwise have provided: the indication
every clickable draws (`AppIndication`), the inherited content colour and text style, the text
selection handle colours, and every component and icon.

Screens read tokens through one accessor:

```kotlin
AppText(text = order.title, style = AppTheme.typography.titleLarge, color = AppTheme.colors.contentPrimary)
Spacer(Modifier.height(AppTheme.spacing.lg))
```

Nothing outside `Palette.kt` names a colour by its hue. That indirection is what makes rebranding
one file, and what makes a dark theme one more instance of `AppColors` rather than a search
through every feature.

## Build

### Flavours are only on the application modules

Four environments — dev, staging, prod, playstore — times debug and release, minus
`playstoreDebug`, which does not exist. Library modules carry no flavours: a library with four
would build eight variants instead of two, which multiplies sync time and how much AGP has to
invalidate on a variant switch. What actually varies per environment is configuration, and that
reaches the libraries through Hilt from `:app`.

That is why switching from devDebug to stagingDebug does not rebuild a single library module.

### No library has a BuildConfig

A library that reads `BuildConfig.DEBUG` behaves differently depending on which variant compiled
it — the kind of difference that only appears in a release build. `NetworkConfig` is constructed
in `:app` from its own `BuildConfig` and injected.

### Release artifacts are named

`./gradlew :app:distProdRelease` copies every APK and the bundle into
`build/outputs/dist/<variant>/` named with the variant, ABI, version and build time, with a
`checksums.sha256` beside them. Four files all called `app-release.apk` in four sibling
directories is how the wrong build reaches a tester.

ABI splitting is enabled only when the requested tasks are actually assembling a release, so it
never slows the debug builds that make up almost every build anyone runs.

### Signing degrades rather than fails

A missing `keystore.properties`, or a missing block within it, falls back to the debug key and
says so on the console. A fresh clone still produces a running devDebug without anyone being
handed secrets first — and nobody discovers at upload time that their release was debug-signed.

## Testing

- Dispatchers are injected, never `Dispatchers.IO` inside a class, so `runTest` controls the
  clock instead of the test waiting real seconds.
- Repositories are interfaces so a ViewModel test injects a fake and never opens a socket.
- Screens are stateless functions of their state, so they are previewable and assertable without
  a ViewModel.
- `MainDispatcherRule` swaps `Dispatchers.Main`; without it every ViewModel test hangs, because
  `viewModelScope` has no main looper off-device.

`feature/sample` is the worked example of all four.
