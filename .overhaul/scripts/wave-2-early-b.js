export const meta = {
  name: 'android-base-wave-2-early-b',
  description: 'Start the work that does not depend on the running wave 0-1 packages: Compose Multiplatform foundation (WP07-A), PeriFlow-only performance fixes (WP11-early) and the liquid-glass foundation (WP05-early); each reviewed against the Google bar, no merges here',
  phases: [
    { title: 'Implement', detail: 'WP07-A foundation and WP11-early in their own worktrees' },
    { title: 'Review', detail: 'fresh-context review and fix loop' },
  ],
}

const R = '/home/crazyhusir/Documents/projects/tools/android-base'
const PFR = '/home/crazyhusir/Documents/projects/jorwen/periflow'
const OV = '/home/crazyhusir/Documents/projects/tools/.overhaul'
const DESIGN = OV + '/design'
const RS = OV + '/research'
const L = OV + '/bin/locked'
const SKILLS = '/home/crazyhusir/.claude/plugins/marketplaces/android-skills'

const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'waiting', 'blocked'] }, waiting_for: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' }, accept_ok: { type: 'boolean' } }, required: ['state', 'waiting_for', 'summary', 'accept_ok'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }

const COMMON = `
Read first: ${OV}/REQUIREMENTS.md (all; items 17-32 came from the user during the work), ${OV}/notes/INDEX.md and every plan it lists that names your package, ${RS}/ANDROID-SKILLS-DIGEST.md, ${RS}/CODE-STANDARDS.md, your WP file, the MASTER-PLAN sections it names (${DESIGN}/MASTER-PLAN.md is 218 KB: grep a heading, read a line range; always "Part D" and A11.3), and your handoff note if it exists.
Quality bar: Google/AOSP level (REQUIREMENTS 27): androidx Compose API guidelines, Kotlin style guide, deeper well-organised packages, zero AI-slop comments (comment why, never what; no history or narration). Google's Android skills are at ${SKILLS}; read the relevant SKILL.md before working in its area.
The user is away and asked not to be asked anything. Granted: Gradle dependency resolution (incl. org.jetbrains.compose, org.jetbrains.androidx.navigation3, io.insert-koin, io.ktor darwin, androidx.room3, io.github.kyant0, the Kotlin/Native toolchain). Not granted: pushing anything, merging into any shared branch, other downloads.
Gradle only via ${L} gradle, adb only via ${L} adb (phone serial 9a7c74f4, Redmi Note 10 Pro 120 Hz; screenrecord needs --size 720x1600 on it; judge smoothness only on release/benchmark builds). Long commands in the background with logs; read logs with tail/grep. Six other package agents are running (WP02, WP03, WP04, WP06, WP08, WP10), so the locks will be busy: work on code while you wait.
Habits: commit after every completed step with a message that explains why (end every commit message with the line "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"); keep a "## Progress" section at the top of your handoff current; stage explicit paths only.`

