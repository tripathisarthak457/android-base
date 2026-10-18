plugins {
    id("com.example.hud.lean.android.library")
    id("com.example.hud.lean.android.compose")
    id("com.example.hud.lean.android.hilt")
}

android {
    namespace = "com.example.hud.lean.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
