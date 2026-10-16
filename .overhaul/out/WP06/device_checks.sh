#!/usr/bin/env bash
# accept.sh's device checks on one device:  device_checks.sh <serial>
# The phone (9a7c74f4) is the acceptance device. The periflow_api35 emulator (emulator-5554, 411 dp wide, the
# width QA-44 names) stands in for correctness only when the phone is missing.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP06; P=$OV/perf/WP06
S=$1
mkdir -p $P
# devRelease is split per ABI: install the split the device runs.
abi=$($L adb adb -s $S shell getprop ro.product.cpu.abi | tr -d '\r')
apk() { ls $1/app/build/outputs/apk/dev/release/*-$abi-release.apk; }
# The devtools badge follows a drag (everything has devtools; dev flavour).
$L adb bash $O/with_phone.sh $S python3 $O/badge_drag_check.py --serial $S \
  --package com.example.wp06.everything.dev --apk "$(apk $O/everything)" --out $P/badge-$S
# The badge keeps clear of the large title and Back, and Back under it pops (QA-44).
$L adb bash $O/with_phone.sh $S python3 $O/badge_qa_check.py --serial $S \
  --package com.example.wp06.badgeqa.dev --apk "$(apk $O/badgeqa)" --out $P/badge-qa-$S
echo "DEVICE CHECKS OK on $S"
