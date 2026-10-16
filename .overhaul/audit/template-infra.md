# Template infrastructure audit

Scope: `template/core/{network,database,datastore,analytics,notification,media,devtools,flags}` and
`template/app/src/main/kotlin/com/base/app/{push,lock,session,widget,playstore}` in
`/home/crazyhusir/Documents/projects/tools/android-base` (branch `overhaul/multiplatform-v2`),
compared against the hand-improved PeriFlow (`jorwen/periflow/periflow-android`) and VitFleet
(`tools/vitfleet/vitfleet-android`) copies.

All paths below are relative to `android-base/template/` unless prefixed. Library behaviour was
checked against the exact sources in the Gradle cache (Compose UI 1.12.1 from BOM 2026.09.00,
Ktor 3.6.0, firebase-messaging 25.1.x, glance-appwidget 1.2.0, app-update-ktx 2.1.0). No Gradle
was run.

---

## 0. Executive summary

* **The devtools badge cannot be dragged** because the drag callback adds each per-event delta
  to a position that was copied into a plain `val` when the composition ran. The pointer-input
  coroutine is never restarted, so every move computes `defaultPosition + thisEventDelta`. Details
  and a fix are in section 1. PeriFlow and VitFleet ship the same file byte for byte, so the bug
  is in both apps too.
* **The same kind of gesture bug shows up in two more places.** A stepped `AppSlider` cannot be
  dragged slowly, because each delta is added to the already-snapped value and rounds back.
  `busyOverlay` does not block taps, because it consumes in the Main pass, after the children
  have already handled the event.
* **Six features are broken or quietly off:**
  * The WebSocket never connects. The client has no `WebSockets` plugin, so every attempt throws
    and retries forever. Both apps fixed this.
  * Push notifications never show on Android 13+. Nothing requests `POST_NOTIFICATIONS`.
  * The analytics opt-out toggle is written to settings and never read.
  * Remote Config is never fetched.
  * An app-lock bypass: the lock screen restarts its own grace timer.
  * A finished flexible Play update restarts the app under the user without asking.
* **Auth and session bugs that can leak data between accounts:**
  * Ktor's bearer-token cache is never cleared. After sign-out and sign-in as someone else,
    requests carry the previous user's access token. VitFleet fixed this with
    `KtorBearerTokenCache`.
  * A refresh that finishes after sign-out writes the tokens back.
  * A 408 or 429 from the refresh endpoint signs the user out.
  * The push installation and the app database are not cleared on sign-out.
* **Performance and robustness problems:**
  * `NetworkMonitor.isOnline` is a cold `callbackFlow`, called with `first()` up to three times
    per request. Each call registers and unregisters a `NetworkCallback`. Android caps an app at
    about 100 callbacks, so this costs binder IPC and risks a crash. VitFleet fixed this.
  * Every body is decoded twice, as a String and then as a `JsonElement` tree.
  * A 204 empty body fails decoding. The template documents this as a workaround instead of
    fixing it. PeriFlow fixed it.
  * There are three separate OkHttp clients.
  * The Room response cache has no size limit, and a cache read or write error escapes the
    `AppResult` contract.
* **Heavy parts most apps do not need:**
  * `push` forces Firebase Analytics, Crashlytics and Remote Config.
  * `workmanager` has no workers.
  * `widget` drags in WorkManager and Room for one line of text, and initialises WorkManager at
    every cold start.
  * `room` is on by default and duplicates the OkHttp HTTP cache.
  * `lifecycle-process` is an unused dependency.
  * The media module ships an image compressor, a video transcoder, a probe and permission
    helpers that no feature calls. Its "capture limits" are never applied.

---

## 1. KNOWN BUG — the devtools "API log" badge does not drag

### Where

`core/devtools/src/main/kotlin/com/base/app/core/devtools/DevBadge.kt`, `DraggableBadge` (lines 50–102).
It is rendered last inside the root `Box` in `app/src/main/kotlin/com/base/app/ui/AppRoot.kt:88-91`.
Tapping it opens `DevToolsPanel`, the full-screen API log `Dialog`. The badge is the only thing
meant to be draggable.

```kotlin
57  var offsetX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
58  var offsetY by rememberSaveable { mutableFloatStateOf(Float.NaN) }
...
64  val x = if (offsetX.isNaN()) startX else offsetX      // a plain Float copy, made at composition
65  val y = if (offsetY.isNaN()) startY else offsetY
...
74  .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
76  .pointerInput(bounds, badge) {
77      detectDragGestures { change, drag ->
78          change.consume()
81          offsetX = (x + drag.x)          // x is the copy captured by THIS lambda instance
82              .coerceIn(0f, (bounds.width - badge.width).coerceAtLeast(0).toFloat())
83          offsetY = (y + drag.y)
84              .coerceIn(0f, (bounds.height - badge.height).coerceAtLeast(0).toFloat())
85      }
86  }
```

### Root cause, step by step

1. `x` and `y` (lines 64–65) are local `val`s, i.e. snapshot copies of the state taken when
   `DraggableBadge` composed. The `detectDragGestures` lambda captures those copies by value.
   It does not capture the `offsetX` and `offsetY` state objects.
2. In `detectDragGestures`, `drag` is the incremental delta of one pointer event (a few pixels),
   not the total since the gesture began.
3. `pointerInput(bounds, badge)` only restarts its coroutine when a key changes. The Compose UI
   1.12.1 source that ships with BOM 2026.09.00
   (`SuspendingPointerInputFilter.kt`, `update()` around lines 619–665) resets the handler only if
   `key1`, `key2` or `keys` differ, or if the handler's class differs. A new lambda instance from
   the same call site has the same class, so it does not reset. The comment there says:
   "Only reset when keys have changed or pointerInputEventHandler is called from a different call
   site". `bounds` and `badge` stay the same during a drag, so the coroutine started on the first
   touch keeps running the first lambda forever, because `detectDragGestures` loops through
   `awaitEachGesture`.
4. That first lambda was created while `offsetX` was still `NaN`, so its `x` is `startX`. Every
   move event therefore does `offsetX = startX + thisEventDelta`. The position never accumulates.
   The badge shakes within one event's delta of its default spot (16dp, 48dp) and stays there when
   released. Later drags do the same thing, because the base never changes.

Nothing else is at fault:

* The outer `Box(fillMaxSize)` has no pointer modifiers, so it is not hit-tested and does not
  steal touches.
* `appClickable` comes after `pointerInput` in the chain, so it is the child. Its tap detection
  sees the move in Main first, and then sees `detectDragGestures` consume it after touch slop, so
  the tap cancels correctly.
* No overlay window is involved. The badge is in the activity's composition.

It is the same bug in PeriFlow and VitFleet: `diff` shows only the package lines differ
(`periflow-android/core/devtools/.../DevBadge.kt`, `vitfleet-android/core/devtools/.../DevBadge.kt`).

### Secondary defects in the same composable

