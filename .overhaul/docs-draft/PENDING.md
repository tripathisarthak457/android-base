# Pending: performance techniques that wait for approval

Nothing in this file is a rule yet, and none of it appears in `PERFORMANCE.md` or `AGENTS-rules.md`.
Each entry is one proposal from the Telegram study (REQUIREMENTS 48), the District study (REQUIREMENTS
45) or the overlay metrics design (REQUIREMENTS 49). Each is asked one at a time, and the docs ship in
the same change as the code (REQUIREMENTS 50).

For every entry:

- **Status** is what the study recommends and what decides it.
- **Ships with** is the code change the text describes.
- **Text for `docs/PERFORMANCE.md`** is ready to paste into the named section once the code is merged.
  Values in `{braces}` come from the acceptance run on the reference phone and must be filled in from
  it. Never paste a projection as a measurement.
- **Line for `AGENTS.md`** goes into the "Smooth and light" section of `AGENTS-rules.md`, where the
  entry adds an enforceable rule.

Template facts below are read from `staging` at `b21f0a981`. Where a study cites older code, the entry
says what has changed since.

---

## Telegram (REQUIREMENTS 48; source `research/telegram/RESEARCH.md`, revision 2; phone study not run)

### T-P1. Hold decorative motion still while its screen moves

**Status.** Phone check: a back gesture over a screen that is still loading, texture renders counted per
gesture (`research/back-google/sql/layerts.sql`; today expected more than one, after it one), plus the
`pushpop_gesture` rows. Effort S-M. The study's `AppLottie` part is moot: Lottie was left out of the
template (`removed.toml:11`).

**Ships with.** `TabVisibility` gains `isStill` (default true; false while a slide, the back card or a
presentation moves the entry); `StackLayout` builds each entry's holder with the stack's settled signal and
the card's `active`; the presentation and sheet hosts do the same for covers and sheets. The skeleton's
node, `AppActivityIndicator` and `AppPullToRefresh`'s spinner wait for `isVisible && isStill`. The pull to
refresh spinner reads `LocalTabVisible` for the first time with this change: on staging it steps every
125 ms while refreshing, even in a hidden tab.

**Text for `docs/PERFORMANCE.md`** (section "Motion and springs", after "Nothing animates while idle"):

~~~markdown
#### Decoration holds still while its screen moves

**Rule.** Anything decorative that runs on a clock (a skeleton sweep, an activity indicator, a refresh
spinner, an image fade) waits while its screen moves: `LocalTabVisible.current.isStill` is false while a
slide, the back card or a presentation moves the screen. Read it with `isVisible`, in an effect or a draw
block, never in composition.

**Why.** Each step of a clock redraws its layer on every slide frame, and under the back card any change
inside the card's screen renders the whole full-screen texture again (a texture render measured
+2-4 ms of GPU). Holding still costs one plain draw at each end of the motion: a bone draws its plain
colour, so the shimmer band disappears for the motion (about 0.36 s for a push) instead of freezing
mid-sweep.

```kotlin
val visibility = LocalTabVisible.current
LaunchedEffect(visibility) {
    snapshotFlow { visibility.isVisible && visibility.isStill }.collectLatest { running ->
        while (running) {
            delay(stepMillis)
            head = (head + 1) % SpokeCount
        }
    }
}
```

**Don't.** Read `isStill` in composition (it flips at the start and the end of every motion), or tie a
clock to `isVisible` alone.

**Protects.** One texture render per back gesture (measured {before} → {after} over {n} gestures);
`pushpop_gesture.hitch_ms_per_s` {before} → {after} ms/s.
~~~

**Also in `docs/PERFORMANCE.md`, once it ships:**

- "The back gesture": in the paragraph that begins "The texture is rendered once per gesture only while
  the screen in the card holds still", replace its last sentence ("No signal tells a screen that it is in
  the card … keep animating in it.") with: "A screen in the card is not still (`isStill` is false), so
  the template's own clocks (the skeleton shimmer, the activity indicator, the pull-to-refresh spinner)
  hold still in it; see 'Decoration holds still while its screen moves'."
- "Nothing animates while idle": delete "The pull-to-refresh spinner, which turns only while a refresh
  runs, does not yet." (this change makes it read `LocalTabVisible`).
- `AGENTS-rules.md` merge note 6: drop the pull-to-refresh bullet.

**Line for `AGENTS.md`.** "A clock-driven decoration waits for `isVisible && isStill` from
`LocalTabVisible`, read in an effect or a draw block. — *review*"

