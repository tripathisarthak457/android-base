# Android project generator

> **Beta.** The presets and the every-feature combination are compiled, tested and linted in CI
> before a change ships, and the checks below are the ones I run myself — but not many people have
> used this yet, and that is the part that finds the last few bugs.
> [Open an issue](https://github.com/tripathisarthak457/android-base/issues) if something breaks;
> the browser version has a report button that attaches your configuration for you.

This repository is two things.

`templates/android/` is a working Android app: multi-module, Jetpack Compose with a design system
of its own and no Material, MVI, Hilt, Ktor, Navigation 3, four build environments, signed release
output. You can open it in Android Studio right now and run it.

`generator/` is a Python script that copies that app, renames it to whatever you are building,
strips out the parts you said you didn't want, generates your signing keys, and hands you a zip.

The point is that the first day of a new project stops being a day, and that what you start from
is already smooth on a budget phone at 120 Hz, adapts to every window from a phone to a desktop,
and comes with the rules and the measurements that keep it that way as you build on it.

A Compose Multiplatform template (Android and iOS from one codebase) and a native SwiftUI template
are in `templates/multiplatform/` and `templates/ios/`. They are not released yet: the website
shows them as coming soon, and the generator's API does not offer them.

**Using an AI coding agent?** Point it at [`AGENTS.md`](AGENTS.md) to generate a project, and every
generated project ships its own `AGENTS.md` for the work after that.

If you would rather not clone anything, **[android-base.vercel.app](https://android-base.vercel.app)**
runs the same script against the same template and hands back the same zip. It will generate
signing keys too, including prod and playstore, but only if you ask — it is off by default, and
the site spells out the trade before you turn it on. Keys made on someone else's server are keys
whose custody is not solely yours.

---

## Contents

1. [Requirements](#requirements)
2. [Generating a project](#generating-a-project)
3. [Checking that it worked](#checking-that-it-worked)
4. [What the wizard asks](#what-the-wizard-asks)
5. [Optional features](#optional-features)
6. [What you get either way](#what-you-get-either-way)
7. [What the build refuses to let through](#what-the-build-refuses-to-let-through)
8. [Non-interactive use](#non-interactive-use)
9. [Adding a feature module later](#adding-a-feature-module-later)
10. [Working on the template itself](#working-on-the-template-itself)
11. [When something goes wrong](#when-something-goes-wrong)
12. [Licence](#licence)

---

## Requirements

| | | |
|---|---|---|
| **Python** | 3.11 or newer | Runs the generator. No packages needed. |
| **JDK** | 17 or newer | Runs Gradle. Android Studio's bundled JDK (JBR 21 or 25) is enough — nothing downloads a second one. |
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

The wizard walks through a dozen short sections, and skips the ones a project does not need: the
backend URLs without networking, the deep links without deep linking, the typeface without
downloadable fonts. Every question has a default in brackets; pressing Enter takes it. At the end
it prints a summary, asks you to confirm, and opens a save dialog. If there is no display — over
SSH, in a container — it asks for a path instead.

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

The three presets, counted after each feature has brought in what it requires:

| Preset | Features | What's in it |
|---|---|---|
| `lean` | 6 | The design system, the navigation shell and one home screen, with networking, images and CI. The smallest app that still looks and moves like the rest. |
| `standard` | 18 | The lean set plus sign-in, onboarding, settings with a language picker, the in-app browser, feature flags, the developer panel with its performance overlay, a local database and the reference list-detail feature to copy. What most products want on day one. |
| `everything` | 44 | Every feature the template offers, working together. The template's own proof that they coexist; delete what you do not need. |

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

Then open it from the launcher. With the Standard preset you should land on a three-page
onboarding walkthrough, then a sign-in screen. Sign-in will fail unless you pointed it at a real
backend — the default is `https://dev.example.com/api/` — and it should fail *politely*, with
"Could not reach the server." in a red banner rather than a crash. That banner is the proof that
Ktor, the coroutine dispatchers, the error classifier, the MVI event loop and the design system
are all wired together.

Install the catalog beside it and every component is on a page you can scroll:

```bash
./gradlew :catalog:installDebug
```

A debug build is for checking that things work, not how smoothly: it runs without R8 or its
baseline profile and drops several times as many frames as a release build. Judge motion on
`./gradlew :app:installDevRelease`.

### 3. Everything passes (about seven minutes, cold)

```bash
./gradlew build
```

This is the check that matters. It compiles all seven variants, runs the unit tests, runs Android
lint over every module and runs the project's own guards (see
[What the build refuses to let through](#what-the-build-refuses-to-let-through)). A generated project should be green on
the first run, before you have written a line.

### 4. A release is actually signed

```bash
./gradlew :app:distDevRelease
ls app/build/outputs/dist/devRelease
```

You should see the APK and a checksum file:

```
KeyedApp-devRelease-universal-1.0.0-1-20260904-1048.apk
checksums.sha256
```

Add `-PabiSplits=true` for one APK per ABI beside the universal one.

Confirm the signature is yours and not the debug key:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs \
  app/build/outputs/dist/devRelease/KeyedApp-devRelease-universal-1.0.0-1-20260904-1048.apk
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

**Project.** App name, the launcher label if it should differ, and the application id. The name is
used for the Gradle root project, the `Application` subclass and the deep-link scheme, each in the
appropriate form — `My Great App` becomes `MyGreatApp`, `mygreatapp` and `my_great_app` depending
on where it lands.

**Features.** A preset, or a yes/no question per feature. Dependencies resolve themselves: ask for
push notifications and Firebase comes with it.

**Architecture.** Compact (one module per feature and per data source, what most apps need) or
modular (each feature and data module split into `:api` and `:impl`, for larger teams). A compact
project can be split later.

**Look and feel.** Three brand hexes. Each one becomes a full ramp — the pressed state, the
subtle fill, both dark-theme variants and whether text on it is black or white — and the primary
also becomes the launcher icon background. Leave the second and third blank and they are worked
out from the primary: the same hue with the chroma taken out, and a sixth of a turn around the
wheel. Harmonious by construction, and a starting point rather than a brand decision.

The design system itself draws with the primary alone, which is what keeps one action per screen
looking like the action. The other two are there for the screen that genuinely needs a second
colour — a chart with two series, a promotion beside a primary button — so that it reaches for
`AppTheme.colors.secondary` rather than for a hex somebody typed inline.

Then a motion style, a design style and whether haptics start on.

The design style decides what the components look like, all together: corner radii, the button
shape, how cards and text fields are drawn, the tab bar, border weight, the tone of the surfaces
and the weight of the type. Every combination of style and theme is checked against WCAG AA by
`PaletteContrastTest`.

| Style | Looks like | Suits |
|---|---|---|
| Utility | Cool neutrals, hairline outlines, a floating tab bar | Tools, finance, admin |
| Social | Brand-tinted surfaces, pill buttons, heavier headlines, a floating tab bar | Feeds and chat |
| Editorial | Warm paper and ink, large tight display type, underlined fields | Reading and news |
| Playful | Cream surfaces, extra-bold type, thick outlines and offset shadows | Games and kids |

The motion style decides how controls respond to a finger: Standard (crisp, a trace of
overshoot), Bouncy (a visible overshoot), Calm (no overshoot anywhere, slightly longer) or Snappy
(the shortest response that still reads as motion). The shell's own motion, the push, the back
gesture and the tab switch, is the same in every style. Both are one argument to `AppTheme`, so
changing your mind later is one line, and the catalog app switches between all of them live.

**Typeface.** With downloadable fonts on, a Google Fonts family name (DM Sans unless you choose
another) and a monospace one. The type scale reads the family from one place, so changing the
app's font later is editing one name.

**Feature modules.** Comma-separated names, or blank. Each one produces a matching `:data:x` and
`:feature:x` pair with a repository, an MVI contract, a list and a detail screen, a navigation key
and tests.

**Languages.** English is always there. Add Spanish, French, German, Brazilian Portuguese or
Hindi and every screen the template ships is translated, including the components' own copy, and
the language picker in Settings lists them.

**Android versions.** minSdk, targetSdk, compileSdk. The prompt prints the full API-level table
because nobody remembers that 34 is Android 14. Anything below 26 turns on core-library
desugaring automatically so `java.time` keeps working.

**Version.** versionName and versionCode. dev and staging builds get stamped `1.0.0-devDebug` and
so on; prod and playstore releases stay a bare `1.0.0`, because that string ends up on a store
listing.

**Backend URLs.** With networking on, one per environment, for REST and — if you enabled it —
WebSocket. These end up in `AppConfig.kt`, which is the only place the build reads them from.

**Deep links.** With deep linking on, a custom scheme and a host for verified App Links.

**Signing keys.** Four: dev, staging, prod, playstore. dev and staging may share one — they never
leave your team. prod and playstore must not share with anything; the Play upload key is the one
credential whose loss cannot be undone. The script generates `.jks` files with `keytool` and
writes a git-ignored `keystore.properties`, or takes paths to keys you already have. Press Enter at
a password prompt and a strong one is generated and printed once — save it before you go on.

---

## Optional features

Everything here can be switched off, and switching it off removes the module rather than leaving
dead code behind. "In" says which smaller preset includes a feature; `everything` includes them
all. Firebase is never chosen on its own: Firebase Analytics, Crashlytics, Remote Config and push
bring it in. The website and `--list-features` read the same list, grouped the same way.

