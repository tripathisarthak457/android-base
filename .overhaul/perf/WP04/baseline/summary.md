# perf.sh: com.example.wp04.base.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/base/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 29.9 °C at the start
- Runs: 3, seed 632725, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, idle, meminfo, coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 336 | 346, 336, 332 |
| coldstart.runs_ms | [301, 337, 355, 307, 327, 354] | [341, 362, 331, 351, 326, 372], [313, 332, 382, 372, 341, 324], [301, 337, 355, 307, 327, 354] |
| Idle 5 s on every tab, frames rendered | 0 | 608, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0, 0, 0] | [0, 605, 0, 608, 607, 0, 0], [0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0] |
| PSS after the journey (MB) | 109.8 | 129.7, 108.6, 109.8 |
| pushpop.frames | 868 | 962, 861, 868 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop.gpu_p90_ms | 10 | 10, 10, 11 |
| pushpop.gpu_p99_ms | 15 | 15, 15, 15 |
| Push/pop with the Back key, janky frames (%) | 4.03 | 3.95, 4.88, 4.03 |
| pushpop.p50_ms | 8 | 8, 8, 8 |
| pushpop.p90_ms | 17 | 17, 16, 18 |
| Push/pop with the Back key, p99 frame (ms) | 34 | 36, 34, 34 |
| Pop, first frame max (ms) | 27.64 | 30.09, 26.24, 27.64 |
| Pop, first frame median (ms) | 19.09 | 19.68, 12.56, 19.09 |
| Push, the frame that composes the screen, max (ms) | 9.98 | 9.98, 19.77, 9.78 |
| Push, the frame that composes the screen, median (ms) | 8.35 | 7.8, 9.31, 8.35 |
| pushpop_gesture.frames | 702 | 706, 698, 702 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 7 | 7, 7, 7 |
| pushpop_gesture.gpu_p99_ms | 14 | 14, 14, 14 |
| Push/pop with the back gesture, janky frames (%) | 3.54 | 3.54, 3.44, 3.56 |
| pushpop_gesture.p50_ms | 8 | 8, 8, 8 |
| pushpop_gesture.p90_ms | 13 | 13, 12, 13 |
| pushpop_gesture.p99_ms | 36 | 30, 48, 36 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 2 | 2, 2, 2 |
| Rapid Back: lands on the expected screen | no | no, no, no |
| rapid_back.frames | 282 | 273, 282, 287 |
| rapid_back.gpu_p50_ms | 6 | 5, 6, 6 |
| rapid_back.gpu_p90_ms | 11 | 11, 10, 11 |
| rapid_back.gpu_p99_ms | 24 | 21, 24, 4950 |
| Rapid Back, janky frames (%) | 5.32 | 7.33, 5.32, 4.53 |
| rapid_back.p50_ms | 10 | 10, 10, 9 |
| rapid_back.p90_ms | 23 | 25, 19, 23 |
| rapid_back.p99_ms | 73 | 73, 53, 77 |
| scroll_root.frames | 1354 | 1354, 1356, 1351 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 7, 8 |
| Scroll a root list, janky frames (%) | 0.22 | 0.22, 0.07, 0.22 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 12, 17 |
| tab1.first_frame_ms.max | 53.56 | 53.56, 50.53, 53.62 |
| tab1.first_frame_ms.median | 21.63 | 22.12, 21.63, 21.23 |
| First visit to a tab, first frame max (ms) | 25.51 | 35.25, 25.51, 25.44 |
| First visit to a tab, first frame median (ms) | 19.41 | 28.32, 15.25, 19.41 |
| Tab switch to a visited tab, first frame max (ms) | 45.28 | 50.24, 42.03, 45.28 |
| Tab switch to a visited tab, first frame median (ms) | 18.97 | 20.78, 18.97, 17.66 |
| tabs.frames | 1333 | 1333, 1333, 1332 |
| tabs.gpu_p50_ms | 6 | 7, 6, 6 |
| tabs.gpu_p90_ms | 11 | 11, 10, 11 |
| tabs.gpu_p99_ms | 15 | 15, 14, 15 |
| Tab switch, janky frames (%) | 1.58 | 1.58, 1.58, 1.58 |
| tabs.p50_ms | 9 | 9, 9, 9 |
| tabs.p90_ms | 17 | 17, 17, 17 |
| tabs.p99_ms | 36 | 40, 36, 36 |

Notes:

- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
