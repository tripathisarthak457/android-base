"""
The generator writes the same bytes on every host. A newline translated on Windows is invisible on
the Linux CI, so the rule that prevents it is checked in the source, where every host can see it.
"""

from __future__ import annotations

import ast
import tempfile
import unittest
from pathlib import Path

from genkit.core.copy import write_text

GENERATOR = Path(__file__).resolve().parent.parent
#: Everything that writes a project, an emitted copy or a template file.
SOURCES = sorted([
    *GENERATOR.glob("*.py"),
    *(GENERATOR / "genkit").rglob("*.py"),
    *(GENERATOR.parent / "tools").glob("*.py"),
])


def text_writes(source: str) -> list[int]:
    """Lines that write text with the host's line endings: `.write_text(...)`, or `open` for text without `newline`."""
    lines = []
    for node in ast.walk(ast.parse(source)):
        if not isinstance(node, ast.Call):
            continue
        function = node.func
        name = function.attr if isinstance(function, ast.Attribute) else getattr(function, "id", None)
        if name == "write_text" and isinstance(function, ast.Attribute):
            lines.append(node.lineno)
        elif name == "open" and _writes_text(node) and not any(keyword.arg == "newline" for keyword in node.keywords):
            lines.append(node.lineno)
    return lines


def _writes_text(call: ast.Call) -> bool:
    # `open(file, mode)` takes the mode second, `Path.open(mode)` first.
    position = 1 if isinstance(call.func, ast.Name) else 0
    modes = [keyword.value for keyword in call.keywords if keyword.arg == "mode"]
    if len(call.args) > position:
        modes.append(call.args[position])
    return any(
        isinstance(mode, ast.Constant) and isinstance(mode.value, str)
        and "b" not in mode.value and any(flag in mode.value for flag in "wax+")
        for mode in modes
    )


class HostTest(unittest.TestCase):

    def test_text_is_written_only_through_write_text(self):
        found = {
            f"{path.relative_to(GENERATOR.parent).as_posix()}:{line}"
            for path in SOURCES
            for line in text_writes(path.read_text(encoding="utf-8"))
        }
        self.assertEqual(set(), found, "write through genkit.core.copy.write_text")

    def test_the_check_sees_each_form_of_text_write(self):
        source = "\n".join([
            "path.write_text(text)",
            "open(name, 'w')",
            "path.open(mode='a', encoding='utf-8')",
            "open(name, 'wb')",
            "open(name, 'w', newline='')",
            "path.open()",
            "Image.open(source)",
        ])
        self.assertEqual([1, 2, 3], sorted(text_writes(source)))

    def test_write_text_keeps_the_line_endings_it_is_given(self):
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "mixed.txt"
            write_text(path, "lf\ncrlf\r\nend é\n")
            self.assertEqual("lf\ncrlf\r\nend é\n".encode("utf-8"), path.read_bytes())


if __name__ == "__main__":
    unittest.main()
