export const meta = {
  name: 'noui-text-fix',
  description: "REQUIREMENTS 59: fix the five findings the final No-UI reviews left (untrue feature copy, README and AGENTS.md claims) on staging-wp/NOUI-FIX while the gate runs on staging-wp/NOUI, then re-review them; never pushes",
  phases: [
    { title: 'Fix', detail: 'the five remaining findings, test-first where they are code' },
    { title: 'Review', detail: 'a fresh Opus check that each finding is closed and nothing new is untrue' },
  ],
}

// The setup-only run's review round 3 (run wf_9f5e0ac2-9d6) left five blocking findings, all about text that would show on
// the setup-only production site or in the docs. Its script has no third fix round, and its gate is running on
// staging-wp/NOUI, so these fixes go on their own branch from the same head and merge after the gate.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const WT = `${OV}/wt/NOUI-FIX`
const HO = `${OV}/handoff/NOUI.md`
const FINDINGS = `${OV}/release/noui-review3-blocking.json`
const RULES = `Rules: read D:/projects/tools/CLAUDE.md (Windows rules) and ${OV}/handover/HANDOVER.md section 6 first if you have not; ${OV}/REQUIREMENTS.md item 59 is the user's spec. Read the orchestrator note at the top of ${HO} (never run Python by a bare 'python' in a stripped environment; use C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe). Heavy Python and npm through ${OV}/bin/memwait; Gradle only through ${OV}/bin/locked gradle (the gate holds it; you should not need it). Emitted files (web/frontend/lib/catalogue.json) are regenerated with the generator's own --write, never hand-edited. Commit with messages that say why, ending with "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>", explicit paths only. NEVER push, never touch main, never delete a branch or a worktree, never stash, never rewrite history. Work only in ${WT} (branch staging-wp/NOUI-FIX); never touch ${OV}/wt/NOUI, where the gate is running.`
const BUILT = { type: 'object', properties: {
  state: { type: 'string', enum: ['done', 'partial', 'blocked'] },
  head: { type: 'string' },
  commits: { type: 'array', items: { type: 'string' } },
  checks: { type: 'array', items: { type: 'object', properties: { check: { type: 'string' }, result: { type: 'string' }, log: { type: 'string' } }, required: ['check', 'result', 'log'] } },
  open: { type: 'array', items: { type: 'string' } },
}, required: ['state', 'head', 'commits', 'checks', 'open'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { file: { type: 'string' }, line: { type: 'integer' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['file', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

let blocking = null
for (let round = 1; round <= 2; round++) {
  const task = round === 1 ? `the five findings in ${FINDINGS}` : `what the re-review found: ${JSON.stringify(blocking)}`
  const fx = await agent(`You fix ${task} on staging-wp/NOUI-FIX in ${WT}. ${RULES}
Every kept feature's No-UI copy must say only what a No-UI project really has: the logic, ViewModel and state, the placeholder route, and what the developer's own screen must draw or call; nothing about screens, walkthroughs, permission prompts at the end of onboarding, or links held until onboarding, unless the No-UI code really does it (read the generated No-UI project to check each claim: generate one with the CLI into ${OV}/out/NOUI/textfix-${round}/). For the design_style and motion_style claim in AGENTS.md: decide on evidence whether the generator itself should enforce the option's [hides] (a spec with ui_components false and a non-default style) or whether the text should say that the clients enforce it; if code, test-first, and keep the unticked output byte-identical. Then regenerate the catalogue with --write, run the generator tests that cover options, copy and docs, the catalogue check, and the website's typecheck and tests (memwait), and fix what they find. Write what you did under "## Text fix round ${round}" in ${HO}. Return the state, head, commits, checks and what is open.`, { label: `textfix:fix#${round}`, phase: 'Fix', schema: BUILT })
  if (!fx) throw new Error('fix failed')
  const rv = await agent(`You are a fresh Google/AOSP-level reviewer. ${RULES} Do not modify anything. Check on staging-wp/NOUI-FIX in ${WT} (git diff staging-wp/NOUI..staging-wp/NOUI-FIX) that each finding in ${FINDINGS}${round > 1 ? ' and each one of the previous re-review' : ''} is closed, and that every No-UI text the site, the CLI's --list-features --no-ui-components, the wizard, the README, AGENTS.md and the generated docs show is true of a generated No-UI project (generate one into ${OV}/out/NOUI/textfix-review-${round}/ and check each claim against its code). Blocking: an untrue claim, a broken test, an emitted file edited by hand, a change to UI projects' output beyond what the plan allows. There is no pressure to find something.`, { label: `textfix:review#${round}`, phase: 'Review', schema: REVIEW })
  blocking = rv ? rv.blocking : [{ file: '-', problem: 'the re-review failed to run', fix: 'run it again' }]
  if (!blocking.length) break
}
return { open: blocking }
