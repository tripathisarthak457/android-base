# Wipe readiness: what this laptop held that a wipe would lose (public, redacted)

Read-only scan, **Oct 16, 2026, 11:05 to 11:19 IST**, on the Pop!_OS laptop (Mi NoteBook Ultra) before it was wiped
for Windows. No secret values appear in this file: only paths, key *names*, counts and sizes.

> **Redacted version (Oct 16, about 12:50 IST, handover fix round 1).** This copy is on the PUBLIC branch
> `overhaul-workspace`, so it now covers only this project (android-base) and the user's own app PeriFlow. The
> details about the user's other projects (work-organisation and client repositories, their servers, keys, logins and
> personal documents) were removed: they are not needed to resume the overhaul. The full scan is kept out of git:
> on the old laptop at `.overhaul/handover/local-only/WIPE-READINESS-full.md`, and in the user's Google Drive backup
> `backup-2026-10-16`, zip `00-android-base-not-on-github-p01.zip`, path
> `Documents/projects/tools/.overhaul/handover/WIPE-READINESS.md`. The offline backup's own `README-RESTORE.md` and
> `PLAN.md` say where every other project's files went.
>
> **Note for the user:** the first two commits of `overhaul-workspace` still contain the full
> version (paths and names only, no values). Removing it from history needs a force-push or deleting and recreating the
> branch; agents must not do either. It is your call (`HANDOVER.md` §1.7).

Other agents were pushing the android-base and PeriFlow backups **at the same time** as this scan. At 11:19 the
android-base remote still had only `main`. The android-base numbers below are therefore the "before" state. What was
actually pushed is in `PUSHED.md`.

---

## 0. The biggest risks, in order (this project and PeriFlow)

