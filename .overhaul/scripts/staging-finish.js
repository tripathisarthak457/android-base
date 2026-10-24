export const meta = {
  name: 'staging-finish',
  description: "Finish the staging consolidation from fix round 2 (run wf_8be4c97d-b05 can no longer resume from its cache): the last fix round under REQUIREMENTS 58, the re-review, the requirements matrix and the report for the user",
  phases: [
    { title: 'Fix', detail: 'fix round 2 of 2 on staging, with the noise-aware gates of REQUIREMENTS 58' },
    { title: 'Review', detail: 'Google/AOSP-level re-review of the combined code by area' },
    { title: 'Requirements', detail: 'every requirement checked on staging: fully met, and met the most optimised, smoothest way' },
    { title: 'Report', detail: 'new work branches from staging and the report for the user' },
  ],
}

// The rest of scripts/consolidate-staging.js after its fix round 1 and re-review 2, with the same prompts. Its run
// (wf_8be4c97d-b05) stopped mid fix round 2 at 09:49 Oct 24; at 16:37 a resume by run id no longer matched its cached
// results past the merge (identical prompts, new cache keys) and re-ran the Oct 20 test phase. The finished results
// that this script continues from were copied out of that run's journal into staging/finish-inputs.json.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const WT = `${OV}/wt/STAGING`
const SD = `${OV}/staging`
const INPUTS = `${SD}/finish-inputs.json`

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md §6 first if you have not. Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${SD}/logs/, polled with tail. One Gradle build at a time machine-wide (the lock enforces it). Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md, the template's AGENTS.md: no Material, AppTheme only, strings in resources); never weaken, skip or delete a test to get to green. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never rewrite history. PeriFlow (WP11) never enters staging. Phone 9a7c74f4: screenshots and recordings to /data/local/tmp, never /sdcard (it wakes Google Photos); screenrecord needs --size 720x1600; restore any device setting you change; judge smoothness only on release or benchmark builds.`

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

phase('Fix')
log('fix round 2 (resumed): 4 blocking from re-review 2, desktop and phone partial after fix round 1')
const fix = await agent(`You fix the combined branch staging in ${WT}. ${RULES}
Blocking review findings: the "blocking" entries of the three reviews under "reviews2" in ${INPUTS} (re-review 2 at 7230b666: the release gate's scroll, navigation-stress and first-frame rows; the website's glass claim in BuildStep.tsx).
Test failures: "fix1.failures" in ${INPUTS} (fix round 1's report; its rows are under "fix1.rows").
KNOWN BEFORE STAGING (bug run, Oct 19 23:00; bugs/BUG-2026-10-19-back-see-through.md Result and bugs/2026-10-19-back-licenses/verify/proof-1/): the combined preview tree (WP04 + T + C1 + WP06 + WP03 + HUD, 22992d3c/722ced37) regresses the back gesture against T + C1 alone on REQUIREMENTS 42's release rows, confirmed by a same-session A/B control: hitch 12.7 against 6.9 ms/s (limit 5.0), janky after the first frame 1.2-1.5 % against 0.3-0.4 % (limit 1.0), the gesture's first three frames 4.75-5.33 against 2.1-2.2 ms (ratchet 2.7); most hitch time is in the push half, most jank in the gesture half. First suspect, unverified: app/build.gradle.kts adds :core:perfhud as devImplementation, so devBenchmarkRelease measures with the HUD's JankStats listener and DEV badge running (measured builds must not carry dev tooling, or must measure it explicitly). Otherwise bisect the merges WP06 (ebb1ca15), WP03 and its port (e0f443b8, fe33d475), HUD and its port (aaab6fe4, 22992d3c). This is blocking for staging: find the cause with traces, fix it at its root, and prove the rows back at T + C1's level. Fix each at its root on staging, one commit per cause, with a regression test where the cause is code; re-run exactly the checks each fix touches (through the locks) and update ${SD}/TEST-REPORT.md and ${SD}/MERGE-NOTES.md. A failure that is a known open point of one package from before the merge (its handoff says so) and not made worse by the combination: record it in ${SD}/CARRY-OVER.md for that package's new branch instead of fixing it here. Return what you did as a test report (rows for every check you re-ran).
THIS IS FIX ROUND 2 OF 2, RESTARTED. Read the orchestrator notes at the end of ${SD}/MERGE-NOTES.md first (Oct 24 08:03, 09:21 and 09:49); they are the user's decisions and they win over the text above where they differ:
- REQUIREMENTS 58 (the user, Oct 24 09:49; ${OV}/REQUIREMENTS.md item 58): change templates/android/tools/perf/gates.toml as the 09:49 note says, in one commit citing REQUIREMENTS 58, document the noise rule where the gates are explained, then STOP chasing the scroll and navigation-stress difference against C1 (record it in ${SD}/CARRY-OVER.md as a post-release optimisation item with the evidence so far). The back-gesture text above is history: push/pop and the gesture's release rows pass since 09a66b81 and 86bc23b5.
- The five non-regression first-frame rows keep their old limits (the user, 08:03): work the composing cost; if they still miss at the end of this round, record the fresh numbers in ${SD}/TEST-REPORT.md and say so in your report (the orchestrator asks the user again). Never loosen them.
- The first fix round 2 agent ran 06:35-09:49 Oct 24 and was stopped by a usage pause. It committed 4760b407 (the website's glass claim) and left uncommitted work in ${WT}: templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/content/AppSurface.kt (+95/-1, an outlineUnderContent / drawWithCache path) and an untracked AppSurfaceTest.kt beside it (written 08:41-08:42). It is the previous agent's work in progress, not yours to trust: read the diff, decide on evidence whether it fixes a blocking row the right way, then finish it with its test, or save it as a patch under ${SD}/fix/ and restore the file with git checkout -- <path> (never stash, reset or clean), and record which in ${SD}/MERGE-NOTES.md.
- Ignore ${SD}/r2-tree, ${SD}/r2-glass-tree, ${SD}/gen/r2-*, ${SD}/logs/phone/r2 and ${SD}/scripts/r2: a broken resume at 16:37-17:20 Oct 24 made them by replaying the Oct 20 test phase at b21f0a98; they are not evidence for staging's head.
- At the end, run the release gate once at the new values on staging's head (perf.sh --runs 3 --gate solid, exactly as fix round 1 ran it, through the locks) and put its table in ${SD}/TEST-REPORT.md.`, { label: 'staging:fix#2', phase: 'Fix', schema: TESTED })
if (!fix) throw new Error('fix round failed')

