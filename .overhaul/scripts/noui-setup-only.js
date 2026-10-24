export const meta = {
  name: 'noui-setup-only',
  description: "REQUIREMENTS 59 tonight: the No UI components option (generator, Android template, website, API, docs and vendored Android skills), reviewed and gated for a setup-only production release tomorrow morning; never pushes, never touches main",
  phases: [
    { title: 'Architecture', detail: 'two Opus reviews: the architecture against the best of the industry, and the guardrails for the user\'s own components' },
    { title: 'Plan', detail: 'one file-level design with disjoint lanes' },
    { title: 'Build', detail: 'generator and template, website and API, docs and skills, in parallel worktrees' },
    { title: 'Integrate', detail: 'the three lanes merged into staging-wp/NOUI, emitted files regenerated' },
    { title: 'Review', detail: 'fresh Opus Google/AOSP reviews with fix rounds' },
    { title: 'Gate', detail: 'desktop matrix and phone smoke, Sonnet operators with Opus verifiers' },
    { title: 'Release', detail: 'the setup-only release candidate and the deploy checklist for the user' },
  ],
}

// The user, Oct 24 20:5x-21:1x (REQUIREMENTS 59, decided in three rounds of questions): a "No UI components" checkbox,
// built tonight and released from staging tomorrow morning in setup-only mode. Opus writes, reviews and judges; Sonnet
// only runs the gate's commands and returns raw facts, each followed by an Opus verifier (global CLAUDE.md section 10).
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const BASE = '8fdcba741'
const PLAN_MD = `${OV}/plans/2026-10-24/noui.md`
const HO = `${OV}/handoff/NOUI.md`
const WT = { GEN: `${OV}/wt/NOUI-GEN`, WEB: `${OV}/wt/NOUI-WEB`, DOCS: `${OV}/wt/NOUI-DOCS`, INT: `${OV}/wt/NOUI` }

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md section 6 first if you have not; ${OV}/REQUIREMENTS.md item 59 is the user's spec for this work and wins over anything older. Gradle only through ${L} gradle, adb only through ${L} adb (take adb before gradle when a command needs both), heavy Python and npm through ${OV}/bin/memwait; long commands in the background with logs under ${OV}/logs/NOUI/, polled with tail; never pass -Dorg.gradle.jvmargs or kotlin.daemon.jvmargs. Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md, the template's AGENTS.md: no Material, everything through AppTheme, strings in resources, the module dependency rules); never weaken, skip or delete a test to get to green; emitted files (web/frontend/lib/catalogue.json, the token copies) are regenerated with the generator's own --write, never hand-edited. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never rewrite history, never stash. PeriFlow never. No private names (periflow, vitfleet, jorwen) anywhere. Phone 9a7c74f4: screenshots to /data/local/tmp, never /sdcard; restore any setting you change. Staging's own fix run works in ${OV}/wt/STAGING at the same time: never touch that worktree.`

const LANE = { type: 'object', properties: {
  key: { type: 'string', enum: ['GEN', 'WEB', 'DOCS'] },
  owns: { type: 'array', items: { type: 'string' } },
  tasks: { type: 'array', items: { type: 'string' } },
  acceptance: { type: 'array', items: { type: 'string' } },
}, required: ['key', 'owns', 'tasks', 'acceptance'] }
const PLAN = { type: 'object', properties: {
  plan_path: { type: 'string' },
  summary: { type: 'string' },
  lanes: { type: 'array', items: LANE },
  contract: { type: 'string' },
  release_switch: { type: 'string' },
  gate_desktop: { type: 'array', items: { type: 'string' } },
  gate_phone: { type: 'array', items: { type: 'string' } },
  user_steps: { type: 'array', items: { type: 'string' } },
  risks: { type: 'array', items: { type: 'string' } },
}, required: ['plan_path', 'summary', 'lanes', 'contract', 'release_switch', 'gate_desktop', 'gate_phone', 'user_steps', 'risks'] }
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
const RC = { type: 'object', properties: {
  state: { type: 'string', enum: ['ready', 'not-ready'] },
  branch: { type: 'string' },
  head: { type: 'string' },
  checklist_path: { type: 'string' },
  notes_path: { type: 'string' },
  summary: { type: 'string' },
  open: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'branch', 'head', 'checklist_path', 'notes_path', 'summary', 'open'] }

phase('Architecture')
// The user, 21:2x Oct 24: "review the non-UI architecture again once. Make sure it is the best in the industry, like the
// ultimate best for modern day scalable, maintainable and multi-module apps. Still, it should enforce the user to make good
// stateless modular UI components later if they add their design system, and also enforce the greatest architecture."
const FINDINGS = { type: 'object', properties: {
  verdict: { type: 'string' },
  findings: { type: 'array', items: { type: 'object', properties: {
    title: { type: 'string' }, today: { type: 'string' }, best: { type: 'string' }, why: { type: 'string' },
    change: { type: 'string' }, size: { type: 'string', enum: ['tonight', 'later'] }, evidence: { type: 'string' },
  }, required: ['title', 'today', 'best', 'why', 'change', 'size', 'evidence'] } },
  report_path: { type: 'string' },
}, required: ['verdict', 'findings', 'report_path'] }
const ARCH_LENSES = [
  { key: 'architecture', text: `the architecture a No-UI project keeps (REQUIREMENTS 59: everything non-UI) judged against the best of the industry today for scalable, maintainable, multi-module Android apps: the module graph and its layers (app, core, data, feature api/impl), the dependency rules and how the build enforces them, the MVI/UDF pattern (state, events, effects, process death, SavedStateHandle), DI, Navigation 3 routes owned by feature api modules, data layer (repositories, offline-first, Room, DataStore, Ktor, error model, dispatchers injected), convention plugins and the version catalog, build speed (configuration cache, non-transitive R, build-logic isolation), testing strategy (fakes over mocks, test fixtures modules, architecture tests), R8 and baseline profiles, security defaults, and readiness for Compose Multiplatform later. Compare with Google's official app architecture guidance, Now in Android, and the official Android skills under C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills (read the relevant SKILL.md files). For each gap: what we do today (file:line), what the best practice is, why it matters, the concrete change, and whether it fits tonight (small, safe, testable) or later` },
  { key: 'guardrails', text: `the guardrails that must make a user who adds their own design system to a No-UI project build GOOD components: stateless (state hoisted, values and lambdas in, no ViewModel or repository access, no side effects except through hoisted callbacks), modular (components only in core:designsystem, features compose them but never define reusable ones, designsystem depends on nothing but Compose foundation and its own tokens), API-guideline-correct (the androidx Compose API guidelines: Modifier as the first optional parameter applied to the root, slot APIs, defaults objects, @Stable/@Immutable parameters, no MutableState parameters, content descriptions), themed (every color, type, shape, spacing and motion value from AppTheme's tokens, never hard-coded), and fast (no per-frame state reads in composition, deferred reads through lambdas, no offscreen layers without a written reason, stable keys and content types in lazy lists, previews and tests). For each rule: is it enforced today, and how could it be (the template's build guards in build-logic .../guard/CodeRules.kt and friends, lint or detekt checks, Compose compiler stability reports, architecture tests, a component template the docs point to, the docs in CLAUDE.md and docs/DESIGN-SYSTEM.md); prefer machine-enforced rules that the existing UI design system already passes, so the rule can apply to every project; mark which fit tonight and which later` },
]
// A relaunch after a usage pause passes the finished architecture reviews and plan back in through args (copied from
// the stopped run's journal), because a resume by run id has missed this tool's cache before and would redo them.
const RESUMED = args && args.plan ? args : null
if (RESUMED) log('relaunched with the earlier architecture reviews and plan')
const arch = RESUMED ? RESUMED.arch : (await parallel(ARCH_LENSES.map(a => () => agent(`You are a principal Android architect doing a fresh, independent review (REQUIREMENTS 59; the user wants "the ultimate best for modern day scalable, maintainable and multi-module apps"). ${RULES} Do not modify code. Base: staging at ${BASE}, read it in ${WT.INT}. Your lens: ${a.text}. Be concrete and evidence-based (cite files, guidelines, skills); say plainly where today's design is already the best and why, so nothing good is churned. Write your report to ${OV}/plans/2026-10-24/noui-arch-${a.key}.md and return the findings.`, { label: `noui:arch-${a.key}`, phase: 'Architecture', schema: FINDINGS })))).filter(Boolean)
log(`architecture review: ${arch.map(a => `${a.findings.length} findings (${a.findings.filter(f => f.size === 'tonight').length} tonight)`).join(', ')}`)

