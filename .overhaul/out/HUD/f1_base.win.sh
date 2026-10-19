#!/usr/bin/env bash
# The same everything spec from wp/WP06 as it stands (no perfhud), as devRelease, for the hidden cold-start
# comparison (startup.py), in one hold of the shared Gradle lock:
#   bin/locked gradle bash out/HUD/f1_base.win.sh
set -euo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/HUD; B=$O/base-src
# wp/WP06 as committed: its worktree may hold another run's uncommitted edits.
rm -rf "$B" && mkdir -p "$B"
git -C D:/projects/tools/android-base archive --format=tar wp/WP06 generator templates tokens | tar -x -C "$B"
(cd $B/generator && python create_project.py --spec $O/specs/hud-everything-base.json --out $O/base --no-zip --force)
cd $O/base
timeout --kill-after=2m 40m ./gradlew -Pandroid.builder.sdkDownload=false --console=plain :app:assembleDevRelease
mkdir -p $O/apks-f1
cp app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk $O/apks-f1/base-devRelease.apk
echo BASE OK
