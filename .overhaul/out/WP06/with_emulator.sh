#!/usr/bin/env bash
# Runs a command with the periflow_api35 emulator up:  $L emu bash with_emulator.sh <command...>
# `locked emu` holds the Gradle lock as well, so no build runs while the emulator is up (the 15 GB laptop). The
# emulator is shut down at the end; one this script did not boot is someone else's and is left running.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP06
S=emulator-5554
booted() { [ "$(adb -s $S shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; }
started=no
stop() { if [ $started = yes ]; then adb -s $S emu kill >/dev/null 2>&1; echo "emulator stopped"; fi; }
trap stop EXIT
if ! booted; then
  started=yes
  # Without this shell's descriptors, so the locks' descriptors do not outlive the hold.
  python3 -c 'import os, sys; os.closerange(3, 65536); os.setsid(); os.execv(sys.argv[1], sys.argv[1:])' \
    "$ANDROID_HOME/emulator/emulator" -avd periflow_api35 -no-window -no-snapshot -no-audio -no-boot-anim \
    < /dev/null > "$O/logs/emulator.log" 2>&1 &
  timeout 300 adb -s $S wait-for-device || { echo "the emulator did not appear"; exit 1; }
  for _ in $(seq 1 120); do booted && break; sleep 3; done
  booted || { echo "the emulator did not boot"; exit 1; }
  # Boot completes long before the post-boot broadcasts are handled; input sent in between can meet a
  # "System UI isn't responding" dialog on a busy laptop.
  timeout 180 adb -s $S shell am wait-for-broadcast-idle >/dev/null || echo "broadcasts still busy after 180 s"
  echo "emulator booted"
fi
"$@"
