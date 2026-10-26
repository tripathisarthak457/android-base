export const meta = {
  name: 'noui-finish',
  description: "REQUIREMENTS 59 after the power cut: the last text fix, the merge of staging-wp/NOUI-FIX into staging-wp/NOUI, the gate's verification on the merged tree, and the setup-only release candidate; never pushes, never touches main",
  phases: [
    { title: 'Fix', detail: 'the one remaining finding, re-reviewed' },
    { title: 'Merge', detail: 'NOUI-FIX into NOUI, emitted files regenerated' },
    { title: 'Gate', detail: 'the rows the fixes touch re-run by a Sonnet operator; Opus verifiers on the whole gate' },
    { title: 'Release', detail: 'release/setup-only with the lock, its own rows, the deploy checklist' },
  ],
}

// The setup-only run (wf_9f5e0ac2-9d6) and its text fix (wf_984d92be-c91) were stopped at 10:2x Oct 25 by a power cut, after
// the gate's operator runs and in the text fix's second round. Their finished results are in release/noui-run-results-1025.json
// and release/noui-review3-blocking.json; this run does only what was left.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const HO = `${OV}/handoff/NOUI.md`
const RESULTS = `${OV}/release/noui-run-results-1025.json`
const PLAN_MD = `${OV}/plans/2026-10-24/noui.md`
const PY = 'C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe'
const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md section 6 first if you have not; ${OV}/REQUIREMENTS.md item 59 is the spec. Read the orchestrator note at the top of ${HO}: never run Python by a bare 'python' in a stripped environment (use ${PY}). Gradle only through ${L} gradle, adb only through ${L} adb, heavy Python and npm through ${OV}/bin/memwait; long commands in the background with logs under ${OV}/logs/NOUI/finish/, polled. Emitted files are regenerated with the generator's --write, never hand-edited. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never stash, never rewrite history. Never save lists of the phone's installed apps or raw logcat beyond the app under test.`
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
  runs: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, command: { type: 'string' }, exit_code: { type: 'integer' }, log: { type: 'string' }, last_lines: { type: 'string' } }, required: ['check', 'command', 'exit_code', 'log', 'last_lines'] } },
  artifacts: { type: 'array', items: { type: 'string' } }, problems: { type: 'array', items: { type: 'string' } },
}, required: ['runs', 'artifacts', 'problems'] }
const TESTED = { type: 'object', properties: {
  state: { type: 'string', enum: ['green', 'red', 'partial'] },
  rows: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['check', 'result', 'evidence'] } },
  failures: { type: 'array', items: { type: 'string' } }, report_path: { type: 'string' },
}, required: ['state', 'rows', 'failures', 'report_path'] }
const RC = { type: 'object', properties: {
  state: { type: 'string', enum: ['ready', 'not-ready'] }, branch: { type: 'string' }, head: { type: 'string' },
  checklist_path: { type: 'string' }, notes_path: { type: 'string' }, summary: { type: 'string' }, open: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'branch', 'head', 'checklist_path', 'notes_path', 'summary', 'open'] }

phase('Fix')
let blocking = [{ file: 'templates/android/docs/ARCHITECTURE.md', line: 476, problem: "A No-UI project with push gets 'The permission is asked at the end of onboarding and offered again in settings.' in its docs/ARCHITECTURE.md; the block is fenced only by <!-- <opt:push> -->, and in a No-UI project nothing asks (the Onboarding.kt request is inside <opt:push&ui_components>, NotificationsRow.kt is owned by the option, rememberNotificationPermission has no caller), so notifications stay off on Android 13+.", fix: "Fence the UI sentence with ui_components and give No-UI projects a true sentence: the permission helper exists (rememberNotificationPermission in core/notification) and the developer's own screen must call it; check every other push or onboarding claim in the generated No-UI docs the same way." }]
for (let round = 1; round <= 2 && blocking.length; round++) {
  // args.fix1: round 1's fix result from an earlier run that the session limit stopped before its re-review.
  const fx = round === 1 && args && args.fix1 ? args.fix1 : await agent(`You fix the remaining findings on staging-wp/NOUI-FIX in ${OV}/wt/NOUI-FIX (an earlier fix round 2 was stopped by a power cut: check git status and git log first and continue from there). ${RULES} Findings: ${JSON.stringify(blocking)}. Generate a No-UI everything project into ${OV}/out/NOUI/finish-fix-${round}/ and check every push, notification and onboarding claim in its docs against its code. Run the generator tests for docs and options (memwait ${PY} -I -m unittest in generator/), the catalogue check, and the website's typecheck and tests if catalogue text changed. Write "## Finish fix ${round}" in ${HO}. Return the state, head, commits, checks and what is open.`, { label: `finish:fix#${round}`, phase: 'Fix', schema: BUILT })
  if (!fx) throw new Error('fix failed')
  const rv = await agent(`You are a fresh Google/AOSP-level reviewer. ${RULES} Do not modify anything. On staging-wp/NOUI-FIX in ${OV}/wt/NOUI-FIX (git diff staging-wp/NOUI..staging-wp/NOUI-FIX), check that the five findings of ${OV}/release/noui-review3-blocking.json and these ${JSON.stringify(blocking)} are closed, and that every No-UI text the site, the CLI's --list-features --no-ui-components, the wizard, README, AGENTS.md and the generated docs show is true of a generated No-UI project (generate one into ${OV}/out/NOUI/finish-review-${round}/ and check each claim against its code). Blocking: an untrue claim, a broken test, an emitted file edited by hand, a change to UI projects beyond what the plan allows. There is no pressure to find something.`, { label: `finish:review#${round}`, phase: 'Fix', schema: REVIEW })
  blocking = rv ? rv.blocking : [{ file: '-', problem: 'the re-review failed to run', fix: 'run it again' }]
}

