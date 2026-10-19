#!/usr/bin/env bash
# The phone half of research/back-google/DESIGN.md §5, in one adb hold, after build-ladder.win.sh logged LADDER BUILT:
#   1. perf.sh pushpop_gesture + meminfo, 3 runs per build, in the order head tex c1 c2, then c2 c1 tex head
#   2. perf.sh back_during_push, 3 runs per build (the mid-push card: one opaque card, no double exposure)
#   3. back-trace.win.sh: two Perfetto captures per build, in both orders
#   4. per build: heldmem.win.sh (held card at 45 %, GPU memory), rec-gesture.win.sh (commit), rec-cancel.win.sh
# Results under perf/WP04-perf/bg-<name>-{bg1,bg2,bgp,t1,t2,held,rec,cancel}. Changes no device setting.
#   ./bg-phone.win.sh > D:/projects/tools/.overhaul/out/WP04-perf/logs/bg-phone.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; R=$OV/research/back-google; L=$OV/bin/locked
grep -q "LADDER BUILT" $O/logs/bg-build.log || { echo "the ladder is not built (logs/bg-build.log)"; exit 1; }
echo "== $(date '+%F %H:%M:%S') bg phone"
$L adb bash -c "
set -uo pipefail
export MSYS_NO_PATHCONV=1
NOTRACE=1 TAG=-bg1 SCENARIOS='pushpop_gesture meminfo' $O/measure.win.sh bg-head bg-tex bg-c1 bg-c2
NOTRACE=1 TAG=-bg2 SCENARIOS='pushpop_gesture meminfo' $O/measure.win.sh bg-c2 bg-c1 bg-tex bg-head
NOTRACE=1 TAG=-bgp SCENARIOS=back_during_push $O/measure.win.sh bg-head bg-tex bg-c1 bg-c2
$R/bin/back-trace.win.sh t1 bg-head bg-tex bg-c1 bg-c2
$R/bin/back-trace.win.sh t2 bg-c2 bg-c1 bg-tex bg-head
pkg=com.example.wp04.everything.dev
for n in bg-head bg-tex bg-c1 bg-c2; do
  apk=$O/\$n/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
  adb -s 9a7c74f4 shell am force-stop \$pkg
  adb -s 9a7c74f4 install -r -t -d \"\$apk\" > /dev/null
  adb -s 9a7c74f4 shell pm clear \$pkg > /dev/null
  adb -s 9a7c74f4 shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n \$pkg/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
  adb -s 9a7c74f4 shell am force-stop \$pkg
  adb -s 9a7c74f4 shell cmd package compile -f -m speed-profile \$pkg > /dev/null
  adb -s 9a7c74f4 shell am start -W -n \$pkg/com.example.wp04.everything.MainActivity --es perf.fakeSession true > /dev/null
  sleep 6
  $R/bin/heldmem.win.sh $P/\$n-held \$pkg
  $O/rec-gesture.win.sh $P/\$n-rec \$pkg
  $R/bin/rec-cancel.win.sh $P/\$n-cancel \$pkg
done
"
echo "== $(date '+%F %H:%M:%S') BG PHONE DONE"