* **The offset is only clamped inside the drag callback.** It is not clamped when the badge is
  placed (line 74). The offsets are `rememberSaveable` (lines 57–58), but the bounds are not saved
  (line 59). After rotating from portrait to landscape, a badge that was dragged low ends up below
  the window and cannot be reached. The comment on lines 79–80 claims the opposite.
* **`x` and `y` are read during composition** (lines 64–65 feed line 74). Once the drag works,
  every frame of a drag recomposes `DraggableBadge` instead of just re-placing it.
* **The touch target is tiny.** `appClickable(..., minTouchTarget = 0.dp)` (line 91) on a
  `labelSmall` pill with `xs` vertical padding gives roughly a 20dp-tall target to drag or tap.
* **The default top inset is a guess.** `DEFAULT_TOP_INSET = 48.dp` (line 104) approximates the
  status bar, and clamping to `0f` lets the badge be dragged under the status bar or cutout.
* **Back from a request's detail closes the whole inspector.** In `DevToolsPanel`
  (`DevToolsPanel.kt:58-79`) nothing intercepts back while `inspecting != null`, so the Dialog's
  `onDismissRequest` runs. Add `BackHandler(enabled = current != null) { inspecting = null }`.
* **The website over-promises.** `generator/genkit/spec.py:323` and `README.md:302` advertise
  "A draggable badge", which the website shows (AGENTS.md: "The website must not over-promise").

### Fix

```kotlin
@Composable
private fun DraggableBadge(label: String, onClick: () -> Unit) {
    val density = LocalDensity.current
    val startX = with(density) { AppTheme.spacing.lg.toPx() }
    val startY = with(density) { DEFAULT_TOP_INSET.toPx() }

    var offsetX by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var offsetY by rememberSaveable { mutableFloatStateOf(Float.NaN) }
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    var badge by remember { mutableStateOf(IntSize.Zero) }

    // Local functions read the State objects every time they run, never a captured copy, and they
    // clamp at placement too, so a rotation cannot strand the badge off-screen.
    fun currentX() = (if (offsetX.isNaN()) startX else offsetX)
        .coerceIn(0f, (bounds.width - badge.width).coerceAtLeast(0).toFloat())
    fun currentY() = (if (offsetY.isNaN()) startY else offsetY)
        .coerceIn(0f, (bounds.height - badge.height).coerceAtLeast(0).toFloat())

    Box(Modifier.fillMaxSize().onSizeChanged { bounds = it }) {
        Box(
            Modifier
                .offset { IntOffset(currentX().roundToInt(), currentY().roundToInt()) } // layout-phase read
                .onSizeChanged { badge = it }
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        offsetX = currentX() + drag.x   // accumulates from the live value
                        offsetY = currentY() + drag.y
                    }
                }
                .background(AppTheme.colors.warning.content, RoundedCornerShape(percent = ROUNDED_PERCENT))
                .appClickable(onClick = onClick)          // keep the 48dp minimum touch target
                .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs)
                .semantics { contentDescription = "$label build. Opens the developer inspector." },
        ) { AppText(text = label, style = AppTheme.typography.labelSmall, color = Color.White) }
    }
}
```

Optionally, settle with a spring to the nearest horizontal edge on `onDragEnd`, using an
`Animatable<Offset>`, as iOS AssistiveTouch does. Use `WindowInsets.safeDrawing` for the
starting inset instead of the 48dp constant.

---

## 2. Similar gesture bugs elsewhere

### 2.1 A stepped `AppSlider` cannot be dragged slowly (same family: delta added to a base that does not accumulate)

`core/designsystem/.../component/selection/AppSlider.kt`

```kotlin
67  val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)     // derived from the snapped value
84  val snapped = if (steps > 0) { ... Math.round(raw / stepSize) * stepSize ... }
134 .draggable(state = rememberDraggableState { delta ->
137         emitFraction(fraction + (if (rtl) -delta else delta) / travel)
```

With `steps > 0`, `fraction` is the already-snapped value. One move event's delta is usually 2–10px
on a track of about 1000px. That is far less than half a step: with `steps = 9` the step is 0.1, so
half a step is roughly 50px. `fraction + delta/travel` rounds back to the same step, nothing changes,
and the next event starts from the same snapped base again. The thumb only moves when one event
jumps more than half a step, which takes a fast flick. The catalog shows this case directly
(`catalog/.../CatalogComponents.kt:235`, `steps = 9`). A continuous slider also drops deltas when
more than one event arrives before the parent recomposes `value`.

The fix is to track the finger's absolute position, as `AppRangeSlider` already does correctly
(`AppRangeSlider.kt:132-155`, `fractionAt(change.position.x)`), or to accumulate an unsnapped raw
fraction in a `remember`ed float during the drag:

```kotlin
var raw by remember { mutableFloatStateOf(0f) }
.draggable(
    state = rememberDraggableState { delta -> raw += (if (rtl) -delta else delta) / travel(); emitFraction(raw) },
    onDragStarted = { raw = fraction },
    ...
)
```

Better still, replace the press-plus-draggable pair with one `awaitEachGesture` that maps
`change.position.x`. PeriFlow's `AppSlider` is identical to the template, so both apps have the
bug too.

### 2.2 `busyOverlay` dims the content but does not block it

`core/designsystem/.../animation/AppTransitions.kt:237-258`

```kotlin
Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } } }
```

`awaitPointerEvent()` defaults to `PointerEventPass.Main`. Main bubbles from children to parent
(Compose `HitPathTracker.dispatchMainEventPass`: Initial on this node, then the children, then Main
on this node). A child `clickable` therefore sees the down and the up before this modifier consumes
them, and the click fires. It is used on the sign-in and sign-up forms
(`feature/auth/.../SignIn.kt:174`, `SignUp.kt:160`), where it is meant to stop a second submit or
edits during submission.

Fix: `awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }`. Also add
`semantics { disabled() }` so TalkBack agrees. PeriFlow and VitFleet carry the same code
(`AppTransitions.kt:309-311` and `:247-249`).

### 2.3 Drag-driven containers recompose every frame (performance, same area)

* **`AppSwipeToAction`** (`core/designsystem/.../list/AppSwipeToAction.kt:126-131`) runs
  `scope.launch { offset.snapTo(...) }` on every pointer delta, which allocates a coroutine per
  event and adds one dispatch of lag. Lines 160–168 read `offset.value` during composition
  (`if (offset.value != 0f)`), so the row recomposes on every animation frame. Wrap it in
  `derivedStateOf { offset.value != 0f }`, or move to `AnchoredDraggableState` (foundation),
  which gives velocity-aware settling for free.
* **`AppBottomSheet`** (`core/designsystem/.../overlay/AppBottomSheet.kt:81-87`) computes `progress`
  from `offsetY.value` during composition, so the whole dialog content recomposes every frame of
  the open, close and drag. Compute it inside the scrim's `graphicsLayer { alpha = ... }`. The
  `draggable` on the whole sheet (line 112) also has no nested-scroll hand-off, so a scrolling list
  inside the sheet can never drag it down. PeriFlow's `AppDetentSheet.kt:191-245` already does this
  with a `NestedScrollConnection`, which is worth bringing back.

