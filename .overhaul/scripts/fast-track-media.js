export const meta = {
  name: 'fast-track-media',
  description: "Fast track (REQUIREMENTS 56): build the images and video package on staging-wp/MEDIA beside staging's last fixes: Telegram P3, P7, P9 and District P3 from lists-images.md, then Coil and Media3 as opt-in features with the real-media catalog scenes, perf gates, generator support and docs from media.md. Opus implementation runs that continue from the handoff, fresh Opus Google/AOSP reviews with fix rounds, a Sonnet phone gate run with an Opus verifier. Low-priority locks; never merges or pushes",
  phases: [
    { title: 'Implement', detail: 'Opus implementation runs, each continuing from the handoff' },
    { title: 'Review', detail: 'fresh Opus Google/AOSP reviews and fix rounds' },
    { title: 'Gate', detail: 'Sonnet runs the media gates on the phone; Opus verifies the numbers' },
  ],
}

// The user, Oct 23 21:4x: "Fast track work"; asked with pros, cons and catches, they chose to start the safe packages
// now beside staging's last fixes (REQUIREMENTS 56). This package does not touch the shell and bar drawing staging is
// still fixing; its builds and phone runs yield the locks to staging (LOCKED_PRIORITY=low). Staging's later commits are
// merged into the branch before the combined review that brings it into staging.
// Model routing (global CLAUDE.md section 10): code, reviews and verdicts are Opus; the phone gate run is Sonnet with an
// Opus verifier.
const OV = 'D:/projects/tools/.overhaul'
const R = 'D:/projects/tools/android-base'
const WT = `${OV}/wt/MEDIA`
const L = `LOCKED_PRIORITY=low ${OV}/bin/locked`
const PD = `${OV}/plans/2026-10-20`
const HO = `${OV}/handoff/MEDIA.md`
const SKILLS = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'

const RULES = `Rules: work only in the worktree ${WT} (branch staging-wp/MEDIA, cut from staging) and in ${HO}; never touch ${OV}/wt/STAGING or any other worktree, never merge into staging or any other branch, never push, never touch main. Merge staging into your branch (git -C ${WT} merge staging) at the start of every run and before every review, so staging's newest fixes are in. Do NOT change the code staging's open fix is changing (read ${OV}/staging/TEST-REPORT.md "Still open"): the navigation shell and its bar layer (BarAnchor, followCovered), the Utility bar's floating drawing, the edge fade, the DEV badge's layer, and anything else that file names as under investigation; if a step needs one of them, do the rest and record the dependency in the handoff. Gradle only as ${L} gradle ./gradlew ...; adb only as ${L} adb adb ... (the low priority yields to staging's waiting commands; never drop it). Long commands in the background with logs. Judge smoothness only on release or benchmark builds; restore any phone setting you change. Commit after every completed step with a message that explains why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"; stage explicit paths only. Every approved change ships with its docs (REQUIREMENTS 50). Quality bar: Google/AOSP (REQUIREMENTS 27), the template's AGENTS.md (no Material, AppTheme only, 100 % Compose), Google's jetpack-compose-performance skill (${SKILLS}) for every composable you touch. Comments explain why, never what. Stable versions only, in the version catalog.`

const STATUS = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'continue', 'blocked'] },
  summary: { type: 'string' },
  milestones: { type: 'array', items: { type: 'string' } },
  waiting_on_staging: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'summary', 'milestones', 'waiting_on_staging'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }
const GATE = { type: 'object', properties: {
  state: { type: 'string', enum: ['green', 'red'] },
  rows: { type: 'array', items: { type: 'object', properties: { row: { type: 'string' }, value: { type: 'string' }, limit: { type: 'string' }, result: { type: 'string' }, evidence: { type: 'string' } }, required: ['row', 'result'] } },
  failures: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'rows', 'failures'] }

