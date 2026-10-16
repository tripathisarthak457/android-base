export const meta = {
  name: 'local-offline-backup',
  description: 'Before the laptop wipe: find everything on this machine that is not on GitHub and that the user would need later on Windows, and pack it into zips under tools/backup-2026-10-16 for a pen drive, with a restore guide, checksums and a test of every zip',
  phases: [
    { title: 'Scan', detail: 'whole home folder: what is not on GitHub, what is regenerable' },
    { title: 'Pack', detail: 'zips under 3.9 GB each, restore guide, checksums' },
    { title: 'Verify', detail: 'test every zip, check nothing needed is left out' },
  ],
}

const HOME = '/home/crazyhusir'
const TOOLS = HOME + '/Documents/projects/tools'
const OUT = TOOLS + '/backup-2026-10-16'
const OV = TOOLS + '/.overhaul'

const CONTEXT = `CONTEXT. The user (Sarthak) will WIPE this Linux laptop (Pop!_OS, user crazyhusir, home ${HOME}, 185 GB free on /) and install Windows. In chat on Oct 16, they said: "nothing should not be left unbacked up on this machine thats ur responsibity if anything u cant save on github make a folder in the toold directuory as backup-todays date and zip it and have all the files which i would need later in windows which arent sent to git i will save it offline in pendrive". They also asked for the notes to say that Claude on Windows will get this zip somehow. The destination changed (Oct 16): the user uploads the folder to Google Drive (as backup-2026-10-16) instead of a pen drive. The zips stay under 3.9 GB (resumable uploads), and every zip, 01-secrets included, is a plain unencrypted .zip that opens with Windows' built-in extractor (the user: "no need to protect the zips my account is secure").

At the same time, other workflows handle two things: pushing the android-base overhaul (android-base and its workspace ${OV}) to github.com/tripathisarthak457/android-base, and pushing PeriFlow (${HOME}/Documents/projects/jorwen/periflow) to its private repo. Afterwards every branch of both is merged into main. Even so, this backup must hold what git does not carry: ignored files, secrets, local-only data, and a git bundle of both repos as a safety net.

Rules:
- Read anything you need, but write only under ${OUT}/.
- Never print secret values in your output (paths only).
- No Gradle builds.
- Keep CPU heat down: the laptop powers off when the CPU sits near 100 C. Use nice -n 19, one compression at a time, zip -1 for text, and -0 (store) for files that are already compressed (jpg, png, mp4, zip, apk, gz, zst).
- Never delete or move anything outside ${OUT}. Never kill processes.`

