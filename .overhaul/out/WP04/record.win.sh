#!/usr/bin/env bash
# Run 21 on the phone: the REQUIREMENTS 24, 28 and 32 recordings again from final-everything at the worktree's HEAD
# (release build), plus the phone clips run 11 left to the emulator: the y-follow swipe, RTL and twice the font.
# The font scale and the app's locale are the owner's settings: read first and put back however the script ends.
#   $L adb ./record.win.sh <apk> <out dir> > logs/<step>.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4; PKG=com.example.wp04.everything.dev
ACT=com.example.wp04.everything.MainActivity
APK=$1; R=$2; mkdir -p $R
export ANDROID_SERIAL=$S
adb install -r -d "$APK" | tail -1
adb shell pm clear $PKG >/dev/null
font=$(adb shell settings get system font_scale | tr -d '\r')
restore() {
  if [ "$font" = null ] || [ -z "$font" ]; then adb shell settings delete system font_scale >/dev/null; else adb shell settings put system font_scale "$font"; fi
  adb shell cmd locale set-app-locales $PKG --user 0 --locales ""
  echo "restored: font scale $font, the app's own locale"
}
trap restore EXIT
rec() { echo "== $(date +%H:%M:%S) $*"; $T/record.sh $PKG "$@" --serial $S 2>&1 | tail -3; }
rec tab_switch $R/tab-switch/light --extra perf.theme=light
rec tab_switch $R/tab-switch/dark --extra perf.theme=dark
for j in back_slow_commit back_slow_cancel back_fast_flick back_y_follow back_to_home; do
  rec $j $R/predictive-back/$j --extra perf.theme=light
done
rec back_slow_commit $R/predictive-back/back_slow_commit_right --edge right --extra perf.theme=light
rec back_slow_commit $R/predictive-back/dark/back_slow_commit --extra perf.theme=dark
rec back_during_push $R/back-during-push/left --edge left --extra perf.theme=light
rec back_during_push $R/back-during-push/right --edge right --extra perf.theme=light
rec rapid_back $R/rapid-back --extra perf.theme=light
# RTL: an app locale other than the system's, read back from the activity's configuration.
for locale in ar-XB ar; do
  adb shell cmd locale set-app-locales $PKG --user 0 --locales $locale
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/$ACT --es perf.fakeSession true >/dev/null
  sleep 2
  config=$(adb shell dumpsys activity activities | grep -m1 -o "mLastReportedConfiguration={[^}]*}")
  echo "$locale: $config"
  if echo "$config" | grep -q ldrtl; then
    rec tab_switch $R/rtl/tab-switch --extra perf.theme=light
    rec back_slow_commit $R/rtl/back_slow_commit --edge right --extra perf.theme=light
    rec back_during_push $R/rtl/back-during-push --edge right --extra perf.theme=light
    break
  fi
done
adb shell cmd locale set-app-locales $PKG --user 0 --locales ""
adb shell settings put system font_scale 2.0
rec tab_switch $R/font2/tab-switch --extra perf.theme=light
echo RECORD DONE
