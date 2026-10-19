# District phone study: the procedure

Written 2026-10-19 for REQUIREMENTS 45 by the motion and design-system lead (Opus 5.5); revised the same day after
review (safe dim taps, long-press safety, per-window frame timing). **Not run yet.** It answers the questions
`RESEARCH.md` §2.6 lists, so that proposals P1-P10 are decided on measurements of District on your own phone rather
than on guesses.

**When it may run.** All four must hold:

1. the bug fix and staging have freed the phone (REQUIREMENTS 44);
2. you have installed District from the Play Store **and signed in yourself** (the run never signs in, creates an
   account or types anything personal);
3. you have said go in chat, and answered the two questions in §2.1;
4. the never-publish rule of §2.2 is in place.

**What it does.** It films District's sheets, page transitions, feed, headers and back gesture under scripted touches,
takes frame timings, and measures each motion frame by frame (position per frame, fitted curve, overshoot, whether the
finger's speed is kept). Then it films the template's catalog and sample doing the same things, for a side-by-side.
About two hours of phone time, one agent holding the adb lock, plus an hour of analysis off the phone.

---

## 1. The rules: the run only watches

**Never:**

- decompile, extract or pull District's APK or any of its files (`pm path` + `adb pull` is forbidden), read its data
  folders, or copy its images, icons, fonts, animations or text into the template or anywhere public;
- sign in, create an account, enter or change any personal data, or grant or deny a permission dialog for you. If any
  system dialog or sign-in screen appears: stop the scene, press nothing, and ask you;
- tap anything that buys, books, pays, reserves or holds (seats, tables, tickets, passes), adds to a cart, applies an
  offer or coupon, redeems, gifts, follows, likes, marks "interested", sets a reminder, rates, reviews, shares, sends,
  invites, calls, changes the location or the profile, or logs out. That includes **showtimes**: tapping one may start
  a booking and hold seats, so the run never taps a showtime or a seat map;
- open the profile, tickets, bookings, wallet, payments or gift-card screens (they show your personal data);
- change any setting, system or app (theme, animation scale, refresh rate, navigation mode, developer options, "show
  taps", battery saver), or force-stop District (you may be in the middle of something in it);
- capture District's logcat.

**Allowed (browsing only):** switching District's main tabs; opening a movie, event or restaurant page from a list;
opening a sheet or picker that only shows choices (filters, sort, language or format) and closing it again by the
dim, Back or a drag, **never** by "Apply", "Done", "Continue" or any confirm button; scrolling; swiping between detail
pages; the system back gesture; reading frame timings (`dumpsys gfxinfo`), the package's version (`dumpsys package`),
the display mode, the thermal status and the read-only settings of §2.3.

**Taps: only at points a tool has checked in a fresh UI dump.** No tap ever goes to a coordinate typed by hand or
read off a recording. `tools/safe_target.py` gives the point and refuses (exit 2) anything doubtful:

- **A labelled control** (`point_label`, §3.1): two UI dumps about two seconds apart, the label found in both. The
  target is the nearest clickable at or above the label, and the point is the label's own centre kept inside that
  target. Refused when anything inside the target carries a word from the deny list (book, buy, pay, proceed,
  confirm, seat, ticket, apply, done, continue, offer, share, interested, location, profile, login, showtime and
  about 60 more) or a time of day (`10:30 AM`, `22.15`, `7 pm`: a showtime); when any other clickable contains the
  point (a scrim or a bar over it, a clickable around it, a control inside it); when the label names a sheet's
  scrim; and when the two dumps differ (a banner or carousel that moves by itself). The run never targets an item of
  a banner or an auto-advancing carousel in any case.
- **The dim above an open sheet** (`point_dim`, §3.1): two fresh dumps must each show a scrim (Views'
  `touch_outside`, or a full-window clickable labelled like "Close sheet", as Material 3's Compose sheet and the
  template label theirs) and a sheet container drawn above it. The point is the window's horizontal centre, halfway
  between the status bar's bottom and 48 dp above the sheet's top (the highest top of anything drawn above the
  scrim). Refused when that band is under 48 dp (a tall sheet), when anything drawn above the scrim contains the
  point, or when the sheet moved between the dumps. A refused dim closes the sheet with **Back**, but only after a
  third dump shows a sheet is still open. When a dump shows no sheet at all, **nothing is pressed**: Back would leave
  the page, and a tap high on a page lands on the location picker, profile, share or language controls.

A refusal of a labelled tap ends the scripted part of that step: either you tap that one control by hand while the
recording runs, or the step is skipped.

**Drags: never a long press, never a hidden tap.**

- A finger that stays within touch slop for the long-press timeout (400 ms by default; read in §2.3) fires whatever
  long-press action lies under it. Each `input motionevent` starts its own process (often a hundred ms or more), so
  every scripted drag makes its **first move at least three times the touch slop** (24 dp, 66 px) away from where it
  went down, and the pre-flight (§4.0) measures the gap between a DOWN and the first MOVE on the catalog and
  requires it under half the timeout before any drag runs on District. A drag that has left slop may hold still
  afterwards: the long press is already cancelled.
- A drag that stays inside a clickable is a tap on lift, in Compose and in Views alike, unless a surface beneath the
  finger takes the drag (a sheet, a list, a pager, the system back gesture). So every scripted drag starts on a
  surface that moves along the drag's axis: a sheet's grabber band or header, a list, a pager. The catalog pre-flight
  confirms the surface moves on the first MOVE.
- There is **no scripted press-and-hold on District**: the press scene (P1) is yours to do by hand, or skipped.

**Privacy of what is recorded.** Recordings and UI dumps can show your name, city and history. They stay on this PC in
`research/district/phone/`, which is excluded from the public workspace backup (§2.2), are never committed or pushed,
and are never sent anywhere. Device copies are deleted right after each pull. Only numbers and our own words go into
`PHONE-RESULTS.md`.

---

## 2. Before the run

### 2.1 Questions for you (in chat)

1. **Which District sheets do you mean when you say you love its bottom sheet, and how do you reach them?** (For
   example a filter sheet in Movies, a language or format chooser on a movie page, a sheet on a restaurant page.) The
   run studies those first. If one sits inside a booking flow, you open it by hand and the run only drags it from its
   grabber band and closes it (`close_sheet`).
2. **May the run read District's window and view class names** (`dumpsys window`, `dumpsys activity top`, §6)? It
   tells whether a sheet is a separate dialog window and whether screens are Compose or Views. It reads names the app
   exposes at runtime; nothing is extracted. Optional.

### 2.2 Workspace and phone (no system changes)

- Add the never-publish rule **before the first file is written**: in `.overhaul/handover/backup-include.txt`,
  section 1, the line `- /research/district/phone/` (as was done for the Gmail recordings). `*.mp4` and `f_*.png` are
  already excluded everywhere; this also keeps screenshots, UI dumps and timings of District out of the public branch.
- Folders: `research/district/phone/{rec,timing,ui,tracks,fits,checks}/`.
- The phone: `locked adb adb devices` lists `9a7c74f4`; screen on, unlocked, at the home screen, not charging hot.
- The template apps: the catalog **release** build from staging (`pm list packages | grep -i catalog` gives its id;
  `com.base.app.catalog` in the template) and the staging sample app that `perf.sh` uses for push and pop (its package
  is in `staging/STAGING-REPORT.md`). Release builds only: debug builds are several times jankier. If they are not
  installed, ask; this run does not build.
- Tools: `ffmpeg` and `ffprobe` (inside `locked`), Python 3.15, and this folder's `tools/` (standard library only):

| Tool | Does |
|---|---|
| `safe_target.py` | the tap point for a label, or for the dim above an open sheet, from two UI dumps; refuses doubtful points (§1) |
| `edge_track.py` | position of an edge (a sheet's top, a page's leading edge) in every frame |
| `scroll_track.py` | scroll position of a list from frame-to-frame shift (flings, overscroll) |
| `patch_luma.py` | grey level of a patch per frame, and the dim's alpha from it |
| `fit_motion.py` | t50/t90/t99, settle time, overshoot, carry of the finger's speed, best spring and best tween, decay rate; compares trials |
| `frame_summary.py` | per window: janky %, frame p50/p90/p99, GPU p90, hitch ms/s, refresh interval; each window's first frame with its flags, the opening frame and tap-to-present (§7.3) |

  The tracking and fitting tools were checked on synthetic recordings and tracks (720 × 1600 at 120 fps, 10 % of
  frames dropped, ±0.6 px noise), on travels over 100 dp: a critically damped spring at k 341.5 came back as k
  331-339, Material's standard spring (ζ 0.9, k 700) as ζ 0.90, k 700-708, a 300 ms FastOutSlowIn as a 300 ms tween
  with its speed dropped (carry 0.12-0.31), an iOS 0.998/ms coast as 0.9980. Short, fast settles (under 60 dp) gave
  stiffness 10-45 % off, hence the 100 dp rule in §7.2. `safe_target.py` was checked on synthetic dumps shaped like a
  Material 3 Compose sheet (own window), a Views `BottomSheetDialog`, the template's in-window sheet, a sheet at
  Large, a page with no sheet, a card holding a showtime, and a screen that changed between dumps; `frame_summary.py`
  on a synthetic two-window dump whose sheet window's first frame carries the window-changed flag.

### 2.3 Device facts, read once (read-only)

```bash
export MSYS_NO_PATHCONV=1; A="adb -s 9a7c74f4"
$A shell getprop ro.build.fingerprint
$A shell wm size; $A shell wm density
$A shell dumpsys display | grep -iE "renderFrameRate|refreshRate|mActiveModeId" | head -8
$A shell dumpsys thermalservice | grep -i "Thermal Status"
$A shell settings get global low_power                 # 1 = battery saver on: stop and tell the user
$A shell settings get global animator_duration_scale   # anything but 1 or empty: stop and tell the user
$A shell settings get secure long_press_timeout        # null = the default 400 ms
$A shell settings get secure navigation_mode           # 2 = gestures; 0 or 1 = buttons, so S4 and T3 are skipped
$A shell dumpsys window | grep -m1 -oE "type=statusBars frame=\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"
$A shell dumpsys package com.application.zomato.district | grep -E "versionName|versionCode|targetSdk|minSdk"
```

Write them to `phone/device.txt`. Battery saver, a non-default animation scale or a hot phone changes every timing:
the run stops and you decide (it never changes them itself). The status bar's bottom edge is the second number of the
frame's second corner; if the line is missing, `point_dim` is never used and sheets are closed with Back.

---

## 3. Recording and timing settings

- **Recording**: `screenrecord --size 720x1600 --bit-rate 20000000 --time-limit <s> /data/local/tmp/ds-<scene>.mp4`,
  pulled to `phone/rec/`, then deleted on the phone. 720 × 1600 is exactly two thirds of 1080 × 2400, so 1 recorded px
  is 1.5 device px, and **1 dp = 1.8333 recorded px** (density 2.75). screenrecord writes a variable frame rate:
  always use frame times (`ffprobe`), never frame numbers. Extract with `-fps_mode passthrough`.
- **Timing**: `dumpsys gfxinfo <package> reset`, the gesture, wait 1.5 s, then `dumpsys gfxinfo <package>
  framestats` to `phone/timing/`. HWUI keeps each window's last 120 frames, about 1 s at 120 Hz, so one dump covers
  one gesture. **A dump holds one block per window**: District's activity, and its sheet's own dialog window if the
  sheet is one (Material 3's `ModalBottomSheet` and Views' `BottomSheetDialog` both are; the template's sheets are
  not). The windows are read separately (§7.3). The summary lines above the blocks cover the whole process since the
  reset. If District draws with something other than HWUI, the dump has no rows: record that, it is itself a finding.
- **Video in the window.** A trailer drawn through a `TextureView` adds a frame to the activity's series for every
  video frame; one in a `SurfaceView` adds none. Time sheet scenes on screens without an auto-playing trailer where
  you have the choice, and mark any timing taken while one played.
- **Separate passes.** Recording costs the GPU and encoder, so timing runs are made with nothing recording, and the
  recordings are never used for jank numbers.
- **Repeats**: three runs of every timed gesture; three recorded runs of every gesture whose curve is fitted.
- **Display rate**: whatever the phone runs (the run never changes it). `frame_summary.py` reports each window's frame
  interval; if District runs at 60 Hz while the catalog runs at 120 Hz, say so beside every comparison.

### 3.1 Reference shell functions

Run as `D:/projects/tools/.overhaul/bin/locked adb bash phone/study.sh`, with these at the top of `study.sh`. Positions
given to the drag helpers are thousandths of the screen, so the same call drives District and the catalog; they come
from a still of the screen in question (§4.1 S13), never from a guess. **Every helper runs on the catalog first
(§4.0).**

```bash
#!/usr/bin/env bash
set -uo pipefail
export MSYS_NO_PATHCONV=1
S=9a7c74f4; A="adb -s $S"
D=com.application.zomato.district
OUT=D:/projects/tools/.overhaul/research/district/phone
T=D:/projects/tools/.overhaul/research/district/tools
read -r W H < <($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{s=(o!=""?o:p); split(s,a,"x"); print a[1], a[2]}')
DPI=$($A shell wm density | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
DENS=$(awk "BEGIN{print $DPI / 160}")
SLOP3=$(( 24 * DPI / 160 ))                                # 3 x the 8 dp touch slop, in px (66 at 440 dpi)
SB=$($A shell dumpsys window | tr -d '\r' | grep -m1 -oE 'type=statusBars frame=\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]' \
     | sed -E 's/.*\]\[[0-9]+,([0-9]+)\]$/\1/')             # the status bar's bottom edge; empty = unknown
LPT=$($A shell settings get secure long_press_timeout | tr -d '\r'); [[ "$LPT" =~ ^[0-9]+$ ]] || LPT=400
px() { echo $(( $1 * $2 / 1000 )); }                       # px <thousandths> <W or H>

ui() {                                                     # ui <name>: the screen's UI tree, kept locally
  $A shell uiautomator dump /data/local/tmp/ds-ui.xml >/dev/null 2>&1
  $A pull /data/local/tmp/ds-ui.xml "$OUT/ui/$1.xml" >/dev/null 2>&1
  $A shell rm -f /data/local/tmp/ds-ui.xml
}
point_label() {                                            # point_label <label> [index]: "x y", or non-zero = no tap
  local name; name=$(echo "$1" | tr -c 'A-Za-z0-9' '_')
  ui "${name}_a"; sleep 2; ui "${name}_b"
  python -I "$T/safe_target.py" "$OUT/ui/${name}_a.xml" "$1" --index "${2:-0}" --again "$OUT/ui/${name}_b.xml"
}
point_dim() {                                              # point_dim: "x y"; 2 = sheet open, dim unsafe; 3 = no sheet
  [ -n "$SB" ] || return 2
  ui dim_a; sleep 2; ui dim_b
  python -I "$T/safe_target.py" "$OUT/ui/dim_a.xml" --dim --again "$OUT/ui/dim_b.xml" --status-bottom "$SB" --density "$DENS"
}
still_open() {                                             # a fresh dump still shows a modal sheet
  ui still
  python -I "$T/safe_target.py" "$OUT/ui/still.xml" --dim --status-bottom "${SB:-0}" --density "$DENS" >/dev/null 2>&1
  case $? in 0|2) return 0 ;; *) return 1 ;; esac
}
tap_label() { local xy; xy=$(point_label "$@") || return 2; $A shell input tap $xy; }
close_sheet() {                                            # the dim when safe; else Back while a sheet is still open
  local xy code; xy=$(point_dim); code=$?
  case $code in
    0) $A shell input tap $xy ;;
    2) if still_open; then back_key; else echo "close_sheet: no sheet open now, nothing pressed" >&2; return 3; fi ;;
    *) echo "close_sheet: no sheet open (code $code), nothing pressed" >&2; return 3 ;;
  esac
}
rec() {                                                    # rec <scene> <seconds> <command...>
  local name=$1 secs=$2; shift 2
  $A shell rm -f /data/local/tmp/ds-$name.mp4
  $A shell screenrecord --size 720x1600 --bit-rate 20000000 --time-limit "$secs" /data/local/tmp/ds-$name.mp4 &
  local pid=$!; sleep 1.0
  "$@"
  wait $pid; sleep 0.3
  $A pull /data/local/tmp/ds-$name.mp4 "$OUT/rec/$name.mp4" >/dev/null && $A shell rm -f /data/local/tmp/ds-$name.mp4
}
timing() {                                                 # timing <scene> <package> <command...>
  local name=$1 pkg=$2; shift 2
  $A shell dumpsys gfxinfo "$pkg" reset >/dev/null
  "$@"; sleep 1.5
  $A shell dumpsys gfxinfo "$pkg" framestats > "$OUT/timing/$name.txt"
}
swipe() {                                                  # swipe <x> <y1> <y2> <ms>: one process, constant speed
  $A shell input swipe "$(px "$1" "$W")" "$(px "$2" "$H")" "$(px "$1" "$W")" "$(px "$3" "$H")" "$4"
}
hswipe() {                                                 # hswipe <y> <x1> <x2> <ms>
  $A shell input swipe "$(px "$2" "$W")" "$(px "$1" "$H")" "$(px "$3" "$W")" "$(px "$1" "$H")" "$4"
}
drag() {                                                   # drag <x1> <y1> <x2> <y2> <hold s> (px): leaves slop at once
  local x1=$1 y1=$2 x2=$3 y2=$4 hold=$5 dx dy len fx fy m i
  dx=$(( x2 - x1 )); dy=$(( y2 - y1 )); len=$(awk "BEGIN{print int(sqrt($dx * $dx + $dy * $dy))}")
  if [ "$len" -lt $(( 2 * SLOP3 )) ]; then echo "drag: $len px is too short to leave slop at once" >&2; return 2; fi
  fx=$(( x1 + dx * SLOP3 / len )); fy=$(( y1 + dy * SLOP3 / len ))
  m="input motionevent DOWN $x1 $y1; input motionevent MOVE $fx $fy"
  for i in $(seq 1 23); do m="$m; input motionevent MOVE $(( fx + (x2 - fx) * i / 23 )) $(( fy + (y2 - fy) * i / 23 ))"; done
  $A shell "$m; sleep $hold; input motionevent UP $x2 $y2"
}
hold_release() { drag "$(px "$1" "$W")" "$(px "$2" "$H")" "$(px "$1" "$W")" "$(px "$3" "$H")" 0.6; }  # <x> <y1> <y2>
hhold_release() { drag "$(px "$2" "$W")" "$(px "$1" "$H")" "$(px "$3" "$W")" "$(px "$1" "$H")" 0.6; } # <y> <x1> <x2>
open_and_catch() {                                         # open_and_catch "<tap x y>" <x> <y>: tap, then at once a drag
  local tx ty x y; read -r tx ty <<< "$1"; x=$(px "$2" "$W"); y=$(px "$3" "$H")
  $A shell "input tap $tx $ty; input motionevent DOWN $x $y; input motionevent MOVE $x $(( y + SLOP3 ));" \
    "input motionevent MOVE $x $(( y + SLOP3 + 40 )); input motionevent MOVE $x $(( y + SLOP3 + 80 ));" \
    "input motionevent MOVE $x $(( y + SLOP3 + 120 )); input motionevent UP $x $(( y + SLOP3 + 120 ))"
}
press_cancel() {                                           # CATALOG ONLY: hold 0.5 s, slide off, lift (no click)
  local x y; x=$(px "$1" "$W"); y=$(px "$2" "$H")
  $A shell "input motionevent DOWN $x $y; sleep 0.5; input motionevent MOVE $x $((y + 90)); input motionevent MOVE $x $((y + 180)); input motionevent UP $x $((y + 180))"
}
back_key() { $A shell input keyevent 4; }
back_swipe() {                                             # back_swipe commit|cancel: edge swipe to 45 % (gestures only)
  local y far first m i last
  y=$((H / 2)); far=$((W * 45 / 100)); first=$((2 + SLOP3))
  m="input motionevent DOWN 2 $y; input motionevent MOVE $first $y"
  for i in $(seq 1 23); do m="$m; input motionevent MOVE $(( first + (far - first) * i / 23 )) $y"; done
  m="$m; sleep 0.8"; last=$far
  if [ "$1" = cancel ]; then for i in $(seq 22 -1 0); do m="$m; input motionevent MOVE $(( 2 + (far - 2) * i / 23 )) $y"; done; last=2; fi
  $A shell "$m; input motionevent UP $last $y"
}
back_flick() { $A shell input swipe 2 $((H / 2)) $((W * 60 / 100)) $((H / 2)) 120; }
gap_check() {                                              # gap_check <x> <y> (px): ms from a DOWN to its first MOVE
  local i
  for i in 1 2 3 4 5; do
    $A shell "input motionevent DOWN $1 $2; a=\$(cat /proc/uptime); input motionevent MOVE $1 $(( $2 + SLOP3 ));" \
      "b=\$(cat /proc/uptime); input motionevent MOVE $1 $(( $2 + 2 * SLOP3 )); input motionevent UP $1 $(( $2 + 2 * SLOP3 )); echo \$a \$b" \
      | tr -d '\r' | awk '{ printf "%d\n", ($3 - $1) * 1000 }'
    sleep 1.5
  done
}
```

`point_label`, `point_dim` and the decisions on their exit codes run **before** a recording or timing starts; the
recorded command is only the tap at the point they printed (`rec S1_r1 6 $A shell input tap $xy`), so a refusal
never happens mid-recording.

For a step you make by hand (a refused tap, the press scene), the agent runs `rec <scene> 8 sleep 6`, tells you in chat
to act once the recording has started, and notes in `PHONE-RESULTS.md` that the touch was a person's, not a script's.

`input swipe` moves the injected finger in a straight line at constant speed and lifts at the end, all in one process,
so it leaves touch slop within milliseconds and its release speed is distance ÷ duration: over 40 % of the height
(960 px), 300 ms is about 1160 dp/s, 150 ms about 2330 dp/s and 80 ms about 4360 dp/s. `fit_motion.py --swipe-ms <ms>`
finds the lift where the track leaves that straight line. `drag` sends one event per process instead, which is why it
moves 3 × slop at once and ends with a still hold: its lift carries no speed.

---

## 4. Scenes

Before each recorded scene, let the screen settle for 2 s. Names below are the scene ids used in file names
(`S1_r1.mp4`, `S1_r1.txt`).

### 4.0 Pre-flight on the catalog (before anything touches District)

On the catalog's "Sheets" section, "Detents" → "Basic" and "Filters":

1. **Gap**: open a sheet, then `gap_check` on its grabber band (from a still). The largest of the five gaps must be
   under half the long-press timeout (`LPT / 2`, 200 ms by default). If it is not, no `drag`, `open_and_catch` or
   `back_swipe` runs on District: S5, S6, S9, S11, H2's slow drag, T4's slow drag, F2's pull, S4 and T3's slow
   swipes are done by hand or skipped, and only the one-process `input swipe` scenes run.
2. **First move**: record `hold_release` on the sheet's grabber band; in the recording the sheet must start moving
   with the first MOVE. If it waits for the second, the phone's slop is larger than 8 dp: stop and ask.
3. **Dim**: with the sheet at Medium, `point_dim` must print a point above the sheet and `close_sheet` must close it;
   at Large it must refuse (exit 2) and `close_sheet` must close it with Back; with no sheet open it must exit 3 and
   press nothing.
4. **Labels**: `point_label` on a catalog row must print its point; with a sheet open, the same label must be refused
   (the scrim covers it).

Record the results in `phone/checks/preflight.txt`. Any failure stops the run.

### 4.1 Sheets (the main part): for each sheet you named, and one with a scrolling list inside

Open a sheet with `xy=$(point_label "<its trigger's label>")` then the tap; reach the screen with `tap_label` or by
hand where it refuses. **After every scene, close the sheet with `close_sheet`** (the dim when `point_dim` finds a safe
point, otherwise Back while a fresh dump still shows the sheet, otherwise nothing), never with a confirm button.

| Id | Gesture | Recorded | Timed | What it answers |
|---|---|---|---|---|
| S1 | open it with one tap on its trigger (point from `point_label` before the recording) | 3 | 3 | open curve (spring or tween, t90, overshoot); whether the surface slides, fades or grows out of the trigger (P10); the sheet window's first frame or our opening frame, and tap-to-present (§7.3) → P2, P3, P10 |
| S2 | close by tapping the dim: `xy=$(point_dim)` with the sheet at rest, then `rec S2_rN 6 $A shell input tap $xy` | 3 | 3 | drag-free close curve; dim alpha per frame (`patch_luma.py` on a flat patch the sheet never covers) → P1, P4. Exit 2: close with Back instead and file the run under S3; exit 3: the sheet is not open, so re-open it and try once more |
| S3 | close with the Back key (`back_key`) | 3 | 3 | the same for Back → P1 |
| S4 | predictive back: `back_swipe cancel`, then `back_swipe commit` (gesture navigation only) | 2 + 2 | — | how far the sheet shrinks or moves under the gesture, and the commit motion |
| S5 | `hold_release` from the grabber band down by 30 % of the sheet's height (at least 2 × 3 × slop, 132 px) | 3 | — | the snap back from rest: its curve and duration |
| S6 | `hold_release` from the grabber band down by 70 % | 2 | — | whether position alone closes it, and where the line is |
| S7 | fling down from the grabber band with `swipe` at 300, 150 and 80 ms over 25 % of the height | 3 × 3 | 1 × 3 | dismiss or next stop; **carry** of the finger's speed; whether duration or stiffness stays fixed across speeds |
| S8 | if a taller stop exists: the same flings up | 3 × 3 | — | expansion; overshoot at the top |
| S9 | at the top stop, `hold_release` from the grabber band up past the top, then the still lift | 2 | — | rubber band or stretch, and the return |
| S10 | inner list: at a partial stop, `swipe` the list up slowly (2000 ms); then `swipe` it down hard (80 ms) to its top; then `swipe` down from the list's top | 2 each | 1 | does the sheet grow before the list scrolls; can a list fling move the sheet; hand-off from list to sheet |
| S11 | `open_and_catch "$xy" <x> <y>`: the opening tap, then at once (one process start later, 100-300 ms) a DOWN at the grabber's x, 12 % of the height below the sheet's resting top (from S13), a first MOVE of 3 × slop down, three more, and the lift. No hold anywhere | 2 | — | does a touch catch a sheet mid-open, and does the drag take over from where it was. The DOWN lands on the moving sheet, or on the dim if the sheet is still lower; it moves past slop at once, so it is never a press, and on the sheet the sheet takes the drag. A DOWN that landed on the dim closes the sheet like a tap on it (dim tap closes are allowed); that run is filed as such |
| S12 | (from S5/S6 recordings) dim against position | — | — | does the dim follow the finger or switch by state |
| S13 | (from a still frame at rest) | — | — | corner radius, side inset, whether the screen behind shrinks (card stack), the grabber band's position for S5-S11 |

Drags and flings of a sheet start on its grabber band or header (positions from the S13 still), never on a list or a
control inside it, except in S10, which is about the list. In a sheet you opened by hand inside a booking flow, the run
does only S5-S7 from the grabber band and closes it with `close_sheet`.

### 4.2 Screen transitions

| Id | Gesture | Recorded | Timed | Answers |
|---|---|---|---|---|
| T1 | open a movie or event page from a list (`point_label` on its title; refused when the card holds a "Book" button, a showtime or any other clickable over the title: then you tap the poster by hand) | 3 | 3 | push curve; opening frame cost |
| T2 | Back key from that page | 3 | 3 | pop curve |
| T3 | `back_swipe cancel`, `back_swipe commit`, `back_flick` (gesture navigation only) | 2 each | 1 each | whether the page shrinks into a card (as Gmail) or uses the system animation |
| T4 | on a movie page, swipe to the next movie: `hswipe <y> 850 150 <ms>` at 300, 150 and 80 ms, with `<y>` over the poster or trailer (from a still: not over a horizontal list such as the cast), then `hhold_release <y> 850 550` (a slow drag of 30 % released still) | 3 × 3 + 2 | 1 × 3 | page landing time and curve (O1); when the trailer and the text arrive after landing → P6 |
| T5 | switch between District's main tabs (`tap_label` on each tab's label) | 3 | 3 | tab change motion and cost |

### 4.3 Feed, headers, press, loading

| Id | Gesture | Recorded | Timed | Answers |
|---|---|---|---|---|
| F1 | home feed: `swipe` up at 300, 150 and 80 ms over 40 % | 3 × 3 | 1 × 3 | deceleration (`scroll_track.py`, then `fit_motion.py --decay`): rate per ms against iOS's 0.998 |
| F2 | at the top, `hold_release` down 25 % on the list; then `swipe` down into the top at 80 ms | 2 + 2 | — | overscroll: Android stretch, iOS bounce, or none |
| F3 | a horizontal carousel: `hswipe` at 150 ms (a swipe, never a tap on its items) | 3 | 1 | carousel snap |
| F4 | (optional) a list whose centred card is emphasised, if Android has one: `swipe` at 300 ms | 2 | — | → P9 |
| H1 | home top: `swipe 500 700 300 2000` (slow, steady), pause, then the reverse | 2 | 1 | does the header fold into tabs, and is it tied to the scroll (positions proportional) or triggered (moves on its own) → P7 |
| H2 | movie page with the panel over the trailer: `hold_release` from the panel's grabber up 30 %; a `swipe` up at 150 ms; a `swipe` down at 150 ms | 2 each | 1 (marked: trailer playing) | sheet (snaps to stops) or scroll (coasts); where it stops; does the trailer dim → P5 |
| P1 | press on a card and slide off: **by hand on District** (or skipped); `press_cancel` on the catalog only | 3 | — | press scale or dim, and its release |
| P2 | open a page that loads (first visit) | 2 | — | skeleton style (sweep, pulse, none) and when content replaces it relative to the move's end |

### 4.4 Order on the phone

Pre-flight on the catalog (about 10 min), the District recording pass for S, T, F, H, P (about 45 min), then the
timing pass (about 20 min), with District reaching each screen the same way each time. Back to the home screen at the
end (`input keyevent 3`).

---

## 5. The same scenes on the template

Same gestures, same thousandths, same theme as District (dark; add light runs of S1, S2 for completeness), motion style
Standard, and Snappy once for S1 and S2.

| District scene | Template app and screen |
|---|---|
| S1-S13 | catalog, `am start -n <catalog id>/com.base.app.catalog.CatalogActivity --es section Sheets --es theme dark --es motion Standard`: the "Detents" group ("Basic", "Filters" for two stops and a list inside), "Places" for a non-modal sheet, "Now playing" for artwork under the grabber |
| T1-T3 | the staging sample app through `tools/perf/record.sh <package> pushpop|back_slow_commit|back_slow_cancel|back_fast_flick <out>` and `perf.sh` timing rows, so they match the release gates |
| T4 | catalog "Bars and pages" pager, and "Collections" carousel (no full-bleed detail pager exists yet: say so) |
| T5 | sample app tab switches (`record.sh … tab_switch`) |
| F1-F3 | catalog "Lists" and "Animation" (deceleration, rubber band); "Collections" carousel |
| H1 | catalog "Bars and pages" large title |
| P1 | catalog "Press and motion" (`press_cancel`) |
| P2 | catalog "Loading screen" |

The catalog's `--es section` names come from `CatalogSection` (`Sheets`, `Bars`, `Lists`, `Motion`, `Feel`,
`Loading`, `Collections`); `--es theme`, `--es motion` from `CatalogEnvironment`.

