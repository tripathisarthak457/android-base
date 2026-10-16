# VitFleet → android-base: learnings to port back

Scope: `vitfleet-android` (read-only), diffed against `android-base/template` (branch `overhaul/multiplatform-v2`, HEAD `99c4edf`).

Paths used below:

- `VF/` = `/home/crazyhusir/Documents/projects/tools/vitfleet/vitfleet-android/`
- `T/` = `/home/crazyhusir/Documents/projects/tools/android-base/template/`
- `G/` = `/home/crazyhusir/Documents/projects/tools/android-base/generator/`
- `…/` = `src/main/kotlin/com/<vitfleet|base>/app/`
- Nav3 source used to check claims: `.overhaul/scratch/nav3src` (navigation3 1.1.7, the version both projects pin).

## 0. Method and baseline

- VitFleet's first commit `25db943` ("chore: import VitFleet specs, design and Android template", 2026-10-04) holds the generated project untouched: 476 files. The template's last commit is `99c4edf` (2026-09-24), so it has not changed since. I checked that the import equals the template after the rename (`com.base.app`→`com.vitfleet.app`, `BaseApp`→`VitFleet`): `NavTransitions.kt`, `AppNavHost.kt` and `AppBottomBar.kt` differ only by blank lines left where markers were stripped. **So `git diff 25db943 HEAD` on generic paths is exactly the hand work done on top of the template.** That snapshot is extracted at `.overhaul/scratch/vitfleet/base/`.
- 265 commits touch `vitfleet-android/`. About 110 of them touch generic paths (`build-logic`, `core/*`, the app shell, `catalog`, `config`, `gradle`). The diff over those paths is 288 files and about 19k lines. Most of the lines are VitFleet-only (generated `ApiModels.kt` at 3,679 lines, 60 vector icons, Lottie JSON, SDUI, fleet widgets).
- Each item marked **[template bug]** was checked by reading the current template file at the line cited. Code inferred only from a commit message is not reported.
- `vitfleet-android/AGENTS.md`, `docs/ARCHITECTURE.md`, `README.md` and `benchmark/` were never edited. No documentation learnings come from the Android folder. The repo-root `CHANGELOG-DECISIONS.md` (rows A9-1b, AX-10, AX-11, TA-7, TA-10, PB2-5, PB3-6) and `CLAUDE-CODE-PROMPT.md:124` record the reasoning and are quoted where relevant.
- VitFleet has **no Kyant backdrop or liquid glass** (no `kyant` or `backdrop` in any `.kt`, `.kts` or `.toml`). Liquid-glass learnings must come from PeriFlow. §4.4 lists VitFleet patterns that would get in the way of a glass bar.

---

## 1. Template and generator bugs VitFleet had to fix by hand

Ordered by severity. Each one is still present in the template today.

### 1.1 Session and network

#### B1. Ktor's cached bearer token outlives sign-out and account switches **[template bug, critical, security]**

- VitFleet `53da40b` "fix(auth): switching company keeps working after the first request": *"Ktor's bearer plugin kept the old company's token in memory after the new one was saved; whoever saves tokens now drops that copy, and so does sign-out."*
- Template: `T/core/network/…/core/network/di/HttpClientModule.kt:97-101`. `loadTokens { … BearerTokens(access, …) }` runs once and Ktor keeps the non-null answer until `clearToken()` is called. A search of the template finds no `clearToken` or `authProvider` anywhere.
- How it fails: user A signs in, so A's token is cached. A signs out, which clears `AuthTokenStore` but not Ktor's copy. B signs in; the sign-in call carries `SkipAuthAttribute`, so no token is sent. B's next authenticated request carries **A's** cached access token. The template's sign-out never revokes the token on the server, so that request runs as A until the token expires.
- VitFleet fix: `VF/core/network/…/core/network/auth/BearerTokenCache.kt` (whole file). `KtorBearerTokenCache` calls `client.get().authProvider<BearerAuthProvider>()?.clearToken()`, is a `SessionScopedStore`, and is dropped by whoever saves tokens. The client is injected as `Provider<HttpClient>` to break the DI cycle.
- Port: as-is. Also call `drop()` in `AuthRepository.persist()` after `tokenStore.save` (`T/data/auth/…/AuthRepository.kt:109-121`).

#### B2. A 408 or 429 on refresh signs the user out *and* deletes their offline writes **[template bug, high, data loss]**

- VitFleet `b445966` "fix(network): one refresh at a time, and only a 401 ends the session", and `c61f279` "fix(network): never lose an entry queued offline".
- Template chain:
  1. `T/core/network/…/di/HttpClientModule.kt:126-129`: `is TokenRefreshResult.Failure -> { if (result.code in REFUSED_RANGE) endSession(…) }`, with `REFUSED_RANGE = 400..499` at `:198`. A 429 (rate limited) or 408 from the refresh endpoint therefore ends the session.
  2. `endSession` → `sessionEvents.notifyExpired()` → `T/app/…/session/SessionCoordinator.kt:44` `sessionEvents.expired.collect { signOut() }` → `:51` `stores.forEach { store.clear() }`.
  3. `T/core/network/…/ResponseCache.kt:162-164`: `class RoomRequestQueue … : RequestQueue, SessionScopedStore`. The offline queue is one of those stores, so it is wiped.
- VitFleet fix:
  - `VF/core/network/…/auth/SessionTokens.kt:31-63`. One `Mutex`. A stale refresh token that someone else already rotated is answered from the store. Tokens saved during the call (sign-in, switch) are neither overwritten nor ended. Only `401` (`SESSION_REFUSED`) calls `end()`.
  - `HttpClientModule.kt:107` and the socket renewal both go through it, which also fixes the race where two refreshes presented one refresh token and the backend revoked the family.
  - The queue is no longer session-scoped. Entries are owner-bound instead: `VF/core/network/…/QueueOwnership.kt` plus Room schema v2 with a migration (`VF/core/network/schemas/…/2.json`, `NetworkDatabaseMigrationTest.kt`).
- Port: `SessionTokens` as-is, since it is generic. For the queue, either keep it session-scoped but stop wiping it on an *involuntary* expiry (ask first), or port the owner tagging. The owner tagging depends on a user id the template does not have, so it would read from `AuthTokenStore` or a `UserIdProvider`.

#### B3. The offline replayer deletes entries on any 4xx, including 401, 408 and 429 **[template bug, high, data loss]**

