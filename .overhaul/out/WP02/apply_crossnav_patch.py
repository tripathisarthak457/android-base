#!/usr/bin/env python3
"""
Adds a cross-feature destination to a compact project generated from modules-compact.json:
`OrderDetailKey` in the shared-key folder of :core:navigation (MASTER-PLAN A2.4), registered by
:feature:orders, and opened from :feature:trips. Usage: apply_crossnav_patch.py <project>
"""

import re
import sys
from pathlib import Path


def main(project: Path) -> None:
    namespace = re.search(r'namespace\s*=\s*"([\w.]+)\.feature\.orders"', (project / "feature/orders/build.gradle.kts").read_text())
    package = namespace.group(1)
    path = package.replace(".", "/")

    key = project / "core/navigation/src/main/kotlin" / path / "feature/orders/api/OrderDetailKey.kt"
    key.parent.mkdir(parents=True, exist_ok=True)
    key.write_text(
        f"package {package}.feature.orders.api\n\n"
        f"import {package}.core.common.navigation.AppNavKey\n"
        "import kotlinx.serialization.Serializable\n\n"
        "/** One order, opened from any feature that shows an order id. */\n"
        "@Serializable\n"
        "data class OrderDetailKey(val id: String) : AppNavKey\n"
    )

    navigation = project / "feature/orders/src/main/kotlin" / path / "feature/orders/navigation/OrdersNavigation.kt"
    text = navigation.read_text()
    text = text.replace(
        f"import {package}.feature.orders.api.OrdersDetailKey\n",
        f"import {package}.feature.orders.api.OrderDetailKey\nimport {package}.feature.orders.api.OrdersDetailKey\n",
    )
    entry = "        entry<OrdersDetailKey>(pane = NavPane.Detail) { key -> OrdersDetailRoute(id = key.id, navigator = navigator) }\n"
    assert entry in text, "OrdersNavigation.kt changed shape"
    text = text.replace(
        entry,
        entry + "        entry<OrderDetailKey>(pane = NavPane.Detail) { key -> OrdersDetailRoute(id = key.id, navigator = navigator) }\n",
    )
    serializer = "        subclass(OrdersDetailKey::class, OrdersDetailKey.serializer())\n"
    assert serializer in text
    text = text.replace(serializer, serializer + "        subclass(OrderDetailKey::class, OrderDetailKey.serializer())\n")
    navigation.write_text(text)

    route = project / "feature/trips/src/main/kotlin" / path / "feature/trips/list/TripsListRoute.kt"
    text = route.read_text()
    call = "is TripsListEffect.OpenDetail -> navigator.navigate(TripsDetailKey(effect.id))"
    assert call in text, "TripsListRoute.kt changed shape"
    text = text.replace(call, "is TripsListEffect.OpenDetail -> navigator.navigate(OrderDetailKey(effect.id))")
    text = text.replace(
        f"import {package}.feature.trips.api.TripsDetailKey\n",
        f"import {package}.feature.orders.api.OrderDetailKey\n",
    )
    route.write_text(text)


if __name__ == "__main__":
    main(Path(sys.argv[1]))
