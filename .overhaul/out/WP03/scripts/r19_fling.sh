#!/usr/bin/env bash
# REQUIREMENTS 41's phone proof alone: the look catalog (installed and speed-compiled by r19_phone.sh) flung against
# iOS's curve.  $L adb bash $O/scripts/with_phone.sh <serial> bash $O/scripts/r19_fling.sh <serial>
set -uo pipefail
serial=$1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; P=$OV/perf/WP03
python "$O/scripts/fling_curve.py" --serial "$serial" --package com.example.wp03.look.catalog --out "$P/fling"
status=$?
echo "R19 FLING EXIT $status"
exit $status