const PKGS = {
  'WP05-early': {
    review: `git -C ${R} diff overhaul/multiplatform-v2...wp/WP05`,
    prompt: `You are the implementation agent for the first half of WP05 (optional liquid glass, Kyant backdrop 2.0.1 + shapes 1.2.1) of the android-base overhaul. WP file: ${DESIGN}/wp/WP05.md; master plan A4.9 and §2 glass gates; ${OV}/handoff/WP05-preread.md; audits ${OV}/audit/periflow-learnings.md (G1-G17, the PeriFlow costs to avoid), ${OV}/audit/research.md (Kyant source facts: blur needs API 31, lens API 33, below 31 an unblurred copy, lens throws on non-rounded shapes on 33+, sampling from inside the recorded scene crashes the RenderThread) and ${RS}/SMOOTHNESS-PLAN.md / MOTION-PLAN.md items naming WP05.
Worktree: ${OV}/wt/WP05 on branch wp/WP05 (create it if missing: git -C ${R} worktree add ${OV}/wt/WP05 -b wp/WP05 overhaul/multiplatform-v2). WP03 (design system) and WP04 (shell, AppTabBar) are NOT merged yet; they are being finished in ${OV}/wt/WP03 and ${OV}/wt/WP04 — read them, do not edit them.
SCOPE OF THIS RUN: the self-contained glass foundation inside your owned paths (templates/android/core/designsystem/.../foundation/glass/**): the GlassQuality policy and tiers (Live, LiveNoLens, Frosted, Solid from API level, isLowRamDevice, power save, thermal status, a user "Reduce transparency" preference, a sticky FrameMetrics GPU-time verdict persisted per fingerprint and version), a SceneBackdrop that records the scene once and is sampled only by chrome, drawn content glass as a Modifier.Node with drawWithCache brushes and a cached outer-only shadow (Compose 1.12 dropShadow where it fits), frosted without any sampling, a DstIn scroll-edge fade that works on every API, static effect chains, every glass Animatable with a visibility threshold, the catalog-independent unit and Robolectric tests, and the liquidglass feature TOML (behind <opt:liquidglass>) with the Kyant catalogue entries. Design the integration points against WP04's AppTabBar and ShellChrome seam and WP03's tokens as they exist on those branches. When the foundation is done, tested and committed, write ${OV}/handoff/WP05.md with exactly what remains (LiquidTabs on the bar and rail, the shell stage behind <opt:liquidglass>, catalog Glass section, glass gates on the phone, CP2 recordings) and return state="waiting" with waiting_for ["WP03", "WP04"].`,
  },
  'WP07-A': {
    review: `git -C ${R} diff overhaul/multiplatform-v2...wp/WP07`,
    prompt: `You are the implementation agent for WP07 phase A (Compose Multiplatform foundation, Android + iOS) of the android-base overhaul. WP file: ${DESIGN}/wp/WP07.md (Phase A section). Master plan A7, A9, A12.3-A12.4.
Worktree: ${OV}/wt/WP07 on branch wp/WP07 (create it if missing: git -C ${R} worktree add ${OV}/wt/WP07 -b wp/WP07 overhaul/multiplatform-v2). Merged so far: WP01 (generator engine v2 and tokens), WP02-M1 (pure :core:common, :core:mvi), WP09. WP04 (the Android navigation shell) is NOT merged yet: it is being finished in ${OV}/wt/WP04 and merges later.
SCOPE OF THIS RUN: Phase A steps 1, 2, 3 (every module except the core/navigation shell port), 5, 6 (sync check for core/common and core/mvi), 7 and 8 (tests except the shell tests), plus the multiplatform items in ADAPTIVE-PLAN.md, MOTION-PLAN.md and STANDARDS-PLAN.md that belong to phase A. For core/navigation, build only what the reference screens need to run without the WP04 shell, behind a clearly named seam the shell port will replace; do not invent a second shell design. Try the iOS klib compile and record the result. When everything except the shell port and the acceptance steps that need it is done and committed, update ${OV}/handoff/WP07-A.md with exactly what remains, and return state="waiting" with waiting_for ["WP04"]. Return "done" only if the whole Phase A acceptance passes.`,
  },
  'WP11-early': {
    review: `git -C ${PFR} diff main...overhaul/smooth-glass -- periflow-android`,
    prompt: `You are the implementation agent for the first half of WP11 (PeriFlow performance fixes) of the android-base overhaul. WP file: ${DESIGN}/wp/WP11.md. Audit: ${OV}/audit/periflow-learnings.md (G1-G17, N1-N11) and ${OV}/audit/device-baseline.md. PeriFlow repo: ${PFR} (app in periflow-android/). Follow the WP file's Setup exactly: worktree ${OV}/wt/WP11 on the LOCAL branch overhaul/smooth-glass created from main, ignored build files symlinked, never copied or staged; never push, never set an upstream, never touch anything outside periflow-android/.
SCOPE OF THIS RUN (things that do not need the new generator template, which is still being built):
- Step 1: the baseline measurement on the phone into ${OV}/perf/WP11/before/.
- Step 6: the PeriFlow-specific costs (G4, G5, G7).
- Step 7: the devtools badge drag fix.
- Step 8: the Play update prompt.
- Step 9: comments.
- From step 5, only the PeriFlow-local glass fixes that do not need ports from the generator: G1 (no press swell or bar-body scale on tap; lens only while pressed or dragged), G2/G3 (the bar stays composed for the shell's lifetime; no fade over a backdrop), G6/G13 (static effect chains; no per-event or per-frame coroutine launches; remembered items), G9/G10 (drawn glass as a Modifier.Node with cached brushes and a cached outer shadow), G11 (scroll-edge fade on every API), G12 (frosted without sampling), G16 (dead glass code removed), G17 (quality tiers: API level, low-RAM, power save, thermal, "Reduce transparency", sticky FrameMetrics GPU verdict).
- Step 10: tests for these.
- Step 11: re-measure into ${OV}/perf/WP11/after-early/ with three runs, iterating at most three times per failing scenario.
The machine has 15 GB and the template's Gradle daemon is already resident, so PeriFlow must reuse it rather than start a second one: run every PeriFlow Gradle command with the template's exact daemon arguments on the command line (do not edit PeriFlow's gradle.properties for this): ${L} gradle ./gradlew -Dorg.gradle.jvmargs='-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8' -Pkotlin.daemon.jvmargs='-Xmx2g -XX:+UseParallelGC' <tasks>. If a build runs out of memory at 4 GB, report it rather than raising the heap. Leave steps 2-4 and the generator ports in step 5 (LiquidTabs, SceneBackdrop, KeptTabs v2, BackGate, sheets, insets) for the second half: list them in ${OV}/handoff/WP11.md and return state="waiting" with waiting_for ["WP04", "WP05", "WP06"] once this scope is done, committed and measured.`,
  },
}

