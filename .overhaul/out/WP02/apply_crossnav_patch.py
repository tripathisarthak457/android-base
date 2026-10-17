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
    namespace = re.search(r'namespace\s*=\s*"([\w.]+)\.feature\.orders"', (project / "feature/orders/build.gradle.kts").read_text(encoding="utf-8"))
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
        "data class OrderDetailKey(val id: String) : AppNavKey\n",
        encoding="utf-8",
        newline="\n",
    )

    navigation = project / "feature/orders/src/main/kotlin" / path / "feature/orders/navigation/OrdersNavigation.kt"
    text = navigation.read_text(encoding="utf-8")
    text = text.replace(
        f"import {package}.feature.orders.api.OrdersDetailKey\n",
        f"import {package}.feature.orders.api.OrderDetailKey\nimport {package}.feature.orders.api.OrdersDetailKey\n",
    )
    # The scaffold writes ktlint's layout, which wraps the entry past 100 columns.
    entry = (
        "        entry<OrdersDetailKey>(pane = NavPane.Detail) { key ->\n"
        "            OrdersDetailRoute(id = key.id, navigator = navigator)\n"
        "        }\n"
    )
    assert entry in text, "OrdersNavigation.kt changed shape"
    text = text.replace(entry, entry + entry.replace("entry<OrdersDetailKey>", "entry<OrderDetailKey>"))
    serializer = "        subclass(OrdersDetailKey::class, OrdersDetailKey.serializer())\n"
    assert serializer in text
    text = text.replace(serializer, serializer + "        subclass(OrderDetailKey::class, OrderDetailKey.serializer())\n")
    navigation.write_text(text, encoding="utf-8", newline="\n")

    route = project / "feature/trips/src/main/kotlin" / path / "feature/trips/list/TripsListRoute.kt"
    text = route.read_text(encoding="utf-8")
    call = "navigator.navigate(TripsDetailKey(effect.id))"
    assert text.count(call) == 1, "TripsListRoute.kt changed shape"
    text = text.replace(call, "navigator.navigate(OrderDetailKey(effect.id))")
    text = text.replace(
        f"import {package}.feature.trips.api.TripsDetailKey\n",
        f"import {package}.feature.orders.api.OrderDetailKey\n",
    )
    route.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    main(Path(sys.argv[1]))