- Template `T/core/network/…/ResponseCache.kt:208-240`, KDoc: *"A request the server answers with a 4xx is dropped."* Code `:233-236`: `if (result.code !in REFUSED_RANGE) return … queue.remove(queued.id)`. A replay that meets an expired session (401), a timeout (408), an old app (426) or a rate limit (429) silently loses the user's entry.
- Replay also only happens on an offline→online edge (`:221` `isOnline.distinctUntilChanged().filter { it }`). Entries queued while the *server* was down are never retried until the device goes offline and online again.
- VitFleet fix: `VF/core/network/…/ResponseCache.kt:352-451`.
  - `refusesEntry()` (`:441-451`) treats `401, 408, 426, 429` and the codes `TENANT_MISMATCH` and `REQUEST_IN_PROGRESS` as momentary. A refused entry is kept and marked (`markRefused`), not deleted.
  - Passes run when the network comes back, when the queue grows, and when the app comes to the front (`replaySoon()`), and again after 30 s doubling to 5 min while entries remain.
  - A `Mutex` stops two passes sending one entry twice.
  - Replays carry `X-Queued-At`.
  - The app shows a refused-entries notice and an "Unsent entries" screen (`VF/app/…/sync/UnsentEntries*.kt`, `UnsentNotice.kt`).
- Port: the replay loop and `refusesEntry` (with no VitFleet codes in it), plus a minimal outbox screen behind an `outbox` option.

#### B4. The WebSocket never connects **[template bug, high]**

- VitFleet `HttpClientModule.kt:57-59`: `// AppWebSocket opens its socket on this client; without the plugin the upgrade fails.` `install(WebSockets)`.
- Template: `AppWebSocket` uses `@PlainClient` (`T/core/network/…/websocket/AppWebSocket.kt:50`). `providePlainClient` (`T/…/di/HttpClientModule.kt:51-65`) never installs `WebSockets`, and no `install(WebSockets)` exists anywhere in the template. The `websocket` feature only adds the dependency (`T/core/network/build.gradle.kts:31-33`). `client.webSocketSession(...)` throws "Plugin WebSockets is not installed", the `catch (Throwable)` at `:93-95` logs it, and the loop retries forever with backoff. (Also found independently in `template-infra.md:270-279`.)
- Port: a dedicated realtime client with `install(WebSockets) { pingInterval = … }`.

#### B5. With the socket fixed, debug builds kill it within milliseconds, and backoff never grows **[template bug, medium, latent]**

- VitFleet `81e12c6`: *"The dev-tools recorder's ResponseObserver split the body of the WebSocket upgrade, which made OkHttp fail the socket milliseconds after it opened. The reconnect loop reset its backoff on every open, so the app reconnected every second."*
- Template `T/core/network/…/NetworkRecording.kt:16`: `install(ResponseObserver)` has no filter and is installed on the plain client (`HttpClientModule.kt:62-64`). Separately, `AppWebSocket.kt:82` sets `attempt = 0` right after opening, before any frame arrives.
- VitFleet fix: `VF/core/network/…/NetworkRecording.kt:20` `filter { call -> !call.request.url.protocol.isWebsocket() }`, and `AppWebSocket.kt:101` resets `attempt` only inside the frame loop.
- Port both.

#### B6. Races in `AppWebSocket.disconnect()` and `connect()` **[template bug, medium]**

- VitFleet `e6ced8f` and `d76596c`. Template `AppWebSocket.kt:112-120`: `disconnect()` closes "whatever session is current when its coroutine runs", and the `finally { session = null }` at `:97-99` can clear the socket that a restart just opened. `connect()` (`:66-68`) is not synchronized, so two callers on different threads can both start a connection loop, and every event arrives twice.
- VitFleet fix: `VF/…/websocket/AppWebSocket.kt:72` and `:130` (`@Synchronized`), `:116` `if (session === opened) session = null`, and `:136` `val closing = session` taken synchronously.
- Also generic: `connect(path: suspend () -> String, headers: suspend () -> Map<…>)` is evaluated on every attempt, so a reconnect resumes from a cursor and carries current headers.

#### B7. The connectivity monitor registers a new `NetworkCallback` per request **[template bug, medium, perf]**

- VitFleet `f3111ef` and `2ec05a8`. Template `T/core/common/…/network/NetworkMonitor.kt:33` `override val isOnline: Flow<Boolean> = callbackFlow { … }` is cold. `KtorNetworkClient` calls `isOnline.first()` up to three times per request (`T/…/KtorNetworkClient.kt:84, 135, 166`). Each call registers and drops a ConnectivityManager callback, and Android caps an app at about 100.
- VitFleet fix: `VF/core/common/…/network/NetworkMonitor.kt:91` `.shareIn(scope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), replay = 1)` with `@ApplicationScope`. The test `ConnectivityNetworkMonitorTest` asserts one callback across 25 calls.

#### B8. Idempotency keys only on queued POST and PATCH **[template bug, medium]**

- Template `T/…/KtorNetworkClient.kt:243-248`: `if (!enqueueOnFailure || repeatable) return this`. A POST that is not queued has no key, yet `RetryPolicy` can still resend it after a `Retry-After` (VitFleet `300b743`). PUT and DELETE never get one.
- VitFleet `d2a7ffa`: `VF/…/KtorNetworkClient.kt:247-249` gives every POST, PUT, PATCH and DELETE a key that is kept across retries and replay.

#### B9. `header()` appends, so request headers duplicate defaults **[template bug, low]**

- Template `KtorNetworkClient.kt:152-153`: `header(AcceptLanguage, …)` then `request.headers.forEach { header(k, v) }`. A request that sets its own `Accept-Language` sends two. VitFleet hit the same thing with context headers and the server read the first (`53da40b`). Fix: `VF/…/KtorNetworkClient.kt` `withRequestHeaders(context, request)`, where request headers replace defaults case-insensitively.
- Port: the merge, plus an optional `HeaderProvider` multibinding for per-call context headers (tenant, app version). VitFleet needed it (`ContextHeaders.kt`, `X-App-Version` in `NetworkConfig.appVersionCode`).

### 1.2 Navigation and app shell

#### B10. Rotation or resize rebuilds every tab's stack, ViewModels and saved state **[template bug, high]**

- VitFleet `00baeaf`: *"The shell called the navigation host from three branches (bar, rail, full screen), so opening a full-screen destination or rotating rebuilt every tab's stack; it is called from one place."*
- Template `T/core/navigation/…/AppShell.kt:195-225`: `TabbedNavHost` is called in the rail branch (`:202`) and again in the bar branch (`:218`), separated by `return@Box`. A phone turned to landscape (width ≥ 600 dp) moves to the other call site. That gives new `rememberSaveableStateHolderNavEntryDecorator` and `rememberViewModelStoreNavEntryDecorator` instances, so every screen loses its `rememberSaveable` state and its ViewModels.
- VitFleet fix: `VF/core/navigation/…/AppShell.kt:217-246`. `val wide …; val rail = wide && !fullScreen`. A single `Row { if (rail) Rail(); TabbedNavHost(…weight(1f)) }`, with the inset and `consumeWindowInsets` chosen by flags.

#### B11. Two `MainActivity` instances share one navigator `Channel`, so every other tap does nothing **[template bug, high]**

