export const meta = {
  name: 'merge-all-to-main',
  description: 'The user asked (Oct 16) to merge all code of android-base and of PeriFlow into main and delete every branch afterwards, before wiping the laptop: archive-tag every branch head, merge everything into main, push, delete the branches, update the handover docs (including the offline pen-drive zip), and verify from fresh clones',
  phases: [
    { title: 'Merge', detail: 'android-base and PeriFlow, all branches into main' },
    { title: 'Docs', detail: 'handover docs for the one-branch layout and the offline zip' },
    { title: 'Verify', detail: 'fresh clones, ancestry of every archived head, secrets, cold read' },
  ],
}

const TOOLS = '/home/crazyhusir/Documents/projects/tools'
const R = TOOLS + '/android-base'
const OV = TOOLS + '/.overhaul'
const H = OV + '/handover'
const PF = '/home/crazyhusir/Documents/projects/jorwen/periflow'
const ZIP = TOOLS + '/backup-2026-10-16'
const SCRATCH = '/tmp/claude-1000/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/scratchpad/merge-all'

const CONTEXT = `CONTEXT. The user (Sarthak, GitHub tripathisarthak457) will wipe this laptop and move to Windows. They said in chat on Oct 16: "merge all the code of android base in the main branch and delete all the branches after merge is fully done of all branches. same for periflow too." They also said: "nothing should not be left unbacked up on this machine", and that anything that cannot go to GitHub goes into ${ZIP} as zips for a pen drive, and that the notes must say Claude on Windows will get that zip somehow. The user uploads it to Google Drive (Oct 16: "i am gonna upload to google drive over pendirve") and downloads it on Windows.
- android-base: ${R}, origin github.com/tripathisarthak457/android-base (PUBLIC). Vercel deploys the website from main. The user confirmed the merge (Oct 16: "u merge it pls i get confused in lots of branches will stop vercel to deploy that we will do after testing"), so deploys from main get paused in vercel.json before the push.
  - The branches were already pushed and verified by the handover-backup workflow (see ${H}/PUSHED.md): overhaul/multiplatform-v2 (integration), wp/WP01..WP10, wp/WP03-skel, wp/WP04-perf, wp/WP05-glass, backup/*-wip-2026-10-16 (WIP snapshots of uncommitted work, may not compile), the orphan branch overhaul-workspace (the .overhaul workspace, handover docs and Claude memory), and the merged/* tags.
  - Worktrees live in ${OV}/wt/<name>, plus ${OV}/scratch/WP09-trial.
- PeriFlow: ${PF}, origin github.com/tripathisarthak457/periflow, which now redirects to github.com/JorwenLabs/periflow (PRIVATE, an organization repo). Before deleting branches there, list the organization's other members (gh api orgs/JorwenLabs/members) and record them in the MERGE-ALL file. The user asked for it, and the archive tags keep every branch restorable.
  - Branches: main, develop, feat/p5..p11-*, fix/p7-device-verification, worktree-agent-* (local only), overhaul/smooth-glass (the overhaul's PeriFlow work, worktree ${OV}/wt/WP11), backup/smooth-glass-wip-2026-10-16 (if the backup workflow pushed it), and remote-only branches such as feat/p0-foundation-docs, feat/p6-android-foundation and claude/full-audit-bug-fixes-*.
  - Some branches are checked out in other worktrees (git worktree list), which may hold uncommitted work.
  - NEVER commit periflow-recovery-codes.txt or PeriFlow-Postman/ (secrets; the offline zip holds them).

Rules:
- No Gradle builds and no emulator: CPU heat powers the laptop off. Quick Python tests at nice -n 19 are fine.
- Never kill processes. Never force-push. Never rewrite published history.
- Never commit secrets: check every merge's new files for keystores, google-services.json with real keys, local.properties, .env and tokens.
- Commit trailer: "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>".
- If a push or deletion is refused (by the auto-mode classifier, branch protection or GitHub), stop at that step and return blocked, with the exact commands for the user to run.`

