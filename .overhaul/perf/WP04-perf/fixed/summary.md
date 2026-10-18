# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\fixed\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.2 °C at the start
- Runs: 3, seed 556109, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 385 | 385, 391, 380 |
| coldstart.runs_ms | [371, 379, 393, 382, 389, 377] | [407, 382, 392, 388, 381, 366], [395, 369, 401, 387, 402, 354], [371, 379, 393, 382, 389, 377] |
| Cold start as installed (profile from the APK only), median (ms) | 441 | 452, 431, 441 |
| coldstart_installed.runs_ms | [447, 467, 427, 455, 436, 416] | [427, 453, 411, 451, 483, 460], [426, 409, 424, 436, 465, 440], [447, 467, 427, 455, 436, 416] |
| First back swipe after pm clear (cold), janky frames | 3 | 2, 3, 3 |
| First back swipe after a restart (warm), janky frames | 3 | 3, 3, 1 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.78 | 6.61, 6.78, 8.14 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.56 | 6.82, 7.56, 15.58 |
| meminfo.graphics_mb | 55 | 54.9, 55, 55.5 |
| PSS after the journey, less the window's graphics buffers (MB) | 84.7 | 85.1, 82.2, 84.7 |
| PSS after the journey (MB) | 140.1 | 140.1, 137.2, 140.2 |
| pushpop.first3_dequeue_ms.max | 0.12 | 0.12, 0.12, 0.13 |
| pushpop.first3_dequeue_ms.median | 0.03 | 0.03, 0.03, 0.03 |
| pushpop.first3_gpu_ms.max | 20.66 | 19.41, 22.34, 20.66 |
| pushpop.first3_gpu_ms.median | 11.46 | 12.04, 11.46, 11.15 |
| pushpop.first3_issue_ms.max | 8.24 | 6.34, 8.24, 9.08 |
| pushpop.first3_issue_ms.median | 2.73 | 2.32, 2.73, 2.92 |
| pushpop.frames | 1023 | 1023, 1018, 1023 |
| pushpop.gpu_p50_ms | 7 | 7, 6, 7 |
| pushpop.gpu_p90_ms | 9 | 9, 9, 9 |
| pushpop.gpu_p99_ms | 15 | 15, 15, 15 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 1 | 0, 3.05, 1 |
| Push/pop with the Back key, janky frames (%) | 1.56 | 1.56, 1.57, 1.56 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 11 | 11, 11, 11 |
| pushpop.p90_ms | 17 | 17, 16, 18 |
| Push/pop with the Back key, p99 frame (ms) | 28 | 28, 30, 28 |
| Pop, first frame max (ms) | 6.09 | 6.14, 5.89, 6.09 |
| Pop, first frame median (ms) | 2.56 | 2.54, 2.56, 2.64 |
| Push, the frame that composes the screen, max (ms) | 16.72 | 15.93, 16.72, 16.74 |
| Push, the frame that composes the screen, median (ms) | 7.64 | 12.75, 7.28, 7.64 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.08 | 0.08, 0.1, 0.08 |
| pushpop_gesture.first3_dequeue_ms.median | 0.04 | 0.04, 0.04, 0.03 |
| pushpop_gesture.first3_gpu_ms.max | 19.86 | 19.86, 19.9, 18.25 |
| pushpop_gesture.first3_gpu_ms.median | 12.2 | 12.2, 11.36, 12.85 |
| pushpop_gesture.first3_issue_ms.max | 9.69 | 19.22, 9.69, 6.25 |
| pushpop_gesture.first3_issue_ms.median | 2.38 | 2.38, 3.52, 2.21 |
| pushpop_gesture.frames | 747 | 749, 744, 747 |
| pushpop_gesture.gesture_first_frame_ms.max | 4.91 | 4.91, 3.72, 5.62 |
| Back gesture, slowest of its first three frames, median (ms) | 3.14 | 3.14, 2.44, 3.8 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 20.43 | 20.43, 23.22, 19.15 |
| Push/pop with the back gesture, janky frames (%) | 2.55 | 2.27, 2.55, 2.68 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 11, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 18, 17 |
| pushpop_gesture.p99_ms | 28 | 28, 31, 27 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.06 | 0.06, 0.07, 0.06 |
| rapid_back.first3_dequeue_ms.median | 0.03 | 0.03, 0.03, 0.03 |
| rapid_back.first3_gpu_ms.max | 11.52 | 10.19, 11.52, 14.68 |
| rapid_back.first3_gpu_ms.median | 5.31 | 5.28, 6.03, 5.31 |
| rapid_back.first3_issue_ms.max | 8.69 | 7.78, 8.69, 9.56 |
| rapid_back.first3_issue_ms.median | 2.98 | 2.94, 2.98, 3.38 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 10 | 10, 13, 10 |
| Rapid Back, hitch time per second of motion (ms/s) | 12.28 | 12.28, 12.25, 15.39 |
| Rapid Back, janky frames (%) | 1.22 | 1.07, 1.53, 1.22 |
| rapid_back.late_first_frames | 4 | 3, 5, 4 |
| rapid_back.p50_ms | 11 | 11, 11, 11 |
| rapid_back.p90_ms | 13 | 13, 13, 13 |
| rapid_back.p99_ms | 17 | 17, 18, 17 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 6.36 | 6.31, 6.36, 8.66 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 6.02 | 6.02, 5.85, 6.06 |
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
| scroll_minimize.frames | 1357 | 1330, 1357, 1360 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 8, 7, 7 |
| Scroll with the bar minimizing, janky frames (%) | 0.96 | 0.98, 0.96, 0.96 |
| scroll_minimize.p50_ms | 9 | 9, 9, 9 |
| scroll_minimize.p90_ms | 10 | 10, 10, 10 |
| scroll_minimize.p99_ms | 14 | 16, 14, 14 |
| scroll_root.frames | 1357 | 1358, 1349, 1357 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 8 | 9, 7, 8 |
| Scroll a root list, janky frames (%) | 0.81 | 0.81, 1.11, 0.52 |
| scroll_root.p50_ms | 9 | 9, 9, 9 |
| scroll_root.p90_ms | 10 | 10, 10, 10 |
| scroll_root.p99_ms | 17 | 17, 13, 17 |
| tab1.first_frame_ms.max | 10.39 | 10.84, 10.39, 10.36 |
| tab1.first_frame_ms.median | 4.9 | 4.9, 6.79, 4.56 |
| First visit to a tab, first frame max (ms) | 10.91 | 9.23, 10.91, 11.73 |
| First visit to a tab, first frame median (ms) | 7.4 | 6.58, 7.4, 8.07 |
| First visit to a tab after a restart (warm), first frame max (ms) | 11.03 | 11.03, 11.86, 10.91 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.03 | 4.52, 6.03, 7.9 |
| tabs.first3_dequeue_ms.max | 0.08 | 0.13, 0.08, 0.07 |
| tabs.first3_dequeue_ms.median | 0.03 | 0.03, 0.03, 0.03 |
| tabs.first3_gpu_ms.max | 19.26 | 19.26, 20.87, 18.67 |
| tabs.first3_gpu_ms.median | 9.78 | 9.78, 10.23, 9.71 |
| tabs.first3_issue_ms.max | 5.7 | 7.04, 5.51, 5.7 |
| tabs.first3_issue_ms.median | 1.88 | 2.04, 1.65, 1.88 |
| Tab switch to a visited tab, first frame max (ms) | 10.94 | 10.94, 11.94, 10.67 |
| Tab switch to a visited tab, first frame median (ms) | 4.7 | 4.92, 4.41, 4.7 |
| tabs.frames | 1303 | 1303, 1303, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 7 | 7, 7, 7 |
| tabs.gpu_p99_ms | 13 | 13, 13, 13 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.15 | 1.07, 1.15, 1.15 |
| tabs.late_first_frames | 15 | 14, 15, 15 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 11, 11 |
| tabs.p99_ms | 20 | 21, 20, 19 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.08 | 0.25, 0.08, 0.08 |
| tabs_tap.first3_dequeue_ms.median | 0.03 | 0.03, 0.03, 0.03 |
| tabs_tap.first3_gpu_ms.max | 18.84 | 18.84, 17.73, 19.91 |
| tabs_tap.first3_gpu_ms.median | 9.88 | 9.36, 9.88, 10.49 |
| tabs_tap.first3_issue_ms.max | 6.28 | 6.79, 4.87, 6.28 |
| tabs_tap.first3_issue_ms.median | 1.93 | 1.79, 1.93, 2.06 |
| Tab switch with plain taps, first frame max (ms) | 10.93 | 10.93, 11.49, 10.64 |
| Tab switch with plain taps, first frame median (ms) | 4.87 | 4.87, 4.25, 5.05 |
| tabs_tap.frames | 1304 | 1304, 1305, 1304 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 7, 7, 8 |
| tabs_tap.gpu_p99_ms | 13 | 13, 13, 13 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs_tap.late_first_frames | 15 | 15, 15, 15 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 11, 11, 15 |
| tabs_tap.p99_ms | 20 | 20, 20, 22 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 7.4 | ratchet | report |
| First visit to a tab, first frame max (ms) | 10.91 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.03 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 11.03 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.78 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.56 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.15 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.7 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 10.94 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.87 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 10.93 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 7.64 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 16.72 | ratchet | report |
| Pop, first frame median (ms) | 2.56 | 8 | pass |
| Pop, first frame max (ms) | 6.09 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.56 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 28 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 1 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.55 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 3.14 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 20.43 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 1.22 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 6.02 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 6.36 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 12.28 | ratchet | report |
| Navigation stress: the app is still running | — | yes | not measured |
| Navigation stress: stack invariants hold | — | yes | not measured |
| Navigation stress, janky frames (%) | — | 4 | not measured |
| Process death: tab, stacks and presented sheet restored | — | yes | not measured |
| Process death: Back pops the restored stack | — | yes | not measured |
| Scroll a root list, janky frames (%) | 0.81 | 0.5 | fail |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.96 | 0.5 | fail |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | — | 0 | not measured |
| Cold start, speed-profile, median (ms) | 385 | 380 | fail |
| Cold start as installed (profile from the APK only), median (ms) | 441 | 400 | fail |
| PSS after the journey, less the window's graphics buffers (MB) | 84.7 | 48 | fail |
| PSS after the journey (MB) | 140.1 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
