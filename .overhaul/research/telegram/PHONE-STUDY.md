# Telegram phone study: the procedure

Written 2026-10-20 for REQUIREMENTS 48 by the motion and performance lead (Opus 5.5); revision 2 of the same day.
**Not run yet.** It measures Telegram for Android on your Redmi so that `RESEARCH.md`'s proposals (and its §2.8
questions) rest on numbers from your own phone, and it films the template doing the same things for a side-by-side.
Its method is the District study's (`research/district/PHONE-STUDY.md`): scripted touches, screen recordings, `gfxinfo`
frame statistics per window, and curve fits compared with the template's catalog measured the same way.

What differs is privacy. Telegram is a messenger, so the run records **no one's messages and no one's names, yours
included, with one exception that you choose and approve**: the posts of one public channel you pick (question 1),
because a long scroll and a profile opening need real content. Pick one with author signatures and comments switched
off and few forwards, so its posts carry no person's name; anything that shows one anyway is deleted (§1.5, §9).

**What revision 2 changed** (after an independent review): Do Not Disturb is now a precondition, checked before every
pass; the keyboard scenes wait until your clipboard holds nothing personal; every pop, swipe-back and tab swipe first
proves, with a check that never leaves the phone, that Network Usage is in front; the channel is named as the one
exception; the sheet scenes now expect what Telegram's source does (a short drag snaps back, a long one dismisses); the
profile scenes are PR1-PR4 so they do not read as proposals; the chip deny list comes from `SOURCES.md` §4.1, since the
downloaded source goes to the Recycle Bin after the research.

**When it may run.** All six must hold:

1. the bug fix and staging have freed the phone (REQUIREMENTS 44);
2. you have installed Telegram **and signed in yourself** (the run never signs in or types anything);
3. you have said go in chat and answered the questions in §2.1;
4. the never-publish rule of §2.2 is in place;
5. you have named the public channel and the empty test chat the run may use (§2.1);
6. **Do Not Disturb is on, set by you**, in a mode that silences Telegram's notifications; the run only reads it
   (`settings get global zen_mode`, §3.1 `dnd_on`) before each pass and stops when it reads 0 or nothing.

**What it does.** About 90 minutes of phone time under one adb lock, plus an hour of analysis off the phone: Settings
sub-screens for push, pop, swipe back, predictive back and a tab pager; the Language list and a public channel's long
history for scrolling; the channel's profile for Telegram's custom open; the keyboard and a popup menu in your empty
test chat; optionally one sheet you name. Then the same scenes on the template's catalog and sample.

---

## 1. The rules: the run only watches, and records no one

### 1.1 Never

- Send, type, paste, react, vote, forward, share, reply, pin, edit, delete, clear, report or block anything; join,
  leave, mute, unmute or subscribe to anything; call; start a bot; open a link, a button inside a post, comments or a
  discussion; open stories.
- Change any setting, in Telegram or in Android: Power Saving switches, language, theme, night mode, auto-download,
  data usage ("Reset statistics" included), notifications, privacy, animation scale, refresh rate, navigation mode,
  Do Not Disturb, the keyboard's settings, battery saver. Never force-stop Telegram or clear its data. Settings the
  study needs (Do Not Disturb, the keyboard's clipboard) are yours to set; the run only reads Do Not Disturb.
- Pull, decompile or copy Telegram's APK, files, images, stickers, fonts or text (`pm path` and `adb pull` of the app
  are forbidden); read its data folders; capture its logcat.
- Long-press anything by script (a long press opens actions; the one long-press scene is done by your hand, or
  skipped).
- Touch the attach button (it shows your own photos) or the emoji and sticker panel.

### 1.2 Where the run may look, and where it never does

| May record and dump | Never records, dumps or screenshots |
|---|---|
| Data and Storage, Network Usage and its tabs | the chat list, the stories row, the top Settings screen (it shows your name, number, username and photo) |
| the Language list (scrolled, never tapped) | any private chat, group or person's profile; Contacts, Calls, Saved Messages (unless it is the test chat you name) |
| Power Saving, once, if you open it yourself (to read which effects are on) | Devices, Privacy and its exceptions, Storage Usage (it lists chats), Notifications |
| **the one exception**: the public channel you choose, its posts, its profile, its shared media | share sheets, the attach sheet, notification banners and the notification shade |
| your empty test chat: its header (the title you gave it), its keyboard with nothing in its suggestion strip, its ⋮ menu | anything reached by a tap the run did not plan; a keyboard strip that shows text |

**A pop shows the screen beneath, so a scene is recorded only when both screens are on the left column.** For
example, Network Usage → Back reveals Data and Storage (allowed); Data and Storage → Back would reveal the top Settings
screen (never recorded). Getting to and from the allowed screens through the top Settings screen or the chat list is
**done by your hand or with the recording off**, and no UI dump is ever pulled from there.

