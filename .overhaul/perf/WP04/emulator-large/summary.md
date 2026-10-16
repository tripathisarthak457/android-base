# perf.sh: com.example.wp04.everything.dev

- APK: `/home/crazyhusir/Documents/projects/tools/.overhaul/out/WP04/final-everything/app/build/outputs/apk/dev/benchmarkRelease/app-dev-x86_64-benchmarkRelease.apk`
- Device: sdk_gphone64_x86_64 (emulator-5554), battery 25.0 °C at the start
- Runs: 1, seed 530326, extras {"perf.fakeSession": "true"}, compiled `speed-profile`
- Scenarios: tabs_large, pushpop_large, listdetail_select, scroll_large, sidebar_toggle

| Metric | Median | Runs |
|---|---|---|
| First push after pm clear (cold), the frame that composes the screen (ms) | 4.98 | 4.98 |
| First push after a restart (warm), the frame that composes the screen (ms) | 5.39 | 5.39 |
| listdetail_select.first3_dequeue_ms.max | 10.36 | 10.36 |
| listdetail_select.first3_dequeue_ms.median | 0 | 0 |
| listdetail_select.first3_gpu_ms.max | 29.71 | 29.71 |
| listdetail_select.first3_gpu_ms.median | 12.77 | 12.77 |
| listdetail_select.first3_issue_ms.max | 4.77 | 4.77 |
| listdetail_select.first3_issue_ms.median | 0.96 | 0.96 |
| Large window: the detail swaps in place, first frame max (ms) | 5.35 | 5.35 |
| Large window: the detail swaps in place, first frame median (ms) | 4.87 | 4.87 |
| listdetail_select.frames | 153 | 153 |
| listdetail_select.gpu_p50_ms | 23 | 23 |
| listdetail_select.gpu_p90_ms | 4950 | 4950 |
| listdetail_select.gpu_p99_ms | 4950 | 4950 |
| Large window: a row opens its detail beside the list, janky frames (%) | 19.61 | 19.61 |
| listdetail_select.p50_ms | 29 | 29 |
| listdetail_select.p90_ms | 46 | 46 |
| listdetail_select.p99_ms | 57 | 57 |
| pushpop_large.first3_dequeue_ms.max | 9.81 | 9.81 |
| pushpop_large.first3_dequeue_ms.median | 0 | 0 |
| pushpop_large.first3_gpu_ms.max | 30.48 | 30.48 |
| pushpop_large.first3_gpu_ms.median | 11.92 | 11.92 |
| pushpop_large.first3_issue_ms.max | 3.57 | 3.57 |
| pushpop_large.first3_issue_ms.median | 0.88 | 0.88 |
| pushpop_large.frames | 104 | 104 |
| pushpop_large.gpu_p50_ms | 21 | 21 |
| pushpop_large.gpu_p90_ms | 4950 | 4950 |
| pushpop_large.gpu_p99_ms | 4950 | 4950 |
| Large window: open and close a detail, janky frames (%) | 24.04 | 24.04 |
| pushpop_large.p50_ms | 24 | 24 |
| pushpop_large.p90_ms | 31 | 31 |
| pushpop_large.p99_ms | 34 | 34 |
| Large window: close a detail with Back, first frame max (ms) | 5.3 | 5.3 |
| Large window: close a detail with Back, first frame median (ms) | 3.44 | 3.44 |
| pushpop_large.push_first_frame_ms.max | 4.56 | 4.56 |
| pushpop_large.push_first_frame_ms.median | 4.08 | 4.08 |
| scroll_large.frames | 631 | 631 |
| scroll_large.gpu_p50_ms | 23 | 23 |
| Large window: scroll the list pane, GPU p90 (ms) | 4950 | 4950 |
| scroll_large.gpu_p99_ms | 4950 | 4950 |
| Large window: scroll the list pane, janky frames (%) | 7.61 | 7.61 |
| scroll_large.p50_ms | 28 | 28 |
| scroll_large.p90_ms | 42 | 42 |
| scroll_large.p99_ms | 65 | 65 |
| sidebar_toggle.first3_dequeue_ms.max | 8.89 | 8.89 |
| sidebar_toggle.first3_dequeue_ms.median | 0 | 0 |
| sidebar_toggle.first3_gpu_ms.max | 50.78 | 50.78 |
| sidebar_toggle.first3_gpu_ms.median | 16.03 | 16.03 |
| sidebar_toggle.first3_issue_ms.max | 331.74 | 331.74 |
| sidebar_toggle.first3_issue_ms.median | 1.59 | 1.59 |
| sidebar_toggle.frames | 246 | 246 |
| sidebar_toggle.gpu_p50_ms | 24 | 24 |
| sidebar_toggle.gpu_p90_ms | 4950 | 4950 |
| sidebar_toggle.gpu_p99_ms | 4950 | 4950 |
| Large window: fold and open the sidebar, janky frames (%) | 33.74 | 33.74 |
| sidebar_toggle.p50_ms | 40 | 40 |
| sidebar_toggle.p90_ms | 48 | 48 |
| sidebar_toggle.p99_ms | 61 | 61 |
| First visit to a tab, first frame max (ms) | 1.92 | 1.92 |
| First visit to a tab, first frame median (ms) | 1.72 | 1.72 |
| First visit to a tab after a restart (warm), first frame max (ms) | 2.83 | 2.83 |
| First visit to a tab after a restart (warm), first frame median (ms) | 1.8 | 1.8 |
| tabs_large.first3_dequeue_ms.max | 8.49 | 8.49 |
| tabs_large.first3_dequeue_ms.median | 0 | 0 |
| tabs_large.first3_gpu_ms.max | 31.94 | 31.94 |
| tabs_large.first3_gpu_ms.median | 15.35 | 15.35 |
| tabs_large.first3_issue_ms.max | 5.12 | 5.12 |
| tabs_large.first3_issue_ms.median | 0.65 | 0.65 |
| Large window: tab switch to a visited tab, first frame max (ms) | 1.82 | 1.82 |
| Large window: tab switch to a visited tab, first frame median (ms) | 1.41 | 1.41 |
| tabs_large.frames | 353 | 353 |
| tabs_large.gpu_p50_ms | 24 | 24 |
| tabs_large.gpu_p90_ms | 4950 | 4950 |
| tabs_large.gpu_p99_ms | 4950 | 4950 |
| Large window: tab switch beside the rail or sidebar, janky frames (%) | 30.31 | 30.31 |
| tabs_large.p50_ms | 32 | 32 |
| tabs_large.p90_ms | 53 | 53 |
| tabs_large.p99_ms | 61 | 61 |

Notes:

- run 1: tabs_large: LargexMedium window (1280x800dp), Sidebar
- run 1: pushpop_large: LargexMedium window (1280x800dp), Sidebar
- run 1: listdetail_select: LargexMedium window (1280x800dp), Sidebar
- run 1: scroll_large: LargexMedium window (1280x800dp), Sidebar
- run 1: sidebar_toggle: LargexMedium window (1280x800dp), Sidebar
