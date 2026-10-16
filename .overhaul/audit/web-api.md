# Audit: web/api (Go), Dockerfile, ignore files, vercel.json, .github, web/deploy

Repo: `/home/crazyhusir/Documents/projects/tools/android-base` @ `overhaul/multiplatform-v2` (HEAD `99c4edf`).
Paths below are relative to that root. Read-only audit: nothing in the repo was modified (`git status --porcelain` is empty after the run).
Scratch probes are under `/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/webapi/`.

## 0. What was run

| Command | Result |
| --- | --- |
| `gofmt -l .` (web/api) | clean |
| `go vet ./...` | clean |
| `go test -count=1 ./...` | pass. `cmd/server`, `internal/config` and `internal/store` have **no tests** |
| `staticcheck ./...` | 3 x U1000: `logger()` (middleware.go:167), `logWriter` + `Write` (server.go:535-537) are dead code |
| `golangci-lint --enable-only=errcheck,unused,gosec,bodyclose,noctx,unparam` | the dead code above; `intParam` `minimum` always 1; G204 on the two `exec.CommandContext` calls (false positives: binary comes from env) |
| `go list -m -u all` | `pgx/v5 v5.7.2 [v5.11.0]`, `puddle v2.2.2 [v2.2.3]`, `x/crypto v0.31.0 [v0.57.0]`, `x/text v0.21.0 [v0.42.0]`, `x/sync v0.10.0 [v0.23.0]` |
| go.dev release feed | supported Go releases today: **1.27.2, 1.26.9**. Repo uses 1.24 (go.mod, CI) and 1.25 (Dockerfile), and neither is supported now |
| Generator probes (TMPDIR in scratch, `-B`) | full-feature spec: 0.32 s, 19 MB RSS, 504 KB zip. 4000 feature modules (35 KB body): **29.4 s, 358 MB RSS, 29 MB zip**. Killed with SIGKILL after 5 s: **619 MB staging directory left behind** |
| Python import timing | stdlib import set without `.pyc`: 0.18-0.20 s; with `.pyc`: 0.03 s |
| Go probe (`scratch/webapi/probe`) | `VisitorHash("2001:db8::1")` hashes `"2001:db8:"`. A small JSON response followed by 400 ms of post-handler work reaches the client after **402 ms** (headers included) |

---

## 1. Critical and high findings

### H1. One request can use about 30 s of CPU, 360 MB of RAM and 600 MB of /tmp (no caps on spec cardinality or length)

- `web/api/internal/httpapi/server.go:155` caps the body at 64 KB. After that the Go layer does no validation at all: `generate.Request` (generate.go:19-45) is a typed struct, but nothing bounds counts or lengths.
- `generator/genkit/spec.py:832-835` validates each feature module's *name* but sets no limit on how many there are. `_APP_NAME` (spec.py:702) is `^[A-Za-z][A-Za-z0-9 ._-]*$` with no length bound. `validate_package_name` (spec.py:917) puts no limit on the segment count or segment length.
- Measured: a 35 KB body with 4000 modules took 29.4 s wall time and 358 MB RSS, and produced a 29 MB zip. Killing it after 5 s left 619 MB in the staging directory. The 64 KB limit allows about 7000 modules, which is close to the 90 s `GENERATE_TIMEOUT` (config.go:65).
- With `MAX_CONCURRENT_GENERATIONS=4` (config.go:66), four such requests take more than 1.4 GB of RAM plus several GB of disk. The rate limit is 30/hour per IP and per process. It does not help: it is per IPv6 address (see M7), and on a multi-instance container platform the real ceiling is 30 times the instance count. On the VPS the systemd unit (`web/deploy/install.sh:157-185`) has no `MemoryMax`/`TasksMax`, so the OOM killer can take Postgres down with it.

**Fix:** the single source of truth is `spec.validated()`. Add `MAX_FEATURE_MODULES` (around 12), `len(app_name) <= 50`, `len(package_name) <= 100`, segment count <= 8, and expose these in the catalogue so the form enforces them too. In Go, add a cheap pre-check before a semaphore slot is taken, so abuse never reaches Python:

```go
// generate/limits.go
func (r Request) precheck() error {
	if len(r.FeatureModules) > 12 || len(r.Features) > 64 || len(r.Languages) > 16 || len(r.Keystores) > 4 {
		return &InvalidRequestError{Message: "That is more than a generated project can hold."}
	}
	for _, s := range append([]string{r.AppName, r.PackageName, r.FontName, r.MonoFontName, r.VersionName}, r.FeatureModules...) {
		if len(s) > 120 || strings.ContainsFunc(s, unicode.IsControl) {
			return &InvalidRequestError{Message: "A field is too long or contains control characters."}
		}
	}
	return nil
}
```

### H2. Every timeout and every abandoned download leaks the generator's whole staging tree in /tmp, including signing keys and passwords

