source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.acme.field.dev
$A shell am force-stop $PKG
$A install -r /home/crazyhusir/Documents/projects/tools/.overhaul/out/FullDevtools/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk
$A shell am start -W -n $PKG/com.acme.field.MainActivity | grep TotalTime
sleep 3
dump_ui dt0; shot dt0
