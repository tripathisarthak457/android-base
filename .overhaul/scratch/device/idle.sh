source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=$1; TAG=$2
$A shell input keyevent 4; sleep 1
for t in "${@:3}"; do
  set -- $t
  $A shell input tap $1 $2; sleep 3
  $A shell dumpsys gfxinfo $PKG reset >/dev/null; sleep 5
  echo "$TAG idle5s at tab($1,$2): $($A shell dumpsys gfxinfo $PKG | grep -m1 'Total frames rendered')"
done
