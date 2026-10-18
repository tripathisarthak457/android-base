# perf.sh: com.example.wp04.standard.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-standard\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 34.8 °C at the start
- Runs: 3, seed 455863, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 269 | 265, 269, 275 |
| coldstart.runs_ms | [277, 265, 255, 274, 307, 278] | [264, 267, 260, 263, 372, 269], [258, 275, 262, 282, 304, 264], [277, 265, 255, 274, 307, 278] |
| Cold start as installed (profile from the APK only), median (ms) | 295 | 282, 305, 295 |
| coldstart_installed.runs_ms | [295, 293, 337, 294, 295, 338] | [289, 291, 275, 276, 265, 294], [323, 312, 417, 286, 299, 280], [295, 293, 337, 294, 295, 338] |
| First back swipe after pm clear (cold), janky frames | 1 | 1, 1, 1 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 16.23 | 17.84, 16.23, 15.53 |
| First push after a restart (warm), the frame that composes the screen (ms) | 8.11 | 8.11, 7.84, 15.41 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.9 | 55.7, 54.9, 54.9 |
| PSS after the journey, less the window's graphics buffers (MB) | 48 | 48.3, 47.1, 48 |
| PSS after the journey (MB) | 102.9 | 104, 102, 102.9 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.07, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.85 | 19.13, 23.79, 20.85 |
| pushpop.first3_gpu_ms.median | 10.68 | 12.02, 10.68, 10.66 |
| pushpop.first3_issue_ms.max | 5.19 | 4.5, 5.19, 9.25 |
| pushpop.first3_issue_ms.median | 1.96 | 1.96, 1.88, 2.08 |
| pushpop.frames | 1021 | 1020, 1021, 1023 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop.gpu_p90_ms | 9 | 9, 9, 9 |
| pushpop.gpu_p99_ms | 16 | 16, 17, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 1.02 | 1.02, 1.97, 1.02 |
| pushpop.janky_after_first_pct | 0.1 | 0, 0.1, 0.1 |
| Push/pop with the Back key, janky frames (%) | 1.56 | 1.57, 1.47, 1.56 |
| pushpop.late_first_frames | 15 | 16, 14, 15 |
| pushpop.p50_ms | 10 | 10, 10, 10 |
| pushpop.p90_ms | 16 | 16, 16, 16 |
| Push/pop with the Back key, p99 frame (ms) | 28 | 29, 28, 26 |
| Pop, first frame max (ms) | 4.97 | 5.46, 4.97, 4.53 |
| Pop, first frame median (ms) | 2.28 | 4.82, 1.96, 2.28 |
| Push, the frame that composes the screen, max (ms) | 15.77 | 15.61, 15.92, 15.77 |
| Push, the frame that composes the screen, median (ms) | 10.26 | 10.26, 6.63, 12.71 |
| pushpop.transitions | 16 | 16, 16, 16 |
| scroll_root.frames | 1362 | 1363, 1308, 1362 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 8 | 7, 9, 8 |
| Scroll a root list, janky frames (%) | 0.22 | 0.15, 0.38, 0.22 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 10, 9 |
| scroll_root.p99_ms | 16 | 12, 17, 16 |
| First visit to a tab, first frame max (ms) | 8.25 | 9.45, 8.25, 4.17 |
| First visit to a tab, first frame median (ms) | 5.81 | 6.32, 5.81, 3.92 |
| First visit to a tab after a restart (warm), first frame max (ms) | 6.39 | 6.54, 6.39, 4.14 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.33 | 5.33, 5.5, 3.78 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.05 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 19.84 | 19.7, 19.84, 21.18 |
| tabs.first3_gpu_ms.median | 12.87 | 12.67, 12.87, 13.07 |
| tabs.first3_issue_ms.max | 4.45 | 4.45, 4.21, 4.78 |
| tabs.first3_issue_ms.median | 1.45 | 1.46, 1.37, 1.45 |
| Tab switch to a visited tab, first frame max (ms) | 8.13 | 6.07, 8.13, 9.12 |
| Tab switch to a visited tab, first frame median (ms) | 3.47 | 3.41, 3.47, 3.84 |
| tabs.frames | 783 | 783, 783, 783 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 9 | 7, 10, 9 |
| tabs.gpu_p99_ms | 15 | 15, 14, 16 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs.late_first_frames | 9 | 9, 9, 9 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 13 | 10, 14, 13 |
| tabs.p99_ms | 21 | 20, 21, 21 |
| tabs.transitions | 9 | 9, 9, 9 |

| Gate `standard` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | 1.15 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 3.47 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 8.13 | 16 | pass |
| Pop, first frame median (ms) | 2.28 | 8 | pass |
| Pop, first frame max (ms) | 4.97 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.56 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 28 | 25 | fail |
| Scroll a root list, janky frames (%) | 0.22 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 269 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 295 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) | 48 | 48 | pass |
