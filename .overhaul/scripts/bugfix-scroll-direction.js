export const meta = {
  name: 'bugfix-scroll-direction',
  description: 'Fix the bug the user reported on Oct 18: a right-to-left swipe on a carousel and an upward list scroll move the opposite way. Reproduce on the phone, find the root cause, fix it with regression tests, review, verify on the phone',
  phases: [
    { title: 'Fix', detail: 'reproduce, root cause, fix with tests' },
    { title: 'Review', detail: 'fresh-context review, then fixes' },
  ],
}

// The user, Oct 18 about 16:50: "i was checking carousel components when i swiping right to left to go on the last card
// its doing opposite movement and same happened in a list i was scrolllung up and it was going down or something find the
// location and fix it in the upcoming session (priority)". Their standing rule: a bug they report comes first, so this
// runs alone before the other workflows resume.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const DESIGN = OV + '/design'
const RS = OV + '/research'
const L = OV + '/bin/locked'
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
const BUG = OV + '/bugs/BUG-2026-10-18-scroll-direction.md'

const ORCH = `
Orchestrator rules (they add to the WP file and the master plan; REQUIREMENTS.md wins on conflict):
- Today is 2026-10-18. The user is not to be asked anything: decide soundly and record decisions in your handoff. Granted: npm ci in web/frontend (through ${OV}/bin/memwait); Gradle dependency resolution (incl. androidx.room3, io.insert-koin, io.ktor darwin, io.github.kyant0, org.jetbrains.androidx.navigation3, org.jetbrains.compose, the Kotlin/Native toolchain). Not granted: pushing anything, merging into main, installing anything, other downloads. There is no Docker on this machine: the Postgres store tests and go test -race run in CI only.
- QUALITY BAR (REQUIREMENTS 27): Google/AOSP-level code. ${RS}/CODE-STANDARDS.md, ${RS}/STANDARDS-PLAN.md, the androidx Compose API guidelines, and Google's Android skills at ${SKILLS} (read the relevant SKILL.md before working in its area; digest at ${RS}/ANDROID-SKILLS-DIGEST.md). For any Compose UI change, follow Google's jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md): run its exhaustive audit over the composables you touch and read the matching references/*.md before each fix (the user, Oct 16). Zero AI-slop comments: comment why, never what; no history or narration.
- Which packages are merged: git -C ${R} tag -l 'merged/*'. Decisions: ${OV}/decisions/CP1.md and CP2.md (CP2 exists only once decided).
- Gradle only via ${L} gradle; adb only via ${L} adb; long commands in the background with logs, polled with tail/grep. The Gradle lock is shared by every agent, so work on code while you wait. Memory safety (REQUIREMENTS "Memory safety"): never raise the Gradle heap. Never kill, stop or signal processes you did not start in this run (no taskkill, no Stop-Process), and never run ./gradlew --stop: a stale build from an earlier run finishes on its own.
- Windows (the user's machine since Oct 16; native, no WSL): your Bash tool is Git Bash. ${L} and ${OV}/bin/memwait set JAVA_HOME, ANDROID_HOME (C:/Users/crazyhusir/AppData/Local/Android/Sdk), GRADLE_USER_HOME (D:/.caches/gradle) and the Go, pip and npm cache variables, which your shell lacks: run gradlew, adb, go, npm and pip only through them, never bare (a bare run creates forbidden cache folders under C:/Users). Gradle's heap comes from the global D:/.caches/gradle/gradle.properties: never pass -Dorg.gradle.jvmargs or kotlin.daemon.jvmargs. Set MSYS_NO_PATHCONV=1 for adb device paths (/sdcard/...) and for git show <ref>:.<path>; with it set, give git D:/ paths, not /d/. Helper scripts under ${OV}/out and ${OV}/logs were written on Linux: before re-running one, port its paths (/home/crazyhusir/Documents/projects/tools is D:/projects/tools) and its Linux-only commands (flock, /proc, /sys). Never write under %APPDATA% and never pip install outside a venv. Generator unit tests: until package WIN merges, 6 of them fail on this machine with no change at all (${OV}/logs/windows/generator-baseline.md); judge your work and every merge by failures beyond those. Perfetto's trace_processor v57.2 is pre-fetched under D:/.caches/perfetto: run it only as USERPROFILE='D:\\.caches\\perfetto' python ${SKILLS}/profilers/android-profiler/references/perfetto/bin/trace_processor <args> (without that override it downloads itself again into the user profile). ffmpeg and ffprobe 9.0.1 are installed and on PATH inside ${L} and ${OV}/bin/memwait. Never install anything yourself.
- Phone: serial 9a7c74f4 (Redmi Note 10 Pro, 120 Hz). screenrecord needs --size 720x1600. Judge smoothness only on release or benchmark builds. Restore any device setting you change for a test. There is no emulator on this machine: if the phone is missing (${L} adb adb devices), do every code and test step first, poll for it in the background for up to 45 minutes, then record in the handoff which phone steps remain.
- Commit after every completed step with a message that explains why, ending with the line "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>". Stage explicit paths only.`
const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'waiting', 'blocked'] }, waiting_for: { type: 'array', items: { type: 'string' } }, milestones: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' }, accept_ok: { type: 'boolean' } }, required: ['state', 'waiting_for', 'milestones', 'summary', 'accept_ok'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }

