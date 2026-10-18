# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\cardclip\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 32.0 °C at the start
- Runs: 3, seed 269818, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, rapid_back, scroll_root, scroll_minimize

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 3 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.34 | 5.8, 14.61, 7.34 |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.59 | 19.01, 6.38, 15.59 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.1 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 20.96 | 22.73, 18.97, 20.96 |
| pushpop.first3_gpu_ms.median | 11.21 | 11.81, 11.21, 10.26 |
| pushpop.first3_issue_ms.max | 6.17 | 5.36, 6.17, 9.33 |
| pushpop.first3_issue_ms.median | 2.06 | 2.06, 1.86, 2.77 |
| pushpop.frames | 1022 | 1022, 1021, 1022 |
| pushpop.gpu_p50_ms | 7 | 7, 6, 7 |
| pushpop.gpu_p90_ms | 9 | 10, 9, 9 |
| pushpop.gpu_p99_ms | 15 | 16, 15, 15 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 3 | 4.04, 1, 3 |
| pushpop.janky_after_first_pct | 0 | 0, 0, 0 |
| Push/pop with the Back key, janky frames (%) | 1.57 | 1.57, 1.57, 1.57 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 11 | 11, 10, 11 |
| pushpop.p90_ms | 16 | 17, 16, 16 |
| Push/pop with the Back key, p99 frame (ms) | 25 | 29, 25, 25 |
| Pop, first frame max (ms) | 5.14 | 5.14, 5.07, 5.28 |
| Pop, first frame median (ms) | 2.4 | 2.4, 2.33, 3.46 |
| Push, the frame that composes the screen, max (ms) | 15.79 | 16.19, 15.5, 15.79 |
| Push, the frame that composes the screen, median (ms) | 9.07 | 9.54, 9.07, 6.8 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.05 | 0.06, 0.05, 0.05 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 19.39 | 18.45, 19.88, 19.39 |
| pushpop_gesture.first3_gpu_ms.median | 11.53 | 11.84, 10.85, 11.53 |
| pushpop_gesture.first3_issue_ms.max | 9.44 | 7.2, 10.14, 9.44 |
| pushpop_gesture.first3_issue_ms.median | 1.76 | 1.76, 1.82, 1.68 |
| pushpop_gesture.frames | 744 | 741, 744, 755 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.39 | 3.39, 2.66, 3.63 |
| Back gesture, slowest of its first three frames, median (ms) | 1.9 | 1.9, 1.52, 2.98 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 11 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 21.97 | 28.79, 21.97, 12.26 |
| pushpop_gesture.janky_after_first_pct | 0.81 | 0.81, 1.21, 0.53 |
| Push/pop with the back gesture, janky frames (%) | 2.43 | 2.43, 2.82, 2.12 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 28 | 27, 28, 32 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.04 | 0.04, 0.04, 0.05 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.01, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 9.37 | 8.98, 9.37, 10.23 |
| rapid_back.first3_gpu_ms.median | 5.21 | 5.06, 5.21, 5.76 |
| rapid_back.first3_issue_ms.max | 6.5 | 6.5, 3.87, 7.58 |
| rapid_back.first3_issue_ms.median | 2.48 | 1.98, 2.48, 2.54 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 9 | 10, 8, 9 |
| Rapid Back, hitch time per second of motion (ms/s) | 9.27 | 12.35, 9.27, 7.72 |
| rapid_back.janky_after_first_pct | 0.46 | 0.46, 0.46, 0.46 |
| Rapid Back, janky frames (%) | 0.76 | 0.61, 0.76, 0.92 |
| rapid_back.late_first_frames | 2 | 1, 2, 3 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 12, 12, 12 |
| rapid_back.p99_ms | 14 | 15, 13, 14 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.72 | 5.69, 5.82, 5.72 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.5 | 5.46, 5.5, 5.55 |
| rapid_back.transitions | 6 | 6, 6, 6 |
| scroll_minimize.frames | 1360 | 1363, 1360, 1302 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 10 | 10, 8, 10 |
| Scroll with the bar minimizing, janky frames (%) | 0.29 | 0.29, 0.29, 0.69 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 9 | 9, 9, 11 |
| scroll_minimize.p99_ms | 17 | 17, 13, 17 |
| scroll_root.frames | 1362 | 1361, 1365, 1362 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 7 | 7, 7, 7 |
| Scroll a root list, janky frames (%) | 0.37 | 0.37, 0.37, 0.44 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 9, 9 |
| scroll_root.p99_ms | 12 | 12, 12, 12 |
| tab1.first_frame_ms.max | 8.5 | 8.5, 9.49, 8.18 |
| tab1.first_frame_ms.median | 3.67 | 3.46, 8.43, 3.67 |
| First visit to a tab, first frame max (ms) | 9.4 | 9.8, 9.4, 4.37 |
| First visit to a tab, first frame median (ms) | 5.26 | 6.44, 5.26, 4.03 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.97 | 8.97, 3.93, 9.23 |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.91 | 3.91, 3.67, 6.36 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 21.01 | 21.01, 21.54, 19.82 |
| tabs.first3_gpu_ms.median | 9.88 | 9.67, 9.88, 10.1 |
| tabs.first3_issue_ms.max | 5.47 | 5.6, 5.17, 5.47 |
| tabs.first3_issue_ms.median | 1.38 | 1.27, 1.38, 1.45 |
| Tab switch to a visited tab, first frame max (ms) | 9.92 | 9.97, 9.92, 9.33 |
| Tab switch to a visited tab, first frame median (ms) | 4.07 | 4.07, 4.06, 4.15 |
| tabs.frames | 1305 | 1305, 1304, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 7 | 7, 9, 7 |
| tabs.gpu_p99_ms | 14 | 13, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 1.57, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1 | 1, 1, 1.15 |
| tabs.late_first_frames | 13 | 13, 13, 15 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 14, 11 |
| tabs.p99_ms | 20 | 19, 21, 20 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.07 | 0.06, 0.07, 0.15 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 19.63 | 21.38, 19.63, 18.77 |
| tabs_tap.first3_gpu_ms.median | 9.79 | 10.82, 9.79, 9.44 |
| tabs_tap.first3_issue_ms.max | 4.66 | 4.66, 4.3, 4.75 |
| tabs_tap.first3_issue_ms.median | 1.4 | 1.4, 1.21, 1.44 |
| Tab switch with plain taps, first frame max (ms) | 9.58 | 9.95, 9.58, 9.26 |
| Tab switch with plain taps, first frame median (ms) | 3.62 | 3.62, 4, 3.56 |
| tabs_tap.frames | 1305 | 1305, 1304, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 8, 7, 7 |
| tabs_tap.gpu_p99_ms | 13 | 13, 13, 13 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1, 1.07, 1.07 |
| tabs_tap.late_first_frames | 14 | 13, 14, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 11, 10, 11 |
| tabs_tap.p99_ms | 19 | 19, 19, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 5.26 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.4 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.91 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.97 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.34 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.59 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) | 1 | 1 | pass |
| Tab switch to a visited tab, first frame median (ms) | 4.07 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.92 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 3.62 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.58 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | 9.07 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 15.79 | ratchet | report |
| Pop, first frame median (ms) | 2.4 | 8 | pass |
| Pop, first frame max (ms) | 5.14 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.57 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 25 | 25 | pass |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 3 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.43 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 1.9 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 21.97 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 0.76 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.5 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.72 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 9.27 | ratchet | report |
| Navigation stress: the app is still running | — | yes | not measured |
| Navigation stress: stack invariants hold | — | yes | not measured |
| Navigation stress, janky frames (%) | — | 4 | not measured |
| Process death: tab, stacks and presented sheet restored | — | yes | not measured |
| Process death: Back pops the restored stack | — | yes | not measured |
| Scroll a root list, janky frames (%) | 0.37 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.29 | 0.5 | pass |
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