- Go creates `androidgen-out-*` and removes only that (`generate.go:126-130`).
- Python stages the project in a *separate* `tempfile.TemporaryDirectory(prefix="androidgen-")` under the system temp dir (`generator/genkit/build.py:42`).
- On timeout **or client disconnect**, `runCtx` is cancelled and `process_unix.go:233` sends `SIGKILL` to the process group. A Python `with TemporaryDirectory` block never gets to clean up after SIGKILL.
- Reproduced: `timeout -s KILL 5 python3 generate_headless.py ... < big.json` left `androidgen-p2d5ke4t` at 619 MB.
- Client aborts are routine. The site passes an `AbortSignal` to `generateProject` (`web/frontend/lib/api.ts:157`), and navigating away cancels the request, so the leak is not limited to abuse.
- When keystores were requested, the staging tree holds `keys/*.jks` and `keystore.properties` with **plaintext passwords** (render.py:449-507, build.py:59-64). Those files then stay on disk indefinitely. On the VPS, `PrivateTmp=true` clears them only on restart. On a container they stay until the instance is recycled.
- The README promise is false under these conditions: `web/README.md:179-180`, "written to a temporary directory that is deleted as the download finishes".

**Fix (one line, and it also fixes L3):** give the child an explicit environment with `TMPDIR` set to the Go work directory. Everything Python and keytool write then lives under the one directory that Go's `RemoveAll` deletes, whatever signal ends the process:

```go
cmd.Env = []string{
	"PATH=" + os.Getenv("PATH"), "JAVA_HOME=" + os.Getenv("JAVA_HOME"),
	"HOME=" + workDir, "TMPDIR=" + workDir, "LANG=C.UTF-8",
	"PYTHONUNBUFFERED=1", "PYTHONIOENCODING=utf-8", "PYTHONDONTWRITEBYTECODE=1",
}
```

### H3. Internal generator failures are reported as the user's fault (HTTP 400), never reach the error list, and lose their traceback

- `generator/generate_headless.py:108-113` uses the *same* `fail()` shape for `SpecError` (user input) and for `RenderError`, `OSError` and any other `Exception` (bugs). `fail()` writes `{"error": ..., "detail": traceback}` and exits 1.
- `web/api/internal/generate/generate.go:155-162`: any parseable `{"error": ...}` on stdout becomes `*InvalidRequestError`.
- `server.go:176-185` then answers **400**, records a failed generation and deliberately does **not** call `RecordError`, citing "the error list is for bugs".
- Result: the admin "errors" panel never shows real template or generator bugs such as `RenderError("Missing variant directory ...")` (render.py:222), `RenderError("AppConfig.kt not found ...")` (render.py:288), an unbalanced-marker crash or a `KeyError`. Only timeouts and non-JSON crashes get there.
- `failure.Detail` (the traceback) is parsed at generate.go:157 and dropped. `RecordError` is always called with `detail=""` (server.go:191, 364, 495), so the `errors.detail` column (0001_initial.sql:56) is **never populated**.
- Internal messages are shown to visitors: `Missing variant directory for 'x': /app/generator/variants/...` reveals server paths and exception class names.

**Fix:** add `"kind": "invalid" | "internal"` to the headless failure JSON. Only `SpecError` and `TypeError` count as `invalid`. In Go, `invalid` maps to 400, and `internal` maps to 500 plus `RecordError(kind="generation", message, detail=traceback)` plus a generic message to the user.

### H4. CI never builds the site's default preset, and never exercises the code path users actually download

- `.github/workflows/ci.yml:86`: `preset: [none, lean, everything, all]`. The presets are `lean`, `standard` and `everything` (`generator/genkit/spec.py:574-612`), and **`standard` is the site's default** (`generator/genkit/catalogue.py:197`, `"preset": "standard"`).
- `standard` is the only CI-reachable configuration with **auth on and googlesignin off**. `lean` has no auth, and `everything`/`all` turn googlesignin on. `<opt:googlesignin>` blocks are stripped from `template/feature/auth/build.gradle.kts`, `feature/auth/.../SignIn.kt`, `feature/auth/src/main/res/values/strings.xml`, `data/auth/.../AuthRepository.kt` and `data/auth/.../remote/AuthDto.kt` only in that combination. The most downloaded rendering is therefore never compiled before shipping.
- The matrix is hand-written rather than derived from `PRESETS`, so the next preset added will be skipped as well.
- CI generates with `create_project.py --no-zip` (ci.yml:124) and then runs `test -x ./gradlew` (ci.yml:126-130). The website path is `generate_headless.py`, which goes through `render.zip_project` (render.py:519-537) and sets permission bits by hand. **Neither `generate_headless.py` nor `zip_project` is executed anywhere in CI or in the unit tests** (`grep zip|headless generator/tests/test_generator.py` finds only `zip_output=False`). The executable-bit check exercises the path users do not get.

**Fix:**
```yaml
# derive the matrix
  presets:
    runs-on: ubuntu-latest
    outputs: { list: ${{ steps.p.outputs.list }} }
    steps:
      - uses: actions/checkout@v7
      - id: p
        working-directory: generator
        run: echo "list=$(python -c 'import json,sys;sys.path.insert(0,".");from genkit.spec import PRESETS;print(json.dumps(["none","all"]+[p.key for p in PRESETS]))')" >> "$GITHUB_OUTPUT"
  android:
    needs: presets
    strategy: { matrix: { preset: ${{ fromJson(needs.presets.outputs.list) }} } }
    steps:
      # ...generate exactly as the API does:
      - run: python generator/generate_headless.py "$RUNNER_TEMP/p.zip" < generator/ci-spec.json && unzip -q "$RUNNER_TEMP/p.zip" -d out-zip && test -x out-zip/*/gradlew
```

---

## 2. Medium findings

### M1. Telemetry writes are synchronous and on the client's critical path, including the page-load call `/api/options`