---

## 3. Networking (Ktor): `core/network`

### 3.1 [CRITICAL] The WebSocket feature never connects

`websocket/AppWebSocket.kt:50` uses `@PlainClient`. `di/HttpClientModule.kt:54-65` builds that
client without `install(WebSockets)`. In Ktor 3.6.0, `HttpClient.webSocketSession` first calls
`plugin(WebSockets)` (`ktor-client-core .../websocket/builders.kt:34`), which throws
`IllegalStateException("Plugin WebSockets is not installed...")`. `AppWebSocket.connect` catches it
as a `Throwable` (lines 94–96), sets `Failed(willRetry = true)` and backs off forever. The feature is
in the "Everything" preset (`generator/genkit/spec.py`).

Both apps found and fixed this:

* VitFleet `core/network/.../di/HttpClientModule.kt:58-59`: "without the plugin the upgrade fails".
* PeriFlow added a separate `@RealtimeHttpClient` with `install(WebSockets)`.

VitFleet also found that once the plugin is installed, the devtools `ResponseObserver`
(`NetworkRecording.kt:16`) splits the upgrade response's body and tears the socket down within
milliseconds: "the debug build reconnected every second, forever". Their fix is
`filter { call -> !call.request.url.protocol.isWebsocket() }`.

Other WebSocket defects, all fixed in VitFleet's copy:

* **`attempt = 0` is reset as soon as the socket opens** (line 82). A server that accepts and then
  closes at once, for example on an auth failure, is retried every second forever. Reset only after
  the first frame arrives.
* **`session` and `connection` are not `@Volatile`, and `connect` is not synchronised**
  (lines 63–64 and 67–70). Two callers can open two sockets.
* **`disconnect()` closes whichever socket exists when its coroutine runs** (lines 112–120).
  That can be the next socket a restart just opened.
* **The handshake reads `tokenStore.accessToken()` directly** (line 77) and nothing refreshes it.
  After the token expires, every reconnect is refused.
* **No `pingInterval` is set.** Ktor's default is `PINGER_DISABLED` (`WebSockets.kt:68`), so a
  half-open TCP connection behind a NAT is never detected and the `for (frame in incoming)` loop
  hangs forever.
* **No lifecycle or connectivity awareness.** The socket keeps reconnecting in the background and
  while offline.

Fix: build a dedicated realtime client with `install(WebSockets) { pingInterval = 20.seconds }`,
no recording and no HTTP timeouts. Port VitFleet's `AppWebSocket`. Gate reconnects on
`NetworkMonitor` and on `ProcessLifecycleOwner`'s STARTED state.

### 3.2 [HIGH] Ktor's bearer-token cache is never invalidated, so one user's token leaks into the next user's session

`HttpClientModule.kt:97-141` uses `install(Auth) { bearer { loadTokens {...} } }` with the default
`cacheTokens = true`. Ktor 3.6 `AuthTokenHolder.loadToken()` returns the cached value once it is set
(`if (value != null) return value // Hot path`). Only `BearerAuthProvider.clearToken()` resets it,
and nothing in the template calls it (`grep clearToken` finds nothing).

Sequence: user A signs in, and the first request caches A's `BearerTokens`. A signs out:
`SessionCoordinator` clears the DataStore, but Ktor's in-memory copy stays. B signs in through a
`requiresAuth = false` request (`data/auth/.../AuthRepository.kt:61-66`), and `tokenStore.save(B)`
runs. Every authenticated request now still sends A's access token. If the backend does not revoke
A's token at sign-out, B sees A's data.

VitFleet hit exactly this (`vitfleet-android/core/network/.../auth/BearerTokenCache.kt:12-17`:
"every call after a switch carried the old company's token") and fixed it with
`KtorBearerTokenCache`. It is a `SessionScopedStore` that calls
`client.get().authProvider<BearerAuthProvider>()?.clearToken()`, and every token save also drops
the cache.

PeriFlow went further and removed the Auth plugin. `periflow-android/core/network/.../auth/Authentication.kt`
is a small `HttpSend` interceptor that asks an `AuthTokenProvider` seam on every request. That
drops the `ktor-client-auth` dependency and moves token ownership out of the network module.
**Recommend adopting PeriFlow's approach as the template default.**

### 3.3 [HIGH] Refresh races: a sign-out can be undone, and a rate limit signs the user out

* **Undone sign-out.** `HttpClientModule.kt:117-124` calls `tokenStore.save(...)` with no check
  that the session is still the one it refreshed. If the user signs out while a refresh is in
  flight, the late success writes the tokens back. `isAuthenticated` flips true again and
  `MainActivity`'s `combine(settings, isAuthenticated)` (lines 123–129) routes the user back into
  the signed-in shell.
* **Rate limit signs the user out.** `HttpClientModule.kt:127-129` ends the session on any
  `result.code in 400..499` (`REFUSED_RANGE`, line 198). That includes 408 (timeout), 429 (rate
  limit) and 409.
* **Rotation reuse.** With rotating refresh tokens, a second refresh path, such as a socket's
  `auth.expiring` or a WorkManager job, would reuse a spent refresh token. Many backends revoke the
  whole token family when that happens.

VitFleet's `auth/SessionTokens.kt` fixes all three:

* A `Mutex`.
* "If someone else already replaced it, the saved pair is the answer".
* "If one did while the call was out, that is the session now: neither overwrite it nor end it".
* "Only a 401 ends the session".

Adopt it as is.

### 3.4 [HIGH] `NetworkMonitor.isOnline` is cold and registers a `NetworkCallback` on every call

`core/common/.../network/NetworkMonitor.kt:33-80` is a cold `callbackFlow` whose body calls
`registerNetworkCallback` and, on close, `unregisterNetworkCallback`. `KtorNetworkClient` calls
`networkMonitor.isOnline.first()` at line 84 for every request, again at line 135 on every
no-answer, and again at line 166. `QueuedRequestReplayer` also collects it.

Each `first()` costs a register binder call, a `getNetworkCapabilities` binder call and an
unregister binder call. Android limits an app to about 100 outstanding network requests and throws
`ConnectivityManager.TooManyRequestsException` beyond that. A burst of parallel requests (feed,
images, prefetch) can crash before the `try`, because line 84 is outside `sendWithRetries`.

