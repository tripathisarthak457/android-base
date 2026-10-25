/*
 * Compose helpers for unit tests that render under Robolectric: the recomposition counter, Back
 * input through the navigation-event dispatcher, the window sizes adaptive tests render at, and the
 * preview tester every generated preview test runs. A sibling of :core:testing, so test classpaths
 * that need only coroutines and fakes stay free of Compose.
 */
plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.core.testing.compose"

    // Its own tests render under Robolectric, which reads the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    // `api`: a test module adds this one dependency and gets the Compose test rule and Robolectric.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.robolectric)
    api(libs.androidx.activity.compose)

    // The preview tester: Roborazzi's scanner and capture, and Google's Accessibility Test
    // Framework through Roborazzi's accessibility check.
    api(libs.roborazzi.preview.scanner)
    implementation(libs.roborazzi)
    implementation(libs.roborazzi.compose)
    implementation(libs.composable.preview.scanner)
    implementation(libs.roborazzi.accessibility.check)
    implementation(libs.accessibility.test.framework)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
