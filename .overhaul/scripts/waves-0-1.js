export const meta = {
  name: 'android-base-waves-0-1',
  description: 'Implement work packages of waves 0-1 (WP01, WP02-M1, WP02-M2, WP03, WP04, WP06, WP08, WP09, WP10) in worktrees with dependency waits, a motion checkpoint, fresh-context review and serialized merges',
  phases: [
    { title: 'Implement', detail: 'package agents in their worktrees; they pause when blocked on a dependency' },
    { title: 'Review', detail: 'fresh-context reviewer per package, fix loop' },
    { title: 'Merge', detail: 'serialized merges into overhaul/multiplatform-v2 with integration checks' },
    { title: 'Checkpoint', detail: 'motion-feel decision on behalf of the sleeping user' },
  ],
}

const R = '/home/crazyhusir/Documents/projects/tools/android-base'
const OV = '/home/crazyhusir/Documents/projects/tools/.overhaul'
const DESIGN = OV + '/design'
const L = OV + '/bin/locked'

// id -> worktree name, WP file, prerequisites that must be merged/decided before the agent may start, scope note
const PKGS = {
  'WP01': { wt: 'WP01', file: 'WP01.md', start: [], scope: 'The whole package.' },
  'WP02-M1': { wt: 'WP02', file: 'WP02.md', start: [], scope: 'Milestone M1 ONLY (the "Milestone M1" section). Stop when M1 is accepted; M2 and M3 are separate runs.' },
  'WP02-M2': { wt: 'WP02', file: 'WP02.md', start: ['WP01', 'WP02-M1'], scope: 'Milestone M2 ONLY (the "Milestone M2" section). M1 is already merged. Its database scaffolding waits for WP06 (return waiting_for WP06 if you reach it first). M3 is a later run.' },
  'WP03': { wt: 'WP03', file: 'WP03.md', start: [], scope: 'The whole package: phase A, the call-site sweep after WP02-M1 merges, and phase B after WP01 merges and CP1 is decided.' },
  'WP04': { wt: 'WP04', file: 'WP04.md', start: [], scope: 'The whole package: phase 1, phase 2 (feature/** edits only after WP02-M1 merges), phase 3. Report milestone WP04:phase2 as soon as phase 2 is complete with its recordings, and wait for CP1 if phase 3 needs the chosen screen response.' },
  'WP06': { wt: 'WP06', file: 'WP06.md', start: ['WP01', 'WP02-M1'], scope: 'The whole package.' },
  'WP08': { wt: 'WP08', file: 'WP08.md', start: ['WP01'], scope: 'The whole package.' },
  'WP09': { wt: 'WP09', file: 'WP09.md', start: [], scope: 'The whole package. Its acceptance needs WP01 merged; do everything else first.' },
  'WP10': { wt: 'WP10', file: 'WP10.md', start: ['WP01'], scope: 'The whole package.' },
}
const KNOWN = new Set([...Object.keys(PKGS), 'CP1'])
const UI_ANDROID = new Set(['WP03', 'WP04', 'WP06', 'WP02-M2'])

// Deferred signals: a key resolves when a package merges, a milestone is reported, or a decision is written.
const signals = {}
function signal(k) {
  if (!signals[k]) { let r; const p = new Promise(res => { r = res }); signals[k] = { p, r, done: false } }
  return signals[k]
}
function fire(k) { const s = signal(k); if (!s.done) { s.done = true; s.r(true); log(`signal: ${k}`) } }
const doneKeys = () => Object.keys(signals).filter(k => signals[k].done)

// Liveness: a key is dead when the flow that would produce it has ended without producing it.
const ended = {}
function endedOf(name) {
  if (!ended[name]) { let r; const p = new Promise(res => { r = res }); ended[name] = { p, r } }
  return ended[name]
}
const producerOf = k => (k === 'CP1' ? 'CP1' : k.split(':')[0])
function waitFor(keys) {
  const all = Promise.all(keys.map(k => signal(k).p)).then(() => 'ok')
  const dead = keys.map(k => endedOf(producerOf(k)).p.then(() => (signal(k).done ? new Promise(() => {}) : 'dead:' + k)))
  return Promise.race([all, ...dead])
}

