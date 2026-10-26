#!/usr/bin/env bash
# genpair.sh <before|after> : the 8 preset shapes (lean, standard, everything compact, everything modular; UI and No-UI)
# from the exported HEAD tree (before) or the worktree (after), into out/NOUI-FINAL/AFTER/cmp-<side>/<shape>, all with
# the same package so the trees compare file by file.
set -u
side=$1
O=D:/projects/tools/.overhaul
PY=C:/Users/crazyhusir/AppData/Local/Python/pythoncore-3.15-64/python.exe
if [ "$side" = before ]; then G=$O/out/NOUI-FINAL/AFTER/headtree/generator; else G=$O/wt/NOUI-FINAL/generator; fi
dest=$O/out/NOUI-FINAL/AFTER/cmp-$side
mkdir -p $dest
for m in ui noui; do
  for shape in "lean compact" "standard compact" "everything compact" "everything modular"; do
    set -- $shape; preset=$1; arch=$2
    name=$m-$preset-$arch
    ui=""; flag=""; [ "$m" = noui ] && ui=', "ui_components": false'
    spec=$dest/spec-$name.json
    echo "{\"schema\": 2, \"platform\": \"android\", \"app_name\": \"Final After\", \"package_name\": \"com.example.cmp\", \"architecture\": \"$arch\", \"features\": [], \"feature_modules\": []$ui}" > $spec
    (cd $G && $O/bin/memwait $PY -E -s create_project.py --spec $spec --preset $preset --out $dest/$name --no-zip --force > $dest/gen-$name.log 2>&1; echo "$side $name $?")
  done
done
