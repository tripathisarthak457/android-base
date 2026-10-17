export const meta = {
  name: 'deep-smoothness-fable',
  description: 'One deep-work agent on Fable: root-cause and fix the remaining jank to iOS level on the Redmi (side branch wp/WP04-perf), with an Opus Google-bar review loop and before/after numbers',
  phases: [
    { title: 'Deep work', detail: 'Fable: traces, root causes, fixes, measurements' },
    { title: 'Review', detail: 'Opus fresh-context review, Fable fixes' },
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

const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = OV + '/bin/locked'
const WT = OV + '/wt/WP04-perf'
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
// The android-profiler skill's trace_processor caches its binary under the user profile; on this machine it was
// pre-fetched into D:/.caches/perfetto (2026-10-17), so it must run with USERPROFILE pointed there.
const PERFETTO = `Perfetto: keep traces under ${OV}/perf/WP04-perf/. trace_processor v57.2 is pre-fetched: run it only as USERPROFILE='D:\\.caches\\perfetto' python ${SKILLS}/profilers/android-profiler/references/perfetto/bin/trace_processor <args>; without that override it downloads itself again into the user profile.`

const BRIEF = `You are the deep-work smoothness engineer for the android-base overhaul. The user's non-negotiable goal (${OV}/REQUIREMENTS.md items 19, 20, 21, 22 and 36) is iOS-level smoothness and the lowest achievable jank on their Redmi Note 10 Pro (serial 9a7c74f4, Snapdragon 732G, 120 Hz) without giving up any animation. The Android release cannot ship until it holds.
Where it stands (read in this order):
- ${OV}/handoff/WP04-perf.md and ${OV}/FABLE-FINDINGS.md: round 1 is merged into wp/WP04 (730f8e0), and WP04 committed the KeptTabs change as fb866b3 (the handoff's §2 still lists it as uncommitted). The 4 uncommitted feature/sample/.../detail files in your worktree are WP02-M3's item 1 (handoff/WP02-M3.md:17-21; WP04 left them there, handoff/WP04.md run 18): do not commit them on wp/WP04-perf. Save them as ${OV}/out/WP04-perf/sample-detail-related-after-settle.patch (git diff), add a pointer to it in handoff/WP02-M3.md, then restore those 4 files in the worktree, and correct the handoff's §2. FABLE-FINDINGS §1 says the measurement finished on Oct 16; it did not (${OV}/out/WP04-perf/logs/measure2.log: the HEAD build ran 2 of 3 runs, then perf.sh exited 2 when uiautomator could not dump the screen; the fixed and floor builds were never measured).
- ${OV}/STATUS.md §4 (old template vs the new build);
- ${OV}/handoff/WP04-RESUME.md and the Progress and Measurements sections of ${OV}/handoff/WP04.md;
- every ${OV}/perf/WP04/*/summary.md (phase2, phase3*, cp1, tab-switch, back-during-push, predictive-back);
- ${OV}/research/SMOOTHNESS-PLAN.md, RECOMPOSITION-PLAN.md and compose-perf.md;
- ${OV}/docs-draft/SMOOTHNESS.md;
- the android-profiler skill at ${SKILLS} (Perfetto recording and trace_processor SQL).
- Google's new jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md): run its exhaustive seven-category audit over the shell, stack, tab bar and sample screens, and read the matching references/*.md before every fix (the user, Oct 16).

First, finish the measurement round 1 planned and never completed: ${OV}/out/WP04-perf/everything (built at 3c9c403, before the fixes) against fixed (round 1, now in wp/WP04 as 730f8e0) and floor, three runs each in one adb hold (${OV}/out/WP04-perf/measure.sh and the build scripts next to it; port them to Windows first). KeptTabs (fb866b3) has no build yet: build the current head as well and measure it the same way. The old builds' outputs were not carried over to this machine, so rebuild each from its commit. Its numbers decide WP04's perf escalation (the first frame after idle on this phone's idle clocks), and relaxing a gate is the user's call: if the fixed build is near the floor, write the numbers and a recommendation at the top of the handoff and in your summary (gate what users see: the hitch ratio and late first frames, with the old rows kept as ratchets), and never change gates.toml yourself.
Known open gaps, measured at phase 2 (later numbers are in the "Measurements" of ${OV}/handoff/WP04.md; verify them at HEAD):
- tab switch: 1.9% janky, worse than the old template's 1.58%;
- push/pop: p99 57 ms (a cold pop), first frame of a pop 15/37 ms (median/max);
- rapid back: 2.62% janky; back gesture: 2.82%;
- cold start: 355 ms against 336 ms (baseline profiles were just added);
- PSS: 93.8 MB against a 70 MB target.
Targets: about 0% janky on tab switches, push/pop, back and scroll; first frames within one 8.33 ms vsync where achievable; p99 at or under 16 ms; cold start below 336 ms.

Worktree: ${WT} on branch wp/WP04-perf. If missing: git -C ${R} worktree add ${WT} -b wp/WP04-perf wp/WP04. Work only there (the WP04 agent keeps working in ${OV}/wt/WP04; never edit that worktree). If wp/WP04 moves on, merge it into your branch.
Method, per gap:
1. Measure at HEAD on a release or benchmark build with the template's perf tooling (templates/android/tools/perf/perf.sh, gates.toml): three runs, through ${L} adb.
2. Record a Perfetto trace of the worst case and analyse it (the Perfetto rule below says with what). Find the real cause: composition, layout, draw, RenderThread or GPU, binder, GC, JIT or class loading, shader compilation, IO on the main thread.
3. Fix the cause at the Google/AOSP bar (${OV}/research/CODE-STANDARDS.md, the androidx Compose API guidelines; comment why, never what).
4. Re-measure the same way.
Keep every animation (REQUIREMENTS 21): never remove an effect to save cost; make it cheap instead (the lens lesson, REQUIREMENTS 35). Commit each fix with a why-message ending in "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", and stage explicit paths only.
Rules:
- Gradle only via ${L} gradle; adb only via ${L} adb (screenrecord needs --size 720x1600 on this phone).
- Windows, native (the user's machine since Oct 16): your Bash tool is Git Bash. ${L} sets JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME, which your shell lacks, so never run gradlew or adb bare; never pass -Dorg.gradle.jvmargs (the heap comes from D:/.caches/gradle/gradle.properties). Set MSYS_NO_PATHCONV=1 for adb device paths (/sdcard/...). Helper scripts under ${OV}/out were written on Linux: port their paths (/home/crazyhusir/Documents/projects/tools is D:/projects/tools) and Linux-only commands before re-running them. ${PERFETTO}
- There is no emulator on this machine.
- Never kill or stop processes you did not start; never run ./gradlew --stop.
- Restore any device setting you change.
- If the phone is missing, do code and trace analysis first and poll adb devices in the background for up to 45 minutes.
Write ${OV}/handoff/WP04-perf.md as you go: each gap with before and after numbers (with the paths of the summaries and traces), the root cause, the fix and its commit, plus what is still open and why. End it with the line "WP04: merge wp/WP04-perf into wp/WP04 (git merge wp/WP04-perf) at the start of your next run; the orchestrator verified it". Save it after every gap.`

const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'partial', 'blocked'] }, summary: { type: 'string' }, before_after: { type: 'string' } }, required: ['state', 'summary', 'before_after'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Deep work')
let r = await fableAgent(BRIEF + '\n\nReturn state "done" when every gap meets its target or has a proven, documented limit; "partial" with what remains otherwise.', { label: 'fable:smoothness', phase: 'Deep work', schema: STATUS })
if (!r) throw new Error('deep work failed')

phase('Review')
for (let round = 1; round <= 3; round++) {
  const rv = await fableAgent(`You are a fresh-context reviewer holding a performance change set to the Google/AOSP bar. Review git -C ${R} diff wp/WP04...wp/WP04-perf (start with --stat; read every changed file) together with ${OV}/handoff/WP04-perf.md and the perf summaries it cites. Check that every claimed improvement is backed by release-build measurements (three runs), and that the root-cause reasoning matches the evidence. ${PERFETTO}
BLOCKING:
- correctness bugs, or behaviour changes that the handoff does not state;
- an animation or effect removed or weakened to save cost (REQUIREMENTS 21 and 35);
- per-frame recomposition, state reads in composition that change per frame, allocation in draw, or alpha over full-screen layers;
- unproven numbers;
- API guideline violations;
- narrating comments or slop;
- missing tests for changed logic.
NON-BLOCKING: nits. Do not modify anything; cite file:line.`, { label: `review:perf#${round}`, phase: 'Review', schema: REVIEW })
  if (!rv) throw new Error('review failed')
  if (!rv.blocking.length) { log(`perf review #${round}: clean`); break }
  log(`perf review #${round}: ${rv.blocking.length} blocking`)
  r = await fableAgent(`${BRIEF}\n\nMODE: fix. A fresh-context reviewer found BLOCKING issues in wp/WP04-perf. Fix every one (commits with why-messages), re-measure what the fixes touch, update ${OV}/handoff/WP04-perf.md, and return the same schema:\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking: ${JSON.stringify(rv.nonblocking)}`, { label: `fable:fix#${round}`, phase: 'Review', schema: STATUS })
  if (!r) throw new Error('fix failed')
}
return r
