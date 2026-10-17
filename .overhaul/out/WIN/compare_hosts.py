"""
Proves the fixed generator writes, on Windows, the bytes the generator writes on Linux.

    python compare_hosts.py OLD_GENERATOR NEW_GENERATOR OUT

There is no Linux machine here, so the Linux bytes are derived: the integration head's generator
(OLD) writes exactly the Linux bytes except that Path.write_text turns "\n" into "\r\n" on Windows.
So the Linux output is OLD's Windows output with CRLF folded to LF in every file whose template
source has no CR. NEW's Windows output must equal that byte for byte, have the same file list, and
its zip must mark exactly the same entries executable. Also reports how many files OLD broke.
"""

from __future__ import annotations

import json
import subprocess
import sys
import zipfile
from pathlib import Path

SPECS = {
    "standard": None,  # tests/fixtures/v2/standard.json, the acceptance spec
    "everything-brand": {
        "schema": 2, "platform": "android", "app_name": "Trail Notes", "package_name": "com.example.win.trail",
        "architecture": "compact", "features": "everything", "feature_modules": ["orders", "order_history"],
        "languages": "all", "design_style": "Editorial", "motion_style": "Calm",
        "accent_colour": "#1F7A5C", "secondary_colour": "#445566", "tertiary_colour": "#AA3311",
    },
}


def generate(generator: Path, spec: Path, out: Path, *extra: str) -> None:
    command = [sys.executable, str(generator / "create_project.py"), "--spec", str(spec), "--out", str(out), "--force", "--json", *extra]
    result = subprocess.run(command, capture_output=True, text=True, cwd=generator)
    if result.returncode != 0:
        raise SystemExit(f"{' '.join(command)} failed:\n{result.stdout}\n{result.stderr}")


def tree(root: Path) -> dict[str, bytes]:
    return {path.relative_to(root).as_posix(): path.read_bytes() for path in sorted(root.rglob("*")) if path.is_file()}


def without_stamp(name: str, data: bytes) -> bytes:
    # The record names the generator commit, which differs between the two checkouts by design.
    if name != "generator-spec.json":
        return data
    record = json.loads(data)
    record.pop("generator", None)
    return json.dumps(record).encode()


def main(old: Path, new: Path, out: Path) -> int:
    out.mkdir(parents=True, exist_ok=True)
    standard = json.loads((new / "tests" / "fixtures" / "v2" / "standard.json").read_text(encoding="utf-8"))
    sys.path.insert(0, str(new))
    from genkit.core import translations
    from genkit.targets import get

    manifest = get("android").manifest
    template = new.parent / "templates" / "android"
    failures = 0
    for name, raw in SPECS.items():
        if raw is None:
            raw = standard
        else:
            raw = dict(raw)
            if raw["features"] == "everything":
                raw["features"] = sorted(manifest.preset_features("everything"))
            if raw["languages"] == "all":
                raw["languages"] = list(translations.available("android"))
        spec = out / f"{name}.json"
        spec.write_bytes(json.dumps(raw, indent=2).encode())

        results = {}
        for side, generator in (("old", old), ("new", new)):
            directory = out / f"{side}-{name}"
            archive = out / f"{side}-{name}.zip"
            generate(generator, spec, directory, "--no-zip")
            generate(generator, spec, archive)
            results[side] = (tree(directory), archive)

        old_tree, old_zip = results["old"]
        new_tree, new_zip = results["new"]
        crlf_in_old = sorted(path for path, data in old_tree.items() if b"\r\n" in data)
        sources_with_cr = set()
        for path in old_tree:
            source = template / path
            if source.is_file() and b"\r" in source.read_bytes():
                sources_with_cr.add(path)
        linux = {
            path: data if path in sources_with_cr else data.replace(b"\r\n", b"\n")
            for path, data in old_tree.items()
        }
        print(f"{name}: {len(new_tree)} files; the old generator wrote CRLF into {len(crlf_in_old) - len(sources_with_cr)} "
              f"of them on Windows; template sources that are CRLF themselves: {sorted(sources_with_cr)}")

        if sorted(linux) != sorted(new_tree):
            failures += 1
            print(f"  FILE LISTS DIFFER: only old {sorted(set(linux) - set(new_tree))[:10]}, only new {sorted(set(new_tree) - set(linux))[:10]}")
        differing = [
            path for path in sorted(set(linux) & set(new_tree))
            if without_stamp(path, linux[path]) != without_stamp(path, new_tree[path])
        ]
        if differing:
            failures += 1
            print(f"  {len(differing)} files differ from the Linux bytes: {differing[:10]}")
        stray_cr = [path for path, data in new_tree.items() if b"\r" in data and path not in sources_with_cr]
        if stray_cr:
            failures += 1
            print(f"  CR in files whose source has none: {stray_cr[:10]}")

        with zipfile.ZipFile(old_zip) as before, zipfile.ZipFile(new_zip) as after:
            old_entries = {info.filename: info for info in before.infolist()}
            new_entries = {info.filename: info for info in after.infolist()}
            if sorted(old_entries) != sorted(new_entries):
                failures += 1
                print("  ZIP ENTRY LISTS DIFFER")
            modes = {name_: info.external_attr >> 16 for name_, info in new_entries.items()}
            executable = sorted(name_ for name_, mode in modes.items() if mode & 0o111)
            if any((old_entries[name_].external_attr >> 16) != modes[name_] for name_ in modes if name_ in old_entries):
                failures += 1
                print("  ZIP MODES DIFFER")
            root = next(iter(new_entries)).split("/", 1)[0]
            for entry, info in new_entries.items():
                relative = entry.split("/", 1)[1]
                if without_stamp(relative, after.read(info)) != without_stamp(relative, new_tree[relative]):
                    failures += 1
                    print(f"  zip entry {entry} differs from the directory output")
                    break
            print(f"  zip: {len(new_entries)} entries under {root}/, executable: {executable}")

    print("COMPARE OK" if failures == 0 else f"COMPARE FAILED ({failures})")
    return 0 if failures == 0 else 1


if __name__ == "__main__":
    sys.exit(main(*(Path(argument) for argument in sys.argv[1:4])))
