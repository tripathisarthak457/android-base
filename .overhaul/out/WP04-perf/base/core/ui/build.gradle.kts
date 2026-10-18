plugins {
    id("com.example.wp04.base.android.library")
    id("com.example.wp04.base.android.compose")
    id("com.example.wp04.base.android.hilt")
}

android {
    namespace = "com.example.wp04.base.core.ui"
}

dependencies {
    api(project(":core:common"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
    api(libs.androidx.paging.compose)
}
