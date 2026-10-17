---
name: android-profiler
description: >
  Manages Android performance profiling and debugging. Triggers when the user
  asks to record or analyze Android performance data, such as system traces,
  heap dumps, method recordings, callstack samples, memory allocations, or
  investigate bottlenecks, jank, memory leaks, and app startup issues on
  Android, or when the user asks to write, debug, or execute ad-hoc SQL
  queries. Applies to both user and system apps or services.
---

Google's `android-profiler` skill is vendored at `.agents/skills/android-profiler/` (Android skills v1.0.13, Apache-2.0).
First read the `android-profiler` row of the "Google's Android skills" table in `AGENTS.md`: it lists where this project
differs, and `AGENTS.md` wins wherever the two disagree. Then read `.agents/skills/android-profiler/SKILL.md` and resolve every
relative path in it from that folder.
