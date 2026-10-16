"""Copies the per-module profiles split in a generated project into the template, renamed to the template's
package, keeping only rules for classes whose package exists in the template (a preset's scaffolded feature
modules do not) and the template's hand-written wildcard rules at the top.

    python3 port_profile.py <generated project> <template dir> <generated package path, e.g. com/example/wp04/everything>
"""
import os
import sys

sys.path.insert(0, os.path.join(sys.argv[2], "tools", "perf"))
import split_profile  # noqa: E402

MODULES = ["core/navigation", "core/designsystem", "core/ui", "app"]


def template_packages(template):
    packages = set()
    for directory, names, _ in os.walk(template):
        names[:] = [n for n in names if n not in ("build", ".gradle", "node_modules")]
        if directory.endswith(os.path.join("src", "main")):
            packages |= split_profile.module_packages(os.path.dirname(os.path.dirname(directory)))
    return packages


def main():
    project, template, generated_root = sys.argv[1:4]
    known = template_packages(template)
    for module in MODULES:
        source = os.path.join(project, module, "src/main/baseline-prof.txt")
        target = os.path.join(template, module, "src/main/baseline-prof.txt")
        lines = [line.replace(generated_root, "com/base/app") for line in split_profile.read_lines(source)]
        rules = [line for line in lines if "*" not in line and split_profile.rule_package(line) in known]
        dropped = len([line for line in lines if "*" not in line]) - len(rules)
        split_profile.write(target, sorted(set(rules)))
        print(f"{module}: {len(rules)} rules, {dropped} dropped (packages not in the template)")


if __name__ == "__main__":
    main()
