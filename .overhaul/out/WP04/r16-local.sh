#!/usr/bin/env bash
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04
cd $O
until grep -q 'R14-BUILD OK' logs/r16-build2.log; do
  if ! pgrep -f 'r14-build.sh everything standard lean' >/dev/null; then echo "build ended without R14-BUILD OK"; exit 1; fi
  sleep 10
done
./accept-local.sh > logs/r16-accept-local.log 2>&1
echo "== $(date +%H:%M:%S) accept-local exit $?"
