# Workspace backup manifest: `.overhaul` to GitHub (2026-10-16)

This manifest records how every file of the workspace `/home/crazyhusir/Documents/projects/tools/.overhaul` is backed up before the laptop is wiped. It was built on Oct 16, around 11:30, from a full file inventory (`find`, `du`, `file`), a file-by-file classifier, a secrets scan and a personal-data review. Two companion files sit next to it:

- `backup-include.txt`: the rsync filter that selects the **git** part (branch `overhaul-workspace` of the PUBLIC repo `tripathisarthak457/android-base`).
- `release-assets.txt`: the **draft-release archives** (release `overhaul-raw-data-2026-10-16`, visible only to the owner). One line per archive: the archive name, then its paths, separated by TABs.

The scope is `.overhaul` only. Claude's memory, the chat transcripts and the git branches are handled by other steps of the handover.

> **Status (12:45, handover fix round 1).** This file is the plan. The git part was pushed as planned (`PUSHED.md` §1.5).
> The draft release had **not** been created yet (`PUSHED.md` §3); until it is, every path in `release-assets.txt`
> exists only in the user's Google Drive backup (`00-android-base-not-on-github-p*.zip`; `RESUME-ON-WINDOWS.md` §3.10).
> The PeriFlow repo is now named `JorwenLabs/periflow` (formerly `tripathisarthak457/periflow`). The filter
> `backup-include.txt` now also leaves out `handover/claude-memory/` (that folder becomes the branch's top-level
> `claude-memory/` instead).

## 1. Totals

| Where it goes | Files | Size | Notes |
|---|---:|---:|---|
| **git**: branch `overhaul-workspace` (public) | 13,315 | 498 MB (475 MiB) | largest file 30.8 MB; tested with rsync (see §8) |
| **draft-release archives** (owner-only), 9 archives | 22,047 unique (23,476 counting 1,429 small text files that are also in git) | 9.13 GB raw (8,708 MiB); about 8.0 GB after zstd (estimate) | largest archive 1.70 GB raw; all under 1.9 GB |
| **left out, regenerable**: generated projects, build outputs, APKs, binaries, lock files | 546,600 | 14.4 GB | see §4 |
| **left out, regenerable**: `wt/` (git worktrees) | 356,955 | 15.2 GB | WIP goes to `backup/<name>-wip-2026-10-16` branches |
| **never published** (flagged) | 3,564 | 384 MB | see §5 |
| **secrets** (left out) | 158 | 0.3 MB | 154 of them inside generated projects; see §6 |

The workspace holds 585,684 files outside `wt/` (24.4 GB) and 356,955 in `wt/` (15.2 GB): 39.6 GB in all. `du -sh` shows `42G` on disk and `37G` apparent; those are GiB.

Upload estimate: the git push is about 0.5 GB. The archives are about 8.0 GB: PNG frames and MP4 recordings do not compress (measured: a frame PNG stays at 100 %), raw atrace text drops to about 7 % and a Perfetto trace to about 21 %, so `overhaul-raw-traces` shrinks from 1.28 GB to roughly 0.13 GB.

## 2. What goes to git, and why

Everything that is a note, plan, handoff, decision, review, report, script, log or small result goes to git. Every kept file is under 50 MB.

