"""Finds where to tap in a `uiautomator dump` of Telegram, and refuses any point that could act for the person.

Standard library only. A dump is untrusted data from another app: it is only read, never executed. It builds on the
District study's safe_target.py (research/district/tools), with rules for a messenger:

    python -I tg_target.py ui.xml "Network usage" --again ui2.xml [--allow-extra allow.txt]
        a labelled control: prints "x y" in device px
    python -I tg_target.py ui.xml "Message" --field --again ui2.xml
        the empty message field of the test chat: prints "x y"

A label is accepted only when it is on ALLOW (or on a line of --allow-extra: the public channel's title and a sheet's
trigger, written from the person's own answers) and a node's text or description equals it, ignoring case and spaces
at the ends. The target is the nearest clickable at or above that node. Refused (exit 2) when:
  - the label is on neither list, or no node carries it exactly;
  - the target or anything inside it is checkable, a switch, a check box, a radio button, a slider, a seek bar or a
    text field: tapping any of them changes a setting, a choice or a draft (--field accepts exactly one empty text
    field whose text or hint is the label);
  - anything inside the target says a word of DENY, or a time of day (a message or post carries one);
  - safe_target.py's own checks fail: another clickable contains the point, or the second dump differs.
Exit 1: bad input. False refusals are acceptable; a false acceptance is not.
"""

import argparse
import os
import re
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "district", "tools"))
import safe_target as base  # noqa: E402  (the District tool, read-only)

# Navigation rows and tabs of the screens the study may use, the chat's own menu button, and the jump arrow. Nothing
# here sends, joins, changes or opens a person's data.
ALLOW = {
    "network usage", "all", "mobile", "wi-fi", "wifi", "roaming",
    "more options", "go to bottom", "scroll to bottom",
}

DENY = [
    "send", "join", "leave", "mute", "unmute", "subscribe", "unsubscribe", "call", "video call", "voice chat",
    "live stream", "share", "forward", "reply", "react", "reaction", "vote", "poll", "report", "block", "unblock",
    "delete", "clear", "remove", "reset", "pin", "unpin", "edit", "save", "done", "apply", "ok", "accept", "agree",
    "allow", "enable", "disable", "turn on", "turn off", "log out", "logout", "sign", "account", "terminate", "add",
    "invite", "create", "new", "start", "bot", "pay", "buy", "stars", "premium", "gift", "boost", "translate",
    "download", "install", "update", "proxy", "archive", "notifications", "privacy", "security", "devices",
    "contacts", "phone", "username", "bio", "birthday", "story", "stories", "comment", "comments", "discuss",
    "discussion", "set", "change", "select", "open", "link", "copy", "favorite", "favourite", "sticker", "attach",
    "camera", "gallery", "location", "contact", "file", "music", "schedule", "silent", "record", "voice", "upgrade",
    "language", "theme", "night", "auto-download", "autoplay", "keep media", "storage usage",
]
DENY_PATTERN = re.compile(r"\b(" + "|".join(re.escape(word) for word in DENY) + r")\b", re.IGNORECASE)
# A time of day ("10:30", "7 pm"). Sizes such as "12.34 MB" are not times, so the dotted form is left out.
TIME_PATTERN = re.compile(r"\b\d{1,2}:\d{2}\b|\b\d{1,2}\s?[ap]\.?\s?m\b", re.IGNORECASE)
CHANGERS = ("switch", "checkbox", "radiobutton", "seekbar", "slider", "edittext", "ratingbar", "togglebutton")

# safe_target's own checks run with both word lists, and with this time pattern: its dotted form would take a data
# size such as "12.34 MB" for a showtime.
base.DENY_PATTERN = re.compile(r"\b(" + "|".join(re.escape(word) for word in base.DENY + DENY) + r")\b", re.IGNORECASE)
base.TIME_PATTERN = TIME_PATTERN


def norm(text):
    return " ".join((text or "").split()).lower()


def allowed_labels(extra_path):
    labels = set(ALLOW)
    if extra_path:
        with open(extra_path, encoding="utf-8") as handle:
            labels |= {norm(line) for line in handle if norm(line)}
    return labels


