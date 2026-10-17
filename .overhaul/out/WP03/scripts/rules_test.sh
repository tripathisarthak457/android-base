#!/usr/bin/env bash
# The sheet code rules against a copy of wp/WP02's build-logic (scratch/wp03-rules, made with
# code-rules/install.py), in one Gradle lock hold. Log: $O/logs/<name>.log
set -uo pipefail
OV=D:/projects/tools/.overhaul; O=$OV/out/WP03; L=$OV/bin/locked
name=${1:?name}
cd $OV/scratch/wp03-rules/templates/android/build-logic || exit 2
{ $L gradle ../gradlew :convention:test --tests '*SheetRule*' --tests '*CodeRulesTest*' --console=plain; echo "RULES EXIT $?"; } > $O/logs/$name.log 2>&1
tail -n 1 $O/logs/$name.log
