---

## What you get either way

Some things are not optional, because they are the reason the template exists.

**No Material.** The design system is built on `androidx.compose.foundation` and
`androidx.compose.ui` and nothing else: its own typography, about eighty components, bottom
sheets, charts, pickers and a hand-drawn icon set. A control says what its press looks like
(`Modifier.pressable` with a spring-driven dip), so nothing flashes a grey ripple. An
`androidx.compose.material` import fails the build — see the `verifyCodeRules` task.

**One place for the look.** Colours, type, spacing, shapes, elevation, motion and haptics are
read through `AppTheme`, from a token file the project owns (`DesignTokens.kt`). Changing the
font is one name; changing a brand colour is one hex, and the ramp around it follows; changing how
every control responds to a finger is one enum.

**Smooth on a budget phone.** The template is measured on a Redmi Note 10 Pro at 120 Hz, which is
8.33 ms a frame, on release builds. Tabs stay composed and the new one is nudged in rather than
rebuilt or cross-faded; a push slides on UIKit's curve over a parallax and a drawn scrim; scrolling
decelerates like iOS and rubber-bands at both edges; animations are read in layout and drawing, so
they recompose nothing frame by frame. Every project carries baseline profiles, and the rules that
keep it this way, with the reason and the number each protects, are in its `docs/PERFORMANCE.md`.
With the performance harness, `tools/perf/perf.sh` measures every journey on a connected phone
against the template's gates.

**A back gesture like Gmail's.** During a predictive back swipe the screen shrinks into a rounded
card that follows the finger with the previous screen behind it; it flies out on commit and returns
on cancel. A swipe that starts while a push is still moving runs the same card, and Back pressed
quickly many times lands where it should.

**Every form factor.** Layout follows Android's window size classes, never orientation or device
type, so the same app is designed for a phone, a foldable, a tablet and a desktop window. The tab
bar becomes a rail from Medium width and a sidebar from Large; a list and its detail share the
window from Expanded width; forms and text stop at a readable width and centre; sheets become a
centred card or a side sheet on a large window. Nothing recreates the activity when the window
changes, and locking it to one orientation fails the build. Every screen's preview renders at
seven sizes and settings, and those previews are its screenshot tests.

**Decentralised navigation.** A feature registers its own screens through Hilt multibinding.
Adding a screen touches no file outside its own module — there is no central sealed `Route` class
to extend and no `when` in `:app` to add a branch to. Only `:core:navigation` imports Navigation 3,
so replacing it is a change to that module.

**Per-tab back stacks.** Switching tabs and coming back returns the user where they were, scroll
position and loaded data included. Re-tapping the active tab pops it to its root, or at its root
scrolls its list to the top, and Back at another tab's root returns to the first tab.

**A ViewModel per screen.** Every destination gets its own ViewModel store, cleared when the
screen is popped, so two detail screens never share state.

**One sheet system.** `AppSheet` rises to detents chosen by the speed of the finger, floats inset at
partial heights, lets the screen behind recede into a card at full height, and closes with a drag,
a tap on the dim, Escape or the predictive back shrink.

**An AI agent guide.** `AGENTS.md` (with `CLAUDE.md` and `GEMINI.md` pointing at it) tells an agent
how the project is laid out, that the base components are there to be reshaped, the rules that keep
the app smooth and adaptive, what to check before calling work done, and how to write code and
comments the way the rest of the project does. The guides it links, `docs/PERFORMANCE.md`,
`docs/ADAPTIVE.md` and `docs/ARCHITECTURE.md`, give each rule its reason and say how to check it.
Google's official Android skills ship in `.agents/skills/`, and `AGENTS.md` says when to use each.

**Seven build variants.** dev / staging / prod / playstore × debug / release, minus
`playstoreDebug`, which does not exist. Only `:app` carries flavours, so switching environment
rebuilds nothing in `core`, `data` or `feature`.

