# Sheet specs: judge 1 (feel, look and motion first)

Written 2026-10-13 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). Read in full: `design-feel.md` (1690 lines),
`design-api.md` (1745), `design-perf.md` (1115), and the inputs `current-state.md` and `research.md`. Nothing in any
repository or worktree was changed. No Gradle or adb was run.

The lens is an Apple HI designer who has shipped iOS 26 sheets. Feel, look and motion come first, then the
120 Hz budget on the Redmi Note 10 Pro, because REQUIREMENTS 19 and 21 make them two halves of one requirement.

Tags as in the inputs: **[C]** code fact, **[V]** verified in library sources this session, **[D]** derived,
**[U]** unverified.

---

## 0. Verdict

**Winner: `design-feel.md`.** Weighted totals: feel 8.3, api 7.0, perf 6.4.

It is the only complete spec. It also gets the four things a person notices in the first ten seconds:

- a flick moves a detent without a hard throw;
- the floating sheet attaches to the edges without its content reflowing;
- the haptic tick lands on the frame where the sheet arrives;
- the form sheet behaves like an iPad form sheet, not like an alert.

The other two specs are cut short on disk. That is a finding in its own right:

- `design-api.md` ends after §12, at line 1745. It still cites a §13 (tests), §14 (Compose Multiplatform),
  §15 (SwiftUI) and §16 (docs) (`design-api.md:6`, `:1396`, `:1446`, `:1713-1717`).
- `design-perf.md` ends after §8, at line 1115. It still cites a §9 (sheet layers and their invariants), §10
  (first frame), §11 (files), §12 (tokens), §15 (screenshots) and §16 (gates) (`design-perf.md:48`, `:92`, `:525`,
  `:785`, `:1085`, `:1100`). Its central hosting decision, the "sheet layer" system, is therefore unspecified.

I scored what exists. If complete copies turn up, rescore those two on parity, fit and robustness. Feel would very
likely still win: the gaps in api and perf are in feel, not in coverage.

## 1. Weighting

| Dimension | Weight | Why |
|---|---|---|
| feel | 25 % | The user asked for iOS-level feel; it is what the person notices first |
| performance | 15 % | REQUIREMENTS 19, 21, 22: ~0 % jank at 120 Hz, no per-frame recomposition |
| robustness | 12 % | Rapid Back, interruption, process death, the N1–N11 seams |
| customisability | 10 % | "highly customisable" |
| api_quality | 10 % | androidx API guidelines (REQUIREMENTS 27) |
| adaptive_a11y | 10 % | REQUIREMENTS 23, TalkBack actions |
| fit_with_architecture | 10 % | Hosts, Navigation 3, glass tiers, tokens, work split |
| parity | 8 % | Compose Multiplatform and SwiftUI |

## 2. Scores

| Spec | feel | custom | api | perf | robust | a11y | fit | parity | **total** |
|---|---|---|---|---|---|---|---|---|---|
| design-feel | 9 | 8 | 7 | 9 | 8 | 8 | 8 | 8 | **8.30** |
| design-api | 6 | 9 | 9 | 8 | 7 | 8 | 7 | 2 | **7.00** |
| design-perf | 6 | 7 | 6 | 9 | 7 | 8 | 5 | 1 | **6.37** |

### design-feel

- **Feel.** The release rule separates intent from conviction:
  - a 400 dp/s flick moves one detent;
  - closing the sheet takes 2 H/s or 52 % of the travel (`design-feel.md:675-691`).
- **Arrival haptic.** The haptic plays on the arrival frame, within 1 px of the detent, never at the start of
  the settle (`:830-838`).
- **Floating to attached.** One uniform round rect throughout. At Large the bottom corners slide below the window,
  so the clip stays a hardware rrect on every API (`:905-929`, `:1062-1071`).
- **Content never reflows.** It is measured once at the floating width; `sheetBleed` lets children reach the edge.
- **Rubber band.** The asymptote runs to Large for a sheet whose top detent is not Large (`:714-717`).
- **Zoom from a source.** Modelled as a second progress, so a finger can catch it mid-morph (`:992-1011`).
- **Form sheet.** It slides up and can be swiped away, as iPadOS does (`:49`).
- **One dim.** Drawn once, inside the presenter (`:931-950`).
- **Shadow.** Cut out of the shape's inside, fixed in size, never seen through a fading body (`:1127-1144`).
- **Completeness.** Tests (§13), phone gates (§14), feel acceptance from frame strips (§14.3), parity (§15, §16),
  a work split (§17) and a measure-before-trusting list (§18).

