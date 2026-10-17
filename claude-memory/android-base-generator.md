---
name: android-base-generator
description: User's own android-base app generator (github tripathisarthak457/android-base, android-base.vercel.app); multi-day overhaul since Oct 8, resumed natively on Windows at D:\projects\tools on Oct 17; read .overhaul/handover/HANDOVER.md first
metadata:
  type: project
---

android-base is the user's own generator: a Python generator, a Kotlin/Compose Android template, a Compose
Multiplatform template, a hidden SwiftUI template, a Next.js site and admin portal, and a Go API on Vercel. Workspace
root `D:\projects\tools` (native Windows): `android-base\` (main checkout on the integration branch
`overhaul/multiplatform-v2`; package branches `wp/<WP>` in worktrees `.overhaul\wt\<WP>`) and `.overhaul\` side by side;
`D:\projects\tools\CLAUDE.md` holds the Windows rules. On Linux the root was `/home/crazyhusir/Documents/projects/tools`.
PeriFlow (his private app, repo `JorwenLabs/periflow`) and VitFleet (a client's private app) were generated from it;
neither is on this machine.

To resume: `.overhaul/handover/HANDOVER.md` (§0, §1.3, §1.7, §6), then the newest `.overhaul/SESSION-*.md`. The
handover's WSL2 plan is superseded ([[android-base-windows-decisions]]). `.overhaul/REQUIREMENTS.md` has 39 items plus
memory safety. `STATUS.md` is partly stale (trust §1 ids and §3 decisions).

State on Oct 17: merged into the integration branch: WP01, WP02-M1, WP09, WP10 (`a402d87`). Everything else is on
`wp/*` branches; the uncommitted work of WP03 (bottom sheet, does not compile), WP05-glass and WP04-perf is back in
their worktrees.

Standing instructions (Oct 8-17):
- Keep going until told to stop; decide and record. Ask only for what only he can do (physical actions, logins, money,
  the deploy go-ahead) and for system changes (his global rules).
- Never compromise quality: a fresh-context Google/AOSP-level review before every merge; phone gates on release builds
  on his Redmi (serial 9a7c74f4, 120 Hz); the final report shows measured before/after numbers.
- Android first (REQUIREMENTS 36): release gate, then his explicit go-ahead in chat, then merge to main. A push to main
  deploys production through CI.
- Opus 5.5 only, session and subagents; no model switch mid-chat; no `model` key on a run meant to resume; no
  separate billing.
- A bug he reports comes first ("first fix it").
- Google's jetpack-compose-performance skill is BLOCKING in reviews; the skills are vendored into generated projects
  (REQUIREMENTS 38).
- The Gmail reference recordings show real e-mails: describe motion only, never publish them.
- Gradle and adb only through `.overhaul/bin/locked`; never kill processes you did not start.
- A short status with an ETA when asked; save notes (SESSION file, STATUS §1, PROGRESS, memory) before every usage
  limit or compaction.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

**Why:** his answers and repeated instructions across Oct 8-17.
**How to apply:** don't re-ask. Superseded: "never push", "Fable first", the WSL2 plan, merge-all into main.
Related: [[android-base-windows-decisions]], [[android-skills-installed]], [[laptop-memory-freeze]].
