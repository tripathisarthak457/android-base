export const meta = {
  name: 'noui-final',
  description: "REQUIREMENTS 59 (n): every remaining non-UI item finished in both modes, integrated, reviewed, gated on desktop and phone, and the release candidate for the user's one final deploy; never pushes, never touches main",
  phases: [
    { title: 'Plan', detail: 'an Opus planner turns every remaining item into lanes that own disjoint files' },
    { title: 'Build', detail: 'the lanes in parallel worktrees, test-first' },
    { title: 'Integrate', detail: 'staging-wp/NOUI, the lanes and the steps that need the whole tree, into staging-wp/NOUI-FINAL' },
    { title: 'Review', detail: 'fresh Opus reviews by area, fix rounds until clean' },
    { title: 'Profile', detail: 'the baseline profiles re-recorded on the Redmi from the final code' },
    { title: 'Gate', detail: 'Sonnet operators on desktop and phone, Opus verifiers, a fix round per red row' },
    { title: 'Release', detail: 'release/setup-only with the lock and its checks, then an Opus go or no-go' },
  ],
}

// The user, 13:1x Oct 25 (REQUIREMENTS 59 (n)): no separate setup-only deploy; finish everything that is left, then one
// final deploy, even if it takes the night. This run replaces the follow-up batch (scripts/noui-next.js, stopped 15
// minutes in; its TEST and CORE lanes continue here from their branches) and the finish run's gate and release steps.
// args.done maps a step's label to its result from an earlier run, so a relaunch after a usage pause redoes only the rest.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const PY = 'C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe'
const BASE = '1f2fd6c85'
const HO = `${OV}/handoff/NOUI-FINAL.md`
const PLAN = `${OV}/plans/2026-10-25/final.md`
const INT = `${OV}/wt/NOUI-FINAL`
const GATE = `${OV}/release/NOUI-FINAL-GATE.md`
const DONE = (args && args.done) || {}

const PLAN_RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md section 6 first if you have not; ${OV}/REQUIREMENTS.md items 58 and 59 hold, and 59 (n) is the user's latest word. Read the orchestrator note at the top of ${OV}/handoff/NOUI.md: never run Python by a bare 'python' in a stripped environment (use ${PY}); go test may be blocked by Smart App Control (record it, never bypass it; CI on Linux runs it). Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python and npm through ${OV}/bin/memwait; one Gradle build at a time machine-wide (the lock enforces it, so use the narrowest task that proves a change and keep full builds for the proof at the end); long commands in the background with logs under ${OV}/logs/NOUI-FINAL/, polled. Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md, the template's AGENTS.md and the generated docs: no Material, everything through AppTheme, strings in resources, stateless modular components); never weaken, skip or delete a test to get to green; emitted files are regenerated with the generator's --write, never hand-edited; stable library versions only, all in the version catalog. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never stash, never rewrite history. Until the finish run's merge is in staging-wp/NOUI, never touch ${OV}/wt/NOUI or ${OV}/wt/NOUI-FIX. No private names (periflow, vitfleet, jorwen) anywhere. Phone 9a7c74f4: screenshots and recordings to /data/local/tmp, never /sdcard; screenrecord needs --size 720x1600; restore any device setting you change; judge smoothness only on release or benchmark builds; never save lists of the phone's installed apps or raw logcat beyond the app under test.`
// The plan's prompt keeps the rules it ran with, so a resumed run takes its result from the cache.
const RULES = PLAN_RULES + ` Disk: D: filled up at 18:38 Oct 25 (195 GB drive). The user approved (18:5x): right after a check's result and logs are recorded, permanently delete the build output (the build, .gradle and .kotlin folders) of the projects YOU generated under ${OV}/out/NOUI-FINAL/, and nothing else; keep logs, screenshots, perf files and any APK a later step still needs. Before each full build, check D:'s free space (powershell -NoProfile -Command "(Get-PSDrive D).Free/1GB"); under 8 GB, do not build: report it as a problem and stop.`

