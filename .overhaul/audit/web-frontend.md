# Audit: `web/frontend` (Next.js configurator + `/admin`)

Repo: `/home/crazyhusir/Documents/projects/tools/android-base` @ `overhaul/multiplatform-v2` (HEAD `99c4edf`).
Area: `web/frontend/**`. Read-only. Cross-checked against `generator/genkit/*`, `template/**`,
`web/api/**`, `vercel.json`, `.github/workflows/ci.yml`, and PeriFlow's newer navigation/glass code.

**Not run:** `npx tsc --noEmit` / `next build`. `web/frontend/node_modules` does not exist and the
rules forbid `npm install`. Everything below comes from reading the code and running the generator's
own Python (catalogue, validators, colour maths) to check the frontend against it. All line numbers
point at the current files.

---

## 0. Executive summary

The site is better than most generator front-ends. The catalogue drives the feature list. The API
records `generated` itself rather than trusting the client. The admin token goes in
`sessionStorage`, not `localStorage`. There is a CSP. No user string is ever rendered as HTML.

The weak points are these:

1. **The configurator and the generator disagree on what a valid project is, and on what a project
   contains.** Unticking a feature leaves transitive dependents ticked. The generator then quietly
   adds the removed features back, so the UI and the telemetry under-report. The site does not
   check `versionName`, the colour hexes or `targetSdk ≥ minSdk`. Its keyword list for package
   names is missing 19 words the generator rejects. Each of these mismatches shows up only as a
   400 after the round trip, and the admin never shows those rejections.
2. **The previews have drifted from the template.** The colour ramp predates commit `99c4edf`.
   In about 19% of random accents the preview draws the wrong text colour on the dark-theme
   accent, and that includes the site's own Editorial showcase accent. Font weights 800/900
   are never loaded. The landing showcase loads three of its four families at weight 400 only.
   Feature modules become tabs in the generated app but not in the preview.
3. **The design and motion styles are hand-mirrored `Record<Key,…>` maps indexed by
   catalogue-supplied keys.** If the generator adds a style (a "Glass" style, for example), the
   Look and Motion steps throw.
4. **Performance.** Each keystroke or colour-picker drag in the configurator re-renders the whole
   landing page through `onContextChange → setReportContext`. On the Look step that is 9 full phone
   previews per input event. The whole page is a client component. The catalogue is fetched after
   hydration. Thirteen separate Google Fonts stylesheets load on the Look step.
5. **Accessibility.** The whole site has exactly two ARIA state attributes. Feature cards, presets,
   style pickers and segmented controls give no selected state to assistive tech. The modal has no
   focus trap. framer-motion ignores reduced motion. 36 usages of `text-ink-500` sit at
   2.3–2.7:1 contrast.
6. **Admin.** It shows 8 widgets. The header says "last 30 days", but success rate, durations,
   zip size and feature popularity are all-time. Features nobody picks cannot appear in "Feature
   popularity", which is the stated purpose of that chart. Referrer is always the site's own
   origin. Country is never written. Validation rejections are invisible. There are no date
   ranges, filters, pagination, CSV export or per-feature trends. A failed Refresh logs the admin out.

---

## 1. Findings by severity