phase('Review')
const AREAS = [
  { key: 'generator', text: 'the generator, the CLI, the website, the API, CI (ci.yml) and the host code (WIN, WP02, WP10 parts, scaffold.py)' },
  { key: 'android-ui', text: 'the Android template UI: design system and components (WP03), navigation shell and the back card (WP04), the glass components (WP05), WP06, the catalog app, and how their merge resolutions fit together; also check two security holes the coverage audit found unrouted (handoff/WP04.md:1393-1397): the app lock reopening unlocked when a link or Recents arrives within 30 s of LockActivity (H-4) and DeepLink.from not checking the host of an https link against deeplink_host while MainActivity is exported (H-5); if either is present on staging it is blocking (security first)' },
  { key: 'perf-multiplatform', text: 'performance and tooling (WP04-perf harness, gates.toml, the performance overlay HUD and its prod absence, and its absence from the builds the perf harness measures; look for merge regressions in performance: code a merge resolution put on a per-frame path, a modifier or listener that now runs twice, a remembered value lost, a recomposition the tests of the owner had ruled out) and the multiplatform and iOS work (WP07, WP08)' },
]
const reviews = (await parallel(AREAS.map(a => () => agent(`You are a separate Google/AOSP-level reviewer of the combined branch staging in ${WT}, area: ${a.text}. ${RULES} Do not modify anything.
Read ${SD}/STAGING-PLAN.md, ${SD}/MERGE-NOTES.md and ${SD}/TEST-REPORT.md. Review every merge resolution and repair commit in your area (git log --merges and the repair commits on staging; git diff <package head> staging -- <paths> shows what the merge changed against each package), how the packages interact in your area (duplicate or conflicting implementations, a fix one package made that another package's code undoes, dead code left by a resolution), and the test failures in your area. Blocking means a real defect, a lost feature or test, a broken build or a failed release gate caused by the combination; open points a package already had before the merge are nonblocking unless the merge made them worse (they move to that package's new branch). The release gate's limits are those of REQUIREMENTS 58 (${OV}/REQUIREMENTS.md item 58, the user's decision of Oct 24): judge the gate against them, and the remaining scroll difference against C1 is a post-release item, not blocking. There is no pressure to find something. This is a re-review after fixes: check the earlier blocking findings (the "reviews2" entries in ${INPUTS}) are fixed and report only what remains or is new.`, { label: `staging:review-${a.key}#3`, phase: 'Review', schema: REVIEW })))).filter(Boolean)
const blocking = reviews.flatMap(r => r.blocking)
log(`re-review 3: ${blocking.length} blocking`)

