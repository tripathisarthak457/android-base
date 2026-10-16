import sys, json, shutil, collections, re
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/scratch-gen")
import matrix_defs
from matrix_defs import runs, BASE
from gen import make
from hilt import analyse
agg = collections.defaultdict(list)
for name, r in runs.items():
    spec, dest, res, el = make("h-" + name, dict(BASE, **r))
    miss, _ = analyse(dest)
    for m in set(miss): agg[m].append(name)
    shutil.rmtree(dest)
for k, v in sorted(agg.items(), key=lambda x: -len(x[1])):
    print(len(v), k, v[:4])
print("done", len(runs))
