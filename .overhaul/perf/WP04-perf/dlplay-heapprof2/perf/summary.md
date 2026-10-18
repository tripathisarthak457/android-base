# perf.sh: com.example.wp04.dlplay.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\dlplay\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 31.1 °C at the start
- Runs: 1, seed 510721, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, meminfo, scroll_minimize

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 6 | 6 |
| First back swipe after a restart (warm), janky frames | 6 | 6 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.12 | 6.12 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.8 | 6.8 |
| meminfo.graphics_mb | 55.1 | 55.1 |
| PSS after the journey, less the window's graphics buffers (MB) | 66.4 | 66.4 |
| PSS after the journey (MB) | 121.5 | 121.5 |
| pushpop.first3_dequeue_ms.max | 0.1 | 0.1 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02 |
| pushpop.first3_gpu_ms.max | 18.55 | 18.55 |
| pushpop.first3_gpu_ms.median | 11.59 | 11.59 |
| pushpop.first3_issue_ms.max | 7.78 | 7.78 |
| pushpop.first3_issue_ms.median | 2.24 | 2.24 |
| pushpop.frames | 790 | 790 |
| pushpop.gpu_p50_ms | 7 | 7 |
| pushpop.gpu_p90_ms | 11 | 11 |
| pushpop.gpu_p99_ms | 16 | 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 230.63 | 230.63 |
| pushpop.janky_after_first_pct | 8.48 | 8.48 |
| Push/pop with the Back key, janky frames (%) | 10.51 | 10.51 |
| pushpop.late_first_frames | 16 | 16 |
| pushpop.p50_ms | 16 | 16 |
| pushpop.p90_ms | 48 | 48 |
| Push/pop with the Back key, p99 frame (ms) | 81 | 81 |
| Pop, first frame max (ms) | 5.45 | 5.45 |
| Pop, first frame median (ms) | 5.17 | 5.17 |
| Push, the frame that composes the screen, max (ms) | 16.26 | 16.26 |
| Push, the frame that composes the screen, median (ms) | 12.61 | 12.61 |
| pushpop.transitions | 16 | 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.03 | 0.03 |
| pushpop_gesture.first3_gpu_ms.max | 18.01 | 18.01 |
| pushpop_gesture.first3_gpu_ms.median | 11.07 | 11.07 |
| pushpop_gesture.first3_issue_ms.max | 7.19 | 7.19 |
| pushpop_gesture.first3_issue_ms.median | 2.06 | 2.06 |
| pushpop_gesture.frames | 575 | 575 |
| pushpop_gesture.gesture_first_frame_ms.max | 2.65 | 2.65 |
| Back gesture, slowest of its first three frames, median (ms) | 2.21 | 2.21 |
| pushpop_gesture.gpu_p50_ms | 7 | 7 |
| pushpop_gesture.gpu_p90_ms | 10 | 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 276.29 | 276.29 |
| pushpop_gesture.janky_after_first_pct | 9.04 | 9.04 |
| Push/pop with the back gesture, janky frames (%) | 11.13 | 11.13 |
| pushpop_gesture.late_first_frames | 12 | 12 |
| pushpop_gesture.p50_ms | 15 | 15 |
| pushpop_gesture.p90_ms | 48 | 48 |
| pushpop_gesture.p99_ms | 81 | 81 |
| pushpop_gesture.transitions | 12 | 12 |
| Rapid Back: the app is still running | yes | yes |
| rapid_back.depth | 4 | 4 |
| Rapid Back: lands on the expected screen | yes | yes |
| rapid_back.first3_dequeue_ms.max | 0.06 | 0.06 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02 |
| rapid_back.first3_gpu_ms.max | 10.96 | 10.96 |
| rapid_back.first3_gpu_ms.median | 5.88 | 5.88 |
| rapid_back.first3_issue_ms.max | 7.72 | 7.72 |
| rapid_back.first3_issue_ms.median | 2.75 | 2.75 |
| rapid_back.frames | 510 | 510 |
| rapid_back.gpu_p50_ms | 7 | 7 |
| rapid_back.gpu_p90_ms | 10 | 10 |
| rapid_back.gpu_p99_ms | 11 | 11 |
| Rapid Back, hitch time per second of motion (ms/s) | 244.81 | 244.81 |
| rapid_back.janky_after_first_pct | 7.25 | 7.25 |
| Rapid Back, janky frames (%) | 8.04 | 8.04 |
| rapid_back.late_first_frames | 4 | 4 |
| rapid_back.p50_ms | 14 | 14 |
| rapid_back.p90_ms | 40 | 40 |
| rapid_back.p99_ms | 77 | 77 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 62.42 | 62.42 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 10.83 | 10.83 |
| rapid_back.transitions | 6 | 6 |
| scroll_minimize.frames | 1271 | 1271 |
| scroll_minimize.gpu_p50_ms | 6 | 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 8 | 8 |
| scroll_minimize.gpu_p99_ms | 10 | 10 |
| Scroll with the bar minimizing, janky frames (%) | 1.57 | 1.57 |
| scroll_minimize.p50_ms | 9 | 9 |
| scroll_minimize.p90_ms | 16 | 16 |
| scroll_minimize.p99_ms | 18 | 18 |
| scroll_root.frames | 1279 | 1279 |
| scroll_root.gpu_p50_ms | 6 | 6 |
| Scroll a root list, GPU p90 (ms) | 7 | 7 |
| scroll_root.gpu_p99_ms | 10 | 10 |
| Scroll a root list, janky frames (%) | 1.64 | 1.64 |
| scroll_root.p50_ms | 9 | 9 |
| scroll_root.p90_ms | 12 | 12 |
| scroll_root.p99_ms | 17 | 17 |
| tab1.first_frame_ms.max | 9.45 | 9.45 |
| tab1.first_frame_ms.median | 3.79 | 3.79 |
| First visit to a tab, first frame max (ms) | 4.11 | 4.11 |
| First visit to a tab, first frame median (ms) | 3.95 | 3.95 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.81 | 8.81 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.08 | 4.08 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02 |
| tabs.first3_gpu_ms.max | 20.14 | 20.14 |
| tabs.first3_gpu_ms.median | 9.43 | 9.43 |
| tabs.first3_issue_ms.max | 5.28 | 5.28 |
| tabs.first3_issue_ms.median | 1.66 | 1.66 |
| Tab switch to a visited tab, first frame max (ms) | 9.75 | 9.75 |
| Tab switch to a visited tab, first frame median (ms) | 3.84 | 3.84 |
| tabs.frames | 1256 | 1256 |
| tabs.gpu_p50_ms | 5 | 5 |
| tabs.gpu_p90_ms | 10 | 10 |
| tabs.gpu_p99_ms | 13 | 13 |
| Tab switch, hitch time per second of motion (ms/s) | 43.02 | 43.02 |
| tabs.janky_after_first_pct | 1.11 | 1.11 |
| Tab switch, janky frames (%) | 2.23 | 2.23 |
| tabs.late_first_frames | 14 | 14 |
| tabs.p50_ms | 8 | 8 |
| tabs.p90_ms | 17 | 17 |
| tabs.p99_ms | 48 | 48 |
| tabs.transitions | 15 | 15 |
| tabs_tap.first3_dequeue_ms.max | 0.11 | 0.11 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02 |
| tabs_tap.first3_gpu_ms.max | 19.73 | 19.73 |
| tabs_tap.first3_gpu_ms.median | 9.48 | 9.48 |
| tabs_tap.first3_issue_ms.max | 5.83 | 5.83 |
| tabs_tap.first3_issue_ms.median | 1.74 | 1.74 |
| Tab switch with plain taps, first frame max (ms) | 11.46 | 11.46 |
| Tab switch with plain taps, first frame median (ms) | 4.17 | 4.17 |
| tabs_tap.frames | 1263 | 1263 |
| tabs_tap.gpu_p50_ms | 5 | 5 |
| tabs_tap.gpu_p90_ms | 8 | 8 |
| tabs_tap.gpu_p99_ms | 13 | 13 |
| tabs_tap.hitch_ms_per_s | 38.36 | 38.36 |
| tabs_tap.janky_after_first_pct | 1.27 | 1.27 |
| Tab switch with plain taps, janky frames (%) | 2.38 | 2.38 |
| tabs_tap.late_first_frames | 14 | 14 |
| tabs_tap.p50_ms | 8 | 8 |
| tabs_tap.p90_ms | 16 | 16 |
| tabs_tap.p99_ms | 46 | 46 |
| tabs_tap.transitions | 15 | 15 |
