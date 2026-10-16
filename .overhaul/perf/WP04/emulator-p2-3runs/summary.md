# perf.sh: com.example.wp04.p2.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/p2/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 3, seed 978914, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop, pushpop_gesture, rapid_back, idle

| Metric | Median | Runs |
|---|---|---|
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0, 0, 0] | [0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0], [0, 0, 0, 0, 0, 0, 0] |
| pushpop.frames | 280 | 283, 280, 208 |
| pushpop.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Push/pop with the Back key, janky frames (%) | 61.84 | 61.84, 52.5, 78.37 |
| pushpop.p50_ms | 53 | 53, 48, 69 |
| pushpop.p90_ms | 65 | 65, 65, 113 |
| Push/pop with the Back key, p99 frame (ms) | 77 | 77, 77, 150 |
| Pop, first frame max (ms) | 5.36 | 5, 5.36, 7.69 |
| Pop, first frame median (ms) | 4.2 | 4.2, 4.19, 6.63 |
| Push, first frame max (ms) | 10.62 | 8.38, 10.62, 11.29 |
| Push, first frame median (ms) | 5.75 | 5.72, 5.75, 8.5 |
| pushpop_gesture.frames | 320 | 320, 286, 323 |
| pushpop_gesture.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| pushpop_gesture.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| pushpop_gesture.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Push/pop with the back gesture, janky frames (%) | 67.81 | 67.81, 70.28, 60.37 |
| pushpop_gesture.p50_ms | 57 | 57, 61, 53 |
| pushpop_gesture.p90_ms | 81 | 81, 97, 81 |
| pushpop_gesture.p99_ms | 105 | 101, 150, 105 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 2 | 2, 2, 2 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.frames | 104 | 106, 96, 104 |
| rapid_back.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| rapid_back.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| rapid_back.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Rapid Back, janky frames (%) | 61.46 | 65.09, 61.46, 57.69 |
| rapid_back.p50_ms | 53 | 53, 53, 46 |
| rapid_back.p90_ms | 65 | 65, 81, 61 |
| rapid_back.p99_ms | 77 | 77, 150, 73 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 13.03 | 6.46, 17.57, 13.03 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 4.31 | 3.98, 4.45, 4.31 |
| First visit to a tab, first frame max (ms) | 11.09 | 14.69, 10.7, 11.09 |
| First visit to a tab, first frame median (ms) | 7.06 | 7.62, 6.35, 7.06 |
| Tab switch to a visited tab, first frame max (ms) | 2.61 | 3.62, 2.61, 2.31 |
| Tab switch to a visited tab, first frame median (ms) | 1.75 | 2.09, 1.75, 1.65 |
| tabs.frames | 470 | 382, 470, 496 |
| tabs.gpu_p50_ms | 25 | 4950, 25, 24 |
| tabs.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| tabs.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Tab switch, janky frames (%) | 37.66 | 64.14, 37.66, 31.05 |
| tabs.p50_ms | 36 | 48, 36, 32 |
| tabs.p90_ms | 61 | 77, 61, 57 |
| tabs.p99_ms | 89 | 117, 89, 65 |
