# District phone study: results

Written 2026-10-20 (02:00-03:00 IST) by the motion and performance analyst (Opus 5.5) for REQUIREMENTS 45, from the
capture a Sonnet operator made on 2026-10-19 (23:04-00:45) on your Redmi, following `PHONE-STUDY.md`. Revised
03:00-04:00 IST after review: the sheet fits were redone with a corrected fitter, and the missed frames are split by
cause (§2.4 lists what changed). Every number below was re-derived from the raw recordings and timing dumps; where the
operator's report or an earlier analysis pass was wrong, §2.4 says so. Raw data, tracks, fits and the scripts that
made them stay local in `research/district/phone/` (`INDEX.txt`; the analysis files are `lib/an2_*.py`,
`lib/an3_refit.py`, `lib/an3_frames.py`, `lib/fit_rest.py`, `lib/crop_check.py`, `lib/patch_mean.py`,
`lib/pooled.py`, `tracks/an2/`, `fits/an2/`, `fits/an3/`, `checks/an2/`, `logs/an3_*.log`). Nothing was committed
or pushed, and this revision did not touch the phone.

**Status in one line.** The template side of the study is complete and measured. **District's side never ran**: the
catalog pre-flight failed (§2.3), and the procedure says a failed pre-flight stops the run until you decide. The only
District numbers on this phone are the frame counters Android kept from your own use of it right after you installed
it (read without touching District, as §1 of the procedure allows).

---

## Part 1. In plain English

### Is the template already ahead on the Redmi?

**On smoothness, very probably yes. On how fast sheets open and close, nobody knows yet.** It is not proven
side by side, because District's side of the study did not run.

- **The template on this phone** (compiled, warmed up, scripted touches): over 7,138 measured frames in 96
  interactions (sheets, page push and pop, tabs, pager, list flings), about 1 frame in 85 missed its deadline.
  Android's own counters say the same: 1.35 % in the catalog and 1.39 % in the sample app. Nine frames in ten were
  ready within 10.7 ms in the catalog and 19 ms in the sample app (the screen refreshes every 8.3 ms). No frame took
  longer than 43 ms, and the app's own work never took longer than 25 ms in any frame.
- **Where the template's missed frames come from.** Nearly all of them (78 of 83) are the first frame of an
  interaction, and that first frame misses for two different reasons:
  - **Our own work.** Opening a sheet with a list inside takes 13.5-24 ms of work on its first frame, so that frame
    misses every time (18 of the catalog's 62 misses). In the sample app, the page push and four of the tab
    switches do the same (7 of its 21).
  - **Start-up after idle.** The app did under 5 ms of work, and the render thread and GPU waking up made the
    frame late (41 in the catalog, 12 in the sample app).
  - Leaving every first frame out, 5 of about 7,040 frames miss (1 in 1,400).
- **District on this phone** (your own session at 20:31, two and a half minutes after installing it): it drew 749
  frames in about 12 seconds, and 1 frame in 14 missed its deadline. Nine frames in ten were ready within 24 ms; the
  slowest one in a hundred took about 200 ms or more. In the last 4 seconds Android kept in detail, its main thread
  **stalled four times for 180-225 ms** on the home screen, three of them in measuring and laying out the screen.
  Those stalls are what lag feels like.
- **Why that is not yet a fair verdict.** District's numbers come from its **first launch**. It was **not compiled**
  yet (Android had only checked its code, so it ran on the slow first-run path) and it was loading live content from
  the network. The template had been compiled ahead of time for this test and shows local sample data. Your Poco F7
  is a much faster phone, and District has probably been compiled there by now. Part of what you felt on the Redmi is
  very likely "first launch of a fresh install". The Redmi compiles apps on its own overnight, while charging and idle,
  and District should get smoother after that. How much smoother cannot be said without measuring it again.

### Where District may still be better

Only the **tempo** of sheets and pages, and that is unmeasured. Our sheets open with 90 % of their travel done in
217 ms, read frame by frame. They close on the same spring: half gone in 91 ms, 90 % in about 211 ms. Whether
District's sheets are quicker (the research guessed they might be, if District uses Google's stock sheet) needs
District's own recordings. Everything else we measured on our side is in good shape:

- sheets follow the finger and keep its speed when you let go;
- a touch catches a moving sheet;
- the dim follows the sheet frame by frame;
- list flings coast like iOS (0.998 of the speed kept per ms);
- page swipes keep most of the finger's speed (about 80 % after a quick flick, all of it after a slower one) and
  land in 0.14-0.18 s.

### What we found in our own template (worth fixing whatever District does)

Sheets that open with a list inside (Filters, Inbox, the language pickers) spend **13.5-24 ms** of main-thread work
on their first frame, two to three times the 8.3 ms a frame has. Most of it (10-16 ms) is laying out the list's rows.
Light sheets (photo actions, now playing) spend 4-7 ms. So every list-sheet open misses its first frame and shows
up about 8 ms later after the tap (46 ms against 38 ms). That missed frame is one of the "first frame" misses above,
the part that is our own work, not a separate kind of miss. **P3 changes because of it** (see below). The sample
app's page push (up to 15 ms on its first frame) and some tab switches (9-10 ms) overrun the same way.

### Which proposals are worth your approval

