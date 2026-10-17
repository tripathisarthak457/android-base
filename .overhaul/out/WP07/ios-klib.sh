#!/usr/bin/env bash
# The iOS klib compile of the template, as the acceptance runs it; result in logs/ios-klib.log.
# Windows, Git Bash: locked sets KONAN_DATA_DIR (D:\.caches\konan), so the toolchain never lands in ~/.konan.
set -o pipefail
OV=D:/projects/tools/.overhaul
cd $OV/wt/WP07/templates/multiplatform
{ date; $OV/bin/locked gradle ./gradlew :shared:compileKotlinIosSimulatorArm64 --console=plain; echo "ios klib exit=$?"; date; } > $OV/out/WP07/logs/ios-klib.log 2>&1
