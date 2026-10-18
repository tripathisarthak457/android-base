#!/usr/bin/env bash
# Run 19's phone check in one adb lock hold, on the look project's catalog release that accept.sh builds: the
# pages its fixes touch (the tile grids, the fields' end controls, the phone field) in light and at 2x, then the
# fling against iOS's curve and the edge (REQUIREMENTS 41).
#   $L adb bash $O/scripts/with_phone.sh <serial> bash $O/scripts/r19_phone.sh <serial>
set -uo pipefail
serial=$1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
pkg=com.example.wp03.look.catalog
apk=$O/look/catalog/build/outputs/apk/release/catalog-release.apk
out=$P/catalog-r19
status=0
adb -s "$serial" install -r -d "$apk" | tail -1
# Judged as a user would run it once ART has compiled it, not interpreted on first launch.
adb -s "$serial" shell cmd package compile -m speed -f "$pkg" | tail -1
mkdir -p "$out/raw"
for pass in "light 1.0 light" "light 2.0 2x"; do
    set -- $pass
    python "$O/scripts/catalog_shots.py" --serial "$serial" --package "$pkg" --out "$out/raw" --theme "$1" \
        --font-scale "$2" --suffix "$3" Foundations Text Inputs Fields Motion || status=1
done
mv "$out"/raw/*-light.png "$out"/raw/*-2x.png "$out"/ 2>/dev/null || true
python "$O/scripts/fling_curve.py" --serial "$serial" --package "$pkg" --out "$P/fling" || status=1
echo "R19 PHONE EXIT $status"
exit $status