def changes_something(node):
    for element in node.iter("node"):
        if element.get("checkable") == "true":
            return f"{base.describe(element)} is checkable"
        kind = (element.get("class") or "").lower()
        if any(word in kind for word in CHANGERS):
            return f"{base.describe(element)} is a {element.get('class')}"
    return None


def exact_index(dump, label):
    """The --index for safe_target's substring search that lands on the first node carrying [label] exactly."""
    wanted = norm(label)
    matches = [node for node in dump.nodes
               if wanted in (node.get("text") or "").lower() or wanted in (node.get("content-desc") or "").lower()]
    for index, node in enumerate(matches):
        if norm(node.get("text")) == wanted or norm(node.get("content-desc")) == wanted:
            return index
    raise base.Refused(2, f"no node carries '{label}' exactly")


def target_of(dump, point):
    x, y = point
    hits = dump.clickables_at(x, y)
    return hits[-1] if hits else None


def resolve_label(path, label, extra):
    if norm(label) not in allowed_labels(extra):
        raise base.Refused(2, f"'{label}' is not on the allow-list (ALLOW or --allow-extra)")
    dump = base.Dump(path)
    point, signature, note = base.label_point(dump, label, exact_index(dump, label))
    target = target_of(dump, point)
    if target is None:
        raise base.Refused(2, "no clickable under the point")
    reason = changes_something(target)
    if reason:
        raise base.Refused(2, f"the target changes something: {reason}")
    seen = sorted(set(base.texts(target)))
    found = sorted({m.group(1).lower() for v in seen for m in DENY_PATTERN.finditer(v)}
                   | {m.group(0) for v in seen for m in TIME_PATTERN.finditer(v)})
    if found:
        raise base.Refused(2, f"the target {base.describe(target)} says {seen}; denied {found}")
    return point, signature, note


def resolve_field(path, label):
    dump = base.Dump(path)
    wanted = norm(label)
    fields = [node for node in dump.nodes if "edittext" in (node.get("class") or "").lower()]
    named = [node for node in fields if wanted in (norm(node.get("text")), norm(node.get("hint")),
                                                    norm(node.get("content-desc")))]
    if len(fields) != 1 or len(named) != 1:
        raise base.Refused(2, f"expected exactly one text field labelled '{label}': {len(fields)} fields, "
                              f"{len(named)} labelled so")
    field = named[0]
    text, hint = norm(field.get("text")), norm(field.get("hint"))
    if text and text != wanted and text != hint:
        raise base.Refused(2, f"the field holds text ('{field.get('text')}'): a draft exists, stop and ask")
    box = base.bounds(field)
    if box is None:
        raise base.Refused(2, "the field has no usable bounds")
    x, y = (box[0] + box[2]) // 2, (box[1] + box[3]) // 2
    others = [node for node in dump.clickables_at(x, y) if node is not field and node not in dump.ancestors(field)]
    if others:
        raise base.Refused(2, f"({x}, {y}) is also in {'; '.join(base.describe(node) for node in others)}")
    return (x, y), (x, y, box), f"field {base.describe(field)}"


def main():
    parser = base.Parser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("dump")
    parser.add_argument("label")
    parser.add_argument("--again", help="a second dump of the same screen, taken a few seconds after the first")
    parser.add_argument("--allow-extra", help="a file of extra allowed labels, one per line")
    parser.add_argument("--field", action="store_true", help="the one empty text field labelled so")
    args = parser.parse_args()

    def resolve(path):
        return resolve_field(path, args.label) if args.field else resolve_label(path, args.label, args.allow_extra)

    try:
        point, signature, note = resolve(args.dump)
        if args.again:
            try:
                again_point, again_signature, _ = resolve(args.again)
            except base.Refused as refusal:
                raise base.Refused(2, f"the screen changed between the two dumps: {refusal}") from refusal
            if not base.same(signature, again_signature):
                raise base.Refused(2, f"the screen changed between the two dumps: {signature} then {again_signature}")
            point = again_point
    except base.Refused as refusal:
        print(f"tg_target: REFUSED: {refusal}", file=sys.stderr)
        sys.exit(refusal.code)
    except (OSError, base.ElementTree.ParseError) as error:
        print(f"tg_target: ERROR: {error}", file=sys.stderr)
        sys.exit(1)
    print(f"tg_target: {note}", file=sys.stderr)
    print(f"{point[0]} {point[1]}")


if __name__ == "__main__":
    main()
