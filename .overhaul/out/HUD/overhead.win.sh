#!/usr/bin/env bash
# The overlay's cost on the phone, shown vs hidden, in one hold of the adb lock:
#   bin/locked adb bash out/HUD/overhead.win.sh <devRelease apk> <out dir> [rounds]
# Installs the APK fresh, compiles it with its own profile as Play would (as perf.sh does), then alternates
# hidden and shown launches through overhead.py.
set -euo pipefail
OV=D:/projects/tools/.overhaul; S=${SERIAL:-9a7c74f4}; ID=com.example.hud.everything.dev
APK=$1; OUT=$2; ROUNDS=${3:-4}
adb -s "$S" get-state
adb -s "$S" uninstall "$ID" >/dev/null 2>&1 || true
adb -s "$S" install -r -t -d "$APK"
adb -s "$S" shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE \
  -n "$ID/androidx.profileinstaller.ProfileInstallReceiver" >/dev/null
adb -s "$S" shell am force-stop "$ID"
adb -s "$S" shell cmd package compile -f -m speed-profile "$ID"
python -I "$OV/out/HUD/overhead.py" --serial "$S" --package "$ID" --rounds "$ROUNDS" --out "$OUT"
echo OVERHEAD OK
