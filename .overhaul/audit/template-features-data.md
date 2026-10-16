# Audit: template features, data, core/{model,common,coroutines,testing}

Scope: `template/feature/**`, `template/data/**`, `template/core/model`, `core/common`,
`core/coroutines`, `core/testing` of `android-base` (branch `overhaul/multiplatform-v2`), compared
against the two hand-evolved apps PeriFlow (`periflow-android`) and VitFleet (`vitfleet-android`).
I also read the `core/ui`, `core/network`, `core/navigation`, `build-logic` and `generator/genkit/scaffold.py`
code these modules rely on, because several defects in this area are only visible there.

All paths below are relative to `android-base/template/` unless prefixed `PF:` (PeriFlow) or
`VF:` (VitFleet) or `generator/`.

---

## 1. Executive summary

The MVI base (`MviViewModel`) is small and readable. Its central design choice is a serial,
**suspending** event queue: `handleEvent` is `suspend` and the next event is not read until the
current handler returns. The reference sample, the scaffolder and most template ViewModels then do
their network I/O *inside* `handleEvent`. Every other event waits behind it: Back taps, keystrokes in
a controlled text field, and Retry all queue until the request returns or times out. VitFleet's 59
ViewModels contain 249 `launchLatest` and 113 `launchWork` calls, so its authors had to learn to
route around the default. The same queue makes the double-submit guard in `FormState.submitting` unreachable.

The data layer is a thin "repository returns `AppResult`" pattern with no offline-first flows. The
only caching is the network client's response cache. `AppResult.Failure` is a bag of eight
optional fields rather than a typed error. Both real apps had to extend it: PeriFlow added
`errorCode`, and VitFleet parses `rawBody` into an `ApiError`.

`NetworkMonitor` is a cold `callbackFlow`, and the network client calls `isOnline.first()` on every
request. Each call registers and unregisters a system `NetworkCallback`, and Android caps an app at
about 100 registered callbacks. VitFleet hit this and fixed it with `shareIn` plus a test. The
template also gates every request on `NET_CAPABILITY_VALIDATED`, so it refuses to send anything on
networks Android has not validated.

On module boundaries, the rule "feature never depends on feature, data never on data" holds in the
template. At real-app scale it pushed shared types into the wrong places:
- PeriFlow moved cross-feature nav keys into `core:navigation/Destinations.kt`.
- VitFleet hand-writes cross-feature "Links" ports in a 292-line `:app` module.
- VitFleet dumped 3,679 lines of generated DTOs into `core:model`, which 18 feature modules import
  directly.

The template has no api/impl split and no domain layer. Its impl classes and DTOs are public.

**Proposal: keep the flat layout as the default, and add an opt-in "modular" architecture.** The
modular layout would add `feature:x:api` / `feature:x:impl` and `data:x:api` / `data:x:impl`, plus an
optional `domain:x`. It needs matching `ModuleGraph` rules. Section 9 has the details.

**Sample and scaffold code that users copy:**
- **The `feature_modules` scaffold is broken at runtime.** It decodes JSON into a class with no
  `@Serializable`, so every load fails. It also navigates to a detail key it never registers, so the
  first tap on a row crashes.
- **Profile stores a photo-picker URI without persisting the permission**, so the avatar breaks
  after process death.
- **Every preset, including "lean", ships `sample`.** PeriFlow generated sample, feed, profile,
  search and settings, then deleted all of them by hand. Only their `build/` directories remain.

**Dead code is substantial:**
- `core:model` is imported by nothing.
- 6 of 8 `AppResult` helpers are unused.
- 5 of 9 `Validators` are unused, and their strings are translated in 5 languages anyway.
- `MviTestScope` is unused in the template and in both apps.
- `mockk` is an `api` dependency of `core:testing` with zero users.
- The `@MainDispatcher` and `@MainImmediateDispatcher` qualifiers are unused.

**Test coverage is thin where it matters.**
- `feature/auth` (991 lines), `data/auth`, settings, onboarding and feed have **zero** tests.
- The only `MviViewModel` test uses a `delay(1000)` misnamed `advanceUntilIdleCompat`.

---

## 2. Findings index (most severe first)