VitFleet fixed it (`vitfleet-android/core/common/.../NetworkMonitor.kt`, comment: "cold, each
`first()` registered and dropped a callback of its own, and the system allows an app only about a
hundred at once") with
`.shareIn(scope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), replay = 1)`.

Also reconsider the up-front gate at line 84. Refusing to send because the system has not marked
a network `VALIDATED` breaks apps on captive portals, some enterprise Wi-Fi and some VPNs or
private-DNS setups, where the network works but never validates. Let OkHttp fail fast instead, and
use connectivity only for the message and for queueing.

### 3.5 [MEDIUM] An empty success body (204) fails decoding, and the template works around it at the call site

`NetworkClient.kt:46` and `:73` call `NetworkJson.parseToJsonElement(result.data.body)`. That throws
on `""`, so a `delete<Unit>(...)` or `post<_, Unit>` against a 204 returns
`Failure(Serialization)` after a success. The template knows this:
`data/auth/.../AuthRepository.kt:79-82` says "decoding an empty body into `Unit` fails at the JSON
parse … `execute` rather than `post<_, Unit>`". PeriFlow fixed it at the source
(`periflow-android/core/network/.../NetworkClient.kt`, `parseBody`: blank → `JsonObject(emptyMap())`).
Adopt it.

### 3.6 [MEDIUM] Each response is parsed twice and all bodies are held in memory

`send()` reads the whole body into a `String` (`KtorNetworkClient.kt:157`, `bodyAsText()`). Decoding
then builds a full `JsonElement` tree (`NetworkClient.kt:46`) and decodes again from that tree
(line 49). For a large list that roughly triples the allocation and the CPU compared with
`NetworkJson.decodeFromString(deserializer, body)`.

Requests do the same in reverse: `encodeToJsonElement(body)` (`NetworkClient.kt:146`), then
`ContentNegotiation` serialises the tree again. `ContentNegotiation` is installed
(`HttpClientModule.kt:176`) only for that second step.

Suggestion: keep the unwrapper, but give it a fast path. When the unwrapper is passthrough, decode
straight from the string. For envelopes, decode an `Envelope<JsonElement>` once, or use a generic
`Envelope<T>` serializer. Also stop hard-coding `Dispatchers.Default` (`NetworkClient.kt:44`, `:71`),
which breaks the project's own "dispatchers are injected" rule (`core/coroutines/.../Dispatchers.kt:5`).

### 3.7 [MEDIUM] Errors from the response cache escape the `AppResult` contract

* `KtorNetworkClient.kt:76-81` (`responseCache.fresh(...)`) and `:173-175` (`responseCache.put(...)`)
  are outside any `try`.
* `cached_responses.body` is unbounded `TEXT` (`ResponseCache.kt:63-69`). Reading a row over the
  framework `CursorWindow` limit (about 2MB) throws `SQLiteBlobTooBigException`.
* A full disk throws `SQLiteFullException` on `put`.
* Either way a successful HTTP call becomes an exception thrown at the ViewModel.
* There is also no eviction at all. The table grows with every distinct key until sign-out.

Wrap both calls in `runCatching`, skip caching bodies over a size cap, and add an LRU or TTL purge.

### 3.8 [MEDIUM] Three separate OkHttp clients; certificate pins on only one

* **Ktor clients.** Plain (`HttpClientModule.kt:59`) and authenticated (`:84`) each call
  `HttpClient(OkHttp)`, which builds its own `OkHttpClient` with its own connection pool and
  dispatcher.
* **Coil.** Coil's default `ImageLoader` (`coil-network-okhttp`, `app/build.gradle.kts:101-102`;
  `grep` finds no `ImageLoader` or `OkHttpClient` setup) creates a third.
* **Pinning.** Pins are only added to the authenticated client (`:89`). The refresh call (plain
  client) sends the refresh token without pinning, and so does the WebSocket handshake.
* **Shared work is lost.** No TLS sessions or HTTP/2 connections are shared with the API host.

Provide one `@Singleton OkHttpClient` through Hilt. Use `OkHttp.create { preconfigured = shared }`
for both Ktor clients, `shared.newBuilder()` where timeouts differ, and
`OkHttpNetworkFetcherFactory(callFactory = { shared })` in a `SingletonImageLoader.Factory`. Put
pins in `res/xml/network_security_config.xml` with `<pin-set expiration=…>` instead of in code, so
every client, Coil and WebView all get them.

### 3.9 [MEDIUM] The devtools recorder captures credentials in shippable non-production builds

`app/.../di/AppModule.kt:30` sets `recordExchanges = buildDevEnvironment().visible`, which is true
for debug builds and also for dev and staging release builds (`DevEnvironment.kt:13`).
`NetworkRecording.kt:31-33` stores `TextContent.text` for every request body, including the
sign-in, sign-up and refresh JSON with the password and refresh token. `DevToolsLog.redacted()`
(`DevToolsLog.kt:38-40`) only scrubs headers. The panel shows the bodies in a `SelectionContainer`
(`DevToolsPanel.kt:250`) to anyone holding a QA or stakeholder phone.

Debug logcat has the same problem: `Logging` at `LogLevel.BODY` (`HttpClientModule.kt:188-195`)
sanitises only `Authorization`. PeriFlow added a path filter for credential endpoints to `Logging`
(`periflow-android/core/network/.../HttpClientModule.kt`: "Their bodies carry the refresh token and
the request-signing key"). Apply the same filter, plus JSON field redaction (`password`,
`*_token`, `secret`), to the recorder.

Related problems in the recorder:

* `failure = null` is hard-coded (`NetworkRecording.kt:37`). Requests that never got a response
  (timeouts, DNS failures, TLS errors) are never recorded, which is exactly when the inspector is
  needed. The `ERR` pill (`DevToolsPanel.kt:182-186`) and the `failure` field are dead. Record
  failures from `KtorNetworkClient.sendWithRetries` or from an `HttpSend` interceptor.
* `responseBytes` comes only from `Content-Length` (`NetworkRecording.kt:47-48`). That header is
  absent on gzip or chunked responses, so "Received" under-reports.
* The memory budget is large. `CAPACITY = 200` with `MAX_BODY_CHARS = 128 * 1024` per body
  (`DevToolsLog.kt:46-47`) allows tens of MB of strings in QA builds, which skews the performance
  testing those builds are used for. Use about 32KB per body, or a byte-budgeted ring buffer.

### 3.10 [MEDIUM] Total latency budget and error typing

* **The worst case is about 90 seconds for one GET.** Timeouts are 30s request, 15s connect and
  30s socket (`NetworkConfig.kt:42-44`), with two retries (`RetryPolicy.kt:39`), and the request
  timeout applies to each attempt. That is 30s × 3 plus the backoff delays. Add an overall
  deadline (`withTimeout`) for user-initiated calls.
* **`NetworkException.Timeout` is never constructed** (`grep` finds only the declaration at
  `model/NetworkModels.kt:91`). Ktor's `HttpRequestTimeoutException` is an `IOException`
  (`HttpTimeout.kt:210-214`), so a timeout becomes `NetworkException.Unknown` with a message that
  includes the URL.
* **`AppResult.Failure.isOffline` is documented as "no connectivity, or a timeout"**
  (`core/common/.../AppResult.kt`), but timeouts set it to `false` (`KtorNetworkClient.kt:166`).
* **The typed `NetworkException` hierarchy is mostly unused.** `userMessage()` checks only flags
  and an `IOException` cause (`AppResultMessages.kt:11-17`). Either use the hierarchy or delete it.
