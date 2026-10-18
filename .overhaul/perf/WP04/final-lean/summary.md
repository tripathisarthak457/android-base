# perf.sh: com.example.wp04.lean.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-lean\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 34.4 °C at the start
- Runs: 3, seed 1245, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, idle, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 266 | 266, 280, 266 |
| coldstart.runs_ms | [274, 275, 251, 252, 259, 276] | [279, 264, 273, 269, 257, 238], [292, 257, 257, 280, 314, 280], [274, 275, 251, 252, 259, 276] |
| Cold start as installed (profile from the APK only), median (ms) | 293 | 293, 294, 278 |
| coldstart_installed.runs_ms | [267, 278, 278, 285, 244, 283] | [294, 297, 317, 292, 278, 268], [300, 289, 307, 287, 281, 314], [267, 278, 278, 285, 244, 283] |
| First back swipe after pm clear (cold), janky frames | 3 | 2, 3, 3 |
| First back swipe after a restart (warm), janky frames | 2 | 1, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 14.45 | 14.45, 7.84, 14.67 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.28 | 6.2, 6.52, 6.28 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0] | [0], [0], [0] |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.07, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 21.5 | 21.5, 20.07, 24.27 |
| pushpop.first3_gpu_ms.median | 11.14 | 10.74, 11.14, 11.25 |
| pushpop.first3_issue_ms.max | 6.64 | 7.7, 6.64, 5.12 |
| pushpop.first3_issue_ms.median | 2.14 | 2.14, 2.46, 1.58 |
| pushpop.frames | 1024 | 1022, 1024, 1024 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 7 |
| pushpop.gpu_p90_ms | 9 | 9, 9, 10 |
| pushpop.gpu_p99_ms | 16 | 16, 15, 17 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2 | 0, 2, 3.02 |
| pushpop.janky_after_first_pct | 0 | 0, 0.1, 0 |
| Push/pop with the Back key, janky frames (%) | 1.56 | 1.57, 1.56, 1.56 |
| pushpop.late_first_frames | 16 | 16, 15, 16 |
| pushpop.p50_ms | 10 | 10, 10, 10 |
| pushpop.p90_ms | 16 | 16, 16, 17 |
| Push/pop with the Back key, p99 frame (ms) | 25 | 27, 23, 25 |
| Pop, first frame max (ms) | 5.04 | 5.04, 5.01, 5.58 |
| Pop, first frame median (ms) | 2.27 | 2.26, 2.27, 2.3 |
| Push, the frame that composes the screen, max (ms) | 15.32 | 15.32, 14.82, 17.11 |
| Push, the frame that composes the screen, median (ms) | 6.76 | 7.53, 6.37, 6.76 |
| pushpop.transitions | 16 | 16, 16, 16 |

Not applicable to this app:

- tabs: the app has one tab

| Gate `lean` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | — | 1 | not applicable (the app has one tab) |
| Tab switch to a visited tab, first frame median (ms) | — | 8 | not applicable (the app has one tab) |
| Tab switch to a visited tab, first frame max (ms) | — | 16 | not applicable (the app has one tab) |
| Push/pop with the Back key, janky frames (%) | 1.56 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 25 | 25 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 266 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 293 | 400 | pass |
