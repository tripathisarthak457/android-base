export const meta = {
  name: 'android-base-to-release',
  description: 'Carry the android-base overhaul from its current state to a release candidate: every remaining package with dependency waits, Google-bar review loops, serialized merges, the CP2 glass decision, visual QA round 2 and the final verification matrix',
  phases: [
    { title: 'Implement', detail: 'package agents in their worktrees' },
    { title: 'Review', detail: 'fresh-context Google/AOSP-bar reviewer per package, then fixes' },
    { title: 'Decide', detail: 'CP2 glass decision from the phone recordings' },
    { title: 'Merge', detail: 'serialized merges into overhaul/multiplatform-v2 with integration checks' },
  ],
}

// Fable first, until its weekly limit runs out; after a failed Fable call the rest of the run uses Opus (the user,
// Oct 15). A relaunch tries Fable again, so the switch happens by itself once the limit resets.
// Oct 15, 20:55: the user switched every agent to Opus 5.5 only, so Fable is skipped from the start.
let fableOut = true
async function fableAgent(prompt, opts) {
  if (!fableOut) {
    const r = await agent(prompt, Object.assign({}, opts || {}, { model: 'fable' }))
    if (r !== null && r !== undefined) return r
    fableOut = true
    log('a Fable call failed (weekly limit?); the rest of this run uses Opus')
  }
  // Opus 5.5 only (the user, Oct 15 20:55). Opts pass through unchanged: an agent with no model runs on this
  // session's model (Opus 5.5), and replays of calls made before the Fable switch match their cached results.
  // Drop any per-call model too, so nothing runs on Fable.
  const { model, ...rest } = opts || {}
  return agent(prompt, rest)
}

// PeriFlow (WP11) is not restored on the Windows machine (2026-10-17): its private repository and build secrets are
// not here. Set PERIFLOW to true once they are, and WP11 and its lens fix run as before.
const PERIFLOW = false
const R = 'D:/projects/tools/android-base'
const PFR = 'D:/projects/jorwen/periflow'
const OV = 'D:/projects/tools/.overhaul'
const DESIGN = OV + '/design'
const RS = OV + '/research'
const L = OV + '/bin/locked'
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
const INTEG = 'overhaul/multiplatform-v2'

// ---------- signals: merged packages, decisions, milestones ----------
const signals = {}
function signal(k) {
  if (!signals[k]) { let r; const p = new Promise(res => { r = res }); signals[k] = { p, r, done: false } }
  return signals[k]
}
function fire(k) { const s = signal(k); if (!s.done) { s.done = true; s.r(true); log(`signal: ${k}`) } }
const ended = {}
function endedOf(name) {
  if (!ended[name]) { let r; const p = new Promise(res => { r = res }); ended[name] = { p, r } }
  return ended[name]
}
const producerOf = k => (k === 'CP1' || k === 'CP2' ? k : k.split(':')[0])
function waitFor(keys) {
  if (!keys.length) return Promise.resolve('ok')
  const all = Promise.all(keys.map(k => signal(k).p)).then(() => 'ok')
  const dead = keys.map(k => endedOf(producerOf(k)).p.then(() => (signal(k).done ? new Promise(() => {}) : 'dead:' + k)))
  return Promise.race([all, ...dead])
}
// LENS:generator: the generator's lens on tap is done and committed on wp/WP05 (SESSION-2026-10-15.md, handoff/WP05.md
// "Lens on tap"), so a relaunch does not redo it; WP05's package review covers that code before it merges.
for (const k of ['WP01', 'WP02-M1', 'WP09', 'WP10', 'CP1', 'WP03:phaseA', 'WP04:phase2', 'LENS:generator']) fire(k)