**Every pop, swipe-back, predictive back and tab swipe on the Settings sub-screens is preceded by `on_network_usage`**
(§3.1), outside any recording: a UI dump taken on the phone, searched on the phone for Network Usage's own tab names,
and deleted on the phone, so nothing from a wrong screen ever reaches the PC. If it fails, the run stops: an unexpected
commit (a T3b that crossed the line) or a missed re-push would otherwise make the next pop reveal and record the top
Settings screen.

### 1.3 Taps: only allow-listed labels at points a tool checked in two fresh dumps

`tools/tg_target.py` (this folder) builds on District's `safe_target.py`:

- **A label must be on its allow-list** (`network usage`, the Network Usage tab names `all`, `mobile`, `wi-fi`,
  `wifi`, `roaming`, `more options`, `go to bottom`, `scroll to bottom`) **or on a line of
  `phone/checks/allow-extra.txt`**, which the run writes from your answers only (the channel's title, a sheet's
  trigger), and a node must carry it exactly.
- **Refused** when the target or anything in it is checkable, a switch, a check box, a radio button, a slider or a text
  field (they change a setting or a draft); when anything in it says a word of the deny list (send, join, mute, share,
  delete, reset, language, notifications, privacy, comment, open, link, premium and about 90 more, District's list
  included) or a time of day (posts carry one); when another clickable contains the point; and when two dumps taken two
  seconds apart differ.
- **The message field** (`--field`): exactly one text field labelled "Message", and it must be empty. A field holding
  text means a draft exists: the run stops and asks.
- Exit 2 is a refusal: you tap that control by hand (with the recording off, unless the scene is the tap itself), or
  the step is skipped.
- Tested here on synthetic dumps: a plain row accepted; a toggle row, a row showing a time, a row naming "share", a
  label off the list, a scrim over the row and a screen that moved between dumps all refused; an empty field accepted
  and a field with text refused.

Sheets, menus and the keyboard are **closed with the Back key only**, and only after a fresh check shows they are open
(Telegram's dims carry no label the District tool's `--dim` mode can verify).

### 1.4 Drags

As in District §1: every scripted drag leaves touch slop on its first move (3 × slop, 66 px), so it can never become a
long press; it starts on a surface that moves along its axis (a list, a pager, a page being swiped back, a profile
header, a sheet's top band); the pre-flight checks the gap between a DOWN and its first MOVE on the catalog. A drag must
be at least two first moves long (132 px), District's `drag` refuses a shorter one. A sideways swipe-back starts at
30 % of the width, never at the edge, so the system gesture does not take it (the predictive-back scenes start at the
edge on purpose).

### 1.5 Keeping and deleting

- Everything goes to `research/telegram/phone/`, excluded from the public backup (§2.2), never committed, pushed or
  sent anywhere. Device copies are deleted right after each pull (`adb shell rm` of the run's own files in
  `/data/local/tmp`).
- **A recording or dump that shows a notification banner, a person's name (a channel post's signature, a "forwarded
  from" person, a commenter's avatar), a message outside the chosen channel, or text in the keyboard's suggestion strip
  is moved to the Recycle Bin at once**, and the scene is redone only once the cause is gone (otherwise skipped).
- When `PHONE-RESULTS.md` is written, the recordings, extracted frames, crops and UI dumps go to the **Recycle Bin**
  (your rule: never a permanent delete), the same day:

  ```powershell
  Add-Type -AssemblyName Microsoft.VisualBasic
  foreach ($d in 'rec', 'frames', 'checks\crops', 'ui') {
      $p = "D:\projects\tools\.overhaul\research\telegram\phone\$d"
      if (Test-Path $p) { [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory($p, 'OnlyErrorDialogs', 'SendToRecycleBin') }
  }
  ```

  Timing dumps, tracks and fits hold numbers and window class names only; they stay until the proposals are decided,
  then go the same way. Only numbers and our own words go into `PHONE-RESULTS.md`; no images.

---

## 2. Before the run

### 2.1 Questions for you (in chat)

1. **The public channel, and its exception.** Which one? Prefer one with **author signatures off, comments off**, few
   or no forwards from people, a long history and photos. Its posts are the one place the run records messages (public
   ones, which you picked); do you accept that exception to REQUIREMENTS 48's "records no one's messages or names"?
   Will you open the channel yourself before its scenes, or may the run open a `t.me` link you give with
   `adb shell am start -a android.intent.action.VIEW -d https://t.me/<name>` (opening it does not join it)?
2. **The empty test chat.** Which chat (no messages, a title that names no one, for example a group with only you
   called "Test")? The run only opens its keyboard and its ⋮ menu there.
3. **The Telegram motions you love most** (push, swipe back, predictive back, a long channel scroll, the keyboard,
   opening a profile, photos, a sheet): those are studied first.
4. **Do Not Disturb (required).** Please turn it on yourself before the run, in a mode that silences Telegram (total
   silence, alarms only, or priority with no message exceptions), and turn it off yourself afterwards. The run reads
   `zen_mode` before each pass and stops if it is off; it never changes it.
5. **Your keyboard (for K1 and K2).** Before the keyboard scenes, please copy a harmless word (for example "test") so
   the clipboard holds nothing personal, or turn off your keyboard's clipboard suggestions yourself. If your keyboard
   shows suggested words on an empty field (they can include names), turn its suggestion strip off yourself for the
   run, or the keyboard scenes are skipped. Which will you do, and which word did you copy?
6. **Power Saving.** Will you open Settings → Power Saving yourself once, so the run can take one UI dump there and read
   which of Telegram's effects are on for your phone (deleted after reading)? Optional.
7. **A sheet** (optional): one Telegram sheet you like that only shows choices, reached without passing a screen with
   names. Otherwise no Telegram sheet scenes.
8. **Window and view class names** (optional): may the run read `dumpsys activity top` while Telegram is in front, to
   name its windows and views (§6)?

### 2.2 Workspace and phone (no system changes)

- **The never-publish rule first**, before any file is written: in `.overhaul/handover/backup-include.txt`, section 1,
  the line `- /research/telegram/phone/`, beside the existing `- /research/telegram/src/`.
- Folders: `research/telegram/phone/{rec,frames,timing,ui,tracks,fits,checks}/`.
- The phone: `D:/projects/tools/.overhaul/bin/locked adb adb devices` lists `9a7c74f4`; screen on, unlocked, not hot;
  Do Not Disturb on (precondition 6).
- Telegram's package: `adb shell pm list packages | grep -i telegram` (`org.telegram.messenger` from the Play Store,
  `org.telegram.messenger.web` from telegram.org). Never `pm path`.
