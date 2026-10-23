# Remote images

Images come from the network through one component, `AppNetworkImage`, and one loader, `appImageLoader()`,
both in `:core:image`. Together they make a list of photos cost what a list of text costs: one composition per
row, no layout pass when a picture arrives, and decoding that stays off the frame.

A feature that shows remote images depends on `:core:image` itself:

```kotlin
// feature/orders/build.gradle.kts
dependencies {
    implementation(project(":core:image"))
}
```

## Show an image

```kotlin
AppNetworkImage(
    url = order.thumbnailUrl,
    contentDescription = null,
    modifier = Modifier.size(AppTheme.sizes.rowHeight).clip(AppTheme.shapes.sm),
)
```

While it loads it draws the skeleton colour, and when it fails a glyph; inside an active `AppRedacted` it is a
skeleton and loads nothing. Both stand-ins are painters, so a finished load changes only what that image draws:
nothing recomposes.

## Rules for images in a list

#### Give every image its size before it loads

**Rule.** In a row, give `AppNetworkImage` a fixed size or an aspect ratio taken from your data, never from the
loaded picture: `Modifier.size(…)`, a fixed slot such as `AppAvatar`'s, or
`Modifier.fillMaxWidth().aspectRatio(item.width.toFloat() / item.height)`. Never `ContentScale.None` in a list.

**Why.** With both sides known, the image never asks the picture for its size: a finished load redraws that one
image, the row is not measured again, and nothing below it jumps mid-scroll. The request also asks for the slot's
pixels, so a 4000-pixel original is decoded at the 360 or 1080 pixels it is drawn at. `ContentScale.None` asks for
the original instead; the loader caps that at the screen's longest side, but in a list it is still the largest
decode the image can make.

**Don't.** `Modifier.fillMaxWidth()` alone on a list image; an aspect ratio read from the loaded image.

**Protects.** A finished load: no recomposition and no row measured again (`AppNetworkImageTest`); the request is
the slot's size (same test).

#### Fade only outside lists

**Rule.** Leave `fadeIn` off in rows. A header or a hero may pass `fadeIn = true`: it fades in over the theme's slow
fade (200 ms), not under reduce motion, and never when the picture comes from the memory cache.

**Why.** A fade redraws its image on every frame of its 200 ms. In a flung list several land at once, for a look
nobody sees at that speed.

#### Fetch the images ahead to disk, not to memory

**Rule.** A scrolling list of images calls `PrefetchImages` with its state and a way to find each item's image:

```kotlin
val list = rememberLazyListState()
LazyColumn(state = list, flingBehavior = AppScrollDefaults.flingBehavior()) {
    items(posts, key = { it.id }, contentType = { "post" }) { post -> PostRow(post) }
}
PrefetchImages(list) { index -> posts.getOrNull(index)?.photoUrl }
```

It has `LazyGridState` and `PagerState` overloads too.

**Why.** A fling outruns the network: rows arrive faster than their pictures. Prefetch fetches the bytes of about
three screens of items ahead into the disk cache, so a fling meets pictures that only need decoding. It decodes
nothing and holds nothing in memory: the row decodes its picture at its own size when it composes. It starts on the
second line past the last one on screen (a line is an item of a list or a pager, a row of a grid), because the list
composes the next line itself; an item that comes within a line of the screen is left to its row, dropped while it
waits and cancelled while it fetches, so nothing is downloaded twice. It runs at most two requests at once, so the
pictures on screen never queue behind it in the HTTP client, and turning back drops what waits. It looks at the list
once per item that reaches the top, never per scroll frame.

**Don't.** Prefetch with a plain `ImageRequest` and `enqueue`: without a size it decodes the original, up to the
screen's size, and keeps it in the memory cache.

## The loader and its limits

`appImageLoader()` builds the app's one Coil loader, installed in `VideoAloneApplication.onCreate` with the app's one
media client. Change a limit there and nowhere else; a second loader, or a limit set at a call site, splits the
caches and the HTTP client. The build fails on a second loader (`image-loader`).

