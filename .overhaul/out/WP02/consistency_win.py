#!/usr/bin/env python3
"""Runs `python -m tests.consistency` on Windows, where the module form cannot start its workers.

The runner hands `run_one` to a `ProcessPoolExecutor`. Windows starts workers with `spawn`, which pickles a
function by its module name, and `multiprocessing` deliberately never re-imports a package's `__main__` in a
worker, so every worker dies on "module '__main__' has no attribute 'run_one'". Imported under its own name
here, the same function pickles as `tests.consistency.__main__.run_one`, which a worker can import.
Run from `generator/`:

    python consistency_win.py --quick
"""

import os
import sys

if __name__ == "__main__":
    sys.path.insert(0, os.getcwd())
    from tests.consistency.__main__ import main

    sys.exit(main(sys.argv[1:]))