phase('Requirements')
// The user, Oct 19 19:56: "make sure requirements are fully fulfilled in their most optimises best and smoothest way"
// (REQUIREMENTS 46). Judgments, so Opus throughout.
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
let matrix = (await parallel(REQ_GROUPS.map(g => () => agent(matrixPrompt(g, ''), { label: `staging:requirements-${g.key}`, phase: 'Requirements', schema: MATRIX })))).filter(Boolean)
const mreview = await agent(`You are a separate Opus reviewer of the requirements matrix ${SD}/REQUIREMENTS-MATRIX.md for staging in ${WT}. ${RULES} Do not modify anything. Check every "met" against its proof (open the test, the capture, the number; a "met" without real proof is blocking), every "deferred" against a real user decision, that no requirement or sub-point of ${OV}/REQUIREMENTS.md is missing, and that best_way is honest about smoothness on the Redmi (a red perf row under REQUIREMENTS 58's limits cannot sit under "best"). The rows: ${JSON.stringify(matrix.map(m => m.rows))}. There is no pressure to find something.`, { label: 'staging:requirements-review', phase: 'Requirements', schema: REVIEW })
if (mreview && mreview.blocking.length) {
  log(`requirements review: ${mreview.blocking.length} blocking`)
  matrix = (await parallel(REQ_GROUPS.map(g => () => agent(matrixPrompt(g, `\nThis is a revision of your rows. A separate reviewer found: ${JSON.stringify(mreview.blocking)}. Fix the rows in your group that these concern (re-check the proof), rewrite your section and return all your rows.`), { label: `staging:requirements-${g.key}#2`, phase: 'Requirements', schema: MATRIX })))).filter(Boolean)
}
const reqRows = matrix.flatMap(m => m.rows)
const reqGaps = reqRows.filter(r => r.verdict === 'partly' || r.verdict === 'not-met')
log(`requirements: ${reqRows.filter(r => r.verdict === 'met').length} met, ${reqGaps.length} with gaps, ${reqRows.filter(r => r.verdict === 'deferred').length} deferred`)

phase('Report')
const done = await agent(`You finish the consolidation. ${RULES}
1. Cut a work branch from staging's head for every package that still has work and does not have one yet (read the handoffs, ${SD}/CARRY-OVER.md and the release plan ${OV}/plans/2026-10-20/release-pipeline.md): git -C ${R} branch staging-wp/<package> staging. Several staging-wp/* branches already exist with fast-track work on them (git -C ${R} branch --list 'staging-wp/*'): never move, reset or recreate those. Do not create worktrees and do not move or delete any old branch.
2. Write ${SD}/STAGING-REPORT.md for the user in plain English, readable on a phone: what is in staging (each package, one line, finished or how far along), what was tested and the results (the desktop matrix and the phone, with the perf gate table under REQUIREMENTS 58's limits and cold start), the APKs to install with sha256 (build them fresh from staging's head through the locks if ${OV}/out/share/ has none from it: the everything preset's release-like build and the catalog app, named android-base-staging-<what>-<short head>.apk with a sha256 file each), the screenshots and the tour recording, what is rough or unfinished and on which branch it continues, what the review found and what was fixed, and what still needs the user (as clear questions with options).
3. The requirements: put the matrix's counts in the report and, in plain English, every requirement not fully met or not met the smoothest way, with its owner and the better way; write ${SD}/REQUIREMENTS-GAPS.md grouped by staging-wp branch (each gap with its proof and the better way), and make sure every owner in it has a branch after step 1.
4. Route every FORGOTTEN item of ${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md to its suggested owner, so nothing in it is lost: under that staging-wp branch in ${SD}/REQUIREMENTS-GAPS.md, or in ${SD}/RELEASE-PLAN-NOTES.md for what belongs to the release run (the one-time template reformat after the last UI wave, tools/size_budgets.toml, the signed-in /admin check that needs the user); list them in the report under 'Routed from the coverage audit'.
5. Add a section at the top of ${OV}/STATUS.md naming staging's finished head and the staging-wp/* branches, and one PROGRESS.md line.
Inputs: ${INPUTS} (the plan, the merge, the verified desktop and phone tests, fix round 1, re-review 2); fix round 2 ${JSON.stringify(fix)}; open blocking review findings ${JSON.stringify(blocking)}; nonblocking ${JSON.stringify(reviews.flatMap(r => r.nonblocking))}; requirement gaps ${JSON.stringify(reqGaps)}; matrix review ${JSON.stringify(mreview)}.`, { label: 'staging:report', phase: 'Report', schema: DONE })
return { fix, openBlocking: blocking, requirements: { met: reqRows.filter(r => r.verdict === 'met').length, gaps: reqGaps.length, deferred: reqRows.filter(r => r.verdict === 'deferred').length }, done }
