"""Tests for the parsers and the gate checker: python3 tools/perf/test_perf.py

The dumps in testdata/ are real ones, cut to what the parsers read, from a Redmi Note 10 Pro running
the template before its navigation rewrite: tab switches at 1.88 % janky, push/pop at 5.02 %, a pop
whose first frame took 27 ms of UI time, and a list scroll at 0.07 %.
"""

import contextlib
import io
import json
import os
import sys
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import check_gates  # noqa: E402
import check_nav_log  # noqa: E402
import parse_framestats  # noqa: E402
import parse_gfx  # noqa: E402
import perf  # noqa: E402
import split_profile  # noqa: E402


def fixture(name):
    with open(os.path.join(HERE, "testdata", name)) as handle:
        return handle.read()


def gate_rows():
    return check_gates.load_rows(check_gates.DEFAULT_GATES)


def clean_summary():
    """A run that meets every solid limit, built from the real clean scroll and passing values."""
    metrics = {}
    for row in gate_rows():
        limit = row.get("solid")
        if limit is None:
            continue
        if limit == "ratchet":
            value = 1.0
        elif isinstance(limit, bool):
            value = True
        else:
            value = limit / 2 if limit else 0
        metrics[row["metric"]] = {"value": value, "runs": [value]}
    scroll = parse_gfx.combine([parse_gfx.parse(fixture("gfx_scroll_clean.txt"))])
    metrics["scroll_root.janky_pct"] = {"value": scroll["janky_pct"], "runs": [scroll["janky_pct"]]}
    metrics["scroll_root.gpu_p90_ms"] = {"value": scroll["gpu_p90_ms"], "runs": [scroll["gpu_p90_ms"]]}
    return {"metrics": metrics}


def janky_summary():
    """The clean run with the tab, push/pop and pop numbers the template measured before the rewrite."""
    summary = clean_summary()
    tabs = parse_gfx.combine([parse_gfx.parse(fixture("gfx_tabs_janky.txt"))])
    pushpop = parse_gfx.combine([parse_gfx.parse(fixture("gfx_pushpop_janky.txt"))])
    pop = parse_framestats.first_frame(parse_framestats.load(fixture("framestats_pop.txt")))
    summary["metrics"].update({
        "tabs.janky_pct": {"value": tabs["janky_pct"]},
        "pushpop.janky_pct": {"value": pushpop["janky_pct"]},
        "pushpop.p99_ms": {"value": pushpop["p99_ms"]},
        "pushpop.pop_first_frame_ms.median": {"value": pop["ui_ms"]},
    })
    return summary


class ParseGfxTest(unittest.TestCase):
    def test_reads_the_app_block(self):
        dump = parse_gfx.parse(fixture("gfx_tabs_janky.txt"))
        self.assertEqual(dump["frames"], 1331)
        self.assertEqual(dump["janky"], 25)
        self.assertEqual(dump["slow_ui_thread"], 22)

    def test_percentiles_match_the_platform(self):
        for name in ("gfx_tabs_janky.txt", "gfx_scroll_clean.txt", "gfx_pushpop_janky.txt"):
            dump = parse_gfx.parse(fixture(name))
            combined = parse_gfx.combine([dump])
            for p in (50, 90, 95, 99):
                self.assertEqual(combined[f"p{p}_ms"], dump[f"p{p}_reported"], f"{name} p{p}")
                self.assertEqual(combined[f"gpu_p{p}_ms"], dump[f"gpu_p{p}_reported"], f"{name} gpu p{p}")
            self.assertAlmostEqual(combined["janky_pct"], dump["janky_pct_reported"], places=2)

    def test_dumps_add_up(self):
        tabs = parse_gfx.parse(fixture("gfx_tabs_janky.txt"))
        scroll = parse_gfx.parse(fixture("gfx_scroll_clean.txt"))
        combined = parse_gfx.combine([tabs, scroll])
        self.assertEqual(combined["frames"], 1331 + 1359)
        self.assertEqual(combined["janky"], 25 + 1)
        self.assertAlmostEqual(combined["janky_pct"], 100 * 26 / 2690, places=2)

    def test_an_empty_dump_has_no_percentiles(self):
        combined = parse_gfx.combine([parse_gfx.parse("Total frames rendered: 0\n")])
        self.assertEqual(combined["frames"], 0)
        self.assertIsNone(combined["p90_ms"])


