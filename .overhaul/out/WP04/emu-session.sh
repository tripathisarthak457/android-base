#!/usr/bin/env bash
# One emulator session for what does not need the phone (the emulator is never gated, Part D 12):
#   build     (Gradle, before the emulator) the non-minified APK the profile is recorded on, and the
#             benchmark and profile task names for accept.sh;
#   bench     (Gradle, before the emulator) final-everything's benchmark test APK rebuilt with the
#             worktree's journeys, copied in with the package renamed as the generator renames it;
#   profile   the baseline profile from final-everything's journeys, run as the Gradle plugin runs them
#             (am instrument, then the outputs pulled and merged): the rules name the code paths the
#             journeys run, not their timings. Gradle and the emulator never run at once (15 GB laptop);
#   journeys  the profile's journeys rule alone, keeping the startup rule's earlier output;
#             either way the app's perf.shell log is kept, with a screenshot and a UI dump on a failure;
#   adaptive  tools/adaptive sizes, multiinstance and keyboard;
#   large     the large-window scenarios on a 1280x800 dp display (correctness only, no gate);
#   font      a tab switch recorded at twice the font size;
#   yfollow   a back swipe whose finger drifts down, then returns and cancels (the card follows it);
#   rtl       Force RTL (a reboot): a screenshot, a tab switch, a push, and right-edge back swipes.
# The emulator boots inside a short Gradle lock hold, so no build is running when it starts; it is
# stopped at the end, and every display and font setting changed here is restored.
#   $L emu ./emu-session.sh [build] [bench] [profile|journeys] [adaptive] [large] [font] [yfollow] [rtl] > logs/<step>.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools; S=emulator-5554; PKG=com.example.wp04.everything.dev
TEST_PKG=com.example.wp04.everything.benchmark
phases=("${@:-build profile adaptive large font rtl}")
read -r -a phases <<< "${phases[*]}"
step() { echo "== $(date +%H:%M:%S) $*"; "$@"; echo "== exit $? : $1"; }
want() { [[ " ${phases[*]} " == *" $1 "* ]]; }
booted() { [ "$(adb -s $S shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; }
EV=$O/final-everything

# An emulator this script did not boot is someone else's: it is used, never stopped.
STARTED=$O/logs/.emu-started-by-session
rm -f "$STARTED"
NAV_BEFORE=
RTL_BEFORE=
cleanup() {
  adb -s $S shell 'wm size reset; wm density reset; settings put system font_scale 1.0' >/dev/null 2>&1
  case "$RTL_BEFORE" in
    "") ;;
    null) adb -s $S shell settings delete global debug.force_rtl >/dev/null 2>&1 ;;
    *) adb -s $S shell settings put global debug.force_rtl "$RTL_BEFORE" >/dev/null 2>&1 ;;
  esac
  [ -n "$NAV_BEFORE" ] && adb -s $S shell cmd overlay enable-exclusive --category $NAV_BEFORE >/dev/null 2>&1
  if [ -f "$STARTED" ]; then
    adb -s $S emu kill >/dev/null 2>&1
    rm -f "$STARTED"
    echo "== $(date +%H:%M:%S) emulator stopped"
  fi
}
trap cleanup EXIT

# Started with none of this script's file descriptors, so the Gradle lock's descriptor does not
# outlive the hold in a process that keeps running.
boot() {
  if booted; then echo "== an emulator is already running"; return 0; fi
  touch "$STARTED"
  python3 -c 'import os, sys; os.closerange(3, 65536); os.setsid(); os.execv(sys.argv[1], sys.argv[1:])' \
    "$ANDROID_HOME/emulator/emulator" -avd periflow_api35 -no-window -no-snapshot -no-audio -no-boot-anim \
    < /dev/null > "$O/logs/r11-emulator.log" 2>&1 &
  timeout 300 adb -s $S wait-for-device || { echo "the emulator did not appear"; return 1; }
  for _ in $(seq 1 120); do booted && break; sleep 3; done
  booted || { echo "the emulator did not boot"; return 1; }
  echo "== $(date +%H:%M:%S) emulator booted"
}
export -f boot booted
export O S STARTED

