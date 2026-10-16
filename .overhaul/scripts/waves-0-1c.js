export const meta = {
  name: 'android-base-research-and-waves-0-1',
  description: 'Research (performance, iOS motion, recomposition, adaptive, Google/AOSP standards) and visual QA routed into per-package plans; then resume the wave 0-1 packages with dependency waits, review against the Google bar, and serialized merges',
  phases: [
    { title: 'Research', detail: 'five researchers and one QA inspector' },
    { title: 'Plan', detail: 'two mappers write per-package plans' },
    { title: 'Implement', detail: 'package agents in their worktrees' },
    { title: 'Review', detail: 'fresh-context reviewer per package' },
    { title: 'Merge', detail: 'serialized merges with integration checks' },
    { title: 'Checkpoint', detail: 'motion-feel decision on behalf of the user' },
  ],
}

const R = '/home/crazyhusir/Documents/projects/tools/android-base'
const OV = '/home/crazyhusir/Documents/projects/tools/.overhaul'
const DESIGN = OV + '/design'
const RS = OV + '/research'
const L = OV + '/bin/locked'
const SKILLS = '/home/crazyhusir/.claude/plugins/marketplaces/android-skills'

// ---------- signals ----------
const signals = {}
function signal(k) {
  if (!signals[k]) { let r; const p = new Promise(res => { r = res }); signals[k] = { p, r, done: false } }
  return signals[k]
}
function fire(k) { const s = signal(k); if (!s.done) { s.done = true; s.r(true); log(`signal: ${k}`) } }
const doneKeys = () => Object.keys(signals).filter(k => signals[k].done)
const ended = {}
function endedOf(name) {
  if (!ended[name]) { let r; const p = new Promise(res => { r = res }); ended[name] = { p, r } }
  return ended[name]
}
const producerOf = k => (k === 'CP1' || k === 'PLANS' ? k : k.split(':')[0])
function waitFor(keys) {
  if (!keys.length) return Promise.resolve('ok')
  const all = Promise.all(keys.map(k => signal(k).p)).then(() => 'ok')
  const dead = keys.map(k => endedOf(producerOf(k)).p.then(() => (signal(k).done ? new Promise(() => {}) : 'dead:' + k)))
  return Promise.race([all, ...dead])
}
// Already merged before this run, and milestones already reached.
for (const k of ['WP01', 'WP02-M1', 'WP03:phaseA', 'WP04:phase2']) fire(k)

