---
name: android-profiler
description: 'Records and analyzes Android performance data and diagnoses performance
  bottlenecks across user apps, system apps, and system services, even if the user
  does not explicitly mention profiling or traces. Use when: recording system traces,
  CPU call stacks, Vulkan GPU/DRM memory, or Java/native heap profiles; analyzing
  Perfetto traces for UI jank, frame drops, ANRs, or app startup latency issues; investigating
  CPU bottlenecks, IPC/Binder storms, or thread scheduling stalls; diagnosing battery
  drain, modem power rails, and kernel wake locks; debugging memory pressure, Low
  Memory Kills (LMK), OOMs, leaks, and allocation churn; performing A/B baseline trace
  comparisons to find regressions; executing ad hoc PerfettoSQL queries on Android
  traces.'
---

Google's `android-profiler` skill (Android skills v1.0.15, Apache-2.0) is vendored for the generated projects at `templates/android/.agents/skills/android-profiler/`.
Read `templates/android/.agents/skills/android-profiler/SKILL.md` and resolve every relative path in it from that folder.
When you change a template, also read the `android-profiler` row of the "Google's Android skills" table in that template's `AGENTS.md`:
it lists where the generated projects differ, and it wins wherever the two disagree.
