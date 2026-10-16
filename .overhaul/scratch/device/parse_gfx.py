import re, sys, json
def parse(path):
    t = open(path, errors="ignore").read()
    # Only the first "Stats since" block (the app process), before per-window "Profile data"
    out = {}
    def g(pat, cast=float):
        m = re.search(pat, t)
        return cast(m.group(1)) if m else None
    out["frames"] = g(r"Total frames rendered: (\d+)", int)
    m = re.search(r"Janky frames: (\d+) \(([\d.]+)%\)", t)
    out["janky"], out["janky_pct"] = (int(m.group(1)), float(m.group(2))) if m else (None, None)
    m = re.search(r"Janky frames \(legacy\): (\d+) \(([\d.]+)%\)", t)
    out["janky_legacy_pct"] = float(m.group(2)) if m else None
    for p in (50, 90, 95, 99):
        out[f"p{p}"] = g(rf"^{p}th percentile: (\d+)ms", int) if False else None
        m = re.search(rf"\n{p}th percentile: (\d+)ms", t)
        out[f"p{p}"] = int(m.group(1)) if m else None
        m = re.search(rf"\n{p}th gpu percentile: (\d+)ms", t)
        out[f"gpu_p{p}"] = int(m.group(1)) if m else None
    for k in ["Number Missed Vsync", "Number High input latency", "Number Slow UI thread",
              "Number Slow bitmap uploads", "Number Slow issue draw commands", "Number Frame deadline missed",
              "Number Frame deadline missed \\(legacy\\)"]:
        m = re.search(k + r": (\d+)", t)
        out[k.replace("Number ", "").replace("\\", "")] = int(m.group(1)) if m else None
    m = re.search(r"\nHISTOGRAM: (.*)", t)
    if m:
        hist = {}
        for pair in m.group(1).split():
            a, b = pair.split("=")
            hist[int(a.rstrip("ms"))] = int(b)
        tot = sum(hist.values())
        out["hist_total"] = tot
        # fraction over budget at 120Hz (8.33ms) and 60Hz (16.67ms)
        out["pct_over_8ms"] = round(100*sum(v for k,v in hist.items() if k > 8)/tot, 1) if tot else None
        out["pct_over_16ms"] = round(100*sum(v for k,v in hist.items() if k > 16)/tot, 1) if tot else None
        out["pct_over_33ms"] = round(100*sum(v for k,v in hist.items() if k > 33)/tot, 1) if tot else None
        out["max_bucket"] = max([k for k,v in hist.items() if v]) if tot else None
    return out
if __name__ == "__main__":
    for p in sys.argv[1:]:
        print(p.split("/")[-1], json.dumps(parse(p)))