- The template apps: the catalog release build and the sample app from `staging`, as District §2.2; release builds
  only. This run builds nothing.
- Tools (standard-library Python, `ffmpeg`/`ffprobe` inside `locked`):

| Tool | Where | Does |
|---|---|---|
| `tg_target.py` | `research/telegram/tools/` | tap points for allow-listed labels and the empty message field (§1.3) |
| `tg_fit.py` | `research/telegram/tools/` | `fit_motion.py` with Telegram's curves added: Decelerate(1.5), AccelerateDecelerate, DEFAULT, EASE_OUT_QUINT and the chat curve. On synthetic 120 fps tracks with 10 % of frames dropped and ±0.6 px noise it named each curve and duration exactly (150, 200, 380, 250 ms) |
| `edge_track.py`, `scroll_track.py`, `patch_luma.py`, `fit_motion.py`, `frame_summary.py` | `research/district/tools/` | unchanged: edge and scroll tracking, a patch's grey level, fits, per-window frame timing |

### 2.3 Device facts, read once (read-only)

```bash
export MSYS_NO_PATHCONV=1; A="adb -s 9a7c74f4"
TG=$($A shell pm list packages | tr -d '\r' | grep -oE 'org\.telegram\.messenger(\.web)?$' | head -1)
$A shell getprop ro.build.fingerprint
$A shell wm size; $A shell wm density
$A shell dumpsys display | grep -m 2 -oE "[0-9.]+ x [0-9.]+\) dpi"   # physical x and y dpi: Telegram's cm thresholds
$A shell dumpsys display | grep -iE "renderFrameRate|refreshRate|mActiveModeId|supportedModes" | head -8
$A shell dumpsys thermalservice | grep -i "Thermal Status"
$A shell settings get global zen_mode                  # 0 or empty: stop (precondition 6)
$A shell settings get global low_power                 # 1: stop and tell you
$A shell settings get global animator_duration_scale   # anything but 1 or empty: stop and tell you
$A shell settings get secure long_press_timeout
$A shell settings get secure navigation_mode           # 2 = gestures; otherwise T4 is skipped
$A shell dumpsys package "$TG" | grep -E "versionName|versionCode|targetSdk"
# Telegram's device grade inputs (SharedConfig.java:1625-1697, restated in SOURCES.md §4.1)
$A shell cat /sys/devices/system/cpu/possible
for c in 0 1 2 3 4 5 6 7; do $A shell cat /sys/devices/system/cpu/cpu$c/cpufreq/cpuinfo_max_freq; done
$A shell getprop dalvik.vm.heapgrowthlimit             # Android's memoryClass
$A shell getprop dalvik.vm.heapsize                    # largeMemoryClass
$A shell getprop ro.soc.model; $A shell getprop ro.soc.manufacturer; $A shell getprop ro.board.platform
$A shell grep MemTotal /proc/meminfo
```

