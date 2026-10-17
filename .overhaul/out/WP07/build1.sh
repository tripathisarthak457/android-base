#!/usr/bin/env bash
# Windows, Git Bash; locked sets the SDK and cache variables.
set -o pipefail
OV=D:/projects/tools/.overhaul
cd $OV/wt/WP07/templates/multiplatform
date
$OV/bin/locked gradle ./gradlew :androidApp:assembleDevDebug --continue --console=plain
echo "exit=$?"
date
