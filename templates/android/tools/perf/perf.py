"""Device measurements for the app: the work behind perf.sh and record.sh.

    perf.sh --package <applicationId> (--apk <path> | --variant devBenchmarkRelease|devRelease)
            [--runs 3] [--out DIR] [--extra key=value]... [--gate <column> [--gates FILE]
            [--baseline summary.json]] [--seed N] [--serial ID] <scenario>...

One invocation measures only an app it installed itself. Each run installs the APK, clears the app,
installs its baseline profile and compiles it the way Play does, launches it with the dev launch
options (`perf.fakeSession=true` is always passed, so onboarding and sign-in are skipped), visits every
tab once (recorded as `tab_first_visit`), then runs the scenarios. The median of the runs is what the
summary reports and what `--gate` checks.

UI is found by resource id (`Modifier.testTag` with `testTagsAsResourceId` at the root): `tab_<i>`,
`primary_list`, `related_row`. Smoothness is only meaningful on release builds compiled with their
profile, on a real phone; debug builds are an order of magnitude jankier.
"""

import argparse
import json
import os
import random
import re
import shlex
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
    "tabs", "tab1", "pushpop", "pushpop_gesture", "rapid_back", "scroll_root", "idle",
    "nav_stress", "restore", "meminfo", "scroll_minimize", "coldstart", "coldstart_installed",
]
JOURNEYS = ["tab_switch", "push", "pop", "pushpop", "gesture_pop", "rapid_back", "scroll", "custom"]

PROFILE_RECEIVER = "androidx.profileinstaller.ProfileInstallReceiver"
PROFILE_ACTION = "androidx.profileinstaller.action.INSTALL_PROFILE"
MAX_BATTERY_C = 37.0


class HarnessError(Exception):
    """The run could not measure what it was asked to; the message says why."""


# ── Device ───────────────────────────────────────────────────────────────────


class Device:
    def __init__(self, serial):
        self.serial = serial
        self._size = None

    def adb(self, *args, timeout=180, check=True):
        command = ["adb"] + (["-s", self.serial] if self.serial else []) + list(args)
        result = subprocess.run(command, capture_output=True, text=True, timeout=timeout, errors="replace")
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
        self.shell("input keyevent KEYCODE_WAKEUP")

    def ui(self, attempts=4):
        """The current UI hierarchy. Retried: uiautomator refuses while the screen keeps animating."""
        last = ""
        for _ in range(attempts):
            output = self.adb("exec-out", "uiautomator dump /dev/tty", check=False, timeout=30)
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

    def first_row(self, list_id="primary_list"):
        """The first clickable row wholly inside the list, or None."""
        holder = None
        for element in self.root.iter("node"):
            if element.get("resource-id") == list_id:
                holder = element
                break
        if holder is None:
            return None
        bounds = Node(holder).bounds
        for element in holder.iter("node"):
            if element is holder:
                continue
            node = Node(element)
            inside = node.bounds[1] >= bounds[1] and node.bounds[3] <= bounds[3]
            if node.clickable and inside and node.height > 40:
                return node
        return None


# ── App ──────────────────────────────────────────────────────────────────────


class App:
    def __init__(self, device, package, extras):
        self.device = device
        self.package = package
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
        self.device.shell(f"cmd package compile -f -m speed-profile {self.package}", timeout=600)

    def compile_with_runtime_profile(self):
        self.stop()
        self.device.shell(f"cmd package compile -f -m speed-profile {self.package}", timeout=600)

    def reset_compilation(self):
        self.stop()
        self.device.shell(f"cmd package compile --reset {self.package}", timeout=600)


