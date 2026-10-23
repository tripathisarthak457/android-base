#!/usr/bin/env bash
# H-8 experiment: does an incremental compile of :core:designsystem write a partial Compose report?
set -euo pipefail
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
P=$O/h8/lean
cd $W/generator
python -B create_project.py --spec tests/fixtures/v2/lean.json --out $P --no-zip --force
cd $P
R=core/designsystem/build/compose_reports
$L gradle bash $OV/out/WP02/gw.win.sh --offline :core:designsystem:compileDebugKotlin --console=plain
ls -la $R; cp $R/designsystem-composables.txt $O/h8/full.txt
f=$(ls core/designsystem/src/main/kotlin/com/example/*/core/designsystem/component/button/*.kt 2>/dev/null | head -1 || true)
[ -z "$f" ] && f=$(find core/designsystem/src/main/kotlin -name '*.kt' | grep -i button | head -1)
echo "touching $f"
printf '\nprivate const val H8_PROBE = 1\n' >> "$f"
$L gradle bash $OV/out/WP02/gw.win.sh --offline :core:designsystem:compileDebugKotlin --console=plain
ls -la $R; cp $R/designsystem-composables.txt $O/h8/incr.txt
wc -l $O/h8/full.txt $O/h8/incr.txt
grep -c '^restartable\|^fun\| fun ' $O/h8/full.txt $O/h8/incr.txt || true
echo H8 DONE
