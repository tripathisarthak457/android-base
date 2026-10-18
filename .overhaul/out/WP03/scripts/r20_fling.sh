#!/usr/bin/env bash
# REQUIREMENTS 41's phone proof on the build that carries the rubber band's fixes (daafdbd, 91cb638): the look
# catalog release built from 91cb638, speed-compiled, flung against iOS's curve, plus the throw into the top.
#   $L adb bash $O/scripts/with_phone.sh <serial> bash $O/scripts/r20_fling.sh <serial>
set -uo pipefail
serial=$1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
pkg=com.example.wp03.look.catalog
apk=$O/apk/look-catalog-91cb638.apk
adb -s "$serial" install -r -d "$apk" | tail -1
adb -s "$serial" shell cmd package compile -m speed -f "$pkg" | tail -1
python "$O/scripts/fling_curve.py" --serial "$serial" --package "$pkg" --out "$P/fling-91cb638"
status=$?
echo "R20 FLING EXIT $status"
exit $status
