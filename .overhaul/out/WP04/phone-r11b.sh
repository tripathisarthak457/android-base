#!/usr/bin/env bash
# Run 11 on the phone, in one adb lock hold:
#   1. the baseline profile from final-everything's journeys, run as the Gradle plugin runs them (the non-minified
#      app and the benchmark's non-minified test APK, am instrument, outputs pulled and merged), so the phone is not
#      held while Gradle waits for its lock;
#   2. the current final-everything and final-standard installed and compiled as perf.sh compiles them, then the
#      recordings (phone-rec.sh). record.sh holds the phone upright and gives its settings back.
#   $L adb ./phone-r11b.sh > logs/r11-phone3.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04; EV=$O/final-everything
export ANDROID_SERIAL=9a7c74f4
PKG=com.example.wp04.everything.dev; TEST_PKG=com.example.wp04.everything.benchmark
step() { echo "== $(date +%H:%M:%S) $*"; "$@"; echo "== exit $? : $1 $2"; }

adb shell input keyevent KEYCODE_WAKEUP
if adb shell dumpsys trust | grep -q "deviceLocked=1"; then echo "PHONE LOCKED: nothing run"; exit 3; fi

# The journeys' coordinates are the portrait layout's; the owner's rotation and screen timeout come back after.
auto=$(adb shell settings get system accelerometer_rotation | tr -d '\r')
rotation=$(adb shell settings get system user_rotation | tr -d '\r')
stay=$(adb shell settings get global stay_on_while_plugged_in | tr -d '\r')
restore() {
  for kv in "system accelerometer_rotation $auto" "system user_rotation $rotation" "global stay_on_while_plugged_in $stay"; do
    set -- $kv
    if [ "$3" = null ]; then adb shell settings delete $1 $2 >/dev/null; else adb shell settings put $1 $2 $3; fi
  done
  echo "restored: auto-rotate $auto, rotation $rotation, stay-on $stay"
}
trap restore EXIT
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb shell settings put global stay_on_while_plugged_in 7

if [ "${PROFILE:-1}" = 1 ]; then
  adb uninstall $PKG >/dev/null 2>&1; adb uninstall $TEST_PKG >/dev/null 2>&1
  step adb install -r -t $EV/app/build/outputs/apk/dev/nonMinifiedRelease/app-dev-arm64-v8a-nonMinifiedRelease.apk
  step adb install -r -t $EV/benchmark/build/outputs/apk/nonMinifiedRelease/benchmark-nonMinifiedRelease.apk
  MEDIA=/sdcard/Android/media/$TEST_PKG/additional_test_output
  adb shell "rm -rf /sdcard/Android/media/$TEST_PKG; mkdir -p $MEDIA"
  step adb shell am instrument -w -r \
    -e class $TEST_PKG.BaselineProfileGenerator \
    -e androidx.benchmark.enabledRules BaselineProfile \
    -e additionalTestOutputDir $MEDIA \
    -e targetAppId $PKG \
    $TEST_PKG/androidx.test.runner.AndroidJUnitRunner
  OUT=$P/profile/phone
  rm -rf $OUT; mkdir -p $OUT
  adb pull $MEDIA $OUT/
  ls -la $OUT/additional_test_output
  GEN=$EV/app/src/main/generated/baselineProfiles
  mkdir -p $GEN
  # As the Gradle plugin does, the startup rules are part of the baseline profile too.
  cat $OUT/additional_test_output/*-prof.txt | sort -u > $GEN/baseline-prof.txt
  cat $OUT/additional_test_output/*-startup-prof.txt 2>/dev/null | sort -u > $GEN/startup-prof.txt
  wc -l $GEN/*.txt
  adb uninstall $TEST_PKG >/dev/null 2>&1
  adb uninstall $PKG >/dev/null 2>&1
  echo PROFILE DONE
fi

for n in everything standard; do
  build_type=release; [ $n = everything ] && build_type=benchmarkRelease
  apk=$O/final-$n/app/build/outputs/apk/dev/$build_type/app-dev-arm64-v8a-$build_type.apk
  step adb install -r "$apk"
  adb shell cmd package compile -m speed-profile -f com.example.wp04.$n.dev
done
[ "${RECORD:-1}" = 1 ] && $O/phone-rec.sh
echo PHONE-R11B DONE