class ParseFramestatsTest(unittest.TestCase):
    def test_first_frame_of_a_pop(self):
        frames = parse_framestats.load(fixture("framestats_pop.txt"))
        first = parse_framestats.first_frame(frames)
        self.assertAlmostEqual(first["ui_ms"], 27.31, places=2)
        self.assertAlmostEqual(first["phases"]["animation_and_composition"], 8.25, places=2)
        self.assertAlmostEqual(first["phases"]["layout_and_draw"], 19.06, places=2)
        self.assertEqual(first["dropped_vsyncs"], 2)

    def test_slowest_frame(self):
        frames = parse_framestats.load(fixture("framestats_pop.txt"))
        self.assertAlmostEqual(parse_framestats.max_ui_ms(frames), 27.31, places=2)

    def test_a_push_is_measured_by_the_frame_that_composes_it(self):
        # A light frame (the press letting go), the frame that composes the screen, then the slide.
        frames = [frame(0, 2.25), frame(1, 12.96), frame(2, 2.47), frame(3, 30.0)]
        self.assertAlmostEqual(parse_framestats.ui_ms(parse_framestats.opening_frame(frames)), 12.96, places=2)

    def test_no_frames(self):
        self.assertIsNone(parse_framestats.first_frame([]))
        self.assertIsNone(parse_framestats.opening_frame([]))
        self.assertIsNone(parse_framestats.max_ui_ms([]))
        self.assertIsNone(parse_framestats.hitch_ratio([[]]))

    def test_stages_of_the_first_frames(self):
        frames = parse_framestats.load(fixture("framestats_pop.txt"))
        stages = parse_framestats.stages(frames)
        self.assertEqual(len(stages), 3)
        self.assertAlmostEqual(stages[0]["ui"], 27.31, places=2)
        for stage in stages:
            self.assertGreaterEqual(stage["dequeue"], 0)
            self.assertTrue(stage["issue"] is None or stage["issue"] >= 0)

    def test_a_late_present_is_a_hitch(self):
        interval = 8_333_333
        presents = [(1 + i) * interval for i in range(10)] + [13 * interval, 14 * interval]
        # A second burst of motion after a second of idle adds motion time, not hitch time.
        presents += [10**9 + i * interval for i in range(4)]
        rows = [dict(frame(i, 2.0), DisplayPresentTime=t) for i, t in enumerate(presents)]
        hitch, motion = parse_framestats.hitch_time(rows)
        self.assertAlmostEqual(hitch, 2 * interval / 1_000_000, places=3)
        self.assertAlmostEqual(motion, 16 * interval / 1_000_000, places=3)
        self.assertAlmostEqual(parse_framestats.hitch_ratio([rows]), 1000 * hitch / motion, places=1)

    def test_late_first_frames_are_counted_per_transition(self):
        on_time = dict(frame(0, 4.0), FrameDeadline=13_666_666, FrameCompleted=10_000_000, GpuCompleted=12_000_000)
        gpu_late = dict(frame(0, 4.0), FrameDeadline=13_666_666, FrameCompleted=10_000_000, GpuCompleted=19_000_000)
        later = dict(frame(1, 1.0), FrameDeadline=40_000_000, FrameCompleted=20_000_000, GpuCompleted=30_000_000)
        self.assertEqual(parse_framestats.late_first_frames([[on_time, later], [gpu_late, later], []]), (1, 2))

    def test_jank_after_the_first_frame_sets_each_late_first_frame_apart(self):
        on_time = dict(frame(0, 4.0), FrameDeadline=13_666_666, FrameCompleted=10_000_000, GpuCompleted=12_000_000)
        late = dict(frame(0, 4.0), FrameDeadline=13_666_666, FrameCompleted=10_000_000, GpuCompleted=19_000_000)
        later = dict(frame(1, 1.0), FrameDeadline=40_000_000, FrameCompleted=20_000_000, GpuCompleted=30_000_000)
        # A switch whose only janky frame is its late first one, one whose first frame was on time but
        # which dropped two later, and one that was late first and dropped one more.
        dumps = [{"frames": 50, "janky": 1}, {"frames": 50, "janky": 2}, {"frames": 100, "janky": 2}]
        frames = [[late, later], [on_time, later], [late, later]]
        self.assertEqual(perf.janky_after_first_pct(dumps, frames), 1.5)
        self.assertEqual(perf.janky_after_first_pct([], []), 0.0)

    def test_frames_the_display_did_not_report_are_left_out(self):
        rows = [dict(frame(i, 2.0), DisplayPresentTime=0) for i in range(5)]
        self.assertEqual(parse_framestats.hitch_time(rows), (0.0, 0.0))


