"""
Records the phone's screen while sending input, for the scroll-direction bug's evidence.

    python -I rec.py --serial 9a7c74f4 --out DIR --name NAME --seconds 8 -- STEP...

A STEP is "sleep:SECONDS", "swipe:X1,Y1,X2,Y2,MILLIS" (device pixels) or "sh:COMMAND". Writes DIR/NAME.mp4 and
DIR/NAME.steps.tsv (each step's start, in seconds after screenrecord was asked to start). Run it under the adb lock.
"""

import argparse
import os
import subprocess
import time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--name", required=True)
    parser.add_argument("--seconds", type=int, default=8)
    parser.add_argument("steps", nargs="*")
    args = parser.parse_args()
    os.makedirs(args.out, exist_ok=True)
    adb = ["adb", "-s", args.serial]
    remote = f"/sdcard/bugscroll_{args.name}.mp4"
    subprocess.run(adb + ["shell", f"rm -f {remote}"], check=True)
    start = time.monotonic()
    recorder = subprocess.Popen(
        adb + ["shell", f"screenrecord --time-limit {args.seconds} --size 720x1600 --bit-rate 16000000 {remote}"],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    # screenrecord takes about a second before its first frame; input sent earlier is not seen.
    time.sleep(1.2)
    log = []
    for step in args.steps:
        kind, _, value = step.partition(":")
        log.append((time.monotonic() - start, step))
        if kind == "sleep":
            time.sleep(float(value))
        elif kind == "swipe":
            x1, y1, x2, y2, millis = (int(v) for v in value.split(","))
            subprocess.run(adb + ["shell", f"input swipe {x1} {y1} {x2} {y2} {millis}"], check=True)
        elif kind == "sh":
            subprocess.run(adb + ["shell", value], check=True)
        else:
            raise SystemExit(f"unknown step {step}")
    recorder.wait(timeout=args.seconds + 30)
    time.sleep(1.0)
    video = os.path.join(args.out, f"{args.name}.mp4")
    subprocess.run(adb + ["pull", remote, video], check=True, capture_output=True)
    subprocess.run(adb + ["shell", f"rm -f {remote}"], check=True)
    with open(os.path.join(args.out, f"{args.name}.steps.tsv"), "w", encoding="utf-8", newline="\n") as handle:
        handle.writelines(f"{t:.3f}\t{s}\n" for t, s in log)
    print(f"{args.name}: {video}")


if __name__ == "__main__":
    main()
