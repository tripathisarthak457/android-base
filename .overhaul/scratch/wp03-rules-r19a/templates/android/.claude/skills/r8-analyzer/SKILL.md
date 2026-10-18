---
name: r8-analyzer
description: Analyzes Android build files and R8 keep rules to identify redundancies,
  broad package-wide rules, and rules that subsume library consumer keep rules. Use
  when developers want to optimize their app's size, remove redundant or overly broad
  keep rules, or troubleshoot Proguard configurations.
---

Google's `r8-analyzer` skill is vendored at `.agents/skills/r8-analyzer/` (Android skills v1.0.13, Apache-2.0).
First read the `r8-analyzer` row of the "Google's Android skills" table in `AGENTS.md`: it lists where this project
differs, and `AGENTS.md` wins wherever the two disagree. Then read `.agents/skills/r8-analyzer/SKILL.md` and resolve every
relative path in it from that folder.
