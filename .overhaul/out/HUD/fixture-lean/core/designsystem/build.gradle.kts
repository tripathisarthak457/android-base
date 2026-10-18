plugins {
    id("com.example.hud.lean.android.library")
    id("com.example.hud.lean.android.compose")
}

android {
    namespace = "com.example.hud.lean.core.designsystem"
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
}