- VitFleet `d9990f2`: *"both activities' shells collected the one navigator channel, and each command went to one of them in turn: every other row or button tap on screen did nothing."*
- Template: `T/core/navigation/…/AppNavigator.kt:40-41` `Channel<NavCommand>(BUFFERED).receiveAsFlow()`, where each element goes to one collector. It is collected without lifecycle awareness at `AppNavHost.kt:56-58` and `AppShell.kt:171-173`. `MainActivity` has the default `standard` launch mode (`T/app/src/main/AndroidManifest.xml:44-48`). A notification's `PendingIntent` (`T/core/notification/…/AppNotifications.kt:105-112`) or a link from another app therefore creates a second instance.
- VitFleet fix:
  - `VF/core/navigation/…/AppNavigator.kt:72-76` `suspend fun AppNavigator.applyWhileStarted(lifecycle, apply)` = `lifecycle.repeatOnLifecycle(STARTED) { commands.collect(apply) }`.
  - Used at `AppNavHost.kt:69` and `AppShell.kt` (LaunchedEffect).
  - Test: `VF/core/navigation/src/test/…/AppNavigatorTest.kt`.

#### B12. Links and notifications stack a second copy of the app, and `onNewIntent` never fires for them **[template bug, high]**

- VitFleet `ae94915` (singleTask) and then `fa388a5` (singleTask cleared the camera and gallery picker, so it moved to singleTop plus a trampoline). Decision AX-11 in `CHANGELOG-DECISIONS.md:535`.
- Template: `MainActivity` (standard) holds the VIEW filters (`AndroidManifest.xml:44-…`). The comment at `T/app/…/MainActivity.kt:219-222`, *"A link that arrives while the app is running comes here, not to onCreate"*, is false under `standard`.
- VitFleet fix:
  - `VF/app/src/main/AndroidManifest.xml`: `MainActivity` gets `launchMode="singleTop"` and only the launcher filter.
  - `deeplink/LinkActivity` (`taskAffinity=""`, `excludeFromRecents`, `noHistory`, `Theme.NoDisplay`) owns the VIEW filters.
  - `VF/app/…/deeplink/LinkActivity.kt:27-42` resolves the link into `PendingDeepLink`, then starts `MainActivity` with the launcher's own intent and `NEW_TASK | CLEAR_TOP | SINGLE_TOP`.
  - `LinkEntryManifestTest.kt` locks both declarations in place.
- Port: as-is. It is generic and has no VitFleet logic in it.

#### B13. A deep link is pushed again on rotation and on launch from Recents, and is lost if it arrives before sign-in **[template bug, medium]**

- VitFleet `1c96fda`. Template `MainActivity.kt:179-183` calls `handleDeepLink(intent)` on *every* `onCreate`, re-creations included, and `:225-228` sends it to the navigator immediately. On the sign-in stack, the linked screen loads with no session and is gone once the user signs in.
- VitFleet fix:
  - `VF/app/…/MainActivity.kt:164-169`: only `savedInstanceState == null && !FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`.
  - `handleDeepLink` only `hold`s the link (`:178-181`).
  - The tabbed host takes it once it is up (`:138-142`) via `PendingDeepLink` (`VF/app/…/deeplink/PendingDeepLink.kt`, a singleton `MutableStateFlow` with `take()` = `getAndUpdate { null }`).
  - Test: `PendingDeepLinkTest`.

#### B14. Pushing another tab's root draws it on the current tab with no bar and no back arrow **[template bug, medium]**

- VitFleet `ae94915`. Template `AppShell.kt:172`: `state.current.apply(command)` for every command.
- Fix: `VF/…/AppShell.kt:86-99` `ShellState.apply(command, rootKeys)`. A plain `Navigate` to a tab's root key pops that tab to its root and selects it. Test in `ShellStateTest.kt`.

#### B15. A nav key with a property called `key` makes the whole back stack unsaveable, silently **[template bug, medium]**

- VitFleet `b661ac0`: *"A saved stack names each screen's type in a 'key' field, and SduiKey had a property of that name: encoding the stack threw, the stack was not saved, and after a background kill the app came back without it."*
- Template `T/core/navigation/…/NavKeySerialization.kt:40` `classDiscriminator = "key"`. The failure is swallowed at `AppBackStack.kt:89,93` and `AppShell.kt:128,135` (`runCatching { … }.getOrNull()` with no log).
- VitFleet fix: the key was renamed via `@SerialName`, and failures are logged (`VF/…/AppShell.kt:150,157`, `AppBackStack.kt` `AppLogger.e(…, NAV_LOG_TAG)`). `VF/app/src/test/…/navigation/NavKeyRegistrationTest.kt` (160 lines) scans the classpath for every `AppNavKey` and asserts each is registered in a `navKeys {}` block and round-trips with sample values.
- Better template fix: use a discriminator that cannot collide (`"#type"`), and register the serializer inside `navGraph { entry<T>() }` (`T/…/NavRegistry.kt` `entry<reified T>`, using `serializer<T>()`), so that the `navKeys {}` list and the `entry` list cannot drift. Keep the scan test, without the hard-coded `> 50` threshold.

#### B16. The back gesture scrubs a spring, every pushed screen is transparent, and push and pop fade towards the window colour **[template bug, high, UX: "pushy" transitions]**

- VitFleet `00baeaf` and decision A9-1b (`CHANGELOG-DECISIONS.md:229`): *"The template's transitions faded screens towards the window colour and drew pop in the wrong order."*
- Template:
  - `T/core/navigation/…/AppNavHost.kt:126-129` reuses `pop()`, a spring, for `predictivePopTransitionSpec`. `:187` does the same for the modal and fade metadata.
  - `NavTransitions.kt:30-33` puts `fadeOut(targetAlpha = 1 - outgoingDim)` on push, and `:43-46` and `:67-70` put matching `fadeIn`/`fadeOut` on pop and modal.
  - `AppNavHost.kt:155-168` paints a background only when `inset > 0.dp`, so every pushed screen (inset 0) is transparent unless it paints its own.
- VitFleet fix (`VF/…/NavTransitions.kt`):
  - Alpha is removed everywhere (`:30-39`, `:88-98`).
  - `predictivePop` (`:65-82`) runs on `tween(slow, LinearEasing)` so it tracks the finger, with the leaving screen scaled to 0.92.
  - `modalPredictiveDismiss` (`:113-123`) is linear too, and `predictivePopForStyle` (`:167-176`) wires them in.
  - Every entry is wrapped in an opaque `Box(background(background))` (`VF/…/AppNavHost.kt:213-226`).
- **Correction to VitFleet's diagnosis.** In Nav3 1.1.7, NavDisplay builds its own `ContentTransform` with `targetContentZIndex = targetZIndex` (`nav3src/ui/…/NavDisplay.kt:822-829`) and already lowers it on pop (`:673` `isPop || inPredictiveBack -> initialZIndex - 1f`). The template's pop was therefore **not** drawn in the wrong order. VitFleet's `.underneath()` (`NavTransitions.kt:126`) is dead code: do not port it. What VitFleet saw as "drawn on top" was the transparent entries combined with alpha fades, letting the two screens show through each other. The real fixes are the opaque entries and the removed alpha. Removing alpha also saves an off-screen compositing layer per animated frame (graphicsLayer alpha < 1 with overlapping draws).
- Also: `outgoingDim` is now unused in VitFleet. Remove it from `AppMotion` (`T/…/theme/AppMotion.kt:160`) when porting.