---

## 6. Optional: what District is built with (only with your yes to question 2)

```bash
$A shell dumpsys window windows | grep -cE "com.application.zomato.district"   # with a sheet open, then closed
$A shell dumpsys activity top | grep -m 20 -E "AndroidComposeView|RecyclerView|FlutterView|ReactRootView|ViewPager|CoordinatorLayout"
```

One more window while a sheet is open means the sheet is its own dialog window (as Material's Compose sheet and Views'
`BottomSheetDialog` both are; our sheets never are, SHEET-SPEC D-22). `AndroidComposeView` with few child views means
Compose; widget classes mean Views; `FlutterView` or `ReactRootView` mean those. Output goes to `phone/` only. Without
your yes, the timing dumps still show it: a second block that appears with the sheet is its window (§7.3).

---

## 7. Measuring

Every command in this section runs from `research/district/phone/`, with
`T=D:/projects/tools/.overhaul/research/district/tools`.

### 7.1 Find what to track

For each recording, look at three frames (the start, mid-motion, the end) as small crops, then choose the strip:

```bash
ffprobe -v error -select_streams v:0 -show_entries frame=best_effort_timestamp_time -of csv=p=0 rec/S1_r1.mp4 | head
ffmpeg -v error -ss 0.80 -i rec/S1_r1.mp4 -frames:v 1 -vf "scale=360:-1" checks/S1_r1_t080.png
```

