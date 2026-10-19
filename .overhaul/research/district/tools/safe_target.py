"""Finds where to tap in a `uiautomator dump`, and refuses any point that could act on the person's behalf.

Standard library only. A dump is untrusted data from another app: it is only read, never executed.

    python -I safe_target.py ui.xml "Filters" --again ui2.xml
        a labelled control: prints "x y" in device px
    python -I safe_target.py ui.xml --dim --again ui2.xml --status-bottom 110 --density 2.75
        the dim above an open sheet: prints "x y" in device px

Exit 0: the point is printed. Exit 2: refused, with the reason on stderr; the person taps by hand or the step is
skipped, and a refused --dim closes the sheet with Back instead. Exit 3 (--dim only): a dump shows no open sheet,
so nothing is pressed. Exit 1: bad input. False refusals are acceptable; a false acceptance is not.

A labelled control. The label is matched, case-insensitively, against each node's text and content description,
and the target is the nearest clickable node at or above the match. The tap point is the centre of the matched
node, kept inside the target. Refused when:
  - any text or description inside the target carries a word of DENY or a time of day (a showtime);
  - any clickable node other than the target contains the point: a scrim or bar laid over it, a clickable around
    it, or a control inside it, any of which could take the tap instead;
  - with --again, a second dump taken a few seconds later gives another target or point (a banner or a carousel
    that moves on its own).

The dim (--dim). Needs a scrim, a clickable node over at least half the window that is Views' touch_outside or
carries a sheet's close label ("Close sheet" in Material 3's Compose sheet and in the template), and a sheet
container drawn above it: a node at least half the window wide reaching the bottom fifth of the window. The
sheet's top is the highest top of everything drawn above the scrim. The point is the window's horizontal centre,
halfway between the status bar's bottom and 48 dp above the sheet's top. Refused when that band is under 48 dp,
when the point is outside the scrim, when anything drawn above the scrim (a later node in the dump: a control
inside it, the sheet, a bar) contains the point, so that the scrim is the topmost clickable there, or, with
--again, when the sheet moved between the dumps. A label that names the scrim itself is refused in label mode.
"""

import argparse
import re
import sys
import xml.etree.ElementTree as ElementTree

DENY = [
    "book", "booking", "buy", "pay", "payment", "proceed", "checkout", "check out", "confirm", "reserve",
    "reservation", "hold", "seat", "seats", "ticket", "tickets", "pass", "order", "cart", "add", "apply", "done",
    "continue", "submit", "ok", "accept", "agree", "allow", "enable", "turn on", "permission", "offer", "offers",
    "coupon", "redeem", "claim", "gift", "wallet", "card", "upi", "otp", "verify", "interested", "notify", "remind",
    "reminder", "like", "follow", "rate", "rating", "review", "share", "send", "invite", "call", "join", "register",
    "rsvp", "subscribe", "upgrade", "membership", "pro", "save", "delete", "remove", "log out", "logout", "log in",
    "login", "sign", "account", "profile", "location", "detect", "address", "help", "support", "chat", "report",
    "block", "showtime", "showtimes", "rent", "download", "install",
]
DENY_PATTERN = re.compile(r"\b(" + "|".join(re.escape(word) for word in DENY) + r")\b", re.IGNORECASE)
# A showtime ("10:30 AM", "22.15", "7 pm"). Ratings with two decimals and prices are refused too: harmless.
TIME_PATTERN = re.compile(r"\b\d{1,2}\s?[:.]\s?\d{2}\b|\b\d{1,2}\s?[ap]\.?\s?m\b", re.IGNORECASE)
SCRIM_LABELS = {"close sheet", "dismiss sheet", "close", "dismiss"}
BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")
CLEAR_DP = 48.0
SAME_PX = 2


class Refused(Exception):
    def __init__(self, code, reason):
        super().__init__(reason)
        self.code = code


def bounds(node):
    match = BOUNDS.match(node.get("bounds") or "")
    if not match:
        return None
    left, top, right, bottom = (int(value) for value in match.groups())
    return (left, top, right, bottom) if right > left and bottom > top else None


def contains(box, x, y):
    return box is not None and box[0] <= x < box[2] and box[1] <= y < box[3]


