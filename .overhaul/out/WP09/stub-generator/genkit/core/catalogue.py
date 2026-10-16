"""Stub of `python3 -m genkit.core.catalogue --json` (MASTER-PLAN A1.9) for WP09's smoke run."""
import json, sys
assert sys.argv[1:] == ["--json"], sys.argv
android = {"key": "android", "title": "Android Native", "status": "available", "summary": "Compose.",
           "features": [{"key": k, "title": k.title(), "implied_only": k == "firebase"} for k in
                        ["network", "auth", "forms", "settings", "liquidglass", "firebase"]],
           "presets": [{"key": "lean", "features": ["network"]},
                       {"key": "standard", "features": ["network", "auth", "forms", "settings"]}],
           "defaults": {"preset": "standard", "architecture": "compact", "design_style": "Utility", "motion_style": "Standard"}}
mp = dict(android, key="multiplatform", title="Compose Multiplatform", status="beta",
          status_note="Beta: the iOS app is not yet verified on a Mac")
ios = {"key": "ios", "title": "Swift", "status": "coming_soon", "summary": "SwiftUI.", "features": [], "presets": [], "defaults": {}}
json.dump({"version": 2, "generator_commit": "stub000", "targets": [android, mp, ios],
           "rules": {"spec_bytes_max": 32768, "feature_modules_max": 12, "languages_max": 10, "keystores_max": 4}}, sys.stdout)
