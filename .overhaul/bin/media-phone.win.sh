#!/usr/bin/env bash
# The images-and-video package's phone work (media.md section 4, lists-images.md 2.4, 3.4, 5.4), for the gate step.
# Opus wrote it; a Sonnet operator runs it and returns facts and paths; an Opus verifier re-derives every number from
# the raw dumps under perf/media/. Builds and phone steps take their own locks, at low priority while staging is open:
#
#   media-phone.win.sh build <name> [media-arms options]   generate Everything from the MEDIA branch, apply the arm,
#                                                           build the catalog's release APK -> perf/media/apks/<name>.apk
#   media-phone.win.sh build-staging                       the same from staging's head, no arm -> apks/A0.apk
#   media-phone.win.sh smoke                               install B, open each scene, capture what L3 needs; no gate
#   media-phone.win.sh session1                            image A/B (media.md 4.5): N A B B-P B+F, both trace controls
#   media-phone.win.sh session2                            video arms (4.6), then both columns' acceptance (3 runs)
#   media-phone.win.sh sheets                              District P3: held presses on the six list sheets, A/B
#   media-phone.win.sh build-lists                         the app's devBenchmarkRelease APKs of P3 (B0 B4 B2 B1, the
#                                                           bench patch) and P7 (A from staging, B from this branch)
#   media-phone.win.sh p3 | p7                             lists-images.md 2.4 and 3.4 on the app (media-lists.py)
#
# smoke takes the adb lock once for all its steps, and sheets once per sheet: a press must land on the build this
# script just opened, never on whatever another session's command installed between two of its adb calls.
# Before session 1: `build N --arm N`, `build A --arm A`, `build B --arm B`, `build BP --arm B-P`, `build BF --arm B+F`,
# `build BS --arm B-S`, `build BHW0 --arm B-HW0`. Before session 2: `build V2 --arm B --pool 2`, `build V3b0 --arm B
# --beyond 0`, `build VT --arm B --texture` (and `build VF --arm B --frame-rate-off` only if 4.6 asks for it).
# Logs go to logs/media-phone.log; device files only under /data/local/tmp. perf.py restores the settings it changes.
set -uo pipefail
OV=D:/projects/tools/.overhaul
WT=$OV/wt/MEDIA
OUT=$OV/perf/media
GEN=$OV/out/media/gen
PKG=com.example.everything.catalog
LOG=$OV/logs/media-phone.log
export LOCKED_PRIORITY=low
mkdir -p "$OUT/apks" "$GEN"
say() { echo "[$(date '+%F %T')] $*" | tee -a "$LOG"; }

generate() { # <generator dir> <out dir>
  rm -rf "$2"
  (cd "$1" && python create_project.py --spec tests/fixtures/v2/everything.json --out "$2" --no-zip --force) >> "$LOG" 2>&1
}

build_catalog() { # <project dir> <apk name>
  (cd "$1" && "$OV/bin/locked" gradle ./gradlew --console=plain :catalog:assembleRelease) >> "$LOG" 2>&1 || return 1
  cp "$1/catalog/build/outputs/apk/release/catalog-release.apk" "$OUT/apks/$2.apk"
  say "built $OUT/apks/$2.apk"
}

build_app() { # <project dir> <apk name>
  (cd "$1" && "$OV/bin/locked" gradle ./gradlew --console=plain :app:assembleDevBenchmarkRelease) >> "$LOG" 2>&1 || return 1
  cp "$1/app/build/outputs/apk/dev/benchmarkRelease/app-dev-benchmarkRelease.apk" "$OUT/apks/$2.apk"
  say "built $OUT/apks/$2.apk"
}

staging_source() { # staging's generator and template, as of its head, into $GEN/staging-src
  rm -rf "$GEN/staging-src"; mkdir -p "$GEN/staging-src"
  git -C "$WT" archive staging generator templates | tar -x -C "$GEN/staging-src"
  say "staging-src at $(git -C "$WT" rev-parse --short staging)"
}

bench() { # <project dir> [--decodes N]: P3's patch, with ffmpeg on PATH from memwait
  "$OV/bin/memwait" python -I "$OV/bin/media-lists.py" bench "$@" >> "$LOG" 2>&1 || { say "bench patch failed on $1"; return 1; }
}

perf() { # <out dir> <apk name> perf.py arguments...
  local dir=$1 apk=$2; shift 2
  "$OV/bin/locked" adb python "$GEN/B/tools/perf/perf.py" --package "$PKG" --apk "$OUT/apks/$apk.apk" --out "$dir" "$@" \
    >> "$LOG" 2>&1
  say "perf $apk $* -> $dir (exit $?)"
}

