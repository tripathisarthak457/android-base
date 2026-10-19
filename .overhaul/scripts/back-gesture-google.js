export const meta = {
  name: 'back-gesture-google',
  description: "The back gesture done Google's way: research Google's guidance and code, design the smoothest approach, measure it on the Redmi against the texture patch, independent review",
  phases: [
    { title: 'Research', detail: "Google's guidance, Google's own code, the current implementation" },
    { title: 'Design', detail: 'the smoothest approach that follows Google' },
    { title: 'Measure', detail: 'candidates against HEAD and the texture patch on the phone' },
    { title: 'Review', detail: 'a separate reviewer checks every claim against the evidence' },
  ],
}

const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const NAV = `${OV}/wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation`
const CTX = `Context: the android-base overhaul on Windows (D:/projects/tools; read D:/projects/tools/CLAUDE.md's Windows rules). Package WP04 owns the app shell's navigation and the back gesture, in the worktree ${OV}/wt/WP04 (branch wp/WP04): ${NAV}/back/BackCard.kt (BackCardState: the card that follows the finger), back/BackHandlerEffect.kt, back/StackBackHandler.kt, back/ShellRootBack.kt, stack/StackScene.kt, stack/StackHost.kt, presentation/PresentationCard.kt, HostCrossfade.kt; library versions in ${OV}/wt/WP04/templates/android/gradle/libs.versions.toml (Compose BOM 2026.09.00, navigation3 1.2.0, navigationevent 1.2.0). The template forbids Material (Compose Foundation and UI only; templates/android/AGENTS.md) and has its own rules (A11.3 rule 1: no offscreen layer on a whole screen; find the rule text in the template docs). History and numbers: ${OV}/handoff/WP04-perf.md §2h (the back gesture: what is left, every lever tried, the texture lever and phase 17b's measurements), §0, §3, and the patch ${OV}/out/WP04-perf/backcard-texture.patch (CompositingStrategy.Offscreen on the card's layer for the whole gesture). The phone is the user's benchmark: Redmi Note 10 Pro Max (9a7c74f4, custom ROM, Android 17 / SDK 37, 120 Hz, budget GPU whose clocks start low). The user decided on Oct 19 06:16 to adopt the texture patch, then asked at 06:25: "That back gesture fix or implementation do what google says n check also for better idea how to do it smoothly". The user's goal: the app light and smooth, the most optimised visual feel. Never edit ${OV}/wt/WP04 (WP04's own agent works there), never push, never change device settings without restoring them, Gradle and adb only through ${L}.`

const RESEARCH = { type: 'object', properties: {
  findings_md: { type: 'string' },
  key_points: { type: 'array', items: { type: 'string' } },
  sources: { type: 'array', items: { type: 'string' } },
}, required: ['findings_md', 'key_points', 'sources'] }

