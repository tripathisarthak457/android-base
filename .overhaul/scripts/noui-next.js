export const meta = {
  name: 'noui-next',
  description: "The non-UI follow-up batch after the setup-only release (the user, Oct 25 09:4x: fix the remaining non-UI flaws in parallel, no change to the deploy ETA): tests, shared test fixtures, the theme skeleton and multiplatform readiness, reviewed and gated for a second deploy; low-priority locks; never pushes",
  phases: [
    { title: 'Build', detail: 'two lanes in parallel worktrees: tests, and core' },
    { title: 'Integrate', detail: 'both lanes merged into staging-wp/NOUI-NEXT' },
    { title: 'Review', detail: 'fresh Opus reviews with a fix round' },
    { title: 'Gate', detail: 'desktop rows by a Sonnet operator with an Opus verifier' },
  ],
}

// The setup-only release (REQUIREMENTS 59) deploys from release/setup-only at about 17:00 Oct 25. Its architecture reviews left
// "later" findings (plans/2026-10-24/noui-arch-*.md, plan section 9). The user asked to fix the non-UI ones in parallel
// without moving the deploy, so this batch runs on its own branches from the text-fix head, takes Gradle and the phone
// only at low priority, and ships in a second deploy after its own gate and the user's go.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const BASE = 'f5a773e74'
const HO = `${OV}/handoff/NOUI-NEXT.md`
const WT = { TEST: `${OV}/wt/NOUI-NEXT-TEST`, CORE: `${OV}/wt/NOUI-NEXT-CORE`, INT: `${OV}/wt/NOUI-NEXT` }
const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md section 6 first if you have not; ${OV}/REQUIREMENTS.md items 58 and 59 hold. Read the orchestrator note at the top of ${OV}/handoff/NOUI.md: never run Python by a bare 'python' in a stripped environment (use C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe); go test may be blocked by Smart App Control (record it, never bypass it). The setup-only release is being gated and deployed right now from ${OV}/wt/NOUI, ${OV}/wt/NOUI-FIX and ${OV}/wt/RELEASE-SETUP-ONLY: never touch those worktrees, and prefix EVERY ${L} command with LOCKED_PRIORITY=low so the release always goes first. Gradle only through ${L} gradle, adb only through ${L} adb, heavy Python and npm through ${OV}/bin/memwait; long commands in the background with logs under ${OV}/logs/NOUI-NEXT/, polled. Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md, the template's AGENTS.md and the generated docs); never weaken, skip or delete a test to get to green; emitted files are regenerated with the generator's --write, never hand-edited. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never stash, never rewrite history. No private names (periflow, vitfleet, jorwen) anywhere; never save lists of the phone's installed apps or raw logcat beyond the app under test.`
const BUILT = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  head: { type: 'string' },
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
  artifacts: { type: 'array', items: { type: 'string' } },
  problems: { type: 'array', items: { type: 'string' } },
}, required: ['runs', 'artifacts', 'problems'] }
const TESTED = { type: 'object', properties: {
  state: { type: 'string', enum: ['green', 'red', 'partial'] },
  rows: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['check', 'result', 'evidence'] } },
  failures: { type: 'array', items: { type: 'string' } },
  report_path: { type: 'string' },
}, required: ['state', 'rows', 'failures', 'report_path'] }

const LANES = {
  TEST: `Lane TEST (the architecture review's L3, L4 and L7; plans/2026-10-24/noui-arch-architecture.md and plan section 9 have the detail):
(1) Shared test fakes in testFixtures source sets where the modules share them, and ONE test-dispatcher factory (a MainDispatcher rule or extension) that every ViewModel test uses; migrate the existing tests to it without changing what they assert.
(2) ViewModel and logic tests the kept features lack: SignUp, PasswordReset, Settings, Onboarding and Feed ViewModels, the feature-flag seam, the notification logic and toUiText. Each tests behaviour (state and effects for every intent and failure), with fakes, not mocks; a test that cannot fail is not a test. They ship in both modes, so UI projects gain them too.
(3) Drop isReturnDefaultValues = true from the unit-test options, and fix every test that silently relied on Android stubs returning defaults (by a fake, a Robolectric test where it is really needed, or moving the logic off the Android type).
Prove it: the template's unit tests green, verifyCodeRules, a No-UI and a UI generation of the everything preset built with check (LOCKED_PRIORITY=low).`,
  CORE: `Lane CORE:
(1) The theme skeleton S2 (plan risk 7): in No-UI projects only, the emitted DesignTokens and AppTheme carry only the token slots of REQUIREMENTS 59 (b) (colors, typography, spacing and metrics, shapes, motion, reduce-motion), without the sections only the old components used (sheet, tab bar and the like); docs/DESIGN-SYSTEM.md says how to add a component's own tokens. UI projects stay byte-identical.
(2) Multiplatform readiness (L9): replace the last JVM-only calls in the non-UI modules (core, data, the features' logic) with Kotlin-common equivalents where one exists and the behaviour is unchanged, with a test for each; list what cannot move yet and why.
(3) If time remains and only then, the MVI hardening of L6 (busy flags derived from the work in flight, one initial-load pattern), recorded as a decision in ${OV}/decisions/LOG.md, with tests; otherwise leave it for the next batch and say so.
Prove it: the generator suite and the consistency matrix (quick), a No-UI generation of every preset built with check, a UI lean build with check (LOCKED_PRIORITY=low).`,
}

