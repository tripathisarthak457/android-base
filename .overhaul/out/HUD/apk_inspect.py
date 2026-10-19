"""Whether an APK carries any trace of the dev tooling: the inspector (:core:devtools) and the
performance overlay (:core:perfhud).

Five looks per APK, as REQUIREMENTS 40 asks:
  r8        the build's own R8 mapping file: every class a minified APK kept, by its source name;
  dex       dexdump's class descriptors under core/devtools or core/perfhud (R8 renames classes in a
            minified build, so this finds them in a debug build, and the string search below covers
            the minified ones);
  manifest  aapt2's dump of the binary manifest: any component, provider, initializer or permission
            naming either module;
  resources aapt2's resource table: any resource named after either module;
  strings   every dex file, the manifest and resources.arsc searched raw, as UTF-8 and UTF-16LE, for
            strings only the tooling has (its tags, actions, log tag, copy and resource prefixes).

  python -I apk_inspect.py <apk> [<apk>...]     prints a JSON verdict per APK
"""

import json
import os
import re
import subprocess
import sys
import tempfile
import zipfile

BUILD_TOOLS = os.path.join(os.environ.get("ANDROID_HOME", r"C:\Users\crazyhusir\AppData\Local\Android\Sdk"),
                           "build-tools", "37.0.0")
AAPT2 = os.path.join(BUILD_TOOLS, "aapt2.exe")
DEXDUMP = os.path.join(BUILD_TOOLS, "dexdump.exe")

# The two modules, and the seams main code reaches them through (the root's Optional<DevTooling>,
# the build facts it provides, the screen reporting): R8 should drop all three from a shippable build.
MODULES = re.compile(r"core[/.](devtools|perfhud)|tooling[/.]DevTooling|environment[/.]AppEnvironment|ReportCurrentScreen",
                     re.IGNORECASE)
NAMES = re.compile(r"devtools|perfhud", re.IGNORECASE)

MARKERS = [
    # The inspector.
    "core/devtools",
    "core.devtools",
    "devtools_badge",
    "Opens the developer inspector",
    "No requests yet",
    "Received (decoded)",
    # The performance overlay.
    "core/perfhud",
    "core.perfhud",
    "PerfHud",
    "perfhud_",
    "perfhud.action",
    "perf.hud",
    "perf-report-",
]


def tool(*args):
    result = subprocess.run(args, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=300)
    return result.stdout + result.stderr


def dex_classes(apk):
    found = []
    with zipfile.ZipFile(apk) as archive, tempfile.TemporaryDirectory() as scratch:
        for name in archive.namelist():
            if not name.endswith(".dex"):
                continue
            path = os.path.join(scratch, name.replace("/", "_"))
            with open(path, "wb") as handle:
                handle.write(archive.read(name))
            for line in tool(DEXDUMP, path).splitlines():
                match = re.search(r"Class descriptor\s*:\s*'([^']+)'", line)
                if match and MODULES.search(match.group(1)):
                    found.append(match.group(1))
    return sorted(set(found))


def manifest_entries(apk):
    tree = tool(AAPT2, "dump", "xmltree", "--file", "AndroidManifest.xml", apk)
    return sorted({line.strip() for line in tree.splitlines() if NAMES.search(line)})


def resources(apk):
    table = tool(AAPT2, "dump", "resources", apk)
    return sorted({line.strip() for line in table.splitlines() if NAMES.search(line)})


def raw_strings(apk):
    found = {}
    with zipfile.ZipFile(apk) as archive:
        for name in archive.namelist():
            if not (name.endswith(".dex") or name in ("AndroidManifest.xml", "resources.arsc")):
                continue
            data = archive.read(name)
            for marker in MARKERS:
                for encoding in ("utf-8", "utf-16-le"):
                    if marker.encode(encoding) in data:
                        found.setdefault(marker, set()).add(name)
    return {marker: sorted(files) for marker, files in found.items()}


def mapping_classes(apk):
    """Classes R8 kept, by their source names: what dexdump cannot show of a minified APK.

    Read from the build's own mapping file beside the APK (outputs/mapping/<variant>/mapping.txt); an APK
    without one (a debug build) reports none here and is covered by dexdump.
    """
    parts = os.path.normpath(apk).split(os.sep)
    if "apk" not in parts:
        return []
    at = len(parts) - 1 - parts[::-1].index("apk")
    flavor, build_type = parts[at + 1], parts[at + 2]
    mapping = os.path.join(os.sep.join(parts[:at]), "mapping", flavor + build_type[:1].upper() + build_type[1:],
                           "mapping.txt")
    if not os.path.exists(mapping):
        return []
    with open(mapping, encoding="utf-8") as handle:
        return sorted({line.split(" -> ")[0] for line in handle if not line.startswith((" ", "#"))
                       and MODULES.search(line.split(" -> ")[0])})


def inspect(apk):
    traces = {
        "r8_mapping_classes": mapping_classes(apk),
        "dex_classes": dex_classes(apk),
        "manifest": manifest_entries(apk),
        "resources": resources(apk),
        "strings": raw_strings(apk),
    }
    return {"clean": not any(traces.values()), "traces": traces}


def main():
    verdicts = {path: inspect(path) for path in sys.argv[1:]}
    print(json.dumps(verdicts, indent=2))


if __name__ == "__main__":
    main()
