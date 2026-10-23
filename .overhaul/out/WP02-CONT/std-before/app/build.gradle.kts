plugins {
    id("com.example.standard.android.application")
    id("com.example.standard.android.application.flavors")
    id("com.example.standard.android.compose")
    id("com.example.standard.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.standard"

    defaultConfig {
        applicationId = "com.example.standard"
    }

    // Writes the android:localeConfig the phone's per-app language setting reads, from the
    // values-xx directories that actually exist. The default language is in resources.properties.
    androidResources {
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:sample"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Hands the baseline profiles the app and its libraries ship to ART when the app is installed
    // outside Play, so a sideloaded or CI build starts compiled rather than interpreted. Every preset
    // ships the checked-in profiles, with or without the benchmark module.
    implementation(libs.androidx.profileinstaller)

    implementation(project(":core:flags"))

    // Dev-flavour and debug builds only. A release of any other flavour has no trace of the
    // inspector, rather than one hidden behind a flag: the root finds no DevTooling bound and
    // composes nothing for it. `verify<Variant>CarriesNoDevTooling` fails `check` if that slips.
    debugImplementation(project(":core:devtools"))
    "devImplementation"(project(":core:devtools"))

    implementation(project(":core:database"))

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    debugImplementation(libs.leakcanary)
}

