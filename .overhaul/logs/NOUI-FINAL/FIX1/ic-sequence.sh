#!/usr/bin/env bash
# Edit-one-file-and-check on :core:navigation:shell: a full compile and check, then one shell file
# that declares neither baseline function gains a private val, then the check again (incremental).
set -u
O=/d/projects/tools/.overhaul
P=$1; TAG=$2; L=$O/logs/NOUI-FINAL/FIX1
F=$(ls $P/core/navigation/shell/src/main/kotlin/*/*/*/core/navigation/shell/ShellShortcuts.kt 2>/dev/null || find $P/core/navigation/shell/src/main -name ShellShortcuts.kt)
cd $P
$O/bin/locked gradle ./gradlew :core:navigation:shell:checkComposeStability --offline > $L/ic-$TAG-1-full.log 2>&1; echo "$TAG step1 rc=$?"
cp "$(find core/navigation/shell/build -name '*-composables.txt' | head -1)" $L/ic-$TAG-report-full.txt
printf '\nprivate val incrementalProbe = 0\n' >> "$F"
$O/bin/locked gradle ./gradlew :core:navigation:shell:checkComposeStability --offline > $L/ic-$TAG-2-incremental.log 2>&1; echo "$TAG step2 rc=$?"
cp "$(find core/navigation/shell/build -name '*-composables.txt' | head -1)" $L/ic-$TAG-report-after-ic.txt
/c/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe -I -c "
import sys; p=sys.argv[1]; s=open(p,encoding='utf-8',newline='').read(); t='\nprivate val incrementalProbe = 0\n'
assert s.endswith(t); open(p,'w',encoding='utf-8',newline='').write(s[:-len(t)])" "$F"
echo "$TAG restored"