phase('Research')
const [guide, google, audit] = await parallel([
  () => agent(`${CTX}
Your task: find out exactly what Google says about building an in-app predictive back animation like this one (a full-screen card that follows the finger, then commits or cancels), and what Google says about doing it smoothly. Read, in this order: the Android skills vendored in ${OV}/wt/WP02/templates/android/.claude/skills (navigation-event, navigation-3, jetpack-compose-performance, including their references), then developer.android.com (predictive back: the Compose guides, custom in-app animations, the navigationevent library's NavigationEventHandler and NavigationBackHandler, the back progress and swipe edge, the Material design guidance for predictive back with its numbers for scale, margins, corners and timing, any performance advice on graphicsLayer and CompositingStrategy, layers and offscreen buffers) and the Android Developers blog posts on predictive back. Write what Google recommends as concrete rules: which APIs, how to map progress to the animation, what to animate (only graphicsLayer properties? which?), the commit and cancel motion, what to avoid for performance, and anything about the first frame or rendering the outgoing screen. Quote exact numbers and API names. Mark which advice is Material-specific (the template cannot use Material components, but can follow the motion spec). Return findings_md (complete, organised), key_points, and sources (URLs or file paths). Do not modify files.`, { label: 'research:google-guidance', phase: 'Research', schema: RESEARCH }),
  () => agent(`${CTX}
Your task: find out how Google's own code renders predictive back cheaply, so we can copy the technique. Study the sources (look for *-sources.jar under D:/.caches/gradle first and unzip what you need into ${OV}/research/back-google/src/ with python's zipfile; otherwise read cs.android.com or android.googlesource.com and github.com/androidx/androidx on the web): (1) navigation3-ui 1.2.0's NavDisplay predictive pop (SeekableTransitionState, predictivePopTransitionSpec, what is recomposed or redrawn per progress step); (2) navigationevent 1.2.0 (how progress is delivered, frame alignment); (3) Compose's graphicsLayer and GraphicsLayer (rememberGraphicsLayer, record, drawLayer), and what CompositingStrategy.Offscreen and ModulateAlpha do on Android (RenderNode setUseCompositingLayer / layer types); (4) Material3's predictive back for sheets and search (reference only: what it animates per frame); (5) the platform's own back animations in AOSP WindowManager Shell (CrossActivityBackAnimation / DefaultCrossActivityBackAnimation, back-to-home): what they draw each frame (a SurfaceControl leash with the window's last buffer, scaled and clipped, no re-render?). For each, say what work happens per gesture frame on the UI thread, the RenderThread and the GPU, and why it is cheap. Return findings_md, key_points, sources. Write nothing outside ${OV}/research/back-google/.`, { label: 'research:google-code', phase: 'Research', schema: RESEARCH }),
  () => agent(`${CTX}
Your task: audit the current back gesture implementation (read-only, at wp/WP04's current head in ${OV}/wt/WP04; also read the texture patch). List precisely what runs on each gesture frame: input to progress, recomposition, layout, draw recording, graphicsLayer property updates, RenderThread replay of display lists, GPU work, and what the commit fade adds. Compare with the measurements in WP04-perf §2h (RenderThread frames of 6-12 ms on little cores, GPU frames in the commit fade, first frame from idle). Then list every place where it differs from what Google's APIs offer (for example: own back handling instead of navigationevent's handlers, progress mapping, the commit and cancel specs, how the outgoing and incoming screens are drawn), and every place where a cheaper path may exist. Cite file:line. Return findings_md, key_points, sources. Do not modify files.`, { label: 'research:current-audit', phase: 'Research', schema: RESEARCH }),
])
if (!guide || !google || !audit) throw new Error('a research agent failed')
log('research done')

phase('Design')
const DESIGN = { type: 'object', properties: {
  recommendation: { type: 'string' },
  candidates: { type: 'array', items: { type: 'object', properties: {
    name: { type: 'string' }, description: { type: 'string' }, google_alignment: { type: 'string' },
    expected_effect: { type: 'string' }, risks: { type: 'string' },
  }, required: ['name', 'description', 'google_alignment', 'expected_effect', 'risks'] } },
  experiment_plan: { type: 'string' },
  doc_path: { type: 'string' },
}, required: ['recommendation', 'candidates', 'experiment_plan', 'doc_path'] }
const design = await agent(`${CTX}
You are the design lead (Opus). Three research reports follow. Decide how the back gesture should be built so that it (a) follows Google's guidance and Google's own technique wherever they apply, (b) is the smoothest possible on the Redmi, and (c) stays light (memory, battery) and keeps text and corners crisp. Consider the texture patch (it is what the user adopted), Google's recommended path, and any better idea the research suggests (for example a hybrid: a texture only while the finger moves and a cheap fade, an animation that touches only graphicsLayer properties so no display list is re-recorded, drawing the outgoing screen from the platform-style captured buffer, a different commit motion that avoids a full-screen alpha fade, or navigationevent's own handlers). Reject ideas that break the template's rules (no Material) unless Google's guidance requires it, and say so. Pick at most two candidates to measure besides HEAD and the texture patch, and write an exact experiment plan for the phone (what to build, which scenario, run order, how many runs, which tools in ${OV}/out/WP04-perf to read the results with, which visual checks: mid-gesture frames for text sharpness, corners, shadows, and the first frame's latency and GPU memory). Write the full design to ${OV}/research/back-google/DESIGN.md and return it.
Google's guidance: ${JSON.stringify(guide)}
Google's code: ${JSON.stringify(google)}
Current implementation: ${JSON.stringify(audit)}`, { label: 'design', phase: 'Design', schema: DESIGN })
if (!design) throw new Error('design failed')

