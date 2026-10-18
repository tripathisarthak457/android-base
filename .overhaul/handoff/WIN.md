# WIN handoff

Branch `wp/WIN` in `$OV/wt/WIN` (`OV=D:/projects/tools/.overhaul`, `O=$OV/out/WIN`). Logs in `$OV/logs/WIN/`.
Nothing pushed. There is no WP file: the scope in the orchestrator's task is the specification.

## Progress

- **Orchestrator note (Oct 17, 23:05).** Fix round 2 (the two blocking findings in the `--force` swap) returned done at about 16:40; the run was then paused for the user's usage reserve. Next: review round 3, then the merge.

Updated 2026-10-18 (the orchestrator's run "fix#1", after WIN merged: `merged/WIN` is `5a16778`). Nothing to change
in the repository. The run's findings are, word for word, those of the third fresh-context review (of `91ba5d7`,
table "Review fix#3"): the blocking one (the other-volume test no longer reached `shutil.move`) and the seven
non-blocking ones were fixed in fix#3 (`12ee3e8`, `6b7e88f`, `ae5941f`, `84e7d98`, `659fecc`, `0bd63b4`, and this
handoff's "Lock time used" section and blank line) and are in the merge. Re-verified at the merged head: with
`placement.py`'s `_move(project, finished)` replaced by the integration head's `shutil.move(str(project),
str(finished))`, `test_a_git_project_moves_from_staging_on_another_volume` errors with `PermissionError [WinError 5]
... android-gen-2hxa0nxv\TrailNotes\.git\objects\02\...` (file restored with `git checkout`, nothing left in
`%TEMP%`); each non-blocking fix read in place. `wp/WIN` fast-forwarded to `5a16778`. `$O/accept.sh` now takes
`BASE` and refuses `BASE` = HEAD: after the merge the merge-base is HEAD itself, which would make the CR check and
the host comparison compare the branch with itself. Re-run with `BASE=a402d87` (the integration head before the
merge): **ACCEPT OK** (Measurements). Worktree clean; nothing pushed.

