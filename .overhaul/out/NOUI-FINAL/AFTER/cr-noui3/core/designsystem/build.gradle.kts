plugins {
    id("com.example.finalafter.crnoui3.android.library")
    id("com.example.finalafter.crnoui3.android.compose")
    id("com.example.finalafter.crnoui3.android.screenshots")
}

android {
    namespace = "com.example.finalafter.crnoui3.core.designsystem"
}

dependencies {

    // `api` so modules can use Modifier, Color and friends without re-declaring Compose.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.animation)

    // Android's own window size classes, so breakpoints match every other app on the device.
    implementation(libs.androidx.window.core)

    implementation(libs.androidx.compose.ui.text.googlefonts)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // A test activity that owns a back dispatcher, as every app's activity does.
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.robolectric)
}
