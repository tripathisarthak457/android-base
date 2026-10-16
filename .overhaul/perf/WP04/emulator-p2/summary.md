# perf.sh: com.example.wp04.p2.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/p2/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 1, seed 5339, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop, pushpop_gesture, rapid_back, idle, nav_stress, restore

| Metric | Median | Runs |
|---|---|---|
| Idle 5 s on every tab, frames rendered | 0 | 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0, 0, 0] | [0, 0, 0, 0, 0, 0, 0] |
| Navigation stress: the app is still running | yes | yes |
| nav_stress.exits | 10 | 10 |
| nav_stress.frames | 687 | 687 |
| nav_stress.gpu_p50_ms | 4950 | 4950 |
| nav_stress.gpu_p90_ms | 4950 | 4950 |
| nav_stress.gpu_p99_ms | 4950 | 4950 |
| Navigation stress: stack invariants hold | yes | yes |
| Navigation stress, janky frames (%) | 60.26 | 60.26 |
| nav_stress.p50_ms | 57 | 57 |
| nav_stress.p90_ms | 117 | 117 |
| nav_stress.p99_ms | 950 | 950 |
| nav_stress.states | 77 | 77 |
| pushpop.frames | 237 | 237 |
| pushpop.gpu_p50_ms | 4950 | 4950 |
| pushpop.gpu_p90_ms | 4950 | 4950 |
| pushpop.gpu_p99_ms | 4950 | 4950 |
| Push/pop with the Back key, janky frames (%) | 67.09 | 67.09 |
| pushpop.p50_ms | 57 | 57 |
| pushpop.p90_ms | 85 | 85 |
| Push/pop with the Back key, p99 frame (ms) | 150 | 150 |
| Pop, first frame max (ms) | 13.59 | 13.59 |
| Pop, first frame median (ms) | 4.92 | 4.92 |
| Push, first frame max (ms) | 11.64 | 11.64 |
| Push, first frame median (ms) | 7.3 | 7.3 |
| pushpop_gesture.frames | 206 | 206 |
| pushpop_gesture.gpu_p50_ms | 4950 | 4950 |
| pushpop_gesture.gpu_p90_ms | 4950 | 4950 |
| pushpop_gesture.gpu_p99_ms | 4950 | 4950 |
| Push/pop with the back gesture, janky frames (%) | 96.12 | 96.12 |
| pushpop_gesture.p50_ms | 105 | 105 |
| pushpop_gesture.p90_ms | 150 | 150 |
| pushpop_gesture.p99_ms | 200 | 200 |
| Rapid Back: the app is still running | yes | yes |
| rapid_back.depth | 2 | 2 |
| Rapid Back: lands on the expected screen | yes | yes |
| rapid_back.frames | 91 | 91 |
| rapid_back.gpu_p50_ms | 4950 | 4950 |
| rapid_back.gpu_p90_ms | 4950 | 4950 |
| rapid_back.gpu_p99_ms | 4950 | 4950 |
| Rapid Back, janky frames (%) | 70.33 | 70.33 |
| rapid_back.p50_ms | 53 | 53 |
| rapid_back.p90_ms | 81 | 81 |
| rapid_back.p99_ms | 150 | 150 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 21.46 | 21.46 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 6.16 | 6.16 |
| Process death: Back pops the restored stack | no | no |
| Process death: tab, stacks and presented sheet restored | yes | yes |
| First visit to a tab, first frame max (ms) | 12.34 | 12.34 |
| First visit to a tab, first frame median (ms) | 7.46 | 7.46 |
| Tab switch to a visited tab, first frame max (ms) | 2.22 | 2.22 |
| Tab switch to a visited tab, first frame median (ms) | 1.62 | 1.62 |
| tabs.frames | 469 | 469 |
| tabs.gpu_p50_ms | 4950 | 4950 |
| tabs.gpu_p90_ms | 4950 | 4950 |
| tabs.gpu_p99_ms | 4950 | 4950 |
| Tab switch, janky frames (%) | 38.59 | 38.59 |
| tabs.p50_ms | 36 | 36 |
| tabs.p90_ms | 61 | 61 |
| tabs.p99_ms | 77 | 77 |
