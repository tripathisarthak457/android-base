#!/usr/bin/env bash
# Run 11 on the phone, second pass, in one adb lock hold: the baseline profile again with the fixed journeys
# (phone-r11b.sh without its recordings), then the recordings the first pass missed: the y-follow swipe (its
# video had one frame) and a tab switch at twice the font size (CP1 §4). The font scale is the owner's setting:
# it is read first and put back however the script ends.
#   $L adb ./phone-r11c.sh > logs/<step>.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4; PKG=com.example.wp04.everything.dev
export ANDROID_SERIAL=$S
RECORD=0 $O/phone-r11b.sh || exit $?

font=$(adb shell settings get system font_scale | tr -d '\r')
restore_font() {
  if [ "$font" = null ]; then adb shell settings delete system font_scale >/dev/null; else adb shell settings put system font_scale "$font"; fi
  adb shell cmd locale set-app-locales $PKG --user 0 --locales ""
  echo "restored: font scale $font, the app's own locale"
}
trap restore_font EXIT
R=$P/predictive-back
rm -rf $R/back_y_follow
$T/record.sh $PKG back_y_follow $R/back_y_follow --extra perf.theme=light --serial $S
# RTL: the first pass set the app's locale to ar-XB and recorded a left-to-right layout; see what the
# activity was given, and fall back to plain Arabic (still English strings, mirrored) if ar-XB is dropped.
for locale in ar-XB ar; do
  adb shell cmd locale set-app-locales $PKG --user 0 --locales $locale
  echo "app locales: $(adb shell cmd locale get-app-locales $PKG --user 0 | tr -d '\r')"
  adb shell am force-stop $PKG
  adb shell am start -W -n $PKG/com.example.wp04.everything.MainActivity --es perf.fakeSession true >/dev/null
  sleep 2
  config=$(adb shell dumpsys activity activities | grep -m1 -o "mLastReportedConfiguration={[^}]*}")
  echo "$locale: $config"
  if echo "$config" | grep -q ldrtl; then
    rm -rf $P/tab-switch/rtl
    $T/record.sh $PKG tab_switch $P/tab-switch/rtl --extra perf.theme=light --serial $S
    rm -rf $R/rtl/back_slow_commit $P/back-during-push/rtl
    $T/record.sh $PKG back_slow_commit $R/rtl/back_slow_commit --edge right --extra perf.theme=light --serial $S
    $T/record.sh $PKG back_during_push $P/back-during-push/rtl --edge right --extra perf.theme=light --serial $S
    break
  fi
done
adb shell cmd locale set-app-locales $PKG --user 0 --locales ""

adb shell settings put system font_scale 2.0
rm -rf $P/tab-switch/font2
$T/record.sh $PKG tab_switch $P/tab-switch/font2 --extra perf.theme=light --serial $S
echo PHONE-R11C DONE
