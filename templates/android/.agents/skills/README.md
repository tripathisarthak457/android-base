# Google's Android skills

Google's official Android skills for AI agents, from https://github.com/android/skills at tag v1.0.15
(commit 5bda39f154fb859a08d45e2392f3568e049f83fb), under the Apache License 2.0 in `LICENSE.txt`.

| Skill | Upstream folder | Shipped |
|---|---|---|
| `navigation-3` | `navigation/navigation-3` | always |
| `navigation-event` | `navigation/navigation-event` | always |
| `adaptive` | `jetpack-compose/adaptive` | always |
| `edge-to-edge` | `system/edge-to-edge` | always |
| `testing-setup` | `testing/testing-setup` | always |
| `android-profiler` | `profilers/android-profiler` | always |
| `r8-analyzer` | `performance/r8-analyzer` | always |
| `android-intent-security` | `security/android-intent-security` | always |
| `android-permissions-security` | `security/android-permissions-security` | always |
| `agp-9-upgrade` | `build-system/agp/agp-9-upgrade` | always |
| `jetpack-compose-performance` | `jetpack-compose/jetpack-compose-performance` | always |
| `android-cli` | `devtools/android-cli` | always |
| `restore-credentials` | `identity/restore-credentials` | with the `auth` feature |
| `verified-email` | `identity/verified-email` | with the `auth` feature |
| `camerax` | `camera/camerax` | with the `media` feature |

Each skill folder is an unmodified upstream copy. The only addition is `LICENSE.txt` beside each `SKILL.md`,
which the skill's front matter points at. `MANIFEST.sha256` holds the SHA-256 of every file below the skill
folders, so a test can prove they are unchanged.

Where a skill disagrees with `AGENTS.md`, `AGENTS.md` wins. Its "Google's Android skills" table says when to
read each skill and how this project differs.

Codex, Gemini CLI and Android Studio read this folder. Claude Code reads the pointers in `.claude/skills/`,
which name these files, so this folder stays the only copy.

To refresh a skill, replace its folder with a newer upstream copy (`android skills add <name> --project=.` with
the Android CLI, or a checkout of the repository), put `LICENSE.txt` back beside its `SKILL.md`, and
regenerate `MANIFEST.sha256` (`sha256sum` over every file below the skill folders, paths relative to this
folder).
