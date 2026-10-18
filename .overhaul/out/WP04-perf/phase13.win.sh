#!/usr/bin/env bash
# The Play Store block after the first frame (MainActivity.followPlayStore): `dlplay` is HEAD e2fdb08's tree with it,
# built under its own package (com.example.wp04.dlplay.dev) so it can alternate with `dl` on the phone. In one adb hold:
#  1. cold starts of the old template, dl and dlplay interleaved, 15 rounds (coldstart-ab.win.sh);
#  2. six cold starts each of dl and dlplay alternated in one capture (startup-ab-trace.win.sh): the resume's slices
#     and binder calls, stage by stage;
#  3. perf.sh's cold start rows on dl and dlplay, then dlplay and dl, three runs each.
#   ./phase13.win.sh > logs/phase13-win.log 2>&1
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP04-perf; P=$OV/perf/WP04-perf; L=$OV/bin/locked
until grep -q "^BUILT dlplay" $O/logs/build-dlplay.log 2>/dev/null; do
  grep -qE "FAILED|BUILD FAILED" $O/logs/build-dlplay.log 2>/dev/null && { echo "dlplay did not build"; exit 1; }
  sleep 20
done
echo "== $(date '+%F %H:%M:%S') phase 13"
$L adb bash -c "
KEEP=base $O/coldstart-ab.win.sh base-dl-dlplay 15 base=com.example.wp04.base.dev dl=com.example.wp04.everything.dev dlplay=com.example.wp04.dlplay.dev
$O/startup-ab-trace.win.sh $P/startab-dl-dlplay 6 com.example.wp04.everything.dev com.example.wp04.dlplay.dev
NOTRACE=1 TAG=-c3 SCENARIOS='coldstart coldstart_installed' $O/measure.win.sh dl dlplay
NOTRACE=1 TAG=-c4 SCENARIOS='coldstart coldstart_installed' $O/measure.win.sh dlplay dl
adb -s 9a7c74f4 uninstall com.example.wp04.dlplay.dev
"
echo "== $(date '+%F %H:%M:%S') PHASE13 DONE"
