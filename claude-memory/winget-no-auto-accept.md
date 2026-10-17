---
name: winget-no-auto-accept
description: "Never pass --accept-source-agreements or --accept-package-agreements to winget, even for read-only queries; use --source winget"
metadata:
  node_type: memory
  type: feedback
  originSessionId: 59fc6fe8-3960-47f4-b4f7-458761b66208
  modified: 2026-10-17T07:55:21.850Z
---

On 2026-10-17 a read-only `winget show --id Gyan.FFmpeg.Essentials -e --accept-source-agreements` recorded acceptance
of the Microsoft Store (msstore) source's terms on the user's behalf, because the query touched msstore. Nothing was
installed, but accepting terms is the user's decision.

**Why:** accepting agreements needs the user's explicit OK (his global rule: ask before any change; agreements and
consents are his to accept).
**How to apply:** for lookups use `winget show --id <id> -e --source winget` (the community source has no agreement
prompt) and never add `--accept-*` flags; when an install is approved, let the user see and accept any agreement, or
run it in a Terminal tab he watches. Related: [[android-base-windows-decisions]].
