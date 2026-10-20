export const meta = {
  name: 'plan-release-pipeline',
  description: "Plan the road from staging to the Android release gate: every package and approved item still to build on staging-wp branches, in dependency order with the one-build-at-a-time and one-phone limits, the WP12 docs pass, WP13 verification, the REQUIREMENTS 52 full code review and the REQUIREMENTS 46 matrix before the gate, an honest ETA from measured durations, and a draft workflow script. Opus planner, separate Opus reviewer, one revision. Read-only",
  phases: [
    { title: 'Plan', detail: 'Opus plans the post-staging pipeline and drafts its workflow script' },
    { title: 'Review', detail: 'a separate Opus review of the plan and the draft script, then one revision' },
  ],
}

// The user, Oct 20 19:12: "keep working till 8:40 am tommorrow use full limits". Package work waits for STAGING-REPORT.md
// and the user's go (REQUIREMENTS 44), so this only plans, read-only, so the release run can start the moment they say go.
// The old to-release.js targeted the integration branch and per-package reviews; staging replaced both (Oct 19 15:50).
// Model routing (global CLAUDE.md §10): planning, the script draft and the review are Opus.
const OV = 'D:/projects/tools/.overhaul'
const R = 'D:/projects/tools/android-base'
const PD = `${OV}/plans/2026-10-20`

const RULES = `Rules: read-only everywhere except your own files under ${PD}/. Never run gradle or adb, never touch a worktree under ${OV}/wt/ (staging's fix step is editing wt/STAGING right now), never commit or push. Read code from the staging branch head with git -C ${R} show staging:<path>, git -C ${R} log staging and git -C ${R} grep -n <pattern> staging; staging moves while you work, so note the head you read.`

