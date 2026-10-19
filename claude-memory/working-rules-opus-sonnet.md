---
name: working-rules-opus-sonnet
description: "The user's working rules (Oct 18) - workflows for substantial work, Opus for all code and decisions, Sonnet only for supervised mechanical work (device runs, lookups), decision log, design pre-approved"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-19T00:48:20.789Z
---

The user's working rules (Oct 18 23:10, ultracode on; they adopted my adjustments the same evening):
- Substantial work goes through workflows and subagents; decide alone while the user is away and log every decision
  with its reasoning in `.overhaul/decisions/LOG.md`.
- **Opus 5.5** does all code, planning, architecture, reviews, merges, gate and pass/fail judgments, root causes, and
  everything the user reads. Opus is the advisor on every important decision. This session's own model never switches.
- **Sonnet 5.5 only for mechanical, token-heavy work with a fixed procedure:** phone runs through `bin/locked` (build,
  install, measure with scripts, screenshots, recordings, frame extraction, log pulls), web and doc lookups, file
  inventories. It returns structured facts plus raw-evidence paths, never judgments, never commits, never edits code.
- **Supervision is mandatory and proportional:** an Opus verifier re-derives key numbers from the raw files, looks at the
  images, checks the device state, and writes the user-facing result; then a separate Opus reviewer checks claims against
  evidence and reports only real problems. A pure lookup gets the verifier check against its source.
- Never Sonnet for: code, reviews, secret or security scans, perf trace diagnosis, visual or motion quality judgments,
  merges, anything that changes a branch or a decision.
- Design decisions are pre-approved: take them with Opus advice and the design skills, record the reasoning.
- Quality, polish and security first; save tokens only where that costs nothing in quality.
- **Decisions only the user can take** (rule exceptions, release gates, trade-offs an agent leaves open): ask them in
  chat with AskUserQuestion, one question per decision, every option with its pros, cons and catches, the recommended
  one first (the user, Oct 19 06:12: "ask me in question format with pros and cons and all catches I will answer").
  Never leave them only in a status line or a handoff.

**Why:** the user's pasted "Working instructions" plus "if u think its a good approach specially device checks and
controlling the device ... opus can supervise"; they asked for the adjustments to share with another project's session.
**How to apply:** new workflow steps that drive the phone follow the capture -> verify -> review pattern in
`scripts/hud-first.js` (Show phase); add `model: 'sonnet'` only to such capture steps, never to a call already in a run
you may resume. Related: [[android-base-windows-decisions]], [[usage-reserve-10-percent]].
