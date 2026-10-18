#!/usr/bin/env bash
# Run 16's phone work in one adb lock hold, on the custom-look project's catalog release that accept.sh builds:
# the catalog screenshots (light, dark, 2x), the sheet and toast recordings, and the K12 probe.
#   $L adb bash $O/scripts/with_phone.sh <serial> bash $O/scripts/r16_phone.sh <serial>
set -uo pipefail
serial=$1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
pkg=com.example.wp03.look.catalog
apk=$O/look/catalog/build/outputs/apk/release/catalog-release.apk
status=0
bash "$O/scripts/look_shots_session.sh" "$serial" "$P/catalog" || status=1
CP1_PACKAGE=$pkg CP1_APK=$apk bash "$O/scripts/cp1_session.sh" "$serial" "$P/cp1-phone-r16" sheet toast || status=1
python "$O/scripts/sheet_probe.py" --serial "$serial" --package "$pkg" --out "$P/k12" || status=1
echo "R16 PHONE EXIT $status"
exit $status
