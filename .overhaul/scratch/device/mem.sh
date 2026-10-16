source /home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/common.sh
for P in com.acme.field.dev com.jorwen.periflow.dev; do
  if [ $P = com.acme.field.dev ]; then ACT=com.acme.field.MainActivity; else ACT=com.jorwen.periflow.MainActivity; fi
  $A shell am start -W -n $P/$ACT >/dev/null; sleep 3
  echo "== $P"; $A shell dumpsys meminfo $P | grep -E "TOTAL PSS|TOTAL RSS|Java Heap|Native Heap|Graphics|GL mtrack|EGL mtrack|Views:|  TOTAL:" | head -8
  $A shell dumpsys gfxinfo $P | grep -E "Total GPU memory usage|Pipeline=|Layers Total" | head -4
done
