---
name: android-base-generator
description: User's own android-base project generator (github tripathisarthak457/android-base, android-base.vercel.app); multi-day overhaul in progress — read .overhaul/STATUS.md first to resume
metadata:
  type: project
---

android-base is the user's own generator: a Python generator, Kotlin template(s), a Next.js site and a Go API on Vercel. It is cloned to `~/Documents/projects/tools/android-base`, integration branch `overhaul/multiplatform-v2`, with nothing pushed. PeriFlow (`~/Documents/projects/jorwen/periflow/periflow-android`) and VitFleet (`~/Documents/projects/tools/vitfleet/vitfleet-android`) were generated from it.

**To resume the overhaul, read `~/Documents/projects/tools/.overhaul/SESSION-2026-10-15.md` (latest session notes), then `STATUS.md`.** It has running state, package table, merge order, decisions, phone numbers and resume steps. Then `PROGRESS.md`, `REQUIREMENTS.md` (1–32 + memory safety), `notes/INDEX.md`, `design/MASTER-PLAN.md`, `design/wp/WP*.md`, `handoff/*.md`. Since Oct 12 a single workflow, `.overhaul/scripts/to-release.js`, carries all remaining packages to a release candidate; after a usage cutoff, resume it with `resumeFromRunId` (same session) or launch it fresh (new session). The autopilot prompt (cron every 20 min) is in `scripts/AUTOPILOT-PROMPT.md`.

User decisions and standing instructions:
- Telemetry is anonymous only. Compose Multiplatform targets Android + iOS (beta: iOS unverified). Swift is SwiftUI + @Observable MVI, iOS 17, XcodeGen, shown as "coming soon". Design system is custom, foundation-only, no Material components; material3.adaptive is allowed.
- Keep running until they say stop. Launch full-width subagents, Opus everywhere. Never compromise quality: Google/AOSP-level review before every merge.
- Don't ask questions; decide and record. Merge to main only when the full matrix is green. PeriFlow stays on local branch `overhaul/smooth-glass`, never pushed.
- Smoothness bar is iOS level on their Redmi Note 10 Pro (serial 9a7c74f4, 120 Hz). Final report must show measured before/after numbers. Every smoothness/motion/adaptive finding must be documented in generated projects' AGENTS.md/CLAUDE.md/docs.
- Visual polish is continuous: hunt UI defects while working.
- Oct 12: "do the best just achieve our goal": proceed autonomously through release.
- Oct 14: **Android first** (REQUIREMENTS 36). Go live as soon as the Android generator is 100% done (smoothness, security and all goals); Compose Multiplatform and Swift ship later. A release gate writes RELEASE-GATE-android.md, then ask the user for the go-ahead before merging to main and pushing (the classifier blocked an autonomous deploy step).
- Closing the laptop lid suspends it and stops all work (Oct 10 18:06 → Oct 12 13:06 lost). Workflow concurrency is capped at 6 agents (8 CPUs).
- When the user reports a bug they saw ("first fix it"), it takes priority: pause the pipeline workflows, which resume from cache, and run the fix alone. They also pause near the usage limit ("save progress and stop, continue at 6"); STATUS.md §0 holds the pause snapshot.
- Workflow resume is prefix-cached: after a cutoff of early agents, everything reruns live (agents resume from their handoffs). The phone may need `adb kill-server && adb start-server` after a replug. Emulator sessions must go through `bin/locked emu`.
- **Models (latest, Oct 15 20:55): Opus 5.5 only, in subagents too.** The scripts' `fableAgent()` passes options through unchanged (no `model` key), so agents run on the session's model (Opus 5.5). Don't add a `model` key to a resumed run: it changes every cache key and reruns finished agents live (that caused the Oct 15 usage spike). Fable's findings are in `.overhaul/FABLE-FINDINGS.md`.
- Oct 16: the user wants Google's **jetpack-compose-performance** skill *used*, not only vendored. It is now in every implementation and review prompt (BLOCKING on changed code). A template-wide audit writes `research/COMPOSE-PERF-AUDIT.md`, and every generated project vendors it, with an AGENTS.md instruction to run it.
- Usage: the 5-hour limit is shared by all models (Fable included; Fable also has its own weekly cap). Avoid switching models mid-chat: each switch rebuilds the cache of the whole long conversation (about a million tokens). Compact the chat once it passes about 50% of context.
- The auto-mode classifier blocks killing or stopping processes (even stale builds from dead agents); leave them to finish and tell agents never to kill processes.

**Why:** these were the user's answers and repeated instructions across Oct 8–10.
**How to apply:** don't re-ask; follow them when touching the generator, site or PeriFlow. Relaunch workflows from STATUS.md state after any restart. Related: [[android-skills-installed]], [[laptop-memory-freeze]].

- Oct 16: **Opus 5.5 only for every agent** (the user: Fable used too much usage). The scripts strip any `model` option in `fableAgent()`, so agents run on the session's model. The two former Fable workflows continue on Opus with the same goals. Fable's findings: `.overhaul/FABLE-FINDINGS.md`. The user wants Google's jetpack-compose-performance skill both used (in every UI agent prompt, BLOCKING in reviews, and a template-wide audit) and vendored into every generated project.
