# Audit: template build system (android-base `template/`)

Area: `build-logic/**`, `settings.gradle.kts`, root and module `build.gradle.kts`, `gradle/libs.versions.toml`,
`gradle.properties`, R8 rules, manifests, `config/**`, lint config, `:benchmark`, and the `android` job in
`.github/workflows/ci.yml`. Repo state: branch `overhaul/multiplatform-v2`, HEAD `99c4edf`.

## How this was checked

Everything here was read statically. Gradle was not run. Evidence came from:

- **The template sources.** Every line reference below points to them.
- **AGP 9.4.1 sources** in the local Gradle cache (`com/android/build/gradle/options/BooleanOption.kt`, the `gradle-api` DSL), to confirm which `gradle.properties` flags are deprecated.
- **The Compose compiler Gradle plugin 2.4.20** (`ComposeFeatureFlags.kt`) and the compiler plugin's `KnownStableConstructs`.
- **The KSP 2.3.12 Gradle plugin** (its message strings) and the bundled R8 rules in `kotlinx-serialization-core-jvm-1.11.0.jar`.
- **POM files** for biometric, lottie, glance, splashscreen, browser and Kyant backdrop.
- **The two apps generated from this template, as built:**
  - PeriFlow: `app/build/outputs/apk/prod/release/app-prod-universal-release.apk` and `dev/benchmarkRelease` with `mapping/devBenchmarkRelease/mapping.txt`.
  - VitFleet: `app/build/outputs/apk/prod/release/app-prod-release.apk` and `dev/benchmarkRelease` with its mapping.
  - Both apps' merged release manifests.

  Size figures come from `apkanalyzer dex packages --defined-only --proguard-mappings` on those release APKs, and `aapt2 dump configurations/resources`.
- **The git history of both apps**, for the fixes they made by hand.

Scratch outputs are under `/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/template-build/`
(`vit-dex-packages.txt`, `peri-dex-packages.txt`, `vit-res.txt`).

---

## 1. Executive summary

The build logic is better than average. It has:

- real convention plugins
- the configuration cache, build cache and non-transitive R
- one catalogue for every version
- module-tier enforcement
- unit tests disabled outside `devDebug`
- R8 full mode with resource shrinking

The Compose compiler set-up is correct for Kotlin 2.4.20. Strong skipping, IntrinsicRemember,
OptimizeNonSkippingGroups and PausableComposition are all on by default (`ComposeFeatureFlags.kt:98,119,142,163`),
so nothing needs adding.

The problems fall into four groups.

1. **Wiring that is broken or misleading.**
   - The baseline-profile feature cannot generate a profile for a flavoured app, and would not ship one to `prod`/`playstore` even if it did. PeriFlow's own commit says *"The benchmark module never ran"*.
   - The dependency-analysis setup is configured to **fail**, while the generated README says it "prints rather than fails". No CI ever runs it.
   - Crashlytics uploads a mapping file on every release build.

