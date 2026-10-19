export const meta = {
  name: 'coverage-audit',
  description: "Audit everything the handover, the requirements and the plan ask for against where the work really stands, to find anything forgotten or stalled (as liquid glass was): Sonnet inventories, an Opus audit against branches and handoffs, a separate Opus review",
  phases: [
    { title: 'Inventory', detail: 'three Sonnet readers list every ask, requirement, plan step and open item with its documented status' },
    { title: 'Audit', detail: 'Opus checks each item against the branches and handoffs and classifies it' },
    { title: 'Review', detail: 'a separate Opus reviewer checks the audit against the evidence, then one revision' },
  ],
}

// The user, Oct 19 19:58, after learning liquid glass had stalled since Oct 17: "and anything else u havent seen or read
// yet from the handover?". Read-only: no code, no build, no phone (other runs are using them).
// Model routing (global CLAUDE.md §10): reading documents into an inventory is mechanical, so Sonnet lists facts with
// locations; deciding what is done, stalled or forgotten is Opus, and a separate Opus reviewer checks it.
const R = 'D:/projects/tools/android-base'
const OV = 'D:/projects/tools/.overhaul'
const AD = `${OV}/audit/2026-10-19-coverage`

const RULES = `Rules: read-only everywhere except your own files under ${AD}/. Never run gradle or adb, never touch a worktree's files or branches (other runs are editing them; reading with git -C ${R} log/show/branch and reading files is fine), never commit or push. Quote at most a short phrase from a document; give its path and line.`
const READER = `You are a careful reader building an inventory. List facts only, each with the document path and line where it is stated and the status the document gives it (with the document's date); do not judge whether it is really done. Miss nothing: completeness matters more than brevity. Save the inventory to the file named below as you go.`

const INV = { type: 'object', properties: {
  items: { type: 'array', items: { type: 'object', properties: {
    id: { type: 'string' }, what: { type: 'string' }, source: { type: 'string' }, owner: { type: 'string' },
    documented_status: { type: 'string' }, status_date: { type: 'string' },
  }, required: ['id', 'what', 'source', 'owner', 'documented_status'] } },
  notes_path: { type: 'string' },
}, required: ['items', 'notes_path'] }
const AUDIT = { type: 'object', properties: {
  summary: { type: 'string' },
  forgotten: { type: 'array', items: { type: 'object', properties: { item: { type: 'string' }, why: { type: 'string' }, evidence: { type: 'string' }, suggested_owner: { type: 'string' } }, required: ['item', 'why', 'evidence', 'suggested_owner'] } },
  stalled: { type: 'array', items: { type: 'object', properties: { item: { type: 'string' }, since: { type: 'string' }, blocked_by: { type: 'string' }, evidence: { type: 'string' } }, required: ['item', 'since', 'blocked_by', 'evidence'] } },
  user_decisions_open: { type: 'array', items: { type: 'string' } },
  not_in_staging_or_release_plan: { type: 'array', items: { type: 'string' } },
  counts: { type: 'object', properties: { total: { type: 'integer' }, done_merged: { type: 'integer' }, done_on_branch: { type: 'integer' }, in_progress: { type: 'integer' }, waiting: { type: 'integer' }, not_started: { type: 'integer' }, forgotten: { type: 'integer' }, superseded: { type: 'integer' } }, required: ['total'] },
  report_path: { type: 'string' },
}, required: ['summary', 'forgotten', 'stalled', 'user_decisions_open', 'not_in_staging_or_release_plan', 'counts', 'report_path'] }
const REVIEW = { type: 'object', properties: {
  verdict: { type: 'string' },
  blocking: { type: 'array', items: { type: 'object', properties: { where: { type: 'string' }, problem: { type: 'string' }, fix: { type: 'string' } }, required: ['where', 'problem', 'fix'] } },
  nonblocking: { type: 'array', items: { type: 'string' } },
}, required: ['verdict', 'blocking', 'nonblocking'] }

