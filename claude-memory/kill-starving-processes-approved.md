---
name: kill-starving-processes-approved
description: "The user approved killing processes that starve the system during the overhaul (Oct 19), within limits; Windows core, Defender, drivers, the Claude app and the user's apps stay excluded"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-19T16:18:44.209Z
---

The user, Oct 19 21:50: "You can kill processes which suffocate system I approve" (after a build stalled on low memory
and a kill of a stuck lock waiter was refused by the permission classifier).

**Scope:** when the machine is starved (available memory under 1 GB with work waiting more than 15 minutes, or CPU
pinned for more than 30 minutes without progress), stop the cause: this project's own leftovers first (stuck
`bin/locked.py` waiters, idle or hung Gradle and Kotlin daemons, orphaned java, ffmpeg, python, adb), then idle
developer tooling, then a runaway non-essential helper with its name, numbers and reason logged first.

**Never:** Windows core processes and services (System, svchost, lsass, csrss, wininit, services, dwm, explorer,
LogonUI, wlanext, spoolsv), Defender, drivers, the Claude app, or apps the user is using; report those instead.

**Why:** the global rule "never kill processes you did not start" protects the user's system; this approval relaxes it
only for what starves the machine, and killing core services would break Wi-Fi or Windows mid-run.

**How to apply:** `scripts/AUTOPILOT-PROMPT.md` step 3 carries the rule. Log every kill in PROGRESS.md and the owning
handoff. If the permission classifier still refuses, report it and do not work around it; the user can add a
permission rule. Windows keeps file cache in the system working set and trims it only under allocation pressure, so
check `bin/locked status` (it shows the cache since Oct 19) before deciding memory is really short.
Related: [[laptop-memory-freeze]], [[android-base-windows-decisions]].
