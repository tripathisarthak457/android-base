#!/usr/bin/env bash
set -uo pipefail
cd D:/projects/tools/android-base/generator
python -I -m unittest discover -s tests -t .
