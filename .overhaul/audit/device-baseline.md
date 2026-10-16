# Real-device performance baseline

Device: Xiaomi Redmi Note 10 Pro (M2101K6I, serial `9a7c74f4`), Snapdragon 732G (`sm6150`, Adreno 618),
custom ROM on Android 17 / API 37 (`CP2A.260605.016`), 1080x2400 @ 440 dpi, **display locked at 120 Hz**
(`min_refresh_rate=120`, `renderFrameRate 120.00001`, so the frame budget is **8.33 ms**), 5.6 GB RAM, HWUI
pipeline `Skia (OpenGL)`, gesture navigation (`navigation_mode=2`), animator scales at their defaults (1.0).
Battery 33.8–34.5 °C throughout, so no thermal throttling. The second serial (`3627d0ae`) was not attached.

Everything below was measured on 2026-10-08/09 using scripts in
`/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device/` (raw `dumpsys` output is in its
`results/` folder). The frames I looked at are saved in `/home/crazyhusir/Documents/projects/tools/.overhaul/audit/frames/`.

---

## 0. TL;DR

| What | Generated "Full" app (devRelease, R8, speed-profile) | PeriFlow (devBenchmarkRelease, glass) |
|---|---|---|
| Cold start (`am start -W`, TotalTime, median) | **366 ms** (speed-profile), 468 ms (as installed / `verify`) | 469 ms (speed-profile), 560 ms (`verify`, onboarding screen) |
| Tab switching, janky frames (FrameTimeline) | **1.6–1.9 %**, p90 17 ms, p99 36–57 ms | 4.0 %, p90 27 ms, p99 53 ms |
| Push/pop (Back key), janky | **4.2–5.3 %**, p99 36–48 ms | 6.5 %, p99 77 ms |
| Push/pop (back gesture), janky | 3.1 %, p99 31 ms | 6.1 %, p99 73 ms |
| List scrolling, janky | **0.07–0.22 %**, p99 11–12 ms, GPU p90 6 ms | 6.8 % (Today, glass), p90 38 ms, **GPU p90 20 ms** |
| First frame of a tab switch (UI thread) | **21–22 ms median, 46 ms max → 1–4 dropped vsyncs every switch** | 8–12 ms median, 18 ms max |
| PSS after use | 63 MB (Graphics 22 MB) | 108 MB (Graphics 33 MB, Native 34 MB) |
| APK (arm64) | 3.89 MB (debug: 25.06 MB, 6.4x) | 9.34 MB |

Key qualitative results (each one is backed by frames in `audit/frames/`):

1. **Tab switch is a double-exposed cross-fade**, not a push: for ~3–4 frames both tabs are drawn on top of
   each other (titles "Feed" and "Samples" overlap), and the incoming tab is delayed 90 ms. Before that, every
   switch drops 1–4 frames because the target tab is **recomposed from nothing** on each switch.
2. **The bottom bar moves on a different axis and at a different time from the page** on every push and pop:
   - push: the bar stays fully drawn **on top of the incoming page** for ~150 ms, then drops downward;
   - pop with the Back key: the bar springs **up over the outgoing page** while that page still covers 60 % of
     the screen;
   - pop with the back gesture: the root page settles with an **empty strip where the bar belongs**, and the
     bar rises into it 100–200 ms later.
   This is the glitchy, "pushy" bar the overhaul brief describes.
3. Root tab screens are **clipped above the floating bar** by a reserved strip. Content never scrolls under
   the bar, and a hard cut line shows above it.
4. The **devtools badge cannot be dragged**: a 503x1038 px drag moved it by (7, 14) px. Confirmed on device;
   root cause is a stale capture in `DevBadge.kt`.
5. Plain scrolling in the generated app is excellent (0.07 % jank). The costs are concentrated in
   **navigation**: tab switch, pop, and bar choreography.
6. PeriFlow's glass makes every frame GPU-bound on this phone (GPU p50 9 ms, p90 20 ms, against an 8.33 ms
   budget). Its navigation choices are still clearly better: tabs stay composed, the bar fades in place, and
   content scrolls under the bar. The overhaul should adopt those, with glass as an opt-in that has a measured
   budget.

---

## 1. Generating and building

```
cd android-base/generator && python3 create_project.py --spec tests/spec_full.json \
   --out /home/crazyhusir/Documents/projects/tools/.overhaul/out/Full --no-zip --force     # 0.23 s
```

