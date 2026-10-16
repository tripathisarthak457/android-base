#!/usr/bin/env bash
# Regenerates the phase 2 check project from the worktree and builds its devBenchmarkRelease APKs.
# The primary_list tag is the same baseline-only patch as specs/BASELINE-PATCH.md (WP02-M1 adds it
# to the template).
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP04; L=$OV/bin/locked; O=$OV/out/WP04
cd $W/generator
python3 create_project.py --spec $O/specs/p2.json --out $O/p2 --no-zip --force
f=$O/p2/feature/sample/src/main/kotlin/com/example/wp04/p2/feature/sample/list/SampleListScreen.kt
python3 - "$f" <<'PY'
import sys
path = sys.argv[1]
s = open(path).read()
if "primary_list" not in s:
    # The first LazyColumn's modifier: the root list of rows.
    anchor = s.index("LazyColumn(")
    target = "modifier = Modifier.fillMaxSize(),"
    at = s.index(target, anchor)
    s = s[:at] + 'modifier = Modifier.fillMaxSize().testTag("primary_list"),' + s[at + len(target):]
    if "import androidx.compose.ui.platform.testTag" not in s:
        s = s.replace("import androidx.compose.ui.Modifier\n", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.platform.testTag\n", 1)
    open(path, "w").write(s)
PY
grep -c 'primary_list' "$f"
cd $O/p2
$L gradle ./gradlew :app:assembleDevBenchmarkRelease --console=plain
echo P2-REBUILD OK
