# Video

A feed of clips, as a reels app has it, through `rememberVideoFeedState` and `AppVideo` in `:core:video`. Everything
that keeps such a feed smooth is built in: a few players shared by the whole feed, each clip drawn on a SurfaceView,
its poster until its first frame, the next clips ready before a swipe lands, and every player released when the
screen stops. Media3 is used only inside `:core:video`; nothing else names it.

## Set up

The app hands over its one media client once, in `Application.onCreate`: the same client images use, so pictures
and clips share one connection pool, one set of certificate pins and one dispatcher. Nothing of Media3 loads there;
each feed reads it when it first builds its players.

```kotlin
// VideoAloneApplication: the app's own client, or MediaCallFactory() in a project without the networking module.
private val mediaClient: Call.Factory by lazy { okHttp.get().newBuilder().cache(null).build() }

AppVideoSetup.install(callFactory = { mediaClient })
```

Media3 never falls back to a client of its own: a feed that opens before the install stops with a message that says
so. The client is first used on Media3's loading thread, never on the main one.

## A feed

```kotlin
val clips: List<VideoClip> = state.clips   // id, url, poster URL, width and height, from your API
val pager = rememberPagerState { clips.size }
val feed = rememberVideoFeedState(clips, current = { pager.settledPage })
VerticalPager(
    state = pager,
    beyondViewportPageCount = AppVideoDefaults.BeyondPages,
    flingBehavior = AppScrollDefaults.pagerFlingBehavior(pager),
    key = { clips[it].id },
) { page ->
    AppVideo(feed, index = page, contentDescription = clips[page].title, modifier = Modifier.fillMaxSize())
}
PrefetchImages(pager) { page -> clips.getOrNull(page)?.posterUrl }
```

The catalog's short-video scene (`--es section Reels`) is the full example, with a sound button and the clip's
credit over it.

## What happens when

| Event | Players | Playback |
|---|---|---|
| The screen opens | none yet; every page shows its poster | nothing plays |
| Two frames later | built one per frame: the settled page, the next, the previous | the settled clip plays, muted |
| A swipe settles | a page leaving the window gives its player to the page entering it | only the settled clip plays |
| A fling passes pages | pages never settled get no player | |
| A screen, sheet or cover over the feed | kept | paused |
| Home, the screen off, the tab hidden | positions kept, then the preloading and every player released | |
| Back again | rebuilt as on open | each clip resumes where it was |
| Sound on | the settled clip's audio track and audio focus, nobody else's | |
| A clip fails | its player stays | "This video can't play" and a retry over the clip; the feed goes on |
| Reduce motion | | nothing plays until tapped |

## Rules

#### Players come from the feed's pool, never one per row

**Rule.** Show video through `rememberVideoFeedState` and `AppVideo`. Never build an `ExoPlayer` in a row or a screen.

**Why.** Each player holds a video decoder, buffers and a share of a playback thread, and a phone runs only a few
decoders at once. The feed holds at most three players and hands them from page to page, so a feed of a thousand
clips costs what a feed of three does.

**Enforced by.** `video-player` (an `ExoPlayer.Builder`, a preload manager or a `PlayerPool` outside `:core:video`).

#### Video draws on a SurfaceView

**Rule.** Keep `AppVideo`'s SurfaceView; never a `TextureView`.

**Why.** On a SurfaceView the system composites every video frame itself: a playing clip costs the app no frames,
and the UI thread stays idle. A TextureView makes the app draw each video frame into its own layer. A SurfaceView is
a separate layer, so glass chrome cannot blur it and the back gesture's card does not carry it: give the floating
bar its solid look over a full-screen feed.

**Enforced by.** `video-surface`; `media3-views` (Media3's View player and its Material player).

#### A poster until the first frame; the next clip ready before the swipe lands

**Rule.** Give every clip a poster URL and fetch the posters ahead with `PrefetchImages(pager)`. Keep the pager's
`beyondViewportPageCount` at `AppVideoDefaults.BeyondPages`.

**Why.** A page shows its poster until the player has drawn the clip's first frame, so a swipe never reveals a black
page. The neighbours hold a player with their first frame drawn and paused, and the clips further on are preloaded
by distance: a second or two in memory for the next few, the first seconds on disk for the ones after.

#### Muted means no audio track and no audio focus

**Why.** A muted feed must not stop the person's music, and a disabled audio track decodes nothing. Turning the sound
on enables audio on the settled clip only, and takes audio focus then.

#### Short buffers and bounded caches

**Why.** Media3 buffers fifty seconds per player by default; three players of short clips would hold minutes of
media. The feed buffers five to fifteen seconds and starts after half a second, keeps a 128 MB disk cache of clip
bytes (least recently used out first) and drops preloaded clips more than three behind or six ahead.

**Why the cache is wrapped twice.** Media3's preload manager reads the disk cache for playback through a reader that
never writes it, so the feed's own data source wraps the network in a second, writing cache layer: what a player
streams reaches the disk, and a swipe back or a second visit plays from it. When the manager pre-caches a far clip
it writes through a writer of its own over the same chain; the inner layer then finds the span locked and passes the
bytes through instead of waiting. `VideoCacheTest` fails if either layer is taken away.

## How the scene is measured

`tools/perf/perf.sh` measures the catalog's short-video scene on its release APK:

```bash
tools/perf/perf.sh --package <app id>.catalog --apk catalog/build/outputs/apk/release/catalog-release.apk     --gate video media_reels media_reels_warm media_background
```

- Each swipe is measured like a scroll: janky frames after its first at most 0.5 %, hitches at most 5 ms per second.
- The scene logs each settle, hand-over and first frame on the phone's clock, so the time from a swipe landing to its
  clip's first frame is read from the app. Warm, no swipe may land on a poster.
- While a clip plays, the app draws no frame at all: counted in a quiet window that opens only once every page
  holding a player has drawn its first frame and no image is loading.
- Decoders in use never exceed the pool, and ten seconds after Home none is held and no player thread runs.
- Memory is read after the catalog collects garbage, before and after twenty more clips.

## What it adds to the app

`:core:video` declares INTERNET itself, so a project without the networking module can stream. Media3's own
manifests add ACCESS_NETWORK_STATE and WAKE_LOCK to the merged manifest; both are normal permissions that ask the
person nothing, and the feed never takes a wake lock (a playing clip keeps the screen on through its view instead).
Media3 ships no baseline profile, so `:core:video`'s own profile covers the feed's code and Media3's classes are
compiled as the app runs them.

## Not part of it

Background audio, a media session, picture-in-picture, Cast, DRM and adaptive streaming are other products with their
own costs. HLS or DASH need `media3-exoplayer-hls` or `media3-exoplayer-dash` in the version catalog and a
`MediaItem` with the manifest's URL; nothing else changes.

## Sample clips

The catalog's twelve clips are public domain (CC0), each its uploader's own work: ten on Wikimedia Commons, by
Roesalow, Derrellwilliams, Gustamons, Cesaremanfredi, Pohled 111, Bautsch, Netha Hussain, Roman Ryabenko, Benoit Prieur
and Pastelitodepapa, played from Commons with Commons' stills as posters; and two from MDN's CC0 examples, which have
no poster and show the skeleton colour until their first frame. Their URLs are in one file,
`catalog/…/media/video/SampleClips.kt`.
