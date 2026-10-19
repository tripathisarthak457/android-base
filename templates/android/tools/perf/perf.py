"""Device measurements for the app: the work behind perf.sh and record.sh.

    perf.sh --package <applicationId> (--apk <path> | --variant devBenchmarkRelease|devRelease)
            [--runs 3] [--out DIR] [--extra key=value]... [--gate <column> [--gates FILE]
            [--baseline summary.json]] [--seed N] [--compile speed-profile|speed] [--serial ID] <scenario>...

One invocation measures only an app it installed itself. Each run installs the APK, clears the app,
installs its baseline profile and compiles it the way Play does, launches it with the dev launch
options (`perf.fakeSession=true` is always passed, so onboarding and sign-in are skipped), visits every
tab once (recorded as `tab_first_visit`), then runs the scenarios. The median of the runs is what the
summary reports and what `--gate` checks. While it runs (and while record.sh records), the display stays in
its natural rotation and on while plugged in; the owner's settings come back at the end.

UI is found by resource id (`Modifier.testTag` with `testTagsAsResourceId` at the root): `tab_<i>`,
`primary_list`, `related_row`. Smoothness is only meaningful on release builds compiled with their
profile, on a real phone; debug builds are an order of magnitude jankier.
"""

import argparse
import contextlib
import json
import os
import random
import re
import shlex
import shutil
import statistics
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import check_gates  # noqa: E402
import check_nav_log  # noqa: E402
import parse_framestats  # noqa: E402
import parse_gfx  # noqa: E402

# The order scenarios run in, whatever order they are given: everything that keeps the process
# alive first, then memory after the journey, then the scenarios that kill or recompile the app.
SCENARIOS = [
    "tabs", "tabs_tap", "tab1", "pushpop", "pushpop_gesture", "back_during_push", "back_card_held", "rapid_back",
    "scroll_root", "tabs_large", "pushpop_large", "listdetail_select", "scroll_large", "sidebar_toggle",
    "idle", "nav_stress", "restore", "meminfo", "scroll_minimize", "recompose", "trace",
    "coldstart", "coldstart_installed",
]
# Scenarios that measure the large-window layouts. They need a window of at least Expanded width, where
# the shell shows a rail or the sidebar and a list has its detail beside it.
LARGE_SCENARIOS = {"tabs_large", "pushpop_large", "listdetail_select", "scroll_large", "sidebar_toggle"}
WIDTH_CLASSES = ["Compact", "Medium", "Expanded", "Large", "ExtraLarge"]
LARGE_MIN_WIDTH = "Expanded"
JOURNEYS = [
    "tab_switch", "push", "pop", "pushpop", "gesture_pop", "rapid_back", "scroll", "back_slow_commit",
    "back_slow_cancel", "back_fast_flick", "back_y_follow", "back_to_home", "back_during_push", "custom",
]

PROFILE_RECEIVER = "androidx.profileinstaller.ProfileInstallReceiver"
PROFILE_ACTION = "androidx.profileinstaller.action.INSTALL_PROFILE"
MAX_BATTERY_C = 37.0
# Long enough for a press feedback to settle, short of the 400 ms that makes a press a long press.
PRESS_SETTLE_S = 0.25


# The settings a run holds while it measures (Device.upright), as `settings get` names them.
HELD_SETTINGS = ("system accelerometer_rotation", "system user_rotation", "global stay_on_while_plugged_in")


class HarnessError(Exception):
    """The run could not measure what it was asked to; the message says why."""


def write_text(path, text):
    """Writes [text] with LF line ends on every host. A text-mode write on Windows turns each newline
    into CRLF, and summaries from two hosts would then differ in every line."""
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)


def require_tools(tools, purpose):
    """Fails before anything is installed when a host tool [purpose] needs is not on PATH."""
    missing = [tool for tool in tools if shutil.which(tool) is None]
    if missing:
        raise HarnessError(f"{purpose} needs {' and '.join(missing)} on PATH")


class Device:
    def __init__(self, serial):
        self.serial = serial
        self._size = None

    def adb(self, *args, timeout=180, check=True):
        command = ["adb"] + (["-s", self.serial] if self.serial else []) + list(args)
        # The device writes UTF-8 (labels in any language); a Windows host's default codec is not.
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout, encoding="utf-8",
                                errors="replace")
        if check and result.returncode != 0:
            raise HarnessError(f"{' '.join(command)} failed: {result.stderr.strip() or result.stdout.strip()}")
        return result.stdout

    def shell(self, command, timeout=180):
        return self.adb("shell", command, timeout=timeout, check=False)

    def check_attached(self):
        state = subprocess.run(
            ["adb"] + (["-s", self.serial] if self.serial else []) + ["get-state"],
            capture_output=True, text=True,
        ).stdout.strip()
        if state != "device":
            raise HarnessError(f"device {self.serial or '(default)'} is not attached")

    @property
    def size(self):
        if self._size is None:
            output = self.shell("wm size")
            match = re.search(r"Override size: (\d+)x(\d+)", output) or re.search(r"(\d+)x(\d+)", output)
            if not match:
                raise HarnessError(f"cannot read the screen size: {output.strip()}")
            self._size = (int(match.group(1)), int(match.group(2)))
        return self._size

    def prop(self, name):
        return self.shell(f"getprop {name}").strip()

    def battery_celsius(self):
        match = re.search(r"temperature: (\d+)", self.shell("dumpsys battery"))
        return int(match.group(1)) / 10 if match else None

    def wake(self):
        """Turns the screen on; a locked phone stops the run, since nothing can be measured behind it."""
        self.shell("input keyevent KEYCODE_WAKEUP")
        time.sleep(0.5)
        if "isKeyguardShowing=true" in self.shell("dumpsys window | grep -m1 isKeyguardShowing"):
            raise HarnessError("the phone is locked: unlock it (and keep the screen on) before measuring")

    @contextlib.contextmanager
    def upright(self):
        """Holds the display in its natural rotation, with the screen on while plugged in, and gives the
        owner's settings back afterwards. A phone left on its side with auto-rotate on would otherwise
        measure and record the landscape layout, with taps and swipes aimed at the portrait one."""
        saved = {key: self.shell(f"settings get {key}").strip() for key in HELD_SETTINGS}
        try:
            self.shell("settings put system accelerometer_rotation 0; settings put system user_rotation 0; "
                       "settings put global stay_on_while_plugged_in 7")
            time.sleep(0.5)
            yield
        finally:
            for key, value in saved.items():
                self.shell(f"settings delete {key}" if value in ("", "null") else f"settings put {key} {value}")

    def ui(self, attempts=4):
        """The current UI hierarchy. Retried: uiautomator refuses while the screen keeps animating, and
        now and then never answers at all."""
        last = ""
        for _ in range(attempts):
            try:
                output = self.adb("exec-out", "uiautomator dump /dev/tty", check=False, timeout=30)
            except subprocess.TimeoutExpired:
                # Ending the host's adb leaves the device's dump running, holding the one UiAutomation
                # connection the next dump needs. A device too busy to answer that either gets the
                # next attempt anyway.
                try:
                    self.shell("pkill -f uiautomator", timeout=15)
                except subprocess.TimeoutExpired:
                    pass
                last = "no answer in 30 s"
                time.sleep(1.0)
                continue
            end = output.rfind("</hierarchy>")
            if end >= 0:
                start = output.find("<?xml")
                return UiTree(output[max(start, 0): end + len("</hierarchy>")])
            last = output.strip()
            time.sleep(1.0)
        raise HarnessError(f"uiautomator could not dump the screen: {last}")

    def screenshot(self, path):
        with open(path, "wb") as handle:
            subprocess.run(
                ["adb"] + (["-s", self.serial] if self.serial else []) + ["exec-out", "screencap", "-p"],
                stdout=handle, timeout=30,
            )


class Node:
    def __init__(self, element):
        self.resource_id = element.get("resource-id") or ""
        self.clickable = element.get("clickable") == "true"
        self.text = element.get("text") or element.get("content-desc") or ""
        left, top, right, bottom = map(int, re.findall(r"-?\d+", element.get("bounds", "[0,0][0,0]")))
        self.bounds = (left, top, right, bottom)
        self.element = element

    @property
    def center(self):
        left, top, right, bottom = self.bounds
        return (left + right) // 2, (top + bottom) // 2

    @property
    def height(self):
        return self.bounds[3] - self.bounds[1]


