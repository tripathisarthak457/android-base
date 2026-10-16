#!/usr/bin/env bash
# WP10 acceptance (design/wp/WP10.md). Run in the background with the log in logs/accept.log.
set -euo pipefail

W=/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP10

cd "$W/web/frontend"
npm ci

# The checked-in catalogue is what the generator emits today.
(cd "$W/generator" && python3 -m genkit.core.catalogue --check)

npx tsc --noEmit

# Includes the parity run over generator/tests/fixtures/validation_cases.json and the contract
# tests over WP09's web/api/testdata/contract/*.json, which exist once WP09 has merged.
for contract in generate_headers events; do
  if [[ ! -f "$W/web/api/testdata/contract/$contract.json" ]]; then
    echo "missing web/api/testdata/contract/$contract.json: merge overhaul/multiplatform-v2 once WP09 has merged"
    exit 1
  fi
done
node --test "features/**/*.test.ts"

npx next build

echo ACCEPT OK