| # | Sev | Title | Where |
|---|-----|-------|-------|
| F1 | high | Suspending serial event queue blocks Back, typing and Retry behind I/O in the reference code | `core/common/.../mvi/MviViewModel.kt:53-73`, `feature/sample/.../SampleListViewModel.kt:33-48`, `SampleDetail.kt:69-86`, auth, licences, scaffold |
| F2 | high | Scaffolded `:data:x` decodes into a non-`@Serializable` model; every load fails at runtime | `generator/genkit/scaffold.py:99-110,131-135` |
| F3 | high | Scaffolded `:feature:x` navigates to an unregistered `DetailKey`, which crashes via `error()` | `generator/genkit/scaffold.py:401,431-442`; `core/navigation/.../NavRegistry.kt:85-89`, `AppNavHost.kt:148` |
| F4 | high | Cold `NetworkMonitor` registers a system `NetworkCallback` per request (`isOnline.first()`) | `core/common/.../network/NetworkMonitor.kt:33-80`; `core/network/.../KtorNetworkClient.kt:84,135,166`; fixed in VF |
| F5 | medium | Requests are refused pre-flight unless Android marked the network VALIDATED | `NetworkMonitor.kt:65-68,83-86`; `KtorNetworkClient.kt:84-86` |
| F6 | medium | `FormState.submitting` guard is unreachable under the serial queue; IME Done can double-submit | `core/ui/.../form/FormState.kt:140-150`; `feature/auth/.../SignIn.kt:195`, `SignUp.kt:198`, `PasswordReset.kt:178` |
| F7 | medium | Profile persists a photo-picker URI without `takePersistableUriPermission` | `feature/profile/.../ProfileScreen.kt:122-125`; `data/profile/.../ProfileRepository.kt:50-52`; `core/media/.../MediaPickerController.kt:61-63` vs `69-83` |
| F8 | medium | No cross-feature navigation contract; real apps leak keys into core or wire them by hand in `:app` | `build-logic/.../ModuleGraph.kt:64-69`; PF `core/navigation/Destinations.kt`; VF `app/di/FeatureBindingsModule.kt` |
| F9 | medium | `core:model` is empty (dead `Paged`) yet attached to every module; in VF it became a 3.7k-line DTO dump imported by features | `core/model/**`; `AndroidDataConventionPlugin.kt:20`; `AndroidFeatureConventionPlugin.kt:18`; VF `core/model/api/ApiModels.kt` |
| F10 | medium | Typed input is lost on process death in profile and all auth forms; `UiState` contract contradicted by `FormState` living outside state | `ProfileViewModel.kt:16-25,41-51`; `SignIn.kt:77-80,147`; `UiContracts.kt:7-9` |
| F11 | medium | `AppResult.Failure` is an untyped bag of optional fields; apps re-invent typed errors | `core/common/.../AppResult.kt:14-32`; `feature/auth/.../AuthMessages.kt:12-16`; PF `AppResult.kt` (+`errorCode`); VF `ApiError.kt` |
| F12 | medium | ViewModels collect repository flows forever from `init`; one `stateIn(WhileSubscribed)` is cargo-cult and causes a default-value flash | `Settings.kt:119-125`; `ProfileViewModel.kt:45-51`; `SearchViewModel.kt:51`; PF `TodayViewModel.kt` (8 collectors) |
| F13 | medium | Impl classes and DTOs are public; there is no domain layer, so business logic and DTOs flow into ViewModels at scale | `data/*/Default*Repository` (public), `data/sample/remote/SampleDto.kt:9`, `data/feed/remote/FeedPostDto.kt:8`; VF `feature/trips/LoadForm.kt` (VM ~765 lines on `TripsTripDto`) |
| F14 | medium | Auth (most consequential flows) and `data:auth` have zero tests; `MviTestScope` helper is unused everywhere | `feature/auth/**`, `data/auth/**`, `core/testing/.../MviTestScope.kt:50-68` |
| F15 | low | `UiMessage` is a data class: the same message twice is swallowed by `mutableStateOf` and dismissed on the first one's timer | `core/common/.../mvi/UiMessage.kt:6-12`; `core/ui/.../MessageHost.kt:24-35`; fixed in VF |
| F16 | low | Messages are collected without lifecycle awareness and can time out while the app is stopped | `core/ui/.../MessageHost.kt:26-27` vs `MviScreen.kt:46-53` |
| F17 | low | Settings: needless plumbing (`setAppVersion` via `LaunchedEffect`) and magic theme indices in events | `Settings.kt:70-75,128-135,161-170,183-187`; `SettingsNav.kt:34-40` |
| F18 | low | Navigation delayed behind a DataStore fsync (`remember` before `emitEffect`) | `SearchViewModel.kt:78-81` |
| F19 | low | One shared `session` DataStore file for tokens, profile and recent searches; `AppSettingsStore` is a cross-feature god store | `core/datastore/.../DataStoreModule.kt:40-54`; `ProfileRepository.kt:32`; `SearchRepository.kt:48`; `AuthTokenStore.kt:38` |
| F20 | low | Data convention adds `core:network`, `core:datastore`, `core:coroutines` and `core:model` to every data module whether used or not; `paging-runtime` pulls RecyclerView and LiveData | `AndroidDataConventionPlugin.kt:19-29`; `data/feed/build.gradle.kts:10-11`; `data/auth/build.gradle.kts:10-11` |
| F21 | low | Dead code across `core:common`, `core:coroutines`, `core:testing` and the features | see §7 |
| F22 | low | i18n: English literals in the data layer, a developer message in translatable strings, resolved strings fed back into state | `data/sample/.../SampleDto.kt:21`; `feature/auth/.../strings.xml:27`; `SignIn.kt:61,89`; `MviViewModel.kt:159-161` |
| F23 | low | `core:common` is Android-bound (Context, R, Log, ConnectivityManager, manifest), which blocks the CMP generator | `UiText.kt:3-5,27-32`; `AppLogger.kt:3`; `NetworkMonitor.kt:3-8`; `core/common/src/main/AndroidManifest.xml` |
| F24 | low | Inconsistent feature file layout and navigation wiring across the reference features | see §6.3 |
| F25 | low | AI-slop and comment issues (false claim, duplicated KDoc, `advanceUntilIdleCompat`) | `Validators.kt:6-9`; `Onboarding.kt:49-59`; `MviViewModelTest.kt:58-63` |
| F26 | low | `handleEvent` runs synchronously inside the subclass constructor in production but not in tests | `MviViewModel.kt:53-73` + `init { onEvent(...) }` pattern |

---

## 3. MVI base (`core/common/mvi`)

### F1 (high): the suspending serial queue blocks the screen behind I/O

**Evidence.**

`core/common/src/main/kotlin/com/base/app/core/common/mvi/MviViewModel.kt`:

```kotlin
53  init {
54      viewModelScope.launch {
55          for (event in events) {
56              try {
57                  handleEvent(event)          // suspends; next event not received until it returns
...
69  protected abstract suspend fun handleEvent(event: E)
```

The KDoc (lines 29-31) says long work "goes through `launchWork` or `launchLatest`". The reference
code users are told to copy does the opposite:

- `feature/sample/.../list/SampleListViewModel.kt:15` says it is "The reference ViewModel. Every
  feature in this project is shaped like this one." Lines 33-48 run `load()`, a `suspend` network
  call, directly inside `handleEvent`.
  - The search field is in the top bar during loading (`SampleListScreen.kt:56-59`). It is a
    controlled `BasicTextField(value = state.query)` (`core/designsystem/.../AppTextField.kt:127-130`).
  - `QueryChanged` sits in the queue until `repository.items()` returns, so `state.query` does not
    change. The field re-renders the old value, which drops keystrokes. Offline, a request can take
    the full timeout plus retries.
- `feature/sample/.../detail/SampleDetail.kt:69-86`: `BackClicked` (top-bar back, line 101) is
  queued behind `load()`. **The back arrow does nothing until the request finishes.**
- The same pattern appears in three more places:
  - `SignUp.kt:76-81` and `PasswordReset.kt:74-79`: `BackClicked` waits behind `submit()`.
  - `Licenses.kt:68-73`: Back waits behind an asset read.
  - The generated scaffold, `generator/genkit/scaffold.py:249-269`, which every `feature_modules`
    entry gets.

**Counter-evidence from the real apps.** VF's 59 ViewModels contain 249 `launchLatest` and 113
`launchWork` calls (grep of `VF:feature/**/src/main`). VF also added `cancelLatest`
(`VF:core/common/.../MviViewModel.kt:146-150`). The authors learned to bypass the queue for nearly
everything.

**Root cause.** The serial queue exists to give "read state, then write state" atomicity
(`MviViewModel.kt:29-30`). On `Dispatchers.Main.immediate` that is already guaranteed by
single-threaded execution, as long as handlers are **not suspending**. The `suspend` modifier is
what lets a handler hold the queue across I/O.

**Fix (sketch).** Make `handleEvent` non-suspending, so blocking becomes impossible by
construction. Provide explicit concurrency policies:

```kotlin
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(initial: S) : ViewModel() {
    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    // Main thread only; ordering is guaranteed because nothing here suspends.
    fun onEvent(event: E) = handleEvent(event)
    protected abstract fun handleEvent(event: E)

    /** Latest wins: a new call under [key] cancels the old one (search, reload). */
    protected fun launchLatest(key: Any, debounce: Duration = Duration.ZERO, block: suspend CoroutineScope.() -> Unit): Job
    /** Exclusive: ignored while one under [key] is running (submit, pay, save). */
    protected fun launchExclusive(key: Any, block: suspend CoroutineScope.() -> Unit): Job?
    protected fun launch(block: suspend CoroutineScope.() -> Unit): Job
}
```

