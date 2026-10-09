"""
The module layout a spec asks for. `compact` is the template as it is authored; `modular` splits
each feature and data module into `:api` and `:impl`, which is not implemented yet.
"""

from __future__ import annotations

from ...core.spec import Spec, SpecError

MODULAR_AVAILABLE = False


def check(spec: Spec) -> None:
    if spec.architecture == "modular" and not MODULAR_AVAILABLE:
        raise SpecError("modular is not available yet; generate compact for now.", "architecture")
