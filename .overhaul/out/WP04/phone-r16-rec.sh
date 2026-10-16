#!/usr/bin/env bash
# Run 16 on the phone, one adb lock hold: the tab switch, a right-edge back card and a back swipe during a push in
# RTL, and the tab switch at twice the font size (CP1 §4), on final-everything's devBenchmarkRelease build. RTL
# without translations: debug.force_rtl plus an app locale other than the system's, which makes the system compute
# the app's layout direction again. The owner's font scale, the property and the app's locale are put back however
# the script ends.
#   $L adb ./phone-r16-rec.sh > logs/<step>.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4; PKG=com.example.wp04.everything.dev
export ANDROID_SERIAL=$S
font=$(adb shell settings get system font_scale | tr -d '\r')
rtl=$(adb shell getprop debug.force_rtl | tr -d '\r')
restore() {
  if [ "$font" = null ]; then adb shell settings delete system font_scale >/dev/null; else adb shell settings put system font_scale "$font"; fi
  adb shell setprop debug.force_rtl "${rtl:-false}"
  adb shell cmd locale set-app-locales $PKG --user 0 --locales ""
  echo "restored: font scale $font, debug.force_rtl ${rtl:-false}, the app's own locale"
}
trap restore EXIT
system=$(adb shell getprop persist.sys.locale | tr -d '\r')
other=en-GB; [ "$system" = en-GB ] && other=en-AU
adb shell setprop debug.force_rtl true
adb shell cmd locale set-app-locales $PKG --user 0 --locales $other
adb shell am force-stop $PKG
adb shell am start -W -n $PKG/com.example.wp04.everything.MainActivity --es perf.fakeSession true >/dev/null
sleep 3
adb shell uiautomator dump /data/local/tmp/rtl.xml >/dev/null; adb pull /data/local/tmp/rtl.xml $O/logs/rtl-root.xml >/dev/null
echo "tab_0 bounds: $(grep -o 'resource-id="tab_0"[^>]*bounds="[^"]*"' $O/logs/rtl-root.xml | grep -o 'bounds="[^"]*"')"
mkdir -p $P/tab-switch $P/predictive-back/rtl $P/back-during-push
rm -rf $P/tab-switch/rtl $P/predictive-back/rtl/back_slow_commit $P/back-during-push/rtl
$T/record.sh $PKG tab_switch $P/tab-switch/rtl --extra perf.theme=light --serial $S
$T/record.sh $PKG back_slow_commit $P/predictive-back/rtl/back_slow_commit --edge right --extra perf.theme=light --serial $S
$T/record.sh $PKG back_during_push $P/back-during-push/rtl --edge right --extra perf.theme=light --serial $S
adb shell setprop debug.force_rtl "${rtl:-false}"
adb shell cmd locale set-app-locales $PKG --user 0 --locales ""
adb shell settings put system font_scale 2.0
rm -rf $P/tab-switch/font2
$T/record.sh $PKG tab_switch $P/tab-switch/font2 --extra perf.theme=light --serial $S
echo PHONE-R16-REC DONE
