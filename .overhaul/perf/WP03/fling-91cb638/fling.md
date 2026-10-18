# Fling against iOS's curve (catalog Deceleration lab, release build)

Package `com.example.wp03.look.catalog`, device `9a7c74f4`. Distance and duration are measured by the app;
the iOS columns are UIScrollView.DecelerationRate.normal (0.998 per ms) for the same release speed.

| Swipe | Release (dp/s) | Distance (dp) | iOS (dp) | Error | Duration (s) | iOS (s) | Error |
|---|---|---|---|---|---|---|---|
| 700 ms | 395 | 192 | 197 | -2.5% | 1.81 | 1.79 | +1.1% |
| 400 ms | 695 | 342 | 347 | -1.4% | 2.08 | 2.07 | +0.5% |
| 250 ms | 1099 | 543 | 549 | -1.1% | 2.32 | 2.30 | +0.9% |
| 160 ms | 1713 | 850 | 855 | -0.6% | 2.54 | 2.53 | +0.4% |
| 110 ms | 2511 | 1249 | 1254 | -0.4% | 2.72 | 2.72 | +0.0% |
| 80 ms | 3401 | 1693 | 1699 | -0.4% | 2.88 | 2.87 | +0.4% |

Frames over the flings (`dumpsys gfxinfo`, reset before the first):

- Total frames rendered: 1689
- Janky frames: 4 (0.24%)
- Janky frames (legacy): 3 (0.18%)
- 50th percentile: 5ms
- 90th percentile: 6ms
- 95th percentile: 6ms
- 99th percentile: 8ms
- 50th gpu percentile: 3ms
- 90th gpu percentile: 4ms
- 95th gpu percentile: 4ms
- 99th gpu percentile: 5ms

Edge: `edge/edge.mp4`, `edge/edge_crop_strip.png` (a short fling down, then a throw back into the top).
