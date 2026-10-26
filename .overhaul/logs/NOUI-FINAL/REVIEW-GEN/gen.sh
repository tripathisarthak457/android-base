#!/usr/bin/env bash
set -u
O=D:/projects/tools/.overhaul
PY=C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe
G=$O/wt/NOUI-FINAL/generator
dest=$O/out/NOUI-FINAL/review
L=$O/logs/NOUI-FINAL/REVIEW-GEN
date
for m in ui noui; do
  for preset in lean standard everything; do
    name=$m-$preset
    ui=""; [ "$m" = noui ] && ui=', "ui_components": false'
    spec=$dest/spec-$name.json
    echo "{\"schema\": 2, \"platform\": \"android\", \"app_name\": \"Review Gen\", \"package_name\": \"com.example.reviewgen.$m$preset\", \"architecture\": \"compact\", \"features\": [], \"feature_modules\": []$ui}" > $spec
    (cd $G && $O/bin/memwait $PY -E -s create_project.py --spec $spec --preset $preset --out $dest/$name --no-zip --force > $L/gen-$name.log 2>&1; echo "$name exit $?")
  done
done
date
echo GEN DONE
(cd $G && $O/bin/memwait $PY -I -m unittest discover -s tests -t . > $L/unittest.log 2>&1; echo "unittest exit $?")
date
echo ALL DONE
