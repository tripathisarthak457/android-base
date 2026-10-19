# District by Zomato: what it does differently, and what android-base could take from it

Written 2026-10-19 for REQUIREMENTS 45 by the motion and design-system lead (Opus 5.5). Read-only research: no code,
no worktree, no Gradle, no adb, no commit. **Every proposal below waits for your yes, one at a time**; none is planned
as work until you approve it, and approved ones land on `staging-wp/*` after staging (REQUIREMENTS 44, 45).

Inputs: the verified sources in `SOURCES.md` (this folder), the template's motion work (`research/MOTION-PLAN.md`,
`SHEET-SPEC.md` and `sheet/`, `SMOOTHNESS-PLAN.md`, `ios-motion.md`, `decisions/CP1.md`, REQUIREMENTS 19, 21, 22, 24,
28, 29, 41, 42) and the code at the branch heads `wp/WP03` (`1dc5714c`), `wp/WP04` (`4a15dda6`) and `wp/WP05-glass`
(`282dd3c2`). The phone procedure that settles the open questions is `PHONE-STUDY.md`; its measuring tools are in
`tools/` (standard-library Python plus ffmpeg, tested on synthetic recordings).

How sure each statement is:

- **Confirmed**: read at the source (store pages, Zomato's open-source code, Google's androidx code, Play reviews).
- **Observed**: seen frame by frame in a public video. It describes what the screen did, not how District is built.
- **Guess**: inference. Only your phone can settle it.

---

## Part 1. In plain English

### The short answer

1. **Nobody outside Zomato has published how District's Android app is built or how its motion is tuned.** No talk,
   blog post, job ad or repository names its toolkit, and no source gives a single duration or spring value for any
   District animation. Everything public about its motion describes the **iPhone** app. The only usable Android screen
   recording is one UX review on YouTube, and it shows no bottom sheet at all.
2. **The best lead is Zomato's own open-source design kit (Sushi).** Its bottom sheet is Google's stock Material 3
   sheet with only colours and corners changed; it adds no motion of its own. Zomato says Sushi runs in production on
   Android, but never says District uses it. **If** District's sheets are that stock sheet (a guess), then compared with
   ours District's sheet:
   - opens noticeably quicker (two thirds to four fifths of the way after 0.1 s, depending on its theme's motion
     scheme, against our 55 %);
   - closes **much** quicker when you tap outside it or press Back (gone in about 0.1 s, against our 0.3 s and more);
   - dims the screen a little less (32 %, against our 40 % light and 56 % dark), and keeps the dim fully on while you
     drag the sheet down, until it decides to close;
   - can be moved by a fling of the list inside it, and stretches its surface when pulled above the top instead of
     rubber-banding.
   The first two are the likeliest reason it feels "snappy". The dim's strength is a matter of taste (P4). Keeping the
   dim fully on during a drag, letting a list's fling move the sheet, and stretching the surface are places where ours
   behaves better.
3. **On the mechanics that make a sheet feel fluid, our sheet is already at or above that level**: it follows the finger
   with no recomposition, keeps the finger's speed when you let go, can be caught mid-flight, rubber-bands like iOS, dims
   in step with your finger, floats inset at half height, recedes the screen behind it, and ticks a haptic when it lands.
   What District may do better is **tempo** (how fast it opens and closes) and a few **screen patterns** (a panel over a
   playing trailer, quick swipes between movie pages, a header that folds into tabs).
4. **Recommendation: run the phone study before deciding the sheet items (P1-P4).** P1, P2 and P4 are one-token
   changes and P3 only makes sense if our sheet's first frame is measurably worse, but whether District really is
   faster or lighter, and by how much, is a measurement, not a guess. The study takes about two hours on your phone
   once you have installed District and signed in yourself, and it only watches.

### What District does differently, by how sure we are

