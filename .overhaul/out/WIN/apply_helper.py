"""One-off: routes the generator's text writes through genkit.core.copy.write_text."""
import pathlib
import re
import sys

root = pathlib.Path(sys.argv[1])
imports = {
    "genkit/core/record.py": ("from .spec import STAMP_KEY, Spec, to_json\n", "from .copy import write_text\n"),
    "genkit/targets/android/target.py": ("from ...core import translations\n", "from ...core.copy import write_text\n"),
    "genkit/targets/android/strings.py": ("from ...core import translations\n", "from ...core.copy import write_text\n"),
    "genkit/targets/android/icons.py": ("from ...core.spec import Spec\n", "from ...core.copy import write_text\n"),
    "genkit/targets/android/scaffold.py": ("from ...core.spec import Spec, module_title, pascal_case\n", "from ...core.copy import write_text\n"),
    "genkit/targets/android/look.py": ("from ... import brand\n", "from ...core.copy import write_text\n"),
    "genkit/core/catalogue.py": ("from . import record, translations\n", "from .copy import write_text\n"),
    "create_project.py": ("from genkit.core import catalogue, pipeline, prompts\n", "from genkit.core.copy import write_text\n"),
    "add_feature.py": ("from genkit.core import prompts, translations\n", "from genkit.core.copy import write_text\n"),
    "remove_feature.py": ("from genkit.core import prompts\n", "from genkit.core.copy import write_text\n"),
}
# receiver.write_text(content, encoding="utf-8")  ->  write_text(receiver, content)
call = re.compile(r'([A-Za-z_][\w.]*|\([^()\n]*\))\.write_text\((.*), encoding="utf-8"\)$', re.M)
for name, (anchor, line) in imports.items():
    path = root / name
    text = path.read_bytes().decode("utf-8")
    text, count = call.subn(lambda m: f"write_text({m.group(1).strip('()') if m.group(1).startswith('(') else m.group(1)}, {m.group(2)})", text)
    if anchor not in text:
        raise SystemExit(f"{name}: no anchor")
    if name.startswith("genkit/core/record") or name.startswith("genkit/targets/android/icons") or name.startswith("genkit/targets/android/scaffold"):
        text = text.replace(anchor, line + anchor, 1)
    else:
        text = text.replace(anchor, anchor + line, 1)
    path.write_bytes(text.encode("utf-8"))
    print(f"{name}: {count}")
