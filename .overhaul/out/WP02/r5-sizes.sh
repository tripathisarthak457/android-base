#!/usr/bin/env bash
# Post-M2 sizes (devRelease, ABI splits) and configuration time, next to the pre-M2 projects.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
L=${L:-$OV/bin/locked}
cd $OV/wt/WP02/generator
for p in lean standard everything; do
  python3 create_project.py --spec tests/fixtures/v2/$p.json --out $O/post-m2/$p --no-zip --force > $O/logs/gen-post-$p.log 2>&1 || echo "GEN FAILED $p"
done
$O/sizes.sh post-M2 $O/post-m2/lean $O/post-m2/standard $O/post-m2/everything
# Configuration time without the configuration cache, warm daemon: the second of two runs.
for d in pre-m2/everything post-m2/everything; do
  for i in 1 2; do
    t=$(cd $O/$d && $L gradle ./gradlew help --no-configuration-cache --console=plain 2>&1 | grep -o 'BUILD SUCCESSFUL in .*')
  done
  echo "config $d $t"
  echo "variants $d $(cd $O/$d && $L gradle ./gradlew :app:tasks --all --console=plain 2>/dev/null | grep -oE '^assemble(Dev|Staging|Prod|Playstore)(Debug|Release|BenchmarkRelease|NonMinifiedRelease) ' | sort -u | wc -l)"
done
echo "exit=done"
