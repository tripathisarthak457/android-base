# Audit: generator (Python) and tools/

Scope: `android-base/generator/**` (`create_project.py`, `generate_headless.py`, `add_feature.py`,
`remove_feature.py`, `genkit/*.py`, `tests/*`, `variants/`, `translations/`) and `android-base/tools/**`.
Branch `overhaul/multiplatform-v2`, HEAD `99c4edf`. Nothing in the three source repositories was modified.
Gradle was not run.

## 1. Executive summary

The marker engine and the template are in good shape for the feature matrix. Across 159 generated
combinations (every preset, every single feature on its own, every feature removed from "all", the 18
pairwise sets from `tools/feature_combinations.py`, and 60 random subsets, each with all 5 languages)
no project referenced a stripped module, class, resource, convention plugin, version-catalogue alias or
Hilt binding. All the hard breakage I found is in the paths around the matrix: naming, scaffolding,
icons, signing, and the CLI and headless front ends.

The most serious findings, all reproduced:

1. **The generator renames its own output twice.** Scaffolded modules and `<generated:…>` lines are
   written with the user's package and then put through the template rename again. Any package or
   module name that contains a template token breaks the project. Examples: `com.example.databaseapp`
   and `com.firebaseapp.notes` crash generation. `com.base.apple` produces `com.base.applele`. Module
   `database_app` under app "Store" becomes `:data:datastore`.
2. **`--icon` writes malformed adaptive-icon XML.** The `re.sub` replacement drops its capture
   groups, so every project generated with an icon fails resource compilation. This has been there
   since the first commit.
3. **Some valid specs produce an app that crashes on launch.** If no tab feature is on and there are
   no feature modules (for example `features: ["auth"]`), `AppDestinations.start` is
   `?: error(...)`. `MainActivity` reads it on startup. CI never builds this case.
4. **Validation lets broken names through and blocks good ones.** Module names that are Kotlin
   keywords (`in`, `fun`, `object`, `null`, `class`) are accepted. Packages are checked against
   *Python's* keyword list, so `com.global.bank` and `com.example.pass` are rejected while
   `com.example.null`, `com.example.true` and `java.*` are accepted.
5. **Signing can break silently.**
   - If the store and key passwords differ, keytool ignores the key password for PKCS12, but
     `keystore.properties` records it. Release signing then fails with `UnrecoverableKeyException`.
   - Non-ASCII passwords are written as UTF-8 but read back as ISO-8859-1, which gives "keystore
     password was incorrect".
6. **`add_feature.py` breaks multi-language projects.** It adds an untranslated module, which lint
   (`MissingTranslation`) fails. It also puts the new tab first, which silently changes the app's
   start destination.
7. **The CLI has three traps.**
   - `--json` prints the human summary before the JSON.
   - `--out` means the parent folder with zip output but the project folder with `--no-zip`. With
     `--force` this deleted a sibling project in my test.
   - The wizard only validates at the end, so one typo in the first answer throws away every answer.

Smaller items:
- Generated apps carry dead weight: about 43 unreferenced version-catalogue entries in a lean
  project, two never-registered convention plugins, and leftover validation strings and drawables.
- `database` pulls in Ktor networking only because of how a convention plugin is gated.
- `render.py` is a ~800-line god module with silent-no-op regex appliers tied to Android file paths.
  That will get in the way of the planned Compose Multiplatform and SwiftUI generators.
- The tests have real gaps: a mid-file `unittest.main()`, unused fixtures, no tests for icons,
  headless or `add_feature`, and CI never builds the `standard` preset or the `firebase`-off
  variants.

## 2. How this was tested

All scratch tooling lives in `/home/crazyhusir/Documents/projects/tools/.overhaul/scratch-gen/`:

| Script | What it does |
|---|---|
| `gen.py` | Calls `genkit.build.build()` straight into `.overhaul/out/gen-<name>` (no zip, no Gradle). |
| `markers.py` | Checks every template file's markers: balance, unknown feature names, more than one marker per line, code sharing a line with a marker. |
| `check.py` | Static checks on a generated project. Leftover markers and identity tokens. `settings.gradle.kts` includes against directories, and orphan modules. `project(":x")` references. Imports of the project's own packages against declared symbols. Unused imports. References to top-level symbols that exist in the full-feature reference but were stripped. `libs.*` aliases and `libs.library("…")` against the stripped TOML. Convention plugin ids and `implementationClass`. Manifest components. `R.x.y` and `@x/y` resources. Translation files against their source strings. |
| `hilt.py` | Every `@Inject` constructor parameter, `@Provides`/`@Binds` parameter and injected field needs an `@Inject` class or a `@Provides`/`@Binds` return type (including `@IntoSet`). |
| `matrix.py` / `hiltmatrix.py` | The 159-combination matrix described above. |
| `names.py` | 43 hostile app, package and module names. |
| `unusedres.py` | Resources nothing references. |

I sanity-checked `check.py` by deleting `core/analytics` and `AppNetworkImage.kt` from a copy of the
full build. It reported the dangling imports, the dangling symbols and the broken `settings.gradle.kts`
include.

Unit tests: `cd generator && python3 -m unittest discover -s tests -t .` gives **116 tests, OK, 1.95 s**.
It prints one stray warning ("No AppDestinations.kt at …") from `RemoveFeatureTest`.

### Matrix result

