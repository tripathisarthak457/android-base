export const meta = {
  name: 'release-from-staging',
  description: "The Android release from staging (REQUIREMENTS 36, 44, 46, 50-57): every remaining package and approved item on its own staging-wp branch (Opus implementation, release-build phone checks and phone sessions run by Sonnet operators inside one phone queue with an Opus verifier after each, the combined Google/AOSP review with fix rounds, docs in the same change), merged into staging in trains behind a desktop gate and a release-build phone gate, then QA2, the WP12 knowledge pass, the one reformat, WP13 verification and report/FINAL.md, the REQUIREMENTS 52 review by dimension with adversarial verification, the REQUIREMENTS 46 matrix and the adversarial release gate. Stops for the user at the gate. Never pushes, never touches main, never PeriFlow. args.mode 'fast-track' runs only the REQUIREMENTS 56 stages to ready",
  whenToUse: "After staging returns (REQUIREMENTS 55: launch at once), or with args {mode: 'fast-track'} beside staging's last fixes (REQUIREMENTS 56)",
  phases: [
    { title: 'Intake', detail: 'read what staging left, route every gap to a stage, baseline gate 0, the compose-performance audit' },
    { title: 'Build', detail: 'per stage: Opus implementation, Sonnet phone runs with Opus verifiers, Opus reviews and fixes, docs, ready' },
    { title: 'Merge', detail: 'merge trains into staging: Opus integrator, Sonnet desktop and phone gate runs, Opus gate verifiers, bisect and revert on red, halt when a merge is left ungated' },
    { title: 'Verify', detail: 'QA2 and its add-on, the WP12 knowledge pass, the reformat, WP13 matrix and device gates, report/FINAL.md' },
    { title: 'Review', detail: 'REQUIREMENTS 52 by dimension: finders, three adversarial verifiers each, fixes, clean re-review; the REQUIREMENTS 46 matrix; the final re-measure' },
    { title: 'Gate', detail: 'the adversarial Android release gate, then stop for the user' },
  ],
}

// Plan: D:/projects/tools/.overhaul/plans/2026-10-20/release-pipeline.md (stages, order, merges, ETA, questions).
// Model routing (global CLAUDE.md section 10): Sonnet only for mechanical runs (APK builds, phone checks and phone
// sessions, the desktop and phone gate runs, bisect runs, WP13's profile, matrix and gate runs, R52's speed run, the
// final re-measure, the District capture, gate 0), each followed by an Opus verifier or analyst that re-derives the
// numbers from the raw files. Every implementation, fix, review, verdict, decision, plan and report is Opus (no model
// option: the session's model).
// The phone: one queue (withPhone) for every phone use of this run; only Sonnet operators inside it touch the phone, and
// each runs its whole session as one "locked adb bash <script>", so the adb lock spans the session against other
// workflows too. Implementers, fix agents, reviewers and verifiers never run adb.
// Safety: agents never push, never touch main, never delete a branch or worktree, never rewrite history, never touch
// PeriFlow, never publish anything (the sample-media release, the CI-only PR and the user's own checks are the
// orchestrator's, after the user's yes). The workflow stops at the gate; main needs the user's explicit go in chat,
// given to the orchestrator.

const MODE = (typeof args === 'object' && args && args.mode) ? args.mode : 'release'
const FAST = MODE === 'fast-track'
const TRAIN_MAX = (typeof args === 'object' && args && args.trainMax) ? args.trainMax : 3

const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const PD = `${OV}/plans/2026-10-20`
const SD = `${OV}/staging`
const RD = `${OV}/release`
const WTS = `${OV}/wt/STAGING`
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
const L = `${FAST ? 'LOCKED_PRIORITY=low ' : ''}${OV}/bin/locked`
const MW = `${OV}/bin/memwait`
const P = `${PD}/release-pipeline.md`
const TAG = 'merged/staging/'
const ORQ = `${RD}/ORCHESTRATOR-REQUESTS.md`

// The shared navigation files and the one order in which stages change them (release-pipeline.md 2.2).
const NAV = 'core/navigation stack/ (StackHost.kt, StackScene.kt, StackLayout.kt), back/ (BackCard.kt, StackBackHandler.kt, BackGate.kt), presentation/PresentationHost.kt, shell/tabs/KeptTabs.kt, shell/ShellState.kt, NavTransitionStyle.kt, and the navigation rows of tools/perf/gates.toml'
const NAV_ORDER = ['WP04-P8', 'FREEZE', 'REG', 'WP04-P4', 'WP04-P10', 'WP04-SHEET', 'P6', 'WP04-P10T', 'WP04-P11']

const RULES = `Rules (D:/projects/tools/CLAUDE.md and handover/HANDOVER.md section 6 hold; these are the ones this run leans on): Gradle only as ${L} gradle ./gradlew ..., heavy Python and npm through ${MW}; long commands in the background with logs, polled; never pass -Dorg.gradle.jvmargs or kotlin.daemon.jvmargs. THE PHONE: only this workflow's Sonnet phone operators use it, inside the workflow's phone queue, each running its whole session as ONE command ${L} adb bash <session script> (take adb before Gradle when a command needs both: ${L} adb ${L} gradle ...); every other agent (implementers, fix agents, reviewers, verifiers, analysts, integrators) never runs adb and reads only the files the operators pulled. Device files only under /data/local/tmp; screenrecord --size 720x1600; judge smoothness only on release or benchmark builds; restore every phone setting you change. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", staging explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never rewrite history, never publish or upload anything, never touch PeriFlow (WP11). Quality: Google/AOSP (${OV}/research/CODE-STANDARDS.md, the androidx Compose API guidelines, the template AGENTS.md: no Material, AppTheme only, strings in resources, 100 % Compose), Google jetpack-compose-performance skill (${SKILLS}) on every composable you touch; comments say why, never what; never weaken, skip or delete a test to get to green; stable versions only, in the version catalog. Every approved change ships with its docs in the same merge (REQUIREMENTS 50).`

// ---------- schemas ----------
const STATUS = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'waiting', 'blocked', 'skipped', 'lost'] },
  summary: { type: 'string' },
  heads: { type: 'array', items: { type: 'object', properties: { branch: { type: 'string' }, sha: { type: 'string' } }, required: ['branch', 'sha'] } },
  touched_measured_path: { type: 'boolean' },
  waiting_for: { type: 'array', items: { type: 'string' } },
  kinds_won: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'summary', 'heads', 'touched_measured_path', 'waiting_for'] }
