# Sheet system: the specification

Written 2026-10-13 for REQUIREMENTS 33 (`REQUIREMENTS.md:125-136`). This is the single authoritative spec for the sheet
system of every generated app: implementation agents build from it and reviewers check against it. It replaces the
three competing designs (`research/sheet/design-feel.md`, `design-api.md`, `design-perf.md`), which stay as background.

**How it was made.** `design-feel.md` is the base (all three judges picked it). Its public API is replaced by
`design-api.md` §2's shape. The grafts the judges asked for (`research/sheet/judge-1.md` §3, `judge-2.md` §2-§3,
`judge-3.md` §3) are applied, and their rejects are dropped (listed in §22 so reviewers can flag them). Where the
judges disagreed, the majority decides and the decision row says so. No repository or worktree was changed; no Gradle
or adb was run.

**Revision 2** (2026-10-13, same day). Revised in place against two adversarial reviews,
`research/sheet/critique-1.md` (C1-01 to C1-31: performance and Compose correctness) and `critique-2.md` (C2-01 to
C2-64: API, accessibility, adaptive, completeness). Every blocking and major finding is resolved in the section it
concerns; §24 maps each finding to where it landed and records the few parts rejected, with the reason. Revision 1 is
kept at `research/sheet/SHEET-SPEC.rev1.md` for diffing. WP03 has a partial, uncommitted implementation of revision 1
(`wip/`, below); it applies revision 2 to those files before committing (C1-31).

**Precedence.** `REQUIREMENTS.md` > this spec > `design/MASTER-PLAN.md` A3.7/A3.9, `research/MOTION-PLAN.md`,
`research/ADAPTIVE-PLAN.md`, `design/wp/WP05.md` and the handoffs, where they disagree. Every override is marked
**Overrides** in §1.

**Tags.** **[C]** project code, `file:line`. **[V]** verified in a library's shipped sources: by the design authors or
the judges, or by the revision-2 reviews in the 1.12.1 sources jars under `~/.gradle/caches/modules-2/files-2.1/`
(as cited). **[A]** Apple documentation. **[G]** Google documentation or androidx/MDC code. **[M]** measured by a
third party (Flutter's iOS 18 constants, `src/flutter/sheet.dart`). **[D]** derived here. **[U]** unverified: measure
before relying on it (§21 lists who measures each). **taste**: a design value QA2 confirms from recordings.

**Path prefixes.** Worktree heads as read for revision 2; every [C] citation was re-checked at these heads.

- `ds/` = `wt/WP03/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` (head `87a3a06`)
- `wip/` = `ds/component/sheet/`, WP03's uncommitted first implementation of revision 1 (files of 2026-10-13 14:26-14:47)
- `ds4/` = the `ds/` path in `wt/WP04/` (WP04 owns `foundation/layout/Chrome.kt` there)
- `nav4/` = `wt/WP04/templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/` (head `f8ee600`)
- `app4/` = `wt/WP04/templates/android/app/src/main/` (`AppRoot.kt` under `kotlin/com/base/app/ui/`)
- `glass/` = `wt/WP05/templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/foundation/glass/`
  (head `d549b2a`)
- `swift/` = `wt/WP08/templates/ios/Packages/Core/Sources/DesignSystem/Components/Sheet/` (head `00fd9b9`)
- `src/` = `research/sheet/sources/` (pinned androidx, foundation 1.12.1, MDC, Flutter and nav3-recipes copies)
- `ui112/`, `fnd112/`, `rt112/` = `androidx/compose/{ui,foundation,runtime}/` in the 1.12.1 `-android` sources jars;
  `ne/`, `nec/` = navigationevent and navigationevent-compose 1.1.2 sources
- `perf/` = `wt/WP04/templates/android/tools/perf/`

**Reference phone.** Redmi Note 10 Pro, 1080 × 2400 px at 2.75 = 392.7 × 872.7 dp, 120 Hz (8.33 ms per frame), Android
17 (`research/ios-motion.md:15`). Worked numbers use H = 872.7 dp and a 33 dp status bar [U: read it on the phone,
§21]: `largeTop` = 43 dp, Medium's top = 436.4 dp, 2 H/s = 1745 dp/s.

**Definitions used throughout.**

- `y`: the sheet's top edge in px from the top of its bounds (the window for modal sheets). Larger is lower.
- `H`: the height of the sheet's bounds. `h`: the hidden offset (just below the bounds). `W`: their width.
- `o_1 < … < o_n`: the offsets of the active detents, tallest first. `o_n` is the lowest detent.
- `largeTop` = `safeDrawing.top + sheet.detent.largeTopGap` (10 dp). For a stacked sheet see B-54.
- `restBottom`: the line a resting sheet's content ends at. `H − navigationBars.bottom` for a sheet anchored to the
  window; `H − barTop` for a non-modal sheet resting above the tab bar (B-52; never the keyboard).
- `pf` = `presentationFraction`, `e` = `expansionFraction` (both defined in §4.4). `m` = the non-modal ramp (B-51).
- `fl` = 1 while the sheet floats (§6.3), else 0. `i₀` = the style's floating inset (`AppSheetStyle.floatingInset`),
  `R` = its corner radius.
- **Leaving**: the sheet's internal `leaving` flag (§4.4), set in the same frame as any dismissal decision or `hide()`
  and cleared only by `show()`. Never derived from foundation's `targetValue`.
- **Placement class**: the size class a sheet's own metrics come from: the window's for a modal sheet, its pane's for
  a non-modal one (D-20, §7.2). Never the caller's container class.
- Cost classes are MOTION-PLAN §2's (`research/MOTION-PLAN.md:66-75`): **L0** layer properties only, **L1**
  re-placement, **D1** re-recording one card-size node, **D2** one full-screen blended rect. They appear in cost columns
  and in §10.
- `D-n` are this spec's decisions (§1). `D1`-`D16` (sheet), `A1`-`A5` (action sheet) and `N1`-`N11` (navigation seams)
  are the defects of `research/sheet/current-state.md` §2.6, §3 and §4.4, cited as "fixes D7". `C1-n`/`C2-n` are the
  revision-2 findings (§24).

---

## 1. Decisions at a glance

| # | Decision | Rejected alternative | Why |
|---|---|---|---|
| D-1 | **One component, one state type, two hosts.** Destination sheets render in WP04's presentation host (layer 3). In-screen sheets of every modality render in WP03's overlay host (layer 4). Both hosts are outside the recorded glass scene. Both register in one window-level `SheetStack`, which drives the card stack, Back order and the dim | In-place hosting of non-modal sheets (design-api); per-level "sheet layers" (design-perf, never specified) | A non-modal sheet inside the recorded scene could never be Glass (the iOS 26 Maps card), would draw under the floating bar, and would leave the bar bright under its dim (judge-1 reject 6, judge-3 X7/X8) |
| D-2 | **Destination sheets are always modal.** Non-modal sheets are in-screen only | Non-modal destinations | Touch pass-through from the presentation host to a tab host is a cross-host path nobody needs; a persistent sheet belongs to its screen |
| D-3 | **Detents are a sealed type**: `Large`, `Medium`, `FitContent`, `Peek`, `Fraction`, `Height`, `Custom(id, resolver → Dp?)`. Default `[Large]` | Enum `Medium`/`Large` (`ds/component/overlay/AppSheetState.kt:20`); default `{Medium, Large}` | iOS's model and default ("By default, sheets support the large detent" [A]). `Peek` (grabber band plus header, as measured) exists because a fixed-height peek clips its header at font scale 2.0 (C2-35) |
| D-4 | **Release rule:** a flick ≥ 400 dp/s moves exactly one detent; dismissal needs ≥ 2 H/s, or the sheet released ≥ 48 % of the way from the lowest detent to hidden; otherwise the detent nearest `y + 0.099 s · v` (B-22) | 2 H/s as the only flick threshold (design-api B-10, design-perf §4.3); a projected 52 % position (revision 1, inverted from Flutter, C1-24) | Under 2 H/s, a deliberate 1000 dp/s flick from Medium projects 99 dp and falls back: "the sheet ignored me". 2 H/s stays the dismiss rule. 400 dp/s is [U], tuned on the phone |
| D-5 | **Rubber band** `c = 0.55` (shared). Above the top detent the asymptote runs to Large's stretch line (0.008 H above `largeTop`); below the lowest detent while dismissal is blocked it runs to hidden. A list's nested scroll reaches the same band (B-28) | 0.008 H above any top detent (design-api B-8) | A Medium-only sheet would get 7 dp of stretch and feel welded (judge-1 reject 11) |
| D-6 | **Floating at partial detents, attached at Large** (iOS 26), with one uniform corner radius. On by default for every material; the inset is a per-design-style token (Editorial 0). Never floating under 3-button navigation, at Compact height, or for a sheet without a partial detent. A stop within 1 px of Large's offset counts as Large whatever detent produced it (R-6a) | Floating off for Solid (design-perf decision 15) | Solid is every shipped style's material: the default app would never show REQUIREMENTS 33's inset sheet |
| D-7 | **Content is measured once, at the sheet's full width.** The floating inset is a draw-time clip; the header slot sits inside a constant `i₀` side inset; `contentPadding` carries the sheet's gutter at start and end, so text never meets the clip (C2-23). No `sheetBleed` modifier | Content measured at the floating width plus `Modifier.sheetBleed()` (design-feel) | No adoption tax, no 8 dp strips at Large, nothing reflows either way (judge-3 X2; judge-1 open issue) |
| D-8 | **One dim per presenter, drawn once**, as one rect whose colour and alpha are the exact composite of the card overlay and every scrim that reaches that presenter (§6.4). It never lightens as the sheet rises. The scrim colour stays `colors.scrim` | A lerp from scrim to overlay that lightens toward Large (design-feel, design-api `× (1 − f)`); two rects in dark mode (design-perf) | Majority (judges 1, 2). One D2 rect in both themes. iOS's dim strength is [U]: QA2 compares, then the token's values change, never the mechanism (judge-3 X17) |
| D-9 | **Card stack**: the presenter scales to 0.9165 about its top centre and its top moves to `safeDrawing.top`; the sheet's top at Large is `safeDrawing.top + 10 dp`, so the presenter peeks 10 dp. **Only at Compact window width**, and a sheet can opt out (`recedesPresenter = false`); elsewhere the dim alone. **Overrides** MOTION-PLAN M6's "moves down by the status bar + 10 dp" | 7 % of H; status-bar-only tops; receding at every width (revision 1, C2-22) | On Android's short status bars 7 % leaves a black band; `safeDrawing` covers a ChromeOS caption bar and a landscape cutout (judge-1 graft 5). iPadOS never shrinks a desktop-width window behind a sheet |
| D-10 | **Stacked sheets**: the covered sheet lifts 0.005 H and scales to 0.9165; the covering sheet's Large top is the covered sheet's lifted top + 10 dp; at most two covered sheets are drawn | The new sheet 0.02 H higher (design-api B-36) | 0.02 H higher puts the grabber in the Redmi's status bar and punch-hole; a 10 dp peek reads as a stack (judge-2 graft 25) |
| D-11 | **Confirm hook in the content**: `SheetDismissGuard(enabled, onAttempt: (SheetDismissSource) -> Unit)`, with `SheetDismissConfirmation` as the ready-made Discard / Keep Editing form. `dismissible = false` is the static "never closable by the person" switch. `scrimTapDismisses` is the only per-path switch | Call-site `onDismissAttempt` plus per-path flags (design-perf) | A destination's screen never sees the `AppSheet` call, so only a content-side guard can protect a dirty form there |
| D-12 | **Back closes in one gesture**: MDC bottom container, eased progress, content counter-scale. Only a persistent non-modal sheet steps down, automatically. Back never reaches the screen beneath a **modal** sheet | Public `SheetBackBehavior.StepDown`; Back passing through a non-dismissible modal sheet (design-feel §4.5) | Gmail and iOS close in one gesture; two Backs to close was material3's bug source (`02b1dbca`) |
| D-13 | **Rapid Back during a dismissal**: a destination sheet pops first and slides out in `onRemove()`, and the presentation host's `BackGate` counts the extra presses; an in-screen sheet consumes and drops them | Pop after the slide (design-perf); extra presses popping the screen beneath an in-screen sheet (design-perf) | Navigation 3's `OverlayScene` contract animates after the pop; a reflexive double press must not cost the person their screen (WP03 `SheetTest.kt:65-79`) |
| D-14 | **Structural Back routing** (B-37, B-44): explicit enablement of the sheet's handler, and the default-priority stack handlers beneath a modal sheet disabled while it is up, so no ordering of the IME's callback can let Back reach what is beneath | Trusting registration order; disabling the sheet's handler while the IME is up (revision 1, C1-09) | `navigationevent` asks overlay handlers before default ones, LIFO across child dispatchers [V] (`NavigationEventProcessor.kt:272`, `:579`), but registers one platform callback per priority, which can be re-registered after the IME's [V] (`ne/OnBackInvokedInput.android.kt:76-91`) |
| D-15 | **`HostMotion`** for both hosts: a sheet's show, hide, park and unpark are reported to its host's gate as **overlay motion**, which makes `NavGuard` wait, never drop | Reporting only destination sheets; reporting as stack motion (revision 1: `NavGuard` drops a push while `lastChange == Push`, `nav4/back/NavGuard.kt:21-23`, C1-08) | In-screen sheets also move their host's content (judge-1 graft 3); a push submitted during a sheet's motion must be applied after it, never lost |
| D-16 | **Haptic on arrival**: the `Threshold` haptic plays on the first frame the settle has ≤ max(1 px, 2 % of its travel) left to a new detent | On settle start (`ds/component/overlay/AppSheetState.kt:194-195`); within 1 px (revision 1: ~0.5 s after release, C1-24) | The tick must coincide with the visible snap; a critically damped spring's last pixel arrives 150-250 ms after the motion looks done |
| D-17 | **Materials Solid / Frosted / Glass.** `Automatic` follows the design style's `sheetMaterial` token (Solid unless the Glass design style). Glass samples live only at partial detents on the Live tiers, only when its presenter is the shell and no cover is up, and is opaque by `e = 0.5`; otherwise it draws Frosted. **Overrides** `design/wp/WP05.md:64` ("no glass on sheets") | `Automatic` = Glass whenever glass ships (design-feel); sampling at every level (revision 1, C1-13) | REQUIREMENTS 33 is later and wins; live half-screen sampling must not be the default on a GPU-bound phone (CP1 §1: push/pop GPU p90 12 ms against 8.33 ms); the recorded scene holds only tab content (`glass/GlassStage.kt:47-50`), so above level 1 it is the wrong picture |
| D-18 | **Shadow**: none on modal bottom sheets unless their scrim is faint (alpha < 0.2). Non-modal, side, form and anchored-card surfaces draw a nine-slice made once from a small round-rect shadow, outside the shape only, light theme only | A `dropShadow` per surface size (all three designs) | [V] `ShadowRenderer.kt:70-79` rebuilds a CPU-blurred Alpha8 bitmap whenever the size changes, and `AndroidShadowContext.android.kt:53-84` caches one per size without a bound; a floating sheet changes size on every drag frame (judge-2 §2.1, blocker) |
| D-19 | **Form sheet** (Center placement): slides up, can be swiped away, no detents, height `min(0.85 H, 620 dp, fit)`, predictive back scales it about its centre to 0.90. **Overrides** `ADAPTIVE-PLAN.md:434` (scale-in on `dialog`, no drag). **taste**: confirmed or reverted at the sheet checkpoint from QA2's recording | Scale-in on `dialog` with no drag (design-api, design-perf, judge 3) | Majority (judges 1, 2): iPadOS form sheets slide up and swipe away; a scale-in is the alert's language |
| D-20 | **Placement and metrics**: modal sheets resolve from the **window** class, non-modal sheets from their pane's class. Every sheet metric (width, gutter, form width) and the content's own adaptive locals come from that placement class, resolved inside the sheet (§7.2, §11.4). Automatic: Medium width and up → Center for modal, Start for non-modal from Expanded width. A size-class change snaps to the new frame at the same detent. **Overrides** ADAPTIVE-PLAN's "every rule reads `AppTheme.containerClass`" for sheets | Container class for every sheet; a morph on resize (design-feel §4.13); metrics read in the caller (revision 1, C2-02) | `HostedOverlay` re-provides the caller's locals, including the container class and `AppLayout` (`ds/component/overlay/AppOverlayHost.kt:89-92`, `ds/foundation/adaptive/WindowClass.kt:60-72` [C]): a modal sheet from a Compact pane on a tablet would come up 540 dp wide with phone gutters, and its content would lay out as the caller's pane. A resize is not navigation |
| D-21 | **Action sheet at Medium width and up**: an in-window card anchored to a `SheetAnchor`, modal for accessibility | `AppMenu`'s `Popup` window (`ds/component/overlay/AppMenu.kt:40-42` [C]) | One window, glass can sample it, and A4 (wrong anchor) is fixed |
| D-22 | **No `Dialog` or `Popup` window** anywhere in the sheet system, the DevTools panel included (§13) | — | No glass sampling across windows (`glass/SceneBackdrop.kt:105-113` [C]), no card stack, an extra window in the first frame. The focus isolation a dialog window gave is rebuilt by B-42a |
| D-23 | **Navigating from a presented sheet**: a sheet key stacks on it; a cover or an ordinary key first closes the presented sheets, then is submitted once the presentation gate settles. This settles "a cover over a sheet": it never happens | Cover as an overlay scene above the sheet | With `[base, sheet, cover]` the cover is a non-overlay scene, so the sheet's `OverlayScene` is removed and the sheet would vanish mid-push (judge-2 §2.2); HIG: close the sheet before navigating [A] |
| D-24 | **In-screen sheets follow their owner**: parked instantly while the owner's tab is not selected; slid away on `sheet` while the owner is covered by a push or a presentation, and back when uncovered; moved with the owner's own pose during a predictive pop, a push arrival and a tab reveal (B-14a). Parking keeps the sheet placed at layer alpha 0 off the window, as `KeptTabs` parks a tab. Logical state is kept | Kept drawn over the next tab (N7); hidden with a pop-in; parking by not placing (revision 1: drops every layer, C1-12) | Fixes N7 and judge-3 G23 without a visible pop; REQUIREMENTS 24/32 forbid the double exposure of a full-size sheet over a shrinking back card (C1-17, C2-05); `nav4/shell/tabs/KeptTabs.kt:38-42` measured why unplacing is wrong |
| D-25 | **The sheet adds no scroller.** `SheetColumn` is the column for forms. `contentPadding` is a snapshot-backed `PaddingValues` read in measure. No `fitInside(Ime)` or `imePadding()` inside a sheet | Wrapping content in `verticalScroll` (VitFleet); the edge-to-edge skill's `fitInside` | A wrapper scroller breaks lazy lists; [V] `RulerAlignment.kt:36-50` re-measures `fitInside`'s child in placement on every IME frame |
| D-26 | **Keyboard**: `SheetKeyboard.Lift` (default) rides the keyboard in placement; `Expand` goes to the top detent. While the IME target is visible, the content box is measured to the region actually visible (two re-measures per IME transition). Detents resolve only from the keyboard-free measure | Content padding only (keeps `bringIntoView` aimed at an off-screen viewport); detents from the IME-constrained measure (revision 1, a wobble per IME transition, C1-10) | Fixes D4 including focus scrolling at a partial detent |
| D-27 | **`AppSheetState` is a concrete `@Stable` class.** **Deviation** from CODE-STANDARDS API-8 (interface + factory) | Interface + factory (design-feel; judge 2 kept it) | Judge 3 (G2), design-api and design-perf: the layout writes anchors and reads motion specs through internal members an interface would have to publish, or reach by a cast; material3's `SheetState` and foundation's `AnchoredDraggableState` are classes |
| D-28 | **`AppSheetStyle`** bundles the look (material, colours, container brush, corner, inset, max width, shadow), built by `AppSheetDefaults.style()`. **Deviation** from CODE-STANDARDS API-7 (no grab-bag): it holds no structural variant, colours still resolve through `AppSheetColors`, and it maps 1:1 onto Compose's `Style` when that API is stable | Flat `cornerRadius`, `floatingInset`, `maxWidth`, `colors` parameters (judge 2) | Majority (judges 1, 3); keeps the §4.1 call sites short and the Styles migration mechanical |
| D-29 | **Tokens**: a new `sheet` section in `tokens/motion.json`; `sheetInset` and `sheetMaterial` per design style in `tokens/styles.json`. Reduce-motion fades use the existing `fades.longMs`/`fades.shortMs`. **Overrides** MOTION-PLAN §1 rule 5's 150 ms literal | Private `val`s (`ds/component/overlay/AppSheet.kt:335-345` [C]) | One source of truth for Android, Multiplatform and SwiftUI |
| D-30 | **Medium = 0.5 H** (top at the window's middle). **taste** against WP03's 0.55 | 0.55 (`AppSheet.kt:335` [C]) | iOS "approximately half" [A] |
| D-31 | **Zoom from a source** ships in phase 2 (§20) as `SheetTransition.Zoom`. `SheetTransition` is a value class with companion constants (D-37), so adding `Zoom` breaks no caller | Zoom in v1 (design-feel); "sealed, so not breaking" (revision 1: adding a sealed subtype breaks every exhaustive `when` in callers, C2-25) | Not in REQUIREMENTS 33; it adds a second progress, a source registry and a glass morph |
| D-32 | **No navigation results store** in this work (`NavResultKey`, `popWithResult`); RECIPES shows a picker destination returning its value through a shared ViewModel or a result callback (§19.7) | design-api §2.9 | A navigation-wide feature; WP04's backlog |
| D-33 | **Levels follow the window's stack order.** A modal sheet registers at the top visible level + 1 whoever opens it, so a modal sheet opened beside a persistent sheet stacks on it, and every presenter between it and the nearest lower modal sheet (or the shell) draws its dim (B-54a) | Owner level + 1 (revision 1: two sheets at one level, the dim drawn under the persistent sheet, C1-16, C2-06) | iOS presents from the top-most presented controller; the persistent sheet must be dimmed and blocked with the rest |
| D-34 | **Focus moves into every modal sheet**, in every input mode: saved on present, the surface focused (a pane-titled focus group, never a field), the app root closed to focus while it is up, restored on close in keyboard mode. Escape is handled on the surface (B-42, B-42a) | Focus moved only when opened by keyboard (revision 1, C2-01) | Without a dialog window, a focused field beneath keeps focus and its keyboard, Tab starts beneath, and TalkBack stays on the hidden trigger; MDC moves TalkBack into its sheet (`src/mdc/BottomSheetBehavior.java:2520`, `:2537`) |
| D-35 | **Reduce motion honours "Remove animations"**: present, dismiss, settle and the card stack go to their end values in one frame; drags follow the finger. `SheetTransition.CrossFade` keeps its 200/120 ms fades only at a normal duration scale (B-63) | A 200 ms fade under reduce motion (revision 1: impossible, Compose scales it to 0, C2-08) | `ds/theme/ReduceMotion.kt:41-42` reads `ANIMATOR_DURATION_SCALE == 0`, which Compose's `MotionDurationScale` follows [V] (`ui112/platform/WindowRecomposer.android.kt:97`, `:427-446`) |
| D-36 | **Modal side sheet**: an explicit `Start`/`End` on a modal sheet is a full-height panel with one stop, slid in from its edge, dismissed by a drag toward the edge, predictive back with MDC's side-container values (§8) | Start/End as the non-modal Maps panel only (revision 1, C2-18) | REQUIREMENTS 33 names "side sheet on larger windows"; the DevTools panel and tablet filter panels need it |
| D-37 | **Evolvable public types**: options callers only pass in (`SheetPlacement`, `SheetContentInteraction`, `SheetTransition`, `SheetKeyboard`) are `@JvmInline value class`es with companion constants, as Compose's `Role` and `TextAlign`; `SheetDetent` and `SheetDismissSource`, which callers branch on, document that values will be added and `when` needs `else`. Option holders are plain classes with `copy`, not data classes | Sealed and enum option types; public data classes (revision 1, C2-25, C2-46) | Generated apps take template changes through `upgrade_project.py` (API-14); a new enum value or sealed subtype breaks exhaustive `when` (Kotlin ≥ 1.7), a new data-class property breaks `componentN` and positional `copy` |
| D-38 | **Shell seams are opt-in** (`@ShellSeamApi`, `RequiresOptIn(ERROR)`), and the in-place sheet is `AppSheetLayout` behind it | Public seams and a third `AppSheet(state, …)` overload (revision 1, C2-27, C2-44) | `AppSheet(state = s, onDismissRequest = …) {}` must not compile into a sheet that never appears; features never see `SheetStack` in completion |
| D-39 | **Content is one root in a `Box`** measured with Large's height as its maximum, children stacked at the top start (B-1a) | No layout scope (revision 1, C2-24) | Two implementers would otherwise build overlap, stacking or a crash; a `ColumnScope` would fight lazy lists |
| D-40 | **In-screen sheet state survives recreation**: each hosted overlay is keyed by its call site's compound key and composed inside the call site's `SaveableStateHolder` (§11.1) | `key(entry)` with an identity-hashed entry (revision 1: every `rememberSaveable` inside is lost on recreation, C1-03, C2-03) | `rt112/GapComposer.kt:3085-3097` mixes `dataKey.hashCode()` into the compound key that `rememberSaveable` keys by [V] |

---

## 2. Feel rules

Every later section implements these. Reviews cite them.

- **F1 The finger owns the sheet.** It tracks 1:1. A touch catches a sheet that is presenting or settling. A release
  seeds a spring with its velocity. Never a tween after a finger (`MOTION-PLAN.md:54-58`).
- **F2 Content never reflows while the sheet moves.** It is measured once per settle and never during a drag; only the
  surface around it changes.
- **F3 Stops are chosen by intent; dismissal needs conviction.**
- **F4 Edges resist; nothing stops dead.** Past the top, and past the bottom when dismissal is blocked, the sheet
  rubber-bands with iOS's `c = 0.55`, whether a finger drags the surface or a list hands its scroll to the sheet.
- **F5 Floating while partial, anchored when full.**
- **F6 One dim per presenter, drawn once**, and it never lightens as the sheet rises.
- **F7 A haptic marks arrival**, never departure.
- **F8 No shadow follows a size that animates, and nothing ever fades through a shadow.**
- **F9 One gesture closes**, unless the content says it is precious (the guard).
- **F10 Every per-frame value is read in placement, a layer block or draw.** Zero recompositions per drag frame.
- **F11 A per-frame read re-records only its own node.** A draw node that reads a per-frame value wraps a layer, and
  every child that moves gets its own layer, so a frame of motion re-records a rect or two, never a subtree (§10.3).
- **F12 The sheet belongs to its owner.** It moves, parks and leaves with the screen that opened it; it never hangs
  over a screen that is shrinking, sliding or gone (B-14, B-14a, B-4a).

---

## 3. Anatomy, hosts and states

### 3.1 Layers

```
window root (outermost AppTheme: LocalSheetStack, the SheetBackdropHolder, the overlay host)
├── app content box        AppOverlayHost's first child (ds/component/overlay/AppOverlayHost.kt:57-62):
│   │                      HideBeneathNode (semantics) and the focus guard of B-42a; no pointer modifiers
│   ├── shell, level 0     Modifier.cardStackPresenter(stack, level = 0) on ShellChrome's box:
│   │   │                  recede layer → dim-and-clip draw node → content layer (§6.4)
│   │   └── GlassStage     (glass only) records the tab content only
│   ├── presentation host  layer 3: destination sheets (SheetScene → PresentedSheetFrame) and covers (PresentedFrame);
│   │                      a cover's frame applies cardStackPresenter(stack, level = its level)
│   └── DevToolsOverlay    debug builds only: above the shell and the presentation host (app4/…/ui/AppRoot.kt:86-89),
│                          so a destination sheet neither dims nor recedes it; below the overlay host, so it is
│                          covered by in-screen sheets and hidden from accessibility under any modal one
├── overlay box, layer 4   in-screen sheets of every modality, action and choice sheets, alerts, menus, HUD
└── toasts, layer 5

SheetLayout: one Layout per sheet, filling its bounds; the traversal group (§9)
├── scrim node        hit-testing and semantics; placed only while it blocks (m > 0 or modal, B-51); draws the dim
│                     itself only when no presenter claims it (§6.4) or for a pane-bounded non-modal sheet (B-51)
└── sheet layer       placed at y (L1); graphicsLayer: back shrink, covered-sheet recede, owner pose, park alpha (L0)
    ├── surface node  SheetSurfaceNode: body, brush, rim, nine-slice shadow, Glass backdrop through SheetBackdrop;
    │                 a constant local rect while attached (§6.3); carries paneTitle and the focus group (§9)
    ├── grabber node  its own layer; owns hit-testing, focus, click, hover and semantics; the slot only draws (§4.8)
    ├── header        its own layer; pinned above the content, inside a constant i₀ side inset
    ├── content       its own layer: clipped to the visible surface in draw; back counter-scale (L0)
    └── footer        its own layer, placed at the visible bottom or on the keyboard (L0 per frame)
```

The z-order is content (0) < tab bar (1) < presentation host (3) < overlay host (4) < toasts (5)
(`ds/component/overlay/AppOverlayHost.kt:20-33` [C]; `nav4/presentation/PresentationHost.kt:53-56` [C]).

### 3.2 Levels

- Level 0 is the shell. A presented entry at presentation depth `d` is level `d`. WP04 provides
  `LocalPresentationLevel` to each presented entry's content (§4.10).
- A **non-modal** sheet's level is its owner's level + 1.
- A **modal** sheet's level is `max(owner level, the highest level of a visible, unparked sheet in its window) + 1`
  (B-54a). So an action sheet opened from a Maps screen whose persistent sheet is up stacks on the persistent sheet,
  as iOS presents from the top-most presented controller.
- A sheet's **presenter** is whatever sits at the level directly below it: the shell, a cover, or another sheet. The
  presenter recedes (card stack, B-54). The **dim** of a modal sheet reaches every presenter from its own down to the
  nearest lower modal sheet or the shell, each drawing one rect over itself (§6.4).
- Every sheet provides its own level to its content through `LocalPresentationLevel`, so a sheet opened from it
  stacks on it.

### 3.3 States and interruption

```
Hidden ──show──▶ Presenting ──arrives──▶ Resting(detent) ◀──settle── Settling ◀──release── Dragging
                     │  ▲                     │  ▲                      │  ▲                  ▲
                     │  └──finger catches─────┼──┼──────────────────────┘  │                  │
                     │                        │  └──finger down───────────┼──────────────────┘
                     └──────── dismiss ───────┴──────────────▶ Leaving ──gone──▶ Hidden
Parked (in-screen only): any state above except Leaving ⇄ Parked, logical state kept (B-14)
```

| Running | Finger down on the sheet | Back key (no progress) | Back gesture (predictive) | `animateTo` | `hide()`, `visible = false`, pop | `show()` | Window resize |
|---|---|---|---|---|---|---|---|
| Presenting | catches it, velocity kept (B-17) | reverses into the dismissal, velocity kept | stops at the current offset and shrinks from there (B-38a) | retargets, velocity kept | reverses to hidden, velocity kept | no-op | anchors retarget in flight |
| Resting | drag | dismisses (B-41) | predictive dismiss (B-38) | animates | leaves | no-op | stays on its detent |
| Settling | catches it | dismisses, velocity kept | stops and shrinks (B-38a) | retargets | leaves, velocity kept | no-op | retargets |
| Leaving, decided by the person | ignored | destination: counted by the host gate; in-screen: consumed (B-43) | as the key | ignored (D9) | joins | turns around with `lastVelocity` (D1) | hidden retargets |
| Leaving, programmatic | ignored | as above | as above | ignored | joins | turns around | — |
| Parked | not hit-tested | not enabled | not enabled | ignored | leaves (no animation needed) | no-op | anchors move |

A person who flicked a sheet away has committed; catching it on the way out would undo a decision. A program that
hides and re-shows within the slide gets a smooth reversal instead of a stuck sheet. A Back gesture is not yet a
decision, so it never starts a reversal until it commits (C2-07).

---

## 4. Public API (Android, `:core:designsystem` and `:core:navigation`)

Package `com.base.app.core.designsystem.component.sheet` unless noted. Every public declaration gets KDoc (C-4). The
KDoc below is the intent the real KDoc must carry, trimmed of what the signature already says (C-5). Public names are
checked mechanically: `:core:designsystem` and `:core:navigation` carry the Kotlin binary-compatibility validator with
committed `api/*.api` dumps, and the sheet package's dump must equal this section (§19.1).

### 4.1 Acceptance examples

Each block is the whole call for a common sheet. **Review rule: a change that adds an argument to one of these, or
makes one need another statement, is wrong.** Length is counted in arguments, not characters: the blocks are wrapped
at 100 columns (K-1) and ktlint may reflow them. They are compiled as tests: the design-system blocks in
`SheetApiShapeTest`, the destination block in `SheetSpecShapeTest` (§15.2), against stub composables in the test
source sets (`FilterList`, `PlaceRow`, `RenameField`, `SearchHeader`, `PlaceList`, `TrackList`, `ShareRoute`,
`ShareKey`, and string resources `orders_filters`, `lists_rename`).

```kotlin
// A quick in-screen sheet: full height, a title and a close button.
AppSheet(
    visible = showFilters,
    onDismissRequest = { showFilters = false },
    header = { AppSheetHeader(title = stringResource(R.string.orders_filters)) },
) {
    FilterList(contentPadding = contentPadding)
}

// Half height that can grow, with a list handing off to the sheet.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
) {
    LazyColumn(contentPadding = contentPadding) {
        items(places, key = { it.id }) { PlaceRow(it) }
    }
}

// A short form sized to its content, a sticky Save above the keyboard, a confirmation while dirty.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(listOf(SheetDetent.FitContent)),
    header = { AppSheetHeader(title = stringResource(R.string.lists_rename)) },
    footer = {
        AppBottomActionBar {
            AppButton(text = save, onClick = onSave, modifier = Modifier.weight(1f))
        }
    },
) {
    SheetDismissConfirmation(enabled = form.changed, onDiscard = { show = false })
    SheetColumn { RenameField(form.name, onNameChange) }
}

// A Maps-style persistent sheet: a peek that fits its header, half and full;
// the map stays usable up to half.
AppSheet(
    visible = true,
    onDismissRequest = {},
    state = rememberAppSheetState(
        listOf(SheetDetent.Peek, SheetDetent.Medium, SheetDetent.Large),
    ),
    modality = SheetModality.NonModal(upThrough = SheetDetent.Medium),
    dismissible = false,
    header = { SearchHeader() },
) {
    PlaceList(contentPadding = contentPadding)
}

// Glass through the liquid glass feature's tiers: glass while partial, opaque at Large.
AppSheet(
    visible = show,
    onDismissRequest = { show = false },
    state = rememberAppSheetState(listOf(SheetDetent.Medium, SheetDetent.Large)),
    style = AppSheetDefaults.style(material = SheetMaterial.Glass),
) {
    TrackList(contentPadding = contentPadding)
}

// A custom detent: a tenth of the room, never under 44 dp (Apple's sample).
val Sliver = SheetDetent.Custom("sliver") { maxOf(44.dp, maxHeight * 0.1f) }

// Choices.
AppActionSheet(
    visible = show,
    actions = actions,
    onDismissRequest = { show = false },
    title = title,
)
AppChoiceSheet(
    visible = show,
    choices = languages,
    selected = current,
    onSelect = onLanguage,
    onDismissRequest = { show = false },
    label = { it.displayName },
)
```

```kotlin
// :core:navigation. A destination sheet, registered once; navigating to the key opens it over
// the tab bar.
entry<ShareKey>(
    presentation = Presentation.Sheet(
        SheetSpec(detents = listOf(SheetDetent.Medium, SheetDetent.Large)),
    ),
) {
    ShareRoute(it)
}
```

### 4.2 Surface map

| Layer | Types | Who uses it |
|---|---|---|
| Component | `AppSheet` (two overloads: `visible`, `item`) | every sheet |
| State | `AppSheetState`, `rememberAppSheetState` | callers that need detents, selection, programmatic moves or the visible frame |
| Detents | `SheetDetent`, `SheetDetentContext` | callers with custom heights |
| Behaviour | `SheetModality`, `dismissible`, `AppSheetProperties` (`SheetPlacement`, `SheetContentInteraction`, `SheetTransition`, `SheetKeyboard`, `scrimTapDismisses`, `dragEnabled`, `recedesPresenter`, `contentUnderGrabber`, `keepContent`), `SheetDismissSource` | callers that change what the sheet does |
| Look | `AppSheetStyle`, `SheetMaterial`, `AppSheetColors`, `AppSheetDefaults` | callers that change how it looks; the theme |
| Slots and content | `AppSheetScope`, `AppSheetGrabber`, `AppSheetHeader`, `AppSheetCloseButton`, `SheetColumn`, `sheetContentPadding()`, `ProvideSheetHeader`, `ProvideSheetFooter`, `ProvideSheetStyle`, `SheetBackHandler`, `SheetDismissGuard`, `SheetDismissConfirmation`, `Modifier.sheetDragExclusion()`, `LocalSheetSurface` | content, including a destination's screen |
| Presets | `AppActionSheet`, `SheetAction`, `SheetAnchor`, `AppChoiceSheet` | quick choices |
| Navigation (WP04) | `Presentation.Sheet(SheetSpec)`, `SheetSpec`, `AppSheetNavHost` | features |
| Seams (`@ShellSeamApi`, opt-in at ERROR level) | `AppSheetLayout`, `SheetStack`, `LocalSheetStack`, `Modifier.cardStackPresenter`, `HostMotion`, `LocalHostMotion`, `EntryVisibility`, `EntryPose`, `LocalEntryVisibility`, `PaneBounds`, `LocalPaneBounds`, `BarFrame`, `LocalBarFrame`, `LocalPresentationLevel`, `SheetBackdrop`, `SheetFrame`, `SheetBackdropHolder`, `LocalSheetBackdrop` | the shell and navigation (WP04), liquid glass (WP05); a code rule forbids the opt-in under `feature/**` |

Rules the whole surface follows (CODE-STANDARDS §2):

- **Deviation** from API-3 ("modifier on the outermost layout"): `AppSheet`'s `modifier` is applied to the sheet
  **surface**, not the bounds-filling host box, so `testTag`, `semantics` and focus land on the sheet (material3 does
  the same, `src/ModalBottomSheet.kt:159` [G]). It is for test tags, semantics and focus only: the sheet owns its own
  size, position, padding, clip and layers, and a size, offset, padding, clip, shadow or `graphicsLayer` modifier
  passed to it is ignored where it conflicts and flagged by a code rule (§19.1 item 9). The sheet's own semantics are
  chained after `modifier`, so the sheet's `paneTitle` wins over a caller's `semantics { paneTitle = … }`; use the
  `paneTitle` parameter.
- No `title: String`, `showCloseButton` or `showHandle` flags: those are slots with ready-made content (API-5).
- Every frequently changing value is a `@FrequentlyChangingValue` getter read in layout or draw (API-8, R7). No
  `State<T>` parameters.
- No visual value is read anywhere except from `AppSheetStyle`, and `AppSheetStyle` holds nothing that is not visual.
- Types callers branch on (`SheetDetent`, `SheetModality`, `SheetDismissSource`, `SheetMaterial`) will gain values:
  their KDoc says so and a `when` over them must carry `else` (D-37). Option types callers only pass in are value
  classes with companion constants.

### 4.3 `AppSheet`

File `component/sheet/AppSheet.kt`.

```kotlin
/**
 * A sheet in the app's design language with iOS 26 behaviour. It rises to one of [state]'s detents,
 * follows the finger between them, floats inset with rounded corners at partial detents and attaches
 * to the window's edges at [SheetDetent.Large], where the screen behind recedes into a card on a
 * phone. The person closes it by dragging it down, tapping the dimmed screen, Back (the predictive
 * shrink), Escape, or a close button.
 *
 * Keep it composed and drive [visible]: do not put the call inside an `if` or a lazy item. Showing
 * it again while it leaves turns it around at the speed it had. [onDismissRequest] runs once, when
 * the person has decided to close it, and the sheet is already leaving: set [visible] to false there.
 * [visible] is the truth: a sheet closed from [AppSheetState.hide] while [visible] is still true
 * calls [onDismissRequest] too.
 *
 * The content is laid out in a box as tall as the sheet at [SheetDetent.Large], children at the top
 * start. Bring one root: a lazy list or [SheetColumn], given [AppSheetScope.contentPadding], which
 * keeps the gutter at the sides and the last item above the footer, the navigation bar and the
 * keyboard. The content is never re-measured during a drag. The sheet adds no scroll container.
 *
 * A modal sheet draws over the whole window, the tab bar included, and takes focus while it is up.
 * A non-modal sheet stays within its pane and rests above the tab bar at its undimmed detents.
 *
 * @param state detents and position; one per call site.
 * @param dismissible false: the person cannot close the sheet. A drag down rubber-bands, Back on a
 *   modal sheet is consumed, and attempts go to a [SheetDismissGuard] in the content. To ask before
 *   closing a dirty form, use a guard or [SheetDismissConfirmation] instead. Code can always close it.
 * @param paneTitle what screen readers call the sheet. Wins over the header's title; without either,
 *   "Sheet".
 * @param onDetentChange runs when the person settles the sheet on a different detent; not for moves
 *   made from code.
 * @param onHidden runs once the sheet has left the screen, however it was closed: show the next
 *   sheet from here.
 * @param grabber drawn over the top band; null for none. The sheet keeps the grabber's touch target,
 *   focus, click and accessibility actions whatever this draws, and hides it when the sheet has one
 *   active detent, is a form or side sheet, or is covered by another sheet.
 * @param header pinned above the content. Content can replace it with [ProvideSheetHeader].
 * @param footer pinned at the visible bottom, above the navigation bar and the keyboard. Content can
 *   replace it with [ProvideSheetFooter].
 */
@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    paneTitle: String? = null,
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    onHidden: (() -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
)

/**
 * A sheet for [item] while it is not null, like SwiftUI's `sheet(item:)`. The last item stays
 * composed during the exit. A different item while one is shown closes the sheet, then presents
 * the new one: one sheet at a time, as the HIG asks.
 */
@Composable
fun <T : Any> AppSheet(
    item: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    state: AppSheetState = rememberAppSheetState(),
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    paneTitle: String? = null,
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    onHidden: (() -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.(T) -> Unit)? = null,
    footer: (@Composable AppSheetScope.(T) -> Unit)? = null,
    content: @Composable AppSheetScope.(T) -> Unit,
)
```

Parameter order (API-2): required, `modifier`, the state (as material3 places `sheetState`), behaviour, the
`XDefaults` value, accessibility, the callbacks, then slots. Both overloads host themselves through `HostedOverlay`
(B-12).

The sheet without its own visibility, for a presenter that owns where it is drawn (the shell's sheet scene, the
catalog's fake presenter), is a seam, not a third overload (D-38; a same-named overload taking `state` would make
`AppSheet(state = s, onDismissRequest = …) {}` compile into a sheet that never appears):

```kotlin
// component/sheet/layout/AppSheetLayout.kt
/**
 * The sheet drawn where it is composed. It appears and leaves only through [state]'s show() and
 * hide(); keep it composed until hide() returns. App code uses AppSheet.
 */
@ShellSeamApi
@Composable
fun AppSheetLayout(
    state: AppSheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modality: SheetModality = SheetModality.Modal,
    dismissible: Boolean = true,
    properties: AppSheetProperties = AppSheetProperties(),
    style: AppSheetStyle = AppSheetDefaults.style(),
    paneTitle: String? = null,
    onDetentChange: ((SheetDetent) -> Unit)? = null,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
    header: (@Composable AppSheetScope.() -> Unit)? = null,
    footer: (@Composable AppSheetScope.() -> Unit)? = null,
    content: @Composable AppSheetScope.() -> Unit,
)
```

### 4.4 `AppSheetState`

File `component/sheet/AppSheetState.kt`.

```kotlin
/**
 * Where a sheet rests and how it moves, kept outside composition so a presenter can show it, hide
 * it and read its position from layout and draw.
 *
 * It wraps foundation's AnchoredDraggableState: a drag, a fling, a programmatic change and a window
 * resize go through one mutex, so any of them interrupts another and the motion continues from the
 * current offset at the current velocity.
 *
 * [offset], [presentationFraction], [expansionFraction], [visibleTop] and [visibleHeight] change on
 * every frame of a drag: read them in a layout, layer or draw block only. [currentDetent],
 * [isVisible], [isLeaving] and [isMoving] change at most once per settle and may be read in
 * composition. [targetDetent] also changes while a finger drags across a midpoint.
 */
@Stable
class AppSheetState @RememberInComposition constructor(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
) {
    // init: require(detents.isNotEmpty()); require(initialDetent in detents)

    /** The heights the sheet can rest at, in any order; at least one. */
    val detents: List<SheetDetent>

    /**
     * The detent the sheet rests on or is settling onto; null while hidden or leaving. A form or side
     * sheet has one height: there it is the detent the sheet takes back at the bottom edge (§8).
     */
    val currentDetent: SheetDetent?

    /**
     * Where the sheet is heading: while it settles, the detent it settles onto; while a finger drags
     * it, the detent a release without velocity would reach, so it changes as the finger crosses
     * midpoints. Null while hidden or leaving. For UI that should change once per settle, read
     * [currentDetent].
     */
    val targetDetent: SheetDetent?

    /** True from show() until the sheet has left after hide() or a dismissal. */
    val isVisible: Boolean

    /** True from the moment the sheet is decided to close (by the person or by hide()) until it has gone. */
    val isLeaving: Boolean

    /** True while the sheet is dragged or animates. */
    val isMoving: Boolean

    /** The sheet's top edge in px from the top of its bounds, before the stretch, lift and park; NaN before its first layout. */
    @get:FrequentlyChangingValue
    val offset: Float

    /** 0 hidden, 1 at the lowest active detent and above. The dim follows it. */
    @get:FrequentlyChangingValue
    val presentationFraction: Float

    /**
     * 0 at the highest partial detent and below, 1 at [SheetDetent.Large] or a stop as tall (R-6a);
     * 0 for a form sheet and a side sheet. For a sheet whose only stop is Large it rises with the
     * present. The card stack, the floating-to-attached change and the glass opacity follow it.
     */
    @get:FrequentlyChangingValue
    val expansionFraction: Float

    /**
     * The top of the sheet's visible surface in window px, after the rubber band, the keyboard lift,
     * parking and its owner's movement; NaN while hidden. For UI that rides the sheet, such as a
     * button kept above it: read it in `Modifier.offset { }` or another placement block.
     */
    @get:FrequentlyChangingValue
    val visibleTop: Float

    /** Window px from [visibleTop] to the window's bottom edge, 0 while hidden: the padding that keeps a map's controls clear. */
    @get:FrequentlyChangingValue
    val visibleHeight: Float

    /**
     * Shows the sheet at [detent], or its initial detent. A sheet that is leaving turns around. For
     * a sheet shown through `AppSheet(visible)`, the caller's `visible` decides: while it is false
     * this returns at once.
     */
    suspend fun show(detent: SheetDetent? = null)

    /**
     * Slides the sheet away and returns once it has gone. A second call joins the first. For a
     * sheet shown through `AppSheet(visible)` or `AppSheet(item)`, closing it this way while the
     * caller still shows it calls onDismissRequest once, so the caller's value follows.
     */
    suspend fun hide()

    /**
     * Moves to [detent], keeping the current velocity. Returns without moving while a finger is on
     * the sheet, while it leaves or is parked. On a form or side sheet it records [detent] as the
     * detent to take back at the bottom edge and does not move.
     */
    suspend fun animateTo(detent: SheetDetent)

    suspend fun snapTo(detent: SheetDetent)

    /** One detent up; false at the top, and always on a form or side sheet. */
    suspend fun expand(): Boolean

    /** One detent down, never to hidden; false at the bottom, and always on a form or side sheet. */
    suspend fun collapse(): Boolean

    /**
     * Replaces the detents. A sheet resting on one that stays active stays; otherwise it animates to
     * the nearest active one.
     */
    fun updateDetents(detents: List<SheetDetent>)

    companion object {
        /**
         * Saves whether the sheet was shown and the identity of the detent it rested on. The detents
         * come back from the caller, because a custom resolver cannot be saved.
         */
        fun Saver(detents: List<SheetDetent>): Saver<AppSheetState, String>
    }
}

/**
 * An [AppSheetState] kept across recomposition, configuration changes and process death. A sheet
 * restored this way appears at its detent without sliding in. A changed [detents] list, or a
 * [SheetDetent.Custom] whose resolver captured new values, is applied on the next layout.
 */
@Composable
fun rememberAppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState
```

Definitions (symbols as defined at the top; `y` is the anchored offset, before the stretch, park, keyboard and pose
translations):

- `presentationFraction` = `clamp((h − y) / (h − o_n), 0, 1)`.
- `expansionFraction`: with `y_P` = the offset of the highest active partial stop (or `h` when there is none) and
  `y_L` = the offset of the Large-equivalent stop (R-6a): `clamp((y_P − y) / (y_P − y_L), 0, 1)` when such a stop
  exists, else 0; 0 for Center and Start/End placement.
- `visibleTop` = the bounds' window top + `y` − stretch − lift + `parkProgress · (h − y)` + the owner pose's vertical
  translation (B-14a), computed on read from snapshot values, so a reader's placement block subscribes to them. The
  one value not already in snapshot state is the keyboard lift, which only the sheet's placement can read (B-45): the
  placement writes it into a snapshot float while the keyboard moves, so a rider lags the lift by one frame and
  nothing is written per frame otherwise. The pose's scale is not applied (a riding button follows the sheet's edge
  within the card's scale error during a back gesture, ≤ 24 dp).

Notes for WP03 (not public):

- Inside: `AnchoredDraggableState<SheetStop>`, `SheetStop` = `Hidden` | `At(detent)`. `Hidden` is in the anchor set
  while presenting, while leaving, and whenever the person may dismiss by drag; otherwise it is absent and the drag
  below the lowest detent goes to the rubber band. That is foundation's replacement for the deprecated
  `confirmValueChange` (`src/foundation-1.12.1/.../AnchoredDraggable.kt:1716-1735` [G]).
- **Mutex priorities.** Foundation runs a drag and its fling at `MutatePriority.Default`
  (`AnchoredDraggable.kt:410-428`, `:457-477`, default at `:1155` [V]), and `MutatorMutex` cancels a running mutation
  of equal or lower priority (`fnd112/MutatorMutex.kt:82` [V]). So:

  | Mutation | Priority | Consequence |
  |---|---|---|
  | drag, its fling (B-22), the nested-scroll session (B-28) | `Default` (foundation's) | a new drag stops a settle; a settle never stops a drag |
  | `animateTo`, `expand`, `collapse`, the `Expand` keyboard move, `updateDetents`' move | `Default`, after an explicit check that returns while a pointer is down on the sheet (a plain field the catch node keeps, B-17), while leaving and while parked | they retarget a settle with its velocity and never cancel a finger |
  | `show()`, `hide()` | `PreventUserInput` | code closing its sheet wins over a finger; `show()` during `hide()` interrupts it and starts from the current offset with `lastVelocity` |
  | the catch (B-17), the gesture stop of B-38a | `PreventUserInput`, an empty block | a finger or a Back gesture stops a presenting or settling sheet; skipped while leaving or parked |

- **`leaving`** is the sheet's own snapshot field behind `isLeaving`. It is set synchronously, in the frame of the
  decision, by: a release that resolves to hidden (in `SheetFlingBehavior`, before the animation starts), a dismissing
  scrim tap, Back's commit, the Back key, Escape, a closing `requestDismiss()` or `dismiss()`, `hide()`, and a changed
  or null `item`. Only `show()` clears it. B-16, B-17, B-18, B-37, B-43 and the nested-scroll connection read it.
  Leaving is never derived from foundation's `targetValue`, which during an untargeted fling is the closest anchor
  (`AnchoredDraggable.kt:868-871`, `isAnimationRunning` false at `:935-936` [V]) and would read "not leaving" for the
  first 60-100 ms of a flick-dismiss (C1-05).
- **`targetDetent`** is the sheet's own field: written by `SheetFlingBehavior` at the release, by `show()`,
  `animateTo()` and the IME `Expand` move, and during a drag by the B-22 rule at `v = 0` (written only when it
  changes). `currentDetent` is written on arrival (B-61's arrival frame) and by `snapTo`. `isMoving` comes from the
  sheet's own flags, not foundation's.
- **Springs** come from `AppTheme.springs` through the attached layout node, never from fields written in composition
  (fixes D14). Every offset animation uses `springs.sheet.pixels` (visibility threshold 0.5 px,
  `ds/theme/AppMotion.kt:72` [C]), never `.unit`, whose 0.001 threshold would run ~0.75 s of sub-pixel tail and hold
  `isMoving` and host motion (C1-24). Before the first layout every request waits for the anchors
  (`AppSheetState.kt:107-118`, `:132` [C]).
- **Saver encoding:** one string, `"<shown>|<key>"`, where `<shown>` is `1` or `0` and `<key>` is `large`, `medium`,
  `fit`, `peek`, `fraction:<f>`, `height:<dp>` or `custom:<id>`. A restored key that is no longer listed or active maps
  to the nearest active detent (B-62). `<shown>` is used only by `AppSheetLayout` presenters (destination sessions);
  for the `visible` and `item` overloads whether the sheet shows comes only from the caller (B-6).
- `rememberAppSheetState` runs a `SideEffect` on every composition that (a) calls `updateDetents` when the list
  differs, and (b) stores the latest resolver of each `Custom` id as a plain field write; when any resolver instance
  changed it increments a snapshot generation counter that `SheetLayout`'s measure reads, so the next measure
  re-resolves and anchors are written only if a value changed (R-11). The KDoc of `Custom` tells callers to `remember`
  a detent whose call site recomposes often.
- An internal `presentSpring` (default `springs.sheet`) lets the presets arrive on `springs.dialog` (M17) without a
  public parameter.
- **Previews and tests** (API-13, C2-41): under `LocalInspectionMode` a visible sheet is placed at its initial detent
  on the first frame, with no animation, so layoutlib previews and `com.android.compose.screenshot` tests render it.
  An `internal` `@VisibleForTesting fun snapToFraction(expansion: Float)` poses a sheet at any `e` for §15.5's
  card-stack screenshots.

### 4.5 Detents

File `component/sheet/SheetDetent.kt`.

```kotlin
/**
 * A height a sheet can rest at. A sheet lists one or more; the person moves between them by dragging,
 * flicking, tapping the grabber, or with the grabber's accessibility actions.
 *
 * Heights are measured within the safe area: above the navigation bar, or above the tab bar for a
 * non-modal sheet that rests on it. They resolve in the sheet's measure pass whenever the window,
 * the fold, the font scale or the content's height changes, never during a drag and never from the
 * keyboard. A detent may resolve to nothing: an inactive detent is skipped, as iOS skips medium in
 * compact height.
 *
 * More detents will be added: a `when` over a SheetDetent must carry an `else` branch.
 */
@Immutable
sealed interface SheetDetent {
    /** Nearly the whole window; on a phone the screen behind recedes into a card. Always active. */
    data object Large : SheetDetent

    /** About half the window: the sheet's top at the middle. Inactive in Compact height. */
    data object Medium : SheetDetent

    /**
     * The sheet's own height: grabber band, header, content and footer as measured without the
     * keyboard, capped at Large's height. Content that fills the height it is given (a lazy list) is
     * as tall as Large and behaves as Large (R-6a). When the content's height changes, a sheet
     * resting here animates to the new height.
     */
    data object FitContent : SheetDetent

    /**
     * The grabber band and the header as measured: a peek that shows the header and nothing more,
     * and grows with the font scale. Without a header, the minimum height (48 dp).
     */
    data object Peek : SheetDetent

    /** [fraction] of the height Large shows, in (0, 1]. */
    data class Fraction(val fraction: Float) : SheetDetent {
        init { require(fraction > 0f && fraction <= 1f) { "A fraction is in (0, 1]: $fraction" } }
    }

    /** A fixed height. Font scale does not change it: a peek with text in it is [Peek]. */
    data class Height(val height: Dp) : SheetDetent

    /**
     * A height from [resolve], or null to leave the detent out in that context, as iOS's custom
     * resolvers do. Two Custom detents with the same [id] are the same detent; the latest [resolve]
     * passed with that id is the one used, so it may capture values from composition. Remember the
     * detent if its call site recomposes often. [resolve] runs in measure and must read no snapshot
     * state.
     */
    class Custom(val id: String, val resolve: SheetDetentContext.() -> Dp?) : SheetDetent {
        override fun equals(other: Any?): Boolean = other is Custom && other.id == id
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "Custom($id)"
    }
}

/** What a [SheetDetent.Custom] resolver can read. Every value is settled; none changes during a drag. */
interface SheetDetentContext : Density {
    /** Large's height: the most any detent shows. iOS's maximumDetentValue. */
    val maxHeight: Dp
    /** The height of the sheet's bounds: the window, the pane of a non-modal sheet, or a hinge half (§8). */
    val containerHeight: Dp
    /** Grabber band, header, content and footer as measured without the keyboard. Always measured. */
    val contentHeight: Dp
    /** Grabber band and header as measured: what [SheetDetent.Peek] resolves to. */
    val headerHeight: Dp
    /** The placement class: the window's for a modal sheet; the pane's for a non-modal one. */
    val sizeClass: SizeClass
    /** The bottom of a horizontal hinge, from the top of the bounds, or null. */
    val foldBottom: Dp?
    /** Another built-in detent's height in this context, or null when it is inactive. Null for Custom. */
    fun heightOf(detent: SheetDetent): Dp?
}
```

Resolution rules (WP03, `layout/DetentResolution.kt`, pure Kotlin, unit-tested). Heights are "within the safe area",
measured up from `restBottom`; the offset is `restBottom − height` except for Large and Medium, which are absolute.

| # | Rule |
|---|---|
| R-1 | `Large`: offset `largeTop`; height `maxHeight = restBottom − largeTop`. For a stacked sheet see B-54. |
| R-2 | `Medium`: offset `0.5 · H` (`sheet.detent.mediumFraction`). **Inactive** in Compact height [A], and when it lies within 48 dp (`sheet.detent.mediumMinGap`) of Large. With a horizontal hinge its top rests at `foldBottom` (kept from `ds/component/overlay/AppSheet.kt:138-139`, `:235` [C]). |
| R-3 | `FitContent`: height = `contentHeight`, the column measured **without the keyboard**: grabber band or header, content measured with `maxHeight` as its limit, footer. The measure at the IME target (B-47) sizes the content box for placement only and never updates `contentHeight` or the anchors (C1-10). |
| R-3a | `Peek`: height = `headerHeight`, the grabber band plus the measured header (also keyboard-free); with neither, R-5's minimum. |
| R-4 | `Fraction(f)`: height `f · maxHeight`. `Height(d)`: height `d`. `Custom`: the latest resolver's value for its id. |
| R-5 | Every height is clamped to `[sheet.detent.minHeight (48 dp), maxHeight]`. |
| R-6 | Detents that resolve within 1 px of each other merge into one stop. Its identity is `Large` when Large is among them; otherwise the first in list order. |
| R-6a | A stop within 1 px of Large's offset is **Large-equivalent** whatever detent produced it: it drives `expansionFraction`, attaching (no floating), the card stack and the glass ramp exactly as Large does. So `[FitContent]`, `[FitContent, Large]` and `[Large, FitContent]` with a 60-row list look and behave the same (C2-09). |
| R-7 | Stops are ordered by offset; list order matters only for R-6 ties and `initialDetent`. |
| R-8 | If every detent is inactive, the sheet uses `Large`. |
| R-9 | An inactive initial detent opens at the nearest active one by height. |
| R-10 | Center placement, and a modal Start/End panel, have **one stop**: Center's fitted height `min(0.85 · H, 620 dp, contentHeight)`, the side panel's full safe height (§8). The detents stay listed and are recorded, not resolved: `currentDetent` is the recorded detent (initially the initial detent's, else the tallest active listed one), `animateTo(d)` records `d` without moving, `expand()`/`collapse()` return false, `onDetentChange` never fires. A non-modal Start/End panel keeps its detents, resolved against the panel's height. |
| R-11 | When the stops change (rotation, unfold, resize, new detents, a refreshed resolver, content height), the sheet keeps its detent by identity and an animation in flight is retargeted: `updateAnchors(new, newTarget = target)` (`AnchoredDraggable.kt:1001-1029` [G]). Anchors are written in measure only when they changed. On a placement change into Bottom from Center or Start/End, the sheet lands on the recorded detent if active, else the tallest active one. |
| R-12 | With a horizontal hinge (tabletop), every partial stop's top is clamped to `foldBottom` or lower, so no partial sheet straddles the crease; a Large-equivalent stop is allowed (§8). |
| R-13 | `SheetModality.NonModal(upThrough)`: `upThrough` must be listed (`require`). When it is inactive (Medium in Compact height), the boundary is the highest active detent whose height is at most `upThrough`'s nominal height, else the lowest active detent (C2-29). |

### 4.6 Behaviour types

Files `component/sheet/SheetModality.kt`, `AppSheetProperties.kt`.

```kotlin
/** Whether what is behind the sheet stays usable. More may be added: a `when` needs `else`. */
@Immutable
sealed interface SheetModality {
    /**
     * Dims and blocks what is behind at every detent, hides it from screen readers and takes focus;
     * drawn over the tab bar.
     */
    data object Modal : SheetModality

    /**
     * What is behind stays usable, undimmed and visible to screen readers while the sheet rests at
     * [upThrough] or lower (every detent when null), as iOS's
     * presentationBackgroundInteraction(.enabled(upThrough:)). Above it the dim fades in over the
     * travel to the next detent up, and the sheet blocks like a modal one. [upThrough] must be one of
     * the sheet's detents. With [aboveBottomChrome] the sheet rests above the floating tab bar at its
     * undimmed detents.
     */
    @Immutable
    class NonModal(
        val upThrough: SheetDetent? = null,
        val aboveBottomChrome: Boolean = true,
    ) : SheetModality {
        fun copy(upThrough: SheetDetent? = this.upThrough, aboveBottomChrome: Boolean = this.aboveBottomChrome): NonModal
        // equals, hashCode and toString over both fields
    }
}

/**
 * How the person tried to close a sheet. TalkBack's dismiss action reports Request; a tap outside an
 * anchored action card reports Scrim. More may be added: a `when` needs `else`.
 */
enum class SheetDismissSource { Drag, Scrim, Back, Escape, Request }

/** Behaviour a sheet seldom changes. */
@Immutable
class AppSheetProperties(
    val placement: SheetPlacement = SheetPlacement.Automatic,
    val contentInteraction: SheetContentInteraction = SheetContentInteraction.Automatic,
    val transition: SheetTransition = SheetTransition.Slide,
    val keyboard: SheetKeyboard = SheetKeyboard.Lift,
    /** False makes a tap on the dim do nothing; drag and Back still close the sheet. */
    val scrimTapDismisses: Boolean = true,
    /**
     * False holds the sheet on its detent: no drag and no resizing from the content's scroll, for a
     * signature pad, a canvas, a pannable chart or a payment in progress. Back, the dim and the close
     * button still follow `dismissible`; the grabber's accessibility actions stay.
     */
    val dragEnabled: Boolean = true,
    /**
     * False keeps the screen behind upright at Large: the dim only. For a video call, a camera
     * preview or a SurfaceView map behind the sheet.
     */
    val recedesPresenter: Boolean = true,
    /**
     * True starts the content at the surface's top edge, under the grabber, for artwork or a map
     * that fills the top corners; the grabber draws over it with a contrast pill.
     */
    val contentUnderGrabber: Boolean = false,
    /**
     * True keeps the content composed, parked, between openings, so opening it again composes
     * nothing. For a sheet opened often from one screen; it holds its memory meanwhile.
     */
    val keepContent: Boolean = false,
) {
    fun copy(/* every property, defaulting to this one's */): AppSheetProperties
    // equals, hashCode and toString over every property
}

/** Where the sheet sits. Automatic follows the window (§8), as iOS 27's presentationPlacement(.automatic). */
@JvmInline
value class SheetPlacement private constructor(private val value: Int) {
    companion object {
        val Automatic = SheetPlacement(0)
        val Bottom = SheetPlacement(1)
        val Center = SheetPlacement(2)
        /** The start edge in the layout direction (iOS 27 `.leading`). */
        val Start = SheetPlacement(3)
        val End = SheetPlacement(4)
    }
    // toString names the constant
}

/** What a vertical scroll inside the sheet does first. */
@JvmInline
value class SheetContentInteraction private constructor(private val value: Int) {
    companion object {
        /**
         * Resizes for a modal sheet dragged by touch or stylus; Scrolls for a non-modal sheet, for a
         * wheel, a touchpad or a mouse, and while TalkBack's touch exploration is on (B-27).
         */
        val Automatic = SheetContentInteraction(0)
        /** Grows the sheet to its top detent, then scrolls: iOS's default. */
        val Resizes = SheetContentInteraction(1)
        /** Scrolls at every detent; the sheet resizes from its grabber, header, footer and a pull at the content's top. */
        val Scrolls = SheetContentInteraction(2)
    }
}

/** How the sheet arrives and leaves. Phase 2 adds Zoom (§20). */
@JvmInline
value class SheetTransition private constructor(private val value: Int) {
    companion object {
        val Slide = SheetTransition(0)
        /** A 200 ms fade in and 120 ms fade out at the sheet's detent. */
        val CrossFade = SheetTransition(1)
    }
}

/** What the sheet does when the keyboard rises for a field inside it. */
@JvmInline
value class SheetKeyboard private constructor(private val value: Int) {
    companion object {
        /** Rises with the keyboard, keeping its height, up to Large. */
        val Lift = SheetKeyboard(0)
        /** Goes to its tallest detent while the keyboard is up, and back when it hides. */
        val Expand = SheetKeyboard(1)
    }
}
```

- `SheetPlacement.Start`/`End` are iOS 27's `.leading`/`.trailing`, named for Compose's RTL-aware convention.
- `AppSheet` checks `require(modality !is NonModal || modality.upThrough == null || modality.upThrough in
  state.detents)` (R-13).
- Value classes, not enums or sealed types, because callers only pass these in: adding `SheetTransition.Zoom` or a
  placement later breaks no caller (D-37). Each is a non-null `Int` inside, so nothing boxes where it is stored as
  its own type.

### 4.7 Style

Files `theme/AppDesignStyle.kt` (the enum, so the theme never imports a component), `component/sheet/AppSheetStyle.kt`,
`AppSheetDefaults.kt`.

```kotlin
/**
 * What a sheet's surface is made of. The design style's `sheetMaterial` token is one of the last
 * three. More may be added: a `when` needs `else`.
 */
enum class SheetMaterial {
    /** The design style's choice; Solid unless the Glass design style. */
    Automatic,
    /** The opaque container. The cheapest, and what every material becomes under Reduce transparency. */
    Solid,
    /** Drawn translucency (container at 92-94 %, a rim, a sheen). Samples nothing: a few cached brush draws. */
    Frosted,
    /**
     * Live liquid glass at partial detents on the Live tiers, over the shell only; Frosted on the
     * lower tiers, over a cover or another sheet, and without the liquid glass feature. It turns
     * opaque by halfway to Large, so a sheet whose only detent is Large is opaque at rest.
     */
    Glass,
}

/** A sheet's colours. */
@Immutable
class AppSheetColors(
    /** The body. Frosted and Glass draw it translucent at partial detents (sheet.frosted, sheet.glass tokens). */
    val container: Color,
    val content: Color,
    /** The grabber pill; AppSheetGrabber reads it from the enclosing sheet. */
    val grabber: Color,
    val rim: Color,
    /** The dim at full strength, alpha included. */
    val scrim: Color,
) {
    fun copy(
        container: Color = this.container,
        content: Color = this.content,
        grabber: Color = this.grabber,
        rim: Color = this.rim,
        scrim: Color = this.scrim,
    ): AppSheetColors
    // equals and hashCode over every field
}

/**
 * How a sheet looks. Build it with [AppSheetDefaults.style]. Each field maps to a property of
 * Compose's Style, so moving the sheet to Modifier.styleable is mechanical (§4.7 mapping).
 */
@Immutable
class AppSheetStyle internal constructor(
    val material: SheetMaterial,
    val colors: AppSheetColors,
    /** Painted over the container where the body is opaque: a gradient or a brand texture; null for none. */
    val containerBrush: Brush?,
    /** All four corners. */
    val cornerRadius: Dp,
    /** The gap to the side and bottom edges at partial detents; 0 keeps the sheet attached. */
    val floatingInset: Dp,
    /** Dp.Unspecified: the placement's own width (§7.2). */
    val maxWidth: Dp,
    /** Drawn outside the shape, only where §7.7 allows; null for none. */
    val shadow: androidx.compose.ui.graphics.shadow.Shadow?,
) {
    fun copy(
        material: SheetMaterial = this.material,
        colors: AppSheetColors = this.colors,
        containerBrush: Brush? = this.containerBrush,
        cornerRadius: Dp = this.cornerRadius,
        floatingInset: Dp = this.floatingInset,
        maxWidth: Dp = this.maxWidth,
        shadow: Shadow? = this.shadow,
    ): AppSheetStyle
    // equals and hashCode over every field
}

object AppSheetDefaults {
    val Detents: List<SheetDetent> = listOf(SheetDetent.Large)

    val Grabber: @Composable AppSheetScope.() -> Unit = { AppSheetGrabber() }

    @Composable
    fun colors(
        container: Color = AppTheme.colors.surfaceElevated,
        content: Color = AppTheme.colors.contentPrimary,
        grabber: Color = AppTheme.colors.contentTertiary.copy(alpha = DesignTokens.Sheet.Grabber.alpha),
        rim: Color = solidRim(),
        scrim: Color = AppTheme.colors.scrim,
    ): AppSheetColors

    /** For inset-grouped content (AppSection): the grouped background, as iOS's grouped sheets. */
    @Composable
    fun groupedColors(): AppSheetColors

    /** White at sheet.rim.solidDarkAlpha (0.08) in dark theme; transparent in light, where the shadow or dim separates. */
    @Composable
    fun solidRim(): Color

    /** Soft in light theme; null in dark, where the rim shows the edge. */
    @Composable
    fun shadow(): Shadow?

    @Composable
    fun style(
        material: SheetMaterial = SheetMaterial.Automatic,
        colors: AppSheetColors = colors(),
        containerBrush: Brush? = null,
        cornerRadius: Dp = AppTheme.style.sheetRadius,
        floatingInset: Dp = AppTheme.style.sheetInset,
        maxWidth: Dp = Dp.Unspecified,
        shadow: Shadow? = shadow(),
    ): AppSheetStyle
}
```

- `Shadow` is `androidx.compose.ui.graphics.shadow.Shadow` (radius, spread, offset, colour, alpha, brush, blend mode),
  never the text shadow `androidx.compose.ui.graphics.Shadow`; both exist in ui-graphics 1.12.1 [V]. The nine-slice
  (§7.7) supports radius, spread, offset, colour and alpha; a brush or a blend mode other than `SrcOver` fails a
  `require` in `AppSheetDefaults.style`.
- `AppSheetDefaults` holds only the defaults of parameters (API-6). The geometry values (grabber size and top, content
  and header tops, header minimum, form fraction and cap, side inset, grabber alpha, Medium fraction, Large top gap)
  live in the generated, internal `DesignTokens.Sheet` (§6.2), so `upgrade_project.py` never carries them as API
  (API-14).
- `maxWidth = Dp.Unspecified` means the placement's own width, resolved inside the sheet from the placement class
  (§7.2). A caller value is honoured as a cap. Nothing here reads `AppTheme.layout`, which in the caller's
  composition is the caller's pane's (C2-02).
- `AppStyle` (`ds/theme/AppDesignStyle.kt:154` [C]) gains `sheetRadius: Dp` (from `radii.sheet`, which exists:
  `tokens/styles.json:5`, `:22`, `:39`, `:56` [C], 24 / 30 / 8 / 28 dp), `sheetInset: Dp` and
  `sheetMaterial: SheetMaterial` from new `tokens/styles.json` fields (§6.2). `SheetMaterial.Automatic` resolves from
  `AppTheme.style.sheetMaterial` inside the sheet.
- Every value above is generated from the `sheet` token section (§6.2); none is private to a component file (API-6).
- `SheetMaterial` sits in `theme/` beside `BarTreatment` (`ds/theme/AppDesignStyle.kt:102` [C]), which is the same kind
  of design-style choice, so the theme never imports a component package. The sheet's uniform radius replaces
  `shapes.sheet`'s top-only rounding (`ds/theme/AppMetrics.kt:47` [C]) for sheets.

**Mapping to the Styles API** (`androidx.compose.foundation.style`, experimental in foundation 1.12.1 [V]
`fnd112/.../style/ExperimentalFoundationStyleApi.kt:23`). Not adopted now. When it is stable the sheet takes
`style: Style` and applies `Modifier.styleable(...)`:

| `AppSheetStyle` | `StyleScope` | Stays in the node |
|---|---|---|
| `colors.container`, `containerBrush` | `background(color)`, `background(brush)` | Frosted and Glass bodies |
| `colors.content` | `contentColor(color)` | — |
| `cornerRadius` | `shape(RoundedCornerShape(r))` | the floating clip (it follows `e`) |
| `shadow` | `dropShadow(shadow)` | the nine-slice and its outside-only cut (§7.7) |
| `maxWidth` | `maxWidth(dp)` | the placement's width when unspecified |
| `floatingInset` | `externalPadding(…)` at rest | the morph |
| `material`, `colors.scrim`, `colors.grabber`, `colors.rim` | none | other nodes |

### 4.8 Scope, slots and content-side API

Files `component/sheet/AppSheetScope.kt`, `chrome/`, `content/`.

```kotlin
/** What a sheet's slots and content can reach. */
@Stable
interface AppSheetScope {
    val sheetState: AppSheetState

    /**
     * Padding for the content's scroller. Start and end: the sheet's gutter (its placement class's
     * AppLayout.gutter), so text never meets the rounded clip while the sheet floats; full-bleed rows
     * ignore it and pad their own text. Bottom: the footer, the navigation bar or the keyboard, and
     * with Scrolls the part of the content below the visible bottom at the settled detent, each
     * counted once. Top: 0. Backed by snapshot state and read in measure: a change re-measures the
     * scroller and never recomposes it. It changes only when the sheet settles, the footer's height
     * changes, the keyboard's target changes or the placement class changes; never during a drag.
     */
    val contentPadding: PaddingValues

    /** How far the content has scrolled under the header, 0 to 1 over sheet.header.scrollEdgeRamp. Read in draw. */
    @get:FrequentlyChangingValue
    val contentScrolled: Float

    /** True while another sheet is stacked on this one. */
    val isCovered: Boolean

    /** Closes the sheet as the scrim would: a SheetDismissGuard or dismissible = false turns it into an attempt. */
    fun requestDismiss()

    /** Closes the sheet without asking: a Done button after the work is saved. Calls onDismissRequest. */
    fun dismiss()
}

/**
 * Draws the grabber pill in the enclosing sheet's grabber colour, as a contrast pill when the
 * content runs under it; outside a sheet, in contentTertiary, in place. Its tap, keys, hover, focus,
 * 48 dp target and accessibility actions belong to the sheet's grabber node, so a custom grabber
 * lambda that draws something else keeps all of them.
 */
@Composable
fun AppSheetGrabber(modifier: Modifier = Modifier)

/**
 * A sheet's title row: [title] centred between [leading] and [trailing] (Cancel, Done, close), never
 * under them: the wider side's width is reserved on both sides. At font scale 1.5 and above the title
 * takes up to two lines and the row grows. The title is a heading for screen readers. Outside a
 * sheet it draws in place, without the scroll-edge fade.
 */
@Composable
fun AppSheetHeader(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: (@Composable () -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = { AppSheetCloseButton() },
)

/** [title] and [subtitle] as text; [title] also names the sheet for screen readers (§9). */
@Composable
fun AppSheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = { AppSheetCloseButton() },
)

/**
 * Asks the enclosing sheet to close (guarded). Emits nothing while the person cannot close the sheet
 * at all (`dismissible = false` and no enabled guard), and outside a sheet, so a screen shown both as
 * a sheet and full screen can keep it.
 */
@Composable
fun AppSheetCloseButton(modifier: Modifier = Modifier)

/** The enclosing sheet's contentPadding; zero outside a sheet. */
@Composable
fun sheetContentPadding(): PaddingValues

/**
 * A scrolling column with the sheet's contentPadding applied inside the scroll: a form, a settings
 * group. When the sheet's footer cannot stay pinned (large text in a short window), it is composed
 * here, after [content].
 */
@Composable
fun SheetColumn(
    modifier: Modifier = Modifier,
    state: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = sheetContentPadding(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
)

/** In a lazy list, the sheet's footer as the last item when it cannot stay pinned; nothing otherwise. */
fun LazyListScope.sheetFooterItem()

/**
 * Puts [content] in the enclosing sheet's header slot, replacing `header`. [title] names the sheet
 * for screen readers. Outside a sheet it draws in place.
 */
@Composable
fun ProvideSheetHeader(title: String? = null, content: @Composable AppSheetScope.() -> Unit)

/** Puts [content] in the enclosing sheet's footer slot, pinned to its visible bottom. In place outside a sheet. */
@Composable
fun ProvideSheetFooter(content: @Composable AppSheetScope.() -> Unit)

/**
 * Sets the enclosing sheet's look and grabber from its content: for a destination sheet, whose
 * AppSheet call the screen never sees. Lands before the first visible frame. Nothing outside a sheet.
 */
@Composable
fun ProvideSheetStyle(
    style: AppSheetStyle,
    grabber: (@Composable AppSheetScope.() -> Unit)? = AppSheetDefaults.Grabber,
)

/**
 * Takes Back (the key, a committed gesture, Escape) inside the enclosing sheet while [enabled],
 * instead of closing it: a step back in a flow the sheet holds. The sheet does not shrink for it.
 */
@Composable
fun SheetBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * Keeps the enclosing sheet up while [enabled]: a drag down rubber-bands and springs back; the dim,
 * Back, Escape and the close button call [onAttempt] instead, once per attempt. Enable it only while
 * there is something to lose, so an untouched form keeps the system's predictive back. Closing from
 * code is not guarded. Outside a sheet it does nothing.
 */
@Composable
fun SheetDismissGuard(enabled: Boolean = true, onAttempt: (SheetDismissSource) -> Unit)

/**
 * Asks before the enclosing sheet closes while [enabled]: a drag, the dim, Back, Escape or the close
 * button opens a stacked action sheet with [title], a destructive [discardLabel] that calls
 * [onDiscard], and Keep Editing. Built from SheetDismissGuard and AppActionSheet; build those
 * yourself for a different question.
 */
@Composable
fun SheetDismissConfirmation(
    enabled: Boolean,
    onDiscard: () -> Unit,
    title: String = stringResource(R.string.designsystem_sheet_discard_title),
    discardLabel: String = stringResource(R.string.designsystem_sheet_discard),
)

/**
 * A region of the content where a drag never moves the sheet: a canvas, a signature, a map, a
 * chart. Its own pointer input and scrolling are untouched.
 */
fun Modifier.sheetDragExclusion(): Modifier

/**
 * The sheet a composable is drawn in, or null outside one. AppScaffold lets the sheet's material show
 * instead of painting its own background; AppTopBar pads no status bar and shows a close button where
 * it would show Back at the root; AppBottomActionBar paints no background and pads no inset.
 */
val LocalSheetSurface: CompositionLocal<SheetSurface?>   // provided internally by the sheet

/** One per sheet, remembered: the declared material and container, never what animates. */
@Stable
class SheetSurface internal constructor(val material: SheetMaterial, val container: Color, val scope: AppSheetScope)
```

**Which to call** (C2-10):

| The person or code | Call | Guard asked | `onDismissRequest` |
|---|---|---|---|
| taps Done after the work is saved | `scope.dismiss()` | no | once |
| taps Cancel or a close button | `scope.requestDismiss()` (what `AppSheetCloseButton` does) | yes | once, if it closes |
| code decides (a result arrived, the item was deleted) | `visible = false`, `item = null`, a pop | no | never |
| code holding only the state, under the `visible` or `item` overload | `state.hide()` | no | once, so the caller's value follows (B-4) |

Slot rules:

- **Grabber.** The sheet owns a grabber node: hit-testing, focus, click (B-19), hover, `stateDescription`, the
  accessibility actions and visibility (§7.3, §9). The `grabber` slot (or `ProvideSheetStyle`'s) only draws inside it,
  and the node is drawn with no room taken from the layout. The node is shown when the sheet has two or more active
  stops, and hidden for Center and modal Start/End placement, while covered (iOS `prefersGrabberVisible` [A]) and when
  one stop is active. `grabber = null` removes the drawing and the hit band; the semantics move to the surface.
- **Header.** Measured in the keyboard-free pass, placed above the content box with its own layer, never scrolled,
  laid out inside a constant `i₀` side inset (D-7) so its actions never meet a rounded corner while the sheet floats.
- **Footer.** Measured separately and placed with its own layer at the visible bottom (B-45); drawn over the content's
  bottom. The footer slot consumes `navigationBars ∪ ime`, so an `AppBottomActionBar` in it pads nothing twice (fixes
  D2, `AppBottomActionBar.kt:38-39` [C]). **Not pinned** while the measured header and footer together exceed half the
  visible height (large text in a short window, C2-35): the sheet stops drawing it and `SheetColumn` or
  `sheetFooterItem()` composes it at the end of the scroll, so content always keeps at least half the visible height.
- **Content** (B-1a). An `AppSheetScope` receiver, laid out in a `Box` measured at the sheet's full width with Large's
  content height as its maximum and no minimum, children stacked at the top start, as `Scaffold`'s body is. Bring one
  root (a lazy list or `SheetColumn`); providers, guards and confirmations emit no layout and may sit beside it. A
  code rule flags a second layout emitter at the root of an `AppSheet` content lambda (§19.1 item 9).
- **Content-side providers** (`ProvideSheetHeader`, `ProvideSheetFooter`, `ProvideSheetStyle`, `SheetBackHandler`,
  `SheetDismissGuard`) register through an internal `LocalSheetSlots` in a `DisposableEffect`. A provider of
  composable content captures `currentCompositionLocalContext` and a call-site `rememberSaveableStateHolder()`, and
  the slot host composes it as `CompositionLocalProvider(captured) { holder.SaveableStateProvider(callSiteKey) {
  content(scope) } }`, the pattern of `ds/component/overlay/AppOverlayHost.kt:89-92` [C]. So a destination's header
  or footer gets its screen's `ViewModelStoreOwner`, lifecycle, saved-state registry, nested theme and
  `LocalPaneContext`, never the presentation host's (C1-15). **Registration lands before the first visible frame**
  (B-2).
- **Precedence.** The registration of an uncovered entry wins over a covered one (`AppSheetNavHost` gives each step
  its own registry layer, which forwards to the sheet only while the step is uncovered, `LocalEntryVisibility`);
  among the rest, the innermost wins, and among siblings the last registered. During a step transition the outgoing
  and incoming headers and footers are both drawn, cross-fading with the transition's progress (`ModulateAlpha` on
  their layers, read in the layer block); a covered step's guards and Back handlers are ignored, and the pane title
  follows the uncovered step (B-59).
- **`LocalSheetSurface`** is a `CompositionLocal`, provided only by the sheet. Its `SheetSurface` is remembered once per
  sheet and changes only when the caller passes a different material or container, never on a settle, a tier change,
  a fade or `e` passing `opaqueAt`; anything that animates is read through `scope` in draw (C1-30).

### 4.9 Presets

Files `component/sheet/action/`, `choice/`.

```kotlin
/**
 * "What would you like to do with this?": actions in a sheet that fits them, with Cancel apart. At
 * Medium width and up it is a card anchored to [anchor], without Cancel; with no anchor it is centred.
 */
@Composable
fun AppActionSheet(
    visible: Boolean,
    actions: List<SheetAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelLabel: String = stringResource(R.string.designsystem_cancel),
    anchor: SheetAnchor? = null,
)

@Immutable
class SheetAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    /** Draws a checkmark and announces the row as selected (QA-14). */
    val selected: Boolean = false,
)

/** The bounds an anchored card points at, recorded in placement without snapshot writes. */
@Stable
class SheetAnchor

@Composable
fun rememberSheetAnchor(): SheetAnchor

/** Records the bounds and takes focus back when the anchored card closes (B-42a). */
fun Modifier.sheetAnchor(anchor: SheetAnchor): Modifier

/**
 * One choice from a list, the current one checked: a language, a unit, a plan. Rows have the radio
 * role and may carry [supportingText] and be disabled. A search field appears from
 * AppChoiceSheetDefaults.SearchFrom choices; it matches the label and the supporting text, ignoring
 * case and accents. [onSelectNone] adds a row labelled [noneLabel] that selects nothing. Choosing
 * calls onSelect, then onDismissRequest. [key] must return a value a Bundle can hold; by default rows
 * are keyed by position.
 */
@Composable
fun <T : Any> AppChoiceSheet(
    visible: Boolean,
    choices: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    title: String? = null,
    supportingText: ((T) -> String?)? = null,
    enabled: (T) -> Boolean = { true },
    onSelectNone: (() -> Unit)? = null,
    noneLabel: String = stringResource(R.string.designsystem_none),
    key: ((T) -> Any)? = null,
)

object AppChoiceSheetDefaults {
    const val SearchFrom: Int = 6           // sheet.choice.searchFrom (VitFleet ChoiceSheet)
    const val MaxFitFraction: Float = 0.6f  // sheet.choice.maxFitFraction
}
```

- **`AppActionSheet`, Compact width:** an in-screen `AppSheet` with one stop
  `Custom("action-fit") { minOf(contentHeight, maxHeight * 0.6f) }` (`sheet.actionSheet.maxFitFraction`), `Scrolls`,
  two `surfaceElevated` groups (actions, then Cancel), rows at least 56 dp tall (`heightIn(min =
  sheet.actionSheet.rowHeight)`, so a label wrapped at font scale 2.0 grows its row), arriving on `springs.dialog`
  (M17). It scrolls when taller than the cap (fixes A1), keeps consuming Back during its exit (A3), sets its pane
  title (A5). An action runs before the sheet asks to close (`AppActionSheet.kt:138-141` [C], kept). As a modal sheet
  it stacks on a visible persistent sheet (B-54a).
- **`AppActionSheet`, Medium width and up (D-21):** a card in the overlay host, `sheet.actionSheet.cardWidth` (320 dp)
  wide, placed below the anchor's bounds (above when there is no room), aligned to its start edge, clamped 8 dp inside
  the window. It scales 0.94 → 1 from the anchor's nearest edge with a `ModulateAlpha` fade on `springs.dialog`
  (card-size layer). No dim; pointer input outside the card is consumed while it is up, and a tap there closes it
  (reported as `Scrim`); Esc and Back close it. Without an anchor it is a centred card over the dim. Shadow per §7.7.
  Never a `Popup` window. It is **modal for accessibility** (C2-14): what is beneath gets `hideFromAccessibility()`,
  the card carries `paneTitle = title ?: designsystem_actions_pane` and a built-in `dismiss(label =
  designsystem_sheet_dismiss)` action, focus moves to its first enabled action on present (B-42a) and returns to the
  `SheetAnchor` on close.
- **`AppChoiceSheet`:** detents `[Custom("choice-fit") { minOf(contentHeight, maxHeight * 0.6f) }, Large]`, so a long
  list opens at 60 % and grows (VitFleet `ChoiceSheet.kt:34-114`, `current-state.md` §11). Rows are keyed by
  `key(choice)` when given, else by position: a lazy item key must be Bundle-saveable
  (`SaveableStateHolder.kt:74-77` requires it, runtime-saveable 1.12.1 [V]), so the revision-1 default `{ it }`
  crashed for a plain data class (C2-36). Search folds both sides with `Normalizer.normalize(s, NFD)`, strips
  combining marks and compares with `lowercase(locale)`. Multiple selection is a recipe built from `AppSheet` (WP12),
  not a flag.
- A persistent (Maps-style) sheet is not a separate component: it is `AppSheet` with `NonModal(upThrough)`,
  `dismissible = false`, and `Automatic` content interaction (§4.1). A mini player is not a sheet: it is the tab
  bar's accessory (`ds4/foundation/layout/Chrome.kt:28-31` [C]); WP12's docs state the rule (C2-52).
- Call sites `remember` their action list (A5: a list rebuilt on every recomposition defeats skipping).
- `stringResource(R.string…)` defaults are Android's; the Multiplatform template uses `Res.string…` (§17).

### 4.10 Seams (public, opt-in, not for features)

Every declaration here carries `@ShellSeamApi`. `:core:navigation` and the glass module opt in; a code rule forbids
the opt-in under `feature/**` (C2-44).

```kotlin
// foundation/layout/ShellSeamApi.kt
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "A shell seam: features use AppSheet")
@Retention(AnnotationRetention.BINARY)
annotation class ShellSeamApi

// component/sheet/stack/SheetStack.kt (WP03 writes, WP04 applies)
/**
 * The sheets up in a window, for whatever recedes behind them. The outermost AppTheme installs one;
 * every sheet registers itself with its level (§3.2). Level 0 is the shell. A fixed-capacity array,
 * walked with index loops: no iterator, filter or allocation per frame.
 */
@ShellSeamApi
@Stable
class SheetStack internal constructor() {
    /** How far the presenter at [level] has receded: 0 upright, 1 behind a sheet at Large. Read in a layer block. */
    @FrequentlyChangingValue
    fun recede(level: Int): Float

    /** The merged dim to draw over the presenter at [level], alpha included (§6.4). Read in draw. */
    @FrequentlyChangingValue
    fun dim(level: Int): Color

    /**
     * The presenter at [level]'s visible bottom in its own px: its full height, or, while an opaque
     * sheet above it is at Large, that sheet's top plus R (§6.4). Read in draw.
     */
    @FrequentlyChangingValue
    fun visibleBottom(level: Int): Float

    /** True while a sheet at [level] may sample the live scene: its presenter is the shell and no cover is up. */
    fun samplesScene(level: Int): Boolean

    /**
     * True while a modal sheet is up above [level], so the default-priority Back handlers of the hosts
     * at that level stay off (B-44). Changes at most twice per opening.
     */
    fun blocksBack(level: Int): Boolean

    /** True while the shell has receded past sheet.cardStack.statusBarFlip. Changes once per crossing. */
    val rootReceded: Boolean

    /** The container of the sheet covering the window's bottom edge, or null. Changes once per crossing. */
    val bottomEdgeContainer: Color?

    /** The front sheet's settled bottom chrome, for toasts. */
    val topBottomChrome: Dp
}

@ShellSeamApi
val LocalSheetStack: ProvidableCompositionLocal<SheetStack?> = staticCompositionLocalOf { null }

/**
 * Recedes this layout behind the sheet above [level], as iOS's card stack does, and draws the dim
 * that reaches [level] over it. Three nodes in this order: a recede layer (scale and translation,
 * no shape, no clip), a draw node (the round-rect and visible-band clip, then the content layer, then
 * the dim rect), and a content layer with no properties. A frame of sheet motion re-records only the
 * draw node's two operations; the presenter never recomposes and its content is never re-recorded.
 */
@ShellSeamApi
fun Modifier.cardStackPresenter(stack: SheetStack, level: Int = 0): Modifier

// foundation/layout/ (WP03 declares with no-op defaults; WP04 provides)
/**
 * Where a component that moves its host's content says so. The host counts it as overlay motion: its
 * Back gate counts presses meanwhile, and its NavGuard makes a navigation wait for it, never drops it.
 */
@ShellSeamApi
fun interface HostMotion { fun report(mover: Any, moving: Boolean) }

@ShellSeamApi
val LocalHostMotion: ProvidableCompositionLocal<HostMotion> = staticCompositionLocalOf { HostMotion { _, _ -> } }

/**
 * How the navigation entry this content is in is shown. Every getter reads snapshot state: read it
 * in placement, a layer block, draw or a snapshotFlow, never in composition.
 */
@ShellSeamApi
@Stable
class EntryVisibility(
    private val tabSelected: () -> Boolean,
    private val uncovered: () -> Boolean,
    private val settled: () -> Boolean,
    /** The transform the entry's own layer has this frame. */
    val pose: EntryPose = EntryPose.Identity,
) {
    /** The entry's tab is the selected one; true outside tabs. Flips on the switch frame. */
    val isTabSelected: Boolean get() = tabSelected()

    /** The entry is the target top of its stack and nothing is presented above its host. */
    val isUncovered: Boolean get() = uncovered()

    /** The entry's own arrival (a push, the pop that reveals it, a tab reveal) is 99 % done. */
    val isSettled: Boolean get() = settled()
}

/** The entry layer's transform, for a layer drawn outside the entry that must move with it. */
@ShellSeamApi
interface EntryPose {
    /** Applies the entry's scale, translation, alpha and pivot to [scope], a layer whose top-left is at [origin] in window px. */
    fun applyTo(scope: GraphicsLayerScope, origin: Offset)

    companion object { val Identity: EntryPose }
}

@ShellSeamApi
val LocalEntryVisibility: ProvidableCompositionLocal<EntryVisibility> =
    staticCompositionLocalOf { EntryVisibility({ true }, { true }, { true }) }

/** The window-space bounds of the pane this content is in; null for the whole window. */
@ShellSeamApi
@Stable
class PaneBounds(
    private val bounds: () -> Rect?,
    private val onBlock: (blocker: Any, blocked: Boolean) -> Unit = { _, _ -> },
) {
    /** The stored rect, replaced only when the pane moves: reading it allocates nothing. Read in measure or placement. */
    val rect: Rect? get() = bounds()

    /** A sheet bounded by this pane blocks it (B-51): the pane's subtree is hidden from accessibility while any blocker is registered. */
    fun block(blocker: Any, blocked: Boolean) = onBlock(blocker, blocked)
}

@ShellSeamApi
val LocalPaneBounds: ProvidableCompositionLocal<PaneBounds> = staticCompositionLocalOf { PaneBounds({ null }) }

/** The tab bar's frame in window px, for a sheet that rests on it. Never the keyboard (C1-11). */
@ShellSeamApi
@Stable
class BarFrame(private val settled: () -> Float, private val live: () -> Float) {
    /** The bar's top when shown in full and settled: detents resolve against it. Changes when the bar's size does. */
    val settledTop: Float get() = settled()

    /** The bar's top this frame, while it minimises on scroll or returns. Read in placement. */
    @get:FrequentlyChangingValue
    val liveTop: Float get() = live()
}

@ShellSeamApi
val LocalBarFrame: ProvidableCompositionLocal<BarFrame?> = staticCompositionLocalOf { null }

/** The presentation depth of this content: 0 in the shell, d in the d-th presented entry, a sheet's level inside it. */
@ShellSeamApi
val LocalPresentationLevel: ProvidableCompositionLocal<Int> = staticCompositionLocalOf { 0 }

// component/sheet/surface/SheetBackdrop.kt (WP03 declares; WP05 implements)
/**
 * The sheet's visible frame in its own coordinates, and what its live body needs. Non-generic
 * getters, so nothing boxes; read in draw; allocate nothing (C1-20).
 */
@ShellSeamApi
interface SheetFrame {
    @get:FrequentlyChangingValue val left: Float
    @get:FrequentlyChangingValue val top: Float
    @get:FrequentlyChangingValue val right: Float
    @get:FrequentlyChangingValue val bottom: Float
    val cornerRadius: Float
    /** 0 glass, 1 opaque: the pass is skipped at 1, and whenever the stack says this level must not sample. */
    @get:FrequentlyChangingValue val opaque: Float
    /** The presenter's dim, which the recorded scene does not contain. */
    @get:FrequentlyChangingValue val dim: Color
}

/** Draws a Glass sheet's live body: the scene behind the frame, blurred, under the dim and a tint. */
@ShellSeamApi
@Stable
interface SheetBackdrop {
    fun Modifier.sheetBackdrop(frame: SheetFrame): Modifier
}

/**
 * One per window, installed by the outermost AppTheme, stable for the window's life. GlassStage
 * writes [backdrop] when it attaches and when the glass tier changes; null makes Glass draw Frosted.
 */
@ShellSeamApi
@Stable
class SheetBackdropHolder internal constructor() {
    var backdrop: SheetBackdrop?   // snapshot-backed
}

@ShellSeamApi
val LocalSheetBackdrop: ProvidableCompositionLocal<SheetBackdropHolder?> = staticCompositionLocalOf { null }
```

- `SheetStack` replaces `largeFraction` on `AppSheetState` and on `SheetSceneStrategy` (N10): the shell no longer casts
  a strategy, and in-screen sheets reach the shell too.
- `LocalTabVisible` (`ds4/foundation/layout/Chrome.kt:40`, `:48-52` [C]) stays as the conjunction
  `isTabSelected && isUncovered` for clocks and shimmers; WP04 provides it per entry from the same holders
  (`nav4/stack/StackLayout.kt:123`, `:136` [C]). Sheets never read it: it is false on every push, which would park a
  sheet instantly instead of sliding it (C2-04).
- `EntryVisibility` replaces revision 1's single `isUncovered`. WP04 writes each getter from state it already has: the
  tab selection (`nav4/shell/tabs/KeptTabs.kt:120` [C]), the stack scene's target top, the arrival's 99 % point
  (`navigation.backGateRelease`, `nav4/back/BackGate.kt:108` [C]) and the entry's `pose(key)`
  (`nav4/stack/StackLayout.kt:128` [C]).
- `SheetBackdropHolder` exists because the overlay host is an ancestor of the shell (`ds/theme/AppTheme.kt:145` [C])
  and `GlassStage` a descendant: the stage cannot provide a local around both hosts, and swapping a static local on a
  tier demotion (`glass/GlassQuality.kt:96-108` [C]) would recompose both hosts and every sheet's content at the moment
  the GPU watcher has fired (C1-21).

### 4.11 Navigation API (WP04, `:core:navigation`)

```kotlin
// Presentation.kt, replacing nav4/Presentation.kt:8-24
sealed interface Presentation {
    /** A sheet over the current screen, which stays composed and visible beneath it. Always modal. */
    data class Sheet(val spec: SheetSpec = SheetSpec()) : Presentation

    data object Cover : Presentation
}

/**
 * How a destination presented as a sheet rests and leaves. Its look comes from the theme, or from
 * its screen through ProvideSheetStyle; its header, footer and confirm hook from its screen.
 */
@Immutable
class SheetSpec(
    val detents: List<SheetDetent> = AppSheetDefaults.Detents,
    val initialDetent: SheetDetent = detents.first(),
    val dismissible: Boolean = true,
    val material: SheetMaterial = SheetMaterial.Automatic,
    val properties: AppSheetProperties = AppSheetProperties(),
    /** What screen readers call the sheet, as a string resource: the graph is built outside composition. */
    @StringRes val paneTitle: Int? = null,
) {
    // init: require(detents.isNotEmpty()); require(initialDetent in detents)
    fun copy(/* every property, defaulting to this one's */): SheetSpec
    // equals, hashCode and toString over every property
}

internal object PresentationKey : NavMetadataKey<Presentation>

/**
 * Steps of a flow that stay inside a sheet: a nested stack with its own push and pop, clipped to the
 * sheet. Back pops this stack first and closes the sheet from its root. Each step can set the sheet's
 * header and footer with ProvideSheetHeader and ProvideSheetFooter. Outside a sheet it is an ordinary
 * single-pane host.
 */
@Composable
fun AppSheetNavHost(
    backStack: NavBackStack<AppNavKey>,
    modifier: Modifier = Modifier,
    entryProvider: (AppNavKey) -> NavEntry<AppNavKey>,
)
```

- `entry<K>(presentation = Presentation.Sheet(SheetSpec(...)))` stays the only way a feature declares a sheet
  destination (`nav4/NavGraph.kt:50-57` [C]). `register` adds `require(presentation == null || pane ==
  NavPane.Single)`: a destination is a pane or a presentation, never both (C2-58). "Detail as a sheet on a phone, a
  pane on a tablet" (SwiftUI's `.inspector`) is phase 2 (§20).
- `SheetDetent` is the design system's. WP04's enum (`nav4/Presentation.kt:18` [C]), WP03's `SheetPresentation`
  (`wt/WP03/.../core/navigation/SheetScene.kt:24-28`) and WP07's copy are deleted (N3).
- The metadata travels under `PresentationKey` (`NavMetadataKey` exists in navigation3 1.1.7 [V] judge-3 §5, and in
  1.2.0). The string key `PRESENTATION_METADATA` and its `as?` read go (`nav4/presentation/PresentationHost.kt:199-205`
  [C]).
- A destination's header and footer come from its own screen (`ProvideSheetHeader`/`ProvideSheetFooter`), its look and
  grabber from `ProvideSheetStyle`, its confirm hook from `SheetDismissGuard` or `SheetDismissConfirmation`. Only
  behaviour and the pane title travel through metadata.
- A feature reaches its destination sheet's state through `LocalSheetSurface.current?.scope?.sheetState`, for example to
  expand it from a button, and closes it with `scope.dismiss()` (after saving) or `scope.requestDismiss()`.

### 4.12 Not in the API

| Left out | Why |
|---|---|
| A `Dialog`- or `Popup`-window sheet, for any preset or the DevTools panel | D-22 |
| `confirmValueChange` | Deprecated in foundation; the anchor set and `SheetDismissGuard` replace it |
| Per-path dismissal switches (drag, Back) and `SheetBackBehavior.StepDown` | `dismissible`, the guard and `scrimTapDismisses` cover the real uses; D-12. `dragEnabled` holds the sheet still; it is not a dismissal switch |
| `onDismissAttempt` on the component | D-11 |
| A `title: String` overload | `header = { AppSheetHeader(title) }` is one line |
| A third `AppSheet(state, …)` overload | `AppSheetLayout` behind `@ShellSeamApi` (D-38) |
| A `ColumnScope` content receiver | D-39: a lazy list or `SheetColumn` is the one root |
| Wrapping content in a scroll container | D-25 |
| A per-detent background effect (iOS 26.1 `Detent.backgroundEffect`) | The `e` ramp covers the shipped behaviour |
| `Modifier.sheetBleed()` | D-7 |
| `SheetTransition.Zoom`, `Modifier.sheetSource` | Phase 2 (§20) |
| Concentric bottom corners | A non-uniform round rect becomes a path clipped on the canvas below API 33 [V] (`OutlineResolver.android.kt:263`, `:282-288`); minSdk is 26. Phase 2 with a recording |
| `NavResultKey`, `popWithResult`, `NavResultEffect` | D-32 |
| A public `@JvmInline value class` detent | Boxed every time it is used as the sealed type; detents carry payloads |
| Public data classes for options | D-37: plain classes with `copy` |
| Multiple selection in `AppChoiceSheet` | A different structure (Done button, no close on select): a recipe |
| Sheet geometry constants in `AppSheetDefaults` | Generated `DesignTokens.Sheet` (C2-45) |

---

## 5. Behaviour

Rules are numbered so tests and reviews can cite them (`B-22 violated`). `v` is a velocity in px/s, positive
downward. "The person" means a touch, a key or an accessibility action; "code" means `AppSheetState` calls, `visible`,
`item` and pops. §15.6 maps each rule to its owner package and test.

### 5.1 Lifecycle

- **B-1 First frame.** A sheet is composed, measured and placed at the hidden offset `h` on the frame it is composed,
  and drawn in that pose (API-13). No `LaunchedEffect` sets up initial state. Under `LocalInspectionMode` it is placed
  at its initial detent instead (§4.4), so previews show it. An in-screen sheet composes **one frame after** its call
  site: `HostedOverlay` registers its entry in a `DisposableEffect` (`ds/component/overlay/AppOverlayHost.kt:90-98`
  [C]), which runs after the caller's frame. Its content is measured at Large's height, so a lazy list in a Medium
  sheet composes about twice the rows it shows on that frame. With `keepContent`, a sheet opened again composes
  nothing: its content stayed composed, parked (B-14).
- **B-1a Content layout** (D-39). The content is laid out in a `Box` at the sheet's full width with Large's content
  height as its maximum, children at the top start. Its first frame is the frame above.
- **B-2 Show.** `show()` waits for (a) the first layout with anchors and (b) any slot registration made by the content
  in its first composition (`ProvideSheetHeader`, `ProvideSheetFooter`, `ProvideSheetStyle`) to have been composed and
  measured, so `FitContent` and `Peek` include them. It then starts the spring on the next frame (the clock starts
  after composition, SMOOTHNESS S3): `h → initial detent` on `springs.sheet` (÷ speed). Presets use `springs.dialog`
  (M17). The first moving frame shows ≤ 4 % of the travel (CP1 §1 condition 1). Implementation: the slot registry
  carries a `pending` flag set by a registration and cleared by a `SideEffect` in the slot host; `show()` awaits
  `anchors != null && !pending`. Frame by frame for an in-screen sheet with a provider: N the caller composes; N+1 the
  overlay host composes the sheet and its content, which registers the provider; N+2 the slot host composes the
  provider's content; N+3 the first moving frame. Without a provider the first moving frame is N+2. §16.1 measures it
  as `sheet_present.tap_to_motion_ms`. A `show()` while the owner entry is still arriving (`EntryVisibility.isSettled`
  false) waits for it to settle, so a sheet never composes on a push's busiest frames and never slides in beside a
  screen that is sliding in (B-14a).
- **B-3 Hide.** Idempotent: a second `hide()` joins the running one. `show()` during `hide()` turns the sheet around from
  its offset with `lastVelocity` (fixes D1, D7). `onHidden` runs once the sheet has gone, whoever closed it.
- **B-4 The `visible` overload.** One `AppSheetState` per call site (not one per opening, as WP03 did at
  `ds/component/overlay/AppSheet.kt:273-286` [C]). `LaunchedEffect(visible) { if (visible) state.show() else
  state.hide() }`. The sheet stays composed while `visible || state.isVisible`. **The caller's value is the truth**
  (C2-10): a `hide()` from code that completes while `visible` is still true calls `onDismissRequest()` once, so the
  caller's flag follows and a later `visible = true` shows the sheet again; `show()` while `visible` is false returns
  at once. After the person closes it, `visible` stays true until the caller clears it in `onDismissRequest`;
  meanwhile the sheet is hidden, not drawn, not hit-tested and not in the `SheetStack`, so it costs nothing and cannot
  be stuck half-way (D1 cannot happen).
- **B-4a The call site leaves composition.** A sheet whose `AppSheet` call leaves composition while visible (an `if`
  around it turned false, a lazy item scrolled away, the owner removed without a pop) is removed in that frame: it
  unregisters from the `SheetStack` (its presenter un-recedes and its dim goes in the same frame), reports
  `HostMotion(false)`, removes its Back handler, lifts its focus guard and restores focus (B-42a). `onHidden` does not
  run: its caller is gone. A code rule flags `AppSheet` inside an `if`, `when` or lazy item scope, and the docs say
  "keep the call composed and drive `visible`" (§19.1 item 9, §19.7).
- **B-5 The `item` overload.** A non-null `item` shows the sheet; null hides it, and the last item stays composed
  during the exit. A different non-null item while one is shown: `hide()`, then `show()` with the new item.
- **B-6 Restored** (C2-55). For the `visible` and `item` overloads, whether the sheet shows comes only from the caller;
  the restored state contributes its detent. `visible` true on the first composition after a restore snaps to that
  detent with no slide (and, at a Large-equivalent stop, the presenter is receded from the first frame); `visible`
  false means the sheet is absent. A destination sheet restored with `shown = 1` snaps the same way (§11.2). A cold
  start through a deep link to a sheet destination is not a restore: the sheet slides in, starting after the
  presenter's first frame (CP1's first-frame rule).
- **B-7 A dismissal by the person** (B-33) is decided at the release, the tap, Back's commit or the key. Then, in the
  same frame: `leaving` is set, `onDismissRequest()` runs once, synchronously, and the exit starts, seeded with the
  release velocity (0 for a tap or a key). The sheet leaves by itself; the owner only removes it.
- **B-8 A dismissal by code** (`hide()`, `visible = false`, `item = null`, a pop) asks no guard. It calls no
  `onDismissRequest`, except B-4's `hide()` under a caller that still shows the sheet.
- **B-9 `animateTo`, `expand`, `collapse`** retarget with the current velocity on `springs.sheet`. They return without
  moving while a pointer is down on the sheet, while it leaves (fixes D9) and while it is parked (§4.4 mutex table).
  `snapTo` is for restoration and tests. On a form or side sheet they follow R-10.
- **B-10 Resize and content changes.** Rotation, unfold, freeform resize, a font-scale change or a content-height change
  re-resolve the stops in measure (R-11) from the keyboard-free measure; the keyboard never re-resolves them (R-3). The
  detent's identity is kept; an animation in flight is retargeted (fixes D10). A sheet resting on `FitContent` whose
  content height changed animates to the new height on `springs.sheet`. A placement change (Bottom ↔ Center ↔
  Start/End) snaps to the new frame with no animation: into Center or a side panel it records the current detent; back
  into Bottom it lands on the recorded detent, else the tallest active one (R-10, R-11).
- **B-11 `updateDetents`.** A sheet resting on a detent that stays active stays. Otherwise the layout node launches
  `animateTo(nearest active detent by height)`.

### 5.2 Hosting and parking

- **B-12 Hosts.** `Presentation.Sheet` destinations render in the presentation host (§11.2). `AppSheet(visible)` and
  `AppSheet(item)` render through `HostedOverlay` in the overlay host (`ds/component/overlay/AppOverlayHost.kt:81-100`
  [C]), keeping the caller's composition locals, whatever their modality. Both register in `LocalSheetStack` with
  their level (§3.2). `HostedOverlay` keys each entry by its call site's compound key and composes it inside the call
  site's `SaveableStateHolder`, and its modality is a snapshot value read where semantics are applied, never an effect
  or `key` input (§11.1, D-40). So the content's `rememberSaveable` state (a list's scroll, a typed field,
  `SheetColumn`'s scroll) survives recreation and process death, and crossing `upThrough` (B-51) creates no
  composition.
- **B-13 Bounds and class.** A modal sheet's bounds are the window (or one half of a separating hinge, §8) and its
  placement class is the **window** class. A non-modal sheet's bounds are `LocalPaneBounds` (the window when null) and
  its placement class is that pane's. Every metric of the sheet and its content's adaptive locals come from the
  placement class (§7.2, §11.4); the caller's container class is never read.
- **B-14 Parking.** An in-screen sheet parks while its owner is not shown, and keeps its logical state (`isVisible`,
  `currentDetent`, the app's `visible`):
  - **owner's tab not selected** (`EntryVisibility.isTabSelected` false): parks and returns on the switch frame, with
    no animation (fixes N7);
  - **owner covered** (`isTabSelected` true, `isUncovered` false: a push in its own stack, from the push's first
    frame, or a presentation above its host): slides to hidden on `springs.sheet` and slides back to its detent when
    uncovered (judge-3 G23). Never instant (C2-04);
  - **parked the `KeptTabs` way** (`nav4/shell/tabs/KeptTabs.kt:38-42` [C]; C1-12): still measured and placed, so its
    layers and display lists survive; the sheet layer at alpha 0 (the renderer skips it without an offscreen pass) and
    translated off the window in placement; semantics cleared by a node that calls `invalidateSemantics()` from a
    `snapshotFlow`; pointer input ignored through a plain field; no frame-rate vote; out of the `SheetStack` (the
    presenter un-recedes in step during the park animation, because the stack reads the parked fractions); Back not
    enabled; its focus guard lifted;
  - `isTabSelected` and `isUncovered` are read in the layer block, in placement and in a `snapshotFlow`, never in
    composition, so a tab switch recomposes nothing in the sheet and re-records nothing on the return frame;
  - the park slide is a separate `Animatable` (`parkProgress`, 0 to 1, on `springs.sheet.unit`, whose 0.001
    threshold is about a pixel of the slide) added to the placement as `parkProgress · (h − y)`, so the anchored
    state and its detent are untouched (L1);
  - a sheet that is already leaving is not parked: it finishes its exit drawn;
  - the same parking applies to every hosted overlay (alerts, action sheets, menus).
- **B-14a Owner moving** (C1-17, C2-05). The sheet's layer applies its owner entry's own transform,
  `EntryVisibility.pose.applyTo(layerScope, layerWindowOrigin)`, read in the layer block (L0), composed with the
  sheet's own back shrink and recede. So it shrinks and moves with the owner's Gmail-style back card during a
  predictive pop (REQUIREMENTS 24, 32), leaves with it on commit (the owner then leaves composition, B-4a, with the
  sheet already off screen), returns with it on cancel, and moves with a tab reveal. A sheet is never drawn full size
  over a card that is shrinking or sliding. **Fallback**, if WP04 cannot expose the pose: the sheet parks from the
  gesture's first frame with `parkProgress` following the gesture's eased progress, unparks on cancel, and finishes
  the park on commit. `SheetParkingTest` asserts the same mid-gesture outcome for either.
- **B-15 Host motion.** Every sheet reports `LocalHostMotion.current.report(state, moving)` from an effect over
  `snapshotFlow`, where `moving` is true while a show, hide, park or unpark animation runs and more than 1 % of its
  travel remains (`navigation.backGateRelease` = 0.99, `nav4/back/BackGate.kt:108` [C]). Drags, settles and the
  owner-pose following are not reported. `report(state, false)` always runs before the owner removes the sheet. WP04
  counts these as **overlay motion** (§11.3): the gate counts Back presses meanwhile, and `NavGuard` returns `Wait`
  for any command, never `Drop`, so a push submitted while a sheet presents or leaves is applied once the sheet is 99 %
  there (C1-08; fixes N4(c)).

### 5.3 Gestures

- **B-16 Drag.** Anywhere on the surface: grabber band, header, footer and the content's background. The sheet layer
  carries `anchoredDraggable(state, Vertical, enabled = !leaving && !parked && dragEnabled && placement allows it,
  overscrollEffect = SheetStretch, flingBehavior = SheetFlingBehavior)` at foundation's `Default` priority;
  `startDragImmediately = isAnimationRunning` (`src/foundation-1.12.1/.../AnchoredDraggable.kt:479` [G]). The finger
  tracks 1:1 (F1). Inside a `Modifier.sheetDragExclusion()` region, a node consumes every pointer change on the `Main`
  pass after the region's own handlers, so the sheet's draggable sees consumed changes and never starts, and a nested
  scroll connection there consumes all post-scroll and post-fling leftovers, so nothing reaches the sheet. A modal
  Start/End panel drags horizontally toward its edge instead (§8).
- **B-17 Catch.** An `Initial`-pass pointer node on the sheet layer stops any running show or settle on a finger down
  anywhere on the sheet, content included, through `anchoredDrag(MutatePriority.PreventUserInput) {}` (§4.4; fixes
  D8; judge-1 graft 4). It is skipped while the sheet leaves or is parked, and it consumes nothing on the down. The
  node also keeps two plain fields: whether a pointer is down on the sheet (B-9) and the gesture's `PointerType`
  (B-27). If the down stopped motion and the pointer never crosses touch slop vertically:
  - it consumes the matching up, so a tap that stops a moving sheet does not also activate the row under it;
  - on that up, or on a cancel, or once a horizontal scroll takes the pointer, the sheet settles at `v = 0` to the
    stopped animation's target (a stopped present to its initial detent), with no haptic (C1-26). It never rests
    between stops.
- **B-18** A leaving sheet ignores touches (the person committed; code decided). "Leaving" is the `leaving` flag, set
  in the decision's frame (§4.4), so a touch 16 ms after a flick-dismiss is not a catch.
- **B-19 Grabber activation** (tap, Enter, Space, TalkBack double tap, Switch Access): the next taller active detent;
  from the tallest, the lowest. It never dismisses. The click is labelled with the state it moves to (§9). The
  `Threshold` haptic plays on arrival (B-61).
- **B-20 Pointer** (C2-21). A mouse moves the sheet only by a primary-button press-and-drag, as a finger does. A wheel
  notch and a touchpad's two-finger scroll are scrolls, never drags: the B-17 node records a `PointerEventType.Scroll`
  event, or a touch stream the platform classifies as a two-finger swipe (`MotionEvent.CLASSIFICATION_TWO_FINGER_SWIPE`
  [U: whether Compose surfaces it, §21]), as `Mouse`. A stylus drags like a finger, except that handwriting started on
  a text field wins: the field's handwriting detector consumes the stream before the sheet's draggable sees it [U:
  `SheetGestureTest` proves it]. The grabber shows a hover state and `PointerIcon.Hand` (ADAPTIVE L-5).
- **B-21 Press feedback** on rows inside a sheet appears no later than the platform tap timeout (100 ms,
  `fnd112/Clickable.android.kt:40` [V]) plus one frame, as in a `LazyColumn` row. `clickable` delays a press by the
  tap timeout whenever an ancestor gesture is waiting (`fnd112/Clickable.kt:2155-2156`, `:2179-2190` [V]), and the
  sheet's draggable is such an ancestor; CP1 already accepts that delay (`decisions/CP1.md:137`). No foundation flag
  changes it in 1.12.1 (C1-25).

### 5.4 Choosing a stop on release

- **B-22 Release rule.** With release offset `y`, velocity `v`, projection `p = y + T · v` (`T` =
  `sheet.release.projectionSeconds` = 0.099 s, iOS's 0.99/ms deceleration), detent flick `F` =
  `sheet.release.detentFlick` (400 dp/s) and dismiss flick `D` = `sheet.release.dismissFlick · H` per second (2 H/s):
  1. `y < o_1` (over-pulled above the top detent) → `o_1`.
  2. `o_1 ≤ y ≤ o_n` (between the top and the lowest detent, inclusive):
     - `|v| ≥ F` and a detent lies strictly beyond `y` in the direction of `v` → the nearest such detent (one stop per
       flick, never hidden);
     - `|v| ≥ F` upward and no detent lies above `y` → `o_1`;
     - `v ≥ F` downward and no detent lies below `y` (the sheet is at its lowest detent) → rule 3's test;
     - otherwise → the detent nearest `p`, clamped to `[o_1, o_n]`.
  3. `y > o_n` (below the lowest detent), or sent here by rule 2:
     - `−v ≥ F` (upward) → `o_n`;
     - dismissal allowed → hidden if `v ≥ D` or `(y − o_n) / (h − o_n) ≥ sheet.release.dismissPosition` (0.48, judged
       at the release position, not projected: Flutter's sheet stays while its controller is above 0.52, which falls
       from 1 as it is dragged, `src/flutter/sheet.dart:1071`, `:1110-1119` [M]); else `o_n`;
     - dismissal blocked → `o_n`; if the allowed rule would have dismissed, the innermost enabled guard's
       `onAttempt(Drag)` runs once for this gesture.

  A release that resolves to hidden sets `leaving` and calls `onDismissRequest` in the release frame (B-7), before the
  exit animation starts. Worked on the Redmi (Medium top 436 dp, Large top 43 dp, `h` 873 dp, `h − o_n` = 437 dp):

  | Release | Rule | Lands |
  |---|---|---|
  | At Medium, 300 dp/s up | 2, projection 436 − 30 = 406 | Medium |
  | At Medium, 600 dp/s up | 2, flick | Large |
  | 60 dp above Medium, 200 dp/s down | 2, projection 376 + 20 = 396 | Medium |
  | At Large, 1200 dp/s down, `[Medium, Large]` | 2, flick | Medium |
  | At Large, 3000 dp/s down, `[Medium, Large]` | 2, flick (one stop) | Medium |
  | At Large, 1800 dp/s down, `[Large]` | 2 → 3, ≥ 2 H/s | hidden |
  | At Medium, 1200 dp/s down | 2 → 3, < 2 H/s, 0 / 437 = 0 < 0.48 | Medium |
  | At Medium, 1800 dp/s down | 2 → 3, ≥ 2 H/s | hidden |
  | 150 dp below Medium, 900 dp/s down | 3, < 2 H/s, 150 / 437 = 0.34 < 0.48 | Medium |
  | 250 dp below Medium, still | 3, 250 / 437 = 0.57 ≥ 0.48 | hidden |

  This replaces WP03's projection-only rule (which needs about 4.6 H/s to dismiss from Large, `research.md` §1.9) and
  foundation's 125 dp/s default (`AnchoredDraggable.kt:1551-1566` [G]), which M3 found reads small scroll flings as
  dismissals (`5ab9549e`). Whether iOS skips Medium on a very hard flick from Large is [U]; one stop per flick is
  predictable and matches androidx's `computeTarget` (`AnchoredDraggable.kt:1483-1514` [G]).
- **B-23 Settle.** On `springs.sheet.pixels` (÷ speed; 0.5 px threshold), seeded with the release velocity.

### 5.5 Rubber band

The formula is iOS's, shared with overscroll and pull to refresh: `shown = c · x · d / (d + c · x)`, `c` =
`scroll.rubberBand` = 0.55 (`ds/theme/AppMotion.kt:182-186` [C]).

- **B-24 Above the top detent:** `d = o_1 − (largeTop − sheet.stretch.aboveLarge · H)`. At Large that is 0.008 H
  (7.0 dp on the Redmi: a 200 dp over-pull shows 6.6 dp) [M] `sheet.dart:27-37`. A Medium-only sheet gets the room up
  to Large's stretch line (≈ 400 dp: a 200 dp pull shows 86 dp).
- **B-25 Below the lowest detent while dismissal is blocked:** `d = h − o_n` (from Medium, 437 dp: a 300 dp pull shows
  120 dp).
- **B-26 Release from a stretch:** `springs.sheet` with the release velocity times the band's slope at release. A
  settle that overshoots the top detent passes through the same band, so the visible overshoot never exceeds 0.008 H
  above `largeTop`. The surface stretches with its top edge; its bottom edge stays put, so a floating sheet never shows
  a gap below itself.
- Implemented as a custom `OverscrollEffect` (`SheetStretch`) given to `anchoredDraggable(overscrollEffect = …)`,
  whose excess the layout reads in placement (`AnchoredDraggable.kt:410-477` [G]). The sheet's own `dragBy(delta)`
  applies the same band past the stops and is shared by the draggable's overscroll path and the nested-scroll
  connection (B-28), so a list handing its scroll to the sheet bands exactly as a surface drag does (C1-07).

### 5.6 Nested scroll and reaching the last item

- **B-27 `Automatic` resolution, per gesture** (C2-21, C2-53). The B-17 node records each gesture's pointer type
  before the content sees it, so the choice follows the input, not the window:
  - a **modal** sheet: `Resizes` for a gesture started by `Touch` or `Stylus`; `Scrolls` for a `Mouse` gesture, a
    wheel notch (`NestedScrollSource.UserInput` too, [V] `NestedScrollModifier.kt:294`) and a touchpad's two-finger
    scroll (B-20). A touchscreen Chromebook or a tablet with a trackpad cover therefore never grows the sheet from the
    wheel;
  - a **non-modal** sheet: `Scrolls`. Reason: a persistent sheet is browsed at its resting detent over a live map,
    and growing it over the map on every list scroll hides what the person is browsing for. Apple Maps' behaviour is
    [U] (§21); flipping this changes only this rule, not the API;
  - while TalkBack's touch exploration is on (`AccessibilityManager.isTouchExplorationEnabled`, read in a
    `snapshotFlow`-fed field): `Scrolls`, so B-31's reach padding applies and every row is reachable (C2-12).
- **B-28 `Resizes`** (iOS `prefersScrollingExpandsWhenScrolledToEdge = true` [A]). Deltas go through the sheet's own
  `dragBy()` inside **one `anchoredDrag(MutatePriority.Default)` session per gesture**: opened with
  `CoroutineStart.UNDISPATCHED` on the gesture's first delta, so the mutex is held before that delta is applied and
  each `dragTo` runs synchronously in `onPreScroll`/`onPostScroll`; closed in `onPreFling`, which runs
  `SheetFlingBehavior` (B-22) inside the same session and consumes the velocity when the sheet is not resting.
  - `onPreScroll`: an upward `UserInput` delta goes to the sheet until it reaches its top detent; the content scrolls
    the rest.
  - `onPostScroll`: a downward `UserInput` leftover at the content's top goes to the sheet, down to hidden or into the
    B-25 band.
  - The connection consumes nothing while `leaving`, while `parkProgress > 0`, while `show()` or `hide()` holds the
    mutex at `PreventUserInput`, when `dragEnabled` is false, or for a gesture whose pointer type is not `Touch` or
    `Stylus`.
  - Never `dispatchRawDelta`, which clamps to the anchors (so the bands are unreachable and the list stretches inside
    a static sheet) and bypasses the mutex (so it fights a running show, hide or park) (`AnchoredDraggable.kt:1234-1250`
    [V]); never `AnchoredDraggableState.settle(velocity)`, deprecated and throwing unless
    `usePreModifierChangeBehavior` (`:1062-1069` [V]) (C1-07).
- **B-29 `Scrolls`**: upward deltas go to the content first and never grow the sheet; the downward leftover at the
  content's top still drags it, through the same session. The grabber band, header and footer still resize it
  ("People can always resize your presentation using the drag indicator" [A]).
- **B-30** Momentum never moves the sheet: deltas whose source is not `UserInput` are ignored, so a fling that reaches
  the top bounces in the list's own overscroll (`ds/component/overlay/AppSheet.kt:160-161` [C], kept). An
  accessibility scroll is not `UserInput`, so TalkBack scrolling never resizes the sheet; the grabber's expand action
  does.
- **B-31 Reachability with `Scrolls`.** The content is measured at Large's height, so at a partial detent part of it is
  below the visible bottom. `contentPadding.bottom` adds `y_settled − largeTop` at each settle (≤ 1 re-measure per
  settle, never during a drag), so the last row can scroll into view at Medium.
- **B-32 `contentScrolled`** comes from the same connection: it rises with upward scroll the content consumes and
  returns to 0 when the content hands back leftover at its top. A programmatic jump (`scrollToItem`) can leave it stale
  until the next touch scroll [D]; acceptable for a fade.

### 5.7 Dismissal and the confirm hook

- **B-33 Ways out.** A guard on a non-modal sheet behaves as the "Guard enabled" column.

| Path | Modal, no guard | Guard enabled | Modal, `dismissible = false` | Non-modal, dismissible (C2-11) | Non-modal, `dismissible = false` (persistent) |
|---|---|---|---|---|---|
| Drag or flick down (or toward the edge, modal Start/End) | B-22 rule 3 | rubber band (B-25); `onAttempt(Drag)` once if rule 3 would have closed it | rubber band; attempt to a guard if any | B-22 rule 3 | rubber band; attempt to a guard if any |
| Tap on the dim (`scrimTapDismisses`); a tap outside the anchored action card | closes (`Scrim`) | `onAttempt(Scrim)` | attempt to a guard if any; else consumed | undimmed detents: no dim, the tap passes through to the screen and never dismisses; above `upThrough`: closes | undimmed: passes through; above `upThrough`: consumed, attempt to a guard if any |
| Back gesture | predictive dismiss (B-38) | resisted shrink, commit springs back, `onAttempt(Back)` | resisted shrink, commit springs back, attempt to a guard if any | predictive dismiss at every detent | steps down one detent; at the lowest the handler is disabled and Back reaches the screen (B-37) |
| Back key | closes | `onAttempt(Back)` | consumed; attempt to a guard if any | closes | as the gesture, without preview |
| Escape (B-42) | closes | `onAttempt(Escape)` | as the Back key | closes | as the Back key |
| `requestDismiss()` (close button), TalkBack's dismiss action | closes (`Request`) | `onAttempt(Request)` | attempt to a guard if any | closes | attempt to a guard if any |
| `scope.dismiss()` (Done after save) | closes | closes | closes | closes | closes |
| Code (`hide()`, `visible = false`, pop) | closes | closes | closes | closes | closes |

- **B-34 Guard semantics.** Dismissal is blocked when `dismissible = false` or an enabled guard is registered. Attempts
  go to the innermost enabled guard of the uncovered step (§4.8 precedence), once per gesture or press: a drag's on
  release, the dim's, Escape's and `requestDismiss`'s at once, the Back gesture's on commit. No haptic on an attempt:
  the confirmation the app shows is the feedback. A guard with `enabled = false` registers nothing, so a clean form
  keeps the system's predictive back (one handler that branches, navigation-event skill `SKILL.md:255-266` [G];
  VitFleet `LoadFormScreen.kt:128`). `SheetDismissConfirmation` is a guard whose `onAttempt` shows a stacked
  `AppActionSheet` (it stacks per B-54); Discard calls its `onDiscard`, Keep Editing closes the action sheet.
- **B-35 Callbacks.** `onDismissRequest` runs for every path that closes the sheet in the "no guard" and non-modal
  dismissible columns, including `scope.dismiss()`, and for B-4's `hide()` under a caller that still shows it; never
  for other code paths. `onHidden` runs once the sheet has gone, for every path.

### 5.8 Back, predictive back and Escape

- **B-36 Registration.** One handler per sheet, the class-based pattern of
  `ds/component/overlay/OverlayBackHandler.kt:38-50` [C]: an `object : NavigationEventHandler(…)` added with
  `dispatcher.addHandler(handler, PRIORITY_OVERLAY)` on the dispatcher of the composition that calls `AppSheet`, its
  `isBackEnabled` written from the B-37 `snapshotFlow` collector, branching in `onBackCompleted`. Not the Compose
  `NavigationBackHandler`, which registers at the default priority (`nec/NavigationEventHandler.kt:115`,
  `ne/NavigationEventDispatcher.kt:292-295` [V]) and applies `isBackEnabled` in a `SideEffect` (`:97-103`), so only
  through a recomposition (C1-27). Verified ordering [V] (navigationevent 1.1.2; 1.2.0 bytecode keeps the same
  deques): overlay handlers anywhere in the tree are asked before default ones, last registered first
  (`NavigationEventProcessor.kt:272`, `:579`); a handler is off while its dispatcher or an ancestor is off
  (`NavigationEventDispatcher.kt:169-170`, `NavigationEventHandler.kt:126`). So a sheet in a hidden kept tab is deaf,
  and a sheet's handler outranks the shell's.
- **B-37 Enablement.** Registration order is not trusted. The handler's `isBackEnabled` is true only when all hold:
  1. the sheet is the top-most visible sheet in stack order (B-54a), not covered and not parked;
  2. a destination sheet is not leaving (its extra presses go to the host gate, B-43);
  3. a way out or a branch exists: always for a modal sheet (Back on a blocked modal sheet is a consumed attempt,
     never a pass-through) and for a dismissible non-modal sheet (it closes at every detent, C2-11); for a persistent
     non-modal sheet, only above its lowest detent or while the IME target is visible (branch 0).

  The IME no longer disables the handler (C1-09): branch 0 handles it. Each input is a snapshot value that changes
  rarely; the collector sets `isBackEnabled`. None is read in composition, so nothing recomposes. A persistent sheet
  at its lowest detent leaves `isBackEnabled` false, so the system still plays back-to-home at a tab root, and the
  owner's own pop runs with the sheet following it (B-14a).
- **B-38 Branches**, in order:
  0. the IME target is visible (`SheetImeSource.target`, B-47) → the commit hides the keyboard
     (`LocalSoftwareKeyboardController.hide()`); no shrink, no attempt, no dismissal; progress events are ignored;
  1. a content `SheetBackHandler` of the uncovered step is enabled → it handles Back; no shrink;
  2. a persistent sheet (non-modal, `dismissible = false`) above its lowest detent → the preview moves the sheet down
     by `sheet.back.persistentFollow · p` (24 dp), no shrink; the commit runs `collapse()` (one detent per Back [U:
     against Google Maps, §21]);
  3. dismissal is blocked (an enabled guard, or `dismissible = false` on a modal sheet) → resisted shrink
     (`p · sheet.back.guardedShare`, 0.2); the commit springs back on `springs.sheet` and fires the attempt;
  4. otherwise → predictive dismiss.
- **B-38a A Back gesture during a present or a settle** (C2-07; REQUIREMENTS 32). At `onBackStarted` while the sheet is
  Presenting or Settling, the motion stops at the current offset (`anchoredDrag(PreventUserInput) {}`, as B-17), and
  the branch's preview runs from there. Cancel settles by B-22 with `v = 0` from that offset (a present lands on its
  initial detent). Commit exits from the shrunken pose; the sheet was held still for the gesture, so the exit starts
  at rest, as from a resting sheet. The Back **key** keeps the state table's reversal with velocity (§3.3).
- **B-39 Predictive progress and pose.** `p = CubicBezierEasing(0.1, 0.1, 0, 1).transform(event.progress)`
  (`src/BackHandler.kt:47-51` [G]), held in a float state read only in layer blocks (material3 reads it in composition,
  `src/BottomSheet.kt:168`: not copied). MDC bottom container (`research/predictive-back/SPEC.md:79` [G]):
  - Bottom placement: sheet layer `scaleX = 1 − min(48 dp, w) · p / w`, `scaleY = 1 − min(24 dp, hv) · p / hv`, `hv` the
    visible height, `transformOrigin` at the visible bottom centre; the content layer counter-scales
    `scaleY' = scaleX / scaleY` about its top so text keeps its aspect ratio (fixes D6; `src/BottomSheet.kt:475-480`).
  - Center placement: uniform scale `1 − (1 − sheet.back.formScale) · p` (to 0.90) about the card's centre.
  - Non-modal Start/End panel: as Bottom, about the panel's visible bottom centre.
  - Modal Start/End panel (§8): MDC's side container (`research/predictive-back/SPEC.md:80` [G]): width shrinks by up
    to 24 dp, or grows by up to 12 dp, depending on whether the swipe starts on the panel's own edge
    (`sheet.side.backShrink`, `backGrow`), height by up to 48 dp (`backY`), pivot at the panel's attached edge, edges
    mirrored in RTL; the exact edge-to-direction mapping is copied from `MaterialSideContainerBackHelper` [U: WP03
    reads it].
  - The dim does not change during the gesture; it follows `pf` once the exit starts. The presenter does not move
    during the gesture.
- **B-40 Commit and cancel.** Commit: the exit slides the shrunken sheet to hidden on `springs.sheet` while the scale is
  held, then resets it (MDC, `research.md` §2.5). One gesture closes the sheet from any detent. Cancel: the system
  springs the progress back before `onBackCancelled` (`predictive-back/SPEC.md:77`); any residual returns to 0 on
  `springs.sheet`.
- **B-41** The Back key, 3-button navigation, TalkBack's Back and API < 34 have no progress: no shrink, straight to the
  commit.
- **B-42 Escape** (C2-01). `onKeyEvent` (not `onPreviewKeyEvent`) on the sheet surface: an unconsumed `Key.Escape`
  key-down runs the Back key's path and reports `SheetDismissSource.Escape` (fixes D16, `ADAPTIVE-PLAN.md:565`).
  Focused children see Escape first, so a search field clears its query and a step inside `AppSheetNavHost` goes back
  before the sheet closes. The key reaches the surface because focus is inside every modal sheet (B-42a). An Escape
  no node consumed may also come back from the platform as Back (`OverlayBackHandler.kt:24-25` [C]); it is then a
  Back, reported as `Back`.
- **B-42a Focus on present** (C2-01, D-34). For every modal present, in every input mode, and for a non-modal sheet
  when it starts blocking (B-51):
  1. on the first moving frame, `saveFocusedChild()` on a `FocusRequester` attached to the app content box (the app-root
     focus group, `ds/component/overlay/AppOverlayHost.kt:57-62` [C]; `ui112/focus/FocusRequester.kt:156` [V]);
  2. focus moves to the sheet surface, a `focusGroup()` + `focusable()` node that carries the pane title, never a text
     field, so no keyboard opens. This takes focus and its keyboard from a field beneath, and emits
     `TYPE_VIEW_FOCUSED`, so TalkBack moves into the sheet as it does into MDC's
     (`src/mdc/BottomSheetBehavior.java:2520`, `:2537` [V]). The anchored action card focuses its first enabled
     action instead;
  3. while a modal sheet is up, the app content box has `focusProperties { onEnter = { cancelFocusChange() } }`, read
     from a plain field, so Tab and arrow traversal never leave the sheet; the sheet root keeps
     `onExit = { cancelFocusChange() }` ([V] `FocusProperties.kt:188`);
  4. on close (B-7, B-8, B-4a), in keyboard mode, `restoreFocusedChild()` (`FocusRequester.kt:174` [V]); for the
     action sheet, focus returns to its `SheetAnchor`. In touch mode clickables are not focusable
     (`ui112/focus/Focusability.kt:66-67` [V]), so where TalkBack lands is recorded by QA2 (§16.4) [U];
  5. while parked (B-14), the guard is lifted and focus inside the sheet is cleared.
- **B-43 Rapid Back during a dismissal** (decides N6):
  - **Destination sheet.** The commit calls `onDismissRequest` (B-7), which is the host's `gate.onBack(pop)`
    (`nav4/presentation/PresentationHost.kt:122` [C]): the pop happens while the gate is idle. `onRemove()` then runs
    the slide, which the sheet reports as host motion (B-15), and the sheet's handler is off while it leaves (B-37).
    The presentation host stays composed through the slide, because `PresentedSheetFrame` counts in its `live` set
    (§11.2; C1-06). Further presses reach the presentation host's `StackBackHandler` (enabled while
    `canGoBack || gate.moving`, `nav4/back/StackBackHandler.kt:67` [C]), are counted (`BackGate.kt:51-57`) and applied
    as one pop at 99 % of the exit (`BackGate.kt:108`); they pop further presented entries and are dropped at the
    presentation root, so they never reach the tab beneath. MASTER-PLAN:911 holds.
  - **In-screen sheet.** No stack owns it. Its handler stays enabled while it leaves and consumes and drops Back
    (WP03's tested behaviour, `SheetTest.kt:65-79` [C]). A burst on a sheet never goes past the sheet's presenter.
- **B-44 Back with the keyboard, made structural** (C1-09). `navigationevent` keeps one platform callback per
  priority, registered while a handler of that priority is enabled (`ne/OnBackInvokedInput.android.kt:76-91` [V]), so
  the app's default-priority callback can be re-registered after the IME's and win, for example when
  `StackBackHandler`'s `canGoBack || gate.moving` toggles on a sheet's own motion report. Two rules make the outcome
  independent of that order:
  1. while a modal sheet is up above a host, that host's default-priority stack handlers are disabled:
     `StackBackHandler`'s `enabled` adds `!stack.blocksBack(level)` (§4.10), derived from `SheetStack`, so it changes
     at most twice per opening and recomposes only `StackBackHandler`. The tab hosts are blocked from a modal sheet's
     present until it has gone; the presentation host's own handler is blocked while its top destination sheet is up
     and **not** leaving, so B-43's counting still works;
  2. the sheet's handler stays enabled while the IME target is visible, and its branch 0 hides the keyboard.

  Whichever callback the platform picks, Back either closes the keyboard (the IME's or branch 0) or reaches the sheet;
  it never pops a guarded destination without its guard, or the screen beneath an in-screen modal sheet. If QA2 shows
  on the phone that the IME always takes Back first, rule 2's branch 0 simply never runs; the record goes in §21.

### 5.9 Keyboard and insets

The sheet reads the keyboard through an internal `SheetImeSource` (C2-43): the inset this frame, the running
animation's target and whether it animates, all read in the sheet's placement. Production backs it with
`WindowInsetsRulers.Ime` (`current`, `getAnimation(scope).target`/`isAnimating`, [V] `WindowInsetsRulers.kt:48`, `:69`,
`:97`, `:231-250`), which only a placement scope can read; `SheetImeTest` backs it with a double driven by the test
clock, because ui-test 1.12.1 can inject only static insets (`DeviceConfigurationOverride.android.kt:406-424` [V]) and
dispatches no `WindowInsetsAnimation`. Placement writes a snapshot field `imeTargetVisible` only when it changes; B-37,
B-38, B-46 and B-47 read that field.

- **B-45 `Lift` (default).** In the placement block (API 30+), with `K` the IME inset and `bottomInset = H −
  restBottom` (the navigation bar, or the tab bar for a sheet resting on it): `lift = clamp(K − bottomInset, 0, y −
  largeTop)`; the sheet is placed at `y − lift`, so a Medium sheet rises with the keyboard frame by frame on the
  system's curve, keeps its height while it fits, and stops at `largeTop`. **Only while focus is inside the sheet**
  (an `onFocusChanged` on the sheet root writes a plain field the placement reads, C1-11): a keyboard opened by a
  field beneath a non-modal sheet leaves the sheet where it is [U: against iOS Maps, §21]. The footer's top is
  `max(min(restBottom, surfaceBottom, H − K) − footerHeight, y_v + headerBottom)`, with `surfaceBottom` from §6.3 and
  `y_v` the placed top: above the navigation area or the floating gap, on the keyboard when it is up, and never above
  the header's bottom (it rides the header out when the sheet leaves, as `AppSheet.kt:249-250` [C] does). The footer
  is placed with its own layer, so its per-frame offset is a RenderNode position (L0). Detent anchors exclude the
  keyboard, so drag math, `currentDetent` and `e` are unchanged: 0 anchor writes. L1, no recomposition.
- **B-45a Center and side panels** (C2-17). A Center form sheet rests at `safeTop + (safeHeight − height) / 2`. With
  the keyboard up it lifts in placement by `clamp(cardBottom − (H − K) + 16 dp, 0, cardTop − safeTop)`; past that the
  content box shrinks to the visible region (B-47) and the footer rides the keyboard, as iPadOS lifts its form sheet.
  A Start/End panel's bottom edge rides `H − K − side.inset` (L1) and its content box shrinks the same way. Both only
  while focus is inside.
- **B-46 `Expand`.** When the IME target becomes visible for a field inside the sheet, it animates to its tallest active
  detent on `springs.sheet`; when it hides, back to the detent it left, unless the person moved it meanwhile.
- **B-47 Content at the IME target.** While the IME target is visible, the content box is measured to the region
  actually visible at the target: from the lifted header's bottom to the keyboard's top, so `bringIntoView` aims at
  the real viewport (fixes D4). While the target is hidden, it is measured at Large's content height. That is one
  re-measure when the keyboard starts to open and one after it closes (≤ 2 per IME transition), plus ≤ 1 per settle
  while it is up. **This measure sizes the content box for placement only**: `contentHeight`, `headerHeight` and the
  anchors keep the keyboard-free values (R-3), so a `FitContent` form taller than the visible region (the Redmi: 720 dp
  of form, a 300 dp keyboard) neither shrinks its detent nor animates while the keyboard moves (C1-10).
- **B-47a Bringing a focused row into view** (C2-12). The content box carries a `BringIntoViewModifierNode`
  (`ui112/relocation/BringIntoViewModifierNode.kt:32` [V]). A request whose rect ends below the visible bottom (the
  footer's top, the keyboard's top or the window's bottom) first moves the sheet to the lowest active detent that
  reveals it, else the tallest, with `animateTo` (no haptic, no `onDetentChange`: not a person's settle), then forwards
  the request to its parent. For Resizes and Scrolls alike; never during a drag. So keyboard Tab past the last visible
  row at Medium expands the sheet instead of focusing a row below the window.
- **B-48 API 26-29.** No IME animation: the inset jumps. `lift` is animated toward the new value on `springs.screen`
  (MOTION-PLAN M22) by an `Animatable` that runs whenever the source is not animating and the value changed.
- **B-49** A drag of the sheet itself (not a content scroll) that moves 24 dp (`sheet.keyboard.dragHidesAfter`) while
  the IME is up hides the keyboard (`LocalSoftwareKeyboardController.hide()` from the drag callback) [D].
- **B-50 Insets are counted once.** The content box consumes `safeDrawing.only(Top)`, `navigationBars` and `ime`, so an
  `imePadding()`, `navigationBarsPadding()` or `statusBarsPadding()` inside returns 0 and an `AppTopBar` inside pads no
  status bar (QA-19). `contentPadding.start`/`.end` are the placement class's gutter, so body text keeps its gutter from
  the visible edge while floating and the rounded clip never cuts it (closes QA-17, `qa/r1/DEFECTS.md:41`; C2-23).
  While the keyboard is down, `contentPadding.bottom` = `footerHeight + max(bottomInset, floatingGap) + reach`, where
  `floatingGap` is `i₀` for a floating sheet (a constant at measure time) and `reach` is B-31's; while it is up,
  `footerHeight` (the box already ends at the keyboard, B-47). The footer itself is never padded by an inset: it is
  placed above `restBottom` or on the keyboard. Content without a footer therefore clears the navigation bar (fixes
  D3), and the IME is never counted in both the footer and the content (fixes D2). The sheet's frame is centred within
  `safeDrawing.only(Horizontal)` and is at most the safe width wide.
- **Forbidden inside sheet content** (docs and a code rule, §19.1 item 9): `Modifier.fitInside(WindowInsetsRulers.Ime…)`,
  `imePadding()`, `windowInsetsPadding(WindowInsets.ime)`. `fitInside` re-measures its child with fixed constraints in
  the placement block on every IME frame ([V] foundation-layout 1.12.1 `RulerAlignment.kt:36-50`).

### 5.10 Background interaction (non-modal)

- **B-51 The boundary** is `upThrough`'s offset `y_U` (R-13 when `upThrough` is inactive; no boundary when it is null:
  always undimmed). With `y_N` the offset of the next stop above it, the modal ramp is
  `m = clamp((y_U − y) / (y_U − y_N), 0, 1)`.
  - `m = 0` (at or below `upThrough`): no dim, and **the scrim child is not placed**: `SheetLayout`'s placement block
    decides it from a plain field, and hit testing skips an unplaced child (`ui112/node/InnerNodeCoordinator.kt:216`
    [V]). A touch around the sheet therefore misses the overlay box, which carries no pointer modifiers, and reaches
    the app content box beneath in the same frame. No recomposition, no node swap. What is beneath stays visible to
    screen readers. (Revision 1's `sharePointerInputWithSiblings()` on the scrim cannot work: sharing is checked per
    child LayoutNode at each level on that node's own modifier chain, and the overlay box above the scrim has none,
    `InnerNodeCoordinator.kt:215-244`, `NodeCoordinator.kt:1478-1491` [V]; C1-04.)
  - `m > 0`: the scrim is placed and blocks: taps are attempts or dismissals as for a modal sheet. The dim fades in
    with `m` (drawn per frame by the presenter, §6.4). iOS: "dimming still fades in if I resize to the large detent"
    (WWDC21 10063 [A]).
  - `blocking` (the settled or target stop above `upThrough`) is a snapshot field. The overlay host's `HideBeneathNode`
    reads it through the entry's modality (§11.1) and calls `invalidateSemantics()` when it flips, so beneath is
    hidden with 0 recompositions; B-42a moves focus in when it starts blocking.
  - **A pane-bounded sheet** (bounds smaller than the window, B-13) dims and blocks only its pane (C2-20): its scrim
    fills `LocalPaneBounds.rect` and draws the merged dim itself (one D2 rect over the pane, `S` at `a2 = m ·
    scrim.alpha`); hidden semantics go through `PaneBounds.block(this, blocking)`, which WP04 applies to that pane's
    subtree; the rail, the other pane and the bar stay usable and announced.
  - **An explicit Center non-modal sheet** is undimmed only when `upThrough` is null; with an `upThrough`, its one stop
    is above it, so it blocks as a modal sheet does (C2-29).
- **B-52 Above the tab bar** (C1-11, C2-52). With `aboveBottomChrome`, `restBottom` is the bar's settled top
  (`BarFrame.settledTop`, §4.10), which the shell writes from the bar's own expanded height and which never includes
  the keyboard. Revision 1's `LocalBottomChrome` is the keyboard while the keyboard hides the bar
  (`ds4/foundation/layout/Chrome.kt:27-31`, `nav4/shell/BottomChromeWriter.kt:45-55` [C]), so any keyboard would have
  re-resolved every detent. While the bar minimises on scroll (REQUIREMENTS 21), placement adds
  `(BarFrame.liveTop − settledTop) · (1 − e)`, so the resting sheet rides the bar in step (L1, no anchor writes). The
  surface's bottom edge moves from `restBottom` to the window bottom across `e` (§6.3), covering the bar at Large.
- **B-53** A non-modal sheet never drives the card stack: `recede` reads only modal sheets. A pane-bounded non-modal
  sheet's dim never reaches a presenter (B-51).

### 5.11 Stacking

- **B-54 Geometry.** A sheet presented while another is up stacks on it (an in-screen sheet opened from a sheet's
  content, a modal sheet opened beside a persistent one (B-54a), or a destination over a destination). The covered
  sheet, in its own layer (L0), scales by `1 − (1 − sheet.stack.coveredScale) · e_top` (to 0.9165) about its top
  centre and moves up by `sheet.stack.coveredLift · H · e_top` (0.005 H, 4.4 dp on the Redmi) [M] `sheet.dart:57-62`,
  `:84-90`, where `e_top` is the covering sheet's `expansionFraction`: as with the shell, a covering sheet at a partial
  detent (an action sheet, a Medium sheet) only dims the covered one, and the covered sheet recedes as the covering one
  reaches Large. The covering sheet's `largeTop` is the covered sheet's lifted top plus `sheet.stack.peek` (10 dp), so
  each level shows a 10 dp strip and no sheet reaches the status bar. The covered sheet draws the dim that reaches it
  (it is a presenter) with the same three-node order as `cardStackPresenter` (§6.4, F11): its recede layer, a draw
  node with the dim, a content layer with no properties around its surface and children. Top corners keep `R`
  (WWDC21: "the system will keep stacked corners looking consistent" [A]).
- **B-54a Levels by stack order** (C1-16, C2-06; D-33). A modal sheet's level is `max(owner level, the highest level of
  a visible, unparked sheet in its window) + 1`. So a Maps screen's action sheet, opened while its persistent sheet is
  up at level 1, takes level 2 and the persistent sheet is its presenter: covered (B-55), dimmed, hidden from
  accessibility, and receded only if the action sheet reaches Large. The dim of a modal sheet at level `L` is drawn by
  every presenter from `L − 1` down to the nearest level whose sheet is modal (which dims itself as that presenter), or
  down to the shell: here both the persistent sheet and the shell, one rect each, merged per §6.4. Back, Escape and
  focus go to the top of the stack order. A non-modal sheet keeps owner level + 1.
- **B-55** Only the top sheet takes drags, Back, Escape and focus. A covered sheet's grabber hides, its drag and its
  nested-scroll resizing are disabled, and it is hidden from accessibility while a modal sheet is above it.
- **B-56** At most `sheet.stack.maxCoveredDrawn` (2) sheets behind the front one are drawn; deeper ones get layer alpha
  0, which the renderer skips without an offscreen pass. Their compositions stay. Under an opaque sheet at a
  Large-equivalent stop, a covered sheet is also clipped to its visible band (§6.4).
- **B-57** When the top sheet leaves, the covered sheet un-recedes by the same fraction, so the two motions stay locked
  together. The HIG asks for one sheet at a time [A]; `AppSheet(item)` follows it, stacking stays possible.

### 5.12 Navigating from a sheet

- **B-58 From a presented sheet** (`ShellState.submit`, `nav4/ShellState.kt:116`, `:178` [C]), while the top presented
  entry is a sheet:
  - a key registered as `Presentation.Sheet` is pushed on the presented stack and stacks over it (B-54);
  - a `Presentation.Cover` key or an ordinary key: the presented sheets above the topmost non-sheet presented entry are
    popped as one stack change (they slide out in `onRemove()`), and the key is submitted as if they had not been there
    once the presentation gate settles (`NavGuard` holds it). A cover is presented; an ordinary key goes to the front
    stack (the tab, or the presented stack when a cover remains). So a cover never sits above a sheet (D-23), and
    `onRemove()`'s `isPresented` check (§11.2) stays as a guard only.
- **B-59 Inside a sheet.** `AppSheetNavHost` keys never reach `ShellState`. Its push and pop run on `springs.screen`
  with parallax `navigation.parallax` (0.333), clipped to the sheet, with `SheetBackHandler(enabled = backStack.size >
  1) { backStack.removeLastOrNull() }`. Each step gets its own slot-registry layer (§4.8): the uncovered step's header,
  footer, guard, Back handler and pane title win; during a transition both steps' headers and footers cross-fade with
  its progress. A `FitContent` sheet resolves to the **target** step's keyboard-free height when the transition starts
  and animates to it on `springs.sheet` (B-10). The inner Back is a key-style pop in v1 (no predictive tracking inside
  the sheet): this defers REQUIREMENTS 24 inside sheets and is listed for the user's sign-off in the sheet
  checkpoint's decisions (§19.6, §20). Fixes N5 and MASTER-PLAN A3.7.
- **B-60 From an in-screen sheet.** If its content navigates its own screen away, the owner is covered and the sheet
  parks (B-14), sliding down with the push. The docs tell features to close an in-screen sheet in the same event that
  navigates; the exit, the push and the presenter's un-recede then run together, which §16.1's `sheet_push_from` row
  measures.

### 5.13 Haptics

- **B-61** `AppHaptic.Threshold` (`GESTURE_THRESHOLD_ACTIVATE` on API 34+, `CONTEXT_CLICK` below;
  `ds/foundation/interaction/AppHaptics.kt:59-60` [C]) plays on the first frame a settle has ≤ max(1 px,
  `sheet.haptic.arrivalShare` (0.02) × its travel) left to a detent other than the one the gesture started on, played
  from the settle coroutine, never from composition; and on arrival after a grabber tap, Enter or Space. With
  `springs.sheet` (ζ = 1, ω = 18.48 rad/s), 2 % remains at ≈ 0.31 s, when the sheet visibly lands; 1 px of a 1081 px
  Medium → Large travel remains only at ≈ 0.50 s [D] (C1-24). At most one per settle: a retarget before arrival plays
  it once, at the final arrival. None on show, dismissal, attempts, code moves, B-47a moves or accessibility actions.
  `currentDetent` is written on the same frame. Perf builds emit a `sheet_arrival` trace section there (§16.3).

### 5.14 Process death and configuration

- **B-62**

| Case | Behaviour |
|---|---|
| Destination sheet, process death | `ShellState` restores the presented keys (`nav4/ShellState.kt` saver); `SheetSessions` restores each sheet's saved state (§11.2): `shown = 1` → snap to the detent, no slide; the presenter is receded from the first frame when the stop is Large-equivalent |
| In-screen sheet, process death | `visible` is the caller's (`rememberSaveable` or ViewModel state); `rememberAppSheetState` restores the detent; B-6 decides whether it shows |
| Activity recreated (night mode, locale, density or font scale change; WP04's manifest keeps the activity through rotation, resize, fold and a keyboard, `app4/AndroidManifest.xml:57-64` [C]) | as process death |
| Resize without recreation (rotation, freeform, fold, unfold, split) | B-10 |
| Restored detent no longer listed or active (it was Medium, now Compact height) | the nearest active detent by height |
| Content state | the content's own `rememberSaveable`, which holds because the overlay host keys entries by call site and composes them in the call site's `SaveableStateHolder` (§11.1, D-40); the slot providers do the same (§4.8) |

### 5.15 Reduce motion and transitions

- **B-63** (C2-08, C1-22; D-35). Reduce motion is `ANIMATOR_DURATION_SCALE == 0` (`ds/theme/ReduceMotion.kt:41-42`
  [C]), and Compose's `MotionDurationScale` follows the same setting (`ui112/platform/WindowRecomposer.android.kt:97`,
  `:427-446` [V]): every suspend animation finishes in one frame under it. The sheet **honours** it rather than
  overriding the person's choice:

| Motion | `Slide` (default) | Reduce motion ("Remove animations") | `SheetTransition.CrossFade`, normal scale |
|---|---|---|---|
| Present | slide on `springs.sheet` | at its detent in one frame; dim at its end value | fade of the sheet layer with `CompositingStrategy.ModulateAlpha` at its detent, in over `fades.longMs` (200 ms) with `fades.enterEasing`; the dim fades with it |
| Dismiss | slide | gone in one frame | fade out over `fades.shortMs` (120 ms) with `fades.exitEasing` |
| Card stack | scale, translation, corners, overlay | end values in one frame; no scale or translation at all, overlay and dim only | follows the fade |
| Drag | follows the finger | follows the finger | follows the finger |
| Settle, rubber-band release | springs | snaps to the target in one frame | springs |
| Back gesture | shrink | the shrink follows the finger; the commit removes the sheet in one frame | shrink; the commit fades |
| Floating to attached | follows `e` | follows `e` (it follows the finger or a snap) | follows `e` |
| Form sheet | slide | in one frame | fade |
| Glass | sampling | sampling at rest, as usual; nothing fades | a Glass sheet draws Frosted while its layer alpha < 1: never a layer alpha around a sampling surface (`handoff/WP05.md:230`) |

  The whole sheet layer fades (`ModulateAlpha`: no offscreen buffer; the shadow is outside-only, §7.7), never only the
  content: two implementers must build the same fade (C1-22). Tests set the real setting through
  `Settings.Global.ANIMATOR_DURATION_SCALE` in Robolectric, not only `AppTheme(reduceMotion = true)`
  (`ds/theme/AppTheme.kt:88`, `:103-104` [C]), which leaves the duration scale at 1. **Overrides** MOTION-PLAN §1 rule
  5's 150 ms, which is not a token (`tokens/motion.json:31` holds the fades).

### 5.16 Frame rate

- **B-64** (C1-23). `preferredFrameRate` propagates to every child layer whose own rate is 0
  (`ui112/FrameRate.kt:115-145` [V]), and each such layer votes whenever its display list updates
  (`GraphicsLayerOwnerLayer.android.kt:274-277` [V], live from API 35, `AndroidComposeView.android.kt:364-365`), so an
  always-on vote would let a caret or a spinner in a resting sheet hold 120 Hz. Placement changes already vote High
  (`NodeCoordinator.kt:450-451` [V]), which covers drags, settles, the lift and parking, and the presenter's recede,
  which only ever moves while the sheet's placement does. The one layer-only motion is the back gesture's shrink. So
  the sheet layer votes `preferredFrameRate(FrameRateCategory.High)` only while back progress is moving, and casts no
  vote (`0f`) otherwise (SMOOTHNESS S10); `cardStackPresenter` needs no vote of its own. A `snapshotFlow` flips a
  boolean at the start and the end of the gesture; the flip recomposes only the sheet's small layer wrapper (never
  its content), at most twice per gesture: 1.12.1 offers only value-taking modifiers, no setter outside composition
  (`ui112/FrameRate.kt:52`, `:71`, its node internal at `:96` [V]). Verified on an API
  35+ device with `dumpsys SurfaceFlinger` or the Perfetto frame-rate track: a resting sheet with a spinner casts no
  High vote (§21).

---

## 6. Motion and tokens

### 6.1 Springs

All from `AppTheme.springs`: `AppSpring(response, bounce)`, `k = (2π / response)²`, `ζ = 1 − bounce`
(`ds/theme/AppMotion.kt:61-77` [C]); travelling springs are divided by the motion style's speed (Calm 0.87, Snappy 1.15,
`tokens/motion.json:3-6`). Offsets in px animate on the spring's `.pixels` spec (visibility threshold 0.5 px,
`AppMotion.kt:72` [C]); 0-to-1 values (`parkProgress`, a back-progress residual) on `.unit`, whose 0.001 threshold is
about a pixel of a full-height slide.

| Use | Token | Standard | Calm / Snappy | Source |
|---|---|---|---|---|
| Show, hide, detent settle, programmatic move, stretch release, park, `Expand`, covered-sheet recede, card stack (it rides the sheet; no spring of its own) | `springs.sheet` | 0.34 s, bounce 0 (k 341.5, ζ 1) | 0.3908 s / 0.2957 s | CP1 keep (`decisions/CP1.md:40`, `:137`) |
| Action and choice sheet arrival, anchored card | `springs.dialog` | 0.275 s, bounce 0 (k 522) | ÷ 0.87 / ÷ 1.15 | CP1 (`:41`), M17 |
| Keyboard lift on API 26-29; push and pop inside `AppSheetNavHost` | `springs.screen` | 0.344 s, bounce 0 (k 333) | ÷ speed | M22; CP1 (`:32`) |
| Cross-fade (normal duration scale only, B-63) | `fades.longMs` / `fades.shortMs` | 200 / 120 ms, enter/exit easings | — | `tokens/motion.json:31` [C] |

No sheet motion bounces (CP1: "no bounce on anything that moves a full screen"). Every animation is seeded with the
velocity it inherits (release, retarget): `anchoredDrag` retargets with `lastVelocity` (androidx does the same,
`src/SheetDefaults.kt:316-339` [G]), which fixes D7. The only fixed-duration animations are the fades. `springs.sheet`
on the Redmi: 55 % of the travel at 100 ms, 88 % at 200 ms, 98 % at 315 ms (the arrival haptic, B-61), 99 % at 359 ms
[D]. Under "Remove animations" every one of them finishes in one frame (B-63).

### 6.2 The `sheet` token section

New top-level section in `tokens/motion.json` (beside `springs`, `scroll`, `fades`). Lengths in dp, speeds in dp/s,
`release.dismissFlick` in bound heights (or, for a modal side panel, widths) per second, `stretch.aboveLarge` and
`stack.coveredLift` as fractions of H. The token writer (`generator/genkit/tokens/kotlin.py`, the Multiplatform
writer) emits it as the internal `DesignTokens.Sheet` for Android and Compose Multiplatform. The Swift writer
(`swift.py`) emits only what the SwiftUI template reads: `springs.sheet` and `radii.sheet` (C2-45, §18).

```json
"sheet": {
  "detent":      { "mediumFraction": 0.5, "largeTopGap": 10, "mediumMinGap": 48, "minHeight": 48 },
  "release":     { "projectionSeconds": 0.099, "detentFlick": 400, "dismissFlick": 2.0, "dismissPosition": 0.48 },
  "stretch":     { "aboveLarge": 0.008 },
  "grabber":     { "width": 36, "height": 5, "top": 5, "touchTarget": 48, "touchBand": 14, "alpha": 0.45,
                   "hoverAlpha": 0.7, "contrastAlpha": 0.7, "contentTop": 15 },
  "header":      { "top": 14, "minHeight": 44, "scrollEdgeHeight": 24, "scrollEdgeRamp": 8 },
  "keyboard":    { "dragHidesAfter": 24, "formLiftGap": 16 },
  "form":        { "heightFraction": 0.85, "maxHeight": 620 },
  "side":        { "inset": 16, "backShrink": 24, "backGrow": 12, "backY": 48 },
  "cardStack":   { "scale": 0.9165, "corner": 12, "deviceCornerFactor": 0.9, "deviceCornerMin": 20,
                   "overlayAlpha": 0.10, "overlayColor": { "light": "#000000", "dark": "#C8C8C8" }, "statusBarFlip": 0.5 },
  "stack":       { "coveredScale": 0.9165, "coveredLift": 0.005, "peek": 10, "maxCoveredDrawn": 2 },
  "back":        { "shrinkX": 48, "shrinkY": 24, "easing": [0.1, 0.1, 0.0, 1.0], "guardedShare": 0.2,
                   "persistentFollow": 24, "formScale": 0.90 },
  "haptic":      { "arrivalShare": 0.02 },
  "frosted":     { "bodyAlpha": { "light": 0.92, "dark": 0.94 } },
  "glass":       { "opaqueAt": 0.5, "blur": 16, "bodyAlpha": { "light": 0.55, "dark": 0.50 } },
  "rim":         { "solidDarkAlpha": 0.08 },
  "shadow":      { "blur": 20, "y": 6, "alpha": 0.10, "faintScrimAlpha": 0.2 },
  "actionSheet": { "rowHeight": 56, "maxFitFraction": 0.6, "cardWidth": 320 },
  "choice":      { "maxFitFraction": 0.6, "searchFrom": 6 }
}
```

`tokens/styles.json` gains two fields per design style (judge-1 graft 10, judge-3 G9):

| Style | `sheetInset` (dp) | `sheetMaterial` |
|---|---|---|
| Utility | 8 | `Solid` |
| Social | 8 | `Solid` |
| Editorial | 0 (square sheets read wrong floating inside rounded display corners) | `Solid` |
| Playful | 10 | `Solid` |
| Glass (WP05, `requires = ["liquidglass"]`) | 8 | `Glass` |

`radii.sheet` already exists (24 / 30 / 8 / 28 dp, `tokens/styles.json:5`, `:22`, `:39`, `:56` [C]). The inset values are
**taste**.

| Token | Value | Meaning | Source |
|---|---|---|---|
| `detent.mediumFraction` | 0.5 | Medium's top at half the bounds | [A] "approximately half"; WP03 0.55 (`AppSheet.kt:335` [C]); **taste** |
| `detent.largeTopGap` | 10 dp | Large's top below `safeDrawing.top` | WP03 (`AppSheet.kt:343` [C]); iOS ≈ 0.01 H [M] |
| `detent.mediumMinGap` | 48 dp | Medium is inactive within this of Large | [D] |
| `detent.minHeight` | 48 dp | lower clamp for any detent; `Peek` without a header | [D] |
| `release.projectionSeconds` | 0.099 s | `v · r / (1 − r) / 1000` at r = 0.99/ms | WWDC18 803 [A]; `AppSheetState.kt:216-219` [C] |
| `release.detentFlick` | 400 dp/s | a flick moves one detent | [D] **[U]: QA2 tunes it (§21)** |
| `release.dismissFlick` | 2.0 H/s | dismiss velocity from the lowest detent (W/s toward the edge for a modal side panel) | [M] `sheet.dart:76-78` |
| `release.dismissPosition` | 0.48 | share of the lowest-to-hidden travel at the release position that dismisses | [M] `sheet.dart:1071`, `:1110-1119`: the sheet stays while its controller, 1 when shown and falling as it is dragged, is above 0.52. Revision 1 and `research.md:278` had it inverted as a projected 0.52 (C1-24) |
| `stretch.aboveLarge` | 0.008 H | rubber-band asymptote past Large | [M] `sheet.dart:27-37` (0.08 → 0.072 H) |
| `grabber.*` | 36 × 5 dp, 5 dp top, 48 dp semantic target, 14 dp touch band, α 0.45, hover α 0.7, contrast pill white α 0.7, content 15 dp | | [M] `sheet.dart:704-724`; WP03 α (`AppSheet.kt:302` [C]); 48 dp [G]; band and contrast pill [D] (§7.3) |
| `header.*` | 14 dp top, 44 dp min, 24 dp fade, full after 8 dp of scroll | | WP03 (`AppSheet.kt:306` [C]); iOS bar height |
| `keyboard.*` | 24 dp drag hides it; 16 dp gap above the keyboard for a lifted form sheet | B-49, B-45a | [D] |
| `form.*` | ≤ min(0.85 H, 620 dp) | Center placement height cap | WP03 (`AppSheet.kt:227`, `:337` [C]); ADAPTIVE-PLAN:434 |
| `side.*` | 16 dp gap of a panel; back: shrink 24, grow 12, y 48 dp | §8, B-39 | M3 detached side sheet [G]; MDC side container (`research/predictive-back/SPEC.md:80`) [G] |
| `cardStack.*` | 0.9165 scale; corners `0.9 ×` display corner (if ≥ 20 dp, else 0) → 12 dp; overlay α 0.10, black / `#C8C8C8` | | [M] `sheet.dart:14-25`, `:64-74`, `:84-90`, `:327-345` |
| `cardStack.statusBarFlip` | 0.5 | light status icons past half the recede | [D] **taste**: Flutter keeps light icons for the whole transition (`sheet.dart:361-368`); a flip at 0.5 keeps dark icons readable over a barely receded light screen (C1-24) |
| `stack.*` | 0.9165, 0.005 H, 10 dp peek, 2 drawn | B-54, B-56 | [M] `sheet.dart:57-62`; peek [D] |
| `back.*` | 48 / 24 dp; `(0.1, 0.1, 0, 1)`; guarded 0.2; persistent 24 dp; form 0.90 | | [G] MDC `motion_dimens.xml:23-24`, SPEC §2; guarded and persistent [D]; form: MDC main container minimum [G] |
| `haptic.arrivalShare` | 0.02 | the arrival haptic fires with this share of the settle left | [D] (B-61, C1-24) |
| `frosted.bodyAlpha` | 0.92 / 0.94 | Frosted body at partial detents | `glass/GlassDefaults.kt:57-67` [C] |
| `glass.*` | opaque by e = 0.5; blur 16 dp; tint 0.55 / 0.50 | | [A] WWDC25 323; values **[U]: CP2 decides** |
| `rim.solidDarkAlpha` | 0.08 | hairline in dark theme | [D] |
| `shadow.*` | blur 20 dp, y 6 dp, black α 0.10, light only; a modal Bottom sheet draws it when its scrim's alpha < 0.2 | §7.7 | [D] between WP05's chrome (16/4/0.10) and content (10/3/0.08) shadows, `glass/GlassDefaults.kt:84-85` [C] |
| `actionSheet.*`, `choice.*` | rows ≥ 56 dp; 0.6 cap; 320 dp card; search from 6 | | WP03 (`AppActionSheet.kt:197` [C]); VitFleet `ChoiceSheet.kt` |

### 6.3 Floating ↔ attached

Let `a = e`, `fl` = 1 when the sheet floats (Bottom placement, `i₀ > 0`, a partial stop active, gesture navigation
(`WindowInsets.tappableElement.bottom == 0` [G]), height not Compact), else 0. Values below are in **the sheet layer's
own coordinates** (its top-left at the placed `y_v`), because that is what its draw sees; they are read in draw or
placement.

| Quantity | Value (layer-local) |
|---|---|
| Side inset | `i₀ · fl · (1 − a)` |
| Resting surface bottom `b₀` (window) | window-anchored: `H − i₀` floating, `H + R` attached; above the bar: `restBottom − i₀ · fl` |
| Surface bottom | while `fl · (1 − a) > 0`: `lerp(b₀, H + R, a) − y_v` (it changes with `y_v`: the floating case re-records, D1). While `fl · (1 − a) = 0` (attached: Large-equivalent, 3-button navigation, Compact height, no partial stop, or `i₀ = 0`): the **constant** `layerHeight + R`, the layer being measured to reach the bounds' bottom from the highest stop, so whatever lies below the window is cut by the window |
| Corners | uniform `R` on all four |
| Content clip | the same round rect: `(i, 0, W − i, surface bottom)`, radius `R`; constant while attached |
| Header | own layer, inside a constant side inset `i₀ · fl` (D-7) |
| Grabber | own layer (the grabber node), constant offset |
| Footer | own layer; B-45's top minus `y_v`, applied as the layer's position (L0) |
| Body alpha (Frosted, Glass) | `lerp(bodyAlpha, 1, min(a / glass.opaqueAt, 1))` |
| Shadow (where allowed, §7.7) | non-modal Bottom: `alpha · (1 − a)`; Start/End, Center, cards: constant |

The change is continuous and the outline stays a **uniform** round rect throughout. **Costs** (C1-02): an attached
sheet's drag, settle or lift moves one RenderNode (L1) and re-records nothing: its body, rim and clip are a constant
local rect, and its grabber, header, content and footer are their own layers whose positions change at most. A floating
sheet's surface node re-records one round rect, the rim and the clip per frame (D1) while `fl · (1 − a) > 0`, because
its height is `surface bottom`; its children are not re-recorded. The round rect is rebuilt (a rewound `Path`) only
then. The shadow never follows the size (§7.7).

### 6.4 Card stack and the merged dim

The presenter at level `L` reads `e = stack.recede(L)`: the `expansionFraction` of the sheet directly above it when that
sheet is modal, Bottom-placed, has a Large-equivalent stop active, `recedesPresenter` is true, the **window width is
Compact** and the height is not Compact; otherwise 0, and the presenter is only dimmed (D-9; C2-22: iPadOS never
shrinks a desktop-width window, and a SurfaceView behind would not follow the layer's transform [U], §21).

**Node order** (C1-01, F11). `Modifier.cardStackPresenter(stack, level)` is three nodes, outermost first:

1. **recede layer**: `graphicsLayer { scaleX; scaleY; translationY; transformOrigin = (0.5, 0) }`, no shape, `clip =
   false`, reading `recede(L)` in the layer block (L0);
2. **dim-and-clip draw node**: `clipRect(0, 0, w, stack.visibleBottom(L))`, then the presenter's corner as a draw-time
   round-rect clip (the reused `Path` of §7.1) while `e > 0`, then `drawContent()`, then `drawRect(dim(L))` when its
   alpha is > 0. It reads `dim`, `recede` and `visibleBottom` in draw;
3. **content layer**: `graphicsLayer()` with no properties, around the presenter's content.

A frame of sheet motion re-records only node 2's display list (a clip, a `drawLayer` of node 3's RenderNode and a
rect) and moves node 1's RenderNode. The content layer is never re-recorded, so the shell's inline nodes are never
re-run and, on the glass presets, `RecordSceneNode` (`glass/SceneBackdrop.kt:157-161` [C], which re-records the tab
scene whenever it draws) does not draw: the bar's live blur is not re-rendered by sheet motion. WP03's uncommitted
presenter draws the dim inline after `drawContent()` with no layer beneath (`wip/stack/CardStackPresenter.kt:35-36`,
`:110-128`) and is corrected before commit. A covered sheet uses the same order (B-54).

| Quantity | Value | Phase |
|---|---|---|
| Scale | `1 − (1 − cardStack.scale) · e` about `(0.5, 0)` | node 1 |
| Translation Y | `safeDrawing.top · e`: the presenter's top meets the safe area, 10 dp above the sheet's top | node 1 |
| Corners | `lerp(R₀, cardStack.corner, e)`, uniform; `R₀ = deviceCornerFactor ·` the display's top corner radius (`WindowInsets.getRoundedCorner`, API 31+) when that is ≥ `deviceCornerMin`, else 0. A draw-time clip in node 2, so no outline changes: a changing `Shape` with `clip = true` would invalidate the shell's semantics on every px step (~49 on the Redmi, 81 → 33 px; `ui112/node/NodeCoordinator.kt:640-646` [V]). Fallback if the draw-time round-rect clip costs an offscreen pass (§21): node 1 takes an outline clip quantised to at most 8 steps between `R₀` and 12 dp, shapes held in an 8-entry array | node 2 |
| Visible band | at `e = 1` with the sheet's body opaque (Solid, or Frosted/Glass past `opaqueAt`), `visibleBottom(L)` is the sheet's top in presenter coordinates plus `R`: HWUI has no occlusion culling, so without it every scroll, drag or IME frame near Large redraws the whole receded presenter under an opaque sheet for a 10 dp strip (C1-19). Otherwise the full height | node 2 |
| Merged dim | below | node 2 |
| Backing | §7.9 | — |
| Status bar | light icons while `stack.rootReceded` (light theme) | ≤ 1 change per crossing (§7.9) |
| Reduce motion | no scale, translation or corners; the dim stays | — |

A layer scale or translation change is not free in Compose: `updateLayerParameters` calls `onLayoutChange` for
accessibility and content-capture bookkeeping and requests positioned callbacks for observers in the subtree
(`ui112/node/NodeCoordinator.kt:612-680`; `ui112/platform/AndroidComposeView.android.kt:2187-2190` [V]). The shell keeps
positioned observers off its hot path: WP05's `SceneBackdrop` invalidates its samples on the scene-to-surface offset
(`scene.localPositionOf(surface)`), not on `positionInRoot()` (`glass/SceneBackdrop.kt:52`, `:58-61` [C]), so a
transform shared by the stage and the bar invalidates nothing (§11.5; C1-14). §16.1's `glass_live` column checks it.

**The merged dim** (D-8; judge-1 graft 7, judge-2 graft 29). Each presenter draws one rect that is the exact composite
of its card overlay (part of the presenter, drawn first) and every scrim that reaches it (B-54a), drawn over it in
stack order. For two layers:

```
u  = pf (modal) or m (non-modal ramp, B-51)          l  = e (0 when the card stack does not apply)
a1 = cardStack.overlayAlpha · l                      a2 = scrim.alpha · u
A  = 1 − (1 − a1) · (1 − a2)                         // the combined alpha
K  = (O · a1 · (1 − a2) + S · a2) / A                // per channel; O = overlay RGB, S = scrim RGB
drawRect(color = K, alpha = A)                       // skipped when A == 0
```

With more than two layers (a modal sheet over a non-modal sheet whose ramp is up), the same "over" step folds each
further scrim `(S_i, a_i)` into `(K, A)` in stack order: `A' = 1 − (1 − A)(1 − a_i)`,
`K' = (K · A · (1 − a_i) + S_i · a_i) / A'`. `A` never decreases as a sheet rises (F6). One D2 rect per presenter in both
themes; a `Color` returned from a non-generic function is a `long`, so nothing is allocated. Worked with `colors.scrim`:
light at Medium `A = 0.40`, at Large `1 − 0.9 · 0.6 = 0.46`; dark at Medium 0.56, at Large `1 − 0.9 · 0.44 = 0.604`.
With no presenter at that level (a preview, a catalog page without the shell), the sheet's scrim node draws `S` at
`a2` itself. The dim covers the whole presenter, status bar and tab bar included (QA-13). Its strength against iOS 26
is [U] (§21). `SheetStack` computes `recede`, `dim` and `visibleBottom` with index loops over its fixed-capacity array
(C1-20).

### 6.5 Stacked sheets

B-54 to B-57. The covered sheet's transform is in its sheet layer (L0), read from the stack; its dim is drawn by the
draw node between that layer and its content layer (F11).

### 6.6 Glass

| Tier (`LocalGlassQuality`, `glass/GlassQuality.kt:24-48` [C]) | `SheetMaterial.Glass` at partial detents, presenter = the shell, no cover up | Across the expansion | At a Large-equivalent stop |
|---|---|---|---|
| Live, LiveNoLens (`samplesScene`, `:42-43`) | live backdrop through the `SheetBackdropHolder`: blur `glass.blur`, the presenter's dim, tint at `glass.bodyAlpha` | tint `lerp(bodyAlpha, 1, min(e / 0.5, 1))`; the sampling pass is skipped in draw once the tint is 1 | opaque `container`, no sampling |
| Frosted | Frosted | body alpha → 1 by `e = 0.5` | opaque |
| Solid (Reduce transparency) | Solid | — | — |

- **Only over the shell** (C1-13). The recorded scene holds only the tab content (`glass/GlassStage.kt:47-50` [C]):
  covers and other sheets live in the hosts. A Glass sheet whose presenter is not level 0 (stacked on a sheet, opened
  from a destination sheet's content, over a cover), or while any cover is up, draws Frosted. The decision is
  `stack.samplesScene(level)`, read in draw through `SheetFrame.opaque` (1 when it must not sample), so it changes no
  composition.
- **The bar inside the glass.** The bar is in the stage's chrome slot (`GlassStage.kt:51-53` [C]), not in the scene,
  so a floating Glass sheet over the bar would show content without the bar inside its body while the real bar shows
  through the gap below it. `GlassSheetBackdrop` draws the scene's recording, then the bar's own recording (the chrome
  records itself into a `GraphicsLayer` it also draws, as `RecordSceneNode` does), under the blur. If WP05 cannot
  record the chrome without re-recording it per frame, the gap is documented and CP2's recording checks it (§11.5).
- Without the liquid glass feature (lean presets), the holder's backdrop stays null and Glass draws Frosted; the
  backdrop file is not generated (WP01 marker).
- Never a layer alpha around a sampling surface; Kyant `effects` read no state; the tint and dim animate in
  `onDrawSurface` (`handoff/WP05.md:230`; `glass/GlassChrome.kt:47-54` [C]).
- The sampled scene excludes the presenter's dim, so the backdrop draws `SheetFrame.dim` over the sample (judge-3 G8).
- The card stack scales the presenter while the sheet samples it between `e = 0` and `0.5`. WP05 verifies alignment on
  the phone; if sampling cannot follow the presenter's transform within 1 px, `glass.opaqueAt` becomes the first frame
  the presenter recedes (the body is opaque whenever `e > 0`). While `recede(0) > 0` the bar may draw its last sample
  or Frosted, since it is dimmed beneath a modal sheet anyway.
- A Glass sheet whose only detent is Large is opaque at rest; the §4.1 example lists Medium for that reason (C2-54).

---

## 7. Visuals

### 7.1 Shape and clip

- **One uniform radius `R`** (`AppSheetStyle.cornerRadius`, the style's `radii.sheet`) on all four corners, so the
  outline is a simple round rect, which the renderer clips without an offscreen buffer on every API [V]
  (`OutlineResolver.android.kt:263` uses `setRoundRect` only when the round rect is simple; any other shape takes the
  path branch at `:282-288`, a canvas clip below API 33).
- **Attached = the bottom corners below the window** (§6.3), not a different shape.
- **Circular corners**, not continuous ones: a continuous corner is a path (`glass/GlassDefaults.kt:17-22` [C]); at
  24-30 dp the difference is about a pixel of curvature [D].
- **Clip mechanism.** The content layer is clipped to the surface rect in the sheet layer's draw, before
  `drawContent()`: a reused `Path` rebuilt with the platform's float-argument `addRoundRect(l, t, r, b, rx, ry, dir)`
  (Android `android.graphics.Path`), so nothing is allocated per frame. In the sheet layer's own coordinates the rect
  is **constant while the sheet is attached** (§6.3), so an attached drag re-records nothing, and the path is rebuilt
  only while the sheet floats. A single round rect is clipped analytically [U: Perfetto shows no `saveLayer` and no
  stencil pass during a drag, §21]. **Fallback** if it does: a constant-size layer outline (round rect, hardware clip)
  cached per whole px of side inset in an array indexed by px (at most 23 entries for 8 dp at 2.75; never a
  `HashMap<Int, Shape>`, which boxes keys above 127, C1-20), its bottom R below the window: the floating bottom gap is
  dropped and the sides keep floating.
- **Center and Start/End:** the same radius, all four corners visible, constant shape.

### 7.2 Width and what the content is measured at

Every metric here comes from `AppLayout.forClass(placementClass)` (`ds/foundation/adaptive/AppLayout.kt:41-49` [C]),
resolved inside the sheet: the window class for a modal sheet, the pane's class for a non-modal one (D-20, B-13). Never
from `AppTheme.layout` in the caller's composition, which is the caller's pane's (`ds/theme/AppTheme.kt:110-111` [C]):
a modal form sheet opened from a Compact list pane on a Large window is 600 dp with 32 dp gutters, not 540 dp with 16
(C2-02). A caller's `AppSheetStyle.maxWidth` caps the result.

| Placement | Sheet width | Content measured at |
|---|---|---|
| Bottom, Compact width | the safe width, ≤ `sheetMaxWidth` (640 dp, `AppLayout.kt:65` [C]), centred | the sheet width (the floating inset is a clip; `contentPadding` carries the gutter) |
| Bottom, Medium width and up (non-modal, or forced) | `min(640 dp, W − 2 · gutter)`, centred | the sheet width |
| Bottom, Compact height | `min(safe width, 640 dp)`, centred; attached | the sheet width |
| Center (form sheet) | `min(formSheetWidth, W − 2 · gutter)` (540 / 600 dp) | the sheet width |
| Start / End, non-modal | `supportingPaneWidth` (360 / 412 dp) | the sheet width |
| Start / End, modal (§8) | `supportingPaneWidth`, full safe height | the sheet width |

The content column is measured with `maxHeight` = Large's height and `minHeight = 0`; the content box is
`weight(1f, fill = false)`, so a lazy list fills it and a short form wraps (material3 measures the same way,
`src/BottomSheet.kt:299-307` [G]). Its keyboard-free height is `FitContent`'s. `contentPadding` carries the gutter at
start and end (B-50), so body text keeps the app's gutter from the sheet's edge at Large and sits `i₀` closer to the
visible edge while floating, still `gutter − i₀` clear of the clip; nothing is clipped (REQUIREMENTS 34). Full-bleed
rows (an image, a divider) ignore it.

### 7.3 Grabber

| Property | Value |
|---|---|
| Size and place | 36 × 5 dp, fully rounded, centred, 5 dp from the sheet's top [M] `sheet.dart:704-724` |
| Colour | `colors.grabber`, read by `AppSheetGrabber` from the enclosing sheet's style through the slot registry: `contentTertiary` at α 0.45 (≈ iOS `tertiaryLabel`; `AppSheet.kt:302` [C]); with high-contrast text (API 36): `contentSecondary`, opaque. With `contentUnderGrabber`, a contrast pill: white at `grabber.contrastAlpha` (0.7) over a 1 dp black α 0.2 edge, readable over any image |
| Touch target | horizontally the pill's centre ± 24 dp; vertically only the band above the header's top, 0 to 14 dp (`grabber.touchBand`), so it never takes the top of a header's search field or segmented control and never reaches above the sheet into the dim or the map (C2-30). A drag starting in the band drags the sheet; a tap cycles detents. The 48 × 48 dp rect centred on the pill stays its semantic and focus bounds |
| Owner | the sheet's grabber node: hit-testing, focus, click, hover, semantics, visibility. A custom `grabber` lambda only draws inside it (C2-31) |
| Hover (mouse) | α 0.7 and `PointerIcon.Hand` |
| Keyboard focus | focusable; a 2 dp ring in `colors.focusRing` around a 48 × 24 dp rounded rect |
| Contrast | 1.9:1 on `surfaceElevated` in light theme and 2.1:1 in dark [D, `ds/theme/tokens/DesignTokens.kt:348`, `:356`, `:391`, `:399`], below WCAG 1.4.11's 3:1. Accepted as iOS's `tertiaryLabel` pill is, because the grabber is never the only way: the whole surface drags and the actions are in the accessibility menu. The API 36 high-contrast path is opaque `contentSecondary`; `SheetA11yInstrumentedTest` suppresses only that ATF contrast check on `sheet_grabber`, with this reason (C2-50) |
| Shown | §4.8 slot rules |
| Test tag | `sheet_grabber` (resource-id contract, `handoff/WP04.md:248`) |

`ContentTop` (15 dp) applies only when a grabber is drawn and there is no header: content then starts 15 dp below the
surface's top. With no grabber and no header, content starts at the top. With `contentUnderGrabber`, content starts at
the surface's top whatever the grabber, and the grabber draws over it (C2-26).

### 7.4 Header

- Pinned above the content, never over it: a caller who forgets padding never loses content under it (REQUIREMENTS 34).
- `AppSheetHeader`: starts 14 dp below the sheet's top; min height 44 dp (`heightIn`, never fixed); title in
  `typography.headline`, `contentPrimary`, centred, one line (up to two at font scale ≥ 1.5, then the row grows),
  ellipsized, marked `heading()`; subtitle in `footnote`, `contentSecondary`. The String overload delegates to the slot
  overload.
- A three-slot `Layout` measures `leading` and `trailing` first and reserves `max(leadingWidth, trailingWidth)` on both
  sides of the title (fixes D5, `AppSheet.kt:306-325` [C]). RTL mirrors the slots.
- `AppSheetCloseButton`: `AppIconButton` in its Tinted Small size with the close glyph and `designsystem_close`, 48 dp
  target, as today (`AppSheet.kt:316-326` [C]); in the Frosted and Glass materials its circle is drawn frosted. It
  emits nothing while the person cannot close the sheet (`dismissible = false` and no enabled guard), so a persistent
  sheet's default header shows no dead button (C2-49). Text actions are `AppButton` text variants; Done is semibold.
- **Scroll-edge effect:** while `contentScrolled > 0`, the header draws a 24 dp gradient of the container colour over
  the content's top, alpha = `contentScrolled` (full after 8 dp of scroll), read in draw in the header's own layer (M27,
  `ds/foundation/scroll/ScrollEdge.kt`). No hairline.
- Background: transparent, on the sheet's surface.

### 7.5 Footer

- Placed per B-45 with its own layer, so on a short detent it never rides over the header.
- Draws on the sheet's surface; `AppBottomActionBar` inside a sheet paints no background and keeps its top hairline.
- Never padded by an inset: the sheet places it above the navigation area, or on the keyboard.
- Not pinned while header and footer together exceed half the visible height; then it scrolls at the content's end
  (§4.8).

### 7.6 Materials, light and dark

| Element | Light (Cool palette) | Dark (Cool palette) |
|---|---|---|
| Container | `surfaceElevated` #FFFFFF | `surfaceElevated` #1D232E (`ds/theme/tokens/DesignTokens.kt:237`, `:280` [C]) |
| Container brush (optional) | `containerBrush` painted over the container where the body is opaque | same |
| Frosted body (partial) | container α 0.92 + rim + sheen | container α 0.94 + rim + sheen |
| Glass tint (partial) | α 0.55 [U, CP2] | α 0.50 [U, CP2] |
| Rim | none (Solid); white α 0.85 (Frosted, Glass) | white α 0.08 hairline (Solid); white α 0.20 (Frosted, Glass) |
| Shadow | black α 0.10, blur 20, y 6, where §7.7 allows | none |
| Grabber | `contentTertiary` α 0.45 | `contentTertiary` α 0.45 |
| Dim | `colors.scrim` `0x660B0F1A` | `colors.scrim` `0x8F000000` (`DesignTokens.kt:249`, `:292` [C]) |
| Card overlay | black α 0.10 | `#C8C8C8` α 0.10 |
| Backing behind a receded presenter | black | black |
| Status bar icons over the card stack | light past `e = 0.5` | light |

Every palette and design style reads the same roles, so Warm, Paper and Cream follow without code. `groupedColors()`
uses `backgroundGrouped` behind `surfaceGrouped` rows, as iOS's grouped sheets.

The surface is its own `SheetSurfaceNode` (not `glassSurface`): it draws the body as one `drawRoundRect` of the
current rect, the container brush (a `Brush` whose shader is cached by width and radius), the rim as a stroke, and
Frosted's brushes; brushes and strokes are cached by width and radius, never by height (the rim gradient spans a fixed
distance from the top). WP05's `glassSurface` rebuilds its draw cache on every size change (`glass/GlassSurface.kt:81`,
`:97-121` [C]) and is not used for sheets.

### 7.7 Shadow

The pull-to-refresh black ring (REQUIREMENTS 29) came from a shadow seen through a fading surface; the sheet's drag
would add a CPU blur per frame (D-18). The rules:

1. **Who.** Non-modal Bottom sheets, Start/End panels, Center form sheets and anchored action-sheet cards, in light
   theme; and a modal Bottom sheet whose scrim alpha is below `shadow.faintScrimAlpha` (0.2), so a sheet over a
   faint or transparent scrim (a camera, a map) still has an edge (C2-48). **Never** another modal Bottom sheet (the
   dim separates it, as on iOS), never in dark theme (the rim does). `style.shadow` is the shadow drawn where these
   rules allow one; elsewhere it is ignored, and its KDoc says so.
2. **How: a nine-slice.** Once per key (corner radius px, blur px, spread px, offset px, colour, alpha, density), draw
   a shadow of a round rect of side `2R + 2 px` with `DropShadowPainter` into an `ImageBitmap` padded by the blur,
   spread and offset, then clear the rect's interior (`BlendMode.Clear`), so only the outside remains and a
   translucent body can never show it. Each frame, draw it as nine `drawImage(image, srcOffset, srcSize, dstOffset,
   dstSize)` slices around the current surface rect: four fixed corners, four edges stretched along their axis, no
   centre. No allocation per frame. A `Shadow` with a brush or a blend mode other than `SrcOver` is rejected when the
   style is built (§4.7).
3. **Skip** every slice when the shadow's alpha is 0 (the painter rasterises even at alpha 0).
4. **Bounded.** The node holds at most one bitmap per key it has used (two at most: one per theme).
5. **No `Modifier.shadow(elevation)`, no RenderNode elevation, no `dropShadow` sized to the surface** on any sheet
   node or ancestor.
6. **The surface never fades by itself.** The only fade is B-63's whole-sheet-layer `ModulateAlpha` fade, which needs
   no offscreen buffer; because the shadow is outside-only, nothing is seen through the body as it fades.

### 7.8 Scrim node

- Fills the sheet's bounds (a pane-bounded non-modal sheet: its pane, B-51); hit-tests (dismissal, attempts) and
  carries semantics (§9).
- Placed only while it blocks: always for a modal sheet, while `m > 0` for a non-modal one (B-51).
- Draws only when no presenter claims its level (§6.4), and for a pane-bounded non-modal sheet.
- No `graphicsLayer { alpha }` on it or on any window-sized node (SMOOTHNESS S8).

### 7.9 Backing, status bar and navigation bar

- **Backing.** Behind a receded presenter the window shows black: the top band, the side slivers, the bottom band, the
  presenter's rounded-corner cut-outs, and whatever shows through a part-floating sheet's gaps (judge-2 §2.5). It is
  the fill that already lies beneath the presenter, recoloured black while `e > 0`: for the shell, WP04 mutates the
  colour of the window's existing `ColorDrawable` background from a derived flag (one change per crossing), never
  calling `setBackgroundDrawable`, which can request a layout; for a cover, the cover frame's own full-screen fill.
  **Never an added full-screen draw** (judge-2 R15).
- **Status bar.** Light icons while `stack.rootReceded` in light theme (`sheet.cardStack.statusBarFlip` = 0.5). WP04
  writes only `WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars` from a `snapshotFlow`. It never
  feeds the flag into the template's `SystemBarIcons`, which re-runs `activity.enableEdgeToEdge(...)` from a
  `LaunchedEffect` keyed on the theme (`app4/kotlin/com/base/app/ui/AppRoot.kt:171-180` [C]) and would re-run
  `setDecorFitsSystemWindows`, the bar colours and an inset dispatch mid-drag (C1-29). A trace check confirms no View
  layout pass during a Medium → Large drag (§16.1).
- **Navigation bar** (C2-63). While a sheet covers the window's bottom edge (attached, or at a Large-equivalent stop)
  under 3-button navigation, the same owner sets `isAppearanceLightNavigationBars` from the luminance of
  `stack.bottomEdgeContainer` (light icons on a dark container), at most one change per crossing, and restores the
  theme's value when it no longer covers the edge. Gesture navigation's handle adapts by itself [U: §21].

### 7.10 RTL and large text

Start/End placement, the side panel's back pivot and swipe edge, and the header's slots follow the layout direction;
the grabber and the bottom predictive-back origin are centred. At font scale 1.5 and 2.0 the header grows, `Peek` and
`FitContent` re-resolve once, the footer unpins when header and footer exceed half the visible height (§4.8), action
rows grow (§4.9), and nothing is clipped (REQUIREMENTS 34; screenshot tests §15.5).

---

## 8. Adaptive behaviour

`SheetPlacement.Automatic` resolves in measure, with no `BoxWithConstraints`, from the placement class (modal: the
window's; non-modal: the pane's, B-13), the window's hinge and `AppTheme.posture` (`ds/foundation/adaptive/SizeClass.kt`
[C]). Width classes: Compact < 600, Medium 600-839, Expanded 840-1199, Large 1200-1599, ExtraLarge ≥ 1600 dp; heights:
Compact < 480, Medium 480-899, Expanded ≥ 900 dp. `SizeClass` is equality-stable, so a resize within a class changes
nothing.

**The rows apply top to bottom, and the first match wins** (C2-64): a landscape phone at 800 × 360 dp (Medium width,
Compact height) is a Compact-height window. The hinge rows are keyed on a **separating hinge**, not on the posture's
name (C2-19), so a flat dual-screen device (`WindowPosture(Kind.Flat, hinge != null)`, `WindowPosture.kt:12` [C]) is
covered.

| Window | Modal sheet | Non-modal sheet |
|---|---|---|
| A vertical separating hinge (book posture, or a flat dual screen) | The sheet's bounds become one half: the half holding the presenting pane or the action sheet's anchor (`LocalPaneBounds`, `SheetAnchor`), else the larger half, else the end half in the layout direction. Placement then runs again from the next rows with **that half's own class**, so a Compact half gets a bottom sheet in that half, never a card across the hinge. Explicit placements are honoured within the half | the same, within its pane's half |
| A horizontal separating hinge (tabletop) | **Bottom**. Every partial stop's top rests at `foldBottom` or lower (R-12); Medium rests on the fold (R-2). A Large-equivalent stop is allowed, as the person's choice, with the dim and **no card stack**: nothing straddles the crease at rest except a sheet the person pulled up | same, above the bar when it rests on it |
| Compact height, any width (phone sideways, short split) | **Bottom**, ≤ 640 dp, centred in the safe width; Medium inactive (R-2); other detents if they fit; attached, never floating; dim only, no card stack (ADAPTIVE §3.5) | same, its peek kept if it resolves |
| Compact width (phone portrait) | **Bottom**: detents as given; floating while partial; attached at a Large-equivalent stop; card stack there (D-9) | Bottom, above the tab bar at undimmed detents |
| Medium width (small tablet, unfolded inner screen, resized window) | **Center** form sheet: one stop `min(0.85 H, 620 dp, fit)` (R-10), all corners `R`, slides up on `springs.sheet`, swipe down closes (D-19), grabber hidden, dim, no card stack (fixes D11) | Bottom, `min(640 dp, W − 2 · gutter)`, centred; no card stack |
| Expanded width and up | Center form sheet (600 dp wide from Large width) | **Start** panel (iOS 27 `presentationPlacement(.leading)`, the Maps panel [A]): `supportingPaneWidth`, 16 dp (`side.inset`) from the start edge, the top (below the safe area) and the bottom of its bounds; detents as heights within the panel; what is beside it stays usable |
| Explicit `Bottom` | honoured; at Medium width and up it never recedes the presenter (D-9) | honoured |
| Explicit `Center` | honoured | honoured; undimmed only when `upThrough` is null (B-51) |
| Explicit `Start` / `End` | **modal side panel** from Expanded width (below it: Bottom, no room) | the non-modal Start/End panel from Expanded width (below it: Bottom) |
| Size-class, hinge or posture change while shown | re-resolved; snaps to the new frame (B-10): into Center or a side panel the detent is recorded, back into Bottom it lands on the recorded detent, else the tallest (R-10, R-11) | same |
| Freeform, ChromeOS, desktop windowing | the caption bar is part of `safeDrawing.top`; a resize within a class moves anchors without recomposing the content | same |

- **Form sheet mechanics.** The same state and layout node. One stop plus hidden. It rests centred in the safe area,
  at `safeTop + (safeHeight − height) / 2`, and lifts for the keyboard (B-45a). It enters by sliding up from below
  the window (the dim follows `pf`), the person can drag it down (B-22 with one stop), and predictive back scales it
  about its centre (B-39). `e = 0`: nothing recedes. No grabber; give it a header with a close button (docs). Detents
  under it follow R-10.
- **Modal side panel** (D-36; C2-18). An explicit `Start`/`End` on a modal sheet, from Expanded width: a panel
  `supportingPaneWidth` wide and as tall as the safe area less `side.inset` top and bottom, `side.inset` from its edge,
  all corners `R`, one stop (R-10), no grabber, shadow per §7.7. It slides in horizontally from its edge on
  `springs.sheet` (the state's offset runs along x for this placement; `pf` follows it; `e = 0`). Dim, blocking, what
  is beneath hidden from accessibility, focus moved in (B-42a), as any modal sheet. A horizontal drag toward its edge
  dismisses it by B-22 rule 3 with W for H (≥ 2 W/s toward the edge, or released ≥ 48 % of the way out); a drag away
  from the edge rubber-bands (B-24's formula, `d = side.inset`). Predictive back uses MDC's side-container pose with
  the swipe edge respected (B-39; REQUIREMENTS 32). `Start` and `End` follow the layout direction, and every x value
  mirrors in RTL. Keyboard: B-45a. Consumers: tablet filter panels and the DevTools panel (§13).
- **Non-modal Start/End mechanics.** A floating card at all detents (no attach morph), all corners `R`, shadow per
  §7.7. Its bottom edge is fixed `side.inset` above its bounds' bottom (plus the navigation bar); its top moves with
  the detent. Large's top is the bounds' safe top + `side.inset`. No card stack.
- **Input on large screens.** Tab reaches the grabber; Enter or Space cycles detents (Material [G]); Escape requests
  dismissal (B-42); a modal sheet takes focus, traps it and returns it on close (B-42a); a mouse drags the sheet by a
  primary-button press-and-drag on any non-scrolling part; the wheel and a touchpad scroll (B-27); right-click does
  nothing on the sheet. No orientation or resizability locks (L-1).

---

## 9. Accessibility

| Node | Semantics |
|---|---|
| `SheetLayout` | the traversal group (`isTraversalGroup = true`), so its children's `traversalIndex` values order them: surface 0, scrim 1 (`src/ModalBottomSheet.kt:141` [G] orders its scrim the same way under its outer `Box`) |
| Surface (where `modifier` lands) | `paneTitle`, always: the explicit `paneTitle` (the parameter, or `SheetSpec.paneTitle` for a destination), else the title of `AppSheetHeader(String)` or `ProvideSheetHeader(title)` of the uncovered step, else `designsystem_sheet_pane` ("Sheet") (fixes D12; C2-13); `traversalIndex = 0f`; the focus group of B-42a; the grabber's state and actions when no grabber is drawn. The sheet's semantics are chained after `modifier`, so its `paneTitle` wins over a caller's. `dialog()` while modal or above `upThrough`, **for tests only**: in ui-android 1.12.1 `IsDialog` is set but read nowhere else, so TalkBack never sees it, and its merge policy throws if merged (`SemanticsProperties.kt:180`, `:183-187`, `:1150` [V]); the pane title and B-42a's focus move are what TalkBack uses (C2-49) |
| Inside the surface | traversal order grabber, header, content, footer |
| Grabber | `Role.Button`; `contentDescription` `designsystem_sheet_grabber` ("Resize sheet"); `stateDescription` from the settled detent: "Expanded" at the tallest, "Collapsed" at the lowest when there are three or more, else "Partially expanded" (`derivedStateOf`, ≤ 1 recomposition per settle); the click (B-19) labelled with the state it moves to ("Expand sheet" or "Collapse sheet", as MDC's drag handle names the next state, `src/mdc/BottomSheetDragHandleView.java:82-87` [V]); the **built-in** actions `expand(label)`, `collapse(label)` and `dismiss(label)`, each only when it applies, which TalkBack, Switch Access and Voice Access all know as `ACTION_EXPAND`, `ACTION_COLLAPSE` and `ACTION_DISMISS` (material3 uses the same, `src/BottomSheet.kt:368-392` [G]; C2-50) |
| No grabber drawn | the state and actions move to the surface |
| Scrim (modal, or non-modal above `upThrough`, `scrimTapDismisses`) | `onClick(label = "Close sheet")`, `contentDescription` `designsystem_sheet_close_scrim`, `traversalIndex = 1f`, so it is the last stop (`src/Scrim.kt:58-71` [G]); nothing when it does not dismiss |
| Content beneath a modal sheet | `hideFromAccessibility()` through the overlay host's `HideBeneathNode` and, for destination sheets and covers, on the tab hosts (§11.2, N8) ([V] `SemanticsProperties.kt:1075`), replacing `clearAndSetSemantics` at `AppOverlayHost.kt:58` [C]. Hidden from the first moving frame of a present; restored on the frame a dismissal is decided (B-7), so TalkBack can land on the trigger while the sheet leaves (C2-49). Compose also prunes nodes the scrim covers (`ui112/semantics/SemanticsOwner.kt:274-275`, `:318` [V]); whether TalkBack's linear navigation skips a hidden node's descendants is [U] (§21; if not, keep `clearAndSetSemantics`) |
| Beneath a pane-bounded non-modal sheet above `upThrough` | only its pane, through `PaneBounds.block` (B-51) |
| Covered sheet | hidden from accessibility while a modal sheet is above it |
| Anchored action card | modal for semantics: beneath hidden, `paneTitle = title ?: designsystem_actions_pane`, a built-in `dismiss` action, focus to its first action and back to the anchor (§4.9; C2-14) |
| Header title | `heading()` (C2-49) |
| Close button | `designsystem_close`, 48 dp; absent while the person cannot close the sheet (§7.4) |
| Detent changes | announced through the grabber's `stateDescription` and the pane title; never `announceForAccessibility` (deprecated in API 36 [G]) |

- **Focus.** B-42a: focus moves into every modal sheet, in every input mode, is trapped there and is restored on close.
  Escape is handled on the surface after focused children (B-42).
- **Dismiss sources.** TalkBack's dismiss action is a `requestDismiss()`: it reports `Request` and asks a guard. The
  anchored card's outside tap reports `Scrim` (B-33).
- **TalkBack scrolling** never resizes the sheet (B-30); the grabber's expand action does. While touch exploration is
  on, `Automatic` content interaction is `Scrolls`, so the reach padding lets TalkBack's page scroll visit every row at
  a partial detent (B-27, B-31), and a row focused below the visible bottom moves the sheet up first (B-47a; C2-12).
- **Touch targets** 48 dp for the grabber (semantic bounds; its touch band is narrower vertically, §7.3), the close
  button and header actions.
- **Haptics:** none for accessibility actions (B-61).
- **Reduce motion:** B-63. **Reduce transparency:** Glass and Frosted draw Solid through `GlassQuality.Solid`.
- **Grabber contrast:** the documented exception of §7.3.
- **Strings** in `core/designsystem/src/main/res/values/strings.xml`, prefix `designsystem_` (F-5), with translations in
  every `generator/translations/android/` file: `designsystem_sheet_pane`, `designsystem_sheet_grabber`,
  `designsystem_sheet_expand`, `designsystem_sheet_collapse`, `designsystem_sheet_dismiss`,
  `designsystem_sheet_close_scrim`, `designsystem_sheet_state_expanded`, `designsystem_sheet_state_partial`,
  `designsystem_sheet_state_collapsed`, `designsystem_actions_pane` ("Actions"), `designsystem_sheet_discard_title`
  ("Discard changes?"), `designsystem_sheet_discard` ("Discard"), `designsystem_sheet_keep_editing` ("Keep Editing"),
  `designsystem_none` ("None"). `designsystem_close` and `designsystem_cancel` exist (`strings.xml:4-5` [C]).

---

## 10. Performance contract

The reviewer's checklist. Frame budget at 120 Hz: 8.33 ms.

### 10.1 Budget per phase

| Phase | Allowed per frame | Recompositions |
|---|---|---|
| Drag, attached (Large-equivalent, 3-button navigation, Compact height, `i₀ = 0`) | L1: the sheet layer's RenderNode moves; **0 re-records** in the sheet (§6.3); each presenter's dim node re-records (a clip, a `drawLayer`, one D2 rect) and its recede layer moves (L0) | **0** per move |
| Drag, floating or above the bar | as attached + D1: the surface node re-records one round rect, the rim and the clip; no child is re-recorded | **0** per move |
| Show, settle, hide | the same | **0** in frames 2…N−1; frame 1 composes; ≤ 1 at a settle, in readers of `currentDetent` |
| Show of an in-screen sheet | +1 frame: `HostedOverlay` registers in a `DisposableEffect`; the overlay host then composes the sheet and its content at Large's height (B-1); with a content-side provider the slot host composes once more before the first moving frame (B-2); with `keepContent`, nothing after the first opening | as listed; gated by `sheet_present.hitch_ms_per_s` and `.tap_to_motion_ms` (§16.1) |
| Predictive back progress | L0 (sheet layer scale, content counter-scale) | **0** per event |
| IME show or hide | L1 (lift) + L0 (the footer layer's position) + content re-measure | **0** in content; ≤ 2 content re-measures per IME transition; **0** anchor writes |
| Card stack (presenter) | L0 (recede layer) + the dim node's re-record + Compose's `onLayoutChange` bookkeeping for a layer transform (§6.4); the content layer is not re-recorded; the backing is the existing fill | **0** in the shell's content; **0** `RecordSceneNode` records |
| Presenter under an opaque sheet at Large | only the visible band above the sheet is drawn (§6.4) | 0 |
| Floating ↔ attached | D1 (surface and clip) + L0 | 0 |
| Non-modal boundary crossing | D2 dim fade; the scrim placed or unplaced in placement | **0** (beneath semantics through `HideBeneathNode`) |
| Park, unpark | L1 + the sheet layer's alpha (L0) | 0 per frame; a tab switch with a parked peek: 0 recompositions and 0 re-records on the return frame |
| Following the owner's pose (B-14a) | L0 | 0 |
| Glass at a partial detent | one backdrop pass over the visible card (Live tiers, presenter = the shell only) | 0; a tier change recomposes only the backdrop slot, no content |

### 10.2 Where each fast value is read

| Value | Written by | Read in | Class |
|---|---|---|---|
| `offset` (anchored) | drag, fling, the nested-scroll session, `animateTo`, `updateAnchors` | `SheetLayout` placement (inside `withMotionFrameOfReferencePlacement`, `src/DraggableAnchors.kt:161-193` [G]); `SheetStack` fractions; `visibleTop` | L1 |
| stretch amount | `SheetStretch` and the sheet's `dragBy()` | placement | L1 |
| `parkProgress` | the park `Animatable` | placement; `SheetStack`; `visibleTop` | L1 |
| `isTabSelected`, `isUncovered` | WP04 (§4.10) | the sheet layer's block (alpha 0), placement (off-window), a `snapshotFlow` (semantics, Back, stack) | L0, L1 |
| owner pose | WP04's stack scene | the sheet layer's block (`EntryPose.applyTo`) | L0 |
| IME current inset, `lift`, footer y | nobody: read from `SheetImeSource` | placement (the footer as its layer's position) | L1, L0 |
| `imeTargetVisible` | placement writes a snapshot field only when it changes | content box measure; `contentPadding`; B-37 and B-38's collector | ≤ 2 re-measures per transition |
| bar `liveTop` | WP04's bar | placement (B-52) | L1 |
| `presentationFraction`, `expansionFraction`, `recede(level)`, `dim(level)`, `visibleBottom(level)` | derived from `offset` | presenter layer and draw; covered sheet layer and draw; surface draw; scrim draw; backdrop draw (`SheetFrame.opaque`, `.dim`) | L0, D1, D2 |
| back progress | the Back handler | the sheet layer's and the content layer's layer blocks | L0 |
| `contentScrolled` | the nested-scroll connection | the header's draw | D1 (the header's own layer) |
| `contentPadding` values | settles, footer measure, IME target, placement class | the scroller's measure | re-measure, 0 recompositions |
| `currentDetent` | arrival | composition (grabber `stateDescription`, caller UI) | ≤ 1 per settle |
| `targetDetent` | release, `animateTo`, the drag's midpoint crossings | composition only where a reader wants it | ≤ 1 per midpoint crossing during a drag |
| `isMoving`, `isLeaving`, show/hide progress | the sheet's own flags | `snapshotFlow` in effects: `HostMotion.report`, Back enablement | 0 |
| back progress moving (a boolean) | a `snapshotFlow` | the sheet's layer wrapper (the frame-rate vote, B-64) | ≤ 2 per back gesture, in that wrapper only |
| `blocking` (non-modal) | the settled or target stop | `HideBeneathNode` (`invalidateSemantics()`), `PaneBounds.block`, B-42a | 0 |
| `visibleTop`, `visibleHeight` | derived on read; the lift part written by the sheet's placement only while the keyboard moves | a rider's placement block | L1 in the rider |

### 10.3 Forbidden in sheet code

Each is a review finding; those marked (rule) are also code rules (§19.1 item 9):

- reading `offset`, `presentationFraction`, `expansionFraction`, `visibleTop`, `recede`, `dim`, `contentScrolled`, back
  progress or an `EntryVisibility` getter in composition (lint `FrequentlyChangingValue` is an error, M-5);
- **a draw node that reads a per-frame value with content drawn inline beneath it**: such a node wraps a layer
  (`graphicsLayer()` with no properties) around that content, or a frame of motion re-records the whole subtree to the
  next layer (F11; C1-01);
- a child that moves or changes per frame (grabber, header, footer, content) placed without its own layer (C1-02);
- `Modifier.offset(dp)`, or padding driven by the offset;
- `graphicsLayer { alpha }` on any node larger than a card (rule `layer-alpha-fill`, R9). **Allow-listed by name**: the
  sheet layer with `CompositingStrategy.ModulateAlpha` (B-63) and its alpha-0 park (B-14);
- `BoxWithConstraints` or `SubcomposeLayout` anywhere in the sheet (material3 removed its own, `f7fe1d92`);
- allocating in a draw, layer or placement block (paths are rewound, shapes and brushes cached); calling a `() -> Float`
  or `() -> Color` lambda per frame (it boxes: C1-20); a `HashMap<Int, …>` cache; an iterator, `filter` or `maxBy` in
  `SheetStack`'s per-frame functions;
- `dispatchRawDelta` and `AnchoredDraggableState.settle(velocity)` (B-28) (rule);
- the Compose `NavigationBackHandler` for a sheet's own handler (B-36);
- a `Dialog` or `Popup` window (rule);
- a tween after a finger;
- `Modifier.shadow(elevation)`, RenderNode elevation, or a `dropShadow` whose size follows the sheet (§7.7) (rule);
- `fitInside(WindowInsetsRulers.Ime)`, `imePadding()` or `windowInsetsPadding(WindowInsets.ime)` in sheet content (rule);
- a `LaunchedEffect` that sets up first-frame state (API-13);
- `setBackgroundDrawable` or `enableEdgeToEdge` driven by sheet state (§7.9);
- in app code: `AppSheet` inside an `if`, `when` or lazy item scope (B-4a) (rule); size, offset, padding, clip, shadow
  or layer modifiers passed as `AppSheet(modifier = …)` (§4.2) (rule); a second layout emitter at the root of a content
  lambda (§4.8) (rule); `@OptIn(ShellSeamApi::class)` under `feature/**` (rule); a `when` over `SheetDetent`,
  `SheetModality`, `SheetDismissSource` or `SheetMaterial` without `else` (rule).

### 10.4 Recomposition and re-record budgets

Asserted by `RecompositionJourneysTest` (RECOMPOSITION-PLAN §3), by counting draw modifiers in the test fakes, and on the
phone (§16.1).

| Journey | Budget |
|---|---|
| 30 drag moves across detents | 0 recompositions in content, header, footer, surface and presenter |
| 30 attached drag frames (`[Medium, Large]` under 3-button insets, or at Large) | 0 sheet-layer draw executions |
| 30 floating drag frames | ≤ 30 surface re-records; 0 grabber, header, content or footer re-records |
| 30 drag frames with the card stack | 0 `RecordSceneNode` records; 0 runs of a counting draw lambda inside the fake shell's content |
| A settle on a new detent | ≤ 1, only in readers of `currentDetent` |
| A settle and a glass tier change | 0 in content composables reading `LocalSheetSurface` |
| Medium → Large with the card stack | 0 in the shell's content |
| 30 back-gesture progress events | 0 in content and presenter; the sheet's layer wrapper once at the start and once at the end (B-64) |
| IME rise and fall | 0 in content; 0 anchor writes; ≤ 2 content re-measures |
| Show | frames 2…N−1: 0 (the slot host once before the first moving frame when a provider registered) |
| Non-modal boundary crossing | 0, and no new composition group in the sheet's content (`SheetRestoreTest`) |
| Park and unpark (push over the owner, pop) | 0 per frame in the sheet |
| Tab switch away and back with a parked peek | 0 recompositions and 0 re-records in the sheet on the return frame |
| Status-bar flip | ≤ 1 change per crossing, in the edge-to-edge owner only; 0 recompositions |

---

## 11. Integration seams

### 11.1 The overlay host (WP03)

- `AppTheme` (outermost) provides `LocalSheetStack` and `LocalSheetBackdrop` (one `SheetBackdropHolder`) beside
  `LocalOverlayHost`.
- **`HostedOverlay` keys and saved state** (C1-03, C2-03; D-40). Today it wraps each entry in `key(entry)`
  (`ds/component/overlay/AppOverlayHost.kt:66` [C]), where `OverlayEntry` is a plain class with identity `hashCode`
  (`:46`) created anew by each `DisposableEffect` run (`:90-98`). Compose mixes `dataKey.hashCode()` into the compound
  key (`rt112/GapComposer.kt:3085-3097` [V]) and `rememberSaveable` keys by that compound hash
  (runtime-saveable 1.12.1 `RememberSaveable.kt:81-87` [V]), so every saveable inside a sheet got a new key after a
  recreation and restored nothing. Revision 2:
  - the call site captures `currentCompositeKeyHashCode` (stable across recreation for the same call site) and a
    `rememberSaveableStateHolder()`, which lives in the caller's registry, so it is restored with the caller and
    dropped with it (no `removeState` bookkeeping);
  - the host composes each entry as `key(entry.callSiteKey) { CompositionLocalProvider(entry.locals) {
    entry.holder.SaveableStateProvider(entry.callSiteKey) { entry.content() } } }`. Calls made in a loop need
    `key(item.id)` around them, as `rememberSaveable` does; the host breaks a duplicate key by insertion order;
  - `modal` becomes `modal: () -> Boolean`, read only by the host's `HideBeneathNode` and focus guard through a
    `snapshotFlow`, never a `DisposableEffect` or `key` input: today `DisposableEffect(host, modal)` (`:90`) disposes
    and re-creates the entry when `modal` changes, which would rebuild a non-modal sheet's whole composition at every
    `upThrough` crossing;
  - WP04's manifest keeps the activity through rotation, resize and fold (`app4/AndroidManifest.xml:57-64` [C]), so
    the loss showed on night mode, locale, density and font-scale changes and on process death; the fix covers all.
- **`HideBeneathNode`** on the app content box replaces `clearAndSetSemantics {}` (`:58`): a `SemanticsModifierNode`
  that applies `hideFromAccessibility()` while any hosted entry's `modal()` is true and calls `invalidateSemantics()`
  when that changes. 0 recompositions.
- **Focus guard** on the app content box (B-42a): a `FocusRequester` for save and restore, and
  `focusProperties { onEnter = { cancelFocusChange() } }` read from a plain field.
- **No pointer modifiers** on the overlay host's boxes, so a touch that misses every placed overlay reaches the app
  content box (B-51's pass-through relies on it).
- **Parking** (B-14) for sheets and every other hosted overlay (alerts, action sheets, menus), the `KeptTabs` way:
  still placed, layer alpha 0, off the window, semantics cleared through a node, pointer input ignored; returns in
  place on the switch frame.
- **Call-site removal** (B-4a): `onDispose` removes the entry and, for a sheet, unregisters it from the stack, reports
  `HostMotion(false)` and restores focus, in that frame.
- **`keepContent`**: the entry stays registered and composed while hidden, parked as above; the next `visible = true`
  unparks it and shows it.
- Bounds of a non-modal sheet: `LocalPaneBounds` (B-13).
- In-screen sheets register in the `SheetStack` at their level (§3.2): non-modal, the owner's level
  (`LocalPresentationLevel.current`, which inside a sheet is that sheet's level) + 1; modal, B-54a.
- **The DevTools overlay** stays where it is (above the shell and the presentation host, below the overlay host,
  `app4/kotlin/com/base/app/ui/AppRoot.kt:86-89` [C]): a debug-only badge that a destination sheet does not dim or
  recede, that in-screen sheets cover, and that is hidden from accessibility under any modal in-screen sheet. Release
  builds do not contain it (C1-16).

### 11.2 The presentation host and Navigation 3 (WP04)

- **The host builds its own strategy.** `PresentationHost` creates `SheetSceneStrategy(sessions, isPresented)` and puts
  it first in its strategies (`bottomsheet.md:157`: before any non-overlay strategy). The `sheetStrategy` parameters of
  `AppShell` and `PresentationHost` are deleted (`nav4/AppShell.kt:96`, `:200`; `PresentationHost.kt:70`, `:121` [C];
  unmerged, so no deprecation). A sheet key never falls back to a cover, so N2 cannot recur.
- **Stateless strategy.** `calculateScene(entries)`: the top entry's `metadata[PresentationKey] as? Presentation.Sheet`,
  else `null`; `null` when nothing is beneath; otherwise
  `SheetScene(entry = top, overlaidEntries = previousEntries = entries.dropLast(1), spec, sessions, isPresented, onBack)`.
  It mutates nothing: Navigation 3 calls it once per overlay level on every calculation ([V] `current-state.md` §4.2),
  which is why WP03's `states.clear()`/`retainAll` lost every lower sheet's state (N1).
- **`SheetScene`** is an `OverlayScene` with `key = entry.contentKey` and `equals`/`hashCode` over the entry's content
  key, the keys beneath and the spec (lint `SceneEqualsHashCode`). Its content:
  `PresentedSheetFrame { AppSheetLayout(state = sessions[contentKey], onDismissRequest = onBack, modality = Modal,
  dismissible = spec.dismissible, properties = spec.properties, style = AppSheetDefaults.style(material =
  spec.material), paneTitle = spec.paneTitle?.let { stringResource(it) }) { entry.Content() } }` plus
  `LaunchedEffect(state) { state.show() }` (a no-op for a sheet already up).
- **`SheetSessions`** (C1-28). Each session's `AppSheetState` is created with `rememberSaveable(saver =
  AppSheetState.Saver(spec.detents))` inside a `NavEntryDecorator` that runs within the saveable-state decorator's
  scope (`design-perf.md:909`), so it is saved with its entry and dropped with it; no map keyed by
  `contentKey.toString()`, which loses keys without a stable `toString` and keeps popped keys in the saved map. The
  decorator also registers the remembered state in an in-memory map keyed by `contentKey` (for `onRemove()`), removed
  in `onPop`, which runs only when the last entry with that key has left the back stack and composition, so after
  `onRemove()` has finished ([V] `navigation3-runtime 1.1.7 NavEntryDecorator.kt:70-88`, `DecoratedNavEntries.kt:205-223`).
  The decorator joins the host's list after the saveable-state and ViewModel decorators (`PresentationHost.kt:92-94` [C]).
- **`onRemove()`**: when the entry is still presented (`isPresented(contentKey)`), return at once; otherwise
  `sessions.peek(contentKey)?.hide()`. `peek` is null-safe for a scene removed before its first composition (the
  recipe's `lateinit`, `src/nav3-recipes-BottomSheetSceneStrategy.kt:31`, is not copied). Navigation 3 keeps a popped
  overlay composed until `onRemove()` returns and renders the first scene instance it saw for a key ([V]
  `src/NavDisplay.kt:686-698`, `:874-881`), so the session map, not the scene object, is the source of truth.
- **`PresentedSheetFrame`** replaces `PresentedFrame` for sheet entries (N4): no opaque `background(colors.background)`
  and no card `graphicsLayer` (`PresentationHost.kt:162-196` [C]). It **registers in `live` exactly as `PresentedFrame`
  does**, `DisposableEffect(Unit) { onLive(1); onDispose { onLive(-1) } }` (`:173-176` [C]), because the host returns
  before composing `NavDisplay` when `presented.size == 0 && live == 0 && !host.card.active` (`:80` [C]): without it,
  popping the last sheet would remove `NavDisplay`, the `OverlayScene`, its `onRemove()` slide, `StackBackHandler`
  and `GateSettle` (`:153-154`) in one recomposition, and B-43's counted presses would reach the tab (C1-06). It
  provides `LocalTabBarVisible provides false`; `LocalHostMotion` = the presentation gate; `LocalPresentationLevel` =
  the entry's depth; `LocalEntryVisibility` with `isUncovered` true while nothing is presented above it; `LocalBarFrame`
  null. It never reads `LocalNavAnimatedContentScope`, which in an overlay scene is the underlying scene's ([V]
  `current-state.md` §4.2). `ReportMotion` is not used for sheets: the sheet reports itself (B-15).
- **Covers** keep `PresentedFrame`. It applies `cardStackPresenter(stack, level = depth)` inside its existing
  full-screen fill, which turns black while that level recedes (§7.9).
- `PresentationCard` asks "is this key a cover?" through `PresentationKey` (today through the string key,
  `PresentationHost.kt:74-78` [C]).
- `StackBackHandler` and `GateSettle` stay (`PresentationHost.kt:153-154` [C]); the sheet's handler runs ahead of them
  at overlay priority. `StackBackHandler`'s `enabled` becomes `(canGoBack || gate.moving) && !stack.blocksBack(level)`
  (B-44), so Back reaches it only while a destination sheet leaves (B-43).
- **Semantics (N8):** while a modal presentation (sheet or cover) is up, the tab hosts beneath get
  `hideFromAccessibility()` through a semantics node that invalidates itself from a derived flag: 0 recompositions.
- **Routing:** `ShellState.submit` implements B-58. A deep link that changes tab leaves presented sheets up
  (`nav4/ShellState.kt:195-210` behaviour unchanged). A cold deep link to a sheet destination slides in (B-6).

### 11.3 The shell and the stacks (WP04)

- `ShellChrome`'s box (tab content and bar) gets `Modifier.cardStackPresenter(stack, level = 0)`
  (`nav4/AppShell.kt:131-199`, `nav4/shell/ShellChrome.kt` [C]), three nodes in §6.4's order. The `GlassStage` lies
  inside the content layer. The shell content recomposes 0 times (RECOMPOSITION-PLAN `:193`) and is not re-recorded
  by sheet motion.
- An `AppShell` option `sheetCardStack: Boolean = true` turns the card stack off app-wide (the dim stays); a sheet turns
  it off for itself with `recedesPresenter = false` (§4.6).
- The window's background is black while `stack.recede(0) > 0`, by mutating the existing `ColorDrawable`'s colour from
  a derived flag (§7.9); nothing opaque lies between the window background and the receding box.
- The edge-to-edge owner flips the status bar while `stack.rootReceded`, and the navigation-bar icons over a sheet
  under 3-button navigation, through `WindowInsetsControllerCompat` only (§7.9).
- **`LocalHostMotion` per host, as overlay motion** (C1-08; D-15). `BackGate` gains overlay reporters:
  `report(reporter, moving, overlay = true)` adds to a separate set that `moving` includes (so presses are counted)
  but that `guard()` treats as `Wait`: `pushDuringPush` becomes `command is Navigate && lastChange == Push &&
  stackMoving`, where `stackMoving` excludes overlay reporters (`nav4/back/NavGuard.kt:21-23`, `nav4/back/BackGate.kt:47`,
  `:59-74` [C]). Today `lastChange` stays `Push` on any pushed screen and is never cleared by motion reports, so a
  push submitted while that screen's sheet presents, leaves or unparks was dropped and `AppShell`'s loop ended. Each
  kept tab's stack host forwards its sheets' reports this way; the presentation host to `presented.gate`;
  `AppSheetNavHost` to its own gate.
- **Default-priority Back beneath a modal sheet** (B-44): each tab host's and the presentation host's
  `StackBackHandler` adds `!stack.blocksBack(level)` to its `enabled`.
- **`LocalEntryVisibility`** per stack entry (`StackHost`, `KeptTabs`): `isTabSelected` from the tab selection
  (`KeptTabs.kt:120` [C]); `isUncovered` true while the entry is the target top of its stack and nothing is presented
  above its host (false on the first frame of a push over the entry, true on the first frame of the pop that reveals
  it); `isSettled` from the entry's arrival reaching 99 %; `pose` applying the entry's `pose(key)` transform
  (`nav4/stack/StackLayout.kt:128` [C]) to another layer. `LocalTabVisible` stays their conjunction for clocks
  (`StackLayout.kt:123`, `:136` [C]; C2-04).
- **`LocalPaneBounds`**: the shell provides the content area's window bounds (excluding a rail or sidebar); each pane of
  the list-detail and supporting-pane scenes provides its own, with a `block` handler that hides that pane's subtree
  from accessibility while a pane-bounded sheet blocks it (B-51; C2-20). The holder is written from `onPlaced` and
  read in the sheet's measure; the stored `Rect` is replaced only when the pane moves.
- **`LocalBarFrame`**: `settledTop` from the bar's own expanded, settled height (never the keyboard), `liveTop` from
  the bar's minimise motion (`nav4/shell/ShellBar.kt`, `AppTabBarState.kt` [C]), read in placement (B-52).
- **`LocalPresentationLevel`**: 0 in the shell; the depth in each presented entry.
- `BottomChromeWriter` gives the keyboard precedence over a presented bar (N9, `nav4/shell/BottomChromeWriter.kt:50-55`
  [C]) and takes `max(its value, stack.topBottomChrome)` so a toast over a sheet rests above its footer. Sheets no
  longer read `LocalBottomChrome` for their resting line (B-52).
- `Chrome.kt` merge (N11): WP03's `ds/foundation/Chrome.kt` and WP04's `ds4/foundation/layout/Chrome.kt` reconcile at the
  WP03/WP04 merge on WP04's package and `TabVisibility` holder; the new seam files (§12) live beside it.

### 11.4 Chrome and adaptive locals inside a sheet

| Local | Value inside the sheet | Why |
|---|---|---|
| `LocalBottomChrome` | a holder carrying the sheet's settled `contentPadding.bottom` | a screen reused in a sheet that pads with `chromeContentPadding()` (`ds4/foundation/layout/Chrome.kt:92-103` [C]) gets the same bottom as `contentPadding`; use one, never both |
| `LocalTopChrome` | a zero holder; an `AppScaffold` inside provides its own | the header is above the content, not over it |
| `LocalTabBarVisible` | false | no bar under a sheet |
| `LocalBarFrame` | null | no bar under a sheet |
| Container class, `AppTheme.layout`, spacing, gutter | `ProvideContainerWidth(sheetWidth)` (`ds/foundation/adaptive/WindowClass.kt:60` [C]), `sheetWidth` from the placement class and placement (§7.2), known in composition and changed only at class crossings | a 360 dp panel or a 540 dp form sheet on an Expanded window lays out as Compact, not as the caller's pane (C2-02) |
| `LocalPaneContext` | reset to `PaneContext()` (`ds4/foundation/layout/Chrome.kt:81-89` [C]) | a screen reused in a sheet shows no "beside" UI |
| `LocalEntryVisibility`, `LocalTabVisible` | the sheet's own: not uncovered while another sheet covers it or it is parked | clocks and shimmers inside stop when covered |
| `LocalSheetSurface` | the sheet's remembered `SheetSurface` (§4.8) | `AppScaffold`, `AppTopBar`, `AppBottomActionBar` adapt |
| `LocalPresentationLevel` | the sheet's level | sheets opened from its content stack on it |
| insets | `safeDrawing.only(Top)`, `navigationBars`, `ime` consumed | B-50 |

`SheetAdaptiveTest` asserts `AppTheme.containerClass` inside each placement: a Start panel and a Center sheet on
1280 × 800 are Compact; a non-modal Bottom sheet on 900 × 1000 is Medium.

### 11.5 Glass (WP05)

1. `GlassSheetBackdrop` implements `SheetBackdrop` with Kyant's `drawBackdrop` over the stage's `SceneBackdrop`, as
   `rememberGlassChrome` does (`glass/GlassChrome.kt:24-48` [C]): effects built once and reading no state; the tint
   and `SheetFrame.dim` drawn in `onDrawSurface`; the pass skipped while `SheetFrame.opaque == 1` (which includes
   `!stack.samplesScene(level)`) or the sheet is parked; no allocation per frame (if Kyant needs a `Shape`, shapes are
   held in an array indexed by whole px).
2. It reaches the hosts through the one `SheetBackdropHolder` the outermost `AppTheme` installs: `GlassStage` writes
   `holder.backdrop` on attach and on a tier change (null when the tier samples nothing), without widening
   `LocalGlassScene` (`glass/GlassStage.kt:51-58` [C]). The sheet reads it in a small backdrop slot inside the
   surface, so a tier change recomposes that slot only, never the content (C1-21).
3. Live sampling only for a sheet whose presenter is the shell, with no cover up (§6.6; C1-13).
4. The bar inside the glass: the chrome records itself into a `GraphicsLayer` it also draws, and the backdrop draws it
   after the scene; or, if that costs a per-frame re-record, the gap is documented and CP2's recording checks it.
5. `SceneBackdrop` invalidates samples on `scene.localPositionOf(surface)`, not on `positionInRoot()`
   (`glass/SceneBackdrop.kt:52`, `:58-61` [C]), so the card stack's shared transform triggers no re-sample of the bar
   or a Glass sheet (C1-14). §16.1's `glass_live` column checks that neither the scene's RenderNode nor the bar's
   backdrop is re-recorded during a Medium → Large drag.
6. The GPU watcher (`glass/GlassQuality.kt:96-108` [C]) watches frames while a live Glass sheet draws, and demotes the
   tier if the GPU falls behind; the demotion reaches sheets through the holder (item 2).
7. CP2 decides `sheet.glass.blur` and `sheet.glass.bodyAlpha` from recordings.
8. It verifies sampling alignment under the receding presenter (§6.6).
9. `design/wp/WP05.md:64` and the handoff's "sheets never sample" (`handoff/WP05.md:125`) are revised to this spec.

### 11.6 Test tags

`modifier` lands on the surface, so `sheet_surface` is the sheet's root tag. Contract tags (A3.8): `sheet_surface`,
`sheet_grabber` (exists), `sheet_scrim`. The sample destination (WP02-M3) exposes `sheet_open`, `sheet_list`,
`sheet_field`, `sheet_footer`. Catalog triggers are `catalog_sheet_<demo>` (§14). The overlay host already exposes
tags as resource ids (`AppOverlayHost.kt:63-66` [C]); the presentation host does the same.

---

## 12. Packages and files

Concept packages, no `util`, `internal` or `common` packages (F-2, F-3); one public component per file with its
defaults (K-2); every file under 400 lines. Tests mirror the packages (F-6).

```
core/designsystem/src/main/kotlin/com/base/app/core/designsystem/                      (WP03)
├── theme/AppDesignStyle.kt              + SheetMaterial; AppStyle.sheetRadius, sheetInset, sheetMaterial
├── foundation/layout/
│   ├── ShellSeamApi.kt                  the opt-in annotation
│   ├── HostMotion.kt                    HostMotion, LocalHostMotion
│   ├── EntryVisibility.kt               EntryVisibility, EntryPose, LocalEntryVisibility
│   ├── PaneBounds.kt                    PaneBounds, LocalPaneBounds
│   ├── BarFrame.kt                      BarFrame, LocalBarFrame
│   └── PresentationLevel.kt             LocalPresentationLevel
└── component/sheet/
    ├── AppSheet.kt                      the two AppSheet overloads
    ├── AppSheetState.kt                 AppSheetState, rememberAppSheetState, the Saver
    ├── AppSheetStyle.kt                 AppSheetStyle, AppSheetColors
    ├── AppSheetDefaults.kt              AppSheetDefaults
    ├── AppSheetProperties.kt            AppSheetProperties, SheetPlacement, SheetContentInteraction, SheetTransition, SheetKeyboard
    ├── SheetModality.kt                 SheetModality, SheetDismissSource
    ├── SheetDetent.kt                   SheetDetent, SheetDetentContext
    ├── AppSheetScope.kt                 AppSheetScope, LocalSheetSurface, SheetSurface
    ├── chrome/
    │   ├── AppSheetGrabber.kt           the pill, its contrast form
    │   ├── SheetGrabberNode.kt          touch band, hover, focus ring, click, semantics, visibility
    │   ├── AppSheetHeader.kt            the three-slot header and its scroll-edge fade
    │   └── AppSheetCloseButton.kt
    ├── content/
    │   ├── SheetColumn.kt               SheetColumn, sheetContentPadding, sheetFooterItem
    │   ├── SheetSlots.kt                ProvideSheetHeader, ProvideSheetFooter, ProvideSheetStyle, the slot registry
    │   ├── SheetDismissGuard.kt
    │   ├── SheetDismissConfirmation.kt
    │   ├── SheetDragExclusion.kt
    │   └── SheetBackHandler.kt
    ├── layout/
    │   ├── AppSheetLayout.kt            the in-place sheet (@ShellSeamApi)
    │   ├── SheetLayout.kt               the one Layout: placement, footer, padding, park, scrim placement
    │   ├── DetentResolution.kt          R-1…R-13 (pure Kotlin)
    │   ├── SheetGeometry.kt             floating, attach, local clip rect, stretch band (pure Kotlin)
    │   ├── SheetPlacementPolicy.kt      Automatic → Bottom / Center / Start / End, hinge halves (pure Kotlin)
    │   ├── SheetImeSource.kt            the IME seam; the rulers implementation
    │   ├── SheetImeLift.kt              lift, Center and side lifts; API 26-29 fallback
    │   └── SheetBringIntoView.kt        B-47a
    ├── gesture/
    │   ├── SheetReleaseRule.kt          B-22 (pure Kotlin)
    │   ├── SheetFlingBehavior.kt        applies B-22, sets leaving, settles, plays the arrival haptic
    │   ├── SheetStretch.kt              the rubber-band OverscrollEffect and dragBy()
    │   ├── SheetNestedScroll.kt         the per-gesture session, Resizes / Scrolls, contentScrolled
    │   └── SheetCatch.kt                the Initial-pass node: catch, re-settle, pointer down, pointer type
    ├── back/
    │   ├── SheetBack.kt                 the handler, enablement, branches 0-4, eased progress, Escape
    │   └── SheetFocus.kt                B-42a: save, move, guard, restore
    ├── surface/
    │   ├── SheetSurfaceNode.kt          body, brush, rim, Frosted brushes, content clip
    │   ├── SheetShadow.kt               the nine-slice (pure geometry + one cached bitmap)
    │   ├── SheetScrim.kt                hit-testing, semantics, fallback and pane dim
    │   └── SheetBackdrop.kt             SheetBackdrop, SheetFrame, SheetBackdropHolder, LocalSheetBackdrop
    ├── stack/
    │   ├── SheetStack.kt                registry, levels by stack order, recede, merged dim, blocksBack
    │   ├── CardStackMath.kt             §6.4 formulas, the n-layer fold (pure Kotlin)
    │   └── CardStackPresenter.kt        Modifier.cardStackPresenter (recede layer, dim node, content layer)
    ├── action/
    │   ├── AppActionSheet.kt            sheet and anchored card
    │   ├── SheetAction.kt
    │   └── SheetAnchor.kt               SheetAnchor, rememberSheetAnchor, Modifier.sheetAnchor
    └── choice/
        ├── AppChoiceSheet.kt            AppChoiceSheet, AppChoiceSheetDefaults
        └── ChoiceSearch.kt              accent- and case-folding match (pure Kotlin)

core/designsystem/.../component/overlay/          kept: AppAlert, AppMenu, AppContextMenu, AppToast, AppOverlayHost
                                                  (+ call-site keys and saved state, HideBeneathNode, focus guard, parking)
core/designsystem/.../foundation/glass/GlassSheetBackdrop.kt                        (WP05, liquidglass only)

core/navigation/src/main/kotlin/com/base/app/core/navigation/                       (WP04)
├── Presentation.kt                      Presentation, SheetSpec, PresentationKey
├── back/BackGate.kt, NavGuard.kt        overlay reporters; Wait for overlay motion
└── presentation/
    ├── PresentationHost.kt              builds the sheet strategy; semantics beneath; routing hooks
    ├── PresentationCard.kt
    ├── PresentedFrame.kt                covers only (moved out of PresentationHost.kt); cardStackPresenter(level)
    └── sheet/
        ├── SheetSceneStrategy.kt
        ├── SheetScene.kt
        ├── SheetSessions.kt             + its NavEntryDecorator (rememberSaveable per entry)
        ├── PresentedSheetFrame.kt       counts in live
        └── AppSheetNavHost.kt           per-step slot registry layers
core/navigation/.../shell/CardStack.kt   applies cardStackPresenter(0); window backing; status and navigation bars

catalog/src/main/kotlin/com/base/app/catalog/sections/sheets/                       (WP03)
├── CatalogSheets.kt                     the page and its index
├── SheetDetentDemos.kt
├── SheetBehaviourDemos.kt
├── SheetStyleDemos.kt
├── SheetPresetDemos.kt
└── SheetPreviews.kt                     multi-size @Previews of the demos (API-13, REQUIREMENTS 23)
```

Deleted: `ds/component/overlay/AppSheet.kt`, `AppSheetState.kt`, `AppActionSheet.kt` (moved and rebuilt),
`ds/component/overlay/OverlayBackHandler.kt`'s use by sheets (it stays for other overlays), WP03's
`core/navigation/SheetScene.kt` (never wired), the legacy `component/overlay/AppBottomSheet.kt` after its deprecation
release (§13), `catalog/sections/CatalogOverlays.kt`'s sheet demos.

---

## 13. Migration

Only the legacy `AppBottomSheet` and `AppActionSheet` have shipped (integration branch `8672003`, copies in WP01-WP10,
`current-state.md` §1). WP03's `AppSheet` and WP04's `Presentation.Sheet` are unmerged and change without deprecation.
The two shipped ones go through one release of `@Deprecated(level = WARNING, replaceWith = …)` (API-14), because
generated apps take template changes through `upgrade_project.py`.

| From | To | Notes |
|---|---|---|
| `if (show) AppBottomSheet(onDismissRequest, title, showHandle, dismissOnClickOutside) { … }` (a `Dialog`, `ds4/component/overlay/AppBottomSheet.kt:42-50` [C]; its content is a `ColumnScope` lambda, so `SheetColumn` takes it unchanged) | `AppSheet(visible = show, onDismissRequest, state = rememberAppSheetState(listOf(SheetDetent.FitContent)), properties = AppSheetProperties(scrimTapDismisses = dismissOnClickOutside), grabber = if (showHandle) AppSheetDefaults.Grabber else null, header = title?.let { t -> { AppSheetHeader(t) } }) { SheetColumn { … } }` | The deprecated `AppBottomSheet` is exactly this delegation for one release; the `if` goes (B-4a). A tall content behaves as Large (R-6a) |
| Legacy `AppActionSheet(actions, onDismissRequest, title, message, cancelLabel)` (no `visible`) | `AppActionSheet(visible = true, actions, onDismissRequest, …)` | Deprecated overload delegates; `SheetAction.isDestructive` → `destructive` |
| WP03 `AppSheet(state, onDismiss, …, title, showCloseButton, footer)` (draws in place) | app code: `AppSheet(visible, onDismissRequest, header = { AppSheetHeader(title) }, footer = …)`; a presenter: `AppSheetLayout(state, …)` under `@OptIn(ShellSeamApi::class)` | `showCloseButton = false` → `AppSheetHeader(title, trailing = null)` |
| WP03 `AppSheet(visible, onDismissRequest, detents: Set, title, showCloseButton, footer)` | `AppSheet(visible, onDismissRequest, state = rememberAppSheetState(detents.toList()), header = …, footer = …)` | |
| WP03 `enum SheetDetent { Medium, Large }`, WP04's enum, WP07's copy | sealed `SheetDetent` | `SheetDetent.Medium`/`.Large` references compile unchanged; `Set` → `List`; a `when` needs `else` |
| WP03 `AppSheetState(detents: Set)`, `remember { AppSheetState() }` | `rememberAppSheetState(detents: List)` | saveable (fixes D15) |
| `AppSheetState.largeFraction` | `expansionFraction`; the shell reads `SheetStack.recede(0)` | |
| `AppSheetState.dismissing`, `dismiss(onGone)` | `hide()`, `onHidden`; leaving is `isLeaving` | never `isVisible && targetDetent == null` (C1-05) |
| WP03 `SheetSceneStrategy`, `SheetPresentation`, `SheetKey`, `rememberSheetSceneStrategy` | gone; the presentation host owns its strategy | |
| WP04 `Presentation.Sheet(detents)`, string key `PRESENTATION_METADATA` | `Presentation.Sheet(SheetSpec(detents = …))`, `PresentationKey` | same in WP07 |
| WP04 `AppShell(sheetStrategy = …)`, `PresentationHost(sheetStrategy)` | parameters removed | |
| `PresentedFrame` for sheets | `PresentedSheetFrame` (counts in `live`) | covers keep `PresentedFrame` |
| `HostedOverlay(modal: Boolean, …)`, `key(entry)`, modal `clearAndSetSemantics {}` | `HostedOverlay(modal: () -> Boolean, …)`, call-site keys and saved state, `HideBeneathNode` (§11.1) | alerts and menus gain parking and restore too |
| `OverlayBackHandler` for sheets | `SheetBack` (B-36, B-37) | `OverlayBackHandler` stays for alerts and menus |
| WP04 `LocalTabVisible` read for parking | `EntryVisibility.isTabSelected` / `isUncovered` | `LocalTabVisible` stays for clocks |
| WP04 `guard()` dropping a push during any motion after a push | overlay motion → `Wait` (§11.3) | |
| DevTools panel, a `Dialog` (`wt/WP06/templates/android/core/devtools/.../panel/DevToolsPanel.kt:62-83` [C]) | an `AppSheet` with `[Medium, Large]` on a phone and the modal `End` side panel from Expanded width (§8) | WP06, after WP03 merges (§19.8); D-22 holds for every sheet in the app (C2-38) |
| Catalog demos (`catalog/sections/CatalogOverlays.kt:61-97`, `CatalogAdaptive.kt:135-154`, `CatalogFeel.kt:119`) | `catalog/sections/sheets/` (§14); the Feel page keeps one sheet for CP recordings | the Filters demo gets a `LazyColumn` (D13) |
| Settings language picker (`feature/settings/Settings.kt:424-437`, an `AppActionSheet` of every locale) | `AppChoiceSheet` with each language's native name as `label`, its localised name as `supportingText`, search, a checkmark, its list remembered | fixes A1, A2, A5, QA-14 (WP02-M3) |
| Tests `SheetTest.kt`, `RecompositionJourneysTest.kt:171-186`, `PostureTest.kt:70-78` | ported to the new API; every assertion kept | |
| WP08 `appSheet(isPresented:options:selection:onDismiss:)` at `00fd9b9` | kept; §18 adds the parity pieces | the template is hidden on the portal; no deprecation |
| WP07 `Presentation.kt` copy, `Spring(stiffness, damping)` sheet token | the common `SheetSpec`, `PresentationKey`; `Spring(response, bounce)` from the shared `sheet` section | §17 |

Defects closed: D1 (B-3, B-4), D2-D4 (B-45 to B-50), D5 (§7.4), D6 (B-39), D7 (§6.1), D8 (B-17), D9 (B-9), D10 (B-10,
R-11), D11 (D-19, §8), D12 (§9), D13 (D-25, `SheetColumn`, the catalog's lazy list), D14 (§4.4 springs through the
layout), D15 (§4.4 Saver), D16 (B-42); A1-A5 (§4.9, §13); N1-N5 and N10 (§11.2, B-58, B-59, §4.10), N6 (B-43), N7
(B-14), N8 (§9, §11.2), N9 and N11 (§11.3); QA-13 (§6.4 dim over the bar), QA-14 (§4.9), QA-17 (B-50, §7.2:
`qa/r1/DEFECTS.md:41`), QA-19 (B-50).

---

## 14. Catalog demos

The catalog's Sheets page (`catalog/sections/sheets/`) shows each demo over a colourful, scrollable backdrop so glass
and dimming read. Each demo's trigger has the test tag `catalog_sheet_<id>`, so `record.sh` and `perf.sh` can open it.
`SheetPreviews.kt` gives each demo a multi-size `@Preview` set (phone, foldable, tablet, desktop; light and dark).

| id | Demo | Shows | Covers |
|---|---|---|---|
| `basic` | Basic | `[Large]`, `AppSheetHeader` with close, a 60-row `LazyColumn` | card stack, rubber band at the top, scroll hand-off |
| `filters` | Filters | `[Medium, Large]`, a 40-row list, a pinned Apply footer; a Resizes/Scrolls switch; the same list scrolled with a wheel | B-27 to B-31 |
| `rename` | Rename | `FitContent`, a field and a sticky Save; a Lift/Expand switch; a 720 dp variant taller than the keyboard leaves | B-45 to B-47, C1-10 |
| `compose` | Compose message | `[Large]`, `SheetDismissConfirmation`; a variant with a raw `SheetDismissGuard` and a custom question | B-33, B-34 |
| `lab` | Detent lab | `Peek`, `Height(96 dp)`, `Fraction(0.25 / 0.5 / 0.75)`, `Custom("sliver")` with a slider its resolver captures, `Large`; buttons calling `animateTo`, `expand`, `collapse`, `updateDetents`; a draw-phase readout of `currentDetent`, `targetDetent` and `visibleTop` | §4.5, B-9, B-11, R-11 |
| `maps` | Maps-style | a fake map grid behind `NonModal(upThrough = Medium)`, `[Peek, Medium, Large]`, `dismissible = false`, a search header; a locate button riding the sheet through `Modifier.offset { … state.visibleTop … }`; the map's controls padded by `visibleHeight`; a tapped pin opens an `AppActionSheet` that stacks on the persistent sheet; Start panel on a wide window | B-51, B-52, B-54a, B-38 step-down, §8 |
| `route` | Route details | a dismissible `NonModal()` sheet over a scrolling list: taps beside it pass through, Back closes it; the nine-slice shadow. A note shows the bar's accessory (WP04) as the place for a mini player | B-33, B-37 (C2-11, C2-52) |
| `media` | Now playing | `[Medium, Large]`, `contentUnderGrabber` artwork to the top corners, the contrast grabber, a `containerBrush` gradient | §7.3, §7.6 (C2-26) |
| `styles` | Styles | Solid / Frosted / Glass side by side; a glass-tier switch (Live, Frosted, Solid); inset on/off; corner radius; grouped colours | §6.6, §7.6 |
| `headers` | Headers | title only; Cancel + Done; close only; subtitle; a long title at font scale 2; a header-less sheet with `paneTitle` | §7.4, §9, D5 |
| `stack` | Stack | sheet → sheet → sheet; a Glass sheet stacked on a Solid one (draws Frosted) | B-54 to B-57, §6.6 |
| `choice` | Choice | `AppChoiceSheet` with 30 choices, supporting text, a disabled row, accented names for search, None; a multi-select recipe built on `AppSheet` | §4.9 |
| `actions` | Actions | `AppActionSheet` with destructive, disabled and selected rows; anchored card on a wide window | §4.9, A1-A5 |
| `state` | Loading | skeleton → content (`FitContent` animates) → error with retry | B-10 |
| `destination` | Destination | opens the registered sample sheet; deep link `app://catalog/sheet`; its screen sets header, footer and grouped colours with the providers | §11.2, §4.8 |
| `steps` | Steps | `AppSheetNavHost` with three steps of different heights, each with its own header and footer | B-59 |
| `placement` | Placement | Automatic, Bottom, Center, Start, End forced, modal and non-modal; a modal End side panel; resize the window; a hinge | §8 |
| `hold` | Signature | `dragEnabled = false` with a signature pad; a variant with a chart in `Modifier.sheetDragExclusion()` | §4.6, B-16 |
| `motion` | Reduce motion | `SheetTransition.CrossFade`; a note that "Remove animations" makes every sheet change in one frame | B-63 |
| `a11y` | Accessibility | grabber actions and state, scrim label, focus moved in, trapped and restored with a keyboard | §9, B-42a |
| `back` | Predictive back | instructions; a guarded variant; a persistent step-down; Back during a present | B-38 to B-41 |

---

## 15. Tests

### 15.1 Unit (JVM, pure Kotlin; `core/designsystem/src/test/.../component/sheet/{layout,gesture,stack,surface,choice}/`)

- `DetentResolutionTest`: every detent type; R-1 to R-13; Medium inactive in Compact height and within 48 dp of Large;
  Custom returning null and a refreshed resolver; R-6 merges (Large's identity when listed); R-6a for `[FitContent]`,
  `[FitContent, Large]` and `[Large, FitContent]` with a 60-row list (same stop, Large-equivalent); `Peek` from the
  header and without one; R-8 fallback; R-9 nearest; R-10 Center and modal side panel semantics; R-12 hinge clamp; R-13
  `upThrough` mapping at 900 × 400; non-modal `restBottom` from `BarFrame.settledTop`; stacked `largeTop` (B-54).
- `SheetReleaseRuleTest`: every row of the B-22 table, plus boundary cases (exactly `F`, exactly `D`, exactly 0.48 at
  the release position, `y == o_n`, single-detent sheet), guard and `dismissible = false` reporting one attempt; the
  modal side panel along x.
- `SheetStretchTest`: monotonic, below the asymptote, 0.008 H at Large, the Medium-only asymptote, the blocked band.
- `SheetGeometryTest`: side inset, resting bottom and bottom edge at `e` = 0, 0.5, 1 for each case of §6.3; the clip is
  always a uniform round rect; it is constant in layer coordinates while attached; the header inset is constant.
- `CardStackMathTest`: scale, translation and corner at `e` = 0, 0.5, 1; the merged dim equals the two-rect composite to
  1/255 per channel for a grid of `u`, `l` in both themes; the n-layer fold equals stacked rects; `A` is monotonic in
  `u` and in `l`; `A = 0` draws nothing; `visibleBottom` at `e = 1` opaque and not.
- `SheetSaverTest`: every key round-trips (`peek` included); a missing or inactive key maps to the nearest active
  detent.
- `SheetShadowTest`: nine-slice source and destination rects for a range of surface sizes and spreads; corners never
  scale; a brush or a non-`SrcOver` blend mode is rejected.
- `SheetPlacementPolicyTest`: every row of §8 in order (first match: 800 × 360 is Compact height), window class versus
  pane class (B-13), the hinge halves (pane, anchor, larger, end in the layout direction; the half's own class), the
  modal side panel only from Expanded width.
- `ChoiceSearchTest`: case and accent folding ("Español" matches "espanol"), supporting text matched.

### 15.2 Compose UI (Robolectric, clock driven, T-6; `core/designsystem/src/test/.../component/sheet/`)

| Test | Asserts |
|---|---|
| `SheetApiShapeTest` | the design-system §4.1 blocks compile as written against stubs in the test source set (`FilterList`, `PlaceRow`, `RenameField`, `SearchHeader`, `PlaceList`, `TrackList`, string resources `orders_filters` and `lists_rename`). That `AppSheet(state = s, onDismissRequest = {}) {}` does not compile is asserted from the committed API dump: `AppSheet` has exactly the `visible` and `item` overloads (§19.1) |
| `SheetPresentTest` | frame 1 draws the hidden pose; under `LocalInspectionMode` the initial detent; the first moving frame ≤ 4 % of the travel and its frame index (N+2, or N+3 with a provider: B-1, B-2); a provider's header, footer and style are composed and `FitContent`/`Peek` include them before the first moving frame; a restored state snaps without a slide and `visible = false` after a restore shows nothing (B-6); `visible` false → true during the exit reverses with velocity (D1); after a person's dismissal with `visible` left true the sheet is not drawn, hit-tested or stacked (B-4); `state.hide()` under `visible = true` calls `onDismissRequest` once and `show()` under `visible = false` returns; disposing the call site while visible removes the sheet from the stack in that frame (B-4a); `onHidden` runs once per close; a `show()` while the owner arrives waits for `isSettled`; changing a value a `Custom` resolver captured moves a resting sheet to the new height (C2-28) |
| `SheetRestoreTest` (`StateRestorationTester`) | an in-screen sheet at Medium with a typed field and a scrolled list: both restore after recreation, as does the detent; a destination's provided footer restores its `rememberSaveable` into its own entry; a non-modal sheet dragged across `upThrough` keeps its field and scroll, and a first-composition counter in its content stays at 1 (C1-03, C2-03) |
| `SheetGestureTest` | drag follows 1:1; a touch on the content during a settle catches the sheet (D8), and a tap that stops it does not click the row (B-17); a catch followed by a lift without a drag settles to the stopped target with no haptic; a 3000 dp/s flick from Medium plus a touch 16 ms later is not caught; a retarget keeps velocity, per-frame deltas never jump (D7); `animateTo` during a hide is ignored (D9) and during a finger drag returns without moving; a drag started during `animateTo` takes the sheet; a resize mid-settle lands on the detent (D10); the haptic plays once, on the frame 2 % remains; a row's pressed state appears within the tap timeout + 1 frame (B-21); `dragEnabled = false` holds the sheet; a drag inside `sheetDragExclusion()` never moves it; with `touchscreen = true` a wheel notch never resizes; stylus handwriting in a field does not move the sheet [U: B-20]; a tap at (centre, 20 dp) reaches the header, not the grabber (C2-30) |
| `SheetNestedScrollTest` | Resizes grows then scrolls; Scrolls scrolls first; momentum never moves the sheet; the last row is reachable at Medium with Scrolls (B-31); a mouse gesture never resizes (B-27); an accessibility scroll never resizes it (B-30); a list pulled down at its top on a non-dismissible sheet bands the sheet and the list does not stretch; a scroll during the exit leaves the offset unchanged; a scroll during `show()` consumes nothing; `onPreFling` runs the B-22 rule (C1-07) |
| `SheetDismissTest` | drag, flick, scrim, Back key, Escape, close button, TalkBack's dismiss and `scope.dismiss()` each call `onDismissRequest` exactly once; a flick plus a Back 16 ms later calls it once and a destination pops one entry; code paths call it never, except B-4's `hide()`; the non-modal dismissible column of B-33 |
| `SheetGuardTest` | every path of the B-33 table with a guard: the sheet stays and `onAttempt` gets the right source once per attempt; `SheetDismissConfirmation` stacks its question and Discard calls `onDiscard`; a disabled guard registers no handler; `dismissible = false` on a modal sheet: Back is consumed, never passed through; on a persistent non-modal sheet: Back steps down, and at the lowest detent the handler is disabled; a dismissible non-modal sheet closes on Back at every detent and lets a tap beside it through |
| `SheetBackTest` (`DirectNavigationEventInput`) | eased progress → layer `scaleX/Y`; content counter-scale; Center scales to 0.90 about its centre; the modal side panel's side-container pose; cancel returns to 1; commit closes from Medium and from Large in one gesture; a gesture started 60 ms after `show()` stops the present, and cancelled lands on its detent, committed leaves without a jump (B-38a); an in-screen Back burst during the exit is dropped and nothing beneath pops; with the IME source showing a keyboard, Back hides the keyboard and nothing else (branch 0); with the IME up and the default input re-registered after it, the guard is still asked and nothing beneath pops (B-44); only the top sheet in stack order takes Back |
| `SheetImeTest` (a `SheetImeSource` double on the test clock) | Lift: the footer rides the keyboard, a Medium sheet rises with it frame by frame and stops at `largeTop`; the content box is re-measured to the visible region at the target, at most twice per transition; a 720 dp `FitContent` form with a 300 dp keyboard up and down: 0 anchor writes, 0 detent animations; a focused field at Medium ends above the keyboard (D4); content without a footer clears the navigation bar (D3); the IME is counted once (D2); Expand goes to the top detent and back; a drag past 24 dp hides the keyboard; a non-modal peek with focus in a field beneath does not move, and with focus in its header field rises with 0 anchor writes; Center at 900 × 1000 and a Start panel at 1280 × 800 lift per B-45a |
| `SheetModalityTest` | a tap beside a peek reaches a clickable in the app content box; above `upThrough` it does not (C1-04); the dim draws only above; 0 recompositions per crossing; a pane-bounded sheet in a list-detail scene dims and blocks only its pane and hides only that pane from accessibility; at 900 × 400, `upThrough = Medium` maps per R-13; an explicit Center non-modal sheet with `upThrough` blocks |
| `SheetA11yTest` | touch-opened modal sheet: the surface is focused, a focused field beneath loses focus and its IME target hides, Tab cannot leave the sheet, Escape closes it with source `Escape` (and a focused search field consumes Escape first), focus is restored on close in keyboard mode (B-42a); pane title from each source and its precedence (C2-13); traversal group and order; `heading()` on the title; built-in expand, collapse and dismiss actions and the click label (moved to the surface when no grabber is drawn), kept by a custom `grabber` lambda (C2-31); scrim label and order; beneath hidden from the first moving frame and restored at the dismissal decision; the close button absent on a non-dismissible sheet without a guard; at Medium, Tab past the last visible row expands the sheet and the row ends visible (B-47a); with touch exploration on, page-forward visits all 60 rows at Medium |
| `SheetAdaptiveTest` | 1280 × 800 modal → Center 600 dp with 32 dp gutters even when opened from a Compact pane (B-13, D-20); `containerClass` inside a Start panel and a Center sheet on 1280 × 800 is Compact, inside a non-modal Bottom sheet on 900 × 1000 Medium (§11.4); 900 × 400 → Bottom 640 wide, one detent; a vertical hinge places the sheet in the presenting pane's half with that half's class; a horizontal hinge clamps partial stops; unfold at Medium → Center records Medium, fold → back on Medium (R-10, R-11); an explicit modal `End` from Expanded width is the side panel |
| `SheetStackTest` | the covered sheet's scale and lift follow the top sheet; the covering `largeTop` is the lifted top + 10 dp; the covered grabber hides; only the top takes input; at most two covered sheets drawn; a persistent sheet at its peek plus an action sheet from the screen: the persistent sheet is dimmed, takes no input, is hidden from accessibility, and Back closes the action sheet first (B-54a); a Glass sheet over a cover or stacked on a sheet draws Frosted (§6.6) |
| `SheetParkingTest` | tab not selected → parked on the switch frame and back with 0 recompositions and 0 re-records in the sheet (C1-12); a push over the owner → an animated park from the push's first frame, never instant, and an unpark on pop (C2-04); a presentation above the host parks it; a leaving sheet finishes its exit drawn; parked sheets are out of the stack and take no Back; during a predictive pop of the owner (`DirectNavigationEventInput` on its stack) the sheet follows the owner's pose (or, in the fallback, its placed `y` follows the eased progress) and is never drawn full size over the card; cancel restores it |
| `SheetDrawCountTest` | §10.4's draw budgets with counting draw modifiers: 0 sheet-layer draw executions over 30 attached drag frames; no grabber, header, content or footer re-record over 30 floating frames; 0 `RecordSceneNode` records and 0 runs of a counting draw lambda in a fake shell's content over 30 drag frames with the card stack (C1-01, C1-02) |
| `SheetReduceMotionTest` | with `Settings.Global.ANIMATOR_DURATION_SCALE = 0`: present, dismiss, settle and the card stack reach their end values in one frame and a drag still follows the finger; with the scale at 1, `CrossFade` takes 200 / 120 ms (B-63) |
| `SheetShadowTest` (UI) | no shadow on a modal Bottom sheet; one when its scrim alpha is below 0.2; a non-modal sheet dragged 30 frames creates no new shadow bitmap; no draw at alpha 0; none in dark theme |
| `AppActionSheetTest`, `AppChoiceSheetTest` | selection role and checkmark; scroll past the 0.6 cap; Back during the exit consumed (A3); the anchored card at Medium width creates no window (the root view count is unchanged), places against the anchor, hides beneath from accessibility, carries its pane title and dismiss action, focuses its first action and returns focus to the anchor (C2-14); rows grow at font scale 2.0; `AppChoiceSheet` with a non-Parcelable `T` and the default key composes and restores (C2-36); accent-insensitive search; `onSelectNone` adds the None row; choosing calls `onSelect` then `onDismissRequest` |

One end-to-end IME case runs as an instrumented test on an emulator (`SheetImeInstrumentedTest`, `connectedCheck`,
run by WP04's perf job), because a JVM test cannot animate the IME; §16.1's `sheet_ime` row stays the phone gate
(C2-43). `SheetA11yInstrumentedTest` runs the catalog demos with `enableAccessibilityChecks()` (ATF), suppressing only
the documented grabber contrast check (§7.3).

### 15.3 Navigation (WP04, `core/navigation/src/test/`, a real `NavDisplay`)

- `SheetSpecShapeTest`: the destination block of §4.1 compiles against stubs (`ShareKey`, `ShareRoute`).
- `SheetSceneTest`: a lower sheet keeps its state and never re-presents across stack changes (N1); a pop slides out
  before the entry leaves; popping the only sheet keeps the presentation host composed through the slide, and a Back
  during it is counted by `presented.gate` (C1-06); `onRemove` before the first composition returns; metadata through
  `PresentationKey`; a sheet over a cover recedes the cover; a cover submitted over a sheet closes the sheet first,
  then rises (B-58); restore after process death keeps the detent with no slide; a cold deep link slides in; a
  destination footer's `viewModel()` is its screen's instance and is cleared on pop (C1-15).
- `SheetGateTest`: a Back burst during a destination sheet's dismissal is counted, coalesced, and dropped at the
  presentation root; with `lastChange = Push` and a sheet presenting, a submitted push is applied after the sheet
  reaches 99 %, not dropped (C1-08); an in-screen sheet presenting holds its tab's push (B-15).
- `SheetBackRoutingTest`: while a modal sheet is up, the tab's and the presentation host's `StackBackHandler`s are
  disabled as B-44 says; the presentation host's is enabled again while its sheet leaves.
- `SheetNavigationTest`: an ordinary key submitted from a presented sheet closes the sheets, then pushes on the tab
  (B-58).
- `AppSheetNavHostTest`: inner Back pops the inner stack first; from the root it closes the sheet; during a step
  transition the uncovered step's header, footer, guard and pane title win and the headers cross-fade; a `FitContent`
  sheet takes the target step's height; a covered step's guard never blocks Back.
- `PresentationSemanticsTest`: the tab hosts are hidden from accessibility under a modal presentation (N8).
- `CardStackShellTest`: the shell content recomposes 0 times from Medium to Large; the status bar flips once per
  crossing through `WindowInsetsControllerCompat` only; the window background is black only while receded, by mutating
  the same `ColorDrawable`; no receding at Medium width and up.

### 15.4 Recomposition journeys

`RecompositionJourneysTest` gains one journey per row of §10.4, with those budgets.

### 15.5 Screenshots (T-5, `com.android.compose.screenshot`)

Previews render the sheet at its initial detent through `LocalInspectionMode` (§4.4); card-stack cases are posed with
`snapToFraction`. **Acceptance:** every image differs from its no-sheet baseline (C2-41). Light and dark, font scale
1.0 and 1.5 (and 2.0 where listed), LTR and RTL:

| Size (dp) | Cases |
|---|---|
| 400 × 1000 | Medium floating (Solid, Frosted); Large attached with the card stack (presenter posed at `e` = 0.5 and 1); FitContent; header variants; footer; stacked sheets; a guard attempt with an action sheet over it; non-modal peek above a fake bar; `[FitContent]`, `[FitContent, Large]` and `[Large, FitContent]` with a 60-row list, identical (R-6a); a bare `AppText` body at Medium, floating, keeping its gutter (QA-17); `Peek` at font scale 2.0; `contentUnderGrabber` artwork; action-sheet rows at font scale 2.0 |
| 400 × 1000, 3-button insets | Medium attached (no float), last row clear of the buttons; a dark container in light theme with light navigation icons (§7.9) |
| 400 × 500 | Medium and Large (Medium active at 500 dp) |
| 400 × 400 | Compact height: one detent |
| 900 × 400 | Compact height, 640 dp centred; at font scale 2.0 with a header and a footer (the footer unpinned) |
| 610 × 1000, 900 × 1000 | Center form sheet; at 900 × 1000 with an IME inset (lifted) |
| 1280 × 800 | form sheet; Start panel; modal End side panel; anchored action card at font scale 2.0 |
| hinges (injected posture) | horizontal: partial stops below the fold; vertical: the sheet in one half |

Live glass cannot render in a screenshot test; it is verified on the phone (§16).

### 15.6 Rules, owners and tests

Every rule is cited by its id in a test's name or KDoc; the owner package implements it. Reviewers check this table
against the test sources (C2-61).

| Rules | Owner | Tests |
|---|---|---|
| R-1 to R-13 | WP03 | `DetentResolutionTest` |
| B-1 to B-11, B-4a, B-1a | WP03 | `SheetPresentTest`, `SheetRestoreTest` |
| B-12, B-13, B-14, B-14a | WP03 (host, parking, pose application); WP04 (`EntryVisibility`, the pose, `PaneBounds`) | `SheetRestoreTest`, `SheetParkingTest`, `SheetAdaptiveTest` |
| B-15 | WP03 (reports); WP04 (overlay motion → `Wait`) | `SheetGateTest` |
| B-16 to B-23 | WP03 | `SheetGestureTest`, `SheetReleaseRuleTest`, `SheetDrawCountTest` |
| B-24 to B-32 | WP03 | `SheetStretchTest`, `SheetNestedScrollTest` |
| B-33 to B-35 | WP03 | `SheetDismissTest`, `SheetGuardTest` |
| B-36 to B-42a | WP03 | `SheetBackTest`, `SheetA11yTest` |
| B-43 | WP03 (in-screen); WP04 (destination, `live`, gate) | `SheetBackTest`, `SheetSceneTest`, `SheetGateTest` |
| B-44 | WP03 (branch 0); WP04 (`StackBackHandler` blocking) | `SheetBackTest`, `SheetBackRoutingTest` |
| B-45 to B-50, B-47a | WP03 | `SheetImeTest`, `SheetImeInstrumentedTest`, `SheetA11yTest` |
| B-51 to B-53 | WP03; WP04 (`BarFrame`, pane blocking) | `SheetModalityTest` |
| B-54 to B-57, B-54a | WP03 (stack); WP04 (covers' presenter) | `SheetStackTest`, `SheetSceneTest` |
| B-58 to B-60 | WP04 | `SheetNavigationTest`, `AppSheetNavHostTest` |
| B-61 | WP03 | `SheetGestureTest` |
| B-62 | WP03, WP04 | `SheetRestoreTest`, `SheetSceneTest` |
| B-63 | WP03 | `SheetReduceMotionTest` |
| B-64 | WP03, WP04 | §16.1 trace check, §21 |
| §6.4, §6.6, §7.9 | WP03 (math, presenter); WP04 (shell, bars); WP05 (glass) | `CardStackMathTest`, `CardStackShellTest`, `SheetStackTest`, §16.1 |

---

## 16. Phone gates, recordings and feel acceptance

### 16.1 `perf.sh` rows (WP04 adds the scenarios to `perf/perf.py` and the rows to `perf/gates.toml`)

Release builds with their baseline profile, as every row (`perf/gates.toml:1-15` [C]). Scenarios open the sample
destination by `sheet_open` (and the catalog's `catalog_sheet_maps` for the non-modal rows, `solid` only). **Ratchet**
rows record a baseline on QA2's first run and fail on regression; every GPU row is a ratchet until then, because none
rests on a measurement yet, and CP1 measured two screens at GPU p90 12 ms on this phone (`decisions/CP1.md:109-111`;
C1-18). `janky_pct` rows for scenarios with a composing frame count motion frames only, as the push/pop rows separate
composition from motion with a hitch metric (`perf/gates.toml:114-131` [C]).

| Metric | solid | standard | lean | Other | Scenario |
|---|---|---|---|---|---|
| `sheet_present.janky_pct` (motion frames) | 2.0 | 2.0 | 2.0 | smoke `{ add = 0.5, scale = 0.2 }` | open and close 10 times |
| `sheet_present.hitch_ms_per_s`, `.tap_to_motion_ms` | ratchet | ratchet | ratchet | | the same; `tap_to_motion` from the tap's input event to the first moving frame (B-2: N+2 or N+3) |
| `sheet_present.compose_frame_ms.median` / `.max` | ratchet, then 8.0 / 16.0 once SMOOTHNESS S2 lands | same | same | | the frame that composes the sheet |
| `sheet_drag.janky_pct` | 1.0 | 1.0 | 1.0 | | grabber drags Medium ↔ Large, slow and fast, 10 each |
| `sheet_drag.gpu_p90_ms` | ratchet (target 7.0) | ratchet | ratchet | | |
| `sheet_drag.p99_ms` | 16.0 | 16.0 | 16.0 | | a drag is L1 (+ D1 floating) + the presenters' dim nodes |
| `sheet_dismiss.janky_pct`, `.dismissed` | 1.0, true | 1.0, true | 1.0, true | | flick at 3 H/s from Medium |
| `sheet_back.gesture_first_frame_ms.median` | 8.0 | 8.0 | 8.0 | | as `pushpop_gesture` |
| `sheet_back.janky_pct` | 2.0 | 2.0 | 2.0 | | commit and cancel, through `UiDevice` swipes; one gesture 60 ms after opening (B-38a) |
| `back_during_sheet.janky_pct`, `.no_double_exposure` | 2.0, true | | | | a pushed screen with a persistent sheet at its peek; a back swipe held at 50 %: a mid-gesture screenshot shows the sheet inside the shrinking card (or parked), never full size over it (B-14a; C1-17) |
| `sheet_ime.janky_pct`, `.footer_above_ime` | 2.0, true | 2.0, true | 2.0, true | | focus `sheet_field`; bounds from UI Automator |
| `sheet_scroll.janky_pct`, `.gpu_p90_ms` | 0.5, ratchet | 0.5, ratchet | 0.5, ratchet | | a list at Large, as `scroll_root`; QA2 records it once with the visible-band clip disabled to measure its gain (§6.4) |
| `sheet_cardstack.gpu_p90_ms` | ratchet (target 8.0) | ratchet | ratchet | | Medium → Large with the shell receding |
| `sheet_cardstack.no_offscreen` | true | | | | trace with `atrace gfx view hwui` and `setprop debug.hwui.skia_atrace_enabled true`: no slice whose name contains `saveLayer` larger than a bar in the expansion and drag frames; the stencil half needs Skia's clip slices, which are [U] (§21): if they do not appear, only the `saveLayer` half gates |
| `sheet_cardstack.no_view_layout` | true | | | | trace: no `traversal` slice with a layout pass during a Medium → Large drag (§7.9; C1-29) |
| `sheet_nonmodal.janky_pct`, `.tap_passes_through` | 1.0, true | | | | drag peek ↔ Medium; tap the map beside the sheet |
| `sheet_rapid_back.alive`, `.lands_on_presenter` | true, true | true, true | true, true | | three Back keys within 150 ms of opening; and popping the only destination sheet (C1-06) |
| `sheet_push_from.janky_pct` | 2.0 | 2.0 | 2.0 | | tap a row in a sheet that closes it and pushes a screen (B-60) |
| `tabs_peek.switch_frame_ms.max` | as the `tabs` scenario's row | | | | `perf.sh tabs` with a peek sheet on the first tab: switch away and back (C1-12) |
| `sheet_restore.detent_restored` | true | | | | `am kill` at Large, relaunch; beside `restore.restored` |
| `sheet_recreate.text_kept`, `.detent_kept` | true, true | | | | the rename sheet open with typed text; switch night mode (the activity recreates; rotation no longer does) (C2-03) |
| `sheet_idle.frames` | 0 | 0 | 0 | | 5 s with a sheet resting at Medium over **static** content: no field, caret, shimmer or spinner |
| `recompose.sheet_drag.max` | 0 | | | | frames that recompose while dragged |
| `recompose.sheet_settle.max` | 1 | | | | |
| `recompose.sheet_back_progress.max` | 1 | | | | only the sheet's layer wrapper, at the gesture's start and end (B-64); 0 in content |
| `recompose.sheet_ime.max` | 0 | | | | content frames that recompose during the IME animation |
| `meminfo.sheet_cycle_pss_growth_mb` | ratchet | | | | 20 open/close cycles (also catches a growing shadow cache) |
| `sheet_large.janky_pct` | | | | `large` 1.0 | form sheet present and close; Start panel drag; modal End panel (joins `LARGE_SCENARIOS`, `perf/perf.py:50`) |
| `sheet_drag_glass.janky_pct`, `.gpu_p90_ms`, `.no_demotion` | | | | `glass_live` 1.0, ratchet, true | the drag scenario with `SheetMaterial.Glass` (WP05 owns the column) |
| `sheet_cardstack.gpu_p90_ms`, `.no_scene_rerecord` | | | | `glass_live` ratchet, true | Medium → Large over the glass shell: neither the scene's RenderNode nor the bar's backdrop is re-recorded (§6.4, §11.5; C1-01, C1-14) |

### 16.2 Recordings (QA2, `record.sh`, light and dark, into `perf/sheet/`)

Drags are recorded with `adb shell settings put system pointer_location 1`, so the finger is in the strip. Present to
Medium; Medium → Large by drag and by flick; a one-detent flick from Large; flick dismiss from Medium; rubber band at
Large and on a Medium-only sheet; a list pulled past its top on a non-dismissible sheet; a non-dismissible pull; a
guard attempt; predictive back commit and cancel from Medium and Large, and during a present; the Back key; the IME
rising and falling at Medium; the card stack; three stacked sheets; the Maps-style sheet (peek, pass-through tap,
step-down Back, an action sheet stacked on it, a back swipe of its screen); parking on a push and a tab switch; the
glass tiers; font scale 2; RTL; landscape (Compact height); 3-button navigation with a dark container; a tablet form
sheet, Start panel and modal End panel (emulator: relative evidence only); tabletop and book (emulator). Also a
side-by-side against an iOS 26 reference for §21's items.

### 16.3 Feel acceptance from the strips

| Check | Pass |
|---|---|
| First moving frame of a present | ≤ 4 % of the travel (`cp1_edge.py`, CP1 §1 condition 1) |
| A drag | the sheet's top under the `pointer_location` trace within one frame of lag, on every frame |
| A release | no frame in which the sheet moves against its release velocity, except a settle that reverses past a stop |
| Over-pull at Large | the top never goes above `largeTop − 0.008 H` |
| Expansion | content text never moves sideways; the floating inset closes smoothly; no corner pops |
| Dim | never lighter on a later frame while the sheet rises, light and dark; a persistent sheet under a modal one is dimmed with the rest |
| Card stack | the presenter peeks 10 dp above the sheet at Large; black backing, no window colour at the corners or in the floating gaps; light status icons |
| Shadows | no dark band inside any surface, light or dark; a non-modal sheet's shadow corners keep their shape while it resizes |
| Haptic | the perf build's `sheet_arrival` trace section (B-61) within ±1 frame of the frame the strip shows the sheet landing |
| Predictive back | text never squashes; the sheet never moves up during the gesture; an owner's back card never has a full-size sheet over it |
| Parking | no pop-in: the sheet slides with the push and back with the pop |
| IME | the focused field above the keyboard on the last frame of the rise; a `FitContent` form does not change height while the keyboard moves |

### 16.4 Accessibility acceptance (QA2, on the Redmi; C2-15)

With TalkBack on, for each catalog demo:

- opening the sheet moves TalkBack into it and speaks the pane title;
- swipe order: grabber, header, content, footer, then "Close sheet";
- the actions menu offers Expand, Collapse and Dismiss where they apply, and each works; the grabber's double tap
  says what it will do;
- nothing beneath is reachable by swipe or by explore-by-touch, except the Maps demo below `upThrough`;
- every row of the 60-row list is reachable at Medium (B-27, B-31, B-47a);
- after close, focus lands at or near the trigger [U: recorded as found];
- a guarded sheet's confirmation is announced;
- TalkBack's Back with the keyboard up closes the keyboard first, and then the sheet, never the screen beneath (B-44).

With Switch Access: the grabber's actions are reachable. With font scale 2.0 and display size largest: nothing
clipped, the footer unpinned where §4.8 says. On an emulator with a hardware keyboard: Tab is trapped, Escape closes
(and a focused search field takes Escape first), focus is restored. `SheetA11yInstrumentedTest` runs ATF on every demo
(§15.2). Results go to `perf/sheet/a11y.md`.

---

## 17. Compose Multiplatform parity (WP07-B)

- **Same API, same package**, in `core/designsystem/src/commonMain/kotlin/com/base/app/core/designsystem/component/sheet/`.
  WP07 has no overlay component, overlay host or presentation host today (`current-state.md` §6); WP07-B ports WP03's
  overlay host and sheet (with revision 2's call-site keys and saved state, which `SaveableStateHolder` supports in
  common code), and WP04's presentation-host sheet path, to common code. Defaults that name strings use
  `stringResource(Res.string…)`, not `R.string` (C2-60); the common `SheetSpec.paneTitle` is a `StringResource?`.
- **Shared verbatim:** the pure files (`DetentResolution`, `SheetGeometry`, `SheetPlacementPolicy`, `SheetReleaseRule`,
  `CardStackMath`, `ChoiceSearch`, the Saver encoding); the §15.1 tests run in `commonTest` (camelCase names, K-8).
- **Tokens:** WP07 expresses springs as stiffness and damping (`wt/WP07/.../theme/tokens/DesignTokens.kt:100-119`
  [C]). Its generated tokens move to `Spring(response, bounce)` from the same `springs` and `sheet` sections, so values
  cannot drift.
- **API check:** the common module's klib dump, limited to the sheet package, is diffed against Android's API dump; the
  only differences are the ones this section names (§19.4).
- **`expect`/`actual`:**

| Piece | Android target | iOS target |
|---|---|---|
| `Threshold` haptic | `View.performHapticFeedback`, as Android | `UISelectionFeedbackGenerator.selectionChanged()` [U: against `UIImpactFeedbackGenerator(.light)`] |
| Display corner radius for the card stack's `R₀` | `WindowInsets.getRoundedCorner` (API 31+) | no public API: `0.9 · safeDrawing.top` when that is > 20 dp ([M] Flutter's heuristic, `sheet.dart:14-25`) |
| Status bar over the card stack | `WindowInsetsControllerCompat` | the hosting view controller's `preferredStatusBarStyle` [U: the CMP hook] |
| Frame rate | the toggled `preferredFrameRate` (B-64) | `CADisableMinimumFrameDurationOnPhone = YES` in `Info.plist` for 120 Hz [A] |
| Clip path | float-argument `android.graphics.Path.addRoundRect` (no allocation) | Compose `Path.addRoundRect(RoundRect)`: one small allocation per floating drag frame, accepted on iOS only [U: no GC hitch] |
| Shadow bitmap | `DropShadowPainter` into an `ImageBitmap` | the same where the CMP version ships it [U]; otherwise a Skia blur mask filter, rasterised once |
| Back | `navigationevent` (WP07-A rebuilt the 1.2.0 pieces it needs, `handoff/WP07-A.md:39-42`) | **no Back handler is registered**: no iOS sheet shrinks on an edge swipe, and CMP may route an edge swipe into navigation-event Back [U]; the sheet closes by drag, the dim, the close button and Escape on a hardware keyboard |
| IME | `SheetImeSource` over `WindowInsetsRulers.Ime` | `SheetImeSource` over `WindowInsets.ime` [U: animation fidelity]; the same placement-only lift |
| Accessibility | TalkBack through Compose semantics | VoiceOver through CMP's iOS accessibility bridge: how `paneTitle`, `hideFromAccessibility` and the built-in actions map is [U], read in CMP's iOS accessibility sources by WP07-B (§21) |
| Glass | Kyant through the `SheetBackdropHolder` | no Kyant: Glass draws Frosted; the holder's backdrop stays null [D] |

- **Navigation:** JetBrains `navigation3-ui` 1.1.2 is androidx 1.1.7 (`wt/WP07/templates/multiplatform/gradle/libs.versions.toml:15-20`
  [C]). `OverlayScene.onRemove` exists there [V], `NavMetadataKey` exists [V] (judge-3 §5), and so does
  `NavEntryDecorator.onPop` [V] (`NavEntryDecorator.kt:70-88`): use the typed `PresentationKey` in common code and drop
  any string-key fallback.
- **Card stack:** the same `cardStackPresenter` in the common shell.
- **Gates:** the Android target on the Redmi with §16.1's `solid` rows for present, drag, back and IME; the iOS target
  is relative evidence only (no device).

---

## 18. SwiftUI parity (WP08, iOS 17+)

The native sheet already carries the motion, the card stack, the keyboard, VoiceOver and the glass; the template maps
the same options onto it and does not fight it (`MOTION-PLAN.md:526-556`). Revision 2 rebases this section on WP08's
head `00fd9b9` (C2-39), which already ships most of it under the spec's names; what follows is that API, plus the
additions that give every §4.1 example and preset a one-to-one Swift form.

**Shipped at `00fd9b9`** (kept):

- `AppSheetOptions(detents: Set<AppSheetDetent> = [.large], modality: .modal, contentInteraction: .automatic,
  dismissible: true, material: .automatic, placement: .automatic, grabber: Visibility = .automatic)`
  (`swift/AppSheetOptions.swift:5-43`); `grabber: .automatic` shows it with more than one detent (`:49-51`).
- `AppSheetDetent`, a struct: `.large`, `.medium`, `.fitContent` (measured with `onGeometryChange`), `.fraction(_)`,
  `.height(_)`, `.custom(Detent.self)` (`swift/AppSheetDetent.swift:5-58`).
- `AppSheetModality`: `.modal`, `.nonModal(upThrough: AppSheetDetent? = nil)` →
  `presentationBackgroundInteraction` (`swift/AppSheetModality.swift:4-23`).
- `AppSheetContentInteraction`: `.automatic` (resizes a modal sheet, scrolls a non-modal one, as Android's B-27),
  `.resizes`, `.scrolls` (`swift/AppSheetContentInteraction.swift:3-12`, `AppSheetOptions.swift:53-62`).
- `AppSheetMaterial`: `.automatic` (from iOS 26 the system's Liquid Glass, before it the elevated surface), `.solid`,
  `.frosted`, `.glass` (`swift/AppSheetMaterial.swift:2-15`, `AppSheet.swift:143-176`).
- `AppSheetPlacement`: `.automatic`, `.center`, `.leading`, `.trailing`, applied from iOS 27 behind
  `#if compiler(>=6.4)` (`swift/AppSheetPlacement.swift:3-17`, `AppSheet.swift:115-140`).
- `appSheet(isPresented:options:selection:onDismiss:content:)`, `appSheet(item:options:selection:onDismiss:content:)`
  and `appSheetPresentation(_:selection:)` for content something else presents, such as an `.inspector` on a phone
  (`swift/AppSheet.swift:16-50`).
- `appSheetDismissGuard(enabled:onAttempt:)`: `interactiveDismissDisabled` plus a `DismissAttemptForwarder` placed in
  front of the presentation controller's delegate, forwarding every call to SwiftUI's own delegate so the binding and
  the detent still follow (`swift/AppSheetDismissGuard.swift:12-14`, `DismissAttemptForwarder.swift:3-40`). This
  answers revision 1's [U] about replacing the delegate (C2-40).

**Added by revision 2** (WP08):

```swift
extension AppSheetOptions {
    /// The detent the sheet opens at; nil keeps SwiftUI's choice (the smallest). Android's default is
    /// the first listed detent, so cross-platform features set it.
    public var initialDetent: AppSheetDetent?          // new stored property, default nil
}

extension View {
    /// The sheet's title row: an inline navigation title with a close button, as AppSheetHeader on
    /// Android. iOS 26 draws the buttons as glass.
    public func appSheetHeader(_ title: LocalizedStringKey) -> some View
    /// The same with the sheet's own items (Cancel in `.cancellationAction`, Done in
    /// `.confirmationAction`) instead of the close button. Two overloads, because Swift cannot give an
    /// opaque-typed builder parameter a default.
    public func appSheetHeader<Items: ToolbarContent>(_ title: LocalizedStringKey,
                                                      @ToolbarContentBuilder items: () -> Items) -> some View
    /// Pinned at the sheet's visible bottom, above the keyboard: AppSheet's footer.
    public func appSheetFooter(@ViewBuilder _ footer: () -> some View) -> some View
    /// Asks before a swipe or the close button discards changes: Discard / Keep Editing.
    public func appSheetDismissConfirmation(isEnabled: Bool, title: LocalizedStringKey,
                                            discardLabel: LocalizedStringKey, onDiscard: @escaping () -> Void) -> some View
    /// AppActionSheet: the system's confirmation dialog, anchored to its source on iPad.
    public func appActionSheet(isPresented: Binding<Bool>, title: LocalizedStringKey?,
                               actions: [AppSheetAction]) -> some View
    /// AppChoiceSheet: a searchable list with a checkmark, in a sheet at [.fitContent, .large].
    public func appChoiceSheet<Choice: Hashable>(isPresented: Binding<Bool>, choices: [Choice],
                                                 selection: Binding<Choice?>, label: @escaping (Choice) -> String,
                                                 supportingText: ((Choice) -> String?)? = nil) -> some View
}
```

- `appSheetHeader` is `NavigationStack` + `.navigationTitle` + `.navigationBarTitleDisplayMode(.inline)` + `.toolbar`;
  `appSheetFooter` is `.safeAreaInset(edge: .bottom)` (iOS 26 `safeAreaBar` [U]).
- `appSheetDismissConfirmation` uses iOS 27's `dismissalConfirmationDialog` behind the same compiler gate
  (`research.md:143` [A]), and before it the shipped guard plus a `confirmationDialog`.
- `appActionSheet` maps to `confirmationDialog`; `appChoiceSheet` to a `List` with a checkmark and `.searchable` in an
  `appSheet` (`AppSheetOptions(detents: [.fitContent, .large])`).
- The Swift token writer emits only `springs.sheet` and `radii.sheet`, which the template reads (§6.2; C2-45).

**Parity matrix.** "Same": both platforms behave alike. "Platform": each follows its platform, on purpose. "n/a":
not offered on that platform, with the reason. CMP iOS is the Compose sheet on iOS (§17).

| Option or preset | Android | CMP iOS | SwiftUI |
|---|---|---|---|
| Detents | `List<SheetDetent>`, default `[Large]` | same | `Set<AppSheetDetent>`, default `[.large]`: same set; `Peek` → n/a, because SwiftUI's detent context has no header height (use `.fitContent` on a header-only view, or `.height` from an `@ScaledMetric`, `AppSheetDetent.swift:35-36`) |
| Initial detent | `initialDetent`, default the first listed | same | `initialDetent`, default nil = SwiftUI's smallest: **same when set**; features that care set it |
| Modality, `upThrough` | `SheetModality` | same | same (`presentationBackgroundInteraction`); platform: an iOS non-modal sheet covers the tab bar, stays over pushes and tab switches, and blocks other sheets from that screen, while Android parks it (B-14) and stacks (B-54a) |
| Content interaction | per gesture (B-27) | same | same mapping (`AppSheetOptions.swift:53-62`) |
| `dismissible` | yes | same | `interactiveDismissDisabled` |
| Guard, confirmation | `SheetDismissGuard(onAttempt(source))`, `SheetDismissConfirmation` | same | `appSheetDismissGuard(onAttempt)` (no source: UIKit reports only swipe and tap attempts), `appSheetDismissConfirmation` |
| `scrimTapDismisses` | yes | same | n/a: UIKit ties the tap outside to interactive dismissal |
| `dragEnabled` | yes | same | n/a: SwiftUI has no switch that holds a sheet on its detent |
| `recedesPresenter` | yes | same | n/a: the system owns its card stack |
| `contentUnderGrabber`, `containerBrush` | yes | same | platform: the grabber always draws over content; `presentationBackground` takes any `ShapeStyle` before iOS 26 |
| `keepContent` | yes | same | n/a: SwiftUI owns the presentation's lifetime |
| Material | Automatic = the style's token (Solid; Glass in the Glass style) | Glass → Frosted (no Kyant) | Automatic = the system's (Liquid Glass from iOS 26): platform |
| Placement | Automatic, Bottom, Center, Start, End (§8) | same | automatic, center, leading, trailing from iOS 27; Bottom → n/a (the phone's only form; iPad offers none) |
| Transition | Slide, CrossFade | same | platform: iOS 27 `navigationTransition(.crossFade)` [U] |
| Keyboard Lift / Expand | yes | same [U: iOS IME fidelity] | platform: the system lifts the sheet |
| Grabber | slot, hidden by the B-rules | same | `presentationDragIndicator` |
| Header, footer | slots and providers | same | `appSheetHeader`, `appSheetFooter` |
| Pane title | `paneTitle`, header title | [U: VoiceOver mapping] | platform: VoiceOver reads the navigation title |
| `onDetentChange` | person-made settles only | same | platform: `selection` sees every change |
| `onHidden` | after the exit | same | `onDismiss` |
| Haptics | arrival tick (B-61) | `UISelectionFeedbackGenerator` [U] | none: iOS plays none at detents |
| Style (colours, radius, inset, shadow) | `AppSheetStyle` | same | `presentationBackground`, `presentationCornerRadius` before iOS 26; the system's from iOS 26 |
| `AppActionSheet` | sheet; anchored card at Medium width | same | `appActionSheet` → `confirmationDialog` |
| `AppChoiceSheet` | yes | same | `appChoiceSheet` |
| Destination sheets | `Presentation.Sheet(SheetSpec)` | same | n/a: SwiftUI features present from their views with `appSheet(item:)` |
| Inspector as sheet | phase 2 (§20) | phase 2 | `appSheetPresentation` |
| Springs | `springs.sheet` | same | programmatic detent changes in `withAnimation(theme.springs.sheet)` = `.spring(response: 0.34, dampingFraction: 1)` |

- The default detents are `[.large]` (changed from `[.medium, .large]` by WP08).
- iOS 27 symbols need the iOS 27 SDK to compile even behind `#available`: WP08 gates them with `#if compiler(>=6.4)`
  (`swift/AppSheet.swift:120-139`), the Swift that SDK ships [U: confirm on Xcode with the iOS 27 SDK].
- `SampleDetailView.swift`'s raw `.presentationDetents` has moved to `appSheet` (WP08 `9883df5`). The template stays
  hidden on the portal (REQUIREMENTS 12).

---

## 19. Work split

Order: **WP03** (API, state, tokens, seams, overlay host) → **WP04** and **WP05** in parallel → **WP02-M3** sample and
**WP06** DevTools panel → **QA2** on the phone → **WP07-B** and **WP08** (they may start once WP03's API is merged and
finish after QA2's decisions) → **WP12**. Each package writes its numbers and its "rules the docs must state" in its
handoff (REQUIREMENTS 25). §15.6 says which package owns each rule.

### 19.1 WP03: the component, its state and tokens, the overlay host, the catalog

**Delivers**

1. Everything under `component/sheet/` (§4.2-§4.10, §12), `SheetMaterial` and the `AppStyle` fields, and the six
   `foundation/layout/` seam files with no-op defaults (`@ShellSeamApi`).
2. The `sheet` section of `tokens/motion.json`, `sheetInset` and `sheetMaterial` in `tokens/styles.json` (§6.2), and the
   Kotlin token writer (`generator/genkit/tokens/kotlin.py`).
3. The overlay host changes (§11.1): call-site keys and saved state, `modal: () -> Boolean`, `HideBeneathNode`, the
   focus guard, parking the `KeptTabs` way for every hosted overlay, call-site removal, `keepContent`, pane bounds.
4. `LocalSheetSurface` adoption in `AppScaffold`, `AppTopBar` and `AppBottomActionBar` (§4.8, §11.4).
5. The presets (§4.9), `SheetDismissConfirmation`, and the deprecated `AppBottomSheet` and legacy `AppActionSheet`
   delegations (§13).
6. The strings of §9 with translations in `generator/translations/android/`.
7. The catalog's Sheets page and its previews (§14).
8. Tests §15.1, §15.2 (all but the `connectedCheck` IME case, which WP04's job runs), the design-system journeys of
   §15.4, §15.5, and the §15.6 table kept true.
9. Code rules, in the project's code-rule mechanism, each with a fixture: `fitInside(WindowInsetsRulers.Ime…)`,
   `imePadding()` and `windowInsetsPadding(WindowInsets.ime)` inside a lambda passed to `AppSheet` or a content-side
   provider; `Modifier.shadow`, `shadowElevation` and `dropShadow` under `component/sheet/`; `dispatchRawDelta` and
   `settle(` under `component/sheet/`; `Dialog(`/`Popup(` in sheet code; `AppSheet` inside an `if`, `when` or lazy item
   scope; size, offset, padding, clip, shadow or layer modifiers in `AppSheet(modifier = …)`; a second layout emitter
   at the root of an `AppSheet` content lambda; `@OptIn(ShellSeamApi::class)` under `feature/**`; a `when` over
   `SheetDetent`, `SheetModality`, `SheetDismissSource` or `SheetMaterial` without `else`; `layer-alpha-fill`'s
   allow-list entry for the sheet layer (B-63).
10. The Kotlin binary-compatibility validator on `:core:designsystem`, with the committed `api/*.api` dump.
11. Before committing, revision 2 applied to the uncommitted `wip/` files: the presenter's node order
    (`wip/stack/CardStackPresenter.kt:35-36`, `:110-128`), the backdrop's lambdas (`wip/surface/SheetBackdrop.kt:38`),
    the shape cache (`CardStackPresenter.kt:59`, `:76`), the mutex table (`wip/AppSheetState.kt:180-311`,
    `wip/gesture/SheetFlingBehavior.kt:64-104`) (C1-31).
12. The deletions of §12.

**Acceptance**

- The sheet package's `.api` dump equals §4: names, parameter order, defaults, annotations; KDoc per C-4/C-5. The §4.1
  blocks compile (`SheetApiShapeTest`).
- Every rule §15.6 gives WP03 is cited by at least one test (rule id in the test name or KDoc).
- §15.1 and §15.2 green; the §10.4 budgets green (recomposition and draw counts); the Compose compiler metrics show the
  sheet's public types stable and its composables skippable (RECOMPOSITION-PLAN gate).
- §15.5 screenshots recorded and reviewed: nothing clipped at font scale 1.5 and 2.0, in dark theme or in RTL; every
  image differs from its no-sheet baseline.
- The code rules catch each forbidden pattern in a fixture.
- Files under 400 lines; no `util`/`internal`/`common` packages; comments only where they explain why (REQUIREMENTS 7).
- The [C] citations of this spec re-pinned at the sheet checkpoint to the merged heads.

### 19.2 WP04: presenter, Navigation 3 scene, predictive back and BackGate, card stack

**Delivers**

1. `Presentation.Sheet(SheetSpec)`, `PresentationKey`, the `pane`/`presentation` require, and the deletion of WP04's
   enum and string key (§4.11).
2. `presentation/sheet/` (`SheetSceneStrategy`, `SheetScene`, `SheetSessions` with its `rememberSaveable` decorator,
   `PresentedSheetFrame` counted in `live`, `AppSheetNavHost` with per-step slot layers); the host building its own
   strategy; the `sheetStrategy` parameters removed (§11.2).
3. Covers' `PresentedFrame` with `cardStackPresenter(level)` and the black backing; `hideFromAccessibility` beneath
   modal presentations (N8).
4. The shell (§11.3): `cardStackPresenter(stack, 0)` in its three-node order, the `ColorDrawable` backing, the
   status-bar and navigation-bar flips through `WindowInsetsControllerCompat`, the `sheetCardStack` option,
   `LocalHostMotion` per host as overlay motion, `LocalEntryVisibility` (tab selected, uncovered, settled, pose) per
   entry with `LocalTabVisible` as their conjunction, `LocalPaneBounds` per pane with its `block` handler,
   `LocalBarFrame`, `LocalPresentationLevel`; `BottomChromeWriter` (N9); the `Chrome.kt` reconciliation (N11).
5. `BackGate` overlay reporters and `NavGuard`'s `Wait` for overlay motion; `StackBackHandler`'s `blocksBack` (B-44).
6. `ShellState.submit` per B-58.
7. Back: the destination-sheet path of B-43 through the presentation gate; the handler ordering of B-36 proven in tests.
8. Perf: the §16.1 scenarios and rows (all but the `glass_live` column), `sheet_large` in `LARGE_SCENARIOS`, the trace
   checks, `back_during_sheet`, `tabs_peek`, `sheet_push_from`, `sheet_recreate`, baseline-profile steps that open,
   drag and dismiss a sheet, and the `connectedCheck` run of `SheetImeInstrumentedTest`.
9. The binary-compatibility validator on `:core:navigation`.
10. Tests §15.3, `CardStackShellTest` and the card-stack journey of §15.4.

**Acceptance**

- §15.3 green. A sheet key never renders as a cover; `AppRoot` passes no strategy.
- On the Redmi, release builds: every §16.1 row of the `solid`, `standard`, `lean` and `large` columns passes (ratchets
  recorded); numbers in the handoff and `perf/sheet/`.
- `restore`, `rapid_back` and `sheet_recreate` pass with a sheet presented.
- N1-N11 closed as §13 maps them.
- Depends on WP03's merged sheet API, and on WP02-M3's sample destination for the phone rows (a test-only destination
  may stand in until then).

### 19.3 WP05: the Glass style

**Delivers**

1. `GlassSheetBackdrop` implementing `SheetBackdrop`, fenced by the liquidglass marker, written into the
   `SheetBackdropHolder` by `GlassStage` (§11.5 items 1-2).
2. Live sampling only for sheets over the shell with no cover (item 3); the bar inside the glass or its documented gap
   (item 4).
3. `SceneBackdrop`'s invalidation on `scene.localPositionOf(surface)` (item 5).
4. CP2's decisions on `sheet.glass.blur` and `sheet.glass.bodyAlpha`, from recordings.
5. The alignment check under the receding presenter, and `glass.opaqueAt` changed per §6.6 if it fails.
6. The Glass design style's `sheetInset` and `sheetMaterial`; `sheet.frosted.bodyAlpha` kept equal to
   `GlassDefaults.frostedColors()`.
7. The `glass_live` rows of §16.1.
8. `design/wp/WP05.md:64` and the handoff's sheet rule revised (§11.5 item 9).

**Acceptance**

- With `glass.quality = Live` on the Redmi: `sheet_drag_glass.janky_pct ≤ 1.0`, its GPU ratchet recorded, no demotion;
  `sheet_cardstack.no_scene_rerecord` true.
- Recordings show the dim over the sample equal to the dim beside it (no undimmed patch), the ramp to opaque toward
  Large, and alignment within 1 px under the receding presenter (or the recorded `opaqueAt` change).
- The design system and every sheet test build and pass with the liquidglass feature removed.
- 0 recompositions per drag frame with Glass; a tier change recomposes no sheet content.

### 19.4 WP07-B: Compose Multiplatform parity

**Delivers** §17: the common sheet, overlay host and presentation-host sheet path; the `expect`/`actual` pieces; the
shared tokens; the typed metadata key; no Back handler on iOS; `Res.string` defaults; the VoiceOver mapping read and
recorded (§21).

**Acceptance**

- The common module's klib dump, limited to the sheet package, equals Android's `.api` dump except where §17 names a
  difference.
- The §15.1 tests green in `commonTest`; an Android-target subset (`SheetPresentTest`, `SheetGestureTest`,
  `SheetBackTest`, `SheetRestoreTest`, `SheetSceneTest`) green.
- The Android target passes §16.1's `sheet_present`, `sheet_drag`, `sheet_back` and `sheet_ime` rows (`solid`) on the
  Redmi; the iOS target compiles (through `bin/memwait`).

### 19.5 WP08: SwiftUI parity

**Delivers** §18's additions on top of `00fd9b9`: `initialDetent`, `appSheetHeader`, `appSheetFooter`,
`appSheetDismissConfirmation`, `appActionSheet`, `appChoiceSheet`; the parity matrix in the template's docs; the Swift
token writer (`generator/genkit/tokens/swift.py`) emitting only what the template reads.

**Acceptance**

- Every SwiftUI cell of §18's matrix holds, behind the stated `#available` and compiler gates.
- Every §4.1 example has a Swift form in the template's docs.
- The template's forbidden-pattern checks pass; features present through `appSheet`; the template stays hidden.
- Without a Mac: reviewed by reading, type-checked where possible, and the §21 WP08 items listed in the handoff.

### 19.6 QA2: phone verification and recordings

**Delivers**

1. The §16.2 recordings and the §16.3 checks per strip.
2. A full §16.1 run on the merged build (release, every column that applies to the Redmi), in `perf/sheet/summary.md`,
   recording every ratchet's baseline.
3. The §16.4 accessibility run, in `perf/sheet/a11y.md`.
4. The §21 items assigned to QA2, and the taste calls (D-19 form sheet, D-30 Medium, the dim strength, the 400 dp/s
   flick, `sheetInset`, `statusBarFlip`) with a recommendation for the sheet checkpoint (`decisions/`). The checkpoint
   also lists for the user's sign-off the deferral of predictive back inside `AppSheetNavHost` (B-59) and the
   Scrolls default for non-modal sheets (B-27).
5. A visual defect sweep: clipping at font scale 1.5 and 2.0, dark theme, RTL, 3-button navigation, tablet emulator.

**Acceptance**

- Every §16.3 and §16.4 check passes, or a defect is filed to its owner with the frame strip.
- Every §21 QA2 item is replaced by a measured value or a recorded decision.

### 19.7 WP12: documentation

**Delivers**, in every template (Android, Multiplatform, SwiftUI) and the generator repo:

1. `docs/COMPONENTS.md` sheet section: the API with do/don't code, the §4.1 examples and their Swift forms, the "which
   to call" table (§4.8).
2. `docs/RECIPES.md`: a destination sheet with `ProvideSheetHeader`/`ProvideSheetFooter`/`ProvideSheetStyle`; a
   Maps-style persistent sheet with a button riding `visibleTop`; confirm on dirty with `SheetDismissConfirmation`;
   steps inside a sheet; navigating from a sheet (B-58, B-60); a picker destination returning its value through a
   shared ViewModel or a result callback (D-32); sequential sheets through `onHidden`; action and choice sheets;
   multi-select; a camera, `SurfaceView` or Media3 `PlayerSurface` behind a sheet: `recedesPresenter = false`, or
   `PlayerSurface(surfaceType = SURFACE_TYPE_TEXTURE_VIEW)` so it recedes with the screen (C2-22).
3. The smoothness and motion guides: F1-F12, the release rule, the §10 tables, the §10.3 forbidden list with these
   called out: "never let a drop shadow follow a size that animates", "a draw node reading a per-frame value wraps a
   layer", "no `fitInside(Ime)` or `imePadding()` in a sheet", "`contentPadding` or `chromeContentPadding()`, never
   both", "keep `AppSheet` composed and drive `visible`; never put it in an `if`", "one root in the content", "close an
   in-screen sheet in the event that navigates away", "a mini player is the tab bar's accessory, a Maps-style sheet is
   a non-modal `AppSheet`".
4. The §16 gates and how to run them (`perf.sh` on a release build), with QA2's numbers.
5. `AGENTS.md`: the short enforceable rules and pre-merge checklist lines; the CMP and SwiftUI equivalents.

**Acceptance:** every rule of §2 and §10.3 appears with a do/don't snippet; numbers come from QA2's summary; every
link resolves.

### 19.8 Supporting packages

- **WP02-M3:** the sample sheet destination (`Presentation.Sheet(SheetSpec(detents = listOf(Medium, Large)))` with a
  list, a field and a footer, tagged per §11.6); the Settings language picker as `AppChoiceSheet` with its choices
  remembered; feature call sites migrated (§13).
- **WP06:** the DevTools panel (`wt/WP06/templates/android/core/devtools/.../panel/DevToolsPanel.kt:62-83` [C]) moves
  off its `Dialog` to an `AppSheet` with `[Medium, Large]` on a phone and the modal `End` side panel from Expanded
  width (§8, §13), after WP03 merges (C2-38).
- **WP02-M2:** hosts the code-rule mechanism WP03's rules plug into, if it lives there.

---

## 20. Phase 2 (after the core passes §16 on the Redmi)

- **`SheetTransition.Zoom(source)`**, as design-feel §5.9 designs it: a second progress `morph` (0 at the source, 1 at
  the sheet) on `springs.sheet`, composed with the offset; the surface rect is `lerp(sourceRect, sheetRect(y), morph)`
  and the corners `lerp(sourceCorner, R, morph)`; the content fades in over `morph` 0.35 to 0.85 with `ModulateAlpha`
  [U]; the dim follows `morph`; the source hides in its own layer while the sheet morphs. A finger grabbing the sheet
  mid-morph takes the offset while `morph` finishes, so nothing jumps. Dismissal by drag slides; by Back, the dim, the
  close button or code it returns into a visible source. A glass source keeps the glass body (M26). API:
  `rememberSheetSource()`, `Modifier.sheetSource(source, shape)`, a new `SheetTransition` constant (D-37). Own tests
  and gate rows.
- Concentric bottom corners while floating, decided from a recording (a path clip below API 33).
- iOS 26-style action sheets anchored to their source on phones (with Zoom).
- Predictive back for the steps inside `AppSheetNavHost` (deferred from v1 with the user's sign-off, B-59).
- **Inspector as sheet**: a destination that is a pane beside its list on wide windows and a sheet on phones, as
  SwiftUI's `.inspector` with WP08's `appSheetPresentation` (C2-58). Until then `entry` rejects `pane` together with
  `presentation`.
- Precomposing a sheet's content on press-down in the host with `createPausedPrecomposition` (SMOOTHNESS S3.5), if
  `sheet_present.tap_to_motion_ms` shows the composing frame still costs a visible delay after `keepContent` (C1-18).

---

## 21. Measure before trusting

| Item | How | Who | If different |
|---|---|---|---|
| The 400 dp/s detent flick | phone: 20 deliberate drags and 20 flicks; count wrong outcomes | QA2 | tune `release.detentFlick` |
| Dim strength and its curve from Medium to Large, light and dark | an iOS 26 recording beside ours, stepped frame by frame (the user, or public captures) | QA2 | change the scrim value sheets use (a `sheet.dim` token feeding `AppSheetDefaults.colors(scrim)`), never the §6.4 mechanism |
| iOS 26 floating inset, corner radius, glass-to-opaque curve | same | QA2, WP05 | `sheetInset`, `glass.opaqueAt` |
| iOS: a very fast flick from Large with Medium present | same | QA2 | B-22 rule 2 |
| iOS: a Medium sheet and the keyboard (lift or grow) | same | QA2 | the `SheetKeyboard` default |
| iOS Maps: the list scrolled at medium (sheet grows or list scrolls) | a recording of Apple Maps, or a public capture | QA2 | flip B-27's non-modal rule to Resizes (no API change) |
| iOS Maps: a keyboard opened beneath a peek (does the sheet move) | same | QA2 | B-45's focus condition |
| Redmi status-bar height, gesture-navigation inset, display corner radius | `adb shell dumpsys window`, a debug overlay | QA2 | worked numbers only |
| The draw-phase round-rect clip, in the sheet and in the presenter's dim node: no `saveLayer`, no stencil pass | Perfetto with `debug.hwui.skia_atrace_enabled` | WP03, WP04 | §7.1 fallback; §6.4's quantised outline |
| Whether Skia's clip slices appear in a trace with that property | Perfetto | WP04 | `no_offscreen` gates only on `saveLayer` |
| The toggled frame-rate vote: a resting sheet with a spinner casts no High vote | `dumpsys SurfaceFlinger` or the Perfetto frame-rate track, API 35+ | WP04 | remove the vote at rest entirely |
| Back with the keyboard up: whether the IME always takes Back first | phone | QA2 | none: B-44 is structural; record which path ran |
| Glass sampling alignment under the receding presenter | phone recording | WP05 | §6.6 |
| The bar inside a Glass sheet's body | CP2 recording | WP05 | document the gap (§6.6) |
| Slot registration lands before the first moving frame | `SheetPresentTest` | WP03 | B-2 |
| A tap that stops a settling sheet does not click the row | Compose test | WP03 | B-17 |
| `hideFromAccessibility()` hides descendants from TalkBack's linear navigation | test and TalkBack on the phone | WP03, QA2 | keep `clearAndSetSemantics` |
| Where TalkBack lands after a sheet closes in touch mode | TalkBack on the phone (§16.4) | QA2 | add an explicit focus request on the trigger through `SheetAnchor` |
| A touchpad two-finger scroll's classification reaches Compose | Chromebook or emulator with a touchpad | WP03 | treat any `Scroll` event as the only touchpad signal |
| Stylus handwriting in a field inside a draggable sheet | emulator with a stylus, test | WP03 | consume the stylus stream over text fields in the exclusion node |
| MDC's side-container edge mapping (shrink or grow by swipe edge) | read `MaterialSideContainerBackHelper` | WP03 | B-39 |
| A `SurfaceView` or `PlayerSurface` behind a receding presenter | phone recording | QA2 | the RECIPES note (§19.7) |
| Gesture navigation's handle over a dark sheet in light theme | phone | QA2 | add the gesture case to §7.9's navigation-bar rule |
| The nine-slice looks the same as a `dropShadow` | screenshot side by side | WP03 | tune the slice key |
| Form sheet: slide (D-19) or scale-in (ADAPTIVE-PLAN:434) | both recorded on a tablet emulator | QA2 → the sheet checkpoint | flip D-19 |
| Medium at 0.5 or 0.55 | recording | QA2 | `detent.mediumFraction` |
| A persistent sheet: one detent per Back, or straight to the lowest (Google Maps) | `screenrecord` of Google Maps on the Redmi | QA2 | B-38 branch 2 |
| CMP: iOS inset animation, the status-bar hook, `DropShadowPainter`, the clip allocation | prototype | WP07-B | §17 |
| CMP: how `paneTitle`, `hideFromAccessibility` and the built-in actions reach VoiceOver; whether an iOS edge swipe reaches navigation-event Back | CMP's iOS accessibility and input sources | WP07-B | §17 |
| SwiftUI: `safeAreaBar`, `navigationTransition(.crossFade)`, the compiler gate for iOS 27 symbols | Xcode (no Mac here) | WP08 | §18 |

---

## 22. Rejected (do not build)

Reviewers flag any of these.

| Proposal | From | Why not |
|---|---|---|
| 2 H/s as the detent-to-detent flick threshold | design-api B-10, design-perf §4.3 | A Medium → Large flick then needs ≈ 1745 dp/s; 2 H/s is the dismiss rule only (D-4) |
| A projected 0.52 dismiss position | revision 1, `research.md:278` | Inverted from Flutter's controller; 0.48 at the release position (D-4, C1-24) |
| A stacked sheet 0.02 H higher | design-api B-36, R-1 | Puts the grabber in the Redmi's status bar and punch-hole (D-10) |
| Floating off for Solid by default | design-perf decision 15 | The default sheet would lose the iOS 26 inset (D-6) |
| Call-site `onDismissAttempt` and per-path `SheetDismissal` flags | design-perf §3.3-3.4 | Unreachable from a destination's screen; `byBack = false` on a modal sheet gives a dead Back (D-11) |
| Public `SheetBackBehavior.StepDown` | design-perf §3.3 | One gesture closes (D-12) |
| Non-modal sheets drawn in place inside the screen | design-api decisions 1, 3, B-27-B-29 | Never Glass, under the bar, the bar left bright under the dim (D-1) |
| Per-level "sheet layers" | design-perf decision 1 | A cross-package rewrite that was never specified (D-1) |
| Extra Back presses on an in-screen sheet popping the screen beneath | design-perf §4.8 | A reflexive double press must not cost the screen (D-13) |
| Popping a destination only after its slide | design-perf §4.8 | Breaks the `OverlayScene` contract; keeps the entry in the stack ≈ 0.5 s (D-13) |
| A dim that lightens toward Large | design-feel §5.5, design-api §4.4 | D-8, unless an iOS 26 recording shows iOS does it (§21) |
| Form sheet as a scale-in on `dialog` with no drag | design-api, design-perf, ADAPTIVE-PLAN:434 | D-19 (taste; revisited at the checkpoint) |
| A placement morph on resize or fold | design-feel §4.13 | A resize is not navigation; the rect would chase a live resize (B-10) |
| 0.008 H over-pull above a top detent that is not Large | design-api B-8 | D-5 |
| A navigation results store | design-api §2.9 | D-32 |
| A header hairline | design-api §5.4 | iOS 26 uses a scroll-edge fade (§7.4) |
| Any `Dialog` or `Popup` window, including the action sheet's popover and the DevTools panel | all | D-22 |
| `@JvmInline value class` detents in the sealed type | design-perf §3.1 | Always boxed through the interface |
| A `dropShadow` sized to the sheet, or reusing `glassSurface`'s shadow path | all three designs | D-18 |
| `SheetStyle.Automatic` = Glass whenever glass ships | design-feel §3.3 | D-17 |
| Live glass sampling above level 1 or over a cover | revision 1 | The recorded scene holds only tab content: wrong pixels (D-17, C1-13) |
| Zoom from a source in v1 | design-feel §3.5, §5.9 | D-31 |
| `Modifier.sheetBleed()` and measuring content at the floating width | design-feel §0 #5 | D-7 |
| Concentric bottom corners in v1 | design-api §4.6, §5.1 | Path clip below API 33; minSdk 26 (§4.12) |
| Padding the sheet's content box instead of `contentPadding` | design-api §2.2, §3.4 | REQUIREMENTS 17 and 34: insets go in `contentPadding` (D-25) |
| `fittingHeight = Unspecified` unless FitContent is listed | design-perf §3.1 | `AppChoiceSheet`'s custom detent would resolve to NaN; `contentHeight` is always measured |
| A full-screen black rect behind the receded shell | design-api §4.4 | An extra full-screen fill; the existing fill is recoloured (§7.9) |
| A sheet-only dim token at 0.20 / 0.48 now | design-feel §0 #23 | Keep `colors.scrim` until QA2 compares (§21) |
| A cover presented as an overlay scene above a sheet | judge-2 §2.2 option 1 | B-58 closes the sheet first (D-23) |
| The dim drawn inline after the presenter's content, with no layer beneath | revision 1 §6.4, `wip/stack/CardStackPresenter.kt` | Re-records the whole receding shell per frame, and the glass scene with it (F11, C1-01) |
| Surface and clip in window coordinates for an attached sheet; unlayered grabber, header and footer | revision 1 §6.3 | Re-records the sheet layer and its inline children every frame (§6.3, C1-02) |
| `key(entry)` on an identity-hashed overlay entry; `modal` as an effect key | today's `AppOverlayHost` | Loses every `rememberSaveable` on recreation; rebuilds the sheet at `upThrough` (D-40, C1-03) |
| `sharePointerInputWithSiblings()` on the scrim for the non-modal pass-through | revision 1 B-51 | Cannot cross LayoutNode levels; the unplaced scrim does it (B-51, C1-04) |
| Leaving as `isVisible && targetDetent == null` | revision 1 §13 | Late by 60-100 ms on a flick; a second dismissal (§4.4, C1-05) |
| Nested scroll through `dispatchRawDelta`; `settle(velocity)` | revision 1 B-28 | Skips the bands and the mutex; deprecated and throwing (B-28, C1-07) |
| Sheet motion reported as stack motion | revision 1 B-15 | `NavGuard` drops the push (D-15, C1-08) |
| Disabling the sheet's Back handler while the IME is up | revision 1 B-44 | Back could reach what is beneath (B-44, C1-09) |
| `LocalBottomChrome` as a non-modal sheet's resting line; lifting for a keyboard beneath | revision 1 B-52, B-45 | It is the keyboard while the keyboard hides the bar (B-52, C1-11) |
| Parking by not placing; reading `LocalTabVisible` for parking | revision 1 B-14 | Drops layers; parks instantly on every push (B-14, C1-12, C2-04) |
| Owner level + 1 for a modal sheet; parking a persistent sheet when a modal one opens beside it | revision 1 §3.2; C1-16's alternative | The dim lands under the persistent sheet; iOS stacks the new sheet (B-54a) |
| Focus moved into a sheet only when opened by keyboard; Escape in `onPreviewKeyEvent` | revision 1 §9, B-42 | A field beneath keeps focus and its keyboard; children never see Escape (B-42, B-42a) |
| A 200 ms reduce-motion fade | revision 1 B-63 | Compose scales it to 0 under "Remove animations"; honour the setting (D-35) |
| A ready-made peek as `Height(96.dp)` | revision 1 §4.1 | Clips the header at font scale 2.0 (`Peek`, C2-35) |
| `sheetAvoidance(state)` beside `visibleTop` | C2-34's alternative | Two ways to ride the sheet; `visibleTop` in `Modifier.offset { }` is one line (§4.4) |

---

## 23. Sources

**Project.** `REQUIREMENTS.md:125-136`; `research/sheet/current-state.md` (§2-§12), `research.md` (§0-§6),
`design-feel.md`, `design-api.md`, `design-perf.md`, `judge-1.md`, `judge-2.md`, `judge-3.md`, `critique-1.md`,
`critique-2.md`; `research/CODE-STANDARDS.md` §2-§3 (API-1 to API-15, M-1, M-5, K-1, K-2, K-4, K-8, F-2, F-3, F-5, F-6,
T-5, T-6, L-1, L-5); `research/MOTION-PLAN.md` (§1, §2, M1, M6, M17, M22, M26, M27); `decisions/CP1.md:28-60`,
`:109-140`; `research/predictive-back/SPEC.md:60-87`, `:395-425`; `research/RECOMPOSITION-PLAN.md` R7, §3;
`research/ADAPTIVE-PLAN.md:430-437`, `:565`; `research/SMOOTHNESS-PLAN.md` S2, S3, S3.5, S4 (`:296-329`), S8, S10;
`notes/INDEX.md`; `handoff/WP05.md:105-242`; `qa/r1/DEFECTS.md:41`.

**Code read for this spec.** Revision 1 at WP03 `8ea3985`, WP04 `230f894`, WP05 `d549b2a`, WP07 `99239af`, WP08
`a95111a`. Revision 2 re-checked every citation it relies on at WP03 `87a3a06` (plus the uncommitted `wip/`), WP04
`f8ee600`, WP05 `d549b2a`, WP06 `d2febdb`, WP07 `99239af`, WP08 `00fd9b9`. Files: `ds/component/overlay/AppSheet.kt:21`,
`:132`, `:211`, `:227`, `:232`, `:236`, `:335-343`; `ds/component/overlay/AppOverlayHost.kt:40-100`;
`ds/component/overlay/OverlayBackHandler.kt:20-51`; `ds/component/overlay/AppMenu.kt:40-42`;
`ds/foundation/adaptive/WindowClass.kt:16`, `:60-72`; `ds/foundation/adaptive/InputTraits.kt:11-15`;
`ds/foundation/adaptive/AppLayout.kt:15-66`; `ds/theme/AppTheme.kt:88-111`, `:145`, `:217-235`; `ds/theme/AppMotion.kt:61-77`;
`ds/theme/ReduceMotion.kt:41-42`; `ds/theme/tokens/DesignTokens.kt:249`, `:292`, `:348-399`;
`ds/foundation/interaction/AppHaptics.kt:27`, `:59`; `wt/WP03/tokens/motion.json` (whole), `tokens/styles.json:2-56`;
`wt/WP03/generator/genkit/tokens/`; `wip/stack/CardStackPresenter.kt`, `wip/surface/SheetBackdrop.kt`,
`wip/AppSheetState.kt`, `wip/gesture/`; `ds4/foundation/layout/Chrome.kt:27-103`;
`nav4/presentation/PresentationHost.kt:49-205`; `nav4/AppShell.kt:84-200`; `nav4/Presentation.kt:11-18`;
`nav4/NavGraph.kt:30-70`, `:121`; `nav4/back/BackGate.kt:17-109`; `nav4/back/NavGuard.kt:15-35`;
`nav4/back/StackBackHandler.kt:60-72`; `nav4/stack/StackLayout.kt:120-150`; `nav4/shell/tabs/KeptTabs.kt:36-44`,
`:116-123`; `nav4/shell/BottomChromeWriter.kt:45-55`; `nav4/ShellState.kt:86-96`; `app4/AndroidManifest.xml:55-68`;
`app4/kotlin/com/base/app/ui/AppRoot.kt:51-89`, `:168-182`; `glass/GlassStage.kt:43-58`, `glass/SceneBackdrop.kt:52-76`,
`:157-163`; `glass/GlassQuality.kt:22-47`, `:96-108`, `glass/GlassSurface.kt:75-125`;
`wt/WP06/templates/android/core/devtools/.../panel/DevToolsPanel.kt:62-83`; `swift/` (all files);
`perf/gates.toml` (whole), `perf/perf.py:50`.

**Library facts relied on** (verified by the design authors, the judges or the revision-2 reviews, as cited): Compose
UI 1.12.1 (`OutlineResolver.android.kt:263-288`, `SemanticsProperties.kt:180-187`, `:972`, `:1022`, `:1075`, `:1149-1150`,
`:1632-1660`, `SemanticsOwner.kt:274-275`, `:318`, `FrameRate.kt:71`, `:115-145`, `GraphicsLayerOwnerLayer.android.kt:274-277`,
`Placeable.kt:501`, `WindowInsetsRulers.kt:48-97`, `:223-250`, `NestedScrollModifier.kt:294`,
`PointerInputModifierNode.kt:73-79`, `InnerNodeCoordinator.kt:215-244`, `NodeCoordinator.kt:450-451`, `:612-680`,
`:1478-1491`, `AndroidComposeView.android.kt:364-365`, `:2187-2190`, `FocusProperties.kt:188`, `FocusRestorer.kt:109`,
`FocusRequester.kt:156`, `:174`, `Focusability.kt:66-67`, `BringIntoViewModifierNode.kt:32`,
`WindowRecomposer.android.kt:97`, `:427-446`, `draw/Shadow.kt:154`); ui-graphics 1.12.1 (`ShadowRenderer.kt:70-79`,
`DropShadowPainter.kt:76-78`, `:118-122`, `:217-260`, `AndroidShadowContext.android.kt:53-84`, both `Shadow` classes);
ui-test 1.12.1 (`DeviceConfigurationOverride.android.kt:406-424`); foundation-layout 1.12.1 (`RulerAlignment.kt:36-50`);
foundation 1.12.1 (`AnchoredDraggable.kt:410-479`, `:868-871`, `:935-936`, `:1001-1029`, `:1062-1069`, `:1155`,
`:1234-1250`, `:1483-1514`, `:1551-1566`, `:1716-1735`; `MutatorMutex.kt:82`; `Clickable.android.kt:40`;
`Clickable.kt:2155-2190`; `style/ExperimentalFoundationStyleApi.kt:23`); runtime 1.12.1 (`GapComposer.kt:3085-3097`);
runtime-saveable 1.12.1 (`RememberSaveable.kt:81-87`, `SaveableStateHolder.kt:72-78`); navigationevent 1.1.2
(`NavigationEventProcessor.kt:272`, `:579`, `NavigationEventDispatcher.kt:169-170`, `:292-295`,
`NavigationEventHandler.kt:126`, `OnBackInvokedInput.android.kt:38-48`, `:76-91`); navigationevent-compose 1.1.2
(`NavigationEventHandler.kt:97-103`, `:115`); navigation3-runtime 1.1.7 (`NavMetadataKey.kt`,
`NavEntryDecorator.kt:70-88`, `DecoratedNavEntries.kt:205-223`); navigation3-ui 1.2.0 (`NavDisplay.kt:686-698`,
`:874-881`); MDC (`BottomSheetBehavior.java:2520`, `:2537`, `BottomSheetDragHandleView.java:82-87`,
`MaterialBottomContainerBackHelper.java:82-103`, `SideSheetBehavior.java`); Flutter `sheet.dart:14-90`, `:327-375`,
`:704-724`, `:1063-1140`.

**Apple [A].** HIG Sheets https://developer.apple.com/design/human-interface-guidelines/sheets ; WWDC25 323
https://developer.apple.com/videos/play/wwdc2025/323/ ; WWDC25 284 https://developer.apple.com/videos/play/wwdc2025/284/ ;
Adopting Liquid Glass https://developer.apple.com/documentation/technologyoverviews/adopting-liquid-glass ; WWDC21 10063
https://developer.apple.com/videos/play/wwdc2021/10063/ ; WWDC19 224 https://developer.apple.com/videos/play/wwdc2019/224/ ;
WWDC18 803 https://developer.apple.com/videos/play/wwdc2018/803/ ; WWDC24 10145
https://developer.apple.com/videos/play/wwdc2024/10145/ ; `UISheetPresentationController`
https://developer.apple.com/documentation/uikit/uisheetpresentationcontroller ; `presentationDetents`
https://developer.apple.com/documentation/swiftui/view/presentationdetents(_:) ; `presentationBackgroundInteraction`
https://developer.apple.com/documentation/swiftui/view/presentationbackgroundinteraction(_:) ; `presentationPlacement`
https://developer.apple.com/documentation/swiftui/view/presentationplacement(_:) ; `dismissalConfirmationDialog`
https://developer.apple.com/documentation/swiftui/view/dismissalconfirmationdialog(_:shouldpresent:actions:) ;
`confirmationDialog`
https://developer.apple.com/documentation/swiftui/view/confirmationdialog(_:ispresented:titlevisibility:actions:) ;
`presentationControllerDidAttemptToDismiss`
https://developer.apple.com/documentation/uikit/uiadaptivepresentationcontrollerdelegate/presentationcontrollerdidattempttodismiss(_:) ;
ProMotion on iPhone
https://developer.apple.com/documentation/quartzcore/optimizing-promotion-refresh-rates-for-iphone-13-pro-and-ipad-pro
(not fetched for this spec).

**Google [G].** Material 3 bottom sheets https://m3.material.io/components/bottom-sheets/specs , `/guidelines`,
`/accessibility`; Material 3 side sheets https://m3.material.io/components/side-sheets/specs ;
`WindowInsets.getRoundedCorner`
https://developer.android.com/reference/android/view/WindowInsets#getRoundedCorner(int) ; Compose performance
https://developer.android.com/develop/ui/compose/performance/bestpractices ; nav3 recipes
https://github.com/android/nav3-recipes/tree/main/app/src/main/java/com/example/nav3recipes/bottomsheet ; Kotlin binary
compatibility validator https://github.com/Kotlin/binary-compatibility-validator ; androidx and MDC sources pinned in
`research/sheet/sources/`; androidx commits cited by short SHA at `https://github.com/androidx/androidx/commit/<sha>`.

**Third party [M].** Flutter's Cupertino sheet (iOS 18 measurements)
https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/cupertino/sheet.dart

---

## 24. Decisions (revision 2)

Every finding of `research/sheet/critique-1.md` and `critique-2.md`, and where it landed. "Resolved" means the fix
the finding asked for, or an equivalent named here; the few parts not taken say why. Blocking and major findings are
all resolved.

**Blocking**

| # | Resolution |
|---|---|
| C1-01 | Presenter node order recede layer → dim node → content layer (§6.4, §4.10); covered sheets the same (B-54); F11 and §10.3; `SheetDrawCountTest`; `sheet_cardstack` in `glass_live` (§16.1) |
| C1-02 | Constant layer-local body and clip while attached; children placed with layers (§6.3, §7.1, B-45); §10.1 "attached drag: L1, 0 re-records"; draw-count tests (§10.4, §15.2) |
| C1-03 | Call-site compound key + call-site `SaveableStateHolder`; `modal: () -> Boolean` read by `HideBeneathNode` (§11.1, B-12, D-40); `SheetRestoreTest` |
| C2-01 | B-42a focus on present, B-42 Escape on the surface (D-34); §21 row replaced by the 1.12.1 APIs; `SheetA11yTest` |
| C2-02 | Metrics and content locals from the placement class, `maxWidth = Dp.Unspecified`, `ProvideContainerWidth`, `LocalPaneContext` reset (§7.2, §11.4, D-20); `SheetAdaptiveTest` |
| C2-03 | As C1-03. The saved-id and host-owned-holder variant is replaced by the call-site key and a call-site holder: the same restore, without `removeState` bookkeeping. Rotation no longer recreates at WP04's head (`app4/AndroidManifest.xml:57-64`), so §16.1's row switches night mode instead (`sheet_recreate`) |

**Major**

| # | Resolution |
|---|---|
| C1-04 | The scrim is not placed at `m = 0` (B-51); the overlay boxes carry no pointer modifiers (§11.1); claim retagged; §21 row removed; `SheetModalityTest` |
| C1-05 | Mutex table at `Default`/`PreventUserInput` with a finger-down guard; the `leaving` flag and `isLeaving`; `targetDetent` as the sheet's own field (§4.4, B-9, B-18, §13); flick-then-touch and flick-then-Back tests |
| C1-06 | `PresentedSheetFrame` counts in `live` (§11.2, B-43); `SheetSceneTest` |
| C1-07 | `dragBy()` in one `anchoredDrag(Default)` session per gesture, closed by `SheetFlingBehavior` in `onPreFling`; never `dispatchRawDelta` or `settle(velocity)` (B-26, B-28); `SheetNestedScrollTest` |
| C1-08 | Sheet motion is overlay motion: `NavGuard` waits, never drops (B-15, §11.3, D-15); the citation corrected to `NavGuard.kt:21-23`; `SheetGateTest` |
| C1-09 | B-44 made structural: stack handlers beneath a modal sheet disabled through `blocksBack`, the sheet's handler kept on with branch 0 (B-37, B-38, D-14); `SheetBackTest`, `SheetBackRoutingTest` |
| C1-10 | Detents from the keyboard-free measure only (R-3, B-47, D-26); `SheetImeTest` |
| C1-11 | `BarFrame` (settled and live bar top, never the keyboard); lift only while focus is inside (B-45, B-52); §21 row for iOS Maps; `SheetImeTest` |
| C1-12 | Parking the `KeptTabs` way, getters read outside composition (B-14, §11.1); `SheetParkingTest`; `tabs_peek` row |
| C1-13 | Live sampling only over the shell with no cover (§6.6, D-17, `samplesScene`); the bar inside the glass or a documented gap (§11.5); `SheetStackTest` |
| C1-14 | `SceneBackdrop` invalidates on `localPositionOf` (§11.5); corners as a draw-time clip, a quantised outline as fallback (§6.4); `no_scene_rerecord` in `glass_live` |
| C1-15 | Providers capture the call site's locals and a saveable holder (§4.8); `SheetSceneTest` ViewModel case |
| C1-16 | Levels by stack order (B-54a, D-33); the DevTools overlay's z-order stated (§3.1, §11.1). Parking the persistent sheet instead was not taken: iOS stacks |
| C1-17 | B-14a: the sheet follows its owner's pose; parking from the gesture's first frame as the fallback; arrival waits for `isSettled` (B-2); `back_during_sheet` row |
| C1-18 | +1 frame stated (B-1, B-2, §10.1); `hitch_ms_per_s`, `tap_to_motion_ms`, motion-frame `janky_pct` 2.0, GPU ratchets, `sheet_push_from` (§16.1); `keepContent` (§4.6). Precomposing on press-down is phase 2 (§20): it needs a host-level precomposition and a measurement first, and `keepContent` already removes the cost of repeat opens |
| C1-19 | The visible-band clip under an opaque Large sheet, for presenters and covered sheets (§6.4, B-56); measured once (§16.1 `sheet_scroll`) |
| C2-04 | `EntryVisibility(tabSelected, uncovered, settled, pose)`, `LocalTabVisible` kept as their conjunction; tab not selected → instant, covered → slide (B-14, §4.10, §11.3); `SheetParkingTest` |
| C2-05 | B-14a for the owner's predictive pop (the pose, or the park as fallback); B-4a for call-site removal; code rule and docs line (§10.3, §19.7) |
| C2-06 | B-54a; the n-layer merged dim (§6.4); `SheetStackTest` |
| C2-07 | B-38a; §3.3 splits Back key and Back gesture. The commit exits at rest, not with `lastVelocity`: the gesture held the sheet still, and a stale upward velocity would make the exit start upward |
| C2-08 | B-63 honours "Remove animations" (D-35); tests set `Settings.Global` |
| C2-09 | R-6 (Large's identity when listed) and R-6a (Large-equivalent); `DetentResolutionTest`; screenshots |
| C2-10 | The caller's value is the truth (B-4, `show()`/`hide()` KDoc); "which to call" table (§4.8); `SheetDismissTest` |
| C2-11 | B-33 column and B-37 clause for a dismissible non-modal sheet; `SheetGuardTest` |
| C2-12 | B-47a bring-into-view; `Scrolls` under touch exploration (B-27); tests and §16.4 |
| C2-13 | `paneTitle` on both overloads and `AppSheetLayout`; `ProvideSheetHeader(title)`; precedence (§4.3, §9). `SheetSpec.paneTitle` is a `@StringRes Int`, not a `String`: the graph is built outside composition, where a localised string is unavailable and would go stale on a locale change |
| C2-14 | The anchored card is modal for semantics (§4.9, §9); `AppActionSheetTest` |
| C2-15 | §16.4; `SheetA11yInstrumentedTest` with ATF (§15.2) |
| C2-16 | R-10 and R-11 semantics for Center and side panels; B-10; `SheetAdaptiveTest` |
| C2-17 | B-45a; `SheetImeTest` cases; IME screenshot |
| C2-18 | The modal side panel (§8, D-36), B-39's side-container pose, tokens (§6.2). It dismisses at 2 W/s or 48 % of the way out, the same corrected rule as the bottom edge (C1-24), not "52 %" |
| C2-19 | Hinge-keyed rows, first match wins (§8); R-12; `SheetPlacementPolicyTest` |
| C2-20 | Pane-bounded dim, blocking and semantics (B-51, B-53, `PaneBounds.block`); `SheetModalityTest` |
| C2-21 | Per-gesture pointer type (B-17, B-20, B-27); touchpad classification and stylus handwriting kept [U] with tests and §21 rows |
| C2-22 | `recedesPresenter`; recede only at Compact window width (§6.4, D-9); RECIPES note and §21 row |
| C2-23 | Gutter in `contentPadding` (B-50, §7.2, D-7); QA-17 closed (§13); screenshot case |
| C2-24 | Content `Box`, one root (B-1a, §4.8, D-39); code rule |
| C2-25 | Pass-in option types as value classes; branch-on types documented with `else` and a code rule; D-31 corrected (D-37, §4.2, §4.6) |
| C2-26 | `contentUnderGrabber` and `containerBrush` (§4.6, §4.7, §7.3, §7.6); `ContentTop` rule; `media` demo |
| C2-27 | `AppSheetLayout` behind `@ShellSeamApi` (§4.3, D-38); the API dump asserts the two overloads |
| C2-28 | Latest resolver per id, a generation counter read in measure (§4.4, §4.5) |
| C2-29 | `require`s; R-13; non-modal Center (B-51); `SheetModalityTest` at 900 × 400 |
| C2-30 | The grabber's 14 dp touch band (§7.3); test |
| C2-31 | The sheet owns the grabber node; `AppSheetGrabber` reads the sheet's colour (§4.8, §7.3) |
| C2-32 | `modifier` as a marked Deviation with its contract and a code rule (§4.2) |
| C2-33 | `dragEnabled` and `Modifier.sheetDragExclusion()` (§4.6, §4.8, B-16); `hold` demo |
| C2-34 | `visibleTop`, `visibleHeight` (§4.4); the `maps` demo and a RECIPES entry. `sheetAvoidance` not added (§22) |
| C2-35 | `SheetDetentContext.headerHeight` and `SheetDetent.Peek` (§4.5); the footer unpins (§4.8); screenshots |
| C2-36 | `key: ((T) -> Any)? = null`, position keys by default (§4.9); test |
| C2-37 | Per-step registry layers, cross-fade, target-step height, covered guards ignored (§4.8, B-59); the deferral listed for sign-off (§19.6) |
| C2-38 | §13 row; WP06 in §19.8 |
| C2-39 | §18 rebased on `00fd9b9` with a parity matrix and the added Swift forms |
| C2-40 | `SheetDismissConfirmation` and `appSheetDismissConfirmation` (§4.8, §18); WP08's shipped forwarder answers the delegate [U] |
| C2-41 | `LocalInspectionMode` pose, `snapToFraction`, multi-size previews, baseline-diff acceptance (§4.4, §14, §15.5) |
| C2-42 | `SheetApiShapeTest` and `SheetSpecShapeTest` with listed stubs; blocks rewrapped at 100 columns; "longer" in arguments (§4.1) |
| C2-43 | `SheetImeSource` with a test double; `SheetImeInstrumentedTest` (§5.9, §15.2) |

**Minor**

| # | Resolution |
|---|---|
| C1-20 | `opaque` and `dim` as `SheetFrame` getters; shape caches as arrays; index loops in `SheetStack`; the stored pane `Rect` (§4.10, §6.4, §7.1, §10.3) |
| C1-21 | `SheetBackdropHolder` installed by `AppTheme`, written by `GlassStage` (§4.10, §11.5) |
| C1-22 | B-63 keeps the whole-layer `ModulateAlpha` fade; §7.7 rule 6 rewritten; the code rule allow-lists the sheet layer (§10.3) |
| C1-23 | The frame-rate vote toggled only while back progress moves (B-64); `cardStackPresenter` needs none, because the recede only moves while the sheet's placement does, which already votes; §21 row |
| C1-24 | `dismissPosition` 0.48 at the release position, the table and `research.md:278` corrected; the haptic at 2 % (B-61, D-16); `springs.sheet.pixels` (§4.4, §6.1); `statusBarFlip` retagged [D]/taste (§6.2) |
| C1-25 | B-21 restated as the tap timeout + 1 frame; the [U] dropped |
| C1-26 | B-17 re-settles at `v = 0` to the stopped target; test |
| C1-27 | B-36 names the class-based handler of `OverlayBackHandler.kt:38-50` |
| C1-28 | Sessions through `rememberSaveable` inside a decorator (§11.2) |
| C1-29 | `isAppearanceLightStatusBars` from a `snapshotFlow`; the `ColorDrawable` mutated (§7.9); `no_view_layout` row |
| C1-30 | One remembered `SheetSurface` with the declared material (§4.8); §10.4 row |
| C1-31 | Heads re-pinned in the header and §23; WP03 applies revision 2 to `wip/` before committing (§19.1 item 11) |
| C2-44 | `@ShellSeamApi` on every seam; code rule (§4.10) |
| C2-45 | `AppSheetDefaults` trimmed to parameter defaults; the Swift writer emits only what the template reads (§4.7, §6.2) |
| C2-46 | `AppSheetProperties`, `SheetSpec`, `NonModal` as plain classes with `copy`; `AppSheetColors.copy` (§4.6, §4.7, §4.11) |
| C2-47 | `androidx.compose.ui.graphics.shadow.Shadow` named; brush and blend mode rejected (§4.7, §7.7) |
| C2-48 | The nine-slice on a modal Bottom sheet over a scrim with alpha < 0.2 (§7.7) |
| C2-49 | `dialog()` for tests only; title `heading()`; close button absent when it cannot act; nodes named; hide and restore timing (§7.4, §9) |
| C2-50 | Built-in actions, click label, documented contrast exception, dismiss sources (§7.3, §9, B-33) |
| C2-51 | `LocalSheetSurface` as a `CompositionLocal`; providers with the `AppSheetScope` receiver; the slot header with `subtitle` (§4.8) |
| C2-52 | `BarFrame.liveTop` in placement (B-52); the mini-player rule in the docs; `nowplaying` replaced by `route` and `media` (§14, §19.7) |
| C2-53 | The reason for `Scrolls` stated in B-27; §21 row; listed for the checkpoint (§19.6) |
| C2-54 | The Glass example lists Medium; `SheetMaterial.Glass` KDoc (§4.1, §4.7) |
| C2-55 | B-6: the caller decides; a cold deep link slides (§11.2); test |
| C2-56 | Rows at least 56 dp (§4.9); font-scale 2.0 screenshots |
| C2-57 | `supportingText`, `enabled`, `onSelectNone`, accent-folding search, `onSelect` then `onDismissRequest` (§4.9) |
| C2-58 | `require(presentation == null || pane == Single)`; `ProvideSheetStyle`; the inspector pattern in §20 |
| C2-59 | `onHidden` on both overloads (§4.3); the result recipe (§19.7) |
| C2-60 | No Back handler on iOS; `Res.string`; §21 rows for VoiceOver and the edge swipe (§17) |
| C2-61 | The binary-compatibility validator and dumps (§4, §19); §15.6 table |
| C2-62 | `pointer_location`; the `sheet_arrival` trace section; static idle content; `no_offscreen`'s trace setup (§16) |
| C2-63 | Navigation-bar icons from the covering sheet's container (§7.9); screenshot |
| C2-64 | §21 points at branch 2; the KDoc says "never re-measured during a drag"; §8 first match wins; heads refreshed |
