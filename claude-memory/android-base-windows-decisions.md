---
name: android-base-windows-decisions
description: "User's 2026-10-17 decisions for resuming the android-base overhaul on Windows - native (no WSL), no merge-all, rewrite overhaul-workspace history, standing OK for non-main backup pushes"
metadata:
  node_type: memory
  type: project
  originSessionId: 59fc6fe8-3960-47f4-b4f7-458761b66208
  modified: 2026-10-18T08:12:14.790Z
---

On 2026-10-17 (first Windows session, after reading the Linux handover `.overhaul/handover/HANDOVER.md` from branch
`overhaul-workspace`), the user decided:

1. **Native Windows, not WSL2.** Workspace root `D:\projects\tools` (`android-base\` and `.overhaul\` side by side).
   The handover's WSL2 plan (RESUME-ON-WINDOWS.md §2) is superseded by the global rule "never WSL for Android".
2. **Merge-all: no.** Keep the branch model until the Android release gate, then one reviewed merge of
   `overhaul/multiplatform-v2` into `main` after his explicit go-ahead. `.overhaul/scripts/merge-all-to-main.js` is
   superseded; never run it.
3. **`overhaul-workspace` history: rewrite all 7 commits in place** (same branch name), scrubbing only the leaked
   strings (unredacted WIPE-READINESS.md and USER-MESSAGES.md in commits 1-2, the work-org name in
   `scripts/merge-all-to-main.js`, the work-org app id in a `perf/WP11` INVALID.txt, a third-party folder name in
   `SESSION-2026-10-16.md`). Done and pushed on Oct 17 (on his "do what's best" also scrubbing the work-org product
   name, PeriFlow's private perf/log texts and old commit ids); he files the GitHub Support purge request.
4. **Standing OK for non-main backup pushes** of android-base: `wp/*`, `overhaul/*`, `merged/*` tags and
   `overhaul-workspace`, at session end, after merges and before usage cutoffs, after a secret scan. Never `main`,
   never `--force` (the one-off history rewrite has its own OK), never deleting remote branches.
5. Default he did not object to: PeriFlow (WP11) stays out of the relaunch; its build secrets exist only in the Drive
   zip he asked not to touch.
6. **Cronet (REQUIREMENTS 39) is in the Android release** (14:30): "I care about full android release now, uske bad we
   will work on rest platforms". It was scheduled after the gate before; now package NET in `to-release.js` does it
   (after WP06 and WP02-M2 merge) and WP12/WP13 wait for it. Android first, then Compose Multiplatform and Swift.
   Same day he added **Brotli on by default for every generated app** to NET (okhttp-brotli on the shared OkHttpClient):
   his Vercel site serves Brotli but not HTTP/3, so Cronet's HTTP/3 gains need a VPS behind Caddy with UDP 443 open.
   He wants a plain-English hourly status (he "always feels left out" otherwise).
8. Oct 18, also in the Android release: **REQUIREMENTS 40** (live FPS and performance stats beside the DEV badge in dev
   and debug builds, a performance log and a 0-100 optimisation score; package HUD) and **41** (iOS scrolling by default
   on every scrollable surface).
7. Ultracode: advised against for this project (memory-bound laptop, the scripts already parallelise); effort stays
   xhigh because workflow agents inherit the session's effort.

**Why:** his answers to HANDOVER §1.7 items 0 and 10, and to the conflict between the handover's push policy and his
global "ask before pushing" rule.
**How to apply:** don't re-ask these. Pushes outside that scope (main, force, deletes, PeriFlow's private repo) still
need asking. Related: [[android-base-generator]].
