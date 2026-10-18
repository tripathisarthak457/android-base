#!/usr/bin/env bash
# Run 23: is standard's PSS rise (41.7 MB at phase 3c, 50.4 MB now) the code or the phone? Builds the standard
# preset from the phase 3c tree (d10ef94, exported with git archive) on this host, then measures it and today's
# final-standard in one adb lock hold with the same scenarios the standard gate runs before meminfo.
#   ./ab-standard.sh > logs/r23-ab-standard.log 2>&1
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04; P=$OV/perf/WP04
SRC=$O/ab-src; OLD=$O/ab-standard-d10ef94
T=$W/templates/android/tools/perf
python3 $O/rmtree-long.py $SRC $OLD
mkdir -p $SRC
git -C $W archive d10ef94 generator templates tokens tools | tar -x -C $SRC
cd $SRC/generator
python3 create_project.py --spec tests/fixtures/v2/standard.json --set package_name=com.example.wp04.abstd \
  --out $OLD --no-zip --force > $O/logs/gen-ab-standard.log
echo "== $(date +%H:%M:%S) generated d10ef94"
cd $OLD
$L gradle ./gradlew :app:assembleDevRelease --console=plain
echo "== $(date +%H:%M:%S) built"
$L adb bash -c '
set -euo pipefail
for d in "$@"; do
  set -- $d
  if [ -e '"$P"'/$3 ]; then mv '"$P"'/$3 '"$P"'/$3-before-$(date +%H%M%S); fi
  (cd $1 && '"$T"'/perf.sh --serial 9a7c74f4 --runs 3 --out '"$P"'/$3 --package $2 --variant devRelease \
    tabs pushpop scroll_root idle coldstart meminfo) || echo "perf.sh exit $? for $3"
  echo "== $(date +%H:%M:%S) measured $3"
done' _ "$OLD com.example.wp04.abstd.dev ab-standard-d10ef94" \
  "$O/final-standard com.example.wp04.standard.dev ab-standard-7fa49d5"
echo AB DONE
