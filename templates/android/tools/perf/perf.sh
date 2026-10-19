#!/usr/bin/env bash
# Measures the app on a connected phone and, with --gate, fails when it misses the limits in
# gates.toml. The work is in perf.py; run with --help for the options. Needs Python 3.11 or newer,
# as python3, python or the Windows launcher py.
set -euo pipefail
for python in "python3" "python" "py -3"; do
  read -r -a command <<< "$python"
  # Windows ships python3 and python as Store stubs that exist but fail, so each one is tried.
  if command -v "${command[0]}" >/dev/null 2>&1 \
      && "${command[@]}" -c 'import sys; sys.exit(sys.version_info < (3, 11))' >/dev/null 2>&1; then
    exec "${command[@]}" "$(dirname "$0")/perf.py" "$@"
  fi
done
echo "perf.sh: needs Python 3.11 or newer on PATH (python3, python or py)" >&2
exit 127
