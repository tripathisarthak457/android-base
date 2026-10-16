import re, sys, xml.etree.ElementTree as ET
from pathlib import Path
root = Path(sys.argv[1])
decl = {}
for vs in root.rglob("src/main/res/values/*.xml"):
    for el in ET.parse(vs).getroot():
        if el.get("name") and el.tag in ("string", "plurals", "color", "dimen", "string-array"):
            decl[(el.tag if el.tag != "string-array" else "array", el.get("name"))] = vs.relative_to(root).as_posix()
for res in root.rglob("src/main/res"):
    for d in res.iterdir():
        if d.is_dir() and d.name.split("-")[0] in ("drawable", "raw", "xml", "font"):
            for f in d.iterdir(): decl[(d.name.split("-")[0], f.stem)] = f.relative_to(root).as_posix()
blob = ""
for p in root.rglob("*"):
    if p.is_file() and p.suffix in (".kt", ".kts", ".xml") and "/values" not in p.as_posix():
        blob += p.read_text(encoding="utf-8") + "\n"
for p in root.rglob("src/main/res/values*/themes.xml"): blob += p.read_text()
unused = []
for (kind, name), where in sorted(decl.items()):
    if name in ("app_name",): continue
    if re.search(rf"R\.{kind}\.{name}\b|@{kind}/{name}\b", blob): continue
    unused.append(f"{kind}/{name}  ({where})")
print(len(unused), "unreferenced resources")
for u in unused: print("  ", u)