const BRIEF = `You fix a bug the user reported, which comes before everything else. Read ${BUG} first (the user's words, what is known, the suspects), then ${OV}/REQUIREMENTS.md items 21 and 41 (iOS scroll physics), ${RS}/ios-motion.md (the rubber band and decay constants) and the handoffs of the packages that touch scrolling: ${OV}/handoff/WP03.md, ${OV}/handoff/WP04.md, ${OV}/handoff/WP04-perf.md. The other workflows are paused while you work, so the worktrees under ${OV}/wt/ are yours; commit on the branch that owns the code you change, never on main, and never push.
1. Find the build the user was using: through ${L} adb, list the com.example.* packages with their lastUpdateTime and firstInstallTime (dumpsys package), find the ones with carousel and list screens, and map each to its branch and commit (the build folders under ${OV}/out/ and the logs say which branch built them).
2. Reproduce on the phone, release-like builds where possible: adb input swipe right-to-left on the carousel towards the last card, slow and fast; drag and fling a list upwards and downwards, in the middle and at both edges; repeat with the layout direction forced to RTL (and restore it). Record with screenrecord --size 720x1600 and extract frames to see which way content moves. Save evidence under ${OV}/qa/bug-scroll-direction/. If it does not reproduce, check every scrolling surface in the catalog and the template the same way before concluding, and say exactly what was tried.
3. Find the root cause in the code (a sign error in a drag delta, fling velocity, decay, overscroll or rubber-band consumption, a nested-scroll hand-over, a pager or carousel snap target, or RTL mirroring), check whether the same mistake exists on other branches (wp/WP03, wp/WP04, wp/WP04-perf, wp/WP05, wp/HUD, the integration branch), and fix it at the owner. When the owner is another package's branch than the one the user ran, fix it there and note in both handoffs which branches carry the fix and which still need it after the next merge.
4. Regression tests that fail before the fix and pass after: direction and sign of drag, fling, decay and overscroll for horizontal and vertical surfaces, in LTR and RTL, plus the carousel's next and previous card target. Run the affected module tests through ${L} gradle.
5. Rebuild the app the user ran, install it, and verify on the phone with recordings that every case from step 2 now moves the right way.
6. Write the result in ${BUG} ("## Result": the build, the root cause with file and line, the fix commits, the tests, the before/after evidence) and a short note at the top of the owning handoff's "## Progress".
${ORCH}
Return state="done" when fixed and verified on the phone (state "blocked" only with evidence, e.g. the phone is missing), with the summary naming the root cause, the branches and the commits.`

phase('Fix')
let r = await agent(BRIEF, { label: 'bugfix:run', phase: 'Fix', schema: STATUS })
if (!r) throw new Error('bugfix agent failed')
if (r.state !== 'done') return { state: r.state, summary: r.summary }

phase('Review')
let clean = false
for (let round = 1; round <= 3 && !clean; round++) {
  const rv = await agent(`You are a fresh-context code reviewer. Review the fix for the bug in ${BUG} (read its "## Result" for the branches and commits; review every listed commit with git -C ${R} show). Judge: is the root cause really found and fixed in every place it occurs; do the regression tests fail without the fix and cover horizontal and vertical surfaces, drag, fling, decay and overscroll, LTR and RTL, and the carousel's card targets; is there phone evidence of the fix; is the code to the Google/AOSP bar (${RS}/CODE-STANDARDS.md, the androidx Compose API guidelines, Google's jetpack-compose-performance skill at ${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md) with no per-frame allocation or recomposition, no narrating comments or slop. BLOCKING: anything that leaves the bug or an instance of it, a missing or weak test, missing phone evidence, a perf hazard, slop. Do not modify anything; cite file and line.`, { label: `review:bugfix#${round}`, phase: 'Review', schema: REVIEW })
  if (!rv) throw new Error('bugfix review failed')
  if (!rv.blocking.length) { clean = true; break }
  log(`bugfix review #${round}: ${rv.blocking.length} blocking`)
  r = await agent(`${BRIEF}\n\nMODE: fix. A fresh-context reviewer found BLOCKING issues in the fix. Fix every one (commits with why-messages), re-run the tests and the phone check, update ${BUG}, and return state="done":\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking: ${JSON.stringify(rv.nonblocking)}`, { label: `bugfix:fix#${round}`, phase: 'Fix', schema: STATUS })
  if (!r || r.state !== 'done') return { state: r ? r.state : 'error', summary: r ? r.summary : 'fix agent failed' }
}
return { state: r.state, summary: r.summary, reviewClean: clean }
