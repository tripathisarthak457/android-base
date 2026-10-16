import json, random, sys, time, shutil, traceback, subprocess
from pathlib import Path
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/scratch-gen")
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/android-base/tools")
from gen import make, OUT
from check import check, Project
from genkit.spec import FEATURES, PRESETS, resolve_features
from genkit.translations import available

KEYS = sorted(f.key for f in FEATURES if not f.implied_only)
LANGS = list(available())
BASE = dict(app_name="Acme Field", package_name="com.acme.field")

runs = {}
runs["ref-all"] = dict(features=KEYS, feature_modules=["orders"], languages=LANGS)
for p in PRESETS:
    runs[f"preset-{p.key}"] = dict(features=list(p.features), languages=LANGS)
    runs[f"preset-{p.key}-mods"] = dict(features=list(p.features), feature_modules=["orders", "order_history"], languages=LANGS)
runs["none"] = dict(features=[], languages=LANGS)
runs["none-mods"] = dict(features=[], feature_modules=["orders"], languages=LANGS)
for k in KEYS:
    runs[f"only-{k}"] = dict(features=[k], languages=LANGS)
    runs[f"without-{k}"] = dict(features=sorted(resolve_features(set(KEYS) - {k}) ), languages=LANGS)
# pairwise
out = subprocess.run([sys.executable, "/home/crazyhusir/Documents/projects/tools/android-base/tools/feature_combinations.py"], capture_output=True, text=True).stdout
for i, fs in enumerate(json.loads(out)):
    runs[f"pair-{i}"] = dict(features=fs, feature_modules=["orders"], languages=LANGS)
rng = random.Random(7)
for i in range(60):
    d = rng.choice((0.15, 0.35, 0.5, 0.65, 0.85))
    runs[f"rand-{i}"] = dict(features=[k for k in KEYS if rng.random() < d], feature_modules=(["orders"] if rng.random()<0.5 else []), languages=LANGS if rng.random()<0.7 else [])

