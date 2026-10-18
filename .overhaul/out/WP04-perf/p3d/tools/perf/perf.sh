#!/usr/bin/env bash
# Measures the app on a connected phone and, with --gate, fails when it misses the limits in
# gates.toml. The work is in perf.py; run with --help for the options. Needs Python 3.11 or newer.
set -euo pipefail
exec python3 "$(dirname "$0")/perf.py" "$@"
