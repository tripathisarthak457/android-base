export const meta = {
  name: 'hud-metrics-design',
  description: "Design the next performance overlay metrics the user asked for: a recomposition score per screen with 'not optimised' flags, and the other cheap metrics worth showing, without the overlay costing frames. A Sonnet API lookup, an Opus design, a separate Opus review. No code changes; built on staging-wp/HUD after staging",
  phases: [
    { title: 'Lookup', detail: 'Sonnet lists the runtime APIs for recomposition and other metrics, with versions and costs' },
    { title: 'Design', detail: 'Opus designs the metrics, their verdicts and their cost budget' },
    { title: 'Review', detail: 'a separate Opus review, then one revision' },
  ],
}

// The user, Oct 20 00:33: "you know we have showing the matrix like the FPS in jank rate in the app, can we also show
// recomposition score and if it's not optimised and ki other performance stuff". REQUIREMENTS 49. Package work waits for
// staging (REQUIREMENTS 44), so this designs now and the HUD's staging-wp branch builds it after the user's go.
// Model routing (global CLAUDE.md §10): the API lookup is mechanical (Sonnet, facts with sources); the design and the
// review are Opus.
const OV = 'D:/projects/tools/.overhaul'
const R = 'D:/projects/tools/android-base'
const HD = `${OV}/research/hud-metrics`

const RULES = `Rules: read-only everywhere except your own files under ${HD}/. Never run gradle or adb, never touch a worktree under ${OV}/wt/ or the staging worktree (other runs are editing them), never commit or push. Read code from branch heads with git -C ${R} show <branch>:<path> or git -C ${R} grep -n <pattern> <branch> -- templates/android.`

