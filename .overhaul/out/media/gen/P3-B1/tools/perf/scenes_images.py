"""The catalog's photo scenes, measured: a feed and a shop grid of real photos over the network.

    perf.sh --package <app id>.catalog --apk catalog/build/outputs/apk/release/catalog-release.apk \\
        --gate images media_feed media_feed_warm media_grid media_grid_warm media_feed_memory media_trace

Cold runs start from `pm clear`, so the image disk cache is empty and every photo comes over the network;
warm runs start a fresh process after a cold pass, so the memory cache is empty and every photo comes from
disk. Each fling is a segment of its own: frame stats reset, the swipe, a pause for the list to come to
rest, then the dump. Memory is read with the list at rest, after the catalog has collected garbage.

`media_trace` counts pictures decoded or uploaded on the frame threads from an atrace of six cold flings.
An `Upload <w>x<h>` slice counts as a photo only when a photo of that size was decoded in the run (icons
are uploaded too, the first time a row draws them, and are not image decoding). A control arm that
decodes nothing matches its uploads against another run's sizes: set `MEDIA_SIZES_FROM` to that run's
directory.
"""
import os
import re
import time

#: Flings down then up, at 40 % of the window's height: two fast, two medium and two slow each way.
FLING_MS = (80, 80, 150, 150, 300, 300)
FLING_SHARE = 0.4
FLING_SETTLE_S = 1.6
MEMORY_FIRST_FLINGS = 3
MEMORY_MORE_FLINGS = 12
MEMORY_REST_S = 3.0
OPEN_WAIT_S = 2.0
#: Where the photos come from: a scene that cannot reach it measures nothing.
PHOTO_HOSTS = ("picsum.photos",)

_DECODE_SLICE = re.compile(r"decode|ImageDecoder|BitmapFactory", re.IGNORECASE)
_UPLOAD_SLICE = re.compile(r"Upload (\d+)x(\d+)")
_TRACE_LINE = re.compile(r"^\s*(?P<task>.+?)-(?P<tid>\d+)\s.*?tracing_mark_write: B\|(?P<pid>\d+)\|(?P<name>.*)$")
_DECODED = re.compile(r"decoded w=(\d+) h=(\d+)")


def fling(width, height, down, millis):
    """An `input swipe` of FLING_SHARE of the window's height through its middle, [down] or up."""
    x = width // 2
    top = int(height * (0.5 - FLING_SHARE / 2))
    bottom = int(height * (0.5 + FLING_SHARE / 2))
    start, end = (bottom, top) if down else (top, bottom)
    return f"input swipe {x} {start} {x} {end} {millis}"


def flings(run, scenario):
    """Six flings down and six up, each its own segment; records the gfx keys under [scenario]."""
    width, height = run.device.size
    dumps, all_frames = [], []
    for down in (True, False):
        for millis in FLING_MS:
            gfx, frames = run.segment(scenario, fling(width, height, down, millis), FLING_SETTLE_S)
            dumps.append(gfx)
            all_frames.append(frames)
    run.record_gfx(scenario, dumps, all_frames)


def decoded_sizes(lines):
    """The (width, height) of every photo the run decoded, from the scenes' `decoded` lines."""
    return {(int(w), int(h)) for w, h in (match.groups() for match in map(_DECODED.search, lines) if match)}


def frame_thread_work(trace_text, pid, sizes):
    """(decodes, other uploads) on [pid]'s main thread and RenderThread, from atrace text.

    A decode slice there always counts: a raster decode on a frame thread is wrong whatever it decodes.
    An upload counts as a photo when its size is one of [sizes], the photos the run decoded; any other
    upload (an icon's first draw) is counted apart.
    """
    decodes = others = 0
    for line in trace_text.splitlines():
        match = _TRACE_LINE.match(line)
        if not match or match.group("pid") != str(pid):
            continue
        frame_thread = match.group("tid") == str(pid) or match.group("task").strip() == "RenderThread"
        if not frame_thread:
            continue
        name = match.group("name")
        upload = _UPLOAD_SLICE.search(name)
        if upload:
            if (int(upload.group(1)), int(upload.group(2))) in sizes:
                decodes += 1
            else:
                others += 1
        elif _DECODE_SLICE.search(name):
            decodes += 1
    return decodes, others


