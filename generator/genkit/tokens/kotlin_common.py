"""
The resolved tokens for the Compose Multiplatform template's `commonMain`. The Kotlin is the Android
emission itself: it uses only plain values, `Color`, `Dp` and `TextUnit`, which Compose
Multiplatform keeps in the same packages, so the two templates' design systems read identical
token files and `tools/sync_check.py` can pair them.

The shared file takes the Android values of the platform-specific sizes: the 48 dp touch target is
above iOS's 44 pt minimum, so it satisfies both.
"""

from __future__ import annotations

from typing import Any

from . import kotlin

#: Where the multiplatform copy lives, under the module's source root and the package path.
RELATIVE_PATH = (
    "core/designsystem/src/commonMain/kotlin/{package_path}/core/designsystem/theme/tokens/DesignTokens.kt"
)

#: The platform whose values the shared file takes where the tokens differ per platform.
PLATFORM = "android"


def emit(tokens: dict[str, Any], package: str) -> str:
    return kotlin.emit(tokens, package)
