#!/usr/bin/env bash
# device_checks.sh on the Windows machine:  device_checks.win.sh <serial>
# The phone (9a7c74f4) is the acceptance device; there is no emulator here.
set -euo pipefail
OV=D:/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP06; P=$OV/perf/WP06
S=$1
mkdir -p $P
# devRelease is split per ABI: install the split the device runs.
abi=$($L adb adb -s $S shell getprop ro.product.cpu.abi | tr -d '\r')
apk() { ls $1/app/build/outputs/apk/dev/release/*-$abi-release.apk; }
# The devtools badge follows a drag (everything has devtools; dev flavour).
$L adb bash $O/with_phone.sh $S python $O/badge_drag_check.py --serial $S \
  --package com.example.wp06.everything.dev --apk "$(apk $O/everything)" --out $P/badge-$S
# The badge keeps clear of the large title, the search field and Back, at rest and after a drag and a throw to the
# top edge, and Back under it pops (QA-44, K13).
$L adb bash $O/with_phone.sh $S python $O/badge_qa_check.py --serial $S ${BADGE_QA_FLAGS---top-chrome} \
  --package com.example.wp06.badgeqa.dev --apk "$(apk $O/badgeqa)" --out $P/badge-qa-$S
echo "DEVICE CHECKS OK on $S"
