source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.jorwen.periflow.dev
$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $PKG/androidx.profileinstaller.ProfileInstallReceiver | tail -1
$A shell am force-stop $PKG; $A shell cmd package compile -f -m speed-profile $PKG
$A shell dumpsys package dexopt | grep -A3 "\[$PKG\]" | grep status
for i in 1 2 3 4 5 6 7; do $A shell am force-stop $PKG; sleep 2; $A shell am start -W -n $PKG/com.jorwen.periflow.MainActivity | grep -E "TotalTime|LaunchState" | tr '\n' ' '; echo; sleep 3; done
sleep 2
$A shell dumpsys gfxinfo $PKG reset >/dev/null; sleep 5
echo "idle5s Today: $($A shell dumpsys gfxinfo $PKG | grep -m1 'Total frames rendered')"
$A shell dumpsys gfxinfo $PKG framestats > $R/peri_idle_fs.txt
