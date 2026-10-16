#!/usr/bin/env bash
# sync-workspace-branch.sh: rebuild the orphan branch `overhaul-workspace` of the PUBLIC android-base repo from the
# live workspace, commit, and push it (a normal fast-forward push; never --force). This is the regular off-machine
# backup of the workspace (HANDOVER.md section 6.1 #6). The orchestrator runs it; agents inside workflows never do.
#
# What goes into the branch:
#   .overhaul/          rsync with the filter .overhaul/handover/backup-include.txt (files over 50 MB are skipped)
#   .overhaul/handover/ the whole folder except local-only/ (never published) and claude-memory/ (it goes to the root)
#   README.md           from .overhaul/handover/BRANCH-README.md
#   CLAUDE.md, HANDOVER.md, tools-CLAUDE.md   from .overhaul/handover/
#   claude-memory/      from $MEM_SRC (default: this project's live Claude memory folder)
# Then: local dev-only values are redacted, the staged change is checked for secrets, and it is committed and pushed.
#
# Environment (all optional):
#   TOOLS    the tools root that holds android-base/ and .overhaul/ (default: derived from this script's path)
#   WS_DIR   a clone of the overhaul-workspace branch (default: $(dirname $TOOLS)/tools-ws, as restore-on-wsl.sh makes)
#   MEM_SRC  the memory folder to publish (default: ~/.claude/projects/<slug of $TOOLS>/memory)
#   AB_URL   the android-base repository (default: https://github.com/tripathisarthak457/android-base.git)
#   PUSH     1 to push after committing (default), 0 to commit only
#   COMMIT   1 to stage, check and commit (default), 0 to stop after copying and redacting
#   MSG      the commit subject (default: "Back up the overhaul workspace (<date>)")
# Safe to re-run: with nothing changed it commits nothing.
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
TOOLS=${TOOLS:-$(cd "$here/../../.." && pwd)}
OV=$TOOLS/.overhaul
H=$OV/handover
WS_DIR=${WS_DIR:-$(dirname "$TOOLS")/tools-ws}
slug=$(printf '%s' "$TOOLS" | sed 's/[^A-Za-z0-9]/-/g')
MEM_SRC=${MEM_SRC:-$HOME/.claude/projects/$slug/memory}
AB_URL=${AB_URL:-https://github.com/tripathisarthak457/android-base.git}
PUSH=${PUSH:-1}
COMMIT=${COMMIT:-1}
MSG=${MSG:-"Back up the overhaul workspace ($(date +%F))"}
WSB=overhaul-workspace
R='<local-dev-value>'

die() { echo "sync-workspace-branch: $*" >&2; exit 1; }
[ -f "$H/backup-include.txt" ] || die "no $H/backup-include.txt (is TOOLS=$TOOLS right?)"
[ -d "$WS_DIR/.git" ] || die "$WS_DIR is not a git clone of $WSB (restore-on-wsl.sh creates it; or: git clone -b $WSB --single-branch $AB_URL $WS_DIR)"
[ "$(git -C "$WS_DIR" symbolic-ref --short -q HEAD || true)" = "$WSB" ] || die "$WS_DIR is not on branch $WSB"
[ -d "$MEM_SRC" ] || die "memory folder $MEM_SRC not found (set MEM_SRC)"

# 1. Never overwrite someone else's newer push: GitHub's branch head must be an ancestor of ours.
if git -C "$WS_DIR" fetch -q "$AB_URL" "refs/heads/$WSB" 2>/dev/null; then
  if ! git -C "$WS_DIR" merge-base --is-ancestor FETCH_HEAD HEAD; then
    die "GitHub's $WSB ($(git -C "$WS_DIR" rev-parse --short FETCH_HEAD)) is not in this clone's history. Pull or merge it first (git -C $WS_DIR pull --ff-only $AB_URL $WSB); never force-push."
  fi
fi

# 2. Copy.
nice -n 19 rsync -a --max-size=50M --filter="merge $H/backup-include.txt" "$OV/" "$WS_DIR/.overhaul/"
rsync -a --exclude='/local-only/' --exclude='/claude-memory/' "$H/" "$WS_DIR/.overhaul/handover/"
cp "$H/CLAUDE.md" "$WS_DIR/CLAUDE.md"
cp "$H/HANDOVER.md" "$WS_DIR/HANDOVER.md"
cp "$H/tools-CLAUDE.md" "$WS_DIR/tools-CLAUDE.md"
[ -f "$H/BRANCH-README.md" ] && cp "$H/BRANCH-README.md" "$WS_DIR/README.md"
mkdir -p "$WS_DIR/claude-memory"
find "$MEM_SRC" -maxdepth 1 -type f -name '*.md' -exec cp {} "$WS_DIR/claude-memory/" \;

# 3. Redact local dev-only values (a throwaway local Postgres and a local API; PUSHED.md section 1.5).
O=$WS_DIR/.overhaul
for f in "$O/handoff/WP09.md" "$O/out/WP10/run-api.sh" "$O/handover/MANIFEST.md"; do
  [ -f "$f" ] || continue
  sed -i -E "s#POSTGRES_PASSWORD=[^<[:space:]\"'\`,;)]+#POSTGRES_PASSWORD=$R#g; s#(postgres(ql)?://[^:/@[:space:]\"']+):[^@<[:space:]\"']+@#\1:$R@#g; s#ADMIN_TOKEN='[^'<]*'#ADMIN_TOKEN='$R'#g; s#IP_SALT='[^'<]*'#IP_SALT='$R'#g" "$f"
done
leftover=$(grep -rInE "postgres(ql)?://[^:/@[:space:]\"']+:[^@<[:space:]\"']+@|POSTGRES_PASSWORD=[^<[:space:]\"']" \
  --exclude-dir=.git --exclude=sync-workspace-branch.sh "$WS_DIR" | grep -v 'user:pass@' || true)
[ -z "$leftover" ] || { printf '%s\n' "$leftover" | cut -c1-160 >&2; die "a database password is left in the copy: redact it (step 3) and re-run"; }
if [ "$COMMIT" != 1 ]; then echo "sync-workspace-branch: copied and redacted into $WS_DIR (COMMIT=0: nothing staged)"; exit 0; fi

# 4. Stage, then check the change for secrets before committing.
cd "$WS_DIR"
git add --all --force
if git diff --cached --quiet; then echo "sync-workspace-branch: nothing changed; nothing to commit"; else
  bad_names=$(git diff --cached --name-only --diff-filter=AM \
    | grep -iE '\.(jks|keystore|p12|pfx|pem|key)$|(^|/)google-services\.json$|GoogleService-Info\.plist$|(^|/)local\.properties$|keystore\.properties$|(^|/)\.env($|\.)|recovery-codes|PeriFlow-Postman|\.jsonl$|/gmail/' || true)
  bad_text=$(git diff --cached -U0 | grep '^+' | grep -v 'PLACEHOLDER' \
    | grep -nE 'ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|gh[osur]_[A-Za-z0-9]{30,}|sk-ant-[A-Za-z0-9_-]{8,}|sk-[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{30,}|AKIA[0-9A-Z]{16}|xox[abposr]-[A-Za-z0-9-]{10,}|-----BEGIN [A-Z ]*PRIVATE KEY-----|eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.' \
    | cut -d: -f1 || true)
  if [ -n "$bad_names$bad_text" ]; then
    git reset -q
    [ -n "$bad_names" ] && printf 'secret-like file names:\n%s\n' "$bad_names" >&2
    [ -n "$bad_text" ] && printf 'secret-like strings on %s added diff line(s) (values not printed)\n' "$(printf '%s\n' "$bad_text" | wc -l)" >&2
    die "not committed: review the hits (git -C $WS_DIR diff), exclude or redact them, and re-run"
  fi
  git commit -q -m "$MSG

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
  echo "committed $(git rev-parse --short HEAD): $(git show --stat --format= HEAD | tail -1)"
fi

# 5. Push (fast-forward only; never --force, never main).
if [ "$PUSH" = 1 ]; then
  nice -n 19 git -c pack.threads=1 push "$AB_URL" "HEAD:refs/heads/$WSB"
  echo "GitHub $WSB: $(git ls-remote "$AB_URL" "refs/heads/$WSB" | cut -c1-12); local: $(git rev-parse --short=12 HEAD)"
fi