**Enforced layering.** `feature → feature` and `data → data` fail the build rather than a code
review. See the `verifyModuleDependencies` task.

**Copy that can be translated.** Every string the app renders lives in a `strings.xml`, and a
literal passed to any copy parameter (`text`, `title`, `confirmLabel` and the rest), either branch
of an `if` written into one, or an English fallback turned into a `UiText` fails the build in the
features, the app shell and the design system, but never inside a `@Preview`. Adding a
`stringResource` is easy and remembering to is not, so it is checked rather than asked for. For a
string a ViewModel produces, `UiText.of(R.string.x)` resolves at render time — so it follows a
locale change without the ViewModel knowing there was one.

**Nothing of the developer tools in production.** The inspector, its badge and the performance
overlay are wired into dev and debug builds only; a production build does not contain them, and the
build checks that.

`templates/android/docs/ARCHITECTURE.md` explains the reasoning behind each of these.

---

## What the build refuses to let through

None of these is something a reviewer could reliably catch by reading a diff. Each is a build
failure rather than a warning, because a warning in a build log is a thing nobody has read since
the second week.

| Guard | Where it runs | What it catches |
|---|---|---|
| `verifyModuleDependencies` | every library module | An edge the layering forbids — `:feature:cart` depending on `:feature:catalog`, `:core:*` reaching up into `:data:*` |
| `verifyApiPurity` | every Android library module | An `api` package importing Android, Compose, DI or another module's internals, which would stop the project splitting into `:api` and `:impl` modules |
| `verifyCodeRules` | every module | About fifty patterns that have broken apps built on the template: a Material import or a ripple, a raw `tween` or colour in a screen, an unkeyed lazy row, a value that changes every frame read in composition, a whole screen faded offscreen, copy typed into Kotlin, layout decided from orientation or device type. Each failure names its rule and the fix; `// allow(<rule>): <reason>` on the line above is the documented exception |
| `verifyManifestRules` | the app modules | An orientation or resize lock in the merged manifest, a launcher activity that would be recreated by a resize, a component that does not say whether it is exported, predictive back left off |
| `checkComposeStability` | `:core:designsystem`, `:core:ui`, `:core:navigation` | A composable parameter the compiler cannot prove stable, which reruns the composable whenever its caller passes an equal new instance |
| `verify<Variant>CarriesNoDevTooling` | release builds outside the dev flavour, with the inspector on | A production build that depends on a developer-tool module, directly or through another one |
| Android lint | every module | Compose's lint checks as errors, an orientation lock, an unprefixed resource, a string missing from a language |

All of them are Gradle tasks in `build-logic` and run as part of `./gradlew build`.

---

## Non-interactive use

The wizard produces a spec; the generator consumes one. Nothing in between needs a terminal, which
is what lets the website and its API run the same code.

```bash
# Record a spec once.
py create_project.py --save-spec myapp.json

# Regenerate from it, no questions asked.
py create_project.py --spec myapp.json --out ../MyApp --no-zip
```

The spec is plain JSON. Editing it by hand is a supported way to work — the same validation runs
either way, so a typo in the package name is rejected with the same message the wizard would give
you. `--set` changes one field on the way in: `--set android.min_sdk=28`.

Regenerating over a directory that already exists replaces it wholesale, so it asks first — and
refuses outright when there is nobody to ask, which is what `--spec` means. Pass `--force` when
replacing it is genuinely the intent.

Three flags answer questions without generating anything:

```bash
# Every feature, grouped as the website groups them, and what each one drags in.
py create_project.py --list-features

# Resolve the answers and say what would be written. Nothing is.
py create_project.py --spec myapp.json --dry-run

# The same three, for a program rather than a person.
py create_project.py --list-features --json
py create_project.py --spec myapp.json --dry-run --json
py create_project.py --spec myapp.json --out ../MyApp --no-zip --json
```

