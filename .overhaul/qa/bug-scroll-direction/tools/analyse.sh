#!/usr/bin/env bash
# Motion runs of every recording in <out-dir> (scenarios.sh): NAME.track.tsv per frame, NAME.runs.txt, and
# runs.md with the runs of 10 px or more.   bash analyse.sh <out-dir>
set -euo pipefail
OUT=$1
T=$(cd "$(dirname "$0")" && pwd -W 2>/dev/null || pwd)
{
  echo "# Motion runs ($(basename "$OUT"))"
  echo
  echo "Device pixels; carousel: + is right, list: + is down. Runs under 10 px left out (the per-frame tracks are in"
  echo "\`*.track.tsv\`, every run in \`*.runs.txt\`). Step times are when each input began (recording clock, about 0.2 s"
  echo "ahead of the track's)."
  for video in "$OUT"/*.mp4; do
    name=$(basename "$video" .mp4)
    case $name in
      carousel*) axis=x; box=0,320,720,580 ;;
      *) axis=y; box=0,220,720,1480 ;;
    esac
    python -I "$T/track.py" "$video" --axis $axis --box $box --tsv "$OUT/$name.track.tsv" > "$OUT/$name.runs.txt"
    echo
    echo "## $name"
    echo
    echo '```'
    cut -f1 "$OUT/$name.steps.tsv" | paste -sd' ' | sed 's/^/steps at: /'
    awk 'NR == 1 || $5 + 0 >= 10' "$OUT/$name.runs.txt"
    echo '```'
  done
} > "$OUT/runs.md"
echo "ANALYSED $OUT"
