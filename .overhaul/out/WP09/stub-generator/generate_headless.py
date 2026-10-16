"""Stub of generate_headless.py (MASTER-PLAN A1.8) for WP09's smoke run."""
import json, os, re, sys, tempfile, time, zipfile
args = sys.argv[1:]
assert args[0] == "--target", args
target, out = args[1], args[2]
spec = json.load(sys.stdin)
if not re.fullmatch(r"[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+", spec.get("package_name", "com.example.x")):
    json.dump({"ok": False, "kind": "invalid", "error": "The package name is not valid.", "field": "package_name"}, sys.stdout)
    sys.exit(2)
if spec.get("app_name") == "Crash":
    json.dump({"ok": False, "kind": "internal", "error": "The generator failed.", "detail": "Traceback: KeyError 'x'"}, sys.stdout)
    sys.exit(1)
# Proves TMPDIR points into the API's work directory.
staging = tempfile.mkdtemp(prefix="stub-")
with zipfile.ZipFile(out, "w") as z:
    info = zipfile.ZipInfo("Proj/gradlew"); info.external_attr = 0o755 << 16
    z.writestr(info, "#!/bin/sh\n")
    z.writestr("Proj/tmpdir.txt", staging + "\n" + os.environ.get("HOME", ""))
features = spec.get("features", ["network", "auth", "forms", "settings"])
json.dump({"ok": True, "target": target, "project_name": "Proj", "elapsed_ms": 12, "zip_bytes": os.path.getsize(out),
           "resolved_features": sorted(set(features) | ({"forms"} if "auth" in features else set())),
           "notes": [] if "schema" in spec else ["v1 spec upgraded"], "warnings": [], "ignored_fields": [],
           "generator_commit": "stub000"}, sys.stdout)