| # | Sev | Category | Where | One line |
|---|-----|----------|-------|----------|
| F1 | high | bug | `components/configurator.tsx:231-246` | Unticking a feature removes only direct dependents; the generator re-adds what the UI shows as off |
| F2 | high | architecture | `components/look-step.tsx:145,162,249,295`, `components/app-preview.tsx:94,102`, `components/configurator.tsx:168-169` | Catalogue style keys cast to hard-coded maps; a new design/motion style (e.g. Glass) crashes the Look/Motion steps |
| F3 | medium | bug | `components/configurator.tsx:910-949` | `targetSdk` state can fall below `minSdk`; the select shows a different value from what is sent |
| F4 | medium | bug | `components/configurator.tsx:212-227,952-958`, `components/look-step.tsx:184-198,335-346`, `lib/app-theme.ts:5-8` | No client validation for versionName or colour hexes; an invalid hex silently previews as blue |
| F5 | medium | bug | `components/configurator.tsx:36-44` | "Mirrors validate_package_name" is false: 19 reserved words differ; `null`/`true`/`false`/Kotlin keywords pass both sides |
| F6 | medium | bug (cross-area) | `components/configurator.tsx:72-83`; `generator/genkit/spec.py:937-950` | Module names `class`, `object`, `fun`, `when`, `null` are accepted, become a package segment, and the build fails |
| F7 | medium | ux-design (fidelity) | `lib/app-theme.ts:86-103` | Preview colour ramp predates `99c4edf`: wrong dark on-accent text for ~19% of accents |
| F8 | medium | ux-design (fidelity) | `components/styles-showcase.tsx:35-38`, `components/font-picker.tsx:17`, `components/app-preview.tsx:204-233` | Showcase loads weights for the last family only; 800/900 never loaded; "heavier/extra-bold" type is not visible |
| F9 | medium | ux-design (fidelity) | `components/app-preview.tsx:42-57`; `generator/genkit/scaffold.py:41` | Feature modules become tabs in the generated app; the preview ignores them; no tab-count warning |
| F10 | medium | performance | `app/page.tsx:18-22,54`, `components/configurator.tsx:256-271` | Every input event re-renders the whole landing page (Hero, 4 showcase phones, FAQ…) plus 5 configurator phones |
| F11 | medium | performance / architecture | `app/page.tsx:1,24-39` | Whole page is `"use client"`; static catalogue fetched after hydration (waterfall, spinner, deploy-skew crashes) |
| F12 | medium | performance | `components/font-picker.tsx:40-42`, `app/layout.tsx:45-51` | 12 stylesheet requests (×3 weights) on Look step mount, plus a render-blocking third-party font CSS for the site |
| F13 | medium | responsive | `components/app-preview.tsx:62`, `components/look-step.tsx:88-91`, `components/configurator.tsx:393,420` | 280px live preview is wider than the 264px content box on a 360px phone and gets clipped |
| F14 | medium | ux-design | `components/look-step.tsx:62`, `components/configurator.tsx:404` | `lg:sticky` live preview never sticks: the Card's `overflow-hidden` makes it the scroll container |
| F15 | medium | a11y | `components/configurator.tsx:767-849`, `components/look-step.tsx:148-170,252-262`, `components/primitives.tsx:128-170` | Toggle-like controls expose no state; hidden "✓" read in both states; `div role=button` scrolls on Space |
| F16 | medium | a11y | `app/globals.css:14-34` | `ink-500` text 2.32–2.67:1 (36 usages); `ink-400` 4.42; amber 4.01; primary button label 4.41, all below AA |
| F17 | medium | security / privacy (honesty) | `components/hero.tsx:85`; `app/page.tsx:242-249`; `web/api/.../0001_initial.sql:16-17` | Hero says "nothing stored"; the FAQ and the DB store app name, package and features |
| F18 | medium | measurement | `app/admin/page.tsx:182`; `web/api/internal/store/store.go:199-207,304-311` | Header says "last 30 days"; success rate, p50/p95, zip and feature popularity are all-time |
| F19 | medium | measurement | `app/admin/page.tsx:471-502`; `store.go:304-311` | Feature popularity can't show 0% features ("candidates for deletion") and counts unresolved, preset-biased lists |
| F20 | medium | measurement | `lib/api.ts:187-198`; `web/api/internal/httpapi/server.go:283-300`; `vercel.json` | Referrer is always the site's own origin; UTM is dropped; `country` is never written; no device/viewport |
| F21 | medium | measurement | `app/admin/page.tsx:43-52,589-624` | Validation rejections (`failure_reason`) never shown; Recent shows time without date; failure/zip fields unused |
| F22 | medium | measurement | `components/configurator.tsx:320`; `server.go:224-236` | "Configured" means "clicked Generate"; there is no per-step funnel; "generated" is counted even when the download was interrupted |
| F23 | medium | architecture | `components/configurator.tsx` (1370 lines, 28 `useState`) | God component: prop drilling (BuildStep and ReviewStep have 22 props each), duplicated types, untestable request building |
| F24 | low-med | a11y | `components/feedback.tsx:172-189` | Modal has no focus trap or focus return; `role="dialog"` is on the backdrop |
| F25 | low-med | a11y | `app/globals.css:128-137` | Reduced motion is handled by CSS only; no `MotionConfig reducedMotion="user"`, so framer-motion springs still run |
| F26 | low-med | bug | `app/admin/page.tsx:118-126` | Any failed Refresh or mutation reload logs the admin out; the helpful network message is unreachable |
| F27 | low-med | bug | `app/admin/page.tsx:221-245` | Admin mutations ignore `response.ok`; no catch, so the rejection goes unhandled; every update reloads all 7 endpoints |
| F28 | low-med | bug | `app/admin/page.tsx:93`, `components/feedback-panel.tsx:63` | Status filter is client-side over the newest 100 reports; older "new" reports are invisible |
| F29 | low-med | ux-design | `app/admin/page.tsx:551-570`; `server.go:191` | Generation errors have no `detail`, and the message is one truncated line, so the stack trace is unreadable |
| F30 | low-med | ux-design | `components/configurator.tsx:347,355-356` | staging URL forced to the dev URL; deep-link host fixed to `example.com`; WebSocket URLs not settable |
| F31 | low-med | ux-design | `app/page.tsx:27-35,58-68` | Catalogue failure has no Retry (Vercel container cold start leaves the page dead) |
| F32 | low-med | ux-design (fidelity / forward) | `components/motion-preview.tsx:58-69`, `components/app-preview.tsx:108-150,764-797` | Push dims the outgoing screen (PeriFlow removed that); the bar sits below the content instead of over it; the indicator animates `left` |
| F33 | low | ux-design | `components/configurator.tsx:62-69` | App name doubles as launcher label but is ASCII-only, so "Café" or a Hindi name is impossible despite `hi` being offered |
| F34 | low | ux-design | `components/configurator.tsx:154,204` | No warning that Play rejects `com.example.*` ids (the default) |
| F35 | low | ux-design | `components/configurator.tsx:245,156` | Preset is stored rather than derived; a set equal to a preset still shows "Custom" |
| F36 | low | ux-design | `components/configurator.tsx:1132-1137`, `components/look-step.tsx:211-216` | Review/thumbnail fall back to the Settings screen even when `settings` is off |
| F37 | low | ux-design (fidelity) | `components/app-preview.tsx:668` | Preview's Haptic switch always on, whatever the Motion step's haptics choice |
| F38 | low | ux-design | `components/configurator.tsx:185-192,376,1296-1343` | `done`/`failure` go stale after edits; a stale "Downloaded" card shows while generating again |
| F39 | low | ux-design | `app/admin/page.tsx:263-289` | 7 headline cards in `lg:grid-cols-6` leave one orphan card |
| F40 | low | security | `app/layout.tsx:17` | `/admin` inherits `robots: index`; no `noindex` |
| F41 | low | bloat | `lib/app-theme.ts:84-103,215-221,364-367`, `app/globals.css:42-90,164-171`, `package.json:9` | Dead exports and ramps, unused `.grain`, duplicated dark palette, `next lint` script with no ESLint |
| F42 | low | ai-slop | `components/primitives.tsx:8-15,69-74,149-152`, `components/configurator.tsx:71`, `components/hero.tsx:176`, `components/feedback.tsx:20`, `lib/api.ts:196` | Comments that contradict the code, narrate history or explain library basics |
| F43 | low | deprecated | `package.json:9`, `app/layout.tsx:45-51`, whole tree | `next lint` (removed in Next 16), no `next/font`, effects used for derived state, no RSC split |
| F44 | low | docs (forward) | `components/included.tsx:10`, `app/page.tsx:254` | "Its own ripple" and "Everything is MIT" must change if the overhaul drops ripple / ports Apache-2.0 Kyant catalog code |

---

## 2. Configurator correctness

### F1: Unticking a feature leaves transitive dependents on (high, bug)

`components/configurator.tsx:231-246`

```ts
if (next.has(key)) {
  next.delete(key);
  for (const other of catalogue.features) {
    if (next.has(other.key) && other.requires.includes(key)) next.delete(other.key);
  }
}
```

`requires` holds direct dependencies only. `implies` is the transitive closure
(`genkit/catalogue.py:142`: `sorted(resolve_features({feature.key}) - {feature.key})`). Checked
against the real catalogue:

- `push.requires = ['firebase']`, `firebase.requires = ['analytics','flags']`. Untick
  **analytics**: firebase is removed, **push stays ticked**. The UI now shows push on and
  firebase/analytics off. The request carries `push` without `firebase`.
  `ProjectSpec.validated()` runs `resolve_features`, so the zip contains firebase + analytics +
  flags anyway.
- Untick **network**: websocket, room, sample, auth, paging and search go. **database** (needs
  room) and **googlesignin** (needs auth) stay, and network comes back in the zip.
- Untick **forms**: auth goes, **googlesignin** stays.

Consequences: the Review step says "N features" and lists keys that are not what ships. The
generator re-adds features the visitor explicitly turned off, with no explanation.
`generations.features` (`server.go:286`) records the *unresolved* list, so feature popularity
is wrong too.

Fix (one token):

```ts
for (const other of catalogue.features) {
  if (next.has(other.key) && other.implies.includes(key)) next.delete(other.key);
}
```

Also show what changed: "Also removed: push, firebase" with Undo, and on tick "Also adds:
analytics, flags" from `implies` (only `requires` is shown today, at `configurator.tsx:837-841`). The
amber "X depends on this" line (`842-846`) should use `implies` too.

### F3: `targetSdk` can fall below `minSdk` (medium, bug)

`configurator.tsx:936-949` filters the target options to `level >= minSdk`, but `targetSdk`
state is never clamped. Set target to 30, then min to 33. React holds `value={30}` with no
matching `<option>`: the browser shows a blank or first option while the state stays 30. Review
shows "33 / 30" (`1217`) and the generator rejects it ("targetSdk cannot be lower than minSdk",
`spec.py:818-819`). Fix: clamp inside `setMinSdk` (`setTargetSdk(t => Math.max(t, next))`), and
add `errors.sdk`. Consider warning below Play's current target-API floor rather than offering 24
as a target.

### F4: versionName and colour hexes are not validated (medium, bug)

- Generator: `versionName` must match `^\d+\.\d+\.\d+$` (`spec.py:829`). The site has a bare
  `TextInput` (`configurator.tsx:952-958`) with no error, and `ReviewStep.problem`
  (`1199-1206`) never mentions it. "1.0" or "1.0.0-beta" round-trips to a 400.
