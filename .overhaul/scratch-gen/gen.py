"""Generate a spec into .overhaul/out/gen-<name> using genkit directly (no Gradle)."""
import json, sys, time, shutil, traceback
from pathlib import Path
GEN = Path("/home/crazyhusir/Documents/projects/tools/android-base/generator")
sys.path.insert(0, str(GEN))
from genkit.build import build
from genkit.spec import ProjectSpec, KeystoreSpec, FEATURES, resolve_features, PRESETS
OUT = Path("/home/crazyhusir/Documents/projects/tools/.overhaul/out")

def make(name, fields, validate=True):
    dest = OUT / f"gen-{name}"
    if dest.exists(): shutil.rmtree(dest)
    f = dict(fields)
    if "features" in f: f["features"] = frozenset(f["features"])
    if "feature_modules" in f: f["feature_modules"] = tuple(f["feature_modules"])
    if "languages" in f: f["languages"] = tuple(f["languages"])
    if "keystores" in f: f["keystores"] = tuple(KeystoreSpec(**k) for k in f["keystores"])
    spec = ProjectSpec(**f)
    if validate: spec = spec.validated()
    t = time.perf_counter()
    res = build(spec, dest, zip_output=False)
    return spec, dest, res, time.perf_counter() - t

if __name__ == "__main__":
    name = sys.argv[1]; fields = json.loads(sys.argv[2])
    try:
        spec, dest, res, el = make(name, fields)
        print(json.dumps({"ok": True, "dest": str(dest), "secs": round(el,2), "warnings": res.warnings}))
    except Exception as e:
        print(json.dumps({"ok": False, "error": f"{type(e).__name__}: {e}", "tb": traceback.format_exc()[-1500:]}))
