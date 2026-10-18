#!/usr/bin/env bash
# The design system's whole build (every test, lint) on wp/WP03's worktree, then the look project the user ran
# (com.example.wp03.look.catalog) generated again from that worktree and its catalog built for release, as WP03's
# accept.sh builds it. One hold of the Gradle lock, two workers, as accept.sh:
#   OVERHAUL_GRADLE_WORKERS=2 bin/locked gradle bash qa/bug-scroll-direction/tools/rebuild.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP03; O=$OV/out/WP03
cd $W/templates/android
./gradlew :core:designsystem:build --console=plain
echo "DESIGNSYSTEM BUILD OK"
cd $W/generator
python create_project.py --spec tests/fixtures/v2/custom-look.json --set package_name=com.example.wp03.look --out $O/look --no-zip --force
git -C $W rev-parse --short HEAD > $O/look/BUILT_FROM
git -C $W status --short -- templates > $O/look/BUILT_DIRTY
cd $O/look
./gradlew :catalog:assembleRelease --console=plain
echo "LOOK CATALOG BUILT from $(cat $O/look/BUILT_FROM) + $(wc -l < $O/look/BUILT_DIRTY) dirty"
