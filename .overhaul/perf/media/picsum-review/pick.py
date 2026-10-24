import json, glob, sys
rows = []
for f in sorted(glob.glob('C:/Users/CRAZYH~1/AppData/Local/Temp/mrev/picsum/list_*.json')):
    rows += json.load(open(f, encoding='utf-8'))
port = [r for r in rows if int(r['height']) > int(r['width'])]
sq = [r for r in rows if int(r['height']) == int(r['width'])]
land = [r for r in rows if int(r['height']) < int(r['width'])]
# Landscape spread across the id range, not only the first ones.
step = max(1, len(land) // 115)
cands = port + sq + land[::step][:115]
with open('C:/Users/CRAZYH~1/AppData/Local/Temp/mrev/picsum/candidates.tsv', 'w', encoding='utf-8') as out:
    for n, r in enumerate(cands):
        w, h = int(r['width']), int(r['height'])
        tw = 300
        th = max(1, round(tw * h / w))
        out.write(f"{n:03d}\t{r['id']}\t{r['author']}\t{w}\t{h}\thttps://picsum.photos/id/{r['id']}/{tw}/{th}.jpg\n")
print(len(port), len(sq), len(land), len(cands))
