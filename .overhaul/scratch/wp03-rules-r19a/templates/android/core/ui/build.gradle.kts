plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.core.ui"
}

dependencies {
    api(project(":core:mvi"))
    api(project(":core:designsystem"))
    implementation(project(":core:navigation"))

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // <opt:coil>
    implementation(libs.coil.compose)
    // Coil finds its network fetcher at runtime, through a service loader.
    runtimeOnly(libs.coil.network.okhttp)
    // </opt:coil>
    // <opt:browser>
    implementation(libs.androidx.browser)
    // </opt:browser>
    // <opt:paging>
    api(libs.androidx.paging.compose)
    // </opt:paging>
}