#### B17. Predictive back is off on Android 13–15 **[template bug, medium]**

- VitFleet `00baeaf` added `android:enableOnBackInvokedCallback="true"` to `<application>` (`VF/app/src/main/AndroidManifest.xml:17`). The template's `<application>` (`T/app/src/main/AndroidManifest.xml:7-17`) lacks it. On those OS versions NavDisplay's `predictivePopTransitionSpec` never runs.

#### B18. The entry decorators list is rebuilt on every composition **[template, low, hygiene]**

- Template `AppNavHost.kt:133-137` builds `listOf(...)` on every call. VitFleet `:175-179` wraps it in `remember(saveable, viewModels)`.
- VitFleet also remembers the entry provider (`:195`) and claims this stopped a push replay on tab switch. Nav3 1.1.7 `DecoratedNavEntries.kt:125` keeps entries per `remember(backStack.toList())`, so the provider is only invoked when the stack changes. That claim is wrong (it agrees with `template-navigation.md` §4). Remembering is harmless, but the comment is misleading. Port the `remember` without the comment.

### 1.3 Design system

#### B19. The 48 dp touch target grows the content, not the hit area **[template bug, medium]**

- VitFleet `1d12d1e`. Template `T/core/designsystem/…/foundation/Modifiers.kt:79` and `:124`: `.defaultMinSize(minWidth = minTouchTarget, minHeight = minTouchTarget)`. A short text link draws at the top of a 48 dp box, above the line it belongs to.
- Fix: `VF/…/foundation/Modifiers.kt:144-152`, a `layout {}` that measures the content, reports `max(size, 48dp)` and centres the placeable.

#### B20. A checkbox, radio or switch with no callback eats taps meant for its row **[template bug, medium]**

- VitFleet `87bfc58`. Template `T/…/component/selection/AppSelection.kt:67, 151, 215`: `toggleable(enabled = enabled && onCheckedChange != null …)`. A disabled `toggleable` still claims the press.
- Fix: `VF/…/AppSelection.kt:71, 160, 229`. With no callback the control has semantics only (`toggleableState`/`selected`). Test: `AppSelectionTest` (androidTest).

#### B21. The confirm button calls `onDismissRequest()` before `onConfirm()` **[template bug, medium]**

- VitFleet `892efe2`. Template `T/…/overlay/AppDialog.kt:163-164`. When dismiss clears the pending item in the ViewModel (the usual `pendingDelete = null`), confirm then acts on nothing.
- Fix: `VF/…/AppDialog.kt:164` `onClick = onConfirm`. The caller decides when to close. Test: `AppAlertDialogTest`.

#### B22. Pull to refresh reads a stale `isRefreshing`, starts a coroutine per scroll delta, and takes flings **[template bug, medium, perf and UX]**

- VitFleet `2c9ee59` and `4f8df6f`. Template `T/…/component/refresh/AppPullToRefresh.kt`:
  - `:50` `remember(thresholdPx, maxPullPx) { object : NestedScrollConnection { … isRefreshing … } }` captures the first composition's `isRefreshing` for good (`:66`, and `onPreFling`).
  - `:57` and `:68` call `scope.launch { offset.snapTo(…) }` on **every** scroll delta.
  - There is no `NestedScrollSource` check, so a fling's overscroll pulls the indicator.
  - The content is not translated (`:96`).
- VitFleet fix (`VF/…/AppPullToRefresh.kt`):
  - `currentRefreshing by rememberUpdatedState(isRefreshing)` (`:59`).
  - Synchronous `mutableFloatStateOf` offset updates.
  - `source != UserInput` guards (`:72, :80`).
  - The fling is consumed when the indicator is out.
  - Content gets `graphicsLayer { translationY = offset }`.
  - A `settleJob` that is cancelled when a refresh ends within a frame (`:116`). Test: `AppPullToRefreshTest`.
- **Keep, but change:** VitFleet still reads `offset` during composition (`if (offset > 0f)`, `progress`), which recomposes on every frame of the drag. Move those reads into `graphicsLayer`/`drawBehind`. VitFleet also shows the spinner for *any* `isRefreshing=true` (`:111-115`), not only one the user pulled, so a cached list flashes a spinner on every load. Make that opt-in.

#### B23. Bottom sheet: a tall form runs off screen, and the keyboard pans the whole window **[template bug, medium]**

- VitFleet `c54f4d3`. The template sheet (`T/…/overlay/AppBottomSheet.kt`) has no `verticalScroll`, uses `navigationBarsPadding()` (`:130`), and lacks `decorFitsSystemWindows = false`.
- Fix (`VF/…/AppBottomSheet.kt`):
  - `:79` `decorFitsSystemWindows = false`.
  - `:187` `windowInsetsPadding(safeDrawing.only(Bottom))` inside the surface.
  - `:237` content in `weight(1f, fill=false).verticalScroll(…)`, with a `NestedScrollConnection` that hands an overscroll at the top to the sheet drag.
  - `rememberUpdatedState(onDismissRequest)`, and a `showClose` × button.
- Note: the sheet still uses `scope.launch { snapTo }` per drag delta (`drag()`). Same perf remark as B22.

#### B24. The top bar's title runs under the second action icon **[template bug, low]**

- VitFleet `a07e5b5`. Template `T/…/navigation/AppTopBar.kt:65-87` reserves `titleEnd = edge + slot`, one icon. Fix: `VF/…/AppTopBar.kt:103-120, 135-165`. A start-aligned title takes `weight(1f)` between the navigation and actions rows.

#### B25. Tab labels are cut when an equal share is too narrow **[template bug, low]**

- VitFleet `946e7e8`. Template `T/…/AppTabRow.kt:66` `weight(1f)` and `:97` `tabWidth = maxWidth / tabs.size`. Fix: `VF/…/AppTabRow.kt:55-67`. Each label is measured once (`rememberTextMeasurer`); when any label does not fit its share, tabs are weighted by need, and the indicator lerps width and position. The indicator `position` is still read inside `layout {}` (deferred), which is good.

#### B26. A dropdown menu touches the screen edge **[template bug, low]**

- VitFleet `9e9f5bb`. Template `T/…/AppDropdownMenu.kt:41` `Popup(...)` uses default positioning. Fix: `VF/…/AppDropdownMenu.kt:89-100` `MenuPositionProvider(margin, gap)` keeps the gutter and opens upward when there is no room below. Test: `MenuPositionProviderTest`.

#### B27. In-app Dark theme on a light phone leaves dark status-bar icons on a dark app **[template bug, medium]**

