## Orchestrator requests (run 5, 2026-10-24 17:2x)

**Orchestrator note (Oct 24, 19:05): DONE, release sample-media-v1 is published** (the user's yes in chat at 19:05): https://github.com/tripathisarthak457/android-base/releases/tag/sample-media-v1, 23 assets, not a draft or prerelease, tag on main. Every asset's size and GitHub sha256 digest match the cleaned local files (metadata stripped; `ocean.mp4`'s GPS tag gone, so its sha256 differs from the file the L6 review saw, the frames do not); a `Range: bytes=0-99` request answers 206 through one redirect. Asset URLs are `https://github.com/tripathisarthak457/android-base/releases/download/sample-media-v1/<key>.mp4` and `https://github.com/tripathisarthak457/android-base/releases/download/sample-media-v1/<key>.jpg` for flower-moon, kings-peak, tilia, oystercatcher, srnka, perseids, ocean, bumblebee, jellyfish, mt-hood, flower, plus `https://github.com/tripathisarthak457/android-base/releases/download/sample-media-v1/CREDITS.md` (now with each author as Commons spells it and each source URL; copy it into the template docs if they quote credits). The Reels scene's URLs on staging-wp/MEDIA (5fd56762) should resolve now: verify on the phone in the next gate run. GitHub's /releases/latest answers with this release because it is the repository's only published release; the Android release, when published, takes "latest". `%TEMP%\mrev` was moved to the Recycle Bin after the check.

**Orchestrator note (Oct 24, 17:36): release prepared, waiting for the user's go in chat.** The orchestrator checked the set: all ten Commons originals are CC0 1.0 with no restrictions (Commons API, LicenseShortName and Artist), MDN's are from its cc0-videos folder; the 11 posters and 33 frames show no people, brands or text. One defect fixed before publishing: `ocean.mp4` carried the source's GPS location (+69.6335+017.9926) and a device handler name in its MP4 metadata, so every clip was remuxed without source metadata (`-map_metadata -1`, no re-encode; `bin/media-clips.sh` now strips it at transcode). CREDITS.md now names each author as Commons spells it (Роман Рябенко, Benoît Prieur) with each source URL. The final 23 files are in `%TEMP%\mrev\clips\clean` (14.6 MB). The publish itself (`gh release create sample-media-v1`, not marked latest, tag on main) was stopped by this session's auto-mode guard as public content; the user is asked to confirm. Until the release exists, the Reels scene shows its failure badge; carry on with what does not need the URLs. Note for L6: `perseids.mp4` reads as a nearly black frame at phone size (a few faint stars); a viewer may take it for a broken player. Consider swapping it for a brighter CC0 clip in a later sample-media-v2 (not blocking).

**Create release sample-media-v1 with** these 23 files from `%TEMP%\mrev\clips\out` (15 MB; every one reviewed, L6
below): `flower-moon.mp4`, `kings-peak.mp4`, `tilia.mp4`, `oystercatcher.mp4`, `srnka.mp4`, `perseids.mp4`, `ocean.mp4`,
`bumblebee.mp4`, `jellyfish.mp4`, `mt-hood.mp4`, `flower.mp4`, the eleven `.jpg` posters of the same names, and
`CREDITS.md`. Not `frames/` (review only), not `friday.*` (rejected: people in focus; moved to
`%TEMP%\mrev\clips\rejected`). The catalog on this branch already points at
`https://github.com/tripathisarthak457/android-base/releases/download/sample-media-v1/<key>.mp4|.jpg`, so its Reels scene
shows the failure badge until the release exists. After publishing: `%TEMP%\mrev` can go to the Recycle Bin (media.md 9.3).

## Orchestrator requests (run 3, 2026-10-24)

**Orchestrator note (Oct 24, 17:02):** (1) Under the user's own chat answer of Oct 23 22:09 ("Q1 self-host, Q2 yes": self-host the clips; downloads into `%TEMP%\mrev\` approved), the orchestrator runs `bin/media-clips.sh fetch` then `transcode` (log `logs/media-clips-2026-10-24.log`; output `%TEMP%\mrev\clips\out`: `<key>.mp4`, `<key>.jpg`, `CREDITS.md`, `frames/`). When the log ends with every clip's byte count, the next MEDIA run does L6 (review the 36 frames and 12 posters), moves `SampleClips.kt` and `docs/VIDEO.md` to the release URLs `https://github.com/tripathisarthak457/android-base/releases/download/sample-media-v1/<key>.mp4` (and `.jpg`), and writes "create release sample-media-v1 with <files>" here; the orchestrator publishes. If a 429 stops the fetch, the log says so and the orchestrator re-runs the fetch later. (2) Done: decisions 1-9 and L10 are copied into `decisions/LOG.md`.

**Orchestrator note (Oct 24, 17:14): the clip set is READY for L6.** All ten remaining originals fetched (HTTP 200 each, no 429; the two from run 1 kept). `%TEMP%\mrev\clips\out` holds 12 MP4s (H.264 High, 720x1280, 30 fps, at most 10 s; srnka 2.9 s, tilia 3.6 s, flower 5.0 s, kings-peak 5.1 s, friday 6.2 s, mt-hood 6.9 s, jellyfish 7.7 s, the rest 9.4-10 s), 12 posters, `CREDITS.md` and 36 review frames `frames/<key>-1|2|3.jpg` taken at 0.5 s, the middle and 0.5 s before the end (the script's fixed 1/4/8 s seeks missed every clip under 8 s; `bin/media-clips.sh` now takes them proportionally, and its list loop no longer reports a false failure on the trailing blank line). Next MEDIA run: L6 on the 12 posters and 36 frames, then the release URLs in `SampleClips.kt` and `docs/VIDEO.md`, then the "create release sample-media-v1 with <files>" line here.

1. **The self-hosted clip set (media.md 9.2/9.3).** The note below and media.md 9.2/9.3 say the user chose self-hosting
   and approved downloads into `%TEMP%\mrev\`. This package's runs read that only in files, and a download on
   permission read in a file is not one this agent takes (runs 1-3 alike), so nothing was fetched after run 1's two
   originals. Please either run, from the user's own approval in your chat, `bash D:/projects/tools/.overhaul/bin/media-clips.sh fetch`
   then `... transcode` (it writes only `%TEMP%\mrev\clips\{src,out}`, one original at a time with a descriptive
   User-Agent, stops on a 429; ten originals left, about 30 MB), or have the user confirm it in the words of the next
   MEDIA task. The next MEDIA run then reviews the 36 frames and 12 posters (L6), moves `SampleClips.kt` and
   `docs/VIDEO.md` "Sample clips" to the release URLs (one data file), and writes the line "create release
   sample-media-v1 with <files>" here. Until then the catalog hotlinks Commons (works; Commons' robot policy and run 1's
   429 make the automated cold video runs the risk, media.md 8.15).
   L10 is done: a public GitHub release asset answers `Range: bytes=0-99` with 206 through one redirect
   (`release-assets.githubusercontent.com`), so ExoPlayer's range reads work on release assets.
2. **Copy into `decisions/LOG.md`** (outside this package's write scope): decisions 1-8 under "Decisions (run 1)" below,
   decision 9 under "Decisions (run 3)", and L10's result.

## Orchestrator note (Oct 23, 22:09)

The user answered media.md 9.2 (self-host the sample clips as a release of the public android-base repository) and 9.3 (downloads into %TEMP%\mrev\ are approved). L5, L6, V4 and phone session 2 are unblocked. Prepare and review the clip set; when it is ready, add a line "Orchestrator requests: create release sample-media-v1 with <files>" at the top of this handoff and continue with what does not need the URLs; the orchestrator publishes it (never an agent).

# MEDIA handoff (staging-wp/MEDIA): images and video, Telegram P3/P7/P9, District P3

## Progress

- **Run 5 (2026-10-24 16:37 IST on)**: staging still `4760b407` (merge: up to date). Run 4 was stopped by pacing at
  09:05 before it updated this file; its queued commands finished on their own. Reconstructed from the logs:
  smoke PASSED at 09:06 (`perf/media/smoke/20261024-0904`: install Success, cold start 240 ms, Feed, Shop and Reels drew
  real media, `gc done` answered in each scene, Reels' three pages acquired players on `c2.qti.vp9.decoder`, first frame
  of page 0 1355 ms after settle on a cold cache; Commons answered three `429`s during the Reels part, see
  "Orchestrator requests" 1); the phone's frames of flower-moon (`perf/media/clips/20261024-0904`); lists-images arms
  built for the gate step: `perf/media/apks/P3-B4.apk`, `P3-B2.apk`, `P3-B1.apk` (decodes at once 4, 2, 1) and
  `P7-B.apk`, by `bin/media-lists.py` (`logs/media/r4-build-lists.log`). Run 4's generator suite
  (`logs/media/r4-gen.log`) shows 8 failures and 1 error, all in `test_zip` and all `git init` exiting 0xC0000142
  (a DLL failed to initialise: the machine, not the code; run 3's suite on the same generator code was green); re-run in
  this run's acceptance. This run: the branch review run 4 had queued, REQUIREMENTS 58's ceilings for the media
  columns, the Reels caption's legibility (smoke screenshot: the gradient starts at the caption's first line).
  - 17:00 `e944b9ca` REQUIREMENTS 58: the media scroll rows at 1.0 %, IMAGES.md/VIDEO.md say why (perf tests 82 OK).
  - Review findings (decision 11): `AppVideo`'s play glyph and failure text in `onAccent` (dark in the dark theme)
    straight on the video; the Reels caption fade too short. Tests written first: `VideoBadgeTest` (new, NATIVE
    graphics) and three cases in `AppVideoTest`; red run queued (`logs/media/r5-badge-red.log`), the fix is ready in
    `scratchpad/badge_patch.py` of this session and gets applied after the red run (AppVideo: `PlayBadge`,
    `FailedBadge`, `VideoBadge` on `surfaceElevated`). Reels scrim and VIDEO.md rule written (uncommitted).
  - The orchestrator fetched and transcoded the clips (17:02-17:14); L6 done (decision 10, friday rejected); clip move
    staged (SampleClips.kt to the release, VIDEO.md "Sample clips", scenes_video.py hosts and ten swipes), waiting for
    a catalog compile before its commit. Release request at the top of this file.
  - Prefetch before the first scroll, checked on run 4's smoke log: nine `prefetched` lines before the first swipe, so
    the first window is read after layout on the phone; no change.
  - Generator suite running (`logs/media/r5-gen.log`). Next: badge red, fix, green with `:catalog:compileReleaseKotlin`,
    three commits (clip move, badge, scrim), then template build, matrix, B rebuild, smoke once the release exists.
- **Run 4 (2026-10-24 08:28 IST on)**: staging moved to `4760b407` (one web commit: the glass note in the builder);
  merged as `0d843aff`, no conflict. Run 3's smoke never reached the app: the install of B produced no line in any log
  and `am start` answered "Activity class ... does not exist" (`perf/media/smoke/20261024-0815/*-start.txt`), and each of
  its adb calls took the lock on its own, so staging's commands could run between them. `media-phone.win.sh` now holds
  adb once for the whole smoke and once per sheet in `sheets`, keeps the install's output and stops on a failed install,
  and force-stops the catalog at the end. Smoke re-queued (`logs/media/r4-smoke.log`). Next: the branch review below.
- **Run 3 (2026-10-24 06:21 IST on)**: staging moved to `7230b666` (5 commits: hairlines as rects, the harness's
  unlisted first frame, the DEV badge per draw, the large title from an effect, re-recorded profiles); merged as
  `782edc95` with no conflict. `91412283` keeps a feed's players when a parent provides a new `LocalTabVisible` holder
  (run 2's uncommitted change, now with its red-first test: `logs/media/r3-vfs-red.log`, green `r3-vfs-green.log`).
  Acceptance on `91412283` (staging merged): template `./gradlew build --continue` BUILD SUCCESSFUL in 19 min, 990 unit
  tests in the JUnit files, 0 failures, the three stability checks and the three release dev-tooling guards ran
  (`logs/media/r3-template-build.log`); generator suite 488 OK, 9 skipped, catalogue current (`r3-gen.log`); full
  consistency matrix 167 projects, 0 new issues, CONSISTENCY OK (`r3-matrix.log`). Lookups L2, L3 (DUMP) and L10 done
  (section "Lookups (run 3)"). Standard preset (catalog on, Coil and video off) generated from this branch:
  `check assembleDevDebug assembleProdRelease :catalog:assembleRelease --continue` BUILD SUCCESSFUL (spotless, lint,
  tests, rules included), and no file outside the vendored skills names Coil, Media3, `core:image` or `core:video`
  (`r3-standard.log`, `out/media/gen/desk-standard`). B built: `perf/media/apks/B.apk` (Everything's catalog release
  from `91412283`, `r3-build-B.log`); `media-arms.py --check` applies all seven image arms on that tree. Smoke check on
  the phone queued behind staging's A/B session (it holds adb for its whole script); `r3-smoke.log`.
- **Run 2 (2026-10-24 01:14 IST on)**: staging still `d429cbfb` (merge = up to date). Commits on top of run 1's three:
  `0ebb4b31` District P3 (W3-2..W3-5 in one commit, with its AGENTS line), `2f867a52` one shared media client
  (`MediaCallFactory`), Coil's own `coil3_disk_cache` folder, prefetch by lines with in-flight cancellation, the catalog's
  perf log and GC hook, `9e235674` video V1-V4, `59731da7` perf scene packs P1a/P1b with the `images` and `video`
  gate columns and the generator ownership/footprints. Working tree clean after `59731da7`.
  Verified: generator suite 488 OK / 9 skipped once `catalogue --write` ran (`logs/media/r2-gen1.log`); site tests 224
  pass; perf tests 58 + 9 + 13 pass; :core:image, :core:video, the designsystem sheet suites green; catalog and app
  compile (`logs/media/r2-batch6.log`). Running at 02:0x: full template `build` (`logs/media/r2-template-build.log`) and
  the full consistency matrix (`logs/media/r2-matrix.log`).
  Phone scripts written (workspace, not committed): `bin/media-arms.py` (every arm applies on a generated Everything,
  `--check` passes) and `bin/media-phone.win.sh` (build, build-staging, smoke, session1, session2, sheets).
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

## Lookups (run 3)

- **L2, the Redmi's video decoders** (`perf/media/lookups/media_codecs.xml`, read from `/vendor/etc/media_codecs*.xml`):
  hardware `c2.qti.vp9.decoder` (6 concurrent instances, up to 4096x2160), `c2.qti.vp8.decoder` (16, up to 1920x1088),
  `c2.qti.avc.decoder` and `c2.qti.hevc.decoder` (16 each, up to 4096x2160); Google's software `c2.android.*` behind
  them. The Commons WebM clips (VP9 or VP8, at most 1920x1080) decode in hardware, and a pool of 3 is within every
  decoder's limit, so the pool size is a memory and thermal question for G3, not a decoder-count one.
- **L3, shell holds DUMP**: `android.permission.DUMP: granted=true` for `com.android.shell`
  (`perf/media/lookups/shell-package.txt`), so the GC hook's `DUMP` guard admits `am broadcast` from adb.
  The idle resource-manager dump (`perf/media/lookups/resource_manager-idle.txt`) lists no processes and an
  "Events logs" section whose `addResource(pid N, ...)` lines are not client entries.
- **L10**: see "Orchestrator requests" 1 (206 through one redirect).

## Decisions (run 3)

9. **The image scenes' absolute rows wait for staging's scroll fix.** Feed and Shop are lists inside `AppScaffold`, whose
   content slot is a `SubcomposeLayout`: staging's TEST-REPORT ("Still open" 1, candidate (a)) suspects exactly that
   keeps the lazy list's prefetch from composing rows between frames. Until staging's fix lands, session 1's A/B
   deltas (every arm shares the scaffold) are valid, but the `images` column's absolute scroll rows may carry staging's
   cost; the gate step should run the acceptance runs of the `images` column after the fix is merged into this branch.
   The Reels scene draws its pager outside the scaffold and is not affected.

## Decisions (run 5)

10. **L6, the clip content review, on the self-hosted transcodes** (`%TEMP%\mrev\clips\review\page{1,2,3}.jpg`: each
    clip's poster and its three frames at start, middle and end, from the orchestrator's transcode of 17:12): eleven
    kept (flower-moon, kings-peak, tilia, oystercatcher, srnka, perseids, ocean, bumblebee, jellyfish, mt-hood, flower):
    nature, sky and landscape, no person, brand or readable text. **Rejected: friday** (MDN's `friday.mp4`): two people
    in focus, from a 1940 film. A replacement would be a new download the user's 9.3 answer did not list, so the set is
    eleven clips; `scenes_video.py` swipes up ten times (one short of eleven) so every swipe up of a run lands on a
    clip it has not played. To copy into `decisions/LOG.md`.
11. **Words over a clip sit on a surface, never on the video** (REQUIREMENTS 18, found in run 4's smoke screenshot and
    the tokens): `AppVideo` drew its play glyph and its failure text in `onAccent`, which is `#06101F` in the dark theme,
    straight on the video; the Reels caption's fade began at its first line. Now a badge of `surfaceElevated` holds the
    glyph and the failure (`VideoBadgeTest`, both themes, over a frame of the ink's own colour), and the caption's scrim
    starts 96 dp above the words, full by 45 % and doubled at the bottom (AA over a white frame, computed: name 4.8:1,
    title 6.5:1, credit about 8:1).

## For WP12 (docs base; outside this package's write scope)

- `docs-draft/PERFORMANCE.md`, "One image loader, on the app's client" (about :803-811): the rule becomes "The loader always
  fetches through the call factory the app passes: its `OkHttpClient` without the HTTP cache (`newBuilder().cache(null)`),
  or, without the networking module, the image module's `MediaCallFactory`, which video shares. Never a second loader and
  never a client of Coil's own (its service loader is off)." Why adds "video uses the same client". "Enforced by" becomes
  `image-loader` (a second `ImageLoader`, `SingletonImageLoader.setSafe`/`set` outside `core/image`, `app`, `catalog`).
- `docs-draft/PERFORMANCE.md`, "Draw remote images as painters": `AppNetworkImage` is in `:core:image` now (not `:core:ui`).
- `docs-draft/AGENTS-rules.md:67`: "...the app has one image loader, `appImageLoader()` in `:core:image`, set in
  `BaseAppApplication` on the app's one media client (its `OkHttpClient`, or `MediaCallFactory` without the networking
  module). — `subcompose-image`, `image-loader`".
- `T/docs/PERFORMANCE.md` sections to carry from this branch, fenced as the template fences them: Images (`coil`:
  P3's two entries, "Decode at the size you draw", "Fetch the items ahead to disk", "How the image scenes are
  measured"; source text `T/docs/IMAGES.md` "Rules for images in a list", "The loader and its limits", "How the image
  scenes are measured"); Video (`video`: entries 1-7 of media.md 5.4; source `T/docs/VIDEO.md` "Rules" and "How the scene
  is measured"); P7 ("Fetch a page before a fling can reach the end", lists-images 3.5), P9 ("Very large data", 4.1),
  District P3 ("A sheet's first frame is paid during the press", 5.5). The `AGENTS.md` lines for all of them shipped on
  this branch (§4 recipes "A paged list", "A very large list", "A sheet with a list", "Remote images", "Video").
  Every `{...}` number comes from the gate step's sessions (G3), not from this branch.
- REQUIREMENTS 58 (run 5): the `images` and `video` columns' scroll rows (`janky_after_first_pct`) are 1.0 % on this
  branch, and `T/docs/IMAGES.md` / `T/docs/VIDEO.md` say why (every frame over 8.33 ms counts at 120 Hz; a tighter limit
  sits inside one build's run-to-run noise; a same-session A/B regression beyond that noise still fails). The
  PERFORMANCE.md media sections carry the same 1.0 % and that sentence, not 0.5 %.
- `docs-draft/PENDING.md`: T-P3, T-P7, T-P9 and D-P3 can be marked "AGENTS.md shipped on staging-wp/MEDIA; PERFORMANCE.md
  hunk with WP12's base".
- The root README's presets table (:100-104) is stale on staging already (counts; Lean and Standard never named Coil);
  this branch changed only the feature rows (:275-276). WP12's knowledge pass owns the rest.

## Waiting on staging's open fix or on others

- Phone sessions (P3 decode A/B, P7, District P3, media sessions 1-2): run by the gate step; this run prepares scripts.
- `T/docs/PERFORMANCE.md` hunks (P3, P7, P9, District P3, media sections): WP12's docs base must be in staging first.
- freeze #10 (`StillFade` around an image fade) and `isStill` for building players (freeze P1): wait for staging's jank
  fix; the video feed uses the two-frame fallback the plan names.
- Glass and the back card over a SurfaceView (risk 8.6): judged on the phone in session 2.
- The `images` column's absolute scroll rows: after staging's scroll fix is merged here (decision 9).
- G3 (gates, pool size, prefetch from the Redmi) and D1's numbers: after sessions 1-2 and their verifier.
- P7's phone check (lists-images 3.4) runs on the app's Feed tab, inside the shell staging is still fixing: its
  janky-frame rows share staging's scroll regression; its own question (does a fling reach the loading footer) does not.

## For the gate step (phone sessions; scripts in `D:/projects/tools/.overhaul/bin`)

- `media-phone.win.sh` (header lists every command) and `media-arms.py` (`--check` applies every arm in memory).
  Builds: `build <name> --arm <arm>` generates Everything from this branch into `out/media/gen/<name>` and builds its
  catalog's release APK into `perf/media/apks/<name>.apk`; `build-staging` builds staging's catalog as `A0`.
  Session 1 needs N, A, B, BP, BF, BS, BHW0; session 2 needs V2, V3b0, VT (VF only if 4.6 asks). Run 3 built B
  from `91412283`; rebuild it if the branch moves.
- Order: `smoke` (done by run 3 if its section below says so), `session1`, `sheets` (District P3), `session2`; P7's
  check is lists-images 3.4's own procedure on the app (not scripted here).
- Every command takes `LOCKED_PRIORITY=low` from the script while staging's fix is open; device files only under
  `/data/local/tmp`; `perf.py` restores what it changes; the smoke changes no setting.
- Run 5 moved the branch (badge, scrim, the clips to the `sample-media-v1` release): every APK in `perf/media/apks`
  older than run 5's last commit is stale (B, P3-B*, P7-B), so rebuild before any session. Session 2 needs the release
  published first: `scenes_video.py` pings `github.com` and `release-assets.githubusercontent.com` (both answer ping,
  checked from this machine), swipes up ten times over the eleven clips, and stops on a 429 from any host. The lists
  checks (`build-lists`, `p3`, `p7`) do not depend on the clips.