Weak spots, each covered by a graft below:

- the merged dim gets lighter as the sheet rises (`:947-950`);
- with `dismissible = false`, Back leaks to the screen under a *modal* sheet (`:361`, `:733`);
- the grabber is an enum rather than a slot (`:318`);
- the `visible` overload takes no state (`:393-404`);
- a destination's screen cannot fill the header or footer slots;
- `largeTop` uses the status bar, not safe drawing (`:188`);
- in-screen sheets never report motion to `BackGate`/`NavGuard`;
- a cover over a sheet is asserted rather than designed (`:1251`).

### design-api

The best API discipline of the three:

- acceptance examples that every API change must keep short (`design-api.md:57-133`);
- twelve parameters, grouped into `AppSheetStyle` and `AppSheetProperties` (`:456-617`);
- a mapping onto the Styles API (`:623-640`);
- a grabber slot (`:218`);
- content-side `ProvideSheetHeader`, `ProvideSheetFooter` and `SheetBackHandler` (`:696-743`);
- `SheetDetentContext.heightOf`, UIKit's `resolvedValue(in:)` (`:410-411`);
- resolution rules R-1…R-10 (`:415-428`);
- `SheetSessions` cleaned up in a decorator's `onPop` (`:873-889`).

The feel is where it loses:

- **Detent flicks are dead.** 2 H/s is the only flick threshold. A 1000 dp/s flick up from Medium projects 99 dp and
  falls back to Medium (`:1056-1068`).
- **Stacked sheets reach the status bar.** A stacked sheet sits 0.02 H higher (`:1205-1206`, `:419`), which puts its
  top about 7 dp into the status bar on the Redmi, at the punch-hole.
- **A Medium-only sheet barely stretches.** The over-pull range is 0.008 H above *any* top stop (`:1049-1053`).
- **The form sheet speaks alert language.** It scales in and cannot be dragged (`:43`, `:1459`).
- **A hairline instead of the iOS 26 scroll-edge effect** (`:1414`).
- **The scrim lightens toward Large.** It is multiplied by (1 − f) as the card stack takes over (`:1339`).
- **Non-modal sheets lose glass.** They are drawn in place, inside the recorded scene, so a Maps-style sheet can
  never be glass (`:40`).
- **Scope creep.** A navigation results store (`:924-942`).
- **Parity is absent:** the sections are missing.

### design-perf

The most rigorous performance and robustness thinking:

- **Budgets** per phase (`design-perf.md:71-103`).
- **Read sites.** A table of where each fast value is read (`:128-140`).
- **IME:** at most two re-measures per keyboard transition, and `fitInside` rejected with a verified reason
  (`:701-738`).
- **Explicit Back enablement:** top-most, not covered, not dismissing, keyboard hidden, a way out exists (`:782-791`).
  It rests on verified `navigationevent` ordering: I confirmed it in the 1.1.2 sources, overlay before default
  (`NavigationEventProcessor.kt:272`, `:579`) and disabled with the dispatcher (`NavigationEventHandler.kt:126`).
- **`HostMotion`** reports any sheet's motion to its host's gate (`:816-841`).
- **Safe-drawing top**, which covers caption bars and cutouts (`:208-217`).
- **Edges and inputs:** the sheet attaches under 3-button navigation (`:750-752`), and a mouse wheel reports
  `UserInput` (`:696-697`).
- **Stack depth:** at most two covered sheets are drawn (`:881-882`).
- **D1 cannot happen** (`:678-681`).

Its weak points:

- **Floating is off for Solid**, the default style (`:62`, `:501`), so the default sheet loses the iOS 26 look the
  user asked for.
- **The confirm hook cannot reach a destination.** Dismissal is per-path flags plus an `onDismissAttempt` at the
  call site (`:345-358`, `:397`), and a destination's screen never sees that call (`:577-578`).
- **The same dead detent flick as api** (`:639-651`).
- **A double Back on an in-screen sheet pops the screen** (`:837-838`).
- **Missing sections.** The hosting design (§9) and everything after it are absent.

## 3. Grafts into design-feel

1. **Ways out of a modal sheet** (api B-20, `design-api.md:1136`; perf `:790-791`). Under a *modal* sheet, Back never
   reaches the screen beneath:
   - `dismissible = false` on a modal sheet makes Back a consumed attempt;
   - stepping down a detent and then passing Back through applies only to non-modal persistent sheets.

   This fixes `design-feel.md:361`, `:733`.