- Generator: accent, secondary and tertiary must be 6-digit hex (`spec.py:891-899`).
  `BrandColourRow` (`look-step.tsx:335-346`) accepts anything. `hexToRgb` (`app-theme.ts:7`)
  **silently substitutes `[0.17,0.42,0.93]`** for an invalid value, so typing `#2C6` previews a
  convincing blue while the request will be rejected. `<input type="color" value="#2C6">` also
  logs a format warning and shows black.
- Secondary/tertiary editing glitch: the text field's `value={resolved}` is `secondary ||
  deriveSecondary(accent)` (`look-step.tsx:188,343`). Clearing the field re-fills it with the
  derived hex under the caret, so you cannot clear-and-retype.

Each of these becomes a *failed generation* in telemetry (`server.go:180-183` records
validation rejections as `succeeded=false`), which drags down the admin "Success rate".

Fix: one `validate(state, rules)` module. The **rules come from the catalogue** (see §7). Keep the raw
text of each colour field in local state and commit only valid hexes.

### F5: The reserved-word list does not mirror the generator (medium, bug)

`configurator.tsx:36` says it "Mirrors `validate_package_name`". The generator rejects
`keyword.iskeyword(segment) or segment in _JAVA_KEYWORDS` (`spec.py:930`). I diffed the two sets by
running the generator. **Server-only (the site accepts them, the server rejects them):** `and,
async, await, boolean, def, del, elif, except, from, global, lambda, nonlocal, not, or, pass,
raise, strictfp, with, yield`. So `com.acme.from` passes the form and fails on the server.

Both sides **accept** `null`, `true`, `false` (Java literals: the AGP R class `package
com.example.null;` will not compile) and the Kotlin hard keywords `typealias` and `typeof` (verified:
`validate_package_name('com.example.null')` is accepted). The Python keywords are irrelevant to
Kotlin and should leave the generator. The literals and Kotlin keywords should be added. Either
way, the list should be served by the catalogue (`reservedPackageWords`) instead of being copied.

### F6: Keyword module names break the build (medium, cross-area bug)

`moduleError` (`configurator.tsx:72-83`) and `validate_module_name` (`spec.py:937-950`) both accept
`class`, `object`, `fun`, `when`, `null`, `true`, `typealias`. I verified each through
`ProjectSpec(...).validated()`. The name becomes a package segment: `scaffold.py:27`
`f"{spec.package_name}.feature.{name}"` and `scaffold.py:127` `package {pkg}.data.{name}`. The
generated project does not compile. Fix in the generator (reuse the package-segment keyword check)
and let the site pick it up from the catalogue.

### F30: Generator capabilities the site hides (low-med, ux)

- `api_base_urls.staging = devUrl` (`configurator.tsx:347`), while the template default is a
  separate `https://staging.example.com/api/` (`template/build-logic/.../AppConfig.kt:54`).
- `deeplink_host: "example.com"` is hard-coded (`356`). The generator validates a real host
  (`spec.py:715,888-889`). Shipping App Links for `example.com` is never what anyone wants.
- `web_socket_urls` are never sent, even when `websocket` is ticked, so the zip keeps
  `wss://dev.example.com/ws`.

### F31 and F38: Error states and retry (low-med)

- Catalogue failure (`app/page.tsx:27-35`) sets a permanent message whose only action is "Get the
  CLI instead" (`58-68`). There is no Retry, and the API is a Vercel *container* service with cold
  starts. Add Retry with backoff (2–3 attempts) before showing the CLI fallback.
- `generate()` has no timeout or cancel. `done` is not cleared when generation starts again, or
  when the config changes after a download (`376`, `1296-1333`). A failure renders *below* a stale
  "Downloaded" card. `failure` is cleared only by the next attempt.
- The failure and success panels have no `role="alert"`/`aria-live`. Screen-reader users get no
  result.

### F33–F36: Smaller correctness and UX gaps

- **F33** `appNameError` (`62-69`) and `_APP_NAME` (`spec.py:702`) are ASCII-only. The app name is
  also the launcher label, so "Café" or "मेरा ऐप" are impossible, even though `hi` is offered as
  a language. Split this into a *display name* (any Unicode, at most 30 characters for Play) and a
  *code name* (derived, ASCII).
- **F34** The default `com.example.myapp` (`154`, `204`) is rejected by Play Console. Show a
  non-blocking warning.
- **F35** `preset` is separate state, set to `"custom"` on any toggle (`245`). Derive it instead:
  `presets.find(p => sameSet(resolve(p.features), resolved))?.key ?? "custom"`.
- **F36** `reviewScreen()` (`1132-1137`) and `thumbnailScreen()` (`look-step.tsx:211-216`)
  return `"settings"` even when `settings` is off. With features = `{network}`, Review shows a
  Settings screen the app does not have. Three separate "which screen" functions exist
  (`reviewScreen`, `thumbnailScreen`, `screensFor`); collapse them into one.
- Signing: alias `${pascal(appName).toLowerCase()}-${name}` (`303-306`) can exceed the generator's
  64-character `_ALIAS` limit (`spec.py:957`) for long names, because there is no app-name length
  cap.
- The feature-module hint (`673`) never says each module also becomes a **bottom-bar tab** (see F9).

---

## 3. Preview fidelity (app-preview, motion-preview, styles showcase)

What is right: `MOTION_TOKENS` equals `AppMotion.kt` value for value. `DESIGN_TOKENS` equals
`AppDesignStyle.kt`. The tab fade-through (fade + 0.985 scale, `instant` delay,
`instant*1.5` exit) equals `NavTransitions.tabSwitch` (`template/core/navigation/.../NavTransitions.kt:105-113`).
`pressPop` equals `composeSpring(1, k*1.6)`. All preview copy exists in the template's
`strings.xml` (verified string by string).

### F7: Colour ramp predates commit `99c4edf` (medium)

`lib/app-theme.ts:86-103` still uses the old rule. In dark mode it always uses
`on: "#06101f"`. In light mode it compares against pure black (`(lum+0.05)/0.05`). It never
applies the generator's `_legible()` darkening. The generator now does
(`generator/genkit/render.py`, `brand_shades`, `_readable_on`):

```py
light = _legible(base, LIGHT_INK)
dark  = _legible(_dark_variant(light), DARK_INK)
on_dark = White if contrast(dark, white) >= contrast(dark, DARK_INK) else 0xFF06101F
```

I ran the generator's own functions against the preview's formulas over 2,000 random accents
(seed 1):

| Mismatch | Count |
|---|---|
| dark-theme text on accent: generator **white**, preview `#06101f` | **389 / 2000 (19%)** |
| accent shade differs (light or dark) | 111 / 2000 |
| light-theme text on accent differs | 66 / 2000 |

Concrete case: the site's own Editorial showcase accent `#1F4D3A` (`styles-showcase.tsx:13`).
The generator renders the dark accent `#527365` with **white** text. The preview draws near-black
on it, so the preview shows a contrast failure that the generated app does not have.

Fix: port the three functions exactly. Add a golden test: the generator writes
`tests/fixtures/ramps.json` (accent → light, dark, onLight, onDark for ~50 accents) and a tiny
node test (or a Python test that evaluates the TS through a JSON export) compares the two. This
stops the next drift. Better long-term: have the catalogue return the computed ramp for the
chosen accent from a `/api/palette?accent=` endpoint, or compute it once from a WASM-free shared
JSON table.

### F8: Font weights (medium)

