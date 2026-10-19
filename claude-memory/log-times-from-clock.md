---
name: log-times-from-clock
description: "Read the clock (date) before writing any time into notes, logs or prompts; estimated times ran up to 18 minutes ahead on Oct 19"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-19T17:49:01.588Z
---

Before writing a time into PROGRESS.md, STATUS.md, REQUIREMENTS.md, decisions/LOG.md, a handoff or a script comment,
read it with `date '+%H:%M'` in the same command. Never estimate it from the conversation.

**Why:** on Oct 19 the logged times ran ahead of the real clock twice (about 10 minutes around 19:50, up to 18
minutes between 23:00 and 23:20) and had to be corrected from file timestamps. The user values exact records, and the
logs are what the next session trusts.

**How to apply:** put `date '+%H:%M'` at the start of every logging command and use its output. When correcting a time
inside a running workflow's agent prompt, leave the prompt text as it is (a changed prompt makes a resume re-run
finished agents) and correct only the notes and comments.
Related: [[status-show-parked-work]], [[working-rules-opus-sonnet]].