Write them to `phone/device.txt`. Then work out Telegram's grade with its rule, restated in our words: LOW when there
are 2 cores or fewer, a memory class of 100 MB or less, 4 cores or fewer with a mean maximum clock of 1250 MHz or less,
under 2 GiB of RAM, or (on SDK 31+) a chip on its deny list; otherwise AVERAGE when there are fewer than 8 cores, a
memory class of 160 MB or less, or a mean maximum clock of 2055 MHz or less; otherwise HIGH. The deny list compares the
upper-cased `ro.soc.model` with 11 chips, recorded in `SOURCES.md` §4.1 (Exynos 850, 7872, 7880, 7870; MSM8953, 8937,
8940, 8992, 8952, 8917; SDM439); the downloaded source is not needed and will be gone. `RESEARCH.md` §2.2 expects
AVERAGE from spec clocks. If you opened Power Saving (question 6), one dump there gives the switches' states: write
which effects are on, then move the dump to the Recycle Bin.

---

## 3. Recording and timing settings

As District §3, with these additions:

- **Recording**: `screenrecord --size 720x1600 --bit-rate 20000000 --time-limit <s>`; 1 dp = 1.8333 recorded px; frame
  times from `ffprobe`, never frame numbers; extract with `-fps_mode passthrough`.
- **Timing**: `dumpsys gfxinfo <package> reset`, the gesture, 1.5 s, then `dumpsys gfxinfo <package> framestats`.
  Telegram draws popups, the photo viewer and sheets (unless attached) in **windows of their own**: `frame_summary.py`
  keeps windows apart. HWUI keeps each window's last 120 frames (about 1 s at 120 Hz), so the long scrolling scenes
  (L2) also keep the dump's **summary block** (total frames, janky frames, percentiles), which covers everything since
  the reset.
- **Refresh rate**: Telegram asks for the display's top rate only on some windows (RESEARCH §2.4 row 16) and a public
  report describes 120 Hz stutter on custom ROMs. `frame_summary.py` reports each window's frame interval: say beside
  every comparison whether Telegram ran at 60, 90 or 120 Hz.
- **Playing media**: Telegram may autoplay videos and GIFs in the channel; a video in a `TextureView` adds frames to the
  activity's series. Mark any timing taken while media played, and prefer stretches of text and photos.
- **Separate passes**: recordings are never used for frame statistics. Three runs of every timed gesture, three
  recorded runs of every fitted one. `dnd_on` runs before each pass (recording, timing, template) and after any pause.

### 3.1 The script's functions

`phone/study.sh`, run as `D:/projects/tools/.overhaul/bin/locked adb bash phone/study.sh`. Positions are thousandths
of the screen, from a still of the screen in question, never guessed. Every helper runs on the catalog first (§4.0).

```bash
#!/usr/bin/env bash
set -uo pipefail
export MSYS_NO_PATHCONV=1
S=9a7c74f4; A="adb -s $S"
TG=$($A shell pm list packages | tr -d '\r' | grep -oE 'org\.telegram\.messenger(\.web)?$' | head -1)
OUT=D:/projects/tools/.overhaul/research/telegram/phone
DT=D:/projects/tools/.overhaul/research/district/tools
TT=D:/projects/tools/.overhaul/research/telegram/tools
EXTRA=$OUT/checks/allow-extra.txt                          # the channel's title, a sheet's trigger: your answers only
LABELS=$OUT/checks/network-usage-labels.txt                # Network Usage's tab names as your phone shows them
read -r W H < <($A shell wm size | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{s=(o!=""?o:p); split(s,a,"x"); print a[1], a[2]}')
DPI=$($A shell wm density | tr -d '\r' | awk -F': ' '/Override/{o=$2} /Physical/{p=$2} END{print (o!=""?o:p)}')
SLOP3=$(( 24 * DPI / 160 ))
px() { echo $(( $1 * $2 / 1000 )); }

dnd_on() {                                                 # precondition 6, read only: stop unless it is on
  local z; z=$($A shell settings get global zen_mode | tr -d '\r')
  case "$z" in 1|2|3) return 0 ;; *) echo "Do Not Disturb reads '$z': stop and ask" >&2; return 1 ;; esac
}
ui() {                                                     # ui <name>: only on a screen §1.2 allows
  $A shell uiautomator dump /data/local/tmp/tg-ui.xml >/dev/null 2>&1
  $A pull /data/local/tmp/tg-ui.xml "$OUT/ui/$1.xml" >/dev/null 2>&1
  $A shell rm -f /data/local/tmp/tg-ui.xml
}
learn_labels() {                                           # once, right after T1's first tap; read on the phone only
  $A shell uiautomator dump /data/local/tmp/tg-chk.xml >/dev/null 2>&1
  $A shell "grep -oiE '(text|content-desc)=\"(all|mobile|wi-?fi|roaming)\"' /data/local/tmp/tg-chk.xml" \
    | tr -d '\r' | sed -E 's/^[a-z-]+="(.*)"$/\1/' | sort -u > "$LABELS"
  $A shell rm -f /data/local/tmp/tg-chk.xml
  [ "$(grep -c . "$LABELS")" -ge 3 ]                       # fewer: T2-T5 by your hand, or skipped
}
on_network_usage() {                                       # true only with Network Usage in front; nothing leaves the phone
  local ok=0 label
  $A shell uiautomator dump /data/local/tmp/tg-chk.xml >/dev/null 2>&1 || return 1
  while IFS= read -r label; do
    [ -n "$label" ] || continue
    $A shell "grep -qiE '(text|content-desc)=\"$label\"' /data/local/tmp/tg-chk.xml" || ok=1
  done < "$LABELS"
  $A shell rm -f /data/local/tmp/tg-chk.xml
  return $ok
}
point_label() {                                            # point_label <label>: "x y", or non-zero = no tap
  local name; name=$(echo "$1" | tr -c 'A-Za-z0-9' '_')
  ui "${name}_a"; sleep 2; ui "${name}_b"
  python -I "$TT/tg_target.py" "$OUT/ui/${name}_a.xml" "$1" --again "$OUT/ui/${name}_b.xml" --allow-extra "$EXTRA"
}
field_point() {                                            # field_point <label>: the one empty text field
  ui field_a; sleep 2; ui field_b
  python -I "$TT/tg_target.py" "$OUT/ui/field_a.xml" "$1" --field --again "$OUT/ui/field_b.xml"
}
tap_label() { local xy; xy=$(point_label "$1") || return 2; $A shell input tap $xy; }
keyboard_up() { $A shell dumpsys input_method | tr -d '\r' | grep -qE 'mInputShown=true|inputShown=true'; }
popup_up() { $A shell dumpsys window windows | tr -d '\r' | grep -qiE 'PopupWindow'; }
rec() {                                                    # rec <scene> <seconds> <command...>
  local name=$1 secs=$2; shift 2
  $A shell rm -f /data/local/tmp/tg-$name.mp4
  $A shell screenrecord --size 720x1600 --bit-rate 20000000 --time-limit "$secs" /data/local/tmp/tg-$name.mp4 &
  local pid=$!; sleep 1.0
  "$@"
  wait $pid; sleep 0.3
  $A pull /data/local/tmp/tg-$name.mp4 "$OUT/rec/$name.mp4" >/dev/null && $A shell rm -f /data/local/tmp/tg-$name.mp4
}
timing() {                                                 # timing <scene> <package> <command...>
  local name=$1 pkg=$2; shift 2
  $A shell dumpsys gfxinfo "$pkg" reset >/dev/null
  "$@"; sleep 1.5
  $A shell dumpsys gfxinfo "$pkg" framestats > "$OUT/timing/$name.txt"
}
# swipe, hswipe, drag, hold_release, hhold_release, back_key, back_swipe, back_flick and gap_check:
# copied unchanged from research/district/PHONE-STUDY.md §3.1.
```

