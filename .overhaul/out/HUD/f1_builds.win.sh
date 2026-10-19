#!/usr/bin/env bash
# Fix run 1: the touched modules' tests and lint, then both phone presets regenerated from the head and built
# release-like for the phone, the standard one's perfhud tests too (its FrameFeedTest takes the no-analytics
# branch), all in one hold of the shared Gradle lock:
#   bin/locked gradle bash out/HUD/f1_builds.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
gw() {
  local limit=$1; shift
  timeout --kill-after=2m "$limit" ./gradlew $NO_SDK_DOWNLOAD --console=plain "$@"
}

cd $W/templates/android
gw 30m :core:perfhud:testDebugUnitTest :core:perfhud:lintDebug :core:analytics:testDebugUnitTest \
  :core:devtools:testDebugUnitTest :core:ui:compileDebugKotlin :app:compileDevReleaseKotlin \
  :app:compileProdReleaseKotlin
echo TEMPLATE TARGETED OK

cd $W/generator
python create_project.py --spec $O/specs/hud-standard.json --out $O/standard --no-zip --force
python create_project.py --spec $O/specs/hud-everything.json --out $O/everything --no-zip --force
(cd $O/standard && gw 30m :core:perfhud:testDebugUnitTest :app:assembleDevRelease)
echo STANDARD OK
(cd $O/everything && gw 40m :app:assembleDevRelease :app:assembleProdRelease)
echo PHONE APKS OK
