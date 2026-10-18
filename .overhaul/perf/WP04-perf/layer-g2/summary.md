# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\layer\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 34.0 °C at the start
- Runs: 3, seed 368480, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: pushpop_gesture

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 4 | 4, 2, 4 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 3, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.87 | 5.87, 13.05, 5.73 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.03 | 6.82, 19.26, 7.03 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.09, 0.05 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 19.79 | 20.99, 19.79, 18.74 |
| pushpop_gesture.first3_gpu_ms.median | 11.55 | 11.57, 11.55, 10.56 |
| pushpop_gesture.first3_issue_ms.max | 7.09 | 8.27, 7.09, 6.96 |
| pushpop_gesture.first3_issue_ms.median | 1.54 | 1.54, 1.51, 1.57 |
| pushpop_gesture.frames | 750 | 745, 750, 756 |
| pushpop_gesture.gesture_first_frame_ms.max | 2.73 | 4.99, 2.73, 2.71 |
| Back gesture, slowest of its first three frames, median (ms) | 2.31 | 2.31, 2.26, 2.37 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 15.01 | 24.64, 15.01, 8.24 |
| pushpop_gesture.janky_after_first_pct | 0.93 | 1.48, 0.93, 0.66 |
| Push/pop with the back gesture, janky frames (%) | 2.53 | 2.95, 2.53, 2.12 |
| pushpop_gesture.late_first_frames | 11 | 11, 12, 11 |
| pushpop_gesture.p50_ms | 9 | 9, 9, 9 |
| pushpop_gesture.p90_ms | 16 | 16, 17, 15 |
| pushpop_gesture.p99_ms | 28 | 28, 23, 29 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| First visit to a tab, first frame max (ms) | 9.67 | 8.85, 9.76, 9.67 |
| First visit to a tab, first frame median (ms) | 3.85 | 3.85, 3.75, 4.04 |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.35 | 6.9, 10.35, 11.26 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.12 | 4.12, 8.21, 3.78 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.85 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.67 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.12 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.35 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.87 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.03 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 4 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) | — | 1 | not measured |
| Tab switch to a visited tab, first frame median (ms) | — | 8 | not measured |
| Tab switch to a visited tab, first frame max (ms) | — | 16 | not measured |
| Tab switch, hitch time per second of motion (ms/s) | — | ratchet | report |
| Tab switch with plain taps, janky frames (%) | — | 1 | not measured |
| Tab switch with plain taps, first frame median (ms) | — | 8 | not measured |
| Tab switch with plain taps, first frame max (ms) | — | 16 | not measured |
| Push, the frame that composes the screen, median (ms) | — | ratchet | report |
| Push, the frame that composes the screen, max (ms) | — | ratchet | report |
| Pop, first frame median (ms) | — | 8 | not measured |
| Pop, first frame max (ms) | — | 16 | not measured |
| Push/pop with the Back key, janky frames (%) | — | 2 | not measured |
| Push/pop with the Back key, p99 frame (ms) | — | 25 | not measured |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | — | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | 2.53 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.31 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 15.01 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | — | yes | not measured |
| Rapid Back: lands on the expected screen | — | yes | not measured |
| Rapid Back, janky frames (%) | — | 4 | not measured |
| Rapid Back, slowest frame of the coalesced pop, median (ms) | — | 8 | not measured |
| Rapid Back, slowest frame of the coalesced pop, max (ms) | — | 16 | not measured |
| Rapid Back, hitch time per second of motion (ms/s) | — | ratchet | report |
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
