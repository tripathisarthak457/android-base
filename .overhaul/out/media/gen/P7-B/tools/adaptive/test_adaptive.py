"""Tests for adaptive.py's copy of the shell's window rules: python3 tools/adaptive/test_adaptive.py"""

import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import adaptive  # noqa: E402


class PolicyTest(unittest.TestCase):
    def test_classes_follow_the_breakpoints(self):
        widths = {411: "Compact", 599: "Compact", 600: "Medium", 840: "Expanded", 1200: "Large", 1600: "ExtraLarge"}
        for dp, expected in widths.items():
            self.assertEqual(adaptive.width_class(dp), expected, dp)
        heights = {411: "Compact", 480: "Medium", 899: "Medium", 900: "Expanded"}
        for dp, expected in heights.items():
            self.assertEqual(adaptive.height_class(dp), expected, dp)

    def test_the_chrome_follows_the_shell_policy(self):
        self.assertEqual(adaptive.expected_chrome("Compact", "Expanded"), "Bar")
        self.assertEqual(adaptive.expected_chrome("ExtraLarge", "Compact"), "Rail")
        self.assertEqual(adaptive.expected_chrome("Medium", "Medium"), "Rail")
        self.assertEqual(adaptive.expected_chrome("Expanded", "Medium"), "Rail")
        self.assertEqual(adaptive.expected_chrome("Expanded", "Medium", sidebar_open=True), "Sidebar")
        self.assertEqual(adaptive.expected_chrome("Large", "Medium"), "Sidebar")
        self.assertEqual(adaptive.expected_chrome("Large", "Medium", sidebar_open=False), "Rail")


if __name__ == "__main__":
    unittest.main()
