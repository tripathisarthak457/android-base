# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\cardclip\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 35.6 °C at the start
- Runs: 3, seed 586762, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 2 |
| First back swipe after a restart (warm), janky frames | 3 | 4, 3, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.84 | 5.74, 5.86, 5.84 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.1 | 19.9, 6.1, 6.04 |
| First visit to a tab, first frame max (ms) | 9.4 | 12.32, 9.4, 8.64 |
| First visit to a tab, first frame median (ms) | 3.75 | 6.04, 3.75, 3.75 |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.32 | 10.32, 8.11, 10.94 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.57 | 8.07, 3.71, 6.57 |
| tabs.first3_dequeue_ms.max | 0.05 | 0.11, 0.05, 0.05 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.01 | 20.01, 22.82, 19.59 |
| tabs.first3_gpu_ms.median | 9.78 | 9.78, 9.52, 9.83 |
| tabs.first3_issue_ms.max | 4.92 | 5.41, 4.69, 4.92 |
| tabs.first3_issue_ms.median | 1.5 | 1.34, 1.5, 1.66 |
| Tab switch to a visited tab, first frame max (ms) | 9.99 | 9.85, 9.99, 10.01 |
| Tab switch to a visited tab, first frame median (ms) | 4.14 | 4.14, 4.01, 4.14 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 8 | 7, 10, 8 |
| tabs.gpu_p99_ms | 14 | 13, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0.78, 0 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.07, 1.07, 1.07 |
| tabs.late_first_frames | 14 | 14, 14, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 12 | 11, 17, 12 |
| tabs.p99_ms | 20 | 19, 21, 20 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 20.33 | 22.59, 20.33, 18.93 |
| tabs_tap.first3_gpu_ms.median | 9.72 | 9.7, 10.89, 9.72 |
| tabs_tap.first3_issue_ms.max | 5.21 | 5.32, 5.21, 4.93 |
| tabs_tap.first3_issue_ms.median | 1.39 | 1.46, 1.36, 1.39 |
| Tab switch with plain taps, first frame max (ms) | 9.7 | 10.45, 9.7, 9.36 |
| Tab switch with plain taps, first frame median (ms) | 4.07 | 4.12, 4.07, 3.85 |
| tabs_tap.frames | 1305 | 1304, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 7 | 8, 7, 7 |
| tabs_tap.gpu_p99_ms | 14 | 13, 14, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 1.56, 0, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0.08, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1.15, 1.07, 1.07 |
| tabs_tap.late_first_frames | 14 | 14, 14, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 10 | 11, 10, 10 |
| tabs_tap.p99_ms | 20 | 20, 20, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.75 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.4 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.57 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.32 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.84 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.1 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.14 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.99 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.07 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.7 | 16 | pass |
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
