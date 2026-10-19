# WIN handoff

Branch `wp/WIN` in `$OV/wt/WIN` (`OV=D:/projects/tools/.overhaul`, `O=$OV/out/WIN`). Logs in `$OV/logs/WIN/`.
This package's agents never push. Backup pushes do not start CI (`ci.yml` runs on a push to `main` and on a pull
request, nothing else); the draft PR #1 (`wp/WIN` into the integration branch, approved by the user on Oct 19) ran
the suite on Linux once, at `c91798a`, and re-runs when `wp/WIN` is pushed (Open issues). There is no WP file: the
scope in the orchestrator's task is the specification.

## Progress

- **Orchestrator note (Oct 19, 15:56): Linux CI green.** PR #2 (ci/linux-check at `070a91b`): every GitHub check passed on Linux (Generator, API, Frontend, generated-project builds none/lean/all/everything); only Vercel's preview failed (its cause needs the user's Vercel login). PR #2 closed unmerged as planned. `a5db7b0` and `070a91b` reach the integration line through the staging consolidation (REQUIREMENTS 44), not a separate merge into overhaul/multiplatform-v2.
- **Orchestrator note (Oct 19, 15:32): PR #1 and PR #2.** PR #1 shows "merged": GitHub marked it so at 14:16 when the integration branch, pushed as a backup with WIN's merge `3a0d38e`, came to contain its head; nothing was merged through GitHub. With the user's OK (15:30), PR #2 https://github.com/tripathisarthak457/android-base/pull/2 runs Linux CI on `a5db7b0` and `070a91b` from the throwaway branch `ci/linux-check`; it is closed before these fixes are merged and pushed, so GitHub cannot mark it merged.
- **Orchestrator note (Oct 19, 13:02): the third Linux CI run (39214194825, `4746a0b`).** API green. Generator: 1 failure on Linux, `tests.test_zip.ZipTest.test_the_commit_holds_a_file_past_windows_path_limit`: right after the `--git` commit, `git status` shows ` M gradlew` and ` M tools/perf/perf.sh` (Tuples differ: (0, '', '') != (0, ' M gradlew
 M tools/perf/perf.sh
', '')): the generated repository is dirty on Linux, most likely the executable bit (the index says one mode, the working files have another, or core.fileMode differs). A user who generates with --git on Linux or macOS would start with two modified files. The four generated-project builds still fail (see the 12:18 note: `AppResult` in data/orders). Fix both with tests; the orchestrator pushes and reads the next run.
- **Orchestrator note (Oct 19, 12:18): the second Linux CI run (39210292751, `ae886b5`).** Generator and API jobs now pass. The four generated-project builds (none, lean, all, everything) generate fine now but fail to compile: `:data:orders:compileReleaseKotlin` / `compileDebugKotlin`: `Unresolved reference 'AppResult'` in `out/data/orders/src/main/kotlin/com/example/cicheck/data/orders/OrdersRepository.kt` lines 3, 18, 26. Find where the `orders` data module comes from (ci.yml's add-feature step or a generator template) and why `AppResult` is missing; if the generator's add-feature data template is stale on the integration branch, fix it here with a regression test (a generated project with an added data feature must compile), else record it for the owning package in followups.md. Vercel's preview still fails (needs the user's login to read).
- **Orchestrator note (Oct 19, 11:35): the Linux CI run (PR #1, run 39206352955) failed; fix before WIN's next merge.** (1) **Real Linux bugs, WIN's:** the generator suite on ubuntu (Python 3.12): 1 failure + 1 error in `tests/test_keystores.py`: `test_a_non_ascii_password_round_trips` (FileNotFoundError: `/tmp/.../keystore.properties` was never written) and `test_keytool_s_refusal_names_a_non_ascii_path_as_it_is` (the message does not name `/tmp/.../Ána/keys/prod.jks`; keytool's output was `Keystore 'prod' could not be created: 	for: CN=Acme, O=Acme, C=US`). Everything else passed (10 skipped). (2) **`.github/workflows/ci.yml` is stale against the overhaul** (unchanged since main's `b12122b`): the API job pins Go 1.24 while `api/go.mod` needs 1.27 ("go.mod requires go >= 1.27"), and the four generated-project jobs run `from genkit.spec import FEATURES, preset_features, resolve_features` (and `genkit.translations`), modules the generator no longer has. Decision (orchestrator): WIN updates `ci.yml` now (Go version from go.mod, the generate steps on the current generator API), so every later merge can be checked on Linux; WP13 still owns the final CI review. (3) The Vercel preview of the PR failed (`npx vercel inspect dpl_H54c7TfBopDctPGWfE9xpMArHyrz --logs` needs the user's Vercel login; cause not read). Push fixes to `wp/WIN` through the orchestrator; the PR re-runs CI on each push while it is open.
- **Orchestrator note (Oct 19, 11:22): the Linux CI run.** The user approved (asked in chat with pros and cons) a draft PR from `wp/WIN` (c91798a) into `overhaul/multiplatform-v2`, only to run CI on Linux: https://github.com/tripathisarthak457/android-base/pull/1 (draft, "do not merge"). The app's CI monitor reports the result to the orchestrator, which records the Generator job's result here and closes the PR unmerged.
- **Orchestrator note (Oct 17, 23:05).** Fix round 2 (the two blocking findings in the `--force` swap) returned done at about 16:40; the run was then paused for the user's usage reserve. Next: review round 3, then the merge.

Updated 2026-10-19 (the orchestrator's run "fix#5", on the two Linux failures left after WIN's merge `3a0d38e`: CI
runs 39214194825 at `4746a0b` and 39216375280 at `2ce104a`). Step 0: `wp/WIN` fast-forwarded to the integration head
`3a0d38e` (WIN's own merge; nothing else had landed). Both failures are fixed at their root, each with a test that
fails on the old code:

1. **The `--git` repository was dirty on Linux** (`test_zip`'s long-path test: ` M gradlew`, ` M tools/perf/perf.sh`
   right after the commit). Root cause: `git_init` (`generator/genkit/core/package.py:80-83` at `3a0d38e`) records
   every script `executable` names as 100755 with `update-index --chmod=+x` whatever the file's own mode, and left the
   file as it found it. Where `core.fileMode` is true (Linux, macOS; Windows' `git init` sets it false) `git status`
   compares the two, so the tree was clean only if the caller had already set the bit. The pipeline does
   (`genkit/core/pipeline.py:129-130`), so `create_project.py --git` was clean; the suite's fixture
   (`tests/test_zip.py` `setUp`, written 0644) does not, and that is what CI caught. Fixed in `a5db7b0`: `git_init`
   marks the scripts it records (`package.py:84-85`, `copy.mark_executable`), so the tree it hands over is clean by
   construction on every host, whatever its caller did. The long-path test's clean-status assertion is unchanged; the
   new `test_the_tree_is_clean_and_the_scripts_run_right_after_the_commit` (`test_zip.py:86`) asserts a clean
   `git status` and, where the OS has the bit, `S_IXUSR` on both scripts (Decisions, fix#5).
2. **Every generated project with a feature module failed at `:data:orders`** (`Unresolved reference 'AppResult'`,
   `OrdersRepository.kt` 3, 18, 26; in `none`, without network, 3, 16, 22, 23). Origin: not a `ci.yml` step. Its
   spec asks for `"feature_modules": ["orders"]`, and `generator/genkit/targets/android/scaffold.py` writes that
   `:data:orders` and `:feature:orders` pair from Python strings (`add_feature.py` writes the same files). Root
   cause: `scaffold.py` still wrote the APIs from before `c3895f86` (Oct 9). At `3a0d38e`: `core.common.AppResult` (lines 56, 68, 172, 395; now
   `core.common.result`), `core.common.mvi.*` (147-150, 173-174, 238, 396; now `core.common.LoadState` and
   `core.mvi.*`), `core.navigation.AppNavKey` (133; now `core.common.navigation`), a suspending `handleEvent` with
   `updateState` and `emitEffect` (189-207; now non-suspending, with `setState`, `emit`, `launchLatest`),
   `result.data` and `toLoadState()` (202-207; now `value` and `LoadState.Error(error)`),
   `loadState.message.asString()` (247, 264; now `error.text()`), `AppResult.Failure(message = …)` (435; now
   `Failure(AppError)`). Reproduced here first: CI's spec, `--preset lean`, `locked gradle
   :data:orders:compileDebugKotlin` stopped at CI's three errors (`logs/WIN/orders-before.log`). Fixed in `070a91b`,
   the smallest change that builds against today's template, not a redesign (WP02-M2 replaces this file); with
   network on, the item is also `@Serializable`, because the client decodes it with kotlinx.serialization and the
   scaffold gave it no serializer. The consistency matrix had reported the stale imports all along, under a WP02
   known issue; it no longer occurs, so its entry is gone (a full run fails on a listed issue that no longer occurs).
   Regression test: `ScaffoldTest.test_a_scaffolded_module_names_only_what_the_project_declares`
   (`tests/test_android_target.py:165`) generates the project with and without the module, network off and on, runs
   the consistency checks over both and fails on any issue the module adds: 12 on the old scaffold in each subtest,
   none now. It catches every import that resolves to nothing; a wrong member or argument needs a compile, which this
   run did through Gradle and CI's four jobs do on every pull request (their spec is this module).

Ran here: the suite through `memwait`, 307 tests OK, 4 skipped (`logs/WIN/suite-oct19-fix5.log`); the quick
consistency matrix, CONSISTENCY OK, 58 projects, 0 new issues, 323 known (575 before), 11 of 11 lines seen
(`logs/WIN/consistency-oct19-fix5.log`); `tokens --check` and `catalogue --check` current; 0 added lines with CR, none
past 120 columns, `git diff --check` clean; the host comparison against `3a0d38e`: "standard" identical, the
everything-and-brand project differs in exactly the 12 scaffolded files, zip modes the same
(`logs/WIN/compare-hosts-fix5.log`; COMPARE FAILED by design, so `accept.sh` was not used); `create_project.py --git`
on CI's spec: a clean tree here. Gradle (`locked gradle`, `--offline`) on CI's spec generated into `out/WIN/ci/`:
lean, the two orders modules assembled and their 3 ViewModel tests passed (`logs/WIN/orders-lean-modules.log`); lean
with CI's own tasks, `check assembleDevDebug assembleProdRelease` (every module's lint, unit tests and guards, the R8
release build), BUILD SUCCESSFUL in 7 min 42 s (`logs/WIN/ci-lean-full.log`); none (the scaffold's network-off
branch), `:data:orders:compileReleaseKotlin :feature:orders:testDebugUnitTest :app:assembleDevDebug`, BUILD
SUCCESSFUL, 3 tests passed (`logs/WIN/ci-none-orders.log`).
**Not verified here:** the `git_init` change on Linux or macOS (no POSIX host; Windows has no executable bit to
compare), and the `all` and `everything` presets' builds; the next CI run shows both. **For the integrator at
WP02-M2's merge:** `scaffold.py` will conflict; take WP02's version whole and keep the new `ScaffoldTest` test, which
then checks WP02's scaffold. `out/WIN/ci/` keeps the spec and the four projects this run generated (about 650 MB
with build output; nothing refers to them once this entry is read). This run pushed nothing.

Updated 2026-10-19 (the orchestrator's run "fix#4", after the fresh-context review of `4746a0b`; table "Review
fix#4 of Oct 19"). The integration branch had not moved (`4dd3aca`), so nothing was merged in. The blocking finding:
`04510e3`'s `git init --template=` kept the user's git template out of the generated repository for good, so the
hooks that pre-commit or git-secrets install through `init.templateDir` / `GIT_TEMPLATE_DIR` never ran on the user's
own later commits (the integration head's plain `git init` installed them), and the Rules line saying the user's
settings apply "from the next command" was false for a template. Fixed in `976a7e2`: the generator's commands stay
template-free; once the commit is made, the template the caller's own `git init` would use is resolved in the
caller's environment and its `hooks/` and `info/` entries are copied into `.git` as git copies them. Not a second
`git init` (it turns the SHA-1 repository into SHA-256 under the caller's `init.defaultObjectFormat=sha256`;
reproduced here). A template's own `config`, a copy that fails and a configuration git cannot read are each a
warning, and the commit stands (Decisions, fix#4). The two new tests fail on the previous code (5 failures and
errors). Non-blocking: the comments on which hooks get `GIT_DIR` corrected (code, test, Decisions) after checking
git 2.55 here; the long-path test's `git status` drops every `GIT_` variable; the temporary home's cleanup cannot
fail a finished project; CI's interim Generate step lost its rationale (`dcb0ce4`, and a `followups.md` note for
WP13); the website's bare `true` explained (`9228ddd`); the catalogue stamp names a commit with its rules
(`2ce104a`). Still open, for the orchestrator: the user's confirmation of `ee311a8` before WIN merges (Open issues).
Acceptance at `2ce104a` against `a402d87`: **ACCEPT OK**, 305 tests; website: 222 tests, `tsc --noEmit` clean
(Measurements). Worktree clean. **Not verified:** any of it on Linux; PR #1 re-runs once `wp/WIN` is pushed. This
run pushed nothing.

Updated 2026-10-19 (the orchestrator's run "fix#3", after the fresh-context review of `ae886b5`; table "Review
fix#3 of Oct 19"). The integration branch had not moved (`4dd3aca`), so nothing was merged in. The blocking finding:
fix#2 made the non-ASCII password test skip on keytool's ASCII refusal, which hid a product defect (Part D rule 15):
on JDK 17, the JDK `ci.yml` installs and the oldest the generated project supports, the generator accepted a spec it
could not carry out (no key, a warning after the fact). The skip is gone, and the defect is fixed rather than kept as
a known Linux failure: a key the generator makes now needs a printable ASCII password, refused up front by
`keystores.validate` (so by the wizard too) and by the website through a new catalogue rule,
`keystore_generated_password` (`ee311a8`); a key the user brings keeps any printable password. The old test asserted
what the product cannot deliver on JDK 17, so it is replaced: the refusal in `ValidationTest` and in a shared
validation case that the generator and the website both run (each fails on the old code), and a keytool round trip of
a password with every character the properties file escapes. **This is a product rule the reviewer wanted settled
with the user before WIN merges. It is decided here because the one alternative that keeps such passwords means
downgrading every generated key to JKS (Decisions, fix#3). For the orchestrator: put the confirmation question in
Open issues to the user; if they decline, revert `ee311a8`.** Non-blocking: the `--git` repository keeps
`core.longPaths=true` in its own config on Windows (`1ffa268`; the test fails on the old code with "Filename too
long"), two comments corrected (`f3d5c3d`), CI takes its languages from `catalogue --json` (`4746a0b`), and the `HOME`
side effect is recorded. The orchestrator's 12:18 note (the generated-project jobs stop at `Unresolved reference
'AppResult'` in `:data:orders`) is WP02's, not fixed here: the integration branch's `scaffold.py` writes the packages
from before `c3895f86`, a known issue owned by WP02 whose rewrite is already on `wp/WP02`; recorded in
`followups.md` (Decisions, fix#3). Acceptance at `4746a0b` against `a402d87`: **ACCEPT OK**, 303 tests; website: 222 tests,
`tsc --noEmit` clean (Measurements). Worktree clean. **Not verified:** any of it on Linux; PR #1 re-runs once `wp/WIN`
is pushed (Open issues). This run pushed nothing.

Updated 2026-10-19 (the orchestrator's run "fix#2", after the fresh-context review of `c91798a`; table "Review fix#2
of Oct 19"). The integration branch had not moved (`4dd3aca`, an ancestor of `wp/WIN`), so nothing was merged in.
Both blocking findings were about the `--git` commit still depending on the user's git setup: global or default
excludes files dropped `gradle-wrapper.jar` from it silently, and hooks `--no-verify` does not skip
(`prepare-commit-msg`, `reference-transaction`) refused or rewrote it. Rather than override two more settings, git
now runs with none of the caller's configuration (`04510e3`): an empty temporary home, no system file, no `GIT_`
variable but `GIT_EXEC_PATH`, no template; that closes the non-blocking `GIT_DIR`/identity finding too. Found on the
way and fixed: Git for Windows' `git add` silently skipped files past MAX_PATH (`0c7825b`, `core.longPaths=true`).
The cheap non-blocking findings are fixed (`5bff1e9`: the sibling's length and two more astral tests; the test probe
and name in `04510e3`), the rest recorded. The orchestrator's 11:35 note is handled too: keytool's error is now taken
from stdout, which the Linux run showed (`e323eed`), the older non-ASCII password test says why it fails or skips on
keytool's ASCII refusal (whether passwords must be ASCII is left open for the keystores' owner), and `ci.yml` runs
the current generator and go.mod's Go (`ae886b5`). Every new test fails on the old code here, except the two astral
ones, which pin `c91798a`'s change and fail on the code before it. Acceptance at `ae886b5` against `a402d87`:
**ACCEPT OK**, 301 tests (Measurements); worktree clean. **Not verified:**
any of it on Linux; PR #1 re-runs once `wp/WIN` is pushed (Open issues). This run pushed nothing.

Updated 2026-10-19 (the orchestrator's run "fix#1" after the fresh-context review of `4dd3aca`; table "Review of the
follow-up merge"). `wp/WIN` fast-forwarded to `4dd3aca` first. The blocking finding was in this handoff, not in the
code: its plan for the first Linux run could not happen. `ci.yml` starts on a push to `main` and on a pull request,
nothing else, so the orchestrator's backup pushes of `wp/*` and `overhaul/*` never run it; both branches have been on
origin since 03:35 today, `gh run list` shows the last CI run on 2026-09-24 (`main`) and `gh pr list --state all` no
pull request at all. Corrected in the header, here, Deviations and Open issues, with a correction appended to the WIN
entry in `merges.md` and an item in `followups.md`. **The suite has still never run on a POSIX host.** From this
machine (no WSL, no Docker) only a pull request starts that run, and creating one is the user's call (global
CLAUDE.md section 8), so none was opened (Decisions). **For the orchestrator, before the next package merges onto
`4dd3aca`:** ask the user for the draft pull request in Open issues (the command, and what it starts), then record
its Generator job here. Of the seven non-blocking findings, two are fixed in code, each with a test that fails on
the old code (the `--git` commit no longer takes the user's `core.fsmonitor` or `commit.gpgSign`, `4ba27fc`; path
lengths counted in UTF-16 code units as Windows counts them, `c91798a`), the stale names are corrected here, two are
recorded (Python 3.11's `onerror` branch for WP13's Windows job; mapped and `subst` drives in the reported path, for
WP12), and two need nothing. Acceptance at `c91798a` against `a402d87`: **ACCEPT OK** (Measurements). Worktree
clean; this run pushed nothing. **`wp/WIN` is 2 commits ahead of the integration branch, and they need a merge of
their own** (the integrator's step 0 returns once `merged/WIN` exists, as on the earlier Oct 19 follow-up).

Updated 2026-10-19 (run "run", replayed by the release workflow's resume after the merge). The integration branch
had not moved since WIN merged (`overhaul/multiplatform-v2` = `merged/WIN` = `5a16778`), so there was nothing to merge
in. One Windows host bug in the generator was still open, recorded by the orchestrator in `followups.md`
("`python -m tests.consistency` cannot run on Windows (WIN, the generator)"), and two more sat beside it. All three
are fixed on `wp/WIN`, each with a test that fails on the old code: the matrix's pool work moved to an importable
module (`6a22433`), issue lines name files with "/" on every host (`fbac839`), and a failed run deletes its folder in
`%TEMP%` (`3b892f4`). The quick matrix now runs here: CONSISTENCY OK. Acceptance at `3b892f4` against `a402d87`:
**ACCEPT OK**. Worktree clean; this run pushed nothing. **`wp/WIN` is now 3 commits ahead of
`merged/WIN` and those commits need a merge of their own**: the integrator's step 0 returns as soon as the
`merged/WIN` tag exists, so they reach the integration branch only if the integrator or the orchestrator merges
`wp/WIN` again on purpose. The integration branch is still at their parent, so that merge has no conflict, and
`git merge-tree` of `wp/WIN` against every open package branch shows
no conflict beyond the ones `merged/WIN` already has with `wp/WP02`, `wp/WP03-skel` and `wp/WP05-glass`. Acceptance and
the numbers: Measurements, "Run after the merge (Oct 19)".

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
| Oct 19: the consistency matrix's pool work in `tests/consistency/worker.py` (spawn, forkserver) | done (`6a22433`) |
| Oct 19: consistency issue lines name files with "/" on every host | done (`fbac839`) |
| Oct 19: the consistency runner deletes its `%TEMP%` folder when a run fails | done (`3b892f4`) |
| Oct 19: quick consistency matrix on this machine | CONSISTENCY OK (`logs/WIN/consistency-after.log`) |
| Oct 19: acceptance at `3b892f4` against `a402d87` | **ACCEPT OK** (`logs/WIN/accept-oct19.log`) |
| Oct 19: merge of the 3 new commits | done by the integrator (`4dd3aca`; `merges.md`, `out/integration/WIN/oct19/`) |
| Oct 19 fix#1: what starts CI corrected here, in `merges.md` and in `followups.md` | done; **the POSIX run itself waits on the user's yes to a pull request** |
| Oct 19 fix#1: the `--git` commit without the user's `core.fsmonitor` and `commit.gpgSign` | done (`4ba27fc`) |
| Oct 19 fix#1: Windows path lengths in UTF-16 code units | done (`c91798a`) |
| Oct 19 fix#1: acceptance at `c91798a` against `a402d87` | **ACCEPT OK** (`logs/WIN/accept-oct19-fix.log`) |
| Oct 19 fix#2: the `--git` commit with none of the caller's git configuration (home, system file, `GIT_` variables, template) | done (`04510e3`) |
| Oct 19 fix#2: `core.longPaths=true` for the `--git` commands | done (`0c7825b`) |
| Oct 19 fix#2: `_free_sibling` in UTF-16 units; astral tests for the walk and a rename's failure | done (`5bff1e9`) |
| Oct 19 fix#2: the Linux CI failures: keytool's error from stdout; the non-ASCII password test | done (`e323eed`); the password question is open |
| Oct 19 fix#2: `ci.yml` on the current generator and go.mod's Go | done (`ae886b5`); **not run: needs a CI run of the new head** |
| Oct 19 fix#2: acceptance at `ae886b5` against `a402d87` | **ACCEPT OK** (`logs/WIN/accept-oct19-fix2.log`) |
| Oct 19 fix#3: the skip removed; a generated key's password must be printable ASCII (generator, wizard, website, a shared validation case) | done (`ee311a8`); **to be confirmed by the user through the orchestrator** |
| Oct 19 fix#3: the `--git` repository keeps `core.longPaths=true` in its config on Windows | done (`1ffa268`) |
| Oct 19 fix#3: two overstated comments corrected; CI's languages from `catalogue --json` | done (`f3d5c3d`, `4746a0b`) |
| Oct 19 fix#3: the 12:18 note (generated projects stop at `AppResult` in `:data:orders`) | WP02's: recorded in `followups.md`, not fixed here (Decisions) |
| Oct 19 fix#3: acceptance at `4746a0b` against `a402d87`; website tests and typecheck | **ACCEPT OK** (`logs/WIN/accept-oct19-fix3.log`, `logs/WIN/tsc-fix3.log`) |
| Oct 19 fix#4: the caller's git template installed after the commit; warnings for what is not; `GIT_DIR` comments; `git status` isolation; cleanup errors | done (`976a7e2`) |
| Oct 19 fix#4: CI's interim Generate comment; the website's bare `true`; the catalogue stamp | done (`dcb0ce4`, `9228ddd`, `2ce104a`) |
| Oct 19 fix#4: acceptance at `2ce104a` against `a402d87`; website tests and typecheck | **ACCEPT OK** (`logs/WIN/accept-oct19-fix4.log`, `logs/WIN/web-tests-fix4.log`, `logs/WIN/tsc-fix4.log`) |
| Oct 19: merge of the 15 commits `4ba27fc`..`2ce104a` | done by the integrator (`3a0d38e`; `merges.md`, `out/integration/WIN/oct19c/`); the open `test_zip` finding on POSIX went to `followups.md` (WIN) for WP13 |
| Oct 19 fix#5: `wp/WIN` fast-forwarded to the integration head | done (`3a0d38e`) |
| Oct 19 fix#5: `git_init` makes the scripts it records as 100755 executable on disk; a clean-tree test | done (`a5db7b0`); **Linux: next CI run** |
| Oct 19 fix#5: the scaffolded modules on the template's current packages and MVI; known issue dropped; a test over the consistency checks | done (`070a91b`) |
| Oct 19 fix#5: suite, quick consistency matrix, CI's spec built with Gradle | suite 307 OK; CONSISTENCY OK; lean with CI's tasks and none's modules and app BUILD SUCCESSFUL (Measurements) |

## What changed (by path)

Each entry keeps the names of its round. fix#5 (`c1bd711`) moved every placement helper listed under
`genkit/core/pipeline.py` below (`_move`, `_swap`, `_aside`, `_delete_replaced`, `_delete_tree`, `_delete_file`,
`_path_too_long`, `_past_limit`, `_check_windows_path_limit`, `_past_windows_limit`, `_windows_long_paths_on`) to
`genkit/core/placement.py`, where `_aside` is `_free_sibling` and `_windows_path_limit_explained` is the public
`windows_path_limit_explained`; `_delete_beside` is gone. The test helper is `seen_by_placement` since then.

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
  on; the placement fakes patch `os` only as the pipeline sees it, `seen_by_placement(...)`), `test_cli` (resolved
  path for `..` and an 8.3 short name, junction replace, symlink skip, case-insensitive guard, the current-directory
  guard checks its reason), `test_translations` (language order), `test_tokens` (a CRLF token copy is stale until
  `--write`), `test_android_target` (every feature, a module, a brand and every language: a CR byte only in
  `gradlew.bat`), `test_host` (new: the `write_text` source rule; `is_link` of a directory, a file, nothing, a
  symlink, a junction, and of a directory with a stat module that has no reparse tags, as on Linux; no CR in any text
  file the generator copies from the real templates and variants, batch files excepted). fix#2: `tests/links.py` (not
  a test module) holds `symlink_or_skip` and `junction` for `test_cli` and `test_host`; `test_golden`'s placement
  fakes go through `seen_by_placement(module, ...)` (`os` or `shutil` as placement sees it) and add: a deletion that
  fails after the swap keeps the new project and warns; destinations named `a`, `ab` and `replaced` replaced whole as
  a folder and as a zip; on Windows with long paths off, an old project with one file at the in-place limit and one
  past it deleted whole; the kept output no longer than the destination's name; the path named from `filename2` and
  from a `shutil.Error`. `test_catalogue`: a CRLF website copy is stale until `--write`.
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
  `seen_by_placement(os, name="nt")` and a patched `_windows_long_paths_on`, with forward-slash paths, plus a project
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
- Oct 19 (after the merge), `generator/tests/consistency/` (WP01's path; WP13 extends it): `worker.py` (new) holds
  `run_one` and `reference`, moved unchanged from `__main__.py` with the `PACKAGE` they use. `python -m
  tests.consistency` runs `__main__.py` as `__main__`; workers started with spawn (Windows, macOS) or forkserver (Linux
  from Python 3.14) unpickle `run_one` by module name, and `multiprocessing.spawn._fixup_main_from_name` never imports a
  package's `__main__` in a worker (forkserver preloads `__main__` only from a path, and `-m` gives a name), so every
  worker died on "module '__main__' has no attribute 'run_one'" and the run ended in `BrokenProcessPool`. WP06 and WP04
  ran the matrix only through `out/WP06/consistency_win.py`, which imports the runner under its own name; that wrapper
  is no longer needed once this merges. `__main__.py` also deletes its `%TEMP%` folder in a `finally`, so a failed run
  no longer leaves the reference project behind. `checks.py`: MANIFEST, XML (two sites), RES (the XML half),
  TRANSLATION and EMPTY-FILE issue lines name the file through `Project.relative` or `as_posix()`, as every other kind
  already did; with `Path.relative_to` they read `app\src\...` on Windows, which `known_issues.toml` (written with
  "/") and the runner's `{pkg}` substitution do not match. Tests in `tests/test_consistency.py`: `IssuePathTest` (each
  of those kinds on a hand-made module, file part compared exactly), `RunnerTest` (the function the pool gets is not
  defined in the runner's module; a run that fails before the pool leaves no folder). The `__main__.py` changes avoid
  the two hunks `wp/WP02` has in that file (an import and the architecture list), so WP02-M2's merge stays clean.
- Oct 19 fix#1: `genkit/core/package.py`: every `_git` call passes `-c core.fsmonitor=` (empty, which every git
  reads as off), and the commit passes `-c commit.gpgSign=false` with the generator's identity (`as_the_generator`,
  was `identity`). `genkit/core/placement.py`: `_windows_length(path)` (UTF-16 code units, `surrogatepass` for a lone
  surrogate) in `_past_limit`, `_path_too_long`, the choice of the longest path and the three messages that give a
  length. Tests: `test_zip` (a `GIT_CONFIG_GLOBAL` with a monitor hook that leaves a trace and `commit.gpgSign`
  through a gpg that is not there: no warning, no trace, the commit made; skips on a git older than 2.32, which does
  not read that variable), `test_golden` (`WindowsLongPathsTest`, every host: a zip path of 259 code points ending in
  an emoji, 260 UTF-16 code units, refused before anything is written, as 260).
- Oct 19 fix#2: `genkit/core/package.py`: `git_init` runs every git command with `_without_the_callers_configuration(home)`
  (this process's environment without any `GIT_` variable but `GIT_EXEC_PATH`, with `HOME` and `XDG_CONFIG_HOME`
  set to an empty temporary folder and `GIT_CONFIG_NOSYSTEM=1`), `git init --template=`, and
  `-c core.longPaths=true`; the `core.fsmonitor`, `core.autocrlf` and `commit.gpgSign` overrides and `--no-verify`
  are gone. `_git(project, environment, config, *arguments)` takes the environment. `genkit/targets/android/keystores.py`:
  a refused key's warning quotes keytool's stdout first. `genkit/core/placement.py`: `_free_sibling` sizes by
  `_windows_length`. `.github/workflows/ci.yml`: Go from `web/api/go.mod`, the catalogue step on
  `genkit.core.catalogue --json`, the Generate step through `create_project.py`. Tests: `test_zip` (the fixture
  gains `gradle/wrapper/gradle-wrapper.jar`; `Hostile`, a configuration that would cost, change or leave a trace on
  the commit, given through `GIT_CONFIG_GLOBAL` and through `HOME`/`XDG_CONFIG_HOME`; the variables a hook
  inherits plus `GIT_TEMPLATE_DIR`, `GIT_CONFIG_COUNT` and `GIT_DEFAULT_HASH`; a file 300 characters deep; each
  commit must hold every file on disk, the generator's identity and message, no `gpgsig` or `encoding` header, a
  SHA-1 tree, and no hook may have run), `test_golden` (`WindowsLongPathsTest`: an emoji in a project's destination,
  in a rename's failed path, and in `_free_sibling`'s destination), `test_keystores` (`RefusalTest`; the non-ASCII
  password test asserts no warnings and skips on keytool's ASCII refusal).
- Oct 19 fix#3: `genkit/targets/android/keystores.py`: `GENERATED_PASSWORD_PATTERN` (`^[ -~]*$`, printable ASCII);
  `validate` refuses a generated key's store password outside it. `genkit/targets/android/target.py`: the catalogue
  rule `keystore_generated_password`. `web/frontend/lib/catalogue.json`: regenerated with `--write`.
  `web/frontend/features/catalogue/types.ts`: `Rules.keystore_generated_password`.
  `web/frontend/features/configurator/validation/keystores.ts`: `passwordProblem(password, rules, generated)` applies
  the rule to a generated key; `keystoreProblem` passes whether the entry brings its own key. `BuildStep.tsx`: the
  signing panel's fields are generated keys (`true`). `tests/fixtures/validation_cases.json`: "generated keystore
  password outside ASCII" (refused) and "existing keystore with passwords outside ASCII" (accepted), run by
  `test_spec` and the website's `validate.test.ts`. `tests/test_keystores.py`: the skip is gone;
  `test_a_non_ascii_password_round_trips` is replaced by `test_a_password_the_properties_file_escapes_round_trips`
  (`"  pass\\word=1:#!"` through `write` and `keytool -list`); `ValidationTest` gains the refusal, every printable
  ASCII character for a generated key, and a brought key's non-ASCII passwords; the properties test with non-ASCII
  passwords is now about a brought key. `genkit/core/package.py`: `git config core.longPaths true` right after
  `git init` on Windows, in place of `-c core.longPaths=true` on every command; the configuration comment names a
  clean filter instead of CRLF. `tests/test_zip.py`: the long-path test also runs `git status --porcelain` with no
  system or global configuration and expects no output; the variables test's comment. `.github/workflows/ci.yml`:
  the Generate step takes the languages from `python -m genkit.core.catalogue --json`.
- Oct 19 fix#4: `genkit/core/package.py`: after the commit, `git_init` returns `_install_the_callers_template(project)`:
  `_callers_template` takes `GIT_TEMPLATE_DIR` from this process's environment, else runs `git config --path --get
  init.templateDir` in the new repository with this process's environment minus `_ANOTHER_REPOSITORY` (`GIT_DIR`,
  `GIT_COMMON_DIR`, `GIT_WORK_TREE`, `GIT_INDEX_FILE`, `GIT_CONFIG`); exit 1 or an empty value means none.
  `_copy_missing` copies the template's `hooks/` and `info/` into `.git` as `git init` does (dot names skipped,
  folders merged, existing files kept, `shutil.copy` with `follow_symlinks=False`, so modes and links as they are).
  Warnings: a failed copy (with the reason), a `git config` failure, and a template `config` file, which is not
  applied. `_GitFailed` carries `returncode`. The temporary home is a `TemporaryDirectory(...,
  ignore_cleanup_errors=True)`. The configuration comment says which hooks get which `GIT_` variable.
  `tests/test_zip.py`: `test_the_repository_gets_the_users_template_once_the_commit_is_made` (through
  `init.templateDir = ~/template` in an isolated home and through `GIT_TEMPLATE_DIR`: no hook ran for the commit,
  which holds the jar; `.git/hooks/*` byte-equal to the template's and 0755 where the OS has exec bits;
  `.git/info/exclude` is the template's; the user's next commit runs their `pre-commit`, which refuses it),
  `test_a_template_the_repository_cannot_take_whole_is_a_warning` (a refused copy, an unreadable global
  configuration, a template `config` holding `core.hooksPath`: one warning each, the commit made, `.git/config`
  without `hooksPath`), the helper `isolated(home)` (no system file, no `GIT_` variable but `GIT_EXEC_PATH`, the home
  for `HOME` and `XDG_CONFIG_HOME`), used by the long-path test's `git status` too; the variables test's comment.
  `.github/workflows/ci.yml`: the Generate step's comment keeps only where the languages come from.
  `web/frontend/features/configurator/steps/BuildStep.tsx`: the comment beside the password input says why every
  password is checked as a generated key's. `web/frontend/lib/catalogue.json`: regenerated with `--write` (the stamp).

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
- fix#3 changed no public signature (`package._git`, `package._GitFailed` and `pipeline._past_limit`, now
  `placement._past_limit`, are private).
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
- Oct 19: no generator API changed. In the test tooling, `tests.consistency.worker.run_one(name, raw, directory, keep)
  -> tuple[str, list[str]]` and `tests.consistency.worker.reference(directory) -> Project` (moved, same signatures),
  and `tests.consistency.worker.PACKAGE`, all three imported by `tests.consistency.__main__`.
- Oct 19 fix#1: none (`package._git` and `placement._windows_length` are private). `pipeline.build` with `git_init`
  now commits whatever the user's `core.fsmonitor` and `commit.gpgSign` say.
- Oct 19 fix#2: none public (`package._git`'s parameters and `package._without_the_callers_configuration` are
  private). `pipeline.build` with `git_init` now commits with no system, global or template configuration and no
  `GIT_` variable of the caller's, and with files past MAX_PATH on Windows.
- Oct 19 fix#3: added `genkit.targets.android.keystores.GENERATED_PASSWORD_PATTERN`, published as the catalogue rule
  `rules.keystore_generated_password`. `keystores.validate` raises `SpecError` (field `android.keystores`) for a
  generated key's store password outside printable ASCII. Website: `passwordProblem(password, rules, generated)`
  (was `passwordProblem(password, rules)`). `pipeline.build` with `git_init` leaves `core.longPaths=true` in the new
  repository's config on Windows.
- Oct 19 fix#4: none public (`package._install_the_callers_template`, `_callers_template`, `_copy_missing`,
  `_ANOTHER_REPOSITORY` and `_GitFailed.returncode` are private). `pipeline.build` with `git_init`: after the commit
  the repository has the hooks and `info/` of the caller's git template, and the result's warnings can name a
  template that was not copied or whose `config` was not applied.

## Decisions

- **Oct 19 fix#5: `git_init` owns the executable bit on disk, not only in the commit.** The review at the merge
  (`followups.md`, WIN) proposed fixing the fixture instead: mark the test's scripts in `setUp`, as
  `render_template` does. That makes the test pass by copying what today's caller happens to do, and leaves
  `git_init`'s result depending on it: any caller that writes a script after `render_template` (a target's
  `transform` or `finish`), or calls `git_init` on its own as the tests do, would hand the user a dirty tree on Linux
  and macOS with no failure anywhere. The function that decides the commit's modes now makes the files agree with
  them; marking an already executable file again changes nothing. The marking happens after `git add`, on the paths
  the index holds, as the `--chmod` does; the changed ctime only makes git re-read those two files once, and their
  content and mode match the commit.
- **Oct 19 fix#5: the scaffold fixed here, in WP02's path, at the orchestrator's direction.** fix#3 left it to
  WP02-M2 (Decisions, fix#3), whose rewrite on `wp/WP02` (`snippets/scaffold/**`) targets WP02's own template and
  cannot merge before WP02 does; until then every generated-project CI job, and every user who names a feature
  module or runs `add_feature.py`, gets a project that does not compile. The change is the touch-for-compile kind:
  the same files and shape, only the names and calls the current template has, plus `@Serializable` on a
  network-backed item, which the client decodes with kotlinx.serialization and which had no serializer. The test sits on the
  consistency checks rather than on a Gradle build, because the suite has no Gradle; it compares against the same
  project without the module so that the template's own known issues do not count against the scaffold.

- **Oct 19: the consistency runner's Windows failure fixed here, not left for WP13.** `followups.md` routed it to
  WP13, which runs last; but it is a host bug in the generator, the class this package exists for, and every package
  that runs the matrix on this machine before WP13 needs it (WP06 and WP04 carried a wrapper). The fix is the
  smallest one that keeps `wp/WP02`'s two hunks in `__main__.py` mergeable: only the worker's functions moved, not the
  whole runner the follow-up suggested (`runner.py` with a two-line `__main__.py` would have conflicted with WP02-M2's
  merge). The structural test (the function's module is not the runner's) is what can run in a unit suite: the
  failure needs `-m` and a spawned worker, and the quick matrix that shows it takes minutes, so the matrix runs before
  and after are recorded in Measurements instead.

- **Oct 19 fix#1: no pull request opened for the first POSIX run.** A pull request is the only event short of a push
  to `main` that starts `ci.yml`, and creating one needs the user's yes (global CLAUDE.md section 8: ask before
  creating or merging PRs; the standing OK of Oct 17 covers non-main backup pushes only). Agents in these workflows ask
  nobody, so the request goes to the orchestrator, with the command, under Open issues. The form recommended there is
  a draft pull request from `wp/WIN` into `overhaul/multiplatform-v2` rather than the review's into `main`:
  `pull_request:` has no branch filter, GitHub runs it on the merge of head into base, which is exactly the next merge
  of this branch, the `deploy` job needs a push to `main`, and nothing about it reads as a release. A cloud session on
  the repository would be Linux too, but the app reports no cloud environment for this project folder
  (`no_readable_git_checkout`), and starting one is also the user's call. A local POSIX run is not possible: Git
  Bash's tools are MSYS, and Python here is the Windows build.
- **Oct 19 fix#2 (supersedes fix#1's entry on the monitor and signing): the `--git` commit runs git with none of the
  caller's configuration.** Fix#1 overrode settings one at a time (`core.autocrlf`, `core.fsmonitor`,
  `commit.gpgSign`) and said these were "the rest of the user's configuration that could change or cost the first
  commit", and that hooks could stay the user's because `--no-verify` skips the two that can refuse the commit. Both
  were wrong (fix#2 review): a global excludes file, git's default one (`$XDG_CONFIG_HOME/git/ignore`) or a
  template's `info/exclude` holding `*.jar` left `gradle-wrapper.jar` out of the commit with no warning;
  `prepare-commit-msg` is not skipped by `--no-verify` and refused or rewrote the commit, and `reference-transaction`
  refuses even `git init` (seen in this run's test on the old code). The environment was the caller's too: with
  `GIT_DIR`, `GIT_WORK_TREE` or `GIT_INDEX_FILE` set, each command ran against another repository's files and the run
  reported success; `GIT_AUTHOR_*` replaced the identity. Which hooks get which (corrected in fix#3 and again in
  fix#4, checked here with git 2.55.0.windows.5): `pre-commit`, `commit-msg` and `post-commit` get `GIT_INDEX_FILE`,
  `GIT_AUTHOR_*` and `GIT_CONFIG_PARAMETERS` (with a `-c`), and `GIT_DIR` only in a linked worktree (the setup this
  overhaul uses) or a bare repository; `post-checkout` and `pre-push` get none of them; `GIT_WORK_TREE` comes only
  from whoever set it. So `04510e3`'s message ("a generator run from one (the suite in pre-push) committed the
  project into that other repository") overstates it: from a `pre-push` hook in a main worktree it did not; from a
  commit hook in a linked worktree it did. The fix strips every `GIT_` variable either way. The commit is on origin,
  so it is not reworded. So the class is closed rather than
  its members: every git command in `git_init` runs with an empty temporary home (`HOME` and `XDG_CONFIG_HOME`: no
  `~/.gitconfig`, no XDG config, excludes or attributes file, on every git version; `GIT_CONFIG_GLOBAL` would need
  2.32), `GIT_CONFIG_NOSYSTEM=1`, no `GIT_` variable but `GIT_EXEC_PATH` (which only says where git's own programs
  are), and `git init --template=` (no hooks, no `info/exclude`). What the commit holds depends on the project's own
  `.gitattributes` and `.gitignore` alone; the per-setting overrides and `--no-verify` went, being moot. Considered
  and rejected: the review's narrower `-c core.excludesFile= -c core.hooksPath=<missing>`, which closes the two
  routes found and leaves `core.attributesFile` (a filter on every file), `init.defaultObjectFormat` (a SHA-256
  repository older tools cannot read), `i18n.commitEncoding` and the next one; all of those are in the test now.
  Turning off hooks the user installed is now the generator's call: the commit is the generator's, under its own name,
  and a hook written for the user's repositories has nothing to check in a project that did not exist a second ago.
  (Fix#4: for that one commit only. `--template=` also kept the user's template out of the repository for good,
  which was wrong; the next entry installs it once the commit is made.)
  Without the system file `core.autocrlf` is unset, which is false on every host, Git for Windows included (its `true`
  comes from that file), so line endings are as before.
  Side effect (fix#3 review): `HOME` is the empty folder for every git child, so a git that is an asdf or mise shim,
  which finds its data under `$HOME`, fails to run, and `--git` warns that the project is fine and leaves it without
  a repository. Accepted: such shims for git are rare, the failure is graceful and says so, and passing the real home
  back would bring `~/.gitconfig` back for every git older than 2.32 (no `GIT_CONFIG_GLOBAL`). A behaviour change
  from inheriting `HOME`, so it is in the Rules too.
- **Oct 19 fix#4: the repository gets the caller's git template once the commit is made.** A template
  (`init.templateDir`, `GIT_TEMPLATE_DIR`) is how pre-commit (`init-templatedir`) and git-secrets put their hooks into
  every new repository, so the commit-time isolation must not outlive the commit (the review reproduced the loss
  against the integration head, which installed them). Options: (1) copy the template's entries after the commit,
  chosen; (2) run `git init` again in the caller's environment, rejected: under `init.defaultObjectFormat=sha256` a
  re-init rewrites a SHA-1 repository's `extensions.objectformat` and `git log` then fails with "your current branch
  appears to be broken" (reproduced here, git 2.55.0.windows.5); (3) leave it out and say so, rejected: the user's
  secret scanning would silently not run until they noticed. The template is resolved as the caller's `git init`
  would: `GIT_TEMPLATE_DIR` first (an empty one means none), else `git config --path --get init.templateDir` run in
  the new repository (its own file holds no `init` key, so this reads the system and global files and `-c`, as `init`
  does; `--path` expands `~`) with the caller's environment minus `GIT_DIR`, `GIT_COMMON_DIR`, `GIT_WORK_TREE`,
  `GIT_INDEX_FILE` and `GIT_CONFIG` (which would point it at another repository's configuration or, for
  `GIT_CONFIG`, make `git config` alone read another file); the caller's `GIT_CONFIG_GLOBAL`, `GIT_CONFIG_NOSYSTEM`
  and `GIT_CONFIG_PARAMETERS` stay, since `git init` honours them. git's built-in template (no setting) holds only
  `*.sample` hooks and a comment-only `info/exclude`, so it is not copied; a template that does not exist is skipped,
  as git only warns about it. What is copied is `hooks/` and `info/`, the parts tools use, with `git init`'s rules:
  names starting with a dot left out, folders merged, files `.git` already has kept (it has none there after
  `--template=`), modes and symbolic links as they are. The template's `config` is not applied: `git init` starts the
  repository's configuration from it before any format is set, and merged in afterwards it could declare an object
  format or repository version the repository does not have; it is named in a warning instead. Its `description`
  (gitweb) and legacy `branches/` are not copied, silently, as nothing reads them. A failed copy and a configuration
  git cannot read are warnings too, and the commit stands. Side effect: since `04510e3` a generated repository has no
  `.git/hooks`, `.git/info` or `.git/description` unless the user has a template; before it, git's default template
  gave the sample hooks and a comment-only `info/exclude`, so `echo x >> .git/info/exclude` there now needs the
  folder first. Accepted: nothing reads the samples, and a template's `info/exclude` is copied.
- **Oct 19 fix#2: the `--git` commands take `core.longPaths=true`.** Ignoring the user's configuration also drops a
  global `core.longPaths=true`, and without it Git for Windows skips a file past MAX_PATH with "Filename too long"
  and `git add` still exits 0 (checked here, git 2.55, `LongPathsEnabled=0`). With long paths on in Windows the
  generator writes such files in staging, so the commit silently lacked them, before this package too for anyone
  without that global setting. With the key Git for Windows reaches them in the extended-length form whatever the
  registry says; other gits ignore it. Tested on every host (`test_zip`).
  Since fix#3 (`1ffa268`, a non-blocking review finding) the key is in the new repository's own config on Windows,
  written right after `git init`, rather than on each command line: the user's later commands there need it as much
  as the commit did. Without it, `git status` under Git for Windows' defaults printed "Filename too long" for each such
  file (the extended test on the old code). The other hosts' repositories keep git's default config.
- **Oct 19 fix#1: a path's length is what Windows counts.** MAX_PATH counts UTF-16 code units, two for a character
  outside the Basic Multilingual Plane, so placement measures with `_windows_length` and reports that number, which
  keeps "a path of N characters" consistent with the limit the same message quotes. Since fix#2 `_free_sibling` sizes
  its ASCII name by `_windows_length` too, so the sibling is exactly as long as the destination as Windows counts
  (never longer in UTF-8 bytes either, for a POSIX file system's name limit); before, it erred short for an emoji.
- **Oct 19 fix#2: keytool's error is on stdout.** The first Linux CI run failed WIN's own keytool test: the warning
  quoted keytool's progress ("for: CN=...") from stderr. keytool prints `keytool error: ...` on stdout; on Linux a
  folder where the key should go has a non-zero length, so keytool generates the key pair, says so on stderr and fails
  on the write, while on Windows the folder reads as an empty keystore and fails before any progress. The warning
  now takes stdout first; `RefusalTest` replays the Linux output on every host.
- **Oct 19 fix#2: the non-ASCII password test skips where keytool refuses such a password, and says why otherwise.**
  (Superseded by fix#3, next entry. The skip was wrong: it hid a product defect, which Part D rule 15 forbids, and the
  plan to read its reason in CI could not work: `ci.yml` runs unittest at verbosity 1, which prints `OK (skipped=N)`
  and no reasons.)
  `test_a_non_ascii_password_round_trips` is older than WIN (`ff9c777`) and failed on the CI's Linux runner (Temurin
  17 is that image's default JDK) with no message: keytool made no key. Most likely older JDKs' PKCS12 store goes
  through the "PBE" key factory, whose `PBEKey` refuses any character outside printable ASCII ("Password is not
  ASCII"); JBR 25 here accepts it. Not reproducible on this machine (no other JDK, and installing one is not granted),
  so the test now asserts no warnings (a failure quotes keytool) and skips only on that exact refusal. Not decided
  here, because it changes the spec and the website's rules: whether the generator should refuse non-ASCII keystore
  passwords up front, since a key made with a newer JDK may not open under the JDK a project's Gradle runs on (Open
  issues).
- **Oct 19 fix#3: the generated-project CI jobs' compile error is left to WP02 (the orchestrator's 12:18 note).**
  CI run 39210292751 (`ae886b5`) generated all four projects and stopped at `:data:orders`: `Unresolved reference
  'AppResult'`. `orders` is the CI spec's `feature_modules` entry, so the code is `scaffold.py`'s, which still imports
  `{pkg}.core.common.AppResult` (and `core.common.mvi.*`, `core.navigation.AppNavKey`), while `c3895f86` (Oct 9, in
  the integration branch) moved `AppResult` to `core.common.result` and MVI to `:core:mvi`. That is a known issue:
  `tests/consistency/known_issues.toml` matches exactly these IMPORT findings, owner WP02, "WP02-M2 rewrites it", which
  is why the matrix and the suite pass. `wp/WP02` has the rewrite (`snippets/scaffold/**` with the current packages)
  and drops that entry. The note asked for a fix here if the template was stale on the integration branch, else a
  follow-up for its owner. It is stale, but the fix is the owner's and already exists. Patching the imports here
  would not be enough, because the scaffold writes the old MVI shape and not only the old packages. It would
  duplicate WP02-M2's rewrite in WP02's own `scaffold.py` and conflict with that merge, where the owner wins. So it is
  recorded in `followups.md` (CI section): those four jobs pass only once WP02-M2 merges, and the CI spec is the
  regression test. Removing `feature_modules` from the CI spec to turn it green would weaken CI, so it was not done.
- **Oct 19 fix#3: a key the generator makes takes a printable ASCII password; a key the user brings keeps any
  printable one.** Evidence: on Temurin 17 (the JDK `ci.yml` installs, and the floor the generated project's
  `AGENTS.md` names, "any JDK 17+") keytool made no key from `pässwörd1`, while every ASCII password in the same run
  worked (run 39206352955). JDK 17's PKCS12 store gets its keys from the "PBE" `SecretKeyFactory`, whose
  `com.sun.crypto.provider.PBEKey` throws "Password is not ASCII" for any character outside 0x20-0x7E, and its MAC
  check on load takes the same key, so such a store can be neither made nor opened there. That part comes from the
  JDK 17 sources and was not run: there is no JDK 17 on this machine, and installing one is not granted. JBR 25 has
  dropped the check: a probe here (the "PBE" factory, and a PKCS12 store and load with `pässwörd1` and with U+007F)
  passes on 25.0.3. Options: (1) refuse up front, chosen; (2) keep such passwords and make the round trip pass on JDK
  17, which needs a store format JDK 17 opens with them, i.e. the legacy JKS (keytool warns that it is proprietary),
  for every generated key because of a rare password; (3) record a known failure and wait. (3) leaves the generator
  accepting a spec it cannot carry out on a supported JDK, and PR #1 red until someone decides, while the user asked
  to keep the PR open until Linux CI is green (decision log, 11:40). Pros of (1): one error, with its reason, in the
  CLI, the wizard and the website alike; no format change; the round trip holds on every JDK from 17. Cons and
  catches: a non-ASCII password typed by hand is refused (the passwords the wizard and the website invent are ASCII
  already); a key the user brings is not checked, because the generator never opens it and it already works where
  they use it (JKS takes any password on every JDK), so a non-ASCII PKCS12 key brought from a newer JDK still fails
  under a Gradle on 17, as it would without the generator; an old cached copy of the website's catalogue lacks the
  rule, and the generator behind the API still refuses. Where: `keystores.validate` with `GENERATED_PASSWORD_PATTERN`,
  exported by the Android target as `rules.keystore_generated_password`; `passwordProblem(password, rules, generated)`
  on the website; two cases in `tests/fixtures/validation_cases.json` that both sides run. The replaced test was not
  weakened: it asserted what the product cannot deliver on its oldest JDK; the refusal is now tested on both sides, and
  the keytool round trip uses `"  pass\\word=1:#!"`, the characters `keystore.properties` must escape or leave alone.
  `properties_value` keeps its `\uXXXX` branch, which a brought key's password still needs (tested). The paths are
  WP02's (`keystores.py`, `target.py`, after WP01) and WP10's (the website, merged), one hunk each; against every open
  branch the only new merge conflict is `web/frontend/lib/catalogue.json`, which is regenerated with `--write`
  (Measurements). **Not yet the user's:** the reviewer asked for this product rule to be settled with the user through
  the orchestrator before WIN merges. It is decided here because the orchestrator's instruction for this run is to
  decide soundly while the user is away and (2) is not a real alternative; the question is in Open issues, and if the
  user declines it, reverting `ee311a8` brings back the JDK 17 failure as a known Linux failure.
- **Oct 19 fix#2: `ci.yml` updated by this package.** The orchestrator's decision (Progress, Oct 19 11:35). The
  smallest change that lets every job reach its real work: Go from `web/api/go.mod` (`go-version-file`), the catalogue
  step on `genkit.core.catalogue --json`, and the Generate step through `create_project.py` (`--all`, `--preset`, or
  no features; every language for `all`) rather than the generator's modules, which is what changed under it. The
  matrix, Java 17 and the build step are unchanged: WP13 owns the workflows and reviews them last.

- **Which path the CLI reports: the canonical one, directories resolved, last part as given.** `absolute()` already
  resolved the directories so that `--force` replaces a link rather than what it points at, and that is the path the
  build acts on, so it is the honest answer. The test expected the caller's spelling, which held only where `%TEMP%`
  has one name (on this machine it is `C:\Users\CRAZYH~1\...`, and on macOS `/var` is a link to `/private/var`). The
  tests now compare with `Path.resolve()` of the directory they made, and pin `..` and a deliberate 8.3 name. Not
  loosened: each comparison is still exact. On Windows resolving also turns a mapped network drive (`Z:\...`) into
  its UNC form (`\\server\share\...`) and a `subst` drive into the folder behind it (Oct 19 review). Kept: that is
  the path the build writes to and the path-limit check measures, so a refusal names what was measured. WP12's
  gotchas say so. Not reproduced here: making a mapped or `subst` drive is a system change.
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
  touched. The other fakes keep `seen_by_placement`.
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
| process start | `python -m tests.consistency` hands its pool a function defined in the package's `__main__` (spawn on Windows and macOS, forkserver on Linux from 3.14) | fixed Oct 19 (`6a22433`): `tests/consistency/worker.py`; tested; quick matrix run here |
| separators | five consistency issue kinds printed `Path.relative_to` (backslashes on Windows) | fixed Oct 19 (`fbac839`); tested |
| other | a failed consistency run left its reference project in `%TEMP%` | fixed Oct 19 (`3b892f4`); tested |
| process start | other pools in `generator/` and `tools/` | checked: the only other is `check_versions.py`'s `ThreadPoolExecutor`, which pickles nothing |
| path length | lengths counted in code points where Windows counts UTF-16 code units (an emoji counts twice) | fixed Oct 19 (`c91798a`); tested on every host |
| other | the user's `core.fsmonitor` (a hook, or Git for Windows' daemon on the staged repository) and `commit.gpgSign` reached the `--git` commit | fixed Oct 19 (`4ba27fc`); tested. Fix#1 called these the rest of the configuration that could change the commit, which was wrong: see the next row |
| other | everything else of the caller's git setup reached the `--git` commit: excludes files (the wrapper's jar left out), hooks `--no-verify` does not skip, a template, attributes and filters, the object format, the commit encoding, and the `GIT_` variables a hook exports (the commit made in another repository) | fixed Oct 19 fix#2 (`04510e3`): no system, global or template configuration and no `GIT_` variable; tested through `GIT_CONFIG_GLOBAL`, `HOME`/`XDG_CONFIG_HOME` and the variables |
| other | `04510e3`'s `--template=` kept the user's template (pre-commit's or git-secrets' hooks, `info/exclude`) out of the repository for good, not only out of the commit | fixed Oct 19 fix#4 (`976a7e2`): its `hooks/` and `info/` copied after the commit; what is not applied is a warning; tested through `init.templateDir` and `GIT_TEMPLATE_DIR` |
| file systems | the empty home also drops a global `safe.directory`, which users of FAT, exFAT or network drives set because git cannot see an owner there ("detected dubious ownership") | checked Oct 19 fix#4, no change: `git_init` runs in staging under the temp folder (`pipeline.build`), which the user owns, before the project is moved; only a temp folder on such a drive would refuse it, gracefully (the "project itself is fine" warning). `-c safe.directory=*` would cover that on git 2.55 (checked here with `GIT_TEST_ASSUME_DIFFERENT_OWNER=1`), but not on gits that ignore it on the command line, and it was not worth an untestable path |
| path length | Git for Windows' `git add` skips a file past MAX_PATH and exits 0, so with long paths on the `--git` commit lacked the deepest files | fixed Oct 19 fix#2 (`0c7825b`): `core.longPaths=true`; tested on every host. Fix#3 (`1ffa268`): kept in the new repository's config on Windows, so the user's next command sees those files |
| other | a generated key's password outside ASCII: JDK 17's keytool makes no key, and a newer JDK's key does not open in a build on 17 (Linux CI) | fixed Oct 19 fix#3 (`ee311a8`): refused up front by the generator and the website; tested on both |
| process output | keytool's error is on stdout, its progress on stderr; the warning quoted stderr's last line (seen on Linux CI) | fixed Oct 19 fix#2 (`e323eed`); tested on every host |

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
- Correction to the Oct 18 and Oct 19 updates (Open issues, Review fix#5's last row) and to the integrator's 01:35
  WIN entry in `merges.md`: they expected the ubuntu CI job to run after the next backup push. `ci.yml` starts on a
  push to `main` and on a pull request only, so no backup push runs it, and both branches were already on origin
  without a run. The first POSIX run needs a pull request (Decisions, Open issues).
- Correction to fix#2 of Oct 19: the non-ASCII password test's skip and the plan to confirm its cause from the CI log
  (verbosity 1 prints no skip reasons). Replaced in fix#3 by the rule above; the cause stays inferred (Decisions).
- Oct 19 fix#3 edits paths owned by others beyond touch-for-compile: `keystores.py` and `target.py` (WP02), the
  website's validation and type (WP10, merged), and the shared validation fixture (WP01's). The orchestrator routed
  the Linux keystore failure to this package (Progress, 11:35); a rule the generator and the website enforce has to
  change in both, and one hunk each keeps the merges simple.

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

## Review of the follow-up merge (fresh-context review of `4dd3aca`, Oct 19)

| Finding | Severity | Outcome |
|---|---|---|
| handoff and `merges.md`: the first Linux run expected from the next backup push, but `ci.yml` starts only on a push to `main` and on a pull request; "Nothing pushed" stale | blocking | corrected: header, Progress, Deviations and Open issues here, a correction appended to the 01:35 WIN entry in `merges.md`, an item for the orchestrator in `followups.md`. The run itself needs a pull request, so the user's yes (Decisions); none opened. Checked: `ci.yml` (`push: branches: [main]`, `pull_request:`), `origin/wp/WIN` = `3b892f4` and `origin/overhaul/multiplatform-v2` = `4dd3aca` (both pushed Oct 19 03:35, local reflog), `gh pr list --state all` empty, `gh run list` last run 2026-09-24 on `main` |
| handoff: `seen_by_pipeline` and the placement helpers under `pipeline.py` | non-blocking | fixed: `seen_by_placement` where "What changed" and Decisions describe the tests; one paragraph at the top of "What changed" says where fix#5 moved the helpers; the review tables keep their round's names |
| `placement.py:91,103,137-139` lengths in code points, Windows counts UTF-16 units | non-blocking | fixed `c91798a` (Decisions). `WindowsLongPathsTest` case (every host): a zip path of 259 code points and 260 units refused up front as 260; on the old code Windows refused the final rename and the message called the finished sibling "a path of 259 characters" |
| `placement.py:280-283` the Python 3.11 `onerror` branch has never run | non-blocking | recorded: the WP13 request (Open issues, `followups.md`) now asks for a Python 3.11 run in the `windows-latest` job. Not run here: this machine has 3.15 only, and installing 3.11 is not granted |
| `package.py:51-67` the user's `core.fsmonitor` and `commit.gpgSign` reach the first commit | non-blocking | fixed `4ba27fc` (Decisions). `test_zip` case: fails on the old code ("`git commit` failed (fatal: failed to write commit object)"), and with only the signing half fixed on the monitor's trace |
| `create_project.py:286-296` a mapped or `subst` drive is reported (and written to) in its resolved form | non-blocking | recorded: Decisions (which path the CLI reports) and WP12's "Rules and gotchas" |
| edits outside `generator/` core, accepted at the merge | non-blocking (information) | nothing to do |
| the reviewer's scratch (`win-review-suite.log`, `upd-idx`) | non-blocking (information) | nothing to do: another session's scratchpad |

## Review fix#2 of Oct 19 (fresh-context review of `c91798a`)

| Finding | Severity | Outcome |
|---|---|---|
| `package.py:62` global excludes (`core.excludesFile`, `$XDG_CONFIG_HOME/git/ignore`, a template's `info/exclude`) drop `gradle-wrapper.jar` from the first commit, silently; the commit title, test name, Decisions and host-audit row claimed independence from the user's configuration | blocking | fixed `04510e3`, more widely than suggested (Decisions, fix#2): no system, global or template configuration and no `GIT_` variable, so attributes files, filters, object format and encoding are closed with it. `test_zip`: the fixture has the jar; a hostile configuration through `GIT_CONFIG_GLOBAL` (named excludes and attributes files) and through `HOME`/`XDG_CONFIG_HOME` (the default `git/ignore` and `git/attributes`); each asserts every file on disk is committed. Decisions entry and host-audit row corrected (the old row kept, marked wrong) |
| `WIN.md:355` hooks left to the user on the premise that `--no-verify` skips those that can refuse the commit; `prepare-commit-msg` and `reference-transaction` are not skipped | blocking | fixed `04510e3`: no hooks run (no global or system `core.hooksPath`, no template, no `GIT_CONFIG_*` variable). `Hostile` installs six hooks (among them `prepare-commit-msg` and `reference-transaction`) and a monitor, each recording that it ran and exiting 1, in a global hooks folder, a template and through `GIT_CONFIG_COUNT`; the tests assert no trace and the plain message. On the old code a `reference-transaction` hook refused even `git init`. Decision and the Rules line corrected |
| `package.py:82-95` `_git` inherits `GIT_DIR`, `GIT_WORK_TREE`, `GIT_INDEX_FILE` (the commit lands in another repository) and `GIT_AUTHOR_*`/`GIT_COMMITTER_*` | non-blocking | fixed `04510e3`: no `GIT_` variable but `GIT_EXEC_PATH` reaches git. `test_the_commit_is_the_same_whatever_git_variables_the_caller_has`: the other repository stays without a commit, no stray index, the generator's identity; fails on the old code |
| `test_zip.py:82` the test name claims more than it checks | non-blocking | the test now checks what the name says (hooks, excludes, template, filter, signing, monitor, identity, encoding, object format, by two routes); variables have their own test |
| `test_zip.py:95-99` the `GIT_CONFIG_GLOBAL` probe reads `commit.gpgSign`, which a real global configuration may hold | non-blocking | fixed: the probe reads `generatortest.marker`, which only the test's file holds, for each route |
| `placement.py:206-214` `_free_sibling` sizes by code points | non-blocking | fixed `5bff1e9`; test fails on the old code (6 != 5) |
| `test_golden.py:574-583` the astral test covers the zip only | non-blocking | fixed `5bff1e9`: an emoji in a project destination (the walk) and in a rename's failed path (`_path_too_long`), both every host; on the code before `c91798a` the first places the project ("RenderError not raised") and the second re-raises the `FileNotFoundError` (checked here) |
| `package.py:83-86` the fsmonitor comment names Git for Windows only | non-blocking | moot: the comment and the override are gone |
| `c91798a`'s message: "fails on the old code with that message" holds on Windows only | non-blocking | recorded here: on Linux the old code fails with "RenderError not raised", because the final rename succeeds there. The commit is on origin (`wp/WIN`, PR #1), so it is not reworded |
| (found while fixing) Git for Windows skips files past MAX_PATH in `git add`, exit 0 | not in the review | fixed `0c7825b` (Decisions) |
| (orchestrator, Oct 19 11:35) the Linux CI run: two `test_keystores` failures, `ci.yml` stale | not in the review | `e323eed` and `ae886b5` (Decisions); one of the two tests is older than WIN; the Vercel preview failure was not looked at (it needs the user's Vercel login) |

## Review fix#3 of Oct 19 (fresh-context review of `ae886b5`)

| Finding | Severity | Outcome |
|---|---|---|
| `test_keystores.py:144` skips on keytool's ASCII refusal, hiding that the generator accepts a spec it cannot carry out on JDK 17 (rule 15); the comment states an unverified cause as fact; the handoff's plan to read the skip reason in CI cannot work (verbosity 1) | blocking | fixed `ee311a8`: skip and comment gone. The product defect is fixed rather than kept as a known failure: a generated key's password must be printable ASCII, in the generator, the wizard and the website (Decisions, fix#3, which separates the evidence from the inference). The new tests fail on the old code: generator `ValidationTest` and the shared case; website the shared case. The two WIN.md lines are corrected (Open issues); no skip reason is expected from CI. The rule waits for the user's confirmation through the orchestrator (Open issues) |
| `package.py:52-53` the `core.autocrlf` rationale is wrong (autocrlf stores LF; the project's `eol=lf` overrides it) | non-blocking | fixed `f3d5c3d`: a clean filter through a global attributes file is the example |
| `package.py:101-104` `core.longPaths` only on the generator's command lines, so the user's next command in the repository fails on the files it let in | non-blocking | fixed `1ffa268`: in the new repository's config on Windows. The long-path test runs `git status` with no system or global configuration; on the old code it printed "Filename too long" on stderr (exit 0), which the test catches. Rules line updated |
| `package.py:94` the empty `HOME` breaks a git that is an asdf or mise shim | non-blocking | recorded: Decisions (fix#2's entry) and Rules |
| `ci.yml:99-106` the comment says "not the generator's modules" while `EVERY_LANGUAGE` imports one | non-blocking | fixed `4746a0b`: the languages come from `python -m genkit.core.catalogue --json` (the same five tags here); the bash branch dry-run in Git Bash; an assignment, so a failing catalogue fails the step |
| `test_zip.py:115` "git exports the first three to every hook" | non-blocking | fixed `f3d5c3d`; the same words in Decisions (fix#2) corrected |
| `ci.yml` is WP13's path | information | nothing to do (the orchestrator's decision) |
| the reviewer's verification at `ae886b5` | information | nothing to do |

## Review fix#4 of Oct 19 (fresh-context review of `4746a0b`)

| Finding | Severity | Outcome |
|---|---|---|
| `package.py:64` `git init --template=` leaves the user's `init.templateDir` / `GIT_TEMPLATE_DIR` out of the generated repository for good, so pre-commit's or git-secrets' hooks never run on the user's later commits (a regression from the integration head); WIN.md's Rules line says the user's settings apply from the next command, which a template never does | blocking | fixed `976a7e2` as suggested (Decisions, fix#4): resolved in the caller's environment, `hooks/` and `info/` copied after the commit as git copies them, no second `git init` (its SHA-256 rewrite reproduced here). Warnings for a template `config` (not applied), a failed copy and an unreadable configuration. `test_zip`: the template through `init.templateDir` (a `~` path) and `GIT_TEMPLATE_DIR`, with recording hooks and an `info/exclude` of `*.jar`: the commit holds the jar, no hook ran, `.git/hooks/*` and `.git/info/exclude` exist with the template's bytes, and the user's next commit runs their `pre-commit`; the three warning cases. With the previous `package.py` the two tests give 3 failures and 2 errors (Measurements). The Rules line corrected |
| `package.py:56`, `test_zip.py:115-117`, `WIN.md:449`: "the GIT_DIR git exports to every hook" is overstated; `04510e3`'s message on pre-push too | non-blocking | fixed in `976a7e2` (code and test comments) and in Decisions (fix#2's entry), after checking git 2.55.0.windows.5 here: commit hooks get `GIT_INDEX_FILE`, `GIT_AUTHOR_*`, `GIT_CONFIG_PARAMETERS`, and `GIT_DIR` only in a linked worktree; `post-checkout` none of them. The commit message is on origin and is not reworded; the correction is recorded in Decisions |
| merge gate: `ee311a8`'s printable-ASCII rule needs the user's confirmation before WIN merges | non-blocking | still open, for the orchestrator (Open issues), unchanged: nothing on this branch can settle it |
| `test_zip.py:157-161` `git status` passes the runner's `GIT_CONFIG_GLOBAL` through | non-blocking | fixed `976a7e2`: `isolated(home)` drops every `GIT_` variable but `GIT_EXEC_PATH` and sets `GIT_CONFIG_NOSYSTEM=1`; the new template tests use it too, so their later `git commit` can never reach a hook's `GIT_DIR` |
| `package.py:59` `TemporaryDirectory` cleanup can raise after a finished project | non-blocking | fixed `976a7e2`: `ignore_cleanup_errors=True`. Not tested: forcing a refused deletion of a folder that git just used needs a held handle on another thread, for a one-argument change |
| `ci.yml:98-118` the interim Generate step's rationale ("what users run") would be inherited by WP13, whose A12.7 goes through `generate_headless.py` | non-blocking | fixed `dcb0ce4`: the rationale is gone, the step unchanged; `followups.md` (CI) tells WP13. The generated-project jobs stay red on WP02's stale scaffold until WP02-M2 merges, as recorded |
| `BuildStep.tsx:359` the bare `true` | non-blocking | fixed `9228ddd`: the comment beside the input says every key there is one the generator makes, because the API refuses `existing_path`. A named constant would be a one-use flag; the file has no parameter-comment idiom to match |
| `catalogue.json:2` the stamp names `ee311a8`'s parent | non-blocking | fixed `2ce104a`: regenerated with `--write` after the code commits; it names `9228ddd`, which has the rule. Any later `--write` before its commit lags the same way, by design of the stamp |

## Lock time used

No adb in any run of this package. No Gradle before fix#5; the suite, the host comparison and (Oct 19) the
consistency matrix ran through `memwait` only. Fix#5 (Oct 19), `locked gradle` on CI's spec generated into
`out/WIN/ci/`, all `--offline`: the reproduction on the old scaffold, 1 min 4 s; lean's two orders modules, 1 min
20 s; lean with CI's tasks, 7 min 42 s; none's orders modules and dev debug app, 1 min 18 s.

## Measurements

- **Run fix#5 of Oct 19 (the two Linux failures after `3a0d38e`)**, at `070a91b` (`wp/WIN`, 2 commits on `3a0d38e`):
  - Suite (`memwait python -m unittest discover -s tests -t .`, `logs/WIN/suite-oct19-fix5.log`): 307 tests in
    134.2 s, OK, 4 skipped (the same four as before). Two tests are new: `test_zip`'s clean-tree test and
    `ScaffoldTest`'s; the second, against `3a0d38e`'s `scaffold.py` (put back, then restored): 2 subtests, 2
    failures, 12 issues each, the first "IMPORT data/orders/…/OrdersRepository.kt:
    com.acme.field.core.common.AppResult is not declared"; on `070a91b`: OK. `tests.test_zip` alone: 10 tests OK.
  - Quick consistency matrix (`memwait python -m tests.consistency --quick`, `logs/WIN/consistency-oct19-fix5.log`):
    58 projects in 73 s, 0 new issues, 323 known (575 at the last run, `consistency-after.log`; the difference is the
    scaffold's stale imports), 11 of 11 known-issue lines seen. CONSISTENCY OK.
  - `genkit.tokens --check`, `genkit.core.catalogue --check`: current. `git diff 3a0d38e..HEAD`: 0 added lines with
    CR, none past 120 columns, `--check` clean; 2 commits, each with the trailer.
  - Host comparison against `3a0d38e` (`compare_hosts.py`, `logs/WIN/compare-hosts-fix5.log`): standard 314 files
    identical; everything-and-brand 469 files, 12 differ, the six scaffolded files of `orders` and of
    `order_history`, nothing else; only `gradlew` executable in either zip. `accept.sh` stops at that comparison by
    design (it expects no output change), so its other steps were run one by one, as above.
  - `create_project.py --spec ci-spec.json --out out/WIN/ci/git-none --no-zip --git`: `git status --porcelain`
    empty, `gradlew` 100755 in the index (Windows; the POSIX half is the next CI run's).
  - Gradle on CI's spec (`"feature_modules": ["orders"]`, `out/WIN/ci/ci-spec.json`): the old scaffold, lean,
    `:data:orders:compileDebugKotlin :feature:orders:compileDebugKotlin --continue`: FAILED with CI's three errors
    (`OrdersRepository.kt:3:40`, `18:26`, `26:35`; `logs/WIN/orders-before.log`). Fixed, lean:
    `:data:orders:assembleDebug :feature:orders:assembleDebug :feature:orders:testDebugUnitTest`, BUILD SUCCESSFUL,
    `OrdersViewModelTest` 3 tests, 0 failures (`logs/WIN/orders-lean-modules.log`); `check assembleDevDebug
    assembleProdRelease` as CI runs it (without `--no-daemon`), BUILD SUCCESSFUL in 7 min 42 s, 1188 tasks, the
    orders modules' lint, guards and unit tests among them, dev debug and prod release APKs written
    (`logs/WIN/ci-lean-full.log`). None (no
    features, so the repository's network-off branch): `:data:orders:compileReleaseKotlin
    :feature:orders:testDebugUnitTest :app:assembleDevDebug`, BUILD SUCCESSFUL in 1 min 18 s, the same 3 tests pass,
    Hilt's app graph compiled with the module in it (`logs/WIN/ci-none-orders.log`). Not built here: `all` and
    `everything`.

- **Run fix#4 of Oct 19 (after the review of `4746a0b`)**, at `2ce104a` (`wp/WIN`, 4 commits on `4746a0b`):
  - Acceptance (`BASE=a402d87 $O/accept.sh`, log `logs/WIN/accept-oct19-fix4.log`): the whole suite through
    `memwait`, 305 tests in 63.9 s, OK, 4 skipped (the same four: Pillow, the two symlink tests, the filesystem
    exec-bit test). `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR
    byte. Host comparison COMPARE OK (standard: 314 files; everything-and-brand: 469; only `gradlew` executable in
    either zip). Worktree clean. **ACCEPT OK.** `git diff --check 4746a0b..HEAD` clean; the 4 commits: 5 files, 177
    insertions, 16 deletions, each with the noreply author and the trailer; no added generator or test line passes
    120 columns.
  - The new tests against the previous `package.py` (`4746a0b`'s, put back and then restored): `tests.test_zip` 9
    tests, 3 failures and 2 errors, all in the two new tests: both template routes `FileNotFoundError` on
    `.git\hooks\pre-commit`, and each warning case "1 != 0 : []". On `976a7e2`: 9 tests OK in 3.9 s.
  - Website (`web/frontend`, `node_modules` from fix#3's `npm ci`): `memwait node --test "features/**/*.test.ts"`
    222 tests, 222 pass (`logs/WIN/web-tests-fix4.log`); `memwait npx tsc --noEmit` exit 0
    (`logs/WIN/tsc-fix4.log`). The change there is a comment and the catalogue's stamp.
  - Scratch checks (git 2.55.0.windows.5, in the folder `t1` of this session's scratchpad, about 100 KB, left
    there): `git init --template=`
    makes only `HEAD`, `config`, `objects/` and `refs/`, and a commit adds no `hooks/` or `info/`. Hooks in a main
    worktree: `pre-commit`, `commit-msg`, `post-commit` got `GIT_AUTHOR_DATE`, `GIT_AUTHOR_EMAIL`, `GIT_AUTHOR_NAME`,
    `GIT_CONFIG_PARAMETERS`, `GIT_EDITOR`, `GIT_EXEC_PATH`, `GIT_INDEX_FILE`, `GIT_PREFIX`; the same in a linked
    worktree plus `GIT_DIR`; `post-checkout` (from `worktree add`) `GIT_EDITOR`, `GIT_EXEC_PATH`, `GIT_PREFIX`. A
    `git -c init.defaultObjectFormat=sha256 init` in a SHA-1 repository with a commit set `extensions.objectformat`
    to `sha256`, and `git log` then failed with "your current branch appears to be broken".
  - Merge check: of the files this run changed, only `web/frontend/lib/catalogue.json` is also changed on open
    branches (twelve, as before), on the stamp line `ee311a8` already changed, so no new conflict; it is regenerated
    with `--write` on a merge.

- **Run fix#3 of Oct 19 (after the review of `ae886b5`)**, at `4746a0b` (`wp/WIN`, 4 commits on `ae886b5`):
  - Acceptance (`BASE=a402d87 $O/accept.sh`, log `logs/WIN/accept-oct19-fix3.log`): the whole suite through
    `memwait`, 303 tests in 128.4 s, OK, 4 skipped (the same four: Pillow, the two symlink tests, the filesystem
    exec-bit test). `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR
    byte. Host comparison COMPARE OK (standard: 314 files; everything-and-brand: 469; only `gradlew` executable in
    either zip). Worktree clean. **ACCEPT OK.** `git diff --check ae886b5..HEAD` clean; the 4 commits: 11 files, 119
    insertions, 34 deletions, each with the noreply author and the trailer. No added generator or test line passes
    120 columns; the website lines follow their files' own longer lines.
  - Website (`web/frontend`, after `memwait npm ci --prefer-offline`, log `logs/WIN/npm-ci-fix3.log`; `node_modules`
    is ignored): `node --test "features/**/*.test.ts"` 222 tests, 222 pass, among them "refuses: generated keystore
    password outside ASCII" and "accepts: existing keystore with passwords outside ASCII"; `memwait npx tsc --noEmit`
    exit 0 (`logs/WIN/tsc-fix3.log`). `next build` not run: the change is a validation function, its type and one call
    argument, all of which `tsc` covers.
  - Each new test against the old code here: with `keystores.py` at `ae886b5`, `ValidationTest` fails on
    `pässwörd1` ("SpecError not raised") and `test_spec`'s shared case "generated keystore password outside ASCII"
    fails; with `keystores.ts` at `ae886b5`, the website's same case fails (1 of 110 in `validate.test.ts`); with
    `package.py` at `ae886b5`, the long-path test fails on `git status`'s stderr (`...deep.txt: Filename too long`,
    exit 0), as this machine has no system or global `core.longPaths`.
  - Probe on JBR 25.0.3 (`Probe.java` in this session's scratchpad, run as a single-file source): the "PBE"
    `SecretKeyFactory` accepts `pässwörd1` and a password holding U+007F, and a PKCS12 store saved and loaded with
    each opens. No JDK 17 on this machine, so its refusal is from its sources (Decisions, fix#3).
  - `git merge-tree` of `wp/WIN` against every other `wp/*` branch, compared with `ae886b5`: the only new conflict is
    `web/frontend/lib/catalogue.json` (12 branches; the emitted file, regenerated with `--write` on a conflict, rule
    17a). `wp/WP03-skel` and `wp/WP05-glass` keep their earlier conflicts in `look.py` and `target.py`; no other
    file is new.
  - The CI Generate step's `all` branch, dry-run in Git Bash with the new `EVERY_LANGUAGE`: `--all --set
    languages=["de", "es", "fr", "hi", "pt-BR"]`, the same list `genkit.core.translations.available("android")` gives.

- **Run fix#2 of Oct 19 (after the review of `c91798a`)**, at `ae886b5` (`wp/WIN`, 5 commits on `c91798a`):
  - Acceptance (`BASE=a402d87 $O/accept.sh`, log `logs/WIN/accept-oct19-fix2.log`): the whole suite through
    `memwait`, 301 tests in 150.1 s, OK, 4 skipped (the same four as before: Pillow, the two symlink tests, the
    filesystem exec-bit test; the non-ASCII password test runs and passes here on JBR 25). `genkit.tokens --check`
    and `genkit.core.catalogue --check` current. 0 added lines with a CR byte. Host comparison COMPARE OK (standard:
    314 files; everything-and-brand: 469; only `gradlew` executable in either zip). Worktree clean. **ACCEPT OK.**
    `git diff --check c91798a..HEAD` clean; the 5 commits: 7 files, 296 insertions, 82 deletions, each with the
    noreply author and the trailer.
  - Each new test against the old code here: with `package.py` at `c91798a`, the configuration test fails on both
    routes ("`git init` failed (fatal: in 'preparing' phase, update aborted by the reference-transaction hook)"), the
    variables test fails ("`git symbolic-ref` failed", the same hook through `GIT_CONFIG_COUNT`), and the long-path
    test fails with the 300-character file missing from the commit; with `keystores.py` at `c91798a`, `RefusalTest`
    fails with the CI's own message ("could not be created: \tfor: CN=Acme, O=Acme, C=US"); with `placement.py` at
    `c91798a`, the sibling test fails (6 != 5); with `placement.py` at `c91798a^`, the project-walk astral test fails
    ("RenderError not raised") and the failure astral test re-raises the `FileNotFoundError`.
  - Scratch checks (git 2.55.0.windows.5, in `%TEMP%\wlp`, deleted afterwards): `git init --template=` makes only
    `HEAD`, `config`, `objects` and `refs`; with the isolated environment `git config --list --show-origin` shows the
    repository's own file alone; `git add` of a 307-character path printed "Filename too long" and exited 0 without
    `core.longPaths`, and committed it with the key; git reads `$XDG_CONFIG_HOME/git/ignore` and `git/attributes` as
    the test's home route assumes (`check-ignore`, `check-attr`); keytool (JBR 25) on a folder where the key goes
    prints its error on stdout and nothing on stderr. The new Generate step of `ci.yml` was run in Git Bash for each
    matrix entry (`--dry-run --json`; `all` also generated): 0, 4, 36 and 36 features, five languages for `all`.
  - PR #1's Generator job log (run 39206352955, job 117909925610) was read with `gh run view --log` (read-only):
    ubuntu-24.04, Python 3.12, git 2.55.0; the two `test_keystores` failures, nothing else.

- **Run after the review of the follow-up merge (Oct 19, fix#1)**, at `c91798a` (`wp/WIN`, 2 commits on `4dd3aca`):
  - Acceptance (`BASE=a402d87 $O/accept.sh`, log `logs/WIN/accept-oct19-fix.log`): the whole suite through
    `memwait`, 295 tests in 152.6 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit test).
    `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR byte. Host comparison
    COMPARE OK (standard: 314 files; everything-and-brand: 469; only `gradlew` executable in either zip). Worktree
    clean. **ACCEPT OK.** `git diff --check 4dd3aca..HEAD` clean; the 2 commits: 4 files, 66 insertions, 10
    deletions, each with the noreply author and the trailer.
  - Each new test fails on the old code here: `test_zip`'s configuration test with `package.py` at `4dd3aca`
    ("`git commit` failed (fatal: failed to write commit object). The project itself is fine."), and with only
    `core.fsmonitor=` taken out of the fix ("the user's file system monitor ran"); `test_golden`'s emoji test with
    `placement.py` at `4dd3aca` ("Windows refused a path of 259 characters: ...\.5m9v37bnsvq1unv", the finished
    sibling, after the final rename failed).
  - Scratch repository (`scratchpad/fsm`, git 2.55.0.windows.5) with a global configuration holding a monitor hook
    and signing through a missing gpg: the hook ran twice on each of `add`, `ls-files`, `update-index` and `commit`,
    and the commit failed on gpg; with `-c core.fsmonitor= -c commit.gpgSign=false` no hook ran and the commit was
    made.
  - A slip while testing: a backup copy of `package.py` landed in `%LOCALAPPDATA%\package.py.keep` (a `$TEMP/..`
    path); it was compared with the restored file (identical) and sent to the Recycle Bin. Nothing else outside the
    worktree, `$OV/logs/WIN` and the session scratchpad was written.

- **Run after the merge (Oct 19)**, at `3b892f4` (`wp/WIN`, 3 commits on `merged/WIN`):
  - Acceptance (`BASE=a402d87 $O/accept.sh`, log `logs/WIN/accept-oct19.log`, against the integration head before
    WIN, so the CR check and the host comparison cover the whole package): the whole suite through `memwait`, 293
    tests in 224.4 s, OK, 4 skipped (Pillow; the two symlink tests; the filesystem exec-bit test); the time is the
    machine's load (180 to 800 MB free while other packages built and ran the matrix), not the 4 new tests, which take
    about 1 s. `genkit.tokens --check` and `genkit.core.catalogue --check` current. 0 added lines with a CR byte. Host
    comparison COMPARE OK (standard: 314 files; everything-and-brand: 469; only `gradlew` executable in either zip).
    Worktree clean. **ACCEPT OK.** `git diff --check 5a16778..HEAD` clean; the 3 commits: 4 files, 135 insertions,
    53 deletions, each with the noreply author and the trailer.
  - Quick consistency matrix, `python -m tests.consistency --quick --jobs 2` through `memwait` from the branch's
    `generator/` (`logs/WIN/consistency-after.log`): 58 projects in 201 s, 0 new issues, 575 known (12 of 12
    known-issue lines seen), **CONSISTENCY OK**; two workers started with spawn; its `%TEMP%` folder deleted. The
    same command on `git archive 5a16778` (`logs/WIN/consistency-before.log`, with `--keep` under `$O` so the expected
    failure left nothing in `%TEMP%`): the reference project built, then `AttributeError: module '__main__' has no
    attribute 'run_one'` in a worker and `BrokenProcessPool`, exit 1. Both scratch trees went to the Recycle Bin.
  - Each new test fails on the old code here: `IssuePathTest` with `checks.py` at `5a16778` (`'MANIFEST
    app\\src\\main\\AndroidManifest.xml'` and four more with backslashes), `RunnerTest`'s pool test with `__main__.py`
    at `5a16778` (`'tests.consistency.__main__' == 'tests.consistency.__main__'`), its clean-up test with `__main__.py`
    at `fbac839` (the folder still exists).
  - `git merge-tree --write-tree wp/<b> wp/WIN` for every open package branch (WP02, WP03, WP03-skel, WP04, WP04-perf,
    WP05, WP05-glass, WP06, WP07, WP08, HUD): clean for 8; for WP02, WP03-skel and WP05-glass exactly the conflicts
    they already have with `merged/WIN` (in `pipeline.py`, `scaffold.py`, `add_feature.py`, `look.py`, `target.py`),
    none in `tests/consistency/` (WP02 also edits `__main__.py` and `checks.py`; both auto-merge).

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
- On Windows the generator reports, measures and writes the `--out` folder with its directories resolved: a mapped
  network drive appears as its UNC path (`\\server\share\...`), a `subst` drive as the folder behind it, an 8.3
  short name in full. The length limit counts that form, so an output that fits at `Z:\x` can be refused at
  `\\server\share\x`, and the message names the UNC path.
- `--git` makes the first commit as "Project generator <generator@localhost>", unsigned, with none of the user's
  git setup: no system or global configuration (hooks, excludes and attributes files, file system monitor,
  signing, object format), no `init.templateDir` and no `GIT_` environment variable, so the commit holds exactly
  the project's files under its own `.gitattributes` and `.gitignore`, on every host. The user's hooks do not run
  for it. Once it is made, the repository gets the user's git template (`init.templateDir` or `GIT_TEMPLATE_DIR`):
  its `hooks/` and `info/` are copied into `.git` as `git init` would copy them, so hooks that pre-commit or
  git-secrets install through a template run on the user's own commits. The template's `config` file is not
  applied (`--git` says so in a warning), and neither is git's built-in template, so without a template of their
  own the repository has no sample hooks and no `.git/info` folder. The repository's own `.git/config` is git's
  default, plus `core.longPaths=true` on Windows (so a file past MAX_PATH that the commit holds stays usable); the
  user's system and global settings and their `GIT_` variables apply from the next command they run. A
  git that is an asdf or mise shim (it finds its data under `HOME`) cannot run for the commit: `--git` then warns and
  leaves the project without a repository; run `git init` there yourself. To make the commit theirs: `git commit --amend --reset-author --no-edit` (add `-S` to sign it).
- In cmd.exe the wrapper is `.\gradlew` or `gradlew`; `./gradlew` works in PowerShell and Git Bash.
- A signing key the generator makes needs a password of printable ASCII (letters, digits, spaces and the symbols on
  a US keyboard): JDK 17, the oldest JDK the project supports, can neither make nor open a PKCS12 keystore with any
  other character (newer JDKs can). A key you bring keeps its own password, any printable one; a PKCS12 one with
  other characters opens only on a JDK that accepts them. The generated README's `keytool` commands are under the
  same rule on JDK 17.
- `--git` on Windows records `gradlew` as executable (`100755`), so a clone on macOS or Linux runs it; on Windows
  itself `git init` sets `core.fileMode=false`, so the working tree shows no mode change.
- A symbolic link as `--out` needs Developer Mode on Windows; a junction works for any account, and `--force`
  replaces the link or junction, never what it points at.
- For the generator repository's contributor docs: `python -m tests.consistency` (the static matrix) runs on Windows,
  macOS and Linux alike once the Oct 19 commits merge. Whatever a script hands a process pool must be defined in an
  importable module, never in a package's `__main__.py` run with `-m` (a script run by its path is re-imported in the
  worker, a package's `__main__` is not): Windows and macOS start workers with spawn, and Linux starts them with
  forkserver from Python 3.14, so the `fork` that hid this on older Linux Pythons is gone. Issue and report lines name files with "/" (`as_posix()`), so lists written on one host match on another.

## Open issues (with the reason they are open)

- Closed Oct 19 (14:05): the fix#1 to fix#4 commits (`4ba27fc`..`2ce104a`) merged as `3a0d38e`, `merged/WIN` again
  left on `5a16778`. Still open after it: the user's confirmation of the printable ASCII rule (below), and
  `test_zip`'s long-path test failing on POSIX (`followups.md`, WIN; fixed in fix#5, `a5db7b0`, not yet run on
  Linux). `3a0d38e` holds PR #1's head, so a push of
  the integration branch shows PR #1 as merged on GitHub unless it is closed first.
- Closed Oct 19: the three Oct 19 commits (`6a22433`, `fbac839`, `3b892f4`) merged as `4dd3aca`. `merged/WIN` stays
  on the first merge (`5a16778`, already pushed); `git merge-base --is-ancestor wp/WIN overhaul/multiplatform-v2`
  is what shows the follow-up is in.
- Five `consistency-*` folders (2.4 to 3.8 MB each, about 15 MB, each holding a `reference` project) sit in this
  machine's `%TEMP%`, made between Oct 17 14:49 and Oct 18 05:16 by other packages' runs that ended in
  `BrokenProcessPool`. Not deleted here: they are not this run's, and temporary files are the user's to clear
  (Settings, System, Storage, Temporary files). A sixth, `consistency-leswribp`, appeared on Oct 19 at 09:11, after
  `4dd3aca`: the runner that leaves one is still on the branches without `3b892f4` (`wp/HUD`, `wp/WP06`, `wp/WP03`,
  `wp/WP08` as of this run; `out/HUD` was being written at that minute) and goes once they merge the integration
  branch. No consistency run was in progress when this was checked.

- The MAX_PATH case is explained, not avoided: staging under `%TEMP%` costs about 58 characters that the destination
  might not need. Staging beside the destination would change the API's cleanup model (pipeline docstring: staging
  lives under `TMPDIR` so the API deletes what a killed process leaves), which is outside this scope.
- After WIN merges, the line in `D:/projects/tools/CLAUDE.md` and the orchestrator's task text that say six
  generator tests fail on this machine are stale, and `logs/windows/generator-baseline.md` is history.
- The case-insensitive guard test only fails before the fix on macOS (Windows already compared paths without case);
  no macOS host was available to run it there.
- **The suite has run on Linux twice, at `c91798a` and at `ae886b5`, and not yet at this run's head.** PR #1 (draft, into the
  integration branch; the user approved it on Oct 19) ran CI as run 39206352955: Generator on ubuntu-24.04, Python
  3.12, git 2.55: every test passed but two in `test_keystores` (10 skipped), both now fixed (Decisions, fix#2 and
  fix#3). The other failed jobs failed in `ci.yml` itself, now updated (`ae886b5`). **For the orchestrator:** push
  `wp/WIN` (the backup push it makes anyway) so PR #1 re-runs on the new head, then read the Generator job (`gh run
  view <run id> --log --job <id>`) and record it here. Expected there and not checkable here: the new `test_zip`
  tests on git 2.55 Linux (hooks as `/bin/sh` scripts, `HOME` redirected; since fix#4 also the template tests, which
  assert mode 0755 on the copied hooks only where the OS has exec bits), `RefusalTest`, and a pass for the
  replaced keystore tests on Temurin 17 (no test skips on keytool's account any more); anything else in it is this
  package's. `ci.yml` runs unittest at verbosity 1, which prints `OK (skipped=N)` and no reasons, so a skip count
  above run 39206352955's 10 is worth a `-v` run before reading anything into it. The API
  and Generated project jobs now reach their real work, so what they say next is about those parts (WP10's API, the
  template), not about `ci.yml`'s module names. Second run (39210292751 at `ae886b5`, the orchestrator's 12:18 note):
  Generator and API pass; the four generated projects stop at `:data:orders` (`AppResult`), which is WP02's
  (Decisions, fix#3) and stays red until WP02-M2 merges, so PR #1 cannot be all green before then. Close the PR unmerged once read, as planned.
  Third and fourth runs (39214194825 at `4746a0b`, 39216375280 at `2ce104a`): one Generator failure, `test_zip`'s
  long-path test on the `--git` tree's executable bits, and the same `:data:orders` stop. Both fixed in fix#5
  (`a5db7b0`, `070a91b`; Progress), the second here rather than by WP02 at the orchestrator's direction. Expected in
  the next run: Generator green, including the new clean-tree test (it fails on Linux without `a5db7b0`), and the
  four generated-project jobs past `:data:orders`; a failure after that point is news. Since `3a0d38e` holds PR #1's
  head, the next Linux run may need a new pull request (`followups.md`, WIN).
- **For the orchestrator, to ask the user before WIN merges (a confirmation): a password for a signing key the
  generator makes must be printable ASCII** (fix#3, `ee311a8`; the reviewer asked for this rule to be settled with
  the user, and the fix#4 review repeated it as the merge gate). A suggested question: "JDK 17, the oldest JDK generated projects support and the one CI uses, cannot
  make or open a PKCS12 keystore whose password has a character outside printable ASCII (JBR 25 can). Should the
  generator refuse such passwords for the keys it makes?" Recommended, and done: refuse. Pros: an up-front error in
  the CLI, the wizard and the website; the keys open on every JDK from 17; the passwords the wizard and the website
  invent are ASCII already. Catches: a non-ASCII password typed by hand is refused; a key the user brings is not
  checked (it already works where they use it). Alternatives: allow them and generate JKS keys (a legacy,
  proprietary format keytool warns about, for every generated key); or allow them as before and accept that JDK 17
  makes no key (a warning only, and Linux CI stays red on `test_keystores`). On either alternative, revert `ee311a8`
  and decide what replaces it.
- **For WP13 (owner of `.github/workflows/**`):** add a `windows-latest` generator job (the same two steps as the
  ubuntu one), on Python 3.12 and on 3.11, the oldest the generator supports: `_delete_tree`'s `onerror` branch runs
  only on Windows under 3.11 and has never run (Oct 19 review). Without it the tests that need Windows' own file
  system (junction, 8.3 short name, `WindowsPathLimitTest`: the path limit in staging, the deep-file deletion and the
  failed swap beside a deep destination; the CR guards on a Windows checkout) run only on a developer's machine. Since
  fix#4 the pure path-limit logic (`WindowsLongPathsTest`, including the destination-depth refusal) runs on every
  host. `WindowsPathLimitTest` probes for long paths and skips with that reason where they are on, so the job is valid
  on either kind of runner image. Not done here because the path is WP13's and A12.7 rewrites `ci.yml`.
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

## Orchestrator note (Oct 19, 22:10)

The user, asked in chat: keep the printable-ASCII rule for generated keystore passwords (`ee311a8`). Closed.
