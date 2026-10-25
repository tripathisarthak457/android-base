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
`generate_headless.py`, and Vercel's Go runtime image has no Python in it. The `Dockerfile` at the
repository root puts the Go binary, the interpreter, `generator/` and `templates/` in one image.

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
| `ENABLE_TARGETS` | `android` | Comma-separated targets this deployment generates. A target the catalogue lists and this leaves out is shown as coming soon, and its generate route answers 404 `target_unavailable`. The image sets `android` |
| `TRUSTED_PROXY` | `none` | Which `X-Forwarded-For` entry is the client: `none` (the connection), `vercel` (Vercel's edge, whose geo headers are then trusted too) or `hops=N` behind N proxies that append. The image sets `vercel`; anything else behind Vercel counts every visitor as the edge |

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

## No UI components, and the setup-only lock

The Android target declares one option, `ui_components` (`templates/android/options/ui_components.toml`).
A spec that leaves it out, or sets it to `true`, gets a project with the design system's components,
exactly as before the option existed. `"ui_components": false` gets a setup-only project: the
architecture, the theme skeleton and every build guard, without components. The generator drops the
features that need components (the catalogue's `disables`, each with a reason) with a note each,
rather than refusing a preset that lists them, and leaves the fields the option `hides` at their
defaults the same way, so a request that sends another design or motion style gets the defaults.

The site shows the option as the "No UI components" box, first on the Features step. It sends
`"ui_components": false` only when the box is ticked, so every other request is the one it always
sent. Ticking it greys the disabled features with their reasons, hides design and motion style
(the option's `hides`), and Review says what was skipped and why. The features and presets it keeps
but whose own words name what such a project lacks (a screen, a tab, a component, the performance
harness) read in the option's words instead: its `feature_copy` and `preset_copy`, which the wizard
and `--list-features --no-ui-components` show too, and the feature-modules hint reads its
`module_copy`, as the wizard's module question does. The Project step's summary lists the tabs only
for a project with components; one without them has no tab bar and opens on its blank start
screen, and the summary says that instead. Multiplatform and iOS have no such option yet; the site
says so where the box would be.

**The lock.** A deployment can generate setup-only projects and nothing else, until the Android
release brings the components. The switch is a `[locked]` table in the option file:

```toml
[locked]
value = false
note = "UI components come with the Android release. Until then this site generates setup-only projects: the architecture, the theme skeleton and every build guard, without components."
```

It reaches both halves through the catalogue, which records it as the option's `locked`: the image
computes its catalogue from the same commit at build (`CATALOGUE_FILE`, written by the `Dockerfile`),
and the site builds from the checked-in `frontend/lib/catalogue.json`. With it on, the site shows
the box ticked and fixed with the note, and marks each disabled feature "Coming with the Android
release". The site is not the guard. The API holds every request to the lock before it takes a
generator slot: a spec without the field (or with `null`, which the generator reads the same way)
counts as the default, `true`, and is refused like one that asks for `true`:

```
HTTP/1.1 403 Forbidden

{"error": "UI components come with the Android release. Until then …", "code": "option_locked", "field": "ui_components"}
```

A value that is not `true` or `false` is a 400 `invalid_spec` on `ui_components`, locked or not.
A catalogue whose lock has no boolean `value` or no `note` stops the API at boot: serving with the
lock ignored would generate exactly what it is there to refuse; that is all the boot checks of the
lock. `server -selftest` sends the locked value through the route's checks and the generator, but
nothing runs it on its own, neither the image's command nor CI: run it against an image before
deploying it. A refused request is recorded as an invalid generation with the note as its reason,
so the admin shows what people asked for while the lock is on.

The lock lives only on the branch that deploys setup-only; it never merges back into the
integration branches. The Android release removes it: delete `[locked]` from the option file, run
`python3 -m genkit.core.catalogue --write` in `generator/`, and that commit goes through the Android
release gate like any other.

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

## Signing keys

Off by default. Ask for them and the API generates all four — dev, staging, prod and playstore —
with `keytool`, exactly as the CLI does, and the zip comes back with `keys/*.jks` and a filled-in
`keystore.properties`. Ask for none and it ships `keystore.properties.template` and a README with
the four commands, which is what every request did before.

The warning on the form is the point of the feature, not decoration. A key created on a server the
visitor does not control and sent back over the wire is a key whose custody they cannot claim, and
losing control of a Play upload key is the one Android mistake that cannot be undone. That is a
decision to put in front of somebody, not one to make for them: most projects are not on Play yet,
and four `keytool` invocations are exactly the friction that leaves a team shipping release
variants signed with the debug key.

What the passwords touch, and nothing else:

* stdin of the `generate_headless.py` subprocess, and
* `keystore.properties` inside the zip that is streamed back.

They are not written to disk by the API, not recorded in `generations`, and not logged. Adding a
field to `generate.Keystore` that widens that path is a credential leak; `existing_path` is absent
from it for the same reason — the generator would read that file off this server and put it in the
zip.

`keystoresAvailable` in `/api/options` reports whether this host actually has a JDK. Without one
the site hides the offer rather than taking passwords and returning a zip with no keys in it, and
a key that `keytool` refuses individually comes back in the `X-Keystores-Skipped` header so the
visitor is told rather than finding out at release time.
