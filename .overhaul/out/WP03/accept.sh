#!/usr/bin/env bash
set -euo pipefail
export ANDROID_HOME=$HOME/Android/Sdk
OV=/home/crazyhusir/Documents/projects/tools/.overhaul; W=$OV/wt/WP03; L=$OV/bin/locked; O=$OV/out/WP03

cd $W/generator
python3 -m unittest discover -s tests -t .
python3 -m genkit.tokens --check
python3 -m genkit.core.catalogue --check
python3 -m unittest tests.test_catalog_coverage

# Patterns that must not come back. `! grep` would not stop a `set -e` script, so each is an if.
forbid() {  # <description> <grep args...>
    local what=$1; shift
    if grep -rn "$@"; then echo "FORBIDDEN: $what"; exit 1; fi
}
cd $W/templates/android
forbid "composed modifiers" "composed {" core/designsystem core/ui catalog --include=*.kt
forbid "ripple or the old indication" "material.ripple\|material3.ripple\|LocalIndication provides AppIndication" --include=*.kt .
forbid "a default tap haptic" "HapticEffect.Tap\|haptic: HapticEffect? = " core/designsystem --include=*.kt
forbid "a bare clickable" "\.clickable(" feature core/ui --include=*.kt

# A generated project with a custom look carries it through tokens, not regexes (fixture, installed as
# com.example.wp03.look, design_style Editorial, motion_style Calm, accent #8E3B46, font "Plus Jakarta Sans",
# catalog and the four optional component groups on).
cd $W/generator && python3 create_project.py --spec tests/fixtures/v2/custom-look.json --set package_name=com.example.wp03.look --out $O/look --no-zip --force
TOK=$(find $O/look/core/designsystem -name DesignTokens.kt -path '*theme/tokens*')
grep -A20 'object Defaults' "$TOK" | grep -q 'Editorial'
grep -A20 'object Defaults' "$TOK" | grep -q 'Calm'
grep -q 'Plus Jakarta Sans' "$TOK"

# Every build in one Gradle lock hold: the template with all its checks and tests (contrast incl. the
# bar indicator, press, wheel, slider, refresh, sheet, a11y, chrome insets, overlay host, font fallback,
# recomposition journeys), then the custom-look project's app and catalog release.
$L gradle bash -c "set -euo pipefail
cd $W/templates/android
./gradlew build :core:designsystem:testDebugUnitTest --console=plain
cd $O/look
./gradlew :app:assembleDevDebug :catalog:assembleRelease --console=plain"
echo ACCEPT OK
