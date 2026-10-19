plugins {
    id("com.base.app.android.library")
    id("com.base.app.android.compose")
    id("com.base.app.android.screenshots")
}

android {
    namespace = "com.base.app.core.designsystem"
}

kotlin {
    compilerOptions {
        // The shell seams are this module's own API; only other modules have to opt in to them.
        optIn.add("com.base.app.core.designsystem.foundation.layout.ShellSeamApi")
    }
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

    // Back and predictive back for sheets, alerts and the image viewer, and the lifecycle a toast's
    // time is counted against. Both are already in every app this module ships in.
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // <opt:googlefonts>
    implementation(libs.androidx.compose.ui.text.googlefonts)
    // </opt:googlefonts>

    // <opt:liquidglass>
    // Liquid glass on floating chrome: Kyant's backdrop blurs and bends the recorded scene behind
    // the tab bar, and its shapes give large chrome continuous (squircle) corners.
    implementation(libs.kyant.backdrop)
    implementation(libs.kyant.shapes)
    // </opt:liquidglass>

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // A test activity that owns a back dispatcher, as every app's activity does.
    testImplementation(libs.androidx.activity.compose)
    testImplementation(libs.robolectric)
}
