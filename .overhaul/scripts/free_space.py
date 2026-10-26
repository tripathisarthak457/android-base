"""Free space on D: by deleting regenerable output of the overhaul's own runs, and nothing else.

Deletes, permanently (the Recycle Bin on D: lives on D:, so recycling frees nothing):
  * generated test projects under .overhaul/out/, except out/share (the APKs sent to the user), out/NOUI/base (the
    byte-identity baseline the gate compares against) and out/NOUI-FINAL (the running final batch);
  * build output inside the worktrees under .overhaul/wt/: folders named build, .gradle, .kotlin, node_modules and
    .next, never source files or git data, and nothing in wt/_workspace-branch (the workspace backup).
Keeps perf/ (measurement evidence), every branch, every source file and the tool caches under D:/.caches.

Usage: free_space.py            list what would go and how big it is
       free_space.py --delete   delete it
"""
import os
import shutil
import stat
import subprocess
import sys
import time

OV = 'D:/projects/tools/.overhaul'
KEEP_OUT = {'share', 'NOUI-FINAL', 'WP12-KNOW'}
KEEP_OUT_NOUI = {'base'}
REGENERABLE = {'build', '.gradle', '.kotlin', 'node_modules', '.next'}
SKIP_WT = {'_workspace-branch'}


def size(path):
    total = 0
    for root, _, files in os.walk(path):
        for name in files:
            try:
                total += os.lstat(os.path.join(root, name)).st_size
            except OSError:
                pass
    return total


def targets():
    out = f'{OV}/out'
    for name in sorted(os.listdir(out)):
        path = f'{out}/{name}'
        if not os.path.isdir(path) or name in KEEP_OUT:
            continue
        if name == 'NOUI':
            for sub in sorted(os.listdir(path)):
                if sub not in KEEP_OUT_NOUI and os.path.isdir(f'{path}/{sub}'):
                    yield f'{path}/{sub}'
            continue
        yield path
    wt = f'{OV}/wt'
    for name in sorted(os.listdir(wt)):
        if name in SKIP_WT or not os.path.isdir(f'{wt}/{name}'):
            continue
        tree = f'{wt}/{name}'
        for root, dirs, _ in os.walk(tree):
            if '.git' in dirs:
                dirs.remove('.git')
            for d in [d for d in dirs if d in REGENERABLE]:
                dirs.remove(d)
                path = os.path.join(root, d).replace('\\', '/')
                # Some folders named build are source (the vendored skills' docs): never touch what git tracks.
                tracked = subprocess.run(['git', '-C', tree, 'ls-files', '--', os.path.relpath(path, tree)],
                                         capture_output=True, text=True).stdout.strip()
                if not tracked:
                    yield path


def force_writable(func, path, _):
    os.chmod(path, stat.S_IWRITE)
    func(path)


def main():
    if '--delete' not in sys.argv:
        total = 0
        for path in targets():
            mb = size(path) / 2**20
            total += mb
            if mb >= 100:
                print(f'{mb:8.0f} MB  {path}', flush=True)
        print(f'would delete: {total / 1024:.1f} GB')
        return
    # Measuring millions of build files first doubles the time, so the drive's own counter gives the total.
    before = shutil.disk_usage('D:/').free
    failed = []
    for path in targets():
        print(f'deleting {path}', flush=True)
        # Kotlin and Gradle output nests past 260 characters, which plain Win32 paths cannot reach.
        long_path = '\\\\?\\' + os.path.abspath(path)
        for attempt in range(3):
            try:
                shutil.rmtree(long_path, onexc=force_writable)
                break
            except OSError as error:
                if attempt == 2:
                    failed.append(f'{path}: {error}')
                time.sleep(2)
    for line in failed:
        print(f'FAILED {line}', flush=True)
    print(f'freed {(shutil.disk_usage("D:/").free - before) / 2**30:.1f} GB; D: free {shutil.disk_usage("D:/").free / 2**30:.1f} GB')


if __name__ == '__main__':
    main()