- `middleware.go:58-62`: `observe` does a DB `INSERT` after every handler (`RecordRequest`) *before* `ServeHTTP` returns. `server.go:232-237`: generate does `RecordGeneration`, then `RecordVisit`, then `observe`'s `RecordRequest`. That is **3 sequential round trips**, and 4 or more on failures (`record` + `RecordError` + `RecordRequest`). `handleTrack` makes 2.
- The commit that introduced this (`11377ec`) says "the cost is a few milliseconds after the last byte is already on the wire". That holds only for the zip, which has `Content-Length`. For every small JSON response (options 18 KB, track, health, errors, admin), `net/http` holds the bytes in its buffered writer until the handler returns. The Go probe shows **headers and body arrive only after the post-handler work** (402 ms for 400 ms of work).
- Each write has a 5 s budget (`server.go:45-47`). With a slow or cold Postgres (Neon, Supabase) the landing page's `/api/options` waits up to 5 s, `/api/track` up to 10 s, and a failed generate up to 20 s.
- `cmd/server/main.go:55-60`: `store.Open` failure is fatal. **An analytics-database outage takes the generator itself offline**, and every cold start waits on Postgres (and on migrations, M8).

**Fix:** keep the "finish before returning" property, because the commit explains the platform freezes the process after the response. But (a) gather all writes for a request into one `pgx.Batch` and send it in **one round trip**, (b) cut the budget to around 1 s, (c) open the store lazily or in the background and serve generation without it (log `persistent=false` until it connects), and (d) stop writing a `requests` row per request. Aggregate route/status/latency in memory and flush one row per route per minute from whichever request crosses the minute boundary, or rely on platform logs and observability.

```go
type rec struct{ b pgx.Batch }
func (s *Server) flush(ctx context.Context, b *pgx.Batch) {
	if b.Len() == 0 { return }
	if err := s.store.Pool().SendBatch(ctx, b).Close(); err != nil { s.log.Warn("telemetry", "error", err) }
}
```

### M2. Python's `$` accepts a trailing newline, so the API returns HTTP 200 with a project whose build-logic does not compile

- All validation regexes use `re.match(...$)`, and `$` matches before a final `\n`. Verified with `ProjectSpec(...).validated()`, all ACCEPTED: package `"com.example.app\n"`, version_name `"1.0.0\n"`, `api_base_urls={"dev":"https://a.com/\n"}`, font `"DM Sans\n"`, deeplink host/scheme with `\n`, feature module `"orders\n"`. Regexes are at `generator/genkit/spec.py:699-715`, and the version check is at spec.py:829.
- These values are spliced into Kotlin string literals: `render.py:292-298` (`APPLICATION_ID = "..."`, `VERSION_NAME = "..."`), render.py:323-324 (font names), and `_replace_flavour_field` for URLs. A raw newline inside `"..."` is a Kotlin compile error in `build-logic`, so the first `./gradlew` fails. A module or package with `\n` also creates directory names that contain a newline.
- Only one trailing newline gets through, so this is not code injection. It is still a "valid" zip that does not build, and nothing in Go guards against it (Go decodes `"\n"` and re-encodes it faithfully).

**Fix:** use `re.fullmatch` (or `\Z`) everywhere in spec.py, and add the Go control-character pre-check from H1.

### M3. The Go request struct silently drops any field it does not know, so every new spec option needs four synchronized edits (and the multi-target plan does not fit)

- `server.go:161-165` decodes into `generateRequest{generate.Request; Preset}` and `generate.go:121` re-marshals only the known fields. An option added to `spec.py`, `generate_headless.py:69-76` and the frontend but forgotten in `generate.go:19-45` is **dropped without a word**. The user ticks it and the zip lacks it, and Python's `ignoredFields` cannot report it because it never arrives.
- The history shows this pattern: `5931457` (three brand colours), `f0fa4c0` (keystores, module names) and `8e3461d` each edited `generate_headless.py` and `generate.go` together.
- The Python allow-list (`generate_headless.py:69-76`) duplicates `dataclasses.fields(ProjectSpec)`.

**Fix:** treat the spec as opaque in Go. Read the raw body, run generic guards (size, depth, control characters, H1 caps), extract only the few fields needed for telemetry with a narrow struct, and pipe the **raw JSON** to the generator. In Python, derive `allowed` from `dataclasses.fields(ProjectSpec)` minus a small denylist (`existing_path`), and return `ignoredFields`, which Go should record (see section 4).

### M4. Admin telemetry is wrong in several places

