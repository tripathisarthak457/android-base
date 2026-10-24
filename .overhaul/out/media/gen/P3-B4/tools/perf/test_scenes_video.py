"""Tests for the short-video scene's pack: python3 tools/perf/test_scenes_video.py

The log in testdata/media/video/ follows a cold open and three swipes; the resource manager dump is
synthetic, in AOSP's layout, until a capture from the reference phone replaces it.
"""

import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import check_gates  # noqa: E402
import perf  # noqa: E402
import scenes_video  # noqa: E402
from test_scenes_images import FakePhone, FakeTimeTest, catalog_run, fixture  # noqa: E402

LINE = "I/perf.media( 5555): "


def lines(*texts):
    return [LINE + text for text in texts]


class LogTest(unittest.TestCase):
    def setUp(self):
        self.parsed = scenes_video.events(fixture("video/media_log_reels.txt").splitlines())

    def test_each_settle_is_paired_with_its_clips_next_first_frame(self):
        delays = scenes_video.settle_to_frame(self.parsed)

        # Page 0 drew 400 ms after it settled; pages 1 to 3 had drawn while they were neighbours, so 0.
        self.assertEqual([400, 0, 0, 0], delays)
        self.assertEqual(1, scenes_video.posters_after_settle(delays))

    def test_the_open_and_the_frame_counts(self):
        self.assertEqual(612, scenes_video.open_first_frame_ms(self.parsed))
        self.assertEqual((447, 3), scenes_video.frame_counts(self.parsed))

    def test_a_page_still_waiting_for_its_first_frame_is_pending(self):
        # The latest window holds page 4, handed over and not drawn yet.
        self.assertFalse(scenes_video.nothing_pending(self.parsed))
        drawn = self.parsed + scenes_video.events(lines("frame page=4 clip=srnka t=2007900"))
        self.assertTrue(scenes_video.nothing_pending(drawn))
        loading = drawn + scenes_video.events(lines("images busy t=2008000"))
        self.assertFalse(scenes_video.nothing_pending(loading))


class PlayingWindowTest(unittest.TestCase):
    def test_a_loop_between_the_marks_leaves_the_window_valid(self):
        log = scenes_video.events(lines("frame page=2 clip=tilia t=1", "window start", "drops clip=x dropped=0 rendered=1",
                                        "window end"))

        self.assertTrue(scenes_video.window_valid(log))

    def test_a_neighbours_first_frame_inside_the_window_invalidates_it(self):
        for busy in ("frame page=3 clip=o t=2", "acquire page=3 clip=o t=2", "window pages=3,4,2", "images busy t=2"):
            log = scenes_video.events(lines("window start", busy, "window end"))
            self.assertFalse(scenes_video.window_valid(log), busy)

    def test_without_both_marks_there_is_no_window(self):
        self.assertFalse(scenes_video.window_valid(scenes_video.events(lines("window start"))))


class PlayingWindowRunTest(FakeTimeTest):
    def test_a_second_invalid_window_is_not_measured(self):
        phone = FakePhone(log=lines("settle page=2 t=1"))
        run = catalog_run(phone, self.out)

        def busy_segment(name, command, settle):
            # A neighbour's first frame lands while the window is open, every time.
            phone.log.append(LINE + "frame page=3 clip=o t=9")
            return {"frames": 4}, []

        run.segment = busy_segment

        self.assertIsNone(scenes_video.playing_window(run, "media_reels_warm"))
        self.assertTrue(any("not measured" in note for note in run.notes))

    def test_a_quiet_window_counts_the_apps_frames(self):
        phone = FakePhone(log=lines("settle page=2 t=1", "window pages=2", "acquire page=2 clip=t t=2",
                                    "frame page=2 clip=t t=3", "images idle t=3"))
        run = catalog_run(phone, self.out)
        run.segment = lambda name, command, settle: ({"frames": 0}, [])

        self.assertEqual(0, scenes_video.playing_window(run, "media_reels_warm"))


class DeviceDumpTest(unittest.TestCase):
    def test_codecs_are_counted_per_client_of_the_catalogs_process(self):
        self.assertEqual(3, scenes_video.codec_count(fixture("video/resource_manager.txt"), 5555))
        self.assertEqual(1, scenes_video.codec_count(fixture("video/resource_manager.txt"), 7777))
        self.assertIsNone(scenes_video.codec_count("Can't find service: media.resource_manager", 5555))

    def test_player_and_codec_threads(self):
        threads = fixture("video/ps_threads.txt")

        self.assertEqual(2, scenes_video.thread_count(threads, ("ExoPlayer:",)))
        self.assertEqual(2, scenes_video.codec_threads(threads))


class MemoryTest(FakeTimeTest):
    def test_every_reading_of_the_reels_and_the_background_follows_a_collection(self):
        phone = FakePhone(log=lines("frame page=0 clip=flower-moon t=1"))
        run = catalog_run(phone, self.out)
        # The scene's log survives the harness clearing it, so every wait for a first frame ends at once.
        keep = list(phone.log)
        phone.adb = lambda *args, **kwargs: "\n".join(keep + phone.log) if args[:1] == ("logcat",) and "-c" not in args else ""

        scenes_video.reels(run, "media_reels")
        scenes_video.background(run, "media_background")

        meminfo = [i for i, command in enumerate(phone.commands) if command.startswith("dumpsys meminfo")]
        self.assertEqual(9, len(meminfo))
        previous = -1
        for index in meminfo:
            self.assertTrue(any("PERF_GC" in command for command in phone.commands[previous + 1:index]),
                            f"no collection before the reading at command {index}")
            previous = index
        for key in ("graphics_growth_mb", "app_growth_mb", "gc_spread_mb", "codecs_max"):
            self.assertIn(f"media_reels.{key}", run.metrics)
        self.assertEqual(0, run.metrics["media_background.player_threads"])


class GateTest(unittest.TestCase):
    def test_the_video_gate_fails_a_run_without_its_scenes(self):
        results = check_gates.evaluate({"metrics": {}}, check_gates.load_rows(check_gates.DEFAULT_GATES), "video")

        self.assertTrue(results)
        self.assertFalse(check_gates.passed(results))
        self.assertIn("media_reels.codecs_max", [result["metric"] for result in results])

    def test_the_pack_measures_the_catalog(self):
        self.assertTrue(scenes_video.CATALOG)
        self.assertEqual(["media_reels", "media_reels_warm", "media_background"], list(scenes_video.SCENES))
        self.assertTrue(set(scenes_video.SCENES) <= perf.CATALOG_SCENARIOS)


if __name__ == "__main__":
    unittest.main()
