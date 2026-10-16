#!/usr/bin/env bash
# REQUIREMENTS 34 on the phone (the orchestrator's "shimmer and clipping" note, WP04's items): screenshots of
# the Utility (docked bar) build in light and dark, at rest, mid-list, at the end of a list and during a pull;
# font scale 1.5; the landscape rail at the end of a list; plus a scroll-to-end recording of the bar's scroll
# edge. Restores rotation, font scale and the app's theme.
#   $L adb ./r17-req34.sh <apk> <package> <activity> <out dir> > logs/<step>.log 2>&1
set -uo pipefail
apk=$1; pkg=$2; act=$3; out=$4; S=9a7c74f4
a() { adb -s $S "$@"; }
mkdir -p "$out"
saved_rot=$(a shell settings get system accelerometer_rotation)
saved_user_rot=$(a shell settings get system user_rotation)
saved_font=$(a shell settings get system font_scale)
restore() {
  a shell settings put system accelerometer_rotation "${saved_rot:-0}"
  a shell settings put system user_rotation "${saved_user_rot:-0}"
  if [ "$saved_font" = "null" ] || [ -z "$saved_font" ]; then a shell settings delete system font_scale; else a shell settings put system font_scale "$saved_font"; fi
}
trap restore EXIT
a shell input keyevent KEYCODE_WAKEUP; sleep 0.5
if a shell dumpsys window | grep -m1 isKeyguardShowing | grep -q "isKeyguardShowing=true"; then echo "LOCKED"; exit 2; fi
a shell settings put system accelerometer_rotation 0
a shell settings put system user_rotation 0
[ -n "$apk" ] && a install -r -d "$apk" | tail -1
a shell pm clear $pkg >/dev/null
read -r W H < <(a shell wm size | grep -o "[0-9]*x[0-9]*" | tail -1 | tr x ' ')
X=$((W / 2))
shot() { a exec-out screencap -p > "$out/$1.png"; echo "shot $1"; }
launch() { a shell am force-stop $pkg; sleep 0.5; a shell am start -W -n $pkg/$act --es perf.fakeSession true "$@" >/dev/null; sleep 4; }
up() { a shell input swipe $X $((H * 3 / 4)) $X $((H / 4)) ${1:-250}; sleep ${2:-0.8}; }
to_end() { for _ in $(seq 1 ${1:-45}); do a shell input swipe $X $((H * 4 / 5)) $X $((H / 6)) 70; sleep 0.15; done; sleep 3; }
for theme in light dark; do
  launch --es perf.theme $theme
  shot "$theme-rest-top"
  up; sleep 1.5; shot "$theme-mid"
  to_end; shot "$theme-end"
  # A pull at the top: hold the finger down mid-pull for the shot.
  a shell input keyevent KEYCODE_MOVE_HOME 2>/dev/null
  launch --es perf.theme $theme
  a shell input motionevent DOWN $X $((H * 3 / 10))
  for y in 34 38 42 46 50; do a shell input motionevent MOVE $X $((H * y / 100)); done
  sleep 0.6; shot "$theme-pull"
  a shell input motionevent UP $X $((H / 2)); sleep 2
  # The settings tab (a scroll view) at its end.
  a shell input tap $((W * 9 / 10)) $((H - 120)); sleep 1.5; to_end 8; shot "$theme-settings-end"
done
# The scroll edge look as a recording: from the middle of the list to its end and back.
launch --es perf.theme light
up
a shell rm -f /sdcard/r17.mp4
a shell screenrecord --time-limit 9 --size 720x1600 --bit-rate 16000000 /sdcard/r17.mp4 &
rec=$!
sleep 1.2
to_end 12
a shell input swipe $X $((H / 3)) $X $((H * 2 / 3)) 300; sleep 1.5
wait $rec; sleep 1
a pull /sdcard/r17.mp4 "$out/scroll-edge.mp4" >/dev/null
# Font scale 1.5.
a shell settings put system font_scale 1.5
launch --es perf.theme light
shot "font150-rest"; to_end; shot "font150-end"
a shell settings put system font_scale "${saved_font/null/1.0}"
# Landscape: the rail, at the end of a list (the gesture handle and the 3-button bar sit at the bottom edge).
a shell settings put system user_rotation 1
launch --es perf.theme light
sleep 1; shot "landscape-rest"; to_end; shot "landscape-end"
a shell settings put system user_rotation 0
launch --es perf.theme system
a shell am force-stop $pkg
echo R17-REQ34 DONE