| # | Proposal | My recommendation | Why, in one line |
|---|---|---|---|
| P8 | Docs rules: intros play once, choices survive a phone call | **Approve now** | needs no measurement; cheap; fixes complaints District's own reviewers make |
| P3 | Cheaper first frame when a sheet opens | **Approve, changed**: aim at our own budget, not at beating District, and cover the list's rows | our list sheets measurably overrun their first frame (13.5-24 ms against 8.3 ms), mostly in laying out the rows |
| P1 | Quicker close (tap outside, Back) | **Wait** for District's numbers | ours measured: half gone in 91 ms, 90 % in about 211 ms; District unknown |
| P2 | Quicker open | **Wait** | ours measured: 90 % in 217 ms (184 ms in the Snappy style); District unknown |
| P4 | Lighter dim | **Wait** | ours measured: 57 % dark, 41 % light, exactly the tokens; District unknown |
| P6 | Detail-page swiping recipe (a), quicker page snap (b) | (a) **your call**, (b) **wait** | our page snap measured: 142 ms after a quick flick, most of the speed kept |
| P5, P7, P9 | Catalog recipes (panel over a hero picture, folding header, centred-card list) | **Your call** | no Android evidence either way; each stands on the video or the iPhone app |
| P10 | Sheets that grow out of their button | **Wait** (stays in the spec's phase 2 until then) | District's recordings decide; nothing public shows District doing it on Android |

**One decision comes before P1, P2, P4, P6 (b) and P10: whether to finish the study.** That means:

1. the lead fixes two measuring tools (the tap-safety check that stopped the run, and the dim meter, which reads
   9 points too dark; §2.13);
2. the catalog pre-flight runs again;
3. the District pass runs, about 1.5 hours with the phone, best after District has been compiled by the phone's
   overnight maintenance so that the comparison is fair.

The run still only watches: it never books, pays or changes anything.

---

## Part 2. The numbers

Conventions:

- Positions come from 720 × 1600 recordings (1 dp = 1.8333 recorded px).
- Motion times are medians of the runs, from the start of motion (t0, fitted when the sheet starts off screen).
  They are read on the 8.3 ms frame grid unless marked "model" (read on the fitted curve).
- Opens and closes from or to hidden are fitted with `phone/lib/fit_rest.py` as fixed in this revision (`fits/an3/`):
  a hidden sheet's top is held at or below the screen's bottom edge (1600 recorded px), and the spring search can
  reach ζ 1 exactly. A frame read moves by one frame (8.3 ms) when the fitted start or rest moves by a few px, so
  each frame-read t90 carries about ±5 ms of fit uncertainty. District's sheets must go through the same fitter with
  the same bounds.
- Template tokens for orientation: `sheet` 0.34 s (k 341.5), `dialog` 0.275 s (k 522), Snappy `sheet` 0.2957 s
  (k 451), `screen` 0.344 s (k 334). All are critically damped.
- **D** = District, **O** = ours. "n/m" = not measured.

### 2.1 Device, versions, compilation

| Item | Value |
|---|---|
| Phone | Redmi `9a7c74f4`, 1080 × 2400, density 440 (2.75), 120 Hz active (modes 120 and 60), gesture navigation |
| System | Android 17, SDK 37, custom ROM (`ro.product.name` custom_sweet, build CP2A.260605.016). `ro.build.fingerprint` and the vendor fingerprint carry the MIUI 13 string, which is what the operator read (§2.4) |
| State during capture | battery saver off; animation scales 1.0 (animator unset = default); long-press 400 ms; thermal status 0 throughout; AC, 85 %, battery 31-32 °C. Settings read again at the end: unchanged |
| Rendering | HWUI "Skia (OpenGL)" pipeline in every dump, District's included |
| District | 2.76.0 (versionCode 1000027600, minSdk 32, targetSdk 37), from Play, installed 2026-10-19 20:29:13. **Compilation: `verify` (reason vdex-dm)**, before and after the run: no ahead-of-time compiled code. Never touched, compiled or stopped by the run |
| Template | build `722ced37` (`preview/catalog-2026-10-19`, merge of `wp/WP04` at 6c6ee60): catalog 1.0.0 (release) and sample app 1.0.0-devRelease. Compiled by the run with `cmd package compile -m speed-profile -f` (status speed-profile, reason cmdline). No ProfileInstaller broadcast came first, so how much of the code the profile covered is unknown |

### 2.2 What ran and what stopped

- **Ran (template only):**
  - 183 scene recordings;
  - 104 timing dumps;
  - the pre-flight;
  - device facts.
- **Stopped:**
  - everything on District: S1-S13, T1-T5, F1-F4, H1, H2, P1, P2, the timing pass, and §6's window and class
    names;
  - by §4.0 ("any failure stops the run") and §9, after pre-flight item 3 failed;
  - no tap, drag, press, launch or recording was sent to District.
- **§9's stop was applied to District only.** The template captures went on after the pre-flight failure. The
  workflow told the operator to record a failure and go on, and the catalog and the sample app are your own test
  packages, so nothing on District was risked by them.
- **Read without touching District (allowed by §1, "reading frame timings"):**
  - one `dumpsys gfxinfo` of District's running process (pid 2643, started 20:31:51), taken at 01:02 by the
    earlier analysis pass: `phone/timing/District_passive_gfxinfo.txt`.
  - It holds Android's counters for everything District drew since that start, and the last 120 frames in detail.
- **Phone reads by the analysis passes.** At 01:02 the earlier pass ran four read-only commands: `getprop`, `ps`,
  `uptime` and the gfxinfo read above. §1 of the procedure allows them, but the analyst's instruction allowed
  touching the phone only to re-take a missing or wrong capture, so they went beyond it. Nothing was changed on the
  phone. This revision did not touch the phone.
- **Not in this build of the template:**
  - the "Collections" carousel (F3);
  - a full-bleed detail pager (T4 used the 160 dp "Bars and pages" pager);
  - F4 (optional) was skipped.

### 2.3 Pre-flight (§4.0), re-checked

| Item | Operator | Re-check | Verdict |
|---|---|---|---|
| 1. Gap DOWN → first MOVE | 70, 60, 59, 80, 80 ms (largest 80 < LPT/2 = 200) | no raw output was kept. The multi-process MOVEs in `pre_hr1.mp4` arrive 25-100 ms apart, which is consistent | PASS (as reported) |
| 2. First move | sheet moves with the first MOVE (read off a tile) | tracked by template matching against the Filters sheet at rest. The first frame after the DOWN shows the top **43.8 device px** lower (66 px first MOVE − 22 px slop = 44), then +12-13 px per MOVE | PASS (confirmed) |
| 3a. Dim at Medium | `point_dim` 540 551, `close_sheet` closed it | `safe_target.py --dim` on the saved dumps (`f_med.xml`, `cat_filters_open.xml`): 540 551, exit 0 | PASS |
| 3b. Dim at Large | exit 3 "no sheet" (expected exit 2), nothing pressed, the sheet stayed open | confirmed on `cat_basic_open.xml` and `f_large.xml`. Root cause below | **FAIL** |
| 3c. No sheet | exit 3, nothing pressed | confirmed on `cat_sheets_start.xml` | PASS |
| 4a. Label, no sheet | 130 1352 | confirmed | PASS |
| 4b. Label behind an open sheet | exit 1 "not found" (expected exit 2) | confirmed: with a sheet open, the page behind is absent from the dump, so no tap is possible | safe. The procedure should accept "not found" |

**Why 3b fails, and why it matters for District.**

- Compose reports a node that is partly covered by later nodes with only its *uncovered* bounds. At Large, the
  template's `sheet_scrim` ("Close sheet", clickable) is therefore reported as `[0,0][1080,128]`, the strip above the
  sheet. At Medium it is `[0,0][1080,1200]`, exactly half the window.
- `safe_target.py`'s `is_scrim()` requires at least half the window's area. So any Compose sheet taller than half the
  window reads as "no sheet". Čeština (top at 1010 px, 42 %) gave exit 3 on all six S2 tries for the same reason.
- If District's sheets are Compose (Material 3's sheet labels its scrim "Close sheet" the same way), every tall
  District sheet would make `close_sheet` press nothing. The run would then stop under §9. That fails safe, but it
  ends the run.
