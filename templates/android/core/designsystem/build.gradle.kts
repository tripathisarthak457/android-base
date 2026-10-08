plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.core.designsystem"
}

dependencies {

    // `api` so modules can use Modifier, Color and friends without re-declaring Compose.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.ui.tooling.preview)

    // Android's own window size classes, so breakpoints match every other app on the device.
    implementation(libs.androidx.window.core)

    // <opt:googlefonts>
    implementation(libs.androidx.compose.ui.text.googlefonts)
    // </opt:googlefonts>
}
