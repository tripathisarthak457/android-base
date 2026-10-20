export const meta = {
  name: 'plan-approved-work',
  description: "Implementation plans, ready to build after staging, for everything the user approved on Oct 20 (REQUIREMENTS 51): five Opus planners by area on the staging code, then one Opus reviewer that checks each plan and the conflicts between them. Read-only, no code changes",
  phases: [
    { title: 'Plan', detail: 'five Opus planners: the freeze, lists and images, keyboard and slides, the sheet picture trial, the overlay metrics' },
    { title: 'Review', detail: 'one Opus reviewer across all plans, then revisions where it found blocking problems' },
  ],
}

// The user, Oct 20 07:1x: approved Telegram P1-P11 (some opt-in or conditional), District P3 and the overlay metrics
// answers (REQUIREMENTS 51), and at 01:12 asked for the full quota every window. Package work waits for staging and the
// user's go (REQUIREMENTS 44), so this only plans, read-only, so building starts the moment they say go.
// Model routing (global CLAUDE.md §10): planning and review are Opus; nothing here is a mechanical run.
const OV = 'D:/projects/tools/.overhaul'
const R = 'D:/projects/tools/android-base'
const PD = `${OV}/plans/2026-10-20`

const RULES = `Rules: read-only everywhere except your own files under ${PD}/. Never run gradle or adb, never touch a worktree under ${OV}/wt/ or the staging worktree (another run is testing it), never commit or push. Read the code from the staging branch head with git -C ${R} show staging:<path> or git -C ${R} grep -n <pattern> staging -- templates/android (staging may move while you work; note the head you read). The template's AGENTS.md rules hold (no Material, AppTheme only, 100 % Compose); every change ships with its docs (REQUIREMENTS 50; drafts in ${OV}/docs-draft/PENDING.md) and a phone check.`
const PLAN_STEPS = `Write the plan to the file named below: for each item, the owning package and staging-wp branch, the exact files and functions to change (file:line on staging), the design in enough detail to build without guessing (state reads, phases, which signal drives what), the tests to write first (and how each fails on today's code), the phone check that proves it on the Redmi (scene, metric, threshold, A/B against what), the docs paragraph to merge, the risks and how to back out, and the order of commits. Note every file another plan in this batch might also touch (the freeze, lists and images, keyboard and slides, the sheet picture trial, the overlay metrics) so the reviewer can sequence them.`