```kotlin
// VideoAloneApplication: the app's own client, or MediaCallFactory() in a project without the networking module.
private val mediaClient: Call.Factory by lazy { okHttp.get().newBuilder().cache(null).build() }

SingletonImageLoader.setSafe { context -> appImageLoader(context, callFactory = { mediaClient }) }
```

| Limit | Value | Why |
|---|---|---|
| Memory cache | a fifth of the heap class (three twentieths on a low-RAM phone), at most 64 MB | decoded pictures stay inside the app's memory budget |
| In the background | half of it | the process is less likely to be killed there and started cold |
| Largest decode | the display's longest side, as a square | no request decodes more pixels than the screen can show |
| Decodes at once | 2, until the reference phone's A/B sets it | a decode competes with the frame for the CPU |
| Bitmaps | hardware | the pixels reach the GPU once, when decoded, not in the frame that first draws them |
| Fade | none unless an image asks | see above |
| Disk cache | Coil's bounds (2 % of free space, 10 to 250 MB), in the cache directory under Coil's own folder name, `coil3_disk_cache` | the system may clear it and never backs it up; an app that ran Coil's default loader keeps the cache it has |
| Network | the one client images and video share: the app's own without its HTTP cache with the networking module, otherwise `MediaCallFactory`, one OkHttp client built on the first request | one connection pool, one set of certificate pins and one dispatcher for every picture and clip; Coil never builds a client of its own |

Coil's service loader is off, so nothing joins the pipeline unseen. To decode another format, add its decoder in
`appImageLoader()` explicitly:

```kotlin
// In appImageLoader(), beside the network fetcher; coil-svg goes in the version catalog.
.components {
    add(networkFetcher)
    add(SvgDecoder.Factory())
}
```

A disk entry is served without asking the server again, which suits APIs whose image URLs change when the picture
does. For an API that reuses URLs, add `coil-network-cache-control` and pass its cache strategy to the fetcher.

An API that sends a colour per picture (Unsplash's `color`, Pexels' `avg_color`) can show it while loading: draw
`AsyncImage` with your own `ColorPainter` as the placeholder, as `AppNetworkImage` does with the skeleton colour.

## In the catalog

`--es section Feed` opens a photo feed: a row of stories, then full-width photos at their own aspect ratios.
`--es section Shop` opens a shop grid: two columns of product photos on a phone, more on wider windows. Both load
real photos over the network, through the catalog's own `MediaCallFactory`, which names the catalog in its
User-Agent, and are where the image measurements run.

## How the image scenes are measured

`tools/perf/perf.sh` measures the two scenes on the catalog's release APK, beside the app's own gates:

```bash
tools/perf/perf.sh --package <app id>.catalog --apk catalog/build/outputs/apk/release/catalog-release.apk     --gate images media_feed media_feed_warm media_grid media_grid_warm media_feed_memory media_trace
```

- **Cold** runs start from `pm clear`: every photo comes over the network. **Warm** runs start a fresh process after a
  cold pass: every photo comes from the disk cache. Scrolling back within a run is the memory cache.
- Each fling is measured on its own and held to the release limits: janky frames after each fling's first at most
  0.5 %, hitches at most 5 ms per second of motion, GPU p90 at most 7 ms.
- `media_trace` counts photos decoded or uploaded on the main thread or RenderThread: none, because bitmaps are
  hardware bitmaps decoded on Coil's own threads. An upload counts as a photo only when a photo of that size was
  decoded in the run: icons are uploaded too, the first time a row draws them, and are not image decoding.
- Memory is read after the catalog collects garbage, three times, and the median kept: an evicted bitmap stays in
  the graphics memory until ART collects its object, so a reading without a collection measures when ART last ran.
  Twelve flings of new photos after the cache is full may grow it by noise only.
- A host that does not answer fails the scenario, and a `429` from any host stops the session.

The photos are by their photographers on Unsplash, served through Lorem Picsum (picsum.photos), under the Unsplash
License; the catalog credits them by name. Each was looked at before it was listed: no person in focus, no brand
and no readable text.