`keyboard_up` and `popup_up` read the field names this Android build uses: confirm them on the catalog in §4.0 (step
5) and adjust the pattern there once, never on Telegram. `learn_labels` and `on_network_usage` are checked on the
catalog's own tab page in §4.0 (step 6), with that page's tab names in place of Telegram's.

`point_label`, `field_point`, `on_network_usage` and their exit codes are decided **before** a recording starts; the
recorded command is only the gesture itself (`rec T1_r1 4 $A shell input tap $xy`, `rec T2_r1 3 back_key`).

---

## 4. Scenes

Let the screen settle 2 s before each scene. Scene ids are used in file names (`T1_r1.mp4`, `T1_r1.txt`).

### 4.0 Pre-flight on the catalog (before anything touches Telegram)

1. **Gap**: `gap_check` on a catalog list; the largest of five gaps under half the long-press timeout, else every drag
   on Telegram is done by hand or skipped.
2. **First move**: `hold_release` on a catalog sheet's grabber; the sheet moves with the first MOVE.
3. **Labels**: `tg_target.py` refuses a catalog switch row and accepts a plain navigation row (write a one-line
   `allow-extra.txt` with that row's title for the test, then empty it again).
4. **Field**: `field_point` accepts an empty catalog text field and refuses it once text is typed into the catalog
   (the catalog is ours: typing there is allowed).
5. **Keyboard and popup checks**: `keyboard_up` true with the catalog field focused and false after Back; `popup_up`
   true with the catalog's menu open.
6. **Screen check**: on the catalog's "Bars and pages" pager, `learn_labels` (with the pattern set to that page's tab
   names) finds them, `on_network_usage` is true there and false one screen back; then `LABELS` is emptied again.
7. **Do Not Disturb**: `dnd_on` is true.

Results to `phone/checks/preflight.txt`. Any failure stops the run.

### 4.1 Transitions on Settings sub-screens (you bring the phone to Data and Storage)

Order inside each pass: T1 first. **T2 to T5 run only after a T1 tap printed a point, was made, and
`on_network_usage` then passed**; each run of T2, T3, T4 and T5 starts with `on_network_usage` (outside the recording),
and the run stops when it fails. After a commit (T2, T3a/c/d, T4 commit) Data and Storage is in front: the run brings
Network Usage back with T1's path (`point_label "Network usage"`, the tap with the recording off, then
`on_network_usage`), or, if that tap is refused, you tap it by hand and the run checks again.

