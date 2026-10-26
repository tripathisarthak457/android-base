"""Turn a workflow run's journal into {"done": {label: result}} for a relaunch with args.done.

Usage: journal_done.py <run id, e.g. wf_xxx> [out.json]. Only steps with a result are kept; a failed or unfinished step
runs again on the relaunch.
"""
import json
import sys

BASE = 'C:/Users/crazyhusir/.claude/projects/D--projects-tools/8d80ef9d-4533-47d0-b348-d4c6caaeb696/subagents/workflows'
run = sys.argv[1]
labels, done = {}, {}
for line in open(f'{BASE}/{run}/journal.jsonl', encoding='utf-8'):
    entry = json.loads(line)
    if entry.get('type') == 'started':
        labels[entry['key']] = entry['label']
    elif entry.get('type') == 'result' and entry.get('key') in labels and entry.get('result') is not None:
        done[labels[entry['key']]] = entry['result']
out = json.dumps({'done': done})
if len(sys.argv) > 2:
    open(sys.argv[2], 'w', encoding='utf-8').write(out)
print(f'{len(done)} finished steps: {", ".join(done)}', file=sys.stderr)
print(out if len(sys.argv) <= 2 else sys.argv[2])
