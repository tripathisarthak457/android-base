"""
The generator's targets, by the `platform` a spec names. A target whose package or template is not
in this checkout yet is left out, so the registry only ever offers what can be generated.
"""

from __future__ import annotations

import importlib

from ..core.spec import PLATFORMS, SpecError
from ..core.target import Target


def foreign_options(key: str) -> dict[str, str]:
    """
    Each option another target's template declares and `key`'s does not, with the projects it asks
    for: "projects without UI components". Defined before the registry loads, because the targets
    import it while it does; it reads the registry only when called.
    """
    own = set(REGISTRY[key].manifest.options) if key in REGISTRY else set()
    found: dict[str, str] = {}
    for other, target in REGISTRY.items():
        if other == key:
            continue
        for option in target.manifest.options.values():
            if option.key not in own:
                found[option.key] = f"projects {'without' if option.default else 'with'} {option.title}"
    return found


def _load() -> dict[str, Target]:
    registry: dict[str, Target] = {}
    for key in PLATFORMS:
        name = f"{__name__}.{key}"
        try:
            module = importlib.import_module(name)
        except ModuleNotFoundError as error:
            if error.name != name:
                raise
            continue
        if module.TARGET.template_dir.is_dir():
            registry[key] = module.TARGET
    return registry


REGISTRY: dict[str, Target] = _load()


def get(key: str) -> Target:
    try:
        return REGISTRY[key]
    except KeyError:
        raise SpecError(
            f"Unknown target '{key}'. This generator has: {', '.join(REGISTRY)}.", "platform"
        ) from None
