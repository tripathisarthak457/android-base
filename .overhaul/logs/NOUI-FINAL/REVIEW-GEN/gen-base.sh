#!/usr/bin/env bash
set -u
O=D:/projects/tools/.overhaul
PY=C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe
R=$O/out/NOUI-FINAL/review
L=$O/logs/NOUI-FINAL/REVIEW-GEN
for m in ui noui; do
  ui=""; [ "$m" = noui ] && ui=', "ui_components": false'
  echo "{\"schema\": 2, \"platform\": \"android\", \"app_name\": \"Review Cmp\", \"package_name\": \"com.example.reviewcmp\", \"architecture\": \"compact\", \"features\": [], \"feature_modules\": []$ui}" > $R/spec-cmp-$m.json
  (cd $R/base-tree/generator && $O/bin/memwait $PY -E -s create_project.py --spec $R/spec-cmp-$m.json --preset everything --out $R/cmp-base-$m --no-zip --force > $L/gen-cmp-base-$m.log 2>&1; echo "base $m $?")
  (cd $O/wt/NOUI-FINAL/generator && $O/bin/memwait $PY -E -s create_project.py --spec $R/spec-cmp-$m.json --preset everything --out $R/cmp-head-$m --no-zip --force > $L/gen-cmp-head-$m.log 2>&1; echo "head $m $?")
done
echo CMP DONE