// ---------- packages ----------
const ALL_MERGED = ['WP01', 'WP02-M1', 'WP09', 'WP10', 'WP08', 'WP04', 'WP03', 'WP06', 'WP02-M2', 'WP05', 'WP07-A', 'WP02-M3', 'WP14', 'MB', 'QA2', 'WP07-B', 'WP07-C', 'WP11', 'WP12']
const PKGS = {
  'WP04': { wt: 'WP04', file: 'WP04.md', start: [], kind: 'android',
    scope: 'The whole package. Phase 2 is measured and CP1 is decided (decisions/CP1.md). Finish phase 3 and every WP04 item in the research plans, qa/r1/for-WP04.md and REQUIREMENTS 24 (Gmail-style predictive back), 28 (tab-switch effect), 30 and 32 (back swipe during a push), the gates in gates.toml and the baseline profiles. Run 10 was cut off with phone-gates.sh phase3b queued and two tests uncommitted.' },
  'WP10': { wt: 'WP10', file: 'WP10.md', start: [], kind: 'web',
    scope: 'The whole package (website configurator, previews, admin metrics). It is implemented and accepted through six review rounds; finish what the latest review found. Also REQUIREMENTS 36 (Android first): the platform picker offers Android only, and Compose Multiplatform and Swift show "coming soon" (not selectable) until phase B ships; the copy must not promise them yet.' },
  'WP08': { wt: 'WP08', file: 'WP08.md', start: [], kind: 'swift',
    scope: `The whole package. First finish the fixes for the review findings in ${OV}/handoff/WP08-review-r1.md: the fix run was cut off midway, so check every item against the branch and close what is still open. Then re-run accept.sh.` },
  'WP06': { wt: 'WP06', file: 'WP06.md', start: [], kind: 'android',
    scope: 'The whole package, plus the WP06 items in the research plans, qa/r1/for-WP06.md (the DEV badge overlapping the large title and the back button) and the security items in ANDROID-SKILLS-DIGEST.md. Run 3 committed the bulk; next were the owned unit tests, the QA-44 device check, accept.sh, step 16 sizes and the full handoff.' },
  'WP02-M2': { wt: 'WP02', file: 'WP02.md', start: [], kind: 'android',
    scope: 'Milestone M2 ONLY (the "Milestone M2" section), plus the STANDARDS-PLAN.md items it owns (deeper folder structure, code rules in verifyCodeRules, vendoring Google Android skills per ANDROID-SKILLS-DIGEST.md), qa/r1/for-WP02-M2.md and the AGP 9 flag clean-up. Its database scaffolding needs WP06 merged; the sharedTransitions option needs WP04 merged.' },
  'WP03': { wt: 'WP03', file: 'WP03.md', start: [], kind: 'android',
    scope: 'The whole package: the call-site sweep, phase B with the CP1 values (decisions/CP1.md, including its tokens/motion.json block), every WP03 item in the research plans and qa/r1/for-WP03.md, the pull to refresh (REQUIREMENTS 29) measured before and after on the phone, and the items its handoff lists as waiting for WP04 (SheetScene onto Presentation.Sheet, the bar files, the font preload call, navigation transitions onto the tokens, AppBottomActionBar shell hooks).' },
  'WP05': { wt: 'WP05', file: 'WP05.md', start: ['LENS:generator'], kind: 'android',
    scope: 'The whole package (optional liquid glass, Kyant backdrop 2.0.1 + shapes 1.2.1). The glass foundation under foundation/glass is committed and tested; finish what remains of it first. Then, once WP03 and WP04 are merged (return state "waiting" with waiting_for ["WP03", "WP04"] until they are): LiquidTabs on the bar and rail, the shell stage behind <opt:liquidglass>, the catalog Glass section, the glass gates of master plan section 2 on the phone, and the checkpoint-2 recordings (per tier: tab tap, lens drag, scrolling under the bar, bar minimize, with two or three blur and tint variants, plus per-tier numbers) under perf/WP05/cp2/. When the recordings and numbers are written, return state "waiting" with waiting_for ["CP2"]; after decisions/CP2.md exists, apply its values to the tokens and finish the acceptance.' },
  'WP07-A': { wt: 'WP07', file: 'WP07.md', start: [], kind: 'android',
    scope: 'Phase A in full (the "Phase A" section), plus the multiplatform items of ADAPTIVE-PLAN.md, MOTION-PLAN.md and STANDARDS-PLAN.md that belong to phase A. Everything except the core/navigation shell port is written and committed; the first full build was queued. The shell port needs WP04 merged (return waiting_for ["WP04"] until it is), then port it faithfully and pass the whole Phase A acceptance, including the iOS klib compile attempt recorded in the handoff.' },
  'WP11': { wt: 'WP11', file: 'WP11.md', start: ['LENS:periflow'], kind: 'periflow', noMerge: true,
    scope: `The whole of WP11 in two halves. First half (no template ports): step 1 baseline into ${OV}/perf/WP11/before/, steps 6, 7, 8, 9, the PeriFlow-local glass fixes of step 5 (G1, G2/G3, G6/G13, G9/G10, G11, G12, G16, G17), step 10 tests, step 11 re-measure into ${OV}/perf/WP11/after-early/. Run 4 left steps 5, 6, 7, 8 and 10 uncommitted under review and the baseline APK rebuild in wt/WP11-base. Second half, once WP04, WP05 and WP06 are merged and decisions/CP2.md exists (return state "waiting" with waiting_for ["WP04", "WP05", "WP06", "CP2"] until then): steps 2-4 and the generator ports of step 5 (LiquidTabs, SceneBackdrop, KeptTabs v2, BackGate, sheets, insets), then the final measurements and the device gates of the WP file.
PeriFlow rules: worktree ${OV}/wt/WP11 on the LOCAL branch overhaul/smooth-glass; never push, never set an upstream, never touch anything outside periflow-android/, stage explicit paths only (never git add -A), never touch periflow-recovery-codes.txt or PeriFlow-Postman/. Every PeriFlow Gradle command: ${L} gradle ./gradlew -x :app:uploadCrashlyticsMappingFileDevBenchmarkRelease <tasks> (the heap comes from the global D:/.caches/gradle/gradle.properties, so it shares the template's daemon; never pass -Dorg.gradle.jvmargs).` },
  'WP02-M3': { wt: 'WP02', file: 'WP02.md', start: ['WP02-M2', 'WP03', 'CP1'], kind: 'android',
    scope: 'Milestone M3 ONLY (the "Milestone M3" section): the reference features use the new design system, plus the WP02-M3 items of the research plans (adaptive screens, motion, edge-to-edge insets in contentPadding) and qa/r1/for-WP02-M3.md. Milestone M2 is already merged from this same branch; merge the integration branch first.' },
  'WP14': { wt: 'WP14', file: 'WP14.md', start: ['WP02-M2', 'WP06'], kind: 'python',
    scope: 'The whole package: upgrade_project.py (+ --diff), add_tab.py, add_language.py, add_feature.py/remove_feature.py in both modes with the v1 refusal, tests and matrix row 14.' },
  'MB': { wt: 'MB', file: null, start: ['WP02-M3', 'WP14'], kind: 'android',
    scope: `REQUIREMENTS 31's optional "materialbridge" feature (there is no WP file; write a short design at the top of ${OV}/handoff/MB.md first). Off by default. One module that maps our design tokens into a MaterialTheme (colors, typography, shapes, ripple off: no indication), so a team can drop in a complex Material 3 component without it looking foreign. Code rules (verifyCodeRules) confine every androidx.compose.material3 component import to that module; the build stays Material-free when the feature is off (prove it with the dependency report). A generator feature TOML with catalogue entry and tests, a catalog demo screen inside the module, and the docs facts WP12 needs in the handoff ("Rules and gotchas the docs must state").` },
  'QA2': { wt: 'QA2', file: null, start: ['WP04', 'WP03', 'WP05', 'WP06', 'WP02-M3'], kind: 'android',
    scope: `Visual and motion QA round 2 on the merged Android template, then fix what it finds (there is no WP file; the owners have merged, so this branch may touch any template path). Generate the standard, everything and everything-with-glass projects from the integration head, build release (devRelease or benchmark) APKs, and on the phone: screenshot every screen in light and dark, at font scale 1.0 and 1.5, and in a large-window configuration (adb shell wm size 1600x2560 and wm density 320; read and write down the original values first and ALWAYS restore them with wm size reset / wm density reset and the original font_scale, even on failure). Record clips (screenrecord --size 720x1600) of the tab switch, push and pop, the Gmail-style predictive back, a back swipe during a push, pull to refresh, sheets and, with glass, the bar. Check every item of qa/r1/DEFECTS.md is really fixed, REQUIREMENTS 17, 18, 21, 23, 24, 28, 29, 30, 32 and 33 (the bottom sheet system: verify every catalog sheet variant against research/SHEET-SPEC.md and its phone gates), and compare the motion against research/ios-motion-catalogue.md and decisions/CP1.md. Write ${OV}/qa/r2/DEFECTS.md, fix every defect on this branch with tests where they apply, recapture after, and put before/after images under ${OV}/qa/r2/.` },
  'WP07-B': { wt: 'WP07', file: 'WP07.md', start: ['WP07-A', 'WP03', 'WP05', 'CP2'], kind: 'android',
    scope: 'Phase B in full (the "Phase B" section): the design system, shell parity (presentation host, BackGate, kept tabs tests in common), glass with the CP2 values, the Multiplatform bottom sheet parity of research/SHEET-SPEC.md, and the Android device gates on the solid column including rapid Back and gesture pop. Phase A is merged from this same branch; merge the integration branch first.' },
  'WP07-C': { wt: 'WP07', file: 'WP07.md', start: ['WP07-B'], kind: 'android',
    scope: 'Phase C in full (the "Phase C" section): deeplink and auth. Phase B is merged from this same branch; merge the integration branch first.' },
  'WP12': { wt: 'WP12', file: 'WP12.md', start: ['WP02-M3', 'WP04', 'WP05', 'WP06', 'WP14', 'MB', 'QA2'], kind: 'docs',
    scope: `The whole package, plus REQUIREMENTS 25 (every smoothness finding, fix and optimisation in AGENTS.md, CLAUDE.md and the docs of the repository and of every generated project) and the WP12 instructions at the end of ${OV}/notes/INDEX.md. REQUIREMENTS 36 (Android first): this run covers the Android template, the generator, the website and the repository docs, which ship in the Android release. Start from the drafts in ${OV}/docs-draft/ and verify every [verify] item against the merged code. The Compose Multiplatform and iOS docs are WP12-B, after those templates merge; here, say only that they are coming.` },
}
// WP13 runs last, after everything above.
PKGS['WP13'] = { wt: 'WP13', file: 'WP13.md', start: ['WP12', 'WP10', 'QA2', 'WP02-M3', 'WP14', 'MB'], kind: 'android',
  scope: `REQUIREMENTS 36: this is the ANDROID release verification. The Android generator ships as soon as it is 100% done; Compose Multiplatform and Swift follow later (WP13-B). Run the whole package for everything Android: the static matrix, the 14-row matrix and the wide sample for the Android template, every Android preset's final device gates on the Redmi (iOS-level targets, REQUIREMENTS 19), the security audits (the android-intent-security and android-permissions-security skills over the generated app), the website build and its Android-only platform picker, and the API. Multiplatform and iOS rows run as information only: they must not break anything Android, and they never block. The PeriFlow numbers in the report are optional (mark them pending if WP11 has not measured). Do not push and do not merge to main: the user approves the deploy after a release gate. Also: close every item in ${OV}/followups.md (findings a review left open when its rounds ran out); the slop sweep over the whole repository; and REQUIREMENTS 20, the final before/after report: write ${OV}/report/FINAL.md with every metric of perf/WP04/baseline/summary.md (old template) against the final gates (new template), per preset, plus PeriFlow before/after from perf/WP11, the pull-to-refresh numbers, cold start, PSS and APK sizes, with how each was measured. accept_ok=true only when the full matrix, the wide matrix sample, the static matrix, the final device gates and the website build are all green.` }