2. **Explicit Back-handler enablement** and **Back hides the keyboard first** (perf §4.8 `:782-791`, §4.6 `:731-735`).
   The handler is disabled while the IME is visible: a flag written from placement and pushed to `isBackEnabled` from
   an effect. Registration order is not trusted for stacked sheets.
3. **`HostMotion` / `LocalHostMotion`** (perf `:816-841`) for both hosts, so `NavGuard` holds a push while a sheet of
   either kind rises or leaves. Keep feel's Back rule for in-screen sheets, which consume and drop Back during their
   exit (see reject 7).
4. **Catch on the `Initial` pass** (api B-7 `:1046-1048`; perf `:605-609`). An `Initial`-pass pointer node stops any
   settle on finger down, content included. It is mandatory, not the [U] fallback of `design-feel.md:668-671`.
5. **`largeTop = safeDrawing.top + 10 dp`**, and horizontal centring inside `safeDrawing` (perf `:208-217`,
   `:748-749`). This replaces `statusBars.top` (`design-feel.md:188`): a ChromeOS caption bar or a landscape cutout
   would cover the grabber.
6. **3-button navigation: attached, never floating** (perf `:750-752`). The floating anchor above
   `tappableElement` (`design-feel.md:914`) leaves a dimmed gap over an opaque button bar.
7. **The merged dim** (perf `:1011-1014`). In light mode, use the compound alpha
   `1 − (1 − a·pf)(1 − 0.10·lf)`, which never lightens as the sheet rises. Dark mode uses two rects. This replaces the
   lerp at `design-feel.md:947-950`. iOS's sheet dim value stays [U] until QA2 compares against an iOS 26 recording.
8. **`contentPadding` as a snapshot-backed `PaddingValues` read in measure** (perf §3.5 `:465-467`; androidx
   `ff66ec4e`). That is how feel's "1 re-measure, 0 recompositions" (`design-feel.md:1049`) actually holds.
   Also take its docs rule: no `fitInside(Ime)` or `imePadding()` inside a sheet; the container consumes `ime` and the
   safe-drawing top.
9. **`ContentInteraction.Automatic`** resolves to `Scrolls` for non-modal sheets and mouse-only windows (perf
   `:334-341`, `:696-697`). Add a test that a wheel notch never resizes the sheet.
10. **API shape** (api §2.2, §2.5, §2.6). Cut feel's 18 parameters:
    - group the rarely changed behaviour into `AppSheetProperties`: placement, content interaction, transition,
      keyboard, `scrimTapDismisses`;
    - group the look into `AppSheetStyle`: material, colours, corner, floating inset, max width, shadow. It is built by
      `AppSheetDefaults.style()`, has a `copy()`, and maps onto the Styles API;
    - add per-design-style `sheetInset` and `sheetMaterial` to `tokens/styles.json` (`design-api.md:1289-1291`);
    - keep `source` and `onDetentChange` as plain parameters.
11. **The grabber as a slot** (`grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber`,
    `design-api.md:218`). Keep feel's Automatic hiding rule inside the default grabber.
12. **A `state` parameter on the `visible` and `item` overloads** (api `:213`; perf `:422`), so an in-screen sheet
    can be moved and observed. The rule is one state per call site with `LaunchedEffect(visible)`, and a reversal
    keeps its velocity (api B-2).
13. **Content-side providers** (api §2.7 `:696-743`): `ProvideSheetHeader`, `ProvideSheetFooter` and
    `SheetBackHandler`, plus `LocalSheetSurface` carrying the state and the material, replacing `LocalInSheet`.
    - A destination's screen can then pin a Save footer above the keyboard and expand its sheet from code.
    - Add one condition: `show()` waits for one layout after the providers register, so `FitContent` resolves with
      the header and footer included, and nothing pops in after the first frame.
14. **`SheetKeyboard.Lift | Expand`** (api `:486-493`, B-30/B-31), with Lift as the default.
15. **Detent resolution:**
    - `SheetDetentContext.heightOf(detent)` and `foldBottom` (api `:400-412`);
    - R-6, which merges stops within 1 px, and R-8, which falls back to Large when every detent is off (api `:424-426`);
    - resolver results in `Dp` (perf `:186`) to avoid density mistakes in app code.
16. **Navigation state** (api `:873-889`). Use `SheetSessions` keyed by `contentKey`, removed in a `NavEntryDecorator`
    `onPop`, which is verified to run after `onRemove` (`nav3rt117/…/NavEntryDecorator.kt:70-88`). `onRemove` is
    null-safe through `peek`. This replaces feel's `DisposableEffect` registry (`design-feel.md:1235-1241`).
    The presentation host also builds its own strategy, and `AppShell(sheetStrategy)` goes (api `:898-900`), so N2
    cannot come back.
