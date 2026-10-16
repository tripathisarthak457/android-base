# PUSHED: what the backup put on GitHub (2026-10-16)

Written on Oct 16, 2026, at about 12:25 IST, by the pack-and-push agent of the handover workflow, right after the
pushes. Every SHA below was checked against `git ls-remote` after the push. If this file and `HANDOVER.md` disagree on
a SHA or a branch name, trust this file.

Nothing was force-pushed. `main` was not pushed or changed in either repository, and the default branches were not
changed (android-base: `main`; PeriFlow: `main`).

**Updated by the handover's fix round 1 (Oct 16, about 12:45–13:00 IST)** after a fresh-context verifier read the
backup: every ref and WIP snapshot was re-checked (nothing missing, nothing changed), the documents were corrected,
and one more workspace commit was pushed (§1.5, §6). The merge of everything into `main` **did not run** (§6.1).

## 1. android-base (PUBLIC): `https://github.com/tripathisarthak457/android-base`

Before the push, GitHub had only `main`. Local `main` and `origin/main` were both at
`99c4edf97570a117811fe21acde740570c14cfb5`. They matched, so nothing about `main` needed recording, and `main` stays
at that commit.

Pushed with explicit refspecs (`refs/heads/wp/*`, `refs/heads/overhaul/*`, `refs/heads/backup/*`,
`refs/tags/merged/*`), with no `-u` and no `--force`. All 22 refs are new on GitHub, and every remote SHA equals the
local one.

### 1.1 Integration branch and package branches

| Ref | SHA | Head commit |
|---|---|---|
| `refs/heads/overhaul/multiplatform-v2` | `a402d87e45057c22fe21ad9d7371c3885c8cf86d` | Merge WP10: website configurator, previews, telemetry client and admin portal |
| `refs/heads/wp/WP01` | `d19f79c8aec111dad2378814b9bcc5bc2a0a2a36` | Wrap the over-long lines this branch added to the SDK check and the token schema |
| `refs/heads/wp/WP02` | `13eb1f14c9ebf524902b2cf8038ed94043032562` | Fail a feature's scroll container that the chrome does not pad |
| `refs/heads/wp/WP03` | `b595f9d05695a132219b263960013c577a2a96e7` | Merge overhaul/multiplatform-v2 (WP10) into wp/WP03 |
| `refs/heads/wp/WP03-skel` | `46ec781bf54fc6331d932f4e0b349683ed18b8be` | Merge wp/WP03 into wp/WP03-skel |
| `refs/heads/wp/WP04` | `fb866b3b7a3095b2c72a7b2449f48737448191c7` | Lower a hidden tab's lifecycle the frame after the switch |
| `refs/heads/wp/WP04-perf` | `fb866b3b7a3095b2c72a7b2449f48737448191c7` | (same commit as `wp/WP04`) |
| `refs/heads/wp/WP05` | `5d515a0689decc31eb113461a7963bfccff38aa8` | Give the tab lens's state a file of its own |
| `refs/heads/wp/WP05-glass` | `282dd3c2856e6281697a49bfb9f9b00b06a78f67` | Merge wp/WP03 into wp/WP05-glass |
| `refs/heads/wp/WP06` | `a8129c4908a912b3922a33bec2b2855dc6bcf4b2` | Test that the app-lock cover keeps the content out of reach |
| `refs/heads/wp/WP07` | `99239af414735f2a9c8fc0cd7037514be4f6ef00` | Opt every test compilation into the coroutine test API, not only commonTest |
| `refs/heads/wp/WP08` | `d05a6828a6075c67859c1ac2037af6cd53350f8d` | Merge the integration branch to pick up WP10's website |
| `refs/heads/wp/WP09` | `3d580cdd7d30beac0159357a0e5ed9f12a24a44c` | Group the admin's top rejections by cause, not by the value refused |
| `refs/heads/wp/WP10` | `d702b16fb5949bae3809bea90d6a94be0c77a05d` | Name licences only for platforms the site makes |

### 1.2 Merge tags (lightweight)

