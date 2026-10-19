export const meta = {
  name: 'win-linux-fix',
  description: 'Fix the two failures the first Linux CI runs found after WIN merged: the --git repository is dirty on Linux, and the generated projects fail to compile an added data module (AppResult). Fix with regression tests, then a Google-bar review loop',
  phases: [
    { title: 'Fix', detail: 'root cause, regression tests, fix, acceptance' },
    { title: 'Review', detail: 'separate reviewer, then fixes' },
  ],
}

// The user approved a CI-only draft PR (Oct 19 11:20) and asked to keep it open until Linux CI is green (11:40).
// WIN's Oct 19 fixes merged at 3a0d38e with two Linux failures still open, so they are fixed here, on wp/WIN.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const WT = `${OV}/wt/WIN`
const L = `${OV}/bin/locked`
const PR = 'https://github.com/tripathisarthak457/android-base/pull/1'

const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md §6 if you have not. Work only in ${WT} on branch wp/WIN. Gradle only through ${L} gradle, adb only through ${L} adb, heavy Python through ${OV}/bin/memwait; long commands in the background with logs under ${OV}/logs/WIN/, polled. Google/AOSP-level code (${OV}/research/CODE-STANDARDS.md); never weaken, skip or delete a test to get to green: a test that was wrong gets fixed and the handoff says why. Commit each completed step with a message that says why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"; stage explicit paths. Never push (the orchestrator pushes; pushes re-run Linux CI on the draft PR ${PR}), never merge into the integration branch, never touch main.`

const STATUS = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'blocked'] },
  summary: { type: 'string' },
  commits: { type: 'array', items: { type: 'string' } },
  verified: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'summary', 'commits', 'verified'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Fix')
let r = await agent(`You are the engineer for package WIN (the generator on every host). ${RULES}
Read the orchestrator notes at the top of ${OV}/handoff/WIN.md "## Progress" (Oct 19 12:18 and 13:02) and the CI logs they cite (gh run view <id> --log-failed in ${R}; the newest runs are on branch wp/WIN). Two Linux failures remain after WIN's Oct 19 merge (3a0d38e):
1. tests.test_zip.ZipTest.test_the_commit_holds_a_file_past_windows_path_limit: right after the --git commit, git status shows " M gradlew" and " M tools/perf/perf.sh" on Linux. Find the root cause (most likely the executable bit: what the index records versus the files on disk, or core.fileMode in the generated repository) and fix it in the generator so a project generated with --git is clean on Linux, macOS and Windows. The test must keep asserting a clean tree.
2. The four generated-project CI jobs fail to compile :data:orders (Unresolved reference 'AppResult' in OrdersRepository.kt lines 3, 18, 26). Find where the orders data module comes from (ci.yml's steps after WIN's rewrite in ae886b5, or a generator template such as add-feature's data template) and why AppResult is missing in that project (renamed or moved in core, an import the template no longer gets, a step that adds a feature the template does not support). Reproduce it here: generate exactly as ci.yml does and compile that module through ${L} gradle. Fix it at its root (the template or the step, whichever is wrong), with a regression test in the generator suite that fails before the fix (a generated project with that data feature must have every reference resolvable, or compile).
Step 0: merge overhaul/multiplatform-v2 (3a0d38e) into wp/WIN if it is not already contained.
Then: the generator suite through memwait (python -m unittest discover -s tests -t . in generator/), the consistency quick check, and one generated preset with the data feature built through ${L} gradle (assembleDebug of the module at least). Write a Progress entry in ${OV}/handoff/WIN.md (root causes with file:line, commits, tests, what you ran). Return state done only when both are fixed and verified here.`, { label: 'WIN:linux-fix', phase: 'Fix', schema: STATUS })
if (!r) throw new Error('fix agent failed')

phase('Review')
let review = null
for (let round = 1; round <= 3; round++) {
  review = await agent(`You are a separate reviewer at Google/AOSP level for package WIN. Review every commit on wp/WIN after 3a0d38e in ${WT} (git log 3a0d38e..wp/WIN; git diff 3a0d38e...wp/WIN), against the two Linux failures in the orchestrator notes at the top of ${OV}/handoff/WIN.md and the fix agent's report: ${JSON.stringify(r)}. Check the root causes are real (not symptoms), the regression tests fail on the old code and pass now (run them; generator tests through ${OV}/bin/memwait), nothing is skipped or weakened, the change holds on Linux, macOS and Windows (read it as each host would run it), comments explain why, and the handoff is accurate. Blocking means a real defect or a missing proof; everything else is nonblocking. There is no pressure to find something. Do not modify anything.`, { label: `review:WIN-linux#${round}`, phase: 'Review', schema: REVIEW })
  if (!review || !review.blocking.length) break
  log(`review round ${round}: ${review.blocking.length} blocking`)
  r = await agent(`You are the WIN engineer again. ${RULES}
A separate reviewer found these blocking problems in your Linux fixes: ${JSON.stringify(review.blocking)}. Nonblocking notes (fix the cheap ones): ${JSON.stringify(review.nonblocking)}. Fix each with proof, re-run what they touch, update the handoff, and return.`, { label: `WIN:linux-fix#${round + 1}`, phase: 'Review', schema: STATUS })
  if (!r) throw new Error('fix round failed')
}
return { fix: r, review, reviewClean: !!review && !review.blocking.length }
