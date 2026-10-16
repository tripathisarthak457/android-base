# first.sh <pkg> <tag> <n> <actionA> <actionB> : alternates A and B, saving framestats after each
source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
PKG=$1; TAG=$2; N=$3; AA=$4; BB=$5
mkdir -p $R/$TAG
for i in $(seq 1 $N); do
  $A shell dumpsys gfxinfo $PKG reset >/dev/null; $A shell "$AA"; sleep 1.6; $A shell dumpsys gfxinfo $PKG framestats > $R/$TAG/a_$i.txt
  $A shell dumpsys gfxinfo $PKG reset >/dev/null; $A shell "$BB"; sleep 1.6; $A shell dumpsys gfxinfo $PKG framestats > $R/$TAG/b_$i.txt
done