| Ref | SHA |
|---|---|
| `refs/tags/merged/WP01` | `709612df558029712e12aa5f6852236908186e9c` |
| `refs/tags/merged/WP02-M1` | `54d078d79590b0a1a112aa526284ca91c2d65241` |
| `refs/tags/merged/WP09` | `8672003e10629a82f519a7b2e9dd7f16766d3f91` |
| `refs/tags/merged/WP10` | `a402d87e45057c22fe21ad9d7371c3885c8cf86d` |

### 1.3 WIP snapshots (uncommitted work, not reviewed, may not compile)

Each snapshot is **one commit whose parent is the branch head** at snapshot time. It was built with a temporary index
(`GIT_INDEX_FILE`, `git read-tree HEAD`, `git add -A` in the worktree, `git write-tree`, `git commit-tree -p HEAD`).
The worktrees' own index, working files and branches were not touched: the index checksum and `git status` of each
worktree were the same before and after. Commit message: "WIP snapshot of <branch> before the laptop wipe
(2026-10-16): uncommitted work, not reviewed, may not compile", with the Opus 5.5 trailer.

| Ref | SHA | Parent (branch head) | Files vs parent | `git status --porcelain` lines (what `restore_wip` prints) |
|---|---|---|---|---|
| `refs/heads/backup/WP03-wip-2026-10-16` | `847bd145781c7b31315b7c0705746f4d4adc4f34` | `b595f9d` (`wp/WP03`) | 132: 31 modified, 3 deleted, 98 added (10,955 insertions, 990 deletions) | 47 (new folders count as one line; 132 with `--untracked-files=all`) |
| `refs/heads/backup/WP04-perf-wip-2026-10-16` | `988ee432bc38b187e4b1b163e770ad85bfb06c0f` | `fb866b3` (`wp/WP04-perf`) | 4 modified (49 insertions) | 4 |
| `refs/heads/backup/WP05-glass-wip-2026-10-16` | `240df1ac430dfca9ae65f37a62dba9ea3115fed8` | `282dd3c` (`wp/WP05-glass`) | 43: 35 modified, 8 added (2,310 insertions, 216 deletions) | 43 |

No other android-base worktree had uncommitted or untracked files. All 13 under `.overhaul/wt/` were checked:
WP01, WP02, WP03, WP03-skel, WP04, WP04-perf, WP05, WP05-glass, WP06, WP07, WP08, WP09 and WP10. The main checkout
`android-base/` and `.overhaul/scratch/WP09-trial` were clean too. No snapshot contains a keystore,
`google-services.json`, `local.properties`, `keystore.properties` or `.env` file.

WP03's snapshot holds the whole bottom-sheet system. It does not compile (`SheetFlingBehavior.kt:88`).

### 1.4 Extra ref: the WP09 trial merge

| Ref | SHA | Why |
|---|---|---|
| `refs/heads/backup/WP09-trial-28cd271` | `28cd271a9ccf2dd6e94cde0b0dd0434ba2fb8d77` | The detached worktree `.overhaul/scratch/WP09-trial` sat on this commit ("Merge branch 'wp/WP01' into HEAD", Oct 9, author `trial <trial@local>`). It was on no branch and no tag, so it would have been lost. `MANIFEST.md` §9 suggested this name. It has low value: both parents are on pushed branches. |

### 1.5 Workspace branch `overhaul-workspace` (orphan)

| Ref | SHA | Commit |
|---|---|---|
| `refs/heads/overhaul-workspace` | `a9617013ec3d3bc9cd54abd032b55ec89e5007f2` | Back up the android-base overhaul workspace before the laptop wipe (2026-10-16) |

Later commits on top (normal fast-forward pushes, never forced):

| Commit | What |
|---|---|
| `7feafaf9d306f4789ecdc979e99112a93b888f1c` | adds this file (`.overhaul/handover/PUSHED.md`) and re-syncs `logs/thermal.log` |
| `ae3168a11314f68fbbf84f28a7bdca9e52bf0714` (fix round 1, §6.3) | the corrected handover documents, the refreshed `claude-memory/`, the redacted `WIPE-READINESS.md`, the new `README.md` source, `tools/sync-workspace-branch.sh` |