17. **Pushing an ordinary key from a presented sheet** closes the presented sheets, then pushes on the tab, sequenced
    by the gate (perf decision 7, `:54`; HIG [A]). `AppSheetNavHost` stays for flows inside a sheet. This settles
    N5's default path, which feel leaves open.
18. **Automatic placement for non-modal sheets:** Leading (Start) from Expanded width (api `:1460`; perf `:1066`).
    Feel's table at `:1181` puts every Expanded sheet in the centre.
19. **Form sheet height** `min(0.85 H, 620 dp, fit)`. The 620 dp cap exists today (`ds/component/overlay/AppSheet.kt:337`
    [C]) and feel drops it (`design-feel.md:1027`).
20. **Cheaper touch pass-through** (perf `:845-849`). At undimmed detents the dim node returns
    `sharePointerInputWithSiblings() = true`, so crossing the boundary needs no node swap.
21. **The action sheet on wide windows** is an in-window anchored card in the sheet's host, not `AppMenu`'s `Popup`
    window (perf decision 20, `:67`; `AppMenu.kt:84`). That means no window creation and no glass dead zone.
22. **Perf's reviewer contracts:**
    - the per-phase budget table (`design-perf.md:82-92`) and the "written by / read in" table (`:130-140`), as a
      §5.11 appendix;
    - a cap of two covered sheets drawn (`:881-882`);
    - the "D1 cannot happen" rule for the `visible` overload: a hidden sheet whose `visible` stays true is not drawn,
      not hit-tested and not in the stack (`:678-681`);
    - the list of translated strings (`:1106-1115`).
23. **Fade tokens.** Reduce-motion fades use the existing `fades.longMs` and `fades.shortMs` tokens (perf `:944-946`),
    not a sheet-local 150 ms literal (`design-feel.md:1030`).
24. **Zoom from a source ships in phase 2.** `SheetTransition` stays sealed so `Zoom(source)` can be added without
    breaking callers (api `:496-497`). Feel's §5.9 design is kept as the spec for it. It ships once the core sheet
    passes the §14 gates on the Redmi.

## 4. Rejects (do not build)

1. **2 H/s as the threshold for a one-detent flick** (api B-10 `:1059-1060`; perf `:640-642`).
   - A deliberate 1000 dp/s flick from Medium projects 99 dp and lands back on Medium.
   - It reads as "the sheet ignored me". iOS sheets change detent on a light flick.
   - 2 H/s is the *dismiss* rule only. Keep feel's 400 dp/s detent flick, marked [U] for tuning on the phone.
2. **A stacked sheet 0.02 H higher** (api B-36 `:1205-1206`, R-1 `:419`). On Android it puts the sheet's top and
   grabber into the status bar and the punch-hole camera. Use feel's rule: the same top, with the covered sheet
   lifted 0.5 % H.
3. **Floating off for Solid by default** (perf decision 15, `:62`, `:501`). The default sheet would lose the iOS 26
   inset that REQUIREMENTS 33 lists. It floats except under:
   - 3-button navigation;
   - Compact height;
   - an inset token of 0 (Editorial).
4. **Per-way-out dismissal flags plus a call-site `onDismissAttempt`** (perf `SheetDismissal` `:345-358`, `:397`).
   - A destination's screen cannot reach that call site, so the dirty-form guard is impossible for destination
     sheets.
   - `byBack = false` on a modal sheet gives a dead Back.
   - Use a content-side `SheetDismissGuard`, plus `dismissible` and `scrimTapDismisses`.
5. **A public `SheetBackBehavior.StepDown`** (perf `:353-358`). One gesture closes, as in Gmail (REQUIREMENTS 24) and
   on iOS. Two Backs to close was material3's own bug source (`02b1dbca`, `research.md` §2.4). Stepping down applies
   only to persistent non-modal sheets, automatically.
6. **Non-modal sheets drawn in place inside the screen** (api decision 1 `:38`, decision 3 `:40`). They sit inside the
   recorded glass scene, so a Maps-style sheet, the signature iOS 26 glass sheet, could never be Glass. Draw them in a
   host outside the recorded subtree, resting above the bottom chrome (`design-feel.md:826-828`).
