# The website

Three pieces:

```
frontend/   Next.js. Static — no server code, no API routes.
api/        Go. Runs the Python generator and records what happened.
deploy/     Caddyfile and an install script, for running the API on your own VPS instead.
```

Both halves deploy to one Vercel project as two [services](https://vercel.com/docs/services): the
site at `/`, and the API as a container at `/api`. They share a domain, which is why the browser
never needs a second origin and CORS never enters into it.

The API is a container rather than a plain Go build for one reason: it shells out to
`generate_headless.py`, and Vercel's Go runtime image has no Python in it. `Dockerfile.api` at the
repository root puts the Go binary, the interpreter, `generator/` and `template/` in one image.

---

## Why the API shells out to Python

The generator is not reimplemented in Go. `internal/generate` writes a JSON spec to
`generate_headless.py` on stdin and reads a JSON result back, so there is exactly one definition
of what a generated project is.

A Go port would be faster by a second or two and would be wrong within a month: the two would
drift, and the drift would surface as a project from the website that does not match the one from
the terminal — which is precisely the bug nobody thinks to look for.

---

## Running it locally

Two terminals.

```bash
# 1. The API. Without DATABASE_URL it records nothing and the admin routes are not registered.
cd web/api
GENERATOR_DIR=../../generator ALLOWED_ORIGINS=http://localhost:3000 go run ./cmd/server
```

```bash
# 2. The site.
cd web/frontend
npm install
echo 'NEXT_PUBLIC_API_BASE=http://127.0.0.1:8080' > .env.local
npm run dev
```

Open http://localhost:3000. The wizard's options are fetched from the API, so if the feature list
looks empty the API is not running.

### With the admin portal

```bash
createdb androidgen
cd web/api
DATABASE_URL='postgres://localhost/androidgen?sslmode=disable' \
ADMIN_TOKEN="$(openssl rand -hex 32)" \
IP_SALT="$(openssl rand -hex 32)" \
GENERATOR_DIR=../../generator \
ALLOWED_ORIGINS=http://localhost:3000 \
go run ./cmd/server
```

Migrations run at boot. Open http://localhost:3000/admin and paste the token.

---

## Deploying

### The API, on a VPS

One command on a fresh Ubuntu 24.04 box:

```bash
curl -fsSL https://raw.githubusercontent.com/tripathisarthak457/android-base/main/web/deploy/install.sh \
  | sudo bash -s -- api.yourapp.duckdns.org
```

It installs Go, Python, Postgres and Caddy, builds the binary, generates the secrets, writes a
systemd unit and gets a TLS certificate. It prints the admin token once — save it.

Running it again is how you deploy an update: it pulls, rebuilds, restarts, and leaves the
database and the generated `/etc/android-base.env` alone.

**You need a hostname**, not just an IP — Let's Encrypt will not issue for a bare address. A free
DuckDNS subdomain works exactly as well as a bought domain: create one, point it at the box, and
use it above.

### Both, on Vercel

From the repository root — not from `web/frontend`, because the container's build context is the
whole repository:

```bash
vercel --prod
```

`vercel.json` declares the two services and the routing between them. No `NEXT_PUBLIC_API_BASE` is
needed: same domain, so the site fetches `/api/options` relatively. Set it only if you split the
two across origins, in which case the API's `ALLOWED_ORIGINS` has to name the site — the CORS list
is exact-match with no wildcard, because `/api/generate` is a POST that returns a file and there
is no reason for any origin but the site to call it.

For the admin portal, attach a Postgres database and set `DATABASE_URL`, `ADMIN_TOKEN` and
`IP_SALT` in the project's environment variables. Without them the API still generates projects;
it just records nothing and does not register `/api/admin/*` at all.

---

## Configuration

Everything is an environment variable, and the two with no sensible default fail at boot rather
than at the first request that needs them.

| Variable | Default | Notes |
| --- | --- | --- |
| `ADDR` | `127.0.0.1:8080`, or `0.0.0.0:$PORT` | Loopback behind Caddy; every interface in a container, where `PORT` is injected |
| `DATABASE_URL` | — | Empty disables recording and the admin routes entirely |
| `ADMIN_TOKEN` | — | Required with `DATABASE_URL`; at least 24 characters |
| `IP_SALT` | — | Required with `DATABASE_URL`; rotating it forgets who visited |
| `GENERATOR_DIR` | `../../generator` | Must contain `generate_headless.py` |
| `PYTHON_BIN` | `python3` | `py` on Windows |
| `ALLOWED_ORIGINS` | `http://localhost:3000` | Comma-separated, exact match |
| `GENERATE_TIMEOUT` | `90s` | A full-feature project takes about 3s |
| `MAX_CONCURRENT_GENERATIONS` | `4` | Each is one Python process |
| `RATE_LIMIT_PER_HOUR` | `30` | Per client IP |

---

## The API

| | | |
| --- | --- | --- |
| `GET` | `/api/health` | Uptime, and whether recording is on |
| `GET` | `/api/options` | The generator's own catalogue. Cached at boot |
| `POST` | `/api/generate` | Spec in, zip out. Rate limited |
| `POST` | `/api/track` | One funnel step |
| `POST` | `/api/feedback` | A bug report or suggestion. Rate limited |
| `GET` | `/api/admin/overview` | Headline numbers and the funnel |
| `GET` | `/api/admin/daily?days=30` | Generations, failures and visitors per day |
| `GET` | `/api/admin/features` | Which options actually get picked |
| `GET` | `/api/admin/errors?resolved=false` | Grouped by fingerprint |
| `GET` | `/api/admin/generations?limit=50` | The most recent projects |
| `GET` | `/api/admin/health` | Per-route latency and 5xx rate |
| `GET` | `/api/admin/feedback?status=new` | Reports from people, newest first, blocking bugs pinned |
| `POST` | `/api/admin/errors/resolve` | Tick one off |
| `POST` | `/api/admin/feedback/update` | Set a status or add a triage note |

Admin routes live under `/api` like everything else the service owns — the site keeps the
`/admin` page that calls them. They take `Authorization: Bearer <ADMIN_TOKEN>`, compared in constant time. A shared token
rather than accounts, because there is one administrator and a login system for one person is a
login system whose password reset flow nobody ever tests.

```bash
curl -X POST https://android-base.vercel.app/api/generate \
  -H 'Content-Type: application/json' \
  -d '{"app_name":"My App","package_name":"com.acme.myapp","features":["network","catalog"]}' \
  -o MyApp.zip
```

---

## What is recorded, and what is not

Recorded: the app name, package, feature set, SDK levels and timings of each generation; which
funnel step each visitor reached, once per day; the duration and status of every request; any
error the generator produced, grouped so one bug is one row; and any bug report somebody sends,
along with the configuration they had on screen and their browser.

A report attaches that configuration only when the reporter leaves the switch on — the form shows
exactly what is going, field by field, before it is sent. Nothing in it identifies them, and the
email address is optional and used for one thing: asking a follow-up question.

Not recorded: IP addresses. Visitor counts use a salted hash truncated to sixteen bytes, and
rotating `IP_SALT` forgets who visited without losing the numbers. No cookies, no third-party
analytics, and the generated project is written to a temporary directory that is deleted as the
download finishes.

A rejection caused by what somebody typed — a bad package name, an unknown feature — is recorded
as a failed generation but deliberately **not** as an error. The error list is for bugs, and
filling it with typos would make it useless within a day.

---

## Signing keys are not generated here

The CLI generates all four with `keytool`, on your machine. The website generates none, whatever
the request asks for — `generate_headless.py` clears them before rendering.

A production upload key created on a server you do not control and sent back over the wire is a
key whose custody you cannot claim, and losing control of a Play upload key is the one Android
mistake that cannot be undone. The zip ships `keystore.properties.template` and the README has the
four commands.
