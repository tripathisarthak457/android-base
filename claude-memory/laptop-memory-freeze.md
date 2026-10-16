---
name: laptop-memory-freeze
description: User's 15 GB Pop!_OS laptop (Mi NoteBook Ultra) froze under memory pressure (Oct 10) and powered off from a suspect thermal sensor or charger (Oct 16); memory guard, thermlog, how to recover
metadata:
  type: project
---

On 2026-10-10 at 15:23 the user's laptop froze. journalctl showed no panic, OOM or thermal entries, just a silent gap. The cause was memory pressure: zram swap at swappiness 180 with no systemd-oomd or earlyoom, about 3 minutes after 9 agents started Gradle builds and tests. Each freeze also ends the Claude session, which kills the workflows and the session-only cron autopilot.

**Why:** 15 GB RAM. Gradle daemon 4 GB + Kotlin daemon 2 GB, plus Chrome and Claude, plus parallel heavy commands, is past the limit.
**How to apply:**
- Run Gradle only through `~/Documents/projects/tools/.overhaul/bin/locked gradle` (memwait ≥ 3.5 GB, 4 workers, 15 min idle timeout).
- Run other heavy commands through `bin/memwait`.
- Keep one Gradle daemon (identical JVM args).
- Don't start the emulator during builds.
- Suggest `sudo apt install earlyoom` if the user hasn't done it. It's a system change, so the user runs it.
- After a crash: `last -x` and `journalctl -b -1` to confirm, then relaunch from `.overhaul/STATUS.md`.

**Oct 16, 10:32: again.** It froze about 6 minutes after resuming from a 13.5-hour suspend and about 1 minute after
five workflows were launched. Yesterday's daemons were still resident (11.6 GB used), and no pstore panic was recorded.
Workflow agents run inside the one Claude process (about 370 MB), so stale Gradle and Kotlin daemons are the likely
load. After any resume, check `free -m` before launching, and launch workflows two at a time. The fix was given to the
user again: earlyoom, journald SyncIntervalSec, sysrq.

**Oct 16, about 10:41: a second power-off, and this one was NOT memory** (9 GB free, no builds). The likely cause is
thermal sensor SEN3 (thermal_zone3, INT3403; firmware critical trip 80 °C). It reads 60–125 °C and jumps within
seconds while the CPU is at about 60 °C, likely a faulty sensor or a hot USB-C charging circuit. The battery was charging
each time. `.overhaul/bin/thermlog` logs every sensor to `.overhaul/logs/thermal.log` (synced) for the next post-mortem.
After an unexplained power-off, read the tail of that log first. Details: `.overhaul/SESSION-2026-10-16.md`.