def frame(index, ui_ms, interval=8_333_333):
    start = index * interval
    return {"Flags": 0, "IntendedVsync": start, "AnimationStart": start,
            "SyncQueued": start + int(ui_ms * 1_000_000), "FrameInterval": interval}


def shell_line(tab, stacks, presented=(), roots=None):
    roots = roots if roots is not None else [stack[0] if stack else "?" for stack in stacks]
    return (f"10-09 04:00:00.000 I/perf.shell( 123): generation=1 tab={tab} "
            f"stacks={json.dumps(stacks)} presented={json.dumps(list(presented))} roots={json.dumps(roots)}")


class NavLogTest(unittest.TestCase):
    def test_consistent_states(self):
        lines = [
            shell_line(0, [["Home"], ["Feed"]]),
            shell_line(0, [["Home", "Detail(id=1)"], ["Feed"]]),
            shell_line(1, [["Home", "Detail(id=1)"], ["Feed"]], presented=["Sheet"]),
            "unrelated line",
        ]
        states, problems = check_nav_log.check(lines)
        self.assertEqual(states, 3)
        self.assertEqual(problems, [])

    def test_every_invariant(self):
        roots = ["Home", "Feed"]
        cases = {
            "selected tab 2 does not exist": shell_line(2, [["Home"], ["Feed"]], roots=roots),
            "tab 1 has an empty stack": shell_line(0, [["Home"], []], roots=roots),
            "twice in a row": shell_line(0, [["Home", "Detail(id=1)", "Detail(id=1)"], ["Feed"]], roots=roots),
            "not its root": shell_line(0, [["Detail(id=1)"], ["Feed"]], roots=roots),
        }
        for expected, line in cases.items():
            _, problems = check_nav_log.check([line])
            self.assertTrue(any(expected in problem for problem in problems), f"{expected}: {problems}")

    def test_keys_with_brackets_and_spaces(self):
        state = check_nav_log.parse_line(shell_line(0, [["Home", "Search(query=a] b=c)"]]))
        self.assertEqual(state["stacks"][0][1], "Search(query=a] b=c)")

    def test_last_state(self):
        lines = [shell_line(0, [["Home"]]), shell_line(0, [["Home", "Detail(id=2)"]])]
        self.assertEqual(check_nav_log.last_state(lines)["stacks"], [["Home", "Detail(id=2)"]])

    def test_the_window_and_its_chrome(self):
        line = shell_line(0, [["Home"]]) + " more=true chrome=Rail window=891x411dp class=ExpandedxCompact"
        state = check_nav_log.parse_line(line)
        self.assertTrue(state["more"])
        self.assertEqual((state["chrome"], state["window"], state["class"]), ("Rail", "891x411dp", "ExpandedxCompact"))


