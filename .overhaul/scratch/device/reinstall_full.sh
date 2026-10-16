source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.acme.field.dev
$A shell am force-stop $PKG
$A install -r /home/crazyhusir/Documents/projects/tools/.overhaul/out/Full/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk
$A shell am start -W -n $PKG/com.acme.field.MainActivity | grep TotalTime; sleep 4
$A shell am broadcast -a androidx.profileinstaller.action.INSTALL_PROFILE -n $PKG/androidx.profileinstaller.ProfileInstallReceiver | tail -1
$A shell am force-stop $PKG; $A shell cmd package compile -f -m speed-profile $PKG
$A shell am start -W -n $PKG/com.acme.field.MainActivity | grep TotalTime; sleep 4
