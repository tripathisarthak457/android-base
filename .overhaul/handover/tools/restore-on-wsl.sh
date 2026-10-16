#!/usr/bin/env bash
# restore-on-wsl.sh: rebuild the android-base overhaul workspace on a fresh Linux machine (WSL2 Ubuntu on Windows)
# from the GitHub backup of 2026-10-16. Read .overhaul/handover/RESUME-ON-WINDOWS.md (sections 1 and 3) first.
#
# It is safe to re-run: it never pushes, never deletes, never overwrites a file that already exists, and never
# touches a worktree that already has uncommitted changes.
#
# It understands both layouts the backup can have (RESUME-ON-WINDOWS.md section 1):
#   branches    every package branch on GitHub, WIP as backup/*-wip-2026-10-16 branches, the workspace as the orphan
#               branch overhaul-workspace (the layout of scripts/handover-backup.js);
#   one-branch  everything merged into main, branches deleted, every former branch kept as a tag
#               archive/<branch>-2026-10-16, the workspace under overhaul-workspace/ on main
#               (the layout of scripts/merge-all-to-main.js).
#
# Steps:
#   1. Clones android-base (every branch and tag) into $TOOLS/android-base; checks out the integration branch
#      (branches layout) or stays on main (one-branch layout).
#   2. Copies the workspace (.overhaul/ and the root docs) into $TOOLS.
#   3. Recreates every package worktree $TOOLS/.overhaul/wt/<name> on a local branch wp/<name> with no upstream
#      (one-branch layout: only with RECREATE_BRANCHES=1, from the archive tags).
#   4. Re-applies every WIP snapshot as UNCOMMITTED changes in its worktree.
#   5. Clones PeriFlow (private; run `gh auth login` and `gh auth setup-git` first) into $PF_DIR and recreates
#      wt/WP11 (branch overhaul/smooth-glass, no upstream, with its WIP) and wt/WP11-base (detached at $PF_BASE).
#   6. Copies Claude's memory into $CLAUDE_DIR/projects/<slug of $TOOLS>/memory (existing files are kept).
#   7. Prints a status table to compare with RESUME-ON-WINDOWS.md section 3.6.
#
# Every path can be overridden through the environment; the defaults reproduce the Linux layout exactly:
#   TOOLS, WS_DIR, PF_DIR, AB_URL, PF_URL, CLAUDE_DIR, PF_BASE, SKIP_PERIFLOW=1, RECREATE_BRANCHES=1
set -euo pipefail

