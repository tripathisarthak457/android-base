# Working on BaseApp

Instructions for AI coding agents (Claude Code, Codex, Cursor, Copilot, Gemini and the rest).
Read all of this before your first change. It is short on purpose, and every rule here is one
that a generated project has been broken by before.

If you are a person, the README is for you. This file is the same facts written as instructions.

---

## 1. What this project is

A multi-module Jetpack Compose app, generated from a template. MVI, Hilt, Ktor, Navigation 3, and
a design system of its own with no Material underneath. It builds, tests and lints clean as
generated. Keep it that way. A change that turns the build red is not finished.

```
app/          composition root: wiring only, no logic
catalog/      a second app showing every design-system component (if present)
core/         infrastructure. Never depends on data/ or feature/
data/         one business domain each: DTOs, mappers, repository. Never depends on another data/
feature/      screens only. Never depends on another feature/
build-logic/  convention plugins, and the guards that enforce the rules below
```

## 2. Build and verify

Gradle runs on Android Studio's bundled JDK (JBR) or any JDK 17+. Do not add a toolchain, a
`gradle-daemon-jvm.properties` or a JDK download. The code targets Java 17 bytecode.

```bash
./gradlew :app:assembleDevDebug        # it compiles, about a minute
./gradlew test                         # unit tests
./gradlew build                        # everything: all variants, tests, lint, the guards
./gradlew :app:installDevDebug         # run it
```

Before you say a task is done, run `./gradlew build`, or at least `assembleDevDebug` plus the
tests of the modules you touched, and read the output. If something fails, report the failure.
Do not report success on a build you did not run.

## 3. The rules the build enforces

These fail the build. Working around them is never the fix.

- **No Material.** Any `androidx.compose.material*` import fails `verifyComposeUsage`. Build from
  `:core:designsystem`, and from `foundation` and `ui` when it lacks something.
- **Layering.** `feature → feature` and `data → data` fail `verifyModuleDependencies`. When two
  features need the same thing, it belongs in `core/` or in a `data/` module both depend on.
- **Copy lives in `strings.xml`.** A prose literal passed to `text =`, `title =`, `label =` and
  similar parameters in a feature fails the build. Use `stringResource(R.string.x)`, or
  `UiText.of(R.string.x)` from a ViewModel.

## 4. How a feature is shaped

Copy the reference feature (`feature/sample` + `data/sample`) if it exists. Otherwise run the
scaffolder, which writes the same shape:

```bash
python3 path/to/generator/add_feature.py orders --project . --tab
```

By hand, a feature is:

1. A repository **interface** plus a `Default…` implementation, bound in a Hilt `@Module`.
2. A contract: an `@Immutable` state, a sealed event, and a sealed effect.
3. A ViewModel extending `MviViewModel`. State changes go through `updateState`, one-off things
   (navigate, show a message) through `emitEffect` or `showMessage`.
4. A **stateless** screen, `Screen(state, onEvent)`, and a route that connects it to the
   ViewModel through `MviScreen`. Previews call the screen, never the route.
5. A `@Serializable` key implementing `AppNavKey`, registered twice with `@Provides @IntoSet`:
   `navGraph { entry<Key> { … } }` and `navKeys { subclass(…) }`. Forgetting the second one
   works until the process is killed in the background, then comes back on the wrong screen.
6. Tests for the ViewModel with a fake repository and `MainDispatcherRule`, like
   `SampleListViewModelTest`.

Every screen that loads something draws all of its states: loading (a skeleton shaped like the
content), empty, error with retry, and offline. The `LoadState` type exists so none of them is
left out.

## 5. The base components are a starting point

`:core:designsystem` ships about eighty components, four design styles (`AppDesignStyle`) and
four motion styles (`AppMotionStyle`). **They are there for you to change.** Treat them as a
reference, not as a fixed kit you have to use exactly as delivered.

- Make the app look like itself. Adjust tokens first (`Palette.kt`, `AppDesignStyle`, `AppShapes`,
  `AppSpacing`, `AppTypography`), then edit the components. A finished app that looks exactly
  like the template's catalog was not designed.
- Edit a component in place when the design calls for it. Do not wrap it in a new component just
  to avoid touching it, and do not fork a copy per screen.
- When nothing fits, build something new in `:core:designsystem` from `foundation` and `ui`,
  reading the theme through `AppTheme.colors`, `AppTheme.spacing` and the rest. Never hard-code a
  hex colour or a `dp` in a feature.
- Keep what makes them solid when you change how they look: 48dp touch targets, content
  descriptions on icon-only controls, contrast in both themes, reduce-motion handling, and the
  loading states that do not change a button's size.

## 6. Code quality: write like the person who owns this code

This is what separates a good agent change from a typical one. Follow it exactly.

**Comments**

- Comment *why*, never *what*. If a comment repeats the code in English, delete it.
- Most lines need no comment. A comment belongs on the decision a reader would otherwise
  question: a workaround, a non-obvious constraint, a trade-off, a bug it prevents.
- Write in the same voice as the comments already here: full sentences, no filler.
- Never write comments about the change itself: no "Added X", "Updated to…", "New:", "Fixed",
  "As requested", "Step 1:", and no ticket or conversation references. That history belongs in
  the commit message.
- No commented-out code, no banner comments, no `// TODO` unless the user asked for one.
- KDoc on public API only when the name and signature do not already say it all.

**Changes**

- Match the surrounding code: naming, file layout, argument order, how state and errors are
  handled. Read two neighbouring files before writing a new one.
- Keep the diff to what was asked. Do not reformat, rename or "tidy" code you were not asked to
  touch, and do not reorder imports in files you did not otherwise change.
- No placeholders presented as finished work: no stubbed repository, fake delay, `TODO()` body
  or hard-coded list standing in for a real call unless the user asked for a mock. If something
  cannot be done, say so.
- No new library without a reason you can state. Every version goes in
  `gradle/libs.versions.toml`, never inline.
- Do not delete or weaken a test to make the build pass. Fix the code, or explain why the test
  is wrong.
- Small, named functions over long ones. No `!!`, no `runBlocking`, no `GlobalScope`, no
  `Dispatchers.IO` inside a class (inject the dispatcher).

## 7. Where things live

| You want to… | Go to |
|---|---|
| Change a backend URL or a version number | `build-logic/…/AppConfig.kt`, the only place the build reads them |
| Add or reorder a tab | `app/…/ui/AppDestinations.kt` |
| Change the look app-wide | `core/designsystem/…/theme/` and the `AppTheme(…)` call in `app/…/ui/AppRoot.kt` |
| Add an icon | `core/designsystem/…/icon/AppIcons.kt` |
| Call an API | a `data/` module, through `NetworkClient` |
| Store something | `core/datastore`: the session store for per-user data (cleared on sign-out), the settings store for the rest |

## 8. Never

- Commit `keystore.properties`, `keys/`, `local.properties` or a real `google-services.json`.
- Print tokens, passwords or personal data to the log.
- Add Material, a second DI framework, a second navigation library or a second image loader.
- Edit files under `build/`, or generated sources.
- Leave the build red, or say it is green when you did not run it.
