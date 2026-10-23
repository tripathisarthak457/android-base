# WP10-WEB handoff (fast track, website)

Stage 26 of `plans/2026-10-20/release-pipeline.md`: the site's previews made to match the template, the privacy page
and its links, `components/included.tsx` and FAQ claims true, the Android-only picker re-checked. Lane `wt/sw-WP10`,
branch `staging-wp/WP10-WEB`, cut from staging `d429cbfbf`. Website only (`web/frontend`, plus the new
`docs/telemetry-and-privacy.md`); no template, generator or API file changed, so no phone check applies (the plan's
stage table: phone "no").

## Progress

- Run 1 (Oct 24): worktree created from staging `d429cbfbf` (staging had not moved, so the start-of-run merge was a
  no-op). `release/STATE.md` and `staging/REQUIREMENTS-GAPS.md` do not exist yet (staging's Requirements phase has not
  run): no gaps routed here. Baseline on staging: 223/223 site tests.
- Done in run 1 (commits listed under "Commits" once made): the privacy page with a test that checks it against the
  code; the preview's glyphs, chip, module empty state and More list as the template draws them; the tab bar preview
  rebuilt on the template's treatments, Pill/Subtle indicator, two-layer selection, stretch and bounce; the CI, Material,
  tabs, adaptive and motion claims made true.
- Still to do in this run: browser check at 320/1280 in both themes, `next build`, `npm audit`, commits.

## What changed, and why

| Area | Files | What |
|---|---|---|
| Privacy page | `docs/telemetry-and-privacy.md`, `lib/links.ts`, `features/privacy/privacy.test.ts` | The page the hero, the FAQ and the footer link (`PRIVACY`), written from the code: every browser storage key, the two Google Fonts origins and Vercel, every column of every table the API stores, retention, the daily hash, generated projects, self-hosting settings. The test fails when a storage key, a CSP origin, a column or a retention default is missing from the page, or the page is not where `PRIVACY` points. `REPO`/`PRIVACY` moved to `lib/links.ts` (re-exported by `lib/catalogue.ts`) because tests cannot import the catalogue's JSON |
| Glyphs | `features/preview/glyphSource.ts`, `glyphs.ts` (written by `npm run glyphs`, `scripts/glyphs.ts`), `glyphs.test.ts`, `icons.tsx` | The preview draws the template's own 87 `AppIcons` glyphs (strokes at 1.8/2.6/2.4, filled forms, even-odd fills) read from `icon/glyph/*.kt`, instead of hand-drawn look-alikes at 2 px. The test fails when `glyphs.ts` differs from the template or AppIcons gains a glyph it lacks |
| Tab glyphs | `features/preview/tabs.ts`, `tabs.test.ts` | A scaffolded module's glyph is picked from its name as `tabs.py`'s `icons()` picks it (`orders` is Package, `wallet` CreditCard, then the fallbacks, then the outlines), with its filled form inside the pill; the template's own tabs keep the outlined glyph when selected, as `AppDestinations.kt` declares them today; More is MoreHorizontal; the scaffolded `home` module is Grid. The tables are checked against `tabs.py` and `AppDestinations.kt` |
| Tab bar | `features/preview/bar.ts`, `bar.test.ts`, `TabBar.tsx` | Treatments Floating, Translucent, Docked (and WP05's Glass); WP03's retired Chunky and Minimal are gone. Pill (accent pill, onAccent content) and Subtle (accentSubtle, accentContent) indicators. The 44 dp pill sized to its label (52 to 72 dp, slot less 8 dp), two layers clipped to the pill so a half-covered tab is half selected, the leading edge on the lead spring and the trailing one on the trail spring with the 0.5-slot stretch limit and the squash, the tapped glyph's dip to 0.84 and spring back (skipped under reduced motion). Capsule: surface, hairline at the style's border width and the soft shadow in light theme; surfaceElevated, a 7 % white outline and no shadow in dark; Translucent at 88 %; Docked on the surface with a 1 px rule. No inner padding (the old preview had 6 dp). Sizes, enums and springs are read from `AppTabBar.kt`, `TabCells.kt` and `AppDesignStyle.kt` in the test |
| Chip | `features/preview/chip.ts`, `chip.test.ts`, `parts.tsx`, `screens.tsx` | AppChip's look: the `fill` role, no outline, subheadline in contentPrimary, a 16 dp glyph in the content colour, and the Search screen's chips with their remove cross (10 dp end padding). "Recent" is footnote, as SearchScreen sets it |
| Screens | `screens.tsx`, `parts.tsx` | The scaffolded module's empty state as `AppStateView` draws it (no glyph, title3, callout message, a Tinted "Reload" button); the More list under an inline top bar with accent glyphs, as `MoreList.kt` draws it |
| Motion | `MotionPreview.tsx`, `MotionStep.tsx`, `bar.ts` (`SHELL_SPRINGS`), `theme.ts` (`framerSpring`) | The push and the tab switch run on the springs the template's shell uses in every style (`ScreenMotion` 0.344 s, `TabCells`' lead, trail and bounce springs), not on the motion style's tokens, which the shell does not read yet. The Motion step's rows are press, release, sheet and dialog, which a style does change, and its intro says the push and the tab switch move alike in every style |
| Claims | `hero.tsx`, `faq.tsx`, `included.tsx`, `features/catalogue/ci.ts`, `ci.test.ts` | CI: worded from `CI_PROJECTS`, which a test holds equal to `ci.yml`'s matrix (`none`, `lean`, `everything`, `all`): "the Lean and Everything presets, plus every feature at once and none at all" until CI builds every preset, then "every preset" by itself. Material: no Material library at all, not even the adaptive ones (the template uses `androidx.window`). Tabs: kept tabs are shown again rather than rebuilt and pause out of sight (they are laid out, so "only the visible one is laid out" was false). Adaptive: rail, then sidebar on large windows. Motion styles: "set how presses spring back and how sheets, dialogs and layout changes move" (they do not change every spring); the hero stat says "Spring presets for presses, sheets and dialogs" |

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
  means changing that list, and the hero and FAQ word themselves from it.
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
