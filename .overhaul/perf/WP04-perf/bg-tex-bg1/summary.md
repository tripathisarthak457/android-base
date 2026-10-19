# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\bg-tex\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 34.4 °C at the start
- Runs: 3, seed 495915, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: pushpop_gesture, meminfo

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 8.18 | 6.52, 8.18, 14.98 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.68 | 6.76, 15.12, 7.68 |
| meminfo.graphics_mb | 54.8 | 54.8, 54.8, 54.8 |
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | 59.5 | 59.2, 59.5, 59.6 |
| PSS after the journey (MB) | 114.3 | 114, 114.3, 114.4 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| pushpop_gesture.first3_gpu_ms.max | 20.01 | 21.05, 19.57, 20.01 |
| pushpop_gesture.first3_gpu_ms.median | 14.3 | 14.36, 13.57, 14.3 |
| pushpop_gesture.first3_issue_ms.max | 8.28 | 8.28, 6.24, 9.4 |
| pushpop_gesture.first3_issue_ms.median | 1.71 | 1.62, 1.78, 1.71 |
| pushpop_gesture.frames | 746 | 747, 745, 746 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.23 | 3.23, 4.08, 2.71 |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | 2.59 | 2.67, 2.59, 2.43 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 16 | 16, 17, 16 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 11.03 | 8.26, 19.2, 11.03 |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | 0.4 | 0.27, 0.94, 0.4 |
| Push/pop with the back gesture, janky frames (%) [non-regression] | 2.01 | 1.87, 2.55, 2.01 |
| Push/pop with the back gesture, motions whose first frame missed its deadline (count) | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 9 | 9, 9, 9 |
| pushpop_gesture.p90_ms | 17 | 14, 17, 17 |
| pushpop_gesture.p99_ms | 26 | 26, 29, 26 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| First visit to a tab, first frame max (ms) | 4.47 | 3.91, 9.53, 4.47 |
| First visit to a tab, first frame median (ms) | 3.78 | 3.78, 3.67, 4.18 |
| First visit to a tab after a restart (warm), first frame max (ms) | 6.85 | 9.11, 4.38, 6.85 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.45 | 6.46, 4.22, 4.45 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.78 | ratchet | report |
| First visit to a tab, first frame max (ms) | 4.47 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.45 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 6.85 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 8.18 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.68 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 2 | ratchet | report |
| Tab switch, janky frames (%) [non-regression] | — | 1.26 | not measured |
| Tab switch to a visited tab, first frame median (ms) [non-regression] | — | 7.6 | not measured |
| Tab switch to a visited tab, first frame max (ms) [non-regression] | — | 11.9 | not measured |
| Tab switch, hitch time per second of motion (ms/s) | — | 5 | not measured |
| Tab switch, janky frames other than each motion's first (%) | — | 0.5 | not measured |
| Tab switch, motions whose first frame missed its deadline (count) | — | ratchet | report |
| Tab switch with plain taps, janky frames (%) [non-regression] | — | 1.26 | not measured |
| Tab switch with plain taps, first frame median (ms) [non-regression] | — | 4.5 | not measured |
| Tab switch with plain taps, first frame max (ms) [non-regression] | — | 11.7 | not measured |
| Tab switch with plain taps, hitch time per second of motion (ms/s) | — | 5 | not measured |
| Tab switch with plain taps, janky frames other than each motion's first (%) | — | 0.5 | not measured |
| Tab switch with plain taps, motions whose first frame missed its deadline (count) | — | ratchet | report |
| Push, the frame that composes the screen, median (ms) [non-regression] | — | 14.4 | not measured |
| Push, the frame that composes the screen, max (ms) [non-regression] | — | 18.3 | not measured |
| Pop, first frame median (ms) [non-regression] | — | 4.1 | not measured |
| Pop, first frame max (ms) [non-regression] | — | 5.9 | not measured |
| Push/pop with the Back key, janky frames (%) [non-regression] | — | 1.95 | not measured |
| Push/pop with the Back key, p99 frame (ms) [non-regression] | — | 35 | not measured |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | — | 5 | not measured |
| Push/pop with the Back key, janky frames other than each motion's first (%) | — | 0.5 | not measured |
| Push/pop with the Back key, motions whose first frame missed its deadline (count) | — | ratchet | report |
| Push/pop with the back gesture, janky frames (%) [non-regression] | 2.01 | 3.1 | pass |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | 2.59 | 2.7 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 11.03 | 5 | fail |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | 0.4 | 1 | pass |
| Push/pop with the back gesture, motions whose first frame missed its deadline (count) | 12 | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | — | yes | not measured |
| Back swipe during a push: lands on the list | — | yes | not measured |
| Rapid Back: the app is still running | — | yes | not measured |
| Rapid Back: lands on the expected screen | — | yes | not measured |
| Rapid Back, janky frames (%) [non-regression] | — | 1.18 | not measured |
| Rapid Back, slowest frame of the coalesced pop, median (ms) [non-regression] | — | 6.2 | not measured |
| Rapid Back, slowest frame of the coalesced pop, max (ms) [non-regression] | — | 7.9 | not measured |
| Rapid Back, hitch time per second of motion (ms/s) | — | 5 | not measured |
| Rapid Back, janky frames other than each motion's first (%) | — | 1 | not measured |
| Rapid Back, motions whose first frame missed its deadline (count) | — | ratchet | report |
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
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | 59.5 | 58.4 | fail |
| PSS after the journey (MB) | 114.3 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
