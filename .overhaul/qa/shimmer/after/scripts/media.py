"""Frame access without numpy or PIL: ffmpeg decodes, Python reads the bytes."""

import math
import subprocess

TILT = math.radians(20)
COS, SIN = math.cos(TILT), math.sin(TILT)


def size(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries",
                          "stream=width,height", "-of", "csv=p=0", path], capture_output=True, text=True).stdout
    w, h = out.strip().split("\n")[0].split(",")
    return int(w), int(h)


def pts(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v:0", "-show_entries", "frame=pts_time",
                          "-of", "csv=p=0", path], capture_output=True, text=True).stdout
    return [float(v) for v in out.split()]


def gray_frames(path, w, h):
    """Yields every decoded frame (no dropping or duplicating) as w*h bytes of luma."""
    process = subprocess.Popen(["ffmpeg", "-v", "error", "-i", path, "-fps_mode", "passthrough", "-f", "rawvideo",
                                "-pix_fmt", "gray", "-"], stdout=subprocess.PIPE,
                               stderr=subprocess.DEVNULL)
    n = w * h
    try:
        while True:
            chunk = process.stdout.read(n)
            if len(chunk) < n:
                break
            yield chunk
    finally:
        process.kill()
        process.wait()


def rgb_image(path):
    w, h = size(path)
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"],
                         capture_output=True).stdout
    return w, h, raw


def save_frames(path, indices, pattern):
    """Writes the frames at [indices] (decode order, passthrough) as PNGs: pattern % index."""
    for index in indices:
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", path, "-vf", f"select=eq(n\\,{index})",
                        "-fps_mode", "passthrough", "-frames:v", "1", pattern % index], check=True)


def srgb_to_lab_l(r, g, b):
    def lin(c):
        c /= 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    y = 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
    f = y ** (1 / 3) if y > 216 / 24389 else (24389 / 27 * y + 16) / 116
    return 116 * f - 16


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def fit_band(xs, es, half_width, lo, hi):
    """The centre x of a triangle of known half-width that best fits excess es at xs (least squares)."""
    def score(xc):
        sem = smm = 0.0
        for x, e in zip(xs, es):
            m = 1 - abs(x - xc) / half_width
            if m > 0:
                sem += e * m
                smm += m * m
        return (sem * sem / smm if smm > 0 else 0.0), (sem / smm if smm > 0 else 0.0), smm
    best = max(range(int(lo), int(hi), 4), key=lambda xc: score(xc)[0])
    fine = [best + d / 2 for d in range(-10, 11)]
    xc = max(fine, key=lambda c: score(c)[0])
    _, amplitude, coverage = score(xc)
    return xc, amplitude, coverage