| Where | What is wrong |
| --- | --- |
| `server.go:299` `Referrer: trimReferrer(r.Header.Get("Referer"))` | On the Vercel deployment the API is same-origin, so the `Referer` of the `fetch('/api/generate')` is the configurator page. **Every row records the site's own origin**, and the external referrer (Reddit, HN) is never captured. It has to come from `document.referrer` on the `landed` event. |
| `store.go:104` `Country` / `0001_initial.sql:34` | The column exists but **is never written** (`record()` at server.go:283-300 never sets it). |
| `0001_initial.sql:56` `errors.detail` | Never written (H3). |
| `server.go:227-237` | An interrupted download (`io.Copy` error, line 227) is still recorded as `succeeded=true` and as funnel step `generated`. |
| `generate.go:117-118`, `generate.go:164-171` → `server.go:187-192` | A client that gives up while queued (`ctx.Err()` is `context.Canceled`) or mid-generation (`signal: killed`, not `DeadlineExceeded`) is logged at ERROR, **inserted into `errors` as a bug**, and marks the generation failed. Because `RecordError` sets `resolved = FALSE` on every repeat (store.go:146), ordinary user cancellations re-open "bugs" that were marked resolved, and they drag down the success rate. |
| `store.go:199-208` | `successRate`, `median/p95 DurationMs` cover **all rows ever**, including validation rejections (about 50 ms) and abandoned ones, so medians come out too low and failures are a mix of different things. There is no time window, so it is a full scan on every admin load. |
| `store.go:77-84` `VisitorHash` | Strips "a port" by cutting at the last `:`. The inputs never carry ports (clientIP already removes them), so **every IPv6 address loses its last hextet**: the probe shows `2001:db8::1` and `2001:db8::2` give the same hash. |
| `server.go:259` | `handleTrack` inserts with `r.Context()`, while every other write uses the detached context (the reason is given at server.go:42-44). |
| admin page (`web/frontend/app/admin/page.tsx:86-96`) | Each portal load fires 7 API calls, and each one is written to `requests`, so the admin inflates its own route-health numbers. |

### M5. CI deploy gating gaps: no container build, no smoke test, no concurrency control, default token permissions

- The Docker image is **never built in CI**. A broken `apk add openjdk21-jre-headless`, a Go-version mismatch (CI 1.24 versus Docker 1.25) or a missing COPY first shows up in Vercel's remote build during the `deploy` job, after every gate is green. A failed Vercel build does not promote, so this is safe, but it is caught late and nothing checks the running container.
- There is no smoke test of the deployed URL before it receives traffic. `npx vercel@59 deploy --prod` (ci.yml:179) promotes immediately.
- There is no `concurrency:` block. Two quick pushes to `main` run two `deploy` jobs, and the older commit can finish last and **roll production back**.
- There is no top-level `permissions:` block, so `GITHUB_TOKEN` gets the repository default (often read/write).
- `vercel@59` is pinned while 63.1.0 is current. Renovate does not see `npx vercel@59` inside a `run:` script.

**Fix:**
```yaml
permissions: { contents: read }
concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: ${{ github.event_name == 'pull_request' }} }
jobs:
  container:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: docker/setup-buildx-action@v3
      - uses: docker/build-push-action@v6
        with: { context: ., load: true, tags: api:ci, cache-from: type=gha, cache-to: "type=gha,mode=max" }
      - run: |
          docker run -d -p 8080:8080 -e PORT=8080 --name api api:ci
          for i in $(seq 30); do curl -fsS localhost:8080/api/health && break; sleep 1; done
          curl -fsS -X POST localhost:8080/api/generate -H 'Content-Type: application/json' \
            -d '{"app_name":"Smoke","package_name":"com.example.smoke"}' -o smoke.zip
          unzip -l smoke.zip | grep -q 'Smoke/gradlew'
  deploy:
    needs: [generator, api, frontend, android, container]
    concurrency: { group: production, cancel-in-progress: false }
    # vercel deploy (preview) -> curl smoke on the preview URL -> vercel promote
```

### M6. Go toolchain is out of support and inconsistent across three places; dependencies are behind

- `web/api/go.mod:3` `go 1.24`, `ci.yml:47` `go-version: "1.24"`, `Dockerfile:15` `golang:1.25-alpine`. go.dev lists 1.27.2 and 1.26.9 as current, so **1.24 and 1.25 no longer receive security fixes**, and this is a public `net/http` server. CI tests with a different compiler than production builds with.
- `pgx v5.7.2` (latest v5.11.0). The indirect `x/crypto v0.31.0` (latest v0.57.0) is used by pgx for SCRAM.
- `web/deploy/install.sh:82` installs Ubuntu 24.04's `golang-go` (1.22), so every VPS build silently downloads a toolchain through `GOTOOLCHAIN=auto`.
- Renovate has been configured, but the repository history has **zero** Renovate commits (`git log --all | grep -ic renovate` returns 0), so the app is probably not installed or not acting.

**Fix:** set `go 1.27` / `toolchain go1.27.2` in go.mod, have CI use `go-version-file: web/api/go.mod`, and use `golang:1.27-alpine` in Docker. Add `govulncheck ./...` and `staticcheck` to the API job.

### M7. Client IP trust and rate-limit key are spoofable or bypassable outside the Vercel and Caddy setups

- `middleware.go:150-165` takes the **leftmost** `X-Forwarded-For`. Its comment says the service sits "behind Caddy on the same host", but production is the Vercel container. Vercel overwrites XFF with the real client address, and Caddy does too (`web/deploy/Caddyfile:16` `header_up X-Forwarded-For {remote_host}`), so both of those are safe. The Dockerfile is generic, though. Behind any proxy that *appends* (Cloud Run, Fly, Railway, AWS ALB, Cloudflare→origin), the leftmost value is whatever the client sent, so the rate limit and visitor hash can be forged per request. The test at `middleware_test.go:52-54` locks in "leftmost" with the wrong reasoning.
- The limiter key is the full IPv6 address (`middleware.go:139`). One client with a /64 has 2^64 keys, so the limit can be bypassed for free, and the limiter map (`middleware.go:114`) grows by one entry per address per hour.

