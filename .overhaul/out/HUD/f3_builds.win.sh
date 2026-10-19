#!/usr/bin/env bash
# Fix run 3: both phone presets regenerated from the head and built release-like for the phone (the
# everything one also debug, for the adb path), the standard one's perfhud tests too (its FrameFeedTest
# takes the no-analytics branch), in one hold of the shared Gradle lock; the APKs copied aside:
#   bin/locked gradle bash out/HUD/f3_builds.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD; A=$O/apks-f3
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
gw() {
  local limit=$1; shift
  # One retry on a fresh daemon: lint has died of a Metaspace OOM of the daemon on this machine.
  timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain "$@" ||
    timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain --no-daemon "$@"
}
apk() { ls "$1"/*arm64-v8a*.apk 2>/dev/null | head -1 || ls "$1"/*.apk | head -1; }

cd $W/generator
python create_project.py --spec $O/specs/hud-standard.json --out $O/standard --no-zip --force
python create_project.py --spec $O/specs/hud-everything.json --out $O/everything --no-zip --force
(cd $O/standard && gw 30m :core:perfhud:testDebugUnitTest :app:assembleDevRelease)
echo STANDARD OK
(cd $O/everything && gw 40m :app:assembleDevRelease :app:assembleProdRelease :app:assembleDevDebug)
echo EVERYTHING OK
mkdir -p $A
cp "$(apk $O/standard/app/build/outputs/apk/dev/release)" $A/standard-devRelease.apk
cp "$(apk $O/everything/app/build/outputs/apk/dev/release)" $A/everything-devRelease.apk
cp "$(apk $O/everything/app/build/outputs/apk/prod/release)" $A/everything-prodRelease.apk
cp "$(apk $O/everything/app/build/outputs/apk/dev/debug)" $A/everything-devDebug.apk
git -C $W rev-parse --short HEAD > $A/HEAD
echo PHONE APKS OK
# Read beside the build's own R8 mapping, which the copies do not carry.
mkdir -p $OV/perf/HUD/checks-f3
python -I $O/apk_inspect.py "$(apk $O/everything/app/build/outputs/apk/prod/release)"   "$(apk $O/everything/app/build/outputs/apk/dev/release)" > $OV/perf/HUD/checks-f3/apk-inspection-f3.json
echo INSPECTION OK
# Whether R8 removed FrameJankRecorder.frameListener from prodRelease: the review asked, since R8 can keep a
# volatile field it cannot prove unused. The mapping lists every member R8 kept, under its original name.
M=$O/everything/app/build/outputs/mapping/prodRelease/mapping.txt
python - "$M" > $OV/perf/HUD/checks-f3/prod-frame-listener.txt <<'PY'
import sys
lines = open(sys.argv[1], encoding="utf-8").read().splitlines()
block, inside = [], False
for line in lines:
    if line and not line.startswith((" ", "#")):
        inside = line.split(" -> ")[0].endswith(".core.analytics.jank.FrameJankRecorder")
    if inside and not line.lstrip().startswith("#"):
        block.append(line)
for line in block: print(line)
print("frameListener kept on FrameJankRecorder:", any(" frameListener " in l for l in block))
kept = sorted({l.rsplit(")", 1)[-1].split(" ")[0] for l in lines if "FrameJankRecorder.onFrame$" in l})
print("onFrame source lines kept wherever inlined (69 is the listener call):", kept)
PY
cat $OV/perf/HUD/checks-f3/prod-frame-listener.txt
echo MAPPING OK
