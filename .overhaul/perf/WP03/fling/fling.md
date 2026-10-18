# Fling against iOS's curve (catalog Deceleration lab, release build)

Package `com.example.wp03.look.catalog`, device `9a7c74f4`. Distance and duration are measured by the app;
the iOS columns are UIScrollView.DecelerationRate.normal (0.998 per ms) for the same release speed.

| Swipe | Release (dp/s) | Distance (dp) | iOS (dp) | Error | Duration (s) | iOS (s) | Error |
|---|---|---|---|---|---|---|---|
| 700 ms | 398 | 194 | 199 | -2.5% | 1.81 | 1.80 | +0.6% |
| 400 ms | 687 | 337 | 343 | -1.8% | 2.08 | 2.07 | +0.5% |
| 250 ms | 1100 | 544 | 549 | -0.9% | 2.32 | 2.30 | +0.9% |
| 160 ms | 1720 | 854 | 859 | -0.6% | 2.54 | 2.53 | +0.4% |
| 110 ms | 2535 | 1261 | 1266 | -0.4% | 2.73 | 2.72 | +0.4% |
| 80 ms | 3448 | 1717 | 1722 | -0.3% | 2.88 | 2.88 | +0.0% |

Frames over the flings (`dumpsys gfxinfo`, reset before the first):

- Total frames rendered: 2180
- Janky frames: 3 (0.14%)
- Janky frames (legacy): 0 (0.00%)
- 50th percentile: 5ms
- 90th percentile: 6ms
- 95th percentile: 6ms
- 99th percentile: 8ms
- 50th gpu percentile: 3ms
- 90th gpu percentile: 4ms
- 95th gpu percentile: 4ms
- 99th gpu percentile: 5ms

Edge: `edge/edge.mp4`, `edge/edge_crop_strip.png` (a short fling down, then a throw back into the top).
