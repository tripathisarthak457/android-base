export const meta = {
  name: 'hud-first',
  description: 'Build REQUIREMENTS 40 first, as the user asked: the live FPS and performance overlay with on-demand graphs, the performance log and the optimisation score in dev and debug builds, with a Google-bar review loop and a phone demo the user can look at',
  phases: [
    { title: 'Implement', detail: 'the overlay, graphs, log, export and score on wp/HUD' },
    { title: 'Review', detail: 'fresh-context Google/AOSP-bar review, then fixes' },
    { title: 'Show', detail: 'phone build, overhead numbers, screenshots and a recording for the user' },
  ],
}

// The user, Oct 18 14:20: "can u start with tha fps and performance rwquirement first i wanna see it first include graphs
// and all but make sure logging such things dont drop the actual performance of apps draw and render logs graphically when
// user rwquets". REQUIREMENTS 40 is the specification. The release workflow's HUD package merges this branch later (after
// WP06 and WP04 merge); this workflow builds it now on wp/HUD, branched from wp/WP06, which holds the DEV badge, the dev
// panel and the JankStats recorder it extends.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const DESIGN = OV + '/design'
const RS = OV + '/research'
const L = OV + '/bin/locked'
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
const WT = OV + '/wt/HUD'

const ORCH = `
Orchestrator rules (they add to the WP file and the master plan; REQUIREMENTS.md wins on conflict):
- Today is 2026-10-18. The user asked to see this feature first and is not to be asked anything: decide soundly and record decisions in your handoff. Granted: npm ci in web/frontend (through ${OV}/bin/memwait); Gradle dependency resolution (incl. androidx.room3, io.insert-koin, io.ktor darwin, io.github.kyant0, org.jetbrains.androidx.navigation3, org.jetbrains.compose, the Kotlin/Native toolchain). Not granted: pushing anything, merging into main, installing anything, other downloads. There is no Docker on this machine: the Postgres store tests and go test -race run in CI only.
- QUALITY BAR (REQUIREMENTS 27): Google/AOSP-level code. ${RS}/CODE-STANDARDS.md, ${RS}/STANDARDS-PLAN.md, the androidx Compose API guidelines, and Google's Android skills at ${SKILLS} (read the relevant SKILL.md before working in its area; digest at ${RS}/ANDROID-SKILLS-DIGEST.md). For any Compose UI change, follow Google's jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md): run its exhaustive audit over the composables you touch and read the matching references/*.md before each fix (the user, Oct 16). Zero AI-slop comments: comment why, never what; no history or narration.
- Which packages are merged: git -C ${R} tag -l 'merged/*'. Decisions: ${OV}/decisions/CP1.md and CP2.md (CP2 exists only once decided).
- Gradle only via ${L} gradle; adb only via ${L} adb; long commands in the background with logs, polled with tail/grep. The Gradle lock is shared by every agent, so work on code while you wait. Memory safety (REQUIREMENTS "Memory safety"): never raise the Gradle heap. Never kill, stop or signal processes you did not start in this run (no taskkill, no Stop-Process), and never run ./gradlew --stop: a stale build from an earlier run finishes on its own.
- Windows (the user's machine since Oct 16; native, no WSL): your Bash tool is Git Bash. ${L} and ${OV}/bin/memwait set JAVA_HOME, ANDROID_HOME (C:/Users/crazyhusir/AppData/Local/Android/Sdk), GRADLE_USER_HOME (D:/.caches/gradle) and the Go, pip and npm cache variables, which your shell lacks: run gradlew, adb, go, npm and pip only through them, never bare (a bare run creates forbidden cache folders under C:/Users). Gradle's heap comes from the global D:/.caches/gradle/gradle.properties: never pass -Dorg.gradle.jvmargs or kotlin.daemon.jvmargs. Set MSYS_NO_PATHCONV=1 for adb device paths (/sdcard/...) and for git show <ref>:.<path>; with it set, give git D:/ paths, not /d/. Helper scripts under ${OV}/out and ${OV}/logs were written on Linux: before re-running one, port its paths (/home/crazyhusir/Documents/projects/tools is D:/projects/tools) and its Linux-only commands (flock, /proc, /sys). Never write under %APPDATA% and never pip install outside a venv. Generator unit tests: until package WIN merges, 6 of them fail on this machine with no change at all (${OV}/logs/windows/generator-baseline.md); judge your work and every merge by failures beyond those. Perfetto's trace_processor v57.2 is pre-fetched under D:/.caches/perfetto: run it only as USERPROFILE='D:\\.caches\\perfetto' python ${SKILLS}/profilers/android-profiler/references/perfetto/bin/trace_processor <args> (without that override it downloads itself again into the user profile). ffmpeg and ffprobe 9.0.1 are installed and on PATH inside ${L} and ${OV}/bin/memwait. Never install anything yourself.
- Phone: serial 9a7c74f4 (Redmi Note 10 Pro, 120 Hz). screenrecord needs --size 720x1600. Judge smoothness only on release or benchmark builds. Restore any device setting you change for a test. There is no emulator on this machine: if the phone is missing (${L} adb adb devices), do every code and test step first, poll for it in the background for up to 45 minutes, then record in the handoff which phone steps remain.
- Commit after every completed step with a message that explains why, ending with the line "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>". Stage explicit paths only.`
const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'waiting', 'blocked'] }, waiting_for: { type: 'array', items: { type: 'string' } }, milestones: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' }, accept_ok: { type: 'boolean' } }, required: ['state', 'waiting_for', 'milestones', 'summary', 'accept_ok'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }

