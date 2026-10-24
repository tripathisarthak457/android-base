# Google's Android skills

Google's official Android skills for AI agents, from https://github.com/android/skills at tag v1.0.17
(commit f88c691a8a9420694aa174dcb0f6fcd316374932), under the Apache License 2.0 in `LICENSE.txt`.

| Skill | Upstream folder |
|---|---|
| `adaptive` | `jetpack-compose/adaptive` |
| `agp-9-upgrade` | `build-system/agp/agp-9-upgrade` |
| `android-cli` | `devtools/android-cli` |
| `android-for-cars-parked-apps` | `cars/android-for-cars-parked-apps` |
| `android-intent-security` | `security/android-intent-security` |
| `android-permissions-security` | `security/android-permissions-security` |
| `android-profiler` | `profilers/android-profiler` |
| `anr-audit-and-fix` | `performance/anr-audit-and-fix` |
| `appfunctions` | `device-ai/appfunctions` |
| `camerax` | `camera/camerax` |
| `display-glasses-with-jetpack-compose-glimmer` | `xr/display-glasses-with-jetpack-compose-glimmer` |
| `edge-to-edge` | `system/edge-to-edge` |
| `engage-sdk-integration` | `play/engage-sdk-integration` |
| `glance-canonical-layouts` | `system/glance-canonical-layouts` |
| `jetpack-compose-performance` | `jetpack-compose/jetpack-compose-performance` |
| `litert-lm` | `device-ai/litert/litert-lm` |
| `litert-runtime` | `device-ai/litert/litert-runtime` |
| `media3-cast-integration` | `media/media3-cast-integration` |
| `ml-kit-genai-prompt-api` | `device-ai/ml-kit-genai-prompt-api` |
| `navigation-3` | `navigation/navigation-3` |
| `navigation-event` | `navigation/navigation-event` |
| `play-billing-library-version-upgrade` | `play/play-billing-library-version-upgrade` |
| `play-policy-insights` | `play/play-policy-insights` |
| `projected-hardware` | `xr/projected-hardware` |
| `r8-analyzer` | `performance/r8-analyzer` |
| `restore-credentials` | `identity/restore-credentials` |
| `testing-setup` | `testing/testing-setup` |
| `verified-email` | `identity/verified-email` |
| `wear-compose-m3` | `wear/wear-compose-m3` |
| `wear-widgets` | `wear/wear-widgets` |

Left out on purpose:

- `styles`: it needs an alpha Compose release and experimental opt-ins, and this project uses stable releases only.
- `migrate-xml-views-to-jetpack-compose`: it migrates XML views, and this project has none.
- `leanback-to-compose-tv-migration`: it migrates Leanback screens, and this project has none.

Each skill folder is an unmodified upstream copy. The only addition is `LICENSE.txt` beside each `SKILL.md`,
which the skill's front matter points at. `MANIFEST.sha256` holds the SHA-256 of every file below the skill
folders, so a test can prove they are unchanged.

The "Google's Android skills" table in `CLAUDE.md` (and its mirror, `AGENTS.md`) says when to read each skill
and how this project differs; where a skill disagrees with it, the table wins.

Codex, Gemini CLI and Android Studio read this folder. Claude Code reads the pointers in `.claude/skills/`,
which name these files, so this folder stays the only copy.

To move every skill to a newer release, run `python3 tools/android_skills.py --tag <tag>` from the project root
and review the diff. It never runs anything it downloads.
