# perf.sh: com.example.wp04.lean.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/final-lean/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 30.9 °C at the start
- Runs: 3, seed 213460, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, idle, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 287 | 287, 284, 304 |
| coldstart.runs_ms | [305, 350, 298, 300, 321, 304] | [308, 290, 285, 282, 325, 269], [314, 286, 283, 282, 291, 261], [305, 350, 298, 300, 321, 304] |
| Cold start as installed (profile from the APK only), median (ms) | 316 | 335, 307, 316 |
| coldstart_installed.runs_ms | [378, 328, 305, 304, 347, 304] | [338, 332, 325, 306, 344, 351], [328, 309, 304, 279, 316, 306], [378, 328, 305, 304, 347, 304] |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 1, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 3, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 8.23 | 7.81, 13.96, 8.23 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.26 | 8.32, 7.26, 7.24 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0] | [0], [0], [0] |
| pushpop.first3_dequeue_ms.max | 0.05 | 0.05, 0.05, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 21.23 | 21.23, 23.7, 18.37 |
| pushpop.first3_gpu_ms.median | 13.21 | 13.12, 13.21, 13.29 |
| pushpop.first3_issue_ms.max | 7.74 | 4.58, 7.93, 7.74 |
| pushpop.first3_issue_ms.median | 2.13 | 2.13, 2.01, 2.31 |
| pushpop.frames | 1030 | 1030, 1032, 1029 |
| pushpop.gpu_p50_ms | 7 | 7, 7, 7 |
| pushpop.gpu_p90_ms | 12 | 12, 12, 12 |
| pushpop.gpu_p99_ms | 19 | 19, 19, 19 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 8.05 | 8.05, 7.01, 8.07 |
| Push/pop with the Back key, janky frames (%) | 2.24 | 2.23, 2.33, 2.24 |
| pushpop.p50_ms | 11 | 11, 11, 10 |
| pushpop.p90_ms | 18 | 17, 18, 18 |
| Push/pop with the Back key, p99 frame (ms) | 28 | 27, 28, 28 |
| Pop, first frame max (ms) | 11.19 | 10.95, 12.03, 11.19 |
| Pop, first frame median (ms) | 4.83 | 4.72, 7.85, 4.83 |
| Push, the frame that composes the screen, max (ms) | 19.09 | 19.09, 17.97, 19.92 |
| Push, the frame that composes the screen, median (ms) | 12.2 | 12.2, 12.64, 9.01 |

Not applicable to this app:

- tabs: the app has one tab

| Gate `lean` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | — | 1 | not applicable (the app has one tab) |
| Tab switch to a visited tab, first frame median (ms) | — | 8 | not applicable (the app has one tab) |
| Tab switch to a visited tab, first frame max (ms) | — | 16 | not applicable (the app has one tab) |
| Push/pop with the Back key, janky frames (%) | 2.24 | 2 | fail |
| Push/pop with the Back key, p99 frame (ms) | 28 | 25 | fail |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 287 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 316 | 400 | pass |
