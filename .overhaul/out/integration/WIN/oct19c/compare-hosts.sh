#!/usr/bin/env bash
# WIN's host comparison on the merged checkout: the pre-merge head's generator (OLD, 4dd3aca) against the merged one (NEW).
set -euo pipefail
L=D:/projects/tools/.overhaul/out/integration/WIN/oct19c
R=D:/projects/tools/android-base
test ! -e "$L/base" && test ! -e "$L/compare"
mkdir -p "$L/base"
git -C "$R" archive HEAD^1 generator templates tokens | tar -x -C "$L/base"
python -I D:/projects/tools/.overhaul/out/WIN/compare_hosts.py "$L/base/generator" "$R/generator" "$L/compare"
