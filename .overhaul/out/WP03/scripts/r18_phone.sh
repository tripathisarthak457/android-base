#!/usr/bin/env bash
# Run 18's phone check in one adb lock hold, on the look project's catalog release that accept.sh builds: the pages
# its fixes touch (identifier tiles, the field glyph, button labels) in light and at 2x, and the first-frame check
# twice.  $L adb bash $O/scripts/with_phone.sh <serial> bash $O/scripts/r18_phone.sh <serial>
set -uo pipefail
serial=$1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
pkg=com.example.wp03.look.catalog
apk=$O/look/catalog/build/outputs/apk/release/catalog-release.apk
out=$P/catalog-r18
status=0
adb -s "$serial" install -r -d "$apk" | tail -1
mkdir -p "$out/raw"
for pass in "light 1.0 light" "light 2.0 2x"; do
    set -- $pass
    python "$O/scripts/catalog_shots.py" --serial "$serial" --package "$pkg" --out "$out/raw" --theme "$1" \
        --font-scale "$2" --suffix "$3" Foundations Text Inputs Sheets || status=1
done
mv "$out"/raw/*-light.png "$out"/raw/*-2x.png "$out"/ 2>/dev/null || true
python "$O/scripts/font_swap.py" --serial "$serial" --package "$pkg" --out "$P/font-swap-r18" || status=1
python "$O/scripts/font_swap.py" --serial "$serial" --package "$pkg" --out "$P/font-swap-r18b" || status=1
echo "R18 PHONE EXIT $status"
exit $status