| | What | Source |
|---|---|---|
| **Confirmed** | Store facts: Play 4.7 from 195 k ratings, 10 M+ installs, updated 15 Oct 2026; reviewers praise it as "incredibly snappy", lag-free and light | Play listing and reviews (`SOURCES.md` §1, §6) |
| **Confirmed** | Reviewers also complain: a "Best Seats" animation or pop-up replays on every app or theatre open, and after a phone call the booking page came back with the date changed | Play reviews (§6) |
| **Confirmed** | Zomato's open-source Sushi sheet = stock Material 3 sheet: opens on the theme's spatial spring, hides on its fast effects spring, dim 32 % faded by state, list flings hand their speed to the sheet | Sushi and androidx source (§3, §7) |
| **Confirmed** | Sushi's press shrink is 0.95 and snaps back without animating on release; its shimmer is a modifier on each placeholder that loops a 1-second sweep with no pause | Sushi source (§3) |
| **Observed (Android)** | Swiping between movie pages lands in about 0.2 s, edge to edge, no scaling; the new page shows its poster at once with grey skeleton bars, the trailer about 0.1 s after landing, text about 0.25-0.3 s after | aspira design review, 1:38.9-1:39.4 (§5, O1) |
| **Observed (Android)** | On a movie page a dark panel with a grab handle rises over the playing trailer in about 0.5 s; the top bar and a floating "Book tickets" bar stay put | same video, 2:45.3-2:45.8 (§5, O2) |
| **Observed (iPhone only)** | A grid of large 3D category icons folds into a compact sticky tab bar as you scroll; a card list where the centred card is big and the rest shrink and dim; illustrated empty states, a 3D character, glowing intro modals, a drawn-on logo splash | 60fps.design and iPhone reviews (§4) |
| **Guess** | Native Kotlin, Compose likely in use with Views mixed in; sheets possibly Sushi, hence stock Material 3 | §2 verdict |

### What android-base already does as well or better

- **Sheet mechanics** (`wp/WP03`, `component/sheet/`): the release keeps the finger's speed, a flick moves one stop,
  closing needs a deliberate flick or most of the way down, a touch catches a moving sheet, the edges rubber-band with
  iOS's curve, the dim follows the finger, only a finger (never a list's momentum) moves the sheet, the haptic marks
  arrival, predictive back uses Google's own sheet constants, and a drag recomposes nothing.
- **Press feedback**: our release springs back with a trace of bounce; Sushi's snaps back with no animation.
- **Skeleton shimmer**: one band crosses every placeholder together, then pauses with no frames drawn; Sushi's
  shimmer loops on each placeholder, every frame, with no pause.
- **Scrolling**: iOS deceleration and rubber band on every scroller (REQUIREMENTS 41); push/pop on iOS's spring with
  parallax; the Gmail back card; the tab nudge; frame-rate votes; glass that steps down on weak GPUs.
- **"Content after the move"**: what District's movie pages appear to do (poster first, text after landing) is our
  rule already: a screen's first composition is final and secondary content waits for the move to settle
  (`SMOOTHNESS-PLAN` S3; the shell part is built, features have not adopted it yet).

### The proposals, by how much you would feel them

