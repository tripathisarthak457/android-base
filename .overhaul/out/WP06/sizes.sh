#!/usr/bin/env bash
# Step 16: devRelease arm64 APK size and total dex size, before (the integration branch) and after
# (wp/WP06), for standard, everything, and everything without offlinequeue.
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP06; L=$OV/bin/locked; O=$OV/out/WP06/size; P=$OV/perf/WP06
BASE=/home/crazyhusir/Documents/projects/tools/android-base
mkdir -p $O $P
python3 - "$W/generator/tests/fixtures/v2/everything.json" "$O/everything-noqueue.json" <<'PY'
import json, sys
spec = json.load(open(sys.argv[1]))
spec["features"] = [f for f in spec["features"] if f != "offlinequeue"]
json.dump(spec, open(sys.argv[2], "w"), indent=2)
PY
build() {  # name generator-dir spec
  local name=$1 gen=$2 spec=$3
  (cd $gen && python3 create_project.py --spec $spec --set package_name=com.example.wp06size.${name//-/} --out $O/$name --no-zip --force)
  (cd $O/$name && $L gradle ./gradlew :app:assembleDevRelease --console=plain > $O/$name.log 2>&1)
}
for target in before-standard before-everything after-standard after-everything after-everything-noqueue; do
  if [ "${ONLY:-}" != "" ] && [[ " $ONLY " != *" $target "* ]]; then continue; fi
  case $target in
    before-standard) build $target $BASE/generator $BASE/generator/tests/fixtures/v2/standard.json ;;
    before-everything) build $target $BASE/generator $BASE/generator/tests/fixtures/v2/everything.json ;;
    after-standard) build $target $W/generator $W/generator/tests/fixtures/v2/standard.json ;;
    after-everything) build $target $W/generator $W/generator/tests/fixtures/v2/everything.json ;;
    after-everything-noqueue) build $target $W/generator $O/everything-noqueue.json ;;
  esac
done
if [ "${ONLY:-}" != "" ]; then echo "BUILT $ONLY"; exit 0; fi
python3 - $O > $P/sizes.md <<'PY'
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
PY
cat $P/sizes.md
echo SIZES OK
