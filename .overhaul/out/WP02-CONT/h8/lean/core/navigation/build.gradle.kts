plugins {
    id("com.example.lean.android.library")
    id("com.example.lean.android.hilt")
    id("com.example.lean.android.compose")
    alias(libs.plugins.kotlin.serialization)
    // MultiInstanceTest's activity is an @AndroidEntryPoint, which the Hilt plugin rewrites.
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.lean.core.navigation"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    // AppNavKey is in every signature of this module's API.
    api(project(":core:common"))
    implementation(project(":core:designsystem"))

    // Only this module sees androidx.navigation3; features depend on AppNavKey and NavGraphEntry.
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.activity.compose)

    api(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(project(":core:testing"))
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