`--dry-run` is worth the two seconds before a long build: it names the features that were turned
on which nobody ticked. Asking for sign-in quietly brings the form state it is built on, which is
correct and is also the kind of thing better learned before the build than during it.

---

## Adding a feature module later

The generator scaffolds the modules you name at creation time. Afterwards, the same scaffolder is
a separate script:

```bash
cd generator
py add_feature.py orders --project ../MyApp --tab
```

It writes `:data:orders` and `:feature:orders` (a repository, a list and a detail screen with their
ViewModels and previews, a navigation key and tests), and makes the three edits that are otherwise
done by hand and forgotten one at a time: `settings.gradle.kts`, the app module's dependencies, and
— with `--tab` — the tab list. It reads the project's own package name out of
`app/build.gradle.kts` rather than asking, so running it in the wrong directory fails immediately
instead of writing files into the wrong namespace.

Multiple at once:

```bash
py add_feature.py orders invoices --project ../MyApp
```

A modular project can also take a `:domain:` module, for a rule several features share:

```bash
py add_feature.py --domain pricing --project ../MyApp
```

And the inverse, which undoes the same three edits:

```bash
py remove_feature.py orders --project ../MyApp --dry-run   # say what would go
py remove_feature.py orders --project ../MyApp             # ask, then do it
```

It deletes `:data:orders` and `:feature:orders` and takes their lines back out of all three
files. The one people forget by hand is the Gradle include, which fails the next sync with an
error about a missing project rather than about the directory they deleted. It refuses a module
the template itself ships: those are removed by generating without them, not by deleting a
directory that half the app still references.

---

## Working on the template itself

`templates/android/` is a normal Android project. Open it, build it, change it.

```bash
cd templates/android
./gradlew build                    # compile, test and lint everything
./gradlew :app:installDevDebug
./gradlew :catalog:installDebug
```

Run `./gradlew build` before you commit. It has caught real defects here that no amount of reading
would have: a library module calling `ConnectivityManager` without declaring the permission,
`Bitmap.CompressFormat.WEBP_LOSSY` used below the API level that has it, a locale read that never
recomposed when the user changed language.

[`AGENTS.md`](AGENTS.md) is the short version of this section for agents and people alike: how a
feature is added, the markers, and the checks a change must pass.

### Contrast

`PaletteContrastTest` checks every text-on-background pair in every surface tone, design style and
theme against WCAG AA. Contrast is the one design property that is objectively right or wrong,
invisible to whoever changed it, and decisive for whoever cannot read the result — a reviewer
looking at `Grey500 → Grey550` in a diff has no way to evaluate it. It found the success pill at
4.36:1 on the palette as it stood, which is why the green in this repository is three percent
darker than it was.

### Tokens

The design tokens live in `tokens/*.json` at the root: spacing, type, springs, the palette and the
four design styles. `generator/genkit/tokens` writes them into each template as its token file, and
a generated project gets its own copy with the user's brand worked in. After editing a token:

```bash
cd generator
python3 -m genkit.tokens --write     # rewrite the templates' token files
python3 -m genkit.tokens --check     # what the tests run: fails while a copy is stale
```

### Markers

The template carries `// <opt:feature>` marker comments that the generator strips. They are chosen
so the template still compiles with *every* feature on, which is what lets this repository's own
build prove the template works. The marker grammar, in full in `generator/genkit/core/markers.py`:

