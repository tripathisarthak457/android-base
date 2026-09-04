# Android project generator

> **Beta.** Every feature combination is compiled, tested and linted in CI before it ships, and
> the checks below are the ones I run myself — but not many people have used this yet, and that is
> the part that finds the last few bugs.
> [Open an issue](https://github.com/tripathisarthak457/android-base/issues) if something breaks;
> the browser version has a report button that attaches your configuration for you.

This repository is two things.

`template/` is a working Android app: multi-module, Jetpack Compose, MVI, Hilt, Ktor, four build
environments, signed release output. You can open it in Android Studio right now and run it.

`generator/` is a Python script that copies that app, renames it to whatever you are building,
strips out the parts you said you didn't want, generates your signing keys, and hands you a zip.

The point is that the first day of a new project stops being a day.

---

## Contents

1. [Requirements](#requirements)
2. [Generating a project](#generating-a-project)
3. [Checking that it worked](#checking-that-it-worked)
4. [What the wizard asks](#what-the-wizard-asks)
5. [Optional features](#optional-features)
6. [What you get either way](#what-you-get-either-way)
7. [Non-interactive use](#non-interactive-use)
8. [Adding a feature module later](#adding-a-feature-module-later)
9. [Working on the template itself](#working-on-the-template-itself)
10. [When something goes wrong](#when-something-goes-wrong)

---

## Requirements

| | | |
|---|---|---|
| **Python** | 3.10 or newer | Runs the generator. No packages needed. |
| **JDK** | 17 or newer | Runs Gradle. `java -version` should say 17+. |
| **Android SDK** | with API 37 installed | Android Studio installs this. |
| **keytool** | on `PATH`, or under `JAVA_HOME` | Only if you want generated keystores. Ships with the JDK. |
| **git** | on `PATH` | Only for `--git`. |
| **Pillow** | `pip install pillow` | Only for `--icon`. |

The last three are genuinely optional. Without `keytool` you get a project that builds and says
"falling back to the debug key" on every release build; without Pillow you get the template's own
launcher icon and a warning telling you so. Neither costs you the project.

---

## Generating a project

```bash
cd generator
py create_project.py
```

On macOS and Linux use `python3` instead of `py`.

The wizard walks through ten sections. Every question has a default in brackets; pressing Enter
takes it. At the end it prints a summary, asks you to confirm, and opens a save dialog. If there
is no display — over SSH, in a container — it asks for a path instead.

A run takes about two minutes if you accept the defaults, about five if you type your own
answers and set up four keystores.

### Faster ways in

```bash
# Skip the per-feature questions and take a curated set.
py create_project.py --preset standard

# Everything on, including the things most projects don't want.
py create_project.py --all

# Generate launcher icons from your logo, and start a git repo with the first commit made.
py create_project.py --icon ~/Desktop/logo.png --git

# Write to a directory instead of a zip, and skip the save dialog.
py create_project.py --out ../MyApp --no-zip

# Record the answers so you can regenerate the same project later.
py create_project.py --save-spec myapp.json
```

The three presets:

| Preset | Feature count | What's in it |
|---|---|---|
| `lean` | 5 | Ktor, the design system, the catalog app, one reference feature, detekt, LeakCanary. |
| `standard` | 16 | The lean set plus Room caching, Coil, forms, auth, settings, onboarding, media, deep links, GitHub Actions and Fastlane. |
| `everything` | 24 | The standard set plus Firebase with Crashlytics, FCM push, analytics, WorkManager, WebSocket and baseline profiles. |

---

## Checking that it worked

Do not take the script's word for it. Four checks, in order of how long they take.

### 1. It compiles (about a minute)

```bash
cd MyApp
./gradlew :app:assembleDevDebug
```

Expect `BUILD SUCCESSFUL`. If Gradle complains that it cannot find the SDK, see
[When something goes wrong](#when-something-goes-wrong).

### 2. It runs

```bash
./gradlew :app:installDevDebug
```

Then open it from the launcher. With the default features you should land on a three-page
onboarding walkthrough, then a sign-in screen. Sign-in will fail unless you pointed it at a real
backend — the default is `https://dev.example.com/api/` — and it should fail *politely*, with
"Could not reach the server." in a red banner rather than a crash. That banner is the proof that
Ktor, the coroutine dispatchers, the error classifier, the MVI event loop and the design system
are all wired together.

Install the catalog beside it and every component is on a page you can scroll:

```bash
./gradlew :catalog:installDebug
```

### 3. Everything passes (about seven minutes, cold)

```bash
./gradlew build
```

This is the check that matters. It compiles all seven variants, runs the unit tests, runs detekt
over every module and runs Android lint over every module. A generated project should be green on
the first run, before you have written a line.

### 4. A release is actually signed

```bash
./gradlew :app:distDevRelease
ls app/build/outputs/dist/devRelease
```

You should see five APKs and a checksum file:

```
KeyedApp-devRelease-arm64-v8a-1.0.0-1-20260904-1048.apk
KeyedApp-devRelease-armeabi-v7a-1.0.0-1-20260904-1048.apk
KeyedApp-devRelease-universal-1.0.0-1-20260904-1048.apk
KeyedApp-devRelease-x86-1.0.0-1-20260904-1048.apk
KeyedApp-devRelease-x86_64-1.0.0-1-20260904-1048.apk
checksums.sha256
```

Confirm the signature is yours and not the debug key:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs \
  app/build/outputs/dist/devRelease/KeyedApp-devRelease-arm64-v8a-1.0.0-1-20260904-1048.apk
```

```
V3.0 Signer: certificate DN: CN=Keyed App (dev), O=Acme, C=IN
```

If that line says `CN=Android Debug` instead, `keytool` was not found when you generated and the
build fell back. Fix it by creating the keys yourself and filling in `keystore.properties`.

For the Play Store bundle, use the separate task — AGP refuses to build ABI splits and an app
bundle in the same invocation:

```bash
./gradlew :app:distPlaystoreReleaseBundle
```

---

## What the wizard asks

**Project.** App name and application id. The name is used for the display name, the Gradle root
project, the `Application` subclass and the deep-link scheme, each in the appropriate form —
`My Great App` becomes `MyGreatApp`, `mygreatapp` and `my_great_app` depending on where it lands.

**Android versions.** minSdk, targetSdk, compileSdk. The prompt prints the full API-level table
because nobody remembers that 34 is Android 14. Anything below 26 turns on core-library
desugaring automatically so `java.time` keeps working.

**Version.** versionName and versionCode. dev and staging builds get stamped `1.0.0-devDebug` and
so on; prod and playstore releases stay a bare `1.0.0`, because that string ends up on a store
listing.

**Features.** A preset, or twenty-four yes/no questions. Dependencies resolve themselves: ask for
push notifications and Firebase comes with it.

**Backend URLs.** One per environment, for REST and — if you enabled it — WebSocket. These end up
in `AppConfig.kt`, which is the only place the build reads them from.

**Deep links.** A custom scheme and a host for verified App Links.

**Typeface.** A Google Fonts family name, defaulting to DM Sans. Whatever you type is written into
one constant that all seventeen text styles read from, so changing the app's font later is editing
one string.

**Look and feel.** One brand hex — the whole accent ramp is derived from it, including the pressed
state, the subtle fill, the dark-theme variants, the launcher icon background and whether text on
the accent is black or white. Then a motion style (Standard, Bouncy, Calm, Snappy) and whether
haptics start on.

**Feature modules.** Comma-separated names, or blank. Each one produces a matching `:data:x` and
`:feature:x` pair with a repository, an MVI contract, a ViewModel, a screen, a nav key and tests.

**Signing keys.** Four: dev, staging, prod, playstore. dev and staging may share one — they never
leave your team. prod and playstore must not share with anything; the Play upload key is the one
credential whose loss cannot be undone. The script generates `.jks` files with `keytool` and
writes a git-ignored `keystore.properties`, or takes paths to keys you already have.

---

## Optional features

Everything here can be switched off, and switching it off removes the module rather than leaving
dead code behind.

| Feature | Default | What you get |
|---|---|---|
| REST networking (Ktor) | on | Typed client, bearer auth with transparent 401 refresh, classified failures, pluggable response-envelope unwrapper |
| WebSocket | off | One long-lived socket, exponential backoff with jitter, a connection state a UI can render |
| Offline cache + queue (Room) | on | Per-call-site response caching with stale-on-failure, and failed mutations replayed when connectivity returns |
| Image loading (Coil) | on | Remote images with a skeleton placeholder and a failure glyph |
| WorkManager | off | Hilt-injected workers, including the manifest fix that stops WorkManager self-initialising past the Hilt factory |
| Analytics seam | on | Vendor-agnostic `AnalyticsTracker` and `CrashReporter` with no-op defaults |
| Firebase | off | google-services plugin and the BOM |
| Firebase Analytics | off | Binds the analytics seam to Firebase |
| Crashlytics | off | Binds the crash seam, and routes every logged error through it as a breadcrumb |
| Push notifications (FCM) | off | Channels, the runtime permission check, a messaging service, token re-registration |
| Deep linking | on | Custom scheme plus verified App Links, resolved through one function, with the `onNewIntent` handling that is usually missing |
| Component catalog app | on | A second installable app showing every component in both themes |
| Reference feature | on | A list + detail feature against a live public API, with its tests. Delete it once yours exists |
| Media picker and compression | on | Modern photo/video/document/camera pickers, a permission state that distinguishes "denied" from "denied for good", tunable image and video compression |
| Forms and validation | on | Composable validators, per-field touched/error state, server-side errors mapped back onto the fields that caused them |
| Auth | on | Sign in, sign up and password reset against your endpoints, writing the encrypted token store |
| Settings screen | on | Theme, haptics, analytics opt-out, sign-out, build version |
| Onboarding | on | A paged walkthrough shown once. Skipping counts as finishing |
| Downloadable Google Font | on | Real files per weight through the Play Services provider |
| Detekt | on | Style and formatting in one tool |
| LeakCanary | on | Debug builds only |
| Baseline profile | off | A profile generator and a macrobenchmark that measures cold start with and without it |
| Fastlane | off | Version bump, changelog from git history, tag, signed artifacts, Play internal-track upload |
| GitHub Actions | on | Pull requests build devDebug, run detekt and the tests. Tags produce signed release artifacts |

---

## What you get either way

Some things are not optional, because they are the reason the template exists.

**No Material.** The design system is built on `androidx.compose.foundation` and
`androidx.compose.ui` and nothing else. It supplies its own ripple, typography, 80-odd components,
date and time pickers, bottom sheet, charts and a hand-drawn icon set. An
`androidx.compose.material` import fails the build — see the `verifyComposeUsage` task, which also
catches a `@Composable` in a module that forgot the Compose compiler plugin.

**One place for the look.** Colours, type, spacing, shapes, elevation, motion and haptics are
composition locals read through `AppTheme.`. Changing the font is one string; changing the accent
is one hex; changing how every control responds to a finger is one enum.

**Decentralised navigation.** A feature registers its own screens through Hilt multibinding.
Adding a screen touches no file outside its own module — there is no central sealed `Route` class
to extend and no `when` in `:app` to add a branch to. Navigation 3 is named in exactly one file,
so replacing it is a change to that file and its transitions.

**Per-tab back stacks.** Switching tabs and coming back returns the user where they were.
Re-tapping the active tab pops it to its root.

**Seven build variants.** dev / staging / prod / playstore × debug / release, minus
`playstoreDebug`, which does not exist. Only the two application modules carry flavours, so
switching environment rebuilds nothing in `core`, `data` or `feature`.

**Enforced layering.** `feature → feature` and `data → data` fail the build rather than a code
review. See the `verifyModuleDependencies` task.

`template/docs/ARCHITECTURE.md` explains the reasoning behind each of these.

---

## Non-interactive use

The wizard produces a `ProjectSpec`; the renderer consumes one. Nothing in between needs a
terminal, which is what makes this straightforward to put behind an HTTP endpoint later.

```bash
# Record a spec once.
py create_project.py --save-spec myapp.json

# Regenerate from it, no questions asked.
py create_project.py --spec myapp.json --out ../MyApp --no-zip
```

The spec is plain JSON. Editing it by hand is a supported way to work — the same validation runs
either way, so a typo in the package name is rejected with the same message the wizard would give
you.

---

## Adding a feature module later

The generator scaffolds the modules you name at creation time. Afterwards, the same scaffolder is
a separate script:

```bash
cd generator
py add_feature.py orders --project ../MyApp --tab
```

It writes `:data:orders` and `:feature:orders`, and makes the three edits that are otherwise done
by hand and forgotten one at a time: `settings.gradle.kts`, the app module's dependencies, and —
with `--tab` — the bottom-navigation tab list. It reads the project's own package name out of
`app/build.gradle.kts` rather than asking, so running it in the wrong directory fails immediately
instead of writing files into the wrong namespace.

Multiple at once:

```bash
py add_feature.py orders profile settings --project ../MyApp
```

---

## Working on the template itself

`template/` is a normal Android project. Open it, build it, change it.

```bash
cd template
./gradlew build                    # compile, test, detekt and lint everything
./gradlew :app:installDevDebug
./gradlew :catalog:installDebug
```

Run `./gradlew build` before you commit. It has caught real defects here that no amount of reading
would have: a library module calling `ConnectivityManager` without declaring the permission,
`Bitmap.CompressFormat.WEBP_LOSSY` used below the API level that has it, a locale read that never
recomposed when the user changed language.

The template carries `// <opt:feature>` marker comments that the generator strips. They are chosen
so the template still compiles with *every* feature on, which is what lets this repository's own
build prove the template works. The marker grammar is three forms:

```kotlin
// <opt:push>   …  // </opt:push>      keep the block when `push` is on
// <opt:a|b>    …  // </opt:a|b>       keep it when either is on
// <opt:!push>CODE                     emit CODE when `push` is off
// <generated:name>                    replaced by generated lines
```

The third form has to live inside a comment, because two live declarations of the same class —
one per branch — would break the template's own build. The second exists for the lines that
several optional blocks share and none of them owns: the `javax.inject.Inject` import in the
application class is needed by analytics, push and WorkManager, and is an unused import — which
detekt fails on — the moment all three are off.

### The generator's tests

```bash
cd generator
py -m unittest discover -s tests -t .
```

51 tests over the marker grammar, the rename, feature resolution, the validation rules and the
scaffolder. They run in milliseconds and use only the standard library.

They are not sufficient on their own. The real test is generating both extremes and building
them, because that is what catches a marker left unbalanced in a file nobody thought about:

```bash
py create_project.py --spec tests/spec_full.json    --out ../out/Full --no-zip
py create_project.py --spec tests/spec_minimal.json --out ../out/Bare --no-zip
(cd ../out/Full && ./gradlew build)
(cd ../out/Bare && ./gradlew build)
```

### Layout

```
template/                the Android project. Open this in Android Studio.
generator/
  create_project.py      the wizard and the entry point
  add_feature.py         the scaffolder, for a project that already exists
  genkit/
    spec.py              what a project is, the feature catalogue, the presets, the rules
    prompts.py           the wizard. I/O only, no logic
    render.py            copy, strip markers, rename, apply settings, keystores, zip
    scaffold.py          the :data: + :feature: module pair
    icons.py             launcher icons from one source image
    readme.py            the README that ships inside a generated project
  variants/              the "feature is off" version of a file that has to change, not vanish
  tests/
```

---

## When something goes wrong

**`SDK location not found`**

Gradle cannot find the Android SDK. Either set `ANDROID_HOME`, or create `local.properties` in the
project root:

```properties
sdk.dir=/Users/you/Library/Android/sdk
```

On Windows the colon and the backslashes have to be escaped, or Android lint will fail the build
with `PropertyEscape`:

```properties
sdk.dir=C\:/Users/you/AppData/Local/Android/Sdk
```

**`keytool` was not found**

The generator says so and carries on; every variant falls back to the debug key and the build
prints a warning on each run. `keytool` ships with the JDK — it is usually a `JAVA_HOME` problem
rather than a missing install. Fix it and re-run, or create the keys yourself and fill in
`keystore.properties` (there is a `.template` beside it showing the shape).

**`processDevDebugGoogleServices FAILED`**

You enabled Firebase and are still on the placeholder `app/google-services.json`. Replace it with
the file from your Firebase console, with one client entry per application id — `com.you.app.dev`,
`com.you.app.staging`, `com.you.app` — or drop a separate file per flavour under
`app/src/<flavour>/`.

**Text renders in the system font instead of the one I picked**

The downloadable-font provider could not resolve the family. Check the spelling against the family
page on fonts.google.com — it is case- and space-sensitive, and a misspelling degrades quietly to
the platform typeface rather than failing loudly. It also needs Play Services, so it will not work
on an emulator image without Google APIs.

**`Multiple shrunk-resources files found`**

You asked for ABI splits and an app bundle in the same invocation. Use `distProdRelease` for APKs
and `distProdReleaseBundle` for the bundle, as two commands.

**The generated project's first commit is missing `google-services.json`**

That is deliberate — it is git-ignored, as it should be, since it carries project ids tied to your
account. Each developer needs their own copy, the same as every other Android project.
