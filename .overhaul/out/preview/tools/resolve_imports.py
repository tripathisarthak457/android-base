"""Resolve an import-only conflict hunk: keep the union of both sides, minus names given with --drop, sorted."""
import re, sys
path = sys.argv[1]
drop = set(sys.argv[2:])
s = open(path, encoding='utf-8', newline='').read()
def merge(m):
    ours = [l for l in m.group(1).splitlines() if l.strip()]
    theirs = [l for l in m.group(2).splitlines() if l.strip()]
    assert all(l.startswith('import ') for l in ours + theirs), (ours, theirs)
    lines = sorted({l for l in ours + theirs if l.split()[-1].split('.')[-1] not in drop and l.split()[-1] not in drop})
    return '\n'.join(lines) + '\n'
s2, n = re.subn(r'<<<<<<< HEAD\n((?:import [^\n]*\n)*)=======\n((?:import [^\n]*\n)*)>>>>>>> wp/WP03\n', merge, s)
print(path, 'hunks', n)
open(path, 'w', encoding='utf-8', newline='').write(s2)