class GatesTest(unittest.TestCase):
    def test_a_clean_run_passes_solid(self):
        results = check_gates.evaluate(clean_summary(), gate_rows(), "solid")
        failing = [r["label"] for r in results if r["status"] not in ("pass", "report")]
        self.assertEqual(failing, [])

    def test_the_janky_template_fails_solid(self):
        results = check_gates.evaluate(janky_summary(), gate_rows(), "solid")
        failing = {r["metric"] for r in results if r["status"] == "fail"}
        self.assertEqual(failing, {
            "tabs.janky_pct", "pushpop.janky_pct", "pushpop.p99_ms", "pushpop.pop_first_frame_ms.median",
        })
        self.assertFalse(check_gates.passed(results))

    def test_a_skipped_scenario_fails(self):
        summary = clean_summary()
        del summary["metrics"]["idle.frames"]
        results = check_gates.evaluate(summary, gate_rows(), "solid")
        self.assertIn(("idle.frames", "not measured"), [(r["metric"], r["status"]) for r in results])
        self.assertFalse(check_gates.passed(results))

    def test_rows_of_an_inapplicable_scenario_are_not_applicable(self):
        summary = {"metrics": {
            key: value for key, value in clean_summary()["metrics"].items()
            if key.split(".")[0] in ("pushpop", "idle", "coldstart", "coldstart_installed")
        }, "inapplicable": {"tabs": "the app has one tab"}}
        results = check_gates.evaluate(summary, gate_rows(), "lean")
        tab_rows = {r["status"] for r in results if r["metric"].startswith("tabs.")}
        self.assertEqual(tab_rows, {"not applicable (the app has one tab)"})
        self.assertTrue(check_gates.passed(results))

    def test_a_scenario_only_some_runs_skipped_stays_applicable(self):
        self.assertEqual(perf.combine_inapplicable([{"tabs": "the app has one tab"}, {}]), {})
        self.assertEqual(
            perf.combine_inapplicable([{"tabs": "the app has one tab"}] * 3), {"tabs": "the app has one tab"},
        )

    def test_ratchet_rows_are_reported(self):
        summary = clean_summary()
        summary["metrics"]["pushpop.push_first_frame_ms.median"] = {"value": 99.0}
        results = check_gates.evaluate(summary, gate_rows(), "solid")
        row = next(r for r in results if r["metric"] == "pushpop.push_first_frame_ms.median")
        self.assertEqual(row["status"], "report")
        self.assertTrue(check_gates.passed(results))

    def test_lean_needs_only_its_rows(self):
        summary = {"metrics": {
            key: value for key, value in clean_summary()["metrics"].items()
            if key.split(".")[0] in ("tabs", "pushpop", "idle", "coldstart", "coldstart_installed")
        }}
        self.assertTrue(check_gates.passed(check_gates.evaluate(summary, gate_rows(), "lean")))

    def test_the_large_column_gates_only_scenarios_that_need_a_large_window(self):
        def scenarios(column):
            return {row["metric"].split(".")[0] for row in gate_rows() if column in row}

        self.assertEqual(scenarios("large"), perf.LARGE_SCENARIOS)
        self.assertFalse(scenarios("solid") & perf.LARGE_SCENARIOS)

    def test_a_failed_check_fails(self):
        summary = clean_summary()
        summary["metrics"]["rapid_back.final_screen"] = {"value": False}
        results = check_gates.evaluate(summary, gate_rows(), "solid")
        self.assertIn(("rapid_back.final_screen", "fail"), [(r["metric"], r["status"]) for r in results])

    def test_smoke_tolerance(self):
        reference = {"metrics": {"tabs.janky_pct": {"value": 1.0}, "scroll_root.gpu_p90_ms": {"value": 6.0}}}
        rows = [row for row in gate_rows() if row["metric"] in ("tabs.janky_pct", "scroll_root.gpu_p90_ms")]
        within = {"metrics": {"tabs.janky_pct": {"value": 1.5}, "scroll_root.gpu_p90_ms": {"value": 7.0}}}
        beyond = {"metrics": {"tabs.janky_pct": {"value": 1.6}, "scroll_root.gpu_p90_ms": {"value": 7.5}}}
        self.assertTrue(check_gates.passed(check_gates.evaluate(within, rows, "smoke", reference)))
        results = check_gates.evaluate(beyond, rows, "smoke", reference)
        self.assertEqual([r["status"] for r in results], ["fail", "fail"])

    def test_cli_exit_codes(self):
        with tempfile.TemporaryDirectory() as directory:
            for name, summary, expected in (("clean", clean_summary(), 0), ("janky", janky_summary(), 1)):
                path = os.path.join(directory, f"{name}.json")
                with open(path, "w") as handle:
                    json.dump(summary, handle)
                with contextlib.redirect_stdout(io.StringIO()):
                    code = check_gates.main(["--gate", "solid", path])
                self.assertEqual(code, expected, name)


class SummaryTest(unittest.TestCase):
    def test_medians_and_checks_across_runs(self):
        metrics = perf.combine_runs([
            {"tabs.janky_pct": 1.0, "rapid_back.alive": True, "idle.frames": 0},
            {"tabs.janky_pct": 3.0, "rapid_back.alive": False, "idle.frames": 2},
            {"tabs.janky_pct": 2.0, "rapid_back.alive": True},
        ])
        self.assertEqual(metrics["tabs.janky_pct"]["value"], 2.0)
        self.assertIs(metrics["rapid_back.alive"]["value"], False)
        self.assertEqual(metrics["idle.frames"]["value"], 1)
        self.assertEqual(metrics["idle.frames"]["runs"], [0, 2, None])


def atrace_line(when, mark, pid=4242, tid=4242):
    return f"  e.wp04.example-{tid}  ( {pid}) [003] ...1 {when:.6f}: tracing_mark_write: {mark}"