2. **APK weight that nobody asked for.**
   - No `localeFilters`: 83 unused locales make up **35 % of all resource entries** in VitFleet's release APK.
   - ABI splits produce 5 APKs to save about 50 KB, because native code is **69 KB total** across all ABIs.
   - `push` forces all of Firebase, including Analytics. Play-services-measurement alone is about **666 KB of dex** (≈8 % of VitFleet's dex), and it brings `AD_ID` and AdServices permissions.
   - Lottie and biometric each pull in **appcompat** (≈190 KB of dex in both apps). Biometric also pulls fragment 1.2.5, from 2020.

3. **Build-time waste, from conventions that assume every module needs everything.**
   - Hilt-testing and KSP-for-tests run in every module, though there are zero Hilt tests.
   - androidTest dependencies are added everywhere, though there are zero androidTest sources.
   - `includeAndroidResources` is on for unit tests in every module, though only one module uses Robolectric.
   - Coil, `:core:model` and `lifecycle-runtime-compose` are injected into every feature that never imports them.
   - Guard tasks with no outputs re-run on every build.
   - `:core:network` depends on the Compose `:core:devtools`. That chains every `:data:*` module behind the 10 kLOC design system on the critical path.

4. **AGP 9 hygiene.** Five `gradle.properties` flags are deprecated, redundant or opt-outs that AGP 10 will enforce. One more line (`android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE`) exists only to hide the warnings about the others. On top of that: an obsolete `ksp.useKSP2`, a `kotlin-android` alias that must never be applied under built-in Kotlin, empty `-ktx` shells, and Room configured through KSP arguments with an absolute path, which breaks build-cache relocation.

For the stated goals (smooth and high FPS, optional liquid glass, lighter apps), the build-level levers are:

1. Make baseline profiles actually generate, merge into main, and cover the glass and navigation paths. Kyant backdrop ships **no** baseline profile of its own.
2. Add the `enableOnBackInvokedCallback` manifest flag VitFleet added for predictive back on Android 13–15.
3. Add `localeFilters`.
4. Split Firebase.
5. Drop ABI splits.
6. Add `liquidglass` as an opt-in feature. Kyant costs only about **19 KB of dex**, as measured in PeriFlow.

---

## 2. Findings

Severity: **H** high, **M** medium, **L** low.

### A. Broken or misleading wiring

#### A1 [H] Baseline profiles never generate for the flavoured app, and would not reach `prod` if they did

- **Where:**
  - `template/benchmark/build.gradle.kts:14-18`: `defaultConfig` has no `missingDimensionStrategy`, and `targetProjectPath = ":app"` is on line 25.
  - `template/app/build.gradle.kts:105-108`: `baselineProfile(project(":benchmark"))`, with no `baselineProfile { mergeIntoMain = true }`.
  - `template/benchmark/src/main/kotlin/com/base/app/benchmark/BaselineProfileGenerator.kt:38`: `PACKAGE_NAME = "com.base.app.dev"`.
- **Evidence:**
  - `:app` has the dimension `environment` with 4 flavours (`Flavors.kt:9-23`). `:benchmark` declares none.
  - PeriFlow commit `11e50cd` says: *"The benchmark module never ran: with no flavour chosen the app's four were ambiguous. It now measures the dev flavour … and the app merges the result into main so every flavour ships it."* That commit added `missingDimensionStrategy("environment", "dev")` (PeriFlow `benchmark/build.gradle.kts:18-20`) and `baselineProfile { mergeIntoMain = true }` (PeriFlow `app/build.gradle.kts:135-137`).
  - No `baseline-prof.txt` is committed in the template's `app/src`.
- **Root cause:**
  - A `com.android.test` module that targets a flavoured app must pick a flavour.
  - The baselineprofile consumer writes per-variant profiles by default (`src/devRelease/generated/...`). A profile generated against `dev` therefore never reaches `prodRelease`/`playstoreRelease`.
- **Impact:**
  - The feature promises *"Typically a 20-30% cut in cold start"* (`generator/genkit/spec.py:302`). It delivers nothing.
  - The JIT-warm-up jank on first tab switches and push/pop transitions, which this overhaul wants removed, stays in every generated app.
- **Fix:**

```kotlin
// benchmark/build.gradle.kts
defaultConfig {
    minSdk = 28
    missingDimensionStrategy(AppFlavor.DIMENSION, "dev")   // or configureFlavors(this) to mirror
}
// app/build.gradle.kts (inside <opt:baselineprofile>)
baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    automaticGenerationDuringBuild = false
}
```

  - Split the generator into two rules, so the startup profile stays narrow for DEX layout:
    - a startup journey with `includeInStartupProfile = true`
    - a "journeys" rule with `includeInStartupProfile = false` that walks tabs, push/pop and scrolling over glass surfaces
  - PeriFlow puts the whole walk in the startup profile. Its `baseline-prof.txt` and `startup-prof.txt` are identical, 36,576 lines each.
  - Prefer a Gradle Managed Device (`managedDevices += "pixel6Api34"`, `useConnectedDevices = false`) to `useConnectedDevices = true` (`benchmark/build.gradle.kts:34-36`), so the profile is reproducible.

#### A2 [M] Dependency analysis: the config fails the build, the docs say it only prints, and nothing runs it

- **Where:**
  - `template/build.gradle.kts:97-108`: `onAny { severity("fail") }`.
  - `generator/genkit/readme.py` (`_DEPS_NOTE`): *"It prints rather than fails … Making it a gate means … then setting `severity("fail")`"*.
  - `generator/genkit/spec.py:400`: *"Printed on every pull request; a report rather than a gate"*. Generated projects have no CI at all, because the `ci` feature is in `LEGACY_FEATURES` as `None`.
- **Further mismatch:** the comment at `build.gradle.kts:101-103` mentions *"the design system exposes Compose as `api` on purpose"*, but no rule exempts it. `onIncorrectConfiguration` falls under `onAny → fail`.
- **Evidence the gate would fail today:** see C6 and C7. They list a dead `:core:model` wired as `api`, unused Coil and lifecycle dependencies in every feature, unused serialization plugins, and an unused Hilt plugin in `:core:ui`.
- **CI:** `.github/workflows/ci.yml:138-141` runs `check …`. `buildHealth` is not hooked into `check`.
- **Fix:**
  - Either use `severity("warn")` to match the docs, or clean the conventions (C6/C7) first and keep `fail`.
  - Add `buildHealth` to the CI `all` matrix entry.
  - Drop the "printed on every pull request" claim.
  - Use the settings plugin `com.autonomousapps.build-health` instead of `subprojects { apply(...) }` (`build.gradle.kts:93-95`).

#### A3 [M] Crashlytics uploads mapping files on every release build, including CI

- **Where:** `template/build-logic/convention/src/main/kotlin/AndroidFirebaseConventionPlugin.kt:8-21`. It applies `com.google.firebase.crashlytics` with no upload control.
- **Evidence:** VitFleet fixed this in commit `2ef1f33` (`AndroidFirebaseConventionPlugin.kt:16-25`). Its comment reads: *"the plugin would otherwise send each one's mapping file to Firebase: a network call from every local and CI build, and one that cannot succeed with the placeholder google-services.json"*. The CI `everything`/`all` matrix entries run `assembleProdRelease` with Firebase on (`ci.yml:138`).
- **Fix:** port VitFleet's gate as-is.

```kotlin
val uploadMappings = providers.gradleProperty("crashlyticsUpload").orNull.toBoolean()
extensions.configure<ApplicationExtension> {
    buildTypes.configureEach { configure<CrashlyticsExtension> { mappingFileUploadEnabled = uploadMappings } }
}
```

  This needs `compileOnly(libs.firebase.crashlytics.gradlePlugin)`, which is already declared in `build-logic/convention/build.gradle.kts:47`.

#### A4 [L] `dist` tasks share one output directory, and the docs claim the bundle is included

- **Where:**
  - `ReleaseArtifacts.kt:48,57`: both `dist<V>Apks` and `dist<V>Bundle` set `outputDirectory = outputs/dist/<variant>`.
  - `ReleaseArtifacts.kt:121-124`: the APK task runs `deleteRecursively()` on that directory.
  - `ReleaseArtifacts.kt:186-189`: the bundle task appends to `checksums.sha256`.
  - `ReleaseArtifacts.kt:60-65`: `dist<V>` depends only on the APK task.
- **Docs:** `template/docs/ARCHITECTURE.md:637` says *"`./gradlew :app:distProdRelease` copies every APK and the bundle"*.
- **Impact:**
  - Running the bundle task and then the APK task deletes the `.aab`.
  - Overlapping outputs disable up-to-date checks.
  - Repeated bundle runs pile up checksum lines.
- **Fix:** delete this ~190-line file together with ABI splits (C2). Play takes an AAB, and naming files is a CI job. If kept, give each task its own sub-directory and fix the doc.

---

### B. AGP 9.4, Kotlin 2.4 and KSP 2.3 hygiene

#### B1 [M] Five deprecated or redundant `gradle.properties` flags, plus one line that hides the warnings about them

`template/gradle.properties`:

| Line | Flag | Status in AGP 9.4.1 source (`BooleanOption.kt`) |
|---|---|---|
| 25 | `android.useAndroidX=true` | `USE_ANDROID_X … true, ApiStage.Deprecated(removalTarget = VERSION_10_0)` (l.555): default already true, the flag is deprecated |
| 28 | `android.nonTransitiveRClass=true` | `NON_TRANSITIVE_R_CLASS … true, ApiStage.Stable` (l.85): this is the default |
| 30 | `android.nonFinalResIds=true` | `USE_NON_FINAL_RES_IDS … true, ApiStage.Deprecated(VERSION_10_0)` (l.541) |
| 31 | `android.enableResourceOptimizations=true` | `ENABLE_RESOURCE_OPTIMIZATIONS … true, FeatureStage.SoftlyEnforced(VERSION_9_0)` (l.418) |
| 32 | `android.disallowKotlinSourceSets=false` | Experimental opt-out with `FutureStage(VERSION_10_0, …, Enforced)` (l.376-382). It silently ignores `kotlin.sourceSets` and becomes impossible to set in AGP 10 |
| 33 | `android.sync.suppressAgpWarnings=UNSUPPORTED_PROJECT_OPTION_USE` | Hides the deprecation warnings for exactly the lines above |
| 23 | `ksp.useKSP2=true` | KSP 2.3.12's own message: *"KSP1 is no longer available … The ksp.useKSP2 property will be removed in the future."* |
| 20, 22 | `kotlin.incremental=true`, `ksp.incremental=true` | Both are the defaults |

- KSP 2.3.12 already integrates with AGP built-in Kotlin. Its classes reference `isAgpBuiltInKotlinUsed` and `addGeneratedSourceDirectory`. So the `disallowKotlinSourceSets` opt-out is probably not needed. Remove it and let the build name the offender, if there is one.
- AGENTS.md says to read `--warning-mode all`, and line 33 defeats that.
- **Fix:** delete lines 20, 22, 23, 25-33. Keep the configuration cache, build cache and parallel settings.
- PeriFlow and VitFleet carry the same lines, so fix them too.

#### B2 [L] A `kotlin-android` plugin alias that must not be used under AGP 9 built-in Kotlin

- `gradle/libs.versions.toml:196`: `kotlin-android = { id = "org.jetbrains.kotlin.android", … }`. It is unused anywhere in the template.
- The convention plugins rely on AGP's built-in Kotlin (`KotlinAndroidProjectExtension` with no kotlin-android plugin applied, `AndroidCommon.kt:67-72`). Applying the alias would break the build.
- **Fix:** delete it, so nobody "fixes" a module by adding it.

#### B3 [M] Room is configured through raw KSP arguments with an absolute path

- **Where:** `AndroidRoomConventionPlugin.kt:14-17`: `arg("room.schemaLocation", "${projectDir}/schemas")`, `arg("room.generateKotlin", "true")`.
- **Root cause:**
  - The absolute path becomes a KSP task input string, so cache keys differ between checkouts (CI vs laptop, or two worktrees). That breaks build-cache relocation.
  - The schema JSON is not a declared output, so it is never restored from cache.
  - The Room Gradle plugin (`androidx.room`) exists to fix both. `room.generateKotlin` is the default for KSP in current Room; confirm when switching.
- **Fix:**

```toml
[plugins] androidx-room = { id = "androidx.room", version.ref = "room" }
```
```kotlin
pluginManager.apply("androidx.room")
extensions.configure<RoomExtension> { schemaDirectory("$projectDir/schemas") }
```

#### B4 [L] Empty `-ktx` shells declared as dependencies

- `AndroidRoomConventionPlugin.kt:21`: `androidx-room-ktx`. The `room-ktx-2.8.5.aar` `classes.jar` holds **one file**, `META-INF/androidx.room_room-ktx.version`.
- `core/common/build.gradle.kts:17`: `androidx-lifecycle-viewmodel-ktx`. The 2.11.0 `classes.jar` likewise holds only the `.version` file.
- **Fix:** remove both. `androidx-lifecycle-runtime-ktx` (`libs.versions.toml:77`) is already unused.

#### B5 [L] A wrong comment, and duplicated release configuration

- `app/build.gradle.kts:36-37` claims *"`getDefaultProguardFile` is only reachable from a build script"*. In fact `CommonExtension.getDefaultProguardFile(name)` exists (AGP API `CommonExtension.kt:533`).
- `catalog/build.gradle.kts:19-26` duplicates the same block. Move it into `AndroidApplicationConventionPlugin`.
- `catalog/build.gradle.kts:14-15` sets `versionCode = 1` and `versionName = "1.0.0"`. The convention already sets these from `AppConfig`.

#### B6 [L] Legacy settings that do nothing

- `AndroidCommon.kt:22`: `vectorDrawables.useSupportLibrary = true`. It only matters for AppCompat `srcCompat`, and minSdk is at least 24 with no AppCompat views.
- `AndroidLibraryConventionPlugin.kt:22-24`: `isMinifyEnabled = false` for library release. That is already the default, and the comment's rationale is wrong.
- `build.gradle.kts:111-113`: a hand-written root `clean`.
- `ModuleGraph.kt:56-79`: `ModuleTier.APP` is unreachable, because `of()` never returns it. The `APP ->` branches are dead.

---

### C. APK weight and bloat

#### C1 [H] No `localeFilters`: 83 unused library locales ship in every APK

- **Evidence:**
  - `aapt2 dump configurations` reports **99** locale configurations in VitFleet's `app-prod-release.apk` and 98 in PeriFlow's.
  - Counting `aapt2 dump resources` for VitFleet: 4,153 of 11,715 resource values (**35 %**) belong to 83 locales the app does not ship. VitFleet ships `en` + `hi`. The list includes `en-rXC`, a pseudo-locale.
  - `resources.arsc` is stored uncompressed: 1.86 MB in VitFleet, 1.04 MB in PeriFlow.
- **Where:** nothing in `app/build.gradle.kts` limits locales. `generateLocaleConfig = true` (lines 29-31) only writes the picker list.
- **Fix:** the generator already knows `spec.languages`. Emit:

```kotlin
androidResources {
    localeFilters += listOf("en" /* <generated:locale-filters> e.g. "es", "hi", "pt-rBR" */)
}
```

  `localeFilters` is the AGP 9 replacement for `resourceConfigurations`. The AGP API deprecates the latter with *"use androidResources.localeFilters"* (`BaseFlavor.kt:164`). `GenerateLocaleConfigTask` also reads `localeFilters` first (l.185).

#### C2 [M] ABI splits produce 5 APKs per release to save about 50 KB

- **Where:** `Flavors.kt:59-74` (`configureAbiSplits`, enabled whenever a task name contains "Release"), and `ReleaseArtifacts.kt`.
- **Evidence:**
  - All native code in either app is `libandroidx.graphics.path.so` plus `libdatastore_shared_counter.so`, **69 KB across 4 ABIs**.
  - PeriFlow's split APKs are 8.81 MB against the 8.91 MB universal.
  - The task-name heuristic makes outputs depend on how Gradle was invoked. PeriFlow's `dev/benchmarkRelease` has 5 split APKs, VitFleet's has one.
  - 32-bit `x86` is still included.
- **Fix:** delete `configureAbiSplits`, the `abiSplits` property, and `ReleaseArtifacts.kt` (see A4). Play delivers per-ABI from the AAB.

#### C3 [H] `push` forces all of Firebase, including Analytics

- **Where:**
  - `generator/genkit/spec.py:189`: `push` requires `firebase`.
  - `spec.py:169`: `firebase` requires `analytics` and `flags`.
  - `AndroidFirebaseConventionPlugin.kt:14-19` adds analytics, crashlytics and messaging.
  - `core/analytics/build.gradle.kts:13-17` and `core/flags/build.gradle.kts:13-16` add analytics, crashlytics and remote config.
  - Running `resolve_features({"push"})` returns `['analytics', 'firebase', 'flags', 'push']`.
- **Evidence, measured dex in VitFleet's R8 release:**

  | Component | Package | Dex |
  |---|---|---|
  | Analytics | `com.google.android.gms.measurement` + `gms.internal.measurement` | 270 KB + 396 KB = **666 KB** |
  | Crashlytics | `firebase.crashlytics` | 137 KB |
  | Crashlytics sessions | `firebase.sessions` | 33 KB |
  | Messaging | `firebase.messaging` | 36 KB |
  | Remote Config | `firebase.remoteconfig` | 26 KB |

  FCM needs only `firebase-messaging` and the google-services plugin.
- **Fix:** split `firebase` into features with their own dependencies:

  | Feature | Contents |
  |---|---|
  | `firebase-core` | plugin + json, implied only |
  | `push` | messaging |
  | `crashlytics` | |
  | `firebase-analytics` | |
  | `remote-config` | |

  This reverses the `LEGACY_FEATURES` merge at `spec.py:510-519` (`"analytics-firebase"`, `"crashlytics"` and `"flags-remote"` → `"firebase"`). Move `AppMessagingService` from `:app/push` into `:core:notification`. Then `:app` needs no Firebase dependencies (`AndroidFirebaseConventionPlugin.kt:14-19`), because app sources import only `FirebaseMessagingService`/`RemoteMessage`, in `app/push/AppMessagingService.kt`.

#### C4 [M] Firebase Analytics merges `AD_ID` and AdServices permissions

- **Evidence:** the merged release manifests of both apps contain:
  - `com.google.android.gms.permission.AD_ID`
  - `ACCESS_ADSERVICES_AD_ID`
  - `ACCESS_ADSERVICES_ATTRIBUTION`
  - PeriFlow also has `ACCESS_ADSERVICES_TOPICS` and `READ_GSERVICES`

  Play then requires an advertising-ID declaration. The template manifest (`app/src/main/AndroidManifest.xml`) removes none of them.
- **Fix:** inside the `firebase-analytics` feature, unless the app uses ads:

```xml
<uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove" />
<uses-permission android:name="android.permission.ACCESS_ADSERVICES_AD_ID" tools:node="remove" />
<uses-permission android:name="android.permission.ACCESS_ADSERVICES_ATTRIBUTION" tools:node="remove" />
<meta-data android:name="google_analytics_adid_collection_enabled" android:value="false" />
```

#### C5 [M] Lottie and biometric pull appcompat; biometric also pulls fragment 1.2.5 from 2020

- **Evidence:**
  - The `lottie-6.7.1.pom` depends on `appcompat:1.6.1` and okio.
  - The `biometric-1.1.0.pom` depends on `fragment:1.2.5` (compile) and `appcompat:1.2.0` (runtime).
  - `androidx.appcompat` is **190 KB and 191 KB of dex** in PeriFlow and VitFleet. R8 seeds contain 459 kept `androidx.appcompat.widget` members in VitFleet.
  - `LockActivity.kt:19,35` compiles against `androidx.fragment.app.FragmentActivity`, which arrives only transitively, at 1.2.5.
  - VitFleet pinned it: `fragment = "1.9.1"`, with the comment *"biometric alone would pull fragment 1.2.5 from 2020"*, and `core/applock/build.gradle.kts` `api(libs.androidx.fragment)`.
- **Fix:**
  - Declare `androidx.fragment:fragment` explicitly in the applock code. Move applock out of `:app` into `:core:applock` like VitFleet did.
  - Take `lottie` out of the `everything` preset. It exists for one success animation (`spec.py:128-139`) and costs about 145 KB plus appcompat.
  - Offer a Compose-native alternative such as `io.github.alexzhirkevich:compottie`. It is Kotlin Multiplatform, which suits the planned Compose Multiplatform generator. Check its size before adopting it.

#### C6 [M] `:core:model` is dead but wired into every data and feature module

- **Evidence:**
  - `grep -rln "com.base.app.core.model"` matches only `core/model/src/main/.../Paged.kt` and its own test.
  - `AndroidDataConventionPlugin.kt:20` declares `api(project(":core:model"))`.
  - `AndroidFeatureConventionPlugin.kt:18` declares `implementation(project(":core:model"))`.
  - `core/model/build.gradle.kts:3,7` applies the serialization plugin and depends on `kotlinx-serialization-json`, and has no `@Serializable`.
- **Fix:**
  - Either make it the real home for shared domain types (it is JVM-only, which is right for multiplatform), or delete it.
  - Either way, drop it from the conventions and let modules that use it declare it.

#### C7 [M] Conventions add dependencies and plugins that the modules never use

The import scan per module is in the scratch directory.

| Declared | Where | Actual use |
|---|---|---|
| `coil-compose` in every feature | `AndroidFeatureConventionPlugin.kt:28-30` | No feature imports `coil3`. Only `core/ui/AppNetworkImage.kt` does |
| `lifecycle-runtime-compose` in every feature | same file, :25 | No feature imports `androidx.lifecycle.compose` |
| `:core:coroutines` in every feature | same file, :22 | Only `feature/settings` imports it |
| `kotlinx-serialization-json` in every feature | same file, :24 | Features import only `Serializable` and `SerializersModule` (core). Json already arrives `api` from `:core:navigation` (`core/navigation/build.gradle.kts:22`) |
| `:core:datastore` in every data module | `AndroidDataConventionPlugin.kt:22` | `data/feed` and `data/sample` never import it |
| `:core:coroutines` in every data module | same file, :21 | No data module imports it |
| serialization plugin + json | `core/datastore/build.gradle.kts:4,16`, `core/notification/build.gradle.kts:4,17` | No `kotlinx.serialization` import in either module |
| `com.base.app.android.hilt` | `core/ui/build.gradle.kts:4` | No `dagger`, `javax.inject` or Hilt usage in core:ui |
| `androidx-lifecycle-process` | `app/build.gradle.kts:90` | No `ProcessLifecycleOwner` anywhere in the template |
| `coil-compose` | `app/build.gradle.kts:101` | `:app` imports no `coil3`. `coil-network-okhttp` (102) is runtime-only, so make it `runtimeOnly`, or move it to `:core:ui` |
| `espresso-core` | `benchmark/build.gradle.kts:40` | Benchmark sources use only uiautomator |
| `profileinstaller` | `app/build.gradle.kts:106` | Already transitive through Compose. Harmless but redundant |

None of these change the APK size, because R8 removes unreferenced code. They do lengthen compile classpaths, add KSP and plugin work, and would fail A2's gate.

**Fix:** keep conventions to the true minimum (Compose, Hilt, the core UI modules). Let modules declare the rest.

#### C8 [L] The app's R8 rules repeat library rules, and one is broader than needed

- **Where:** `app/proguard-rules.pro`.
  - Lines 24-28 (`@Serializable … Companion/INSTANCE/serializer`) repeat the rules that ship inside `kotlinx-serialization-core-jvm-1.11.0.jar` (`META-INF/com.android.tools/r8/kotlinx-serialization-common.pro` and `…-r8.pro`).
  - Lines 32-35 (enum `values`/`valueOf`) repeat AGP's default `proguard-common.txt`.
  - Lines 49-51 (`conscrypt`, `bouncycastle`, `openjsse`) repeat OkHttp's own rules.
  - Lines 13-18 `-keep` every `AppNavKey` implementation, which also keeps class names unobfuscated. The keys are registered statically, through `subclass(X::class, X.serializer())` in `NavKeySerialization.kt:13-27`. The serial name is a compile-time string, so the comment's "resolved by class name at runtime" does not apply.
- **Evidence:** VitFleet's `seeds.txt` shows 1,593 kept app members. 1,273 of them are `Companion`/`serializer` members.
- **Fix:** reduce the file to `-keepattributes SourceFile,LineNumberTable`, `-renamesourcefileattribute` and the Play `-dontwarn`. Verify with a release build plus a process-death restore test. Any nav-key rule that is genuinely needed belongs in `core/navigation/consumer-rules.pro`, not in `:app`.

#### C9 Estimate: what a generated APK pulls in

These are estimates. Per-library dex sizes were measured in VitFleet and PeriFlow R8 release builds. App-module code is extrapolated.

| Component | Measured dex (R8) | `none` | `lean` | `standard` | `everything` |
|---|---|---|---|---|---|
| Compose ui, foundation, runtime, animation | 1,320–1,360 KB | ✓ | ✓ | ✓ | ✓ |
| Kotlin stdlib | 180–200 KB | ✓ | ✓ | ✓ | ✓ |
| kotlinx.coroutines | 145–165 KB | ✓ | ✓ | ✓ | ✓ |
| kotlinx.serialization | 97–111 KB | ✓ | ✓ | ✓ | ✓ |
| DataStore, plus 2 tiny `.so` | 121 KB | ✓ | ✓ | ✓ | ✓ |
| androidx core, activity, lifecycle, collection, emoji2, nav3 | ≈270 KB | ✓ | ✓ | ✓ | ✓ |
| Ktor + OkHttp + okio | ≈415 KB | | ✓ | ✓ | ✓ |
| Coil | 113–131 KB | | | ✓ | ✓ |
| Room + sqlite | ≈60 KB | | | ✓ | ✓ |
| Firebase, measurement included | ≈1,100 KB | | | | ✓ |
| Glance (+ WorkManager 75–92 KB) | 231 KB | | | | ✓ |
| Lottie + appcompat + fragment | ≈385 KB | | | | ✓ |
| Credentials + gms auth | ≈60 KB | | | | ✓ |
| Kyant backdrop (liquid glass) | **19 KB** | | | | opt-in |
| **Estimated dex** | | ≈2.6 MB | ≈3.1 MB | ≈3.6 MB | ≈6 MB |
| **Estimated universal APK** | | ≈3.3 MB | ≈3.8 MB | ≈4.4 MB | ≈7.5 MB |

For comparison, PeriFlow's dev release is 8.9 MB universal (prod with ads: 12.7 MB). VitFleet's is 11.9 MB.

`localeFilters` (C1) should cut a further 5–15 % from `resources.arsc`. Splitting Firebase (C3) takes about 0.7 MB off any app that only wants push.

---

### D. Build performance and caching

#### D1 [M] Guard tasks declare no outputs, so they are never up to date and re-run on every build

- **Where:**
  - `Compose.kt:100-122`: `VerifyComposeUsageTask` has `@InputFiles`/`@Input` and no output. It hooks `preBuild` (`Compose.kt:97`) in **every** module, application and library alike.
  - `ModuleGraph.kt:81-114`: `VerifyModuleDependenciesTask` has the same shape, also on `preBuild` (`:50-51`).
  - `ComposeStability.kt:116-175`: `CheckComposeStabilityTask` has the same shape.
- **Root cause:** Gradle never treats a task with no declared outputs as up to date, and never caches it. Every `assembleDevDebug` re-reads every Kotlin file of every module through about 2×N task executions.
- **Fix:**

```kotlin
@get:OutputFile abstract val marker: RegularFileProperty   // build/guards/<name>.ok
// …at end of @TaskAction: marker.get().asFile.writeText("ok")
```

  Then add `@CacheableTask`.

#### D2 [M] `isIncludeAndroidResources = true` in every module, though one module uses Robolectric

- **Where:** `AndroidCommon.kt:31-35`. `core/database/build.gradle.kts:15` sets it again locally. Only `core/database` uses Robolectric (`AppDatabaseMigrationTest.kt:14,19`).
- **Impact:** every library's unit-test path gains resource merging and linking, plus a test-config generation step.
- **Fix:** default it to `false` in the convention. Modules that run Robolectric (database, and later navigation or screenshot tests) opt in.

#### D3 [M] Test plumbing installed in modules that have no tests

**Evidence:**
- `grep @HiltAndroidTest|HiltAndroidRule` returns **0**.
- No module has `src/androidTest`.
- Only 12 of about 28 modules have `src/test`: core common, database, designsystem, model, navigation, network; data feed, sample, search; feature profile, sample, search.

**What is added regardless:**
- `AndroidHiltConventionPlugin.kt:16-17`: `testImplementation(hilt-testing)` and `kspTest(hilt-compiler)` in every Hilt module.
- `Compose.kt:59,69-70`: `androidTestImplementation(bom, ui-test-junit4)` and `debugImplementation(ui-test-manifest)` in every Compose module.
- `AndroidLibraryConventionPlugin.kt:37-42` and `AndroidApplicationConventionPlugin.kt:43-46`: junit, turbine, mockk, coroutines-test and androidTest junit/runner everywhere. `:core:testing` also exports these as `api` (`core/testing/build.gradle.kts:14-17`).
- `AndroidCommon.kt:53-56` and `JvmLibraryConventionPlugin.kt:27-29` exist only to stop Gradle 9 failing test tasks that have no tests.

**Fix:** decide from the file system at configuration time. Existence checks are configuration-cache inputs.

```kotlin
val hasUnitTests = file("src/test").exists()
val hasDeviceTests = file("src/androidTest").exists()
extensions.configure<LibraryAndroidComponentsExtension> {
    beforeVariants { v ->
        v.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = hasUnitTests && v.buildType == "debug"
        v.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]?.enable = hasDeviceTests
    }
}
if (hasUnitTests) dependencies { add("testImplementation", project(":core:testing")) }
```

Move Hilt-testing to the modules that actually write Hilt tests.

#### D4 [L–M] Library modules that have no resources still run Android resource processing

- **Evidence:** these modules have no `src/main/res`:
  - core: analytics, coroutines, database, datastore, devtools, flags, navigation, network, testing
  - data: all five modules
- **Fix:** AGP API `LibraryAndroidResources.enable` (`LibraryAndroidResources.kt:25`), set in `AndroidLibraryConventionPlugin`:

```kotlin
extensions.configure<LibraryExtension> { androidResources.enable = file("src/main/res").exists() }
```

#### D5 [M] `:core:network` → `:core:devtools` puts the 10 kLOC design system on every data module's critical path

- **Where:** `core/network/build.gradle.kts:17-21`, inside `<opt:devtools>`, which is on by default.
- **Why it matters:**
  - `:core:devtools` is a Compose module that depends on `:core:designsystem` (`core/devtools/build.gradle.kts:4-7,16`). The design system is 64 files and about 10,030 LOC.
  - Network uses only `DevToolsLog` and `NetworkExchange`, two non-UI files totalling 116 LOC (`NetworkRecording.kt:3-4`, `HttpClientModule.kt:9`).
  - So no `:data:*` module can compile until the design system has compiled.
- **Fix:** move `DevToolsLog` and `NetworkExchange` into a tiny non-UI module, `:core:devtools:log`, or declare a `NetworkRecorder` interface in `:core:network` and bind it from `:app`. Keep the UI in `:core:devtools`.

#### D6 [L–M] API leaks that widen recompilation

- `core/network/build.gradle.kts:25`: `api(libs.ktor.client.core)`. `NetworkClient`'s public API is Ktor-free (`NetworkClient.kt:3-28,66-70`), and no data or feature module imports `io.ktor`. Hilt's aggregating task (on by default) does not need the type on `:app`'s compile classpath.
- `core/network/build.gradle.kts:23`: `api(project(":core:datastore"))`. Data modules already declare it.
- `core/datastore/build.gradle.kts:15`: `api(datastore-preferences)`. This lets `data/profile`, `data/search` and `app` (playstore, widget) hand-roll their own `DataStore<Preferences>`.
- **Fix:** use `implementation`. Expose typed stores rather than raw DataStore.

#### D7 [M] Four flavours where prod and playstore differ only by signing key; the baseline-profile build types bring the total to 15 app variants

- **Where:**
  - `AppConfig.kt:66-73`: *"Identical to [PROD] in every respect except the key it is signed with."* Both have no id suffix.
  - `Flavors.kt:27-33` disables only `playstoreDebug`.
- **Evidence:** with baselineprofile on, every flavour also gets `benchmarkRelease` and `nonMinifiedRelease`. VitFleet's outputs show both for all 4 flavours, which gives 15 variants.
- **Fix:** keep dev, staging and prod. Choose the release signing key with `-PreleaseKey=playstore` in `Signing.kt`. Seven variants become five, and twelve baseline-profile variants become nine.
- **Also adopt:** PeriFlow's per-machine URL overrides through `providers.gradleProperty` (PeriFlow `Flavors.kt:23-25`, `AppConfig` `apiUrlProperty`), with the WebSocket URL derived from the API URL. VitFleet does the same with `-PdevHost` and `-PdevServer` (VitFleet `Flavors.kt:12-29`).

#### D8 [L] The stability check forces Compose reports on every compile of the two largest Compose modules

- **Where:** `ComposeStability.kt:27-30` sets `reportsDestination` unconditionally for `:core:designsystem` and `:core:ui` when `composemetrics` is on.
- **Conflict:** `gradle.properties:35-38` says reports are left off *"because it costs ~15% build time"*.
- **Fix:** set reports only when `checkComposeStability` or `recordComposeStability` is requested, or move them to a CI-only `-PcomposeReports`.

#### D9 [L] Repository content filtering covers plugins only

- `settings.gradle.kts:16-22` filters `google()` by group in `pluginManagement`.
- `settings.gradle.kts:28-33` (`dependencyResolutionManagement`) does not. Every Ktor, Kotlin and Coil artifact is therefore first requested from Google Maven, which slows cold resolution and widens the dependency-confusion surface.
- **Fix:** copy the same `content { includeGroupByRegex(...) }` block.

#### D10 [L] Heap sizes

- `gradle.properties:11,18`: Gradle 6 GB plus Kotlin daemon 3 GB, plus 1.5 GB metaspace. The audit machine has 15 GB.
- **Fix:** start with `-Xmx4g` and `kotlin.daemon.jvmargs=-Xmx2g`, and measure with `--scan`.

#### D11 [L] The Hilt Gradle plugin is applied to every library, but every `@AndroidEntryPoint` is in `:app`

- **Where:** `AndroidHiltConventionPlugin.kt:10`.
- **Evidence:** the `@AndroidEntryPoint` classes are `MainActivity.kt:60`, `LockActivity.kt:34` and `AppMessagingService.kt:18`.
- **Impact:** the bytecode transform registers an ASM pass per library variant.
- **Fix (optional):** libraries need only `hilt-android` plus `ksp(hilt-compiler)`. Measure before changing; Now in Android applies the plugin everywhere.

#### D12 [L] `subprojects {}` and cross-project configuration

- **Where:** `build.gradle.kts:36-42` (Kover) and `:93-95` (DAGP).
- **Impact:** these block Gradle's isolated projects later.
- **Fix:** use the Kover aggregation settings plugin and DAGP's `com.autonomousapps.build-health` settings plugin.

---

### E. Manifests

#### E1 [M] No predictive-back flag, so Android 13–15 devices get no system back animation

- **Where:** `app/src/main/AndroidManifest.xml:7-17`. It has no `android:enableOnBackInvokedCallback="true"`.
- **Evidence:** VitFleet commit `00baeaf`, *"fix(android): smooth push, pop, back gesture and tab switches … predictive back is enabled on Android 13 – 15"*, added it at VitFleet manifest line 17.
- **Why:** with targetSdk 36+ the flag is on by default only on Android 16 devices.
- **Fix:** add it to the template's `<application>`. It belongs with the navigation-transition work.

#### E2 [L] Backup configuration

- `allowBackup="false"` (manifest line 9) makes `backup_rules.xml` a no-op on API 30 and below.
- On API 31 and above, `allowBackup=false` does **not** stop device-to-device transfer. `data_extraction_rules.xml` governs it, and it correctly excludes the session store.
- The comment in `backup_rules.xml:3-7` ("Auto Backup is disabled") is therefore half true. Reword it. The rules themselves are correct.

#### E3 [L] `ACCESS_NETWORK_STATE` is in `:core:common`, so apps without networking still declare it

- **Where:** `core/common/src/main/AndroidManifest.xml:10`. `NetworkMonitor` lives in core:common, which every app has, including the `none` preset.
- **Fix:** move `NetworkMonitor` and the permission to `:core:network`, or put them behind the network marker.

#### E4 [L] `/META-INF/*.version` is stripped from debug builds too

- **Where:** `AndroidCommon.kt:39`.
- **Why it matters:** Android Studio's Layout Inspector reads the AndroidX `.version` files, `androidx.compose.ui_ui.version` included, to detect the Compose version. Stripping them in debug degrades UI inspection, the tool you need to chase recomposition jank.
- **Fix:** apply this exclude to release variants only, through `androidComponents.onVariants { if (it.buildType == "release") it.packaging.resources.excludes.add(...) }`.

#### E5 Verified, no action needed

- **Exported components:** in the template, `MainActivity` is the launcher, `AppWidgetReceiver` is exported as widgets require, and everything else is `exported=false`. The FileProvider is in `:core:media` with an `${applicationId}` authority. The WorkManager initializer is removed correctly (manifest lines 32-41).
- **devtools is absent from production builds,** as the feature claims. The UI strings appear in VitFleet's `dev/release` dex and are absent from `prod/release`, because R8 folds `IS_SHIPPABLE`.

---

### F. Variants and SDK levels

#### F1 [L] `:benchmark` hard-codes SDK levels that the generator never rewrites

- **Where:** `benchmark/build.gradle.kts:12-23,28-32` sets `compileSdk = 37`, `targetSdk = 37` and Java 17 directly. `render.apply_build_settings` (`generator/genkit/render.py:284-308`) rewrites only `AppConfig.kt`.
- **Fix:** add a small `com.base.app.android.test` convention that reads `AppConfig`, or use AGP's settings-level `android { compileSdk; minSdk }` (`SettingsExtension.compileSdk`, noted in `CommonExtension.kt:478`).

#### F2 [L] Default minSdk 26, while both real apps chose higher

- `spec.py:39` sets `DEFAULT_MIN_SDK = 26`. PeriFlow uses 30 and VitFleet 29.
- Liquid glass blurs only on API 31+ (RenderEffect) and refracts only on 33+ (AGSL), per PeriFlow's catalogue comment: *"refraction, dispersion and highlights on Android 13+, blur on 12"*.
- The default does not need to change. The liquid-glass feature should document and implement a solid-surface fallback below 31.
- The desugaring branch (min below 26) is only ever built by the manual `spec_minimal.json` (min_sdk 24), never in CI (see G1).

---

### G. CI: the `android` job (`.github/workflows/ci.yml:72-142`)

#### G1 [L–M] Coverage gaps and avoidable cost

- The `Generate` step (`:98-124`) always uses the default minSdk 26, compile/target SDK 37, and the default locale set:
  - `NEEDS_CORE_LIBRARY_DESUGARING` (`AppConfig.kt:32`) and targetSdk below compileSdk are never built in CI.
  - `generator/tests/spec_minimal.json` (min 24, target 36) is only in the AGENTS.md manual steps.
  - **Fix:** run `none` with `min_sdk: 24` and `target_sdk: 36`.
- `buildHealth` never runs (A2), and the template itself is never built in CI (the `all` preset approximates it).
- `assembleProdRelease` (`:138`) turns ABI splits on, because the task name contains "Release". Every matrix entry packages and signs 5 APKs (C2).
- There is no `actions/upload-artifact` for `**/build/reports/**` on failure, so lint and test failures can only be read in the log.
- JDK 17 (`:92-95`). `AppDatabaseMigrationTest.kt:86` notes *"Robolectric refuses SDK 36 and above on a Java 17 toolchain"*. JDK 21 on CI and in the docs lets Robolectric run at the real target.
- The JVM heap from D10 combined with `--no-daemon` uses about 9 GB. That is tight on private-repo runners.

---

### H. Version-catalogue and config hygiene

#### H1 [L] Unused catalogue aliases

These aliases are referenced nowhere in `*.kts` or build-logic:

| Alias | `libs.versions.toml` line |
|---|---|
| `androidx-lifecycle-runtime-ktx` | 77 |
| `androidx-work-testing` | 136 |
| `androidx-paging-testing` | 149 |
| `androidx-test-core` | 166 |
| `androidx-test-rules` | 168 |
| `mockk-android` | 176 |
| plugin `kotlin-android` | 196 (B2) |

Generated projects also keep catalogue entries for features they turned off (firebase, lottie, glance, play…). Only `kover` and `dependency-analysis` are fenced (`:206-211`).

**Fix:** fence feature-owned aliases with `# <opt:x>`, like the plugins already are.

#### H2 [L] Stability config points at a library the project does not have

- `config/compose-stability.conf:10` lists `kotlinx.collections.immutable.*`. The Compose compiler 2.4.20 already treats `ImmutableList`, `PersistentList` and the rest as stable (`KnownStableConstructs`).
- The library is not in the catalogue, yet the stability-check failure message (`ComposeStability.kt:164-166`) tells users to reach for `PersistentList`.
- **Fix:** add `kotlinx-collections-immutable` to the catalogue and to `:core:designsystem` as `api`, and drop the redundant conf line. Alternatively, change the message to recommend `@Immutable` wrappers.

#### H3 [L] Unused `compileOnly` dependencies in build-logic

`build-logic/convention/build.gradle.kts:41,44,46` declare `kotlin-serialization`, `hilt` and `google-services`. The plugins are applied only by id, and are already on the classpath through the root `apply false`, so these lines do nothing.

Keep crashlytics if A3 is adopted, since it needs `CrashlyticsExtension`.

Also add `tasks.validatePlugins { enableStricterValidation = true; failOnWarning = true }`.

#### H4 [L] Generator dead path

`generator/genkit/render.py:52` lists `_TEMPLATE_ONLY = {"catalog/src/test/screenshots"}`, a directory that no longer exists, because screenshot tests were removed (`LEGACY_FEATURES["screenshottests"] = None`). PeriFlow re-added Roborazzi in its feature convention.

---

## 3. Liquid glass (Kyant backdrop): adding it as an optional, FPS-safe feature at the build level

What PeriFlow did, and what to port:

- **Catalogue** (PeriFlow `libs.versions.toml`):
  - `kyantBackdrop = "2.0.1"`, `kyantShapes = "1.2.1"`
  - `kyant-backdrop = { group = "io.github.kyant0", name = "backdrop", … }`
  - `kyant-shapes = { group = "io.github.kyant0", name = "shapes", … }`
  - Fence them as `# <opt:liquidglass>`.
- **Module:** `:core:designsystem` only, as `implementation` (PeriFlow `core/designsystem/build.gradle.kts:23-27`). Screens use design-system components and never touch Kyant. Keep that rule, and add `kyant` to the `verifyComposeUsage` forbidden-import list for features.
- **Facts about the artifacts,** from the Gradle cache:
  - Apache-2.0, already allowed by licensee (`app/build.gradle.kts:149`).
  - AAR `minSdkVersion="21"`.
  - Depends on Compose foundation, ui and ui-graphics 1.12.0. The BOM `2026.09.00` resolves 1.12.1, so they are compatible.
  - **Ships no consumer R8 rules and no `baseline-prof.txt`.**
  - Published for Android and desktop (`backdrop-android`, `backdrop-desktop`), which fits the Compose Multiplatform generator plan.
  - Measured R8 dex cost in PeriFlow: `com.kyant` is **19 KB**.
- **FPS at build level:**
  1. Because Kyant ships no baseline profile, the first glass frame and shader set-up are JIT-compiled unless the **app** profile covers them. Fix A1, and make the generator journey visit every glass surface (bottom bar, sheets, top bars) and every push/pop and tab switch.
  2. Add a `FrameTimingMetric` macrobenchmark to `:benchmark` next to the startup one: tab switches, push/pop and list fling over glass. The "fps better" goal then becomes a number (`frameOverrunMs` P50/P90), with `CompilationMode.Partial()` against `None()`.
  3. Keep R8 full mode (the AGP 9 default) and `isShrinkResources` (already on). Do not add `-keep` rules for Kyant; none are needed.
  4. For release-only FPS micro-savings, `-assumevalues class androidx.compose.runtime.ComposerKt { boolean isTraceInProgress() return false; }` removes per-composable trace checks. Optional: it costs Perfetto composition tracing in release, and Compose's own `proguard.txt` does not do it.
- **Generator:** add `Feature("liquidglass", requires=(), files=(designsystem glass files,))` with a variant directory that gives the "off" implementation as plain translucent surfaces. Gate runtime use on `Build.VERSION.SDK_INT >= 31`, with refraction from 33.

---

## 4. What to adopt from PeriFlow and VitFleet (build level)

| What | Source | Note |
|---|---|---|
| `missingDimensionStrategy("environment","dev")` + `mergeIntoMain = true` | PeriFlow `benchmark/build.gradle.kts:18-20`, `app/build.gradle.kts:135-137`, commit `11e50cd` | Fixes A1 |
| Journey-walking baseline profile generator | PeriFlow `benchmark/.../BaselineProfileGenerator.kt` | Split into startup and journey rules |
| Crashlytics mapping upload behind `-PcrashlyticsUpload=true` | VitFleet `AndroidFirebaseConventionPlugin.kt:16-25`, commit `2ef1f33` | Fixes A3 |
| `android:enableOnBackInvokedCallback="true"` | VitFleet `app/src/main/AndroidManifest.xml:17`, commit `00baeaf` | Fixes E1 |
| Explicit `androidx.fragment` pin (1.9.1) for biometric | VitFleet `libs.versions.toml` + `core/applock/build.gradle.kts` | Fixes C5 |
| Applock and update moved out of `:app` into `:core:applock` and `:core:update` | VitFleet `settings.gradle.kts`, `core/applock/build.gradle.kts` | Keeps `:app` a thin composition root. Do the same for playstore, widget and push |
| API URL overrides through Gradle properties, with WS URL derived | PeriFlow `AppConfig.kt`/`Flavors.kt`; VitFleet `Flavors.kt:12-29` (`-PdevHost`, `-PdevServer`) | No code edits for local backends |
| `ExperimentalSharedTransitionApi` opt-in | VitFleet `Compose.kt:47` | Only if shared-element transitions are adopted |
| `espresso-core` pinned for androidTest | VitFleet `Compose.kt:67-68` (*"Android 16 removed the InputManager reflection used by older transitive Espresso"*) | Needed once any androidTest exists |
| Kyant backdrop + shapes in the catalogue, designsystem-only | PeriFlow catalogue + `core/designsystem/build.gradle.kts:23-27` | See §3 |
| `navigationevent-compose` in `:core:navigation` | PeriFlow `libs.versions.toml` (`navigationevent = "1.1.2"`), `core/navigation/build.gradle.kts:22` | For the predictive-back work |
| Robolectric + Compose UI test in `:core:navigation` for back routing | PeriFlow `core/navigation/build.gradle.kts` test deps | Opt in per module (D2) |
| Roborazzi screenshot tests (optional) | PeriFlow `AndroidFeatureConventionPlugin.kt` | Behind an opt-in flag, not in every feature, because of its build-time cost |

Also fix in both apps: the `gradle.properties` B1 lines, C1 `localeFilters` (PeriFlow already "dropped template locales" in commit `a50962b`), C2 ABI splits, and C8 R8 duplicates.

---

## 5. Recommended convention-plugin shape (sketch)

```kotlin
// AndroidLibraryConventionPlugin
pluginManager.apply("com.android.library")
extensions.configure<LibraryExtension> {
    configureAndroidCommon(this)                  // no includeAndroidResources, no useSupportLibrary
    androidResources.enable = file("src/main/res").exists()
}
extensions.configure<LibraryAndroidComponentsExtension> {
    val unit = file("src/test").exists(); val device = file("src/androidTest").exists()
    beforeVariants { v ->
        v.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = unit && v.buildType == "debug"
        v.deviceTests[DeviceTestBuilder.ANDROID_TEST_TYPE]?.enable = device
    }
}
if (file("src/test").exists()) dependencies { add("testImplementation", project(":core:testing")) }
registerComposeGuards()        // with @OutputFile markers
verifyModuleDependencies()     // with @OutputFile marker

// AndroidFeatureConventionPlugin — the minimum
deps: :core:designsystem, :core:ui, :core:navigation, hilt-lifecycle-viewmodel-compose,
      kotlinx-serialization-core (nav keys)    // no coil, no core:model, no core:coroutines

// AndroidApplicationConventionPlugin
release { isMinifyEnabled = true; isShrinkResources = true
          proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
androidResources.localeFilters += <generated>
```

`gradle.properties`, trimmed:

```properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.parallel=true
kotlin.daemon.jvmargs=-Xmx2g -XX:+UseParallelGC
kotlin.code.style=official
enableComposeCompilerMetrics=false
enableComposeCompilerReports=false
```
