#!/usr/bin/env bash
# The clips run 24's record.win.sh could not make: back_y_follow (fixed in f60c0c7: the finger now crosses the
# touch slop before it drifts) and RTL, whose check read `mLastReportedConfiguration` from dumpsys, which this
# Android 17 build no longer prints. RTL is now read from the app itself: the first tab sits at the right end.
# The app's locale is the owner's setting: put back however the script ends.
#   $L adb ./record-r25.win.sh <apk> <out dir> > logs/<step>.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4; PKG=com.example.wp04.everything.dev
ACT=com.example.wp04.everything.MainActivity
APK=$1; R=$2; mkdir -p $R
export ANDROID_SERIAL=$S
adb install -r -d "$APK" | tail -1
adb shell pm clear $PKG >/dev/null
restore() {
  adb shell cmd locale set-app-locales $PKG --user 0 --locales ""
  echo "restored: the app's own locale"
}
trap restore EXIT
rec() { echo "== $(date +%H:%M:%S) $*"; $T/record.sh $PKG "$@" --serial $S 2>&1 | tail -3; }
rec back_y_follow $R/predictive-back/back_y_follow --extra perf.theme=light
for locale in ar-XB ar; do
  adb shell cmd locale set-app-locales $PKG --user 0 --locales $locale
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/$ACT --es perf.fakeSession true >/dev/null
  sleep 3
  side=$(python3 -c "
import sys; sys.path.insert(0, '$T'); import perf
device = perf.Device('$S'); tab = device.ui().find('tab_0')
print('none' if tab is None else ('right' if tab.center[0] > device.size[0] // 2 else 'left'))")
  echo "$locale: tab_0 at the $side"
  if [ "$side" = right ]; then
    rec tab_switch $R/rtl/tab-switch --extra perf.theme=light
    rec back_slow_commit $R/rtl/back_slow_commit --edge right --extra perf.theme=light
    rec back_during_push $R/rtl/back-during-push --edge right --extra perf.theme=light
    break
  fi
done
echo RECORD-R25 DONE
