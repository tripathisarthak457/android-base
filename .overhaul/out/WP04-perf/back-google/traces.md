
## Texture renders (drawLayer) per step

| build | fast swipe | slow start | cancel | push | edge touch | edge scroll (ms after its start) |
|---|---|---|---|---|---|---|
| bg-head | 0 0 0 0 0 0 0 0 | 0 0 0 0 | 0 0 0 0 | 0 in all | 0 0 0 0 0 0 | 0 0 0 0 |
| bg-tex | 2 2 2 2 2 2 2 2 | 2 2 2 2 | 2 2 2 2 | 0 in all | 0 0 0 0 0 0 | 0 0 0 0 |
| bg-c1 | 1 1 1 1 1 1 1 1 | 1 1 1 1 | 1 1 1 1 | 0 in all | 0 0 0 0 0 0 | 0 0 0 0 |
| bg-c2 | 1 1 1 1 1 1 1 1 | 1 1 1 1 | 1 1 1 1 | 0 in all | 1 1 1 1 1 1 | 1@53 1@79 1@78 1@81 |

## Progress to screen, start, release, cancel tail (backlat2.sql; medians over gestures)

| build | kind | n | start_to_shrink | write_to_trav | write_to_present | late_draws / writes | release_to_fade | frames_after_settle |
|---|---|---|---|---|---|---|---|---|
| bg-head | fast | 8 | 75.8 | 8.4 | 40.8 | 102 / 110 | 43 | 16 (16 16 16 16 16 16 16 16) |
| bg-head | slow | 4 | 66.3 | 8.1 | 36.7 | 31 / 35 | 47 | 16 (16 16 16 16) |
| bg-head | cancel | 4 | 105.8 | 8.1 | 36.7 | 28 / 30 | – | 13.5 (13 14 13 14) |
| bg-tex | fast | 8 | 76.8 | 8.5 | 37 | 101 / 109 | 42.2 | 16 (16 16 16 16 16 16 16 16) |
| bg-tex | slow | 4 | 88.1 | 8.3 | 36.6 | 30 / 34 | 48.6 | 16 (16 16 16 16) |
| bg-tex | cancel | 4 | 112.5 | 8 | 36.6 | 28 / 30 | – | 14 (14 13 14 14) |
| bg-c1 | fast | 8 | 66.7 | 0.6 | 36.7 | 0 / 116 | 40.8 | 15.5 (16 16 15 16 15 15 16 15) |
| bg-c1 | slow | 4 | 103.4 | 1 | 28.4 | 0 / 33 | 39 | 15.5 (15 15 16 16) |
| bg-c1 | cancel | 4 | 104.2 | 1 | 28.4 | 0 / 30 | – | 0 (0 0 0 0) |
| bg-c2 | fast | 8 | 63.8 | 0.6 | 36.7 | 0 / 113 | 39 | 16 (15 16 15 16 16 16 16 16) |
| bg-c2 | slow | 4 | 77 | 0.8 | 28.4 | 0 / 34 | 35.4 | 15 (15 16 15 15) |
| bg-c2 | cancel | 4 | 101.5 | 0.6 | 28.4 | 0 / 30 | – | 0 (0 0 0 0) |

## The gesture's first frames (startframes.sql; medians)