| Area | Files | MiB | What it is |
|---|---:|---:|---|
| top level | 13 | 0.1 | STATUS.md, SESSION-2026-10-13..16.md, PROGRESS.md, REQUIREMENTS.md, REQUIREMENTS-STATUS.md, FABLE-FINDINGS.md, RELEASE-NOTES-android-draft.md, merges.md, followups.md, peek.py |
| design/, decisions/, handoff/, handover/, docs-draft/, notes/, reviews/ | 55 | 1.4 | MASTER-PLAN and per-package designs, every handoff and RESUME brief, decisions, docs drafts, reviews |
| scripts/, bin/ | 42 | 0.6 | the Workflow scripts (to-release.js and the others, with their `.pre-*` versions), `locked`, `memwait`, `thermlog` and their earlier versions |
| research/ | 141 | 2.7 | research and plans (SHEET-SPEC, MOTION-PLAN, glass, sheet, the predictive-back SPEC and recording scripts) |
| audit/ | 26 | 5.4 | audits, `_result.json`, and the composite images in `audit/frames/` of the generated app |
| perf/ | 5,343 | 353.9 | every perf report: summary.md/json, framestats and gfxinfo text, logs, gates, perf tools, strips and tiles (report images), QA screenshots; PeriFlow measurements as numbers only |
| qa/ | 342 | 41.4 | QA reports, defect lists, screenshots and evidence images, recording scripts |
| out/ | 2,473 | 31.0 | everything in `out/` that is not a generated project or a build: accept scripts, logs, specs, patches, diagnostics, size reports, `out/WP03/src` and `wip-rev1`; plus `generator-spec.json`, `BUILT_FROM` and `BUILT_DIRTY` of each generated project (257 files), which is what is needed to regenerate it |
| scratch/, scratch-gen/, verify-gen/, verify-webapi/ | 4,590 | 36.5 | probe scripts and their results, device scripts, `scratch/WP09-trial` (see §9), `scratch/WP07-skel`, and extracted third-party sources (see §9) |
| logs/, backup/ | 290 | 2.0 | lens and WP11 logs and scripts, thermal.log, and the WP03 backup of Oct 15 (`tracked.diff`, `untracked.tgz`, `stash-hash.txt`) |

Notes about PeriFlow (audits, handoffs, logs, numbers, scripts that drive PeriFlow on the phone) are kept; they contain no PeriFlow source files. The longest code block in `audit/periflow-learnings.md` is a proposed policy for the template, not a copy of a PeriFlow file.

## 3. What goes to the draft release, and why

These files are kept, but they are too bulky for git (raw frame dumps, recordings, raw traces) or must not be in a public branch (PeriFlow screens, listings derived from private APKs). Sizes are raw, before compression.

| Archive | Files | Raw size | Contents |
|---|---:|---:|---|
| `overhaul-raw-traces.tar.zst` | 1,084 | 1.275 GB | all 7 Perfetto traces and 13 raw atrace dumps (`atrace.txt`, `recompose_atrace.txt`, up to 85 MB each), with the run folders around them (perf/WP03/ptr, perf/WP04 phase3c/3d/3e, r14, r16, perf/WP04-perf, perf/WP11/lens-tap/trace-run10) |
| `overhaul-recordings.tar.zst` | 117 | 0.176 GB | every screen recording (MP4) of the generated app and catalog (perf/WP03, perf/WP04, qa, scratch/device) |
| `overhaul-private-app-listings.tar.zst` | 3 | 0.021 GB | `scratch/template-build/peri-dex-packages.txt`, `vit-dex-packages.txt` and `vit-res.txt`: package, class and resource listings of the private PeriFlow and VitFleet APKs |
| `overhaul-frames-1.tar.zst` | 6,039 | 1.635 GB | raw frame dumps (`frames/f_NNNN.png`) of perf/WP03 (cp1, cp1-phone, ptr) and perf/WP04 (back-during-push, baseline, cp1, cp1-emulator, d9, emulator-m1) |
| `overhaul-frames-2.tar.zst` | 5,154 | 1.327 GB | raw frame dumps of perf/WP04 (phase3-frames, phase3a-frames, predictive-back, rtl, tab-switch), qa/ptr, qa/shimmer/evidence and `scratch/device/results/fr_*` |
| `overhaul-periflow-media-1.tar.zst` | 4,402 | 1.309 GB | PeriFlow screens: perf/WP11/before, after-early, after-early-2, lens-tap/after-early-2*, and the 5 `audit/frames/periflow_*.png` |
| `overhaul-periflow-media-2.tar.zst` | 4,428 | 1.595 GB | PeriFlow screens: perf/WP11/lens-tap before-v4, every-frame-lens-4..6, lens-1..4 |
| `overhaul-periflow-media-3.tar.zst` | 1,661 | 1.699 GB | PeriFlow screens: perf/WP11/lens-tap lens-5, lens-6, strips-*, trace-lens-4/5 failure dumps, perf/WP11/smoke |
| `overhaul-periflow-media-4.tar.zst` | 588 | 0.094 GB | PeriFlow screens from Oct 8 in `scratch/device/results` (`fr_peri_*`, `rec_peri_*`, `tile_peri_*`, `shot_p*`, `ui_p*`, `ui_tt.xml`) |

