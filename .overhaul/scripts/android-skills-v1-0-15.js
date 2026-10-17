export const meta = {
  name: 'android-skills-v1-0-15',
  description: 'Fold Google Android skills v1.0.15 (5 new, 10 updated) into the overhaul: digest, feature-aware vendoring into generated projects with AGENTS/CLAUDE guidance, review loop, and notes to the agents that need the new rules',
  phases: [
    { title: 'Vendor', detail: 'feature-aware vendoring in the generator (wt/WP02)' },
    { title: 'Review', detail: 'fresh-context review and fixes' },
  ],
}

// Fable first; after a failed Fable call (weekly limit) the rest of the run uses Opus (the user, Oct 15).
// Oct 15, 20:55: the user switched every agent to Opus 5.5 only, so Fable is skipped from the start.
let fableOut = true
async function fableAgent(prompt, opts) {
  if (!fableOut) {
    const r = await agent(prompt, Object.assign({}, opts || {}, { model: 'fable' }))
    if (r !== null && r !== undefined) return r
    fableOut = true
    log('a Fable call failed; the rest of this run uses Opus')
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
const SK = 'C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills'
const WT = OV + '/wt/WP02'
const NEWS = `Google's Android skills repository was updated from v1.0.13 (42dc227) to v1.0.15 (5bda39f) on Oct 12 and Oct 14. The plugin's clone at ${SK} is shallow (5bda39f only) and, on this Windows machine, its working files have CRLF line endings while the repository stores LF: read and copy the v1.0.15 files from git objects (git -C ${SK} show 5bda39f:<path>, or git -C ${SK} -c core.autocrlf=false archive 5bda39f <paths> | tar -x), never from the working files, and never change that clone.
- New: jetpack-compose/jetpack-compose-performance, system/glance-canonical-layouts, wear/wear-widgets, cars/android-for-cars-parked-apps, device-ai/litert/litert-runtime.
- Updated: navigation/navigation-3, navigation/navigation-event, jetpack-compose/adaptive, profilers/android-profiler, testing/testing-setup, camera/camerax, play/engage-sdk-integration, play/play-billing-library-version-upgrade, wear/wear-compose-m3, and the xr glimmer skill.
The user wants the new skills incorporated and wants every project their generator produces to use them.`

const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'blocked'] }, summary: { type: 'string' } }, required: ['state', 'summary'] }
const REVIEW = { type: 'object', properties: { verdict: { type: 'string' }, blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } }, nonblocking: { type: 'array', items: { type: 'string' } } }, required: ['verdict', 'blocking', 'nonblocking'] }