phase('Plan')
const plan = RESUMED ? RESUMED.plan : await agent(`You are the Opus architect for REQUIREMENTS 59 (read it in full in ${OV}/REQUIREMENTS.md: every sub-point (a) to (m) is the user's decision; do not re-open any; (m) says the option must not compromise the generator's quality in any way). ${RULES} Do not modify code.
Two independent architecture reviews ran first (the user asked for the best architecture in the industry and guardrails that force good stateless, modular components later): ${JSON.stringify(arch)} (reports in ${OV}/plans/2026-10-24/noui-arch-*.md). Adopt every "tonight" finding that is sound into the lanes below (with its test or guard), unless it endangers the morning deadline or the byte-identical promise; list each "later" finding and each one you defer, with the reason, under "Architecture follow-ups" in ${PLAN_MD}. Guardrails that the existing UI design system already passes apply to every project; others apply to No-UI projects only until the UI catches up.
The base is staging at ${BASE} (read-only for you: read it in ${WT.INT}, a fresh worktree on staging-wp/NOUI at that commit). Read how the generator really works before designing: generator/ (create_project.py, add_feature.py, genkit/core: spec, catalogue, regions, emission, removed.toml handling), templates/android/template.toml, presets.toml, features/*.toml (group, owns, requires), the Design group (catalog, collections, datadisplay, formfields, pickers, liquidglass), core/designsystem (theme, foundation, component, animation, icon, preview), core/ui, core/navigation (AppShell, shell/, stack/, back/, presentation/), app/, feature/* screens, benchmark/ and tools/perf/, the build guards (build-logic .../guard/CodeRules.kt and friends), the generator tests and the consistency matrix, web/frontend (configurator steps PlatformStep, IdentityStep, LookStep, MotionStep, FeaturesStep, BuildStep, ReviewStep; lib/catalogue.json), the Go API under web/api (how a generation request is validated and run), .github/workflows/ci.yml (its deploy job), vercel.json, web/README.md (the deploy settings), and the template's CLAUDE.md, AGENTS.md and docs/.
Design the whole change at file level and write it to ${PLAN_MD}:
1. The option: its spec field and CLI flag, its catalogue metadata (which features it disables and the one-line reason shown for each), how it combines with every preset, and the guarantee that with the box unticked every generated file is byte-identical to ${BASE}'s output EXCEPT the paths that (g) and (h) add or change for every project (the vendored skills, CLAUDE.md, AGENTS.md, docs/): name those paths exactly, so the gate can diff everything else.
2. What a No-UI project contains, module by module, per (b), (c), (d), (f), (i): exactly which files are emitted, replaced by a skeleton, or dropped; the theme skeleton's files; the blank start screen; the placeholder route of each screen feature (text from resources); core:ui's non-visual part; the navigation core without the shell; the benchmark and baseline profile that remain; which tests go with what they test and which stay; how the build guards and the prod-absence proof of the overlay keep working. Prefer the generator's existing mechanisms (regions, owns, variants) over new ones.
3. The release switch for (k): how production generates only No-UI projects until the Android release (a committed setting the API enforces and the site reads, so a request asking for UI components is refused with a clear message, and the site shows the UI options as coming with the Android release), off on staging-wp/NOUI and on only on the release branch; how the Android release turns it off.
4. Docs and skills for (g) and (h): the CLAUDE.md layout with its @-imports, the three docs' outlines (PERFORMANCE.md must hold everything this project has learned about jank-free Compose on a budget 120 Hz phone: gather it from ${OV}/docs-draft/, ${OV}/research/CODE-STANDARDS.md, the branches staging-wp/WP12-DOCS and staging-wp/WP12-KNOW (git show), ${OV}/staging/TEST-REPORT.md and MERGE-NOTES.md, ${OV}/bugs/, the handoffs of WP03, WP04 and HUD, REQUIREMENTS 41, 42, 58, and Google's jetpack-compose-performance skill; generic for a generated project, no overhaul internals or package names), how the UI and No-UI variants of each doc differ, the doc test; and the skills: the source (github.com/android/skills, its latest commit or tag checked with git ls-remote), its licence and whether it allows redistribution (if not, fall back to the install note and say so), which skills go in (the full official set unless you find a strong reason), where (.claude/skills and .agents/skills, unless their size makes one copy plus a pointer better: measure and decide), the refresh command, and how they are emitted for every project.
5. Three lanes with DISJOINT file ownership, so three implementers can work in parallel worktrees and merge cleanly: GEN (generator and the Android template's code, tests and catalogue generation, in ${WT.GEN}), WEB (website and Go API, in ${WT.WEB}; it consumes the catalogue fields GEN defines, as written in "contract", and never hand-edits catalogue.json), DOCS (the template's CLAUDE.md, AGENTS.md, docs/ and the vendored skills, in ${WT.DOCS}; the region or variant markers it needs are defined by you in the plan so GEN's emission honours them). Each lane: owns (paths), tasks, acceptance (the exact commands that prove it).
6. The gate: the desktop rows (generator suite, catalogue and tokens checks, the consistency matrix quick AND full including No-UI combinations, add_feature and remove_feature round trips on a generated No-UI project (each result built), a spec file without the new field generating exactly as before, generation time before and after, every preset with the box ticked generated and built with check assembleDevDebug assembleProdRelease, the byte-identical diff with the box unticked, the website build and tests, the API's vet, build and tests including the switch's enforcement) and the phone rows (a No-UI release-like build of the everything preset: install, cold start, the start screen, a deep link to a placeholder route, process-death restore, the overlay absent from prod, the crash log).
7. user_steps: exactly what the user must do before and at the deploy (the Vercel environment settings by name, the /admin check where they type the token, their go in chat), from web/README.md and ci.yml.
8. risks for the morning deadline, honestly.
Return the plan.`, { label: 'noui:plan', phase: 'Plan', schema: PLAN })
if (!plan) throw new Error('plan failed')
log(`plan: ${plan.lanes.map(l => `${l.key} ${l.tasks.length} tasks`).join(', ')}`)