class UiTree:
    def __init__(self, xml):
        self.xml = xml
        self.root = ET.fromstring(xml)

    def nodes(self):
        return [Node(element) for element in self.root.iter("node")]

    def find(self, resource_id):
        for node in self.nodes():
            if node.resource_id == resource_id:
                return node
        return None

    def tabs(self):
        found = {}
        for node in self.nodes():
            match = re.fullmatch(r"tab_(\d+)", node.resource_id)
            if match:
                found[int(match.group(1))] = node
        return [found[index] for index in sorted(found)]

    def rows(self, list_id="primary_list"):
        """The clickable rows wholly inside the list, top to bottom."""
        holder = None
        for element in self.root.iter("node"):
            if element.get("resource-id") == list_id:
                holder = element
                break
        if holder is None:
            return []
        bounds = Node(holder).bounds
        rows = []
        for element in holder.iter("node"):
            if element is holder:
                continue
            node = Node(element)
            inside = node.bounds[1] >= bounds[1] and node.bounds[3] <= bounds[3]
            if node.clickable and inside and node.height > 40:
                rows.append(node)
        return rows

    def first_row(self, list_id="primary_list"):
        """The first clickable row wholly inside the list, or None."""
        rows = self.rows(list_id)
        return rows[0] if rows else None


class App:
    def __init__(self, device, package, extras, compile_mode="speed-profile"):
        self.device = device
        self.package = package
        self.compile_mode = compile_mode
        self.extras = dict(extras)
        self.extras.setdefault("perf.fakeSession", "true")
        self._activity = None

    @property
    def activity(self):
        if self._activity is None:
            output = self.device.shell(
                f"cmd package resolve-activity --brief -a android.intent.action.MAIN "
                f"-c android.intent.category.LAUNCHER {self.package}"
            ).strip().splitlines()
            component = output[-1].strip() if output else ""
            if "/" not in component:
                raise HarnessError(f"{self.package} has no launcher activity: is it installed?")
            self._activity = component
        return self._activity

    def extra_args(self, extras=None):
        values = extras if extras is not None else self.extras
        return " ".join(f"--es {shlex.quote(key)} {shlex.quote(value)}" for key, value in values.items())

    def launch(self, wait=True):
        """Starts the app the way the launcher does; returns TotalTime in ms with wait."""
        output = self.device.shell(
            f"am start {'-W ' if wait else ''}-a android.intent.action.MAIN "
            f"-c android.intent.category.LAUNCHER -f 0x10200000 -n {self.activity} {self.extra_args()}"
        )
        match = re.search(r"TotalTime: (\d+)", output)
        return int(match.group(1)) if match else None

    def stop(self):
        self.device.shell(f"am force-stop {self.package}")

    def pid(self):
        return self.device.shell(f"pidof {self.package}").strip()

    def in_front(self):
        output = self.device.shell("dumpsys activity activities | grep -m1 -E 'topResumedActivity|mResumedActivity'")
        return f"{self.package}/" in output

    def install(self, apk):
        self.stop()
        self.device.adb("install", "-r", "-t", "-d", apk, timeout=300)
        if self.package not in self.device.shell(f"pm list packages {self.package}"):
            raise HarnessError(f"{apk} did not install {self.package}: is --package the APK's application id?")
        self._activity = None

    def clear(self):
        self.device.shell(f"pm clear {self.package}")

    def compile_with_apk_profile(self):
        """What Play does on install: compile with the profile the APK ships, and nothing else."""
        self.device.shell(
            f"am broadcast -a {PROFILE_ACTION} -n {self.package}/{PROFILE_RECEIVER}"
        )
        self.stop()
        self.device.shell(f"cmd package compile -f -m {self.compile_mode} {self.package}", timeout=600)

    def compile_with_runtime_profile(self):
        self.stop()
        self.device.shell(f"cmd package compile -f -m {self.compile_mode} {self.package}", timeout=600)

    def reset_compilation(self):
        self.stop()
        self.device.shell(f"cmd package compile --reset {self.package}", timeout=600)