if want build; then
  $L gradle bash -c '
    set -uo pipefail
    cd '"$EV"'
    ./gradlew :app:tasks :benchmark:tasks --all --console=plain > '"$O"'/logs/r11-tasks.txt 2>&1
    grep -i -E "baselineprofile|connected.*AndroidTest|NonMinified" '"$O"'/logs/r11-tasks.txt | head -40
    ./gradlew :app:assembleDevNonMinifiedRelease :benchmark:assembleNonMinifiedRelease --console=plain'
  echo "== exit $? : build"
fi

if want bench; then
  BENCH_SRC=$OV/wt/WP04/templates/android/benchmark/src/main/kotlin/com/base/app/benchmark
  BENCH_DST=$EV/benchmark/src/main/kotlin/com/example/wp04/everything/benchmark
  (cd $BENCH_SRC && find . -name '*.kt') | while read -r f; do
    sed 's/com\.base\.app/com.example.wp04.everything/g' "$BENCH_SRC/$f" > "$BENCH_DST/$f"
  done
  git -C $OV/wt/WP04 rev-parse --short HEAD > $EV/BENCH_BUILT_FROM
  $L gradle bash -c 'cd '"$EV"' && ./gradlew :benchmark:assembleNonMinifiedRelease --console=plain'
  echo "== exit $? : bench"
fi

if ! booted; then
  # The emulator takes 2.5 GB; boot it only with room for it and for a build that may start later.
  until [ "$(awk '/MemAvailable/ {print int($2/1024)}' /proc/meminfo)" -ge 4500 ]; do sleep 20; done
  MEMWAIT_MB=4500 MEMWAIT_WARM_MB=4500 $L gradle bash -c boot || exit 1
fi

# Edge swipes are back gestures only under gesture navigation; the AVD's own mode comes back at the end.
nav=$(adb -s $S shell cmd overlay list | grep -o '\[x\] com.android.internal.systemui.navbar.[a-z]*' | awk '{print $2}')
echo "== navigation: $nav, navigation_mode $(adb -s $S shell settings get secure navigation_mode | tr -d '\r')"
if [ -n "$nav" ] && [ "$nav" != com.android.internal.systemui.navbar.gestural ]; then
  NAV_BEFORE=$nav
  adb -s $S shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
  sleep 3
  echo "== navigation now: $(adb -s $S shell settings get secure navigation_mode | tr -d '\r')"
fi

