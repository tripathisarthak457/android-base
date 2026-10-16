import re, sys
from pathlib import Path
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/scratch-gen")
from check import strip_code

BUILTIN = {"Context", "Application", "SavedStateHandle", "Activity", "ComponentActivity", "ViewModelLifecycle", "Resources", "WorkerParameters", "FragmentActivity", "HiltWorkerFactory"}

def params(sig):
    depth, cur, out = 0, "", []
    for ch in sig:
        if ch in "(<[": depth += 1
        if ch in ")>]": depth -= 1
        if ch == "," and depth == 0: out.append(cur); cur = ""
        else: cur += ch
    out.append(cur)
    res = []
    for p in out:
        p = p.strip()
        if ":" not in p: continue
        quals = re.findall(r"@(\w+)", p.split(":")[0])
        t = p.split(":", 1)[1].split("=")[0].strip()
        res.append((t, quals))
    return res

def norm(t):
    t = t.strip().rstrip("?")
    t = re.sub(r"\s+", "", t)
    return t

def analyse(root):
    provided, needed = {}, []
    files = [p for p in root.rglob("src/main/kotlin/**/*.kt")]
    for p in files:
        code = strip_code(p.read_text(encoding="utf-8"))
        rel = p.relative_to(root).as_posix()
        for m in re.finditer(r"class\s+(\w+)(?:<[^>]*>)?\s*(?:@\w+(?:\([^)]*\))?\s*)*@Inject\s+constructor\s*\(", code):
            provided.setdefault(m.group(1), rel)
            start = m.end(); depth = 1; i = start
            while depth and i < len(code):
                depth += {"(": 1, ")": -1}.get(code[i], 0); i += 1
            for t, q in params(code[start:i-1]):
                needed.append((norm(t), q, rel, m.group(1)))
        for m in re.finditer(r"@(Provides|Binds)\b((?:\s*@\w+(?:\([^)]*\))?)*)\s*(?:(?:internal|public|abstract|suspend)\s+)*fun\s+(\w+)\s*\(", code):
            start = m.end(); depth = 1; i = start
            while depth and i < len(code):
                depth += {"(": 1, ")": -1}.get(code[i], 0); i += 1
            ps = params(code[start:i-1])
            rest = code[i:i+300]
            rm = re.match(r"\s*:\s*([\w.<>, ?*]+?)\s*(?:=|\{|\n)", rest)
            ret = norm(rm.group(1)) if rm else "?"
            intoset = "IntoSet" in m.group(2) or "IntoMap" in m.group(2) or "ElementsIntoSet" in m.group(2)
            key = f"Set<{ret}>" if intoset and "ElementsIntoSet" not in m.group(2) else ret
            provided.setdefault(key, rel)
            if m.group(1) == "Binds":
                for t, q in ps:
                    needed.append((norm(t), q, rel, m.group(3)))
            else:
                for t, q in ps:
                    needed.append((norm(t), q, rel, m.group(3)))
        for m in re.finditer(r"@Multibinds\s*(?:abstract\s+)?fun\s+\w+\(\)\s*:\s*([\w.<>, ?]+)", code):
            provided.setdefault(norm(m.group(1)), rel)
        for m in re.finditer(r"@Inject\s+(?:lateinit\s+)?(?:internal\s+)?var\s+\w+\s*:\s*([\w.<>, ?]+)", code):
            needed.append((norm(m.group(1)), [], rel, "field"))
    missing = []
    for t, q, rel, who in needed:
        base = re.sub(r"<.*", "", t)
        if base in BUILTIN or t in provided or base in provided:
            continue
        if base in ("Lazy", "Provider") :
            inner = re.match(r"\w+<(.*)>", t)
            if inner and (inner.group(1) in provided or re.sub(r"<.*","",inner.group(1)) in provided): continue
        if base in ("Set", "MutableSet"):
            inner = re.match(r"\w+<(?:@JvmSuppressWildcards)?(.*)>", t).group(1)
            inner = inner.replace("@JvmSuppressWildcards", "")
            if f"Set<{inner}>" in provided or any(k.startswith("Set<") and inner in k for k in provided): continue
        missing.append(f"HILT {rel}: {who} needs {t} {q}")
    return missing, provided

if __name__ == "__main__":
    m, prov = analyse(Path(sys.argv[1]))
    for x in sorted(set(m)): print(x)
    if len(sys.argv) > 2: print(sorted(prov))
