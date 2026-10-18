# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-everything\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 32.7 °C at the start
- Runs: 3, seed 620709, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, back_during_push, rapid_back, scroll_root, idle, nav_stress, restore, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| back_during_push.difference_ratios | [0.98, 0.97, 0.97, 0.96] | [0.98, 0.96, 0.98, 0.96], [0.98, 0.96, 0.98, 0.96], [0.98, 0.97, 0.97, 0.96] |
| Back swipe during a push: lands on the list | yes | yes, yes, yes |
| Back swipe during a push: one opaque card, no double exposure | yes | yes, yes, yes |
| Cold start, speed-profile, median (ms) | 357 | 357, 357, 439 |
| coldstart.runs_ms | [414, 482, 470, 394, 426, 453] | [338, 355, 327, 359, 423, 360], [358, 356, 365, 351, 385, 344], [414, 482, 470, 394, 426, 453] |
| Cold start as installed (profile from the APK only), median (ms) | 416 | 416, 407, 423 |
| coldstart_installed.runs_ms | [530, 390, 426, 423, 412, 424] | [399, 412, 448, 430, 421, 403], [401, 448, 421, 397, 413, 380], [530, 390, 426, 423, 412, 424] |
| First back swipe after pm clear (cold), janky frames | 2 | 3, 2, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 1, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.08 | 12.67, 7.08, 5.77 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.13 | 5.93, 8.47, 6.13 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0] | [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0] |
| meminfo.graphics_mb | 53.4 | 53.4, 53.4, 53.3 |
| PSS after the journey, less the window's graphics buffers (MB) | 55.1 | 55.1, 55.6, 54.8 |
| PSS after the journey (MB) | 108.5 | 108.5, 108.9, 108.1 |
| Navigation stress: the app is still running | yes | yes, yes, yes |
| nav_stress.exits | 14 | 14, 18, 14 |
| nav_stress.frames | 3347 | 4148, 3347, 3295 |
| nav_stress.gpu_p50_ms | 6 | 6, 6, 6 |
| nav_stress.gpu_p90_ms | 10 | 10, 10, 9 |
| nav_stress.gpu_p99_ms | 14 | 14, 16, 14 |
| Navigation stress: stack invariants hold | yes | yes, yes, yes |
| Navigation stress, janky frames (%) | 3 | 2.87, 4.06, 3 |
| nav_stress.p50_ms | 10 | 12, 9, 10 |
| nav_stress.p90_ms | 18 | 18, 19, 18 |
| nav_stress.p99_ms | 44 | 40, 46, 44 |
| nav_stress.states | 34 | 48, 28, 34 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.05, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 21.53 | 23, 20.25, 21.53 |
| pushpop.first3_gpu_ms.median | 11.06 | 11.06, 10.8, 12.29 |
| pushpop.first3_issue_ms.max | 6.88 | 5.92, 8.48, 6.88 |
| pushpop.first3_issue_ms.median | 2.51 | 2, 2.65, 2.51 |
| pushpop.frames | 1020 | 1021, 1017, 1020 |
| pushpop.gpu_p50_ms | 7 | 7, 6, 7 |
| pushpop.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop.gpu_p99_ms | 16 | 16, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.04 | 5.02, 4.04, 1.01 |
| pushpop.janky_after_first_pct | 0.1 | 0.1, 0.2, 0 |
| Push/pop with the Back key, janky frames (%) | 1.67 | 1.67, 1.77, 1.57 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 11 | 11, 10, 11 |
| pushpop.p90_ms | 17 | 17, 17, 18 |
| Push/pop with the Back key, p99 frame (ms) | 30 | 31, 30, 30 |
| Pop, first frame max (ms) | 5.23 | 5.23, 5.23, 5.32 |
| Pop, first frame median (ms) | 3.62 | 3.43, 3.67, 3.62 |
| Push, the frame that composes the screen, max (ms) | 16.2 | 16.2, 16.61, 15.71 |
| Push, the frame that composes the screen, median (ms) | 9.45 | 13.06, 9.45, 7.6 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 19.1 | 17.76, 19.1, 26.05 |
| pushpop_gesture.first3_gpu_ms.median | 11.7 | 11.7, 11.7, 10.83 |
| pushpop_gesture.first3_issue_ms.max | 8.07 | 8.49, 6.8, 8.07 |
| pushpop_gesture.first3_issue_ms.median | 1.74 | 1.62, 1.74, 2.02 |
| pushpop_gesture.frames | 748 | 751, 748, 746 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.69 | 3.69, 4.87, 3.32 |
| Back gesture, slowest of its first three frames, median (ms) | 2.18 | 2.18, 2.02, 2.4 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 7 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 15, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 19.1 | 16.31, 19.1, 24.5 |
| pushpop_gesture.janky_after_first_pct | 0.93 | 0.93, 0.8, 1.21 |
| Push/pop with the back gesture, janky frames (%) | 2.53 | 2.53, 2.41, 2.82 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 11 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 25 | 24, 25, 30 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.09 | 0.04, 0.22, 0.09 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 11.07 | 11.38, 9.96, 11.07 |
| rapid_back.first3_gpu_ms.median | 5.21 | 5.05, 5.21, 5.52 |
| rapid_back.first3_issue_ms.max | 6.29 | 6.23, 6.29, 6.67 |
| rapid_back.first3_issue_ms.median | 2.1 | 2.1, 2.08, 2.15 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 8 |
| rapid_back.gpu_p99_ms | 10 | 9, 10, 11 |
| Rapid Back, hitch time per second of motion (ms/s) | 10.71 | 9.24, 18.33, 10.71 |
| rapid_back.janky_after_first_pct | 0.76 | 0.46, 0.92, 0.76 |
| Rapid Back, janky frames (%) | 0.92 | 0.76, 0.92, 1.07 |
| rapid_back.late_first_frames | 2 | 2, 0, 2 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 12, 12, 13 |
| rapid_back.p99_ms | 15 | 15, 15, 17 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.67 | 5.67, 5.57, 7.14 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.53 | 5.53, 5.46, 5.57 |
| rapid_back.transitions | 6 | 6, 6, 6 |
| recompose.back_gesture.max | 4 | 4, 4, 4 |
| recompose.back_gesture.runs | [4, 4] | [4, 4], [4, 4], [4, 4] |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0, 0, 0 |
| Pop, frames that recompose (most in one pop) | 2 | 2, 2, 2 |
| recompose.pop.runs | [2, 2, 2] | [2, 2, 2], [2, 2, 2], [2, 2, 2] |
| Push, frames that recompose (most in one push) | 5 | 5, 5, 5 |
| recompose.push.runs | [5, 5, 5, 5, 5] | [5, 5, 5, 5, 5], [5, 5, 5, 5, 5], [5, 5, 5, 5, 5] |
| Tab switch, frames that recompose (most in one switch) | 1 | 1, 1, 1 |
| recompose.tab_switch.runs | [1, 1, 1, 1, 1] | [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1] |
| Process death: Back pops the restored stack | yes | yes, yes, yes |
| Process death: tab, stacks and presented sheet restored | yes | yes, yes, yes |
| scroll_minimize.frames | 1361 | 1361, 1362, 1349 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 7, 7, 7 |
| Scroll with the bar minimizing, janky frames (%) | 0.3 | 0.29, 0.44, 0.3 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 10 |
| scroll_minimize.p99_ms | 12 | 12, 12, 12 |
| scroll_root.frames | 1359 | 1358, 1363, 1359 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 8 | 9, 7, 8 |
| Scroll a root list, janky frames (%) | 0.15 | 0.15, 0.07, 0.29 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 15 | 16, 12, 15 |
| tab1.first_frame_ms.max | 9.28 | 9.28, 9.59, 8.98 |
| tab1.first_frame_ms.median | 3.95 | 3.91, 3.95, 3.95 |
| First visit to a tab, first frame max (ms) | 8.44 | 8.44, 3.98, 9.29 |
| First visit to a tab, first frame median (ms) | 3.63 | 3.63, 3.58, 5.53 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.52 | 9.98, 8.72, 9.52 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.74 | 8.97, 5.74, 4.22 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 19.51 | 19.77, 18.68, 19.51 |
| tabs.first3_gpu_ms.median | 10.72 | 10.72, 9.97, 10.82 |
| tabs.first3_issue_ms.max | 4.58 | 5.19, 4.58, 4.34 |
| tabs.first3_issue_ms.median | 1.35 | 1.3, 1.35, 1.36 |
| Tab switch to a visited tab, first frame max (ms) | 9.94 | 9.76, 9.94, 10.75 |
| Tab switch to a visited tab, first frame median (ms) | 4.23 | 6.89, 4, 4.23 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 6 |
| tabs.gpu_p90_ms | 10 | 10, 7, 10 |
| tabs.gpu_p99_ms | 13 | 13, 13, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 1.57 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.07, 1.07, 1.15 |
| tabs.late_first_frames | 14 | 14, 14, 15 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 16 | 17, 10, 16 |
| tabs.p99_ms | 20 | 20, 19, 21 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 19.81 | 21.28, 19.81, 18.78 |
| tabs_tap.first3_gpu_ms.median | 9.67 | 9.67, 9.78, 9.13 |
| tabs_tap.first3_issue_ms.max | 4.45 | 4.32, 4.61, 4.45 |
| tabs_tap.first3_issue_ms.median | 1.39 | 1.39, 1.36, 1.48 |
| Tab switch with plain taps, first frame max (ms) | 9.62 | 9.55, 10.55, 9.62 |
| Tab switch with plain taps, first frame median (ms) | 4.02 | 4.08, 4.02, 3.5 |
| tabs_tap.frames | 1304 | 1303, 1305, 1304 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 10, 7, 7 |
| tabs_tap.gpu_p99_ms | 13 | 13, 14, 13 |
| tabs_tap.hitch_ms_per_s | 0.78 | 0.78, 0, 0.78 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.15, 1.15, 1.07 |
| tabs_tap.late_first_frames | 15 | 15, 15, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 10 | 16, 10, 10 |
| tabs_tap.p99_ms | 19 | 21, 19, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.63 | ratchet | report |
| First visit to a tab, first frame max (ms) | 8.44 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.74 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.52 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.08 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.13 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.23 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.94 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.02 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.62 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 9.45 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 16.2 | ratchet | report |
| Pop, first frame median (ms) | 3.62 | 8 | pass |
| Pop, first frame max (ms) | 5.23 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.67 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 30 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.04 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.53 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.18 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 19.1 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | yes | yes | pass |
| Back swipe during a push: lands on the list | yes | yes | pass |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 0.92 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.53 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.67 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 10.71 | ratchet | report |
| Navigation stress: the app is still running | yes | yes | pass |
| Navigation stress: stack invariants hold | yes | yes | pass |
| Navigation stress, janky frames (%) | 3 | 4 | pass |
| Process death: tab, stacks and presented sheet restored | yes | yes | pass |
| Process death: Back pops the restored stack | yes | yes | pass |
| Scroll a root list, janky frames (%) | 0.15 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.3 | 0.5 | pass |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 357 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 416 | 400 | fail |
| PSS after the journey, less the window's graphics buffers (MB) | 55.1 | 48 | fail |
| PSS after the journey (MB) | 108.5 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
