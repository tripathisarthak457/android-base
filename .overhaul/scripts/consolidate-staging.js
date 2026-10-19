export const meta = {
  name: 'consolidate-staging',
  description: "The user's first target (Oct 19): every package's work merged into one new branch, staging, then the combined app built, tested on the phone and reviewed as a whole, then new work branches cut from staging, with a report for the user before any new work continues",
  phases: [
    { title: 'Prepare', detail: 'inventory every branch, clean stop points, the merge plan' },
    { title: 'Merge', detail: 'staging from the integration branch, every package merged in order' },
    { title: 'Test', detail: 'the whole matrix on the desktop and the combined app on the phone' },
    { title: 'Review', detail: 'Google/AOSP-level review of the combined code by area' },
    { title: 'Fix', detail: 'blocking findings fixed on staging, re-tested, re-reviewed' },
    { title: 'Report', detail: 'new work branches from staging and the report for the user' },
  ],
}

// The user, Oct 19 15:50: "FIRST TARGET IN NEXT SESSION IS TO FINISH UP WORK AND MERGE ALL CODE TO A SINGLE BRANCH "STAGING" U
// NEED TO CREATED AND THEN CREATE WP AND OTHER BRANCHES FOR THAT STAGING TO CONTINUE WORK, I WANNA SEE ALL CHANGES COMBINED AND
// TESTED AND THEN ONLY NEXT WORK CONTINUES". Their choices (asked with pros and cons): every package with work goes in
// (PeriFlow never); one combined review replaces the per-package reviews still open; staging is pushed as a backup by the
// orchestrator; the old branches stay as history and new work branches are cut from staging (staging-wp/<package>).
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const WT = `${OV}/wt/STAGING`
const SD = `${OV}/staging`

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md §6 first if you have not. Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${SD}/logs/, polled with tail. One Gradle build at a time machine-wide (the lock enforces it). Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md, the template's AGENTS.md: no Material, AppTheme only, strings in resources); never weaken, skip or delete a test to get to green. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never rewrite history. PeriFlow (WP11) never enters staging. Phone 9a7c74f4: screenshots and recordings to /data/local/tmp, never /sdcard (it wakes Google Photos); screenrecord needs --size 720x1600; restore any device setting you change; judge smoothness only on release or benchmark builds.`

const PLAN = { type: 'object', properties: {
  order: { type: 'array', items: { type: 'object', properties: { branch: { type: 'string' }, head: { type: 'string' }, why: { type: 'string' }, before_merge: { type: 'string' } }, required: ['branch', 'head', 'why', 'before_merge'] } },
  excluded: { type: 'array', items: { type: 'string' } },
  wip_commits: { type: 'array', items: { type: 'string' } },
  risks: { type: 'array', items: { type: 'string' } },
  plan_path: { type: 'string' },
}, required: ['order', 'excluded', 'wip_commits', 'risks', 'plan_path'] }
const MERGED = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  staging_head: { type: 'string' },
  merged: { type: 'array', items: { type: 'string' } },
  not_merged: { type: 'array', items: { type: 'string' } },
  conflicts: { type: 'array', items: { type: 'string' } },
  notes_path: { type: 'string' },
}, required: ['state', 'staging_head', 'merged', 'not_merged', 'conflicts', 'notes_path'] }
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

