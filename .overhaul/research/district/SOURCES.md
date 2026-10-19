# District by Zomato: verified sources

Verifier: Opus 5.5, 2026-10-19 (Windows, read-only). It checks the three Sonnet lookups in this folder
(`gather-engineering.md`, `gather-design.md`, `gather-technique.md`) and adds frame-by-frame checks of public videos.

**How the checks were done.** Every claim that matters was re-opened at its source: store pages and blogs with `curl`,
GitHub source and API files raw, developer.android.com pages through `curl` and text search, and JS-rendered pages
(Play reviews, Toptal, Medium) in the built-in browser. Public YouTube videos were watched in the browser pane:
paused at a time, high resolution forced, single frames drawn to a canvas and screenshotted. Nothing was downloaded
to disk. No APK was fetched; APKPure and AppBrain were refused by the browser and `curl` and were not retried.
Nothing was decompiled. No gradle, adb, worktree, commit or push.

**Labels.**
- **confirmed**: I read it at the source myself.
- **likely**: the source supports it with a caveat, or it rests on two independent weaker signals.
- **unverified**: I could not open the source, or the claim is inference.

Motion described from videos is **observation of what the screen does**, never a statement about District's code.

---

## 0. Bottom line

**The most likely UI stack is native Android in Kotlin, with Jetpack Compose likely in use and Android Views
probably mixed in.** This is inference (likely, not confirmed): no public source names District Android's toolkit.
The evidence (section 2):
- Eternal ships an official Compose Multiplatform design system, Sushi Compose. It says the system is in production
  at Zomato on Android.
- Zomato's Android engineering manager (2023 to now) says he led Zomato's Compose adoption.
- District launched in Nov 2024 as a new app, inside the Zomato package namespace.
- Against this: Eternal's Restaurant Partner app is still Views and Fragments (Dec 2025). Nothing public ties Sushi
  Compose to District.

Nothing public rules Flutter or React Native in or out. Only the user's own device can settle it (section 10).

**What District's motion looks like.**
- **Public breakdowns are iOS only.** All 12 written breakdowns (60fps.design) come from the iOS app and give no
  numbers (section 4).
- **Android frames, from my own viewing.** The only Android footage I found is a Sep 2026 UX review. Frame by frame,
  it shows two things (section 5):
  - A horizontal swipe between movie-detail pages that lands in about 0.2 s. The new page shows its poster and grey
    skeleton bars first, then its text and trailer.
  - A content panel with a grab handle that rises over the auto-playing trailer.
- **No durations, easings or spring values for District exist publicly.** The only numbers in this file are
  Google's (androidx, MDC) and Zomato's open-source Sushi.

---

## 1. App identity and store facts

