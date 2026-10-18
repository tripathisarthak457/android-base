"""findrow.py <ui.xml>: prints the centre of the first clickable row wholly inside primary_list, as perf.py's rows()
picks it, or exits 1 when the list has none yet."""
import re
import sys
import xml.etree.ElementTree as ET


def box(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds"))))


try:
    root = ET.parse(sys.argv[1]).getroot()
except (ET.ParseError, OSError):
    sys.exit(1)
for holder in root.iter("node"):
    if holder.get("resource-id") == "primary_list":
        top, bottom = box(holder)[1], box(holder)[3]
        for node in holder.iter("node"):
            if node is holder or node.get("clickable") != "true":
                continue
            left, t, right, b = box(node)
            if t >= top and b <= bottom and b - t > 40:
                print((left + right) // 2, (t + b) // 2)
                sys.exit(0)
sys.exit(1)
