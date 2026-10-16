#!/usr/bin/env bash
# ktlint against the scaffold at every module-name length from 1 to 30, with and without network:
# spotlessApply must leave the scaffolded modules untouched and report nothing in them. Then the
# layouts compile and their tests pass at six lengths that between them use every layout.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
G="git -c user.name=wp02 -c user.email=wp02@localhost"
cd $OV/wt/WP02/generator
for s in s-net-0 s-net-1 s-net-2 s-local-0 s-local-1 s-local-2 c-net c-local; do
  rm -rf $O/r10-$s
  python3 create_project.py --spec $O/specs/r10-$s.json --out $O/r10-$s --no-zip --force > $O/logs/r10-gen-$s.log 2>&1 || { echo "GEN FAIL $s"; exit 1; }
  (cd $O/r10-$s && git init -q && git add -A && $G commit -qm base)
done
modules() { python3 -c "import json,sys; print(' '.join(json.load(open('$O/specs/r10-$1.json'))['feature_modules']))"; }
$OV/bin/locked gradle bash -c "
  for s in s-net-0 s-net-1 s-net-2 s-local-0 s-local-1 s-local-2; do
    cd $O/r10-\$s
    ./gradlew spotlessApply --continue --console=plain > $O/logs/r10-fit-\$s.log 2>&1 || echo spotless-\$s-exit=\$?
  done
  for s in c-net c-local; do
    cd $O/r10-\$s
    tasks=':app:assembleDevDebug'
    for m in \$(python3 -c \"import json; print(' '.join(json.load(open('$O/specs/r10-\$s.json'))['feature_modules']))\"); do
      tasks=\"\$tasks :feature:\$m:testDebugUnitTest :feature:\$m:verifyCodeRules :data:\$m:testDebugUnitTest :data:\$m:verifyCodeRules\"
    done
    ./gradlew \$tasks --continue --console=plain > $O/logs/r10-fit-\$s.log 2>&1; echo build-\$s-exit=\$?
  done
"
for s in s-net-0 s-net-1 s-net-2 s-local-0 s-local-1 s-local-2; do
  cd $O/r10-$s
  mods=$(modules $s)
  paths=""; for m in $mods; do paths="$paths feature/$m data/$m"; done
  git diff -- $paths > $O/logs/r10-fit-$s.diff
  echo "$s: $(git diff --stat -- $paths | tail -1) | lint in scaffold: $(grep -cE "^\s+(feature|data)/($(echo $mods | tr ' ' '|'))/" $O/logs/r10-fit-$s.log)"
done
echo FIT DONE