const IMPL = (run, extra) => `You are the Opus implementation engineer of the images and video package (REQUIREMENTS 53, with Telegram P3, P7, P9 and District P3 of REQUIREMENTS 51), run ${run}. ${RULES}
Read first: ${OV}/REQUIREMENTS.md items 27, 44, 46, 50, 51, 53, 55, 56; ${PD}/lists-images.md (P3, P7, P9, District P3) and ${PD}/media.md (Coil and Media3 as opt-in features, the catalog scenes with real media, the perf scenarios and gates, generator support and docs), both reviewed, with their agreed build order; ${PD}/media-lookup.md for the verified APIs and media licences; ${OV}/staging/TEST-REPORT.md and MERGE-NOTES.md; your handoff ${HO} if it exists (keep a "## Progress" section at its top current, so a cut-off run loses nothing).
Earlier runs may have stopped mid-way: reconstruct where from the handoff, git log and git status; uncommitted changes are work in progress to review, keep what is right, and commit.
Build in the plans' order: lists-images first (media.md builds on it; media.md may still be in its last revision when run 1 starts, so read it again before its first step, and follow the version whose revision note is newest), then images, video, scenes, perf scenarios and gates, generator support (spec, presets with Everything including both, the website builder, the consistency matrix footprint proof), docs. Write the tests the plans name first and watch each fail on today's code. Run the acceptance the plans define (generator tests, the consistency matrix for the touched presets, the template build and its checks) through the low-priority lock. The phone sessions of media.md section 4 are run by the gate step after you: prepare their scripts and the release build, and run only a short smoke check yourself.
Return state "done" when every step of both plans that does not wait on staging's open fix is committed with its tests and docs and the acceptance passes; "continue" when you are making progress but ran out of room (the handoff says exactly where the next run starts); "blocked" only with evidence. List in waiting_on_staging every step left for after staging's jank fix.${extra || ''}`

phase('Implement')
let impl = null
for (let run = 1; run <= 5; run++) {
  impl = await agent(IMPL(run, ''), { label: `media:implement#${run}`, phase: 'Implement', schema: STATUS })
  if (!impl) throw new Error(`implementation run ${run} failed`)
  log(`implement#${run}: ${impl.state}`)
  if (impl.state !== 'continue') break
}
if (impl.state === 'blocked') return { impl }

phase('Review')
const REVIEW_PROMPT = (round) => `You are a fresh-context Google/AOSP-level reviewer of the images and video package on branch staging-wp/MEDIA (${WT}); review its diff against staging (git -C ${WT} diff staging...HEAD). ${RULES} Do not modify anything.
Check it against ${PD}/lists-images.md and ${PD}/media.md (every step either done with its tests and docs or listed as waiting on staging), ${OV}/REQUIREMENTS.md 27, 50, 51, 53, the template's AGENTS.md, Google's jetpack-compose-performance skill, and the media licences in ${PD}/media-lookup.md. Blocking means: a defect; a default that costs frames or memory in lists; one player per row or a TextureView without a measured reason; work in composition or allocation per frame; a feature that leaks code or dependencies into projects without it (the consistency matrix must prove it); media whose licence does not allow a public template; a missing test, docs paragraph or website change; a change to the code staging's open fix is changing; AI-slop comments. Cite file and line. There is no pressure to find something.`
let rv = await agent(REVIEW_PROMPT(1), { label: 'media:review#1', phase: 'Review', schema: REVIEW })
for (let round = 1; round <= 3 && rv && rv.blocking.length; round++) {
  log(`review ${round}: ${rv.blocking.length} blocking`)
  impl = await agent(IMPL(`fix#${round}`, `\nMODE: fix. A fresh reviewer found these blocking problems; fix each at its root (or, if one is wrong, say why in the handoff), re-run the affected acceptance, commit, and return "done": ${JSON.stringify(rv.blocking)}. Non-blocking: ${JSON.stringify(rv.nonblocking)}`), { label: `media:fix#${round}`, phase: 'Review', schema: STATUS })
  if (!impl) throw new Error(`fix round ${round} failed`)
  rv = await agent(REVIEW_PROMPT(round + 1), { label: `media:review#${round + 1}`, phase: 'Review', schema: REVIEW })
}

phase('Gate')
const run = await agent(`You run the media phone sessions and report facts with paths to the raw evidence; you do not fix anything and do not judge. ${RULES}
From ${WT}, follow ${PD}/media.md section 4 (4.5 the A/B against today's image path, 4.6 the video arms, 4.8 sessions and evidence) with the scripts and the release build the handoff ${HO} names: build through the low-priority lock, install on the Redmi (9a7c74f4) through the low-priority adb lock, run every scenario for the stated number of runs, cold and warm, and save every raw output under ${OV}/perf/MEDIA/<session>/. Restore every phone setting you change and uninstall what you installed. Return every gate row with its value, limit and evidence path.`, { label: 'media:gate-run', phase: 'Gate', schema: GATE, model: 'sonnet' })
const gate = run ? await agent(`You are the Opus verifier of the media gates. Re-derive every row from the raw outputs under ${OV}/perf/MEDIA/ (open them; recompute the medians and ratios), check them against ${PD}/media.md section 4.3 and templates/android/tools/perf/gates.toml on the branch, and look at the frames or screenshots the sessions saved. The runner reported: ${JSON.stringify(run)}. ${RULES} Do not modify anything. Return green only if every row passes for real; write the verdict at the top of ${HO}.`, { label: 'media:gate-verify', phase: 'Gate', schema: GATE }) : null
return { impl, review: rv, gate }
