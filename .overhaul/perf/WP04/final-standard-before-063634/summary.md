# perf.sh: com.example.wp04.standard.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-standard\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 33.1 °C at the start
- Runs: 3, seed 199651, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 260 | 266, 260, 258 |
| coldstart.runs_ms | [254, 264, 259, 295, 248, 257] | [297, 287, 262, 234, 270, 262], [254, 261, 237, 272, 259, 277], [254, 264, 259, 295, 248, 257] |
| Cold start as installed (profile from the APK only), median (ms) | 302 | 313, 302, 301 |
| coldstart_installed.runs_ms | [298, 304, 286, 337, 288, 313] | [419, 323, 288, 304, 306, 321], [314, 275, 309, 270, 314, 296], [298, 304, 286, 337, 288, 313] |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.98 | 17.23, 6.75, 6.98 |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.71 | 15.83, 15.71, 7.46 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 55.3 | 54.8, 55.3, 55.4 |
| PSS after the journey, less the window's graphics buffers (MB) | 50.4 | 51.1, 50.4, 50.4 |
| PSS after the journey (MB) | 105.7 | 105.9, 105.7, 105.7 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.07 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.89 | 22.57, 20.89, 20.3 |
| pushpop.first3_gpu_ms.median | 11.67 | 11.7, 10.6, 11.67 |
| pushpop.first3_issue_ms.max | 4.82 | 4.82, 6.65, 4.81 |
| pushpop.first3_issue_ms.median | 1.94 | 1.78, 1.96, 1.94 |
| pushpop.frames | 1023 | 1021, 1023, 1023 |
| pushpop.gpu_p50_ms | 6 | 6, 7, 6 |
| pushpop.gpu_p90_ms | 10 | 10, 10, 9 |
| pushpop.gpu_p99_ms | 16 | 18, 15, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 3.08 | 4.04, 3.08, 3.04 |
| pushpop.janky_after_first_pct | 0.1 | 0.1, 0, 0.29 |
| Push/pop with the Back key, janky frames (%) | 1.67 | 1.67, 1.56, 1.76 |
| pushpop.late_first_frames | 16 | 16, 16, 15 |
| pushpop.p50_ms | 10 | 10, 11, 10 |
| pushpop.p90_ms | 17 | 17, 17, 16 |
| Push/pop with the Back key, p99 frame (ms) | 26 | 30, 24, 26 |
| Pop, first frame max (ms) | 5.13 | 5.25, 5.13, 2.34 |
| Pop, first frame median (ms) | 2.29 | 3.67, 2.19, 2.29 |
| Push, the frame that composes the screen, max (ms) | 14.73 | 14.61, 15.61, 14.73 |
| Push, the frame that composes the screen, median (ms) | 6.97 | 9.43, 6.29, 6.97 |
| pushpop.transitions | 16 | 16, 16, 16 |
| scroll_root.frames | 1360 | 1360, 1356, 1360 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 7, 7 |
| Scroll a root list, janky frames (%) | 0.29 | 0.29, 0.29, 0.29 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 12, 12 |
| First visit to a tab, first frame max (ms) | 6.11 | 6.11, 8.4, 4.09 |
| First visit to a tab, first frame median (ms) | 4.88 | 4.88, 6.03, 3.83 |
| First visit to a tab after a restart (warm), first frame max (ms) | 3.8 | 4.08, 3.8, 3.6 |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.7 | 3.94, 3.7, 3.29 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.11, 0.06, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.01, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 21.16 | 23.43, 21.16, 19.77 |
| tabs.first3_gpu_ms.median | 12.61 | 12.61, 12.73, 12.37 |
| tabs.first3_issue_ms.max | 4.6 | 6.44, 4.6, 4.11 |
| tabs.first3_issue_ms.median | 1.63 | 1.35, 1.63, 1.98 |
| Tab switch to a visited tab, first frame max (ms) | 9.05 | 9.05, 8.65, 9.26 |
| Tab switch to a visited tab, first frame median (ms) | 3.92 | 3.92, 3.84, 6.02 |
| tabs.frames | 783 | 782, 783, 783 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 7 | 7, 7, 10 |
| tabs.gpu_p99_ms | 14 | 14, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 1.31, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs.late_first_frames | 9 | 9, 9, 9 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 10, 11, 14 |
| tabs.p99_ms | 20 | 20, 20, 23 |
| tabs.transitions | 9 | 9, 9, 9 |

| Gate `standard` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | 1.15 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 3.92 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.05 | 16 | pass |
| Pop, first frame median (ms) | 2.29 | 8 | pass |
| Pop, first frame max (ms) | 5.13 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.67 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 26 | 25 | fail |
| Scroll a root list, janky frames (%) | 0.29 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 260 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 302 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) | 50.4 | 48 | fail |