const FOUND = { type: 'object', properties: {
  apis: { type: 'array', items: { type: 'object', properties: {
    name: { type: 'string' }, gives: { type: 'string' }, availability: { type: 'string' }, cost: { type: 'string' }, source: { type: 'string' },
  }, required: ['name', 'gives', 'availability', 'cost', 'source'] } },
  not_found: { type: 'array', items: { type: 'string' } },
  notes_path: { type: 'string' },
}, required: ['apis', 'not_found', 'notes_path'] }
const DESIGN = { type: 'object', properties: {
  summary: { type: 'string' },
  metrics: { type: 'array', items: { type: 'object', properties: {
    name: { type: 'string' }, shows: { type: 'string' }, how: { type: 'string' }, verdict_rule: { type: 'string' }, cost: { type: 'string' }, where: { type: 'string' },
  }, required: ['name', 'shows', 'how', 'verdict_rule', 'cost', 'where'] } },
  left_out: { type: 'array', items: { type: 'string' } },
  questions_for_user: { type: 'array', items: { type: 'string' } },
  design_path: { type: 'string' },
}, required: ['summary', 'metrics', 'left_out', 'questions_for_user', 'design_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Lookup')
const apis = await agent(`You are a careful researcher doing lookups. Report facts only, each with its official source URL, the library version that has it, and whether it works in a release-like (non-debuggable, minified) dev build. Do not judge what the overlay should do. Use WebSearch (standard first) and WebFetch, Android developer docs and androidx release notes first. ${RULES} Notes file: ${HD}/apis.md.
1. Counting recompositions at runtime in Jetpack Compose: CompositionObserver / ObservableComposition and RecomposeScope observation (androidx.compose.runtime.tooling, the experimental annotations and versions), Recomposer.runningRecomposers and RecomposerInfo (what changeCount means), composition tracing (androidx.compose.runtime:runtime-tracing, what it needs at run time), what Layout Inspector's recomposition counts rely on and whether an app can read them itself, the Compose compiler's stability and skippability reports, and any official guidance on measuring recomposition in production builds.
2. Other cheap runtime signals: FrameMetrics fields (CPU vs GPU time, input, animation, layout and measure, draw), JankStats states, PowerManager thermal status and getThermalHeadroom, Debug runtime stats (GC counts and time, blocking GC), Looper message timing on the main thread, ActivityManager memory class and trim callbacks, Display refresh rate and frame rate override, ApplicationStartInfo (API 35) for cold start, and bitmap or texture memory if observable. For each: what it gives, the cost of reading it, and its API level.`, { label: 'lookup:apis', phase: 'Lookup', schema: FOUND, model: 'sonnet' })
if (!apis) throw new Error('lookup failed')

phase('Design')
const DESIGN_PROMPT = (extra) => `You are the performance tooling lead of android-base, at the Google/AOSP bar. ${RULES}
The user wants the in-app performance overlay (the HUD, which already shows FPS, janky %, frame histograms, a per-screen table, memory, network timings, StrictMode, build checks, a score and fix hints) to also show a recomposition score, flag what is not optimised, and show other useful performance signals. A Sonnet lookup listed the APIs; verify every fact you rely on against its source: ${JSON.stringify(apis)}.
Read first: ${OV}/handoff/HUD.md (Design and decisions, Measurements, the Compose performance audit, Open issues), the overlay code on wp/HUD (templates/android/core/perfhud, the dev app's ShellLog and any recompose counters, for example the recompose.push.max budget), templates/android/tools/perf/gates.toml on wp/WP04, ${OV}/REQUIREMENTS.md items 22 (no per-frame recomposition, compiler metrics clean), 40 (the HUD) and 42 (the release gates), and the regression the bug run found on Oct 19: the combined app measured about twice as janky with the HUD compiled into the measured build (bugs/BUG-2026-10-19-back-see-through.md, Result; the first suspect is the HUD's per-frame listener and DEV badge).
Design, and write ${HD}/DESIGN.md (plain English first for the user, then engineering detail):
1. A recomposition score: per screen and per state (idle, scrolling, animating, transitioning), recompositions per second and per frame, the worst scopes by name where the runtime can name them, and a verdict against REQUIREMENTS 22 (for example: any recomposition per frame while scrolling or animating is red, steady recomposition while idle is amber). Say exactly which API it uses, what it can and cannot see in a minified dev build, and its cost.
2. 'Not optimised' flags for every metric, each tied to a gate or a documented threshold, with the fix hint the overlay would show.
3. The other signals worth adding (only those cheap enough), each with how it is read, how often, and its verdict.
4. A cost budget the overlay must meet: zero work while hidden; when shown, sampling instead of per-frame work where possible; no allocation per frame; no recomposition of app content; and absent from release builds and from every build the perf harness measures (and how that is enforced, for example a build check and a test). Include how to prove the budget on the Redmi (an A/B run with the overlay on and off).
5. Where each piece goes in the code (module, file, owner: staging-wp/HUD), the tests, and the order to build it.
6. Questions only the user can answer (for example which metrics the pill itself shows).${extra}`
let design = await agent(DESIGN_PROMPT(''), { label: 'design:metrics', phase: 'Design', schema: DESIGN })
if (!design) throw new Error('design failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer at the Google/AOSP bar. Check ${HD}/DESIGN.md against the sources (the official docs for every API it relies on), the overlay code on wp/HUD, gates.toml and REQUIREMENTS 22, 40, 42. ${RULES} Do not modify anything.
The designer's summary: ${JSON.stringify(design)}
Blocking means: an API used wrongly or not available where the design says (release-like builds, API levels), a metric whose own cost would distort what it measures, a design that lets the overlay run in measured or release builds, per-frame allocation or recomposition in the overlay, a verdict rule that contradicts the gates, or a missing test that would catch the overlay costing frames. There is no pressure to find something.`, { label: 'review:design', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(DESIGN_PROMPT(`\nThis is a revision. A separate reviewer found these blocking problems: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in DESIGN.md and return the full updated result.`), { label: 'design:revise', phase: 'Review', schema: DESIGN })
  if (revised) design = revised
}
return { design, review }
