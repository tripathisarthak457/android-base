# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04\final-everything\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 31.1 °C at the start
- Runs: 3, seed 137188, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap, tab1, pushpop, pushpop_gesture, back_during_push, rapid_back, scroll_root, idle, nav_stress, restore, meminfo, scroll_minimize, recompose, coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| back_during_push.difference_ratios | [0.98, 0.96, 0.98, 0.96] | [0.98, 0.96, 0.98, 0.96], [0.98, 0.96, 0.97, 0.96], [0.98, 0.96, 0.98, 0.96] |
| Back swipe during a push: lands on the list | yes | yes, yes, yes |
| Back swipe during a push: one opaque card, no double exposure | yes | yes, yes, yes |
| Cold start, speed-profile, median (ms) | 324 | 324, 337, 316 |
| coldstart.runs_ms | [318, 336, 314, 300, 350, 298] | [371, 319, 325, 324, 322, 354], [359, 377, 371, 311, 305, 315], [318, 336, 314, 300, 350, 298] |
| Cold start as installed (profile from the APK only), median (ms) | 387 | 387, 389, 378 |
| coldstart_installed.runs_ms | [396, 370, 386, 364, 395, 368] | [380, 394, 407, 361, 405, 373], [397, 406, 388, 384, 390, 388], [396, 370, 386, 364, 395, 368] |
| First back swipe after pm clear (cold), janky frames | 3 | 3, 2, 3 |
| First back swipe after a restart (warm), janky frames | 4 | 2, 4, 4 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.1 | 5.59, 6.1, 11.56 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.84 | 13.42, 6.19, 6.84 |
| Idle 5 s on every tab, frames rendered | 0 | 0, 0, 0 |
| idle.frames_per_tab | [0, 0, 0, 0, 0] | [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0] |
| meminfo.graphics_mb | 53.3 | 53.3, 53.3, 53.3 |
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | 56.3 | 55.9, 56.4, 56.3 |
| PSS after the journey (MB) | 109.7 | 109.2, 109.8, 109.7 |
| Navigation stress: the app is still running | yes | yes, yes, yes |
| nav_stress.exits | 14 | 14, 17, 9 |
| nav_stress.frames | 3967 | 4028, 3530, 3967 |
| nav_stress.gpu_p50_ms | 6 | 7, 6, 6 |
| nav_stress.gpu_p90_ms | 9 | 10, 9, 9 |
| nav_stress.gpu_p99_ms | 14 | 14, 14, 14 |
| Navigation stress: stack invariants hold | yes | yes, yes, yes |
| Navigation stress, janky frames (%) | 2.81 | 2.81, 4.14, 1.87 |
| nav_stress.p50_ms | 10 | 12, 10, 10 |
| nav_stress.p90_ms | 19 | 19, 19, 17 |
| nav_stress.p99_ms | 40 | 40, 44, 31 |
| nav_stress.states | 47 | 26, 47, 66 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 21.47 | 19.19, 22.01, 21.47 |
| pushpop.first3_gpu_ms.median | 11.02 | 10.47, 12.13, 11.02 |
| pushpop.first3_issue_ms.max | 9.17 | 9.44, 9.17, 7.31 |
| pushpop.first3_issue_ms.median | 2.11 | 1.85, 2.21, 2.11 |
| pushpop.frames | 1024 | 1024, 1024, 1021 |
| pushpop.gpu_p50_ms | 6 | 6, 7, 6 |
| pushpop.gpu_p90_ms | 10 | 9, 10, 10 |
| pushpop.gpu_p99_ms | 16 | 15, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 3.04 | 4.03, 1.01, 3.04 |
| Push/pop with the Back key, janky frames other than each motion's first (%) | 0.1 | 0.1, 0, 0.1 |
| Push/pop with the Back key, janky frames (%) [non-regression] | 1.66 | 1.66, 1.56, 1.67 |
| Push/pop with the Back key, motions whose first frame missed its deadline (count) | 16 | 16, 16, 16 |
| pushpop.p50_ms | 10 | 10, 10, 10 |
| pushpop.p90_ms | 17 | 17, 18, 17 |
| Push/pop with the Back key, p99 frame (ms) [non-regression] | 26 | 25, 26, 29 |
| Pop, first frame max (ms) [non-regression] | 5.16 | 4.33, 5.16, 6.12 |
| Pop, first frame median (ms) [non-regression] | 2.42 | 2.38, 2.44, 2.42 |
| Push, the frame that composes the screen, max (ms) [non-regression] | 15.28 | 13.9, 15.28, 15.97 |
| Push, the frame that composes the screen, median (ms) [non-regression] | 9.19 | 9.19, 7.05, 14.09 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.05 | 0.05, 0.05, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 19.6 | 18.35, 19.6, 21.63 |
| pushpop_gesture.first3_gpu_ms.median | 11.87 | 11.87, 11.13, 12.48 |
| pushpop_gesture.first3_issue_ms.max | 7.85 | 5.38, 9.93, 7.85 |
| pushpop_gesture.first3_issue_ms.median | 1.59 | 1.59, 1.56, 1.66 |
| pushpop_gesture.frames | 750 | 752, 746, 750 |
| pushpop_gesture.gesture_first_frame_ms.max | 2.82 | 2.7, 4.36, 2.82 |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | 2.41 | 2.25, 2.41, 2.53 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 16 | 16, 15, 17 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 16.39 | 8.27, 23.34, 16.39 |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | 0.93 | 0.66, 1.07, 0.93 |
| Push/pop with the back gesture, janky frames (%) [non-regression] | 2.53 | 2.26, 2.55, 2.53 |
| Push/pop with the back gesture, motions whose first frame missed its deadline (count) | 12 | 12, 11, 12 |
| pushpop_gesture.p50_ms | 9 | 9, 9, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 25 | 29, 24, 25 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.06 | 0.04, 0.06, 0.3 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 9.57 | 10.61, 9.57, 9.07 |
| rapid_back.first3_gpu_ms.median | 5.24 | 5.25, 5.13, 5.24 |
| rapid_back.first3_issue_ms.max | 9.01 | 9.01, 8.73, 10.3 |
| rapid_back.first3_issue_ms.median | 2.44 | 2.62, 2.44, 2.27 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 10 | 10, 10, 9 |
| Rapid Back, hitch time per second of motion (ms/s) | 12.32 | 13.85, 12.32, 12.29 |
| Rapid Back, janky frames other than each motion's first (%) | 0.76 | 0.76, 0.46, 0.76 |
| Rapid Back, janky frames (%) [non-regression] | 0.92 | 0.92, 0.76, 0.92 |
| Rapid Back, motions whose first frame missed its deadline (count) | 1 | 1, 2, 1 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 12, 12, 11 |
| rapid_back.p99_ms | 14 | 14, 14, 14 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) [non-regression] | 8.63 | 6.96, 11.1, 8.63 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) [non-regression] | 5.31 | 5.31, 5.37, 5.08 |
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
| scroll_minimize.frames | 1303 | 1360, 1303, 1287 |
| scroll_minimize.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_minimize.gpu_p99_ms | 7 | 7, 8, 7 |
| Scroll with the bar minimizing, janky frames (%) | 0.59 | 0.59, 0.84, 0.31 |
| scroll_minimize.p50_ms | 8 | 8, 8, 8 |
| scroll_minimize.p90_ms | 10 | 9, 11, 10 |
| scroll_minimize.p99_ms | 13 | 12, 15, 13 |
| scroll_root.frames | 1365 | 1365, 1304, 1367 |
| scroll_root.gpu_p50_ms | 6 | 6, 6, 6 |
| Scroll a root list, GPU p90 (ms) | 6 | 6, 6, 6 |
| scroll_root.gpu_p99_ms | 9 | 9, 8, 10 |
| Scroll a root list, janky frames (%) | 0.37 | 0.37, 0.31, 0.44 |
| scroll_root.p50_ms | 8 | 8, 8, 8 |
| scroll_root.p90_ms | 9 | 9, 11, 9 |
| scroll_root.p99_ms | 16 | 16, 13, 17 |
| tab1.first_frame_ms.max | 9.57 | 8.66, 9.75, 9.57 |
| tab1.first_frame_ms.median | 7.81 | 7.81, 8.23, 5.17 |
| First visit to a tab, first frame max (ms) | 7.92 | 10.05, 7.35, 7.92 |
| First visit to a tab, first frame median (ms) | 4.05 | 3.58, 4.17, 4.05 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.5 | 8.5, 10.72, 8.49 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.4 | 3.79, 8.29, 6.4 |
| tabs.first3_dequeue_ms.max | 0.05 | 0.05, 0.06, 0.05 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.77 | 20.77, 22.03, 18.43 |
| tabs.first3_gpu_ms.median | 10.32 | 9.93, 10.32, 10.34 |
| tabs.first3_issue_ms.max | 5.7 | 5.7, 5.6, 6.43 |
| tabs.first3_issue_ms.median | 1.43 | 1.52, 1.43, 1.43 |
| Tab switch to a visited tab, first frame max (ms) [non-regression] | 9.8 | 9.8, 9.78, 10.13 |
| Tab switch to a visited tab, first frame median (ms) [non-regression] | 4.33 | 4.12, 4.33, 8.59 |
| tabs.frames | 1305 | 1305, 1304, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 7 | 7, 10, 7 |
| tabs.gpu_p99_ms | 13 | 13, 14, 13 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 1.56, 0 |
| Tab switch, janky frames other than each motion's first (%) | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) [non-regression] | 1.07 | 1, 1.15, 1.07 |
| Tab switch, motions whose first frame missed its deadline (count) | 14 | 13, 15, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 16, 11 |
| tabs.p99_ms | 21 | 19, 22, 21 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.08 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 19.91 | 20.4, 19.31, 19.91 |
| tabs_tap.first3_gpu_ms.median | 10.24 | 10.24, 9.8, 10.53 |
| tabs_tap.first3_issue_ms.max | 4.96 | 5.6, 4.18, 4.96 |
| tabs_tap.first3_issue_ms.median | 1.41 | 1.23, 1.41, 1.54 |
| Tab switch with plain taps, first frame max (ms) [non-regression] | 10 | 9.76, 10.07, 10 |
| Tab switch with plain taps, first frame median (ms) [non-regression] | 4.2 | 8.05, 4.07, 4.2 |
| tabs_tap.frames | 1305 | 1305, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 8 | 7, 9, 8 |
| tabs_tap.gpu_p99_ms | 14 | 14, 14, 13 |
| Tab switch with plain taps, hitch time per second of motion (ms/s) | 0 | 0, 0, 0.78 |
| Tab switch with plain taps, janky frames other than each motion's first (%) | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) [non-regression] | 1.15 | 1.15, 1.07, 1.15 |
| Tab switch with plain taps, motions whose first frame missed its deadline (count) | 15 | 15, 14, 15 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 13 | 11, 16, 13 |
| tabs_tap.p99_ms | 20 | 20, 19, 20 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.05 | ratchet | report |
| First visit to a tab, first frame max (ms) | 7.92 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.4 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.5 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.1 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.84 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 4 | ratchet | report |
| Tab switch, janky frames (%) [non-regression] | 1.07 | 1.26 | pass |
| Tab switch to a visited tab, first frame median (ms) [non-regression] | 4.33 | 7.6 | pass |
| Tab switch to a visited tab, first frame max (ms) [non-regression] | 9.8 | 11.9 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 5 | pass |
| Tab switch, janky frames other than each motion's first (%) | 0 | 0.5 | pass |
| Tab switch, motions whose first frame missed its deadline (count) | 14 | ratchet | report |
| Tab switch with plain taps, janky frames (%) [non-regression] | 1.15 | 1.26 | pass |
| Tab switch with plain taps, first frame median (ms) [non-regression] | 4.2 | 4.5 | pass |
| Tab switch with plain taps, first frame max (ms) [non-regression] | 10 | 11.7 | pass |
| Tab switch with plain taps, hitch time per second of motion (ms/s) | 0 | 5 | pass |
| Tab switch with plain taps, janky frames other than each motion's first (%) | 0 | 0.5 | pass |
| Tab switch with plain taps, motions whose first frame missed its deadline (count) | 15 | ratchet | report |
| Push, the frame that composes the screen, median (ms) [non-regression] | 9.19 | 14.4 | pass |
| Push, the frame that composes the screen, max (ms) [non-regression] | 15.28 | 18.3 | pass |
| Pop, first frame median (ms) [non-regression] | 2.42 | 4.1 | pass |
| Pop, first frame max (ms) [non-regression] | 5.16 | 5.9 | pass |
| Push/pop with the Back key, janky frames (%) [non-regression] | 1.66 | 1.95 | pass |
| Push/pop with the Back key, p99 frame (ms) [non-regression] | 26 | 35 | pass |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 3.04 | 5 | pass |
| Push/pop with the Back key, janky frames other than each motion's first (%) | 0.1 | 0.5 | pass |
| Push/pop with the Back key, motions whose first frame missed its deadline (count) | 16 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) [non-regression] | 2.53 | 3.1 | pass |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | 2.41 | 2.7 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 16.39 | 5 | fail |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | 0.93 | 1 | pass |
| Push/pop with the back gesture, motions whose first frame missed its deadline (count) | 12 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | yes | yes | pass |
| Back swipe during a push: lands on the list | yes | yes | pass |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) [non-regression] | 0.92 | 1.18 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) [non-regression] | 5.31 | 6.2 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) [non-regression] | 8.63 | 7.9 | fail |
| Rapid Back, hitch time per second of motion (ms/s) | 12.32 | 5 | fail |
| Rapid Back, janky frames other than each motion's first (%) | 0.76 | 1 | pass |
| Rapid Back, motions whose first frame missed its deadline (count) | 1 | ratchet | report |
| Navigation stress: the app is still running | yes | yes | pass |
| Navigation stress: stack invariants hold | yes | yes | pass |
| Navigation stress, janky frames (%) | 2.81 | 4 | pass |
| Process death: tab, stacks and presented sheet restored | yes | yes | pass |
| Process death: Back pops the restored stack | yes | yes | pass |
| Scroll a root list, janky frames (%) | 0.37 | 0.5 | pass |
| Scroll a root list, GPU p90 (ms) | 6 | 7 | pass |
| Scroll with the bar minimizing, janky frames (%) | 0.59 | 0.5 | fail |
| Scroll with the bar minimizing, GPU p90 (ms) | 6 | 7 | pass |
| Idle 5 s on every tab, frames rendered | 0 | 0 | pass |
| Cold start, speed-profile, median (ms) | 324 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 387 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | 56.3 | 58.4 | pass |
| PSS after the journey (MB) | 109.7 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | 1 | 2 | pass |
| Push, frames that recompose (most in one push) | 5 | ratchet | report |
| Pop, frames that recompose (most in one pop) | 2 | 2 | pass |
| Back gesture, frames that recompose while the card follows the finger | 0 | 0 | pass |
