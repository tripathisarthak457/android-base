#!/usr/bin/env bash
# After phase 9:
#  1. build `dl` from the worktree: layer's tree plus DevLaunchOptions checking for a stored session without
#     decrypting it (Gradle lock);
#  2. in one adb hold: the scroll scenario's first flings after `pm clear` on layer, traced (scroll-trace.win.sh);
#     perf.sh's cold start on layer and dl, in both orders, three runs each; then one capture each of cold starts
#     alternated between the old template and layer, and between the old template and dl (startup-ab-trace.win.sh).
#   ./phase10.win.sh > logs/phase10-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
APK=app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk
echo "== $(date '+%F %H:%M:%S') phase 10: building dl"
$O/build-wt.win.sh dl > $O/logs/build-dl.log 2>&1 || { echo "dl did not build (logs/build-dl.log)"; exit 1; }
tail -1 $O/logs/build-dl.log
until grep -q "PHASE9 DONE" $O/logs/phase9-win.log 2>/dev/null; do sleep 20; done
echo "== $(date '+%F %H:%M:%S') phase 10: phone"
$L adb bash -c "
install() { adb -s 9a7c74f4 install -r -t -d $O/\$1/$APK > /dev/null && echo \"\$1 installed\"; }
# As perf.sh and coldstart-ab.win.sh leave an app before timing it: data cleared, the APK's profile installed, compiled
# speed-profile, started once (the first start after a clear writes the app's files and the session).
prep() {
  A='adb -s 9a7c74f4'
  \$A shell am force-stop \$1; \$A shell pm clear \$1 > /dev/null
  \$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n \$1/androidx.profileinstaller.ProfileInstallReceiver > /dev/null
  \$A shell am force-stop \$1; \$A shell cmd package compile -f -m speed-profile \$1 > /dev/null
  c=\$(\$A shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER \$1 | tr -d '\r' | tail -1)
  \$A shell am start -W -n \$c --es perf.fakeSession true > /dev/null; sleep 6; \$A shell input keyevent KEYCODE_HOME; sleep 1
  echo \"\$1 prepared\"
}
install layer
$O/scroll-trace.win.sh $P/layer-scroll
NOTRACE=1 TAG=-c1 SCENARIOS=coldstart $O/measure.win.sh layer dl
NOTRACE=1 TAG=-c2 SCENARIOS=coldstart $O/measure.win.sh dl layer
install layer
prep com.example.wp04.base.dev; prep com.example.wp04.everything.dev
$O/startup-ab-trace.win.sh $P/startab-base-layer 6 com.example.wp04.base.dev com.example.wp04.everything.dev
install dl
prep com.example.wp04.everything.dev
$O/startup-ab-trace.win.sh $P/startab-base-dl 6 com.example.wp04.base.dev com.example.wp04.everything.dev
"
echo "== $(date '+%F %H:%M:%S') PHASE10 DONE"
