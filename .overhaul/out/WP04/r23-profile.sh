#!/usr/bin/env bash
# Run 23: step 23 at HEAD from its own generated copy (profile-everything), so an acceptance run can regenerate and
# build final-everything beside it. The APKs the profile task installs are built under the Gradle lock alone first,
# so the phone is held only for the recording (run 22 held it for hours behind another package's build).
#   ./r23-profile.sh > logs/r23-profile.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
EV=$O/profile-everything
echo "== $(date +%H:%M:%S) at $(git -C $W rev-parse --short HEAD) $(git -C $W status --short | wc -l) dirty"
python3 $O/rmtree-long.py $EV
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.everything \
  --out $EV --no-zip --force > $O/logs/gen-profile-everything.log
git -C $W rev-parse --short HEAD > $EV/BUILT_FROM
echo "== $(date +%H:%M:%S) generated"
cd $EV
$L gradle ./gradlew :app:assembleDevNonMinifiedRelease :benchmark:assembleNonMinifiedRelease --console=plain
echo "== $(date +%H:%M:%S) prebuilt"
cd $O
$L adb ./profile.win.sh $EV
echo "== $(date +%H:%M:%S)"
echo R23-PROFILE OK