const BUILT = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] }, head: { type: 'string' },
  commits: { type: 'array', items: { type: 'string' } },
  checks: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, result: { type: 'string' }, log: { type: 'string' } }, required: ['check', 'result', 'log'] } },
  open: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'head', 'commits', 'checks', 'open'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }
const RAW = { type: 'object', properties: {
  head: { type: 'string' },
  runs: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, command: { type: 'string' }, exit_code: { type: 'integer' }, log: { type: 'string' }, last_lines: { type: 'string' } }, required: ['check', 'command', 'exit_code', 'log', 'last_lines'] } },
  artifacts: { type: 'array', items: { type: 'string' } }, problems: { type: 'array', items: { type: 'string' } },
}, required: ['head', 'runs', 'artifacts', 'problems'] }
const TESTED = { type: 'object', properties: {
  state: { type: 'string', enum: ['green', 'red', 'partial'] },
  rows: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['check', 'result', 'evidence'] } },
  failures: { type: 'array', items: { type: 'string' } }, report_path: { type: 'string' },
}, required: ['state', 'rows', 'failures', 'report_path'] }
const PLANNED = { type: 'object', properties: {
  plan_path: { type: 'string' },
  lanes: { type: 'array', items: { type: 'object', properties: {
    key: { type: 'string' }, worktree: { type: 'string' }, branch: { type: 'string' }, base: { type: 'string' },
    items: { type: 'array', items: { type: 'string' } }, owns: { type: 'array', items: { type: 'string' } },
    brief: { type: 'string' }, proof: { type: 'string' },
  }, required: ['key', 'worktree', 'branch', 'base', 'items', 'owns', 'brief', 'proof'] } },
  after_integration: { type: 'array', items: { type: 'string' } },
  ui_changes: { type: 'array', items: { type: 'string' } },
  out_of_scope: { type: 'array', items: { type: 'object', properties: { item: { type: 'string' }, reason: { type: 'string' } }, required: ['item', 'reason'] } },
}, required: ['plan_path', 'lanes', 'after_integration', 'ui_changes', 'out_of_scope'] }
const RC = { type: 'object', properties: {
  state: { type: 'string', enum: ['ready', 'not-ready'] }, branch: { type: 'string' }, head: { type: 'string' },
  checklist_path: { type: 'string' }, notes_path: { type: 'string' }, summary: { type: 'string' }, open: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'branch', 'head', 'checklist_path', 'notes_path', 'summary', 'open'] }
const VERDICT = { type: 'object', properties: {
  decision: { type: 'string', enum: ['GO', 'NO-GO'] }, reasons: { type: 'array', items: { type: 'string' } },
  blocking: { type: 'array', items: { type: 'string' } }, report_path: { type: 'string' },
}, required: ['decision', 'reasons', 'blocking', 'report_path'] }

const step = (label, prompt, opts) => DONE[label] !== undefined ? Promise.resolve(DONE[label]) : agent(prompt, { ...opts, label })