The branch head is always: `git ls-remote https://github.com/tripathisarthak457/android-base.git refs/heads/overhaul-workspace`.

**What the branch holds** (first commit): 13,335 files, 498,917,457 bytes (476 MiB) of content; the local `.git` is
284 MiB packed. The largest file is `.overhaul/perf/WP03/cp1-phone/not-driven/toast/toast_crop_strip.png`
(30,758,988 bytes). No file is over 50 MB, and rsync ran with `--max-size=50M` (it skipped nothing).

| Path in the branch | Source |
|---|---|
| `README.md` | written for this branch: what it is and how to restore |
| `HANDOVER.md`, `CLAUDE.md`, `tools-CLAUDE.md` | `.overhaul/handover/HANDOVER.md`, `CLAUDE.md`, `tools-CLAUDE.md` |
| `.overhaul/` | `rsync -a --filter='merge .overhaul/handover/backup-include.txt'` of the workspace (13,327 files, the same count as the dry run) |
| `.overhaul/handover/` | the whole handover folder except `local-only/` and (since fix round 1) `claude-memory/` |
| `claude-memory/` | first commit: the live `~/.claude/projects/-home-crazyhusir-Documents-projects-tools/memory/` (`MEMORY.md` plus 3 memory files). **Since fix round 1:** `.overhaul/handover/claude-memory/`, the same four files refreshed for Windows (they point at `HANDOVER.md`, say what was pushed, the 39 requirements, the merge-all status, the push policy). The live Linux memory was left unchanged; its originals stay in commit `a961701` |
| `README.md` | first commits: written in the scratch build folder. **Since fix round 1:** `.overhaul/handover/BRANCH-README.md` |

**Redacted in the branch copy only** (the local files are unchanged). These were local dev-only values for a
throwaway Postgres container and a local API on `127.0.0.1`. Each one is now `<local-dev-value>`:

- `.overhaul/handoff/WP09.md` lines 71 and 73 (`POSTGRES_PASSWORD`, the password in `TEST_DATABASE_URL`);
- `.overhaul/out/WP10/run-api.sh` lines 4 and 9–11 (`POSTGRES_PASSWORD`, the password in `DATABASE_URL`,
  `ADMIN_TOKEN`, `IP_SALT`);
- `.overhaul/handover/MANIFEST.md` lines 121–122, which quoted the same values.

