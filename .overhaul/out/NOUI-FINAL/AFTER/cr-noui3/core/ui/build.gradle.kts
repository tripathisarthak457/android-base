plugins {
    id("com.example.finalafter.crnoui3.android.library")
    id("com.example.finalafter.crnoui3.android.compose")
    // The tooling seam's optional binding (`tooling/DevToolingModule.kt`).
    id("com.example.finalafter.crnoui3.android.hilt")
}

android {
    namespace = "com.example.finalafter.crnoui3.core.ui"
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
    // MessageHostTest renders the host under Robolectric.
    testImplementation(project(":core:testing:compose"))
    implementation(libs.coil.compose)
    // Coil finds its network fetcher at runtime, through a service loader.
    runtimeOnly(libs.coil.network.okhttp)
    implementation(libs.androidx.browser)
    api(libs.androidx.paging.compose)
}
