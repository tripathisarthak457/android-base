# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-everything\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 32.8 °C at the start
- Runs: 3, seed 948127, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, back_during_push, rapid_back, scroll_root, idle, nav_stress, restore, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| back_during_push.difference_ratios | [0.98, 0.96, 0.98, 0.97] | [0.98, 0.96, 0.99, 0.96], [0.98, 0.96, 0.98, 0.96], [0.98, 0.96, 0.98, 0.97] |
| Back swipe during a push: lands on the list | yes | yes, yes, yes |
| Back swipe during a push: one opaque card, no double exposure | yes | yes, yes, yes |
| Cold start, speed-profile, median (ms) | 358 | 366, 352, 358 |
| coldstart.runs_ms | [356, 362, 389, 347, 347, 360] | [387, 360, 345, 373, 312, 380], [334, 371, 358, 352, 352, 344], [356, 362, 389, 347, 347, 360] |
| Cold start as installed (profile from the APK only), median (ms) | 410 | 391, 415, 410 |
| coldstart_installed.runs_ms | [412, 409, 415, 407, 448, 408] | [477, 393, 427, 383, 389, 375], [395, 409, 433, 422, 423, 384], [412, 409, 415, 407, 448, 408] |
| First back swipe after pm clear (cold), janky frames | 3 | 3, 3, 2 |
| First back swipe after a restart (warm), janky frames | 3 | 4, 3, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.97 | 7.32, 5.97, 5.73 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.26 | 16.05, 6.14, 6.26 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0] | [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0] |
| meminfo.graphics_mb | 53.3 | 53.3, 53.3, 53.4 |
| PSS after the journey, less the window's graphics buffers (MB) | 54.9 | 54.1, 54.9, 55.5 |
| PSS after the journey (MB) | 108.2 | 107.4, 108.2, 108.8 |
| Navigation stress: the app is still running | yes | yes, yes, yes |
| nav_stress.exits | 16 | 16, 16, 15 |
| nav_stress.frames | 3748 | 3419, 3748, 3902 |
| nav_stress.gpu_p50_ms | 6 | 6, 6, 6 |
| nav_stress.gpu_p90_ms | 9 | 9, 9, 10 |
| nav_stress.gpu_p99_ms | 14 | 14, 15, 14 |
| Navigation stress: stack invariants hold | yes | yes, yes, yes |
| Navigation stress, janky frames (%) | 3.89 | 3.89, 3.92, 3.15 |
| nav_stress.p50_ms | 10 | 10, 9, 10 |
| nav_stress.p90_ms | 18 | 18, 18, 18 |
| nav_stress.p99_ms | 40 | 40, 46, 40 |
| nav_stress.states | 60 | 21, 60, 63 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.22, 0.05 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.81 | 20.81, 18.92, 21.66 |
| pushpop.first3_gpu_ms.median | 11.02 | 11.02, 12.77, 10.91 |
| pushpop.first3_issue_ms.max | 6.49 | 7.6, 5.82, 6.49 |
| pushpop.first3_issue_ms.median | 2.17 | 1.99, 2.21, 2.17 |
| pushpop.frames | 1020 | 1022, 1019, 1020 |
| pushpop.gpu_p50_ms | 6 | 7, 6, 6 |
| pushpop.gpu_p90_ms | 9 | 10, 9, 9 |
| pushpop.gpu_p99_ms | 16 | 16, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2.03 | 2.02, 2.04, 2.03 |
| pushpop.janky_after_first_pct | 0 | 0, 0, 0.2 |
| Push/pop with the Back key, janky frames (%) | 1.57 | 1.57, 1.57, 1.76 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 10 | 11, 10, 10 |
| pushpop.p90_ms | 16 | 17, 16, 16 |
| Push/pop with the Back key, p99 frame (ms) | 29 | 27, 29, 30 |
| Pop, first frame max (ms) | 5.11 | 5.11, 5.11, 4.88 |
| Pop, first frame median (ms) | 2.35 | 2.21, 2.38, 2.35 |
| Push, the frame that composes the screen, max (ms) | 16.27 | 16.27, 15.63, 16.31 |
| Push, the frame that composes the screen, median (ms) | 7.35 | 7.35, 7.35, 10.98 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.05 | 0.05, 0.08, 0.05 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 18.47 | 18.47, 20.31, 17.94 |
| pushpop_gesture.first3_gpu_ms.median | 11.29 | 11.89, 11.08, 11.29 |
| pushpop_gesture.first3_issue_ms.max | 8.69 | 8.69, 6.93, 8.87 |
| pushpop_gesture.first3_issue_ms.median | 1.91 | 1.91, 1.95, 1.9 |
| pushpop_gesture.frames | 751 | 741, 751, 752 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.6 | 3.37, 5.4, 3.6 |
| Back gesture, slowest of its first three frames, median (ms) | 2.27 | 2.22, 2.27, 3.22 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 11, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 17.75 | 27.36, 17.75, 13.66 |
| pushpop_gesture.janky_after_first_pct | 0.8 | 1.62, 0.8, 0.66 |
| Push/pop with the back gesture, janky frames (%) | 2.4 | 3.24, 2.4, 2.26 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 9 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 16 |
| pushpop_gesture.p99_ms | 25 | 28, 24, 25 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.05 | 0.05, 0.4, 0.05 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 10.41 | 11.06, 10.41, 9.15 |
| rapid_back.first3_gpu_ms.median | 5.61 | 5.61, 8.94, 5.2 |
| rapid_back.first3_issue_ms.max | 7.77 | 7.77, 8.55, 7.37 |
| rapid_back.first3_issue_ms.median | 2.5 | 2.51, 2.04, 2.5 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 9 | 9, 10, 9 |
| Rapid Back, hitch time per second of motion (ms/s) | 10.74 | 10.74, 7.69, 13.8 |
| rapid_back.janky_after_first_pct | 0.76 | 0.31, 0.76, 0.76 |
| Rapid Back, janky frames (%) | 0.92 | 0.61, 1.38, 0.92 |
| rapid_back.late_first_frames | 2 | 2, 4, 1 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 12, 12, 12 |
| rapid_back.p99_ms | 14 | 14, 15, 14 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.79 | 5.79, 10.77, 5.64 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.57 | 5.57, 5.63, 5.47 |
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
| scroll_minimize.frames | 1358 | 1329, 1358, 1363 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 10, 7, 7 |
| Scroll with the bar minimizing, janky frames (%) | 0.22 | 0.45, 0.22, 0.22 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 9 |
| scroll_minimize.p99_ms | 12 | 17, 12, 12 |
| scroll_root.frames | 1360 | 1360, 1358, 1360 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 9, 7 |
| Scroll a root list, janky frames (%) | 0.29 | 0.22, 0.29, 0.66 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 17, 12 |
| tab1.first_frame_ms.max | 9.6 | 9.12, 9.62, 9.6 |
| tab1.first_frame_ms.median | 3.92 | 3.46, 6.11, 3.92 |
| First visit to a tab, first frame max (ms) | 9.27 | 9.93, 9.27, 9.21 |
| First visit to a tab, first frame median (ms) | 4.04 | 4.04, 3.76, 4.08 |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.01 | 8.75, 10.18, 10.01 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.12 | 4.08, 6.12, 7.15 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.18, 0.05, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 19.26 | 18.34, 19.26, 19.75 |
| tabs.first3_gpu_ms.median | 9.49 | 9.43, 9.49, 10.88 |
| tabs.first3_issue_ms.max | 4.92 | 6.27, 4.92, 4.66 |
| tabs.first3_issue_ms.median | 1.33 | 1.4, 1.33, 1.3 |
| Tab switch to a visited tab, first frame max (ms) | 8.69 | 8.69, 10.01, 8.27 |
| Tab switch to a visited tab, first frame median (ms) | 3.86 | 3.59, 3.86, 4.05 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 7 | 7, 7, 7 |
| tabs.gpu_p99_ms | 13 | 13, 13, 13 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1, 1.07, 1.07 |
| tabs.late_first_frames | 14 | 13, 14, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 10, 11 |
| tabs.p99_ms | 19 | 19, 19, 19 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.1 | 0.06, 0.11, 0.1 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 20.15 | 21.03, 19, 20.15 |
| tabs_tap.first3_gpu_ms.median | 9.97 | 10.8, 9.76, 9.97 |
| tabs_tap.first3_issue_ms.max | 4.94 | 5.15, 4.94, 4.83 |
| tabs_tap.first3_issue_ms.median | 1.58 | 1.43, 1.58, 1.62 |
| Tab switch with plain taps, first frame max (ms) | 9.87 | 10.04, 9.56, 9.87 |
| Tab switch with plain taps, first frame median (ms) | 4.08 | 4.08, 4.09, 3.94 |
| tabs_tap.frames | 1304 | 1304, 1304, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 8 | 8, 7, 8 |
| tabs_tap.gpu_p99_ms | 14 | 14, 13, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.07, 1.15, 1.15 |
| tabs_tap.late_first_frames | 15 | 14, 15, 15 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 11, 10, 11 |
| tabs_tap.p99_ms | 20 | 20, 21, 20 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.04 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.27 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.12 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.01 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.97 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.26 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 3.86 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 8.69 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.08 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.87 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 7.35 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 16.27 | ratchet | report |
| Pop, first frame median (ms) | 2.35 | 8 | pass |
| Pop, first frame max (ms) | 5.11 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.57 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 29 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2.03 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.4 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.27 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 17.75 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | yes | yes | pass |
| Back swipe during a push: lands on the list | yes | yes | pass |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 0.92 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.57 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.79 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 10.74 | ratchet | report |
| Navigation stress: the app is still running | yes | yes | pass |
| Navigation stress: stack invariants hold | yes | yes | pass |
| Navigation stress, janky frames (%) | 3.89 | 4 | pass |
| Process death: tab, stacks and presented sheet restored | yes | yes | pass |
| Process death: Back pops the restored stack | yes | yes | pass |
| Scroll a root list, janky frames (%) | 0.29 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.22 | 0.5 | pass |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 358 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 410 | 400 | fail |
| PSS after the journey, less the window's graphics buffers (MB) | 54.9 | 48 | fail |
| PSS after the journey (MB) | 108.2 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
