#!/usr/bin/env bash
# Run 5, batch 9, on the branch at its committed head (staging 4760b407 merged, nothing new on staging):
#  1. the five presets regenerated from the worktree (v6's were generated before run 4's commits);
#  2. the stability check against task order on `none` (v8's waited 18 min behind the lock and exited 66 at 09:05,
#     the same second as accept's template build), then on the other four again at this head;
#  3. devRelease arm64 APK and dex sizes of lean, standard and everything at this head, for tools/size_budgets.toml;
#  4. the template's full `build --continue` (accept's template-build, which also exited 66 behind the lock).
#   bash v9.sh > logs/v9.log 2>&1      (in the background)
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }
step "branch $(git -C $W rev-parse --short HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"

step "1. presets: generate"
PR=$O/presets
mkdir -p $PR
for p in none lean standard all everything; do
  case $p in all) flag="--all";; none) flag="";; *) flag="--preset $p";; esac
  printf '{"schema": 2, "platform": "android", "app_name": "Size %s", "package_name": "com.example.size%s", "features": []}\n' $p $p > $PR/$p.json
  rm -rf $PR/$p
  (cd $W/generator && python -B create_project.py --spec $PR/$p.json $flag --out $PR/$p --no-zip --force > $O/logs/v9-gen-$p.log 2>&1) && echo "generated $p" || echo "GEN FAILED $p"
done

for p in none lean standard all everything; do
  step "2. $p: the stability check after a forced unit-test compile, one worker"
  P=$PR/$p
  tasks=""
  for m in designsystem navigation ui; do
    [ -d $P/core/$m ] || continue
    [ -d $P/core/$m/src/test ] && tasks="$tasks :core:$m:compileDebugUnitTestKotlin --rerun"
    tasks="$tasks :core:$m:checkComposeStability --rerun"
  done
  echo "tasks:$tasks"
  (cd $P && OVERHAUL_GRADLE_WORKERS=1 $L gradle $GW --offline $tasks --continue --console=plain) > $O/logs/v9-stability-$p.log 2>&1; echo "rc=$?"
  grep -E "^> Task :core:(designsystem|navigation|ui):(compileDebugKotlin|compileDebugUnitTestKotlin|checkComposeStability)|^BUILD|unstable|FAILED|^\[locked" $O/logs/v9-stability-$p.log
  for m in designsystem navigation ui; do
    [ -d $P/core/$m/build/compose_stability ] && echo "  $m: report read $(cat $P/core/$m/build/compose_stability/*-composables.txt | wc -l) lines; the shared file now $(cat $P/core/$m/build/compose_reports/*-composables.txt 2>/dev/null | wc -l) lines"
  done
done

for p in lean standard everything; do
  step "3. $p: devRelease with ABI splits"
  (cd $PR/$p && $L gradle $GW --offline :app:assembleDevRelease -PabiSplits=true --console=plain) > $O/logs/v9-size-$p.log 2>&1; echo "rc=$?"
  grep -E "^BUILD|^\[locked" $O/logs/v9-size-$p.log
  python -I - $p $PR/$p/app/build/outputs/apk/dev/release <<'PY'
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

step "4. the template: build --continue"
T=$W/templates/android
(cd $T && timeout 180m $L gradle $GW build --continue --console=plain) > $O/logs/v9-template-build.log 2>&1; echo "rc=$?"
grep -E "^BUILD|FAILED|^\[locked|actionable tasks" $O/logs/v9-template-build.log | head -40
python -I - $T <<'PY'
import glob, sys, xml.etree.ElementTree as ET
root = sys.argv[1]
cases = fails = skipped = 0
for f in glob.glob(root + "/**/build/test-results/**/TEST-*.xml", recursive=True):
    for case in ET.parse(f).getroot().iter("testcase"):
        cases += 1
        if case.find("skipped") is not None:
            skipped += 1
        if case.find("failure") is not None or case.find("error") is not None:
            fails += 1
            print("FAIL", f.split("build")[0][len(root):], case.get("classname").split(".")[-1], case.get("name"))
print(f"template unit tests: cases={cases} failures={fails} skipped={skipped}")
PY
echo "git status after: $(git -C $W status --porcelain | wc -l) changed paths"

echo; echo "V9 DONE $(date +%H:%M:%S)"