* `Retry-After` is parsed only as seconds (`RetryPolicy.kt:30`). The HTTP-date form is ignored.

### 3.11 [LOW] A User-Agent with non-ASCII characters breaks every request

`AppModule.kt:25-26` interpolates `Build.MANUFACTURER` and `Build.MODEL` into the User-Agent.
OkHttp rejects non-ASCII header values with `IllegalArgumentException`, and some OEM model strings
contain them. On those devices every request fails as `Unknown`. Sanitise the value to printable
ASCII.

### 3.12 [LOW] Other notes on the client

* `InFlightRequests` shares an in-flight GET by path, query and headers (`KtorNetworkClient.kt:257-269`),
  not by user. A request started just before a sign-out can be shared with the next user's
  identical read. Drop the shared map on sign-out.
* `Headers.toMap()` keeps only the first value of each header (`KtorNetworkClient.kt:314-316`).
* `NetworkJson` uses `isLenient = true` (`ResponseUnwrapper.kt:88`). That hides server bugs and
  costs a little parsing speed. Prefer strict parsing in debug builds.

---

## 4. Offline cache and request queue (`room` feature, `ResponseCache.kt`)

* **[MEDIUM] Queued mutations are silently dropped on any 4xx.** `ResponseCache.kt:234` drops on
  `result.code in 400..499`, which includes 408, 429 and 401. Losing a user's offline edit to a
  rate limit is data loss. VitFleet keeps refused entries with the server's answer and treats 401,
  408, 426 and 429 as momentary (`MOMENTARY_REFUSALS`).
* **[MEDIUM] One permanently failing entry blocks the queue.** A 5xx "ends the pass and keeps the
  rest in order" (`ResponseCache.kt:207-211`). There is no maximum age, no attempt cap and no
  timer-based retry. Entries are only replayed when connectivity flips from false to true
  (`:221`), so a request queued while online but with the server unreachable waits until the next
  network change, which may be days away. VitFleet's replayer has a growing retry timer, a `Mutex`
  so passes do not overlap, a "replay soon" hook and an owner/tenant per entry.
* **[MEDIUM] An uncaught exception in the application scope crashes the app.**
  `BaseAppApplication.kt:96` runs `appScope.launch { queuedRequestReplayer.replayWhenOnline() }`.
  `ApplicationScope` is `SupervisorJob() + Dispatchers.Default` with no
  `CoroutineExceptionHandler` (`core/coroutines/.../CoroutinesModule.kt:73-78`). A corrupt row
  (`HttpMethodType.valueOf`, `decodeFromString` at `ResponseCache.kt:242-247`) or an SQLite error
  crashes the process every time the device comes online. VitFleet wraps each pass
  (`passOrRetry`). Add a logging `CoroutineExceptionHandler` to the application scope too.
* **[LOW] The destructive migration deletes unsent user writes.**
  `NetworkModule.kt:93-98` uses `fallbackToDestructiveMigration(dropAllTables = true)` with the
  comment "Everything here is a cache or a replayable request, so dropping it … is safe". Dropping
  queued mutations is not safe. VitFleet added `NETWORK_MIGRATION_1_2` and corrected the comment.
* **[ARCHITECTURE] Two caches, and a misleading feature name.** The `room` feature (on by default
  and in the Standard preset) adds Room and KSP to `core:network` for a cache that duplicates the
  OkHttp HTTP cache (`HttpClientModule.kt:158-161`, 50MB). The key is also misleading: `room` means
  "offline response cache", and the real app database (`database`) `requires=("room",)`
  (`spec.py:316`). An app cannot have a database without the network cache. Split them into
  `offline-cache` and `database`, and make `database` independent.

---

## 5. `core/database`