Updated 2026-10-18 (fix#5, after the fifth fresh-context review; the orchestrator's run "fix#3" of that round). The
blocking finding is fixed: the output is no longer finished in `.<name>.XXXXXXXX/<name>` (`len(name) + 11` characters
deeper than the destination) but under a free sibling name exactly as long as the destination's, so the one limit
measured at the destination holds whether staging is on the same volume or not. Checked for real across volumes
(staging on C:, `--out` on D:): the standard preset at a 142-character `--out` is placed (deepest file 259), 143 is
refused naming the destination; the previous code refused 142 there naming `.Std.81l9pdhe`. A zip is measured at its
destination up front too. The placement code moved to `genkit/core/placement.py` (pure move first, `c1bd711`), which
takes `pipeline.py` back to 224 lines. Non-blocking: six fixed in code (notes printed without `--json`, the Windows
hint for a file, the stale docstring, "hidden" wording, long lines, the module split), two recorded (JSON on stdout,
the first Linux run); table "Review fix#5". Acceptance re-run on the final head `696e210`: **ACCEPT OK**
(Measurements). Worktree clean; nothing pushed. Next: review, then the merge.

Updated 2026-10-18 (fix#4, after the fourth fresh-context review; the orchestrator's run "fix#2" of that round). Both
blocking findings are fixed. The folder beside the destination where the output is finished is now made with
`mkdtemp` and deleted through `_delete_tree` (extended-length form, read-only retry), and a failed deletion is a
warning or a note on the error in flight, never its replacement; before, a failed swap with the new project's deepest
files past MAX_PATH there ended in WinError 145 and a hidden leftover. And a directory output whose paths would pass
Windows' limit at the destination is now refused, explained, before anything is created there; before, a
same-volume rename placed it silently. All seven non-blocking findings are addressed: four in code with tests that
fail on the old code, the cross-host path-limit tests, the variable name, and this handoff (table "Review fix#4").
Acceptance re-run on the final head `ecd3f80`: **ACCEPT OK** (Measurements). Worktree clean; nothing pushed. Next:
review, then the merge.

Updated 2026-10-17 (fix#3, after the third fresh-context review; the orchestrator's run "fix#1" of that round). The
blocking finding is fixed: the other-volume placement test again fakes EXDEV for every `os.rename` in the process
(only for the staged project), so it reaches `shutil.move` too; with the integration head's `shutil.move` put back
into the pipeline it fails here with WinError 5 on `.git/objects`, and on the branch it passes. All seven
non-blocking findings are fixed: five in code with tests that fail on the old code (the git half of the encoding one
only by reading, as it shows on Python 3.11-3.14 alone), the test junction helper checked by hand, and this handoff's
layout and lock-time section (table "Review fix#3"). One more host bug turned up on the way: keytool's messages (ANSI code page) crashed the
generator under this machine's Python 3.15 when they named a non-ASCII path. Acceptance re-run on the final head
`0bd63b4`: **ACCEPT OK** (Measurements). Worktree clean; nothing pushed. Next: review, then the merge.

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
| fix#3: the other-volume test reaches `shutil.move` again | done (`12ee3e8`) |
| fix#3: executable patterns matched with case on every host | done (`6b7e88f`) |
| fix#3: `--git` marks the scripts the index holds; git's output read as UTF-8 | done (`ae5941f`) |
| fix#3: keytool's output read in the ANSI code page, `rev-parse`'s as UTF-8 | done (`84e7d98`) |
| fix#3: a tree copy's failure held to the file or folder limit; the deletion warning gives only the reason | done (`659fecc`) |
| fix#3: the test junction's failure shows cmd's reason | done (`0bd63b4`) |
| fix#3: acceptance re-run | **ACCEPT OK** at `0bd63b4` (`logs/WIN/accept.log`) |
| fix#4: a destination held open is a `RenderError` that says nothing was changed | done (`d79fc13`) |
| fix#4: the folder beside the destination made with `mkdtemp`, deleted through `_delete_tree`, never replacing the error | done (`d178610`, `ecd3f80`) |
| fix#4: a directory output too deep for Windows' limit at the destination refused before placing; path-limit logic tests on every host | done (`76de583`) |
| fix#4: a read-only replaced file deleted on Windows | done (`898b7fc`) |
| fix#4: `tokens --check` names a line-endings-only difference; `test_host` sees `io.open` and `codecs.open` | done (`cc472a4`, `ef11688`) |
| fix#4: acceptance re-run | **ACCEPT OK** at `ecd3f80` (`logs/WIN/accept.log`) |
| fix#5: placement code moved unchanged to `genkit/core/placement.py` | done (`c1bd711`) |
| fix#5: output finished under a sibling name as long as the destination's; zip measured up front; file-or-folder hint | done (`8858f08`) |
| fix#5: notes printed without `--json`; `confirm_replace` docstring | done (`1b24642`) |
| fix#5: added lines past 120 columns wrapped | done (`696e210`) |
| fix#5: acceptance re-run | **ACCEPT OK** at `696e210` (`logs/WIN/accept.log`) |
| after the merge: fix#3's findings replayed; blocking one re-verified against `shutil.move` | done, no change needed |
| after the merge: acceptance at `5a16778` against `a402d87` | **ACCEPT OK** (`logs/WIN/accept-fix6.log`) |

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
  `_aside` (a free sibling name of a dot and lowercase letters and digits, no longer than the destination's name, never
  equal to it without case; `_free_sibling` since fix#5), `_delete_replaced` (after the swap: unlinks a file or link, `_delete_tree` for a folder;
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
  on; the placement fakes patch `os` only as the pipeline sees it, `seen_by_pipeline(...)`), `test_cli` (resolved path for
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
- fix#3: `genkit/core/copy.py` `is_executable` uses `fnmatchcase` (`fnmatch` folds case on Windows only).
  `genkit/core/package.py` `git_init` runs each step through `_git` (UTF-8 with `errors="replace"`; a failure raises the
  private `_GitFailed`, which becomes the same one warning as before) and lists the scripts to mark from the index
  (`git ls-files -z` after `git add`), not from the folder. `genkit/targets/android/keystores.py` reads keytool's
  output in `locale.getencoding()` with `errors="replace"`; `genkit/core/record.py` reads `git rev-parse` as UTF-8.
  `genkit/core/pipeline.py`: `_path_too_long` holds a tree copy's failed destination to the limit for what its staged
  source is (`_past_limit`: 259 for a file, 247 for a folder, and its folder at most 247); a rename's or a single
  call's paths keep the folder limit; the deletion warning gives `error.strerror`. Tests: `test_golden` (the
  other-volume fake patches `os.rename` for the process, refusing only the staged project; the deletion warning's
  reason; a tree copy's file within the limit re-raised, a folder past it explained), `test_host` (executable patterns
  with case), `test_zip` (a script the project's `.gitignore` keeps out: the commit is made and marks the rest),
  `test_keystores` (keytool's refusal naming a path with "Á" comes back as a warning naming that path exactly),
  `tests/links.py` (`junction` raises with cmd's message).
- fix#4: `genkit/core/pipeline.py`: `build` makes the folder beside the destination with `tempfile.mkdtemp` and
  deletes it with `_delete_beside(beside, report)` (through `_delete_tree`; a failure goes to `report`: the error's
  `add_note` while one is in flight, `warnings.append` after a success). Before placing a directory output it calls
  `_check_windows_path_limit(project, destination, shorter)` (Windows, long paths off: every staged file and folder
  measured at `os.path.abspath(destination)`, the longest one past `_past_limit` raised). `_past_windows_limit(what,
  shorter)` builds both path-limit messages. `_swap` turns a `PermissionError` from moving the old output aside into
  a `RenderError` ("could not be moved aside (reason); nothing was changed.", plus what usually holds it on Windows).
  `_delete_replaced` deletes a regular file through `_delete_file` (on Windows, a `PermissionError` clears the
  read-only flag and retries; a link is unlinked as before). `genkit/tokens/__main__.py`: a copy that differs only by
  CRLF is reported as such, without a diff. Tests: `test_golden` (helpers `deepest_destination`, `at_length`,
  `delete_tree`; `PlacementTest`: a held-open destination, the folder beside named when it cannot be deleted, as a
  warning and as a note, a read-only old zip; `WindowsPathLimitTest`: a refused swap with the new project at the
  deepest destination it fits; `WindowsLongPathsTest` now runs on every host, shown a Windows host by
  `seen_by_pipeline(os, name="nt")` and a patched `_windows_long_paths_on`, with forward-slash paths, plus a project
  one character too deep for its destination), `test_tokens` (the line-endings message), `test_host` (`io.open` and
  `codecs.open` take the mode second).
- fix#5: `genkit/core/placement.py` (new) holds everything about putting the staged project in place, moved from
  `pipeline.py` unchanged in `c1bd711`: `place(project, destination, *, zip_output, replace, executable) -> list[str]`
  (what `build`'s placement block did), `windows_path_limit_explained(shorter)` (public now: `build` wraps staging in
  it) and the private helpers named above. `pipeline.py` is back to 224 lines; its diff against the integration head
  is the `place` call, the staging wrapper, `write_text` and `git_init`'s new argument. Then, in `8858f08`: `place`
  finishes the output at `_free_sibling(destination)` (fix#2's `_aside`, renamed: a free name beside the destination,
  exactly as long as its own) instead of `mkdtemp(".<name>.")/<name>`; after a failure `_delete_unplaced(finished,
  error.add_note)` deletes it if it exists (`_delete_beside` is gone, a success leaves nothing to delete); `_delete`
  (link unlinked, folder `_delete_tree`, file `_delete_file`) serves it and `_delete_replaced`;
  `_check_windows_path_limit(project, destination, zip_output)` also measures a zip at its destination and names it
  ("The zip would be a path of N characters: ..."); `_swap`'s Windows hint says "a file in it open" for a folder and
  "A program may have it open, such as an archive viewer" for a file. `create_project.py`: `described(error)` adds the
  notes on the error and on its `__cause__` to the human message. Tests: `test_golden` (helpers
  `staged_on_another_volume()` and `refusing_the_new_output(destination, error)`; `WindowsPathLimitTest`: a zip of 259
  characters written, 260 refused naming it; the mini "all" project at the deepest destination it fits, from staging on
  either volume; `WindowsLongPathsTest`: a zip past the limit refused before anything is written; the deletion test
  now covers the unplaced output, named in a note), `test_cli` (without `--json`, the note on the error and on its
  cause both printed).

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
- fix#3 changed no public signature (`package._git`, `package._GitFailed` and `pipeline._past_limit` are private).
- fix#4 changed no public signature (the new `pipeline` functions are private). `pipeline.build` can now raise
  `RenderError` for a destination it cannot move aside ("nothing was changed"; it was a raw `PermissionError`) and,
  on Windows with long paths off, for a directory output too deep at its destination (it placed it). A failed
  build's error can carry a note naming the folder beside the destination it could not delete.
- fix#5: added `genkit.core.placement.place(project: Path, destination: Path, *, zip_output: bool, replace: bool,
  executable: tuple[str, ...]) -> list[str]` and `genkit.core.placement.windows_path_limit_explained(shorter: str)`
  (a context manager), both moved out of `pipeline`; `pipeline.build` keeps its signature and behaviour except: a
  directory output from staging on another volume is no longer refused when it fits at its destination; a zip past
  Windows' limit is refused before anything is written, naming the destination; a failed build's note names the
  unplaced output itself ("The new output was left at ..."), and a success never warns about a folder of its own.
  `create_project.described(error) -> str` is new (module-level, used by `main`).

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
  sibling with what is left, and the CLI exits 0 with `ok: true`. Raising there would report a finished replace
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
  names are the caller's; beside the destination, its folder counts too. (Until fix#4 the destination itself was not
  checked for a directory output; see the next point.)
- **Over-long paths are explained, not worked around.** `\\?\` paths would let the generator write a project whose
  paths pass 260 characters, but Explorer, git without `core.longpaths` and aapt2 would then fail on it; a clear
  message with the two remedies is the useful answer. Since fix#5 both outputs are measured at the destination before
  anything is made there, and nothing on the way is longer than the destination (next points). A directory output
  needs the measurement: from staging on the same volume it is placed by renames, which Windows performs however
  deep the files inside are; the review placed a project with a 264-character file at a 147-character destination,
  `ok: true`, no warning (the integration head's `shutil.move` behaved the same). From staging on another volume it is
  copied, and the copy writes every path, which is why the copy's depth matters (fix#5). Correction: until fix#5 this
  point said "writing never fails there" of placing, which held only on one volume.
- **fix#4: the destination is measured, not probed, and before anything is created there.** Every file and folder of
  the staged project is measured at `os.path.abspath(destination)` with `_past_limit` (the same file and folder limits
  the explanation of a refused path uses), and the longest one past it is named. Measuring costs one walk of a few
  hundred entries; a probe would have to create the deepest folders and delete them again. It runs only on Windows
  with long paths off (with them on, Python, Java and so the build reach such paths, as the error left alone with
  long paths on already assumes), only for directory output, and before `destination.parent.mkdir`, so a refusal leaves nothing behind.
- **fix#5: the output is finished under a sibling name exactly as long as the destination's.** fix#4 measured the
  project at the destination, but finished it in `.<name>.XXXXXXXX/<name>`, `len(name) + 11` characters deeper, and
  from staging on another volume (the default for any `--out` on D: here, `%TEMP%` being on C:) it is copied there,
  so a project that fits was refused, naming that internal folder; for "standard" an `--out` of 124 to 142 characters
  worked on C: and failed on D: (the review's numbers; 142 reproduced here). Of the review's two options this is the
  one that keeps a single limit: measuring at the deeper path instead would have made the limit depend on the volume,
  which no user can be expected to know. The sibling is `_free_sibling`, which fix#2's aside already was (random
  lowercase letters and digits after a dot, as long as the destination's name, different from it without case, not
  yet taken); the old output, moved aside during `--force`, takes a second one. A zip is finished the same way and is
  now measured at its destination up front, so a too-long zip path is refused naming the destination rather than a
  random sibling. Two things are given up, both small. mkdtemp reserved its folder atomically; the sibling is checked
  with `lexists` and then made by the rename, copy or zip write, so another process creating that exact random name
  in between could have it overwritten, as already accepted for the aside (Open issues). And a leftover after a hard
  kill is `.k3j9x2ab` rather than `.Standard.k3j9x2ab`, harder to tie to its destination; a run that fails normally
  deletes it or names it.
- **fix#5: placement in its own module.** `pipeline.py` says it is "the one place that says what generating a project
  consists of, and in what order"; the swap, the deletions and the path-limit handling had grown to half of it.
  `core/placement.py` takes them, first as a pure move (`c1bd711`) so the behaviour change (`8858f08`) reads on its
  own. It also brings `pipeline.py`'s diff against the integration head down to a few lines, which other packages
  that touch the pipeline will merge against.
- **fix#5: notes reach the human message.** `print(str(error))` drops `__notes__`, so a failure that left the new
  output undeletable said nothing about it without `--json`. `create_project.described` appends the notes on the
  error and on its `__cause__` (the path-limit explanation is raised `from` the error that carries the note).
  `traceback.format_exception_only` would cover the error's own notes but not the cause's, and adds the type name.
- **fix#5: JSON on stdout keeps the host's newline convention.** `create_project --json`, `generate_headless` and
  `catalogue --json` write through `sys.stdout`, which on Windows translates `\n` to CRLF. Left as it is: JSON
  allows CR as whitespace and every reader here parses it (Go's `encoding/json` in the API, which runs on Linux in
  production; Python's `json`; `JSON.parse`); nothing reads those bytes as bytes; and stdout is not a file the
  generator writes, which is what the LF guarantee covers (the website copy is written with `--write`, LF). Changing
  it would mean reconfiguring a stream the caller owns, or writing bytes past the tests' `redirect_stdout`.
- **fix#4: a cleanup never takes the place of the error in flight.** The folder beside the destination is made with
  `mkdtemp` and deleted in the build's own `try`: on failure its deletion's own failure becomes a note on the
  original error (`BaseException.add_note`, Python 3.11+; it shows in the traceback, which `--json` reports as
  `detail`), after a success a warning. Both name the folder. `tempfile.TemporaryDirectory` could not do this: its
  cleanup is a plain-path `rmtree` that ignores `FileNotFoundError` (what Windows says of a path past MAX_PATH) and
  then raises on the folder still holding those files. (Since fix#5 there is no such folder: the unplaced output
  itself is deleted the same way, and a success has nothing to delete.)
- **fix#4: only moving the old output aside is reported as "held open".** That rename is the one Windows refuses
  while a terminal, IDE or indexer holds a file in the destination, and when it fails nothing has changed, so the
  `RenderError` says so (with the Windows hint only on Windows; on POSIX a `PermissionError` there is a permission
  problem). A failure of the next rename (the new output into place) is rarer, and the old output is put back first;
  it keeps its own error, as the existing test requires.
- **fix#4: `codecs.open` is held to the text-write rule like `open`.** It translates newlines only when given no
  encoding, but it takes no `newline` argument either way and is deprecated since Python 3.14, so any write through it
  belongs in `write_text`.
- **fix#3: a fake that stands in for the host patches what the code under test reaches, then narrows by path.**
  fix#1 narrowed the placement fakes from the global `os` to the pipeline module's view of it, which was right for
  `os.replace` (only the pipeline calls it) but wrong for the other-volume fake: the bug it guards was inside
  `shutil.move`, which reaches `os.rename` through its own `os`. That fake now replaces `os.rename` for the process and
  refuses only a rename whose source sits in an `android-gen-*` staging folder, so nothing else in the process is
  touched. The other fakes keep `seen_by_pipeline`.
- **fix#3: each child process's output is read in the encoding that program writes.** git writes UTF-8 on every host
  (Git for Windows too), so `encoding="utf-8"`. keytool, as any Java program on Windows, writes to a pipe in the ANSI
  code page (`native.encoding`; checked here: "Á" arrives as `0xC1`), so `locale.getencoding()`, which is the ANSI
  code page on Windows whatever Python's UTF-8 mode says and the locale's encoding elsewhere, as Java's is.
  `errors="replace"` everywhere: a wrong guess garbles a character of a message rather than failing the generation.
  The test helper's `mklink` output is read as `oem`, cmd's own code page for a pipe (Windows only, as is the helper).
- **fix#3: `_path_too_long` judges a tree copy by its source, a single call by the folder limit.** For a
  `shutil.Error` the staged source still exists when the error passes the placement block, so whether a failed
  destination was to be a file or a folder is known and the limit matching it decides; a failure within that limit
  (a sharing violation, say) is re-raised as it was. For a plain `OSError` (`mkdir`, `open`, a rename) only the path
  is known, and the build makes every folder before it writes into it, so a path past the folder limit that Windows
  calls missing was refused for its length; that branch is unchanged.
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
| file modes | exec bit in zip and git index | fixed and tested; fix#3: the commit marks the scripts the index holds, so one the project ignores no longer costs the commit |
| file modes | read-only `.git/objects` on `shutil.move` across volumes and on `--force` | fixed and tested |
| links | junctions not seen as links | fixed and tested |
| case | `--force` guard compared POSIX paths by spelling (macOS) | fixed (identity); test skips on case-sensitive file systems |
| case | `sorted(Path)` orders without case on Windows: language list in the catalogue | fixed and tested |
| case | other `sorted(Path)` (features, strings, variants, translations' module files) | checked: re-sorted by key strings or order-free |
| case | renamed files that collide only by case | checked: the only renamed file (`BaseAppApplication.kt`) has one sibling, `MainActivity.kt`; module and package names are lowercase by validation |
| separators | zip names, `removed_paths`, keystore `storeFile`, printed paths | checked: all `/` (`as_posix`) |
| encoding | stdout on a legacy code page | checked: `prompts.py` reconfigures stdout/stderr to UTF-8; headless JSON is ASCII |
| encoding | child processes' output decoded in Python's default (ANSI code page up to 3.14, UTF-8 from 3.15) | fixed in fix#3: git as UTF-8, keytool in the ANSI code page, all with `errors="replace"`; keytool's case crashed here and is tested |
| case | `fnmatch` in `is_executable` folds case on Windows only | fixed in fix#3 (`fnmatchcase`); tested |
| case | `Path.glob` folds case on Windows (`features/*.toml`, `translations/*.json`, `ic_launcher*.xml`) | checked: no tracked file has a suffix or name that matches only without case; `case_sensitive=` needs Python 3.12 and the generator supports 3.11 |
| path length | MAX_PATH without long paths, plus 58 characters of `%TEMP%` | explained (see Decisions); not solvable in the generator |
| path length | `--force` over a built project whose Gradle files pass MAX_PATH (Java reaches past it) | fixed in fix#2: deleted through `\\?\`; tested |
| path length | a failure naming the long path as `filename2`, or a tree copy's `shutil.Error` | fixed in fix#2: explained like the rest; tested |
| path length | a directory output deeper at the destination than in staging, placed by a rename that never fails | fixed in fix#4: measured and explained before placing; tested on every host |
| path length | the folder beside the destination, deeper by its name, cleaned up by `TemporaryDirectory` after a failed swap | fixed in fix#4: `mkdtemp` and `_delete_tree`; the cleanup never replaces the error; tested |
| file modes | a read-only replaced file (zip) not deleted on Windows | fixed in fix#4: read-only retry for files; tested |
| path length | a directory output copied from staging on another volume into `.<name>.XXXXXXXX/<name>`, `len(name) + 11` characters deeper than its destination: refused although it fits | fixed in fix#5: finished under a sibling exactly as long as the destination; tested from either volume; checked with staging on C: and `--out` on D: |
| path length | a zip written in that same deeper folder: refused although it fits at its destination | fixed in fix#5: the same sibling, and the zip measured at its destination up front; tested |
| newline translation | JSON written to stdout (`create_project --json`, `generate_headless`, `catalogue --json`) is CRLF on Windows | checked, left: stdout is the caller's stream, and every reader parses JSON with CR as whitespace (Decisions, fix#5) |
| other | `Path.rename` in `icons.py` would fail on Windows over an existing target | checked: the target (`drawable/ic_launcher_monochrome.xml`) never exists in the template |
| other | `keytool` lookup | checked: already tries `keytool.exe` |

## Deviations

- No WP file; the scope text is the specification. The "standard" comparison is derived rather than run on Linux
  (see Decisions for why and how).
- Correction to run 2: its claim that the link tests "run on Linux and in CI" was true of the tests but not of the
  code. At `84b2530` `is_link` raised `AttributeError` for any existing non-link path on Linux and macOS, so the
  ubuntu CI job would have failed `test_force_replaces_exactly_the_destination`. Fixed in `eb22a43`.
- Correction to fix#1: narrowing the placement fakes to the pipeline module's `os` (`6da43d1`) left
  `test_a_git_project_moves_from_staging_on_another_volume` unable to see the `shutil.move` it was written against,
  so from `6da43d1` to `91ba5d7` a return to `shutil.move` would have passed the whole suite. Fixed in `12ee3e8`
  (Decisions, fix#3).
- Correction to fix#1 to fix#3: this handoff said the generator never writes a project whose paths pass 260
  characters, that the destination's folder counts toward the limit, and (for WP12's docs) that "the generator then
  says so". For a directory output on the same volume as staging none of that held: only staging and the folder
  beside the destination were checked, and the placing rename succeeds at any depth. True since `76de583` (fix#4).
- Correction to fix#4: its Decisions said placing a directory output "is a directory rename, which Windows performs
  however deep the files inside are, so writing never fails there", and its docs guidance gave 142 characters as the
  standard preset's limit with no word about the volume. From staging on another volume the project was copied into
  a folder `len(name) + 11` characters deeper than the destination, so a project that fits was refused there, and the
  142 held only on the volume it was measured on. True on both since `8858f08` (fix#5); the 142 is now checked on D:.

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
| placement tests patched the global `os` | non-blocking | fixed `6da43d1`: `pipeline_os(...)` (since fix#2 `seen_by_pipeline(...)`) replaces `os` in the pipeline module only |
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

## Review fix#3 (fresh-context review of `91ba5d7`)

| Finding | Severity | Outcome |
|---|---|---|
| `test_golden.py:232` the other-volume fake reached only the pipeline's `os`, so `shutil.move` (the integration head's placement) passed every test; the handoff's "Reproductions" line was stale | blocking | fixed `12ee3e8`: `os.rename` replaced for the process, refusing only a rename out of an `android-gen-*` staging folder. With `pipeline.py`'s `_move(project, finished)` swapped for `shutil.move(str(project), str(finished))` the test fails here with `PermissionError [WinError 5] ... .git\objects\02\...` (then restored with `git checkout`); on the branch all 6 placement tests pass. "Reproductions" line corrected (Measurements) |
| `copy.py:82` `fnmatch` folds case on Windows only | non-blocking | fixed `6b7e88f`: `fnmatchcase`. `test_host` case: `GRADLEW`, `tools/perf/RUN.SH` and `Tools/perf/run.sh` are not executable, `gradlew` and `tools/perf/run.sh` are; 3 subtests fail on the old code here |
| `package.py:48-53,66` scripts listed from the folder; an ignored one made `update-index` exit 128 and cost the commit | non-blocking | fixed `ae5941f`: listed from the index (`git ls-files -z`) after `git add`. `test_zip` case (an ignored `tools/perf/local.sh`): no warning, `gradlew` and `perf.sh` 100755, `local.sh` not committed; on the old code the warning is "`git update-index` failed (fatal: Unable to process path tools/perf/local.sh)" |
| `pipeline.py:150-160` every path held to the folder limit; a tree copy's message ignored | non-blocking | fixed `659fecc`: a tree copy's failed destination is held to the limit for what its source is (file 259 with its folder at most 247, folder 247); a single call keeps the folder limit (Decisions). `WindowsLongPathsTest` case: a 254-character file in a 243-character folder failing with a sharing violation is re-raised as `shutil.Error`, a 253-character folder is explained; the first subtest fails on the old code |
| `pipeline.py:237` warning carried the `\\?\` spelling | non-blocking | fixed `659fecc`: the reason only (`error.strerror`); the placement test checks "(in use)." and fails on the old code |
| `package.py:70`, `keystores.py:178` child output decoded with Python's default | non-blocking | fixed `ae5941f` (git: UTF-8) and `84e7d98` (keytool: `locale.getencoding()`; `record.py`'s `rev-parse`: UTF-8), all `errors="replace"`. The keytool half was a live crash on this machine: Python 3.15 reads pipes as UTF-8, keytool writes cp1252, and a keystore path with "Á" raised `UnicodeDecodeError ... byte 0xc1` (the new `test_keystores` case errors on the old code). The git half shows only on Python 3.11-3.14 on Windows, so it is fixed by reading, not tested here |
| handoff: no "Lock time used"; the orchestrator note ran into the next paragraph | non-blocking | fixed: section added; blank line after the note |
| `tests/links.py:28` a failed `mklink /J` hid cmd's reason | non-blocking | fixed `0bd63b4`: raises `OSError` with cmd's message (checked by hand: "Cannot create a file when that file already exists.") |

## Review fix#4 (fresh-context review of `0bd63b4`)

| Finding | Severity | Outcome |
|---|---|---|
| `pipeline.py:113` the folder beside the destination cleaned by `TemporaryDirectory`: after a failed swap, WinError 145 replaced the real error and part of the new project stayed in a hidden `.<name>.XXXXXXXX` | blocking | fixed `d178610`: `mkdtemp`, deleted through `_delete_tree`; a failed deletion is a note on the error in flight or a warning after success (Decisions). `WindowsPathLimitTest` case as the review asked: the new project at the deepest destination it fits (200 characters for the mini template's "all"), the swap refused; the error propagates and the folder holds only the old project. With `TemporaryDirectory` put back it fails here with `OSError: [WinError 145] ... \.all.vdn63ojb\all\src\main\kotlin\com\example\trail`, the review's symptom. `PlacementTest` case (every host): the deletion failing gives the warning after a success and the note after a failure, and the original `PermissionError` still propagates. CLI, the review's reproduction (destination 141 characters, deepest file 258, `settings.gradle.kts` held open, `--no-zip --force --json`): exit 1, last line `RenderError: ...\Std could not be moved aside (Access is denied); nothing was changed. A terminal, an IDE or an indexer may hold a file in it: close it and run again.`, only `Std` in its folder; released, the same command gives `ok: true`, no warnings, nothing beside |
| `pipeline.py:118` a same-volume directory output placed past MAX_PATH with `ok: true`; the handoff's claims (Decisions, docs gotcha) unsupported | blocking | fixed `76de583`: `_check_windows_path_limit` before placing (Decisions); the handoff's claims corrected under Deviations and now hold. `WindowsLongPathsTest` case (every host): one character deeper than "all" fits, the `RenderError` names the 260-character `TrailNotesApplication.kt` and the destination's folder stays empty; it fails at `0bd63b4` ("RenderError not raised"). CLI, the review's reproduction (147 characters, `--no-zip`): exit 1, `At ...\Std the project would hold a path of 264 characters: ...\AppFloatingActionButton.kt. ... or choose a shorter destination, app name or package name.`, its folder empty |
| `pipeline.py:199-200` a held-open destination reported as "The generator failed." with a traceback naming an aside that never existed | non-blocking | fixed `d79fc13`: `RenderError` naming the destination and the reason, "nothing was changed", and on Windows what usually holds it. `PlacementTest` case; fails at `0bd63b4` with the raw `PermissionError`. `--json` still reports every `RenderError` as `kind: internal` with the traceback in `detail` (the headless contract); its last line is now this message |
| `pipeline.py:238-239` a read-only replaced file not deleted on Windows | non-blocking | fixed `898b7fc`: `_delete_file` retries after clearing the flag on Windows; links unlinked as before. `PlacementTest` case (a read-only old zip); fails at `0bd63b4` with the "could not all be deleted" warning |
| `tokens/__main__.py:70-77` a CRLF-only difference shown as a diff of identical-looking lines | non-blocking | fixed `cc472a4`: "`<path>` differs only in its line endings: --write writes LF."; the CRLF test checks the whole output |
| `test_host.py:57-59` `io.open`/`codecs.open` passed the guard | non-blocking | fixed `ef11688`: both take the mode second; `codecs.open` flagged with or without an encoding (Decisions). The forms test has both; fails on the old guard |
| `WindowsLongPathsTest` and the pure-logic path-limit tests skip off Windows | non-blocking | fixed `76de583`: the class runs on every host, shown a Windows host (`seen_by_pipeline(os, name="nt")`, `_windows_long_paths_on` patched), paths with forward slashes so `os.path.dirname` splits them on either host; the new destination-depth test lives there. The tests that need Windows' own file system (`WindowsPathLimitTest`) still skip elsewhere. Not run on Linux here (no Linux host); checked by reading that nothing else in those code paths reads `os.name` before the patched checks |
| `pipeline.py:154` comprehension variable `copy` | non-blocking | fixed `5ea9a40`: `destination`, as the tuple's comment names it |
| handoff `pipeline_os(...)` at :111 and :288 | non-blocking | fixed: "What changed" names `seen_by_pipeline`; the fix#1 table row keeps the name it had then, with a note |
| reviewer's reproductions under the session scratchpad (`winrev4`) and `%TEMP%\android-gen-j8e4w32o` | non-blocking (information) | left in place: another session's files, and permanent deletion of temp files is the user's (global CLAUDE.md 2.3) |

## Review fix#5 (fresh-context review of `ecd3f80`)

| Finding | Severity | Outcome |
|---|---|---|
| `pipeline.py:228` a cross-volume directory output copied into `beside/<name>`, `len(name) + 11` characters deeper than the destination: refused although it fits, the error naming `.<name>.XXXXXXXX`; the handoff's Decisions and docs guidance unsupported | blocking | fixed `8858f08`: the output is finished under `_free_sibling(destination)`, exactly as long as the destination, so the one measurement at the destination holds on both volumes (Decisions). `WindowsPathLimitTest` case as the review asked, at the deepest destination itself rather than 5 below: the mini "all" project placed from staging on the same and on another volume (the EXDEV fake); on the code before the fix the other-volume subtest fails with `RenderError: Windows refused a path of 248 characters: ...\.all.pxhc8711\all\src\main\kotlin\com\example\trail`. A zip of 259 characters written (failed before: mkdtemp past the folder limit), 260 refused naming the destination. CLI, for real across volumes (staging in `%TEMP%` on C:, `--out` under `$OV/out/WIN` on D:, standard preset, `--no-zip --json`): 124 and 142 placed, deepest file 259 at 142, nothing beside; 143 refused, `At ...\Std the project would hold a path of 260 characters: ...AppFloatingActionButton.kt`, its folder empty. The same at the code before the fix: 142 refused, `a path of 273 characters: ...\.Std.81l9pdhe\Std\core\...`. Handoff corrected (Decisions, Deviations, docs guidance) |
| `create_project.py:119-152` notes left out of the human message | non-blocking | fixed `1b24642`: `described(error)` appends the notes on the error and on its cause. `test_cli` case, both places a note can be (the error; its cause under the path-limit explanation, shown a Windows host); fails on the old `str(error)` |
| `pipeline.py` grew to 478 lines with 14 placement helpers | non-blocking | fixed `c1bd711`: moved unchanged to `genkit/core/placement.py`; `pipeline.py` is 224 lines and differs from the integration head in a few lines |
| `pipeline.py:268` and the handoff call a dot-name hidden | non-blocking | fixed: the comment says what a leading dot does on Linux and macOS; the handoff's docs guidance says it hides nothing on Windows. The hidden attribute was not set: a leftover the user is told to delete should be visible in Explorer, and the attribute would travel with the new output into place |
| `pipeline.py:244-246` Windows hint worded for a folder, shown for a held zip too | non-blocking | fixed `8858f08`: a folder gets "may have a file in it open", a file "A program may have it open, such as an archive viewer" |
| `create_project.py:257-258` `confirm_replace` docstring stale | non-blocking | fixed `1b24642` |
| added lines past 120 columns | non-blocking | fixed `696e210` (catalogue, test_golden, test_android_target, test_host); the beside-warning line went with `8858f08`. Lines past 120 that the integration head already had are left |
| JSON on stdout newline-translated on Windows, not recorded | non-blocking | recorded: Decisions (fix#5) and the host audit, with why it is left |
| `WindowsLongPathsTest` never run on Linux | non-blocking | recorded under Open issues: the WP13 request stands, and the orchestrator reads the first ubuntu run before the next merge on top |

## Lock time used

None: no Gradle or adb in any run of this package. The suite and the host comparison ran through `memwait` only.

## Measurements

- **Acceptance after the merge** (`BASE=a402d87 $O/accept.sh`, log `logs/WIN/accept-fix6.log`, at `5a16778`): the
  whole suite through `memwait`, 290 tests in 72.1 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem
  exec-bit test). `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR byte
  against `a402d87`. Host comparison COMPARE OK (standard: 314 files, the pre-merge generator wrote CRLF into 4;
  everything-and-brand: 469 files, 96). Worktree clean. **ACCEPT OK.**
- The other-volume test against the integration head's `shutil.move` at `5a16778`: 1 test, errors=1,
  `PermissionError [WinError 5]` on `.git\objects` in the staged project (the reviewer's probe, run here).

- **Acceptance after fix#5** (`$O/accept.sh`, log `logs/WIN/accept.log`, at `696e210`, 1 min 44 s in all): the whole
  suite through `memwait`, 290 tests in 71.4 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit
  test). `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR byte; no tab or
  other control character added; `git diff --check` clean. Host comparison COMPARE OK (standard: 314 files,
  everything-and-brand: 469; only `gradlew` executable in either zip). Worktree clean. **ACCEPT OK.**
- Branch diff against the integration head after fix#5: 30 files, 1317 insertions, 104 deletions; fix#5 alone
  (`ecd3f80..696e210`, 4 commits, each with the noreply author and the trailer): 8 files, 466 insertions, 341
  deletions (most of both is the move of 215 lines from `pipeline.py` to `placement.py`).
- Each fix#5 test was run against the code before its fix and fails there: with `c1bd711`'s `placement.py`, the
  other-volume subtest (`RenderError ... 248 characters ... .all.pxhc8711`), both zip subtests (259 refused through
  mkdtemp, 260 with the old message naming `.TrailNotes.zip.XXXXXXXX`), the cross-host zip test (the old message), and
  the unplaced-output test (a success warned about its folder); `test_the_new_project_stays_when_the_old_one_cannot_all_be_deleted`
  errors there too, its fake now refusing every `rmtree` as the code no longer deletes a folder of its own on success.
  With `create_project.py` printing `str(error)` again, the `test_cli` notes test fails.
- CLI, for real across volumes (`scratchpad/win5/xvol.py`: standard preset, `--no-zip --json`, staging in `%TEMP%` on
  C:, `--out` under `$OV/out/WIN` on D:, long paths off): at `696e210` 124 and 142 placed (deepest file 241 and 259),
  143 refused with nothing created; at `c1bd711` 123 and 124 placed, 142 refused naming `.Std.81l9pdhe`. Both output
  folders were mine and went to the Recycle Bin afterwards. Two `%TEMP%` folders written during the run
  (`android-gen-8llqqyon` with `JsonCheck`, `tmppfgqesqo` with `.app.zip.onk5iog2`, the integration head's naming)
  belong to another agent's suite running at the same minute and were left alone.

- **Acceptance after fix#4** (`$O/accept.sh`, log `logs/WIN/accept.log`, at `ecd3f80`): the whole suite through
  `memwait`, 287 tests in 355.5 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit test). The
  time is the machine's load (about 1.9 GB free while it ran); `test_golden` alone takes 4-17 s here. `genkit.tokens
  --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR byte; `git diff --check` clean. Host
  comparison COMPARE OK (standard: 314 files, everything-and-brand: 469; only `gradlew` executable in either zip).
  Worktree clean. **ACCEPT OK.**
- Branch diff against the integration head after fix#4: 29 files, 1182 insertions, 94 deletions; fix#4 alone
  (`0bd63b4..ecd3f80`, 8 commits, each with the noreply author and the trailer): 5 files, 248 insertions, 31
  deletions.
- Each fix#4 test was run against the code before its fix and fails there: with `TemporaryDirectory` back in the
  pipeline, the deep failed-swap test with WinError 145 and the beside-deletion test (nothing named); at `0bd63b4`,
  the held-open test (raw `PermissionError`), the read-only zip test (the warning), the destination-depth test
  ("RenderError not raised"). The tokens and `test_host` cases fail on the old code by construction (a diff is
  printed; position 0 is a name, not a mode).
- CLI reproductions of both blocking findings (`scratchpad/win4/repro.py`, standard preset, long paths off): see
  "Review fix#4". The standard project fits at a destination of at most 142 characters here.
- A run of the destination-depth test against `0bd63b4` placed its project past MAX_PATH, and the test's plain
  temporary-directory cleanup left `%TEMP%\tmpytv3r7ef`. It was mine: its inner folder was renamed to one letter (all
  paths then under 116 characters) and the folder sent to the Recycle Bin. The test now cleans up through
  `delete_tree`, which reaches such paths. My other scratch outputs (generated projects for the reproductions, all
  paths under 260 characters) are in this session's scratchpad, `win4/{m,r,r1}`.

- **Acceptance after fix#3** (`$O/accept.sh`, log `logs/WIN/accept.log`, at `0bd63b4`, 1 min 22 s in all): the whole
  suite through `memwait`, 282 tests in 59.9 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit
  test). `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR byte. Host
  comparison COMPARE OK (standard: 314 files, everything-and-brand: 469; only `gradlew` executable in either zip).
  Worktree clean. **ACCEPT OK.**
- Branch diff against the integration head after fix#3: 29 files, 965 insertions, 94 deletions; fix#3 alone
  (`91ba5d7..0bd63b4`): 10 files, 143 insertions, 39 deletions.
- Each fix#3 test was run against the code before its fix and fails there (details in "Review fix#3").

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
  `--git --no-zip` from `%TEMP%` on C: to D: (PermissionError on `.git/objects/...` after the copy; true when written,
  then from fix#1's `6da43d1` to `91ba5d7` the test no longer reached `shutil.move`; re-verified at `12ee3e8` with the
  head's `shutil.move` swapped into the branch's pipeline: `PermissionError [WinError 5]` on `.git\objects`);
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
- `--force` over a project Windows will not let go of (a terminal or IDE inside it, or a terminal whose working
  directory it is): the generator says it "could not be moved aside" and that nothing was changed; close what holds
  it and run again. In the rare case the old project can be neither replaced nor put back, the error names the folder
  beside it that holds it: a name of a dot and random letters, as long as the destination's. A leading dot hides
  nothing on Windows (Explorer and `dir` show it); on Linux and macOS it keeps the folder out of listings.
- `--force` deletes the old project only after the new one is in place. If part of it cannot be deleted (a file an
  IDE or indexer holds open), the run still succeeds and a warning names the folder beside the destination (a dot
  and random letters) with what is left; delete it once nothing holds it. When a run fails and the new project
  cannot be deleted either, the error says "The new output was left at ..." with its path. Build files past MAX_PATH (Gradle writes them) are deleted too.
- Without long paths on, Windows takes 259 characters for a file path (247 for a folder). The generator stages in
  `%TEMP%` (about 58 characters with the user's name) and a project's deepest source file is about 96 characters plus
  the package path plus the app name, so a long package name and a long app name together can pass it; the generator
  then says so. It also measures the project at the output folder before placing it, so an `--out` path longer than
  142 characters for the standard preset (the deepest file, `core/designsystem/.../button/AppFloatingActionButton.kt`,
  is 117 characters below the project folder with `com.example.standard`) is refused with the same explanation and
  nothing is created. The limit is the same whatever volume `--out` is on (checked with `%TEMP%` on C: and `--out`
  on D:): nothing is written deeper than the destination on the way. A zip `--out` is measured the same way: up to
  259 characters, its folder at most 247. Building any Android project on Windows needs long paths for the same reason (Gradle's build
  directories are deeper still).
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
  Windows-only tests, the filesystem exec-bit test, `_delete_tree`'s plain `rmtree`, and since fix#4
  `WindowsLongPathsTest` shown a Windows host on Linux, since fix#5 also `test_cli`'s notes test) are covered by the
  simulation in `test_host` and by reading; the ubuntu CI job is the first real Linux run. Nothing is pushed, so it
  has not run. **For the orchestrator:** read that first ubuntu run's generator job before the next package merges
  on top of WIN, so a Linux-only failure is pinned on this package rather than on the one after it.
- **For WP13 (owner of `.github/workflows/**`):** add a `windows-latest` generator job (the same two steps as the
  ubuntu one). Without it the tests that need Windows' own file system (junction, 8.3 short name, `WindowsPathLimitTest`:
  the path limit in staging, the deep-file deletion and the failed swap beside a deep destination; the CR guards on a
  Windows checkout) run only on a developer's machine. Since fix#4 the pure path-limit logic (`WindowsLongPathsTest`,
  including the destination-depth refusal) runs on every host. `WindowsPathLimitTest` probes for long paths and skips
  with that reason where they are on, so the job is valid on either kind of runner image. Not done here because the
  path is WP13's and A12.7 rewrites `ci.yml`.
- `--json` reports every `RenderError` as `kind: internal`, "The generator failed.", with the message only as the
  traceback's last line in `detail` (the headless contract in `genkit/core/headless.py`, WP01's). A held-open
  destination or a path past the limit is the user's to fix, not a generator bug; a separate `kind` for such errors
  would change that contract and the Go API's reading of it (WP10: `internal` is a 500), so it is left as it is. The
  API only writes zips into its own work directory, where neither error arises.
- `_free_sibling` checks a random name is free and then the name is made: the old output renamed to it, the new one
  renamed, copied or zipped to it. Another process creating that exact name in between could be overwritten (a zip
  onto a file on any host, a folder onto an empty folder on POSIX) or, for the new output, deleted by the cleanup
  after a failure. Accepted as unrealistic; an atomic reservation would need a directory, which cannot be renamed
  over on Windows, and mkdtemp's reserved folder is what made the finished output deeper (fix#5).
