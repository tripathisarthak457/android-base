#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04
d=$O/inv-everything/app/src/dev/kotlin/com/example/wp04/inv/perf
sed 's/PKG/com.example.wp04.inv/g' $O/inv-patch/CompositionLog.kt > $d/CompositionLog.kt
cd $O/inv-everything
$L gradle ./gradlew :app:assembleDevDebug --console=plain
echo INV-BUILD OK
