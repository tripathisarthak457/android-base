export const meta = {
  name: 'bottom-sheet-design-from-judge',
  description: 'Design a highly customisable, modern bottom sheet for the android-base templates: map what exists, research the best sheets, three competing specs, judges, synthesis, adversarial critique, final spec',
  phases: [
    { title: 'Understand', detail: 'current sheet code and plans; best-in-class research' },
    { title: 'Design', detail: 'three independent specs from different angles' },
    { title: 'Judge', detail: 'score the specs and pick grafts' },
    { title: 'Synthesize', detail: 'one authoritative SHEET-SPEC.md' },
    { title: 'Critique', detail: 'adversarial perf/correctness and API/a11y/adaptive critics, then revision' },
  ],
}

const R = '/home/crazyhusir/Documents/projects/tools/android-base'
const OV = '/home/crazyhusir/Documents/projects/tools/.overhaul'
const RS = OV + '/research'
const OUT = RS + '/sheet'
const SKILLS = '/home/crazyhusir/.claude/plugins/marketplaces/android-skills'

const CONTEXT = `Project: the user's app project generator "android-base" (${R}, integration branch overhaul/multiplatform-v2), being overhauled. It generates an Android app (Jetpack Compose, Compose UI 1.12 / BOM 2026.09, Navigation 3 1.2.0, Kotlin 2.4, AGP 9, minSdk 26) with its OWN foundation-only design system (no material3 components; androidx.compose.material3.adaptive is allowed), an iOS-level motion system (springs from response+bounce; tokens in templates/android/core/designsystem tokens/motion.json; decisions in ${OV}/decisions/CP1.md), optional liquid glass (Kyant backdrop, WP05), a Compose Multiplatform template (WP07) and a SwiftUI template (WP08, iOS 17+, @Observable MVI). Packages are being built in worktrees ${OV}/wt/<WP>; read them, never edit them. Read ${OV}/REQUIREMENTS.md first (the user's requirements: iOS-level smoothness and lowest jank, iOS-like optimised motion, recomposition discipline, adaptive UI for every window size, Gmail-style predictive back, no ripple, Google/AOSP code quality, no AI slop).
The user has now asked: "add a highly customisable and modern designed bottom sheet component in the generator". Today is 2026-10-13. Reference phone: Redmi Note 10 Pro, 120 Hz.
Rules for you: do not modify any repository or worktree, do not run Gradle or adb. Write only under ${OUT}/ (create it). Cite file:line for code facts and URLs for external facts; mark anything unverified.`

