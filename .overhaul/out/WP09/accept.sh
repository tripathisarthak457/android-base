#!/usr/bin/env bash
# WP09 acceptance (design/wp/WP09.md). Run in the background with its log:
#   bash $O/accept.sh > $O/logs/accept.log 2>&1
# Optional: TEST_DATABASE_URL=postgres://user:pass@host:port/postgres?sslmode=disable runs the store
# integration tests against a real Postgres (each test creates and drops its own database).
set -euo pipefail
export PATH="$PATH:/usr/local/go/bin"
OV=/home/crazyhusir/Documents/projects/tools/.overhaul
W=$OV/wt/WP09
O=$OV/out/WP09
cd "$W/web/api"

echo "== gofmt"
test -z "$(gofmt -l .)"
echo "== vet"
go vet ./...
echo "== test -race"
go test -race -count=1 ./...
echo "== staticcheck"
~/go/bin/staticcheck ./...

if [ -n "${TEST_DATABASE_URL:-}" ]; then
  echo "== store integration (Postgres)"
  go test -race -count=1 -v -run Integration ./internal/store/... | tee "$O/logs/accept-store.log"
  grep -q -- '--- PASS: TestIntegrationAdminQueriesReadWhatWasRecorded' "$O/logs/accept-store.log"
  grep -q -- '--- PASS: TestIntegrationRejectionsAreGroupedByCauseNotByValue' "$O/logs/accept-store.log"
fi

echo "== real generator (needs WP01 merged)"
GENERATOR_DIR="$W/generator" go test -count=1 -v -run Integration ./internal/generate/... | tee "$O/logs/accept-generator.log"
# android must really have run; multiplatform may skip until WP07 adds the target.
grep -q -- '--- PASS: TestIntegrationRealGenerator/android' "$O/logs/accept-generator.log"
grep -q -- '--- PASS: TestIntegrationRealGenerator/v1_spec' "$O/logs/accept-generator.log"
grep -q -- '--- PASS: TestIntegrationRealGenerator/resolution' "$O/logs/accept-generator.log"

echo "== container"
docker build -t android-base-api:wp09 "$W"
docker run --rm android-base-api:wp09 /app/server -selftest | tee "$O/logs/accept-selftest.log"
grep -q '"selftest passed"' "$O/logs/accept-selftest.log"
grep -q '"target":"android"' "$O/logs/accept-selftest.log"

echo ACCEPT OK