phase('Build')
const laneText = (k) => JSON.stringify(plan.lanes.find(l => l.key === k))
const implPrompt = (k, extra) => `You implement lane ${k} of REQUIREMENTS 59 in ${WT[k]} (branch staging-wp/NOUI-${k}, cut from staging ${BASE}). ${RULES}
Read ${PLAN_MD} in full first (it is the design; follow it, and where reality differs, do the sound thing and write why in ${HO} under "## ${k}"). Your lane: ${laneText(k)}. The cross-lane contract: ${plan.contract}. Touch only the paths your lane owns; if you must touch another lane's path, stop and write it in ${HO} for the integrator instead.
Work test-first where it is code: a failing test, then the change, then green. Commit in focused commits as you go (a usage pause can stop you at any moment; the next run continues from your commits and ${HO}). If you find uncommitted changes in ${WT[k]} when you start, they are an earlier run of this lane stopped by a usage pause: read them against ${HO}, keep and finish what is sound (with its test), and say in ${HO} what you kept or reverted and why (save anything you revert as a patch under ${OV}/out/NOUI/ first; never stash, reset or clean). Run every acceptance command of your lane through the locks and keep the logs under ${OV}/logs/NOUI/${k}/.${extra}
Return the state, your head, the commits, each check with its log, and what is still open.`
const lanes = RESUMED && RESUMED.built ? [RESUMED.built.GEN, RESUMED.built.WEB, RESUMED.built.DOCS] : await parallel([
  () => agent(implPrompt('GEN', ' The generator change must keep every existing generator test green and add tests for: the flag and the spec field, the disabled features and their reasons, every preset with the box ticked (the emitted file list), the theme skeleton, the placeholder routes, and the byte-identical output with the box unticked (outside the paths the plan names), a spec file without the new field generating exactly as before, and add_feature and remove_feature on a No-UI project (REQUIREMENTS 59 (m): the quality of the generator must not drop in any way; build it into the existing mechanisms, no scattered special cases).'), { label: 'noui:build-GEN', phase: 'Build', schema: BUILT }),
  () => agent(implPrompt('WEB', ' The website keeps working for every preset; add tests for the checkbox, the greyed features with their reasons, the Look and Motion behaviour (e), the Review step text, the multiplatform note (j), and the release switch (k) both in the site and enforced in the Go API (a request asking for UI components while the switch is on is refused with a clear message; a test proves it).'), { label: 'noui:build-WEB', phase: 'Build', schema: BUILT }),
  () => agent(implPrompt('DOCS', ` The vendored skills: check the latest release of github.com/android/skills with git ls-remote, then clone it shallow into %TEMP%\\askills (a short path; the user approved fetching the latest Android skills in chat), read its LICENSE, copy the files from git objects (git archive, so they keep LF line endings; this machine's checkouts are CRLF), and send %TEMP%\\askills to the Recycle Bin afterwards (PowerShell: Microsoft.VisualBasic.FileIO.FileSystem DeleteDirectory with SendToRecycleBin). Never run anything from the download. The docs must be true to the code at ${BASE} and to what a No-UI project contains per the plan; every performance claim names its reason and, where this project measured it, the number and how to measure it again.`), { label: 'noui:build-DOCS', phase: 'Build', schema: BUILT }),
])
const built = { GEN: lanes[0], WEB: lanes[1], DOCS: lanes[2] }
log(`build: ${Object.entries(built).map(([k, v]) => `${k} ${v ? v.state : 'failed'}`).join(', ')}`)

