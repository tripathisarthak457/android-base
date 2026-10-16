"""Rebuilds the old nested translation files from the split layout and compares them with copies of the originals."""
import json, sys
from pathlib import Path
old_dir, new_root = Path(sys.argv[1]), Path(sys.argv[2])
for old in sorted(old_dir.glob('*.json')):
    data = json.loads(old.read_text(encoding='utf-8'))
    d = new_root / old.stem
    rebuilt = {"language": json.loads((d/'_language.json').read_text())["name"],
               "scaffold": json.loads((d/'_scaffold.json').read_text()),
               "strings": {p.stem.replace('-', '/') + '/src/main/res': json.loads(p.read_text(encoding='utf-8')) for p in sorted(d.glob('*.json')) if not p.name.startswith('_')}}
    assert rebuilt == data, old
    for res, strings in data['strings'].items():
        assert list(rebuilt['strings'][res]) == list(strings), (old, res)
    print(old.stem, 'lossless:', sum(len(s) for s in data['strings'].values()), 'strings in', len(data['strings']), 'modules +', len(data['scaffold']), 'scaffold strings')
print('SPLIT LOSSLESS')
