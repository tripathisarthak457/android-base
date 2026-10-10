/*
 * The app lock: an overlay over the app's own content, decided before the first frame.
 */
plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.hilt")
}

android {
    namespace = "com.base.app.core.applock"
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
}