const STATUS = {
  type: 'object',
  properties: {
    state: { type: 'string', enum: ['done', 'waiting', 'blocked'] },
    waiting_for: { type: 'array', items: { type: 'string' } },
    milestones: { type: 'array', items: { type: 'string' } },
    summary: { type: 'string' },
    accept_ok: { type: 'boolean' },
  },
  required: ['state', 'waiting_for', 'milestones', 'summary', 'accept_ok'],
}
const REVIEW = {
  type: 'object',
  properties: {
    verdict: { type: 'string' },
    blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
    nonblocking: { type: 'array', items: { type: 'string' } },
  },
  required: ['verdict', 'blocking', 'nonblocking'],
}
const MERGE = {
  type: 'object',
  properties: { ok: { type: 'boolean' }, details: { type: 'string' }, needs_package_fix: { type: 'boolean' } },
  required: ['ok', 'details', 'needs_package_fix'],
}

const ORCH_DECISIONS = `
Orchestrator decisions that override MASTER-PLAN where they differ:
- The user is asleep and asked for no further questions; there is no one to ask. Kickoff approvals (MASTER-PLAN B.5) are GRANTED for: npm ci in web/frontend; Gradle dependency resolution including androidx.room3, io.insert-koin, io.ktor (darwin), io.github.kyant0, org.jetbrains.androidx.navigation3, org.jetbrains.compose and the Kotlin/Native toolchain; Docker pulls of golang:1.27-alpine and python:3.12-alpine (WP09 container check only). NOT granted: pushing any branch, merging to main, any other download you cannot justify as a normal dependency of this project.
- User checkpoints CP1 (motion and press feel) and CP2 (glass) are decided by an orchestrator decision agent; the decision is written to ${OV}/decisions/CP1.md (CP2.md later). Produce the recordings and frame strips your WP file asks for under ${OV}/perf/<your id>/ so the decision can be made from them.
- Merged packages are tagged merged/<ID> in ${R} (\`git -C ${R} tag -l 'merged/*'\`).
- The phone: serial 9a7c74f4 (Redmi Note 10 Pro, 120 Hz). If it is not attached, use the emulator AVD periflow_api35 for correctness only and say so; perf gates need the phone.`

function implPrompt(id, mode, extra) {
  const p = PKGS[id]
  return `You are the implementation agent for work package ${id} of the android-base overhaul. Mode: ${mode}.
Read in this order before editing: ${OV}/REQUIREMENTS.md; ${DESIGN}/wp/${p.file}; the MASTER-PLAN sections it names in ${DESIGN}/MASTER-PLAN.md, always including "Part D. Rules every implementation agent follows" and A11.3; the audit sections it cites (${OV}/audit/). ${mode !== 'start' ? `You are continuing earlier work: read ${OV}/handoff/${id}.md and \`git log --oneline -30\` in your worktree first.` : ''}
SCOPE OF THIS RUN: ${p.scope}
Worktree: ${OV}/wt/${p.wt} on branch wp/${p.wt}. If it does not exist, create it: git -C ${R} worktree add ${OV}/wt/${p.wt} -b wp/${p.wt} overhaul/multiplatform-v2 . Work only there. Gradle only via ${L} gradle, adb only via ${L} adb, long commands in the background with logs (Part D 14, 14a).
${ORCH_DECISIONS}
Currently merged or decided: ${JSON.stringify(doneKeys())}.
WAITING PROTOCOL: if part of your work must wait for something not yet merged or decided, do everything else first. If only blocked work remains, commit your progress, update ${OV}/handoff/${id}.md with exactly where you stopped and what remains (a fresh agent with only that note and git history will continue), and return state="waiting" with waiting_for drawn from ${JSON.stringify([...KNOWN])}. Whenever you return, list milestones reached so far (e.g. "WP03:phaseA", "WP04:phase1", "WP04:phase2").
After any wait, start by merging the integration branch into yours: git merge overhaul/multiplatform-v2 (resolve conflicts in your favour only inside your owned paths).
Return state="done" only when every acceptance command for this run's scope passes (the accept.sh log ends with ACCEPT OK), the handoff note is complete, and everything is committed on wp/${p.wt} with git status clean. Return state="blocked" only for something you cannot resolve soundly, with the evidence.
${extra || ''}`
}

async function runImpl(id, mode, extra) {
  const r = await agent(implPrompt(id, mode, extra), { label: `${id}:${mode}`, phase: 'Implement', schema: STATUS })
  if (!r) throw new Error(`${id} agent failed (${mode})`)
  for (const m of r.milestones || []) fire(m)
  return r
}

async function implementUntilDone(id) {
  let r = await runImpl(id, 'start')
  let rounds = 0
  while (r.state === 'waiting' && rounds < 8) {
    const unknown = r.waiting_for.filter(k => !KNOWN.has(k))
    if (unknown.length) { log(`${id} waits for ${unknown} which this workflow does not produce; stopping it`); return r }
    log(`${id} waiting for ${r.waiting_for.join(', ')}`)
    const w = await waitFor(r.waiting_for)
    if (w !== 'ok') { log(`${id}: ${w} will never arrive; stopping`); return { ...r, state: 'blocked', summary: `${w} never arrived. ${r.summary}` } }
    rounds++
    r = await runImpl(id, 'continue')
  }
  return r
}

async function reviewLoop(id) {
  const p = PKGS[id]
  for (let round = 1; round <= 3; round++) {
    const rv = await agent(`You are a fresh-context code reviewer for package ${id} of the android-base overhaul (MASTER-PLAN A12.8 step 1). Repository ${R}.
