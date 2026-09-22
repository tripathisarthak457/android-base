# Working on this repository

Instructions for AI coding agents. This repository is the **generator**, not an app. If you were
pointed at a project that was *generated* from it, read that project's own `AGENTS.md` instead;
every generated project ships one.

```
template/     a real Android app. Every optional feature is in it, fenced by markers
generator/    Python, standard library only: copies template/, strips what was not chosen, renames it
web/frontend  the Next.js site that drives the generator
web/api       the Go service the site calls. It shells out to generator/generate_headless.py
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
  "font_name": "Plus Jakarta Sans"
}
```

- Pick features by what the app needs, not everything. `--dry-run` shows what they imply.
- `design_style` is one of `Utility`, `Social`, `Editorial`, `Playful`. `motion_style` is one
  of `Standard`, `Bouncy`, `Calm`, `Snappy`.
- `feature_modules` scaffolds a `:data:x` and `:feature:x` pair each. Names the template already
  uses (`auth`, `feed`, `profile`, `search` and so on) are refused.
- Then build it (`./gradlew :app:assembleDevDebug`) before you tell the user it is ready.

After that, work in the generated project and follow its `AGENTS.md`. Its base components are a
starting point to reshape, not a kit to ship unchanged.

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

A new optional feature needs all of:

1. Its code in `template/`, behind markers, and any whole directories or files it owns.
2. A `Feature(...)` in `generator/genkit/spec.py`, listing `requires` and the `files` it owns.
3. A group and a headline in `generator/genkit/catalogue.py`. The catalogue test fails without them.
4. Its place in the presets, if it belongs in one.
5. A line in the root README's feature table. The website reads the catalogue by itself.

## Verifying

Nothing is done until these pass. Run them and read the output.

```bash
cd generator && python3 -m unittest discover -s tests -t .       # seconds
cd template  && ./gradlew build                                  # the template, all features on

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