adbq() { "$OV/bin/locked" adb adb "$@"; }
holds_adb() { case ",${LOCKED_HELD:-}" in *,adb:*) return 0 ;; esac; return 1; }

case "${1:-}" in
  build)
    name=$2; shift 2
    generate "$WT/generator" "$GEN/$name" || exit 1
    python -I "$OV/bin/media-arms.py" "$GEN/$name" "$@" >> "$LOG" 2>&1 || { say "arm $* did not apply"; exit 1; }
    build_catalog "$GEN/$name" "$name"
    ;;
  build-staging)
    staging_source
    generate "$GEN/staging-src/generator" "$GEN/A0" && build_catalog "$GEN/A0" A0
    ;;
  build-lists)
    # P3's arms differ only in the loader: B0 is staging's (Coil's defaults), B4/B2/B1 this branch's at 4, 2, 1
    # decodes at once. P7's arms differ only in the feed's paging (staging 10/3/10, this branch 30/20/30).
    staging_source
    generate "$GEN/staging-src/generator" "$GEN/P3-B0" && bench "$GEN/P3-B0" && build_app "$GEN/P3-B0" P3-B0
    for n in 4 2 1; do
      generate "$WT/generator" "$GEN/P3-B$n" && bench "$GEN/P3-B$n" --decodes $n && build_app "$GEN/P3-B$n" P3-B$n
    done
    generate "$GEN/staging-src/generator" "$GEN/P7-A" && build_app "$GEN/P7-A" P7-A
    generate "$WT/generator" "$GEN/P7-B" && build_app "$GEN/P7-B" P7-B
    ;;
  p3)
    # The harness of a project generated from this branch: its frame-stats code is the gates' own.
    "$OV/bin/locked" adb python -I "$OV/bin/media-lists.py" p3 --harness "$GEN/P3-B2/tools/perf" --out "$OUT/p3" \
      --apk B0="$OUT/apks/P3-B0.apk" --apk B4="$OUT/apks/P3-B4.apk" --apk B2="$OUT/apks/P3-B2.apk" \
      --apk B1="$OUT/apks/P3-B1.apk" >> "$LOG" 2>&1
    say "p3 -> $OUT/p3 (exit $?)"
    ;;
  p7)
    "$OV/bin/locked" adb python -I "$OV/bin/media-lists.py" p7 --harness "$GEN/P7-B/tools/perf" --out "$OUT/p7" \
      --apk A="$OUT/apks/P7-A.apk" --apk B="$OUT/apks/P7-B.apk" >> "$LOG" 2>&1
    say "p7 -> $OUT/p7 (exit $?)"
    ;;
  smoke)
    # One install of B: every scene opens and loads, the scenes log, the GC hook answers, and the phone's codec
    # dump is captured for the video pack's parser (L3) before anyone trusts codecs_max.
    holds_adb || exec "$OV/bin/locked" adb bash "$0" smoke
    dir=$OUT/smoke/$(date +%Y%m%d-%H%M); mkdir -p "$dir"
    adbq install -r -t "$OUT/apks/B.apk" > "$dir/install.txt" 2>&1
    grep -q Success "$dir/install.txt" || { say "install of B failed: $dir/install.txt"; exit 1; }
    adbq shell pm clear $PKG > /dev/null
    adbq shell dumpsys package com.android.shell > "$dir/shell-package.txt"
    for section in Feed Shop Reels; do
      adbq logcat -c
      adbq shell am start -W -S -n $PKG/com.example.everything.catalog.CatalogActivity --es section $section --ez perfLog true \
        > "$dir/$section-start.txt"
      sleep 8
      adbq shell input swipe 540 1700 540 700 150; sleep 3
      adbq shell screencap -p /data/local/tmp/media-smoke.png && adbq pull /data/local/tmp/media-smoke.png "$dir/$section.png" > /dev/null
      adbq shell am broadcast -a $PKG.PERF_GC -p $PKG > "$dir/$section-gc.txt"; sleep 2
      adbq shell dumpsys media.resource_manager > "$dir/$section-resource_manager.txt"
      adbq shell "ps -T -p \$(pidof $PKG)" > "$dir/$section-threads.txt"
      adbq logcat -d -v brief -s perf.media:I AndroidRuntime:E > "$dir/$section-log.txt"
    done
    adbq shell rm -f /data/local/tmp/media-smoke.png
    adbq shell "cat /vendor/etc/media_codecs*.xml" > "$dir/media_codecs.xml"
    # Left running, the catalog would hold its players and threads through the next session's measurements.
    adbq shell am force-stop $PKG
    say "smoke -> $dir"
    ;;
  session1)
    s=$OUT/s1
    # Cells alternate arms so heat and battery drift spread evenly; one run per invocation, three rounds.
    for round in 1 2 3; do
      for arm in N A B BP BF; do perf "$s/feed-cold/$arm-$round" $arm --runs 1 media_feed; done
      for arm in N A B BF; do perf "$s/feed-warm/$arm-$round" $arm --runs 1 media_feed_warm; done
      for arm in A B BP; do perf "$s/grid-cold/$arm-$round" $arm --runs 1 media_grid; done
      for arm in A B; do perf "$s/grid-warm/$arm-$round" $arm --runs 1 media_grid_warm; done
      for arm in A B; do perf "$s/memory/$arm-$round" $arm --runs 1 media_feed_memory; done
    done
    perf "$s/trace/B" B --runs 1 media_trace
    perf "$s/trace/BHW0" BHW0 --runs 1 media_trace
    MEDIA_SIZES_FROM="$s/trace/B/run1" perf "$s/trace/BS" BS --runs 1 media_trace
    ;;
  session2)
    s=$OUT/s2
    for round in 1 2 3; do
      for arm in B V2 V3b0; do
        perf "$s/reels-cold/$arm-$round" $arm --runs 1 media_reels
        perf "$s/reels-warm/$arm-$round" $arm --runs 1 media_reels_warm
      done
      perf "$s/reels-warm/VT-$round" VT --runs 1 media_reels_warm
    done
    [ -f "$OUT/apks/VF.apk" ] && for round in 1 2 3; do perf "$s/reels-warm/VF-$round" VF --runs 1 media_reels_warm; done
    perf "$s/accept-images" B --runs 3 --gate images media_feed media_feed_warm media_grid media_grid_warm media_feed_memory media_trace
    perf "$s/accept-video" B --runs 3 --gate video media_reels media_reels_warm media_background
    ;;
  sheets)
    # District P3 (lists-images.md 5.4): each open is a held press, `input swipe x y x y 120`, on the trigger's test
    # tag; arms by launch extra in one build (B), and A0 (staging's catalog) once per sheet. gfxinfo framestats per open.
    s=$OUT/sheets/$(date +%Y%m%d-%H%M); mkdir -p "$s"
    for sheet in filters basic choice choice_multi; do
      "$OV/bin/locked" adb bash "$0" sheet-one "$s" "$sheet" || say "sheet $sheet stopped"
    done
    say "sheets -> $s (the light theme and Snappy variants of Filters: rerun with --es theme light / --es motion Snappy)"
    ;;
  sheet-one)
    # Called by sheets under the adb lock: every open and press of one sheet, A/B, the tap and A0.
    holds_adb || { say "sheet-one runs only under sheets' adb lock"; exit 2; }
    s=$2; sheet=$3
    open_page() { # <apk> <extras>
      adbq install -r -t "$OUT/apks/$1.apk" >> "$LOG" 2>&1
      adbq shell cmd package compile -m speed-profile -f $PKG > /dev/null
      adbq shell am start -W -S -n $PKG/com.example.everything.catalog.CatalogActivity --es section Sheets $2 > /dev/null
      sleep 3
    }
    press() { # <tag> <out file> <gesture: hold|tap>
      adbq shell uiautomator dump /data/local/tmp/media-ui.xml > /dev/null
      local bounds; bounds=$(adbq shell cat /data/local/tmp/media-ui.xml | python -I -c "
import re,sys
m=re.search(r'resource-id=\"$1\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"', sys.stdin.read())
print(f'{(int(m[1])+int(m[3]))//2} {(int(m[2])+int(m[4]))//2}' if m else '')")
      [ -z "$bounds" ] && { say "no $1 on screen"; return 1; }
      set -- "$@" $bounds
      adbq shell dumpsys gfxinfo $PKG reset > /dev/null
      if [ "$3" = hold ]; then adbq shell input swipe $4 $5 $4 $5 120; else adbq shell input tap $4 $5; fi
      sleep 1.5
      adbq shell dumpsys gfxinfo $PKG framestats > "$2"
      adbq shell input keyevent KEYCODE_BACK; sleep 1.2
    }
    for run in 1 2 3; do
      for arm in A B; do
        extras=$([ $arm = A ] && echo "--ez prepareSheets false" || echo "")
        open_page B "$extras"
        press catalog_sheet_$sheet "$s/$sheet-$arm-$run.txt" hold
      done
    done
    open_page B ""
    press catalog_sheet_$sheet "$s/$sheet-B-tap.txt" tap
    [ -f "$OUT/apks/A0.apk" ] && { open_page A0 ""; press catalog_sheet_$sheet "$s/$sheet-A0.txt" hold; }
    adbq shell rm -f /data/local/tmp/media-ui.xml
    adbq shell am force-stop $PKG
    ;;
  *)
    sed -n '2,20p' "$0"; exit 2 ;;
esac
