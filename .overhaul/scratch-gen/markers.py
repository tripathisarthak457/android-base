import os, re, sys
from pathlib import Path
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/android-base/generator")
from genkit.spec import FEATURES_BY_KEY
T = Path("/home/crazyhusir/Documents/projects/tools/android-base/template")
OPEN = re.compile(r"<opt:([a-z0-9|-]+)>")
CLOSE = re.compile(r"</opt:([a-z0-9|-]+)>")
ELSE = re.compile(r"<opt:!([a-z0-9-]+)>")
GEN = re.compile(r"<generated:([a-z0-9-]+)>")
ANY = re.compile(r"</?opt:|<generated:")
SKIP={"build",".gradle",".kotlin",".idea",".git"}
issues=[]
for d, sub, files in os.walk(T):
    sub[:] = [s for s in sub if s not in SKIP]
    for f in files:
        p = Path(d)/f
        try: text = p.read_text(encoding="utf-8")
        except Exception: continue
        if not ANY.search(text): continue
        rel = p.relative_to(T)
        stack=[]
        for n, line in enumerate(text.splitlines(), 1):
            for m in re.finditer(r"<(/?)opt:(!?)([a-z0-9|-]+)>", line):
                for k in m.group(3).split("|"):
                    if k not in FEATURES_BY_KEY: issues.append((str(rel), n, f"unknown feature '{k}' in marker", line.strip()))
            c = CLOSE.search(line); o = OPEN.search(line); e = ELSE.search(line); g=GEN.search(line)
            count = len(re.findall(r"<(/?)opt:", line)) + len(GEN.findall(line))
            if count > 1: issues.append((str(rel), n, "more than one marker on a line", line.strip()))
            if c:
                if not stack: issues.append((str(rel), n, "close without open", line.strip()))
                elif stack[-1][0] != c.group(1): issues.append((str(rel), n, f"close {c.group(1)} but top is {stack[-1][0]} (opened line {stack[-1][1]})", line.strip()))
                else: stack.pop()
                # code on same line as close?
                rest = CLOSE.sub("", line).strip()
                if rest not in ("//", "<!--  -->", "<!-- -->", "#", "") and not rest.startswith("//") :
                    issues.append((str(rel), n, "close marker line has other content (dropped)", line.strip()))
                continue
            if o and not e:
                stack.append((o.group(1), n))
                rest = OPEN.sub("", line).strip()
                if rest not in ("//", "<!--  -->", "<!-- -->", "#", "") and not rest.startswith("//"):
                    issues.append((str(rel), n, "open marker line has other content (dropped)", line.strip()))
                continue
            if e:
                # inline else content
                prefix = line[:e.start()].strip()
                if prefix not in ("//", "<!--", "#", "") : issues.append((str(rel), n, f"inline-else prefix '{prefix}' (prefix dropped, ok?)", line.strip()))
                if line.rstrip().endswith("-->"): issues.append((str(rel), n, "inline-else in XML comment: emitted code keeps the '-->'", line.strip()))
            if g:
                rest = GEN.sub("", line).strip()
                if rest not in ("//", "<!--  -->", "<!-- -->", "#", ""): issues.append((str(rel), n, "generated marker line has other content (dropped)", line.strip()))
        if stack: issues.append((str(rel), stack[-1][1], f"unclosed {[s[0] for s in stack]}", ""))
for i in issues: print(i)
print(len(issues), "issues")
