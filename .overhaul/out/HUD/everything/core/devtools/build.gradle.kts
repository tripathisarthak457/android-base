/*
 * The on-device inspector: the environment badge, and everything behind it.
 */
plugins {
    id("com.example.hud.everything.android.library")
    id("com.example.hud.everything.android.compose")
    id("com.example.hud.everything.android.hilt")
}

android {
    namespace = "com.example.hud.everything.core.devtools"

    // The badge's drag test runs under Robolectric, which needs the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)

    implementation(project(":core:flags"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