const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'blocked'] }, summary: { type: 'string' } }, required: ['state', 'summary'] }
const VERIFY = { type: 'object', properties: { ok: { type: 'boolean' }, gaps: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['ok', 'gaps', 'summary'] }

const MERGE_STEPS = (repo, name, order) => `Do it in this order, verifying each step:
1. Inventory: git -C ${repo} fetch --all --prune. List every local and remote branch and every worktree (git worktree list) with its head and its dirty state. Write the list to ${H}/MERGE-ALL-${name}.md (create the file; you keep appending to it).
2. Nothing uncommitted may be lost. For every worktree (and the main checkout) with uncommitted or untracked non-ignored files that no backup/*-wip branch already captures exactly:
   - make a WIP snapshot branch backup/<branch>-wip-2026-10-16 with a temporary index (GIT_INDEX_FILE=$(mktemp); git read-tree HEAD; git add the explicit changed paths; git write-tree; git commit-tree -p HEAD; git branch ...). Never use git add -A on a real index.
   - Exclude secrets and the never-commit files.
3. Archive tags: for EVERY branch (local and remote-only, except main), create an annotated tag archive/<branch name>-2026-10-16 at its head, with the message "Branch <name> as it was before merging everything into main (2026-10-16)". Push all the archive tags to origin. The tags keep every branch state reachable after the branches are deleted.
4. Merge everything into main, in a sensible order (${order}). Use one --no-ff merge commit per branch, with a message saying what the branch was, plus "merged in the pre-wipe consolidation, not reviewed". Resolve conflicts carefully (keep the newer and more complete work; read the handoffs to decide), and record every conflicted file and the choice made in the MERGE-ALL file. Branches already contained in main need no merge.
5. Run a quick check that does not heat the CPU (Python unit tests or lint at nice -n 19). WIP code is expected to fail some checks; record the results. Do not block on them.
6. Push main to origin (no force).
7. Delete the branches, now that each one's archive tag is an ancestor of main (check with git merge-base --is-ancestor for every tag):
   - remove worktrees that only held those branches (git worktree remove --force, after confirming their uncommitted state is in a WIP snapshot that is now in main);
   - delete the local branches;
   - delete the remote branches with git push origin --delete <branch>, for every remote branch except main.
   Keep the archive and merged tags.
8. Append a final section to the MERGE-ALL file: main's new SHA, every archive tag with its SHA, the branches deleted, the worktrees removed, and anything left (with the reason).`

phase('Merge')
const ab = await agent(`${CONTEXT}

TASK: android-base, every branch into main. ${MERGE_STEPS(R, 'android-base', 'overhaul/multiplatform-v2 first; then wp/WP05-glass, because it already merges wp/WP04 and wp/WP03 with a tested conflict resolution (commit 282dd3c); then wp/WP04, wp/WP04-perf, wp/WP03-skel, wp/WP03, wp/WP02, wp/WP06, wp/WP07, wp/WP08 and any remaining wp/*; then the backup/*-wip-2026-10-16 snapshots; and last overhaul-workspace')}
Special case, the overhaul-workspace branch. Its early commits (a961701, 7feafaf) hold an unredacted WIPE-READINESS.md naming the user's work-org account, colleagues and client files; the branch head is redacted. Bring in ONLY its current tree, WITHOUT its history, so that main never reaches those commits:
  - git read-tree --prefix=overhaul-workspace/ -u overhaul-workspace, on main, then git commit (one ordinary parent: main). No merge, no --allow-unrelated-histories.
  - Do NOT create an archive tag for overhaul-workspace. Then delete the branch (local and remote, like the others), so that the unredacted commits are no longer reachable from any ref.
  - Before committing, check that overhaul-workspace/.overhaul/handover/WIPE-READINESS.md is the redacted version: no work-organisation account names, no colleague names, no client document names. Redact anything left.
  - Add to PUSHED and the MERGE-ALL file a note for the user: GitHub may keep the old commits viewable by SHA for a while; if that matters, GitHub Support can purge them.
Then add a short pointer at the top of main's root CLAUDE.md (and AGENTS.md if it exists): "Overhaul handover: read overhaul-workspace/HANDOVER.md first."
The draft release overhaul-raw-data-2026-10-16 stays as it is.
VERCEL (the user, Oct 16: "will stop vercel to deploy that we will do after testing"): BEFORE pushing main, commit to the root vercel.json (merge into its existing JSON, do not drop keys) "git": { "deploymentEnabled": { "main": false } }, with the message "Pause Vercel deploys from main until the consolidated code is tested" and a why-line. Vercel reads vercel.json from the pushed commit, so the merged main does not deploy. Also tell the user to pause auto-deploy in the Vercel dashboard. The docs say how to re-enable it after testing: remove the key, or deploy by hand.`, { label: 'merge:android-base', phase: 'Merge', schema: STATUS })
if (!ab) throw new Error('android-base merge failed')

const pf = await agent(`${CONTEXT}

TASK: PeriFlow, every branch into main. ${MERGE_STEPS(PF, 'periflow', 'develop first; then the feat/fix branches by phase number (p0 ... p11); then claude/* and worktree-agent-*; then overhaul/smooth-glass; and last the backup/*-wip snapshots')}
PeriFlow is private, so its main is not a public deploy. Note in the MERGE-ALL file which branches were stale or superseded, so that the user can judge whether a merge brought back old code (the archive tags allow undoing it).`, { label: 'merge:periflow', phase: 'Merge', schema: STATUS })
if (!pf) throw new Error('periflow merge failed')

phase('Docs')
const docs = await agent(`${CONTEXT}

Fix these gaps from the backup verification too:
- The draft release now exists: overhaul-raw-data-2026-10-16 on android-base, with 8 of 9 archives; overhaul-private-app-listings was held back (it may contain an API key). Its 3 files exist only in the Drive zip p01. Say so plainly wherever the docs still call the release pending.
- HANDOVER's zip counts: the Drive backup is 25 zips plus 5 movies (~56 GB), including 11-late-changes-and-missed.zip; the user re-runs late-refresh.py just before the wipe.
- claude-memory/laptop-memory-freeze.md: the Windows/WSL2 setup is the RECOMMENDED one, not a fact yet.
- SESSION-2026-10-16.md was repaired (the NUL bytes from the crash were replaced by the rewritten 10:55 section); HANDOVER's 'trailing padding' remark is wrong, so fix it.
The merges are done (android-base: ${ab.state}: ${ab.summary.slice(0, 600)}; PeriFlow: ${pf.state}: ${pf.summary.slice(0, 600)}).
TASK: update the handover documents so that they describe the NEW layout. Edit them in ${R}/overhaul-workspace/ (main) and in ${H}/:
- HANDOVER.md, PACKAGES-AND-BRANCHES.md, RESUME-ON-WINDOWS.md, CLAUDE.md, tools-CLAUDE.md and README.md, plus MERGE-ALL-*.md copied in.
- The layout:
  - Everything is on main in both repos, and the branches were deleted.
  - Every former branch is reachable as the tag archive/<branch>-2026-10-16.
  - The package work was merged WITHOUT review, and the WIP snapshots are in main, so name exactly which parts do not compile or are unfinished (e.g. WP03's sheet system, SheetFlingBehavior.kt:88).
  - Vercel deploys from main are paused (vercel.json git.deploymentEnabled.main = false) until the user has tested; say how to re-enable them.
  - The workspace now lives in main under overhaul-workspace/. On Windows, it is restored to tools/.overhaul (or used in place). Give the exact steps, and fix every path that referred to the old branches or worktrees.
- How to continue the overhaul from here: the plan for re-creating package branches from the archive tags, OR working on main directly. Decide which is better for the user (one branch was their wish), and say how the Workflow scripts must change (worktrees, branch names, merge steps).
- Add a section "The offline backup (Google Drive)": the user uploads ${ZIP} (zips under 3.9 GB, README-RESTORE.md, MANIFEST.txt and SHA256SUMS.txt) to their Google Drive, as a folder named backup-2026-10-16, and downloads it on Windows. Say what is in it (read ${ZIP}/README-RESTORE.md and PLAN.md if they exist; otherwise describe the plan: secrets, Claude config and memory, local-only overhaul files, git bundles, ignored repo files, documents, downloads and media). Tell Claude on Windows to ask the user to download it from Google Drive (or to look for it in Downloads), to check SHA256SUMS, to restore the secrets (01-secrets: plain zip) and the Claude memory before resuming. Add the same to CLAUDE.md in brief.
- Sync any newer workspace files into main's overhaul-workspace/.overhaul/ (compare mtimes against ${OV}; at least REQUIREMENTS.md, the SESSION and PROGRESS notes, and handover/). In particular, REQUIREMENTS 39 was added on Oct 16 after the docs were written: an opt-in Cronet / HTTP/3 transport. Work it properly into HANDOVER.md's goals and plan and into REQUIREMENTS-STATUS-FINAL.md (status: not started; owners WP06, WP02, WP12 and WP13; after the Android release gate), replacing the appended stub.
Commit on main (in ${R}) and push (no force). Return done or blocked.`, { label: 'docs:one-branch', phase: 'Docs', schema: STATUS })
if (!docs) throw new Error('docs update failed')

phase('Verify')
const v = await agent(`${CONTEXT}

TASK: verify independently, from fresh clones in ${SCRATCH}.
1. android-base:
   - git clone the public repo; it must have only main (git ls-remote --heads).
   - Every archive/* tag must exist and be an ancestor of main. The merged/* tags must be there.
   - The content of every former WIP snapshot must be in main (sample-diff the snapshot tag's changed files against main).
   - overhaul-workspace/ must exist with HANDOVER.md and the docs. The root CLAUDE.md must point to it.
   - Secrets scan of main and the tags. No file over 50 MB.
2. PeriFlow: the same checks against its private repo (only main, every archive tag an ancestor). No periflow-recovery-codes.txt or PeriFlow-Postman/ in any commit of main.
3. Locally: no branches other than main in either repo. No worktree with uncommitted work that is not in main.
4. Cold-read test: as a new Claude on Windows with only the android-base clone (and the knowledge that the offline zips are on the user's Google Drive), read CLAUDE.md, then overhaul-workspace/HANDOVER.md. List the next 10 actions you would take, and every question you would still need to ask the user. Each such question is a GAP, unless it is a decision only the user can make.
Return ok=true only if both repos are consistent, nothing is lost, and the gaps are trivial; list every gap.`, { label: 'verify:one-branch', phase: 'Verify', schema: VERIFY })
return { androidBase: ab, periflow: pf, docs: docs.summary, verify: v }
