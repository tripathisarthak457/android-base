export const meta = {
  name: 'staging-finish2',
  description: "Finish the staging consolidation from its requirements review (run wf_a767f315-51c was paused at 22:41 Oct 24 for the setup-only work): the matrix review, a revision if needed, and the report with APKs for the user",
  phases: [
    { title: 'Requirements', detail: 'the separate review of the requirements matrix, and a revision if it finds problems' },
    { title: 'Report', detail: 'new work branches from staging and the report for the user' },
  ],
}

// The rest of scripts/staging-finish.js after its three requirement auditors, with the same prompts. Its run stopped at
// the matrix review so the user's setup-only release (REQUIREMENTS 59) could have the quota; the finished results (fix
// round 2, re-review 3, the three matrix groups) are in staging/finish2-inputs.json, the earlier ones in finish-inputs.json.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const WT = `${OV}/wt/STAGING`
const SD = `${OV}/staging`
const INPUTS = `${SD}/finish-inputs.json`
const INPUTS2 = `${SD}/finish2-inputs.json`

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md §6 first if you have not. Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${SD}/logs/, polled with tail. One Gradle build at a time machine-wide (the lock enforces it). Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md, the template's AGENTS.md: no Material, AppTheme only, strings in resources); never weaken, skip or delete a test to get to green. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never rewrite history. PeriFlow (WP11) never enters staging. Tonight the setup-only work (REQUIREMENTS 59) has priority on Gradle and the phone: prefix every bin/locked command with LOCKED_PRIORITY=low. Phone 9a7c74f4: screenshots and recordings to /data/local/tmp, never /sdcard (it wakes Google Photos); screenrecord needs --size 720x1600; restore any device setting you change; judge smoothness only on release or benchmark builds.`

const TESTED = { type: 'object', properties: {
  state: { type: 'string', enum: ['green', 'red', 'partial'] },
  rows: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['check', 'result', 'evidence'] } },
  failures: { type: 'array', items: { type: 'string' } },
  artifacts: { type: 'array', items: { type: 'string' } },
  report_path: { type: 'string' },
}, required: ['state', 'rows', 'failures', 'artifacts', 'report_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }
const DONE = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  summary: { type: 'string' },
  staging_head: { type: 'string' },
  branches: { type: 'array', items: { type: 'string' } },
  report_path: { type: 'string' },
  apks: { type: 'array', items: { type: 'string' } },
  screenshots: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'summary', 'staging_head', 'branches', 'report_path', 'apks', 'screenshots'] }

const MATRIX = { type: 'object', properties: {
  rows: { type: 'array', items: { type: 'object', properties: {
    req: { type: 'string' }, verdict: { type: 'string', enum: ['met', 'partly', 'not-met', 'deferred'] },
    where: { type: 'string' }, proof: { type: 'string' }, best_way: { type: 'string' }, owner: { type: 'string' },
  }, required: ['req', 'verdict', 'where', 'proof', 'best_way', 'owner'] } },
  summary: { type: 'string' },
}, required: ['rows', 'summary'] }
const REQ_GROUPS = [
  { key: '1-15', text: 'items 1 to 15' },
  { key: '16-30', text: 'items 16 to 30' },
  { key: '31-46', text: 'items 31 to 46, items 47 to 58, and every unnumbered item (memory safety and the like)' },
]
const matrixPrompt = (g, extra) => `You are an Opus requirements auditor at the Google/AOSP bar. ${RULES} Do not modify code or branches.
For ${OV}/REQUIREMENTS.md ${g.text} (every sub-point counts as its own row): on staging in ${WT}, decide whether each is met (fully, with proof), partly, not-met, or deferred (only where the user deferred it, for example Compose Multiplatform and Swift after the Android release per item 36; cite it). For each row: where it lives (file:line on staging), the proof (a test that exercises it, a phone capture or perf number in ${SD}/TEST-REPORT.md, or a check you run now through the locks), and best_way: whether it is done the most optimised, best and smoothest way on the Redmi (budget phone, 120 Hz, the user's goal "light and smooth, the most optimised visual feel"); if not, the concrete better way and what it costs; "best" only with a reason. A requirement with no proof is partly at best. The perf limits are those of REQUIREMENTS 58 (the user, Oct 24). The coverage audit (${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md, its MATRIX-ROUTED and not-in-plan rows) lists sub-points no package implements yet (for example item 37's glass toggle, slider, segmented control, buttons, alert, toast and playground strips; item 34's shimmer proof; 38.5's performance audit; 23.8's AdaptiveAuditTest; 36's TalkBack proof): judge each of them explicitly. Owner: the staging-wp/<package> branch that should close the gap. Write your rows to ${SD}/REQUIREMENTS-MATRIX.md under the heading "Items ${g.key}" (create the file if needed; other auditors write the other groups).${extra}`