phase('Plan')
const plan = await step('final:plan', `You are the Opus planner of the last batch before the user's one final deploy. ${PLAN_RULES} Do not modify code or branches; write only ${PLAN} and ${HO}.
The user, 13:1x Oct 25: "Finish each and everything first that's left and deploy that at last, one and last full final deploy, even if it takes whole night today." Their answers (REQUIREMENTS 59 (n)): the items that change UI projects are done in BOTH modes (one architecture; UI projects are proven by full desktop builds and tests, a phone smoke run and a no-regression speed check instead of byte identity); dependency-guard, the compose-rules ktlint ruleset and Google's Accessibility Test Framework checks in preview tests are added to every generated project (stable versions, in the version catalog); CLAUDE.md keeps @-importing the three guides.
Everything that is left, every item of it: ${OV}/plans/2026-10-24/noui.md section 9 (L1-L10, T4, F1.4, F2-F15, S2, the admin share of No-UI generations), with the detail in ${OV}/plans/2026-10-24/noui-arch-architecture.md and noui-arch-guardrails.md; the open items the fix rounds left (${OV}/release/noui-finish-fix1.json "open", and the "Unchanged from earlier rounds" list in ${OV}/handoff/NOUI.md: the Spotless carry-over, the UI profile's EverythingApplication names, next 15.5.27's critical advisory GHSA-2464-fhr4-w9xw and GHSA-p3m5-59mp-jv5r, fixed in 15.5.28 or the newest stable 15.5.x); the earlier follow-up batch (scripts/noui-next.js) whose two lanes already have work on staging-wp/NOUI-NEXT-TEST (L3, L4, L7) and staging-wp/NOUI-NEXT-CORE (S2, L9; L6 not started) in ${OV}/wt/NOUI-NEXT-TEST and ${OV}/wt/NOUI-NEXT-CORE, from f5a773e74, each a WIP commit from a power cut plus whatever is uncommitted there (read git log, git show --stat and git status in both; that work is unverified).
Out of scope, say so with the reason: rewriting pushed history (needs the user), WEB incidents 1 and 2 (the user's system), the UI product's own gaps on staging and the four first-frame rows (the Android release and the user's open decision), multiplatform and iOS (59 (j)).
Plan it as lanes that can run at once in separate worktrees with disjoint file ownership (at most 6, including TEST and CORE, which continue in their worktrees and branches with base f5a773e74; new lanes get worktree ${OV}/wt/NOUI-FINAL-<KEY> on branch staging-wp/NOUI-FINAL-<KEY> from ${BASE}, which is staging-wp/NOUI-FIX's head with every text fix). Group what touches the same files into one lane (for example L1, L2, L5 and L10 all touch navigation and every feature's navigation files; L6 changes the ViewModels whose tests TEST writes). Anything that needs the integrated tree (a rule rolled out once the code it checks complies, a marker flipped, a doc that names APIs of two lanes) goes in after_integration, in order. For each lane: the items, the files it owns (globs), a brief precise enough for an implementer (what to change, the design chosen and why, at the Now in Android bar or better), and the proof (tests seen failing first, the narrow Gradle tasks, and one full proof: generate the presets it affects in both modes and build them with check). List in ui_changes every way UI projects' output will change (the gate's byte-identity row is judged against it). Write the plan to ${PLAN} with a lane table, the order and the risks, and start ${HO} with a short header. Return it.`, { phase: 'Plan', schema: PLANNED })
if (!plan || !plan.lanes.length) throw new Error('plan failed')
log(`plan: ${plan.lanes.map(l => l.key).join(', ')}; after integration: ${plan.after_integration.length} steps`)

phase('Build')
const lanePrompt = (ln) => `You implement lane ${ln.key} of the final batch in ${ln.worktree} (branch ${ln.branch}, base ${ln.base}). ${RULES}
If the worktree does not exist, create it: git -C ${R} worktree add ${ln.worktree} -b ${ln.branch} ${ln.base} (if the branch exists, git -C ${R} worktree add ${ln.worktree} ${ln.branch}). Start with git log ${ln.base}..HEAD and git status: earlier work in progress on your branch or in your worktree is unverified, so check it as if it were someone else's, keep what is right, and finish in new focused commits on top.
The plan is ${PLAN}; your part: ${JSON.stringify({ items: ln.items, owns: ln.owns, brief: ln.brief, proof: ln.proof })}. Edit only the files you own; if you need a change outside them, write it under "## Cross-lane requests" in ${HO} with the reason and work around it. Prove your change with the tests and the narrowest Gradle tasks that cover it (generate only the presets your change affects, build only the affected modules); do NOT run full preset builds with check yourself, whatever your proof says: the integration and the final gate build every preset in both modes, and Gradle runs one build at a time machine-wide (the orchestrator, 21:0x Oct 25, to reach the user's 09:00 deploy without dropping a single check). Work test-first (a new test seen failing before the change), commit as you go (a usage pause can stop you at any time; the next run continues from your commits and your worktree). Write what you did, decisions where reality differed (also as a line in ${OV}/decisions/LOG.md) and what is open under "## ${ln.key}" in ${HO}. Return the state, your head, the commits, each check with its log, and what is open.`
// Five lanes at most: six with a Gradle daemon left 552 MB free on this 16 GB machine (14:14 Oct 25). args.slots and
// args.first let a short run before a usage reset spend what is left on the lane that is on the critical path.
const LANE_SLOTS = (args && args.slots) || 5
const first = (args && args.first) || []
const order = plan.lanes.map((ln, i) => i).sort((a, b) => (first.includes(plan.lanes[b].key) ? 1 : 0) - (first.includes(plan.lanes[a].key) ? 1 : 0))
const lanes = new Array(plan.lanes.length).fill(null)
let nextLane = 0
await parallel(Array.from({ length: Math.min(LANE_SLOTS, plan.lanes.length) }, () => async () => {
  while (nextLane < order.length) {
    const i = order[nextLane++]
    const ln = plan.lanes[i]
    try { lanes[i] = await step(`final:lane-${ln.key}`, lanePrompt(ln), { phase: 'Build', schema: BUILT }) } catch (e) { lanes[i] = null }
  }
}))
lanes.forEach((r, i) => { if (!r) log(`lane ${plan.lanes[i].key} returned nothing`) })

