# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\bg-c1\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.9 °C at the start
- Runs: 3, seed 108266, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: pushpop_gesture, meminfo

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 1 | 1, 1, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.13 | 6.23, 7.17, 7.13 |
| First push after a restart (warm), the frame that composes the screen (ms) | 9.13 | 9.13, 6.4, 12.25 |
| meminfo.graphics_mb | 54.8 | 54.8, 54.7, 54.8 |
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | 60.5 | 60.2, 60.5, 60.7 |
| PSS after the journey (MB) | 115.2 | 115, 115.2, 115.5 |
| pushpop_gesture.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| pushpop_gesture.first3_dequeue_ms.median | 0.03 | 0.02, 0.03, 0.03 |
| pushpop_gesture.first3_gpu_ms.max | 20.2 | 27.08, 17.56, 20.2 |
| pushpop_gesture.first3_gpu_ms.median | 13.73 | 13.73, 13.7, 14.31 |
| pushpop_gesture.first3_issue_ms.max | 5 | 5.12, 4.73, 5 |
| pushpop_gesture.first3_issue_ms.median | 1.81 | 1.5, 1.85, 1.81 |
| pushpop_gesture.frames | 747 | 747, 745, 749 |
| pushpop_gesture.gesture_first_frame_ms.max | 3.59 | 4.11, 3.4, 3.59 |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | 2.21 | 1.71, 2.22, 2.21 |
| pushpop_gesture.gpu_p50_ms | 6 | 6, 6, 6 |
| pushpop_gesture.gpu_p90_ms | 10 | 10, 10, 10 |
| pushpop_gesture.gpu_p99_ms | 16 | 16, 16, 16 |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 6.91 | 8.28, 6.91, 5.51 |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | 0.27 | 0, 0.4, 0.27 |
| Push/pop with the back gesture, janky frames (%) [non-regression] | 1.87 | 1.61, 2.01, 1.87 |
| Push/pop with the back gesture, motions whose first frame missed its deadline (count) | 12 | 12, 12, 12 |
| pushpop_gesture.p50_ms | 9 | 9, 9, 9 |
| pushpop_gesture.p90_ms | 16 | 17, 15, 16 |
| pushpop_gesture.p99_ms | 28 | 28, 28, 29 |
| pushpop_gesture.transitions | 12 | 12, 12, 12 |
| First visit to a tab, first frame max (ms) | 9.55 | 8.87, 10.23, 9.55 |
| First visit to a tab, first frame median (ms) | 6.28 | 6.42, 6.28, 6.21 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.16 | 8.45, 9.19, 9.16 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.17 | 5.17, 4.59, 8.65 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 6.28 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.55 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.17 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.16 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.13 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 9.13 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 1 | ratchet | report |
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
| Push/pop with the back gesture, janky frames (%) [non-regression] | 1.87 | 3.1 | pass |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | 2.21 | 2.7 | pass |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | 6.91 | 5 | fail |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | 0.27 | 1 | pass |
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
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | 60.5 | 58.4 | fail |
| PSS after the journey (MB) | 115.2 | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
