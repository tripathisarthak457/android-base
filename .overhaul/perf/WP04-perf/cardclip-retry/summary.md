# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\cardclip\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 32.4 °C at the start
- Runs: 3, seed 668363, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: pushpop, pushpop_gesture, rapid_back

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 3 |
| First back swipe after a restart (warm), janky frames | 3 | 3, 4, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.37 | 12.44, 7.37, 7.22 |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.26 | 15.26, 17.55, 13.38 |
| pushpop.first3_dequeue_ms.max | 0.06 | 0.11, 0.06, 0.06 |
| pushpop.first3_dequeue_ms.median | 0.02 | 0.03, 0.02, 0.02 |
| pushpop.first3_gpu_ms.max | 21.2 | 21.03, 21.2, 22.44 |
| pushpop.first3_gpu_ms.median | 10.85 | 10.86, 10.33, 10.85 |
| pushpop.first3_issue_ms.max | 7.6 | 7.6, 7.67, 7.05 |
| pushpop.first3_issue_ms.median | 2.29 | 3.54, 2.29, 2.15 |
| pushpop.frames | 1022 | 1024, 1018, 1022 |
| pushpop.gpu_p50_ms | 7 | 7, 7, 7 |
| pushpop.gpu_p90_ms | 10 | 9, 10, 10 |
| pushpop.gpu_p99_ms | 16 | 15, 16, 16 |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2.02 | 2.02, 5.1, 0.97 |
| pushpop.janky_after_first_pct | 0.1 | 0, 0.2, 0.1 |
| Push/pop with the Back key, janky frames (%) | 1.66 | 1.56, 1.77, 1.66 |
| pushpop.late_first_frames | 16 | 16, 16, 16 |
| pushpop.p50_ms | 11 | 11, 11, 10 |
| pushpop.p90_ms | 17 | 17, 17, 17 |
| Push/pop with the Back key, p99 frame (ms) | 27 | 27, 29, 26 |
| Pop, first frame max (ms) | 5.44 | 5.44, 5.44, 5.31 |
| Pop, first frame median (ms) | 2.48 | 2.5, 2.48, 2.46 |
| Push, the frame that composes the screen, max (ms) | 15.98 | 18.31, 14.78, 15.98 |
| Push, the frame that composes the screen, median (ms) | 7.28 | 7.09, 7.48, 7.28 |
| pushpop.transitions | 16 | 16, 16, 16 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.16, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 19.68 | 20.26, 19.68, 17.54 |
| pushpop_gesture.first3_gpu_ms.median | 11.63 | 12.25, 11.63, 11.62 |
| pushpop_gesture.first3_issue_ms.max | 7.82 | 7.82, 7.08, 9.68 |
| pushpop_gesture.first3_issue_ms.median | 2.25 | 2.25, 2.35, 1.9 |
| pushpop_gesture.frames | 755 | 756, 755, 747 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.07 | 3.23, 3.07, 2.78 |
| Back gesture, slowest of its first three frames, median (ms) | 1.95 | 1.95, 1.95, 2.02 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 16 | 16, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 13.57 | 9.55, 13.57, 23.11 |
| pushpop_gesture.janky_after_first_pct | 0.66 | 0.4, 0.66, 1.47 |
| Push/pop with the back gesture, janky frames (%) | 2.25 | 1.98, 2.25, 3.08 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 30 | 26, 30, 31 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| Rapid Back: the app is still running | yes | yes, yes, yes |
| rapid_back.depth | 4 | 4, 4, 4 |
| Rapid Back: lands on the expected screen | yes | yes, yes, yes |
| rapid_back.first3_dequeue_ms.max | 0.05 | 0.04, 0.05, 0.06 |
| rapid_back.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| rapid_back.first3_gpu_ms.max | 10.52 | 10.26, 10.52, 11 |
| rapid_back.first3_gpu_ms.median | 5.28 | 5.16, 5.28, 5.75 |
| rapid_back.first3_issue_ms.max | 8.55 | 6.16, 8.79, 8.55 |
| rapid_back.first3_issue_ms.median | 2.56 | 2.42, 2.56, 2.75 |
| rapid_back.frames | 654 | 654, 654, 654 |
| rapid_back.gpu_p50_ms | 6 | 6, 6, 6 |
| rapid_back.gpu_p90_ms | 7 | 7, 7, 7 |
| rapid_back.gpu_p99_ms | 9 | 8, 9, 10 |
| Rapid Back, hitch time per second of motion (ms/s) | 10.78 | 9.25, 12.25, 10.78 |
| rapid_back.janky_after_first_pct | 0.76 | 0.76, 0.76, 0.61 |
| Rapid Back, janky frames (%) | 1.07 | 0.92, 1.07, 1.07 |
| rapid_back.late_first_frames | 2 | 1, 2, 3 |
| rapid_back.p50_ms | 10 | 10, 10, 10 |
| rapid_back.p90_ms | 12 | 11, 12, 12 |
| rapid_back.p99_ms | 15 | 13, 15, 15 |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.98 | 5.76, 10.07, 5.98 |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.63 | 5.55, 5.64, 5.63 |
| rapid_back.transitions | 6 | 6, 6, 6 |
| First visit to a tab, first frame max (ms) | 8.54 | 9.23, 8.16, 8.54 |
| First visit to a tab, first frame median (ms) | 4.02 | 4.02, 4.2, 3.91 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.84 | 7.03, 8.93, 8.84 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.32 | 4.32, 4.08, 6.44 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.02 | ratchet | report |
| First visit to a tab, first frame max (ms) | 8.54 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.32 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.84 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.37 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 15.26 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | — | 1 | not measured |
| Tab switch to a visited tab, first frame median (ms) | — | 8 | not measured |
| Tab switch to a visited tab, first frame max (ms) | — | 16 | not measured |
| Tab switch, hitch time per second of motion (ms/s) | — | ratchet | report |
| Tab switch with plain taps, janky frames (%) | — | 1 | not measured |
| Tab switch with plain taps, first frame median (ms) | — | 8 | not measured |
| Tab switch with plain taps, first frame max (ms) | — | 16 | not measured |
| Push, the frame that composes the screen, median (ms) | 7.28 | ratchet | report |
| Push, the frame that composes the screen, max (ms) | 15.98 | ratchet | report |
| Pop, first frame median (ms) | 2.48 | 8 | pass |
| Pop, first frame max (ms) | 5.44 | 16 | pass |
| Push/pop with the Back key, janky frames (%) | 1.66 | 2 | pass |
| Push/pop with the Back key, p99 frame (ms) | 27 | 25 | fail |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | 2.02 | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.25 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 1.95 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 13.57 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | yes | yes | pass |
| Rapid Back: lands on the expected screen | yes | yes | pass |
| Rapid Back, janky frames (%) | 1.07 | 4 | pass |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | 5.63 | 8 | pass |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | 5.98 | 16 | pass |
| Rapid Back, hitch time per second of motion (ms/s) | 10.78 | ratchet | report |
| Navigation stress: the app is still running | — | yes | not measured |
| Navigation stress: stack invariants hold | — | yes | not measured |
| Navigation stress, janky frames (%) | — | 4 | not measured |
| Process death: tab, stacks and presented sheet restored | — | yes | not measured |
| Process death: Back pops the restored stack | — | yes | not measured |
| Scroll a root list, janky frames (%) | — | 0.5 | not measured |
| Scroll a root list, GPU p90 (ms) | — | 7 | not measured |
| Scroll with the bar minimizing, janky frames (%) | — | 0.5 | not measured |
| Scroll with the bar minimizing, GPU p90 (ms) | — | 7 | not measured |
| Idle 5 s on every tab, frames rendered | — | 0 | not measured |
| Cold start, speed-profile, median (ms) | — | 380 | not measured |
| Cold start as installed (profile from the APK only), median (ms) | — | 400 | not measured |
| PSS after the journey, less the window's graphics buffers (MB) | — | 48 | not measured |
| PSS after the journey (MB) | — | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