Restore notes: the branch is a **copy**, not a checkout of the workspace (see `RESUME-ON-WINDOWS.md` §3.4). Use
`git add --all --force` if you ever re-commit it: `scratch/WP09-trial` and `scratch/WP07-skel` carry their own
`.gitignore` files. Since fix round 1 the whole rebuild (copy, redaction, secrets check, commit, push) is one script:
`.overhaul/handover/tools/sync-workspace-branch.sh` (`HANDOVER.md` §6.1 #6). On this laptop it ran with
`WS_DIR=<scratchpad>/handover/workspace MEM_SRC=.overhaul/handover/claude-memory`.

## 2. PeriFlow (PRIVATE): `https://github.com/JorwenLabs/periflow` (pushed through the old URL `tripathisarthak457/periflow`)

`gh repo view` reported `"visibility": "PRIVATE"` before the push. **The repository has moved:** GitHub now names it
**`JorwenLabs/periflow`** (an organization). The old URL redirects, and the push through
`tripathisarthak457/periflow` worked. `gh repo view JorwenLabs/periflow` also says PRIVATE. Use the new name on
Windows: `git remote set-url origin https://github.com/JorwenLabs/periflow.git`. Members of the JorwenLabs
organization can see private repos of that organization if their role allows it.

| Ref | SHA | What |
|---|---|---|
| `refs/heads/overhaul/smooth-glass` | `45b91a207097f47b8f5423dc67ca35e0cb705a9b` | the overhaul's PeriFlow branch (worktree `.overhaul/wt/WP11`); 16 commits that were not on GitHub before |
| `refs/heads/backup/smooth-glass-wip-2026-10-16` | `7c1acf4ae9fa1dd37cae7a8170c88e24c6c1ef46` | WIP snapshot, parent `45b91a2`: 2 modified files, `periflow-android/core/designsystem/.../foundation/glass/LiquidLens.kt` and its test `LiquidLensTest.kt` |

The WP11 snapshot was built the same way as in §1.3, but with `git add -- <explicit paths from git status>`, never
`-A`. `periflow-recovery-codes.txt` and `PeriFlow-Postman/` were not touched: they are untracked in PeriFlow's main
checkout, not in `wt/WP11`. PeriFlow's `main` (`240871f`) was not touched.

Scan before the push: the 17 new commits (16 on the branch plus the snapshot) add 99 blobs under 181 distinct
paths. None of them is a secret-named file, and none holds a token or a password value.

**Not pushed, already safe:** every other local PeriFlow branch either matches GitHub or has 0 commits that are not
already on a remote branch. Local `develop` differs from GitHub's `develop` (`ff55ba9`, not fetched into the local
clone), but all its commits are on remote branches. `worktree-agent-a0577f8311e34315e` and
`worktree-agent-ac2f656f3b85c82d7` both point at `32945fc`, which is on remote branches. The worktrees
`.claude/worktrees/agent-*` and `.overhaul/wt/WP11-base` (detached at `a5b630a`, on `overhaul/smooth-glass`) are clean.

**Not pushed, git-ignored local files in PeriFlow:** `KEYS.md`, `HANDOVER-2026-09-26-1025.md`, `done-till-25th-11pm.md`,
`.claude/settings.local.json` and build outputs (`periflow-admin/dist/`). They are covered by the user's offline
backup plan (`tools/backup-2026-10-16/plan-paths.txt`: `KEYS.md` in `01-secrets`, the notes in
`05-repo-ignored-and-uncommitted`). This agent did not check whether that backup finished.

## 3. Still to go: the draft release `overhaul-raw-data-2026-10-16`

**Not done by this step.** On Oct 16 at 12:25, no release existed on either repo (`gh release list` was empty), and
at 12:45 it still did not (`gh api repos/tripathisarthak457/android-base/releases` and the same for
`JorwenLabs/periflow` returned nothing). The workflow's next step (Archive, `scripts/handover-backup.js`) was due to
create it; if it ran, it appended the archives, sizes and sha256 to the end of this file. The archives and their paths are
listed in `.overhaul/handover/release-assets.txt`. The packing recipe is in `MANIFEST.md` §3. Always create the
release with `--draft`.

**If no archive list was appended to this file (after §6), the release was never made.** Then these files exist only in the user's
Google Drive backup `backup-2026-10-16` (built and coverage-checked on the laptop at 12:19; the upload itself is
unconfirmed): `perf/WP04` in `00-android-base-not-on-github-p12.zip`; `perf/WP11` (PeriFlow) in p11 and p13; the rest
of `perf/` in p10; `audit/`, `qa/`, `research/` and `scratch/` in p01. How to restore from there:
`RESUME-ON-WINDOWS.md` §3.10.

| Archive | Raw size | Target (MANIFEST.md §3 decision) |
|---|---:|---|
| `overhaul-raw-traces.tar.zst` | 1.275 GB | android-base draft release |
| `overhaul-recordings.tar.zst` | 0.176 GB | android-base draft release |
| `overhaul-private-app-listings.tar.zst` | 0.021 GB | android-base draft release |
| `overhaul-frames-1.tar.zst` | 1.635 GB | android-base draft release |
| `overhaul-frames-2.tar.zst` | 1.327 GB | android-base draft release |
| `overhaul-periflow-media-1.tar.zst` | 1.309 GB | draft release with the same tag on the PRIVATE PeriFlow repo (now `JorwenLabs/periflow`); fallback: android-base draft |
| `overhaul-periflow-media-2.tar.zst` | 1.595 GB | same as above |
| `overhaul-periflow-media-3.tar.zst` | 1.699 GB | same as above |
| `overhaul-periflow-media-4.tar.zst` | 0.094 GB | same as above |

