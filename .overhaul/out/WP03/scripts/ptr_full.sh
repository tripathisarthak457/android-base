#!/usr/bin/env bash
# Builds the PTR project's release, then records it on the phone (gradle lock, then adb lock).
set -uo pipefail
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP03; L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
P=/home/crazyhusir/Documents/projects/tools/.overhaul/perf/WP03
export ANDROID_HOME=$HOME/Android/Sdk
(cd $O/ptr && $L gradle ./gradlew :app:assembleDevRelease --console=plain -q) || exit 1
rm -rf $P/ptr/after; mkdir -p $P/ptr/after
$L adb $O/scripts/ptr_session.sh 9a7c74f4 $O/ptr/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk $P/ptr/after