Review the diff: git -C ${R} diff overhaul/multiplatform-v2...wp/${p.wt} (start with --stat; read every changed source file; skim emitted/generated files and lockfiles).
Judge against ${DESIGN}/MASTER-PLAN.md "Part D" rules and A11.3 forbidden patterns, the package file ${DESIGN}/wp/${p.file} (scope and owned paths; this run's scope: ${p.scope}), and ${OV}/REQUIREMENTS.md.
BLOCKING: correctness bugs; perf hazards (animated state read in composition, alpha on full-screen or backdrop layers, per-event coroutine launches, infinite animations without LocalTabVisible/reduce-motion, allocations in draw); narrating or history comments, AI slop, commented-out code, needless abstraction; dead code left behind; missing or weakened tests; substantive edits outside owned paths; secrets; placeholders presented as finished work.
NON-BLOCKING: nits and suggestions. Do not modify anything. Cite file and line.`, { label: `review:${id}#${round}`, phase: 'Review', schema: REVIEW })
    if (!rv) throw new Error(`review of ${id} failed`)
    if (!rv.blocking.length) return { clean: true, notes: rv.nonblocking }
    log(`${id} review #${round}: ${rv.blocking.length} blocking`)
    const fx = await runImpl(id, 'fix', `A fresh-context reviewer found BLOCKING issues in your branch. Fix every one on your branch (commit with why-messages), re-run the affected acceptance commands, update the handoff note, and return state="done":\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking notes (address cheaply or record in the handoff): ${JSON.stringify(rv.nonblocking)}`)
    if (fx.state !== 'done') return { clean: false, notes: [`fix run ended ${fx.state}: ${fx.summary}`] }
  }
  return { clean: false, notes: ['review not clean after 3 rounds'] }
}