- VitFleet `ef82943`, `8bc8392`, and `themes.xml`. The template calls `enableEdgeToEdge()` with no `SystemBarStyle` (`T/app/…/MainActivity.kt:121`). The default `SystemBarStyle.auto` follows the *system* night mode. Nothing in the template sets `isAppearanceLightStatusBars` (a search for `isAppearanceLightStatusBars` or `SystemBarStyle` finds nothing). The settings feature lets the user force Dark or Light (`T/feature/settings/…/Settings.kt:73,132` → `T/app/…/ui/AppRoot.kt:97-100`).
- VitFleet's fix is heavier: `VF/core/designsystem/…/foundation/AppStatusBar.kt`. Each screen declares the colour behind the status bar, and a per-window stack of tokens picks the icon colour (`:31-57, :76-96`). `AppStatusBarProtection` (`:62-66`) paints an opaque strip over everything.
- Port: a minimal fix in `AppTheme`, i.e. `SideEffect { insetsController.isAppearanceLightStatusBars = !dark; …NavigationBars = !dark }`. Port `AppStatusBar(background)` as an optional API for apps with coloured headers. **Do not** port `AppStatusBarProtection` as a default: an opaque strip defeats glass or translucent top chrome (§4.4).

#### B28. "Accent as text" is never contrast-checked; the generator can produce 2.3:1 **[template and generator bug, medium, a11y]**

- VitFleet `46f8917`, `97dc847` and `f284c22` fixed selected chips, text buttons, the selected tab and the bottom-bar label, which read as disabled in dark theme.
- Template components use `colors.accent` as *text* on a surface:
  - `T/…/button/AppButton.kt:229` (Tertiary `content = colors.accent`)
  - `AppTabRow.kt:59, 111`
  - `AppChip.kt:41, 46` (accent on `accentSubtle`)
- The test `T/core/designsystem/src/test/…/PaletteContrastTest.kt` only checks `onAccent on accent`. The generator's `brand_shades()` (`G/genkit/render.py:752-760`) makes each shade legible *under* white or ink text, never *as* text on a surface. Running the generator's own functions:

  | brand | light accent on White | dark accent on Ink800 (dark surface) |
  |---|---|---|
  | `#3A5278` (VitFleet navy) | 7.90 | **3.76** |
  | `#0A2540` | 15.54 | **2.29** |
  | `#F5C518` | **1.63** | 11.45 |
  | `#6C3BD1` | 6.60 | **4.38** |

- VitFleet fix: role functions `tertiaryButtonContent(colors)` (`VF/…/AppButton.kt:255`), `selectedTabContent` (`AppTabRow.kt:148`) and `selectedBottomBarContent` are `internal` so `PaletteContrastTest` can assert them. The selected chip became a fill (`AppChip.kt:48`). The off-switch thumb takes `contentTertiary` in dark (`AppSelection.kt`, `drawCircle`). The template has `colors.surface` at `AppSelection.kt:239`, which vanishes on the dark track.
- Port: add an `accentContent` (accent-as-text) role. The generator computes it to reach AA on `surface` and `background` in both themes. Components use it, and `PaletteContrastTest` checks it for every design style.

#### B29. Smaller design-system bugs **[template bugs, low]**

- `MessageHost`: the same message shown twice does not restart its timer. `UiMessage` is a `data class` (`T/core/common/…/mvi/UiMessage.kt:6`) held in structural-equality state (`T/core/ui/…/MessageHost.kt:24, 35`). Fix: `VF/core/ui/…/MessageHost.kt:27, 57` wraps each showing in a non-data `Shown` holder.
- `AppTimePicker` hard-codes `listOf("AM", "PM")` (`T/…/datetime/AppTimePicker.kt:41`), against the template's own translate-everything rule. Fix: `designsystem_time_am`/`_pm` strings (`d852cbb`).
- `AppCurrencyField` hard-codes 3-digit grouping (`T/…/input/AppSpecialFields.kt:319`). VitFleet hard-coded lakh grouping instead. Port a locale-driven grouping, not VitFleet's.
- `AppDatePicker` `remember { LocalDate.now() }` (`:61, :279`) stays stale across midnight and ignores the server's zone. VitFleet added a `today: LocalDate?` parameter.
- `AppSwipeToAction` draws square corners behind a rounded card (`T/…/list/AppSwipeToAction.kt:124`). Fix: a `shape` parameter (`c01037c`).

### 1.4 Platform

#### B30. A camera photo is lost when the camera app kills the process **[template bug, medium]**

- VitFleet `fd7125b`. Template `T/core/media/…/MediaPickerController.kt:59` `var pendingCameraUri by remember { … }`. Fix: `rememberSaveable` (`VF/…/MediaPickerController.kt:62`). (Siblings in `template-features-data.md` may list this too.)

#### B31. Below Android 13, the in-app language never reaches `Accept-Language`, and `wrap()` mutates shared `Configuration` **[template bug, medium]**

- VitFleet `9cec553`. Template `T/core/ui/…/locale/AppLocales.kt:39-46` only wraps the Activity context and calls `base.resources.configuration.apply { setLocales(…) }`, which mutates the base's own object. `KtorNetworkClient.kt:152` sends `Locale.getDefault()`.
- Fix (`VF/core/ui/…/AppLocales.kt:35, 49-67`): `Locale.setDefault` in `set()` and `wrap()`, a *copy* `Configuration(base.resources.configuration)`, `restoreDefault()` from `Application.onConfigurationChanged`, and `Application.attachBaseContext(AppLocales.wrap(base))` so notification channel names follow too. Robolectric test: `AppLocalesTest` (`@Config(sdk=[30])`).

#### B32. App lock: the content can show for a frame, overlays stay above the lock, and process death means unlocking again **[template design bug, medium]**

- Template `T/app/…/MainActivity.kt:203-210` decides in `onStart` by *launching a coroutine* that reads DataStore (`AppLock.shouldLock()` is `suspend`, `T/app/…/lock/AppLock.kt:30-35`), then starts `LockActivity`. The first frames of content draw before the lock activity appears, despite the comment "before the window is drawn". `lastVisibleAt` is in memory only, so a camera capture that kills the process forces an unlock. Dialogs, sheets and popups are separate windows and stay above the app until the lock activity covers them, and TalkBack can still read them.
- VitFleet (`3e5697a`, `4f42ec1`, `4c4c9a9`, decisions TA-7, TA-10, PB2-5) replaced this with `:core:applock`:
  - `VF/core/applock/…/AppLockController.kt`: `ProcessLifecycleOwner` start/stop. `lockIfDue()` decides synchronously from memory on the main thread, then `onForeground()` confirms from disk. The background time is persisted. Leaving while locked records nothing. PIN-pad trips during the prompt are ignored. A clock moved back locks.
  - The system splash is held while `state == Unknown` (`VF/app/…/MainActivity.kt:111`).
  - `AppLockLayer` (`VF/core/applock/…/AppLockLayer.kt`) is drawn over the composition.
  - `LocalOverlaysHidden` (`VF/core/designsystem/…/overlay/OverlayCover.kt`) makes `AppDialog`, `AppBottomSheet`, `AppDropdownMenu` and `AppTooltip` step aside.
  - `clearAndSetSemantics {}` under the lock (`VF/app/…/ui/AppRoot.kt`), `clearFocus(force = true)` so the keyboard does not come back, and `FLAG_SECURE` while the lock is enabled (`SecureWindow`). Tests: `AppLockControllerTest`, `SecureWindowTest`.
