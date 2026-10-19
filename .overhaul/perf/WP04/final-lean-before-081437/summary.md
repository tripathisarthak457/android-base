# perf.sh: com.example.wp04.lean.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-lean\app\build\outputs\apk\dev\release\app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 34.7 °C at the start
- Runs: 3, seed 933525, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, pushpop, idle, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 257 | 257, 257, 265 |
| coldstart.runs_ms | [265, 266, 246, 270, 285, 247] | [244, 262, 304, 258, 256, 235], [254, 261, 278, 248, 285, 254], [265, 266, 246, 270, 285, 247] |
| Cold start as installed (profile from the APK only), median (ms) | 284 | 278, 288, 284 |
| coldstart_installed.runs_ms | [297, 310, 258, 272, 306, 243] | [273, 279, 278, 275, 289, 290], [265, 297, 267, 299, 281, 295], [297, 310, 258, 272, 306, 243] |
| First back swipe after pm clear (cold), janky frames | 3 | 3, 3, 1 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 4, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.73 | 7.73, 6.29, 14 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.37 | 6.37, 9.14, 5.91 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0] | [0], [0], [0] |
| pushpop.first3_dequeue_ms.max | 0.07 | 0.06, 0.08, 0.07 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.03 |
| pushpop.first3_gpu_ms.max | 21.18 | 20.72, 22.42, 21.18 |
| pushpop.first3_gpu_ms.median | 10.91 | 10.91, 11.48, 10.52 |
| pushpop.first3_issue_ms.max | 6.82 | 5.84, 6.82, 7.68 |
| pushpop.first3_issue_ms.median | 2.37 | 1.65, 2.37, 2.74 |
| pushpop.frames | 1023 | 1024, 1022, 1023 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 7 |
| pushpop.gpu_p90_ms | 9 | 9, 9, 10 |
| pushpop.gpu_p99_ms | 16 | 16, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 3.01 | 3.02, 1.01, 3.01 |
| pushpop.janky_after_first_pct | 0 | 0.1, 0, 0 |
| Push/pop with the Back key, janky frames (%) | 1.56 | 1.46, 1.57, 1.56 |
| pushpop.late_first_frames | 16 | 14, 16, 16 |
| pushpop.p50_ms | 10 | 10, 10, 10 |
| pushpop.p90_ms | 16 | 16, 16, 17 |
| Push/pop with the Back key, p99 frame (ms) | 26 | 24, 26, 26 |
| Pop, first frame max (ms) | 4.81 | 4.81, 5.05, 2.34 |
| Pop, first frame median (ms) | 2.25 | 2.26, 2.21, 2.25 |
| Push, the frame that composes the screen, max (ms) | 16.13 | 16.13, 15.51, 16.47 |
| Push, the frame that composes the screen, median (ms) | 6.75 | 6.53, 10.4, 6.75 |
| pushpop.transitions | 16 | 16, 16, 16 |

Not applicable to this app:

- tabs: the app has one tab

| Gate `lean` | Value | Limit | Result |
|---|---|---|---|
| Tab switch, janky frames (%) | — | 1 | not applicable (the app has one tab) |
| Tab switch to a visited tab, first frame median (ms) | — | 8 | not applicable (the app has one tab) |
| Tab switch to a visited tab, first frame max (ms) | — | 16 | not applicable (the app has one tab) |
| Push/pop with the Back key, janky frames (%) | 1.56 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 26 | 25 | fail |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 257 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 284 | 400 | pass |
