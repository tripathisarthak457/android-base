export const meta = {
  name: 'bugfix-back-see-through',
  description: "Fix the bug the user reported on Oct 19: during a back swipe the pushed screen's card is see-through, so the page beneath shows through it. Combine the newest code, capture the defect on the phone, root cause, fix with regression tests, capture the proof, verify, then a fresh review",
  phases: [
    { title: 'Reproduce', detail: 'finish the combined tree, write the capture procedure, capture the defect on three builds' },
    { title: 'Fix', detail: 'root cause, fix with tests on the owning branch' },
    { title: 'Prove', detail: 'capture the fixed build and its smoothness, then an Opus verifier writes the result' },
    { title: 'Review', detail: 'fresh-context review, then fixes and a new proof' },
  ],
}

// The user, Oct 19 15:41: "THE BACK GLICTH IS STILL THERE SEE I CLICKEC ON OPEN SOURCE LICENSES ... CHECK THE ISSUE". Their
// standing rule: a bug they report comes first, so this runs alone, before the staging consolidation.
// Model routing (global CLAUDE.md §10): the phone builds and captures follow a fixed procedure, so a Sonnet operator runs
// them and returns facts with evidence paths; every merge, root cause, code change, visual judgment and review is Opus.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const BUG = `${OV}/bugs/BUG-2026-10-19-back-see-through.md`
const BD = `${OV}/bugs/2026-10-19-back-licenses`
const PROC = `${BD}/CAPTURE-PROCEDURE.md`
const PV = `${OV}/wt/_preview`
const PB = 'preview/catalog-2026-10-19'

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) first. Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${BD}/logs/, polled. Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md; the template's AGENTS.md: no Material, AppTheme only); never weaken, skip or delete a test. Commit each step with a message that says why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. Never push, never touch main or overhaul/multiplatform-v2, never delete a branch. Phone 9a7c74f4: screenshots and recordings to /data/local/tmp, never /sdcard; screenrecord needs --size 720x1600; restore any device setting you change; judge on release-like builds. Only this project's test apps may be installed or uninstalled; never touch the user's own apps.`
const OPERATOR = `You are a careful operator. Follow ${PROC} exactly and report facts only: what you ran, the exit code, the log path and its last lines, the files you produced. Do not judge what the images show, do not edit code, do not commit, do not fix anything; if a step fails, record it and go on to the next.`

const COMBINED = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  preview_head: { type: 'string' },
  merged: { type: 'array', items: { type: 'string' } },
  conflicts: { type: 'array', items: { type: 'string' } },
  builds: { type: 'array', items: { type: 'object', properties: { name: { type: 'string' }, tree: { type: 'string' }, head: { type: 'string' }, app_id: { type: 'string' } }, required: ['name', 'tree', 'head', 'app_id'] } },
  procedure_path: { type: 'string' },
  notes_path: { type: 'string' },
}, required: ['state', 'preview_head', 'merged', 'conflicts', 'builds', 'procedure_path', 'notes_path'] }
const RAW = { type: 'object', properties: {
  runs: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, command: { type: 'string' }, exit_code: { type: 'integer' }, log: { type: 'string' }, last_lines: { type: 'string' } }, required: ['check', 'command', 'exit_code', 'log', 'last_lines'] } },
  artifacts: { type: 'array', items: { type: 'string' } },
  problems: { type: 'array', items: { type: 'string' } },
}, required: ['runs', 'artifacts', 'problems'] }
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