- `styles-showcase.tsx:35-38`:
  ```ts
  const families = STYLES.map(s => s.font.replace(/ /g,"+")).join("&family=");
  link.href = `…css2?family=${families}:wght@400;600;700&display=swap`;
  ```
  produces `family=DM+Sans&family=Plus+Jakarta+Sans&family=Fraunces&family=Nunito:wght@400;600;700`.
  Only **Nunito** gets weights. The other three load at 400 only, so the landing showcase draws
  every heading in synthetic bold. The fix is to map each family to `${f}:wght@400;600;700;800;900`
  before joining.
- `font-picker.tsx:17` loads `400;600;700`, but `typeFor` uses **800** (Bold voice: display,
  heading, button) and **900** (Rounded voice: display), at `app-preview.tsx:208-232`. The browser
  substitutes the 700 face. So the "Pill buttons, heavier headlines" and "extra-bold type" traits
  (`app-theme.ts:307,309`) are not visible. Request `400;600;700;800;900` and let Google clamp to
  the family's axis.

### F9: Feature modules are tabs in the app, absent in the preview (medium)

`generator/genkit/scaffold.py:41`: `shell_tabs = [shell_tab(name) for name in spec.feature_modules]`,
inserted at `template/app/.../AppDestinations.kt:40` (`// <generated:shell-tabs>`, after Search,
before Profile). `PreviewConfig` (`app-preview.tsx:23-32`) has no modules and `tabsFor` (`42-50`)
ignores them. Typing `orders, cart, inbox` with the Everything preset generates **8 tabs**
(home, feed, search, orders, cart, inbox, profile, settings). The preview shows 5, and nothing
warns that 8 tabs do not fit a phone bar. Add `modules` to `PreviewConfig`, render module tabs with
a generic icon, and warn above 5 tabs (suggest "tab: no" per module, which the generator would need
to support).

### F32: Motion and bar fidelity against the overhaul (low-med, forward-looking)

- `motion-preview.tsx:58-61,66-69` dims the outgoing list to `opacity: 0.72`. That matches
  today's template (`NavTransitions.kt:30-33`, `fadeOut(targetAlpha = 1 - outgoingDim)`), but
  PeriFlow removed the fade for performance with glass (`periflow …/core/navigation/NavTransitions.kt:21-35`:
  "Push and pop only slide… a fade renders the whole screen, every piece of glass in it, into an
  offscreen copy each frame"). When that fix is backported, update the preview in the same change,
  as AGENTS.md "The website must not over-promise" requires.
- Structure: `AppPreview` lays the bar out *below* the content (`flex flex-col`, `108-150`). The
  template's `AppShell` overlays the bar on content that extends beneath it
  (`AppShell.kt:220-238`, `rootBottomInset`). For a floating or glass bar, the content must run under
  it, or a glass bar shows nothing through it.
- The bar indicators animate `left: calc(...)` (`app-preview.tsx:773,795`), a layout property, every
  frame. The template uses `Modifier.offset` (placement only). Animate `x` instead:
  `animate={{ x: \`${selected * 100}%\` }}` on a pill sized `(100% - 12px)/n`.
- Selected icon scale 1.06 on docked/minimal/chunky bars (`AppBottomBar.kt:255-259`) is not
  previewed. Colour transitions are fixed at 250ms instead of `motion.medium`
  (`app-preview.tsx:734`, `Icon` at `833`).

### F37: Preview haptics switch

`app-preview.tsx:668` `<Switch ctx={ctx} on />` is always on, while the configurator holds
`haptics` and never passes it to `PreviewConfig`. Settings rows for `applock`, `licenses` and
`browser` are also missing.

### Liquid glass in the configurator (what the overhaul needs here)

If the overhaul adds optional Kyant glass (as PeriFlow has: `core/designsystem/.../foundation/Glass.kt`,
`GlassBottomBar.kt`, `GlassRail.kt`, and `kyant-backdrop 2.0.1` in `gradle/libs.versions.toml:43`):

1. **Catalogue first.** Make glass either a feature (`glass`) or an `AppTheme` axis. Either way the
   site must render it from catalogue data, never from a new hand-written `Record` (see F2).
2. **Preview.** Restructure `AppPreview` so the content scrolls under an absolutely positioned bar
   (above), then emulate glass in CSS from PeriFlow's tokens (`Glass.kt:113-121`:
   Subtle/Regular/Strong = blur 6/10/16dp, refraction 8/16/24 height):
   ```ts
   const glass = {
     background: `color-mix(in srgb, ${p.surface} ${p.dark ? 38 : 55}%, transparent)`,
     backdropFilter: `blur(${dp(10)}px) saturate(1.6)`,
     border: `0.75px solid color-mix(in srgb, white ${p.dark ? 18 : 60}%, transparent)`,
     boxShadow: `inset 0 1px 0 rgba(255,255,255,${p.dark ? .12 : .5}), 0 8px 24px rgba(0,0,0,.18)`,
   };
   ```
   Use `backdrop-filter` **only in the one live preview**. In the 4 thumbnails, 4 showcase phones
   and the Review thumbnail, use the "drawn glass" approximation PeriFlow already uses for content
   depth (`Glass.kt:146,179-200`: tinted body + rim + sheen, no blur). Nine stacked
   `backdrop-filter` layers inside `transform: scale()` will drop frames.
3. **Honest copy.** Real blur needs RenderEffect (API 31+). PeriFlow falls back to frosted glass
   below that (`Glass.kt:128-130`). With the default `minSdk 26`, the Build step's min-SDK hint
   should say "Glass is frosted (no blur) on Android 8–11".
4. **Licence.** `LiquidInteraction.kt:1-5` is adapted from Kyant0/AndroidLiquidGlass under
   **Apache-2.0**. If it is ported into the template, the FAQ line "Everything is MIT"
   (`app/page.tsx:254`) and the generated README need a third-party notice (F44).

---

## 4. Performance

### F10: Landing-page re-render cascade (medium)

`app/page.tsx:18` holds `reportContext` in **Home's** state. The Configurator publishes on every
change (`configurator.tsx:256-271`, deps include `appName, packageName, accent, fontName…`). So
every keystroke, and every `input` event while dragging `<input type="color">`
(`look-step.tsx:336-339`), does this:

1. Configurator re-renders. On the Look step that means **5 `AppPreview`s**: 4 design-style
   thumbnails at `look-step.tsx:157` plus the live phone at `90`. Each recomputes `palette()`
   (HLS maths, 3 ramps) and `typeFor`, and diffs ~150 styled nodes.
2. `setReportContext` re-renders **Home**. No child is memoised (0 `memo(` in the tree), so Hero,
   `StylesShowcase` (**4 more `AppPreview`s**, `styles-showcase.tsx:81-92`), Included, Faq, Creator,
   Footer and FeedbackButton all re-render.

That is about 9 phone trees per input event while dragging a colour.

Fix (pick all three, they are small):

```tsx
// page.tsx: no state in Home
const contextRef = useRef<ReportContext>({});
<Configurator catalogue={catalogue} contextRef={contextRef} />
<FeedbackButton getContext={() => contextRef.current} />   // read when the modal opens
```

- `const deferred = useDeferredValue(previewConfig)` and feed previews the deferred value.
- `memo` on `AppPreview`/`ScaledPreview`, with a stable `previewConfig` from `useMemo`. Or enable
  the React Compiler (`reactCompiler: true`, `babel-plugin-react-compiler`), which removes this
  whole class of problem.

### F11: Everything is a client component (medium)

