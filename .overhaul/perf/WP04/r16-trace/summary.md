# perf.sh: com.example.wp04.everything.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/final-everything/app/build/outputs/apk/dev/benchmarkRelease/app-dev-arm64-v8a-benchmarkRelease.apk`
- Device: M2101K6I (9a7c74f4), battery 31.2 °C at the start
- Runs: 1, seed 506311, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: trace

| Metric | Median | Runs |
|---|---|---|
| First back swipe after pm clear (cold), janky frames | 1 | 1 |
| First back swipe after a restart (warm), janky frames | 1 | 1 |
| First push after pm clear (cold), the frame that composes the screen (ms) | 6.88 | 6.88 |
| First push after a restart (warm), the frame that composes the screen (ms) | 6.95 | 6.95 |
| First visit to a tab, first frame max (ms) | 10.12 | 10.12 |
| First visit to a tab, first frame median (ms) | 8.12 | 8.12 |
| First visit to a tab after a restart (warm), first frame max (ms) | 12.45 | 12.45 |
| First visit to a tab after a restart (warm), first frame median (ms) | 7.9 | 7.9 |
| trace.captured | yes | yes |