phase('Reproduce')
const combined = await agent(`You prepare the reproduction of a bug the user reported, which comes before everything else. ${RULES}
Read ${BUG} first (the user's words; look at the screenshot ${BD}/user-screenshot.png; the suspects). The app the user ran was built from wp/HUD (WP06 + the overlay), without WP04's navigation rewrite, so the first question is whether the newest code has the defect.
1. Finish the newest combined code: the worktree ${PV} on the local branch ${PB} already holds wp/WP04, the back card T + C1 patches and wp/WP06 (git log). Merge wp/WP03 (the design system with the scroll-direction fix) and wp/HUD (the overlay) there with git merge --no-ff, resolving conflicts by reading both sides and the owning handoffs (${OV}/handoff/*.md); the newer fix wins, never drop a feature or a test. Record each resolution (file, what each side did, what you kept and why) in ${OV}/out/preview/NOTES.md. Compile what the merges touched through ${L} gradle. That branch is local and never pushed.
2. Write ${PROC}, a procedure a Sonnet operator can follow without judgment, for three builds: (a) the combined tree ${PV}, (b) wp/WP04 alone (worktree ${OV}/wt/WP04), (c) wp/HUD alone (worktree ${OV}/wt/HUD). For each: the exact commands to generate the everything preset and build a release-like variant (devRelease or the benchmark variant WP04's perf harness uses), the APK path, the application id, the install command through ${L} adb. Then the scenes, with exact adb commands: Settings, Open source licences, a back swipe from the left edge held mid-way (adb shell input motionevent DOWN/MOVE without UP, a screencap while held, then MOVE back and UP to cancel, then a full swipe to commit; find how WP04's acceptance and perf tooling already inject and hold the gesture, handoff/WP04.md and handoff/WP04-perf.md, and use that), the same on a deep pushed screen with a long scrolled list, with a sheet open, and on a cover; a screenrecord of the licences scene (--size 720x1600). Name every output file (${BD}/shots/<build>-<scene>-<moment>.png, ${BD}/rec/<build>-licences.mp4), the logcat -b crash pull, and at the end: uninstall builds (b) and (c) and leave (a) installed. Also a section "Proof run" for later: the same scenes on the fixed combined build into ${BD}/shots/fixed-*.png, plus the back gesture's smoothness through the WP04 perf harness (pushpop_gesture, exactly as ${OV}/out/WP04-perf/back-google/RESULT.md ran it for T + C1, same repetitions) with its summary files under ${BD}/perf/.
3. Check the procedure works by running its first scene once on build (a) yourself (one held frame is enough) and fix the procedure until it does.
Return the preview head, what you merged, conflicts, the three builds and the procedure path.`, { label: 'bugfix:combine', phase: 'Reproduce', schema: COMBINED })
if (!combined) throw new Error('combine failed')

const before = await agent(`${OPERATOR} ${RULES}
Run every step of ${PROC} except the "Proof run" section, for the three builds ${JSON.stringify(combined.builds)}. Logs under ${BD}/logs/capture-before/.`, { label: 'bugfix:capture-before', phase: 'Reproduce', schema: RAW, model: 'sonnet' })
if (!before) throw new Error('capture failed')

phase('Fix')
let r = await agent(`You fix a bug the user reported, which comes before everything else. ${RULES}
Read ${BUG} (the user's words; look at ${BD}/user-screenshot.png). The combined tree is ready: ${JSON.stringify(combined)}. A Sonnet operator captured the three builds following ${PROC}; trust nothing until you check it. Its report: ${JSON.stringify(before)}
1. Verify the capture: open every held-frame screenshot and a few frames of each recording (ffmpeg), and decide for each build and scene whether the card is opaque or double-exposed. Re-take any capture that is missing or wrong yourself. Record the table in ${BUG} under "## Reproduction".
2. Root cause with file:line (the screen not painting an opaque background and relying on the host or window, the card layer drawing content without the background, alpha applied to the card during the drag, the scrim missing, a transparent window background, or something else), and explain why WP04's acceptance row "one opaque card, no double exposure" did not catch it. Quick targeted phone checks while debugging are yours.
3. Fix it at its root on the branch that owns the cause, in that branch's worktree under ${OV}/wt/ (wp/WP04 for the shell and the card, wp/WP03 for AppScaffold and the surfaces, wp/WP06 for the settings screens; if the cause spans branches, fix each in its own commit). Regression tests that fail before and pass after: at the unit level (for example a layer-probe or screenshot-style test that the card is opaque over a page with distinct content), plus a phone check row added to the owning acceptance (the card's pixels over a contrasting page stay the card's colour at mid-gesture). Run the owning module's tests through ${L} gradle.
4. Merge each fixed branch again into ${PB} in ${PV} so the combined build carries the fix, and build it as the procedure says, ready for the proof run.
If the newest combined code does not have the defect (only the older builds do), say so with the evidence (state not-reproduced), still add the regression test and the acceptance row that would have caught it, and explain which change removed it.`, { label: 'bugfix:fix', phase: 'Fix', schema: STATUS })
if (!r) throw new Error('bug fix agent failed')

