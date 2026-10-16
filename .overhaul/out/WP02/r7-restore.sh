#!/usr/bin/env bash
# Minified devRelease of a sample+settings project without the nav-key keep rules, then the
# process-death restore check on the phone (restore_check.py).
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP02
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
$O/r6-restore-build.sh
apk=$(find $O/restore/app/build/outputs/apk/dev/release -name '*universal*.apk' -o -name '*arm64*.apk' 2>/dev/null | head -1)
[ -z "$apk" ] && apk=$(find $O/restore/app/build/outputs/apk -name '*.apk' | head -1)
echo "apk=$apk"
[ -z "$apk" ] && { echo "NO APK"; exit 1; }
cd $O && $L adb python3 $O/restore_check.py "$apk"
echo "exit=$?"
