"""Splits a generated baseline profile into the checked-in per-module profiles.

    split_profile.py [--project .] [--generated app/src/main/generated/baselineProfiles]
                     [--module core/navigation --module core/designsystem --module core/ui] [--app app]

`:app:generateBaselineProfile` records one profile for the whole app from the benchmark journeys. Every
preset ships these rules, including presets without the benchmark module, so they are copied into
`src/main/baseline-prof.txt` of the modules they belong to: a listed library module's rules (shell,
design system, shared UI) go into that library, and the app module gets the rest of the project's own
code (the app's classes, the features' screens, the data layer), whichever module declares it, since an
app's profile applies to every class in its APK. A rule belongs to the module whose sources declare its
class's package. Wildcard rules already
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
    # LF on every host: the profile is checked in, and a Windows text-mode write would make it CRLF.
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write("\n".join(rules) + ("\n" if rules else ""))


def own_packages(app_packages):
    """Whether a package is the project's own code: the app's root package or one under it."""
    root = min(app_packages, key=len) if app_packages else None
    return lambda package: root is not None and (package == root or package.startswith(root + "/"))


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("--project", default=".")
    parser.add_argument("--generated", default="app/src/main/generated/baselineProfiles")
    parser.add_argument("--module", action="append", dest="modules")
    parser.add_argument("--app", default="app")
    args = parser.parse_args(argv)

    generated = os.path.join(args.project, args.generated)
    profile = os.path.join(generated, "baseline-prof.txt")
    if not os.path.isfile(profile):
        sys.exit(f"{profile} does not exist: run ./gradlew :app:generateBaselineProfile first")
    rules = read_lines(profile)

    claimed = set()
    for module in args.modules or DEFAULT_MODULES:
        module_dir = os.path.join(args.project, module)
        packages = module_packages(module_dir)
        claimed |= packages
        chosen = select(rules, packages)
        write(os.path.join(module_dir, "src/main/baseline-prof.txt"), chosen)
        print(f"{module}: {len(chosen)} rules")

    app_dir = os.path.join(args.project, args.app)
    own = own_packages(module_packages(app_dir))
    packages = {rule_package(line) for line in rules} - {None}
    chosen = select(rules, {package for package in packages if own(package) and package not in claimed})
    write(os.path.join(app_dir, "src/main/baseline-prof.txt"), chosen)
    print(f"{args.app}: {len(chosen)} rules")
    return 0


if __name__ == "__main__":
    sys.exit(main())
