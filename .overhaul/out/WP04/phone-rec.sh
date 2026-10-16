#!/usr/bin/env bash
# The recordings requirements 24, 28 and 32 and CP1 ask for, on the phone, in one adb lock hold, from the
# installed final-everything (and final-standard for four tabs). RTL is the app's own locale (ar-XB, set
# per app and reset after), so the phone's settings are not touched.
#   $L adb ./phone-rec.sh > logs/<step>.log 2>&1
set -uo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04; P=$OV/perf/WP04
T=$OV/wt/WP04/templates/android/tools/perf; S=9a7c74f4; PKG=com.example.wp04.everything.dev
export ANDROID_SERIAL=$S
step() { echo "== $*"; "$@"; echo "== exit $? : $2 $3"; }
rec() { local out=$1; shift; rm -rf "$out"; step $T/record.sh "$PKG" "$@" --serial $S; }

R=$P/predictive-back
for j in back_slow_commit back_slow_cancel back_fast_flick back_y_follow back_to_home gesture_pop; do
  rec $R/$j $j $R/$j
done
rec $R/back_slow_commit_right back_slow_commit $R/back_slow_commit_right --edge right
rec $R/dark/back_slow_commit back_slow_commit $R/dark/back_slow_commit --extra perf.theme=dark
for e in left right; do rec $P/back-during-push/$e back_during_push $P/back-during-push/$e --edge $e; done

rec $P/tab-switch/light tab_switch $P/tab-switch/light --extra perf.theme=light
rec $P/tab-switch/dark tab_switch $P/tab-switch/dark --extra perf.theme=dark

adb shell cmd locale set-app-locales $PKG --user 0 --locales ar-XB
rec $P/tab-switch/rtl tab_switch $P/tab-switch/rtl --extra perf.theme=light
rec $R/rtl/back_slow_commit back_slow_commit $R/rtl/back_slow_commit --edge right
rec $P/back-during-push/rtl back_during_push $P/back-during-push/rtl --edge right
adb shell cmd locale set-app-locales $PKG --user 0 --locales ""

F=$P/phase3-frames
for j in push pop pushpop rapid_back scroll; do rec $F/$j $j $F/$j; done
rec $F/bar_minimize custom $F/bar_minimize --extra perf.barMinimize=OnScrollDown \
  --actions 'input swipe 540 1800 540 700 140; sleep 1.5; input swipe 540 700 540 1800 140'

if adb shell pm path com.example.wp04.standard.dev | grep -q package:; then
  PKG=com.example.wp04.standard.dev rec $P/tab-switch/four-tabs tab_switch $P/tab-switch/four-tabs
fi
echo PHONE-REC DONE
