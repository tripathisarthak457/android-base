"""The lists-images plan's two phone checks on the app (not catalog scenes): P3's decodes at once
(lists-images.md 2.4) and P7's paging distance (3.4). Opus wrote it; a Sonnet operator runs it through
media-phone.win.sh and returns facts and paths; an Opus verifier re-derives every number from the raw dumps
and looks at the recordings.

    python -I media-lists.py bench <generated project> [--decodes N]   P3's throwaway patch: the Feed tab
                                                                      becomes 200 rows of local pictures
    python -I media-lists.py bench <generated project> --check         the patch applies; nothing is written
    python -I media-lists.py p3 --harness <project>/tools/perf --out DIR --apk B0=... --apk B4=... ...
    python -I media-lists.py p7 --harness <project>/tools/perf --out DIR --apk A=... --apk B=...

`bench` never runs on the template or a worktree (it refuses a directory holding `features/` or `generator/`).
The sessions use the generated project's own harness (`tools/perf/perf.py`: its device, install, profile and
frame-stats code), so what they measure is measured the way the gates measure. Device files go only to
/data/local/tmp and /data/misc/perfetto-traces; the rotation and stay-on settings come back at the end.

P3: arms alternate B0, B4, B2, B1; three measured runs each, every one cold (installed, cleared, compiled with
its profile as Play does, launched with the fake session), on the Feed tab: 2 s at rest, then nine flings at
40 % travel (three of 80 ms, three of 150 ms, three of 300 ms, 1.2 s apart), frame stats, memory 3 s after
the last fling and 10 s after Home. Then one more run per arm with a screen recording (placeholders after
rest, judged on the frames) and one with a Perfetto trace (decode threads' cores, the UI thread's runnable
time). Neither of those two counts in the medians: a recorder and a tracer both take CPU from the frames.

P7: arms alternate A, B; three runs per arm of each scene, each cold so the pager starts empty: `single`
(5 flicks of 80 ms at 40 % travel, 3.5 s apart) and `chained` (10 flicks of 80 ms, 0.45 s apart, from the top).
Every run is recorded; the stops at the loading footer are counted on the recordings by the verifier.
"""
import argparse
import importlib
import json
import os
import pathlib
import re
import statistics
import subprocess
import sys
import time

BENCH_ROWS = 200
BENCH_FACES = 20
BENCH_TAG = "bench_list"
FEED_SCREEN = "feature/feed/src/main/kotlin/**/feature/feed/FeedScreen.kt"
FEED_BUILD = "feature/feed/build.gradle.kts"
LOADER = "core/image/src/main/kotlin/**/core/image/AppImageLoader.kt"
ASSETS = "app/src/dev/assets/bench"


class CheckError(Exception):
    pass


# --- P3's bench patch ---------------------------------------------------------------------------------------

BENCH_SOURCE = '''
// P3's phone check (lists-images.md 2.4): local pictures only, so the decodes are measured without the network.
@Composable
private fun BenchFeed(listState: LazyListState) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("BENCH_TAG"),
        contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.stack),
    ) {
        items(count = BENCH_ROWS, key = { it }, contentType = { "bench" }) { index ->
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
                    AppNetworkImage(
                        url = "file:///android_asset/bench/hero_${(index + 1).toString().padStart(3, '0')}.jpg",
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppNetworkImage(
                            url = "file:///android_asset/bench/face_${(index % BENCH_FACES + 1).toString().padStart(2, '0')}.jpg",
                            contentDescription = null,
                            modifier = Modifier.size(AppTheme.sizes.rowHeight),
                        )
                        Column {
                            AppText(
                                text = "Row ${index + 1}",
                                style = AppTheme.typography.headline,
                                color = AppTheme.colors.contentPrimary,
                            )
                            AppText(
                                text = "Two lines of text under a local picture",
                                style = AppTheme.typography.callout,
                                color = AppTheme.colors.contentSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
'''.replace("BENCH_TAG", BENCH_TAG).replace("BENCH_ROWS", str(BENCH_ROWS)).replace("BENCH_FACES", str(BENCH_FACES))

BENCH_IMPORTS = (
    "androidx.compose.foundation.layout.aspectRatio",
    "androidx.compose.foundation.layout.size",
    "androidx.compose.foundation.lazy.LazyListState",
    "androidx.compose.ui.platform.testTag",
)


def one(root, pattern):
    matches = sorted(root.glob(pattern))
    if len(matches) != 1:
        raise CheckError(f"expected one {pattern}, found {len(matches)}")
    return matches[0]