A directory listed in `release-assets.txt` means every file under it. Such a directory holds only files of that archive plus, at most, small text files that are also in git (at least 90 % of its bytes belong to the archive). No listed path contains a never-publish file, a secret or a build output; this was checked file by file.

**PeriFlow media.** These are screenshots, frames, recordings and UI dumps of PeriFlow, a private, unreleased app. They are not source code, but they are kept out of the public branch. The measurement runs clear the app and start it with `perf.fakeSession=true`, so the screens show test data. The Oct 8 runs in `scratch/device` installed the dev build without clearing it, so whose data those screens show is unknown. Target: the draft release of android-base, which only the owner can see. A safer place is a draft release with the same tag on the PRIVATE repo `tripathisarthak457/periflow`: publishing it by mistake would still not make it public. **Decision: upload the four `overhaul-periflow-media-*` archives to a draft release on `tripathisarthak457/periflow`, with the same tag. If that fails, the android-base draft release is the fallback. The other five archives go to the android-base draft release.**

Packing one archive, run from the workspace:

```bash
cd /home/crazyhusir/Documents/projects/tools/.overhaul
NAME=overhaul-frames-1.tar.zst
grep -P "^${NAME}\t" handover/release-assets.txt | cut -f2- | tr '\t' '\n' \
  | tar -cf - -T - | nice -n 19 zstd -3 -T1 -q -o "<scratch>/${NAME}"
stat -c %s "<scratch>/${NAME}"     # must be < 1,900,000,000
```

`-T1` keeps zstd on one thread. Pack one archive at a time, with `bin/memwait` between archives: the laptop powered off three times on Oct 16 under sustained CPU heat. Create the release with `gh release create overhaul-raw-data-2026-10-16 --draft ...`. Never leave out `--draft`.

## 4. Left out because it is regenerable

| What | Files | Size | How to get it back |
|---|---:|---:|---|
| `wt/`: the 15 git worktrees (13 android-base packages; `WP11` and `WP11-base` are PeriFlow) | 356,955 | 15.2 GB | the branches, plus the WIP snapshots `backup/<name>-wip-2026-10-16`; PeriFlow's `overhaul/smooth-glass` and its WIP go to the private PeriFlow repo. `WP11-base` is a clean detached checkout of `a5b630a`, which is on `overhaul/smooth-glass`. |
| generated projects: 229 roots in `out/` (gen-*, Full, FullDevtools, addtest, parentdir, WP00..WP08 outputs) and 11 in `verify-gen/out/` | 528,734 | 13.9 GB | run the generator with the kept `generator-spec.json` of each root; `BUILT_FROM` and `BUILT_DIRTY` give the commit and dirty state |
| other build folders, mainly `out/WP01/build/{standard,standard-merged}` (generated projects with their builds) and `__pycache__/` | 17,149 | 0.34 GB | rebuild |
| APKs: `perf/WP11/apk/` (14 PeriFlow APKs, 12.7 MB each), `out/WP03/apk/` (2 catalog APKs and an `.idsig`), `qa/shimmer/after/device/installed-com.example.wp03.cp1.catalog.apk` | 18 | 0.18 GB | rebuild. The PeriFlow `before-*` APKs carry their commit in the name. `handoff/WP11.md` names `f9f357e` and `06ad83e` for after-early 1 and 2. `lens-tap-1..6` were built from changing WIP in `wt/WP11`: only the latest state survives, in the WIP snapshot, so the older ones cannot be rebuilt byte for byte. Their measurements are kept. |
| `.dex` (`classes.dex` in `scratch/template-build/vitdev/` and `vitprod/`, from VitFleet APKs), `.class`, `.jar` (downloaded sources jars, two tiny stub jars) | 160 | 20 MB | rebuild, or download from Maven |
| compiled Go servers `out/WP09/server`, `out/WP10/api-server` | 2 | 35 MB | `go build ./cmd/server` in `web/api` |
| zips of generated projects (`out/WP01/p.zip`, `t.zip`, `out/WP08/ios.zip`, `out/WP09/p.zip`, `out/parentdir2/JsonCheck.zip`, `scratch/webapi/full.zip`, `verify-gen/out/h2.zip`, `verify-gen/out/jsoncheck/Bare.zip`); two of them hold a `google-services.json` | 8 | 2.5 MB | the web API or the generator |
| `out/WP02/r8-data.b64` (base64 R8 build data) | 1 | 0.3 MB | rebuild |
| `*.lock` files (`adb.lock`, `emu.lock`, `gradle.lock` and Gradle locks inside generated projects) | 527 | 0 | created again by `bin/locked` |
| `scratch/WP09-trial/.git` (worktree pointer file; git refuses that path) | 1 | 89 B | not needed |

