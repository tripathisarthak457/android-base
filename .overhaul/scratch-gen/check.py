"""Static consistency checks over a generated project (no Gradle)."""
from __future__ import annotations

import re
import sys
import tomllib
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path

MARKER = re.compile(r"</?opt:|<generated:")
IDENTITY = re.compile(r"com\.base\.app|com/base/app|BaseApp|base_app|baseapp")

DECL = re.compile(
    r"^[ \t]*(?:@[\w.]+(?:\([^)]*\))?[ \t]+)*"
    r"(?:(?:public|internal|private|protected|abstract|open|sealed|data|enum|annotation|inline|value|inner|const|override|suspend|operator|infix|tailrec|external|lateinit|fun(?=[ \t]+interface))[ \t]+)*"
    r"(class|interface|object|typealias|fun|val|var)[ \t]+(?:<[^>]*>[ \t]*)?(?:[\w.<>?, *]+\.)?(`?\w+`?)",
    re.M,
)
PACKAGE = re.compile(r"^package[ \t]+([\w.]+)", re.M)
IMPORT = re.compile(r"^import[ \t]+([\w.`]+)(?:[ \t]+as[ \t]+(\w+))?[ \t]*$", re.M)


def strip_code(text: str) -> str:
    """Removes comments and string literal contents, keeping template expressions roughly."""
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if text.startswith("//", i):
            j = text.find("\n", i)
            i = n if j < 0 else j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            i = n if j < 0 else j + 2
            out.append(" ")
        elif text.startswith('"""', i):
            j = text.find('"""', i + 3)
            body = text[i + 3 : (n if j < 0 else j)]
            out.append('""' + _templates(body) + '""')
            i = n if j < 0 else j + 3
        elif c == '"':
            j = i + 1
            while j < n and text[j] != '"' and text[j] != "\n":
                if text[j] == "\\":
                    j += 1
                j += 1
            out.append('"' + _templates(text[i + 1 : j]) + '"')
            i = j + 1
        elif c == "'" and i + 2 < n and (text[i + 2] == "'" or text[i + 1] == "\\"):
            j = text.find("'", i + 2 if text[i + 1] == "\\" else i + 1)
            out.append("' '")
            i = j + 1 if j > 0 else n
        else:
            out.append(c)
            i += 1
    return "".join(out)


def _templates(body: str) -> str:
    parts = re.findall(r"\$\{([^}]*)\}", body) + re.findall(r"\$([A-Za-z_]\w*)", body)
    return " " + " ".join(parts) + " " if parts else ""


class Project:
    def __init__(self, root: Path):
        self.root = root
        self.kt = sorted(p for p in root.rglob("*.kt"))
        self.kts = sorted(p for p in root.rglob("*.kts"))
        self.texts = {p: p.read_text(encoding="utf-8") for p in self.kt + self.kts}
        self.code = {p: strip_code(t) for p, t in self.texts.items()}
        self.decls: dict[str, set[str]] = defaultdict(set)  # package -> names
        self.decl_names: dict[str, set[Path]] = defaultdict(set)
        self.top_names: set[str] = set()
        self.file_pkg: dict[Path, str] = {}
        for p in self.kt:
            t = self.code[p]
            m = PACKAGE.search(self.texts[p])
            pkg = m.group(1) if m else ""
            self.file_pkg[p] = pkg
            for d in DECL.finditer(t):
                name = d.group(2).strip("`")
                self.decls[pkg].add(name)
                self.decl_names[name].add(p)
                line_start = t.rfind("\n", 0, d.start()) + 1
                indent = len(t[line_start:d.start(1)]) - len(t[line_start:d.start(1)].lstrip())
                if indent == 0 and not t[line_start:d.start(1)].strip().startswith(("private",)):
                    self.top_names.add(name)
        self.namespaces = set()
        for p in self.kts:
            for m in re.finditer(r'namespace\s*=\s*"([^"]+)"', self.texts[p]):
                self.namespaces.add(m.group(1))

    def rel(self, p: Path) -> str:
        return p.relative_to(self.root).as_posix()


