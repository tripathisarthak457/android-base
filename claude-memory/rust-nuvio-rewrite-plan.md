---
name: rust-nuvio-rewrite-plan
description: "Rust 1:1 rewrite of NuvioDesktop — Phase-0 plan done 2026-10-18, ON HOLD until the user says start; plan at ~/.claude/plans/rust-nuvio-phase0/"
metadata:
  node_type: memory
  type: project
  originSessionId: 54d217bc-3bc1-4cde-a497-1e8ae558bef6
  modified: 2026-10-18T17:10:38.506Z
---

The user wants a ground-up Rust 1:1 clone of NuvioDesktop (NuvioMedia/NuvioDesktop, `Dev`, 0.1.31-alpha, GPL-3.0) for
Windows, macOS and Linux, under a new name. The Phase-0 package (audit, architecture, risks, phased ETA, 15 open
questions) was finished on 2026-10-18 and is **ON HOLD**: the user said "I don't have bandwidth, will let you know when
to start". Do not start P0, installs or repo creation until they say so.

- Plan: `C:\Users\crazyhusir\.claude\plans\rust-nuvio-phase0\PLAN.md`, the canonical copy. The raw Opus reports sit
  beside it: `audit.md`, `architecture.md`, `plan-eta-risks.md`, `critiques.md`.
- Headline decisions:
  - Slint 1.18; mpv 0.41 / FFmpeg 9.0 runtime built in CI.
  - Windows video goes in a DirectComposition layer under the UI (gated on spike S1b); macOS and Linux draw video as a
    GL texture.
  - Likely 368 dev windows ≈ 44 weeks (range 272–525 dev windows).
- Blocking before P0:
  - Q1 project folder under `D:\projects` (no default).
  - Q2 per-item install approvals.
  - Q3 the GitHub repo and a standing rule for non-default-branch pushes.
  - Q4 overnight automation.
  - Q5 this project's share of the weekly usage cap.
- The user asked whether the rewrite is worth it. The answer: it is not justified by Windows playback alone (the
  original already renders mpv natively there). The real gains are RAM (3–5x), startup (2–4x), Linux/Wayland and codecs.
  Recommended first step: measure Nuvio 0.1.31's real startup, RAM and CPU on this laptop before committing.
- Open cleanup: two workflow auditors wrote 6 scratch files to `%TEMP%` (`x`, `en.txt`, `he.txt`, `de.txt`, `id.txt`,
  `pt.txt`). Recycling them needs the user's OK.

**Why:** the brief requires the plan to be confirmed before any code, and the user paused the project on 2026-10-18.
**How to apply:** when the user says start, re-read PLAN.md, ask Q1–Q5 first, and re-check the upstream `Dev` drift
since 2026-10-17. Workflow routing follows [[workflow-model-routing]]; usage pacing follows
[[usage-reserve-10-percent]].
