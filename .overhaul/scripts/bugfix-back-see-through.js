export const meta = {
  name: 'bugfix-back-see-through',
  description: "Fix the bug the user reported on Oct 19: during a back swipe the pushed screen's card is see-through, so the page beneath shows through it. Reproduce on the newest combined code, root cause, fix with regression tests, phone proof, then a fresh review",
  phases: [
    { title: 'Fix', detail: 'reproduce on the combined code, root cause, fix with tests, phone proof' },
    { title: 'Review', detail: 'fresh-context review, then fixes' },
  ],
}

// The user, Oct 19 15:41: "THE BACK GLICTH IS STILL THERE SEE I CLICKEC ON OPEN SOURCE LICENSES ... CHECK THE ISSUE". Their
// standing rule: a bug they report comes first, so this runs alone, before the staging consolidation.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const BUG = `${OV}/bugs/BUG-2026-10-19-back-see-through.md`

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) first. Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${OV}/bugs/2026-10-19-back-licenses/logs/, polled. Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md; the template's AGENTS.md: no Material, AppTheme only); never weaken, skip or delete a test. Commit each step with a message that says why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. Never push, never touch main or overhaul/multiplatform-v2, never delete a branch. Phone 9a7c74f4: screenshots and recordings to /data/local/tmp, never /sdcard; screenrecord needs --size 720x1600; restore any device setting you change; judge on release-like builds.`

const STATUS = { type: 'object', properties: {
  state: { type: 'string', enum: ['fixed', 'not-reproduced', 'blocked'] },
  reproduced_on: { type: 'string' },
  root_cause: { type: 'string' },
  fix_branch: { type: 'string' },
  commits: { type: 'array', items: { type: 'string' } },
  tests: { type: 'array', items: { type: 'string' } },
  evidence: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'reproduced_on', 'root_cause', 'fix_branch', 'commits', 'tests', 'evidence'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Fix')
let r = await agent(`You fix a bug the user reported, which comes before everything else. ${RULES}
Read ${BUG} first (the user's words, the screenshot at ${OV}/bugs/2026-10-19-back-licenses/user-screenshot.png: look at it, the suspects). The app the user ran was built from wp/HUD (WP06 + the overlay), without WP04's navigation rewrite, so first find out whether the newest code has the defect.
1. Build the newest combined code: the worktree ${OV}/wt/_preview on the local branch preview/catalog-2026-10-19 (stopped mid-way; read ${OV}/out/preview/NOTES.md) is meant to hold wp/WP04 with the back card T + C1 (${OV}/out/WP04-perf/backcard-texture.patch then ${OV}/out/WP04-perf/back-google/c1.patch), wp/WP06, wp/WP03 and wp/HUD. Finish those merges there if they are not done (record resolutions in NOTES.md), then generate and build the everything preset as a release-like build (devRelease or the benchmark variant WP04 uses) and install it through ${L} adb. That branch is local and never pushed.
2. Reproduce on the phone exactly as the user did: Settings, Open source licences, then a back swipe from the edge, held mid-way, then cancel, then commit; also a deep pushed screen with a long list (scrolled), a sheet, and a cover. Capture held-frame screenshots and a recording. Also reproduce on a build of wp/WP04 alone and of wp/HUD alone, so you know which branch has the defect and which does not.
3. Root cause with file:line (the screen not painting an opaque background and relying on the host or window, the card layer drawing content without the background, alpha applied to the card during the drag, the scrim missing, a transparent window background, or something else), and explain why WP04's acceptance row "one opaque card, no double exposure" did not catch it.
4. Fix it at its root, on the branch that owns the cause (wp/WP04 for the shell and the card, wp/WP03 for AppScaffold and the surfaces, wp/WP06 for the settings screens; if the cause spans branches, fix each in its own commit). Regression tests that fail before and pass after: at the unit level (for example a layer-probe or screenshot-style test that the card is opaque over a page with distinct content) and a phone check row added to the owning acceptance (the card's pixels over a contrasting page stay the card's colour at mid-gesture).
5. Verify on the phone with the fixed combined build: the same scenes, held-frame screenshots before and after, no double exposure anywhere; and that the back gesture's smoothness did not regress (pushpop_gesture through the WP04 perf harness, compared with T + C1's numbers in ${OV}/out/WP04-perf/back-google/RESULT.md).
6. Write "## Result" in ${BUG} (which build the user saw, whether the newest code had it, root cause with file:line, fix commits, tests, before and after evidence) and an orchestrator-visible note at the top of the owning handoff's "## Progress".`, { label: 'bugfix:run', phase: 'Fix', schema: STATUS })
if (!r) throw new Error('bug fix agent failed')

phase('Review')
let review = null
for (let round = 1; round <= 3 && r.state === 'fixed'; round++) {
  review = await agent(`You are a separate reviewer at Google/AOSP level. Review the fix for the user's see-through back card: ${JSON.stringify(r)}. Read ${BUG} and the commits on ${r.fix_branch} (git show each). Check the root cause is real and explains the user's screenshot, the fix holds for every pushed screen, sheet and cover (not only the licences screen), nothing paints twice (no overdraw added for screens that were already opaque), the tests fail on the old code and pass now (run them through ${L} gradle), the phone evidence shows an opaque card at mid-gesture, and the back gesture's smoothness did not regress. Blocking means a real defect or missing proof. There is no pressure to find something. Do not modify anything.`, { label: `bugfix:review#${round}`, phase: 'Review', schema: REVIEW })
  if (!review || !review.blocking.length) break
  log(`review round ${round}: ${review.blocking.length} blocking`)
  r = await agent(`You are the engineer again. ${RULES} A separate reviewer found these blocking problems in your fix of the see-through back card: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each with proof (re-verify on the phone where it matters), update ${BUG}, and return.`, { label: `bugfix:fix#${round + 1}`, phase: 'Review', schema: STATUS })
  if (!r) throw new Error('fix round failed')
}
return { result: r, review, reviewClean: !!review && !review.blocking.length }
