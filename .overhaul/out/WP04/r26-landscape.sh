#!/usr/bin/env bash
# REQUIREMENTS 34's landscape shots (K6: the rail, the end of a list above the gesture handle), run 25's
# r25-landscape.sh with the APK installed first, so the shots show the build the other "after" shots show, whatever
# another package installed meanwhile. The display is turned with the app already in front, as adaptive.sh does: on
# this phone a display turned before a cold launch comes back portrait once the launcher has been in front. Every
# rotation setting is read first and put back on exit.
#   $L adb ./r26-landscape.sh <apk> <package> <activity> <out dir> > logs/<step>.log 2>&1
set -uo pipefail
apk=$1; pkg=$2; act=$3; out=$4; S=9a7c74f4
a() { adb -s $S "$@"; }
mkdir -p "$out"
saved_rot=$(a shell settings get system accelerometer_rotation | tr -d '\r')
saved_user_rot=$(a shell settings get system user_rotation | tr -d '\r')
saved_wm_rot=$(a shell wm user-rotation | tr -d '\r')
echo "saved: accelerometer_rotation=$saved_rot user_rotation=$saved_user_rot wm user-rotation='$saved_wm_rot'"
restore() {
  a shell settings put system user_rotation "${saved_user_rot:-0}"
  a shell settings put system accelerometer_rotation "${saved_rot:-0}"
  case "$saved_wm_rot" in
    lock*) a shell wm user-rotation $saved_wm_rot ;;
    free*) a shell wm user-rotation free ;;
  esac
  echo "restored: $(a shell wm user-rotation | tr -d '\r'), accelerometer_rotation $(a shell settings get system accelerometer_rotation | tr -d '\r')"
}
trap restore EXIT
a shell input keyevent KEYCODE_WAKEUP; sleep 0.5
if a shell dumpsys window | grep -m1 isKeyguardShowing | grep -q "isKeyguardShowing=true"; then echo "LOCKED"; exit 2; fi
a shell cmd statusbar collapse
a install -r -d "$apk" | tail -1
a shell am force-stop $pkg; sleep 0.5
a shell am start -W -n $pkg/$act --es perf.fakeSession true --es perf.theme light >/dev/null; sleep 4
a shell "settings put system accelerometer_rotation 0; settings put system user_rotation 1"; sleep 2
read -r W H < <(a shell wm size | grep -o "[0-9]*x[0-9]*" | tail -1 | tr x ' ')
echo "rotation: $(a shell dumpsys window displays | grep -m1 -o 'mCurrentRotation=[A-Z_0-9]*')"
echo "front: $(a shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r')"
shot() { a exec-out screencap -p > "$out/$1.png"; echo "shot $1"; }
shot landscape-rest
# Swipes across the short side: the display is turned, so the long side of `wm size` is now its width.
X=$((H / 2)); Y=$W
for _ in $(seq 1 45); do a shell input swipe $X $((Y * 4 / 5)) $X $((Y / 6)) 70; sleep 0.15; done; sleep 3
echo "front: $(a shell dumpsys window | grep -m1 mCurrentFocus | tr -d '\r')"
shot landscape-end
a shell am force-stop $pkg
echo R26-LANDSCAPE DONE
