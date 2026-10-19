# perf.sh: com.example.wp04.everything.dev

- APK: `D:\projects\tools\.overhaul\out\WP04-perf\bg-c2\app\build\outputs\apk\dev\benchmarkRelease\app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 33.5 °C at the start
- Runs: 3, seed 539360, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: back_during_push

| Metric | Median | Runs |
|---|---|---|
| back_during_push.difference_ratios | [0.97, 0.95, 0.97, 0.95] | [0.97, 0.95, 0.97, 0.95], [0.97, 0.95, 0.97, 0.95], [0.97, 0.95, 0.97, 0.95] |
| Back swipe during a push: lands on the list | yes | yes, yes, yes |
| Back swipe during a push: one opaque card, no double exposure | yes | yes, yes, yes |
| First back swipe after pm clear (cold), janky frames | 2 | 2, 2, 2 |
| First back swipe after a restart (warm), janky frames | 2 | 1, 2, 2 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.22 | 6.12, 6.22, 18.47 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.45 | 6.45, 22.73, 6.21 |
| First visit to a tab, first frame max (ms) | 9.93 | 10.35, 7.9, 9.93 |
| First visit to a tab, first frame median (ms) | 4.47 | 6.76, 4.47, 4.15 |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.68 | 10.04, 4.5, 9.68 |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.53 | 5.59, 4.45, 5.53 |

| Gate `solid` | Value | Limit | Result |
|---|---|---|---|
| First visit to a tab, first frame median (ms) | 4.47 | ratchet | report |
| First visit to a tab, first frame max (ms) | 9.93 | ratchet | report |
| First visit to a tab after a restart (warm), first frame median (ms) | 5.53 | ratchet | report |
| First visit to a tab after a restart (warm), first frame max (ms) | 9.68 | ratchet | report |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.22 | ratchet | report |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.45 | ratchet | report |
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
| Push/pop with the back gesture, janky frames (%) [non-regression] | — | 3.1 | not measured |
| Back gesture, slowest of its first three frames, median (ms) [non-regression] | — | 2.7 | not measured |
| Push/pop with the back gesture, hitch time per second of motion (ms/s) | — | 5 | not measured |
| Push/pop with the back gesture, janky frames other than each motion's first (%) | — | 1 | not measured |
| Push/pop with the back gesture, motions whose first frame missed its deadline (count) | — | ratchet | report |
| Back swipe during a push: one opaque card, no double exposure | yes | yes | pass |
| Back swipe during a push: lands on the list | yes | yes | pass |
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
| PSS after the journey, less the window's graphics buffers (MB) [non-regression] | — | 58.4 | not measured |
| PSS after the journey (MB) | — | ratchet | report |
| Tab switch, frames that recompose (most in one switch) | — | 2 | not measured |
| Push, frames that recompose (most in one push) | — | ratchet | report |
| Pop, frames that recompose (most in one pop) | — | 2 | not measured |
| Back gesture, frames that recompose while the card follows the finger | — | 0 | not measured |
