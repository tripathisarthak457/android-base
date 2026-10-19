#!/usr/bin/env bash
# The pill as PillText draws it, on the installed everything devRelease: launched shown, scrolled, captured.
#   bin/locked adb bash out/HUD/pill_shot.sh
set -euo pipefail
S=${SERIAL:-9a7c74f4}; ID=com.example.hud.everything.dev; OUT=D:/projects/tools/.overhaul/perf/HUD/show-f2
adb -s $S shell am force-stop $ID
adb -s $S shell am start -W -n $ID/com.example.hud.everything.MainActivity --es perf.hud on >/dev/null
sleep 4
for i in 1 2 3 4; do adb -s $S shell input swipe 540 1800 540 720 140; sleep 0.8; done
adb -s $S exec-out screencap -p > $OUT/1-pill-after-scroll.png
sleep 3
adb -s $S exec-out screencap -p > $OUT/2-pill-idle.png
echo SHOT OK
