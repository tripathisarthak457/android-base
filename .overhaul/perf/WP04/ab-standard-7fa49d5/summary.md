# perf.sh: com.example.wp04.standard.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-standard\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 30.6 °C at the start
- Runs: 3, seed 33303, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 275 | 275, 280, 267 |
| coldstart.runs_ms | [244, 262, 274, 301, 249, 272] | [277, 305, 249, 261, 283, 273], [317, 267, 276, 270, 290, 285], [244, 262, 274, 301, 249, 272] |
| First back swipe after pm clear (cold), janky frames | 2 | 3, 2, 1 |
| First back swipe after a restart (warm), janky frames | 2 | 1, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 17.3 | 17.66, 7.92, 17.3 |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.85 | 19.65, 15.85, 7.39 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.9 | 54.9, 54.9, 54.9 |
| PSS after the journey, less the window's graphics buffers (MB) | 48.3 | 48.3, 48.2, 48.3 |
| PSS after the journey (MB) | 103.2 | 103.2, 103, 103.2 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.11, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 22.69 | 22.69, 20.44, 23.16 |
| pushpop.first3_gpu_ms.median | 11.06 | 10.95, 12.46, 11.06 |
| pushpop.first3_issue_ms.max | 8.16 | 8.16, 5.97, 8.41 |
| pushpop.first3_issue_ms.median | 1.96 | 2.39, 1.96, 1.95 |
| pushpop.frames | 1022 | 1023, 1021, 1022 |
| pushpop.gpu_p50_ms | 7 | 7, 6, 7 |
| pushpop.gpu_p90_ms | 9 | 10, 9, 9 |
| pushpop.gpu_p99_ms | 16 | 16, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2 | 2, 2.04, 1.01 |
| pushpop.janky_after_first_pct | 0.1 | 0.29, 0, 0.1 |
| Push/pop with the Back key, janky frames (%) | 1.66 | 1.76, 1.57, 1.66 |
| pushpop.late_first_frames | 16 | 15, 16, 16 |
| pushpop.p50_ms | 10 | 11, 10, 10 |
| pushpop.p90_ms | 17 | 17, 17, 17 |
| Push/pop with the Back key, p99 frame (ms) | 28 | 27, 29, 28 |
| Pop, first frame max (ms) | 4.96 | 4.76, 4.96, 5.54 |
| Pop, first frame median (ms) | 2.32 | 2.24, 2.32, 2.4 |
| Push, the frame that composes the screen, max (ms) | 15.69 | 15.4, 15.96, 15.69 |
| Push, the frame that composes the screen, median (ms) | 6.97 | 6.96, 14, 6.97 |
| pushpop.transitions | 16 | 16, 16, 16 |
| scroll_root.frames | 1363 | 1364, 1363, 1360 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 8 | 8, 7, 8 |
| Scroll a root list, janky frames (%) | 0.29 | 0.29, 0.22, 0.29 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 12, 16 |
| First visit to a tab, first frame max (ms) | 7.87 | 4.13, 8.66, 7.87 |
| First visit to a tab, first frame median (ms) | 5.64 | 3.5, 5.75, 5.64 |
| First visit to a tab after a restart (warm), first frame max (ms) | 4.2 | 7.97, 4.2, 3.65 |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.81 | 6.01, 3.81, 3.63 |
| tabs.first3_dequeue_ms.max | 0.05 | 0.08, 0.05, 0.04 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.17 | 20.23, 18.61, 20.17 |
| tabs.first3_gpu_ms.median | 12.52 | 12.52, 12.5, 12.71 |
| tabs.first3_issue_ms.max | 3.99 | 3.82, 4.04, 3.99 |
| tabs.first3_issue_ms.median | 1.38 | 1.43, 1.38, 1.38 |
| Tab switch to a visited tab, first frame max (ms) | 8.95 | 9.15, 8.95, 8.12 |
| Tab switch to a visited tab, first frame median (ms) | 3.41 | 3.77, 3.41, 3.4 |
| tabs.frames | 783 | 783, 783, 783 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 7 | 8, 7, 7 |
| tabs.gpu_p99_ms | 15 | 15, 14, 15 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 1.31, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.02, 1.15, 1.15 |
| tabs.late_first_frames | 9 | 8, 9, 9 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 10, 11 |
| tabs.p99_ms | 20 | 20, 19, 20 |
| tabs.transitions | 9 | 9, 9, 9 |
