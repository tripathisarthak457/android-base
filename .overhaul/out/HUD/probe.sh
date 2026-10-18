#!/usr/bin/env bash
set -uo pipefail
S=9a7c74f4; ID=com.example.hud.everything.dev
adb -s $S shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $ID | tail -2
adb -s $S shell am force-stop $ID
adb -s $S shell am start -W -n $ID/com.example.hud.everything.MainActivity --es perf.hud on | tail -3
sleep 4
adb -s $S shell input swipe 540 1800 540 700 140
sleep 2
pid=$(adb -s $S shell pidof $ID | tr -d '\r')
echo "pid=$pid"
adb -s $S shell ps -T -p $pid | head -80
adb -s $S shell logcat -d -s PerfHud:I | tail -5