`launchExclusive` fixes F6 as well. Update the sample, the scaffold and every template VM
accordingly. Add a test proving that `BackClicked` is handled while a load is in flight.

### F6 (medium): the double-submit guard is unreachable

`core/ui/.../form/FormState.kt:140-150`:

```kotlin
suspend fun <T> FormState.submitting(block: suspend (Map<String, String>) -> T): T? {
    touchAll()
    if (!canSubmit()) return null     // canSubmit = isValid && !isSubmitting
    isSubmitting = true
    return try { block(values()) } finally { isSubmitting = false }
}
```

Because events are processed one at a time, a second `Submit` is only read after the first one's
`finally` has reset `isSubmitting`. The guard can never see `true`.

The only remaining protection is that `AppButton` disables itself while loading
(`AppButton.kt:120`). The keyboard path bypasses it:
- `SignIn.kt:195` and `SignUp.kt:198` (`KeyboardActions(onDone = { onEvent(Submit) })`) and
  `PasswordReset.kt:178`.
- `busyOverlay` only consumes pointer events (`AppTransitions.kt:237-258`), so the IME stays live.

Pressing Done twice while a request is in flight sends two sign-in, two sign-up or two reset-email
requests. After a successful first sign-in, the second runs while the navigation is already
happening. **Fix:** `launchExclusive("submit")` from F1, or check `form.isSubmitting` in `onEvent`
before enqueuing.

### F26 (low): handler runs during construction in production, not in tests

`viewModelScope` uses `Dispatchers.Main.immediate`. When a subclass calls `onEvent(Load)` from
`init` on the main thread, `trySend` resumes the suspended receiver in place. `handleEvent` then runs
**inside the subclass constructor**, up to its first suspension:
- `SampleListViewModel.kt:30`
- `SampleDetail.kt:65-67`
- `SearchResult.kt:60-62`
- `Licenses.kt:64-66`
- the scaffold's VM

A subclass that declares a property below its `init { onEvent(...) }` sees it uninitialised. Unit
tests use a `StandardTestDispatcher` (`core/testing/.../MainDispatcherRule.kt:20-22`), which queues
instead, so they never exercise this ordering. Low severity today, but it is a trap in the reference
pattern. Prefer starting work on first subscription (see F12) rather than from `init`.

### F15 (low): identical messages are swallowed (VF fixed this)

`UiMessage` is a `data class` (`UiMessage.kt:6-12`). `MessageHost` stores it in
`mutableStateOf<UiMessage?>` (`core/ui/.../MessageHost.kt:24-27`), which uses structural equality,
and keys the timeout on it with `LaunchedEffect(current)` (line 30).

If "Profile saved" is emitted twice within 3.5 s, the second assignment is a no-op. The timer is not
restarted, and the message vanishes on the first one's deadline. VF wraps each showing in a
non-data `Shown` class (`VF:core/ui/.../MessageHost.kt`):

> "Saved" emitted twice is two showings, and as equal values the second neither changed the state
> nor restarted the timer.

**Backport the VF change.** PF additionally plays a haptic per `MessageKind`
(`PF:core/ui/.../MessageHost.kt`), which is worth taking for the Apple-polish goal.

### F16 (low): messages are not lifecycle-aware

`MviScreen.kt:46-53` collects effects inside `repeatOnLifecycle(STARTED)` on `Main.immediate`,
which is correct. `MessageHost.kt:26-27` collects messages with a plain `LaunchedEffect`.

A "Could not refresh" message emitted while the app is in the background is consumed, and its
`delay` runs out while nothing is drawn, so the user never sees it. Collect messages under the same
`repeatOnLifecycle` block.

### F12 (medium): forever-collectors in `init`, and a cargo-cult `stateIn`

- **`Settings.kt:119-125`:**

  ```kotlin
  launchWork {
      settingsStore.settings
          .stateIn(this, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), AppSettings())
          .collect { settings -> updateState { copy(settings = settings) } }
  }
  ```

  The only subscriber of this `stateIn` is the `collect` right after it, so `WhileSubscribed` never
  stops anything. The operator only adds a default `AppSettings()` emission before the real value.
  Switches and the segmented control render defaults for a frame and then animate to the stored
  values on every open. That is a visible glitch, which runs against the polish goal.
- **`ProfileViewModel.kt:45-51` and `SearchViewModel.kt:51`** collect DataStore flows for the whole
  VM lifetime. That includes while the screen is in the back stack or the app is in the background.
- **PF's `TodayViewModel` `init`** launches 8 such collectors (`PF:feature/today/.../TodayViewModel.kt`).
  The template is teaching this pattern.

**Fix.** Add one primitive to the base class that observes only while the UI is subscribed:

```kotlin
protected fun <T> Flow<T>.observe(stopAfter: Duration = 5.seconds, reduce: S.(T) -> S) {
    viewModelScope.launch {
        _state.subscriptionCount
            .map { it > 0 }
            .distinctUntilChanged()
            .collectLatest { active ->
                if (active) collect { value -> _state.update { it.reduce(value) } }
                else delay(stopAfter)   // keep briefly for rotation, then this branch is cancelled
            }
    }
}
```

Make "observe the store" the canonical way to load in the sample and the scaffold. It also removes
the need for `init { onEvent(Load) }` (F26).

### F10 (medium): process death and the "complete state" contract

The `persistState` KDoc (`MviViewModel.kt:96-99`) says to persist "what the user typed". The
template does not follow it:

- `ProfileViewModel` (lines 16-25 and 41-51) has no `SavedStateHandle`. A half-typed name or bio
  is lost on process death, and it is the screen with the most typing.
- All auth forms keep their fields in `FormState` (`SignIn.kt:77-80`, `SignUp.kt:65-74`,
  `PasswordReset.kt:70-72`). That type has no `SavedStateHandle` integration
  (`core/ui/.../form/FormState.kt`), so email and name are lost.
- `UiContracts.kt:7-9`:

  > "`UiState` is everything the screen renders, and it is complete … no second source of truth."

  Yet `SignInRoute` passes `form = viewModel.form` beside `state` (`SignIn.kt:147`, also
  `SignUp.kt:130`, `PasswordReset.kt:116`). The documented contract is contradicted by the auth
  sample.
- VF dropped `FormState` entirely (0 uses of `buildForm` across `VF:feature`). PF still uses it in
  4 files.

**Fix.** Either fold form fields into `UiState`, with validation as a pure function and persistence
via `persistState`, or give `FormState` a `saveIn(handle: SavedStateHandle)` and state honestly that
it is a second state holder. The first option is simpler and works for CMP and SwiftUI later. Also
add `persistState` to Profile.