```kotlin
// <opt:push>   …  // </opt:push>      keep the block when `push` is on
// <opt:a|b>    …  // </opt:a|b>       keep it when either is on; `a&b` when both are
// <opt-else:push>                     directly after the block: kept when `push` is off,
//~ fallback()                         each line without its `//~ ` prefix
// </opt-else:push>
// <opt:!push>CODE                     emit CODE when `push` is off
// <generated:name>                    replaced by generated lines
```

The `opt-else` and `!` forms have to live inside a comment, because two live declarations of the
same class — one per branch — would break the template's own build. The `|` form exists for the
lines that several optional blocks share and none of them owns: the `javax.inject.Inject` import in
the application class is needed by analytics, push, feature flags and the offline queue, among
others, and is an unused import the moment all of them are off.

In XML the markers are comments on their own line between elements. A comment inside a tag is not
valid XML, so an optional attribute has to come from somewhere else — the language picker's
`android:localeConfig`, for instance, is generated by AGP from a Gradle setting that is itself
behind a marker. Markdown takes the same comments, so a generated project's docs describe only the
features it has.

### Keeping dependencies current

```bash
python3 tools/check_versions.py            # every version against the newest stable release
python3 tools/check_versions.py --apply    # update libs.versions.toml
```

It reads the version catalogue itself, so a new library needs nothing added to the script, and it
never offers an alpha, beta or release candidate.

### The generator's tests

```bash
cd generator
py -m unittest discover -s tests -t .
py -m tests.consistency --quick
```

About five hundred tests over the marker grammar, the rename, specs and their validation, the
tokens, the catalogue, every target's output, the scaffolder and the docs a generated project
carries (every rule, path, gate and code name they cite must exist in the project). The consistency
run generates the presets, every feature on its own and a pairwise set of feature combinations,
and checks each without Gradle. Neither needs anything beyond the standard library.

They are not sufficient on their own. The real test is generating both extremes and building
them, because that is what catches a marker left unbalanced in a file nobody thought about:

```bash
py create_project.py --spec tests/fixtures/v2/everything.json --out ../out/Full --no-zip --force
py create_project.py --spec tests/fixtures/v2/everything.json --set "features=[]" --out ../out/Bare --no-zip --force
(cd ../out/Full && ./gradlew build)
(cd ../out/Bare && ./gradlew build)
```

### Layout

```
templates/android/         the Android project. Open this in Android Studio
  features/*.toml          one file per optional feature: its title, group, requirements and the paths it owns
  presets.toml             lean, standard, everything
templates/multiplatform/   the Compose Multiplatform template, not released yet
templates/ios/             the SwiftUI template, not released yet
tokens/                    the design tokens every template is written from
generator/
  create_project.py        the wizard and the entry point
  add_feature.py           the scaffolder, for a project that already exists
  remove_feature.py        its inverse: delete a module and the three lines naming it
  generate_headless.py     JSON on stdin, a zip out. What the web API runs
  genkit/
    core/                  what a project is (spec), the feature manifest, markers, copy, rename, the catalogue, the wizard
    targets/android/       what is Android about generating: settings, tabs, the scaffolder, keystores, icons, modes
    targets/multiplatform/ and targets/ios/
    tokens/                the token files every target writes
  translations/            the template's copy in every language it ships
  tests/
tools/                     check_versions.py, update_android_skills.py, sync_check.py, feature_combinations.py
web/frontend/              the website
web/api/                   the Go service behind it
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

The downloadable-font provider could not resolve the family, or took longer than the launch could
wait. Check the spelling against the family page on fonts.google.com — it is case- and
space-sensitive, and a misspelling degrades quietly to the platform typeface rather than failing
loudly. It also needs Play Services, so it will not work on an emulator image without Google APIs.

**`Multiple shrunk-resources files found`**

You asked for ABI splits and an app bundle in the same invocation. Use
`distProdRelease -PabiSplits=true` for APKs and `distProdReleaseBundle` for the bundle, as two
commands.

**The generated project's first commit is missing `google-services.json`**

That is deliberate — it is git-ignored, as it should be, since it carries project ids tied to your
account. Each developer needs their own copy, the same as every other Android project.

---

## Licence

MIT, in [`LICENSE`](LICENSE). Google's Android skills are not covered by it: the copies the templates vendor under
`templates/android/.agents/skills/` keep their Apache License 2.0 (`LICENSE.txt` there), as do the skill names and
descriptions the pointers under `.agents/skills/` and `.claude/skills/` repeat. Optional features that bring
third-party code keep that code's own licence.
