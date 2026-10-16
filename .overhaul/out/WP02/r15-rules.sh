#!/usr/bin/env bash
set -euo pipefail
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP02
exec $L gradle bash $O/r15-rules-inner.sh
