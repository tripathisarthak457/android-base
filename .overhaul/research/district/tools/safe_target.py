"""Finds where to tap for one on-screen label in a `uiautomator dump`, and refuses anything that could act on the
person's behalf.

Standard library only. The dump is untrusted data from another app: it is only read, never executed.

    python -I safe_target.py ui.xml "Filters"            # prints "x y" (device px) and exits 0
    python -I safe_target.py ui.xml "Sort" --index 1     # the second match

The label is matched, case-insensitively, against each node's text and content description. The tap goes to the
nearest clickable node at or above the match. Every text and description inside that clickable node is checked
against DENY: one hit refuses the tap (exit 2) and prints why. A refusal is the end of the scripted scene: the person
does that step by hand, or the scene is skipped. The list is deliberately broad; false refusals are acceptable, a
false acceptance is not.
"""

import argparse
import re
import sys
import xml.etree.ElementTree as ElementTree

DENY = [
    "book", "booking", "buy", "pay", "payment", "proceed", "checkout", "check out", "confirm", "reserve",
    "reservation", "hold", "seat", "seats", "ticket", "tickets", "pass", "order", "cart", "add", "apply", "done",
    "continue", "submit", "ok", "accept", "agree", "allow", "offer", "offers", "coupon", "redeem", "claim", "gift",
    "wallet", "card", "upi", "otp", "verify", "interested", "notify", "remind", "reminder", "like", "follow",
    "rate", "rating", "review", "share", "send", "invite", "call", "join", "register", "rsvp", "subscribe", "upgrade",
    "membership", "pro", "save", "delete", "remove", "log out", "logout", "sign", "account", "profile", "location",
    "detect", "address", "help", "support", "chat", "report", "block",
]
DENY_PATTERN = re.compile(r"\b(" + "|".join(re.escape(word) for word in DENY) + r")\b", re.IGNORECASE)
BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


def texts(node):
    for element in node.iter("node"):
        for key in ("text", "content-desc"):
            value = (element.get(key) or "").strip()
            if value:
                yield value


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dump")
    parser.add_argument("label")
    parser.add_argument("--index", type=int, default=0, help="which match, in document order")
    args = parser.parse_args()

    root = ElementTree.parse(args.dump).getroot()
    parents = {child: parent for parent in root.iter() for child in parent}
    wanted = args.label.strip().lower()
    matches = [node for node in root.iter("node")
               if wanted in (node.get("text") or "").lower() or wanted in (node.get("content-desc") or "").lower()]
    if len(matches) <= args.index:
        sys.exit(f"safe_target: no node {args.index} labelled '{args.label}' ({len(matches)} found)")
    node = matches[args.index]
    target = node
    while target is not None and target.get("clickable") != "true":
        target = parents.get(target)
    if target is None:
        sys.exit(f"safe_target: '{args.label}' is not inside anything clickable")
    seen = sorted(set(texts(target)))
    denied = sorted({match.group(1).lower() for value in seen for match in DENY_PATTERN.finditer(value)})
    if denied:
        print(f"safe_target: REFUSED '{args.label}': the tap target also says {seen}; denied words {denied}",
              file=sys.stderr)
        sys.exit(2)
    bounds = BOUNDS.match(target.get("bounds") or "")
    if not bounds:
        sys.exit(f"safe_target: '{args.label}' has no bounds")
    left, top, right, bottom = (int(value) for value in bounds.groups())
    if right <= left or bottom <= top:
        sys.exit(f"safe_target: '{args.label}' has empty bounds {target.get('bounds')}")
    print(f"{(left + right) // 2} {(top + bottom) // 2}")
    print(f"safe_target: '{args.label}' -> {target.get('class')} {target.get('bounds')} saying {seen}", file=sys.stderr)


if __name__ == "__main__":
    main()
