# Resume the android-base overhaul on Windows

Written on Oct 16, 2026, about 12:00 IST, on the Pop!_OS laptop (Mi NoteBook Ultra, i5-11300H, 8 threads, 15 GB RAM)
just before it was wiped for Windows. It is for the next Claude and for Sarthak. Read `HANDOVER.md` first for the
project, the plan and the rules; this file covers only **how to get the same working machine back on Windows and
restart the work**.

Paths are given relative to the **tools root**: the folder that holds `android-base/` and `.overhaul/` side by side.
On Linux the tools root was `/home/crazyhusir/Documents/projects/tools`. The recommended Windows setup (section 2)
recreates **exactly that path inside WSL2**, so every hard-coded path in the scripts and notes keeps working.

What was tested (section 10 has the details): the restore script against a local copy of the repository shaped like
both possible GitHub layouts, with byte-for-byte checks of the restored work in progress; the WSL-safe `memwait` and
`bin/locked`; the path-rewrite recipe. Nothing could be tested **on Windows itself**, so every Windows-side step
below comes from documentation and is marked where it is uncertain.

---

## 0. The short version

| # | Step | Who | Section |
|---|---|---|---|
| 1 | Find out which backup layout GitHub has (branches, or everything merged into main) | Claude | 1 |
| 2 | Windows: WSL2 with **Ubuntu 24.04**, Linux user **`crazyhusir`**, `.wslconfig` with `memory=10GB`; lid, sleep and update settings; Intel DTT driver present | User (admin) | 2.2, 7 |
| 3 | Ubuntu: base packages, `gh auth login`, `gh auth setup-git`, git identity | User or Claude | 2.3, 3.1 |
| 4 | Install Claude Code **inside WSL** and run it from `~/Documents/projects/tools` | User | 2.4 |
| 5 | Run `restore-on-wsl.sh`: clones, workspace, 13 worktrees, WIP, PeriFlow, memory | Claude | 3.2 |
| 6 | Check the WIP counts (WP03 47, WP05-glass 43, WP04-perf 4, WP11 2) | Claude | 3.6 |
| 7 | Settings (Opus 5.5), the android-skills plugin 1.0.15 | Claude | 3.9 |
| 8 | Toolchain: JDK 21, Android SDK 37, Python 3.12, Node 24.21.0, Go 1.27.1, ffmpeg, Docker | Claude (sudo: user) | 4 |
| 9 | Phone: usbipd-win attaches the Redmi `9a7c74f4` to WSL; `adb devices` | User once, then Claude | 5 |
| 10 | Optional: raw-data archives from the draft release(s) | Claude | 3.10 |
| 11 | Optional: replace `bin/memwait` with the WSL version | Claude | 7.1 |
| 12 | Preflight checks, then relaunch the workflows **two at a time, fresh** | Claude | 8 |

Never, at any step: push or change `main` (Vercel and the CI deploy job ship it), force-push, publish PeriFlow code,
commit secrets, or kill processes you did not start.

---

## 1. Which backup layout do you have?

Two workflows ran on Oct 16, one after the other, and the second one changes the layout:

| Layout | Made by | What GitHub holds |
|---|---|---|
| **branches** | `scripts/handover-backup.js` (this guide's workflow) | `tripathisarthak457/android-base` (PUBLIC): `main` untouched; `overhaul/multiplatform-v2`, `wp/WP01`…`wp/WP10`, `wp/WP03-skel`, `wp/WP04-perf`, `wp/WP05-glass`; tags `merged/WP01`, `merged/WP02-M1`, `merged/WP09`, `merged/WP10`; WIP branches `backup/<name>-wip-2026-10-16`; the orphan branch `overhaul-workspace`. PeriFlow (PRIVATE): `overhaul/smooth-glass` and `backup/smooth-glass-wip-2026-10-16`. |
| **one-branch** | `scripts/merge-all-to-main.js` (the user confirmed it at about 11:30: "u merge it pls i get confused in lots of branches will stop vercel to deploy that we will do after testing") | Everything merged into `main` in both repos, the branches deleted, every former branch kept as the tag `archive/<branch>-2026-10-16`, the workspace under `overhaul-workspace/` on `main`, and Vercel deploys from `main` paused in `vercel.json`. That workflow also rewrites this guide; if you are reading an older copy, prefer the newer one under `overhaul-workspace/.overhaul/handover/` on `main`. |

Check (no clone needed):

```bash
git ls-remote --heads https://github.com/tripathisarthak457/android-base.git | sed 's#.*refs/heads/##'
git ls-remote --tags  https://github.com/tripathisarthak457/android-base.git | grep -c 'refs/tags/archive/'
```

- You see `overhaul/multiplatform-v2`, `wp/*`, `backup/*` and `overhaul-workspace`: **branches** layout. This guide
  applies as written.
- You see only `main`, and a non-zero count of `archive/` tags: **one-branch** layout. Read
  `overhaul-workspace/HANDOVER.md` and `MERGE-ALL-android-base.md` on `main` first: they say whether to keep working on
  `main` or to recreate package branches. The restore script (3.2) handles this layout too: by default it restores
  the workspace, the memory and the clones; with `RECREATE_BRANCHES=1` it also rebuilds every `wp/*` worktree and its
  WIP from the `archive/*` tags. Sections 2, 4, 5, 6 and 7 apply unchanged.
- Anything else: stop, and read the newest `HANDOVER.md` and `PUSHED.md` you can find.

---

## 2. The setup: WSL2 Ubuntu, with Claude Code running inside it

### 2.1 The decision and why

**Recommended:** the workspace lives on WSL2's own Linux disk at `/home/crazyhusir/Documents/projects/tools`, and
**the Claude Code process that runs the overhaul runs inside WSL** (the `claude` CLI in an Ubuntu terminal, or the
VS Code "WSL" window with the Claude Code extension). Windows is the host: the desktop, the browser, the Intel DTT
thermal driver, usbipd-win for the phone.

The brief for this guide suggested "native Windows Claude Code with the workspace inside WSL2". The workspace part is
right; the "native Windows Claude Code" part does not work for this project, for this reason: Claude Code's Bash
tool runs where Claude Code runs. On native Windows that is Git for Windows' bash (or PowerShell), not WSL. Then:

| Needed by the orchestration | Native Windows Claude Code (files in WSL or on NTFS) | Claude Code inside WSL2 |
|---|---|---|
| `bin/locked` needs `flock` (util-linux) | Git Bash has no `flock`: every `locked gradle` and `locked adb` fails | works |
| `bin/memwait` reads `/proc/meminfo`, `pgrep` | no `/proc` in Git Bash | works (7.1 for the heat part) |
| 32 files in `scripts/`, 7 handoffs, 16 design files hard-code `/home/crazyhusir/...` | wrong everywhere; a full rewrite to `C:\...` or `//wsl.localhost/...` | **unchanged** if the Linux user is `crazyhusir` |
| Gradle, adb, Python, Go on the workspace | Windows binaries over the `\\wsl.localhost` network share: very slow, file watching broken | native ext4 speed |
| Symlinks (`wt/WP11/periflow-android/*.properties`), exec bits (`gradlew`), case-sensitive paths, LF line endings | need Developer Mode, `core.autocrlf=false`, and still break | native |
| Claude memory slug | `C--Users-...` (a new, empty memory) | `-home-crazyhusir-Documents-projects-tools`, identical to Linux |
| The auto-mode environment text in `settings.json` names `/home/crazyhusir/Documents/projects/tools` as the trusted repo | wrong | unchanged |

A fully native Windows port (workspace on NTFS) would mean rewriting `bin/`, about 50 helper shell scripts and every
prompt, for no gain. Rejected.

The Claude **desktop app** on Windows is fine for chatting, for bootstrapping (section 2.2 can be done from it), and
for looking at artifacts. Whether its Code sessions can run inside WSL is **unknown** (not checked). If it offers a
WSL environment, it may be used with the folder above; prove it first with a Bash call: `uname -r` must contain
`microsoft-standard-WSL2`, and `flock --version` and `ls /proc/meminfo` must work. Otherwise use the CLI in WSL.

### 2.2 Windows host (the user, in an Administrator PowerShell)

1. Install WSL with the same Ubuntu release the laptop ran (Pop!_OS 24.04 is Ubuntu 24.04: Python 3.12.3, git 2.43,
   gh 2.45, docker.io 29.1.3, ffmpeg 6.1.1 all come from its archive):

   ```powershell
   wsl --install -d Ubuntu-24.04
   wsl --update
   wsl -l -v          # Ubuntu-24.04 must show VERSION 2
   ```

   On first start Ubuntu asks for a UNIX user name: **`crazyhusir`**. That single choice keeps every path, the Claude
   memory slug and the scripts unchanged (section 6). If another name was already chosen, create the user
   (`sudo adduser crazyhusir && sudo usermod -aG sudo crazyhusir`) and make it the default (`/etc/wsl.conf`, below).

2. Create `%UserProfile%\.wslconfig` (that is `C:\Users\<you>\.wslconfig`); why each line is there is in 7.2:

   ```ini
   [wsl2]
   memory=10GB
   swap=4GB
   # processors: leave unset (all 8 threads). The workflows size their agent pool from the CPU count (6 = 8 - 2).

   [experimental]
   autoMemoryReclaim=gradual
   sparseVhd=true
   ```

   Then `wsl --shutdown` and reopen Ubuntu. If `wsl` warns that a key is unknown or has moved out of
   `[experimental]`, follow its message (these two keys are documented as experimental; uncertain for newer WSL).

3. Power, lid and updates (the laptop lost two days on Oct 10 to 12 because closing the lid suspended it):
   - Settings > System > Power & battery: when plugged in, sleep **Never**; power mode **Balanced**.
   - Control Panel > Power Options > "Choose what closing the lid does": when plugged in, **Do nothing**.
   - Windows Update: set active hours or pause updates during long runs (a forced restart kills WSL and every
     workflow).
4. Intel DTT: in Device Manager > System devices, look for entries named "Intel(R) Dynamic Tuning Technology"
   (the exact names vary by driver version; uncertain). If they are missing, install Xiaomi's driver package for the
   Mi NoteBook Ultra, or the optional driver updates in Windows Update. Section 7.3 explains why this matters.
5. usbipd-win for the phone (section 5): `winget install --interactive --exact dorssel.usbipd-win`.
6. Optional: HWiNFO64 (sensors-only mode, CSV logging) to replace `bin/thermlog`, which cannot see the CPU from WSL.
7. Disk: the WSL disk image (`ext4.vhdx`) lives on `C:`. Keep about **100 GB** free: Android SDK about 5 to 9 GB, the
   Gradle cache grew to 42 GB on Linux, the workspace is 0.5 GB of git plus about 9 GB of optional raw data, and
   regenerated outputs come back as agents work.

### 2.3 Ubuntu base (inside WSL)

```bash
sudo apt update && sudo apt full-upgrade -y
sudo apt install -y git rsync zstd unzip jq curl tmux util-linux procps gh
cat /etc/wsl.conf      # expect [boot] systemd=true; if missing, add it (and [user] default=crazyhusir), then wsl --shutdown
```

`/etc/wsl.conf`, if it needs writing:

```ini
[boot]
systemd=true
[user]
default=crazyhusir
```

systemd is needed for Docker and udev (sections 4 and 5).

Keep everything on the Linux disk. **Never** put the repositories under `/mnt/c/...`, and never let Git for Windows
operate on them (it converts line endings and loses exec bits).

### 2.4 Claude Code inside WSL

```bash
curl -fsSL https://claude.ai/install.sh | bash     # the native installer; if that URL changed, see docs.claude.com,
                                                   # or: npm install -g @anthropic-ai/claude-code (after Node, 4.4)
tmux new -s overhaul                               # survives a closed terminal window (not a reboot)
cd ~/Documents/projects/tools && claude            # log in with the same claude.ai account; extra usage stays OFF
```

Always start it from the tools root: that directory decides the memory folder (3.8) and loads `tools/CLAUDE.md`.
Set the model to Opus 5.5 **before the first message** (3.9): switching models mid-chat rebuilds the cache of the
whole conversation, and that caused the Oct 15 usage spike.

---

## 3. Restore

### 3.1 Accounts (user, once)

```bash
gh auth login              # github.com, HTTPS, browser or device code, as tripathisarthak457
gh auth setup-git          # git then uses gh's token for github.com (needed for the PRIVATE periflow repo)
git config --global user.name  tripathisarthak457
git config --global user.email 98253995+tripathisarthak457@users.noreply.github.com
git config --global init.defaultBranch main
```

These are the values `~/.gitconfig` had on Linux. Never copy tokens from the old machine or into any file; `gh`
keeps its own.

### 3.2 Run the restore script

The script is `.overhaul/handover/tools/restore-on-wsl.sh` in the backup. Fetch it, read it, run it:

```bash
cd ~
# branches layout:
curl -fsSLO https://raw.githubusercontent.com/tripathisarthak457/android-base/overhaul-workspace/.overhaul/handover/tools/restore-on-wsl.sh
# one-branch layout (section 1) instead:
# curl -fsSLO https://raw.githubusercontent.com/tripathisarthak457/android-base/main/overhaul-workspace/.overhaul/handover/tools/restore-on-wsl.sh
less restore-on-wsl.sh
bash restore-on-wsl.sh 2>&1 | tee ~/restore.log
```

It needs only `git`, `rsync` and `sed`, plus the `gh` login for PeriFlow. It is safe to re-run: it never pushes,
deletes or overwrites, and it leaves alone any worktree that already has uncommitted changes. Every path can be
overridden by environment variables (`TOOLS`, `WS_DIR`, `PF_DIR`, `AB_URL`, `PF_URL`, `CLAUDE_DIR`, `PF_BASE`,
`SKIP_PERIFLOW=1`, `RECREATE_BRANCHES=1`); the defaults reproduce the Linux layout.

Expected end of the output (branches layout; SHAs as of Oct 16, 11:00, unless agents committed later):

```
  worktree      branch                      head     uncommitted
  WP01          wp/WP01                     d19f79c  0
  WP02          wp/WP02                     13eb1f1  0
  WP03          wp/WP03                     b595f9d  47
  WP03-skel     wp/WP03-skel                46ec781  0
  WP04          wp/WP04                     fb866b3  0
  WP04-perf     wp/WP04-perf                fb866b3  4
  WP05          wp/WP05                     5d515a0  0
  WP05-glass    wp/WP05-glass               282dd3c  43
  WP06          wp/WP06                     a8129c4  0
  WP07          wp/WP07                     99239af  0
  WP08          wp/WP08                     d05a682  0
  WP09          wp/WP09                     3d580cd  0
  WP10          wp/WP10                     d702b16  0
  WP11          overhaul/smooth-glass       45b91a2  2
  WP11-base     HEAD                        a5b630a  0
```

The sections below say what each step does and how to do it by hand.

### 3.3 The resulting layout

| Relative to the tools root | Linux path (Oct 16), also the recommended WSL path | What |
|---|---|---|
| `android-base/` | `/home/crazyhusir/Documents/projects/tools/android-base` | the repository; the main checkout is on `overhaul/multiplatform-v2` (branches layout) |
| `.overhaul/` | `/home/crazyhusir/Documents/projects/tools/.overhaul` | the workspace (not a git repository, as on Linux) |
| `.overhaul/wt/<name>` | `/home/crazyhusir/Documents/projects/tools/.overhaul/wt/<name>` | git worktrees: 13 of android-base, `WP11` and `WP11-base` of PeriFlow |
| `CLAUDE.md` | `/home/crazyhusir/Documents/projects/tools/CLAUDE.md` | from the backup's `tools-CLAUDE.md` (new on Windows) |
| `HANDOVER.md` | (new) | a symlink to `.overhaul/handover/HANDOVER.md` |
| `../tools-ws/` | `/home/crazyhusir/Documents/projects/tools-ws` (new) | a clean clone of the `overhaul-workspace` branch (branches layout only) |
| `../jorwen/periflow/` | `/home/crazyhusir/Documents/projects/jorwen/periflow` | PeriFlow, PRIVATE; the handoffs and `scripts/to-release.js` (`PFR`) expect it here |

From Windows Explorer the same folder is `\\wsl.localhost\Ubuntu-24.04\home\crazyhusir\Documents\projects\tools`.
Look, but do not edit files there with Windows tools.

### 3.4 The workspace: why a copy, not a checkout

By hand, for the branches layout:

```bash
cd ~/Documents/projects
git clone https://github.com/tripathisarthak457/android-base.git tools/android-base
git clone --single-branch -b overhaul-workspace --reference-if-able tools/android-base --dissociate \
    https://github.com/tripathisarthak457/android-base.git tools-ws
rsync -a --ignore-existing --exclude=.git tools-ws/.overhaul/ tools/.overhaul/
cp tools-ws/tools-CLAUDE.md tools/CLAUDE.md
ln -s .overhaul/handover/HANDOVER.md tools/HANDOVER.md
```

For the one-branch layout, the source is `tools/android-base/overhaul-workspace/` instead of `tools-ws/`.

Why the workspace is copied out of a separate clone rather than made a git checkout itself:

- On Linux `.overhaul` was **never** a git repository. Agents write generated projects into `out/` (with test
  keystores and `google-services.json` files), raw frames into `perf/`, and so on. If the tools folder were a
  checkout of a **public** branch, one `git add -A` would publish them. A plain folder keeps the old safety.
- `tools-ws` (outside the tools root) stays a pristine copy of the backup. `diff -rq tools-ws/.overhaul tools/.overhaul`
  shows what changed since, and it is the channel for a later backup: `rsync` with the same filter
  (`.overhaul/handover/backup-include.txt`) into `tools-ws/.overhaul/`, then a normal commit and push of that branch.
- `--reference-if-able ... --dissociate`: the first clone already downloaded every branch, including the 0.5 GB
  workspace branch; this reuses those objects instead of downloading them twice, then makes the clone independent.
- `--ignore-existing`: a second run never overwrites a note an agent has edited since.

Not in git, by design (MANIFEST.md sections 4 and 5): `wt/` (rebuilt from branches), generated projects and build
outputs (regenerable: each `out/*/generator-spec.json` plus `BUILT_FROM` says how), the Gmail reference recordings
(other people's e-mails; `research/predictive-back/SPEC.md` holds what they taught), PeriFlow and VitFleet source
copies, secrets.

### 3.5 The worktrees

The scripts expect a worktree per package at `.overhaul/wt/<name>` on branch `wp/<name>`. By hand, for each
`origin/wp/*` branch:

```bash
R=~/Documents/projects/tools/android-base; OV=~/Documents/projects/tools/.overhaul
git -C $R switch --no-track -c overhaul/multiplatform-v2 origin/overhaul/multiplatform-v2   # the main checkout
for b in $(git -C $R for-each-ref --format='%(refname:strip=3)' refs/remotes/origin/wp/); do
  git -C $R worktree add --no-track -b "$b" "$OV/wt/${b#wp/}" "origin/$b"
done
```

**Do this before launching any workflow.** An implementation prompt in `scripts/to-release.js` says: "create it if
missing with `git worktree add <wt> -b wp/<X> overhaul/multiplatform-v2`". On a fresh clone, where the package work
exists only as `origin/wp/<X>`, that would start a new, empty `wp/<X>` from the integration branch and silently
fork away from the real work.

`--no-track`: on Linux the package branches had no upstream. Without one, a bare `git push` in an agent's shell fails
instead of publishing; the scripts' rule is still "never push package branches".

`scratch/WP09-trial` comes back as plain files (its worktree pointer was left out; its commit `28cd271` is on no
branch). It is reference material only.

### 3.6 The work in progress (WIP)

Uncommitted work was saved as one snapshot commit per worktree: its parent is the branch head and its tree is the
working tree, untracked files included (ignored files excluded). The script writes it back with

```bash
git -C <worktree> restore --source=<snapshot> --worktree -- .
```

which brings modified and deleted tracked files back as **unstaged** changes and new files as **untracked** files:
the exact state of Oct 16. The package agents expect that ("Uncommitted changes are work in progress: review them,
keep what is right, commit"). Do not commit the snapshot itself onto the branch: its message says "not reviewed,
may not compile", and `wp/WP03`'s does not compile.

| Worktree | Snapshot (branches layout) | Entries in `git status --porcelain` (files with `-uall`) | What it is |
|---|---|---|---|
| `wt/WP03` | `backup/WP03-wip-2026-10-16` (name expected; the script reads the branch from the commit message) | **47** (132) | The whole SHEET-SPEC rev-2 bottom-sheet system, overlay host, catalog Sheets page, D10-D12, K3, about 20 sheet tests. **Does not compile**: `component/sheet/gesture/SheetFlingBehavior.kt:88` (`syncAnchors` unresolved); 3 of 43 sheet code rules fail. Read `handoff/WP03-RESUME.md` first. An older copy of the sheet work (Oct 15: `tracked.diff`, `untracked.tgz`) is in `.overhaul/backup/WP03-2026-10-15/`. |
| `wt/WP05-glass` | `backup/WP05-glass-wip-2026-10-16` | **43** (43) | The liquid-glass bottom bar of REQUIREMENTS 37: `GlassButtons.kt`, `LiquidTabs.kt`, `LiquidPress.kt`, `PressGlow.kt`, `RimLight.kt`, `GlassTier.kt` and 2 fixtures. `handoff/WP05-glass.md`. |
| `wt/WP04-perf` | `backup/WP04-perf-wip-2026-10-16` | **4** (4) | `feature/sample/.../detail/SampleDetail{Contract,Route,ViewModel}.kt` and `SampleDetailViewModelTest.kt`. Some notes say 5; at 11:12 on Oct 16 there were 4. `handoff/WP04-perf.md`. |
| `wt/WP11` (PeriFlow) | `backup/smooth-glass-wip-2026-10-16` on the PRIVATE periflow repo | **2** | `periflow-android/.../foundation/glass/LiquidLens.kt` and `LiquidLensTest.kt`. `handoff/WP11-RESUME.md`. |

Check after the restore:

```bash
for w in ~/Documents/projects/tools/.overhaul/wt/*/; do printf '%-12s %s\n' "$(basename $w)" "$(git -C $w status --porcelain | wc -l)"; done
```

If the script warned "the snapshot was taken on X, but the worktree is at Y", the branch moved after the snapshot
(someone committed on it). Re-apply only the snapshot's own changes, as a patch:

```bash
git -C <worktree> diff --binary <snapshot>^ <snapshot> > /tmp/wip.patch     # in the main repo: same objects
git -C <worktree> apply --3way /tmp/wip.patch                               # stages what it applies;
git -C <worktree> restore --staged .                                        # unstage, to match the old state
```

### 3.7 PeriFlow (PRIVATE; its code never goes to the public repo)

By hand:

```bash
PF=~/Documents/projects/jorwen/periflow; OV=~/Documents/projects/tools/.overhaul
git clone https://github.com/tripathisarthak457/periflow.git $PF          # needs gh auth setup-git
git -C $PF worktree add --no-track -b overhaul/smooth-glass $OV/wt/WP11 origin/overhaul/smooth-glass
git -C $OV/wt/WP11 restore --source=origin/backup/smooth-glass-wip-2026-10-16 --worktree -- .
git -C $PF worktree add --detach $OV/wt/WP11-base a5b630a                 # the "before" baseline WP11 measures against
```

`--no-track` matters here: `design/wp/WP11.md` says "the remote (no push, no upstream)".

**PeriFlow builds need files that are in no git backup**, by design. They are secrets, and the user restores them
from their own private storage (`WIPE-READINESS.md` section 4 lists them):

- `periflow-android/keystore.properties`, `periflow-android/keys/{dev,staging,prod,playstore}.jks`;
- `periflow-android/app/src/{dev,staging,prod,playstore}/google-services.json`;
- `periflow-android/local.properties` (only `sdk.dir=/home/crazyhusir/Android/Sdk`; recreate it rather than copy);
- three keys in `~/.gradle/gradle.properties`: `periflow.devApiUrl`, `periflow.googleWebClientId`,
  `periflow.prodApiUrl`.

They go into the main checkout `$PF/periflow-android/`, and are linked into the worktree as the "Setup" section of
`.overhaul/design/wp/WP11.md` shows (`ln -s`, never copied into git, never staged). On Linux,
`keystore.properties` and `local.properties` were symlinks and `keys/` and the `google-services.json` files were
copies. Never touch `periflow-recovery-codes.txt` or `PeriFlow-Postman/`. Without these files only WP11 is blocked;
nothing in android-base needs them.

### 3.8 Claude's memory

Claude Code keeps a memory folder per project at `~/.claude/projects/<slug>/memory/`. The slug is the absolute path
of the directory Claude Code was started in, with **every character that is not a letter or a digit replaced by
`-`**. Evidence from the Linux machine:

| Started in | Slug |
|---|---|
| `/home/crazyhusir/Documents/projects/tools` | `-home-crazyhusir-Documents-projects-tools` |
| `/home/crazyhusir/Documents/projects/tools/.overhaul` | `-home-crazyhusir-Documents-projects-tools--overhaul` (`/.` gives two dashes) |
| `/home/crazyhusir/.config/Claude/scratch-workspaces/...` | `-home-crazyhusir--config-Claude-scratch-workspaces-...` |

So in WSL with the recommended path the slug is unchanged. (For a native Windows start in `C:\Users\Sarthak\tools` it
would be `C--Users-Sarthak-tools`; not recommended, section 2.1.)

The memory files are in the backup under `claude-memory/` (branch `overhaul-workspace`, or `overhaul-workspace/claude-memory/`
on `main` in the one-branch layout): `MEMORY.md`, `android-base-generator.md`, `android-skills-installed.md`,
`laptop-memory-freeze.md`. By hand:

```bash
SLUG=$(printf '%s' ~/Documents/projects/tools | sed 's/[^A-Za-z0-9]/-/g')
mkdir -p ~/.claude/projects/$SLUG/memory
cp -n ~/Documents/projects/tools-ws/claude-memory/* ~/.claude/projects/$SLUG/memory/
```

Then, in Claude, `/memory` should list them. Two facts in them are now stale; update them after the restore:
"nothing pushed" and "PeriFlow ... never pushed" (both changed on Oct 16), and the laptop notes (Linux-specific).

Not restored on purpose:
- the chat transcripts (`*.jsonl`, 712 MB): they contain secrets (another app's keystore password, see `WIPE-READINESS.md`
  risk 7), so they are only in the offline backup (3.11). `CHAT-HISTORY.md` and `USER-MESSAGES.md` in `handover/`
  carry what the user said. Do not resume the old session e3f9060d either: its context is enormous and would burn the
  usage window.
- the memory of other projects (PeriFlow, VitFleet): not part of this backup.

### 3.9 Claude settings and Google's android-skills plugin

`~/.claude/settings.json` in WSL. The Linux file had these keys (no secrets); the `model` value was `sonnet` there,
but the user's rule since Oct 15, 20:55 is **Opus 5.5 only, subagents included**:

```json
{
  "model": "claude-opus-5-5",
  "modelSettings": { "claude-opus-5-5": { "effortLevel": "medium" } },
  "enabledPlugins": { "android-skills@android-skills": true },
  "extraKnownMarketplaces": {
    "android-skills": { "source": { "source": "github", "repo": "android/skills" } }
  },
  "skipWorkflowUsageWarning": true,
  "env": {
    "ANDROID_HOME": "/home/crazyhusir/Android/Sdk",
    "JAVA_HOME": "/usr/lib/jvm/java-21-openjdk-amd64"
  }
}
```

- `env` is new: on Linux the agents' Bash tool had `adb` on `PATH` but **not** `ANDROID_HOME` or `JAVA_HOME` (checked on
  Oct 16), and the handoffs work around it by prefixing commands with `ANDROID_HOME=$HOME/Android/Sdk`. Setting both
  here removes that trap. `JAVA_HOME` assumes the JDK of section 4.1.
- The Linux file also had `autoMode.environment`: 24 lines describing the environment for the auto-mode classifier
  (no secrets). Line 22 names `/home/crazyhusir/Documents/projects/tools` as the trusted repo, which stays right with
  the recommended path. The full text is in the offline backup's `02-claude-and-config` (3.11); without it, auto mode
  uses its defaults.

The plugin (installed on Linux: `android-skills@android-skills` **1.0.15**, git commit
`5bda39f154fb859a08d45e2392f3568e049f83fb`, tag `v1.0.15`, from the GitHub marketplace `android/skills`, user scope):

```bash
claude plugin marketplace add android/skills
claude plugin install android-skills@android-skills
cat ~/.claude/plugins/installed_plugins.json        # version and gitCommitSha
ls ~/.claude/plugins/marketplaces/android-skills/jetpack-compose/jetpack-compose-performance/SKILL.md
```

- The one skill path the active scripts hard-code is the `jetpack-compose-performance/SKILL.md` above; it must exist.
- A newer version than 1.0.15 is fine for reading skills. Note it in `STATUS.md`, because `scripts/android-skills-v1-0-15.js`
  vendors whatever is installed into generated projects.
- The marketplace clone is **shallow** (it was on Linux: one commit). `android-skills-v1-0-15.js` asks agents to run
  `git -C <marketplace> diff --stat 42dc227 5bda39f`, which fails on a shallow clone. Before launching that workflow:
  `git -C ~/.claude/plugins/marketplaces/android-skills fetch --unshallow`.
- Perfetto's `trace_processor` comes with this plugin: the android-profiler skill's wrapper
  `profilers/android-profiler/references/perfetto/bin/trace_processor` downloads its pinned binary (Perfetto
  **v57.2**, `da1d152cf`, on Linux) into `~/.local/share/perfetto/prebuilts/` on first use. The skill says not to
  install another one.
- Never copy `~/.claude/.credentials.json` or any token from the old machine.

### 3.10 Raw data from the draft release(s) (optional)

Bulky raw data went into zstd tar archives on a **draft** release, visible only to the owner (`MANIFEST.md` section 3
lists their contents). None of it is needed to resume work: it is evidence (traces, frames, recordings). Get the
traces and recordings first; the frames and the PeriFlow screens only when a task needs them.

| Archive | Raw size | Where (per MANIFEST's decision) |
|---|---:|---|
| `overhaul-raw-traces.tar.zst` | 1.28 GB (about 0.13 GB packed) | `tripathisarthak457/android-base` |
| `overhaul-recordings.tar.zst` | 0.18 GB | android-base |
| `overhaul-private-app-listings.tar.zst` | 0.02 GB | android-base |
| `overhaul-frames-1.tar.zst`, `overhaul-frames-2.tar.zst` | 1.64 + 1.33 GB | android-base |
| `overhaul-periflow-media-1..4.tar.zst` | 1.31 + 1.60 + 1.70 + 0.09 GB | **`tripathisarthak457/periflow`** (PRIVATE), same tag; android-base was the fallback |

```bash
TAG=overhaul-raw-data-2026-10-16; mkdir -p ~/raw && cd ~/raw
gh release view $TAG -R tripathisarthak457/android-base --json isDraft,assets -q '.isDraft, (.assets[] | "\(.name) \(.size)")'
gh release view $TAG -R tripathisarthak457/periflow     --json isDraft,assets -q '.isDraft, (.assets[] | "\(.name) \(.size)")'
gh release download $TAG -R tripathisarthak457/android-base -p 'overhaul-raw-traces.tar.zst' -p 'overhaul-recordings.tar.zst'
sha256sum *.tar.zst          # compare with the list in .overhaul/handover/PUSHED.md
cd ~/Documents/projects/tools/.overhaul
for f in ~/raw/*.tar.zst; do nice -n 19 tar -I zstd --skip-old-files -xf "$f"; done
```

- Archive member paths are relative to `.overhaul/` (checked: none longer than 72 characters).
- `--skip-old-files`: about 1,400 small text files are in both git and the archives; never overwrite the git copies.
- `gh` is expected to find a draft release by its tag; if it says "release not found", list the drafts with
  `gh api repos/tripathisarthak457/android-base/releases --jq '.[] | select(.draft) | .tag_name'` and download an
  asset by id with `gh api -H 'Accept: application/octet-stream' repos/<owner>/<repo>/releases/assets/<id> > <name>`.
- Unpacked PeriFlow screens are private: they must never be added to the public `overhaul-workspace` branch
  (`backup-include.txt` already excludes their folders).

### 3.11 The offline backup on Google Drive (second source)

A separate workflow (`scripts/local-offline-backup.js`) packed what GitHub does not hold into
`/home/crazyhusir/Documents/projects/tools/backup-2026-10-16/`. The user uploads that folder to Google Drive as
`backup-2026-10-16` and downloads it on Windows. Its own `README-RESTORE.md`, `MANIFEST.txt` and `SHA256SUMS.txt` are
authoritative; the plan (`plan-paths.txt`) had these zips:

| Zip | Holds (planned) |
|---|---|
| `00-android-base-not-on-github-p01..p13` | the **entire** `.overhaul` (including `wt/` with build outputs, `out/`, and the never-publish items such as the Gmail recordings), android-base's ignored files, a git bundle, and every Claude transcript and memory folder about android-base |
| `02-claude-and-config` | `~/.claude/settings.json`, plugin lists, memory folders of all projects, other config |
| `04-git-bundles` | `git bundle --all` of android-base, PeriFlow and every repo with unpushed work |
| `05`..`10` | ignored and uncommitted repo files, documents, downloads, media, unpushed projects |
| `01-secrets` | **held back, not packed** (the plan's header): keystores and `.env` files are the user's to store |

Use it, if needed, **after** the GitHub restore, from WSL:

```bash
B=/mnt/c/Users/<WindowsUser>/Downloads/backup-2026-10-16
cd $B && sha256sum -c SHA256SUMS.txt
unzip -n $B/02-claude-and-config.zip -d ~/offline-restore      # inspect first; paths inside start at the home folder
```

`-n` never overwrites. Unzip into a separate folder and copy what you need; do not unzip the `00-*` parts over
`~/Documents/projects/tools/.overhaul`: they contain material that must never reach the public branch, and their
`wt/*/.git` files point at worktree metadata the new clone does not have. The transcripts belong in a separate folder
for grep (they contain secrets), not in `~/.claude/projects`.

---

## 4. The toolchain, with versions read from the repository

Sources: `templates/android/gradle/wrapper/gradle-wrapper.properties`, `templates/android/gradle/libs.versions.toml`,
`templates/android/build-logic/.../AppConfig.kt`, `templates/android/gradle.properties`,
`templates/multiplatform/...` on `wp/WP07`, `web/api/go.mod`, `web/frontend/package.json`, `Dockerfile`,
`.github/workflows/ci.yml` and `AGENTS.md`, all on `overhaul/multiplatform-v2` (`a402d87`) unless named; and the
laptop's installed versions (Oct 16).

| Tool | Version to install | Where the number comes from | Laptop (Oct 16) |
|---|---|---|---|
| Gradle | **9.7.1** (wrapper downloads it; sha256 pinned) | wrapper, every branch, and PeriFlow | 9.7.1 |
| Android Gradle Plugin | **9.4.1** | `libs.versions.toml` (all branches; PeriFlow too) | |
| Kotlin | **2.4.20**; `wp/WP02` moves to **2.4.21** | `libs.versions.toml` | Kotlin/Native 2.4.20 in `~/.konan` (from WP07) |
| KSP | 2.3.12 | `libs.versions.toml` | |
| Compose BOM | 2026.09.00; Navigation 3 1.1.7 (1.2.0 on `wp/WP04`, `wp/WP04-perf`, `wp/WP05-glass`) | `libs.versions.toml` | |
| Compose Multiplatform | 1.12.1; JetBrains Navigation 3 1.1.2; Koin 4.2.2; Room 3 3.0.3 | `templates/multiplatform/gradle/libs.versions.toml` on `wp/WP07` | |
| Kyant backdrop / shapes | 2.0.1 / 1.2.1 | `scripts/full-liquid-glass-fable.js`, `handoff/WP05*.md` | sources in `~/.gradle/caches/modules-2/files-2.1/io.github.kyant0` after a WP05 build; copies already in `.overhaul/research/glass/kyant-src` |
| JDK running Gradle | **any JDK 17+** ("Do not pin or download a JDK", `AGENTS.md`); install **OpenJDK 21** | `AGENTS.md`; bytecode target is Java 17 (`AppConfig.kt`) | JetBrains Runtime **25.0.3** from Android Studio 2026.2.1 (the Gradle daemon log shows `javaVersion=25`); CI uses Temurin 17 |
| Android SDK platform | `compileSdk = 37`, `targetSdk = 37`, `minSdk = 26` (the benchmark module: 28) | `AppConfig.kt`, `benchmark/build.gradle.kts` | `platforms;android-37.0`, `android-37.1`, `android-36` |
| Build tools | 36.0.0 | not pinned in the build (AGP's default) | 36.0.0 |
| NDK, CMake | **not used** by the template (no `externalNativeBuild`, no `ndkVersion`) | grep of `templates/android` | NDK 30.0.16248370 and CMake 4.1.2 were installed (probably on demand by AGP or another project; install them to avoid surprise downloads) |
| platform-tools (adb) | latest | | 37.0.1 (adb 1.0.41) |
| cmdline-tools | latest | | 23.0 |
| Python | **3.12** (3.11+ is required by `tools/perf`, which uses `tomllib`) | `ci.yml`, `Dockerfile` (`python:3.12-alpine`) | 3.12.3. The generator is **standard library only** (`Dockerfile`: "No pip install"). Pillow is optional: without it, launcher icons are skipped and `test_icons.py` is skipped; it was not installed on Linux. |
| Node | **24.21.0**, npm 11.19.0 (`web/frontend` uses npm: `package-lock.json`) | laptop; `ci.yml` says Node 22 | 24.21.0 in `~/.local/node`; global packages `vercel`, `firebase-tools`, `newman`, `corepack` (none needed by the overhaul) |
| Next.js stack | next 15.5.25, react 19.1.1, typescript 5.9.2, tailwindcss 4.1.13 | `web/frontend/package.json` | |
| Go | **1.27.1** (`go.mod` says `go 1.27`) | `go.mod`, `Dockerfile` (`golang:1.27-alpine`); note that `ci.yml` still says `go-version: "1.24"` (a mismatch for WP13) | 1.27.1 in `/usr/local/go`; `staticcheck`, `golangci-lint`, `sqlc` in `~/go/bin` (WP09/WP13 use `staticcheck` and `govulncheck`) |
| Docker | 29.1.3 (Ubuntu's `docker.io`) | WP09's store tests run Postgres 17 in Docker; container smoke of the `Dockerfile` | 29.1.3 |
| ffmpeg / ffprobe | 6.1.1 (Ubuntu 24.04) | `templates/android/tools/perf/perf.py` on `wp/WP04` calls both | 6.1.1 |
| Perfetto trace_processor | v57.2, via the android-profiler skill (3.9) | `handoff/WP04-perf.md` | v57.2 |
| zstd, rsync, jq, gh, git | Ubuntu 24.04 packages | | zstd 1.5.5, gh 2.45.0, git 2.43.0 |
| Swift / Xcode | **not possible** on Windows or Linux: WP08 is checked statically; `xcodegen` and `xcodebuild` run only in CI on macOS (`design/wp/WP13.md`) | `handoff/WP08.md` | none |

### 4.1 Install commands (inside WSL)

```bash
# JDK, Python, media and helpers
sudo apt install -y openjdk-21-jdk-headless python3 python3-venv ffmpeg zstd rsync jq unzip \
     android-sdk-platform-tools-common docker.io
sudo usermod -aG docker,plugdev "$USER"      # then log out of WSL (wsl --shutdown) and back in
# (an exact match with the laptop would be JDK 25; check `apt-cache policy openjdk-25-jdk-headless`. 21 is enough.)

# Android SDK without Android Studio
mkdir -p ~/Android/Sdk/cmdline-tools && cd ~/Android/Sdk/cmdline-tools
# download "Command line tools only" for Linux from https://developer.android.com/studio (commandlinetools-linux-<build>_latest.zip)
unzip ~/commandlinetools-linux-*_latest.zip && mv cmdline-tools latest
export ANDROID_HOME=$HOME/Android/Sdk PATH=$PATH:$HOME/Android/Sdk/cmdline-tools/latest/bin:$HOME/Android/Sdk/platform-tools
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "platforms;android-37.1" "platforms;android-36" \
           "build-tools;36.0.0" "ndk;30.0.16248370" "cmake;4.1.2"
# emulator, only if needed (section 5.4): sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"

# Node 24.21.0, as on the laptop (~/.local/node on PATH)
mkdir -p ~/.local/share/node && cd ~/.local/share/node
curl -fsSLO https://nodejs.org/dist/v24.21.0/node-v24.21.0-linux-x64.tar.xz && tar -xJf node-v24.21.0-linux-x64.tar.xz
ln -sfn ~/.local/share/node/node-v24.21.0-linux-x64 ~/.local/node

# Go 1.27.1 in /usr/local/go, as on the laptop
cd /tmp && curl -fsSLO https://go.dev/dl/go1.27.1.linux-amd64.tar.gz
sudo rm -rf /usr/local/go && sudo tar -C /usr/local -xzf go1.27.1.linux-amd64.tar.gz
/usr/local/go/bin/go install honnef.co/go/tools/cmd/staticcheck@latest
/usr/local/go/bin/go install golang.org/x/vuln/cmd/govulncheck@latest
```

Environment, appended to **both** `~/.profile` and `~/.bashrc` (Ubuntu's `.bashrc` returns early for non-interactive
shells, so put the block **above** its `case $- in` guard, or source it from `~/.profile` too):

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
export PATH="$HOME/.local/node/bin:$JAVA_HOME/bin:$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:/usr/local/go/bin:$HOME/go/bin:$HOME/.local/bin"
```

Plus the `env` block of 3.9 for Claude Code itself.

Check:

```bash
java -version; sdkmanager --list_installed | head -20; adb version; python3 --version
node -v; npm -v; go version; ffmpeg -version | head -1; docker run --rm hello-world
```

### 4.2 The first build (by Claude, once everything above passes)

The Gradle cache starts empty (it was 42 GB on Linux), so the first build downloads several GB. Do one warm-up build
alone, through the lock, before any workflow, and watch the temperature on Windows (7.3):

```bash
cd ~/Documents/projects/tools/android-base/templates/android
../../../.overhaul/bin/locked gradle ./gradlew assembleDevDebug
cd ~/Documents/projects/tools/android-base/generator && python3 -m unittest discover -s tests -t .
```

`templates/android/gradle.properties` sets `org.gradle.jvmargs=-Xmx4g` and `kotlin.daemon.jvmargs=-Xmx2g`. Never raise
them (REQUIREMENTS "Memory safety"), and keep every build on the same JVM arguments so all agents share one daemon.

---

## 5. The phone: Redmi Note 10 Pro, serial `9a7c74f4`, 120 Hz

### 5.1 Why USB through usbipd, not adb over Wi-Fi

The scripts and helper scripts address the phone as `adb -s 9a7c74f4` and through `bin/locked adb`. Over Wi-Fi the
serial becomes an `ip:port` or mDNS name, and every hard-coded `-s 9a7c74f4` breaks. With usbipd-win the USB device
is handed to WSL, and adb inside WSL sees the same serial as on Linux.

### 5.2 Steps

On the phone: Developer options > USB debugging on (it was on; it runs a custom ROM at SDK 37). Then, in an
Administrator PowerShell (the user):

```powershell
usbipd list                                   # find the phone's BUSID; plug and unplug to be sure which it is
usbipd bind --busid <BUSID>                   # once per device and port (admin)
usbipd attach --wsl --busid <BUSID> --auto-attach   # keep this window open: it re-attaches after replugs
```

Which USB IDs to look for is **uncertain**: the custom ROM may report Google's vendor id `18d1` rather than Xiaomi's
`2717`. On Oct 16 the Linux `lsusb` showed a device `18d1:4e11 Google Inc.`; whether that was the Redmi was not
checked (no phone work was allowed during the backup).

In WSL:

```bash
lsusb                                            # the phone must be listed (package usbutils)
ls /lib/udev/rules.d/51-android.rules            # from android-sdk-platform-tools-common, as on the laptop
groups | grep plugdev                            # needed for non-root adb
~/Documents/projects/tools/.overhaul/bin/locked adb adb devices   # expect: 9a7c74f4  device
```

The first connection shows the RSA prompt on the phone: "Always allow from this computer". The laptop's adb key was
not carried over (`WIPE-READINESS.md` 2.1), so the prompt is expected.

### 5.3 Pitfalls

- While the phone is attached to WSL, Windows cannot see it. Do not run Android Studio's adb on Windows at the same time.
- After a replug: if `adb devices` shows nothing although `lsusb` lists the phone, run
  `bin/locked adb adb kill-server` and then `bin/locked adb adb start-server` (the Oct 13 fix). `bin/locked` uses
  `flock -o` for adb, so an adb server it starts no longer holds the lock (the Oct 16 deadlock fix).
- `screenrecord` needs `--size 720x1600` on this phone. Judge smoothness only on release or benchmark builds.
- Phone steps in the workflows poll for the phone for up to 45 minutes, then record what remains.

### 5.4 The emulator (optional)

The fallback when the phone is missing is the AVD `periflow_api35` (device `pixel_8`, 1080x2400, 2 GB RAM, image
`system-images;android-35;google_apis;x86_64`); WP04 also used `wp04_fold_api35` (`pixel_9_pro_fold`, 2076x2152). The
AVDs were not backed up. In WSL2 the emulator needs `/dev/kvm` (nested virtualization; check `ls -l /dev/kvm`) and
runs headless with `-no-window -gpu swiftshader_indirect`. It counts against the 10 GB WSL memory, and it must only run
through `bin/locked emu`, which holds the Gradle lock too. Whether it performs acceptably in WSL2 on this laptop is
**untested**.

```bash
avdmanager create avd -n periflow_api35 -k "system-images;android-35;google_apis;x86_64" -d pixel_8
```

---

## 6. Paths

### 6.1 With the recommended setup, nothing changes

Every hard-coded path begins with `/home/crazyhusir/`. With the Linux user `crazyhusir` and the tools root at
`~/Documents/projects/tools`, they all resolve. The inventory below is for the case where that is not possible.

| Where | Files with `/home/crazyhusir/` | What they hold |
|---|---:|---|
| `.overhaul/scripts/` | 32 (all `*.js`, `*.pre-*` backups, `AUTOPILOT-PROMPT.md`) | the constants `R`, `OV`, `PFR`, `SKILLS`/`SK`, `WT`, `KY`, `L` |
| `.overhaul/bin/` | 6 | `dir=` in `locked` (and its backups), `out=` in `thermlog`; `memwait` has none |
| `.overhaul/handoff/` | 7 | `OV=...` setup lines that agents copy |
| `.overhaul/design/` | 16 | setup blocks in `design/wp/*.md` and `MASTER-PLAN.md` |
| top level | `REQUIREMENTS.md`, `STATUS.md`, `peek.py` | |
| `.overhaul/research/` | 2 `.md` | |
| helper scripts (`*.sh`, `*.py`) | 66: `logs/WP11` 28, `scratch/device` 24, `scratch-gen` 7, `qa/*` 5, `perf/WP03` 1, `verify-gen` 1 | old one-off measurement and generation scripts |
| logs and results (`*.log`, `*.txt`, `*.json`, reports) | thousands | historical evidence: **never rewrite** |
| `android-base` (every branch) | **0** | the repository itself is clean (`git grep` on every branch) |
| outside the workspace | `~/.claude/settings.json` (auto-mode text), `periflow-android/local.properties` (`sdk.dir`) | |

Six handoffs (`WP02-M3`, `WP03-skel`, `WP04`, `WP06`, `WP07-A`, `WP08`), `STATUS.md` and three scripts
(`handover-backup.js`, `handover-backup.private.js`, `merge-all-to-main.js`) also contain the memory **slug**
`-home-crazyhusir-` (journal and scratchpad paths).

Commands use `$HOME/Android/Sdk` and `~/Documents/...` in many places; those need no change.

### 6.2 If the Linux user name or home differs: the tested recipe

Keep the layout under the home folder (`~/Documents/projects/tools`, `~/Documents/projects/jorwen/periflow`) and
change only the home prefix and the slug:

```bash
OLD_HOME=/home/crazyhusir
NEW_HOME=$HOME                                             # for example /home/sarthak
OLD_SLUG=-home-crazyhusir-
NEW_SLUG=$(printf '%s' "$NEW_HOME" | sed 's/[^A-Za-z0-9]/-/g')-
cd ~/Documents/projects/tools/.overhaul

# Tier 1: instructions and runnable code. handover/ is left alone on purpose: it documents the old paths.
{ grep -rlIZ -e "$OLD_HOME" -e "$OLD_SLUG" scripts bin handoff design notes decisions ./*.md peek.py 2>/dev/null
  grep -rlIZ -e "$OLD_HOME" --include='*.md' research; } \
| xargs -0 -r sed -i.pre-path \
    -e "s#$OLD_HOME/#$NEW_HOME/#g" \
    -e "s#$OLD_HOME\([^A-Za-z0-9_.-]\|\$\)#$NEW_HOME\1#g" \
    -e "s#$OLD_SLUG#$NEW_SLUG#g"

# Check: nothing left, and every workflow script still compiles
grep -rIl --exclude='*.pre-path' -e "$OLD_HOME" -e "$OLD_SLUG" scripts bin handoff design notes decisions ./*.md peek.py
grep -rIl --exclude='*.pre-path' -e "$OLD_HOME" --include='*.md' research
node -e 'const fs=require("fs"),AF=Object.getPrototypeOf(async function(){}).constructor;let bad=0;
  for (const f of process.argv.slice(1)) { try { new AF("agent","parallel","phase","log",
    fs.readFileSync(f,"utf8").replace(/^export const meta/m,"const meta")) } catch (e) { bad++; console.log(f, e.message) } }
  console.log(bad ? bad + " failed" : "all compile")' scripts/*.js

# Tier 2, only for helper scripts you will run again (never logs or results):
grep -rlIZ -e "$OLD_HOME" --include='*.sh' --include='*.py' scratch scratch-gen logs qa perf verify-gen research \
| xargs -0 -r sed -i.pre-path -e "s#$OLD_HOME/#$NEW_HOME/#g"
```

Why it is safe:
- `#` as the delimiter (paths contain `/`); the first rule needs the trailing `/`, so `/home/crazyhusir2` would not
  match; the second catches the bare home at a word end (one script has `HOME = '/home/crazyhusir'`).
- `-i.pre-path` keeps the original next to each changed file. Delete the backups only after the first workflow ran:
  `find . -name '*.pre-path' -delete`.
- `-e` in front of every pattern, because the slug starts with `-`, which grep would take as an option.
- Tested on Oct 16 on a copy (section 10): 69 files changed, 167 occurrences replaced, 0 left, all 23 workflow scripts
  still compile, and no file changed anywhere except in the path text.

Also update by hand: the trusted-repo line in `~/.claude/settings.json`'s `autoMode.environment`;
`periflow-android/local.properties` (`sdk.dir`); and the restore script's defaults if you run it again (`TOOLS=...`).

If the tools root itself moves (not just the home folder), add a second rule
`s#$NEW_HOME/Documents/projects/tools#<new tools root>#g`, and the same for PeriFlow; the memory slug then changes too
(3.8).

---

## 7. Heat and memory on the new OS

### 7.1 Keep `bin/locked` and `bin/memwait`

They stay the only way to run Gradle (`bin/locked gradle ...`), adb (`bin/locked adb ...`) and the emulator
(`bin/locked emu ...`): one build machine-wide, two Gradle workers, `nice -n 10`, a 15-minute daemon idle timeout,
memory checks before a build (3.5 GB available for a cold start, 1.5 GB with a daemon resident).

What changes in WSL2 (tested on Oct 16 by hiding the thermal zones from the scripts):
- WSL2 is not expected to expose the host's CPU sensors, so `memwait`'s heat gate (wait until `x86_pkg_temp` is
  under 80 °C) cannot work there. Check with `ls /sys/class/thermal`.
- The **original** `memwait` does not break: with no thermal zone it prints three errors on every call
  (`cat: ... No such file or directory`, `syntax error: operand expected`), bash abandons the heat loop, and the
  memory wait still runs. Every `locked gradle` still works.
- `.overhaul/handover/tools/memwait-wsl` is a drop-in replacement that skips the heat wait quietly when the zone is
  missing and is otherwise identical. Install it once:
  `cp .overhaul/bin/memwait .overhaul/bin/memwait.pre-wsl && cp .overhaul/handover/tools/memwait-wsl .overhaul/bin/memwait && chmod +x .overhaul/bin/memwait`.
- `bin/thermlog` reads `/sys/class/thermal` and `/sys/class/power_supply/BAT0`; it is useless in WSL. Use HWiNFO64's
  CSV logging on Windows instead (CPU package temperature, package power, battery charging state).
- `bin/locked` itself needs only `flock`, `nice` and bash: unchanged.

### 7.2 WSL2 memory limits: why `.wslconfig` says 10 GB

- WSL2 runs a VM. By default it may take **half** the RAM (about 7.7 GB of 15.4). One build alone needs a 4 GB Gradle
  heap plus up to 1 GB of metaspace, a 2 GB Kotlin daemon and native memory: about 7 to 8 GB at peak, plus Claude
  Code (about 0.4 GB with all agents; they run inside the one process) and `tsc`/`next build`. At 7.7 GB, `memwait`
  would hold builds back for long stretches.
- **10 GB** fits one build plus everything else, and leaves about 5 GB to Windows, the browser and the desktop app.
  `memwait`'s thresholds keep their meaning inside the VM, because `/proc/meminfo` is the VM's.
- The cap is also the protection the Linux laptop lacked. On Oct 10 and on Oct 16, 10:32 the machine froze in zram swap
  thrash (swappiness 180, no OOM daemon). In WSL the worst case is the Linux OOM killer ending a process inside the VM
  (usually the Gradle daemon), while Windows stays responsive. `swap=4GB` gives a cushion before that.
- `autoMemoryReclaim=gradual` hands the VM's page cache back to Windows when idle; `sparseVhd=true` lets the disk
  image shrink.
- Optional inside WSL: `sudo apt install earlyoom` (the fix suggested on Linux), so a runaway build is killed before
  the VM swaps hard.
- Keep the lessons: after any resume from sleep, check `free -m` before launching; launch workflows two at a time;
  never start the emulator during builds; never raise the Gradle heap.

### 7.3 Heat: why Windows should stop the power-offs, and what to watch

- On Oct 16 the laptop powered off three times. The most likely cause is CPU heat under Gradle builds: the CPU sat at
  95 to 100 °C and `acpitz` at 97 °C against a 99 °C hot trip, and the PL1 power limit read **200 W** from the MSR,
  that is, effectively unlimited. On this Xiaomi, power limits and skin temperature are managed by Intel's Dynamic
  Tuning Technology (DTT) driver, which exists for Windows only; Linux ran without it. With DTT, Windows should hold
  the CPU at its OEM limits. This is a strong expectation, not a tested fact.
- The ACPI sensor `SEN3` also read erratically (60 to 125 °C within seconds, firmware critical trip 80 °C), and the
  battery was charging over USB-C at each power-off (`SESSION-2026-10-16.md`). If Windows also powers off under load,
  suspect the hardware: try another charger and cable, update the BIOS, and have the sensor and charging board checked.
- Keep `bin/locked`'s two workers and `nice -n 10`. During the first warm-up build (4.2), watch HWiNFO: if the CPU
  package sits at 95 °C or more for minutes, use Windows power mode "Best power efficiency" while agents build.
- Work on a hard, flat surface (the Oct 16 advice).

---

## 8. Resume the work

### 8.1 Preflight (Claude, in the WSL session started in the tools root)

1. Read, in order: `tools/CLAUDE.md`, `.overhaul/handover/HANDOVER.md`, `.overhaul/STATUS.md`,
   `.overhaul/SESSION-2026-10-16.md`, `.overhaul/SESSION-2026-10-15.md` (its Oct 16 sections),
   `.overhaul/handover/PACKAGES-AND-BRANCHES.md`, `.overhaul/handover/PUSHED.md`.
2. Machine: `uname -r` (contains `microsoft-standard-WSL2`), `nproc` (8), `free -m` (about 10 GB total, most
   available), `df -h ~` (room left).
3. Repository: `git -C android-base status -sb` (on `overhaul/multiplatform-v2`, clean, `a402d87` unless something
   merged later); `git -C android-base tag -l 'merged/*'` (WP01, WP02-M1, WP09, WP10).
4. Worktrees and WIP: the table of 3.2 and the counts of 3.6.
5. Tools: `.overhaul/bin/locked gradle true` returns at once; `.overhaul/bin/locked adb adb devices` lists
   `9a7c74f4 device`; the warm-up build of 4.2 is done.
6. Claude: `/model` shows Opus 5.5; `/memory` lists the four memory files; the plugin's
   `jetpack-compose-performance/SKILL.md` exists; the marketplace clone is unshallowed (3.9).
7. Usage: start the relaunch at the beginning of a fresh 5-hour window (the desktop app's usage tool, or `/usage` in
   the CLI; which one exists depends on the client, so check). Extra usage stays off.
8. The pre-fired list in `scripts/to-release.js` (line 58:
   `['WP01', 'WP02-M1', 'WP09', 'WP10', 'CP1', 'WP03:phaseA', 'WP04:phase2']`) must match the `merged/*` tags. Add any
   package merged since.

### 8.2 Relaunch the workflows: fresh, two at a time

Run ids from Linux (`wf_0f44e8ee-3dc` and the others in `STATUS.md` section 1) do **not** carry over: a new session
cannot resume them. Launch each script fresh with the Workflow tool and its path, for example
`Workflow({scriptPath: '/home/crazyhusir/Documents/projects/tools/.overhaul/scripts/to-release.js'})`, with no
`resumeFromRunId`. A fresh run has no cache, so every agent starts live, reads its handoff and git state, and continues
from there. That costs usage at the start, which is one more reason to launch two at a time.

| Wave | Script | Why at this point |
|---|---|---|
| 1 | `scripts/to-release.js` | The backbone: every remaining package with dependency waits, the review loops, the serialized merges into `overhaul/multiplatform-v2`, the CP2 decision, and the Android release gate. The lens fix (REQUIREMENTS 35, the user's priority) runs first inside it. |
| 1 | `scripts/deep-smoothness-fable.js` | Smoothness is the first Android release blocker (REQUIREMENTS 19 to 22, 36). It works on `wp/WP04-perf`, which feeds WP04, the first merge in `ORDER`. Despite the name, every agent now runs on Opus 5.5. |
| 2 (after 20 to 30 minutes, if `free -m` and the usage window allow) | `scripts/shimmer-fix-from-report.js` | Nearly done: review 3 approved and WP03 merged the fix; what remains is the phone verification. |
| 2 | `scripts/android-skills-v1-0-15.js` | REQUIREMENTS 38: skills digest, vendoring on `wp/WP02`, and the template-wide jetpack-compose-performance audit (`research/COMPOSE-PERF-AUDIT.md`). Mostly reading and writing, little building. Unshallow the marketplace clone first (3.9). |
| 3 (when a wave-2 run ends) | `scripts/full-liquid-glass-fable.js` | REQUIREMENTS 37, on `wp/WP05-glass`. The heaviest phone and GPU work, and its final integration waits for WP03 and WP04 to merge anyway. |

Rules while they run (all from the Linux days):
- Record every new task id and run id in `STATUS.md` section 1, and a line in `PROGRESS.md`.
- After a usage cutoff in the **same** session, resume with `Workflow({scriptPath, resumeFromRunId})`. Do not change
  any agent's options (in particular, never add a `model` key) on a run you intend to resume: it changes the cache
  keys and reruns finished agents live (the Oct 15, 20:30 spike).
- Re-create the autopilot with `CronCreate` from `scripts/AUTOPILOT-PROMPT.md` (run `CronList` first, so there is only
  one). It is session-only. Its step 3 mentions `get_usage`, a desktop-app tool; in the CLI, check the usage window
  another way.
- If the user reports a bug they saw, it comes first: pause the pipeline workflows (they resume from cache), fix it
  alone, then resume.
- The auto-mode classifier blocks killing processes and autonomous deploys. Leave stale processes alone; ask the user
  for anything that deploys.

### 8.3 The release, unchanged

The release model is Android first (REQUIREMENTS 36). `to-release.js` ends with a read-only release gate that writes
`.overhaul/RELEASE-GATE-android.md`. Only after that says GO, and **only with the user's explicit go-ahead in chat**,
are the merge to `main`, the push and the deploy done. Compose Multiplatform and Swift follow later (WP12-B, WP13-B).
The ETA before the crashes was "the Android release gate about Oct 17"; it moves by the days lost to the wipe.

### 8.4 What to ask the user for, once, in one message

Claude decides and records everything else ("Don't ask questions; decide and record"), but these need the user's hands:
1. The Windows admin steps of 2.2 (WSL, `.wslconfig`, power and lid, DTT check, usbipd install), if not yet done.
2. Plug in the phone, run the `usbipd bind` and `attach` commands, and accept the RSA prompt on the phone.
3. `gh auth login` and the Claude login, if not yet done.
4. PeriFlow's secret files (3.7), only when WP11 needs a PeriFlow build.
5. Whether the offline backup (3.11) is downloaded, if a task needs something from it.

---

## 9. Open points and uncertainties

| Point | Status | Where to look |
|---|---|---|
| The exact names of the `backup/*-wip-2026-10-16` branches | decided by the pack step after this guide was written; the script reads the branch from each snapshot's message, so any name works | `handover/PUSHED.md` |
| Whether the one-branch merge ran, and whether it finished | it was confirmed by the user at about 11:30 on Oct 16 | `git ls-remote` (section 1); `MERGE-ALL-*.md` |
| Whether the PeriFlow media archives are on the periflow repo's draft release or on android-base's | MANIFEST decided "periflow first, android-base as fallback" | `PUSHED.md`; `gh release view` on both |
| Claude desktop app with WSL | unknown | test as in 2.1 |
| `/sys/class/thermal` in WSL2 | expected absent; both `memwait` versions handle it | `ls /sys/class/thermal` |
| The phone's USB vendor id under usbipd | `18d1` or `2717` | `usbipd list` |
| `autoMemoryReclaim` and `sparseVhd` section in `.wslconfig` | documented as `[experimental]`; may move | `wsl` warns at start |
| Whether DTT ends the power-offs | expected, not proven | HWiNFO during the warm-up build |
| JDK 21 versus the laptop's JBR 25 | both satisfy "any JDK 17+"; no difference expected | `AGENTS.md` |
| `ci.yml` Go 1.24 and Node 22 versus `go.mod` 1.27 and the laptop's Node 24 | a mismatch to settle in WP13 | `.github/workflows/ci.yml` |

---

## 10. How this guide was tested (Oct 16, on the Linux laptop, no builds and no phone)

All tests ran in the session scratchpad at `nice -n 19`; nothing in the real repositories or worktrees was written.

1. **The restore script, branches layout.** A bare mirror of the local android-base, with GitHub's shape (`main` as
   HEAD, no remote-tracking refs), received WIP snapshot branches built exactly the way the pack step builds them
   (temporary index, `git add -A`, `commit-tree` on HEAD). The snapshots were made from a scratch clone into which the
   **real** worktrees' uncommitted state had been copied read-only (`git diff --binary HEAD` plus a tar of the
   untracked files); `git status` of that copy matched the real worktrees exactly first. A simulated `overhaul-workspace`
   branch (the real scripts, bin, handoffs, memory) and a stand-in PeriFlow repository (placeholder files only; no
   PeriFlow code was copied) completed the setup. Results:
   - all 13 package worktrees on the right heads, no upstreams, the main checkout on `overhaul/multiplatform-v2`, the
     four `merged/*` tags present;
   - WP03 47 entries (132 files), WP05-glass 43, WP04-perf 4: `git status --porcelain -uall` identical to the real
     worktrees, and **every one of the 179 paths byte-identical** to the real files;
   - PeriFlow `wt/WP11` with its 2 uncommitted files and no upstream, `wt/WP11-base` detached at the base commit;
   - `CLAUDE.md` from `tools-CLAUDE.md`, the `HANDOVER.md` symlink, the memory in the right slug folder;
   - a second run changed nothing and printed no warning.
2. **The restore script, one-branch layout**, built the way `merge-all-to-main.js` describes it (`archive/<branch>-2026-10-16`
   tags, the workspace merged as a subtree under `overhaul-workspace/` on `main`): detected; restored workspace and
   memory; without `RECREATE_BRANCHES` it creates no branches and says why; with `RECREATE_BRANCHES=1` it rebuilt all
   13 worktrees, the local `overhaul/multiplatform-v2`, PeriFlow's `wt/WP11`, and the three WIP sets byte-identical
   again.
3. **A remote with neither layout** stops with exit code 2 and a message.
4. **`memwait`**: the original with the thermal zones hidden (as in WSL2) prints the errors and returns 0 after the
   memory check; `memwait-wsl` returns 0 quietly with the zones hidden, returns 0 with real zones under a high limit,
   and waits ("CPU at 95 C, waiting...") with real zones under a 1 °C limit. `bin/locked gradle` and `bin/locked adb`
   ran with `memwait-wsl` in a scratch copy.
5. **The path recipe** of 6.2 on a copy of the tier-1 files with `NEW_HOME=/home/sarthak`: 167 occurrences in 69 files
   replaced, none left; the 23 workflow scripts compile as async function bodies before and after; after normalizing
   the paths, every changed file is identical to its original.

Not testable here: anything on Windows (WSL install, `.wslconfig`, usbipd, DTT), `gh` against the real draft
releases (they did not exist yet at the time of writing), Claude Code's plugin commands on a new install, and builds.