PKGS['WP12-B'] = { wt: 'WP12', file: 'WP12.md', start: ['WP12', 'WP07-C', 'WP08'], kind: 'docs',
  scope: 'Phase B of the docs (REQUIREMENTS 36): the Compose Multiplatform and SwiftUI templates\' docs and agents\' entry files, with the smoothness, motion, sheet and adaptive rules each follows, and the repository docs updated for both. WP12 (Android) is merged from this same branch; merge the integration branch first.' }
PKGS['WP13-B'] = { wt: 'WP13', file: 'WP13.md', start: ['WP13', 'WP07-C', 'WP08', 'WP12-B'], kind: 'android',
  scope: `Phase B verification (REQUIREMENTS 36): the Compose Multiplatform rows of the matrix and its Android device gates (solid column, rapid Back, gesture pop), the iOS klib compile attempt, the SwiftUI static checks and acceptance, and the Android quick subset to prove nothing regressed. Update ${OV}/report/FINAL.md with the multiplatform numbers. The website then offers Compose Multiplatform as "Beta: iOS unverified"; Swift stays "coming soon" (no Mac to test). WP13 (Android) is merged from this same branch; merge the integration branch first. Do not push or merge to main.` }

// The overhaul moved to Windows on Oct 17, where the generator's own suite fails six tests with no change at all.
PKGS['WIN'] = { wt: 'WIN', file: null, start: [], kind: 'python',
  scope: `Make the generator correct on Windows hosts, as on Linux, without weakening any test (there is no WP file; the scope below is the specification). On this Windows machine, at the integration head, python -m unittest discover -s tests -t . in generator/ fails 5 tests and errors 1 (the baseline log is in ${OV}/logs/windows/generator-baseline.md):
1. test_golden (cases all, none, hostile): transformed.txt comes out with CRLF. Path.write_text translates newlines on Windows, so every file the generator writes (13 or more write_text call sites in genkit: transform, tokens, catalogue, look, strings, scaffold, target, icons, ...) and the emitted files (web/frontend/lib/catalogue.json, DesignTokens.*) must be written byte-for-byte as on Linux: newline='\\n' or write_bytes, one consistent helper, no CRLF anywhere in a generated project on any host.
2. test_golden.test_the_wrapper_script_is_executable_and_nothing_else_is: Windows has no exec bit, so os.stat reports run.bat as executable and gradlew as not. The guarantee that matters is that gradlew is executable where it is used: in the zip (external_attr) and, when the git_init feature commits the project, in the git index (git update-index --chmod=+x or equivalent). Prove both on every host; keep the filesystem assertion where the OS has exec bits.
3. test_cli.test_json_means_only_json_on_stdout: the reported path and the test's path differ only by 8.3 short names (C:\\Users\\CRAZYH~1 vs C:\\Users\\crazyhusir). Decide which path the CLI should report (the one the caller passed, or one canonical form) and make the code and the test agree for real, not by loosening the comparison to anything-goes.
4. test_cli symlink test: os.symlink needs a privilege on Windows (WinError 1314). Skip it only where the OS cannot create symlinks, with the reason in the skip message; it keeps running on Linux and in CI.
Also check the rest of the generator for the same classes of host bug (newline translation, path separators in reported or stored paths, case-insensitive file systems, file modes) and fix what you find. Acceptance: the whole suite passes on this Windows machine with python -m unittest discover -s tests -t . (run it through ${OV}/bin/memwait), git diff shows no CRLF introduced, and the generated "standard" project is byte-identical to the one generated from the same spec on Linux (compare against the golden trees). This package merges first: until it does, the integrator judges a merge by failures beyond this baseline.` }