def bench_texts(root, decodes):
    """The files the bench patch changes, as {path: new text}; raises CheckError when a patch does not match."""
    if (root / "features").is_dir() or (root / "generator").is_dir():
        raise CheckError(f"{root} looks like the template or the generator repository, not a generated project")
    texts = {}
    screen = one(root, FEED_SCREEN)
    text = screen.read_text(encoding="utf-8")
    package = re.search(r"^package (\S+)\.feature\.feed$", text, re.M)
    if not package:
        raise CheckError(f"{screen}: no feature.feed package line")
    start = text.find("        AppPagingList(\n")
    end = text.find("\n@Composable\nprivate fun FeedCard(")
    if start < 0 or end < start or text.count("        AppPagingList(\n") != 1:
        raise CheckError(f"{screen}: the feed's AppPagingList call is not where the patch expects it")
    text = text[:start] + "        BenchFeed(listState)\n    }\n}\n" + text[end:]
    # Staging draws remote images from :core:ui; the media branch moved them into :core:image.
    image_module = (root / "core" / "image").is_dir()
    image_import = f"{package.group(1)}.core.{'image' if image_module else 'ui'}.AppNetworkImage"
    missing = [name for name in BENCH_IMPORTS + (image_import,) if f"import {name}\n" not in text]
    first_import = text.index("\nimport ") + 1
    text = text[:first_import] + "".join(f"import {name}\n" for name in missing) + text[first_import:]
    texts[screen] = text.rstrip("\n") + "\n" + BENCH_SOURCE
    if image_module:
        build = root / FEED_BUILD
        build_text = build.read_text(encoding="utf-8")
        if 'implementation(project(":core:image"))' not in build_text:
            anchor = "dependencies {\n"
            if build_text.count(anchor) != 1:
                raise CheckError(f"{build}: expected one dependencies block")
            build_text = build_text.replace(anchor, anchor + '    implementation(project(":core:image"))\n')
        texts[build] = build_text
    if decodes is not None:
        if not image_module:
            raise CheckError("--decodes needs the media branch's loader (core/image)")
        loader = one(root, LOADER)
        loader_text = loader.read_text(encoding="utf-8")
        pattern = r"internal const val DECODES_AT_ONCE = \d+"
        if len(re.findall(pattern, loader_text)) != 1:
            raise CheckError(f"{loader}: expected one DECODES_AT_ONCE")
        texts[loader] = re.sub(pattern, f"internal const val DECODES_AT_ONCE = {decodes}", loader_text)
    return texts


def write_assets(root):
    """200 distinct 1600 x 900 heroes and 20 square faces, as JPEGs the real decoders read from the APK."""
    folder = root / ASSETS
    folder.mkdir(parents=True, exist_ok=True)
    jobs = (
        ("testsrc2=size=1600x900:rate=1", BENCH_ROWS, "hero_%03d.jpg"),
        ("testsrc2=size=256x256:rate=1", BENCH_FACES, "face_%02d.jpg"),
    )
    for source, frames, name in jobs:
        subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "lavfi", "-i", source,
             "-frames:v", str(frames), "-q:v", "4", str(folder / name)],
            check=True,
        )
    count = len(list(folder.glob("*.jpg")))
    if count != BENCH_ROWS + BENCH_FACES:
        raise CheckError(f"{folder}: {count} pictures, expected {BENCH_ROWS + BENCH_FACES}")


def bench(args):
    root = pathlib.Path(args.project)
    texts = bench_texts(root, 1 if args.check and (root / "core" / "image").is_dir() else args.decodes)
    if args.check:
        print(f"{root}: the bench patch applies to {len(texts)} files")
        return 0
    for path, text in texts.items():
        path.write_text(text, encoding="utf-8", newline="\n")
    write_assets(root)
    print(f"{root}: bench patch applied to {len(texts)} files, {BENCH_ROWS + BENCH_FACES} pictures in {ASSETS}")
    return 0


# --- Sessions -----------------------------------------------------------------------------------------------

def load_perf(harness):
    sys.path.insert(0, os.path.abspath(harness))
    return importlib.import_module("perf")


def arm_apks(pairs, names):
    apks = {}
    for pair in pairs:
        name, _, path = pair.partition("=")
        if not os.path.isfile(path):
            raise CheckError(f"--apk {pair}: no such file")
        apks[name] = path
    if sorted(apks) != sorted(names):
        raise CheckError(f"the session needs --apk for exactly {', '.join(names)}")
    return apks


