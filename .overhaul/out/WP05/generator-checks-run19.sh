#!/usr/bin/env bash
# Run 19: the generator's own checks on wp/WP05 after merging WIN's follow-up (4dd3aca).
set -u
OV=D:/projects/tools/.overhaul
L=$OV/logs/lens
cd D:/projects/tools/.overhaul/wt/WP05/generator || exit 1
"$OV/bin/memwait" python -I -m unittest discover -s tests -t . > "$L/run19-generator-unit.log" 2>&1
echo "exit=$?" >> "$L/run19-generator-unit.log"
ls -d "$TEMP"/consistency-* 2>/dev/null > "$L/run19-temp-before.txt"
"$OV/bin/memwait" python -m tests.consistency --quick --jobs 2 > "$L/run19-consistency-plain.log" 2>&1
echo "exit=$?" >> "$L/run19-consistency-plain.log"
ls -d "$TEMP"/consistency-* 2>/dev/null > "$L/run19-temp-after.txt"
"$OV/bin/memwait" python -m genkit.tokens --check > "$L/run19-tokens-check.log" 2>&1
echo "exit=$?" >> "$L/run19-tokens-check.log"
"$OV/bin/memwait" python -m genkit.core.catalogue --check > "$L/run19-catalogue-check.log" 2>&1
echo "exit=$?" >> "$L/run19-catalogue-check.log"
echo DONE > "$L/run19-generator-checks.done"
