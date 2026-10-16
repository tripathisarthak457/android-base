#!/usr/bin/env bash
# WP10 acceptance (design/wp/WP10.md). Run in the background with the log in logs/accept.log.
set -euo pipefail

W=/tmp/claude-1000/-home-crazyhusir-Documents-projects-tools/e3f9060d-6364-4a81-a9a2-20851e25521f/scratchpad/verify

cd "$W/web/frontend"
# node_modules is linked from the worktree, where npm ci ran.

# The checked-in catalogue is what the generator emits today.
(cd "$W/generator" && python3 -m genkit.core.catalogue --check)

npx tsc --noEmit

# Includes the parity run over generator/tests/fixtures/validation_cases.json and the contract
# tests over WP09's web/api/testdata/contract/*.json, which exist once WP09 has merged.
test -f "$W/web/api/testdata/contract/generate_headers.json"
test -f "$W/web/api/testdata/contract/events.json"
node --test "features/**/*.test.ts"

npx next build

echo ACCEPT OK