let mergeChain = Promise.resolve()
function mergePackage(id) {
  const p = PKGS[id]
  const run = async () => {
    for (let attempt = 1; attempt <= 2; attempt++) {
      const m = await agent(`You are the orchestrator's integrator. Merge package ${id} (branch wp/${p.wt}) into the integration branch overhaul/multiplatform-v2 in ${R}; that checkout is on the integration branch (verify, and that git status is clean).
1. git -C ${R} merge --no-ff wp/${p.wt} with a message that says why the package exists in one or two sentences, ending with the line: Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
   Conflicts: MASTER-PLAN B.1 rules (libs.versions.toml and settings.gradle.kts keep both sides' lines in order; emitted files are regenerated with --write; inside a package's owned paths the owner's side wins). If a conflict needs a substantive decision, git merge --abort and return ok=false, needs_package_fix=true with the details.
2. Integration checks (A12.8 step 2). Gradle only via ${L} gradle, in the background with logs under ${OV}/out/integration/${id}/, polled: generator unit tests (cd ${R}/generator && python3 -m unittest discover -s tests -t .); if they exist, python3 -m genkit.tokens --check and python3 -m genkit.core.catalogue --check; templates/android ./gradlew assembleDevDebug; if generator/tests/fixtures/v2/modules-compact.json exists, generate it and build assembleDevDebug testDevDebugUnitTest. Then the "Quick subset" section of ${DESIGN}/wp/${p.file}.
3. ${UI_ANDROID.has(id) ? `Perf smoke (A12.8 step 3) if templates/android/tools/perf/perf.sh and ${OV}/perf/WP04/final/summary.json both exist (adb via ${L} adb).` : 'No perf smoke for this package.'}
4. All green: git -C ${R} tag merged/${id}; return ok=true. A failure caused by this merge: fix it only if the fix is small and mechanical (commit with a why message); otherwise revert the merge (git revert -m 1 --no-edit HEAD), and return ok=false, needs_package_fix=true with the failing output.
Append a short entry (time, package, result, checks run) to ${OV}/merges.md.`, { label: `merge:${id}#${attempt}`, phase: 'Merge', schema: MERGE })
      if (!m) throw new Error(`merge of ${id} failed`)
      if (m.ok) { fire(id); return m }
      if (!m.needs_package_fix || attempt === 2) return m
      const fx = await runImpl(id, 'fix', `The orchestrator could not merge your branch: ${m.details}\nMerge overhaul/multiplatform-v2 into your branch, resolve, fix the failing checks, re-run your acceptance, update the handoff, and return state="done".`)
      if (fx.state !== 'done') return { ok: false, details: fx.summary, needs_package_fix: true }
    }
  }
  mergeChain = mergeChain.then(run, run)
  return mergeChain
}

async function packageFlow(id) {
  const w = await waitFor(PKGS[id].start)
  if (w !== 'ok') return { id, state: 'blocked', summary: `${w} never arrived` }
  const r = await implementUntilDone(id)
  if (r.state !== 'done') { log(`${id} ended ${r.state}: ${r.summary.slice(0, 300)}`); return { id, state: r.state, summary: r.summary } }
  const rv = await reviewLoop(id)
  if (!rv.clean) log(`${id}: review not clean, merging with notes recorded`)
  const m = await mergePackage(id)
  return { id, state: m && m.ok ? 'merged' : 'merge-failed', summary: r.summary, review: rv.notes, merge: m && m.details }
}

async function checkpoint1() {
  const w = await waitFor(['WP04:phase2', 'WP03:phaseA'])
  if (w !== 'ok') return `CP1 not decided: ${w}`
  const d = await agent(`You stand in for the user at checkpoint 1 (motion and press feel, MASTER-PLAN B.5 "User checkpoints") because the user is asleep and asked not to be asked anything.
Inputs: WP04's phase-2 measurements, recordings and frame strips under ${OV}/perf/WP04/ and its note ${OV}/handoff/WP04.md; WP03's phase-A recordings under ${OV}/perf/WP03/ and ${OV}/handoff/WP03.md; MASTER-PLAN A3.3, A3.10, A4.1, A4.3 and §2 gates.
Look at the frame strips (open the PNGs) and the numbers. Decide: the screen response (0.24 / 0.30 / 0.36 s or another value), press and selection spring values, and the pop rows if the warm-pop spike fell back. Criteria: iOS feel (UIKit push ≈ 0.35 s with no bounce, instant tab switch), the user's complaint that transitions felt "pushy" and glitchy, the §2 gates, and the BackGate hold window.
Write ${OV}/decisions/CP1.md: chosen values, reasons, and which recordings the user should watch to override later. Do not modify any repository.`, { label: 'checkpoint:CP1', phase: 'Checkpoint', schema: { type: 'object', properties: { summary: { type: 'string' } }, required: ['summary'] } })
  if (!d) throw new Error('CP1 decision failed')
  fire('CP1')
  return d.summary
}

const flows = Object.keys(PKGS).map(id => packageFlow(id)
  .catch(e => ({ id, state: 'error', summary: String(e) }))
  .then(res => { endedOf(id).r(true); return res }))
const cp1 = checkpoint1().catch(e => `CP1 error: ${e}`).then(res => { endedOf('CP1').r(true); return res })
const results = await Promise.all(flows)
return { results, cp1: await cp1, signals: doneKeys() }
