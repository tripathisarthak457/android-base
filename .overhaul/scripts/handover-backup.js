export const meta = {
  name: 'handover-backup',
  description: 'Before the user wipes this laptop for Windows: document the whole android-base overhaul (project, goals, plan, every requirement, every package, the chat history, memory, how to resume on Windows) so a new Claude needs no re-briefing, then push everything to the existing GitHub repos of the user (every android-base branch, WIP snapshots and the workspace as branch overhaul-workspace on the public android-base; the PeriFlow branch and WIP to its own private repo; oversized raw data as draft-release assets) and verify from fresh clones',
  phases: [
    { title: 'Inventory', detail: 'what to back up, what to leave out, secrets and personal content, other repos at risk' },
    { title: 'Document', detail: 'handover docs, requirements, packages, chat history, Windows setup' },
    { title: 'Pack', detail: 'WIP snapshots, secrets scan, push every branch, the workspace and PeriFlow' },
    { title: 'Verify', detail: 'fresh clone, ref and content checks, cold-read test, fixes' },
    { title: 'Archive', detail: 'files too big for git, as draft-release assets' },
  ],
}

const TOOLS = '/home/crazyhusir/Documents/projects/tools'
const R = TOOLS + '/android-base'
const OV = TOOLS + '/.overhaul'
const H = OV + '/handover'
const MEM = '/home/crazyhusir/.claude/projects/-home-crazyhusir-Documents-projects-tools/memory'
const CP = '/home/crazyhusir/.claude/projects'
const PF = '/home/crazyhusir/Documents/projects/jorwen/periflow'
const AB = 'tripathisarthak457/android-base'
const PFR = 'tripathisarthak457/periflow'
const WSB = 'overhaul-workspace'
const RAW = 'overhaul-raw-data-2026-10-16'
const SCRATCH = '/tmp/claude-1000/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/scratchpad/handover'