- Port: move the app lock out of `:app` into `:core:applock` with this design. Keep `AppLockPolicy` (pure and tested). Make `LockScreen`'s art a slot, since VitFleet's uses `AppBrandMark` and `VfIcons`.

#### B33. Build and tooling **[template and generator bugs]**

- **Generator: the monochrome icon regex writes malformed XML (high).** `G/genkit/icons.py:162-165`:

  ```python
  re.sub(r'(<monochrome[^>]*android:drawable=")@drawable/[A-Za-z0-9_]+(")',
         r"@drawable/ic_launcher_monochrome", text)
  ```

  The replacement drops both captured groups. Reproduced:

  ```
  <monochrome android:drawable="@drawable/ic_launcher_foreground" />
  →    @drawable/ic_launcher_monochrome />
  ```

  It runs on every generation with a custom icon (`icons.py:74`, called from `build.py:71`) and breaks the resource compile. VitFleet carries the fix as `vitfleet/tools/android-base-monochrome-fix.patch`, and `CLAUDE-CODE-PROMPT.md:124` notes: *"Known generator bug fixed locally"*. The fix is `r"\g<1>@drawable/ic_launcher_monochrome\g<2>"`. No generator test covers icons (`grep -rn monochrome G/tests` finds nothing). Add one.
- **Crashlytics uploads every release variant's mapping on each `./gradlew build` (medium).** VitFleet `2ef1f33` changed `VF/build-logic/…/AndroidFirebaseConventionPlugin.kt` to set `mappingFileUploadEnabled = providers.gradleProperty("crashlyticsUpload")`. The template has no such switch (a search for `mappingFileUploadEnabled` finds nothing), so a local or CI build with a placeholder `google-services.json` fails or makes network calls. Decision V-1.
- **Compose UI tests on Android 16 (low).** VitFleet `Compose.kt:+67` adds `androidTestImplementation(espresso-core)`: *"Android 16 removed the InputManager reflection used by older transitive Espresso."* In the template, `T/build-logic/…/Compose.kt:70` adds only `ui-test-junit4`.
- **`.gitignore` (low).** It lacks `gradle/gradle-daemon-jvm.properties` (VitFleet `5438393`), and the template's AGENTS.md forbids pinning the daemon JVM.
- **`fragment` pinned (low).** VitFleet `libs.versions.toml`: *"BiometricPrompt needs a FragmentActivity; biometric alone would pull fragment 1.2.5 from 2020."* The template uses `biometric` (`T/app/build.gradle.kts:123`) without pinning `androidx.fragment`.

---

## 2. Generic improvements worth porting (not bug fixes)

| # | What | Where in VitFleet | Why it is better | Port how |
|---|---|---|---|---|
| P1 | `SessionTokens`: one refresh at a time, with stale-token short-circuit | `core/network/…/auth/SessionTokens.kt` + test (123 lines) | Fixes B2 and serves socket and HTTP refresh alike | As-is |
| P2 | Lifecycle-aware realtime: socket runs while the app is in front and closes 30 s after it leaves; restarts with the new token after an account switch | `app/…/session/AppGate.kt` (KDoc) + `RealtimeClient` | Battery-friendly. The template's `AppWebSocket` has no lifecycle policy | As a generic `RealtimeLifecycle` in `:core:network`, without VitFleet's depot subscription |
| P3 | `ApiSignals`: app-wide reactions to 426 (force update), 503 maintenance, 402 locked and 403 role changed, raised from the network layer and collected by the shell | `core/network/…/ApiSignals.kt` (51 lines) + `KtorNetworkClient` `signals.raiseFor(...)` | Screens stop handling global states one by one | Generic with configurable code-to-signal mapping. Pairs with an optional server-driven min-version gate (`X-App-Version`, `NetworkConfig.appVersionCode`) |
| P4 | `AllPages` (`getAll`, `getAllStream`) follows `next_cursor`, emits nothing partial, and shows a cached copy only when all pages are cached | `core/network/…/AllPages.kt` + test (134 lines) | Lists silently stopping at 100 rows looked complete | Generic, cursor field name configurable |
| P5 | `ObjectStorage`: pre-signed PUT and GET on the plain client, streaming with progress, never sending `Authorization` | `core/network/…/storage/ObjectStorage.kt` | S3, R2 and GCS refuse a pre-signed request that carries an extra auth header. Streaming avoids `bodyAsText` on binaries | Optional, with `media` |
| P6 | `FileShare` + `ExportMenu`: download to cache, share with FileProvider, save to Downloads via MediaStore (no permission on API 29+) | `core/ui/…/share/FileShare.kt`, `ExportMenu.kt`, test | Common need; easy to get wrong | Optional `share` feature (strip `ExportLink` and VitFleet types) |
| P7 | `PendingDeepLink` + `LinkActivity` trampoline + `LinkEntryManifestTest` | §1.2 B12/B13 | Correct link delivery | Replace the template's deep-link handling |
| P8 | `ShellState.apply(command, rootKeys)`, `FullScreenKey`, `LocalCanNavigateUp` | `core/navigation/…/AppShell.kt:86-99, 200`; `AppNavKey.kt:19-23`; `AppNavHost.kt:153-160, 225` | A screen that is both a tab root and a pushed destination shows Back only when pushed (PB3-6). A full-screen destination hides bar and rail without a second host | As-is |
| P9 | `DestinationCatalog` (`registry::has`) | `core/navigation/…/NavRegistry.kt:91-104`, `di/NavigationModule.kt` | Hubs can show an entry only if its feature is installed. Fits the generator's optional features | As-is |
| P10 | `sharedHero(key)`: one `SharedTransitionLayout` per stack, scopes passed per entry, off under reduce motion and two-pane | `core/designsystem/…/animation/SharedHero.kt`; `AppNavHost.kt:134-163` | List-to-detail continuity with no Nav3 types in features | Optional (`motion` extra). A `SharedTransitionLayout` per stack adds an overlay layer, so keep it off by default and check FPS |
| P11 | `rememberCountUp`, `Modifier.popWhenOn` | `core/designsystem/…/animation/Emphasis.kt` | Small, cheap emphasis. `popWhenOn` animates `graphicsLayer` only | As-is |
| P12 | `Shape.groupItem(first, last)` | `core/designsystem/…/foundation/GroupShapes.kt` | Grouped iOS-style lists inside a `LazyColumn` (one card from separate items) | As-is |
| P13 | `AppText(autoSize = …)`, `AppFiguresText` (words in body face, figures in mono) | `component/text/AppText.kt:38-48, 125-153` | Fit-to-width labels; tabular figures in mixed text | `autoSize` as-is; `AppFiguresText` with a generic figure regex (drop the `₹`) |
| P14 | `AppTextField(prefix = …)` + error fill | `component/input/AppTextField.kt` | Phone and currency prefixes; error state readable at a glance | As-is (add a `fieldErrorFill` token) |
| P15 | `AppErrorState(illustration = …)` + states eased in with `AppAppear` | `component/feedback/AppStateViews.kt` | Slot for Lottie art; no flash when a state replaces content | As-is |
| P16 | `AppBottomSheet(showClose = …)` | `overlay/AppBottomSheet.kt:64, 208-224` | Common design ask | As-is |
| P17 | `AppDataTable` (sticky header, shared horizontal scroll, edge fade, totals, widths measured from the widest figure per column) | `component/data/AppDataTable.kt` + `DataTableWidthsTest` | A real dashboard and report table, tuned for perf (`7b4ba49` measures one figure per column) | Optional component (admin and dashboard apps) |
| P18 | `AppDecimalField`/`AppMoneyField` (`DecimalInput` rules unit-tested), `AppDateRangeField`, `AppMonthPicker` | `component/input/AppDecimalFields.kt`, `component/datetime/AppDateFields.kt` | Typed-as-said decimal entry is hard to get right | Optional `forms` extras, with locale-driven grouping |
| P19 | `MviViewModel.cancelLatest(key)` | `core/common/…/mvi/MviViewModel.kt:+148` | Pairs with `launchLatest`; a reload cancels a stale page load | As-is |
| P20 | `-PdevHost=` / `-PdevServer=` | `build-logic/…/Flavors.kt:12-31` | Point the dev build at a USB phone (`adb reverse`) or a deployed server without editing code | Generic: `-PapiBaseUrl=` override for non-shippable flavours |
| P21 | `-PcrashlyticsUpload=true` | `AndroidFirebaseConventionPlugin.kt` | §1.4 | As-is |
| P22 | Structured error payload (`ApiError`: code, warnings, data) and `FormFeedback` (errors vs. confirmable warnings) | `core/common/…/ApiError.kt:65-116` | The template's `AppResult.Failure` has only `message`, `fieldErrors` and `rawBody` (`T/core/common/…/AppResult.kt:14-32`). Every real backend has machine codes | Add `errorCode: String?` and `retryAfter` to `Failure`, parsed by the `ResponseUnwrapper`. Keep the VitFleet codes out |
| P23 | Robolectric-based unit tests for platform code | `ConnectivityNetworkMonitorTest`, `AppLocalesTest`, `SecureWindowTest`, `NetworkDatabaseMigrationTest`, `AppBrowserTest` (`@Config(sdk=[35])`: *"Robolectric refuses SDK 36 and above on a Java 17 toolchain"*) | The template has 16 unit tests, and only `AppDatabaseMigrationTest` uses Robolectric | Port the generic ones with their fixes |
| P24 | `openInAppBrowser(): Boolean` | `core/ui/…/browser/AppBrowser.kt` | Callers know whether anything opened | As-is |

