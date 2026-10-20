---
name: crm-app-from-staging
description: "Oct 20: the user's manager assigned a CRM Android app; the user wants it generated from the improved template (staging), preset Everything, once they give name and package id"
metadata:
  node_type: memory
  type: project
  originSessionId: 8d80ef9d-4533-47d0-b348-d4c6caaeb696
  modified: 2026-10-20T13:56:26.789Z
---

On Oct 20, 2026 at 19:24 the user's manager assigned them a new Android app: a plain CRM. The user wants it generated from the overhauled template (staging), not from `main`, with the preset "Everything". Generate nothing until they give the app name and package id; they said they'd give them on Oct 21. They first asked about having it by 11:00 Oct 21.

**Why:** the user wants the overhaul's improvements in their real work app, and they put quality ahead of speed: "no QUALITY COMPROMISES".

**How to apply:**
- Generate from a staging head that has passed its fix rounds and re-review, never from a RED staging.
- Build it fully, run its checks and tests, and smoke-test it on the Redmi before sending the zip.
- Warn them that the generated project is a snapshot: later template work, such as liquid glass and the approved Telegram items, will not reach it on its own.

Related: [[android-base-generator]], [[usage-reserve-10-percent]].