phase('Integrate')
const integrated = await agent(`You integrate REQUIREMENTS 59's three lanes in ${WT.INT} (branch staging-wp/NOUI at ${BASE}). ${RULES}
Read ${PLAN_MD} and ${HO}. The lanes' results: ${JSON.stringify(built)}. An earlier integration run may have been stopped by a usage pause: check git log and git status in ${WT.INT} first and continue from where it stopped (a lane already merged is not merged again; a merge in progress is finished or, if unsound, aborted with git merge --abort and redone). Merge staging-wp/NOUI-GEN, then staging-wp/NOUI-DOCS, then staging-wp/NOUI-WEB with git merge --no-ff, resolving each conflict by reading both sides and the plan; then regenerate every emitted file with the generator's --write (catalogue.json, the token copies) and commit that separately. Finish what a lane left open where it needs the other lanes (the cross-lane items in ${HO}), in focused commits. Then run, through the locks: the generator suite, catalogue --check and tokens --check, one No-UI generation of the everything preset built with check assembleDevDebug, and the website build. Write what you did under "## Integration" in ${HO}. Return the state, head, commits, checks with logs and what is open.`, { label: 'noui:integrate', phase: 'Integrate', schema: BUILT })
if (!integrated) throw new Error('integration failed')

phase('Review')
const AREAS = [
  { key: 'template', text: 'the generator and the Android template in No-UI mode: does a No-UI project contain exactly what REQUIREMENTS 59 (b), (c), (d), (f), (i) say, with every golden-standard rule still enforced (no Material, AppTheme only, strings in resources, module rules, stability, lint), the architecture and guardrail changes the plan adopted from the two architecture reviews (${OV}/plans/2026-10-24/noui-arch-*.md) done right and machine-enforced where the plan says (a guard that a stateful or hard-coded component would trip, proven by a test), no dead code or orphaned resources left by what was removed, tests that test what remains, the byte-identical promise with the box unticked, and the docs (CLAUDE.md, AGENTS.md, docs/ARCHITECTURE.md, PERFORMANCE.md, DESIGN-SYSTEM.md): true to the generated code, detailed, correct about Compose performance (check each claim against Google\'s jetpack-compose-performance skill and the measured numbers it cites), no internal or private names; and the vendored skills: licence and attribution present, pinned, LF line endings, the refresh command works as written' },
  { key: 'web-api', text: 'the website and the Go API: the checkbox, the greyed features and their reasons, Look and Motion (e), the Review step, the multiplatform note (j), and above all the release switch (k): production must be unable to generate a project with UI components while the switch is on, enforced in the API (not only hidden in the site), with a test; input validation, error messages, no new attack surface, the admin routes unchanged, the deploy path in ci.yml unchanged except where the plan says' },
]
AREAS.push({ key: 'generator', text: 'the generator\'s own quality under REQUIREMENTS 59 (m): the option built into the existing mechanisms (catalogue, regions, owns, variants) with no special cases scattered through emission; the Python code at the generator\'s standard (design, naming, no duplication, typed and documented where the rest is, error messages and CLI help a user understands); spec files without the new field generating exactly as before; add_feature and remove_feature on a No-UI project (run them yourself on a generated project and build the result); the consistency matrix and the generator tests covering the No-UI combinations; a No-UI project meeting every quality bar a UI project meets except those that measure UI; generation no slower' })
const reviewOnce = (round) => parallel(AREAS.map(a => () => agent(`You are a separate Google/AOSP-level reviewer of staging-wp/NOUI in ${WT.INT} (REQUIREMENTS 59), area: ${a.text}. ${RULES} Do not modify anything. Read ${PLAN_MD} and ${HO}; review git diff ${BASE}..staging-wp/NOUI in your area (start with --stat, read every changed source and test file), and generate a No-UI project yourself with the CLI into ${OV}/out/NOUI/review-${a.key}-${round}/ to read what users get. Blocking: correctness bugs, a broken or missing guarantee of REQUIREMENTS 59, weakened or missing tests, perf hazards in what remains, untrue docs, security holes, private names. There is no pressure to find something.${round > 1 ? ' This is a re-review after fixes: check the earlier blocking findings are fixed and report only what remains or is new.' : ''}`, { label: `noui:review-${a.key}#${round}`, phase: 'Review', schema: REVIEW })))
let reviews = (await reviewOnce(1)).filter(Boolean)
let blocking = reviews.flatMap(r => r.blocking)
for (let round = 1; round <= 2 && blocking.length; round++) {
  log(`fix round ${round}: ${blocking.length} blocking`)
  const fx = await agent(`You fix staging-wp/NOUI in ${WT.INT} (REQUIREMENTS 59). ${RULES} Read ${PLAN_MD} and ${HO}. Blocking review findings: ${JSON.stringify(blocking)}. Fix each at its root (or, if one is wrong, show why in ${HO} with evidence), one commit per cause, with a test where the cause is code; re-run exactly the checks each fix touches, through the locks. Write what you did under "## Fix round ${round}" in ${HO}. Return the state, head, commits, checks and what is open.`, { label: `noui:fix#${round}`, phase: 'Review', schema: BUILT })
  if (!fx) throw new Error('fix round failed')
  reviews = (await reviewOnce(round + 1)).filter(Boolean)
  blocking = reviews.flatMap(r => r.blocking)
}