**Fix:** add a `TRUSTED_PROXY` setting (`vercel` → use `x-real-ip`/XFF as given; `hops=N` → take the Nth from the right; `none` → `RemoteAddr`). Key the limiter and visitor hash on the /64 for IPv6.

### M8. Migrations: no version table and no lock; every DDL file re-runs on every cold start

- `store.go:51-70` executes every embedded file at every boot. With no `schema_migrations` record, any future migration must be idempotent forever, which rules out data backfills and `ALTER ... TYPE`. The telemetry work in section 4 needs exactly those ALTERs.
- With no `pg_advisory_lock`, two instances cold-starting together against a fresh database race `CREATE TABLE IF NOT EXISTS`, a known Postgres race that ends in a `pg_type` unique violation and one instance failing to boot.
- Cold-start cost: 2 multi-statement round trips, plus the `CREATE INDEX IF NOT EXISTS` lock checks on 5 tables, on every scale-from-zero.

**Fix:**
```go
func (s *Store) migrate(ctx context.Context) error {
	conn, err := s.pool.Acquire(ctx); if err != nil { return err }
	defer conn.Release()
	if _, err := conn.Exec(ctx, `SELECT pg_advisory_lock(7271)`); err != nil { return err }
	defer conn.Exec(context.Background(), `SELECT pg_advisory_unlock(7271)`)
	conn.Exec(ctx, `CREATE TABLE IF NOT EXISTS schema_migrations (name text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now())`)
	// for each file not in schema_migrations: BEGIN; exec; INSERT name; COMMIT
}
```
Better still, run migrations from the CI `deploy` job (`go run ./cmd/migrate`) and have the server only *check* the version, which is one query.

### M9. Unauthenticated, unlimited database writes and no retention

- `/api/health`, `/api/options` and `/api/track` are not rate-limited (`server.go:88-91`), and each request writes a `requests` row (`middleware.go:58-62`). `/api/track` also writes `visits`. One script can fill the paid Postgres.
- No table has a retention or delete path (`grep DELETE internal/store` is empty). `requests` is described as "kept for every request rather than sampled" (0001_initial.sql:78-79), and `generations` keeps app and package names indefinitely.

**Fix:** rate-limit `track`, aggregate request metrics (M1), and add a daily prune (`DELETE FROM requests WHERE at < now() - interval '30 days'`), triggered opportunistically or by a Vercel cron hitting an admin endpoint.

### M10. Cross-site "simple" POSTs are accepted

`handleGenerate`, `handleTrack` and `handleFeedback` never check `Content-Type`. A third-party page can `fetch(api, {method:'POST', mode:'no-cors', body: JSON-as-text/plain})` with no preflight. The browser blocks reading the response, but the server **runs the generation, records funnel steps and stores feedback**, so other sites' visitors can be used to burn CPU and pollute metrics. CORS (`middleware.go:68-89`) only controls reads.
**Fix:** reject anything but `application/json` with 415 (this forces a preflight, which the allow-list then fails). Optionally also reject `Sec-Fetch-Site: cross-site` when `Origin` is not allowed.

### M11. Byte-based truncation splits UTF-8, so long non-ASCII reports fail with a 500

`store.go:483-488` `truncate` slices at `s[:limit]` bytes. `SaveFeedback` truncates the body to 8000 bytes (store.go:536), while the form allows `maxLength={8000}` **UTF-16 units** (`web/frontend/components/feedback.tsx:304`). A Hindi report (the site ships `hi`) longer than about 2667 characters is cut mid-rune, Postgres rejects the invalid UTF-8 (SQLSTATE 22021), and the user sees "That could not be saved". The same helper is used for error messages (store.go:150), and `generate.truncate` (generate.go:213-218) can produce invalid stderr text that then fails the `errors` insert.
**Fix:** `for limit > 0 && !utf8.RuneStart(s[limit]) { limit-- }`, or `strings.ToValidUTF8(s[:limit], "")`.

### M12. Runtime image is heavy, and Python recompiles on every spawn

- `Dockerfile:33` `apk add openjdk21-jre-headless`. The comment says "about 55MB", but Alpine's package page lists **Installed size 187.9 MiB** (62 MiB download). The JRE exists only to run `keytool`, and it is about 3x everything else in the image. Cold-start image pulls pay for it on every new instance.
- The official `python:3.12-alpine` Dockerfile deletes every `*.pyc` (`find /usr/local ... -name '*.pyc' ... -exec rm -rf`, docker-library/python 3.12/alpine3.24 Dockerfile:122-125). `.dockerignore` excludes `**/__pycache__/`, and the container runs as `nobody` (Dockerfile:59), which cannot write bytecode next to root-owned sources. As a result, **every generation and the boot-time catalogue recompile the stdlib and genkit**. Locally that is 0.18-0.20 s against 0.03 s, about +150 ms per request, on a full-feature generation that otherwise takes 0.32 s. The VPS unit has the same problem (`ProtectSystem=strict`, `ReadOnlyPaths=$APP_DIR`).

