"""
The app icon from `--icon`. Since Xcode 14 an iOS app needs one 1024 × 1024 image, which Xcode
scales for the home screen, Settings and Spotlight, and App Store Connect refuses it when it has an
alpha channel.

A source that is already that PNG is copied byte for byte, which needs nothing beyond the standard
library. Anything else is cropped, scaled and flattened with Pillow when it is installed.
"""

from __future__ import annotations

import shutil
import struct
from pathlib import Path

#: The icon inside the app target's folder; the template's asset catalog names this file.
ICON_PATH = "Assets.xcassets/AppIcon.appiconset/AppIcon.png"

SIZE = 1024

_PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"

#: Greyscale, truecolour and palette. The other two colour types carry an alpha channel.
_COLOR_TYPES_WITHOUT_ALPHA = frozenset({0, 2, 3})


def write(source: Path, app_dir: Path) -> list[str]:
    """Replaces the template's icon in `app_dir` with one made from `source`. Returns warnings, never raises."""
    if not source.is_file():
        return [f"Icon source not found at {source}. The template's placeholder icon was kept."]

    target = app_dir / ICON_PATH
    if is_app_store_ready(source.read_bytes()):
        shutil.copyfile(source, target)
        return []

    try:
        from PIL import Image
    except ImportError:
        return [
            f"The iOS app icon was not made from {source.name}: without Pillow, only a {SIZE}×{SIZE} "
            "PNG with no transparency can be used as it is. Run `pip install pillow` and pass --icon "
            f"again, or replace {ICON_PATH} by hand. The template's placeholder icon was kept."
        ]

    try:
        with Image.open(source) as opened:
            image = opened.convert("RGBA")
    except Exception as error:
        return [f"Could not read {source.name} as an image ({error}). The template's placeholder icon was kept."]

    warnings: list[str] = []
    width, height = image.size
    if width != height:
        warnings.append(
            f"{source.name} is {width}×{height}, not square. It was centre-cropped; supply a square "
            "image to control the crop yourself."
        )
        edge = min(width, height)
        left, top = (width - edge) // 2, (height - edge) // 2
        image = image.crop((left, top, left + edge, top + edge))

    if image.width < SIZE:
        warnings.append(
            f"{source.name} is only {image.width}px. The app icon needs {SIZE}px, so it was scaled up "
            "and will look soft."
        )

    if image.getextrema()[3][0] < 255:
        warnings.append(
            f"{source.name} has transparent pixels, which App Store Connect refuses in an app icon. They "
            "were filled with white; supply an opaque image to choose the background yourself."
        )

    scaled = image.resize((SIZE, SIZE), Image.LANCZOS)
    icon = Image.new("RGB", (SIZE, SIZE), (255, 255, 255))
    icon.paste(scaled, (0, 0), scaled)
    icon.save(target, "PNG")
    return warnings


def is_app_store_ready(data: bytes) -> bool:
    """Whether `data` is a 1024 × 1024 PNG with neither an alpha channel nor a transparent colour."""
    if not data.startswith(_PNG_SIGNATURE):
        return False
    offset = len(_PNG_SIGNATURE)
    header_seen = False
    while offset + 8 <= len(data):
        length, kind = struct.unpack(">I4s", data[offset:offset + 8])
        body = data[offset + 8:offset + 8 + length]
        if kind == b"IHDR":
            if len(body) < 10:
                return False
            width, height, _depth, color_type = struct.unpack(">IIBB", body[:10])
            if (width, height) != (SIZE, SIZE) or color_type not in _COLOR_TYPES_WITHOUT_ALPHA:
                return False
            header_seen = True
        elif kind == b"tRNS":
            return False
        elif kind == b"IDAT":
            # A tRNS chunk must come before the image data, so nothing after this can add transparency.
            return header_seen
        offset += 12 + length
    return False
