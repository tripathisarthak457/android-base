# Prints clickable/text nodes from a uiautomator dump with their centre coordinates.
import sys, re, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
flt = sys.argv[2] if len(sys.argv) > 2 else None
for n in root.iter("node"):
    t = n.get("text") or ""; d = n.get("content-desc") or ""; rid = n.get("resource-id") or ""
    b = n.get("bounds"); cl = n.get("clickable") == "true"; sel = n.get("selected") == "true"
    if not (t or d or cl): continue
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", b))
    label = t or d
    if flt and flt.lower() not in (label + rid).lower(): continue
    print(f"{(x1+x2)//2:5d} {(y1+y2)//2:5d}  [{b}] click={int(cl)} sel={int(sel)} cls={n.get('class','').split('.')[-1]} {label[:60]!r}")
