---
name: anr-audit-and-fix
description: Audits Android codebases for ANR risks and diagnoses ANR stack dumps.
  Use this skill when scanning Android code for ANR risks or triaging ANR stack dumps.
  Don't use for non-ANR crashes, dropped frames, general CPU profiling, or Perfetto
  system traces.
---

Google's `anr-audit-and-fix` skill is vendored at `.agents/skills/anr-audit-and-fix/` (Android skills v1.0.17, Apache-2.0).
First read the `anr-audit-and-fix` row of the "Google's Android skills" table in `CLAUDE.md`: it lists where this project
differs, and that table wins wherever the two disagree. Then read `.agents/skills/anr-audit-and-fix/SKILL.md` and resolve every
relative path in it from that folder.