phase('Integrate')
const integrated = await step('final:integrate', `You integrate the final batch into ${INT} (branch staging-wp/NOUI-FINAL). ${RULES}
If ${INT} does not exist, create it from staging-wp/NOUI's head: git -C ${R} worktree add ${INT} -b staging-wp/NOUI-FINAL staging-wp/NOUI. Check git log first and continue from where an earlier run stopped. Make sure the tree has every text fix: if staging-wp/NOUI does not contain staging-wp/NOUI-FIX's head, merge staging-wp/NOUI-FIX too. Then merge each lane branch with git merge --no-ff (messages that say why, with the trailer): ${JSON.stringify(plan.lanes.map(l => l.branch))}. Resolve conflicts by reading both sides and the plan ${PLAN}; read "## Cross-lane requests" in ${HO} and do each. The lanes' results: ${JSON.stringify(lanes)} (a missing or partial lane: finish what it left, test-first, before going on). Regenerate emitted files with --write. Run the generator suite (memwait ${PY} -I -m unittest discover -s tests -t . in generator/), catalogue --check and tokens --check, and an everything build in both modes with check. Write "## Integration" in ${HO}. Return the state, head, commits, checks and what is open.`, { phase: 'Integrate', schema: BUILT })
if (!integrated) throw new Error('integration failed')
const after = plan.after_integration.length ? await step('final:after', `You do the steps of the final batch that need the integrated tree, in ${INT} (branch staging-wp/NOUI-FINAL), in this order: ${JSON.stringify(plan.after_integration)}. ${RULES} The plan is ${PLAN}. An earlier run of this step was stopped at 06:44 Oct 26 inside step 8 (the mechanical reformat, uncommitted in the worktree): start with git log and git status, never redo a committed step, and finish step 8 from what is there (re-run the formatter if in doubt; it is deterministic). In step 9 do NOT build the presets with check: the final gate builds every preset in both modes right after the reviews (the orchestrator, for the user's 13:00 deploy; no check is dropped); at most one No-UI and one UI everything build, only if step 8 changed what compiles. Test-first where a step is code; one focused commit per step. Then run the generator suite, catalogue and tokens checks and tests.consistency --quick. Write "## After integration" in ${HO}. Return the state, head, commits, checks and what is open.`, { phase: 'Integrate', schema: BUILT }) : null
if (plan.after_integration.length && !after) throw new Error('the steps after integration failed')

