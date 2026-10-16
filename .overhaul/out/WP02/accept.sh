#!/usr/bin/env bash
# WP02 milestone M2 acceptance (design/wp/WP02.md "M2 acceptance", with the deviations the handoff
# records). Run in the background; the log must end with ACCEPT OK.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
W=$OV/wt/WP02
L=${L:-$OV/bin/locked}
O=$OV/out/WP02
mkdir -p $O/logs

cd $W/templates/android && $L gradle ./gradlew build --console=plain
# The baseline-profile consumer still configures after the variant trim.
$L gradle ./gradlew :app:generateBaselineProfile --dry-run --console=plain

cd $W/generator
python3 -m unittest discover -s tests -t .
python3 -m tests.consistency --quick
python3 -m genkit.core.catalogue --check

for spec in standard standard-modular none modules-compact modules-modular; do
  python3 create_project.py --spec tests/fixtures/v2/$spec.json --out $O/$spec --no-zip --force
done
for d in standard standard-modular; do (cd $O/$d && $L gradle ./gradlew check assembleDevDebug --console=plain); done
# Unit tests run only in modules that have them (debug only): libraries name the task
# testDebugUnitTest; the app has no src/test in these fixtures, so testDevDebugUnitTest does not exist.
for d in none modules-compact modules-modular; do (cd $O/$d && $L gradle ./gradlew assembleDevDebug testDebugUnitTest --console=plain); done

# Cross-feature navigation survives the split (A2.4): patch a shared key into a compact copy,
# build, split, build.
G="git -c user.name=wp02 -c user.email=wp02@localhost"
rm -rf $O/crossnav && cp -r $O/modules-compact $O/crossnav && rm -rf $O/crossnav/build $O/crossnav/*/build $O/crossnav/*/*/build
(cd $O/crossnav && git init -q && git add -A && $G commit -qm base)
python3 $O/apply_crossnav_patch.py $O/crossnav
(cd $O/crossnav && $L gradle ./gradlew assembleDevDebug testDebugUnitTest --console=plain)
(cd $O/crossnav && git add -A && $G commit -qm patched)
python3 -m genkit.targets.android.modes $O/crossnav
(cd $O/crossnav && git add -A && git diff --cached -M --name-status HEAD | python3 $O/assert_moves_only.py)
(cd $O/crossnav && $L gradle ./gradlew assembleDevDebug testDebugUnitTest --console=plain)

# A forbidden edge fails the guard with the expected message (a compile or lock error must not count).
cd $O/modules-compact
cp feature/trips/build.gradle.kts $O/logs/trips.build.gradle.kts.orig
sed -i 's#dependencies {#dependencies {\n    implementation(project(":feature:orders"))#' feature/trips/build.gradle.kts
set +e; $L gradle ./gradlew :feature:trips:verifyModuleDependencies --console=plain > $O/logs/guard.log 2>&1; rc=$?; set -e
cp $O/logs/trips.build.gradle.kts.orig feature/trips/build.gradle.kts
test $rc -ne 0
grep -q ':feature:trips -> :feature:orders' $O/logs/guard.log

# verifyCodeRules fires on a planted violation.
cd $O/standard
printf 'package x\nimport androidx.compose.foundation.clickable\nfun f(m: androidx.compose.ui.Modifier) = m.clickable { }\n' > feature/settings/src/main/kotlin/Planted.kt
set +e; $L gradle ./gradlew :feature:settings:verifyCodeRules --console=plain > $O/logs/rules.log 2>&1; rc=$?; set -e
rm feature/settings/src/main/kotlin/Planted.kt
test $rc -ne 0
grep -q 'bare-clickable' $O/logs/rules.log

echo ACCEPT OK
