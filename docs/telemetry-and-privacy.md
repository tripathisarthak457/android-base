# Telemetry and privacy

What the website at [android-base.vercel.app](https://android-base.vercel.app) and its API keep about you, in your
browser and on the server, and for how long. Everything here is read from the code in this repository: the site in
`web/frontend`, the API in `web/api`, and the database tables in `web/api/internal/store/migrations`. A test in
`web/frontend/features/privacy/privacy.test.ts` fails when a stored column, a browser storage key, an outside origin or
a retention default is missing from this page.

The command line generator (`generator/create_project.py`) runs on your own machine and sends nothing anywhere.

## In short

- No account, no sign-up, no cookies and no third-party analytics.
- No IP address and no raw user agent string is stored. The API's logs leave both out, except for the address of a
  request that makes the code answering it crash (below). Visits are counted with a salted hash of your network that
  changes every day, and with a random id that lives only as long as your browser tab.
- For each project generated, the server keeps what it was generated with. Its app and package names, its server
  addresses, its deep link and its Apple team are removed after 90 days.
- A problem report keeps what you write in it, and an email address only if you give one.

## In your browser

| Key | Where | What it holds | When it goes |
|---|---|---|---|
| `android-base.session` | sessionStorage | A random id for this tab (a UUID v4 from `crypto.getRandomValues`) and whether this tab's visit has been counted | When the tab closes |
| `android-base.draft` | localStorage | Your configurator answers, so they are still there when you come back. No signing passwords, no date and no identifier. Written only once an answer differs from the defaults | **Start over**, or when your answers are back at the defaults |
| `theme` | localStorage | `light` or `dark`, only if you pick one with the theme switch | Picking **System** removes it |
| `android-base.admin-token` | sessionStorage | The operator's token, only on `/admin` | When the tab closes, or on signing out |

The site sets no cookies. A visit counts as returning only when a saved draft is found, so a visit that changes nothing
leaves nothing behind and does not make the next one returning; clearing the site's data or **Start over** resets it.

## What your browser sends to others

- **Google Fonts.** The design-style previews and the **Look** step load the preview typefaces (the suggestions and
  the one you type) from `fonts.googleapis.com` and `fonts.gstatic.com`, so Google sees your IP address and the
  typeface asked for, as with any font request. The site's own fonts are served from the site itself.
- **The host.** The site and the API run on Vercel, which handles the connection, so it sees your IP address as any
  host does and keeps its own logs under Vercel's privacy policy.

The page loads nothing else from outside: no analytics scripts, no advertising, no embedded content. Its content
security policy allows only its own origin, the API's, and the two Google Fonts hosts.

## On the server

The API needs your IP address while it answers a request: it is turned into the daily hash below, and it keys the rate
limits. The limits live only in the server's memory. Each one is cleared by the first request it counts after its hour
is up, or when the server restarts, so on a route nobody calls for a while an address stays in memory until someone
does. The address is never written to the database, and the API's own log lines leave it out. The exception is a
request that makes the code answering it crash: Go's HTTP server then logs the address the connection came from with
the error, which is yours when the API is exposed directly and the proxy's when one is in front of it. Your browser,
system and device are reduced to their family names and a class (`mobile`, `tablet`, `desktop` or `unknown`) as the
request arrives; the raw user agent string is not kept. Location comes from the country, region and city headers that
Vercel's edge adds, and only when the API runs behind Vercel.

### The daily hash

`visitor_hash` is the first 16 bytes of SHA-256 over a secret salt, the UTC date and your IPv4 address (or the /64
your IPv6 address belongs to). The same network on the same day gives the same hash, so a day's visitors can be
counted; the next day gives another, so your days cannot be linked together. The salt lives in the server's
environment (`IP_SALT`) and is never stored with the data; changing it forgets every hash.

### Each visit

One row in `visits` per browser tab, written when the page first loads in it. Kept for 400 days. The route the site
used before this version, `/api/track`, still writes a visit and its events, with no session id, until it is removed.

| Column | What |
|---|---|
| `day`, `at` | The date and time |
| `session_id` | The tab's random id |
| `visitor_hash` | The daily hash |
| `step` | Always `landed` |
| `returning_visitor` | Whether a saved draft was found |
| `referrer_origin` | The scheme and host of the page that linked here (never its path), or nothing for a direct visit or the site's own pages |
| `utm_source`, `utm_medium`, `utm_campaign` | A campaign's tags from the address, if it had them, at most 64 characters each |
| `source` | Worked out from those two: the campaign's source, else the referring site, else `direct` |
| `viewport` | The window's width as a bucket: `xs`, `sm`, `md`, `lg` or `xl` |
| `country`, `region`, `city` | From Vercel's edge. The admin portal lists a city only once at least 5 sessions share it |
| `browser`, `os`, `device_class` | Browser family, system family and device class |
| `client` | `web` for this site, `cli` for a script, `bot` for a crawler |

### The steps you take

One row in `events` per step, so the admin portal can see where people stop. Kept for 400 days.

| Column | What |
|---|---|
| `at` | When |
| `session_id` | The tab's random id |
| `name` | One of `landed`, `platform_selected`, `step_viewed`, `preset_chosen`, `generate_clicked` and `download_started`, which the site sends, and `generated`, `downloaded` and `download_interrupted`, which the API writes from what happened |
| `target` | The platform |
| `step` | For `step_viewed`: which configurator step |
| `value` | For `preset_chosen`: which preset |
| `client` | `web`, `cli` or `bot` |
| `config_ms` | On the events the API writes: how long the configurator had been open when **Generate** was pressed. A value over two hours is dropped |

### Each generated project

One row in `generations` per request to generate, whatever its outcome. Kept with no time limit, except the parts
marked below, which are removed after 90 days.

| Column | What |
|---|---|
| `created_at` | When |
| `target` | The platform |
| `outcome` | `ok`, `download_interrupted`, `invalid`, `internal_error`, `timeout`, `abandoned_queue`, `abandoned_running` or `rate_limited` |
| `succeeded` | Whether a zip was made; the column the first version of this table had |
| `spec` | The spec as it was sent, with every signing key reduced to its name: no password and no certificate subject. **After 90 days** its app name, display name, package name, server addresses, deep link and iOS bundle id and team are removed |
| `app_name`, `package_name` | **Removed after 90 days** |
| `preset`, `added`, `removed` | The preset nearest to what was chosen, and the features added to it or taken from it |
| `features`, `feature_modules` | The features it was generated with, and the names of the feature modules asked for |
| `architecture`, `design_style`, `motion_style`, `font_name`, `accent_colour`, `languages`, `glass` | The look and the build choices |
| `min_sdk`, `target_sdk` | The SDK levels |
| `notes`, `ignored_fields` | What the generator said about the spec: how it upgraded an older one, and the fields it did not use |
| `warnings` | The generator's warnings about the project. **Removed after 90 days** |
| `failure_reason`, `failure_field` | Why it failed, and which field. A refused spec's message often quotes the value refused, so it is **removed after 90 days** |
| `duration_ms`, `queue_ms`, `python_ms`, `stream_ms`, `cold_start` | Where the time went: waiting for a slot, generating, streaming the zip, and whether the generator was starting cold |
| `zip_bytes`, `generator_commit` | The zip's size and the generator's version |
| `session_id`, `visitor_hash`, `client` | As for visits |
| `config_ms` | How long the configurator had been open; a value over two hours is dropped |
| `country`, `device_class` | Coarse location and device class; no region or city |
| `referrer` | Written by the first version of the API only; empty since |

The zip itself is not kept: it is built in a temporary folder that is deleted once it has been streamed to you, and
signing keys exist only inside it.

`daily_rollup` holds each day's counts per platform, preset, outcome, country and device class, with the median and
95th percentile time to generate (`day`, `target`, `preset`, `outcome`, `country`, `device_class`, `n`, `p50_ms`,
`p95_ms`), with no time limit.

### A problem report

One row in `feedback` per report sent with **Report a problem**. Kept with no time limit.

| Column | What |
|---|---|
| `created_at`, `visitor_hash` | When, and the daily hash |
| `kind`, `severity`, `area` | What kind of report, how bad, and where |
| `title`, `body`, `steps` | What you wrote |
| `expected`, `actual` | What you expected and what happened, if a script sends them; the site's form does not ask |
| `contact` | An email address, only if you give one |
| `user_agent` | Browser family, system family and device class, as `browser · OS · device class`, derived by the API; never the raw user agent |
| `app_version` | The site's build |
| `target`, `app_name`, `package_name`, `preset`, `features`, `min_sdk`, `motion_style`, `font_name`, `accent_colour` | Your configurator answers, the platform among them, only if **Attach what you configured** stays ticked (a script may send them too). Unlike a generation's, these are not removed after 90 days |
| `page_url` | The page's origin and path, never its query or fragment, only with the answers attached |
| `status`, `notes`, `updated_at` | The maintainer's triage |

### Errors

One row in `errors` per distinct failure, grouped by a `fingerprint`, with the number of `occurrences` and when it was
`first_seen_at` and `last_seen_at`. Kept with no time limit; marking one `resolved` hides it until it happens again.

- A page error the site runs into keeps its `message`, its stack and the site's build in `detail`, and the page's
  path (never its query or fragment) in `path`.
- A generator failure keeps the generator's message and traceback. A traceback can include a temporary folder named
  after the project being generated.
- An API failure keeps its message and the route.

`kind` says which of the three it is.

### Request timings

One row in `requests` per call to the API, for its percentiles, except health checks and the admin portal's own
calls: `at`, `route`, `method`, `status`, `duration_ms` and `bytes_out`. Nothing about who called. Kept for 30 days.

## Who sees it

The admin portal at `/admin`, behind a token only the operator holds. The API sends the data nowhere else.

## Generated projects

A generated project never contacts this site or its API. It records how it was made in `generator-spec.json` at its
root, with signing keys left out, so it can be committed. Its analytics seam writes nothing until you bind a vendor;
Firebase Analytics and Crashlytics, if you tick them, report to the Firebase project whose `google-services.json` you
add.

## Running your own copy

Without `DATABASE_URL` the API records nothing at all and the admin portal is off. With it:

| Setting | Default | What |
|---|---|---|
| `IP_SALT` | none; required with `DATABASE_URL` | The daily hash's salt |
| `RETENTION_EVENTS_DAYS` | 400 | Days `events` and `visits` are kept |
| `RETENTION_NAMES_DAYS` | 90 | Days a generation keeps what identifies its project, and its warnings |
| `RETENTION_REQUESTS_DAYS` | 30 | Days `requests` are kept |
| `TRUSTED_PROXY` | `none` | Which address is the client's: `none` takes the connection's, for a server exposed directly; `vercel` behind Vercel, which also trusts its location headers; `hops=N` behind N proxies that append to `X-Forwarded-For` |

Retention runs once the database is ready and then every hour.