phase('Review')
const AREAS = [
  { key: 'tests', text: 'the tests and test infrastructure: every new test exercises behaviour and fails without the code it covers (spot-check by reasoning or a scratch copy, never in the worktree), fakes over mocks, one dispatcher factory used everywhere, no test weakened when isReturnDefaultValues went, no flaky timing' },
  { key: 'core', text: 'the theme skeleton, the token emission, the multiplatform-readiness changes and the MVI busy-flag and initial-load pattern: behaviour unchanged where it must be, the decision recorded, Google/AOSP-level code' },
  { key: 'navigation', text: 'the navigation changes in both modes (the runtime and shell split, one file per role, one-step destination and serializer registration, tab stacks out of the shell state): correctness, process-death restore, deep links, back handling, the scaffold and add_feature/remove_feature, the docs true to the code' },
  { key: 'guards', text: 'the guards and the build: the design-system rules now in both modes, Compose lint ids as errors, the compose-rules ktlint ruleset, dependency-guard and its baseline, the Accessibility Test Framework checks in preview tests, Isolated Projects readiness, the baselines that only shrink; every rule has a test that a violation fails; no rule weakened to pass' },
  { key: 'product', text: "the generator, the website, the API and the generated docs: emitted files regenerated not hand-edited, the catalogue, the admin share of No-UI generations, next's advisory closed, every No-UI and UI claim in the docs, README, AGENTS.md, CLAUDE.md, the site and the CLI true of a generated project (generate both modes into " + OV + '/out/NOUI-FINAL/review/ and check), UI output changed only as the plan\'s ui_changes list says' },
  { key: 'security', text: 'security, with the security-audit skill if it applies: the API (the lock, the admin endpoints and the new share query, input handling, error bodies), the site (the bumped next, headers), the generator (paths, templates, shelling out), the new build dependencies (where they come from, pinned stable versions), secrets and private names in the diff' },
]
const reviewArea = (a, round) => step(`final:review-${a.key}#${round}`, `You are a separate Google/AOSP-level reviewer of staging-wp/NOUI-FINAL in ${INT}, area: ${a.text}. ${RULES} Do not modify anything. Review git diff ${BASE}..staging-wp/NOUI-FINAL in your area (start with --stat; read every changed source and test file in your area), the plan ${PLAN} and ${HO}. Blocking: correctness bugs, weakened or useless tests, behaviour changes the plan did not ask for, untrue docs, security problems, private names. There is no pressure to find something.${round > 1 ? ' This is a re-review after fixes: report only what remains or is new.' : ''}`, { phase: 'Review', schema: REVIEW })
const profilePrompt = `You re-record the template's baseline profiles on the Redmi from the integrated code (plan item T4), in ${INT} (branch staging-wp/NOUI-FINAL). ${RULES} The first review round reads this tree while you work, and the fix rounds come after you; the gate measures cold start on the final head. Read the template's docs on the baseline profile and how the earlier ones were recorded (git log -- '**/baseline-prof.txt', ${OV}/handoff/NOUI.md). Generate the projects whose profiles the template ships (a UI everything and a No-UI everything, in ${OV}/out/NOUI-FINAL/profile/), record each on the phone through ${L} adb and ${L} gradle, copy the result into the template the way the earlier commits did (keep the regions and the markers), commit with the device and build in the message, and regenerate with --write if emitted files changed. Check that the generator suite and both everything builds still pass. Write "## Profiles" in ${HO}. Return the state, head, commits, checks and what is open.`
// The recording runs beside the first review round, which only reads; a fix round commits in the same worktree, so it waits for the recording.
let profileP = null
let areas = AREAS
let blocking = []
for (let round = 1; round <= 3; round++) {
  const reviewsP = parallel(areas.map(a => () => reviewArea(a, round)))
  if (round === 1) profileP = step('final:profile', profilePrompt, { phase: 'Profile', schema: BUILT })
  const reviews = await reviewsP
  const failed = areas.filter((a, i) => !reviews[i])
  blocking = reviews.filter(Boolean).flatMap(r => r.blocking).concat(failed.map(a => ({ file: '-', problem: `the ${a.key} review failed to run`, fix: 'run it again' })))
  const next = areas.filter((a, i) => !reviews[i] || reviews[i].blocking.length)
  log(`review round ${round}: ${blocking.length} blocking in ${next.map(a => a.key).join(', ') || 'no area'}`)
  if (!blocking.length || round === 3) break
  if (round === 1 && !(await profileP)) throw new Error('profile failed')
  const fx = await step(`final:fix#${round}`, `You fix staging-wp/NOUI-FINAL in ${INT}. ${RULES} Blocking review findings: ${JSON.stringify(blocking)}. Fix each at its root, one commit per cause, with a test where the cause is code; re-run the checks each fix touches. Write "## Fix round ${round}" in ${HO}. Return the state, head, commits, checks and what is open.`, { phase: 'Review', schema: BUILT })
  if (!fx) throw new Error('fix failed')
  areas = next
}