const RAW = { type: 'object', properties: {
  runs: { type: 'array', items: { type: 'object', properties: { step: { type: 'string' }, command: { type: 'string' }, exit_code: { type: 'integer' }, log: { type: 'string' }, last_lines: { type: 'string' } }, required: ['step', 'command', 'exit_code', 'log', 'last_lines'] } },
  artifacts: { type: 'array', items: { type: 'string' } },
  problems: { type: 'array', items: { type: 'string' } },
}, required: ['runs', 'artifacts', 'problems'] }
const CHECK = { type: 'object', properties: {
  pass: { type: 'boolean' },
  verdict: { type: 'string', enum: ['pass', 'fail', 'inconclusive', 'won', 'lost', 'partly-won'] },
  kinds_won: { type: 'array', items: { type: 'string' } },
  rows: { type: 'array', items: { type: 'object', properties: { row: { type: 'string' }, a: { type: 'string' }, b: { type: 'string' }, limit: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['row', 'result', 'evidence'] } },
  failures: { type: 'array', items: { type: 'string' } },
  report_path: { type: 'string' },
}, required: ['pass', 'verdict', 'rows', 'failures', 'report_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }
const MERGE = { type: 'object', properties: {
  ok: { type: 'boolean' },
  staging_head: { type: 'string' },
  merge_commit: { type: 'string' },
  needs_stage_fix: { type: 'boolean' },
  details: { type: 'string' },
}, required: ['ok', 'staging_head', 'needs_stage_fix', 'details'] }
const GATEV = { type: 'object', properties: {
  green: { type: 'boolean' },
  phone_needed: { type: 'boolean' },
  culprits: { type: 'array', items: { type: 'string' } },
  rows: { type: 'array', items: { type: 'object', properties: { row: { type: 'string' }, value: { type: 'string' }, previous: { type: 'string' }, limit: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['row', 'result'] } },
  details: { type: 'string' },
  report_path: { type: 'string' },
}, required: ['green', 'phone_needed', 'culprits', 'rows', 'details', 'report_path'] }
const INTAKE = { type: 'object', properties: {
  staging_head: { type: 'string' },
  regression_open: { type: 'boolean' },
  gaps: { type: 'array', items: { type: 'object', properties: { stage: { type: 'string' }, items: { type: 'array', items: { type: 'string' } } }, required: ['stage', 'items'] } },
  skip: { type: 'array', items: { type: 'object', properties: { stage: { type: 'string' }, reason: { type: 'string' } }, required: ['stage', 'reason'] } },
  decisions_logged: { type: 'array', items: { type: 'string' } },
  notes: { type: 'string' },
}, required: ['staging_head', 'regression_open', 'gaps', 'skip', 'decisions_logged', 'notes'] }
const FINDINGS = { type: 'object', properties: {
  findings: { type: 'array', items: { type: 'object', properties: {
    dimension: { type: 'string' }, area: { type: 'string' }, file: { type: 'string' }, line: { type: 'integer' },
    problem: { type: 'string' }, why_blocking: { type: 'string' }, evidence: { type: 'string' }, recheck: { type: 'boolean' },
    stage: { type: 'string' },
  }, required: ['dimension', 'area', 'file', 'problem', 'why_blocking', 'evidence'] } },
}, required: ['findings'] }
const VOTE = { type: 'object', properties: { refuted: { type: 'boolean' }, reason: { type: 'string' } }, required: ['refuted', 'reason'] }
const MATRIX = { type: 'object', properties: {
  rows: { type: 'array', items: { type: 'object', properties: {
    req: { type: 'string' }, verdict: { type: 'string', enum: ['met', 'partly', 'not-met', 'deferred'] },
    where: { type: 'string' }, proof: { type: 'string' }, best_way: { type: 'string' }, owner_area: { type: 'string' },
  }, required: ['req', 'verdict', 'where', 'proof', 'best_way', 'owner_area'] } },
  summary: { type: 'string' },
}, required: ['rows', 'summary'] }
const GATE = { type: 'object', properties: { go: { type: 'boolean' }, gaps: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['go', 'gaps', 'summary'] }
const DECISION = { type: 'object', properties: { decided: { type: 'boolean' }, summary: { type: 'string' } }, required: ['decided', 'summary'] }

// ---------- signals: ready and merged per stage ----------
const signals = {}
function signal(k) {
  if (!signals[k]) { let r; const p = new Promise(res => { r = res }); signals[k] = { p, r, done: false } }
  return signals[k]
}
function isDone(k) { return !!(signals[k] && signals[k].done) }
function fire(k) {
  const s = signal(k)
  if (!s.done) { s.done = true; s.r(true); log(`signal: ${k}`); kickMerger() }
}
const ended = {}
function endedOf(id) {
  if (!ended[id]) { let r; const p = new Promise(res => { r = res }); ended[id] = { p, r } }
  return ended[id]
}
function waitFor(keys) {
  if (!keys.length) return Promise.resolve('ok')
  const all = Promise.all(keys.map(k => signal(k).p)).then(() => 'ok')
  const dead = keys.map(k => endedOf(k.split(':')[0]).p.then(() => (signal(k).done ? new Promise(() => {}) : `dead:${k}`)))
  return Promise.race([all, ...dead])
}

// ---------- the phone: one queue for every phone use of this run, priority 0 (merge gates) first ----------
const phoneWaiters = []
let phoneBusy = false
let phoneSeq = 0
function nextPhone() {
  if (phoneBusy || !phoneWaiters.length) return
  phoneWaiters.sort((a, b) => a.prio - b.prio || a.seq - b.seq)
  const w = phoneWaiters.shift()
  phoneBusy = true
  w.run()
}
function withPhone(prio, fn) {
  return new Promise((resolve, reject) => {
    phoneWaiters.push({ prio, seq: phoneSeq++, run: async () => {
      try { resolve(await fn()) } catch (e) { reject(e) } finally { phoneBusy = false; nextPhone() }
    } })
    nextPhone()
  })
}

// ---------- long implementation runs: at most three at once (two on the fast track) ----------
const IMPL_SLOTS = FAST ? 2 : 3
let freeSlots = IMPL_SLOTS
const slotQueue = []
async function withImplSlot(fn) {
  if (freeSlots > 0) freeSlots--
  else await new Promise(res => slotQueue.push(res))
  try { return await fn() } finally { const next = slotQueue.shift(); if (next) next(); else freeSlots++ }
}

// ---------- lanes: one worktree per package line, one stage at a time ----------
const lanes = {}
function onLanes(names, fn) {
  const prevs = names.map(n => lanes[n] || Promise.resolve())
  const run = Promise.all(prevs.map(p => p.catch(() => null))).then(() => fn())
  const settled = run.catch(() => null)
  names.forEach(n => { lanes[n] = settled })
  return run
}
function wtOf(lane) { return lane === 'MEDIA' ? `${OV}/wt/MEDIA` : `${OV}/wt/sw-${lane}` }
// Writers of a lane worktree (implementation and fix runs, APK builds) take its tree lock, so an APK is never built
// while an agent edits the same tree; reviewers read branches, not the tree, and take nothing.
const treeTail = {}
function withTrees(names, fn) {
  const keys = [...new Set(names)].sort()
  const prev = Promise.all(keys.map(k => (treeTail[k] || Promise.resolve()).catch(() => null)))
  let release
  const mine = new Promise(r => { release = r })
  keys.forEach(k => { treeTail[k] = prev.then(() => mine) })
  return prev.then(() => fn()).finally(() => release())
}

// ---------- the stages (release-pipeline.md section 1) ----------
// start: stages that must be ready first (the stage builds on their ready commits); after: stages that must be merged
// before the phone check and the merge. holds: files another stage changes first (release-pipeline.md 2.2).
// prio: phone priority (1 critical path, 2 others, 3 research). gate: the declared kind; the train's desktop verifier
// decides the phone gate from the measured APKs (section 3.6), whatever is declared.
const CRIT = ['HUD-O1', 'REG', 'WP04-P8', 'FREEZE', 'WP04-P4', 'WP04-P10', 'WP04-SHEET', 'P6', 'WP02-M3', 'MB', 'WP06-CARRY', 'CAT', 'QA2', 'QA2-ADDON', 'WP13', 'R52']
const S = (o) => Object.assign({ start: [], after: [], holds: [], phone: false, gate: 'phone', docs: true, kind: 'android', fast: false, trial: false, maxChecks: 3, maxWaits: 6, merge: true }, o, { prio: CRIT.includes(o.id) ? 1 : (o.prio || 2) })
const STAGES = [
  S({ id: 'HUD-O1', lanes: ['HUD'], branches: ['staging-wp/HUD-O1'], reqs: '40, 42, 49, 51', phone: true,
    plan: `${PD}/overlay.md section 0 (what staging already did, decided by its table), section 2 (O1: C1, C2, C3) and section 3 (O9: C4 only if session A arm 2b shows a cost); ${OV}/research/hud-metrics/DESIGN.md`,
    scope: `Dev tooling out of every build the perf harness measures (C1, with the guard on every measured variant and its mutation check), perf.py refusing to gate an APK that carries dev tooling (C2), the baseline profiles re-recorded on the measured build without tooling (C3: through a phone session, see PHONE SESSIONS), the DEV badge opaque at rest only if arm 2b of session A shows a cost (C4; ${SD}/apks/badge-modulatealpha-not-committed.patch and TEST-REPORT's trace are evidence for that arm). Session A of overlay.md 2.4 is your phone check. Coverage audit H-14 (perf.hud=off) re-checked.` }),
  S({ id: 'WP12-DOCS', lanes: ['WP12'], branches: ['staging-wp/WP12-DOCS'], reqs: '25, 50', kind: 'docs', gate: 'dex', docs: false, fast: true,
    plan: `${OV}/docs-draft/PERFORMANCE.md, ${OV}/docs-draft/AGENTS-rules.md, ${OV}/docs-draft/PENDING.md, ${OV}/handoff/WP12.md`,
    scope: `The docs base every later merge writes into (build order step 2): templates/android/docs/PERFORMANCE.md and the AGENTS.md "Smooth and light" section from the drafts, every [verify] item and cited name checked on staging, nothing promised that staging lacks (PENDING.md keeps those for their merges), the generator repository AGENTS.md pointer. Before ready, re-check every claim against staging's newest head.` }),
  S({ id: 'REG', lanes: ['REG'], branches: ['staging-wp/REG'], reqs: '19, 41, 42, 46, 47', phone: true, maxWaits: 16, start: ['HUD-O1'], after: ['HUD-O1', 'WP04-P8', 'FREEZE'],
    holds: [{ until: 'FREEZE', files: NAV }],
    cond: () => !!(intake && intake.regression_open),
    plan: `${SD}/TEST-REPORT.md "Fix loop" and "Still open", ${SD}/MERGE-NOTES.md section 15, ${OV}/out/WP04-perf/back-google/RESULT.md`,
    scope: `The phone regressions staging left open against T + C1 (back gesture, push half of push/pop, root-list scroll, navigation stress; bisected to the WP03 merge e3dfd908/d5a1c898). Your diagnosis runs as PHONE SESSIONS (below), one experiment or trace set per session: re-measure first on a build without dev tooling (HUD-O1 is merged), then Perfetto traces of a push and a scroll on a6332dc4 against d5a1c898 with GPU counters and the slow-issue-draw-commands frames, then each candidate fix as an arm built from a local experiment branch. Fix at the root. The user kept the Floating Utility bar (REQUIREMENTS 47): make its drawing inside followCovered cheaper (shell/BarAnchor.kt, shell/ShellBar.kt), never dock it; find the second WP03 cause behind the gesture and scroll rows. Re-record the baseline profiles (a phone session). Your phone check is the proof: the four rows back at T + C1's level A-B-A-B in one session against ${OV}/out/WP04-perf/bg-c1, every REQUIREMENTS 42 release row green, a regression test for each code cause, no limit loosened. In the order of the shared navigation files you come after WP04-P8 and FREEZE (your branch merges staging after they merge, your proof runs on that tree) and before WP04-P4 (it starts from your ready commit).` }),
  S({ id: 'WP03-SEAMS', lanes: ['WP03'], branches: ['staging-wp/WP03-SEAMS'], reqs: '51', after: ['WP12-DOCS', 'HUD-O1'],
    plan: `${PD}/freeze.md #1 and section 6.1, ${PD}/sheet-picture-trial.md W3-1, ${PD}/keyboard-slides.md P10-1`,
    scope: `The inert seams in one merge (build order step 3): TabVisibility.isStill (always still), MotionTextures (off), Modifier.horizontalGestures(); their KDoc is their docs.` }),
  S({ id: 'HUD-C7', lanes: ['HUD'], branches: ['staging-wp/HUD-C7'], reqs: '49', start: ['HUD-O1'], after: ['HUD-O1'], gate: 'dex',
    plan: `${PD}/overlay.md sections 4 (O2: C5, C6), 5.1 (C7) and 11 (ProfileStatus.NotInProject removed in its own commit)`,
    scope: `C5, C6, C7 and the NotInProject removal; C7 must be in staging before the freeze's #8 test is written (build order step 9).` }),
  S({ id: 'WP04-P8', lanes: ['WP04'], branches: ['staging-wp/WP04-P8'], reqs: '51 (P8)', phone: true, after: ['HUD-O1', 'WP12-DOCS'],
    plan: `${PD}/keyboard-slides.md section 2 (P8-1, P8-2, P8-3)`, scope: `P8 in full; it reaches staging before WP04-P4 (both edit StackScene.slideTo) and is the first of the shared navigation files' order.` }),
  S({ id: 'FREEZE', lanes: ['WP03', 'WP04'], branches: ['staging-wp/WP03-FREEZE', 'staging-wp/WP04-FREEZE'], reqs: '51 (P1, P2), 22', phone: true,
    start: ['WP03-SEAMS', 'HUD-C7', 'WP04-P8'], after: ['WP03-SEAMS', 'HUD-C7', 'WP04-P8', 'HUD-O1', 'WP12-DOCS'], twoHead: true,
    plan: `${PD}/freeze.md sections 1, 2, 4 (4.1: one merge with both heads), 5 and 6.2`,
    halves: { WP03: 'freeze #2-#5, #8 and the docs #11', WP04: 'freeze #6, #7, #9 and #7b' },
    scope: `P1 and P2: the WP03 half and the WP04 half on their two branches, the P1 and P2 phone checks of freeze.md section 5 (built from a local check branch check/FREEZE that merges both heads, never pushed), the docs #11; they reach staging as one merge commit with both heads as parents.` }),
  S({ id: 'WP03-CARRY', lanes: ['WP03C'], branches: ['staging-wp/WP03-CARRY'], reqs: '21, 23, 33, 34, 36, 38, 55(c)', phone: true,
    start: ['AUDIT-CP', 'REG'], after: ['REG', 'HUD-O1', 'WP12-DOCS', 'FREEZE'],
    holds: [{ until: 'FREEZE', files: 'core/designsystem foundation/draw/Skeleton.kt and foundation/layout/Chrome.kt (freeze #2 holds the skeleton sweep, #11 documents isStill in Chrome.kt)' }],
    plan: `${SD}/CARRY-OVER.md (WP03 font preload), ${SD}/STAGING-PLAN.md section 6 (WP03), ${OV}/handoff/WP03.md and WP03-skel.md, ${OV}/qa/shimmer/, ${OV}/research/COMPOSE-PERF-AUDIT.md (its design-system findings)`,
    scope: `The font preload from the startup gate (cold start measured); items 3 (AppBottomActionBar), 4, 6 (filled icons), 8 (same-pass chrome only if a recording shows the frame), 10 scaffold side (AppScaffold writes ScreenChrome.topBar, .background, .bottomBar; AppTheme provides LocalWindowTopChrome per window, in Chrome.kt); catalog demos on AppSheetNavHost; the shimmer phone proof (qa/shimmer/VERIFY.md) and its sweep from 0.69 % janky to the 0.5 % target (decisions/LOG.md Oct 19 22:08; Skeleton.kt); list-row glyphs that grow at large text (REQUIREMENTS 55(c)); H-12 (AppScaffold padding a pane's inner edge). Do the items outside the held files first.` }),
  S({ id: 'MEDIA', lanes: ['MEDIA'], branches: ['staging-wp/MEDIA'], reqs: '51 (P3, P7, P9, District P3), 53, 50', phone: true,
    after: ['HUD-O1', 'WP12-DOCS', 'ASSETS'],
    plan: `${PD}/lists-images.md and ${PD}/media.md (the newest revision; 9.2 and 9.3 ANSWERED), ${PD}/media-lookup.md, ${OV}/scripts/fast-track-media.js and its handoff ${OV}/handoff/MEDIA.md, ${OV}/decisions/LOG.md (Oct 23 22:09)`,
    scope: `Adopt the fast-track work (REQUIREMENTS 56; the orchestrator stops scripts/fast-track-media.js before this run, so wt/MEDIA is yours; uncommitted work you find there is the stopped run's: review it, keep what is right, commit it): read the handoff's Progress, its waiting list and its gate verdict; finish every step that waited for staging's fix. media.md 9.1 took its recommended answer (Coil off by default, in Everything only). media.md 9.2 is the USER'S answer (Oct 23 22:09): the sample clips are SELF-HOSTED as release assets of release sample-media-v1 of tripathisarthak457/android-base, which the orchestrator creates after you prepare and review the files, never an agent; the handoff's run-1 decision 2 (Wikimedia Commons hotlinked) is superseded: correct it in the handoff and never point a scene, a test or a gate at a Commons URL. If the clip set is not prepared yet, prepare and review it as media.md 9.2 and 9.3 say (downloads only into %TEMP%\\mrev\\), put the line "Orchestrator requests: create release sample-media-v1 with <files>" at the top of the handoff, and when what remains needs the URLs return waiting with waiting_for ["ASSETS"]; the workflow tells you when the release is published and checked (200 and a 206 Range answer for every asset). Your phone check (media.md sessions 1 and 2, as phone sessions and the check) runs on a measured build without dev tooling (HUD-O1 is merged) against the self-hosted set, keeping the fast-track numbers only where the verifier proves the measured tree and the media set unchanged. Your docs sections go into the docs base files as staging has them after WP12-DOCS.` }),
  S({ id: 'WP03-P5', lanes: ['WP03'], branches: ['staging-wp/WP03-P5'], reqs: '51 (P5)', phone: true, start: ['FREEZE', 'WP03-CARRY'], after: ['FREEZE', 'WP03-CARRY'],
    plan: `${PD}/keyboard-slides.md section 1`, scope: `P5-1, P5-2, its phone check, P5-3.` }),
  S({ id: 'WP03-FADE', lanes: ['WP03'], branches: ['staging-wp/WP03-FADE'], reqs: '51 (P1, image fade)', phone: true, start: ['FREEZE', 'MEDIA'], after: ['FREEZE', 'MEDIA'],
    plan: `${PD}/freeze.md #10, section 1.4 scene 5 and section 6.3`,
    scope: `StillFade on AppNetworkImage's fade (MEDIA brought lists-images W3-1); P1 scene 5. If W3-1 added no fade, return state "skipped" with the reason (freeze decision 2).` }),
  S({ id: 'WP04-SHEET', lanes: ['WP04S'], branches: ['staging-wp/WP04-SHEET'], reqs: '21, 23, 24, 33, 41, 42', phone: true,
    start: ['WP03-CARRY', 'REG', 'AUDIT-CP'], after: ['WP03-CARRY', 'REG', 'HUD-O1', 'WP12-DOCS', 'WP04-P10'],
    holds: [{ until: 'WP04-P10', files: NAV }],
    plan: `${SD}/STAGING-PLAN.md section 6 (WP04), ${OV}/research/SHEET-SPEC.md section 19.2, ${OV}/handoff/WP04.md and WP04-perf.md, ${OV}/followups.md "WP04", ${OV}/out/WP04-perf/back-google/DESIGN.md section 6`,
    scope: `SHEET-SPEC 19.2 (Presentation.Sheet(SheetSpec), presentation/sheet, the card stack behind modal sheets, shell seams 11.3, Back B-36 and B-43, perf rows 16.1, tests 15.3 and 15.4); WP03 item 5 where the shell still has its own transitions; item 10's shell side (LocalWindowTopChrome from the front screen, the bottom-edge fade K11, the AD-15 call site); BarLook.rule deleted; the scrim fold for the back gesture's hitch row; the K-4 renames; the back_card_held release row; the zoom transition M5 (NavTransitionStyle.Zoom, WP02-M3 uses it); the first-swipe shader warm-up S6/S7 and the first fling after an install on the merged tree (REQUIREMENTS 41). Work in your own files (presentation/sheet and the new sheet code) first; everything in the shared navigation files waits for WP04-P10's merge (you follow it in their order, and P6 starts from your ready commit).` }),
  S({ id: 'WP04-P4', lanes: ['WP04'], branches: ['staging-wp/WP04-P4'], reqs: '51 (P4)', phone: true, start: ['FREEZE', 'WP04-P8', 'REG'], after: ['FREEZE', 'WP04-P8', 'REG'],
    plan: `${PD}/freeze.md section 3 (#12-#16) and section 6.4`, scope: `P4, opt-in per screen; #15 kept only if it wins on the phone.` }),
  S({ id: 'WP04-P10', lanes: ['WP04'], branches: ['staging-wp/WP04-P10'], reqs: '51 (P10)', phone: true, start: ['WP04-P4', 'WP03-SEAMS'], after: ['WP04-P4', 'WP03-SEAMS'],
    plan: `${PD}/keyboard-slides.md section 3 (P10-2, session N, P10-2d)`,
    scope: `P10-2, off by default, disabled on horizontal scrollers, predictive back kept; canStartDrag() reads !gate.moving && !isWaiting; Q1 takes its recommended rule (REQUIREMENTS 55(d)).` }),
  S({ id: 'P6', lanes: ['WP03', 'WP04'], branches: ['staging-wp/WP03-P6', 'staging-wp/WP04-P6'], reqs: '51 (P6)', phone: true, trial: true, twoHead: true,
    start: ['WP04-P10', 'WP04-SHEET'], after: ['WP04-P10', 'WP04-SHEET', 'MEDIA', 'FREEZE', 'WP04-P8', 'WP04-P4'],
    plan: `${PD}/sheet-picture-trial.md (sections 3-8; the win condition of section 5 is fixed before measuring)`,
    halves: { WP03: 'W3-2, then W3-3 (keep) or W3-R (remove)', WP04: 'W4-1 to W4-3, then W4-4 (keep) or W4-R (remove)' },
    scope: `The P6 trial: the switch-guarded code on both branches, the trial session (built from a local check branch check/P6 that merges both heads, never pushed), then per kind keep (the exception written in the rule, the code comments and the docs) or remove; a confirmation run of the settled configuration's win rows (PHONE.md section Confirm) decides what stays. Write ${RD}/P6/REPORT.md with the numbers and the exception's final text for the user (information; REQUIREMENTS 51 fixed the rule). Return kinds_won.` }),
  S({ id: 'WP04-P10T', lanes: ['WP04'], branches: ['staging-wp/WP04-P10T'], reqs: '51 (P10 texture)', phone: true, trial: true, start: ['P6'], after: ['P6'],
    plan: `${PD}/keyboard-slides.md section 3 (P10-3, session T, rule T, P10-3d or P10-3R)`,
    scope: `Q2 takes its recommended answer (REQUIREMENTS 55(d)): the texture, kept only if rule T wins, confirmed on the settled code (PHONE.md section Confirm). Add the catalog demo yourself (CAT does not wait for you).` }),
  S({ id: 'WP04-P11', lanes: ['WP04'], branches: ['staging-wp/WP04-P11'], reqs: '51 (P11)', phone: true, trial: true, start: ['WP04-P10T'], after: ['WP04-P10T'],
    cond: () => !!(results.P6 && results.P6.kinds_won && results.P6.kinds_won.length),
    plan: `${PD}/keyboard-slides.md section 4`,
    scope: `P11 only because P6 kept at least one kind after its confirmation run. If the push kind was kept, P11-1 as planned; if P6 kept only part, Q3's recommended answer applies (REQUIREMENTS 55(d)): run P11's own texture A/B (section 4.6) and ship only if it wins and its confirmation run holds. Add the catalog demo yourself.` }),
  S({ id: 'WP06-CARRY', lanes: ['WP06'], branches: ['staging-wp/WP06-CARRY'], reqs: '8, 18, 30, 33, 40', phone: true,
    start: ['WP04-SHEET', 'WP03-CARRY'], after: ['WP04-SHEET', 'WP03-CARRY'],
    plan: `${SD}/STAGING-PLAN.md section 6 (WP06), ${OV}/handoff/WP06.md, ${OV}/followups.md "WP06", ${OV}/research/SHEET-SPEC.md section 19.8`,
    scope: `K13 (the badge's top bound from LocalWindowTopChrome, read in offset), SHEET-SPEC 19.8 (the DevTools panel as an AppSheet), H-11, H-4 if INTAKE says it is still open, the followups (field jank attribution S16, repository fakes in testFixtures, the badge over a rail or sidebar, windows above the lock cover). wp/HUD-statekeys stays out (REQUIREMENTS 40).` }),
  S({ id: 'NET', lanes: ['NET'], branches: ['staging-wp/NET'], reqs: '39', phone: true, after: ['HUD-O1', 'WP12-DOCS'],
    plan: `${OV}/REQUIREMENTS.md item 39, ${OV}/scripts/to-release.js (the NET scope), ${OV}/research/NET-CRONET-BROTLI-NOTES.md, ${OV}/followups.md (what to check once NET lands), ${PD}/media.md test 6.21`,
    scope: `REQUIREMENTS 39 exactly, as to-release.js's NET scope says: the opt-in Cronet transport with its fallbacks, Brotli on by default for every app, generator feature and tests, the phone measurements under ${OV}/perf/NET/ (your phone check). Coordinate with MEDIA: video requests keep Accept-Encoding: identity; whichever of you merges second resolves.` }),
  S({ id: 'WP05-BAR', lanes: ['WP05'], branches: ['staging-wp/WP05-BAR'], reqs: '35, 37, 22, 42', phone: true, cp2: true,
    start: ['REG', 'AUDIT-CP'], after: ['REG', 'HUD-O1', 'WP12-DOCS'],
    plan: `${OV}/handoff/WP05.md, ${OV}/handoff/WP05-glass.md, ${SD}/CARRY-OVER.md (WP05-glass), ${SD}/MERGE-NOTES.md section 10, ${SD}/TEST-REPORT.md (Glass rows), ${OV}/design/MASTER-PLAN.md B.5 (CP2) and A4.9`,
    scope: `Apply ${SD}/glass/glass-on-staging-f7b6f76c.patch; fix the five failing lens tests at their cause (the geometry is part of the glass look that CP2 settles); the Live bar laid out at the bottom like the other tiers; the tier budget (+2 ms GPU, the watcher demotes Live within 20-30 s); SceneBackdrop keyed on localPositionOf; then write the CP2 procedure as PHONE.md check 100 (per-tier recordings and numbers: tap morph, lens drag, scroll under the bar, minimize, two or three blur and tint variants, raw files under ${OV}/perf/WP05/cp2/) and return state "waiting" with waiting_for ["CP2"]; after ${OV}/decisions/CP2.md exists, apply its values to the tokens, set glass_live limits for the REQUIREMENTS 42 rows from measurements (your phone check), and give features/liquidglass.toml back its description of the bar.` }),
  S({ id: 'WP05-SET', lanes: ['WP05'], branches: ['staging-wp/WP05-SET'], reqs: '37', phone: true,
    start: ['WP05-BAR', 'WP04-SHEET', 'WP03-CARRY'], after: ['WP05-BAR', 'WP04-SHEET', 'WP03-CARRY'],
    plan: `${OV}/handoff/WP05-glass.md section 1 (the playground read from source), ${OV}/research/SHEET-SPEC.md section 19.3, ${OV}/decisions/CP2.md`,
    scope: `Every other REQUIREMENTS 37 component at the Live tier, made cheap: glass top-bar buttons, toggle, slider, segmented control, buttons, alert, toast, the Glass sheet on SheetSurfaceNode, the catalog Glass playground section; the full effect list as acceptance (magnification, rim dispersion, specular and rim light, press swell, drag-to-select with velocity, the travelling tap morph); the side-by-side strips against Kyant's playground, which the user has installed on the Redmi (REQUIREMENTS 57), as part of your phone check.` }),
  S({ id: 'WP02-CONT', lanes: ['WP02'], branches: ['staging-wp/WP02-CONT'], reqs: '6, 9, 10, 23, 26, 38', kind: 'python', gate: 'dex', docs: false, fast: true,
    plan: `${OV}/handoff/WP02-M2.md ("Continuation plan"), ${OV}/handoff/WP02-M2-RESUME.md, ${SD}/STAGING-PLAN.md section 6 (WP02), ${OV}/followups.md, ${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md`,
    scope: `Continuation steps 2a-6 (AndroidLogger move, the scaffold on WP04's chrome with its audit findings, database scaffolding on WP06's shape, the sharedTransitions decision, accept.win.sh), H-8, the per-project test time limit, the AdaptiveAuditTest guard (REQUIREMENTS 23.8), tools/size_budgets.toml unless staging wrote it, the stability check's order fix re-checked on every preset. Generator and build-logic; nothing in the shell or the bar. The AndroidLogger move changes :app, so its train's desktop verifier compares the measured APK and runs the phone gate if it differs.` }),
  S({ id: 'WP14', lanes: ['WP14'], branches: ['staging-wp/WP14'], reqs: '10', kind: 'python', gate: 'dex', docs: false, fast: true, start: ['WP02-CONT'], after: ['WP02-CONT'],
    plan: `${OV}/design/wp/WP14.md`, scope: `The whole package, built on WP02-CONT's ready commit (the finished scaffolder): upgrade_project.py with --diff, make_modular.py, add_tab.py, add_language.py, add_feature.py and remove_feature.py in both modes with the v1 refusal, tests, matrix row 14.` }),
  S({ id: 'WP10-WEB', lanes: ['WP10'], branches: ['staging-wp/WP10-WEB'], reqs: '13, 14, 36', kind: 'web', gate: 'dex', docs: false, fast: true,
    plan: `${SD}/MERGE-NOTES.md section 13, ${OV}/handoff/WP10.md (release blockers), ${OV}/design/wp/WP12.md (site copy)`,
    scope: `The site's previews made to match the template (the tab bar preview knows WP03's retired Chunky and Minimal, not its Pill/Subtle indicator; the chip's border), docs/telemetry-and-privacy.md and its links from hero, FAQ and footer, the included.tsx and FAQ claims true, the Android-only picker re-checked; site tests, tsc, next build, npm audit. You own the FAQ, components/included.tsx, the telemetry page and its links until you merge; WP12-KNOW reviews them after.` }),
  S({ id: 'WP13-PREP', lanes: ['WP13'], branches: ['staging-wp/WP13-PREP'], reqs: '9, 36', kind: 'python', gate: 'dex', docs: false, fast: true, start: ['WP14'], after: ['WP02-CONT', 'WP14'],
    plan: `${OV}/design/wp/WP13.md steps 1, 2, 7, 8; ${OV}/followups.md "WIN" and "CI"`,
    scope: `tools/matrix.py (14 rows, --wide, --list-json, the size-budget checker), the ci.yml rewrite of WP13.md step 7 with the windows-latest generator job, tools/check_versions.py, the static matrix's extensions, the WIN and CI follow-ups. No template code. Built on WP14's ready commit (matrix row 14).` }),
  S({ id: 'HUD-METRICS', lanes: ['HUD'], branches: ['staging-wp/HUD-METRICS'], reqs: '49, 51', start: ['HUD-C7', 'AUDIT-CP'], after: ['HUD-C7'], gate: 'dex',
    plan: `${PD}/overlay.md sections 5.2-8 and 12 (C8-C14), ${OV}/research/COMPOSE-PERF-AUDIT.md (its core/perfhud findings)`, scope: `C8 to C14, each green and reviewed before the next.` }),
  S({ id: 'WP02-M3', lanes: ['WP02'], branches: ['staging-wp/WP02-M3'], reqs: '6, 21, 22, 23, 27', phone: true,
    start: ['WP02-CONT', 'WP04-SHEET', 'FREEZE', 'WP04-P4', 'WP03-CARRY'], after: ['WP02-CONT', 'WP04-SHEET', 'FREEZE', 'WP04-P4', 'WP03-CARRY'],
    plan: `${OV}/handoff/WP02-M3.md, ${OV}/design/wp/WP02.md "Milestone M3", ${OV}/research/STANDARDS-PLAN.md, ${OV}/research/ADAPTIVE-PLAN.md, ${OV}/followups.md`,
    scope: `Milestone M3 and every note in its handoff (first composition final, press-down prefetch, contentType and keys, AppPagingList state, row motion, iOS fling, keyboard in layout, the zoom with sharedTransitions, AppPullToRefresh, per-feature recomposition tests), ST-01, AD-30, ST-07, SampleListViewModel Empty, H-7. Your phone check is the full re-gate of everything, standard and lean (A12.3).` }),
  S({ id: 'HUD-CAL', lanes: ['HUD'], branches: ['staging-wp/HUD-CAL'], reqs: '40, 49, 51, 53', phone: true, gate: 'dex',
    start: ['HUD-METRICS', 'MEDIA'], after: ['HUD-METRICS', 'MEDIA', 'FREEZE', 'WP03-P5', 'WP04-P8', 'WP04-P4', 'WP04-P10'],
    plan: `${PD}/overlay.md section 11 (C15, session B), ${PD}/media.md section 2.8 (H1, H2)`,
    scope: `H1 and H2 (decodes, video frames and decoders in the overlay; their fences in BaseAppApplication must leave the measured release APK's dex unchanged, which the train's desktop verifier checks), C15 calibration with session B (your phone check), ${OV}/perf/HUD-metrics/SHOW.md with a recording and an exported report.` }),
  S({ id: 'MB', lanes: ['MB'], branches: ['staging-wp/MB'], reqs: '31', phone: true, start: ['WP02-M3', 'WP14'], after: ['WP02-M3', 'WP14'],
    plan: `${OV}/REQUIREMENTS.md item 31, ${OV}/scripts/to-release.js (the MB scope)`,
    scope: `The optional materialbridge feature exactly as to-release.js's MB scope says (write the short design at the top of ${OV}/handoff/MB.md first); off by default; Material imports confined by code rules; a Material-free build when off, proven by the dependency report.` }),
  S({ id: 'CAT', lanes: ['CAT'], branches: ['staging-wp/CAT'], reqs: '43', phone: true, start: ['WP03-CARRY', 'WP04-SHEET'],
    after: ['WP05-SET', 'HUD-CAL', 'MEDIA', 'P6', 'WP06-CARRY', 'NET', 'WP03-P5', 'MB', 'WP03-FADE', 'WP02-M3'],
    plan: `${OV}/REQUIREMENTS.md item 43, ${OV}/scripts/to-release.js (the CAT scope)`,
    scope: `The catalog as the showcase of everything merged, exactly as to-release.js's CAT scope says; P10T and P11 bring their own demos, so do not wait for them. Your phone check: every page, a tour recording, perf.sh rows for the scrolling pages.` }),
  S({ id: 'QA2', lanes: ['QA2'], branches: ['staging-wp/QA2'], reqs: '17, 18, 21, 23, 24, 28, 29, 30, 32, 33, 34, 35, 36, 37, 40, 41', phone: true, docs: false, maxChecks: 4,
    start: ['CAT', 'MB', 'WP02-M3'], after: ['CAT', 'MB', 'WP02-M3'],
    plan: `${OV}/scripts/to-release.js (the QA2 scope), ${OV}/qa/r1/DEFECTS.md, ${OV}/research/ios-motion-catalogue.md, ${OV}/decisions/CP1.md, ${OV}/decisions/CP2.md, ${OV}/research/SHEET-SPEC.md`,
    scope: `QA round 2 on the merged template: write the capture procedure (${RD}/QA2/PHONE.md, its Run section as the session script) for the Sonnet operator: every screen in light and dark, at font scale 1.0 and 1.5, in a large window (wm size 1600x2560, wm density 320, always restored), the recordings (tab switch, push and pop, predictive back, a back swipe during a push, pull to refresh, sheets, the glass bar and the lens tap morph, the shimmer), TalkBack proof (REQUIREMENTS 36), the fling against the iOS curve and the first fling after an install (REQUIREMENTS 41). The verifier writes ${OV}/qa/r2/DEFECTS.md; you fix every defect on this branch with tests (any template path; shared navigation files only through their order's current owner, see FILE ORDER), and the recapture is the next check. The old emulator's fold and posture results stand (the user, Oct 19).` }),
  S({ id: 'QA2-ADDON', lanes: ['QA2'], branches: ['staging-wp/QA2-ADDON'], reqs: 'as QA2', phone: true, docs: false,
    start: ['QA2', 'WP04-P11'], after: ['QA2', 'WP04-P10T', 'WP04-P11'],
    plan: `${RD}/QA2/PHONE.md, ${OV}/qa/r2/DEFECTS.md`, scope: `The QA2 checks for whatever merged after QA2's capture began (P10T, P11 if it ran, late fixes).` }),
  S({ id: 'WP12-KNOW', lanes: ['WP12'], branches: ['staging-wp/WP12-KNOW'], reqs: '13, 15, 16, 25, 50, 36', kind: 'docs', gate: 'dex', docs: false, fast: true,
    start: ['WP12-DOCS'], finalAfter: ['QA2-ADDON'], after: ['QA2-ADDON'],
    plan: `${P} section 4.1, ${OV}/handoff/WP12.md, ${OV}/design/wp/WP12.md, ${OV}/docs-draft/, ${OV}/RELEASE-NOTES-android-draft.md, every handoff's "Rules and gotchas the docs must state"`,
    scope: `The knowledge pass of release-pipeline.md 4.1: draft now on the branch; the final pass after QA2-ADDON is in staging checks every claim against the final head. While WP10-WEB is not merged, do not edit the files it owns (the FAQ, components/included.tsx, the telemetry page and its links); your final pass reviews and corrects them.` }),
  S({ id: 'WP02-FMT', lanes: ['WP02'], branches: ['staging-wp/WP02-FMT'], reqs: '7, 27', docs: false, startMerged: ['QA2-ADDON'], after: ['QA2-ADDON'],
    plan: `${SD}/CARRY-OVER.md (WP02, the formatting check)`,
    scope: `The one mechanical reformat after the last UI change: ./gradlew spotlessApply on the template, delete the formatting-check region (build.gradle.kts, its [regions] line in template.toml, the emptying in targets/android/settings.py), check on all and everything. The reviewer confirms git diff -w shows only the region's removal.` }),
]
const STAGE = {}
STAGES.forEach(s => { STAGE[s.id] = s })

let intake = null
let audit = null
const results = {}
const readyHeads = {}

function gapsFor(id) {
  const g = intake ? intake.gaps.filter(x => x.stage === id).flatMap(x => x.items) : []
  const a = audit ? audit.findings.filter(f => f.stage === id).map(f => `${f.file}:${f.line || 0} ${f.problem}`) : []
  const all = g.concat(a)
  return all.length ? JSON.stringify(all) : `none routed (read ${RD}/STATE.md and ${SD}/REQUIREMENTS-GAPS.md for rows naming your branch or package)`
}
function baseText(s) {
  const prev = s.start.filter(k => readyHeads[k] && !isDone(`${k}:merged`)).map(k => `${k} at ${JSON.stringify(readyHeads[k])}`)
  return prev.length ? `staging merged with the ready commits of ${prev.join('; ')}` : 'staging'
}
function setupText(s, half) {
  const idx = s.lanes.map((lane, i) => i).filter(i => !half || s.lanes[i] === half)
  return idx.map(i => [s.lanes[i], i]).map(([lane, i]) => `Lane ${lane}: worktree ${wtOf(lane)}, branch ${s.branches[i]}. If the worktree does not exist, create it with git -C ${R} worktree add ${wtOf(lane)} -b ${s.branches[i]} <base>; if it exists for an earlier stage of this lane and is clean, run git -C ${wtOf(lane)} switch -c ${s.branches[i]} <base> there; if the branch already exists, continue on it. Base: ${baseText(s)}.`).join(' ') +
    ` Merge staging into the branch at the start of every run (git merge staging); the owner wins inside owned paths; emitted files (catalogue.json, DesignTokens.*) are regenerated with --write, never hand-merged. Leave the worktree clean (everything committed) when you return.` +
    (s.twoHead && !half ? ` This stage has two heads: after any change, update the local branch check/${s.id} in ${wtOf(s.lanes[0])} to a merge of both heads (never pushed, never merged into staging); the phone checks build from it.` : '')
}
// The stages that wait, directly or not, for a stage (so it must never wait for them).
function dependentsOf(id) {
  const out = new Set()
  const todo = [id]
  while (todo.length) {
    const cur = todo.pop()
    Object.values(STAGE).forEach(x => {
      const deps = (x.start || []).concat(x.after || [], x.startMerged || [], x.finalAfter || [])
      if (deps.includes(cur) && !out.has(x.id)) { out.add(x.id); todo.push(x.id) }
    })
  }
  return out
}
function fileOrderText(s) {
  const pos = NAV_ORDER.indexOf(s.id)
  const deps = dependentsOf(s.id)
  const open = NAV_ORDER.filter(k => !isDone(`${k}:merged`) && !deps.has(k))
  const outside = !open.length ? 'You are not in that order, and every stage of it that does not wait for you is merged: the files are open to you (the stages of the order that wait for you merge staging over your change).'
    : `You are not in that order: while ${open.join(', ')} are not merged, do not change those files; if your work needs a change there, do everything else first, then return waiting with waiting_for ["${open[open.length - 1]}:merged"] and make the change after merging staging.`
  const order = `FILE ORDER: the shared navigation files (${NAV}) change only in this order: ${NAV_ORDER.join(', ')}; each starts from its predecessor's ready commit and merges after it. ${pos >= 0 ? `You are number ${pos + 1} in that order.` : outside} gates.toml: add your rows in your own fenced block; change another stage's row only in that row owner's order.`
  const holds = (s.holds || []).map(h => `HELD FILES: ${h.files} belong to ${h.until} until it is merged (${isDone(`${h.until}:merged`) ? 'it IS merged now: merge staging first, then they are yours' : 'it is NOT merged yet'}). Commit no change to them before; an experiment that needs them goes on a local branch exp/${s.id}-<n> (never pushed, never merged) for a phone session, never into your branch. When only work in them remains, return waiting with waiting_for ["${h.until}:merged"].`)
  return [order].concat(holds).join(' ')
}
function sessionText(s) {
  return `PHONE SESSIONS: you never use the phone yourself. When you need a measurement, a trace or an experiment on the phone before your check (diagnosis, a baseline-profile recording, a quick A/B), write ${RD}/${s.id}/SESSION-<k>.md (k = ${(sessionNo[s.id] || 0) + 1} next) with a Build section (each APK: preset or spec, variant, the exact commit, the ${L} gradle command) and the session script ${RD}/${s.id}/session-<k>.sh that runs the whole phone part (install, scenes, runs, traces, pulls into ${RD}/${s.id}/session-<k>/raw/, settings restored, packages uninstalled) and that a Sonnet operator runs as one command ${L} adb bash ${RD}/${s.id}/session-<k>.sh; then return waiting with waiting_for ["PHONE"]. An Opus analyst re-derives the numbers into ${RD}/${s.id}/session-<k>/RESULT.md and you continue from it.`
}

// ---------- prompts ----------
function implPrompt(s, run, extra) {
  const half = extra && extra.half ? ` You build the ${extra.half} half only: ${s.halves[extra.half]}; the other half runs beside you in its own lane.` : ''
  return `You are the Opus implementation engineer of stage ${s.id} of the android-base release from staging (${R}). Run: ${run}. ${RULES}
Read before editing: ${OV}/REQUIREMENTS.md (all of it; for this stage above all items ${s.reqs}); ${P} sections 1, 2.2 and 3 (your stage, the file order, what ready means, how merges and gates work); ${s.plan}; ${RD}/STATE.md (routing and decisions); your handoff ${OV}/handoff/${s.id}.md if it exists (keep a "## Progress" section at its top current, so a cut-off run loses nothing, and "Measurements" and "Rules and gotchas the docs must state" for WP12).
Earlier runs may have stopped mid-way: reconstruct where from the handoff, git log and git status; uncommitted changes are work in progress to review, keep what is right, and commit. Never touch ${WTS} or another lane's worktree.
SCOPE: ${s.scope}${half}
GAPS ROUTED HERE (close each with proof, REQUIREMENTS 46): ${gapsFor(s.id)}
SETUP: ${setupText(s, extra && extra.half)}
${fileOrderText(s)}
${sessionText(s)}
Tests first: write each test the plan names, watch it fail on staging's code (or label it a guard), then make it pass and commit both; no red commits. ${s.phone ? `Write the phone procedure ${RD}/${s.id}/PHONE.md for a Sonnet operator who follows it exactly and judges nothing: a Build section (each arm's APK: preset or spec, variant, the exact commit, the ${L} gradle command; arm B is your branch head${s.twoHead ? ' or the local check branch that merges both heads' : ''}), the Run section as the session script ${RD}/${s.id}/run-check.sh taking the check number (install, scenes, runs, the perf.sh or script commands, pulls into ${OV}/perf/${s.id}/check-<n>/, settings restored, packages uninstalled), ${s.trial ? 'a Confirm section and script run-confirm.sh taking the check number (the win rows only, on the settled branch), ' : ''}and the thresholds fixed now, before anything is measured (from the plan).` : ''} ${s.docs && s.phone ? 'Leave the docs paragraph with its {braces} for the docs step after the phone check; KDocs and docs/ARCHITECTURE.md lines go in the code commits.' : 'Your docs (KDoc, docs files, handoff facts for WP12) go in your own commits.'}
${FAST ? `FAST TRACK (REQUIREMENTS 56): staging is still being fixed. Do not change what ${SD}/TEST-REPORT.md "Still open" names (the shell and bar drawing behind the push, the gesture and scroll jank, the DEV badge layer); stop at ready; never merge into staging.` : ''}
Return state "done" when the scope is committed with its tests and the plan's acceptance passes; "waiting" with waiting_for (only "PHONE", "CP2", "ASSETS", "<stage>:ready" or "<stage>:merged" keys) when what remains needs that first; "skipped" only where the plan says so, with the reason; "blocked" only with evidence. heads: every branch you worked on with its head sha. touched_measured_path: whether this run changed code a phone check measures.${extra && extra.text ? '\n' + extra.text : ''}`
}
function reviewPrompt(s, round) {
  const target = s.branches.map(b => `git -C ${R} diff staging...${b}`).join(' and ')
  return `You are a fresh-context Google/AOSP-level reviewer of stage ${s.id} (REQUIREMENTS ${s.reqs}), review round ${round}. Review ${target} (start with --stat; read every changed source, test and docs file; skim emitted files). Do not modify anything. ${RULES}
Judge against: ${OV}/research/CODE-STANDARDS.md, the androidx Compose API guidelines, Google's jetpack-compose-performance skill (${SKILLS}; a violation in changed Compose code is BLOCKING), the android-intent-security and android-permissions-security skills where the change touches components, intents, links or permissions, the template AGENTS.md, ${s.plan}, ${OV}/REQUIREMENTS.md, the stage's routed gaps (${gapsFor(s.id)}), the file order of ${P} section 2.2 (a change to a shared navigation file out of order, or to a held file before its owner merged, is BLOCKING), and for docs WP12's accuracy lens (every command, path, task and number exists on the branch and is current; braces filled from ${RD}/${s.id}/CHECK-*.md).
BLOCKING: correctness bugs; perf hazards (state read in composition that changes per frame, alpha or offscreen layers on full screens without a written exception, allocation or work per frame, per-event coroutines, infinite animations without visibility or reduce-motion handling, recomposition per frame); public API guideline violations; slop or narrating comments, dead code, needless abstraction; missing, weakened or skipped tests; a rule exception without its text in the rule, the code and the docs; docs missing or untrue; secrets or private names (periflow, vitfleet, jorwen); a change to what the stage does not own without a reason; claims in the handoff the code or the measurements do not support.${round > 1 ? ' This is a re-review after fixes: check the earlier blocking findings are fixed and report only what remains or is new.' : ''} NON-BLOCKING: nits. Cite file and line. There is no pressure to find something.`
}
function opPrompt(what) {
  return `You are a careful operator. Follow the steps exactly and report facts only: what you ran, the exit code, the log path and its last lines, the files you produced. Do not judge quality, do not edit code, do not commit, do not merge, do not fix anything; if a step fails, record it and go on to the next. ${RULES}\n${what}`
}

// ---------- waits: phone sessions, CP2, the sample-media release, other stages ----------
const sessionNo = {}
async function phoneSession(s) {
  const k = sessionNo[s.id] = (sessionNo[s.id] || 0) + 1
  const dir = `${RD}/${s.id}/session-${k}`
  const apk = await withTrees(s.lanes, () => agent(opPrompt(`Build exactly the APKs the Build section of ${RD}/${s.id}/SESSION-${k}.md lists, through ${L} gradle, each from the exact commit it names (git -C <worktree> rev-parse and git status --porcelain first: record both; if the tree is not clean or not at that commit, stop and report it), in ${s.lanes.map(wtOf).join(' or ')} or, for an experiment branch, in that same worktree after git switch --detach <commit> and back to the stage branch afterwards; copy each APK to ${dir}/apks/ with a sha256 file and the commit. Use no adb.`),
    { label: `${s.id}:session#${k}:apk`, phase: 'Build', schema: RAW, model: 'sonnet' }))
  const run = await withPhone(s.prio, () => agent(opPrompt(`Phone session ${k} of stage ${s.id} on the Redmi 9a7c74f4: run exactly one command, ${L} adb bash ${RD}/${s.id}/session-${k}.sh, with the APKs in ${dir}/apks/ (built by the previous step: ${JSON.stringify(apk ? apk.artifacts : [])}), so the adb lock spans the whole session. Save every raw output under ${dir}/raw/. Report the script's exit code and the files it produced.`),
    { label: `${s.id}:session#${k}:run`, phase: 'Build', schema: RAW, model: 'sonnet' }))
  const an = await agent(`You are the Opus analyst of stage ${s.id}'s phone session ${k}. A Sonnet operator built and ran it; trust nothing until you check it. Build report: ${JSON.stringify(apk)}. Run report: ${JSON.stringify(run)}. Read ${RD}/${s.id}/SESSION-${k}.md (what the session was for). Check every APK's sha256 and commit; re-derive every number from ${dir}/raw/ (framestats, gfxinfo, metrics.json, traces with trace_processor as the android-profiler skill runs it, recordings frame by frame with ffmpeg); write ${dir}/RESULT.md: what was measured, the numbers with their spread, what they show and what they do not. Do not modify code. ${RULES}`,
    { label: `${s.id}:session#${k}:analyse`, phase: 'Build', schema: CHECK })
  return { ok: !!an, k, report: an ? an.report_path : `${dir}/RESULT.md (analysis failed)` }
}

// The orchestrator publishes the sample-media release (the user approved it Oct 23 22:09); agents only check it.
let assetsP = null
function assetsWait() {
  if (assetsP) return assetsP
  assetsP = (async () => {
    for (let k = 1; k <= 36; k++) {
      const a = await agent(`You check, read-only, whether release sample-media-v1 of tripathisarthak457/android-base is published and serves what the catalog's video scene needs (decisions/LOG.md Oct 23 22:09: the user chose to self-host the sample clips; the orchestrator creates the release, never an agent). Read the top of ${OV}/handoff/MEDIA.md (the "Orchestrator requests" line, the prepared files with their sizes and sha256, the asset URLs the orchestrator wrote back). Check with gh release view sample-media-v1 --repo tripathisarthak457/android-base and curl only: for every clip and poster asset, curl -sSIL ends in 200 after its redirect; curl -sSL -r 0-99 -o /dev/null -w "%{http_code}" answers 206; size and sha256 match the prepared file; CREDITS.md is an asset. Never create, edit, upload or delete a release or anything else; never push. If the release is not there yet, wait up to 40 minutes, checking every 5 minutes (a background poll you wait on), then answer. Return decided true only if every asset answered 200 and 206 and matched; the summary lists the URLs.`,
        { label: `assets:check#${k}`, phase: 'Build', schema: DECISION })
      if (a && a.decided) { log(`sample-media-v1 checked: ${a.summary.slice(0, 200)}`); fire('ASSETS:ready'); fire('ASSETS:merged'); return { ok: true, summary: a.summary } }
      if (k === 1) log(`orchestrator: MEDIA waits for release sample-media-v1 (top of ${OV}/handoff/MEDIA.md); publish it after checking the files`)
    }
    return { ok: false, summary: 'release sample-media-v1 was not published and checked after 36 polls' }
  })().catch(e => ({ ok: false, summary: `assets check error: ${e}` })).then(r => { endedOf('ASSETS').r(true); return r })
  return assetsP
}

async function handleWait(s, r) {
  const keys = r.waiting_for || []
  const notes = []
  const sig = keys.filter(k => /^[A-Za-z0-9-]+:(merged|ready)$/.test(k) && !k.startsWith(`${s.id}:`))
  const bad = keys.filter(k => !['PHONE', 'CP2', 'ASSETS'].includes(k) && !sig.includes(k))
  if (bad.length) return { ok: false, text: `unknown or self wait keys ${JSON.stringify(bad)}` }
  if (keys.includes('CP2')) {
    if (!s.cp2) return { ok: false, text: 'CP2 asked by a stage that does not own it' }
    const rec = await checkLoop(s, 100)
    if (!rec.v.pass) return { ok: false, text: `CP2 recordings failed: ${JSON.stringify(rec.v.failures)}` }
    await cp2Decision(s)
    notes.push(`${OV}/decisions/CP2.md is written`)
  }
  if (keys.includes('ASSETS')) {
    const a = await assetsWait()
    if (!a.ok) return { ok: false, text: a.summary }
    notes.push(`release sample-media-v1 is published and checked: ${a.summary}`)
  }
  if (keys.includes('PHONE')) {
    const ps = await phoneSession(s)
    if (!ps.ok) return { ok: false, text: `phone session ${ps.k} could not be analysed` }
    notes.push(`phone session ${ps.k} ran; its analysis is ${ps.report}`)
  }
  if (sig.length) {
    const w = await waitFor(sig)
    if (w !== 'ok') return { ok: false, text: `${w} never arrived` }
    notes.push(`${sig.join(', ')} now hold`)
  }
  return { ok: true, text: `${notes.join('; ')}. Continue from your handoff.` }
}

// ---------- implementation runs (every call takes its lane trees; waits never hold a slot or a tree) ----------
async function runImpl(s, run, extra) {
  const long = run === 'impl' || run.startsWith('impl:')
  const base = `${FAST ? 'ft:' : ''}${s.id}:${run}`
  const trees = extra && extra.half ? [extra.half] : s.lanes
  const phase = (extra && extra.phase) || 'Build'
  // The heads are recorded inside the tree lock, so an APK build that takes the lock next sees the heads the tree is at.
  const call = (label, more) => {
    const ex = Object.assign({}, extra || {}, more ? { text: ((extra && extra.text) ? extra.text + '\n' : '') + more } : {})
    const go = () => withTrees(trees, async () => { const x = await agent(implPrompt(s, run, ex), { label, phase, schema: STATUS }); note(s, x); return x })
    return long ? withImplSlot(go) : go()
  }
  let r = await call(base)
  if (!r) throw new Error(`${s.id} agent failed (${run})`)
  let touched = !!r.touched_measured_path
  for (let n = 1; r.state === 'waiting' && n <= s.maxWaits; n++) {
    const w = await handleWait(s, r)
    if (!w.ok) return { state: 'blocked', summary: w.text, heads: r.heads, touched_measured_path: touched, waiting_for: [] }
    r = await call(`${base}:cont#${n}`, `CONTINUE (${r.waiting_for.join(', ')}): ${w.text}`)
    if (!r) throw new Error(`${s.id} agent failed (${run} continue ${n})`)
    touched = touched || !!r.touched_measured_path
  }
  if (r.state === 'waiting') return { state: 'blocked', summary: `still waiting after ${s.maxWaits} waits: ${JSON.stringify(r.waiting_for)}`, heads: r.heads, touched_measured_path: touched, waiting_for: [] }
  return Object.assign({}, r, { touched_measured_path: touched })
}
function note(s, r) {
  if (r && r.heads && r.heads.length) readyHeads[s.id] = r.heads
  if (r && r.touched_measured_path) results[s.id] = Object.assign({}, results[s.id] || {}, { touched: true })
}

async function implement(s) {
  if (!s.twoHead) return runImpl(s, 'impl')
  const halves = await parallel(Object.keys(s.halves).map(h => () => runImpl(s, `impl:${h}`, { half: h })))
  const bad = halves.filter(x => !x || x.state !== 'done')
  if (bad.length) return bad[0] || { state: 'blocked', summary: 'a half failed', heads: [] }
  readyHeads[s.id] = halves.flatMap(x => x.heads)
  // Both halves done: one agent joins them for the phone check (the local check branch and the stage's PHONE.md).
  const j = await runImpl(s, 'join', { text: `MODE: join. Both halves are committed (${JSON.stringify(readyHeads[s.id])}; ${halves.map(x => x.summary).join(' | ')}). Create or update the local branch check/${s.id} as a merge of both heads (never pushed, never merged into staging), write ${RD}/${s.id}/PHONE.md and its scripts for the stage as a whole (both halves' scenes), run both halves' tests on the check branch through the lock, commit nothing else to the stage branches. Return heads (both stage branches, not the check branch).` })
  if (j.state !== 'done') return j
  return { state: 'done', summary: halves.map(x => x.summary).join(' | '), heads: readyHeads[s.id], touched_measured_path: true, waiting_for: [] }
}

// ---------- the phone check of a stage ----------
async function phoneCheck(s, n, opts) {
  const section = (opts && opts.section) || 'Run'
  const pre = ['HUD-O1'].concat(s.after).filter(k => k !== s.id && !(FAST && k === 'HUD-O1'))
  const w = await waitFor(pre.map(k => `${k}:merged`))
  if (w !== 'ok') return { v: { pass: false, verdict: 'fail', rows: [], failures: [`${w} never arrived`], report_path: '', dead: true }, measured: '' }
  let measured = ''
  const where = s.twoHead ? `in ${wtOf(s.lanes[0])} after git switch --detach check/${s.id} (switch back to ${s.branches[0]} afterwards; the check branch's two parents must be the recorded heads)` : `in the lane worktree ${wtOf(s.lanes[0])}`
  const apk = await withTrees(s.lanes, () => {
    measured = JSON.stringify(readyHeads[s.id] || [])
    return agent(opPrompt(`Build exactly the APKs that the Build section of ${RD}/${s.id}/PHONE.md lists for check ${n}, through ${L} gradle ${where}. The stage's recorded heads are ${measured}: before building, record git rev-parse HEAD and git status --porcelain; if the tree is not clean or not at the recorded head, stop and report it as a problem without building. Copy each APK to ${RD}/${s.id}/apks/check-${n}/ with a sha256 file and the commit it was built from. Use no adb.`),
      { label: `${s.id}:apk#${n}`, phase: 'Build', schema: RAW, model: 'sonnet' })
  })
  const script = section === 'Confirm' ? 'run-confirm.sh' : 'run-check.sh'
  const run = await withPhone(s.prio, () => agent(opPrompt(`Check ${n} of stage ${s.id} on the Redmi 9a7c74f4: run exactly one command, ${L} adb bash ${RD}/${s.id}/${script} ${n} (the ${section} section of ${RD}/${s.id}/PHONE.md), so the adb lock spans the whole session, with the APKs in ${RD}/${s.id}/apks/check-${n}/ (built by the previous step: ${JSON.stringify(apk ? apk.artifacts : [])}). Every raw output goes under ${OV}/perf/${s.id}/check-${n}/. Report the exit code and the files.`),
    { label: `${s.id}:phone#${n}`, phase: 'Build', schema: RAW, model: 'sonnet' }))
  const v = await agent(`You are the Opus verifier of stage ${s.id}'s phone check ${n} (${section} section). A Sonnet operator built and ran it; trust nothing until you check it. Build report: ${JSON.stringify(apk)}. Run report: ${JSON.stringify(run)}. ${RULES} Do not modify code.
Check every APK's sha256 and that it was built from a clean tree at the recorded heads ${measured} (a mismatch fails the check); re-derive every number from the raw files under ${OV}/perf/${s.id}/check-${n}/ (framestats, gfxinfo, metrics.json, traces with trace_processor as the android-profiler skill runs it, recordings frame by frame with ffmpeg, every screenshot looked at); judge only against the thresholds ${RD}/${s.id}/PHONE.md fixed before measuring, with the harness's run-to-run spread (an unclear spread is inconclusive, not a pass)${s.trial ? '; for a trial, give the verdict per kind (won, lost; inconclusive counts as lost) and list kinds_won (for the Confirm section: the kinds whose win rows still hold)' : ''}. Write ${RD}/${s.id}/CHECK-${n}.md (each row: value A, value B, limit, result, evidence path; the numbers the docs braces need). Return pass only if every row passes for real.`,
    { label: `${s.id}:verify#${n}`, phase: 'Build', schema: CHECK })
  return { v, measured }
}

async function checkLoop(s, first) {
  let last = null
  for (let n = first; n < first + s.maxChecks; n++) {
    const c = await phoneCheck(s, n)
    if (!c.v) throw new Error(`${s.id} check ${n} verifier failed`)
    last = { v: c.v, n, measured: c.measured }
    if (c.v.dead || c.v.pass || (s.trial && c.v.verdict !== 'fail')) return last
    const fx = await runImpl(s, `fixcheck#${n}`, { text: `MODE: fix after the phone check. The verifier's report ${c.v.report_path} says: ${JSON.stringify(c.v.failures)}. Fix each at its root (never a looser limit, never a weaker test), re-run the affected tests, update PHONE.md only if a scene was wrong and say why in the handoff, commit, return "done".` })
    if (fx.state !== 'done') return { v: Object.assign({}, c.v, { failures: c.v.failures.concat([fx.summary]) }), n, measured: c.measured }
  }
  return last
}

async function reviewRound(s, round) {
  const rv = await agent(reviewPrompt(s, round), { label: `${FAST ? 'ft:' : ''}${s.id}:review#${round}`, phase: 'Build', schema: REVIEW })
  if (!rv) throw new Error(`${s.id} review ${round} failed`)
  if (!rv.blocking.length) return { clean: true }
  log(`${s.id} review #${round}: ${rv.blocking.length} blocking`)
  const fx = await runImpl(s, `fix#${round}`, { text: `MODE: fix. A fresh reviewer found these BLOCKING problems; fix each at its root (or, if one is wrong, show why in the handoff with evidence), re-run the affected tests and acceptance, commit, return "done" and set touched_measured_path: ${JSON.stringify(rv.blocking)}. Non-blocking (address cheaply or record why not): ${JSON.stringify(rv.nonblocking)}` })
  return { clean: false, fixed: fx.state === 'done', touched: !!fx.touched_measured_path, summary: fx.summary }
}
function headsMoved(s, check) { return !!check && JSON.stringify(readyHeads[s.id] || []) !== check.measured }

// A trial's settle: keep the kinds that won, remove the rest, then a confirmation run of the settled code decides
// what stays (sheet-picture-trial.md and keyboard-slides.md: kept only if the phone numbers win, on the shipped code).
// Confirmation runs are checks 201 and up, merge-fix checks 300 and up, the CP2 recordings check 100.
const confirmNo = {}
async function settleTrial(s, check) {
  const st = await runImpl(s, 'settle', { text: `MODE: settle the trial. The verified verdict (${check.v.report_path}): ${JSON.stringify({ verdict: check.v.verdict, kinds_won: check.v.kinds_won || [], rows: check.v.rows })}. Apply the plan's keep commits for every kind that won (the exception written in the rule's text, the code comments and the docs, each saying which measured win it rests on) and its remove commits for every kind that lost or was inconclusive, newest first. Make sure PHONE.md's Confirm section and run-confirm.sh measure the win rows of the kept kinds on the settled branch. Return kinds_won and state "done"; return state "lost" with no heads only if nothing won and the branch no longer differs from staging.` })
  return confirmTrial(s, st, '')
}
async function confirmTrial(s, settled, tag) {
  let st = settled
  for (let c = 1; c <= 2; c++) {
    const kept = st.kinds_won || []
    results[s.id] = Object.assign({}, results[s.id] || {}, { kinds_won: kept })
    if (st.state !== 'done' || !kept.length) return st
    confirmNo[s.id] = (confirmNo[s.id] || 0) + 1
    const cf = await phoneCheck(s, 200 + confirmNo[s.id], { section: 'Confirm' })
    if (!cf.v) throw new Error(`${s.id} confirmation verifier failed`)
    if (cf.v.dead) return { state: 'blocked', summary: cf.v.failures.join('; '), heads: readyHeads[s.id] || [] }
    const holding = cf.v.kinds_won || []
    const lostNow = kept.filter(k => !holding.includes(k))
    if (cf.v.pass && !lostNow.length) return st
    st = await runImpl(s, `resettle${tag}#${c}`, { text: `MODE: re-settle. The confirmation run of the settled configuration (${cf.v.report_path}) did not hold for ${JSON.stringify(lostNow)}: apply those kinds' remove commits (their exception text, code and docs go with them), keep the others. Return kinds_won (the kinds whose win rows held) and state "done", or "lost" with no heads if nothing is kept and the branch no longer differs from staging.` })
  }
  const kept = st.kinds_won || []
  results[s.id] = Object.assign({}, results[s.id] || {}, { kinds_won: kept })
  if (st.state === 'done' && kept.length) return { state: 'blocked', summary: 'the settled trial did not hold in two confirmation runs', heads: readyHeads[s.id] || [] }
  return st
}

// impl -> (phone check || review round 1, whose fix waits for the check's APK build) -> trial settle and confirm ->
// docs -> final pass -> sync with staging -> the combined review of the merge-ready branch, rounds until clean.
async function buildStage(s) {
  const blocked = (summary) => ({ state: 'blocked', summary, heads: readyHeads[s.id] || [] })
  const r = await implement(s)
  if (r.state !== 'done') return r
  // The check's APK build and round 1's fix share the lane trees' lock: whichever comes first, the APK is built from a
  // clean tree at the recorded heads, and a fix that lands after it moves the heads and triggers a re-check.
  const usesPhone = s.phone && !FAST
  const [c1, rv1] = await parallel([
    () => usesPhone ? checkLoop(s, 1) : Promise.resolve(null),
    () => reviewRound(s, 1),
  ])
  if (usesPhone && !c1) throw new Error(`${s.id} phone check failed to run`)
  if (!rv1) throw new Error(`${s.id} review 1 failed to run`)
  let check = c1
  if (check && check.v.dead) return blocked(check.v.failures.join('; '))
  if (!rv1.clean && !rv1.fixed) return blocked(rv1.summary)
  if (check && check.v.pass && !rv1.clean && rv1.touched && !s.trial && headsMoved(s, check)) check = await checkLoop(s, check.n + 1)
  if (check && !check.v.pass && !s.trial) return blocked(`phone check failed: ${JSON.stringify(check.v.failures)}`)
  if (check) results[s.id] = Object.assign({}, results[s.id] || {}, { check: check.v })
  if (s.trial && check) {
    const st = await settleTrial(s, check)
    if (st.state === 'lost' && (!st.heads || !st.heads.length)) return { state: 'lost', summary: st.summary, heads: [] }
    if (st.state !== 'done' && st.state !== 'lost') return st
  }
  if (s.docs && s.phone && !FAST) {
    if (s.id !== 'WP12-DOCS') { const w = await waitFor(['WP12-DOCS:merged']); if (w !== 'ok') return blocked(w) }
    const d = await runImpl(s, 'docs', { text: `MODE: docs. Write this stage's docs commit (REQUIREMENTS 50: the rule, why it keeps the app smooth, how to apply it in a screen, the anti-pattern, the number it protects) into the docs base files as staging has them (templates/android/docs/PERFORMANCE.md, AGENTS.md "Smooth and light", and the files the plan names), every brace from ${check ? check.v.report_path : RD + '/' + s.id}, saying only what this merge makes true; mark ${OV}/docs-draft/PENDING.md; commit.` })
    if (d.state !== 'done') return d
  }
  if (s.finalAfter) {
    if (FAST) return { state: 'draft', summary: 'drafted on the fast track; the final pass waits for the release run', heads: readyHeads[s.id] || [] }
    const w = await waitFor(s.finalAfter.map(k => `${k}:merged`))
    if (w !== 'ok') return blocked(w)
    const f = await runImpl(s, 'finalize', { text: `MODE: final pass. ${s.finalAfter.join(', ')} is in staging; merge staging in and bring every docs claim to the final head (every number from ${OV}/report/ and the stages' CHECK files, every command and path checked). Commit.` })
    if (f.state !== 'done') return f
  }
  const sync = await runImpl(s, 'sync', { text: `MODE: sync. Merge today's staging into ${s.branches.join(' and ')} (git merge staging), regenerate emitted files with --write, compile and test what the merge touched through the lock, commit the merge. If a conflict needed a substantive resolution, describe it in the summary (the review that follows reads it) and set touched_measured_path if it touched measured code. Return heads.` })
  if (sync.state !== 'done') return sync
  const recheck = async (touched, tag) => {
    if (!touched || !check || !headsMoved(s, check)) return null
    if (s.trial) {
      const kept = (results[s.id] && results[s.id].kinds_won) || []
      if (!kept.length) return null
      const st = await confirmTrial(s, { state: 'done', summary: 're-confirm after a change on the measured path', heads: readyHeads[s.id] || [], kinds_won: kept }, tag)
      return (st.state === 'done' || st.state === 'lost') ? null : (st.summary || 'trial confirmation failed')
    }
    check = await checkLoop(s, check.n + 1)
    return check.v.pass ? null : `phone check failed after ${tag}: ${JSON.stringify(check.v.failures)}`
  }
  const afterSync = await recheck(!!sync.touched_measured_path, ':sync')
  if (afterSync) return blocked(afterSync)
  // The one combined Google/AOSP review of what this stage merges, on the synced branch: rounds until a round finds
  // nothing blocking, then ready.
  for (let round = 2; round <= 5; round++) {
    const rv = await reviewRound(s, round)
    if (rv.clean) return { state: 'ready', summary: r.summary, heads: readyHeads[s.id] || [] }
    if (!rv.fixed) return blocked(rv.summary)
    const bad = await recheck(rv.touched, `:r${round}`)
    if (bad) return blocked(bad)
  }
  const last = await agent(reviewPrompt(s, 99), { label: `${FAST ? 'ft:' : ''}${s.id}:review#final`, phase: 'Build', schema: REVIEW })
  if (!last || last.blocking.length) return blocked(`open blocking findings: ${last ? JSON.stringify(last.blocking) : 'final review failed'}`)
  return { state: 'ready', summary: r.summary, heads: readyHeads[s.id] || [] }
}

// ---------- merge trains (release-pipeline.md section 3) ----------
const mq = []
let mergerBusy = false
let trainNo = 0
let gate0P = Promise.resolve(null)
let mergeHalted = null
const attempts = {}
const trainAttempts = {}
function haltMerges(why) {
  if (!mergeHalted) { mergeHalted = why; log(`MERGES HALTED (staging carries a merge no green gate covers): ${why}`) }
  mq.splice(0).forEach(it => it.res({ ok: false, halted: true, needs_stage_fix: false, details: `merges halted: ${mergeHalted}` }))
}
function requestMerge(s, heads) {
  if (mergeHalted) return Promise.resolve({ ok: false, halted: true, needs_stage_fix: false, details: `merges halted: ${mergeHalted}` })
  return new Promise(res => { mq.push({ s, heads, res }); kickMerger() })
}
function mergeEligible(item) {
  return item.s.start.concat(item.s.after).filter(k => k !== item.s.id).every(k => isDone(`${k}:merged`)) && isDone('INTAKE:merged')
}
function kickMerger() {
  if (FAST || mergerBusy) return
  if (mergeHalted) { haltMerges(mergeHalted); return }
  const ready = mq.filter(mergeEligible).sort((a, b) => a.s.prio - b.s.prio)
  if (!ready.length) return
  const train = ready.slice(0, TRAIN_MAX)
  train.forEach(t => mq.splice(mq.indexOf(t), 1))
  mergerBusy = true
  trainNo++
  const n = trainNo
  runTrain(n, train)
    .catch(e => { haltMerges(`train ${n} error: ${e}`); train.forEach(t => t.res({ ok: false, halted: true, needs_stage_fix: false, details: String(e) })) })
    .then(() => { mergerBusy = false; kickMerger() })
}
function integratorPrompt(item, attempt, n) {
  const s = item.s
  const tag = `${TAG}${s.id}-${attempt}`
  const two = s.twoHead ? ` This stage has two heads (${JSON.stringify(item.heads)}): bring both in ONE --no-ff merge commit with both heads as parents (git merge --no-ff -m "<message>" <head1> <head2>); if git refuses the octopus, cut merge/${s.id}-${attempt} from staging, merge each head --no-ff there, run the same checks, then merge that branch into staging with one --no-ff (leave it in place; never push it).` : ''
  return `You are the release's integrator. Merge stage ${s.id} (REQUIREMENTS ${s.reqs}), merge attempt ${attempt}, into staging in ${WTS}. ${RULES}
0. Verify ${WTS} is on staging and clean; if an interrupted merge is in progress, finish it if it is this attempt's and sound, else abort it, and record what you found. This attempt's merge is already done only if the tag ${tag} exists, its commit is an ancestor of staging, and no later commit on staging reverts it (git log staging --grep with that commit's sha, and ${RD}/merges.md): then verify and return ok with that commit. A tag of an earlier attempt of this stage never counts: earlier attempts were reverted or refused.
1. Merge the stage's ready commit(s) ${JSON.stringify(item.heads)} with git merge --no-ff and a message that says in one or two sentences what the stage brings and why, ending with the Co-Authored-By line.${two} Do not revert any earlier revert yourself: when an earlier attempt was reverted, the stage branch already carries the revert of that revert (its merge-fix run made it), so this merge brings the changes back once.
2. Conflicts: only emitted files (web/frontend/lib/catalogue.json, DesignTokens.*) may be resolved here, by regenerating them with --write. Any other conflict: git merge --abort and return ok=false, needs_stage_fix=true with the files; the stage resolves it on its branch, where a review reads the resolution.
3. Compile what the merge touched through ${L} gradle (and the generator tests through ${MW} when generator files changed); a failure caused by the merge: git revert -m 1 --no-edit HEAD, record the revert in ${RD}/merges.md, and return ok=false, needs_stage_fix=true with the failing output.
4. Tag ${tag} at the merge commit (local; agents never push) and append an entry to ${RD}/merges.md (train ${n}, stage ${s.id}, attempt ${attempt}, the merge commit, what was compiled). Return the new staging head and merge_commit. Anything else that goes wrong (a dirty staging tree you cannot explain, an unknown in-progress operation): touch nothing more and return ok=false, needs_stage_fix=false with what you found.`
}
let haltRecorded = false
async function recordHalt(why, detail) {
  if (haltRecorded) return
  haltRecorded = true
  await agent(`You record a halted merge queue for the orchestrator. ${why}. ${detail} Read ${RD}/merges.md, ${RD}/gates/ and git -C ${R} log staging; write ${RD}/HALT.md: every merge in staging that no green gate covers (stage, attempt, commit), the evidence of what went wrong, and the two ways forward (revert those merges newest first and re-queue them, or re-run the gate once the cause is fixed). Do not modify any repository.`,
    { label: 'merge:halt-record', phase: 'Merge', schema: DECISION })
}
async function runTrain(n, train) {
  await gate0P
  const merged = []
  for (let i = 0; i < train.length; i++) {
    const item = train[i]
    const id = item.s.id
    attempts[id] = (attempts[id] || 0) + 1
    const m = await agent(integratorPrompt(item, attempts[id], n), { label: `merge:${id}#${attempts[id]}`, phase: 'Merge', schema: MERGE })
    if (m && m.ok) { merged.push({ item, m }); continue }
    if (m && m.needs_stage_fix) { item.res({ ok: false, needs_stage_fix: true, details: m.details }); continue }
    const why = `the integrator could not merge ${id} cleanly: ${m ? m.details : 'the integrator agent failed'}`
    haltMerges(why)
    await recordHalt(why, `Merged in this train before it: ${JSON.stringify(merged.map(x => x.item.s.id))}.`)
    train.slice(i).concat(merged.map(x => x.item)).forEach(t => t.res({ ok: false, halted: true, needs_stage_fix: false, details: why }))
    return
  }
  if (!merged.length) return
  const ids = merged.map(x => x.item.s.id)
  const key = ids.slice().sort().join('+')
  trainAttempts[key] = (trainAttempts[key] || 0) + 1
  const tag = `train:${key}#${trainAttempts[key]}`
  log(`train ${n} = ${tag}`)
  const halt = async (why) => {
    haltMerges(why)
    await recordHalt(why, `Train ${n} (${tag}) merged ${JSON.stringify(ids)}.`)
    merged.forEach(x => x.item.res({ ok: false, halted: true, needs_stage_fix: false, details: why }))
  }
  let gv = await gateTrain(tag, ids)
  if (!gv) gv = await gateTrain(`${tag}:retry`, ids)
  if (!gv) return halt(`${tag}: the gate could not be verified twice; merges ${JSON.stringify(ids)} are in staging ungated`)
  if (gv.green) {
    merged.forEach(x => { fire(`${x.item.s.id}:merged`); x.item.res({ ok: true }) })
    return
  }
  let culprits = gv.culprits.filter(c => ids.includes(c))
  if (!culprits.length && ids.length > 1) {
    const bdir = `${RD}/gates/${safe(tag)}/bisect`
    const commits = merged.map(x => x.m.merge_commit)
    const bb = await agent(opPrompt(`Bisect build for ${tag}: in the detached scratch worktree ${OV}/wt/sw-bisect (git -C ${R} worktree add --detach ${OV}/wt/sw-bisect <commit> if it is missing, else git -C ${OV}/wt/sw-bisect checkout --detach <commit>; never ${WTS}), for the commit before the first merge (the first parent of ${commits[0]}) and for each merge commit ${JSON.stringify(commits)}, build the measured APK(s) the gate used (${RD}/gates/${safe(tag)}/desk/) through ${L} gradle and copy them to ${bdir}/apks/<commit>/ with sha256. Use no adb.`),
      { label: `${tag}:bisect-apk`, phase: 'Merge', schema: RAW, model: 'sonnet' })
    const b = await withPhone(0, () => agent(opPrompt(`Bisect run for ${tag}: write ${bdir}/run.sh that installs each APK set in ${bdir}/apks/ and runs the scenarios the gate verifier named as red (${gv.details}) A-B-A-B against the commit before the first merge, 2 runs per block, pulling into ${bdir}/raw/, restoring every setting; then run it as one command ${L} adb bash ${bdir}/run.sh. Builds: ${JSON.stringify(bb ? bb.artifacts : [])}.`),
      { label: `${tag}:bisect-run`, phase: 'Merge', schema: RAW, model: 'sonnet' }))
    const bv = await agent(`You are the Opus verifier of ${tag}'s bisect. Re-derive every row from ${bdir}/raw/ and name the merge(s) that caused each red row (stage ids from ${JSON.stringify(ids)}). Build report: ${JSON.stringify(bb)}. Run report: ${JSON.stringify(b)}. Do not modify anything. ${RULES}`,
      { label: `${tag}:bisect-verify`, phase: 'Merge', schema: GATEV })
    culprits = bv ? bv.culprits.filter(c => ids.includes(c)) : []
  }
  if (!culprits.length) culprits = ids
  const rv = await agent(`You are the release's integrator. ${tag}'s gate is red (${gv.report_path}): ${gv.details}. Revert the merges of ${JSON.stringify(culprits)} on staging in ${WTS} with git revert -m 1 --no-edit <merge commit>, newest first, each with a message naming the red row; delete nothing (tags stay; the next attempt of a stage gets its own tag); record each revert with its commit in ${RD}/merges.md. Compile what the reverts touched. Return ok only if every revert is committed and compiles. ${RULES}`,
    { label: `${tag}:revert`, phase: 'Merge', schema: MERGE })
  if (!rv || !rv.ok) return halt(`${tag}: the revert of ${JSON.stringify(culprits)} failed: ${rv ? rv.details : 'agent failed'}`)
  merged.filter(x => culprits.includes(x.item.s.id)).forEach(x => x.item.res({ ok: false, needs_stage_fix: true, details: `${tag} gate red: ${gv.details}; reverted in ${rv.staging_head}` }))
  const rest = merged.filter(x => !culprits.includes(x.item.s.id))
  if (!rest.length) return
  const rg = await gateTrain(`${tag}:regate`, rest.map(x => x.item.s.id))
  if (rg && rg.green) { rest.forEach(x => { fire(`${x.item.s.id}:merged`); x.item.res({ ok: true }) }); return }
  merged.splice(0, merged.length, ...rest)
  return halt(`${tag}: still not green after reverting ${JSON.stringify(culprits)}: ${rg ? rg.details : 'regate not verified'}`)
}
function safe(t) { return t.replace(/[^A-Za-z0-9+_-]/g, '-') }
async function gateTrain(tag, ids) {
  const dir = `${RD}/gates/${safe(tag)}`
  const media = isDone('MEDIA:merged') || ids.includes('MEDIA')
  const glass = isDone('WP05-BAR:merged') || ids.includes('WP05-BAR')
  const apks = `the measured APK of the everything preset (devBenchmarkRelease)${media ? ', the catalog release APK of the everything preset (for the images and video columns, media.md 4.1)' : ''}${glass ? ', the measured APK of the everything-glass spec (generator/tests/fixtures/v2/everything-glass.json, for glass_live and glass_frosted_vs_solid)' : ''}`
  const desk = await agent(opPrompt(`Desktop gate for ${tag} (stages ${JSON.stringify(ids)}) on staging in ${WTS} (read-only for you: build and test only). Logs under ${dir}/desk/. Run: the generator suite (python -I -B -m unittest discover -s tests -t . in generator/ through ${MW}), python -m genkit.tokens --check, python -m genkit.core.catalogue --check, tools/sync_check.py, python -m tests.consistency --quick (memwait); templates/android ./gradlew build --continue through ${L} gradle (timeout 90 min); the presets lean and everything generated with the CLI into ${dir}/gen/ and built with check assembleDevDebug assembleProdRelease --continue through the lock; when web/ changed since the previous gate: npm ci, the site tests, tsc --noEmit, npm run build in web/frontend (memwait); when web/api changed: go vet ./... and go test -count=1 ./... (memwait); then build ${apks}, copy them to ${dir}/desk/apks/ with sha256, and compare each one's dex and resources with the same APK of the previous gate recorded in ${RD}/gates/LAST.md (apkanalyzer dex packages and dexdump checksums, aapt2 resources), saving the diff output.`),
    { label: `${tag}:desk-run`, phase: 'Merge', schema: RAW, model: 'sonnet' })
  const dv = await agent(`You are the Opus verifier of ${tag}'s desktop gate (stages ${JSON.stringify(ids)}). Operator report: ${JSON.stringify(desk)}. Open every log and recount (tests run, failures, skips); a failure caused by one of these merges names it in culprits (git log and the files each merge changed). Decide phone_needed from the measured APKs alone, whatever gate the stages declared: TRUE if any measured APK's dex or resources differ from the previous gate's, or a comparison is missing or unreadable; FALSE only when every measured APK is dex- and resource-identical (the dex proof, release-pipeline.md 3.6: then update ${RD}/gates/LAST.md with these APKs and carry the previous phone table over), or when one verified run measured a tree byte-identical to staging's head now with perf.sh --gate solid plus every column that applies (images and video once MEDIA is in, glass_live and glass_frosted_vs_solid once WP05-BAR is in), 3 runs each (tree reuse: git diff <measured commit> staging empty; then update ${RD}/gates/LAST.md from that run and name it in details). A stage's own phone check is not a gate run unless it ran all of that. Write ${dir}/DESK.md. Do not modify code. ${RULES}`,
    { label: `${tag}:desk-verify`, phase: 'Merge', schema: GATEV })
  if (!dv || !dv.green || !dv.phone_needed) return dv
  const pr = await withPhone(0, () => agent(opPrompt(`Release-build phone gate for ${tag} on staging's head (${WTS}, read-only for you), with the APKs in ${dir}/desk/apks/. Write ${dir}/phone/run.sh that installs them and runs, with perf.hud=off and exactly as ${OV}/handoff/WP04.md runs it: templates/android/tools/perf/perf.sh --gate solid with every app scenario, 3 runs${media ? '; on the catalog release APK (--package <pkg>.catalog --apk <catalog apk>) the --gate images and --gate video invocations of media.md 4.1, 3 runs each' : ''}${glass ? '; on the everything-glass APK the glass_live and glass_frosted_vs_solid columns, 3 runs each' : ''}; keeping summary.json, metrics.json per run and the gate tables under ${dir}/phone/ and restoring every setting. Run it as one command: ${L} adb bash ${dir}/phone/run.sh.`),
    { label: `${tag}:phone-run`, phase: 'Merge', schema: RAW, model: 'sonnet' }))
  return agent(`You are the Opus verifier of ${tag}'s release-build phone gate (stages ${JSON.stringify(ids)}). Operator report: ${JSON.stringify(pr)}. Re-derive every gate table from the per-run metrics.json against templates/android/tools/perf/gates.toml on staging (solid${media ? ', images, video' : ''}${glass ? ', glass_live, glass_frosted_vs_solid' : ''}), and compare every row with the previous green gate in ${RD}/gates/LAST.md: release rows (REQUIREMENTS 42 hitch ratio and jank after the first frame; REQUIREMENTS 41 scroll) must pass; ratchet rows must not get worse beyond the harness's spread; rows that were red before this train (staging's open regression, until REG or staging closes it) may stay red but not get worse. Green only if all of that holds; otherwise name the culprit stage(s) when the evidence shows them (the rows each stage touches), else leave culprits empty for a bisect. Write ${dir}/GATE.md and, when green, update ${RD}/gates/LAST.md (these APKs, the tables and the columns). Do not modify code. ${RULES}`,
    { label: `${tag}:phone-verify`, phase: 'Merge', schema: GATEV })
}

// A stage's merges: up to three attempts; a refused or reverted attempt goes back through a merge fix on the stage
// branch (in the shared fix worktree), its review and, for a phone row, its phone check.
async function mergeLoop(s, heads, summary) {
  for (let k = 1; k <= 3; k++) {
    const m = await requestMerge(s, heads)
    if (m.ok) return { id: s.id, state: 'merged', summary }
    if (m.halted) return { id: s.id, state: 'merge-halted', summary: m.details }
    if (!m.needs_stage_fix) return { id: s.id, state: 'merge-failed', summary: m.details }
    const fs = Object.assign({}, s, { lanes: s.lanes.map(() => 'FIX') })
    const fx = await onLanes(['FIX'], async () => {
      const f = await runImpl(fs, `mergefix#${k}`, { phase: 'Merge', text: `MODE: merge fix in the shared fix worktree ${wtOf('FIX')} (check out ${s.branches.join(' and ')} there; your lane has moved on). The merge into staging failed: ${m.details}. 1) Merge staging into the branch. 2) If staging now carries a revert of an earlier merge of this stage (a gate revert or the integrator's own revert after a failed compile; ${RD}/merges.md lists them), revert that revert ON THIS BRANCH first (git revert --no-edit <revert commit>), so the branch carries the stage's changes again; the integrator never does it. 3) Resolve at the root and fix what the gate or the compile found, with tests. Do not use the phone: if the failure was a phone row, the workflow re-runs your phone check after this run. Commit; return heads and touched_measured_path.` })
      if (!f || f.state !== 'done') return f
      const rv = await reviewRound(fs, 10 + k)
      if (!rv.clean && !rv.fixed) return { state: 'blocked', summary: rv.summary, heads: [] }
      if (!rv.clean) {
        const rv2 = await agent(reviewPrompt(s, 20 + k), { label: `${s.id}:review#${20 + k}`, phase: 'Merge', schema: REVIEW })
        if (!rv2 || rv2.blocking.length) return { state: 'blocked', summary: 'merge fix review not clean', heads: [] }
      }
      if (s.phone && /phone|row|janky|hitch|frame|gate|red/i.test(m.details)) {
        const c = await checkLoop(fs, 300 + k * 10)
        if (!c.v.pass && !s.trial) return { state: 'blocked', summary: 'phone check failed after the merge fix', heads: [] }
      }
      return Object.assign({}, f, { heads: readyHeads[s.id] || f.heads })
    })
    if (!fx || fx.state !== 'done') return { id: s.id, state: 'blocked', summary: fx ? fx.summary : 'merge fix failed' }
    heads = fx.heads
  }
  return { id: s.id, state: 'merge-failed', summary: 'three merge attempts failed' }
}

// ---------- one stage end to end ----------
async function stageFlow(s) {
  const startKeys = (FAST ? [] : ['INTAKE:ready']).concat(s.start.map(k => `${k}:ready`)).concat((s.startMerged || []).map(k => `${k}:merged`))
  const w = await waitFor(startKeys)
  if (w !== 'ok') return { id: s.id, state: 'blocked', summary: `${w} never arrived` }
  const skip = intake ? intake.skip.find(x => x.stage === s.id) : null
  if ((s.cond && !s.cond()) || skip) {
    const why = skip ? skip.reason : 'its condition does not hold'
    log(`${s.id} skipped: ${why}`)
    fire(`${s.id}:ready`); fire(`${s.id}:merged`)
    return { id: s.id, state: 'skipped', summary: why }
  }
  const built = await onLanes(s.lanes, () => buildStage(s))
  results[s.id] = Object.assign({}, results[s.id] || {}, { built })
  if (built.state === 'lost' || built.state === 'skipped') { fire(`${s.id}:ready`); fire(`${s.id}:merged`); return { id: s.id, state: built.state, summary: built.summary } }
  if (built.state !== 'ready') { log(`${s.id} ended ${built.state}: ${String(built.summary).slice(0, 300)}`); return { id: s.id, state: built.state, summary: built.summary } }
  fire(`${s.id}:ready`)
  if (FAST) return { id: s.id, state: 'ready', summary: built.summary, heads: built.heads }
  if (!s.merge) { fire(`${s.id}:merged`); return { id: s.id, state: 'done', summary: built.summary } }
  return mergeLoop(s, built.heads, built.summary)
}

// ---------- CP2 (decisions/LOG.md Oct 19 22:08: the agent picks, the user may veto) ----------
let cp2Done = null
async function cp2Decision(s) {
  if (cp2Done) return cp2Done
  cp2Done = agent(`You decide checkpoint 2 (the glass look) on the user's behalf (they chose on Oct 19 22:08 that an agent picks from the recordings and they may veto). Read ${OV}/design/MASTER-PLAN.md B.5 "CP2", A4.9 and section 2's glass gates; ${OV}/decisions/CP1.md (the format); ${OV}/handoff/WP05.md and WP05-glass.md; ${OV}/REQUIREMENTS.md 21, 35, 37, 42 (full effects at the Live tier, made cheap, never removed); every recording, frame strip and number under ${OV}/perf/WP05/cp2/ and ${RD}/${s.id}/CHECK-*.md (open the frames; judge the look as an Apple designer would, the cost only from release-build numbers). Settle the lens geometry the five tests were about (may the lens pass the bar's edge, how it magnifies), the blur and tint per tier and the tier policy. Write ${OV}/decisions/CP2.md with token entries WP05 can apply, the evidence for each choice, and what would make the user want to override it. Append a line to ${ORQ}: "Send the user decisions/CP2.md and the CP2 recordings (they may veto)". Do not modify any repository.`,
    { label: 'CP2:decide', phase: 'Build', schema: DECISION }).then(d => { if (!d) throw new Error('CP2 decision failed'); fire('CP2:ready'); fire('CP2:merged'); log(`CP2 decided: orchestrator, send the user decisions/CP2.md and the recordings now (they may veto). ${d.summary.slice(0, 200)}`); return d })
  return cp2Done
}

// The orchestrator's own steps the gate needs (REQUIREMENTS 57): the CI-only draft PR and the signed-in /admin check.
async function orchestratorStep(key, request, check, polls) {
  await agent(`Append this line to ${ORQ} (create the file if missing; keep earlier lines), dated from the clock with date: "${key}: ${request}". Do not modify anything else.`, { label: `${key}:request`, phase: 'Verify', schema: DECISION })
  log(`orchestrator request (${key}): ${request}`)
  for (let k = 1; k <= polls; k++) {
    const a = await agent(`You check, read-only, whether the orchestrator finished "${key}" (${ORQ}). ${check} If it is not there yet, wait up to 40 minutes, checking every 5 minutes (a background poll you wait on), then answer. Never push, publish or change anything. Return decided true only when the result exists and you have read it; the summary states the result.`,
      { label: `${key}:check#${k}`, phase: 'Verify', schema: DECISION })
    if (a && a.decided) return { done: true, summary: a.summary }
  }
  return { done: false, summary: `${key} not done after ${polls} polls; the gate lists it as open` }
}

// ======================= FAST TRACK MODE =======================
if (FAST) {
  phase('Intake')
  const ft = await agent(`You prepare the fast track of the release (REQUIREMENTS 56, the user Oct 23 21:50) beside staging's last fixes. ${RULES} Read ${P} sections 1.4, 2.2 and 3, ${SD}/TEST-REPORT.md "Still open", ${OV}/REQUIREMENTS.md 44, 46, 50, 55, 56, 57. For each fast-track stage (${STAGES.filter(x => x.fast).map(x => x.id).join(', ')}), confirm from its scope and the files it will touch that it changes nothing staging is still fixing; append the decision and the reason to ${OV}/decisions/LOG.md (one line per stage, dated from the clock with date). Run ${OV}/bin/locked status and put the free memory in notes. Do not create branches or worktrees (each stage does its own); do not touch ${WTS}. Return gaps: none yet (staging's matrix has not run), skip: any stage that is not safe with the reason.`,
    { label: 'ft:intake', phase: 'Intake', schema: INTAKE })
  if (!ft) throw new Error('fast-track intake failed')
  intake = ft
  log(`fast track: ${ft.notes.slice(0, 200)}`)
  phase('Build')
  // A skipped stage ends at once, so a stage that builds on it sees it dead instead of waiting forever.
  STAGES.filter(x => x.fast && ft.skip.some(k => k.stage === x.id)).forEach(x => endedOf(x.id).r(true))
  const out = await Promise.all(STAGES.filter(x => x.fast && !ft.skip.some(k => k.stage === x.id)).map(x => stageFlow(x).catch(e => ({ id: x.id, state: 'error', summary: String(e) })).then(r => { endedOf(x.id).r(true); return r })))
  return { mode: MODE, stages: out }
}

// ======================= RELEASE MODE =======================
phase('Intake')
intake = await agent(`You open the release run from staging (REQUIREMENTS 44, 46, 55, 56, 57). ${RULES} Do not modify code or branches.
Read: ${SD}/STAGING-REPORT.md, ${SD}/REQUIREMENTS-MATRIX.md, ${SD}/REQUIREMENTS-GAPS.md, ${SD}/RELEASE-PLAN-NOTES.md, ${SD}/CARRY-OVER.md, ${SD}/TEST-REPORT.md ("Still open"), ${SD}/MERGE-NOTES.md section 15, ${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md (forgotten items), ${OV}/followups.md, ${OV}/decisions/LOG.md (every decision already taken; never contradict one), ${P} (every stage: ${STAGES.map(x => x.id).join(', ')}, plus WP13, R52, R46), and the fast-track handoffs (${OV}/handoff/MEDIA.md and the WP12-DOCS, WP12-KNOW, WP02-CONT, WP14, WP10-WEB, WP13-PREP handoffs if present, with their "Requests to the navigation owners").
1. Route EVERY gap row, carry-over item, forgotten item, open follow-up and navigation-owner request to exactly one stage id above (a row no stage fits goes to its owner's CARRY stage; WIN and CI follow-ups to WP13-PREP; a change to a shared navigation file to the first stage of the file order of ${P} section 2.2 that has not merged; nothing dropped); return them in gaps.
2. regression_open: true if TEST-REPORT "Still open" item 1 (back gesture, push half, root-list scroll, navigation stress against T + C1) is still open at staging's head.
3. skip: only REG when the regression is closed, and any stage the report shows already done (with the commit that did it).
4. Append to ${OV}/decisions/LOG.md (dated from the clock with date) the decisions of ${P} section 10 that LOG.md does not already hold, each with its reason. media.md 9.1 and 9.2 are already logged (Oct 23 22:09: 9.2 self-hosted by the user's answer): do not log them again and never log a different answer.
5. Write ${RD}/STATE.md: the routing table, the decisions, the lane and branch map of ${P} section 1.1, the file order of section 2.2, and the staging head you read.`,
  { label: 'intake', phase: 'Intake', schema: INTAKE })
if (!intake) throw new Error('intake failed')
log(`intake: staging ${intake.staging_head}, regression open: ${intake.regression_open}, ${intake.gaps.reduce((a, g) => a + g.items.length, 0)} gap rows routed`)
fire('INTAKE:ready'); fire('INTAKE:merged')
assetsWait()

gate0P = (async () => {
  const apk = await agent(opPrompt(`Baseline gate 0, build part, on staging's head ${intake.staging_head} (${WTS}, read-only for you): generate the everything preset into ${RD}/gates/0/gen/ and build its measured APK (devBenchmarkRelease) through ${L} gradle; copy it to ${RD}/gates/0/apks/ with its sha256. Use no adb.`),
    { label: 'gate0:apk', phase: 'Intake', schema: RAW, model: 'sonnet' })
  const run = await withPhone(0, () => agent(opPrompt(`Baseline gate 0, phone part: write ${RD}/gates/0/run.sh that installs ${RD}/gates/0/apks/ on the Redmi and runs templates/android/tools/perf/perf.sh --gate solid with every scenario, 3 runs, perf.hud=off, exactly as ${OV}/handoff/WP04.md runs it, keeping summary.json, metrics.json per run and the gate table under ${RD}/gates/0/ and restoring every setting; run it as one command ${L} adb bash ${RD}/gates/0/run.sh. Build report: ${JSON.stringify(apk)}.`),
    { label: 'gate0:run', phase: 'Intake', schema: RAW, model: 'sonnet' }))
  return agent(`You are the Opus verifier of baseline gate 0 on staging ${intake.staging_head}. Build report: ${JSON.stringify(apk)}. Run report: ${JSON.stringify(run)}. Check the APK's sha256; re-derive the gate table from the per-run metrics.json against gates.toml; list every red row and compare the regressed rows with ${SD}/TEST-REPORT.md. Write ${RD}/gates/0/GATE.md and ${RD}/gates/LAST.md (this APK, its table, the columns that apply). Do not modify code. ${RULES}`,
    { label: 'gate0:verify', phase: 'Intake', schema: GATEV })
})().catch(e => { log(`gate 0 failed: ${e}; the first train's verifier takes TEST-REPORT's numbers as the reference`); return null })

const auditP = agent(`You run the template-wide compose-performance audit REQUIREMENTS 38 asked for on Oct 16 (research/COMPOSE-PERF-AUDIT.md, which no package wrote). Read-only on staging ${intake.staging_head} (git -C ${R} show staging:<path>, git grep). Apply Google's jetpack-compose-performance skill (${SKILLS}, all seven categories, the matching references before each finding) to the design system, the navigation shell, core/ui, core/image and core/video if present, the features, the catalog and core/perfhud. Write ${OV}/research/COMPOSE-PERF-AUDIT.md: every finding with file:line, the category, the cost on the Redmi where known, and in stage the stage that owns that code and has not merged yet (the stage table of ${P} section 1.2: ${STAGES.map(x => x.id).join(', ')}; a shared navigation file goes to the first stage of the file order of section 2.2 that has not merged), or R52 when its owner has merged or none exists. Do not modify code.`,
  { label: 'audit:compose-perf', phase: 'Intake', schema: FINDINGS })
  .catch(e => { log(`compose-performance audit failed: ${e}; its stages start without it and R52 covers the gap`); return null })
  .then(a => { audit = a || { findings: [] }; fire('AUDIT-CP:ready'); fire('AUDIT-CP:merged'); return audit })

const districtP = (async () => {
  const plan = await agent(`You write the exact procedure of the District re-run REQUIREMENTS 51 approved (the user, Oct 20 08:36: filters and sort sheets, the location and city picker, movie and event detail sheets, any safe sheet; REQUIREMENTS 45; proposals asked one by one and built after the release, REQUIREMENTS 57). Read ${OV}/research/district/PHONE-STUDY.md in full (its Never list, the tap rule, tools/safe_target.py and what it refuses: location, profile, apply, done, continue, showtime and the rest) and RESULTS. Write ${OV}/research/district/PHONE-STUDY-2.md: the exact scenes (each filter and sort sheet opened and closed by Back, a drag or the dim, never Apply or Done; the city picker opened and closed by Back without a selection; movie and event detail sheets), for each the tap target and how safe_target.py checks it, which steps the tool refuses and therefore become the user's by-hand steps (the location or city control is one unless a reviewed allowlist entry says otherwise; propose any allowlist change as a diff in the file, never applied), the close-only rules, the template-side scenes on the newest staging catalog build, and the session script research/district/tools/run-2.sh for the operator. Nothing that orders, pays, books or changes the account or the location. Do not modify code or tools.`,
    { label: 'district:study-plan', phase: 'Build', schema: DECISION })
  if (!plan) return 'district: no procedure'
  let rv = await agent(`You are a separate Opus reviewer of ${OV}/research/district/PHONE-STUDY-2.md against ${OV}/research/district/PHONE-STUDY.md: every scene safe on the user's signed-in account (no selection, no confirm, no location change, no booking path), every tap checked by safe_target.py or handed to the user, any allowlist change justified and narrow, the run script doing only the listed scenes. Do not modify anything. There is no pressure to find something.`,
    { label: 'district:study-review', phase: 'Build', schema: REVIEW })
  if (rv && rv.blocking.length) {
    await agent(`Revise ${OV}/research/district/PHONE-STUDY-2.md for these blocking review findings: ${JSON.stringify(rv.blocking)}. Do not modify code or tools.`, { label: 'district:study-revise', phase: 'Build', schema: DECISION })
    rv = await agent(`You are a separate Opus reviewer of the revised ${OV}/research/district/PHONE-STUDY-2.md (the same lens as before: safety on the signed-in account first). Do not modify anything.`, { label: 'district:study-review#2', phase: 'Build', schema: REVIEW })
    if (!rv || rv.blocking.length) { log('District re-run procedure not safe after one revision: not run; the orchestrator looks at it'); return 'district: procedure not approved' }
  }
  const apk = await agent(opPrompt(`Build the template side of the District re-run: the catalog release APK named in ${OV}/research/district/PHONE-STUDY-2.md from staging's head (${WTS}, read-only for you) through ${L} gradle into ${OV}/research/district/phone-2/apks/ with sha256. Use no adb.`),
    { label: 'district:apk', phase: 'Build', schema: RAW, model: 'sonnet' })
  const run = await withPhone(3, () => agent(opPrompt(`The District re-run: run only what ${OV}/research/district/PHONE-STUDY-2.md lists, as one command ${L} adb bash ${OV}/research/district/tools/run-2.sh (first it records District's compilation filter with dumpsys package, never forcing a compile). A step the tool refuses is not retried and not tapped another way: record it under "by hand for the user". Raw outputs under ${OV}/research/district/phone-2/ (local only, excluded from backups). Build report: ${JSON.stringify(apk)}.`),
    { label: 'district:rerun-run', phase: 'Build', schema: RAW, model: 'sonnet' }))
  const a = await agent(`You analyse the District re-run (Opus). Operator report: ${JSON.stringify(run)}. Re-derive every curve and frame number from ${OV}/research/district/phone-2/, compare with the template's numbers, and write ${OV}/research/district/PHONE-RESULTS-2.md with proposals in the format of research/district/RESEARCH.md (what District does differently, measured; the Compose way; per-frame cost; pros, cons, catches) and the by-hand steps left for the user. Ideas only: no decompiling, no copied assets. Do not modify code.`,
    { label: 'district:rerun-analyse', phase: 'Build', schema: DECISION })
  const rr = await agent(`You are a separate Opus reviewer of ${OV}/research/district/PHONE-RESULTS-2.md: every number against the raw files, every proposal honest about its cost on the Redmi. Do not modify anything. There is no pressure to find something.`,
    { label: 'district:rerun-review', phase: 'Build', schema: REVIEW })
  if (rr && rr.blocking.length) await agent(`Revise ${OV}/research/district/PHONE-RESULTS-2.md for these blocking review findings: ${JSON.stringify(rr.blocking)}.`, { label: 'district:rerun-revise', phase: 'Build', schema: DECISION })
  log('District re-run written: the orchestrator asks each proposal in chat one by one (REQUIREMENTS 45, 57); nothing is built from it in this run')
  return a
})().catch(e => `district error: ${e}`)

phase('Build')
const flows = STAGES.map(s => stageFlow(s)
  .catch(e => ({ id: s.id, state: 'error', summary: String(e) }))
  .then(r => { results[s.id] = Object.assign({}, results[s.id] || {}, { final: r }); endedOf(s.id).r(true); return r }))

// ---------- WP13 (release-pipeline.md section 4.2) ----------
phase('Verify')
const wp13 = { id: 'WP13', lanes: ['WP13'], branches: ['staging-wp/WP13'], reqs: '9, 19, 20, 36, 39, 40, 41', phone: true, gate: 'phone', docs: false, kind: 'android', start: ['WP12-KNOW', 'WP02-FMT', 'WP13-PREP'], after: ['WP12-KNOW', 'WP02-FMT', 'WP13-PREP'], holds: [], prio: 1, maxChecks: 3, maxWaits: 6, merge: true,
  plan: `${OV}/design/wp/WP13.md, ${OV}/scripts/to-release.js (the WP13 scope), ${P} section 4.2, ${OV}/followups.md`,
  scope: `The Android release verification of release-pipeline.md 4.2 on the final code.` }
STAGE.WP13 = wp13
let ciP = Promise.resolve({ done: false, summary: 'not requested' })
let adminP = Promise.resolve({ done: false, summary: 'not requested' })
const wp13Flow = (async () => {
  const w = await waitFor(wp13.after.map(k => `${k}:merged`))
  if (w !== 'ok') return { id: 'WP13', state: 'blocked', summary: `${w} never arrived` }
  const built = await onLanes(['WP13'], async () => {
    const im = await runImpl(wp13, 'impl', { text: `WP13 steps: merge staging (final); write ${RD}/WP13/PROFILES.md, MATRIX.md and GATES.md as operator procedures (the baseline-profile re-record of build order step 11 as one session script; the static matrix, tools/matrix.py --all and --wide, the site and API checks; the final device gates of every preset, the images and video columns on the catalog release APK against the self-hosted sample-media-v1 set (every asset URL checked with curl -I and a Range request first), everything-glass Live and Frosted vs Solid, the Macrobenchmark journeys, the HUD prod absence and overhead, iOS scrolling against the iOS curve, and the old template against the new for everything, standard and lean in one session, the old one generated by main's generator from a detached read-only checkout ${OV}/wt/sw-oldmain that nothing commits to; GATES.md has a Build section and the session script ${RD}/WP13/run-gates.sh); write ${RD}/WP13/ADMIN.md, the signed-in /admin click-through procedure the user runs with the orchestrator (REQUIREMENTS 57: the user types an admin token into a local run; it is never written anywhere); finish CI from WP13-PREP; run the two Android security skills over a generated everything app and check the certificate pins on the Cronet path; close followups.md (each item in its own commit naming it, or shown done); the slop sweep. Commit.` })
    if (im.state !== 'done') return im
    const pr = await withPhone(1, () => agent(opPrompt(`Follow ${RD}/WP13/PROFILES.md exactly: its whole session as one command (Gradle inside the adb lock: ${L} adb ${L} gradle ...). Raw outputs under ${OV}/perf/WP13/profiles/.`), { label: 'WP13:profiles-run', phase: 'Verify', schema: RAW, model: 'sonnet' }))
    const pv = await runImpl(wp13, 'profiles', { text: `MODE: verify WP13's baseline-profile run and commit the result: check the generated profile covers the final code (core/ui's StateHold and the readiness path, the shell, the media modules), split it into the checked-in baseline-prof.txt files with split_profile.py, rebuild row 6, commit on staging-wp/WP13. Run report: ${JSON.stringify(pr)}.` })
    if (pv.state !== 'done') return pv
    let ok = false
    let ciAsked = false
    for (let k = 1; k <= 3 && !ok; k++) {
      // APKs first, then the matrix (Gradle) and the device gates (the phone) side by side.
      const ga = await withTrees(['WP13'], () => agent(opPrompt(`Build exactly the APKs the Build section of ${RD}/WP13/GATES.md lists, round ${k}, through ${L} gradle in ${wtOf('WP13')} (record git rev-parse HEAD and git status --porcelain first; stop if the tree is not clean), into ${OV}/perf/WP13/gates-${k}/apks/ with sha256. Use no adb.`), { label: `WP13:gates-apk#${k}`, phase: 'Verify', schema: RAW, model: 'sonnet' }))
      const [mx, gr] = await parallel([
        () => withTrees(['WP13'], () => agent(opPrompt(`Follow ${RD}/WP13/MATRIX.md exactly, round ${k}, in ${wtOf('WP13')} (Gradle through the lock, Python and npm through memwait). Logs under ${OV}/out/WP13/matrix-${k}/.`), { label: `WP13:matrix-run#${k}`, phase: 'Verify', schema: RAW, model: 'sonnet' })),
        () => withPhone(1, () => agent(opPrompt(`Run WP13's final device gates, round ${k}: exactly one command, ${L} adb bash ${RD}/WP13/run-gates.sh ${k}, with the APKs in ${OV}/perf/WP13/gates-${k}/apks/. Raw outputs under ${OV}/perf/WP13/gates-${k}/. Build report: ${JSON.stringify(ga)}.`), { label: `WP13:gates-run#${k}`, phase: 'Verify', schema: RAW, model: 'sonnet' })),
      ])
      const gt = { ga, gr }
      const [mv, gv] = await parallel([
        () => agent(`You are the Opus verifier of WP13's matrix, round ${k}. Report: ${JSON.stringify(mx)}. Open every log, recount rows, tests, sizes against tools/size_budgets.toml; write ${OV}/out/WP13/matrix-${k}/VERIFY.md. Do not modify code. ${RULES}`, { label: `WP13:matrix-verify#${k}`, phase: 'Verify', schema: CHECK }),
        () => agent(`You are the Opus verifier of WP13's final device gates, round ${k}. Reports: ${JSON.stringify(gt)}. Check every APK's sha256; re-derive every gate table from the raw metrics against gates.toml (solid, images, video, standard, lean, glass_live, glass_frosted_vs_solid), the Macrobenchmark results, the HUD inspection and overhead, the iOS curve fit, and the old-vs-new rows for FINAL.md; look at the recordings. Write ${OV}/perf/WP13/gates-${k}/VERIFY.md. Do not modify code. ${RULES}`, { label: `WP13:gates-verify#${k}`, phase: 'Verify', schema: CHECK }),
      ])
      ok = !!(mv && mv.pass && gv && gv.pass)
      if (mv && mv.pass && !ciAsked) {
        ciAsked = true
        ciP = orchestratorStep('ci-only-pr', `WP13's matrix is green on staging-wp/WP13 (${OV}/out/WP13/matrix-${k}/VERIFY.md): open the CI-only draft PR from a throwaway branch, record the run's job results in ${RD}/WP13/CI-RESULT.md, close it unmerged (REQUIREMENTS 57)`, `Read ${RD}/WP13/CI-RESULT.md, and gh run list / gh run view (read-only) for the run it names.`, 30)
        adminP = orchestratorStep('admin-check', `run the signed-in /admin click-through with the user (${RD}/WP13/ADMIN.md; the user types the token into a local run, never written anywhere) and record the result in ${RD}/WP13/ADMIN-RESULT.md`, `Read ${RD}/WP13/ADMIN-RESULT.md.`, 30)
      }
      if (!ok) {
        const fx = await runImpl(wp13, `fix#m${k}`, { text: `MODE: fix the red rows (one commit per failing row, naming it): matrix ${JSON.stringify(mv ? mv.failures : ['verifier failed'])}; gates ${JSON.stringify(gv ? gv.failures : ['verifier failed'])}. A row whose cause belongs to a merged stage is fixed here at the root with a test; never loosen a limit; a shared navigation file only as its order allows (FILE ORDER).` })
        if (fx.state !== 'done') return fx
      }
    }
    if (!ok) return { state: 'blocked', summary: 'WP13 matrix or device gates still red after three rounds', heads: [] }
    const fin = await runImpl(wp13, 'final', { text: `MODE: report. Write ${OV}/report/FINAL.md (release-pipeline.md 4.2 step 6) from the verified files only, every number with how it was measured and the staging head it was measured on; update WP12's docs numbers from it in the same branch; ${OV}/design/wp/WP13.md acceptance: ${OV}/out/WP13/accept.sh ends ACCEPT OK. Commit.` })
    if (fin.state !== 'done') return fin
    for (let r = 1; r <= 4; r++) {
      const rv = await reviewRound(wp13, r)
      if (rv.clean) {
        const fr = await agent(`You are a separate Opus reviewer of ${OV}/report/FINAL.md: check every number against the raw files and VERIFY.md files it cites; any claim without evidence is blocking. Do not modify anything.`, { label: `WP13:final-review#${r}`, phase: 'Verify', schema: REVIEW })
        if (fr && !fr.blocking.length) return { state: 'ready', summary: fin.summary, heads: readyHeads.WP13 || [] }
        const fx = await runImpl(wp13, `fix#final${r}`, { text: `MODE: fix FINAL.md: ${JSON.stringify(fr ? fr.blocking : ['the FINAL.md review failed to run'])}` })
        if (fx.state !== 'done') return fx
      } else if (!rv.fixed) return { state: 'blocked', summary: rv.summary, heads: [] }
    }
    return { state: 'blocked', summary: 'WP13 reviews not clean', heads: [] }
  })
  if (built.state !== 'ready') return { id: 'WP13', state: built.state, summary: built.summary }
  fire('WP13:ready')
  return mergeLoop(wp13, built.heads && built.heads.length ? built.heads : (readyHeads.WP13 || []), built.summary)
})().catch(e => ({ id: 'WP13', state: 'error', summary: String(e) })).then(r => { endedOf('WP13').r(true); return r })

// ---------- REQUIREMENTS 52 and 46 (release-pipeline.md section 5) ----------
const DIMS = [
  { key: 'security', text: 'security: exported components, intents and deep links, the app lock, storage and crypto, network security config and certificate pins (the Cronet path too), permissions (the android-intent-security and android-permissions-security skills); web and API: OWASP top 10, auth, input handling, headers; secrets in the tree and in history; insecure defaults and sharp edges; dependency advisories' },
  { key: 'architecture', text: 'architecture: module boundaries and the dependency rules of AGENTS.md, layering, MVI and unidirectional data flow, DI, the generator structure, dead code' },
  { key: 'speed', text: `speed: recomposition, allocations and work per frame, start-up, lists, images and video; read ${OV}/research/COMPOSE-PERF-AUDIT.md (its R52 findings) and the speed run's verified report` },
  { key: 'standards', text: `standards: ${OV}/research/CODE-STANDARDS.md, Kotlin and Go style, lint, accessibility, strings and i18n, the docs (REQUIREMENTS 25, 50), H-17 (WP01 deviations 1 and 11 accepted or fixed)` },
  { key: 'licensing', text: 'licensing and privacy: no copied third-party code (GPL above all; Telegram was read for ideas only, REQUIREMENTS 48), media licences (CC0 or Unsplash; the self-hosted clips and their CREDITS.md), no private data, nothing named periflow, vitfleet or jorwen anywhere in the tree, the catalogue or the site (REQUIREMENTS 54), the MIT LICENSE with Google skills on Apache-2.0' },
]
const AREAS = [
  { key: 'android', text: 'the Android template and every preset (templates/android), the catalog, build-logic' },
  { key: 'generator', text: 'the generator, its CLI and project tools, tools/, CI (.github)' },
  { key: 'web', text: 'the website (web/frontend) and the API (web/api)' },
]
function findKey(f) { return `${f.dimension}|${f.file}|${f.line || 0}|${String(f.problem).slice(0, 80)}` }

const lateFixes = []
async function r52Fix(area, round, items) {
  const s = { id: `R52-${area}-${round}`, lanes: [`R52${area}`], branches: [`staging-wp/R52-${area}-${round}`], reqs: '52', phone: false, gate: 'phone', docs: false, kind: area === 'android' ? 'android' : 'python', start: [], after: ['WP13'], holds: [], prio: 1, maxChecks: 3, maxWaits: 6, merge: true,
    plan: `${OV}/REQUIREMENTS.md item 52, ${OV}/report/R52-round-${round}.md`,
    scope: `Fix these confirmed blocking findings of the REQUIREMENTS 52 review at their root, each with a test where the cause is code: ${JSON.stringify(items)}. If a fix changes code the baseline profiles cover, re-record and split the profiles in this stage (a phone session, then split_profile.py) so the shipped profiles match the shipped code.` }
  STAGE[s.id] = s
  const r = await stageFlow(s).catch(e => ({ id: s.id, state: 'error', summary: String(e) }))
  endedOf(s.id).r(true)
  lateFixes.push(s.id)
  return r
}

const reviewP = (async () => {
  const w = await waitFor(['WP13:merged'])
  if (w !== 'ok') return { clean: false, summary: `${w} never arrived` }
  phase('Review')
  const seen = new Set()
  const confirmedAll = []
  let clean = false
  let lastFixed = []
  for (let round = 1; round <= 3 && !clean; round++) {
    const speed = (round === 1 || lastFixed.includes('android')) ? await (async () => {
      const sa = await agent(opPrompt(`REQUIREMENTS 52 speed run, round ${round}, build part, on staging's head (${WTS}, read-only for you): the measured APKs of the everything preset, its catalog release APK and everything-glass, through ${L} gradle, into ${OV}/perf/R52/round-${round}/apks/ with sha256; if ${OV}/perf/WP13/ holds a verified run on a byte-identical tree (git diff <its commit> staging empty), say so and build nothing. Use no adb.`), { label: `R52:speed-apk#${round}`, phase: 'Review', schema: RAW, model: 'sonnet' })
      const run = await withPhone(1, () => agent(opPrompt(`REQUIREMENTS 52 speed run, round ${round}, phone part: unless the build part found a reusable WP13 run (then run nothing and say so), write ${OV}/perf/R52/round-${round}/run.sh with perf.sh --gate solid, --gate images and --gate video (catalog APK), glass_live and glass_frosted_vs_solid (everything-glass), 3 runs each, and a Perfetto trace (perf.sh trace) for every scenario, and run it as one command ${L} adb bash ${OV}/perf/R52/round-${round}/run.sh. Raw outputs under ${OV}/perf/R52/round-${round}/. Build report: ${JSON.stringify(sa)}.`), { label: `R52:speed-run#${round}`, phase: 'Review', schema: RAW, model: 'sonnet' }))
      return agent(`You are the Opus verifier and trace analyst of the REQUIREMENTS 52 speed run, round ${round}. Reports: ${JSON.stringify({ sa, run })}. Re-derive every row; for any red row, read its trace with trace_processor and name the cause at file:line. Return each blocking speed problem as a finding (dimension speed, area android). Do not modify code. ${RULES}`, { label: `R52:speed-verify#${round}`, phase: 'Review', schema: FINDINGS })
    })() : { findings: [] }
    const found = (await parallel(DIMS.flatMap(d => AREAS.map(a => () => agent(`You are a separate Opus reviewer for REQUIREMENTS 52 (the full code review before main), dimension ${d.text}; area ${a.text}. Review everything that ships on staging's head in ${WTS} (read-only; git -C ${R} show staging:<path> and git grep). ${round > 1 ? `Round ${round}: the earlier confirmed findings were fixed (${OV}/report/R52-round-${round - 1}.md); check each fix holds (mark a finding that remains with recheck true) and report what remains or is new. ` : ''}Report only BLOCKING problems (a real defect, vulnerability, rule breach or requirement gap a Google/AOSP reviewer would stop a release for), each with file, line, why it is blocking and the evidence. Do not modify anything. There is no pressure to find something.`,
      { label: `R52:find#${round}:${d.key}-${a.key}`, phase: 'Review', schema: FINDINGS }))))).filter(Boolean).flatMap(r => r.findings).concat(speed ? speed.findings : [])
    const fresh = []
    found.forEach(f => { const k = findKey(f) + (f.recheck ? `#r${round}` : ''); if (!seen.has(k)) { seen.add(k); fresh.push(f) } })
    log(`R52 round ${round}: ${found.length} found, ${fresh.length} new`)
    if (!fresh.length) { clean = true; break }
    const judged = await parallel(fresh.map((f, i) => () => parallel([1, 2, 3].map(v => () => agent(`You are adversarial verifier ${v} of a REQUIREMENTS 52 finding. Try to REFUTE it: read the code on staging (${WTS}, read-only), check whether the problem is real, reachable and blocking for a release at the Google/AOSP bar. Default to refuted true if you are unsure. Finding: ${JSON.stringify(f)}`,
      { label: `R52:verify#${round}:${i}:${v}`, phase: 'Review', schema: VOTE }))).then(vs => ({ f, real: vs.filter(Boolean).filter(x => !x.refuted).length >= 2 }))))
    const confirmed = judged.filter(Boolean).filter(j => j.real).map(j => j.f)
    log(`R52 round ${round}: ${confirmed.length} confirmed`)
    if (!confirmed.length) { clean = true; break }
    await agent(`Write ${OV}/report/R52-round-${round}.md: every confirmed blocking finding of round ${round} (${JSON.stringify(confirmed)}) with its dimension, area, file:line, evidence and the verifiers' agreement, and the refuted ones in a short list (${JSON.stringify(judged.filter(Boolean).filter(j => !j.real).map(j => j.f))}). Do not modify code.`, { label: `R52:record#${round}`, phase: 'Review', schema: DECISION })
    confirmedAll.push(...confirmed)
    const byArea = {}
    confirmed.forEach(f => { const a = AREAS.some(x => x.key === f.area) ? f.area : 'android'; (byArea[a] = byArea[a] || []).push(f) })
    lastFixed = Object.keys(byArea)
    const fixes = await parallel(lastFixed.map(a => () => r52Fix(a, round, byArea[a])))
    const failed = fixes.filter(x => !x || x.state !== 'merged')
    if (failed.length) return { clean: false, summary: `R52 fixes not merged: ${JSON.stringify(failed)}` }
  }
  if (!clean) return { clean: false, summary: 'R52 still finds blocking problems after three rounds' }
  // REQUIREMENTS 46: the matrix on the final head, as the requirements dimension and a gate condition
  const GROUPS = [{ key: '1-15', text: 'items 1 to 15' }, { key: '16-30', text: 'items 16 to 30' }, { key: '31-45', text: 'items 31 to 45' }, { key: '46-57', text: 'items 46 to 57 and every unnumbered item (memory safety, process rules)' }]
  let matrixClean = false
  for (let it = 1; it <= 2 && !matrixClean; it++) {
    const mprompt = (g, extra) => `You are an Opus requirements auditor (REQUIREMENTS 46, the requirements dimension of REQUIREMENTS 52) at the Google/AOSP bar, on staging's final head in ${WTS} (read-only). For ${OV}/REQUIREMENTS.md ${g.text} (every sub-point its own row): met (with proof: a test that exercises it, a phone capture or perf number under ${OV}/perf/ or ${OV}/report/FINAL.md, or a desktop check you run now through the Gradle lock or memwait; never the phone), partly, not-met, or deferred (only by a user decision you cite: phase B by item 36, PeriFlow, District proposals after the release by item 57); and whether it is done the smoothest way on the Redmi (if not, the better way and its cost). A row with no proof is partly at best. Write your rows to ${OV}/report/REQUIREMENTS-MATRIX-final.md under "Items ${g.key}" (pass ${it}).${extra}`
    let rows = (await parallel(GROUPS.map(g => () => agent(mprompt(g, ''), { label: `R46:audit#${it}:${g.key}`, phase: 'Review', schema: MATRIX })))).filter(Boolean)
    const mr = await agent(`You are a separate Opus reviewer of ${OV}/report/REQUIREMENTS-MATRIX-final.md (pass ${it}): every "met" against its proof (open it), every "deferred" against a real user decision, no requirement or sub-point missing, best_way honest about red perf rows. Rows: ${JSON.stringify(rows.map(r => r.rows))}. Do not modify anything. There is no pressure to find something.`, { label: `R46:review#${it}`, phase: 'Review', schema: REVIEW })
    if (mr && mr.blocking.length) rows = (await parallel(GROUPS.map(g => () => agent(mprompt(g, `\nRevision: a separate reviewer found ${JSON.stringify(mr.blocking)}; fix your rows (re-check the proof) and return all of them.`), { label: `R46:audit#${it}:${g.key}:rev`, phase: 'Review', schema: MATRIX })))).filter(Boolean)
    const gaps = rows.flatMap(r => r.rows).filter(r => r.verdict === 'partly' || r.verdict === 'not-met')
    log(`R46 pass ${it}: ${gaps.length} gaps`)
    if (!gaps.length) { matrixClean = true; break }
    const byArea = {}
    gaps.forEach(g => { const a = AREAS.some(x => x.key === g.owner_area) ? g.owner_area : 'android'; (byArea[a] = byArea[a] || []).push(g) })
    const fixes = await parallel(Object.keys(byArea).map(a => () => r52Fix(a, 10 + it, byArea[a])))
    if (fixes.some(x => !x || x.state !== 'merged')) return { clean: false, summary: 'requirement gap fixes not merged' }
  }
  if (!matrixClean) return { clean: false, summary: 'the requirements matrix still has gaps after two fix passes' }
  // Fixes merged after WP13 that touched a measured path: re-measure on the final head, so FINAL.md and the docs
  // numbers describe the shipped code.
  const touchedLate = lateFixes.filter(id => results[id] && results[id].touched)
  if (touchedLate.length) {
    const plan = await agent(`You plan the final re-measure (Opus). Fix stages ${JSON.stringify(touchedLate)} merged after WP13 measured (${OV}/report/FINAL.md names the head it measured) and touched measured code. From git -C ${R} diff <that head> staging, decide which rows of ${RD}/WP13/GATES.md they can move (when unsure, all of them) and write ${RD}/WP13/REMEASURE.md: a Build section and the session script ${RD}/WP13/run-remeasure.sh. Do not modify code.`, { label: 'final:remeasure-plan', phase: 'Review', schema: DECISION })
    const ra = await agent(opPrompt(`Build exactly the APKs the Build section of ${RD}/WP13/REMEASURE.md lists, from staging's head (${WTS}, read-only for you), through ${L} gradle into ${OV}/perf/WP13/remeasure/apks/ with sha256. Use no adb. Plan: ${JSON.stringify(plan)}.`), { label: 'final:remeasure-apk', phase: 'Review', schema: RAW, model: 'sonnet' })
    const rr = await withPhone(1, () => agent(opPrompt(`Run the final re-measure as one command: ${L} adb bash ${RD}/WP13/run-remeasure.sh, with the APKs in ${OV}/perf/WP13/remeasure/apks/. Raw outputs under ${OV}/perf/WP13/remeasure/. Build report: ${JSON.stringify(ra)}.`), { label: 'final:remeasure-run', phase: 'Review', schema: RAW, model: 'sonnet' }))
    const rv = await agent(`You are the Opus verifier of the final re-measure. Reports: ${JSON.stringify({ ra, rr })}. Re-derive every row against gates.toml and against FINAL.md's numbers; write ${OV}/perf/WP13/remeasure/VERIFY.md. Pass only if every release row passes and every number FINAL.md and the docs quote still holds within the harness's spread. Do not modify code. ${RULES}`, { label: 'final:remeasure-verify', phase: 'Review', schema: CHECK })
    await agent(`Add an addendum to ${OV}/report/FINAL.md (Opus): the fixes ${JSON.stringify(touchedLate)} merged after its measurement, the re-measured rows from ${OV}/perf/WP13/remeasure/VERIFY.md on today's staging head, and whether every number the generated docs quote still holds. Do not modify any repository.`, { label: 'final:addendum', phase: 'Review', schema: DECISION })
    if (!rv || !rv.pass) return { clean: false, summary: `the final re-measure after the late fixes is not green: ${rv ? JSON.stringify(rv.failures) : 'verifier failed'}` }
  }
  return { clean: true, summary: 'R52 clean, every Android requirement met with proof or deferred by the user, and the shipped head measured', confirmed: confirmedAll.length }
})().catch(e => ({ clean: false, summary: `review error: ${e}` }))

// ---------- the gate, then stop ----------
const stageResults = await Promise.all(flows)
const wp13Result = await wp13Flow
const review = await reviewP
const ci = await ciP
const admin = await adminP
phase('Gate')
const gate = await agent(`You are the release gate for the Android release of the android-base overhaul (REQUIREMENTS 36, 46, 52, 57). You are adversarial: default to NO-GO when evidence is missing. Do not modify anything.
Check that every Android requirement is met on staging (${WTS}) with proof or deferred by the user: ${OV}/report/REQUIREMENTS-MATRIX-final.md, ${OV}/report/FINAL.md and its addendum (measured on the head that ships, or re-measured), ${OV}/report/R52-round-*.md (the REQUIREMENTS 52 review: ${JSON.stringify(review)}), ${OV}/out/WP13/ (ACCEPT OK, the matrix), ${OV}/perf/WP13/ (the final device gates), ${RD}/merges.md and ${RD}/gates/ (every merge in staging covered by a green gate; ${RD}/HALT.md must not exist: merges halted = ${JSON.stringify(mergeHalted)}), ${OV}/followups.md (closed), the docs (REQUIREMENTS 25, 50), the website (only Android selectable, REQUIREMENTS 36), NET (REQUIREMENTS 39, in this release), the HUD absent from prod (REQUIREMENTS 40), the self-hosted sample media (every URL answers), the code itself where a claim looks thin. The user's own items: the CI-only draft PR's jobs (${JSON.stringify(ci)}), the signed-in /admin check (${JSON.stringify(admin)}), the Vercel settings (ENABLE_TARGETS=android, ADMIN_TOKEN, DATABASE_URL, TRUSTED_PROXY; listed as open until the user confirms them). Stage results: ${JSON.stringify(stageResults.concat([wp13Result]).map(r => ({ id: r.id, state: r.state })))}.
Write ${OV}/RELEASE-GATE-android.md with GO or NO-GO and every gap concretely. Return go=true only if nothing that applies is missing.`,
  { label: 'gate:android', phase: 'Gate', schema: GATE })
if (!gate) throw new Error('release gate failed')
log(`release gate: ${gate.go ? 'GO; stop here: the orchestrator asks the user, and only their explicit go in chat lets the orchestrator merge staging into main' : 'NO-GO, ' + gate.gaps.length + ' gaps'}`)
return { mode: MODE, intake: { staging_head: intake.staging_head, regression_open: intake.regression_open }, stages: stageResults, wp13: wp13Result, review, ci, admin, halted: mergeHalted, district: await districtP, gate, trains: trainNo }
