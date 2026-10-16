#!/usr/bin/env bash
# Run 15: final-everything from HEAD, then one emulator session for the baseline profile, the
# large-window scenarios, the tab switch at twice the font size and the RTL clips.
#   ./r15-chain.sh > logs/r15-chain.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04; L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
cd $O
./r14-build.sh everything > logs/r15-build.log 2>&1 || { echo "build failed: logs/r15-build.log"; exit 1; }
echo "== $(date +%H:%M:%S) built"
$L emu ./emu-session.sh profile large font rtl > logs/r15-emu.log 2>&1
echo "== $(date +%H:%M:%S) emulator session exit $?"
echo R15-CHAIN DONE
