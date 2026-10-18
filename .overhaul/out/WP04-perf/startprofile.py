"""startprofile.py <samples.csv> <stacks.csv> <package=mapping.txt>... [--phase P] [--top N] [--self]

Where two apps' cold starts spend their main thread, from startup-ab-perf.win.sh's capture: startsamples.sql's
samples and startstacks.sql's callsites, R8 names put back from each build's mapping.txt. Prints, per phase, the
samples per start of each app's frames (inclusive, or self with --self) and the difference, largest first.
A sample is one period of the sampling rate (2 ms at 500 Hz) of the main thread running."""
import collections, csv, re, sys

args = [a for a in sys.argv[1:]]
opts = {"--phase": None, "--top": "40"}
flags = set()
i = 0
rest = []
while i < len(args):
    if args[i] in opts:
        opts[args[i]] = args[i + 1]; i += 2
    elif args[i].startswith("--"):
        flags.add(args[i]); i += 1
    else:
        rest.append(args[i]); i += 1
samples_csv, stacks_csv, maps = rest[0], rest[1], dict(m.split("=", 1) for m in rest[2:])


def load_mapping(path):
    """R8 mapping: obfuscated class -> original, and (original class, obfuscated method) -> original method."""
    classes, methods = {}, collections.defaultdict(set)
    current = None
    for line in open(path, encoding="utf-8", errors="replace"):
        if line.startswith("#"):
            continue
        if not line.startswith(" "):
            m = re.match(r"(\S+) -> (\S+):", line)
            if m:
                current = m.group(1)
                classes[m.group(2)] = current
            continue
        m = re.match(r"\s+(?:[\d:]+:)?\S+ ([\w$<>-]+)\(.*\)(?::[\d:]+)? -> ([\w$<>-]+)", line)
        if m and current:
            methods[(current, m.group(2))].add(m.group(1))
    return classes, methods


mappings = {pkg: load_mapping(path) for pkg, path in maps.items()}
frame_re = re.compile(r"^(?:(?P<ret>[\w$.\[\]]+) )?(?P<cls>[\w$.]+)\.(?P<meth>[\w$<>-]+)(?P<args>\(.*\))?$")


def deobf(pkg, name, mapping_name):
    if pkg not in mappings or not re.search(r"\.(apk|odex|oat|vdex|dex)", mapping_name or "") and "base.apk" not in (mapping_name or ""):
        return name
    classes, methods = mappings[pkg]
    m = frame_re.match(name)
    if not m:
        return name
    cls = classes.get(m.group("cls"), m.group("cls"))
    meth = m.group("meth")
    names = methods.get((cls, meth))
    if names:
        meth = "|".join(sorted(names))
    return f"{cls}.{meth}"


stacks = {}
for row in csv.DictReader(open(stacks_csv, encoding="utf-8", errors="replace")):
    stacks[int(row["id"])] = (int(row["parent"]), row["frame"], row["mapping"])

samples = list(csv.DictReader(open(samples_csv, encoding="utf-8", errors="replace")))
starts = collections.defaultdict(set)
incl = collections.defaultdict(collections.Counter)
for s in samples:
    pkg = s["package"]
    starts[pkg].add(s["startup_id"])
    if opts["--phase"] and s["phase"] not in opts["--phase"].split(","):
        continue
    cs = int(s["callsite_id"]) if s["callsite_id"] not in ("", "[NULL]") else -1
    seen = set()
    first = True
    while cs != -1 and cs in stacks:
        parent, frame, mapping = stacks[cs]
        name = deobf(pkg, frame, mapping)
        if "--self" in flags:
            incl[pkg][name] += 1
            break
        if name not in seen:
            incl[pkg][name] += 1
            seen.add(name)
        cs = parent
        first = False
    incl[pkg]["<all>"] += 1 if "--self" not in flags else 0

pkgs = sorted(incl)
n = {p: len(starts[p]) for p in pkgs}
print("starts:", {p: n[p] for p in pkgs}, "phase:", opts["--phase"] or "all", "mode:", "self" if "--self" in flags else "inclusive")
names = set().union(*(incl[p] for p in pkgs))
rows = []
for name in names:
    per = [incl[p][name] / n[p] for p in pkgs]
    rows.append((per[-1] - per[0] if len(per) > 1 else per[0], per, name))
rows.sort(key=lambda r: -abs(r[0]))
print(f"{'diff':>7} " + " ".join(f"{p[-22:]:>22}" for p in pkgs) + "  frame (samples per start)")
for d, per, name in rows[: int(opts["--top"])]:
    print(f"{d:7.1f} " + " ".join(f"{v:22.1f}" for v in per) + "  " + name[:150])
