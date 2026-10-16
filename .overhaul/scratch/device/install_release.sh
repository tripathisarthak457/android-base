source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.acme.field.dev
$A shell am force-stop $PKG
$A install -r /home/crazyhusir/Documents/projects/tools/.overhaul/out/Full/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk
$A shell dumpsys package $PKG | grep -E "versionName|flags=\[" | head -3
$A shell dumpsys package dexopt | grep -A3 "\[$PKG\]" | head -6
echo "== cold start, as installed"
for i in 1 2 3 4 5 6; do $A shell am force-stop $PKG; sleep 2; $A shell am start -W -n $PKG/com.acme.field.MainActivity | grep -E "TotalTime|WaitTime|LaunchState" | tr '\n' ' '; echo; sleep 3; done
$A shell dumpsys package dexopt | grep -A3 "\[$PKG\]" | head -6
echo "== profile install + speed-profile compile"
$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $PKG/androidx.profileinstaller.ProfileInstallReceiver | tail -1
sleep 2
$A shell am force-stop $PKG
$A shell cmd package compile -f -m speed-profile $PKG
$A shell dumpsys package dexopt | grep -A3 "\[$PKG\]" | head -6
echo "== cold start, speed-profile"
for i in 1 2 3 4 5 6; do $A shell am force-stop $PKG; sleep 2; $A shell am start -W -n $PKG/com.acme.field.MainActivity | grep -E "TotalTime|WaitTime|LaunchState" | tr '\n' ' '; echo; sleep 3; done
echo "== warm start (HOME then relaunch)"
for i in 1 2 3; do $A shell input keyevent 3; sleep 2; $A shell am start -W -n $PKG/com.acme.field.MainActivity | grep -E "TotalTime|LaunchState" | tr '\n' ' '; echo; sleep 2; done
sleep 2; shot release_launch; dump_ui release_launch
