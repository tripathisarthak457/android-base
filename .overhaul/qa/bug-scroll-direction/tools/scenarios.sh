#!/usr/bin/env bash
# Every scroll case of the scroll-direction bug, recorded on the phone, on the WP03 look catalog
# (com.example.wp03.look.catalog). Run under the adb lock with the phone unlocked:
#   bin/locked adb bash qa/bug-scroll-direction/tools/scenarios.sh <out-dir> [scenario...]
# RTL uses the catalog's own launch option (--ez rtl true), which sets the layout direction inside the app and
# changes no device setting. Analysis (track.py) runs separately: analyse.sh <out-dir>.
set -euo pipefail
OUT=$1; shift
S=9a7c74f4; A="adb -s $S"; PKG=com.example.wp03.look.catalog
T=$(cd "$(dirname "$0")" && pwd -W 2>/dev/null || pwd)
REC="python -I $T/rec.py --serial $S --out $OUT"
mkdir -p "$OUT"

launch() { # section [rtl]
  $A shell am force-stop $PKG
  $A shell am start -W -n $PKG/.CatalogActivity --es section "$1" ${2:+--ez rtl true} > /dev/null
  sleep 2.5
}

# One finger held down through a list of points, as a single gesture: drag x1,y1 to x2,y2 and back.
there_and_back() { # x1 y1 x2 y2 steps
  local x1=$1 y1=$2 x2=$3 y2=$4 n=$5 cmd i
  cmd="input motionevent DOWN $x1 $y1"
  for ((i = 1; i <= n; i++)); do cmd+="; input motionevent MOVE $((x1 + (x2 - x1) * i / n)) $((y1 + (y2 - y1) * i / n))"; done
  for ((i = n - 1; i >= 0; i--)); do cmd+="; input motionevent MOVE $((x1 + (x2 - x1) * i / n)) $((y1 + (y2 - y1) * i / n))"; done
  cmd+="; input motionevent UP $x1 $y1"
  echo "sh:$cmd"
}

FAST_RL="swipe:900,675,200,675,120"; SLOW_RL="swipe:900,675,200,675,700"
FAST_LR="swipe:200,675,900,675,120"; SLOW_LR="swipe:200,675,900,675,700"

carousel_fast() { launch Collections; $REC --name carousel_fast --seconds 13 -- $FAST_RL sleep:1.8 $FAST_RL sleep:1.8 $FAST_RL sleep:1.8 $FAST_RL sleep:1.8 $FAST_RL sleep:1.8; }
carousel_slow() { launch Collections; $REC --name carousel_slow --seconds 15 -- $SLOW_RL sleep:1.5 $SLOW_RL sleep:1.5 $SLOW_RL sleep:1.5 $SLOW_RL sleep:1.5 $SLOW_RL sleep:1.5; }
# A second swipe while the first is still settling: the case where a stretch left by one swipe met the next.
carousel_quick() { launch Collections; $REC --name carousel_quick --seconds 10 -- $FAST_RL sleep:0.35 $FAST_LR sleep:0.35 $FAST_RL sleep:0.35 $FAST_RL sleep:2.5; }
carousel_edge_back() { launch Collections; $REC --name carousel_edge_back --seconds 8 -- "$(there_and_back 300 675 700 675 8)" sleep:2; }
carousel_rtl_fast() { launch Collections rtl; $REC --name carousel_rtl_fast --seconds 13 -- $FAST_LR sleep:1.8 $FAST_LR sleep:1.8 $FAST_LR sleep:1.8 $FAST_LR sleep:1.8 $FAST_LR sleep:1.8; }
carousel_rtl_slow() { launch Collections rtl; $REC --name carousel_rtl_slow --seconds 15 -- $SLOW_LR sleep:1.5 $SLOW_LR sleep:1.5 $SLOW_LR sleep:1.5 $SLOW_LR sleep:1.5 $SLOW_LR sleep:1.5; }
carousel_rtl_edge_back() { launch Collections rtl; $REC --name carousel_rtl_edge_back --seconds 8 -- "$(there_and_back 780 675 380 675 8)" sleep:2; }

