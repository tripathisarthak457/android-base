# WP10-WEB handoff (fast track, website)

Stage 26 of `plans/2026-10-20/release-pipeline.md`: the site's previews made to match the template, the privacy page
and its links, `components/included.tsx` and FAQ claims true, the Android-only picker re-checked. Lane `wt/sw-WP10`,
branch `staging-wp/WP10-WEB`, cut from staging `d429cbfbf`. Website only (`web/frontend`, plus the new
`docs/telemetry-and-privacy.md`); no template, generator or API file changed, so no phone check applies (the plan's
stage table: phone "no").

## Progress

- Run 1 (Oct 24, to about 03:00): worktree created from staging `d429cbfbf`. `release/STATE.md` and
  `staging/REQUIREMENTS-GAPS.md` do not exist (staging's Requirements phase has not run): no gaps routed here. Baseline
  on staging: 223/223 site tests. Committed the privacy page (`24d3e4154`); the rest was left uncommitted.
- Run 2 (Oct 24, from 06:21): merged staging `7230b666e` (`0538b2d0f`; staging touched no web or docs file since
  `d429cbfbf`). Reviewed run 1's uncommitted work against staging's template (every value, claim and file it cites
  re-read), kept it, and committed it in four steps; added the tab reveal, the claims test and the docs. Line endings:
  run 1 had left `glyphSource.ts` with CRLF; every file is LF now (counted with Python).
- Commits on `staging-wp/WP10-WEB` (oldest first): `24d3e4154` privacy page; `0538b2d0f` merge of staging;
  `ab0e716d0` glyphs, tab glyphs, chip, module empty state, More list; `453bce2e5` tab bar (treatments, indicators,
  springs) and the Motion step's shell springs; `3199b7040` hero, included cards and FAQ claims, `ci.ts`,
  `claims.test.ts`; `dc98fe2bd` the tab reveal in the live phone and the Motion step; `5fbc1283a` docs (`AGENTS.md`,
  `web/README.md`); `7be629354` `next` 15.5.28 (a new critical advisory, Decisions); `e3754904d` the Motion cards'
  spring values kept inside the card (found by the browser check); `337f64c72` merge of staging `4760b4071` (the Build
  step's glass notice in `state.ts`/`BuildStep.tsx`, no overlap).
- Run 2 ended **ready for review** at `337f64c72` (257/257 site tests, `tsc`, `next build`, `npm audit` 0).
- **Fix run 1** (Oct 24, from 17:01; reviewer's two blocking findings and 15 non-blocking ones). Staging `4760b4071`
  was already merged. In progress: both blocking findings fixed at their root in the working tree (section "Fix run 1"
  below), tests written and seen failing on the old logic, checks running; commits and the browser check next.
- Left running, not started by this run so not stopped: run 1's `next start -p 3110` (node pid 9988 with its npx, bash
  and cmd parents, since 02:26) and its Browser pane tab "seed" on that port. It serves the `.next` this run rebuilt.
  The orchestrator may stop it.

## Fix run 1 (the reviewer's findings)

Blocking, both fixed at the root:

1. **The bar preview's pill sat below the content it encloses, by the style's border width** (`TabBar.tsx:55`). Two
   sources disagreed on the row's height: the capsule drew its hairline as a CSS `border` under border-box, so the
   row inside was `dp(64) - 2 * borderWidth` tall and the cells centred their glyph and label in that, while the pill
   was placed from the nominal `dp(BAR.height)`. Both halves fixed, so the preview now has the template's geometry and
   cannot drift again: (a) the hairline is drawn by an overlay over the row (`pointer-events-none`, `border-radius:
   inherit`), as Compose's `Modifier.border` draws over the content, so the row keeps the bar's full 64 dp height and
   full width as `TabRow` does (the Glass sheen moved onto the overlay with the rim, so it stays just inside it); the
   docked rule is an inset `box-shadow`, painted behind the row as `drawBehind` paints it; (b) the pill is placed from
   the row's own measured height (`getComputedStyle`, unrounded and unscaled; `clientWidth` rounded the width too),
   as `PillSlots` insets it from the row the cells are measured in. The geometry moved from `TabBar.tsx` into
   `bar.ts` (`restingPills`, `pillBox`), so `bar.test.ts` tests it: "the pill is centred across the row the cells
   centre their content in, whatever its height" (64 dp, Playful's 2 px border, Glass's 0.75 px rim, 56 dp; resting
   and stretched) fails on the old nominal-height logic (`logs/WP10-WEB/fix1-bar-test-nominal-height.log`: "at
   45.78 px the pill's top is 7.78, not 5.78") and passes now. The resting pills are derived once per measure and the
   pill once a frame (the reviewer's per-frame note).
2. **The privacy page overstated how long an address stays in memory** (`docs/telemetry-and-privacy.md:46`). The
   limiter clears its map lazily at the first request after its hour (`middleware.go` `allow`), with no sweep. The page
   now says so: the limits live only in memory, each is cleared by the first request it counts after its hour is up
   or by a restart, so on a route nobody calls an address stays until someone does. The API is outside this stage;
   bounding the map's age stays the API owner's call.

Non-blocking, each addressed or answered:

| Finding | Done |
|---|---|
| Panic logs can carry the address (`:14`, `:46`) | The page's "In short" and "On the server" now state the exception (the connection's address, the proxy's behind one); routed to the API's owner (finding 6) |
| `requests` "per call" (`:159`) | "except health checks and the admin portal's own calls" (`recordsTimings`) |
| feedback `target` always kept (`:134`) | Moved into the "only if **Attach what you configured** stays ticked (a script may send them too)" row; `Feedback.tsx` sends it only in the attached context |
| CI does not run `node --test` | Routed to WP13-PREP (finding 7) |
| AGENTS.md misses files `bar.test.ts` reads | Now names `NavTransitions.kt`, `AppShell.kt`'s line and `KeptTabs.kt`'s condition; and says the privacy page's prose on logs and the limiter is untested, to re-read with `main.go` or `middleware.go` |
| `claims.test.ts` ripple and comments | The included card's row also asserts `id = "ripple"`; every Kotlin read drops comments first (`withoutComments`), so a KDoc naming `springs.sheet` no longer holds a claim up. Guard tightening: passes on the current template |
| hero "refuses Material imports" | Now "refuses Material component imports", its claims row with it (the `material` rule exempts the adaptive imports) |
| `BarLook.borderWidth` "In dp" | KDoc says CSS px and why (a hairline stays a whole pixel in a scaled phone, as the other parts draw borders) |
| `FramerSpring` export and `restDelta?` | Back to staging's private type |
| Per-frame `restingPills` | Done with blocking 1 |
| Docked surface always drawn | A comment in `TabBar.tsx` says why |
| `TranslucentAlpha`, `DarkOutline` hard-coded | `TRANSLUCENT_ALPHA`, `DARK_OUTLINE_ALPHA` in `bar.ts`, read from `AppTabBar.kt` by `bar.test.ts` |
| `storedColumns` and RENAME | The reader (`columnsOf`) follows `RENAME COLUMN`, `RENAME TO`, `ADD COLUMN IF NOT EXISTS`, `ALTER TABLE IF EXISTS/ONLY`; a new test runs it on SQL that does each (the old reader would keep `ip_hash`, add a column named `IF` and miss the renamed table) |
| glyphs.ts size | Corrected in Decisions: 21.3 kB, 4.2 kB gzipped |
| Template KDocs (Subtle "No pill", Floating "elevated surface") | Routed to WP03 / WP12-KNOW (finding 8) |

## What changed, and why

| Area | Files | What |
|---|---|---|
| Privacy page | `docs/telemetry-and-privacy.md`, `lib/links.ts`, `features/privacy/privacy.test.ts` | The page the hero, the FAQ and the footer link (`PRIVACY`), written from the code: every browser storage key, the two Google Fonts origins and Vercel, every column of every table the API stores, retention, the daily hash, generated projects, self-hosting settings. The test fails when a storage key, a CSP origin, a column or a retention default is missing from the page, or the page is not where `PRIVACY` points. `REPO`/`PRIVACY` moved to `lib/links.ts` (re-exported by `lib/catalogue.ts`) because tests cannot import the catalogue's JSON |
| Glyphs | `features/preview/glyphSource.ts`, `glyphs.ts` (written by `npm run glyphs`, `scripts/glyphs.ts`), `glyphs.test.ts`, `icons.tsx` | The preview draws the template's own 87 `AppIcons` glyphs (strokes at 1.8/2.6/2.4, filled forms, even-odd fills) read from `icon/glyph/*.kt`, instead of hand-drawn look-alikes at 2 px. The test fails when `glyphs.ts` differs from the template or AppIcons gains a glyph it lacks |
| Tab glyphs | `features/preview/tabs.ts`, `tabs.test.ts` | A scaffolded module's glyph is picked from its name as `tabs.py`'s `icons()` picks it (`orders` is Package, `wallet` CreditCard, then the fallbacks, then the outlines), with its filled form inside the pill; the template's own tabs keep the outlined glyph when selected, as `AppDestinations.kt` declares them today; More is MoreHorizontal; the scaffolded `home` module is Grid. The tables are checked against `tabs.py` and `AppDestinations.kt` |
| Tab bar | `features/preview/bar.ts`, `bar.test.ts`, `TabBar.tsx` | Treatments Floating, Translucent, Docked (and WP05's Glass); WP03's retired Chunky and Minimal are gone. Pill (accent pill, onAccent content) and Subtle (accentSubtle, accentContent) indicators. The 44 dp pill sized to its label (52 to 72 dp, slot less 8 dp), two layers clipped to the pill so a half-covered tab is half selected, the leading edge on the lead spring and the trailing one on the trail spring with the 0.5-slot stretch limit and the squash, the tapped glyph's dip to 0.84 and spring back (skipped under reduced motion). Capsule: surface, hairline at the style's border width and the soft shadow in light theme; surfaceElevated, a 7 % white outline and no shadow in dark; Translucent at 88 %; Docked on the surface with a 1 px rule. No inner padding (the old preview had 6 dp). Sizes, enums and springs are read from `AppTabBar.kt`, `TabCells.kt` and `AppDesignStyle.kt` in the test |
| Chip | `features/preview/chip.ts`, `chip.test.ts`, `parts.tsx`, `screens.tsx` | AppChip's look: the `fill` role, no outline, subheadline in contentPrimary, a 16 dp glyph in the content colour, and the Search screen's chips with their remove cross (10 dp end padding). "Recent" is footnote, as SearchScreen sets it |
| Screens | `screens.tsx`, `parts.tsx` | The scaffolded module's empty state as `AppStateView` draws it (no glyph, title3, callout message, a Tinted "Reload" button); the More list under an inline top bar with accent glyphs, as `MoreList.kt` draws it |
| Motion | `MotionPreview.tsx`, `MotionStep.tsx`, `bar.ts` (`SHELL_SPRINGS`), `theme.ts` (`framerSpring`) | The push and the tab switch run on the springs the template's shell uses in every style (`ScreenMotion` 0.344 s, `TabCells`' lead, trail and bounce springs, `TabReveal`'s reveal spring), not on the motion style's tokens, which the shell does not read yet. The Motion step's rows are press, release, sheet and dialog, which a style does change, and its intro says the push and the tab switch move alike in every style |
| Tab reveal (run 2) | `bar.ts` (`TAB_REVEAL_SHIFT`, `revealFrom`, `SHELL_SPRINGS.tabReveal`), `bar.test.ts`, `AppPreview.tsx`, `MotionPreview.tsx` | The template's `KeptTabs` no longer switches instantly: a tab the person taps arrives 12 dp toward the side it comes from and springs into place (`TabReveal.kt`, stiffness 503.5, damping 1); only it is drawn. The live phone does this for a tab tapped on its bar, not for a screen picked beside it (it stands in for a link or a programmatic switch, which the shell shows at once), and not under reduced motion; a second tap mid-reveal restarts from its own offset, as `switchTo` does. The Motion step's loop nudges its tab switch on the same spring (the whole list tab on the way back, its button with it). The test reads the spring, the shift, the direction formula and `animate = revealSwitch() && !reduceMotion` from the template |
| Claims | `hero.tsx`, `faq.tsx`, `included.tsx`, `features/catalogue/ci.ts`, `ci.test.ts` | CI: worded from `CI_PROJECTS`, which a test holds equal to `ci.yml`'s matrix (`none`, `lean`, `everything`, `all`): "the Lean and Everything presets, plus every feature at once and none at all" until CI builds every preset, then "every preset" by itself. Material: "No Material component library is a dependency and the build refuses their imports; window size classes come from androidx.window" (run 2's wording, which stays true if a later stage adds the adaptive libraries REQUIREMENTS 31 allows; run 1's "not even its adaptive layouts" would not). Tabs: kept tabs are shown again rather than rebuilt and pause out of sight (they are laid out, so "only the visible one is laid out" was false). Adaptive: rail, then sidebar on large windows. Motion styles: "set how presses spring back and how sheets, dialogs and layout changes move" (they do not change every spring); the hero stat says "Spring presets for presses, sheets and dialogs". `features/catalogue/claims.test.ts` (run 2) pairs each of these sentences with the code that makes it true: no `androidx.compose.material`/`material3` artifact in `libs.versions.toml` and the `material` code rule; `SizeClass.kt`'s `androidx.window` import; `KeptTabs`' `owner.cap = Lifecycle.State.CREATED`; `ShellChromePolicy`'s Medium-to-Rail and Large-to-Sidebar lines; `ManifestRules`' `screenOrientation` lock; `springs.press`, `release`, `sheet`, `dialog` and `layout` read in `:core:designsystem`. A second test fails when a sentence leaves the page, so the table cannot go stale. It fails on staging's copy |
| Motion cards (run 2) | `MotionStep.tsx` | The label column is as wide as its longest label (`ch`) instead of 86 px, the value may shrink, a no-break space keeps each number with its unit and the bounce follows a comma ("0.32 s, bounce 0.15"), so the 274 px cards (desktop) and the 320 px screen wrap between words with nothing past the card's edge |
| Dependencies (run 2) | `package.json`, `package-lock.json` | `next` 15.5.27 to 15.5.28 (Decisions) |
| Docs (run 2) | `AGENTS.md` ("The website must not over-promise"), `web/README.md` ("What is recorded") | AGENTS.md names each template change the site's tests now hold it to (glyphs and `npm run glyphs`, the bar, chip and tab copies, the claims table, `CI_PROJECTS`, the privacy page). web/README.md's summary said the funnel was recorded once per visitor per day (telemetry v1); it now points to the privacy page and says one visit per tab and the steps it reached |

## Decisions (and why)

- **The site says what the template does at this commit, not what a later stage will make true** (freeze decision 20:
  each merge's docs say only what that merge makes true). The template's shell keeps private springs (`ScreenMotion`,
  `TabCells.kt`), so the Motion step no longer claims a style changes pushes and tab selection. `bar.test.ts` "the push
  and the tab bar move on the shell's own springs" fails the day the template reads `AppTheme.springs` there
  (`followups.md` "WP04" leftovers; MOTION-PLAN rule 1), and points the site back to the motion style's springs.
- **Drift is a failing test, not a note.** The bar's enums, sizes and springs, the chip, the glyphs, the tab glyph
  tables, the CI matrix and the privacy page are each read from the code they describe. Brittle by design: a format
  change in those files fails the site's tests with a message naming what to update.
- **All 87 glyphs ship in `glyphs.ts`** (21.3 kB source, 4.2 kB gzipped, measured with Python on the file) rather than the ones the preview uses
  today: the generator's glyph choice falls through to every outline past eight unmatched modules (12 are allowed), and
  a selection list would be one more thing to keep in step.
- **`next` 15.5.27 to 15.5.28 (run 2).** `npm audit` on Oct 24 reported a new critical advisory against 15.5.27
  (GHSA-2464-fhr4-w9xw, remote code execution in the image optimisation API when SVGs are used; also
  GHSA-p3m5-59mp-jv5r, a Server Actions denial of service), fixed in 15.5.28, a stable patch of the same minor
  published Oct 14. Neither is reachable on this site (every route is static, it uses neither `next/image` nor Server
  Actions), but the stage's done-definition is a clean audit and the release should not ship a flagged version. Taken
  under the user's standing approval of the npm security upgrades (REQUIREMENTS 47, Oct 19 22:16), as `f0898fc0` did:
  `npm install next@15.5.28 --save-exact --ignore-scripts` through `memwait`, cache in `D:/.caches/npm`; only
  `next` and its `@next/*` packages moved in the lock file. Not 16.x (`latest`): a major upgrade is not a security fix.
- **The Docked bar's surface is always drawn.** The template shows it in proportion to `scrolledUnder`; no built-in
  style docks, so the preview does not track the scroll edge.

## Findings routed to other stages (template; not changed here)

1. **The shell ignores the motion style for pushes and the tab bar** (WP04-SHEET, `followups.md` "WP04":
   "`AppTheme.springs.screen`/`navigation` ... into `ScreenMotion` and the bar"). `ShellConfig.screenMotion()` uses
   `ScreenMotion.DEFAULT_RESPONSE` though its KDoc says "null uses the theme's"; `TabCells.kt` has private
   `LeadSpring`/`TrailSpring`/`TabBounce` springs though `AppSprings.indicatorLead`'s KDoc says the edges take "the
   style's bounce"; `ChromeSlide`, `TabReveal`, `AppTabBarState` and `AppSidebar` keep private copies of `layout` and
   `thumb`. For Standard every value matches the tokens, so the measured builds would not change. When it lands: drop
   `SHELL_SPRINGS` from `MotionPreview` and `TabBar`, give the Motion step its "push and pop" and "selection" rows back,
   and widen the hero stat and the included card.
2. **The template's own tabs have no filled glyph** (`AppDestinations.kt` sets only `icon`), while scaffolded modules
   get one (`followups.md` "WP04": "the filled glyphs in `AppDestinations`"). The preview draws today's state; the tab
   test reads `AppDestinations.kt`, so it follows when that changes.
3. **Two template docs still say a tab switch is instant or crossfades** (WP12-KNOW, WP04): `AppTabBar.kt`'s KDoc
   "Switching tabs is instant; the bar is the motion." and `templates/android/AGENTS.md`'s recipe "switching
   crossfades". `KeptTabs`/`TabReveal` nudge the arriving tab 12 dp and draw nothing over it (no crossfade, no veil).
4. **`web/README.md` (WP12-KNOW, web docs)**: its Configuration table lacks `ENABLE_TARGETS`, `TRUSTED_PROXY` and the
   three `RETENTION_*` variables the privacy page lists (and the user sets `ENABLE_TARGETS` and `TRUSTED_PROXY` in
   Vercel before the deploy); its "Nothing in it identifies them" about an attached report is loose, since a report
   keeps the app and package names with no time limit (the privacy page states the facts). Not changed here: the
   table is the API's documentation, outside this stage's scope.
5. **Editorial's uppercase labels may be cut in a five-tab bar** (QA2, WP03): with five tabs at 360 dp the preview
   shrinks "PROFILE" and "SETTINGS" to the template's 0.85 floor (`MIN_LABEL_SHRINK`) and still ends them with an
   ellipsis ("PROFI…", "SETTIN…"), in Fraunces caption2. The preview follows `TabCells.kt`'s rule, so the app probably
   does the same (REQUIREMENTS 18 names truncated tab labels); only a phone capture of the Editorial style with five
   tabs can confirm it.
6. **The API can log a visitor's address when a handler panics** (fix run 1; the API's owner, `web/api`; no stage of
   this release changes it, so the integrator or a fix stage). `cmd/server/main.go` sets no `http.Server.ErrorLog` and
   no middleware recovers a panic, and `slog.SetDefault` routes the standard logger into the JSON logs, so Go's
   `net/http` writes `http: panic serving <RemoteAddr>: ...` there. Behind Vercel that address is the proxy's; with
   `TRUSTED_PROXY=none` (a server exposed directly) it is the visitor's. The fix is an `ErrorLog` that drops the
   address, or a recover middleware that answers 500 and logs the route only. The privacy page now states the
   exception; once the fix merges, the sentence ("The exception is a request that crashes the server ...") and the
   "short of a request that crashes the server" in "In short" can go.
7. **The site's tests do not run in CI** (fix run 1; WP13-PREP, `design/wp/WP13.md` line 69): `.github/workflows/ci.yml`'s
   frontend job runs `npm ci`, `tsc` and `next build` but not `npm test` (`node --test`), so a template-only change that
   makes the site untrue (a glyph, a bar size, a claim, the CI matrix, a new stored column) goes green until someone
   runs the site's tests locally. WP13's design already lists `node --test` for the job (Node 24); the rewrite must
   keep it, and on Node 24 or 22.18+, since the tests are `.ts` files run with Node's type stripping (today's job pins
   Node 22 with no minor). Every job runs on every push and pull request (no path filters), so nothing else is needed
   for a template-only change to run them. AGENTS.md's sentence ("The site's tests (`cd web/frontend && npm test`)
   read the template") stays accurate either way.
8. **Two `AppDesignStyle.kt` KDocs disagree with `barLookFor`** (fix run 1; WP03 / WP12-KNOW): `BarIndicator.Subtle`
   says "No pill: the selected glyph turns filled", while `barLookFor` gives Subtle an `accentSubtle` pill; and
   `BarTreatment.Floating` says "on the elevated surface", while `barLookFor` uses `surface` in light theme and
   `surfaceElevated` only in dark. The preview follows the code (`bar.test.ts` pins both), so only the KDocs change.

## Measurements

No phone measurement: the stage changes no template, generator or API code (`touched_measured_path` false).

| Check (logs in `logs/WP10-WEB/run2-*`) | Staging `7230b666e` (tests: `4760b4071`) | This branch `337f64c72` |
|---|---|---|
| Site tests | 224/224 | 257/257 (33 new: glyphs 3, tabs 6, chip 2, bar 11, CI 4, claims 2, privacy 5) |
| `tsc --noEmit` | clean | clean |
| `next build`, `/` (gzip) / first load | 62.1 kB / 209 kB (`staging/logs/fix/f112-site.log`) | 68.5 kB / 216 kB: +6.4 kB, mostly the 87 glyphs (4.2 kB gzipped source) and the two-layer bar |
| `next build`, `/admin`; shared | 14.4 kB / 162 kB; 104 kB | unchanged |
| `npm audit` | 1 critical (`next` 15.5.27, new advisories) | 0 (`next` 15.5.28) |
| `genkit.core.catalogue --check` | current | current |

**Browser check** (built site served by `next start` on 127.0.0.1:3112, the built-in Browser pane; light and dark; 320,
400 and 1024 px wide): the showcase's four styles draw their bars as the template does (Utility and Editorial a
Subtle tinted pill, Social and Playful a solid Pill in the accent with the legible content colour, Editorial's
Translucent capsule on the elevated surface in dark with no shadow); the live phone's Subtle pill and capsule in both
themes; tapping Settings on the live bar nudged the screen in from the right (`translateX` 9.33 px = 12 dp at the
preview's scale, falling toward 0 and settling at none), and picking a screen beside the phone did not; no horizontal
page overflow at 320; the platform step offers Android Native alone, Compose Multiplatform and Swift "Coming soon";
the footer links Privacy. Found and fixed: the Motion cards' values broke inside "0.16 s" and ran past the card edge at
320 px and on the 274 px desktop cards (`e3754904d`). The pane was hidden, so animations ran on throttled frames:
the reveal's start, direction and landing were read from the computed transform, not watched.

## Rules and gotchas the docs must state

- **The privacy page is checked against the code** (`features/privacy/privacy.test.ts`). A new browser storage key, a
  new origin in `next.config.mjs`'s CSP, a new column in `web/api/internal/store/migrations` or a changed retention
  default fails the site's tests until `docs/telemetry-and-privacy.md` names it (a storage key on a line that also says
  `localStorage` or `sessionStorage`; a retention variable on a line with its default).
- **The preview reads the template.** After changing a glyph in `core/designsystem/.../icon/glyph/`, run
  `npm run glyphs` in `web/frontend` and commit `features/preview/glyphs.ts`. After changing the tab bar's sizes, enums
  or springs (`AppTabBar.kt`, `TabCells.kt`, `AppDesignStyle.kt`), AppChip, `tabs.py`'s glyph tables or
  `AppDestinations.kt`'s tabs, change the preview's copy of them (`bar.ts`, `chip.ts`, `tabs.ts`) in the same commit;
  the site's tests say which.
- **CI's claims follow `ci.yml`'s matrix** through `CI_PROJECTS` (`features/catalogue/ci.ts`); changing the matrix
  means changing that list, and the hero and FAQ word themselves from it. **WP13-PREP:** `ci.test.ts` reads the matrix
  as one `preset: [...]` line; the `ci.yml` rewrite either keeps that line or changes the test's reader in the same
  commit, and adds `standard` to `CI_PROJECTS` once CI builds it (the copy then says "every preset" by itself).
- **The site's claims about the template are a table** (`features/catalogue/claims.test.ts`): each sentence of the
  hero, the included cards and the FAQ that a template change could falsify, beside the code that makes it true.
  Rewording a sentence means rewording its row; a template change that falsifies one fails the site's tests.
- **The tab switch in the previews follows `TabReveal`**: the shift, the spring and "only a switch the person makes,
  never under reduced motion" are read from `TabReveal.kt` and `KeptTabs.kt` by `bar.test.ts`. When the shell takes
  the motion style's springs (finding 1), the reveal moves with them.
- **Reports and errors are kept with no time limit** and the admin has no delete for them; app and package names in a
  report are not removed after 90 days as a generation's are. Whether to offer deletion on request is the user's call
  (not promised on the page).
- **`TRUSTED_PROXY` defaults to `none`**; the location columns stay empty unless it is `vercel` (set it in the Vercel
  project, with `IP_SALT`, `ADMIN_TOKEN` and `DATABASE_URL`).

## Release blockers carried from `handoff/WP10.md`

| Item | Status at this branch |
|---|---|
| `ENABLE_TARGETS=android` | Closed on staging: the API's default is `android` (`ab27ac9e`), the Dockerfile sets it, multiplatform is `coming_soon` |
| CI claim | Closed: the copy names what `ci.yml` builds, held to it by `ci.test.ts`; it says "every preset" by itself once WP13-PREP's matrix builds `standard` |
| Privacy page | Closed: `docs/telemetry-and-privacy.md`, tested |
| Licence | Closed on staging: `LICENSE` (MIT) and the Apache-2.0 skills wording (`cfb765cc`) |
| End-state claims in `included.tsx` | Closed: each card checked against staging's template (below) |

`included.tsx`, card by card, against staging `d429cbfbf`: no Material (`verifyCodeRules`' `material` and `ripple`
rules); smooth by rule (`composed`, `collect-as-state`, `raw-tween`, `lazy-key`; `nav-fade`, transitions Push or
None); kept tabs (`KeptTabs.kt`, `ShellState.select`, `BackGate`); Navigation 3 with per-feature `NavGraphEntry`
sets; adaptive (`NavigationChrome` bar, rail, sidebar; `NavPane` list-detail; `readableMaxWidth`;
`verifyManifestRules`' orientation and resize locks); styles and motion (the corrected sentence; `PaletteContrastTest`
for AA); modular (`feature-public`, `verifyModuleDependencies`, `verifyApiPurity`); MVI (`launchLatest`,
`launchExclusive`, `AppError`, `TextFieldState`); agents (`AGENTS.md`, `CLAUDE.md` `@AGENTS.md`); flavours from the
catalogue; release artifacts (`ReleaseArtifacts.kt`: per-ABI and universal APKs, `checksums.sha256`).
