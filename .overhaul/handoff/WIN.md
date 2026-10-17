# WIN handoff

Branch `wp/WIN` in `$OV/wt/WIN` (`OV=D:/projects/tools/.overhaul`, `O=$OV/out/WIN`). Logs in `$OV/logs/WIN/`.
Nothing pushed. There is no WP file: the scope in the orchestrator's task is the specification.

## Progress

Updated 2026-10-17 (fix#2, after the second fresh-context review). Both blocking findings are fixed: `--force` no
longer reports a finished replace as failed when the old output cannot be deleted (it was renamed 19+ characters
deeper and deleted by a cleanup that raised), and a destination named `replaced` no longer collides with the aside.
The old output now takes a hidden sibling name no longer than the destination's, is deleted explicitly (through the
extended-length form on Windows), and what cannot be deleted is a warning. Seven of the nine non-blocking findings were
addressed in code or tests; the Windows CI job is for WP13 (owner of `.github/workflows/**`), and the reviewer's
scratch leftovers are not deleted (table "Review fix#2"). Acceptance re-run on the final head `91ba5d7`:
**ACCEPT OK** (Measurements). Worktree clean; nothing pushed.

Updated 2026-10-17 (fix#1, after the fresh-context review). The review's one blocking finding (`is_link` read a
Windows-only constant, so `--force` crashed on Linux and macOS) is fixed and now tested on every host; all nine
non-blocking findings were addressed in code or tests (table "Review fix#1" below). Acceptance re-run on the
final head `31364eb`: **ACCEPT OK** (Measurements).

