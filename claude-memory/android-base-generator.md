---
name: android-base-generator
description: User's own android-base app generator (github tripathisarthak457/android-base, android-base.vercel.app); multi-day overhaul since Oct 8, moved to Windows/WSL2 after the Oct 16 laptop wipe — read .overhaul/handover/HANDOVER.md first
metadata:
  type: project
---

android-base is the user's own generator: a Python generator, a Kotlin/Compose Android template, a Compose Multiplatform
template, a hidden SwiftUI template, a Next.js site and admin portal, and a Go API on Vercel. Workspace root ("tools"):
`android-base/` (integration branch `overhaul/multiplatform-v2`; package branches `wp/<WP>` in worktrees
`.overhaul/wt/<WP>`) and `.overhaul/` side by side. On Linux the root was `/home/crazyhusir/Documents/projects/tools`;
the recommended Windows setup recreates that exact path inside WSL2 (user `crazyhusir`). PeriFlow (private app,
`../jorwen/periflow`, repo now **`JorwenLabs/periflow`**, PRIVATE; the old `tripathisarthak457/periflow` URL
redirects) and VitFleet (a client's private app) were generated from it.

**To resume, read `.overhaul/handover/HANDOVER.md` first** (§0, §1.3, §1.7, §6, §8), then
`.overhaul/handover/RESUME-ON-WINDOWS.md` (setup, restore, relaunch), `.overhaul/handover/PUSHED.md` (what is on GitHub,
with SHAs), `.overhaul/handover/REQUIREMENTS-STATUS-FINAL.md` (every requirement), `.overhaul/SESSION-2026-10-16.md`.
`.overhaul/REQUIREMENTS.md` has **39** items plus memory safety (39, Cronet/HTTP3 opt-in, added Oct 16, after the
Android release). `STATUS.md` is partly stale (trust §1 ids and §3 decisions only).

State at the wipe (Oct 16, ~12:45 IST): merged into the integration branch: WP01, WP02-M1, WP09, WP10 (`a402d87`).
Everything else is on `wp/*` branches. **Pushed to GitHub on Oct 16** (no force, `main` untouched at `99c4edf`): every
branch except `main`, the `merged/*` tags, the WIP snapshots `backup/<name>-wip-2026-10-16` (WP03 47 files, the
bottom-sheet system, does not compile at `SheetFlingBehavior.kt:88`; WP05-glass 43; WP04-perf 4), and the workspace as
the orphan branch `overhaul-workspace`. PeriFlow's `overhaul/smooth-glass` and its WIP went to its private repo. Raw
traces/frames/recordings: draft release `overhaul-raw-data-2026-10-16` was planned but NOT created at 12:45 (check with
`gh release view`); they are in the user's Google Drive backup `backup-2026-10-16` (`00-android-base-not-on-github-p*.zip`).
No workflow is running; relaunch fresh, two at a time, in the order of HANDOVER §5.11.

**Merge-all into main: did NOT run; pending the user's decision; do not run `.overhaul/scripts/merge-all-to-main.js`
without asking him again in chat.** (He asked on Oct 16 at 11:11 and 11:17 to merge everything into `main` and delete
the branches; the auto-mode classifier blocked it once; the script's `vercel.json` "pause" pauses nothing, because CI's
deploy job ships `main`.)

User decisions and standing instructions:
- Keep going until told to stop. Don't ask questions; decide and record (ask only for what only he can do: physical
  actions, logins, money, the deploy go-ahead, and the open decisions in HANDOVER §1.7, all in one message).
- Never compromise quality: a fresh-context Google/AOSP-level review before every merge; phone gates on release builds
  on his Redmi Note 10 Pro (serial 9a7c74f4, 120 Hz). Final report must show measured before/after numbers.
- **Android first** (REQUIREMENTS 36): release gate (`RELEASE-GATE-android.md`) first, then the user's explicit
  go-ahead in chat, then merge to `main`, push and deploy. A push to `main` deploys production through CI. Compose
  Multiplatform and Swift ship later.
- Pushing non-`main` branches is allowed (Oct 16: "push to public there is no problem"). The orchestrator pushes the
  package branches, the integration branch, the `merged/*` tags and the `overhaul-workspace` branch as an off-machine
  backup at the end of every session and after every merge (HANDOVER §6.1 #6); agents inside workflows never push.
  Never push `main`, never force-push. PeriFlow code goes only to its private repo. Never commit secrets.
- No separate billing: extra usage stays off. **Opus 5.5 only**, session and subagents (Oct 15 20:55; Oct 16 10:34).
  Never switch models mid-chat, never add a `model` key to a run you mean to resume (it invalidates the resume cache),
  launch workflows two at a time. Compact the chat once it passes about half its context.
- A bug the user reports comes first ("first fix it"): pause the pipelines, fix it alone.
- Telemetry is anonymous only. CMP targets Android + iOS (beta: iOS unverified). Swift is SwiftUI + @Observable MVI,
  iOS 17, XcodeGen, "coming soon". Design system is custom, foundation-only, no Material components;
  material3.adaptive is allowed. CP1 (motion) was decided by an agent; CP2 (glass) is open.
- Use Google's jetpack-compose-performance skill (BLOCKING in reviews) and vendor the Android skills (v1.0.15) into
  generated projects (REQUIREMENTS 38).
- The Gmail reference recordings show real e-mails: describe motion only, never publish them.
- Never kill or stop processes you did not start (the auto-mode classifier blocks it too). Gradle only via
  `.overhaul/bin/locked gradle`, adb via `bin/locked adb`, the emulator only inside `bin/locked emu`, heavy commands
  via `bin/memwait`.
- Send a short status with an ETA when asked ("send summary and eta") and at milestones; save notes (SESSION file,
  STATUS §1, PROGRESS, memory) before every usage limit or compaction.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

**Why:** these are the user's answers and repeated instructions across Oct 8–16, plus the decisions recorded in the
handover documents.
**How to apply:** don't re-ask; follow them when touching the generator, the site or PeriFlow. Superseded and no longer
valid: "nothing pushed / PeriFlow never pushed", "Fable first", "merge to main only when the full matrix is green"
(replaced by the release gate plus the go-ahead), and the Sept memory "push verified work to main without asking".
Related: [[android-skills-installed]], [[laptop-memory-freeze]].