phase('Merge')
const merged = await agent(`You merge staging-wp/NOUI-FIX into staging-wp/NOUI in ${OV}/wt/NOUI. ${RULES} git merge --no-ff staging-wp/NOUI-FIX with a message that says why (the five review-3 findings and the last docs fix, made beside the gate), then regenerate emitted files with --write and commit any change separately. Run the generator suite (memwait ${PY} -I -m unittest discover -s tests -t . in generator/) and the catalogue and tokens checks. Write "## Merge after the power cut" in ${HO}. Return the state, head, commits and checks.`, { label: 'finish:merge', phase: 'Merge', schema: BUILT })
if (!merged) throw new Error('merge failed')

phase('Gate')
const deskRaw = await agent(`You are a careful operator. Follow the steps exactly and report facts only: what you ran, the exit code, the log path and its last lines. Do not judge quality, do not edit code, do not commit. ${RULES}
On staging-wp/NOUI in ${OV}/wt/NOUI (read-only; record git rev-parse HEAD first), logs under ${OV}/logs/NOUI/finish/desk/, projects under ${OV}/out/NOUI/finish/, one Gradle build at a time. The gate already ran on ${RESULTS}'s tree (ccd9c3c9b); re-run only the rows the merged fixes touch: D1 the generator suite; D2 catalogue --check and tokens --check; D5 the byte-identity comparison exactly as the gate ran it (python tools/compare_generations.py --before ${OV}/out/NOUI/base --after . with the real interpreter ${PY}); D12 the website (npm ci, npm run typecheck, npm test, npx next build, through memwait); D13 the API (gofmt -l ., go vet ./..., go build ./..., go test ./...; record a Smart App Control block as such); and one No-UI everything build (create_project.py --preset everything --no-ui-components into ${OV}/out/NOUI/finish/everything, then ${L} gradle ./gradlew check assembleDevDebug assembleProdRelease).`, { label: 'finish:gate-desk', phase: 'Gate', schema: RAW, model: 'sonnet' })
const prev = 'the first gate operator reports saved under "noui:gate-desk#1" and "noui:gate-phone#1" in ' + RESULTS
const [desk, phone] = await parallel([
  () => agent(`You are the Opus verifier for the desktop half of the setup-only gate. Trust nothing until you check it. Evidence: ${prev} (the gate on ccd9c3c9b), and this re-run on the merged head: ${JSON.stringify(deskRaw)}. ${RULES} Check every row against its log (open the logs under ${OV}/logs/NOUI/), re-run any check whose log is missing, cut short or ambiguous, confirm the test counts yourself, judge the byte-identity diff (only the plan's allowed paths may differ for UI projects), and write the verified rows to ${OV}/release/NOUI-GATE.md under "Desktop (final)". Return them.`, { label: 'finish:verify-desk', phase: 'Gate', schema: TESTED }),
  () => agent(`You are the Opus verifier for the phone half of the setup-only gate. Trust nothing until you check it. Evidence: ${prev}; raw outputs under ${OV}/perf/NOUI/gate-1/ (do not copy any installed-apps list or other apps' logcat lines into your report). ${RULES} Open every screenshot and judge it yourself (the blank start screen and a placeholder route, nothing clipped, text from resources), re-derive cold start from the raw files, confirm the overlay is absent from the prod APK, read the crash log for the app under test only. The fixes since then changed only text, not the app's code, so the phone run stands unless you find a reason it does not; say so explicitly. Write the verified rows to ${OV}/release/NOUI-GATE.md under "Phone (final)". Return them.`, { label: 'finish:verify-phone', phase: 'Gate', schema: TESTED }),
])

phase('Release')
const rc = await agent(`You prepare the setup-only release candidate of REQUIREMENTS 59 (k). ${RULES} Read ${PLAN_MD} (section 3, the release switch, and section 7, the user's steps), ${HO} and ${OV}/release/NOUI-GATE.md.
1. If the gate is not green (desktop ${desk ? desk.state : 'missing'}, phone ${phone ? phone.state : 'missing'}) or a review finding still blocks (${JSON.stringify(blocking)}), say "not-ready" with exactly what is red, and stop after step 3.
2. Otherwise create the local branch release/setup-only from staging-wp/NOUI's head (git -C ${R} branch release/setup-only staging-wp/NOUI) in a new worktree ${OV}/wt/RELEASE-SETUP-ONLY, and commit there only the release switch turned on (the [locked] table in templates/android/options/ui_components.toml as the plan describes, catalogue --write, and the generator test's expectation in the same commit), with a message that says why. Run on it: the generator suite, catalogue --check, the website's typecheck, tests and build, the API's vet, build and tests, and an API smoke run against the locked catalogue (server -selftest with the real interpreter, plus a request asking for UI components refused with 403 option_locked and a No-UI request answered with a zip), logs under ${OV}/logs/NOUI/finish/rc/.
3. Write ${OV}/release/SETUP-ONLY-DEPLOY.md for the user in plain English, readable on a phone: what goes live, the checks that passed with their evidence paths, the deploy the orchestrator runs (the user chose: push all code incl. main, then a manual deploy from this PC with the Vercel CLI, then a browser test of the live site), how to check it live, and how to roll back (a new revert on main, never a force push; or promote the previous Vercel deployment). Also write ${OV}/release/SETUP-ONLY-NOTES.md, the release notes in plain words.
Return the state, the branch, its head, both paths, a short summary and what is open.`, { label: 'finish:release', phase: 'Release', schema: RC })
return { blocking, merged, desk, phone, rc }