`tests/spec_full.json` is **not "all features"**. It leaves out `devtools`, `onboarding`, `lottie`, `licenses`,
`applock`, `playstore`, `database`, `composemetrics`, `coverage` and `depsanalysis` (`--dry-run`: "Enabled: 26 of 36
features"). To test the devtools drag bug I generated a second project, `out/FullDevtools`, from
`scratch/device/spec_full_devtools.json` (spec_full plus `"devtools"`).

### Variants

- Flavours `dev`, `staging`, `prod`, `playstore` (`build-logic/.../AppConfig.kt:35-80`); `playstoreDebug` is
  disabled (`Flavors.kt:126-131`).
- Build types: `debug`, `release` (`isMinifyEnabled=true`, `isShrinkResources=true`,
  `AndroidApplicationConventionPlugin.kt:28-31`), plus `benchmarkRelease` and `nonMinifiedRelease` added by the
  `androidx.baselineprofile` plugin.
- Signing (`Signing.kt:80-89`): with no `keystore.properties`, **every flavour's release is signed with the
  debug key**. Gradle says so: `[signing] No key configured for dev, staging, prod, playstore — those variants
  will be signed with the debug key`.

I chose **`devRelease`** as the most production-like installable variant: R8-minified, resources shrunk,
non-debuggable, debug-signed. The `dev` flavour points at `https://jsonplaceholder.typicode.com/`, so Home
(Samples) and Feed load real data. `prod` points at `api.acme.example.com`, which does not exist. The release
manifest has no `<profileable>` (checked with `aapt2 dump xmltree`), so CPU profiling of this variant is
impossible, but `dumpsys gfxinfo` works on any app.

### Build times (Gradle 9.7.1, machine shared with other agents, run under `.overhaul/bin/locked gradle`)

| Build | Wall time | Tasks |
|---|---|---|
| `Full :app:assembleDevRelease` (first build of this project) | **234 s** (3 m 52 s) | 1132 actionable: 999 executed, 133 from cache. Configuration cache stored. |
| `Full :app:assembleDevDebug` (right after) | **74 s** | 701 actionable: 425 executed, 270 from cache |
| `FullDevtools :app:assembleDevRelease` (warm build cache) | **119 s** | 1173 actionable: 749 executed, 424 from cache |

### APK sizes

| APK | Bytes | Notes |
|---|---|---|
| Full `app-dev-arm64-v8a-release.apk` | **3,890,120** | `classes.dex` 5.62 MB raw / 2.80 MB deflated; `resources.arsc` 496 KB; `assets/dexopt/baseline.prof` 11 KB (library profiles only) |
| Full `app-dev-universal-release.apk` | 3,988,008 | |
| Full `armeabi-v7a` / `x86` / `x86_64` | 3,888,196 / 3,887,724 / 3,889,178 | |
| Full `app-dev-debug.apk` | **25,061,673** | 58 MB of unminified dex, LeakCanary. **6.4x the release.** |
| FullDevtools arm64 release | 3,939,272 | devtools costs **+49 KB** |
| PeriFlow dev benchmarkRelease arm64 (prebuilt, 2026-09-26) | 9,343,672 | |
| PeriFlow prod release arm64 (prebuilt, 2026-09-29) | 12,619,679 | |

Size observations:

- **The ABI splits are pointless for this app.** The only native code is `libandroidx.graphics.path.so` and
  `libdatastore_shared_counter.so`, about 10 KB each per ABI. The five split APKs differ from each other by
  under 3 KB and from the universal APK by 98 KB, yet every release build produces, signs and zips five APKs
  (`template/build-logic/convention/src/main/kotlin/com/base/app/buildlogic/Flavors.kt:59-69`, `isEnable = explicit ?: assemblingRelease`).
- 21 `META-INF/**/LICENSE.txt` files (106 KB uncompressed) and 54 KB of `kotlin/*.kotlin_builtins` ship in the
  release APK. A `packaging { resources.excludes += ... }` removes them.
- **No app baseline profile is shipped.** `find app/src -name "*baseline*"` finds nothing, even though
  `baselineprofile` is in the spec. Only the libraries' 11 KB profile is merged, and section 2 shows what that
  costs.

---

## 2. Cold start

Method: `am force-stop`, 2 s sleep, then `am start -W -n com.acme.field.dev/com.acme.field.MainActivity`.
TotalTime is measured to the first drawn frame; the splash is held until settings and the auth token are read
(`MainActivity.kt:118`). Getting the Full app past the sign-in screen needed a session: `auth/login` on
jsonplaceholder returns 404, so the app can never sign in by itself. I wrote a DataStore file containing the
two token keys through `run-as` on the debug build, then installed the release over it with `install -r`
(same debug key, so app data survives). `isAuthenticated` only checks that the keys exist
(`AuthTokenStore.kt:40-42`). PeriFlow ran through onboarding as a guest, with a dummy birth year and period
date; its dev flavour talks to `10.0.2.2`, which the phone cannot reach.

| App / state | Runs (TotalTime ms) | Median |
|---|---|---|
| Full devRelease, as installed (`status=verify`) | 448, 450, 489, 471, 464, 475 | **468** |
| Full devRelease, after profile install + `cmd package compile -m speed-profile` | 422, 364, 362, 368, 365, 392 | **366** |
| Full devRelease, first launch after a fresh `install -r` | 908 (rel2), 819 (FullDevtools) | n/a |
| Full devRelease, hot (HOME, then relaunch) | 39, 57, 41 | 41 |
| Full devDebug | 2984 (first, sign-in screen), 2430 (tabs) | ~2.4–3.0 s |
| PeriFlow, as installed (onboarding screen; WARM runs excluded) | 595, 561, 559, 510 | 560 |
| PeriFlow, speed-profile (Today, glass) | 479, 469, 477, 422, 451, 455, 516 | **469** |

Reading: the template starts fast. Release start is 22 % faster once speed-profile compiled, and that gap is
exactly what a real app baseline profile would close on Play installs. The debug build is 6–8x slower, so
nobody should judge startup or smoothness on `devDebug`.

---

## 3. Frame statistics

Method: `dumpsys gfxinfo <pkg> reset`, the scripted interaction on the device (`input tap` / `input swipe` in a
single `adb shell` loop), then `dumpsys gfxinfo <pkg>` and `... framestats`. "Janky" is the modern
FrameTimeline count (frames that missed their present deadline). The legacy count, also recorded, is
meaningless at 120 Hz because it flags every frame over 16 ms. "High input latency" counts are inflated by
`input` injection and should be ignored.

Scenarios (`scratch/device/scen.sh`):

- **tabs**: 3 cycles over all 7 tabs (Feed, Search, Orders, Wallet, Profile, Settings, Home), 0.9 s apart, so 21 switches.
- **pushpop**: 8 times, tap the first Home card, wait 1.3 s, Back key, wait 1.3 s.
- **pushpop_gesture**: 6 times, tap a card, then a back gesture from the left edge (`input swipe 3 1200 760 1250 260`).
- **scroll_home / scroll_feed**: 6 flings up, then 6 down (`input swipe 540 1800 540 700 140`).

| Run | Frames | Janky | p50 | p90 | p95 | p99 (ms) | GPU p50/p90/p95 | Slow UI thread |
|---|---|---|---|---|---|---|---|---|
| **Full devRelease** tabs (run 1) | 1331 | **1.88 %** | 8 | 17 | 19 | 36 | 6/10/12 | 22 |
| Full devRelease tabs (run 2, fresh install) | 1332 | 1.73 % | 9 | 17 | 19 | 53 | 6/11/12 | 23 |
| FullDevtools devRelease tabs (`verify`, badge on screen) | 1334 | 1.57 % | 9 | 17 | 19 | 57 | 6/10/12 | 21 |
| **Full devRelease** pushpop (run 1) | 856 | **5.02 %** | 8 | 14 | 22 | 36 | 6/9/10 | 32 |
| Full devRelease pushpop (run 2) | 855 | 4.21 % | 8 | 12 | 22 | 38 | 6/7/10 | 32 |
| FullDevtools pushpop | 848 | 5.31 % | 8 | 15 | 24 | 48 | 6/8/10 | 41 |
| Full devRelease pushpop, back gesture | 709 | 3.10 % | 8 | 12 | 17 | 31 | 6/7/10 | 15 |
| **Full devRelease** scroll Home (Samples, 100 cards) | 1359 | **0.07 %** | 8 | 9 | 9 | 11 | 6/6/6 | 0 |
| Full devRelease scroll Feed (paging) | 1358 | 0.22 % | 8 | 9 | 9 | 12 | 6/6/6 | 2 |
| Full **devDebug** tabs | 986 | **20.59 %** | 14 | 40 | 77 | 200 | 5/7/9 | 203 |
| Full devDebug pushpop | 687 | 10.92 % | 11 | 30 | 77 | 200 | 5/10/13 | 75 |
| Full devDebug scroll Home | 1302 | 4.76 % | 10 | 17 | 21 | 27 | 6/6/6 | 61 |
| **PeriFlow** tabs (5 tabs, glass bar) | 1129 | **3.99 %** | 17 | 27 | 36 | 53 | 8/11/17 | 38 |
| PeriFlow pushpop (Today ↔ You) | 1222 | 6.46 % | 17 | 32 | 46 | 77 | 9/13/– | 55 |
| PeriFlow pushpop, back gesture | 904 | 6.08 % | 17 | 29 | 44 | 73 | 9/12/21 | 42 |
| PeriFlow scroll Today (glass) | 703 | **6.83 %** | 16 | 38 | 46 | 61 | **9/20/–** | 14 |

(PeriFlow's `scroll_feed` on Learn rendered 0 frames: the dev backend is unreachable, so Learn had nothing to
scroll.) Idle check: after 5 s idle on Home, Feed, Search and Profile, and on PeriFlow Today, **0 frames** were
rendered. Nothing animates forever.

### 3.1 Where the cost sits: per-frame breakdown of a single transition

`scratch/device/parse_fs2.py` splits each `framestats` row into animation/composition
(`AnimationStart→PerformTraversalsStart`), layout+draw (`→SyncQueued`), RenderThread and GPU. Composition
inside `AndroidComposeView.dispatchDraw` lands in "layout+draw".

Distribution over 6 repetitions each way (`scratch/device/first.sh` + `firstframe.py`):

| Transition | 1st-frame UI ms (median / max) | 2nd-frame UI ms | Dropped vsyncs at start (median / max) |
|---|---|---|---|
| Full: tab Home→Feed | **20.9 / 21.9** | 1.5 | 1.5 / 2 |
| Full: tab Feed→Home | **22.3 / 45.8** | 1.7 | 2 / 4 |
| Full: push (Home → detail) | 6.0 / 9.6 | 7.9 | 0 / 0 |
| Full: pop (Back key) | 11.5 / 25.7 | **18.3** | 0 / 2 |
| PeriFlow: tab Today→Learn | 12.2 / 12.4 | 0.3 | 1 / 1 |
| PeriFlow: tab Learn→Today | 7.9 / 18.4 | 0.3 | 0 / 1 |
| PeriFlow: push (Today → You) | 4.0 / 10.3 | **33.1** | 0 / 0 |
| PeriFlow: pop (Back key) | 16.6 / 37.2 | 21.7 | 1 / 3 |

Representative single traces from `results/rel_*_fs.txt`:

```
Full, tab Home→Feed
    0.0 anim/compose= 7.15 layout+draw=14.55 rt=13.74 total=35.96   <- whole Feed tab composed + measured; 2 vsyncs skipped (8.4 -> 33.4)
    8.4 anim/compose= 0.82 layout+draw= 0.59 rt=18.39 total=36.47
   33.4 ...                                rt=14.76
Full, tab Feed→Home (second sample, rel2)
    0.0 anim/compose=19.54 layout+draw=32.86 rt=13.60 total=67.60   <- 52 ms UI; next frame is 58 ms later (5 vsyncs)
Full, pop (Back key)
    0.0 anim/compose= 8.25 layout+draw=19.06 rt= 9.62 total=38.32   <- root list recomposed from scratch
    8.4 anim/compose= 5.25 layout+draw=13.22 rt= 7.04 total=48.13   <- bottom bar AnimatedVisibility enters
   33.4 ...                                                          <- 2 vsyncs skipped
Full, steady push/pop frames: anim/compose 0.3–0.5, layout+draw 0.3–0.5, rt 5–7   (very cheap)
Full, tab-switch animation frames on Feed→Home: rt 14–19 ms for ~15 frames   (alpha+scale of a full screen of shadowed cards)
PeriFlow, tab Today→Learn: anim/compose 4.2, layout+draw 3.1, but rt 32.6 / 26.0 / 24.3 / 29.4 ms   (GPU-bound on glass)
PeriFlow, steady frames: UI ~1.2 ms, rt 14–17 ms  -> latency ~20 ms per frame at 120 Hz
```

Conclusions:

- In the generated app, **steady animation is cheap and the stalls come from composition at transition
  boundaries.** Each tab switch composes the whole destination tab (21–52 ms), and each pop recomposes the
  root list (11–28 ms over two frames). That is what reads as a hitch at the start of a tab switch and a stutter
  at the start of a pop.
- In PeriFlow the UI thread is light (tabs stay composed) but **the GPU is over budget on every frame** while
  the glass is on screen. RenderThread spends 14–33 ms per frame, and GPU p90 rises from 6 ms to 20 ms while
  scrolling.

---

## 4. What the transitions look like, frame by frame

Recorded with `screenrecord --size 720x1600` (the default 1080x2400 at 120 fps produced "recorded 0 frames" on
this ROM). Every frame was extracted with `ffmpeg -fps_mode passthrough`, at about 8 ms per frame while
animating. The tiles carry a frame number and timestamp. A "120" refresh-rate overlay (a developer option
already on, which I left untouched) appears at the top left of every frame.

### 4.1 Tab switch (Full): `frames/full_tab_home_to_feed_sequence.png`, `frames/full_tab_crossfade_double_exposure_3up.png`, `frames/full_tab_orders_to_home_sequence.png`

- t=0–70 ms (#5–#13): only the **bar's sliding pill** moves, from Home toward Feed (a `sheet()` spring with
  damping 0.86, a slight overshoot). The content does not change.
- t≈85–120 ms (#15–#17): **double exposure**. Home fades out over 135 ms while Feed fades in, delayed 90 ms,
  scaling from 0.985. For 3–4 frames both screens are visible at once: "Feed" and "Samples" overlap, and the
  cards of both lists interleave (3-up image). On Orders→Home, the "Something went wrong" error state of Orders
  shows through the Home list (#200–#204).
- t≈120–200 ms: Feed settles. The visible change runs about 200 ms in total, plus the dropped frames at the
  start (section 3.1).
- Throughout, the bar keeps 7 items. Labels truncate ("Settin…"), and **Orders and Wallet share the same Grid
  icon**.

Root cause: `template/core/navigation/src/main/kotlin/com/base/app/core/navigation/NavTransitions.kt:105-112`
(`tabSwitch` = delayed `fadeIn` + `scaleIn(0.985)` `togetherWith` a 135 ms `fadeOut`), run by an `AnimatedContent`
keyed on the selected index (`AppNavHost.kt:99-105`). That `AnimatedContent` also **disposes the previous tab's
`NavDisplay` and composes the new one from scratch**, which is where the 21–52 ms first frame comes from.

### 4.2 Push (Full): `frames/full_push_sequence.png`, 4th panel of `frames/full_bar_glitches_4up.png`

- The detail page slides in from the right, a critically damped spring (stiffness 400). Home moves left with
  25 % parallax and dims 28 %.
- **The bottom bar stays fully drawn on top of the incoming detail page** from #9 to #21 (1063–1164 ms, about
  100–150 ms). The detail slides *under* the floating pill. Only then does the bar fall away downward (#24–#33).
  Content moves horizontally while the bar moves vertically, with a delay between them.
- The outgoing Home is **clipped at the bar's reserved strip** (cards end at about 87 % of the height with
  blank background below), while the detail fills the full height.

Root cause: `AppShell.kt:227-232`. The bar is an `AnimatedVisibility` above the `NavDisplay`, keyed on
`onRoot` (`AppShell.kt:175-176`). Its exit is `slideOutVertically(tween(motion.medium=280, easing = motion.exit))`,
and `motion.exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)` (`AppMotion.kt:103`) is a strong ease-in: about
10 % of the travel happens in the first half of the 280 ms. So the bar visibly sits on top of the new page.

### 4.3 Pop with the Back key (Full): `frames/full_pop_keyback_sequence.png`, 3rd panel of `frames/full_bar_glitches_4up.png`

- #56–#59: Home slides back in from the left with parallax, and the detail starts sliding out to the right.
- #62–#68 (2722–2772 ms): **the bar springs up from the bottom edge across the full width, on top of the
  detail page that still covers about 60 % of the screen.** `onRoot` turns true as soon as the stack pops, so the
  bar's enter (`slideInVertically(motion.sheet())`, a bouncy spring) starts at frame 1 of the pop.
- #74–#86: a 10–15 % sliver of the detail page lingers at the right edge for about 100 ms. That is the tail of a
  critically damped spring with stiffness 400 (`AppMotion.navigation()`, `navigationStiffness=400f`,
  `navigationDamping=1f`, `AppMotion.kt:124-130`). The end of the pop feels slow even though the start is fast.
- The first two frames of the pop cost 26–33 ms of UI time (root list recomposed, bar entering) and skip about
  2 vsyncs.

### 4.4 Pop with the back gesture (Full): `frames/full_pop_gesture_sequence.png`, panels 1–2 of `frames/full_bar_glitches_4up.png`

- The system back arrow appears and the pop runs as a plain slide.
- #183–#195 (6139–6238 ms): **Home has settled, but there is no bar.** The bottom 13 % is the reserved strip,
  empty (panel 1 of the 4-up).
- #201–#213 (6288–6388 ms): **the bar rises into the empty strip 100–200 ms after the page stopped moving**
  (panel 2). With predictive back, the stack pops only when the gesture commits, so `onRoot`, and with it the
  bar's enter animation, arrives late.
- Result: the gesture pop and the button pop show **opposite** bar timings: late with the gesture, early with
  the button. Neither matches the page.

### 4.5 PeriFlow for comparison: `frames/periflow_*`

- **Tab switch** (`periflow_tab_today_to_calendar_sequence.png`): content swaps **instantly**, with no
  cross-fade and no double exposure. Only the glass "lens" in the bar animates, stretching from Today to
  Calendar over about 150 ms. This is the iOS model.
- **Push** (`periflow_push_sequence.png`): the glass bar **fades out in place** (gone within about 3 frames) while
  the page slides. There is no vertical motion, and content runs to the bottom edge with no reserved strip.
- **Pop** (`periflow_pop_keyback_sequence.png`): the bar fades in at once over both pages. Still drawn over the
  outgoing page, but a short in-place fade reads far less jarring than a slide-up.
- **Back gesture** (`periflow_pop_gesture_sequence.png`): the page follows the finger progressively, and the bar
  fades in during the gesture. **No late bar, no empty strip.**
- Code: `KeptTabs.kt:43-72` keeps every opened tab composed and simply does not *place* the hidden ones, with a
  per-tab lifecycle and back dispatcher. `AppShell.kt:287-295` animates the bar with `fadeIn`/`fadeOut(tween(motion.quick))`,
  and `rootBottomInset = 0.dp` lets content run under the bar.

---

## 5. Devtools (`out/FullDevtools`, devRelease)

### 5.1 Badge drag: confirmed broken

| Gesture | Badge before (centre) | Badge after |
|---|---|---|
| `input swipe 97 162 600 1200 600` (503x1038 px over 600 ms) | 97,162 `[44,132][151,193]` | **104,176** `[51,146][158,207]` |
| `input swipe 97 162 700 1500 1500` (slow) | 104,176 | **100,169** (moved *back*) |
| `input swipe 100 169 800 1800 1200` (screen-recorded) | 100,169 | **107,185** |

`frames/fulldevtools_badge_drag_sequence.png`: across 1.8 s of the recorded drag the badge never leaves the
top-left corner, while the finger travels 700x1630 px.

Root cause, `template/core/devtools/src/main/kotlin/com/base/app/core/devtools/DevBadge.kt:64-85`:

```kotlin
val x = if (offsetX.isNaN()) startX else offsetX      // read in composition
...
.pointerInput(bounds, badge) {                        // keys do not include x/y, so the block is never restarted
    detectDragGestures { change, drag ->
        offsetX = (x + drag.x)...                     // x is the value captured when the block started
        offsetY = (y + drag.y)...
```

Each move event sets the offset to *stale start + this single event's delta*, so the badge jitters around
where it was when the gesture began, and the last delta decides where it lands. The fix is to accumulate from
state:

```kotlin
.pointerInput(bounds, badge) {
    detectDragGestures { change, drag ->
        change.consume()
        val maxX = (bounds.width - badge.width).coerceAtLeast(0).toFloat()
        val maxY = (bounds.height - badge.height).coerceAtLeast(0).toFloat()
        offsetX = ((if (offsetX.isNaN()) startX else offsetX) + drag.x).coerceIn(0f, maxX)
        offsetY = ((if (offsetY.isNaN()) startY else offsetY) + drag.y).coerceIn(0f, maxY)
    }
}
```

Other observations: the badge sits on top of every screen title by default (it covers "Samples" at 48 dp from
the top). The tabs frame stats with the badge on screen (1.57 % janky) match the build without it, so the
overlay costs nothing measurable.

### 5.2 Inspector panel shows "0 kB" for real responses

`frames/fulldevtools_inspector_panel.png`: the panel shows `GET /posts · 1035 ms · 0 kB · 200`, but
jsonplaceholder's `/posts` returns 100 posts. The size comes from
`template/core/network/src/main/kotlin/com/base/app/core/network/NetworkRecording.kt:36`
(`responseBytes = response.contentLengthOrZero()`). A chunked or compressed response has no Content-Length,
so it records 0. The integer division `responseBytes / BYTES_PER_KB` (`DevToolsPanel.kt:159`, `:206`) also
shows anything under 1 KB as 0. Count the bytes actually read instead, and format with one decimal.

---

## 6. Findings (ordered by impact on perceived smoothness)

### F1. Every tab switch recomposes the whole destination tab: 21–52 ms first frame, 1–5 dropped vsyncs (high, performance)

- Evidence: `template/core/navigation/src/main/kotlin/com/base/app/core/navigation/AppNavHost.kt:99-105`
  (`AnimatedContent(targetState = state.selectedIndex) { index -> StackDisplay(entries = tabEntries[index]) }`).
  Device: first-frame UI median 20.9/22.3 ms (max 45.8 ms, one sample 52.4 ms), with the next frame 25–58 ms
  later. PeriFlow, which keeps tabs composed, measures 7.9–12.2 ms.
- Fix: port PeriFlow's `KeptTabs` (`periflow-android/core/navigation/.../KeptTabs.kt:43-72`). Keep opened tabs
  composed, skip `place()` for hidden ones, give each tab its own `LifecycleOwner` so hidden tabs drop to
  STARTED/CREATED, and give each a back dispatcher. Switch instantly, and animate only the bar indicator.
  Optionally add a ≤100 ms alpha fade-in on the incoming tab's `graphicsLayer`, with no outgoing fade.

### F2. Tab-switch transition double-exposes two full screens (high, ux-design)

- Evidence: `NavTransitions.kt:105-112`. Frames #15–#17 and #200–#204 show both tabs' text overlapped
  (`frames/full_tab_crossfade_double_exposure_3up.png`). The incoming tab is held back by
  `delayMillis = motion.instant` (90 ms). During the fade, RenderThread time rises to 14–19 ms per frame,
  because alpha plus scale on a screen of shadowed cards needs offscreen layers.
- Fix: no cross-fade between peers. Swap instantly (iOS), or fade the incoming tab only. Delete
  `tabSwitch`/`TAB_ENTER_SCALE`.

### F3. The bottom bar animates out of sync with push/pop, on another axis (high, ux-design)

- Evidence: `AppShell.kt:175-176, 227-232` and `AppMotion.kt:103` (`exit = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)`):
  - push: the bar stays over the incoming page for about 100–150 ms (`frames/full_push_sequence.png` #9–#21);
  - Back-key pop: the bar springs up over the outgoing page from frame 1 (`full_pop_keyback_sequence.png`
    #62–#68);
  - gesture pop: the bar appears 100–200 ms after the page settles, into an empty strip
    (`full_pop_gesture_sequence.png` #183–#213, `full_bar_glitches_4up.png`).
- Fix, measured on PeriFlow: hide and show the bar with a short in-place fade (`tween(motion.quick)`, linear or
  decelerate) and **no vertical slide**. The more iOS-faithful option (`hidesBottomBarWhenPushed`) moves the bar
  *with* the root page: draw it in a `NavEntryDecorator` that wraps only root entries, so it rides the same
  slide and predictive-back progress as its page. Either way, take `onRoot` from the scene that is actually on
  screen (`NavDisplay`'s transition state), not from the back stack, so gesture and button pops behave the same.

### F4. Root screens are clipped above the floating bar by a reserved strip (medium, ux-design)

- Evidence: `AppNavHost.kt:93` gives the root entry `inset = AppBottomBarDefaults.occupiedHeight()`
  (`AppShell.kt:221`; floating = 64 dp + 2x12 dp + nav bar, `AppBottomBar.kt:81-85`). `AppNavHost.kt:156-163`
  applies it as `Modifier.background(...).padding(bottom = inset)`. On device the list stops at a hard edge 13 %
  above the bottom (every Full screenshot). During push/pop that strip is the "content jumps by inset height"
  moment: the root page has a blank band, and the pushed page does not. With a floating, translucent bar this
  also wastes space.
- Fix: pass the bar height down as `contentPadding` (a `LocalShellInsets` CompositionLocal that root screens
  add to their `LazyColumn` `contentPadding`), so content scrolls under the bar. PeriFlow does this
  (`rootBottomInset = 0.dp`).

### F5. Pop recomposes the root screen from scratch: 26–33 ms over the first two frames (medium, performance)

- Evidence: `results/rel_pop1_fs.txt` (8.25 + 19.06 ms, then 5.25 + 13.22 ms UI, 2 vsyncs skipped);
  6-run median 11.5 ms then 18.3 ms. `NavDisplay` keeps only the top entry composed, so the root list, its
  ViewModel state collection and the bar's `AnimatedVisibility` all start cold on frame 1 of the pop.
- Fix: (a) ship a real app baseline profile covering startup, tab switch, push and pop. The template has a
  `benchmark` module but no generated `baseline-prof.txt` (`find app/src -name "*baseline*"` returns nothing). A
  missing profile cost about 100 ms of cold start here (468 → 366 ms), and the same JIT penalty hits the first
  pop. (b) Make list rows cheaper (the Home cards use elevation shadows; GPU and RT are 14–19 ms during
  transitions involving them). (c) Longer term, keep the previous entry composed underneath, like iOS, with a
  custom scene. PeriFlow did not solve this either (pop 16.6 / 21.7 ms).

### F6. The navigation spring leaves a slow sliver at the end of every pop (low, ux-design)

- Evidence: `AppMotion.kt:124-130` (`navigationDamping = 1f`, `navigationStiffness = 400f`). Frames #74–#86 of
  `full_pop_keyback_sequence.png` show a 10–15 % sliver of the old page at the edge for about 100 ms. A full push
  or pop renders about 50 frames (420 ms) before the content stops moving.
- Fix: a stiffer spring for page travel (`Snappy` already uses 700) or an iOS-like response of about 0.35 s.
  Tune by recording, as in section 4.

### F7. Debug builds are 10x jankier and must not be used to judge motion (medium, dx)

- Evidence: devDebug tabs 20.59 % janky, p99 200 ms, against 1.88 % on devRelease. Debug cold start is 2.4–3.0 s
  against 0.37–0.47 s. The debug APK is 25.06 MB against 3.89 MB. The only run instruction a generated project gets is
  `./gradlew :app:installDevDebug` (`generator/create_project.py:366`, `generator/genkit/readme.py:26`,
  `template/AGENTS.md:35`).
- Fix: document `installDevBenchmarkRelease` (profileable, R8, debug-signed; the baselineprofile plugin already
  creates it) as the way to judge smoothness. Consider a `run`/`perf` script in the generated project that
  installs it and prints gfxinfo, as `scratch/device/scen.sh` does.

### F8. Devtools badge cannot be dragged (medium, bug). See section 5.1.

### F9. Devtools reports 0 kB for chunked or compressed responses (low, bug). See section 5.2.

### F10. Scaffolded feature modules become tabs with identical icons; 7 tabs truncate labels (low, ux-design)

- Evidence: `generator/genkit/scaffold.py:23` emits
  `ShellTab(key = …ListKey, label = …, icon = AppIcons.Grid)` for every `feature_modules` entry. With
  `spec_full` the bar has **7 tabs** (`out/Full/app/.../ui/AppDestinations.kt:21-30`). On device "Settings"
  renders as "Settin…", and Orders and Wallet show the same glyph (`frames/full_tab_home_to_feed_sequence.png`).
- Fix: cap the tabs at 5 (overflow goes into Profile or More), and do not make every scaffolded module a tab by
  default.

### F11. ABI splits produce 5 near-identical APKs (low, bloat/dx)

- Evidence: per-ABI APKs 3,887,724–3,890,120 bytes, universal 3,988,008. The only native code is about 84 KB of
  tiny `.so` files across 4 ABIs. `configureAbiSplits` turns splits on for every `*Release*` APK task.
- Fix: default the splits off, and turn them on only when a native-heavy dependency is added (`abiSplits=true`
  already exists).

### F12. Release APK ships LICENSE.txt and kotlin builtins (low, bloat)

- Evidence: `unzip -l` shows 21 `META-INF/androidx/**/LICENSE.txt` files (106 KB) and 54 KB of
  `kotlin/*.kotlin_builtins`. Fix: `packaging.resources.excludes += listOf("META-INF/**/LICENSE.txt", "kotlin/**.kotlin_builtins")`
  in the application convention plugin.

### F13. "Showing saved data" banner uses a Wi-Fi-off icon for cached data (low, ux-design)

- Evidence: `template/feature/sample/.../SampleListScreen.kt:101-106` shows
  `AppBanner(..., icon = AppIcons.WifiOff)` whenever `state.isFromCache`. `template/core/network/.../KtorNetworkClient.kt:96-100` emits the
  saved copy first on every `stream()`. On the Full build the banner, with the Wi-Fi-off icon, was on screen in
  every capture while the phone was on Wi-Fi. (Why the follow-up refresh did not clear it on that build was not
  established. On the FullDevtools build the refresh logged `200` and the banner was gone.)
- Fix: show "updated N min ago" with a neutral icon. Reserve the Wi-Fi-off treatment for an actual
  `isOffline` failure.

### F14. The floating bar's sliding pill recomposes every animation frame (low, performance)

- Evidence: `template/core/designsystem/.../component/navigation/AppBottomBar.kt:161-170`:
  `val x by animateDpAsState(...)` read in composition and applied with `Modifier.offset(x = x)`. Device: composition
  during tab-switch frames is 0.8–1.1 ms against 0.3–0.4 ms during push. That is small, but it runs on every
  frame of every switch.
- Fix: `Modifier.offset { IntOffset(x.roundToPx(), 0) }` with `val x = animateDpAsState(...)` (read the State
  in the layout lambda), or `graphicsLayer { translationX = ... }`.

### F15. Liquid glass (Kyant backdrop, as in PeriFlow) is GPU-bound on a mid-range phone (high for the overhaul's "optional glass at high FPS" goal, performance)

- Evidence: PeriFlow scroll on Today: 6.83 % janky, p90 38 ms, GPU p90 20 ms (against 6 ms for a plain list).
  Tabs 3.99 %, push/pop 6.1–6.5 %. RenderThread 14–33 ms per frame during transitions. PSS 108 MB against 63 MB
  (Graphics +11 MB, Native +23 MB). Code:
  - `GlassStage` records the **entire scene** (aura + content) into a `layerBackdrop` on every frame
    (`periflow-android/core/designsystem/.../foundation/Glass.kt:83-92`);
  - the bar runs `vibrancy() + blur(8dp) + lens(24dp, 24dp)` plus highlight and shadow
    (`GlassBottomBar.kt:180-186`);
  - it also records a second, invisible copy of the tabs into `tabsBackdrop` for the lens
    (`GlassBottomBar.kt:228`, `:253-265`, the comment says "once more every frame");
  - `ProgressiveBlur.kt:79-84` adds another backdrop blur.
- What to keep for the generator: glass as an **opt-in design-system variant** with a measured budget. On this
  phone, GPU p90 must stay at or under 8 ms during scroll. Levers, in order: blur only the bar's own bounds (do
  not record a full-screen layer when only a 64 dp strip samples it); drop `lens` and the second tabs layer
  except during a press or drag (PeriFlow scales the press lens by `drag.pressProgress` and swaps its backdrop on
  `lensPressed`, `GlassBottomBar.kt:253-265`, but still records `tabsBackdrop` on every frame); lower blur radius and
  resolution; skip progressive blur on Skia GL devices; fall back to frosted (opaque tint) when
  `isRenderEffectSupported()` is false or the device is low-RAM or 60 Hz. PeriFlow's non-glass navigation
  choices (F1, F3, F4) are worth more than the glass itself.

---

## 7. What to adopt into the generator

1. `KeptTabs` from PeriFlow (`periflow-android/core/navigation/src/main/kotlin/com/jorwen/periflow/core/navigation/KeptTabs.kt`)
   in place of the `AnimatedContent` in `TabbedNavHost` (F1, F2).
2. PeriFlow's bar choreography (`periflow-android/core/navigation/.../AppShell.kt:287-295`): in-place fade, no
   slide, content runs under the bar (`rootBottomInset = 0.dp`) (F3, F4).
3. A device perf harness in the generated project, modelled on `scratch/device/scen.sh` and `parse_gfx.py` /
   `parse_fs2.py`, or Macrobenchmark `FrameTimingMetric` journeys in the existing `benchmark` module (tab
   switch, push, pop, scroll). Plus a generated baseline profile, so "smooth" can be checked in CI and on a
   phone (F5, F7).
4. The per-frame present-cadence parsing (`DisplayPresentTime`, `FrameDeadline`) from
   `scratch/device/parse_fs2.py` as a reference for the admin and perf tooling.

---

## 8. Reproducing

```
S=/home/crazyhusir/Documents/projects/tools/.overhaul/scratch/device
L=/home/crazyhusir/Documents/projects/tools/.overhaul/bin/locked
$L gradle bash $S/build.sh                      # devRelease + devDebug of out/Full, timed
$L adb bash $S/install_debug.sh; $L adb bash $S/fix_session.sh   # fake session (DataStore keys) via run-as
$L adb bash $S/install_release.sh               # install -r release over it; cold start, verify vs speed-profile
$L adb bash $S/scen.sh com.acme.field.dev rel tabs tab1 push1 pushpop pushpop_gesture scroll_home scroll_feed
python3 $S/parse_gfx.py $S/results/rel_tabs.txt; python3 $S/parse_fs2.py $S/results/rel_pop1_fs.txt
$L adb bash $S/first.sh com.acme.field.dev ff_full_tab 6 "input tap 284 2213" "input tap 156 2213"; python3 $S/firstframe.py $S/results/ff_full_tab
$L adb bash $S/rec_full.sh                      # screen recordings -> results/rec_*.mp4 -> fr_*/f_NNNN.png -> tile.sh
```

Phone state left behind: `com.acme.field.dev` (Full devRelease, with the fake session) and
`com.jorwen.periflow.dev` (PeriFlow dev benchmarkRelease APK from its own build output, onboarded as a guest
with dummy values; its dev backend is unreachable, so nothing left the device). VitFleet
(`com.vitfleet.app`, `com.vitfleet.app.dev`) was installed already and was not opened. It is a signed-in
business app with real data, so it was left out of the comparison. No system settings were changed.
