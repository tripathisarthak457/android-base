"""Whether an APK carries any trace of the performance overlay.

Looks in every dex file, the binary manifest and the resource table for strings only the overlay
has: its package and classes (which R8 may rename, so its string constants too), its receiver,
its logcat tag, its actions and its resource names. Each is searched as UTF-8 and as UTF-16LE,
the encodings dex and binary XML use.

  python -I apk_inspect.py <apk> [<apk>...]     prints a JSON verdict per APK
"""

import json
import sys
import zipfile

MARKERS = [
    "core/perfhud",
    "core.perfhud",
    "PerfHud",
    "perfhud_",
    "ReportReceiver",
    "perfhud.action",
    "perf.hud",
    "FrameIntake",
    "perf-report-",
]


def inspect(path):
    found = {}
    with zipfile.ZipFile(path) as apk:
        for name in apk.namelist():
            if not (name.endswith(".dex") or name in ("AndroidManifest.xml", "resources.arsc")):
                continue
            data = apk.read(name)
            for marker in MARKERS:
                for encoding in ("utf-8", "utf-16-le"):
                    if marker.encode(encoding) in data:
                        found.setdefault(marker, set()).add(name)
    return {marker: sorted(files) for marker, files in found.items()}


def main():
    verdicts = {}
    for path in sys.argv[1:]:
        traces = inspect(path)
        verdicts[path] = {"clean": not traces, "traces": traces}
    print(json.dumps(verdicts, indent=2))


if __name__ == "__main__":
    main()
