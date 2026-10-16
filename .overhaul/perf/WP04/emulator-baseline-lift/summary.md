# perf.sh: com.example.wp04.base.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/base/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 3, seed 383323, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop

| Metric | Median | Runs |
|---|---|---|
| pushpop.frames | 198 | 198, 182, 237 |
| pushpop.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Push/pop with the Back key, janky frames (%) | 93.43 | 93.43, 98.35, 90.3 |
| pushpop.p50_ms | 117 | 117, 129, 85 |
| pushpop.p90_ms | 150 | 150, 150, 133 |
| Push/pop with the Back key, p99 frame (ms) | 200 | 200, 250, 200 |
| Pop, first frame max (ms) | 26.99 | 26.99, 30.22, 14.45 |
| Pop, first frame median (ms) | 13.11 | 13.11, 13.32, 6.89 |
| Push, first frame max (ms) | 7.21 | 7.21, 8.71, 2.27 |
| Push, first frame median (ms) | 3.19 | 3.31, 3.19, 1.97 |
| First visit to a tab, first frame max (ms) | 31.9 | 48.64, 31.9, 29.9 |
| First visit to a tab, first frame median (ms) | 16.07 | 24.45, 8.86, 16.07 |
| Tab switch to a visited tab, first frame max (ms) | 45.24 | 33.64, 45.24, 60.1 |
| Tab switch to a visited tab, first frame median (ms) | 12.76 | 12.76, 9.85, 14.43 |
| tabs.frames | 249 | 249, 276, 245 |
| tabs.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| tabs.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| tabs.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Tab switch, janky frames (%) | 91.97 | 91.97, 86.59, 93.06 |
| tabs.p50_ms | 101 | 101, 85, 105 |
| tabs.p90_ms | 150 | 150, 150, 150 |
| tabs.p99_ms | 200 | 200, 200, 200 |
