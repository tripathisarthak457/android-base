plugins {
    id("com.base.app.android.application")
    id("com.base.app.android.application.flavors")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    // <opt:firebase>
    id("com.base.app.android.firebase")
    // </opt:firebase>
    // <opt:performance>
    alias(libs.plugins.baselineprofile)
    // </opt:performance>
    // <opt:licenses>
    alias(libs.plugins.licensee)
    id("com.base.app.android.licenses")
    // </opt:licenses>
}

android {
    namespace = "com.base.app"

    defaultConfig {
        applicationId = "com.base.app"
    }

    // <opt:language>
    // Writes the android:localeConfig the phone's per-app language setting reads, from the
    // values-xx directories that actually exist. The default language is in resources.properties.
    androidResources {
        generateLocaleConfig = true
    }
    // </opt:language>
}

// <opt:performance>
// One profile, recorded on the dev flavour and merged into main so every flavour ships it. It is
// generated only on request (`:app:generateBaselineProfile`), because that needs a connected phone.
// Only dev has the benchmark and non-minified build types, so the other flavours' missing variants
// are expected and not warned about.
baselineProfile {
    mergeIntoMain = true
    saveInSrc = true
    automaticGenerationDuringBuild = false
    warnings {
        disabledVariants = false
    }
}
// </opt:performance>

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    // <opt:ui_components>
    implementation(project(":core:navigation:shell"))
    // </opt:ui_components>
    implementation(project(":core:ui"))
    // <opt:network>
    implementation(project(":core:network"))
    // </opt:network>
    // <opt:push>
    implementation(project(":core:notification"))
    // </opt:push>
    // <opt:applock>
    implementation(project(":core:applock"))
    // </opt:applock>
    // <opt:playstore>
    implementation(project(":core:playstore"))
    // </opt:playstore>
    // <opt:analytics>
    implementation(project(":core:analytics"))
    // </opt:analytics>

    // <opt:auth>
    implementation(project(":feature:auth"))
    // </opt:auth>
    // <opt:sample>
    implementation(project(":feature:sample"))
    // </opt:sample>
    // <opt:settings>
    implementation(project(":feature:settings"))
    // </opt:settings>
    // <opt:onboarding>
    implementation(project(":feature:onboarding"))
    // </opt:onboarding>
    // <opt:paging>
    implementation(project(":feature:feed"))
    // </opt:paging>
    // <opt:search>
    implementation(project(":feature:search"))
    // </opt:search>
    // <opt:profile>
    implementation(project(":feature:profile"))
    // </opt:profile>
    // <generated:app-feature-dependencies>

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    // <opt:offlinequeue>
    implementation(libs.androidx.work.runtime)
    // </opt:offlinequeue>

    // <opt:coil>
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // </opt:coil>

    // Hands the baseline profiles the app and its libraries ship to ART when the app is installed
    // outside Play, so a sideloaded or CI build starts compiled rather than interpreted. Every preset
    // ships the checked-in profiles, with or without the benchmark module.
    implementation(libs.androidx.profileinstaller)

    // <opt:performance>
    baselineProfile(project(":benchmark"))
    // </opt:performance>

    // <opt:flags>
    implementation(project(":core:flags"))
    // </opt:flags>

    // <opt:devtools>
    // Dev-flavour and debug builds only. A release of any other flavour has no trace of the
    // inspector, rather than one hidden behind a flag: the root finds no DevTooling bound and
    // composes nothing for it. `verify<Variant>CarriesNoDevTooling` fails `check` if that slips.
    debugImplementation(project(":core:devtools"))
    "devImplementation"(project(":core:devtools"))
    // </opt:devtools>
    // <opt:perfhud>
    debugImplementation(project(":core:perfhud"))
    "devImplementation"(project(":core:perfhud"))
    // </opt:perfhud>

    // <opt:database>
    implementation(project(":core:database"))
    // </opt:database>

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    // <opt:leakcanary>
    debugImplementation(libs.leakcanary)
    // </opt:leakcanary>

    // The token store and preferences in memory, for the session and start-up tests.
    testImplementation(testFixtures(project(":core:datastore")))
}

// <opt:licenses>
/*
 * What this app is allowed to ship. Licensee fails the build on anything not listed, which turns
 * "we shipped a copyleft dependency" from a discovery into a build error at the moment the
 * dependency is added.
 */
licensee {
    allow("Apache-2.0")
    allow("MIT")
    allow("BSD-2-Clause")
    allow("BSD-3-Clause")
    allow("EPL-1.0")
    allow("CC0-1.0")

    // These three state their licence as a URL rather than an SPDX id, so each is allowed by hand.
    allowUrl("https://developer.android.com/studio/terms.html") {
        because("The Android Software Development Kit License, on Google's own artifacts.")
    }
    allowUrl("https://developer.android.com/guide/playcore/license") {
        because("The Play Core Software Development Kit Terms of Service.")
    }
    allowUrl("https://opensource.org/license/mit") {
        because("MIT, stated as a URL rather than as an identifier — slf4j-api does this.")
    }
}
// </opt:licenses>