| Id | Gesture | Rec | Timed | What it measures (source expectation, RESEARCH §2.7) |
|---|---|---|---|---|
| T1 | push: `xy=$(point_label "Network usage")`, then the tap; the first time only, `learn_labels` 1 s after it (outside the recording) | 3 | 3 | the new page's alpha and 48 dp slide over 150 ms on Decelerate(1.5); page beneath still; opening frame, tap-to-present |
| T2 | pop: `on_network_usage`, then `back_key` | 3 | 3 | 150 ms fade and slide out |
| T3 | swipe back from mid-page on Network Usage, with its first tab showing, each run after `on_network_usage`. Telegram restarts the page's offset where it recognises the swipe (about 0.4 cm in, so after `drag`'s first 66 px move), which the end points allow for: **a** `hswipe 600 300 950 <ms>` at 300, 150 and 80 ms (commits); **b** `hhold_release 600 300 500` (released still with the page about 14 % in: a cancel); **c** `hhold_release 600 300 750` (about 39 %: a commit by position); **d** `hswipe 600 300 500 50` (about 4300 px/s over 14 %: a commit by speed) | 2 each | 1 each | recognition after 0.4 cm; 1:1 follow; commit past a third or at 3500 px/s; commit max(200 ms·rest/w, 50), cancel max(320 ms·x/w, 120), AccelerateDecelerate; carry of speed (expected none); scrim up to 0.38; page beneath still. A T3b that commits instead leaves Data and Storage in front, which the next `on_network_usage` catches |
| T4 | predictive back from the edge on Network Usage, each after `on_network_usage`: `back_swipe cancel`, `back_swipe commit`, `back_flick` (gestures only) | 2 each | 1 each | scale toward 0.90 then 0.85, pivot on the far edge at the touch height, no slide during the gesture; commit at least 380 ms and cancel 320 ms on EASE_OUT_QUINT |
| T5 | Network Usage tabs, each after `on_network_usage`: `hswipe 600 800 200 <ms>` (leftward only: a rightward swipe on the first tab is T3) at 300, 150 and 80 ms; then `tap_label` on the next tab's name | 3 × 3 + 3 | 1 × 3 + 3 | settle from velocity, 150-600 ms, quintic; a tab tap 540 ms EASE_OUT_QUINT |

### 4.2 Lists

