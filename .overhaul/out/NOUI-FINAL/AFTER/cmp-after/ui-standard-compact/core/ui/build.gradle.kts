plugins {
    id("com.example.cmp.android.library")
    id("com.example.cmp.android.compose")
    // The tooling seam's optional binding (`tooling/DevToolingModule.kt`).
    id("com.example.cmp.android.hilt")
}

android {
    namespace = "com.example.cmp.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    // Coil finds its network fetcher at runtime, through a service loader.
    runtimeOnly(libs.coil.network.okhttp)
    implementation(libs.androidx.browser)
}
