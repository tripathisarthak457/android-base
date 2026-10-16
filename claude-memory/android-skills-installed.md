---
name: android-skills-installed
description: Google's official Android agent skills are installed as a Claude Code plugin; read the relevant SKILL.md before Android work
metadata:
  type: reference
---

Google's Android skills (github.com/android/skills, Apache-2.0) are installed at user scope as the Claude Code
plugin `android-skills@android-skills` (installed 2026-10-09 at the user's request). Files:
`~/.claude/plugins/marketplaces/android-skills/<area>/<skill>/SKILL.md` plus `references/`.

**How to apply:** before Android/Compose work, read the relevant skill. The ones that matter most for the user's
generator: navigation-3, navigation-event, adaptive, edge-to-edge, testing-setup, agp-9-upgrade, r8-analyzer,
android-profiler, and the intent/permissions security skills. Refresh with
`claude plugin marketplace update android-skills`. Digest for the overhaul: `.overhaul/research/ANDROID-SKILLS-DIGEST.md`.
Related: [[android-base-generator]].
