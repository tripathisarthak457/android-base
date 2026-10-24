plugins {
    id("com.example.deskstandard.android.library")
    id("com.example.deskstandard.android.compose")
    // The tooling seam's optional binding (`tooling/DevToolingModule.kt`).
    id("com.example.deskstandard.android.hilt")
}

android {
    namespace = "com.example.deskstandard.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.browser)
}
