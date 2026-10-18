#!/usr/bin/env bash
# Eight frames of a recording side by side, evenly spread over [start, end] seconds, left to right:
#   bash tiles.sh VIDEO START END OUT.png [crop w:h:x:y, recording pixels]
set -euo pipefail
VIDEO=$1; START=$2; END=$3; OUT=$4; CROP=${5:-720:1260:0:220}
STEP=$(python -I -c "print(($END - $START) / 7)")
ffmpeg -v error -y -ss "$START" -t "$(python -I -c "print($END - $START + 0.05)")" -i "$VIDEO" -vf \
  "fps=1/$STEP,crop=$CROP,scale=iw/2:-1,tile=8x1:padding=4" \
  -frames:v 1 "$OUT"
echo "TILES $OUT"