const KNOWN = new Set([...Object.keys(PKGS).filter(id => PERIFLOW || PKGS[id].kind !== 'periflow'), 'WP01', 'WP02-M1', 'WP09', 'CP1', 'CP2'])
const UI_ANDROID = new Set(['WP03', 'WP04', 'WP05', 'WP06', 'WP02-M2', 'WP02-M3', 'QA2', 'MB'])

const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'waiting', 'blocked'] }, waiting_for: { type: 'array', items: { type: 'string' } }, milestones: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' }, accept_ok: { type: 'boolean' } }, required: ['state', 'waiting_for', 'milestones', 'summary', 'accept_ok'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }
const MERGE = { type: 'object', properties: { ok: { type: 'boolean' }, details: { type: 'string' }, needs_package_fix: { type: 'boolean' } }, required: ['ok', 'details', 'needs_package_fix'] }
const DECISION = { type: 'object', properties: { decided: { type: 'boolean' }, summary: { type: 'string' } }, required: ['decided', 'summary'] }

const ORCH = `
Orchestrator rules (they add to the WP file and the master plan; REQUIREMENTS.md wins on conflict):
- Today is 2026-10-17. The user is away and asked not to be asked anything: decide soundly and record decisions in your handoff. Granted: npm ci in web/frontend (through ${OV}/bin/memwait); Gradle dependency resolution (incl. androidx.room3, io.insert-koin, io.ktor darwin, io.github.kyant0, org.jetbrains.androidx.navigation3, org.jetbrains.compose, the Kotlin/Native toolchain). Not granted: pushing anything, merging into main, installing anything, other downloads. There is no Docker on this machine: the Postgres store tests and go test -race run in CI only.
- QUALITY BAR (REQUIREMENTS 27): Google/AOSP-level code. ${RS}/CODE-STANDARDS.md, ${RS}/STANDARDS-PLAN.md, the androidx Compose API guidelines, and Google's Android skills at ${SKILLS} (read the relevant SKILL.md before working in its area; digest at ${RS}/ANDROID-SKILLS-DIGEST.md). For any Compose UI change, follow Google's jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md): run its exhaustive audit over the composables you touch and read the matching references/*.md before each fix (the user, Oct 16). Zero AI-slop comments: comment why, never what; no history or narration.
- Which packages are merged: git -C ${R} tag -l 'merged/*'. Decisions: ${OV}/decisions/CP1.md and CP2.md (CP2 exists only once decided).
- Gradle only via ${L} gradle; adb only via ${L} adb; long commands in the background with logs, polled with tail/grep. The Gradle lock is shared by every agent, so work on code while you wait. Memory safety (REQUIREMENTS "Memory safety"): never raise the Gradle heap. Never kill, stop or signal processes you did not start in this run (no taskkill, no Stop-Process), and never run ./gradlew --stop: a stale build from an earlier run finishes on its own.
- Windows (the user's machine since Oct 16; native, no WSL): your Bash tool is Git Bash. ${L} and ${OV}/bin/memwait set JAVA_HOME, ANDROID_HOME (C:/Users/crazyhusir/AppData/Local/Android/Sdk), GRADLE_USER_HOME (D:/.caches/gradle) and the Go, pip and npm cache variables, which your shell lacks: run gradlew, adb, go, npm and pip only through them, never bare (a bare run creates forbidden cache folders under C:/Users). Gradle's heap comes from the global D:/.caches/gradle/gradle.properties: never pass -Dorg.gradle.jvmargs or kotlin.daemon.jvmargs. Set MSYS_NO_PATHCONV=1 for adb device paths (/sdcard/...) and for git show <ref>:.<path>; with it set, give git D:/ paths, not /d/. Helper scripts under ${OV}/out and ${OV}/logs were written on Linux: before re-running one, port its paths (/home/crazyhusir/Documents/projects/tools is D:/projects/tools) and its Linux-only commands (flock, /proc, /sys). Never write under %APPDATA% and never pip install outside a venv. Generator unit tests: until package WIN merges, 6 of them fail on this machine with no change at all (${OV}/logs/windows/generator-baseline.md); judge your work and every merge by failures beyond those. Perfetto's trace_processor v57.2 is pre-fetched under D:/.caches/perfetto: run it only as USERPROFILE='D:\\.caches\\perfetto' python ${SKILLS}/profilers/android-profiler/references/perfetto/bin/trace_processor <args> (without that override it downloads itself again into the user profile). ffmpeg and ffprobe 9.0.1 are installed and on PATH inside ${L} and ${OV}/bin/memwait. Never install anything yourself.
- Phone: serial 9a7c74f4 (Redmi Note 10 Pro, 120 Hz). screenrecord needs --size 720x1600. Judge smoothness only on release or benchmark builds. Restore any device setting you change for a test. There is no emulator on this machine: if the phone is missing (${L} adb adb devices), do every code and test step first, poll for it in the background for up to 45 minutes, then record in the handoff which phone steps remain.
- Commit after every completed step with a message that explains why, ending with the line "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>". Stage explicit paths only.`

function wpRef(p) { return p.file ? `${DESIGN}/wp/${p.file}` : `(no WP file: the scope below is the specification)` }

