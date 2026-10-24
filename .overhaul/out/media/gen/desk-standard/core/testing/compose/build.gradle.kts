/*
 * Compose helpers for unit tests that render under Robolectric: the recomposition counter, Back
 * input through the navigation-event dispatcher, and the window sizes adaptive tests render at. A
 * sibling of :core:testing, so test classpaths that need only coroutines and fakes stay free of
 * Compose.
 */
plugins {
    id("com.example.deskstandard.android.library")
    id("com.example.deskstandard.android.compose")
}

android {
    namespace = "com.example.deskstandard.core.testing.compose"

    // Its own tests render under Robolectric, which reads the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    // `api`: a test module adds this one dependency and gets the Compose test rule and Robolectric.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.robolectric)
    api(libs.androidx.activity.compose)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
