"""Directory links for the tests: a symbolic link where the account may make one, a junction on Windows."""

from __future__ import annotations

import subprocess
import unittest
from pathlib import Path

#: What os.symlink raises on Windows for an account without the privilege to create links.
ERROR_PRIVILEGE_NOT_HELD = 1314


def symlink_or_skip(test: unittest.TestCase, link: Path, directory: Path) -> None:
    """Makes `link` a symbolic link to `directory`, or skips `test` where Windows will not let this account."""
    try:
        link.symlink_to(directory, target_is_directory=True)
    except OSError as error:
        if getattr(error, "winerror", None) != ERROR_PRIVILEGE_NOT_HELD:
            raise
        test.skipTest(
            "this Windows account may not create symbolic links (that takes Developer Mode or "
            "SeCreateSymbolicLinkPrivilege); the junction test covers links here"
        )


def junction(link: Path, directory: Path) -> None:
    """Makes `link` a junction to `directory`: the directory link any Windows account can make."""
    subprocess.run(["cmd", "/c", "mklink", "/J", str(link), str(directory)], capture_output=True, check=True)
