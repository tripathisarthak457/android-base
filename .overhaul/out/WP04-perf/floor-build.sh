#!/usr/bin/env bash
# The floor experiment (not a template change): the fixed everything preset with every navigation entry's
# content removed, so a tab switch draws the entry's one background fill, the bar and the edge fade and nothing
# else. What its first frame from idle costs is the least this phone can do for a switch; the real screens
# are judged against it.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04-perf
rm -rf $O/floor && cp -r $O/fixed $O/floor && rm -rf $O/floor/app/build $O/floor/build
f=$O/floor/core/navigation/src/main/kotlin/com/example/wp04/everything/core/navigation/stack/StackHost.kt
grep -c 'EntryFrame(contentKey, hides, screenBars) { destination.content(shown) }' $f
sed -i 's|EntryFrame(contentKey, hides, screenBars) { destination.content(shown) }|EntryFrame(contentKey, hides, screenBars) { }|' $f
grep -q 'EntryFrame(contentKey, hides, screenBars) { }' $f
cd $O/floor
$L gradle ./gradlew :app:assembleDevBenchmarkRelease --console=plain
echo FLOOR-BUILD OK
