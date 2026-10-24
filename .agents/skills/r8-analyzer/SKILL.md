---
name: r8-analyzer
description: Analyzes Android build files and R8 keep rules to identify redundancies,
  broad package-wide rules, and rules that subsume library consumer keep rules. Use
  when developers want to optimize their app's size, remove redundant or overly broad
  keep rules, or troubleshoot Proguard configurations.
---

Google's `r8-analyzer` skill (Android skills v1.0.17, Apache-2.0) is vendored for the generated projects at `templates/android/.agents/skills/r8-analyzer/`.
Read `templates/android/.agents/skills/r8-analyzer/SKILL.md` and resolve every relative path in it from that folder.
When you change a template, also read the `r8-analyzer` row of the "Google's Android skills" table in that template's `CLAUDE.md`:
it lists where the generated projects differ, and it wins wherever the two disagree.
