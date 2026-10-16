# perf.sh: com.example.wp04.standard.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/final-standard/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 24.2 °C at the start
- Runs: 3, seed 180655, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, scroll_root, idle, meminfo, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 289 | 289, 288, 308 |
| coldstart.runs_ms | [291, 329, 334, 318, 298, 281] | [293, 285, 285, 282, 326, 303], [289, 274, 287, 294, 292, 285], [291, 329, 334, 318, 298, 281] |
| Cold start as installed (profile from the APK only), median (ms) | 321 | 321, 321, 332 |
| coldstart_installed.runs_ms | [325, 339, 309, 308, 354, 373] | [326, 325, 331, 313, 318, 314], [326, 316, 350, 309, 357, 306], [325, 339, 309, 308, 354, 373] |
| First push after pm clear (cold), the frame that composes the screen (ms) | 11.08 | 16.88, 8.97, 11.08 |
| First push after a restart (warm), the frame that composes the screen (ms) | 10.22 | 11.17, 9.95, 10.22 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0] | [0, 0, 0], [0, 0, 0], [0, 0, 0] |
| meminfo.graphics_mb | 54.4 | 54.4, 54.4, 54.4 |
| meminfo.pss_app_mb | 41.5 | 40.8, 41.5, 41.9 |
| PSS after the journey (MB) | 95.9 | 95.3, 95.9, 96.4 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.1 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 25.87 | 26.7, 25.55, 25.87 |
| pushpop.first3_gpu_ms.median | 14.41 | 14.02, 14.59, 14.41 |
| pushpop.first3_issue_ms.max | 7.85 | 7.5, 7.85, 17.3 |
| pushpop.first3_issue_ms.median | 2.52 | 2.52, 2.83, 2.49 |
| pushpop.frames | 1002 | 1005, 1002, 999 |
| pushpop.gpu_p50_ms | 8 | 10, 7, 8 |
| pushpop.gpu_p90_ms | 15 | 13, 15, 15 |
| pushpop.gpu_p99_ms | 22 | 22, 22, 24 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 23.26 | 23.26, 19.37, 24.35 |
| Push/pop with the Back key, janky frames (%) | 2.79 | 2.79, 2.5, 2.8 |
| pushpop.p50_ms | 12 | 17, 11, 12 |
| pushpop.p90_ms | 22 | 22, 22, 22 |
| Push/pop with the Back key, p99 frame (ms) | 31 | 31, 31, 31 |
| Pop, first frame max (ms) | 11.84 | 11.86, 11.84, 11.67 |
| Pop, first frame median (ms) | 8.12 | 8.12, 4.96, 10.04 |
| Push, the frame that composes the screen, max (ms) | 17.13 | 19.84, 16.94, 17.13 |
| Push, the frame that composes the screen, median (ms) | 9.05 | 13.82, 9.05, 8.93 |
| scroll_root.frames | 1356 | 1356, 1356, 1356 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 7, 6 |
| scroll_root.gpu_p99_ms | 9 | 10, 9, 8 |
| Scroll a root list, janky frames (%) | 0.74 | 0.81, 0.74, 0.74 |
| scroll_root.p50_ms | 9 | 9, 9, 9 |
| scroll_root.p90_ms | 10 | 10, 10, 10 |
| scroll_root.p99_ms | 17 | 17, 17, 13 |
| First visit to a tab, first frame max (ms) | 8.59 | 7.31, 8.59, 8.91 |
| First visit to a tab, first frame median (ms) | 6.21 | 5.85, 6.21, 6.75 |
| First visit to a tab after a restart (warm), first frame max (ms) | 5.37 | 5.37, 10.28, 5.27 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.67 | 4.62, 7.18, 4.67 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.05, 0.06, 0.06 |
| tabs.first3_dequeue_ms.median | 0.01 | 0.01, 0.01, 0.02 |
| tabs.first3_gpu_ms.max | 23.25 | 20.87, 25.57, 23.25 |
| tabs.first3_gpu_ms.median | 15.07 | 15.07, 15.06, 15.25 |
| tabs.first3_issue_ms.max | 6.1 | 5.77, 6.1, 6.2 |
| tabs.first3_issue_ms.median | 1.72 | 1.72, 1.72, 2.23 |
| Tab switch to a visited tab, first frame max (ms) | 11.09 | 11.09, 9.55, 12.14 |
| Tab switch to a visited tab, first frame median (ms) | 5.79 | 6.2, 5.79, 5.73 |
| tabs.frames | 782 | 782, 782, 782 |
| tabs.gpu_p50_ms | 7 | 7, 7, 6 |
| tabs.gpu_p90_ms | 12 | 11, 12, 12 |
| tabs.gpu_p99_ms | 17 | 17, 18, 17 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 2.62, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs.p50_ms | 9 | 10, 9, 9 |
| tabs.p90_ms | 18 | 18, 18, 18 |
| tabs.p99_ms | 25 | 25, 26, 25 |

| Gate `standard` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | 1.15 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 5.79 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 11.09 | 16 | pass |
| Pop, first frame median (ms) | 8.12 | 8 | fail |
| Pop, first frame max (ms) | 11.84 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 2.79 | 2 | fail |
| Push/pop with the Back key, p99 frame (ms) | 31 | 25 | fail |
| Scroll a root list, janky frames (%) | 0.74 | 0.5 | fail |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 289 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 321 | 400 | pass |
| PSS after the journey (MB) | 95.9 | 70 | fail |
