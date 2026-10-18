#!/usr/bin/env bash
# trace_processor from the android-profiler skill, with its binary cache at D:\.caches\perfetto (never the user profile).
#   ./tp.sh <query.sql> <trace>
exec env USERPROFILE='D:\.caches\perfetto' python C:/Users/crazyhusir/.claude/plugins/marketplaces/android-skills/profilers/android-profiler/references/perfetto/bin/trace_processor -q "$1" "$2"
