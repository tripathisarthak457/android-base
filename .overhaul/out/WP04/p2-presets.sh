#!/usr/bin/env bash
# Phase 2 code in the lean and standard presets: generated from the worktree, built devRelease (the
# variant the gates use for them) so marker mistakes show before phase 3.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
cd $W/generator
for n in lean standard; do
  python3 - "$O/specs/base-$n.json" "$O/specs/p2-$n.json" "com.example.wp04.p2$n" <<'PY'
import json, sys
spec = json.load(open(sys.argv[1]))
spec["package_name"] = sys.argv[3]
spec["app_name"] = "WP04 P2 " + sys.argv[3][-5:]
json.dump(spec, open(sys.argv[2], "w"), indent=2)
PY
  python3 create_project.py --spec $O/specs/p2-$n.json --out $O/p2-$n --no-zip --force
done
for n in lean standard; do (cd $O/p2-$n && $L gradle ./gradlew :app:assembleDevRelease --console=plain); done
echo P2-PRESETS OK