class Session:
    def __init__(self, perf, args):
        self.perf = perf
        self.args = args
        self.device = perf.Device(args.serial)
        self.device.check_attached()

    def fresh(self, arm, apk, out, index):
        """A cold app, as every run here starts: installed, cleared, compiled as Play would, launched."""
        perf, device = self.perf, self.device
        app = perf.App(device, self.args.package, {})
        run = perf.Run(device, app, os.path.join(out, arm), index, seed=0)
        device.wake()
        info = {
            "arm": arm,
            "apk": os.path.abspath(apk),
            "battery_c": perf.wait_until_cool(device),
            "thermal": device.shell("dumpsys thermalservice | grep -m1 'Thermal Status'").strip(),
            "started": time.strftime("%Y-%m-%d %H:%M:%S"),
        }
        app.install(apk)
        app.clear()
        app.compile_with_apk_profile()
        device.shell("input keyevent KEYCODE_HOME")
        app.launch()
        return app, run, info

    def open_feed(self, run):
        _, tabs = run.wait_for(lambda tree: tree.tabs(), "the tab bar (tab_0)", timeout=30)
        named = [tab for tab in tabs if "feed" in tab.text.lower()]
        if not named and len(tabs) <= self.args.feed_tab:
            run.fail("no Feed tab")
        tab = named[0] if named else tabs[self.args.feed_tab]
        self.device.shell(run.tap(tab))

    def swipes(self, durations, apart):
        width, height = self.device.size
        x, low, high = width // 2, int(height * 0.7), int(height * 0.3)
        return "; sleep {:g}; ".format(apart).join(f"input swipe {x} {low} {x} {high} {ms}" for ms in durations)

    def measure(self, run, name, command, settle):
        gfx, frames = run.segment(name, command, settle)
        run.record_gfx(name, [gfx], [frames])

    def memory(self, run, name):
        output = self.device.shell(f"dumpsys meminfo {self.args.package}")
        run.save(f"{name}_meminfo.txt", output)
        reading = self.perf.parse_meminfo(output) or {}
        for key, value in reading.items():
            run.record(f"{name}.{key}", value)

    def background(self, command, stdin_bytes=None):
        """Starts [command] on the phone and returns at once; [stdin_bytes] is its whole input, as Perfetto's
        config is (a binary pipe: a text one on Windows would turn each line end into CRLF)."""
        process = subprocess.Popen(
            ["adb"] + (["-s", self.args.serial] if self.args.serial else []) + ["shell", command],
            stdin=subprocess.PIPE if stdin_bytes is not None else subprocess.DEVNULL,
            stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        if stdin_bytes is not None:
            process.stdin.write(stdin_bytes)
            process.stdin.close()
        return process

    def pull(self, remote, local):
        self.device.adb("pull", remote, local, check=False)
        self.device.shell(f"rm -f {remote}")
        return os.path.isfile(local) and os.path.getsize(local) > 0


def write_run(run, info):
    path = os.path.join(run.out, "summary.json")
    with open(path, "w", encoding="utf-8") as handle:
        json.dump({"info": info, "metrics": run.metrics, "notes": run.notes}, handle, indent=2, sort_keys=True)
    print(f"[{info['arm']} run{run.index}] {path}", flush=True)
    return run.metrics


def medians(results):
    """{arm: [metrics of each run]} -> {arm: {metric: median}} over the numeric metrics every run has."""
    table = {}
    for arm, runs in results.items():
        keys = set.intersection(*(set(metrics) for metrics in runs)) if runs else set()
        table[arm] = {
            key: round(statistics.median(metrics[key] for metrics in runs), 2)
            for key in sorted(keys)
            if all(isinstance(metrics[key], (int, float)) and not isinstance(metrics[key], bool) for metrics in runs)
        }
    return table


def write_table(out, name, results):
    table = medians(results)
    with open(os.path.join(out, f"{name}-medians.json"), "w", encoding="utf-8") as handle:
        json.dump({"medians": table, "runs": results}, handle, indent=2, sort_keys=True)
    print(json.dumps(table, indent=2, sort_keys=True))


P3_FLINGS = (80, 80, 80, 150, 150, 150, 300, 300, 300)


def p3_run(session, arm, apk, index, kind=None):
    perf, device = session.perf, session.device
    app, run, info = session.fresh(arm, apk, session.args.out, index)
    session.open_feed(run)
    run.wait_for(lambda tree: tree.find(BENCH_TAG), f"the bench list ({BENCH_TAG}): is this a bench build?")
    time.sleep(2.0)
    command = session.swipes(P3_FLINGS, 1.2)
    capture = None
    if kind == "record":
        remote = "/data/local/tmp/media-p3.mp4"
        capture = session.background(f"screenrecord --bit-rate 16000000 --time-limit 25 {remote}")
        time.sleep(1.0)
    elif kind == "trace":
        remote = "/data/misc/perfetto-traces/media-p3.perfetto-trace"
        config = perf.PERFETTO_CONFIG.replace("PACKAGE", session.args.package).encode()
        capture = session.background(f"perfetto --txt -c - -o {remote}", stdin_bytes=config)
        time.sleep(2.0)
    session.measure(run, "p3_flings", command, 1.5)
    time.sleep(1.5)
    session.memory(run, "p3_rest")
    device.shell("input keyevent KEYCODE_HOME")
    time.sleep(10.0)
    session.memory(run, "p3_home")
    if capture:
        capture.wait(timeout=60)
        local = os.path.join(run.out, "recording.mp4" if kind == "record" else "trace.perfetto-trace")
        info[kind] = local if session.pull(remote, local) else "missing"
    info["kind"] = kind or "measured"
    app.stop()
    return write_run(run, info)


def p3(args):
    perf = load_perf(args.harness)
    apks = arm_apks(args.apk, ("B0", "B4", "B2", "B1"))
    session = Session(perf, args)
    order = ("B0", "B4", "B2", "B1")
    results = {arm: [] for arm in order}
    with session.device.upright():
        for index in range(1, args.runs + 1):
            for arm in order:
                results[arm].append(p3_run(session, arm, apks[arm], index))
        for arm in order:
            p3_run(session, arm, apks[arm], args.runs + 1, kind="record")
            p3_run(session, arm, apks[arm], args.runs + 2, kind="trace")
    write_table(args.out, "p3", results)
    return 0


P7_SCENES = {
    "single": ((80,) * 5, 3.5, 25),
    "chained": ((80,) * 10, 0.45, 20),
}


def p7_run(session, arm, apk, scene, index):
    perf, device = session.perf, session.device
    app, run, info = session.fresh(arm, apk, os.path.join(session.args.out, scene), index)
    info["ping"] = device.shell("ping -c 3 -W 2 jsonplaceholder.typicode.com").strip().splitlines()[-1:]
    session.open_feed(run)
    # The first page is in when a card's button shows: the redacted cards hide everything they draw.
    run.wait_for(lambda tree: any(node.text.strip().lower() == "show more" for node in tree.nodes()),
                 "the feed's first page", timeout=30)
    time.sleep(1.0)
    durations, apart, limit = P7_SCENES[scene]
    remote = "/data/local/tmp/media-p7.mp4"
    recorder = session.background(f"screenrecord --bit-rate 16000000 --time-limit {limit} {remote}")
    time.sleep(1.0)
    info["flicks_from_host_s"] = round(time.time(), 3)
    session.measure(run, f"p7_{scene}", session.swipes(durations, apart), 4.0)
    recorder.wait(timeout=limit + 30)
    local = os.path.join(run.out, "recording.mp4")
    info["recording"] = local if session.pull(remote, local) else "missing"
    info["scene"] = scene
    app.stop()
    return write_run(run, info)


def p7(args):
    perf = load_perf(args.harness)
    apks = arm_apks(args.apk, ("A", "B"))
    session = Session(perf, args)
    for scene in P7_SCENES:
        results = {"A": [], "B": []}
        with session.device.upright():
            for index in range(1, args.runs + 1):
                for arm in ("A", "B"):
                    results[arm].append(p7_run(session, arm, apks[arm], scene, index))
        write_table(args.out, f"p7-{scene}", results)
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    commands = parser.add_subparsers(dest="command", required=True)
    patch = commands.add_parser("bench")
    patch.add_argument("project")
    patch.add_argument("--decodes", type=int)
    patch.add_argument("--check", action="store_true")
    for name in ("p3", "p7"):
        session = commands.add_parser(name)
        session.add_argument("--harness", required=True)
        session.add_argument("--out", required=True)
        session.add_argument("--apk", action="append", default=[], metavar="ARM=PATH")
        session.add_argument("--package", default="com.example.everything.dev")
        session.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
        session.add_argument("--runs", type=int, default=3)
        session.add_argument("--feed-tab", type=int, default=1, help="the Feed tab's index when no tab is named Feed")
    args = parser.parse_args(argv)
    try:
        if args.command == "bench":
            return bench(args)
        os.makedirs(args.out, exist_ok=True)
        return p3(args) if args.command == "p3" else p7(args)
    except CheckError as error:
        print(f"media-lists: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
