plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.core.video"

    // The feed's tests compose under Robolectric, which reads the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:designsystem"))
    // Posters, through the app's one image loader.
    implementation(project(":core:image"))
    // AppVideoSetup.install() takes OkHttp's Call.Factory, so video goes through the app's own client.
    api(libs.okhttp)

    // Never Media3's View player or its Material player: AppVideo is the player UI.
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing:compose"))
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