**Fix:**
```dockerfile
FROM golang:1.27-alpine AS build
WORKDIR /src
COPY web/api/go.mod web/api/go.sum ./
RUN --mount=type=cache,target=/go/pkg/mod go mod download
COPY web/api/ ./
RUN --mount=type=cache,target=/root/.cache/go-build CGO_ENABLED=0 go build -trimpath -ldflags '-s -w' -o /out/server ./cmd/server

FROM eclipse-temurin:21-jdk-alpine AS jre   # keytool ships with java.base
RUN jlink --add-modules java.base,jdk.crypto.ec --strip-debug --no-man-pages --no-header-files \
          --compress=zip-9 --output /jre

FROM python:3.13-alpine
COPY --from=jre /jre /opt/jre
COPY --from=build /out/server /app/server
COPY generator/ /app/generator/
COPY template/ /app/template/
RUN python -m compileall -q -j0 /usr/local/lib/python3.13 /app/generator \
 && cd /app/generator && python -m genkit.catalogue > /app/catalogue.json   # precomputed: no Python at boot
ENV JAVA_HOME=/opt/jre PATH="/opt/jre/bin:$PATH" GENERATOR_DIR=/app/generator CATALOGUE_FILE=/app/catalogue.json
USER nobody
```
Check the jlink output with `ls /jre/bin/keytool`. A longer-term option: generate the keystores **in the browser**, since the README itself (`web/README.md:195-198`) calls server-made keys a custody problem. That removes the JRE, the passwords-on-the-wire issue and the argv exposure (L3) together.

### M13. The second deployment path (VPS) has drifted and duplicates the production path

`web/deploy/install.sh` builds with apt's Go 1.22, installs no JRE (so keystores are unavailable), never writes `generator/COMMIT`, says "No Docker" (install.sh:54-56) while production is a Docker image, and prints the wrong admin URL `https://$DOMAIN/admin/overview` (install.sh:209). The routes are at `/api/admin/*` (server.go:98). Nothing tests it. For the overhaul's "lighter" goal, delete `web/deploy/` or reduce it to `docker run` of the same image behind Caddy.

---

## 3. Low findings

