#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP06; L=$OV/bin/locked; O=$OV/out/WP06; P=$OV/perf/WP06
# The device checks run on the Redmi; SERIAL=emulator-5554 runs them on the periflow_api35 emulator, booted for
# the purpose, when the phone is missing (correctness only).
S=${SERIAL:-9a7c74f4}
mkdir -p $P

cd $W/generator
python3 -m unittest discover -s tests -t .
python3 -m tests.consistency --quick
python3 -m genkit.core.catalogue --check
python3 create_project.py --spec tests/fixtures/v1/v1-firebase.json --out $O/v1fb --no-zip --force --json > $O/v1fb.json
grep -q "remoteconfig" $O/v1fb.json
test ! -e $W/generator/variants/firebase-off
# The template's build and tests, check + assembleDevDebug of standard, everything and standard-nofirebase, and
# the two devRelease APKs the device checks install, all in one hold of the shared Gradle lock. A rerun at the
# same clean commit reuses the builds that passed.
head=$(git -C $W rev-parse HEAD)
clean() { [ -z "$(git -C $W status --porcelain)" ]; }
stamp=$O/logs/builds-ok.$head
if clean && [ -e $stamp ]; then
  echo "Gradle builds passed at $head on a clean tree: $(cat $stamp)"
else
  was_clean=no; clean && was_clean=yes
  $L gradle bash $O/accept_builds.sh
  if [ $was_clean = yes ] && clean && [ "$(git -C $W rev-parse HEAD)" = "$head" ]; then date -Is > $stamp; fi
fi
# The badge follows a drag, keeps clear of the large title and Back, and Back under it pops (QA-44).
if [ "$S" = emulator-5554 ]; then
  $L emu bash $O/with_emulator.sh bash $O/device_checks.sh $S
else
  bash $O/device_checks.sh $S
fi
echo ACCEPT OK
