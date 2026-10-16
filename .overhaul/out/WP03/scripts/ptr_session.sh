#!/usr/bin/env bash
# One adb lock hold: install the PTR build and record it. ptr_session.sh <serial> <apk> <out>
set -uo pipefail
S=$1; APK=$2; OUT=$3; PKG=com.example.wp03.ptr.dev
adb -s $S install -r -d "$APK" | tail -1
bash /home/crazyhusir/Documents/projects/tools/.overhaul/out/WP03/scripts/ptr_record.sh $S $PKG "$OUT"
