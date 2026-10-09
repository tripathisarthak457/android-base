plugins {
    id("com.base.app.android.application")
    id("com.base.app.android.application.flavors")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
    alias(libs.plugins.kotlin.serialization)
    // <opt:firebase>
    id("com.base.app.android.firebase")
    // </opt:firebase>
    // <opt:baselineprofile>
    alias(libs.plugins.baselineprofile)
    // </opt:baselineprofile>
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

// <opt:baselineprofile>
// Only dev has the benchmark and non-minified build types: the profile it generates is merged into
// main and ships with every flavour, so the other flavours' missing variants are expected.
baselineProfile {
    warnings {
        disabledVariants = false
    }
}
// </opt:baselineprofile>

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    // <opt:network>
    implementation(project(":core:network"))
    // </opt:network>
    // <opt:push>
    implementation(project(":core:notification"))
    // </opt:push>
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

    // <opt:workmanager>
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    // </opt:workmanager>

    // Hands the baseline profiles the app and its libraries ship to ART when the app is installed
    // outside Play, so a sideloaded or CI build starts compiled rather than interpreted.
    implementation(libs.androidx.profileinstaller)

    // <opt:baselineprofile>
    baselineProfile(project(":benchmark"))
    // </opt:baselineprofile>

    // <opt:flags>
    implementation(project(":core:flags"))
    // </opt:flags>

    // <opt:devtools>
    implementation(project(":core:devtools"))
    // </opt:devtools>

    // <opt:database>
    implementation(project(":core:database"))
    // </opt:database>

    // <opt:applock>
    implementation(libs.androidx.biometric)
    // </opt:applock>

    // <opt:playstore>
    implementation(libs.play.app.update)
    implementation(libs.play.app.review)
    // </opt:playstore>

    // <opt:widget>
    implementation(libs.androidx.glance.appwidget)
    // </opt:widget>

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    // <opt:leakcanary>
    debugImplementation(libs.leakcanary)
    // </opt:leakcanary>
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
