#!/usr/bin/env bash
# The Gradle-free steps of accept.win.sh on scratch copies, to prove the Windows port before the real run: the
# catalogue check, the five fixtures, the cross-feature patch, the split and the moves-only assertion, the guard edit.
set -euo pipefail
OV=D:/projects/tools/.overhaul
W=$OV/wt/WP02
O=$OV/out/WP02
D=$O/r17-dry
rm -rf $D && mkdir -p $D

cd $W/generator
python -m genkit.core.catalogue --check
for spec in standard standard-modular none modules-compact modules-modular; do
  python create_project.py --spec tests/fixtures/v2/$spec.json --out $D/$spec --no-zip --force > /dev/null
  test -f $D/$spec/settings.gradle.kts
done

G="git -c user.name=wp02 -c user.email=wp02@localhost -c core.autocrlf=false"
cp -r $D/modules-compact $D/crossnav
(cd $D/crossnav && $G init -q && $G add -A && $G commit -qm base)
python $O/apply_crossnav_patch.py $D/crossnav
(cd $D/crossnav && $G add -A && $G commit -qm patched)
python -m genkit.targets.android.modes $D/crossnav
(cd $D/crossnav && $G add -A && $G diff --cached -M --name-status HEAD | python $O/assert_moves_only.py)

cd $D/modules-compact
sed -i 's#dependencies {#dependencies {\n    implementation(project(":feature:orders"))#' feature/trips/build.gradle.kts
grep -q 'implementation(project(":feature:orders"))' feature/trips/build.gradle.kts
echo R17 DRY OK
