"""findrelated.py <ui.xml> <screen height>: prints the centre of the first related_row wholly on screen, as perf.py's
find_related() picks it, or exits 1 when there is none."""
import re
import sys
import xml.etree.ElementTree as ET

try:
    root = ET.parse(sys.argv[1]).getroot()
except (ET.ParseError, OSError):
    sys.exit(1)
height = int(sys.argv[2])
for node in root.iter("node"):
    if node.get("resource-id") == "related_row":
        left, top, right, bottom = map(int, re.findall(r"\d+", node.get("bounds")))
        if top >= 0 and bottom <= height:
            print((left + right) // 2, (top + bottom) // 2)
            sys.exit(0)
sys.exit(1)
