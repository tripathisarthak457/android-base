# perf.sh: com.example.wp04.standard.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-standard\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 31.5 °C at the start
- Runs: 3, seed 403877, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 270 | 281, 259, 270 |
| coldstart.runs_ms | [249, 266, 258, 277, 274, 287] | [299, 271, 291, 269, 326, 245], [380, 245, 244, 259, 259, 261], [249, 266, 258, 277, 274, 287] |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.09 | 7.09, 7.02, 17.11 |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.61 | 8.28, 15.61, 18.46 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.8 | 54.8, 54.8, 54.8 |
| PSS after the journey, less the window's graphics buffers (MB) | 42.1 | 42.1, 41.8, 42.8 |
| PSS after the journey (MB) | 96.9 | 96.9, 96.6, 97.5 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.08, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.54 | 20.45, 20.54, 21.64 |
| pushpop.first3_gpu_ms.median | 12.59 | 12.59, 11.45, 12.92 |
| pushpop.first3_issue_ms.max | 7.59 | 7.59, 7.58, 7.78 |
| pushpop.first3_issue_ms.median | 2.19 | 2.19, 2.5, 1.83 |
| pushpop.frames | 1021 | 1022, 1021, 1021 |
| pushpop.gpu_p50_ms | 7 | 7, 7, 6 |
| pushpop.gpu_p90_ms | 9 | 9, 10, 9 |
| pushpop.gpu_p99_ms | 16 | 15, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 1.03 | 3, 1.03, 1 |
| pushpop.janky_after_first_pct | 0.1 | 0, 0.1, 0.2 |
| Push/pop with the Back key, janky frames (%) | 1.57 | 1.47, 1.57, 1.67 |
| pushpop.late_first_frames | 15 | 15, 15, 15 |
| pushpop.p50_ms | 11 | 11, 11, 10 |
| pushpop.p90_ms | 17 | 17, 17, 16 |
| Push/pop with the Back key, p99 frame (ms) | 26 | 26, 26, 26 |
| Pop, first frame max (ms) | 5.21 | 5.43, 5.08, 5.21 |
| Pop, first frame median (ms) | 2.33 | 2.33, 2.35, 2.29 |
| Push, the frame that composes the screen, max (ms) | 15.48 | 15.89, 15.48, 14.61 |
| Push, the frame that composes the screen, median (ms) | 7.03 | 7.15, 7.03, 7.03 |
| pushpop.transitions | 16 | 16, 16, 16 |
| scroll_root.frames | 1308 | 1301, 1308, 1356 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 8, 7, 7 |
| Scroll a root list, janky frames (%) | 0.38 | 0.46, 0.38, 0.37 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 10 | 10, 10, 9 |
| scroll_root.p99_ms | 13 | 14, 13, 12 |
| First visit to a tab, first frame max (ms) | 4.12 | 7.63, 4.12, 3.66 |
| First visit to a tab, first frame median (ms) | 3.6 | 5.63, 3.6, 3.27 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.33 | 8.33, 8.29, 8.48 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.82 | 5.67, 7.38, 5.82 |
| tabs.first3_dequeue_ms.max | 0.05 | 0.05, 0.04, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.26 | 19.34, 21.89, 20.26 |
| tabs.first3_gpu_ms.median | 12.84 | 12.47, 12.84, 13.29 |
| tabs.first3_issue_ms.max | 4.33 | 5.33, 3.85, 4.33 |
| tabs.first3_issue_ms.median | 1.85 | 2, 1.45, 1.85 |
| Tab switch to a visited tab, first frame max (ms) | 9.06 | 4.06, 9.36, 9.06 |
| Tab switch to a visited tab, first frame median (ms) | 3.93 | 3.49, 7.51, 3.93 |
| tabs.frames | 783 | 783, 783, 783 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 8 | 7, 9, 8 |
| tabs.gpu_p99_ms | 15 | 14, 15, 15 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 1.3, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0.13, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs.late_first_frames | 9 | 8, 9, 9 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 13, 11 |
| tabs.p99_ms | 21 | 19, 21, 21 |
| tabs.transitions | 9 | 9, 9, 9 |