| Class of check | Result over 159 combinations |
|---|---|
| Leftover `<opt:` / `</opt:` / `<generated:` | 0 |
| `com.base.app`, `BaseApp`, `base_app`, `baseapp` left in output | 0 (with a neutral package and name) |
| `include` without a directory, or a module dir not included | 0 |
| Import of a project symbol that was stripped | 0 |
| Same-package reference to a stripped symbol | 0 |
| `libs.*` or `libs.library("…")` pointing at a stripped TOML entry | 0 |
| Convention plugin applied but not registered | 0 |
| Manifest component missing | 0 |
| Missing resource | 0 |
| Hilt binding missing | 0 |
| Translation set differs from source strings | 0 |
| Dead convention plugin file (never registered) | 70 (firebase off), 63 (room off) |
| Unused imports produced by stripping | 5 distinct, in 10–34 combinations each (§4.12) |

Template marker balance (`markers.py`): 0 issues across 47 files and 518 markers.

## 3. High-severity findings

### 3.1 The generator renames its own output twice, so names containing a template token break the project

- `genkit/build.py:47-49` calls `scaffold.write_feature_modules(project, spec)` **before**
  `render.rewrite_all(...)`.
- `genkit/render.py:248` does `rename(strip_markers(original, enabled, generated), spec)`. That
  applies `rename()` to:
  - the files the scaffolder already wrote with `spec.package_name`;
  - the `<generated:…>` lines, which also contain the user's package and module names.
- `rename()` (`render.py:115-126`) is a chain of `str.replace` calls with no word boundaries:
  `com.base.app` → `com/base/app` → `BaseApp` → `base_app` → `baseapp`. Each replacement also
  rewrites text that an earlier one inserted.
- Separately, the `apply_*` steps look files up by `spec.package_path` (`render.py:286`, `:316`,
  `:336`, `:686`, `:780`), while directories were renamed with `rename()`. When the two disagree the
  build aborts.

Reproduced:

| Input | Result |
|---|---|
| package `com.example.databaseapp` | `RenderError: AppConfig.kt not found where expected: …/com/example/databaseapp/buildlogic/AppConfig.kt` |
| package `com.firebaseapp.notes` | same `RenderError` |
| package `com.base_app.demo` | same `RenderError` |
| package `com.base.apple` + module `orders` | 12 scaffolded files say `package com.base.applele.feature.orders` (directory is `com/base/apple`) → compile errors |
| package `com.base.app.demo` + module | `package com.base.app.demo.demo.feature.orders` |
| module `database_app`, app "Store" | `settings.gradle.kts` gets `include(":data:datastore")` / `include(":feature:datastore")`; dirs are `data/database_app`, `feature/database_app` |
| module `baseapp` with `languages` | `TranslationError: No de translation for trailnotes_title, …` (misleading) |
| module `baseapp` without languages | `include(":feature:trailnotes")` → dir `feature/baseapp` orphaned |
| app name `Xbaseapp` | `rootProject.name = "Xxbaseapp"`, class `XxbaseappApplication`, while `AppConfig.APP_NAME = "Xbaseapp"` and README says `Xbaseapp` |
| app name `Baseapp X` | notification channels `baseappx_x_default` (snake form re-renamed) |

`databaseapp`, `firebaseapp`, `database_app` and `firebase_app` are ordinary names, so this is not
a theoretical edge.

**Fix.** Rename only text that came from the template, in a single pass, before inserting anything
generated:

```python
_TOKENS = re.compile(r"com\.base\.app|com/base/app|BaseApp|base_app|baseapp")

def rename(text, spec):
    mapping = {"com.base.app": spec.package_name, "com/base/app": spec.package_path,
               "BaseApp": spec.pascal_name, "base_app": spec.snake_name, "baseapp": spec.lower_name}
    return _TOKENS.sub(lambda m: mapping[m.group(0)], text)   # single pass: inserted text is never re-scanned

# render.rewrite_all: rename first, then resolve markers, so generated lines are never renamed
rewritten = collapse_blank_runs(strip_markers(rename(original, spec), enabled, generated))

# build.py: scaffold after the rewrite pass (scaffold output is already final)
render.rewrite_all(project, spec, scaffold.generated_blocks(spec))
scaffold.write_feature_modules(project, spec)
```

Add a regression test that generates with package `com.example.databaseapp`, package
`com.base.apple`, module `database_app` and app `Xbaseapp`, then asserts no `RenderError` and that
every `package` line matches its directory.

### 3.2 `--icon` writes malformed adaptive-icon XML

`genkit/icons.py:162-166`:

```python
updated = re.sub(
    r'(<monochrome[^>]*android:drawable=")@drawable/[A-Za-z0-9_]+(")',
    r"@drawable/ic_launcher_monochrome",   # drops \1 and \2
    text,
)
```

I ran `_point_monochrome_at_the_template_mark` on a copy of
`template/app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml`. Both files came out as:

```xml
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    @drawable/ic_launcher_monochrome />
</adaptive-icon>
```

`xml.etree` says `not well-formed (invalid token): line 5, column 4`. aapt2 fails the same way, so
every project generated with `--icon` fails resource linking. VitFleet was generated with `--icon` (it
has `play-store-icon.png`). Its committed `ic_launcher.xml` has the *intended* form
(`<monochrome android:drawable="@drawable/ic_launcher_monochrome" />`), which the current code cannot
produce. The bug dates from `be9c689`, the first commit. No test covers icons, and Pillow is absent
both on this machine and in CI.

**Fix.** Use `r"\g<1>@drawable/ic_launcher_monochrome\g<2>"`. Add a test for
`_point_monochrome_at_the_template_mark` (it needs no Pillow) that parses the XML afterwards. Install
Pillow in the CI generator job and run one `--icon` generation in the Gradle matrix.

### 3.3 Some valid specs generate an app that crashes on launch

