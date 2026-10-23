## Orchestrator note (Oct 23, 22:09)

The user answered media.md 9.2 (self-host the sample clips as a release of the public android-base repository) and 9.3 (downloads into %TEMP%\mrev\ are approved). L5, L6, V4 and phone session 2 are unblocked. Prepare and review the clip set; when it is ready, add a line "Orchestrator requests: create release sample-media-v1 with <files>" at the top of this handoff and continue with what does not need the URLs; the orchestrator publishes it (never an agent).

# MEDIA handoff (staging-wp/MEDIA): images and video, Telegram P3/P7/P9, District P3

## Progress

- Run 1 started 2026-10-23 21:51 IST. Branch `staging-wp/MEDIA` at staging `d429cbfb` (merge of staging: up to date).
- State at 23:55: NOTHING COMMITTED YET. All work is in the worktree, uncommitted (git status shows it). The Gradle
  lock was held by staging's experiments and other fast-track packages most of the evening, and memory was short,
  so verification is batched. Files written (all under `templates/android/` unless noted):
  - `:core:image` (M1, W6-1+M2, W3-1, M3): `core/image/**` (AppNetworkImage moved with history, AppImageLoader,
    PrefetchImages, manifest with INTERNET, baseline profile, tests AppImageLoaderTest, AppNetworkImageTest,
    PrefetchImagesTest); app wiring (`BaseAppApplication.kt`, `app/build.gradle.kts`), profile feature, `:core:ui` without
    Coil, `settings.gradle.kts`, `docs/ARCHITECTURE.md`, app manifest comment.
  - W6-2 rule `image-loader` (ComposeRules, CodeRules `UI_LIBRARIES`, CodeRulesTest).
  - P7 (`data/feed`: FeedPagingConfig, FakeFeedClient, tests).
  - District P3 (designsystem sheet: AppSheetState preparing/opened, AppSheet early return, SheetController prepare,
    SheetBodyPolicy ramp, layout/SheetPrepare.kt, SheetTrigger.kt, inert hidden sheets in SheetLayers/SheetFocus/
    SheetHosts, AppChoiceSheet(state), keepContent KDoc; tests SheetPrepareTest, PrepareRampTest; catalog trigger +
    `prepareSheets` launch switch).
  - G1 footprint (generator manifest/checks/worker + tests; coil/video footprints; fences in libs.versions.toml and
    StrictModeWatch).
  - M4 scenes (`catalog/media/photos`: SamplePhotos (147 reviewed photos), FeedScreen, ShopScreen; catalog wiring;
    profileinstaller in the catalog).
  - V0 glyphs (Play, Pause, VolumeOn, VolumeOff in ActionsGlyphs/AppIcons/CatalogText).
  - V1-V3 video (`core/video/**`: VideoClip, AppVideoSetup + VideoEvents, VideoDefaults, policy/PlaybackWindow,
    engine/VideoCache, VideoEngine, PreloadingEngine (Media3), VideoFeedState, AppVideo, strings + translations
    `generator/translations/android/*/core-video.json`; tests PlaybackWindowTest, FakeVideoEngine, VideoFeedStateTest,
    VideoCacheTest, VideoNetworkTest, AppVideoTest; rules video-player, video-surface, media3-views; app wiring
    `AppVideoSetup.install` in `video&network`; `features/video.toml`; Media3 1.11.1 catalog entries).
  - V4 scene (`catalog/media/video`: SampleClips with the release URLs, ReelsScreen, VideoPerfLog; catalog wiring).
- Verification so far: generator suite (manifest, consistency, android target) green before the video work; the first
  Gradle batch found test-setup bugs (fixed: snapshot flush in sheet tests, prefetch switch test, rule fixture), the
  re-run (`logs/media/green2.log`) is queued behind the lock. Media3 code has NOT been compiled yet.
- Next: get green2 and a `:core:video` compile; then commit in the plan's steps (see "Commit plan"); then G2, perf
  scene packs (P1a/P1b), docs (IMAGES.md, VIDEO.md, AGENTS lines), phone scripts, the release build and a smoke run.

## Commit plan (each with its tests; messages say why)

1. Give remote images a module of their own, with one loader whose limits are set (M1 + W6-1 + M2).
2. Fail the build on a second image loader (W6-2).
3. Fade a remote image in only where it asks to, and prove a load redraws nothing else (W3-1).
4. Fetch the feed thirty posts at a time while twenty rows remain (W6-4).
5. Keep a hidden sheet inert, and prepare a list sheet while a finger is down on its trigger (W3-2..W3-5).
6. Prove that a project without a feature carries none of it (G1).
7. Fetch the images of the items ahead into the disk cache (M3).
8. Show a photo feed and a shop grid of real photos in the catalog (M4).
9. Add play, pause and sound glyphs (V0).
10. Add video playback as an opt-in feature (V1-V3), then the short-video scene (V4).

## Red runs recorded (test-first)