TOOLS=${TOOLS:-$HOME/Documents/projects/tools}
WS_DIR=${WS_DIR:-$(dirname "$TOOLS")/tools-ws}
PF_DIR=${PF_DIR:-$HOME/Documents/projects/jorwen/periflow}
AB_URL=${AB_URL:-https://github.com/tripathisarthak457/android-base.git}
PF_URL=${PF_URL:-https://github.com/tripathisarthak457/periflow.git}
CLAUDE_DIR=${CLAUDE_DIR:-$HOME/.claude}
PF_BASE=${PF_BASE:-a5b630a}          # PeriFlow commit the WP11 "before" builds came from (on overhaul/smooth-glass)
SKIP_PERIFLOW=${SKIP_PERIFLOW:-0}
RECREATE_BRANCHES=${RECREATE_BRANCHES:-0}
INTEG=overhaul/multiplatform-v2
WSB=overhaul-workspace
DAY=2026-10-16

R=$TOOLS/android-base
OV=$TOOLS/.overhaul

say() { printf '\n== %s\n' "$*"; }
warn() { printf 'WARNING: %s\n' "$*" >&2; }
need() { command -v "$1" >/dev/null || { echo "missing command: $1 (sudo apt install $2)" >&2; exit 1; }; }
need git git; need rsync rsync; need sed sed

# Puts a branch in a new worktree. A local branch is reused; otherwise it is created from <start> with no upstream,
# so a bare `git push` in an agent's shell cannot publish anything by accident.
add_worktree() { # <repo> <path> <branch> <start>
  local repo=$1 path=$2 branch=$3 start=$4
  if [ -e "$path/.git" ]; then echo "  $path: already a worktree"; return 0; fi
  if [ -e "$path" ] && [ -n "$(ls -A "$path" 2>/dev/null)" ]; then
    warn "$path exists and is not empty (restored from the offline zip?): move it aside, then re-run"; return 0
  fi
  if git -C "$repo" show-ref -q --verify "refs/heads/$branch"; then
    git -C "$repo" worktree add -q "$path" "$branch"
  else
    git -C "$repo" worktree add -q --no-track -b "$branch" "$path" "$start^{commit}"
  fi
  echo "  $path: $branch @ $(git -C "$path" rev-parse --short HEAD)"
}

# Writes a WIP snapshot's files into the worktree without staging them: modified and deleted tracked files come
# back as unstaged changes, new files as untracked files, which is exactly the state the snapshot was taken from.
apply_wip() { # <worktree> <snapshot ref> <label>
  local wt=$1 snap=$2 label=$3 parent head
  parent=$(git -C "$wt" rev-parse "$snap^{commit}^")
  head=$(git -C "$wt" rev-parse HEAD)
  if [ "$parent" != "$head" ]; then
    warn "$label: the snapshot was taken on ${parent:0:7}, but the worktree is at ${head:0:7}; not applied (see RESUME-ON-WINDOWS.md 3.6)"
    return 0
  fi
  if [ -n "$(git -C "$wt" status --porcelain)" ]; then
    echo "  $label: worktree already has uncommitted changes; left as it is"; return 0
  fi
  git -C "$wt" restore --source="$snap^{commit}" --worktree -- .
  echo "  $label: $(git -C "$wt" status --porcelain | wc -l) uncommitted entries restored from $snap"
}

# The branch a snapshot belongs to, from its message ("WIP snapshot of wp/WP03 before the laptop wipe ...").
snap_branch() { # <repo> <ref>
  git -C "$1" log -1 --format=%s "$2^{commit}" | sed -n 's/^WIP snapshot of \([^ ]*\) .*/\1/p'
}

say "1. android-base -> $R"
mkdir -p "$TOOLS"
if [ -d "$R/.git" ]; then git -C "$R" fetch -q origin --prune --tags; else git clone "$AB_URL" "$R"; fi
if git -C "$R" rev-parse -q --verify "refs/remotes/origin/$INTEG" >/dev/null; then
  LAYOUT=branches
elif [ -n "$(git -C "$R" tag -l 'archive/*')" ] && git -C "$R" cat-file -e "origin/main:$WSB" 2>/dev/null; then
  LAYOUT=one-branch
else
  echo "Neither origin/$INTEG nor archive/* tags with $WSB/ on main: the backup is not what this script expects." >&2
  echo "Stop and read the newest HANDOVER.md (on the $WSB branch, or under $WSB/ on main)." >&2
  exit 2
fi
echo "  layout: $LAYOUT"
if [ "$LAYOUT" = branches ]; then
  cur=$(git -C "$R" symbolic-ref --short -q HEAD || true)
  if [ "$cur" != "$INTEG" ]; then
    if [ -n "$(git -C "$R" status --porcelain)" ]; then echo "$R has uncommitted changes; stop." >&2; exit 2; fi
    if git -C "$R" show-ref -q --verify "refs/heads/$INTEG"; then git -C "$R" switch -q "$INTEG"
    else git -C "$R" switch -q --no-track -c "$INTEG" "origin/$INTEG"; fi
  fi
fi
echo "  checkout: $(git -C "$R" rev-parse --abbrev-ref HEAD) @ $(git -C "$R" rev-parse --short HEAD)"
echo "  merged tags: $(git -C "$R" tag -l 'merged/*' | tr '\n' ' ')"

say "2. workspace -> $OV"
if [ "$LAYOUT" = branches ]; then
  if [ -d "$WS_DIR/.git" ]; then
    git -C "$WS_DIR" pull -q --ff-only || warn "could not fast-forward $WS_DIR; using it as it is"
  else
    # --reference borrows the objects the android-base clone already downloaded; --dissociate then copies them, so
    # the two clones stay independent.
    git clone -q --single-branch -b "$WSB" --reference-if-able "$R" --dissociate "$AB_URL" "$WS_DIR"
  fi
  SRC=$WS_DIR
else
  SRC=$R/$WSB
fi
mkdir -p "$OV"
# --ignore-existing: a re-run never overwrites a file someone has edited since.
rsync -a --ignore-existing --exclude=.git "$SRC/.overhaul/" "$OV/"
if [ ! -e "$TOOLS/CLAUDE.md" ]; then
  if [ -e "$SRC/tools-CLAUDE.md" ]; then cp "$SRC/tools-CLAUDE.md" "$TOOLS/CLAUDE.md"
  elif [ -e "$SRC/CLAUDE.md" ]; then cp "$SRC/CLAUDE.md" "$TOOLS/CLAUDE.md"; fi
fi
if [ ! -e "$TOOLS/HANDOVER.md" ]; then
  if [ -e "$OV/handover/HANDOVER.md" ]; then ln -s .overhaul/handover/HANDOVER.md "$TOOLS/HANDOVER.md"
  elif [ -e "$SRC/HANDOVER.md" ]; then cp "$SRC/HANDOVER.md" "$TOOLS/HANDOVER.md"; fi
fi
echo "  from $SRC: $(find "$OV" -path "$OV/wt" -prune -o -type f -print | wc -l) files in $OV (outside wt/)"

say "3. package worktrees in $OV/wt"
git -C "$R" worktree prune
if [ "$LAYOUT" = branches ]; then
  for b in $(git -C "$R" for-each-ref --format='%(refname:strip=3)' refs/remotes/origin/wp/); do
    add_worktree "$R" "$OV/wt/${b#wp/}" "$b" "origin/$b"
  done
elif [ "$RECREATE_BRANCHES" = 1 ]; then
  # The old integration branch too, as a plain local branch: main (which has everything merged) stays checked out.
  if ! git -C "$R" show-ref -q --verify "refs/heads/$INTEG" \
     && git -C "$R" rev-parse -q --verify "archive/$INTEG-$DAY^{commit}" >/dev/null; then
    git -C "$R" branch -q --no-track "$INTEG" "archive/$INTEG-$DAY^{commit}"
    echo "  local branch $INTEG recreated from archive/$INTEG-$DAY (not checked out)"
  fi
  for t in $(git -C "$R" for-each-ref --format='%(refname:strip=2)' refs/tags/archive/wp/); do
    b=${t#archive/}; b=${b%-"$DAY"}
    add_worktree "$R" "$OV/wt/${b#wp/}" "$b" "$t"
  done
else
  echo "  one-branch layout: the package branches were deleted on purpose (the user wanted a single branch)."
  echo "  Work as HANDOVER.md says. To recreate them from the archive tags, re-run with RECREATE_BRANCHES=1."
fi

say "4. WIP snapshots as uncommitted changes"
if [ "$LAYOUT" = branches ]; then
  snaps=$(git -C "$R" for-each-ref --format='%(refname:strip=2)' refs/remotes/origin/backup/ | grep -e "-wip-$DAY\$" || true)
else
  snaps=$(git -C "$R" for-each-ref --format='%(refname:strip=2)' refs/tags/archive/backup/ | grep -e '-wip-' || true)
fi
for snap in $snaps; do
  b=$(snap_branch "$R" "$snap")
  if [ -z "$b" ]; then n=${snap##*/backup/}; n=${n%%-wip-*}; n=${n#wp-}; b=wp/$n; fi
  wt=$OV/wt/${b#wp/}
  if [ ! -e "$wt/.git" ]; then echo "  $snap: no worktree for $b; skipped"; continue; fi
  apply_wip "$wt" "$snap" "$b"
done

if [ "$SKIP_PERIFLOW" != 1 ]; then
  say "5. PeriFlow (PRIVATE) -> $PF_DIR"
  mkdir -p "$(dirname "$PF_DIR")"
  if [ -d "$PF_DIR/.git" ]; then git -C "$PF_DIR" fetch -q origin --prune --tags; else git clone "$PF_URL" "$PF_DIR"; fi
  git -C "$PF_DIR" worktree prune
  pf_start=; pf_snap=
  if git -C "$PF_DIR" rev-parse -q --verify origin/overhaul/smooth-glass >/dev/null; then
    pf_start=origin/overhaul/smooth-glass
    git -C "$PF_DIR" rev-parse -q --verify "origin/backup/smooth-glass-wip-$DAY" >/dev/null && pf_snap=origin/backup/smooth-glass-wip-$DAY
  elif [ "$RECREATE_BRANCHES" = 1 ] && git -C "$PF_DIR" rev-parse -q --verify "archive/overhaul/smooth-glass-$DAY" >/dev/null; then
    pf_start=archive/overhaul/smooth-glass-$DAY
    pf_snap=$(git -C "$PF_DIR" for-each-ref --format='%(refname:strip=2)' refs/tags/archive/backup/ | grep -e 'smooth-glass-wip' | head -1 || true)
  fi
  if [ -n "$pf_start" ]; then
    add_worktree "$PF_DIR" "$OV/wt/WP11" overhaul/smooth-glass "$pf_start"
    if [ -n "$pf_snap" ]; then apply_wip "$OV/wt/WP11" "$pf_snap" overhaul/smooth-glass
    else warn "no smooth-glass WIP snapshot found; WP11's 2 uncommitted files were not restored"; fi
  else
    echo "  overhaul/smooth-glass is not on the remote (one-branch layout?): wt/WP11 not created."
    echo "  It is merged into PeriFlow's main; RECREATE_BRANCHES=1 recreates it from archive/overhaul/smooth-glass-$DAY."
  fi
  if [ -e "$OV/wt/WP11-base/.git" ]; then echo "  $OV/wt/WP11-base: already a worktree"
  elif git -C "$PF_DIR" rev-parse -q --verify "$PF_BASE^{commit}" >/dev/null; then
    git -C "$PF_DIR" worktree add -q --detach "$OV/wt/WP11-base" "$PF_BASE"
    echo "  $OV/wt/WP11-base: detached @ $PF_BASE"
  else warn "PeriFlow commit $PF_BASE not found; wt/WP11-base not created"; fi
  echo "  PeriFlow builds also need its ignored files (keystore.properties, local.properties, keys/, the four"
  echo "  google-services.json, ~/.gradle/gradle.properties periflow.*) from the user's private storage; link them"
  echo "  into wt/WP11 as design/wp/WP11.md 'Setup' shows. They are NOT in any git backup, by design."
fi

say "6. Claude memory"
slug=$(printf '%s' "$TOOLS" | sed 's/[^A-Za-z0-9]/-/g')
dst=$CLAUDE_DIR/projects/$slug/memory
mkdir -p "$dst"
if [ -d "$SRC/claude-memory" ]; then
  for f in "$SRC"/claude-memory/*; do
    [ -e "$f" ] || continue
    if [ -e "$dst/$(basename "$f")" ]; then echo "  kept existing $(basename "$f")"; else cp "$f" "$dst/"; fi
  done
  echo "  $dst: $(ls "$dst" | tr '\n' ' ')"
else
  warn "$SRC/claude-memory not found"
fi

say "7. status"
printf '  %-13s %-27s %-8s %s\n' worktree branch head uncommitted
for wt in "$OV"/wt/*/; do
  [ -e "$wt/.git" ] || continue
  printf '  %-13s %-27s %-8s %s\n' "$(basename "$wt")" "$(git -C "$wt" rev-parse --abbrev-ref HEAD)" \
    "$(git -C "$wt" rev-parse --short HEAD)" "$(git -C "$wt" status --porcelain | wc -l)"
done
echo
echo "Done. Next: RESUME-ON-WINDOWS.md section 3.6 (check the WIP counts), then sections 4 to 8."
