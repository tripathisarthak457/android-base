# perf.sh: com.example.wp04.dlplay.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\dlplay\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.9 °C at the start
- Runs: 3, seed 407969, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 375 | 376, 370, 375 |
| coldstart.runs_ms | [377, 355, 371, 373, 393, 413] | [373, 380, 360, 394, 367, 389], [376, 365, 381, 364, 382, 357], [377, 355, 371, 373, 393, 413] |
| Cold start as installed (profile from the APK only), median (ms) | 376 | 375, 376, 376 |
| coldstart_installed.runs_ms | [381, 364, 376, 377, 371, 416] | [383, 329, 394, 365, 375, 375], [373, 377, 377, 375, 335, 387], [381, 364, 376, 377, 371, 416] |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 2 |
| First back swipe after a restart (warm), janky frames | 3 | 3, 4, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.74 | 15.3, 5.68, 5.74 |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.43 | 7.43, 7.27, 8.51 |
| First visit to a tab, first frame max (ms) | 9.23 | 11.53, 4.28, 9.23 |
| First visit to a tab, first frame median (ms) | 5.87 | 5.87, 3.98, 6.56 |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.34 | 10.34, 10.13, 10.81 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.56 | 3.95, 8.87, 5.56 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 5.87 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.23 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.56 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 10.34 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.74 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 7.43 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 2 | ratchet | report |
| First back swipe after a restart (warm), janky frames | 3 | ratchet | report |
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
| Cold start, speed-profile, median (ms) | 375 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 376 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) | — | 48 | not measured |
| PSS after the journey (MB) | — | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
