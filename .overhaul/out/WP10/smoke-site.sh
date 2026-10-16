#!/usr/bin/env bash
# Builds the site in TREE against the API on 8090 and serves it on http://localhost:3110.
set -euo pipefail
TREE=${TREE:-/home/crazyhusir/Documents/projects/tools/.overhaul/wt/WP10}
cd "$TREE/web/frontend"
NEXT_PUBLIC_API_BASE=http://127.0.0.1:8090 NEXT_PUBLIC_BUILD=wp10smoke npx next build
exec npx next start -p 3110 -H 127.0.0.1
