#!/usr/bin/env bash
# Screen-records one journey on the installed app, then writes every frame, frames.json (moving and
# near-blank frames) and a frame strip:
#   record.sh <package> <journey> <out dir> [--extra key=value]... [--prepare '<shell>'] [--actions '<shell>']
#             [--edge left|right] [--serial ID] [--seconds N]
# Journeys: tab_switch push pop pushpop gesture_pop rapid_back scroll back_slow_commit back_slow_cancel
# back_fast_flick back_y_follow back_to_home back_during_push custom.
# Needs ffmpeg and ffprobe, and Python 3.11 or newer.
set -euo pipefail
exec "$(dirname "$0")/perf.sh" record "$@"