def check(root: Path, reference: Project | None, package: str) -> list[str]:
    issues: list[str] = []
    proj = Project(root)

    # 1. leftover markers and template identity
    for p in root.rglob("*"):
        if p.is_dir() or p.suffix in {".jar", ".png", ".jks", ".webp"}:
            continue
        try:
            t = p.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        rel = p.relative_to(root).as_posix()
        for n, line in enumerate(t.splitlines(), 1):
            if MARKER.search(line):
                issues.append(f"MARKER {rel}:{n}: {line.strip()[:120]}")
            if IDENTITY.search(line):
                issues.append(f"IDENTITY {rel}:{n}: {line.strip()[:120]}")
        if IDENTITY.search(rel):
            issues.append(f"IDENTITY-PATH {rel}")

    # 2. settings includes vs module directories
    settings = (root / "settings.gradle.kts").read_text(encoding="utf-8")
    included = set(re.findall(r'include\("([^"]+)"\)', strip_code_keep_strings(settings)))
    for inc in included:
        d = root / inc.strip(":").replace(":", "/")
        if not (d / "build.gradle.kts").is_file():
            issues.append(f"SETTINGS include {inc} has no build.gradle.kts")
    for bf in root.rglob("build.gradle.kts"):
        if "build-logic" in bf.parts or bf.parent == root:
            continue
        path = ":" + bf.parent.relative_to(root).as_posix().replace("/", ":")
        if path not in included:
            issues.append(f"SETTINGS orphan module {path} not included")

    # 3. project(":x") references
    for p in proj.kts:
        for m in re.finditer(r'project\("(:[^"]+)"\)', proj.texts[p]):
            if m.group(1) not in included:
                issues.append(f"PROJECTREF {proj.rel(p)} -> {m.group(1)} not included")

    # 4. imports of the project's own packages
    for p in proj.kt:
        for m in IMPORT.finditer(proj.texts[p]):
            full, alias = m.group(1), m.group(2)
            if not full.startswith(package + ".") or full.endswith(".*"):
                continue
            pkg, _, name = full.rpartition(".")
            if name in ("R", "BuildConfig"):
                if pkg not in proj.namespaces:
                    issues.append(f"IMPORT {proj.rel(p)}: {full} (no module with namespace {pkg})")
                continue
            if name in proj.decls.get(pkg, ()):
                continue
            ppkg, _, outer = pkg.rpartition(".")
            if outer in proj.decls.get(ppkg, ()):
                continue  # nested import
            issues.append(f"IMPORT {proj.rel(p)}: {full} not declared")

    # 5. unused imports
    for p in proj.kt:
        text = proj.texts[p]
        body = IMPORT.sub("", proj.code[p])
        for m in IMPORT.finditer(text):
            full, alias = m.group(1), m.group(2)
            if full.endswith(".*"):
                continue
            name = alias or full.rpartition(".")[2].strip("`")
            if name in {"getValue", "setValue", "provideDelegate", "invoke", "plus", "minus", "times", "div", "contains", "compareTo", "rangeTo", "iterator", "component1", "component2"}:
                continue
            if not re.search(rf"(?<![\w]){re.escape(name)}(?![\w])", body):
                # KDoc references count for the IDE but not the compiler; report as KDOC-only when that is the use
                if re.search(rf"\[{re.escape(name)}[\].]", text):
                    issues.append(f"UNUSED-IMPORT(kdoc-only) {proj.rel(p)}: {full}")
                else:
                    issues.append(f"UNUSED-IMPORT {proj.rel(p)}: {full}")

    # 6. dangling symbols relative to reference
    if reference is not None:
        ref_names = reference.top_names
        here_names = set(proj.decl_names)
        gone = {n for n in ref_names - here_names if len(n) > 2}
        for p in proj.kt + proj.kts:
            text = proj.texts[p]
            external = {(m.group(2) or m.group(1).rpartition(".")[2]) for m in IMPORT.finditer(text) if not m.group(1).startswith(package + ".")}
            code = IMPORT.sub("", proj.code[p])
            for tok in (set(re.findall(r"(?<![\w.])([A-Z]\w+)", code)) & gone) - external:
                issues.append(f"DANGLING {proj.rel(p)}: {tok}")
            for tok in (set(re.findall(r"(?<![\w.])([a-z]\w+)\s*[({<]", code)) & gone) - external:
                issues.append(f"DANGLING-FUN {proj.rel(p)}: {tok}")

    # 7. version catalogue references
    toml_path = root / "gradle/libs.versions.toml"
    try:
        toml = tomllib.loads(toml_path.read_text(encoding="utf-8"))
    except Exception as e:  # noqa
        issues.append(f"TOML parse error {e}")
        toml = {}
    norm = lambda k: re.sub(r"[-_]", ".", k)
    libs = {norm(k) for k in toml.get("libraries", {})}
    plugins = {norm(k) for k in toml.get("plugins", {})}
    bundles = {norm(k) for k in toml.get("bundles", {})}
    versions = {norm(k) for k in toml.get("versions", {})}
    for v in toml.get("libraries", {}).values():
        ref = v.get("version", {}).get("ref") if isinstance(v.get("version"), dict) else None
        if ref and norm(ref) not in versions:
            issues.append(f"TOML library refers to missing version {ref}")
    for v in toml.get("plugins", {}).values():
        ref = v.get("version", {}).get("ref") if isinstance(v.get("version"), dict) else None
        if ref and norm(ref) not in versions:
            issues.append(f"TOML plugin refers to missing version {ref}")
    for name, members in toml.get("bundles", {}).items():
        for mem in members:
            if norm(mem) not in libs:
                issues.append(f"TOML bundle {name} names missing library {mem}")
    for p in proj.kts:
        code = proj.code[p]
        for m in re.finditer(r"(?<![\w])libs\.((?:\w+\.)*\w+)", code):
            ref = m.group(1)
            if ref.startswith("plugins."):
                key, pool = ref[8:], plugins
            elif ref.startswith("bundles."):
                key, pool = ref[8:], bundles
            elif ref.startswith("versions."):
                key, pool = ref[9:], versions
            else:
                key, pool = ref, libs
            key = re.sub(r"\.(get|asProvider)$", "", key)
            if key not in pool and not any(k.startswith(key + ".") for k in pool):
                issues.append(f"LIBS {proj.rel(p)}: libs.{ref}")
    for p in proj.kt:
        if "build-logic" not in p.parts:
            continue
        for m in re.finditer(r'libs\.(library|plugin|version|bundle|findLibrary|findPlugin|findVersion|findBundle)\("([^"]+)"\)', proj.texts[p]):
            kind, key = m.group(1).lower(), norm(m.group(2))
            pool = libs if "library" in kind else plugins if "plugin" in kind else versions if "version" in kind else bundles
            if key not in pool:
                issues.append(f"LIBS {proj.rel(p)}: {m.group(0)}")

    # 8. convention plugins: ids used vs registered
    conv = root / "build-logic/convention/build.gradle.kts"
    ctext = conv.read_text(encoding="utf-8")
    registered = set(re.findall(r'id\s*=\s*"([^"]+)"', ctext))
    impl = set(re.findall(r'implementationClass\s*=\s*"([^"]+)"', ctext))
    for c in impl:
        if c not in proj.decl_names:
            issues.append(f"CONVENTION implementationClass {c} not declared")
    conv_files = list((root / "build-logic/convention/src/main/kotlin").glob("*ConventionPlugin.kt"))
    for f in conv_files:
        if f.stem not in impl:
            issues.append(f"CONVENTION {f.stem} is never registered (dead file)")
    for p in proj.kts:
        if "build-logic" in p.parts:
            continue
        for m in re.finditer(r'id\("(%s\.[^"]+)"\)' % re.escape(package), proj.texts[p]):
            if m.group(1) not in registered:
                issues.append(f"CONVENTION {proj.rel(p)} applies unregistered {m.group(1)}")
    for p in proj.kt:
        if "build-logic" not in p.parts:
            continue
        for m in re.finditer(r'apply\("(%s\.[^"]+)"\)' % re.escape(package), proj.texts[p]):
            if m.group(1) not in registered:
                issues.append(f"CONVENTION {proj.rel(p)} applies unregistered {m.group(1)}")

    # 9. manifest components
    for mf in root.rglob("AndroidManifest.xml"):
        text = mf.read_text(encoding="utf-8")
        mod = mf.parents[2]
        ns = None
        bf = mod / "build.gradle.kts"
        if bf.is_file():
            m = re.search(r'namespace\s*=\s*"([^"]+)"', bf.read_text(encoding="utf-8"))
            ns = m.group(1) if m else None
        if mod.name == "app":
            ns = package
        for m in re.finditer(r'<(activity|service|receiver|provider|application|activity-alias)\b[^>]*?android:name="([^"]+)"', text, re.S):
            name = m.group(2)
            if name.startswith("."):
                if ns is None:
                    continue
                name = ns + name
            if not name.startswith(package):
                continue
            pkg, _, cls = name.rpartition(".")
            if cls not in proj.decls.get(pkg, ()):
                issues.append(f"MANIFEST {mf.relative_to(root)}: {m.group(1)} {name} not declared")

    # 10. resources
    res_names: dict[str, set[str]] = defaultdict(set)
    for res in root.rglob("src/*/res"):
        for d in res.iterdir():
            if not d.is_dir():
                continue
            kind = d.name.split("-")[0]
            for f in d.iterdir():
                if kind == "values":
                    try:
                        tree = ET.parse(f).getroot()
                    except Exception as e:  # noqa
                        issues.append(f"XML parse {f.relative_to(root)}: {e}")
                        continue
                    for el in tree:
                        nm = el.get("name")
                        if not nm:
                            continue
                        tag = el.tag
                        if tag == "item":
                            tag = el.get("type", "item")
                        if tag in ("string-array", "integer-array", "array"):
                            tag = "array"
                        if tag == "declare-styleable":
                            tag = "styleable"
                            for a in el:
                                if a.get("name"):
                                    res_names["attr"].add(a.get("name").replace(".", "_"))
                        res_names[tag].add(nm.replace(".", "_"))
                else:
                    res_names[kind].add(f.name.split(".")[0])
                    if f.suffix == ".xml" and kind not in ("raw",):
                        try:
                            ET.parse(f)
                        except Exception as e:  # noqa
                            issues.append(f"XML parse {f.relative_to(root)}: {e}")
    for p in proj.kt:
        for m in re.finditer(r"(?<![\w.])(?:\w+R|R)\.(string|plurals|drawable|mipmap|raw|color|dimen|font|xml|array|bool|integer|style|layout|anim|id)\.(\w+)", proj.code[p]):
            kind, nm = m.group(1), m.group(2)
            if nm not in res_names.get(kind, set()):
                issues.append(f"RES {proj.rel(p)}: R.{kind}.{nm}")
    for x in list(root.rglob("*.xml")):
        try:
            t = x.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        t = re.sub(r"<!--.*?-->", "", t, flags=re.S)
        for m in re.finditer(r'"@(?!android:)(?:\+)?(string|drawable|mipmap|raw|color|dimen|font|xml|array|bool|integer|style|layout|anim)/([\w.]+)"', t):
            kind, nm = m.group(1), m.group(2).replace(".", "_")
            if nm not in res_names.get(kind, set()) and not nm.startswith("glance_"):
                issues.append(f"RES {x.relative_to(root)}: @{kind}/{m.group(2)}")

    # 11. translations line up with the source strings
    for vs in root.rglob("src/main/res/values/strings.xml"):
        src = [e.get("name") for e in ET.parse(vs).getroot() if e.get("translatable") != "false"]
        for tr in vs.parent.parent.glob("values-*/strings.xml"):
            got = [e.get("name") for e in ET.parse(tr).getroot()]
            if sorted(got) != sorted(src):
                issues.append(f"TRANSLATION {tr.relative_to(root)} differs: missing={sorted(set(src)-set(got))} extra={sorted(set(got)-set(src))}")

    # 12. empty/hollow artefacts
    for p in root.rglob("*"):
        if p.is_file() and p.stat().st_size == 0:
            issues.append(f"EMPTY-FILE {p.relative_to(root)}")
    for p in proj.kts:
        for m in re.finditer(r"\b(dependencies|plugins|android)\s*\{\s*\}", proj.code[p]):
            issues.append(f"EMPTY-BLOCK {proj.rel(p)}: {m.group(1)} {{}}")
    for p in proj.kt:
        for m in re.finditer(r"\n([ \t]*)//[^\n]*\n(?=[ \t]*\})", proj.texts[p]):
            pass
    return issues


def strip_code_keep_strings(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"(?m)^\s*//.*$", "", text)


if __name__ == "__main__":
    root = Path(sys.argv[1])
    ref = Project(Path(sys.argv[2])) if len(sys.argv) > 2 and sys.argv[2] != "-" else None
    pkg = sys.argv[3] if len(sys.argv) > 3 else "com.acme.field"
    for i in check(root, ref, pkg):
        print(i)