7. **A double Back on an in-screen sheet that also pops the screen** (perf `:837-838`). An accidental double tap must
   not cost the person their screen.
   - Destination sheets already drop extra presses at the presentation root.
   - In-screen sheets match that: they consume Back during their exit (WP03's tested behaviour, `SheetTest.kt:65-79`).
   - The rule is "a burst on a sheet never goes past the sheet's presenter".
8. **A dim that lightens as the sheet reaches Large** (feel's lerp `:947-950`, light 0.20 → 0.10, dark 0.48 black →
   0.10 `#C8C8C8`; api's `scrim × (1 − f)` `:1339`). The screen behind would brighten while the sheet rises. Build it
   only if an iOS 26 recording shows that iOS does this [U].
9. **The form sheet as a scale-in on the `dialog` spring with no drag** (api `:43`, `:1459`; perf `:1066`, `:1075`;
   ADAPTIVE-PLAN:434). On iOS that is the alert's motion language. iPadOS form sheets slide up and are dismissed by a
   swipe. Keep feel's slide-up and swipe-down, and record the override of ADAPTIVE-PLAN:434.
10. **A placement morph during a resize** (feel §4.13 `:851-853`, §7 `:1184`). During a live freeform or ChromeOS
    resize the rect would chase the window. A resize is not navigation: snap to the new frame at the same detent
    (perf `:912`, `:1076-1077`).
11. **A 0.008 H over-pull range above a top detent that is not Large** (api B-8 `:1049-1053`). A Medium-only sheet
    would feel nailed down. Use feel's asymptote to Large's top, or perf's 0.04 H [U].
12. **A navigation results store in the sheet work** (api §2.9 Results, `:924-942`, saved as JSON in `ShellState`).
    It is general navigation, not a sheet feature: leave it to a navigation spec if one is ever needed. In-screen sheets
    use callbacks.
13. **A header hairline** (api `:1414`). iOS 26 uses a scroll-edge fade under bars. Keep feel's 24 dp scroll-edge
    gradient (`design-feel.md:1108-1109`).
14. **Any `Dialog` or `Popup` window for a sheet or its presets**, including the action sheet's popover. A separate
    window can't sample glass, can't drive the card stack, and adds a window to the first frame
    (`research.md` §3 "ModalBottomSheet pitfalls"; `glass/SceneBackdrop.kt:105-113`).
15. **`@JvmInline value class Fraction : SheetDetent`** (perf `:177`). A value class used through its sealed interface
    is always boxed, so it saves nothing. Use a `data class`.

## 5. Open issues none of the three settles

- **A cover presented over a sheet.**
  - A cover is a single-pane scene, so Navigation 3 shows no overlay beneath it. The sheet either vanishes (api's
    `isPresented` path, `design-api.md:883-886`) or slides out and later slides back in.
  - Feel's `SheetSceneTest` "a cover over a sheet" (`design-feel.md:1444`) assumes behaviour nobody has designed.
  - Decide one of two:
    - close the sheet first (HIG [A]);
    - render covers in the presentation host as overlays above a sheet.

  Then test that decision.
- **The dim strength and its curve** across Medium → Large, in light and dark, against an iOS 26 recording [U]
  (feel §18).
- **The detent flick threshold.** 400 dp/s is [D]. QA2 measures it with 20 deliberate drags and 20 flicks on the
  Redmi (`design-feel.md:1638`).
- **`sheetBleed`.**
  - Gutter content sits 8 dp further in at Large (24 dp from the window edge on a phone).
  - The DS components that draw edge to edge must apply `sheetBleed` themselves. Without a code rule, a hero image in
    app code will show 8 dp side strips at Large.
  - Add the rule, then check it in screenshots.

## 6. Code facts checked this session

- `ds/component/overlay/AppSheet.kt:335` `MediumFraction = 0.55f`; `:337` `FormSheetMaxHeight = 620.dp`;
  `:232` large top `statusBar.getTop + LargeGap` [C].
- `nav4/back/BackGate.kt:108` `SETTLED_ENOUGH = 0.99f`, `:109` `MAX_COALESCED = 8` [C].
- `nav4/back/StackBackHandler.kt:67` `enabled = stack.canGoBack || gate.moving` [C].
- navigationevent 1.1.2 sources:
  - `NavigationEventProcessor.kt:272` adds overlay handlers LIFO;
  - `:579` asks overlay handlers before default ones;
  - `NavigationEventHandler.kt:126` disables a handler while its dispatcher is disabled.

  This confirms perf's [V] claim and answers `current-state.md` §4.5's [U].
- `ds/theme/AppMotion.kt:182-186`: `rubberBand` is the shared iOS `c` [C].