def frame_slices(when, recompose=False, pid=4242):
    lines = [atrace_line(when, f"B|{pid}|Choreographer#doFrame 123")]
    if recompose:
        lines += [atrace_line(when + 0.001, f"B|{pid}|Compose:recompose"), atrace_line(when + 0.002, f"E|{pid}")]
    lines.append(atrace_line(when + 0.004, f"E|{pid}"))
    return lines


class UiTreeTest(unittest.TestCase):
    XML = """<hierarchy><node resource-id="primary_list" bounds="[0,200][600,1000]" clickable="false">
      <node resource-id="" bounds="[0,150][600,260]" clickable="true"/>
      <node resource-id="" bounds="[0,260][600,400]" clickable="true"/>
      <node resource-id="" bounds="[0,400][600,420]" clickable="true"/>
      <node resource-id="" bounds="[0,420][600,560]" clickable="true"/>
      <node resource-id="" bounds="[0,900][600,1080]" clickable="true"/>
    </node></hierarchy>"""

    def test_rows_are_the_clickable_ones_wholly_inside_the_list(self):
        tree = perf.UiTree(self.XML)
        self.assertEqual([row.bounds[1] for row in tree.rows()], [260, 420])
        self.assertEqual(tree.first_row().bounds[1], 260)
        self.assertEqual(tree.rows("missing_list"), [])

    class HangingDumpDevice(perf.Device):
        """A device whose first dump never answers, as uiautomator sometimes does on the phone."""

        def __init__(self, xml, kill_hangs=False):
            super().__init__("fake")
            self.xml = xml
            self.kill_hangs = kill_hangs
            self.commands = []

        def adb(self, *args, timeout=180, check=True):
            self.commands.append(" ".join(args))
            if args[:2] == ("exec-out", "uiautomator dump /dev/tty"):
                if sum(command.startswith("exec-out uiautomator") for command in self.commands) == 1:
                    raise perf.subprocess.TimeoutExpired(["adb", *args], timeout)
                return self.xml
            if self.kill_hangs and args[:1] == ("shell",):
                raise perf.subprocess.TimeoutExpired(["adb", *args], timeout)
            return ""

    def test_a_dump_that_never_answers_is_ended_on_the_device_and_tried_again(self):
        device = self.HangingDumpDevice(self.XML)
        with mock_sleep():
            tree = device.ui()
        self.assertEqual(tree.first_row().bounds[1], 260)
        self.assertEqual(device.commands, [
            "exec-out uiautomator dump /dev/tty",
            "shell pkill -f uiautomator",
            "exec-out uiautomator dump /dev/tty",
        ])

    def test_a_dump_is_tried_again_when_ending_the_hung_one_gets_no_answer_either(self):
        device = self.HangingDumpDevice(self.XML, kill_hangs=True)
        with mock_sleep():
            tree = device.ui()
        self.assertEqual(tree.first_row().bounds[1], 260)
        self.assertEqual(device.commands[-1], "exec-out uiautomator dump /dev/tty")


class RecomposeTraceTest(unittest.TestCase):
    def test_frames_that_recompose_are_counted_per_action(self):
        lines = []
        # A tap (down and up), then three frames, the first recomposing.
        for when in (1.000, 1.080):
            lines += [atrace_line(when, "B|4242|deliverInputEvent src=0x1002"), atrace_line(when + 0.001, "E|4242")]
        lines += frame_slices(1.1, recompose=True) + frame_slices(1.108) + frame_slices(1.116)
        # A swipe: five moves, frames in between, one of them recomposing after the third move.
        for index in range(5):
            when = 3.0 + index * 0.08
            lines += [atrace_line(when, "B|4242|deliverInputEvent src=0x1002"), atrace_line(when + 0.001, "E|4242")]
            lines += frame_slices(when + 0.01, recompose=(index == 3))
        # Another thread's recomposition is not the main thread's.
        lines += [atrace_line(3.5, "B|4242|Compose:recompose", tid=4300), atrace_line(3.501, "E|4242", tid=4300)]
        windows = perf.recomposing_frames("\n".join(lines), 4242)
        self.assertEqual([window["recomposing"] for window in windows], [1, 1])
        self.assertEqual(windows[0]["frames"], 3)
        self.assertEqual(windows[1]["recomposing_while_dragged"], 1)

    def test_a_back_swipe_ends_where_it_lets_go(self):
        commit = perf.back_swipe(1080, 2400, steps=4)
        self.assertTrue(commit.startswith("input motionevent DOWN 2 1200"))
        self.assertTrue(commit.endswith("input motionevent UP 488 1200"))
        cancel = perf.back_swipe(1080, 2400, edge="right", steps=4, drift=100, cancel=True)
        self.assertTrue(cancel.startswith("input motionevent DOWN 1077 1200"))
        self.assertTrue(cancel.endswith("input motionevent UP 1077 1300"))

    def test_a_drifting_back_swipe_is_a_back_gesture_before_it_drifts(self):
        # The system cancels an edge swipe whose first moves past the touch slop go more down than across.
        commands = perf.back_swipe(1080, 2400, share=0.40, steps=24, drift=600, cancel=True).split("; ")
        moves = [tuple(map(int, command.split()[-2:])) for command in commands if " MOVE " in command]
        across_before_drift = max(x - 2 for x, y in moves if y == 1200)
        self.assertGreater(across_before_drift, 48)
        first_drift = next(i for i, (x, y) in enumerate(moves) if y != 1200)
        self.assertGreater(moves[first_drift][0] - 2, moves[first_drift][1] - 1200)
        self.assertEqual(moves[23], (2 + 432, 1800))


