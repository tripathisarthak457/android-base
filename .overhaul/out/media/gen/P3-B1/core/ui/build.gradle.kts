plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.compose")
    // The tooling seam's optional binding (`tooling/DevToolingModule.kt`).
    id("com.example.everything.android.hilt")
}

android {
    namespace = "com.example.everything.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:network"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.browser)
    api(libs.androidx.paging.compose)
}
