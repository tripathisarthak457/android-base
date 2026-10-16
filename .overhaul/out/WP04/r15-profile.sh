#!/usr/bin/env bash
# Step 23: splits final-everything's generated baseline profile into its modules, then copies each
# module's profile into the template with the package renamed back (the generator renames it again).
#   ./r15-profile.sh
set -euo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04/templates/android; EV=$OV/out/WP04/final-everything
test -s $EV/app/src/main/generated/baselineProfiles/baseline-prof.txt
grep -q 'BaselineProfileGenerator_journeys' <(ls $OV/perf/WP04/profile/emulator/additional_test_output)
python3 $W/tools/perf/split_profile.py --project $EV
for m in core/navigation core/designsystem core/ui app; do
  sed 's#com/example/wp04/everything/#com/base/app/#g; s#com/example/wp04/everything;#com/base/app;#g' \
    $EV/$m/src/main/baseline-prof.txt > $W/$m/src/main/baseline-prof.txt
  echo "$m: $(wc -l < $W/$m/src/main/baseline-prof.txt) lines, $(grep -c 'wp04' $W/$m/src/main/baseline-prof.txt || true) unrenamed"
done
