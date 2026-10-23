plugins {
    id("com.example.videoalone.android.library")
    id("com.example.videoalone.android.compose")
}

android {
    namespace = "com.example.videoalone.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
