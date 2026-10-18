---
name: usage-reserve-10-percent
description: "Usage caps for the overhaul - 90% of the 5-hour window during 09:00-21:00 IST (the user's working hours), up to 99% overnight"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-18T11:54:15.642Z
---

Cap the overhaul's use of the 5-hour usage window by time of day:
- **09:00-21:00 IST (the user's working hours): stop at 90%**, so 10% stays free for the user's own work. This also
  applies before 09:00 to a window that resets after 09:00, so 10% is free when the day starts.
- **21:00-09:00 (the user is asleep): up to 99%.** Stop at about 97%, because usage can rise ~10% between 20-minute
  checks.
- When pausing for usage, schedule a one-shot resume 3 minutes after the window's reset, so the work never sits idle.
- Before a wind-down: save every note (STATUS §1, PROGRESS, SESSION file), back up off-machine, stop crons that would
  relaunch, and let running builds finish, then record their results.

**Why:** 2026-10-17 16:40: "leave 10% of usage for my personal work and wind up then save all progress..."; 23:10: "u can
consume full 99% in overnight as i am sleeping just working hours 9 am to 9 pm mai 90% kiya karo make sure u dont get
stuck and auto resume on times".
- The user can override a single window (Oct 18: 45% for their own work) or ask that nothing restarts by itself
  (Oct 18 16:55: "i will begin next session manually u dont auto start it"); then delete every resume one-shot and the
  autopilot, and wait for their message.

**How to apply:** the autopilot prompt (`scripts/AUTOPILOT-PROMPT.md` and the live cron) carries this rule. Related:
[[android-base-windows-decisions]].