phase('Measure')
const MEASURE = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  summary: { type: 'string' },
  table_md: { type: 'string' },
  patches: { type: 'array', items: { type: 'string' } },
  best: { type: 'string' },
  needs_user_decision: { type: 'boolean' },
  decision_question: { type: 'string' },
}, required: ['state', 'summary', 'table_md', 'patches', 'best', 'needs_user_decision', 'decision_question'] }
const measure = await agent(`${CTX}
You are the performance engineer (Opus). Carry out this design's experiment plan on the phone: ${JSON.stringify(design)}
How: work in ${OV}/wt/WP04-perf (branch wp/WP04-perf). It last merged wp/WP04 at 39a6546; merge wp/WP04's current head first (commit only that merge, as WP04-perf did before) so every build has WP04's latest fixes. Write each candidate as a patch against that tree under ${OV}/out/WP04-perf/back-google/ (git diff output that applies with git apply from the repository root), never as a commit on any branch; the texture patch is ${OV}/out/WP04-perf/backcard-texture.patch. Build each variant the way phase 17b did (${OV}/out/WP04-perf/phase17b.win.sh and build-wt.win.sh; read handoff/WP04-perf.md §2h for the method), through ${L} gradle, one build at a time; run the phone through ${L} adb with the same harness (measure.win.sh, pushpop_gesture, both run orders, three runs each, NOTRACE as before; screenshots and recordings to /data/local/tmp, never /sdcard, because /sdcard wakes Google Photos). Read results with abstats.py, hitchpos.py, gesturephases.py. Check the visuals yourself: pull mid-gesture frames from a recording and look at them (text sharpness, corners, shadow, the page underneath), and record the first frame's latency and the GPU memory while the card is up (dumpsys gfxinfo / meminfo). Revert the worktree to its committed state after every variant. Write ${OV}/out/WP04-perf/back-google/RESULT.md (method, the comparison table for HEAD, the texture patch and each candidate, the visual checks, which variant is best and why, which Google guidance each follows) and add a section "## 2j. Back gesture, Google's way (Oct 19)" to ${OV}/handoff/WP04-perf.md. "best" names the variant and its patch path. Set needs_user_decision only if the best variant differs from what the user already adopted in a way with a real trade-off (then write decision_question with each option's pros, cons and catches); otherwise false and an empty question.`, { label: 'measure', phase: 'Measure', schema: MEASURE })
if (!measure) throw new Error('measure failed')

phase('Review')
const VERDICT = { type: 'object', properties: { ok: { type: 'boolean' }, summary: { type: 'string' }, problems: { type: 'array', items: { type: 'string' } } }, required: ['ok', 'summary', 'problems'] }
let review = await agent(`${CTX}
You are a separate reviewer at Google/AOSP level. Read ${OV}/research/back-google/DESIGN.md, ${OV}/out/WP04-perf/back-google/RESULT.md, the new §2j in ${OV}/handoff/WP04-perf.md and every patch under ${OV}/out/WP04-perf/back-google/. Check the claims against the evidence (re-derive the key numbers from the raw result files, look at the mid-gesture frames), check the patches for correctness (state handling across cancel and commit, a second gesture during the commit, accessibility, configuration change, the layer released when the card ends, no allocation per frame), and check that the recommended variant really follows Google's guidance as the research documents it. Report a problem only if it is real; ok=true with an empty list is a fine answer. Do not modify anything.`, { label: 'review', phase: 'Review', schema: VERDICT })
if (review && !review.ok && review.problems.length) {
  log(`review: ${review.problems.length} problems`)
  const fixed = await agent(`${CTX}
You are the performance engineer again. A separate reviewer found these real problems in the back-gesture work (${OV}/out/WP04-perf/back-google/, RESULT.md, handoff/WP04-perf.md §2j): ${JSON.stringify(review.problems)}. Fix each one (patches, re-measure through the locks where a number changes, correct RESULT.md and §2j). Return the updated result.`, { label: 'measure-fix', phase: 'Review', schema: MEASURE })
  if (fixed) Object.assign(measure, fixed)
  review = await agent(`${CTX}
You are the separate reviewer again. Check that these problems are now fixed, against the files and evidence: ${JSON.stringify(review.problems)}. Report only problems that remain or new real ones; ok=true with an empty list is fine. Do not modify anything.`, { label: 'review#2', phase: 'Review', schema: VERDICT })
}
return { design: { recommendation: design.recommendation, doc: design.doc_path }, measure, review }
