#!/usr/bin/env bash
# The back-gesture ladder of research/back-google/DESIGN.md §5: four builds of one wp/WP04 commit, each generated
# from a git archive of that commit (no worktree is touched) with the ladder's patches applied in order, then
# instrument.py's trace counters, into out/WP04-perf/bg-<name>:
#   bg-head  the commit without backcard-texture.patch    bg-tex  with it
#   bg-c1    bg-tex + out/WP04-perf/back-google/c1.patch   bg-c2   bg-c1 + out/WP04-perf/back-google/c2.patch
# :core:navigation's unit tests run in each patched tree before its build. Gradle runs only under the lock, one
# build at a time. Stops if an output folder exists: pick a new commit or move the old folders to the Recycle Bin.
# HEAD_BASE=<commit> builds bg-head from that commit as it is (no texture handling): use the parent of WP04's
# texture commit if `git apply -R` of the patch fails on a reworded T. Patches are applied with core.autocrlf off: outside
# a repository the user's global autocrlf would write CRLF, and instrument.py's anchors would then miss.
#   [HEAD_BASE=<commit>] ./build-ladder.win.sh <commit> > D:/projects/tools/.overhaul/out/WP04-perf/logs/bg-build.log 2>&1
set -euo pipefail
export MSYS_NO_PATHCONV=1
OV=D:/projects/tools/.overhaul; R=$OV/research/back-google; O=$OV/out/WP04-perf; L=$OV/bin/locked
W=$OV/wt/WP04-perf    # read only: `git archive` and `git show` of the shared repository
B=$(git -C $W rev-parse --short=8 "$1")
TEX=$O/backcard-texture.patch
PATCHES=$O/back-google
APPLY="git -c core.autocrlf=false apply"
CARD=templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/back/BackCard.kt
if git -C $W show "$B:$CARD" | grep -q "compositingStrategy = CompositingStrategy.Offscreen"; then base_has_tex=1; else base_has_tex=0; fi
echo "== $(date '+%F %H:%M:%S') ladder from $B (texture already in it: $base_has_tex)"
for name in head tex c1 c2; do
  test ! -e $O/bg-$name || { echo "exists: $O/bg-$name"; exit 1; }
  test ! -e $O/s/bg-$name-$B || { echo "exists: $O/s/bg-$name-$B"; exit 1; }
done
for name in head tex c1 c2; do
  src=$O/s/bg-$name-$B
  mkdir -p $src
  applied=()
  if [ $name = head ] && [ -n "${HEAD_BASE:-}" ]; then
    git -C $W archive "$HEAD_BASE" | tar -x -C $src; applied+=("head from $HEAD_BASE")
  else
    git -C $W archive $B | tar -x -C $src
  fi
  if [ $name = head ] && [ -z "${HEAD_BASE:-}" ] && [ $base_has_tex = 1 ]; then (cd $src && $APPLY -R $TEX); applied+=("-R texture"); fi
  if [ $name != head ] && [ $base_has_tex = 0 ]; then (cd $src && $APPLY $TEX); applied+=("texture"); fi
  if [ $name = c1 ] || [ $name = c2 ]; then (cd $src && $APPLY $PATCHES/c1.patch); applied+=("c1"); fi
  if [ $name = c2 ]; then (cd $src && $APPLY $PATCHES/c2.patch); applied+=("c2"); fi
  python -I $R/bin/instrument.py $src
  applied+=("instrument")
  echo "PATCHED bg-$name: $B + ${applied[*]}"
  $L gradle bash -c "set -euo pipefail; cd $src/templates/android && ./gradlew :core:navigation:testDebugUnitTest --console=plain"
  echo "TESTED bg-$name"
  (cd $src/generator && python3 create_project.py --spec tests/fixtures/v2/everything.json \
    --set package_name=com.example.wp04.everything --out $O/bg-$name --no-zip --force > $O/logs/gen-bg-$name.log)
  echo "$B" > $O/bg-$name/BUILT_FROM
  echo "${applied[*]}" > $O/bg-$name/BUILT_DIRTY
  $L gradle bash -c "set -euo pipefail; cd $O/bg-$name && ./gradlew :app:assembleDevBenchmarkRelease --console=plain"
  echo "BUILT bg-$name"
done
echo "== $(date '+%F %H:%M:%S') LADDER BUILT"
