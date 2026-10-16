source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=com.acme.field.dev
$A shell cmd statusbar collapse
$A shell am force-stop $PKG
$A push $D/session.preferences_pb /data/local/tmp/session.preferences_pb
$A shell chmod 644 /data/local/tmp/session.preferences_pb
$A shell run-as $PKG cp /data/local/tmp/session.preferences_pb files/datastore/session.preferences_pb; echo "cp rc=$?"
$A shell run-as $PKG ls -la files/datastore
$A shell rm /data/local/tmp/session.preferences_pb
$A shell am start -W -n $PKG/com.acme.field.MainActivity | grep -E "TotalTime|LaunchState"
sleep 4
shot debug_launch2
dump_ui debug_launch2