## 5. Never published (flagged): not in git, not in the draft release

These are **not backed up by this plan**. If the user wants any of them, they must copy them to private storage (USB disk or private drive) before the wipe.

| Path | Files | Size | Why |
|---|---:|---:|---|
| `research/predictive-back/gmail/` | 2,677 | 379 MB | Gmail reference recordings (7 MP4), 2,666 frames, `key_frames.png`, `conversation.png`, `ui.xml` and `tap.txt`: they show real e-mails of other people. The user allowed motion analysis only. What the recordings taught is in `research/predictive-back/SPEC.md` (kept). |
| `research/predictive-back/record-gmail-r2.log` | 1 | 1.7 KB | line 3 is the subject of a real e-mail in the inbox: `row: 282 1405 <subject>`. **May be added back to git redacted**: `sed '3s/^row: \([0-9]* [0-9]*\) .*/row: \1 <redacted>/'`. The other recording logs and scripts (`record-gmail.log`, `record-gmail.sh`, `record-gmail-r3.sh`) are kept: they hold no e-mail content. |
| `perf/WP11/after-early/run2/failure_run2_041.png` and `.xml` | 2 | 1.0 MB | a failed run that captured the Google app instead of PeriFlow: the UI dump holds the user's Google account e-mail and a personal feed |
| `scratch/pf-diff/` | 408 | 2.0 MB | PeriFlow source copies: `pf/`, `pfapp/` (PeriFlow files), `base-raw/` (= `periflow-android` of PeriFlow's first base), `base/` (the same renamed to `com.base.app`). `tpl/` and `tplapp/` are copies of the android-base template, regenerable from git. PeriFlow's source lives in its private repo. |
| `scratch/vitfleet/` | 476 | 1.4 MB | a copy of VitFleet's `vitfleet-android/` at commit `25db943`. VitFleet is a PRIVATE repo (`tripathisarthak457/vitfleet`, checked with `gh repo view`), so its source stays out of the public repo. Regenerable from that commit. |

Nothing named `periflow-recovery-codes.txt` or `PeriFlow-Postman/` exists in the workspace. The filter still blocks both names as a guard.

## 6. Secrets scan

**Method.** File names: `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.pem`, `*.key`, `local.properties`, `google-services.json`, `GoogleService-Info.plist`, `keystore.properties`, `.env*` (except `.env.example`). Contents of every kept text file (12,461 text files of git and archives on the first pass; the 261 files added since, among them the handover files, were scanned again before writing this): `ghp_`, `github_pat_`, `gho_`/`ghs_`/`ghu_`/`ghr_`, `sk-…`, `AIza…`, `AKIA…`, private-key headers, Slack tokens, JWTs, `password`/`passwd`/`storePassword`/`keyPassword`/`store_password`/`key_password`/`secret`/`api_key`/`*_token` followed by a literal value, `ADMIN_TOKEN`, database URLs with credentials, `VERCEL_TOKEN`/`GH_TOKEN`/`NPM_TOKEN`.

**Hits by name (left out; the filter blocks them):**

- `verify-gen/ks/t.jks`: a test PKCS12 keystore.
- `verify-gen/ks/p.properties`: its test password (`dev.storePassword=…`).
- `scratch/WP07-skel/local.properties` (only `sdk.dir`).
- `scratch/WP09-trial/templates/android/app/google-services.json`: the template's placeholder (`AIzaSyPLACEHOLDER…`). It is in android-base git anyway.
- Inside generated projects, which are left out as a whole: 130 `google-services.json`, 6 `keystore.properties`, 18 keystores (`dev.jks` ×5, `prod.jks` ×5, `staging.jks` ×4, `playstore.jks` ×4).

**Hits inside documents and scripts.** These files are kept because they are needed. Each value is a dev-only credential for a throwaway local Postgres container or local API on `127.0.0.1`, not a real secret. Redact them in the branch checkout before committing:

| File | Lines | What |
|---|---|---|
| `handoff/WP09.md` | 71, 73 | `POSTGRES_PASSWORD=<local-dev-value>` and `TEST_DATABASE_URL='postgres://wp09:<local-dev-value>@127.0.0.1:55439/…'` (local docker container) |
| `out/WP10/run-api.sh` | 4, 9, 10, 11 | `POSTGRES_PASSWORD=<local-dev-value>`, `DATABASE_URL='postgres://wp10:<local-dev-value>@127.0.0.1:55440/…'`, `ADMIN_TOKEN='<local-dev-value>'`, `IP_SALT='<local-dev-value>'` (local API). Line 10 has the most value to redact: nothing on this laptop shows whether the production `ADMIN_TOKEN` (a Vercel env var) differs. The web README tells you to generate it with `openssl rand`. |

Suggested redaction: replace each value with `<local-dev-value>` in those lines only.

**Reviewed and kept (not secrets):**

- Test-fixture passwords (`devpass123`, `prodpass123`, `uploadpass123`, `hunter22`, `pässwörd1`, `secret1`) in `out/WP01/specs/spec_keys.json` and in the test files of `scratch/WP09-trial` (`generator/tests/…`, `web/api/…_test.go`). `spec_keys.json` is byte-identical to `generator/tests/fixtures/v1/spec_keys.json` on `overhaul/multiplatform-v2`, which is being pushed publicly anyway.
- `audit/generator-python.md:291` quotes the test password `pässwörd1`.
- `scratch/device/session.preferences_pb`: fake session tokens written as `AAAA…`.
- `scratch/template-build/vit-res.txt:9498`: a placeholder `AIza…` key. That file goes to the private-listings archive.
- Translation strings for "password" (`out/WP01/translations-v1/*.json`, the generator translations), accessibility dumps (`password: false`) and Go test names (`PASS:`).
- `scratch/WP09-trial/web/frontend/.env.example`: an example file, already in android-base git.

No GitHub, OpenAI, Anthropic, Google, AWS or Slack token was found, and no private key.

## 7. Personal-data review

- **UI dumps.** Every `uiautomator` dump in the workspace outside generated projects was read for the app on screen.
  - The 61 that go to git show only the generated apps, the emulator launcher (`perf/WP04/emulator-large/run1/failure_run1_051.*`: the emulator's default apps), an empty system-UI frame (`perf/WP04/smoke-try/...`) and the quick-settings shade (`scratch/device/results/ui_debug_launch.xml` with `shot_debug_launch.png`). The shade screenshot was checked at low resolution: no notification and no personal content.
  - The PeriFlow dumps, which include one permission dialog, go to the owner-only archive.
  - The Google-app dump with the user's account (`failure_run2_041`) is never published.
- **E-mail addresses** in kept text:
  - `tripathisarthak457@gmail.com` in `scratch/WP09-trial/web/frontend/components/creator.tsx`: the public contact on the site, already in android-base.
  - `ada+notes@example.co` (test data).
  - Two `@google.com` addresses inside third-party AndroidX and Kotlin source files.
  - The user's other address appears only in the never-published UI dump.
- **The user's name.** `out/WP04/logs/r8-net-diag.log:2` shows the phone owner's name, `Sarthak Tripathi`. The same name is on the public site (`creator.tsx`). Kept.
- **Logcats** (`out/WP06/diag/logcat*.txt` from the emulator, `perf/WP04/profile/emulator/logcat-1217.txt`, and every `nav_stress_logcat.txt` and `glass-tier-logcat.txt`): no account names, notification texts or messenger packages were found.
- **Screenshots and strips going to git** come from the generated app and catalog: lorem-ipsum data, test inputs such as `not-an-email`, and the user's own phone status bar (icons only). Two were checked at low resolution: `qa/user/back-glitch-1309.png` and `qa/r1/shots/light/36-signin-invalid-email-keyboard.png`.
- **Residual risk.** The 21,612 raw frames and the strips were not all opened. A heads-up notification could appear in a phone frame. Every raw frame dump is in the owner-only archives, not in git; the report images in git (strips, tiles, shots) are composites of the generated app.

## 8. The git filter (`backup-include.txt`) and its test

The rules are in this order; the first match wins:

1. never publish
2. out of scope (`wt/`, `handover/local-only/`)
3. secrets
4. regenerable (extensions, binaries, generated-project zips, build folders). Before the build-folder rules, the third-party AGP source trees are kept whole: their packages are named `com/android/build`.
5. generated projects: `+ generator-spec.json`, `+ BUILT_FROM`, `+ BUILT_DIRTY`, then `- /<root>/*` for each of the 240 roots
6. draft-release content
7. the allowlist of workspace folders
8. `- *` as the last line

Test run on Oct 16:

```
rsync -a --dry-run --stats --filter='merge /home/crazyhusir/Documents/projects/tools/.overhaul/handover/backup-include.txt' \
  /home/crazyhusir/Documents/projects/tools/.overhaul/ /tmp/x
Number of files: 15,967 (reg: 13,315, dir: 2,652)
Total file size: 498,058,162 bytes            (475 MiB, under 2 GB)
largest file: perf/WP03/cp1-phone/not-driven/toast/toast_crop_strip.png, 30,758,988 bytes (under 50 MB)
```

After this MANIFEST.md was written, the same command gave 13,316 files and 498,091,018 bytes, and still no file over 50 MB. The 13,315 files of the first run are exactly the files the classifier put in git, compared path by path. No kept path has characters that are invalid on Windows. No two paths differ only by case. The longest path is 152 characters.

## 9. Notes for the packer

- **Re-run the dry-run just before packing.** Other agents may add files, for example in `handover/` or `scripts/`. The allowlist takes new files in known folders and at the top level (`*.md`, `*.txt`, `*.py`, `*.sh`, `*.js`, `*.json`). A new top-level folder would be dropped by `- *`; check with `rsync -avn --debug=FILTER1 …`. For extra safety, add `--max-size=50M` to the rsync command line.
- **Use `git add --all --force`.** `scratch/WP09-trial` and `scratch/WP07-skel` contain their own `.gitignore` files (`*.log`, `/*.json`, `.env*`, `out/`, `*.zip`, …), and a global git excludes file could hide files too. After committing, check that `git ls-files | wc -l` matches the rsync count, plus any redacted file added back.
- **`scratch/WP09-trial`** is a clean, detached worktree of android-base at `28cd271` ("Merge branch 'wp/WP01' into HEAD"). That commit is on **no branch and no tag**, so its files are kept here. Pushing a ref such as `backup/WP09-trial-28cd271` would also preserve it. After the wipe, `git worktree prune` in a new clone forgets it.
- **Third-party sources kept in git:**
  - AndroidX navigation3, compose and macrobenchmark/baseline-profile in `scratch/nav3*`, `bp`, `macro`, `compose-ui`, `verify-ds/f112`, `verify-nav/compose`, `out/WP03/src`
  - AGP in `scratch/agp`, `template-build/agpsrc`, `agpapi`
  - KSP in `template-build/ksp`
  - the Kotlin Compose compiler Gradle plugin in `scratch/ccgp`
  - Ktor in `verify-nav/ktor`
  - Kyant backdrop in `scratch/kyant-backdrop`

  They are Apache-2.0 per their file headers or POMs. For Kyant backdrop, the POM of `io.github.kyant0:backdrop:2.0.1` in the Gradle cache says Apache 2.0. Audits cite these sources (for example "Nav3 source used to check claims: .overhaul/scratch/nav3src"). Their own `.jar` and `.class` files are left out.
- **Heat.** No Gradle is needed for any of this. tar, zstd and git compression still load the CPU: pack one archive at a time, at `nice -n 19`, and wait with `bin/memwait` until the CPU is below 80 °C.
- **Windows.** To extract on Windows, decompress with zstd's Windows build (`zstd -d NAME.tar.zst`), then run `tar -xf NAME.tar` (Windows 10 and later ship `tar.exe`). Archive member paths are relative to `.overhaul/`; none is longer than 72 characters or invalid on Windows.
- **`handover/WIPE-READINESS.md`** (written by another agent) names where secret files live on this laptop, by path only. The token scan found no secret value in it, and its header says it was written for the public branch. Reviewed and kept.
