#!/usr/bin/env bash
# Screen-records one journey on the installed app, then writes every frame, frames.json (moving and
# near-blank frames) and a frame strip:
#   record.sh <package> tab_switch|push|pop|pushpop|gesture_pop|rapid_back|scroll|custom <out dir>
#             [--extra key=value]... [--prepare '<shell>'] [--actions '<shell>'] [--serial ID]
# Needs ffmpeg and Python 3.11 or newer.
set -euo pipefail
exec python3 "$(dirname "$0")/perf.py" record "$@"
