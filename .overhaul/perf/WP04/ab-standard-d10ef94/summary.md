# perf.sh: com.example.wp04.abstd.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\ab-standard-d10ef94\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 29.4 °C at the start
- Runs: 3, seed 440118, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 265 | 264, 265, 267 |
| coldstart.runs_ms | [246, 257, 274, 261, 284, 285] | [279, 245, 286, 244, 314, 249], [239, 265, 286, 266, 288, 245], [246, 257, 274, 261, 284, 285] |
| First back swipe after pm clear (cold), janky frames | 1 | 1, 1, 3 |
| First back swipe after a restart (warm), janky frames | 2 | 3, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 11.42 | 11.42, 8.67, 20.93 |
| First push after a restart (warm), the frame that composes the screen (ms) | 10.3 | 19.42, 9.04, 10.3 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.5 | 54.5, 54.5, 54.4 |
| PSS after the journey, less the window's graphics buffers (MB) | 46.5 | 46.5, 46.5, 46 |
| PSS after the journey (MB) | 101 | 101, 101, 100.4 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.14 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.94 | 25.28, 20.94, 19.36 |
| pushpop.first3_gpu_ms.median | 13.71 | 13.71, 14.09, 13.5 |
| pushpop.first3_issue_ms.max | 7.88 | 7.02, 8.29, 7.88 |
| pushpop.first3_issue_ms.median | 2.56 | 2.88, 2.56, 2.5 |
| pushpop.frames | 1009 | 1009, 1009, 1009 |
| pushpop.gpu_p50_ms | 7 | 8, 7, 7 |
| pushpop.gpu_p90_ms | 12 | 12, 12, 12 |
| pushpop.gpu_p99_ms | 18 | 18, 19, 18 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 8.17 | 9.15, 7.15, 8.17 |
| pushpop.janky_after_first_pct | 0.2 | 0.4, 0.2, 0 |
| Push/pop with the Back key, janky frames (%) | 1.78 | 1.98, 1.78, 1.59 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 11 | 12, 11, 11 |
| pushpop.p90_ms | 19 | 19, 19, 18 |
| Push/pop with the Back key, p99 frame (ms) | 31 | 30, 31, 31 |
| Pop, first frame max (ms) | 11.09 | 4.7, 11.09, 11.24 |
| Pop, first frame median (ms) | 7.29 | 4.59, 7.29, 10.56 |
| Push, the frame that composes the screen, max (ms) | 18.77 | 18.63, 18.77, 19.04 |
| Push, the frame that composes the screen, median (ms) | 9.19 | 12.7, 9.02, 9.19 |
| pushpop.transitions | 16 | 16, 16, 16 |
| scroll_root.frames | 1358 | 1360, 1358, 1269 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 7 | 7, 7, 7 |
| scroll_root.gpu_p99_ms | 7 | 7, 7, 8 |
| Scroll a root list, janky frames (%) | 0.52 | 0.37, 0.52, 0.95 |
| scroll_root.p50_ms | 9 | 9, 9, 9 |
| scroll_root.p90_ms | 10 | 10, 10, 11 |
| scroll_root.p99_ms | 13 | 12, 13, 14 |
| First visit to a tab, first frame max (ms) | 4.38 | 4.36, 4.38, 4.39 |
| First visit to a tab, first frame median (ms) | 4.15 | 4.13, 4.3, 4.15 |
| First visit to a tab after a restart (warm), first frame max (ms) | 5.35 | 5.3, 10.6, 5.35 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.78 | 4.57, 6.92, 4.78 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 19.04 | 19.09, 19.04, 17.47 |
| tabs.first3_gpu_ms.median | 13.05 | 12.43, 13.05, 13.18 |
| tabs.first3_issue_ms.max | 5.07 | 5.03, 5.69, 5.07 |
| tabs.first3_issue_ms.median | 1.76 | 1.76, 1.66, 2.01 |
| Tab switch to a visited tab, first frame max (ms) | 5.99 | 12.22, 5.98, 5.99 |
| Tab switch to a visited tab, first frame median (ms) | 5.2 | 6.05, 5.2, 5.15 |
| tabs.frames | 783 | 782, 783, 783 |
| tabs.gpu_p50_ms | 7 | 7, 7, 7 |
| tabs.gpu_p90_ms | 11 | 11, 11, 11 |
| tabs.gpu_p99_ms | 15 | 16, 15, 15 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 1.31, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs.late_first_frames | 9 | 9, 9, 9 |
| tabs.p50_ms | 9 | 9, 9, 10 |
| tabs.p90_ms | 17 | 17, 16, 18 |
| tabs.p99_ms | 22 | 24, 21, 22 |
| tabs.transitions | 9 | 9, 9, 9 |
