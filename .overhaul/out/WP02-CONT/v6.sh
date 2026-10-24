#!/usr/bin/env bash
# Run 3, batch 6, on the branch after the staging merge (2e447a6f) with step 4 and the audit uncommitted:
#  1. generator suite;
#  2. c-local (no network, no database: the no-source data module) assembled, its features tested, every module's
#     code rules (a data module without src/test has no unit-test task);
#  3. AdaptiveAuditTest on a generated project with the reference feature and two scaffolded modules (expect PASS),
#     then with a planted screen that breaks each rule (expect FAIL naming each planted node);
#  4. the audit on the template's own screens, every feature on, with the app's code rules;
#  5. the stability check against task order on every preset (each checked module's unit-test compile forced first,
#     one worker, so the order asked is the order run, as staging's f33);
#  6. devRelease arm64 APK and dex sizes of lean, standard and everything for tools/size_budgets.toml (G-7).
#   bash v6.sh > logs/v6.log 2>&1
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
M=$OV/bin/memwait
GW="bash $OV/out/WP02/gw.win.sh"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }
step "branch $(git -C $W rev-parse --short HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"

step "1. generator suite"
(cd $W/generator && $M python -B -m unittest discover -s tests -t .) > $O/logs/v6-gen-suite.log 2>&1; echo "rc=$?"
grep -E "^Ran|^OK|^FAILED" $O/logs/v6-gen-suite.log

step "2. c-local and the audit project: generate"
for s in c-local audit; do
  rm -rf $O/$s
  (cd $W/generator && python -B create_project.py --spec $O/specs/$s.json --out $O/$s --no-zip --force > $O/logs/v6-gen-$s.log 2>&1) || echo "GEN FAILED $s"
done

step "2. c-local: build"
tasks=":app:assembleDevDebug :app:verifyCodeRules"
for m in $(python -c "import json; print(' '.join(json.load(open('$O/specs/c-local.json'))['feature_modules']))"); do
  tasks="$tasks :feature:$m:testDebugUnitTest :feature:$m:verifyCodeRules :data:$m:verifyCodeRules"
done
(cd $O/c-local && $L gradle $GW --offline $tasks --continue --console=plain) > $O/logs/v6-c-local.log 2>&1; echo "rc=$?"
grep -E "FAILED|BUILD|e: file" $O/logs/v6-c-local.log | head -20

step "3. the audit on the generated screens (expect PASS)"
P=$O/audit
(cd $P && $L gradle $GW --offline :app:verifyCodeRules :core:database:compileDebugKotlin :data:order:testDebugUnitTest :feature:order:testDebugUnitTest :app:testDevDebugUnitTest --console=plain) > $O/logs/v6-audit.log 2>&1; echo "rc=$?"
grep -E "BUILD|FAILED" $O/logs/v6-audit.log | head
python - $P/app/build/test-results/testDevDebugUnitTest <<'PY'
import sys, glob, xml.etree.ElementTree as ET
cases = fails = skips = 0
names = set()
for f in glob.glob(sys.argv[1] + "/TEST-*Adaptive*.xml"):
    r = ET.parse(f).getroot()
    cases += int(r.get("tests")); fails += int(r.get("failures")) + int(r.get("errors")); skips += int(r.get("skipped"))
    for case in r.iter("testcase"):
        names.add(case.get("name").split("[")[1].split(" at ")[0] if "[" in case.get("name") else case.get("name"))
print(f"audit cases={cases} failures={fails} skipped={skips}")
print("screens:", sorted(names))
PY
cp -r $P/app/build/test-results/testDevDebugUnitTest $O/logs/v6-audit-results 2>/dev/null

