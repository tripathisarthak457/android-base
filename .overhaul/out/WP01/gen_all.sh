#!/usr/bin/env bash
# Usage: gen_all.sh <generator dir> <out dir>. Generates every spec in specs/ into <out dir>/<name>.
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP01
gen="$1"; out="$2"
mkdir -p "$out"; : > "$out/_failures.txt"
cd "$gen"
for f in "$O"/specs/*.json; do
  name=$(basename "$f" .json)
  rm -rf "$out/$name"
  if ! python3 create_project.py --spec "$f" --out "$out/$name" --no-zip --force > "$out/_$name.log" 2>&1; then
    echo "$name: $(tail -n 1 "$out/_$name.log")" >> "$out/_failures.txt"
  fi
done
echo done