class Run:
    def __init__(self, device, app, out, index, seed):
        self.device = device
        self.app = app
        self.out = os.path.join(out, f"run{index}")
        os.makedirs(self.out, exist_ok=True)
        self.index = index
        self.seed = seed
        self.metrics = {}
        self.notes = []
        # Scenarios the app's shape rules out (one tab has no tab switch), with the reason.
        self.inapplicable = {}
        self.tabs = []
        self.counter = 0

    # Helpers

    def save(self, name, text):
        write_text(os.path.join(self.out, name), text)

    def segment(self, name, command, settle):
        """Resets frame stats, runs one shell command, waits, and dumps; returns (gfx, frames)."""
        self.counter += 1
        package = self.app.package
        self.device.shell(f"dumpsys gfxinfo {package} reset >/dev/null; {command}" if command
                          else f"dumpsys gfxinfo {package} reset >/dev/null")
        time.sleep(settle)
        raw = self.device.shell(f"dumpsys gfxinfo {package} framestats")
        self.save(f"{name}_{self.counter:03d}.txt", raw)
        return parse_gfx.parse(raw), parse_framestats.load(raw)

    def tap(self, node):
        x, y = node.center
        return f"input tap {x} {y}"

    def press(self, node):
        """Puts a finger down on [node] and returns the shell command that lifts it.

        A measured tap runs only the lift: the frames of the press feedback come before the frame
        reset, so the first frame measured is the one the tap opens, not a press highlight.
        """
        x, y = node.center
        output = self.device.shell(f"input motionevent DOWN {x} {y}")
        if "rror" in output or "nknown" in output:
            return self.tap(node)
        time.sleep(PRESS_SETTLE_S)
        return f"input motionevent UP {x} {y}"

    def ui(self):
        return self.device.ui()

    def fail(self, message):
        stamp = f"run{self.index}_{self.counter:03d}"
        # What the app logged is kept with the screen: a step that waits on a crashed app, or on one
        # that Back took to the launcher, fails the same way as a step that waits on a slow screen.
        self.save(f"failure_{stamp}.log", "\n".join(self.logcat()))
        if not self.app.pid():
            message += f" (the app is not running: see the crash in failure_{stamp}.log)"
        elif not self.app.in_front():
            message += " (the app is no longer in front)"
        try:
            self.device.screenshot(os.path.join(self.out, f"failure_{stamp}.png"))
            self.save(f"failure_{stamp}.xml", self.device.ui().xml)
        except HarnessError:
            pass
        raise HarnessError(message)

    def wait_for(self, predicate, what, timeout=20.0):
        deadline = time.time() + timeout
        while time.time() < deadline:
            tree = self.ui()
            found = predicate(tree)
            if found:
                return tree, found
            time.sleep(0.8)
        self.fail(f"{what} did not appear within {timeout:.0f} s")

    def at_root(self, tab=0):
        """Selects [tab] and returns it to its root list, then waits for rows."""
        node = self.tabs[tab]
        # Reselecting a tab pops it to its root; a second tap leaves a root list where it is.
        self.device.shell(f"{self.tap(node)}; sleep 0.6; {self.tap(node)}")
        time.sleep(1.0)
        _, row = self.wait_for(lambda tree: tree.first_row(), "a row of primary_list")
        return row

    def record(self, key, value):
        self.metrics[key] = value

    def record_spread(self, key, values):
        values = [v for v in values if v is not None]
        if values:
            self.record(f"{key}.median", round(statistics.median(values), 2))
            self.record(f"{key}.max", round(max(values), 2))

    def record_gfx(self, scenario, dumps, frames=None):
        combined = parse_gfx.combine(dumps)
        for name in ("janky_pct", "p50_ms", "p90_ms", "p99_ms", "gpu_p50_ms", "gpu_p90_ms", "gpu_p99_ms", "frames"):
            self.record(f"{scenario}.{name}", combined[name])
        if frames:
            self.record_frames(scenario, frames)
            self.record(f"{scenario}.janky_after_first_pct", janky_after_first_pct(dumps, frames))

    def record_frames(self, scenario, frames):
        """The hitch ratio and the render-thread, GPU and buffer cost of each transition's first frames."""
        self.record(f"{scenario}.hitch_ms_per_s", parse_framestats.hitch_ratio(frames))
        late, transitions = parse_framestats.late_first_frames(frames)
        self.record(f"{scenario}.late_first_frames", late)
        self.record(f"{scenario}.transitions", transitions)
        first = [stage for rows in frames for stage in parse_framestats.stages(rows)]
        self.record_spread(f"{scenario}.first3_issue_ms", [stage["issue"] for stage in first])
        self.record_spread(f"{scenario}.first3_gpu_ms", [stage["gpu"] for stage in first])
        self.record_spread(f"{scenario}.first3_dequeue_ms", [stage["dequeue"] for stage in first])

    # Start

    def start(self, apk):
        """Installs, then pays every first use twice: cold, right after `pm clear` wiped the shader
        cache and everything else the app keeps, and warm, after a restart that kept them, which is
        what a user who has opened the app before sees."""
        self.device.wake()
        self.app.install(apk)
        self.app.clear()
        self.app.compile_with_apk_profile()
        self.device.shell("input keyevent KEYCODE_HOME")
        self.app.launch()
        self.first_use("")
        self.app.stop()
        time.sleep(1.0)
        self.app.launch()
        self.first_use("_warm")
        self.at_root(0)

    def first_use(self, suffix):
        _, self.tabs = self.wait_for(lambda t: t.tabs(), "the tab bar (tab_0)", timeout=30)
        _, row = self.wait_for(lambda t: t.first_row(), "a row of primary_list on the first tab")
        first_visits = []
        for index in range(1, len(self.tabs)):
            _, frames = self.segment(f"tab_first_visit{suffix}", self.press(self.tabs[index]), 1.5)
            first = parse_framestats.first_frame(frames)
            first_visits.append(first["ui_ms"] if first else None)
        self.record_spread(f"tab_first_visit{suffix}.first_frame_ms", first_visits)
        row = self.at_root(0)
        _, frames = self.segment(f"first_push{suffix}", self.press(row), 1.3)
        opening = parse_framestats.opening_frame(frames)
        self.record(f"first_push{suffix}.frame_ms", round(parse_framestats.ui_ms(opening), 2) if opening else None)
        self.device.shell("input keyevent 4")
        time.sleep(1.3)
        # The back card draws a screen scaled, clipped to round corners and faded, which takes
        # shaders nothing else draws with; the render thread compiles them on the first swipe after
        # `pm clear`, for up to 70 ms. Paid here, that is reported as a first use instead of landing
        # in pushpop_gesture's janky frames.
        width, height = self.device.size
        self.device.shell(self.tap(row))
        time.sleep(1.3)
        gfx, _ = self.segment(f"first_gesture{suffix}", gesture_swipe(width, height), 1.3)
        self.record(f"first_gesture{suffix}.janky", gfx["janky"])

    # Scenarios

    def tabs_scenario(self, scenario="tabs"):
        """Seven rounds of switches through every tab. `tabs` puts the finger down first and measures
        from the lift; `tabs_tap` sends plain taps, so whatever the press itself costs is counted."""
        if len(self.tabs) < 2:
            self.inapplicable[scenario] = "the app has one tab"
            return
        self.at_root(0)
        order = list(range(1, len(self.tabs))) + [0]
        dumps, firsts, all_frames = [], [], []
        for _ in range(3):
            for index in order:
                node = self.tabs[index]
                command = self.tap(node) if scenario == "tabs_tap" else self.press(node)
                gfx, frames = self.segment(scenario, command, 0.9)
                dumps.append(gfx)
                all_frames.append(frames)
                first = parse_framestats.first_frame(frames)
                firsts.append(first["ui_ms"] if first else None)
        self.record_gfx(scenario, dumps, all_frames)
        self.record_spread(f"{scenario}.first_frame_ms", firsts)

    def tab1(self):
        if len(self.tabs) < 2:
            self.inapplicable["tab1"] = "the app has one tab"
            return
        self.at_root(0)
        firsts = []
        for _ in range(6):
            for index in (1, 0):
                _, frames = self.segment("tab1", self.press(self.tabs[index]), 1.6)
                first = parse_framestats.first_frame(frames)
                firsts.append(first["ui_ms"] if first else None)
        self.record_spread("tab1.first_frame_ms", firsts)

    def pushpop(self, scenario="pushpop"):
        """Eight pushes of the first row, each popped with the Back key. In a window with room for two
        panes the row opens its detail beside the list, and Back closes it again."""
        row = self.at_root(0)
        dumps, pushes, pops, all_frames = [], [], [], []
        for _ in range(8):
            gfx, frames = self.segment("push", self.press(row), 1.3)
            dumps.append(gfx)
            all_frames.append(frames)
            opening = parse_framestats.opening_frame(frames)
            pushes.append(round(parse_framestats.ui_ms(opening), 2) if opening else None)
            gfx, frames = self.segment("pop", "input keyevent 4", 1.3)
            dumps.append(gfx)
            all_frames.append(frames)
            pops.append((parse_framestats.first_frame(frames) or {}).get("ui_ms"))
        self.record_gfx(scenario, dumps, all_frames)
        self.record_spread(f"{scenario}.push_first_frame_ms", pushes)
        self.record_spread(f"{scenario}.pop_first_frame_ms", pops)

    def pushpop_gesture(self):
        """Push with a tap, pop with an edge swipe. The swipe's first frames are where the back card
        starts: the screen beneath must already be laid out, so they cost a layer change."""
        row = self.at_root(0)
        width, height = self.device.size
        swipe = gesture_swipe(width, height)
        dumps, starts, all_frames = [], [], []
        for _ in range(6):
            gfx, frames = self.segment("gesture_push", self.tap(row), 1.3)
            dumps.append(gfx)
            all_frames.append(frames)
            gfx, frames = self.segment("gesture_pop", swipe, 1.3)
            dumps.append(gfx)
            all_frames.append(frames)
            starts.append(parse_framestats.max_ui_ms(frames[:3]))
        self.record_gfx("pushpop_gesture", dumps, all_frames)
        self.record_spread("pushpop_gesture.gesture_first_frame_ms", starts)

    def back_during_push(self):
        """A back swipe that starts while a push is still sliding in, 60 to 120 ms after the tap,
        from each edge in turn.

        The screenshot taken mid-gesture must show the arriving screen as one opaque card over the
        list. Two screens blended into each other (double exposure) leave the middle of the window
        about half as different from the list as the detail screen itself is; an opaque card leaves
        it as different. The release must land on the list.
        """
        shots = os.path.join(self.out, "back_during_push")
        rest_list, apart = self.list_and_detail(shots, "back_during_push")
        if rest_list is None:
            return
        width, height = self.device.size
        ratios, landed = [], []
        y = int(height * BACK_CHECK_HEIGHT)
        for edge in ("left", "right", "left", "right"):
            row = self.at_root(0)
            start = 2 if edge == "left" else width - 3
            step = (1 if edge == "left" else -1) * width * 35 // 100 // 8
            moves = "; ".join(f"input motionevent MOVE {start + step * i} {y}" for i in range(1, 9))
            # Not shared storage: the media scanner picks up an image there, and a gallery app can then
            # run its own GPU and CPU work on it for seconds, inside the scenarios measured next.
            remote = "/data/local/tmp/perf_back_during_push.png"
            self.device.shell(
                f"{self.tap(row)}; input motionevent DOWN {start} {y}; {moves}; "
                f"screencap -p {remote}; input motionevent UP {start + step * 8} {y}"
            )
            self.counter += 1
            mid = os.path.join(shots, f"mid_{self.counter:03d}_{edge}.png")
            self.device.adb("pull", remote, mid)
            self.device.shell(f"rm -f {remote}")
            ratio = image_difference(mid, rest_list)
            ratios.append(round(ratio / apart, 2) if ratio is not None else None)
            time.sleep(1.5)
            self.device.screenshot(os.path.join(shots, f"after_{self.counter:03d}_{edge}.png"))
            tree = self.ui() if self.app.in_front() else None
            landed.append(bool(tree and tree.first_row() and tree.find("related_row") is None))
            if not self.app.in_front():
                self.app.launch()
                time.sleep(1.5)
        measured = [ratio for ratio in ratios if ratio is not None]
        self.record("back_during_push.difference_ratios", ratios)
        self.record("back_during_push.opaque", bool(measured) and min(measured) >= OPAQUE_RATIO)
        self.record("back_during_push.lands_on_list", all(landed))

    def back_card_held(self):
        """A back swipe held over a pushed screen that has come to rest, from each edge in turn, the
        screenshot taken while the finger is still down at 45 % of the width: the frame a person
        looks at, and may screenshot, while deciding whether to let go.

        As in back_during_push, the shot must show the pushed screen as one opaque card over the
        list. The finger goes back to the edge before it lifts, so every gesture is cancelled.
        """
        shots = os.path.join(self.out, "back_card_held")
        rest_list, apart = self.list_and_detail(shots, "back_card_held")
        if rest_list is None:
            return
        width, height = self.device.size
        remote = "/data/local/tmp/perf_back_card_held.png"
        ratios = []
        for edge in ("left", "right", "left", "right"):
            row = self.at_root(0)
            self.device.shell(self.tap(row))
            time.sleep(1.5)
            self.device.shell(back_swipe(width, height, edge=edge, y=int(height * BACK_CHECK_HEIGHT),
                                         while_held=f"screencap -p {remote}", cancel=True))
            self.counter += 1
            held = os.path.join(shots, f"held_{self.counter:03d}_{edge}.png")
            self.device.adb("pull", remote, held)
            self.device.shell(f"rm -f {remote}")
            ratio = image_difference(held, rest_list)
            ratios.append(round(ratio / apart, 2) if ratio is not None else None)
            time.sleep(1.0)
            if not self.app.in_front():
                self.app.launch()
                time.sleep(1.5)
        measured = [ratio for ratio in ratios if ratio is not None]
        self.record("back_card_held.difference_ratios", ratios)
        self.record("back_card_held.opaque", bool(measured) and min(measured) >= OPAQUE_RATIO)

    def list_and_detail(self, shots, scenario):
        """Screenshots the root list, and the first row's detail at rest, into [shots]. Returns the
        list's screenshot and how different the two are, or (None, None) when they look too alike
        for a blend of them to be told from either."""
        os.makedirs(shots, exist_ok=True)
        row = self.at_root(0)
        rest_list = os.path.join(shots, "list.png")
        rest_detail = os.path.join(shots, "detail.png")
        self.device.screenshot(rest_list)
        self.device.shell(self.tap(row))
        time.sleep(1.5)
        self.device.screenshot(rest_detail)
        self.device.shell("input keyevent 4")
        time.sleep(1.5)
        apart = image_difference(rest_detail, rest_list)
        if apart is None or apart < MIN_SCREEN_DIFFERENCE:
            self.notes.append(f"{scenario}: the list and the detail look alike ({apart}); nothing to compare")
            return None, None
        return rest_list, apart

    def push_to_depth(self, row):
        """Pushes the first row, then related rows: depth 4 when the detail has them, else 2."""
        self.device.shell(self.tap(row))
        time.sleep(1.3)
        depth = 2
        for _ in range(2):
            related = self.find_related()
            if related is None:
                break
            self.device.shell(self.tap(related))
            time.sleep(1.3)
            depth += 1
        return depth

    def find_related(self):
        width, height = self.device.size
        for attempt in range(3):
            node = self.ui().find("related_row")
            if node is not None and node.bounds[3] <= height and node.bounds[1] >= 0:
                return node
            if attempt < 2:
                self.device.shell(f"input swipe {width // 2} {int(height * 0.7)} {width // 2} {int(height * 0.3)} 200")
                time.sleep(0.8)
        return None

    def rapid_back(self):
        row = self.at_root(0)
        pid = self.app.pid()
        dumps, slowest, landed, alive, depths, all_frames = [], [], [], [], [], []
        for _ in range(6):
            depth = self.push_to_depth(row)
            depths.append(depth)
            gfx, frames = self.segment("rapid_back", "input keyevent 4 4 4", 1.8)
            dumps.append(gfx)
            all_frames.append(frames)
            slowest.append(parse_framestats.max_ui_ms(frames))
            still = self.app.pid() == pid and pid != ""
            alive.append(still)
            in_front = self.app.in_front()
            tree = self.ui() if in_front else None
            at_root = bool(tree and tree.first_row() and tree.find("related_row") is None)
            landed.append(in_front and at_root)
            if not in_front:
                self.notes.append(f"rapid_back from depth {depth}: three Back presses left the app")
                self.app.launch()
                time.sleep(1.5)
            if not still:
                self.notes.append("rapid_back: the process died")
                pid = self.app.pid()
            row = self.at_root(0)
        self.record_gfx("rapid_back", dumps, all_frames)
        self.record_spread("rapid_back.pop_frame_ms", slowest)
        self.record("rapid_back.depth", max(depths))
        self.record("rapid_back.alive", all(alive))
        self.record("rapid_back.final_screen", all(landed))

    def scroll(self, scenario):
        self.at_root(0)
        width, height = self.device.size
        x, low, high = width // 2, int(height * 0.75), int(height * 0.29)
        command = (
            f"for i in 1 2 3 4 5 6; do input swipe {x} {low} {x} {high} 140; sleep 0.7; done; "
            f"for i in 1 2 3 4 5 6; do input swipe {x} {high} {x} {low} 140; sleep 0.7; done"
        )
        gfx, _ = self.segment(scenario, command, 1.0)
        self.record_gfx(scenario, [gfx])

    def require_window(self, scenario):
        """Stops [scenario] unless the shell is laid out for a window at least as wide as it needs: on a
        phone-sized window it would measure the phone layout under a large layout's name."""
        state = check_nav_log.last_state(self.device.shell("logcat -d -s perf.shell:I").splitlines())
        size_class = (state or {}).get("class") or ""
        width = size_class.split("x")[0]
        if width not in WIDTH_CLASSES or WIDTH_CLASSES.index(width) < WIDTH_CLASSES.index(LARGE_MIN_WIDTH):
            window = (state or {}).get("window") or "unknown"
            raise HarnessError(
                f"{scenario} needs a window of {LARGE_MIN_WIDTH} width or wider; the shell is laid out for "
                f"{size_class or 'an unknown class'} ({window}). Run it on a tablet or a resized emulator.")
        self.notes.append(f"{scenario}: {size_class} window ({state.get('window')}), {state.get('chrome')}")

    def listdetail_select(self):
        """Opens each visible row of the list in turn: with room for two panes, every tap swaps the
        detail beside the list in place, which must cost about what a visited tab does."""
        self.at_root(0)
        _, rows = self.wait_for(lambda t: t.rows(), "rows of primary_list")
        dumps, firsts, all_frames = [], [], []
        for _ in range(2):
            for row in rows[:10]:
                gfx, frames = self.segment("listdetail_select", self.press(row), 0.9)
                dumps.append(gfx)
                all_frames.append(frames)
                first = parse_framestats.first_frame(frames)
                firsts.append(first["ui_ms"] if first else None)
        self.record_gfx("listdetail_select", dumps, all_frames)
        self.record_spread("listdetail_select.first_frame_ms", firsts)
        self.device.shell("input keyevent 4")
        time.sleep(1.0)

    def sidebar_toggle(self):
        """Ten folds and openings of the sidebar with Ctrl+Shift+S: only transforms move, so nothing
        but the chrome's own frames should run."""
        self.at_root(0)
        toggle = "input keycombination KEYCODE_CTRL_LEFT KEYCODE_SHIFT_LEFT KEYCODE_S"
        dumps, all_frames = [], []
        for _ in range(10):
            gfx, frames = self.segment("sidebar_toggle", toggle, 0.9)
            dumps.append(gfx)
            all_frames.append(frames)
        self.record_gfx("sidebar_toggle", dumps, all_frames)

    def idle(self):
        frames = []
        for node in self.tabs:
            self.device.shell(self.tap(node))
            time.sleep(3.0)
            gfx, _ = self.segment("idle", "", 5.0)
            frames.append(gfx["frames"])
        self.record("idle.frames", max(frames))
        self.record("idle.frames_per_tab", frames)
        self.device.shell(self.tap(self.tabs[0]))
        time.sleep(1.0)

    def logcat(self):
        return self.device.adb("logcat", "-d", "-v", "brief", "-s",
                               "perf.shell:I", "AndroidRuntime:E", check=False).splitlines()

    def crashed(self, lines):
        return any(f"Process: {self.app.package}, PID" in line for line in lines)

    def nav_stress(self, seconds=60.0):
        row = self.at_root(0)
        rng = random.Random(self.seed + self.index)
        self.device.adb("logcat", "-c", check=False)
        pid = self.app.pid()
        self.device.shell(f"dumpsys gfxinfo {self.app.package} reset")
        deadline = time.time() + seconds
        exits = 0
        while time.time() < deadline:
            steps = []
            for _ in range(10):
                roll = rng.random()
                if roll < 0.35:
                    steps.append(self.tap(rng.choice(self.tabs)))
                elif roll < 0.70:
                    steps.append(self.tap(row))
                else:
                    steps.append("input keyevent" + " 4" * rng.randint(1, 3))
                steps.append(f"sleep {rng.randint(30, 120) / 1000:.3f}")
            self.device.shell("; ".join(steps))
            if self.app.pid() != pid:
                break
            if not self.app.in_front():
                exits += 1
                self.app.launch()
                time.sleep(1.0)
        time.sleep(1.5)
        raw = self.device.shell(f"dumpsys gfxinfo {self.app.package} framestats")
        self.save("nav_stress.txt", raw)
        lines = self.logcat()
        self.save("nav_stress_logcat.txt", "\n".join(lines))
        states, problems = check_nav_log.check(lines)
        if states == 0:
            problems = ["no perf.shell lines: not a dev build with the shell log"]
        self.record_gfx("nav_stress", [parse_gfx.parse(raw)])
        self.record("nav_stress.alive", self.app.pid() == pid and not self.crashed(lines))
        self.record("nav_stress.invariants", not problems)
        self.record("nav_stress.states", states)
        self.record("nav_stress.exits", exits)
        if problems:
            self.notes.append("nav_stress: " + "; ".join(problems[:5]))

    def restore(self):
        row = self.at_root(0)
        self.device.shell(self.tap(row))
        time.sleep(1.3)
        if len(self.tabs) > 1:
            self.device.shell(self.tap(self.tabs[1]))
            time.sleep(1.3)
            second = self.ui().first_row()
            if second is not None:
                self.device.shell(self.tap(second))
            else:
                # The second tab has no list to push from (empty, or still loading): go back to the
                # first, so the tab in front has a pushed screen for Back to pop after the restore.
                self.device.shell(self.tap(self.tabs[0]))
            time.sleep(1.3)
        # The shell logs its state on every change, so the last line logged is the state now.
        before = check_nav_log.last_state(self.logcat())
        self.device.shell("input keyevent KEYCODE_HOME")
        time.sleep(1.5)
        self.device.shell(f"am kill {self.app.package}")
        time.sleep(1.0)
        killed = self.app.pid() == ""
        self.device.adb("logcat", "-c", check=False)
        self.app.launch()
        time.sleep(2.0)
        after = check_nav_log.last_state(self.logcat())
        self.device.shell("input keyevent 4")
        time.sleep(1.5)
        after_back = check_nav_log.last_state(self.logcat())

        def shape(state):
            return state and (state["tab"], state["stacks"], state["presented"])

        restored = killed and before is not None and shape(before) == shape(after)
        popped = False
        if restored and after_back is not None:
            tab = after["tab"]
            popped = (after_back["tab"] == tab
                      and after_back["stacks"][tab] == after["stacks"][tab][:-1])
        if not killed:
            self.notes.append("restore: am kill did not end the process")
        self.save("restore.json", json.dumps({"before": before, "after": after, "after_back": after_back}, indent=2))
        self.record("restore.restored", bool(restored))
        self.record("restore.back_pops", bool(popped))

    def recompose(self):
        """Frames that recomposed during each transition, from an atrace capture of the app's own
        trace sections: a `Choreographer#doFrame` with a `Compose:recompose` inside it. Separate from
        the timing scenarios, because tracing costs time on every frame.

        Each action (a tap, a Back key, a swipe) opens a window that ends at the next action; the
        windows are matched to the actions in the order they were sent.
        """
        # Its tab switches are switches to visited tabs. A tab not opened since the app last started
        # (scroll_minimize restarts it) would count what its screen loads on a first visit instead.
        for node in self.tabs[1:]:
            self.device.shell(self.tap(node))
            time.sleep(1.5)
        row = self.at_root(0)
        width, height = self.device.size
        y = height // 2
        actions = []
        script = []
        for index in list(range(1, len(self.tabs))) + [0]:
            actions.append("tab_switch")
            script.append(self.tap(self.tabs[index]))
        for _ in range(3):
            actions += ["push", "pop"]
            script += [self.tap(row), "input keyevent 4"]
        slow = "; ".join(f"input motionevent MOVE {2 + width * 40 // 100 * i // 12} {y}" for i in range(1, 13))
        for _ in range(2):
            actions += ["push", "back_gesture"]
            script += [self.tap(row), f"input motionevent DOWN 2 {y}; {slow}; input motionevent UP {2 + width * 40 // 100} {y}"]
        package = self.app.package
        remote = "/data/local/tmp/perf_atrace.txt"
        self.device.shell(f"atrace --async_stop >/dev/null 2>&1; atrace --async_start -b 32768 -a {package} gfx view input")
        try:
            time.sleep(1.0)
            for command in script:
                self.device.shell(command)
                time.sleep(1.5)
        finally:
            self.device.shell(f"atrace --async_stop -o {remote}")
        local = os.path.join(self.out, "recompose_atrace.txt")
        self.device.adb("pull", remote, local)
        self.device.shell(f"rm -f {remote}")
        with open(local, errors="replace") as handle:
            windows = recomposing_frames(handle.read(), self.app.pid())
        if len(windows) != len(actions):
            self.notes.append(f"recompose: {len(windows)} input bursts in the trace for {len(actions)} actions")
            return
        by_kind = {}
        for kind, window in zip(actions, windows):
            by_kind.setdefault(kind, []).append(window["recomposing"])
        for kind, counts in by_kind.items():
            self.record(f"recompose.{kind}.max", max(counts))
            self.record(f"recompose.{kind}.runs", counts)
        self.record("recompose.back_gesture_progress.max",
                    max(window["recomposing_while_dragged"] for kind, window in zip(actions, windows) if kind == "back_gesture"))

    def trace(self):
        """A Perfetto capture of tab switches, a push and pop, a back swipe and a scroll down and
        back up the root list, for reading in ui.perfetto.dev: FrameTimeline jank types, GPU and CPU
        frequency, scheduling. Nothing is computed from it here."""
        row = self.at_root(0)
        width, height = self.device.size
        package = self.app.package
        config = PERFETTO_CONFIG.replace("PACKAGE", package)
        remote = "/data/misc/perfetto-traces/perf_trace.perfetto-trace"
        # A binary pipe: a text one on Windows would turn every line end of the config into CRLF.
        process = subprocess.Popen(
            ["adb"] + (["-s", self.device.serial] if self.device.serial else [])
            + ["shell", f"perfetto --txt -c - -o {remote}"],
            stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
        )
        process.stdin.write(config.encode())
        process.stdin.close()
        time.sleep(2.0)
        steps = [self.tap(node) for node in self.tabs[1:]] + [self.tap(self.tabs[0]), self.tap(row), "input keyevent 4",
                 self.tap(row), gesture_swipe(width, height)]
        for command in steps:
            self.device.shell(command)
            time.sleep(1.2)
        self.at_root(0)
        x, low, high = width // 2, int(height * 0.75), int(height * 0.29)
        self.device.shell(
            f"for i in 1 2 3; do input swipe {x} {low} {x} {high} 140; sleep 0.7; done; "
            f"for i in 1 2 3; do input swipe {x} {high} {x} {low} 140; sleep 0.7; done")
        process.wait(timeout=60)
        local = os.path.join(self.out, "trace.perfetto-trace")
        self.device.adb("pull", remote, local, check=False)
        self.device.shell(f"rm -f {remote}")
        self.record("trace.captured", os.path.isfile(local) and os.path.getsize(local) > 0)

    def meminfo(self):
        output = self.device.shell(f"dumpsys meminfo {self.app.package}")
        self.save("meminfo.txt", output)
        memory = parse_meminfo(output)
        if memory is None:
            self.fail("dumpsys meminfo has no TOTAL PSS line")
        for name, value in memory.items():
            self.record(f"meminfo.{name}", value)

    def coldstarts(self, scenario):
        times = []
        for _ in range(6):
            self.app.stop()
            time.sleep(2.0)
            total = self.app.launch()
            if total is not None:
                times.append(total)
            time.sleep(3.0)
        if not times:
            self.fail(f"{scenario}: am start -W reported no TotalTime")
        self.record(f"{scenario}.median_ms", int(statistics.median(times)))
        self.record(f"{scenario}.runs_ms", times)

    def coldstart(self):
        self.app.compile_with_runtime_profile()
        self.coldstarts("coldstart")

    def coldstart_installed(self):
        self.app.reset_compilation()
        self.app.compile_with_apk_profile()
        self.coldstarts("coldstart_installed")

    def scroll_minimize(self):
        self.app.stop()
        self.app.extras["perf.barMinimize"] = "OnScrollDown"
        try:
            self.app.launch()
            self.wait_for(lambda t: t.tabs(), "the tab bar (tab_0)", timeout=30)
            self.scroll("scroll_minimize")
        finally:
            del self.app.extras["perf.barMinimize"]
            self.app.stop()
            self.app.launch()
            self.wait_for(lambda t: t.tabs(), "the tab bar (tab_0)", timeout=30)

    def run(self, scenario):
        handler = {
            "tabs": self.tabs_scenario,
            "tabs_tap": lambda: self.tabs_scenario("tabs_tap"),
            "back_during_push": self.back_during_push,
            "back_card_held": self.back_card_held,
            "recompose": self.recompose,
            "trace": self.trace,
            "tab1": self.tab1,
            "pushpop": self.pushpop,
            "pushpop_gesture": self.pushpop_gesture,
            "rapid_back": self.rapid_back,
            "scroll_root": lambda: self.scroll("scroll_root"),
            "tabs_large": lambda: self.tabs_scenario("tabs_large"),
            "pushpop_large": lambda: self.pushpop("pushpop_large"),
            "listdetail_select": self.listdetail_select,
            "scroll_large": lambda: self.scroll("scroll_large"),
            "sidebar_toggle": self.sidebar_toggle,
            "scroll_minimize": self.scroll_minimize,
            "idle": self.idle,
            "nav_stress": self.nav_stress,
            "restore": self.restore,
            "meminfo": self.meminfo,
            "coldstart": self.coldstart,
            "coldstart_installed": self.coldstart_installed,
        }[scenario]
        if scenario in LARGE_SCENARIOS:
            self.require_window(scenario)
        handler()