# ── One run ──────────────────────────────────────────────────────────────────


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
        self.tabs = []
        self.counter = 0

    # Helpers

    def save(self, name, text):
        with open(os.path.join(self.out, name), "w") as handle:
            handle.write(text)

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

    def ui(self):
        return self.device.ui()

    def fail(self, message):
        stamp = f"run{self.index}_{self.counter:03d}"
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

    def record_gfx(self, scenario, dumps):
        combined = parse_gfx.combine(dumps)
        for name in ("janky_pct", "p50_ms", "p90_ms", "p99_ms", "gpu_p50_ms", "gpu_p90_ms", "gpu_p99_ms", "frames"):
            self.record(f"{scenario}.{name}", combined[name])

    # Start

    def start(self, apk):
        self.app.install(apk)
        self.app.clear()
        self.app.compile_with_apk_profile()
        self.device.wake()
        self.device.shell("input keyevent KEYCODE_HOME")
        self.app.launch()
        tree, tabs = self.wait_for(lambda t: t.tabs(), "the tab bar (tab_0)", timeout=30)
        self.tabs = tabs
        self.wait_for(lambda t: t.first_row(), "a row of primary_list on the first tab")
        first_visits = []
        for index in range(1, len(self.tabs)):
            _, frames = self.segment("tab_first_visit", self.tap(self.tabs[index]), 1.5)
            first = parse_framestats.first_frame(frames)
            first_visits.append(first["ui_ms"] if first else None)
        self.record_spread("tab_first_visit.first_frame_ms", first_visits)
        self.at_root(0)

    # Scenarios

    def tabs_scenario(self):
        if len(self.tabs) < 2:
            self.notes.append("tabs skipped: the app has one tab")
            return
        self.at_root(0)
        order = list(range(1, len(self.tabs))) + [0]
        dumps, firsts = [], []
        for _ in range(3):
            for index in order:
                gfx, frames = self.segment("tabs", self.tap(self.tabs[index]), 0.9)
                dumps.append(gfx)
                first = parse_framestats.first_frame(frames)
                firsts.append(first["ui_ms"] if first else None)
        self.record_gfx("tabs", dumps)
        self.record_spread("tabs.first_frame_ms", firsts)

    def tab1(self):
        if len(self.tabs) < 2:
            self.notes.append("tab1 skipped: one tab")
            return
        self.at_root(0)
        firsts = []
        for _ in range(6):
            for index in (1, 0):
                _, frames = self.segment("tab1", self.tap(self.tabs[index]), 1.6)
                first = parse_framestats.first_frame(frames)
                firsts.append(first["ui_ms"] if first else None)
        self.record_spread("tab1.first_frame_ms", firsts)

    def pushpop(self):
        row = self.at_root(0)
        dumps, pushes, pops = [], [], []
        for _ in range(8):
            gfx, frames = self.segment("push", self.tap(row), 1.3)
            dumps.append(gfx)
            pushes.append((parse_framestats.first_frame(frames) or {}).get("ui_ms"))
            gfx, frames = self.segment("pop", "input keyevent 4", 1.3)
            dumps.append(gfx)
            pops.append((parse_framestats.first_frame(frames) or {}).get("ui_ms"))
        self.record_gfx("pushpop", dumps)
        self.record_spread("pushpop.push_first_frame_ms", pushes)
        self.record_spread("pushpop.pop_first_frame_ms", pops)

    def pushpop_gesture(self):
        row = self.at_root(0)
        width, height = self.device.size
        swipe = f"input swipe 3 {height // 2} {int(width * 0.7)} {height // 2 + 50} 260"
        dumps = []
        for _ in range(6):
            gfx, _ = self.segment("gesture_push", self.tap(row), 1.3)
            dumps.append(gfx)
            gfx, _ = self.segment("gesture_pop", swipe, 1.3)
            dumps.append(gfx)
        self.record_gfx("pushpop_gesture", dumps)

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
        dumps, slowest, landed, alive, depths = [], [], [], [], []
        for _ in range(6):
            depth = self.push_to_depth(row)
            depths.append(depth)
            gfx, frames = self.segment("rapid_back", "input keyevent 4 4 4", 1.8)
            dumps.append(gfx)
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
        self.record_gfx("rapid_back", dumps)
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

    def meminfo(self):
        output = self.device.shell(f"dumpsys meminfo {self.app.package}")
        self.save("meminfo.txt", output)
        match = re.search(r"TOTAL PSS:\s+(\d+)", output) or re.search(r"^\s+TOTAL\s+(\d+)", output, re.MULTILINE)
        if not match:
            self.fail("dumpsys meminfo has no TOTAL PSS line")
        self.record("meminfo.pss_mb", round(int(match.group(1)) / 1024, 1))

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
            "tab1": self.tab1,
            "pushpop": self.pushpop,
            "pushpop_gesture": self.pushpop_gesture,
            "rapid_back": self.rapid_back,
            "scroll_root": lambda: self.scroll("scroll_root"),
            "scroll_minimize": self.scroll_minimize,
            "idle": self.idle,
            "nav_stress": self.nav_stress,
            "restore": self.restore,
            "meminfo": self.meminfo,
            "coldstart": self.coldstart,
            "coldstart_installed": self.coldstart_installed,
        }[scenario]
        handler()


# ── Summary ──────────────────────────────────────────────────────────────────


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


