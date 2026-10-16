#!/usr/bin/env bash
# Run 11 on the phone, in one adb lock hold: the current final-everything and final-standard installed and
# compiled as perf.sh compiles them, the recordings (phone-rec.sh), then the phase 3 gates before the
# generated baseline profile (phone-gates.sh, with the --compile speed A/B).
#   $L adb ./phone-r11.sh > logs/r11-phone.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04
export ANDROID_SERIAL=9a7c74f4
for n in everything standard; do
  build_type=release; [ $n = everything ] && build_type=benchmarkRelease
  apk=$O/final-$n/app/build/outputs/apk/dev/$build_type/app-dev-arm64-v8a-$build_type.apk
  echo "== install $apk"
  adb install -r "$apk" && adb shell cmd package compile -m speed-profile -f com.example.wp04.$n.dev
done
adb shell input keyevent KEYCODE_WAKEUP
$O/phone-rec.sh
SPEED=1 $O/phone-gates.sh phase3b everything standard lean
echo PHONE-R11 DONE