- Views' `touch_outside` reports full bounds and would pass.
- The fix belongs to the lead and is outlined in §2.13.

### 2.4 The operator's report, checked

- **Correct:**
  - device facts and settings, unchanged at the end;
  - District never touched; compilation states;
  - build `722ced37` (the branch head is that commit);
  - capture lists and file names;
  - Places non-modal; no Collections section;
  - the pre-flight outcomes (§2.3).
- **Wrong: "Android 13, not the custom Android 17 ROM".** `ro.build.version.release` is 17, SDK 37, and the product
  is custom_sweet (`phone/logs/an_device_props.txt`). The MIUI 13 string is the vendor fingerprint, which custom
  ROMs keep. The global note about the phone is right.
- **The earlier analysis pass, corrected:**
  - **Light-theme S1 tracks:** their first rows were false matches (match cost 11-12). Cleaned with a cost cut
    (4.0); the light fits now agree with dark (frame t90 217 ms in both).
  - **Sheet fits (`lib/fit_rest.py`), corrected in this revision.** The search's grid stepped ω by 1.0 and then
    refined ζ by only ±0.02, so it never reached ζ 1 from its coarse pick. The closes stuck in a local optimum
    (Filters: ζ 0.94-0.96 with the hidden sheet's top at 1574-1592 recorded px, 8-26 px above the screen's bottom
    edge, where it cannot sit). That gave model t90 196-201 ms, labelled "ζ 1.0, k 339" because the median was rounded to one
    decimal. The fixed search (grid, then a Nelder-Mead polish of ζ, ω and t0, with the hidden top bounded at the
    edge) finds every close critically damped. Filters: k 342, rest 1600 px, RMS 1.10 px against the earlier
    1.26-1.48 px, model t90 211 ms, which is the code's `sheet` spring (k 341.5, 210 ms on paper). The opens moved
    less: Filters frame t90 211 → 217 ms, Snappy 190 → 184 ms. Most earlier opens also had the hidden top 4-34 px
    above the edge.
  - **Basic's close from Large, frame read:** 208 → 216 ms. Its 90 % threshold is 10 % of the travel short of the
    rest. With the rest at the edge (1600 px), not 1592 px, the threshold moves 7 px down, and the frame that crossed
    it before (1442 px) now falls short, so the read lands one frame later.
  - **First-frame misses:** the earlier text called them "the render thread and GPU waking from idle" and then
    counted the list-sheet opening miss beside them as a second kind. That opening frame is itself one of the first
    frames. §2.8 now splits them by the app's work on that frame.
  - **S7 and S8 fits:** they started from the recording's static first frame (t = 0), so every value was wrong
    (t50 750-1000 ms, overshoots of 1,000 %+). Redone with the lift found on the track.
  - **T2 pop track:** wrong. The tracker's 3-px coarse search missed the true match on a text-dense band and locked
    onto the screen's right edge (k 193 "ambiguous"). Re-tracked with a full search: k 334 (§2.6).
  - **S11 track:** wrong. Its reference band was taken from the last frame, where the sheet had already gone.
    Re-tracked against a frame of the open sheet: the catch works (§2.5).
  - **Dim numbers:** `tools/patch_luma.py` does not average the patch (it shrinks 576 × 144 px to 1 px with
    swscale's area filter). It read alpha 0.646 dark; a true pixel mean reads **0.567**.
  - **S1 Places:**
    - Only run 1 opened the sheet. The cleanup between runs pressed Back, which left the Sheets page with the
      non-modal sheet on it, so runs 2-3 tapped the catalog's start page and nothing opened.
    - The three Places timing dumps hold 0 frames.
    - S3 Places is a page pop, not a sheet close.
- **Caveats that stand:**
  - the catalog draws a frame-time readout every 20 frames (it is in every catalog dump; the sample app has none);
  - the ROM draws its own "120" meter (system-drawn, not in the app's counters);
  - a Gradle build ran on the PC (no effect on the phone).
- **For the lead: `phone/lib/sheet_scenes.sh` must not be reused on District as written.** On `point_dim` exit 3 its
  S2 branch presses Back without first checking that a sheet is open. That is harmless on the catalog but against §1
  on District.

### 2.5 Sheets (catalog, dark, Standard unless named)

Every S1/S2/S3 track was checked per §7.1: five crops, ±40 px; the edge sat on the mark in each
(`checks/an2/*_crops.png`). Sheets were tracked by template matching a header band inside the sheet
(`lib/band_track.py`), because `edge_track.py` lost the faint dark edge. S1, S2 and S3 are fitted with the corrected
`lib/fit_rest.py` (`fits/an3/S1_opens.json`, `S23_closes.json`); "ζ 1 fit" is the best critically damped spring on
the same track, given beside the free fit.

| Scene | O (template) | D |
|---|---|---|
| **S1 open**, Filters (two stops, list inside, 436 dp) | frame t50 92, **t90 217**, t99 359 ms (model 92 / 211 / 358). Critically damped: ζ 0.994, k 335 with the start on the edge bound; the ζ 1 fit gives k 340 at RMS 0.87 against 0.85 px. Best tween 3.3 px. No overshoot. Slides up | n/m |
| S1, other sheets | frame t50 / t90 / t99 ms. Light Filters 92 / 217 / 359 (k 339, ζ 1 fit 341). **Snappy** Filters 84 / **184** / 317 (k 448, ζ 1 fit 450). Basic, Large (830 dp) 92 / 217 / 367 (k 340). Several languages 93 / 218 / 360 (ζ 1 fit k 341). Now playing 92 / 217 / 367 (k 339). **Dialog-like**: Photo actions 74 / **174** / 299 (k 527, ζ 1 fit 521), Čeština 75 / 175 / 292 (k 522). Places, non-modal (1 run, short travel, shape loosely pinned): ζ 1 fit k 347, t90 216. All springs at ζ 0.99-1.01, none overshoot; best tweens 2.6-6.3 px against springs 0.5-1.1 px | n/m |
| **S2 dim tap / S3 Back**, Filters | the same motion for both, frame for frame. **t50 91 ms**. The sheet leaves the screen before 90 %, so t90 comes from the model: **211 ms**, t99 360 ms. Critically damped: ζ 1.00-1.01, k 342 (ζ 1 fit k 341), rest 1600-1605 px (at or just below the screen edge), RMS 1.10-1.13 px; best tween 10.1 px | n/m |
| S2/S3, other sheets | light: t50 91, model t90 211 (k 341). Snappy: t50 83, model t90 184 (k 454, ζ 1 fit 452). Now playing: 91 / 212 (k 341-345). One Back run has a double step mid-motion (a frame missing from the recording or skipped on screen) that the re-timing onto the vsync grid cannot place, so it fits badly (RMS 8.6 px); the other two give k 345. Čeština (Back, and Back after the refused dim): 91 / 211 (k 342). Several languages: 91 / 211 (k 342). Basic from Large (826 dp), the only close whose 90 % point is on screen: frame t50 91, **frame t90 216** (model 211, k 343, ζ 1 fit 341). Photo actions: t50 100 (shorter travel, so the read falls a frame later), model t90 215 (k 341, ζ 1.01; ζ 1 fit k 339, 212). Photo actions and Čeština open on `dialog` (frame t90 174-175) but close on `sheet` (model 211-215), so they close about 40 ms slower than they open | n/m |
| **Dim at rest** (true pixel mean of a flat card, black scrim) | **dark 0.567** (token 0x8F = 0.561), **light 0.406** against the near-black scrim colour (token 0.40), Snappy 0.568 | n/m |
| S12 dim during motion | follows the sheet's position frame by frame: dim share = travel share within 0.03 on open and close | n/m |
| S4 predictive back | the sheet shrinks under the gesture and returns on cancel; on commit it slides down and closes | n/m |
| S5 snap back after a held drag (no release speed) | Filters (82 dp): 92 / 208 / 350 ms. Basic (179 dp): 100 / 217 / 367 ms. Spring k 339-340, no overshoot | n/m |
| S6 70 % drag, still lift | closed 2 / 2 (past the 48 % position line) | n/m |
| **S7 fling down 25 %** (727 / 1456 / 2730 dp/s at 300 / 150 / 80 ms) | **Filters (Medium):** 80 ms dismissed 3 / 3, speed kept (2-frame carry 0.69-0.70; fitted start speed 0.89-0.96 of the finger). 150 and 300 ms: 1 of 3 dismissed, 2 returned. **Basic (Large):** 150 and 300 ms returned 6 / 6; 80 ms dismissed 3 / 3 (carry 0.68-0.89). Returns are the `sheet` spring (k 339) starting at 0.64-0.69 of the finger's speed: the sheet travels on two frames before turning; t90 221-229 ms | n/m |
| S8 fling up to Large | Large reached 9 / 9, no overshoot. t90 from the lift 196 / 171-206 / 154 ms at 300 / 150 / 80 ms. Speed kept (carry 0.8-1.5; above 1 when the spring adds speed toward a far stop) | n/m |
| S9 pull above Large | 6 dp of movement for about 79 dp of finger past slop: the band's room above Large is the gap to the status bar, about 28 px (10 dp). Returns on lift | n/m |
| S10 list inside | a slow upward list drag grows the sheet to Large before the list scrolls | n/m |
| S11 catch while opening | the touch stopped the sheet at 86 % and 93 % of its open travel. The drag continued from there with no jump, and the quick lift dismissed it | n/m |
| S13 shape | Utility style: inset with all corners rounded at Medium; full width at Large | n/m |
| Classification | every sheet motion is a critically damped spring, on the code's tokens: k 334-345 `sheet` (341.5), 448-455 Snappy (451), 515-532 `dialog` (522). Stiffness stays the same across release speeds, and the speed is carried | n/m |

Why S7's split is not a defect:

- the 25 % scripted swipe moves a Medium sheet about 49 % of the way to hidden, right on the 48 % dismiss-position
  line (`SheetReleaseRule.kt`);
- closing by speed alone needs two sheet heights per second (`dismissFlick` 2.0), which only the 80 ms swipe
  reaches.

### 2.6 Screen transitions

| Scene | O | D |
|---|---|---|
| T1 push (sample app, list → detail) | frame t50 98, **t90 231**, t99 373 ms (model 92 / 213 / 365). Spring ζ 1.01, k 343 (runs 342-359; ζ 1 fit 332-346) (`screen` k 334) | n/m |
| T2 Back-key pop | frame t50 100, **t90 215** ms (model 92 / 214). Spring k 334 (runs 310-345, ζ 0.95-1.01; ζ 1 fit 332-339). Runs 2 and 3 are noisier (RMS 3.5-3.9 px against 0.8) | n/m |
| T3 back gesture | the detail page shrinks into a card under the finger and returns on cancel; on commit the list replaces it. Frame timing of the scripted drags is not valid (one MOVE per process, about 50 ms apart): the 20 % janky and 785 ms/s hitch on "cancel" are that artefact. Flick: janky 2.6 %, p90 10.8 ms | n/m |
| T4 page swipe (Bars pager, 160 dp) | **150 ms flick** (the finger moves 275 dp in 150 ms, 1833 dp/s): lands from 89 dp, t50 50, **t90 142**, t99 283 ms, starting at 1441 dp/s, **0.79 of the finger's speed** (runs 0.77-0.83; kept by §7.2's ≥ 0.6). **300 ms** (916 dp/s): from 94 dp, 67 / **183** / 325 ms, starting at 927 dp/s (1.01, the finger's). Springs near ζ 1: median k 324 (150 ms) and 320 (300 ms), runs 306-342. The 150 ms run 3 fits overdamped (ζ 2.0, k 4147) at the same RMS (0.28 px): a landing under 100 dp does not pin the shape (§7.2). 80 ms: not resolved (the page changed within two frames). Slow drag: 2 runs recorded, not fitted | n/m |
| T5 tab switch | content swaps in one frame; the indicator moves | n/m |

### 2.7 Feed, header, press, loading

| Scene | O | D |
|---|---|---|
| F1 fling on "Lists" (40 % at 300 / 150 / 80 ms = 1162 / 2324 / 4378 dp/s) | the coast keeps **0.9980 of its speed per ms** at 300 and 150 ms (iOS normal 0.998; RMS 1.0-3.2 px). 80 ms: 0.9976-0.9978, a weaker fit (only about 265 ms before the list's end). The fling starts at the finger's speed (carry 0.95-1.02; 0.72 at 80 ms). It bounces 17 dp (150 ms) and 45 dp (80 ms) at the end | n/m |
| F2 pull at the top (25 %) | content follows about 102 dp for about 210 dp of finger, then returns (rubber band). An 80 ms swipe into the top shows 112 dp and returns | n/m |
| F2b "Edges" list | a stretch effect: not measurable by shift tracking | n/m |
| F3 carousel, F4 centred card | not in this build / skipped | n/m |
| H1 large title | the large title moves with the scroll; the inline title appears past a threshold (frames) | n/m |
| P1 press and slide off | recorded (3 controls × 3); not measured further. District's P1 is by hand only | n/m |
| P2 loading | skeleton rows with one band sweeping across (frames) | n/m |

### 2.8 Frame timing per window (timing passes, nothing recording; `tools/frame_summary.py`)

All template scenes draw in one window, the activity's (template sheets are not dialog windows). The refresh
interval is 8.33 ms everywhere. "Janky" = missed the frame deadline. "Opening" = the slowest UI-thread frame of the
first three. "Tap → present" = from the input frame's vsync to the present of the opening frame. Medians of 3 runs.

| Scene (O) | Janky % | Frame p50 / p90 / p99 ms | GPU p90 | Hitch ms/s | Opening UI ms | Tap → present ms |
|---|---|---|---|---|---|---|
| S1 Filters | 1.43 | 10.2 / 10.8 / 24.6 | 7.4 | 0 | **16.4** (16.2-20.0) | **46.0** |
| S1 Filters light / Snappy | 1.43 / 1.59 | 10.1 / 10.4 / 24.1 ; 10.3 / 10.9 / 24.4 | 6.6 / 7.0 | 0 | 18.7 / 16.6 | 46.2 / 46.1 |
| S1 Basic (Large) | 1.37 | 10.4 / 10.9 / 28.7 | 7.2 | 0 | **20.5** | **54.4** |
| S1 Čeština / Several languages | 1.56 / 1.43 | 9.4 / 9.8 / 27.0 ; 10.0 / 10.5 / 24.7 | 5.4 / 7.0 | 0 | **24.2** / 15.4 | 46.2 / 46.1 |
| S1 Photo actions / Now playing | 0 / 1.43 | 9.8 / 10.7 / 13.1 ; 10.4 / 11.5 / 14.1 | 5.4 / 7.4 | 0 | **7.0** / **4.2** | **37.6** / **37.8** |
| S2 dim close (Filters, Actions, Now playing, light, Snappy) | 1.43-1.61 | 7.4-8.6 / 9.3-10.9 / 17.4-23.7 | 6.1-6.7 | 0 | 1.5-3.1 | 37.7-46.2 |
| S3 Back close (6 sheets) | 1.35-1.45 | 8.2-8.7 / 9.2-18.2 / 17.2-22.4 | 5.7-10.9 | 0 | 1.4-3.9 | 37.6-46.2 |
| S7 fling 150 ms (Filters / Basic) | 1.25 / 1.25 | 9.4 / 10.3 / 13.7 ; 10.1 / 11.1 / 19.8 | 6.3 / 6.7 | 0 | 1.8 / 1.6 | 37.6 / 38.0 |
| S10 list slow up | 0 | 9.5 / 11.3 / 12.7 | 7.5 | 0 | — | — |
| T1 push | 1.59 | 11.5 / **30.2** / 33.8 | **14.5** | 16.2 | 14.5 | 54.4 |
| T2 Back pop | 0 | 12.0 / 18.9 / 19.6 | 12.0 | 0 | 1.1 | — |
| T3 flick (1 run) | 2.63 | 8.5 / 10.8 / 15.0 | 6.9 | 52.6 | 2.1 | 37.7 |
| T4 pager 150 ms | 1.54 | 6.7 / 7.8 / 17.1 | 4.5 | 0 | 1.6 | 37.8 |
| T5 tabs (to each of 5) | 1.15 | 9.0-10.2 / 9.5-19.5 / 17.6-27.3 | 5.9-11.1 | 0 | 3.4-9.3 | 37.7-46.1 |
| F1 fling 150 ms | **0** | 6.4 / 7.3 / 8.3 | 3.4 | 8.3 | — | — |
| H1 slow scroll (1 run) | 0 | 8.2 / 8.8 / 11.7 | 3.2 | 24.8 | 2.5 | 29.4 |

**Pooled:**

- **Catalog** (74 dumps, 5,417 frames): janky 1.14 %, frame p50 / p90 / p99 8.8 / 10.7 / 23.2 ms, UI p99 3.3 ms;
  no frame over 43 ms, no UI frame over 25 ms.
  - **59 of its 62 missed frames are the first frame of an interaction** (the first row of a dump taken after one
    scripted interaction). They split by the app's own work on that frame (`lib/an3_frames.py`,
    `logs/an3_frames.log`):
    - **18 are list-sheet openings** (S1 of Filters, light, Snappy, Basic, Čeština and Several languages: all 18 of
      their runs). UI 13.5-24.2 ms: animation phase 1.8-2.5 ms, traversal (measure and layout) 10.0-16.1 ms, draw
      1.7-6.3 ms. This is app work, the P3 finding.
    - **41 are the render thread and GPU after idle**: UI 0.8-4.8 ms (30 of them under 3 ms), yet the frame
      completed late.
  - The other 3 misses are mid-motion, each with UI under 2.1 ms. Without the first frames, janky is 0.06 %
    (3 of 5,343).
- **Sample app** (22 dumps, 1,721 frames): janky 1.22 %, 9.5 / 19.0 / 30.5 ms.
  - 19 of its 21 misses are first frames. **7 are app work**: the three T1 pushes (UI 14.5, 15.1 and 7.3 ms, of
    which 5.1-10.1 ms is the draw phase) and four tab switches (UI 8.8-10.1 ms); 6 of the 7 are over one 8.33 ms
    frame. **12 are start-up after idle** (UI 2.1-4.7 ms). The other 2 are mid-motion (UI 0.8-1.3 ms). Without the
    first frames, janky is 0.12 % (2 of about 1,700).
  - Push and pop are GPU-bound (GPU p90 12-14.5 ms against the 8.33 ms frame) but stay on cadence.
  - This is known: SHEET-SPEC D-17.
- **Android's own counters** (the process-wide "Janky frames" and HISTOGRAM lines of each dump, summed; the measure
  District's row below uses): catalog **84 / 6,239 = 1.35 %**, frame p50 / p90 / p99 **8 / 10 / 22 ms**; sample app
  **24 / 1,725 = 1.39 %**, **9 / 17 / 24 ms**. District: 54 / 749 = 7.21 %, 10 / 24 / 200 ms.

**District, the only numbers on this phone.** Window: District's home activity, one window; the 8 frames of another
window are no longer in the dump. All from the session that started 20:31:51, 2.5 min after install, uncompiled:

| | Frames | Janky % | Frame p50 / p90 / p99 ms | GPU p90 | Other |
|---|---|---|---|---|---|
| Whole session (Android's counters, flagged frames included) | 749 in about 12 s | **7.21** | 10 / **24** / **200** (p95 40) | 10 ms | 20 frames in the histogram's buckets from 150 ms up; "slow UI thread" 46, "slow issue draw commands" 52 |
| Last 120 frames (7.8-11.7 s after start), Flags 0 | 120 | **15.83** | 17.8 / **45.5** / **304** | 12.1 ms | 9 frames over 100 ms; hitch 258 ms/s; **4 UI-thread stalls**: one of 180 ms in the animation phase (178 ms), three of 197, 214 and 225 ms in measure/layout (traversal 195, 211 and 221 ms); the 197 ms one carried input |

### 2.9 What makes the comparison unfair

1. **Compilation**: District `verify`, so the first-run path with no compiled code. The template was forced to
   `speed-profile` by the run. This weighs on frame cost (jank, opening frames), not on animation tempo (springs run
   on time).
2. **First launch against warm**: District's counters are its first 12 seconds after install: cold caches, network
   loading, first layout of every view. No template cold start was timed.
3. **Content**: District shows live network content; the catalog shows local sample data.
4. **Touches**: District's frames come from your own touches; the template's from scripts.
5. **Frame deadlines**: District's FrameDeadline sits a median 30.3 ms after IntendedVsync (p10-p90 21.9-38.7 ms),
   the catalog's 22.1 ms (13.7-30.4 ms). District's janky % is counted against looser deadlines, so if anything it
   understates District's jank.
6. **Same in both**: the phone, 120 Hz, the Skia (OpenGL) pipeline, thermal status 0, battery saver off.
7. The catalog's frame-time readout adds a small redraw every 20 frames: against the template, not for it.

### 2.10 RESEARCH.md §2.6, row by row

| Measurement | D | O | Result |
|---|---|---|---|
| Sheet open from rest (S1): t90 | n/m | frame read 217 ms (Filters, Standard; model 211); 184 Snappy; 174-175 dialog-like | **undecided**. With O = 217: P2 if D ≤ 167 ms, a side-by-side if 167-197 ms, drop if D ≥ 197 ms. The lines carry about ±5 ms of fit uncertainty (the unbounded fit read 211), so District's opens must go through the same bounded `lib/fit_rest.py` |
| Close without a drag (S2, S3): t90 | n/m | critically damped, k ≈ 342 (medians per sheet 341-345), rest at the screen edge: model t90 211 ms (Standard closes 211-215, Snappy 184); frame t90 216 ms where the 90 % point is on screen (Basic from Large); frame t50 91 ms | **undecided**. Compare like with like. **Frame read** (District's 90 % point on screen) against O = 216: P1 if D ≤ 136 ms, a side-by-side if 136-186 ms, drop if D ≥ 186 ms. **Model read** (District's sheet also leaves the screen first) against O = 211: 131 / 181 ms |
| Opening frame (S1 timing) | n/m | list sheets: UI 13.5-24.2 ms per run (opening-frame medians 15.4-24.2), 10.0-16.1 ms of it in measure/layout; tap → present 46-54 ms. Light sheets: 4.2-7.0 ms, 37.6-37.8 ms | the District comparison is undecided. **Our own opening frame is over budget** → P3 changed (§2.11) |
| Dim alpha at rest (S2 patch) | n/m | 0.567 dark, 0.406 light | **undecided**. P4 if D is lighter by 0.05 or more |
| Modal surface during S1 | n/m | ours slides | **not triggered**: District's S1 was not recorded, so the District S1 pass decides P10 |
| Movie page panel (H2) | n/m | — | not found (not run) → P5 rests on the video: **you decide** |
| Page swipe (T4): landing t90 | n/m | 142 ms at 150 ms, 183 ms at 300 ms (Bars pager, not full-bleed) | **undecided** for P6 (b); P6 (a) unaffected |
| Home header (H1) | n/m | our large title follows the scroll; the inline title appears on a threshold | absent (not run) → P7 rests on the iPhone app: **you decide** |
| Release speed (S7, S8, T4): carry | n/m | kept: sheets 0.64-0.96 fitted, 0.69-0.89 over two frames on dismissals; pager 0.79 at the quick flick, 1.01 at the slower one | nothing to take whatever District does |
| Classification | n/m | springs throughout, stiffness constant across speeds | records only |

### 2.11 P1-P10

| # | Decision | With the numbers |
|---|---|---|
| P1 | **keep, pending** | O measured: the close is the `sheet` spring from rest, critically damped, k ≈ 342: t50 91 ms, model t90 211 ms (every Standard close within 211-215), frame t90 216 ms (Basic from Large, the only close whose 90 % point is on screen). Lines in §2.10: against a frame read, P1 if D ≤ 136 ms, a side-by-side if 136-186 ms, drop if D ≥ 186 ms; against a model read, 131 / 181 ms. One note for when it is decided: dialog-like sheets already open on `dialog` (frame t90 174-175 ms) but close on `sheet` (model t90 211-215 ms) |
| P2 | **keep, pending** | O: frame t90 217 ms (Standard; model 211), 184 ms (Snappy), 174-175 ms (dialog). Lines from O = 217: P2 if D ≤ 167 ms, a side-by-side if 167-197 ms, drop if D ≥ 197 ms, each with about ±5 ms of fit uncertainty; District's opens go through the same bounded `lib/fit_rest.py`. If District lands between 167 and 197 ms, consider whether the Snappy style (184 ms) already covers it before adding a token |
| P3 | **change** | Measured regardless of District: every list-sheet open (18 of 18 runs) misses its opening frame, with 13.5-24.2 ms of UI work (2-3 frames' worth), and it adds one vsync of tap → present (46 against 37.6 ms). That missed frame is one of §2.8's first-frame misses, the part that is app work (18 of the catalog's 59), not a second kind of miss; the other 41 are start-up after idle with UI under 5 ms. Where the time goes: about 2 ms (1.8-2.5) in the animation phase, where the sheet's content recomposes; **10.0-16.1 ms in the traversal** (measure and layout; per-sheet medians 11.3-15.7 ms), which is where a `LazyColumn` composes and measures its visible items; 1.7-6.3 ms in draw. Re-base P3 on our own budget (opening UI ≤ one 8.33 ms frame on the Redmi). The fix must cover the lazy items, not only the sheet slot: paused precomposition of the slot alone (SHEET-SPEC §20, on press-down) may leave the items' cost in the traversal. So precompose and premeasure the list's first screen of items on press-down, or keep the content (`keepContent`) for often-opened sheets. Judge success by the traversal share of the opening frame (now 10-16 ms), not the animation phase. Measure the fix with a real or held press: a scripted `input tap` has no gap between down and up, so precomposition cannot show in it. District's opening frame stays a cross-check once measured |
| P4 | **keep, pending** | O: 0.567 dark, 0.406 light (the tokens). Needs District's S2 patch read with a true mean |
| P5 | **keep as written** (your call) | H2 not run; the parts exist. The non-modal Places sheet opens on the `sheet` spring (t90 about 216 ms, 1 run, ζ 1 fit k 347) |
| P6 | **keep**: (a) as written; (b) pending | O landing t90 142 / 183 ms at 150 / 300 ms flicks, starting at 0.79 / 1.01 of the finger's speed (kept, by §7.2's ≥ 0.6), on the 160 dp pager, not a full-bleed one |
| P7 | **keep as written** (your call) | H1 not run on District; ours scrolls the large title and switches the inline title on a threshold |
| P8 | **keep** | needs no measurement |
| P9 | **keep, low priority** (your call) | iPhone evidence only; F4 skipped |
| P10 | **keep, pending** (not triggered) | District's S1 was never recorded, so RESEARCH's condition for dropping it ("if S1 shows nothing") was not met; the District S1 pass decides. Until then it stays deferred in SHEET-SPEC §20 phase 2. Nothing public shows District doing it on Android |

Nothing was dropped for "the template already does it better", because no District motion was measured to compare
against. Where the template's own numbers show strength (speed carried, catch, dim tied to position, iOS coast), no
proposal was asking to change it.

### 2.12 What could not be measured, and why

- **All District scenes**: pre-flight stop (§2.3). §6 (District's window and class names) was not run either. The
  workflow script's note that you allowed §6 could not be checked from here.
- **District's opening frame and tempo**: its counters hold no sheet; they cover only the first 12 s after launch.
- **Template**:
  - Places S1 runs 2-3 and all Places timings (the cleanup pressed Back);
  - Čeština S2 (dim refused, exit 3; filed as S3 dim fallback);
  - Basic S2 (Large only, same cause);
  - T4 at 80 ms (track lost);
  - F2b (stretch, not a shift);
  - F3 and F4 (not in this build / skipped);
  - frame timing of multi-process drags (T3 cancel / commit; S4-S6, S9 were not timed).
- **Touches**: no touch was made by hand; every touch was scripted, on the template only.

### 2.13 Tool problems found, and what the District pass needs

1. **`safe_target.py --dim`** (blocks the run).
   - **Problem:** `is_scrim()`'s "half the window" area test reads Compose's uncovered bounds, so it fails for any
     sheet taller than half the window.
   - **Proposed fix (lead):** relax the area test only for the sheet-specific scrims: the labels "Close sheet" and
     "Dismiss sheet", and the id `touch_outside`. Accept these at any reported area when a sheet container is drawn
     above them. Keep the area test for the generic labels in `SCRIM_LABELS` ("close", "dismiss"): without it, a
     small Close button sitting under a sheet container could pass as a scrim. Keep exit 2 for "band under 48 dp",
     so tall sheets close with Back after a fresh dump.
   - **Test:** add `cat_basic_open.xml` and `f_large.xml` as test dumps, then re-run the catalog pre-flight.
2. **`patch_luma.py`**: averages wrongly (0.646 against a true 0.567). Average the cropped pixels, as
   `phone/lib/patch_mean.py` does.
3. **`fit_motion.py` "from rest"**: screenrecord writes one static frame at t = 0, so "motion start" becomes 0 unless
   `--from` is given. A sheet rising from below the screen edge has no visible start at all: use a fitted start
   (`phone/lib/fit_rest.py`).
4. **`phone/lib/fit_rest.py`** (fixed in this revision, before any District close goes through it): its spring
   search could not reach ζ 1 from a coarse pick and let a hidden sheet's top sit above the screen edge (§2.4). It
   now polishes ζ, ω and t0 with Nelder-Mead from the five best grid cells, reports a ζ 1 fit beside the free one,
   and takes `--start-min` / `--rest-min` bounds. For District: opens with `--start-min 1600`, closes to hidden with
   `--rest-min 1600` (720 × 1600 recordings), as `lib/an3_refit.py` does for the template.
5. **`edge_track.py`**: lost the edge of dark sheets. Template matching of a band inside the surface
   (`phone/lib/band_track.py`) worked, and passed the §7.1 crop checks. Use `--coarse 1` on text-dense horizontal
   bands, and drop rows with match cost over 4.
6. **Before the District pass**:
   - your go-ahead to resume after the stop;
   - the fixes above, and a passing pre-flight;
   - preferably District compiled by the phone's own overnight maintenance (`dumpsys package` shows speed-profile;
     the run must never compile it), or its state recorded beside every number;
   - S2 run on sheets at Medium first.


## The user's decisions (Oct 20 08:36)

P3 approved (prepare a list sheet's list off-screen before it opens). The re-run measures, in order: filters and sort sheets, the location and city picker, movie and event detail sheets (never seats or booking), then any other sheet it can safely open. Other proposals wait for the re-run.
