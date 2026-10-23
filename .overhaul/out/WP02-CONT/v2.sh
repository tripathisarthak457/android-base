#!/usr/bin/env bash
# Run 2, verification batch 2: the build-logic tests run 1 never reached; H-8 with a parameter that really is
# unstable (run 1's MutableList<String> counts as stable through compose-stability.conf); the generator suite.
#   LOCKED_PRIORITY=low bash v2.sh > logs/v2.log 2>&1
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02-CONT
W=$OV/wt/sw-WP02
L=$OV/bin/locked
GW="bash $OV/out/WP02/gw.win.sh"
step() { echo; echo "== $(date +%H:%M:%S) $*"; }

step "a. build-logic tests on the branch"
(cd $W/templates/android && $L gradle $GW --offline :build-logic:convention:test --console=plain) ; echo "rc=$?"

DSREL=core/designsystem/src/main/kotlin/com/example/standard/core/designsystem
plant() { # <project>
  printf 'package com.example.standard.core.designsystem\n\nimport androidx.compose.runtime.Composable\n\n/** Mutable, so the compiler cannot prove it stable. */\nclass H8Box(var count: Int)\n\n@Composable\nfun H8Planted(box: H8Box) {\n    box.count\n}\n' > $1/$DSREL/H8Planted.kt
}
probe_file() { ls $1/$DSREL/component/button/*.kt | head -1; }
unprobe() { # run 1 left a probe line in the first button file of both projects
  local b; b=$(probe_file $1)
  python - "$b" <<'PY'
import sys
p = sys.argv[1]
text = open(p, encoding="utf-8", newline="").read()
clean = text.replace("\nprivate const val H8_PROBE = 1\n", "")
open(p, "w", encoding="utf-8", newline="").write(clean)
print("probe lines removed:", text.count("H8_PROBE"))
PY
}
h8() { # <project> <tag>
  local P=$1 tag=$2
  unprobe $P
  plant $P
  (cd $P && $L gradle $GW --offline :core:designsystem:compileDebugKotlin --console=plain | grep -E "compileDebugKotlin|BUILD")
  printf '\nprivate const val H8_PROBE = 1\n' >> "$(probe_file $P)"
  (cd $P && $L gradle $GW --offline :core:designsystem:compileDebugKotlin --console=plain | grep -E "compileDebugKotlin|BUILD")
  echo "raw report after the incremental compile: $(wc -l < $P/core/designsystem/build/compose_reports/designsystem-composables.txt) lines, H8Planted: $(grep -c H8Planted $P/core/designsystem/build/compose_reports/designsystem-composables.txt)"
  (cd $P && $L gradle $GW --offline :core:designsystem:checkComposeStability --console=plain) > $O/logs/v2-h8-$tag.log 2>&1; local rc=$?
  echo "check rc=$rc"; grep -nE 'compileDebugKotlin|H8Planted|BUILD' $O/logs/v2-h8-$tag.log | head -8
}
cleanup() { # <project>
  rm -f $1/$DSREL/H8Planted.kt; unprobe $1
}

step "b. H-8 on staging's code: the check after an incremental compile (expect it to PASS on a partial report: the bug)"
h8 $O/std-before before
step "b2. control on staging's code: a full compile, then the check (expect FAIL on H8Planted: the probe is detectable)"
(cd $O/std-before && $L gradle $GW --offline :core:designsystem:compileDebugKotlin --rerun :core:designsystem:checkComposeStability --console=plain) > $O/logs/v2-h8-before-full.log 2>&1; echo "rc=$?"
grep -nE 'H8Planted|BUILD' $O/logs/v2-h8-before-full.log | head -5
cleanup $O/std-before

step "c. H-8 on the branch: the same sequence (expect FAIL on H8Planted)"
h8 $O/std-after after
cleanup $O/std-after
step "c2. the branch, probe removed: the check passes again"
(cd $O/std-after && $L gradle $GW --offline :core:designsystem:checkComposeStability --console=plain) > $O/logs/v2-h8-after-clean.log 2>&1; echo "rc=$?"
grep -nE 'compileDebugKotlin|BUILD' $O/logs/v2-h8-after-clean.log | head -4

step "d. generator suite on the branch"
(cd $W/generator && $OV/bin/memwait python -I -B -m unittest discover -s tests -t .) > $O/logs/v2-gen.log 2>&1; echo "rc=$?"
tail -4 $O/logs/v2-gen.log

echo; echo "V2 DONE $(date +%H:%M:%S)"
