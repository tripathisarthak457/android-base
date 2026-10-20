export const meta = {
  name: 'plan-media',
  description: "Plan REQUIREMENTS 53: Coil (images) and Media3 ExoPlayer (video) as opt-in generator features with every list and media optimisation built in, plus catalog scenes with real photos and real video (feed, e-commerce grid, short-video feed) measured on the Redmi. A Sonnet lookup, an Opus plan, a separate Opus review. Read-only; built after staging and before the Android release",
  phases: [
    { title: 'Lookup', detail: 'Sonnet lists current Coil 3 and Media3 APIs, versions, guidance and licensed sample media' },
    { title: 'Plan', detail: 'Opus plans the two features, the scenes, the gates and the generator support' },
    { title: 'Review', detail: 'a separate Opus review, then one revision' },
  ],
}

// The user, Oct 20 (09:5x-10:0x): image-heavy lists lagged in an earlier app, "optimise that as well load real images and
// see if it's good", "Also test one video kinda stuff too if possible", "Setup coil and exoplayer as a opt in including all
// the optimisation in the app", and "after staging but before our android release". REQUIREMENTS 53. Telegram P3 and
// plans/2026-10-20/lists-images.md already cover the list-cheap image path; this plan extends it to the features and the
// real-media proof.
// Model routing (global CLAUDE.md §10): the API and media lookup is mechanical (Sonnet, facts with sources); the plan and
// the review are Opus.
const OV = 'D:/projects/tools/.overhaul'
const R = 'D:/projects/tools/android-base'
const PD = `${OV}/plans/2026-10-20`

const RULES = `Rules: read-only everywhere except your own files under ${PD}/. Never run gradle or adb, never touch a worktree under ${OV}/wt/ or the staging worktree (another run is reviewing it), never commit or push. Read the code from the staging branch head with git -C ${R} show staging:<path> or git -C ${R} grep -n <pattern> staging. Stable library versions only, at least two weeks old, in the version catalog; the template's AGENTS.md rules hold (no Material, AppTheme only, 100 % Compose).`

const FOUND = { type: 'object', properties: {
  facts: { type: 'array', items: { type: 'object', properties: { topic: { type: 'string' }, fact: { type: 'string' }, source: { type: 'string' }, version: { type: 'string' } }, required: ['topic', 'fact', 'source'] } },
  media: { type: 'array', items: { type: 'object', properties: { url: { type: 'string' }, kind: { type: 'string' }, size: { type: 'string' }, licence: { type: 'string' } }, required: ['url', 'kind', 'licence'] } },
  notes_path: { type: 'string' },
}, required: ['facts', 'media', 'notes_path'] }
const PLAN = { type: 'object', properties: {
  summary: { type: 'string' },
  steps: { type: 'array', items: { type: 'string' } },
  gates: { type: 'array', items: { type: 'string' } },
  questions_for_user: { type: 'array', items: { type: 'string' } },
  plan_path: { type: 'string' },
}, required: ['summary', 'steps', 'gates', 'questions_for_user', 'plan_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Lookup')
const found = await agent(`You are a careful researcher doing lookups. Report facts only, each with its official source URL and the library version; do not judge what the template should do. Use WebSearch (standard first) and WebFetch, official docs and release notes first. ${RULES} Notes file: ${PD}/media-lookup.md.
1. Coil 3 for Compose: the latest stable version; AsyncImage vs SubcomposeAsyncImage and their cost in lazy lists; sizing (size resolution from layout, Precision), memory and disk cache configuration and defaults, hardware bitmaps, allowRgb565, crossfade cost, placeholders, prefetching with ImageLoader.enqueue, OkHttp vs Ktor network fetchers, and official performance guidance.
2. Media3 for Compose: the latest stable media3 version; media3-ui-compose (PlayerSurface, SURFACE_TYPE_SURFACE_VIEW vs TEXTURE_VIEW, ContentFrame or the presentation state for the shutter and first frame); recommended patterns for video in scrolling feeds (player pooling and reuse, PreloadManager / DefaultPreloadManager, setVideoSurface swapping, pausing off screen, lifecycle release), memory and decoder limits, and official performance guidance.
3. Real sample media that may be used in a public open-source template: photos (for example Lorem Picsum or Unsplash under its licence) and short videos (for example Google's ExoPlayer test media and other clearly licensed sample clips), each with its URL, size, format and licence terms.`, { label: 'lookup:media', phase: 'Lookup', schema: FOUND, model: 'sonnet' })
if (!found) throw new Error('lookup failed')

phase('Plan')
const PLAN_PROMPT = (extra) => `You are the Opus engineer who will build REQUIREMENTS 53 after staging and before the Android release. ${RULES}
Read ${OV}/REQUIREMENTS.md items 50, 51 (P3), 52 and 53 in full; ${PD}/lists-images.md (P3's plan, which this extends; do not duplicate it, build on it); the generator's feature system on staging (the coil feature: the spec, fences, presets, catalogue; how an opt-in feature adds dependencies, code and tests, and how the consistency matrix proves a project without it carries none of it); the image path (AppNetworkImage, WP06's image loader); the catalog app; templates/android/tools/perf (perf.sh, gates.toml, the scenarios). Verified lookup facts (check what you rely on against its source): ${JSON.stringify(found)}.
Write ${PD}/media.md: (1) Coil as an opt-in feature with every optimisation built in by default, stated as concrete defaults with their reasons; (2) a new opt-in video feature on Media3 ExoPlayer (its name in the spec, its fences, its module, its dependencies in the catalog) with pooled players, a SurfaceView-backed Compose surface, a poster until the first frame, preload of the next clip, pause and release off screen and in the background; (3) three catalog scenes with real media (a social photo feed, an e-commerce grid, a short-video feed), the media sources and their licences, cold and warm cache runs; (4) the perf harness scenarios and gate rows for them on the Redmi (janky after the first frame and hitch ratio within the release limits, no decode frame over budget, time to first video frame, bounded memory) and the A/B against today's image path; (5) generator support (spec, presets: 'everything' includes both, the website builder, docs per REQUIREMENTS 50); (6) tests to write first; (7) the build order and commits; (8) risks and how to back out. List any question only the user can answer.${extra}`
let plan = await agent(PLAN_PROMPT(''), { label: 'plan:media', phase: 'Plan', schema: PLAN })
if (!plan) throw new Error('plan failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer at the Google/AOSP bar. Check ${PD}/media.md against the lookup's sources, the staging code (git show), ${PD}/lists-images.md, REQUIREMENTS 50-53 and the template's AGENTS.md. ${RULES} Do not modify anything.
The planner's summary: ${JSON.stringify(plan)}
Blocking means: an API used wrongly or a version that is not stable; a default that costs frames or memory in lists; one player per row or a TextureView without a measured reason; media whose licence does not allow use in a public template; a feature that leaks code or dependencies into projects without it; a gate that cannot tell success from noise; a missing test, docs paragraph or website change; a conflict with lists-images.md. There is no pressure to find something.`, { label: 'review:media', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(PLAN_PROMPT(`\nThis is a revision. A separate reviewer found these blocking problems: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in media.md and return the full updated result.`), { label: 'plan:media#2', phase: 'Review', schema: PLAN })
  if (revised) plan = revised
}
return { plan, review }
