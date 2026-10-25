# Working on this repository

Instructions for AI coding agents. This repository is the **generator**, not an app. If you were
pointed at a project that was *generated* from it, read that project's own `AGENTS.md` instead;
every generated project ships one.

```
templates/android/  a real Android app. Every optional feature is in it, fenced by markers
generator/          Python, standard library only: copies templates/android/, strips what was not chosen, renames it
web/frontend        the Next.js site that drives the generator
web/api             the Go service the site calls. It shells out to generator/generate_headless.py
```

## Generating a project for someone

When a user asks you to create an Android app with this, do not run the interactive wizard.
Write a spec and generate from it:

```bash
cd generator
python3 create_project.py --list-features --json        # every feature, its group and what it implies
python3 create_project.py --spec app.json --dry-run     # what would be written, and what got pulled in
python3 create_project.py --spec app.json --out ../MyApp --no-zip
```

A spec is plain JSON. Only `app_name` and `package_name` are required:

```json
{
  "app_name": "Trail Notes",
  "package_name": "com.example.trailnotes",
  "features": ["network", "auth", "settings", "profile", "search", "coil"],
  "feature_modules": ["trips"],
  "design_style": "Social",
  "motion_style": "Calm",
  "accent_colour": "#1F7A5C",
  "font_name": "Plus Jakarta Sans",
  "languages": ["es", "hi"]
}
```

- Pick features by what the app needs, not everything. `--dry-run` shows what they imply.
- `design_style` is one of `Utility`, `Social`, `Editorial`, `Playful`. `motion_style` is one
  of `Standard`, `Bouncy`, `Calm`, `Snappy`.
- `languages` adds translations besides English: `de`, `es`, `fr`, `hi`, `pt-BR`. Each lists
  itself in the app's language picker. Adding one means adding it to `generator/translations/`.
- `feature_modules` scaffolds a `:data:x` and `:feature:x` pair each. Names the template already
  uses (`auth`, `feed`, `profile`, `search` and so on) are refused.
- `"ui_components": false` (Android only; the CLI flag is `--no-ui-components`) is for a user who
  brings their own design system: the architecture, a theme skeleton in `:core:designsystem`, a
  placeholder route over each screen's ViewModel and every build guard, without components. The
  features that need components (`catalog`, `collections`, `datadisplay`, `formfields`, `pickers`,
  `liquidglass`) are left out with a note each, and `design_style` and `motion_style` stay at
  their defaults (a value given for either is noted and not used): the theme starts from neutral
  values for the design system you bring. Leave the field out for a project with components.
  `--list-features --json --no-ui-components` lists the features as such a project has them.
- Then build it (`./gradlew :app:assembleDevDebug`) before you tell the user it is ready.

After that, work in the generated project and follow its `AGENTS.md`. Its base components are a
starting point to reshape, not a kit to ship unchanged. For a new design-system component,
`python3 add_component.py Tag --project ../MyApp` (from `generator/`) writes `AppTag` and its test
from the two templates in that project's `docs/DESIGN-SYSTEM.md`, with or without UI components.

## Changing the template

The template must compile, test and lint with **every** feature on, because that is how this
repository proves it works. Optional code is fenced with markers the generator strips:

```kotlin
// <opt:push>   …  // </opt:push>      kept when `push` is on
// <opt:a|b>    …  // </opt:a|b>       kept when either is on
// <opt:!push>CODE                     CODE emitted only when `push` is off
// <generated:name>                    replaced by generated lines
```

In XML the same markers go in comments on their own line, **between** elements. A comment inside
a tag is not valid XML, so an optional attribute needs another route: a Gradle setting, a library
manifest, or a variant file under `generator/variants/`.

**Options.** A spec-level switch such as `ui_components` lives in
`templates/android/options/<key>.toml`: its default, the features it `disables` and the spec fields
it `hides` (each with a reason; the generator leaves them at their defaults, with a note when a spec
gave another value), the paths it `owns` (absent while it is off), its `variant`
directory, and `feature_copy`, `preset_copy` and `module_copy` for the kept features, presets and
feature modules whose usual words no longer hold (`test_android_noui.py` names every kept feature
whose UI the option takes and that still lacks its entry). Its key is a marker key like a feature's,
and the code with UI components is always
the live branch, so the raw template stays the project with components:

