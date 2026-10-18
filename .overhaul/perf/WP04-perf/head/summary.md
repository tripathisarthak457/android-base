# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\head\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.0 °C at the start
- Runs: 3, seed 967575, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 368 | 380, 356, 368 |
| coldstart.runs_ms | [368, 364, 373, 337, 381, 368] | [382, 362, 379, 340, 382, 438], [358, 349, 333, 360, 368, 355], [368, 364, 373, 337, 381, 368] |
| Cold start as installed (profile from the APK only), median (ms) | 403 | 405, 398, 403 |
| coldstart_installed.runs_ms | [406, 399, 428, 401, 389, 410] | [375, 400, 410, 389, 448, 412], [407, 411, 440, 377, 390, 386], [406, 399, 428, 401, 389, 410] |
| First back swipe after pm clear (cold), janky frames | 2 | 3, 2, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 3, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.38 | 15.65, 7.08, 7.38 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.26 | 15.26, 6.24, 7.26 |
| meminfo.graphics_mb | 55.1 | 55.1, 55.1, 55 |
| PSS after the journey, less the window's graphics buffers (MB) | 80.5 | 80.5, 81.5, 79 |
| PSS after the journey (MB) | 135.6 | 135.6, 136.6, 134 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 22.09 | 23.22, 18.87, 22.09 |
| pushpop.first3_gpu_ms.median | 11.29 | 11.29, 11.57, 11.26 |
| pushpop.first3_issue_ms.max | 6.89 | 6.89, 7.73, 5.45 |
| pushpop.first3_issue_ms.median | 2.22 | 2.58, 2.22, 2.13 |
| pushpop.frames | 1020 | 1017, 1020, 1023 |
| pushpop.gpu_p50_ms | 7 | 7, 7, 7 |
| pushpop.gpu_p90_ms | 10 | 10, 10, 9 |
| pushpop.gpu_p99_ms | 15 | 16, 15, 15 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.01 | 9.1, 4.01, 1.99 |
| pushpop.janky_after_first_pct | 0.1 | 0.49, 0.1, 0.1 |
| Push/pop with the Back key, janky frames (%) | 1.67 | 1.97, 1.67, 1.66 |
| pushpop.late_first_frames | 16 | 15, 16, 16 |
| pushpop.p50_ms | 11 | 11, 11, 11 |
| pushpop.p90_ms | 16 | 18, 16, 16 |
| Push/pop with the Back key, p99 frame (ms) | 29 | 31, 29, 26 |
| Pop, first frame max (ms) | 5.37 | 5.58, 5.03, 5.37 |
| Pop, first frame median (ms) | 2.35 | 2.33, 2.36, 2.35 |
| Push, the frame that composes the screen, max (ms) | 15.73 | 16.13, 15.73, 15.61 |
| Push, the frame that composes the screen, median (ms) | 7.34 | 6.37, 10.17, 7.34 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.03, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 18.99 | 22.9, 17.8, 18.99 |
| pushpop_gesture.first3_gpu_ms.median | 11.83 | 10.84, 12.09, 11.83 |
| pushpop_gesture.first3_issue_ms.max | 7.05 | 7.32, 7.05, 7.04 |
| pushpop_gesture.first3_issue_ms.median | 2.66 | 2.66, 4.13, 2.08 |
| pushpop_gesture.frames | 749 | 752, 747, 749 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.62 | 3.54, 3.62, 5.24 |
| Back gesture, slowest of its first three frames, median (ms) | 2.69 | 1.8, 2.69, 2.71 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 11, 10 |
| pushpop_gesture.gpu_p99_ms | 16 | 17, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 21.72 | 13.55, 21.72, 23.1 |
| pushpop_gesture.janky_after_first_pct | 0.93 | 0.53, 1.07, 0.93 |
| Push/pop with the back gesture, janky frames (%) | 2.54 | 2.13, 2.68, 2.54 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 27 | 27, 28, 25 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.05 | 0.05, 0.05, 0.07 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 9.87 | 10.58, 9.87, 9.26 |
| rapid_back.first3_gpu_ms.median | 5.32 | 5.32, 5.8, 5.15 |
| rapid_back.first3_issue_ms.max | 7.23 | 7.23, 7, 7.29 |
| rapid_back.first3_issue_ms.median | 2.33 | 2.33, 2.56, 2.22 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 9 | 9, 10, 9 |
| Rapid Back, hitch time per second of motion (ms/s) | 10.8 | 12.28, 7.71, 10.8 |
| rapid_back.janky_after_first_pct | 0.61 | 0.61, 0.92, 0.61 |
| Rapid Back, janky frames (%) | 0.92 | 0.92, 1.38, 0.92 |
| rapid_back.late_first_frames | 2 | 2, 3, 2 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 12, 12, 12 |
| rapid_back.p99_ms | 14 | 14, 14, 14 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.74 | 5.74, 8.14, 5.69 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.67 | 5.72, 5.67, 5.53 |
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
| scroll_minimize.frames | 1360 | 1362, 1360, 1356 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 7, 7, 9 |
| Scroll with the bar minimizing, janky frames (%) | 0.37 | 0.37, 0.59, 0.22 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 9 |
| scroll_minimize.p99_ms | 13 | 12, 13, 16 |
| scroll_root.frames | 1357 | 1358, 1356, 1357 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 7, 9 |
| Scroll a root list, janky frames (%) | 0.52 | 0.59, 0.22, 0.52 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 13 | 13, 12, 17 |
| tab1.first_frame_ms.max | 9.26 | 9.43, 9.12, 9.26 |
| tab1.first_frame_ms.median | 4.04 | 4.04, 3.82, 7.91 |
| First visit to a tab, first frame max (ms) | 7.32 | 6.95, 7.32, 9.82 |
| First visit to a tab, first frame median (ms) | 4.21 | 4.21, 3.84, 6.8 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.95 | 9.95, 10.8, 8.61 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.17 | 6.17, 5.5, 6.36 |
| tabs.first3_dequeue_ms.max | 0.05 | 0.05, 0.05, 0.03 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.16 | 21.86, 20.16, 20.16 |
| tabs.first3_gpu_ms.median | 10 | 10, 9.03, 10.76 |
| tabs.first3_issue_ms.max | 6.49 | 7.89, 6.49, 3.39 |
| tabs.first3_issue_ms.median | 1.42 | 1.56, 1.42, 1.38 |
| Tab switch to a visited tab, first frame max (ms) | 9.82 | 9.82, 8.79, 10.54 |
| Tab switch to a visited tab, first frame median (ms) | 4.04 | 4.04, 3.94, 8.04 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 8 | 8, 9, 8 |
| tabs.gpu_p99_ms | 14 | 14, 13, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0.78, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.15, 1.07, 1.07 |
| tabs.late_first_frames | 14 | 15, 14, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 12 | 12, 14, 11 |
| tabs.p99_ms | 19 | 19, 19, 20 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.05 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 20.08 | 20.08, 21.02, 19.66 |
| tabs_tap.first3_gpu_ms.median | 9.98 | 9.98, 9.68, 10.67 |
| tabs_tap.first3_issue_ms.max | 4.95 | 4.87, 5.2, 4.95 |
| tabs_tap.first3_issue_ms.median | 1.6 | 1.6, 1.64, 1.49 |
| Tab switch with plain taps, first frame max (ms) | 9.21 | 9.93, 9.21, 9.2 |
| Tab switch with plain taps, first frame median (ms) | 4.05 | 4.05, 4.05, 3.99 |
| tabs_tap.frames | 1305 | 1305, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 10, 7, 7 |
| tabs_tap.gpu_p99_ms | 14 | 14, 14, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0, 0.78 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.15, 1.15, 1.07 |
| tabs_tap.late_first_frames | 15 | 15, 15, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 17, 11, 11 |
| tabs_tap.p99_ms | 20 | 20, 20, 18 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.21 | ratchet | report |
| First visit to a tab, first frame max (ms) | 7.32 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.17 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.95 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.38 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.26 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.04 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.82 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.05 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.21 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 7.34 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 15.73 | ratchet | report |
| Pop, first frame median (ms) | 2.35 | 8 | pass |
| Pop, first frame max (ms) | 5.37 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.67 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 29 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 4.01 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.54 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.69 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 21.72 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 0.92 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.67 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.74 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 10.8 | ratchet | report |
| Navigation stress: the app is still running | — | yes | not measured |
| Navigation stress: stack invariants hold | — | yes | not measured |
| Navigation stress, janky frames (%) | — | 4 | not measured |
| Process death: tab, stacks and presented sheet restored | — | yes | not measured |
| Process death: Back pops the restored stack | — | yes | not measured |
| Scroll a root list, janky frames (%) | 0.52 | 0.5 | fail |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.37 | 0.5 | pass |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | — | 0 | not measured |
| Cold start, speed-profile, median (ms) | 368 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 403 | 400 | fail |
| PSS after the journey, less the window's graphics buffers (MB) | 80.5 | 48 | fail |
| PSS after the journey (MB) | 135.6 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