const profiled = await profileP
if (!profiled) throw new Error('profile failed')

phase('Gate')
const deskOp = (round, only) => step(`final:gate-desk#${round}`, `You are a careful operator. Follow the steps exactly and report facts only: what you ran, the exit code, the log path and its last lines. Do not judge quality, do not edit code, do not commit. ${RULES}
On staging-wp/NOUI-FINAL in ${INT} (read-only for you; record git rev-parse HEAD first as head), logs under ${OV}/logs/NOUI-FINAL/gate-${round}/desk/, projects under ${OV}/out/NOUI-FINAL/gate-${round}/, one Gradle build at a time. ${only ? `Re-run only these rows: ${JSON.stringify(only)}.` : `Rows: D1 the generator suite (memwait ${PY} -I -m unittest discover -s tests -t . in generator/); D2 catalogue --check and tokens --check; D3 the consistency matrix, full run; D4 add_feature and remove_feature on a No-UI and a UI project as the matrix's own script does; D5 the comparison against the old output (${PY} tools/compare_generations.py --before ${OV}/out/NOUI/base --after .), recording every differing path; D6 and D7: first the No-UI and the UI everything (compact) builds, then the rest: No-UI generations of lean, standard and everything (compact and modular) and UI generations of the same four, each built with ${L} gradle ./gradlew check assembleDevDebug assembleProdRelease; as soon as both everything (compact) builds are done, write their prodRelease APK paths to ${OV}/out/NOUI-FINAL/gate-${round}/APKS-READY.txt for the phone operator and keep those two projects' build output until the end of your run; D8 the formatter on every generated project (./gradlew spotlessCheck where the project has it); D9 dependency-guard and the compose-rules ktlint check on the everything builds; D10 the website (npm ci, npm run typecheck, npm test, npx next build, npm audit --omit=dev, through memwait); D11 the API (gofmt -l ., go vet ./..., go build ./..., go test ./...; record a Smart App Control block as such).`}`, { phase: 'Gate', schema: RAW, model: 'sonnet', effort: 'low' })
const phoneOp = (round, only) => step(`final:gate-phone#${round}`, `You are a careful operator on the phone. Follow the steps exactly and report facts only: what you ran, the exit code, the log path, the raw files. Do not judge quality, do not edit code, do not commit. ${RULES}
On staging-wp/NOUI-FINAL in ${INT} (read-only for you; record git rev-parse HEAD first as head), raw outputs under ${OV}/perf/NOUI-FINAL/gate-${round}/, projects under ${OV}/out/NOUI-FINAL/gate-${round}/phone/. Follow the method of the earlier setup-only phone gate (${OV}/release/NOUI-GATE.md and ${OV}/perf/NOUI/gate-1/) and of staging's perf gate (${OV}/staging/TEST-REPORT.md, REQUIREMENTS 58's limits). ${only ? `Re-run only these rows: ${JSON.stringify(only)}.` : 'Rows (P1 and P2 use the No-UI and UI everything prodRelease APKs the desktop operator builds: wait, polling every 30 s, until ' + OV + '/out/NOUI-FINAL/gate-' + round + '/APKS-READY.txt lists them, and never build those two yourself; build only P3 and P4): P1 No-UI everything prodRelease: install, cold start x10 (am start -W), screenshots of the start screen and two placeholder routes (to /data/local/tmp, then pulled), the overlay absent from the prod APK (apkanalyzer or a dex search), the crash buffer for the app only; P2 UI everything prodRelease: install, cold start x10, the catalogue tour (tabs, a push and pop, predictive back, one sheet) with a 720x1600 recording and frame stats (dumpsys gfxinfo framestats) for each scene, screenshots of each tab; P3 the same UI scenes on staging\'s head (git -C ' + R + ' worktree list shows wt/STAGING; generate from it into a separate folder) for a before-and-after of every perf row; P4 the benchmark module\'s startup benchmark for both modes if the project has it.'}`, { phase: 'Gate', schema: RAW, model: 'sonnet', effort: 'low' })
const verifyDesk = (round, raw) => step(`final:verify-desk#${round}`, `You are the Opus verifier for the desktop half of the final gate. Trust nothing until you check it. The operator's report: ${JSON.stringify(raw)}. ${RULES} Check every row against its log (open the logs), re-run any check whose log is missing, cut short or ambiguous, confirm the test counts yourself, judge D5's differences against the plan's ui_changes ${JSON.stringify(plan.ui_changes)} and the paths 59 (g) and (h) add (anything else is red), and write the verified rows to ${GATE} under "Desktop (round ${round})". Return them.`, { phase: 'Gate', schema: TESTED })
const verifyPhone = (round, raw) => step(`final:verify-phone#${round}`, `You are the Opus verifier for the phone half of the final gate. Trust nothing until you check it. The operator's report: ${JSON.stringify(raw)}. ${RULES} Open every screenshot and judge it yourself (nothing clipped, text from resources, the placeholder routes and the UI tabs as the docs say), re-derive cold start and every frame number from the raw files with the noise rule of the template's PERFORMANCE.md, compare UI against staging's head row by row (a regression beyond noise is red; rows that were already red on staging stay what they were and are named as such), confirm the overlay is absent from prod, and read the crash buffer for the app only. Write the verified rows to ${GATE} under "Phone (round ${round})". Return them.`, { phase: 'Gate', schema: TESTED })
const rcPrompt = (round) => `You prepare the release candidate for the user's one final deploy (REQUIREMENTS 59 (k) and (n)). ${RULES} Read ${OV}/plans/2026-10-24/noui.md (section 3, the release switch, and section 7, the user's steps), ${PLAN} and ${HO}. Opus verifiers are checking the final gate at the same time and the judge decides GO from both, so prepare and check the candidate and leave the gate's verdict to them.
1. ${round === 1 ? `Create the local branch release/setup-only from staging-wp/NOUI-FINAL's head (git -C ${R} branch release/setup-only staging-wp/NOUI-FINAL; if it exists from an earlier run, check what it holds and say so instead of moving it) in a new worktree ${OV}/wt/RELEASE-SETUP-ONLY, and commit there only the release switch turned on (the [locked] table in templates/android/options/ui_components.toml as the plan describes, catalogue --write, and the generator test's expectation in the same commit), with a message that says why.` : `A gate fix round moved staging-wp/NOUI-FINAL after the candidate was made: in ${OV}/wt/RELEASE-SETUP-ONLY merge staging-wp/NOUI-FINAL into release/setup-only (git merge, never a reset), regenerate the catalogue with --write if it changed, and commit.`} Run on it: the generator suite, catalogue --check, the website's typecheck, tests and build, the API's vet, build and tests, and an API smoke run against the locked catalogue (server -selftest with the real interpreter, a request asking for UI components refused with 403 option_locked, a No-UI request answered with a zip that unpacks and builds with check), logs under ${OV}/logs/NOUI-FINAL/rc-${round}/. Check that git merge-base --is-ancestor origin/main release/setup-only holds, so main can fast-forward.
2. Write ${OV}/release/SETUP-ONLY-DEPLOY.md for the user in plain English, readable on a phone: what goes live (everything this batch finished, in a few lines), the checks that passed with their evidence paths (the gate's verified rows go into ${GATE}), the deploy the orchestrator runs (the user's choice: push all code incl. main, then a manual deploy from this PC with the Vercel CLI, then a browser test of the live site), how to check it live, and how to roll back (a new revert on main, never a force push; or promote the previous Vercel deployment). Also write ${OV}/release/SETUP-ONLY-NOTES.md, the release notes in plain words.
Return the state ("ready" when the candidate and its checks pass), the branch, its head, both paths, a short summary and what is open (name the staging-wp/NOUI-FINAL head it carries).`
let desk = null
let phone = null
let deskOnly = null
let phoneOnly = null
let rc = null
let headMoved = true
for (let round = 1; round <= 3; round++) {
  const [dRaw, pRaw] = await parallel([
    () => desk && desk.state === 'green' ? Promise.resolve(null) : deskOp(round, deskOnly),
    () => phone && phone.state === 'green' ? Promise.resolve(null) : phoneOp(round, phoneOnly),
  ])
  // The candidate only adds the lock commit to the gated head and checks it, so it is made while Opus verifies the gate.
  const rcLabel = round === 1 ? 'final:release' : `final:release#${round}`
  const rcP = headMoved ? step(rcLabel, rcPrompt(round), { phase: 'Release', schema: RC }) : Promise.resolve(rc)
  const [d, p, r] = await parallel([
    () => dRaw ? verifyDesk(round, dRaw) : Promise.resolve(desk),
    () => pRaw ? verifyPhone(round, pRaw) : Promise.resolve(phone),
    () => rcP,
  ])
  desk = d
  phone = p
  rc = r
  headMoved = false
  log(`gate round ${round}: desktop ${desk ? desk.state : 'missing'}, phone ${phone ? phone.state : 'missing'}, candidate ${rc ? rc.state : 'missing'}`)
  if (desk && desk.state === 'green' && phone && phone.state === 'green') break
  if (round === 3) break
  const red = [].concat(desk ? desk.failures : ['the desktop gate did not run'], phone ? phone.failures : ['the phone gate did not run'])
  const gfx = await step(`final:gatefix#${round}`, `You fix what the final gate found red on staging-wp/NOUI-FINAL in ${INT}. ${RULES} Red rows: ${JSON.stringify(red)}; the verified rows are in ${GATE}. Find each root cause (read the logs and raw files), fix it with a test where it is code, one commit per cause; a speed regression on the phone is found with the android-profiler skill on a trace, never by loosening a limit. Re-run the narrow checks each fix touches. Write "## Gate fix ${round}" in ${HO}. Return the state, head, commits, checks and what is open, and name in open the gate rows that must run again.`, { phase: 'Gate', schema: BUILT })
  if (!gfx) throw new Error('gate fix failed')
  headMoved = gfx.commits.length > 0
  deskOnly = desk && desk.state === 'green' ? null : (desk ? desk.failures : null)
  phoneOnly = phone && phone.state === 'green' ? null : (phone ? phone.failures : null)
  // A fix to code can move rows that were green; the verifier decides from the diff whether they stand.
  if (desk && desk.state === 'green' && gfx.commits.length) desk = null
  if (phone && phone.state === 'green' && gfx.commits.length) phone = null
}

phase('Release')
const judge = rc && rc.state === 'ready' ? await step('final:judge', `You are the Opus release judge for the user's one final deploy. ${RULES} Do not modify anything. Decide GO or NO-GO for release/setup-only (${rc.head}) from the evidence, not from the summaries: ${GATE} and the logs it cites, the last reviews (blocking: ${JSON.stringify(blocking)}), ${HO}, the release candidate's checks under ${OV}/logs/NOUI-FINAL/rc/, ${OV}/release/SETUP-ONLY-DEPLOY.md, and git diff staging-wp/NOUI-FINAL..release/setup-only (the lock commit only). NO-GO for any red row, any blocking finding, a lock that the API does not enforce, a secret or private name in git diff origin/main..release/setup-only (scan it), or a deploy doc that says something untrue. Write the decision with reasons to ${OV}/release/FINAL-GO.md. Return it.`, { phase: 'Release', schema: VERDICT }) : null
return { plan: { lanes: plan.lanes.map(l => l.key), out_of_scope: plan.out_of_scope }, lanes, integrated, after, blocking, profiled, desk, phone, rc, judge }
