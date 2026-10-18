"""Adaptive checks on an emulator: the app across window sizes, folds, postures, a second window and
the keyboard.

    adaptive.sh --package <applicationId> [--apk <path> | --variant devRelease] [--serial ID]
                [--out DIR] [--allow-display-overrides] sizes|fold|posture|multiinstance|keyboard ...

Each step changes the window one way, waits for the app to settle, and checks what the shell logs
(`perf.shell`, dev builds only): the chrome follows the window's size class, the selected tab and
every stack are unchanged, the activity was not created again, and the process is alive. Every step
saves a screenshot and the UI tree; `summary.md` lists the results. The exit code is non-zero when a
step fails.

`sizes` changes the display's size and density with `wm`, which is a system setting: it runs on an
emulator only, unless --allow-display-overrides says the device's owner agreed. Every exit restores
the size, the density and the rotation.
"""

import argparse
import os
import re
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "perf"))

import check_nav_log  # noqa: E402
from perf import App, Device, HarnessError, find_apk  # noqa: E402

SCENARIOS = ["sizes", "fold", "posture", "multiinstance", "keyboard"]

# (name, wm size, wm density): a live resize series at one density, then a density change.
SIZES = [
    ("fold", "1346x1682", 320),
    ("tablet-8", "2048x1280", 320),
    ("tablet-10", "2560x1600", 320),
    ("fold-again", "1346x1682", 320),
    ("smallest", "640x1136", 320),
    ("desktop", "1920x1080", 160),
]

# Android's breakpoints (window-core BREAKPOINTS_V2), in dp.
WIDTHS = [(1600, "ExtraLarge"), (1200, "Large"), (840, "Expanded"), (600, "Medium"), (0, "Compact")]
HEIGHTS = [(900, "Expanded"), (480, "Medium"), (0, "Compact")]

SETTLE_S = 2.5


def width_class(dp):
    return next(name for floor, name in WIDTHS if dp >= floor)


def height_class(dp):
    return next(name for floor, name in HEIGHTS if dp >= floor)


def expected_chrome(width, height, sidebar_open=None):
    """The shell's policy (ShellChromePolicy.kt), for checking it from outside."""
    if height == "Compact":
        return "Rail"
    if width == "Compact":
        return "Bar"
    if width == "Medium":
        return "Rail"
    large = width in ("Large", "ExtraLarge")
    return "Sidebar" if (sidebar_open if sidebar_open is not None else large) else "Rail"


class Checker:
    def __init__(self, device, app, out):
        self.device = device
        self.app = app
        self.out = out
        self.results = []
        self.counter = 0

    def shell_state(self):
        lines = self.device.shell("logcat -d -s perf.shell:I").splitlines()
        state = check_nav_log.last_state(lines)
        if state is None:
            raise HarnessError("no perf.shell line: is this a dev build, signed in (perf.fakeSession)?")
        return state

    def creations(self):
        """How many times the app's activities were created since the log was cleared."""
        events = self.device.shell("logcat -d -b events -s wm_on_create_called:I")
        return sum(1 for line in events.splitlines() if self.app.package in line)

    def alive(self):
        return bool(self.app.pid())

    def step(self, name, change, expect_chrome=True):
        """Applies [change], settles, and checks the shell against the state before it."""
        self.counter += 1
        before = self.shell_state()
        created = self.creations()
        change()
        time.sleep(SETTLE_S)
        problems = []
        if not self.alive():
            problems.append("the app is no longer running")
            state = before
        else:
            state = self.shell_state()
            if (state["tab"], state["stacks"], state["presented"]) != (before["tab"], before["stacks"], before["presented"]):
                problems.append(f"navigation changed: tab {before['tab']} -> {state['tab']}, "
                                f"stacks {before['stacks']} -> {state['stacks']}")
            if self.creations() != created:
                problems.append("the activity was created again")
            if expect_chrome:
                problems += self.chrome_problems(state)
            problems += check_nav_log.violations(state)
        stamp = f"{self.counter:02d}_{name}"
        self.device.screenshot(os.path.join(self.out, f"{stamp}.png"))
        try:
            with open(os.path.join(self.out, f"{stamp}.xml"), "w") as handle:
                handle.write(self.device.ui().xml)
        except HarnessError:
            pass
        self.results.append((name, state.get("window"), state.get("class"), state.get("chrome"), problems))
        verdict = "ok" if not problems else "FAIL: " + "; ".join(problems)
        print(f"{name}: window={state.get('window')} class={state.get('class')} chrome={state.get('chrome')} {verdict}")
        return state

    def chrome_problems(self, state):
        window = re.fullmatch(r"(\d+)x(\d+)dp", state.get("window") or "")
        if not window:
            return [f"the shell logged no window: {state.get('window')}"]
        width, height = width_class(int(window.group(1))), height_class(int(window.group(2)))
        problems = []
        if state.get("class") != f"{width}x{height}":
            problems.append(f"class {state.get('class')}, expected {width}x{height}")
        expected = expected_chrome(width, height)
        if state.get("chrome") != expected:
            problems.append(f"chrome {state.get('chrome')}, expected {expected}")
        return problems

    def summary(self):
        lines = ["# Adaptive checks", "", "| Step | Window | Class | Chrome | Result |", "|---|---|---|---|---|"]
        for name, window, size_class, chrome, problems in self.results:
            lines.append(f"| {name} | {window} | {size_class} | {chrome} | {'ok' if not problems else '; '.join(problems)} |")
        with open(os.path.join(self.out, "summary.md"), "w") as handle:
            handle.write("\n".join(lines) + "\n")
        return all(not problems for *_, problems in self.results)