Until those archives are uploaded, these files exist only on this laptop and in the user's offline backup (if it
holds them).

## 4. Deliberately not pushed, and why

| What | Why |
|---|---|
| `main` of android-base | It is production: Vercel and the CI deploy job ship it. It stays untouched until the Android release gate and the user's go-ahead. Local `main` equals `origin/main` (`99c4edf`). |
| `refs/backup/wp03-2026-10-15` (`f574221`, a non-branch ref in android-base: the Oct 15 stash of WP03) | Superseded by `backup/WP03-wip-2026-10-16`. Its contents are also in the workspace branch as `.overhaul/backup/WP03-2026-10-15/` (`tracked.diff`, `untracked.tgz`, `stash-hash.txt`). Stash commits can carry ignored files, so it was not published unchecked. |
| `.overhaul/wt/` (15 worktrees, 15.2 GB) | They are git: the branches and WIP snapshots above hold their content. Their build folders can be rebuilt. |
| Build outputs (generated projects, `build/` folders, APKs, `.dex`/`.class`/`.jar`, compiled Go servers, zips of generated projects, lock files; about 14.4 GB outside `wt/`) | Regenerable. Each generated project keeps `generator-spec.json`, `BUILT_FROM` and `BUILT_DIRTY` in the workspace branch (`MANIFEST.md` §4). |
| `research/predictive-back/gmail/`, `research/predictive-back/record-gmail-r2.log`, `perf/WP11/after-early/run2/failure_run2_041.png` and `.xml` | Other people's personal data (real e-mails), or the user's Google account. Never published (`MANIFEST.md` §5). |
| `scratch/pf-diff/`, `scratch/vitfleet/` | Copies of PeriFlow and VitFleet source code (private repos). Never in the public repo. |
| Secrets (keystores, `keystore.properties`, `local.properties`, real `google-services.json`, `.env`, `verify-gen/ks/p.properties`, tokens, `periflow-recovery-codes.txt`, `PeriFlow-Postman/`) | Never published. The PeriFlow secrets are in the user's offline-backup plan (`01-secrets-HELD-list.txt`). The user has to build that zip himself. |
| Chat transcripts (`~/.claude/projects/*/*.jsonl`, subagent transcripts) | They contain secrets. Their content is summarised in `CHAT-HISTORY.md` and `USER-MESSAGES.md`, which are in the branch. The transcripts themselves are only in the offline backup. |
| `handover/local-only/` | Out of scope by design (it was empty at the first push; since fix round 1 it holds `WIPE-READINESS-full.md`). |
| The full `WIPE-READINESS.md` (since fix round 1) | It mapped where the user's keystores, SSH deploy key, 2FA codes and service-account file lived and named his work-organisation repos, a colleague-named branch and a client's server. Not needed to resume; the public copy is now redacted (§6.2). The full one: `handover/local-only/WIPE-READINESS-full.md` on the laptop and `00-android-base-not-on-github-p01.zip` on Drive. |

## 5. Secrets scan before the public push (android-base)

- **Scope:** every object that the 22 refs reach and `origin/main` does not: 584 commits, 9,413 trees and 3,983 blobs
  (26 MB). This covers every commit of history that became public, merges included. Also scanned: the full tree of each
  ref (`git grep`), every commit message, and every author and committer address.
- **File names:** no keystore, `.p12`, `.pem`, `.key`, `local.properties`, `keystore.properties` or `.env`. Three
  name matches, all harmless:
  - `templates/android/app/google-services.json` is already public on `main` as the same blob
    (`template/app/google-services.json`). It is a placeholder: `AIzaSyPLACEHOLDER0000…`, project
    `baseapp-placeholder`, app ids made of zeros.
  - `templates/android/keystore.properties.template` is also the same blob as on `main`. Its password fields are
    empty.
  - `web/frontend/.env.example` holds only comments.
