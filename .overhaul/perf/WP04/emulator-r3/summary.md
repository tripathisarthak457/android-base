# perf.sh: com.example.wp04.p2.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/p2/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 1, seed 730890, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, pushpop, pushpop_gesture, rapid_back, idle, nav_stress, restore

| Metric | Median | Runs |
|---|---|---|
| Idle 5 s on every tab, frames rendered | 0 | 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0, 0, 0] | [0, 0, 0, 0, 0, 0, 0] |
| Navigation stress: the app is still running | yes | yes |
| nav_stress.exits | 5 | 5 |
| nav_stress.frames | 130 | 130 |
| nav_stress.gpu_p50_ms | 4950 | 4950 |
| nav_stress.gpu_p90_ms | 4950 | 4950 |
| nav_stress.gpu_p99_ms | 4950 | 4950 |
| Navigation stress: stack invariants hold | yes | yes |
| Navigation stress, janky frames (%) | 100 | 100 |
| nav_stress.p50_ms | 200 | 200 |
| nav_stress.p90_ms | 850 | 850 |
| nav_stress.p99_ms | 2900 | 2900 |
| nav_stress.states | 23 | 23 |
| pushpop.frames | 157 | 157 |
| pushpop.gpu_p50_ms | 4950 | 4950 |
| pushpop.gpu_p90_ms | 4950 | 4950 |
| pushpop.gpu_p99_ms | 4950 | 4950 |
| Push/pop with the Back key, janky frames (%) | 91.08 | 91.08 |
| pushpop.p50_ms | 105 | 105 |
| pushpop.p90_ms | 150 | 150 |
| Push/pop with the Back key, p99 frame (ms) | 350 | 350 |
| Pop, first frame max (ms) | 13.99 | 13.99 |
| Pop, first frame median (ms) | 9.4 | 9.4 |
| Push, first frame max (ms) | 35.88 | 35.88 |
| Push, first frame median (ms) | 13.72 | 13.72 |
| pushpop_gesture.frames | 184 | 184 |
| pushpop_gesture.gpu_p50_ms | 4950 | 4950 |
| pushpop_gesture.gpu_p90_ms | 4950 | 4950 |
| pushpop_gesture.gpu_p99_ms | 4950 | 4950 |
| Push/pop with the back gesture, janky frames (%) | 94.57 | 94.57 |
| pushpop_gesture.p50_ms | 125 | 125 |
| pushpop_gesture.p90_ms | 150 | 150 |
| pushpop_gesture.p99_ms | 250 | 250 |
| Rapid Back: the app is still running | yes | yes |
| rapid_back.depth | 2 | 2 |
| Rapid Back: lands on the expected screen | yes | yes |
| rapid_back.frames | 73 | 73 |
| rapid_back.gpu_p50_ms | 4950 | 4950 |
| rapid_back.gpu_p90_ms | 4950 | 4950 |
| rapid_back.gpu_p99_ms | 4950 | 4950 |
| Rapid Back, janky frames (%) | 87.67 | 87.67 |
| rapid_back.p50_ms | 81 | 81 |
| rapid_back.p90_ms | 125 | 125 |
| rapid_back.p99_ms | 150 | 150 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 9.37 | 9.37 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 8.53 | 8.53 |
| Process death: Back pops the restored stack | yes | yes |
| Process death: tab, stacks and presented sheet restored | yes | yes |
| First visit to a tab, first frame max (ms) | 15.44 | 15.44 |
| First visit to a tab, first frame median (ms) | 12.4 | 12.4 |
| Tab switch to a visited tab, first frame max (ms) | 6.27 | 6.27 |
| Tab switch to a visited tab, first frame median (ms) | 2.62 | 2.62 |
| tabs.frames | 329 | 329 |
| tabs.gpu_p50_ms | 4950 | 4950 |
| tabs.gpu_p90_ms | 4950 | 4950 |
| tabs.gpu_p99_ms | 4950 | 4950 |
| Tab switch, janky frames (%) | 75.08 | 75.08 |
| tabs.p50_ms | 61 | 61 |
| tabs.p90_ms | 97 | 97 |
| tabs.p99_ms | 125 | 125 |
