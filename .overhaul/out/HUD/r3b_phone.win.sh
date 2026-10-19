#!/usr/bin/env bash
# Run 3, after the graph-label and PSS-schedule commits: the overlay's cost again (shown vs hidden, 6 rounds), then
# the demo capture, in one hold of the adb lock:
#   bin/locked adb bash out/HUD/r3b_phone.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; P=$OV/perf/HUD; S=${SERIAL:-9a7c74f4}
EV=$O/everything/app/build/outputs/apk
adb -s "$S" get-state
bash "$O/overhead.win.sh" "$EV/dev/release/app-dev-arm64-v8a-release.apk" "$P/overhead-everything-r3b" "${ROUNDS:-6}"
python -I "$O/show.py" --serial "$S" --release "$EV/dev/release/app-dev-arm64-v8a-release.apk" \
  --debug "$EV/dev/debug/app-dev-arm64-v8a-debug.apk" --out "$P/show"
echo PHONE OK
