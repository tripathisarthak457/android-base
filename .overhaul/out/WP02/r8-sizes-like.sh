#!/usr/bin/env bash
# Post-M2 sizes for the pre-M2 lean and standard feature sets (A5.2 redefined both presets), so the
# comparison isolates M2's build changes from the preset change.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
cd $OV/wt/WP02/generator
for p in lean standard; do
  python3 create_project.py --spec $O/specs/pre-m2-$p.json --out $O/post-m2/$p-preset-pre --no-zip --force > $O/logs/gen-post-$p-preset-pre.log 2>&1 || echo "GEN FAILED $p"
done
$O/sizes.sh post-M2-pre-preset $O/post-m2/lean-preset-pre $O/post-m2/standard-preset-pre
echo "exit=done"
