import sys, json
base = sys.argv[1]
rejected = {1, 11, 12, 20, 21, 24, 28, 30, 38, 47, 64, 70, 73, 75, 77, 79, 80, 85, 91, 93, 94, 95, 102, 106, 111, 118,
            123, 139, 141, 143, 146, 147, 155, 156, 157, 159, 160, 161, 165, 167, 174, 175, 177, 183, 184, 186, 188}
rows = []
for line in open(base + '/candidates.tsv', encoding='utf-8'):
    n, pid, author, w, h, url = line.rstrip('\n').split('\t')
    if int(n) in rejected:
        continue
    rows.append((int(n), int(pid), author, int(w), int(h)))
port = [r for r in rows if r[4] > r[3]]
sq = [r for r in rows if r[4] == r[3]]
land = [r for r in rows if r[4] < r[3]]
print(len(rows), len(port), len(sq), len(land))
json.dump({'port': port, 'sq': sq, 'land': land}, open(base + '/accepted.json', 'w', encoding='utf-8'), ensure_ascii=False)
