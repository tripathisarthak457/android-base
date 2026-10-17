plugins {
    id("com.example.none.android.library")
    id("com.example.none.android.compose")
}

android {
    namespace = "com.example.none.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
