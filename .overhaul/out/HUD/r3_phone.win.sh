#!/usr/bin/env bash
# Run 3's phone steps on the final head, in one hold of the adb lock:
#   bin/locked adb bash out/HUD/r3_phone.win.sh
# The overlay's cost again (shown vs hidden, 6 rounds), then the demo capture (show.py). Changes no device setting.
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; P=$OV/perf/HUD; S=${SERIAL:-9a7c74f4}
EV=$O/everything/app/build/outputs/apk
apk() { ls "$1"/*arm64-v8a*.apk 2>/dev/null | head -1 || ls "$1"/*.apk | head -1; }
adb -s "$S" get-state
bash "$O/overhead.win.sh" "$(apk "$EV/dev/release")" "$P/overhead-everything-r3" "${ROUNDS:-6}"
python -I "$O/show.py" --serial "$S" --release "$(apk "$EV/dev/release")" --debug "$(apk "$EV/dev/debug")" \
  --out "$P/show"
echo PHONE OK
