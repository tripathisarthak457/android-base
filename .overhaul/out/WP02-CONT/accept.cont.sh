#!/usr/bin/env bash
# WP02's acceptance (out/WP02/accept.win.sh) for stage WP02-CONT, on the lane worktree: the template's build, the
# generator suite, the consistency matrix, the catalogue and token checks, five generated projects built and tested
# (the adaptive audit included), the cross-feature key surviving the modular split, and the two guards firing on a
# planted violation. Unlike accept.win.sh it runs every step and lists what failed; the log must end with ACCEPT OK.
#   bash accept.cont.sh > logs/accept.log 2>&1      (in the background)
set -uo pipefail
export LOCKED_PRIORITY=low
OV=D:/projects/tools/.overhaul
W=$OV/wt/sw-WP02
L=$OV/bin/locked
M=$OV/bin/memwait
O=$OV/out/WP02-CONT/accept
LOGS=$OV/out/WP02-CONT/logs/accept
GW="bash $OV/out/WP02/gw.win.sh"
G="git -c user.name=wp02 -c user.email=wp02@localhost -c core.autocrlf=false"
mkdir -p $O $LOGS
failed=()
run() { # <name> <dir> <command...>
  local name=$1 dir=$2; shift 2
  echo "$(date +%H:%M:%S) start $name"
  (cd $dir && "$@") > $LOGS/$name.log 2>&1
  local rc=$?
  echo "$(date +%H:%M:%S) $name exit=$rc"
  [ $rc -eq 0 ] || failed+=("$name")
  return $rc
}
echo "HEAD $(git -C $W rev-parse HEAD); changes: $(git -C $W status --porcelain | wc -l)"

T=$W/templates/android
run template-build $T timeout 120m $L gradle $GW build --console=plain
# The baseline-profile consumer still configures after the variant trim.
run baseline-dry-run $T $L gradle $GW :app:generateBaselineProfile --dry-run --console=plain

run generator-suite $W/generator $M python -B -m unittest discover -s tests -t .
run consistency-quick $W/generator $M python -B -m tests.consistency --quick
run catalogue-check $W/generator python -B -m genkit.core.catalogue --check
run tokens-check $W/generator python -B -m genkit.tokens --check

for spec in standard standard-modular none modules-compact modules-modular; do
  rm -rf $O/$spec
  run gen-$spec $W/generator python -B create_project.py --spec tests/fixtures/v2/$spec.json --out $O/$spec --no-zip --force
done
for d in standard standard-modular; do run build-$d $O/$d $L gradle $GW check assembleDevDebug --console=plain; done
# Library unit tests are testDebugUnitTest; the app's (the adaptive audit over the scaffolded screens) is per flavour.
for d in none modules-compact modules-modular; do
  run build-$d $O/$d $L gradle $GW assembleDevDebug testDebugUnitTest :app:testDevDebugUnitTest --console=plain
done

# Cross-feature navigation survives the split (A2.4): a shared key patched into a compact copy, built, split, built.
rm -rf $O/crossnav && cp -r $O/modules-compact $O/crossnav
rm -rf $O/crossnav/.gradle $O/crossnav/build-logic/.gradle $O/crossnav/build $O/crossnav/*/build $O/crossnav/*/*/build
run crossnav-base $O/crossnav bash -c "$G init -q && $G add -A && $G commit -qm base"
run crossnav-patch $O/crossnav python $OV/out/WP02/apply_crossnav_patch.py $O/crossnav
run crossnav-build $O/crossnav $L gradle $GW assembleDevDebug testDebugUnitTest --console=plain
run crossnav-commit $O/crossnav bash -c "$G add -A && $G commit -qm patched"
run crossnav-split $W/generator python -B -m genkit.targets.android.modes $O/crossnav
run crossnav-moves $O/crossnav bash -c "$G add -A && $G diff --cached -M --name-status HEAD | python $OV/out/WP02/assert_moves_only.py"
run crossnav-split-build $O/crossnav $L gradle $GW assembleDevDebug testDebugUnitTest --console=plain

# A forbidden edge fails the guard with the expected message (a compile or lock error must not count).
P=$O/modules-compact/feature/trips/build.gradle.kts
cp $P $LOGS/trips.build.gradle.kts.orig
sed -i 's#dependencies {#dependencies {\n    implementation(project(":feature:orders"))#' $P
(cd $O/modules-compact && $L gradle $GW :feature:trips:verifyModuleDependencies --console=plain) > $LOGS/guard.log 2>&1; rc=$?
cp $LOGS/trips.build.gradle.kts.orig $P
if [ $rc -ne 0 ] && grep -q ':feature:trips -> :feature:orders' $LOGS/guard.log; then echo "guard fires: ok"; else failed+=(guard); echo "guard: FAILED"; fi

# verifyCodeRules fires on a planted violation.
printf 'package x\nimport androidx.compose.foundation.clickable\nfun f(m: androidx.compose.ui.Modifier) = m.clickable { }\n' > $O/standard/feature/settings/src/main/kotlin/Planted.kt
(cd $O/standard && $L gradle $GW :feature:settings:verifyCodeRules --console=plain) > $LOGS/rules.log 2>&1; rc=$?
rm $O/standard/feature/settings/src/main/kotlin/Planted.kt
if [ $rc -ne 0 ] && grep -q 'bare-clickable' $LOGS/rules.log; then echo "code rules fire: ok"; else failed+=(rules); echo "rules: FAILED"; fi

echo "git status after: $(git -C $W status --porcelain | wc -l) changed paths"
if [ ${#failed[@]} -eq 0 ]; then echo "ACCEPT OK $(date +%H:%M:%S)"; else echo "ACCEPT FAILED: ${failed[*]}"; fi
