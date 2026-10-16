#!/usr/bin/env bash
# Waits until the phone is connected and unlocked (an agent may not enter its PIN), checking every two
# minutes in a short adb lock hold, then exits so the phase 3 runs can start.
#   ./phone-watch.sh > logs/<step>.log 2>&1
set -uo pipefail
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked; S=9a7c74f4
while true; do
  state=$($L adb timeout 20 adb -s $S shell 'dumpsys trust | grep -o "deviceLocked=[01]" | head -1' 2>/dev/null | tr -d '\r')
  echo "$(date +%H:%M:%S) ${state:-offline}"
  [ "$state" = deviceLocked=0 ] && { echo PHONE UNLOCKED; exit 0; }
  sleep 120
done
