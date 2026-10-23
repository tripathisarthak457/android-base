#!/usr/bin/env bash
# ktlint over the scaffold at every module-name length (1 to 30) and every data shape: spotlessApply must change
# nothing. Prints the diff stat; the full diff goes to logs/fit-<tag>.diff.
#   LOCKED_PRIORITY=low bash fit.sh <tag>
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
H=$O/fit
tag=${1:-run}
G="git -c user.name=wp02 -c user.email=wp02@localhost -c core.autocrlf=false"
python -I -B $O/fit_gen.py $OV/wt/sw-WP02/generator $H || { echo "GEN FAILED"; exit 1; }
cd $H
rm -rf .git && $G init -q && $G add -A && $G commit -qm base
$OV/bin/locked gradle ./gradlew --offline spotlessApply --console=plain > $O/logs/fit-$tag.log 2>&1; echo "spotlessApply rc=$?"
$G diff > $O/logs/fit-$tag.diff
echo "changed: $($G diff --stat | tail -1)"
$G diff --stat | head -40
echo "FIT DONE $(date +%H:%M:%S)"