// ---------- research ----------
const BASE = `Project: the user's app project generator "android-base" (${R}, integration branch overhaul/multiplatform-v2): an Android template (Jetpack Compose with its own design system on foundation only, no material3; Compose UI 1.12 / BOM 2026.09; Navigation 3 1.2.0; Kotlin 2.4; AGP 9; minSdk 26; targetSdk 37), a Compose Multiplatform template and a SwiftUI template are being built. Read ${OV}/REQUIREMENTS.md fully first (items 17-30 were added by the user during the work and are the priority). Reference phone: Redmi Note 10 Pro (Snapdragon 732G, Adreno 618, 120 Hz). Measured baseline and the new build: ${OV}/perf/WP04/baseline/summary.md and ${OV}/perf/WP04/phase2/summary.md. Google's official Android agent skills are installed at ${SKILLS} (one SKILL.md per skill, with references/): treat them as a primary source where relevant. Today is 2026-10-09. Use WebSearch/WebFetch (ToolSearch "select:WebSearch,WebFetch"); prefer primary sources (developer.android.com, androidx and AOSP source on cs.android.com / android.googlesource.com, Compose release notes, Google I/O, Apple developer docs/WWDC); cite URLs; mark anything unverified. Be concrete: APIs, versions, constants, code patterns, expected effects. Do not modify any repository or worktree.`
const RESEARCH = [
  { key: 'compose-perf', brief: 'Minimum jank in Compose on a 120 Hz mid-range phone: pausable composition and lazy prefetch (cache windows), keeping the next/previous screen warm so pops and first visits have no cold first frame (the new build still has pop first frame 15/37 ms and push/pop p99 57 ms), Modifier.Node, deferred reads, compositing strategies (ModulateAlpha vs offscreen), text layout cost, baseline + startup profiles and dex layout, R8 full mode, ART/JIT warm-up on first navigation, shader-compile stalls (AGSL prewarm), GC churn, preferredFrameRate/FrameRateCategory and ARR, Skia vs Vulkan HWUI, how to measure (FrameTimeline, Perfetto, JankStats, Macrobenchmark FrameTimingMetric), what jank % top apps reach and how.' },
  { key: 'ios-motion', brief: `Exhaustive iOS 17-26 motion catalogue with exact parameters (push/pop parallax+dim, interactive pop thresholds, tab switching, sheets with detents and the card stack, zoom transition, context menu lift, alerts/action sheets, large-title collapse, tab bar minimize, UIScrollView rubber-band formula and deceleration, switch/segmented/picker, list insert/delete/move, swipe actions, UIRefreshControl, search focus, keyboard sync, toast/HUD, SF Symbols bounce, Liquid Glass morph) AND the optimal Compose implementation of each (springs from response+bounce, Animatable velocity hand-off, SeekableTransitionState, SharedTransitionLayout costs, overscroll/rubber band, fling/decay to mimic UIScrollView, anchoredDraggable, animateItem, IME sync, NavigationEvent handlers). Three items need a full design with code sketches: (1) a polished but cheap TAB SWITCH effect for kept tabs (no rebuild, no double exposure, no full-screen offscreen alpha) plus bar selection motion (indicator morph/stretch, icon bounce); (2) an iOS-level PULL TO REFRESH (content follows the finger with rubber-band resistance, spokes revealed progressively, haptic tick at threshold, rests at spinner height while refreshing, springs back with velocity, no surface/shadow, zero per-frame recomposition) — study the current one at ${R}/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/refresh/AppPullToRefresh.kt and the recordings ${OV}/qa/ptr/ (black ring = elevation shadow through an alpha-faded surface), VitFleet's improved version (~/Documents/projects/tools/vitfleet/vitfleet-android/core/designsystem, AppPullToRefresh.kt) and androidx material3 PullToRefresh source for technique; (3) the GMAIL-STYLE PREDICTIVE BACK: exact constants from AOSP WindowManager Shell back animations (CrossActivityBackAnimation / DefaultCrossActivityBackAnimation, interpolators, scale, margins, corner radius, touch-Y following, scrim, commit/cancel animations) and Material guidance; frames recorded from the user's phone are in ${RS}/predictive-back/gmail/frames/ (open a few PNGs per clip to measure scale and corner radius). A partial earlier catalogue exists at ${RS}/ios-motion-catalogue.md; extend it.` },
  { key: 'recomposition', brief: 'Recomposition optimisation state of the art (Compose compiler 2.x strong skipping): stability inference and config files, @Stable/@Immutable, immutable collections, lambda memoisation, static vs dynamic CompositionLocals, derivedStateOf/snapshotFlow, lazy keys/contentType, reading state in the lowest scope, deferring reads to layout/draw, no recomposition during animations, compiler metrics/reports gated in CI (fail on unstable params in public design-system APIs), recomposition counting in tests (a harness that counts recompositions of key composables during a scripted tab switch, push/pop, sheet drag, press feedback and scroll, with budgets), composition tracing in Perfetto, and what large apps do.' },
  { key: 'adaptive', brief: 'Best-in-class adaptive UI and how a template ENFORCES it: WindowSizeClass breakpoints incl. large/extra-large widths and height classes without material3, Navigation 3 list-detail/supporting-pane scenes, adaptive navigation bar -> rail -> sidebar (iPadOS 18 parity), foldables (FoldingFeature, tabletop/book, hinge), Android 16+ large-screen orientation/resizability rules, desktop windowing and ChromeOS (free resize, multi-instance, state continuity, no tab disposal on class change), input (keyboard focus and shortcuts, hover, right-click menus, stylus, drag and drop), large-screen quality tiers; enforcement: an adaptive layer with tokens per class and self-adapting components, build-time code rules (no fixed screen-sized dimensions, no orientation locks), multi-size previews, screenshot tests at several sizes (pick a tool that works with AGP 9 and no material3), resize/state-restoration tests, docs rules for agents; parity for Compose Multiplatform (iPad/desktop) and SwiftUI (size classes, NavigationSplitView). Read the adaptive skill at ' + SKILLS + '/jetpack-compose/adaptive.' },
  { key: 'standards', brief: `The Google/AOSP code-quality bar and project organisation. Read the androidx Compose API guidelines (compose-api-guidelines.md and compose-component-api-guidelines.md in androidx), the Android Kotlin style guide, the official architecture guide (UI layer, domain, data, modularisation guide), Now in Android's module and package organisation, and how AOSP/androidx implement comparable components. Digest every relevant skill under ${SKILLS} (navigation-3, navigation-event, adaptive, theming/styles, edge-to-edge, agp-9-upgrade, r8-analyzer, testing-setup, android-profiler, intent/permission security) into: (a) ${RS}/CODE-STANDARDS.md — the concrete rules reviewers and implementers apply (naming, API shape, parameter order, modifiers, defaults objects, state hoisting, slot APIs, visibility, KDoc/comment policy with what a non-slop comment looks like vs slop, error handling, coroutines, testing, file/package layout), each with a short do/don't; (b) a proposal for a DEEPER, well-organised package and folder structure inside each module type (feature, data, core:designsystem with foundation/tokens/components by family, core:navigation, app) for million-user apps, consistent with the api/impl split already merged (read ${DESIGN}/MASTER-PLAN.md A2.2-A2.4 and the current tree at ${R}/templates/android); (c) which of the Android skills each generated project should vendor and how AGENTS.md/CLAUDE.md should point agents at them (licence: Apache-2.0).` },
]
const QA_PROMPT = `${BASE}

You are a meticulous visual QA inspector with an Apple-designer eye. The user reported an opaque white block around the bottom bar cutting the UI, and asked to find more such defects. Screenshots of the in-progress build (new navigation shell on the OLD design system) were captured in ${OV}/qa/r1/shots/{light,dark}/ (see INDEX.md if present). Also inspect the pull-to-refresh recordings ${OV}/qa/ptr/frames/ and ${OV}/qa/ptr/peak_compare.png, the WP03 catalog frames under ${OV}/perf/WP03/ (new design system), and REQUIREMENTS.md items 17-18, 28-30 (defects already noted). Open the PNGs. List every defect (layout, chrome/insets, opaque bands, clipping, truncation, overlaps e.g. toast behind the bar, DEV badge over the title, oversized selected tab pill, contrast/dark mode, spacing rhythm, typography, component polish vs iOS). For each: find the responsible code (shell/bar/navigation -> ${OV}/wt/WP04/templates/android; components/catalog -> ${OV}/wt/WP03/templates/android; feature screens -> ${R}/templates/android/feature (WP02-M3); devtools/data -> WP06; generator -> WP02-M2), say whether the in-progress branch already fixes it, and assign the owner. Write ${OV}/qa/r1/DEFECTS.md (table) and ${OV}/qa/r1/for-<WP>.md per owner.`

