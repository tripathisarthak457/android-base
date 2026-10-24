#!/usr/bin/env bash
# The self-hosted sample clips of media.md 9.2: each CC0 original fetched once, one at a time, with a descriptive
# User-Agent, then transcoded to what reels apps serve (H.264 High MP4, 720x1280 portrait, about 1.5 Mbit/s, a
# keyframe every second, +faststart, at most 10 s, no audio track needed) with a JPEG poster of its first frame.
#
# It downloads and writes files outside the project, so it runs only after the user's own yes in chat (media.md 9.3:
# %TEMP%\mrev\, recycled when the release is made). It never publishes anything: the orchestrator creates the
# release from $OUT after an Opus reviewer has looked at every poster and three frames of every clip (L6).
#
#   bash media-clips.sh fetch       # originals into %TEMP%\mrev\clips\src (skips what is there; stops on a 429)
#   bash media-clips.sh transcode   # $OUT: <key>.mp4, <key>.jpg, CREDITS.md, frames/ for the content review
#   bash media-clips.sh phone-frames  # L6 for the clips as the catalog plays them now: three screenshots of each
#                                     # clip on the phone (perf/media/apks/B.apk); nothing is fetched to this machine
set -uo pipefail
ROOT=$(cygpath -u "$TEMP")/mrev/clips
SRC=$ROOT/src
OUT=$ROOT/out
UA="BaseAppCatalog-review (https://android-base.vercel.app; sample media)"
COMMONS=https://upload.wikimedia.org/wikipedia/commons
MDN=https://interactive-examples.mdn.mozilla.net/media/cc0-videos
# key | source url | credit
CLIPS="
flower-moon|$COMMONS/a/a8/Flower_Moon_May_2nd_2024.webm|Roesalow, CC0, Wikimedia Commons
kings-peak|$COMMONS/3/3b/King%27s_Peak_summit.webm|Derrellwilliams, CC0, Wikimedia Commons
tilia|$COMMONS/8/80/Tilia_cordata_flowers_under_a_microscope_zoom-in.webm|Gustamons, CC0, Wikimedia Commons
oystercatcher|$COMMONS/3/3d/Oystercatcher_juvenile_15-06-24_1.webm|Cesaremanfredi, CC0, Wikimedia Commons
srnka|$COMMONS/d/dc/Srnka_v_zahrad%C4%9B.webm|Pohled 111, CC0, Wikimedia Commons
perseids|$COMMONS/1/1d/Perseiden.2020-08-12.webm|Bautsch, CC0, Wikimedia Commons
ocean|$COMMONS/5/5d/Ocean_in_Sommaroy.webm|Netha Hussain, CC0, Wikimedia Commons
bumblebee|$COMMONS/0/04/Bumblebee_on_flowers.webm|Roman Ryabenko, CC0, Wikimedia Commons
jellyfish|$COMMONS/a/aa/M%C3%A9duse_%C3%A0_l%27aquarium_de_Monaco_en_novembre_2021.webm|Benoit Prieur, CC0, Wikimedia Commons
mt-hood|$COMMONS/9/99/Mt_Hood_Aerial_View.webm|Pastelitodepapa, CC0, Wikimedia Commons
flower|$MDN/flower.mp4|MDN, CC0
"
# friday.mp4 (MDN) was rejected in the content review (L6, MEDIA run 5): people in focus, from a 1940 film.

each() { echo "$CLIPS" | while IFS='|' read -r key url credit; do [ -z "$key" ] || "$@" "$key" "$url" "$credit"; done; }

