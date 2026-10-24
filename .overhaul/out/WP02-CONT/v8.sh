#!/usr/bin/env bash
# Run 4, batch 8, on the branch after merging staging 4760b407, with the audit's two false positives fixed (uncommitted):
#  1. the audit project regenerated from the worktree (the audit test is a template file);
#  2. AdaptiveAuditTest on its generated screens (expect PASS: v6 failed 58 of 60 on "the text is cut off" for lines
#     shorter than the width offered, and on the sample skeleton's rows under clearAndSetSemantics);
#  3. the planted screen (expect FAIL naming each planted kind, and no real screen failing);
#  4. the audit on the template's own screens, every feature on, with the app's code rules (expect PASS);
#  5. v7: the scaffolded list ViewModel's two "rows written after ..." tests red under a mutated reducer, green restored;
#  6. the stability check against task order on every preset, fixed: the unit-test compile is forced only in modules
#     that have one (v6 asked for :core:ui:compileDebugUnitTestKotlin, which no preset has); the report each check
#     read is counted, so a pass on a partial report shows.
#   bash v8.sh > logs/v8.log 2>&1
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }
step "branch $(git -C $W rev-parse --short HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"

audit_summary() { # <results dir> <expect planted: 0|1>
  python -I - "$1" "$2" <<'PY'
import sys, glob, re, xml.etree.ElementTree as ET
folder, planted_expected = sys.argv[1], sys.argv[2] == "1"
cases = real_fail = planted_fail = planted_pass = 0
screens, kinds = set(), {}
for f in glob.glob(folder + "/TEST-*AdaptiveAuditTest.xml"):
    for case in ET.parse(f).getroot().iter("testcase"):
        cases += 1
        name = case.get("name")
        failure = case.find("failure")
        if failure is None:
            failure = case.find("error")
        if "[" in name:
            screens.add(name.split("[")[1].split(" at ")[0])
        if "AuditPlanted" in name:
            if failure is None:
                planted_pass += 1
                print("PLANTED PASSED (should fail):", name)
                continue
            planted_fail += 1
            for line in (failure.get("message") or "").splitlines()[1:]:
                line = line.strip()
                if line.startswith('"'):
                    kind = re.sub(r"[0-9.]+", "#", line)
                    kinds.setdefault(kind, set()).add(name.split(" at ")[1].split("]")[0])
            continue
        if failure is not None:
            real_fail += 1
            print("REAL SCREEN FAILED:", name)
            for line in (failure.get("message") or failure.text or "").splitlines()[:8]:
                print("     ", line)
print(f"audit cases={cases} real-screen failures={real_fail} planted failed={planted_fail} planted passed={planted_pass}")
print("screens:", sorted(screens))
for kind, where in sorted(kinds.items()):
    print(f"  {kind}  <- {len(where)} windows")
PY
}

P=$O/audit
step "1. the audit project: regenerate"
rm -rf $P
(cd $W/generator && python -B create_project.py --spec $O/specs/audit.json --out $P --no-zip --force > $O/logs/v8-gen-audit.log 2>&1) || echo "GEN FAILED audit"
grep -c "isCut" $P/app/src/test/kotlin/io/example/audit/adaptive/AdaptiveAuditTest.kt

step "2. the audit on the generated screens (expect PASS)"
(cd $P && $L gradle $GW --offline :app:verifyCodeRules :app:testDevDebugUnitTest --console=plain) > $O/logs/v8-audit.log 2>&1; echo "rc=$?"
grep -E "^BUILD|FAILED$" $O/logs/v8-audit.log | head
audit_summary $P/app/build/test-results/testDevDebugUnitTest 0
rm -rf $O/logs/v8-audit-results; cp -r $P/app/build/test-results/testDevDebugUnitTest $O/logs/v8-audit-results 2>/dev/null

step "3. the planted screen (expect FAIL naming each planted kind; every real screen passes)"
cp $O/audit-planted/AuditPlanted.kt $P/app/src/main/kotlin/io/example/audit/AuditPlanted.kt
(cd $P && $L gradle $GW --offline :app:testDevDebugUnitTest --tests '*AdaptiveAuditTest*' --console=plain) > $O/logs/v8-planted.log 2>&1; echo "rc=$?"
rm -f $P/app/src/main/kotlin/io/example/audit/AuditPlanted.kt
audit_summary $P/app/build/test-results/testDevDebugUnitTest 1
rm -rf $O/logs/v8-planted-results; cp -r $P/app/build/test-results/testDevDebugUnitTest $O/logs/v8-planted-results 2>/dev/null

step "4. the audit on the template's own screens, every feature on (expect PASS)"
T=$W/templates/android
(cd $T && $L gradle $GW --offline :app:verifyCodeRules :app:testDevDebugUnitTest --continue --console=plain) > $O/logs/v8-template-app.log 2>&1; echo "rc=$?"
grep -E "^BUILD|FAILED$" $O/logs/v8-template-app.log | head
audit_summary $T/app/build/test-results/testDevDebugUnitTest 0
python -I - $T/app/build/test-results/testDevDebugUnitTest <<'PY'
import sys, glob, xml.etree.ElementTree as ET
cases = fails = 0
for f in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    for case in ET.parse(f).getroot().iter("testcase"):
        cases += 1
        if case.find("failure") is not None or case.find("error") is not None:
            fails += 1
print(f"template app tests={cases} failures={fails}")
PY
rm -rf $O/logs/v8-template-results; cp -r $T/app/build/test-results/testDevDebugUnitTest $O/logs/v8-template-results 2>/dev/null

step "5. v7: the list ViewModel's tests against a mutated reducer, then restored"
bash $O/v7.sh > $O/logs/v7.log 2>&1; echo "rc=$?"
cat $O/logs/v7.log

for p in none lean standard all everything; do
  step "6. $p: the stability check after a forced unit-test compile, one worker"
  PR=$O/presets/$p
  tasks=""
  for m in designsystem navigation ui; do
    [ -d $PR/core/$m ] || continue
    [ -d $PR/core/$m/src/test ] && tasks="$tasks :core:$m:compileDebugUnitTestKotlin --rerun"
    tasks="$tasks :core:$m:checkComposeStability --rerun"
  done
  (cd $PR && OVERHAUL_GRADLE_WORKERS=1 $L gradle $GW --offline $tasks --continue --console=plain) > $O/logs/v8-stability-$p.log 2>&1; echo "rc=$?"
  grep -E "^> Task :core:(designsystem|navigation|ui):(compileDebugKotlin|compileDebugUnitTestKotlin|checkComposeStability)|^BUILD|unstable|FAILED" $O/logs/v8-stability-$p.log
  for m in designsystem navigation ui; do
    [ -d $PR/core/$m/build/compose_stability ] && echo "  $m: report read $(cat $PR/core/$m/build/compose_stability/*-composables.txt | wc -l) lines; the shared file now $(cat $PR/core/$m/build/compose_reports/*-composables.txt 2>/dev/null | wc -l) lines"
  done
done

echo; echo "V8 DONE $(date +%H:%M:%S)"
