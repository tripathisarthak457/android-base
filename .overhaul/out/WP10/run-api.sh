#!/usr/bin/env bash
# WP09's API, built from TREE (default: the WP10 worktree once WP09 has merged into it), against a
# local Postgres, for clicking through the site. Start Postgres first:
#   docker run -d --rm --name wp10-pg -e POSTGRES_USER=wp10 -e POSTGRES_PASSWORD=<local-dev-value> -p 127.0.0.1:55440:5432 postgres:17-alpine
set -euo pipefail
TREE=${TREE:-/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP10}
O=/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP10
(cd "$TREE/web/api" && GOFLAGS=-mod=mod GOPROXY=off go build -o "$O/api-server" ./cmd/server)
export DATABASE_URL='postgres://wp10:<local-dev-value>@127.0.0.1:55440/postgres?sslmode=disable'
export ADMIN_TOKEN='<local-dev-value>'
export IP_SALT='<local-dev-value>'
export ALLOWED_ORIGINS='http://127.0.0.1:3110,http://localhost:3110'
export GENERATOR_DIR="$TREE/generator"
export PORT=8090
export ENABLE_TARGETS='android,multiplatform'
exec "$O/api-server"
