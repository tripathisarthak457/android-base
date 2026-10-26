plugins {
    id("com.example.cmp.android.application")
    id("com.example.cmp.android.application.flavors")
    id("com.example.cmp.android.compose")
    id("com.example.cmp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    id("com.example.cmp.android.firebase")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.licensee)
    id("com.example.cmp.android.licenses")
}

android {
    namespace = "com.example.cmp"

    defaultConfig {
        applicationId = "com.example.cmp"
    }

    // Writes the android:localeConfig the phone's per-app language setting reads, from the
    // values-xx directories that actually exist. The default language is in resources.properties.
    androidResources {
        generateLocaleConfig = true
    }
}

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

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(project(":core:notification"))
    implementation(project(":core:applock"))
    implementation(project(":core:playstore"))
    implementation(project(":core:analytics"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:sample"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:search"))
    implementation(project(":feature:profile"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.work.runtime)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Hands the baseline profiles the app and its libraries ship to ART when the app is installed
    // outside Play, so a sideloaded or CI build starts compiled rather than interpreted. Every preset
    // ships the checked-in profiles, with or without the benchmark module.
    implementation(libs.androidx.profileinstaller)

    baselineProfile(project(":benchmark"))

    implementation(project(":core:flags"))

    // Dev-flavour and debug builds only. A release of any other flavour has no trace of the
    // inspector, rather than one hidden behind a flag: the root finds no DevTooling bound and
    // composes nothing for it. `verify<Variant>CarriesNoDevTooling` fails `check` if that slips.
    debugImplementation(project(":core:devtools"))
    "devImplementation"(project(":core:devtools"))
    debugImplementation(project(":core:perfhud"))
    "devImplementation"(project(":core:perfhud"))

    implementation(project(":core:database"))

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    debugImplementation(libs.leakcanary)

    // The token store and preferences in memory, for the session and start-up tests.
    testImplementation(testFixtures(project(":core:datastore")))
}

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