# The middle of the window, which the back card covers at any point of a gesture: x from 25 to 75 %,
# y from 30 to 70 %.
_MIDDLE = "crop=iw*0.5:ih*0.4:iw*0.25:ih*0.3,format=gray"
# Below this mean difference (0 to 255) the list and the detail are too alike to tell a blend apart.
MIN_SCREEN_DIFFERENCE = 6.0
# Mid-gesture against the list, as a share of the detail against the list: an opaque card stays near
# 1, a half-and-half blend of the two screens near 0.5.
OPAQUE_RATIO = 0.75
# How far down the window the opacity checks swipe back: where a thumb swipes on a phone held in one
# hand, and clear of the dev tools badge, which docks to the right edge about half-way down and takes
# that edge's back gesture where it lies.
BACK_CHECK_HEIGHT = 0.37

PERFETTO_CONFIG = """
buffers { size_kb: 65536 fill_policy: RING_BUFFER }
data_sources { config { name: "android.surfaceflinger.frametimeline" } }
data_sources { config { name: "linux.process_stats" target_buffer: 0 } }
data_sources {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "sched/sched_switch"
      ftrace_events: "power/cpu_frequency"
      ftrace_events: "power/gpu_frequency"
      ftrace_events: "power/suspend_resume"
      atrace_categories: "gfx"
      atrace_categories: "view"
      atrace_categories: "input"
      atrace_apps: "PACKAGE"
    }
  }
}
duration_ms: 32000
"""


