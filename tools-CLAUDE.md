# tools/: the android-base overhaul workspace (Windows)

This folder holds, side by side:

| Path | What |
|---|---|
| `android-base/` | clone of `github.com/tripathisarthak457/android-base`; the main checkout stays on the integration branch `overhaul/multiplatform-v2` |
| `.overhaul/` | the overhaul workspace: a plain folder, never a git repository; its off-machine backup is the orphan branch `overhaul-workspace` |
| `.overhaul/wt/<WP>/` | git worktrees, one per package branch `wp/<WP>` |

Read `.overhaul/handover/HANDOVER.md` first (the full briefing: goals, the 39 requirements, the plan, where every package
stopped, the rules), then the newest `.overhaul/SESSION-*.md`.

The handover was written on Linux and plans a WSL2 setup. **This machine runs the work natively on Windows** (the
user's decision, 2026-10-17). Wherever the handover or `RESUME-ON-WINDOWS.md` mention WSL2, usbipd, `apt`, `flock`,
`/home/crazyhusir/...` or the emulator, the notes below win.

## Windows specifics

- The old root `/home/crazyhusir/Documents/projects/tools` is `D:/projects/tools`. Forward slashes work in Git Bash, git,
  Python and Java.
- The Bash tool is Git Bash: there is no `flock`, `rsync`, `zstd`, `pgrep` or `lslocks`. Run Gradle and adb only through
  `.overhaul/bin/locked` (`locked gradle <cmd>`, `locked adb <cmd>`, `locked status`), and other heavy commands as
  `.overhaul/bin/memwait <cmd...>`. `locked gradle` runs one build machine-wide with 4 workers after a free-memory wait;
  take adb before gradle when a command needs both. A CPU-temperature wait exists but is off unless `THERMWAIT_C` is set
  (the user considers the Linux heat problems a Linux issue).
- Claude's shells do not inherit the user environment variables. `locked` and `memwait` set `JAVA_HOME`, `ANDROID_HOME`,
  `GRADLE_USER_HOME` (`D:\.caches\gradle`), `GOMODCACHE`, `GOCACHE`, `PIP_CACHE_DIR`, `NPM_CONFIG_CACHE` (and
  `KONAN_DATA_DIR` once the user sets it) from the registry, and put Node on PATH. A build run without them would create
  the forbidden `~\.gradle`.
- Gradle memory comes from the global `D:\.caches\gradle\gradle.properties`, which overrides the project files. Never pass
  `-Dorg.gradle.jvmargs` or `kotlin.daemon.jvmargs` on the command line: it starts a second daemon.
- `locked adb` keeps device paths (`/sdcard/...`) as typed. Elsewhere, set `MSYS_NO_PATHCONV=1` for
  `git show <ref>:.<path>`; with it set, git needs `D:/...` paths, not `/d/...`.
- The phone is the Redmi `9a7c74f4` over native USB adb. There is no emulator on this machine; when the phone is missing,
  do code and analysis first and record which phone steps remain.
- The clone has `core.autocrlf=false` (repository-local). Count CR bytes with Python, never with Git Bash's `grep`
  (it reports 0 on CRLF files). Until package WIN merges, 6 generator tests fail on this machine with no change at all
  (`.overhaul/logs/windows/generator-baseline.md`).
- Installed for the overhaul: ffmpeg/ffprobe 9.0.1 (winget; on PATH inside `locked` and `memwait`) and Perfetto's
  trace_processor v57.2 under `D:\.caches\perfetto` (run the android-profiler skill's wrapper with
  `USERPROFILE='D:\.caches\perfetto'`). Not on this machine: gh, Docker, WSL, staticcheck/govulncheck, a C compiler
  (so no `go test -race`). Ask the user before installing anything.

## Rules (brief; the full list is HANDOVER.md §6)

- `main` is production: a push to `main` deploys through CI. Never merge into or push `main` without the Android release
  gate's GO and the user's explicit go-ahead in chat. **Merge-all was declined (2026-10-17): never run
  `.overhaul/scripts/merge-all-to-main.js`.**
- Pushes: the orchestrator may push non-main backups without asking (`wp/*`, `overhaul/*`, `merged/*` tags) with
  explicit refspecs, after a secret scan. Never `main`, never `--force`, never delete remote branches. Agents inside
  workflows never push. `overhaul-workspace` was rewritten locally to scrub leaked names: it is pushed once, with
  `--force-with-lease`, only after the user OKs the diff; after that, normal pushes of it are backups like the others.
- Ask the user before any system change: installs, environment variables, PATH, services, files outside the project.
- Never commit secrets. PeriFlow (WP11) is out of this relaunch; its code goes only to its private repository.
- Never kill or stop processes you did not start.
- Opus 5.5 only, no model switches mid-chat. Start one workflow at a time while free memory is low.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