def label_of(node):
    return (node.get("content-desc") or node.get("text") or "").strip()


def describe(node):
    return f"{node.get('class')} {node.get('bounds')} '{label_of(node)}'"


def texts(node):
    for element in node.iter("node"):
        for key in ("text", "content-desc"):
            value = (element.get(key) or "").strip()
            if value:
                yield value


def denied_in(values):
    found = {match.group(1).lower() for value in values for match in DENY_PATTERN.finditer(value)}
    found |= {match.group(0) for value in values for match in TIME_PATTERN.finditer(value)}
    return sorted(found)


class Dump:
    def __init__(self, path):
        self.root = ElementTree.parse(path).getroot()
        self.nodes = list(self.root.iter("node"))
        self.order = {node: index for index, node in enumerate(self.nodes)}
        self.parents = {child: parent for parent in self.root.iter() for child in parent}

    def ancestors(self, node):
        found = []
        parent = self.parents.get(node)
        while parent is not None and parent.tag == "node":
            found.append(parent)
            parent = self.parents.get(parent)
        return found

    def clickables_at(self, x, y):
        return [node for node in self.nodes if node.get("clickable") == "true" and contains(bounds(node), x, y)]

    def window_of(self, node):
        chain = [node] + self.ancestors(node)
        return bounds(chain[-1])


def label_point(dump, label, index):
    wanted = label.strip().lower()
    matches = [node for node in dump.nodes
               if wanted in (node.get("text") or "").lower() or wanted in (node.get("content-desc") or "").lower()]
    if len(matches) <= index:
        raise Refused(1, f"no node {index} labelled '{label}' ({len(matches)} found)")
    match = matches[index]
    target = match if match.get("clickable") == "true" else None
    if target is None:
        target = next((node for node in dump.ancestors(match) if node.get("clickable") == "true"), None)
    if target is None:
        raise Refused(2, f"'{label}' is not inside anything clickable")
    if is_scrim(dump, target):
        raise Refused(2, f"'{label}' is a sheet's scrim {describe(target)}: its centre may lie on the sheet; use --dim")
    seen = sorted(set(texts(target)))
    denied = denied_in(seen)
    if denied:
        raise Refused(2, f"the target {describe(target)} also says {seen}; denied {denied}")
    own, outer = bounds(match), bounds(target)
    if own is None or outer is None:
        raise Refused(2, f"'{label}' or its target has no usable bounds")
    left, top = max(own[0], outer[0]), max(own[1], outer[1])
    right, bottom = min(own[2], outer[2]), min(own[3], outer[3])
    if right <= left or bottom <= top:
        raise Refused(2, f"'{label}' {match.get('bounds')} lies outside its target {target.get('bounds')}")
    x, y = (left + right) // 2, (top + bottom) // 2
    others = [node for node in dump.clickables_at(x, y) if node is not target]
    if others:
        inside = set(target.iter("node"))
        around = set(dump.ancestors(target))
        relation = ["inside the target" if node in inside else "around the target" if node in around
                    else "over or beside it" for node in others]
        listed = "; ".join(f"{describe(node)} ({where})" for node, where in zip(others, relation))
        raise Refused(2, f"({x}, {y}) is also in another clickable: {listed}")
    note = f"'{label}' -> {describe(target)} saying {seen}"
    return (x, y), (x, y, outer, tuple(seen)), note


def area(box):
    return (box[2] - box[0]) * (box[3] - box[1])


def is_scrim(dump, node):
    box = bounds(node)
    window = dump.window_of(node)
    if node.get("clickable") != "true" or box is None or window is None or area(box) < 0.5 * area(window):
        return False
    return (node.get("resource-id") or "").endswith("/touch_outside") or label_of(node).lower() in SCRIM_LABELS