class UprightTest(unittest.TestCase):
    class FakeDevice(perf.Device):
        def __init__(self, settings):
            super().__init__("fake")
            self.settings = dict(settings)

        def shell(self, command, timeout=180):
            for part in command.split("; "):
                verb, *rest = part.split()[1:]
                key = " ".join(rest[:2])
                if verb == "get":
                    return self.settings.get(key, "null") + "\n"
                if verb == "put":
                    self.settings[key] = rest[2]
                elif verb == "delete":
                    self.settings.pop(key, None)
            return ""

    def test_the_owners_rotation_and_screen_timeout_come_back(self):
        device = self.FakeDevice({"system accelerometer_rotation": "1", "system user_rotation": "1"})
        with mock_sleep(), device.upright():
            self.assertEqual(device.settings["system user_rotation"], "0")
            self.assertEqual(device.settings["system accelerometer_rotation"], "0")
            self.assertEqual(device.settings["global stay_on_while_plugged_in"], "7")
        self.assertEqual(device.settings, {"system accelerometer_rotation": "1", "system user_rotation": "1"})

    def test_settings_come_back_when_the_run_fails(self):
        device = self.FakeDevice({"global stay_on_while_plugged_in": "2"})
        with self.assertRaises(perf.HarnessError), mock_sleep(), device.upright():
            raise perf.HarnessError("the phone is locked")
        self.assertEqual(device.settings, {"global stay_on_while_plugged_in": "2"})


@contextlib.contextmanager
def mock_sleep():
    sleep, perf.time.sleep = perf.time.sleep, lambda seconds: None
    try:
        yield
    finally:
        perf.time.sleep = sleep


class MeminfoTest(unittest.TestCase):
    def test_graphics_is_split_from_what_the_app_holds(self):
        memory = perf.parse_meminfo(fixture("meminfo.txt"))

        self.assertEqual(memory["pss_mb"], 94.0)
        self.assertEqual(memory["graphics_mb"], 53.1)
        self.assertEqual(memory["pss_app_mb"], 40.8)

    def test_a_dump_without_totals_is_not_read(self):
        self.assertIsNone(perf.parse_meminfo("No process found for: com.example"))


