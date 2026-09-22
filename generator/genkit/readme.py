"""
The README that ships inside a generated project.

Kept out of `create_project.py` and written with `__TOKEN__` placeholders rather than an f-string,
because the text is full of Kotlin and Gradle snippets — and every `{ }` in those would have to be
doubled inside an f-string, which is exactly the kind of quiet corruption nobody notices until a
generated README shows `navGraph { }` as `navGraph  `.
"""

from __future__ import annotations

from pathlib import Path

from .spec import ProjectSpec

_TEMPLATE = """# __APP_NAME__

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

This project was generated with the **__DESIGN_STYLE__** design style and the **__MOTION_STYLE__**
motion style. Both are one argument to the single `AppTheme` call in `app/.../ui/AppRoot.kt`:

```kotlin
AppTheme(
    motionStyle = AppMotionStyle.__MOTION_STYLE__,
    designStyle = AppDesignStyle.__DESIGN_STYLE__,
)
```

Change either and every screen follows. The components in `:core:designsystem` are a starting
point, not a finished brand. Change their shapes, spacing and colours until the app looks like
yours rather than like the template.
__CATALOG__
__CHECKS__
## Variants

Four environments — `dev`, `staging`, `prod`, `playstore` — times debug and release, minus
`playstoreDebug`, which does not exist. `dev` and `staging` install alongside each other and
alongside production (`.dev` and `.staging` application id suffixes) and are stamped
`__VERSION__-devDebug` and so on; `prodRelease` and `playstoreRelease` stay a bare `__VERSION__`,
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

Feature modules generated: __MODULES__

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

## Signing

__SIGNING__
__FIREBASE__
## Enabled features

__FEATURES__

## Conventions worth knowing

- **No Material.** `:core:designsystem` is built on `foundation` + `ui` and supplies its own
  indication, typography, components and icons. An `androidx.compose.material` import fails the
  build — see the `verifyComposeUsage` task, which also catches `@Composable` in a module that
  forgot the Compose compiler plugin.
- **Library modules have no `BuildConfig`.** Anything that varies per environment is injected
  from `:app`, so a library cannot behave differently depending on which variant compiled it.
- **Dispatchers are injected**, never `Dispatchers.IO` inside a class, so tests get a virtual
  clock instead of real seconds.
- **Events are queued, not launched.** `MviViewModel` drains them in order on one coroutine, so
  two events cannot interleave between a read and a write of the state.
- **`LoadState` is sealed.** There is no `isLoading` + `error` pair, and therefore no way to
  represent "loading and errored at the same time".
"""

_CATALOG_NOTE = """
```bash
./gradlew :catalog:installDebug
```

The catalog installs beside the app and shows every design-system component in both themes. It
depends on `:core:designsystem` alone, so working on a component rebuilds two modules rather than
the whole graph.
"""




_STABILITY_NOTE = """
### Compose stability

```bash
./gradlew checkComposeStability
```

Reads the Compose compiler's own report for `:core:designsystem` and `:core:ui` and fails on a
composable that restarts without being able to skip, or that takes a parameter the compiler cannot
prove immutable. Either one costs a frame every time the parent recomposes — in a list, once per
row per frame — and neither is visible in a diff.

`config/compose-stability-baseline.txt` holds what was already there when the check was turned on.
Deleting a line is how a fix is recorded; the check fails again if it comes back. When the cause is
a type from a library this project cannot change, declare it immutable in
`config/compose-stability.conf` rather than accepting it into the baseline.

To accept a new one deliberately: `./gradlew :core:designsystem:recordComposeStability`, in a
commit that says why.
"""

_COVERAGE_NOTE = """
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
"""

_DEPS_NOTE = """
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
"""


_FIREBASE_NOTE = """
### Firebase

`app/google-services.json` is a placeholder with no real project behind it — it exists so the
first build succeeds. Replace it with the file from your Firebase console, with one client entry
per application id (`__PACKAGE__.dev`, `__PACKAGE__.staging`, `__PACKAGE__`), or drop a separate
file per flavour under `app/src/<flavour>/`.
"""


def write_readme(project_dir: Path, spec: ProjectSpec) -> None:
    modules = ", ".join(f"`:feature:{name}`" for name in spec.feature_modules) or "none yet"
    features = "\n".join(f"- {name}" for name in sorted(spec.features))

    if spec.keystores:
        signing = (
            "`keystore.properties` and `keys/*.jks` were generated, and both are git-ignored. Put "
            "the passwords in your password manager now: the Play upload key is the one "
            "credential whose loss cannot be undone."
        )
    else:
        alias = spec.lower_name or "app"
        commands = "\n".join(
            f"keytool -genkeypair -keystore keys/{name}.jks -alias {alias}-{name} "
            f"-keyalg RSA -keysize 2048 -validity 10000 -storetype PKCS12"
            for name in ("dev", "staging", "prod", "playstore")
        )
        signing = (
            "No signing keys were set up, so every variant falls back to the debug key and the "
            "build says so on each run. To make your own, run these from the project root — each "
            "asks for a password and the certificate's details:\n\n"
            f"```bash\nmkdir -p keys\n{commands}\n```\n\n"
            "Then copy `keystore.properties.template` to `keystore.properties` and fill in each "
            "block with the alias and password you chose. dev and staging may share one key; prod "
            "and playstore must not share with anything, because the Play upload key is the one "
            "credential whose loss cannot be undone."
        )

    # Each check documents itself only when the project actually has it. A README describing a
    # task that does not exist is worse than one that says nothing: the first command somebody
    # runs out of it fails, and they stop trusting the rest of the file.
    checks = "".join(
        note
        for feature, note in (
            ("composemetrics", _STABILITY_NOTE),
            ("coverage", _COVERAGE_NOTE),
            ("depsanalysis", _DEPS_NOTE),
        )
        if spec.has(feature)
    )

    body = (
        _TEMPLATE
        .replace("__APP_NAME__", spec.app_name)
        .replace("__DESIGN_STYLE__", spec.design_style)
        .replace("__MOTION_STYLE__", spec.motion_style)
        .replace("__VERSION__", spec.version_name)
        .replace("__MODULES__", modules)
        .replace("__CHECKS__", checks)
        .replace("__SIGNING__", signing)
        .replace("__CATALOG__", _CATALOG_NOTE if spec.has("catalog") else "")
        .replace(
            "__FIREBASE__",
            _FIREBASE_NOTE.replace("__PACKAGE__", spec.package_name) if spec.has("firebase") else "",
        )
        .replace("__FEATURES__", features)
    )

    (project_dir / "README.md").write_text(body, encoding="utf-8")
