#!/usr/bin/env bash
# Run 19: the device-free acceptance lines at HEAD, then the three presets built for the phone.
#   ./r19-local.sh > logs/r19-local.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04
cd $O
echo "== $(date +%H:%M:%S) accept-local"
./accept-local.sh > logs/r19-accept-local.log 2>&1; echo "== $(date +%H:%M:%S) accept-local exit $? $(tail -1 logs/r19-accept-local.log)"
./r14-build.sh everything standard lean > logs/r19-build.log 2>&1; echo "== $(date +%H:%M:%S) build exit $? $(tail -1 logs/r19-build.log)"
echo R19-LOCAL DONE
