# Telegram for Android: how it keeps motion cheap, and what android-base can take from it

Written 2026-10-20 for REQUIREMENTS 48 by the motion and performance lead (Opus 5.5); revision 2 of the same day. Your
focus (Oct 19, REQUIREMENTS 48): **"Over the actual animations look for optimisation."** So this report leads with
*how* Telegram makes moving and scrolling cheap, with the Compose equivalent of each technique and what it would save
per frame on your Redmi Note 10 Pro Max at 120 Hz. How the motion looks (durations, curves) comes after.

Read-only research: no code, no worktree, no Gradle, no adb, no commit. **Every proposal waits for your yes, one at a
time**; approved ones land on `staging-wp/*` after staging (REQUIREMENTS 44). exteraGram is out, as you said at 23:14.

**Licence.** Telegram for Android is GPL-2.0 and android-base is MIT. This report takes ideas, techniques and measured
numbers only. It describes what Telegram's code does in my own words, cites file:line, and records parameter values as
facts. No Telegram code is copied here, and none may be copied into android-base. Every Compose sketch below is
written from Compose's own APIs.

**What revision 2 changed** (after an independent review): which Telegram motions use a saved picture and where its
data gate comes from (item 1 below, §2.4 rows 3-4); the chat-bar blur row (§2.6); the template's opt-in Lottie component,
which P1 now covers and which already breaks the template's own rule 7; the image proposal, rebuilt on WP06's existing
image loader; the hold proposal's real delay (about 0.35 s) and its release rule; the keyboard proposal, smaller now that
staging reads the keyboard in measure only; every template citation moved to `staging` at 4818a926; and the proposal
order (old P3 is now P5, old P4 is P3, old P5 is P4). The phone study's privacy steps and its sheet scene were fixed too.

