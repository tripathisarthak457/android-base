plugins {
    id("io.example.fitclocal.android.application")
    id("io.example.fitclocal.android.application.flavors")
    id("io.example.fitclocal.android.compose")
    id("io.example.fitclocal.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.example.fitclocal"

    defaultConfig {
        applicationId = "io.example.fitclocal"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))

    implementation(project(":feature:abc"))
    implementation(project(":feature:abcdefghi"))
    implementation(project(":feature:abcdefghijklmno"))
    implementation(project(":feature:abcdefghijklmnopqrs"))
    implementation(project(":feature:abcdefghijklmnopqrstuvw"))
    implementation(project(":feature:abcdefghijklmnopqrstuvwxyzabcd"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hands the baseline profiles the app and its libraries ship to ART when the app is installed
    // outside Play, so a sideloaded or CI build starts compiled rather than interpreted. Every preset
    // ships the checked-in profiles, with or without the benchmark module.
    implementation(libs.androidx.profileinstaller)

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)
}