```kotlin
// <opt:ui_components>    …    // </opt:ui_components>    kept with UI components
// <opt-else:ui_components>                               directly after that block's end
//~ placeholder()                                         emitted without them, minus the `//~ `
// </opt-else:ui_components>
```

What only a project without components has is that `//~` fallback, or a whole file under
`generator/variants/noui/` at the template's path, which replaces or adds that file. A file only a
project with components has goes in `owns`. A file you add that reaches a component, a foundation
primitive or the navigation shell needs one of these three, or every project without components
stops compiling. `generator/tests/test_android_noui.py` checks what those projects hold and lack,
the consistency matrix generates its `noui-*` shapes in both its quick and its full run, and
`python3 tools/compare_generations.py --before <a checkout from before> --after .` proves that the
projects with components did not change by a byte.

A new optional feature needs all of:

1. Its code in `templates/android/`, behind markers, and any whole directories or files it owns.
2. A `Feature(...)` in `generator/genkit/spec.py`, listing `requires` and the `files` it owns.
3. A group and a headline in `generator/genkit/catalogue.py`. The catalogue test fails without them.
4. Its place in the presets, if it belongs in one.
5. A line in the root README's feature table. The website reads the catalogue by itself.

A new user-facing string in the template also needs a translation in every file under
`generator/translations/`. `tests/test_translations.py` names any that are missing.

## Google's Android skills

The Android template ships Google's Android skills in `templates/android/.agents/skills/` (unmodified, Apache-2.0,
`README.md` there gives the version). The Compose Multiplatform template has none yet: its `template.toml` does not
copy `.agents/skills/**` verbatim, so the generator would rename and marker-parse upstream's text, and the refresh
tool skips it. When you change a template, read the skill for the area first (`.agents/skills/<name>/SKILL.md` and
`.claude/skills/<name>/SKILL.md` here point at the Android copy) and the differences table in
`templates/android/AGENTS.md`. A change to a template's Compose UI is not done until it has been through the
`jetpack-compose-performance` audit, with the category's reference read before each fix. Never edit a vendored file:
refresh all of them with `python3 tools/update_android_skills.py <checkout> --tag <tag>` and review the diff. Every
project ships the whole set except the skills `LEFT_OUT` names in `templates/android/tools/android_skills.py`; a skill
added to the set needs its row in that template's CLAUDE.md and AGENTS.md table, and
`generator/tests/test_vendored_skills.py` fails otherwise.

## Keeping dependencies current

Every version lives in `templates/android/gradle/libs.versions.toml`, and only stable releases belong
there — no alpha, beta or rc.

```bash
python3 tools/check_versions.py            # each version against the newest stable release
python3 tools/check_versions.py --apply    # update the catalogue, then run everything below
```

After an update, read the build's warnings as well as its result: a new release often deprecates
something the template calls, and `./gradlew build --warning-mode all` is where that shows up.

## Verifying

Nothing is done until these pass. Run them and read the output.

```bash
cd generator && python3 -m unittest discover -s tests -t .       # seconds
cd templates/android && ./gradlew build                                  # the template, all features on

# both extremes of the generator, built for real
cd generator
python3 create_project.py --spec tests/spec_full.json    --out ../out/Full --no-zip --force
python3 create_project.py --spec tests/spec_minimal.json --out ../out/Bare --no-zip --force
(cd ../out/Full && ./gradlew build) && (cd ../out/Bare && ./gradlew build)

cd web/api && go vet ./... && go test ./...
cd web/frontend && npx tsc --noEmit && npx next build
```

Gradle runs on Android Studio's bundled JBR or any JDK 17+. Do not pin or download a JDK.

## The website must not over-promise

`web/frontend` renders its feature list from the generator's catalogue. Anything it says in its
own words (previews, copy, captions) must describe what the template actually does today. When
you remove or rename something in the template, search `web/frontend` for it too.

## Code quality

The same standard as the generated projects: comment why, not what; no comments narrating the
change ("added", "updated", "new"); no commented-out code; match the file you are in; keep the
diff to what was asked; never weaken a test to get to green.
