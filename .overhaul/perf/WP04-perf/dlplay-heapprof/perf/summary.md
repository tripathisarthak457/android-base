# perf.sh: com.example.wp04.dlplay.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\dlplay\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 31.2 °C at the start
- Runs: 1, seed 834559, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, meminfo, scroll_minimize

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 5 | 5 |
| First back swipe after a restart (warm), janky frames | 6 | 6 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.49 | 7.49 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.27 | 7.27 |
| meminfo.graphics_mb | 55 | 55 |
| PSS after the journey, less the window's graphics buffers (MB) | 64.4 | 64.4 |
| PSS after the journey (MB) | 119.5 | 119.5 |
| pushpop.first3_dequeue_ms.max | 0.07 | 0.07 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02 |
| pushpop.first3_gpu_ms.max | 19.22 | 19.22 |
| pushpop.first3_gpu_ms.median | 11.04 | 11.04 |
| pushpop.first3_issue_ms.max | 8.37 | 8.37 |
| pushpop.first3_issue_ms.median | 2.25 | 2.25 |
| pushpop.frames | 784 | 784 |
| pushpop.gpu_p50_ms | 7 | 7 |
| pushpop.gpu_p90_ms | 11 | 11 |
| pushpop.gpu_p99_ms | 16 | 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 237.26 | 237.26 |
| pushpop.janky_after_first_pct | 7.78 | 7.78 |
| Push/pop with the Back key, janky frames (%) | 9.82 | 9.82 |
| pushpop.late_first_frames | 16 | 16 |
| pushpop.p50_ms | 16 | 16 |
| pushpop.p90_ms | 46 | 46 |
| Push/pop with the Back key, p99 frame (ms) | 81 | 81 |
| Pop, first frame max (ms) | 5.58 | 5.58 |
| Pop, first frame median (ms) | 2.48 | 2.48 |
| Push, the frame that composes the screen, max (ms) | 16.74 | 16.74 |
| Push, the frame that composes the screen, median (ms) | 10.95 | 10.95 |
| pushpop.transitions | 16 | 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.07 | 0.07 |
| pushpop_gesture.first3_dequeue_ms.median | 0.03 | 0.03 |
| pushpop_gesture.first3_gpu_ms.max | 20.74 | 20.74 |
| pushpop_gesture.first3_gpu_ms.median | 11.66 | 11.66 |
| pushpop_gesture.first3_issue_ms.max | 5.22 | 5.22 |
| pushpop_gesture.first3_issue_ms.median | 1.92 | 1.92 |
| pushpop_gesture.frames | 568 | 568 |
| pushpop_gesture.gesture_first_frame_ms.max | 2.73 | 2.73 |
| Back gesture, slowest of its first three frames, median (ms) | 1.59 | 1.59 |
| pushpop_gesture.gpu_p50_ms | 7 | 7 |
| pushpop_gesture.gpu_p90_ms | 11 | 11 |
| pushpop_gesture.gpu_p99_ms | 16 | 16 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 256.01 | 256.01 |
| pushpop_gesture.janky_after_first_pct | 7.75 | 7.75 |
| Push/pop with the back gesture, janky frames (%) | 9.86 | 9.86 |
| pushpop_gesture.late_first_frames | 12 | 12 |
| pushpop_gesture.p50_ms | 14 | 14 |
| pushpop_gesture.p90_ms | 48 | 48 |
| pushpop_gesture.p99_ms | 85 | 85 |
| pushpop_gesture.transitions | 12 | 12 |
| Rapid Back: the app is still running | yes | yes |
| rapid_back.depth | 4 | 4 |
| Rapid Back: lands on the expected screen | yes | yes |
| rapid_back.first3_dequeue_ms.max | 2.43 | 2.43 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02 |
| rapid_back.first3_gpu_ms.max | 11.39 | 11.39 |
| rapid_back.first3_gpu_ms.median | 5.48 | 5.48 |
| rapid_back.first3_issue_ms.max | 6.7 | 6.7 |
| rapid_back.first3_issue_ms.median | 2.19 | 2.19 |
| rapid_back.frames | 516 | 516 |
| rapid_back.gpu_p50_ms | 7 | 7 |
| rapid_back.gpu_p90_ms | 10 | 10 |
| rapid_back.gpu_p99_ms | 13 | 13 |
| Rapid Back, hitch time per second of motion (ms/s) | 251.17 | 251.17 |
| rapid_back.janky_after_first_pct | 7.36 | 7.36 |
| Rapid Back, janky frames (%) | 7.95 | 7.95 |
| rapid_back.late_first_frames | 3 | 3 |
| rapid_back.p50_ms | 16 | 16 |
| rapid_back.p90_ms | 34 | 34 |
| rapid_back.p99_ms | 77 | 77 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 22.56 | 22.56 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 9.93 | 9.93 |
| rapid_back.transitions | 6 | 6 |
| scroll_minimize.frames | 1343 | 1343 |
| scroll_minimize.gpu_p50_ms | 6 | 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6 |
| scroll_minimize.gpu_p99_ms | 8 | 8 |
| Scroll with the bar minimizing, janky frames (%) | 1.04 | 1.04 |
| scroll_minimize.p50_ms | 9 | 9 |
| scroll_minimize.p90_ms | 12 | 12 |
| scroll_minimize.p99_ms | 15 | 15 |
| scroll_root.frames | 1298 | 1298 |
| scroll_root.gpu_p50_ms | 6 | 6 |
| Scroll a root list, GPU p90 (ms) | 7 | 7 |
| scroll_root.gpu_p99_ms | 10 | 10 |
| Scroll a root list, janky frames (%) | 2 | 2 |
| scroll_root.p50_ms | 9 | 9 |
| scroll_root.p90_ms | 14 | 14 |
| scroll_root.p99_ms | 17 | 17 |
| tab1.first_frame_ms.max | 10.09 | 10.09 |
| tab1.first_frame_ms.median | 6 | 6 |
| First visit to a tab, first frame max (ms) | 10.12 | 10.12 |
| First visit to a tab, first frame median (ms) | 4.09 | 4.09 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.73 | 8.73 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.55 | 6.55 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02 |
| tabs.first3_gpu_ms.max | 19.99 | 19.99 |
| tabs.first3_gpu_ms.median | 10.21 | 10.21 |
| tabs.first3_issue_ms.max | 6.01 | 6.01 |
| tabs.first3_issue_ms.median | 1.63 | 1.63 |
| Tab switch to a visited tab, first frame max (ms) | 10.1 | 10.1 |
| Tab switch to a visited tab, first frame median (ms) | 4.32 | 4.32 |
| tabs.frames | 1256 | 1256 |
| tabs.gpu_p50_ms | 5 | 5 |
| tabs.gpu_p90_ms | 9 | 9 |
| tabs.gpu_p99_ms | 13 | 13 |
| Tab switch, hitch time per second of motion (ms/s) | 43.02 | 43.02 |
| tabs.janky_after_first_pct | 1.27 | 1.27 |
| Tab switch, janky frames (%) | 2.31 | 2.31 |
| tabs.late_first_frames | 13 | 13 |
| tabs.p50_ms | 8 | 8 |
| tabs.p90_ms | 17 | 17 |
| tabs.p99_ms | 53 | 53 |
| tabs.transitions | 15 | 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02 |
| tabs_tap.first3_gpu_ms.max | 18.08 | 18.08 |
| tabs_tap.first3_gpu_ms.median | 9.75 | 9.75 |
| tabs_tap.first3_issue_ms.max | 5.84 | 5.84 |
| tabs_tap.first3_issue_ms.median | 1.81 | 1.81 |
| Tab switch with plain taps, first frame max (ms) | 10.78 | 10.78 |
| Tab switch with plain taps, first frame median (ms) | 3.78 | 3.78 |
| tabs_tap.frames | 1278 | 1278 |
| tabs_tap.gpu_p50_ms | 5 | 5 |
| tabs_tap.gpu_p90_ms | 9 | 9 |
| tabs_tap.gpu_p99_ms | 13 | 13 |
| tabs_tap.hitch_ms_per_s | 26.52 | 26.52 |
| tabs_tap.janky_after_first_pct | 0.78 | 0.78 |
| Tab switch with plain taps, janky frames (%) | 1.96 | 1.96 |
| tabs_tap.late_first_frames | 15 | 15 |
| tabs_tap.p50_ms | 8 | 8 |
| tabs_tap.p90_ms | 17 | 17 |
| tabs_tap.p99_ms | 40 | 40 |
| tabs_tap.transitions | 15 | 15 |
