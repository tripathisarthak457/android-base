"""The catalog's short-video scene, measured: a reels feed of clips over the network.

    perf.sh --package <app id>.catalog --apk catalog/build/outputs/apk/release/catalog-release.apk \\
        --gate video media_reels media_reels_warm media_background

The scene logs on the `perf.media` tag when launched with `--ez perfLog true`: the open, each settle, the
pages that hold a player (`window`), each hand-over (`acquire`) and each clip's first frame, all on the
phone's `SystemClock.elapsedRealtime`, so the time from a swipe landing to its clip showing is read from
the app, not guessed from the screen.

`playing_app_frames` counts the app's frames while the settled clip plays, in a quiet window: it opens only
once nothing is pending (every page holding a player has drawn its first frame, no image is loading, and a
second has passed without a line), the harness marks its start and end in the same log, and a window with
a first frame, a hand-over or an image load between the marks is taken again once, then not measured.
"""
import re
import time

SWIPES_UP = 12
SWIPES_DOWN = 4
SWIPE_MS = 220
DWELL_S = 1.8
#: Reading A is taken after this many swipes up, when the window and the caches are full.
READING_A_AFTER = 10
GROWTH_SWIPES = 20
GROWTH_DWELL_S = 1.0
FIRST_FRAME_TIMEOUT_S = 20.0
BACKGROUND_S = 10.0
#: Two frames at 120 Hz: a first frame later than this after its settle means the poster showed.
POSTER_AFTER_MS = 2 * 1000 / 120

WINDOW_AFTER_SETTLE_S = 3.0
WINDOW_QUIET_S = 1.0
WINDOW_SILENT_S = 3.0
WINDOW_GIVE_UP_S = 15.0
PLAYING_S = 5.0
#: Where the clips come from: a scene that cannot reach them measures nothing.
VIDEO_HOSTS = ("upload.wikimedia.org", "interactive-examples.mdn.mozilla.net")

_LINE = re.compile(r"perf\.media[^:]*:\s*(?P<text>.*)$")
_FIELD = re.compile(r"(\w+)=(\S+)")
#: Lines that mean the scene is still busy: a window that holds one of them did not measure a still clip.
_BUSY = ("frame", "acquire", "window", "images busy")


def events(lines):
    """The scene's lines and the harness's marks, in logcat's order, as (kind, fields).

    The kind is the line's first word, or `images busy` / `images idle`, or `mark start` / `mark end`
    for the playing window's marks.
    """
    parsed = []
    for line in lines:
        match = _LINE.search(line)
        if not match:
            continue
        text = match.group("text").strip()
        if text in ("window start", "window end"):
            parsed.append((f"mark {text.split()[1]}", {}))
            continue
        words = text.split()
        if not words:
            continue
        kind = " ".join(words[:2]) if words[0] == "images" and len(words) > 1 else words[0]
        parsed.append((kind, dict(_FIELD.findall(text))))
    return parsed


def settle_to_frame(parsed):
    """Milliseconds from each settle to its page's first frame; 0 when the frame came before the settle."""
    settles = [(int(f["page"]), int(f["t"])) for kind, f in parsed if kind == "settle"]
    frames = [(int(f["page"]), int(f["t"])) for kind, f in parsed if kind == "frame"]
    delays = []
    for page, settled in settles:
        times = [t for frame_page, t in frames if frame_page == page]
        if not times:
            continue
        delays.append(0 if any(t <= settled for t in times) else min(t for t in times if t > settled) - settled)
    return delays


def posters_after_settle(delays):
    """Settles whose clip showed its first frame more than two frames after landing: the poster showed."""
    return sum(1 for delay in delays if delay > POSTER_AFTER_MS)


def open_first_frame_ms(parsed):
    """The scene's own time from its first composition to its first clip's first frame."""
    for kind, fields in parsed:
        if kind == "open" and "ms" in fields:
            return int(fields["ms"])
    return None


def nothing_pending(parsed):
    """Every page of the latest window has drawn a first frame since its latest hand-over, and images are idle."""
    windows = [fields for kind, fields in parsed if kind == "window"]
    if windows:
        for page in windows[-1].get("pages", "").split(","):
            if not page:
                continue
            acquired = max((i for i, (kind, f) in enumerate(parsed) if kind == "acquire" and f.get("page") == page), default=-1)
            drawn = max((i for i, (kind, f) in enumerate(parsed) if kind == "frame" and f.get("page") == page), default=-1)
            if drawn < acquired or drawn < 0:
                return False
    images = [kind for kind, _ in parsed if kind.startswith("images")]
    return not images or images[-1] == "images idle"


