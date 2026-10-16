# perf.sh: com.example.wp04.lean.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/final-lean/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 25.6 °C at the start
- Runs: 3, seed 61208, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, idle, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 296 | 313, 296, 270 |
| coldstart.runs_ms | [261, 260, 274, 272, 279, 268] | [280, 310, 312, 316, 383, 315], [270, 328, 298, 310, 294, 293], [261, 260, 274, 272, 279, 268] |
| Cold start as installed (profile from the APK only), median (ms) | 315 | 315, 324, 291 |
| coldstart_installed.runs_ms | [265, 291, 337, 304, 291, 289] | [315, 310, 395, 314, 322, 316], [305, 332, 342, 316, 372, 314], [265, 291, 337, 304, 291, 289] |
| First push after pm clear (cold), the frame that composes the screen (ms) | 8.3 | 17.66, 8.3, 8.18 |
| First push after a restart (warm), the frame that composes the screen (ms) | 8.87 | 8.16, 15.18, 8.87 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0] | [0], [0], [0] |
| pushpop.first3_dequeue_ms.max | 0.05 | 0.05, 0.05, 0.05 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 26.27 | 26.27, 26.45, 25.67 |
| pushpop.first3_gpu_ms.median | 15.32 | 14.71, 16.58, 15.32 |
| pushpop.first3_issue_ms.max | 5.72 | 5.6, 5.72, 6.27 |
| pushpop.first3_issue_ms.median | 2.12 | 2.12, 1.69, 2.13 |
| pushpop.frames | 1008 | 1008, 1012, 1008 |
| pushpop.gpu_p50_ms | 8 | 8, 8, 8 |
| pushpop.gpu_p90_ms | 15 | 15, 15, 14 |
| pushpop.gpu_p99_ms | 21 | 20, 21, 21 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 25.1 | 26.07, 25.1, 21.26 |
| Push/pop with the Back key, janky frames (%) | 2.77 | 3.08, 2.77, 2.58 |
| pushpop.p50_ms | 12 | 12, 13, 12 |
| pushpop.p90_ms | 21 | 21, 21, 21 |
| Push/pop with the Back key, p99 frame (ms) | 29 | 29, 30, 28 |
| Pop, first frame max (ms) | 11.42 | 11.85, 11.11, 11.42 |
| Pop, first frame median (ms) | 4.83 | 4.83, 4.77, 9.23 |
| Push, the frame that composes the screen, max (ms) | 18.46 | 19.81, 18.46, 16.39 |
| Push, the frame that composes the screen, median (ms) | 12.64 | 12.64, 15.82, 8.46 |

Notes:

- run 1: tabs skipped: the app has one tab
- run 2: tabs skipped: the app has one tab
- run 3: tabs skipped: the app has one tab

| Gate `lean` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | — | 1 | not measured |
| Tab switch to a visited tab, first frame median (ms) | — | 8 | not measured |
| Tab switch to a visited tab, first frame max (ms) | — | 16 | not measured |
| Push/pop with the Back key, janky frames (%) | 2.77 | 2 | fail |
| Push/pop with the Back key, p99 frame (ms) | 29 | 25 | fail |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 296 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 315 | 400 | pass |
