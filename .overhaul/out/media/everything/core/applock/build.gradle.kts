/*
 * The app lock: an overlay over the app's own content, decided before the first frame.
 */
plugins {
    id("com.example.everything.android.library")
    id("com.example.everything.android.compose")
    id("com.example.everything.android.hilt")
}

android {
    namespace = "com.example.everything.core.applock"

    // The gate's test runs under Robolectric, which needs the merged manifest and resources.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.biometric)
    // `api`: BiometricPrompt runs in a FragmentActivity, so the app's activity has to be one. Pinned
    // in the catalogue, since biometric alone would bring fragment 1.2.5.
    api(libs.androidx.fragment)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
