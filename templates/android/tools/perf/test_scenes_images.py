"""Tests for the photo scenes' pack: python3 tools/perf/test_scenes_images.py

The trace and log in testdata/media/images/ are cut to the lines the parsers read. FakePhone stands in for
adb: it answers the commands a catalog run sends, keeps the scenes' log, and records every command, so a
test can check the order a scenario talks to the phone in.
"""

import os
import sys
import tempfile
import unittest
from unittest import mock

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import perf  # noqa: E402
import scenes_images  # noqa: E402

PACKAGE = "com.base.app.catalog"


def fixture(name):
    with open(os.path.join(HERE, "testdata", "media", name), encoding="utf-8") as handle:
        return handle.read()


class FakeClock:
    """time.time and time.sleep for a run that must not wait for real seconds."""

    def __init__(self):
        self.now = 1000.0

    def time(self):
        return self.now

    def sleep(self, seconds):
        self.now += seconds


class FakePhone:
    """The phone as a catalog run sees it through adb: a log, memory, and nothing that draws."""

    def __init__(self, answers_gc=True, log=()):
        self.answers_gc = answers_gc
        self.log = list(log)
        self.commands = []
        self.size = (1080, 2400)
        self.serial = None

    def shell(self, command, timeout=180):
        self.commands.append(command)
        if command.startswith("am broadcast") and "PERF_GC" in command and self.answers_gc:
            self.log.append(f"I/perf.media( 4321): gc done t={len(self.commands)}")
        elif command.startswith("log -p i -t perf.media"):
            self.log.append("I/perf.media( 4321): " + command.split("perf.media", 1)[1].strip().strip("'"))
        elif command.startswith("dumpsys meminfo"):
            return fixture_meminfo()
        elif command.startswith("pidof"):
            return "4321\n"
        elif command.startswith("ping"):
            return "3 packets transmitted, 3 received\nrtt min/avg/max/mdev = 20.1/24.6/30.2/4.1 ms\n"
        elif command.startswith("cat /proc/uptime"):
            return "2000.000 8000.00\n"
        elif command.startswith("cmd package resolve-activity"):
            return f"{PACKAGE}/.CatalogActivity"
        elif command.startswith("dumpsys activity"):
            return f"topResumedActivity={PACKAGE}/.CatalogActivity"
        return ""

    def adb(self, *args, timeout=180, check=True):
        if args[:2] == ("logcat", "-c"):
            self.log.clear()
        elif args and args[0] == "logcat":
            return "\n".join(self.log)
        return ""

    def screenshot(self, path):
        raise perf.HarnessError("no screen in tests")

    def ui(self):
        raise perf.HarnessError("no screen in tests")


def fixture_meminfo():
    with open(os.path.join(HERE, "testdata", "meminfo.txt"), encoding="utf-8") as handle:
        return handle.read()


def catalog_run(phone, out):
    """A CatalogRun on [phone] whose scenes open at once and whose segments draw nothing."""
    app = perf.App(phone, PACKAGE, {})
    run = perf.CatalogRun(phone, app, out, 1, 0)
    run.launch_section = mock.Mock()
    run.segment = mock.Mock(return_value=({"frames": 0}, []))
    run.record_gfx = mock.Mock()
    return run


class FakeTimeTest(unittest.TestCase):
    """Runs each test with a fake clock, so waits and timeouts take no real time."""

    def setUp(self):
        self.clock = FakeClock()
        for target in ("time.sleep", "time.time"):
            patcher = mock.patch(target, getattr(self.clock, target.split(".")[1]))
            patcher.start()
            self.addCleanup(patcher.stop)
        self.out = tempfile.mkdtemp()


class TraceTest(unittest.TestCase):
    def test_decodes_and_photo_uploads_on_frame_threads_count_and_icons_are_apart(self):
        sizes = scenes_images.decoded_sizes(fixture("images/media_log_feed.txt").splitlines())
        self.assertEqual({(1080, 1350), (1080, 608)}, sizes)

        decodes, others = scenes_images.frame_thread_work(fixture("images/atrace_feed.txt"), 4321, sizes)

        # The main thread's decode and RenderThread's photo-sized upload; the icon upload counts apart, and a
        # photo-sized upload on a decode thread or in another process not at all.
        self.assertEqual((2, 1), (decodes, others))

    def test_a_control_without_photos_matches_its_uploads_against_another_runs_sizes(self):
        trace = fixture("images/atrace_feed.txt").replace("B|4321|decode bitmap", "B|4321|Choreographer#doFrame 2")

        self.assertEqual((0, 2), scenes_images.frame_thread_work(trace, 4321, set()))
        self.assertEqual((1, 1), scenes_images.frame_thread_work(trace, 4321, {(1080, 1350)}))