- **A sheet**: a strip (`--strip`, fractions of the width) through the sheet's top edge away from the grabber and from
  text, for example `0.70:0.80`; `--polarity rise` when the sheet is lighter than what is above it, `fall` when darker,
  `any` when unsure; `--start` its resting y in recorded px. District is dark: if the top edge is faint, track a strong
  edge inside the sheet (its title row or a button's top), which moves rigidly with it.
- **A page swipe**: `--axis x` and a horizontal strip (`--strip 0.30:0.40` of the height) through an edge of the page.
- **A feed**: `scroll_track.py` with `--window` covering only rows that scroll rigidly (no status bar, sticky header,
  tab bar or bottom bar).
- **A dim**: `patch_luma.py --rect` on a flat area the sheet never covers; `--scrim 0` for a black scrim.

```bash
python -I $T/edge_track.py rec/S1_r1.mp4 --strip 0.70:0.80 --polarity any --start 1500 > tracks/S1_r1.csv
python -I $T/patch_luma.py rec/S2_r1.mp4 --rect 0.05:0.12:0.30:0.06 --scrim 0 > tracks/S2_r1_dim.csv
python -I $T/scroll_track.py rec/F1_150_r1.mp4 --window 0.14:0.86 > tracks/F1_150_r1.csv
```

**Check every track**: take the tracked position at five times and crop ±40 px around it
(`ffmpeg -ss <t> -i … -frames:v 1 -vf "crop=720:80:0:<y-40>"`); the edge must sit in the middle of each crop. A track
that jumps to another edge is redone with a narrower `--max-step`, another strip or a `--window`.

### 7.2 Fit

```bash
python -I $T/fit_motion.py tracks/S1_r1.csv tracks/S1_r2.csv tracks/S1_r3.csv          # from rest
python -I $T/fit_motion.py tracks/S7_150_r1.csv tracks/S7_150_r2.csv tracks/S7_150_r3.csv --swipe-ms 150 \
    --json > fits/S7_150.json                                                            # one --swipe-ms per call
python -I $T/fit_motion.py tracks/F1_150_r1.csv --swipe-ms 150 --decay
```

(Run the flings of one speed together, since `--swipe-ms` applies to every file in a call.) Read:

- **Duration**: t50, t90, t99 and the time to settle within 1 dp, from the release (a tap's motion starts at rest).
  Each is read in 8.3 ms frame steps, so District's values are compared only with the template's values measured the
  same way (a critically damped k 341.5, t90 210 ms on paper, reads 217-225 ms), as medians of three runs.
- **Overshoot**: dp and percent of travel, and crossings of the rest position.
- **Damping and stiffness**: the best spring's ζ and k (and the same as iOS response and template bounce), with its
  RMS error. For orientation: ours `sheet` ζ 1, k 341.5; `dialog` k 522; Material standard spatial ζ 0.9, k 700,
  expressive ζ 0.8, k 380, fast effects ζ 1, k 3800.
- **Spring or tween**: one curve rarely tells a critically damped spring from a quintic ease-out; the trials do. A
  spring keeps k across release speeds; a fixed tween keeps its duration; `ViewDragHelper` (Views sheets) shortens its
  duration as the release speed grows, up to 600 ms.
- **Release speed kept**: `carry` near 1 (≥ 0.6) means the finger's speed went into the motion; near 0 (≤ 0.35) means
  it restarted from rest. It needs release speeds of about 800 dp/s or more (the 150 and 80 ms flings).
- **Fling coast**: `--decay` gives the share of speed kept per ms (iOS normal 0.998) and the coast distance.

Use trials whose travel is at least 100 dp for stiffness; short, fast settles are dominated by the release speed.

### 7.3 Frame timings, per window

```bash
python -I $T/frame_summary.py timing/S1_r1.txt timing/S1_r2.txt timing/S1_r3.txt
```

`frame_summary.py` keeps each window apart and reports, per window: the regular frames' (Flags 0) janky %, frame-time
p50/p90/p99, GPU p90, hitch ms/s and frame interval; the window's **first frame with its flags**; the **opening frame**
(the slowest UI-thread frame of its first three, flagged ones included); and **tap-to-present**.

- **District, sheet in its own window**: the window that appeared during the dump has the window-changed flag on its
  first frame. That frame composed the sheet: its UI time and its tap-to-present (from the vsync of the activity's
  last frame that handled input) are District's opening numbers. Any Flags-0 statistic leaves exactly that frame out,
  which is why the template's `parse_framestats.py` (it merges windows and drops flagged rows) is **not** used on
  District.
- **District, sheet in the activity's window, and the template**: the activity window's opening frame and its
  tap-to-present.
- `RESEARCH.md` §2.6's P3 row compares these opening numbers only. Jank and hitch are compared per window, never
  pooled across windows, and a window with a playing trailer is marked (§3).

For our own runs, the template's parser gives perf.sh's opening frame as a cross-check. It lives on `staging` once
staging exists, and on `wp/WP04` until then:

```bash
mkdir -p lib
git -C D:/projects/tools/android-base show staging:templates/android/tools/perf/parse_framestats.py > lib/parse_framestats.py 2>/dev/null \
  || git -C D:/projects/tools/android-base show wp/WP04:templates/android/tools/perf/parse_framestats.py > lib/parse_framestats.py
python -I -c "import sys,json; sys.path.insert(0,'lib'); import parse_framestats as p; r=p.load(open('timing/S1_tpl_r1.txt').read()); print(json.dumps({'opening_ui_ms': p.ui_ms(p.opening_frame(r)), 'first': p.first_frame(r)}))"
```

(Run with `MSYS_NO_PATHCONV=1`, as the whole study script is.)

---

## 8. Output

- `research/district/phone/`: recordings, crops, UI dumps, raw timings, tracks and fits. **Local only, never
  committed, never pushed.**
- `research/district/PHONE-RESULTS.md`: numbers and verdicts only, no images:
  - the device facts and District's version;
  - the pre-flight results (gap, first move, dim and label checks);
  - one table per scene group, District beside the template (Standard; Snappy where run), with t50/t90/t99, overshoot,
    fitted spring or tween, carry, and per window janky %, p90/p99, hitch, first frame, opening frame and
    tap-to-present;
  - the answers to each row of `RESEARCH.md` §2.6, and so which of P1-P10 the measurements support, change or drop;
  - what could not be measured, and why (a refused tap, a refused dim closed with Back, a faint edge, a scene District
    does not have, a scene done by hand).
- Then the proposals are asked in chat one at a time (pros, cons, catches, the recommended option first), with the
  measured numbers in each question.

## 9. Stop conditions

Stop the run, press nothing more, and ask in chat if: a sign-in, payment, permission or consent screen appears; a
confirmation dialog appears (for example after Back); a tap lands somewhere unexpected (a booking, cart or checkout
screen, a seat map); District shows your profile, tickets or payment details; `close_sheet` reports no sheet when one
should be open, until a fresh dump shows where the run is; the pre-flight fails; the phone gets hot (thermal status 2
or more) or battery saver turns on; or a step needs a tap that `safe_target.py` refuses and you are not there to make
it.
