"""Splits a generated baseline profile into the checked-in per-module profiles.

    split_profile.py [--project .] [--generated app/src/main/generated/baselineProfiles]
                     [--module core/navigation --module core/designsystem --module core/ui] [--app app]

`:app:generateBaselineProfile` records one profile for the whole app from the benchmark journeys. Every
preset ships these rules, including presets without the benchmark module, so they are copied into
`src/main/baseline-prof.txt` of the modules they belong to: a library module's rules (shell, design
system, shared UI) go into that library, and the app module gets the startup rules for its own
classes. A rule belongs to the module whose sources declare its class's package. Wildcard rules already
in a module's profile are written by hand (the safety net that covers code no journey reaches), so a
split keeps them at the top and adds the generated rules under them.
"""

import argparse
import os
import re
import sys

DEFAULT_MODULES = ["core/navigation", "core/designsystem", "core/ui"]
_CLASS = re.compile(r"L([\w/$]+);")


def module_packages(module_dir):
    """Every package path (com/example/app/ui) that the module's main sources declare."""
    packages = set()
    for source_root in ("src/main/kotlin", "src/main/java"):
        root = os.path.join(module_dir, source_root)
        for directory, _, files in os.walk(root):
            for name in files:
                if not name.endswith((".kt", ".java")):
                    continue
                with open(os.path.join(directory, name), errors="ignore") as handle:
                    match = re.search(r"^\s*package\s+([\w.]+)", handle.read(), re.MULTILINE)
                if match:
                    packages.add(match.group(1).replace(".", "/"))
    return packages


def rule_package(line):
    match = _CLASS.search(line)
    if not match:
        return None
    descriptor = match.group(1)
    return descriptor.rsplit("/", 1)[0] if "/" in descriptor else ""


def select(lines, packages):
    """The rules whose class is in one of [packages], deduplicated, in a stable order."""
    chosen = {line.strip() for line in lines if line.strip() and rule_package(line) in packages}
    return sorted(chosen)


def read_lines(path):
    with open(path, errors="ignore") as handle:
        return handle.read().splitlines()


def hand_written(path):
    """The wildcard rules in [path]: a generated profile never has one."""
    if not os.path.isfile(path):
        return []
    return [line.strip() for line in read_lines(path) if "*" in line]


def write(path, rules):
    kept = hand_written(path)
    rules = kept + [rule for rule in rules if rule not in kept]
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as handle:
        handle.write("\n".join(rules) + ("\n" if rules else ""))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--project", default=".")
    parser.add_argument("--generated", default="app/src/main/generated/baselineProfiles")
    parser.add_argument("--module", action="append", dest="modules")
    parser.add_argument("--app", default="app")
    args = parser.parse_args(argv)

    generated = os.path.join(args.project, args.generated)
    profile = os.path.join(generated, "baseline-prof.txt")
    startup = os.path.join(generated, "startup-prof.txt")
    if not os.path.isfile(profile):
        sys.exit(f"{profile} does not exist: run ./gradlew :app:generateBaselineProfile first")
    rules = read_lines(profile)
    startup_rules = read_lines(startup) if os.path.isfile(startup) else rules

    for module in args.modules or DEFAULT_MODULES:
        module_dir = os.path.join(args.project, module)
        chosen = select(rules, module_packages(module_dir))
        write(os.path.join(module_dir, "src/main/baseline-prof.txt"), chosen)
        print(f"{module}: {len(chosen)} rules")

    app_dir = os.path.join(args.project, args.app)
    chosen = select(startup_rules, module_packages(app_dir))
    write(os.path.join(app_dir, "src/main/baseline-prof.txt"), chosen)
    print(f"{args.app}: {len(chosen)} startup rules")
    return 0


if __name__ == "__main__":
    sys.exit(main())
