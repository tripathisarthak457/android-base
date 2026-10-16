set -x
source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.acme.field.dev
$A install -r /home/crazyhusir/Documents/projects/tools/.overhaul/out/Full/app/build/outputs/apk/dev/debug/app-dev-debug.apk
$A shell am force-stop $PKG
$A shell run-as $PKG mkdir -p files/datastore
$A exec-in run-as $PKG sh -c 'cat > files/datastore/session.preferences_pb' < $D/session.preferences_pb
$A shell run-as $PKG ls -la files/datastore
$A shell am start -W -n $PKG/com.acme.field.MainActivity
sleep 4
shot debug_launch
dump_ui debug_launch