---

## 4. Error modelling (`AppResult`, `LoadState`, `UiText`)

### F11 (medium): `AppResult.Failure` is a bag of optional fields

`core/common/.../AppResult.kt:14-32` gives `Failure` eight fields: `message`, `cause`, `code`,
`fieldErrors`, `isOffline`, `rawBody`, `queued`, plus `Success.fromCache` and `cachedAtEpochMillis`.

- Every consumer re-derives meaning from field combinations:
  - `authMessage` switches on raw HTTP codes (`feature/auth/.../AuthMessages.kt:12-19`).
  - `userMessage` walks the cause chain looking for `IOException` (`AppResultMessages.kt:11-21`).
  - Each auth VM checks `fieldErrors.isEmpty()` (`SignIn.kt:120-125`).
- PF added `errorCode: String?` (`PF:core/common/.../AppResult.kt:23-27`). VF built a 116-line
  `ApiError` parser over `rawBody` with 35 error-code constants (`VF:core/common/.../ApiError.kt`).
- Transport caching metadata (`fromCache`, `cachedAtEpochMillis`) sits on the universal result type
  that `ImageCompressor`/`VideoTranscoder` also return (`core/media/.../ImageCompressor.kt:92`).
- **The contract leaks.** `AppResult` is described as "The outcome of anything that talks to the
  outside world" (line 3). But `DefaultAuthRepository.persist()` calls `tokenStore.save()`
  (`data/auth/.../AuthRepository.kt:109-122`), which can throw from Keystore encryption
  (`core/datastore/.../KeystoreCipher.kt:21`, no catch) or DataStore I/O. Those exceptions escape a
  function typed `AppResult<Unit>` and land in `MviViewModel.onError` as "Something went wrong".

**Fix (sketch).** A closed error type that both real apps would have fitted, without a big framework:

```kotlin
sealed interface AppError {
    data object Offline : AppError
    data object Timeout : AppError
    data class Http(val status: Int, val apiCode: String?, val fieldErrors: Map<String, List<String>>, val message: String?) : AppError
    data class Decode(val cause: Throwable) : AppError
    data class Unexpected(val cause: Throwable) : AppError
}
sealed interface AppResult<out T> { data class Success<T>(val data: T) ; data class Failure(val error: AppError, val queued: Boolean = false) }
```

Parse `apiCode` once in `core:network` through the existing `ResponseUnwrapper` seam. Freshness
belongs in a `Cached<T>`, or in a `Flow` from an offline-first repository, not on every result.

### `LoadState` / `UiText`

- `LoadState` (`LoadState.kt:6-33`) is sensible. Keep it, but drop the unused extensions (see §7).
- `LoadState.Error.message: UiText` puts Android resource ids (`@StringRes Int`) into
  `core:common`'s model. That is fine for Android, and a blocker for CMP and SwiftUI (F23).
- `UiText.Resource(args: List<Any>)` (`UiText.kt:12-15`) cannot nest a `UiText` argument. Minor.

---

## 5. Data layer (`template/data/**`)

### Pattern assessment
- Every repository is `interface` + `@Singleton Default*Repository @Inject` + a Hilt `@Binds`
  module. That is fine and testable.
- **There is no offline-first example.** No repository exposes a `Flow` backed by Room. The only
  "offline" is `CachePolicy.Enabled` in the network client (`data/sample/.../SampleRepository.kt:30-50`),
  so the screen still awaits a suspend call.
  - For the million-user goal, the canonical sample should be **Room-as-source-of-truth with
    `Flow`, plus a `refresh()` that writes through**. `core:database` already exists behind
    `<opt:room>`.
  - Today the SWR-style `NetworkClient.stream()` (`core/network/.../NetworkClient.kt:27-28`, KDoc
    "the saved copy first … then the network's answer") is not used by any template data module.
- Mapping layers are inconsistent:
  - sample: `remote/SampleDto.kt` plus a mapper and a test.
  - feed: `remote/FeedPostDto.kt` plus a mapper.
  - search: the DTO, mapper, domain model, `RecentSearches` logic and repository are all in one
    file (`data/search/.../SearchRepository.kt:19-112`).
  - auth: DTOs, with no domain model (only `Unit`).
  - The generated scaffold has no DTO and decodes straight into the domain model (F2).
- Cancellation: the template's data and feature code does not swallow `CancellationException`. The
  only catch-alls rethrow it (`MviViewModel.kt:58-63,88-92`; `AppResult.kt:68-74`).
  - The apps regress, though. VF has 79 `runCatching` in feature and data code, including
    `private suspend fun <T> Flow<T>.firstOrNullSafe(): T? = runCatching { first() }.getOrNull()`
    (`VF:feature/daily/.../DailyEntry.kt:793`), which swallows cancellation.
  - The template should ship a `suspendRunCatching`/`resultOf` that rethrows cancellation. It exists
    (`resultOf`, `AppResult.kt:67-74`) but is unused and undocumented. A detekt/lint rule against
    `runCatching` around suspend calls would help.

### F7 (medium): the profile photo breaks after process death

- The `PickVisualMedia` result goes straight to `ProfileEvent.PhotoPicked(uri.toString())`
  (`feature/profile/.../ProfileScreen.kt:122-125`).
- `ProfileViewModel.kt:70` then calls `repository.setPhoto`, which stores the string in DataStore
  (`data/profile/.../ProfileRepository.kt:50-52`).
- The image launcher in `core/media/.../MediaPickerController.kt:61-63` does **not** call
  `takePersistableUriPermission`. The `OpenDocument` launcher 8 lines below does, with the comment:

  > "Without this the URI is readable until the process dies and then is not"

  So the stored avatar URI stops resolving after a background kill or reboot.

**Fix:** copy the picked image into app-private storage (`filesDir/profile/avatar.jpg`) through
`core:media`'s compressor and store that path. That also survives backup and restore, and avoids the
512-grant cap. At minimum, take the persistable permission for the image launcher too.

### F19 (low): shared session DataStore file and the god settings store
- `@SessionDataStore` is one `Preferences` file (`core/datastore/.../DataStoreModule.kt:40-47`)
  used by `AuthTokenStore` (`AuthTokenStore.kt:38`), `ProfileRepository` (line 32) and
  `SearchRepository` (line 48). Keys are namespaced by convention only (`"profile.name"`,
  `"search.recent"`).
  - Every edit rewrites and fsyncs the whole file, so a recent-search write serialises with a token
    refresh write.
  - Prefer one file per domain, registered as a `SessionScopedStore` each. The mechanism already
    exists (`core/common/.../SessionScopedStore.kt`).
- `AppSettingsStore` (`core/datastore/.../AppSettingsStore.kt`) holds theme, onboarding, analytics,
  haptics and app lock. Both `feature/settings` and `feature/onboarding` reach into `core:datastore`
  directly (`feature/settings/build.gradle.kts:10-11`, `feature/onboarding/build.gradle.kts:10-12`).
  At scale every team edits one core class. A `:data:preferences` domain (or a per-feature
  preferences file) is the scalable shape.

