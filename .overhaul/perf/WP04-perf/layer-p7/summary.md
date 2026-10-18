# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\layer\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.0 °C at the start
- Runs: 3, seed 910133, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, scroll_minimize

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 3, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.41 | 5.84, 7.01, 6.41 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.45 | 6.11, 6.45, 12.08 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.76 | 20.76, 19.57, 21.19 |
| pushpop.first3_gpu_ms.median | 10.96 | 10.69, 10.96, 11.04 |
| pushpop.first3_issue_ms.max | 8.71 | 8.71, 4.48, 9.18 |
| pushpop.first3_issue_ms.median | 2.08 | 2.41, 2.08, 2.07 |
| pushpop.frames | 1023 | 1023, 1024, 1021 |
| pushpop.gpu_p50_ms | 6 | 6, 6, 7 |
| pushpop.gpu_p90_ms | 9 | 9, 9, 9 |
| pushpop.gpu_p99_ms | 15 | 15, 15, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2.02 | 2.02, 2.01, 4.01 |
| pushpop.janky_after_first_pct | 0 | 0, 0, 0.1 |
| Push/pop with the Back key, janky frames (%) | 1.56 | 1.56, 1.56, 1.57 |
| pushpop.late_first_frames | 16 | 16, 16, 15 |
| pushpop.p50_ms | 10 | 10, 10, 10 |
| pushpop.p90_ms | 16 | 16, 16, 17 |
| Push/pop with the Back key, p99 frame (ms) | 26 | 26, 25, 29 |
| Pop, first frame max (ms) | 5.11 | 5.08, 5.14, 5.11 |
| Pop, first frame median (ms) | 2.3 | 2.3, 2.38, 2.3 |
| Push, the frame that composes the screen, max (ms) | 15.32 | 14.26, 15.32, 15.36 |
| Push, the frame that composes the screen, median (ms) | 9.57 | 7.11, 10.45, 9.57 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.2 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 17.45 | 17.34, 17.45, 20.22 |
| pushpop_gesture.first3_gpu_ms.median | 11.75 | 11.75, 10.46, 12.36 |
| pushpop_gesture.first3_issue_ms.max | 7.47 | 7.47, 7.55, 4.92 |
| pushpop_gesture.first3_issue_ms.median | 1.66 | 1.77, 1.66, 1.65 |
| pushpop_gesture.frames | 750 | 750, 753, 745 |
| pushpop_gesture.gesture_first_frame_ms.max | 2.26 | 2.15, 2.26, 10.03 |
| Back gesture, slowest of its first three frames, median (ms) | 1.77 | 1.77, 1.92, 1.2 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 15, 16 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 23 | 20.38, 23, 23.28 |
| pushpop_gesture.janky_after_first_pct | 1.2 | 1.07, 1.2, 1.34 |
| Push/pop with the back gesture, janky frames (%) | 2.67 | 2.67, 2.39, 2.95 |
| pushpop_gesture.late_first_frames | 12 | 12, 9, 12 |
| pushpop_gesture.p50_ms | 9 | 9, 9, 9 |
| pushpop_gesture.p90_ms | 17 | 17, 15, 17 |
| pushpop_gesture.p99_ms | 28 | 24, 32, 28 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.05 | 0.11, 0.04, 0.05 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.01 |
| rapid_back.first3_gpu_ms.max | 10.42 | 10.42, 10, 10.95 |
| rapid_back.first3_gpu_ms.median | 5.98 | 5.52, 5.98, 8.94 |
| rapid_back.first3_issue_ms.max | 8.61 | 9.11, 8.61, 7.71 |
| rapid_back.first3_issue_ms.median | 2.5 | 2.5, 2.58, 2.26 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 10 | 10, 9, 10 |
| Rapid Back, hitch time per second of motion (ms/s) | 6.19 | 10.72, 6.19, 3.08 |
| rapid_back.janky_after_first_pct | 0.31 | 0.46, 0.31, 0.15 |
| Rapid Back, janky frames (%) | 0.92 | 0.92, 1.07, 0.92 |
| rapid_back.late_first_frames | 5 | 3, 5, 5 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 11 | 11, 11, 11 |
| rapid_back.p99_ms | 14 | 16, 14, 14 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.56 | 5.31, 5.62, 5.56 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.18 | 5.18, 5.16, 5.19 |
| rapid_back.transitions | 6 | 6, 6, 6 |
| scroll_minimize.frames | 1358 | 1358, 1359, 1309 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 10 | 10, 10, 9 |
| Scroll with the bar minimizing, janky frames (%) | 0.37 | 0.37, 0.37, 0.84 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 11 |
| scroll_minimize.p99_ms | 17 | 17, 17, 16 |
| scroll_root.frames | 1356 | 1356, 1365, 1356 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 9, 7, 7 |
| Scroll a root list, janky frames (%) | 0.44 | 0.37, 0.59, 0.44 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 13 | 17, 12, 13 |
| tab1.first_frame_ms.max | 8.59 | 8.59, 8.05, 9.19 |
| tab1.first_frame_ms.median | 3.91 | 3.91, 3.93, 3.68 |
| First visit to a tab, first frame max (ms) | 8.56 | 4.64, 8.56, 10.09 |
| First visit to a tab, first frame median (ms) | 4.26 | 3.84, 4.26, 5.95 |
| First visit to a tab after a restart (warm), first frame max (ms) | 7.34 | 4.45, 9.38, 7.34 |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.94 | 3.94, 4.22, 3.65 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.53 | 18.68, 25.29, 20.53 |
| tabs.first3_gpu_ms.median | 10.16 | 9.64, 10.75, 10.16 |
| tabs.first3_issue_ms.max | 4.59 | 4.59, 5.25, 3.89 |
| tabs.first3_issue_ms.median | 1.35 | 1.35, 1.25, 1.43 |
| Tab switch to a visited tab, first frame max (ms) | 10.42 | 10.83, 10.27, 10.42 |
| Tab switch to a visited tab, first frame median (ms) | 4.2 | 7.93, 4.16, 4.2 |
| tabs.frames | 1304 | 1304, 1304, 1304 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 9 | 8, 10, 9 |
| tabs.gpu_p99_ms | 14 | 14, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 1.57, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.15, 1.07, 1.07 |
| tabs.late_first_frames | 14 | 15, 14, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 14 | 11, 16, 14 |
| tabs.p99_ms | 21 | 20, 21, 21 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.05 | 0.05, 0.05, 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 17.43 | 17.33, 19.88, 17.43 |
| tabs_tap.first3_gpu_ms.median | 9.85 | 9.55, 9.85, 10.39 |
| tabs_tap.first3_issue_ms.max | 4.25 | 4.25, 4.18, 5.02 |
| tabs_tap.first3_issue_ms.median | 1.42 | 1.8, 1.42, 1.42 |
| Tab switch with plain taps, first frame max (ms) | 9.41 | 9.79, 9.36, 9.41 |
| Tab switch with plain taps, first frame median (ms) | 3.96 | 3.62, 4.1, 3.96 |
| tabs_tap.frames | 1305 | 1304, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 7, 7, 8 |
| tabs_tap.gpu_p99_ms | 14 | 13, 14, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0.79, 0, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0.08, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.07, 1.15, 1.15 |
| tabs_tap.late_first_frames | 14 | 14, 14, 15 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 11, 11, 11 |
| tabs_tap.p99_ms | 20 | 20, 20, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.26 | ratchet | report |
| First visit to a tab, first frame max (ms) | 8.56 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.94 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 7.34 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.41 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.45 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.2 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 10.42 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 3.96 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.41 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 9.57 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 15.32 | ratchet | report |
| Pop, first frame median (ms) | 2.3 | 8 | pass |
| Pop, first frame max (ms) | 5.11 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.56 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 26 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2.02 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.67 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 1.77 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 23 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 0.92 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.18 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.56 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 6.19 | ratchet | report |
| Navigation stress: the app is still running | — | yes | not measured |
| Navigation stress: stack invariants hold | — | yes | not measured |
| Navigation stress, janky frames (%) | — | 4 | not measured |
| Process death: tab, stacks and presented sheet restored | — | yes | not measured |
| Process death: Back pops the restored stack | — | yes | not measured |
| Scroll a root list, janky frames (%) | 0.44 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.37 | 0.5 | pass |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | — | 0 | not measured |
| Cold start, speed-profile, median (ms) | — | 380 | not measured |
| Cold start as installed (profile from the APK only), median (ms) | — | 400 | not measured |
| PSS after the journey, less the window's graphics buffers (MB) | — | 48 | not measured |
| PSS after the journey (MB) | — | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
