#!/usr/bin/env bash
# Light, dark and 2x font-scale screenshot passes over every catalog section, in one adb lock hold.
#   catalog_shots_session.sh <serial> <out> [catalog package]
set -euo pipefail
serial="$1"; out="$2"; pkg="${3:-com.example.wp03.cp1.catalog}"
O=D:/projects/tools/.overhaul/out/WP03
python "$O/scripts/catalog_shots.py" --serial "$serial" --package "$pkg" --out "$out" --theme light --suffix light
python "$O/scripts/catalog_shots.py" --serial "$serial" --package "$pkg" --out "$out" --theme dark --suffix dark
python "$O/scripts/catalog_shots.py" --serial "$serial" --package "$pkg" --out "$out" --font-scale 2.0 --suffix 2x
echo SHOTS OK