phase('Gate')
const OPERATOR = 'You are a careful operator. Follow the steps exactly and report facts only: what you ran, the exit code, the log path and its last lines, the files you produced. Do not judge quality, do not edit code, do not commit, do not fix anything; if a step fails, record it and go on to the next.'
const deskSteps = (k) => `The desktop gate of REQUIREMENTS 59, round ${k}, on staging-wp/NOUI in ${WT.INT} (read-only for you; record git rev-parse HEAD first). Logs under ${OV}/logs/NOUI/gate-${k}/desk/, generated projects under ${OV}/out/NOUI/gate-${k}/. Run every row of this list, in order, one Gradle build at a time: ${JSON.stringify(plan.gate_desktop)}.`
const phoneSteps = (k) => `The phone gate of REQUIREMENTS 59, round ${k}, from staging-wp/NOUI in ${WT.INT} (read-only for you; record git rev-parse HEAD first). Run the whole session as one command through ${L} adb (with ${L} gradle inside it for the build), raw outputs under ${OV}/perf/NOUI/gate-${k}/ and logs under ${OV}/logs/NOUI/gate-${k}/phone/: ${JSON.stringify(plan.gate_phone)}. Pull screenshots from /data/local/tmp.`
const verifyPrompt = (half, raw, focus) => `You are the Opus verifier for the "${half}" half of REQUIREMENTS 59's gate. A Sonnet operator ran it; trust nothing until you check it. Its report: ${JSON.stringify(raw)}. ${RULES}
Check every run against its log (open the logs, read the failures), re-run any check whose log is missing, cut short or ambiguous, and ${focus}. Do not fix code. Write your rows to ${OV}/release/NOUI-GATE.md under the heading "${half}" (create the file if needed). Return the verified rows.`
let gate = null
for (let k = 1; k <= 2; k++) {
  const [deskRaw, phoneRaw] = await parallel([
    () => agent(`${OPERATOR} ${RULES}\n${deskSteps(k)}`, { label: `noui:gate-desk#${k}`, phase: 'Gate', schema: RAW, model: 'sonnet' }),
    () => agent(`${OPERATOR} ${RULES}\n${phoneSteps(k)}`, { label: `noui:gate-phone#${k}`, phase: 'Gate', schema: RAW, model: 'sonnet' }),
  ])
  const [desk, phone] = await parallel([
    () => agent(verifyPrompt(`Desktop, round ${k}`, deskRaw, 'confirm the counts (tests run, failures, skips) and the build results from the logs yourself, and run the byte-identical diff yourself (outside the paths the plan names, every file the same with the box unticked)'), { label: `noui:verify-desk#${k}`, phase: 'Gate', schema: TESTED }),
    () => agent(verifyPrompt(`Phone, round ${k}`, phoneRaw, 'open every screenshot and judge it yourself (the blank start screen and a placeholder route, nothing clipped, text from resources), re-derive cold start from the raw files, confirm the overlay is absent from the prod APK, and read the crash log'), { label: `noui:verify-phone#${k}`, phase: 'Gate', schema: TESTED }),
  ])
  gate = { desk, phone }
  const failures = [...(desk ? desk.failures : ['desktop verification failed to run']), ...(phone ? phone.failures : ['phone verification failed to run'])]
  if (desk && phone && desk.state === 'green' && phone.state === 'green') break
  if (k === 2) break
  log(`gate round ${k}: ${failures.length} failures, fixing`)
  const gf = await agent(`You fix what REQUIREMENTS 59's gate found on staging-wp/NOUI in ${WT.INT}. ${RULES} Read ${OV}/release/NOUI-GATE.md and ${HO}. Failures: ${JSON.stringify(failures)}. Fix each at its root, one commit per cause, with a test where the cause is code; re-run the checks each fix touches through the locks. Write what you did under "## Gate fix ${k}" in ${HO}.`, { label: `noui:gate-fix#${k}`, phase: 'Gate', schema: BUILT })
  if (!gf) throw new Error('gate fix failed')
}

