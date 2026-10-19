export const meta = {
  name: 'catalog-preview',
  description: "A preview catalog app with everything done so far (WP03, WP04 with the new back card, WP06, the overlay) combined on a throwaway local branch, built as a release APK, smoke-tested on the Redmi, then independently checked",
  phases: [
    { title: 'Build', detail: 'combine the packages, build the APKs, smoke-test on the phone' },
    { title: 'Check', detail: 'a separate reviewer checks every claim' },
  ],
}

// The user, Oct 19 15:40: "update the quality of catalog app with all the changes we have done thats in the plan"; they chose
// a preview build now plus the content done properly later (REQUIREMENTS 43, package CAT).
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const L = `${OV}/bin/locked`
const WT = `${OV}/wt/_preview`
const BR = 'preview/catalog-2026-10-19'

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) first. Gradle only through ${L} gradle, adb only through ${L} adb, heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${OV}/out/preview/logs/, polled. This is a PREVIEW for the user to feel the work so far: it lives only on the local branch ${BR} in the worktree ${WT}. Never push anything, never commit to any wp/* or overhaul/* branch, never change another worktree. Screenshots and recordings go to /data/local/tmp on the phone, never /sdcard (it wakes Google Photos). Restore any device setting you change. Commit merges and patch applications on ${BR} only, each with a message that says why and ends with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>".`

const BUILT = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  included: { type: 'array', items: { type: 'string' } },
  excluded: { type: 'array', items: { type: 'string' } },
  conflicts: { type: 'array', items: { type: 'string' } },
  apks: { type: 'array', items: { type: 'string' } },
  screenshots: { type: 'array', items: { type: 'string' } },
  recording: { type: 'string' },
  smoke: { type: 'array', items: { type: 'string' } },
  rough_edges: { type: 'array', items: { type: 'string' } },
  notes_path: { type: 'string' },
}, required: ['state', 'included', 'excluded', 'conflicts', 'apks', 'screenshots', 'recording', 'smoke', 'rough_edges', 'notes_path'] }
const VERDICT = { type: 'object', properties: { ok: { type: 'boolean' }, summary: { type: 'string' }, problems: { type: 'array', items: { type: 'string' } } }, required: ['ok', 'summary', 'problems'] }

phase('Build')
let built = await agent(`You build a preview of the template's catalog app with everything the overhaul has done so far, so the user can install it and feel it. ${RULES}
1. Create the worktree: git -C ${R} worktree add ${WT} -b ${BR} overhaul/multiplatform-v2 (if it exists from an earlier attempt, reuse it and say so).
2. Combine, in this order, merging each branch's current head (git merge --no-ff): wp/WP04 (the navigation shell); then apply ${OV}/out/WP04-perf/backcard-texture.patch and ${OV}/out/WP04-perf/back-google/c1.patch with git apply from the repository root unless wp/WP04 already contains them (check BackCard.kt), and commit; then wp/WP06, wp/WP03 (the design system with the scroll-direction fix and iOS scrolling), wp/HUD (the performance overlay; it branched from wp/WP06). Leave out wp/WP05 and wp/WP05-glass (glass is unfinished and wp/WP05-glass still has the old rubber-band bug), wp/WP08 (iOS), wp/WP02 (generator only) and anything else, and list them as excluded with the reason. Resolve every conflict by reading both sides and each package's handoff (${OV}/handoff/*.md); record each resolution (file, what each side did, what you kept and why) in ${OV}/out/preview/NOTES.md. If a conflict cannot be resolved soundly, leave that package out and say so rather than guess.
3. Find the catalog app (the module the user ran as com.example.wp03.look.catalog/.CatalogActivity; WP03's handoff and ${OV}/out/WP03/look show how it is generated and built) and build it from the preview tree: a release build (the real feel), and, if the catalog has a dev or debug variant that carries the performance overlay, that one too. Copy each APK to ${OV}/out/share/ named android-base-catalog-preview-<variant>-<short head>.apk and write its sha256 next to it.
4. Smoke-test on the phone (9a7c74f4) with the release APK: launch, open every catalog page, scroll lists and carousels in both directions (the user's Oct 18 bug: content must move with the finger), use the back gesture on a pushed screen (the card follows the finger, cancel and commit), switch tabs, and, on the dev APK if built, show the overlay pill and open its Performance tab. Take screenshots of each page (${OV}/out/preview/shots/) and one screenrecord tour (--size 720x1600, under 60 s) of the release build. Note crashes (logcat -b crash), visual glitches and anything rough.
5. Return everything: included packages with the heads merged, excluded ones with reasons, conflicts and resolutions, APK paths with sha256, screenshot and recording paths, smoke results, rough edges, and the notes path.`, { label: 'preview:build', phase: 'Build', schema: BUILT })
if (!built) throw new Error('preview build failed')

phase('Check')
let check = await agent(`You are a separate reviewer of a preview catalog build made for the user. ${RULES} Do not modify anything except to re-take a screenshot that is missing or wrong. The builder reported: ${JSON.stringify(built)}
Check: the preview branch ${BR} contains each included package's head (git merge-base --is-ancestor) and the back-card patches if claimed; nothing was pushed and no wp/* or overhaul/* ref moved because of this work (compare with the reflog); each APK exists, its sha256 matches, and it was built from the preview tree's head (the build log); open every screenshot and a few frames of the recording (ffmpeg) and check they show what the report says; the conflict resolutions in ${OV}/out/preview/NOTES.md are sound (read the resolved files); the rough edges are honest (nothing visible in the evidence is missing from the list). Report only real problems; ok=true with an empty list is a fine answer.`, { label: 'preview:check', phase: 'Check', schema: VERDICT })
if (check && !check.ok && check.problems.length) {
  log(`check: ${check.problems.length} problems`)
  const fixed = await agent(`You are the preview builder again. ${RULES} A separate reviewer found these real problems: ${JSON.stringify(check.problems)}. Fix each one (re-resolve, rebuild, re-capture as needed) and return the full updated report.`, { label: 'preview:fix', phase: 'Check', schema: BUILT })
  if (fixed) built = fixed
}
return { built, check }