fetch_one() {
  local key=$1 url=$2 file=$SRC/$1.${2##*.}
  [ -s "$file" ] && return 0
  local code
  code=$(curl.exe -sS -A "$UA" -L -o "$file" -w '%{http_code}' "$url")
  echo "$key: HTTP $code"
  if [ "$code" = 429 ]; then rm -f "$file"; echo "a 429: stopping, as the host asks"; exit 1; fi
  [ "$code" = 200 ] || { rm -f "$file"; return 1; }
  sleep 2
}

transcode_one() {
  local key=$1 credit=$3 file
  file=$(ls "$SRC/$key".* 2>/dev/null | head -1)
  [ -z "$file" ] && { echo "$key: no original"; return 1; }
  # Fill a portrait 720x1280 frame, cropped as the scene shows landscape clips.
  ffmpeg -nostdin -y -loglevel error -i "$file" -t 10 -an \
    -vf "scale=720:1280:force_original_aspect_ratio=increase,crop=720:1280,fps=30" \
    -c:v libx264 -profile:v high -preset slow -b:v 1500k -maxrate 1800k -bufsize 3000k -g 30 -keyint_min 30 \
    -sc_threshold 0 -pix_fmt yuv420p -map_metadata -1 -map_metadata:s:v -1 -movflags +faststart "$OUT/$key.mp4" || return 1
  ffmpeg -nostdin -y -loglevel error -i "$OUT/$key.mp4" -frames:v 1 -q:v 4 "$OUT/$key.jpg"
  # Start, middle and end of the clip: several clips run under 8 s, so fixed 1/4/8 s seeks would miss them.
  local d i=0 t
  d=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$OUT/$key.mp4" | tr -d '\r')
  for t in 0.5 $(awk "BEGIN{print $d/2}") $(awk "BEGIN{print $d-0.5}"); do
    i=$((i + 1))
    ffmpeg -nostdin -y -loglevel error -ss "$t" -i "$OUT/$key.mp4" -frames:v 1 -q:v 5 "$OUT/frames/$key-$i.jpg"
  done
  echo "| $key.mp4 | $credit |" >> "$OUT/CREDITS.md"
  echo "$key: $(stat -c %s "$OUT/$key.mp4") bytes"
}

OV=D:/projects/tools/.overhaul
PKG=com.example.everything.catalog
export LOCKED_PRIORITY=low
# adb reads stdin: without </dev/null the first `adb shell` inside the clip loop eats the rest of the list.
adbq() { "$OV/bin/locked" adb adb "$@" < /dev/null; }

frames_one() { # <key>: three screenshots of the settled clip, about 1, 4 and 8 s after the swipe that brought it
  local key=$1 t
  for t in 1 4 8; do
    case $t in 1) sleep 1 ;; 4) sleep 2.3 ;; 8) sleep 3.3 ;; esac
    adbq shell screencap -p /data/local/tmp/media-clip.png
    adbq pull /data/local/tmp/media-clip.png "$FRAMES/$key-$t.png" > /dev/null
  done
  adbq shell input swipe 540 1800 540 600 120
}

case "${1:-}" in
  fetch) mkdir -p "$SRC"; each fetch_one ;;
  phone-frames)
    # One adb lock for the whole pass, so every screenshot is of the page this pass swiped to.
    case ",${LOCKED_HELD:-}" in *,adb:*) ;; *) exec "$OV/bin/locked" adb bash "$0" phone-frames ;; esac
    FRAMES=$OV/perf/media/clips/$(date +%Y%m%d-%H%M); mkdir -p "$FRAMES"
    adbq install -r -t "$OV/perf/media/apks/B.apk" > "$FRAMES/install.txt" 2>&1
    grep -q Success "$FRAMES/install.txt" || { echo "install failed: $FRAMES/install.txt"; exit 1; }
    adbq shell am start -W -S -n $PKG/$PKG.CatalogActivity --es section Reels > "$FRAMES/start.txt"
    sleep 4
    echo "$CLIPS" | while IFS='|' read -r key url credit; do [ -n "$key" ] && frames_one "$key"; done
    adbq shell rm -f /data/local/tmp/media-clip.png
    adbq shell am force-stop $PKG
    echo "frames -> $FRAMES"
    ;;
  transcode)
    mkdir -p "$OUT/frames"
    printf '# Sample clips\n\nEach clip is public domain (CC0), its uploader'"'"'s own work, transcoded once for the template'"'"'s catalog.\n\n| File | Credit |\n|---|---|\n' > "$OUT/CREDITS.md"
    each transcode_one ;;
  *) sed -n '2,13p' "$0"; exit 2 ;;
esac
