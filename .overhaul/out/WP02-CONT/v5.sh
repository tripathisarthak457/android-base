#!/usr/bin/env bash
# Run 2, batch 5, on the committed branch: (a) the stability check against task order on every preset: each checked
# module's unit-test compile forced to run, then its check, one worker so the order asked is the order run (as
# staging's f33); (b) the devRelease arm64 APK and dex sizes of lean, standard and everything for
# tools/size_budgets.toml (G-7).
#   bash v5.sh > logs/v5.log 2>&1
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
P=$O/presets
step() { echo; echo "== $(date +%H:%M:%S) $*"; }
mkdir -p $P

step "generate at $(git -C $W rev-parse HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"
for p in none lean standard all everything; do
  case $p in all) flag="--all";; none) flag="";; *) flag="--preset $p";; esac
  printf '{"schema": 2, "platform": "android", "app_name": "Size %s", "package_name": "com.example.size%s", "features": []}\n' $p $p > $P/$p.json
  rm -rf $P/$p
  (cd $W/generator && python -B create_project.py --spec $P/$p.json $flag --out $P/$p --no-zip --force > $O/logs/v5-gen-$p.log 2>&1) || echo "GEN FAILED $p"
done
ls $P

for p in none lean standard all everything; do
  step "$p: the stability check after a forced unit-test compile, one worker"
  tasks=""
  for m in designsystem navigation ui; do
    [ -d $P/$p/core/$m ] && tasks="$tasks :core:$m:compileDebugUnitTestKotlin --rerun :core:$m:checkComposeStability --rerun"
  done
  (cd $P/$p && OVERHAUL_GRADLE_WORKERS=1 $L gradle $GW --offline $tasks --continue --console=plain) > $O/logs/v5-stability-$p.log 2>&1; echo "rc=$?"
  grep -E "^> Task :core:(designsystem|navigation|ui):(compileDebugKotlin|compileDebugUnitTestKotlin|checkComposeStability)|BUILD|identity" $O/logs/v5-stability-$p.log
done

for p in lean standard everything; do
  step "$p: devRelease with ABI splits"
  (cd $P/$p && $L gradle $GW --offline :app:assembleDevRelease -PabiSplits=true --console=plain) > $O/logs/v5-size-$p.log 2>&1; echo "rc=$?"
  grep -E "BUILD" $O/logs/v5-size-$p.log
  python - $p $P/$p/app/build/outputs/apk/dev/release <<'PY'
import os, sys, zipfile
preset, folder = sys.argv[1], sys.argv[2]
for name in sorted(os.listdir(folder)):
    if not name.endswith(".apk"):
        continue
    path = os.path.join(folder, name)
    with zipfile.ZipFile(path) as apk:
        dex = sum(i.file_size for i in apk.infolist() if i.filename.startswith("classes") and i.filename.endswith(".dex"))
    print(f"SIZE {preset} {name} apk={os.path.getsize(path)} dex={dex}")
PY
done

echo; echo "V5 DONE $(date +%H:%M:%S)"
