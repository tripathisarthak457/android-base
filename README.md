# android-base overhaul: workspace backup (2026-10-16)

This is the orphan branch `overhaul-workspace` of `github.com/tripathisarthak457/android-base`. It is **not** part of
the generator's source history and shares no commits with `main`. It holds the working notes of the multi-day
android-base overhaul (started Oct 8, 2026), backed up on Oct 16, 2026 before the user's Linux laptop was wiped for
Windows, and from then on refreshed regularly as the off-machine backup of the workspace.

(The source of this file is `.overhaul/handover/BRANCH-README.md`; `.overhaul/handover/tools/sync-workspace-branch.sh`
copies it here.)

## What is here

| Path | What |
|---|---|
| `HANDOVER.md` | **Start here.** The complete briefing: the user, the project, the 39 requirements, the plan, where every package stopped, the orchestration, the rules, the lessons, and the first 30 minutes on Windows. Its §0 box has the backup status at a glance. |
| `CLAUDE.md` | Short instructions for Claude when this branch is checked out. |
| `tools-CLAUDE.md` | The `CLAUDE.md` for the parent `tools/` folder (it holds `android-base/` and `.overhaul/` side by side). Rename it to `CLAUDE.md` there. |
| `.overhaul/` | The workspace itself: `REQUIREMENTS.md`, `STATUS.md`, `SESSION-*.md`, `PROGRESS.md`, `design/`, `handoff/`, `decisions/`, `research/`, `qa/`, `perf/`, `audit/`, `scripts/` (the Workflow scripts), `bin/` (`locked`, `memwait`, `thermlog`), `out/` (logs and specs, no builds) and `handover/` (all handover documents). |
| `.overhaul/handover/RESUME-ON-WINDOWS.md` | How to set the machine up again (WSL2 Ubuntu) and restore everything. |
| `.overhaul/handover/PUSHED.md` | What the backup actually pushed: every ref with its SHA, the WIP branches, the draft-release status, what was left out on purpose, and what each fix round changed. |
| `.overhaul/handover/MANIFEST.md` | Which workspace files are here, which were meant for the draft release, and which were left out, with the reasons. |
| `claude-memory/` | Claude's memory files for this project, refreshed for Windows. Restore them into `~/.claude/projects/<project-slug>/memory/`. |

## How to restore

1. Read `HANDOVER.md` (§0 and §8 first if short on time).
2. Follow `.overhaul/handover/RESUME-ON-WINDOWS.md`. Its restore script is `.overhaul/handover/tools/restore-on-wsl.sh`.
   It clones android-base, copies this branch's `.overhaul/` next to it, recreates the package worktrees and puts
   the uncommitted work back from the `backup/*-wip-2026-10-16` branches.
3. Check `.overhaul/handover/PUSHED.md` for the exact branch names and SHAs.

## What is not here, on purpose

- **Source code.** It is on the android-base branches (`overhaul/multiplatform-v2`, `wp/*`, `backup/*`).
- **Raw traces, recordings and frame dumps** (about 9.1 GB raw). Too large for git. They were planned for the
  owner-only draft release `overhaul-raw-data-2026-10-16` (`.overhaul/handover/release-assets.txt`); at 12:45 on
  Oct 16 that release did not exist yet (`PUSHED.md` §3). In every case they are in the user's private Google Drive
  backup `backup-2026-10-16` (the `00-android-base-not-on-github-p*.zip` parts).
- **PeriFlow's code.** It lives only in its own private repository, `JorwenLabs/periflow` (formerly
  `tripathisarthak457/periflow`).
- **Secrets, chat transcripts and other people's personal data.** Never published. A few local dev-only values
  (a throwaway local Postgres password and a local API token) are replaced with `<local-dev-value>` in
  `.overhaul/handoff/WP09.md`, `.overhaul/out/WP10/run-api.sh` and `.overhaul/handover/MANIFEST.md`.
- **Build outputs** (APKs, `build/` folders, generated projects). They can be regenerated: each generated project's
  `generator-spec.json`, `BUILT_FROM` and `BUILT_DIRTY` are kept under `.overhaul/out/`.

## Rules that still hold

Never push or merge into `main` without the Android release gate and the user's go-ahead (production deploys from
`main`). The Oct 16 request to merge everything into `main` **did not run** and is pending the user's decision: do not
run `.overhaul/scripts/merge-all-to-main.js` without asking him (`HANDOVER.md` §1.7 item 0). Never force-push or
change the default branch. Never commit secrets. PeriFlow code goes only to its private repository. The full list is
in `HANDOVER.md` §6.
