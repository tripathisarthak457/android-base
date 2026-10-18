plugins {
    id("com.example.hud.standard.android.library")
    id("com.example.hud.standard.android.compose")
    id("com.example.hud.standard.android.hilt")
}

android {
    namespace = "com.example.hud.standard.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
}
