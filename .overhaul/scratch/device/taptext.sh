# taptext.sh <text>... : for each text, dump UI, tap the first node whose text/desc contains it (case-insensitive)
source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
for T in "$@"; do
  dump_ui tt
  pos=$(python3 $D/ui.py $R/ui_tt.xml "$T" | head -1 | awk '{print $1, $2}')
  if [ -z "$pos" ]; then echo "NOT FOUND: $T"; python3 $D/ui.py $R/ui_tt.xml | awk '{$1="";$2="";$3=""; print}' | grep -v "''" | head -25; exit 1; fi
  echo "tap '$T' at $pos"; $A shell input tap $pos; sleep 1.5
done
