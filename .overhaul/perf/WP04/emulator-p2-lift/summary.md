# perf.sh: com.example.wp04.p2.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/p2/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 3, seed 966398, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop

| Metric | Median | Runs |
|---|---|---|
| pushpop.frames | 224 | 182, 286, 224 |
| pushpop.gpu_p50_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| pushpop.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Push/pop with the Back key, janky frames (%) | 78.12 | 90.66, 57.69, 78.12 |
| pushpop.p50_ms | 69 | 93, 48, 69 |
| pushpop.p90_ms | 101 | 125, 65, 101 |
| Push/pop with the Back key, p99 frame (ms) | 117 | 200, 81, 117 |
| Pop, first frame max (ms) | 10.41 | 10.41, 4.98, 14.28 |
| Pop, first frame median (ms) | 6.45 | 7.36, 4.23, 6.45 |
| Push, first frame max (ms) | 11.96 | 15.49, 8.23, 11.96 |
| Push, first frame median (ms) | 8.68 | 11.85, 5.88, 8.68 |
| First visit to a tab, first frame max (ms) | 13.99 | 20.57, 13.99, 12.58 |
| First visit to a tab, first frame median (ms) | 9.02 | 12.45, 9.02, 6.1 |
| Tab switch to a visited tab, first frame max (ms) | 3.02 | 15.19, 2.99, 3.02 |
| Tab switch to a visited tab, first frame median (ms) | 1.92 | 2.75, 1.92, 1.77 |
| tabs.frames | 447 | 294, 447, 476 |
| tabs.gpu_p50_ms | 4950 | 4950, 4950, 24 |
| tabs.gpu_p90_ms | 4950 | 4950, 4950, 4950 |
| tabs.gpu_p99_ms | 4950 | 4950, 4950, 4950 |
| Tab switch, janky frames (%) | 42.95 | 76.87, 42.95, 36.13 |
| tabs.p50_ms | 40 | 69, 40, 32 |
| tabs.p90_ms | 65 | 109, 65, 61 |
| tabs.p99_ms | 109 | 150, 89, 109 |
