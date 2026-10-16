# perf.sh: com.example.wp04.base.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/base/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 3, seed 750693, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop, pushpop_gesture, rapid_back, idle

| Metric | Median | Runs |
|---|---|---|
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0, 0, 0] | [0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0] |
| pushpop.frames | 265 | 264, 265, 404 |
| pushpop.gpu_p50_ms | 4950 | 4950, 4950, 25 |
| pushpop.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Push/pop with the Back key, janky frames (%) | 78.03 | 78.03, 83.4, 44.31 |
| pushpop.p50_ms | 69 | 69, 69, 40 |
| pushpop.p90_ms | 113 | 113, 121, 57 |
| Push/pop with the Back key, p99 frame (ms) | 150 | 150, 150, 69 |
| Pop, first frame max (ms) | 8.79 | 8.79, 21.82, 4.14 |
| Pop, first frame median (ms) | 6.67 | 7.32, 6.67, 3.64 |
| Push, first frame max (ms) | 2.25 | 2.43, 2.25, 1.8 |
| Push, first frame median (ms) | 1.69 | 1.69, 1.87, 1.1 |
| pushpop_gesture.frames | 172 | 157, 172, 268 |
| pushpop_gesture.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| pushpop_gesture.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| pushpop_gesture.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Push/pop with the back gesture, janky frames (%) | 84.88 | 91.72, 84.88, 54.48 |
| pushpop_gesture.p50_ms | 85 | 93, 85, 46 |
| pushpop_gesture.p90_ms | 150 | 150, 150, 69 |
| pushpop_gesture.p99_ms | 200 | 200, 200, 89 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 2 | 2, 2, 2 |
| Rapid Back: lands on the expected screen | no | no, no, no |
| rapid_back.frames | 65 | 48, 65, 68 |
| rapid_back.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| rapid_back.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| rapid_back.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Rapid Back, janky frames (%) | 98.53 | 100, 96.92, 98.53 |
| rapid_back.p50_ms | 129 | 200, 129, 109 |
| rapid_back.p90_ms | 200 | 350, 200, 150 |
| rapid_back.p99_ms | 300 | 1350, 300, 200 |
| First visit to a tab, first frame max (ms) | 49.45 | 77.55, 49.45, 8.87 |
| First visit to a tab, first frame median (ms) | 11.73 | 11.73, 17.32, 6.45 |
| Tab switch to a visited tab, first frame max (ms) | 12.33 | 12.33, 33.96, 8.06 |
| Tab switch to a visited tab, first frame median (ms) | 7.55 | 7.55, 13.72, 5.46 |
| tabs.frames | 356 | 356, 236, 449 |
| tabs.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| tabs.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| tabs.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Tab switch, janky frames (%) | 76.12 | 76.12, 92.37, 56.57 |
| tabs.p50_ms | 65 | 65, 109, 48 |
| tabs.p90_ms | 101 | 101, 150, 73 |
| tabs.p99_ms | 150 | 150, 200, 97 |

Notes:

- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 1: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 2: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
- run 3: rapid_back from depth 2: three Back presses left the app