phase('Prepare')
const plan = await agent(`You prepare the consolidation of every package into one branch, staging. ${RULES}
1. Inventory, read-only first: git -C ${R} branch --list 'wp/*' 'overhaul/*' 'preview/*' 'ci/*' with heads; for each wp/* branch: commits ahead of overhaul/multiplatform-v2, whether it is already contained, and which branches contain which (git merge-base --is-ancestor), so superseded branches are not merged twice (wp/WP03-skel vs wp/WP03, wp/WP05 vs wp/WP05-glass, wp/HUD-statekeys vs wp/HUD, wp/WP04-perf vs wp/WP04). Read each package's handoff in ${OV}/handoff/ (Progress top, Open issues) and STATUS.md §1 for what is finished, what is mid-review and what is unfinished. Read ${OV}/out/preview/NOTES.md if the preview build (scripts/catalog-preview.js) wrote one: its conflict resolutions for WP04, WP06, WP03 and HUD are a starting point.
2. Clean stop points: for every worktree under ${OV}/wt/ except _workspace-branch, _preview and STAGING, if git status is not clean, commit the work in progress on that worktree's own branch with a message starting "WIP (staging stop point):" that says what is in it, after checking it builds or at least that no secret or private file is in it (keystores, local.properties, google-services.json, logcat or usagestats dumps; leave those uncommitted and say so).
3. Decide the merge order and what goes in. Everything with work goes in (the user's choice): WIN (with its two Linux fixes), WP02 (generator), WP03 (design system, the scroll-direction fix, iOS scrolling), WP04 (navigation shell) with the back card ${OV}/out/WP04-perf/backcard-texture.patch then ${OV}/out/WP04-perf/back-google/c1.patch applied if wp/WP04 does not contain them (REQUIREMENTS 42; handoff/WP04.md 14:50 note), WP04-perf's harness and tools, WP05 and the glass branch (wp/WP05-glass carries the old rubber band: WP03's daafdbd and 91cb638 must win in AppOverscroll.kt), WP06, WP07, WP08, HUD (and HUD-statekeys only if HUD's handoff says it belongs). Exclude only PeriFlow and branches fully contained in another. Order: dependencies first (the order the packages were built on each other; the release script ${OV}/scripts/to-release.js lists starts), the smallest-conflict path, Android before multiplatform.
4. Write ${SD}/STAGING-PLAN.md: the inventory, the order with heads and reasons, what each merge must take care of (before_merge), exclusions with reasons, the WIP commits, the risks. Return it.`, { label: 'staging:prepare', phase: 'Prepare', schema: PLAN })
if (!plan) throw new Error('prepare failed')

phase('Merge')
const merged = await agent(`You merge every package into a new branch, staging. ${RULES}
The plan: ${JSON.stringify(plan)}
1. git -C ${R} worktree add ${WT} -b staging overhaul/multiplatform-v2 (reuse it if it exists from an interrupted attempt and continue where it stopped; read ${SD}/MERGE-NOTES.md).
2. Merge each branch in the plan's order with git merge --no-ff, doing each before_merge step first (the back-card patches with git apply from the repository root and a commit of their own). Resolve every conflict by reading both sides, the owning handoffs and REQUIREMENTS; the newer fix wins over older code (for example AppOverscroll.kt from WP03's daafdbd/91cb638); never drop a feature or a test to make a conflict go away. After each merge, compile what it touched (the template's affected modules through ${L} gradle, the generator's tests for generator merges through memwait) so a breakage is found at the merge that caused it; fix it in a separate commit on staging that says which merge it repairs.
3. Record in ${SD}/MERGE-NOTES.md, per merge: the branch and head, the conflicts (file, what each side did, what you kept and why), the repair commits, what you compiled. If a branch cannot be merged soundly, leave it out, say exactly why in the notes and in not_merged; do not guess.
Return the staging head and everything above.`, { label: 'staging:merge', phase: 'Merge', schema: MERGED })
if (!merged) throw new Error('merge failed')