| Fact | Status | Source and why |
|---|---|---|
| Play listing "District: Movies Events Dining", package `com.application.zomato.district`; rating value 4.661996841430664 (shown 4.7), ratingCount 195459 in the page's JSON-LD; "1.95L reviews"; 10M+ downloads; updated 15 Oct 2026; "#6 top free lifestyle" | confirmed | https://play.google.com/store/apps/details?id=com.application.zomato.district&hl=en_IN&gl=IN (fetched 2026-10-19; JSON-LD author "Zomato", url district.in) |
| What's new: Gift Cards, Comedy tab, Salons tab | confirmed | same page |
| Android version number, APK size and minimum Android version are not in the public Play HTML | confirmed (absent) | same page: no "Requires Android" or size strings. Mirror info pages (APKPure, AppBrain) were refused by the network or the browser, so these values stay unknown |
| App Store: version 2.77.0, 307.6 MB, iOS 16.0 or later, seller Zomato Media Pvt. Ltd., rating 4.8 from 267,076 ratings; notes mention Siri and "Been here" | confirmed | https://apps.apple.com/in/app/district-movies-events-dining/id6670536058 |
| App Store "No. 1 Lifestyle" chart position | unverified | not found in the fetched HTML |
| District grew out of Zomato LIVE; acquired an entertainment ticketing business in Aug 2024 | confirmed | https://eternal.com/our-businesses/district |
| That business was Paytm's movie and events ticketing (Rs 2,048 crore, about 280 staff) | likely | widely reported. The Week page returned no matching text to `curl`, so it was not re-read |
| Rolled out on Android and iOS in mid-Nov 2024 | confirmed (date likely 15 Nov) | Inc42 article dated 16 Nov 2024 (https://inc42.com/buzz/zomato-rolls-out-district-app-for-android-iphone-users/); OfficeChai 16 Nov 2024 |
| Google Play Best App of 2025 in India (post 18 Nov 2025). Reasons given: social utility and AI-driven hyper-local recommendations. Nothing on design or motion | confirmed | https://blog.google/intl/en-in/products/platforms/google-play-best-apps-and-games-of-2025/ |

## 2. UI toolkit: the evidence, weighed

| # | Evidence | Status | Source and why |
|---|---|---|---|
| T1 | No public source states District Android's toolkit (Compose, Views, Flutter, React Native) | confirmed (absence) | My own searches: the exact package id, District plus Compose, Zomato Compose or server-driven UI, crash traces naming the packages. Eternal's blog has 16 posts and none is about District Android |
| T2 | Sushi Compose is an official Eternal Compose Multiplatform design system. Its blog file (added to the repo 2025-07-29) calls it "Production Proven", says it powers Zomato, and says it is "currently being used on Android". It is "stable and production-ready for Android" | confirmed | https://github.com/Zomato/compose-sushi/blob/dev/blog/SUSHI_DESIGN_SYSTEM_BLOG.md (commit date from the GitHub API) |
| T3 | The same blog never names District. Its only mention of "all ETERNAL apps" is an example of apps that scale to millions of users. It does not say Sushi Compose is used in every Eternal app | confirmed | same file, the line about scaling from MVP to millions. The Sonnet reading "meant to scale across all Eternal apps" over-reads it |
| T4 | Tarun Goyal, Engineering Manager at Zomato (2023 to present), says on his Toptal resume that he led the introduction and adoption of Jetpack Compose, plus screenshot testing and modularisation. His portfolio links the main Zomato app (`com.application.zomato`), not District | confirmed (self-reported) | https://www.toptal.com/developers/resume/tarun-goyal (read in the browser) |
| T5 | A candidate's "Zomato(District) Android Interview Experience" (Medium, 23 Nov 2025, member-only) says the interview questions centred on Compose because the candidate said he used it. This shows the interviewers know Compose, not what District uses | confirmed (weak signal) | https://medium.com/@p.thapar99/zomato-district-android-interview-experience-dbda616ca4ea |
| T6 | Eternal's Restaurant Partner app is Android Views and Fragments (SlidingPaneLayout, EmbeddedActivity, SavedStateHandle) and does not mention Compose (1 Dec 2025). So Eternal's Android apps are not all Compose | confirmed | https://www.eternal.com/blog/beyond-the-phone-re-architecting-zomato-restaurant-partner-app-for-large-screens |
| T7 | Eternal's blog has two Android engineering posts, Restaurant Partner (T6) and Pulse. Pulse is an MQTT library that runs in the Delivery Partner, Consumer and Hyperpure apps; District is not named | confirmed | https://www.eternal.com/blogs ; https://www.eternal.com/blog/pulse (23 Jun 2026). The Sonnet "only one Android post" was wrong |
| T8 | Server-driven UI: Zomato's (not District's) iOS team presented a Snippet Handler architecture with SwiftUI components at iOS Ketchup 2.0. District's talk there was about Live Activities | confirmed | https://www.eternal.com/blog/ios-ketchup-2-where-intention-becomes-interaction-4a1534dd (12 Sep 2025) |
| T9 | District iOS uses SwiftUI for its Live Activities | confirmed | https://www.eternal.com/blog/live-activities-push-to-start-dynamic-images (13 Mar 2026) |
| T10 | A LinkedIn post says District hires Android developers for Kotlin and "modern Android architecture" | unverified | LinkedIn returned HTTP 999; known only from a search summary |
| T11 | Android Developers case study (page last updated 2022-01-21) on the main Zomato app: Perfetto, App Startup, about 30% faster startup, 60% fewer out-of-memory issues. It does not mention Compose or District | confirmed | https://developer.android.com/stories/apps/zomato |
| T12 | The District designer who built its design system (Jun 2024 to Jul 2025, self-reported) points to District-specific foundations. Whether its Android components are Sushi Compose is unknown | confirmed (self-reported) / link to Sushi unverified | https://www.anmolarora.me/experience |