def prepare(checker):
    """Opens the first row of the first tab, so a resize has a stack to keep."""
    device, app = checker.device, checker.app
    device.shell("logcat -c; logcat -b events -c")
    app.stop()
    app.launch()
    time.sleep(SETTLE_S)
    row = device.ui().first_row()
    if row is not None:
        x, y = row.center
        device.shell(f"input tap {x} {y}")
        time.sleep(SETTLE_S)


def run_sizes(checker):
    device = checker.device
    for name, size, density in SIZES:
        checker.step(name, lambda: device.shell(f"wm size {size}; wm density {density}"))
    checker.step("reset", lambda: device.shell("wm size reset; wm density reset"))
    checker.step("rotate", lambda: device.shell("settings put system accelerometer_rotation 0; "
                                                "settings put system user_rotation 1"))
    checker.step("rotate-back", lambda: device.shell("settings put system user_rotation 0"))


def run_fold(checker):
    device = checker.device
    checker.step("unfold", lambda: device.adb("emu", "unfold", check=False))
    checker.step("fold", lambda: device.adb("emu", "fold", check=False))
    checker.step("unfold-again", lambda: device.adb("emu", "unfold", check=False))


def run_posture(checker):
    device = checker.device
    checker.step("half-open", lambda: device.adb("emu", "posture", "2", check=False))
    checker.step("open", lambda: device.adb("emu", "posture", "3", check=False))


def run_multiinstance(checker):
    device, app = checker.device, checker.app
    # NEW_TASK | MULTIPLE_TASK | LAUNCH_ADJACENT: a second window of the app beside the first.
    checker.step("second-window", lambda: device.shell(f"am start -f 0x18001000 -n {app.activity} {app.extra_args()}"),
                 expect_chrome=False)
    tasks = sum(1 for line in device.shell("dumpsys activity activities").splitlines()
                if "Task{" in line and app.package in line)
    if tasks < 2:
        checker.results.append(("two-windows", None, None, None, [f"expected two tasks of {app.package}, found {tasks}"]))
        print(f"two-windows: FAIL: {tasks} task(s)")


def run_keyboard(checker):
    device = checker.device
    before = checker.shell_state()
    tabs = len(before["roots"])
    if tabs > 1:
        device.shell("input keycombination KEYCODE_CTRL_LEFT KEYCODE_2")
        time.sleep(SETTLE_S)
        state = checker.shell_state()
        problems = [] if state["tab"] == 1 else [f"Ctrl+2 left tab {state['tab']} selected"]
        checker.results.append(("ctrl-2", state.get("window"), state.get("class"), state.get("chrome"), problems))
        print(f"ctrl-2: {'ok' if not problems else problems}")
        device.shell("input keycombination KEYCODE_CTRL_LEFT KEYCODE_1")
        time.sleep(SETTLE_S)


def main(argv=None):
    parser = argparse.ArgumentParser(prog="adaptive.sh", description=__doc__.split("\n\n")[0])
    parser.add_argument("--package", required=True)
    source = parser.add_mutually_exclusive_group()
    source.add_argument("--apk")
    source.add_argument("--variant")
    parser.add_argument("--serial", default=os.environ.get("ANDROID_SERIAL"))
    parser.add_argument("--out", default="adaptive-results")
    parser.add_argument("--allow-display-overrides", action="store_true")
    parser.add_argument("scenarios", nargs="+", choices=SCENARIOS)
    args = parser.parse_args(argv)

    device = Device(args.serial)
    try:
        device.check_attached()
        emulator = device.prop("ro.kernel.qemu") == "1" or device.prop("ro.boot.qemu") == "1"
        if "sizes" in args.scenarios and not emulator and not args.allow_display_overrides:
            raise HarnessError("sizes changes the display's size and density: run it on an emulator, or pass "
                               "--allow-display-overrides with the device owner's agreement")
        app = App(device, args.package, {})
        apk = args.apk or (find_apk(device, args.variant) if args.variant else None)
        if apk:
            app.install(apk)
        os.makedirs(args.out, exist_ok=True)
        checker = Checker(device, app, args.out)
        device.wake()
        prepare(checker)
        runners = {"sizes": run_sizes, "fold": run_fold, "posture": run_posture,
                   "multiinstance": run_multiinstance, "keyboard": run_keyboard}
        try:
            for scenario in args.scenarios:
                runners[scenario](checker)
        finally:
            if "sizes" in args.scenarios:
                device.shell("wm size reset; wm density reset; settings put system user_rotation 0")
        passed = checker.summary()
    except HarnessError as error:
        print(f"adaptive: {error}", file=sys.stderr)
        return 2
    print(f"{os.path.join(args.out, 'summary.md')}: {'all steps passed' if passed else 'some steps failed'}")
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