phase('Build')
const implPrompt = (k) => `You implement lane ${k} of the non-UI follow-up batch in ${WT[k]} (branch staging-wp/NOUI-NEXT-${k}, from ${BASE}). ${RULES}
${LANES[k]}
An earlier run of this lane was stopped by a power cut at 10:27 Oct 25: start with git log ${BASE}..HEAD and git status. Its work in progress is the WIP commit on your branch; it is unverified, so check it as if it were someone else's, keep what is right, and finish the lane in new focused commits on top (never rewrite history). Work test-first; commit in focused commits as you go (a usage pause can stop you at any time; the next run continues from your commits, any uncommitted work in your worktree and ${HO}). Write what you did, decisions where reality differed and what is open under "## ${k}" in ${HO}. Return the state, your head, the commits, each check with its log, and what is open.`
const lanes = await parallel([
  () => agent(implPrompt('TEST'), { label: 'next:build-TEST', phase: 'Build', schema: BUILT }),
  () => agent(implPrompt('CORE'), { label: 'next:build-CORE', phase: 'Build', schema: BUILT }),
])

phase('Integrate')
const integrated = await agent(`You integrate the non-UI follow-up batch in ${WT.INT} (branch staging-wp/NOUI-NEXT, from ${BASE}). ${RULES} The lanes' results: ${JSON.stringify(lanes)}. Check git log first and continue from where an earlier run stopped. Merge staging-wp/NOUI-NEXT-TEST and staging-wp/NOUI-NEXT-CORE with git merge --no-ff (messages that say why, with the trailer), resolve conflicts by reading both sides, and merge staging-wp/NOUI too if it has moved past ${BASE} (the release's own fixes win in their files). Regenerate emitted files with --write. Run the generator suite and a No-UI and a UI build of the everything preset with check (LOCKED_PRIORITY=low). Write "## Integration" in ${HO}. Return the state, head, commits, checks and what is open.`, { label: 'next:integrate', phase: 'Integrate', schema: BUILT })
if (!integrated) throw new Error('integration failed')

phase('Review')
const AREAS = [
  { key: 'tests', text: 'the tests and test infrastructure: every new test exercises behaviour and fails without the code it covers (spot-check by breaking the code locally in your head or a scratch copy, never in the worktree), fakes over mocks, one dispatcher factory used everywhere, no test weakened when isReturnDefaultValues went, no flaky timing' },
  { key: 'core', text: 'the theme skeleton, the multiplatform-readiness changes and any MVI change: behaviour unchanged where it must be, UI projects byte-identical where the batch promised it, the docs true to the code, the golden-standard guards still green, Google/AOSP-level code' },
]
const reviewOnce = (round) => parallel(AREAS.map(a => () => agent(`You are a separate Google/AOSP-level reviewer of staging-wp/NOUI-NEXT in ${WT.INT}, area: ${a.text}. ${RULES} Do not modify anything. Review git diff ${BASE}..staging-wp/NOUI-NEXT in your area (start with --stat; read every changed source and test file) and read ${HO}. Blocking: correctness bugs, weakened or useless tests, behaviour changes that were not asked for, untrue docs, private names. There is no pressure to find something.${round > 1 ? ' This is a re-review after fixes: report only what remains or is new.' : ''}`, { label: `next:review-${a.key}#${round}`, phase: 'Review', schema: REVIEW })))
let reviews = (await reviewOnce(1)).filter(Boolean)
let blocking = reviews.flatMap(r => r.blocking)
if (blocking.length) {
  const fx = await agent(`You fix staging-wp/NOUI-NEXT in ${WT.INT}. ${RULES} Blocking review findings: ${JSON.stringify(blocking)}. Fix each at its root, one commit per cause, with a test where the cause is code; re-run the checks each fix touches (LOCKED_PRIORITY=low). Write "## Fix round" in ${HO}.`, { label: 'next:fix#1', phase: 'Review', schema: BUILT })
  if (!fx) throw new Error('fix failed')
  reviews = (await reviewOnce(2)).filter(Boolean)
  blocking = reviews.flatMap(r => r.blocking)
}

phase('Gate')
const deskRaw = await agent(`You are a careful operator. Follow the steps exactly and report facts only: what you ran, the exit code, the log path and its last lines. Do not judge quality, do not edit code, do not commit. ${RULES}
On staging-wp/NOUI-NEXT in ${WT.INT} (read-only for you; record git rev-parse HEAD first), logs under ${OV}/logs/NOUI-NEXT/gate/, generated projects under ${OV}/out/NOUI-NEXT/gate/, every ${L} command with LOCKED_PRIORITY=low, one Gradle build at a time: the generator suite (memwait C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe -I -m unittest discover -s tests -t . in generator/); catalogue --check and tokens --check; the consistency matrix --quick; No-UI generations of lean, standard and everything (compact and modular) each built with check assembleDevDebug assembleProdRelease; UI generations of lean and everything built with check assembleDevDebug; the website's npm run typecheck and npm test; the API's go vet and go test ./... (record a Smart App Control block as such).`, { label: 'next:gate-desk', phase: 'Gate', schema: RAW, model: 'sonnet' })
const desk = await agent(`You are the Opus verifier of the non-UI follow-up batch's desktop gate. A Sonnet operator ran it; trust nothing until you check it. Its report: ${JSON.stringify(deskRaw)}. ${RULES} Check every run against its log, re-run any check whose log is missing, cut short or ambiguous (LOCKED_PRIORITY=low), confirm the test counts yourself, and write the verified rows to ${OV}/release/NOUI-NEXT-GATE.md. Return them.`, { label: 'next:verify-desk', phase: 'Gate', schema: TESTED })
return { lanes, integrated, openBlocking: blocking, gate: desk }
