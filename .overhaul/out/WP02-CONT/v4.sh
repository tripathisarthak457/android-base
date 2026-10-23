#!/usr/bin/env bash
# Run 2, batch 4: AdaptiveAuditTest on a generated project with the reference feature and two scaffolded modules
# (every screen at six windows and two font scales), then with a planted screen that breaks each rule once: the audit
# must fail and name every planted node.
#   bash v4.sh > logs/v4.log 2>&1
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
P=$O/audit-net
step() { echo; echo "== $(date +%H:%M:%S) $*"; }

step "generate at $(git -C $W rev-parse --short HEAD) with $(git -C $W status --porcelain | wc -l) changed paths"
rm -rf $P
(cd $W/generator && python -B create_project.py --spec $O/specs/audit.json --out $P --no-zip --force > $O/logs/v4-gen.log 2>&1) || { echo "GEN FAILED"; exit 1; }

step "the audit on the generated screens (expect PASS)"
(cd $P && $L gradle $GW --offline :app:verifyCodeRules :core:database:compileDebugKotlin :data:order:testDebugUnitTest :feature:order:testDebugUnitTest :app:testDevDebugUnitTest --tests '*AdaptiveAuditTest*' --console=plain) > $O/logs/v4-audit.log 2>&1; echo "rc=$?"
grep -E "BUILD|tests completed|FAILED" $O/logs/v4-audit.log | head
python - $P/app/build/test-results/testDevDebugUnitTest <<'PY'
import sys, glob, xml.etree.ElementTree as ET
cases = fails = skips = 0
for f in glob.glob(sys.argv[1] + "/TEST-*Adaptive*.xml"):
    r = ET.parse(f).getroot()
    cases += int(r.get("tests")); fails += int(r.get("failures")) + int(r.get("errors")); skips += int(r.get("skipped"))
print(f"audit cases={cases} failures={fails} skipped={skips}")
PY

step "a planted screen that breaks every rule (expect FAIL naming each planted node)"
cp $O/audit/AuditPlanted.kt $P/app/src/main/kotlin/io/example/audit/AuditPlanted.kt
(cd $P && $L gradle $GW --offline :app:testDevDebugUnitTest --tests '*AdaptiveAuditTest*' --console=plain) > $O/logs/v4-planted.log 2>&1; echo "rc=$?"
rm -f $P/app/src/main/kotlin/io/example/audit/AuditPlanted.kt
python - $P/app/build/test-results/testDevDebugUnitTest <<'PY'
import sys, glob, re, xml.etree.ElementTree as ET
seen = {}
for f in glob.glob(sys.argv[1] + "/TEST-*Adaptive*.xml"):
    for case in ET.parse(f).getroot().iter("testcase"):
        failure = case.find("failure")
        if failure is None:
            print("PASSED (should have failed):", case.get("name")); continue
        for line in (failure.get("message") or "").splitlines()[1:]:
            line = line.strip()
            if line.startswith('"'):
                kind = re.sub(r'^"[^"]*"( and "[^"]*")?:? ?', "", line)
                kind = re.sub(r"[0-9.]+", "#", kind)
                seen.setdefault(kind, set()).add(case.get("name").split(" at ")[1].split("]")[0] if " at " in case.get("name") else case.get("name"))
for kind, where in sorted(seen.items()):
    print(f"{kind}  <- {sorted(where)[:4]}")
PY
grep -c "<testcase" $P/app/build/test-results/testDevDebugUnitTest/TEST-*Adaptive*.xml

echo; echo "V4 DONE $(date +%H:%M:%S)"
