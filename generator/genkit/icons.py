"""
Launcher icons from one source image.

Android wants the same mark at eight sizes in three shapes, and getting one of them wrong is not
visible until the app is on a device with that launcher. This produces the whole set from a single
square image.

Pillow is an optional import. The rest of the generator is standard library on purpose — it runs
on a colleague's laptop with nothing installed — so a missing Pillow is a warning and a project
that still builds with the template's own mark, never a failure.
"""

from __future__ import annotations

import re
from pathlib import Path

from .spec import ProjectSpec

#: Legacy launcher icon sizes, in density buckets. Still used by every pre-Oreo launcher and by
#: a surprising number of third-party ones on current Android.
_LEGACY_SIZES: dict[str, int] = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

#: The adaptive-icon foreground layer is 108dp square at xxxhdpi, of which only the middle 72dp is
#: guaranteed visible — the launcher masks, scales and parallaxes the rest. Artwork drawn to the
#: full 432px is the reason so many icons look cropped on one launcher and fine on another.
_FOREGROUND_CANVAS = 432
_FOREGROUND_SAFE_FRACTION = 72 / 108

#: What Play Console requires for the store listing.
_PLAY_STORE_SIZE = 512


def generate(source: Path, project_dir: Path, spec: ProjectSpec) -> list[str]:
    """
    Writes every launcher asset derived from [source]. Returns warnings, never raises.

    The project already has a working icon before this runs, so every failure here is recoverable
    by leaving that one in place — which is why nothing in this module aborts the generation.
    """
    warnings: list[str] = []

    if not source.is_file():
        return [f"Icon source not found at {source}. The template's own mark was kept."]

    try:
        from PIL import Image, ImageDraw
    except ImportError:
        return [
            "Launcher icons were not generated: this needs Pillow, which is not installed. "
            "Run `pip install pillow` and pass --icon again, or replace the icons by hand. "
            "The template's own mark was kept and the project builds either way."
        ]

    try:
        original = Image.open(source).convert("RGBA")
    except Exception as error:
        return [f"Could not read {source.name} as an image ({error}). The template's mark was kept."]

    width, height = original.size
    if width != height:
        warnings.append(
            f"{source.name} is {width}×{height}, not square. It was centre-cropped; supply a "
            "square image to control the crop yourself."
        )
        edge = min(width, height)
        left, top = (width - edge) // 2, (height - edge) // 2
        original = original.crop((left, top, left + edge, top + edge))

    if original.width < _PLAY_STORE_SIZE:
        warnings.append(
            f"{source.name} is only {original.width}px. The Play Store icon needs "
            f"{_PLAY_STORE_SIZE}px, so it was scaled up and will look soft."
        )

    for module in _icon_modules(project_dir, spec):
        _write_icon_set(module, original, Image, ImageDraw)

    _write_play_store_icon(project_dir, original, Image)
    _point_monochrome_at_the_template_mark(project_dir, warnings)

    return warnings


def _foreground_name(res: Path) -> str:
    """
    The drawable the module's adaptive icon uses as its foreground.

    Read rather than assumed: the catalog deliberately carries a different mark, and writing a PNG
    under the app's name there would produce an icon nothing references.
    """
    adaptive = res / "mipmap-anydpi-v26" / "ic_launcher.xml"
    if adaptive.is_file():
        match = re.search(
            r'<foreground[^>]*android:drawable="@drawable/([A-Za-z0-9_]+)"',
            adaptive.read_text(encoding="utf-8"),
        )
        if match:
            return match.group(1)
    return "ic_launcher_foreground"


def _icon_modules(project_dir: Path, spec: ProjectSpec) -> list[Path]:
    """
    Every application module's res directory.

    The catalog is a second installable app and gets the same icon; two apps that look identical
    in the launcher is the point — they are the same product — and leaving it with the template's
    default is how a tester ends up reporting the wrong build.
    """
    modules = [project_dir / "app" / "src" / "main" / "res"]
    catalog = project_dir / "catalog" / "src" / "main" / "res"
    if spec.has("catalog") and catalog.is_dir():
        modules.append(catalog)
    return [module for module in modules if module.is_dir()]