const PLAN = { type: 'object', properties: {
  summary: { type: 'string' },
  stages: { type: 'array', items: { type: 'object', properties: {
    id: { type: 'string' }, branch: { type: 'string' }, depends_on: { type: 'array', items: { type: 'string' } },
    needs_phone: { type: 'boolean' }, estimate_hours: { type: 'number' }, done_when: { type: 'string' },
  }, required: ['id', 'branch', 'depends_on', 'needs_phone', 'estimate_hours', 'done_when'] } },
  eta: { type: 'object', properties: { best: { type: 'string' }, likely: { type: 'string' }, risk: { type: 'string' }, basis: { type: 'string' } }, required: ['best', 'likely', 'risk', 'basis'] },
  questions_for_user: { type: 'array', items: { type: 'string' } },
  plan_path: { type: 'string' },
  script_path: { type: 'string' },
}, required: ['summary', 'stages', 'eta', 'questions_for_user', 'plan_path', 'script_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Plan')
const PLAN_PROMPT = (extra) => `You are the Opus release lead of the android-base overhaul, planning the road from the staging branch to the Android release gate. ${RULES}
Read first: ${OV}/REQUIREMENTS.md in full (items 36-54 above all: 44 staging first and the staging-wp branches, 46 every requirement met with proof, 50 docs ship with each change and the WP12 knowledge pass, 51 the approved Telegram and District items with their conditions, 52 the full pre-main code review by dimension, adversarially verified, after WP13 and before the gate, 53 Coil and Media3 as opt-in features with real-media scenes, 54 the private fixture rename); ${OV}/handover/HANDOVER.md (the package list, what each package still owes, the old order MB, QA2, WP12, WP13, gate); ${OV}/STATUS.md §1; ${OV}/staging/STAGING-PLAN.md, MERGE-NOTES.md and TEST-REPORT.md (what staging fixes now and what it routes to staging-wp branches); ${OV}/audit/2026-10-19-coverage/COVERAGE-AUDIT.md (the forgotten items and where they were routed); the plans in ${PD}/ (freeze.md, lists-images.md, keyboard-slides.md, sheet-picture-trial.md, overlay.md and their agreed build order, media.md); every handoff under ${OV}/handoff/ for remaining work (liquid glass WP05 and CP2, WP07, WP02-M3, WP14, MB, QA2, HUD, CAT, NET and the rest); ${OV}/scripts/to-release.js and ${OV}/scripts/consolidate-staging.js (the patterns to reuse: package flows, slots, the Google/AOSP review and fix loop, the gate, the model routing with Sonnet only for mechanical runs and an Opus verifier after each); ${OV}/decisions/LOG.md for decisions already taken (never re-argue them).
Measure, do not guess, the durations: read the timestamps in ${OV}/PROGRESS.md and in the workflow journals under C:/Users/crazyhusir/.claude/projects/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/subagents/workflows/*/journal.jsonl (started and result rows per label) and the build logs under ${OV}/logs/ and ${OV}/staging/logs/ to derive how long a package implement, review and fix round, a full desktop matrix and a phone gate really take on this machine (one gradle build at a time machine-wide, one phone, 16 GB of memory, usage paused about one hour in every five).
Write ${PD}/release-pipeline.md: (1) every stage from the user's go on staging to the gate's GO: each package or approved item, its staging-wp branch, what it must deliver (with the requirement numbers), what it depends on, whether it needs the phone, its done-definition and its estimate; (2) the order and the parallelism the gradle lock and the phone allow, with the critical path named; (3) how each stage merges back into staging (one combined Google/AOSP review per merge, the phone gate on release builds, docs in the same change); (4) WP12's knowledge pass and WP13's verification (the matrix over every preset and feature combination, the before and after report, report/FINAL.md); (5) the REQUIREMENTS 52 review as its own package (dimensions: security, architecture, speed, standards, licensing and privacy, requirements; finders, adversarial verifiers, fixes, a clean re-review) and the REQUIREMENTS 46 matrix as a gate condition; (6) the gate, then asking the user (main only with their explicit go); (7) an honest ETA (best, likely, risk) with the arithmetic shown; (8) the risks and what would cut the critical path without lowering quality; (9) any question only the user can answer.
Then draft the workflow script that runs this pipeline as ${PD}/release-from-staging.draft.js, following the Workflow script rules (plain JavaScript, export const meta as a pure literal with phases, agent/parallel/pipeline/phase/log, no Date.now or Math.random, no apostrophes inside single-quoted strings, opts.model 'sonnet' only on mechanical capture or run steps each followed by an Opus verifier, every other agent on the default model, labels unique and stable so a resume hits the cache). It must never push, never touch main, never include PeriFlow, and stop for the user at the gate. Check its syntax with node -e (new Function over the body after stripping the export keyword) and fix until it parses.${extra}`
let plan = await agent(PLAN_PROMPT(''), { label: 'plan:release', phase: 'Plan', schema: PLAN })
if (!plan) throw new Error('plan failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer at the Google/AOSP bar. Check ${PD}/release-pipeline.md and ${PD}/release-from-staging.draft.js against REQUIREMENTS.md (36-54 above all), the handoffs, the staging notes, the plans in ${PD}/ and their agreed build order, decisions/LOG.md and the measured durations. ${RULES} Do not modify anything.
The planner's summary: ${JSON.stringify(plan)}
Blocking means: a requirement, approved item or routed coverage item with no stage; a dependency that is wrong or missing; two stages that would edit the same files at once; a stage that needs the phone running beside another that measures on it; a decision the user already took that is re-argued, or a user-only decision taken silently; a merge without the combined review or the release-build phone gate; docs not shipped with their change; the REQUIREMENTS 52 review or the REQUIREMENTS 46 matrix missing before the gate; an ETA not backed by the measured numbers; in the draft script: a syntax error, a Sonnet agent doing judgement, code or review, a mechanical run with no Opus verifier, a push, a main merge, PeriFlow, an unstable label. There is no pressure to find something.`, { label: 'review:release', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(PLAN_PROMPT(`\nThis is a revision. A separate reviewer found these blocking problems: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in both files and return the full updated result.`), { label: 'plan:release#2', phase: 'Review', schema: PLAN })
  if (revised) plan = revised
}
return { plan, review }