const SCOPE = `Package HUD: REQUIREMENTS 40, exactly as ${OV}/REQUIREMENTS.md item 40 specifies (read it, and items 19, 25, 27 and 38). Worktree ${WT} on branch wp/HUD; create it if missing with git -C ${R} worktree add ${WT} -b wp/HUD wp/WP06 (WP06 owns :core:devtools and :core:analytics and is accepted but not merged yet). If wp/WP06 has moved since, merge it in. Read ${OV}/handoff/WP06.md (devtools, the DEV badge, the dev panel, FrameJankRecorder), ${OV}/handoff/WP04.md (perf.sh, gates.toml, the measuring rules) and your handoff ${OV}/handoff/HUD.md if it exists (an earlier run may have been cut off: reconstruct from it, git log and git status, keep what is right).
What the user asked for, in their words: FPS and smoothness stats beside the DEV badge so developers see how their app behaves; graphs; detailed logs and an optimisation score; logging must not drop the app's real performance; the logs are drawn and rendered as graphs only when the user asks.
Build:
1. Collection that costs almost nothing: JankStats/FrameMetrics frame data (extend the existing FrameJankRecorder rather than adding a second listener), the current route as JankStats state, written into fixed-size primitive ring buffers off the main thread (no allocation per frame, no boxing, no per-frame logging); memory, GC, start-up and network numbers sampled at a low rate. Hidden overlay and closed panel: no frame listener at all unless the developer turned recording on.
2. The pill beside the DEV badge: current FPS at the display's real refresh rate and janky %, refreshed a few times a second through draw-phase reads (no recomposition per frame).
3. The Performance tab in the existing dev panel, drawn only while it is open (the "render graphically when the user requests" rule): a frame-time graph against the frame budget with the 60/90/120 Hz budget lines, an FPS-over-time graph, a frame-time histogram, janky % per screen as bars, memory over time, slow and frozen frame counts, p50/p90/p99, cold and warm start, network timings. Canvas drawing in the draw phase from the ring buffers, no per-frame allocation; it follows AppTheme and the template's rules (Compose Foundation/UI only, no Material; strings in resources).
4. The structured log (one logcat tag, a summary every few seconds and per screen, never per frame) and the export: a JSON plus Markdown report from a panel action, and a debug-only, non-exported adb path (android-intent-security rules).
5. The 0-100 optimisation score with a grade per metric against the template's targets (REQUIREMENTS 19, gates.toml) and the build checks (R8, baseline profile installed via ProfileInstaller, debuggable, StrictMode), each lost point explained with the fix; debug builds say plainly that their numbers are pessimistic and point to devRelease.
6. A generator feature (on by default, removable): spec entry, markers, catalogue and the emitted website catalogue; absent from every prod release APK (a test plus APK inspection).
Proof: unit tests (ring buffers, stats, score, per-route attribution); the template builds and its tests pass; on the phone with release-like builds (devRelease of the everything preset, application id com.example.hud.everything), overlay shown vs hidden through perf.sh: at most +0.3 percentage points of jank and under 0.5 ms per frame; hidden adds no frame listener. Record numbers in ${OV}/perf/HUD/ and the handoff ("Measurements", and "Rules and gotchas the docs must state" for WP12).
${ORCH}
DONE: state="done" only when all of the above passes and git status is clean; "blocked" only with evidence. waiting_for stays empty (nothing here waits on another package).`

