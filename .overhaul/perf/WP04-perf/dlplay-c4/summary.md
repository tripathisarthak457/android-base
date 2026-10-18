# perf.sh: com.example.wp04.dlplay.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\dlplay\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 34.7 °C at the start
- Runs: 3, seed 375468, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: coldstart, coldstart_installed

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 371 | 368, 382, 371 |
| coldstart.runs_ms | [371, 364, 372, 394, 370, 399] | [357, 366, 394, 364, 388, 370], [410, 402, 374, 377, 387, 351], [371, 364, 372, 394, 370, 399] |
| Cold start as installed (profile from the APK only), median (ms) | 378 | 386, 378, 371 |
| coldstart_installed.runs_ms | [379, 376, 363, 445, 361, 367] | [401, 359, 406, 351, 382, 390], [382, 372, 367, 424, 376, 380], [379, 376, 363, 445, 361, 367] |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 3, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 2, 2, 4 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.7 | 5.7, 5.63, 12.58 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.01 | 5.97, 6.01, 6.35 |
| First visit to a tab, first frame max (ms) | 7.35 | 7.35, 6.7, 10.48 |
| First visit to a tab, first frame median (ms) | 4.22 | 3.98, 4.22, 8.82 |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.92 | 11.37, 3.84, 8.92 |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.4 | 7.11, 3.63, 6.4 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.22 | ratchet | report |
| First visit to a tab, first frame max (ms) | 7.35 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 6.4 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 8.92 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 5.7 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.01 | ratchet | report |
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
| Cold start, speed-profile, median (ms) | 371 | 380 | pass |
| Cold start as installed (profile from the APK only), median (ms) | 378 | 400 | pass |
| PSS after the journey, less the window's graphics buffers (MB) | — | 48 | not measured |
| PSS after the journey (MB) | — | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
