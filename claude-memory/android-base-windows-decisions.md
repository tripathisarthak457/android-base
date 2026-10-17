---
name: android-base-windows-decisions
description: "User's 2026-10-17 decisions for resuming the android-base overhaul on Windows - native (no WSL), no merge-all, rewrite overhaul-workspace history, standing OK for non-main backup pushes"
metadata:
  node_type: memory
  type: project
  originSessionId: 59fc6fe8-3960-47f4-b4f7-458761b66208
  modified: 2026-10-17T06:56:32.039Z
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
   `SESSION-2026-10-16.md`). One `--force-with-lease` push, only after he has seen the verified diff.
4. **Standing OK for non-main backup pushes** of android-base: `wp/*`, `overhaul/*`, `merged/*` tags and
   `overhaul-workspace`, at session end, after merges and before usage cutoffs, after a secret scan. Never `main`,
   never `--force` (the one-off history rewrite has its own OK), never deleting remote branches.
5. Default he did not object to: PeriFlow (WP11) stays out of the relaunch; its build secrets exist only in the Drive
   zip he asked not to touch.

**Why:** his answers to HANDOVER §1.7 items 0 and 10, and to the conflict between the handover's push policy and his
global "ask before pushing" rule.
**How to apply:** don't re-ask these. Pushes outside that scope (main, force, deletes, PeriFlow's private repo) still
need asking. Related: [[android-base-generator]].