class SplitProfileTest(unittest.TestCase):
    def test_library_rules_join_their_wildcards_and_the_app_takes_the_rest_of_the_project(self):
        with tempfile.TemporaryDirectory() as project:
            sources = {
                "core/navigation/src/main/kotlin/com/acme/core/navigation/AppShell.kt": "package com.acme.core.navigation",
                "core/ui/src/main/kotlin/com/acme/core/ui/Screen.kt": "package com.acme.core.ui",
                "app/src/main/kotlin/com/acme/MainActivity.kt": "package com.acme",
                "app/src/main/kotlin/com/acme/ui/AppRoot.kt": "package com.acme.ui",
            }
            for path, text in sources.items():
                os.makedirs(os.path.join(project, os.path.dirname(path)), exist_ok=True)
                with open(os.path.join(project, path), "w") as handle:
                    handle.write(text + "\n")
            generated = os.path.join(project, "app/src/main/generated/baselineProfiles")
            os.makedirs(generated)
            with open(os.path.join(generated, "baseline-prof.txt"), "w") as handle:
                handle.write("\n".join([
                    "HSPLcom/acme/core/navigation/AppShellKt;->AppShell(Landroidx/compose/runtime/Composer;I)V",
                    "Lcom/acme/core/navigation/AppShellKt$AppShell$1;",
                    "HSPLcom/acme/core/ui/ScreenKt;->Screen()V",
                    "HSPLandroidx/compose/runtime/ComposerImpl;->startGroup()V",
                    "HSPLcom/acme/MainActivity;->onCreate(Landroid/os/Bundle;)V",
                    "HSPLcom/acme/feature/sample/DetailScreenKt;->DetailScreen()V",
                ]) + "\n")
            safety_net = ["HSPLcom/acme/core/navigation/**->**(**)**", "Lcom/acme/core/navigation/**;"]
            with open(os.path.join(project, "core/navigation/src/main/baseline-prof.txt"), "w") as handle:
                handle.write("\n".join(safety_net + ["HSPLcom/acme/core/navigation/Stale;->gone()V"]) + "\n")
            with contextlib.redirect_stdout(io.StringIO()):
                split_profile.main(["--project", project, "--module", "core/navigation", "--module", "core/ui"])

            def rules(module):
                with open(os.path.join(project, module, "src/main/baseline-prof.txt")) as handle:
                    return handle.read().splitlines()

            self.assertEqual(rules("core/navigation")[:2], safety_net)
            self.assertEqual(len(rules("core/navigation")), 4)
            self.assertEqual(rules("core/ui"), ["HSPLcom/acme/core/ui/ScreenKt;->Screen()V"])
            self.assertEqual(rules("app"), [
                "HSPLcom/acme/MainActivity;->onCreate(Landroid/os/Bundle;)V",
                "HSPLcom/acme/feature/sample/DetailScreenKt;->DetailScreen()V",
            ])
            with open(os.path.join(project, "core/navigation/src/main/baseline-prof.txt"), "rb") as handle:
                self.assertNotIn(b"\r", handle.read())


class HostTest(unittest.TestCase):
    """What a Windows host does differently: line ends on write, and tools that may be missing."""

    @contextlib.contextmanager
    def without(self, *tools):
        which = perf.shutil.which
        perf.shutil.which = lambda name, *args, **kwargs: None if name in tools else which(name, *args, **kwargs)
        try:
            yield
        finally:
            perf.shutil.which = which

    class UntouchedDevice:
        """A device the harness must not reach: any call fails the test."""

        def __getattr__(self, name):
            raise AssertionError(f"the device was used ({name}) before the tools were checked")

    def test_summaries_are_written_with_lf_line_ends(self):
        info = {
            "package": "com.acme.dev", "apk": "app.apk", "runs": 1, "seed": 1, "extras": {}, "scenarios": ["idle"],
            "gate": "solid", "gates_file": check_gates.DEFAULT_GATES,
            "device": {"model": "Phone", "serial": "1", "battery_c": 30.0},
        }
        metrics = perf.combine_runs([{"idle.frames": 0}])
        results = check_gates.evaluate({"metrics": metrics}, gate_rows(), "solid")
        with tempfile.TemporaryDirectory() as out:
            perf.write_summary(out, info, metrics, ["a note"], results)
            for name in ("summary.json", "summary.md"):
                with open(os.path.join(out, name), "rb") as handle:
                    self.assertNotIn(b"\r", handle.read(), name)

    def test_back_during_push_without_ffmpeg_stops_before_the_device_is_touched(self):
        args = perf.argparse.Namespace(gates=check_gates.DEFAULT_GATES, gate="solid", apk=None, variant="devRelease",
                                       scenarios=["tabs", "back_during_push"])
        with self.without("ffmpeg"), self.assertRaisesRegex(perf.HarnessError, "back_during_push .* ffmpeg"):
            perf.measure_on(self.UntouchedDevice(), args)

    def test_record_without_ffprobe_fails_with_a_message(self):
        stderr = io.StringIO()
        with self.without("ffprobe"), contextlib.redirect_stderr(stderr):
            code = perf.main(["record", "com.acme.dev", "push", tempfile.gettempdir(), "--serial", "absent"])
        self.assertEqual(code, 2)
        self.assertIn("needs ffprobe on PATH", stderr.getvalue())


if __name__ == "__main__":
    unittest.main()
