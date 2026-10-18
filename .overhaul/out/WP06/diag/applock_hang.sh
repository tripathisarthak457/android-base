#!/usr/bin/env bash
# Reruns standard-nofirebase's unit tests (all modules, in parallel, as `check` runs them) until one stalls, and dumps
# the threads of every test JVM still running after STALL_S seconds. Run under the Gradle lock:
#   $L gradle bash diag/applock_hang.sh
set -uo pipefail
O=D:/projects/tools/.overhaul/out/WP06; D=$O/diag/hang; mkdir -p $D
JDK="C:/Users/crazyhusir/AppData/Local/Programs/Android Studio/jbr/bin"
STALL_S=${STALL_S:-300}
export WP06_TEST_TIMEOUT_MIN=${WP06_TEST_TIMEOUT_MIN:-9}
cd $O/standard-nofirebase
for i in $(seq 1 ${RUNS:-6}); do
  log=$D/run$i.log
  start=$(date +%s)
  ./gradlew -Pandroid.builder.sdkDownload=false -I $O/test-timeout.init.gradle --no-build-cache \
    cleanTestDebugUnitTest testDebugUnitTest :app:cleanTestDevDebugUnitTest :app:testDevDebugUnitTest \
    --continue --console=plain > $log 2>&1 &
  pid=$!
  while kill -0 $pid 2>/dev/null; do
    sleep 5
    if [ $(( $(date +%s) - start )) -gt $STALL_S ] && [ ! -e $D/run$i.dumped ]; then
      "$JDK/jcmd" -l | grep 'Gradle Test Executor' > $D/run$i.jvms
      while read -r jpid _; do
        "$JDK/jstack" -l "$jpid" > $D/run$i.$jpid.jstack 2>&1
        ls $O/standard-nofirebase/*/*/build/test-results/*/binary/in-progress* $O/standard-nofirebase/app/build/test-results/*/binary/in-progress* 2>/dev/null >> $D/run$i.inprogress
      done < $D/run$i.jvms
      touch $D/run$i.dumped
    fi
  done
  wait $pid; rc=$?
  echo "run $i: exit $rc after $(( $(date +%s) - start )) s$([ -e $D/run$i.dumped ] && echo ', DUMPED')"
done
