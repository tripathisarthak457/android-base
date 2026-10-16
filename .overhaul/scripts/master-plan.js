export const meta = {
  name: 'android-base-master-plan',
  description: 'Architect drafts the overhaul master plan + work packages, two critics review (user lens, engineering/delivery lens), architect revises',
  phases: [
    { title: 'Draft', detail: 'one architect writes MASTER-PLAN.md + wp/*.md' },
    { title: 'Critique', detail: 'two critics with different lenses' },
    { title: 'Revise', detail: 'architect applies the critiques' },
  ],
}

const ROOT = '/home/crazyhusir/Documents/projects/tools'
const OV = ROOT + '/.overhaul'
const DESIGN = OV + '/design'

const CONTEXT = `
You are designing the overhaul of the user's own project "android-base" — an app project GENERATOR (repo ${ROOT}/android-base, branch overhaul/multiplatform-v2). READ FIRST:
- ${OV}/REQUIREMENTS.md  (the user's requirements — the bar every decision is judged against)
- ${OV}/audit/*.md        (13 deep, verified audit reports: generator-python, template-build, template-designsystem, template-navigation, template-features-data, template-infra, catalog-benchmark, web-frontend, web-api, periflow-learnings, vitfleet-learnings, research, device-baseline). They contain concrete root causes, measurements on the user's Redmi Note 10 Pro, and fix sketches. Use them; do not re-audit.
- ${ROOT}/android-base/AGENTS.md, README.md, template/docs/ARCHITECTURE.md, generator/genkit/spec.py (feature catalogue), template/settings.gradle.kts, template/build-logic.
Reference apps generated from it and improved by hand: PeriFlow (~/Documents/projects/jorwen/periflow/periflow-android) and VitFleet (~/Documents/projects/tools/vitfleet/vitfleet-android).
Key facts already established: Kyant backdrop 2.0.1 is Kotlin Multiplatform (Android/iOS/desktop); blur needs API 31+, lens API 33+, below 31 it draws an unblurred copy so a frosted fallback is mandatory. Navigation3 1.2.0 is stable (androidx) and JetBrains navigation3-ui 1.1.2 exists for CMP. AGP 9 requires the Android app to be a separate module from KMP libs (com.android.kotlin.multiplatform.library, kotlin { android {} }). Room 3 (androidx.room3 3.0.3) is stable and KMP. Koin 4.2.2 (+compiler plugin) or Metro 1.4.5 for KMP DI. XcodeGen 2.46.0. User decisions: CMP targets Android+iOS only; Swift = SwiftUI + @Observable MVI, iOS 17 min, XcodeGen, local SPM packages, iOS 26 glass behind #available, hidden as "coming soon" on the site; admin telemetry = richer anonymous only.
Machine constraints for the implementation phase: 15 GB RAM, 8 cores; Gradle builds are serialised machine-wide through ${OV}/bin/locked gradle ...; one adb session at a time through ${OV}/bin/locked adb ...; one Android phone (Redmi Note 10 Pro, 120 Hz) for perf testing; no Mac (Swift and iOS cannot be compiled — only written and statically checked). Implementation agents will work in git worktrees per work package and be merged by the orchestrator, so the plan must give each work package clear directory/file OWNERSHIP to minimise merge conflicts, explicit dependencies, and acceptance criteria.
`