phase('Research')
const research = Promise.all([
  ...RESEARCH.map(a => agent(`${BASE}\n\nYOUR TOPIC: ${a.brief}\n\nWrite ${RS}/${a.key}.md, ranked by importance for THIS project. Return a short summary.`, { label: `research:${a.key}`, phase: 'Research', schema: { type: 'object', properties: { summary: { type: 'string' } }, required: ['summary'] } })),
  agent(QA_PROMPT, { label: 'qa:inspect', phase: 'Research', schema: { type: 'object', properties: { summary: { type: 'string' }, count: { type: 'integer' } }, required: ['summary', 'count'] } }),
])

const plans = research.then(async () => {
  const MAPS = [
    { key: 'motion-perf', brief: `From ${RS}/compose-perf.md, ${RS}/ios-motion.md and ${RS}/recomposition.md write: ${RS}/SMOOTHNESS-PLAN.md (techniques ranked by effect on the measured weak spots: pop first frames and push/pop p99, tab switch janky and first-frame max, nav stress; each APPLIED with file:line / PARTIAL / MISSING in the current code, the concrete change, the owner), ${RS}/MOTION-PLAN.md (every iOS motion: status, exact spring/parameters consistent with AppTheme.springs, implementation, per-frame cost rule, owner, how to verify — including the tab-switch effect, pull to refresh and the Gmail predictive back), ${RS}/RECOMPOSITION-PLAN.md (checklist, CI gate on compiler metrics, the recomposition-count harness and budgets), and ${RS}/predictive-back/SPEC.md (constants table with sources, curves, Navigation 3 implementation design with code sketch, how it composes with BackGate and the tab bar, verification).` },
    { key: 'adaptive-standards', brief: `From ${RS}/adaptive.md and ${RS}/standards.md (and ${RS}/CODE-STANDARDS.md) write: ${RS}/ADAPTIVE-PLAN.md (adaptive contract, breakpoints, tokens, per-component behaviour, navigation bar->rail->sidebar, scenes, foldables, desktop windowing, input, manifest, enforcement mechanisms, verification on this machine — emulator wm size/density overrides, AVDs under ~/Android/Sdk/system-images — and owners) and ${RS}/STANDARDS-PLAN.md (the deeper package/folder structure per module type as a concrete tree with the moves needed from the current tree, the code-standards rules to enforce through verifyCodeRules/lint/review, and the vendored Android skills plan per template with the AGENTS.md/CLAUDE.md wording).` },
  ]
  await Promise.all(MAPS.map(m => agent(`${BASE}\n\nYou turn research into per-package plans. In-progress code: navigation/shell/bar/perf harness in ${OV}/wt/WP04/templates/android (handoff ${OV}/handoff/WP04.md); design system and catalog in ${OV}/wt/WP03/templates/android (handoff ${OV}/handoff/WP03.md); features, core and build-logic on the integration branch ${R}/templates/android and in ${OV}/wt/WP02; SwiftUI in ${OV}/wt/WP08; master plan ${DESIGN}/MASTER-PLAN.md (read only the sections you need). Owners: WP02-M2 (build-logic, conventions, verifyCodeRules, generator Android target, presets, modular split), WP02-M3 (feature screens on the new design system), WP03 (design system, components, catalog, tokens), WP04 (navigation, shell, bars, transitions, predictive back, perf harness, baseline profiles), WP05 (glass), WP06 (data, network, devtools, platform services), WP07 (Compose Multiplatform), WP08 (SwiftUI), WP12 (docs).\n\n${m.brief}\n\nThen append to each named owner's handoff note ${OV}/handoff/<WP>.md (create a minimal one if missing; use WP05-preread.md and WP07-preread.md for packages not started) a section "## Orchestrator note: ${m.key}" listing that package's MISSING/PARTIAL items with the concrete change and the plan file to read. Return a summary.`, { label: `plan:${m.key}`, phase: 'Plan', schema: { type: 'object', properties: { summary: { type: 'string' } }, required: ['summary'] } })))
  fire('PLANS')
}).catch(e => log('plans failed: ' + e)).finally(() => endedOf('PLANS').r(true))

