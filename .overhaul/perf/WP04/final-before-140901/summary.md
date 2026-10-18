# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-everything\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 34.1 °C at the start
- Runs: 3, seed 315830, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, back_during_push, rapid_back, scroll_root, idle, nav_stress, restore, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| back_during_push.difference_ratios | [0.98, 0.96, 0.98, 0.96] | [0.98, 0.96, 0.98, 0.96], [0.98, 0.96, 0.98, 0.96], [0.98, 0.96, 0.98, 0.96] |
| Back swipe during a push: lands on the list | yes | yes, yes, yes |
| Back swipe during a push: one opaque card, no double exposure | yes | yes, yes, yes |
| Cold start, speed-profile, median (ms) | 357 | 360, 356, 357 |
| coldstart.runs_ms | [359, 364, 383, 346, 316, 356] | [346, 353, 349, 367, 379, 378], [355, 335, 358, 361, 313, 360], [359, 364, 383, 346, 316, 356] |
| Cold start as installed (profile from the APK only), median (ms) | 402 | 406, 397, 402 |
| coldstart_installed.runs_ms | [397, 398, 405, 438, 401, 403] | [408, 405, 405, 390, 408, 428], [396, 408, 391, 403, 399, 388], [397, 398, 405, 438, 401, 403] |
| First back swipe after pm clear (cold), janky frames | 2 | 1, 5, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.25 | 7.25, 5.82, 12.14 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.32 | 7.2, 7.32, 7.67 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0] | [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0] |
| meminfo.graphics_mb | 53.4 | 53.4, 53.3, 53.4 |
| PSS after the journey, less the window's graphics buffers (MB) | 53.7 | 53, 53.7, 55 |
| PSS after the journey (MB) | 107.1 | 106.3, 107.1, 108.4 |
| Navigation stress: the app is still running | yes | yes, yes, yes |
| nav_stress.exits | 15 | 13, 19, 15 |
| nav_stress.frames | 4042 | 4188, 2651, 4042 |
| nav_stress.gpu_p50_ms | 6 | 6, 6, 6 |
| nav_stress.gpu_p90_ms | 10 | 10, 10, 10 |
| nav_stress.gpu_p99_ms | 14 | 14, 15, 14 |
| Navigation stress: stack invariants hold | yes | yes, yes, yes |
| Navigation stress, janky frames (%) | 2.97 | 2.65, 5.32, 2.97 |
| nav_stress.p50_ms | 10 | 10, 10, 11 |
| nav_stress.p90_ms | 18 | 18, 21, 18 |
| nav_stress.p99_ms | 40 | 40, 48, 38 |
| nav_stress.states | 69 | 69, 11, 77 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.05, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 21.24 | 20.52, 21.24, 23.38 |
| pushpop.first3_gpu_ms.median | 10.84 | 10.91, 10.53, 10.84 |
| pushpop.first3_issue_ms.max | 6.69 | 6.73, 6.69, 5.86 |
| pushpop.first3_issue_ms.median | 1.96 | 1.96, 1.9, 2.25 |
| pushpop.frames | 1020 | 1021, 1019, 1020 |
| pushpop.gpu_p50_ms | 6 | 6, 7, 6 |
| pushpop.gpu_p90_ms | 9 | 9, 10, 9 |
| pushpop.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.02 | 4.04, 4.02, 3.06 |
| pushpop.janky_after_first_pct | 0.1 | 0.1, 0.1, 0.1 |
| Push/pop with the Back key, janky frames (%) | 1.67 | 1.67, 1.57, 1.67 |
| pushpop.late_first_frames | 16 | 16, 15, 16 |
| pushpop.p50_ms | 10 | 10, 11, 10 |
| pushpop.p90_ms | 16 | 16, 17, 16 |
| Push/pop with the Back key, p99 frame (ms) | 28 | 31, 28, 27 |
| Pop, first frame max (ms) | 5.15 | 5.15, 4.9, 5.35 |
| Pop, first frame median (ms) | 2.33 | 2.18, 2.37, 2.33 |
| Push, the frame that composes the screen, max (ms) | 15.54 | 16.08, 15.54, 12.35 |
| Push, the frame that composes the screen, median (ms) | 7.18 | 14.37, 7.18, 6.82 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.07 | 0.07, 0.06, 0.11 |
| pushpop_gesture.first3_dequeue_ms.median | 0.03 | 0.03, 0.03, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 20.14 | 18.11, 20.14, 20.87 |
| pushpop_gesture.first3_gpu_ms.median | 10.89 | 10.81, 12.16, 10.89 |
| pushpop_gesture.first3_issue_ms.max | 9.38 | 6.74, 9.38, 10.43 |
| pushpop_gesture.first3_issue_ms.median | 2.27 | 2.77, 2.27, 1.64 |
| pushpop_gesture.frames | 749 | 747, 749, 751 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.52 | 7.64, 3.16, 3.52 |
| Back gesture, slowest of its first three frames, median (ms) | 2.65 | 2.65, 2.24, 2.67 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 11, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 17.78 | 20.47, 17.78, 16.37 |
| pushpop_gesture.janky_after_first_pct | 0.8 | 1.07, 0.8, 0.8 |
| Push/pop with the back gesture, janky frames (%) | 2.4 | 2.68, 2.4, 2.4 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 16 |
| pushpop_gesture.p99_ms | 26 | 31, 26, 24 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.04 | 0.04, 0.04, 0.03 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 9.54 | 9.54, 9.78, 9.14 |
| rapid_back.first3_gpu_ms.median | 5.08 | 5.28, 5.03, 5.08 |
| rapid_back.first3_issue_ms.max | 6.98 | 7.17, 6.98, 5.93 |
| rapid_back.first3_issue_ms.median | 2.1 | 2.54, 2, 2.1 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 9 | 9, 9, 9 |
| Rapid Back, hitch time per second of motion (ms/s) | 12.23 | 12.23, 10.83, 15.29 |
| rapid_back.janky_after_first_pct | 0.76 | 0.92, 0.76, 0.61 |
| Rapid Back, janky frames (%) | 1.07 | 1.07, 1.07, 0.76 |
| rapid_back.late_first_frames | 1 | 1, 2, 1 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 12, 12, 12 |
| rapid_back.p99_ms | 14 | 14, 14, 14 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 6.06 | 10.27, 5.62, 6.06 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.49 | 5.38, 5.55, 5.49 |
| rapid_back.transitions | 6 | 6, 6, 6 |
| recompose.back_gesture.max | 4 | 4, 4, 4 |
| recompose.back_gesture.runs | [4, 4] | [4, 4], [4, 4], [4, 4] |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0, 0, 0 |
| Pop, frames that recompose (most in one pop) | 2 | 2, 2, 2 |
| recompose.pop.runs | [2, 2, 2] | [2, 2, 2], [2, 2, 2], [2, 2, 2] |
| Push, frames that recompose (most in one push) | 5 | 5, 5, 5 |
| recompose.push.runs | [5, 5, 5, 5, 5] | [5, 5, 5, 5, 5], [5, 5, 5, 5, 4], [5, 5, 5, 5, 5] |
| Tab switch, frames that recompose (most in one switch) | 1 | 1, 1, 1 |
| recompose.tab_switch.runs | [1, 1, 1, 1, 1] | [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1] |
| Process death: Back pops the restored stack | yes | yes, yes, yes |
| Process death: tab, stacks and presented sheet restored | yes | yes, yes, yes |
| scroll_minimize.frames | 1361 | 1361, 1361, 1364 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 7, 7, 7 |
| Scroll with the bar minimizing, janky frames (%) | 0.22 | 0.22, 0.22, 0.44 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 9 |
| scroll_minimize.p99_ms | 12 | 12, 12, 12 |
| scroll_root.frames | 1355 | 1359, 1327, 1355 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 7, 7 |
| Scroll a root list, janky frames (%) | 0.3 | 0.15, 0.3, 0.37 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 12, 12 |
| tab1.first_frame_ms.max | 9.19 | 8.5, 9.19, 9.21 |
| tab1.first_frame_ms.median | 4 | 4, 4.04, 3.92 |
| First visit to a tab, first frame max (ms) | 9.39 | 9.39, 9.98, 7.16 |
| First visit to a tab, first frame median (ms) | 3.99 | 6.72, 3.89, 3.99 |
| First visit to a tab after a restart (warm), first frame max (ms) | 7.88 | 4.34, 7.88, 8.51 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4 | 3.8, 5.52, 4 |
| tabs.first3_dequeue_ms.max | 0.07 | 0.07, 0.09, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.91 | 20.91, 20.89, 22.99 |
| tabs.first3_gpu_ms.median | 9.83 | 9.72, 9.91, 9.83 |
| tabs.first3_issue_ms.max | 4.82 | 4.82, 4.81, 5.67 |
| tabs.first3_issue_ms.median | 1.42 | 1.42, 1.4, 1.61 |
| Tab switch to a visited tab, first frame max (ms) | 9.95 | 11.94, 9.95, 9.24 |
| Tab switch to a visited tab, first frame median (ms) | 4.12 | 4.17, 4.12, 4.11 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 8 | 8, 7, 9 |
| tabs.gpu_p99_ms | 14 | 14, 13, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.07, 1.15, 1.07 |
| tabs.late_first_frames | 14 | 14, 15, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 11, 17 |
| tabs.p99_ms | 20 | 20, 20, 20 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 18.98 | 17.04, 23.61, 18.98 |
| tabs_tap.first3_gpu_ms.median | 9.66 | 10.74, 9.65, 9.66 |
| tabs_tap.first3_issue_ms.max | 5.04 | 5.34, 4.97, 5.04 |
| tabs_tap.first3_issue_ms.median | 1.42 | 1.42, 1.56, 1.35 |
| Tab switch with plain taps, first frame max (ms) | 10.03 | 9.04, 10.03, 10.29 |
| Tab switch with plain taps, first frame median (ms) | 4.27 | 4.02, 7.81, 4.27 |
| tabs_tap.frames | 1305 | 1305, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 8 | 8, 9, 8 |
| tabs_tap.gpu_p99_ms | 14 | 13, 14, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0.78, 0, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.15, 1.15, 1.07 |
| tabs_tap.late_first_frames | 15 | 15, 15, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 11, 15, 11 |
| tabs_tap.p99_ms | 20 | 19, 22, 20 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.99 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.39 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 7.88 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.25 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.32 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.12 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.95 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.27 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 10.03 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 7.18 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 15.54 | ratchet | report |
| Pop, first frame median (ms) | 2.33 | 8 | pass |
| Pop, first frame max (ms) | 5.15 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.67 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 28 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.02 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.4 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.65 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 17.78 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | yes | yes | pass |
| Back swipe during a push: lands on the list | yes | yes | pass |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 1.07 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.49 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 6.06 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 12.23 | ratchet | report |
| Navigation stress: the app is still running | yes | yes | pass |
| Navigation stress: stack invariants hold | yes | yes | pass |
| Navigation stress, janky frames (%) | 2.97 | 4 | pass |
| Process death: tab, stacks and presented sheet restored | yes | yes | pass |
| Process death: Back pops the restored stack | yes | yes | pass |
| Scroll a root list, janky frames (%) | 0.3 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.22 | 0.5 | pass |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 357 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 402 | 400 | fail |
| PSS after the journey, less the window's graphics buffers (MB) | 53.7 | 48 | fail |
| PSS after the journey (MB) | 107.1 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