phase('Inventory')
const invs = await parallel([
  () => agent(`${READER} ${RULES} Inventory file: ${AD}/inv-user.md.
Read ${OV}/handover/HANDOVER.md sections 1.2 to 1.8 (what the user said verbatim, the standing instructions, preferences, delegated decisions, decisions only the user can make, superseded instructions), ${OV}/handover/USER-MESSAGES.md, and ${OV}/REQUIREMENTS.md (every numbered item, 1 to 45, and every sub-point). One item per distinct ask, requirement or sub-requirement, and one per open user decision; ids like REQ-37.2, ASK-<date>-<n>, DEC-<n>.`, { label: 'inventory:user', phase: 'Inventory', schema: INV, model: 'sonnet' }),
  () => agent(`${READER} ${RULES} Inventory file: ${AD}/inv-plan.md.
Read ${OV}/handover/HANDOVER.md sections 2.4 to 4.9 (the template, multiplatform and Swift templates, website and API, CI, the goals, every requirement in 3.2, the definitions of done in 3.3 and 3.4, the smoothness gates in 3.5, the packages, dependencies, merge order, checkpoints, package by package in 4.6, what remains in 4.7) and its appendices, ${OV}/handover/REQUIREMENTS-STATUS-FINAL.md, ${OV}/handover/PACKAGES-AND-BRANCHES.md and ${OV}/design/MASTER-PLAN.md (its package list, checkpoints and each package's acceptance items). One item per plan step, acceptance item, checkpoint, definition-of-done line and gate; ids like PLAN-WP05.3, DOD-A.4, CP-2, GATE-<name>.`, { label: 'inventory:plan', phase: 'Inventory', schema: INV, model: 'sonnet' }),
  () => agent(`${READER} ${RULES} Inventory file: ${AD}/inv-now.md.
Read what the work says about itself today: ${OV}/STATUS.md (the top notes, section 1 and the package table), the top of "## Progress" and the sections named like "What remains", "Open issues" or "Next" in every file under ${OV}/handoff/, ${OV}/followups.md, every file under ${OV}/bugs/ (the .md files), ${OV}/notes/INDEX.md, and the headers and status lines of ${OV}/research/*-PLAN.md. For each package also record the branch head and its last commit date (git -C ${R} log -1 --format='%h %ad' --date=short <branch>) and the worktree's uncommitted file count (git -C ${OV}/wt/<WP> status --porcelain | wc -l, read-only). One item per open point, follow-up, bug, plan status line and package state; ids like NOW-WP06.2, FU-<n>, BUG-<date>.`, { label: 'inventory:now', phase: 'Inventory', schema: INV, model: 'sonnet' }),
])
const got = invs.filter(Boolean)
if (!got.length) throw new Error('every inventory failed')
if (got.length < 3) log(`${3 - got.length} inventory reader(s) failed; auditing what came back`)

phase('Audit')
const AUDIT_PROMPT = (extra) => `You are the Opus auditor of the android-base overhaul. The user asked whether anything from the handover has not been seen or acted on, after learning that liquid glass (WP05-glass) had quietly stalled since Oct 17. ${RULES}
Three Sonnet readers listed every ask, requirement, plan step and open point with its documented status; trust nothing until you check it: ${JSON.stringify(got)}. Their notes are in ${AD}/inv-*.md.
1. Merge the lists into one set of distinct items (the same requirement appears in several documents).
2. For each item decide its real status from evidence, not from the documents' claims: done and merged into overhaul/multiplatform-v2 (git merge-base --is-ancestor, tags merged/*), done on a package branch (enters staging), in progress (commits or uncommitted work in the last 2 days), waiting (on what, and is that still true), not started but owned by a package in scripts/to-release.js or scripts/consolidate-staging.js, FORGOTTEN (no owner in any running or planned script, or an owner that never picked it up, or a handoff that says "do X" with no later sign of X), or superseded (by a later user decision; say which). Read the handoffs, git log of the owning branches and the scripts as needed.
3. Find especially: items stalled more than 2 days with nobody actively unblocking them; user asks that never became a requirement or a package step; open user decisions never asked in chat; definition-of-done lines for the Android release that no package or gate covers; orchestrator notes in handoffs that asked for something no later run did (for example "merge wp/WP05-glass into wp/WP05 at the start of your next run"); bugs or follow-ups with no owner.
4. Write ${AD}/COVERAGE-AUDIT.md: first a plain-English part for the user (what is forgotten or stalled, why it matters for the app being light and smooth, what should own it, and what is fine), then the full table (item, source, owner, real status, evidence, last activity). Recommend an owner and a place in the order for each forgotten or stalled item (staging, a staging-wp/<package> branch after the user's go, or a user decision), but change nothing.${extra}`
let audit = await agent(AUDIT_PROMPT(''), { label: 'audit:coverage', phase: 'Audit', schema: AUDIT })
if (!audit) throw new Error('audit failed')

phase('Review')
const review = await agent(`You are a separate Opus reviewer of a coverage audit of the android-base overhaul. Check ${AD}/COVERAGE-AUDIT.md against the evidence: open the cited documents and lines, run the git checks behind every "done", "forgotten" and "stalled" claim (read-only), and look for items the audit missed in ${OV}/handover/HANDOVER.md 1.7, 3.2, 3.3, 4.6 and 4.7 and in ${OV}/REQUIREMENTS.md. ${RULES}
The auditor's summary: ${JSON.stringify(audit)}
Blocking means: a status claim the evidence contradicts; an item called forgotten that a running or planned script owns (or the reverse); a missed requirement, definition-of-done line or open user decision; a recommendation that conflicts with the user's decisions (REQUIREMENTS 44: staging first, no package work before the user's go). There is no pressure to find something. Do not modify anything.`, { label: 'review:coverage', phase: 'Review', schema: REVIEW })
if (review && review.blocking.length) {
  log(`review: ${review.blocking.length} blocking`)
  const revised = await agent(AUDIT_PROMPT(`\nThis is a revision. A separate reviewer found these blocking problems in your first version: ${JSON.stringify(review.blocking)}. Nonblocking: ${JSON.stringify(review.nonblocking)}. Fix each in the report and return the full updated result.`), { label: 'audit:revise', phase: 'Review', schema: AUDIT })
  if (revised) audit = revised
}
return { audit, review }
