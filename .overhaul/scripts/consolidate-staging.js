export const meta = {
  name: 'consolidate-staging',
  description: "The user's first target (Oct 19): every package's work merged into one new branch, staging, then the combined app built, tested on the phone and reviewed as a whole, then new work branches cut from staging, with a report for the user before any new work continues",
  phases: [
    { title: 'Prepare', detail: 'inventory every branch, clean stop points, the merge plan' },
    { title: 'Merge', detail: 'staging from the integration branch, every package merged in order' },
    { title: 'Test', detail: 'the whole matrix on the desktop and the combined app on the phone' },
    { title: 'Review', detail: 'Google/AOSP-level review of the combined code by area' },
    { title: 'Fix', detail: 'blocking findings fixed on staging, re-tested, re-reviewed' },
    { title: 'Requirements', detail: 'every requirement checked on staging: fully met, and met the most optimised, smoothest way' },
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
3. Add LICENSE at the repository root as its own commit on staging: the standard MIT License text with 'Copyright (c) 2026 Sarthak Tripathi' (the user, Oct 19 22:08); make every place that names the license (the website, README, generator metadata) match.
Also, approved by the user on Oct 19 22:16: apply the npm security upgrades in web/frontend as one commit on staging (next 15.5.27, postcss 8.5.23 for the direct pin and Next's copy, sharp past the libvips advisories; npm through memwait, cache D:\.caches\npm) and confirm npm audit is clean; the Gradle wrapper may download Gradle 9.8.1 into D:\.caches\gradle if the merged template asks for it.
4. Record in ${SD}/MERGE-NOTES.md, per merge: the branch and head, the conflicts (file, what each side did, what you kept and why), the repair commits, what you compiled. If a branch cannot be merged soundly, leave it out, say exactly why in the notes and in not_merged; do not guess.
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
const PHONE_STEPS = `On the phone, from staging (${merged.staging_head}) in ${WT}: build release-like APKs of the everything preset and of the template's catalog app (the module the user ran as com.example.wp03.look.catalog; ${OV}/handoff/WP03.md and ${OV}/out/WP03/look show how it is generated and built), plus a dev build that carries the performance overlay; copy them to ${OV}/out/share/ as android-base-staging-<what>-<short head>.apk with a sha256 file each. Install them on the Redmi. Capture to ${SD}/shots/ (pulled from /data/local/tmp): every catalog page; Settings, Open source licences, and a back swipe held mid-way on it (the user's Oct 19 bug); a list and a carousel mid-drag in both directions; a sheet open; tab switches; the overlay pill and its Performance tab on the dev build; the liquid glass bottom bar (the user asked about it on Oct 19 19:55): a build with the liquidglass feature and the Glass design style as ${OV}/handoff/WP05-glass.md §2.1 describes (the everything preset if it selects them, otherwise generate one that does), screenshots of the bar at rest over a busy scrolled list, mid-tap and mid-drag of the lens, a 10 s screenrecord of tab switches and a lens drag, and the perf harness's tab-switch scene on that build with its summary kept (log which glass tier ShellLog reports). One screenrecord tour of the catalog (--size 720x1600, under 60 s) to ${SD}/tour.mp4. Run the template's perf harness (templates/android/tools/perf/perf.sh exactly as ${OV}/handoff/WP04.md runs it, through the locks) on the everything preset's release build; keep its summary and gate table files. Cold start as the harness measures it. logcat -b crash after the session to ${SD}/logs/phone/crash.txt. Logs under ${SD}/logs/phone/.`
const [deskRaw, phoneRaw] = await parallel([
  () => agent(`${OPERATOR} ${RULES}\n${DESK_STEPS}`, { label: 'staging:run-desktop', phase: 'Test', schema: RAW, model: 'sonnet' }),
  () => agent(`${OPERATOR} ${RULES}\n${PHONE_STEPS}`, { label: 'staging:capture-phone', phase: 'Test', schema: RAW, model: 'sonnet' }),
])
if (!deskRaw || !phoneRaw) throw new Error('a test run failed')
const verifyPrompt = (half, raw, focus) => `You are the Opus verifier for the "${half}" half of staging's tests. A Sonnet operator ran it; trust nothing until you check it. Its report: ${JSON.stringify(raw)}. ${RULES}
Check every run against its log (open the logs, read the failures), re-run any check whose log is missing, cut short or ambiguous, and ${focus}. For each failure, find the merge it most likely comes from (git log, ${SD}/MERGE-NOTES.md). Do not fix code. Write your rows to ${SD}/TEST-REPORT.md under the heading "${half}" (create the file if needed; the other verifier writes the other half): each check, its result, its evidence path. Return the verified rows.`
const [desk, phone] = await parallel([
  () => agent(verifyPrompt('Desktop matrix', deskRaw, 'confirm the counts (tests run, failures, skips) from the logs yourself'), { label: 'staging:verify-desktop', phase: 'Test', schema: TESTED }),
  () => agent(verifyPrompt('On the phone', phoneRaw, 'open every screenshot and a few frames of the tour (ffmpeg) and judge them yourself (the back card must be opaque at mid-gesture, content must move with the finger, nothing clipped or overlapping), re-derive the perf gate table against tools/perf/gates.toml (and compare the back gesture rows with T + C1 in out/WP04-perf/back-google/RESULT.md: the bug run found the combined tree about twice as janky there, which is a failure here); then hunt merge regressions everywhere, not only there (the user, Oct 19 23:04: other issues may have come in with the merges): compare every perf scene and every visual check with the best result its owning package recorded before the merge (the push, back, tab and cold-start rows of WP04 in out/WP04-perf and handoff/WP04.md; the scroll, carousel, overscroll and shimmer rows of WP03 in handoff/WP03.md and out/WP03; the overhead and prod absence of the HUD in handoff/HUD.md; the screens of WP06 in handoff/WP06.md); a scene worse than the number of its owner beyond the run-to-run spread of the harness is a merge regression and a failure; where the spread is unclear, run the APK of the owning package in the same session, alternating A B A B as the control in the bug run did (bugs/2026-10-19-back-licenses/verify/proof-1/), and list each regression with the merge it most likely comes from (REQUIREMENTS 42: hitch ratio and jank after the first frame are the release limits, the rest are ratchets) and cold start from the raw files, check every APK exists with a matching sha256 and was built from staging\'s head, and read the crash log; for the liquid glass bar, say from the images and frames whether the blur, rim light and lens show as WP05-glass.md §2.1 describes, which tier ran, and what the frame numbers of the glass build are against the same scene without glass'), { label: 'staging:verify-phone', phase: 'Test', schema: TESTED }),
])
if (!desk || !phone) throw new Error('a test verification failed')

phase('Review')
const AREAS = [
  { key: 'generator', text: 'the generator, the CLI, the website, the API, CI (ci.yml) and the host code (WIN, WP02, WP10 parts, scaffold.py)' },
  { key: 'android-ui', text: 'the Android template UI: design system and components (WP03), navigation shell and the back card (WP04), the glass components (WP05), WP06, the catalog app, and how their merge resolutions fit together; also check two security holes the coverage audit found unrouted (handoff/WP04.md:1393-1397): the app lock reopening unlocked when a link or Recents arrives within 30 s of LockActivity (H-4) and DeepLink.from not checking the host of an https link against deeplink_host while MainActivity is exported (H-5); if either is present on staging it is blocking (security first)' },
  { key: 'perf-multiplatform', text: 'performance and tooling (WP04-perf harness, gates.toml, the performance overlay HUD and its prod absence, and its absence from the builds the perf harness measures; look for merge regressions in performance: code a merge resolution put on a per-frame path, a modifier or listener that now runs twice, a remembered value lost, a recomposition the tests of the owner had ruled out) and the multiplatform and iOS work (WP07, WP08)' },
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
KNOWN BEFORE STAGING (bug run, Oct 19 23:00; bugs/BUG-2026-10-19-back-see-through.md Result and bugs/2026-10-19-back-licenses/verify/proof-1/): the combined preview tree (WP04 + T + C1 + WP06 + WP03 + HUD, 22992d3c/722ced37) regresses the back gesture against T + C1 alone on REQUIREMENTS 42's release rows, confirmed by a same-session A/B control: hitch 12.7 against 6.9 ms/s (limit 5.0), janky after the first frame 1.2-1.5 % against 0.3-0.4 % (limit 1.0), the gesture's first three frames 4.75-5.33 against 2.1-2.2 ms (ratchet 2.7); most hitch time is in the push half, most jank in the gesture half. First suspect, unverified: app/build.gradle.kts adds :core:perfhud as devImplementation, so devBenchmarkRelease measures with the HUD's JankStats listener and DEV badge running (measured builds must not carry dev tooling, or must measure it explicitly). Otherwise bisect the merges WP06 (ebb1ca15), WP03 and its port (e0f443b8, fe33d475), HUD and its port (aaab6fe4, 22992d3c). This is blocking for staging: find the cause with traces, fix it at its root, and prove the rows back at T + C1's level. Fix each at its root on staging, one commit per cause, with a regression test where the cause is code; re-run exactly the checks each fix touches (through the locks) and update ${SD}/TEST-REPORT.md and ${SD}/MERGE-NOTES.md. A failure that is a known open point of one package from before the merge (its handoff says so) and not made worse by the combination: record it in ${SD}/CARRY-OVER.md for that package's new branch instead of fixing it here. Return what you did as a test report (rows for every check you re-ran).`, { label: `staging:fix#${round}`, phase: 'Fix', schema: TESTED })
  if (!fix) throw new Error('fix round failed')
  tested = { desk: { ...tested.desk, state: fix.state, failures: fix.failures }, phone: { ...tested.phone, state: fix.state, failures: fix.failures } }
  reviews = (await reviewOnce(round + 1)).filter(Boolean)
  blocking = reviews.flatMap(r => r.blocking)
}

phase('Requirements')
// The user, Oct 19 19:56: "make sure requirements are fully fulfilled in their most optimises best and smoothest way"
// (REQUIREMENTS 46). Each requirement is judged on the combined code with proof; what falls short goes to the owning
// staging-wp branch with the better way spelled out. Judgments, so Opus throughout.
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
  { key: '31-46', text: 'items 31 to 46 and every unnumbered item (memory safety and the like)' },
]
const matrixPrompt = (g, extra) => `You are an Opus requirements auditor at the Google/AOSP bar. ${RULES} Do not modify code or branches.
For ${OV}/REQUIREMENTS.md ${g.text} (every sub-point counts as its own row): on staging in ${WT}, decide whether each is met (fully, with proof), partly, not-met, or deferred (only where the user deferred it, for example Compose Multiplatform and Swift after the Android release per item 36; cite it). For each row: where it lives (file:line on staging), the proof (a test that exercises it, a phone capture or perf number in ${SD}/TEST-REPORT.md, or a check you run now through the locks), and best_way: whether it is done the most optimised, best and smoothest way on the Redmi (budget phone, 120 Hz, the user's goal "light and smooth, the most optimised visual feel"); if not, the concrete better way and what it costs; "best" only with a reason. A requirement with no proof is partly at best. The coverage audit (${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md, its MATRIX-ROUTED and not-in-plan rows) lists sub-points no package implements yet (for example item 37's glass toggle, slider, segmented control, buttons, alert, toast and playground strips; item 34's shimmer proof; 38.5's performance audit; 23.8's AdaptiveAuditTest; 36's TalkBack proof): judge each of them explicitly. Owner: the staging-wp/<package> branch that should close the gap. Write your rows to ${SD}/REQUIREMENTS-MATRIX.md under the heading "Items ${g.key}" (create the file if needed; other auditors write the other groups).${extra}`
let matrix = (await parallel(REQ_GROUPS.map(g => () => agent(matrixPrompt(g, ''), { label: `staging:requirements-${g.key}`, phase: 'Requirements', schema: MATRIX })))).filter(Boolean)
const mreview = await agent(`You are a separate Opus reviewer of the requirements matrix ${SD}/REQUIREMENTS-MATRIX.md for staging in ${WT}. ${RULES} Do not modify anything. Check every "met" against its proof (open the test, the capture, the number; a "met" without real proof is blocking), every "deferred" against a real user decision, that no requirement or sub-point of ${OV}/REQUIREMENTS.md is missing, and that best_way is honest about smoothness on the Redmi (a known red perf row, such as the back gesture's hitch ratio in REQUIREMENTS 42, cannot sit under "best"). The rows: ${JSON.stringify(matrix.map(m => m.rows))}. There is no pressure to find something.`, { label: 'staging:requirements-review', phase: 'Requirements', schema: REVIEW })
if (mreview && mreview.blocking.length) {
  log(`requirements review: ${mreview.blocking.length} blocking`)
  matrix = (await parallel(REQ_GROUPS.map(g => () => agent(matrixPrompt(g, `\nThis is a revision of your rows. A separate reviewer found: ${JSON.stringify(mreview.blocking)}. Fix the rows in your group that these concern (re-check the proof), rewrite your section and return all your rows.`), { label: `staging:requirements-${g.key}#2`, phase: 'Requirements', schema: MATRIX })))).filter(Boolean)
}
const reqRows = matrix.flatMap(m => m.rows)
const reqGaps = reqRows.filter(r => r.verdict === 'partly' || r.verdict === 'not-met')
log(`requirements: ${reqRows.filter(r => r.verdict === 'met').length} met, ${reqGaps.length} with gaps, ${reqRows.filter(r => r.verdict === 'deferred').length} deferred`)

phase('Report')
const done = await agent(`You finish the consolidation. ${RULES}
1. Cut a work branch from staging's head for every package that still has work (read the handoffs, ${SD}/CARRY-OVER.md and the release order in ${OV}/scripts/to-release.js): git -C ${R} branch staging-wp/<package> staging (for example staging-wp/WP04, staging-wp/WP05, staging-wp/HUD, staging-wp/NET, staging-wp/CAT ...). Do not create worktrees and do not move or delete any old branch.
2. Write ${SD}/STAGING-REPORT.md for the user in plain English, readable on a phone: what is in staging (each package, one line, finished or how far along), what was tested and the results (the desktop matrix and the phone, with the perf gate table and cold start), the APKs to install with sha256, the screenshots and the tour recording, what is rough or unfinished and on which new branch it continues, what the review found and what was fixed, and what still needs the user (as clear questions with options).
3. The requirements: put the matrix's counts in the report and, in plain English, every requirement not fully met or not met the smoothest way, with its owner and the better way; write ${SD}/REQUIREMENTS-GAPS.md grouped by staging-wp branch (each gap with its proof and the better way), and make sure every owner in it got a branch in step 1.
4. Route every FORGOTTEN item of ${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md to its suggested owner, so nothing in it is lost: under that staging-wp branch in ${SD}/REQUIREMENTS-GAPS.md, or in ${SD}/RELEASE-PLAN-NOTES.md for what belongs to the re-based release script (the one-time template reformat after the last UI wave, tools/size_budgets.toml, the signed-in /admin check that needs the user); list them in the report under 'Routed from the coverage audit'.
5. Add a section at the top of ${OV}/STATUS.md §1 naming staging as the new integration branch and the staging-wp/* branches, and one PROGRESS.md line.
Inputs: plan ${JSON.stringify(plan)}; merge ${JSON.stringify(merged)}; desktop tests ${JSON.stringify(tested.desk)}; phone tests ${JSON.stringify(tested.phone)}; open blocking review findings ${JSON.stringify(blocking)}; nonblocking ${JSON.stringify(reviews.flatMap(r => r.nonblocking))}; requirement gaps ${JSON.stringify(reqGaps)}; matrix review ${JSON.stringify(mreview)}.`, { label: 'staging:report', phase: 'Report', schema: DONE })
return { plan, merged, tested, openBlocking: blocking, requirements: { met: reqRows.filter(r => r.verdict === 'met').length, gaps: reqGaps.length, deferred: reqRows.filter(r => r.verdict === 'deferred').length }, done }
