#!/usr/bin/env bash
# Run 18: the build-logic tests after the code rules learned to cite the vendored skill pages.
set -euo pipefail
OV=D:/projects/tools/.overhaul
O=$OV/out/WP02
L=$OV/bin/locked
cd $OV/wt/WP02/templates/android
$L gradle bash $O/gw.win.sh :build-logic:convention:test --console=plain
echo R18 RULES OK
