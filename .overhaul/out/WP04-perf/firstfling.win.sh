#!/usr/bin/env bash
# What the first scroll after `pm clear` draws for the first time (REQUIREMENTS 41: no hitch on the first fling after a
# cold start or `pm clear`). The installed everything APK is left as perf.sh leaves it (data cleared, profile, speed-
# profile, one launch, settled); then, in one Perfetto capture (HWUI's shader_compile sections, input, FrameTimeline),
# the finger goes down on the list and moves it 20 px at a time to 600 px, then lifts, then two flings; `steps.txt`
# has each step's device uptime, so a compile can be put between two known scroll positions. Screenshots at rest,
# at 600 px and at the end. Under the adb lock:  $L adb ./firstfling.win.sh <out-dir> [package]
set -uo pipefail
# Device paths (/sdcard, /data) must reach adb as typed; every local path here is a D:/ path.
export MSYS_NO_PATHCONV=1
out=$1; pkg=${2:-com.example.wp04.everything.dev}; S=9a7c74f4
A="adb -s $S"
mkdir -p "$out"
comp=$($A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER $pkg | tr -d '\r' | tail -1)
$A shell input keyevent KEYCODE_WAKEUP; sleep 0.5
$A shell am force-stop $pkg
$A shell pm clear $pkg > /dev/null
$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $pkg/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
$A shell am force-stop $pkg
$A shell cmd package compile -f -m speed-profile $pkg > /dev/null
$A shell am start -W -n "$comp" --es perf.fakeSession true > "$out/am-start.txt" 2>&1
sleep 6
size=$($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
w=${size%x*}; h=${size#*x}
x=$((w / 2)); y0=$((h * 75 / 100))
cat > "$out/config.pbtxt" <<CFG
buffers { size_kb: 65536 fill_policy: RING_BUFFER }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" target_buffer: 0 process_stats_config { scan_all_processes_on_start: true } } }
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_apps: "$pkg"
    }
  }
}
duration_ms: 45000
CFG
$A shell "perfetto --txt -c - -o /data/misc/perfetto-traces/firstfling.perfetto-trace" < "$out/config.pbtxt" > "$out/perfetto.log" 2>&1 &
sleep 2
shot() { $A shell screencap -p /data/local/tmp/ff.png; $A pull /data/local/tmp/ff.png "$out/$1.png" > /dev/null; }
shot 0-rest
$A push "$(dirname "$0")/ffdrag.sh" /data/local/tmp/ffdrag.sh > /dev/null
$A shell rm -f /data/local/tmp/ffsteps.txt
# 20 px at a time, each step's time recorded, so a compile lands between two known positions of the list.
$A shell sh /data/local/tmp/ffdrag.sh $x $y0 $h
$A pull /data/local/tmp/ffsteps.txt "$out/steps.txt" > /dev/null
$A pull /data/local/tmp/ff1.png "$out/1-move600.png" > /dev/null
shot 5-end
wait
$A pull /data/misc/perfetto-traces/firstfling.perfetto-trace "$out/trace.perfetto-trace" > /dev/null 2>&1
$A shell rm -f /data/misc/perfetto-traces/firstfling.perfetto-trace /data/local/tmp/ff.png /data/local/tmp/ff1.png /data/local/tmp/ffsteps.txt /data/local/tmp/ffdrag.sh
ls -la "$out/trace.perfetto-trace"
echo FIRSTFLING DONE
