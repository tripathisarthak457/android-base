# Final After

Generated from the [Android base template](https://github.com/tripathisarthak457/android-base).

> **The generator is in beta.** This project compiles, tests and lints clean, but not many
> people have used the generator yet. If you hit something, a report at
> [the issue tracker](https://github.com/tripathisarthak457/android-base/issues) is genuinely
> useful.

> **Working with an AI coding agent?** Claude Code reads [`CLAUDE.md`](CLAUDE.md), and Codex, Gemini CLI and
> Android Studio read [`AGENTS.md`](AGENTS.md), the same text. Both give the rules this project is built to, which of
> Google's Android skills to use when, and the detailed docs: [architecture](docs/ARCHITECTURE.md),
> [performance](docs/PERFORMANCE.md) and the [design system](docs/DESIGN-SYSTEM.md).

## Run it

Open the project in Android Studio and run the `app` configuration, or:

```bash
./gradlew :app:installDevDebug
```

Gradle runs on whichever JDK Android Studio ships (its bundled JetBrains Runtime) or any JDK 17
or newer on your `PATH`. Nothing downloads a second JDK. The code is compiled for Java 17.

## The look

This project was generated without UI components. `:core:designsystem` is a theme skeleton: `AppTheme` with every
token slot (colour, type, spacing, shape, motion, reduce motion, window size classes) holding your colours, your font
and neutral values for the rest. Every screen is a placeholder route over its ViewModel, and the app opens on a blank
start screen. [docs/DESIGN-SYSTEM.md](docs/DESIGN-SYSTEM.md) says how to build components on it, and
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#what-this-project-leaves-to-you) what is left to you.

### Compose stability

```bash
./gradlew checkComposeStability
```

Part of `check`. Reads the Compose compiler's own report for `:core:designsystem`, `:core:ui`,
`:core:navigation` and every feature module, and fails on a composable parameter whose type the
compiler cannot prove stable.
Under strong skipping such a parameter is compared by identity, so the composable reruns whenever
its caller passes a new instance, even an equal one, and nothing in a diff shows it.

Pass a stable type, a `() -> T` provider, or declare a type immutable in
`config/compose-stability.conf`. `config/compose-stability-baseline.txt` accepts the rest, one
`fqName(parameter)  # reason` per line; deleting a line records a fix. To accept a new one
deliberately: `./gradlew :core:designsystem:recordComposeStability`, then write the reason, in a
commit that says why.

### Accessibility in every preview

Every `@Preview` is a screenshot test, and each render goes through Google's Accessibility Test
Framework: a touch target under 48 dp, a control with no label or two controls on one spot fails
the test that drew it (`./gradlew check` runs them all). Fix the finding in the component or the
screen; a result that is right to accept goes in `AccessibilityPreviewTester`'s list with its
reason, never by turning a check off.

### What the release ships

```bash
./gradlew dependencyGuard              # part of check
./gradlew dependencyGuardBaseline      # after a deliberate change
```

The prod release's libraries, transitive ones included, are listed under app/dependencies. The first
build writes the list; commit it. From then on a new library, or a version a bump drags in, fails
the build until `dependencyGuardBaseline` rewrites the list, so the change is read in the same
commit as its cause.

### Formatting and the Compose rules

```bash
./gradlew spotlessCheck      # part of check
./gradlew spotlessApply      # fixes the layout
```

Spotless runs ktlint in Android Studio's style over every Kotlin and Gradle file, so the IDE's
reformat and the build agree, and the Compose Rules for ktlint, which check how a composable takes
and uses its parameters. `.editorconfig` holds both, and names each Compose rule turned off and why.


### Coverage

```bash
./gradlew koverHtmlReport      # read it
./gradlew koverVerify          # the floor the build enforces
```

One merged number for the whole project, with generated code excluded — Hilt's factories, Room's
DAO implementations, the singletons the Compose compiler emits — so that what is left is code
somebody wrote and could have tested.

The floor is `coverageMinimum` in `gradle.properties`. It is a ratchet: raise it when a release
comfortably clears it. Lowering it to turn a red build green is the edit that makes the whole
thing decorative, so do that in a commit that says so out loud.

### Dependency health

```bash
./gradlew buildHealth
```

Names every module declaring a dependency it never uses, and every module using one it only gets
transitively. The second is the one worth having: that module compiles only because something else
put the library on its classpath, and it breaks the day that something else drops it — for a
reason unrelated to the commit that broke it.

It prints rather than fails. Two of this template's own decisions produce advice the plugin is
right to give and this project is right to ignore: `:core:designsystem` exposes Compose as `api`
on purpose, and the Compose convention plugin gives every module the same dependency set. Making
it a gate means working through the report first and then setting `severity("fail")` in the root
`build.gradle.kts` — worth doing, and only in that order.

## Variants

Four environments — `dev`, `staging`, `prod`, `playstore` — times debug and release, minus
`playstoreDebug`, which does not exist. `dev` and `staging` install alongside each other and
alongside production (`.dev` and `.staging` application id suffixes) and are stamped
`1.0.0-devDebug` and so on; `prodRelease` and `playstoreRelease` stay a bare `1.0.0`,
because that string ends up on a store listing.

Only `:app` carries flavours. Every library module builds once, which is why switching
environment does not rebuild the project.

## Release artifacts

```bash
./gradlew :app:distProdRelease                # per-ABI APKs + a universal one
./gradlew :app:distPlaystoreReleaseBundle     # the AAB for Play
```

Everything lands in `build/outputs/dist/<variant>/` named with the variant, ABI, version and
build time, with a `checksums.sha256` beside it.

Two calls rather than one, because AGP refuses to build ABI splits and an app bundle in the same
invocation — and they are different jobs anyway: per-ABI APKs are for handing to testers, while
Play splits the bundle itself.

Per-ABI APKs are built only with `-PabiSplits=true`; without it `dist` copies the universal APK.
Play already delivers one ABI per device from the bundle, so most builds never need the split.

## Structure

```
app/          composition root — wires everything, owns no logic
core/         infrastructure. Never depends on data or feature.
data/         one business domain each. Never depends on another data module.
feature/      presentation only. Never depends on another feature.
```

Those last two rules are enforced by the `verifyModuleDependencies` task, which fails the build
rather than relying on review discipline.

Feature modules generated: none yet

## Where the app starts

The app opens on a blank start screen (`StartKey`, named by `AppDestinations.start`) hosted by
Navigation 3's own `NavDisplay`, and nothing leads off it yet: every destination a feature
registers draws a placeholder, which a navigator call reaches, and a link only once a
`DeepLinkRoute` maps it. The session still keys the host, so signing out starts a fresh back
stack; [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#the-entry-identity-is-wired-the-gates-are-yours)
shows how to send each identity to its own start once you have onboarding and sign-in screens.

## Adding a feature

A feature needs two things, both inside its own module:

1. A `@Serializable` key implementing `AppNavKey`.
2. A `navGraph { entry<YourKey> { YourRoute() } }` contribution, `@Provides @IntoSet`.

Nothing in `:app` or `:core:navigation` is edited — that is the point of the decentralised
registry, and it is why two people adding screens in the same week do not conflict.

`entry<YourKey>` also records the key's serializer, so a key that can be opened can be saved: the
back stack comes back where the user was after the app is killed in the background.
`NavKeyRegistrationTest` still checks every key.

The generator will scaffold all of that for you, including a matching `:data:` module and its
tests:

```bash
py add_feature.py orders --project .
```

In this project a new feature brings its data module, ViewModel, tests and a placeholder route,
never UI components, and `--tab` is refused: there is no tab bar until you build one.

And to take one back out again, including the three registrations it added:

```bash
py remove_feature.py orders --project .
```

## Regenerating

`generator-spec.json` records the answers this project was generated from, and the generator
commit that produced it. Signing keys are left out of it, so it is safe to commit. To see what a
newer template would change, generate the same project again and compare:

```bash
python3 create_project.py --spec path/to/this/generator-spec.json --out /tmp/fresh --no-zip
```

## Signing

No signing keys were set up, so every variant falls back to the debug key and the build says so on each run. To make your own, run these from the project root — each asks for a password and the certificate's details:

```bash
mkdir -p keys
keytool -genkeypair -keystore keys/dev.jks -alias finalafter-dev -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
keytool -genkeypair -keystore keys/staging.jks -alias finalafter-staging -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
keytool -genkeypair -keystore keys/prod.jks -alias finalafter-prod -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
keytool -genkeypair -keystore keys/playstore.jks -alias finalafter-playstore -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
```

Then copy `keystore.properties.template` to `keystore.properties` and fill in each block with the alias and password you chose. dev and staging may share one key; prod and playstore must not share with anything, because the Play upload key is the one credential whose loss cannot be undone.

### Firebase

`app/google-services.json` is a placeholder with no real project behind it — it exists so the
first build succeeds. Replace it with the file from your Firebase console, with one client entry
per application id (`com.example.cmp.dev`, `com.example.cmp.staging`, `com.example.cmp`), or drop a separate
file per flavour under `app/src/<flavour>/`.

## Enabled features

- analytics
- applock
- appstatus
- auth
- browser
- ci
- coil
- coverage
- crashlytics
- database
- deeplink
- depsanalysis
- devtools
- firebase
- firebaseanalytics
- flags
- formatting
- forms
- googlefonts
- googlesignin
- language
- leakcanary
- licenses
- media
- network
- offlinequeue
- onboarding
- paging
- perfhud
- performance
- playstore
- profile
- push
- remoteconfig
- sample
- search
- settings
- websocket

## Conventions worth knowing

- **No Material.** `:core:designsystem` is built on `foundation` + `ui` and supplies its own
  theme and indication. An `androidx.compose.material` import fails the build — see the
  `verifyCodeRules` task, which also catches `@Composable` in a module that forgot the Compose
  compiler plugin.
- **Library modules have no `BuildConfig`.** Anything that varies per environment is injected
  from `:app`, so a library cannot behave differently depending on which variant compiled it.
- **Dispatchers are injected**, never `Dispatchers.IO` inside a class, so tests get a virtual
  clock instead of real seconds.
- **Events never suspend.** `MviViewModel.onEvent` handles each event on the main thread, so two
  events cannot interleave between a read and a write of the state; slow work goes through
  `launchLatest` or `launchExclusive`, and nothing an event starts can make Back or a keystroke wait.
- **`LoadState` is sealed.** There is no `isLoading` + `error` pair, and therefore no way to
  represent "loading and errored at the same time".
- **Smoothness has rules and numbers.** [docs/PERFORMANCE.md](docs/PERFORMANCE.md) says what keeps
  the app at 120 Hz on a budget phone, why, and how to measure it.