const DECISIONS = `
Your plan MUST decide, concretely (names, directory trees, code sketches where it matters):
 1. Repository layout for three targets (e.g. templates/android, templates/multiplatform, templates/ios) + generator engine layout (target plugins, shared engine: spec v2 with a "platform" field and backward compatibility for existing v1 specs, single-pass rename, marker engine reuse across Kotlin/Swift/XML/YAML, validation, zip, record). Stdlib-only Python stays.
 2. Android module architecture that scales to million-user, feature-heavy apps WITHOUT over-engineering small apps: e.g. compact vs modular (feature:x:api/impl, data:x:api/impl, optional domain), how ONE template supports both (or why not), package naming, ModuleGraph rules, deeper intra-module directory structure (ui/, presentation/, navigation/, data/remote, data/local ...), what lives in core:*. Decide core:common purity (pure Kotlin) so code can be shared with CMP.
 3. Android navigation/shell design that fixes every glitch in the audits: kept tabs v2 (measure only active, LocalTabVisible, per-tab back dispatcher, trim on session reset), BackHold or Navigation3 1.2.0 mid-flight predictive back, rapid-back-press safety (user reported glitches when pressing back quickly during a pop), slide-only push/pop with one critically-damped spring (~350 ms), linear predictive pop, draw-phase scrim instead of alpha, bar behaviour (persistent by default like iOS vs hide-on-push; if hide, it must move with the root screen), content under chrome via insets locals, single host call site, deep links/singleTop, auth hand-off without host swap glitches, sheets as OverlayScene.
 4. Design system: press-feedback taxonomy (None/Scale/Dim/Highlight via Modifier.Node; NO global grey overlay/ripple), haptics policy (iOS-like), motion tokens as (duration,bounce) springs precomputed per style, typography aligned with iOS Dynamic Type, colour/state/elevation tokens, a11y fixes, the component set (which to keep as core, which to move into optional groups, which to delete, which NEW components to add to reach Apple-software polish — e.g. inset-grouped lists, collapsing large-title bar, detent sheet, context menu, search bar, toast/HUD, etc.), floating bottom bar redesign, and the OPTIONAL liquid glass feature (Kyant): GlassStage/SceneBackdrop record-once, chrome-only sampling, drawn content glass, quality tiers (Live / LiveNoLens / Frosted / Solid from API level, low-RAM, power-save, thermal, user "reduce transparency", optional FrameMetrics GPU-time adaptation), lens only while dragging, no alpha over backdrops, cached shadows (Compose 1.12 dropShadow), one LiquidTabs(orientation) for bar+rail. Name the perf budget (e.g. GPU p90 <= 8 ms while scrolling under glass on the Redmi).
 5. Feature catalogue changes: which heavy/unused features to REMOVE or demote (e.g. lottie, widget, workmanager-without-workers, room-as-http-cache, media transcoder, analytics forced by push), how to split firebase (push / crashlytics / analytics / remote-config), which NEW practically useful features to add (e.g. liquid glass, modular architecture option, generated CI workflow for the generated project, perf script + baseline profile journeys, in-app updates..., offline-first sync sample, Room 3, detekt/ktlint/spotless, ...), presets (lean/standard/everything) redesign. Keep the list honest and justified.
 6. MVI/data layer fixes: non-blocking event handling (no suspending serial queue blocking Back/typing), typed AppError, offline-first repository pattern, NetworkMonitor shared, auth token cache cleared on sign-out, etc.
 7. Compose Multiplatform template (Android + iOS): module layout under AGP 9 (shared umbrella, androidApp, iosApp via XcodeGen, feature/data/core KMP modules), DI choice (Koin vs Metro — pick one and justify), navigation (JetBrains navigation3 vs alternatives), networking (Ktor), persistence (Room 3 KMP / DataStore KMP), resources (compose resources), design system shared in commonMain (ported from the overhauled Android design system), glass via Kyant KMP with the same quality tiers, how the generator strips/renames it, what features it supports in v1.
 8. SwiftUI template: package/module layout mirroring the Android graph, @Observable MVI store pattern, NavigationStack routing per tab, design system (tokens shared with Android via a generated token file?), glass on iOS 26 with fallback, networking (URLSession + async/await), persistence (SwiftData? or plain), testing (Swift Testing), XcodeGen project.yml, how the generator renames bundle ids/targets; what features v1 supports.
 9. Shared design tokens: should one tokens JSON (colours/type/spacing/motion springs) drive Compose, CMP and SwiftUI so all three generate the same look? Decide.
10. Web portal + Go API: platform picker (Android | Compose Multiplatform | Swift — coming soon/hidden), configurator refactor (split god components), validation parity with the generator (catalogue-driven limits), previews fidelity, anonymous telemetry schema additions (funnel events, geo region/city via Vercel headers, UA class, UTM, time-in-configurator, returning visitor, per-platform, failure by combination), admin portal metrics/charts/filters/CSV, security fixes (caps on spec size, temp cleanup, IPv6 hashing, etc.), migrations.
11. AI-agent-facing docs: what AGENTS.md / CLAUDE.md / docs/* each generated project and the repo must contain so AI agents build POLISHED, SMOOTH apps (motion & perf rules, component usage rules, checklists, forbidden patterns with code).
12. Verification: generator unit tests, combination matrix (which combos get a real Gradle build — mind the serialised builds; estimate time), CMP Android build, Swift static checks (swift syntax can't compile; maybe validate project.yml schema + grep checks), on-device perf gates (gfxinfo/framestats scripts, macrobenchmark FrameTimingMetric journeys, rapid-back test), CI changes.
13. WORK BREAKDOWN: 8-14 work packages with: id, title, scope, OWNED paths (globs), must-not-touch paths, inputs/dependencies on other packages, deliverables, acceptance criteria (exact commands), whether it needs Gradle/adb, and an ordering/wave plan (which run in parallel worktrees, which must follow). Include the PeriFlow fix package (separate repo, local branch, not pushed).
`


const ECONOMY = `Token economy: the audit reports are long. Read each report's executive summary and findings index first, then only the sections you need. Do not re-read whole files repeatedly.`