phase('Requirements')
const mreview = await agent(`You are a separate Opus reviewer of the requirements matrix ${SD}/REQUIREMENTS-MATRIX.md for staging in ${WT}. ${RULES} Do not modify anything. Check every "met" against its proof (open the test, the capture, the number; a "met" without real proof is blocking), every "deferred" against a real user decision, that no requirement or sub-point of ${OV}/REQUIREMENTS.md is missing (items 1 to 58 and the unnumbered ones; item 59 is the setup-only work on its own branch, not on staging, and is out of this matrix), and that best_way is honest about smoothness on the Redmi (a red perf row under REQUIREMENTS 58's limits cannot sit under "best"). The rows are the "matrix" entries of ${INPUTS2}. There is no pressure to find something.`, { label: 'staging2:requirements-review', phase: 'Requirements', schema: REVIEW })
let revised = []
if (mreview && mreview.blocking.length) {
  log(`requirements review: ${mreview.blocking.length} blocking`)
  revised = (await parallel(REQ_GROUPS.map(g => () => agent(matrixPrompt(g, `
This is a revision of your rows (your earlier rows are the "matrix" entries of ${INPUTS2}, group ${g.key}). A separate reviewer found: ${JSON.stringify(mreview.blocking)}. Fix the rows in your group that these concern (re-check the proof), rewrite your section and return all your rows.`), { label: `staging2:requirements-${g.key}#2`, phase: 'Requirements', schema: MATRIX })))).filter(Boolean)
}

phase('Report')
const done = await agent(`You finish the consolidation. ${RULES}
1. Cut a work branch from staging's head for every package that still has work and does not have one yet (read the handoffs, ${SD}/CARRY-OVER.md and the release plan ${OV}/plans/2026-10-20/release-pipeline.md): git -C ${R} branch staging-wp/<package> staging. Several staging-wp/* branches already exist with fast-track and setup-only work on them (git -C ${R} branch --list 'staging-wp/*'): never move, reset or recreate those. Do not create worktrees and do not move or delete any old branch.
2. Write ${SD}/STAGING-REPORT.md for the user in plain English, readable on a phone: what is in staging (each package, one line, finished or how far along), what was tested and the results (the desktop matrix and the phone, with the perf gate table under REQUIREMENTS 58's limits and cold start), the APKs to install with sha256 (build them fresh from staging's head through the locks if ${OV}/out/share/ has none from it: the everything preset's release-like build and the catalog app, named android-base-staging-<what>-<short head>.apk with a sha256 file each), the screenshots and the tour recording, what is rough or unfinished and on which branch it continues, what the review found and what was fixed, and what still needs the user (as clear questions with options; the four first-frame rows are waiting on the user's choice between rebaselining after a cool-phone run and keeping the old limits: say so with the numbers from fix round 2).
3. The requirements: put the matrix's counts in the report and, in plain English, every requirement not fully met or not met the smoothest way, with its owner and the better way; write ${SD}/REQUIREMENTS-GAPS.md grouped by staging-wp branch (each gap with its proof and the better way), and make sure every owner in it has a branch after step 1.
4. Route every FORGOTTEN item of ${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md to its suggested owner, so nothing in it is lost: under that staging-wp branch in ${SD}/REQUIREMENTS-GAPS.md, or in ${SD}/RELEASE-PLAN-NOTES.md for what belongs to the release run (the one-time template reformat after the last UI wave, tools/size_budgets.toml, the signed-in /admin check that needs the user); list them in the report under 'Routed from the coverage audit'.
5. Add a section at the top of ${OV}/STATUS.md naming staging's finished head and the staging-wp/* branches, and one PROGRESS.md line.
Inputs: ${INPUTS} (the plan, the merge, the verified desktop and phone tests, fix round 1, re-review 2) and ${INPUTS2} (fix round 2, re-review 3, the three matrix groups); the matrix review ${JSON.stringify(mreview)}; the revised rows ${JSON.stringify(revised)} (empty if no revision was needed).`, { label: 'staging2:report', phase: 'Report', schema: DONE })
return { mreview, revised: revised.length, done }
