#!/usr/bin/env bash
# Run 17's phone work in one adb lock hold, on the custom-look project's catalog release that accept.sh builds:
# the catalog screenshots (light, dark, 2x), the sheet and toast recordings, the K12 probe and the font-swap check.
#   $L adb bash $O/scripts/with_phone.sh <serial> bash $O/scripts/r17_phone.sh <serial>
set -uo pipefail
serial=$1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
pkg=com.example.wp03.look.catalog
apk=$O/look/catalog/build/outputs/apk/release/catalog-release.apk
# The shots of run 16 stay beside the new ones for comparison.
if [ -d "$P/catalog" ] && [ ! -e "$P/catalog-r16" ]; then mv "$P/catalog" "$P/catalog-r16"; fi
status=0
bash "$O/scripts/look_shots_session.sh" "$serial" "$P/catalog" || status=1
CP1_PACKAGE=$pkg CP1_APK=$apk bash "$O/scripts/cp1_session.sh" "$serial" "$P/cp1-phone-r17" sheet toast || status=1
python "$O/scripts/sheet_probe.py" --serial "$serial" --package "$pkg" --out "$P/k12" || status=1
python "$O/scripts/font_swap.py" --serial "$serial" --package "$pkg" --out "$P/font-swap" || status=1
echo "R17 PHONE EXIT $status"
exit $status
