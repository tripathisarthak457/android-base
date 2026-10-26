plugins {
    id("com.example.cmp.android.application")
    id("com.example.cmp.android.application.flavors")
    id("com.example.cmp.android.compose")
    id("com.example.cmp.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.cmp"

    defaultConfig {
        applicationId = "com.example.cmp"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))

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

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    debugImplementation(libs.leakcanary)

    // The token store and preferences in memory, for the session and start-up tests.
    testImplementation(testFixtures(project(":core:datastore")))
}