function implPrompt(id, label, extra) {
  const p = PKGS[id]
  const wtPath = `${OV}/wt/${p.wt}`
  const setup = p.kind === 'periflow'
    ? `Worktree ${wtPath} (PeriFlow repo ${PFR}, local branch overhaul/smooth-glass; the WP file's Setup describes it).`
    : `Worktree ${wtPath} on branch wp/${p.wt}; create it if missing with git -C ${R} worktree add ${wtPath} -b wp/${p.wt} ${INTEG}. Work only there. Merge the integration branch into yours first and whenever a package you depend on has merged (git merge ${INTEG}; the owner wins inside owned paths).`
  return `You are the implementation agent for package ${id} of the android-base overhaul (${R}, integration branch ${INTEG}). Run: ${label}.
Read before editing: ${OV}/REQUIREMENTS.md (all of it); ${OV}/notes/INDEX.md and every plan it lists that names ${id}; the package file ${wpRef(p)}; the MASTER-PLAN sections it names (${DESIGN}/MASTER-PLAN.md is 218 KB: grep the heading and read a line range; always "Part D. Rules every implementation agent follows" and A11.3); your handoff ${OV}/handoff/${id}.md if it exists.
Earlier runs on this package may have been cut off by usage limits: reconstruct where they stopped from the handoff, git log and git status / git diff --stat. Uncommitted changes are work in progress: review them, keep what is right, commit in logical steps, continue. Keep a "## Progress" section at the top of the handoff current, so a cut-off run loses nothing.
SCOPE: ${p.scope}
${setup}
${ORCH}
WAITING: if all that remains is blocked on a package that has not merged or a decision that does not exist yet, commit, write exactly where you stopped in the handoff, and return state="waiting" with waiting_for taken only from ${JSON.stringify([...KNOWN])}. List milestones as "<id>:<name>" (e.g. "${id}:accept"); never a bare package id.
DONE: state="done" only when every acceptance command for the scope passes (the accept log ends with ACCEPT OK where the WP file defines one), the handoff is complete (including "Measurements" and "Rules and gotchas the docs must state" for WP12), and git status is clean. state="blocked" only for something you cannot resolve soundly, with evidence.
${extra || ''}`
}

// Long implementation runs take at most four of the workflow's six agent slots, so reviews, fixes and merges of
// packages that are nearly done never queue behind them.
// Three while the lens fix (the user's priority) holds two slots of its own; the fourth is added when it ends.
const IMPL_SLOTS = 3
let freeSlots = IMPL_SLOTS
function addImplSlot() { const next = slotQueue.shift(); if (next) next(); else freeSlots++ }
const slotQueue = []
async function withImplSlot(fn) {
  if (freeSlots > 0) freeSlots--
  else await new Promise(res => slotQueue.push(res))
  try { return await fn() } finally { const next = slotQueue.shift(); if (next) next(); else freeSlots++ }
}

async function runImpl(id, label, extra) {
  const long = label === 'run' || label.startsWith('continue')
  const call = () => fableAgent(implPrompt(id, label, extra), { label: `${id}:${label}`, phase: 'Implement', schema: STATUS })
  const r = await (long ? withImplSlot(call) : call())
  if (!r) throw new Error(`${id} agent failed (${label})`)
  for (const m of r.milestones || []) if (m.includes(':')) fire(m)
  return r
}

async function implementUntilDone(id) {
  let r = await runImpl(id, 'run')
  let n = 0
  while (r.state === 'waiting' && n < 8) {
    const unknown = r.waiting_for.filter(k => !KNOWN.has(k))
    if (unknown.length) { log(`${id} waits for ${unknown}, which nothing here produces`); return { ...r, state: 'blocked' } }
    if (id === 'WP05' && r.waiting_for.includes('CP2')) fire('WP05:cp2-recordings')
    log(`${id} waiting for ${r.waiting_for.join(', ')}`)
    const w = await waitFor(r.waiting_for)
    if (w !== 'ok') return { ...r, state: 'blocked', summary: `${w} never arrived. ${r.summary}` }
    n++
    r = await runImpl(id, `continue#${n}`, `What you waited for (${r.waiting_for.join(', ')}) is now merged or decided. Merge the integration branch and continue.`)
  }
  return r
}

function reviewTarget(id) {
  const p = PKGS[id]
  return p.kind === 'periflow' ? `git -C ${PFR} diff main...overhaul/smooth-glass -- periflow-android` : `git -C ${R} diff ${INTEG}...wp/${p.wt}`
}
const LENS = {
  android: `${RS}/CODE-STANDARDS.md, ${RS}/STANDARDS-PLAN.md, ${RS}/ANDROID-SKILLS-DIGEST.md, the androidx Compose API guidelines, Google's jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md, all seven categories; a violation in changed Compose code is BLOCKING)`,
  periflow: `${RS}/CODE-STANDARDS.md, ${RS}/ANDROID-SKILLS-DIGEST.md, the androidx Compose API guidelines, Google's jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md), ${OV}/audit/periflow-learnings.md`,
  swift: `the Swift API Design Guidelines, Swift 6 strict concurrency, current SwiftUI and Observation practice (iOS 17+, iOS 26 APIs behind availability checks), and the WP file`,
  web: `React and Next.js current practice, WCAG 2.2 AA, the website's own tests and the WP file`,
  python: `PEP 8, the generator's existing conventions and tests, and the WP file`,
  docs: `accuracy against the code (every command, path, task and number must exist and be current), REQUIREMENTS 25, and plain, precise writing without filler`,
}

async function reviewLoop(id) {
  const p = PKGS[id]
  for (let round = 1; round <= 4; round++) {
    const rv = await fableAgent(`You are a fresh-context code reviewer for package ${id} of the android-base overhaul, holding it to the Google/AOSP bar (REQUIREMENTS 27).
Review: ${reviewTarget(id)} (start with --stat; read every changed source file; skim generated files and lockfiles).
Judge against: ${LENS[p.kind]}; ${DESIGN}/MASTER-PLAN.md "Part D" and A11.3; the package file ${wpRef(p)} (scope: ${p.scope}); ${OV}/REQUIREMENTS.md; the plans in ${OV}/notes/INDEX.md that name ${id}; the package's handoff ${OV}/handoff/${id}.md.
BLOCKING: correctness bugs; perf hazards (animated state read in composition, alpha on full-screen or backdrop layers, per-event coroutine launches, infinite animations without visibility or reduce-motion handling, allocations in draw, recomposition per frame); public API guideline violations; narrating or history comments, AI slop, commented-out code, needless abstraction; dead code; missing or weakened tests; substantive edits outside the package's owned paths; secrets; placeholders presented as finished; non-adaptive UI where the plans require it; claims in the handoff that the code or measurements do not support.
NON-BLOCKING: nits. Do not modify anything. Cite file and line.`, { label: `review:${id}#${round}`, phase: 'Review', schema: REVIEW })
    if (!rv) throw new Error(`review of ${id} failed`)
    if (!rv.blocking.length) return { clean: true, notes: rv.nonblocking }
    log(`${id} review #${round}: ${rv.blocking.length} blocking`)
    const fx = await runImpl(id, `fix#${round}`, `MODE: fix. A fresh-context reviewer found BLOCKING issues. Fix every one on your branch (commits with why-messages), re-run the affected acceptance commands, update the handoff, and return state="done":\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking (address cheaply or record why not): ${JSON.stringify(rv.nonblocking)}`)
    if (fx.state !== 'done') return { clean: false, notes: [`fix run ended ${fx.state}: ${fx.summary}`], stop: true }
  }
  const last = await fableAgent(`You are a fresh-context code reviewer for package ${id} of the android-base overhaul. Review ${reviewTarget(id)} against ${LENS[p.kind]} and ${OV}/REQUIREMENTS.md. Four review-and-fix rounds have run. List only the BLOCKING problems that are still present (correctness, perf hazards, guideline violations, slop, missing tests). Do not modify anything; cite file and line.`, { label: `review:${id}#final`, phase: 'Review', schema: REVIEW })
  return { clean: !!last && !last.blocking.length, notes: last ? last.blocking.map(b => `${b.file}: ${b.problem}`) : ['final review failed'], open: last ? last.blocking : [] }
}