def open_scene(run, section, tag):
    run.clear_log()
    run.launch_section(section, tag)
    time.sleep(OPEN_WAIT_S)


def scroll_scene(section, tag):
    """A cold or warm pass of flings over the catalog page [section], whose list carries [tag]."""

    def scenario(run, name):
        run.check_hosts(name, PHOTO_HOSTS)
        run.app.clear()
        if name.endswith("_warm"):
            # A cold pass first fills the disk cache; a fresh process then starts with only that.
            open_scene(run, section, tag)
            flings(run, f"{name}_prime")
            run.check_http(name, run.media_log())
        open_scene(run, section, tag)
        flings(run, name)
        run.check_http(name, run.media_log())

    return scenario


def feed_memory(run, name):
    """Memory at rest after three flings (the cache is full by then), and again after twelve more of new
    photos: a bounded cache grows by noise only."""
    run.check_hosts(name, PHOTO_HOSTS)
    width, height = run.device.size
    run.app.clear()
    open_scene(run, "Feed", "media_feed")
    for index in range(MEMORY_FIRST_FLINGS):
        run.device.shell(fling(width, height, True, FLING_MS[index % len(FLING_MS)]))
        time.sleep(FLING_SETTLE_S)
    time.sleep(MEMORY_REST_S)
    first = run.gc_reading(f"{name}_a")
    for index in range(MEMORY_MORE_FLINGS):
        run.device.shell(fling(width, height, True, FLING_MS[index % len(FLING_MS)]))
        time.sleep(FLING_SETTLE_S)
    time.sleep(MEMORY_REST_S)
    second = run.gc_reading(f"{name}_b")
    run.check_http(name, run.media_log())
    record_growth(run, name, first, second)


def record_growth(run, name, first, second):
    """The second reading, what grew between the two, and how far a reading's three collections spread."""
    for field in ("pss_mb", "graphics_mb", "pss_app_mb"):
        if field in second:
            run.record(f"{name}.{field}", second[field])
    if "graphics_mb" in first and "graphics_mb" in second:
        run.record(f"{name}.graphics_growth_mb", round(second["graphics_mb"] - first["graphics_mb"], 1))
        run.record(f"{name}.app_growth_mb", round(second["pss_app_mb"] - first["pss_app_mb"], 1))
    run.record(f"{name}.gc_spread_mb", max(first["gc_spread_mb"], second["gc_spread_mb"]))


def sizes_for(run, lines):
    """The decoded sizes to match uploads against: this run's, or the run MEDIA_SIZES_FROM names."""
    other = os.environ.get("MEDIA_SIZES_FROM")
    if not other:
        return decoded_sizes(lines)
    with open(os.path.join(other, "media_trace_media_log.txt"), encoding="utf-8") as handle:
        return decoded_sizes(handle.read().splitlines())


def feed_trace(run, name):
    """Six cold flings under atrace: no photo may be decoded or uploaded on the frame threads."""
    run.check_hosts(name, PHOTO_HOSTS)
    width, height = run.device.size
    run.app.clear()
    open_scene(run, "Feed", "media_feed")
    run.device.shell(f"atrace --async_start -b 32768 -a {run.app.package} gfx view")
    for millis in FLING_MS:
        run.device.shell(fling(width, height, True, millis))
        time.sleep(FLING_SETTLE_S)
    text = run.device.shell("atrace --async_stop", timeout=120)
    run.save(f"{name}_atrace.txt", text)
    lines = run.media_log()
    run.save(f"{name}_media_log.txt", "\n".join(lines))
    run.check_http(name, lines)
    pid = run.app.pid()
    if not pid:
        run.fail("the catalog stopped during the trace")
    decodes, others = frame_thread_work(text, pid, sizes_for(run, lines))
    run.record(f"{name}.frame_thread_decodes", decodes)
    run.record(f"{name}.frame_thread_uploads_other", others)


SCENES = {
    "media_feed": scroll_scene("Feed", "media_feed"),
    "media_feed_warm": scroll_scene("Feed", "media_feed"),
    "media_grid": scroll_scene("Shop", "media_shop"),
    "media_grid_warm": scroll_scene("Shop", "media_shop"),
    "media_feed_memory": feed_memory,
    "media_trace": feed_trace,
}

#: Every scene here measures the catalog app, not the app: perf.sh runs them with the catalog's APK.
CATALOG = True