// The digest and its feature-aware vendoring map (research/ANDROID-SKILLS-DIGEST.md) were updated for v1.0.15 on
// Oct 15; the vendoring below follows it.
phase('Vendor')
const IMPL = `${NEWS}
You implement the skills update in the generator. Worktree ${WT} on branch wp/WP02 (WP02-M2 owns vendoring. Read ${OV}/handoff/WP02-M2.md and ${OV}/handoff/WP02-M2-RESUME.md first.) The orchestrator launches you only while WP02-M2's own run is waiting on other merges; if the worktree nevertheless has uncommitted changes that are not yours, or the handoff's Progress shows WP02-M2 mid-step, return state "blocked" with that evidence instead of editing. WP02-M2 already vendors Google's skills into the template, with a tool that refreshes them: find it, and use it.
Do, per the updated vendoring map in ${OV}/research/ANDROID-SKILLS-DIGEST.md:
1. Refresh the vendored skills to v1.0.15, byte for byte, with LICENSE and NOTICE. The refresh tool copies a checkout's working tree, so give it an LF checkout made from the local clone (no download): git clone -c core.autocrlf=false --branch v1.0.15 ${SK} "$LOCALAPPDATA/Temp/sk1015" (a short path), run the tool against that folder, check every vendored file is byte-identical to git -C ${SK} show 5bda39f:<path>, then send the temporary folder to the Recycle Bin (PowerShell: [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory(path, 'OnlyErrorDialogs', 'SendToRecycleBin')), never a permanent delete.
2. Add the new skills that apply.
3. Make vendoring feature-aware, so a generated project carries exactly the skills its features use: through the generator's feature specs (the files a feature owns) and the markers, the way other optional files work.
4. Update the template's AGENTS.md and CLAUDE.md (and the other agents' entry files) with when to use each skill (jetpack-compose-performance is vendored into every Android project, and the entry files tell an agent to run its audit on any Compose UI change before it is done), the generator repository's own AGENTS.md likewise, and the README's feature or skills table if one exists.
5. Add generator tests: a project with and without each feature gets the right skills folders and AGENTS.md lines.
6. Run the generator unit tests and the quick consistency matrix, through ${OV}/bin/memwait (Gradle only via ${L} gradle if needed). Windows, native: your Bash tool is Git Bash and lacks JAVA_HOME, ANDROID_HOME and GRADLE_USER_HOME, which ${L} and memwait set; never run gradlew bare.
Commit each step with a message that explains why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"; stage explicit paths only. Never push. Never kill or stop processes you did not start.
Append a section "Android skills v1.0.15 (Oct 15)" to ${OV}/handoff/WP02-M2.md: what changed, the commits, the test results.`
// The user (Oct 16): use the new Compose performance skill on the template itself, not only vendor it.
const audit = fableAgent(`You audit the Android template of the android-base generator with Google's new jetpack-compose-performance skill (${SK}/jetpack-compose/jetpack-compose-performance/SKILL.md). Follow its "Optimization audit workflow" exactly: an exhaustive scan of every @Composable, modifier chain and lazy-layout model against all seven categories, and read the matching references/*.md before you report any finding.
Scope, at each package's branch head (read with git -C ${R} show <branch>:<path>, or in the worktrees ${OV}/wt/<WP>): the design system on wp/WP03 (templates/android/core/designsystem; its 47 uncommitted sheet files in ${OV}/wt/WP03 too), navigation and the shell on wp/WP04 (templates/android/core/navigation), the glass on wp/WP05-glass (foundation/glass, component/navigation/LiquidTabs.kt), and the sample feature screens on wp/WP04.
For each finding: the file and line, the skill's rule and reference, the concrete fix, the expected effect on frames (recompositions, phases, allocations), the owner package (WP03 design system, WP04 navigation and shell, WP05 glass, WP02 feature scaffolding), and whether it is BLOCKING for the release (a per-frame recomposition, a backwards write, an allocation in draw, or an unstable parameter on a hot path) or a follow-up. Note what the template already does right, briefly.
Write ${OV}/research/COMPOSE-PERF-AUDIT.md. Then append an "Orchestrator note (Oct 16): Compose performance skill audit" section to each owner's handoff (${OV}/handoff/WP03.md, WP04.md, WP05.md, WP05-glass.md, WP02-M2.md) listing its findings, saying they are part of its acceptance and that reviewers will check them. Do not modify any repository. Return a summary with the counts per owner.`, { label: 'audit:compose-perf', phase: 'Vendor', schema: STATUS })
let r = await fableAgent(IMPL, { label: 'vendor:skills', phase: 'Vendor', schema: STATUS })
if (!r) throw new Error('vendoring failed')

phase('Review')
for (let round = 1; round <= 3; round++) {
  const rv = await fableAgent(`You are a fresh-context reviewer. Review the Android skills v1.0.15 update on wp/WP02: git -C ${R} log --oneline -15 wp/WP02, then the diff of its skills commits. Check them against ${OV}/research/ANDROID-SKILLS-DIGEST.md (the vendoring map), the upstream files at 5bda39f (vendored files must be byte-identical to git -C ${SK} show 5bda39f:<path>; the clone's working files have CRLF on this machine, so never compare against those), the licence notices, the generator's feature mechanism, and the tests.
BLOCKING:
- wrong or missing skills for a feature;
- modified vendored files;
- a missing LICENSE or NOTICE;
- AGENTS.md or CLAUDE.md guidance that is wrong or vague;
- skills leaking into projects without the feature;
- broken or missing tests;
- slop.
Do not modify anything; cite file:line.`, { label: `review:skills#${round}`, phase: 'Review', schema: REVIEW })
  if (!rv) throw new Error('review failed')
  if (!rv.blocking.length) { log(`skills review #${round}: clean`); break }
  r = await fableAgent(`${IMPL}\n\nMODE: fix. A fresh-context reviewer found BLOCKING issues. Fix every one (commits with why-messages), re-run the tests, update the handoff, and return the same schema:\n${JSON.stringify(rv.blocking, null, 1)}\nNon-blocking: ${JSON.stringify(rv.nonblocking)}`, { label: `vendor:fix#${round}`, phase: 'Review', schema: STATUS })
  if (!r) throw new Error('fix failed')
}
const a = await audit
return { vendor: r.summary, audit: a ? a.summary : 'audit failed' }