async function run(id) {
  const p = PKGS[id]
  let r = await agent(p.prompt + COMMON, { label: `${id}:start`, phase: 'Implement', schema: STATUS })
  if (!r) throw new Error(`${id} agent failed`)
  for (let round = 1; round <= 2; round++) {
    const rv = await agent(`You are a fresh-context code reviewer for ${id} of the android-base overhaul, holding it to the Google/AOSP bar (REQUIREMENTS 27). Review: ${p.review} (start with --stat; read every changed source file). Judge against ${RS}/CODE-STANDARDS.md, ${RS}/STANDARDS-PLAN.md, ${RS}/ANDROID-SKILLS-DIGEST.md, ${DESIGN}/MASTER-PLAN.md "Part D" and A11.3, the WP file, ${OV}/REQUIREMENTS.md, and the plans in ${OV}/notes/INDEX.md naming the package. BLOCKING: correctness bugs; perf hazards (animated state read in composition, alpha over full-screen or backdrop layers, per-event coroutine launches, infinite animations without visibility/reduce-motion handling, allocations in draw, recomposition per frame); API-guideline violations; narrating or history comments, AI slop, commented-out code, needless abstraction; dead code; missing or weakened tests; edits outside the package's scope; secrets; placeholders presented as finished. NON-BLOCKING: nits. Do not modify anything; cite file and line.`, { label: `review:${id}#${round}`, phase: 'Review', schema: REVIEW })
    if (!rv || !rv.blocking.length) return { id, state: r.state, summary: r.summary, review: rv ? rv.nonblocking : ['review failed'] }
    const fx = await agent(`${p.prompt}${COMMON}\n\nMODE: fix. A fresh-context reviewer found BLOCKING issues in your branch. Fix every one (commits with why-messages), re-run the affected checks, update the handoff, and return the same state as before ("${r.state}" with the same waiting_for if work remains):\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking (address cheaply or record): ${JSON.stringify(rv.nonblocking)}`, { label: `${id}:fix#${round}`, phase: 'Implement', schema: STATUS })
    if (!fx) throw new Error(`${id} fix failed`)
    r = fx
  }
  return { id, state: r.state, summary: r.summary, review: ['review rounds exhausted; remaining findings recorded in the handoff'] }
}

const results = await Promise.all(Object.keys(PKGS).map(id => run(id).catch(e => ({ id, state: 'error', summary: String(e) }))))
return results
