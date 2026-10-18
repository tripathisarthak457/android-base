"""heapsum.py <heapprof out-dir>: what each process of a heapprofd capture (heapprof.win.sh) still held at the end,
by the library that asked for it: each sampled allocation's stack is walked from malloc up to the first frame
outside the allocator and the C/C++ runtimes, and the unreleased bytes are summed by that frame's library and,
for the largest libraries, by function. Runs trace_processor through tp.sh."""
import collections, csv, io, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
SKIP = ("libc.so", "libc++.so", "libheapprofd", "libdl.so", "libm.so", "libutils.so", "libcutils.so", "libbase.so",
        "liblog.so")


def tp(query, trace, name):
    qpath = os.path.join(HERE, "sql", name)
    with open(qpath, "w", encoding="utf-8") as f:
        f.write(query)
    out = subprocess.run(["bash", os.path.join(HERE, "tp.sh"), qpath, trace], capture_output=True, text=True).stdout
    return list(csv.DictReader(io.StringIO(out.strip())))


trace = os.path.join(sys.argv[1], "trace.perfetto-trace")
held = tp("""select a.upid, p.name proc, p.pid, a.callsite_id, sum(a.size) bytes from heap_profile_allocation a
join process p using(upid) group by 1, 4 having bytes > 0""", trace, "heapheld.sql")
sites = tp("""select c.id, c.parent_id, f.name fn, m.name lib from stack_profile_callsite c
join stack_profile_frame f on c.frame_id = f.id join stack_profile_mapping m on f.mapping = m.id""", trace, "heapsites.sql")
site = {r["id"]: r for r in sites}


def owner(cid):
    chain = []
    while cid and cid in site:
        chain.append(site[cid])
        cid = site[cid]["parent_id"]
    for frame in chain:
        lib = os.path.basename(frame["lib"] or "?")
        if not lib.startswith(SKIP):
            return lib, frame["fn"]
    return "?", "?"


procs = collections.defaultdict(lambda: (collections.Counter(), collections.Counter()))
for r in held:
    lib, fn = owner(r["callsite_id"])
    libs, fns = procs[(r["proc"], r["pid"])]
    libs[lib] += int(r["bytes"])
    fns[(lib, fn)] += int(r["bytes"])
for (proc, pid), (libs, fns) in procs.items():
    print(f"== {proc} ({pid}): {sum(libs.values()) / 2**20:.1f} MB held at the end (sampled)")
    for lib, b in libs.most_common(14):
        print(f"   {b / 2**20:7.2f} MB  {lib}")
        for (l, fn), fb in fns.most_common():
            if l == lib and fb > 0.25 * 2**20:
                print(f"            {fb / 2**20:6.2f}  {fn[:110]}")