### T-P2. Hold new screen data until the slide has landed

**Status.** Phone check: `perf.sh recompose` (push frames that recompose, 5 today, at most 2 inside the
slide after), plus a heavier detail screen. Effort S.

**Ships with.** `MviScreen` keeps `collectAsStateWithLifecycle()` unchanged, shows the first state at
once, and applies later states only once `LocalNavigationSettled.current()` is true, conflated, with a cap
of 600 ms from the start of a hold. The hold is released at once when the content calls the `onEvent`
lambda `MviScreen` passes it (events a route sends straight to the ViewModel, such as `Appeared` from a
`LaunchedEffect`, never release it). Effects are not held. Outside a host the local is always settled.

**Text for `docs/PERFORMANCE.md`** (section "Screen transitions", after "A destination's first
composition is final"):

~~~markdown
#### New data waits for the landing

`MviScreen` shows a screen's first state at once and holds every later one until the slide has landed
(`LocalNavigationSettled` true, at 99 % of the travel), then applies the latest in one recomposition. A
hold never lasts more than 600 ms, and ends at once when the screen sends an event through the `onEvent`
it was given. Effects (navigation, messages) are never held.

**Why.** Every new state that lands during the slide recomposes the arriving screen inside the motion: a
push recomposed in 5 frames, which this moves to the landing frame, where the page is about 11 px from
rest. Data that arrives early in the slide shows up to about 0.35 s later than it would without the hold.

**For a screen.** Nothing to do. A screen that must show a change during its own arrival (an inline error
from an event the person just sent) gets it at once, because the event released the hold.

**Protects.** `recompose.push.max` {measured} (was 5).
~~~

### T-P3. Limits for the image loader

**Status.** Phone check: a catalog list of 200 image rows flung with 80, 150 and 300 ms swipes (janky,
GPU p90, Perfetto compose slices per row), before and after. Effort S-M. The study's first half is already
on staging: `AppNetworkImage` draws `AsyncImage` with painters (`core/ui/…/AppNetworkImage.kt:60-67`). What
remains is the loader.

**Ships with.** The `SingletonImageLoader.setSafe` builder in `BaseAppApplication` gains an explicit
memory-cache bound, keeps hardware bitmaps on, sets `bitmapFactoryMaxParallelism` from a measurement on
the reference phone, and uses a short cross-fade only outside lists. A build with Coil and without the
network module gets the same limits from a `<opt:coil&!network>` block, without the OkHttp fetcher.
Exactly one loader per build, with a test that the singleton's fetcher uses the app's `OkHttpClient`.

**Text for `docs/PERFORMANCE.md`** (section "Images", after "One image loader, on the app's client"):

~~~markdown
The loader's limits are set, not Coil's defaults: a memory cache of {bound}, hardware bitmaps, at most
{n} decodes at once (measured on the reference phone, whose 2 big and 6 little cores make decode
parallelism a trade between filling images in and competing with the frame), and no cross-fade inside a
list. A build without the network module builds the same loader without the OkHttp fetcher.

**Protects.** A list of image rows flung hard: janky {measured} %, GPU p90 {measured} ms.
~~~

### T-P4. A push may wait for its first content

**Status.** Phone check: tap to first moving frame, `recompose.push`, the first three frames' GPU, on the
sample with a delayed repository. Effort M. Risks: tap-to-motion latency (press feedback must cover it),
Back during the wait must cancel the push, rapid pushes queue.

**Ships with.** An opt-in per destination (navigation metadata, as `NavTransitionStyle` is). The arriving
entry stays parked off the window until it reports ready or a cap passes (200 ms; 250 ms while the
keyboard is up), is drawn once under the current page as `TabWarmUp` draws a tab, then slides. A small
readiness contract in `core/mvi`: a state may say "my first content is here"; `MviScreen` reports ready
from the ViewModel's raw state, never from T-P2's held state, and the wait does not count as moving.

**Text for `docs/PERFORMANCE.md`** (section "Screen transitions", after "A destination's first
composition is final"):

~~~markdown
#### A destination may wait for its content

A destination registered with `waitsForContent` (opt-in) is composed off the window when it is pushed
and slides in once its state reports its first content, or after 200 ms (250 ms with the keyboard up),
whichever is first. It is drawn once out of sight before the slide, so its first glyph and texture uploads
are paid before anything moves. Use it for a screen whose data is usually not in memory and whose content
would otherwise arrive mid-slide; a screen seeded from `peek(id)` does not need it.

**Cost.** Up to the cap of latency between the tap and the motion on an uncached load, none when the data
is in memory. Back during the wait cancels the push.

**Protects.** `recompose.push.max` {measured}; first three frames' GPU {measured} ms.
~~~

### T-P5. Keyboard: one layout, then a slide

**Status.** Phone check: open and close the keyboard on a screen with `AppBottomActionBar` and a list,
with Perfetto measure slices and frame stats, before and after. Effort S-M. API 26-29 keep today's
behaviour.

**Ships with.** Outside a sheet, `AppBottomActionBar` pads by the navigation bar only and rides the
keyboard in placement through a layout modifier reading `WindowInsetsRulers.Ime`; the content's bottom
inset is written once per keyboard change with the animation's target (opening: the target at once;
closing: kept until the animation ends). Inside a sheet nothing changes.

**Text for `docs/PERFORMANCE.md`** (section "Keyboard", replacing its "Known cost" paragraph):

~~~markdown
A screen with `AppBottomActionBar` lays out once when the keyboard starts to move and once when it
stops: the bar rides the keyboard in placement, and the list's bottom inset takes the keyboard's final
height at the start of an opening and keeps it until the end of a closing. On the frames between, nothing
is measured. On opening, a list scrolled to its end moves up once on the first frame instead of gliding;
on closing, blank space stays under the list until the keyboard is gone.

**Protects.** About 34 measure passes of the bar, the scaffold and the list each way, removed; keyboard
open and close janky {measured} %.
~~~

### T-P6. Sheets and covers drawn from one texture while they open and close

**Status.** Needs the owner's OK for a rule exception: it extends the "no whole screen offscreen"
exception granted for the back card only. Decided on the phone by the ladder method of
`research/back-google/DESIGN.md` §5 (texture renders per open, present latency, RenderThread per frame).
Comes after T-P1 and T-P2, because any change inside the surface renders the whole texture again. Effort
M.

**Ships with.** An innermost `graphicsLayer` on the sheet or cover surface whose compositing strategy is
`Offscreen` only during a present or dismiss the sheet drives itself, while its content is still; never
while a finger holds it, never for a glass sheet that samples its backdrop, never for a pop. A push
texture only as an experiment. Each case named in the exception's text and the code comment, with a layer
test like `SlideRecordingTest`.

**Text for `docs/PERFORMANCE.md`** (section "Screen transitions", after the back card's table):

~~~markdown
**The exception also covers sheets and covers while they present and dismiss themselves.** For the
length of a present or a dismiss that no finger drives, a sheet's or a cover's surface is drawn from one
texture rendered once, then moved and faded as one quad. It is never a texture while a finger holds it,
for a glass sheet, or for a pop. Measured on the reference phone: RenderThread per moving frame
{before} → {after} ms, first frame +{n} ms GPU, {n} MB of GPU memory for a full-screen cover while it
moves. Anything that changes inside the surface while it moves renders the whole texture again, which is
why decoration holds still and new data waits for the landing.
~~~

### T-P7. Fetch list pages further ahead

**Status.** Phone check: 80 ms flings down the feed, counting flings that end at the footer's spinner.
Effort S. No frame cost.

**Ships with.** The feed's `PagingConfig`: `prefetchDistance` about two screens of rows, `pageSize` near
30, `initialLoadSize` two pages (today 3, 10 and 10, `data/feed/…/NetworkFeedRepository.kt:22-36`).

**Text for `docs/PERFORMANCE.md`** (section "Lists and very large data"):

~~~markdown
#### Fetch a page before a fling can reach the end

**Rule.** A paged list asks for its next page while at least two screens of rows remain
(`prefetchDistance`), with pages of about 30 rows and a first load of two pages.

**Why.** The template's fling keeps 0.998 of its speed per millisecond, so a 6000 px/s fling coasts about
3000 px, 10 to 20 rows of a card feed. A next page requested 3 rows before the end arrives after a hard
fling has already stopped at the footer's spinner.

**Protects.** Flings that end at the footer: {before} → {after} of {n}.
~~~

**Line for `AGENTS.md`.** "A paged list prefetches at least two screens of rows ahead. — *review*"

### T-P8. A hitch slows a slide instead of making it jump

**Status.** Phone check: a dev-only stall injected mid-slide, recorded with and without the cap. Effort S.
Whether people prefer the slowdown is not known.

**Ships with.** A `MonotonicFrameClock` wrapper, like the back card's `FromRelease`, that caps each step
at two vsync periods (16.7 ms at 120 Hz), used by self-driven moves only: the slide, sheet present and
dismiss, the back card's commit.

**Text for `docs/PERFORMANCE.md`** (section "How the shell moves screens", one bullet):

~~~markdown
- **A late frame slows a slide, it does not skip it.** Self-driven moves (a slide, a sheet presenting or
  dismissing itself, the back card's commit) advance at most two vsync periods per frame, so after a 50 ms
  stall the slide carries on from where it was instead of jumping about 350 px at its peak speed. Motion a
  finger drives stays on the finger.
~~~

### T-P9. Rules for very large data

**Status.** No phone check. Effort S. Two of its points are already code rules (`subcompose-image`,
`adaptive-box-constraints`); the rest is new.

**Ships with.** Docs only, plus T-P1 and T-P7 if approved.

**Text for `docs/PERFORMANCE.md`** (section "Lists and very large data"):

~~~markdown
#### Very large data

- Map rows to ready-to-draw UI models off the main thread, in the repository or the ViewModel: formatted
  strings, `AnnotatedString`s, dates. A row only lays out and draws.
- Split long text into lazy items by paragraph, so a long article is a list, not one text layout.
- A refresh that replaces the whole list swaps without item animations: animating every row of a new
  list is a full-list placement animation for no visible reason.
~~~

**Line for `AGENTS.md`.** "Rows take ready-to-draw models built off the main thread; long text is lazy
items by paragraph; a whole-list refresh swaps without item animations. — *review*"

### T-P10. Swipe back from anywhere on the page (look and feel)

**Status.** Optional, off by default; extends the back card's texture exception to this gesture; the
commit rule is the owner's to pick (Telegram: a third of the width or 3500 px/s; iOS: half the width or 1
width per second). Effort M-L. Risks: conflicts with carousels, sliders, swipe actions, maps and text
selection; RTL; two back gestures to keep consistent.

**Ships with.** A nested-scroll-aware horizontal drag on each stack entry that loses to every horizontal
scroller and draggable control under the finger, driving the slide 1:1 with the same strips and scrim,
the top page from one texture, settling on the `screen` spring seeded with the finger's speed. Starting
recognition values: 0.4 cm (about 23 dp on the reference phone) and a mostly sideways move (dx / 3 > |dy|).

**Text for `docs/PERFORMANCE.md`** (section "The back gesture"):

~~~markdown
**Swipe back from anywhere (optional).** With `{option}` on, a horizontal drag that starts anywhere on a
pushed page and is not taken by a horizontal scroller or a draggable control under the finger drives the
slide back 1:1. It commits past {rule} and settles on the `screen` spring with the finger's speed. The
page is drawn from one texture for the drag, as the back card is: RenderThread {n} ms per frame,
{n} MB of GPU memory while dragging, +{n} ms GPU on the first frame.
~~~

### T-P11. A quick push (look and feel)

**Status.** Optional, opt-in only. It fades a whole screen, so it needs the same kind of written exception
as T-P6 and the owner's OK; cheap only with T-P6's texture. Effort M.

**Ships with.** A third `NavTransitionStyle`: 150 ms, the arriving page fading in and moving 48 dp, the
page beneath still; its duration and curve a motion token, never a raw tween.

**Text for `docs/PERFORMANCE.md`** (section "How the shell moves screens"):

~~~markdown
- **A quick push (opt-in).** `NavTransitionStyle.{Name}` fades the arriving page in over {n} ms while it
  moves 48 dp, with the page beneath still: about 18 frames instead of about 44. The page is drawn from one
  texture for the fade, under the same exception as the back card; without it, the fade would render the
  whole page offscreen on every frame.
~~~

---

## District (REQUIREMENTS 45; source `research/district/RESEARCH.md` and `PHONE-RESULTS.md`)

District's own scenes were never measured (the catalog pre-flight failed), so every District number below
is still open. One decision comes before D-P1, D-P2, D-P4, D-P6 (b) and D-P10: whether to finish the
study (fix the tap-safety check and the dim meter, run the pre-flight again, then the District pass).
Every comparison is measured with measured, on the same phone and tools, median of three; `D` is
District's value and `O` ours.

### D-P1. A quicker sheet close when nothing drags it

**Status.** Keep, pending District's close. Decision: against a frame read, D-P1 if `D` ≤ 136 ms, a
side-by-side if 136-186 ms, drop at 186 ms or more (against a model read: 131 / 181 ms). Ours, measured:
the `sheet` spring from rest, t50 91 ms, t90 about 211 ms. Risk: iOS closes on the curve it opens with.

**Ships with.** A `sheetDismiss` spring in `DesignTokens.Motion`, used only by the Slide exit when it
starts from rest (a dim tap, Back, the close button); a drag dismissal keeps `sheet` seeded with the
release velocity. Starting value without a study: 0.22-0.24 s, critically damped.

**Text for `docs/PERFORMANCE.md`** (section "Motion and springs", a row in the spring table, and a line
under it):

~~~markdown
| `sheetDismiss` | {response} s | 0 | a sheet closing from rest: a tap outside, Back, a close button |

A sheet a finger flings away leaves on `sheet` with the finger's speed; one closed from rest leaves on
`sheetDismiss`, {n} ms to 90 %. The dim and the card stack follow the sheet's position, so they speed up
with it at no extra cost.
~~~

### D-P2. A quicker sheet open

**Status.** Keep, pending. Decision from `O` = 217 ms (t90): D-P2 if `D` ≤ 167 ms, a side-by-side if
167-197 ms, drop at 197 ms or more (each about ±5 ms of fit). Ours: `sheet` t90 217 ms; the Snappy style
184 ms; dialog-like sheets already open on `dialog`, 174-175 ms. If District lands between 167 and 197 ms,
consider whether the Snappy style already covers it before adding a token. A quicker present makes a slow
first frame more visible, so D-P3 goes with it.

**Ships with.** A `sheetPresent` token set from the study (candidates: `dialog` 0.275 s, `thumb` 0.28 s,
Snappy's `sheet` 0.2957 s); detent settles after a drag stay on `sheet`.

**Text for `docs/PERFORMANCE.md`** (the spring table):

~~~markdown
| `sheetPresent` | {response} s | 0 | a sheet arriving by itself; settles after a drag stay on `sheet` |
~~~

### D-P3. A cheaper first frame when a sheet opens

**Status.** Recommended: approve, changed. Measured on our template whatever District does: every
list-sheet open (18 of 18) misses its opening frame with 13.5-24.2 ms of main-thread work (animation
phase 1.8-2.5 ms, measure and layout 10.0-16.1 ms, draw 1.7-6.3 ms) and adds a vsync of tap-to-present
(46 against 37.6 ms for a light sheet). Target: our own budget, opening main-thread work within one
8.33 ms frame. Judge by the measure-and-layout share, with a real or held press (a scripted tap has no gap
between down and up). Costs memory for the precomposed content and wasted work when the press becomes a
scroll; never for every row of a list. Effort M.

**Ships with.** For an explicit sheet trigger, precompose and premeasure the list's first screen of items
on press-down (`SubcomposeLayoutState.createPausedPrecomposition`, resumed only before frame deadlines),
disposed if the press becomes a scroll; documented `keepContent` for sheets a screen opens often.

**Text for `docs/PERFORMANCE.md`** (section "Sheets", replacing the "Known cost" bullet):

~~~markdown
#### A sheet's first frame is paid before the tap ends

**Rule.** A control that opens a sheet with a list inside passes `{api}`: the sheet's content and its
first screen of rows are composed and measured during the press, in slices that stop before each frame is
due, and thrown away if the press becomes a scroll. A sheet a screen opens often can keep its content
composed between openings with `AppSheetProperties(keepContent = true)`, at the cost of the memory it
holds.

**Why.** Measured on the reference phone, a list sheet spent 13.5-24.2 ms of main-thread work on its
opening frame, 10-16 ms of it laying out the list's first rows, so every open missed its first frame and
appeared a vsync later than a light sheet.

**Protects.** Opening frame's main-thread work {before} → {after} ms (budget 8.33 ms); tap to present
{before} → {after} ms.
~~~

### D-P4. A lighter dim behind sheets

**Status.** Keep, pending; offered in a side-by-side if District's dim is lighter than ours by 0.05 or
more. Ours, measured: 0.567 dark, 0.406 light (the tokens 0.56 and 0.40; 46 % and 60 % merged with the card
stack at the large detent). The modal shadow rule (a shadow only under a scrim below 0.2) is not crossed.

**Ships with.** A `sheet.dim` token feeding the sheet's scrim colour (for example 0.32 light, 0.48 dark).
The mechanism, one rectangle following the finger, stays.

**Text for `docs/PERFORMANCE.md`** (section "Sheets", the "Dim" bullet): "… drawn once, at {light} in
light and {dark} in dark theme at a partial detent …". No cost change: the same rectangle at another alpha.

### D-P5. Catalog recipe: detail over media

**Status.** The owner's call; no Android evidence. Effort S-M.

**Ships with.** A catalog page and a docs recipe: a still image pinned under a fixed top bar, an in-screen
non-modal `AppSheet` with a `Fraction` rest stop and a top stop below the bar (a `Custom` detent from the
bar's measured height, tested at font scale 2), `SheetModality.NonModal(upThrough = …)`, the call to action
in the sheet's `footer`.

**Text for `docs/PERFORMANCE.md`** (section "Sheets"):

~~~markdown
**Recipe: a panel over a picture.** A still hero image under a fixed top bar, with a non-modal `AppSheet`
resting at a fraction and stopping below the bar, costs one layer move per drag frame and one rounded
rectangle re-recorded while the sheet floats; nothing dims or recedes, and the idle page draws nothing.
Keep the hero still: a looping hero or a video in the template's screens would keep the page drawing at
120 Hz while idle and under every frame of the drag. See the catalog's "{page}".
~~~

### D-P6. Swiping between detail pages; a quicker page snap

**Status.** (a) the owner's call; (b) wait. Ours, measured: a pager landing t90 142 ms after a 150 ms flick
and 183 ms after a 300 ms flick, keeping 0.79 and 1.01 of the finger's speed (a 160 dp pager, not
full-bleed). (b) only if District lands clearly quicker under the same flick: `D` ≤ `O` − 0.05 s, a
side-by-side between `O` − 0.05 and `O` − 0.02 s, drop above.

**Ships with.** (a) A recipe: a full-bleed `HorizontalPager` with `AppScrollDefaults.pagerFlingBehavior`,
keyed pages composed from what the list already has, secondary sections loaded once
`settledPage == page`. (b) A separate paging token on `thumb` (0.28 s), since `screen` is shared with
carousels and the date picker.

**Text for `docs/PERFORMANCE.md`** (section "Lists and very large data"):

~~~markdown
**Recipe: swiping between detail pages.** Pages of a full-bleed pager are composed from what the list
already has; a page's secondary sections load once `pagerState.settledPage == page`, as a pushed screen's
wait for `LocalNavigationSettled`, so nothing composes during the snap. Pages move by translation, and
Compose prefetches the next page within frame deadlines while the finger drags.
~~~

### D-P7. A scroll-linked header helper

**Status.** The owner's call; evidence is iPhone only. Effort M. Risk: API growth; must coexist with the
bar minimizing and pull to refresh, which read the same scroll state.

**Ships with.** A design-system helper returning a header's collapse progress as `() -> Float`, built on
`LazyListState.scrolledFromTop()`, plus a catalog recipe.

**Text for `docs/PERFORMANCE.md`** (section "Motion and springs"):

~~~markdown
**Collapsing headers.** `{helper}(listState)` gives a header's collapse progress as a `() -> Float`. Read
it in layer and layout blocks only: icons scale and move in their own layers, labels fade with
`CompositingStrategy.ModulateAlpha`, a tab row appears as a `stickyHeader`. Nothing re-measures per scroll
frame, and text is never scaled (scaled text is re-rasterised on every frame).
~~~

### D-P8. Intros play once; choices survive an interruption

**Status.** Recommended: approve now; needs no measurement. Effort S.

**Ships with.** Docs rules, optionally a small "seen" helper on `AppSettingsStore` (a set of intro keys)
beside `onboardingCompleted`.

**Text for `docs/PERFORMANCE.md`** (section "Start-up", after "Background work starts when there is
work"):

~~~markdown
#### Intros play once

A feature intro plays once per install (or per version), can be skipped, and is shortened under reduce
motion. Its "seen" flag lives in the settings store in `:core:datastore`, beside `onboardingCompleted`,
which is read once at start-up and kept in memory, so deciding whether to play it reads no disk. Every
choice a multi-step flow collects is kept in `SavedStateHandle`, and the flow's restore test covers a
process death in the middle of it.
~~~

**Line for `AGENTS.md`.** "An intro plays once, its seen flag in the settings store; a flow's choices live
in `SavedStateHandle`. — *review*"

### D-P9. Catalog recipe: a snapping list with an emphasised centre card

**Status.** The owner's call, low priority; iPhone evidence only. Effort S-M.

**Text for `docs/PERFORMANCE.md`** (section "Lists and very large data"):

~~~markdown
**Recipe: emphasise the centred card.** A list that snaps with `rememberSnapFlingBehavior` scales and
fades each card by its distance from the viewport's centre, read in `graphicsLayer`: one layer property
change per visible card per frame, no re-measure. Cards never change height as the list scrolls; that
would re-measure and re-place the list on every frame.
~~~

### D-P10. Sheets that grow out of the control that opened them

**Status.** Wait; strictly conditional on District's Android recordings showing it (nothing public does).
Until then it stays deferred in SHEET-SPEC §20. Effort L.

**Ships with.** `SheetTransition.Zoom(source)` as SHEET-SPEC §20 specifies: rect and corner interpolation
of the surface, content fade with `ModulateAlpha`, the dim following the morph, a finger able to take over
mid-morph; no glow or beam.

**Text for `docs/PERFORMANCE.md`** (section "Sheets"):

~~~markdown
**Zoom.** `SheetTransition.Zoom(source)` grows a sheet out of its trigger: per frame one rounded rectangle
re-recorded, a fade without an offscreen buffer and the dim rectangle; no blur. Measured: sheet present
janky {n} %, opening frame {n} ms.
~~~

---

## The overlay's recomposition score, flags and signals (REQUIREMENTS 49; source `research/hud-metrics/DESIGN.md`, revision 2)

REQUIREMENTS 49 is the owner's (Oct 20). The design waits for the go to build it on `staging-wp/HUD`, and
three questions are the owner's to answer (below). The metrics' limits are a starting point and must be
calibrated (build step 9) before they ship.

### O-1. Dev tooling absent from the measured builds

**Status.** Required by REQUIREMENTS 49; waits for the build go. Today the measured builds
(`devBenchmarkRelease`, `devNonMinifiedRelease`) carry `:core:devtools` and `:core:perfhud`: the baseline
profile plugin copies `devRelease`'s `implementation` dependencies into them, and
`verify<Variant>CarriesNoDevTooling` skips the whole dev flavour (`DevToolingGuard.kt:32`). It is the first
suspect, not verified, for the Oct 19 back-gesture regression (a translucent DEV badge over every screen).
Proof: A/B arm 1 (the measured build with and without the tooling at rest).

**Ships with.** `debugImplementation` plus `"devReleaseRuntimeOnly"` for both modules, replacing
`"devImplementation"` (the plugin does not copy `runtimeOnly`; not `"devReleaseImplementation"`, which it
does copy). The guard registered for every variant but the debug build type and `devRelease`. `perf.py`
refuses `--gate` when the APK's dex holds the tooling's markers, unless `--allow-dev-tooling`, which never
gates. The baseline profile re-recorded on the new `devNonMinifiedRelease`.

**Text for `docs/PERFORMANCE.md`** (section "Builds that mean something", replacing the bullet about the
measured builds carrying dev tooling):

~~~markdown
- The measured builds carry no dev tooling: `devBenchmarkRelease` and `devNonMinifiedRelease` have no DEV
  badge, inspector or overlay, and `verify<Variant>CarriesNoDevTooling` checks them as it checks every
  shippable variant. Only `devDebug` and `devRelease` carry the tooling. `perf.sh --gate` refuses an APK
  that holds it. A dev-only dependency that must stay out of measured builds is declared with
  `"devReleaseRuntimeOnly"`: the baseline profile plugin copies a release build's `implementation`
  dependencies into the measured variants, but not `runtimeOnly` ones.
~~~

**Line for `AGENTS.md`** (replacing the dev tooling line). "Dev tooling reaches `:app` only as
`debugImplementation` and `"devReleaseRuntimeOnly"`. — `verify<Variant>CarriesNoDevTooling`"

### O-2. The recomposition score

**Status.** Pending the build go and question 2. Proof: A/B arms 2 and 3 (hidden costs nothing; shown
within REQUIREMENTS 40's limits, target at most 0.1 ms per frame), calibration step 9 (the template's
journeys grade A; a scroll offset read in composition and a `BoxWithConstraints` under an animated size
are caught and named).

**Ships with.** While recording only, the overlay installs a composition tracer that forwards every event
to Perfetto (so traces keep composable names) and puts `runtime-tracing`'s tracer back when recording
stops. It classifies each execution as an app restart, an app root, a list row entering or the list's own
window update, and credits them by time to the frame they ran in.

**Text for `docs/PERFORMANCE-OVERLAY.md`** (a new section "Recomposition", after "The score"):

~~~markdown
## Recomposition

The overlay watches every motion and counts the frames that rebuilt something, per screen and per state:
**Scrolling** (a finger is down, and the fling after it), **Animating** (motion without a finger),
**Transition** (the screen changed during the motion) and **Idle**. A motion is at least 4 frames whose
starts are at most 12 refresh intervals apart; its middle frames are the 2nd to the one before last.
List rows entering, a list's own window updates, and the first and last frame of a motion are not held
against the app.

| Finding | When | Colour |
|---|---|---|
| Per-frame recomposition | a motion with at least 4 middle frames, at least half of them with an app restart | red |
| Per-frame composition | at least half of the middle frames composed an app root (a `SubcomposeLayout` slot, a `BoxWithConstraints`, a scaffold body) with no list row entering | red |
| Recomposition mid-motion | middle frames with a restart, below the per-frame rule | amber past grade A |
| Periodic recomposition while idle | restarts in Idle at a steady period (at least 3, at least 1 s apart, within 25 %) | red if they draw frames, amber if not |

Score metrics: `RestartsMidMotion` (share of middle frames with an app restart; A/B/C/D at most
{1/3/10/25} %; weight 10; after 300 middle frames), `RestartsWhileIdle` (bursts per minute in Idle; at most
{2/6/12/30}; weight 5; after 60 s in Idle; a field heuristic, not a gate) and `JankyAfterFirst` (at most
{0.5/1.0/2/4} %; weight 10; after 300 such frames; comparable to the release gate's
`*.janky_after_first_pct`, not the gate's number). **Capture** records 10 s or 1200 frames after the panel
closes and lists the composables that rebuilt, by name and file:line, worst first.

The tracer has one slot: the overlay takes it only while recording, forwards to Perfetto, and gives it
back. Another tool that sets the composition tracer must do the same; if one replaces it, recomposition
metrics go ungraded and the report says `tracer = lost`.
~~~

### O-3. "Not optimised" flags and the extra signals

**Status.** Pending the build go and question 1. Limits for allocation per frame are calibrated in step 9
and unscored until then.

**Ships with.** One list of findings at the top of the Perf tab (also in the report's `findings` and one
`PerfHud` line per new finding), each with severity, value, limit and its source, a hint and a link here.
New signals: where late frames lost their time (main thread, RenderThread, GPU; API 31+), skipped vsyncs
during motion, process allocation per frame drawn, blocking GC while drawing, thermal status and headroom,
the previous exit reason (an ANR after this build's install is red), whether a full display time was
reported, and frames while the inspector is open excluded. Rejected: message logging, a second frame
metrics listener, a main-thread watchdog, more PSS reads, compiler stability reports in the overlay.

**Text for `docs/PERFORMANCE-OVERLAY.md`** (a new section "Findings", after "Reading it"):

~~~markdown
## Findings

The top of the Perf tab lists what is not optimised, red before amber, each with its value, the limit and
where the limit comes from, and what to look at. Besides the score's metrics:

- **Jank cause.** For each late frame, whether the main thread, the RenderThread or the GPU lost the time
  (Android 12 and later). Main thread: composition, layout or work in the frame. After it: overdraw,
  blur, offscreen layers, large bitmaps.
- **Allocation per frame drawn**, process-wide, sampled once a second; and a **blocking garbage
  collection** in a second with at least 30 frames drawn.
- **Thermal.** From `MODERATE` the numbers recorded are marked as recorded while throttled.
- **Previous exit.** An ANR after this build was installed is red.
- **Full display not reported.** The latest cold start has no `reportFullyDrawn`, so start-up can be judged
  only to the first frame.
~~~

### O-4. Questions only the owner can answer

1. **What the pill shows.** (a, recommended) FPS and janky % plus one coloured dot for the worst open
   finding; (b) a third line with the mid-motion restart share; (c) no change.
2. **Recomposition in the 0-100 score.** (a, recommended) yes: `RestartsMidMotion` (10) and
   `RestartsWhileIdle` (5) join the total, with a Recomposition card of its own; every existing score moves
   a little; (b) a separate Recomposition score only.
3. **Only if arm 1 or 2 shows the badge costs frames:** make the badge opaque at rest (recommended), or draw
   its alpha per operation (`ModulateAlpha`).

For information: with O-1, the measured builds lose the DEV badge; QA wanting it in a release-like build
uses `devRelease`.
