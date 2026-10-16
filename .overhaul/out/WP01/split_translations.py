"""One-off: split generator/translations/<tag>.json into translations/android/<tag>/<slug>.json."""
import json, sys
from pathlib import Path
root = Path(sys.argv[1])
for old in sorted(root.glob("*.json")):
    data = json.loads(old.read_text(encoding="utf-8"))
    target = root / "android" / old.stem
    target.mkdir(parents=True, exist_ok=True)
    def write(name, obj):
        (target / name).write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    write("_language.json", {"name": data["language"]})
    write("_scaffold.json", data["scaffold"])
    for res, strings in data["strings"].items():
        assert res.endswith("/src/main/res"), res
        module = res[: -len("/src/main/res")]
        write(module.replace("/", "-") + ".json", strings)
    assert set(data) == {"language", "strings", "scaffold"}
print("split")
