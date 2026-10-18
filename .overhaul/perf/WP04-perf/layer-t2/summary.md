# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\layer\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 35.9 °C at the start
- Runs: 3, seed 28889, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 3 | 3, 3, 4 |
| First back swipe after a restart (warm), janky frames | 3 | 3, 2, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.95 | 5.85, 11.79, 5.95 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.21 | 6.04, 6.21, 13.43 |
| First visit to a tab, first frame max (ms) | 8.51 | 8.51, 6.56, 9.34 |
| First visit to a tab, first frame median (ms) | 3.98 | 3.94, 4.17, 3.98 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.67 | 9.67, 8.39, 9.73 |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.42 | 3.66, 4.42, 9.1 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.06, 0.06, 0.05 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.25 | 23.69, 18.55, 20.25 |
| tabs.first3_gpu_ms.median | 10.25 | 10.39, 10.25, 9.89 |
| tabs.first3_issue_ms.max | 5.16 | 4.35, 5.16, 5.16 |
| tabs.first3_issue_ms.median | 1.43 | 1.56, 1.43, 1.3 |
| Tab switch to a visited tab, first frame max (ms) | 9.66 | 10.04, 9.34, 9.66 |
| Tab switch to a visited tab, first frame median (ms) | 4.09 | 4.09, 4.13, 4.09 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 8 | 8, 7, 8 |
| tabs.gpu_p99_ms | 14 | 14, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0.78, 0, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.07, 1, 1.07 |
| tabs.late_first_frames | 14 | 14, 13, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 11, 11, 11 |
| tabs.p99_ms | 19 | 20, 19, 19 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.14 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 20.36 | 20.36, 21.31, 18.12 |
| tabs_tap.first3_gpu_ms.median | 10.27 | 10.72, 10.07, 10.27 |
| tabs_tap.first3_issue_ms.max | 4.42 | 4.86, 4.33, 4.42 |
| tabs_tap.first3_issue_ms.median | 1.28 | 1.28, 1.22, 1.59 |
| Tab switch with plain taps, first frame max (ms) | 10.12 | 10.13, 9.74, 10.12 |
| Tab switch with plain taps, first frame median (ms) | 4.04 | 8.11, 3.56, 4.04 |
| tabs_tap.frames | 1305 | 1304, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 9, 7, 7 |
| tabs_tap.gpu_p99_ms | 14 | 14, 13, 14 |
| tabs_tap.hitch_ms_per_s | 0.78 | 0.78, 0.78, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0.08, 0 |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1.07, 1, 1.07 |
| tabs_tap.late_first_frames | 14 | 14, 12, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 13, 10, 11 |
| tabs_tap.p99_ms | 20 | 21, 20, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.98 | ratchet | report |
| First visit to a tab, first frame max (ms) | 8.51 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 4.42 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.67 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.95 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.21 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.09 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.66 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.04 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 10.12 | 16 | pass |
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
