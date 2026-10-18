plugins {
    id("com.example.wp04.dlplay.android.library")
    id("com.example.wp04.dlplay.android.hilt")
    id("com.example.wp04.dlplay.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.wp04.dlplay.core.navigation"
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
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
