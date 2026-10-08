"""Navigation invariants, checked against the shell's own log.

Dev builds log one line after every change to the shell state (see `app/src/dev/.../perf`):

    perf.shell: generation=1 tab=0 stacks=[["HomeKey","DetailKey(id=3)"],["FeedKey"]] presented=[] roots=["HomeKey","FeedKey"]

The lists are JSON. Every line must satisfy:

- the selected tab exists;
- no stack is empty;
- no stack (and not the presentation stack) holds the same key twice in a row;
- every tab's stack starts with that tab's root key.
"""

import json
import re
import sys

# Matches both logcat formats: "I perf.shell: …" (threadtime) and "I/perf.shell( 123): …" (brief).
_TAG = re.compile(r"perf\.shell(?:\(\s*\d+\))?: ")
_NAME = re.compile(r"\s*(\w+)=")


def _fields(text):
    """name=value pairs; a value starting with "[" is read as JSON, so keys may hold any text."""
    decoder = json.JSONDecoder()
    fields = {}
    position = 0
    while True:
        name = _NAME.match(text, position)
        if name is None:
            return fields
        position = name.end()
        if text.startswith("[", position):
            fields[name.group(1)], position = decoder.raw_decode(text, position)
        else:
            end = text.find(" ", position)
            end = len(text) if end < 0 else end
            fields[name.group(1)], position = text[position:end], end


def parse_line(line):
    """The state logged on one logcat line, or None when the line is not a shell dump."""
    tag = _TAG.search(line)
    if tag is None:
        return None
    try:
        fields = _fields(line[tag.end():].strip())
        return {
            "generation": int(fields.get("generation", "0")),
            "tab": int(fields["tab"]),
            "stacks": fields["stacks"],
            "presented": fields.get("presented", []),
            "roots": fields["roots"],
        }
    except (KeyError, ValueError):
        return {"unreadable": line.strip()}


def violations(state):
    """What is wrong with one logged state; empty when it is consistent."""
    if "unreadable" in state:
        return [f"unreadable shell line: {state['unreadable']}"]
    found = []
    stacks, roots, tab = state["stacks"], state["roots"], state["tab"]
    if not 0 <= tab < len(stacks):
        found.append(f"selected tab {tab} does not exist ({len(stacks)} tabs)")
    for index, stack in enumerate(stacks):
        if not stack:
            found.append(f"tab {index} has an empty stack")
            continue
        if index < len(roots) and stack[0] != roots[index]:
            found.append(f"tab {index} starts at {stack[0]}, not its root {roots[index]}")
    for name, stack in [(f"tab {i}", s) for i, s in enumerate(stacks)] + [("presented", state["presented"])]:
        for below, above in zip(stack, stack[1:]):
            if below == above:
                found.append(f"{name} holds {above} twice in a row")
    return found


def check(lines):
    """(number of states read, list of violations with the offending line number)."""
    states = 0
    problems = []
    for number, line in enumerate(lines, 1):
        state = parse_line(line)
        if state is None:
            continue
        states += 1
        problems += [f"line {number}: {problem}" for problem in violations(state)]
    return states, problems


def last_state(lines):
    """The most recent state in the log, or None."""
    for line in reversed(lines):
        state = parse_line(line)
        if state is not None and "unreadable" not in state:
            return state
    return None


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: check_nav_log.py <logcat dump>")
    with open(sys.argv[1], errors="ignore") as handle:
        count, found = check(handle.read().splitlines())
    if count == 0:
        sys.exit("no perf.shell lines: install a dev build, which logs the shell state")
    for problem in found:
        print(problem)
    print(f"{count} states, {len(found)} violations")
    sys.exit(1 if found else 0)
