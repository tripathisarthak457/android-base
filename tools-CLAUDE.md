# tools/: the android-base overhaul workspace root

Rename this file to `CLAUDE.md` and put it in the `tools/` folder that holds these side by side:

| Path | What |
|---|---|
| `android-base/` | git clone of the user's public generator repo `github.com/tripathisarthak457/android-base`; the orchestrator's checkout stays on the integration branch `overhaul/multiplatform-v2` |
| `.overhaul/` | the overhaul workspace (not part of android-base's git history; restored from the backup branch `overhaul-workspace`): requirements, plan, handoffs, scripts, `bin/`, research, QA and perf data |
| `.overhaul/wt/<WP>/` | git worktrees, one per work package (`wp/<WP>` branches) |
| `../jorwen/periflow/` | the user's private app PeriFlow (private repo `tripathisarthak457/periflow`), if restored |

On the old Linux laptop this root was `/home/crazyhusir/Documents/projects/tools/`; many scripts and notes hard-code
that path (HANDOVER.md §5.13).

**Read `.overhaul/handover/HANDOVER.md` first.** It is the complete briefing: the user, the project, the 38
requirements, the plan and where every package stopped, the orchestration, the rules, the lessons, and the first 30
minutes on Windows. Then `.overhaul/handover/RESUME-ON-WINDOWS.md` for the setup.

## In brief

- **Check the layout first:** `git ls-remote --heads https://github.com/tripathisarthak457/android-base.git`. Many
  `wp/*` heads: the branch layout. Only `main` plus `archive/*` tags: everything was merged into `main`
  (HANDOVER.md §0 and §4.8).
- **Standing instructions:** keep going until told to stop; decide and record instead of asking; never compromise
  quality (Google/AOSP-level review before every merge, phone gates on release builds, Redmi `9a7c74f4`); Android
  first; a bug the user reports comes first; no separate billing; Opus 5.5 only, no model switches mid-chat; short
  status with an ETA when asked.
- **Safety:** never merge into or push `main` without the Android release gate's GO and the user's explicit go-ahead
  (a push to `main` deploys production through CI); never force-push; never commit secrets; PeriFlow code only to its
  private repo; never publish transcripts or the Gmail recordings; never kill processes you did not start; Gradle,
  adb and the emulator only through `.overhaul/bin/locked`; watch CPU heat and memory.
- **Commit trailer:** `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
