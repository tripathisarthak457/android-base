#!/usr/bin/env bash
# The visual half of DESIGN.md §5.6 over the phone session's files, with ffmpeg only:
#   1. held.png (the card held at 45 %): SSIM and PSNR of three crops, every build against bg-tex and bg-head, and
#      6x nearest-neighbour enlargements of the crops and of the card's four corners for looking at;
#   2. gesture.mp4 (rec-gesture's commit): every composed frame (passthrough), and the mean difference of each from
#      the one before, so the frame after the finger lifts shows whether it already fades or repeats the pose;
#   3. cancel.mp4 frames (rec-cancel): the same differences at the end of the cancel (a flash is a spike there);
#   4. both recordings again on the card alone (CARD), for the four frames after the hold.
# Crops are W:H:X:Y in the 1080 x 2400 screenshot, chosen once from bg-head's shot: TEXT1 a title line, TEXT2 a body
# line, ICON an icon, all inside the card; CORNERS the card's four corners (from the head shot's card bounds). Without
# TEXT1 the held-card section is skipped. CARD is W:H:X:Y in the 720 x 1600 recordings; the default is the held card
# right of the system's back arrow and above the tab bar.
# A difference is the mean luma change (0-255), printed as ffmpeg gives it, exponent included: 6.08e-06 is 0.00000608.
#   TEXT1=... TEXT2=... ICON=... CORNERS="tl tr bl br" ./bg-visual.sh > $O/back-google/visual.txt
# The Oct 19 run: TEXT1=680:62:110:452 TEXT2=880:56:108:768 ICON=60:60:110:235
#   CORNERS="70:70:66:96 70:70:995:96 70:70:66:2225 70:70:995:2225" (CARD the default).
set -uo pipefail
P=D:/projects/tools/.overhaul/perf/WP04-perf; V=D:/projects/tools/.overhaul/out/WP04-perf/back-google/visual
mkdir -p $V
builds="bg-head bg-tex bg-c1 bg-c2"
crop() { ffmpeg -v error -y -i "$1" -vf "crop=$2,scale=iw*6:ih*6:flags=neighbor" "$3"; }
cmp() {  # cmp <a.png> <b.png> <crop> -> "ssim psnr"
  local s p
  s=$(ffmpeg -v info -i "$1" -i "$2" -lavfi "[0]crop=$3[a];[1]crop=$3[b];[a][b]ssim" -f null - 2>&1 | grep -o 'All:[0-9.]*' | cut -d: -f2)
  p=$(ffmpeg -v info -i "$1" -i "$2" -lavfi "[0]crop=$3[a];[1]crop=$3[b];[a][b]psnr" -f null - 2>&1 | grep -o 'average:[0-9a-z.]*' | cut -d: -f2)
  echo "$s $p"
}
# diffs <video> [crop]: "time:difference" of each composed frame from the one before, on one line
diffs() {
  local vf="tblend=all_mode=difference,signalstats,metadata=print:key=lavfi.signalstats.YAVG"
  [ -n "${2:-}" ] && vf="crop=$2,$vf"
  ffmpeg -v info -i "$1" -fps_mode passthrough -vf "$vf" -f null - 2>&1 \
    | grep -oE 'pts_time:[0-9.]+|YAVG=[0-9.]+(e[-+]?[0-9]+)?' | paste - - | awk '{split($1,a,":"); split($2,b,"="); printf "%s:%s ", a[2], b[2]} END {print ""}'
}
# after_hold: of diffs' line, the four frames from the first after the last pause of over 0.3 s that more frames
# follow: the lift, after the finger rested (a lone frame after the motion, such as the clock's, is not the lift)
after_hold() {
  tr ' ' '\n' | awk -F: 'NF == 2 { t[++n] = $1; d[n] = $2 }
    END { g = 0; for (i = 2; i <= n - 2; i++) if (t[i] - t[i-1] > 0.3) g = i
          if (g == 0) { print "no pause"; exit }
          for (i = g; i < g + 4 && i <= n; i++) printf "%s:%s ", t[i], d[i]; print "" }'
}
CARD=${CARD:-570:1350:130:90}
echo "== crops: TEXT1=${TEXT1:-} TEXT2=${TEXT2:-} ICON=${ICON:-} CORNERS=\"${CORNERS:-}\" CARD=$CARD"
echo "== held.png crops: SSIM PSNR(dB) against bg-tex, then against bg-head"
[ -n "${TEXT1:-}" ] || echo "(skipped: no TEXT1, TEXT2, ICON and CORNERS given)"
[ -n "${TEXT1:-}" ] && for n in $builds; do
  for c in TEXT1 TEXT2 ICON; do
    r=${!c}
    printf '%-8s %-6s vs tex: %-24s vs head: %s\n' $n $c "$(cmp $P/$n-held/held.png $P/bg-tex-held/held.png $r)" "$(cmp $P/$n-held/held.png $P/bg-head-held/held.png $r)"
    crop $P/$n-held/held.png $r $V/$n-$c.png
  done
  i=0
  for r in $CORNERS; do i=$((i + 1)); crop $P/$n-held/held.png $r $V/$n-corner$i.png; done
done
echo "== commit recording: mean luma difference of each composed frame from the one before (frame time s: diff)"
for n in $builds; do
  rec=$P/$n-rec/gesture.mp4
  [ -f $rec ] || { echo "$n: no recording"; continue; }
  if [ ! -f $P/$n-rec/all/f0001.png ]; then
    mkdir -p $P/$n-rec/all
    ffmpeg -v fatal -y -i $rec -fps_mode passthrough $P/$n-rec/all/f%04d.png
  fi
  echo "-- $n ($(ls $P/$n-rec/all | wc -l) frames)"
  diffs $rec
done
echo "== cancel recording: the same"
for n in $builds; do
  rec=$P/$n-cancel/cancel.mp4
  [ -f $rec ] || { echo "$n: no recording"; continue; }
  echo "-- $n ($(ls $P/$n-cancel/frames | wc -l) frames)"
  diffs $rec
done
echo "== the card alone ($CARD in the recording): the first four composed frames after the hold, commit then cancel"
for n in $builds; do
  for r in $P/$n-rec/gesture.mp4 $P/$n-cancel/cancel.mp4; do
    [ -f $r ] || continue
    printf '%-8s %-8s %s\n' $n "$(basename $r .mp4)" "$(diffs $r $CARD | after_hold)"
  done
done
echo "== GPU memory (Total GPU memory usage, then its purgeable line) at each point"
for n in $builds; do
  for f in before held after1 after35 edgehold edgeafter; do
    g=$P/$n-held/gfx-$f.txt
    [ -f $g ] || continue
    printf '%-8s %-9s %s | %s\n' $n $f "$(grep -A1 'Total GPU memory usage' $g | tail -1 | tr -s ' ')" "$(grep -iE 'purgeable' $g | head -2 | tr -s ' ' | tr '\n' ';')"
  done
done
echo "== meminfo: Graphics and GL mtrack (kB) at each point"
for n in $builds; do
  for f in before held after1 after35; do
    m=$P/$n-held/mem-$f.txt
    [ -f $m ] || continue
    printf '%-8s %-8s %s | %s | %s\n' $n $f "$(grep -E '^\s*Graphics:' $m | tr -s ' ')" "$(grep -E '^\s*GL mtrack' $m | tr -s ' ')" "$(grep -E 'TOTAL PSS:|^\s*TOTAL ' $m | head -1 | tr -s ' ')"
  done
done
