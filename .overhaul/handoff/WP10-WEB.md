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
  `web/README.md`).
- Checks at `5fbc1283a`: 256/256 site tests, `tsc --noEmit` clean; `next build` and `npm audit` under Measurements.
- Not done: a browser check of the rebuilt bar and the reveal (see Measurements for what ran).

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
- **All 87 glyphs ship in `glyphs.ts`** (20 KB source, about 6 KB gzipped) rather than the ones the preview uses
  today: the generator's glyph choice falls through to every outline past eight unmatched modules (12 are allowed), and
  a selection list would be one more thing to keep in step.
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

## Measurements

No phone measurement: the stage changes no template code. Site: tests 223 on staging, 253 after (30 new); `tsc` clean;
`next build` and `npm audit` below once run.

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
