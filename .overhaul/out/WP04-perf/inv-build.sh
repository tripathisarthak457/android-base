#!/usr/bin/env bash
# An investigation copy of out/WP04-perf/<from> with CompositionTrace.kt in :app (devBenchmarkRelease), so a
# Perfetto capture shows every composable's own section inside a frame.  ./inv-build.sh <from> <name>
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04-perf
from=$1; name=$2; pkg=com.example.wp04.everything
rm -rf $O/$name && cp -r $O/$from $O/$name && rm -rf $O/$name/app/build $O/$name/build
dst=$O/$name/app/src/main/kotlin/com/example/wp04/everything/perf
mkdir -p $dst
sed "s/PKG/$pkg/g" $O/inv-patch/CompositionTrace.kt > $dst/CompositionTrace.kt
grep -q 'interface LaunchHook' $O/$name/app/src/main/kotlin/com/example/wp04/everything/startup/LaunchHook.kt
cd $O/$name
$L gradle ./gradlew :app:assembleDevBenchmarkRelease --console=plain
echo INV-BUILD OK
