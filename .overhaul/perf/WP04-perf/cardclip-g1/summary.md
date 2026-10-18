# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\cardclip\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.8 °C at the start
- Runs: 3, seed 829279, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: pushpop_gesture

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 3 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.98 | 8.14, 5.98, 5.88 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.2 | 6.2, 6.18, 6.53 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.15 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 22.08 | 26.42, 18.62, 22.08 |
| pushpop_gesture.first3_gpu_ms.median | 11.95 | 11.95, 10.91, 13.41 |
| pushpop_gesture.first3_issue_ms.max | 7.55 | 7.66, 7.07, 7.55 |
| pushpop_gesture.first3_issue_ms.median | 1.65 | 1.6, 2.2, 1.65 |
| pushpop_gesture.frames | 748 | 744, 755, 748 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.2 | 3.2, 3.39, 3.17 |
| Back gesture, slowest of its first three frames, median (ms) | 2.68 | 3.12, 1.41, 2.68 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 16 | 16, 15, 17 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 19.15 | 28.46, 13.63, 19.15 |
| pushpop_gesture.janky_after_first_pct | 1.07 | 1.21, 0.53, 1.07 |
| Push/pop with the back gesture, janky frames (%) | 2.67 | 2.82, 2.12, 2.67 |
| pushpop_gesture.late_first_frames | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 10 | 10, 10, 10 |
| pushpop_gesture.p90_ms | 17 | 17, 17, 17 |
| pushpop_gesture.p99_ms | 26 | 27, 24, 26 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| First visit to a tab, first frame max (ms) | 8.73 | 4.3, 8.73, 10.08 |
| First visit to a tab, first frame median (ms) | 3.75 | 4.15, 3.75, 3.71 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.11 | 7.35, 9.46, 9.11 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.29 | 4.29, 8.75, 3.96 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.75 | ratchet | report |
| First visit to a tab, first frame max (ms) | 8.73 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.29 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.11 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.98 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.2 | ratchet | report |
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
| Push/pop with the back gesture, janky frames (%) | 2.67 | 2 | fail |
| Back gesture, slowest of its first three frames, median (ms) | 2.68 | 8 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 19.15 | ratchet | report |
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