// ---------- implementation ----------
const PKGS = {
  'WP02-M2': { wt: 'WP02', file: 'WP02.md', start: ['PLANS'], mode: 'resume', scope: 'Milestone M2 ONLY (the "Milestone M2" section). M1 is merged. A previous M2 run was cut off with uncommitted work in the worktree. Its database scaffolding waits for WP06 (return waiting_for WP06 if you reach it first). Also own the deeper folder structure moves and the code-standards rules for build-logic/verifyCodeRules from research/STANDARDS-PLAN.md, and vendoring the Android skills into the Android template (with the licence) per that plan.' },
  'WP03': { wt: 'WP03', file: 'WP03.md', start: ['PLANS'], mode: 'resume', scope: 'The whole package: phase A is done; the call-site sweep (WP02-M1 is merged now); phase B after CP1; plus every WP03 item in the research plans and qa/r1/for-WP03.md, in particular the new pull to refresh (REQUIREMENTS 29) recorded before and after on the phone.' },
  'WP04': { wt: 'WP04', file: 'WP04.md', start: ['PLANS'], mode: 'resume', scope: 'The whole package: phase 2 is measured (perf/WP04/phase2/summary.md: several solid gates fail); finish phase 2 bookkeeping, then phase 3 (WP01 is merged now). Plus every WP04 item in the research plans and qa/r1/for-WP04.md, in particular the tab-switch animation (REQUIREMENTS 28), the Gmail-style predictive back (24, research/predictive-back/SPEC.md) and the oversized selected pill (30). Report milestone "WP04:phase3" when the gates are run.' },
  'WP06': { wt: 'WP06', file: 'WP06.md', start: ['PLANS'], mode: 'start', scope: 'The whole package, plus WP06 items in the research plans and qa/r1/for-WP06.md (e.g. the DEV badge overlapping the large title).' },
  'WP08': { wt: 'WP08', file: 'WP08.md', start: ['PLANS'], mode: 'resume', scope: 'The whole package, plus WP08 items in ADAPTIVE-PLAN.md, MOTION-PLAN.md and STANDARDS-PLAN.md.' },
  'WP09': { wt: 'WP09', file: 'WP09.md', start: [], mode: 'resume', scope: 'The whole package. WP01 is merged now, so the remaining acceptance steps can run.' },
  'WP10': { wt: 'WP10', file: 'WP10.md', start: [], mode: 'resume', scope: 'The whole package.' },
}
const KNOWN = new Set([...Object.keys(PKGS), 'WP01', 'WP02-M1', 'CP1', 'PLANS'])
const UI_ANDROID = new Set(['WP03', 'WP04', 'WP06', 'WP02-M2'])

