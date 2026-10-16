#!/usr/bin/env bash
# One adb lock hold: install the signed catalog release, compile it as Play would after a while
# (speed), then record every checkpoint 1 scenario. Usage: cp1_session.sh <serial> <out> [scenario...]
set -euo pipefail
serial="$1"; out="$2"; shift 2
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP03
pkg=com.example.wp03.cp1.catalog
if adb -s "$serial" shell dumpsys window | grep -q "isKeyguardShowing=true"; then
    echo "cp1: $serial is locked; unlock it and run again" >&2
    exit 3
fi
adb -s "$serial" install -r -t "$O/cp1/catalog/build/outputs/apk/release/catalog-release.apk"
adb -s "$serial" shell cmd package compile -m speed -f "$pkg"
adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
mkdir -p "$out"
python3 "$O/scripts/cp1_record.py" --serial "$serial" --package "$pkg" --out "$out" "$@"
echo CP1 SESSION OK