- **L1. Dead code:** `logger()` at middleware.go:167-169 and `logWriter` at server.go:535-537 (staticcheck U1000).
- **L2. `/api/health` hard-codes `"keystores": false`** (server.go:119) even though the catalogue reports `keystoresAvailable: true` whenever keytool exists, which the Dockerfile installs for exactly that purpose.
- **L3. Child processes inherit the whole server environment** (`generate.go:141`, `append(os.Environ(), ...)`), including `DATABASE_URL`, `ADMIN_TOKEN`, `IP_SALT` and any Vercel secrets. The generator needs none of them. Keystore passwords reach keytool on **argv** (`generator/genkit/render.py:485-496` `-storepass`, `-keypass`), which `/proc/<pid>/cmdline` can read. Fix: the explicit env in H2, plus `-storepass:env KS_PASS -keypass:env KEY_PASS`.
- **L4. Admin write endpoints:** `handleFeedbackUpdate` and `handleResolveError` turn *every* error, including DB outages, into **404** and echo `err.Error()` (server.go:413-415, 485-487). `UpdateFeedback` does not check `status` against the closed set that `handleFeedbackList` enforces (server.go:385), so a typo makes an item disappear from every filter. `notes = COALESCE(NULLIF($3,''), notes)` (store.go:617) means notes can never be cleared.
- **L5. Graceful shutdown:** `Shutdown` waits 30 s (main.go:101), but a generation can run for 90 s. On timeout `run` returns an error and the process exits 1 with "fatal". Children run in their own process group (process_unix.go:232) and are **not** signalled when the server exits.
- **L6. No load shedding:** requests queue on the semaphore (generate.go:114-119) with no cap and no wait deadline, holding goroutines, bodies and connections. Return 503 with `Retry-After` when the queue is deeper than around 2x the concurrency, or after about 10 s of waiting.
- **L7. Body read errors map to 413:** server.go:155-158 and 324-327 answer 413 for *any* `ReadAll` error (client hang-up, read timeout). Use `errors.As(err, new(*http.MaxBytesError))`.
- **L8. `recorder` lacks `ReadFrom`/`Unwrap`:** (middleware.go:15-40) so `io.Copy(w, file)` cannot use sendfile and falls back to a 32 KB buffer copy. Add `func (r *recorder) Unwrap() http.ResponseWriter { return r.ResponseWriter }` plus a `ReadFrom` passthrough.
- **L9. ETag handling:** `If-None-Match` is compared as one exact string (server.go:135), so `W/` tags, lists and `*` are not honoured. Better: since the catalogue is fixed per deployment, send `Cache-Control: public, max-age=0, s-maxage=86400` so Vercel's CDN serves it without waking the container. Vercel purges the CDN cache on each deploy, which removes the stale-catalogue risk described at server.go:129-131. Better again: bake `catalogue.json` into the static site at build time and drop the landing-page API call entirely.
- **L10. Admin reads full-scan history:** `Overview` (store.go:199-208), `Daily` (store.go:271-278: both subqueries `GROUP BY` the whole table before joining 30 days) and `FeaturePopularity` (store.go:305-311, `unnest` over all rows, which the GIN index does not help). Bound them with `WHERE created_at >= now() - $window`, or keep a daily rollup table.
- **L11. Error shape and versioning:** errors are `{"error": "<prose>"}` only, with no machine code, field or request id (server.go:508-510). There is no `/v1`. Go response types are copied by hand into `web/frontend/app/admin/page.tsx:9-58`. Proposed shape: `{"error":{"code":"invalid_spec","message":"…","field":"package_name","requestId":"…"}}`. Generate TS types from Go structs (or OpenAPI) in CI.
- **L12. Ignore files:** `.vercelignore` and `.dockerignore` miss `out/` (where AGENTS.md tells you to generate test projects), `*.zip`, `.claude/`, `.agents/`, `.venv/` and `tools/`, all of which `.gitignore` covers. A local `vercel --prod` (the README's instruction, web/README.md:97-99) uploads them. Comments say the payload is "about 1.5MB", but `template/` is 3.9 MB without build output.
- **L13. Docs drift:** `web/README.md:17` names `Dockerfile.api`, but the file is `Dockerfile` (vercel.json:16). `ci.yml:11` says "Vercel IS connected to this repository, for pull-request previews" while `ci.yml:147` says "Vercel is not connected to this repository on purpose". The Caddyfile comment at lines 30-31 about a "default 30s" timeout sits above `request_body { max_size 1MB }`. `middleware.go:150-152` still describes Caddy as the deployment.
- **L14. Renovate:** the comment at renovate.json5:14-16 promises a 3-day minimum release age, but `config:best-practices` only includes `security:minimumReleaseAgeNpm` (npm only, confirmed in the Renovate docs). Add `minimumReleaseAge: "3 days"` at the top level. *(Plausible:)* the "Android Gradle plugin" group lists `matchManagers: ["gradle-wrapper"]`, but the wrapper's dependency name is `gradle`, which matches neither `com.android.tools.build:gradle` nor `com.android.**`, so the wrapper is not actually grouped with AGP as the comment claims.
- **L15. Privacy:** `IP_SALT` is static (install.sh:130), so `visitor_hash` is a permanent pseudonymous identifier. "Rotating it" (config.go:52-53) is manual. A daily-derived salt, `HMAC(IP_SALT, date)`, gives Plausible-style counts with no linkage across days.
- **L16. Preview deployments:** `vercel.json:3-7` disables only `main`, so every other branch push builds a container preview, JRE included. If `DATABASE_URL` and `ADMIN_TOKEN` are also set for the *Preview* environment in Vercel (not visible from the repo), previews write to the production database. Verify in the Vercel dashboard and scope them to Production.
- **L17. Feedback contradicts the rate limit:** feedback shares the generation limiter (server.go:92, 313-314), so a user who has just hit the 30/h cap cannot report the problem.

---

## 4. Telemetry: what is recorded today and what the admin portal needs

**Recorded per generation** (`store.Generation`, store.go:88-106): visitor hash, app name, package, client-claimed preset, features, feature modules, min/target SDK, motion style, font, accent, succeeded, zip bytes, total duration (queue + spawn + build, excluding the stream), failure text, and referrer (always the site itself, M4). Recorded but never written: country. Funnel (`visits`): landed, configured, generated, downloaded per visitor per day. Requests: route, method, status, ms and bytes for every call.

**Missing, roughly in order of value:**

1. **Outcome taxonomy:** `outcome` ∈ `ok | invalid | internal_error | timeout | abandoned_queue | abandoned_running | download_interrupted | rate_limited`. Today these collapse into a single boolean and pollute the error list (H3, M4).
2. **Timing breakdown:** `queue_ms` (semaphore wait), `python_ms` (the `ElapsedMillis` the generator already returns, generate.go:71, sent only as a header), `spawn_overhead_ms`, `stream_ms`, `cold_start` (first request of the process) and `instance_id`.
3. **Geography (anonymous):** read `x-vercel-ip-country`, `x-vercel-ip-country-region` and `x-vercel-ip-city` (URL-encoded) when present, and `CF-IPCountry` elsewhere. Store country and region only, with city optional and coarse. Confirm the container service receives these headers by logging header names once.
4. **Client class:** `client` ∈ `web | curl | cli | bot`, from the UA, `Sec-Fetch-Mode` and an explicit `X-Client: android-base-web/<build>` header, plus a UA class (desktop or mobile, from `Sec-CH-UA-Mobile`). This separates scripted use from site use and lets bots be excluded from the funnel.
5. **Acquisition:** the `landed` event should carry `document.referrer`'s origin and `utm_source`, `utm_medium` and `utm_campaign`. Store them on `visits` (new columns) and join them into generations through a random **session id** that the page keeps in `sessionStorage`. This also fixes NAT/CGNAT users all looking like one IP-hash visitor.
6. **Configuration dimensions the portal cannot chart today:** `design_style`, `languages`, `haptics_enabled`, `compile_sdk`, `keystores_requested` (a count, never the values), whether custom URLs or deep links were set, `ignored_fields` and `warnings` from the generator result (which show contract drift), `generator_commit` (record.py already computes it), and `target` (section 6).
7. **Funnel resolution:** `options_loaded`, `preset_chosen:<key>`, `generate_clicked`, `generate_failed:<code>`, `download_saved`. These should stay a closed enum as `/api/track` does now (server.go:257-266), but extensible through one constant list shared with the frontend.
8. **Client errors:** the schema comment lists `kind 'client'` (0001_initial.sql:54), but no endpoint accepts frontend exceptions. Add a rate-limited `/api/v1/client-error` that writes fingerprinted rows.
9. **Retention and rollups:** a `daily_rollup(day, target, preset, outcome, country, client, n, p50_ms, p95_ms)` table maintained on write (UPSERT) makes the admin queries O(days) instead of O(rows).

Proposed schema delta (migration 0003, which only works once the version table from M8 exists):
```sql
ALTER TABLE generations
  ADD COLUMN target text NOT NULL DEFAULT 'android',
  ADD COLUMN outcome text,
  ADD COLUMN queue_ms int, ADD COLUMN python_ms int, ADD COLUMN stream_ms int,
  ADD COLUMN cold_start boolean, ADD COLUMN client text, ADD COLUMN region text,
  ADD COLUMN session_id text, ADD COLUMN generator_commit text,
  ADD COLUMN spec jsonb;          -- sanitized spec (no keystores), replaces per-option columns going forward
ALTER TABLE visits ADD COLUMN session_id text, ADD COLUMN referrer_origin text,
  ADD COLUMN utm_source text, ADD COLUMN utm_medium text, ADD COLUMN utm_campaign text, ADD COLUMN country text;
CREATE INDEX ON generations (target, created_at DESC);
```

---

## 5. What is already right (keep it)

- Spec on **stdin**, never in a file or argv (generate.go:139, generate_headless.py:8-9).
- A typed Go struct as the first filter blocks type confusion. `existing_path` is excluded in **both** layers (the Go `Keystore` struct, and `KEYSTORE_FIELDS` at generate_headless.py:34-37), so path-traversal and file-read through the keystore copy is closed. No zip is ever extracted, so zip-slip does not apply. Zip entry names come from validated names, and `ProjectName` is alphanumeric, so the `Content-Disposition` header is safe.
- Process-group kill on timeout, with a test (process_unix.go, process_unix_test.go). `WaitDelay` is set.
- The semaphore slot is released before streaming (generate.go:116), so slow downloads do not block generation.
- `MaxBytesReader` on the bodies, `ReadHeaderTimeout` set.
- Constant-time bearer comparison (middleware.go:96-106) with good tests. The token is never accepted in the query string and never logged. Config refuses a token shorter than 24 characters and refuses a database without a token.
- All SQL is parameterized. There is no string-built SQL (`grep Sprintf internal/store` returns only error messages).
- Closed sets for feedback kind, severity and area. The `generated` funnel step cannot be claimed by the client.
- The generator makes no outbound network calls (no SSRF surface).

---

## 6. What has to change for three targets (android, cmp, swift)

1. **Routing and versioning:** `/api/v1/targets` lists targets and capabilities; `/api/v1/{target}/options` and `/api/v1/{target}/generate`. Keep `/api/options` and `/api/generate` as aliases for `android` during the transition.
2. **Opaque spec passthrough (M3):** drop the Android-shaped `generate.Request` (`min_sdk`, `target_sdk` and so on make no sense for Swift). Go validates generically (size, depth, control characters, H1 caps per target from the catalogue) and pipes raw JSON to `generate_headless.py --target <t>`, or to per-target entrypoints under `generator/targets/<t>/`.
3. **Generator registry:** turn `generate.Generator` into `map[target]*Generator`, each with its own script, timeout, concurrency and **catalogue cache**. The current single `catalogue json.RawMessage` (server.go:32) becomes per target. Precompute all catalogues at image build (M12).
4. **Failure taxonomy (H3)** has to be identical across targets, so define it once in a shared Python module (`genkit/headless.py`) that all target entrypoints use.
5. **Storage:** a `target` column on `generations`, `feedback` and `visits`. Move per-option columns to `spec jsonb` with a few generated columns for hot dimensions. Every admin query takes a `target` filter.
6. **Docker image:** add `template-cmp/` and `template-swift/`. Swift generation is text templating and needs no Xcode at generation time, so it stays Linux. Only Android and CMP need keytool, and only if server keygen survives (M12 suggests moving it to the browser). Extend `.dockerignore` and `.vercelignore` with `**/DerivedData/`, `**/.build/`, `**/xcuserdata/`, `**/Pods/`, `**/.swiftpm/` and `**/kotlin-js-store/`.
7. **CI:** derive a matrix per target from each generator's preset list (H4). Android and CMP Android build on `ubuntu-latest`. The CMP iOS framework and the SwiftUI app need a `macos-latest` job (`xcodebuild -scheme ... -destination 'generic/platform=iOS Simulator' build`). `deploy.needs` must include all of them, and the container smoke test (M5) should generate one zip per target.
8. **Rate limiting and concurrency:** keep a per-target semaphore so a slow target (for example CMP with a larger template) cannot starve Android.

The user's request also mentions Kyant liquid glass for Android. The API does not care about it: once `liquidglass` is a `Feature` in `spec.py` with a catalogue group, the site lists it automatically. The one API risk is M3. If the glass work adds a *new top-level spec field* (for example `glass_style`) instead of a feature key, today's Go struct drops it silently. Do the passthrough before adding such fields.

---

## 7. Suggested order

1. H2 (explicit child env with TMPDIR): one-line fix that removes the disk leak, the secrets left in /tmp and the env leak.
2. H1 + M2 (caps, `fullmatch`, Go pre-check): closes the cheap DoS and the broken-zip input.
3. H3 + the M4 cancellation classification: makes the admin error list trustworthy.
4. H4 + M5 (CI: standard preset, headless/zip path, container build and smoke, concurrency, permissions).
5. M6 (Go 1.27, deps, govulncheck) and M12 (compileall, jlink or browser keygen, precomputed catalogue).
6. M1 + M8 + M9 (batched telemetry, lazy store, migration versioning, retention), then the section 4 telemetry schema.
7. M3 + section 6 (opaque spec, `/v1/{target}`), together with the multiplatform work.