const prove = async (round) => {
  const raw = await agent(`${OPERATOR} ${RULES}
Run the "Proof run" section of ${PROC} on the fixed combined build in ${PV} (branch ${PB}, now at the fix: ${JSON.stringify(r.commits)}). Logs under ${BD}/logs/capture-proof-${round}/.`, { label: `bugfix:capture-proof#${round}`, phase: 'Prove', schema: RAW, model: 'sonnet' })
  if (!raw) throw new Error('proof capture failed')
  const v = await agent(`You are the Opus verifier of a fix for the user's see-through back card. A Sonnet operator captured the proof; trust nothing until you check it. The fix: ${JSON.stringify(r)}. The operator's report: ${JSON.stringify(raw)}. ${RULES}
Open every fixed-* held frame next to its before frame (${BD}/shots/) and a few frames of the recording: the card must be one opaque card over a dimmed page in every scene, no double exposure anywhere. Re-derive the smoothness numbers from the perf harness's raw files under ${BD}/perf/ and compare them with T + C1's in ${OV}/out/WP04-perf/back-google/RESULT.md (the hitch ratio and jank after the first frame are the release limits, REQUIREMENTS 42; a regression is a failure). Re-take anything missing or ambiguous yourself. Do not edit code. Write "## Result" in ${BUG} (which build the user saw, whether the newest code had it, root cause with file:line, fix commits, tests, the before and after evidence table, the smoothness comparison) and an orchestrator-visible note at the top of the owning handoff's "## Progress". Return the fix's status with your verified evidence; state blocked if the proof fails, with why in root_cause.`, { label: `bugfix:verify#${round}`, phase: 'Prove', schema: STATUS })
  if (!v) throw new Error('verifier failed')
  return v
}

phase('Prove')
r = await prove(1)

phase('Review')
let review = null
for (let round = 1; round <= 3 && r.state !== 'blocked'; round++) {
  review = await agent(`You are a separate reviewer at Google/AOSP level. Review the work on the user's see-through back card: ${JSON.stringify(r)}. Read ${BUG} and the commits (git show each). Check the root cause is real and explains the user's screenshot, the fix holds for every pushed screen, sheet and cover (not only the licences screen), nothing paints twice (no overdraw added for screens that were already opaque), the tests fail on the old code and pass now (run them through ${L} gradle), the phone evidence shows an opaque card at mid-gesture (open the images), and the back gesture's smoothness did not regress. Blocking means a real defect or missing proof. There is no pressure to find something. Do not modify anything.`, { label: `bugfix:review#${round}`, phase: 'Review', schema: REVIEW })
  if (!review || !review.blocking.length) break
  log(`review round ${round}: ${review.blocking.length} blocking`)
  const fixed = await agent(`You are the engineer again. ${RULES} A separate reviewer found these blocking problems in the fix of the see-through back card: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. The current state: ${JSON.stringify(r)}. Fix each at its root with tests, merge the fixed branch again into ${PB} in ${PV} and build it as ${PROC} says, update ${BUG}, and return.`, { label: `bugfix:fix#${round + 1}`, phase: 'Review', schema: STATUS })
  if (!fixed) throw new Error('fix round failed')
  r = fixed
  r = await prove(round + 1)
}
return { combined, result: r, review, reviewClean: !!review && !review.blocking.length }