async function implement(label, extra) {
  const r = await agent(`You are the implementation agent for package HUD of the android-base overhaul (${R}). Run: ${label}.
${SCOPE}
${extra || ''}`, { label: `HUD:${label}`, phase: 'Implement', schema: STATUS })
  if (!r) throw new Error(`HUD agent failed (${label})`)
  return r
}

phase('Implement')
let r = await implement('run')
if (r.state !== 'done') return { state: r.state, summary: r.summary }

phase('Review')
let clean = false
let open = []
for (let round = 1; round <= 4 && !clean; round++) {
  const rv = await agent(`You are a fresh-context code reviewer for package HUD of the android-base overhaul, holding it to the Google/AOSP bar (REQUIREMENTS 27).
Review: git -C ${R} diff wp/WP06...wp/HUD (start with --stat; read every changed source file).
Judge against: ${OV}/REQUIREMENTS.md item 40 (and 19, 25, 38), ${RS}/CODE-STANDARDS.md, the androidx Compose API guidelines, Google's jetpack-compose-performance skill (${SKILLS}/jetpack-compose/jetpack-compose-performance/SKILL.md, all seven categories), the android-intent-security skill for the adb export path, and ${OV}/handoff/HUD.md.
BLOCKING: anything that costs the app frames when the overlay is hidden; per-frame allocation, boxing, logging or recomposition; graphs drawn while the panel is closed; a second frame listener where FrameJankRecorder could serve; any trace of the feature in a prod release APK; an exported component or adb path reachable in release; overhead numbers missing or above the limits; correctness bugs in stats, percentiles, per-route attribution or the score; Material components or hard-coded strings; missing or weak tests; narrating comments, AI slop, dead or commented-out code.
NON-BLOCKING: nits. Do not modify anything. Cite file and line.`, { label: `review:HUD#${round}`, phase: 'Review', schema: REVIEW })
  if (!rv) throw new Error('HUD review failed')
  if (!rv.blocking.length) { clean = true; break }
  open = rv.blocking
  log(`HUD review #${round}: ${rv.blocking.length} blocking`)
  r = await implement(`fix#${round}`, `MODE: fix. A fresh-context reviewer found BLOCKING issues. Fix every one (commits with why-messages), re-run the affected tests and measurements, update the handoff, and return state="done":\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking (address cheaply or record why not): ${JSON.stringify(rv.nonblocking)}`)
  if (r.state !== 'done') return { state: r.state, summary: r.summary, review: 'fix run did not finish' }
}

phase('Show')
const show = await agent(`The user wants to SEE the new performance overlay (package HUD, branch wp/HUD in ${WT}) on their phone. ${ORCH}
1. Build the everything preset from wp/HUD twice: devDebug and devRelease (application id com.example.hud.everything), through ${L} gradle.
2. Install both on the phone through ${L} adb. Open the devRelease build, show the pill, open the Performance tab, use the app for a minute (tabs, push and pop, scrolling, a sheet) so the graphs fill, and capture screenshots of the pill, each graph, the per-screen bars and the score, plus one screen recording (screenrecord --size 720x1600, under 60 s) of the overlay while navigating. Do the same briefly on the devDebug build to show its "debug numbers are pessimistic" notice. Save everything under ${OV}/perf/HUD/show/ with readable names.
3. Export the report (the panel action) and copy the JSON and Markdown into ${OV}/perf/HUD/show/.
4. Write ${OV}/perf/HUD/SHOW.md for the user in plain English: what each part of the overlay and each graph means, the measured overhead (shown vs hidden), the optimisation score of the everything preset and its biggest lost points, and how to open it on the phone.
5. Leave the devRelease build installed and open on its Performance tab so the user can pick up the phone and look. Restore any device setting you changed.
Return a short summary with the overhead numbers and the score.`, { label: 'HUD:show', phase: 'Show' })
return { state: r.state, summary: r.summary, reviewClean: clean, openFindings: clean ? [] : open, show }