let mergeChain = Promise.resolve()
function mergePackage(id, openFindings) {
  const p = PKGS[id]
  const extraSteps = [
    id === 'WP04' ? 'After the merge is green, write the ratchet rows into templates/android/tools/perf/gates.toml from perf/WP04/final (B.5 rule 4) and commit.' : '',
    (id === 'WP02-M2' || id === 'WP06') ? 'If both WP02-M2 and WP06 are now merged, write tools/size_budgets.toml (measured + 5 %) and commit (B.5 rule 4).' : '',
    id === 'WP02-M3' ? 'This merge triggers the full device re-gate (A12.3 / A12.8): run the final perf gates for everything, standard and lean on the phone and record them under ' + OV + '/perf/integration/WP02-M3/.' : '',
    openFindings && openFindings.length ? `Review rounds ran out with findings still open; append them to ${OV}/followups.md under a "${id}" heading (WP13 closes them): ${JSON.stringify(openFindings)}` : '',
  ].filter(Boolean).join('\n')
  const run = async () => {
    for (let attempt = 1; attempt <= 2; attempt++) {
      const m = await fableAgent(`You are the orchestrator's integrator. Merge package ${id} (branch wp/${p.wt}) into ${INTEG} in ${R}.
0. Verify the checkout is on ${INTEG}. If a merge from an interrupted earlier run is in progress or the tree is dirty, inspect it: finish it if it is this package's merge and sound, otherwise git merge --abort / restore, and record what you found. If git tag -l merged/${id} already exists, the merge is done: verify and return ok=true.
1. git -C ${R} merge --no-ff wp/${p.wt} with a message that says in one or two sentences why the package exists, ending with: Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>. Conflicts: master plan B.1 (libs.versions.toml and settings.gradle.kts keep both sides' lines in order; emitted files regenerated with --write; owner wins inside owned paths). A substantive conflict: git merge --abort, return ok=false, needs_package_fix=true.
2. Integration checks, Gradle via ${L} gradle in the background with logs under ${OV}/out/integration/${id}/, polled: generator unit tests; python3 -m genkit.tokens --check and python3 -m genkit.core.catalogue --check (in generator/); templates/android ./gradlew assembleDevDebug; generate generator/tests/fixtures/v2/modules-compact.json if it exists and build assembleDevDebug testDevDebugUnitTest; web/frontend tests if the package touched web/; then the "Quick subset" of ${wpRef(p)} if it has one.
3. ${UI_ANDROID.has(id) ? `Perf smoke (A12.8 step 3) if templates/android/tools/perf/perf.sh and ${OV}/perf/WP04/final/summary.json exist (adb via ${L} adb; screenrecord needs --size 720x1600 on this phone).` : 'No perf smoke.'}
4. Green: git -C ${R} tag merged/${id}; ok=true. A failure caused by this merge: fix it only if small and mechanical (commit with a why message); otherwise git revert -m 1 --no-edit HEAD and return ok=false, needs_package_fix=true with the failing output.
${extraSteps}
Never push. ${ORCH}
Append an entry to ${OV}/merges.md.`, { label: `merge:${id}#${attempt}`, phase: 'Merge', schema: MERGE })
      if (!m) throw new Error(`merge of ${id} failed`)
      if (m.ok) { fire(id); return m }
      if (!m.needs_package_fix || attempt === 2) return m
      const fx = await runImpl(id, `mergefix#${attempt}`, `MODE: fix. The orchestrator could not merge your branch: ${m.details}\nMerge ${INTEG} into your branch, resolve, fix the failing checks, re-run your acceptance, update the handoff, return state="done".`)
      if (fx.state !== 'done') return { ok: false, details: fx.summary, needs_package_fix: true }
    }
  }
  mergeChain = mergeChain.then(run, run)
  return mergeChain
}


// ---------- the lens-tap fix (REQUIREMENTS 35): the user asked for it first; WP05 and WP11 start after it ----------
const LENS_BRIEF = `THE USER'S REPORT (Oct 12, 14:20, on PeriFlow's after-early build, glass tier Live): "the glass effect, the actual glass distortion when navigating, is gone". They asked for it to be fixed FIRST. Read ${OV}/REQUIREMENTS.md item 35 (and 21, 19, 27), the G1 section of ${OV}/audit/periflow-learnings.md (the old cost: a tab tap pressed and released the lens, and the bar body read pressProgress in a layerBlock, re-rendering the whole bar with inner shadow and dispersion for ~0.5 s), and ${OV}/research/ios-motion-catalogue.md (tab bar glass).
Target, iOS 26 tab bar: on a tap (and on any programmatic selection: back stack, deep link), the lens travels to the new tab as a liquid droplet. It lifts and swells slightly, refracts and magnifies what is under it, and stretches along the direction of travel while moving. It calms as the selection spring settles, all within the selection animation.
Preferred mechanism, the cheapest sound one (adopt it, or something provably cheaper with the same look):
- Derive a "travel" intensity from the lens's own slot spring velocity (normalised and clamped, eased), with no new animation and no new coroutine.
- Swell, refraction amount and stretch follow max(pressure, travel). Read it only in draw or in the lens's own layer block.
- The lens samples the backdrop while pressed OR travelling, and stops when travel drops below its threshold. Sampling happens only within the lens bounds, from the scene recorded once.
- Keep every real part of G1: the bar body is never re-rendered and nothing outside the lens reads the intensity. No dispersion or inner shadow on the tap path. The tab row stays the once-recorded GraphicsLayer.
- The Live tier only; LiveNoLens and below keep today's distortion-free tap. Reduce motion: no distortion.
- A drag keeps its full press behaviour.
${ORCH}
Phone missing? Check adb devices. Do every code and test step first; when a step needs the phone and it is still missing, poll for it in the background (an until-loop on adb devices, up to 45 minutes), then record in the handoff exactly which phone steps remain.`

