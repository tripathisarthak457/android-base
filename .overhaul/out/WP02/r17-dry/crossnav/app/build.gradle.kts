plugins {
    id("com.example.modulescompact.android.application")
    id("com.example.modulescompact.android.application.flavors")
    id("com.example.modulescompact.android.compose")
    id("com.example.modulescompact.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.modulescompact"

    defaultConfig {
        applicationId = "com.example.modulescompact"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))

    implementation(project(":feature:orders:impl"))
    implementation(project(":feature:trips:impl"))
    implementation(project(":feature:wallet:impl"))
    implementation(project(":data:orders:impl"))
    implementation(project(":data:trips:impl"))
    implementation(project(":data:wallet:impl"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hands the baseline profiles the app and its libraries ship to ART when the app is installed
    // outside Play, so a sideloaded or CI build starts compiled rather than interpreted.
    implementation(libs.androidx.profileinstaller)

    // Names each composable in a Perfetto trace, so a slow frame shows what composed in it. Only
    // the dev flavour, where traces and benchmarks are taken; it costs nothing until one records.
    devImplementation(libs.androidx.compose.runtime.tracing)

    debugImplementation(libs.leakcanary)
}