`genkit/scaffold.py:46-60`: with no `sample`, `paging`, `search`, `profile` or `settings`, and no
feature modules, the start fallback is
`?: error("Set a start destination: name your first feature's nav key here.")`.

Generated from `{"features": ["auth"]}` (`.overhaul/out/gen-crash-auth`):

```kotlin
val tabs: List<ShellTab> = listOf(
    // Settings sits last because that is where people look for it.
)
val start: AppNavKey = tabs.firstOrNull()?.key
    ?: error("Set a start destination: name your first feature's nav key here.")
```

`MainActivity.kt:79` reads `AppDestinations.signIn` at startup, which initialises the `object`, so
`start` throws (`ExceptionInInitializerError`). `FeatureBindingsModule.kt:18` also reads
`AppDestinations.start` while building the Hilt graph.

The website can produce this spec, for example by unticking `sample` from Lean, or by picking
only auth, onboarding or settings-less combinations. CI never builds it: every row of the Android
matrix adds `feature_modules: ["orders"]` (`.github/workflows/ci.yml:104-109`). The unit test
`test_with_neither_the_start_destination_says_what_to_do` asserts the crash path rather than
preventing it.

**Fix.** Either:
- refuse the spec in `ProjectSpec.validated()` ("pick at least one screen: sample, a feed, search,
  profile, settings or a feature module"); or, better,
- auto-scaffold a `home` feature module when nothing provides a destination.

Then add `features=[]`, `feature_modules=[]` to the CI Gradle matrix.

### 3.4 Kotlin keywords are accepted as module names

`genkit/spec.py:937-947` (`validate_module_name`) checks only `^[a-z][a-z0-9_]*$` and
`RESERVED_MODULE_NAMES`. All of these were accepted and generated: `in`, `fun`, `object`, `null`,
`class` and `default`. Output: `package com.example.trail.feature.in`,
`import com.example.trail.feature.in.InListKey` and `package com.example.trail.data.null`. These are
Kotlin compile errors because hard keywords cannot be package segments without backticks.

**Fix.** Reuse one JVM/Kotlin identifier check for package segments and module names (§3.5).

## 4. Medium-severity findings

### 4.1 Package-name validation uses Python's keyword list

`genkit/spec.py:930`: `if keyword.iskeyword(segment) or segment in _JAVA_KEYWORDS`. `keyword` is
Python's module.

- **False rejections** (valid Java/Kotlin package segments): `and`, `async`, `await`, `def`, `del`,
  `elif`, `except`, `from`, `global`, `lambda`, `nonlocal`, `not`, `or`, `pass`, `raise`, `with`,
  `yield`. Reproduced: `com.global.bank` and `com.example.pass` give "is a reserved word".
- **False acceptances.** `_JAVA_KEYWORDS` (`spec.py:1005-1012`) misses `null`, `true`, `false`,
  `typealias` and `typeof`. `com.example.null`, `com.example.true` and `com.example.typealias` all
  generated, producing `package com.example.null` (compile error).
- `java.trail.notes` is accepted. ART refuses to load classes in `java.*` ("Prohibited package
  name").

**Fix.**

```python
_KOTLIN_HARD = {"as","break","class","continue","do","else","false","for","fun","if","in","interface",
                "is","null","object","package","return","super","this","throw","true","try","typealias",
                "typeof","val","var","when","while"}
_JAVA_RESERVED = {...Java keywords..., "true", "false", "null", "_"}
RESERVED_SEGMENTS = _KOTLIN_HARD | _JAVA_RESERVED
# validate_package_name: also reject first segment "java", and len(package) > 100
# validate_module_name: reject RESERVED_SEGMENTS too
```

Drop the `import keyword`.

### 4.2 PKCS12 keys whose key password differs from the store password cannot be used for signing

- `render.py:485-496` runs keytool with `-storetype PKCS12 -storepass S -keypass K`. keytool prints
  "Different store and key passwords not supported for PKCS12 KeyStores. Ignoring user-specified
  -keypass value." The generator never shows this, because output is only read on failure.
- `write_keystore_properties` (`render.py:406-422`) then writes `keyPassword=K`.

Reproduced (`gen-keys2`): loading the generated `keys/prod.jks` with store password `storepass1` and
recovering the key with `keypass222` gives
`UnrecoverableKeyException: Get Key failed: Given final block not properly padded`. This is the
failure `assembleProdRelease` would hit.

Who is affected:
- The CLI wizard explicitly asks "Key password" separately (`prompts.py:465`).
- Spec files can set it.
- The website sends the same value for both (`web/frontend/components/configurator.tsx:309-310`), so
  web users are not affected.

**Fix.** For generated PKCS12 keys, reject `key_password != store_password` in `validate_keystore`
(with the reason), or always write `keyPassword` equal to `storePassword`. Also surface keytool
warnings.

### 4.3 Non-ASCII keystore passwords are written in one encoding and read in another

- The generator writes `keystore.properties` as UTF-8 (`render.py:422`).
- The template's `Signing.kt:28-29` reads it with `Properties().load(propertiesFile.inputStream())`,
  which is ISO-8859-1.
- `validate_keystore` allows any printable character.

Reproduced (`gen-keys`) with password `pässwörd1`:
- Java's `Properties.load` returns `pÃ¤sswÃ¶rd1`.
- `keytool -list -storepass pÃ¤sswÃ¶rd1` says "keystore password was incorrect".
- The real password works.

**Fix.** In `_properties_value`, escape every non-ASCII code point as `\uXXXX`, which
`Properties.load` decodes in any encoding. Better still, also change the template to
`propertiesFile.reader(Charsets.UTF_8).use { load(it) }`.

### 4.4 `add_feature.py` breaks multi-language projects and changes the start destination

Tested on a copy of the full build, which has 5 languages (`.overhaul/out/addtest`).

- **No translations.** `feature/wallet/src/main/res/` has only `values/`, while sibling modules have
  `values-de|es|fr|hi|pt-rBR`. `genkit/translations.py` states "Lint fails a build that ships a
  language with a string missing", and `MissingTranslation` is error severity. So the first
  `./gradlew build` after `add_feature.py` fails. `add_feature.py` never calls
  `translations.apply`, and it cannot know the languages without reading `generator-spec.json`.
- **The tab goes first.** `register_tabs` (`add_feature.py:191-195`) inserts right after
  `listOf(`. The tab list becomes `WalletListKey, SampleListKey, FeedKey, …`. Because
  `start = tabs.firstOrNull()?.key`, the new module silently becomes the cold-start destination. At
  generation time, module tabs go *after* sample, feed and search (`<generated:shell-tabs>`).
- **No record update.** `generator-spec.json` and the README are not updated, by either
  `add_feature.py` or `remove_feature.py`. Regenerating from the record drops added modules and
  resurrects removed ones.
- **No tab limit.** Nothing caps the tab count. The full build with 2 modules has 7 bottom tabs, which
  goes against the "Apple-level" goal (HIG: at most 5).

**Fix.** Have `add_feature.py`:
- read `generator-spec.json` for `languages` and write `values-<lang>/strings.xml` from
  `translation["scaffold"]`;
- insert tabs before the `<generated:shell-tabs>` position. Keep an `// app-tabs:end` anchor in the
  template, or insert after the last `ShellTab(key = …ListKey` line;
- update `feature_modules` in `generator-spec.json`.

### 4.5 `create_project.py --json` does not print valid JSON

`create_project.py:111` calls `prompts.summarise(spec)` unconditionally, before the `args.json`
branch at `:141`. Reproduced: stdout starts with `\nSummary\n───────\n App Json Check …`, and
`json.load` fails with "Expecting value: line 2 column 1".

AGENTS.md tells agents to use `--json`, and `load_spec` sends its notes to stderr precisely "because
--json promises that stdout is JSON and nothing else" (`create_project.py:316`).
`confirm_overwrite` also prints to stdout.

**Fix.** Skip `summarise` and every other human print when `args.json` is set, or send them to
stderr.

### 4.6 `--out` means two different things, and `--force` deletes the wrong one

`create_project.py:239-246`:
- zip output: `--out DIR` is the **parent** (`DIR/<Name>.zip`);
- `--no-zip`: `--out DIR` is the **project directory itself**.

`build.py:83-84` then `shutil.rmtree(destination)`.

Reproduced: `--out parentdir --no-zip --force`, where `parentdir/ExistingProjectA/important.txt`
existed. `ExistingProjectA` was deleted and replaced by the generated tree. The same `--out` in zip
mode left it alone and wrote `parentdir/JsonCheck.zip`. If the destination is an existing *file*,
`rmtree` raises `NotADirectoryError`, an unhandled traceback.

**Fix.** Make `--out` always the project path. Give zip its own flag, or derive `.zip` from the
suffix. Refuse `--force` when the destination contains a `settings.gradle.kts` for a different
`rootProject.name`, or when it is `$HOME` or a filesystem root.

### 4.7 The wizard validates only at the end, and asks about URLs using the unresolved feature set

- `genkit/prompts.py:1-4` says the wizard "re-asks whatever `ProjectSpec.validated()` rejects".
  It does not. `run_wizard` (`:494-539`) collects every answer, then calls `spec.validated()` once.
  `create_project.py:95-102` turns the `SpecError` into exit 1. An invalid app name (`Café`, `2048`),
  a version name `1.0`, an accent `#12345`, a URL with a space, or a typo in a font name each throw
  away ~40 answers.
- `ask_environments(features)` (`:243-245`, called at `:499`) checks `"network" in features` on the
  *unresolved* selection. Choosing `auth` or `sample` individually (both require network) skips the
  backend-URL questions, and the project ships `dev.example.com`.
- Out-of-range menu choices are silently clamped (`:342`, `:354`).
- `suggested_package = "com.example." + re.sub(...) or "com.example.app"` (`:159`): `+` binds before
  `or`, so the fallback never applies.

**Fix.** Validate each answer as it is typed. The validators already exist: `validate_package_name`
is used per field at `:163`. Add `validate_app_name`, `validate_hex`, `validate_url` and
`validate_font_name`, and call `resolve_features(features)` before the dependent sections.

### 4.8 `generate_headless.py` does not always fail with JSON

The module docstring promises "Anything that goes wrong exits non-zero with `{"error": "…"}` on
stdout". Reproduced counter-examples:

| Body | Result |
|---|---|
| `[]`, `"x"` | `AttributeError` at `:82` (`payload.items()`), traceback on stderr, empty stdout |
| `null` | `TypeError` at `:77` (`set(payload)`) outside any `try` |
| `{"app_name": 5, …}`, `{"package_name": 12}`, `{"accent_colour": 123456}`, `{"api_base_urls": ["x"]}` | `AttributeError` from `validated()`; only `SpecError` and `TypeError` are caught at `:95-100` |
| `{"feature_modules": "home"}` | **succeeds** with modules `h`, `o`, `m`, `e` (`:85-87` only converts lists, and a string is iterable) |
| `{"features": "network"}` | "Unknown feature(s): e, k, n, o, r, t, w." |
| `{"haptics_enabled": "false"}` | succeeds with haptics **on** |
| `{"version_code": 1.5}` | succeeds; `AppConfig.VERSION_CODE = 1.5` (Kotlin `Double` assigned to `versionCode`) |
| `{"min_sdk": "26"}` | "minSdk 26 is outside the supported range" (misleading) |

The Go API decodes into a typed `generate.Request` (`web/api/internal/generate/generate.go:19-45`),
so the website cannot send most of these. This is defence in depth, but the script is also the
documented machine interface.

**Fix.** Wrap all of `main` in one `try`. Check `isinstance(payload, dict)`, and type-check each
field against a small schema (`str`, `int` but not `bool`, `list[str]`, `dict[str, str]`, `bool`)
before building `ProjectSpec`.

### 4.9 Keystore passwords are visible to other local users during generation

`render.py:489-490` passes `-storepass` and `-keypass` on the keytool command line, which any local
user can read via `/proc/<pid>/cmdline` or `ps`. The headless docstring makes a point of taking the
spec on stdin "so the API never has to write the user's answers — which now include keystore
passwords — to a path that something else could read".

Relatedly, `create_project.py --save-spec` (`:329-336`) writes keystore passwords in plain text,
while `record.py` deliberately strips them ("a keystore password in a repository is a leaked
password").

**Fix.** Use `-storepass:env GEN_STOREPASS -keypass:env GEN_KEYPASS` with
`env={**os.environ, ...}` on the subprocess. Strip or warn about passwords in `save_spec`.

### 4.10 `database` pulls in networking for no functional reason

- `spec.py:316`: `database` → `requires=("room",)`.
- `room` → `requires=("network",)`.
- The only reason is that the `androidRoom` convention plugin registration is fenced with
  `<opt:room>` (`template/build-logic/convention/build.gradle.kts:81-87`).
- `core/database/build.gradle.kts` depends only on `:core:common` and `com.base.app.android.room`.

So an offline-only app that wants a local database must ship Ktor, the response cache and the
request queue. That runs against the "lighter apps" goal.

**Fix.** Fence the registration with `<opt:room|database>`, give the plugin file the same ownership
(§4.11), and drop `requires=("room",)` from `database`.

### 4.11 Generated apps carry code and dependencies their features do not use

- **Dead convention plugins.**
  - `AndroidFirebaseConventionPlugin.kt` is copied when firebase is off (70 of 159 combinations).
  - `AndroidRoomConventionPlugin.kt` is copied when room and database are off (63 combinations).
  - Their registrations are stripped, but the files are not in any `Feature.files` (`spec.py:161-180`,
    `:106-119`).
- **Version catalogue.** `template/gradle/libs.versions.toml` has only 4 markers. In the bare
  project, **43 of 93 libraries and 5 of 13 plugins are unreferenced**: ktor-*, coil-*, paging-*,
  work-*, firebase-*, play-*, credentials, glance, lottie, leakcanary, benchmark, robolectric,
  google-services, licensee, baselineprofile and others. Even the full build has 6 dead library
  entries and the `kotlin-android` plugin (`androidx-lifecycle-runtime-ktx`, `androidx-work-testing`,
  `androidx-paging-testing`, `androidx-test-core`, `androidx-test-rules`, `mockk-android`). That is a
  template issue, noted for the template auditor.
- **Leftover resources.** With `forms` off, the 11 `validation_*` strings and plurals stay in
  `core/common/src/main/res/values/strings.xml`, and get translated into every language. With `push`
  off, `app/src/main/res/drawable/ic_notification.xml` stays.

**Fix.**
- Add `build-logic/convention/src/main/kotlin/AndroidFirebaseConventionPlugin.kt` to `firebase.files`.
- Make `AndroidRoomConventionPlugin.kt` owned by "room and database both off". The `files` model
  cannot express that, so add an `owned_when_all_off` field, or generalise ownership to a marker
  expression.
- Fence the TOML entries per feature.
- Fence the `validation_*` strings with `<opt:forms>`.
- Add `ic_notification.xml` to `push.files`.

## 5. Low-severity findings

### 5.1 Modules whose Pascal names collide produce conflicting imports
`scaffold.pascal()` (`scaffold.py:10-12`) maps both `order_history` and `order__history` to
`OrderHistory`. Both are accepted (uniqueness is checked on the raw names, `spec.py:834`).
`AppDestinations.kt` then imports `OrderHistoryListKey` and `R as OrderHistoryR` twice, which is a
compile error. **Fix:** reject `__`, trailing `_`, and Pascal-form collisions.

### 5.2 No length limits
A package of 8 segments × 28 characters crashes with
`OSError: [Errno 36] File name too long: …/core/database/schemas/<package>.core.database.AppDatabase`.
The Room schema directory is named after the fully qualified class name. `create_project.py`
catches only `RenderError` (`:137`), so the CLI shows a traceback. Windows' 260-character MAX_PATH
is much tighter, because deep Compose paths like
`core/designsystem/src/main/kotlin/<pkg>/core/designsystem/component/navigation/AppNavigationRail.kt`
add about 110 characters.

There is no cap on `app_name` either. Its Pascal form becomes `AppLogger.DEFAULT_TAG`
(`core/common/.../AppLogger.kt:8`). With `min_sdk` 24 or 25 and a name over 23 characters, lint's
`LongLogTag` may apply (plausible; not built). **Fix:** cap the package at about 100 characters,
`app_name` at about 30, and catch `OSError` in the CLI.

### 5.3 App names are limited to ASCII
`_APP_NAME = ^[A-Za-z][A-Za-z0-9 ._-]*$` (`spec.py:702`) rejects "Mom's Kitchen", "Tom & Jerry",
"Café Olé", "Rocket 🚀", "2048 Game" and `Say "Hi"`. Each was reproduced as a `SpecError`. The
restriction exists because `apply_app_name` (`render.py:376-403`) and the README insert the name
without escaping.

**Fix:** separate `display_name` (any Unicode, escaped with `translations._escape` for XML and aapt)
from the identifier forms, which can be derived via NFKD → ASCII with a fallback
(`Café` → `Cafe`, `2048 Game` → `App2048Game`). All-caps names also give ugly snake forms
(`NASA Tracker` → `n_a_s_a_tracker`, used for channel ids and the datastore key alias, `spec.py:784-786`).

### 5.4 The marker engine is silent about bad markers
`strip_markers` (`render.py:72-112`):
- ignores a close tag that does not match the top of the stack (`:85-87` just `continue`s);
- never reports blocks left open at EOF;
- accepts unknown feature names (a typo like `<opt:pushh>` is always stripped);
- does not support `<opt:!a|b>`: `_INLINE_ELSE` (`:38`) excludes `|`, so the line is kept *with* the
  marker.

The template is clean today (`markers.py`: 0 issues), but nothing enforces it. Only the Gradle CI job
would notice, and only for the 4 combinations it builds. **Fix:** raise `RenderError(f"{path}:{line}:
…")` on mismatch or leftovers, validate names against `FEATURES_BY_KEY`, and add a unit test that
runs the same scan over `template/`.

### 5.5 The generator reformats files it had no reason to touch
`collapse_blank_runs` (`render.py:129-133`) runs on every text file (`:248`). 114 marker-free files
differ from `rename(template)` in the full build. For example, the blank line after
`sealed interface AppResult<out T> {` is removed, as are blank lines in `catalog/build.gradle.kts`.
This contradicts the stated principle in `test_a_blank_line_after_an_opening_parenthesis_is_left_alone`
("the generator reformatting code nobody asked it to touch"). It also makes the README's "regenerate
and compare" workflow noisy. **Fix:** collapse only when `strip_markers` actually removed lines, and
only around the removed region. Better still, have `strip_markers` swallow one adjacent blank line
when it drops a block.

### 5.6 Line endings are rewritten
The template's `gradlew.bat` is CRLF (94 CRs) but comes out LF, because text is read with universal
newlines and written with `write_text` (`render.py:189`, `:234`, `:255`). On Windows the reverse
happens: `write_text` writes CRLF, so a CLI user on Windows gets a CRLF `gradlew` inside the zip.
That is the "bad interpreter" failure `template/.gitattributes` documents. An LF `gradlew.bat` is a
known `cmd.exe` label-parsing hazard (plausible; not reproduced). **Fix:** use
`read_text(encoding="utf-8", newline="")` and `write_text(..., newline="")` everywhere, so line
endings pass through unchanged.

### 5.7 Unused imports and orphan comments left by stripping
Kotlin does not fail on these, but they are visible slop in a fresh project:

| Leftover | When |
|---|---|
| `AppDestinations.kt`: `import <pkg>.R` | 34 combinations with no tab feature |
| `AppDestinations.kt`: `import …AppIcons` | 30 combinations |
| `ProfileScreen.kt`: `import …layout.Row` | `profile` on, `media` off |
| `FeatureBindingsModule.kt`: `import …ui.AppDestinations` | `settings` on, auth/onboarding/firebase/flags/googlesignin/push/search off |
| `KtorNetworkClientTest.kt`: `JsonPrimitive`, `buildJsonObject` | `network` on, `room` off |
| `// Settings sits last because that is where people look for it.` | `settings` off |
| `// ── tooling ──…` header with nothing under it in `settings.gradle.kts` | every combination without benchmark/catalog |
| `resolveSignIn()` reduced to `return start` | `auth` off |

**Fix:** move the imports into the matching `<opt:>` blocks in the template. The `check.py`
unused-import rule could become a unit test.

### 5.8 Exceptions the CLI does not catch
`create_project.py:95-105` catches only `SpecError`; `:129-139` catches only `RenderError`. Each of
these escapes as a traceback:
- `load_spec` with an unknown or misspelt key (`TypeError` from `ProjectSpec(**data)`);
- malformed JSON (`JSONDecodeError`);
- a missing spec file (`FileNotFoundError`);
- `TranslationError` (a `ValueError`, `translations.py:27`);
- `OSError` from §5.2.

`load_spec` also has the string-as-list problem (`"features": "network"`).

### 5.9 The "Reuse the dev key for staging?" prompt does not reuse the key
`prompts.py:444-447` copies the dev `KeystoreSpec` under the name `staging`. `generate_keystores`
(`render.py:472-505`) then runs keytool again, producing a different key pair with the same alias
and password (`tests/spec_keys.json` does the same). It is harmless because the application ids
differ, but the prompt is untrue. **Fix:** copy `dev.jks` to `staging.jks`.

### 5.10 Generated README mistakes (`genkit/readme.py`)
- `:128-136` says `py add_feature.py orders --project . --tab` and `py remove_feature.py …`. Those
  scripts live in the generator repository, not in the project, so the commands fail as written. The
  template's own AGENTS.md correctly says `python3 path/to/generator/add_feature.py`.
- The "Structure" block (`:79-85`) lists `catalog/` and the onboarding and sign-in gates (`:98-102`)
  even when those features are off.
- `py` (the Windows launcher) and `python3` are mixed.
- `:181-184` holds four blank lines left by a deleted note.

### 5.11 Presets and defaults disagree
- The "Everything" preset leaves out 7 features: applock, composemetrics, coverage, database,
  depsanalysis, licenses and playstore.
- The per-feature `default=True` set differs from the `standard` preset, which the site uses as its
  default (`catalogue.py:197`). `analytics` is on by default but not in standard; `browser`,
  `language` and `profile` are in standard but off by default. The wizard's "Choose individually"
  path and the site's default therefore produce different projects.
- `catalogue.py:185-198` duplicates defaults that already exist as `ProjectSpec` field defaults and
  `render.TEMPLATE_ACCENT`.
- `catalogue.py:208-221` and `prompts.FONT_SUGGESTIONS` are two different font lists.

### 5.12 Dead code and stale or contradictory comments
- `render.RenderResult` (`render.py:59-64`) is never constructed. It is used only as a wrong type
  hint in `create_project.report` (`create_project.py:344`), which receives a `build.BuildResult`.
- `Feature.implied_only` (`spec.py:75-76`) is false for every feature, yet 7 call sites handle it
  (catalogue, prompts, tests, CI, `feature_combinations.py`).
- `_TEMPLATE_ONLY = {"catalog/src/test/screenshots"}` (`render.py:52`) points at a directory that no
  longer exists.
- There are two executable-bit tables: `_EXECUTABLE_NAMES` and `_EXECUTABLE_SUFFIXES`
  (`render.py:202-203`), and `_EXECUTABLE` (`render.py:516`). The zip ignores `.sh`, and the template
  has no `.sh` files.
- `remove_feature.normalise` (`remove_feature.py:93-101`) keeps a branch its own comment calls
  "Unreachable", and duplicates `add_feature.normalise` (`add_feature.py:106-109`).
- `generate_headless.py:33` and `:104-106` refer to explanations "in the module docstring" that it
  does not contain, and to "the same nine render steps", while `build()` has more.
- `prompts.ask_password` docstring (`prompts.py:128-131`): "Read visibly rather than through
  getpass. A hidden prompt is the right default…" contradicts itself, and the passwords are echoed.
- `spec.py:640-651`: the section header says "Signing", but the section opens with
  `MOTION_STYLES`/`DESIGN_STYLES`.
- `tests/test_generator.py:182`, `:196` and `:216` justify behaviour by "ktlint fails the build" and
  "detekt fails the build". Neither tool exists in the template any more (`grep` finds no ktlint or
  detekt in `template/`).
- Tone: several comments editorialise rather than explain ("a spectacularly unhelpful failure",
  "Material's rule, because the obvious alternatives are worse", "the least informative message in
  the entire toolchain"). AGENTS.md asks for "comment why, not what".

### 5.13 Silent no-op appliers
`apply_feel` matches one exact source line (`render.py:352-356`). `apply_fonts`, `apply_accent`,
`_apply_on_brand` and `translations._list_languages` use regexes over specific template files
(`Palette.kt`, `AppColors.kt`, `AppFonts.kt`, `AppRoot.kt`, `AppLocales.kt`) and `return` quietly
when the file or pattern is missing. All of them work against today's template (verified: custom
accent, fonts, Playful/Bouncy, haptics off and languages all land in the output). But one rename
in the template would silently drop the user's choice, with no test failing.
`test_the_styles_are_written_into_the_one_theme_call` builds its own fake `AppRoot.kt`. **Fix:**
count substitutions and raise `RenderError` when an expected one did not happen. Even better,
replace the regexes with explicit `<generated:…>` placeholders in the template (§7).

## 6. Test-suite and CI gaps

1. **Running the file directly skips tests.** `tests/test_generator.py:779-780` has
   `if __name__ == "__main__": unittest.main()` in the middle of the file.
   `python3 tests/test_generator.py` runs 98 tests and silently skips `RecordTest`,
   `CommitStampTest`, `CompileSdkTest` and `BrandContrastTest`; `python3 -m unittest
   tests.test_generator` runs 103. Move it to the end.
2. **Untested areas**, each now known to hide a bug:
   - `icons.py` (§3.2);
   - `generate_headless.py` (§4.8);
   - `add_feature.register_*` (§4.4; only `remove_feature` is tested);
   - `zip_project` (mode bits, root folder);
   - keystore generation and `keystore.properties` round-trip (§4.2, §4.3);
   - scaffold plus rename (§3.1);
   - `apply_build_settings` and `_replace_flavour_field`;
   - marker balance and known feature names over `template/` (§5.4);
   - the static consistency checks of §2.
3. **Unused fixtures and tools.** `tests/spec_keys.json` is not referenced by any test, CI step or
   doc. `tools/feature_combinations.py` claims pairwise coverage "for CI", but CI never calls it.
4. **Gradle matrix gaps** (`.github/workflows/ci.yml:84`: `none, lean, everything, all`):
   - the **`standard`** preset, the website default, is never built;
   - no row has zero feature modules (§3.3);
   - the `firebase-off` variant files (`AnalyticsModule.kt`, `CrashModule.kt`, `FlagsModule.kt` with
     analytics or flags on) are **never compiled**: `none` and `lean` have neither analytics nor
     flags, and `everything` and `all` have firebase on;
   - only one package name and app name are ever used.
5. Tests print to stdout ("No AppDestinations.kt at …").

**Suggested additions:**
- port `.overhaul/scratch-gen/check.py`, `hilt.py` and `markers.py` into
  `tests/test_consistency.py`, run over presets and the pairwise sets (about 20 s, no Gradle);
- add `standard` and a "screens only, no modules" spec to the Gradle matrix;
- add a hostile-names spec (`com.example.databaseapp`, module `database_app`) to the unit tests.

## 7. Structure and the planned multi-target overhaul

- **`genkit/render.py` (804 lines) is a god module.** It holds the marker engine, renaming, copying,
  variant overlay, the rewrite pass, AppConfig and flavour patching, fonts, feel, app name, keystore
  properties, keytool, zip, git, and ~220 lines of colour science (`_hex_to_rgb` … `_apply_on_brand`,
  `:587-804`). `prompts.describe_brand_colours` imports the private `_to_argb` from it
  (`prompts.py:569`). `catalogue.py` imports all of `render` just for `find_keytool` (`:13`).
  Inside the colour code, `_MAX_DARKENING_STEPS` and `Rgb` are defined after their first use
  (`:750`, `:753`), and `colorsys` is imported inside functions.
- **`genkit/spec.py` (1,028 lines) mixes concerns:** the API table, ~450 lines of feature catalogue
  data, presets, styles, the keystore model and all validation. `GROUPS` and `HEADLINES` live in
  `catalogue.py` as parallel dicts keyed by feature, so they drift unless a test catches it; they
  should be fields on `Feature`. `spec.validated()` lazy-imports `translations` to dodge a cycle
  (`spec → translations → scaffold → spec`).
- **Android paths are hard-coded across `apply_*`** (`AppRoot.kt`, `AppConfig.kt`, `Palette.kt`,
  `AppColors.kt`, `AppFonts.kt`, `strings.xml`, `colors.xml`, `AppDestinations.kt`,
  `mipmap-anydpi-v26`). Each is a hidden contract with the template.

For the Compose Multiplatform and SwiftUI generators, I recommend splitting into:

```
genkit/core/       spec model (target-agnostic fields), feature graph, presets, validation,
                   marker engine (with error reporting), single-pass rename, packaging (zip, git),
                   record, translations model
genkit/android/    android adapter: identity tokens, file ownership, build settings, palette,
                   fonts, feel, icons, keystores, scaffold
genkit/swiftui/    later: xcodeproj/xcconfig, Info.plist, asset catalogs, Swift scaffold
genkit/brand.py    colour science shared by all targets
```

Each target should declare its substitution points in the template, as named `<generated:…>`
blocks or a small `template.toml`, instead of regexes over arbitrary source. Then a missing anchor
fails the build. Feature ownership should become a marker expression (`room|database`) rather than
a flat `files` list, which also solves §4.10 and §4.11.

## 8. Adding Kyant liquid glass as an optional generator feature

PeriFlow already ships it:
- `gradle/libs.versions.toml:43-44, 78-79`: `io.github.kyant0:backdrop:2.0.1` and `shapes:1.2.1`;
- `core/designsystem/build.gradle.kts:26`: `implementation(libs.kyant.backdrop)`;
- files `foundation/Glass.kt`, `foundation/LiquidInteraction.kt`, `foundation/ProgressiveBlur.kt`,
  `component/navigation/GlassBottomBar.kt` and `component/navigation/GlassRail.kt`;
- it gates on `com.kyant.backdrop.isRenderEffectSupported()` with a solid fallback
  (`Glass.kt:149`, `GlassBottomBar.kt:102`);
- PeriFlow's `MIN_SDK` is 30.

To land it in the generator cleanly:

1. Add `Feature(key="liquidglass", default=False, files=(those 5 files under core/designsystem…))`.
   Add a catalogue group and headline (Design), and a translation entry if any strings are added.
2. Fence the two TOML entries and the `designsystem` dependency with `<opt:liquidglass>`. The TOML
   has only 4 markers today, so without this every project would carry the library (§4.11).
3. The call sites sit inside shared files: PeriFlow's `AppNavigationRail.kt:51` calls
   `LiquidGlassRail(...)`. The engine only has a **single-line** `<opt:!x>`, so a multi-line
   "glass off" body would need a `variants/liquidglass-off/` whole-file copy. That is the
   drift-prone mechanism already used for firebase and googlefonts, and its firebase copies are
   never compiled in CI (§6.4). **Add a block-level else to `strip_markers`** first:

   ```kotlin
   // <opt:liquidglass>
   LiquidGlassRail(items, selectedIndex, onItemSelected, modifier)
   // </opt:liquidglass>
   // <opt-else:liquidglass>
   //~ SolidRail(items, selectedIndex, onItemSelected, modifier)
   // </opt-else:liquidglass>
   ```

   In the else block the generator strips the `//~ ` prefix when the feature is off. The template
   still compiles with everything on.
4. Add `liquidglass` to the CI Gradle matrix both on and off. Run the §2 static checks over it.

## 9. Reproduction

```bash
cd /home/crazyhusir/Documents/projects/tools/.overhaul/scratch-gen
python3 markers.py                         # template marker scan
python3 matrix.py ref-all && python3 matrix.py   # 159 combinations → matrix-results.json
python3 hiltmatrix.py                      # Hilt graph over the same set
python3 names.py                           # hostile names (§3.1, §3.4, §4.1, §5.1-5.3)
python3 check.py ../out/<project> [<reference>|-] <package>
python3 unusedres.py ../out/gen-minimal
```

Outputs kept for inspection in `/home/crazyhusir/Documents/projects/tools/.overhaul/out/`:
- `gen-ref-all`, `gen-full`, `gen-minimal`;
- `gen-n-pkg-apple` and `gen-n-pkg-prefix` (§3.1);
- `gen-n-mod-in`, `gen-n-mod-null` and `gen-n-pkg-null` (§3.4, §4.1);
- `gen-crash-auth` (§3.3);
- `gen-keys` and `gen-keys2` (§4.2, §4.3);
- `addtest` (§4.4);
- `parentdir` (§4.6);
- `gen-pascal-collide` (§5.1).
