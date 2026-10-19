---
name: status-show-parked-work
description: "Status updates must list parked/waiting packages and how long they have waited, not only active work (the user missed liquid glass for days)"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-19T14:25:30.217Z
---

Every status update for the overhaul (hourly, ETA, wind-down reports) lists the parked packages in one line: what each
waits for and since when (last commit date of its branch), not only what is actively running.

**Why:** on Oct 19 at 19:55 the user asked "whats the status of liquid glass ... havent seen it for a while now". Glass
(WP05, WP05-glass) had been parked since Oct 15-17 waiting for WP03 and WP04, with 43 uncommitted bottom-bar files in its
worktree, and no update had mentioned it because updates only covered active work. They then asked whether it would
have been done at all had they not asked (it was owned by to-release.js's WP05 package, but the uncommitted work was at
risk until staging's WIP commits).

**How to apply:** the hourly cron prompt carries this line since Oct 19 19:53; keep it in any re-created hourly prompt
and in wind-down reports. When a package has waited more than 2 days, say so plainly and say what unblocks it.
Related: [[working-rules-opus-sonnet]], [[usage-reserve-10-percent]].
