# NET: Cronet / HTTP/3 transport and Brotli: research notes

Groundwork for package NET (REQUIREMENTS 39). Research only: nothing in any worktree or repository was changed, nothing was built.
Written 2026-10-18. Template facts come from `git show` on `overhaul/multiplatform-v2` and `wp/WP06` (WP06 is **not** merged
into the integration branch yet: `git merge-base --is-ancestor wp/WP06 overhaul/multiplatform-v2` is false).

Marks: **[V]** verified here (source code, Maven listing, official reference read today). **[I]** inferred from verified facts.
**[U]** not verified; NET must check it (usually on the phone or in a release build).

## Summary

1. Template (wp/WP06): OkHttp **5.5.0** (explicit), Ktor **3.6.0** (OkHttp engine, itself on OkHttp 5.5.0), Coil **3.6.3** (declares OkHttp 4.12.0, resolves to 5.5.0), AGP **9.4.1**, Kotlin **2.4.20**, compileSdk/targetSdk **37**, minSdk **26**. WP06 adds one `@Singleton OkHttpClient` that every Ktor client and Coil share; it has **no OkHttp interceptors at all** (auth, logging, UA, headers, recording are Ktor plugins). [V]
2. Latest: `com.google.net.cronet:cronet-okhttp:0.1.2` (Google Maven, 2026-10-05), `org.chromium.net:cronet:500.1.0` (new unified Java artifact, 2026-10-01; `cronet-api`, `cronet-common`, `cronet-fallback`, `cronet-shared`, `httpengine-native-provider` are now empty transitional artifacts), `cronet-bundled:500.1.0` (`cronet-embedded` is now an alias), `com.google.android.gms:play-services-cronet:18.1.1`, `com.squareup.okhttp3:okhttp-brotli:5.5.0` (+ `org.brotli:dec:0.1.2`). [V]
3. OkHttp 5: bridge v0.1.2 (tag 2026-10-02) is the first release after the OkHttp 5 fixes (#52 NPE, "compatible with OkHttp 5+", "source compatibility with OkHttp 5.4.0"); its POM still lists OkHttp 3.12.13 as a floor, so Gradle resolves 5.5.0. [V]
4. **Q3: the bridge takes only `org.chromium.net.CronetEngine`, but Cronet's own `HttpEngineNativeProvider` (inside `org.chromium.net:cronet`, which the bridge already depends on) wraps the platform `android.net.http.HttpEngine` as a `CronetEngine`. No adapter is needed; item 39's order becomes a choice of Cronet provider.** [V]
5. HttpEngine is available where `SDK_INT >= R && SdkExtensions.getExtensionVersion(S) >= 7`, not strictly API 34+. [V]
6. NET must pick the provider explicitly. Cronet's default `CronetEngine.Builder(context)` falls back to the HttpURLConnection-based Java provider, but item 39 wants plain OkHttp in that case. [V]
7. Bypassed on the Cronet path: the OkHttp `Cache`, `CookieJar`, `CertificatePinner`, network interceptors, connect timeout, the intermediate `EventListener` events and WebSockets. OkHttp sends WebSocket upgrades through **application** interceptors, so they must be routed around Cronet explicitly. [V]
8. Brotli: `BrotliInterceptor` = `CompressionInterceptor(Brotli, Gzip)`, which must be an application interceptor. On the Cronet path the bridge strips `Content-Encoding`/`Content-Length` after Cronet decodes, so nothing is decoded twice. [V]
9. Top risks: the bridge polls every 500 ms for the process lifetime (#54, open, reported on our exact stack); Licensee fails the build on Cronet's Chromium-LICENSE URL; reflection-loaded providers under R8; the HTTP/3 benefit is only measurable once Alt-Svc is known.
10. Measuring: `cdn.jsdelivr.net` and `http3.is` serve h3 + br; `images.unsplash.com`, `quic.nginx.org`, `www.google.com` serve h3. Check the protocol of every request with Ktor's `response.version == QUIC`. Without root, packet loss cannot be made on the phone itself; use the dev-option rate limit, adb network toggles, or shaping upstream.

---

## 1. Template versions and how the shared OkHttpClient is built

### Versions (`templates/android/gradle/libs.versions.toml`, `build-logic/.../AppConfig.kt`) [V]

| Item | overhaul/multiplatform-v2 | wp/WP06 |
|---|---|---|
| AGP | 9.4.1 | 9.4.1 |
| Kotlin / KSP | 2.4.20 / 2.3.12 | same |
| compileSdk / targetSdk / minSdk | 37 / 37 / 26 | 37 / 37 / 26 |
| Ktor | 3.6.0 | 3.6.0 |
| OkHttp | not declared (transitive from Ktor 3.6.0: `okhttp-jvm:5.5.0`, `okio-jvm:3.18.2`) | `okhttp = "5.5.0"`, `libs.okhttp`, `api(libs.okhttp)` in `:core:network` |
| Coil | 3.6.3 (`coil-compose`, `coil-network-okhttp`; its POM declares okhttp 4.12.0) | 3.6.3 |
| Hilt / coroutines | 2.60.1 / 1.11.0 (`kotlinx-coroutines-play-services` already in the catalog) | same |
| Licensee | 1.14.1 | same |

Maven Central today: okhttp 5.5.0 is the latest release (2026-08-16), Ktor 3.6.0 is the latest (2026-09-16), and Coil 3.6.3 is the latest (2026-09-18). [V]

### Wiring on the integration branch (pre-WP06) [V]
Each Ktor client builds its own OkHttp inside `engine { config { cache(httpCache); certificatePinner(...) } }`. There is no shared client, and auth uses Ktor's `Auth` plugin. NET must not target this shape.

### Wiring on wp/WP06 (what NET builds on) [V]
`core/network/.../di/HttpClientModule.kt`:
- `provideOkHttpClient(config, cache)`: `OkHttpClient.Builder().cache(cache)` (50 MB `Cache` in `cacheDir/http`), plus `certificatePinner(...)` from `NetworkConfig.certificatePins: Map<String, List<String>>` (`"sha256/…"` strings) when the map is not empty. **No `addInterceptor`, no `addNetworkInterceptor`, no `eventListener`, no `cookieJar`.**
- `@AuthenticatedClient` and `@PlainClient` are `HttpClient(OkHttp)` with `engine { preconfigured = okHttp }`, `expectSuccess = false`, and these Ktor plugins: `HttpTimeout` (request 30 s, connect 15 s, socket 30 s), `UserAgent` (ASCII-filtered), `DefaultRequest` (`X-App-Version`), `Logging` (debug only), `ResponseObserver` + `HttpSend` intercept (recording), and the template's own `authenticate(...)`.
- `@RealtimeClient` (`<opt:websocket>`): the **same** `okHttp` as `preconfigured`, plus `WebSockets { pingInterval = 20 s }`. `webSocketFactory` is not set, so Ktor uses the shared client as the `WebSocket.Factory` (`OkHttpEngine.kt:90`: `webSocketFactory = config.webSocketFactory ?: engine`).
- `provideHttpCacheClearing`: a `SessionScopedStore` calls `cache.evictAll()` on sign-out.
- Ktor's OkHttp engine with `preconfigured` derives per-timeout clients with `preconfigured.newBuilder()` (LRU, `clientCacheSize = 10`) and keeps the dispatcher. Every derived client therefore shares the interceptor instances, the pool and the dispatcher (`OkHttpEngine.kt:159-167`).
- Coil (commit 10e2916e, `BaseAppApplication.kt`, `<opt:coil&network>`): `SingletonImageLoader.setSafe { ImageLoader.Builder(ctx).components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttp.get().newBuilder().cache(null).build() })) } }`, with `okHttp: dagger.Lazy<OkHttpClient>`. `newBuilder()` keeps the application interceptors, so **an interceptor added to the shared client reaches Ktor, the derived timeout clients and Coil**. That is the seam NET needs. [V]/[I]
- The template has no cookie handling: no `CookieJar` and no Ktor `HttpCookies`, only redaction of `cookie`/`set-cookie` in DevToolsLog. It installs no Ktor `ContentEncoding`. [V by `git grep`]

---

## 2. Artifacts, latest versions, OkHttp 4 vs 5

| Coordinates | Latest | Published | Notes | Source |
|---|---|---|---|---|
| `com.google.net.cronet:cronet-okhttp` | **0.1.2** (0.1.0, 0.1.1, 0.1.2) | AAR 2026-10-05; tag v0.1.2 2026-10-02 (`41d108f`) | AAR 104 KB. POM deps: `okhttp 3.12.13`, `okio 2.10.0` (floors), `guava 31.1-android`, `org.chromium.net:cronet 500.1.0`, `kotlin-stdlib 2.2.21`, `androidx.annotation 1.9.1`, `error_prone_annotations 2.46.0`. 0.1.1 still depended on `cronet-api 98.4758.101`. **Google Maven only.** | Google Maven POM [V] |
| `org.chromium.net:cronet` | **500.1.0** (500.0.1, 500.0.2, 500.1.0) | 2026-10-01 | "The Cronet HTTP client library for Android." AAR 655 KB, Java only (API + providers, including `HttpEngineNativeProvider` and the Java fallback). License **"Chromium License…" by URL** `https://chromium.googlesource.com/chromium/src/+/refs/heads/main/LICENSE`. | POM [V] |
| `org.chromium.net:cronet-api` | 500.1.0 | 2026-10-01 | "(DEPRECATED) … transitional empty artifact that merely pulls the cronet artifact." The last real API release was 143.7445.0. The same holds for `cronet-common`, `cronet-fallback`, `cronet-shared` and `httpengine-native-provider` (the latter existed as a real artifact since 141.7340.3). | POMs [V] |
| `org.chromium.net:cronet-bundled` | 500.1.0 | 2026-10-01 | "This artifact contains the entire Cronet library itself for direct embedding, and enables the corresponding Provider." **AAR 13.96 MB** (native libraries for all ABIs). | POM, HEAD [V] |
| `org.chromium.net:cronet-embedded` | 500.1.0 | 2026-10-01 | 500.x is a deprecated alias pulling `cronet-bundled`. 143.7445.0 was the last real one (AAR 13.7 MB). | POM [V] |
| `com.google.android.gms:play-services-cronet` | **18.1.1** | 2025-10-09 | AAR 92 KB. Deps: `play-services-base 18.5.0`, `basement 18.9.0`, `tasks 18.2.0`, `cronet-api 141.7340.3` (resolves to the 500.1.0 transitional artifact, then `cronet 500.1.0`). The docs page still shows 18.0.1. | POM [V] |
| `com.squareup.okhttp3:okhttp-brotli` | **5.5.0** | 2026-08-16 | jar 4 KB. Deps: `okhttp 5.5.0` (`requires` in the Gradle module), `org.brotli:dec 0.1.2`, `kotlin-stdlib 2.1.21`. Must match the okhttp version, so use `version.ref = "okhttp"`. | Maven Central POM/.module [V] |
| `org.brotli:dec` | 0.1.2 (latest; 2017) | 2017-05-07 | jar 98 KB, including the 122 KB built-in dictionary as string data. License: MIT by URL `http://www.opensource.org/licenses/mit-license.php`. | Maven Central [V] |
| (`com.squareup.okhttp3:okhttp-zstd` 5.5.0) | — | — | Exists; uses `com.squareup.zstd:zstd-kmp-okio` (native). Not proposed. | [V] |

No release notes exist for the 500.x Cronet line; searches found nothing. The jump from 143.7445.0 to 500.0.1 looks like versioning decoupled from Chromium milestones. [I]/[U]

**OkHttp 4 vs 5 compatibility of the bridge** [V unless marked]
- Commit history: "Ensure compatibility with OkHttp 5 when setting ResponseBody. Fixes #52" (2026-04-02; #52 was an NPE in `ResponseConverter.createResponse` on 0.1.1), "Make the bridge compatible with OkHttp 5+." (2026-07-29), "Ensure source compatibility with OkHttp 5.4.0" (2026-09-11), and "Restore original versions of okio and okhttp dependencies" (2026-10-02, the v0.1.2 tag). **0.1.2 is the first release with the OkHttp 5 work; do not use 0.1.1 with OkHttp 5.**
- `ResponseConverter` handles both versions explicitly: with no body it leaves the field unset, so OkHttp 4 keeps `null` and OkHttp 5 keeps `ResponseBody.EMPTY`.
- Issue #54 was filed against **cronet-okhttp 0.1.2 + OkHttp 5.5.0 + play-services-cronet 18.1.1 on Android 17**, so the stack runs functionally. [V from issue text]
- Protocol: the bridge maps `h3`/`quic` to `Protocol.QUIC`. OkHttp 5.5.0 still has `QUIC("quic")` and adds `HTTP_3("h3")`. Ktor 3.6.0 maps `QUIC` to `HttpProtocolVersion.QUIC` and `HTTP_3` to `HTTP_3_0` (`OkUtils.kt:70-77`). On the Cronet path, Ktor's `response.version` therefore reports **QUIC** for HTTP/3.
- Okio 2.10 is the bridge's floor and the template resolves 3.18.2. Okio 3 kept the `okio.Okio` Java facade. [I], not run [U].

---

## 3. HttpEngine or CronetEngine? (the design question)

**Bridge API (google/cronet-transport-for-okhttp, master).** The code has `public final class CronetInterceptor implements Interceptor, AutoCloseable` and `public static Builder newBuilder(CronetEngine cronetEngine)`, importing `org.chromium.net.CronetEngine`. `CronetCallFactory` takes the same type. There is no `android.net.http.HttpEngine` overload. The README never mentions HttpEngine, and issue #20 "Add a parallel version using android.net.http.HttpEngine" (2023-03-31) is open with no maintainer reply. **The bridge accepts only `CronetEngine`.** [V]

**Cronet already adapts HttpEngine into a CronetEngine.** Source: `components/cronet/android/java/src/org/chromium/net/impl/HttpEngineNativeProvider.java` and `AndroidHttpEngineBuilderWrapper.java`, Chromium main [V]:
- `HttpEngineNativeProvider extends CronetProvider`: "A Cronet provider implementation which loads the HttpEngine implementation from the Android platform." `getName()` returns `PROVIDER_NAME_HTTPENGINE_NATIVE = "HttpEngine-Native-Provider"`.
- `createBuilder()` returns `new ExperimentalCronetEngine.Builder(new AndroidHttpEngineBuilderWrapper(mContext, new HttpEngine.Builder(mContext)))`. The wrapper's `build()` returns `new AndroidHttpEngineWrapper(mBackend.build())`, an `ExperimentalCronetEngine` backed by the platform `HttpEngine`.
- `isEnabled()` = `isHttpEngineAvailable()` = `Build.VERSION.SDK_INT >= R && SdkExtensions.getExtensionVersion(S) >= 7` (`@RequiresExtension(extension = 31, version = 7)` in the reference docs). The provider also covers devices below API 34 whose Connectivity/Tethering mainline module reports S-extension 7. [V]; which real devices qualify is [U].
- It ships in `org.chromium.net:cronet` 500.x, which `cronet-okhttp:0.1.2` already pulls (compile scope). Before 500.x it was the separate `httpengine-native-provider` artifact (141.7340.3+). [V]
- Documented limits: no netlog, no internal metrics, and experimental features not guaranteed. The wrapper ignores `setThreadPriority`, logs that `NetworkQualityEstimator` is unsupported, and translates only known experimental options (connection migration, DNS, QUIC options). [V]

**So:** `CronetInterceptor.newBuilder(httpEngineBackedCronetEngine).build()` uses the platform HttpEngine through the existing bridge, with no adapter, no own `Call.Factory`, and no Play Services. That follows from the source [V]; running it on the phone is [U]. The alternatives are worse:
- **Own HttpEngine to OkHttp adapter:** it would re-implement the bridge's ~15 classes against `android.net.http` (the same API shape in another package) for no gain now that the provider exists.
- **Play Services / embedded only:** gives up the platform engine, which needs no Play Services. That matters on custom ROMs; the test phone runs one.
- **`HttpEngine.openConnection` / `createUrlStreamHandlerFactory`:** no OkHttp integration.

**What it means for the selection order.** Default selection [V] (`CronetEngine.getPreferredCronetProvider`, `CronetProvider.getAllProviderInfos`) sorts by score, and the first enabled provider wins:

| Provider | Score |
|---|---|
| resource-declared provider | 6 |
| App-Packaged (`cronet-bundled`, `NativeCronetProvider`) | 5 |
| HttpEngine-Native, when `HttpEngine.getVersionString()` ≥ flag `Cronet_PreferredMinimumHttpEngineVersion` (default `"133.0.6876.3"`) | 4 |
| Play Services (`com.google.android.gms.net.PlayServicesCronetProvider`) | 3 |
| older HttpEngine | 2 |
| Java fallback (`JavaCronetProvider`, "Fallback-Cronet-Provider", HttpURLConnection-based, "significantly degraded performance") | 1 |

If nothing else is enabled, the default builder hands back the **Java fallback**. Running OkHttp through the bridge over HttpURLConnection is strictly worse than plain OkHttp, which contradicts item 39. NET therefore selects explicitly from `CronetProvider.getAllProviders(context)`:

1. **HttpEngine-Native** if `isEnabled()`.
2. **Google-Play-Services-Cronet-Provider** (`CronetProviderInstaller.PROVIDER_NAME`), after `CronetProviderInstaller.installProvider(context)` succeeds. It returns `Task<Void>`; `Tasks.await` must not run on the main thread. Failures are `GooglePlayServicesNotAvailableException` or `GooglePlayServicesRepairableException`. [V]
3. App-Packaged, only if the documented `cronet-bundled` option is on (see the open question below).
4. **Otherwise no engine, so plain OkHttp.** Never `Fallback-Cronet-Provider`.

Item 39's order (HttpEngine, then Play Services, then OkHttp) stands as agreed. Two details to settle in NET (open questions, not a re-argument):
- (a) Test the gate as "HttpEngine available" (the provider's `isEnabled`), not `SDK_INT >= 34`.
- (b) Cronet's own heuristic ranks an *old* platform HttpEngine (< 133.0.6876.3, i.e. an un-updated early Android 14 mainline) below Play Services. Adopting that guard keeps "best engine first".
- If `cronet-bundled` is opted into, its exact pinned version is the reason to bundle, so it would go first.

---

## 4. HTTP/3, Brotli, connection migration and pinning per engine

All three providers are driven through the same `CronetEngine.Builder` API. For HttpEngine, `AndroidHttpEngineBuilderWrapper` forwards each call to `android.net.http.HttpEngine.Builder` [V].

| Concern | `CronetEngine.Builder` (Play Services / bundled / HttpEngine via wrapper) | `HttpEngine.Builder` (what the wrapper calls) |
|---|---|---|
| HTTP/2 | `enableHttp2(boolean)`, **default on** [V docs + `CronetEngineBuilderImpl`] | `setEnableHttp2(boolean)` [V api] |
| QUIC / HTTP/3 | `enableQuic(boolean)`, **default on** [V]. `addQuicHint(host, port, altPort)` tries QUIC on the first request; otherwise QUIC starts only after Alt-Svc is learned [I, standard Chromium behaviour]. Docs: 0-RTT across sessions needs `HTTP_CACHE_DISK` [V]. `setQuicOptions` (experimental): allowed hosts, idle timeout, handshake UA. | `setEnableQuic`, `addQuicHint`, `setQuicOptions(android.net.http.QuicOptions)` [V api] |
| Brotli | `enableBrotli(boolean)`, **default off** (`enableBrotli(false)` in the impl constructor) [V]. It advertises `br` in Accept-Encoding and Cronet decodes. | `setEnableBrotli(boolean)`; the wrapper maps it [V] |
| zstd | Cronet advertises zstd only if feature `EnableZstdV2` is on; **disabled by default** (`cronet_context.cc`) [V] | same code base [I] |
| Connection migration (QUIC only) | `setConnectionMigrationOptions(ConnectionMigrationOptions)`. Builder: `enableDefaultNetworkMigration`, `enablePathDegradationMigration` (stable), `allowNonDefaultNetworkUsage` (experimental, "can result in requests being sent on non-default metered networks"), `migrateIdleConnections`, `setMaxTimeOnNonDefaultNetworkSeconds`, etc. Defaults are not documented [V docs / U defaults]. | `setConnectionMigrationOptions(android.net.http.ConnectionMigrationOptions)` with `setDefaultNetworkMigration`, `setPathDegradationMigration`, `setAllowNonDefaultNetworkUsage` (int `MIGRATION_OPTION_*`) [V api]. The wrapper builds these from experimental options (`migrate_sessions_on_network_change_v2`, `allow_port_migration`, `migrate_sessions_early_v2`) [V]. That `CronetEngine.Builder.setConnectionMigrationOptions` reaches it through that translation is [I]: `getSupportedConfigOptions()` lists only PROXY_OPTIONS. |
| Public-key pins | `addPublicKeyPins(String host, Set<byte[]> pinsSha256, boolean includeSubdomains, java.util.Date expiration)`. Pins are SHA-256 of the DER SPKI. A host made only of digits and dots is invalid. A second call for the same host overrides the first. A chain with no pin fails even if the trust store accepts it [V docs]. | `addPublicKeyPins(String, Set<byte[]>, boolean, java.time.Instant)` [V api]. The wrapper converts `Date.toInstant()` [V]. |
| Pin bypass for user CAs | `enablePublicKeyPinningBypassForLocalTrustAnchors(boolean)`, **default true** (impl constructor) [V]. Docs call disabling it "highly discouraged" (it breaks local proxies). | `setEnablePublicKeyPinningBypassForLocalTrustAnchors` [V] |
| Cache / persistence | `enableHttpCache(mode, maxSize)`, modes DISABLED(0), IN_MEMORY(1), DISK_NO_HTTP(2), DISK(3); **default disabled**. `setStoragePath(dir)` must exist, must not be shared between engines, and is required for disk modes [V docs]. | `setEnableHttpCache`, `setStoragePath` [V] |
| Cookies | Native Cronet's network delegate **refuses to send and to save cookies** ("Disallow sending cookies by default", `OnCanSetCookie` returns false) [V `cronet_context.cc`]. That matches OkHttp's default `CookieJar.NO_COOKIES`. | Same Chromium code [I] |
| Cleartext / trust | Cronet checks `NetworkSecurityPolicy.isCleartextTrafficPermitted(host)` and verifies with `X509TrustManagerExtensions.checkServerTrusted(chain, authType, host)` [V Chromium `AndroidNetworkLibrary`, `X509Util`]. | Same [I] |

**Mirroring the template's `CertificatePinner` into Cronet** (`NetworkConfig.certificatePins: Map<String, List<"sha256/BASE64">>`):
- `sha256/…` → base64-decode to 32 bytes. `sha1/…` (OkHttp still accepts it) → **unsupported by Cronet: reject.**
- Exact `host` → `(host, includeSubdomains = false)`.
- `**.example.com` → `("example.com", true)`. This is exact: OkHttp's `**` matches "any number of prefixes … including no prefix" [V KDoc].
- `*.example.com` (exactly one label) has **no Cronet equivalent**. Reject it, or widen it to `("example.com", true)`. Recommend rejecting at startup in debug builds and in the pin-translator test.
- Expiration: Cronet requires one; OkHttp has none. Chromium stops enforcing expired pins [U], so use a far-future date (e.g. build time + 10 years) and say so in the docs.
- Bypass: set `enablePublicKeyPinningBypassForLocalTrustAnchors(false)` when pins exist, for parity. OkHttp's pinner has no user-CA bypass; it fails if the cleaned chain matches no pin. [I]
- Alternative single source of truth: a Network Security Config `<pin-set>`. Because Chromium verifies through the host-aware `X509TrustManagerExtensions`, the platform's NSC trust manager should enforce the pin-set on OkHttp, all Cronet providers and WebView alike [I; U on HttpEngine and Play Services]. A deliberately wrong pin on the phone settles it.

---

## 5. What the bridge bypasses or does not support

The README says: "The entirety of OkHttp core is bypassed. This includes caching, retries, authentication, and network interceptors." The interceptor must be the **last** application interceptor. [V README + `CronetInterceptor` Javadoc]

| OkHttp feature | On the Cronet path | Workaround / template impact |
|---|---|---|
| **WebSockets** | "WebSockets are not supported" [V README, issue #39 open]. `RealWebSocket.connect` sets `Upgrade: websocket` and runs a `RealCall(forWebSocket = true)`, whose chain **includes `client.interceptors`** (application) and skips only network interceptors (`RealCall.kt:213-219`) [V]. The bridge has no Upgrade check [V], so Cronet would receive the upgrade, and OkHttp needs a raw `exchange`, which fails. | Route around it: the transport interceptor sends any request with an `Upgrade` header to `chain.proceed()`. Belt and braces: `@RealtimeClient` sets `engine { webSocketFactory = <client without Cronet> }` (Ktor `OkHttpConfig.webSocketFactory` exists [V]). |
| Application interceptors | Run if placed **before** the Cronet interceptor [V]. | Order: `BrotliInterceptor`, then the Cronet transport (last). The template's own concerns (auth, logging, UA, `X-App-Version`, recording, retry) are Ktor plugins above the engine, so they are unaffected [V]. |
| Network interceptors | Never invoked [V]. | The template has none [V]; docs must say "use application interceptors or Ktor plugins". |
| `Cache` (OkHttp) | Bypassed; `CacheInterceptor` sits below the application interceptors [V]. | Cronet's own cache could replace it, but **Cronet has no evict-all call, so the sign-out `cache.evictAll()` parity breaks** [I from API surface]. Recommend `HTTP_CACHE_DISK_NO_HTTP` (persists QUIC/server data only, with no response bodies) and documenting "no HTTP response cache on the Cronet path". Coil's own disk cache is above OkHttp and unaffected. |
| `CookieJar` | Bypassed; it lives in `BridgeInterceptor` (`loadForRequest`/`receiveHeaders`) [V]. Cronet itself neither sends nor saves cookies [V]. | Parity holds for the template (no cookies). A generated app that needs cookies should use Ktor's `HttpCookies` plugin, which works on both paths [I]. |
| `CertificatePinner`, `HostnameVerifier`, `ConnectionSpec`, `Dns`, `Proxy`, `SocketFactory`, `ConnectionPool`, protocols | Bypassed: "Most `OkHttpClient` network configuration is bypassed and must be set on the `CronetEngine` builder" [V]. | Mirror pins (section 4). Proxy has `CronetEngine.Builder.setProxyOptions` [V docs]; it is unused by the template. |
| Timeouts | The bridge passes `chain.readTimeoutMillis()` and `chain.writeTimeoutMillis()` to the converter; **connect timeout is not used** [V source]. A call timeout or coroutine cancellation cancels the `Call`; a 500 ms poller notices and cancels the `UrlRequest` ("Call cancellation propagates with a delay") [V]. | Ktor `HttpTimeout`: socket timeout maps to read/write and is honoured. Connect timeout (15 s) is ignored, and Chromium's own connect limit applies. Request timeout (30 s) works through cancellation, up to 500 ms late. Docs must say this. |
| `EventListener` | "Intermediate `EventListener` stages are not reported" [V]. The HttpEngine provider also exposes no Cronet metrics [V]. | The template uses no `EventListener` [V]. Measure end to end at the Ktor level (the recorder already has `durationMillis`). |
| Redirects / retries / `Authenticator` | Cronet follows redirects per the bridge's `RedirectStrategy` (default: follow). OkHttp's `RetryAndFollowUp` is bypassed [V]. | Ktor's `HttpRedirect` and the template's `RetryPolicy` sit above and are unaffected. Issue #44: `rewind()` fails on 307/308 with a request body (0.1.0, still open) [V]. |
| Headers | One value per header name ("Cronet uses the last value"). Custom `Accept-Encoding` is ignored and Cronet sets its own. A body without a content type gets `application/octet-stream` [V]. | No call site may set `Accept-Encoding` (generator test). |
| `Response` fields | `handshake`, `networkResponse`, `cacheResponse`, `sentRequestAtMillis`/`receivedResponseAtMillis` are unset. Parsing is more lenient [V]. | Coil 3's default cache strategy does not use the timestamps [I]. Do not read `handshake` in template code. |
| Tags | Supported by the interceptor; `CronetCallFactory.newCall(...).tag()` throws [V]. | Use the interceptor, not the call factory. |
| Threads, ANR | Init loads native code. Issue #28: ANRs when `installProvider`/`build` run on main; the maintainer advises a background thread [V]. | Pre-warm the engine on IO at app start, with a lazy bounded wait in `intercept()` (always on a dispatcher thread). |
| Idle cost | **`CronetInterceptor` runs `scheduleAtFixedRate` every 500 ms from construction until `close()`, whether or not calls are in flight.** `close()` leaks the upload executor (`newFixedThreadPool(uploadDataProviderExecutorSize)`, default 4) [V source, issue #54 open, no reply]. | One interceptor per process, which the shared client gives us. `setUploadDataProviderExecutorSize(1)` to limit threads. Measure idle wakeups with Perfetto. If they cost real battery, close the interceptor on `ProcessLifecycleOwner` stop and rebuild on demand (accepting about 1 parked thread per cycle), or vendor a patched bridge (Apache-2.0). |

---

## 6. okhttp-brotli

- **What it is (5.5.0)** [V source]: `object BrotliInterceptor : CompressionInterceptor(Brotli, Gzip)`.
  - **Request side:** only if the request has **no** `Accept-Encoding`, it sets `Accept-Encoding: br, gzip`.
  - **Response side:** if `promisesBody()` and `Content-Encoding` is exactly `br` or `gzip` (case-insensitive, single token), it wraps the body in `BrotliInputStream` or gzip, then removes `Content-Encoding` and `Content-Length` (length becomes -1).
  - **Otherwise:** it passes the response through.
  - `Brotli` = `BrotliInputStream(compressedSource.inputStream()).source()` from `org.brotli:dec`.
- **Relation to OkHttp's transparent gzip** [V `BridgeInterceptor.kt:65-92`]: `BridgeInterceptor` adds `Accept-Encoding: gzip` and decodes only when the request had no `Accept-Encoding` (and no `Range`). Because `BrotliInterceptor` runs earlier and sets the header, `BridgeInterceptor` stands down and BrotliInterceptor takes over gzip too. That is what its KDoc means by "this replaces the transparent gzip compression in BridgeInterceptor".
- **Must be an application interceptor (`addInterceptor`)** [I from the chain order `client.interceptors` → RetryAndFollowUp → Bridge → Cache → Connect → network interceptors]. As a network interceptor it would see the `gzip` header Bridge already added and do nothing, so `br` would never be advertised.
  - As an application interceptor it sits above `Cache`. The cache therefore stores the *encoded* `br` body and decodes on every read, which is fine.
  - Existing cache entries with `Vary: Accept-Encoding` miss once after the change.
- **Range:** unlike Bridge, `BrotliInterceptor` also advertises encodings on `Range` requests. The template has none; a resumable-download feature should set `Accept-Encoding: identity`.
- **Decoder size:** `org.brotli:dec:0.1.2` jar is 98 KB and holds the 122 KB dictionary, which R8 cannot shrink. `okhttp-brotli` is 4 KB. Expect roughly 100 KB in a release APK [I]; REQUIREMENTS 39 asks NET to measure it (apkanalyzer diff).
- **Licensee:** brotli's MIT URL `http://www.opensource.org/licenses/mit-license.php` is in Licensee's built-in MIT fallback URLs on trunk (`defaultFallbackUrls.kt`) [V]. Whether 1.14.1 has it is [U]; run the licensee task in NET because this touches **every** generated app.
- **Ktor `ContentEncoding`** [V Ktor docs + OkHttp source]: the plugin "Sets the Accept-Encoding header" and decodes; it supports gzip/deflate, with brotli only through a custom `ContentEncoder`. If someone installs it:
  - **OkHttp path:** Ktor's header reaches OkHttp. `BrotliInterceptor` and `BridgeInterceptor` both stand down, and Ktor decodes gzip. Nothing is decoded twice, but **Brotli is silently lost**.
  - **Cronet path:** Cronet ignores the custom header, decodes itself, and the bridge strips `Content-Encoding`, so Ktor sees an identity body.
  - Rule and generator test: no `ContentEncoding` in the template.
- **Cronet path:** `ResponseConverter` drops `Content-Encoding` and `Content-Length` when every listed encoding is one Cronet decodes (`br`, `deflate`, `gzip`, `x-gzip`) [V]. `BrotliInterceptor` above Cronet therefore sees no encoding and passes the body through, so nothing is decoded twice.
  - `Accept-Encoding` on that path is Cronet's own, so **`enableBrotli(true)` on the engine is what gives `br` there.**
  - Latent bug: if a provider ever turns on zstd (`EnableZstdV2`, off by default), the bridge, whose set lacks `zstd`, would forward a decoded body that still carries `Content-Encoding: zstd` and the compressed length [I].
- **Coil:** it uses the shared client, so images also send `br, gzip`. Image formats are rarely content-encoded; SVG/JSON (Lottie) benefit. Coil writes the already-decoded bytes to its disk cache [I]. Nothing decodes twice.
- **Side effect for Ktor callers:** `contentLength()` is null for compressed responses on both paths, just as with OkHttp's gzip today. A download-progress UI loses its total size.

---

## 7. Measuring fairly on the phone

### Endpoints
Probed 2026-10-18 with `curl -sI`. Schannel curl has no HTTP/3, so `alt-svc` is the evidence. [V]

| Endpoint | h3 (Alt-Svc) | br | Use |
|---|---|---|---|
| `https://cdn.jsdelivr.net/npm/<pkg>@<ver>/…` (e.g. a large `.js`/`.json`) | yes | yes | **Primary H3 + Brotli JSON/text** (Cloudflare). Pinned versions keep payloads stable. |
| `https://http3.is/` | yes | yes | H3 + br sanity check |
| `https://images.unsplash.com/photo-…?w=400` (imgix) | yes | n/a (jpeg) | H3 image loading through Coil |
| `https://picsum.photos/…` (302 to Fastly) | yes | n/a | Image alternative; the redirect costs an extra request |
| `https://quic.nginx.org/`, `https://www.google.com/`, `https://www.gstatic.com/generate_204` | yes | no / gzip | H3 latency; `generate_204` is ideal for tiny RTT probes |
| `https://httpbin.org/brotli`, `https://httpbin.org/headers` | no | yes | Brotli correctness, and **seeing which Accept-Encoding each path sends** |
| `https://android-base.vercel.app/` (the user's site) | no | yes | Brotli gain the user cares about (item 39) |
| `https://www.cloudflare.com/cdn-cgi/trace` | not in headers | yes | Body shows `http=` as Cloudflare saw it: an independent protocol check |
| `https://speed.cloudflare.com/__down?bytes=N` | no alt-svc header | no | Sized payloads for throughput (uncompressed) |

### Method
- **One release APK, two transports.** Add a dev-flavour launch extra (the `DevLaunchOptions` pattern on wp/WP04-perf), e.g. `perf.transport=okhttp|cronet|httpengine|gms`, that forces the selector. A/B then compares the same R8 output.
- **Logging.** Add a `perf.net:` logcat line per exchange in the `ShellLog` style: protocol, provider, ms, bytes. A `tools/perf/netbench.py` installs, runs and parses.
- **Prove the protocol on every sample:** Ktor `response.version` (`QUIC` = h3 via the bridge, `HTTP_2_0`, `HTTP_1_1`). A "Cronet" sample that ran over h2 is reported as such, not as h3.
- **Cold vs warm.** "Cold" = `am force-stop`, then the first request. It includes engine init (HttpEngine build, or GMS `installProvider` + build) and the TLS/QUIC handshake. "Warm" = the next request on the open connection. "New connection, warm process" = a second host.
  - On the very first run Cronet has no Alt-Svc, so the first connection is TCP.
  - Run each condition with (a) `addQuicHint` for the test host and (b) only persisted Alt-Svc (storage path + `HTTP_CACHE_DISK_NO_HTTP`). Whether Alt-Svc persists in DISK_NO_HTTP mode is [U] and is the first thing to check.
- **Fairness.** Interleave ABAB within the same minutes and on the same network. Use ≥30 samples per cell and report median, p90 and spread. Disable HTTP caches (OkHttp cache off for the A/B build path, Cronet cache off) and Coil memory/disk caches (`CachePolicy.DISABLED`).
  - The screen stays on, battery saver is off, and the run happens before thermal throttling (the Redmi is the slow reference device).
  - Bytes: `TrafficStats.getUidRxBytes(myUid)` deltas count on both engines; EventListener only works on OkHttp.
- **Images.** N fixed URLs (unsplash/imgix), with time-to-all-decoded through `SingletonImageLoader` and caches disabled.
- **Brotli.** The same resource with `br, gzip` vs `gzip` (toggle `BrotliInterceptor` in the dev build). Record rx bytes and latency against jsDelivr and the Vercel site, and the APK size delta (`apkanalyzer` from cmdline-tools: release APK with and without).
- **Idle cost of the bridge (#54).** Take Perfetto traces (trace_processor is installed under `D:\.caches\perfetto`) of the app idle in the foreground and just after going to the background. Count wakeups of the `pool-N-thread-1` poller.

### Lossy and changing networks without root
- **Bandwidth cap (both engines, including UDP):** Developer options → "Network download rate limit" (Android 13+; Settings.Global `ingress_rate_limit_bytes_per_second`, ≤0 disables, applied per network; 1 Mbps = 125000) [V Esper]. That a value written with `adb shell settings put global …` takes effect on this ROM is [U]. It caps bandwidth only; there is no loss or latency.
- **Network switches (connection migration):** `adb shell cmd connectivity airplane-mode enable|disable`, `adb shell cmd wifi set-wifi-enabled disabled|enabled`, `adb shell svc data disable|enable`, timed in the middle of a long download (`speed.cloudflare.com/__down?bytes=50000000`). These are standard shell commands; per-ROM behaviour is [U].
- **Real loss:** a reproducibly weak-signal spot (cellular, or a distant Wi-Fi AP) with ABAB interleaving. It is not repeatable across days, so compare only within one session.
- **True packet loss and delay** need an upstream shaper:
  - (a) the PC's Windows Mobile hotspot plus **clumsy** (WinDivert; loads a signed kernel driver, so **this needs the user's OK under the machine rules**). Whether clumsy shapes hotspot-forwarded traffic is [U].
  - (b) a router with OpenWrt `tc netem`.
- **Not suitable:**
  - `adb reverse` + Toxiproxy: TCP only, so no QUIC.
  - An HTTP proxy on Wi-Fi: Cronet with a proxy will not use QUIC.
  - gnirehtet: tunnels UDP inside adb's TCP, which distorts loss.
  - The emulator: none on this machine, and it has no loss model anyway.
- **Read-only device facts to record first:**
  - `adb shell getprop ro.build.version.sdk`
  - `adb shell getprop build.version.extensions.s` (≥7 means HttpEngine)
  - `adb shell dumpsys package com.google.android.gms | grep versionName` (custom ROM: Play Services may be absent)
  - `HttpEngine.getVersionString()`, logged by the app

---

## Risks and open questions

1. **Bridge maturity / idle wakeups (largest).** `CronetInterceptor` polls every 500 ms for the whole process lifetime, and `close()` leaks executors (#54, filed 2026-10-15 on our exact stack, no reply). Fifteen issues are open, including #44 (307/308 with a body), #15 (FD leaks), #12 (upload ISE) and #28 (ANR on init); #19 asks to drop Guava. The bridge is 0.1.x. NET must measure the idle cost; the fallback options are close-on-background or a vendored patch.
2. **Licensee build break.** `org.chromium.net:cronet` (pulled by the bridge) and `cronet-bundled` declare their license only as the Chromium LICENSE URL, which Licensee does not map. Generated apps with `licenses` + cronet fail until `allowUrl("https://chromium.googlesource.com/chromium/src/+/refs/heads/main/LICENSE")` is added under `<opt:licenses&cronet>` [I from POM + Licensee source]. Guava's transitive annotation jars and brotli's MIT URL also need a licensee run; Brotli touches the **default** template.
3. **R8 / reflection.** Providers are loaded by class name (`org.chromium.net.impl.HttpEngineNativeProvider`, `com.google.android.gms.net.PlayServicesCronetProvider`, `NativeCronetProvider`, `JavaCronetProvider`). If the AARs' consumer rules do not keep them, release builds silently drop a provider and fall back [U]. Log `getAllProviders` names in release and check them.
4. **Play Services provider vs API 500.** `play-services-cronet` 18.1.1 was built against `cronet-api` 141. With `cronet` 500.1.0 on the classpath, check that `PlayServicesCronetProvider.isEnabled()` is true and that an engine builds [U]. ("A provider being out-of-date (older than the API)… could be disabled.")
5. **Sign-out parity.** The OkHttp cache is bypassed and Cronet's cache cannot be evicted through the API, so keep Cronet's HTTP cache off (`DISK_NO_HTTP`). Cookies are already refused by Cronet [V]; confirm it on the phone (`httpbin.org/cookies/set` → `/cookies`) for the HttpEngine and GMS providers [U].
6. **Pin parity gaps.**
   - Three OkHttp pin forms have no Cronet translation: `*.` single-label wildcards, `sha1/` pins and IP-literal hosts.
   - Cronet bypasses pins for user CAs by default.
   - Cronet pins expire.
   - The security audit should have a wrong-pin test on each path. An NSC `<pin-set>` is a possible single source; whether it applies to HttpEngine/GMS is [U].
7. **HTTP/3 may not show up in the numbers.** The first connection to a host is TCP until Alt-Svc is known. Most generated apps' backends (like the user's Vercel site) have no h3. The proof should report results per protocol, with `addQuicHint` as an explicit config (`NetworkConfig.quicHosts`, default empty).
8. **Size and dependency weight on the Cronet path:** `cronet` Java (655 KB AAR), Guava 31.1-android (2.8 MB jar before R8) plus `failureaccess`/`listenablefuture`, and the play-services base/basement/tasks AARs. Measure with R8. `cronet-bundled` adds about 14 MB (all ABIs) and stays a documented option only.
9. **Connect timeout and cancellation latency.** The 15 s connect timeout is not applied on Cronet, and cancellation lags by up to 500 ms. Document both; the overall request timeout still bounds calls.
10. **Selection details to decide:** (a) "API 34+" → "HttpEngine available" (S-ext ≥ 7); (b) the old-HttpEngine (< 133.0.6876.3) guard; (c) where `cronet-bundled` ranks.
11. **zstd latent bug** (section 6). Low probability, but the test via `httpbin.org/headers` is cheap.
12. **Unverified Cronet 500.x behaviour:** the provider scoring and wrapper code were read on Chromium **main**, not on the 500.1.0 tag (no release notes found). Confirm in NET by logging the provider list, the chosen provider and `getVersionString()` on the phone.

---

## Recommended implementation outline for NET

Start after WP06 and WP02-M2 merge (item 39). Keep everything in `:core:network`; no new module is needed.

### Dependencies (`gradle/libs.versions.toml`)
- **Always** (with `network`): `okhttp-brotli = { group = "com.squareup.okhttp3", name = "okhttp-brotli", version.ref = "okhttp" }`, used as `implementation(libs.okhttp.brotli)` in `:core:network`.
- **Under `# <opt:cronet>`:**
  - versions: `cronetOkhttp = "0.1.2"`, `cronet = "500.1.0"`, `playServicesCronet = "18.1.1"`.
  - `cronet-okhttp = { group = "com.google.net.cronet", name = "cronet-okhttp", version.ref = "cronetOkhttp" }`
  - `cronet = { group = "org.chromium.net", name = "cronet", version.ref = "cronet" }` (declared directly, as the deprecation notes advise)
  - `play-services-cronet = { group = "com.google.android.gms", name = "play-services-cronet", version.ref = "playServicesCronet" }`
  - Docs only: `org.chromium.net:cronet-bundled:500.1.0`.
- `kotlinx-coroutines-play-services` (already in the catalog) for `installProvider(ctx).await()`.

### Classes (`core/network/.../transport/`, owned by the feature)
- `CronetProviderChoice`: a pure policy. Input: a list of `(name, enabled, version)` plus flags (`allowBundled`, `minPreferredHttpEngineVersion`). Output: the provider name or `null`. It never returns `Fallback-Cronet-Provider`. This is the unit-tested fallback logic item 39 asks for.
- `CronetEngineFactory`: `provider.createBuilder()` with, always set explicitly (no reliance on defaults):
  - `enableHttp2(true)`, `enableQuic(true)`, `enableBrotli(true)`;
  - `setStoragePath(noBackupFilesDir/cronet)` + `enableHttpCache(HTTP_CACHE_DISK_NO_HTTP, small)`;
  - pins from `PinTranslator`, plus `enablePublicKeyPinningBypassForLocalTrustAnchors(false)` when pins exist;
  - migration on for the default network and for path degradation, non-default network off;
  - `addQuicHint` for `NetworkConfig.quicHosts`;
  - `setUserAgent(config.userAgent)` when set.
- `PinTranslator`: `Map<String, List<String>>` → `(host, Set<byte[]>, includeSubdomains)`. It rejects `sha1/`, `*.` and IP hosts, and sets a far-future expiry.
- `CronetTransport` (`@Singleton`): holds the engine and the one `CronetInterceptor` (`setUploadDataProviderExecutorSize(1)`).
  - `prewarm()` runs on `AppDispatchers.io` at start-up: GMS `installProvider` only if HttpEngine is not chosen, then build.
  - `engineOrNull(timeout)` does a bounded wait. Any `Throwable` at init → permanently `null` → OkHttp path. It exposes `providerName` for logs and devtools.
- `CronetTransportInterceptor` (an application interceptor, added **last**):
  - `Upgrade` header present → `chain.proceed`;
  - engine `null` → `chain.proceed`;
  - otherwise → `cronetInterceptor.intercept(chain)`.
  - Its delegate is an injectable `Interceptor` so tests can fake it.

### DI wiring (`HttpClientModule`)
- `OkHttpClient.Builder().cache(cache).certificatePinner(...)` (kept for the OkHttp path), then `.addInterceptor(BrotliInterceptor)`, then `// <opt:cronet>` `.addInterceptor(cronetTransportInterceptor)` `// </opt:cronet>`, then `.build()`. Every Ktor client, the derived timeout clients and Coil inherit it, all sharing one interceptor and therefore one poller.
- `@RealtimeClient` (`<opt:websocket&cronet>`): additionally set `engine { webSocketFactory = okHttp.newBuilder().apply { interceptors().removeAll { it is CronetTransportInterceptor } }.build() }`, or provide a qualified base client. The Upgrade check already covers this; this second guard is optional.
- `BaseAppApplication` (`<opt:cronet>`): `cronetTransport.prewarm()` launched on the application scope.
- `NetworkConfig`: add `quicHosts: List<String> = emptyList()`.

### Generator feature (WP02 format)
- `templates/android/features/cronet.toml`:
  - `key = "cronet"`. The marker grammar allows `network-cronet` (`[a-z][a-z0-9_-]*`, `markers.py:87`) [V], but every existing key is one lowercase word (`websocket`, `offlinequeue`, `appstatus`).
  - `title = "HTTP/3 transport (Cronet)"`, `group = "Networking and data"`, `default = false`, `requires = ["network"]`.
  - `owns = ["core/network/src/main/kotlin/{pkg_path}/core/network/transport", "core/network/src/test/kotlin/{pkg_path}/core/network/transport"]`.
- Markers (grammar `EXPR := TERM ('|' TERM)*`, `TERM := ATOM ('&' ATOM)*`, `ATOM := '!'? key` [V]):
  - `# <opt:cronet>` in the catalog;
  - `// <opt:cronet>` in `core/network/build.gradle.kts`, and around the imports and `.addInterceptor` in `HttpClientModule`;
  - `<opt:licenses&cronet>` around the Licensee `allowUrl` in `app/build.gradle.kts`;
  - `<opt:websocket&cronet>` for the realtime guard;
  - `<opt:cronet>` in `BaseAppApplication` for prewarm.
- Brotli carries no marker; it is part of `network`.
- R8: add keep rules for the four provider classes only if the release check (risk 3) shows the AARs lack them.

### Tests
- **JVM unit (`:core:network`):**
  - `CronetProviderChoiceTest`: HttpEngine enabled; HttpEngine absent with GMS enabled; GMS install failed; only the Java fallback present → `null`; old HttpEngine vs GMS; bundled allowed or not.
  - `PinTranslatorTest`: sha256 decode, `**` mapping, rejection of `*.`/`sha1`/IP.
  - `CronetTransportInterceptorTest` with a fake `Chain`: Upgrade → proceed; no engine → proceed; engine → delegate.
  - Client-shape test: `BrotliInterceptor` is in `interceptors` (not `networkInterceptors`) and the transport interceptor is last.
  - Brotli decode test with a fixture `br` body (mockwebserver3 5.5.0 would need a catalog entry; otherwise a fake chain).
- **Robolectric:** with no HttpEngine and no GMS, the real `CronetProvider` listing gives the plain-OkHttp path.
- **Generator (Python):**
  - feature on/off: catalog entries, `:core:network` deps, imports, balanced markers;
  - `allowUrl` present iff `licenses&cronet`; `okhttp-brotli` present iff `network`;
  - no `ContentEncoding` and no `addNetworkInterceptor(BrotliInterceptor)` anywhere; no `Accept-Encoding` set at call sites;
  - both variants build through `locked gradle`, with build guards and licensee green.
- **Phone (release, the Redmi):**
  - chosen provider and version logged;
  - `response.version == QUIC` against jsDelivr;
  - WebSocket still connects with cronet on;
  - wrong pin fails on OkHttp, HttpEngine and GMS;
  - cookie and Accept-Encoding echo checks via httpbin;
  - section 7 measurements plus the idle-wakeup trace and the APK size deltas (Brotli, Cronet).
- **Docs (WP12):** when to turn cronet on; bypassed features (table in section 5); timeouts; the cache and cookie policy; pin rules; the bundled option and its size.

---

## Sources
- google/cronet-transport-for-okhttp: README; `CronetInterceptor.java`, `RequestResponseConverterBasedBuilder.java`, `ResponseConverter.java`, `RequestResponseConverter.java` (master); tags; commits; issues #20, #28, #52, #54 — https://github.com/google/cronet-transport-for-okhttp
- Google Maven metadata, POMs and HEAD sizes: `https://dl.google.com/android/maven2/{com/google/net/cronet/cronet-okhttp, org/chromium/net/{cronet,cronet-api,cronet-common,cronet-fallback,cronet-shared,cronet-bundled,cronet-embedded,httpengine-native-provider}, com/google/android/gms/play-services-cronet}/…`
- Maven Central metadata and POMs: `https://repo1.maven.org/maven2/{com/squareup/okhttp3/okhttp(-brotli,-zstd), org/brotli/dec, io/ktor/ktor-client-okhttp(-jvm), io/coil-kt/coil3/coil-network-okhttp}/…`
- Chromium main: `components/cronet/android/api/src/org/chromium/net/{CronetEngine,CronetProvider}.java`; `components/cronet/android/java/src/org/chromium/net/impl/{HttpEngineNativeProvider,AndroidHttpEngineBuilderWrapper,CronetEngineBuilderImpl}.java`; `components/cronet/cronet_context.cc`; `net/url_request/url_request_http_job.cc`; `net/android/java/src/org/chromium/net/{X509Util,AndroidNetworkLibrary}.java` — https://chromium.googlesource.com/chromium/src/+/main/
- Cronet reference: https://developer.android.com/develop/connectivity/cronet/reference/org/chromium/net/CronetEngine.Builder, …/CronetProvider, …/impl/HttpEngineNativeProvider, …/ConnectionMigrationOptions.Builder; guide https://developer.android.com/develop/connectivity/cronet/start
- AOSP API surface for `android.net.http` (HttpEngine.Builder signatures): `packages/modules/Connectivity/framework/api/current.txt` — https://android.googlesource.com/platform/packages/modules/Connectivity/
- Play Services: https://developers.google.com/android/reference/com/google/android/gms/net/CronetProviderInstaller
- OkHttp 5.5.0 (tag `parent-5.5.0`): `okhttp-brotli/.../BrotliInterceptor.kt`, `Brotli.kt`; `okhttp/.../CompressionInterceptor.kt`, `internal/http/BridgeInterceptor.kt`, `internal/ws/RealWebSocket.kt`, `internal/connection/RealCall.kt`, `Protocol.kt`, `CertificatePinner.kt` — https://github.com/square/okhttp
- Ktor 3.6.0: `ktor-client-okhttp/jvm/src/io/ktor/client/engine/okhttp/{OkHttpEngine,OkUtils,OkHttpConfig}.kt` — https://github.com/ktorio/ktor; ContentEncoding docs https://ktor.io/docs/client-content-encoding.html
- Licensee: `build-logic/.../defaultFallbackUrls.kt`, `src/main/kotlin/app/cash/licensee/licenses.kt` — https://github.com/cashapp/licensee
- Network rate limit: https://www.esper.io/blog/android-13-network-throttling-setting
- Template: `git show {overhaul/multiplatform-v2,wp/WP06,wp/WP02,wp/WP04-perf}:templates/android/…` and `wp/WP02:generator/genkit/core/markers.py`