const CONTEXT = `CONTEXT. The user (Sarthak, GitHub tripathisarthak457) is about to WIPE this Linux laptop (Pop!_OS, Mi NoteBook Ultra, i5-11300H, 15 GB RAM) and install Windows. Before that, everything about the "android-base" overhaul must be documented and backed up to GitHub, so that Claude on Windows has the plan and the full context without the user explaining anything again.

The project: "android-base" (github.com/tripathisarthak457/android-base, PUBLIC; site android-base.vercel.app, which Vercel deploys from main) is the user's app generator: a Python generator, a Kotlin/Compose Android template, a Compose Multiplatform template, a hidden SwiftUI template, a Next.js website and admin portal, and a Go API. Since Oct 8, 2026 a multi-day overhaul has been running.
- Local clone: ${R}. Integration branch: overhaul/multiplatform-v2. Package branches: wp/WP01 to wp/WP10, plus wp/WP03-skel, wp/WP04-perf and wp/WP05-glass. Worktrees live in ${OV}/wt/<WP>.
- Workspace: ${OV} (NOT in git). It holds STATUS.md, SESSION-2026-10-13..16.md, PROGRESS.md, REQUIREMENTS.md (items 1-38), REQUIREMENTS-STATUS.md, FABLE-FINDINGS.md, design/ (the MASTER-PLAN and per-package designs), handoff/ (every package's handoff and RESUME briefs), decisions/, research/, qa/, audit/, perf/, scripts/ (the Workflow scripts that orchestrate the agents, e.g. to-release.js), bin/ (locked, memwait, thermlog), merges.md, followups.md and docs-draft/.
- Claude's memory for this project: ${MEM} (MEMORY.md plus one file per memory).
- Chat transcripts (JSONL, one per session): ${CP}/-home-crazyhusir-Documents-projects-tools/*.jsonl (the current long session is e3f9060d-6364-4a81-a9a2-20851e25521f.jsonl; it was compacted several times, so its summaries also matter). Related sessions: ${CP}/-home-crazyhusir-Documents-projects-tools--overhaul/ and ${CP}/-home-crazyhusir-Downloads-android-base/. Workflow subagent transcripts are under <session>/subagents/workflows/<run>/.
- PeriFlow (${PF}, remote github.com/${PFR}, PRIVATE) is a separate app of the user's. Its local branch overhaul/smooth-glass (worktree ${OV}/wt/WP11) carries the overhaul's PeriFlow work. Never touch periflow-recovery-codes.txt or PeriFlow-Postman/ (secrets).

THE USER'S DECISION FOR THE WIPE (Oct 16, in chat): push everything to the existing GitHub repos. "push to public there is no problem, just push each and every file periflow too." So:
- android-base (PUBLIC) gets every branch except main (main is untouched: Vercel deploys it), the merged/* tags, the WIP snapshots as backup/<name>-wip-2026-10-16 branches, and the whole workspace as the orphan branch ${WSB} (the curated .overhaul, the handover docs, Claude's memory).
- PeriFlow's branch overhaul/smooth-glass and a WIP snapshot go to its own PRIVATE repo, ${PFR}. PeriFlow source never goes into the public repo.
- Files git cannot hold (over 95 MB, or bulky raw data) go as archives onto a DRAFT release (${RAW}) of android-base; drafts are visible only to the owner. Regenerable build outputs (APKs, build/ folders, generated projects; about 17 GB) are left out.
- Even with the user's go-ahead, NEVER publish: secrets (keystores, signing keys, tokens, API keys, passwords, google-services.json, local.properties, .env, periflow-recovery-codes.txt, PeriFlow-Postman/); PeriFlow source code; or other people's personal data (the Gmail reference recordings and frames show real emails).

The state on Oct 16, around 11:00:
- Merged into the integration branch: WP01, WP02-M1, WP09, WP10 (a402d87).
- Every other package is on its branch. Uncommitted work-in-progress: WP03 (47 files: the whole bottom-sheet system, not compiling at SheetFlingBehavior.kt:88), WP05-glass (43 files: the liquid-glass bottom bar), WP04-perf (5 files) and WP11 (2 files, PeriFlow).
- The release model is Android first (REQUIREMENTS 36). A read-only release gate (RELEASE-GATE-android.md) comes first; then the USER's explicit go-ahead in chat; then the merge to main, the push and the Vercel deploy. Compose Multiplatform and Swift come later.
- The ETA for the Android release gate was about Oct 17, before the crashes.
- The orchestration runs five Workflow scripts: to-release.js, shimmer-fix-from-report.js, deep-smoothness-fable.js, full-liquid-glass-fable.js and android-skills-v1-0-15.js. Since Oct 15 20:55, every agent runs on Opus 5.5 only (Fable used too much usage). They all died in the crashes and are NOT running now.

The user's standing instructions:
- Keep going until told to stop.
- Never compromise quality: a Google/AOSP-level review before every merge, and phone gates on release builds (the Redmi Note 10 Pro, serial 9a7c74f4, 120 Hz).
- Don't ask questions; decide and record.
- Android first.
- No separate billing: extra usage stays off.
- Never merge to main without the Android release gate and the user's go-ahead (Vercel deploys main). Pushing other branches is allowed for this backup (the user, Oct 16).
- Never commit secrets, keystores, local.properties or a real google-services.json.
- Never kill processes you did not start.
- The Gmail recordings describe motion only, never content.
- Commit trailer: "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>".

Lessons from the last days (the docs must carry them):
- Usage spikes come from switching models mid-chat and from launching many workflows at once.
- Changing an agent's options invalidates the workflow resume cache.
- Laptop: a memory freeze on Oct 10. On Oct 16, three sudden power-offs. The cause is most likely CPU heat under Gradle builds: the CPU sat at 95-100 C and acpitz at 97 C (hot trip 99 C), and the PL1 limit is 200 W on MSR. Windows' Intel DTT driver manages this; Linux doesn't. Thermal sensor SEN3 also reads erratically, between 60 and 125 C.
- bin/locked now uses flock -o for adb (an adb server started inside flock held the lock forever) and two Gradle workers at nice 10. bin/memwait waits for the CPU to cool below 80 C.
- The auto-mode classifier blocks killing processes and autonomous deploys.

Rules for every agent in this workflow:
- No Gradle builds, no emulator and no phone work: the laptop powers off under build heat.
- Never kill processes. Never push or change main, never force-push, never change the default branch.
- PeriFlow's code goes only to its own private repo.
- Write only where your task says.
- Do not invent facts. When something is unknown, say so and point to where it can be found.`