const IMPL_SLOTS = 3
let free = IMPL_SLOTS
const queue = []
async function withSlot(fn) {
  if (free > 0) free--
  else await new Promise(res => queue.push(res))
  try { return await fn() } finally { const next = queue.shift(); if (next) next(); else free++ }
}

const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'waiting', 'blocked'] }, waiting_for: { type: 'array', items: { type: 'string' } }, milestones: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' }, accept_ok: { type: 'boolean' } }, required: ['state', 'waiting_for', 'milestones', 'summary', 'accept_ok'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }
const MERGE = { type: 'object', properties: { ok: { type: 'boolean' }, details: { type: 'string' }, needs_package_fix: { type: 'boolean' } }, required: ['ok', 'details', 'needs_package_fix'] }

const ORCH = `
Orchestrator decisions that override MASTER-PLAN where they differ:
- The user asked not to be asked anything; there is no one to ask. Granted: npm ci in web/frontend; Gradle dependency resolution (incl. androidx.room3, io.insert-koin, io.ktor darwin, io.github.kyant0, org.jetbrains.androidx.navigation3, org.jetbrains.compose, Kotlin/Native); Docker pulls of golang:1.27-alpine and python:3.12-alpine for WP09's container check. Not granted: pushing anything, merging to main, other downloads.
- QUALITY BAR (REQUIREMENTS 27): Google/AOSP-level code. Follow ${RS}/CODE-STANDARDS.md and ${RS}/STANDARDS-PLAN.md (deeper package structure) once they exist, the androidx Compose API guidelines, and the relevant Google Android skills at ${SKILLS} (navigation-3, navigation-event, adaptive, theming/styles, edge-to-edge, agp-9-upgrade, r8-analyzer, testing-setup, android-profiler, security). Zero AI-slop comments.
- Checkpoints CP1 (motion feel) and CP2 (glass) are decided by an orchestrator decision agent into ${OV}/decisions/CP1.md / CP2.md. Produce the recordings your WP asks for under ${OV}/perf/<id>/.
- Merged packages are tagged merged/<ID> in ${R}.
- Phone: serial 9a7c74f4 (Redmi Note 10 Pro, 120 Hz). Its hardware encoder now fails at full resolution: always pass --size 720x1600 to screenrecord (record.sh too). If the phone is missing, use emulator-5554 / AVD periflow_api35 for correctness only.`

