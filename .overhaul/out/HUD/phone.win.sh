#!/usr/bin/env bash
# Every phone step of package HUD in one hold of the adb lock:
#   bin/locked adb bash out/HUD/phone.win.sh [inspect|overhead|checks|prod|all]
# Changes no device setting. The test apps are uninstalled before installing, since builds signed on
# another machine cannot be updated in place.
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; P=$OV/perf/HUD; S=${SERIAL:-9a7c74f4}
step=${1:-all}
EV=$O/everything/app/build/outputs/apk
ST=$O/standard/app/build/outputs/apk
BASE=$O/base/app/build/outputs/apk
EV_ID=com.example.hud.everything.dev
ST_ID=com.example.hud.standard.dev

apk() { ls "$1"/*arm64-v8a*.apk 2>/dev/null | head -1 || ls "$1"/*.apk | head -1; }
install() {
  adb -s "$S" uninstall "$2" >/dev/null 2>&1 || true
  adb -s "$S" install -r -t -d "$1"
}

adb -s "$S" get-state
mkdir -p "$P/checks"
if [ "$step" = inspect ] || [ "$step" = all ]; then
  python -I "$O/apk_inspect.py" "$(apk "$EV/prod/release")" "$(apk "$BASE/prod/release")" "$(apk "$EV/dev/release")" \
    "$(apk "$EV/dev/debug")" | tee "$P/checks/apk-inspection.json"
fi
if [ "$step" = overhead ] || [ "$step" = all ]; then
  bash "$O/overhead.win.sh" "$(apk "$EV/dev/release")" "$P/overhead-everything" "${ROUNDS:-6}"
fi
if [ "$step" = checks ] || [ "$step" = all ]; then
  install "$(apk "$ST/dev/release")" "$ST_ID"
  install "$(apk "$EV/dev/release")" "$EV_ID"
  python -I "$O/device_checks.py" --serial "$S" --standard "$ST_ID" --everything "$EV_ID" --listener-only \
    --out "$P/checks"
  install "$(apk "$EV/dev/debug")" "$EV_ID"
  python -I "$O/device_checks.py" --serial "$S" --standard "$ST_ID" --everything "$EV_ID" --adb-only \
    --release-apk "$(apk "$EV/dev/release")" --out "$P/checks"
fi
if [ "$step" = prod ] || [ "$step" = all ]; then
  python -I "$O/device_checks.py" --serial "$S" --standard "$ST_ID" --everything "$EV_ID" \
    --prod-before "$(apk "$BASE/prod/release")" --prod-after "$(apk "$EV/prod/release")" --out "$P/checks"
fi
echo PHONE OK
