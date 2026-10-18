# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\layer\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 35.4 °C at the start
- Runs: 3, seed 380866, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: coldstart

| Metric | Median | Runs |
|---|---|---|
| Cold start, speed-profile, median (ms) | 399 | 399, 400, 396 |
| coldstart.runs_ms | [400, 399, 394, 387, 376, 399] | [378, 422, 399, 403, 399, 399], [411, 419, 397, 403, 381, 386], [400, 399, 394, 387, 376, 399] |
| First back swipe after pm clear (cold), janky frames | 3 | 3, 3, 2 |
| First back swipe after a restart (warm), janky frames | 3 | 3, 2, 4 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.05 | 6.73, 7.05, 14.96 |
| First push after a restart (warm), the frame that composes the screen (ms) | 14.78 | 7.15, 14.78, 15.54 |
| First visit to a tab, first frame max (ms) | 9.84 | 10.99, 9.84, 8.3 |
| First visit to a tab, first frame median (ms) | 6.12 | 6.58, 6.12, 3.86 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.25 | 4.17, 9.25, 9.93 |
| First visit to a tab after a restart (warm), first frame median (ms) | 7.31 | 3.84, 7.31, 9.13 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 6.12 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.84 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 7.31 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.25 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 7.05 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 14.78 | ratchet | report |
| First back swipe after pm clear (cold), janky frames | 3 | ratchet | report |
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
| Cold start, speed-profile, median (ms) | 399 | 380 | fail |
| Cold start as installed (profile from the APK only), median (ms) | — | 400 | not measured |
| PSS after the journey, less the window's graphics buffers (MB) | — | 48 | not measured |
| PSS after the journey (MB) | — | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
