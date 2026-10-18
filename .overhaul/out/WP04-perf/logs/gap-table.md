| Row | Before `3c9c403` | Fixed `730f8e0` | Head `fb866b3` | Tip `a256582` | `solid` limit |
|---|---|---|---|---|---|
| Tab switch, janky (%) / after the first (%) | 1.07 / 0 | 1.15 / 0 | 1.07 / 0 | 1.07 / 0 | 1 |
| Tab switch, late first frames / switches | 14 / 15 | 15 / 15 | 14 / 15 | 14 / 15 |  |
| Tab switch, hitch (ms/s) | 0 | 0 | 0 | 0 | ratchet |
| Tab switch, first frame UI median / max (ms) | 4.29 / 11.13 | 4.7 / 10.94 | 4.04 / 9.82 | 3.95 / 9.41 | 8 / 16 |
| Tab switch, p99 / GPU p90 (ms) | 21 / 10 | 20 / 7 | 19 / 8 | 20 / 7 |  |
| Plain taps, janky (%) / after the first (%) | 1.15 / 0 | 1.15 / 0 | 1.15 / 0 | 1.07 / 0 | 1 |
| First visit to a tab, first frame median / max (ms) | 4.21 / 9.74 | 4.9 / 10.39 | 4.04 / 9.26 | 3.86 / 8.69 | ratchet |
| Push, composing frame median / max (ms) | 7.29 / 15.53 | 7.64 / 16.72 | 7.34 / 15.73 | 10.04 / 15.59 | ratchet |
| Pop, first frame median / max (ms) | 4.44 / 9.31 | 2.56 / 6.09 | 2.35 / 5.37 | 2.34 / 5.16 | 8 / 16 |
| Push/pop (Back key), janky (%) / after the first (%) / p99 (ms) | 1.78 / 0.2 / 30 | 1.56 / 0 / 28 | 1.67 / 0.1 / 29 | 1.57 / 0 / 29 | 2 / – / 25 |
| Push/pop, hitch (ms/s) / GPU p90 (ms) | 12.15 / 12 | 1 / 9 | 4.01 / 10 | 3.01 / 10 |  |
| Back gesture, janky (%) / after the first (%) / p99 (ms) | 3.06 / 1.23 / 30 | 2.55 / 0.94 / 28 | 2.54 / 0.93 / 27 | 2.4 / 0.8 / 26 | 2 / – / – |
| Back gesture, hitch (ms/s) / GPU p90 (ms) | 25.1 / 12 | 20.43 / 10 | 21.72 / 10 | 16.4 / 10 |  |
| Rapid Back, janky (%) / after the first (%) / p99 (ms) | 1.85 / 0.93 / 19 | 1.22 / 0.61 / 17 | 0.92 / 0.61 / 14 | 1.07 / 0.46 / 14 | 4 |
| Rapid Back, coalesced pop slowest frame median / max (ms) | 7.55 / 10.58 | 6.02 / 6.36 | 5.67 / 5.74 | 5.58 / 5.83 | 8 / 16 |
| Scroll root, janky (%) / GPU p90 (ms) | 0.98 / 6 | 0.81 / 6 | 0.52 / 6 | 0.22 / 6 | 0.5 / 7 |
| Scroll with minimize, janky (%) / GPU p90 (ms) | 0.88 / 7 | 0.96 / 6 | 0.37 / 6 | 0.37 / 6 | 0.5 / 7 |
| Recompositions per switch / push / pop | 1 / 5 / 2 | 1 / 5 / 2 | 1 / 5 / 2 | 1 / 5 / 2 | 2 / – / 2 |
| Cold start, speed-profile / as installed (ms) | 354 / 408 | 385 / 441 | 368 / 403 | 355 / 408 | 380 / 400 |
| PSS app / total (MB) | 79 / 133.7 | 84.7 / 140.1 | 80.5 / 135.6 | 82.2 / 137.2 | 48 / – |
