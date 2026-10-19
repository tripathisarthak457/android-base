#!/usr/bin/env bash
# The hidden cold start of this branch's everything devRelease against wp/WP06's, once f1_base.win.sh has built the
# latter, in one hold of the adb lock:
#   bin/locked adb bash out/HUD/f1_startup.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; A=$O/apks-f1
adb -s "${SERIAL:-9a7c74f4}" get-state
python -I "$O/startup.py" --serial "${SERIAL:-9a7c74f4}" --hud "$A/everything-devRelease.apk" \
  --base "$A/base-devRelease.apk" --out "$OV/perf/HUD/startup-f1"
echo STARTUP OK
