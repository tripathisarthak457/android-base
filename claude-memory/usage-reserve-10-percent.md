---
name: usage-reserve-10-percent
description: "Usage caps for the overhaul - since Oct 19 09:25: every 5-hour window until the Android release, 80% by day (09:00-21:00 IST), 98% at night, auto-resume after each reset"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-19T03:51:15.557Z
---

**Current rule (the user, Oct 19 09:25, chosen from options with pros and cons): run in every 5-hour window until the
Android release, 80% by day, 98% at night, resuming by itself.**
- **09:00-21:00 IST (the user's working hours): cap 80%**, so 20% stays free for the user's own work. This also applies
  before 09:00 to a window that resets after 09:00, so 20% is free when the day starts.
- **21:00-09:00 (the user is asleep): cap 98%** (97% until Oct 19 10:47: "next reset in working hours 80% else night etc 98% ok?").
- Stop about 4 points under the cap (day 76%, night 94%) and add 7-minute one-shot checks from about 17 points under:
  usage rose up to ~15 points per 20 minutes and ~5 per 7 minutes with three workflows.
- At the cap: stop the workflows (running builds finish), note it, back up, and schedule a one-shot resume 3 minutes
  after the window's reset, so the work never sits idle.
- Before a wind-down: save every note (STATUS §1, PROGRESS, SESSION file), back up off-machine, stop crons that would
  relaunch, and let running builds finish, then record their results (`scripts/WINDDOWN-PROMPT.md`).

**History:** Oct 17 16:40 "leave 10% of usage for my personal work"; Oct 17 23:10 "u can consume full 99% in overnight as i
am sleeping just working hours 9 am to 9 pm mai 90% kiya karo make sure u dont get stuck and auto resume on times" (90%
day / 99% night until Oct 19); Oct 19 09:25 the current rule.
- The user can override a single window (Oct 18: 45% for their own work; Oct 19: 95% for the window ending 11:10) or ask
  that nothing restarts by itself (Oct 18 16:55: "i will begin next session manually u dont auto start it"); then
  delete every resume one-shot and the autopilot, and wait for their message.
- A cap "then stop" (Oct 19 06:20: "run till I hit 95% cap", later replaced by the current rule): stop about 4 points
  early and follow `scripts/WINDDOWN-PROMPT.md` (no resume after the reset).

**How to apply:** `scripts/AUTOPILOT-PROMPT.md` step 0 carries this rule; the live autopilot cron only says to follow that
file, so edit the file, not the cron. Crons die when the Claude app restarts: re-create them from STATUS.md's top line.
Related: [[android-base-windows-decisions]], [[working-rules-opus-sonnet]].