def parse_meminfo(text):
    """TOTAL PSS from `dumpsys meminfo <package>`, and how much of it is graphics, in MB.

    Graphics is mostly the window's own buffers (EGL mtrack), whose size follows the panel and the
    buffer count, not the app: the same on any build of any app on one phone. `pss_app_mb` is the
    rest, the part the app's code decides.
    """
    total = re.search(r"TOTAL PSS:\s+(\d+)", text) or re.search(r"^\s+TOTAL\s+(\d+)", text, re.MULTILINE)
    if not total:
        return None
    graphics = re.search(r"^\s*Graphics:\s+(\d+)", text, re.MULTILINE)
    memory = {"pss_mb": round(int(total.group(1)) / 1024, 1)}
    if graphics:
        memory["graphics_mb"] = round(int(graphics.group(1)) / 1024, 1)
        memory["pss_app_mb"] = round((int(total.group(1)) - int(graphics.group(1))) / 1024, 1)
    return memory


def janky_after_first_pct(dumps, frames):
    """Janky frames other than each transition's first, as a share of all frames.

    [dumps] are gfxinfo dumps and [frames] the framestats rows of the same segments, one transition
    each. A transition's first frame starts from idle, with the CPU and GPU at their lowest clocks,
    and on a phone whose touch boost leaves them there it can miss its deadline whatever it draws.
    This is the rest: the jank of the motion itself.
    """
    total = sum(dump["frames"] for dump in dumps)
    janky = sum(max(0, dump["janky"] - (1 if rows and parse_framestats.missed_deadline(rows[0]) else 0))
                for dump, rows in zip(dumps, frames))
    return round(100 * janky / total, 2) if total else 0.0


