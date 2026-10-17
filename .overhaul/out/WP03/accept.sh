#!/usr/bin/env bash
# WP03's acceptance on the Windows workspace (Git Bash). locked and memwait set JAVA_HOME, ANDROID_HOME,
# GRADLE_USER_HOME and the cache variables from the user's registry environment.
set -euo pipefail
OV=D:/projects/tools/.overhaul; W=$OV/wt/WP03; L=$OV/bin/locked; M=$OV/bin/memwait; O=$OV/out/WP03

cd $W/generator
# Six tests fail on this host with no change at all until package WIN merges; anything beyond them fails here.
set +e
$M python -m unittest discover -s tests -t . > $O/logs/accept-generator.log 2>&1
set -e
python $O/scripts/beyond_baseline.py $O/logs/accept-generator.log
python -m genkit.tokens --check
python -m genkit.core.catalogue --check
python -m unittest tests.test_catalog_coverage

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
python $O/scripts/sweep_callsites.py . --check

# A generated project with a custom look carries it through tokens, not regexes (fixture, installed as
# com.example.wp03.look, design_style Editorial, motion_style Calm, accent #8E3B46, font "Plus Jakarta Sans",
# catalog and the four optional component groups on).
cd $W/generator && python create_project.py --spec tests/fixtures/v2/custom-look.json --set package_name=com.example.wp03.look --out $O/look --no-zip --force
TOK=$(find $O/look/core/designsystem -name DesignTokens.kt -path '*theme/tokens*')
grep -A20 'object Defaults' "$TOK" | grep -q 'Editorial'
grep -A20 'object Defaults' "$TOK" | grep -q 'Calm'
grep -q 'Plus Jakarta Sans' "$TOK"

# Every build in one Gradle lock hold: the template with all its checks and tests (contrast incl. the
# bar indicator, press, wheel, slider, refresh, sheets, a11y, chrome insets, overlay host, font fallback,
# recomposition journeys, the sheet's binary API), then the custom-look project's app and catalog release.
# Two workers: four R8 tasks at once exhaust the 3 GB daemon (WP04's accept, Oct 17); the heap is not raised.
OVERHAUL_GRADLE_WORKERS=2 $L gradle bash -c "set -euo pipefail
cd $W/templates/android
./gradlew build :core:designsystem:testDebugUnitTest --console=plain
cd $O/look
./gradlew :app:assembleDevDebug :catalog:assembleRelease --console=plain"
echo ACCEPT OK
