#!/usr/bin/env bash
# Fix run 1 on the phone, in one hold of the adb lock, most needed first:
#   bin/locked adb bash out/HUD/f1_phone.win.sh
# 1. the overlay's cost on the standard preset (no analytics, so the overlay's own JankStats), 6 rounds;
# 2. hidden adds no frame listener, on both presets (the everything one installed fresh for it);
# 3. the cost again on the everything preset (analytics' JankStats shared), 6 rounds;
# 4. the hidden cold start against wp/WP06's build of the same spec, when that APK exists.
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; P=$OV/perf/HUD; S=${SERIAL:-9a7c74f4}
# Copies taken when they were built, so an acceptance build regenerating the presets cannot pull them away.
A=$O/apks-f1
ST=$A/standard-devRelease.apk
EV=$A/everything-devRelease.apk
BASE=$A/base-devRelease.apk
adb -s "$S" get-state
ID=com.example.hud.standard.dev bash "$O/overhead.win.sh" "$ST" "$P/overhead-standard-f1" 6
echo STANDARD OVERHEAD OK
adb -s "$S" uninstall com.example.hud.everything.dev >/dev/null 2>&1 || true
adb -s "$S" install -r -t -d "$EV"
python -I "$O/device_checks.py" --serial "$S" --listener-only --standard com.example.hud.standard.dev \
  --everything com.example.hud.everything.dev --out "$P/checks-f1"
echo LISTENER OK
ID=com.example.hud.everything.dev bash "$O/overhead.win.sh" "$EV" "$P/overhead-everything-f1" 6
echo EVERYTHING OVERHEAD OK
if [ -f "$BASE" ]; then
  python -I "$O/startup.py" --serial "$S" --hud "$EV" --base "$BASE" --out "$P/startup-f1"
  echo STARTUP OK
fi
echo PHONE OK
