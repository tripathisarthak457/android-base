#!/usr/bin/env bash
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; O=$OV/out/WP04-perf; L=$OV/bin/locked
cd $O
$L adb ./measure.sh everything fixed > logs/measure1.log 2>&1
echo "== $(date +%H:%M:%S) measure exit $?"
echo MEASURE1 DONE
