#!/usr/bin/env bash
# Fix run 2 on the phone, in one hold of the adb lock, most needed first:
#   bin/locked adb bash out/HUD/f2_phone.win.sh
# 1. the overlay's cost on the standard preset (no analytics, so the overlay's own JankStats), 6 rounds;
# 2. the cost on the everything preset (analytics' JankStats shared), 6 rounds;
# 3. hidden adds no frame listener, on both presets;
# 4. the adb path on the everything devDebug (the receiver now in the debug manifest only), and its
#    refusal on devRelease.
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; P=$OV/perf/HUD; S=${SERIAL:-9a7c74f4}
A=$O/apks-f2
ST_ID=com.example.hud.standard.dev
EV_ID=com.example.hud.everything.dev
adb -s "$S" get-state
ID=$ST_ID bash "$O/overhead.win.sh" "$A/standard-devRelease.apk" "$P/overhead-standard-f2" 6
echo STANDARD OVERHEAD OK
ID=$EV_ID bash "$O/overhead.win.sh" "$A/everything-devRelease.apk" "$P/overhead-everything-f2" 6
echo EVERYTHING OVERHEAD OK
python -I "$O/device_checks.py" --serial "$S" --listener-only --standard $ST_ID --everything $EV_ID \
  --out "$P/checks-f2"
echo LISTENER OK
adb -s "$S" uninstall $EV_ID >/dev/null 2>&1 || true
adb -s "$S" install -r -t -d "$A/everything-devDebug.apk"
python -I "$O/device_checks.py" --serial "$S" --adb-only --standard $ST_ID --everything $EV_ID \
  --release-apk "$A/everything-devRelease.apk" --out "$P/checks-f2"
echo ADB PATH OK
# The refusal check leaves the everything devRelease installed, so the overlay can be tried on the phone.
echo PHONE OK