**Inputs.** The verified source notes `SOURCES.md` (this folder; Telegram 13.0.2, commit f32ffee of 2026-10-13); the
template's motion work (`research/MOTION-PLAN.md`, `SHEET-SPEC.md`, `SMOOTHNESS-PLAN.md`, `back-google/DESIGN.md`,
`district/RESEARCH.md`); REQUIREMENTS 19, 21, 22, 24, 28, 29, 41, 42; the forbidden patterns of
`design/MASTER-PLAN.md` A11.3 (`:2025-2060`); the code at `staging` 4818a926 (which now holds `wp/WP03` 1dc5714c,
`wp/WP04` 6c6ee608 with the back card's texture, `wp/WP06` 08c0d0ac and `wp/HUD` 936fa2e8) and `wp/WP05-glass`
41203904 (not merged); and the phone numbers in `perf/WP04/final/summary.md` (release build, 3 runs, Redmi, Oct 19).

How sure each statement is: **[M]** measured on your Redmi (template only; Telegram has not been measured yet),
**[S]** read in source, **[D]** derived by arithmetic from [S] or [M], **[L]** likely (framework or library behaviour,
or documentation, not re-read here), **[U]** unverified until the phone study.

---

## Part 1. In plain English

### The short answer

1. **Telegram's secret is not its curves. While heavy content moves, it keeps that content from changing.** The
   screens and components that move heavy content take an "animation token": the chat (from the moment it is created
   until its opening slide ends), the profile and the topics list during their own transitions, the chats list during
   its search animation, sheets while they open, the keyboard helper, the tab pager, popup sub-menus and the predictive
   back gesture, about 45 classes in all. While any token is held, Telegram (a) holds back incoming data (new messages,
   updates) until the motion ends and (b) pauses animated stickers, GIFs and video. Sheets also pause those animations,
   without holding data back, while they close or snap back.
   Some motions also draw the moving surface from one saved picture of itself (a hardware layer): the finger-tracked
   swipe-back, a sheet opening, the photo viewer opening and the profile opening. A tapped push or pop does neither by
   itself: the page stack sets no layer, its page fade goes through an offscreen buffer on every frame, and its only
   gate is the one the arriving or leaving screen takes (the chat, the profile and the topics list do). Read from its
   source, keeping moving content unchanged is the main reason its transitions stay smooth even while a chat with
   thousands of messages loads behind them.
2. **It prepares before it moves.** A chat starts loading its messages before its screen is even built, the slide
   waits up to 0.2 s for the first messages so the page arrives finished, sheets wait until they are laid out, and the
   keyboard is handled with one layout followed by a pure slide.
3. **Its lists are drawn, not built.** Each message is one view that paints itself; its text is laid out on a
   background thread before it reaches the screen; images decode on a background queue; history is fetched 50 messages
   at a time when 25 rows remain, so a fling never hits the end.
4. **It quietly does less on phones like yours.** Telegram grades the phone from its CPU cores, clock and memory. By
   its own rule your Redmi is most likely "average" (not yet confirmed on the phone; its chip is not on Telegram's list
   of chips forced to "low"), and on that grade Telegram turns off its blur, wallpaper motion, spoiler particles and
   chat zoom effect by default. Part of what you feel as smoothness is Telegram not attempting its expensive effects
   on your phone.
5. **android-base already does most of the hard part, and in places better**: covered screens stay ready (so a pop
   or a back swipe starts instantly, 2.4 ms first frame measured), only the visible strip of the lower page is drawn,
   the back card is drawn from one saved picture, motions keep your finger's speed (Telegram ignores it), unopened tabs
   are built while idle, and an idle screen draws nothing.
6. **What android-base does not do yet is the "keep it unchanged" part.** A screen that is moving can still change
   while it moves: a skeleton keeps shimmering, a spinner keeps stepping, a Lottie animation keeps playing, and fresh
   data recomposes the screen mid-slide (the sample's push recomposes in 5 frames [M]). Under the back card that is
   worse than it sounds: the card's saved picture must be redrawn in full on every frame anything inside it changes.
   The first two proposals fix exactly that.

### What Telegram does to make motion cheap, and where android-base stands

| Telegram technique (plain words) | android-base today |
|---|---|
| The moving surface is drawn from a saved picture during a swipe-back, a sheet opening, the photo viewer opening and the profile opening; a tapped push or pop uses none | Only the back card does this (approved on Oct 19, REQUIREMENTS 42). Push, pop and sheets redraw the page each frame |
| New data is queued while a heavy screen, a sheet or a gesture is moving, then delivered in one go | Covered screens and hidden tabs pause their data; the screen that is arriving does not |
| Animated stickers, GIFs and videos pause while any of those motions runs | Skeletons and the activity indicator pause only in a hidden tab; pull to refresh's spinner and the optional Lottie component not even then; nothing pauses because its screen moves |
| A chat's open waits up to 0.2 s for its first messages, so it slides in finished | The slide starts one frame after the screen is first built; content swaps in during the slide |
| Keyboard: one layout, then only a slide | Sheets already do this. Screens with a bottom action bar measure again on every keyboard frame (no recomposition) |
| Only the visible strip of the lower page is drawn, under one dimming rectangle | **Already ours**, and more thorough (also during settles and around the back card) |
| Pages are kept alive; nothing is rebuilt on back | **Already ours**: three pages kept ready, so a pop's first frame is 2.4 ms [M]; Telegram re-attaches the page on swipe start |
| Lists: one self-painting view per row, text measured off the main thread, images decoded on a background queue, 50 rows fetched when 25 remain | Rows lay out in one pass and Compose prefetches rows in idle time; the image component subcomposes, which the template's own rule 6 forbids in lists; the feed fetches 10 rows when 3 remain |
| Decorative animations tick at 15 to 30 fps on one shared clock | **Already ours** in spirit: one shared shimmer clock that stops when idle; the spinner steps 8 times a second |
| Device grades switch effects off on slower phones | **Ours is better where it matters**: glass steps down from a measured GPU verdict, heat and battery saver |
| A hitch slows the push slide down instead of making it jump (each frame's time step is capped) | Not done; a hitch makes the slide jump ahead |

**Two template components already break the template's own rule 7** ("never an infinite animation that ignores
LocalTabVisible and reduce motion", MASTER-PLAN A11.3): the opt-in `AppLottie` loops forever by default and reads no
visibility, and pull to refresh's spinner keeps stepping in a hidden tab. And the template's only network image
component is built on the one API rule 6 forbids in lists. P1 and P3 fix these on the way.

### The proposals, ranked by how much smoothness they would add on your Redmi

| # | Proposal | What you would notice | Cost on the Redmi | Phone check |
|---|---|---|---|---|
| P1 | Hold decorative motion (shimmer, spinners, Lottie, image fades) still while its screen is moving | Back swipes over a loading screen stay smooth; slides draw less | none; a shimmer band disappears while its screen moves (about 0.36 s for a push) | yes |
| P2 | Hold new screen data until the slide has landed (one gate in the screen base) | No content popping in mid-slide; fewer recomposing frames per push (5 today) | none; content may appear up to about 0.35 s later | yes |
| P3 | List-cheap network images (no subcomposition; WP06's image loader given explicit limits) | Smoother scrolling through image lists in apps built on the template | less work per new row; a bounded image cache | yes |
| P4 | A push may wait (up to about 0.2 s) for its first content, and draw it once out of sight before sliding | Pages slide in finished; the first frames of the slide are lighter | a short wait on slow loads; one hidden frame of drawing | yes |
| P5 | Keyboard: one layout, then only a slide, for screens with a bottom action bar | Smoother keyboard on forms and composers | none; one measure fewer per keyboard frame | yes |
| P6 | Draw sheets and covers from one saved picture while they open and close (the back card's technique) | Lighter sheet and cover motion on heavy content | +4 ms on a first frame, about 6-10 MB of GPU memory while moving; needs your OK for a rule exception | yes, decides it |
| P7 | Fetch list pages further ahead | A fling never stops at "loading more" | no frame cost; a few more rows fetched | yes |
| P8 | A hitch slows a slide down instead of making it jump | Less visible stutter when the phone hiccups | none; a slide lasts as long as the hitch longer | yes |
| P9 | Docs rules for very large data | Apps built on the template stay smooth with big lists | none | no |
| P10 | Look and feel: Telegram's swipe back from anywhere on the page, on our springs | Swipe back like Telegram, not only from the edge | as the back card: one saved picture moved per frame | yes |
| P11 | Look and feel: an optional Telegram-style quick push (fade and short slide, 150 ms) | Snappier navigation for apps that want it | cheap only with P6's saved picture; needs a rule exception | yes |

### Left out, and why (details in §2.6)

Telegram also does things that would cost smoothness on your phone, or that android-base already does better: fading
whole pages through an offscreen buffer, a layout on every frame of the profile opening, a blur snapshot taken
synchronously on the main thread, live blur and particle effects (Telegram itself turns these off on your phone's
grade), a 60 fps background clock that never stops, settles that ignore your finger's speed, animations that step by a
fixed 16 ms per frame (and so run fast at 120 Hz), Lottie drawn at its source frame rate instead of the screen's,
lower-quality image decoding, and grading phones by CPU clock instead of measuring them.

---

## Part 2. Engineering detail

### 2.1 Paths

| Prefix | Path | Branch |
|---|---|---|
| `T/` | `research/telegram/src/telegram/TMessagesProj/src/main/java/org/telegram/` | Telegram 13.0.2 (f32ffee) |
| `ABL` / `BS` / `CA` / `NC` | `T/ui/ActionBar/ActionBarLayout.java` / `T/ui/ActionBar/BottomSheet.java` / `T/ui/ChatActivity.java` / `T/messenger/NotificationCenter.java` | Telegram |
| `ds/` | `templates/android/core/designsystem/src/main/kotlin/com/base/app/core/designsystem/` | `staging` 4818a926 |
| `nav/` | `templates/android/core/navigation/src/main/kotlin/com/base/app/core/navigation/` | `staging` 4818a926 |
| `ui/` | `templates/android/core/ui/src/main/kotlin/com/base/app/core/ui/` | `staging` 4818a926 |
| `app/` | `templates/android/app/` (`app/src/main/kotlin/com/base/app/BaseAppApplication.kt`) | `staging` 4818a926 |
| `feat/` | `templates/android/feature/` (`feat/sample/…/feature/sample/`, `feat/search/…`, `feat/profile/…`, `feat/auth/…`) | `staging` 4818a926 |
| `feed/` | `templates/android/data/feed/src/main/kotlin/com/base/app/data/feed/` | `staging` 4818a926 |
| `hud/` | `templates/android/core/perfhud/src/main/kotlin/com/base/app/core/perfhud/` | `staging` 4818a926 |
| `glass/` | `ds/foundation/glass/` | `wp/WP05-glass` 41203904 |
| `perf` | `perf/WP04/final/summary.md` (`com.example.wp04.everything.dev`, benchmarkRelease, 3 runs, before the back card's texture) | measured Oct 19 |

Line numbers moved between the package heads and staging (WP03's `foundation/Chrome.kt` is now
`foundation/layout/Chrome.kt`, `LocalTabVisible` is a `TabVisibility` holder rather than a Boolean, the feed repository
is now `NetworkFeedRepository`); every citation below was re-read at 4818a926.

### 2.2 What the evidence can and cannot carry

- **Nothing of Telegram has been measured yet.** Every Telegram statement is what its source does [S]; how it
  performs on your phone is the phone study's job (`PHONE-STUDY.md`). The template's numbers are [M].
- **Your phone's Telegram grade is derived, not read.** `SharedConfig.java:1625-1697` grades a device AVERAGE when the
  mean of its cores' maximum clocks is at or below 2055 MHz; the Snapdragon 732G's spec clocks (2 x 2.3 + 6 x 1.8 GHz)
  give a mean of about 1925 MHz [D], so AVERAGE, unless its `memoryClass` says LOW. Telegram's chip deny list, which
  forces LOW on SDK 31+, has 11 entries: Exynos 850, 7872, 7880 and 7870; MSM8953, 8937, 8940, 8992, 8952 and 8917; and
  SDM439 (`SharedConfig.java:360-372, 1640-1647`; recorded in `SOURCES.md` §4.1 because the download goes to the
  Recycle Bin after this research). SM7150, the 732G's family, is not on it. The custom ROM's sysfs clocks,
  `memoryClass` and `ro.soc.model` were not read [U]; the study reads them.
- **On AVERAGE, Telegram's defaults switch effects off** (`LiteMode.java:52-85, 202-210`; `SOURCES.md` §13a): no chat
  blur, no wallpaper motion, no spoiler particles, no chat zoom; translation-only parallax with no layers on the chat
  list (`DialogsActivity.java:12528-12671`); a first chat page of 15 messages (`CA:3131-3136`). Telegram's smoothness on
  your phone is partly the absence of those effects [D].
- **The download has no `res/` and no `jni/`**, so native rendering (rlottie, stack blur) and XML resources are
  unknown. One public report (bugs.telegram.org/c/59829) describes list stutter at 120 Hz in 12.5.0, worse on custom
  ROMs; your phone is both, so the study measures Telegram's own frame rate rather than assuming it.
- **Compose and library facts** used below: foundation 1.12.1 (the template's version) ships `LazyLayoutCacheWindow`
  and pausable composition in prefetch (`ComposeFoundationFlags.isPausableCompositionInPrefetchEnabled`), both read in
  the cached AAR's class list; the keyboard's `WindowInsetsRulers` are already used by the template's sheets
  (`ds/component/sheet/layout/SheetImeSource.kt:21-33`). Coil 3.6.3's `ImageLoader.Builder` has
  `decoderCoroutineContext` and `fetcherCoroutineContext`, and its Android builder has `bitmapFactoryMaxParallelism`
  (read in the cached AARs). Coil's own docs say subcomposition "is slower than regular composition" and
  `SubcomposeAsyncImage` may not suit "performance-critical parts of your UI", naming `LazyList`
  (coil-kt.github.io/coil/compose, fetched 2026-10-20). lottie-compose 6.7.1 is not in the local cache, so its
  `restartOnPlay` parameter is [L]. That HWUI renders a layer again whenever its display list or any child's is damaged
  is the template's own finding (`back-google/DESIGN.md` §1) [L].

### 2.3 The Redmi's budget today (template, measured)

| What | Value | Source |
|---|---|---|
| Frame budget at 120 Hz | 8.33 ms | — |
| Push/pop with the Back key: janky / janky after each motion's first frame / hitch | 1.66 % / 0.1 % / 3.04 ms/s | `perf` |
| Push/pop with the back gesture: janky / after the first / hitch (before the card's texture) | 2.53 % / 0.93 % / 16.39 ms/s | `perf` |
| Motions whose first frame missed its deadline | 16 of 16 (GPU still at its idle clock; an empty build misses too) | `perf`; `tools/perf/gates.toml` header |
| Push: the frame that composes the screen, median / max | 9.19 / 15.28 ms | `perf` |
| Push: frames that recompose, most in one push | **5** | `perf` (`recompose.push.runs` [5, 5, 5, 5, 5]) |
| Pop: first frame, median / max | 2.42 / 5.16 ms | `perf` |
| First three frames' GPU time, median / max | 11.02 / 21.47 ms | `perf` |
| Back card: RenderThread per gesture frame, replayed screen vs one texture | 2.24 → 1.50 ms; RenderThread hitches 38 → 2 in 36 gestures | `back-google/DESIGN.md` §2.1 (phase 17b) |
| Back card texture: first frame cost, memory | +4 ms GPU at the idle clock (10.8 → 14.9 ms); 10.4 MB | same |
| Idle 5 s on every tab | 0 frames | `perf` |
| PSS after the journey (graphics buffers included) | 109.7 MB | `perf` |

So after each motion's first frame the sample is already clean with the Back key (0.1 %); the gesture was the weaker
case before the texture landed. The sample is light; generated apps will not be. The proposals below target what
makes heavier screens hitch *inside* a motion (content changing while it moves, per-frame measuring, subcompositions),
and the back card's promise of one texture render per gesture.

### 2.4 Technique map: Telegram, its Compose equivalent, the saving, android-base's status

| # | Telegram (source) | Compose equivalent | Saving per frame on the Redmi | android-base |
|---|---|---|---|---|
| 1 | Pages kept as view trees; only the top attached at rest; the previous page re-attached (and built if null) at swipe start (`ABL:1306-1309, 1365-1379, 1768-1805`) | Covered entries kept composed, measured, placed and parked | the start frame of a pop or swipe composes nothing | **Ours, better**: three covered pages kept ready (`nav/stack/StackScene.kt:37-50`, parked at `:253-270`); pop first frame 2.42 ms [M] |
| 2 | Each page clipped to its visible strip while the finger tracks; one scrim rect over the page beneath (`ABL:1084-1217`) | `clipRect` strips + one `drawRect` scrim in the entry's draw | avoids drawing the covered part of the lower page (most of it early in a pop) | **Ours, better**: strips during slides and settles (`nav/stack/StackScene.kt:272-328`), four strips around the back card (`:358-384`), cached edge-shadow brush (`:315-323`) |
| 3 | Hardware layer on the top page for the length of a finger-tracked swipe-back only (`ABL:1404`, the only HARDWARE call in ABL; removed `:1354`); on sheets while they open (`BS:1735-1736, 1792-1793`); on the photo viewer's container (`PhotoViewer.java:17757, 17796-17838`); on the profile list (`ProfileActivity.java:9957`). A tapped push or pop sets none (`ABL:1873-1907`), and its fade goes offscreen because the page container reports overlapping rendering on SDK 28+ (`ABL:250-256`) | `graphicsLayer { compositingStrategy = Offscreen }` on the moving surface for the motion's length | one textured quad instead of replaying the surface: RenderThread 2.24 → 1.50 ms median, hitches 38 → 2 on the back card [M] | **Back card only** (`nav/stack/StackLayout.kt:185-187`, `nav/back/BackCard.kt:223-240`); sheets use `ModulateAlpha` and replay (`ds/component/sheet/layout/SheetLayers.kt:90-97`) → **P6** |
| 4 | Data notifications queued while any animation token is live, replayed in order at the end (`NC:463-614`). Tokens come from the screens and components that move heavy content: the chat from creation (`CA:2908, 27346-27405`; first-open messages allowed through), the profile (`ProfileActivity.java:9730-9735`), topics (`TopicsFragment.java:3990-3995`), the chats list's search (`DialogsActivity.java:7656`), sheets while they open (`BS:1781`), the keyboard helper, the pager, predictive back (`LaunchActivity.java:741, 780-783`); about 45 classes. The page stack itself locks only for theme animations (`ABL:642, 3038-3043`) | Hold state emissions in the screen's collector until the host settles, with a cap; input from the screen releases it | no recomposition or relayout of a whole screen inside slide frames | Partial: covered screens capped at STARTED/CREATED (`nav/stack/StackScene.kt:226-233`), tabs raised two frames late (`nav/shell/tabs/KeptTabs.kt:174-207`), `LocalNavigationSettled` for secondary loads (`nav/NavigationSettled.kt:6-25`, true at 99 % of the travel: `nav/stack/StackScene.kt:170`, `nav/back/BackGate.kt:108`). The arriving screen collects freely (`ui/MviScreen.kt:40`) → **P2** |
| 5 | Heavy animated drawables (Lottie, GIF, video) paused while any token is live (`NC:467-478, 530-535`; `ImageReceiver.java:1195-1233, 3159-3195`; sheets `BS:416, 1816-1818, 2093`) | Decorative loops read a "still" signal and stop while their entry moves | no per-frame redraw of moving content; under a texture, no full re-render per frame | Partial: bones and the activity indicator stop only in a hidden tab (`ds/foundation/draw/Skeleton.kt:196-207`, `ds/component/feedback/AppProgress.kt:52-63`); pull to refresh's spinner (`ds/component/refresh/AppPullToRefresh.kt:378-388`) and `AppLottie` (`ui/AppLottie.kt:53-94`) never stop → **P1** |
| 6 | The open slide waits until the new page reports its content, or 200 ms (`ABL:1920-1928, 2270-2282`; `CA:27141-27149, 20736, 21846-21853`); the chat's load starts before its view exists (`CA:2716, 3138, 3298-3330`) | Opt-in entry readiness: the stack keeps the arriving entry parked until ready or a cap, then slides | the skeleton-to-content swap leaves the slide | Not done: the slide starts after the composing frame whatever the content (`nav/stack/StackScene.kt:149-152, 160-182`); the sample swaps Idle → Loading → Success → related during the slide (`feat/sample/…/detail/SampleDetailViewModel.kt:26-53`) → **P4** |
| 7 | Keyboard: heights frozen at max(old, new) with one layout, then only translationY over 250 ms (`T/ui/ActionBar/AdjustPanLayoutHelper.java:89-125, 164-254`) | Read the IME in placement (`WindowInsetsRulers.Ime`), write the content inset once with the target | no measure per keyboard frame (about 34 frames at 120 Hz for a 285 ms IME animation [D]) | **Sheets: ours** (`ds/component/sheet/layout/SheetImeSource.kt:7-33`); the shell's bottom chrome follows the target once settled (`nav/shell/WindowChromeWriter.kt:21-58`, MOTION-PLAN M22). Screens with an action bar: measured per frame, no recomposition (`ds/component/container/AppBottomActionBar.kt:46`, `ds/component/container/AppScaffold.kt:84-86`, `ds/foundation/layout/Chrome.kt:156-191`) → **P5** |
| 8 | Sheets open only after two layout passes (fallback 150 ms) (`BS:1591-1618`) | Present when the sheet's frame is laid out | the first moving frame does not also lay out | **Ours** (`ds/component/sheet/SheetController.kt:229`: frame laid out, slots ready, entry settled) |
| 9 | Message cells are one view with no children, heights stored, text laid out in 10-line blocks on a background thread before binding (`ChatMessageCell.java:263, 13676-13697`; `MessageObject.java:1821-1822, 6833`; `MessagesController.java:12082-12087`) | Single-pass row layouts; ready-to-draw row models built off the main thread; long text split into lazy blocks | composing a new row costs less, so prefetch keeps up with a fling | Rows: **ours** (`ds/component/list/AppRow.kt:198-240`, one pass). Models and long text: docs → **P9** |
| 10 | Image decode on one priority queue, pausable; memory cache sized by memory class; 150 ms cross-fade (`DispatchQueuePriority.java:13-86`; `ImageLoader.java:129, 2075-2085`; `ImageReceiver.java:226`) | `AsyncImage` with painters (no subcomposition); the app's one `ImageLoader` with an explicit cache and decode parallelism | no subcomposition per image per row; bounded decode contention | Partial: an app loader exists under `<opt:coil&network>` (`app/…/BaseAppApplication.kt:135-150`, WP06), with Coil's default limits; the component is `SubcomposeAsyncImage` (`ui/AppNetworkImage.kt:41-48`) → **P3** |
| 11 | History fetched 50 at a time when 25 rows remain while scrolling (`CA:13886-13915`) | `PagingConfig(pageSize, prefetchDistance)` sized to a fling | none per frame; the list never runs dry mid-fling | 10 per page, 3 rows ahead (`feed/NetworkFeedRepository.kt:22-36`) → **P7** |
| 12 | Push/pop time step: a first gap up to 18 ms passes, 18-40 ms is capped at 18, over 40 ms becomes 0; every later step capped at 18 ms (`ABL:1822-1828`) | A `MonotonicFrameClock` wrapper that caps each step | a hitch no longer jumps the motion ahead | First step: **ours, approximately**. The slide's first animation frame is its start (`nav/stack/StackScene.kt:160-182`), so its first step is 0; the back card's commit (`FromRelease`, `nav/back/BackCard.kt:303-336`) counts a first gap of up to 50 ms as progress and drops a longer one. The per-step cap → **P8** |
| 13 | One shared Choreographer callback dispatches decorative ticks at 15-30 fps (`Choreographer60FpsContent.java:28-40, 231-300`), but never unregisters (`:228, 245`) | One clock per window for decorative motion, stopped when nothing uses it | fewer invalidations; no idle wake-ups | **Ours, better**: one shimmer sweep for all bones, stopped when the last leaves (`ds/foundation/draw/ShimmerClock.kt:12-76`); spinner 8 steps a second (`ds/component/feedback/AppProgress.kt:41-63, 142`); idle draws 0 frames [M] |
| 14 | Device class from cores, mean max clock and memory class; Lite Mode presets per class; the server can override (`SharedConfig.java:1625-1697`; `LiteMode.java:23-210`) | A measured tier for optional effects | heavy effects off where they would drop frames | **Ours, better for glass**: tier from a measured GPU verdict, heat, battery saver, low RAM (`glass/GlassPolicy.kt:17-35`, `glass/GpuVerdict.kt:15-49`). The template has few other heavy effects to tier (§2.6) |
| 15 | Long jumps animate two screens of translation, 300-1300 ms (`RecyclerAnimationScrollHelper.java:320-347`) | Jump near the target, glide the rest | composes two screens, not every row on the way | **Ours** (`ds/foundation/scroll/ScrollToTop.kt:6-17`) |
| 16 | Max refresh rate requested on some windows only, on the main window only after a configuration change (`LaunchActivity.java:7125`) | `preferredFrameRate(High)` while layer-only motion runs | keeps 120 Hz on adaptive-rate phones during motion | **Ours** (`nav/stack/StackLayout.kt:63-72`) |
| 17 | Frame metrics overlay on its own thread, off by default (`FrameMetricsOverlayView.java:170-224`) | Overlay that redraws only inside frames already drawn | the readout never schedules a frame | **Ours, better** (`hud/overlay/RedrawDuringFrames.kt:13-27`) |
| 18 | Unopened tabs and pages: built on first visit | Precompose in idle slices, draw once out of sight | first visit costs a switch | **Ours, Telegram has none**: `nav/shell/tabs/TabWarmUp.kt:19-56` |

### 2.5 The proposals in full

Owners follow REQUIREMENTS 44 (approved work goes to `staging-wp/<package>`). Effort: S ≤ half a day, M 1-2 days,
L 3 days or more for one package agent, tests and catalog included. "Redmi" is the Redmi Note 10 Pro Max (Snapdragon
732G, Adreno 618, 1080 x 2400, 8.33 ms frames). Renumbered in revision 2: P3 was P4, P4 was P5, P5 was P3.

#### P1. Hold decorative motion still while its screen is moving

- **Telegram.** The first animation token posts a global "stop heavy operations" (flag 512); image receivers stop
  heavy Lottie, GIF and video drawables and restart them when the last token ends (`NC:467-478, 530-535`;
  `ImageReceiver.java:1195-1233, 3159-3195`). Sheets post it on open, dismiss and snap-back (`BS:416, 1816-1818,
  2093`); the custom emoji window also pauses image decoding (`HwEmojis.java:34-105`).
- **Ours.** Each entry of a stack gets a `TabVisibility` holder that is true while its tab is in front and it is the
  stack's target top (`nav/stack/StackLayout.kt:127, 135`; the type is `ds/foundation/layout/Chrome.kt:53-69`). A
  skeleton bone joins the window's shimmer only while that is true, motion is allowed and it is not a preview
  (`ds/foundation/draw/Skeleton.kt:196-207`); the activity indicator steps only while it is true
  (`ds/component/feedback/AppProgress.kt:52-63`). Two clocks ignore it, which already breaks MASTER-PLAN A11.3 rule 7:
  - pull to refresh's spinner steps while it refreshes, even in a hidden tab
    (`ds/component/refresh/AppPullToRefresh.kt:378-388`, step 125 ms at `:484`);
  - `AppLottie`, the opt-in `<opt:lottie>` component (`templates/android/features/lottie.toml`, off by default),
    defaults to `iterations = LottieConstants.IterateForever` (`ui/AppLottie.kt:58`) and reads neither
    `LocalTabVisible` nor any motion signal (`:64-71`; it honours reduce motion only). Its one use in the template is a
    one-shot success check (`feat/auth/…/PasswordReset.kt:170-178`), but a feature that drops it in with the default
    loops for as long as its screen is composed, hidden or not. It is the closest thing the template has to what
    Telegram's flag 512 pauses.
  None of these knows that its screen is sliding, or is the back card's subject. The arriving page of a push is its
  stack's target top, so its bones sweep and its spinner steps on every slide frame. The back card's texture is
  "rendered once per gesture while the screen's own content is still" (`nav/back/BackCard.kt:223-240`): with a shimmer,
  a spinner or a Lottie inside, HWUI must render the whole 1080 x 2400 texture again on every frame one of them changes
  [L, `back-google/DESIGN.md` §1].
- **Change (Compose).** One "still" signal per entry, read only in observers, effects and draw, never in composition:
  - WP03: `TabVisibility` gains a second getter, `isStill` (default true): no slide, no back card and no presentation
    moves this content. A second getter rather than folding it into `isVisible`, because `isVisible` also means "on
    screen" to things that act on being seen (`nav/shell/tabs/KeptTabs.kt:46`), and `AppOtpField` reads `isVisible`
    in composition (`ds/component/formfields/AppOtpField.kt:150`), which would then recompose its caret twice per motion.
    `LocalEntryVisibility.isSettled` is not the vehicle: on staging only sheet hosts provide it
    (`ds/component/sheet/content/SheetHosts.kt:133-162`).
  - WP04: `StackLayout` builds each entry's holder with `still = { !gate.moving && !scene.card.active }`: the host's
    settled signal (`nav/stack/StackHost.kt:89`, true at 99 % of the travel) and the back card. It covers every entry
    of the host, as Telegram's gate covers the whole app. `scene.moving` would be later: it stays true until the
    spring is within 0.001 of rest, about 0.5 s into a push [D]. The presentation host does the same for a cover
    while it presents or dismisses.
  - WP03: sheet hosts forward the owner's `isStill` and add their own present, dismiss and snap-back
    (`ds/component/sheet/content/SheetHosts.kt:136`). `SkeletonNode.follow()` adds `isStill` to its `sweeps`
    condition (it already observes reads, so a change re-subscribes the node with no recomposition); the activity
    indicator waits for `isVisible && isStill` inside its existing `snapshotFlow`; the pull-to-refresh spinner reads
    `LocalTabVisible` (fixing the rule 7 breach) and waits for both inside its loop.
  - WP02-M3: `AppLottie` plays only while its tab is visible and its entry is still: a `derivedStateOf` over the two
    getters feeds `isPlaying`, so it flips only at the start and the end of a motion and costs one recomposition of the
    component at each edge, not one per frame. It needs `restartOnPlay = false` so a paused one-shot resumes rather than
    replays [L: confirm the parameter in lottie-compose 6.7.1].
  - Images (with P3): an image that finishes loading while its screen moves draws the result at once and skips its
    fade-in.
- **Owner.** WP03 (the getter, bones, indicator, pull to refresh, sheet hosts), WP04 (the stack and cover providers),
  WP02-M3 (`AppLottie`; images with P3).
- **Cost on the Redmi.** None added. Saves the bones', spinners' and Lottie's redraws on every slide frame, and under
  the back card a full-texture render per changed frame (a texture render measured +2-4 ms GPU on the card's commit
  frame [M, DESIGN §3.2]). Each edge of a motion costs one draw of each bone (plain) and one recomposition of each
  `AppLottie`. The look: a bone that leaves the clock draws its plain colour (`ds/foundation/draw/Skeleton.kt:212-215`),
  so the shimmer band disappears for the motion rather than freezing mid-sweep, and comes back with the next sweep when
  the motion ends (a sweep starts on the first frame after a bone joins an idle clock,
  `ds/foundation/draw/ShimmerClock.kt:69-71`). For a push that is about 0.36 s [D: 99 % of the 0.344 s critically
  damped spring]; for a back card, as long as you hold it.
- **Effort.** S-M. **Risk.** Low: a shimmer band missing during motion; a spinner that pauses for 0.36 s mid-push.
  Telegram accepts the same for stickers.
- **Evidence.** Telegram source [S]; template source and the texture contract [S]; the HWUI behaviour [L].
- **Phone check.** Yes: a back gesture over a screen that is still loading (a dev launch option that delays the sample
  detail's load, owner's run), counted with `research/back-google/sql/layerts.sql` (today expected more than one
  texture render per gesture; after, one) and `pushpop_gesture` rows.

#### P2. Hold new screen data until the slide has landed

- **Telegram.** While any animation token is live, a notification is delivered only if every live token allows its
  id; the rest queue and replay in order (`NC:463-487, 586-614, 662-666`). ChatActivity gates the global centre from
  creation until its open ends (`CA:2908, 27405`), lets the first page of messages through on the first open
  (`CA:27355-27383`), and force-releases after 800 ms (`CA:20638-20646`); tokens expire after 5017 ms (`NC:35`).
- **Ours.** Every screen collects its state in one place, `val state by viewModel.state.collectAsStateWithLifecycle()`
  (`ui/MviScreen.kt:40`), and hands its content `viewModel::onEvent` as the input lambda (`:57`). The arriving screen
  is RESUMED from its first frame (`nav/stack/StackScene.kt:226-233`), so each new state recomposes it inside the
  slide: a push recomposes in 5 frames [M], most likely the composing frame plus Idle → Loading → Success → related rows
  (`feat/sample/…/detail/SampleDetailViewModel.kt:26-53`) [D]. The host already knows when it has settled: the stack
  host provides `LocalNavigationSettled` (`nav/stack/StackHost.kt:89-92`), which turns true at 99 % of the travel
  (`nav/back/BackGate.kt:108`, `SETTLED_ENOUGH = 0.99f`, checked at `nav/stack/StackScene.kt:170`), about 0.36 s into a
  push [D].
- **Change (Compose).** In `MviScreen`:
  - Keep the lifecycle-aware collection exactly as it is (A11.3 rule 10): `val raw =
    viewModel.state.collectAsStateWithLifecycle()`, without reading `raw.value` in composition.
  - Show a state of its own: the first value at once; later ones, from a `LaunchedEffect` over
    `snapshotFlow { raw.value }` with `collectLatest`, only once `LocalNavigationSettled.current()` is true, with a cap of
    600 ms from the start of a hold (Telegram's forced release is 800 ms). Conflated, so the landing frame recomposes
    once with the latest state.
  - Release at once on input, and only on input: the hold ends when the content calls the `onEvent` lambda `MviScreen`
    passes it, which `MviScreen` wraps to mark the release before forwarding. Events a route sends straight to the
    ViewModel never release it: every sample route sends `Appeared` from a `LaunchedEffect` on its first composition
    (`feat/sample/…/detail/SampleDetailRoute.kt:18`, `feat/sample/…/list/SampleListRoute.kt:16`,
    `feat/search/…/SearchScreen.kt:60`), and releasing on those would release the hold on frame 1.
  - Outside a host the local is always settled, so previews and tests behave as today. `core/ui` already depends on
    `core:navigation`.
- **Owner.** WP02-M3 (`core/ui`), with WP04's existing local.
- **Cost on the Redmi.** None added. Moves up to four of the five recomposing frames of a push out of the slide, into
  the frame where the page is about 11 px from rest [D: 1 % of 1080 px]. Data that arrives early in the slide shows at
  the landing, up to about 0.35 s later than today [D]; the cap bounds any stall at 0.6 s. The back card's follow phase
  is not held (the local is true while the card follows the finger); a state change there costs one texture render,
  not one per frame.
- **Effort.** S. **Risk.** A screen that expects instant state during its arrival (an inline error, a counter) shows it
  at the landing; the sample renders Idle and Loading alike (`feat/sample/…/detail/SampleDetailScreen.kt:59-75`), so
  its skeleton shows during the slide as today. The hold must not touch effects (navigation stays immediate:
  `MviScreen.kt:46-53` collects them separately).
- **Evidence.** Telegram source [S]; `perf` recompose rows [M].
- **Phone check.** Yes: `perf.sh recompose pushpop` (push recomposition frames 5 → at most 2 inside the slide), and a
  heavier detail screen to show the hitch it removes.

#### P3. List-cheap network images

- **Telegram.** Images are drawn by an `ImageReceiver` inside the cell, decoded on a single-thread priority queue that
  can be paused (`DispatchQueuePriority.java:13-86`; `ImageLoader.java:129`), from a memory cache sized by memory class
  (`ImageLoader.java:2075-2085`), with a 150 ms cross-fade (`ImageReceiver.java:226`). A finished load only invalidates
  the cell's drawing.
- **Ours.**
  - The component. `AppNetworkImage` is `SubcomposeAsyncImage` with a composable skeleton for loading and a composable
    glyph for errors (`ui/AppNetworkImage.kt:41-48`): one subcomposition per image, and the subcomposed content swaps
    when the load finishes, mid-scroll. MASTER-PLAN A11.3 rule 6 already says "never SubcomposeAsyncImage in lists",
    so the template's only network image component is one its own rule forbids in a list. Today it is used once, for
    the profile avatar (`feat/profile/…/ProfileScreen.kt:97`), so the scrolling gain is for apps built on the template.
  - The loader. WP06 (merged into staging at cf5ac425) already installs the app's one `ImageLoader`: under
    `<opt:coil&network>`, `SingletonImageLoader.setSafe` builds it with an `OkHttpNetworkFetcherFactory` on the app's
    own client, so images share its connections and certificate pins (`app/…/BaseAppApplication.kt:135-150`). It sets
    no memory-cache bound, decode parallelism or cross-fade, so those are Coil's defaults. The `coil` feature is on by
    default and does not require `network` (`templates/android/features/coil.toml`), so a build with Coil and without
    the network module has no app loader at all.
- **Change (Compose).**
  - WP02-M3: `AsyncImage` (or `rememberAsyncImagePainter` + `Image`) with `placeholder` and `error` painters; the
    skeleton look while loading drawn by the image's own draw from the painter state (a bone drawn by a
    `DrawModifierNode` that reads the state, joining the shimmer clock as bones do), so a finished load changes the
    drawing only.
  - WP06: extend the existing `setSafe` builder, never add a second loader: an explicit memory-cache bound, hardware
    bitmaps kept on, `bitmapFactoryMaxParallelism` set from a measurement on the Redmi (2 big + 6 little cores), and a
    short cross-fade only outside lists. A second route (an Application-level `SingletonImageLoader.Factory`) would
    conflict with `setSafe`, and if it ever replaced WP06's loader, image requests would skip the pinned client. For
    builds with Coil and without `network`, a separate `<opt:coil&!network>` block (the marker grammar has `&` and `!`,
    `generator/genkit/core/markers.py:18`) builds the same limits without the OkHttp fetcher. Exactly one loader per
    build.
  - Rule 6 already exists; the docs only need to point features at the painter-based component.
- **Owner.** WP06 (the loader), WP02-M3 (`AppNetworkImage`), WP03 (the bone hook).
- **Cost on the Redmi.** Removes one subcomposition per image row composed and per finished load during scrolling
  (size not measured [U]); a bounded cache (memory).
- **Effort.** S-M. **Risk.** `AsyncImage` takes painters, not composables, so the error glyph becomes a painter;
  `AppRedacted` keeps its skeleton path; the decode cap trades CPU contention against how fast images fill in; the
  loader change must keep the pinned client (a test that the singleton's fetcher uses the app's `OkHttpClient`).
- **Evidence.** Coil documentation (fetched) and the 3.6.3 builder (read); Telegram source [S]; template source [S].
- **Phone check.** Yes: a catalog list of 200 image rows flung at 80/150/300 ms swipes: janky %, GPU p90 and the
  Perfetto `Compose:prefetch`/compose slices per row, before and after.

#### P4. A push may wait for its first content, and draw it once out of sight

- **Telegram.** A fragment that asks for it delays its open slide until it calls back or 200 ms pass
  (`ABL:1920-1928, 2270-2282`); ChatActivity asks while its first messages load and releases on their arrival
  (`CA:27141-27149, 20736, 21846-21853`), having started the load before its view is built (`CA:2716, 3138`).
- **Ours.** The slide starts the frame after the screen first composes, whatever the screen shows
  (`nav/stack/StackScene.kt:149-152, 160-182`); content then swaps in during the slide (P2's 5 frames). SMOOTHNESS-PLAN
  S3.2 plans to seed screens from an in-memory cache (only for cached data). `TabWarmUp` already draws a tab once under
  the one in front to pay its first draws out of sight (`nav/shell/tabs/TabWarmUp.kt:50-55`). `UiState` is a bare
  marker with no notion of loading (`templates/android/core/mvi/…/core/mvi/UiContracts.kt:8`).
- **Change (Compose).**
  - Opt-in per destination (navigation metadata, as `NavTransitionStyle` is, `nav/NavTransitionStyle.kt:3-10`): the
    arriving entry stays parked off the window, where `apply` already puts it (`cover = 0f`), until it reports ready,
    or until a cap (Telegram 200 ms; 250 ms when the keyboard is up). Then, as `TabWarmUp` does, it is drawn for one
    frame under the current page, and the slide starts. A destination that does not opt in behaves as today.
  - A small opt-in readiness contract in `core/mvi`: a state may implement an interface with one property ("my first
    content is here"); `MviScreen` reports ready the first time the **ViewModel's raw state** says so, and at once for a
    state that does not implement it.
  - Readiness must never read P2's held state, and the wait must not count as moving: the gate reports moving only
    from the slide's first frame. Otherwise P2 would hold the very state that signals readiness, and every opted-in
    push would wait the full cap. With the wait outside the motion, P2 holds nothing during it, the screen composes its
    content off the window, and P2 holds only what arrives once the slide runs.
- **Owner.** WP04 (the gate in `StackScene.apply`, the holder), WP02-M3 (the readiness contract in `core/mvi` and
  `MviScreen`; the feature template).
- **Cost on the Redmi.** Slide frames compose nothing new; the arriving page's first draw (glyph and texture uploads)
  moves into a hidden frame before the slide, which may help the 16-of-16 late first frames [U]. Latency: up to the cap
  on uncached loads, zero when data is in memory.
- **Effort.** M. **Risk.** Tap-to-motion latency (press feedback must cover it); Back during the wait must cancel the
  push cleanly (`BackGate`); rapid pushes queue.
- **Evidence.** Telegram source [S]; template source and `perf` [S][M].
- **Phone check.** Yes: tap-to-first-moving-frame, `recompose.push`, `pushpop.first3_gpu_ms`, on the sample with a
  delayed repository (owner's run).

#### P5. Keyboard: one layout, then only a slide

- **Telegram.** A pre-draw listener catches the window's one-step resize, sets the heights from the content down to
  max(old, new), lays out once, then animates only the parent's translationY over 250 ms on the chat curve, and lays
  out once more at the end (`T/ui/ActionBar/AdjustPanLayoutHelper.java:89-125, 164-254, 42-43`). The IME-insets path
  is off in release (`:39`).
- **Ours.** MOTION-PLAN M22 is PARTIAL: the shell reads the keyboard in layout and writes the window's bottom chrome
  from `imeAnimationTarget` once the screens have settled (`nav/shell/WindowChromeWriter.kt:21-58`); sheets read it in
  placement only (`ds/component/sheet/layout/SheetImeSource.kt:7-33`); "a rule for screens" is listed as missing. A
  full-screen destination with a bottom action bar does not follow either: the bar pads itself by the keyboard's inset
  (`ds/component/container/AppBottomActionBar.kt:46`), so its height changes on every keyboard frame; `AppScaffold`
  measures it and writes that height into the content's bottom inset during measure
  (`ds/component/container/AppScaffold.kt:84-86`); and `chromeContentPadding()` reads it when the list measures its
  padding (`ds/foundation/layout/Chrome.kt:156-191`). Staging already moved that read out of composition, so every
  keyboard frame measures the bar, the scaffold and the list again but recomposes nothing [S; the cost is not measured].
- **Change (Compose).** Outside a sheet only: the bar pads by the navigation bar and rides the keyboard in placement,
  through a layout modifier that reads `WindowInsetsRulers.Ime` (as `RulerImeSource` does) and places the bar `-ime` px
  higher, so the keyboard moves it with no measure. The content's bottom inset is written once per keyboard change with
  the animation's target (opening: the target at once; closing: kept until the animation ends, Telegram's
  max(old, new)), so the list lays out once at each end. Inside a sheet the bar is the sheet's footer, and the sheet
  already keeps it above the keyboard (`ds/component/container/AppBottomActionBar.kt:29-31`, `LocalSheetSurface`): a
  ruler lift there would lift it a second time, so that path stays as it is. M22's "rule for screens" becomes a docs line
  (owner WP02-M3, per M22).
- **Owner.** WP03 (bar and scaffold), WP02-M3 (the screens' rule).
- **Cost on the Redmi.** None added. Removes one measure pass of the bar, the scaffold and the list per keyboard frame,
  about 34 frames each way [D].
- **Effort.** S-M. **Risk.** On opening, a list scrolled to its end shifts once in the first frame instead of gliding
  with the keyboard (Telegram avoids this by translating the whole content as well, the alternative if it shows); on
  closing, blank space stays under the list until the keyboard is gone (Telegram's choice too). The bar must track the
  IME exactly (the ruler is the one the sheets use). API 26-29, where the inset jumps, keeps today's behaviour.
- **Evidence.** Telegram source [S]; template source and MOTION-PLAN M22 [S].
- **Phone check.** Yes: open and close the keyboard on a catalog screen with `AppBottomActionBar` and a list, with
  Perfetto measure slices and gfxinfo frame stats, before and after; the same scene on Telegram (`PHONE-STUDY.md` K1)
  for the side-by-side.

#### P6. Draw sheets and covers from one saved picture while they open and close

- **Telegram.** Sheets set a hardware layer on their container for the open and remove it at the end (`BS:1735-1736,
  1792-1793`); the attach sheet holds one for its spring (`ChatAttachAlert.java:5345-5440`); the photo viewer for its
  200 ms open (`PhotoViewer.java:17757, 17796-17838`); the profile open on its list (`ProfileActivity.java:9957`).
  A tapped push or pop sets none (`ABL:1873-1907`): only the finger-tracked swipe does (`ABL:1404`).
- **Ours.** The back card is the one approved exception to A11.3 rule 1 (`nav/back/BackCard.kt:223-240`,
  REQUIREMENTS 42). A sheet's layer fades with `ModulateAlpha` and replays its content each frame
  (`ds/component/sheet/layout/SheetLayers.kt:90-97`); covers slide by `NavDisplay`'s transition
  (`nav/transition/NavTransitions.kt:53-63`).
- **Change (Compose).** An innermost `graphicsLayer { compositingStrategy = if (textured) Offscreen else Auto }` on the
  surface, true only during a self-driven present or dismiss of a sheet or cover whose content is still (P1 and P2
  first), false the moment a finger takes it, and never for glass sheets that sample their backdrop. For push, an
  experiment only: the arriving page textured during the slide, rendered on the composing frame while it is still off
  the window; never for pop, whose first frame is 2.42 ms today and would gain a full render. Each case named in the
  A11.3 exception text, with a layer test like `SlideRecordingTest`.
- **Owner.** WP03 (sheets), WP04 (covers, the push experiment).
- **Cost on the Redmi.** Per moving frame, one quad instead of the surface's display list (the card measured
  RenderThread 2.24 → 1.50 ms, hitches 38 → 2 [M]); the first frame +4 ms GPU at the idle clock [M]; GPU memory while
  moving, 10.4 MB for a full screen, proportionally less for a partial sheet [D]. Any change inside the surface
  re-renders the whole texture, which is why P1 and P2 come first.
- **Effort.** M. **Risk.** Needs your OK: it extends the rule exception you granted for the back card only. A later
  first frame on opens; text drawn from a texture at fractional offsets.
- **Evidence.** Telegram source [S]; the back card's measurements [M].
- **Phone check.** Yes, and it decides: the T ladder method of `back-google/DESIGN.md` §5 on a sheet journey
  (`layerts.sql` renders per open, `presentlat.sql`, RenderThread per frame) and `pushpop` for the push experiment.

#### P7. Fetch list pages further ahead

- **Telegram.** While scrolling, the chat loads 50 more messages when 25 rows remain (5 when not scrolling)
  (`CA:13886-13915`).
- **Ours.** The feed fetches 10 per page and asks for the next page 3 rows before the end
  (`feed/NetworkFeedRepository.kt:22-36`), by design "without fetching pages nobody scrolls to". Our iOS fling keeps
  0.998 of its speed per ms (`ds/theme/AppMotion.kt:188-194`), so it coasts about v / 2.0 per second: a 6000 px/s fling
  travels about 3000 px [D], 10 to 20 rows of a card feed, far past 3 rows. When it reaches the end before the next page
  arrives, the fling stops at the footer's spinner.
- **Change.** A docs rule and the feed's values: `prefetchDistance` at least the rows a hard fling coasts plus one
  network round trip of scrolling (about two screens of rows), `pageSize` near 30, `initialLoadSize` two pages.
- **Owner.** WP06 (data layer), WP12 (the rule).
- **Cost on the Redmi.** No frame cost; a few more rows fetched and kept.
- **Effort.** S. **Risk.** More requests for people who stop early; a choice the current comment made on purpose.
- **Evidence.** Telegram source [S]; template source and decay maths [S][D].
- **Phone check.** Yes: 80 ms flings down the feed; count flings that end at the footer.

#### P8. A hitch slows a slide down instead of making it jump

- **Telegram.** Its push/pop loop takes wall-clock steps; a first step over 40 ms becomes 0, and every step, the first
  included, is capped at 18 ms (`ABL:1822-1828`): a dropped frame slows the motion rather than skipping it.
- **Ours.** The first step is roughly covered already, by different rules: the slide's first animation frame is its
  start, so its first step is 0 (`nav/stack/StackScene.kt:160-182`); the back card's commit is timed from the release
  and counts a first gap of up to 50 ms as progress, dropping a longer one (`FromRelease`, `MAX_LEAD_NANOS`,
  `nav/back/BackCard.kt:303-336`). After that, a 50 ms stall makes the next frame show the slide 50 ms further on: at
  the spring's peak speed (about 7000 px/s for a 1080 px slide [D]) that is a jump of about 350 px.
- **Change (Compose).** A `MonotonicFrameClock` wrapper, like the existing `FromRelease`, that caps each step at two
  vsync periods (16.7 ms at 120 Hz), used by self-driven moves only: the slide, sheet present and dismiss, the back
  card's commit. Finger-tracked motion stays on the finger.
- **Owner.** WP04 (slide, card), WP03 (sheets).
- **Cost on the Redmi.** None; a motion lasts longer by the hitch.
- **Effort.** S. **Risk.** A very long stall reads as a pause; test clocks pass through as `FromRelease`'s do.
- **Evidence.** Telegram source [S]; whether people prefer the slowdown is [U].
- **Phone check.** Yes: a dev-only stall injected mid-slide, recorded with and without the cap.

#### P9. Docs rules for very large data

- **Telegram.** Message objects and their text layouts are built on a worker thread before the UI sees them
  (`MessagesController.java:12082-12087, 12168`); text is laid out in blocks of 10 lines (`MessageObject.java:1821-1822`);
  bulk list updates run without the item animator and coalesce within 36 ms (`DialogsActivity.java:471-489`).
- **Ours.** `AppRow` measures in one pass (`ds/component/list/AppRow.kt:198-240`); `AppPagingList` animates every row
  change (`ui/paging/AppPagingList.kt:82-85`); SMOOTHNESS-PLAN S11 covers `key`, `contentType` and a measured cache
  window; A11.3 rules 6 and 13 already cover subcomposed images and keys.
- **Change.** Rules in `AGENTS.md` and `docs/`: map rows to ready-to-draw UI models off the main thread (formatted
  strings, `AnnotatedString`s, dates); split long text into lazy items by paragraph; no subcomposition in rows
  (`BoxWithConstraints` as well as the images rule 6 names); a refresh that replaces the whole list swaps without item
  animations; P1's still signal for any looping decoration; P7's prefetch sizing.
- **Owner.** WP12. **Cost.** None. **Effort.** S. **Risk.** None. **Phone check.** No.

#### P10. Look and feel: swipe back from anywhere on the page

- **Telegram.** The swipe can start anywhere: dx at least 0.4 cm (by the screen's xdpi) and dx/3 > |dy|, with no
  horizontally scrollable child under the finger (`ABL:1412-1446`; `AndroidUtilities.java:2902-2904`). The page follows
  1:1; it commits past a third of the width or on a fling of 3500 px/s mostly sideways (`ABL:1471-1489`). Settles take
  max(200 ms x rest/width, 50 ms) to commit and max(320 ms x x/width, 120 ms) to cancel on AccelerateDecelerate, with
  no velocity (`ABL:1597-1669`). The top page is on a hardware layer for the drag (`ABL:1404`).
- **Ours.** The system edge gesture drives the Gmail back card (`nav/back/BackCard.kt`, REQUIREMENTS 24); a full-page
  swipe is MOTION-PLAN M3, missing and off by default.
- **Change (Compose).** An optional, nested-scroll-aware horizontal drag on each stack entry that loses to every
  horizontal scroller and draggable control under the finger; it drives the slide's `cover` 1:1 with the same strips and
  scrim, draws the top page from one texture for the drag (the back card's technique), and settles on the `screen`
  spring seeded with the finger's speed (better than Telegram's tween). Telegram's recognition numbers as the starting
  values (0.4 cm is about 62 px, 23 dp on your phone [D, xdpi not read]); the commit rule is yours to pick: Telegram's
  (a third of the width, 3500 px/s) or iOS's (half, 1 width per second).
- **Owner.** WP04. **Cost on the Redmi.** As the back card per frame (one textured quad, strips, scrim; RenderThread
  about 1.5 ms [M]); 10.4 MB of GPU memory while dragging; the first drag frame +4 ms GPU at the idle clock [M].
- **Effort.** M-L. **Risk.** Gesture conflicts (carousels, sliders, swipe actions, maps, text selection), RTL, and two
  back gestures to keep consistent; the texture extends the back card's rule exception to this gesture.
- **Evidence.** Telegram source [S]. **Phone check.** Yes (feel, and `PHONE-STUDY.md` T3 for Telegram's own numbers).

#### P11. Look and feel: an optional Telegram-style quick push

- **Telegram.** Push and pop take 150 ms on DecelerateInterpolator(1.5) (1 − (1 − t)³): the new page fades from 0 to 1
  and slides from 48 dp to 0; on close the leaving page fades out and slides 48 dp away; the page beneath never moves
  (`ABL:614, 1830, 1873-1907`). Its container reports overlapping rendering on SDK 28+, so the fade goes through an
  offscreen buffer every frame (`ABL:250-256`) [L].
- **Ours.** Push and None (`nav/NavTransitionStyle.kt:3-10`); push rides the 0.344 s critically damped spring with a
  0.333 parallax (`nav/transition/NavTransitions.kt:25-45`), about 0.36 s to 99 % [D].
- **Change (Compose).** A third, opt-in style. Done cheaply only with P6's texture: the arriving page rendered once,
  then faded and moved as one quad. Without it, the fade costs an offscreen pass of the whole page on every frame,
  or shows overlapping draws through each other with `ModulateAlpha`. Its duration and curve become a theme motion token
  (`AppTheme.motion`), never a raw tween (`templates/android/AGENTS.md:218-219`; A11.3 rule 11).
- **Owner.** WP04. **Cost on the Redmi.** About 18 frames instead of about 44 [D]; per frame the full page beneath plus
  one faded quad (two full-screen fills); +4 ms on the first frame for the texture.
- **Effort.** M. **Risk.** It fades a whole screen, which A11.3 rule 1 forbids, so it needs the same kind of written
  exception as P6 and the back card, and your OK for it. A second style to test with the back card and Rapid Back; it
  departs from the iOS default (CP1), so it is opt-in only.
- **Evidence.** Telegram source [S]. **Phone check.** Yes (side by side with `PHONE-STUDY.md` T1/T2).

### 2.6 Left out, and why

| Telegram does | Why not, on the Redmi or under the template's rules |
|---|---|
| Fades whole pages through an offscreen buffer on a tapped push or pop (`ABL:250-256, 1876-1907`) | An offscreen pass of a full screen every frame (A11.3 rule 1); only as P11 with a texture and an exception |
| Lays out every frame of the profile opening (`ProfileActivity.java:9872, 8627-8693`) | A full measure per frame; the template moves layers |
| Takes the long-press blur snapshot synchronously on the UI thread at 1/15 scale (`AndroidUtilities.java:6032-6100`) | A long main-thread frame on open. The template's menu dims without blur (`ds/component/overlay/AppContextMenu.kt:73-123`); if a blur is ever added: once, off the UI thread, at 1/8 scale or less |
| Live chat-bar blur: the node is re-recorded whenever the content beneath is invalidated in the chat and the chats list (every frame while the chat scrolls), and on every draw in the base class (`SizeNotifierFrameLayout.java:916-930, 1022-1025`; `CA:17375-17377`; `DialogsActivity.java:1129-1131`) | A full-width re-render per scrolling frame; Telegram itself turns it off on your phone's grade. Glass has its own budget and tiers (`glass/`) |
| Particles, spoilers, wallpaper motion (`ThanosEffect.java:1112-1123`; `SpoilerEffect.java:113-133`) | Heavy per frame; off on AVERAGE in Telegram; not template defaults |
| One 60 fps callback that re-posts every vsync forever (`Choreographer60FpsContent.java:228, 245`) | Wakes the main thread every vsync; ours stops, idle draws 0 frames [M] |
| Settles that ignore the finger's speed (`ABL:1606-1630`) and tweens without velocity on retarget (`AnimatedFloat.java:149-187`) | "Never a tween after a finger" (MOTION-PLAN rule 2); `Animatable` keeps velocity |
| Animations stepped by a fixed 16 ms per draw (`ImageReceiver.java:1842-1852`; `DialogCell.java:4923-4952`) | Runs about 1.5x fast at 90 Hz and 2x at 120 Hz [D] |
| Lottie rendered to bitmaps on worker threads at its source frame rate, halved when limited (`RLottieDrawable.java:132-133, 1399-1406`) | Compose draws a composable on the main thread only, and `AppLottie` (opt-in, `ui/AppLottie.kt:53-94`) interpolates its keyframes at the screen's rate; capping it at 30 fps would make it visibly coarser at 120 Hz (REQUIREMENTS 21: make effects cheap, never remove them). P1 stops it while hidden or moving instead |
| Emoji picker rows drawn into bitmaps on background threads (`DrawingInBackgroundThreadDrawable.java:20, 98`) | No emoji picker in the template; Compose cannot draw composables off the main thread |
| A 2 fps text caret (`EditTextBoldCursor.java:1148`) | For information only: `AppOtpField`'s caret fades on every frame while it has focus (`ds/component/formfields/AppOtpField.kt:148-167`), about 120 frames a second on an otherwise still screen. A blink would save them but removes the fade, which REQUIREMENTS 21 rules out; it already stops when hidden or with reduced motion |
| RGB_565 decoding below memory class 192 (`ImageLoader.java:2077, 1302`) | Banding; nothing in the template asks Coil for RGB_565 (no `RGB_565` or `allowRgb565` on staging) |
| A CPU-clock device grade with presets the server can override (`SharedConfig.java:1625-1697`; `LiteMode.java:176-200`) | Grades by spec, not by measurement; the template has few heavy effects, and glass already steps down on a measured verdict |
| Drops to 60 Hz under load (`RefreshRateController.java:48-62`, commented out in Telegram) | You judge at 120 Hz |
| Blocks all touches during a transition (`ABL:1039-1042`) | The template counts Back presses instead (BackGate); blocking feels unresponsive |
| A vendored RecyclerView with a bigger recycled pool (`RecyclerView.java:5655`) | Compose's lazy layouts reuse compositions by `contentType`; nothing to port |

### 2.7 Look and feel: Telegram's parameters beside ours (facts, not proposals)

| Motion | Telegram (source) | android-base |
|---|---|---|
| Push / pop | 150 ms, Decelerate(1.5), fade + 48 dp; page beneath still; no layer (`ABL:614, 1830, 1873-1907`) | 0.344 s critically damped spring, parallax 0.333, scrim 0.10 light / 0.24 dark, edge shadow 0.04 (`nav/transition/NavTransitions.kt:25-45`; `nav/stack/StackScene.kt:391-399`) |
| Swipe back (anywhere) | 0.4 cm, dx/3 > \|dy\|; commit past w/3 or 3500 px/s; commit max(200 ms·rest/w, 50), cancel max(320 ms·x/w, 120), AccelerateDecelerate, no velocity; scrim up to 96/255 = 0.38 (`ABL:1412-1519, 1597-1669, 1204-1211`) | none (P10); the edge gesture drives the back card |
| Predictive back | starts at progress 0.015; scale toward 0.90, then 0.85 as alpha drops; pivot 82 dp from the far edge at the touch Y; no slide during the gesture; commit max(200 ms·d/w, 380), cancel 320 ms, EASE_OUT_QUINT (`LaunchActivity.java:738-851`; `ABL:1119-1133, 1526-1630`) | Gmail's card: 0.9 scale, 8 dp gap, 24 dp Y follow, 28 dp corners, scrim 0.32, commit fade 100 ms, velocity-seeded spring (`nav/back/BackCard.kt:48-61`) |
| Sheet | waits for layout; open 400 ms EASE_OUT_QUINT after 20 ms, dim 51/255; dismiss 250 ms EASE_OUT (button 180); snap-back only when released under 0.8 cm (by ydpi) or flung up, over 250 ms·ty/0.8 cm DEFAULT (`BS:213-214, 382-418, 1591-1619, 1728-1819, 2065-2066`); attach sheet spring damping 0.75, stiffness 350 (`ChatAttachAlert.java:5345-5440`) | `sheet` spring 0.34 s, velocity-seeded detents, dim following the finger (District P1/P2 already weigh sheet tempo) |
| Popup menu | background scale and alpha plus a 4-item wave, 150 + 16·n ms; dismiss 150 ms (`ActionBarPopupWindow.java:843-913, 1024-1091`) | `dialog` spring 0.275 s |
| Keyboard | 250 ms on the chat curve (0.19919, 0.01064, 0.27921, 0.91025) (`AdjustPanLayoutHelper.java:42-43`) | follows the system's IME animation frame by frame |
| List changes | moves 250 ms on the chat curve, adds/removes 120 ms, enter scale from 0.9 (`ChatListItemAnimator.java:46-47, 1495-1512`) | `layout` spring placement, fade in 200 / out 120 ms (`ds/animation/AppTransitions.kt:143-152`) |
| Tab pager settle | from velocity, 150-600 ms, quintic ease-out; tab tap 540 ms (`ViewPagerFixed.java:760-774, 285-292`) | kept tabs with a 12 dp nudge on `thumb` (`nav/shell/tabs/TabReveal.kt:27-74`) |
| Image fade | 150 ms (`ImageReceiver.java:226`) | none set (Coil default) |

### 2.8 What the phone study settles

`PHONE-STUDY.md` runs once you have installed Telegram and signed in. Every Telegram number is compared with the
template doing the same thing on the same phone, measured the same way.

| Measurement | Decides |
|---|---|
| Telegram's grade inputs (cores, clocks, memory class, SoC) and the Power Saving switches as shown | whether Telegram runs with its effects off on your phone (§2.2) |
| T1/T2 push and pop: duration, curve, opening frame, frame stats | P11's tempo, side by side |
| T3 swipe back: recognition, follow, settle times, carry of speed | P10's thresholds |
| T4 predictive back | for information (your Gmail look stands) |
| L1/L2 list flings and the channel's history loads: janky %, GPU p90, hitch, refresh interval | how Telegram's lists compare with ours at 120 Hz (bug 59829), P3/P7 context |
| K1 keyboard: frames and curve | P5's side-by-side |
| PR1 profile open | for information (Telegram's per-frame layout, §2.6) |
| S1-S4 one sheet (if you name one) | sheet tempo beside District P1/P2 and P6 |
| Template-only checks in each proposal | adopt, change or drop P1-P8 (run by the owning package, with builds) |

### 2.9 Sources

`SOURCES.md` (this folder) holds every Telegram claim with its line numbers and the verifier's corrections, now with
Telegram's chip deny list and where its animation tokens come from (§3.1, §4.1); the inventories
`gather-transitions.md`, `gather-lists-perf.md` and `gather-web.md` are the raw notes behind it. Template evidence is
cited by file and line at `staging` 4818a926 (glass at `wp/WP05-glass` 41203904); phone numbers are from
`perf/WP04/final/summary.md` and `research/back-google/DESIGN.md`; the forbidden patterns are
`design/MASTER-PLAN.md:2025-2060`. Web: coil-kt.github.io/coil/compose and /image_loaders (fetched 2026-10-20),
bugs.telegram.org/c/59829, news.ycombinator.com/item?id=41148996, developer.android.com compose-animation release notes
(via `SOURCES.md` §14-15). Nothing from Telegram was built, run or copied.

The phone study's two helpers are in `tools/` here: `tg_target.py` (tap points for a messenger, allow-listed and
refusing anything that changes a setting or a draft) and `tg_fit.py` (the District fitter with Telegram's curves added).
Both were checked on synthetic dumps and tracks only.
