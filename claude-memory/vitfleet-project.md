---
name: vitfleet-project
description: "VitFleet (tank-truck fleet SaaS: Go backend + Next.js portal + Compose Android app) cloned to D:\\projects\\tools\\vitfleet on 2026-10-24 for flow changes and bug fixes; local limits on this machine"
metadata:
  node_type: memory
  type: project
  originSessionId: be61873f-4698-4989-a724-f27749838216
  modified: 2026-10-24T07:13:15.443Z
---

On 2026-10-24 the user had VitFleet (private repo tripathisarthak457/vitfleet) cloned into `D:\projects\tools\vitfleet` (their chosen location, beside the android-base overhaul). The user will dictate flow changes and bug fixes. The repo was built on the old Linux machine (Oct 4–8); 0.2.0 (versionCode 3) shipped on Oct 8, and the backend `78ca6ed` runs on the Oracle self-host stack behind a temporary Cloudflare quick tunnel.

**Why:** what the repo itself does not say is what this Windows machine can and cannot do for it.

**How to apply:**
- Backend integration tests use testcontainers and need Docker. Docker is not installed here (Docker Desktop is forbidden; Docker Engine inside WSL2 is the user's plan, only when they say so). `make` is also absent, so run `go vet`, `go test -short ./...` and `go run` the pinned golangci-lint/sqlc directly. Say plainly which tests were not run.
- The production signing key (`vitfleet-android/keys/prod.jks` + `keystore.properties`) is git-ignored and was not found on this machine. A release APK signed with another key cannot update phones on 0.2.0, and the server refuses another signer. Ask the user for the key before any release build.
- Any new release must bump `VERSION_CODE` above 3 in `build-logic/.../AppConfig.kt`.
- Repo rules: `AGENTS.md`, `vitfleet-android/AGENTS.md`, decisions go to `CHANGELOG-DECISIONS.md`, do not push unless asked.

Related: [[android-base-generator]], [[working-rules-opus-sonnet]].
