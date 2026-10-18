#!/usr/bin/env bash
# The scroll tests against the fixed rubber band and against earlier or broken versions of it, so every regression test
# is seen failing, for its own reason, without the change it guards. The fixed file is put back however this ends.
#   bin/locked gradle bash qa/bug-scroll-direction/tools/mutants.sh [variant...]
# Variants: fix2 (the working tree), on-daafdbd (the first fix), on-c55a8fc (the build the user ran),
# no-leftover-threshold (the working tree without the 50 dp/s minimum on what a fling leaves), relax-sign-reverted
# (the working tree with the first fix's `pull += relaxed` turned back into `-=`, which isolates the catch cases).
set -uo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP03; Q=$OV/qa/bug-scroll-direction
F=templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/scroll/AppOverscroll.kt
SAVED=$Q/logs/AppOverscroll.fix2.kt.txt
R=$W/templates/android/core/designsystem/build/test-results/testDebugUnitTest
TESTS=(--tests '*ScrollDirectionTest' --tests '*AppCarouselTest' --tests '*AppOverscrollTest' --tests '*AppFlingTest'
  --tests '*AppFlingCurveTest' --tests '*PullToRefreshTest' --tests '*ScrollToTopTest')
cp "$W/$F" "$SAVED"
trap 'cp "$SAVED" "$W/$F"' EXIT

run() { # variant
  (cd "$W/templates/android" && ./gradlew :core:designsystem:testDebugUnitTest --console=plain "${TESTS[@]}") \
    > "$Q/logs/$1.log" 2>&1
  echo "EXIT $?" >> "$Q/logs/$1.log"
  mkdir -p "$Q/logs/$1-results"
  cp "$R"/TEST-*.xml "$Q/logs/$1-results/"
  python -I "$Q/tools/junit_summary.py" "$Q/logs/$1-results/*.xml" > "$Q/logs/$1-results/summary.txt"
  echo "== $1: $(grep -c '^ok' "$Q/logs/$1-results/summary.txt") passed, $(grep -c '^FAIL' "$Q/logs/$1-results/summary.txt") failed"
}

replace() { # old new: the fixed file with one line changed
  python -I -c "
import sys
s = open(sys.argv[1], encoding='utf-8').read()
assert s.count(sys.argv[3]) == 1, sys.argv[3]
open(sys.argv[2], 'w', encoding='utf-8', newline='').write(s.replace(sys.argv[3], sys.argv[4]))
" "$SAVED" "$W/$F" "$1" "$2"
}

variant() {
  case $1 in
    fix2) ;;
    on-daafdbd) MSYS_NO_PATHCONV=1 git -C "$W" show "daafdbd:$F" > "$W/$F" ;;
    on-c55a8fc) MSYS_NO_PATHCONV=1 git -C "$W" show "c55a8fc:$F" > "$W/$F" ;;
    no-leftover-threshold) replace 'val carried = Offset(left.x.orNoneBelow(minimum), left.y.orNoneBelow(minimum))' \
      'val carried = Offset(left.x, left.y)' ;;
    relax-sign-reverted) replace 'pull += relaxed' 'pull -= relaxed' ;;
    *) echo "unknown variant $1"; return 1 ;;
  esac
  run "$1"
  cp "$SAVED" "$W/$F"
}

for name in "${@:-fix2 on-daafdbd on-c55a8fc no-leftover-threshold relax-sign-reverted}"; do
  for v in $name; do variant "$v"; done
done
cmp "$SAVED" "$W/$F" && echo "RESTORED the fixed AppOverscroll.kt"
echo "MUTANTS DONE"
