#!/usr/bin/env bash
# Step 16's "after" builds in one hold of the shared Gradle lock:  $L gradle bash size_builds.sh
# The "before" builds come from sizes.sh, built from the integration branch's generator.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP06; O=$OV/out/WP06/size
python3 - "$W/generator/tests/fixtures/v2/everything.json" "$O/everything-noqueue.json" <<'PY'
import json, sys
spec = json.load(open(sys.argv[1]))
spec["features"] = [f for f in spec["features"] if f != "offlinequeue"]
json.dump(spec, open(sys.argv[2], "w"), indent=2)
PY
build() {  # name spec
  (cd $W/generator && python3 create_project.py --spec $2 --set package_name=com.example.wp06size.${1//-/} --out $O/$1 --no-zip --force)
  (cd $O/$1 && ./gradlew :app:assembleDevRelease --console=plain > $O/$1.log 2>&1) && return 0
  # The shared daemon can run out of class metadata after many projects' builds; it expires after that build,
  # so one retry runs on a fresh daemon. Any other failure stands.
  grep -q "OutOfMemoryError: Metaspace" $O/$1.log || return 1
  (cd $O/$1 && ./gradlew :app:assembleDevRelease --console=plain > $O/$1.retry.log 2>&1)
}
build after-standard $W/generator/tests/fixtures/v2/standard.json
build after-everything $W/generator/tests/fixtures/v2/everything.json
build after-everything-noqueue $O/everything-noqueue.json
echo SIZE BUILDS OK
