#!/usr/bin/env bash
# Waits up to MINUTES (default 45) for the phone to be connected and unlocked (an agent may not enter its PIN),
# checking every two minutes in a short adb lock hold. Exits 0 once it is usable, 1 when the time runs out.
#   ./phone-wait.sh > logs/<step>.log 2>&1
set -uo pipefail
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked; S=9a7c74f4
deadline=$(( $(date +%s) + ${MINUTES:-45} * 60 ))
while [ "$(date +%s)" -lt "$deadline" ]; do
  state=$($L adb timeout 20 adb -s $S shell 'dumpsys trust | grep -o "deviceLocked=[01]" | head -1' 2>/dev/null | tr -d '\r')
  echo "$(date +%H:%M:%S) ${state:-offline}"
  [ "$state" = deviceLocked=0 ] && { echo PHONE READY; exit 0; }
  sleep 120
done
echo "PHONE MISSING after ${MINUTES:-45} min"
exit 1