---

## 3. Do not port as-is

1. **`.underneath()` / `targetContentZIndex = -1`** (`VF/…/NavTransitions.kt:125-126`). This is dead code under NavDisplay 1.1.7 (§1.2 B16).
2. **The comment on the remembered entry provider** (`VF/…/AppNavHost.kt:193-195`). It states a cause the library source contradicts (B18).
3. **`AppCard` → `animateContentSize` on every card** (`VF/…/container/AppCard.kt:39, 66-77`). This adds a layout animation node to every card in every list. Make it an opt-in parameter (`animateSize = false`).
4. **`AppStatusBarProtection` over the whole app** (`VF/app/…/ui/AppRoot.kt`, after the lock layer). It paints an opaque strip over all content, which conflicts with glass or translucent top chrome.
5. **Pull-to-refresh spinner on programmatic refresh** (§1.3 B22).
6. **Hard-coded Indian digit grouping** (`AppSpecialFields.formatMinorUnits`, `formatLitres`). Group by locale instead.
7. **`AppChip` selected = solid accent fill** (`VF/…/AppChip.kt:41-48`). This is a VitFleet design decision. The generic fix is the `accentContent` role (B28).
8. **`EnvelopeUnwrapper` as the hard-coded default argument in seven inline functions** (`VF/core/network/…/NetworkClient.kt:94-220`). VitFleet had to edit seven defaults. The template should take the default from the DI-bound `ResponseUnwrapper` (already bound in `T/…/di/NetworkModule.kt:63`) rather than hard-coding `PassthroughUnwrapper()` per function (`T/…/NetworkClient.kt:97, 120, 139, 160, 181, …`).
9. Business-only: `ApiModels.kt` (generated), `TenantContext*`, `ContextHeaders`, the SDUI renderer (`core/ui/…/sdui`, about 1,250 lines), `core/ui/…/fleet`, `core/ui/…/history`, `core/update` (sideloaded APK installer), the fleet colour tokens, `VfIcons`, Lottie art. The SDUI renderer is interesting but heavy; it runs against the "lighter generated apps" goal.

---

## 4. Architecture observations for the overhaul

### 4.1 Cross-feature navigation ends up in a god module

VitFleet needed screens in one feature to open screens in another without a feature-to-feature dependency.

- It invented `…Links` interfaces per feature (e.g. `VF/feature/trips/…/TripsNavKeys.kt:27-37`) bound in `:app`.
- `VF/app/…/di/FeatureBindingsModule.kt` grew to **292 lines** importing about 60 keys from 18 features.
- `VF/app/…/routes/AppRoutes.kt` is a second central `when` over about 30 routes, shared by deep links and SDUI.
- Every new screen edits two files in `:app`. This scales badly to feature-heavy apps (merge conflicts, `:app` recompiles).

Recommendation for the template: split each feature into `:feature:x:api` (nav keys, plus the `Links` contract if any, pure Kotlin) and `:feature:x:impl`. Features may depend on other features' `:api`. Deep-link routes are contributed per feature through a multibound `Set<DeepLinkRoute>`, the same way `NavGraphEntry` is today, so `:app` holds no route table. The template's `DeepLinkResolver` is already a central `when` (`T/app/…/deeplink/DeepLinkResolver.kt`) and would grow the same way.

### 4.2 Colour roles cannot be extended without editing core

VitFleet edited `AppColors` (`VF/core/designsystem/…/theme/AppColors.kt`, +92 lines) to add `chrome` (branded header surface), `fleet` (domain tones) and `fieldErrorFill`.

