#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
R=/home/crazyhusir/Documents/projects/tools/android-base
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; L=$OV/bin/locked; O=$OV/out/WP00
cd $R/generator && python3 -m unittest discover -s tests -t . 2>&1 | tail -3
python3 create_project.py --spec tests/spec_full.json    --out $O/after-full    --no-zip --force > /dev/null
python3 create_project.py --spec tests/spec_minimal.json --out $O/after-minimal --no-zip --force > /dev/null
for v in full minimal; do
  diff -r -x generator-spec.json -x gradle.properties -x Chrome.kt -x README.md $O/before-$v $O/after-$v
  diff <(grep -v -E '^(org\.gradle\.jvmargs|kotlin\.daemon\.jvmargs)=' $O/before-$v/gradle.properties) \
       <(grep -v -E '^(org\.gradle\.jvmargs|kotlin\.daemon\.jvmargs)=' $O/after-$v/gradle.properties)
  grep -qx 'org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseParallelGC -Dfile.encoding=UTF-8' $O/after-$v/gradle.properties
  grep -qx 'kotlin.daemon.jvmargs=-Xmx2g -XX:+UseParallelGC' $O/after-$v/gradle.properties
  test -n "$(find $O/after-$v -name Chrome.kt -path '*foundation*')"
done
cd $R/templates/android && $L gradle ./gradlew :core:designsystem:compileDebugKotlin :app:assembleDevDebug --console=plain 2>&1 | tail -15
echo ACCEPT OK