def write_summary(out, info, metrics, notes, gate_results):
    summary = dict(info)
    summary["metrics"] = metrics
    summary["notes"] = notes
    with open(os.path.join(out, "summary.json"), "w") as handle:
        json.dump(summary, handle, indent=2)

    labels = {row["metric"]: row["label"] for row in check_gates.load_rows(info["gates_file"])}
    lines = [
        f"# perf.sh: {info['package']}",
        "",
        f"- APK: `{info['apk']}`",
        f"- Device: {info['device']['model']} ({info['device']['serial']}), "
        f"battery {info['device']['battery_c']} °C at the start",
        f"- Runs: {info['runs']}, seed {info['seed']}, extras {json.dumps(info['extras'])}",
        f"- Scenarios: {', '.join(info['scenarios'])}",
        "",
        "| Metric | Median | Runs |",
        "|---|---|---|",
    ]
    for key, entry in metrics.items():
        runs = ", ".join(check_gates._show(value) for value in entry["runs"])
        lines.append(f"| {labels.get(key, key)} | {check_gates._show(entry['value'])} | {runs} |")
    if notes:
        lines += ["", "Notes:", ""] + [f"- {note}" for note in notes]
    if gate_results is not None:
        lines += ["", check_gates.table(gate_results, info["gate"])]
    with open(os.path.join(out, "summary.md"), "w") as handle:
        handle.write("\n".join(lines) + "\n")
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


def measure(args):
    device = Device(args.serial)
    device.check_attached()
    rows = check_gates.load_rows(args.gates)
    if args.gate and args.gate not in check_gates.columns(rows):
        raise HarnessError(f"no column '{args.gate}' in {args.gates}")
    unknown = [name for name in args.scenarios if name not in SCENARIOS]
    if unknown:
        raise HarnessError(f"unknown scenarios {unknown}; known: {', '.join(SCENARIOS)}")
    apk = args.apk or find_apk(device, args.variant)
    if not os.path.isfile(apk):
        raise HarnessError(f"{apk} does not exist")
    scenarios = [name for name in SCENARIOS if name in args.scenarios]
    seed = args.seed if args.seed is not None else random.randrange(1_000_000)
    os.makedirs(args.out, exist_ok=True)

    app = App(device, args.package, parse_extras(args.extra))
    info = {
        "package": args.package,
        "apk": os.path.abspath(apk),
        "variant": args.variant,
        "runs": args.runs,
        "seed": seed,
        "extras": app.extras,
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
    results, notes = [], []
    for index in range(1, args.runs + 1):
        wait_until_cool(device)
        run = Run(device, app, args.out, index, seed)
        print(f"run {index}/{args.runs}: install, profile, warm-up", flush=True)
        run.start(apk)
        for scenario in scenarios:
            print(f"run {index}/{args.runs}: {scenario}", flush=True)
            run.run(scenario)
        results.append(run.metrics)
        notes += [f"run {index}: {note}" for note in run.notes]
    metrics = combine_runs(results)

    gate_results = None
    if args.gate:
        baseline = None
        if args.baseline:
            with open(args.baseline) as handle:
                baseline = json.load(handle)
        gate_results = check_gates.evaluate({"metrics": metrics}, rows, args.gate, baseline)
    write_summary(args.out, info, metrics, notes, gate_results)
    with open(os.path.join(args.out, "summary.md")) as handle:
        print(handle.read())
    if gate_results is not None and not check_gates.passed(gate_results):
        return 1
    return 0


# ── Recording ────────────────────────────────────────────────────────────────


def record(args):
    """Screen-records one journey on the installed app and writes its frames and a frame strip."""
    device = Device(args.serial)
    device.check_attached()
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
        swipe = f"input swipe 3 {height // 2} {int(width * 0.7)} {height // 2 + 50} 260"
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
    if prepare:
        device.shell(prepare)
        time.sleep(1.6)

    remote = "/sdcard/perf_record.mp4"
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
    with open(os.path.join(args.out, "frames.json"), "w") as handle:
        json.dump(report, handle, indent=2)
    print(f"{video}\n{strip}\n{len(report['moving'])} moving frames, "
          f"near-blank frames: {report['blank'][:20]}")
    return 0


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
    with open(listing, "w") as handle:
        handle.writelines(f"file '{os.path.join(frames_dir, f'f_{n:04d}.png')}'\n" for n in chosen)
    rows = (len(chosen) + columns - 1) // columns
    subprocess.run(
        ["ffmpeg", "-v", "error", "-y", "-f", "concat", "-safe", "0", "-i", listing, "-vf",
         f"scale={width}:-1,tile={columns}x{rows}:padding=4:color=black", "-frames:v", "1", path],
        check=True,
    )


# ── CLI ──────────────────────────────────────────────────────────────────────


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