phase('Test')
// The user's working rules (Oct 18): Sonnet runs the mechanical, token-heavy part (commands, builds, installs, captures) and
// returns facts with evidence paths, never judgments; an Opus verifier re-derives the key results from the raw evidence and
// writes what the user reads; the area reviewers below are separate Opus agents.
const RAW = { type: 'object', properties: {
  runs: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, command: { type: 'string' }, exit_code: { type: 'integer' }, log: { type: 'string' }, last_lines: { type: 'string' } }, required: ['check', 'command', 'exit_code', 'log', 'last_lines'] } },
  artifacts: { type: 'array', items: { type: 'string' } },
  problems: { type: 'array', items: { type: 'string' } },
}, required: ['runs', 'artifacts', 'problems'] }
const OPERATOR = `You are a careful operator. Follow the steps exactly and report facts only: what you ran, the exit code, the log path and its last lines, the files you produced. Do not judge quality, do not edit code, do not commit, do not fix anything; if a step fails, record it and go on to the next.`
const DESK_STEPS = `Desktop matrix on staging (${merged.staging_head}) in ${WT}: the generator suite (python -m unittest discover -s tests -t . in generator/, through memwait); the consistency matrix (python -m tests.consistency --quick through memwait, then the full one if it finishes within about an hour); genkit.tokens --check and the catalogue check; the template's full build with tests and lint (./gradlew build --continue in templates/android through ${L} gradle, with timeout 90m); every preset generated with the CLI (none, lean, standard, all, everything) and built with check assembleDevDebug assembleProdRelease through the lock, one at a time; the website (npm ci and npm run build in web/frontend through memwait); the API (go vet ./..., go build ./..., go test ./... in api/). Logs under ${SD}/logs/desk/.`
const PHONE_STEPS = `On the phone, from staging (${merged.staging_head}) in ${WT}: build release-like APKs of the everything preset and of the template's catalog app (the module the user ran as com.example.wp03.look.catalog; ${OV}/handoff/WP03.md and ${OV}/out/WP03/look show how it is generated and built), plus a dev build that carries the performance overlay; copy them to ${OV}/out/share/ as android-base-staging-<what>-<short head>.apk with a sha256 file each. Install them on the Redmi. Capture to ${SD}/shots/ (pulled from /data/local/tmp): every catalog page; Settings, Open source licences, and a back swipe held mid-way on it (the user's Oct 19 bug); a list and a carousel mid-drag in both directions; a sheet open; tab switches; the overlay pill and its Performance tab on the dev build. One screenrecord tour of the catalog (--size 720x1600, under 60 s) to ${SD}/tour.mp4. Run the template's perf harness (templates/android/tools/perf/perf.sh exactly as ${OV}/handoff/WP04.md runs it, through the locks) on the everything preset's release build; keep its summary and gate table files. Cold start as the harness measures it. logcat -b crash after the session to ${SD}/logs/phone/crash.txt. Logs under ${SD}/logs/phone/.`
const [deskRaw, phoneRaw] = await parallel([
  () => agent(`${OPERATOR} ${RULES}\n${DESK_STEPS}`, { label: 'staging:run-desktop', phase: 'Test', schema: RAW, model: 'sonnet' }),
  () => agent(`${OPERATOR} ${RULES}\n${PHONE_STEPS}`, { label: 'staging:capture-phone', phase: 'Test', schema: RAW, model: 'sonnet' }),
])
if (!deskRaw || !phoneRaw) throw new Error('a test run failed')
const verifyPrompt = (half, raw, focus) => `You are the Opus verifier for the "${half}" half of staging's tests. A Sonnet operator ran it; trust nothing until you check it. Its report: ${JSON.stringify(raw)}. ${RULES}
Check every run against its log (open the logs, read the failures), re-run any check whose log is missing, cut short or ambiguous, and ${focus}. For each failure, find the merge it most likely comes from (git log, ${SD}/MERGE-NOTES.md). Do not fix code. Write your rows to ${SD}/TEST-REPORT.md under the heading "${half}" (create the file if needed; the other verifier writes the other half): each check, its result, its evidence path. Return the verified rows.`
const [desk, phone] = await parallel([
  () => agent(verifyPrompt('Desktop matrix', deskRaw, 'confirm the counts (tests run, failures, skips) from the logs yourself'), { label: 'staging:verify-desktop', phase: 'Test', schema: TESTED }),
  () => agent(verifyPrompt('On the phone', phoneRaw, 'open every screenshot and a few frames of the tour (ffmpeg) and judge them yourself (the back card must be opaque at mid-gesture, content must move with the finger, nothing clipped or overlapping), re-derive the perf gate table against tools/perf/gates.toml (REQUIREMENTS 42: hitch ratio and jank after the first frame are the release limits, the rest are ratchets) and cold start from the raw files, check every APK exists with a matching sha256 and was built from staging\'s head, and read the crash log'), { label: 'staging:verify-phone', phase: 'Test', schema: TESTED }),
])
if (!desk || !phone) throw new Error('a test verification failed')