### Small data-layer findings
- `AuthRepository.persist` uses `System.currentTimeMillis()` (`data/auth/.../AuthRepository.kt:115`),
  which makes the token expiry untestable. PF added an injectable `AppClock` plus `FakeAppClock`
  (`PF:core/common/.../time/AppClock.kt`, `PF:core/testing/.../FakeAppClock.kt`) and uses it in 10
  files. **Backport.**
- `RecentSearches.add` de-duplicates case-insensitively (`SearchRepository.kt:86-90`), but `forget`
  removes with exact `- query` (line 69). The two are inconsistent. Minor.
- `FeedRepository.posts()` builds a new `Pager` per call (`FeedRepository.kt:32-42`). That is fine
  because the VM caches it with `cachedIn`, but the KDoc should say "call once".
- **`FeedScreen` maps errors in the composable** by casting to a data-layer exception
  (`feature/feed/.../FeedScreen.kt:71-79`, `error as? FeedLoadException`). The UI depends on a
  data-layer implementation detail, so this belongs in the VM or a mapper.

### F20 (low): convention plugins over-attach dependencies
- `AndroidDataConventionPlugin.kt` (lines 19-29) adds `api(core:common)`, `api(core:model)`,
  `implementation(core:coroutines)`, `implementation(core:datastore)`, `implementation(core:network)`
  and `kotlinx-serialization-json` to **every** data module:
  - `data:profile` uses neither network nor coroutines qualifiers.
  - `data:feed` and `data:sample` do not use DataStore.
  - `data/auth/build.gradle.kts:10-11` re-adds `core:datastore` with a comment that implies it was
    a decision.
- `AndroidFeatureConventionPlugin.kt` (lines 17-22) adds `core:model` and `core:coroutines` to every
  feature. Only `Licenses.kt:17` uses `core:coroutines`, and nothing uses `core:model`.
- `data/feed/build.gradle.kts:10-11` uses `api(libs.androidx.paging.runtime)`. Its published
  metadata (`paging-runtime-3.5.1.module`) depends on `androidx.recyclerview:recyclerview`,
  `lifecycle-livedata-ktx` and others. A Compose-only data module needs only `paging-common`, which
  is KMP-ready. `paging-compose` already depends on `paging-common`.

Unneeded edges cost build time and make an ABI change in any of these core modules recompile every
data module.

---

## 6. Features (`template/feature/**`)

### 6.1 Sample code users copy

**F2 (high): scaffolded data module fails every request.** `generator/genkit/scaffold.py:99-110`
generates the following, and the model at lines 131-135 has **no `@Serializable`**:

```kotlin
override suspend fun items(): AppResult<List<{X}Item>> =
    networkClient.get<List<{X}Item>>(path = ITEMS_PATH)
```

```kotlin
data class {X}Item(val id: Int, val title: String)
```

The data convention applies the serialization plugin. For a type without a compiled serializer, the
plugin's `serializer<T>()` intrinsic inserts a runtime `noCompiledSerializer` call; the jar shows
`insertNoCompiledSerializerCall` in `SerializationJvmIrIntrinsicSupport`
(`kotlin-serialization-compiler-plugin-embeddable-2.4.20`).

So the project compiles, and at runtime `decodeResult`'s `runCatching` (`NetworkClient.kt:45-58`)
turns the `SerializationException` into `NetworkException.Serialization`. **Every scaffolded feature
shows "Something went wrong" on first run.** `tests/spec_full.json` (network on, scaffolds
`orders` and `wallet`) builds, so CI cannot catch it, and the scaffold test uses a fake repository.

**Fix:** generate `remote/{X}Dto.kt` (`@Serializable`) with a `toDomain()` mapper, mirroring
`data/sample`, and add a generator test asserting `@Serializable` in the output.

**F3 (high): scaffolded feature crashes on the first tap.** The route navigates to
`{X}DetailKey(effect.id)` (`scaffold.py:401`). The nav module registers only
`entry<{X}ListKey>` (lines 440-442), and its KDoc admits:

> "`{X}DetailKey` has no screen yet" (line 431)

`AppNavHost.kt:148` calls `registry.destinationFor(key)`, which throws via `error("No destination
registered …")` (`NavRegistry.kt:85-89`) during composition. **Fix:** scaffold the detail screen as
the sample does, or have the scaffold emit no `OpenDetail` until one exists.

**Sample features ship in every preset and are deleted in practice.**
- `generator/genkit/spec.py` PRESETS includes `sample` in "lean", "standard" and "everything".
- PF's spec enabled sample, search, profile, paging and settings. PF then deleted
  `feature/{feed,profile,sample,search,settings}` and `data/{feed,profile,sample,search}`: only
  `build/` directories remain under those paths.
- VF has none of them.

**Recommendation:**
- Make the scaffold the single exemplary reference (fixed per F1-F3, offline-first, tested).
- Move the showcase features into `:catalog`, or behind an explicit `examples` option that is off
  by default.
- This shrinks every generated app by ~9 modules and roughly 3,000 lines.

### 6.2 Specific feature defects
- **Settings first-frame glitch.** See F12.
- **F17: needless plumbing in Settings.**
  - `SettingsAppInfo` is already injectable (`SettingsNav.kt:34-40`), yet the version takes a long
    route: nav module → route parameter → `LaunchedEffect(appVersion) { viewModel.setAppVersion(...) }`
    (`Settings.kt:183-184`) → a public mutator outside MVI (`Settings.kt:161-164`). The first frame
    shows "Version " empty.
  - `setLanguage` (lines 165-170) is the same pattern.
  - `ThemeSelected(val index: Int)` maps magic 0/1/2 in two places (`Settings.kt:70-75` and
    `128-135`). Use the theme enum or constant in the event.
  - `SettingsEvent.BackClicked` and `SettingsEffect.NavigateBack` (lines 97, 101, 158, 194) are dead:
    the screen has no back button (`AppLargeTitle`, line 225).
- **F18: navigation waits on disk.** `SearchViewModel.kt:78-81` awaits `repository.remember(...)`
  (a DataStore edit, which fsyncs) **before** `emitEffect(OpenResult)`. Tap-to-push latency includes
  a disk flush. Emit first and persist in `launchWork`.
- **Google sign-in.**
  - `GoogleSignInButton` resolves a string and sends it to the VM (`SignIn.kt:61,89`,
    `GoogleFailed(val message: String)` → `UiText.Dynamic`). The state then holds a resolved English
    or other string, which goes stale on locale change.
  - The credential request runs in `rememberCoroutineScope` (`GoogleSignIn.kt:34,47-55`), so it is
    cancelled on configuration change and `busy` is never reset in a `finally`.
