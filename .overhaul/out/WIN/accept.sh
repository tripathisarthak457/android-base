#!/usr/bin/env bash
# Package WIN's acceptance, from Git Bash: the generator's whole suite on this Windows machine, no CR
# added by the branch, and the "standard" (plus an everything-and-brand) project equal to the Linux
# bytes derived from the integration head (compare_hosts.py says how).
set -euo pipefail
OV=D:/projects/tools/.overhaul
WT=$OV/wt/WIN
OUT=$OV/out/WIN
# Once WIN has merged, the merge-base is HEAD itself and both comparisons would be empty: pass
# BASE=<the integration head before the merge> then.
BASE=${BASE:-$(git -C "$WT" merge-base HEAD overhaul/multiplatform-v2)}
test "$BASE" != "$(git -C "$WT" rev-parse HEAD)" || { echo "BASE is HEAD: nothing to compare" >&2; exit 1; }

cd "$WT/generator"
"$OV/bin/memwait" python -m unittest discover -s tests -t .
python -m genkit.tokens --check
python -m genkit.core.catalogue --check

added_cr=$(git -C "$WT" diff "$BASE"..HEAD | python -I -c "
import sys
lines = sys.stdin.buffer.read().split(b'\n')
print(sum(1 for line in lines if line.startswith(b'+') and not line.startswith(b'+++') and b'\r' in line))
")
echo "added lines with CR: $added_cr"
test "$added_cr" = 0

rm -rf "$OUT/base" "$OUT/compare"
mkdir -p "$OUT/base"
git -C "$WT" archive "$BASE" generator templates tokens | tar -x -C "$OUT/base"
"$OV/bin/memwait" python -I "$OUT/compare_hosts.py" "$OUT/base/generator" "$WT/generator" "$OUT/compare" | tee "$OUT/compare.log"
grep -q "^COMPARE OK$" "$OUT/compare.log"
rm -rf "$OUT/base" "$OUT/compare"

test -z "$(git -C "$WT" status --porcelain)"
echo ACCEPT OK
