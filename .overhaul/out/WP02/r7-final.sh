#!/usr/bin/env bash
# Post-M2 sizes and configuration time, then the M2 acceptance, inside one Gradle lock hold, so the busy queue is
# joined once instead of once per command. adb is not used.
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
$OV/bin/locked gradle bash -c '
  O='"$O"'
  export L=$O/nolock
  date > $O/logs/sizes-post-m2.start
  $O/r5-sizes.sh > $O/logs/sizes-post-m2.log 2>&1
  date > $O/logs/accept-m2.start
  $O/accept.sh > $O/logs/accept-m2.log 2>&1
  echo "accept exit=$?" >> $O/logs/accept-m2.log
  date > $O/logs/accept-m2.end
'