- **`SampleListState.visibleItems` is a getter** (`SampleListContract.kt:20-25`). It filters the
  full list on every read, and `Content` reads it on every recomposition (`SampleListScreen.kt:109`).
  Fine for 100 rows, but a poor exemplar. Compute it in the reducer.
- **Duplicate events.** `SampleListEvent.Load` duplicates `Retry` (`SampleListContract.kt:29-31`,
  VM lines 35-37). `Load` is only sent from `init`.

### 6.3 F24: inconsistent conventions across the reference features

| Concern | sample | search | auth | settings | feed/profile |
|---|---|---|---|---|---|
| File split | Contract / ViewModel / Screen / Routes (list) but **one file** for detail | VM+screen+route in `SearchResult.kt` (also name-clashes with `data.search.SearchResult`) | one file per screen | one file per screen | one file per screen |
| Nav wiring | Route takes `navigator`; Back via event and effect | Route takes `onOpenResult` lambda; detail uses `navigator::navigateUp` **directly in UI** (`SearchResult.kt:89`) | Route takes `navigator` plus callbacks | Route takes `navigator` | Route takes nothing |
| Keys and DI | `SampleNavKeys.kt` + `di/SampleNavModule.kt` | `SearchNavKeys.kt` + `di/` | keys **and** module in `AuthNav.kt` | keys **and** module in `SettingsNav.kt` | `XNavKeys.kt` + `di/` |
| Empty effect | n/a | `sealed interface SearchResultEffect : UiEffect` | n/a | n/a | `FeedEffect`, `ProfileEffect` empty; `NoEffect` exists but is unused (`UiContracts.kt:22`) |

Pick one layout: `XContract.kt`, `XViewModel.kt`, `XScreen.kt`, `XRoute.kt` and `di/XNavModule.kt`.
Use one Back mechanism. Have the scaffold emit exactly that layout. VF shows what happens without
it: `feature/trips/LoadForm.kt` is 1,309 lines with a ~765-line ViewModel.

---

## 7. F21: dead code and over-provisioned API (verified by grep across `app`, `catalog`, `core`, `data`, `feature`, `benchmark`)

| Item | Location | Users |
|---|---|---|
| `AppResult.flatMap`, `fold`, `onSuccess`, `onFailure`, `getOrDefault`, `resultOf` | `core/common/.../AppResult.kt:40-74` | 0 (only `map` ×3 and `getOrNull` ×1 are imported) |
| `LoadState.isInitialLoading`, `isTerminal` | `LoadState.kt:36,44-45` | 0 |
| `UiText.Composite`, `UiText.Empty`, `String?.orUiText` | `UiText.kt:25,35,45-46` | 0 |
| `showMessage(text: String)` overload (+ `asUiText`) | `MviViewModel.kt:159-161`, `UiText.kt:43` | 0; it also invites untranslated copy |
| `MessageAction`, `LONG_DURATION_MILLIS`, `MessageKind.Warning` emission | `UiMessage.kt:15,21-25` | 0 in template (PF uses `MessageAction`, so keep it) |
| `NoEffect` | `UiContracts.kt:22` | test only; features declare empty sealed interfaces instead |
| `Validators.maxLength`, `phone`, `matches`, `numericRange`, `none` | `Validators.kt:50-56,71-86,88-100,131-132` | 0. Their strings (`validation_phone`, `validation_number`, `validation_range`, `validation_max_length`) are translated in all 5 `generator/translations/*.json` |
| `@MainDispatcher`, `@MainImmediateDispatcher` (+ providers) | `core/coroutines/.../Qualifiers.kt:14-25`, `CoroutinesModule.kt:30-36` | 0 |
| `core:model` module (`Paged`) and its serialization plugin and dependency | `core/model/**`, `core/model/build.gradle.kts:3,7` | **0 imports of `com.base.app.core.model` anywhere**, yet `api` in every data module and `implementation` in every feature |
| `MviTestScope` / `MviViewModel.test {}` | `core/testing/.../MviTestScope.kt` | 0 in template; 0 in PF; 0 in VF |
| `mockk` as `api` dependency | `core/testing/build.gradle.kts:16` | 0 in template (puts mockk, byte-buddy and objenesis on every test classpath) |
| Outer `SLOW_MILLIS` | `core/common/src/test/.../MviViewModelTest.kt:62` | 0 |
| `search_remove_recent` | `feature/search/src/main/res/values/strings.xml:12` | 0 |
| `SettingsEvent.BackClicked` / `SettingsEffect.NavigateBack` | `Settings.kt:97,101,158,194` | never sent |
| `MessagingSurface`, `asStringOrEmpty` (core/ui, adjacent) | `core/ui/.../MessageHost.kt:54`, `UiTextExt.kt:15` | 0 |

**F9 detail.** `core:model` is empty here. In VF it became
`core/model/src/main/kotlin/com/vitfleet/app/core/model/api/ApiModels.kt`: 3,679 lines of DTOs
generated from OpenAPI, imported by 18 feature modules (`feature/accounts` alone imports it in 36
places). Two things drive this:
- Data modules may not share types, so shared DTOs sink to `core:model`.
- Nothing stops features from consuming wire DTOs.

The result is that **any backend schema change invalidates `core:model` and therefore every data
and feature module.** Give `core:model` a charter (cross-domain value types and ids only, never
DTOs), enforce it with a lint or Konsist test, or remove it from the template and the conventions.

---

## 8. Network monitor (`core/common/network`)

### F4 (high): one system callback registration per request
`ConnectivityNetworkMonitor.isOnline` is a cold `callbackFlow` (`NetworkMonitor.kt:33-80`).
`KtorNetworkClient.execute` calls `networkMonitor.isOnline.first()` on every request (line 84), again
on each IO failure (line 135) and again when completing a failure (line 166). Each `first()` does
the following:
1. `registerNetworkCallback`, a binder IPC (line 70).
2. Seeds a value, possibly a second IPC (line 74).
3. Unregisters (line 76).
4. Hops threads (`flowOn(ioDispatcher)`, line 80).

Concurrent requests (paging, prefetch, retries, image loads behind the client) multiply the number
of live registrations. Android throws `TooManyRequestsException` at about 100 per app.

VF fixed exactly this (`VF:core/common/.../NetworkMonitor.kt`) with
`.shareIn(scope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), replay = 1)`. Its
KDoc says:

> "the system allows an app only about a hundred at once"

VF also added a Robolectric test asserting a single registration after 25 requests
(`VF:core/common/src/test/.../ConnectivityNetworkMonitorTest.kt`). **Backport both.**

### F5 (medium): pre-flight gating on VALIDATED
- The monitor only counts networks with `NET_CAPABILITY_VALIDATED` (`NetworkMonitor.kt:65-68`,
  seed `83-86`).
