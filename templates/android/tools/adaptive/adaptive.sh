#!/usr/bin/env bash
# Checks the app across window sizes, folds, postures, a second window and the keyboard on an
# emulator. The work is in adaptive.py; run with --help for the options. Needs Python 3.11 or newer,
# as python3, python or the Windows launcher py.
set -euo pipefail
for python in "python3" "python" "py -3"; do
  read -r -a command <<< "$python"
  # Windows ships python3 and python as Store stubs that exist but fail, so each one is tried.
  if command -v "${command[0]}" >/dev/null 2>&1 \
      && "${command[@]}" -c 'import sys; sys.exit(sys.version_info < (3, 11))' >/dev/null 2>&1; then
    exec "${command[@]}" "$(dirname "$0")/adaptive.py" "$@"
  fi
done
echo "adaptive.sh: needs Python 3.11 or newer on PATH (python3, python or py)" >&2
exit 127
