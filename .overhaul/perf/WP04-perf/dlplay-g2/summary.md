# perf.sh: com.example.wp04.dlplay.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\dlplay\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 31.0 °C at the start
- Runs: 3, seed 600470, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: pushpop_gesture

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 3, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.33 | 7.16, 7.37, 7.33 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.17 | 14.43, 7.17, 6.45 |
| pushpop_gesture.first3_dequeue_ms.max | 0.07 | 0.07, 0.06, 0.31 |
| pushpop_gesture.first3_dequeue_ms.median | 0.03 | 0.03, 0.02, 0.03 |
| pushpop_gesture.first3_gpu_ms.max | 18.04 | 18.74, 18.04, 17.84 |
| pushpop_gesture.first3_gpu_ms.median | 10.99 | 10.84, 11.02, 10.99 |
| pushpop_gesture.first3_issue_ms.max | 4.8 | 4.8, 4.7, 4.83 |
| pushpop_gesture.first3_issue_ms.median | 1.59 | 1.59, 1.58, 1.72 |
| pushpop_gesture.frames | 752 | 756, 751, 752 |
| pushpop_gesture.gesture_first_frame_ms.max | 2.77 | 2.99, 2.77, 2.67 |
| Back gesture, slowest of its first three frames, median (ms) | 2.5 | 1.3, 2.55, 2.5 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 15 | 15, 16, 15 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 19.02 | 16.22, 19.09, 19.02 |
| pushpop_gesture.janky_after_first_pct | 0.93 | 0.93, 1.2, 0.66 |
| Push/pop with the back gesture, janky frames (%) | 2.51 | 2.51, 2.8, 2.26 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 25 | 25, 25, 27 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| First visit to a tab, first frame max (ms) | 9.93 | 9.22, 9.93, 10.25 |
| First visit to a tab, first frame median (ms) | 8.13 | 8.62, 8.13, 7.08 |
| First visit to a tab after a restart (warm), first frame max (ms) | 4.22 | 4.11, 10.67, 4.22 |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.92 | 3.92, 6.63, 3.9 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 8.13 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.93 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.92 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 4.22 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.33 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.17 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
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
| Push/pop with the back gesture, janky frames (%) | 2.51 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.5 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 19.02 | ratchet | report |
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