- `KtorNetworkClient.kt:84-86` returns `unreachable(... deviceIsOffline = true)` **without trying**
  when that is false.
- Android leaves networks unvalidated when its connectivity probe is blocked, even though they reach
  the app's own backend fine. Examples: corporate firewalls, some VPNs, regions where the probe host
  is blocked, and the gap right after a Wi-Fi handover.
- In those environments the app is permanently "offline" and queues every mutation.

**Fix:** never gate on the monitor. Always attempt the request, classify the `IOException`, and use
the monitor only for UI hints and replay triggers. Or gate only on `NET_CAPABILITY_INTERNET`.

### F23 (low): Android-bound "common"
`core:common` contains the following Android dependencies, which make it unshareable with a Compose
Multiplatform or SwiftUI generator:
- `ConnectivityManager`, plus a manifest permission (`AndroidManifest.xml:10`).
- `android.util.Log` (`AppLogger.kt:3`).
- `Context` and `@StringRes` (`UiText.kt:3-5,27-32`).
- `R.string` (`AppResultMessages.kt:11-16`, `Validators.kt:3`).
- The `SavedStateHandle` API (`build.gradle.kts:16`).

Split it into `core:common` (pure Kotlin: `AppResult`/`AppError`, `AppClock`, validation rules
returning error *codes*, coroutine qualifiers merged from `core:coroutines`) and Android-side
`core:connectivity` / `core:ui` (UiText resolution, logging backend). `core:coroutines` is an
Android library module for two files and could merge into the pure module.

---

## 9. Module structure: what scales, without over-engineering small apps

### 9.1 What the two real apps tell us

| Pressure | PeriFlow (9 features, 7 data) | VitFleet (18 features, 16 data) |
|---|---|---|
| Cross-feature navigation | Keys moved into `core/navigation/Destinations.kt` with comments like "Registered by `:feature:log`". The coupling is unenforced, and a key with no registration crashes at runtime | Per-feature `XLinks` interfaces (e.g. `VF:feature/daily/DailyNavKeys.kt:25-40`) implemented by hand in `VF:app/di/FeatureBindingsModule.kt` (292 lines, 16 providers) |
| Shared types between data domains | n/a | All DTOs in `core:model` (3,679 lines), imported by features |
| Business logic spanning domains | `data:tracking` used by 6 of 9 features | `feature:daily` depends on 5 data modules; the ViewModels hold pricing and quantity rules on wire DTOs (`LoadForm.kt` ~765-line VM, functions such as `TripsTripDto.toForm()`, `LoadForm.loadedKl()`) |
| Every feature needs the session | n/a | **Every** feature depends on `:data:auth` |

The template's rule table (`build-logic/.../ModuleGraph.kt:64-69`) has three gaps:
- It permits `FEATURE → :core:, :data:` and nothing else, with no `api` modules, so cross-feature
  contracts have nowhere to live.
- Data impl classes are `public` (`DefaultSampleRepository`, `DefaultSearchRepository` and others,
  and DTOs such as `SampleDto` and `FeedPostDto`), so features can and do bind to wire types.
- There is no home for cross-domain rules other than ViewModels.

### 9.2 Proposal: two generator modes, same rules engine

**Mode A, "compact" (default; solo or small team, up to about 10 features).** Keep today's shape,
tightened:

```
:app                          composition root only
:core:common                  pure Kotlin: AppResult/AppError, AppClock, dispatcher qualifiers, Validators (codes)
:core:{designsystem, ui, navigation, network?, database?, datastore, testing}
:data:<domain>                public: Repository interface + domain models
                              internal: DTOs, mappers, Default*Repository, Room DAOs, Hilt module
:feature:<name>               internal: everything except its NavKeys and nav module
```

Changes from today:
- Make DTOs and `Default*` classes `internal`. Hilt `@Binds` in the same module works with
  `internal` implementations.
- Delete `core:model` and `core:coroutines` (merge them into `core:common`).
- Strip unneeded convention dependencies (F20).
- Shared nav keys go in a generated `core:navigation/SharedDestinations.kt` with a
  registration-completeness test: every key in the file has exactly one `NavGraphEntry`. That is
  PeriFlow's approach, made safe.

**Mode B, "modular" (opt-in; many teams, many features, million users):**

```
:feature:<name>:api      NavKeys (+ result contracts, deep-link patterns). Pure Kotlin/JVM, @Serializable only.
                         deps: :core:navigation-api (AppNavKey interface only)
:feature:<name>:impl     screens, ViewModels, nav registration, strings.
                         deps: :feature:*:api, :data:*:api, :domain:*, :core:*
:data:<domain>:api       Repository interfaces + domain models. Pure Kotlin (KMP-ready).
:data:<domain>:impl      Ktor/Room/DataStore, DTOs, mappers, Hilt bindings.
                         deps: own :api, :core:network, :core:database, :core:datastore
:domain:<area>           OPTIONAL, only for rules spanning ≥2 data domains (VitFleet's daily/trips/fuel pricing).
                         deps: :data:*:api, :core:common. No Android.
:core:*                  as above; :core:navigation split into -api (AppNavKey) and the host.
:app                     depends on every :impl; wiring only.
```