Run 1 left 18 modified files, a root `.gitattributes` and `tests/test_host.py` uncommitted and no handoff; with them
the suite already passed here (`logs/WIN/suite-wip.log`). Run 2 reviewed that work, committed it in logical steps with
three corrections (Python 3.11 support in `is_link`, the `--git` commit's line endings, the `.\gradlew` hint dropped),
and found and fixed five more host bugs.

| Step | State |
|---|---|
| root `.gitattributes`: LF working trees on every host | done (`162dcfb`) |
| one `write_text` helper for every file the generator writes, plus a source check (`test_host`) | done (`9933e2e`) |
| exec bit proved in the zip and in the `--git` commit; commit blobs independent of `core.autocrlf` | done (`ad0e436`) |
| CLI reports the destination with its directories resolved (8.3 short names, `..`, links) | done (`c26bf3d`) |
| junctions handled as links; symlink test skips only without the privilege | done (`efcb4a4`) |
| `--git --no-zip` across volumes and `--force` over a `--git` project on Windows | done (`d995a64`) |
| replace guard by file identity (case-insensitive file systems) | done (`cefa77f`) |
| language order independent of the host's path ordering | done (`7459842`) |
| a path over Windows' length limit explained instead of "The generator failed." | done (`84b2530`) |
| "standard" project against the Linux bytes | done: COMPARE OK (`logs/WIN/compare-hosts.log`) |
| acceptance (`$O/accept.sh`) | **ACCEPT OK** at `84b2530` (its log was overwritten by fix#1's run) |
| fix#1: `is_link` reads the reparse tag only on Windows; `is_link` tests on every host | done (`eb22a43`) |
| fix#1: `update-index --chmod` hashes with `core.autocrlf=false`; warnings name the git command | done (`7250671`) |
| fix#1: old project kept beside the destination when it can be neither replaced nor put back | done (`6da43d1`) |
| fix#1: path-limit message only with long paths off; remedy per stage (staging or destination) | done (`fdb3543`) |
| fix#1: CRLF token copy tested; CR guards over the real templates and a full generated project | done (`d05aff8`, `99a2110`) |
| fix#1: acceptance re-run | **ACCEPT OK** at `31364eb` (`logs/WIN/accept.log`) |
| fix#2: old output renamed to a hidden sibling no longer than the destination, deleted explicitly, a warning if it stays | done (`c284603`, `91ba5d7`) |
| fix#2: a path over the limit explained from `filename2` and from a tree copy's `shutil.Error` | done (`3b501fb`) |
| fix#2: `catalogue --check` holds the website copy to the bytes `--write` writes | done (`5ab83e7`) |
| fix#2: link fixtures shared by `test_cli` and `test_host` (`tests/links.py`) | done (`291856b`) |
| fix#2: acceptance re-run | **ACCEPT OK** at `91ba5d7` (`logs/WIN/accept.log`) |

## What changed (by path)

- `.gitattributes` (new, repository root): `* text=auto eol=lf`, `*.bat`/`*.cmd` CRLF, the golden fixtures `-text`.
  Git for Windows sets `core.autocrlf=true` system-wide (it is set on this machine, `C:/Program Files/Git/etc/gitconfig`),
  and the generator copies template bytes, so a Windows clone put CRLF into every generated project. Every text file
  in the index was already LF, so the commit changes no file; `templates/android/.gitattributes` (which ships in
  projects) is unchanged.
- `generator/genkit/core/copy.py`: `write_text(path, text)` writes UTF-8 bytes as given; `is_link(path)` is a symbolic
  link or, on Windows only, a junction by the reparse tag (Python 3.11 has no `Path.is_junction`; `st_reparse_tag`
  and `stat.IO_REPARSE_TAG_*` exist only on Windows, so the tag is read only where `os.name == "nt"`).
- Every text write in `generator/` and `tools/` goes through `write_text`: `add_feature.py`, `remove_feature.py`,
  `create_project.py` (`--save-spec`), `genkit/core/{catalogue,record,pipeline}.py`,
  `genkit/targets/android/{icons,look,scaffold,strings,target}.py`, `genkit/tokens/__main__.py`,
  `tools/{check_versions,feature_combinations}.py`. `tests/test_host.py` fails on any `.write_text(...)` call or
  text-mode `open(..., "w")` without `newline` in those sources, so the rule holds on the Linux CI where the bug cannot
  show. `genkit.tokens --check` compares bytes, so a CRLF copy no longer passes for current, and
  `genkit.core.catalogue --check` also requires the website copy's bytes to be exactly what `--write` writes (its
  comparison parses the JSON, which hid line endings and layout). Printed paths use `/`.
- `generator/genkit/core/package.py`: `git_init(project, message, executable)` adds, and marks the manifest's
  executable files with `git update-index --chmod=+x`, both with `-c core.autocrlf=false` (both hash the files). A
  failed step's warning names the git command (`git add`, `git commit`), not the `git -c` its config options made it.
- `generator/genkit/core/pipeline.py`: `_move` (rename, or copy across volumes and let the staging cleanup delete the
  source), `_swap(finished, destination) -> Path` (old destination renamed to `_aside(destination)`, new one renamed
  in, old one put back if that fails; if putting it back fails too, the `RenderError` names the sibling that holds it),
  `_aside` (a free hidden sibling name of lowercase letters and digits, no longer than the destination's name, never
  equal to it without case), `_delete_replaced` (after the swap: unlinks a file or link, `_delete_tree` for a folder;
  an `OSError` with the aside still there becomes a warning in `Generated.warnings` naming it), `_delete_tree`
  (`shutil.rmtree`; on Windows through the `\\?\` or `\\?\UNC\` extended-length form, with `_writable_and_again`
  clearing the read-only flag and retrying, `onexc` on 3.12+, `onerror` on 3.11), `_windows_path_limit_explained(shorter)`
  (a `RenderError` naming the path, its length, the limit and the remedies, only when `LongPathsEnabled` is not 1;
  one block for staging, "shorter app name or package name", one for placing beside the destination, "shorter
  destination, app name or package name"), `_path_too_long` (the longest of `filename` and `filename2`, or of a tree
  copy's failed destinations, past the folder limit), `_windows_long_paths_on` (reads the registry value). `_remove`
  and fix#1's `_keep_beside` are gone.
- `generator/create_project.py`: `confirm_replace` uses `is_link`; `_unsafe_to_delete` compares by file identity
  (`samefile`); `absolute`'s docstring says the reported path is the resolved one.
- `generator/genkit/core/translations.py`: `available` sorts by directory name.
- Tests: `test_golden` (zip and commit exec bits, commit blobs equal the generated bytes, `git status` clean after
  `--git`, placement across volumes and over a `--git` project, a failed swap keeps the old project, a double failure
  keeps it beside, the Windows path limit in staging and beside the destination, the error left alone with long paths
  on; the placement fakes patch `os` only as the pipeline sees it, `pipeline_os(...)`), `test_cli` (resolved path for
  `..` and an 8.3 short name, junction replace, symlink skip, case-insensitive guard, the current-directory guard
  checks its reason), `test_translations` (language order), `test_tokens` (a CRLF token copy is stale until
  `--write`), `test_android_target` (every feature, a module, a brand and every language: a CR byte only in
  `gradlew.bat`), `test_host` (new: the `write_text` source rule; `is_link` of a directory, a file, nothing, a symlink,
  a junction, and of a directory with a stat module that has no reparse tags, as on Linux; no CR in any text file the
  generator copies from the real templates and variants, batch files excepted). fix#2: `tests/links.py` (not a test
  module) holds `symlink_or_skip` and `junction` for `test_cli` and `test_host`; `test_golden`'s placement fakes go
  through `seen_by_pipeline(module, ...)` (`os` or `shutil` as the pipeline sees it) and add: a deletion that fails
  after the swap keeps the new project and warns; destinations named `a`, `ab` and `replaced` replaced whole as a
  folder and as a zip; on Windows with long paths off, an old project with one file at the in-place limit and one past
  it deleted whole; the kept output no longer than the destination's name; the path named from `filename2` and from a
  `shutil.Error`. `test_catalogue`: a CRLF website copy is stale until `--write`.

## Public APIs added, changed, removed (signatures)

- Added `genkit.core.copy.write_text(path: Path, text: str) -> None` and `genkit.core.copy.is_link(path: Path) -> bool`.
- Changed `genkit.core.package.git_init(project: Path, message: str, executable: tuple[str, ...]) -> list[str]`
  (was `git_init(project, message)`). Only `pipeline.build` calls it.
- `pipeline.build` keeps its signature; `--force` now renames the old output aside instead of deleting it first.
- fix#1 changed no public signature. `pipeline.build` can now raise `RenderError` naming a `<name>.previous.*` folder
  (double failure on `--force`); the private `_windows_path_limit_explained` takes the remedy text.
- fix#2 changed no public signature. With `replace`, `pipeline.build` no longer raises once the new output is in
  place: an old output it cannot delete completely is a warning in `Generated.warnings` naming the hidden sibling that
  holds the rest. The double-failure `RenderError` names that same sibling, no longer a `<name>.previous.*` folder.

## Decisions

- **Which path the CLI reports: the canonical one, directories resolved, last part as given.** `absolute()` already
  resolved the directories so that `--force` replaces a link rather than what it points at, and that is the path the
  build acts on, so it is the honest answer. The test expected the caller's spelling, which held only where `%TEMP%`
  has one name (on this machine it is `C:\Users\CRAZYH~1\...`, and on macOS `/var` is a link to `/private/var`). The
  tests now compare with `Path.resolve()` of the directory they made, and pin `..` and a deliberate 8.3 name. Not
  loosened: each comparison is still exact.
- **The symlink test skips only on `WinError 1314`** (no SeCreateSymbolicLinkPrivilege, i.e. no Developer Mode), with
  that reason; any other `OSError` still fails it, and it runs on Linux and in CI. A junction test (any Windows
  account can make one, `mklink /J`) covers links on Windows instead.
- **The filesystem exec-bit test is skipped on Windows** (`os.name == "nt"`, reason in the skip). The guarantee moved
  to where the bit lives on Windows: the zip's `external_attr` and the git index, both tested on every host.
- **`git_init` adds with `core.autocrlf=false`**, so the first commit holds the project's bytes as its own
  `.gitattributes` says, never this machine's setting. For the Android template (whose `.gitattributes` sets `eol`
  for everything) this changes nothing; for a template without one it keeps the blobs equal on every host. The
  golden test checks every blob against the generated file.
- **`--force` renames the old output aside** rather than deleting it in place: one rename that fails whole when
  Windows holds a file open (Android Studio, a terminal inside). A failed second rename puts the old output back; if
  that fails too (something re-created the destination in between), the old output stays where it was renamed and
  the error names it. Without `--force` placement is one rename, as at the integration head; a successful `--force`
  is two renames and then the deletion of the old output.
- **fix#2: the aside is a sibling of the destination, never deeper.** Its name is a dot and random lowercase letters
  and digits, no longer than the destination's name (a single character for a one-letter name), different from it
  without case and not yet taken. Every path in the old output therefore stays as long as it was, so what Windows let
  the user create and delete in place can still be deleted, and a leftover is as usable as the project was. fix#1's
  aside, `.<name>.XXXXXXXX/replaced` inside the folder the new output is finished in, was `len(name) + 19` characters
  deeper and could collide with a destination named `replaced`. A sibling cannot collide with that folder either: its
  name is 10 characters longer than the destination's. The name is checked with `lexists` and then renamed to, not
  reserved atomically: a reservation would be a directory, which `os.replace` cannot replace on Windows, and a
  container directory would add depth; another process creating that random name in between is not a realistic risk.
  `random` (not `secrets`) is enough for a name that only needs to be unlikely to exist.
- **fix#2: deleting the old output is not part of success.** The new output is in place first, so a deletion that
  fails (a file held open by an IDE, indexer or antivirus; a path no tool can reach) becomes a warning naming the
  hidden sibling with what is left, and the CLI exits 0 with `ok: true`. Raising there would report a finished replace
  as failed, which is what the review reproduced.
- **fix#2: on Windows the old output is deleted through the extended-length form** (`\\?\C:\...`, `\\?\UNC\...` for a
  share). Java, and so Gradle, Kotlin and KSP, reach past MAX_PATH by themselves, so a built project can hold files
  Python cannot delete by their plain path with long paths off; the integration head failed on them too (half of the
  old project deleted, the new one discarded). This is not the "explained, not worked around" case below: that is about
  writing a project no other tool could then open, while deleting a path past the limit only removes what the user
  asked to replace. The retry clears the read-only flag Windows needs cleared and git sets on its objects; on Linux and
  macOS the plain `shutil.rmtree` is enough.
- **The path-limit explanation reads `LongPathsEnabled`** (`HKLM\SYSTEM\CurrentControlSet\Control\FileSystem`, the
  documented switch; read-only) and stays silent when it is 1, so a genuinely missing file on a long path is reported
  as missing. It assumes the Python process is long-path aware, which python.org's `python.exe` is since 3.6; an
  embedding that is not would get the raw error, as before. `RtlAreLongPathsEnabled` would answer for the process
  exactly but is undocumented.
- **The remedy follows the stage**, by wrapping staging and placement separately rather than guessing from the
  failing path (the destination can itself be under `%TEMP%`, as in the tests). While staged, only the app and package
  names are the caller's; beside the destination, its folder counts too.
- **Over-long paths are explained, not worked around.** `\\?\` paths would let the generator write a project whose
  paths pass 260 characters, but Explorer, git without `core.longpaths` and aapt2 would then fail on it; a clear
  message with the two remedies is the useful answer.
- **Dropped from run 1: printing `.\gradlew` on Windows** in the CLI's "Next" lines. `.\gradlew` works in cmd and
  PowerShell but not in Git Bash; `./gradlew` works in PowerShell (the Windows default) and Git Bash. Not a host bug
  in the sense of this scope, so the line stays as it was.
- **"Byte-identical to Linux" is proven by derivation**, because there is no Linux machine, WSL or Docker here and no
  Linux-generated tree survived the move (`$OV/out/gen-*` from Oct 8 hold only `generator-spec.json`). Three parts:
  the golden trees under `generator/tests/golden/` are Linux bytes, and the engine reproduces them exactly here; the
  integration head's generator on Windows differs from Linux only where it translated newlines (every other host
  dependency was audited, below), so folding its CRLF back (except in files whose template source has CR) gives the
  Linux bytes; and this branch's output on Windows equals those bytes file for file, with the same zip entries and
  modes. `$O/compare_hosts.py` (written in run 1, reviewed in run 2) does the second and third. Two limits, as the
  review noted: the derivation rests on the audit's claim that newline translation was the integration head's only
  host difference, and the script lives outside the repository, so it is evidence for this merge, not a regression
  test. What guards the bytes from now on is in the repository: `test_golden` (Linux golden trees), `test_host` (the
  `write_text` rule and LF template sources) and `test_android_target` (a full project with CR only in
  `gradlew.bat`), which run on every host.

## Host audit (what was checked, beyond the four named failures)

| Class | Finding | Outcome |
|---|---|---|
| newline translation | 13+ `write_text` sites, `tools/` scripts | fixed, enforced by `test_host` |
| newline translation | `git add` under `core.autocrlf=true` | fixed (`-c core.autocrlf=false`) |
| newline translation | Windows clones of the generator repo | fixed (root `.gitattributes`) |
| file modes | exec bit in zip and git index | fixed and tested |
| file modes | read-only `.git/objects` on `shutil.move` across volumes and on `--force` | fixed and tested |
| links | junctions not seen as links | fixed and tested |
| case | `--force` guard compared POSIX paths by spelling (macOS) | fixed (identity); test skips on case-sensitive file systems |
| case | `sorted(Path)` orders without case on Windows: language list in the catalogue | fixed and tested |
| case | other `sorted(Path)` (features, strings, variants, translations' module files) | checked: re-sorted by key strings or order-free |
| case | renamed files that collide only by case | checked: the only renamed file (`BaseAppApplication.kt`) has one sibling, `MainActivity.kt`; module and package names are lowercase by validation |
| separators | zip names, `removed_paths`, keystore `storeFile`, printed paths | checked: all `/` (`as_posix`) |
| encoding | stdout on a legacy code page | checked: `prompts.py` reconfigures stdout/stderr to UTF-8; headless JSON is ASCII |
| path length | MAX_PATH without long paths, plus 58 characters of `%TEMP%` | explained (see Decisions); not solvable in the generator |
| path length | `--force` over a built project whose Gradle files pass MAX_PATH (Java reaches past it) | fixed in fix#2: deleted through `\\?\`; tested |
| path length | a failure naming the long path as `filename2`, or a tree copy's `shutil.Error` | fixed in fix#2: explained like the rest; tested |
| other | `Path.rename` in `icons.py` would fail on Windows over an existing target | checked: the target (`drawable/ic_launcher_monochrome.xml`) never exists in the template |
| other | `keytool` lookup | checked: already tries `keytool.exe` |

## Deviations

- No WP file; the scope text is the specification. The "standard" comparison is derived rather than run on Linux
  (see Decisions for why and how).
- Correction to run 2: its claim that the link tests "run on Linux and in CI" was true of the tests but not of the
  code. At `84b2530` `is_link` raised `AttributeError` for any existing non-link path on Linux and macOS, so the
  ubuntu CI job would have failed `test_force_replaces_exactly_the_destination`. Fixed in `eb22a43`.

## Review fix#1 (fresh-context review of `84b2530`)

| Finding | Severity | Outcome |
|---|---|---|
| `copy.py:75` `stat.IO_REPARSE_TAG_MOUNT_POINT` read on every host | blocking | fixed `eb22a43`: tag read only on Windows; `test_host.LinkTest` (6 cases, one simulating a stat module without the tags, which fails on the old code here); the current-directory guard test now checks its reason, not only the exit code |
| `package.py:63` `update-index` hashed under the host's `core.autocrlf` | non-blocking | fixed `7250671`: `-c core.autocrlf=false` on both hashing commands; the warning names the subcommand |
| `pipeline.py:158` double failure lost the old project | non-blocking | fixed `6da43d1`: kept beside the destination and named; tested |
| `pipeline.py:119-131` any long ENOENT taken for the limit; no destination remedy | non-blocking | fixed `fdb3543`: `LongPathsEnabled` checked; per-stage remedy; three tests (staged, destination too deep, long paths on) |
| `tokens/__main__.py:88-91` byte-exact `_read` untested | non-blocking | fixed `d05aff8`: a CRLF copy fails `--check`, `--write` restores LF |
| `test_golden.py` E303/E305 blank lines | non-blocking | fixed (in `6da43d1`) |
| `test_cli.py:37` `import ctypes` inside a function | non-blocking | fixed `faa3ee7` |
| `.gitattributes` does not fix an old Windows clone; no repository guard | non-blocking | fixed `99a2110`: CR guard over the real templates and variants, and over a full generated project; the refresh command is in the test's message and under "Rules and gotchas" |
| placement tests patched the global `os` | non-blocking | fixed `6da43d1`: `pipeline_os(...)` replaces `os` in the pipeline module only |
| derived Linux comparison | non-blocking | recorded under Decisions; in-repository guards listed there |

## Review fix#2 (fresh-context review of `31364eb`)

| Finding | Severity | Outcome |
|---|---|---|
| `pipeline.py:115` the aside, `len(name) + 19` characters deeper, deleted by a cleanup that raised after the new project was in place: "The generator failed." for a finished replace, a hidden `.<name>.*` left (WinError 3, then 145) | blocking | fixed `c284603`: aside is a hidden sibling no longer than the destination's name; deleted explicitly after the swap; failure is a warning naming what is left; on Windows deleted through `\\?\`, so files past MAX_PATH go too. Tests: deletion failing after the swap (warning, new tree, old tree in the named sibling); on Windows, an old project with a file at the in-place limit (259) and one past it (320) deleted whole. Both fail at `31364eb` (`ValueError` unpacking the leftover; WinError 145), the second also at the integration head. The reviewer's CLI reproduction (`--out <scratch>/out/other --no-zip --force --json` over a project whose deepest file is 242 characters here) now returns `ok: true`, no warning, nothing beside it |
| `pipeline.py:115` aside `beside/"replaced"` equals `finished` for a destination named `replaced`: folder swap fails, a zip keeps its old bytes | blocking | fixed `c284603`: the aside is outside the folder `finished` is in, and its name is never the destination's. Test: `a`, `ab` and `replaced`, each as a folder and as a zip, replaced whole with nothing left beside; at `31364eb` the folder case fails with WinError 5 and the zip case with the old tree. `create_project.py --out <scratch>/out/replaced --no-zip --force --json` returns `ok: true` |
| `catalogue.py:262` `--check` read text and compared parsed JSON | non-blocking | fixed `5ab83e7`: bytes, and the bytes must be what `--write` writes (reading bytes alone was not enough: `_comparable` parses the JSON). Test: a CRLF copy fails `--check` with that reason, `--write` restores LF; it fails on the old code |
| `pipeline.py:132` only `error.filename` looked at; `shutil.Error` from the cross-volume copy has no errno | non-blocking | fixed `3b501fb`: `_path_too_long` takes the longest of `filename`/`filename2`, or of a tree copy's failed destinations. Two subtests; both fail on the old code |
| `test_android_target.py:282` CR guard 23 s | non-blocking | measured, kept: 6.0 s in the full suite while another agent's Gradle build held 3.3 GB (`logs/WIN/suite-fix2.log`, `--durations`), of which 2.8 s is the generation (every language; 2.1 s with one). The suite's slowest test is `test_rename`'s hostile names (41.7 s in that run), which this package does not touch; whole-suite times across runs track the machine's load. A smaller spec would leave template families unchecked, which is the test's point |
| `test_host.py:23` cross-module import; symlink-or-skip duplicated | non-blocking | fixed `291856b`: `tests/links.py` (`symlink_or_skip`, `junction`) |
| `pipeline.py:190-194` `_keep_beside` used `mkdtemp` (0700 on POSIX) | non-blocking | gone with `c284603`: the old output is renamed straight to its sibling and keeps its own mode |
| handoff: "common path unchanged" true only without `--force` | non-blocking | corrected under Decisions |
| `ci.yml`: the generator suite runs only on ubuntu-latest | non-blocking | not changed here: `.github/workflows/**` belongs to WP13 (MASTER-PLAN ownership table; A12.7 rewrites `ci.yml`). Recorded under Open issues as a request to WP13 |
| reviewer's leftovers under the session scratchpad (`winrev/out/.other.*`, `.replaced.5rz6f60z`) | non-blocking | left in place: another agent's temporary files, and permanent deletion of temp files is the user's (global CLAUDE.md 2.3). They sit in the scratchpad, not in the repository or the worktree. `_delete_tree`'s `\\?\` form would remove them |

## Measurements

- **Acceptance after fix#2** (`$O/accept.sh`, log `logs/WIN/accept.log`, at `91ba5d7`, 2 min 13 s in all): the whole
  suite through `memwait`, 278 tests in 83.9 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit
  test). `genkit.tokens --check` and `genkit.core.catalogue --check` current (the latter now byte-exact). 0 added
  lines with a CR byte. Host comparison COMPARE OK (standard: 314 files, everything-and-brand: 469; only `gradlew`
  executable in either zip). Worktree clean. **ACCEPT OK.**
- Branch diff against the integration head after fix#2: 26 files, 851 insertions, 84 deletions; fix#2 alone
  (`31364eb..91ba5d7`): 7 files, 247 insertions, 64 deletions.
- **Whole suite at `91ba5d7` with `--durations 15`** (`logs/WIN/suite-fix2.log`, through `memwait`): 278 tests in
  166.3 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit test), while another agent's Gradle
  build ran (java at 3.3 GB, 3.2 GB free). Slowest: `test_rename` hostile names 41.7 s, two `test_headless` script
  tests 7.4 and 6.5 s, the CR guard 6.0 s.
- Each fix#2 test was run against the code before its fix and fails there (details in "Review fix#2"); 7 errors or
  failures in `test_golden` at the `31364eb` pipeline, 1 in `test_catalogue` at the `31364eb` catalogue.

- **Acceptance after fix#1** (`$O/accept.sh`, log `logs/WIN/accept.log`, at `31364eb`): the whole suite through
  `memwait`, 273 tests in 197.6 s, OK, 4 skipped (Pillow is not installed; the two symlink tests, `test_cli` and
  `test_host`, for want of the privilege; the filesystem exec-bit test, on Windows). `genkit.tokens --check` ("0 token
  copies current": none is checked in; the CRLF case is now a unit test) and `genkit.core.catalogue --check` current.
  0 added lines with a CR byte. Host comparison COMPARE OK (standard: 314 files, everything-and-brand: 469; only
  `Standard/gradlew` executable in the zip). Worktree clean. **ACCEPT OK.** A first run that straddled the last
  wording commit also passed (273 tests in 351.5 s on a busier machine).
- Each fix#1 test was checked against the code before its fix and fails there: `LinkTest`'s stat simulation
  (`AttributeError`), the double-failure placement test, the three path-limit tests, and both CR guards (made to fail
  by re-saving `templates/android/README.md` with CRLF, then restored with `git -c core.autocrlf=true checkout-index`).
- Branch diff against the integration head after fix#1: 24 files, 666 insertions, 82 deletions.
- **Acceptance before the review** (`$O/accept.sh` at `84b2530`): the whole suite through `memwait`,
  263 tests in 48.7 s, OK, 3 skipped (Pillow is not installed, as at the baseline; the symlink test, for want of
  the privilege; the filesystem exec-bit test, on Windows). `genkit.tokens --check` and
  `genkit.core.catalogue --check` current. 0 added lines with a CR byte. Host comparison COMPARE OK. Worktree clean.
  **ACCEPT OK.**
- Baseline at the integration head (`logs/windows/generator-baseline.md`): 249 tests, 5 failures, 1 error, 1 skipped.
- Run 1's uncommitted state (`logs/WIN/suite-wip.log`): 256 tests in 343 s, OK, 3 skipped. The time is the machine's
  load at that moment (other agents' suites ran at the same time; their temp folders appeared in `%TEMP%`), not the
  new tests, which take about 4 s together.
- Host comparison (`logs/WIN/compare-hosts.log`, spec `tests/fixtures/v2/standard.json` and an everything-and-brand
  spec with every language): **COMPARE OK**. Standard: 314 files, the integration head wrote CRLF into 4 of them on
  Windows; everything-and-brand: 469 files, 96 of them. The branch's output equals the derived Linux bytes in both;
  only `gradlew` is executable in both zips; the only CR bytes are in `gradlew.bat` (CRLF in the template on every
  host, `eol=crlf`) and the wrapper jar.
- Reproductions, each a test that fails at the integration head on this machine and passes on the branch:
  `--git --no-zip` from `%TEMP%` on C: to D: (PermissionError on `.git/objects/...` after the copy);
  `--force` over a `--git` project (PermissionError, old project half deleted, new one discarded); a 50-letter app
  name with a 99-character package (FileNotFoundError on a 293-character path, now explained); `zh-Hans`/`zh-HK`
  ordered differently from Linux.
- Branch diff at `84b2530`: 458 added lines, none with a CR byte.

## Rules and gotchas the docs must state

For WP12 (generator README/AGENTS and the generated projects' docs where they talk about Windows):
- The generator needs **Python 3.11 or newer** (`tomllib`). README's requirements table says 3.10, which is wrong.
- On Windows, clone the generator repository as it is: its `.gitattributes` keeps LF working files whatever
  `core.autocrlf` says. Never re-save template files with CRLF. A clone made before `.gitattributes` existed keeps its
  CRLF working files (git sees them as unchanged, so a plain checkout leaves them); refresh it once with
  `git checkout-index --force --all`, which rewrites every tracked file with LF even under `core.autocrlf=true`
  (verified here) and overwrites uncommitted changes, so commit or stash first. `tests/test_host.py` fails on such a
  clone and prints that command.
- `--force` over a project Windows will not let go of (a terminal or IDE inside it): the old project stays where it
  was; in the rare case it can be neither replaced nor put back, the error names the hidden folder beside it (a dot
  and random letters, no longer than the destination's name) that holds it.
- `--force` deletes the old project only after the new one is in place. If part of it cannot be deleted (a file an
  IDE or indexer holds open), the run still succeeds and a warning names the hidden folder beside the destination
  with what is left; delete it once nothing holds it. Build files past MAX_PATH (Gradle writes them) are deleted too.
- Without long paths on, Windows takes 259 characters for a file path. The generator stages in `%TEMP%` (about
  58 characters with the user's name) and a project's deepest source file is about 96 characters plus the package path
  plus the app name, so a long package name and a long app name together can pass it; the generator then says so.
  Building any Android project on Windows needs long paths for the same reason (Gradle's build directories are deeper
  still).
- In cmd.exe the wrapper is `.\gradlew` or `gradlew`; `./gradlew` works in PowerShell and Git Bash.
- `--git` on Windows records `gradlew` as executable (`100755`), so a clone on macOS or Linux runs it; on Windows
  itself `git init` sets `core.fileMode=false`, so the working tree shows no mode change.
- A symbolic link as `--out` needs Developer Mode on Windows; a junction works for any account, and `--force`
  replaces the link or junction, never what it points at.

## Open issues (with the reason they are open)

- The MAX_PATH case is explained, not avoided: staging under `%TEMP%` costs about 58 characters that the destination
  might not need. Staging beside the destination would change the API's cleanup model (pipeline docstring: staging
  lives under `TMPDIR` so the API deletes what a killed process leaves), which is outside this scope.
- After WIN merges, the line in `D:/projects/tools/CLAUDE.md` and the orchestrator's task text that say six
  generator tests fail on this machine are stale, and `logs/windows/generator-baseline.md` is history.
- The case-insensitive guard test only fails before the fix on macOS (Windows already compared paths without case);
  no macOS host was available to run it there.
- The suite has still run only on Windows. The non-Windows branches (`is_link` without reparse tags, the skipped
  Windows-only tests, the filesystem exec-bit test, `_delete_tree`'s plain `rmtree`) are covered by the simulation in
  `test_host` and by reading; the ubuntu CI job is the first real Linux run. Nothing is pushed, so it has not run.
- **For WP13 (owner of `.github/workflows/**`):** add a `windows-latest` generator job (the same two steps as the
  ubuntu one). Without it the Windows-only tests (junction, 8.3 short name, the path limit and the deep-file
  deletion, the CR guards on a Windows checkout) run only on a developer's machine. The path-limit tests probe for
  long paths and skip with that reason where they are on, so the job is valid on either kind of runner image. Not
  done here because the path is WP13's and A12.7 rewrites `ci.yml`.
- `_aside` checks a random name is free and then renames to it; another process creating that exact hidden name in
  between could be overwritten (a zip onto a file on any host, a folder onto an empty folder on POSIX). Accepted as
  unrealistic; an atomic reservation would need a directory, which cannot be renamed over on Windows.
