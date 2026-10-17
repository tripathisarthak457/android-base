#!/usr/bin/env bash
# Run 22, step 23 at HEAD: final-everything generated from the worktree, then the baseline profile recorded on the
# phone and copied into the template (profile.win.sh; adb lock outside, Gradle lock inside).
#   ./r22-profile.sh > logs/r22-profile.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
echo "== $(date +%H:%M:%S) at $(git -C $W rev-parse --short HEAD) $(git -C $W status --short | wc -l) dirty"
python3 $O/rmtree-long.py $O/final-everything
cd $W/generator
python3 create_project.py --spec tests/fixtures/v2/everything.json --set package_name=com.example.wp04.everything \
  --out $O/final-everything --no-zip --force > $O/logs/gen-everything.log
git -C $W rev-parse --short HEAD > $O/final-everything/BUILT_FROM
echo "== $(date +%H:%M:%S) generated"
cd $O
$L adb ./profile.win.sh
echo "== $(date +%H:%M:%S)"
echo R22-PROFILE OK
