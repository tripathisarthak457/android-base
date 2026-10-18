#!/usr/bin/env python3
"""
Generate a project from a JSON spec on stdin, writing a zip to the path given on the command line.

    python3 generate_headless.py --target android /tmp/out.zip < spec.json

The spec comes on stdin so the API never writes the user's answers, keystore passwords included,
to a path something else could read. stdout is exactly one JSON object; the exit code is 0 (ok),
2 (the spec was refused) or 1 (the generator failed). The shapes are in genkit/core/headless.py.
"""

from __future__ import annotations

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from genkit.core import headless  # noqa: E402

if __name__ == "__main__":
    sys.exit(headless.run(sys.argv[1:], sys.stdin.buffer, sys.stdout))