- **Tokens** (`ghp_`, `github_pat_`, `gh?_`, `sk-`, `AIza`, `AKIA`, `xox?-`, private-key headers, JWTs, npm or
  Vercel tokens): none. The only `AIza` string is the placeholder above.
- **Password and secret literals:** all are obvious test fixtures.
  - `hunter22`, `hunter2`, `secret1`, `secret`, `p@ss word`, `devpass1`, `keypass1`, `pässwörd1`, `different`,
    `other-one`, `secret-on-argv`, `short`, `first1234`, `second1234`: generator, Go and Kotlin tests, and validation
    cases.
  - `0123456789abcdef0123456789abcdef`: a Go test admin token.
  - `salt`: a Go test salt.
  - `postgres://wp09@127.0.0.1:1/none` (Go tests): no password, and port 1 on purpose, so it cannot connect.
  - `postgres://secret` and `postgres://localhost/x`: test environment values.
  - In `web/README.md`, `ADMIN_TOKEN` and `IP_SALT` are generated with `openssl rand`.
- **Private apps:** the only PeriFlow or VitFleet content is `generator/tests/fixtures/v1/periflow.json` and
  `vitfleet.json`. They are generator spec fixtures (app name, package, features, `example.com` URLs, no keystores), not
  app source.
- **Result:** no real secret. Nothing was blocked.

## 6. Fix round 1 (Oct 16, about 12:45–13:00 IST)

A fresh-context verifier cloned the public repo, checked every ref, the WIP, the contents and the secrets, and did a
cold read of the documents. It found the refs, WIP and secrets in order, and nine gaps in the documents plus one privacy
note. This round fixed them. It changed no package branch, no `main`, and nothing in PeriFlow.

### 6.1 Re-checks (no ref or WIP was missing)

- `git ls-remote` of android-base: all 22 refs of §1 at the same SHAs; `overhaul-workspace` at `7feafaf` before this
  round; `main` = `99c4edf`; **no `archive/*` tags** (so merge-all did not run). `JorwenLabs/periflow`: both refs of
  §2 at the same SHAs; PRIVATE.
- Local refs equal the remote ones. Each dirty worktree's current files, written into a temporary index
  (`read-tree HEAD`, `add -A`; for WP11 only the paths `git status` lists), give exactly the tree of its snapshot:
  WP03 `ed06126`, WP04-perf `1cb11c5`, WP05-glass `79f4543`, WP11 `49b74fe`. Nothing new to push.
- No secret was found on the public repo, so nothing needs rotating from this round's checks.

### 6.2 What changed in the documents (all under `.overhaul/handover/`)

