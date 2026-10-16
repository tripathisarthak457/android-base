#!/usr/bin/env bash
# tools/adaptive fold and posture on a foldable emulator (wp04_fold_api35: the pixel_9_pro_fold profile on the
# installed android-35 google_apis x86_64 image), from final-everything. Correctness only, never gated. The
# emulator boots inside a short Gradle lock hold, so no build is running when it starts, and is stopped at the end.
#   $L emu ./emu-fold.sh > logs/<step>.log 2>&1
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools; S=emulator-5554; PKG=com.example.wp04.everything.dev
booted() { [ "$(adb -s $S shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; }
if booted; then echo "an emulator is already running; not starting another"; exit 1; fi

trap 'adb -s $S emu kill >/dev/null 2>&1; echo "== $(date +%H:%M:%S) emulator stopped"' EXIT
boot() {
  python3 -c 'import os, sys; os.closerange(3, 65536); os.setsid(); os.execv(sys.argv[1], sys.argv[1:])' \
    "$ANDROID_HOME/emulator/emulator" -avd wp04_fold_api35 -no-window -no-snapshot -no-audio -no-boot-anim \
    < /dev/null > "$O/logs/r11-emulator-fold.log" 2>&1 &
  timeout 300 adb -s $S wait-for-device || return 1
  for _ in $(seq 1 120); do booted && break; sleep 3; done
  booted && echo "== $(date +%H:%M:%S) emulator booted"
}
export -f boot booted
export O S

until [ "$(awk '/MemAvailable/ {print int($2/1024)}' /proc/meminfo)" -ge 4500 ]; do sleep 20; done
MEMWAIT_MB=4500 MEMWAIT_WARM_MB=4500 $L gradle bash -c boot || { echo "the emulator did not boot"; exit 1; }

cd $O/final-everything
$T/adaptive/adaptive.sh --package $PKG --variant devBenchmarkRelease --serial $S --out $P/adaptive/emulator-fold \
  fold posture
echo "== exit $? : adaptive fold posture"
echo EMU-FOLD DONE
