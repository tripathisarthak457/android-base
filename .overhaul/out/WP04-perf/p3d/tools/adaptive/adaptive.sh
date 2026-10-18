#!/usr/bin/env bash
# Checks the app across window sizes, folds, postures, a second window and the keyboard on an
# emulator. The work is in adaptive.py; run with --help for the options. Needs Python 3.11 or newer.
set -euo pipefail
exec python3 "$(dirname "$0")/adaptive.py" "$@"