step "3. a planted screen that breaks every rule (expect FAIL naming each planted node)"
cp $O/audit-planted/AuditPlanted.kt $P/app/src/main/kotlin/io/example/audit/AuditPlanted.kt
(cd $P && $L gradle $GW --offline :app:testDevDebugUnitTest --tests '*AdaptiveAuditTest*' --console=plain) > $O/logs/v6-planted.log 2>&1; echo "rc=$?"
rm -f $P/app/src/main/kotlin/io/example/audit/AuditPlanted.kt
python - $P/app/build/test-results/testDevDebugUnitTest <<'PY'
import sys, glob, re, xml.etree.ElementTree as ET
seen, passed, failed = {}, 0, 0
for f in glob.glob(sys.argv[1] + "/TEST-*Adaptive*.xml"):
    for case in ET.parse(f).getroot().iter("testcase"):
        name = case.get("name")
        failure = case.find("failure")
        if "AuditPlanted" not in name:
            if failure is not None:
                print("A REAL SCREEN FAILED:", name)
            continue
        if failure is None:
            passed += 1; print("PLANTED PASSED (should fail):", name); continue
        failed += 1
        for line in (failure.get("message") or "").splitlines()[1:]:
            line = line.strip()
            if line.startswith('"'):
                kind = re.sub(r"[0-9.]+", "#", line)
                seen.setdefault(kind, set()).add(name.split(" at ")[1].split("]")[0])
print(f"planted cases failed={failed} passed={passed}")
for kind, where in sorted(seen.items()):
    print(f"  {kind}  <- {sorted(where)}")
PY
cp -r $P/app/build/test-results/testDevDebugUnitTest $O/logs/v6-planted-results 2>/dev/null

step "4. the audit on the template's own screens, every feature on"
T=$W/templates/android
(cd $T && $L gradle $GW --offline :app:verifyCodeRules :app:testDevDebugUnitTest --continue --console=plain) > $O/logs/v6-template-app.log 2>&1; echo "rc=$?"
grep -E "BUILD|FAILED" $O/logs/v6-template-app.log | head
python - $T/app/build/test-results/testDevDebugUnitTest <<'PY'
import sys, glob, xml.etree.ElementTree as ET
cases = fails = 0
for f in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    r = ET.parse(f).getroot()
    for case in r.iter("testcase"):
        cases += 1
        failure = case.find("failure") if case.find("failure") is not None else case.find("error")
        if failure is not None:
            fails += 1
            print("FAIL", case.get("classname").split(".")[-1], case.get("name"))
            for line in (failure.get("message") or "").splitlines()[:12]:
                print("     ", line)
print(f"template app tests={cases} failures={fails}")
PY
rm -rf $O/logs/v6-template-results; cp -r $T/app/build/test-results/testDevDebugUnitTest $O/logs/v6-template-results 2>/dev/null

step "5./6. presets: generate"
PR=$O/presets
mkdir -p $PR
for p in none lean standard all everything; do
  case $p in all) flag="--all";; none) flag="";; *) flag="--preset $p";; esac
  printf '{"schema": 2, "platform": "android", "app_name": "Size %s", "package_name": "com.example.size%s", "features": []}\n' $p $p > $PR/$p.json
  rm -rf $PR/$p
  (cd $W/generator && python -B create_project.py --spec $PR/$p.json $flag --out $PR/$p --no-zip --force > $O/logs/v6-gen-$p.log 2>&1) || echo "GEN FAILED $p"
done

for p in none lean standard all everything; do
  step "5. $p: the stability check after a forced unit-test compile, one worker"
  tasks=""
  for m in designsystem navigation ui; do
    [ -d $PR/$p/core/$m ] && tasks="$tasks :core:$m:compileDebugUnitTestKotlin --rerun :core:$m:checkComposeStability --rerun"
  done
  (cd $PR/$p && OVERHAUL_GRADLE_WORKERS=1 $L gradle $GW --offline $tasks --continue --console=plain) > $O/logs/v6-stability-$p.log 2>&1; echo "rc=$?"
  grep -E "^> Task :core:(designsystem|navigation|ui):(compileDebugKotlin|compileDebugUnitTestKotlin|checkComposeStability)|BUILD|unstable|FAILED" $O/logs/v6-stability-$p.log
done

for p in lean standard everything; do
  step "6. $p: devRelease with ABI splits"
  (cd $PR/$p && $L gradle $GW --offline :app:assembleDevRelease -PabiSplits=true --console=plain) > $O/logs/v6-size-$p.log 2>&1; echo "rc=$?"
  grep -E "BUILD" $O/logs/v6-size-$p.log
  python - $p $PR/$p/app/build/outputs/apk/dev/release <<'PY'
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

echo; echo "V6 DONE $(date +%H:%M:%S)"