class HttpTest(FakeTimeTest):
    def test_a_429_stops_the_session_and_other_errors_are_noted(self):
        run = catalog_run(FakePhone(), self.out)
        run.check_http("media_feed", fixture("images/media_log_feed.txt").splitlines())
        self.assertEqual(["media_feed: HTTP 404 from picsum.photos"], run.notes)

        with self.assertRaisesRegex(perf.HarnessError, "429"):
            run.check_http("media_feed", ["I/perf.media( 4321): http status=429 host=upload.wikimedia.org"])

    def test_an_unreachable_host_fails_the_scenario(self):
        phone = FakePhone()
        phone.shell = mock.Mock(side_effect=lambda command, timeout=180: "" if command.startswith("ping") else
                                FakePhone.shell(phone, command, timeout))
        run = catalog_run(phone, self.out)

        with self.assertRaisesRegex(perf.HarnessError, "picsum.photos does not answer"):
            run.check_hosts("media_feed", scenes_images.PHOTO_HOSTS)


class MemoryTest(FakeTimeTest):
    def test_three_readings_give_medians_and_their_spread(self):
        readings = [
            {"pss_mb": 120.0, "graphics_mb": 60.0, "pss_app_mb": 60.0},
            {"pss_mb": 118.0, "graphics_mb": 59.0, "pss_app_mb": 59.0},
            {"pss_mb": 125.5, "graphics_mb": 64.5, "pss_app_mb": 61.0},
        ]

        reading = perf.gc_median(readings)

        self.assertEqual({"pss_mb": 120.0, "graphics_mb": 60.0, "pss_app_mb": 60.0, "gc_spread_mb": 7.5}, reading)

    def test_every_memory_reading_follows_a_collection(self):
        phone = FakePhone()
        run = catalog_run(phone, self.out)

        scenes_images.feed_memory(run, "media_feed_memory")

        meminfo = [i for i, command in enumerate(phone.commands) if command.startswith("dumpsys meminfo")]
        self.assertEqual(6, len(meminfo))
        previous = -1
        for index in meminfo:
            self.assertTrue(any("PERF_GC" in command for command in phone.commands[previous + 1:index]),
                            f"no collection before the reading at command {index}")
            previous = index
        for key in ("pss_mb", "graphics_growth_mb", "app_growth_mb", "gc_spread_mb"):
            self.assertIn(f"media_feed_memory.{key}", run.metrics)

    def test_a_catalog_that_never_collects_fails_the_scenario(self):
        run = catalog_run(FakePhone(answers_gc=False), self.out)

        with self.assertRaisesRegex(perf.HarnessError, "GC hook did not answer"):
            scenes_images.feed_memory(run, "media_feed_memory")


class DiscoveryTest(unittest.TestCase):
    def test_the_pack_measures_the_catalog_in_its_declared_order(self):
        self.assertTrue(scenes_images.CATALOG)
        self.assertEqual(
            ["media_feed", "media_feed_warm", "media_grid", "media_grid_warm", "media_feed_memory", "media_trace"],
            list(scenes_images.SCENES),
        )
        self.assertTrue(set(scenes_images.SCENES) <= perf.CATALOG_SCENARIOS)
        self.assertEqual(list(scenes_images.SCENES), [name for name in perf.SCENARIOS if name in scenes_images.SCENES])

    def test_the_images_gate_fails_a_run_without_its_scenes(self):
        import check_gates

        results = check_gates.evaluate({"metrics": {}}, check_gates.load_rows(check_gates.DEFAULT_GATES), "images")

        self.assertTrue(results)
        self.assertFalse(check_gates.passed(results))
        self.assertTrue(all(result["status"] in ("not measured", "report") for result in results))


if __name__ == "__main__":
    unittest.main()