def _write_icon_set(res: Path, original, Image, ImageDraw) -> None:
    for bucket, size in _LEGACY_SIZES.items():
        directory = res / f"mipmap-{bucket}"
        directory.mkdir(parents=True, exist_ok=True)

        square = original.resize((size, size), Image.LANCZOS)
        square.save(directory / "ic_launcher.png", "PNG")
        _circular(square, size, Image, ImageDraw).save(
            directory / "ic_launcher_round.png", "PNG"
        )

    _write_adaptive_foreground(res, original, Image, _foreground_name(res))

    # The template's pre-26 fallback is a layer-list over the vector that has just been replaced.
    # The density PNGs cover every bucket, so it has nothing left to fall back for.
    for fallback in (res / "mipmap").glob("ic_launcher*.xml"):
        fallback.unlink()


def _write_adaptive_foreground(res: Path, original, Image, foreground: str) -> None:
    """
    The adaptive foreground: the artwork inset into the safe zone on a transparent canvas.

    `-nodpi` because there is exactly one of these and it must not be resampled per density.
    """
    inner = int(_FOREGROUND_CANVAS * _FOREGROUND_SAFE_FRACTION)
    offset = (_FOREGROUND_CANVAS - inner) // 2

    canvas = Image.new("RGBA", (_FOREGROUND_CANVAS, _FOREGROUND_CANVAS), (0, 0, 0, 0))
    canvas.paste(original.resize((inner, inner), Image.LANCZOS), (offset, offset))

    directory = res / "drawable-nodpi"
    directory.mkdir(parents=True, exist_ok=True)
    canvas.save(directory / f"{foreground}.png", "PNG")

    # The template ships a vector of the same name. Two drawables with one name in two
    # configurations is legal and confusing; the PNG is the one that should win, so the vector
    # moves aside rather than staying to be picked at random by whoever reads the folder next.
    vector = res / "drawable" / f"{foreground}.xml"
    if vector.is_file():
        vector.rename(res / "drawable" / "ic_launcher_monochrome.xml")


def _circular(square, size: int, Image, ImageDraw):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)

    round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    round_icon.paste(square, (0, 0), mask)
    return round_icon


def _write_play_store_icon(project_dir: Path, original, Image) -> None:
    """
    512×512 for the store listing, flattened onto white.

    Play rejects a listing icon with transparency, and a source image with a transparent corner
    would otherwise be discovered at upload time rather than here.
    """
    listing = Image.new("RGB", (_PLAY_STORE_SIZE, _PLAY_STORE_SIZE), (255, 255, 255))
    scaled = original.resize((_PLAY_STORE_SIZE, _PLAY_STORE_SIZE), Image.LANCZOS)
    listing.paste(scaled, (0, 0), scaled)
    listing.save(project_dir / "play-store-icon.png", "PNG")


def _point_monochrome_at_the_template_mark(project_dir: Path, warnings: list[str]) -> None:
    """
    Repoints the themed-icon layer at the vector the foreground PNG replaced.

    A photographic foreground cannot become a themed icon: Android tints the layer flat, so a
    full-bleed image renders as a solid blob of the wallpaper colour. Keeping the template's
    silhouette there is wrong, but visibly wrong in a way somebody will fix — a blob is not.
    """
    replaced = False
    for xml in project_dir.rglob("mipmap-anydpi-v26/ic_launcher*.xml"):
        text = xml.read_text(encoding="utf-8")
        updated = re.sub(
            r'(<monochrome[^>]*android:drawable=")@drawable/[A-Za-z0-9_]+(")',
            r"@drawable/ic_launcher_monochrome",
            text,
        )
        if updated != text:
            xml.write_text(updated, encoding="utf-8")
            replaced = True

    if replaced:
        warnings.append(
            "The themed (monochrome) icon still uses the template's silhouette — a photographic "
            "foreground tints to a solid blob. Draw a single-colour vector at "
            "res/drawable/ic_launcher_monochrome.xml when you have one."
        )
