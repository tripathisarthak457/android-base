# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\cardclip\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 36.0 °C at the start
- Runs: 3, seed 23746, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs, tabs_tap

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 3 | 3, 3, 2 |
| First back swipe after a restart (warm), janky frames | 3 | 3, 2, 3 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.84 | 5.84, 5.78, 12.5 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.22 | 5.89, 6.37, 6.22 |
| First visit to a tab, first frame max (ms) | 9.46 | 9.71, 9.46, 4.27 |
| First visit to a tab, first frame median (ms) | 3.62 | 0.97, 9.2, 3.62 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.94 | 9.94, 4.46, 10.36 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.21 | 7.89, 3.91, 6.21 |
| tabs.first3_dequeue_ms.max | 0.06 | 0.11, 0.06, 0.05 |
| tabs.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs.first3_gpu_ms.max | 20.45 | 20.45, 19.09, 22.6 |
| tabs.first3_gpu_ms.median | 9.55 | 10.32, 9.55, 9.53 |
| tabs.first3_issue_ms.max | 4.78 | 4.53, 5.19, 4.78 |
| tabs.first3_issue_ms.median | 1.45 | 1.45, 1.46, 1.43 |
| Tab switch to a visited tab, first frame max (ms) | 9.3 | 9.23, 9.57, 9.3 |
| Tab switch to a visited tab, first frame median (ms) | 4.17 | 8, 4.17, 4.09 |
| tabs.frames | 1305 | 1305, 1305, 1305 |
| tabs.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs.gpu_p90_ms | 7 | 9, 7, 7 |
| tabs.gpu_p99_ms | 14 | 14, 14, 14 |
| Tab switch, hitch time per second of motion (ms/s) | 0 | 0, 0, 0.79 |
| tabs.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch, janky frames (%) | 1.07 | 1.07, 1.07, 1.07 |
| tabs.late_first_frames | 14 | 14, 14, 14 |
| tabs.p50_ms | 8 | 8, 8, 8 |
| tabs.p90_ms | 11 | 15, 11, 10 |
| tabs.p99_ms | 19 | 20, 19, 19 |
| tabs.transitions | 15 | 15, 15, 15 |
| tabs_tap.first3_dequeue_ms.max | 0.06 | 0.06, 0.05, 0.06 |
| tabs_tap.first3_dequeue_ms.median | 0.02 | 0.02, 0.02, 0.02 |
| tabs_tap.first3_gpu_ms.max | 18.22 | 16.98, 18.22, 18.97 |
| tabs_tap.first3_gpu_ms.median | 10.59 | 10.79, 10.59, 9.55 |
| tabs_tap.first3_issue_ms.max | 5.09 | 5.09, 4.37, 5.68 |
| tabs_tap.first3_issue_ms.median | 1.44 | 1.34, 1.47, 1.44 |
| Tab switch with plain taps, first frame max (ms) | 9.65 | 9.95, 9.65, 9.43 |
| Tab switch with plain taps, first frame median (ms) | 4.04 | 3.95, 4.05, 4.04 |
| tabs_tap.frames | 1305 | 1305, 1305, 1305 |
| tabs_tap.gpu_p50_ms | 5 | 5, 5, 5 |
| tabs_tap.gpu_p90_ms | 8 | 8, 8, 9 |
| tabs_tap.gpu_p99_ms | 13 | 13, 13, 14 |
| tabs_tap.hitch_ms_per_s | 0 | 0, 0.79, 0 |
| tabs_tap.janky_after_first_pct | 0 | 0, 0, 0 |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1.07, 1.07, 1.07 |
| tabs_tap.late_first_frames | 14 | 14, 14, 14 |
| tabs_tap.p50_ms | 8 | 8, 8, 8 |
| tabs_tap.p90_ms | 11 | 11, 11, 14 |
| tabs_tap.p99_ms | 20 | 20, 20, 19 |
| tabs_tap.transitions | 15 | 15, 15, 15 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 3.62 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.46 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.21 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.94 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.84 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.22 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
| Tab switch, janky frames (%) | 1.07 | 1 | fail |
| Tab switch to a visited tab, first frame median (ms) | 4.17 | 8 | pass |
| Tab switch to a visited tab, first frame max (ms) | 9.3 | 16 | pass |
| Tab switch, hitch time per second of motion (ms/s) | 0 | ratchet | report |
| Tab switch with plain taps, janky frames (%) | 1.07 | 1 | fail |
| Tab switch with plain taps, first frame median (ms) | 4.04 | 8 | pass |
| Tab switch with plain taps, first frame max (ms) | 9.65 | 16 | pass |
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
