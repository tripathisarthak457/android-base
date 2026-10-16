# perf.sh: com.example.wp04.basestandard.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/base-standard/app/build/outputs/apk/dev/release/app-dev-arm64-v8a-release.apk`
- Device: M2101K6I (9a7c74f4), battery 34.5 °C at the start
- Runs: 3, seed 647120, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop, coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 271 | 274, 271, 268 |
| coldstart.runs_ms | [267, 274, 269, 265, 249, 313] | [273, 285, 286, 276, 268, 266], [245, 269, 267, 278, 274, 277], [267, 274, 269, 265, 249, 313] |
| pushpop.frames | 867 | 867, 867, 859 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop.gpu_p90_ms | 11 | 11, 11, 10 |
| pushpop.gpu_p99_ms | 15 | 15, 15, 15 |
| Push/pop with the Back key, janky frames (%) | 4.73 | 4.73, 4.5, 5.24 |
| pushpop.p50_ms | 8 | 8, 8, 8 |
| pushpop.p90_ms | 17 | 17, 17, 16 |
| Push/pop with the Back key, p99 frame (ms) | 30 | 30, 30, 34 |
| Pop, first frame max (ms) | 20.18 | 20.08, 25.4, 20.18 |
| Pop, first frame median (ms) | 11.96 | 9.48, 11.96, 14.62 |
| Push, the frame that composes the screen, max (ms) | 22.26 | 22.87, 21.14, 22.26 |
| Push, the frame that composes the screen, median (ms) | 8.3 | 8.3, 7.87, 19.33 |
| First visit to a tab, first frame max (ms) | 26.81 | 26.81, 25.47, 27.4 |
| First visit to a tab, first frame median (ms) | 18.77 | 19.21, 17.98, 18.77 |
| Tab switch to a visited tab, first frame max (ms) | 49.93 | 54.52, 45.89, 49.93 |
| Tab switch to a visited tab, first frame median (ms) | 20.52 | 20.52, 19.35, 20.9 |
| tabs.frames | 617 | 617, 614, 618 |
| tabs.gpu_p50_ms | 7 | 6, 7, 7 |
| tabs.gpu_p90_ms | 10 | 10, 11, 10 |
| tabs.gpu_p99_ms | 15 | 14, 17, 15 |
| Tab switch, janky frames (%) | 1.46 | 1.46, 1.47, 1.46 |
| tabs.p50_ms | 9 | 9, 9, 9 |
| tabs.p90_ms | 17 | 17, 17, 17 |
| tabs.p99_ms | 38 | 38, 38, 53 |
