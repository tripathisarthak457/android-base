"""
The resolved tokens as the catalogue's `tokens` object, which the website's previews read instead
of keeping their own copies of the numbers: roles per design style and theme, the type scale, and
every motion style's springs.
"""

from __future__ import annotations

from typing import Any


def catalogue_tokens(tokens: dict[str, Any]) -> dict[str, Any]:
    palette = tokens["palette"]
    return {
        "spacing": tokens["spacing"],
        "radii": tokens["radii"],
        "sizes": tokens["sizes"],
        "borders": tokens["borders"],
        "elevation": tokens["elevation"],
        "type": tokens["type"],
        "motion": tokens["motion"],
        "ramps": palette["ramps"],
        "brand": palette["brand"],
        # A style's palette is its surface tone's; the website looks colours up by style.
        "palette": {name: palette["tones"][style["surfaces"]] for name, style in tokens["styles"].items()},
        "styles": {
            name: {key: value for key, value in style.items() if key != "description"}
            for name, style in tokens["styles"].items()
        },
        "defaults": tokens["defaults"],
    }