def image_difference(first, second):
    """The mean absolute grey-level difference (0 to 255) between the middles of two screenshots."""
    output = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", first, "-i", second, "-filter_complex",
         f"[0:v]{_MIDDLE}[a];[1:v]{_MIDDLE}[b];[a][b]blend=all_mode=difference,signalstats,metadata=print:file=-",
         "-f", "null", "-"],
        capture_output=True, text=True,
    ).stdout
    match = re.search(r"lavfi\.signalstats\.YAVG=([\d.]+)", output)
    return float(match.group(1)) if match else None


_ATRACE_LINE = re.compile(r"-(\d+)\s.*?\s([\d.]+): tracing_mark_write: ([BE])\|(\d+)\|?(.*)$")
# Inputs closer together than this are one action: a tap's down and up, or a swipe's moves.
_ACTION_GAP_S = 0.4


def recomposing_frames(text, pid):
    """One window per action in an atrace dump of the app: how many frames recomposed in it.

    Only the main thread is read (its thread id is the process id). An action is a burst of
    `deliverInputEvent` slices; `recomposing_while_dragged` counts the frames that recomposed
    between the third input of a burst and its last, which for a back swipe are the frames that
    only follow the finger.
    """
    pid = str(pid)
    stack, frames, inputs = [], [], []
    for line in text.splitlines():
        match = _ATRACE_LINE.search(line)
        if not match or match.group(1) != pid:
            continue
        when, kind, name = float(match.group(2)), match.group(3), match.group(5)
        if kind == "B":
            stack.append([name, when, False])
            if name.startswith("Compose:recompose"):
                for open_slice in stack:
                    if open_slice[0].startswith("Choreographer#doFrame"):
                        open_slice[2] = True
            elif name.startswith("deliverInputEvent"):
                inputs.append(when)
        elif stack:
            name, start, recomposed = stack.pop()
            if name.startswith("Choreographer#doFrame") and not any(s[0].startswith("Choreographer#doFrame") for s in stack):
                frames.append((start, recomposed))
    bursts = []
    for when in inputs:
        if bursts and when - bursts[-1][-1] < _ACTION_GAP_S:
            bursts[-1].append(when)
        else:
            bursts.append([when])
    windows = []
    for index, burst in enumerate(bursts):
        end = bursts[index + 1][0] if index + 1 < len(bursts) else float("inf")
        inside = [recomposed for start, recomposed in frames if burst[0] <= start < end]
        dragged_from = burst[2] if len(burst) > 2 else burst[-1]
        dragged = [recomposed for start, recomposed in frames if dragged_from <= start < burst[-1]]
        windows.append({
            "frames": len(inside),
            "recomposing": sum(inside),
            "recomposing_while_dragged": sum(dragged),
        })
    return windows


def combine_runs(runs):
    """{metric: {"value": median or all(), "runs": [...]}} over every run."""
    metrics = {}
    for key in sorted({key for run in runs for key in run}):
        values = [run.get(key) for run in runs]
        present = [value for value in values if value is not None]
        if not present:
            continue
        if all(isinstance(value, bool) for value in present):
            value = all(present)
        elif all(isinstance(value, (int, float)) for value in present):
            value = statistics.median(present)
            value = round(value, 2) if isinstance(value, float) else value
        else:
            value = present[-1]
        metrics[key] = {"value": value, "runs": values}
    return metrics


def combine_inapplicable(runs):
    """The scenarios every run found inapplicable, with the reason. When any run could measure a
    scenario, the other runs' skips are a harness problem, so its rows stay gated."""
    common = set.intersection(*(set(run) for run in runs)) if runs else set()
    return {scenario: runs[0][scenario] for scenario in sorted(common)}


