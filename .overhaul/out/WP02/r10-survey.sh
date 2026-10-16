#!/usr/bin/env bash
# What ktlint changes or reports in scaffolded modules across module-name lengths (3 to 30 letters),
# with and without the network feature.
set -uo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
O=$OV/out/WP02
$OV/bin/locked gradle bash -c "
  for s in net local; do
    cd $O/r10-fmt-\$s
    ./gradlew spotlessApply --continue --console=plain > $O/logs/r10-survey-\$s.log 2>&1 || echo spotless-\$s-exit=\$?
  done
"
for s in net local; do
  cd $O/r10-fmt-$s
  git diff -- feature data ':!feature/auth' ':!feature/feed' ':!feature/search' ':!feature/sample' ':!feature/settings' ':!feature/profile' ':!feature/onboarding' ':!data/auth' ':!data/feed' ':!data/search' ':!data/sample' > $O/logs/r10-survey-$s.diff
done
echo SURVEY DONE
