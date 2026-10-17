"""`python3 -m tests.consistency` on this Windows host. Run from generator/ with the same arguments.

The runner's workers call `run_one`, a function of the package's `__main__.py`. Windows has no fork, so the pool
spawns its workers, and a spawned worker never imports a package's `__main__` (multiprocessing.spawn skips it on
purpose): every worker dies with "module '__main__' has no attribute 'run_one'" (BrokenProcessPool, run 22).
Imported as a plain module, the runner's functions pickle by their module name and the workers find them.
Retire it once package WIN fixes the runner itself.
"""
import os
import sys

sys.path.insert(0, os.getcwd())
from tests.consistency import __main__ as runner  # noqa: E402

if __name__ == "__main__":
    sys.exit(runner.main(sys.argv[1:]))
