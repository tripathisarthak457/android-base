#!/usr/bin/env bash
# accept.sh on the Windows machine (native, Git Bash): paths under D:/projects/tools, Python as `python`, the generator
# tests judged against the host baseline until package WIN merges, and the phone as the only device: this machine
# has no emulator, so the emulator pass of accept.sh is dropped (handoff, Oct 17 note). accept.sh stays as run on Linux.
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP06; L=$OV/bin/locked; O=$OV/out/WP06; P=$OV/perf/WP06
S=${SERIAL:-9a7c74f4}
mkdir -p $P $O/logs

cd $W/generator
python $O/gen_tests.py .
# `python -m tests.consistency` cannot start its workers on Windows (consistency_win.py says why).
python $O/consistency_win.py --quick
python -m genkit.core.catalogue --check
python create_project.py --spec tests/fixtures/v1/v1-firebase.json --out $O/v1fb --no-zip --force --json > $O/v1fb.json
grep -q "remoteconfig" $O/v1fb.json
test ! -e $W/generator/variants/firebase-off
# Every Gradle build in one hold of the shared lock. A rerun at the same clean commit reuses the builds that passed;
# the stamps are this machine's own, apart from the Linux ones.
head=$(git -C $W rev-parse HEAD)
clean() { [ -z "$(git -C $W status --porcelain)" ]; }
stamp=$O/logs/builds-ok.win.$head
if clean && [ -e $stamp ]; then
  echo "Gradle builds passed at $head on a clean tree: $(cat $stamp)"
else
  was_clean=no; clean && was_clean=yes
  $L gradle bash $O/accept_builds.win.sh
  if [ $was_clean = yes ] && clean && [ "$(git -C $W rev-parse HEAD)" = "$head" ]; then date -Is > $stamp; fi
fi
# The badge follows a drag, keeps clear of the large title, the search field and Back, and Back under it pops
# (QA-44, K13).
bash $O/device_checks.win.sh $S
echo ACCEPT OK