| Id | Gesture | Rec | Timed | What it measures |
|---|---|---|---|---|
| L1 | the Language list (you open it; the run never taps a row): `swipe 500 750 350 <ms>` up at 300, 150 and 80 ms, starting below the switches at the top (from a still), then the same down | 3 × 3 | 1 × 3 | coast distance and duration (Android's fling, against our iOS decay); janky %, p90/p99, GPU p90, hitch, frame interval |
| L2 | the public channel, from the newest post: ten `swipe 500 300 750 80` (older posts) with 0.6 s pauses, then ten back down | 2 | 3 (summary block + last 120 frames) | how scrolling holds while older history loads (Telegram fetches 50 posts when 25 rows remain); frame interval; mark playing media |
| L3 | after L2, the jump arrow if its label is on the allow-list (`tap_label "Go to bottom"` or `"Scroll to bottom"`) | 3 | 3 | Telegram's two-screen jump: (distance/height + 1) × 200 ms within 300-1300 ms, EASE_OUT_QUINT |
| L4 | optional, **by your hand**: long-press a post's text; the run records (`rec L4 6 sleep 5`) and times it; you close the menu with Back | 2 | 2 | the menu's opening frame (Telegram builds its blur snapshot on the main thread, RESEARCH §2.6) |

After the first L2 recording, the run looks at five evenly spaced frames before going on: a post showing a person's
name (a signature, a "forwarded from" person, a commenter's avatar) sends the recording to the Recycle Bin and stops
the channel scenes until you pick another channel or stretch.

### 4.3 The channel's profile

| Id | Gesture | Rec | Timed | What it measures |
|---|---|---|---|---|
| PR1 | in the channel, `xy=$(point_label "<channel title>")` (from `allow-extra.txt`), then the tap; `back_key` to return | 3 + 3 | 3 + 3 | Telegram's custom open: 180 ms Decelerate for a title tap (250 ms for the avatar), started 50 ms after creation; opening frame and per-frame cost (it lays out every frame) |
| PR2 | on the profile, `hold_release` from the avatar's centre down 30 % (a drag that leaves slop at once) | 2 | 1 | the avatar's pull-down expand and its settle: (1 − v) × 250 ms / factor, EASE_BOTH |
| PR3 | the shared media tabs: `hswipe <y> 800 200 150` over the media pager (from a still), never a tap on media | 2 | 1 | the pager's settle |
| PR4 | optional, **by your hand**: open a photo from shared media; the run then drags it down 30 % with `hold_release` | 2 | 2 | the photo viewer's 200 ms open (its own window) and the drag-down close past a sixth of the height |

### 4.4 Keyboard and menu in the empty test chat (you open the chat)

K1 and K2 run only once you have done what you chose in question 5 (a harmless word in the clipboard, or clipboard
suggestions off, and no suggested words on an empty field). After the first K1 recording, the run looks at the
keyboard's suggestion strip in its last frame: any text other than the word you copied sends the recording to the
Recycle Bin and stops the keyboard scenes (§9).

| Id | Gesture | Rec | Timed | What it measures |
|---|---|---|---|---|
| K1 | `xy=$(field_point "Message")`, then the tap; nothing is typed | 3 | 3 | the keyboard: Telegram's single layout then a 250 ms slide on the chat curve; frames per keyboard frame (against RESEARCH P5) |
| K2 | `keyboard_up && back_key` (one Back, only while the keyboard is up) | 3 | 3 | the keyboard closing |
| K3 | `tap_label "More options"`; then `popup_up && back_key`; never a tap on an item | 3 | 3 | the popup: background scale plus a 4-item wave over 150 + 16 × items ms; dismiss 150 ms (its own window) |

### 4.5 One sheet (only if you named one in question 7)

Open it with `tap_label "<its trigger>"` (from `allow-extra.txt`) and close it with `back_key` once `dumpsys window
windows` shows its dialog window. Telegram's sheet snaps back only when it is released less than 0.8 cm down (measured
with the screen's y dpi: about 124 px at 395 dpi [D; the study reads the dpi in §2.3]) or flung up at 3500 px/s or
more; anything else dismisses it (`BS:382-418`). Its drag starts counting where it is recognised, at the first move of
at least the touch slop (`BS:550-551`), so the translation is the drag less its first 66 px move.

| Id | Gesture | Rec | Timed | What it measures |
|---|---|---|---|---|
| S1 | open with `tap_label` | 3 | 3 | waits for its layout, then 400 ms EASE_OUT_QUINT after 20 ms |
| S2 | close by `back_key` | 3 | 3 | 250 ms EASE_OUT |
| S3 | snap-back: `hold_release <x> <y> <y + 61>` from its top band (about 146 px: the 66 px first move plus about 80 px, released still) | 2 | 1 | the translation (about 80 px, under 0.8 cm) returns over 250 ms × translation / 0.8 cm on DEFAULT, about 160 ms here [D] |
| S4 | drag-dismiss: `hold_release <x> <y> <y + 300>` from its top band (30 % of the height, released still) | 2 | 1 | a dismiss, not a snap-back: 250 ms EASE_OUT to the bottom; then `tap_label` opens it again for the next run |

### 4.6 Order on the phone

Pre-flight (about 10 min), `dnd_on`, the recording pass over T, L, PR, K, S (about 45 min), `dnd_on`, then the timing
pass (about 20 min), reaching each screen the same way each time and turning recording off for every passage through a
screen §1.2 forbids. End at the phone's home screen (`input keyevent 3`), and tell you that Do Not Disturb and the
keyboard are yours to set back.

---

## 5. The same scenes on the template

Same gestures, same thousandths, Standard motion style, and the template's theme set to match Telegram's (read
Telegram's day or night look from the recordings; never change Telegram's).

| Telegram scene | Template |
|---|---|
| T1, T2 | the staging sample app through `tools/perf/record.sh <package> pushpop` and `perf.sh pushpop`, so they match the release gates |
| T3 | no full-page swipe exists yet (RESEARCH P10): record the back card with `back_swipe` in its place, and say so |
| T4 | `record.sh … back_slow_commit`, `back_slow_cancel`, `back_fast_flick` |
| T5 | the catalog's "Bars and pages" pager |
| L1, L2, L3 | the catalog's "Lists" (deceleration) and its longest list; re-tap of the active tab on the sample's list (scroll to top). No list as long as a channel's history exists: compare the cost per frame, not the size |
| PR1-PR4 | no profile hero; PR4 against the catalog's image viewer ("Collections") |
| K1, K2 | a catalog screen with a text field and `AppBottomActionBar` (note which one); RESEARCH P5's case |
| K3 | the catalog's menu (`AppMenu`) |
| S1-S4 | the catalog's "Sheets", "Detents" → "Basic" (the template's snap-back and dismiss follow its own detent rules; compare tempo, not thresholds) |

`am start -n <catalog id>/com.base.app.catalog.CatalogActivity --es section <Section> --es theme <dark|light> --es motion Standard`,
with the section names of District §5.

---

## 6. Optional: Telegram's windows and views (only with your yes to question 8)

```bash
$A shell dumpsys window windows | grep -cE "org.telegram.messenger"     # with a popup or sheet open, then closed
$A shell dumpsys activity top | grep -m 20 -E "LaunchActivity|ActionBarLayout|RecyclerListView|ChatMessageCell|SizeNotifierFrameLayout"
```

Class names only, written to `phone/` and kept like the timing dumps. Run it only on a screen §1.2 allows.

---

## 7. Measuring

Commands run from `research/telegram/phone/`, with `DT=…/district/tools` and `TT=…/telegram/tools`.

### 7.1 What to track

| Scene | Track |
|---|---|
| T1, T2 | Two tracks, because the page fades while it slides. **Alpha**: `patch_luma.py --rect` on a flat area of the arriving page that differs from the page beneath; alpha = (L − L_before) / (L_after − L_before). **Slide**: `edge_track.py --axis x` on a strong vertical edge inside the arriving page, from the frame its alpha passes about 0.3 (`fit --from`); 48 dp is 88 recorded px |
| T3 | `edge_track.py --axis x` on the moving page's left edge; `--swipe-ms` for the release; the dim from `patch_luma.py` on the page beneath, left of the edge |
| T4 | the card's left and right edges (`--axis x`, two strips) give its width, hence its scale; its top edge gives the vertical follow |
| T5, PR3 | `edge_track.py --axis x` on a tab page's edge |
| L1, L2 | `scroll_track.py --window` covering only rows that scroll rigidly (no header, date pill or input bar); Android's fling is not exponential, so report coast distance and duration beside `--decay` |
| L3 | `scroll_track.py`, or `edge_track.py` on a post's edge |
| K1, K2 | `edge_track.py` on the input bar's top edge |
| K3 | `edge_track.py` on the popup's bottom edge (its background scales from the top right) |
| PR1, PR2, PR4 | the avatar's or the header's bottom edge; the photo's top edge |
| S1-S4 | `edge_track.py` on the sheet's top edge; for S3 the release frame comes from the recording, and the translation at release from the track |

Check every track with five crops around the tracked position (District §7.1).

### 7.2 Fit

```bash
python -I $TT/tg_fit.py tracks/T1_slide_r1.csv tracks/T1_slide_r2.csv tracks/T1_slide_r3.csv
python -I $TT/tg_fit.py tracks/T3_150_r1.csv tracks/T3_150_r2.csv --swipe-ms 150 --json > fits/T3_150.json
```

A scene **matches the source** when the best tween is the curve RESEARCH §2.7 gives, at its duration within one
8.3 ms frame, clearly better than the best spring. A 48 dp slide is too short to tell a spring from a tween on its own
(on the synthetic track their errors were 0.38 and 0.37 px); the duration's constancy across trials and the alpha
track decide. For T3, T5 and S3, compare the settle durations with the source's formulas at the measured release
position and speed; `carry` near 0 confirms that Telegram does not keep the finger's speed.

### 7.3 Frame timings, per window

```bash
python -I $DT/frame_summary.py timing/T1_r1.txt timing/T1_r2.txt timing/T1_r3.txt
```

Per window: janky %, p50/p90/p99, GPU p90, hitch ms/s, frame interval, the first frame with its flags, the opening
frame and tap-to-present (District §7.3 explains each). For L2, add the dump's summary block (frames rendered, janky
frames, 50th/90th/95th/99th percentiles, GPU percentiles) for the whole scene. The template's own runs are
cross-checked with its `parse_framestats.py` as District §7.3 shows.