def window_valid(parsed):
    """True when nothing busy lies between the latest `window start` and `window end` marks."""
    starts = [i for i, (kind, _) in enumerate(parsed) if kind == "mark start"]
    ends = [i for i, (kind, _) in enumerate(parsed) if kind == "mark end"]
    if not starts or not ends or ends[-1] < starts[-1]:
        return False
    return not any(kind in _BUSY for kind, _ in parsed[starts[-1] + 1:ends[-1]])


def frame_counts(parsed):
    """Rendered and dropped video frames over every clip the renderers let go of."""
    rendered = dropped = 0
    for kind, fields in parsed:
        if kind == "drops":
            rendered += int(fields.get("rendered", 0))
            dropped += int(fields.get("dropped", 0))
    return rendered, dropped


def codec_count(resource_dump, pid):
    """Codec clients [pid] holds in `dumpsys media.resource_manager`, or None when the dump lists no clients.

    The dump groups clients under their process (`Pid: <n>`) and names each client's resources; a client
    with a codec resource holds one codec instance.
    """
    if not resource_dump or not re.search(r"\bpid\b", resource_dump, re.IGNORECASE):
        return None
    count, inside, client_has_codec = 0, False, False
    for line in resource_dump.splitlines():
        process = re.match(r"\s*(?:Process\s+)?Pid:\s*(\d+)", line, re.IGNORECASE)
        if process:
            count += client_has_codec
            client_has_codec = False
            inside = process.group(1) == str(pid)
            continue
        if not inside:
            continue
        if re.match(r"\s*(?:Client\s+)?Id:\s*", line, re.IGNORECASE):
            count += client_has_codec
            client_has_codec = False
        elif "codec" in line.lower():
            client_has_codec = True
    return count + client_has_codec


def codec_threads(ps_threads):
    """Threads of a `ps -T -p <pid>` listing that a codec runs on: the fallback when the resource dump is missing."""
    return thread_count(ps_threads, ("CodecLooper", "MediaCodec_loop"))


def thread_count(ps_threads, prefixes):
    """Threads of a `ps -T -p <pid>` listing whose name starts with one of [prefixes]."""
    names = [line.split()[-1] for line in ps_threads.splitlines()[1:] if line.split()]
    return sum(1 for name in names if name.startswith(tuple(prefixes)))


def swipe(width, height, up):
    """A page swipe through the middle of the window."""
    x = width // 2
    low, high = int(height * 0.78), int(height * 0.22)
    start, end = (low, high) if up else (high, low)
    return f"input swipe {x} {start} {x} {end} {SWIPE_MS}"


def wait_first_frame(run, page):
    deadline = time.time() + FIRST_FRAME_TIMEOUT_S
    while time.time() < deadline:
        if any(kind == "frame" and f.get("page") == str(page) for kind, f in events(run.media_log())):
            return
        time.sleep(0.5)
    run.fail(f"clip {page} drew no first frame within {FIRST_FRAME_TIMEOUT_S:.0f} s")


def codecs_now(run, pid):
    """Codec instances the catalog holds now: from the resource manager, else from its codec threads."""
    count = codec_count(run.device.shell("dumpsys media.resource_manager"), pid)
    if count is None:
        count = codec_threads(run.device.shell(f"ps -T -p {pid}"))
        note = "codecs counted from codec threads: the resource manager's dump lists no clients on this phone"
        if note not in run.notes:
            run.notes.append(note)
    return count


def wait_until_quiet(run):
    """Waits for the scene to have nothing pending; False if it never does within WINDOW_GIVE_UP_S."""
    time.sleep(WINDOW_AFTER_SETTLE_S)
    started = last_change = time.time()
    seen = None
    while time.time() - started < WINDOW_GIVE_UP_S:
        parsed = events(run.media_log())
        busy = sum(1 for kind, _ in parsed if kind in _BUSY or kind.startswith("images"))
        if busy != seen:
            seen, last_change = busy, time.time()
        quiet = time.time() - last_change
        if quiet >= WINDOW_SILENT_S or (quiet >= WINDOW_QUIET_S and nothing_pending(parsed)):
            return True
        time.sleep(0.25)
    return False


def playing_window(run, name):
    """The app's frames while the settled clip plays, in a quiet window; None when none could be had."""
    for _ in range(2):
        if not wait_until_quiet(run):
            run.notes.append(f"{name}: the page never settled, so its playing window is not measured")
            return None
        run.log_mark("window start")
        gfx, _ = run.segment(f"{name}_playing", "", PLAYING_S)
        run.log_mark("window end")
        if window_valid(events(run.media_log())):
            return gfx["frames"]
    run.notes.append(f"{name}: a first frame or a load fell inside both playing windows, so it is not measured")
    return None


