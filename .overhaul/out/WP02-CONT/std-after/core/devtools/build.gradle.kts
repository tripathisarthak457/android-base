/*
 * The on-device inspector: the environment badge, and everything behind it. :app depends on it from
 * dev-flavour and debug builds only, so a shippable release carries none of it.
 */
plugins {
    id("com.example.standard.android.library")
    id("com.example.standard.android.compose")
    id("com.example.standard.android.hilt")
}

android {
    namespace = "com.example.standard.core.devtools"

    // The badge's drag test runs under Robolectric, which needs the merged manifest.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:network"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)

    implementation(project(":core:flags"))

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
