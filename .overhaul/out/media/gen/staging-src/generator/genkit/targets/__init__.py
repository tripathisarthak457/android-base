"""
The generator's targets, by the `platform` a spec names. A target whose package or template is not
in this checkout yet is left out, so the registry only ever offers what can be generated.
"""

from __future__ import annotations

import importlib

from ..core.spec import PLATFORMS, SpecError
from ..core.target import Target


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
