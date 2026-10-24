import json, sys
base, out = sys.argv[1], sys.argv[2]
data = json.load(open(base + '/accepted.json', encoding='utf-8'))
port, sq, land = data['port'], data['sq'], data['land']
# Interleave as a feed mixes them: roughly 5 landscape : 3 portrait : 1 square, spread evenly.
order = []
pi = si = li = 0
total = len(port) + len(sq) + len(land)
for k in range(total):
    want = []
    # Pick the kind whose share is furthest behind.
    shares = []
    if pi < len(port): shares.append(((pi + 1) / len(port), 'p'))
    if si < len(sq): shares.append(((si + 1) / len(sq), 's'))
    if li < len(land): shares.append(((li + 1) / len(land), 'l'))
    kind = min(shares)[1]
    if kind == 'p': order.append(port[pi]); pi += 1
    elif kind == 's': order.append(sq[si]); si += 1
    else: order.append(land[li]); li += 1
lines = []
for n, pid, author, w, h in order:
    lines.append(f'    SamplePhoto({pid}, "{author}", {w}, {h}),')
src = '''package com.base.app.catalog.media.photos

import androidx.compose.runtime.Immutable

/** A photo Lorem Picsum serves by [id] at any size; [width] and [height] are the original's, for its aspect ratio. */
@Immutable
internal class SamplePhoto(val id: Int, val author: String, val width: Int, val height: Int)

/**
 * Unsplash photos, through Lorem Picsum (`picsum.photos/v2/list`), in a feed's mix of portrait, square
 * and landscape. Each was looked at before it was listed: no person in focus, no brand and no readable
 * text. The Unsplash License allows this use without attribution; the scenes credit the photographers
 * anyway.
 */
internal val SamplePhotos: List<SamplePhoto> = listOf(
''' + '\n'.join(lines) + '''
)
'''
open(out, 'w', encoding='utf-8', newline='\n').write(src)
print(len(order))
