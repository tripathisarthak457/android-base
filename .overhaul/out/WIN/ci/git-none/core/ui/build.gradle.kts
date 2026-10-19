plugins {
    id("com.example.cicheck.android.library")
    id("com.example.cicheck.android.compose")
    id("com.example.cicheck.android.hilt")
}

android {
    namespace = "com.example.cicheck.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