FLING_UP="swipe:540,1700,540,1000,90"; FLING_DOWN="swipe:540,1000,540,1700,90"
DRAG_UP="swipe:540,1700,540,1100,800"; DRAG_DOWN="swipe:540,1100,540,1700,800"
# The user's sequence: flings to both ends, then a slow drag up and a slow drag down in the middle.
list_mid() { launch Lists ${1:-}; $REC --name list_mid${1:+_rtl} --seconds 15 -- $FLING_UP sleep:3.5 $FLING_DOWN sleep:3.5 $DRAG_UP sleep:2 $DRAG_DOWN sleep:2; }
# Flings that stop in the middle of the page, both ways, after a drag away from the top.
list_flings() { launch Lists ${1:-}; $REC --name list_flings${1:+_rtl} --seconds 16 -- $DRAG_UP sleep:2.5 "swipe:540,1500,540,1100,150" sleep:3.5 "swipe:540,1100,540,1500,150" sleep:3.5 $DRAG_UP sleep:0.5 $DRAG_UP sleep:2.5; }
# Pulled past the top, then back without lifting the finger; then the same past the bottom.
list_edge_back() {
  launch Lists ${1:-}
  $REC --name list_edge_back_top${1:+_rtl} --seconds 8 -- "$(there_and_back 540 900 540 1500 10)" sleep:2
  $A shell input swipe 540 1700 540 900 80; sleep 3; $A shell input swipe 540 1700 540 900 80; sleep 4
  $REC --name list_edge_back_bottom${1:+_rtl} --seconds 8 -- "$(there_and_back 540 1500 540 900 10)" sleep:2
}
list_rtl() { list_mid rtl; list_flings rtl; list_edge_back rtl; }

# Let go with the band still out and heading back towards the content: thrown into an end, the band is caught on its
# way back by a quick flick back that travels less than the band's stretch, so the finger lifts with the band still
# out. Three throws each, the flick 0.05, 0.15 and 0.25 s after the throw, since the input tool's start-up varies.
catch_steps() { # away-swipe throw-in flick-back
  local pause
  for pause in 0.05 0.15 0.25; do
    steps+=("$1" "sleep:1.5" "sh:input swipe $2; sleep $pause; input swipe $3" "sleep:2.5")
  done
}
# 500 px from the top, thrown into it, caught with a quick flick up.
list_catch_top() { # [rtl]
  launch Lists ${1:-}
  local steps=()
  catch_steps "swipe:540,1600,540,1100,900" "540 900 540 1300 50" "540 1400 540 1320 20"
  $REC --name list_catch_top${1:+_rtl} --seconds 18 -- "${steps[@]}"
}
# 500 px from the bottom, thrown into it, caught with a quick flick down.
list_catch_bottom() { # [rtl]
  launch Lists ${1:-}
  local i steps=()
  for i in 1 2 3 4 5; do $A shell input swipe 540 1700 540 900 80; sleep 2.5; done
  catch_steps "swipe:540,1100,540,1600,900" "540 1300 540 900 50" "540 1000 540 1080 20"
  $REC --name list_catch_bottom${1:+_rtl} --seconds 18 -- "${steps[@]}"
}
# On the last card, swiped on past it and caught on the way back with a quick flick towards the earlier cards.
carousel_catch_end() { # [rtl]
  launch Collections ${1:-}
  local i onward="900 675 200 675 120" in="900 675 300 675 60" back="400 675 480 675 20"
  if [ -n "${1:-}" ]; then onward="200 675 900 675 120"; in="180 675 780 675 60"; back="680 675 600 675 20"; fi
  for i in 1 2 3 4 5; do $A shell input swipe $onward; sleep 1.5; done
  local pause steps=()
  for pause in 0.05 0.15 0.25; do steps+=("sh:input swipe $in; sleep $pause; input swipe $back" "sleep:2.5"); done
  $REC --name carousel_catch_end${1:+_rtl} --seconds 11 -- "${steps[@]}"
}
catch_all() { list_catch_top; list_catch_top rtl; list_catch_bottom; list_catch_bottom rtl; carousel_catch_end; carousel_catch_end rtl; }

all=(carousel_fast carousel_slow carousel_quick carousel_edge_back carousel_rtl_fast carousel_rtl_slow carousel_rtl_edge_back list_mid list_flings list_edge_back list_rtl)
for scenario in "${@:-${all[@]}}"; do
  echo "== $(date '+%H:%M:%S') $scenario"
  $scenario
done
$A shell am force-stop $PKG
echo "SCENARIOS DONE"