if want profile || want journeys; then
  APK=$(ls $EV/app/build/outputs/apk/dev/nonMinifiedRelease/*x86_64*.apk $EV/app/build/outputs/apk/dev/nonMinifiedRelease/*universal*.apk 2>/dev/null | head -1)
  TEST_APK=$EV/benchmark/build/outputs/apk/nonMinifiedRelease/benchmark-nonMinifiedRelease.apk
  echo "== target $APK"
  adb -s $S uninstall $PKG >/dev/null 2>&1; adb -s $S uninstall $TEST_PKG >/dev/null 2>&1
  step adb -s $S install -r -t "$APK"
  step adb -s $S install -r -t "$TEST_APK"
  MEDIA=/sdcard/Android/media/$TEST_PKG/additional_test_output
  adb -s $S shell "rm -rf /sdcard/Android/media/$TEST_PKG; mkdir -p $MEDIA"
  OUT=$P/profile/emulator
  if want profile; then CLASS=$TEST_PKG.BaselineProfileGenerator; rm -rf $OUT; else CLASS=$TEST_PKG.BaselineProfileGenerator#journeys; fi
  mkdir -p $OUT/additional_test_output
  stamp=$(date +%H%M)
  adb -s $S logcat -c
  step adb -s $S shell am instrument -w -r \
    -e class $CLASS \
    -e androidx.benchmark.enabledRules BaselineProfile \
    -e additionalTestOutputDir $MEDIA \
    -e targetAppId $PKG \
    $TEST_PKG/androidx.test.runner.AndroidJUnitRunner | tee $OUT/instrument-$stamp.txt
  if grep -q 'FAILURES!!!' $OUT/instrument-$stamp.txt; then
    adb -s $S exec-out screencap -p > $OUT/failure-$stamp.png
    adb -s $S shell uiautomator dump /sdcard/failure.xml >/dev/null && adb -s $S pull /sdcard/failure.xml $OUT/failure-$stamp.xml >/dev/null
    echo "== failure kept: $OUT/failure-$stamp.{png,xml}"
  fi
  adb -s $S logcat -d -v time > $OUT/logcat-$stamp.txt
  grep -E 'perf\.shell|TestRunner|AndroidRuntime|BackAnimation|BackNavigation' $OUT/logcat-$stamp.txt > $OUT/shell-$stamp.txt
  echo "== $(grep -c 'perf.shell' $OUT/shell-$stamp.txt) perf.shell lines in $OUT/shell-$stamp.txt"
  adb -s $S pull $MEDIA $OUT/
  ls -la $OUT/additional_test_output
  GEN=$EV/app/src/main/generated/baselineProfiles
  mkdir -p $GEN
  # As the Gradle plugin does, the startup rules are part of the baseline profile too.
  cat $OUT/additional_test_output/*-prof.txt | sort -u > $GEN/baseline-prof.txt
  cat $OUT/additional_test_output/*startup*-startup-prof.txt 2>/dev/null | sort -u > $GEN/startup-prof.txt
  wc -l $GEN/*.txt
  adb -s $S uninstall $TEST_PKG >/dev/null 2>&1
fi

cd $EV
if want adaptive; then
  step $T/adaptive/adaptive.sh --package $PKG --variant devBenchmarkRelease --serial $S --out $P/adaptive/emulator \
    sizes multiinstance keyboard
fi

if want large; then
  adb -s $S shell 'wm size 2560x1600; wm density 320'
  sleep 3
  step $T/perf/perf.sh --package $PKG --variant devBenchmarkRelease --runs 1 --serial $S --out $P/emulator-large \
    tabs_large pushpop_large listdetail_select scroll_large sidebar_toggle
  adb -s $S logcat -d -b crash > $P/emulator-large/crash.txt
  adb -s $S shell 'wm size reset; wm density reset'
fi

if want font; then
  adb -s $S shell settings put system font_scale 2.0
  rm -rf $P/tab-switch/emulator-font2
  step $T/perf/record.sh $PKG tab_switch $P/tab-switch/emulator-font2 --serial $S
  adb -s $S shell settings put system font_scale 1.0
fi
if want yfollow; then
  rm -rf $P/predictive-back/emulator-y-follow
  step $T/perf/record.sh $PKG back_y_follow $P/predictive-back/emulator-y-follow --extra perf.theme=light --serial $S
fi
if want rtl; then
  # An app locale the app has no strings for resolves to the app's default language, so an Arabic app
  # locale leaves an untranslated app left to right (run 12), and the Force RTL setting alone only takes
  # at boot on this image, where the reboot left System UI unresponsive (run 14). The developer option
  # works through the debug.force_rtl property, which every layout direction computed from a locale
  # reads; a per-app locale other than the system's makes the system compute the app's again.
  R=$P/rtl/emulator
  rm -rf $R; mkdir -p $R
  RTL_PROP_BEFORE=$(adb -s $S shell getprop debug.force_rtl | tr -d '\r')
  adb -s $S shell setprop debug.force_rtl true
  adb -s $S shell am force-stop $PKG
  adb -s $S shell cmd locale set-app-locales $PKG --locales en-GB
  echo "== $(date +%H:%M:%S) debug.force_rtl=$(adb -s $S shell getprop debug.force_rtl | tr -d '\r')"
  adb -s $S shell am start -W -n $PKG/com.example.wp04.everything.MainActivity --es perf.fakeSession true >/dev/null
  sleep 4
  adb -s $S exec-out screencap -p > $R/root.png
  adb -s $S shell uiautomator dump /sdcard/rtl.xml >/dev/null && adb -s $S pull /sdcard/rtl.xml $R/root.xml >/dev/null
  python3 - "$R/root.xml" <<'PY'
import re, sys
xml = open(sys.argv[1]).read()
left = {m.group(1): int(m.group(2)) for m in re.finditer(r'resource-id="(tab_\d)"[^>]*bounds="\[(\d+),', xml)}
print("== tab lefts:", left, "mirrored" if left.get("tab_0", 0) > left.get("tab_1", 0) else "NOT MIRRORED")
PY
  step $T/perf/record.sh $PKG tab_switch $R/tab_switch --serial $S
  step $T/perf/record.sh $PKG push $R/push --serial $S
  step $T/perf/record.sh $PKG back_slow_commit $R/back_slow_commit --edge right --serial $S
  step $T/perf/record.sh $PKG back_during_push $R/back_during_push --edge right --serial $S
  adb -s $S shell cmd locale set-app-locales $PKG --locales ""
  adb -s $S shell setprop debug.force_rtl "${RTL_PROP_BEFORE:-false}"
fi
echo EMU-SESSION DONE
