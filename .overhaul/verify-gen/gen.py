import sys, json
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/android-base/generator")
sys.dont_write_bytecode = True
from pathlib import Path
from genkit import build as builder
from genkit.spec import ProjectSpec, KeystoreSpec

def gen(out, **kw):
    ks = tuple(KeystoreSpec(**k) for k in kw.pop("keystores", []))
    spec = ProjectSpec(keystores=ks, **{k: (frozenset(v) if k=="features" else tuple(v) if k in ("feature_modules","languages") else v) for k,v in kw.items()}).validated()
    return builder.build(spec, Path(out), zip_output=False, icon_source=kw.get("icon"))

if __name__ == "__main__":
    out = sys.argv[1]
    kw = json.loads(sys.argv[2])
    try:
        r = gen(out, **kw)
        print("OK", r.warnings)
    except Exception as e:
        print("ERR", type(e).__name__, e)
