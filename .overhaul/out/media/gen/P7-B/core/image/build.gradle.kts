plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.compose")
}

android {
    namespace = "com.example.everything.core.image"

    // The component's tests compose under Robolectric, which reads the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:designsystem"))
    // appImageLoader() returns Coil's ImageLoader and takes OkHttp's Call.Factory. Naming OkHttp here also
    // makes every build resolve the catalog's one OkHttp, never the older release Coil's fetcher declares.
    api(libs.coil.compose)
    api(libs.okhttp)
    implementation(libs.coil.network.okhttp)

    testImplementation(project(":core:testing:compose"))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
