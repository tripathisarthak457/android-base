plugins {
    id("io.example.audit.android.application")
    id("io.example.audit.android.application.flavors")
    id("io.example.audit.android.compose")
    id("io.example.audit.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "io.example.audit"

    defaultConfig {
        applicationId = "io.example.audit"
    }

    // AdaptiveAuditTest renders every screen under Robolectric, which reads the merged resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))

    implementation(project(":feature:sample"))
    implementation(project(":feature:order"))
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

    implementation(project(":core:database"))

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    // AdaptiveAuditTest: every feature's screen previews, found by scanning and rendered here,
    // where the app sees them all.
    testImplementation(project(":core:testing:compose"))
    testImplementation(libs.composable.preview.scanner)
}

