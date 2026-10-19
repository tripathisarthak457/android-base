# perf.sh: com.example.wp04.standard.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-standard\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 35.8 °C at the start
- Runs: 3, seed 758216, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 266 | 266, 276, 251 |
| coldstart.runs_ms | [248, 239, 263, 248, 254, 265] | [294, 250, 265, 353, 267, 263], [276, 255, 300, 256, 287, 276], [248, 239, 263, 248, 254, 265] |
| Cold start as installed (profile from the APK only), median (ms) | 295 | 295, 299, 293 |
| coldstart_installed.runs_ms | [291, 274, 284, 304, 296, 329] | [283, 273, 294, 336, 298, 296], [302, 327, 313, 288, 280, 296], [291, 274, 284, 304, 296, 329] |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 3, 1 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 3, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.85 | 7.96, 7.85, 7.06 |
| First push after a restart (warm), the frame that composes the screen (ms) | 8.52 | 17.32, 7.65, 8.52 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.9 | 54.8, 54.9, 55.6 |
| PSS after the journey, less the window's graphics buffers (MB) | 50.7 | 50.7, 50.8, 50.6 |
| PSS after the journey (MB) | 105.6 | 105.5, 105.6, 106.2 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.13 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 22.7 | 22.91, 21.64, 22.7 |
| pushpop.first3_gpu_ms.median | 11.37 | 11.37, 11.35, 12.2 |
| pushpop.first3_issue_ms.max | 7.26 | 7.26, 6.89, 7.26 |
| pushpop.first3_issue_ms.median | 2.11 | 1.81, 2.4, 2.11 |
| pushpop.frames | 1020 | 1022, 1020, 1019 |
| pushpop.gpu_p50_ms | 7 | 7, 7, 6 |
| pushpop.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop.gpu_p99_ms | 16 | 15, 17, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.05 | 6.06, 4.05, 3.02 |
| pushpop.janky_after_first_pct | 0.1 | 0.1, 0, 0.2 |
| Push/pop with the Back key, janky frames (%) | 1.57 | 1.57, 1.57, 1.77 |
| pushpop.late_first_frames | 16 | 15, 16, 16 |
| pushpop.p50_ms | 11 | 11, 11, 10 |
| pushpop.p90_ms | 17 | 17, 17, 17 |
| Push/pop with the Back key, p99 frame (ms) | 29 | 27, 30, 29 |
| Pop, first frame max (ms) | 5.2 | 5.31, 5.2, 4.96 |
| Pop, first frame median (ms) | 2.29 | 2.29, 2.27, 2.4 |
| Push, the frame that composes the screen, max (ms) | 16.05 | 14.52, 16.05, 16.5 |
| Push, the frame that composes the screen, median (ms) | 6.65 | 6.31, 10.41, 6.65 |
| pushpop.transitions | 16 | 16, 16, 16 |
| scroll_root.frames | 1359 | 1356, 1359, 1362 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 9, 7 |
| Scroll a root list, janky frames (%) | 0.22 | 0.22, 0.22, 0.15 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 17, 12 |
| First visit to a tab, first frame max (ms) | 8.46 | 6.49, 10.96, 8.46 |
| First visit to a tab, first frame median (ms) | 6.03 | 5.12, 6.99, 6.03 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.1 | 8.97, 7.5, 8.1 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.51 | 5.93, 5.51, 5.46 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.03, 0.07, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 19.05 | 20.87, 18.91, 19.05 |
| tabs.first3_gpu_ms.median | 12.96 | 12.96, 13.41, 12.66 |
| tabs.first3_issue_ms.max | 3.02 | 1.78, 4.11, 3.02 |
| tabs.first3_issue_ms.median | 1.38 | 1.27, 1.44, 1.38 |
| Tab switch to a visited tab, first frame max (ms) | 9.11 | 9.11, 9.33, 7.81 |
| Tab switch to a visited tab, first frame median (ms) | 7.76 | 7.76, 8.5, 3.59 |
| tabs.frames | 783 | 783, 782, 783 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 8 | 8, 8, 8 |
| tabs.gpu_p99_ms | 15 | 15, 16, 15 |
| Tab switch, hitch time per second of motion (ms/s) | 1.27 | 1.31, 0, 1.27 |
| tabs.janky_after_first_pct | 0 | 0.13, 0, 0 |
| Tab switch, janky frames (%) | 1.02 | 1.15, 1.02, 1.02 |
| tabs.late_first_frames | 8 | 8, 8, 8 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 11, 11 |
| tabs.p99_ms | 24 | 24, 24, 18 |
| tabs.transitions | 9 | 9, 9, 9 |

| Gate `standard` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | 1.02 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 7.76 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.11 | 16 | pass |
| Pop, first frame median (ms) | 2.29 | 8 | pass |
| Pop, first frame max (ms) | 5.2 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.57 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 29 | 25 | fail |
| Scroll a root list, janky frames (%) | 0.22 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 266 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 295 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) | 50.7 | 48 | fail |
