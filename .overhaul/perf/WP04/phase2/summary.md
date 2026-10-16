# perf.sh: com.example.wp04.p2.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/p2/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 36.0 °C at the start
- Runs: 3, seed 216279, extras {"perf.fakeSession": "true"}
- Scenarios: tabs, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, idle, nav_stress, restore, meminfo, scroll_minimize, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 355 | 346, 357, 355 |
| coldstart.runs_ms | [366, 321, 315, 346, 364, 373] | [326, 338, 339, 353, 389, 378], [354, 367, 401, 330, 325, 361], [366, 321, 315, 346, 364, 373] |
| Cold start as installed (profile from the APK only), median (ms) | 416 | 408, 421, 416 |
| coldstart_installed.runs_ms | [443, 404, 412, 441, 416, 416] | [420, 429, 402, 393, 400, 415], [458, 425, 421, 407, 421, 417], [443, 404, 412, 441, 416, 416] |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0] | [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0] |
| PSS after the journey (MB) | 93.8 | 93.6, 93.8, 94 |
| Navigation stress: the app is still running | yes | yes, yes, yes |
| nav_stress.exits | 18 | 15, 19, 18 |
| nav_stress.frames | 2490 | 3181, 2132, 2490 |
| nav_stress.gpu_p50_ms | 6 | 6, 6, 6 |
| nav_stress.gpu_p90_ms | 10 | 8, 11, 10 |
| nav_stress.gpu_p99_ms | 19 | 16, 22, 19 |
| Navigation stress: stack invariants hold | yes | yes, yes, yes |
| Navigation stress, janky frames (%) | 5.26 | 3.36, 5.82, 5.26 |
| nav_stress.p50_ms | 9 | 9, 10, 9 |
| nav_stress.p90_ms | 21 | 17, 22, 21 |
| nav_stress.p99_ms | 53 | 46, 57, 53 |
| nav_stress.states | 56 | 59, 56, 46 |
| pushpop.frames | 689 | 684, 694, 689 |
| pushpop.gpu_p50_ms | 10 | 7, 10, 10 |
| pushpop.gpu_p90_ms | 12 | 12, 12, 12 |
| pushpop.gpu_p99_ms | 18 | 18, 19, 18 |
| Push/pop with the Back key, janky frames (%) | 2.34 | 2.34, 2.45, 2.32 |
| pushpop.p50_ms | 16 | 10, 16, 16 |
| pushpop.p90_ms | 19 | 19, 19, 19 |
| Push/pop with the Back key, p99 frame (ms) | 57 | 57, 57, 61 |
| Pop, first frame max (ms) | 36.93 | 37.51, 36.93, 32.73 |
| Pop, first frame median (ms) | 15.01 | 33.84, 14.86, 15.01 |
| Push, the frame that composes the screen, max (ms) | 48.61 | 42.59, 50.52, 48.61 |
| Push, the frame that composes the screen, median (ms) | 19.95 | 19.09, 19.95, 32.27 |
| pushpop_gesture.frames | 828 | 828, 828, 817 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 11, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 17 | 18, 17, 17 |
| Push/pop with the back gesture, janky frames (%) | 2.82 | 2.66, 3.14, 2.82 |
| pushpop_gesture.p50_ms | 9 | 9, 9, 9 |
| pushpop_gesture.p90_ms | 18 | 18, 18, 18 |
| pushpop_gesture.p99_ms | 42 | 42, 40, 48 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.frames | 453 | 447, 458, 453 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 8 |
| rapid_back.gpu_p99_ms | 10 | 8, 10, 10 |
| Rapid Back, janky frames (%) | 2.62 | 2.68, 2.62, 2.43 |
| rapid_back.p50_ms | 9 | 9, 9, 9 |
| rapid_back.p90_ms | 11 | 11, 11, 16 |
| rapid_back.p99_ms | 32 | 32, 34, 32 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 34.56 | 36.76, 34.56, 24.61 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 23.63 | 23.83, 23.63, 23.59 |
| Process death: Back pops the restored stack | yes | yes, yes, yes |
| Process death: tab, stacks and presented sheet restored | yes | yes, yes, yes |
| scroll_minimize.frames | 1355 | 1357, 1355, 1352 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 8, 7, 7 |
| Scroll with the bar minimizing, janky frames (%) | 0.3 | 0.29, 0.3, 0.44 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 9 |
| scroll_minimize.p99_ms | 13 | 13, 11, 13 |
| scroll_root.frames | 1355 | 1362, 1331, 1355 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 9 | 9, 9, 10 |
| Scroll a root list, janky frames (%) | 0.15 | 0.22, 0.15, 0.15 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 16 | 16, 16, 17 |
| tab1.first_frame_ms.max | 16.01 | 7.81, 16.01, 16.44 |
| tab1.first_frame_ms.median | 6.63 | 6.63, 6.49, 7.37 |
| First visit to a tab, first frame max (ms) | 27.55 | 27.55, 30.03, 26.31 |
| First visit to a tab, first frame median (ms) | 22.68 | 22.68, 24.82, 19.46 |
| Tab switch to a visited tab, first frame max (ms) | 16.45 | 16.45, 16.97, 14.54 |
| Tab switch to a visited tab, first frame median (ms) | 6.51 | 7.41, 6.51, 6.42 |
| tabs.frames | 789 | 788, 789, 789 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 8 | 8, 8, 7 |
| tabs.gpu_p99_ms | 17 | 18, 17, 17 |
| Tab switch, janky frames (%) | 1.9 | 1.9, 1.9, 1.9 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 10 | 10, 11, 9 |
| tabs.p99_ms | 27 | 29, 24, 27 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 22.68 | ratchet | report |
| First visit to a tab, first frame max (ms) | 27.55 | ratchet | report |
| Tab switch, janky frames (%) | 1.9 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 6.51 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 16.45 | 16 | fail |
| Push, the frame that composes the screen, median (ms) | 19.95 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 48.61 | ratchet | report |
| Pop, first frame median (ms) | 15.01 | 8 | fail |
| Pop, first frame max (ms) | 36.93 | 16 | fail |
| Push/pop with the Back key, janky frames (%) | 2.34 | 2 | fail |
| Push/pop with the Back key, p99 frame (ms) | 57 | 25 | fail |
| Push/pop with the back gesture, janky frames (%) | 2.82 | 2 | fail |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 2.62 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 23.63 | 8 | fail |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 34.56 | 16 | fail |
| Navigation stress: the app is still running | yes | yes | pass |
| Navigation stress: stack invariants hold | yes | yes | pass |
| Navigation stress, janky frames (%) | 5.26 | 4 | fail |
| Process death: tab, stacks and presented sheet restored | yes | yes | pass |
| Process death: Back pops the restored stack | yes | yes | pass |
| Scroll a root list, janky frames (%) | 0.15 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.3 | 0.5 | pass |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 355 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 416 | 400 | fail |
| PSS after the journey (MB) | 93.8 | 70 | fail |