def write_summary(out, info, metrics, notes, gate_results, inapplicable=None):
    summary = dict(info)
    summary["metrics"] = metrics
    summary["inapplicable"] = inapplicable or {}
    summary["notes"] = notes
    write_text(os.path.join(out, "summary.json"), json.dumps(summary, indent=2))

    labels = {row["metric"]: row["label"] for row in check_gates.load_rows(info["gates_file"])}
    lines = [
        f"# perf.sh: {info['package']}",
        "",
        f"- APK: `{info['apk']}`",
        f"- Device: {info['device']['model']} ({info['device']['serial']}), "
        f"battery {info['device']['battery_c']} °C at the start",
        f"- Runs: {info['runs']}, seed {info['seed']}, extras {json.dumps(info['extras'])}, "
        f"compiled `{info.get('compile', 'speed-profile')}`",
        f"- Scenarios: {', '.join(info['scenarios'])}",
        "",
        "| Metric | Median | Runs |",
        "|---|---|---|",
    ]
    for key, entry in metrics.items():
        runs = ", ".join(check_gates._show(value) for value in entry["runs"])
        lines.append(f"| {labels.get(key, key)} | {check_gates._show(entry['value'])} | {runs} |")
    if inapplicable:
        lines += ["", "Not applicable to this app:", ""]
        lines += [f"- {scenario}: {reason}" for scenario, reason in inapplicable.items()]
    if notes:
        lines += ["", "Notes:", ""] + [f"- {note}" for note in notes]
    if gate_results is not None:
        lines += ["", check_gates.table(gate_results, info["gate"])]
    write_text(os.path.join(out, "summary.md"), "\n".join(lines) + "\n")
    return summary


def find_apk(device, variant):
    match = re.fullmatch(r"([a-z0-9]+)([A-Z]\w*)", variant)
    if not match:
        raise HarnessError(f"--variant {variant}: expected a flavour and a build type, like devRelease")
    flavor, build_type = match.group(1), match.group(2)[0].lower() + match.group(2)[1:]
    folder = os.path.join("app", "build", "outputs", "apk", flavor, build_type)
    if not os.path.isdir(folder):
        raise HarnessError(f"{folder} does not exist: build :app:assemble{variant[0].upper()}{variant[1:]} first")
    apks = sorted(name for name in os.listdir(folder) if name.endswith(".apk"))
    abi = device.prop("ro.product.cpu.abi")
    for preference in (abi, "universal"):
        for name in apks:
            if preference and f"-{preference}-" in name:
                return os.path.join(folder, name)
    if len(apks) == 1:
        return os.path.join(folder, apks[0])
    raise HarnessError(f"cannot choose an APK in {folder}: {apks}")


def wait_until_cool(device):
    for _ in range(20):
        celsius = device.battery_celsius()
        if celsius is None or celsius < MAX_BATTERY_C:
            return celsius
        print(f"battery at {celsius} °C; waiting for it to drop under {MAX_BATTERY_C} °C", flush=True)
        time.sleep(30)
    raise HarnessError("the battery stayed above 37 °C for ten minutes; measurements would be throttled")


def parse_extras(pairs):
    extras = {}
    for pair in pairs or []:
        key, sep, value = pair.partition("=")
        if not sep:
            raise HarnessError(f"--extra {pair}: expected key=value")
        extras[key] = value
    return extras


def started_run(device, app, out, index, seed, apk):
    """A run that has installed the app and paid its first uses.

    The first screens of most presets load from a live API, which now and then does not answer
    within a step's wait. That is not what a run measures, so a start that fails is made once more
    from a clean install before the invocation gives up.
    """
    try:
        run = Run(device, app, out, index, seed)
        run.start(apk)
        return run
    except HarnessError as error:
        print(f"run {index}: the start failed ({error}); starting it again", flush=True)
    run = Run(device, app, out, index, seed)
    run.start(apk)
    return run


def measure(args):
    device = Device(args.serial)
    device.check_attached()
    with device.upright():
        return measure_on(device, args)


def measure_on(device, args):
    rows = check_gates.load_rows(args.gates)
    if args.gate and args.gate not in check_gates.columns(rows):
        raise HarnessError(f"no column '{args.gate}' in {args.gates}")
    unknown = [name for name in args.scenarios if name not in SCENARIOS]
    if unknown:
        raise HarnessError(f"unknown scenarios {unknown}; known: {', '.join(SCENARIOS)}")
    for compared in ("back_during_push", "back_card_held"):
        if compared in args.scenarios:
            require_tools(["ffmpeg"], f"{compared} (it compares screenshots)")
    apk = args.apk or find_apk(device, args.variant)
    if not os.path.isfile(apk):
        raise HarnessError(f"{apk} does not exist")
    scenarios = [name for name in SCENARIOS if name in args.scenarios]
    seed = args.seed if args.seed is not None else random.randrange(1_000_000)
    os.makedirs(args.out, exist_ok=True)

    app = App(device, args.package, parse_extras(args.extra), compile_mode=args.compile)
    info = {
        "package": args.package,
        "apk": os.path.abspath(apk),
        "variant": args.variant,
        "runs": args.runs,
        "seed": seed,
        "extras": app.extras,
        "compile": args.compile,
        "scenarios": scenarios,
        "gate": args.gate,
        "gates_file": os.path.abspath(args.gates),
        "started": time.strftime("%Y-%m-%dT%H:%M:%S"),
        "device": {
            "serial": args.serial,
            "model": device.prop("ro.product.model"),
            "sdk": device.prop("ro.build.version.sdk"),
            "battery_c": device.battery_celsius(),
        },
    }
    results, skipped, notes = [], [], []
    for index in range(1, args.runs + 1):
        wait_until_cool(device)
        print(f"run {index}/{args.runs}: install, profile, warm-up", flush=True)
        run = started_run(device, app, args.out, index, seed, apk)
        for scenario in scenarios:
            print(f"run {index}/{args.runs}: {scenario}", flush=True)
            run.run(scenario)
        # On disk as soon as it is done, so a later run that fails leaves the earlier ones readable.
        write_text(os.path.join(run.out, "metrics.json"),
                   json.dumps({"metrics": run.metrics, "inapplicable": run.inapplicable, "notes": run.notes}, indent=2))
        results.append(run.metrics)
        skipped.append(run.inapplicable)
        notes += [f"run {index}: {note}" for note in run.notes]
    metrics = combine_runs(results)
    inapplicable = combine_inapplicable(skipped)

    gate_results = None
    if args.gate:
        baseline = None
        if args.baseline:
            with open(args.baseline, encoding="utf-8") as handle:
                baseline = json.load(handle)
        gate_results = check_gates.evaluate(
            {"metrics": metrics, "inapplicable": inapplicable}, rows, args.gate, baseline,
        )
    write_summary(args.out, info, metrics, notes, gate_results, inapplicable)
    with open(os.path.join(args.out, "summary.md"), encoding="utf-8") as handle:
        print(handle.read())
    if gate_results is not None and not check_gates.passed(gate_results):
        return 1
    return 0


def record(args):
    """Screen-records one journey on the installed app and writes its frames and a frame strip."""
    require_tools(["ffmpeg", "ffprobe"], "record.sh (it cuts the video into frames)")
    device = Device(args.serial)
    device.check_attached()
    with device.upright():
        return record_on(device, args)