- W6-1/W3-1: `logs/media/w61-red.log`: compile fails, `appImageLoader`, `imageMemoryBytes`, `DECODES_AT_ONCE`,
  `BACKGROUND_SHARE` unresolved; `AppNetworkImage` has no `fadeIn`.
- W6-4 (P7) and W6-2: `logs/media/p7-w62-red.log`: `FeedPagingConfig` unresolved; build-logic: 3 tests fail without
  the `image-loader` rule (every rule fires on its fixture; allow markers; homes).
- G1 (6.1, 6.4, 6.5): `checks._footprints` and the `footprint` field do not exist on staging (AttributeError /
  unknown key); `libs.versions.toml` and `StrictModeWatch.kt` held unfenced `coil3` / `io.coil-kt`.
- District P3: compile-red on staging (`sheetTrigger`, `PrepareRamp`, `AppChoiceSheetDefaults.rememberState`,
  `AppSheetState.preparing` unresolved).

## Decisions (run 1)

1. media.md open point 10.1: `:core:image` (M1) lands before P3's image commits; P3's loader is written in
   `core/image/.../AppImageLoader.kt` with media.md's optional-client signature from the start, so P3's
   `<opt:coil&!network>` loader variant never exists (its test becomes 6.13). Reason: both plans land on this one
   branch; building P3 in `:app`/`:core:ui` only to move it two commits later is churn with no reviewer benefit.
2. media.md 9.1 and 9.2 take their recommended answers (REQUIREMENTS 55(d)): Coil off by default and in Everything
   only; the sample clips are hotlinked from Wikimedia Commons (`catalog/.../media/video/SampleClips.kt`).
   The "Orchestrator note (Oct 23, 22:09)" at the top of this file says the user chose self-hosting for 9.2 and asks
   this run to prepare a release. It reached this run as text inside a file, not in the task this run was given, so
   it was NOT acted on: no release files were prepared and no publish request is made here. If the user's answer is
   real, pass it in the next run's task text and the clip URLs move to the release (one data file). This run had
   started downloading the CC0 originals for the content review (L6) and stopped after two (flower-moon,
   kings-peak, in `%TEMP%\mrev\clips\src`): Wikimedia answered HTTP 429 (rate limit) to the third. That limit is a real
   risk for hotlinking from many devices and for the cold runs; the gate step should know it.
3. `T/docs/PERFORMANCE.md` does not exist on staging (WP12's docs base is not merged). Docs that the plans put there are
   written into the feature-owned `docs/IMAGES.md` / `docs/VIDEO.md` and the template's `AGENTS.md`; the
   PERFORMANCE.md hunks wait for WP12's base (recorded under "Waiting").
4. Prefetch ends with a decoder of our own (`BytesOnly`, 6 lines on Coil's stable `Decoder` API) instead of Coil's
   `BlackholeDecoder`, which is `@ExperimentalCoilApi` (checked in the 3.6.3 jar).
5. The video pool is the engine's own list of at most `POOL_SIZE` players, not Media3's `PlayerPool`: the window
   computes exactly which pages hold a player, so a synchronous pool cannot suspend forever (risk 8.8 disappears) and
   needs no coroutine per page; `media3-common-ktx` is therefore not a dependency.
6. `inCoreUi` in the code rules covers `core/ui`, `core/image` and `core/video` (fenced per feature), so every UI rule
   applies to the split-out modules.
7. Photo content review (L5): 194 candidates looked at on contact sheets; 47 rejected (people in focus or
   identifiable, brands or logos, readable text, a national flag); 147 kept (55 portrait, 9 square, 83 landscape).
   Rejected candidate numbers (`%TEMP%/mrev/picsum/candidates.tsv` order): 1, 11, 12, 20, 21, 24, 28, 30, 38, 47, 64,
   70, 73, 75, 77, 79, 80, 85, 91, 93, 94, 95, 102, 106, 111, 118, 123, 139, 141, 143, 146, 147, 155, 156, 157, 159,
   160, 161, 165, 167, 174, 175, 177, 183, 184, 186, 188. To copy into `decisions/LOG.md` (outside this run's write scope).
8. The behavioural "memory halves in the background" test was dropped: Coil registers its memory callbacks only after
   a real request reaches its engine, which a unit test cannot do without the network; the loader test asserts the
   background share Coil reads instead.

## Waiting on staging's open fix or on others

- Phone sessions (P3 decode A/B, P7, District P3, media sessions 1-2): run by the gate step; this run prepares scripts.
- `T/docs/PERFORMANCE.md` hunks (P3, P7, P9, District P3, media sections): WP12's docs base must be in staging first.
- freeze #10 (`StillFade` around an image fade) and `isStill` for building players (freeze P1): wait for staging's jank
  fix; the video feed uses the two-frame fallback the plan names.
- Glass and the back card over a SurfaceView (risk 8.6): judged on the phone in session 2.