def reels(run, name):
    """Swipes through the feed cold or warm; records the swipes' frames, each clip's time to first frame,
    the decoders in use, the app's frames while a clip plays, video frames dropped, and memory over twenty
    more clips."""
    run.check_hosts(name, VIDEO_HOSTS)
    width, height = run.device.size
    run.app.clear()
    if name.endswith("_warm"):
        # A cold pass over the same clips fills the disk caches; a fresh process then starts with only those.
        run.clear_log()
        run.launch_section("Reels", "media_reels")
        wait_first_frame(run, 0)
        for _ in range(SWIPES_UP):
            run.device.shell(swipe(width, height, up=True))
            time.sleep(DWELL_S)
        run.check_http(name, run.media_log())
    run.clear_log()
    run.launch_section("Reels", "media_reels")
    wait_first_frame(run, 0)
    pid = run.app.pid()
    dumps, all_frames, codecs, first = [], [], [], None
    for index, up in enumerate([True] * SWIPES_UP + [False] * SWIPES_DOWN):
        gfx, frames = run.segment(name, swipe(width, height, up), DWELL_S)
        dumps.append(gfx)
        all_frames.append(frames)
        codecs.append(codecs_now(run, pid))
        if index + 1 == READING_A_AFTER:
            first = run.gc_reading(f"{name}_a")
    run.record_gfx(name, dumps, all_frames)
    run.record(f"{name}.codecs_max", max(codecs) if codecs else None)
    run.record(f"{name}.playing_app_frames", playing_window(run, name))

    for _ in range(GROWTH_SWIPES):
        run.device.shell(swipe(width, height, up=True))
        time.sleep(GROWTH_DWELL_S)
    time.sleep(3.0)
    second = run.gc_reading(f"{name}_b")
    run.record(f"{name}.graphics_growth_mb", round(second["graphics_mb"] - first["graphics_mb"], 1)
               if "graphics_mb" in first and "graphics_mb" in second else None)
    run.record(f"{name}.app_growth_mb", round(second["pss_app_mb"] - first["pss_app_mb"], 1)
               if "pss_app_mb" in first and "pss_app_mb" in second else None)
    run.record(f"{name}.gc_spread_mb", max(first["gc_spread_mb"], second["gc_spread_mb"]))

    # Home stops the feed, and each renderer reports what it drew and dropped as it lets go of its clip.
    run.device.shell("input keyevent KEYCODE_HOME")
    time.sleep(2.0)
    lines = run.media_log()
    run.save(f"{name}_media_log.txt", "\n".join(lines))
    run.check_http(name, lines)
    parsed = events(lines)
    delays = settle_to_frame(parsed)
    run.record_spread(f"{name}.settle_to_frame_ms", delays)
    run.record(f"{name}.posters_after_settle", posters_after_settle(delays))
    run.record(f"{name}.open_first_frame_ms", open_first_frame_ms(parsed))
    rendered, dropped = frame_counts(parsed)
    run.record(f"{name}.dropped_video_pct", round(100 * dropped / (rendered + dropped), 2) if rendered + dropped else None)


def background(run, name):
    """Home while a clip plays: no decoder and no player thread may stay; then the clip comes back."""
    run.check_hosts(name, VIDEO_HOSTS)
    run.clear_log()
    run.launch_section("Reels", "media_reels")
    wait_first_frame(run, 0)
    pid = run.app.pid()
    run.device.shell("input keyevent KEYCODE_HOME")
    time.sleep(BACKGROUND_S)
    resources = run.device.shell("dumpsys media.resource_manager")
    threads = run.device.shell(f"ps -T -p {pid}")
    run.save(f"{name}_resource_manager.txt", resources)
    run.save(f"{name}_threads.txt", threads)
    codecs = codec_count(resources, pid)
    run.record(f"{name}.codecs", codecs if codecs is not None else codec_threads(threads))
    run.record(f"{name}.player_threads", thread_count(threads, ("ExoPlayer:",)))
    # A stopped process may be frozen and not answer the collection: then the reading says so.
    reading = run.gc_reading(name, required=False)
    run.record(f"{name}.pss_mb", reading["pss_mb"])
    if reading.get("no_gc"):
        run.notes.append(f"{name}: the stopped catalog did not answer the collection; PSS read without one")
    run.clear_log()
    resumed = run.device_clock_ms()
    run.app.launch(wait=False)
    wait_first_frame(run, 0)
    frames = [int(f["t"]) for kind, f in events(run.media_log()) if kind == "frame"]
    run.record(f"{name}.resume_first_frame_ms", min(frames) - resumed if frames and resumed is not None else None)


SCENES = {
    "media_reels": reels,
    "media_reels_warm": reels,
    "media_background": background,
}

#: Every scene here measures the catalog app, not the app: perf.sh runs them with the catalog's APK.
CATALOG = True