const MAP = { type: 'object', properties: { file: { type: 'string' }, summary: { type: 'string' } }, required: ['file', 'summary'] }
const SCORE = { type: 'object', properties: { scores: { type: 'array', items: { type: 'object', properties: { spec: { type: 'string' }, feel: { type: 'number' }, customisability: { type: 'number' }, api_quality: { type: 'number' }, performance: { type: 'number' }, robustness: { type: 'number' }, adaptive_a11y: { type: 'number' }, fit_with_architecture: { type: 'number' }, parity: { type: 'number' }, total: { type: 'number' }, notes: { type: 'string' } }, required: ['spec', 'total', 'notes'] } }, winner: { type: 'string' }, grafts: { type: 'array', items: { type: 'string' } }, rejects: { type: 'array', items: { type: 'string' } } }, required: ['scores', 'winner', 'grafts', 'rejects'] }
const CRIT = { type: 'object', properties: { issues: { type: 'array', items: { type: 'object', properties: { section: { type: 'string' }, severity: { type: 'string', enum: ['blocking', 'major', 'minor'] }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['section', 'severity', 'problem', 'fix'] } } }, required: ['issues'] }

// The map, the research and the three designs were written on Oct 12 (files complete on disk).
const current = { file: OUT + '/current-state.md' }
const research = { file: OUT + '/research.md' }
const designs = ['feel', 'api', 'perf'].map(k => ({ file: `${OUT}/design-${k}.md` }))
const specs = designs.filter(Boolean)
if (specs.length < 2) throw new Error('not enough designs')

phase('Judge')
const JUDGES = [
  'an Apple Human Interface designer who has shipped iOS 26 sheets and judges feel, look and motion above all',
  'an androidx Compose framework engineer who judges API quality, performance (recomposition, phases, jank) and robustness',
  'a staff engineer running a million-user app on this template who judges customisability, adaptive behaviour, accessibility, cross-platform parity and how cheaply teams can adopt and maintain it',
]
const verdicts = await parallel(JUDGES.map((j, i) => () => agent(`${CONTEXT}

You are ${j}. Read these competing specs fully: ${specs.map(s => s.file).join(', ')}; and the inputs ${current.file}, ${research.file}.
Score each spec 1-10 on feel, customisability, api_quality, performance, robustness, adaptive_a11y, fit_with_architecture, parity, and a weighted total (your weighting, stated in notes). Pick a winner and list concrete grafts (specific sections or decisions from the other specs that should be merged into the winner) and rejects (things any spec proposes that must NOT be built, with why). Write your verdict to ${OUT}/judge-${i + 1}.md too.`, { label: `judge:${i + 1}`, phase: 'Judge', schema: SCORE })))
const vs = verdicts.filter(Boolean)
const tally = {}
for (const v of vs) for (const s of v.scores) tally[s.spec] = (tally[s.spec] || 0) + (s.total || 0)
log(`judge totals: ${JSON.stringify(tally)}; winners: ${vs.map(v => v.winner).join(', ')}`)

phase('Synthesize')
const draft = await agent(`${CONTEXT}

Inputs: the specs ${specs.map(s => s.file).join(', ')}; the judges' verdicts ${vs.map((_, i) => `${OUT}/judge-${i + 1}.md`).join(', ')} (totals ${JSON.stringify(tally)}; their grafts and rejects: ${JSON.stringify(vs.map(v => ({ winner: v.winner, grafts: v.grafts, rejects: v.rejects })))}); ${current.file}; ${research.file}.
TASK: write the single authoritative spec ${RS}/SHEET-SPEC.md: start from the strongest spec, graft what the judges asked for, drop the rejects. It is what implementation agents build from and reviewers check against, so it must be precise and buildable: API signatures, behaviour rules, every motion constant as a token with its value, visuals, adaptive rules, accessibility, integration seams, file layout, migration of AppSheet/AppBottomSheet/AppActionSheet, the catalog demo list, the tests and recomposition budgets, the phone gates with thresholds, and a "Work split" section with one subsection per package (WP03 component, WP04 presenter/navigation/predictive back, WP05 glass style, WP07-B multiplatform parity, WP08 SwiftUI parity, QA2 phone verification, WP12 docs) listing exactly what each must deliver and its acceptance. Plain, precise writing; no filler. Return the path and a 15-line summary.`, { label: 'synthesize:spec', phase: 'Synthesize', schema: MAP })
if (!draft) throw new Error('synthesis failed')

phase('Critique')
const CRITICS = [
  'PERFORMANCE AND COMPOSE CORRECTNESS: find anything that would recompose per frame, read animated state in composition, put alpha on a full-screen or backdrop layer, allocate in draw, create a window/popup on the open path, break nested scroll or IME, mishandle predictive back/BackGate, leak state across process death, or conflict with the presentation host layering contract, kept tabs or glass tiers. Check every constant against the research.',
  'API, ACCESSIBILITY, ADAPTIVE AND COMPLETENESS: check the API against the androidx Compose component API guidelines and our CODE-STANDARDS; find missing customisation a real app needs (and over-engineering it does not), TalkBack/semantics gaps, window-size-class and foldable/ChromeOS gaps, SwiftUI/Multiplatform parity gaps, untestable acceptance, and anything ambiguous an implementer could get wrong.',
]
const critiques = await parallel(CRITICS.map((c, i) => () => agent(`${CONTEXT}

Adversarially review ${draft.file} (and the inputs it was built from in ${OUT}/). Lens: ${c}
Default to raising an issue when in doubt; every issue needs a concrete fix. Write them to ${OUT}/critique-${i + 1}.md too.`, { label: `critique:${i + 1}`, phase: 'Critique', schema: CRIT })))
const issues = critiques.filter(Boolean).flatMap(c => c.issues)
log(`critique: ${issues.length} issues (${issues.filter(i => i.severity === 'blocking').length} blocking)`)
const final = await agent(`${CONTEXT}

Revise ${draft.file} in place to resolve every issue below (blocking and major must be resolved; minor ones resolved or explicitly rejected with a reason in a short "Decisions" section at the end). Keep the spec precise and buildable. Return the path and a 15-line summary of the final design (API shape, detents, styles, motion, adaptive, a11y, work split).
Issues: ${JSON.stringify(issues, null, 1)}`, { label: 'revise:spec', phase: 'Critique', schema: MAP })
return { spec: final ? final.file : draft.file, summary: final ? final.summary : draft.summary, tally, issues: issues.length }