---

## 8. Output

- `research/telegram/phone/`: local only, deleted to the Recycle Bin as §1.5 says.
- `research/telegram/PHONE-RESULTS.md`, numbers and verdicts only:
  - device facts, Telegram's version, its grade worked out from §2.3, and the Power Saving switches if read;
  - the pre-flight results;
  - one table per scene group, Telegram beside the template: durations (t50/t90/t99), the fitted curve or spring,
    carry, the frame interval, and per window janky %, p90/p99, GPU p90, hitch, opening frame and tap-to-present;
  - each scene against the source's expectation (matches, differs, and by how much);
  - the answers to RESEARCH §2.8, and what they change in P1-P11;
  - what could not be measured and why (a refused tap, a scene done by hand, a scene skipped for privacy, a failed
    screen check).
- Then the proposals are asked in chat one at a time, with pros, cons, catches and the recommended option first, and
  the measured numbers in each question.

## 9. Stop conditions

Stop, press nothing more, and ask in chat when:

- a sign-in, permission, payment, consent or update screen appears; a dialog asks to confirm anything; a tap lands
  somewhere unplanned;
- a name, a message outside the chosen channel, a channel post showing a person's name, a story or a notification
  banner appears on a screen being recorded or dumped (move that file to the Recycle Bin first);
- **the keyboard's suggestion strip shows text** (a clipboard chip, a suggested word or a name) other than the harmless
  word you copied (move the recording to the Recycle Bin first);
- **`on_network_usage` fails** before a pop, a swipe-back, a predictive back or a tab swipe, or `learn_labels` finds
  fewer than three tab names;
- **`dnd_on` fails** (Do Not Disturb reads off or nothing);
- the message field holds text; Telegram shows a promotion, a "what's new" sheet or any sheet nobody opened;
- the phone gets hot (thermal status 2 or more) or battery saver turns on; the pre-flight fails; or a planned tap is
  refused and you are not there to make it.