`app/page.tsx:1` `"use client"` makes Hero, Included (11 copy blocks), FAQ (9 Q&As), Creator and
Footer ship as JS and hydrate. Only Nav/ThemeToggle, StylesShowcase, Configurator and FeedbackButton
need the client. framer-motion (`motion.*`) is imported in 13 files for entrance fades that CSS
`@starting-style`/`animation-timeline: view()` or a single `LazyMotion` + `m` would cover.

The catalogue is static per deploy, yet it is fetched in `useEffect` after hydration
(`page.tsx:24-39`). The configurator renders a spinner first, and the Hero's feature count shows
"—" (`hero.tsx:176-180`). The deploy-skew crash fixed in `b12122b` (an old cached catalogue
without `languages`) is a symptom of the same design. Better options:

- CI already runs the generator. Have it emit `catalogue.json` into `web/frontend/` at build time
  (`python3 create_project.py --list-features --json > web/frontend/lib/catalogue.json`) and import
  it. The page and the catalogue then deploy atomically, with no fetch, no waterfall and no skew.
  Keep `/api/options` for the CLI and agents.
- Or fetch it in a Server Component with `revalidate`.

### F12: Fonts (medium)

- `font-picker.tsx:40-42` calls `loadGoogleFont` once **per suggestion**: 12 `<link>`s, each
  `wght@400;600;700`, the moment the Look step mounts, plus up to 36 woff2 files as chips render.
  Use one `css2` request with all 12 `family=` params and `&text=` set to the chip labels' glyphs
  (Google then serves tiny subsets), and load the full family only for the one actually chosen.
- `app/layout.tsx:45-51` loads the site's own three families through a render-blocking third-party
  stylesheet. `next/font/google` self-hosts them, removes the blocking request, prevents layout shift
  and avoids sending every visitor's IP to Google on load. That matters for the "nothing stored / no
  third parties" stance (F17).

### Smaller performance notes

- `motion.button {...press}` adds `whileHover: {scale: 1.01}` (`primitives.tsx:13-17`) to every
  card, including the design-style cards that *contain an entire scaled phone* (`look-step.tsx:148-157`).
  Hovering re-rasterises the whole subtree, and text blurs during the scale.
- `useEffect` used for derived state: package suggestion (`configurator.tsx:201-205`; derive it in
  the `setAppName` handler) and LivePreview screen sync (`look-step.tsx:55-59`; `activeScreen` is
  already derived, so the effect is redundant). Each adds an extra render per keystroke.
- Good, keep: `StylesShowcase` ticks only while in view and not reduced-motion
  (`styles-showcase.tsx:28-32`). `MotionPreview` loops only in view (`motion-preview.tsx:40-41`).

---

## 5. Accessibility

### F15: Toggle-like controls expose no state (medium)

There are exactly two ARIA state attributes in the tree (`aria-pressed` on languages,
`configurator.tsx:697`; `aria-checked` on `Toggle`, `primitives.tsx:110`). Missing:

- Feature cards (`configurator.tsx:810-849`): `motion.button` with no `aria-pressed`. The check glyph
  `✓` (`822-831`) is always rendered, with `text-transparent` when off, so a screen reader announces
  "check mark" for **unticked** features too. Use `role="checkbox" aria-checked={on}`, or
  `aria-pressed`, and `aria-hidden` on the glyph.
- Preset chips (`767-781`), design-style cards (`look-step.tsx:148-170`), LivePreview screen tabs
  (`look-step.tsx:65-76`), `Segmented` (`primitives.tsx:143`), feedback kind/area/severity buttons
  (`feedback.tsx:216-279`): none expose a selected state. These are radio groups:
  `role="radiogroup"` / `role="radio" aria-checked`.
- Motion-style cards are `div role="button" tabIndex=0` (`look-step.tsx:252-262`). Space calls
  `setMotionStyle` without `preventDefault()`, so the page also scrolls. There is no selected state.
- StepBar (`configurator.tsx:577-609`) lacks `aria-current="step"`. On a step change, focus stays on
  "Next" and the new panel has no heading to move focus to.
- `Field` (`primitives.tsx:54-77`) never sets `aria-invalid` / `aria-describedby`. Its comment
  (`69-73`) says "The hint stays put when an error appears rather than being replaced by it", and the
  very next line does the opposite (`{hint && !error && …}`, `74`). Either the comment or the code is
  wrong. The comment describes the better behaviour.

### F16: Contrast (medium)

Computed from `app/globals.css:14-34` (light) and `42-63` (dark):

| Token pair | Light | Dark | Used for |
|---|---|---|---|
| `ink-500` on `ink-950/900/850` | **2.32 / 2.49 / 2.67** | 3.33 / 3.15 / 2.98 | 36 usages: group captions (`configurator.tsx:801`), "needs …" (`838`), step counter (`539`), admin subtitles |
| `ink-400` on `ink-900` | **4.42** | 5.57 | most `text-xs` hints |
| `amber` on `ink-900` | **4.01** | 9.14 | the signing warning paragraph (`configurator.tsx:1061`) |
| `on-accent` on `accent` | **4.41** | 6.08 | every primary button label (15px, not "large") |
| `accent` on `ink-950` | 3.95 | 6.05 | eyebrow labels (`styles-showcase.tsx:48`, `text-xs`) |

The site promises the generated app is "checked against WCAG AA in both themes" (`included.tsx:51`)
while failing AA itself. Darken light `ink-500` to about `#857865`, `ink-400` to about `#71665a`,
`amber` to about `#94600f`, and `accent` to about `#c2401b`.

### F24: Feedback modal (low-med)

`feedback.tsx:172-189` has `role="dialog" aria-modal` on the **backdrop** `motion.div`, not on the
panel. There is no initial-focus management beyond `autoFocus` on the title, no focus trap (Tab walks
into the page behind), and no focus return to the trigger on close. Use a native `<dialog>` with
`showModal()`, which gives trap, Escape and inert-behind for free, or a small focus-trap hook.

### F25: Reduced motion (low-med)

`globals.css:128-137` zeroes CSS transitions only. framer-motion springs (hero entrance, step slide
`configurator.tsx:415-419`, `whileHover/whileTap` everywhere, `Segmented` layout pill, the preview's
tab and bar springs, the admin bars) still run. The comment there claims nothing else carries meaning,
but these are motion, not meaning. Wrap the tree in `<MotionConfig reducedMotion="user">` in
`layout.tsx`.

### Others

- The admin chart has `role="img"` with no accessible name (`admin/page.tsx:401`).
- Busy and result states lack `aria-live` (see F31/F38).
- External links are rendered as `Button onClick={() => window.open(...)}`
  (`app/page.tsx:64`, `hero.tsx:73-76`). Use real `<a>` elements: middle-click, copy link, and
  correct semantics.

---

## 6. Responsiveness and mobile

### F13: Live preview clipped on phones (medium)

Horizontal chrome around the live phone: section `px-6` (`configurator.tsx:393`) + step panel `p-6`
(`420`) gives 96px. On a 360px phone the content box is **264px**. `AppPreview` is fixed at
`width = 280` (`app-preview.tsx:62,87`), centred in `flex justify-center` (`look-step.tsx:89`)
inside `Card overflow-hidden` (`configurator.tsx:404`), so 8px is clipped from each side. At 375px
there is 279px, still 1px short. Fix: render the live phone through `ScaledPreview` with
`scale = min(1, containerWidth/280)` from a `ResizeObserver`, or a CSS container query, on every
breakpoint.

