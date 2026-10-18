# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\everything\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 31.7 °C at the start
- Runs: 3, seed 377492, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 354 | 354, 348, 397 |
| coldstart.runs_ms | [442, 407, 424, 349, 377, 387] | [361, 370, 348, 371, 346, 333], [343, 408, 341, 386, 353, 327], [442, 407, 424, 349, 377, 387] |
| Cold start as installed (profile from the APK only), median (ms) | 408 | 400, 408, 416 |
| coldstart_installed.runs_ms | [407, 429, 401, 445, 421, 411] | [411, 487, 406, 382, 394, 386], [411, 406, 389, 411, 512, 350], [407, 429, 401, 445, 421, 411] |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 3, 2 |
| First back swipe after a restart (warm), janky frames | 3 | 4, 3, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 11.45 | 5.63, 15.99, 11.45 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6 | 5.87, 7.18, 6 |
| meminfo.graphics_mb | 54.8 | 54.8, 54.8, 54.8 |
| PSS after the journey, less the window's graphics buffers (MB) | 79 | 79, 78.9, 82.3 |
| PSS after the journey (MB) | 133.7 | 133.7, 133.7, 137.1 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.05 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 18.98 | 17.35, 19.2, 18.98 |
| pushpop.first3_gpu_ms.median | 13.47 | 13.39, 13.47, 13.7 |
| pushpop.first3_issue_ms.max | 7.73 | 9.26, 7.73, 7.64 |
| pushpop.first3_issue_ms.median | 2.71 | 3.04, 2.71, 2.4 |
| pushpop.frames | 1010 | 1009, 1010, 1015 |
| pushpop.gpu_p50_ms | 8 | 8, 7, 9 |
| pushpop.gpu_p90_ms | 12 | 12, 12, 12 |
| pushpop.gpu_p99_ms | 18 | 18, 18, 18 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 12.15 | 12.11, 12.15, 12.15 |
| Push/pop with the Back key, janky frames (%) | 1.78 | 1.78, 1.78, 1.97 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 12 | 12, 12, 15 |
| pushpop.p90_ms | 19 | 19, 18, 19 |
| Push/pop with the Back key, p99 frame (ms) | 30 | 31, 30, 27 |
| Pop, first frame max (ms) | 9.31 | 4.43, 10.24, 9.31 |
| Pop, first frame median (ms) | 4.44 | 4.33, 4.44, 4.47 |
| Push, the frame that composes the screen, max (ms) | 15.53 | 15.57, 14.17, 15.53 |
| Push, the frame that composes the screen, median (ms) | 7.29 | 7.26, 7.36, 7.29 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.03 | 0.03, 0.02, 0.03 |
| pushpop_gesture.first3_gpu_ms.max | 17.55 | 21.23, 16.93, 17.55 |
| pushpop_gesture.first3_gpu_ms.median | 12.08 | 12.08, 12.27, 12.03 |
| pushpop_gesture.first3_issue_ms.max | 12.14 | 12.45, 12.14, 8.82 |
| pushpop_gesture.first3_issue_ms.median | 2.41 | 2.41, 2.13, 2.48 |
| pushpop_gesture.frames | 653 | 653, 652, 654 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.72 | 3.72, 3.74, 3.61 |
| Back gesture, slowest of its first three frames, median (ms) | 2.75 | 2.02, 3.2, 2.75 |
| pushpop_gesture.gpu_p50_ms | 7 | 8, 7, 7 |
| pushpop_gesture.gpu_p90_ms | 12 | 12, 13, 12 |
| pushpop_gesture.gpu_p99_ms | 18 | 18, 18, 17 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 25.1 | 26.42, 25.1, 24.79 |
| Push/pop with the back gesture, janky frames (%) | 3.06 | 3.06, 3.07, 3.06 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 11 | 13, 11, 11 |
| pushpop_gesture.p90_ms | 19 | 19, 19, 19 |
| pushpop_gesture.p99_ms | 30 | 30, 31, 29 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.05 | 0.04, 0.05, 0.07 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 15.8 | 16.93, 13.83, 15.8 |
| rapid_back.first3_gpu_ms.median | 10.71 | 10.71, 7.48, 11.67 |
| rapid_back.first3_issue_ms.max | 7.46 | 6.9, 7.46, 7.61 |
| rapid_back.first3_issue_ms.median | 2.6 | 2.1, 4.75, 2.6 |
| rapid_back.frames | 648 | 648, 648, 648 |
| rapid_back.gpu_p50_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p90_ms | 9 | 9, 9, 9 |
| rapid_back.gpu_p99_ms | 12 | 13, 11, 12 |
| Rapid Back, hitch time per second of motion (ms/s) | 12.49 | 7.81, 16.95, 12.49 |
| Rapid Back, janky frames (%) | 1.85 | 1.85, 2.16, 1.7 |
| rapid_back.late_first_frames | 5 | 6, 5, 5 |
| rapid_back.p50_ms | 11 | 11, 11, 12 |
| rapid_back.p90_ms | 13 | 13, 13, 14 |
| rapid_back.p99_ms | 19 | 20, 18, 19 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 10.58 | 10.58, 7.48, 12.56 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 7.55 | 8.71, 7.19, 7.55 |
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
| scroll_minimize.frames | 1355 | 1355, 1362, 1305 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 7 | 6, 7, 7 |
| scroll_minimize.gpu_p99_ms | 9 | 9, 8, 10 |
| Scroll with the bar minimizing, janky frames (%) | 0.88 | 0.81, 0.88, 2.68 |
| scroll_minimize.p50_ms | 9 | 9, 9, 10 |
| scroll_minimize.p90_ms | 10 | 10, 10, 14 |
| scroll_minimize.p99_ms | 16 | 16, 14, 18 |
| scroll_root.frames | 1357 | 1322, 1359, 1357 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 7 |
| scroll_root.gpu_p99_ms | 10 | 10, 10, 8 |
| Scroll a root list, janky frames (%) | 0.98 | 0.98, 0.66, 1.25 |
| scroll_root.p50_ms | 9 | 9, 9, 10 |
| scroll_root.p90_ms | 10 | 10, 10, 11 |
| scroll_root.p99_ms | 17 | 17, 17, 15 |
| tab1.first_frame_ms.max | 9.74 | 9.74, 9.99, 9.71 |
| tab1.first_frame_ms.median | 4.21 | 4.05, 4.31, 4.21 |
| First visit to a tab, first frame max (ms) | 9.14 | 10.51, 9.14, 9.01 |
| First visit to a tab, first frame median (ms) | 5 | 3.93, 6.25, 5 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.6 | 4.16, 11.13, 8.6 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.21 | 3.91, 8.85, 4.21 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.05, 0.06, 0.12 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 18.23 | 18.83, 18.18, 18.23 |
| tabs.first3_gpu_ms.median | 11.28 | 11.09, 11.37, 11.28 |
| tabs.first3_issue_ms.max | 6.07 | 5.53, 6.07, 6.55 |
| tabs.first3_issue_ms.median | 1.71 | 1.76, 1.71, 1.61 |
| Tab switch to a visited tab, first frame max (ms) | 11.13 | 11.29, 11.13, 10.48 |
| Tab switch to a visited tab, first frame median (ms) | 4.29 | 4.13, 4.29, 4.35 |
| tabs.frames | 1305 | 1305, 1304, 1305 |
| tabs.gpu_p50_ms | 6 | 6, 6, 6 |
| tabs.gpu_p90_ms | 10 | 10, 10, 10 |
| tabs.gpu_p99_ms | 14 | 14, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.07, 1.07, 1.07 |
| tabs.late_first_frames | 14 | 14, 14, 14 |
| tabs.p50_ms | 9 | 9, 9, 9 |
| tabs.p90_ms | 15 | 15, 14, 15 |
| tabs.p99_ms | 21 | 20, 22, 21 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.05, 0.06, 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 16.93 | 16.42, 16.93, 17.58 |
| tabs_tap.first3_gpu_ms.median | 11.18 | 11.16, 11.59, 11.18 |
| tabs_tap.first3_issue_ms.max | 6.6 | 6.29, 6.6, 7.07 |
| tabs_tap.first3_issue_ms.median | 1.83 | 1.81, 1.83, 1.93 |
| Tab switch with plain taps, first frame max (ms) | 10.69 | 10.69, 10.74, 9.99 |
| Tab switch with plain taps, first frame median (ms) | 4.23 | 3.95, 4.23, 4.24 |
| tabs_tap.frames | 1304 | 1304, 1303, 1305 |
| tabs_tap.gpu_p50_ms | 6 | 6, 7, 6 |
| tabs_tap.gpu_p90_ms | 9 | 8, 10, 9 |
| tabs_tap.gpu_p99_ms | 14 | 14, 14, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs_tap.late_first_frames | 15 | 15, 15, 15 |
| tabs_tap.p50_ms | 9 | 9, 9, 9 |
| tabs_tap.p90_ms | 13 | 12, 15, 13 |
| tabs_tap.p99_ms | 21 | 21, 21, 21 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 5 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.14 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.21 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.6 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 11.45 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.29 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 11.13 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.23 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 10.69 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 7.29 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 15.53 | ratchet | report |
| Pop, first frame median (ms) | 4.44 | 8 | pass |
| Pop, first frame max (ms) | 9.31 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.78 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 30 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 12.15 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 3.06 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.75 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 25.1 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 1.85 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 7.55 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 10.58 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 12.49 | ratchet | report |
| Navigation stress: the app is still running | — | yes | not measured |
| Navigation stress: stack invariants hold | — | yes | not measured |
| Navigation stress, janky frames (%) | — | 4 | not measured |
| Process death: tab, stacks and presented sheet restored | — | yes | not measured |
| Process death: Back pops the restored stack | — | yes | not measured |
| Scroll a root list, janky frames (%) | 0.98 | 0.5 | fail |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.88 | 0.5 | fail |
| Scroll with the bar minimizing, GPU p90 (ms) | 7 | 7 | pass |
| Idle 5 s on every tab, frames rendered | — | 0 | not measured |
| Cold start, speed-profile, median (ms) | 354 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 408 | 400 | fail |
| PSS after the journey, less the window's graphics buffers (MB) | 79 | 48 | fail |
| PSS after the journey (MB) | 133.7 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