- `chrome` (a brand-coloured header surface with its content, muted and indicator colours) and `fieldErrorFill` are generic roles, and so is `StatusTone(hue, content, subtle)`.
- `fleet` is domain-specific. The template should offer an extension point (a `CompositionLocal` for app tones in the app's own module, or a generic `tones: Map<…>`), so domain colours never touch `:core:designsystem`.

### 4.3 "Lighter generated apps": evidence from VitFleet

- VitFleet deleted the template's sample modules on day 2 (`2272e81`; decision A9-2: *"Removed the template's `feature:feed`, `data:feed` and `feature:settings`: nothing in VitFleet navigates to them, and feed called an endpoint the API does not have."*).
- It also removed `PlayStorePrompts.kt` and `play-app-update`/`play-app-review` (`21ff590`), replacing them with a server-driven update and maintenance gate.
- It rebuilt the app lock rather than enabling the template's `:app`-resident one.
- Implication: sample and demo features should be off by default and trivially removable. `applock` belongs in `:core:applock`. An optional "remote app status" (force update and maintenance via `ApiSignals`) is more widely useful than Play-only prompts.

### 4.4 Liquid glass compatibility (for the PeriFlow port)

VitFleet has no glass, but two patterns it inherited or added would defeat it:

- Root entries are padded so content never goes under the bar: `VF/…/AppNavHost.kt:220-223` `.padding(bottom = insetFor(it))`, and the template does the same at `T/…/AppNavHost.kt:158-163`. A backdrop-blur bar needs content scrolling *under* it. Pass the bar height as `contentPadding` (a `LocalBottomBarInset`) instead, and keep the opaque entry background *behind* the content, not as padding.
- `AppStatusBarProtection` paints an opaque strip at the top (§3.4).

### 4.5 Notes on cheap motion

VitFleet's motion changes that are good for FPS:

- No alpha on full-screen layers during push and pop.
- `popWhenOn` and `rememberCountUp` animate `graphicsLayer` or a single Int.
- The tab indicator position is read in `layout {}`.

Changes that recompose on every frame and should be fixed when porting:

- `AppPullToRefresh` reads `offset` during composition.
- `SlidingRule` and `SlidingPill` in `AppBottomBar` use `animateDpAsState` + `Modifier.offset(x = …)` (composition read). The template has the same (`T/…/navigation/AppBottomBar.kt` `SlidingRule`). Use `offset { IntOffset(...) }`.
- `AppBottomSheet` and `AppSwipeToAction` call `scope.launch { snapTo }` per drag delta.

---

## 5. Tests VitFleet added that the template should adopt

| Test | Asserts | Template gap |
|---|---|---|
| `VF/core/navigation/src/test/…/AppNavigatorTest.kt` | A stopped activity takes no commands; commands wait while backgrounded | B11 |
| `ShellStateTest` (+2 cases) | Navigating to a tab root selects the tab | B14 |
| `VF/app/src/test/…/navigation/NavKeyRegistrationTest.kt` | Every `AppNavKey` on the classpath is registered and round-trips | B15 |
| `VF/app/src/test/…/deeplink/LinkEntryManifestTest.kt` | `singleTop` + trampoline flags | B12 |
| `PendingDeepLinkTest`, `DeepLinkResolverTest` | Hold and take once | B13 |
| `AppDestinationsTest` | Entry identity is hash-stable across processes (keys, not `ImageVector`s) | VitFleet `d1c9a1d`: keying the host on `ShellTab`s (an `ImageVector` hashes a path node by identity) lost restored state after process death on release builds. Generic lesson: never key composition on `ShellTab` |
| `SessionTokensTest`, `QueuedRequestReplayerTest` (+200 lines), `KtorNetworkClientTest` (+77), `RetryPolicyTest`, `NetworkDatabaseMigrationTest`, `ConnectivityNetworkMonitorTest` | B1–B9 | |
| `PaletteContrastTest` (component roles) | Accent-as-text roles | B28 |
| `AppAlertDialogTest`, `AppPullToRefreshTest`, `AppSelectionTest`, `AppStatusBarTest` (androidTest) | B19–B27 | |
| `MenuPositionProviderTest`, `DataTableWidthsTest`, `DecimalInputTest`, `CurrencyFieldFormatTest` | Pure layout and format rules | |
| `AppLockControllerTest`, `SecureWindowTest` | B32 | |
| `AppLocalesTest` | B31 | |

---

## 6. Suggested port order

1. **Security and data loss:** B1 (bearer cache), B2 + P1 (refresh only on 401, `SessionTokens`), B3 (replayer), and B33's generator regex.
2. **Broken features:** B4–B6 (WebSocket), B7 (monitor), B12/B13 (links), B11 (navigator fan-out).
3. **Navigation smoothness**, combined with the navigation audit's target design:
   - B10: single host call site.
   - B16: opaque entries, no alpha, linear predictive back. Skip `.underneath()`.
   - B17: `enableOnBackInvokedCallback`.
   - B14, P8: `ShellState.apply`, `FullScreenKey`, `LocalCanNavigateUp`.
   - B15: discriminator and single registration.
4. **Design system:** B19–B29, the `accentContent` role with generator support, B27 (status icons in `AppTheme`).
5. **Platform:** B30, B31, B32 (app lock to `:core:applock`), B33 build switches.
6. **Optional components** (P5, P6, P10, P17, P18) as catalogue features, off by default.

### Code sketches

**Bearer cache drop** (`:core:network`):

```kotlin
fun interface BearerTokenCache { fun drop() }

@Singleton
class KtorBearerTokenCache @Inject constructor(
    @AuthenticatedClient private val client: Provider<HttpClient>,
) : BearerTokenCache, SessionScopedStore {
    override fun drop() { client.get().authProvider<BearerAuthProvider>()?.clearToken() }
    override suspend fun clear() = drop()
}
// AuthRepository.persist(): tokenStore.save(...); bearerTokenCache.drop()
```

**Navigator delivery** (`:core:navigation`):

```kotlin
suspend fun AppNavigator.applyWhileStarted(lifecycle: Lifecycle, apply: (NavCommand) -> Unit) =
    lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { commands.collect(apply) }
```

**Single host call site** (`AppShell`):

```kotlin
val wide = AppTheme.windowSize.width >= WindowWidth.Medium
val rail = wide && !fullScreen
Row(Modifier.fillMaxSize()) {
    if (rail) AppNavigationRail(items, state.selectedIndex, state::select)
    TabbedNavHost(state, registry, /* inset by flags */, Modifier.weight(1f).fillMaxHeight())
}
```

**Linear predictive pop** (`NavTransitions`):

```kotlin
fun predictivePop(m: AppMotion) = {
    val follow = tween<IntOffset>(m.slow, easing = LinearEasing)
    slideIntoContainer(End, follow, initialOffset = { (it * m.outgoingParallax).toInt() }) togetherWith
        (slideOutOfContainer(End, follow) + scaleOut(tween(m.slow, easing = LinearEasing), targetScale = 0.92f))
}
```

**Generator regex** (`G/genkit/icons.py:164`):

```python
r"\g<1>@drawable/ic_launcher_monochrome\g<2>"
```
