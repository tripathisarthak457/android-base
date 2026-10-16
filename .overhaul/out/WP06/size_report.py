#!/usr/bin/env python3
"""Step 16 table: APK, dex and per-package dex bytes of the five size builds. Usage: size_report.py <size dir>."""
import subprocess, sys, zipfile
from pathlib import Path
root = Path(sys.argv[1])
analyzer = Path.home() / "Android/Sdk/cmdline-tools/latest/bin/apkanalyzer"
packages = ["androidx.room", "androidx.room3", "androidx.sqlite", "androidx.work"]

def package_bytes(apk, mapping):
    # Defined dex bytes per package, names restored through R8's mapping.
    out = subprocess.run([str(analyzer), "dex", "packages", "--defined-only", "--proguard-mappings", str(mapping), str(apk)],
                         capture_output=True, text=True, check=True).stdout
    sizes = {}
    for line in out.splitlines():
        parts = line.split()
        if parts and parts[0] == "P":
            sizes[parts[-1]] = int(parts[-2])
    return sizes

print("devRelease, arm64-v8a split, R8 on. Package columns are defined dex bytes (apkanalyzer, mapping applied).")
print()
print("| Build | APK (bytes) | dex, uncompressed (bytes) | dex files | " + " | ".join(f"`{p}`" for p in packages) + " |")
print("|---|---:|---:|---:|" + "---:|" * len(packages))
for name in ["before-standard", "after-standard", "before-everything", "after-everything", "after-everything-noqueue"]:
    out = root / name / "app/build/outputs"
    apk = next((out / "apk/dev/release").glob("*arm64-v8a*.apk"))
    with zipfile.ZipFile(apk) as z:
        dex = [i for i in z.infolist() if i.filename.endswith(".dex")]
    sizes = package_bytes(apk, out / "mapping/devRelease/mapping.txt")
    cells = " | ".join(f"{sizes.get(p, 0):,}" for p in packages)
    print(f"| {name} | {apk.stat().st_size:,} | {sum(i.file_size for i in dex):,} | {len(dex)} | {cells} |")
