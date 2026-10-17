export const meta = {
  name: 'shimmer-fix-from-report',
  description: 'The user reports the shimmer has issues and content gets cut: capture it on the phone, audit every skeleton and clipping site, fix the shimmer on a side branch for WP03, route clipping fixes to owners, review against the Google bar, verify on the phone',
  phases: [
    { title: 'Verify', detail: 'before/after on the phone' },
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
const Q = OV + '/qa/shimmer'
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
const WT = OV + '/wt/WP03-skel'

const CONTEXT = `Project: the user's app generator "android-base" (${R}, integration branch overhaul/multiplatform-v2), mid-overhaul. Packages are built in worktrees ${OV}/wt/<WP>: WP03 owns the design system (core/designsystem, catalog), WP04 owns the navigation shell and chrome (floating tab bar, large title, LocalBottomChrome/LocalTopChrome, content under chrome), WP02-M3 will own the feature screens, WP07 the Compose Multiplatform copy, WP08 the SwiftUI template. Read ${OV}/REQUIREMENTS.md (iOS-level polish and smoothness, zero per-frame recomposition, no alpha over full-screen layers, adaptive UI, Google/AOSP code quality, no AI slop) and ${OV}/research/CODE-STANDARDS.md.
THE USER'S REPORT (Oct 12, 13:35, while watching their phone, which was running WP03's pull-to-refresh test app com.example.wp03.ptr.dev): "shimmer has issues and content gets cut". No screenshot was attached.
Leads already seen by the orchestrator:
- ${OV}/wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/component/feedback/AppSkeleton.kt: every placeholder sweeps its own band across its own width (band = 60 % of the element), so a list of lines and circles of different widths shimmers out of step with different band sizes instead of one band crossing the screen; the clock is a LocalShimmer that is null unless provided.
- ${OV}/perf/WP03/ptr/r8-after/light-rest.png: the last card is cut by an opaque full-width strip behind the tab bar (the old shell that WP03's test project still uses; REQUIREMENTS 17, which WP04's branch claims to fix).
- WP08 has a commit "Sweep the skeleton's highlight over it instead of masking to it" (the SwiftUI skeleton had a related problem).
Phone: serial 9a7c74f4 (Redmi Note 10 Pro, 120 Hz); adb only via ${L} adb (other agents hold it at times: wait); screenrecord needs --size 720x1600; judge smoothness only on release builds. Gradle only via ${L} gradle. Never kill or stop processes you did not start; never run ./gradlew --stop. Restore any device setting you change (night mode, font scale, wm size/density, airplane mode). Today is 2026-10-17. Windows, native (no WSL): your Bash tool is Git Bash; ${L} sets JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME, which your shell lacks, so never run gradlew or adb bare; set MSYS_NO_PATHCONV=1 for adb device paths (/sdcard/...); helper scripts under ${OV}/qa and ${OV}/out were written on Linux, so port their paths (/home/crazyhusir/Documents/projects/tools is D:/projects/tools) and Linux-only commands before re-running them. There is no emulator on this machine. If the phone (adb devices) is missing, do every non-phone step and poll for it in the background (up to 45 minutes) before the phone steps; record what remains.`

const MAP = { type: 'object', properties: { file: { type: 'string' }, summary: { type: 'string' } }, required: ['file', 'summary'] }

// The report (qa/shimmer/REPORT.md), the fix on wp/WP03-skel (review #3 approved; wp/WP03 already contains it) and the
// owner notes finished on Oct 15. Only the phone verification is left.
const evidence = { file: Q + '/evidence/EVIDENCE.md' }
const report = { file: Q + '/REPORT.md' }

phase('Verify')
const verify = await fableAgent(`${CONTEXT}

Verify the shimmer fix on the phone. Build a release APK of the catalog (and the sample app if the report's evidence used it) from ${WT} through ${L} gradle, then in one ${L} adb hold: record the same states as ${evidence.file} (catalog skeleton section, cold start of the sample list with data cleared, light and dark), extract frames, and measure janky % (FrameTimeline via dumpsys gfxinfo framestats) while a skeleton list is on screen for 10 s. Put before/after frame strips side by side in ${Q}/after/ (before = the evidence frames). Write ${Q}/VERIFY.md with the verdict per defect from REPORT.md (fixed, still present, owned by another package and routed), the numbers, and the image paths. Append the result to ${OV}/handoff/WP03-skel.md. Return the path and a 10-line summary.`, { label: 'verify:phone', phase: 'Verify', schema: MAP })
return { report: report.file, verify: verify ? verify.summary : 'verification failed' }
