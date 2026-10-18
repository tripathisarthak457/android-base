plugins {
    id("com.example.wp04.everything.android.library")
    id("com.example.wp04.everything.android.compose")
    id("com.example.wp04.everything.android.hilt")
}

android {
    namespace = "com.example.wp04.everything.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
    implementation(libs.lottie.compose)
    api(libs.androidx.paging.compose)
}