| build | kind | n | frame 1: after start | RT | GPU | layer renders | frame 2 GPU | renders in the 400 ms before |
|---|---|---|---|---|---|---|---|---|
| bg-head | fast | 8 | 7.2 | 3 | 10.4 (10.66 9.57 10.71 11.42 8.08 9.28 10.07 11.16) | 0 | 5.6 | 0 |
| bg-head | slow | 4 | 8.2 | 5 | 9.4 (8.9 10.03 9.87 8.19) | 0 | 9.5 | 0 |
| bg-head | cancel | 4 | 7.2 | 2.8 | 12.4 (10.36 19.55 12.84 11.92) | 0 | 4.6 | 0 |
| bg-tex | fast | 8 | 9.4 | 3.4 | 14.1 (12.23 16.57 14.2 14.47 13.98 13.03 15.13 13.28) | 8 | 6 | 0 |
| bg-tex | slow | 4 | 8.4 | 5.5 | 12.5 (12.34 12.65 12.04 13.12) | 4 | 9.8 | 0 |
| bg-tex | cancel | 4 | 6.5 | 3.9 | 12.9 (9.38 12.42 13.29 16.4) | 4 | 4.2 | 0 |
| bg-c1 | fast | 8 | 6.7 | 3.4 | 14.7 (13.34 15.18 14.5 15.62 12.92 12.12 16.06 14.84) | 8 | 4.4 | 0 |
| bg-c1 | slow | 4 | 7.5 | 5.8 | 13.2 (12.78 14.04 13.58 11.6) | 4 | 8.9 | 0 |
| bg-c1 | cancel | 4 | 8.9 | 9.8 | 10.5 (9.42 8.81 11.53 13.85) | 4 | 3.7 | 0 |
| bg-c2 | fast | 8 | 6.9 | 6.8 | 4.4 (5.73 3.74 5.36 4.21 6.42 4.51 3.7 4.38) | 0 | 3.9 | 8 |
| bg-c2 | slow | 4 | 9.2 | 2.2 | 3.5 (3.45 3.22 3.53 8.69) | 0 | 4.6 | 4 |
| bg-c2 | cancel | 4 | 9.1 | 4.8 | 2.1 (0.74 3.21 1.23 2.88) | 0 | 3.3 | 4 |

## Vsync to present per gesture frame (presentlat.sql; medians over gestures)

| build | kind | n | avg frames 1-10 | avg frames 11-30 | skipped vsyncs 1-30 |
|---|---|---|---|---|---|
| bg-head | fast | 8 | 37.7 (42.7 36.8 43.5 36.9 37.7 35.3 38.5 37.7) | 32.5 | 2 |
| bg-head | slow | 4 | 36.4 (36.8 44.3 36 36) | 35.6 | 7.5 |
| bg-head | cancel | 4 | 34.4 (32.7 35.2 35.2 33.5) | 29.3 | 8 |
| bg-tex | fast | 8 | 37.7 (37.7 41.9 36.9 46 37.7 37.7 37.7 37.7) | 31 | 2 |
| bg-tex | slow | 4 | 41 (36 41 41 41) | 36.4 | 7 |
| bg-tex | cancel | 4 | 34.8 (34.3 34.4 35.2 35.2) | 29.6 | 8 |
| bg-c1 | fast | 8 | 37.7 (37.7 41.8 37.7 43.5 37.7 37.7 46 37.7) | 37.6 | 0 |
| bg-c1 | slow | 4 | 39.8 (36 40.2 39.3 41) | 35.8 | 7.5 |
| bg-c1 | cancel | 4 | 34.3 (34.3 34.4 34.3 34.3) | 34.1 | 7.5 |
| bg-c2 | fast | 7 | 37.7 (36.9 36 37.7 37.7 37.7 42.8 37.7) | 37.6 | 0 |
| bg-c2 | slow | 4 | 30.2 (30.2 29.4 30.2 35.3) | 29.4 | 8 |
| bg-c2 | cancel | 4 | 30.6 (34.4 31 30.2 30.2) | 29.3 | 7.5 |

## The release's first frames (releaseframes.sql; frame 1 and 2 of each release)

| build | kind | n | frame 1: after release | RT | actual | layer renders | frame 2 RT | frame 2 actual |
|---|---|---|---|---|---|---|---|---|
| bg-head | commit | 12 | 5.8 | 3.4 | 9.5 | 0 | 4.2 | 13.2 |
| bg-head | cancel | 4 | 8 | 4.9 | 9.8 | 0 | 2.7 | 7 |
| bg-tex | commit | 12 | 5.6 | 3.6 | 12.4 | 12 | 3.1 | 11.4 |
| bg-tex | cancel | 4 | 7.8 | 6 | 14.4 | 4 | 5.2 | 10.2 |
| bg-c1 | commit | 12 | 3.9 | 4.5 | 13 | 0 | 2.9 | 12.3 |
| bg-c1 | cancel | 4 | 7.8 | 3.1 | 12.9 | 0 | – | – |
| bg-c2 | commit | 12 | 3.9 | 2.9 | 12 | 0 | 3.2 | 12.2 |
| bg-c2 | cancel | 4 | 7.8 | 3.1 | 12.3 | 0 | – | – |