def dim_point(dump, status_bottom, density):
    scrims = [node for node in dump.nodes if is_scrim(dump, node)]
    if not scrims:
        raise Refused(3, "no scrim in the dump: no modal sheet is open here")
    scrim = scrims[-1]
    window = dump.window_of(scrim)
    width, height = window[2] - window[0], window[3] - window[1]
    beneath = set(scrim.iter("node"))
    above = [node for node in dump.nodes[dump.order[scrim] + 1:] if node not in beneath and bounds(node) is not None]
    containers = [node for node in above
                  if bounds(node)[2] - bounds(node)[0] >= 0.5 * width and bounds(node)[3] >= window[1] + 0.8 * height]
    if not containers:
        raise Refused(2, f"a scrim {describe(scrim)} but no sheet container drawn above it")
    sheet_top = min(bounds(node)[1] for node in above)
    clear = CLEAR_DP * density
    low = max(status_bottom, window[1])
    high = sheet_top - clear
    if high - low < clear:
        raise Refused(2, f"the band between the status bar ({low}) and 48 dp above the sheet ({sheet_top}) is "
                         f"{max(0, round(high - low))} px, under 48 dp ({round(clear)} px)")
    x, y = window[0] + width // 2, int((low + high) // 2)
    if not contains(bounds(scrim), x, y):
        raise Refused(2, f"({x}, {y}) is outside the scrim {scrim.get('bounds')}")
    # Drawn later is drawn above: a clickable after the scrim in the dump (inside it, or a later layer) would take
    # the tap; those before it, the screen behind and the scrim's own parents, lie under it.
    others = [node for node in dump.clickables_at(x, y) if dump.order[node] > dump.order[scrim]]
    if others:
        raise Refused(2, f"({x}, {y}) is also in {'; '.join(describe(node) for node in others)}")
    covering = [node for node in above if contains(bounds(node), x, y)]
    if covering:
        raise Refused(2, f"({x}, {y}) is under {'; '.join(describe(node) for node in covering)}, drawn above the scrim")
    note = f"dim at ({x}, {y}): scrim {describe(scrim)}, sheet top {sheet_top}, status bar bottom {status_bottom}"
    return (x, y), (x, y, bounds(scrim), sheet_top), note


def same(first, second):
    if len(first) != len(second):
        return False
    for a, b in zip(first, second):
        if isinstance(a, int) and isinstance(b, int):
            if abs(a - b) > SAME_PX:
                return False
        elif isinstance(a, tuple) and isinstance(b, tuple) and len(a) == len(b)                 and all(isinstance(v, int) for v in a + b):
            if any(abs(p - q) > SAME_PX for p, q in zip(a, b)):
                return False
        elif a != b:
            return False
    return True


def resolve(args, path):
    dump = Dump(path)
    if args.dim:
        return dim_point(dump, args.status_bottom, args.density)
    return label_point(dump, args.label, args.index)


class Parser(argparse.ArgumentParser):
    def error(self, message):
        self.print_usage(sys.stderr)
        print(f"safe_target: ERROR: {message}", file=sys.stderr)
        sys.exit(1)


def main():
    parser = Parser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dump")
    parser.add_argument("label", nargs="?")
    parser.add_argument("--index", type=int, default=0, help="which match, in document order")
    parser.add_argument("--again", help="a second dump of the same screen, taken a few seconds after the first")
    parser.add_argument("--dim", action="store_true", help="the dim above an open sheet instead of a label")
    parser.add_argument("--status-bottom", type=int, help="the status bar's bottom edge in px (--dim)")
    parser.add_argument("--density", type=float, help="px per dp, wm density / 160 (--dim)")
    args = parser.parse_args()
    if args.dim == (args.label is not None):
        parser.error("give a label or --dim, not both")
    if args.dim and (args.status_bottom is None or not args.density):
        parser.error("--dim needs --status-bottom and --density")

    try:
        point, signature, note = resolve(args, args.dump)
        if args.again:
            try:
                again_point, again_signature, _ = resolve(args, args.again)
            except Refused as refusal:
                if refusal.code == 1:
                    raise Refused(2, f"the screen changed between the two dumps: {refusal}") from refusal
                raise
            if not same(signature, again_signature):
                raise Refused(2, f"the screen changed between the two dumps: {signature} then {again_signature}")
            point = again_point
    except Refused as refusal:
        verdict = "NO SHEET" if refusal.code == 3 else "REFUSED" if refusal.code == 2 else "ERROR"
        print(f"safe_target: {verdict}: {refusal}", file=sys.stderr)
        sys.exit(refusal.code)
    print(f"{point[0]} {point[1]}")
    print(f"safe_target: {note}", file=sys.stderr)


if __name__ == "__main__":
    main()
