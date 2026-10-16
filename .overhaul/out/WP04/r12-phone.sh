#!/usr/bin/env bash
# Run 12 on the phone, in one adb lock hold: the recordings run 11 missed (phone-r11c.sh without the
# profile, which run 12 recorded on the emulator), then the phase 3 gates on all three presets with the
# --compile speed A/B.
#   $L adb ./r12-phone.sh > logs/<step>.log 2>&1
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04
export ANDROID_SERIAL=9a7c74f4
PROFILE=0 $O/phone-r11c.sh
echo "== phone-r11c exit $?"
SPEED=1 $O/phone-gates.sh phase3b everything standard lean
echo R12-PHONE DONE