def record_on(device, args):
    app = App(device, args.package, parse_extras(args.extra))
    os.makedirs(args.out, exist_ok=True)
    run = Run(device, app, args.out, 0, 0)
    device.wake()
    app.stop()
    app.launch()
    width, height = device.size
    prepare, actions = args.prepare or "", args.actions or ""
    if args.journey == "custom":
        if not actions:
            raise HarnessError("the custom journey needs --actions '<shell commands>'")
        time.sleep(2.0)
    else:
        _, run.tabs = run.wait_for(lambda t: t.tabs(), "the tab bar (tab_0)", timeout=30)
        row = run.at_root(0)
        swipe = gesture_swipe(width, height)
        if args.journey == "tab_switch":
            actions = "; sleep 1.2; ".join([run.tap(node) for node in run.tabs[1:]] + [run.tap(run.tabs[0])])
        elif args.journey == "push":
            actions = run.tap(row)
        elif args.journey == "pop":
            prepare, actions = run.tap(row), "input keyevent 4"
        elif args.journey == "pushpop":
            actions = f"{run.tap(row)}; sleep 1.6; input keyevent 4"
        elif args.journey == "gesture_pop":
            prepare, actions = run.tap(row), swipe
        elif args.journey == "rapid_back":
            print(f"rapid_back recorded from depth {run.push_to_depth(row)}")
            actions = "input keyevent 4 4 4"
        elif args.journey == "scroll":
            x = width // 2
            actions = f"input swipe {x} {int(height * 0.75)} {x} {int(height * 0.29)} 140"
        elif args.journey == "back_during_push":
            edge = args.edge or "right"
            y = int(height * BACK_CHECK_HEIGHT)
            actions = f"{run.tap(row)}; {back_swipe(width, height, edge=edge, share=0.35, steps=8, y=y)}"
        elif args.journey.startswith("back_"):
            edge = args.edge or "left"
            start, far = (2, int(width * 0.6)) if edge == "left" else (width - 3, int(width * 0.4))
            prepare = run.tap(row)
            actions = {
                "back_slow_commit": back_swipe(width, height, edge=edge, share=0.45, steps=24, hold=1.0),
                "back_slow_cancel": back_swipe(width, height, edge=edge, share=0.45, steps=24, hold=1.0, cancel=True),
                "back_fast_flick": f"input swipe {start} {height // 2} {far} {height // 2} 120",
                "back_y_follow": back_swipe(
                    width, height, edge=edge, share=0.40, steps=24, hold=1.0, drift=height // 4, cancel=True,
                ),
                "back_to_home": f"input keyevent 4; sleep 1.6; "
                                f"{back_swipe(width, height, edge=edge, share=0.40, steps=24, hold=1.0)}",
            }[args.journey]
    if prepare:
        device.shell(prepare)
        time.sleep(1.6)

    # Outside shared storage, so no gallery app starts processing the video while the next one records.
    remote = "/data/local/tmp/perf_record.mp4"
    device.shell(f"rm -f {remote}")
    # 720 px wide: at the panel's own size this phone's encoder records no frames at 120 Hz.
    recorder = subprocess.Popen(
        ["adb"] + (["-s", args.serial] if args.serial else [])
        + ["shell", f"screenrecord --time-limit {args.seconds} --size 720x1600 --bit-rate 16000000 {remote}"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    time.sleep(1.2)
    device.shell(actions)
    recorder.wait(timeout=args.seconds + 30)
    time.sleep(1.0)
    video = os.path.join(args.out, f"{args.journey}.mp4")
    device.adb("pull", remote, video)
    device.shell(f"rm -f {remote}")
    frames_dir = os.path.join(args.out, "frames")
    report = extract_frames(video, frames_dir)
    strip = os.path.join(args.out, f"{args.journey}_strip.png")
    make_strip(frames_dir, report, strip)
    write_text(os.path.join(args.out, "frames.json"), json.dumps(report, indent=2))
    print(f"{video}\n{strip}\n{len(report['moving'])} moving frames, "
          f"near-blank frames: {report['blank'][:20]}")
    return 0


def gesture_swipe(width, height):
    """A quick back swipe from the left edge, as one `input swipe`: the gesture of the timing scenarios."""
    return f"input swipe 3 {height // 2} {int(width * 0.7)} {height // 2 + 50} 260"


def back_swipe(width, height, edge="left", share=0.45, steps=24, hold=0.0, drift=0, cancel=False, y=None,
               while_held=None):
    """Shell commands for a back gesture from one edge at [y] (half-way down unless given), made of
    single motion events: [share] of the width in [steps] moves, the finger drifting down by [drift]
    px, the shell command [while_held] run once it has arrived, held for [hold] s, then let go where
    it is (a commit) or after returning to the edge at the height it drifted to (a cancel)."""
    y = height // 2 if y is None else y
    start = 2 if edge == "left" else width - 3
    direction = 1 if edge == "left" else -1
    far = int(width * share)
    # The system drops an edge swipe whose first moves past the touch slop go more down than across, so
    # the finger drifts only once the first quarter of the moves has made it a back gesture.
    lead = steps // 4 if drift else 0

    def at(i):
        return start + direction * far * i // steps, y + drift * max(0, i - lead) // (steps - lead)

    commands = [f"input motionevent DOWN {start} {y}"]
    commands += [f"input motionevent MOVE {x} {my}" for x, my in map(at, range(1, steps + 1))]
    if while_held:
        commands.append(while_held)
    if hold:
        commands.append(f"sleep {hold}")
    if cancel:
        commands += [f"input motionevent MOVE {at(i)[0]} {y + drift}" for i in range(steps - 1, -1, -1)]
        commands.append(f"input motionevent UP {start} {y + drift}")
    else:
        x, my = at(steps)
        commands.append(f"input motionevent UP {x} {my}")
    return "; ".join(commands)


def _ffmpeg_values(video, filters, name):
    output = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", video, "-fps_mode", "passthrough", "-vf",
         f"{filters},metadata=print:file=-", "-f", "null", "-"],
        capture_output=True, text=True,
    ).stdout
    return [float(value) for value in re.findall(rf"lavfi\.signalstats\.{name}=([\d.]+)", output)]


def extract_frames(video, frames_dir):
    """Every frame as a PNG, the frames that differ from the one before, and the flat ones."""
    os.makedirs(frames_dir, exist_ok=True)
    for name in os.listdir(frames_dir):
        os.remove(os.path.join(frames_dir, name))
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", video, "-fps_mode", "passthrough",
                    os.path.join(frames_dir, "f_%04d.png")], check=True)
    times = [
        float(value) for value in subprocess.run(
            ["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "frame=pts_time",
             "-of", "csv=p=0", video], capture_output=True, text=True, check=True,
        ).stdout.split() if value.strip()
    ]
    # Difference n is between frames n+1 and n+2 (1-based), so a moving frame is numbered n+2.
    differences = _ffmpeg_values(video, "tblend=all_mode=difference,signalstats", "YAVG")
    moving = [index + 2 for index, value in enumerate(differences) if value > 0.25]
    # The status and navigation bars are drawn by the system and never blank, so only the app's
    # middle is looked at: a frame whose darkest and brightest pixels there are a few levels apart is
    # one flat colour, the blank frame between two screens.
    middle = "crop=iw:ih*0.7:0:ih*0.12,signalstats"
    lows = _ffmpeg_values(video, middle, "YMIN")
    highs = _ffmpeg_values(video, middle, "YMAX")
    blank = [index + 1 for index, (low, high) in enumerate(zip(lows, highs)) if high - low < 8]
    return {"times": times, "difference": differences, "moving": moving, "blank": blank}


def make_strip(frames_dir, report, path, columns=8, width=180, limit=64):
    """The moving frames (two either side), in order, as one tiled image."""
    moving = report["moving"] or list(range(1, min(len(report["times"]), 16) + 1))
    first = max(1, moving[0] - 2)
    chosen = [n for n in range(first, min(moving[-1] + 3, first + limit))
              if os.path.exists(os.path.join(frames_dir, f"f_{n:04d}.png"))]
    listing = os.path.join(frames_dir, "strip.txt")
    write_text(listing, "".join(f"file '{os.path.join(frames_dir, f'f_{n:04d}.png')}'\n" for n in chosen))
    rows = (len(chosen) + columns - 1) // columns
    subprocess.run(
        ["ffmpeg", "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", listing, "-vf",
         f"scale={width}:-1,tile={columns}x{rows}:padding=4:color=black", "-frames:v", "1", path],
        check=True,
    )


def main(argv=None):
    argv = list(sys.argv[1:] if argv is None else argv)
    if argv and argv[0] == "record":
        parser = argparse.ArgumentParser(prog="record.sh", description="Screen-record one journey.")
        parser.add_argument("package")
        parser.add_argument("journey", choices=JOURNEYS)
        parser.add_argument("out")
        parser.add_argument("--extra", action="append")
        parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
        parser.add_argument("--seconds", type=int, default=8)
        parser.add_argument("--prepare", help="shell commands run before recording (custom journey)")
        parser.add_argument("--actions", help="shell commands recorded (custom journey)")
        parser.add_argument("--edge", choices=["left", "right"],
                            help="the edge a back_* journey swipes from (back_during_push: right, others: left)")
        args = parser.parse_args(argv[1:])
        handler = record
    else:
        parser = argparse.ArgumentParser(prog="perf.sh", description=__doc__.split("\n\n")[0])
        parser.add_argument("--package", required=True)
        source = parser.add_mutually_exclusive_group(required=True)
        source.add_argument("--apk")
        source.add_argument("--variant")
        parser.add_argument("--runs", type=int, default=3)
        parser.add_argument("--out", default="perf-results")
        parser.add_argument("--extra", action="append")
        parser.add_argument("--gate")
        parser.add_argument("--gates", default=check_gates.DEFAULT_GATES)
        parser.add_argument("--baseline")
        parser.add_argument("--seed", type=int)
        # speed compiles every method ahead of time: the most a baseline profile could give, so a run
        # against the default tells what is still interpreted or JIT-compiled on the measured paths.
        parser.add_argument("--compile", choices=["speed-profile", "speed"], default="speed-profile")
        parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
        parser.add_argument("scenarios", nargs="+")
        args = parser.parse_args(argv)
        handler = measure
    try:
        return handler(args)
    except HarnessError as error:
        print(f"perf: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
