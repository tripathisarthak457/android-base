plugins {
    id("com.example.hud.fixture.android.library")
    id("com.example.hud.fixture.android.compose")
    id("com.example.hud.fixture.android.hilt")
}

android {
    namespace = "com.example.hud.fixture.core.ui"
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
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
    implementation(libs.lottie.compose)
    api(libs.androidx.paging.compose)
}