`ModuleGraph.kt` rules for mode B:
- `feature:*:api` → `core:navigation-api`, `core:common`
- `feature:*:impl` → `feature:*:api`, `data:*:api`, `domain:*`, `core:*` (never another `:impl`)
- `data:*:api` → `core:common`
- `data:*:impl` → its own `:api`, `core:*` (never another data module's `:impl`; may read another
  domain's `:api` only through `domain:*`)
- `domain:*` → `data:*:api`, `core:common`

Also include `testImplementation` and `testFixtures` in the checked configurations; today only
`implementation`, `api` and `compileOnly` are checked (`ModuleGraph.kt:54`).

**Why this scales:**
1. **Build avoidance.** Editing a screen or a repository changes only an `:impl` ABI. Dependants
   compile against the `:api` modules, which change rarely. In VF today, regenerating DTOs
   recompiles all 34 data and feature modules.
2. **Ownership.** A team owns `feature:x:*` and `data:x:*`. Cross-team contracts are the small
   `:api` modules, which can be reviewed by CODEOWNERS.
3. **Typed cross-feature navigation.** `navigator.navigate(ArticleKey(slug))` compiles only if
   `:feature:learn:api` is a dependency. That replaces both PF's shared core file and VF's
   hand-written `Links` providers.
4. **KMP and SwiftUI readiness.** `:data:*:api`, `:domain:*`, `:feature:*:api` and `:core:common`
   are pure Kotlin. They are exactly the modules a CMP generator can share, and their shape maps
   one-to-one onto Swift packages for the SwiftUI generator.
5. **No blanket UseCase layer.** Neither real app wrote a single `UseCase` (0 matches), and neither
   suffered for it except where rules span domains. So `:domain:*` is opt-in per area, not a layer
   every feature must pass through.

**Why not make it the default:** for a 3-to-6 feature app, mode B doubles the module count and
configuration time for no benefit. The generator can scaffold `feature_modules` in either mode from
the same templates, since only `build.gradle.kts` files and package visibility differ.

---

## 10. Tests and testability (F14)

Per-module `@Test` counts in the audited area:

| Module | Main lines | Tests |
|---|---|---|
| feature/auth | 991 | **0** |
| feature/settings | 683 | **0** |
| feature/onboarding | 302 | **0** |
| feature/feed | 241 | **0** |
| feature/sample | 594 | 6 (list VM only; detail VM untested) |
| feature/search | 520 | 4 (`SearchResultViewModel` untested) |
| feature/profile | 260 | 3 |
| data/auth | 190 | **0** (token persistence and expiry math untested) |
| data/profile | 75 | **0** |
| data/feed | 132 | 4 (only the `nextPage` helper and mapper; `load()`/`getRefreshKey` untested) |
| data/sample | 121 | 5 (mapper only; cache policy untested) |
| data/search | 128 | 4 |
| core/common | 725 | 2 (no tests for `userMessage`, Validators, UiText, NetworkMonitor, or the handler-error-keeps-loop-alive guarantee) |
| core/testing | 116 | n/a; `MviTestScope` unused |

**Quality issues:**
- **`MviViewModelTest.kt:58`:** `private suspend fun advanceUntilIdleCompat() = delay(SETTLE_MILLIS)`
  is a 1-second virtual delay named as if it were a compatibility shim. `runTest(dispatcher)`
  shares the Main test dispatcher, so `advanceUntilIdle()` works, and the sibling tests in
  `feature/*` use it. The test also re-implements `MainDispatcherRule` (lines 20-24) instead of using
  `core:testing`. That is not possible because `core:testing` depends on `core:common`, which is
  another sign the base class belongs in its own `core:mvi` module that both can depend on.
- **Fakes are good.** All feature tests use hand-written fakes, with no mockk. Drop the `mockk`
  `api` dependency from `core:testing`. VF uses mockk in 16 files, so offer it per module, not
  globally.
- **Missing critical-path tests:**
  - sign-in success emits `SignedIn` and writes tokens;
  - a 401 maps to the wrong-credentials message;
  - a 422 applies field errors;
  - double submit sends one request;
  - Back during load is handled immediately (F1);
  - a scaffold integration test decoding a real JSON fixture (F2).

---

## 11. i18n (F22)

- **Data layer.** `data/sample/.../remote/SampleDto.kt:21` has `title.trim().ifBlank { "Untitled" }`,
  an English literal produced in the data layer and shown in the UI. Map it to `null` and let the UI
  choose a string resource.
- **Developer message.** `feature/auth/.../strings.xml:27` defines `auth_google_not_configured`:

  > "Google sign-in is not set up yet. Add the web client id to auth_google_server_client_id."

  This is developer-facing text the generator makes translators translate into 5 languages. Log it,
  and use a generic user-facing failure string instead.
- **Resolved strings in state.** `SignInEvent.GoogleFailed(val message: String)` → `UiText.Dynamic`
  (`SignIn.kt:61,89`) puts a resolved string into state. Pass a reason enum.
- **Untranslated overload.** `MviViewModel.showMessage(text: String, …)` (`MviViewModel.kt:159-161`)
  is an open door for untranslated copy, and it is unused. Remove it.
- **Mostly good.** Plurals are used correctly (`core/common/.../strings.xml:16-27`), feature strings
  live in feature modules, and only `search_remove_recent` is unused.
- **Repeated header comment.** The same 6-line XML header ("Every piece of copy this feature
  renders. Here rather than in the app module because …") is pasted into four `strings.xml` files.
  Keep it once in docs.

---

## 12. AI-slop and comment quality (F25)

Overall the comments in this area are mostly "why" comments and are better than typical generated
code. The specific problems:

- **A comment claims a property the code does not have.** `Validators.kt:6-9`:

  > "A sealed result rather than a nullable error string, so a validator that returns "no error"
  > and one that was never run are different things."

  `ValidationResult` has only `Valid` and `Invalid`, so "never run" is not representable.
- **Duplicated and misplaced KDoc.** `Onboarding.kt:49-59` has a KDoc, then `@Immutable`, then a
  second KDoc block before `data class OnboardingPage`. One of them is orphaned.
- **Misnamed helper and dead constant.** `MviViewModelTest.kt:58-63` defines
  `advanceUntilIdleCompat` (see §10) and a duplicate, unused `SLOW_MILLIS`.
- **What-comments restating the name.** `/** Sign in. */` (`SignIn.kt:71`), `/** Create an
  account. */` (`SignUp.kt:58`), `/** Search as you type. */` (`SearchViewModel.kt:38`).
- **Over-long essays for trivial facts.** `core/common/src/main/AndroidManifest.xml:2-7` spends five
  lines on one permission. `UiContracts.kt:3-14` spends 12 lines to justify three empty marker
  interfaces. Generic bounds on member-less marker interfaces add no type safety; they read as
  cargo cult.
- **Misleading justification.** `data/auth/build.gradle.kts:10`:

  > "a repository in front would only forward"

  It sits on a dependency the convention plugin already adds, so the comment implies a decision
  that does not exist.

---

## 13. Backport list from PeriFlow and VitFleet (in this area)

1. VF `ConnectivityNetworkMonitor` `shareIn(WhileSubscribed(5s, replayExpiration = 0), replay = 1)`
   plus `ConnectivityNetworkMonitorTest` (F4).
2. VF `MessageHost` `Shown` wrapper (F15). PF per-`MessageKind` haptics in `MessageHost`.
3. PF `AppClock` / `SystemAppClock` (`PF:core/common/.../time/AppClock.kt`), including the `days()`
   flow that rolls over at midnight and handles time-zone changes, plus `FakeAppClock` in
   `core:testing`.
4. VF `MviViewModel.cancelLatest(key)`. Better still, adopt the `launchExclusive` and `launchLatest`
   policy set from F1.
5. PF `AppResult.Failure.errorCode` and VF `ApiError`/`FormFeedback` (warnings vs blocking field
   errors). Generalise them into the typed `AppError.Http(apiCode, fieldErrors, warnings)` (F11),
   with the envelope parsing in `core:network`'s `ResponseUnwrapper`.
6. VF `DestinationCatalog` (`VF:core/navigation/NavRegistry.kt:96-104`): lets a hub screen show
   only sections whose destinations are registered. Useful for optional features in generated apps.
7. **Do not backport:**
   - VF's DTO dump in `core:model`;
   - PF's unenforced shared-keys file (replace it with the mode-A test or mode-B `:api` modules);
   - VF's `runCatching { first() }` (cancellation swallowing);
   - PF's 8 forever-collectors in `init` (replace them with `observe`, F12).
