#!/usr/bin/env bash
# Dark and 2x font-scale screenshot passes over every catalog section, in one adb lock hold.
set -euo pipefail
serial="$1"; out="$2"
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP03
python3 "$O/scripts/catalog_shots.py" --serial "$serial" --package com.example.wp03.cp1.catalog --out "$out" --theme dark --suffix dark
python3 "$O/scripts/catalog_shots.py" --serial "$serial" --package com.example.wp03.cp1.catalog --out "$out" --font-scale 2.0 --suffix 2x
echo SHOTS OK
