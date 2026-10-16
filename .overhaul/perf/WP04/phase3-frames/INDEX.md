# WP04 phase 3 recordings

Recorded 2026-10-12 13:54–14:06 IST on the Redmi Note 10 Pro (9a7c74f4, 120 Hz) by `out/WP04/phone-r11b.sh`
(log `out/WP04/logs/r11-phone3.log`) with `templates/android/tools/perf/record.sh` (`screenrecord --size 720x1600`),
from `out/WP04/final-everything` (`devBenchmarkRelease`, speed-profile compiled; templates as of `14dccc2`) and
`final-standard` (`devRelease`) for four tabs. Each folder has the video, a frame strip of the moving frames (at most
64, so a long clip's strip can miss its end: read `frames/`), `frames.json` and the frames. No clip has a near-blank
frame.

| Folder (under `perf/WP04/`) | Journey | Moving frames | What it shows |
|---|---|---|---|
| `phase3-frames/push` | tap a row | 59 | `cp1_edge.py … push --resp 0.344`: the first moving frame is 3.6 % of travel, the clock trails the frames by 12–25 ms (it starts after the composing frame) |
| `phase3-frames/pop`, `pushpop` | Back-key pop; push then pop | 54, 113 | parallax, drawn scrim and edge shadow; the bar stays put |
| `phase3-frames/rapid_back` | three Back presses at depth 4 | 100 | the first pop, a hold while it reaches 99 %, one coalesced pop to the root |
| `phase3-frames/scroll`, `bar_minimize` | fling the root list; scroll down then up with `perf.barMinimize=OnScrollDown` | 154, 308 | content runs under the floating bar; the bar folds and opens on `thumb` |
| `predictive-back/back_slow_commit`, `back_slow_commit_right`, `dark/back_slow_commit` | slow edge swipe, held, released | 122, 126, 121 | the Gmail card: shrinks to 0.90 toward the far edge (the right-edge swipe mirrors it), 28 dp corners, the list under a 0.32 scrim; the commit un-shrinks and fades in 100 ms, then the list is on top |
| `predictive-back/back_slow_cancel` | slow swipe that returns to the edge | 139 | the card follows the progress back to rest, the scrim fades |
| `predictive-back/back_fast_flick`, `gesture_pop` | quick swipes | 31, 43 | the commit never returns toward rest before the pop |
| `predictive-back/back_to_home` | Back key to the root, then a slow swipe there | 159 | no in-app handler at the start tab's root: the system's back-to-home animation (window card, then the launcher), frames 110–150 |
| `predictive-back/back_y_follow` | swipe drifting down | 0 | `screenrecord` kept one frame; re-recorded by `out/WP04/phone-r11c.sh` |
| `back-during-push/left`, `right` | tap a row, swipe 60–120 ms later | 59, 58 | REQ 32: the card takes over from where the sliding detail is, the list springs to rest beneath it, every frame opaque until the 100 ms commit fade |
| `tab-switch/light`, `dark` | tap every tab in turn | 126, 145 | nudge and unveil on the incoming tab only (12 dp, veil 0.6 → 0 on `thumb`), the two-edge indicator stretching toward the target, the icon dip |
| `tab-switch/four-tabs` | the same on `standard` (four tabs) | 75 | as above |
| `tab-switch/rtl`, `predictive-back/rtl`, `back-during-push/rtl` | the app locale set to `ar-XB` | — | **not valid**: the layout stayed left to right (the locale did not take); `phone-r11c.sh` re-records them after checking the configuration |

Still to record: 2× font tab switch, a cold first visit to a tab, RTL (above), and a sheet over a tab root (no
template feature has a sheet destination until WP03's `SheetScene` lands).