function implPrompt(id, mode, extra) {
  const p = PKGS[id]
  return `You are the implementation agent for work package ${id} of the android-base overhaul. Mode: ${mode}.
Read before editing: ${OV}/REQUIREMENTS.md (all of it; items 17-30 came from the user during the work); ${OV}/notes/INDEX.md and every plan it lists that names ${id}; ${DESIGN}/wp/${p.file}; the MASTER-PLAN sections it names (${DESIGN}/MASTER-PLAN.md is 218 KB: grep the heading and read a line range; always read "Part D. Rules every implementation agent follows" and A11.3); the audit sections it cites; your handoff ${OV}/handoff/${id}.md (orchestrator notes are appended there).
${mode === 'resume' ? `A previous agent on this package was cut off by a usage limit. Reconstruct where it stopped from the handoff, \`git log --oneline overhaul/multiplatform-v2..HEAD\`, and \`git status\`/\`git diff --stat\`. Uncommitted changes are its work in progress: review them, keep what is right, commit in logical steps, continue.` : mode !== 'start' ? `You are continuing earlier work: read the handoff and \`git log --oneline -30\` first.` : ''}
SCOPE OF THIS RUN: ${p.scope}
Worktree: ${OV}/wt/${p.wt} on branch wp/${p.wt}. If missing: git -C ${R} worktree add ${OV}/wt/${p.wt} -b wp/${p.wt} overhaul/multiplatform-v2 . Work only there. First merge the integration branch into yours (git merge overhaul/multiplatform-v2; owner wins inside your owned paths). Gradle only via ${L} gradle, adb only via ${L} adb, long commands in the background with logs (Part D 14, 14a).
${ORCH}
Currently merged/decided/available: ${JSON.stringify(doneKeys())}.
HABITS: commit after every completed step with a message that explains why; keep a "## Progress" section at the top of ${OV}/handoff/${id}.md current; read build logs with tail/grep; read big files by section.
WAITING: if only work blocked on something not merged/decided remains, commit, update the handoff with exactly where you stopped, and return state="waiting" with waiting_for from ${JSON.stringify([...KNOWN])}. List milestones reached as "<id>:<name>" (e.g. "WP04:phase3"); never list a bare package id as a milestone.
DONE: state="done" only when every acceptance command for this run's scope passes (accept.sh log ends with ACCEPT OK), the handoff is complete (incl. "Measurements" and "Rules and gotchas the docs must state" for WP12), and git status is clean. state="blocked" only for something you cannot resolve soundly, with evidence.
${extra || ''}`
}

async function runImpl(id, mode, extra) {
  const r = await withSlot(() => agent(implPrompt(id, mode, extra), { label: `${id}:${mode}`, phase: 'Implement', schema: STATUS }))
  if (!r) throw new Error(`${id} agent failed (${mode})`)
  for (const m of r.milestones || []) if (m.includes(':')) fire(m)
  return r
}

async function implementUntilDone(id, mode) {
  let r = await runImpl(id, mode)
  let rounds = 0
  while (r.state === 'waiting' && rounds < 8) {
    const unknown = r.waiting_for.filter(k => !KNOWN.has(k))
    if (unknown.length) { log(`${id} waits for ${unknown}, not produced here; stopping it`); return r }
    log(`${id} waiting for ${r.waiting_for.join(', ')}`)
    const w = await waitFor(r.waiting_for)
    if (w !== 'ok') return { ...r, state: 'blocked', summary: `${w} never arrived. ${r.summary}` }
    rounds++
    r = await runImpl(id, 'continue')
  }
  return r
}

async function reviewLoop(id) {
  const p = PKGS[id]
  for (let round = 1; round <= 3; round++) {
    const rv = await agent(`You are a fresh-context code reviewer for package ${id} of the android-base overhaul, holding it to the Google/AOSP bar (REQUIREMENTS 27). Repository ${R}.