Also on mobile the grid collapses to one column, so the live phone sits **below** four style cards,
three colour rows and the font picker. Nobody sees a colour change without scrolling. Add a small
sticky preview (a bottom-docked mini phone or a "Preview" sheet) under `lg`.

### F14: `lg:sticky` live preview never sticks (medium)

`look-step.tsx:62` `className="lg:sticky lg:top-24"`. Its ancestor Card has `overflow-hidden`
(`configurator.tsx:404`), which makes the Card the sticky scroll container. The Card does not scroll,
so the phone scrolls away on desktop. Use `overflow-clip`, which clips without creating a scroll
container and still clips the step slide.

### Other mobile notes

- The floating "Report a bug" pill (`feedback.tsx:73`, `fixed bottom-5 right-5`) can cover the
  configurator footer's **Next** button (`configurator.tsx:542-548`) when the footer reaches the
  bottom of the viewport. Hide it while the configurator footer is on screen on small widths, or move
  it into the Nav.
- StepBar (`configurator.tsx:572`) scrolls horizontally, but the active step is never scrolled into
  view on change.

---

## 7. Architecture and code organisation

### F2: Catalogue keys index hand-written maps (high)

`DesignStyleKey`/`MotionStyleKey` are closed unions (`app-theme.ts:226,314`). The site still renders
styles **from the catalogue** and casts:

- `look-step.tsx:145` `const key = style.key as DesignStyleKey;` then `DESIGN_TRAITS[key].map`
  (`162`) and `ScaledPreview … designStyle: key` → `DESIGN_TOKENS[key]` (`app-preview.tsx:94`) →
  `t.surfaces` throws.
- `look-step.tsx:249` `style.key as MotionStyleKey` → `MotionNumbers` → `MOTION_TOKENS[styleKey]`
  (`295`) → `t.pressScale` throws. `app-preview.tsx:102` `MOTION_TOKENS[config.motionStyle].medium`.
- `configurator.tsx:168-169` casts `catalogue.defaults.motionStyle/designStyle`.

