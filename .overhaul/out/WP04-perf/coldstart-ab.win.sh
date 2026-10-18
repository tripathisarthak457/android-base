#!/usr/bin/env bash
# Cold starts of several builds interleaved in one adb hold, so the phone's temperature and clocks drift over all of
# them alike: each <name>=<package> is installed from out/WP04-perf/<name> as perf.sh installs it (pm clear, the APK's
# profile, speed-profile), opened once, then started N rounds of one start per build (force-stop, then am start -W;
# TotalTime, as perf.sh's coldstart reads it). Writes perf/WP04-perf/coldstart-ab/<label>.tsv and a median per build.
#   $L adb ./coldstart-ab.win.sh <label> <rounds> base=com.example.wp04.base.dev layer=com.example.wp04.everything.dev
# KEEP="<name>..." starts those builds as the phone already has them (no install), with the same pm clear, profile and
# compile: the old base on the phone is Oct 9's Linux build, signed with that machine's debug key.
set -uo pipefail
# Device paths (/data, /sdcard) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf/coldstart-ab; S=9a7c74f4
A="adb -s $S"
label=$1; rounds=$2; shift 2
mkdir -p $P
out=$P/$label.tsv
echo -e "round\tbuild\ttotal_ms" > $out
activity() {
  $A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$1" | tr -d '\r' | tail -1
}
declare -A comp
for pair in "$@"; do
  n=${pair%%=*}; pkg=${pair#*=}
  apk=$O/$n/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
  $A shell am force-stop $pkg
  if [[ " ${KEEP:-} " == *" $n "* ]]; then
    echo "$n: as installed, $($A shell dumpsys package $pkg | tr -d '\r' | grep -m1 lastUpdateTime | xargs)"
  else
    [ -f "$apk" ] || { echo "no apk for $n"; exit 2; }
    $A install -r -t -d "$apk" > /dev/null || { echo "install $n failed"; exit 2; }
  fi
  $A shell pm clear $pkg > /dev/null
  $A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $pkg/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
  $A shell am force-stop $pkg
  $A shell cmd package compile -f -m speed-profile $pkg > /dev/null
  comp[$n]=$(activity $pkg)
  echo "$n: ${comp[$n]} ($(cat $O/$n/BUILT_FROM 2>/dev/null))"
  # The first start after pm clear writes the app's files and fonts once; it is not a cold start a user repeats.
  $A shell am start -W -n "${comp[$n]}" --es perf.fakeSession true > /dev/null
  sleep 6
  $A shell input keyevent KEYCODE_HOME; sleep 1
done
for round in $(seq 1 $rounds); do
  for pair in "$@"; do
    n=${pair%%=*}; pkg=${pair#*=}
    $A shell am force-stop $pkg; sleep 1.5
    t=$($A shell am start -W -n "${comp[$n]}" --es perf.fakeSession true | tr -d '\r' | awk '/TotalTime/{print $2}')
    echo -e "$round\t$n\t$t" >> $out
    sleep 3
    $A shell input keyevent KEYCODE_HOME; sleep 1
  done
done
$A shell input keyevent KEYCODE_HOME
python3 - "$out" <<'PY'
import csv, statistics, sys
rows = list(csv.DictReader(open(sys.argv[1]), delimiter="\t"))
for build in dict.fromkeys(r["build"] for r in rows):
    v = [int(r["total_ms"]) for r in rows if r["build"] == build and r["total_ms"]]
    print(f"{build}: median {statistics.median(v)} ms, mean {statistics.mean(v):.1f}, n {len(v)}, runs {v}")
PY
echo COLDSTART-AB DONE