phase('Review')
const AREAS = [
  { key: 'generator', text: 'the generator, the CLI, the website, the API, CI (ci.yml) and the host code (WIN, WP02, WP10 parts, scaffold.py)' },
  { key: 'android-ui', text: 'the Android template UI: design system and components (WP03), navigation shell and the back card (WP04), the glass components (WP05), WP06, the catalog app, and how their merge resolutions fit together' },
  { key: 'perf-multiplatform', text: 'performance and tooling (WP04-perf harness, gates.toml, the performance overlay HUD and its prod absence) and the multiplatform and iOS work (WP07, WP08)' },
]
const reviewOnce = (round) => parallel(AREAS.map(a => () => agent(`You are a separate Google/AOSP-level reviewer of the combined branch staging in ${WT}, area: ${a.text}. ${RULES} Do not modify anything.
Read ${SD}/STAGING-PLAN.md, ${SD}/MERGE-NOTES.md and ${SD}/TEST-REPORT.md. Review every merge resolution and repair commit in your area (git log --merges and the repair commits on staging; git diff <package head> staging -- <paths> shows what the merge changed against each package), how the packages interact in your area (duplicate or conflicting implementations, a fix one package made that another package's code undoes, dead code left by a resolution), and the test failures in your area. Blocking means a real defect, a lost feature or test, a broken build or a failed release gate caused by the combination; open points a package already had before the merge are nonblocking unless the merge made them worse (they move to that package's new branch). There is no pressure to find something.${round > 1 ? ' This is a re-review after fixes: check the earlier blocking findings are fixed and report only what remains or is new.' : ''}`, { label: `staging:review-${a.key}#${round}`, phase: 'Review', schema: REVIEW })))
let reviews = (await reviewOnce(1)).filter(Boolean)
let blocking = reviews.flatMap(r => r.blocking)

phase('Fix')
let tested = { desk, phone }
for (let round = 1; round <= 2 && (blocking.length || desk.state !== 'green' || phone.state !== 'green'); round++) {
  log(`fix round ${round}: ${blocking.length} blocking, desktop ${tested.desk.state}, phone ${tested.phone.state}`)
  const fix = await agent(`You fix the combined branch staging in ${WT}. ${RULES}
Blocking review findings: ${JSON.stringify(blocking)}
Test failures: desktop ${JSON.stringify(tested.desk.failures)}; phone ${JSON.stringify(tested.phone.failures)}
Fix each at its root on staging, one commit per cause, with a regression test where the cause is code; re-run exactly the checks each fix touches (through the locks) and update ${SD}/TEST-REPORT.md and ${SD}/MERGE-NOTES.md. A failure that is a known open point of one package from before the merge (its handoff says so) and not made worse by the combination: record it in ${SD}/CARRY-OVER.md for that package's new branch instead of fixing it here. Return what you did as a test report (rows for every check you re-ran).`, { label: `staging:fix#${round}`, phase: 'Fix', schema: TESTED })
  if (!fix) throw new Error('fix round failed')
  tested = { desk: { ...tested.desk, state: fix.state, failures: fix.failures }, phone: { ...tested.phone, state: fix.state, failures: fix.failures } }
  reviews = (await reviewOnce(round + 1)).filter(Boolean)
  blocking = reviews.flatMap(r => r.blocking)
}

phase('Report')
const done = await agent(`You finish the consolidation. ${RULES}
1. Cut a work branch from staging's head for every package that still has work (read the handoffs, ${SD}/CARRY-OVER.md and the release order in ${OV}/scripts/to-release.js): git -C ${R} branch staging-wp/<package> staging (for example staging-wp/WP04, staging-wp/WP05, staging-wp/HUD, staging-wp/NET, staging-wp/CAT ...). Do not create worktrees and do not move or delete any old branch.
2. Write ${SD}/STAGING-REPORT.md for the user in plain English, readable on a phone: what is in staging (each package, one line, finished or how far along), what was tested and the results (the desktop matrix and the phone, with the perf gate table and cold start), the APKs to install with sha256, the screenshots and the tour recording, what is rough or unfinished and on which new branch it continues, what the review found and what was fixed, and what still needs the user (as clear questions with options).
3. Add a section at the top of ${OV}/STATUS.md §1 naming staging as the new integration branch and the staging-wp/* branches, and one PROGRESS.md line.
Inputs: plan ${JSON.stringify(plan)}; merge ${JSON.stringify(merged)}; desktop tests ${JSON.stringify(tested.desk)}; phone tests ${JSON.stringify(tested.phone)}; open blocking review findings ${JSON.stringify(blocking)}; nonblocking ${JSON.stringify(reviews.flatMap(r => r.nonblocking))}.`, { label: 'staging:report', phase: 'Report', schema: DONE })
return { plan, merged, tested, openBlocking: blocking, done }