Review: git -C ${R} diff overhaul/multiplatform-v2...wp/${p.wt} (start with --stat; read every changed source file; skim emitted files and lockfiles).
Judge against: ${RS}/CODE-STANDARDS.md and ${RS}/STANDARDS-PLAN.md (if present), the androidx Compose API guidelines, ${DESIGN}/MASTER-PLAN.md "Part D" and A11.3, the package file ${DESIGN}/wp/${p.file} (scope: ${p.scope}), ${OV}/REQUIREMENTS.md, and the plans in ${OV}/notes/INDEX.md that name ${id}.
BLOCKING: correctness bugs; perf hazards (animated state read in composition, alpha on full-screen or backdrop layers, per-event coroutine launches, infinite animations without LocalTabVisible/reduce-motion, allocations in draw, recomposition per frame); API-guideline violations in public components (modifier parameter, defaults objects, naming, parameter order, state hoisting); narrating/history comments, AI slop, commented-out code, needless abstraction; dead code; missing or weakened tests; substantive edits outside owned paths; secrets; placeholders presented as finished work; non-adaptive UI where the plans require adaptivity.
NON-BLOCKING: nits. Do not modify anything. Cite file and line.`, { label: `review:${id}#${round}`, phase: 'Review', schema: REVIEW })
    if (!rv) throw new Error(`review of ${id} failed`)
    if (!rv.blocking.length) return { clean: true, notes: rv.nonblocking }
    log(`${id} review #${round}: ${rv.blocking.length} blocking`)
    const fx = await runImpl(id, 'fix', `A fresh-context reviewer found BLOCKING issues. Fix every one on your branch (commits with why-messages), re-run the affected acceptance commands, update the handoff, return state="done":\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking (address cheaply or record): ${JSON.stringify(rv.nonblocking)}`)
    if (fx.state !== 'done') return { clean: false, notes: [`fix run ended ${fx.state}: ${fx.summary}`] }
  }
  return { clean: false, notes: ['review not clean after 3 rounds'] }
}

