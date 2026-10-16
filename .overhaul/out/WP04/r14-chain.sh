#!/usr/bin/env bash
# Run 14: final-everything from HEAD, then one emulator session for the profile, the large-window
# scenarios, the y-follow recording and RTL.
#   ./r14-chain.sh > logs/r14-chain.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04; L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
cd $O
./r14-build.sh everything > logs/r14-build.log 2>&1 || { echo "build failed: logs/r14-build.log"; exit 1; }
echo "== $(date +%H:%M:%S) built"
$L emu ./emu-session.sh profile large yfollow rtl > logs/r14-emu.log 2>&1
echo "== $(date +%H:%M:%S) emulator session exit $?"
echo R14-CHAIN DONE