| # | Risk | Where | Why it matters |
|---|------|-------|----------------|
| 1 | **Signing keystores and their passwords exist only on this disk** | PeriFlow `periflow-android/keys/{dev,staging,prod,playstore}.jks` + `keystore.properties` (and the release keys of the user's other apps; full version) | Gitignored and in no remote. If an app's release key is lost, **updates can never again be installed over the existing installs**. Play App Signing can reset only an *upload* key. |
| 2 | **One private SSH deploy key** | `~/.ssh/` | A deploy key for a server of another project (details in the full version). Without it, SSH access to that server may be lost. |
| 3 | **PeriFlow's account and backend secrets** | `periflow-recovery-codes.txt`, `KEYS.md`, `periflow-backend/.env`, `periflow-backend/deploy/secrets/firebase-service-account.json`, `periflow-android/app/src/{dev,staging,prod,playstore}/google-services.json`, and three `periflow.*` lines in `~/.gradle/gradle.properties` | All gitignored or untracked, and in no remote. The recovery codes are 2FA recovery codes: password manager, **never any git repo**. |
| 4 | **Other projects with no remote** | the user's other projects (full version) | Lost on wipe unless pushed or copied. The offline backup's `09-unpushed-projects.zip` holds them. |
| 5 | **android-base: 580 commits existed only locally** | 14 branches + 4 `merged/*` tags; uncommitted WIP in WP03 (47 entries), WP05-glass (43), WP04-perf (4) | Pushed later on Oct 16 (`PUSHED.md` §1). |
| 6 | **PeriFlow `overhaul/smooth-glass`: 16 commits not on GitHub + 2 dirty files** | worktree `.overhaul/wt/WP11` | Pushed later on Oct 16 to the private repo (`PUSHED.md` §2). |
| 7 | **The transcripts contain secrets. Never put them in the public repo.** | `~/.claude/projects/-home-crazyhusir-Documents-projects-tools/**.jsonl` | **A real keystore password (of one of the user's other apps) appears in 2 subagent transcripts** (`e3f9060d…/subagents/workflows/wf_1b6fefc0-712/agent-a93f68cc1bb4477fd.jsonl` and `…/wf_d5a4bdd1-aef/agent-a0df22f553c35a907.jsonl`). The transcripts also hold 7 distinct strings shaped like Google API keys of unknown origin (not the template's placeholder key), `storePassword=`/`keyPassword=` lines in 17 and 20 files, `JWT_SECRET=` in 1 file and `DATABASE_URL=postgres…` in 11. The PeriFlow keystore passwords were checked and do **not** appear. Transcripts live only on the user's private drive. |
| 8 | **`.overhaul` holds private source and private data that must stay out of the public `overhaul-workspace` branch** | see section 3.3 | `scratch/pf-diff/pf` (110 PeriFlow `.kt` files); `scratch/vitfleet/base` (322 `.kt` files of a client's private app); `perf/WP11` (4.9 GB of PeriFlow perf captures); `logs/WP11`; `audit/periflow-learnings.md`; `audit/frames/periflow_*.png`; `research/predictive-back/gmail` (367 MB, real e-mails on screen); `verify-gen/ks/t.jks` (a test keystore, which secret scanners flag anyway). |

---

## 1. Git repositories

Scan: `find ~/Documents ~/Downloads -maxdepth 9 -name .git` (node_modules excluded): 6 real repositories plus their
worktrees, 1 scratch worktree and 13 generated test projects. "Local-only commits" = `git rev-list --count <branch>
--not --remotes`. Each live remote was compared with `git ls-remote` at about 11:11. Only this project and PeriFlow
are described here; the user's other repositories are in the full version.

### 1.1 android-base (this project): `~/Documents/projects/tools/android-base`

| Field | Value |
|---|---|
| Remote | `origin https://github.com/tripathisarthak457/android-base.git`: **PUBLIC**, default branch `main` (production deploys from it) |
| Live remote at 11:11 / 11:19 | only `main` = `99c4edf` (same as local `main`, last pushed Sep 24) |
| HEAD | `overhaul/multiplatform-v2` @ `a402d87` (Oct 14 17:54, "Merge WP10…"), clean |
| Branches with no upstream | all 14 except `main` (table in 3.1) |
| Unique local-only commits | **580** (all branches + tags, `--not --remotes`) |
| Tags (local only) | `merged/WP01` 709612d, `merged/WP02-M1` 54d078d, `merged/WP09` 8672003, `merged/WP10` a402d87 |
| Stashes | 0 |
| Main checkout dirty | 0 modified, 0 untracked. Only ignored file of note: `web/frontend/tsconfig.tsbuildinfo` (build cache) |
| Worktrees | 14 under `.overhaul/wt/WP01…WP10` (incl. WP03-skel, WP04-perf, WP05-glass) + `.overhaul/scratch/WP09-trial` |
| Last commit | Oct 16 10:38 (`wp/WP04` = `wp/WP04-perf` = `fb866b3`) |
| `.git` size | 23 MB |

The scratch worktree `.overhaul/scratch/WP09-trial` has a detached HEAD at `28cd271` ("Merge branch 'wp/WP01' into
HEAD", Oct 9). It was pushed as `backup/WP09-trial-28cd271` (`PUSHED.md` §1.4).

### 1.2 PeriFlow: `~/Documents/projects/jorwen/periflow`

| Field | Value |
|---|---|
| Remote | `origin https://github.com/tripathisarthak457/periflow.git`: **PRIVATE**, default `main`. GitHub now names it **`JorwenLabs/periflow`** (the old URL redirects; `PUSHED.md` §2) |
| HEAD | `main` @ `240871f` (Sep 28), clean tracked tree |
| Branches with no upstream | `overhaul/smooth-glass` (**16 local-only commits**, head `45b91a2`, Oct 15 16:12); `worktree-agent-a0577f8311e34315e` and `worktree-agent-ac2f656f3b85c82d7` (0 local-only: their commits are on remote branches) |
| Ahead of upstream | none. The 10 tracked branches all match their upstream. |
| Remote has more than local | `develop` on GitHub (`ff55ba9`) is ahead of local (`12a6614`); three remote-only branches. Nothing to lose. |
| Stashes / tags | 0 / 0 |
| Untracked (5) | `PeriFlow-Postman/` (4 files) and `periflow-recovery-codes.txt`. **Secrets: never commit.** |
| Ignored but important | `KEYS.md`, `periflow-android/keys/*.jks` (4), `periflow-android/keystore.properties`, `periflow-android/local.properties` (sdk.dir only), `periflow-android/app/src/{dev,staging,prod,playstore}/google-services.json`, `periflow-android/app/src/prodRelease/res/xml/`, `periflow-backend/.env`, `periflow-backend/deploy/secrets/firebase-service-account.json`, `periflow-backend/bin/periflow` (a build output), and two notes (`HANDOVER-2026-09-26-1025.md`, `done-till-25th-11pm.md`) |
| Worktrees | `.claude/worktrees/agent-a0577…` and `agent-ac2f6…` (clean); `.overhaul/wt/WP11` (`overhaul/smooth-glass`, **2 modified files**); `.overhaul/wt/WP11-base` (detached at `a5b630a`, which `overhaul/smooth-glass` contains; clean) |
| Last commit | Oct 15 16:12 |

WP11's 2 dirty files are `periflow-android/core/designsystem/src/main/kotlin/com/jorwen/periflow/core/designsystem/foundation/glass/LiquidLens.kt`
and `…/src/test/kotlin/…/foundation/glass/LiquidLensTest.kt`. The `.overhaul/wt/WP11*` worktrees also hold copies of the
PeriFlow keystores, `keystore.properties` and `local.properties` (gitignored).

### 1.3–1.6, 1.8 The user's other repositories

Removed from the public copy (work-organisation, client and personal repositories). Full version: see the box at the
top. The offline backup holds their unpushed code (`09-unpushed-projects.zip`) and git bundles (`04-git-bundles.zip`).

### 1.7 Generated test projects inside `.overhaul` (regenerable)

`.overhaul/out/WP02/{crossnav, r9-fmt, r10-c-local, r10-c-net, r10-fmt-local, r10-fmt-net, r10-s-local-0..2, r10-s-net-0..2}`
have 1 or 2 commits each and no remote; `.overhaul/verify-gen/out/gitcheck` has 0 commits. The generator produced all of
them (`git init` in its output). They are part of the regenerable `out/` (17 GB). Nothing unique.

---

## 2. Important data outside git

### 2.1 Keystores and signing keys (this project and PeriFlow; paths only)

| Path | What | Save? |
|---|---|---|
| `~/Documents/projects/jorwen/periflow/periflow-android/keys/{dev,staging,prod,playstore}.jks` + `../keystore.properties` | PeriFlow signing keys and their passwords | **YES, critical** (password manager plus private drive) |
| `~/.android/debug.keystore` | debug signing key | Optional. Keep it if any Firebase or Google Sign-In config registered its SHA-1 (unknown). Otherwise Windows makes a new one. |
| `~/.android/adbkey` (+ `.pub`) | adb host key | Optional. Without it the Redmi (9a7c74f4) only asks to authorize USB debugging again. |
| `.overhaul/out/gen-keys*/`, `.overhaul/out/WP01/*/spec_keys/`, `.overhaul/verify-gen/ks/t.jks` | **test** keystores that the generator tests made | No (regenerable). **Must not go into the public repo.** |
| `.overhaul/wt/WP11*/periflow-android/keys/*` | copies of the PeriFlow keys | Covered by the first row |

The release keys of the user's other apps: full version. `gpg`: `~/.gnupg` held 0 keys.

### 2.2 `~/.ssh`

One private deploy key (for another project's server) with its `.pub`, plus `known_hosts`. Details in the full version.

### 2.3 Other secret files (PeriFlow; names only)

| Path | Save to |
|---|---|
| `periflow/periflow-recovery-codes.txt` | password manager (2FA recovery codes) |
| `periflow/KEYS.md` | password manager |
| `periflow/PeriFlow-Postman/` (2 environments may hold tokens) | private drive |
| `periflow/periflow-backend/.env`, `periflow-backend/deploy/secrets/firebase-service-account.json` | password manager or private drive |
| `periflow/periflow-android/app/src/{dev,staging,prod,playstore}/google-services.json` | private drive (prod differs from the placeholder, so treat all four as real) |
| `~/.gradle/gradle.properties` keys `periflow.devApiUrl`, `periflow.googleWebClientId`, `periflow.prodApiUrl` | copy the file; PeriFlow builds need them |
| `~/.claude/.credentials.json`, `~/.config/Claude/*`, gh keyring token, `~/.docker/.token_seed`, `~/.emulator_console_auth_token` | **do not copy**; log in again on Windows |

The other projects' secret files: full version. The user packed all of them himself into the offline backup's
`01-secrets.zip` (12:04, not encrypted, by his choice).

### 2.4 `local.properties`

Eight on the laptop, all holding only `sdk.dir` (no secrets), including `periflow/periflow-android`,
`.overhaul/scratch/WP07-skel`, `.overhaul/wt/WP11-base/periflow-android` and `.overhaul/wt/WP11/periflow-android`.
Android Studio or the build regenerates them.

### 2.5 `~/.claude` (Claude Code)

| Item | Size | What matters |
|---|---|---|
| `projects/-home-crazyhusir-Documents-projects-tools/memory/` | 24 KB | **This project's memory** (MEMORY.md, android-base-generator.md, android-skills-installed.md, laptop-memory-freeze.md). A refreshed copy for Windows is `claude-memory/` in `overhaul-workspace`. |
| `projects/-home-crazyhusir-Documents-projects-tools/*.jsonl` + `e3f9060d…/` (subagents, workflows, tool-results) | 712 MB | This project's transcripts. **They contain secrets (risk 7)**: private drive only. |
| `projects/-home-crazyhusir-Documents-projects-jorwen-periflow/` | 98 MB, **8 memory files** | PeriFlow memory and transcripts. Private only (offline backup). |
| `projects/-home-crazyhusir-Downloads-android-base/` | 29 MB, 4 memory files (`github-push-auth.md`, `improvement-plan-progress.md`, `push-to-main-deploys.md`) | the earlier android-base session |
| `projects/-home-crazyhusir-Documents-projects-tools--overhaul/` | 220 KB | related session |
| other projects' folders | | private; offline backup (`02-claude-and-config.zip`) |
| `settings.json` | 2.7 KB | Keys: `model` (= `sonnet`), `enabledPlugins` (`android-skills@android-skills`), `extraKnownMarketplaces` (`android-skills`), `modelSettings` (`claude-opus-5-5`: effort medium), `skipWorkflowUsageWarning`, `autoMode.environment` (24 lines of custom auto-mode environment text). No secret-looking keys. |
| `plugins/` | 22 MB | `android-skills@android-skills` 1.0.15 from GitHub `android/skills`: reinstall it |
| `skills/synced/` | 11 MB | synced from the claude.ai account; comes back on login |
| `~/.claude.json` | 68 KB | app state; no MCP servers configured. No need to copy. |
| `.credentials.json` | n/a | do not copy; log in again |

### 2.6 `~/.config/Claude` (Claude desktop app)

875 MB, mostly Chromium caches. `claude_desktop_config.json` holds only `coworkUserFilesPath` and UI preferences (no
MCP servers); there are also `claude-code-sessions/` and `local-agent-mode-sessions/` and the bundled Claude Code
`claude-code/2.1.284`. **Nothing that you need to copy.** Connectors come from the account.

### 2.7 gh CLI

`gh auth status`: logged in to github.com as **`tripathisarthak457`** (token in the system keyring, https protocol).
The token's scopes include `repo`, `workflow`, `delete_repo` and admin scopes. Git uses `gh auth git-credential` for
github.com. On Windows run `gh auth login` again; do not copy the token. Git identity (`~/.gitconfig`):
`user.name=tripathisarthak457`, `user.email=98253995+tripathisarthak457@users.noreply.github.com`, `safe.directory=*`.

### 2.8 Toolchain (versions only; all reinstallable)

| Tool | Version |
|---|---|
| OS | Pop!_OS 24.04 LTS, kernel 7.0.11 |
| Android Studio | Rabbit 1 **2026.2.1** (build 262.9437.185.2621.16467767) through JetBrains Toolbox; settings for 2026.1.4 also present |
| Android SDK (`~/Android/Sdk`, 8.5 GB) | platforms android-36, android-37.0, android-37.1; build-tools 36.0.0; NDK 30.0.16248370; CMake 4.1.2; platform-tools 37.0.1; emulator 37.1.11; cmdline-tools latest; system image android-35 google_apis x86_64 |
| AVDs (`~/.android/avd`, 3.2 GB) | `periflow_api35`, `wp04_fold_api35` |
| Gradle | wrapper **9.7.1** in android-base and PeriFlow; dists 9.7.1 and 9.8.0 cached; `~/.gradle` 42 GB (cache) |
| Gradle JDK | JetBrains Runtime 21.0.11 (auto-provisioned in `~/.gradle/jdks`); system `java` is OpenJDK 25.0.3 |
| android-base template (integration branch) | AGP 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00, `org.gradle.jvmargs=-Xmx4g …` |
| Others | Node 24.21.0, npm 11.19.0, Go 1.27.1, Python 3.12.3, Docker 29.1.3, gh 2.45.0, git 2.43.0, adb 1.0.41 (37.0.1) |

### 2.9 Other data a wipe would lose

Docker dev volumes (`periflow-dev_{pgdata,redisdata,s3data}` and those of other projects; probably reseedable,
unverified), `~/Downloads` documents and media, brand assets, Codex and Gemini histories, analysis caches. Details in
the full version; the offline backup covers them (`README-RESTORE.md` §1).

---

## 3. This project (other agents pushed these; this is the "before" picture)

### 3.1 android-base branches at 11:11 (live remote had `main` only)

| Branch | Head | Last commit | Local-only commits | Worktree | Uncommitted |
|---|---|---|---|---|---|
| `main` | 99c4edf | Sep 24 | 0 (pushed; **do not touch**) | n/a | n/a |
| `overhaul/multiplatform-v2` | a402d87 | Oct 14 17:54 | 233 | main checkout | clean |
| `wp/WP01` | d19f79c | Oct 9 08:34 | 45 | wt/WP01 | clean |
| `wp/WP02` | 13eb1f1 | Oct 15 16:25 | 277 | wt/WP02 | clean |
| `wp/WP03` | b595f9d | Oct 15 16:18 | 312 | wt/WP03 | **34 tracked changes (M/D) + 13 untracked entries (98 files when expanded) = 47 entries**: the bottom-sheet system |
| `wp/WP03-skel` | 46ec781 | Oct 14 18:53 | 158 | wt/WP03-skel | clean |
| `wp/WP04` | fb866b3 | Oct 16 10:38 | 328 | wt/WP04 | clean |
| `wp/WP04-perf` | fb866b3 | Oct 16 10:38 | 328 | wt/WP04-perf | **4 modified** (`feature/sample/.../detail/SampleDetail{Contract,Route,ViewModel}.kt`, `SampleDetailViewModelTest.kt`). The brief says 5; at 11:12 there were 4. |
| `wp/WP05` | 5d515a0 | Oct 15 15:37 | 95 | wt/WP05 | clean |
| `wp/WP05-glass` | 282dd3c | Oct 15 15:53 | 403 | wt/WP05-glass | **35 modified + 8 untracked = 43**: the liquid-glass bar (`GlassButtons.kt`, `LiquidTabs.kt`, `LiquidPress.kt`, `PressGlow.kt`, `RimLight.kt`, `GlassTier.kt`, 2 fixtures) |
| `wp/WP06` | a8129c4 | Oct 15 16:15 | 265 | wt/WP06 | clean |
| `wp/WP07` | 99239af | Oct 12 14:16 | 95 | wt/WP07 | clean |
| `wp/WP08` | d05a682 | Oct 15 15:33 | 298 | wt/WP08 | clean |
| `wp/WP09` | 3d580cd | Oct 9 17:05 | 79 | wt/WP09 | clean |
| `wp/WP10` | d702b16 | Oct 14 17:37 | 232 | wt/WP10 | clean |

Tags `merged/WP01`, `merged/WP02-M1`, `merged/WP09` and `merged/WP10` were all **local only** at 11:11.

### 3.2 PeriFlow (private repo only)

`overhaul/smooth-glass` @ `45b91a2` had **16 commits** not on GitHub (`35a16bf` "Let tools/perf find PeriFlow's tabs…"
through `45b91a2` "Draw the screen into the travelling lens unscaled…"), branched from `main` `240871f`. The worktree
`.overhaul/wt/WP11` has 2 modified files (`LiquidLens.kt`, `LiquidLensTest.kt`). These go to the PRIVATE PeriFlow repo
only (now `JorwenLabs/periflow`). The secrets in 1.2 stay out of every push.

### 3.3 `.overhaul` coverage (sizes from `du`, before `MANIFEST.md` existed)

| Item | Size | Suggested destination |
|---|---|---|
| `STATUS.md`, `PROGRESS.md`, `REQUIREMENTS.md`, `REQUIREMENTS-STATUS.md`, `FABLE-FINDINGS.md`, `SESSION-2026-10-13..16.md`, `RELEASE-NOTES-android-draft.md`, `merges.md`, `followups.md`, `peek.py` | under 200 KB | workspace branch |
| `design/` 444 KB, `handoff/` 900 KB, `decisions/` 32 KB, `reviews/` 24 KB, `notes/` 8 KB, `docs-draft/` 96 KB, `scripts/` 648 KB (incl. `.pre-*` backups), `bin/` 40 KB, `backup/WP03-2026-10-15` 220 KB | about 2.4 MB | workspace branch |
| `logs/` 2.7 MB | | workspace, **but `logs/WP11` (1.4 MB) is PeriFlow: private only.** `logs/thermal.log` is crash evidence: keep it. |
| `audit/` 9.4 MB | | workspace, **except `periflow-learnings.md` and `frames/periflow_*.png`** (PeriFlow; private only) |
| `research/` 370 MB | | `.md` files and `glass/`, `sheet/` to the workspace (`glass/kyant-*` is third-party Apache-2.0 source). **`predictive-back/gmail/` (367 MB) shows real e-mails: never publish.** |
| `qa/` 143 MB | | `.md` to the workspace; mp4 and png frames to the raw-data archives |
| `perf/` 8.8 GB | | WP02, WP06, WP07, WP08 are small. WP03 998 MB, WP04 2.7 GB and WP04-perf 241 MB are raw data. **`perf/WP11` (4.9 GB) is PeriFlow: never in the public repo.** |
| `scratch/` 260 MB | | Mostly regenerable. **`pf-diff/` (PeriFlow source) and `vitfleet/base` (a client app's source) are private: never publish.** `WP09-trial` is a worktree (see 1.1). |
| `scratch-gen/` 284 KB, `verify-gen/` 26 MB, `verify-webapi/` 36 KB | | small; **`verify-gen/ks/t.jks` is a test keystore: exclude it** |
| `out/` 17 GB | | regenerable generated projects and APKs: left out. Contains test keystores and placeholder `google-services.json` files. |
| `wt/` 16 GB | | the worktrees: covered by the branch pushes plus the WIP snapshots |
| `adb.lock`, `emu.lock`, `gradle.lock` | 0 B | no need |

Files over 95 MB outside `wt/` and `out/`: **none**.

---

## 4. Checklist the user had before wiping (this project and PeriFlow)

**Secrets: to a password manager and/or a private drive (never into git).** The user made `01-secrets.zip` himself at
12:04 from `backup-2026-10-16/01-secrets-HELD-list.txt` (53 files, including every item below and those of his other
projects):
- [ ] PeriFlow: `periflow-android/keys/` (4 `.jks`) + `periflow-android/keystore.properties`
- [ ] PeriFlow: `periflow-recovery-codes.txt`, `KEYS.md`, `periflow-backend/.env`, `periflow-backend/deploy/secrets/firebase-service-account.json`
- [ ] PeriFlow: `periflow-android/app/src/{dev,staging,prod,playstore}/google-services.json`, `periflow-android/app/src/prodRelease/res/xml/`, `PeriFlow-Postman/`
- [ ] `~/.gradle/gradle.properties` (the 3 `periflow.*` entries)
- [ ] the SSH deploy key and the other projects' keys (full version)
- [ ] Optional: `~/.android/debug.keystore` and `~/.android/adbkey*`

**Claude context:**
- [ ] `~/.claude/settings.json` (the auto-mode environment text and plugin choice)
- [ ] This project's transcripts (712 MB): **only a private drive, never a public branch**. At least two subagent
      transcripts contain a real keystore password, so consider changing that password if they were ever shared.
- [ ] PeriFlow's Claude memory folder (private only)

**Already safe (no action):** android-base `main`, PeriFlow's other branches.

---

## 5. Last checks to run just before the wipe

```bash
# android-base: every branch and tag should now be on GitHub (expect wp/*, overhaul/*, merged/*, backup/*, overhaul-workspace)
cd ~/Documents/projects/tools/android-base
git fetch origin && git ls-remote origin | grep -v '\^{}'
git rev-list --count --branches --tags --not --remotes     # expect 0
for w in ~/Documents/projects/tools/.overhaul/wt/*; do echo "$w $(git -C "$w" status --porcelain | wc -l)"; done   # WIP must be in the backup/*-wip branches

# PeriFlow: the overhaul branch and its WIP snapshot on the PRIVATE repo (now JorwenLabs/periflow)
cd ~/Documents/projects/jorwen/periflow
git ls-remote origin | grep -E 'smooth-glass|wip'
git rev-list --count overhaul/smooth-glass --not --remotes   # expect 0

# The raw-data draft release (may not exist: PUSHED.md section 3; the Drive backup holds the same files)
gh release view overhaul-raw-data-2026-10-16 -R tripathisarthak457/android-base --json isDraft,assets -q '.isDraft, (.assets|length)'

# The public repo must not contain secrets or private code
git -C ~/Documents/projects/tools/android-base ls-tree -r --name-only origin/overhaul-workspace \
  | grep -Ei '\.jks$|\.p12$|keystore\.properties|local\.properties|\.env$|recovery-codes|Postman|pf-diff|scratch/vitfleet|perf/WP11|logs/WP11|periflow|gmail|\.jsonl$'   # expect no output (review any hits)
```
