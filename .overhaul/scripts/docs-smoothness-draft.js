export const meta = {
  name: 'docs-smoothness-draft',
  description: "Draft the template's smoothness documentation (REQUIREMENTS 50) from everything the overhaul learned: a Sonnet inventory of the rules every package said the docs must state and of the research, then Opus writes the performance guide and the AGENTS.md rules, then a separate Opus review. Draft only, outside the repository; WP12 places it after staging",
  phases: [
    { title: 'Inventory', detail: 'Sonnet lists every rule, gotcha, number and lesson with its source' },
    { title: 'Write', detail: 'Opus drafts PERFORMANCE.md and the AGENTS.md rules for generated projects' },
    { title: 'Review', detail: 'a separate Opus review against the sources and the code, then one revision' },
  ],
}

// The user, Oct 20 01:03: "Whatever changes telegram research brings and I approve add them in the docs too regarding how
// to make smooth and keep the project running smooth..updated the docs too with all knowledge later" (REQUIREMENTS 50), and
// 01:12: use the full quota every window. Staging is the only package work running (REQUIREMENTS 44), so this drafts the
// knowledge pass now, read-only, outside the repository; WP12 commits it on its staging-wp branch after the user's go.
// Model routing (global CLAUDE.md §10): collecting the rules into an inventory is mechanical (Sonnet, facts with paths);
// writing what developers will read and reviewing it are Opus.
const OV = 'D:/projects/tools/.overhaul'
const R = 'D:/projects/tools/android-base'
const DD = `${OV}/docs-draft`

const RULES = `Rules: read-only everywhere except your own files under ${DD}/. Never run gradle or adb, never touch a worktree under ${OV}/wt/ or the staging worktree (another run is editing it), never commit or push. Read the template from the staging branch head with git -C ${R} show staging:<path> or git -C ${R} grep -n <pattern> staging -- templates/android.`

const INV = { type: 'object', properties: {
  items: { type: 'array', items: { type: 'object', properties: {
    topic: { type: 'string' }, rule: { type: 'string' }, why: { type: 'string' }, number: { type: 'string' }, source: { type: 'string' },
    status: { type: 'string', enum: ['in-code', 'requirement', 'proposal-pending-approval', 'lesson'] },
  }, required: ['topic', 'rule', 'source', 'status'] } },
  notes_path: { type: 'string' },
}, required: ['items', 'notes_path'] }
const DRAFT = { type: 'object', properties: {
  summary: { type: 'string' },
  files: { type: 'array', items: { type: 'string' } },
  pending: { type: 'array', items: { type: 'string' } },
  open_questions: { type: 'array', items: { type: 'string' } },
}, required: ['summary', 'files', 'pending', 'open_questions'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Inventory')
const inv = await agent(`You are a careful reader building an inventory. List facts only, each with its source path and line; do not write documentation and do not judge. Save the inventory to ${DD}/inventory.md as you go. ${RULES}
Collect every rule, gotcha, threshold and lesson about keeping a Compose app smooth and light that the overhaul recorded: the section "Rules and gotchas the docs must state" (and similar) in every file under ${OV}/handoff/; ${OV}/research/MOTION-PLAN.md, SHEET-SPEC.md, SMOOTHNESS-PLAN.md, RECOMPOSITION-PLAN.md, compose-perf.md, recomposition.md, back-google/DESIGN.md; ${OV}/research/district/RESEARCH.md and PHONE-RESULTS.md; ${OV}/research/telegram/RESEARCH.md; ${OV}/research/hud-metrics/DESIGN.md; ${OV}/REQUIREMENTS.md (19, 21, 22, 24, 28, 29, 38, 41, 42, 46, 49, 50); the lessons in ${OV}/decisions/LOG.md; templates/android/tools/perf/gates.toml and the existing docs on staging (templates/android/AGENTS.md, docs/ARCHITECTURE.md, docs/PERFORMANCE-OVERLAY.md). Mark each item in-code (the template already does it; cite file:line on staging), requirement, proposal-pending-approval (District and Telegram proposals and the overlay metrics the user has not approved yet), or lesson.`, { label: 'inventory:rules', phase: 'Inventory', schema: INV, model: 'sonnet' })
if (!inv) throw new Error('inventory failed')

phase('Write')
const WRITE = (extra) => `You are the technical writer and performance lead of android-base, at the Google/AOSP bar. ${RULES}
Write the draft of the smoothness documentation for projects generated from the template (REQUIREMENTS 50), from this inventory (verify each item you use against its source; drop what the source does not say): ${JSON.stringify(inv)}.
Write into ${DD}/:
1. PERFORMANCE.md for templates/android/docs/: how an app built on the template stays light and smooth on a budget phone at 120 Hz. Lead with the few rules that matter most, then by area (recomposition and state reads; lists and very large data; screen transitions, the back gesture and the back card; sheets; images; motion and springs; glass tiers; keyboard; start-up and baseline profiles; memory), each rule with why it keeps frames cheap, how to apply it in a screen (a short Compose sketch written from Compose's own APIs, never copied from another project), the anti-pattern, and the number it protects (from gates.toml or the measurements). Then how to measure: the performance overlay, the perf harness and the gates, and how to read a red row. Use the template's real names (AppTheme, AppSheet, MviScreen, the navigation shell) and check every name exists on staging.
2. AGENTS-rules.md: the short, enforceable rules to merge into templates/android/AGENTS.md (one line each, with the check that enforces it where one exists).
3. PENDING.md: the techniques that wait for the user's approval (District and Telegram proposals, the overlay metrics), each a ready-to-merge paragraph, so the docs ship in the same change as the code once approved. Nothing pending may appear in PERFORMANCE.md as a rule.
Plain, precise English; no marketing; no claims beyond the evidence.${extra}`
let draft = await agent(WRITE(''), { label: 'write:docs', phase: 'Write', schema: DRAFT })
if (!draft) throw new Error('writing failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer at the Google/AOSP bar. Check the drafts in ${DD}/ (PERFORMANCE.md, AGENTS-rules.md, PENDING.md) against their sources (${DD}/inventory.md and the files it cites) and against the template on the staging branch (git show). ${RULES} Do not modify anything.
The writer's summary: ${JSON.stringify(draft)}
Blocking means: a rule the code or the sources contradict; a name, API or file that does not exist on staging; a pending proposal presented as a rule; a Compose sketch that would itself cost frames, allocate per frame or recompose per frame; a number that does not match gates.toml or the measurement it cites; copied third-party code. There is no pressure to find something.`, { label: 'review:docs', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(WRITE(`\nThis is a revision. A separate reviewer found these blocking problems: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in the files and return the full updated result.`), { label: 'write:revise', phase: 'Review', schema: DRAFT })
  if (revised) draft = revised
}
return { draft, review }
