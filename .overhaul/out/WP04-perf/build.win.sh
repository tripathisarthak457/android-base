#!/usr/bin/env bash
# Windows port of build.sh + floor-build.sh: each <name>=<commit> is generated from that commit's own generator and
# templates (a git archive under s/<commit>, so the worktree is never switched) and built under the Gradle lock.
# A name of "floor" takes the commit's project with every navigation entry's content removed (floor-build.sh).
# Every build keeps the everything preset's package, so measure.win.sh drives them all the same way.
#   ./build.win.sh everything=3c9c403 fixed=730f8e0 floor=730f8e0 head=fb866b3 > logs/<step>.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked; O=$OV/out/WP04-perf
names=()
for pair in "$@"; do
  n=${pair%%=*}; c=$(git -C $W rev-parse --short=7 "${pair#*=}")
  src=$O/s/$c
  if [ ! -d $src/generator ]; then
    mkdir -p $src
    git -C $W archive $c | tar -x -C $src
    git -C $W rev-parse --short=12 $c > $src/generator/COMMIT
  fi
  python3 $OV/out/WP04/rmtree-long.py $O/$n
  (cd $src/generator && python3 create_project.py --spec tests/fixtures/v2/everything.json \
    --set package_name=com.example.wp04.everything --out $O/$n --no-zip --force > $O/logs/gen-$n.log)
  echo $c > $O/$n/BUILT_FROM
  : > $O/$n/BUILT_DIRTY
  if [ "$n" = floor ]; then
    f=$O/floor/core/navigation/src/main/kotlin/com/example/wp04/everything/core/navigation/stack/StackHost.kt
    # The third argument was renamed (screenBars, then screens) between commits.
    test "$(grep -cE 'EntryFrame\(contentKey, hides, [a-zA-Z]+\) \{ destination\.content\(shown\) \}' $f)" = 1
    sed -i -E 's/(EntryFrame\(contentKey, hides, [a-zA-Z]+\) \{) destination\.content\(shown\) \}/\1 }/' $f
    grep -qE 'EntryFrame\(contentKey, hides, [a-zA-Z]+\) \{ \}' $f
    echo "StackHost.kt: entry content removed" > $O/floor/BUILT_DIRTY
  fi
  names+=("$n")
  echo "GENERATED $n from $c"
done
$L gradle bash -c '
set -euo pipefail
for n in "$@"; do
  cd '"$O"'/$n
  ./gradlew :app:assembleDevBenchmarkRelease --console=plain
  echo "BUILT $n"
done' _ "${names[@]}"
echo BUILD OK
