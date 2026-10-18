#!/usr/bin/env bash
# Run 24's REQUIREMENTS 34 and D9 "after" shots again where the phone, not the app, spoiled them: the light pull
# caught the notification shade, the landscape shots stayed portrait (`settings put system user_rotation` no
# longer turned the display), and D9 ran with the phone itself in dark mode, so it could not show a dark app on a
# light phone. Rotation goes through `wm user-rotation`; the phone's night mode is set to light for D9. Every
# setting it changes is read first and put back on exit.
#   $L adb ./r25-req34-redo.sh <apk> <package> <activity> <req34 out dir> <d9 out dir> > logs/<step>.log 2>&1
set -uo pipefail
apk=$1; pkg=$2; act=$3; out=$4; d9out=$5; S=9a7c74f4
O=D:/projects/tools/.overhaul/out/WP04
a() { adb -s $S "$@"; }
mkdir -p "$out"
saved_rot=$(a shell settings get system accelerometer_rotation | tr -d '\r')
saved_user_rot=$(a shell settings get system user_rotation | tr -d '\r')
saved_wm_rot=$(a shell wm user-rotation | tr -d '\r')
saved_night=$(a shell cmd uimode night | tr -d '\r' | sed 's/^Night mode: //')
echo "saved: accelerometer_rotation=$saved_rot user_rotation=$saved_user_rot wm user-rotation='$saved_wm_rot' night=$saved_night"
restore() {
  case "$saved_wm_rot" in
    lock*) a shell wm user-rotation $saved_wm_rot ;;
    free*) a shell wm user-rotation free ;;
  esac
  a shell settings put system accelerometer_rotation "${saved_rot:-0}"
  a shell settings put system user_rotation "${saved_user_rot:-0}"
  [ -n "$saved_night" ] && a shell cmd uimode night "$saved_night" >/dev/null
  echo "restored: $(a shell wm user-rotation | tr -d '\r'), night $(a shell cmd uimode night | tr -d '\r')"
}
trap restore EXIT
a shell input keyevent KEYCODE_WAKEUP; sleep 0.5
if a shell dumpsys window | grep -m1 isKeyguardShowing | grep -q "isKeyguardShowing=true"; then echo "LOCKED"; exit 2; fi
a shell cmd statusbar collapse
a shell wm user-rotation lock 0
a install -r -d "$apk" | tail -1
a shell pm clear $pkg >/dev/null
read -r W H < <(a shell wm size | grep -o "[0-9]*x[0-9]*" | tail -1 | tr x ' ')
X=$((W / 2))
front() { a shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r'; }
shot() { echo "shot $1: $(front)"; a exec-out screencap -p > "$out/$1.png"; }
launch() { a shell am force-stop $pkg; sleep 0.5; a shell am start -W -n $pkg/$act --es perf.fakeSession true "$@" >/dev/null; sleep 4; }
to_end() { for _ in $(seq 1 ${1:-45}); do a shell input swipe $X $((H * 4 / 5)) $X $((H / 6)) 70; sleep 0.15; done; sleep 3; }
# A pull at the top of the light list, the finger held down mid-pull for the shot.
launch --es perf.theme light
a shell input motionevent DOWN $X $((H * 3 / 10))
for y in 34 38 42 46 50; do a shell input motionevent MOVE $X $((H * y / 100)); done
sleep 0.6; shot "light-pull"
a shell input motionevent UP $X $((H / 2)); sleep 2
# Landscape: the rail, at the end of a list (the gesture handle sits at the bottom edge).
a shell wm user-rotation lock 1
launch --es perf.theme light
sleep 1; shot "landscape-rest"; to_end; shot "landscape-end"
a shell wm user-rotation lock 0
a shell am force-stop $pkg
# D9 on a light phone: the app's theme dark, the system's light.
a shell cmd uimode night no >/dev/null; sleep 2
cd $O && ./d9-coldstart.sh "" $pkg $act "$d9out"
a shell am force-stop $pkg
echo R25-REDO DONE