phase('Release')
const rc = await agent(`You prepare the setup-only release candidate of REQUIREMENTS 59 (k). ${RULES} Read ${PLAN_MD}, ${HO} and ${OV}/release/NOUI-GATE.md.
1. If the gate is not green (desktop ${gate && gate.desk ? gate.desk.state : 'missing'}, phone ${gate && gate.phone ? gate.phone.state : 'missing'}) or any review finding is still blocking (${JSON.stringify(blocking)}), say "not-ready" with exactly what is red, and stop after step 3.
2. Otherwise create the local branch release/setup-only from staging-wp/NOUI's head (git -C ${R} branch release/setup-only staging-wp/NOUI) in a new worktree ${OV}/wt/RELEASE-SETUP-ONLY, and commit there only the release switch turned on (the plan: ${plan.release_switch}), with a message that says why; run the generator suite, the website build and tests and the API's tests on it through the locks, and confirm that a generation request asking for UI components is refused and a No-UI one succeeds.
3. Write ${OV}/release/SETUP-ONLY-DEPLOY.md for the user in plain English, readable on a phone: what goes live (what a setup-only project contains, what the site shows), the checks that passed with their evidence paths, exactly what they must do before the deploy (${JSON.stringify(plan.user_steps)}), the deploy steps the orchestrator runs after their go (a throwaway draft PR for CI first, then the fast-forward of main to release/setup-only, which triggers ci.yml's deploy job), how to check it live, and how to roll back (main back to its previous head through a new revert, never a force push). Also write ${OV}/release/SETUP-ONLY-NOTES.md: the release notes for the public page (what's new, in plain words).
Return the state, the branch, its head, both paths, a short summary and what is open.`, { label: 'noui:release', phase: 'Release', schema: RC })
return { plan: { path: plan.plan_path, summary: plan.summary, user_steps: plan.user_steps, risks: plan.risks }, built, integrated, openBlocking: blocking, gate, rc }