let mergeChain = Promise.resolve()
function mergePackage(id) {
  const p = PKGS[id]
  const run = async () => {
    for (let attempt = 1; attempt <= 2; attempt++) {
      const m = await agent(`You are the orchestrator's integrator. Merge package ${id} (branch wp/${p.wt}) into overhaul/multiplatform-v2 in ${R} (that checkout is on the integration branch; verify, and that git status is clean).
1. git -C ${R} merge --no-ff wp/${p.wt} with a message saying why the package exists in one or two sentences, ending with: Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>. Conflicts: MASTER-PLAN B.1 (libs.versions.toml and settings.gradle.kts keep both sides' lines in order; emitted files regenerated with --write; owner wins inside owned paths). A substantive conflict: git merge --abort, return ok=false, needs_package_fix=true.
2. Integration checks, Gradle via ${L} gradle in the background with logs under ${OV}/out/integration/${id}/, polled: generator unit tests; python3 -m genkit.tokens --check and python3 -m genkit.core.catalogue --check (in generator/); templates/android ./gradlew assembleDevDebug; if generator/tests/fixtures/v2/modules-compact.json exists and the scaffolder has been rewritten (WP02-M2 merged), generate it and build assembleDevDebug testDevDebugUnitTest; then the "Quick subset" of ${DESIGN}/wp/${p.file}.
3. ${UI_ANDROID.has(id) ? `Perf smoke (A12.8 step 3) if templates/android/tools/perf/perf.sh and ${OV}/perf/WP04/final/summary.json exist (adb via ${L} adb; screenrecord needs --size 720x1600 on this phone).` : 'No perf smoke.'}
4. Green: git -C ${R} tag merged/${id}; ok=true. A failure caused by this merge: fix only if small and mechanical (commit with a why message); else git revert -m 1 --no-edit HEAD and return ok=false, needs_package_fix=true with the failing output.
Append an entry to ${OV}/merges.md.`, { label: `merge:${id}#${attempt}`, phase: 'Merge', schema: MERGE })
      if (!m) throw new Error(`merge of ${id} failed`)
      if (m.ok) { fire(id); return m }
      if (!m.needs_package_fix || attempt === 2) return m
      const fx = await runImpl(id, 'fix', `The orchestrator could not merge your branch: ${m.details}\nMerge overhaul/multiplatform-v2 into your branch, resolve, fix the failing checks, re-run your acceptance, update the handoff, return state="done".`)
      if (fx.state !== 'done') return { ok: false, details: fx.summary, needs_package_fix: true }
    }
  }
  mergeChain = mergeChain.then(run, run)
  return mergeChain
}

async function packageFlow(id) {
  const w = await waitFor(PKGS[id].start)
  if (w !== 'ok') return { id, state: 'blocked', summary: `${w} never arrived` }
  const r = await implementUntilDone(id, PKGS[id].mode)
  if (r.state !== 'done') { log(`${id} ended ${r.state}: ${r.summary.slice(0, 300)}`); return { id, state: r.state, summary: r.summary } }
  const rv = await reviewLoop(id)
  const m = await mergePackage(id)
  return { id, state: m && m.ok ? 'merged' : 'merge-failed', summary: r.summary, review: rv.notes, merge: m && m.details }
}

async function checkpoint1() {
  const w = await waitFor(['PLANS'])
  if (w !== 'ok') return `CP1 not decided: ${w}`
  const d = await agent(`You stand in for the user at checkpoint 1 (motion and press feel, MASTER-PLAN B.5 "User checkpoints"); the user asked not to be asked. Inputs: ${OV}/perf/WP04/cp1/ (phone recordings: push/pop at screen response 0.24/0.30/0.36 s, tab switch, gesture pop, rapid back, bar label modes, bar minimize) with ${OV}/perf/WP04/phase2/summary.md; ${OV}/perf/WP03/cp1*/ and ${OV}/handoff/WP03.md (press feedback); ${RS}/MOTION-PLAN.md and ${RS}/ios-motion.md (iOS parameters); REQUIREMENTS items 19-24 and 28. Open the frame strips. Decide the screen response, press/release/selection springs, bar label mode, the tab-switch effect parameters, and the pop rows if the warm-pop spike fell back. Criteria: iOS feel (no bounce on full-screen moves, UIKit push ~0.35 s), the user's complaints (pushy/glitchy transitions, wants polished tab-switch animation), the smoothness gates, the BackGate hold window. Write ${OV}/decisions/CP1.md with values, reasons, and which recordings to watch to override. Do not modify repositories.`, { label: 'checkpoint:CP1', phase: 'Checkpoint', schema: { type: 'object', properties: { summary: { type: 'string' } }, required: ['summary'] } })
  if (!d) throw new Error('CP1 decision failed')
  fire('CP1')
  return d.summary
}

const flows = Object.keys(PKGS).map(id => packageFlow(id)
  .catch(e => ({ id, state: 'error', summary: String(e) }))
  .then(res => { endedOf(id).r(true); return res }))
const cp1 = checkpoint1().catch(e => `CP1 error: ${e}`).then(res => { endedOf('CP1').r(true); return res })
await plans
const results = await Promise.all(flows)
return { results, cp1: await cp1, signals: doneKeys() }
