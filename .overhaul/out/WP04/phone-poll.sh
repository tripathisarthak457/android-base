#!/usr/bin/env bash
# Waits up to $1 minutes (default 45) for the phone, checking each minute without holding the adb lock between checks.
OV=D:/projects/tools/.overhaul; L=$OV/bin/locked
end=$(( $(date +%s) + ${1:-45} * 60 ))
while [ "$(date +%s)" -lt "$end" ]; do
  if $L adb adb devices 2>/dev/null | grep -q '^9a7c74f4[[:space:]]*device'; then echo "PHONE $(date +%H:%M:%S)"; exit 0; fi
  sleep 60
done
echo "NO PHONE $(date +%H:%M:%S)"; exit 1