Adding a 5th style to the generator (the overhaul's likely "Glass", or a new motion style)
**crashes the Look and Motion steps** for every visitor. A browser holding an older bundle hits the
same problem in reverse. This is the `b12122b` crash again, in a new place.

Fix: put the design and motion **tokens in the catalogue**. Have the generator parse `AppMotion.kt` /
`AppDesignStyle.kt`, or better, make a JSON file the single source that generates the Kotlin
defaults. The site then imports no hand-mirrored numbers. Until then, filter
`catalogue.designStyles` to keys present in `DESIGN_TOKENS` and fall back to `Utility`/`Standard`.

### F23: `configurator.tsx` is a god component (medium)

1370 lines. `Configurator` alone holds **28 `useState`** (`152-192`). It drills 22 props into
`BuildStep` (`861-909`) and 22 into `ReviewStep` (`1139-1197`). The `done` type is written twice
(`187-192`, `1182-1187`) and the `errors` shape twice (`212-227`, `1188-1196`). The request builder
`generate()` (`317-388`) mixes UI state, side effects and spec building, so none of F1/F3/F4 is
unit-testable. There are no frontend tests at all.

Proposed structure:

```
web/frontend/
  app/(site)/page.tsx                 RSC: Hero, Included, Faq, Creator, Footer (static)
  app/(site)/configure-island.tsx     "use client" island
  app/admin/layout.tsx                metadata: robots noindex
  app/admin/page.tsx                  shell only
  features/configurator/
    state.ts        type ConfigState; reducer(action); initialState(catalogue)
    resolve.ts      resolveFeatures(set, catalogue) using `implies`; derivePreset()
    validate.ts     validate(state, catalogue.rules): typed errors per field
    request.ts      toGenerateRequest(state, catalogue): pure, unit-tested
    steps/{Identity,Features,Look,Motion,Build,Review}Step.tsx
    useConfigurator.ts   useReducer + derived values + generate()
  features/preview/
    theme.ts        ramp/palette (ported from render.py; golden-tested)
    AppPreview.tsx, BottomBar.tsx, screens/*.tsx, MotionPreview.tsx
  features/admin/
    api.ts          typed fetchers; one useAdminQuery(path, params)
    Overview.tsx, Funnel.tsx, Adoption.tsx, Failures.tsx, Errors.tsx, Reports.tsx, Generations.tsx
  lib/catalogue.json  emitted by CI from the generator (build-time import)
```

```ts
// request.ts: what F1/F3/F4 need to be testable
export function toGenerateRequest(s: ConfigState, c: Catalogue): GenerateRequest {
  const features = resolveFeatures(s.features, c);           // closure via `implies`
  return { app_name: s.appName.trim(), package_name: s.packageName.trim(),
           min_sdk: s.minSdk, target_sdk: Math.max(s.targetSdk, s.minSdk), /* … */
           features: [...features].sort(), preset: derivePreset(features, c) };
}
```

Extend the catalogue with the validation rules, so they stop being copied:

```jsonc
"rules": {
  "appName": "^[A-Za-z][A-Za-z0-9 ._-]*$",
  "versionName": "^\\d+\\.\\d+\\.\\d+$",
  "hex": "^#?[0-9A-Fa-f]{6}$",
  "font": "^[A-Za-z0-9][A-Za-z0-9 -]{0,59}$",
  "httpUrl": "^https?://[A-Za-z0-9._~:/?#\\[\\]@!&'()*+,;=%-]+$",
  "reservedPackageWords": ["abstract", "…", "null", "true", "false", "typealias", "typeof"],
  "minCompileSdk": 37
}
```

### TypeScript strictness

`strict: true` is on (`tsconfig.json`). Worth adding:

- `noUncheckedIndexedAccess`. It would have flagged `DESIGN_TRAITS[key]`, `STEPS[stepIndex + 1].id`
  (`configurator.tsx:544`), `tabs[index].screen` (`app-preview.tsx:147`) and `part[0]`
  (`configurator.tsx:141`).
- A runtime guard on the catalogue. `(await response.json()) as Catalogue` (`lib/api.ts:142`) trusts
  the shape blindly. Only `languages` is defaulted.
- `Feature.default` (`lib/api.ts:16`) and `Feature.title` are never read.

---

## 8. Security and privacy

- **XSS:** none found. Every user string (app name, package, feedback title/body, error messages,
  admin rows) renders as React text. The only `dangerouslySetInnerHTML` is the static theme
  bootstrap (`layout.tsx:44`). Font names go into `style.fontFamily` (CSSOM, cannot break out) and
  into `encodeURIComponent` URLs (`font-picker.tsx:17`). `pageUrl` is rendered as text, not as a
  link (`feedback-panel.tsx:191-195`).
- **CSP** (`next.config.mjs:16-27`): sound for a static site, but `script-src 'unsafe-inline'`
  means CSP gives no XSS protection to the admin token. Acceptable given there are no HTML sinks.
  If the admin grows, hash the one bootstrap script, or move `/admin` to its own route with a
  nonce-based policy, since that route can be dynamic. `style-src 'unsafe-inline'` is required by
  the inline-style previews.
- **Admin token:** `sessionStorage` (`admin/page.tsx:117,134`), not `localStorage`. Good. The API
  compares in constant time (`middleware.go:101`). Admin routes are not rate-limited against
  guessing (`server.go:97-106` does not wrap them in `rateLimit`). Cross-area, but worth fixing
  alongside.
- **F40:** `layout.tsx:17` `robots: {index: true}` covers `/admin`. Add `app/admin/layout.tsx` with
  `robots: { index: false, follow: false }`.
- **F17, honesty:** `hero.tsx:85` says "No sign-up, nothing stored, no email." The FAQ (`app/page.tsx:242-249`)
  says app name, package and features **are** kept, and `generations` stores them
  (`web/api/internal/store/migrations/0001_initial.sql:16-25`) along with font, accent and
  feature modules. Feedback stores optional email and UA. Make the hero line say
  "No sign-up. We keep the name, package and features you pick, never your IP."
- **Future CSV export:** feedback `title`/`body` are free text. Prefix cells starting with
  `= + - @` with `'` to prevent CSV formula injection.

---

## 9. Dead code, bloat and AI-slop

### F41: Dead code and bloat

- `EASE_ENTER/EASE_EXIT/EASE_STANDARD` are exported (`app-theme.ts:364-367`) and never imported.
  The same béziers are inlined five times instead (`app-preview.tsx:130,135`,
  `motion-preview.tsx:75,81,91`).
- `Palette.secondary`, `Palette.tertiary`, `Palette.skeleton` and `Ramp.pressed` are computed on
  every render of every preview (`app-theme.ts:84-103,215-221`) and never read. The template uses
  secondary and tertiary only in the catalog app (`CatalogFoundations.kt:54-59`).
- `.grain` (`globals.css:164-171`) is unused.
- The dark palette is written out twice (`globals.css:42-63` and `66-89`). Use Tailwind v4
  `@custom-variant dark` with one variable block, or `light-dark()`.
- `"lint": "next lint"` (`package.json:9`) has no ESLint dependency or config, `next lint` is gone
  in Next 16, and CI doesn't lint. Either add ESLint flat config + `eslint-plugin-react-hooks`
  (which would have caught F10/F14-style effect misuse) or delete the script.
- The `<span … style={{ width: 18, height: 18 }}>` duplicates `h-4.5 w-4.5` (`configurator.tsx:822-828`).

### F42: Comments that violate the repo's own rule (AGENTS.md: "comment why, not what; no comments narrating the change")

- `primitives.tsx:8-12` describes the press as "0.96 … converted for framer-motion", but the code
  uses `whileTap: { scale: 0.97 }` (`15`).
- `primitives.tsx:69-73` says the opposite of line 74 (see F15).
- `primitives.tsx:149-152` explains what `layoutId` does (library basics, a "what" comment).
- `configurator.tsx:71` `/** The reserved list is not written out here. */` is a remnant narrating an
  edit, not a description.
- `hero.tsx:176` "a placeholder number is how the old one survived so long" narrates history.
- `feedback.tsx:20` "the feedback endpoint does not record it yet" narrates a roadmap.
- `lib/api.ts:196` `// Ignored on purpose.` and `admin/page.tsx:3` "Deliberately one page." (the
  page is 666 lines) defend a choice instead of explaining it.
- `configurator.tsx:405-413`: a 9-line JSX comment arguing against `AnimatePresence`. One line
  ("animate in only; `mode="wait"` can strand the old panel") is enough.

UI copy is long-winded in places: 55 em-dashes in TSX copy. The signing warning (`1061-1070`), the
Review signing note (`1274-1281`) and the Build-step AppConfig note (`980-984`) each run 3–5
sentences where one would do. For an "Apple-level" product, trim each to its first sentence and move
detail behind a "Why?" disclosure.

### F44: Copy that the overhaul will make false

- `included.tsx:10` "Its own ripple" must change if the overhaul removes ripple ("no unnecessary
  ripple effects").
- `app/page.tsx:254` "Everything is MIT" must change if Apache-2.0 Kyant catalog code is ported (see §3).
- `included.tsx:42-44` ("switching crossfades… The bar slides away") and `motion-preview.tsx` must
  follow the new navigation transitions.

---

## 10. Admin portal: what exists, what's broken, what's missing

### 10.1 Metrics shown today (exhaustive)

| Widget | Source | Window | Notes |
|---|---|---|---|
| Projects generated (+ today) | `store.go:199-207` | all-time / today | includes 400 validation rejections |
| Unique visitors (+ today) | `visits` | 30d / today | salted IP hash per day; X-Forwarded-For first hop |
| Success rate (sub: "N in 30 days") | `store.go:203` | **all-time** | mixes user-input rejections with real crashes |
| Median build, p95 | `store.go:204-205` | **all-time** | per request, not per feature set |
| Median zip | `store.go:206` | **all-time** | |
| Open errors | `errors` | n/a | |
| New reports (+ blocking) | `feedback` | n/a | |
| Funnel: landed → configured → generated → downloaded | `visits` | 30d | "configured" = clicked Generate (`configurator.tsx:320`) |
| Daily bars (generated, failed) + visitors line | `store.go:258-301` | 30d (API allows 1–365, UI hard-codes 30) | first/last date labels only; hover via `<title>` |
| Feature popularity | `store.go:304-311` | **all-time** | share of successful projects; raw client lists |
| Reports (feedback) | `feedback` | latest 100 | client-side status filter; notes; config context |
| Errors (grouped by fingerprint) | `errors` | latest 100 | resolve/reopen; expand only if `detail` |
| Recent projects | `generations` | latest 40 | name, package, feature *count*, duration, **time only** |
| Route health | `requests` | 7d | count, 5xx %, p50, p95 per route |

**Stored but never shown:** `preset`, `feature_modules`, `min_sdk`, `target_sdk`, `motion_style`,
`font_name`, `accent_colour`, `failure_reason` and `zip_bytes` (both are fetched into `Recent` at
`admin/page.tsx:43-52` and never rendered), `referrer`, errors' `path`/`firstSeen`, feedback `updated_at`.

**Stored but broken:** `referrer` is always the site's own origin. The value is the `Referer` of
the `fetch` to `/api/generate`, which under `Referrer-Policy: strict-origin-when-cross-origin`
(`vercel.json`) is the page URL itself, and `trimReferrer` keeps only the origin
(`server.go:299,522`). `country` is never assigned in `record()` (`server.go:283-300`), so it is
always NULL.

**Not captured at all:** design style, languages, haptics, signing requested/skipped, deep link,
custom URLs, custom secondary/tertiary, font chosen from suggestions vs typed, wizard step reached,
time-to-generate, device/browser/viewport, external referrer/UTM, generator commit
(`generator/COMMIT` exists since `b12122b` but is not recorded per generation), and platform (needed
once the KMP and SwiftUI generators exist). There is no client error reporting either: the `errors.kind = 'client'`
mentioned in the schema comment is never produced, and there is no `app/error.tsx`.

### 10.2 Bugs in the admin (beyond §1)

- **F18:** "Everything on this page is the last 30 days unless it says otherwise"
  (`admin/page.tsx:182`) is false for 5 of 7 headline numbers and for feature popularity.
- **F19:** `FROM generations, unnest(features)` can only list features that someone picked, yet the
  card says "The ones nobody picks are candidates for deletion" (`admin/page.tsx:475-478`). Left-join
  against the catalogue so 0% rows appear. Resolve features before recording them (F1), and record
  the preset base so "picked" can be told apart from "came with the preset".
- **F21:** Recent shows `toLocaleTimeString()` (`615`), so yesterday's row is indistinguishable from
  today's. Rows use `key={index}`.
- **F26:** `load()` sets `setAuthed(false)` on *any* failure (`118-126`). A single 500 from
  `/api/admin/health` during Refresh, or the reload after a status change, throws the admin back to
  the login form. `fetch` network errors are `TypeError`, which is an `Error`, so the explanatory
  `DATABASE_URL` message (`122-123`) can never show. Only a 401 should log out. Other failures should
  keep the last data and show a banner.
- **F27:** `onUpdate`/`onResolve` (`221-245`) don't check `response.ok` and have no `catch`, so a
  failure is silent or an unhandled rejection. Each action re-fetches all 7 endpoints. Use an
  optimistic local update and refetch only the affected list.
- **F28:** `feedback?limit=100` without a status (`93`) plus a client-side filter
  (`feedback-panel.tsx:63`): with more than 100 reports, older **new** reports never appear under the
  default "new" filter. Pass `status=` to the API (it already supports it, `server.go:378-390`).
- **F29:** Generation errors are recorded with `detail = ""` (`server.go:191`). The stderr is folded
  into `message`, which is rendered `truncate` on one line (`admin/page.tsx:551-555`). Expanding
  requires `group.detail` (`570`), so clicking does nothing and the stack trace cannot be read.
  Store stderr in `detail`.
- Triage notes can't be cleared (`NoteBox` disables on empty, `feedback-panel.tsx:239`, and the SQL
  `COALESCE(NULLIF($3,''), notes)`).
- **F39:** 7 headline cards in `lg:grid-cols-6` (`289`) leave one orphan card.

### 10.3 Missing for "who generated what" (prioritised)

**P0, fix what's there**

1. Honest windows. Add one global date-range picker (Today / 7d / 30d / 90d / custom) that passes
   `from`/`to` to **every** endpoint. Show the window on each card.
2. Split "success rate" into **user rejections** (400, with `failure_reason`) and **generator
   failures** (500). Add a **Top rejection reasons** table:
   `SELECT failure_reason, count(*) FROM generations WHERE NOT succeeded AND failure_reason NOT LIKE 'the generator failed%' GROUP BY 1 ORDER BY 2 DESC`.
   That table detects frontend/generator validation drift (F3–F6) directly.
3. Feature adoption **including zeros**, based on resolved features, split into *picked
   deliberately* vs *came with preset*. Record `preset_base` + `added[]` + `removed[]`.
4. Recent generations as a real table: date+time, preset, design, motion, minSdk, languages,
   modules, failure reason, zip size, duration. Click a row for the full spec. Server-side
   pagination (`?before=<id>`), filters (succeeded, preset, feature contains, date), and **CSV export**
   (formula-safe, see §8).

**P1, the questions the product needs answered**

5. **Adoption over time**: per-feature weekly share (small multiples or a heatmap of
   features × weeks), plus presets, design styles, motion styles and languages over time.
6. **Failure rate by feature / combo**: for each feature, failure rate when it is on vs off. Top
   failing pairs (`unnest` twice, `HAVING count >= 5`). By minSdk, by preset, by generator commit.
7. **Wizard funnel**: `track({step:"step", name:"features"})` on each step change (the API's
   closed set needs `step_*` values). Shows where people abandon: Look? Build? Signing?
8. **Durations**: time from `landed` to the first Generate (configuration time), generation p50/p95
   **by feature count** (bucketed), and download-to-browser time (client `elapsed` vs server
   `X-Generation-Ms`).
9. **Download completion**: record "generated" only after `io.Copy` succeeds and log an
   `interrupted` counter separately (`server.go:224-236`). Show generated → downloaded conversion.
10. **Acquisition**: on `landed`, send `document.referrer` (origin only) and `utm_source/medium/campaign`
    from `location.search`. Store them on `visits` (new columns). Table: sources → landed → generated.
11. **Geo**: record `x-vercel-ip-country` (Vercel sets it) into `generations.country` and `visits`.
    Show a country table, with no IP stored, consistent with the privacy copy.
12. **Device / browser**: coarse UA family + `mobile|tablet|desktop` + viewport bucket on `landed`.
    It matters because F13 shows mobile is broken.
13. **Returning visitors**: visitors seen on more than one distinct day in the window (from `visits`;
    note salt rotation resets it). Also repeat generators: the same `visitor_hash` generating more
    than once, which marks iterating users.

**P2, for the multiplatform roadmap**

14. `platform` (`android` | `kmp` | `swiftui`) and `generator_version` columns on `generations`,
    with every chart split or filterable by them.
15. Client error reporting: `app/error.tsx` + `window.onerror`/`unhandledrejection` → `POST
    /api/client-error` (kind `client`, fingerprinted), so a crash like F2 shows up in Errors within
    minutes instead of through a bug report.
16. Charts: the hand-rolled SVG bar chart is fine for one series. For the above, either keep
    hand-rolled small multiples (cheap, no dependency) or add one small charting lib only to the
    `/admin` route chunk. Do not add it to the landing bundle.

Schema sketch (API side, for reference):

```sql
ALTER TABLE generations
  ADD COLUMN design_style TEXT, ADD COLUMN languages TEXT[] DEFAULT '{}',
  ADD COLUMN haptics BOOLEAN, ADD COLUMN signing_requested BOOLEAN,
  ADD COLUMN preset_base TEXT, ADD COLUMN added TEXT[] DEFAULT '{}', ADD COLUMN removed TEXT[] DEFAULT '{}',
  ADD COLUMN platform TEXT NOT NULL DEFAULT 'android', ADD COLUMN generator_version TEXT,
  ADD COLUMN device TEXT, ADD COLUMN failure_kind TEXT;   -- 'invalid' | 'crash' | 'timeout'
ALTER TABLE visits ADD COLUMN source TEXT, ADD COLUMN utm_campaign TEXT, ADD COLUMN country TEXT, ADD COLUMN device TEXT;
```

---

## 11. Suggested order of work

1. F1 (one-line fix plus Undo copy) and F2 (guard, then tokens in the catalogue). Both block the
   overhaul.
2. Validation from catalogue rules (F3–F6); a golden-tested colour ramp (F7); font weights (F8).
3. F10 (ref instead of lifted state) and F14/F13 (`overflow-clip`, scaled live preview). Each is
   small and visible.
4. Split `configurator.tsx` into `state/resolve/validate/request` + step files with unit tests (F23).
   Move static sections to RSC and import `catalogue.json` at build (F11).
5. A11y pass: radio/checkbox semantics, `<dialog>`, `MotionConfig`, contrast tokens (F15, F16,
   F24, F25).
6. Admin P0 (honest windows, rejection reasons, zero-adoption, paginated generations + CSV), then P1.
7. When glass and the new transitions land in the template, update the preview structure and the
   motion loop in the same PR (§3).

---

## 12. Worth keeping or adopting

- Catalogue-driven feature list and presets (`components/configurator.tsx:792-855` renders
  `catalogue.groups`/`features`). Extend the same idea to tokens and rules.
- The server records `generated` itself; the client cannot claim it (`server.go:262-266`).
- ETag + `no-cache` catalogue (`server.go`, commit `b12122b`).
- In-view-only animation loops (`styles-showcase.tsx:28-32`, `motion-preview.tsx:40-41`).
- The signing UX (off by default, passwords generated on enable and never persisted,
  `configurator.tsx:178-183,292-300`).
- From PeriFlow, to reflect on the site when backported: push/pop without the dim fade
  (`periflow-android/core/navigation/.../NavTransitions.kt:21-47`), the linear predictive-back track
  (`49-66`), the glass strength tokens and drawn-glass fallback
  (`periflow-android/core/designsystem/.../foundation/Glass.kt:113-121,146,179-200`).