const MAP = { type: 'object', properties: { files: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['files', 'summary'] }
const STATUS = { type: 'object', properties: { state: { type: 'string', enum: ['done', 'blocked'] }, summary: { type: 'string' } }, required: ['state', 'summary'] }
const VERIFY = { type: 'object', properties: { ok: { type: 'boolean' }, gaps: { type: 'array', items: { type: 'string' } }, summary: { type: 'string' } }, required: ['ok', 'gaps', 'summary'] }

phase('Scan')
const scan = await agent(`${CONTEXT}

TASK: decide exactly what goes into the offline backup, and write the plan to ${OUT}/PLAN.md (create ${OUT}). Measure with du, find and git; do not guess.
A. GIT REPOSITORIES anywhere under ${HOME} (find -name .git, including worktrees). Skip caches and tool folders: ~/.cache, ~/.gradle, ~/.m2, ~/.npm, ~/.pub-cache, ~/go/pkg, ~/Android/Sdk, ~/.local/share/Trash, node_modules. For each repo:
   - its remote, and its state: every branch pushed? stashes? uncommitted or untracked files?
   - if anything is not on its remote, or it has no remote at all: plan a git bundle (git bundle create <name>.bundle --all), plus a tar of its uncommitted and untracked files (not the ignored build output);
   - for EVERY repo, android-base and periflow included: plan to copy the IGNORED files that matter and are not regenerable (keystores, *.jks, google-services.json, .env*, local.properties when it holds more than sdk.dir, signing configs, periflow-recovery-codes.txt, PeriFlow-Postman/, other credentials or notes kept out of git);
   - android-base (${TOOLS}/android-base) and periflow: ALSO a full git bundle --all each, as a safety net taken before their branches are merged and deleted.
B. THE OVERHAUL WORKSPACE ${OV} (42 GB): another workflow pushes its curated part to GitHub (branch overhaul-workspace) and its big raw data to a draft release. When ${OV}/handover/MANIFEST.md exists, read it. Include here every never-publish item (the Gmail reference recordings and frames, secrets) and anything else it left out that is NOT regenerable. Regenerable means: build outputs, APKs, generated projects, build/ and .gradle/ folders, Gradle caches, and wt/ (git worktrees; their WIP is pushed as branches). If MANIFEST.md is not there yet, decide the same way yourself.
B0. ANDROID-BASE FIRST (the user, Oct 16: "I want everything, anything u have exclude in github should be present in zip make a seperate zip for android base stuff which isnt on github"). Zip group 00-android-base-not-on-github, built first, in numbered parts under 3.9 GB:
   - the ENTIRE ${OV} (out/, perf/, qa/, scratch*/, verify-*/, logs, and wt/ with its uncommitted, ignored and build files; only node_modules excluded; the never-publish items included);
   - android-base's ignored and untracked files (except node_modules);
   - a git bundle --all of android-base;
   - every Claude transcript and memory folder about android-base under ~/.claude/projects (the tools, tools--overhaul and Downloads-android-base folders, subagent transcripts included).
B2. UNPUSHED PROJECTS (the user, Oct 16: "also in the offline zip include the projects on my system that werent pushed to github"). Every project not fully on GitHub, anywhere under ${HOME}: git repos with unpushed commits or branches, stashes, uncommitted or untracked work, or no remote (a bundle --all, plus a tar of the uncommitted and untracked files, plus the important ignored files); and project folders that are not under git at all (their source, minus node_modules, build/, .gradle/, dist/, .next/, venvs and caches). They form their own zip group, 09-unpushed-projects (split as needed). PLAN.md lists each project: path, reason (unpushed / no remote / not git), size and zip.
C. PERSONAL FILES outside git: ${HOME}/Documents (outside the git repos), Downloads, Desktop, Pictures, Videos, Music, and loose files in ${HOME}. Include everything except regenerable or reinstallable things: node_modules, build/, dist/, .next/, venv and .venv, __pycache__, target/, caches, IDE indexes, Android SDK images and AVDs.
   - Installers and ISOs in Downloads are reinstallable. List them with sizes and put them in their own zips, so the user can skip them.
   - Exclude ${OUT} itself.
D. CONFIG AND KEYS:
   - ~/.ssh and ~/.gnupg; ~/.android (debug.keystore, adbkey*); ~/.gitconfig; ~/.gradle/gradle.properties and ~/.gradle/init.d; ~/.m2/settings.xml; ~/.npmrc;
   - shell rc files (for reference); VS Code and JetBrains user settings, if small;
   - ~/.claude: the memory folders of every project (~/.claude/projects/*/memory), settings.json, the plugin list and marketplaces config (not plugin caches), and the top-level session transcripts (~/.claude/projects/*/*.jsonl; measure them, include subagent transcripts only if the total stays under about 2 GB);
   - ~/.config/Claude (only what restores settings, if small).
   - Skip browser profiles (the browsers sync them) and the GNOME keyring (unusable on Windows), but name them in the plan as "not backed up, why, what to do".
E. ZIP LAYOUT. Windows' own zip support and FAT32 pen drives both need files under 4 GB, so every zip stays under 3.9 GB, with no split archives. Group them as:
   - 01-secrets (keys, keystores, .env, recovery codes; the user should encrypt or store this one carefully);
   - 02-claude-and-config;
   - 03-overhaul-local-only;
   - 04-git-bundles (android-base, periflow, and every repo with unpushed work);
   - 05-repo-ignored-and-uncommitted;
   - 06-documents;
   - 07-downloads (installers marked);
   - 08-media;
   - more zips as needed, numbered.
   PLAN.md lists every source path, its size, its target zip and the reason, plus a table of what is NOT backed up and why. Also write ${OUT}/plan-paths.txt in a machine-readable form: one line per item, "<zip-name>\t<absolute path>\t<exclude globs, comma-separated>".
Return the plan's path, the total size, and the per-zip sizes.`, { label: 'scan:machine', phase: 'Scan', schema: MAP })
if (!scan) throw new Error('scan failed')

phase('Pack')
const pack = await agent(`${CONTEXT}

TASK: build the offline backup in ${OUT} exactly as ${OUT}/PLAN.md and ${OUT}/plan-paths.txt say.
- Make the git bundles first (git bundle create ... --all, then git bundle verify each), plus the tars of uncommitted and untracked files, in ${OUT}/staging/.
- Then build the zips (zip -r -1, store already-compressed types with -n .jpg:.png:.mp4:.zip:.apk:.gz:.zst:.webm:.mkv:.mov:.7z), each under 3.9 GB. If one would be bigger, split it into more numbered zips by subfolder, never into a split archive.
- Keep paths inside each zip relative to the home folder (e.g. Documents/..., .ssh/...), so that restoring is obvious.
- Run under nice -n 19, one zip at a time. Check free disk space before each zip.
- Delete ${OUT}/staging/ once its content is zipped.
- Write ${OUT}/README-RESTORE.md:
  - what this is (the offline part of the Oct 16 laptop-wipe backup; the rest is on GitHub);
  - a table of each zip with its contents and size;
  - how to restore each one on Windows (and into WSL2 for the dev setup): where .ssh goes, how to import the GPG keys, where the Claude memory folders go (~/.claude/projects/<slug>/memory, where the slug derives from the new project path), how to restore a repo from a bundle (git clone x.bundle), and where the android-base and PeriFlow ignored files go;
  - a note that 01-secrets holds keys and credentials (the user keeps the zips unencrypted on their own secure Google Drive);
  - a pointer to the GitHub side: android-base main and its overhaul-workspace docs, which say "read HANDOVER.md".
- Write ${OUT}/MANIFEST.txt (unzip -l of each zip) and ${OUT}/SHA256SUMS.txt.
Return done with the zips and their sizes, or blocked with the reason (e.g. a permission or classifier denial: name the exact path and let the user copy it).`, { label: 'pack:zips', phase: 'Pack', schema: STATUS })
if (!pack) throw new Error('pack failed')

phase('Verify')
const v = await agent(`${CONTEXT}

TASK: verify the offline backup in ${OUT} independently.
1. unzip -t every zip; sha256sum -c SHA256SUMS.txt; every zip under 3.9 GB.
2. Every item in plan-paths.txt is present in a zip (compare with MANIFEST.txt; sample-check that sizes and file counts match the sources).
3. Every git bundle verifies (git bundle verify), and for android-base and periflow, git bundle list-heads shows every local branch.
4. A fresh sweep for anything MISSED: run a quick independent scan of ${HOME} for files modified in the last 90 days that are neither in a git repo that is fully pushed, nor in the zips, nor regenerable (node_modules, build outputs, caches, SDKs). Also check for keystores, .env files and credentials not in 01-secrets. List every miss.
If you find misses, add them yourself: a new numbered zip, then update README-RESTORE.md, MANIFEST.txt and SHA256SUMS.txt. Then re-verify.
Return ok=true only if every zip tests clean and nothing needed is missing; list any gaps.`, { label: 'verify:offline', phase: 'Verify', schema: VERIFY })
return { scan: scan.summary, pack: pack.summary, verify: v }
