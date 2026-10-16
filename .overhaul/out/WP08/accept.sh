#!/usr/bin/env bash
# WP08 acceptance (design/wp/WP08.md), run from anywhere.
set -euo pipefail
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
W=$OV/wt/WP08
O=$OV/out/WP08

cd "$W/generator"
python3 -m unittest tests.test_target_ios -v
python3 -m unittest discover -s tests -t .
python3 -m genkit.tokens --check
python3 -m genkit.core.catalogue --check
# --target ios is explicit: inferring the target from the spec's platform is a CLI change for WP01
# to land (handoff, Open issues), not this package.
for s in ios-lean ios-standard ios-everything ios-modules; do
  python3 create_project.py --target ios --spec "tests/fixtures/v2/$s.json" --out "$O/$s" --no-zip --force
done
python3 generate_headless.py --target ios "$O/ios.zip" < tests/fixtures/v2/ios-standard.json \
  | python3 -c 'import json,sys; assert json.load(sys.stdin)["ok"]'
# XcodeGen writes Info.plist (to Config/, outside the synced app folder) from project.yml, which
# owns the key; test_target_ios asserts its exact place and value.
grep -q 'CADisableMinimumFrameDurationOnPhone: true' "$O/ios-standard/project.yml"
# The WP's form, `! grep …`, cannot fail under set -e (Part D 14b), so the absence is asserted explicitly.
if grep -rn '@MainActor' "$O/ios-everything/Packages/Core/Sources/Networking" "$O/ios-everything/Packages/Core/Sources/Persistence"; then
  echo "@MainActor found in a nonisolated target" >&2
  exit 1
fi
echo ACCEPT OK
