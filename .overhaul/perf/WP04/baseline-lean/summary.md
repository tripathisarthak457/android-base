# perf.sh: com.example.wp04.baselean.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/base-lean/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 35.4 °C at the start
- Runs: 3, seed 199427, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop, coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 306 | 292, 309, 306 |
| coldstart.runs_ms | [298, 292, 309, 304, 324, 316] | [294, 276, 311, 334, 290, 271], [278, 310, 320, 274, 309, 313], [298, 292, 309, 304, 324, 316] |
| pushpop.frames | 865 | 866, 865, 860 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop.gpu_p90_ms | 10 | 10, 10, 11 |
| pushpop.gpu_p99_ms | 14 | 14, 14, 14 |
| Push/pop with the Back key, janky frames (%) | 4.73 | 4.73, 4.62, 5.23 |
| pushpop.p50_ms | 8 | 8, 8, 8 |
| pushpop.p90_ms | 17 | 17, 16, 18 |
| Push/pop with the Back key, p99 frame (ms) | 32 | 38, 30, 32 |
| Pop, first frame max (ms) | 16.08 | 56.9, 16.03, 16.08 |
| Pop, first frame median (ms) | 10.66 | 14.75, 10.66, 7.8 |
| Push, the frame that composes the screen, max (ms) | 19.37 | 19.27, 19.37, 19.62 |
| Push, the frame that composes the screen, median (ms) | 8.71 | 8.71, 8.67, 11.36 |

Notes:

- run 1: tabs skipped: the app has one tab
- run 2: tabs skipped: the app has one tab
- run 3: tabs skipped: the app has one tab