const DOCSTYLE = `Writing style: plain, precise English for a future Claude and for the user. Use headings, tables and checklists. Give exact paths, branch names, commit SHAs and commands. Explain why, not only what. Mark anything uncertain as such. Where the workspace moves on Windows, give paths relative to the workspace root (.overhaul/ and android-base/ side by side under one "tools" folder), with the old Linux absolute path alongside.`

const MAP = { type: 'object', properties: { files: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['files', 'summary'] }
const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'blocked'] }, summary: { type: 'string' } }, required: ['state', 'summary'] }
const VERIFY = { type: 'object', properties: { ok: { type: 'boolean' }, gaps: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['ok', 'gaps', 'summary'] }

phase('Inventory')
const [inv, wipe] = await parallel([
  () => agent(`${CONTEXT}

TASK (read-only, except that you write two files under ${H}/). Decide how every file of the workspace ${OV} (42 GB) is backed up. The user wants each and every file kept; the only exceptions are regenerable build outputs and what must never be published.
- GIT (branch ${WSB} of the PUBLIC android-base): every file under 50 MB that is not excluded below: notes, plans, handoffs, decisions, research, reviews, QA and perf reports, images, logs, every script, small traces. The target is under 2 GB in total (one push).
- DRAFT-RELEASE ARCHIVES (owner-only): kept files that are too big or too bulky for git, such as raw Perfetto traces, screen recordings of the generated app and catalog, and anything over 50 MB. Group them into tar.zst (or tar.gz) archives under 1.9 GB each. List each archive's source paths in ${H}/release-assets.txt (one line per archive: archive name, then the paths).
- LEAVE OUT (regenerable): wt/ (the worktrees are git; their WIP is snapshotted into branches separately); build outputs (APKs, AABs, .class and .dex files, build/ and .gradle/ folders, the Gradle caches, generated projects under out/, scratch/, scratch-gen/, verify-gen/, verify-webapi/; keep any notes, scripts and logs inside them); *.lock files.
- NEVER PUBLISH, not even in the draft release (flag them): personal content of other people, i.e. the Gmail reference recordings, their frames and any screenshot showing real emails, notifications or personal data (the user allowed motion analysis only); PeriFlow source code or copies of PeriFlow files (notes ABOUT PeriFlow are fine); and periflow-recovery-codes.txt and PeriFlow-Postman/.
- SECRETS SCAN over what you keep: keystores (*.jks, *.keystore), google-services.json, local.properties, .env files, API keys and tokens (ghp_, github_pat_, sk-, AIza, AKIA, private keys, "password ="). List every hit with its path. Hits are left out; if a hit sits inside a document, list the document and the line, so the packer can redact it.
Use du, find and file. Do not open videos or images beyond what you need to judge whether they are personal.
Write ${H}/MANIFEST.md (human-readable: what is kept and why, what is left out and why, the size estimate, the flagged files) and ${H}/backup-include.txt (one rsync filter rule per line, in rsync --filter syntax relative to ${OV}, ending with "- *" so that only what is listed is kept; test it with rsync -a --dry-run --stats --filter='merge ${H}/backup-include.txt' ${OV}/ /tmp/x and report the total size; under 2 GB, with no file over 50 MB) and ${H}/release-assets.txt.
Return the paths and a summary with the git size, the archive size, the counts, the flagged items and the secret hits.`, { label: 'inv:workspace', phase: 'Inventory', schema: MAP }),
  () => agent(`${CONTEXT}

TASK (read-only, except ${H}/WIPE-READINESS.md). The user will wipe this whole laptop. Find everything that would be LOST, beyond this project's backup (which other agents handle):
1. Every git repository under /home/crazyhusir/Documents and /home/crazyhusir/Downloads (find -name .git -maxdepth 6, worktrees included). For each one: the remote URL; branches with no upstream, or ahead of their upstream (git log @{u}.. counts); stashes; uncommitted and untracked files (counts, plus the notable ones); and the last commit date.
2. Important non-git data: keystores and signing keys anywhere under the home folder (find -name '*.jks' -o -name '*.keystore' ; just list the paths, never print their contents), ~/.ssh, ~/.gnupg, ~/.android (debug keystore, adb keys), ~/.claude (memory, settings, plugins: list what matters), ~/.config/Claude, the gh CLI login, local.properties files with SDK paths or secrets (names only), and the Gradle and Android SDK setup (versions only).
3. For this project specifically (other agents push these): the android-base branches (local only versus pushed), the merged/* tags, the PeriFlow branch overhaul/smooth-glass and its 2 dirty files, and anything in ${OV} that is not covered by ${H}/MANIFEST.md (another agent writes it in parallel; just note it if it exists).
Write ${H}/WIPE-READINESS.md: one table per repository plus a checklist of what the user must copy or push themselves before wiping (e.g. other projects with unpushed work, keystores to save to a password manager or drive, PeriFlow). Never print secret values. Return the path and a short summary of the biggest risks.`, { label: 'inv:wipe-readiness', phase: 'Inventory', schema: MAP }),
])
if (!inv || !wipe) throw new Error('inventory failed')

phase('Document')
const DOCS = [
  { key: 'handover', prompt: `Write the master entry point ${H}/HANDOVER.md, plus two small files: ${H}/CLAUDE.md (it goes at the root of the backup's workspace branch, so Claude loads it automatically: what this folder is, "read HANDOVER.md first", the standing instructions and the safety rules in brief) and ${H}/tools-CLAUDE.md (for the parent "tools" folder on Windows, which holds android-base/ and .overhaul/ side by side: the same pointer, in brief).
HANDOVER.md must let a brand-new Claude continue the overhaul with NO re-briefing from the user. Cover:
1. Who the user is, and how they want to work: their standing instructions, verbatim where they gave them; their preferences; what annoys them (e.g. long silences without a status, usage waste).
2. What android-base is: its product, its architecture (generator, templates, features, the website and admin portal, the API) and its repository layout.
3. The overhaul's goals: every original goal and every requirement 1-38, one line each, with the details in REQUIREMENTS-STATUS-FINAL.md. The definition of done for the Android release, and for phase B (Compose Multiplatform and Swift).
4. The plan: the master plan's structure (read ${OV}/design/MASTER-PLAN.md), the packages WP01-WP14, MB, QA2 and CP1/CP2, their dependencies and merge order, the release gate, and what remains, in order, with an honest ETA in usage windows.
5. How the work is orchestrated: the Workflow scripts (what each one does and how they resume), the review-then-fix loops, serialized merges, bin/locked, memwait and thermlog, the handoff and RESUME-brief convention, and STATUS.md, SESSION notes and PROGRESS.md. Model policy: Opus 5.5 only.
6. The safety and quality rules: the full list.
7. Lessons and pitfalls: usage, the resume cache, the classifier, adb, memory, heat and power-offs, lid sleep.
8. "First 30 minutes on Windows": the exact steps to restore and resume (link RESUME-ON-WINDOWS.md), and what to tell the user.
9. An index of every other handover document and of the key workspace files.
Read everything relevant: ${OV}/STATUS.md, the SESSION-*.md files (newest first), PROGRESS.md, REQUIREMENTS.md, REQUIREMENTS-STATUS.md, FABLE-FINDINGS.md, design/, decisions/, merges.md, followups.md, handoff/ (skim every file; read the RESUME briefs), scripts/*.js (headers and package tables) and ${MEM}/*. HANDOVER.md should be long and thorough (2000+ lines is fine) but well organised.` },
  { key: 'requirements', prompt: `Write ${H}/REQUIREMENTS-STATUS-FINAL.md: for EVERY requirement in ${OV}/REQUIREMENTS.md (1-38 and every sub-item and addendum), plus the original overhaul goals in ${OV}/design/MASTER-PLAN.md, give:
- the exact requirement (quote the user's words where the file has them);
- its status: done-and-merged, done-on-branch, in progress, not started or blocked;
- the evidence: the branch and commit, the handoff section, the perf or QA report path, and the review verdicts;
- what remains, concretely, and which package owns it;
- the acceptance check that proves it, for the release gate.
Check the evidence against git (git -C ${R} log, branch heads, merged/* tags) and the handoffs; ${OV}/REQUIREMENTS-STATUS.md is an older, incomplete draft. End with a one-page summary table and the list of items the user must decide (e.g. the WP11 PeriFlow lens deviation: 3.62% against a 2.5% gate, with a baseline of 3.41%).` },
  { key: 'packages', prompt: `Write ${H}/PACKAGES-AND-BRANCHES.md. For every branch in ${R} (git branch -a, the merged/* tags) and every worktree in ${OV}/wt/:
- the purpose and owner package; the head SHA and subject; commits ahead of overhaul/multiplatform-v2;
- the uncommitted files (they will be preserved as backup/<name>-wip-2026-10-16 branches, so say what that WIP is, from the handoffs and the file list);
- the last review verdict, what is done, what remains (from handoff/<WP>.md and <WP>-RESUME.md), and its dependencies.
Then:
- the dependency graph and merge order, from ${OV}/scripts/to-release.js (PKGS, ORDER, the pre-fired list);
- the release-gate process;
- the side branches (WP03-skel, WP04-perf, WP05-glass) and who merges them;
- the PeriFlow branch, pushed with its WIP snapshot to the private ${PFR} repo;
- a catalogue of ${OV}/scripts/*.js: each workflow's purpose, phases, run ids, how to relaunch, and the hard-coded Linux paths that must change on Windows (list each path constant per script).
Include exact git commands for restoring the WIP branches into the worktrees on the new machine (git worktree add, then cherry-pick or checkout the WIP snapshot as uncommitted changes).` },
  { key: 'history', prompt: `Write ${H}/CHAT-HISTORY.md: the full story of the overhaul conversation, so that Claude on Windows knows everything the user has said and decided.
1. Use a small Python script (save it as ${H}/tools/extract_user_messages.py) to extract, from every transcript under ${CP}/-home-crazyhusir-Documents-projects-tools/*.jsonl, ${CP}/-home-crazyhusir-Documents-projects-tools--overhaul/*.jsonl and ${CP}/-home-crazyhusir-Downloads-android-base/*.jsonl (top-level session files only, not subagents), every message the USER typed: type "user", content that is a plain string or text blocks, NOT tool results, NOT system reminders, NOT command caveats or task notifications, NOT "<agent-message>" blocks. Keep the timestamps.
2. Write ${H}/USER-MESSAGES.md: every user message, verbatim, in chronological order, with its timestamp and session id. Redact secrets if any appear (none should). Keep only the sessions about android-base, its overhaul, PeriFlow-in-the-overhaul and the laptop; skip unrelated projects (note which sessions you skipped and why).
3. Read the compaction summaries in the transcripts too: messages whose content starts with "This session is being continued from a previous conversation". They are dense records of earlier context.
4. In CHAT-HISTORY.md, write a chronological narrative day by day (Oct 8 to Oct 16): what the user asked, what was decided, what was built and merged, what went wrong and how it was fixed (classifier denials, usage spikes, crashes, memory, heat, adb locks, network outages, lid sleep), every requirement as it was added, every model and usage decision, and every standing instruction with the date it was given.
Then add a section "Things the user has said they want, in their own words": a deduplicated list. The goal: nothing the user ever said about this project is lost.` },
  { key: 'windows', prompt: `Write ${H}/RESUME-ON-WINDOWS.md: a tested-as-far-as-possible guide for restoring and continuing the overhaul on Windows.
1. Recommend a setup and justify it: native Windows Claude Code with the workspace inside WSL2 Ubuntu, because the orchestration uses bash, flock, coreutils and Linux paths, versus native Windows. Give the exact steps for the recommendation.
2. Restore:
   - git clone github.com/${AB} into tools/android-base (all branches come with it). Put the ${WSB} branch's contents at tools/ (git clone -b ${WSB} --single-branch ... tools-ws, then move .overhaul/, CLAUDE.md and the rest into place; explain the cleanest way).
   - Download the raw-data archives from the draft release with gh, and unpack them into .overhaul/.
   - Clone the private periflow repo to where the handoffs expect it (it was ~/Documents/projects/jorwen/periflow) and check out overhaul/smooth-glass.
   - Recreate the worktrees under .overhaul/wt/ from the branches, and re-apply the backup/*-wip-2026-10-16 snapshots as uncommitted changes.
   - Restore Claude's memory into the new ~/.claude/projects/<slug>/memory folder. Explain how the slug is derived from the path, and that the memory files sit under claude-memory/ in the ${WSB} branch.
   - Reinstall Google's android-skills Claude plugin: read ~/.claude/plugins and ~/.claude/settings.json on this machine for the marketplace source and version (1.0.15). Never copy tokens.
3. The toolchain, with exact versions read from the repo:
   - JDK, Android SDK, platform and build tools, the NDK if used, Gradle and AGP from templates/android (the Gradle wrapper, libs.versions.toml);
   - Python and its packages for the generator (generator/, requirements files);
   - Node and the package manager for web/;
   - Go for the API; perfetto trace_processor; adb.
4. The phone: USB debugging on the Redmi (9a7c74f4); usbipd-win to attach it to WSL2; adb.
5. Paths: every hard-coded /home/crazyhusir/... path in ${OV}/scripts/*.js, ${OV}/bin/*, handoffs and helper scripts (grep -rl). Give the replacement and a safe sed recipe.
6. Heat and memory on the new OS: keep bin/locked and memwait; Windows' Intel DTT should prevent the power-offs; WSL2 memory limits via .wslconfig (e.g. 10 GB) and why.
7. Resume: which workflows to relaunch, in which order, two at a time, fresh (run ids do not carry over), and what to check first.
Read the repo's build files for the exact versions; do not guess.` },
]
const docs = await parallel(DOCS.map(d => () => agent(`${CONTEXT}

${DOCSTYLE}

The inventory is done: ${H}/MANIFEST.md (what goes into the backup) and ${H}/WIPE-READINESS.md. The backup layout:
- github.com/${AB} (PUBLIC; main untouched) gets every branch and the merged/* tags, the backup/<name>-wip-2026-10-16 branches (uncommitted work), and the orphan branch ${WSB}: CLAUDE.md, HANDOVER.md and README.md at its root; .overhaul/ (the workspace, with handover/ inside); claude-memory/.
- Raw data too big for git: archives on the DRAFT release ${RAW} of ${AB} (gh release download ${RAW} -R ${AB}).
- PeriFlow: branch overhaul/smooth-glass and backup/smooth-glass-wip-2026-10-16 on the private github.com/${PFR}.

TASK: ${d.prompt}
Write only under ${H}/. Return the files you wrote and a summary.`, { label: `doc:${d.key}`, phase: 'Document', schema: MAP })))
if (docs.some(x => !x)) throw new Error('a documentation agent failed')

phase('Pack')
const pack = await agent(`${CONTEXT}

TASK: pack and push the backup. You are the only agent writing to git now. Be careful and verify each step.
1. WIP snapshots, without touching any worktree's index, working files or branch. For each worktree in ${OV}/wt/* that belongs to android-base and has uncommitted or untracked files (git status --porcelain; expected: WP03, WP04-perf, WP05-glass; check all):
   - use a temporary index: export GIT_INDEX_FILE=$(mktemp), git read-tree HEAD, git add -A (in the worktree; .gitignore applies), git write-tree, then git commit-tree with parent HEAD and the message "WIP snapshot of <branch> before the laptop wipe (2026-10-16): uncommitted work, not reviewed, may not compile" plus the trailer, then git branch backup/<name>-wip-2026-10-16 <sha>;
   - before branching, list the snapshot's files against HEAD and check them against the secrets patterns in ${H}/MANIFEST.md (no keystores, google-services.json, local.properties or .env). If any is present, rebuild the snapshot without it.
   Never run git add -A on the real index, and never on the PeriFlow repo.
2. A SECRETS SCAN before anything goes public. These branches have never been pushed, so their whole history becomes public. For every android-base branch to be pushed (all local branches except main, plus the new backup/* branches): scan git log -p origin/main..<branch> and the branch's tree (git grep over the ref) for keystores, signing keys, google-services.json with real keys, local.properties, .env, tokens (ghp_, github_pat_, sk-, AIza, AKIA, xox, -----BEGIN ... PRIVATE KEY-----), and "password"/"secret" assignments with literal values. Test fixtures that are obviously fake are fine; say why each is fake. If a real secret exists in a branch's history, do NOT push that branch: return blocked, naming the branch, commit and file (rewriting history is the user's call).
3. Push android-base to origin (PUBLIC; the user approved it): git -C ${R} push origin <each local branch except main> and the merged/* tags, using explicit refspecs ('refs/heads/wp/*:refs/heads/wp/*', 'refs/heads/overhaul/*:refs/heads/overhaul/*', 'refs/heads/backup/*:refs/heads/backup/*', 'refs/tags/merged/*:refs/tags/merged/*'), with no -u and no --force. If local main differs from origin/main, do NOT push main; record it. Pushing branches may start Vercel preview builds; that is expected and harmless (production deploys only from main).
4. PeriFlow to its own private repo: confirm gh repo view ${PFR} --json visibility says PRIVATE (otherwise stop and return blocked). In ${OV}/wt/WP11, make a WIP snapshot the same way as in step 1, but EXCLUDE periflow-recovery-codes.txt and PeriFlow-Postman/ (use git add with explicit paths from git status, never -A), and name it backup/smooth-glass-wip-2026-10-16. Then git -C ${PF} push origin 'refs/heads/overhaul/smooth-glass:refs/heads/overhaul/smooth-glass' 'refs/heads/backup/smooth-glass-wip-2026-10-16:refs/heads/backup/smooth-glass-wip-2026-10-16' (no -u, no --force). Never touch the periflow repo's main.
5. The workspace branch: build it in ${SCRATCH}/workspace (a fresh git init, branch "${WSB}"):
   - rsync -a --filter='merge ${H}/backup-include.txt' ${OV}/ ${SCRATCH}/workspace/.overhaul/;
   - also copy ${H}/ (everything EXCEPT local-only/) to ${SCRATCH}/workspace/.overhaul/handover/;
   - put ${H}/CLAUDE.md at the workspace root as CLAUDE.md, and ${H}/HANDOVER.md as HANDOVER.md at the root as well;
   - put ${H}/tools-CLAUDE.md at the root;
   - copy ${MEM}/ to claude-memory/;
   - add a README.md that says what this repo is and how to restore (point to HANDOVER.md and RESUME-ON-WINDOWS.md).
   Leave out every never-publish item flagged in ${H}/MANIFEST.md. Run the secrets scan over the whole tree (the memory files and handover docs too: redact any secret value, the user's email may stay), and check that no file is over 50 MB. Commit (author from git config; message "Back up the android-base overhaul workspace before the laptop wipe (2026-10-16)" with the trailer). Then git push https://github.com/${AB}.git ${WSB}:refs/heads/${WSB} (no -u, no --force). Do not change the default branch (it stays main).
6. Write ${H}/PUSHED.md (it stays local and is also committed in a second workspace commit): every ref pushed (repo, ref, SHA); the WIP branches with their file counts; the workspace size; what is still to go to the draft release (${H}/release-assets.txt); and what was deliberately not pushed (never-publish items, build outputs), with the reasons.
Return state done with a summary, or blocked with the exact reason.`, { label: 'pack:push', phase: 'Pack', schema: STATUS })
if (!pack) throw new Error('pack failed')
if (pack.state !== 'done') return { inventory: inv.summary, wipe: wipe.summary, pack }

phase('Verify')
let result = null
for (let round = 1; round <= 2; round++) {
  const v = await agent(`${CONTEXT}

TASK: verify the backup independently. Do not trust ${H}/PUSHED.md; check everything against GitHub.
1. Clone https://github.com/${AB}.git fresh into ${SCRATCH}/verify-${round} (all branches, plus the ${WSB} branch).
   - Every local branch (except main) and every merged/* tag in ${R} must exist remotely with the SAME SHA (compare git for-each-ref with git ls-remote).
   - main on GitHub must be unchanged (the same SHA as before this run: origin/main in ${R}).
   - PeriFlow: git ls-remote https://github.com/${PFR}.git must show overhaul/smooth-glass and backup/smooth-glass-wip-2026-10-16 at the local SHAs.
   - Every backup/*-wip-2026-10-16 branch's tree must match its worktree's current files: check out the snapshot in a temporary worktree and diff -r it against ${OV}/wt/<name>, excluding ignored build output.
2. The ${WSB} branch must contain: CLAUDE.md, HANDOVER.md, README.md, the .overhaul notes (STATUS.md, the SESSION files, REQUIREMENTS.md, design/, handoff/, scripts/, bin/), claude-memory/ and every handover document.
3. Secrets scan of the whole public clone (git grep across every ref for the patterns in ${H}/MANIFEST.md). No file over 50 MB. No PeriFlow source files, and none of the never-publish items, anywhere in the public repo.
4. Cold-read test: pretend you are a new Claude on Windows with no other context. Read CLAUDE.md, then HANDOVER.md and the documents it links, in the clone only. Then:
   - write down the next 10 actions you would take;
   - write down every question you would still have to ask the user about the project, its goals, its state or its rules.
   Each such question is a GAP in the docs, unless it is a genuine decision only the user can make (list those separately).
   Check a sample of factual claims in HANDOVER.md against the repo (branch heads, requirement statuses).
Return ok=true only if the refs, the WIP, the contents and the secrets are all right and the cold-read gaps are trivial. List every gap concretely.`, { label: `verify#${round}`, phase: 'Verify', schema: VERIFY })
  if (!v) throw new Error('verify failed')
  result = v
  if (v.ok) break
  if (round === 2) break
  const fix = await agent(`${CONTEXT}

${DOCSTYLE}

TASK: fix every gap a fresh-context verifier found in the backup. Update the documents under ${H}/ (and only push-safe content). Then rebuild and push the workspace branch the same way as before (rsync with ${H}/backup-include.txt into ${SCRATCH}/workspace; commit a new commit on top, never force-push). If refs or WIP are missing, push them the same way (no force, never main; PeriFlow only to its private repo). If a secret reached the public repo, do NOT try to hide it with a force-push; report it (it must be rotated, which is the user's call). Update ${H}/PUSHED.md.
Gaps:
${JSON.stringify(v.gaps, null, 1)}
Return state done with what you changed.`, { label: `fix#${round}`, phase: 'Verify', schema: STATUS })
  if (!fix) throw new Error('fix failed')
}
phase('Archive')
const arch = await agent(`${CONTEXT}

TASK: upload the files that are too big for git, so that every file the user wants kept is on GitHub.
- Read ${H}/release-assets.txt and ${H}/MANIFEST.md.
- Create a DRAFT release on ${AB}: gh release create ${RAW} --draft --title "Overhaul raw data (2026-10-16 backup)" --notes "Raw perf traces and recordings from the android-base overhaul; restore with gh release download ${RAW}. Draft: visible to the owner only." -R ${AB}. If it already exists, reuse it.
- Build each archive in ${SCRATCH}/raw/, under 1.9 GB each, excluding every never-publish item. Upload with gh release upload ${RAW} <file> -R ${AB} --clobber, one at a time, checking each; retry a failed upload twice.
- Delete each local archive after it uploads, to save disk.
- Append to ${H}/PUSHED.md: the archives, their sizes and sha256, and the restore command. Then rebuild and push the ${WSB} branch with that change (a new commit; never force-push).
- Confirm with gh release view ${RAW} -R ${AB} --json isDraft,assets that the release is still a draft and that every asset is present.
Return state done, or blocked with what is left.`, { label: 'archive:release', phase: 'Archive', schema: STATUS })
return { inventory: inv.summary, wipe: wipe.summary, docs: docs.map(d => d.summary), pack: pack.summary, verify: result, archive: arch && arch.summary }