* **[MEDIUM] No feature uses the module, and it is not cleared on sign-out.** `grep` for
  `AppDatabase` or `NoteDao` outside the module returns nothing. If a feature stores per-user rows
  there, they survive sign-out, because no `SessionScopedStore` is bound. PeriFlow added one
  (`periflow-android/core/database/.../di/DatabaseModule.kt`: "Everything in it belongs to the
  person signed in, so it all goes when they change", `database.clearAllTables()`).
* **[LOW] Cargo-cult migration.** A brand-new generated app ships at `VERSION = 2` with
  `MIGRATION_1_2` for a reference `notes` table (`AppDatabase.kt:9-35`). Only `schemas/.../2.json`
  is committed, so `MigrationTestHelper` or `AutoMigration` cannot validate from 1. The test
  hand-writes the v1 SQL instead (`AppDatabaseMigrationTest.kt:54-79`) and never checks the
  migrated schema against `2.json`. Start generated apps at version 1, and keep the migration
  example as documentation or a test fixture.
* **[LOW] `@Insert(onConflict = REPLACE)` is named `upsert`** (`Note.kt:31-32`). REPLACE deletes
  and re-inserts, which fires `ON DELETE CASCADE`. Use Room's `@Upsert`.
* **[LOW] Schema location is set the old way.** `AndroidRoomConventionPlugin.kt:14-17` passes
  `room.schemaLocation` as a KSP argument. The Room Gradle plugin (`androidx.room`,
  `room { schemaDirectory(...) }`) is the supported route and wires schemas into test assets.
* Good: there are no main-thread queries and every DAO is `suspend` or `Flow`.

---

## 6. `core/datastore` and token storage

* **[MEDIUM] A token that cannot be decrypted leaves the user stuck "signed in".**
  `AuthTokenStore.kt:42-44` derives `isAuthenticated` from whether the encrypted keys are present.
  `KeystoreCipher.decrypt` returns `null` on any failure (`KeystoreCipher.kt:29-39`). After a
  Keystore key is lost (OEM bugs, a device-transfer edge case), `isAuthenticated` is true but
  `accessToken()` is null. Ktor sends no token, gets a 401, and `oldTokens == null` means no
  `endSession` (`HttpClientModule.kt:114`). Every request fails with no route back to sign-in.
  Treat a decrypt failure as signed out: clear the entries and emit `false`.
* **[LOW] The Keystore is opened on every operation, and key creation can race.**
  `secretKey()` (`KeystoreCipher.kt:41-60`) runs `KeyStore.getInstance` and `load(null)` and looks
  up the entry, which is Keystore IPC, on every `encrypt` and `decrypt`. It is not synchronised, so
  two first-time callers can each generate the key. The second overwrites the alias and the first
  ciphertext becomes unreadable. Cache the key in a `by lazy(SYNCHRONIZED)` or `@Synchronized`
  holder.
* **[LOW] Duplicate clearing.** `DataStoreAuthTokenStore` is registered as a `SessionScopedStore`
  (`StoreBindings.kt:23-25`), and the whole session file is also cleared
  (`DataStoreModule.kt:50-54`). That is two clears of the same keys. One is enough.
* Good: there is one instance per file, both files have a `ReplaceFileCorruptionHandler`, and
  scope and dispatcher are injected.

---

## 7. Analytics, Crashlytics and Firebase wiring

* **[HIGH] The analytics opt-out is cosmetic.** The Settings switch writes `analyticsEnabled`
  (`feature/settings/.../Settings.kt:137`; `AppSettingsStore.kt:20,58`). Nothing reads it to call
  `AnalyticsTracker.setEnabled` (`grep` finds no caller). Crashlytics has no `setEnabled` at all
  (`CrashlyticsReporter.kt`). With `firebase` on, Firebase Analytics auto-collects screen views,
  sessions and first_open, and Crashlytics keeps reporting, after the user turns it off. This is a
  privacy and compliance bug. Add a collector in `Application.onCreate` along these lines:
  `settings.map { it.analyticsEnabled }.distinctUntilChanged().collect { tracker.setEnabled(it); crashReporter.setEnabled(it) }`.
  Also add `firebase_analytics_collection_enabled=false` metadata until consent is known, if the app
  needs opt-in. PeriFlow added
  `CrashlyticsReporter.setEnabled { crashlytics.isCrashlyticsCollectionEnabled = enabled }`.
* **[MEDIUM] The tracker seam is unused.** `AnalyticsTracker` is injected nowhere, and there is no
  screen-view hook on navigation. The KDoc on `NoOpAnalyticsTracker` says "The binding used in
  debug builds" (`AnalyticsTracker.kt:32-35`), which is false: with `firebase` on, debug builds
  bind Firebase (`di/AnalyticsModule.kt:19-25`). Debug crashes go to Crashlytics too, because the
  convention plugin applies Crashlytics to every variant (`AndroidFirebaseConventionPlugin.kt:10-16`).
* **[MEDIUM] Every `AppLogger.e` becomes a Crashlytics non-fatal.**
  `BaseAppApplication.kt:83-85` installs a reporter that calls `recordException`. That includes
  expected failures such as "Image compression failed" and "Request failed" (`KtorNetworkClient.kt:119`).
  At scale that floods non-fatals and hides real issues. Send `e()` as a breadcrumb (`log`) and
  record exceptions explicitly.
* **[ARCHITECTURE/BLOAT] `push` requires `firebase`, which requires `analytics` and `flags`**
  (`spec.py:169`, `:189`). Turning on push therefore ships Firebase Analytics, Crashlytics and
  Remote Config SDKs. Push only needs `firebase-messaging` and Installations. Split it into
  `firebase-core` (google-services), `crashlytics`, `firebase-analytics`, `remote-config` and
  `push`.

---

## 8. Notifications and push (`core/notification`, `app/push`)

* **[HIGH] `POST_NOTIFICATIONS` is never requested.** The manifest declares it
  (`core/notification/src/main/AndroidManifest.xml:12`), and `AppNotifications.post` silently
  returns when it is missing (`AppNotifications.kt:90-93`). No screen ever asks:
  `notificationsPermission` (`core/media/.../AppPermissions.kt:131-136`) and `rememberPermission`
  are referenced nowhere outside `core/media`. With target SDK 37 the system does not prompt on its
  own, so on Android 13+ pushes are never displayed. PeriFlow added the request in onboarding
  (`feature/onboarding/.../Onboarding.kt:86-95`) and in notification settings.
* **[MEDIUM] A push without a `link` does nothing when tapped.** `contentIntent` is set only when
  the push carries a link (`AppMessagingService.kt:61`, `AppNotifications.kt:105-115`). Without
  one, or with `deeplink` off, tapping the notification only dismisses it. Default to the launcher
  intent (`packageManager.getLaunchIntentForPackage`).
* **[MEDIUM] The push registration is not cleared on sign-out.** Nothing in `SessionCoordinator`
  touches push, so the next person on the device receives the previous account's pushes if the
  backend keys on the FID. Add a `SessionScopedStore` that tells the sink to unregister, or calls
  `FirebaseMessaging.unregister()`.
* **[LOW] Startup cost.** `registerOnLaunch()` calls `FirebaseMessaging.register()` and fetches the
  installation ID on every cold start (`PushTokenRegistrar.kt:21-27`, `BaseAppApplication.kt:92`).
  `createChannels()` makes four `NotificationManager` binder calls on the main thread during
  `Application.onCreate` (`BaseAppApplication.kt:91`). Both are small, but both are avoidable:
  register only when the ID changes, and create channels off the main thread or lazily.
* **[LOW] Dead pre-O code.** `minSdk = 26` (`build-logic/.../AppConfig.kt:13`), so
  `if (SDK_INT < O) return` (`AppNotifications.kt:63`) and the whole `toCompatPriority()` block
  with its KDoc about pre-O devices (`:133-143`) are dead. `notificationsPermission` returns
  `INTERNET` below API 33 as a stand-in (`AppPermissions.kt:135`), and it lives in `core:media`.
  Move it to `core:notification` and return `null`.
* Not present, so nothing to flag: no foreground services (no Android 14 FGS types needed) and no
  exact alarms.
* Worth adopting: PeriFlow's lock-screen-safe public version (`AppNotifications.kt:97-102` there)
  and `NotificationTaps`.

---

## 9. Media (`core/media`)

* **[MEDIUM] The FileProvider shares the whole cache directory.** `res/xml/media_file_paths.xml`
  has `<cache-path name="uploads" path="." />`. That covers all of `cacheDir`: the OkHttp cache of
  authenticated responses (`cacheDir/http`), Coil's disk cache, and so on. The file's own comment
  says "Only the two cache directories captures and compressed images are written to … Deliberately
  not `path="."`". The cause is `ImageCompressor.compressToFile` writing to the cache root
  (`ImageCompressor.kt:104`). Write to `cacheDir/uploads/` and use `path="uploads/"`.
* **[MEDIUM] A camera capture is lost after process death.** `pendingCameraUri` is held in
  `remember`, not `rememberSaveable` (`MediaPickerController.kt:59`). The camera app commonly
  forces the activity, and often the process, to be recreated on low-RAM devices. On return the
  URI is `null` and the photo is silently dropped (lines 85–91). `Uri` is `Parcelable`, so
  `rememberSaveable` works directly.
* **[MEDIUM] The profile avatar persists a temporary photo-picker URI.**
  `ProfileScreen.kt:123-125` stores `uri.toString()` through `ProfileRepository.setPhoto`
  (`data/profile/.../ProfileRepository.kt:50-52`). Photo-picker grants are temporary, so after a
  reboot or process death the avatar fails to load. Copy the image into `filesDir` with the
  existing, unused `ImageCompressor` and `ImageCompression.Avatar`, or call
  `takePersistableUriPermission`.
* **[MEDIUM, dead code and misleading docs] Capture limits are never applied.**
  `recordVideo()` claims "the duration and quality limits applied *at capture*"
  (`MediaPickerController.kt:47`), but `ActivityResultContracts.CaptureVideo` takes no extras.
  `VideoCompression.captureExtras()` (`:141-145`) is never called, and the `videoSettings`
  parameter (`:54`) is used only as a `remember` key. `PassthroughVideoTranscoder` tells developers
  to "constrain capture with MediaPicker.recordVideo" (`VideoTranscoder.kt:73-74`), which does
  nothing. `ImageCompressor`, `VideoTranscoder`, `VideoProbe`, `rememberPermission` and the
  read-permission helpers have no caller in any feature (`grep`). Trim the module to the picker
  and one compressor, or make the claims true.
* **[LOW] Images can come out sideways, and decoding uses too much memory.**
  - Orientation is applied only when `stripMetadata` is on (`ImageCompressor.kt:73`). Re-encoding
    always drops EXIF, so `stripMetadata = false` produces a sideways image with no orientation
    tag.
  - `ORIENTATION_TRANSPOSE` and `ORIENTATION_TRANSVERSE` are not handled (`:165-171`).
  - Decode, rotate and scale can hold three ARGB_8888 bitmaps at once (`:70-79`).
  - `ImageDecoder` with `setTargetSize` decodes at the target size and applies EXIF on API 28+.
  - Encoding is CPU-bound and runs on `IoDispatcher` (`:66`).
* Good: it uses the photo picker (`PickVisualMedia`) and declares no `READ_MEDIA_*` permissions.

---

## 10. Feature flags (`core/flags`)

* **[MEDIUM] Remote Config never activates, and flags have no reader.** `FeatureFlags.refresh()`
  is never called. `grep` for `FeatureFlags` outside `core/flags` finds only Gradle files. Remote
  Config values therefore never apply, and with firebase on, every flag reads its default forever.
  Nothing in the template reads a flag at all. `get` is not observable either, so a refresh could
  not update the UI. Add `fun <T> observe(flag): Flow<T>` (Remote Config has `addOnConfigUpdateListener`
  for real-time updates), call `refresh()` from the app start-up path, and use the sample flag
  somewhere real or remove it.
* **[LOW] Debug overrides are unreachable.** `LocalFeatureFlags.override` and `clearOverrides`
  (`FeatureFlags.kt:24-29`) are only on the concrete class, use a plain `mutableMapOf` (not
  thread-safe), and have no UI. `core/devtools/build.gradle.kts:18-20` depends on `:core:flags`,
  but devtools never uses it. The obvious home for overrides is a "Flags" tab in the inspector.
* **[LOW] Remote Config is set up in the constructor and fetches too rarely in debug.**
  `Firebase.remoteConfig` setup runs in the constructor on whichever thread first injects it
  (`RemoteConfigFeatureFlags.kt:17-22`). `minimumFetchIntervalInSeconds = 3600` applies to debug
  builds too, so flags cannot be iterated in debug.

---

## 11. App lock (`app/lock`, `MainActivity`)

* **[HIGH] The lock screen restarts the grace timer, which allows a bypass.**
  - `MainActivity.onStop` calls `appLock.onHidden()` (`MainActivity.kt:212-215`), which records
    `lastVisibleAt` (`AppLock.kt:24-26`).
  - `LockActivity` is opaque, so when it covers `MainActivity`, `MainActivity` stops and the timer
    restarts at that moment.
  - Steps: the app has been away for more than 30s, so the lock shows. The user presses Home.
    `LockActivity` is `noHistory` and finishes (`AndroidManifest.xml:126-132`). The user reopens
    within 30s.
  - `shouldLock()` then sees `elapsed < GRACE_MILLIS` (`AppLock.kt:33`) and the app opens unlocked
    without authenticating.
  - PeriFlow rewrote this. It adds `justUnlocked` and an "own action" grace, and always locks on any
    other return (`periflow-android/app/.../lock/AppLock.kt:21-54`), which closes the bypass.
    Adopt that version, or better, make the lock a full-screen Compose layer inside `MainActivity`
    driven by `ProcessLifecycleOwner`.
    `androidx.lifecycle.process` is already a dependency and is unused (`app/build.gradle.kts:90`;
    `grep ProcessLifecycleOwner` finds nothing).
* **[MEDIUM] App content is visible before the lock appears.** The KDoc says "decided in
  `onStart`, before the window is drawn, so the app's own content is never on screen — even for a
  frame" (`MainActivity.kt:199-202`). The check is `lifecycleScope.launch { if (appLock.shouldLock()) startActivity(...) }`,
  which suspends on a DataStore read. `MainActivity` resumes and draws first, and `LockActivity`
  then opens with a window transition over the visible content. `FLAG_SECURE` stops screenshots,
  not someone looking at the screen. Using an in-activity overlay that starts locked
  (`rememberSaveable`) fixes this as well.
* **[LOW] Lock-screen behaviour gaps.**
  - `onAuthenticationError` ignores every error code (`LockActivity.kt:137`), including lockout.
  - The prompt starts again on every `LockActivity` recreation (`:123`).
  - `androidx.biometric` requires `FragmentActivity`, which pulls `androidx.fragment` into an app
    that is otherwise Compose-only.
  - The lock is UI-only: tokens are not tied to the authentication. That is acceptable, but say so
    in the docs.

---

## 12. Session handling (`app/session`)

`SessionCoordinator` (`SessionCoordinator.kt:49-58`) clears only `SessionScopedStore` multibindings.
These do not reach them today:

* Ktor's bearer cache (3.2).
* The Room `AppDatabase` (section 5).
* The push registration (section 8).
* The Glance widget's per-widget DataStore: `AppWidget.HEADLINE` survives sign-out
  (`AppWidget.kt:43,100`).
* In-flight shared GETs (3.12).
* The WebSocket, which keeps the old user's socket open.

Also:

* A late token refresh can resurrect the session (3.3).
* `runCatching { store.clear() }` (`:53`) also swallows `CancellationException`.
* Two concurrent `signOut()` calls, one from `sessionEvents.expired` and one from the user, run
  teardown twice.

PeriFlow's version wipes when the signed-in uid changes and reconnects the realtime socket
(`periflow-android/app/.../session/SessionCoordinator.kt`). That is a better rule for guest-to-account
upgrades.

---

## 13. Widget (Glance, `app/widget`)

* **[MEDIUM, cost] Glance brings WorkManager and Room for one line of text.** glance-appwidget
  1.2.0's runtime dependencies include `androidx.work:work-runtime-ktx`
  (`glance-appwidget-1.2.0.module`), which brings Room and SQLite. With `widget` on and
  `workmanager` off, `WorkManagerInitializer` is still registered through App Startup. The manifest
  removal is fenced by `<opt:workmanager>` (`AndroidManifest.xml:19-42`), so WorkManager, and its
  Room database, initialise on every cold start even when no widget is placed. Remove the
  initializer whenever WorkManager is on the classpath, and use on-demand initialisation. For a
  static app-name-plus-headline widget, a plain `RemoteViews` `AppWidgetProvider` is far lighter.
* **[LOW] `AppWidgets.setHeadline` is never called** (`grep`). The headline is never cleared on
  sign-out (section 12). It creates a fresh `AppWidget()` per call (`AppWidget.kt:98`). It calls
  `update()` for each widget in turn, and each call starts a Glance session.
* Good: `updatePeriodMillis="0"` (`res/xml/app_widget_info.xml`).

---

## 14. Play in-app update and review (`app/playstore`)

* **[HIGH] The app restarts under the user as soon as a flexible update has downloaded.**
  `MainActivity.kt:190-192` comments "Asking rather than restarting under the user: an app that
  relaunches itself mid-sentence loses whatever they were doing" and then immediately calls
  `appUpdates.install(result)`, which runs `completeUpdate()` (`PlayStorePrompts.kt:46-48`). The KDoc
  on `install` says "never on its own". The flow is collected while RESUMED, so this happens while
  the user is actively using the app. PeriFlow has the same code. Show a snackbar or dialog with a
  "Restart" action, or install on the next `onStop`.
* **[MEDIUM] The update prompt nags on every resume.** `repeatOnLifecycle(RESUMED)`
  (`MainActivity.kt:171-176`) restarts `updates()` each time the app resumes: returning from
  another app, a permission dialog or the biometric prompt. Each `Available` launches the flexible
  update dialog again, so "No thanks" is never respected. PeriFlow stores a
  `dismissedUpdateVersion` in settings. Adopt that, and check once per process or once per day.
* **[MEDIUM] The review prompt counts resumes, not launches.** `reviewPrompt.onLaunch` is called
  on every resume (`MainActivity.kt:174`), but the KDoc says "Call once per launch"
  (`PlayStorePrompts.kt:62`). The "5 launches" threshold is reached after a few resumes, and every
  resume writes to DataStore. Call it once per process, for example from `onCreate` when
  `savedInstanceState == null`.
* [LOW] Prefer the `ActivityResultLauncher` overload of `startFlexibleUpdate`
  (app-update-ktx 2.1.0 has both) to the `requestCode` form (`PlayStorePrompts.kt:41-43`), whose
  result is ignored.

---

## 15. Heavy things most apps do not need (making generated apps lighter)

| Item | Evidence | Suggestion |
|---|---|---|
| `workmanager` feature with zero workers | `grep CoroutineWorker/HiltWorker` returns nothing; `core/network/build.gradle.kts:37-41` adds work-runtime, hilt-work and **KSP** to the network module for nothing | Drop it, or ship one real worker (the queue replayer is the natural one) |
| `push` → `firebase` → `analytics` + `flags` | `spec.py:169,189` | Split the Firebase features |
| Glance widget pulls in WorkManager and Room, and runs start-up init | §13 | RemoteViews variant, or on-demand WorkManager init |
| `room` (response cache and queue) on by default, alongside the OkHttp cache | `spec.py:107-119`, `HttpClientModule.kt:158-161` | Off by default; rename it; use one cache |
| `androidx.lifecycle.process` unused | `app/build.gradle.kts:90` | Use it for the lock and socket, or drop it |
| `ktor-client-auth` | §3.2 | PeriFlow's `HttpSend` interceptor |
| `ktor-client-logging` in release | `core/network/build.gradle.kts:29`; only used when `isDebug` | `debugImplementation`, or keep it and let R8 strip it (verify) |
| Three `OkHttpClient`s | §3.8 | One shared client |
| Media helpers with no callers | §9 | Trim |
| `core:devtools` → `core:flags` dependency unused | `core/devtools/build.gradle.kts:18-20` | Remove it, or add a flags tab |
| `kotlinx-coroutines-play-services` in `core:notification` while `RemoteConfigFeatureFlags` hand-wraps a Task "to avoid the adapter" | `core/notification/build.gradle.kts:23`, `RemoteConfigFeatureFlags.kt:31-34` | Pick one approach |

---

## 16. Worth adopting from PeriFlow and VitFleet

* **VitFleet `core/network/.../auth/BearerTokenCache.kt`**: clears Ktor's token cache on sign-out
  and on every token save. Alternatively, PeriFlow's `auth/Authentication.kt` and
  `AuthTokenProvider.kt` replace the Auth plugin with an `HttpSend` interceptor.
* **VitFleet `auth/SessionTokens.kt`**: serialised refresh that respects rotation, does not
  resurrect a signed-out session, and ends the session only on 401.
* **VitFleet `core/common/.../NetworkMonitor.kt`**: the shared `shareIn` monitor.
* **VitFleet `HttpClientModule.kt`**: `install(WebSockets)`, the `AppWebSocket` hardening, and the
  `NetworkRecording.kt` WebSocket filter.
* **VitFleet `ResponseCache.kt`**: the queue with refusals kept, the momentary-refusal set, the
  retry timer, the pass `Mutex`, owner scoping and `NETWORK_MIGRATION_1_2`.
* **PeriFlow `NetworkClient.kt`**: `parseBody` (empty body → `{}`), per-request `timeoutMillis`,
  and `RawBody`.
* **PeriFlow `HttpClientModule.kt`**: a `Logging` filter for credential paths, and `DefaultRequest`
  with an app-version header.
* **PeriFlow `lock/AppLock.kt`**: the fixed grace logic.
* **PeriFlow**: `CrashlyticsReporter.setEnabled`, the `setEnabled` and `flush` analytics contract,
  `database.clearAllTables()` as a `SessionScopedStore`, a POST_NOTIFICATIONS request in onboarding
  and settings, notification public versions, and `dismissedUpdateVersion`.
* **PeriFlow `AppDetentSheet.kt`**: a sheet with nested-scroll hand-off.

---

## 17. Test gaps

There are no tests for any of these:

* Bearer refresh, token-cache clearing or the sign-out race.
* 204 or empty bodies.
* The WebSocket. A single test would have caught the missing plugin.
* `NetworkMonitor` sharing.
* `AppLock.shouldLock` sequences. The bypass is a pure state-machine bug that a JVM test can
  catch.
* The devtools badge drag. A `createComposeRule` test with `performTouchInput { swipe(...) }`
  that asserts the bounds moved would catch the regression.
* `AppSlider` with `steps`.
* `busyOverlay` blocking a child click.

The existing `QueuedRequestReplayerTest` asserts "a request the server refuses is dropped", which
encodes the data-loss behaviour (§4).

---

## 18. Priority order for the overhaul

1. Devtools badge drag (§1), `busyOverlay` (§2.2), `AppSlider` steps (§2.1).
2. Ktor bearer-cache clearing and `SessionTokens` (§3.2, §3.3), and the session teardown set (§12).
3. WebSocket plugin, observer filter and hardening (§3.1).
4. `NetworkMonitor` sharing (§3.4), 204 handling (§3.5), cache exception safety and size cap (§3.7).
5. POST_NOTIFICATIONS request and a default tap intent (§8), and wiring the analytics opt-out (§7).
6. App-lock bypass (§11), Play update restart and nagging (§14).
7. Lightness: split the Firebase features, drop or justify WorkManager, the widget init cost, one
   OkHttp client, trimming media, renaming `room` (§15).
8. Recorder redaction and failure capture (§3.9), FileProvider path and camera URI (§9).
