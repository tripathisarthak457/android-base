#!/usr/bin/env bash
set -uo pipefail
cd D:/projects/tools/android-base/web/frontend
npm ci --no-audit --no-fund || exit 1
echo "== typecheck"
npx tsc --noEmit; tsc=$?
echo "tsc exit $tsc"
echo "== tests"
node --test "features/**/*.test.ts"; t=$?
echo "tests exit $t"
exit $(( tsc || t ))
