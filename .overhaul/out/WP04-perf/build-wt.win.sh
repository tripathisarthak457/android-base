#!/usr/bin/env bash
# The everything preset generated from the worktree as it is (uncommitted template changes included) into
# out/WP04-perf/<name> and built under the Gradle lock: the B side of an A/B before a change is committed.
#   ./build-wt.win.sh <name> > logs/<step>.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04-perf; L=$OV/bin/locked; O=$OV/out/WP04-perf
n=$1
python3 $OV/out/WP04/rmtree-long.py $O/$n
(cd $W/generator && python3 create_project.py --spec tests/fixtures/v2/everything.json \
  --set package_name=com.example.wp04.everything --out $O/$n --no-zip --force > $O/logs/gen-$n.log)
git -C $W rev-parse --short HEAD > $O/$n/BUILT_FROM
git -C $W status --short -- templates > $O/$n/BUILT_DIRTY
echo "GENERATED $n from $(cat $O/$n/BUILT_FROM) + $(wc -l < $O/$n/BUILT_DIRTY) dirty"
$L gradle bash -c "cd $O/$n && ./gradlew :app:assembleDevBenchmarkRelease --console=plain"
echo "BUILT $n"
