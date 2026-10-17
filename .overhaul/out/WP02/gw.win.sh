#!/usr/bin/env bash
# ./gradlew in the current directory, run under the shared lock as `$L gradle bash gw.win.sh <tasks...>`.
set -euo pipefail
# SDK packages change only through Android Studio's SDK Manager on this machine, never from a build.
NO_SDK_DOWNLOAD=-Pandroid.builder.sdkDownload=false
# The shared daemon can run out of class metadata after many projects' builds and expires after that build
# (WP06, Oct 17), so a build that died of it runs once more on a fresh daemon. Any other failure stands.
log=$(mktemp)
if ./gradlew $NO_SDK_DOWNLOAD "$@" 2>&1 | tee "$log"; then rm -f "$log"; exit 0; fi
if grep -q "OutOfMemoryError: Metaspace" "$log"; then
  rm -f "$log"; echo "RETRY: the daemon ran out of Metaspace"; exec ./gradlew $NO_SDK_DOWNLOAD "$@"
fi
rm -f "$log"; exit 1