phase('Draft')
const draft = await agent(
  `${CONTEXT}\n\n${DECISIONS}\n\n${ECONOMY}\n\nYou are the chief architect. Write the COMPLETE master plan to ${DESIGN}/MASTER-PLAN.md: (A) decisions 1-12, final and concrete (directory trees, names, code sketches where it matters, justified by audit findings), (B) the WORK BREAKDOWN (decision 13) with owned paths, must-not-touch paths, dependencies, deliverables and exact acceptance commands per package, plus a wave plan that respects serialised Gradle, (C) a risk register, (D) "rules every implementation agent follows" (comment why not what; no narrating comments; no AI slop; match surrounding code; perf rules; use the gradle/adb lock wrappers; never weaken tests; judge smoothness only on release/benchmark builds). Then write one self-contained file per work package at ${DESIGN}/wp/<id>.md with everything an implementation agent needs (context, scope, owned paths, step-by-step, acceptance). Prefer fewer, larger, cleanly separated packages over many entangled ones. Do not modify any repository.`,
  { label: 'draft:master-plan', phase: 'Draft', schema: { type: 'object', properties: { summary: { type: 'string' }, packages: { type: 'array', items: { type: 'string' } } }, required: ['summary', 'packages'] } })
if (!draft) throw new Error('draft failed')

phase('Critique')
const CRITIC_SCHEMA = { type: 'object', properties: { verdict: { type: 'string' }, issues: { type: 'array', items: { type: 'object', properties: { severity: { type: 'string', enum: ['blocker', 'major', 'minor'] }, where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['severity', 'where', 'problem', 'fix'] } }, missing: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'issues', 'missing'] }
const LENSES2 = [
  'CRITIC LENS — THE USER: does the plan deliver smooth (no glitchy tab/push/back transitions, including rapid back presses), light, Apple-polished apps, no ripple, optional fast liquid glass, more/better components and floating bars, deeper scalable architecture, CMP + Swift generators, richer admin metrics, great AI-agent docs, and every other line of REQUIREMENTS.md? Flag anything vague, missing, or likely to look like AI slop.',
  'CRITIC LENS — PRINCIPAL ENGINEER + DELIVERY LEAD: technical correctness against the audits and library facts (Navigation3 1.2.0, Compose 1.12, Kyant backdrop, AGP 9 KMP rules, Room 3, Koin/Metro, XcodeGen), migration risk (e.g. moving template/ to templates/android, spec back-compat, website/API/CI/Docker/Vercel path updates), merge-conflict risk between parallel packages (shared files like libs.versions.toml, spec.py, settings.gradle.kts, catalogue, translations), serialised-build feasibility, and whether every acceptance criterion is an exact runnable check.',
]
const critiques = await parallel(LENSES2.map((lens, i) => () => agent(
  `${CONTEXT}\n\n${lens}\n\n${ECONOMY}\n\nReview ${DESIGN}/MASTER-PLAN.md and the files in ${DESIGN}/wp/. Be concrete and adversarial; only raise issues that matter. Do not modify files.`,
  { label: `critic:${i + 1}`, phase: 'Critique', schema: CRITIC_SCHEMA })))

phase('Revise')
const final = await agent(
  `${CONTEXT}\n\n${ECONOMY}\n\nYou wrote ${DESIGN}/MASTER-PLAN.md and ${DESIGN}/wp/*.md. Two critics reviewed them:\n${JSON.stringify(critiques.filter(Boolean))}\n\nApply every blocker and major issue (and minor ones where cheap); fill the missing items; keep the plan coherent. Update the files in place. Then return the final wave plan and package list.`,
  { label: 'revise:master-plan', phase: 'Revise', schema: {
    type: 'object',
    properties: {
      summary: { type: 'string' },
      wave_plan: { type: 'array', items: { type: 'object', properties: { wave: { type: 'integer' }, packages: { type: 'array', items: { type: 'string' } }, note: { type: 'string' } }, required: ['wave', 'packages'] } },
      packages: { type: 'array', items: { type: 'object', properties: { id: { type: 'string' }, title: { type: 'string' }, file: { type: 'string' }, needs_gradle: { type: 'boolean' }, needs_adb: { type: 'boolean' }, depends_on: { type: 'array', items: { type: 'string' } }, owned_paths: { type: 'array', items: { type: 'string' } } }, required: ['id', 'title', 'file', 'needs_gradle', 'needs_adb', 'depends_on', 'owned_paths'] } },
    },
    required: ['summary', 'wave_plan', 'packages'],
  } })
return { draft: draft.summary, critiques: critiques.filter(Boolean).map(c => ({ verdict: c.verdict, n: c.issues.length })), final }
