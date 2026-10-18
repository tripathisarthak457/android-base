#!/usr/bin/env bash
# The builds the phone checks need, in one hold of the Gradle lock:
#   bin/locked gradle bash out/HUD/phone_builds.win.sh
# everything: the overhead (devRelease), the adb path (devDebug) and the prod release checks (prodRelease);
# standard: no analytics, so the overlay's own JankStats (devRelease); base: the same everything spec from
# wp/WP06 (no perfhud there), for "no extra thread in prod compared with the same build before this package".
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/HUD; O=$OV/out/HUD; B=$O/base-src
# A daemon that ran out of Metaspace (KSP on a busy machine, Oct 19) is retried once: the retry starts a fresh one.
gw() {
  local limit=$1; shift
  local log rc=0; log=$(mktemp)
  timeout --kill-after=2m "$limit" ./gradlew -Pandroid.builder.sdkDownload=false --console=plain "$@" 2>&1     | tee "$log" || rc=$?
  if [ $rc -ne 0 ] && grep -q "OutOfMemoryError: Metaspace" "$log"; then
    rm -f "$log"; echo "RETRY: the daemon ran out of Metaspace"
    timeout --kill-after=2m "$limit" ./gradlew -Pandroid.builder.sdkDownload=false --console=plain "$@"; return
  fi
  rm -f "$log"; return $rc
}
# wp/WP06 as committed: its worktree may hold another run's uncommitted edits.
rm -rf "$B" && mkdir -p "$B"
git -C D:/projects/tools/android-base archive --format=tar wp/WP06 generator templates tokens | tar -x -C "$B"
cd $W/generator
python create_project.py --spec $O/specs/hud-everything.json --out $O/everything --no-zip --force
python create_project.py --spec $O/specs/hud-standard.json --out $O/standard --no-zip --force
(cd $B/generator && python create_project.py --spec $O/specs/hud-everything-base.json --out $O/base --no-zip --force)
(cd $O/everything && gw 50m :app:assembleDevRelease :app:assembleProdRelease :app:assembleDevDebug)
(cd $O/standard && gw 30m :app:assembleDevRelease)
(cd $O/base && gw 40m :app:assembleProdRelease)
echo BUILDS OK