| Gap | Fix |
|---|---|
| 1. Merge-all had no clear status | One explicit line in `HANDOVER.md` §0 (new status box and bullet), §1.7 (new item 0, with the reasons and a recommendation), `CLAUDE.md`, `tools-CLAUDE.md`, the branch `README.md`, `RESUME-ON-WINDOWS.md` §1 and §9, `PACKAGES-AND-BRANCHES.md` §0.2 and §9.3, `REQUIREMENTS-STATUS-FINAL.md` §9 item 7, `CHAT-HISTORY.md`, and the memory: **merge-all did not run; it is pending the user's decision; do not run it without asking** |
| 2. Docs said the draft release existed | Every mention made conditional (`HANDOVER.md` §0 box, §6.2 #12, §8.2 step 8; `RESUME-ON-WINDOWS.md` §3.10, §3.11; `PACKAGES-AND-BRANCHES.md` §0.1; `REQUIREMENTS-STATUS-FINAL.md` §0.2; `MANIFEST.md`; the branch `README.md`; `CLAUDE.md`), with the Drive fallback and which zip holds which folder (§3 above). The release itself is the Archive step's job |
| 3. PeriFlow repo renamed | `JorwenLabs/periflow` everywhere a reader would use it (HANDOVER §0.1, §2.9, §6.2, §8.2 step 7, §9.9; `CLAUDE.md`; `tools-CLAUDE.md`; RESUME §3.7, §3.10; PACKAGES §0.1, §4, §8.5; REQUIREMENTS-STATUS-FINAL; CHAT-HISTORY); `restore-on-wsl.sh` `PF_URL` default |
| 4. Three different relaunch orders | One canonical order in `HANDOVER.md` §5.11 (wave 1 `to-release` + `deep-smoothness`; wave 2 `shimmer` (Verify only, phone attached) + `android-skills`; wave 3 `full-liquid-glass`), copied into §4.7, §8.2 step 14, §8.3, RESUME §8.2 and PACKAGES §9.2 |
| 5. Stale memory copy | `claude-memory/` rebuilt from `.overhaul/handover/claude-memory/` (refreshed for Windows); HANDOVER §8.2 step 5 and RESUME §3.8 now say no edit is needed |
| 6. Push policy ambiguous | Decided and recorded in `HANDOVER.md` §6.1 #6: the orchestrator pushes `wp/*`, `overhaul/*`, `merged/*` and the workspace branch at every session end and after every merge (never `main`, never force); agents never push; worktrees stay `--no-track`. Tool: `tools/sync-workspace-branch.sh`. Aligned in §1.3 #15, §1.8, §5.9, PACKAGES §9.1, RESUME §3.5 |
| 7. 38 vs 39 requirements | 39 in `HANDOVER.md` §0 and §3.2 (new row 39), §9.1/§9.2, `CLAUDE.md`, `tools-CLAUDE.md`, the branch `README.md`, `REQUIREMENTS-STATUS-FINAL.md` header, the memory |
| 8. Tools unverified in the CLI | New `HANDOVER.md` §5.14 (preflight with `ToolSearch`, fallbacks for `Workflow`, cron, `Monitor`, usage); preflight added to §5.11, §8.2 step 1, `CLAUDE.md`, `tools-CLAUDE.md`, RESUME §2.1, §8.1 item 0, §9 |
| 9. 11:30 vs 11:17 | 11:17 is the user's message; "~11:30" is when `PROGRESS.md` logged it (HANDOVER §1.2 row 43, RESUME §1, §9, PACKAGES §0.2) |
| Privacy note | `WIPE-READINESS.md` on the branch head is now a redacted public copy (only android-base and PeriFlow; no work-organisation, colleague or client names). The full scan moved to `handover/local-only/WIPE-READINESS-full.md` (never published) and is in the Drive zip p01. **Commits `a961701` and `7feafaf` still contain the full version**: removing it needs a force-push or deleting and recreating the branch, which is the user's call (`HANDOVER.md` §1.7 item 10; `REQUIREMENTS-STATUS-FINAL.md` §9 item 16). One work-project name was also generalised in `USER-MESSAGES.md` |

Also corrected while there: RESUME §3.11 (14 parts, `01-secrets.zip` was built by the user, the offline backup
finished), the offline backup's status in HANDOVER §5.2, §8.1 and §9.9, and `--no-track` in HANDOVER §8.2 step 3.
New user decisions were added to `REQUIREMENTS-STATUS-FINAL.md` §9 (items 14–16) and `HANDOVER.md` §1.7 (items 0, 9,
10).

### 6.3 The fix-round commit

Built with `.overhaul/handover/tools/sync-workspace-branch.sh` (`WS_DIR` = the scratchpad build folder,
`MEM_SRC=.overhaul/handover/claude-memory`): rsync with `backup-include.txt` (now also excluding
`handover/claude-memory/`), the generic redaction of the local dev values, the secrets check of the staged diff
(no hits), a normal commit and a fast-forward push (`7feafaf..ae3168a`).

| Ref | SHA | Commit |
|---|---|---|
| `refs/heads/overhaul-workspace` | `ae3168a11314f68fbbf84f28a7bdca9e52bf0714` | Fix the handover gaps a fresh-context verifier found (24 files) |

A last small commit on top records this SHA here; the branch head is whatever `git ls-remote` shows.