const LENS_TREES = [
  { key: 'periflow', diff: `git -C ${OV}/wt/WP11 diff 06ad83e..HEAD -- periflow-android`, prompt: `${LENS_BRIEF}

You fix PeriFlow. Worktree ${OV}/wt/WP11 (repo ${PFR}, LOCAL branch overhaul/smooth-glass): never push, never set an upstream, never touch anything outside periflow-android/, never git add -A, never touch periflow-recovery-codes.txt or PeriFlow-Postman/. Read ${OV}/handoff/WP11.md first; its wrappers are in ${OV}/logs/WP11/ (gradle.sh, build-apk.sh, test-and-apk.sh, measure.sh, record.sh, compare.py). An earlier run was stopped midway: its change is committed as 58c17e0 ("Let the tab lens lift and bend the screen as it travels to a tab"). Review that commit against the brief, keep what is right, and finish.
Files: periflow-android/core/designsystem/src/main/kotlin/com/jorwen/periflow/core/designsystem/foundation/glass/LiquidLens.kt, component/navigation/GlassBottomBar.kt and GlassRail.kt, and their tests (LiquidLensTest).
Steps:
1. Finish the implementation. Unit tests must show that travel rises with slot velocity, decays to 0 at rest, and is 0 under reduce motion and below the Live tier, and that the lens samples while travelling and stops after.
2. Run the designsystem tests, then build the devBenchmarkRelease APK with the wrappers. They pass the template daemon's arguments and exclude :app:uploadCrashlyticsMappingFileDevBenchmarkRelease.
3. On the phone, pinned with --extra glass.quality=Live:
   - Record tab taps between distant and adjacent tabs (screenrecord --size 720x1600). Extract frames, confirm the distortion is visible while the lens travels, and save side-by-side strips against the after-early build in ${OV}/perf/WP11/lens-tap/.
   - Run the tab scenario with measure.sh (three runs) and compare tabs.janky_pct, frame p90 and p99, and GPU p90 with ${OV}/perf/WP11/after-early*/ and ${OV}/perf/WP11/gates.toml. Iterate at most three times if a gate fails.
4. Write a "Lens on tap (REQUIREMENTS 35)" section at the top of ${OV}/handoff/WP11.md.
Return state "done" with the numbers in the summary, or "blocked" with evidence.` },
  { key: 'generator', diff: `git -C ${OV}/wt/WP05 log --oneline -12 and the diff of the lens-tap commits on wp/WP05`, prompt: `${LENS_BRIEF}

You fix the generator's lens. Worktree ${OV}/wt/WP05 on branch wp/WP05 (read ${OV}/handoff/WP05.md first). An earlier run was stopped with 6 uncommitted files in this worktree: they are its lens-tap work in progress. Review them, keep what is right and commit. File: templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/LiquidLens.kt, its tests and the lens modifier it feeds. The generator's LiquidTabs bar is not built yet (it waits for WP03 and WP04), so the lens state and modifier must carry the travelling behaviour and LiquidTabs gets it for free. Add a "Lens on tap (REQUIREMENTS 35)" section at the top of the handoff that says exactly how LiquidTabs must wire it, and that the CP2 recordings must include the tap morph.
Steps: implement; unit and Robolectric tests as for PeriFlow; run ${L} gradle ./gradlew :core:designsystem:testDebugUnitTest in ${OV}/wt/WP05/templates/android (in the background, log under ${OV}/logs/lens/, polled); commit. Return state "done" or "blocked" with evidence.` },
]
// A lens fix whose signal is already fired is done; PeriFlow's needs its repository on this machine.
const LENS_TODO = LENS_TREES.filter(t => (PERIFLOW || t.key !== 'periflow') && !signal(`LENS:${t.key}`).done)
const LENS_STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'blocked'] }, summary: { type: 'string' } }, required: ['state', 'summary'] }

async function lensFlow() {
  const out = await pipeline(
    LENS_TODO,
    t => fableAgent(t.prompt, { label: `lens:${t.key}`, phase: 'Implement', schema: LENS_STATUS }),
    async (r, t) => {
      if (!r) throw new Error(`lens ${t.key} failed`)
      let cur = r
      for (let round = 1; round <= 3; round++) {
        const rv = await fableAgent(`You are a fresh-context reviewer. Review ${t.diff} against ${OV}/REQUIREMENTS.md item 35 and the brief below, the G1 audit section in ${OV}/audit/periflow-learnings.md, ${RS}/CODE-STANDARDS.md and the androidx Compose API guidelines.
BLOCKING:
- The distortion does not show on a tap or a programmatic selection.
- Anything outside the lens reads the intensity, or the bar body re-renders.
- Dispersion or inner shadow on the tap path.
- Sampling does not stop after travel.
- Per-frame recomposition, state read in composition that changes per frame, a new coroutine per tap, or allocation in draw.
- The Live-only, reduce-motion or drag behaviour is broken.
- Missing or weak tests.
- Narrating comments or slop.
- Edits outside the allowed paths.
NON-BLOCKING: nits. Do not modify anything; cite file:line.
Brief:
${LENS_BRIEF}`, { label: `review:lens-${t.key}#${round}`, phase: 'Review', schema: REVIEW })
        if (!rv) throw new Error(`lens ${t.key} review failed`)
        if (!rv.blocking.length) { fire(`LENS:${t.key}`); return { key: t.key, state: cur.state, summary: cur.summary } }
        log(`lens ${t.key} review #${round}: ${rv.blocking.length} blocking`)
        cur = await fableAgent(`${t.prompt}\n\nMODE: fix. A fresh-context reviewer found BLOCKING issues in your change. Fix every one (commits with why-messages), re-run the tests${t.key === 'periflow' ? ' and, if drawing or sampling changed, the phone frames and the tab gates' : ''}, update the handoff, and return state "done":\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking: ${JSON.stringify(rv.nonblocking)}`, { label: `lens:${t.key}:fix#${round}`, phase: 'Implement', schema: LENS_STATUS })
        if (!cur) throw new Error(`lens ${t.key} fix failed`)
      }
      fire(`LENS:${t.key}`)
      return { key: t.key, state: cur.state, summary: cur.summary, review: 'rounds exhausted; open findings go to the package review' }
    },
  )
  return out
}