| # | Proposal | What you would notice | Cost on the Redmi | Phone study first? |
|---|---|---|---|---|
| P1 | Quicker sheet close when nothing is dragging it (tap outside, Back, close button) | Sheets get out of the way at once | none (fewer frames) | yes |
| P2 | Quicker sheet open | Sheets arrive sooner | none (fewer frames) | yes |
| P3 | Cheaper first frame when a sheet opens | No catch on the tap | memory for pre-built content | yes |
| P4 | Lighter dim behind sheets | Less heavy, more like District | none | yes |
| P5 | Catalog recipe: a movie-style page with a panel over a playing picture | You can build District's detail page | as any sheet; no new layers | yes |
| P6 | Catalog recipe: swipe between detail pages, and a paging-speed check | Quick, clean page swipes | one prepared page in memory | yes |
| P7 | A scroll-linked header helper: icons fold into tabs as you scroll | District's home header, done cheaply | a few small layers per frame | yes |
| P8 | Docs rules: intros play once; choices survive a phone call | Fewer annoyances in apps built on it | none | no |
| P9 | Catalog recipe: a snapping list whose centred card is emphasised | A showcase list | small layers per visible card | yes |
| P10 | Sheets that grow out of the button that opened them (the spec's "zoom", phase 2) | A richer arrival | one rect re-recorded per frame | yes |

### Left out, and why

- **Blur behind sheets or full screens, glowing borders and light beams drawn live**: a full-screen blur re-renders on
  every frame of a moving sheet, on a GPU that already used 12 ms of an 8.33 ms frame in push/pop (SHEET-SPEC D-17).
- **Cards that change height as the list scrolls** (District's iPhone weekend list): re-measures the list every frame.
- **Always-running idle animations** (a hanging 3D character, bouncing icons, rotating banners) as template defaults:
  an idle screen must draw nothing; loops keep the 120 Hz pipeline and the GPU awake and cost battery.
- **Lottie as a default dependency, animated splash screens**: weight and startup time against "light"; the
  cold-start target is 400 ms (SMOOTHNESS-PLAN §1).
- **Material 3's sheet itself or its behaviours**: Material components are out (REQUIREMENTS 31), and its timer dim and
  fling-moves-sheet behaviour are worse than ours.
- **Dropping the card stack because District may not have one**: it is your requirement (21, 33), so it is only
  measured, not proposed.

---

## Part 2. Engineering detail

Path prefixes: `ds/` = `templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` at
`wp/WP03`; `nav/` = `templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/` at `wp/WP04`;
`glass/` = `ds/foundation/glass/` at `wp/WP05-glass`; `tok` = `ds/theme/tokens/DesignTokens.kt` at `wp/WP03`. Spring
progress numbers marked [D] are computed here from the stated damping and stiffness (unit mass, start at rest unless
said otherwise).

### 2.1 What the evidence can and cannot carry

- **District's toolkit is unverified** (`SOURCES.md` §2, T1-T12). The Compose lean rests on Sushi's blog (production at
  Zomato on Android), an engineering manager's self-reported Compose adoption, and one interview write-up; against it,
  Eternal's Restaurant Partner app is still Views. Nothing ties Sushi Compose to District.
- **No Android recording of a District sheet exists in public** (§9). So every sheet comparison below is "District *if*
  it ships Sushi's sheet", which makes it stock Material 3 at whatever version District builds with. Material 3 changed
  sheet motion several times (1.3.0-alpha02 spring to tween; 1.4.0-alpha15 motion scheme; 1.5.0-alpha16 nested scroll
  and drag follow the scheme; 1.5.0-alpha21 "improved dismiss gesture physics"), so even the stock behaviour depends on
  a version we do not know.
- **The Android video timings are coarse** (33 ms steps, a possibly retimed edit) and the iPhone catalogue gives no
  numbers at all. They show what to measure, not values to copy.
- **Reviews are reviews**: some praise repeats word for word on different dates (§6).

### 2.2 District's likely sheet against ours, mechanic by mechanic

Left column: stock Material 3 as read in androidx (`SOURCES.md` §7), which is what Sushi's `SushiBottomSheet` wraps.

| Mechanic | Stock Material 3 (if District = Sushi) | android-base (`wp/WP03`) | Verdict |
|---|---|---|---|
| Open | theme `defaultSpatialSpec`: standard scheme ζ 0.9, k 700 → 79 % at 100 ms, 99 % at 200 ms, 0.15 % overshoot; expressive ζ 0.8, k 380 → 66 % at 100 ms, 1.5 % overshoot [D] | `presentSpring` = `springs.sheet` 0.34 s, ζ 1, k 341.5 (`ds/component/sheet/SheetController.kt:221`, `ds/component/sheet/gesture/SheetFlingBehavior.kt:117-121`; `tok:136`) → 55 % at 100 ms, 88 % at 200 ms, 99 % at 359 ms (SHEET-SPEC §6.1) | stock is quicker; whether District is: study → **P2** |
| Close without a drag (dim tap, Back, button, code) | `fastEffectsSpec` ζ 1, k 3800 → 81 % at 50 ms, 98.5 % at 100 ms [D] | `closeByPerson(source, velocity = 0f)` (`SheetController.kt:250-253`, `:281-288`; Back: `ds/component/sheet/back/SheetBack.kt:80-89`) → `exit()` on `springs.sheet` from rest (`SheetFlingBehavior.kt:137-172`, spring at `:159`) | stock is about 3× quicker; whether District is: study → **P1** |
| Release after a drag | settles with the drag's last velocity on the spatial spec | `release()` seeds `settleTo` with the release velocity, through the band's slope when stretched (`SheetFlingBehavior.kt:23-47`, `:54-91`) | equal |
| Which stop a release picks | 56 dp positional threshold, 125 dp/s fling threshold, damped downward fling near Hidden | one stop per flick ≥ 400 dp/s; close at ≥ 2 H/s or past 48 % of the lowest-to-hidden travel; else the stop nearest a 0.099 s projection (`ds/component/sheet/gesture/SheetReleaseRule.kt:46-94`; `tok:246-251`) | ours is intent-based (iOS); keep |
| Pull past the top | surface stretched in Y, content counter-scaled | rubber band `c·x·d/(d + c·x)`, c 0.55 (`ds/component/sheet/gesture/SheetStretch.kt:17-43`, `:83-114`) | ours (no per-frame scale of content) |
| Dim | scrim colour at 0.32, faded by **state** (stays full while dragged down until Hidden is the target) | `colors.scrim` = 0x66 near-black (40 %) light, 0x8F black (56 %) dark (`tok:378`, `:421`; `ds/component/sheet/AppSheetDefaults.kt:28-33`), drawn as one rect at `presentationFraction`, i.e. from the **live offset** (`ds/component/sheet/stack/StackedSheet.kt:55-59`, `ds/component/sheet/AppSheetState.kt:158-167`, `ds/component/sheet/surface/SheetScrim.kt:47-55`) | ours tracks the finger; stock is lighter → **P4** |
| List inside the sheet | upward drag grows the sheet first; an upward pre-fling below the top is consumed by the sheet; **post-fling always flings the sheet** with what is left | the same grow-first for a modal sheet under a finger; only finger input moves the sheet, never a fling's momentum (`ds/component/sheet/gesture/SheetNestedScroll.kt:21-34`, `:43-75`) | ours (a list flung to its top cannot drag the sheet) |
| Touch during motion | `anchoredDraggable` grabs a moving sheet | `sheetCatch`: stops it, a lift without a drag finishes the move and does not tap the row (`ds/component/sheet/gesture/SheetCatch.kt:22-31`, `:75-105`) | ours |
| Predictive back | shrink ≤ 48 dp × 24 dp, `PredictiveBack.transform` | the same MDC numbers (`tok:311-318`) | equal |
| Screen behind | not touched | recedes to 0.9165 at Large on compact windows, opt-out `recedesPresenter` (`ds/component/sheet/stack/CardStackPresenter.kt:29-42`, `:63-76`; `ds/component/sheet/AppSheetProperties.kt:20-24`) | your requirement (21, 33); measured only |
| Haptic | none by default | `Threshold` haptic on arrival, 2 % of travel left (`SheetFlingBehavior.kt:70-80`) | ours |
| Shape at rest | 28 dp top corners, attached | style radius (24/30/8/28 dp) on all corners; floats 8-10 dp inset at partial stops (`tok:749`, `:760`; SHEET-SPEC D-6) | different look, both fine |

**What this means.** On mechanics we are equal or ahead. The two places a stock Material 3 sheet would plausibly feel
"snappier" are the open and the drag-free close, and both are a single spring token here.

### 2.3 Other District motions against ours

| District (evidence) | android-base | Gap |
|---|---|---|
| **O1** detail pager: ~0.2 s, decelerating, edge to edge, poster first, text after landing (observed) | `AppCarousel`: peeking pages scaled to 0.92 in the layer (`ds/component/collections/AppCarousel.kt:46-63`, `:89`); pager fling = `flingFast` decay + snap on `springs.screen.pixels` 0.344 s (`ds/foundation/scroll/AppScrollDefaults.kt:54-62`); `LocalNavigationSettled` for deferring secondary content (`nav/NavigationSettled.kt:6-23`), unused by features so far | no full-bleed detail-pager recipe; whether our page snap is slower is unknown → **P6** |
| **O2** panel with a handle over a playing trailer, fixed top bar and floating CTA (observed) | in-screen non-modal `AppSheet` with detents, a pinned footer and content under the grabber (`ds/component/sheet/SheetModality.kt:22-38`, `ds/component/sheet/AppSheet.kt:52-72`, `AppSheetProperties.kt:25-29`); catalog demos "Places" (non-modal) and "Now playing" (artwork) | the parts exist; no page shows them composed this way → **P5** |
| Grid of icons folding into sticky tabs as you scroll (iPhone) | large title moves in its layer, the inline title fades on a threshold (`ds/component/navigation/AppTopBar.kt:117-131`, `:145-151`, `:202`); `LazyListState.scrolledFromTop()` (`:54-67`) | no helper for a 0-1 collapse progress, no recipe → **P7** |
| Centred card emphasised in a snapping list (iPhone) | per-row transforms in `AppWheelPicker`; side-page scale in `AppCarousel` | no vertical recipe → **P9**; the height-changing part is left out (§2.5) |
| Best Seats intro and Ask AI sheet morph out of their trigger (iPhone) | `SheetTransition.Slide` / `CrossFade` (`AppSheetProperties.kt:134-145`); Zoom designed for phase 2 (SHEET-SPEC §20, D-31) | → **P10** |
| Press 0.95, release snaps (Sushi) | 0.96 / 0.98 / 0.90 by size, release on `springs.release` with bounce (`ds/foundation/interaction/PressableNode.kt:265`, `:279-282`, `:374-383`; `tok:211-214`) | ours |
| Shimmer: a per-placeholder modifier looping `tween(1000)`, no pause (Sushi) | one window-wide band, 1200 ms sweep then 800 ms with no frames (`ds/foundation/draw/ShimmerClock.kt:12-22`, `:66-76`; `tok:231-232`) | ours |
| Push/pop, back gesture (unknown on Android) | `ScreenMotion` 0.344 s, parallax 0.333 (`nav/transition/NavTransitions.kt:24-45`), slide clock after composition (`nav/stack/StackScene.kt:160-182`), Gmail card with velocity commit (`nav/back/BackCard.kt:43-59`, `:139-150`) | measured in the study for information |
| Scroll physics (unknown on Android) | iOS 0.998/ms decay with momentum, rubber band at every edge (`ds/theme/AppMotion.kt:193-194`, `ds/foundation/scroll/AppScrollDefaults.kt:43-48`, `:104-111`, `ds/foundation/scroll/AppOverscroll.kt:53-59`, `:91-130`) | your decision (REQUIREMENTS 41); measured for information |

### 2.4 The proposals in full

Owners follow REQUIREMENTS 44: approved work goes to `staging-wp/<package>`. Effort: S ≤ half a day, M 1-2 days,
L 3 days or more, for one package agent including tests and catalog. "Redmi" is the Redmi Note 10 Pro Max
(Snapdragon 732G, Adreno 618, 1080 × 2400, 8.33 ms frames at 120 Hz).

#### P1. Quicker sheet close when nothing is dragging it

- **District**: unknown on Android. If it ships Sushi's sheet, a tap outside or Back hides it on Material 3's fast
  effects spring (ζ 1, k 3800): about 81 % gone at 50 ms, 98.5 % at 100 ms [D from `SOURCES.md` §7 tokens].
- **Ours**: a dim tap, Back (the key, or a predictive gesture's commit) and the close button call
  `closeByPerson(…, velocity = 0f)` (`SheetController.kt:250-253`, `:281-288`; `SheetBack.kt:80-89`), and
  `AppSheetState.hide()` passes the sheet's current velocity, zero at rest (`ds/component/sheet/AppSheetState.kt:239-246`).
  All of them run `exit()` on `springs.sheet` (0.34 s): from rest, 55 % at 100 ms, 88 % at 200 ms, 97 % at 300 ms
  (`SheetFlingBehavior.kt:137-172`). A drag-to-close already leaves at the finger's speed.
- **Change**: a `sheetDismiss` spring in `DesignTokens.Motion` and `tokens/motion.json`, used by `exit()` only when it
  starts from rest (no meaningful velocity); drag dismissals keep `springs.sheet` seeded with the release velocity. Starting value from the
  study; without one, 0.22-0.24 s critical (k 685-816: 74-78 % at 100 ms, 97-98 % at 200 ms [D]). The dim, the card
  stack's recede and the glass opacity follow the sheet's offset, so they speed up with it at no extra work.
- **Owner**: WP03 (token, `exit()`, tests, catalog), WP07-B later for Multiplatform parity.
- **Cost on the Redmi**: none added; about 0.15-0.2 s fewer frames of sheet motion and of the D2 dim per close.
- **Effort**: S.
- **Risk**: iOS closes sheets on the same curve it opens them (about 0.34 s); a 0.22 s exit of a tall sheet with the
  card stack un-receding at the same time may read as abrupt, which is why the value comes from the study and a
  side-by-side. CP1 kept `sheet` at 0.34 s (decided for you by the decision agent, `decisions/CP1.md`); this splits a
  token rather than reversing that. Reduce motion is unaffected (already one frame).
- **Evidence**: Material 3 source (confirmed); District ships it (unverified).
- **Needs the phone study**: yes (scenes S2, S3).

#### P2. Quicker sheet open

- **District**: unknown. If stock Material 3: 79 % at 100 ms (standard scheme) or 66 % (expressive), against our 55 %.
- **Ours**: `presentSpring` = `springs.sheet` 0.34 s, or `springs.dialog` 0.275 s for dialog-like sheets
  (`SheetController.kt:221`; `SheetFlingBehavior.kt:98-131`).
- **Change**: a `sheetPresent` token (or a new `sheet` value) set from the study. Candidates already in the system:
  `dialog` 0.275 s (66 %, 94 % at 100/200 ms [D]) or `thumb` 0.28 s (66 %, 94 %); the Snappy motion style's sheet
  0.2957 s (63 %, 92.5 %). Detent settles after a drag stay on `sheet` with the finger's speed.
- **Owner**: WP03.
- **Cost on the Redmi**: none added. A quicker present makes a slow first frame more visible (it skips a larger share
  of the travel), so P3's measurement goes with it.
- **Effort**: S.
- **Risk**: as P1: iOS parity and CP1's token; also the arrival haptic fires at 2 % of travel left, which comes
  sooner on its own and needs no change.
- **Evidence**: as P1.
- **Needs the phone study**: yes (S1).

#### P3. Cheaper first frame when a sheet opens

- **District**: unknown; reviews call it snappy. The study compares the slowest of the first three frames after the
  tap and the vsyncs skipped after it, the same way `tools/perf/parse_framestats.py` measures our taps.
- **Ours**: content composes when the sheet is shown; `keepContent` keeps it composed between openings, opt-in
  (`AppSheetProperties.kt:30-34`, `AppSheet.kt:125`); composing on press-down is phase 2 (SHEET-SPEC §20 last bullet,
  SMOOTHNESS-PLAN S3.5).
- **Change**: only if our sheet's opening frame is measurably slower than District's (more than one 8.33 ms frame, or
  a skipped vsync District does not have): bring SHEET-SPEC §20's paused precomposition on press-down forward for sheet
  triggers (`SubcomposeLayoutState.createPausedPrecomposition`, resumed within frame deadlines), and document
  `keepContent` for sheets a screen opens often.
- **Owner**: WP03 (overlay host) and WP04 (presentation host, destination sheets).
- **Cost on the Redmi**: composition moves into the 50-150 ms between press and release, inside frame deadlines, so no
  new frame cost; memory for the precomposed content (and for kept content while it is kept); wasted work when the
  press becomes a scroll (the precomposition is disposed).
- **Effort**: M.
- **Risk**: new API surface in the hosts; state and semantics of a precomposed but unplaced sheet; must not run for
  every row of a list (only for an explicit sheet trigger).
- **Evidence**: none yet (reviews only). **Needs the phone study**: yes (S1 timing pass).

#### P4. Lighter dim behind sheets

- **District**: if stock Material 3, the scrim colour at 32 %.
- **Ours**: 40 % near-black light, 56 % black dark (`tok:378`, `:421`), merged with the card-stack overlay to 46 % /
  60 % at Large (SHEET-SPEC §6.4). SHEET-SPEC §21 already plans a dim comparison (against iOS) and §22 keeps
  `colors.scrim` until QA2 compares.
- **Change**: add District to that comparison; if you prefer it, the planned `sheet.dim` token feeding
  `AppSheetDefaults.colors(scrim)` (SHEET-SPEC §21 row 2), for example 0.32 light / 0.48 dark. The mechanism (one rect,
  following the finger) stays.
- **Owner**: WP03 (QA2 measures).
- **Cost on the Redmi**: none (the same rect at another alpha).
- **Effort**: S.
- **Risk**: less separation in light theme; the modal shadow rule (drawn only when the scrim alpha is below 0.2,
  D-18) is not crossed at 0.32.
- **Evidence**: Material 3 source (confirmed); District (unverified). **Needs the phone study**: yes (S2, dim patch).

#### P5. Catalog recipe: a page with a panel over a playing picture (District O2)

- **District**: observed: a dark rounded panel with a grab handle rises over the auto-playing trailer in about 0.5 s;
  the top bar and a floating "Book tickets" bar stay fixed. Sheet or scroll: unknown.
- **Ours**: the parts exist (in-screen `SheetModality.NonModal(upThrough)`, `Peek`/`Fraction` detents, pinned
  `footer`, `contentUnderGrabber`; references in §2.3), but no page composes them.
- **Change**: a catalog page (and a docs recipe) "Detail over media": a pinned hero (an image, or an animated gradient
  standing in for video) under a fixed top bar; an in-screen non-modal `AppSheet` resting at a `Fraction` detent that
  the person drags or scrolls to `Large`; the CTA in the sheet's `footer`. If the study shows District's panel is a
  plain scroll (no snapping), the recipe instead uses a list whose first item is a hero-height spacer over the pinned
  hero, with the hero dimmed by one rect over its own band only.
- **Owner**: CAT (REQUIREMENTS 43), WP12 (recipe). No design-system change expected.
- **Cost on the Redmi**: a non-modal sheet drag is re-placement (L1), plus one round rect re-recorded per frame while
  it floats inset (D1, SHEET-SPEC §6.3), and it draws no dim below `upThrough`; the pinned hero never re-lays out. A
  real video would be a `SurfaceView`, composited by the system rather than drawn by the app, but the catalog uses no
  video, so no Media3 dependency.
- **Effort**: S-M.
- **Risk**: low; a `SurfaceView` under a receding presenter is an open SHEET-SPEC §21 item, not touched here because a
  non-modal sheet at partial does not recede.
- **Evidence**: observed (likely). **Needs the phone study**: yes (H2: sheet or scroll, does the trailer dim).

#### P6. Catalog recipe: swipe between detail pages, and a paging-speed check (District O1)

- **District**: observed: lands in about 0.2 s, edge to edge, no scale or parallax; poster first, skeleton bars for
  the text, trailer about 0.1 s and text 0.25-0.3 s after landing.
- **Ours**: `AppCarousel` is a peeking card carousel; the pager fling snaps on the `screen` spring 0.344 s
  (`AppScrollDefaults.kt:54-62`); the settle-then-load rule exists for navigation (`nav/NavigationSettled.kt:6-23`) and
  applies the same way to a pager's `settledPage`.
- **Change**: (a) a catalog/sample recipe: a full-bleed `HorizontalPager` with `AppScrollDefaults.pagerFlingBehavior`,
  keyed pages composed from what the list already has (title, poster), secondary sections loaded once
  `settledPage == page`; (b) only if the study shows District's page landing is clearly quicker under the same scripted
  flick, a full-width-paging snap on `thumb` (0.28 s) instead of `screen`.
- **Owner**: CAT and WP02-M3 (recipe and the sample's adoption of S3.2); WP03 for (b).
- **Cost on the Redmi**: pages move by translation; Compose prefetches the next page within frame deadlines while the
  finger drags; deferring secondary sections keeps them out of the settle frames. Memory: one prefetched page.
- **Effort**: M for (a), S for (b).
- **Risk**: (b) touches a token shared with carousels and the date picker, so it would be a separate paging token.
- **Evidence**: observed (likely, 33 ms steps). **Needs the phone study**: yes (T4).

#### P7. A scroll-linked header helper (District's icon grid folding into tabs)

- **District**: confirmed on iPhone (60fps.design breakdown; iPhone review frames show both end states). Android
  unknown.
- **Ours**: `AppLargeTitleTopBar` moves its title in the layer and fades the inline title on a threshold
  (`AppTopBar.kt:117-131`, `:145-151`, `:202`); no general collapse-progress API.
- **Change**: a small design-system helper returning the collapse progress as a `() -> Float` read only in layout and
  draw (built on `scrolledFromTop()`), plus a catalog recipe: icons scale and move in their own layers, labels fade with
  `ModulateAlpha`, a sticky tab row appears as a `stickyHeader`. Nothing re-measures per frame; text is never scaled.
- **Owner**: WP03 (helper), CAT (recipe).
- **Cost on the Redmi**: per scroll frame, a handful of small layer property changes (L0) and one small faded label
  layer per icon; no offscreen buffer, no full-screen work.
- **Effort**: M.
- **Risk**: API growth; must coexist with tab-bar minimize and pull to refresh (all read the same scroll state);
  scaling text would re-rasterise glyphs every frame, so labels fade instead.
- **Evidence**: iPhone only. **Needs the phone study**: yes (H1: does Android have it, scrubbed or triggered).

#### P8. Docs rules from District's reviews: intros once, choices survive interruptions

- **District**: confirmed by Play reviews: the Best Seats animation replays on every open; after a call, the booking
  page came back with its date changed.
- **Ours**: sheets keep their state across recreation (SHEET-SPEC D-40, §5.14); there is no "show once" helper or rule.
- **Change**: two `AGENTS.md`/docs rules for generated apps: a feature intro plays once per install (or per version),
  remembered in preferences, skippable, and shortened under reduce motion; every choice a flow collects is in
  `SavedStateHandle`, with the restore scenario covering a process death mid-flow. Optionally a tiny `OnceFlags` store
  in `:core:data`.
- **Owner**: WP12 (rules), WP06 (helper, if wanted).
- **Cost on the Redmi**: none per frame; one preferences read where an intro is considered.
- **Effort**: S. **Risk**: low. **Evidence**: confirmed (reviews). **Needs the phone study**: no.

#### P9. Catalog recipe: a snapping list whose centred card is emphasised

- **District**: iPhone only: the centred card is full size with its image, the others are dim compact rows; the list
  snaps.
- **Ours**: per-row transforms exist in `AppWheelPicker`; no vertical card recipe.
- **Change**: a recipe with `rememberSnapFlingBehavior` (centred) and per-card scale and alpha from the distance to
  the viewport's centre, read in `graphicsLayer`. District's height change is left out (§2.5).
- **Owner**: CAT. **Cost on the Redmi**: L0 per visible card. **Effort**: S-M. **Risk**: low (a recipe, not a
  component). **Evidence**: iPhone only. **Needs the phone study**: yes (optional scene F4).

#### P10. Sheets that grow out of the button that opened them

- **District**: iPhone only: the Best Seats modal arrives from the tapped control; the Ask AI field morphs.
- **Ours**: Slide and CrossFade; `SheetTransition.Zoom(source)` is designed and deferred to phase 2 (SHEET-SPEC §20,
  D-31).
- **Change**: only if the study shows District's Android modals grow from their trigger and you want it now: build
  §20's Zoom as specified (rect and corner lerp of the surface, content fade with `ModulateAlpha`, dim follows the
  morph, a finger can take over mid-morph). No glow or beam.
- **Owner**: WP03, WP04 (destination sheets).
- **Cost on the Redmi**: the surface re-records one round rect per frame (D1), one small-to-full fade without an
  offscreen buffer, the D2 dim; no blur.
- **Effort**: L. **Risk**: the most complex item here; adds a source registry and gate rows.
- **Evidence**: iPhone only. **Needs the phone study**: yes (S1 on a Best Seats or similar modal, if it appears).

### 2.5 Left out, with the reason

| Item | Why it stays out on the Redmi or under the rules |
|---|---|
| Live blur behind a modal sheet or a full screen (if District does it) | A full-screen `RenderEffect` renders offscreen every frame the sheet or the content moves; push/pop already measured GPU p90 12 ms against the 8.33 ms frame (SHEET-SPEC D-17); Haze measured 23-46 % higher CPU P90 for backdrop blur on a Pixel 8a (`SOURCES.md` §7); window blur is disabled in battery saver and costly. The one designed exception stays: Glass sheets at partial detents on the Live tiers only (SHEET-SPEC §6.6, `glass/GlassQuality.kt:17-48`) |
| Glow borders, light beams | blur or wide soft shadows per frame; static artwork is fine for apps that want the look |
| Cards whose height follows scroll (accordion) | re-measures and re-places the list every frame; the layer-only version is P9 |
| Always-running idle loops as defaults (3D character, bouncing icon, rotating banner, orbiting pins) | an idle screen must draw nothing (`ShimmerClock` stops between sweeps); loops keep 120 Hz frames and the GPU busy, cost battery, and compete with scrolling |
| Lottie by default | renders on the UI thread every frame, adds size and memory; Zomato's file-size numbers are a vendor case study. Could be an opt-in feature later; not proposed |
| Animated logo splash | delays first content against the 400 ms cold-start target (SMOOTHNESS-PLAN §1); the system splash covers launch |
| Material 3's sheet or its behaviours (timer dim, list fling moves the sheet, Y stretch) | REQUIREMENTS 31 forbids Material components; each behaviour is worse than ours (§2.2) |
| Removing the card stack | your requirement (21, 33); the study records whether District recedes, for information |
| Auto-playing video in template screens | a decoder and its buffers in memory and battery on every visit; recipes show the slot with an image |

### 2.6 What the study decides

| Measurement (`PHONE-STUDY.md`) | Result | Then |
|---|---|---|
| District sheet open, from rest (S1): t90 and fitted spring | t90 ≤ 0.17 s (clearly under our 0.21 s) | P2 with the fitted value, or the nearest existing token |
| | t90 within 0.03 s of ours | drop P2 |
| District drag-free close (S2, S3): t90 | ≤ 0.12 s | P1 with a value between District's and ours, chosen from a side-by-side |
| | about ours (0.21 s) | drop P1 |
| Opening frame UI ms and skipped vsyncs (S1 timing) | ours worse by more than a frame | P3 |
| Dim alpha (S2 patch) | lighter than ours | P4, if you like it in the side-by-side |
| Movie page panel (H2) | snaps to stops | P5 with a sheet; coasts with momentum → P5 with a list |
| Page swipe (T4): landing t90 under the same flick | clearly quicker than ours | P6 (b) |
| Home header (H1) | folds with scroll on Android | P7 confirmed |
| Release velocity (S7, S8): carry | District drops it (carry ≤ 0.35) | nothing to take; ours keeps it |
| Classification across trials | spring / tween / ViewDragHelper | records which toolkit family District's sheet behaves like |

### 2.7 Sources

`SOURCES.md` in this folder holds every URL with its confidence: the Play and App Store listings, the Play reviews,
Zomato's compose-sushi repository files (`SushiBottomSheet.kt`, `ScaleOnPress.kt`, `Spring.kt`, `Shimmer.kt`,
`SushiViewFlipper.kt`), the androidx `material3` and `foundation` sources and release notes, MDC's
`BottomSheetBehavior.java`, the aspira design UX review (https://www.youtube.com/watch?v=l4VzORzIHiQ), 60fps.design's
District pages, and the performance pages on developer.android.com and source.android.com. Template evidence is cited
by branch, file and line throughout. Nothing from District was downloaded, decompiled or copied.
