"""
Feature sets that between them put every pair of features in every on/off pairing.

A marker that only balances when two features are both on, or one on and the other off, is
invisible to the all-features and no-features builds. Testing every pairing directly would take
2^35 projects; a pairwise set takes about a dozen.

    python3 tools/feature_combinations.py              # one JSON list of feature lists
    python3 tools/feature_combinations.py --specs DIR  # also write DIR/combo-N.json specs
"""

from __future__ import annotations

import argparse
import itertools
import json
import random
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "generator"))

from genkit.spec import FEATURES, resolve_features  # noqa: E402

# Fixed, so CI builds the same sets on every run and a failure can be reproduced locally.
SEED = 20260924
CANDIDATES_PER_ROUND = 200


def pairs_of(features: frozenset[str], keys: list[str]) -> set[tuple[str, bool, str, bool]]:
    return {(a, a in features, b, b in features) for a, b in itertools.combinations(keys, 2)}


def combinations() -> list[list[str]]:
    keys = sorted(f.key for f in FEATURES if not f.implied_only)
    # A pairing that `requires` rules out, such as push on with firebase off, can never be built,
    # so only pairings some resolved set actually reaches are asked for.
    rng = random.Random(SEED)
    reachable: set[tuple[str, bool, str, bool]] = set()
    for _ in range(4000):
        chosen = {k for k in keys if rng.random() < 0.5}
        reachable |= pairs_of(frozenset(resolve_features(chosen)), keys)
    for key in keys:
        reachable |= pairs_of(frozenset(resolve_features({key})), keys)
        reachable |= pairs_of(frozenset(resolve_features(set(keys) - {key})), keys)

    uncovered = set(reachable)
    result: list[list[str]] = []
    while uncovered:
        best, best_gain = frozenset(), -1
        for _ in range(CANDIDATES_PER_ROUND):
            density = rng.choice((0.2, 0.5, 0.8))
            candidate = frozenset(resolve_features({k for k in keys if rng.random() < density}))
            gain = len(pairs_of(candidate, keys) & uncovered)
            if gain > best_gain:
                best, best_gain = candidate, gain
        if best_gain <= 0:
            # Random candidates stopped finding the last few; build one around a missing pairing.
            a, a_on, b, b_on = next(iter(sorted(uncovered)))
            wanted = {k for k, on in ((a, a_on), (b, b_on)) if on}
            best = frozenset(resolve_features(wanted))
            if not pairs_of(best, keys) & uncovered:
                uncovered.discard((a, a_on, b, b_on))
                continue
        uncovered -= pairs_of(best, keys)
        result.append(sorted(best))
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.strip().splitlines()[0])
    parser.add_argument("--specs", type=Path, help="write one generator spec per set into this directory")
    args = parser.parse_args()

    sets = combinations()
    if args.specs:
        args.specs.mkdir(parents=True, exist_ok=True)
        for index, features in enumerate(sets):
            spec = {
                "app_name": f"Combo {index}",
                "package_name": "com.example.combo",
                "features": features,
                "feature_modules": ["orders"],
            }
            (args.specs / f"combo-{index}.json").write_text(json.dumps(spec, indent=2) + "\n")
    print(json.dumps(sets))


if __name__ == "__main__":
    main()