**Verdict.** Native Kotlin is the safe assumption, and Compose is likely used somewhere in District Android: T2, T4 and
T5, plus a greenfield 2024 app at a company adopting Compose. The share of Compose versus Views, and whether Sushi
Compose or a server-driven snippet layer renders the screens, are **unverified**. None of District's visible motion
(sections 4 and 5) needs any particular toolkit; Compose or Views could produce all of it.

## 3. Sushi design system (Zomato's own open source, read from source)

These facts are about the open-source kit, which is not proven to be what District ships.

| Fact | Status | Source |
|---|---|---|
| compose-sushi: Apache-2.0; created 2025-03-13; last push 2026-10-16; default branch dev; 12 stars; 528 commits on dev; `sushi/version.properties` VERSION_NAME 0.6.2 (stamped 7 Sep 2026) | confirmed | https://api.github.com/repos/Zomato/compose-sushi ; raw `sushi/version.properties` |
| Catalog: Kotlin 2.4.10, Compose Multiplatform 1.11.1, AGP 9.3.1, coroutines 1.11.0, Coil 3.5.0, lottie-compose 6.7.1 | confirmed | raw `gradle/libs.versions.toml` (dev) |
| `SushiBottomSheet` is a thin wrapper over Material 3 `ModalBottomSheet`: top corners 28 dp, container surface.primary, content text.primary, tonal elevation 0, default drag handle, insets, max width and `rememberModalBottomSheetState()`. No animation spec, scrim or nested-scroll code of its own, so its sheet motion is stock M3 | confirmed | raw `components/bottomsheet/SushiBottomSheet.kt` |
| `Modifier.scaleOnPress` is a pointer and draw `Modifier.Node`. Press target 0.95, animated with androidx `spring()` (imported from androidx, not Sushi's helper). Any Move, Scroll, Release or cancel un-presses it. Scale is drawn only while pressed, so the release snaps back without visible animation. No haptics | confirmed | raw `modifiers/scaleonpress/ScaleOnPress.kt` |
| Sushi `spring()` helper: physical damping 77, stiffness 1500, mass 1, giving a damping ratio of 77 / (2·√1500) ≈ 0.994. Invalid inputs fall back to defaults | confirmed (ratio computed) | raw `animation/Spring.kt` |
| Shimmer is a draw `Modifier.Node`. Auto mode loops `infiniteRepeatable(tween(1000), Restart)`, so easing is tween's default (FastOutSlowIn), not linear. `Auto.duration` is not passed into the tween. The band is a `linearGradient` at 10°. Fade in and out is `tween(300)` | confirmed | raw `modifiers/shimmer/Shimmer.kt`. Corrects the Sonnet "linear" |
| `SushiViewFlipper`: `AnimatedContent` vertical slide, interval 3000 ms, animation 600 ms. The outgoing slide runs 0-300 ms; the incoming one is delayed 300 ms and runs 300-600 ms, so they play in sequence, not together | confirmed | raw `components/viewflipper/SushiViewFlipper.kt`, `SushiViewFlipperDefaults.kt` |
| Older Views kit `sushi-ui-android` (Apache-2.0, created 2022-01-28, last push 2022-03-01, 111 stars): press animator scales to 0.8 in 100 ms; text-button alpha goes to 0.45 in 100 ms; `AnimationConstants` (in `organisms/stacks`, 2019) uses 350 ms with Bounce, Overshoot, Decelerate and FastOutSlowIn interpolators | confirmed | raw `res/animator/*.xml`, `organisms/stacks/AnimationConstants.kt` |
| Zomato app (not District) moved from video and GIF to Lottie JSON and dotLottie. Most files are 70-80 KB, all under 100 KB; dotLottie saved 60-70%; Zomato built a dedicated animation team. This is vendor marketing | confirmed (vendor claim) | https://lottiefiles.com/case-studies/zomato |

## 4. District motion, third-party catalogue (iOS recordings)

60fps.design lists 12 District shots under the heading "District iOS App" (https://60fps.design/apps/district). No
team is named, and no duration, easing or spring value appears anywhere. Each shot page carries its own `uploadDate`,
so the Sonnet note that the dates were a site-wide stamp is wrong. All descriptions below are confirmed as what the
pages say, paraphrased. They describe iOS behaviour as 60fps worded it.

| Shot (upload date) | What the page describes |
|---|---|
| Ask AI intro sheet (2026-02-02, "on iOS") | A bottom sheet with an AI search field; a query types itself. The input morphs into a pulsing purple star while text behind fades. The star shrinks and fades, then two result cards slide up and fade in |
| Best Seats intro (2026-06-20, "From District") | A tap fires a blue beam up into a dark modal with a glowing border. Wireframe scenes morph in sequence (seats, sound, screen, combined) with captions in step. Close slides the modal down and fades it |
| Icon grid to tabs (2026-02-02, iOS) | The home scroll is scrubbed: a 3x2 grid of large 3D-style icons shrinks into flat icons in a sticky tab bar, labels fade, and the search bar slides out |
| Weekend pick scroll snap (2026-10-08, iOS) | In a vertical list, each card's height, opacity and image follow its distance from centre. The centred card is expanded; the others become dim compact rows; the list snaps |
| Spider-Man interstitial (2026-08-10, iOS) | A 3D character hangs over the showtimes with idle breathing and sway |
| No-internet state (2026-07-26, iOS) | A looping flashlight sweep; text and button are static |
| Something-went-wrong state (2026-04-07, "From District") | A car drives in, decelerates with a slight springy tilt, then puffs smoke; text static |
| Play card icon (2026-02-02, iOS) | An idle paddle tilt with a squash-and-stretch ball, plus a NEW badge |
| Play intro header (2026-02-02, iOS) | A pill grows into a looping step-by-step banner, then shrinks back |
| Login carousel (2026-01-18, iOS) | Three glowing pins orbit in 3D; the front one scales up; the label crossfades |
| 2025 banner (2024-12-27, "From District") | Title wiggle with shimmer, a bobbing bubble and a rotating wheel |
| Logo splash (2024-11-17, iOS) | Neon lines draw the wordmark, hold, then fade into the app. 60fps also posted it on X on 2024-11-18 (76 likes at fetch) |

Corroboration from a second source: the "Tech with cash" review (8 Aug 2026, iPhone screen recording) shows the
home's 3D icon grid at 2:45 and the compact tab bar at 5:45, which matches the grid-to-tabs shot. This is an
observation: I saw the start and end states, not the morph.

## 5. District motion, my own frame-by-frame observations

**aspira design, "District App UX Review" (24 Sep 2026, 7:33).** https://www.youtube.com/watch?v=l4VzORzIHiQ

The creator's chapters are confirmed: 0:06, 0:28, 1:07 Movies like an OTT platform, 1:47 "Blinkit-style browsing
interaction", 2:09 CTA problem, 2:53 Events, 3:23 onward category overload, 6:28 verdict. The phone recording is
**Android**: the three-button navigation bar is visible. It is shown as an inset in a phone frame the editor added,
at 1080p. Inset segments sit at about 1:10-1:26, 1:39-1:41, 2:11-2:24, 2:35-2:52, 3:00-3:09 and 3:28-3:36.

Caveats on timings: I stepped through at 33 ms intervals, the video's own frame rate caps precision, and the editor
may have retimed the clip.

- **O1, swipe between movie-detail pages (1:38.9-1:39.4).**
  - The narration says the user can keep browsing by swiping.
  - The "I'm Game" page starts moving left at about 98.93 s and the "Immortal" page has landed by about 99.13 s. That
    is **about 0.2 s on screen**, with large early steps and small late ones (decelerating).
  - Both pages move together and stay edge to edge. I saw no scale, parallax or gap.
  - The incoming page shows its poster at once, with grey skeleton bars where the text goes. The trailer replaces the
    poster about 0.1 s after landing, and the text fills in about 0.25-0.3 s after landing.
- **O2, content panel rising over the trailer (2:45.3-2:45.8).**
  - On the "Toxic" detail page, a dark rounded panel with a small grab handle sits over the auto-playing trailer.
  - Over about 0.5 s the panel rises until it meets the top bar (back chevron, language pill, share). The trailer
    stays in place and is covered rather than scrolled away.
  - The top bar and a floating bottom bar ("Releasing … / Book tickets", rounded, over the content) stay fixed.
  - I could not tell whether the panel was finger-driven or settling, or whether the trailer is dimmed (its own
    frames change at the same time).
- **O3, layout seen.** Movies lists ("Trending in District" with large rank numerals, "Only in Theatres", an "Upcoming
  movies" grid with filter chips) and an Events "Explore by" grid with 3D icons. No bottom sheet appears in this
  video.

**Other videos.**
- Business Today, "Tech Today" (25 Nov 2024): chapters confirmed (1:27 User Interface, 1:53 Sharing Your Seat Map,
  2:08, 3:04, 3:35). Frames from 1:26-2:12 show handheld **iPhones** (Dynamic Island visible) at 720p, with no
  motion detail resolvable. https://www.youtube.com/watch?v=E1jzDqRYVbk
- AK Online Support (24 Dec 2025): an iPhone screen recording in a mockup (App Store, login, seat map, payment).
  Not studied frame by frame.
- Tech with cash (8 Aug 2026): an iPhone recording in a mockup. Used only for the grid and tab states above.
- TechBro (4 Oct 2025): a camera filming an Android phone. Too small for motion.
- Not opened: Gyan Mind, DEKH DUNIYA (both 2 Dec 2024), Techie Bon (28 Dec 2024), Saral Guide (26 May 2026). Their
  titles, dates and lengths are confirmed from the watch pages.

## 6. What users say (Play reviews, Android users)

I read about 150 reviews in the Play dialog ("most relevant", 2026-10-19). Everything is paraphrased; all items are
confirmed as written by the reviewers.
- **Praise for feel:**
  - "incredibly snappy" interface (3 Jul 2026).
  - No lag while picking seats, fast checkout (25 Jul).
  - No lag, and the app "does not sit heavy" on the phone (21 Jul).
  - Glitch-free (24 Jul).
  - Sleek UI (4 Sep).
  - "magnetic interface" (3 Sep).
  - Clean, modern, smooth (29 Aug).
  - "Beautiful UI lot of fun elements" (28 Aug).
  - Caution: a few glowing reviews repeat word for word on different dates (24 May and 9 Aug), so some praise may not
    be organic.
- **Criticism of motion:**
  - Since the Best Seats update an animation plays every time the app opens and takes a few seconds; the reviewer
    wants it shown once or twice (12 Sep 2026).
  - The Best Seats pop-up appears every time a theatre opens; wanted once per install (11 Sep 2026).
- **Criticism of state:**
  - After an interruption such as a phone call, the app returns to the booking page with the date changed; seat and
    F&B choices were kept (2 Sep 2026).
  - Laggy and glitchy booking (19 Jul).
  - "slow at times" (18 Jul).
  - Server extremely slow at booking time (15 Sep).
  - Over-designed, cluttered ticket details (4 Apr).
  - The Sonnet's "1 May: slow" is dropped: that review says booking is smooth "most of the time".
- **Press:** OfficeChai (16 Nov 2024) praises the purple theme, the "zany" font, a dark theme that is easy on the
  eyes, an uncluttered layout, better design than BookMyShow, "slick and well-made". It says nothing about animation.
  Confirmed.
- **Peerlist "Zomato's" UX article** (Ishika Jagatramka): about the Zomato food app; District appears only as a
  bottom-nav cross-promotion. Confirmed; do not cite it for District.

## 7. Platform technique references (numbers re-read at the source)

These are the numbers a proposal would rely on. androidx files were read at `androidx-main` on 2026-10-19 and may be
ahead of stable releases.

**Anchored drag and Animatable**
- `AnchoredDraggableDefaults`: `SnapAnimationSpec = tween()`, `PositionalThreshold = distance / 2`,
  `DecayAnimationSpec = exponentialDecay()`, minimum fling velocity 125 dp/s. **Confirmed** (AnchoredDraggable.kt).
- `AnchoredDraggableState.animateTo(target, spec)` takes no velocity and uses `lastVelocity`. **Confirmed**.
  - `lastVelocity` is updated on every `dragTo`, including animation frames. A completed animation therefore leaves
    about zero and an interrupted one keeps the velocity it had. **Likely** (inferred from the code path; not tested).
- `Animatable.animateTo(..., initialVelocity = velocity)`: the default continues from the current velocity.
  **Confirmed** (Animatable.kt).
- The customize page says springs keep velocity continuous when the target changes mid-animation and handle
  interruption better than tween. **Confirmed** (https://developer.android.com/develop/ui/compose/animation/customize).

**Spring constants and M3 motion tokens**
- `Spring` constants:
  - Stiffness: High 10 000, Medium 1500, MediumLow 400, Low 200, VeryLow 50.
  - Damping: HighBouncy 0.2, MediumBouncy 0.5, LowBouncy 0.75, NoBouncy 1.
  - **Confirmed** (VectorizedAnimationSpec.kt). There is no StiffnessMediumHigh.
- M3 motion tokens (damping / stiffness), **confirmed** (ExpressiveMotionTokens.kt, StandardMotionTokens.kt):

  | Scheme | Fast | Default | Slow |
  |---|---|---|---|
  | Expressive spatial | 0.6 / 800 | 0.8 / 380 | 0.8 / 200 |
  | Standard spatial | 0.9 / 1400 | 0.9 / 700 | 0.9 / 300 |
  | Effects (both schemes) | 1.0 / 3800 | 1.0 / 1600 | 1.0 / 800 |

**M3 sheet**
- Modal and standard sheets use `defaultSpatialSpec` for show and for the fling settle, and `fastEffectsSpec` for
  hide. **Confirmed** (BottomSheet.kt, BottomSheetScaffold.kt).
- `SheetState`'s default `BottomSheetAnimationSpec` is `tween(300, FastOutSlowInEasing)`. A code comment in the
  nested-scroll pre-fling path speaks of "tween settling". **Confirmed** (SheetDefaults.kt).
- Modal-sheet fling: velocity is clamped to the system maximum.
  - A **downward** fling within 125 dp of the Hidden anchor is scaled by (distance / 125 dp)², but never below
    125 dp/s if it started above that. A hard downward fling cannot overshoot the floor yet still dismisses.
  - **Confirmed** (BottomSheet.kt, SheetDefaults.kt: PositionalThreshold 56 dp, VelocityThreshold 125 dp,
    BoundaryDampeningZone 125 dp). The Sonnet "hard upward flick" was wrong.
- Overshoot above the top anchor stretches the surface in Y (pivot top) and counter-scales the content.
  **Confirmed** (`verticalScaleUp` and `verticalScaleDown`).
- Scrim: theme `scrim` colour at opacity 0.32. Alpha animates by **state** (`targetValue != Hidden`) with the
  DefaultEffects spring, not from the live offset. **Confirmed** (ModalBottomSheet.kt, ScrimTokens.kt).
- Nested scroll (`ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection`), **confirmed** (SheetDefaults.kt):
  - pre-scroll: the sheet takes upward user drag first;
  - post-scroll: the leftover goes to the sheet;
  - pre-fling: an upward fling below the top anchor runs the sheet fling and consumes all of it;
  - post-fling: always flings the sheet with the remainder.
- Predictive back on the sheet: scale down by up to 48 dp in X and 24 dp in Y, with progress through
  `PredictiveBack.transform`. **Confirmed** (BottomSheet.kt).

**Release history (material3 notes)**

All **confirmed** with dates (https://developer.android.com/jetpack/androidx/releases/compose-material3).

| Version | Date | Change |
|---|---|---|
| 1.3.0-alpha02 | 2024-03-06 | Sheet spec changed from spring to TweenSpec to fix a visible overshoot |
| 1.3.0-alpha06 | 2024-05-01 | Predictive back curve (0.1, 0.1, 0, 1) |
| 1.4.0-alpha15 | 2025-05-20 | Sheet uses fast-effect to hide or collapse and default-spatial to expand |
| 1.5.0-alpha14 | 2026-02-11 | `sheetPeekHeight` 0 disables the partial anchor; `targetValue` prefers the current anchor |
| 1.5.0-alpha15 | 2026-02-25 | Parameter to disable the sheet back handler; motion scheme graduated from experimental |
| 1.5.0-alpha16 | 2026-03-25 | Sheets respect `motionScheme` during nested scroll and drag |
| 1.5.0-alpha20 | 2026-05-19 | `rememberBottomSheetState` unified; the old remember functions deprecated |
| 1.5.0-alpha21 | 2026-06-03 | PartiallyExpanded is app-controlled; "Improved BottomSheet dismiss gesture physics" (external contribution) |
| 1.5.0-alpha27 | 2026-08-26 | `LocalMotionScheme` removed |
| 1.5.0-beta01 | 2026-10-07 | Current beta |

Expressive first shipped in 1.4.0-alpha10 (2025-03-12; the Androidify blog of 2025-05-20 cites it).

**Views (MDC and customview)**
- `BottomSheetBehavior`: a finger release settles with `settleCapturedViewAt` (velocity-aware). The nested-scroll
  stop settles with `smoothSlideViewTo`, which passes zero velocity. Constants: significant-velocity threshold
  500 px/s, hide friction 0.1, corner animation 500 ms. **Confirmed** (BottomSheetBehavior.java,
  ViewDragHelper.java).
- `ViewDragHelper` settle:
  - Base 256 ms, maximum 600 ms, quintic ease-out. **Confirmed**.
  - With velocity the duration is 4 × round(1000 × distance / velocity), where "distance" is a snap-weighted
    half-range value, not the raw delta. **Likely** (the Sonnet's "4000 × distance / velocity" is roughly right).

**Predictive back guidance**
- Compose setup: activity 1.6.0+, navigation-compose 2.8.0+, material3 1.3.0+; ModalBottomSheet animates
  automatically; default on Android 15+. **Confirmed**.
- MDC: modal sheets get predictive back from 1.10.0 with `enableOnBackInvokedCallback`. **Confirmed**.
- Design spec, **confirmed** (https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back):
  - full-screen exit scale 100→90%, enter scale 110→100%;
  - fade-through at the 35% progress mark;
  - 8 dp margins, with shift ((screen width / 20) − 8) dp.

**Shared elements**
- `sharedBounds` is the container-transform tool. It does not work across View interop: `Dialog` and
  `ModalBottomSheet` are excluded. ContentScale and shape changes are not animated automatically. **Confirmed**
  (shared-elements page).
- compose-animation history, **confirmed**:
  - 1.9.0-alpha02 (2025-05-07): scroll and drag deltas are applied directly to transitioning shared elements.
  - 1.10.0-alpha04 (2025-09-24): initial-velocity API; stable in 1.10.0 (2025-12-03).
  - 1.12.0-alpha03 (2026-05-19): `mutableTransform`.
  - 1.12.0-beta01 (2026-06-17): `permitTransformDuringDeferredTransition`.
- The Compose August 2026 post says deferred transitions hand off "including velocity transfer". **Confirmed**.

**Cost of effects**
- Compose graphics: alpha below 1 renders offscreen unless `ModulateAlpha`; a RenderEffect always renders
  offscreen. **Confirmed**.
- The performance page says alpha "doubles the required fill-rate", and recommends not drawing more than 2.5 times
  the screen's pixels per frame. **Confirmed**.
- Window blurs (source.android.com):
  - Cross-window blur is expensive and supported only on selected devices.
  - The system disables it in battery saving, some video playback, or by developer override.
  - Over 150 px hurts performance; 80 px gives a good frosted-glass effect.
  - When blur is off, use a more opaque background or a higher dim.
  - **Confirmed**. The "20 px blur-behind" comes from the page's example code, not a recommendation.
- Haze (Chris Banes), on a Pixel 8a at 60 Hz, **confirmed** (https://chrisbanes.github.io/haze/latest/performance/):
  - native Backdrop: 23-46% higher CPU frame P90 than Sources;
  - Glass Balanced: about 8.1 ms of headroom at P90, versus 7.0 ms for Quality;
  - measure on the slowest devices you support.
- Frame budgets: under 16 ms at 60 fps, 11 ms at 90, 8 ms at 120; frozen frames take over 700 ms (page updated
  2026-09-21). **Confirmed**.

**Lazy layouts and blur API**
- Pausable composition in lazy prefetch:
  - 2025-12 blog: on by default.
  - foundation 1.10.6 (2026-03-25): flag disabled by default "to address stability concerns".
  - 1.13.0-alpha03: flag removed; `LazyLayoutCacheWindow` preferred.
  - **Confirmed** as written. Whether the feature is on in today's stable is **unverified**.
- compose-ui 1.13.0-alpha03 added `Modifier.blur { }` for progressive blur; 1.13.0-beta01 renamed the parameter to
  `blurRadiusSpec`. **Confirmed**.

## 8. Corrections to the Sonnet lookups

1. **Sushi and "all Eternal apps".** The blog uses the phrase only as an example of scale, not as a plan to use Sushi
   in every app (T3).
2. **Android posts on Eternal's blog.** There are two (Restaurant Partner and Pulse), not one (T7).
3. **60fps dates.** Each shot has its own `uploadDate` (2024-11-17 to 2026-10-08); it is not a site-wide stamp.
4. **Shimmer easing.** Sushi's shimmer uses tween's default FastOutSlowIn easing and ignores `Auto.duration`; it is
   not "linear".
5. **Sheet fling clamp.** The modal-sheet clamp applies to **downward** flings near Hidden, not "upward flicks".
6. **Window blur 20 px.** "~20 px blur-behind" is example code, not guidance.
7. **"Slow" review.** The 1 May 2026 review does not call the app slow.
8. **Video platforms.** The Business Today, AK Online Support and Tech with cash footage is iPhone. The only Android
   screen recording found is aspira design's.

## 9. Not found or not verifiable

- **UI toolkit.** No public statement of District Android's toolkit, image loader or motion system. No talk, post,
  job ad or repo from District's Android team.
- **App facts.** District Android's minimum SDK, APK size and version number: absent from the Play HTML; mirror info
  pages blocked.
- **Motion numbers.** No durations, easing, spring or overshoot values for any District motion.
- **Android motion.** No Android motion catalogue; every written breakdown is iOS.
- **Bottom sheets on Android.** No Android recording of a District bottom sheet: drag, detents, scrim, blur or
  dismiss.
- **Unread sources.** LinkedIn (HTTP 999), Mobbin (403), the uxdesign.cc Sushi history (403), DesiDime and the
  App Store "No. 1 Lifestyle" rank were not readable or not re-checked.
- **Benchmarks.** No low-end frame-time benchmarks for blur, scrim or container transforms beyond the Haze figures.
- **Lazy prefetch.** The current default of pausable composition in lazy prefetch is unknown.

## 10. Notes for the phone study and the user

- **The toolkit question.** Only the user's device can settle it. The least invasive check reads the running
  window's view hierarchy (`adb shell dumpsys activity top`). A screen hosted in `AndroidComposeView` with no child
  Views is Compose; a tree of widget classes (RecyclerView and the like) is Views. It reads class names the app
  exposes at runtime, so ask the user before running it.
- **What to film on Android,** since nothing public covers it:
  - bottom sheets (seat count, offers, filters, Best Seats);
  - the detail pager (O1) and the panel over the trailer (O2);
  - the grid-to-tabs scroll.
- **Browser pane.** It was left on a paused YouTube page, with the viewport reset to desktop. No ad was clicked.
