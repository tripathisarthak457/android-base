#!/usr/bin/env bash
set -u
O=D:/projects/tools/.overhaul
PY=C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe
R=$O/out/NOUI-FINAL/review
L=$O/logs/NOUI-FINAL/REVIEW-GEN
G=$O/wt/NOUI-FINAL/generator
date
for m in ui noui; do
  ui=""; [ "$m" = noui ] && ui=', "ui_components": false'
  name=sp-$m
  echo "{\"schema\": 2, \"platform\": \"android\", \"app_name\": \"Review Sp\", \"package_name\": \"com.example.reviewsp.$m\", \"architecture\": \"compact\", \"features\": [\"network\", \"coil\", \"googlefonts\", \"leakcanary\", \"deeplink\", \"ci\", \"formatting\"], \"feature_modules\": []$ui}" > $R/spec-$name.json
  (cd $G && $O/bin/memwait $PY -E -s create_project.py --spec $R/spec-$name.json --out $R/$name --no-zip --force > $L/gen-$name.log 2>&1; echo "gen $name $?")
  (cd $G && $PY -E -s add_component.py Tag --project $R/$name > $L/addcomp-$name.log 2>&1; echo "add_component $name $?")
done
echo "D free GB: $(powershell -NoProfile -Command '(Get-PSDrive D).Free/1GB')"
for m in ui noui; do
  name=sp-$m
  (cd $R/$name && $O/bin/locked gradle ./gradlew spotlessCheck --offline > $L/spotless-$name.log 2>&1; echo "spotless $name $?")
done
date
echo SP DONE