async function packageFlow(id) {
  const p = PKGS[id]
  if (signals[id] && signals[id].done) return { id, state: 'merged earlier' }
  const w = await waitFor(p.start)
  if (w !== 'ok') return { id, state: 'blocked', summary: `${w} never arrived` }
  const r = p.reviewOnly ? { state: 'done', summary: 'implemented and accepted earlier' } : await implementUntilDone(id)
  if (r.state !== 'done') { log(`${id} ended ${r.state}: ${String(r.summary).slice(0, 300)}`); return { id, state: r.state, summary: r.summary } }
  const rv = await reviewLoop(id)
  if (rv.stop) return { id, state: 'blocked', summary: rv.notes.join(' ') }
  if (p.noMerge) { fire(id); return { id, state: 'done-local', summary: r.summary, review: rv.notes } }
  const m = await mergePackage(id, rv.clean ? [] : rv.open)
  return { id, state: m && m.ok ? 'merged' : 'merge-failed', summary: r.summary, review: rv.notes, merge: m && m.details }
}

const GATE = { type: 'object', properties: { go: { type: 'boolean' }, gaps: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['go', 'gaps', 'summary'] }

// A read-only verdict for the user, who approves each deploy themselves.
async function releaseGate(phase, verifyId, scopeText) {
  const w = await waitFor([verifyId])
  if (w !== 'ok') return { phase, go: false, summary: `${w} never arrived` }
  const gate = await fableAgent(`You are the release gate for ${scopeText} of the android-base overhaul. You are adversarial: default to NO-GO when evidence is missing. Do not modify anything.
Check that every requirement in ${OV}/REQUIREMENTS.md that applies to ${scopeText} is met on the integration branch overhaul/multiplatform-v2 (merged tags: git -C ${R} tag -l 'merged/*'). Evidence: ${OV}/report/FINAL.md, ${OV}/handoff/WP13*.md, ${OV}/merges.md, ${OV}/followups.md (must be empty or closed), the perf summaries under ${OV}/perf/, the security audit results, and the code itself where a claim looks thin.
Smoothness (REQUIREMENTS 19, 20), security, adaptive, accessibility, the docs (REQUIREMENTS 25) and the website (REQUIREMENTS 36: only shipped platforms selectable) are all part of it.
Write your verdict to ${OV}/RELEASE-GATE-${phase}.md. Return go=true only if nothing that applies is missing; otherwise list every gap concretely.`, { label: `gate:${phase}`, phase: 'Merge', schema: GATE })
  if (!gate) throw new Error(`release gate ${phase} failed`)
  log(`release gate ${phase}: ${gate.go ? 'GO (awaiting the user to deploy)' : 'NO-GO, ' + gate.gaps.length + ' gaps'}`)
  return { phase, ...gate }
}

async function cp2Flow() {
  const w = await waitFor(['WP05:cp2-recordings'])
  if (w !== 'ok') return `CP2 not decided: ${w}`
  const d = await fableAgent(`You decide checkpoint 2 (glass feel) of the android-base overhaul on the user's behalf; the user asked not to be asked. Read ${DESIGN}/MASTER-PLAN.md B.5 "CP2" and A4.9 and section 2's glass gates, ${OV}/decisions/CP1.md (the format to follow), ${OV}/handoff/WP05.md, ${OV}/REQUIREMENTS.md (the user wants the smoothest, iOS-level result; PeriFlow's glass cost FPS, which this must not repeat), and every recording, frame strip and number under ${OV}/perf/WP05/ (open the frames; judge looks the way an Apple designer would, and judge cost only from release-build numbers). Pick the blur and tint variant per tier and confirm or adjust the tier policy (Live, LiveNoLens, Frosted, Solid: API level, low RAM, power save, thermal, Reduce transparency, the sticky GPU verdict). Write ${OV}/decisions/CP2.md with the values as token entries WP05 can apply, the evidence for each choice, and what would make the user want to override it. Do not modify any repository.`, { label: 'CP2:decide', phase: 'Decide', schema: DECISION })
  if (!d) throw new Error('CP2 decision failed')
  fire('CP2')
  return d.summary
}

const ORDER = ['WIN', 'WP04', 'WP10', 'WP08', 'WP06', 'WP02-M2', 'WP03', 'WP05', 'WP07-A', 'WP11', 'WP02-M3', 'WP14', 'MB', 'QA2', 'WP07-B', 'WP07-C', 'WP12', 'WP13', 'WP12-B', 'WP13-B']
  .filter(id => PERIFLOW || PKGS[id].kind !== 'periflow')
// Without PeriFlow nothing runs WP11, so a wait on it ends as dead:WP11 instead of hanging.
if (!PERIFLOW) endedOf('WP11').r(true)
const lens = lensFlow().catch(e => `lens error: ${e}`).then(res => { addImplSlot(); endedOf('LENS').r(true); return res })
const cp2 = cp2Flow().catch(e => `CP2 error: ${e}`).then(res => { endedOf('CP2').r(true); return res })
const flows = ORDER.map(id => packageFlow(id)
  .catch(e => ({ id, state: 'error', summary: String(e) }))
  .then(res => { endedOf(id).r(true); return res }))
const gateA = releaseGate('android', 'WP13', 'the Android release (the Android template, the generator, the website and the API)').catch(e => ({ phase: 'android', go: false, summary: String(e) }))
const gateB = releaseGate('multiplatform', 'WP13-B', 'phase B (Compose Multiplatform beta and the SwiftUI template, still coming soon)').catch(e => ({ phase: 'multiplatform', go: false, summary: String(e) }))
const results = await Promise.all(flows)
const merged = Object.keys(signals).filter(k => signals[k].done && !k.includes(':'))
return { results, lens: await lens, cp2: await cp2, merged, gateAndroid: await gateA, gateMultiplatform: await gateB }
