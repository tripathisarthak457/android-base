#!/usr/bin/env bash
# Puts back the pointer-location overlay that run 6's back-gesture diagnosis (diag/edge_diag2.sh) left on in the
# periflow_api35 AVD, and reports the other input overlays.  $L emu bash with_emulator.sh bash restore_emulator_settings.sh
set -euo pipefail
A="adb -s emulator-5554"
echo "before: pointer_location=$($A shell settings get system pointer_location | tr -d '\r') show_touches=$($A shell settings get system show_touches | tr -d '\r')"
$A shell settings put system pointer_location 0
echo "after: pointer_location=$($A shell settings get system pointer_location | tr -d '\r') show_touches=$($A shell settings get system show_touches | tr -d '\r')"