const PLAN = { type: 'object', properties: {
  summary: { type: 'string' },
  items: { type: 'array', items: { type: 'object', properties: { id: { type: 'string' }, owner: { type: 'string' }, files: { type: 'array', items: { type: 'string' } }, tests: { type: 'string' }, phone_check: { type: 'string' }, risk: { type: 'string' } }, required: ['id', 'owner', 'files', 'tests', 'phone_check', 'risk'] } },
  shared_files: { type: 'array', items: { type: 'string' } },
  plan_path: { type: 'string' },
}, required: ['summary', 'items', 'shared_files', 'plan_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { plan: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['plan', 'problem', 'fix'] } },
  order: { type: 'array', items: { type: 'string' } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'order', 'nonblocking'] }

const AREAS = [
  { key: 'freeze', text: `Telegram P1 (hold decorative motion still while its screen moves: shimmer, AppActivityIndicator, pull-to-refresh spinner, AppLottie, image fades; also their rule-7 breach in hidden tabs) and P2 (hold new screen data until the slide has landed, about 0.35 s, one gate in the screen base, with an opt-out), plus P4 (a push may wait up to about 0.2 s for first content, opt-in per screen through a readiness contract). Sources: ${OV}/research/telegram/RESEARCH.md (§2.5 P1, P2, P4) and SOURCES.md.` },
  { key: 'lists-images', text: `Telegram P3 (list-cheap network images: no subcomposition per image, WP06's loader with explicit limits, one loader per build), P7 (fetch list pages further ahead), P9 (big-list docs rules), and District P3 (prepare a list sheet's list off-screen just before it opens so its first frame is not dropped; 18 of 18 opens miss it today on 10-16 ms of measure and layout). Sources: ${OV}/research/telegram/RESEARCH.md §2.5, ${OV}/research/district/PHONE-RESULTS.md and RESEARCH.md.` },
  { key: 'keyboard-slides', text: `Telegram P5 (keyboard: one layout, then only a slide, for screens with AppBottomActionBar; sheet footers untouched), P8 (cap each frame's time step on screen slides so a hitch slows instead of jumping; never on finger-driven springs), P10 (swipe back from anywhere on the page, opt-in and off by default, disabled on screens with horizontal scrolling, system predictive back kept) and P11 (a quick push style, fade plus short slide at 150 ms, only if P6's trial wins: plan it behind that condition). Sources: ${OV}/research/telegram/RESEARCH.md §2.5, ${OV}/research/back-google/DESIGN.md, ${OV}/research/MOTION-PLAN.md.` },
  { key: 'sheet-picture-trial', text: `Telegram P6 as a trial the user approved only if it wins: draw sheets and full-screen covers from one saved picture (the back card's texture technique) while they open and close. Plan the trial: the implementation behind a flag, the rule exception it needs and its wording, the A/B protocol on the Redmi (scenes over heavy content, frame metrics, GPU memory, the +4 ms first frame), the win condition stated in numbers before measuring, and how it is removed cleanly if it loses. Sources: ${OV}/research/telegram/RESEARCH.md §2.5 P6, the back card on staging (core/navigation BackCard, StackLayout), ${OV}/research/SHEET-SPEC.md.` },
  { key: 'overlay', text: `The overlay metrics (REQUIREMENTS 49 and 51): build order and plan from ${OV}/research/hud-metrics/DESIGN.md with the user's answers at its end (pill: FPS, janky % and one dot; recomposition in the score plus its own card; opaque DEV badge if the A/B shows a cost), and first the measured-build wiring fix (devReleaseRuntimeOnly plus a build check on every measured variant), noting that staging's fix step may already have done it (check staging).` },
]

phase('Plan')
const plans = (await parallel(AREAS.map(a => () => agent(`You are the Opus engineer who will build these approved changes, planning them now so the build can start the moment the user says go after staging. ${RULES}
Items: ${a.text}
${PLAN_STEPS} Plan file: ${PD}/${a.key}.md.`, { label: `plan:${a.key}`, phase: 'Plan', schema: PLAN })))).filter(Boolean)
if (!plans.length) throw new Error('every planner failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer at the Google/AOSP bar. Review the implementation plans in ${PD}/ (freeze.md, lists-images.md, keyboard-slides.md, sheet-picture-trial.md, overlay.md) against the staging code (git show), the research they cite, REQUIREMENTS 22, 42, 44, 46, 49, 50 and 51 in ${OV}/REQUIREMENTS.md, and the template's AGENTS.md. ${RULES} Do not modify anything.
The planners' summaries: ${JSON.stringify(plans)}
Blocking means: a plan that would cost frames, allocate or recompose per frame, or break a rule without naming the exception; a file or function that does not exist on staging; a test that would not fail on today's code; a phone check that cannot tell success from noise; a missing docs paragraph; two plans changing the same code in conflicting ways. Also give the build order across all plans (which branch first, which commits must land together). There is no pressure to find something.`, { label: 'review:plans', phase: 'Review', schema: REVIEW })
let revised = []
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const byPlan = AREAS.filter(a => review.blocking.some(b => b.plan.includes(a.key)))
  revised = (await parallel(byPlan.map(a => () => agent(`You are the Opus engineer who wrote ${PD}/${a.key}.md. ${RULES} A separate reviewer found these blocking problems in the batch: ${JSON.stringify(review.blocking.filter(b => b.plan.includes(a.key)))}. The agreed build order: ${JSON.stringify(review.order)}. Fix each in your plan and return the full updated result.`, { label: `plan:${a.key}#2`, phase: 'Review', schema: PLAN })))).filter(Boolean)
}
return { plans: plans.map(p => ({ path: p.plan_path, items: p.items.length })), review, revised: revised.map(p => p.plan_path) }
