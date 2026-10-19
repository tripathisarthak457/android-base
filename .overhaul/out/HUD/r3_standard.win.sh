#!/usr/bin/env bash
# The standard preset (no analytics: the overlay's own JankStats) regenerated from the final head and checked:
#   bin/locked gradle bash out/HUD/r3_standard.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD
cd $W/generator
python create_project.py --spec $O/specs/hud-standard.json --out $O/standard --no-zip --force
cd $O/standard
timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain check assembleDevDebug
echo STANDARD OK
