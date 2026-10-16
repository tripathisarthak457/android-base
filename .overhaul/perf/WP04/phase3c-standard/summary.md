# perf.sh: com.example.wp04.standard.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/final-standard/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 31.4 °C at the start
- Runs: 3, seed 765583, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 285 | 282, 285, 293 |
| coldstart.runs_ms | [285, 292, 295, 285, 313, 294] | [288, 313, 275, 297, 264, 277], [273, 273, 286, 286, 284, 291], [285, 292, 295, 285, 313, 294] |
| Cold start as installed (profile from the APK only), median (ms) | 312 | 339, 312, 309 |
| coldstart_installed.runs_ms | [301, 312, 323, 307, 316, 304] | [342, 373, 370, 321, 336, 310], [329, 319, 311, 313, 304, 309], [301, 312, 323, 307, 316, 304] |
| First push after pm clear (cold), the frame that composes the screen (ms) | 18.85 | 18.85, 8.36, 19.55 |
| First push after a restart (warm), the frame that composes the screen (ms) | 17.74 | 8.89, 18.31, 17.74 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.4 | 54.4, 54.4, 54.6 |
| PSS after the journey, less the window's graphics buffers (MB) | 41.7 | 41.7, 41.7, 41.9 |
| PSS after the journey (MB) | 96.1 | 96.1, 96.1, 96.5 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.14 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 18.76 | 17.93, 20.08, 18.76 |
| pushpop.first3_gpu_ms.median | 13.28 | 13.14, 13.48, 13.28 |
| pushpop.first3_issue_ms.max | 8.78 | 8.78, 8.92, 8.03 |
| pushpop.first3_issue_ms.median | 2.58 | 2.33, 2.58, 2.6 |
| pushpop.frames | 1013 | 1013, 1013, 1010 |
| pushpop.gpu_p50_ms | 7 | 7, 7, 7 |
| pushpop.gpu_p90_ms | 12 | 12, 12, 12 |
| pushpop.gpu_p99_ms | 18 | 18, 18, 18 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 11.11 | 11.11, 9.19, 13.18 |
| Push/pop with the Back key, janky frames (%) | 1.68 | 1.68, 1.58, 1.68 |
| pushpop.p50_ms | 11 | 11, 11, 12 |
| pushpop.p90_ms | 18 | 18, 18, 19 |
| Push/pop with the Back key, p99 frame (ms) | 27 | 27, 27, 29 |
| Pop, first frame max (ms) | 10.55 | 10.24, 10.55, 11.19 |
| Pop, first frame median (ms) | 6.54 | 4.39, 6.54, 6.84 |
| Push, the frame that composes the screen, max (ms) | 17.38 | 18.21, 9.74, 17.38 |
| Push, the frame that composes the screen, median (ms) | 8.11 | 7.92, 8.23, 8.11 |
| scroll_root.frames | 1358 | 1358, 1355, 1359 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 7, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 8, 7 |
| Scroll a root list, janky frames (%) | 0.52 | 0.37, 0.52, 0.74 |
| scroll_root.p50_ms | 9 | 9, 9, 9 |
| scroll_root.p90_ms | 10 | 10, 10, 10 |
| scroll_root.p99_ms | 13 | 12, 14, 13 |
| First visit to a tab, first frame max (ms) | 10.02 | 4.44, 15.13, 10.02 |
| First visit to a tab, first frame median (ms) | 7.48 | 4.21, 9.53, 7.48 |
| First visit to a tab after a restart (warm), first frame max (ms) | 5.24 | 4.33, 5.24, 5.27 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.55 | 4.23, 4.9, 4.55 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.05, 0.06, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 18.67 | 21.22, 18.67, 17.6 |
| tabs.first3_gpu_ms.median | 13.23 | 13.23, 13.44, 11.83 |
| tabs.first3_issue_ms.max | 5.46 | 4.34, 8.47, 5.46 |
| tabs.first3_issue_ms.median | 1.83 | 1.71, 1.88, 1.83 |
| Tab switch to a visited tab, first frame max (ms) | 9.73 | 10.79, 8.7, 9.73 |
| Tab switch to a visited tab, first frame median (ms) | 5.1 | 6.05, 5.07, 5.1 |
| tabs.frames | 783 | 783, 783, 783 |
| tabs.gpu_p50_ms | 7 | 7, 7, 7 |
| tabs.gpu_p90_ms | 11 | 11, 11, 10 |
| tabs.gpu_p99_ms | 15 | 16, 15, 15 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 1.31, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs.p50_ms | 9 | 9, 9, 9 |
| tabs.p90_ms | 17 | 17, 18, 14 |
| tabs.p99_ms | 22 | 23, 22, 21 |

| Gate `standard` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | 1.15 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 5.1 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.73 | 16 | pass |
| Pop, first frame median (ms) | 6.54 | 8 | pass |
| Pop, first frame max (ms) | 10.55 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.68 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 27 | 25 | fail |
| Scroll a root list, janky frames (%) | 0.52 | 0.5 | fail |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 285 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 312 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) | 41.7 | 48 | pass |
