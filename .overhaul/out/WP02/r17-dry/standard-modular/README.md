# Standard Modular

Generated from the [Android base template](https://github.com/tripathisarthak457/android-base).

> **The generator is in beta.** This project compiles, tests and lints clean, but not many
> people have used the generator yet. If you hit something, a report at
> [the issue tracker](https://github.com/tripathisarthak457/android-base/issues) is genuinely
> useful.

> **Working with an AI coding agent?** Point it at [`AGENTS.md`](AGENTS.md) first. It explains
> how this project is laid out, what the base components are for, and how code here is written.

## Run it

Open the project in Android Studio and run the `app` configuration, or:

```bash
./gradlew :app:installDevDebug
```

Gradle runs on whichever JDK Android Studio ships (its bundled JetBrains Runtime) or any JDK 17
or newer on your `PATH`. Nothing downloads a second JDK. The code is compiled for Java 17.

## The look

This project was generated with the **Utility** design style and the **Standard**
motion style. Both are one argument to the single `AppTheme` call in `app/.../ui/AppRoot.kt`:

```kotlin
AppTheme(
    motionStyle = AppMotionStyle.Standard,
    designStyle = AppDesignStyle.Utility,
)
```

Change either and every screen follows. The components in `:core:designsystem` are a starting
point, not a finished brand. Change their shapes, spacing and colours until the app looks like
yours rather than like the template.

```bash
./gradlew :catalog:installDebug
```

The catalog installs beside the app and shows every design-system component in both themes. It
depends on `:core:designsystem` alone, so working on a component rebuilds two modules rather than
the whole graph.

### Compose stability

```bash
./gradlew checkComposeStability
```

Part of `check`. Reads the Compose compiler's own report for `:core:designsystem`, `:core:ui` and
`:core:navigation` and fails on a composable parameter whose type the compiler cannot prove stable.
Under strong skipping such a parameter is compared by identity, so the composable reruns whenever
its caller passes a new instance, even an equal one, and nothing in a diff shows it.

Pass a stable type, a `() -> T` provider, or declare a type immutable in
`config/compose-stability.conf`. `config/compose-stability-baseline.txt` accepts the rest, one
`fqName(parameter)  # reason` per line; deleting a line records a fix. To accept a new one
deliberately: `./gradlew :core:designsystem:recordComposeStability`, then write the reason, in a
commit that says why.


## Variants

Four environments — `dev`, `staging`, `prod`, `playstore` — times debug and release, minus
`playstoreDebug`, which does not exist. `dev` and `staging` install alongside each other and
alongside production (`.dev` and `.staging` application id suffixes) and are stamped
`1.0.0-devDebug` and so on; `prodRelease` and `playstoreRelease` stay a bare `1.0.0`,
because that string ends up on a store listing.

Only `:app` and `:catalog` carry flavours. Every library module builds once, which is why
switching environment does not rebuild the project.

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

ABI splitting is on only when the requested tasks are actually assembling a release, so it never
slows down the debug builds that make up almost every build anyone runs.

## Structure

```
app/          composition root — wires everything, owns no logic
catalog/      the design system, installable on its own
core/         infrastructure. Never depends on data or feature.
data/         one business domain each. Never depends on another data module.
feature/      presentation only. Never depends on another feature.
```

Those last two rules are enforced by the `verifyModuleDependencies` task, which fails the build
rather than relying on review discipline.

Feature modules generated: none yet

## Where the app starts

`app/.../ui/AppDestinations.kt` holds the bottom-bar tabs and the start destination, and nothing
else names a destination directly. `MainActivity.entryPoint()` reads it, together with the two
gates in front of it:

```
onboarding not finished  ->  the walkthrough, no tab bar
not signed in            ->  sign in, no tab bar
otherwise                ->  the tabs, starting at the first
```

Each gate is a function of state the Activity already collects, so finishing onboarding or
signing in is a write to a store — the composition follows. There is no path where the flag says
one thing and the back stack says another.

Each tab keeps its own back stack: switching away and back returns the user where they were, and
re-tapping the active tab pops it to its root.

## Adding a feature

A feature needs three things, all inside its own module:

1. A `@Serializable` key implementing `AppNavKey`.
2. A `navGraph { entry<YourKey> { YourRoute() } }` contribution, `@Provides @IntoSet`.
3. A `navKeys { subclass(YourKey::class, YourKey.serializer()) }` contribution, also `@IntoSet`.

Nothing in `:app` or `:core:navigation` is edited — that is the point of the decentralised
registry, and it is why two people adding screens in the same week do not conflict.

Step 3 is the one worth not forgetting. Without it the app works perfectly until it is killed in
the background, and then comes back at the start destination instead of where the user was.

The generator will scaffold all of that for you, including a matching `:data:` module and its
tests:

```bash
py add_feature.py orders --project . --tab
```

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
keytool -genkeypair -keystore keys/dev.jks -alias standardmodular-dev -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
keytool -genkeypair -keystore keys/staging.jks -alias standardmodular-staging -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
keytool -genkeypair -keystore keys/prod.jks -alias standardmodular-prod -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
keytool -genkeypair -keystore keys/playstore.jks -alias standardmodular-playstore -keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12
```

Then copy `keystore.properties.template` to `keystore.properties` and fill in each block with the alias and password you chose. dev and staging may share one key; prod and playstore must not share with anything, because the Play upload key is the one credential whose loss cannot be undone.

## Enabled features

- auth
- browser
- catalog
- ci
- coil
- database
- deeplink
- devtools
- flags
- forms
- googlefonts
- language
- leakcanary
- network
- onboarding
- room
- sample
- settings

## Conventions worth knowing

- **No Material.** `:core:designsystem` is built on `foundation` + `ui` and supplies its own
  indication, typography, components and icons. An `androidx.compose.material` import fails the
  build — see the `verifyCodeRules` task, which also catches `@Composable` in a module that
  forgot the Compose compiler plugin.
- **Library modules have no `BuildConfig`.** Anything that varies per environment is injected
  from `:app`, so a library cannot behave differently depending on which variant compiled it.
- **Dispatchers are injected**, never `Dispatchers.IO` inside a class, so tests get a virtual
  clock instead of real seconds.
- **Events are queued, not launched.** `MviViewModel` drains them in order on one coroutine, so
  two events cannot interleave between a read and a write of the state.
- **`LoadState` is sealed.** There is no `isLoading` + `error` pair, and therefore no way to
  represent "loading and errored at the same time".
