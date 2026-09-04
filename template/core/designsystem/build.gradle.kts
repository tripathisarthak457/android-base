plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
}

android {
    namespace = "com.base.app.core.designsystem"
}

dependencies {

    // Exposed as `api` on purpose: every module that draws anything needs foundation and ui
    // types in its own signatures — Modifier, Color, Dp, TextStyle — and re-declaring them per
    // module is noise that also lets two modules end up on different Compose versions.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.animation)
    api(libs.androidx.compose.ui.tooling.preview)

    // <opt:googlefonts>
    implementation(libs.androidx.compose.ui.text.googlefonts)
    // </opt:googlefonts>
}
