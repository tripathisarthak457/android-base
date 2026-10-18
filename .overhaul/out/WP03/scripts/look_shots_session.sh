#!/usr/bin/env bash
# The acceptance screenshots: the custom-look project's catalog release, every section in light, dark
# and 2x font scale, in one adb lock hold.  $L adb look_shots_session.sh <serial> <out>
set -euo pipefail
serial="$1"; out="$2"
O=D:/projects/tools/.overhaul/out/WP03
pkg=com.example.wp03.look.catalog
adb -s "$serial" install -r -d "$O/look/catalog/build/outputs/apk/release/catalog-release.apk" | tail -1
adb -s "$serial" shell input keyevent KEYCODE_WAKEUP
mkdir -p "$out/raw"
status=0
for pass in "light 1.0 light" "dark 1.0 dark" "light 2.0 2x"; do
    set -- $pass
    python "$O/scripts/catalog_shots.py" --serial "$serial" --package "$pkg" --out "$out/raw" --theme "$1" --font-scale "$2" --suffix "$3" || status=1
done
mv "$out"/raw/*-light.png "$out"/raw/*-dark.png "$out"/raw/*-2x.png "$out"/ 2>/dev/null || true
if [ $status -eq 0 ]; then echo LOOK SHOTS OK; else echo "LOOK SHOTS INCOMPLETE"; fi
exit $status
