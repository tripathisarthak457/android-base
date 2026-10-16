import sys, json, shutil, traceback
sys.path.insert(0, "/home/crazyhusir/Documents/projects/tools/.overhaul/scratch-gen")
from gen import make, OUT
from check import check, Project
from hilt import analyse
from genkit.spec import FEATURES
KEYS = sorted(f.key for f in FEATURES if not f.implied_only)
cases = [
  ("pkg-baseapp", "Trail Notes", "com.example.baseapp", ["orders"]),
  ("pkg-base_app", "Trail Notes", "com.base_app.demo", ["orders"]),
  ("pkg-apple", "Trail Notes", "com.base.apple", ["orders"]),
  ("pkg-prefix", "Trail Notes", "com.base.app.demo", ["orders"]),
  ("pkg-exact", "Trail Notes", "com.base.app", ["orders"]),
  ("pkg-global", "Trail Notes", "com.global.bank", []),
  ("pkg-pass", "Trail Notes", "com.example.pass", []),
  ("pkg-null", "Trail Notes", "com.example.null", []),
  ("pkg-true", "Trail Notes", "com.example.true", []),
  ("pkg-typealias", "Trail Notes", "com.example.typealias", []),
  ("pkg-java", "Trail Notes", "java.trail.notes", []),
  ("pkg-android", "Trail Notes", "android.trail.notes", []),
  ("pkg-digits", "Trail Notes", "com.ex4mple.app_1.v2", ["orders"]),
  ("pkg-long", "Trail Notes", ".".join(["segment" + "x"*20 + str(i) for i in range(8)]), ["orders"]),
  ("app-App", "App", "com.example.app", ["orders"]),
  ("app-single", "a", "com.example.a", []),
  ("app-ios", "iOS Clone", "com.example.ios", []),
  ("app-punct", "My-App_v2.0", "com.example.myapp", []),
  ("app-trailing", "  Padded Name  ", "com.example.padded", []),
  ("app-Xbaseapp", "Xbaseapp", "com.example.x", ["orders"]),
  ("app-Baseapp-x", "Baseapp X", "com.example.bx", ["orders"]),
  ("app-long", "The Quite Extraordinarily Long Application Name For Testing Limits", "com.example.longname", []),
  ("app-apostrophe", "Mom's Kitchen", "com.example.moms", []),
  ("app-amp", "Tom & Jerry", "com.example.tj", []),
  ("app-unicode", "Café Olé", "com.example.cafe", []),
  ("app-emoji", "Rocket 🚀", "com.example.rocket", []),
  ("app-lt", "A <b> app", "com.example.ab", []),
  ("app-backslash", "Back\\slash", "com.example.bs", []),
  ("app-digit", "2048 Game", "com.example.game", []),
  ("app-quote", 'Say "Hi"', "com.example.sayhi", []),
  ("mod-in", "Trail Notes", "com.example.trail", ["in"]),
  ("mod-fun", "Trail Notes", "com.example.trail", ["fun"]),
  ("mod-object", "Trail Notes", "com.example.trail", ["object"]),
  ("mod-null", "Trail Notes", "com.example.trail", ["null"]),
  ("mod-default", "Trail Notes", "com.example.trail", ["default"]),
  ("mod-class", "Trail Notes", "com.example.trail", ["class"]),
  ("mod-baseapp", "Trail Notes", "com.example.trail", ["baseapp"]),
  ("mod-base_app", "Trail Notes", "com.example.trail", ["base_app"]),
  ("mod-core-name", "Trail Notes", "com.example.trail", ["network", "common", "ui", "designsystem"]),
  ("mod-dunder", "Trail Notes", "com.example.trail", ["order__history", "a_"]),
  ("mod-digit", "Trail Notes", "com.example.trail", ["v2", "x1y"]),
  ("mod-trail-name", "Trail Notes", "com.example.trail", ["trail_notes"]),
  ("mod-devtools", "Trail Notes", "com.example.trail", ["devtools", "analytics", "notification", "media", "database", "widget", "lock", "push", "deeplink", "playstore"]),
]
results = {}
for name, app, pkg, mods in cases:
    fields = dict(app_name=app, package_name=pkg, features=KEYS, feature_modules=mods, languages=["de", "pt-BR"])
    try:
        spec, dest, res, el = make("n-" + name, fields)
    except Exception as e:
        print(f"{name}: REFUSED/FAILED {type(e).__name__}: {e}")
        continue
    iss = check(dest, None, spec.package_name)
    miss, _ = analyse(dest)
    iss = [i for i in iss if not i.startswith("CONVENTION A")]
    print(f"{name}: ok pascal={spec.pascal_name!r} snake={spec.snake_name!r} lower={spec.lower_name!r} issues={len(iss)+len(miss)}")
    for i in (iss + miss)[:12]:
        print("    ", i[:200])
