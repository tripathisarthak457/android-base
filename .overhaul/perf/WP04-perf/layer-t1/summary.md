# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\layer\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 35.7 °C at the start
- Runs: 3, seed 746187, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 3 | 2, 3, 3 |
| First back swipe after a restart (warm), janky frames | 3 | 2, 3, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 12.02 | 12.14, 12.02, 5.63 |
| First push after a restart (warm), the frame that composes the screen (ms) | 8.83 | 15.72, 8.83, 5.96 |
| First visit to a tab, first frame max (ms) | 8.44 | 9.29, 8.09, 8.44 |
| First visit to a tab, first frame median (ms) | 3.83 | 3.61, 4, 3.83 |
| First visit to a tab after a restart (warm), first frame max (ms) | 4.7 | 4.42, 7.25, 4.7 |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.96 | 3.75, 4.33, 3.96 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.05 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 19.02 | 20.21, 18.69, 19.02 |
| tabs.first3_gpu_ms.median | 9.9 | 9.98, 9.9, 9.89 |
| tabs.first3_issue_ms.max | 4.83 | 4.1, 4.97, 4.83 |
| tabs.first3_issue_ms.median | 1.41 | 1.41, 1.53, 1.24 |
| Tab switch to a visited tab, first frame max (ms) | 9.41 | 9.1, 9.41, 9.66 |
| Tab switch to a visited tab, first frame median (ms) | 4.08 | 4.1, 4.08, 3.76 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 7 | 8, 7, 7 |
| tabs.gpu_p99_ms | 14 | 14, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0.79 | 0.79, 0.78, 0.79 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0.08 |
| Tab switch, janky frames (%) | 1.15 | 1.07, 1.15, 1.15 |
| tabs.late_first_frames | 14 | 14, 15, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 11, 10 |
| tabs.p99_ms | 19 | 19, 20, 19 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.05 | 0.06, 0.05, 0.05 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 17.64 | 17.08, 19.29, 17.64 |
| tabs_tap.first3_gpu_ms.median | 10.51 | 10.51, 10.25, 10.58 |
| tabs_tap.first3_issue_ms.max | 4.37 | 4.37, 4.74, 4.1 |
| tabs_tap.first3_issue_ms.median | 1.5 | 1.5, 1.57, 1.29 |
| Tab switch with plain taps, first frame max (ms) | 10.16 | 9.67, 10.16, 11.76 |
| Tab switch with plain taps, first frame median (ms) | 3.92 | 4.06, 3.92, 3.88 |
| tabs_tap.frames | 1304 | 1305, 1304, 1304 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 7, 7, 7 |
| tabs_tap.gpu_p99_ms | 14 | 14, 13, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1.15, 1.15, 1.15 |
| tabs_tap.late_first_frames | 15 | 15, 15, 15 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 10 | 10, 10, 10 |
| tabs_tap.p99_ms | 19 | 19, 19, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.83 | ratchet | report |
| First visit to a tab, first frame max (ms) | 8.44 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 3.96 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 4.7 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 12.02 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 8.83 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.15 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.08 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.41 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0.79 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.15 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 3.92 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 10.16 | 16 | pass |
| Push, the frame that composes the screen, median (ms) | — | ratchet | report |
| Push, the frame that composes the screen, max (ms) | — | ratchet | report |
| Pop, first frame median (ms) | — | 8 | not measured |
| Pop, first frame max (ms) | — | 16 | not measured |
| Push/pop with the Back key, janky frames (%) | — | 2 | not measured |
| Push/pop with the Back key, p99 frame (ms) | — | 25 | not measured |
| Push/pop with the Back key, hitch time per second of motion (ms/s) | — | ratchet | report |
| Push/pop with the back gesture, janky frames (%) | — | 2 | not measured |
| Back gesture, slowest of its first three frames, median (ms) | — | 8 | not measured |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | — | ratchet | report |
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
